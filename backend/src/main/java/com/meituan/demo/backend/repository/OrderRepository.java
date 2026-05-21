package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Address;
import com.meituan.demo.backend.model.DomainModels.CartItem;
import com.meituan.demo.backend.model.DomainModels.Order;
import com.meituan.demo.backend.model.DomainModels.OrderLine;
import com.meituan.demo.backend.model.DomainModels.OrderStatus;
import com.meituan.demo.backend.model.DomainModels.OrderStatusLog;
import com.meituan.demo.backend.persistence.PersistenceEntities.AddressEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.CartEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.OrderEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.OrderItemEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.OrderStatusLogEntity;
import com.meituan.demo.backend.persistence.mapper.OrderMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final OrderMapper orderMapper;

    public OrderRepository(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public List<CartItem> findCartByUserId(Long userId) {
        return orderMapper.findCartByUserId(userId).stream()
                .map(entity -> new CartItem(entity.productId(), entity.quantity()))
                .toList();
    }

    public void upsertCartItem(Long userId, Long productId, int quantity) {
        orderMapper.upsertCartItem(userId, productId, quantity);
    }

    public void clearCart(Long userId) {
        orderMapper.clearCart(userId);
    }

    public List<Address> findAddressesByUserId(Long userId) {
        return orderMapper.findAddressesByUserId(userId).stream().map(this::toAddress).toList();
    }

    public Address createAddress(Long userId, String label, String detail, String contactName, String phone, boolean isDefault) {
        if (isDefault) {
            orderMapper.clearDefaultAddress(userId);
        }
        orderMapper.insertAddress(userId, label, detail, contactName, phone, isDefault);
        Long addressId = orderMapper.lastInsertId();
        return orderMapper.findAddressesByUserId(userId).stream()
                .filter(entity -> entity.id().equals(addressId))
                .findFirst()
                .map(this::toAddress)
                .orElseThrow();
    }

    public void updateAddress(Long addressId, Long userId, String label, String detail, String contactName, String phone, boolean isDefault) {
        if (isDefault) {
            orderMapper.clearDefaultAddress(userId);
        }
        orderMapper.updateAddress(addressId, userId, label, detail, contactName, phone, isDefault);
    }

    public void deleteAddress(Long addressId, Long userId) {
        orderMapper.deleteAddress(addressId, userId);
    }

    public Order createOrder(
            Long userId,
            Long shopId,
            Long couponId,
            Long addressId,
            OrderStatus status,
            BigDecimal totalAmount,
            BigDecimal payableAmount,
            List<OrderLine> items) {
        Instant now = Instant.now();
        orderMapper.insertOrder(userId, shopId, null, status.name(), totalAmount, payableAmount, couponId, addressId, now, null, null, null);
        Long orderId = orderMapper.lastInsertId();
        for (OrderLine item : items) {
            orderMapper.insertOrderItem(orderId, item.productId(), item.productName(), item.quantity(), item.unitPrice());
        }
        return findOrderById(orderId).orElseThrow();
    }

    public Optional<Order> findOrderById(Long orderId) {
        OrderEntity entity = orderMapper.findOrderById(orderId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(withItems(List.of(entity)).getFirst());
    }

    public List<Order> findOrdersByUserId(Long userId) {
        return withItems(orderMapper.findOrdersByUserId(userId));
    }

    public List<Order> findOrdersByShopId(Long shopId) {
        return withItems(orderMapper.findOrdersByShopId(shopId));
    }

    public List<Order> findOrdersByRiderId(Long riderId) {
        return withItems(orderMapper.findOrdersByRiderId(riderId));
    }

    public List<Order> findAvailableOrders() {
        return withItems(orderMapper.findAvailableOrders());
    }

    public List<Order> findAllOrders() {
        return withItems(orderMapper.findAllOrders());
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status, Long riderId) {
        orderMapper.updateOrderStatus(orderId, status.name(), riderId);
        return findOrderById(orderId).orElseThrow();
    }

    public void appendStatusLog(Long orderId, OrderStatus status, String note) {
        orderMapper.insertStatusLog(orderId, status.name(), note, Instant.now());
    }

    public List<OrderStatusLog> findStatusLogs(Long orderId) {
        return orderMapper.findStatusLogs(orderId).stream()
                .map(entity -> new OrderStatusLog(entity.orderId(), OrderStatus.valueOf(entity.status()), entity.note(), entity.createdAt()))
                .toList();
    }

    public void submitReview(Long orderId, int reviewScore, String reviewContent) {
        orderMapper.submitReview(orderId, reviewScore, reviewContent, Instant.now());
    }

    private List<Order> withItems(List<OrderEntity> orderEntities) {
        if (orderEntities.isEmpty()) {
            return List.of();
        }
        Map<Long, List<OrderLine>> itemMap = findOrderItems(orderEntities.stream().map(OrderEntity::id).toList());
        return orderEntities.stream()
                .map(entity -> new Order(
                        entity.id(),
                        entity.userId(),
                        entity.shopId(),
                        entity.riderId(),
                        OrderStatus.valueOf(entity.status()),
                        entity.totalAmount(),
                        entity.payableAmount(),
                entity.couponId(),
                entity.addressId(),
                entity.createdAt(),
                itemMap.getOrDefault(entity.id(), List.of()),
                entity.reviewScore(),
                entity.reviewContent(),
                entity.reviewedAt()))
                .toList();
    }

    private Map<Long, List<OrderLine>> findOrderItems(Collection<Long> orderIds) {
        return orderMapper.findOrderItems(orderIds).stream()
                .collect(Collectors.groupingBy(
                        OrderItemEntity::orderId,
                        Collectors.mapping(
                                entity -> new OrderLine(
                                        entity.productId(),
                                        entity.productName(),
                                        entity.quantity(),
                                        entity.unitPrice()),
                                Collectors.toList())));
    }

    private Address toAddress(AddressEntity entity) {
        return new Address(
                entity.id(),
                entity.userId(),
                entity.label(),
                entity.detail(),
                entity.contactName(),
                entity.phone(),
                Boolean.TRUE.equals(entity.isDefault()));
    }
}
