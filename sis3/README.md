# Practice 6 — OpenAPI / Swagger UI & Resilient External Service Call (Timeouts + Fallback)

**Курс:** Backend Framework. Spring (INFT3132) — KBTU  
**Инструктор:** Assubay Miras  
**Задание:** Practice 6 (Неделя 10, 2 балла) — *«Publish the API in Swagger UI and call one external service with configured timeouts and a fallback.»*

---

## 1. Что реализовано в Practice 6

### 1.1. Публикация API в Swagger UI (`springdoc-openapi`)
- В [`pom.xml`](pom.xml) подключена зависимость `org.springdoc:springdoc-openapi-starter-webmvc-ui` (`3.0.2` для Spring Boot 4 / Spring Framework 7).
- В пакете `com.example.sis2.config` создан конфигурационный класс [`OpenApiConfig`](src/main/java/com/example/sis2/config/OpenApiConfig.java) с аннотацией `@OpenAPIDefinition`, задающей метаданные API (`title`, `version`, `description`, `contact`, `servers`).
- В [`application.yml`](src/main/resources/application.yml) настроены пути:
  - OpenAPI JSON-спецификация: `http://localhost:8080/v3/api-docs`
  - Интерактивный Swagger UI: `http://localhost:8080/swagger-ui.html` (редирект на `/swagger-ui/index.html`)
- Все эндпоинты в [`BookController`](src/main/java/com/example/sis2/controller/BookController.java) задокументированы с помощью `@Tag`, `@Operation`, `@ApiResponses`, `@ApiResponse`, `@Parameter` и `@ParameterObject`.
- Все DTO (`CreateBookRequest`, `UpdateBookRequest`, `BookResponse`, `ExternalBookMetadataResponse`, `ErrorResponse`, `ValidationError`) размечены аннотациями `@Schema(description = ..., example = ...)`.

### 1.2. Вызов внешнего сервиса с настроенными таймаутами и Fallback
- **Типизированная конфигурация таймаутов (`ExternalCatalogProperties`):**
  В [`ExternalCatalogProperties`](src/main/java/com/example/sis2/config/ExternalCatalogProperties.java) (`@ConfigurationProperties(prefix = "external.openlibrary")`) вынесены настройки:
  - `base-url`: `https://openlibrary.org`
  - `connect-timeout`: `2s` (максимальное время установки TCP-соединения)
  - `read-timeout`: `3s` (максимальное время ожидания ответа после установки соединения)
- **Бин `RestClient` с таймаутами (`RestClientConfig`):**
  В [`RestClientConfig`](src/main/java/com/example/sis2/config/RestClientConfig.java) создаётся бин `openLibraryRestClient`, использующий `SimpleClientHttpRequestFactory` с установленными `connectTimeout` и `readTimeout`.
- **Клиент внешнего сервиса с Fallback (`ExternalBookCatalogClient`):**
  В [`ExternalBookCatalogClient`](src/main/java/com/example/sis2/client/ExternalBookCatalogClient.java) выполняется запрос к публичному API **OpenLibrary** (`GET /api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data`):
  - При успешном ответе возвращается `ExternalBookMetadataResponse` с `source = "OPEN_LIBRARY_API"` и `fallback = false` (издатель, количество страниц, ссылка на превью).
  - При превышении таймаута (`connectTimeout` / `readTimeout`), сетевой ошибке или HTTP 5xx/4xx клиент перехватывает исключение, логирует предупреждение (`WARN`) и возвращает **Fallback-ответ** (`source = "FALLBACK_LOCAL"`, `fallback = true`).
- **Новые эндпоинты в `BookController`:**
  - `GET /api/v1/books/{id}/external-metadata` — находит книгу в локальной БД по `id`, берет её `isbn` и запрашивает метаданные из OpenLibrary (с fallback при сбое).
  - `GET /api/v1/books/external/isbn/{isbn}` — запрашивает метаданные из OpenLibrary напрямую по любому `isbn`.

---

## 2. Тестирование
- [`ExternalBookCatalogClientTest`](src/test/java/com/example/sis2/client/ExternalBookCatalogClientTest.java) — проверяет:
  1. Успешный парсинг ответа от внешнего сервиса (`MockRestServiceServer`).
  2. Срабатывание Fallback при ошибке `500 Internal Server Error`.
  3. Срабатывание Fallback при реальном превышении `readTimeout` (тест с локальным медленным HTTP-сервером).
- [`BookControllerTest`](src/test/java/com/example/sis2/controller/BookControllerTest.java) — добавлены `MockMvc` тесты для обоих новых эндпоинтов.
- [`BookServiceImplTest`](src/test/java/com/example/sis2/service/BookServiceImplTest.java) — добавлен unit-тест делегирования во внешний клиент.

---

## 3. Команды для запуска

```powershell
# Запуск всех тестов
.\mvnw.cmd clean test

# Запуск тестов внешнего клиента (таймауты и fallback)
.\mvnw.cmd test -Dtest=ExternalBookCatalogClientTest

# Запуск приложения
.\mvnw.cmd spring-boot:run
```

После запуска приложения откройте в браузере:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
