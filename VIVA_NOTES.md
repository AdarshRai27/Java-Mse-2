# Viva Notes - CS336B OOP with Java

These notes explain **why** the code is written the way it is. Read the flow
first, then the likely questions. Exact commands are in the last section.

---

## Part 1 - Q1: JDBC Order Management

### 1.1 One-line architecture

```text
Main (menu + input validation)
   -> OrderDAO (all SQL, PreparedStatement, try-with-resources)
      -> DBConnection (DriverManager + env variables)
         -> MySQL ecommerce_db.Orders

Order is the POJO used to carry one row between Java and the database.
```

`Main` never writes SQL. `OrderDAO` never reads from the keyboard. This
separation is the single-responsibility idea: input/printing in `Main`, data
access in `OrderDAO`.

### 1.2 Flow of every menu option

| Option | Method | SQL / logic |
|--------|--------|-------------|
| 1. Seed | `Main.seedSampleOrders()` | Builds `List<Order>` of 5 rows, calls `insertOrder` for each. A duplicate returns `false`; the loop continues. |
| 2. Insert | `Main.insertOrderFromInput()` | Validates id/name/quantity/price/status, builds `Order`, calls `insertOrder`. |
| 3. Filter | `Main.filterOrdersByStatus()` | Normalizes status, then `findByStatusWithQuantityGreaterThanTwo`. |
| 4. Increase | `Main.increaseQuantity()` | Positive extra quantity; `UPDATE ... quantity = quantity + ?`; re-fetches and prints. |
| 5. Update status | `Main.updateStatus()` | `findById` first (existence), normalize/validate status, then `UPDATE`; re-fetches. |
| 6. Total + delete | `Main.showTotalAndDeleteCancelledOrder()` | `SUM(quantity * unit_price)`, then `findById`; deletes only if status is `Cancelled`. |
| 7. All | `Main.displayAllOrders()` | `SELECT ... ORDER BY order_id`; prints a table. |
| 0. Exit | `main` loop | Sets `running = false`; loop ends; Scanner closed. |

**SQL used**

```sql
INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status)
VALUES (?, ?, ?, ?, ?, ?);

SELECT order_id, customer_name, product_name, quantity, unit_price, status
FROM Orders
WHERE status = ? AND quantity > 2
ORDER BY unit_price DESC;

UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?;
UPDATE Orders SET status = ? WHERE order_id = ?;

SELECT SUM(quantity * unit_price) FROM Orders;

DELETE FROM Orders WHERE order_id = ?;
```

Why `quantity > 2` is written directly in SQL: the assignment fixes that
threshold, so it is a rule of the query, not user input. The status is still a
parameter because it comes from the user.

### 1.3 Why PreparedStatement (the most likely viva question)

1. **Prevents SQL injection.** The SQL structure is fixed before values arrive.
   With string concatenation, a customer name like `x' OR '1'='1` changes the
   query. With a placeholder, the whole value is treated as data.
2. **Precompiled by the database/driver.** The statement can be reused with new
   parameter values; the driver also handles escaping and data types.
3. **Type-safe binding.** `setInt`, `setDouble`, `setString` avoid problems such
   as a decimal comma or a missing quote.
4. **Clearer code.** Parameters are separated from the SQL text.

Example of the danger:

```java
// NEVER do this
"SELECT * FROM Orders WHERE status = '" + userInput + "'"
// userInput = "' OR '1'='1" changes the meaning of the query.
```

### 1.4 Why try-with-resources

`Connection`, `PreparedStatement`, and `ResultSet` are scarce OS/database
resources and implement `AutoCloseable`.

```java
try (Connection connection = DBConnection.getConnection();
     PreparedStatement statement = connection.prepareStatement(sql);
     ResultSet resultSet = statement.executeQuery()) {
    ...
}
```

- Resources are closed automatically, in reverse order, even if an exception is
  thrown.
- No repeated `finally { ... }` blocks and no forgotten `close()`.
- The connection is always returned, so the next menu action can connect again.
- A new connection per DAO call is simple for this console app. A bigger
  application would use a connection pool (e.g., HikariCP).

### 1.5 Duplicate order ids

`order_id` is the primary key. A second insert with the same id causes a
constraint violation. `OrderDAO.insertOrder` catches:

1. `SQLIntegrityConstraintViolationException` - the specific checked JDBC
   exception, or
2. a general `SQLException` whose MySQL error code is `1062` (duplicate entry),
   because some driver versions report it that way.

