# Deployment Guide

## Infrastructure

Run from the repository root:

```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

## Backend

```bash
mvn -pl backend spring-boot:run
```

## Netty Gateway

```bash
mvn -pl netty-gateway -DskipTests package
java -cp netty-gateway/target/netty-gateway-0.1.0-SNAPSHOT.jar com.meituan.demo.gateway.GatewayApplication
```

## Frontends

Install and run each app:

```bash
cd apps/user-h5 && npm install && npm run dev
cd apps/merchant && npm install && npm run dev
cd apps/rider && npm install && npm run dev
cd apps/admin && npm install && npm run dev
```
