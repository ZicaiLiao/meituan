package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.service.AdminService;
import com.meituan.demo.backend.service.MarketingService;
import com.meituan.demo.backend.service.DemoDataStore;
import com.meituan.demo.backend.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final MarketingService marketingService;
    private final DemoDataStore dataStore;

    public AdminController(
            AdminService adminService,
            OrderService orderService,
            MarketingService marketingService,
            DemoDataStore dataStore) {
        this.adminService = adminService;
        this.orderService = orderService;
        this.marketingService = marketingService;
        this.dataStore = dataStore;
    }

    @GetMapping("/merchants")
    public Object merchants() {
        return adminService.merchantsAndShops();
    }

    @GetMapping("/riders")
    public Object riders() {
        return adminService.riders();
    }

    @GetMapping("/orders")
    public Object orders() {
        return orderService.summary();
    }

    @GetMapping("/coupons")
    public Object coupons() {
        return dataStore.coupons().values();
    }

    @GetMapping("/membership/rules")
    public Object membershipRules() {
        return marketingService.membershipRules();
    }

    @PostMapping("/search/rebuild")
    public Object rebuildSearch() {
        return adminService.rebuildSearch();
    }

    @GetMapping("/recommendation/configs")
    public Object recommendationConfigs() {
        return marketingService.recommendationConfig();
    }

    @GetMapping("/support/conversations")
    public Object supportConversations() {
        return adminService.supportConversations();
    }
}