It prints `Order ID 101 already exists - skipped` and returns `false`.
`seedSampleOrders` does not stop; it continues with the remaining orders. That
makes seeding **idempotent enough to re-run**: first run inserts 5, later runs
skip the same 5.

### 1.6 Input validation

- `readInt` loops until `Integer.parseInt` succeeds, so `abc` prints a message
  and asks again instead of crashing.
- `readPositiveInt` rejects `0` and negatives.
- `readNonNegativeDouble` rejects text, `NaN`, infinity, and negatives.
- `readNonBlank` rejects empty names.
- `readValidStatus` and `normalizeStatus` accept any casing and return the
  canonical spelling (`shipped` -> `Shipped`).
- In option 5, invalid status is **not** re-prompted: the assignment asks for a
  distinct failure message, so the order is left unchanged.

### 1.7 Collections used in Q1

| Collection | Where | Why |
|------------|-------|-----|
| `Set<String> VALID_STATUSES` | `Order` | No duplicate statuses, fast `contains`, one source of truth. A `LinkedHashSet` keeps display order; it is wrapped as unmodifiable. |
| `List<Order>` | `seedSampleOrders` | A batch has a deliberate order, and the loop needs index-free iteration. `List.of` gives a fixed 5-row batch. |
| `List<Order>` | DAO query results | SQL already returns the required order; `ArrayList` is the normal growable result container and keeps duplicates/order. |
| `Optional<Order>` | `findById` | Makes "no row" explicit instead of returning a bare `null`; forces the caller to handle absence. |

`Set` is not used for query results because two orders may legitimately have the
same values except id, and order matters; a `Set` would also lose the SQL order.

### 1.8 Checked vs unchecked exceptions in JDBC

`SQLException` is **checked**: the compiler forces the caller to catch it or
declare `throws`. That is useful because database failure is recoverable (show a
message, keep the menu running). `NullPointerException` and
`NumberFormatException` are **unchecked**; they usually indicate a programming
or input problem. `Main` catches `SQLException` around the menu switch and prints
a friendly database error instead of ending the program.

---

## Part 2 - Q2: Spring Boot Library API

### 2.1 Request flow

```text
curl / browser
   -> embedded Tomcat (started by Spring Boot)
      -> DispatcherServlet
         -> BookController (@RestController)
            -> BookService (@Service, business logic + validation)
               -> Map<Integer, Book> (in-memory storage)
         <- Book object / Optional
      <- Jackson converts the object to JSON
   <- HTTP response with an explicit status code
```

The controller is thin: it does HTTP mapping. The service owns rules and data,
so the business logic can be tested or replaced without changing URLs.

### 2.2 Annotations

| Annotation | Meaning in this project |
|------------|-------------------------|
| `@SpringBootApplication` | Combines auto-configuration, component scanning, and a configuration class. `SpringApplication.run` starts the context and embedded Tomcat. |
| `@RestController` | Marks a controller whose return values are written as the response body (JSON), not as view names. |
| `@RequestMapping("/api/books")` | Common base path for every method in the controller. |
| `@PostMapping` | Handles `POST` (create). |
| `@GetMapping` | Handles `GET` (read). |
| `@PutMapping` | Handles `PUT` (full update of editable fields). |
| `@DeleteMapping` | Handles `DELETE`. |
| `@RequestBody` | Tells Jackson to convert the JSON request body into a `Book`. |
| `@PathVariable` | Takes `{id}` from the URL and binds it to the `Integer id` parameter. |
| `@Service` | Marks `BookService` as a Spring-managed business component, so it can be injected into the controller. |
| `@ExceptionHandler` | Converts a service validation exception into a `400` JSON response. |
| `ResponseEntity` | Gives explicit control of status code, headers, and body. |

The controller uses **constructor injection**: Spring sees the
`BookController(BookService)` constructor and passes the service bean. This is
easier to test than creating dependencies inside the class.

### 2.3 Why `Map<Integer, Book>`?

```java
private final Map<Integer, Book> books = new LinkedHashMap<>();
private final AtomicInteger nextId = new AtomicInteger(1);
```

- The id is the natural key, so `books.get(id)`, `put`, and `remove` are O(1)
  average. With a `List<Book>`, finding/deleting by id would scan until a match:
  O(n).
- `LinkedHashMap` also preserves insertion order, so `GET /api/books` is stable
  and predictable.
- `AtomicInteger.getAndIncrement()` gives each new book a unique id without
  scanning the map.
- Service methods are `synchronized`, so two HTTP requests cannot change the map
  at the same time.
