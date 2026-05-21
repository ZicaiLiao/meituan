# Deployment Guide

## Infrastructure

Run from the repository root:

```bash
docker compose -f deploy/docker-compose.yml up -d --build

The backend container is configured to use the real Docker MySQL, Redis, and RocketMQ services through environment variables.
On startup, the backend auto-creates missing tables, patches legacy schema columns, and seeds sample business data only when a target table is empty.
```

## Backend

```bash
mvn -pl backend spring-boot:run
```

For local host execution outside Docker, the default datasource points to:

- MySQL: `localhost:3306/meituan_demo`
- Redis: `localhost:6379`
- RocketMQ NameServer: `localhost:9876`

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
