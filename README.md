# E-Auction System

Real-time online auction platform with REST APIs, WebSocket bidding, chat, wallet, and notifications.

## Quick Start

### Prerequisites

- Java 25
- Maven 3.9+ (or use `./mvnw`)
- MongoDB
- Docker (for integration and e2e tests via Testcontainers)

### Run backend

```bash
cd backend
./mvnw spring-boot:run
```

Provide configuration via environment variables or profile-specific YAML (`application-dev.yml`):

| Property | Description |
| -------- | ----------- |
| `spring.data.mongodb.uri` | MongoDB connection string |
| `app.jwt.secret` | JWT secret (min 32 chars for HS256) |
| `app.jwt.algorithm` | `HS256` |
| `aws.s3.bucket-name` | S3 bucket for media |
| `aws.s3.region` | AWS region |

Test profile defaults live in `backend/src/test/resources/application-test.yml`.

### Run tests

```bash
cd backend
./mvnw test                              # 316 pass, 6 skipped without Docker
./mvnw test -Dtest="**/unit/**"          # unit only (~310 tests)
./mvnw test -Dtest="**/*IntegrationTest"  # integration (Docker)
./mvnw test -Dtest="**/*E2ETest"         # e2e (Docker)
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Features

- User registration, JWT auth, refresh tokens (HTTP-only cookie)
- Product listing and auction room management
- Real-time bidding via STOMP WebSocket (`/ws`)
- Auto-bid engine and bid history
- In-room chat with typing indicators
- Wallet, deposits, withdrawals, transactions
- Push notifications over WebSocket
- Role-based access: `ADMIN`, `MANAGER`, `USER`

## Configuration

| Variable | Description | Default (test profile) |
| -------- | ----------- | ---------------------- |
| `server.port` | HTTP port | `8080` |
| `app.base-url` | Public API base URL | `http://localhost:8080` |
| `app.jwt.access-token-expiration-mins` | Access token TTL (minutes) | `60` |
| `app.jwt.refresh-token-expiration-days` | Refresh token TTL (days) | `7` |
| `app.cookie.name` | Refresh token cookie | `refresh_token` |
| `aws.s3.bucket-name` | Media bucket | `e-auction-test-bucket` (test) |
| `aws.s3.region` | AWS region | `ap-southeast-1` (test) |

## Documentation

- [Project Documentation](./PROJECT_DOCUMENTATION.md) — architecture, REST/WebSocket API, models
- [Agent Guide](./agents/AGENTS.md) — modules, events, testing plan
- [Coding Standards](./agents/SKILL.md) — clean code rules for agents

### API prefix

- REST: `/api/v1`
- WebSocket: `/ws` (SockJS + STOMP)

## Contributing

1. Fork and create a feature branch
2. Follow existing patterns (`@AuthInfo`, MapStruct, Lombok)
3. Use `JobExecutorTasks` for async work in services; mock it in unit tests via `JobExecutorTasksMockHelper`
4. Add or update unit/integration tests
5. Run `./mvnw test` before opening a PR

## License

MIT
