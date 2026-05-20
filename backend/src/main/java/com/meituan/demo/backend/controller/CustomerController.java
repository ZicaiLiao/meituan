package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.model.ApiModels.AddCartItemRequest;
import com.meituan.demo.backend.model.ApiModels.CreateConversationRequest;
import com.meituan.demo.backend.model.ApiModels.CreateMessageRequest;
import com.meituan.demo.backend.model.ApiModels.CreateOrderRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
        return catalogService.search(q, sort);
    }

    @GetMapping("/recommendations/home")
    public Object recommendations() {
        return catalogService.homeRecommendations(securitySupport.currentUser());
    }

    @GetMapping("/cart")
    public Object cart() {
        return orderService.getCart(securitySupport.currentUser().id());
    }

    @PostMapping("/cart/items")
    public Object addCart(@Valid @RequestBody AddCartItemRequest request) {
        return orderService.addCartItem(securitySupport.currentUser().id(), request);
    }

    @GetMapping("/addresses")
    public Object addresses() {
        return orderService.addresses(securitySupport.currentUser().id());
    }

    @PostMapping("/orders")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(securitySupport.currentUser(), request);
    }

    @PostMapping("/orders/{orderId}/pay")
    public Order payOrder(@PathVariable Long orderId) {
        return orderService.payOrder(orderId, securitySupport.currentUser());
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
