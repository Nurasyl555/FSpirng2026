# SIS 2 — Layered Architecture & Test Coverage

**Курс:** Backend Framework. Spring (INFT3132) — KBTU  
**Инструктор:** Assubay Miras  
**Задание:** SIS 2 (Неделя 8, 6 баллов)

---

## 1. Архитектура проекта (Layered & DDD Architecture)

Проект полностью разделен на изолированные слои с использованием **MapStruct** для преобразования моделей данных:

```
[ HTTP Client ]
      │ (JSON)
      ▼
┌──────────────────────────────────────────────────────────────┐
│ 1. Web Layer                                                 │
│    • BookController (/api/v1/books)                          │
│    • DTOs: CreateBookRequest, UpdateBookRequest, BookResponse│
│    • GlobalExceptionHandler & ErrorResponse                  │
└──────────────────────────────────────────────────────────────┘
      │  ▲
      │  │  MapStruct (DTO <-> Domain)
      ▼  │
┌──────────────────────────────────────────────────────────────┐
│ 2. Domain / Service Layer                                    │
│    • Book (Чистая бизнес-модель без JPA-аннотаций)           │
│    • BookService & BookServiceImpl                           │
│    • Business Exceptions: DuplicateResourceException, etc.   │
└──────────────────────────────────────────────────────────────┘
      │  ▲
      │  │  MapStruct (Domain <-> Entity)
      ▼  │
┌──────────────────────────────────────────────────────────────┐
│ 3. Persistence Layer                                         │
│    • BookEntity (@Entity, @Table(name = "books"))            │
│    • BookRepository (Spring Data JPA)                        │
│    • Flyway Migrations (V1__create_books_table, V2__seed)    │
└──────────────────────────────────────────────────────────────┘
      │
      ▼
[ PostgreSQL Database (port 5432) ]
```

---

## 2. Разделение моделей и маппинг (MapStruct)

1. **DTO Layer (`com.example.sis2.dto`)**:
   - `CreateBookRequest`, `UpdateBookRequest` — содержат валидацию `@Valid` (`@NotBlank`, `@Size`, `@Pattern`, `@DecimalMin`, `@Digits`, `@Min`, `@Max`).
   - `BookResponse` — представление для клиента API.
2. **Domain Layer (`com.example.sis2.model`)**:
   - `Book` — чистый Java-объект (POJO) бизнес-слоя, полностью отвязан от Hibernate/JPA.
3. **Persistence Layer (`com.example.sis2.entity`)**:
   - `BookEntity` — сущность базы данных с `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@PrePersist`, `@PreUpdate`.
4. **Mapper (`com.example.sis2.mapper.BookMapper`)**:
   - Реализован через **MapStruct**. Генерирует код маппинга во время компиляции, обеспечивая высокую производительность без рефлексии.

---

## 3. Пирамида тестирования (27 тестов, 100% успех)

| Уровень тестов | Тестовый класс | Количество тестов | Описание |
|---|---|---|---|
| **Unit Tests** (Service Layer) | `BookServiceImplTest` | **11** | Тестирование бизнес-логики в полной изоляции с **JUnit 5** и **Mockito**. Мокаются репозиторий и маппер. Проверяются все позитивные и негативные сценарии (дубликат ISBN, NotFound, пагинация). |
| **Web Slice Tests** (Web Layer) | `BookControllerTest` | **11** | Срез веб-слоя с `@WebMvcTest` и **MockMvc**. Проверяются HTTP-статусы (201, 200, 204, 400, 404, 409), Bean Validation, обработка ошибок через `GlobalExceptionHandler`. |
| **Integration Tests** (Repository) | `BookRepositoryTest` | **4** | Интеграционные тесты с `@DataJpaTest` против реальной БД **PostgreSQL** (`existsByIsbn`, `existsByIsbnAndIdNot`, `findByIsbn`, `save`). |
| **Context Test** | `Sis2ApplicationTests` | **1** | Проверка корректной загрузки Spring Boot ApplicationContext. |

---

## 4. Запуск и проверка

### Запуск всех тестов:
```powershell
.\mvnw.cmd clean test
```

### Запуск конкретного набора тестов:
```powershell
# Только Unit-тесты сервиса
.\mvnw.cmd test -Dtest=BookServiceImplTest

# Только Web Slice-тесты контроллера
.\mvnw.cmd test -Dtest=BookControllerTest

# Только интеграционные тесты репозитория
.\mvnw.cmd test -Dtest=BookRepositoryTest
```

### Ручное тестирование API:
Все запросы CRUD и негативные сценарии подготовлены в файле [requests.http](requests.http).
