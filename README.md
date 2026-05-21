# Meituan Delivery Platform Demo

This repository contains a runnable first-version scaffold for a Meituan-like food delivery platform.
The backend now boots against a real MySQL datasource and auto-creates or patches the required tables on startup.

## Structure

- `backend/`: Spring Boot business application
- `netty-gateway/`: Netty-based long-connection gateway
- `apps/user-h5/`: customer-facing mobile web app
- `apps/merchant/`: merchant console
- `apps/rider/`: rider console
- `apps/admin/`: admin console
- `deploy/`: Docker Compose, SQL schema, protocol docs

## Quick Start

1. Start infrastructure with Docker Compose.
2. Run the backend and Netty gateway with Maven.
3. Install frontend dependencies and run the Vite apps.
4. The backend will seed sample business data into MySQL only when the target tables are empty.

Detailed steps live in `deploy/README.md`.

## Demo Credentials

The backend uses seeded demo accounts and token-based role simulation.

- Customer: `customer1001` -> `demo-customer-1001`
- Merchant: `merchant2001` -> `demo-merchant-2001`
- Rider: `rider3001` -> `demo-rider-3001`
- Admin: `admin4001` -> `demo-admin-4001`
- Support: `support5001` -> `demo-support-5001`

## Runtime Notes

- Backend datasource env vars: `MEITUAN_DB_URL`, `MEITUAN_DB_USERNAME`, `MEITUAN_DB_PASSWORD`
- Redis env vars: `MEITUAN_REDIS_HOST`, `MEITUAN_REDIS_PORT`
- RocketMQ env var: `MEITUAN_ROCKETMQ_NAME_SERVER`
- Web frontends still use browser-side HTTP/SSE to talk to the backend; Netty is reserved for long-connection gateway traffic.
