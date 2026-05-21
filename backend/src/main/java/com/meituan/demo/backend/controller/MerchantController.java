package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.repository.UserRepository;
import com.meituan.demo.backend.security.SecuritySupport;
import com.meituan.demo.backend.service.CatalogService;
import com.meituan.demo.backend.service.ChatService;
import com.meituan.demo.backend.service.MarketingService;
import com.meituan.demo.backend.service.OrderService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/products")
    public Object products() {
        return catalogService.shopDetail(currentMerchantShopId()).get("products");
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