- `findAll()` returns `new ArrayList<>(books.values())`, a copy of the values,
  so a caller cannot change the map structure.

**With a database:** the map would become a JPA repository or DAO. The database
would generate ids (`AUTO_INCREMENT`/identity), `@Transactional` would control
commit/rollback, and `BookService` would keep the same business rules. The
controller and URLs would hardly change - that is the benefit of the layering.

### 2.4 HTTP status choices

| Code | When | Why |
|------|------|-----|
| `201 Created` | Successful `POST` | A new resource was created. `Location: /api/books/{id}` tells the client where it is. |
| `200 OK` | `GET`, successful `PUT` | The request succeeded and a body is returned. |
| `204 No Content` | Successful `DELETE` | Success, but there is nothing sensible to return. |
| `400 Bad Request` | Validation fails | The client sent data the server rules reject (blank title, negative copies). |
| `404 Not Found` | Unknown id | The URL is valid, but that resource does not exist. |
| `500` | Unexpected server error | Spring's fallback; not expected in this app. |

`400` vs `404`: `400` means "your data is invalid"; `404` means "the thing you
addressed is absent". `POST` has no 404 because the collection `/api/books`
always exists. `GET /api/books` has no 404 because an empty list is a valid
result (`200 []`).

`PUT` checks existence **before** validation. If the id is missing, the answer is
`404` even if the body is also invalid; there is no resource to update. `PUT`
never changes the id.

### 2.5 Validation and error body

Hand-written validation lives in `BookService.validate`:

- title not null/blank
- author not null/blank
- `availableCopies` not null and `>= 0`

It throws `IllegalArgumentException`. `BookController.handleInvalidBook` catches
it and returns:

```json
{"error":"Title must not be blank"}
```

A missing book returns:

```json
{"error":"Book with id 9999 not found"}
```

Using `Map.of("error", message)` avoids creating an extra error class for such a
small API. In a larger API, a dedicated `ErrorResponse` DTO would be clearer.

### 2.6 Why the no-arg constructor in `Book`

Jackson creates the object from JSON by calling the no-arg constructor and then
the setters. Without it, deserialization fails. The all-args constructor is for
convenient object creation in Java (seed data and tests).

---

## Part 3 - Likely viva questions (15)

**1. Difference between `ArrayList` and `HashMap`?**
`ArrayList` is an ordered, index-based list: duplicates allowed, `get(index)` is
O(1), search by value is O(n). `HashMap` stores key-value pairs: keys are unique,
lookup by key is O(1) average, and order is not guaranteed. Q1 uses `List` for
query results; Q2 uses `Map` because books are looked up by id.

**2. Why use a `Set` for statuses?**
Statuses are a fixed vocabulary: duplicates are meaningless. A `Set` gives one
source of truth and fast exact-match `contains`. `LinkedHashSet` keeps the order
for messages, and the unmodifiable wrapper prevents accidental changes.
`normalizeStatus` loops over the set because it needs the canonical spelling for
a case-insensitive match, not just a yes/no answer.

**3. Why `PreparedStatement` instead of `Statement`?**
Placeholders separate SQL from data, preventing SQL injection; the statement is
precompiled/reusable; setters handle types and quoting. String concatenation
would let user input change the query.

**4. What does try-with-resources do?**
It automatically closes every resource declared in the `try (...)` header, in
reverse order, even when an exception occurs. This prevents leaked connections,
statements, and result sets.

**5. Checked vs unchecked exceptions in JDBC?**
`SQLException` is checked, so the compiler forces handling or a `throws`
declaration. `NumberFormatException` and `NullPointerException` are unchecked,
usually caused by bad input or a bug. Q1 catches input errors at the helper level
and catches `SQLException` around the menu operation.

**6. How is a duplicate order id handled?**
The primary key raises a constraint violation. The DAO catches
`SQLIntegrityConstraintViolationException` (or MySQL error code 1062), prints a
friendly message, and returns `false`; the seeding loop continues. Therefore one
duplicate does not stop the batch.

**7. `POST` vs `PUT`?**
`POST /api/books` creates a new book and lets the server assign the id; it is not
idempotent. `PUT /api/books/{id}` updates the resource at that known id and is
idempotent: repeating the same request has the same effect. `PUT` here does not
change the id.

**8. `201` vs `200` vs `204`?**
`201` means a new resource was created. `200` means success with a response
body. `204` means success with intentionally no body (used for `DELETE`).

