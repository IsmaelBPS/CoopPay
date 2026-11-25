# CoopPay

[![CI](https://github.com/IsmaelBPS/CoopPay/actions/workflows/ci.yml/badge.svg)](https://github.com/IsmaelBPS/CoopPay/actions/workflows/ci.yml)

Núcleo de movimentações financeiras entre associados de uma cooperativa de crédito, projetado com foco em **consistência de saldo sob concorrência**, **rastreabilidade** e **comunicação assíncrona** entre serviços.

---

## Arquitetura & Decisões Técnicas

O projeto segue uma organização modular por contexto de domínio, separando camadas de entrada (`api`), regras de negócio (`domain` / service) e persistência (`infrastructure`):

```text
src/main/java/com/ismael/CoopPay/
├── associate/
│   ├── api/                        # Controllers e DTOs com Bean Validation
│   ├── domain/                     # Entidades de domínio e portas (AssociateRepository)
│   ├── infrastructure/persistence/ # Entidade JPA e adaptador de repositório
│   └── AssociateService.java       # Regras de negócio de associados
├── shared/
│   ├── api/                        # Tratamento global de erros (GlobalExceptionHandler / ProblemDetail)
│   └── exception/                  # Exceções de domínio reutilizáveis
└── account/                        # Módulo de contas e movimentações (em evolução)
```

- **Ports & Adapters (Hexagonal leve):** o domínio define interfaces de repositório (ex: `AssociateRepository`), mantendo as regras de negócio desacopladas de detalhes do JPA.
- **Integridade de schema com Flyway + `ddl-auto=validate`:** o Hibernate apenas valida o schema em runtime; toda evolução de tabelas passa por migrações SQL versionadas (`src/main/resources/db/migration`).
- **Testes realistas com Testcontainers:** testes de integração sobem instâncias reais de PostgreSQL e Kafka via Docker, evitando divergências comuns de bancos em memória.
- **Testes de mutação com PITest:** além de cobertura tradicional, o projeto utiliza mutation testing para validar a real eficácia dos testes unitários.

---

## Tecnologias

### Em uso na aplicação
- **Core & API:** Java 17, Maven (`mvnw`), Spring Boot 4.2.0, Spring Web MVC, Bean Validation, Springdoc OpenAPI (Swagger UI), Lombok, Spring Boot DevTools
- **Persistência:** PostgreSQL 16 (`postgres:16-alpine`), Spring Data JPA / Hibernate, Flyway (`flyway-database-postgresql`)
- **Observabilidade:** Spring Boot Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`), Micrometer + Prometheus, Micrometer Tracing + OpenTelemetry (exportação OTLP para Grafana Tempo e `traceId`/`spanId` no MDC/logs), métricas customizadas de negócio, observabilidade Kafka habilitada e perfil `prod` com logs estruturados ECS (`application-prod.properties`)
- **Infraestrutura & Testes:** Docker Compose (PostgreSQL, Kafka, Prometheus, Tempo, Grafana), JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL e Kafka), PITest (`pitest-maven`)

### Provisionadas para as próximas etapas
- **Apache Kafka 3.9.0** (`apache/kafka` em modo KRaft) + **Spring for Apache Kafka** e **Kafka UI** — infraestrutura e `TestcontainersConfiguration` já configurados; producers/consumers ainda serão implementados nos fluxos de transação.


### Frontend
- O proxy do vite deve ser usado somente em desenvolvimento, implementar um proxy reverso em produção.

---

## Como executar localmente

### Pré-requisitos
- Java 17+
- Docker e Docker Compose

### 1. Configurar variáveis de ambiente
```bash
cp .env.example .env
```

### 2. Subir infraestrutura (PostgreSQL e Kafka)
```bash
docker compose up -d
```

*(Opcional)* Para subir a stack de observabilidade (**Prometheus**, **Grafana Tempo** e **Grafana**):
```bash
docker compose --profile observability up -d
```

*(Opcional)* Para subir todas as ferramentas (**Kafka UI** na porta `8080` + observabilidade):
```bash
docker compose --profile tools up -d
```

### 3. Iniciar a aplicação
```bash
./mvnw spring-boot:run
```

*(Opcional)* Para rodar com o perfil de produção (logs JSON ECS, `sampling=0.1` e Actuator restrito):
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

A API sobe por padrão na porta **`3499`** com prefixo **`/api/v1`**.

Portas da observabilidade local:
- **Grafana:** `http://localhost:3000` (dashboard provisionado `CoopPay - Observability Overview`)
- **Prometheus:** `http://localhost:9090`
- **Tempo:** `http://localhost:3200` (OTLP HTTP `4318` / gRPC `4317`)
- **Actuator Prometheus:** `http://localhost:3499/actuator/prometheus`

---

## Endpoints disponíveis

Documentação interativa da API:
- **Swagger UI:** `http://localhost:3499/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:3499/v3/api-docs`

### Associados (`/api/v1/associates`)
| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/v1/associates` | Cadastra um novo associado |
| `GET` | `/api/v1/associates/{id}` | Busca associado por UUID |
| `PUT` | `/api/v1/associates/{id}` | Atualiza dados do associado |
| `DELETE` | `/api/v1/associates/{id}` | Remove associado |

Exemplo de cadastro (`POST /api/v1/associates`):
```bash
curl -X POST http://localhost:3499/api/v1/associates \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Silva",
    "email": "maria@cooppay.com",
    "password": "senha-segura",
    "type": "INDIVIDUAL"
  }'
```
*(Valores válidos para `type`: `INDIVIDUAL` ou `COMPANY`.)*

---

## Testes

Executar testes unitários e de integração (requer Docker ativo para o Testcontainers):
```bash
./mvnw test
```

Executar testes de mutação com PITest:
```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

A pipeline de CI (`.github/workflows/ci.yml`) roda a cada push na `main` e em pull requests: testes do backend com Testcontainers, build do frontend e validação do `docker-compose.yml`.

---

## Roadmap
- [ ] Módulo de contas (`account`): criação de conta, consulta de saldo, depósito, saque e transferência entre associados
- [ ] Controle de concorrência de saldo com lock na conta (`SELECT ... FOR UPDATE` / lock pessimista ou otimista com `@Version`) para evitar race conditions, duplicação de operações e débitos simultâneos/quase instantâneos sobre o mesmo saldo desatualizado
- [ ] Ledger imutável de movimentações para auditoria e rastreabilidade
- [ ] Publicação e consumo de eventos financeiros no Apache Kafka
- [x] Configuração explícita de observabilidade básica e health checks via Spring Boot Actuator (`traceId`/`spanId` com OpenTelemetry e logs de negócio)
- [x] Métricas Prometheus (`/actuator/prometheus`) e métricas customizadas de negócio (`cooppay.ledger.entries.*`, `cooppay.associates.operations`)
- [x] Propagação de `traceId` preparada no Apache Kafka (`spring.kafka.template.observation-enabled=true` e `spring.kafka.listener.observation-enabled=true`)
- [x] Stack local de observabilidade no Docker Compose (Prometheus, Grafana Tempo e Grafana com datasources/dashboard provisionados) e exportação OTLP
- [x] Perfil de produção (`application-prod.properties` com logs estruturados ECS, restrição de `show-details` no Actuator e `sampling.probability=0.1`)
- [ ] Logs e métricas de negócio no módulo de contas (`AccountService`) e fluxos de concorrência/locking (saldo insuficiente e conflitos de concorrência)
- [x] Pipeline de CI no GitHub Actions (backend `./mvnw test` com Testcontainers, build do frontend e validação do `docker-compose.yml`)
- [x] Segurança de senha dos associados (hashing seguro com BCrypt/Argon2 antes de persistir)
- [x] Mitigação de race conditions na idempotência (tratamento de violação de chave única para requisições concorrentes com mesma `idempotencyKey`)
- [x] Tratamento global de erros na API REST (`@RestControllerAdvice` / `ProblemDetail` para padronizar respostas 4xx/5xx e mapear exceções de domínio e validação)
- [ ] Integração com serviços AWS em ambiente local usando Floci no Docker Compose (ex.: S3 para comprovantes, SQS/SNS e SES)
