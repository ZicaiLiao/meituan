package com.meituan.demo.backend.service;

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

    public SearchResult search(String keyword, String sort) {
        List<Shop> shops = rankShops(keyword, sort);
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        List<Product> products = normalized.isBlank()
                ? catalogRepository.findTopProducts(8)
                : catalogRepository.searchProducts(keyword, 8);
        List<String> suggestions = normalized.isBlank()
                ? List.of("轻食", "烧烤", "奶茶")
                : List.of(keyword + " 套餐", keyword + " 热门", keyword + " 附近");
        return new SearchResult(shops, products, sort == null ? "composite" : sort, suggestions);
    }

    public RecommendationBundle homeRecommendations(DemoUserPrincipal principal) {
        List<Shop> all = new ArrayList<>(catalogRepository.findAllShops());
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

        return catalogRepository.findAllShops().stream()
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
