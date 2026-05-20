package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.DomainModels.Product;
import com.meituan.demo.backend.model.DomainModels.RecommendationBundle;
import com.meituan.demo.backend.model.DomainModels.SearchResult;
import com.meituan.demo.backend.model.DomainModels.Shop;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final DemoDataStore dataStore;

    public CatalogService(DemoDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Shop> listShops(String keyword, String sort) {
        return rankShops(keyword, sort);
    }

    public Map<String, Object> shopDetail(Long shopId) {
        Shop shop = dataStore.shops().get(shopId);
        List<Product> products = dataStore.products().values().stream()
                .filter(product -> product.shopId().equals(shopId))
                .sorted(Comparator.comparing(Product::monthlySales).reversed())
                .toList();
        List<Shop> similarShops = dataStore.shops().values().stream()
                .filter(candidate -> !candidate.id().equals(shopId))
                .filter(candidate -> candidate.category().equals(shop.category()))
                .limit(2)
                .toList();
        return Map.of(
                "shop", shop,
                "products", products,
                "similarShops", similarShops);
    }

    public SearchResult search(String keyword, String sort) {
        List<Shop> shops = rankShops(keyword, sort);
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        List<Product> products = dataStore.products().values().stream()
                .filter(product -> normalized.isBlank()
                        || product.name().toLowerCase(Locale.ROOT).contains(normalized)
                        || product.category().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(Product::monthlySales).reversed())
                .limit(8)
                .toList();
        List<String> suggestions = normalized.isBlank()
                ? List.of("轻食", "烧烤", "奶茶")
                : List.of(keyword + " 套餐", keyword + " 热门", keyword + " 附近");
        return new SearchResult(shops, products, sort == null ? "composite" : sort, suggestions);
    }

    public RecommendationBundle homeRecommendations(DemoUserPrincipal principal) {
        List<Shop> all = new ArrayList<>(dataStore.shops().values());
        List<Shop> guess = all.stream()
                .sorted(Comparator.comparing(this::compositeRecommendationScore).reversed())
                .limit(3)
                .toList();
        List<Shop> nearby = all.stream()
                .sorted(Comparator.comparing(Shop::distanceKm))
                .limit(3)
                .toList();
        List<Shop> similar = all.stream()
                .filter(shop -> !"轻食沙拉".equals(shop.category()) || principal.id().equals(1001L))
                .sorted(Comparator.comparing(Shop::score).reversed())
                .limit(3)
                .toList();
        return new RecommendationBundle(guess, nearby, similar);
    }

    private List<Shop> rankShops(String keyword, String sort) {
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        Comparator<Shop> comparator = switch (sort == null ? "composite" : sort) {
            case "distance" -> Comparator.comparing(Shop::distanceKm);
            case "sales" -> Comparator.comparing(Shop::monthlySales).reversed();
            case "score" -> Comparator.comparing(Shop::score).reversed();
            default -> Comparator.comparing(this::compositeSearchScore).reversed();
        };

        return dataStore.shops().values().stream()
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

    private BigDecimal compositeRecommendationScore(Shop shop) {
        return shop.score()
                .multiply(BigDecimal.valueOf(25))
                .add(BigDecimal.valueOf(shop.monthlySales() / 120.0))
                .add(BigDecimal.valueOf(shop.tags().contains("回头客多") ? 5 : 2));
    }
}

