package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.model.ApiModels.ProductUpsertRequest;
import com.meituan.demo.backend.model.ApiModels.ShopUpsertRequest;
import com.meituan.demo.backend.repository.UserRepository;
import com.meituan.demo.backend.security.SecuritySupport;
import com.meituan.demo.backend.service.CatalogService;
import com.meituan.demo.backend.service.ChatService;
import com.meituan.demo.backend.service.MarketingService;
import com.meituan.demo.backend.service.OrderService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantController {

    private final SecuritySupport securitySupport;
    private final CatalogService catalogService;
    private final OrderService orderService;
    private final MarketingService marketingService;
    private final ChatService chatService;
    private final UserRepository userRepository;

    public MerchantController(
            SecuritySupport securitySupport,
            CatalogService catalogService,
            OrderService orderService,
            MarketingService marketingService,
            ChatService chatService,
            UserRepository userRepository) {
        this.securitySupport = securitySupport;
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.marketingService = marketingService;
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @GetMapping("/shop")
    public Map<String, Object> shop() {
        return catalogService.shopDetail(currentMerchantShopId());
    }

    @PutMapping("/shop")
    public Map<String, Object> updateShop(@Valid @RequestBody ShopUpsertRequest request) {
        return catalogService.updateMerchantShop(currentMerchantShopId(), request);
    }

    @GetMapping("/dashboard")
    public Object dashboard() {
        return orderService.merchantDashboard(securitySupport.currentUser().id());
    }

    @GetMapping("/products")
    public Object products() {
        return catalogService.shopDetail(currentMerchantShopId()).get("products");
    }

    @PostMapping("/products")
    public Object createProduct(@Valid @RequestBody ProductUpsertRequest request) {
        return catalogService.createMerchantProduct(currentMerchantShopId(), request);
    }

    @PutMapping("/products/{productId}")
    public Object updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductUpsertRequest request) {
        return catalogService.updateMerchantProduct(currentMerchantShopId(), productId, request);
    }

    @DeleteMapping("/products/{productId}")
    public void deleteProduct(@PathVariable Long productId) {
        catalogService.deleteMerchantProduct(currentMerchantShopId(), productId);
    }

    @GetMapping("/orders")
    public Object orders() {
        return orderService.ordersForPrincipal(securitySupport.currentUser());
    }

    @PostMapping("/orders/{orderId}/accept")
    public Object accept(@PathVariable Long orderId) {
        return orderService.merchantAccept(orderId, securitySupport.currentUser());
    }

    @PostMapping("/orders/{orderId}/reject")
    public Object reject(@PathVariable Long orderId) {
        return orderService.merchantReject(orderId, securitySupport.currentUser());
    }

    @GetMapping("/coupons")
    public Object coupons() {
        return marketingService.couponsForShop(currentMerchantShopId());
    }

    @GetMapping("/chat/conversations")
    public Object conversations() {
        return chatService.conversationsForUser(securitySupport.currentUser());
    }

    private Long currentMerchantShopId() {
        return userRepository.findById(securitySupport.currentUser().id())
                .map(user -> user.shopId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
    }
}
