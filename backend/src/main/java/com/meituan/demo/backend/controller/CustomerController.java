package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.model.ApiModels.AddCartItemRequest;
import com.meituan.demo.backend.model.ApiModels.AddressUpsertRequest;
import com.meituan.demo.backend.model.ApiModels.CreateConversationRequest;
import com.meituan.demo.backend.model.ApiModels.CreateMessageRequest;
import com.meituan.demo.backend.model.ApiModels.CreateOrderRequest;
import com.meituan.demo.backend.model.ApiModels.ReviewOrderRequest;
import com.meituan.demo.backend.model.ApiModels.ToggleFavoriteResponse;
import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.Message;
import com.meituan.demo.backend.model.DomainModels.Order;
import com.meituan.demo.backend.security.SecuritySupport;
import com.meituan.demo.backend.service.CatalogService;
import com.meituan.demo.backend.service.ChatService;
import com.meituan.demo.backend.service.MarketingService;
import com.meituan.demo.backend.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final SecuritySupport securitySupport;
    private final CatalogService catalogService;
    private final OrderService orderService;
    private final MarketingService marketingService;
    private final ChatService chatService;

    public CustomerController(
            SecuritySupport securitySupport,
            CatalogService catalogService,
            OrderService orderService,
            MarketingService marketingService,
            ChatService chatService) {
        this.securitySupport = securitySupport;
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.marketingService = marketingService;
        this.chatService = chatService;
    }

    @GetMapping("/shops")
    public List<?> shops(@RequestParam(required = false) String keyword, @RequestParam(required = false) String sort) {
        return catalogService.listShops(keyword, sort);
    }

    @GetMapping("/shops/{shopId}")
    public Map<String, Object> shopDetail(@PathVariable Long shopId) {
        return catalogService.shopDetail(shopId);
    }

    @GetMapping("/search")
    public Object search(@RequestParam(required = false) String q, @RequestParam(required = false) String sort) {
        return catalogService.search(securitySupport.currentUser().id(), q, sort);
    }

    @GetMapping("/search/history")
    public Object searchHistory() {
        return catalogService.searchHistory(securitySupport.currentUser().id());
    }

    @GetMapping("/recommendations/home")
    public Object recommendations() {
        return catalogService.homeRecommendations(securitySupport.currentUser());
    }

    @GetMapping("/favorites")
    public Object favorites() {
        return catalogService.favoriteShops(securitySupport.currentUser().id());
    }

    @PostMapping("/favorites/{shopId}")
    public ToggleFavoriteResponse favorite(@PathVariable Long shopId) {
        return new ToggleFavoriteResponse(catalogService.toggleFavoriteShop(securitySupport.currentUser().id(), shopId, true));
    }

    @DeleteMapping("/favorites/{shopId}")
    public ToggleFavoriteResponse unfavorite(@PathVariable Long shopId) {
        return new ToggleFavoriteResponse(catalogService.toggleFavoriteShop(securitySupport.currentUser().id(), shopId, false));
    }

    @GetMapping("/cart")
    public Object cart() {
        return orderService.getCart(securitySupport.currentUser().id());
    }

    @PostMapping("/cart/items")
    public Object addCart(@Valid @RequestBody AddCartItemRequest request) {
        return orderService.addCartItem(securitySupport.currentUser().id(), request);
    }

    @DeleteMapping("/cart")
    public void clearCart() {
        orderService.clearCart(securitySupport.currentUser().id());
    }

    @GetMapping("/addresses")
    public Object addresses() {
        return orderService.addresses(securitySupport.currentUser().id());
    }

    @PostMapping("/addresses")
    public Object createAddress(@Valid @RequestBody AddressUpsertRequest request) {
        return orderService.createAddress(securitySupport.currentUser().id(), request);
    }

    @PutMapping("/addresses/{addressId}")
    public void updateAddress(@PathVariable Long addressId, @Valid @RequestBody AddressUpsertRequest request) {
        orderService.updateAddress(securitySupport.currentUser().id(), addressId, request);
    }

    @DeleteMapping("/addresses/{addressId}")
    public void deleteAddress(@PathVariable Long addressId) {
        orderService.deleteAddress(securitySupport.currentUser().id(), addressId);
    }

    @PostMapping("/orders")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(securitySupport.currentUser(), request);
    }

    @PostMapping("/orders/{orderId}/pay")
    public Order payOrder(@PathVariable Long orderId) {
        return orderService.payOrder(orderId, securitySupport.currentUser());
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Order cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId, securitySupport.currentUser());
    }

    @PostMapping("/orders/{orderId}/review")
    public Order reviewOrder(@PathVariable Long orderId, @Valid @RequestBody ReviewOrderRequest request) {
        return orderService.reviewOrder(orderId, securitySupport.currentUser(), request);
    }

    @GetMapping("/orders")
    public List<Order> orders() {
        return orderService.ordersForPrincipal(securitySupport.currentUser());
    }

    @GetMapping("/orders/{orderId}/timeline")
    public Object orderTimeline(@PathVariable Long orderId) {
        return orderService.orderTimeline(orderId, securitySupport.currentUser());
    }

    @GetMapping("/coupons")
    public Object coupons() {
        return marketingService.couponsForUser(securitySupport.currentUser().id());
    }

    @GetMapping("/coupon-center")
    public Object couponCenter() {
        return marketingService.allCoupons();
    }

    @PostMapping("/coupons/{couponId}/claim")
    public Object claimCoupon(@PathVariable Long couponId) {
        return marketingService.claimCoupon(securitySupport.currentUser().id(), couponId);
    }

    @GetMapping("/membership")
    public Object membership() {
        return marketingService.membershipForUser(securitySupport.currentUser().id());
    }

    @GetMapping("/chat/conversations")
    public List<Conversation> conversations() {
        return chatService.conversationsForUser(securitySupport.currentUser());
    }

    @PostMapping("/chat/conversations")
    public Conversation createConversation(@Valid @RequestBody CreateConversationRequest request) {
        return chatService.createConversation(securitySupport.currentUser(), request);
    }

    @GetMapping("/chat/messages")
    public List<Message> messages(@RequestParam Long conversationId) {
        return chatService.messages(conversationId);
    }

    @PostMapping("/chat/messages")
    public Message createMessage(@Valid @RequestBody CreateMessageRequest request) {
        return chatService.createMessage(securitySupport.currentUser(), request);
    }
}
