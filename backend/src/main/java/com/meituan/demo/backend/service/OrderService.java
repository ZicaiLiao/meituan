package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.AddCartItemRequest;
import com.meituan.demo.backend.model.ApiModels.CreateOrderRequest;
import com.meituan.demo.backend.model.DomainModels.Address;
import com.meituan.demo.backend.model.DomainModels.CartItem;
import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.Order;
import com.meituan.demo.backend.model.DomainModels.OrderLine;
import com.meituan.demo.backend.model.DomainModels.OrderStatusLog;
import com.meituan.demo.backend.model.DomainModels.OrderStatus;
import com.meituan.demo.backend.model.DomainModels.Product;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.User;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final DemoDataStore dataStore;
    private final StreamService streamService;
    private final IntegrationEventService integrationEventService;

    public OrderService(DemoDataStore dataStore, StreamService streamService, IntegrationEventService integrationEventService) {
        this.dataStore = dataStore;
        this.streamService = streamService;
        this.integrationEventService = integrationEventService;
    }

    public List<CartItem> getCart(Long userId) {
        return dataStore.carts().getOrDefault(userId, new ArrayList<>());
    }

    public List<CartItem> addCartItem(Long userId, AddCartItemRequest request) {
        List<CartItem> items = new ArrayList<>(dataStore.carts().getOrDefault(userId, new ArrayList<>()));
        items.removeIf(item -> item.productId().equals(request.productId()));
        items.add(new CartItem(request.productId(), request.quantity()));
        dataStore.carts().put(userId, items);
        return items;
    }

    public List<Address> addresses(Long userId) {
        return dataStore.addresses().getOrDefault(userId, List.of());
    }

    public Order createOrder(DemoUserPrincipal principal, CreateOrderRequest request) {
        List<CartItem> cart = getCart(principal.id());
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        List<Product> selectedProducts = cart.stream()
                .map(item -> dataStore.products().get(item.productId()))
                .filter(Objects::nonNull)
                .toList();
        Long shopId = selectedProducts.getFirst().shopId();
        boolean mixedShop = selectedProducts.stream().anyMatch(product -> !shopId.equals(product.shopId()));
        if (mixedShop) {
            throw new IllegalArgumentException("Cart contains products from multiple shops");
        }
        List<OrderLine> lines = cart.stream()
                .map(item -> {
                    Product product = dataStore.products().get(item.productId());
                    return new OrderLine(product.id(), product.name(), item.quantity(), product.price());
                })
                .toList();

        BigDecimal total = lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Coupon coupon = request.couponId() == null ? null : dataStore.coupons().get(request.couponId());
        if (coupon != null && coupon.shopId() != null && !coupon.shopId().equals(shopId)) {
            throw new IllegalArgumentException("Coupon does not apply to this shop");
        }
        BigDecimal discount = (coupon != null && total.compareTo(coupon.minimumSpend()) >= 0)
                ? coupon.discountAmount()
                : BigDecimal.ZERO;
        BigDecimal payable = total.subtract(discount);

        Long addressId = request.addressId() != null ? request.addressId() : addresses(principal.id()).stream()
                .findFirst()
                .map(Address::id)
                .orElse(null);

        Order order = new Order(dataStore.nextOrderId(), principal.id(), shopId, null,
                OrderStatus.PENDING_PAYMENT, total, payable, request.couponId(), addressId, Instant.now(), lines);
        dataStore.orders().put(order.id(), order);
        appendStatusLog(order.id(), OrderStatus.PENDING_PAYMENT, "用户提交订单");
        dataStore.carts().remove(principal.id());
        notifyMerchant(shopId, "order.created", "新订单待支付", "用户已创建订单 #" + order.id());
        integrationEventService.publish("order-created", Map.of("orderId", order.id(), "userId", principal.id(), "shopId", shopId));
        return order;
    }

    public Order payOrder(Long orderId, DemoUserPrincipal principal) {
        Order current = dataStore.orders().get(orderId);
        assertOrderExists(current, orderId);
        if (!current.userId().equals(principal.id())) {
            throw new IllegalArgumentException("Order does not belong to current user");
        }
        ensureStatus(current, OrderStatus.PENDING_PAYMENT, "Only pending payment orders can be paid");
        Order paid = new Order(current.id(), current.userId(), current.shopId(), current.riderId(),
                OrderStatus.PAID_WAITING_MERCHANT, current.totalAmount(), current.payableAmount(), current.couponId(),
                current.addressId(), current.createdAt(), current.items());
        dataStore.orders().put(orderId, paid);
        appendStatusLog(orderId, OrderStatus.PAID_WAITING_MERCHANT, "支付成功，等待商家接单");
        notifyMerchant(current.shopId(), "payment.succeeded", "订单已支付", "订单 #" + orderId + " 待商家接单");
        streamService.notifyUser(Role.CUSTOMER, principal.id(), "payment.succeeded", "支付成功", "订单已支付，等待商家接单");
        integrationEventService.publish("payment-succeeded", Map.of("orderId", orderId));
        return paid;
    }

    public List<Order> ordersForPrincipal(DemoUserPrincipal principal) {
        return switch (principal.role()) {
            case CUSTOMER -> dataStore.sortedOrders().stream()
                    .filter(order -> order.userId().equals(principal.id()))
                    .toList();
            case MERCHANT -> dataStore.sortedOrders().stream()
                    .filter(order -> {
                        User merchant = dataStore.users().get(principal.id());
                        return order.shopId().equals(merchant.shopId());
                    })
                    .toList();
            case RIDER -> dataStore.sortedOrders().stream()
                    .filter(order -> principal.id().equals(order.riderId()) || order.status() == OrderStatus.RIDER_PENDING)
                    .toList();
            default -> dataStore.sortedOrders();
        };
    }

    public Order merchantAccept(Long orderId, DemoUserPrincipal principal) {
        Order current = dataStore.orders().get(orderId);
        assertOrderExists(current, orderId);
        validateMerchantOwnership(current, principal);
        ensureStatus(current, OrderStatus.PAID_WAITING_MERCHANT, "Only paid orders can be accepted");
        Order updated = new Order(current.id(), current.userId(), current.shopId(), current.riderId(),
                OrderStatus.RIDER_PENDING, current.totalAmount(), current.payableAmount(), current.couponId(),
                current.addressId(), current.createdAt(), current.items());
        dataStore.orders().put(orderId, updated);
        appendStatusLog(orderId, OrderStatus.RIDER_PENDING, "商家已接单，等待骑手抢单");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "merchant.accepted", "商家已接单", "骑手即将接单配送");
        notifyAllRiders("merchant.accepted", "新的可抢订单", "订单 #" + orderId + " 已进入骑手抢单池");
        integrationEventService.publish("merchant-accepted", Map.of("orderId", orderId));
        return updated;
    }

    public Order merchantReject(Long orderId, DemoUserPrincipal principal) {
        Order current = dataStore.orders().get(orderId);
        assertOrderExists(current, orderId);
        validateMerchantOwnership(current, principal);
        ensureStatus(current, OrderStatus.PAID_WAITING_MERCHANT, "Only paid orders can be rejected");
        Order updated = new Order(current.id(), current.userId(), current.shopId(), current.riderId(),
                OrderStatus.REJECTED, current.totalAmount(), current.payableAmount(), current.couponId(),
                current.addressId(), current.createdAt(), current.items());
        dataStore.orders().put(orderId, updated);
        appendStatusLog(orderId, OrderStatus.REJECTED, "商家拒单，已触发退款");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "merchant.rejected", "商家拒单", "订单已退款并返还优惠资格");
        integrationEventService.publish("merchant-rejected", Map.of("orderId", orderId));
        return updated;
    }

    public List<Order> availableOrders() {
        return dataStore.sortedOrders().stream()
                .filter(order -> order.status() == OrderStatus.RIDER_PENDING)
                .toList();
    }

    public Order riderAccept(Long orderId, DemoUserPrincipal principal) {
        Order current = dataStore.orders().get(orderId);
        assertOrderExists(current, orderId);
        ensureStatus(current, OrderStatus.RIDER_PENDING, "Only rider-pending orders can be accepted");
        Order updated = new Order(current.id(), current.userId(), current.shopId(), principal.id(),
                OrderStatus.DELIVERING, current.totalAmount(), current.payableAmount(), current.couponId(),
                current.addressId(), current.createdAt(), current.items());
        dataStore.orders().put(orderId, updated);
        appendStatusLog(orderId, OrderStatus.DELIVERING, "骑手已接单，正在配送");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "rider.accepted", "骑手已接单", "骑手正在前往商家取餐");
        notifyMerchant(current.shopId(), "rider.accepted", "骑手已接单", "订单 #" + orderId + " 已有骑手接单");
        return updated;
    }

    public Order riderDeliver(Long orderId, DemoUserPrincipal principal) {
        Order current = dataStore.orders().get(orderId);
        assertOrderExists(current, orderId);
        ensureStatus(current, OrderStatus.DELIVERING, "Only delivering orders can be completed");
        if (!principal.id().equals(current.riderId())) {
            throw new IllegalArgumentException("Only the assigned rider can complete delivery");
        }
        Order updated = new Order(current.id(), current.userId(), current.shopId(), principal.id(),
                OrderStatus.COMPLETED, current.totalAmount(), current.payableAmount(), current.couponId(),
                current.addressId(), current.createdAt(), current.items());
        dataStore.orders().put(orderId, updated);
        appendStatusLog(orderId, OrderStatus.COMPLETED, "订单已送达");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "delivery.completed", "订单已送达", "感谢使用，欢迎对本次订单进行评价");
        integrationEventService.publish("delivery-completed", Map.of("orderId", orderId, "riderId", principal.id()));
        return updated;
    }

    public List<OrderStatusLog> orderTimeline(Long orderId, DemoUserPrincipal principal) {
        Order order = dataStore.orders().get(orderId);
        assertOrderExists(order, orderId);
        if (principal.role() == Role.CUSTOMER && !order.userId().equals(principal.id())) {
            throw new IllegalArgumentException("Order does not belong to current user");
        }
        return dataStore.orderStatusLogs().getOrDefault(orderId, List.of());
    }

    public Map<String, Object> summary() {
        List<Order> orders = dataStore.sortedOrders();
        long completed = orders.stream().filter(order -> order.status() == OrderStatus.COMPLETED).count();
        BigDecimal gmv = orders.stream().map(Order::payableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> statuses = orders.stream()
                .collect(Collectors.groupingBy(order -> order.status().name(), Collectors.counting()));
        Map<String, Object> summary = new HashMap<>();
        summary.put("orders", orders);
        summary.put("completedCount", completed);
        summary.put("gmv", gmv);
        summary.put("statusBreakdown", statuses);
        return summary;
    }

    private void notifyMerchant(Long shopId, String event, String title, String content) {
        dataStore.users().values().stream()
                .filter(user -> user.role() == Role.MERCHANT && shopId.equals(user.shopId()))
                .findFirst()
                .ifPresent(merchant -> streamService.notifyUser(Role.MERCHANT, merchant.id(), event, title, content));
    }

    private void notifyAllRiders(String event, String title, String content) {
        dataStore.users().values().stream()
                .filter(user -> user.role() == Role.RIDER)
                .sorted(Comparator.comparing(User::id))
                .forEach(rider -> streamService.notifyUser(Role.RIDER, rider.id(), event, title, content));
    }

    private void assertOrderExists(Order order, Long orderId) {
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
    }

    private void validateMerchantOwnership(Order order, DemoUserPrincipal principal) {
        User merchant = dataStore.users().get(principal.id());
        if (merchant == null || !order.shopId().equals(merchant.shopId())) {
            throw new IllegalArgumentException("Order does not belong to current merchant");
        }
    }

    private void ensureStatus(Order order, OrderStatus expected, String message) {
        if (order.status() != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void appendStatusLog(Long orderId, OrderStatus status, String note) {
        dataStore.orderStatusLogs()
                .computeIfAbsent(orderId, unused -> new ArrayList<>())
                .add(new OrderStatusLog(orderId, status, note, Instant.now()));
    }
}
