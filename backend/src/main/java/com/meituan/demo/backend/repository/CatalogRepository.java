package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Product;
import com.meituan.demo.backend.model.DomainModels.Shop;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public CatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Shop> findAllShops() {
        return jdbcTemplate.query("SELECT * FROM shops ORDER BY id", this::mapShop);
    }

    public Optional<Shop> findShopById(Long shopId) {
        return jdbcTemplate.query("SELECT * FROM shops WHERE id = ?", this::mapShop, shopId).stream().findFirst();
    }

    public List<Shop> findSimilarShops(String category, Long excludedShopId, int limit) {
        return jdbcTemplate.query("SELECT * FROM shops WHERE category = ? AND id <> ? ORDER BY score DESC LIMIT ?",
                this::mapShop, category, excludedShopId, limit);
    }

    public List<Product> findProductsByShopId(Long shopId) {
        return jdbcTemplate.query("SELECT * FROM products WHERE shop_id = ? ORDER BY monthly_sales DESC, id",
                this::mapProduct, shopId);
    }

    public List<Product> searchProducts(String keyword, int limit) {
        String like = "%" + keyword + "%";
        return jdbcTemplate.query("""
                SELECT * FROM products
                WHERE LOWER(name) LIKE LOWER(?) OR LOWER(category) LIKE LOWER(?)
                ORDER BY monthly_sales DESC, id
                LIMIT ?
                """, this::mapProduct, like, like, limit);
    }

    public List<Product> findTopProducts(int limit) {
        return jdbcTemplate.query("SELECT * FROM products ORDER BY monthly_sales DESC, id LIMIT ?", this::mapProduct, limit);
    }

    public int countShops() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shops", Integer.class);
        return count == null ? 0 : count;
    }

    public int countProducts() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        return count == null ? 0 : count;
    }

    public Map<Long, Product> findProductsByIds(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = productIds.stream().map(unused -> "?").collect(Collectors.joining(","));
        List<Object> params = productIds.stream().map(Long.class::cast).map(Object.class::cast).toList();
        return jdbcTemplate.query("SELECT * FROM products WHERE id IN (" + placeholders + ")", this::mapProduct, params.toArray())
                .stream()
                .collect(Collectors.toMap(Product::id, product -> product));
    }

    private Shop mapShop(ResultSet rs, int rowNum) throws SQLException {
        return new Shop(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getBigDecimal("score"),
                rs.getInt("monthly_sales"),
                rs.getBigDecimal("delivery_fee"),
                rs.getInt("delivery_minutes"),
                rs.getBigDecimal("average_price"),
                rs.getDouble("distance_km"),
                parseCsv(rs.getString("tags")),
                rs.getString("announcement"));
    }

    private Product mapProduct(ResultSet rs, int rowNum) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getLong("shop_id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("original_price"),
                rs.getInt("stock"),
                rs.getInt("monthly_sales"),
                rs.getString("description"));
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }
}
