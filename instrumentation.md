# Observability Instrumentation Requirements (Grafana Alloy + OpenTelemetry)

## 1. Purpose
Define the required instrumentation and telemetry pipeline for the POS demo application using OpenTelemetry instrumentation and Grafana Alloy, with Grafana Cloud as the backend.

## 2. Scope
In scope:
- Application traces, metrics, and logs for the Java/Spring Boot POS service.
- Container-level telemetry routing via Grafana Alloy.
- Export of telemetry to Grafana Cloud.
- Correlation between traces and logs.

Out of scope:
- Full production hardening for multi-region or high-scale operation.
- Advanced SLO governance and long-term capacity planning.
- End-user analytics beyond application observability.

## 3. Architecture Requirements
- The observability pipeline shall use Grafana Alloy as the in-stack telemetry collector/processor.
- The POS application shall emit OTLP telemetry to Grafana Alloy over the Docker network.
- Grafana Alloy shall export telemetry to Grafana Cloud OTLP endpoints.
- The solution shall not require a separate OpenTelemetry Collector service when Grafana Alloy is present.

## 4. Instrumentation Strategy Requirements

### 4.1 Base Instrumentation
- The Java application shall be instrumented using the OpenTelemetry Java agent as the default mechanism.
- The Java agent shall provide automatic instrumentation for:
  - Incoming HTTP requests (Spring Web).
  - Outbound MongoDB calls.
  - JVM/runtime telemetry.
- The implementation should avoid invasive code changes for baseline observability.

### 4.2 Semantic Identity and Resource Attributes
- All telemetry shall include stable resource attributes at minimum:
  - `service.name` = `pos-demo-app`
  - `service.namespace` = configurable (recommended: `pos-demo`)
  - `service.version` = image tag or git SHA
  - `deployment.environment` = `dev`, `staging`, or `prod`
- Host/container metadata should be included when available.

### 4.3 Manual Business Instrumentation
- After baseline auto-instrumentation is active, the application should add targeted business telemetry:
  - Counter: `pos_transactions_total` with labels such as `status` and `payment_method`.
  - Histogram: `pos_transaction_amount`.
  - Optional span events/attributes for transaction lifecycle milestones.
- Manual instrumentation shall not include sensitive cardholder or personal data.

## 5. Grafana Alloy Requirements

### 5.1 Receivers
- Grafana Alloy shall expose OTLP gRPC (`4317`) and OTLP HTTP (`4318`) receivers for application telemetry input.
- Grafana Alloy shall support log ingestion from the dedicated transaction logfile path mounted from the application container volume.

### 5.2 Processing
- Grafana Alloy shall batch telemetry before export.
- Grafana Alloy should apply memory-limiting and retry safeguards.
- Grafana Alloy should support optional attribute redaction/filtering for sensitive fields.

### 5.3 Export
- Grafana Alloy shall export traces, metrics, and logs to Grafana Cloud using OTLP exporters.
- Grafana Cloud credentials shall be supplied via environment variables or secrets, not hard-coded in source files.

## 6. Logging and Correlation Requirements
- Application logs shall include trace context (trace ID and span ID) when possible.
- The dedicated transaction logfile shall be ingested through Grafana Alloy and forwarded to Grafana Cloud logs.
- Log entries should include key business fields already emitted by the application (transaction ID, timestamp, cashier ID, total amount, payment method, status).
- Log shipping configuration should preserve parsable structure and timestamps.

## 7. Configuration Requirements
- Observability configuration shall be externalized through environment variables.
- At minimum, the following configuration values shall be supported:
  - OTLP endpoint from app to Alloy (recommended: `http://alloy:4318`).
  - Grafana Cloud OTLP endpoint(s).
  - Grafana Cloud authentication credentials/token.
  - Service environment (`deployment.environment`).
- Configuration defaults should support local Docker Compose execution with minimal setup.

## 8. Security and Data Handling Requirements
- Secrets (API keys, tokens) shall not be committed to version control.
- Telemetry shall exclude sensitive payload values wherever feasible.
- If request/response body capture is enabled, it shall be explicitly scoped and reviewed for data sensitivity.

## 9. Runtime and Deployment Requirements
- Docker Compose shall define an `alloy` service in the local stack.
- The application service shall depend on the `alloy` service for telemetry routing.
- The application shall continue serving requests if Alloy is temporarily unavailable; telemetry loss is acceptable over request failure.

## 10. Verification Requirements
- The team shall verify that at least one request to `POST /api/transactions` creates:
  - A trace in Grafana Cloud containing HTTP and MongoDB spans.
  - Application metrics in Grafana Cloud (JVM and request metrics).
  - Correlated log entries containing trace identifiers.
- Verification shall be executable in local Docker Compose environment.

## 11. Rollout Requirements
- Rollout shall occur in three phases:
  - Phase 1: Enable Java agent + Alloy + Grafana Cloud export for traces/metrics.
  - Phase 2: Add logfile ingestion and trace-log correlation.
  - Phase 3: Add manual business metrics and targeted span enrichment.
- Each phase shall include a verification checklist before moving to the next phase.

## 12. Acceptance Criteria
- The stack runs locally with Docker Compose including Grafana Alloy.
- The POS application emits telemetry to Alloy without breaking transaction functionality.
- Grafana Cloud displays traces, metrics, and logs for the POS service.
- At least one end-to-end transaction can be traced from API request through MongoDB write with correlated logs.
- No credentials are stored in tracked source files.
