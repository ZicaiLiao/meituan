package com.meituan.demo.gateway;

import com.meituan.demo.gateway.core.GatewayServer;

public class GatewayApplication {

    public static void main(String[] args) throws Exception {
        GatewayServer server = new GatewayServer(9000);
        server.start();
    }
}

