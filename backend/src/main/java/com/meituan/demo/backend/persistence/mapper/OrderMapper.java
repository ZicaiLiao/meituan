package com.meituan.demo.backend.persistence.mapper;

import com.meituan.demo.backend.persistence.PersistenceEntities.AddressEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.CartEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.OrderEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.OrderItemEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.OrderStatusLogEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper {

    @Select("SELECT user_id, product_id, quantity FROM carts WHERE user_id = #{userId} ORDER BY product_id")
    List<CartEntity> findCartByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO carts (user_id, product_id, quantity)
            VALUES (#{userId}, #{productId}, #{quantity})
            ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)
            """)
    int upsertCartItem(@Param("userId") Long userId, @Param("productId") Long productId, @Param("quantity") int quantity);

    @Delete("DELETE FROM carts WHERE user_id = #{userId}")
    int clearCart(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, label, detail, contact_name, phone, is_default
            FROM addresses
            WHERE user_id = #{userId}
            ORDER BY is_default DESC, id
            """)
    List<AddressEntity> findAddressesByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO addresses (user_id, label, detail, contact_name, phone, is_default)
            VALUES (#{userId}, #{label}, #{detail}, #{contactName}, #{phone}, #{isDefault})
            """)
    int insertAddress(
            @Param("userId") Long userId,
            @Param("label") String label,
            @Param("detail") String detail,
            @Param("contactName") String contactName,
            @Param("phone") String phone,
            @Param("isDefault") boolean isDefault);

    @Update("""
            UPDATE addresses
            SET label = #{label},
                detail = #{detail},
                contact_name = #{contactName},
                phone = #{phone},
                is_default = #{isDefault}
            WHERE id = #{addressId}
              AND user_id = #{userId}
            """)
    int updateAddress(
            @Param("addressId") Long addressId,
            @Param("userId") Long userId,
            @Param("label") String label,
            @Param("detail") String detail,
            @Param("contactName") String contactName,
            @Param("phone") String phone,
            @Param("isDefault") boolean isDefault);

    @Delete("DELETE FROM addresses WHERE id = #{addressId} AND user_id = #{userId}")
    int deleteAddress(@Param("addressId") Long addressId, @Param("userId") Long userId);

    @Update("UPDATE addresses SET is_default = FALSE WHERE user_id = #{userId}")
    int clearDefaultAddress(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO orders
            (user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at)
            VALUES
            (#{userId}, #{shopId}, #{riderId}, #{status}, #{totalAmount}, #{payableAmount}, #{couponId}, #{addressId}, #{createdAt}, #{reviewScore}, #{reviewContent}, #{reviewedAt})
            """)
    int insertOrder(
            @Param("userId") Long userId,
            @Param("shopId") Long shopId,
            @Param("riderId") Long riderId,
            @Param("status") String status,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("payableAmount") BigDecimal payableAmount,
            @Param("couponId") Long couponId,
            @Param("addressId") Long addressId,
            @Param("createdAt") Instant createdAt,
            @Param("reviewScore") Integer reviewScore,
            @Param("reviewContent") String reviewContent,
            @Param("reviewedAt") Instant reviewedAt);

    @Insert("""
            INSERT INTO order_items
            (order_id, product_id, product_name, quantity, unit_price)
            VALUES (#{orderId}, #{productId}, #{productName}, #{quantity}, #{unitPrice})
            """)
    int insertOrderItem(
            @Param("orderId") Long orderId,
            @Param("productId") Long productId,
            @Param("productName") String productName,
            @Param("quantity") int quantity,
            @Param("unitPrice") BigDecimal unitPrice);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Select("""
            SELECT id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at
            FROM orders
            WHERE id = #{orderId}
            """)
    OrderEntity findOrderById(@Param("orderId") Long orderId);

    @Select("""
            SELECT id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at
            FROM orders
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<OrderEntity> findOrdersByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at
            FROM orders
            WHERE shop_id = #{shopId}
            ORDER BY created_at DESC, id DESC
            """)
    List<OrderEntity> findOrdersByShopId(@Param("shopId") Long shopId);

    @Select("""
            SELECT id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at
            FROM orders
            WHERE rider_id = #{riderId}
            ORDER BY created_at DESC, id DESC
            """)
    List<OrderEntity> findOrdersByRiderId(@Param("riderId") Long riderId);

    @Select("""
            SELECT id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at
            FROM orders
            WHERE status = 'RIDER_PENDING'
            ORDER BY created_at DESC, id DESC
            """)
    List<OrderEntity> findAvailableOrders();

    @Select("""
            SELECT id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at
            FROM orders
            ORDER BY created_at DESC, id DESC
            """)
    List<OrderEntity> findAllOrders();

    @Update("UPDATE orders SET status = #{status}, rider_id = #{riderId} WHERE id = #{orderId}")
    int updateOrderStatus(@Param("orderId") Long orderId, @Param("status") String status, @Param("riderId") Long riderId);

    @Insert("""
            INSERT INTO order_status_logs (order_id, status, note, created_at)
            VALUES (#{orderId}, #{status}, #{note}, #{createdAt})
            """)
    int insertStatusLog(
            @Param("orderId") Long orderId,
            @Param("status") String status,
            @Param("note") String note,
            @Param("createdAt") Instant createdAt);

    @Select("""
            SELECT id, order_id, status, note, created_at
            FROM order_status_logs
            WHERE order_id = #{orderId}
            ORDER BY created_at, id
            """)
    List<OrderStatusLogEntity> findStatusLogs(@Param("orderId") Long orderId);

    @Select("""
            <script>
            SELECT id, order_id, product_id, product_name, quantity, unit_price
            FROM order_items
            WHERE order_id IN
            <foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>
                #{orderId}
            </foreach>
            ORDER BY id
            </script>
            """)
    List<OrderItemEntity> findOrderItems(@Param("orderIds") Collection<Long> orderIds);

    @Update("""
            UPDATE orders
            SET review_score = #{reviewScore},
                review_content = #{reviewContent},
                reviewed_at = #{reviewedAt}
            WHERE id = #{orderId}
            """)
    int submitReview(
            @Param("orderId") Long orderId,
            @Param("reviewScore") int reviewScore,
            @Param("reviewContent") String reviewContent,
            @Param("reviewedAt") Instant reviewedAt);
}
