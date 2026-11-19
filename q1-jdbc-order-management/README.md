# Q1 - Order Management System (Java JDBC + MySQL)

A console application for an e-commerce company. It stores orders in a MySQL
database and exposes a menu for inserting, filtering, updating, valuing, and
deleting orders.

- Java 17
- Maven
- JDBC with `PreparedStatement` only
- MySQL 8.x
- Package: `com.ecommerce`

## Menu and assignment mapping

| Menu | Assignment part | What it does |
|------|-----------------|--------------|
| 1. Seed 5 sample orders | (a) | Inserts 5 rows. Re-running is safe: duplicates are skipped. |
| 2. Insert a new order | (a) | Reads one order from the keyboard and inserts it. |
| 3. Filter by status | (b) | Status + `quantity > 2`, sorted by `unit_price DESC`. |
| 4. Increase quantity | (c) | `quantity = quantity + ?`; reports "Order not found" when needed. |
| 5. Update order status | (d) | Checks existence and validates the new status. |
| 6. Total value / delete | (e) | Shows `SUM(quantity * unit_price)`, then deletes only a Cancelled order. |
| 7. Show all orders | (f) | Lists every order. |
| 0. Exit | (f) | Leaves the loop and closes the Scanner. |

## Prerequisites

1. JDK 17 or newer (`java -version`).
2. Maven 3.9 or newer (`mvn -version`).
3. MySQL 8.x running on `localhost:3306`.

## 1. Create the database and table

From this project folder:

```bash
mysql -u root -p < schema.sql
```

PowerShell does not support `<` redirection, so use:

```powershell
Get-Content .\schema.sql -Raw | mysql -u root -p
```

Verify:

```bash
mysql -u root -p -e "USE ecommerce_db; DESCRIBE Orders;"
```

The script uses `CREATE DATABASE IF NOT EXISTS` and
`CREATE TABLE IF NOT EXISTS`, so it can be run again safely.

## 2. Configure the connection

`DBConnection` reads three environment variables. Real passwords are never
written in the source code.

| Variable | Default |
|----------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/ecommerce_db` |
| `DB_USER` | `root` |
| `DB_PASS` | empty string |

PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/ecommerce_db"
$env:DB_USER = "root"
$env:DB_PASS = "your_mysql_password"
```

Bash:

```bash
export DB_URL="jdbc:mysql://localhost:3306/ecommerce_db"
export DB_USER="root"
export DB_PASS="your_mysql_password"
```

If MySQL 8 rejects the connection with a public-key error, add connection
options to `DB_URL`:

```text
jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

## 3. Build and run

```bash
mvn -q compile
mvn compile exec:java
```

`exec-maven-plugin` is already configured in `pom.xml` with
`com.ecommerce.Main` as the main class.

## Project structure

```text
q1-jdbc-order-management/
├── pom.xml
├── schema.sql
├── README.md
└── src/main/java/com/ecommerce/
    ├── DBConnection.java   connection + env variables
    ├── Order.java          POJO + VALID_STATUSES + status normalization
    ├── OrderDAO.java       all SQL (PreparedStatement + try-with-resources)
    └── Main.java           menu, input validation, printing
```

## Sample run transcript

Captured from a real run against MySQL 8.0.46. Repeated menu blocks are removed
for readability; the messages and values are unchanged.

```text
=== E-Commerce Order Management System ===

----- Menu -----
1. Seed 5 sample orders
2. Insert a new order
3. Filter by status (quantity > 2, price high to low)
4. Increase quantity of an order
5. Update order status
6. Show total value / delete a cancelled order
7. Show all orders
0. Exit
Choose an option: x
Invalid number. Please enter a whole number.
Choose an option: 1
Seed finished: 5 inserted, 0 skipped (order id already existed).

Choose an option: 1
Order ID 101 already exists - skipped
Order ID 102 already exists - skipped
Order ID 103 already exists - skipped
Order ID 104 already exists - skipped
Order ID 105 already exists - skipped
Seed finished: 0 inserted, 5 skipped (order id already existed).

