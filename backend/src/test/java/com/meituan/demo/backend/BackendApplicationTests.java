package com.meituan.demo.backend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import com.meituan.demo.backend.service.StreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StreamService streamService;

    @Test
    void customerCanLoginAndListShops() throws Exception {
        mockMvc.perform(post("/api/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"customer1001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("demo-customer-1001"));

        mockMvc.perform(get("/api/shops")
                        .header("Authorization", "Bearer demo-customer-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void riderCanViewAvailableOrders() throws Exception {
        mockMvc.perform(get("/api/rider/orders/available")
                        .header("Authorization", "Bearer demo-rider-3001"))
                .andExpect(status().isOk());
    }

    @Test
    void customerCanCreatePayAndReadOrderTimeline() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer demo-customer-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":5001,"quantity":1}
                                """))
                .andExpect(status().isOk());

        String orderBody = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer demo-customer-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"couponId":6101,"addressId":6001}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = orderBody.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer demo-customer-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentChannel":"MOCK_PAY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID_WAITING_MERCHANT"));

        mockMvc.perform(get("/api/orders/" + orderId + "/timeline")
                        .header("Authorization", "Bearer demo-customer-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$[1].status").value("PAID_WAITING_MERCHANT"));
    }

    @Test
    void adminMembershipRulesRequiresRole() throws Exception {
        mockMvc.perform(get("/api/admin/membership/rules"))
                .andExpect(status().isForbidden());
    }

    @Test
    void payingDeliveredOrderReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/orders/9002/pay")
                        .header("Authorization", "Bearer demo-customer-1002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentChannel":"MOCK_PAY"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only pending payment orders can be paid"));
    }

    @Test
    void streamNotificationsIgnoreClosedEmitters() {
        DemoUserPrincipal principal = new DemoUserPrincipal(1001L, "customer-1001", Role.CUSTOMER);
        var emitter = streamService.subscribe(principal);
        emitter.completeWithError(new RuntimeException("simulate disconnect"));

        assertDoesNotThrow(() ->
                streamService.notifyUser(Role.CUSTOMER, 1001L, "payment.succeeded", "支付成功", "订单已支付"));
    }
}
