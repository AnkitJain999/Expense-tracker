# Expense Tracker

## Requirements

- Docker Desktop

## Start

```bash
docker compose up --build
```

Detached mode

```bash
docker compose up -d --build
```

---

## Open

Frontend

http://localhost:3000

Backend

http://localhost:8080

MailHog

http://localhost:8025

---

## Stop

```bash
docker compose down
```

---

## Remove Database

```bash
docker compose down -v
```

---

## Rebuild

```bash
docker compose build
```

---

## View Logs

```bash
docker compose logs -f
```

Backend

```bash
docker compose logs -f backend
```

Frontend

```bash
docker compose logs -f frontend
```

Database

```bash
docker compose logs -f postgres
```

MailHog

```bash
docker compose logs -f mailhog
```