package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.model.ApiModels.RiderAvailabilityRequest;
import com.meituan.demo.backend.security.SecuritySupport;
import com.meituan.demo.backend.service.ChatService;
import com.meituan.demo.backend.service.OrderService;
import com.meituan.demo.backend.service.UserOpsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rider")
@PreAuthorize("hasRole('RIDER')")
public class RiderController {

    private final SecuritySupport securitySupport;
    private final OrderService orderService;
    private final ChatService chatService;
    private final UserOpsService userOpsService;

    public RiderController(
            SecuritySupport securitySupport,
            OrderService orderService,
            ChatService chatService,
            UserOpsService userOpsService) {
        this.securitySupport = securitySupport;
        this.orderService = orderService;
        this.chatService = chatService;
        this.userOpsService = userOpsService;
    }

    @GetMapping("/orders/available")
    public Object availableOrders() {
        return orderService.availableOrders();
    }

    @GetMapping("/orders/mine")
    public Object myOrders() {
        return orderService.ordersForPrincipal(securitySupport.currentUser());
    }

    @PostMapping("/orders/{orderId}/accept")
    public Object accept(@PathVariable Long orderId) {
        return orderService.riderAccept(orderId, securitySupport.currentUser());
    }

    @PostMapping("/orders/{orderId}/deliver")
    public Object deliver(@PathVariable Long orderId) {
        return orderService.riderDeliver(orderId, securitySupport.currentUser());
    }

    @GetMapping("/profile")
    public Object profile() {
        return userOpsService.riderProfile(securitySupport.currentUser().id());
    }

    @PostMapping("/profile/availability")
    public Object updateAvailability(@Valid @RequestBody RiderAvailabilityRequest request) {
        userOpsService.updateRiderAvailability(securitySupport.currentUser().id(), request);
        return userOpsService.riderProfile(securitySupport.currentUser().id());
    }

    @GetMapping("/chat/conversations")
    public Object conversations() {
        return chatService.conversationsForUser(securitySupport.currentUser());
    }
}
