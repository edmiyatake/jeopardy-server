# Jeopardy Clone

Real-time multiplayer Jeopardy clone built with Spring Boot and React.

## Prerequisites

- Docker & Docker Compose
- Java 21+
- Maven 3.9+
- Node 20+ (for the frontend, later)

## Getting Started

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

Verify both are healthy:
```bash
docker-compose ps
```

### 2. Run the server

```bash
cd server
mvn spring-boot:run
```

Flyway will automatically run `V1__init_schema.sql` and `V2__seed_questions.sql` on first boot.

The server starts on `http://localhost:8080`.

### 3. Verify the DB

```bash
docker exec -it jeopardy-postgres psql -U jeopardy_user -d jeopardy -c "\dt"
```

You should see: `games`, `game_players`, `game_questions`, `players`, `questions`, `flyway_schema_history`.

### 4. Verify Redis

```bash
docker exec -it jeopardy-redis redis-cli ping
# → PONG
```

## Project Structure

```
jeopardy/
├── docker-compose.yml
└── server/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/com/jeopardy/
            │   └── JeopardyApplication.java
            └── resources/
                ├── application.yml
                └── db/migration/
                    ├── V1__init_schema.sql
                    └── V2__seed_questions.sql
```

## Next steps

Phase 2 adds JPA entities and repositories on top of the schema created here.