Choose an option: 2
Order ID: 101
Customer name: Duplicate Customer
Product name: Duplicate Product
Quantity: 3
Unit price: 10.00
Status (Pending/Shipped/Delivered/Cancelled): pending
Order ID 101 already exists - skipped

Choose an option: 2
Order ID: 201
Customer name: John Doe
Product name: USB Hub
Quantity: 3
Unit price: 30.00
Status (Pending/Shipped/Delivered/Cancelled): Pending
Order inserted successfully:
ID       CUSTOMER             PRODUCT                  QTY    UNIT PRICE   STATUS
-------------------------------------------------------------------------------------
201      John Doe             USB Hub                  3      30.00        Pending

Choose an option: 3
Status to filter: Pending
ID       CUSTOMER             PRODUCT                  QTY    UNIT PRICE   STATUS
-------------------------------------------------------------------------------------
102      Frank Miller         Gaming Headset           3      120.00       Pending
201      John Doe             USB Hub                  3      30.00        Pending
101      Alice Johnson        Wireless Mouse           3      25.50        Pending

Choose an option: 3
Status to filter: Shipped
No matching orders

Choose an option: 4
Order ID: 999
Additional quantity: 2
Order not found

Choose an option: 4
Order ID: 102
Additional quantity: 2
Quantity updated successfully:
ID       CUSTOMER             PRODUCT                  QTY    UNIT PRICE   STATUS
-------------------------------------------------------------------------------------
102      Frank Miller         Gaming Headset           5      120.00       Pending

Choose an option: 5
Order ID: 103
New status: delivered
Status updated successfully:
ID       CUSTOMER             PRODUCT                  QTY    UNIT PRICE   STATUS
-------------------------------------------------------------------------------------
103      Bob Smith            Mechanical Keyboard      2      89.99        Delivered

Choose an option: 5
Order ID: 999
New status: Shipped
Order not found

Choose an option: 5
Order ID: 101
New status: lost
Invalid status "lost". Order was not updated.
Allowed values: Pending, Shipped, Delivered, Cancelled

Choose an option: 6
Total value of all orders: 1267.48
Order ID to delete (0 to skip deletion): 103
Order 103 has status "Delivered" and cannot be deleted. Only Cancelled orders can be deleted.

Choose an option: 6
Total value of all orders: 1267.48
Order ID to delete (0 to skip deletion): 105
Order 105 deleted successfully.

Choose an option: 6
Total value of all orders: 997.48
Order ID to delete (0 to skip deletion): 999
Order not found

Choose an option: 7
ID       CUSTOMER             PRODUCT                  QTY    UNIT PRICE   STATUS
-------------------------------------------------------------------------------------
101      Alice Johnson        Wireless Mouse           3      25.50        Pending
102      Frank Miller         Gaming Headset           5      120.00       Pending
103      Bob Smith            Mechanical Keyboard      2      89.99        Delivered
104      Carol Davis          USB-C Cable              4      12.75        Delivered
201      John Doe             USB Hub                  3      30.00        Pending

Choose an option: 0
Goodbye!
```

The empty-table case was also checked: after `TRUNCATE TABLE Orders`, option 6
printed `Total value of all orders: 0.00` and option 7 printed
`No orders found.`

## Assumptions and notes

- Order IDs are entered by the user, matching the assignment's `order_id INT
  PRIMARY KEY` design. The application does not auto-generate them.
- Seeding inserts exactly 5 rows. Running it again produces one "already
  exists" message per row and does not stop the batch.
- Status input is case-insensitive (`shipped`, `SHIPPED`, `Shipped` all become
  `Shipped`). The database always stores the canonical spelling.
- Option 6 prints the total first. Enter `0` when you only want the total and do
  not want to delete anything.
- Only an order whose stored status is exactly `Cancelled` can be deleted.
- `unit_price` is `DOUBLE` because the assignment requires it. In a production
  money system, `DECIMAL`/`BigDecimal` would avoid floating-point rounding.
- A new JDBC connection is opened per DAO operation. This is simple and safe for
  a console application; a larger application would use a connection pool.
