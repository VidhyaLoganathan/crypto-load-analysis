# Cypher Card Load Analytics Backend

## Overview

This Spring Boot application powers the **Cypher Card Load Analytics** take-home assignment. It provides:

- **Load Volume Endpoints**
    - Daily, weekly, and monthly token load volumes (in USD) for the master wallet.
- **Wallet Analysis**
    - Top 10 counterparties by transaction count (wallets, contracts, known protocols/CEX).
- **Background Sync**
    - Scheduled jobs to fetch and persist missing daily and monthly transfer data from the blockchain via Alchemy.

## Features

- **REST API** using Spring Web MVC
- **Persistence** with Spring Data JPA and MySQL
- **Data Sync** with Spring Data Scheduler and MySQL
- **Exception Handling** via `@ControllerAdvice`
- **Dockerized** for streamlined local and containerized development

## Tech Stack

- **Backend**: Java 17, Spring Boot, Maven
- **Frontend**: React, TypeScript
- **Build Tools**: Maven, npm
- **Containerization**: Dockerfile, Docker Compose

---

## How to Run the Project

---
### Option 1: Run via Docker Compose

#### Prerequisite
1. Install brew (optional , skip if brew is already installed )
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```
2. Install docker with brew ( this can be skipped if docker is already installed )
```bash
brew install --cask docker
```
follow the below step to start docker, if installing docker for the first time.
```bash
open /Applications/Docker.app
```

3. Download the zip file
```bash