**9. `400` vs `404`?**
`400 Bad Request`: the client's data violates validation rules. `404 Not Found`:
the requested id/resource does not exist. In `PUT`, a missing id wins over
invalid body, because there is nothing to update.

**10. Why a service layer if the controller could do everything?**
The controller should only translate HTTP. The service holds business rules
(validation, id generation, storage), so it can be reused and unit-tested
without HTTP. It also keeps the viva explanation clean: controller = HTTP,
service = business.

**11. Why `Map<Integer, Book>` instead of `List<Book>`?**
Books are addressed by id. `Map` gives O(1) get/put/remove by id. A list would
need an O(n) scan for every get/update/delete. `LinkedHashMap` adds stable
insertion order for listing.

**12. What if two requests update the same book concurrently?**
`BookService` methods are `synchronized`, so one update finishes before the next
starts; the map cannot be corrupted. The second update still overwrites the
first (last write wins), which is the usual lost-update problem. A real system
would add optimistic locking (`@Version`) or a pessimistic database lock.

**13. What changes if Q2 uses a database?**
Replace the map with a repository/DAO and an entity. The database generates ids,
`@Transactional` handles commit/rollback, and queries replace map operations.
The controller, URLs, and service rules stay mostly the same. The service layer
is what makes this replacement easy.

**14. Why does `Book` need a no-arg constructor?**
Jackson deserializes JSON by creating an empty object and calling setters.
Without a no-arg constructor it cannot instantiate the class. The all-args
constructor is only a Java convenience.

**15. What does `@SpringBootApplication` do?**
It combines `@Configuration`, `@EnableAutoConfiguration`, and
`@ComponentScan` (plus `@SpringBootConfiguration`). It scans the package for
controllers/services, auto-configures Spring MVC/Jackson/Tomcat, and lets
`SpringApplication.run` start the embedded server.

---

## Part 4 - How to demo (cheat sheet)

### Q1 - JDBC

```powershell
cd q1-jdbc-order-management

# 1. MySQL should be running. Create the schema (PowerShell):
Get-Content .\schema.sql -Raw | mysql -u root -p

# 2. Point the app at MySQL:
$env:DB_URL = "jdbc:mysql://localhost:3306/ecommerce_db"
$env:DB_USER = "root"
$env:DB_PASS = "your_password"

# 3. Build and run:
mvn -q compile
mvn compile exec:java
```

Demo order (about 3 minutes):

1. `1` - seed 5 orders.
2. `1` again - show duplicate handling and that the batch continues.
3. `3` then `Pending` - show `quantity > 2` and price descending.
4. `3` then `Shipped` - show `No matching orders` (the shipped row has quantity 2).
5. `4` with `999` and `2` - show `Order not found`.
6. `4` with `102` and `2` - show quantity increased and re-fetched row.
7. `5` with `101` and `lost` - show invalid status leaves the row unchanged.
8. `5` with `103` and `delivered` - show case-insensitive normalization.
9. `6`, total shown, delete `103` - show "cannot be deleted", status `Delivered`.
10. `6`, delete `105` - show successful delete of the Cancelled row.
11. `7` - show all remaining rows.
12. `0` - exit.

If a previous demo deleted `105`, run option `1` first to restore it.

### Q2 - Spring Boot

Terminal 1:

```bash
cd q2-library-springboot
mvn test
mvn spring-boot:run
```

Terminal 2 (Git Bash on Windows):

```bash
cd q2-library-springboot
bash test-api.sh
```

Manual demo with curl:

```bash
curl -i http://localhost:8080/api/books
curl -i -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Design Patterns","author":"Erich Gamma","category":"Programming","availableCopies":3}'
curl -i http://localhost:8080/api/books/4
curl -i -X PUT http://localhost:8080/api/books/4 \
  -H "Content-Type: application/json" \
  -d '{"title":"Design Patterns 2e","author":"Erich Gamma","category":"Programming","availableCopies":7}'
curl -i http://localhost:8080/api/books/9999
curl -i -X DELETE http://localhost:8080/api/books/4
curl -i -X DELETE http://localhost:8080/api/books/4
```

Expected codes in order: `200`, `201`, `200`, `200`, `404`, `204`, `404`.
Stop the app with `Ctrl+C`.

### If the examiner points at a random line

Say what role the line plays in the flow:

```text
input -> validation -> service/DAO -> SQL or collection -> response/print
```

For Q1, the key phrase is "PreparedStatement binds values; try-with-resources
closes resources". For Q2, the key phrase is "controller handles HTTP, service
handles business logic, LinkedHashMap stores books by id".
