# Practice 5 — AOP Logging, Execution-Time Aspect & Caching

**Курс:** Backend Framework. Spring (INFT3132) — KBTU  
**Инструктор:** Assubay Miras  
**Задание:** Practice 5 (Неделя 9, 2 балла) — *«Write a logging and execution-time aspect for the service layer and add caching to one read-heavy endpoint.»*

---

## 1. Что реализовано в Practice 5

### 1.1. AOP-аспект логирования и замера времени выполнения (`ServiceLoggingAndTimingAspect`)
В пакете `com.example.sis2.aspect` создан аспект [`ServiceLoggingAndTimingAspect`](src/main/java/com/example/sis2/aspect/ServiceLoggingAndTimingAspect.java):
- **Pointcut:** `execution(* com.example.sis2.service..*(..))` — перехватывает все методы сервисного слоя (`BookService` / `BookServiceImpl`).
- **Advice (`@Around`):**
  1. Логирует вход в метод (`>>> [AOP ENTER]`) с именем класса, метода и переданными аргументами.
  2. Замеряет точное время выполнения через `System.nanoTime()`.
  3. Логирует успешный выход (`<<< [AOP EXIT]`) со временем выполнения в миллисекундах (`ms`) и кратким представлением результата.
  4. В случае исключения логирует ошибку (`xxx [AOP EXCEPTION]`) с затраченным временем и пробрасывает исключение дальше в `GlobalExceptionHandler`.

### 1.2. Кэширование read-heavy эндпоинтов (`CacheConfig` + Caffeine)
В пакете `com.example.sis2.config` добавлен конфигурационный класс [`CacheConfig`](src/main/java/com/example/sis2/config/CacheConfig.java) с `@EnableCaching` и менеджером кэша **Caffeine** (`books` и `booksList`, TTL = 10 минут, max size = 500):
- **`@Cacheable(value = "books", key = "#id")`** на `BookServiceImpl.getById(Long id)` (эндпоинт `GET /api/v1/books/{id}`) — при повторном запросе книги по `id` данные возвращаются мгновенно из памяти без SQL-запроса к PostgreSQL.
- **`@Cacheable(value = "booksList")`** на `BookServiceImpl.getAll()` (read-heavy эндпоинт `GET /api/v1/books/catalog`) — кэширует полный каталог книг.
- **Инвалидация и обновление кэша для консистентности данных:**
  - `create()` — `@CacheEvict(value = "booksList", allEntries = true)` сбрасывает кэш каталога при добавлении новой книги.
  - `update()` — `@CachePut(value = "books", key = "#id")` обновляет книгу в кэше и `@CacheEvict(value = "booksList", allEntries = true)` сбрасывает кэш списка.
  - `delete()` — `@Caching(evict = ...)` удаляет книгу из кэша `books` по ключу `#id` и очищает `booksList`.

---

## 2. Тестирование
Помимо всех тестов из SIS 2, добавлен интеграционный тест AOP и кэширования:
- [`BookServiceCachingAndAopTest`](src/test/java/com/example/sis2/service/BookServiceCachingAndAopTest.java) — проверяет работу прокси AOP, `Cache HIT` / `Cache MISS` на `getById()` и `getAll()`, обновление через `@CachePut` при `update()` и очистку через `@CacheEvict` при `delete()`.
- В [`BookControllerTest`](src/test/java/com/example/sis2/controller/BookControllerTest.java) добавлен тест для эндпоинта `GET /api/v1/books/catalog`.

---

## 3. Команды для запуска

```powershell
# Запуск всех тестов
.\mvnw.cmd clean test

# Запуск только тестов кэширования и AOP (Practice 5)
.\mvnw.cmd test -Dtest=BookServiceCachingAndAopTest

# Запуск приложения
.\mvnw.cmd spring-boot:run
```
