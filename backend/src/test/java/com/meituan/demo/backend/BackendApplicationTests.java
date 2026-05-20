package com.meituan.demo.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
