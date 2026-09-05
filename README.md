# Banking System — Microservices

---

## Services Overview

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | Single entry point, Rate limiting |
| account-service | 8081 | Account management, Balance |
| transaction-service | 8082 | Money transfers, Transaction history |
| payment-service | 8083 | Razorpay integration, Webhooks |
| fraud-detection-service | 8084 | Real time fraud detection via Redis |
| notification-service | 8085 | Transaction and fraud alerts |
