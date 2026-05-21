package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.ProductUpsertRequest;
import com.meituan.demo.backend.model.ApiModels.ShopUpsertRequest;
import com.meituan.demo.backend.model.DomainModels.Product;
import com.meituan.demo.backend.model.DomainModels.RecommendationBundle;
import com.meituan.demo.backend.model.DomainModels.SearchResult;
import com.meituan.demo.backend.model.DomainModels.Shop;
import com.meituan.demo.backend.repository.CatalogRepository;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<Shop> listShops(String keyword, String sort) {
        return rankShops(keyword, sort);
    }

    public Map<String, Object> shopDetail(Long shopId) {
        Shop shop = catalogRepository.findShopById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));
        List<Product> products = catalogRepository.findProductsByShopId(shopId);
        List<Shop> similarShops = catalogRepository.findSimilarShops(shop.category(), shopId, 2);
        return Map.of("shop", shop, "products", products, "similarShops", similarShops);
    }

    public SearchResult search(Long userId, String keyword, String sort) {
        if (userId != null && keyword != null && !keyword.isBlank()) {
            catalogRepository.recordSearchHistory(userId, keyword);
        }
        List<Shop> shops = rankShops(keyword, sort);
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        List<Product> products = normalized.isBlank()
                ? catalogRepository.findTopProducts(8)
                : catalogRepository.searchProducts(keyword, 8);
        List<String> suggestions = normalized.isBlank()
                ? List.of("轻食", "烧烤", "川味")
                : List.of(keyword + " 套餐", keyword + " 热门", keyword + " 附近");
        return new SearchResult(shops, products, sort == null ? "composite" : sort, suggestions);
    }

    public RecommendationBundle homeRecommendations(DemoUserPrincipal principal) {
        List<Shop> all = new ArrayList<>(catalogRepository.findAllShops());
        List<Long> favoriteShopIds = principal == null ? List.of() : catalogRepository.findFavoriteShopIds(principal.id());
        List<Shop> guess = all.stream()
                .sorted(Comparator.comparing(
                        (Shop shop) -> compositeRecommendationScore(shop, favoriteShopIds.contains(shop.id()))).reversed())
                .limit(4)
                .toList();
        List<Shop> nearby = all.stream()
                .sorted(Comparator.comparing(Shop::distanceKm))
                .limit(4)
                .toList();
        List<Shop> similar = all.stream()
                .sorted(Comparator.comparing(Shop::score).reversed())
                .limit(4)
                .toList();
        return new RecommendationBundle(guess, nearby, similar);
    }

    public List<Shop> favoriteShops(Long userId) {
        List<Long> favoriteIds = catalogRepository.findFavoriteShopIds(userId);
        return catalogRepository.findAllShops().stream().filter(shop -> favoriteIds.contains(shop.id())).toList();
    }

    @Transactional
    public boolean toggleFavoriteShop(Long userId, Long shopId, boolean favorite) {
        if (catalogRepository.findShopById(shopId).isEmpty()) {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
        if (favorite) {
            catalogRepository.addFavoriteShop(userId, shopId);
        } else {
            catalogRepository.removeFavoriteShop(userId, shopId);
        }
        return favorite;
    }

    public List<String> searchHistory(Long userId) {
        return catalogRepository.searchHistory(userId, 10);
    }

    @Transactional
    public Map<String, Object> updateMerchantShop(Long shopId, ShopUpsertRequest request) {
        catalogRepository.updateShop(
                shopId,
                request.name(),
                request.category(),
                request.deliveryFee(),
                request.deliveryMinutes(),
                request.averagePrice(),
                request.tags(),
                request.announcement(),
                request.status(),
                request.serviceModes(),
                request.minOrderAmount());
        return shopDetail(shopId);
    }

    @Transactional
    public Product createMerchantProduct(Long shopId, ProductUpsertRequest request) {
        return catalogRepository.createProduct(
                shopId,
                request.name(),
                request.category(),
                request.price(),
                request.originalPrice(),
                request.stock(),
                request.description(),
                request.enabled());
    }

    @Transactional
    public Product updateMerchantProduct(Long shopId, Long productId, ProductUpsertRequest request) {
        catalogRepository.updateProduct(
                productId,
                shopId,
                request.name(),
                request.category(),
                request.price(),
                request.originalPrice(),
                request.stock(),
                request.description(),
                request.enabled());
        return catalogRepository.findProductsByIds(List.of(productId)).get(productId);
    }

    @Transactional
    public void deleteMerchantProduct(Long shopId, Long productId) {
        catalogRepository.deleteProduct(productId, shopId);
    }

    private List<Shop> rankShops(String keyword, String sort) {
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        Comparator<Shop> comparator = switch (sort == null ? "composite" : sort) {
            case "distance" -> Comparator.comparing(Shop::distanceKm);
            case "sales" -> Comparator.comparing(Shop::monthlySales).reversed();
            case "score" -> Comparator.comparing(Shop::score).reversed();
            default -> Comparator.comparing(this::compositeSearchScore).reversed();
        };

        return catalogRepository.findAllShops().stream()
                .filter(shop -> "OPEN".equalsIgnoreCase(shop.status()))
                .filter(shop -> normalized.isBlank()
                        || shop.name().toLowerCase(Locale.ROOT).contains(normalized)
                        || shop.category().toLowerCase(Locale.ROOT).contains(normalized)
                        || shop.tags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(normalized)))
                .sorted(comparator)
                .toList();
    }

    private BigDecimal compositeSearchScore(Shop shop) {
        BigDecimal relevance = shop.score().multiply(BigDecimal.valueOf(20));
        BigDecimal sales = BigDecimal.valueOf(shop.monthlySales() / 100.0);
        BigDecimal distance = BigDecimal.valueOf(Math.max(0, 10 - shop.distanceKm()));
        BigDecimal activity = BigDecimal.valueOf(shop.tags().size());
        return relevance.add(sales).add(distance).add(activity);
    }

    private BigDecimal compositeRecommendationScore(Shop shop, boolean isFavorite) {
        BigDecimal favoriteBoost = isFavorite ? BigDecimal.valueOf(6) : BigDecimal.ZERO;
        return shop.score()
                .multiply(BigDecimal.valueOf(25))
                .add(BigDecimal.valueOf(shop.monthlySales() / 120.0))
                .add(BigDecimal.valueOf(shop.tags().contains("回头客多") ? 5 : 2))
                .add(favoriteBoost);
    }
}
