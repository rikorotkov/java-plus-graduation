# Java Plus Graduation — EWM (microservices)

Учебный проект (Spring Boot + Spring Cloud): сервисы доменной логики (`event-service`, `request-service`, `user-service`), статистика (`stats-server`), инфраструктура (`config-server`, `discovery-server`, `gateway-server`).

---

## Архитектура

### Компоненты

- **Gateway** (`gateway-server`) — единая точка входа для клиентов, маршрутизация запросов по сервисам.
- **Service Discovery** (`discovery-server`) — Eureka Registry.
- **Config Server** (`config-server`) — централизованное хранение конфигураций для всех сервисов.
- **Domain services**
    - **Event Service** (`event-service`) — события, категории, подборки, публичные/приватные/админские операции.
    - **Request Service** (`request-service`) — заявки на участие, подтверждения/отклонения.
    - **User Service** (`user-service`) — управление пользователями (админский контур).
- **Stats**
    - **Stats Server** (`stats-server`) — приём хитов и выдача агрегированной статистики.
- **Хранилища**
    - `main-db` (PostgreSQL) — доменная часть (в проекте используется несколько схем/БД: `event_db`, `request_db`, `user_db`).
    - `stats-db` (PostgreSQL) — статистика.



- **Внешний трафик** идёт через `gateway-server`.
- **Обнаружение сервисов** — через `discovery-server` (Eureka), маршрутизация в gateway использует `lb://...`.
- **Конфигурации** сервисов загружаются из `config-server` через discovery (`spring.config.import=configserver:`).
- **Сервис-сервис взаимодействие** (внутреннее) выполняется через **Feign** по именам сервисов (Eureka):
    - `request-service` → `event-service`: получение данных по событию для заявок
    - `event-service` → `request-service`: получение количества подтверждённых заявок
    - доменные сервисы → `stats-server`: запись хитов и запрос статистики

---


## Конфигурации

### Где лежат конфиги

1) **Базовые конфиги сервисов** (подключение config-server через discovery)
- `infra/gateway-server/src/main/resources/application.yaml`
- `infra/config-server/src/main/resources/application.yaml`
- `infra/discovery-server/src/main/resources/application.yaml`
- `core/*-service/src/main/resources/application.yaml`
- `stats/stats-server/src/main/resources/application.yaml`

2) **Централизованные конфиги** — в `config-server`:
- `infra/config-server/src/main/resources/config/infra/gateway-server/application.yaml`
- `infra/config-server/src/main/resources/config/services/event-service/application.yaml`
- `infra/config-server/src/main/resources/config/services/request-service/application.yaml`
- `infra/config-server/src/main/resources/config/services/user-service/application.yaml`
- `infra/config-server/src/main/resources/config/stats/stats-server/application.yaml`

3) **Базы данных / инициализация**
- `docker-compose.yml` — поднимает 2 PostgreSQL:
    - `stats-db`: порт `5433`
    - `main-db`: порт `5434`
- `init_stats.sql` — инициализация схемы/таблиц статистики
- `init_main.sql` — инициализация доменных данных (создание БД и таблиц)

---

## Внутренний API

Внутренние контракты оформлены через Feign-клиенты в модуле **`core/interaction-api`** и реализованы контроллерами в соответствующих сервисах.





## Структура репозитория

```
.
├── infra/                 # инфраструктура Spring Cloud (config/eureka/gateway)
├── core/                  # доменные сервисы + общий interaction-api (DTO/Feign/params)
├── stats/                 # сервис статистики + client/dto
├── docker-compose.yml     # Postgres для main и stats
├── init_main.sql          # инициализация доменной БД
├── init_stats.sql         # инициализация stats БД
├── ewm-main-service-spec.json
├── ewm-stats-service-spec.json
└── postman/feature.json
```

---

