package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Product;
import com.meituan.demo.backend.model.DomainModels.Shop;
import com.meituan.demo.backend.persistence.PersistenceEntities.ProductEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.SearchHistoryEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.ShopEntity;
import com.meituan.demo.backend.persistence.mapper.CatalogMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

    private final CatalogMapper catalogMapper;

    public CatalogRepository(CatalogMapper catalogMapper) {
        this.catalogMapper = catalogMapper;
    }

    public List<Shop> findAllShops() {
        return catalogMapper.findAllShops().stream().map(this::toShop).toList();
    }

    public Optional<Shop> findShopById(Long shopId) {
        return Optional.ofNullable(catalogMapper.findShopById(shopId)).map(this::toShop);
    }

    public Optional<ShopEntity> findShopEntityByMerchantId(Long merchantId) {
        return Optional.ofNullable(catalogMapper.findShopByMerchantId(merchantId));
    }

    public List<Shop> findSimilarShops(String category, Long excludedShopId, int limit) {
        return catalogMapper.findSimilarShops(category, excludedShopId, limit).stream().map(this::toShop).toList();
    }

    public List<Product> findProductsByShopId(Long shopId) {
        return catalogMapper.findProductsByShopId(shopId).stream().map(this::toProduct).toList();
    }

    public List<Product> searchProducts(String keyword, int limit) {
        return catalogMapper.searchProducts(keyword, limit).stream().map(this::toProduct).toList();
    }

    public List<Product> findTopProducts(int limit) {
        return catalogMapper.findTopProducts(limit).stream().map(this::toProduct).toList();
    }

    public int countShops() {
        return Optional.ofNullable(catalogMapper.countShops()).orElse(0);
    }

    public int countProducts() {
        return Optional.ofNullable(catalogMapper.countProducts()).orElse(0);
    }

    public Map<Long, Product> findProductsByIds(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return catalogMapper.findProductsByIds(productIds).stream()
                .map(this::toProduct)
                .collect(Collectors.toMap(Product::id, product -> product));
    }

    public Shop createShop(
            Long merchantId,
            String name,
            String category,
            BigDecimal deliveryFee,
            int deliveryMinutes,
            BigDecimal averagePrice,
            List<String> tags,
            String announcement,
            String status,
            List<String> serviceModes,
            BigDecimal minOrderAmount) {
        catalogMapper.insertShop(
                merchantId,
                name,
                category,
                new BigDecimal("5.00"),
                0,
                deliveryFee,
                deliveryMinutes,
                averagePrice,
                1.0D,
                String.join(",", tags),
                announcement,
                status,
                String.join(",", serviceModes),
                minOrderAmount.toPlainString(),
                Instant.now());
        return findShopById(catalogMapper.lastInsertId()).orElseThrow();
    }

    public void updateShop(
            Long shopId,
            String name,
            String category,
            BigDecimal deliveryFee,
            int deliveryMinutes,
            BigDecimal averagePrice,
            List<String> tags,
            String announcement,
            String status,
            List<String> serviceModes,
            BigDecimal minOrderAmount) {
        catalogMapper.updateShop(
                shopId,
                name,
                category,
                deliveryFee,
                deliveryMinutes,
                averagePrice,
                String.join(",", tags),
                announcement,
                status,
                String.join(",", serviceModes),
                minOrderAmount.toPlainString());
    }

    public Product createProduct(
            Long shopId,
            String name,
            String category,
            BigDecimal price,
            BigDecimal originalPrice,
            int stock,
            String description,
            boolean enabled) {
        catalogMapper.insertProduct(shopId, name, category, price, originalPrice, stock, 0, description, enabled, Instant.now());
        Long productId = catalogMapper.lastInsertId();
        return catalogMapper.findProductsByIds(List.of(productId)).stream().findFirst().map(this::toProduct).orElseThrow();
    }

    public boolean reserveStock(Long productId, int quantity) {
        return catalogMapper.reserveStock(productId, quantity) > 0;
    }

    public void restoreStock(Long productId, int quantity) {
        catalogMapper.restoreStock(productId, quantity);
    }

    public void updateProduct(
            Long productId,
            Long shopId,
            String name,
            String category,
            BigDecimal price,
            BigDecimal originalPrice,
            int stock,
            String description,
            boolean enabled) {
        catalogMapper.updateProduct(productId, shopId, name, category, price, originalPrice, stock, description, enabled);
    }

    public void deleteProduct(Long productId, Long shopId) {
        catalogMapper.deleteProduct(productId, shopId);
    }

    public List<Long> findFavoriteShopIds(Long userId) {
        return catalogMapper.findFavoriteShops(userId).stream().map(entity -> entity.shopId()).toList();
    }

    public void addFavoriteShop(Long userId, Long shopId) {
        catalogMapper.addFavoriteShop(userId, shopId, Instant.now());
    }

    public void removeFavoriteShop(Long userId, Long shopId) {
        catalogMapper.removeFavoriteShop(userId, shopId);
    }

    public List<String> searchHistory(Long userId, int limit) {
        return catalogMapper.findSearchHistory(userId, limit).stream().map(SearchHistoryEntity::keyword).toList();
    }

    public void recordSearchHistory(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        catalogMapper.addSearchHistory(userId, keyword.trim(), Instant.now());
    }

    private Shop toShop(ShopEntity entity) {
        return new Shop(
                entity.id(),
                entity.merchantId(),
                entity.name(),
                entity.category(),
                entity.score(),
                entity.monthlySales(),
                entity.deliveryFee(),
                entity.deliveryMinutes(),
                entity.averagePrice(),
                entity.distanceKm(),
                parseCsv(entity.tags()),
                entity.announcement(),
                entity.status(),
                parseCsv(entity.serviceModes()),
                entity.minOrderAmount() == null ? BigDecimal.ZERO : new BigDecimal(entity.minOrderAmount()));
    }

    private Product toProduct(ProductEntity entity) {
        return new Product(
                entity.id(),
                entity.shopId(),
                entity.name(),
                entity.category(),
                entity.price(),
                entity.originalPrice(),
                entity.stock(),
                entity.monthlySales(),
                entity.description(),
                Boolean.TRUE.equals(entity.enabled()));
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }
}
