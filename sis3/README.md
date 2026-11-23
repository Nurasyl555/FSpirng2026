# SIS 3 — Documented API, Resilient REST Client, AOP/Caching & Event-Driven Kafka with DLT

**Курс:** Backend Framework. Spring (INFT3132) — KBTU  
**Инструктор:** Assubay Miras  
**Задание:** SIS 3 (Неделя 11, 6 баллов) — *«OpenAPI documentation of the whole API, a resilient outbound REST client, a logging or auditing aspect with caching, and a Kafka producer and consumer for a domain event with a dead-letter topic.»*

---

## 1. Что реализовано в SIS 3

### 1.1. OpenAPI / Swagger UI документация всего API
- Подключён `org.springdoc:springdoc-openapi-starter-webmvc-ui` (`3.0.2`).
- В [`OpenApiConfig`](src/main/java/com/example/sis3/config/OpenApiConfig.java) заданы метаданные OpenAPI (`@OpenAPIDefinition`).
- Все эндпоинты [`BookController`](src/main/java/com/example/sis3/controller/BookController.java) задокументированы аннотациями `@Tag`, `@Operation`, `@ApiResponses`, `@ApiResponse`, `@Parameter`, `@ParameterObject`.
- Все DTO (`CreateBookRequest`, `UpdateBookRequest`, `BookResponse`, `ExternalBookMetadataResponse`, `ErrorResponse`, `ValidationError`) описаны через `@Schema`.

### 1.2. Устойчивый исходящий REST-клиент (Timeouts + Retries + Fallback)
- [`ExternalCatalogProperties`](src/main/java/com/example/sis3/config/ExternalCatalogProperties.java) задаёт `base-url`, `connect-timeout` (`2s`) и `read-timeout` (`3s`).
- [`RestClientConfig`](src/main/java/com/example/sis3/config/RestClientConfig.java) настраивает `RestClient` с таймаутами на базе `SimpleClientHttpRequestFactory`.
- [`ExternalBookCatalogClient`](src/main/java/com/example/sis3/client/ExternalBookCatalogClient.java) вызывает OpenLibrary API с **повторными попытками (`maxAttempts = 2`)** и при полном сбое возвращает локальный fallback-ответ (`source = "FALLBACK_LOCAL"`, `fallback = true`).

### 1.3. AOP (логирование, замер времени и бизнес-аудит) + Кэширование
- [`ServiceLoggingAndTimingAspect`](src/main/java/com/example/sis3/aspect/ServiceLoggingAndTimingAspect.java) — перехватывает все методы сервисного слоя (`execution(* com.example.sis3.service..*(..))`) и логирует время выполнения.
- [`Audited`](src/main/java/com/example/sis3/aspect/Audited.java) + [`AuditAspect`](src/main/java/com/example/sis3/aspect/AuditAspect.java) — аспект бизнес-аудита на мутирующие операции (`CREATE_BOOK`, `UPDATE_BOOK`, `DELETE_BOOK`) в [`BookServiceImpl`](src/main/java/com/example/sis3/service/impl/BookServiceImpl.java) (`@AfterReturning` / `@AfterThrowing`).
- [`CacheConfig`](src/main/java/com/example/sis3/config/CacheConfig.java) — кэш на Caffeine (`books` и `booksList` с `@Cacheable`, `@CachePut`, `@CacheEvict`).

### 1.4. Событийная архитектура на Apache Kafka + Dead-Letter Topic (DLT)
- Доменное событие: [`BookEvent`](src/main/java/com/example/sis3/event/BookEvent.java) и [`BookEventType`](src/main/java/com/example/sis3/event/BookEventType.java) (`CREATED`, `UPDATED`, `DELETED`).
- [`KafkaConfig`](src/main/java/com/example/sis3/config/KafkaConfig.java) — объявляет топики `book-events` и `book-events.DLT`, а также `DefaultErrorHandler` с `FixedBackOff(500 ms, 2 retries)` и `DeadLetterPublishingRecoverer`, направляющим неисправимые сообщения в `book-events.DLT`.
- [`BookDomainEventListener`](src/main/java/com/example/sis3/messaging/BookDomainEventListener.java) — слушает `BookEvent` через `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`, чтобы события уходили в Kafka только после успешного коммита в PostgreSQL.
- [`BookEventProducer`](src/main/java/com/example/sis3/messaging/BookEventProducer.java) — сериализует событие в JSON и публикует в `book-events` с ключом `bookId`.
- [`BookEventConsumer`](src/main/java/com/example/sis3/messaging/BookEventConsumer.java) — содержит основной `@KafkaListener` для `book-events` и отдельный DLT `@KafkaListener` для `book-events.DLT`.

---

## 2. Тестирование (41 тест)

- [`BookEventKafkaIntegrationTest`](src/test/java/com/example/sis3/messaging/BookEventKafkaIntegrationTest.java) (`@EmbeddedKafka`) — проверяет отправку и чтение `BookEvent`, а также маршрутизацию невалидного payload в `book-events.DLT`.
- [`ExternalBookCatalogClientTest`](src/test/java/com/example/sis3/client/ExternalBookCatalogClientTest.java) — проверяет успешный ответ, восстановление со 2-й попытки (retry), fallback при `500` и fallback при `readTimeout`.
- [`BookServiceCachingAndAopTest`](src/test/java/com/example/sis3/service/BookServiceCachingAndAopTest.java) — проверяет работу Caffeine-кэша и AOP-аспектов.
- [`BookServiceImplTest`](src/test/java/com/example/sis3/service/BookServiceImplTest.java) — unit-тесты сервиса с проверкой публикации событий.
- [`BookControllerTest`](src/test/java/com/example/sis3/controller/BookControllerTest.java) — `MockMvc` slice-тесты контроллера.
- [`BookRepositoryTest`](src/test/java/com/example/sis3/repository/BookRepositoryTest.java) — интеграционный тест репозитория с PostgreSQL.
- [`Sis3ApplicationTests`](src/test/java/com/example/sis3/Sis3ApplicationTests.java) — тест загрузки полного контекста Spring Boot.

---

## 3. Команды для запуска и проверки

```powershell
# Запуск всех тестов (включая @EmbeddedKafka, без необходимости поднимать внешний брокер)
.\mvnw.cmd test

# Запуск только интеграционного теста Kafka + DLT
.\mvnw.cmd test -Dtest=BookEventKafkaIntegrationTest

# Запуск только тестов внешнего REST-клиента (Retry + Timeout + Fallback)
.\mvnw.cmd test -Dtest=ExternalBookCatalogClientTest

# Запуск Kafka в Docker (для живой демонстрации Producer/Consumer/DLT)
docker compose up -d

# Запуск приложения (PostgreSQL должен быть запущен на localhost:5432/bookdb)
.\mvnw.cmd spring-boot:run

# Остановка Kafka после демонстрации
docker compose down
```

После запуска приложения откройте в браузере:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