```
4. Go to the directory
```bash
/<path_to_downloaded_folder>/AllocationTool
```
---

#### 1. Build and Start
Using docker version 2
```bash
docker compose build --no-cache
docker compose up --build
```

#### 2. Access Application

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend: [http://localhost:8080](http://localhost:8080)

#### 3. Stop Containers

```bash
docker-compose down
```
---
### Option 2: Run Manually (Local Dev Mode)

#### Prerequisites

- Java 17+
- Maven 3.6+
- Node.js and npm
- Docker

#### 1. Run Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### 2. Run Frontend

```bash
cd frontend
npm install
npm start
```
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
---

## Run Tests (Backend)

```bash
cd backend
mvn test
```
## **API Endpoints**

   | Method | Path                               | Description                                        |
      | ------ |------------------------------------|----------------------------------------------------|
   | GET    | `/api/load-volume/daily`           | Query params: `startDate`, `endDate` (YYYY-MM-DD)  |
   | GET    | `/api/load-volume/weekly`          | Query params: `startDate`, `endDate`               |
   | GET    | `/api/load-volume/monthly`         | Query params: `startDate`, `endDate`               |
   | POST   | `/api/v1/wallet/analyze`           | JSON body: `{ "walletAddress": "...", "limit": 10 }` |
   | GET    | `/api/v1/wallet/analyze/{address}` | Path param: wallet address                         |
   | GET    | `/api/health`                      | Health check                                       |
   | GET    | `/api/info`                        | API information                                    |
   | GET    | `/api/debug`                       | Debug diagnostics                                        |



   **Example:**
   ```bash
   curl "http://localhost:8080/api/load-volume/daily?startDate=2025-01-01&endDate=2025-05-19"
   ```

## Project Structure

```
backend/
├── Dockerfile
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/cypher/cardload/
│   │   │   ├── config/            Configuration & constants
│   │   │   ├── controller/        REST controllers
│   │   │   ├── service/           Business logic & sync
│   │   │   ├── dto/               Request/response models
│   │   │   ├── repository/        JPA repository 
│   │   │   ├── simulator/         Mock data
│   │   │   ├── model/             DB model
│   │   │   ├── sync/              USD load volume daily sync job
│   │   │   └── exception/         Custom exceptions & handlers
│   │   └── resources/
│   │       ├── application.properties
│   └── test/                      basic tests
└── target/                        Build outputs
```

## Architecture
### Computing USD Load Volume

1. **Date Range:** Client supplies `startDate`/`endDate`.
2. **Load or Fetch Transfers:** Check the DB; backfill missing days via `BlockchainService`.
3. **Price Lookups:** Query `TokenPriceService` for each token’s USD price at its transfer timestamp.
4. **Value Conversion:** Multiply token amounts by their historical USD prices.
5. **Aggregation:** Sum daily values, then roll up into weekly or monthly totals.
6. **Exposure:** Return the aggregated data via the `/api/load-volume` endpoints.

### Wallet Top Counterparty Analysis

1. **Input:** Wallet address & optional `limit` (default 10).
2. **Fetch Transfers:** Query your DB (with backfill via `BlockchainService`) for all in/out transfers of the given wallet.
3. **Identify Counterparties:** Extract the “other” address from each transfer.
4. **Aggregate Counts:** Group transfers by counterparty address and count them.
5. **Protocol Detection:** Use `ContractDetectionService` to tag known protocols/CEX.
6. **Classification:** Label each counterparty as a wallet, contract, or protocol/CEX.
7. **Sort & Limit:** Sort by transaction count (desc) and take the top N.
8. **Output:** Return the list via `/api/v1/wallet/analyze` with counts and classifications.

### Challenges Faced

- **Data Completeness:** Back-filling missing days/months and ensuring no gaps in historical transfers.
- **API Rate Limits & Reliability:** Handling Alchemy/quoter throttling, retries, and transient errors.
- **Timestamp Alignment:** Matching on-chain block timestamps to the closest available USD price points.
- **Precision & Conversion:** Safely converting big-integer token amounts (wei/decimals) into `BigDecimal` USD values.
- **Aggregation Logic:** Correctly grouping by ISO weeks and calendar months without off-by-one errors.
- **Counterparty Classification:** Detecting protocols/CEX and distinguishing contracts vs. EOA wallets with limited metadata.
- **Performance Optimization:** Writing efficient DB queries and caching price lookups for large date ranges.
- **Idempotent Syncs:** Designing scheduled backfill jobs to be restart-safe and avoid duplicate data.

### Architectural Tradeoffs

- **DB-First vs On-Demand Fetch**
    - Chose to read from the database and backfill missing data in batches to minimize API calls and ensure repeatable results, at the cost of increased storage and sync complexity.

- **Synchronous vs Scheduled Sync**
    - Core endpoints rely on DB queries for low latency; a separate scheduler handles back-fills to avoid blocking user requests, trading immediate completeness for faster response times.

- **Price Accuracy vs Rate Limits**
    - Fetch exact historical prices per transfer timestamp for precision, but cache and batch requests to stay within API limits, accepting slight staleness for efficiency.

- **Aggregation Granularity**
    - Compute daily volumes first, then roll up to weekly/monthly, simplifying logic and ensuring consistency, even though it requires extra grouping steps.

- **JPA Abstraction vs SQL Tuning**
    - Used Spring Data JPA for developer productivity and maintainability, accepting that some complex queries may need manual optimization or native SQL for performance-critical paths.

- **Strict Consistency vs High Availability**
    - Designed sync jobs to be idempotent and restart-safe, favoring eventual consistency in historical data over locking or blocking operations that could hurt API availability.

- **Simple Monolith vs Microservices**
    - Consolidated services into a single Spring Boot app for simplicity in a take-home context, recognizing that a microservices split would improve scalability but add deployment complexity.

### Iterative Development & Testing

1. **Understand Requirements**
2. **Develop Backend** for USD load volume
3. **Integrate Simulator Data** to validate volume endpoints
4. **Build Wallet Analysis Service**
5. **Test End-to-End** and ensure core features work
6. **Add Caching** to minimize external network calls
7. **Implement Frontend** for both load volume and wallet analysis
8. **Fetch from BaseNet** and fix integration errors
9. **Add Fallbacks** to handle rate-limit failures
10. **Create Sync Service** to preload historical data
11. **Containerize** the application with Docker
12. **Seed Database** for faster data retrieval

---
## Author
Vidhya Loganathan <
vidhya7apr@gmail.com >

---