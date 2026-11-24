# CoopPay

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
- **Observabilidade:** Spring Boot Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`), Micrometer Tracing + OpenTelemetry (`traceId`/`spanId` automáticos no MDC/logs)
- **Infraestrutura & Testes:** Docker Compose, JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL e Kafka), PITest (`pitest-maven`)

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

*(Opcional)* Para subir também o **Kafka UI** (porta `8080`):
```bash
docker compose --profile tools up -d
```

### 3. Iniciar a aplicação
```bash
./mvnw spring-boot:run
```

A API sobe por padrão na porta **`3499`** com prefixo **`/api/v1`**.

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

---

## Roadmap
- [ ] Módulo de contas (`account`): criação de conta, consulta de saldo, depósito, saque e transferência entre associados
- [ ] Controle de concorrência de saldo (locking otimista/pessimista) para evitar race conditions
- [ ] Ledger imutável de movimentações para auditoria e rastreabilidade
- [ ] Publicação e consumo de eventos financeiros no Apache Kafka
- [x] Configuração explícita de observabilidade básica e health checks via Spring Boot Actuator (`traceId`/`spanId` com OpenTelemetry e logs de negócio)
- [ ] Métricas Prometheus (`/actuator/prometheus`) e métricas customizadas de negócio (operações, idempotência, saldo insuficiente e conflitos de concorrência)
- [ ] Propagação de `traceId` entre producers e consumers no Apache Kafka
- [ ] Stack local de observabilidade no Docker Compose (Prometheus, Grafana e Tempo/Loki ou `grafana/otel-lgtm`) com exportação OTLP
- [ ] Endurecimento de observabilidade para produção (logs estruturados em JSON, restrição de `show-details` no Actuator e ajuste de `sampling.probability`)
- [ ] Logs de negócio no módulo de contas (`AccountService`) e fluxos de concorrência/locking
