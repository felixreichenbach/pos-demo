# POS Demo Application

A simple Point of Sale (POS) demo built with Spring Boot, MongoDB, and Docker.

The application provides:
- A browser-based POS UI for entering purchase transactions.
- A backend API that validates and processes transactions.
- Transaction logging to MongoDB.
- Transaction logging to a separate dedicated logfile on the POS application system.

## Tech Stack
- Java 21
- Spring Boot 3
- MongoDB
- Docker and Docker Compose
- JUnit 5 and Mockito

## Application Flow
1. A purchase transaction is submitted from the UI (or API).
2. The backend validates input and verifies total amount.
3. On success, the transaction is logged to MongoDB.
4. The same transaction is appended to a dedicated transaction logfile.
5. The API returns a success response.

## Prerequisites
- Docker
- Docker Compose (available via `docker compose`)
- Optional for local non-container run:
  - Java 21+
  - Maven 3.9+

## Environment Configuration
The project includes an environment template file: `.env.example`.

Create a local `.env` from it and adjust values as needed for your machine:

```bash
cp .env.example .env
```

Available variables:
- `APP_PORT`
- `MONGODB_URI`
- `MONGODB_COLLECTION`
- `TRANSACTION_LOG_FILE`

Notes:
- For Docker Compose, `MONGODB_URI` should typically use the service hostname (`mongo`), for example `mongodb://mongo:27017/pos_demo`.
- For local non-container run, `MONGODB_URI` is usually `mongodb://localhost:27017/pos_demo`.

## Bring Up the Full Stack (Recommended)
From the project root, run:

```bash
docker compose up --build
```

This starts:
- `pos-app` on port `8080`
- `mongo` on port `27017`

Stop the stack:

```bash
docker compose down
```

Stop and also remove volumes (clears MongoDB and logfile volume data):

```bash
docker compose down -v
```

## Run Purchase Transactions

### Option A: Use the POS UI
1. Open `http://localhost:8080` in your browser.
2. Fill in cashier, items, and payment method.
3. Verify the total amount is auto-calculated from line items.
4. Click **Submit Transaction**.
5. Check the on-screen status and API response panel.

### Option B: Use the API Directly
Send a transaction with `curl`:

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "cashierId": "cashier-01",
    "items": [
      {
        "productName": "Coffee",
        "quantity": 2,
        "unitPrice": 3.50
      }
    ],
    "paymentMethod": "CARD"
  }'
```

Expected success response:
- HTTP `201 Created`
- JSON with `transactionId`, `timestamp`, and `status: "LOGGED"`

## Verify Transaction Logging

### 1. Verify MongoDB Log Entry
Open a Mongo shell inside the Mongo container:

```bash
docker compose exec mongo mongosh
```

Then run:

```javascript
use pos_demo
db.transaction_logs.find().pretty()
```

### 2. Verify Dedicated Transaction Logfile
Display logfile contents from the app container:

```bash
docker compose exec pos-app sh -c "cat /var/log/pos/transactions.log"
```

Each line includes fields such as timestamp, transactionId, cashierId, totalAmount, paymentMethod, and status.

## Local Development Run (Without Docker)
If you want to run only the Spring Boot app locally:

1. Ensure MongoDB is reachable at `mongodb://localhost:27017/pos_demo` (or override env vars).
2. Start the app:

```bash
mvn spring-boot:run
```

Useful environment variables:
- `APP_PORT` (default: `8080`)
- `MONGODB_URI` (default: `mongodb://localhost:27017/pos_demo`)
- `MONGODB_COLLECTION` (default: `transaction_logs`)
- `TRANSACTION_LOG_FILE` (default: `./logs/transactions.log`)

The same keys are provided in `.env.example`.

## Troubleshooting

### 1. App does not start with Docker
Symptoms:
- `pos-app` exits immediately.

Checks:

```bash
docker compose ps
docker compose logs pos-app
```

Fixes:
- Rebuild images: `docker compose up --build`
- If needed, clean state and retry: `docker compose down -v && docker compose up --build`

### 2. MongoDB connection errors
Symptoms:
- API returns `503 Transaction logging failed`.
- Logs show Mongo connection refused.

Checks:

```bash
docker compose ps
docker compose logs mongo
docker compose logs pos-app
```

Fixes:
- Ensure both services are running.
- Confirm app uses `MONGODB_URI=mongodb://mongo:27017/pos_demo` in Compose.
- Restart stack: `docker compose down && docker compose up --build`

### 3. API returns 400 Bad Request
Symptoms:
- Response body has:
  - `Invalid transaction request`

Fixes:
- Ensure required fields are present: `cashierId`, `items`, `paymentMethod`.
- Ensure each item has valid `productName`, `quantity > 0`, `unitPrice > 0`.
- Ensure at least one item is present.

### 4. Dedicated transaction logfile looks empty
Checks:

```bash
docker compose exec pos-app sh -c "ls -l /var/log/pos && cat /var/log/pos/transactions.log"
```

Fixes:
- Submit at least one successful transaction first.
- Confirm `TRANSACTION_LOG_FILE=/var/log/pos/transactions.log` is set for `pos-app`.
- Verify the `pos_logs` volume is mounted in Compose.

### 5. Port 8080 already in use
Symptoms:
- Compose fails to start `pos-app` due to port binding error.

Fixes:
- Stop the process using port `8080`, or
- Change host port mapping in `docker-compose.yml` from `8080:8080` to another free port (for example `8081:8080`) and open `http://localhost:8081`.

## Run Tests

```bash
mvn test
```

Current tests include:
- Unit tests for core transaction logging behavior.
- Integration tests for API success, validation failures, and backend failure handling.
