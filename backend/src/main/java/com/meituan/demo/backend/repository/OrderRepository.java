package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Address;
import com.meituan.demo.backend.model.DomainModels.CartItem;
import com.meituan.demo.backend.model.DomainModels.Order;
import com.meituan.demo.backend.model.DomainModels.OrderLine;
import com.meituan.demo.backend.model.DomainModels.OrderStatus;
import com.meituan.demo.backend.model.DomainModels.OrderStatusLog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CartItem> findCartByUserId(Long userId) {
        return jdbcTemplate.query("SELECT product_id, quantity FROM carts WHERE user_id = ? ORDER BY product_id",
                (rs, rowNum) -> new CartItem(rs.getLong("product_id"), rs.getInt("quantity")), userId);
    }

    public void upsertCartItem(Long userId, Long productId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO carts (user_id, product_id, quantity)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)
                """, userId, productId, quantity);
    }

    public void clearCart(Long userId) {
        jdbcTemplate.update("DELETE FROM carts WHERE user_id = ?", userId);
    }

    public List<Address> findAddressesByUserId(Long userId) {
        return jdbcTemplate.query("SELECT * FROM addresses WHERE user_id = ? ORDER BY id",
                (rs, rowNum) -> new Address(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("label"),
                        rs.getString("detail"),
                        rs.getString("contact_name"),
                        rs.getString("phone")),
                userId);
    }

    public Order createOrder(Long userId, Long shopId, Long couponId, Long addressId, OrderStatus status,
            java.math.BigDecimal totalAmount, java.math.BigDecimal payableAmount, List<OrderLine> items) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO orders
                    (user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[] {"id"});
            ps.setLong(1, userId);
            ps.setLong(2, shopId);
            ps.setObject(3, null);
            ps.setString(4, status.name());
            ps.setBigDecimal(5, totalAmount);
            ps.setBigDecimal(6, payableAmount);
            ps.setObject(7, couponId);
            ps.setObject(8, addressId);
            return ps;
        }, keyHolder);
        Number generatedKey = Optional.ofNullable(keyHolder.getKeys())
                .map(keys -> keys.get("id"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .orElseGet(keyHolder::getKey);
        Long orderId = generatedKey.longValue();
        for (OrderLine item : items) {
            jdbcTemplate.update("""
                    INSERT INTO order_items
                    (order_id, product_id, product_name, quantity, unit_price)
                    VALUES (?, ?, ?, ?, ?)
                    """, orderId, item.productId(), item.productName(), item.quantity(), item.unitPrice());
        }
        return findOrderById(orderId).orElseThrow();
    }

    public Optional<Order> findOrderById(Long orderId) {
        List<Order> orders = jdbcTemplate.query("SELECT * FROM orders WHERE id = ?", this::mapOrderRow, orderId);
        if (orders.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(withItems(orders).getFirst());
    }

    public List<Order> findOrdersByUserId(Long userId) {
        return withItems(jdbcTemplate.query("SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                this::mapOrderRow, userId));
    }

    public List<Order> findOrdersByShopId(Long shopId) {
        return withItems(jdbcTemplate.query("SELECT * FROM orders WHERE shop_id = ? ORDER BY created_at DESC, id DESC",
                this::mapOrderRow, shopId));
    }

    public List<Order> findOrdersByRiderId(Long riderId) {
        return withItems(jdbcTemplate.query("""
                SELECT * FROM orders
                WHERE rider_id = ?
                ORDER BY created_at DESC, id DESC
                """, this::mapOrderRow, riderId));
    }

    public List<Order> findAvailableOrders() {
        return withItems(jdbcTemplate.query("""
                SELECT * FROM orders
                WHERE status = 'RIDER_PENDING'
                ORDER BY created_at DESC, id DESC
                """, this::mapOrderRow));
    }

    public List<Order> findAllOrders() {
        return withItems(jdbcTemplate.query("SELECT * FROM orders ORDER BY created_at DESC, id DESC", this::mapOrderRow));
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status, Long riderId) {
        jdbcTemplate.update("UPDATE orders SET status = ?, rider_id = ? WHERE id = ?", status.name(), riderId, orderId);
        return findOrderById(orderId).orElseThrow();
    }

    public void appendStatusLog(Long orderId, OrderStatus status, String note) {
        jdbcTemplate.update("INSERT INTO order_status_logs (order_id, status, note) VALUES (?, ?, ?)",
                orderId, status.name(), note);
    }

    public List<OrderStatusLog> findStatusLogs(Long orderId) {
        return jdbcTemplate.query("""
                SELECT order_id, status, note, created_at
                FROM order_status_logs
                WHERE order_id = ?
                ORDER BY created_at, id
                """, (rs, rowNum) -> new OrderStatusLog(
                        rs.getLong("order_id"),
                        OrderStatus.valueOf(rs.getString("status")),
                        rs.getString("note"),
                        rs.getTimestamp("created_at").toInstant()), orderId);
    }

    private List<Order> withItems(List<Order> orders) {
        if (orders.isEmpty()) {
            return orders;
        }
        Map<Long, List<OrderLine>> itemMap = findOrderItems(orders.stream().map(Order::id).toList());
        return orders.stream()
                .map(order -> new Order(
                        order.id(),
                        order.userId(),
                        order.shopId(),
                        order.riderId(),
                        order.status(),
                        order.totalAmount(),
                        order.payableAmount(),
                        order.couponId(),
                        order.addressId(),
                        order.createdAt(),
                        itemMap.getOrDefault(order.id(), List.of())))
                .toList();
    }

    private Map<Long, List<OrderLine>> findOrderItems(Collection<Long> orderIds) {
        String placeholders = orderIds.stream().map(unused -> "?").collect(Collectors.joining(","));
        List<Object> params = orderIds.stream().map(Object.class::cast).toList();
        // Items are loaded in bulk to avoid N+1 reads when rendering order lists for customer, merchant, and rider views.
        return jdbcTemplate.query("""
                SELECT order_id, product_id, product_name, quantity, unit_price
                FROM order_items
                WHERE order_id IN (""" + placeholders + ") ORDER BY id",
                (rs, rowNum) -> Map.entry(rs.getLong("order_id"), new OrderLine(
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"))),
                params.toArray())
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private Order mapOrderRow(ResultSet rs, int rowNum) throws SQLException {
        return new Order(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("shop_id"),
                rs.getObject("rider_id", Long.class),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("payable_amount"),
                rs.getObject("coupon_id", Long.class),
                rs.getObject("address_id", Long.class),
                rs.getTimestamp("created_at").toInstant(),
                List.of());
    }
}
