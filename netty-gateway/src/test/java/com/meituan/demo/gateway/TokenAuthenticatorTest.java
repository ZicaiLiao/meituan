package com.meituan.demo.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meituan.demo.gateway.core.TokenAuthenticator;
import org.junit.jupiter.api.Test;

class TokenAuthenticatorTest {

    @Test
    void parsesDemoToken() {
        TokenAuthenticator authenticator = new TokenAuthenticator();
        var session = authenticator.authenticate("demo-customer-1001");
        assertTrue(session.isPresent());
        assertEquals("CUSTOMER", session.get().role());
        assertEquals(1001L, session.get().userId());
    }
}
