package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.security.SecuritySupport;
import com.meituan.demo.backend.service.StreamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StreamController {

    private final StreamService streamService;
    private final SecuritySupport securitySupport;

    public StreamController(StreamService streamService, SecuritySupport securitySupport) {
        this.streamService = streamService;
        this.securitySupport = securitySupport;
    }

    @GetMapping("/api/stream/events")
    @PreAuthorize("hasRole('CUSTOMER')")
    public SseEmitter customerEvents() {
        return streamService.subscribe(securitySupport.currentUser());
    }

    @GetMapping("/api/merchant/stream/events")
    @PreAuthorize("hasRole('MERCHANT')")
    public SseEmitter merchantEvents() {
        return streamService.subscribe(securitySupport.currentUser());
    }

    @GetMapping("/api/rider/stream/events")
    @PreAuthorize("hasRole('RIDER')")
    public SseEmitter riderEvents() {
        return streamService.subscribe(securitySupport.currentUser());
    }
}

