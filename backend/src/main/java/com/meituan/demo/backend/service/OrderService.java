package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.AddCartItemRequest;
import com.meituan.demo.backend.model.ApiModels.AddressUpsertRequest;
import com.meituan.demo.backend.model.ApiModels.CreateOrderRequest;
import com.meituan.demo.backend.model.ApiModels.ReviewOrderRequest;
import com.meituan.demo.backend.model.DomainModels.Address;
import com.meituan.demo.backend.model.DomainModels.CartItem;
import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.Order;
import com.meituan.demo.backend.model.DomainModels.OrderLine;
import com.meituan.demo.backend.model.DomainModels.OrderStatus;
import com.meituan.demo.backend.model.DomainModels.OrderStatusLog;
import com.meituan.demo.backend.model.DomainModels.Product;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.Shop;
import com.meituan.demo.backend.model.DomainModels.User;
import com.meituan.demo.backend.repository.CatalogRepository;
import com.meituan.demo.backend.repository.MarketingRepository;
import com.meituan.demo.backend.repository.OrderRepository;
import com.meituan.demo.backend.repository.UserRepository;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogRepository catalogRepository;
    private final MarketingRepository marketingRepository;
    private final MarketingService marketingService;
    private final UserRepository userRepository;
    private final StreamService streamService;
    private final IntegrationEventService integrationEventService;

    public OrderService(
            OrderRepository orderRepository,
            CatalogRepository catalogRepository,
            MarketingRepository marketingRepository,
            MarketingService marketingService,
            UserRepository userRepository,
            StreamService streamService,
            IntegrationEventService integrationEventService) {
        this.orderRepository = orderRepository;
        this.catalogRepository = catalogRepository;
        this.marketingRepository = marketingRepository;
        this.marketingService = marketingService;
        this.userRepository = userRepository;
        this.streamService = streamService;
        this.integrationEventService = integrationEventService;
    }

    public List<CartItem> getCart(Long userId) {
        return orderRepository.findCartByUserId(userId);
    }

    @Transactional
    public List<CartItem> addCartItem(Long userId, AddCartItemRequest request) {
        orderRepository.upsertCartItem(userId, request.productId(), request.quantity());
        return orderRepository.findCartByUserId(userId);
    }

    public List<Address> addresses(Long userId) {
        return orderRepository.findAddressesByUserId(userId);
    }

    @Transactional
    public Address createAddress(Long userId, AddressUpsertRequest request) {
        return orderRepository.createAddress(
                userId,
                request.label(),
                request.detail(),
                request.contactName(),
                request.phone(),
                request.isDefault());
    }

    @Transactional
    public void updateAddress(Long userId, Long addressId, AddressUpsertRequest request) {
        orderRepository.updateAddress(
                addressId,
                userId,
                request.label(),
                request.detail(),
                request.contactName(),
                request.phone(),
                request.isDefault());
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        orderRepository.deleteAddress(addressId, userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        orderRepository.clearCart(userId);
    }

    @Transactional
    public Order createOrder(DemoUserPrincipal principal, CreateOrderRequest request) {
        List<CartItem> cart = getCart(principal.id());
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Map<Long, Product> products = catalogRepository.findProductsByIds(cart.stream().map(CartItem::productId).toList());
        if (products.size() != cart.size()) {
            throw new IllegalArgumentException("Some cart products no longer exist");
        }

        List<Product> selectedProducts = cart.stream().map(item -> products.get(item.productId())).filter(Objects::nonNull).toList();
        Long shopId = selectedProducts.getFirst().shopId();
        boolean mixedShop = selectedProducts.stream().anyMatch(product -> !shopId.equals(product.shopId()));
        if (mixedShop) {
            throw new IllegalArgumentException("Cart contains products from multiple shops");
        }

        Shop shop = catalogRepository.findShopById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));
        if (!"OPEN".equalsIgnoreCase(shop.status())) {
            throw new IllegalStateException("Shop is currently unavailable");
        }

        List<OrderLine> lines = cart.stream()
                .map(item -> {
                    Product product = products.get(item.productId());
                    if (!product.enabled()) {
                        throw new IllegalStateException("Product is unavailable: " + product.name());
                    }
                    boolean reserved = catalogRepository.reserveStock(product.id(), item.quantity());
                    if (!reserved) {
                        throw new IllegalStateException("Insufficient stock for " + product.name());
                    }
                    return new OrderLine(product.id(), product.name(), item.quantity(), product.price());
                })
                .toList();

        BigDecimal total = lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(shop.minOrderAmount()) < 0) {
            throw new IllegalStateException("Minimum order amount is " + shop.minOrderAmount());
        }

        Coupon coupon = request.couponId() == null
                ? null
                : marketingRepository.findCouponById(request.couponId())
                        .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        if (coupon != null && coupon.shopId() != null && !coupon.shopId().equals(shopId)) {
            throw new IllegalArgumentException("Coupon does not apply to this shop");
        }

        BigDecimal discount = coupon != null && total.compareTo(coupon.minimumSpend()) >= 0 ? coupon.discountAmount() : BigDecimal.ZERO;
        BigDecimal payable = total.subtract(discount);
        Long addressId = request.addressId() != null
                ? request.addressId()
                : addresses(principal.id()).stream().findFirst().map(Address::id).orElse(null);

        Order order = orderRepository.createOrder(
                principal.id(),
                shopId,
                request.couponId(),
                addressId,
                OrderStatus.PENDING_PAYMENT,
                total,
                payable,
                lines);
        orderRepository.appendStatusLog(order.id(), OrderStatus.PENDING_PAYMENT, "用户提交订单");
        orderRepository.clearCart(principal.id());
        notifyMerchant(shopId, "order.created", "新订单待支付", "用户已创建订单 #" + order.id());
        integrationEventService.publish("order-created", Map.of("orderId", order.id(), "userId", principal.id(), "shopId", shopId));
        return order;
    }

    @Transactional
    public Order payOrder(Long orderId, DemoUserPrincipal principal) {
        Order current = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!current.userId().equals(principal.id())) {
            throw new IllegalArgumentException("Order does not belong to current user");
        }
        ensureStatus(current, OrderStatus.PENDING_PAYMENT, "Only pending payment orders can be paid");
        Order paid = orderRepository.updateOrderStatus(orderId, OrderStatus.PAID_WAITING_MERCHANT, current.riderId());
        orderRepository.appendStatusLog(orderId, OrderStatus.PAID_WAITING_MERCHANT, "支付成功，等待商家接单");
        notifyMerchant(current.shopId(), "payment.succeeded", "订单已支付", "订单 #" + orderId + " 待商家接单");
        streamService.notifyUser(Role.CUSTOMER, principal.id(), "payment.succeeded", "支付成功", "订单已支付，等待商家接单");
        integrationEventService.publish("payment-succeeded", Map.of("orderId", orderId));
        return paid;
    }

    @Transactional
    public Order cancelOrder(Long orderId, DemoUserPrincipal principal) {
        Order current = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!current.userId().equals(principal.id())) {
            throw new IllegalArgumentException("Order does not belong to current user");
        }
        if (current.status() != OrderStatus.PENDING_PAYMENT && current.status() != OrderStatus.PAID_WAITING_MERCHANT) {
            throw new IllegalStateException("Only unpaid or merchant-pending orders can be cancelled");
        }
        OrderStatus nextStatus = current.status() == OrderStatus.PAID_WAITING_MERCHANT ? OrderStatus.REFUNDED : OrderStatus.CANCELLED;
        restoreOrderStock(current);
        Order updated = orderRepository.updateOrderStatus(orderId, nextStatus, current.riderId());
        orderRepository.appendStatusLog(orderId, nextStatus, nextStatus == OrderStatus.REFUNDED ? "用户取消订单，已退款" : "用户取消订单");
        streamService.notifyUser(Role.CUSTOMER, principal.id(), "order.cancelled", "订单已取消", "库存与优惠资格已回滚");
        notifyMerchant(current.shopId(), "order.cancelled", "用户取消订单", "订单 #" + orderId + " 已取消");
        return updated;
    }

    public List<Order> ordersForPrincipal(DemoUserPrincipal principal) {
        return switch (principal.role()) {
            case CUSTOMER -> orderRepository.findOrdersByUserId(principal.id());
            case MERCHANT -> orderRepository.findOrdersByShopId(resolveShopIdForMerchant(principal.id()));
            case RIDER -> orderRepository.findOrdersByRiderId(principal.id());
            default -> orderRepository.findAllOrders();
        };
    }

    @Transactional
    public Order merchantAccept(Long orderId, DemoUserPrincipal principal) {
        Order current = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        validateMerchantOwnership(current, principal);
        ensureStatus(current, OrderStatus.PAID_WAITING_MERCHANT, "Only paid orders can be accepted");
        Order updated = orderRepository.updateOrderStatus(orderId, OrderStatus.RIDER_PENDING, current.riderId());
        orderRepository.appendStatusLog(orderId, OrderStatus.RIDER_PENDING, "商家已接单，等待骑手抢单");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "merchant.accepted", "商家已接单", "骑手即将接单配送");
        notifyAllRiders("merchant.accepted", "新的可抢订单", "订单 #" + orderId + " 已进入骑手抢单池");
        integrationEventService.publish("merchant-accepted", Map.of("orderId", orderId));
        return updated;
    }

    @Transactional
    public Order merchantReject(Long orderId, DemoUserPrincipal principal) {
        Order current = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        validateMerchantOwnership(current, principal);
        ensureStatus(current, OrderStatus.PAID_WAITING_MERCHANT, "Only paid orders can be rejected");
        restoreOrderStock(current);
        Order updated = orderRepository.updateOrderStatus(orderId, OrderStatus.REJECTED, current.riderId());
        orderRepository.appendStatusLog(orderId, OrderStatus.REJECTED, "商家拒单，已触发退款");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "merchant.rejected", "商家拒单", "订单已退款并返还优惠资格");
        integrationEventService.publish("merchant-rejected", Map.of("orderId", orderId));
        return updated;
    }

    public List<Order> availableOrders() {
        return orderRepository.findAvailableOrders();
    }

    @Transactional
    public Order riderAccept(Long orderId, DemoUserPrincipal principal) {
        Order current = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        ensureStatus(current, OrderStatus.RIDER_PENDING, "Only rider-pending orders can be accepted");
        validateRiderCanTakeMoreOrders(principal.id());
        Order updated = orderRepository.updateOrderStatus(orderId, OrderStatus.DELIVERING, principal.id());
        orderRepository.appendStatusLog(orderId, OrderStatus.DELIVERING, "骑手已接单，正在配送");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "rider.accepted", "骑手已接单", "骑手正在前往商家取餐");
        notifyMerchant(current.shopId(), "rider.accepted", "骑手已接单", "订单 #" + orderId + " 已有骑手接单");
        return updated;
    }

    @Transactional
    public Order riderDeliver(Long orderId, DemoUserPrincipal principal) {
        Order current = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        ensureStatus(current, OrderStatus.DELIVERING, "Only delivering orders can be completed");
        if (!principal.id().equals(current.riderId())) {
            throw new IllegalArgumentException("Only the assigned rider can complete delivery");
        }
        Order updated = orderRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED, principal.id());
        orderRepository.appendStatusLog(orderId, OrderStatus.COMPLETED, "订单已送达");
        marketingService.rewardOrderCompleted(current.userId(), current.payableAmount());
        notifyMerchant(current.shopId(), "delivery.completed", "订单已完成", "订单 #" + orderId + " 已由骑手送达");
        streamService.notifyUser(Role.CUSTOMER, current.userId(), "delivery.completed", "订单已送达", "感谢使用，欢迎对本次订单进行评价");
        integrationEventService.publish("delivery-completed", Map.of("orderId", orderId, "riderId", principal.id()));
        return updated;
    }

    @Transactional
    public Order reviewOrder(Long orderId, DemoUserPrincipal principal, ReviewOrderRequest request) {
        Order current = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!current.userId().equals(principal.id())) {
            throw new IllegalArgumentException("Order does not belong to current user");
        }
        ensureStatus(current, OrderStatus.COMPLETED, "Only completed orders can be reviewed");
        if (current.reviewScore() != null) {
            throw new IllegalStateException("Order has already been reviewed");
        }
        orderRepository.submitReview(orderId, request.score(), request.content());
        orderRepository.appendStatusLog(orderId, OrderStatus.COMPLETED, "用户已评价订单");
        marketingService.rewardReview(principal.id());
        notifyMerchant(current.shopId(), "order.reviewed", "订单收到评价", "订单 #" + orderId + " 已收到用户评价");
        return orderRepository.findOrderById(orderId).orElseThrow();
    }

    public List<OrderStatusLog> orderTimeline(Long orderId, DemoUserPrincipal principal) {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (principal.role() == Role.CUSTOMER && !order.userId().equals(principal.id())) {
            throw new IllegalArgumentException("Order does not belong to current user");
        }
        return orderRepository.findStatusLogs(orderId);
    }

    public Map<String, Object> summary() {
        List<Order> orders = orderRepository.findAllOrders();
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

    public Map<String, Object> merchantDashboard(Long merchantId) {
        Long shopId = resolveShopIdForMerchant(merchantId);
        List<Order> orders = orderRepository.findOrdersByShopId(shopId);
        List<Product> products = catalogRepository.findProductsByShopId(shopId);
        BigDecimal gmv = orders.stream()
                .filter(order -> order.status() == OrderStatus.COMPLETED || order.status() == OrderStatus.DELIVERING)
                .map(Order::payableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "orderCount", orders.size(),
                "pendingCount", orders.stream().filter(order -> order.status() == OrderStatus.PAID_WAITING_MERCHANT).count(),
                "deliveringCount", orders.stream().filter(order -> order.status() == OrderStatus.DELIVERING).count(),
                "completedCount", orders.stream().filter(order -> order.status() == OrderStatus.COMPLETED).count(),
                "gmv", gmv,
                "topProducts", products.stream()
                        .sorted(Comparator.comparing(Product::monthlySales).reversed())
                        .limit(5)
                        .toList());
    }

    private void notifyMerchant(Long shopId, String event, String title, String content) {
        userRepository.findByRole(Role.MERCHANT).stream()
                .filter(user -> shopId.equals(user.shopId()))
                .findFirst()
                .ifPresent(merchant -> streamService.notifyUser(Role.MERCHANT, merchant.id(), event, title, content));
    }

    private void notifyAllRiders(String event, String title, String content) {
        userRepository.findByRole(Role.RIDER).stream()
                .sorted(Comparator.comparing(User::id))
                .forEach(rider -> streamService.notifyUser(Role.RIDER, rider.id(), event, title, content));
    }

    private Long resolveShopIdForMerchant(Long merchantId) {
        return userRepository.findById(merchantId)
                .map(User::shopId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
    }

    private void validateMerchantOwnership(Order order, DemoUserPrincipal principal) {
        if (!order.shopId().equals(resolveShopIdForMerchant(principal.id()))) {
            throw new IllegalArgumentException("Order does not belong to current merchant");
        }
    }

    private void validateRiderCanTakeMoreOrders(Long riderId) {
        var profile = userRepository.findRiderProfile(riderId)
                .orElseThrow(() -> new IllegalStateException("Rider profile is missing"));
        if (!Boolean.TRUE.equals(profile.online())) {
            throw new IllegalStateException("Rider must be online before taking orders");
        }
        long activeDeliveries = orderRepository.findOrdersByRiderId(riderId).stream()
                .filter(order -> order.status() == OrderStatus.DELIVERING)
                .count();
        if (activeDeliveries >= profile.capacity()) {
            throw new IllegalStateException("Rider capacity reached");
        }
    }

    private void restoreOrderStock(Order order) {
        order.items().forEach(item -> catalogRepository.restoreStock(item.productId(), item.quantity()));
    }

    private void ensureStatus(Order order, OrderStatus expected, String message) {
        if (order.status() != expected) {
            throw new IllegalStateException(message);
        }
    }
}
