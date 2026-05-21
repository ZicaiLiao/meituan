package com.meituan.demo.backend.persistence.mapper;

import com.meituan.demo.backend.persistence.PersistenceEntities.FavoriteShopEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.ProductEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.SearchHistoryEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.ShopEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CatalogMapper {

    @Select("""
            SELECT id, merchant_id, name, category, score, monthly_sales, delivery_fee, delivery_minutes, average_price, distance_km, tags, announcement, status, service_modes, min_order_amount, created_at
            FROM shops
            ORDER BY id
            """)
    List<ShopEntity> findAllShops();

    @Select("""
            SELECT id, merchant_id, name, category, score, monthly_sales, delivery_fee, delivery_minutes, average_price, distance_km, tags, announcement, status, service_modes, min_order_amount, created_at
            FROM shops
            WHERE id = #{shopId}
            """)
    ShopEntity findShopById(@Param("shopId") Long shopId);

    @Select("""
            SELECT id, merchant_id, name, category, score, monthly_sales, delivery_fee, delivery_minutes, average_price, distance_km, tags, announcement, status, service_modes, min_order_amount, created_at
            FROM shops
            WHERE merchant_id = #{merchantId}
            LIMIT 1
            """)
    ShopEntity findShopByMerchantId(@Param("merchantId") Long merchantId);

    @Select("""
            SELECT id, merchant_id, name, category, score, monthly_sales, delivery_fee, delivery_minutes, average_price, distance_km, tags, announcement, status, service_modes, min_order_amount, created_at
            FROM shops
            WHERE category = #{category}
              AND id <> #{excludedShopId}
            ORDER BY score DESC
            LIMIT #{limit}
            """)
    List<ShopEntity> findSimilarShops(
            @Param("category") String category,
            @Param("excludedShopId") Long excludedShopId,
            @Param("limit") int limit);

    @Select("""
            SELECT id, shop_id, name, category, price, original_price, stock, monthly_sales, description, enabled, created_at
            FROM products
            WHERE shop_id = #{shopId}
            ORDER BY enabled DESC, monthly_sales DESC, id
            """)
    List<ProductEntity> findProductsByShopId(@Param("shopId") Long shopId);

    @Select("""
            SELECT id, shop_id, name, category, price, original_price, stock, monthly_sales, description, enabled, created_at
            FROM products
            WHERE LOWER(name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
               OR LOWER(category) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
            ORDER BY monthly_sales DESC, id
            LIMIT #{limit}
            """)
    List<ProductEntity> searchProducts(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("""
            SELECT id, shop_id, name, category, price, original_price, stock, monthly_sales, description, enabled, created_at
            FROM products
            ORDER BY monthly_sales DESC, id
            LIMIT #{limit}
            """)
    List<ProductEntity> findTopProducts(@Param("limit") int limit);

    @Select("""
            <script>
            SELECT id, shop_id, name, category, price, original_price, stock, monthly_sales, description, enabled, created_at
            FROM products
            WHERE id IN
            <foreach collection='productIds' item='productId' open='(' separator=',' close=')'>
                #{productId}
            </foreach>
            </script>
            """)
    List<ProductEntity> findProductsByIds(@Param("productIds") Collection<Long> productIds);

    @Select("SELECT COUNT(*) FROM shops")
    Integer countShops();

    @Select("SELECT COUNT(*) FROM products")
    Integer countProducts();

    @Insert("""
            INSERT INTO shops
            (merchant_id, name, category, score, monthly_sales, delivery_fee, delivery_minutes, average_price, distance_km, tags, announcement, status, service_modes, min_order_amount, created_at)
            VALUES
            (#{merchantId}, #{name}, #{category}, #{score}, #{monthlySales}, #{deliveryFee}, #{deliveryMinutes}, #{averagePrice}, #{distanceKm}, #{tags}, #{announcement}, #{status}, #{serviceModes}, #{minOrderAmount}, #{createdAt})
            """)
    int insertShop(
            @Param("merchantId") Long merchantId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("score") BigDecimal score,
            @Param("monthlySales") int monthlySales,
            @Param("deliveryFee") BigDecimal deliveryFee,
            @Param("deliveryMinutes") int deliveryMinutes,
            @Param("averagePrice") BigDecimal averagePrice,
            @Param("distanceKm") double distanceKm,
            @Param("tags") String tags,
            @Param("announcement") String announcement,
            @Param("status") String status,
            @Param("serviceModes") String serviceModes,
            @Param("minOrderAmount") String minOrderAmount,
            @Param("createdAt") Instant createdAt);

    @Update("""
            UPDATE shops
            SET name = #{name},
                category = #{category},
                delivery_fee = #{deliveryFee},
                delivery_minutes = #{deliveryMinutes},
                average_price = #{averagePrice},
                tags = #{tags},
                announcement = #{announcement},
                status = #{status},
                service_modes = #{serviceModes},
                min_order_amount = #{minOrderAmount}
            WHERE id = #{shopId}
            """)
    int updateShop(
            @Param("shopId") Long shopId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("deliveryFee") BigDecimal deliveryFee,
            @Param("deliveryMinutes") int deliveryMinutes,
            @Param("averagePrice") BigDecimal averagePrice,
            @Param("tags") String tags,
            @Param("announcement") String announcement,
            @Param("status") String status,
            @Param("serviceModes") String serviceModes,
            @Param("minOrderAmount") String minOrderAmount);

    @Insert("""
            INSERT INTO products
            (shop_id, name, category, price, original_price, stock, monthly_sales, description, enabled, created_at)
            VALUES
            (#{shopId}, #{name}, #{category}, #{price}, #{originalPrice}, #{stock}, #{monthlySales}, #{description}, #{enabled}, #{createdAt})
            """)
    int insertProduct(
            @Param("shopId") Long shopId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("price") BigDecimal price,
            @Param("originalPrice") BigDecimal originalPrice,
            @Param("stock") int stock,
            @Param("monthlySales") int monthlySales,
            @Param("description") String description,
            @Param("enabled") boolean enabled,
            @Param("createdAt") Instant createdAt);

    @Update("""
            UPDATE products
            SET name = #{name},
                category = #{category},
                price = #{price},
                original_price = #{originalPrice},
                stock = #{stock},
                description = #{description},
                enabled = #{enabled}
            WHERE id = #{productId}
              AND shop_id = #{shopId}
            """)
    int updateProduct(
            @Param("productId") Long productId,
            @Param("shopId") Long shopId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("price") BigDecimal price,
            @Param("originalPrice") BigDecimal originalPrice,
            @Param("stock") int stock,
            @Param("description") String description,
            @Param("enabled") boolean enabled);

    @Delete("DELETE FROM products WHERE id = #{productId} AND shop_id = #{shopId}")
    int deleteProduct(@Param("productId") Long productId, @Param("shopId") Long shopId);

    @Update("""
            UPDATE products
            SET stock = stock - #{quantity},
                monthly_sales = monthly_sales + #{quantity}
            WHERE id = #{productId}
              AND enabled = TRUE
              AND stock >= #{quantity}
            """)
    int reserveStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Select("""
            SELECT user_id, shop_id, created_at
            FROM favorite_shops
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            """)
    List<FavoriteShopEntity> findFavoriteShops(@Param("userId") Long userId);

    @Insert("""
            INSERT IGNORE INTO favorite_shops (user_id, shop_id, created_at)
            VALUES (#{userId}, #{shopId}, #{createdAt})
            """)
    int addFavoriteShop(@Param("userId") Long userId, @Param("shopId") Long shopId, @Param("createdAt") Instant createdAt);

    @Delete("DELETE FROM favorite_shops WHERE user_id = #{userId} AND shop_id = #{shopId}")
    int removeFavoriteShop(@Param("userId") Long userId, @Param("shopId") Long shopId);

    @Select("""
            SELECT id, user_id, keyword, created_at
            FROM search_history
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<SearchHistoryEntity> findSearchHistory(@Param("userId") Long userId, @Param("limit") int limit);

    @Insert("""
            INSERT INTO search_history (user_id, keyword, created_at)
            VALUES (#{userId}, #{keyword}, #{createdAt})
            """)
    int addSearchHistory(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("createdAt") Instant createdAt);
}
