package com.ecommerce;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console menu for the Order Management System.
 *
 * <p>All database work is delegated to OrderDAO. This class only handles
 * user input, validation, and printing results.</p>
 */
public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final OrderDAO ORDER_DAO = new OrderDAO();

    public static void main(String[] args) {
        System.out.println("=== E-Commerce Order Management System ===");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");

            try {
                switch (choice) {
                    case 1 -> seedSampleOrders();
                    case 2 -> insertOrderFromInput();
                    case 3 -> filterOrdersByStatus();
                    case 4 -> increaseQuantity();
                    case 5 -> updateStatus();
                    case 6 -> showTotalAndDeleteCancelledOrder();
                    case 7 -> displayAllOrders();
                    case 0 -> {
                        running = false;
                        System.out.println("Goodbye!");
                    }
                    default -> System.out.println("Invalid option. Please choose 0-7.");
                }
            } catch (SQLException e) {
                // A database problem should not terminate the whole application.
                System.out.println("Database error: " + e.getMessage());
                System.out.println("Check that MySQL is running and DB_URL/DB_USER/DB_PASS are correct.");
            }
        }

        SCANNER.close();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("----- Menu -----");
        System.out.println("1. Seed 5 sample orders");
        System.out.println("2. Insert a new order");
        System.out.println("3. Filter by status (quantity > 2, price high to low)");
        System.out.println("4. Increase quantity of an order");
        System.out.println("5. Update order status");
        System.out.println("6. Show total value / delete a cancelled order");
        System.out.println("7. Show all orders");
        System.out.println("0. Exit");
    }

    /**
     * Option (a): inserts 5 realistic rows.
     * A List keeps the batch in a fixed order; insertOrder skips duplicates,
     * so one duplicate never stops the remaining inserts.
     */
    private static void seedSampleOrders() throws SQLException {
        List<Order> sampleOrders = List.of(
                new Order(101, "Alice Johnson", "Wireless Mouse", 3, 25.50, "Pending"),
                new Order(102, "Frank Miller", "Gaming Headset", 3, 120.00, "Pending"),
                new Order(103, "Bob Smith", "Mechanical Keyboard", 2, 89.99, "Shipped"),
                new Order(104, "Carol Davis", "USB-C Cable", 4, 12.75, "Delivered"),
                new Order(105, "Eva Wilson", "Laptop Stand", 6, 45.00, "Cancelled")
        );

        int inserted = 0;
        for (Order order : sampleOrders) {
            if (ORDER_DAO.insertOrder(order)) {
                inserted++;
            }
        }

        System.out.println("Seed finished: " + inserted + " inserted, "
                + (sampleOrders.size() - inserted) + " skipped (order id already existed).");
    }

    /** Option (a): inserts one order typed by the user. */
    private static void insertOrderFromInput() throws SQLException {
        int orderId = readPositiveInt("Order ID: ");
        String customerName = readNonBlank("Customer name: ");
        String productName = readNonBlank("Product name: ");
        int quantity = readPositiveInt("Quantity: ");
        double unitPrice = readNonNegativeDouble("Unit price: ");
        String status = readValidStatus("Status (Pending/Shipped/Delivered/Cancelled): ");

        // The status was normalized by readValidStatus, so the database always
        // stores one consistent spelling.
        Order order = new Order(orderId, customerName, productName, quantity, unitPrice, status);

        if (ORDER_DAO.insertOrder(order)) {
            System.out.println("Order inserted successfully:");
            printOrders(List.of(order));
        }
    }

    /** Option (b): filters by status and quantity > 2, sorted by price descending. */
    private static void filterOrdersByStatus() throws SQLException {
        String rawStatus = readLine("Status to filter: ");
        String status = Order.normalizeStatus(rawStatus);

        if (status == null) {
            System.out.println("Invalid status. Allowed values: " + Order.statusesAsText());
            return;
        }

        List<Order> orders = ORDER_DAO.findByStatusWithQuantityGreaterThanTwo(status);

        if (orders.isEmpty()) {
            System.out.println("No matching orders");
            return;
        }

        printOrders(orders);
    }

    /** Option (c): adds a positive amount to an existing quantity. */
    private static void increaseQuantity() throws SQLException {
        int orderId = readPositiveInt("Order ID: ");
        int extraQuantity = readPositiveInt("Additional quantity: ");

        boolean updated = ORDER_DAO.increaseQuantity(orderId, extraQuantity);

        if (!updated) {
            System.out.println("Order not found");
            return;
        }

        Optional<Order> updatedOrder = ORDER_DAO.findById(orderId);
        System.out.println("Quantity updated successfully:");
        updatedOrder.ifPresent(order -> printOrders(List.of(order)));
    }

    /**
     * Option (d): retrieves the order first, then checks existence and the
     * allowed status before updating. Each failure has its own message.
     */
    private static void updateStatus() throws SQLException {
        int orderId = readPositiveInt("Order ID: ");

        Optional<Order> existingOrder = ORDER_DAO.findById(orderId);
        if (existingOrder.isEmpty()) {
            System.out.println("Order not found");
            return;
        }

        String rawStatus = readLine("New status: ");
        String newStatus = Order.normalizeStatus(rawStatus);
        if (newStatus == null) {
            System.out.println("Invalid status \"" + rawStatus + "\". Order was not updated.");
            System.out.println("Allowed values: " + Order.statusesAsText());
            return;
        }

        boolean updated = ORDER_DAO.updateStatus(orderId, newStatus);
        if (!updated) {
            System.out.println("Order not found");
            return;
        }

        // Re-fetch so the printed details come from the database, not from memory.
        Optional<Order> updatedOrder = ORDER_DAO.findById(orderId);
        System.out.println("Status updated successfully:");
        updatedOrder.ifPresent(order -> printOrders(List.of(order)));
    }

    /**
     * Option (e): prints SUM(quantity * unit_price), then deletes an order only
     * when its status is exactly "Cancelled".
     */
    private static void showTotalAndDeleteCancelledOrder() throws SQLException {
        double total = ORDER_DAO.getTotalOrderValue();
        System.out.printf("Total value of all orders: %.2f%n", total);

        int orderId = readNonNegativeInt("Order ID to delete (0 to skip deletion): ");
        if (orderId == 0) {
            return;
        }

        Optional<Order> orderToDelete = ORDER_DAO.findById(orderId);
        if (orderToDelete.isEmpty()) {
            System.out.println("Order not found");
            return;
        }

        Order order = orderToDelete.get();
        if (!"Cancelled".equals(order.getStatus())) {
            System.out.println("Order " + orderId + " has status \"" + order.getStatus()
                    + "\" and cannot be deleted. Only Cancelled orders can be deleted.");
            return;
        }

        if (ORDER_DAO.deleteOrder(orderId)) {
            System.out.println("Order " + orderId + " deleted successfully.");
        } else {
            System.out.println("Order not found");
        }
    }

    /** Option (f): prints every order. */
    private static void displayAllOrders() throws SQLException {
        List<Order> orders = ORDER_DAO.findAll();

        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }

        printOrders(orders);
    }

    private static void printOrders(List<Order> orders) {
        System.out.printf("%-8s %-20s %-24s %-6s %-12s %-10s%n",
                "ID", "CUSTOMER", "PRODUCT", "QTY", "UNIT PRICE", "STATUS");
        System.out.println("-".repeat(85));

        for (Order order : orders) {
            System.out.printf("%-8d %-20s %-24s %-6d %-12.2f %-10s%n",
                    order.getOrderId(),
                    order.getCustomerName(),
                    order.getProductName(),
                    order.getQuantity(),
                    order.getUnitPrice(),
                    order.getStatus());
        }
    }

    // ---------- Input helpers: each one re-prompts until the input is valid ----------

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a whole number.");
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("Value must be greater than 0.");
        }
    }

    private static int readNonNegativeInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value >= 0) {
                return value;
            }
            System.out.println("Value cannot be negative.");
        }
    }

    private static double readNonNegativeDouble(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                double value = Double.parseDouble(input);
                if (Double.isFinite(value) && value >= 0) {
                    return value;
                }
                System.out.println("Value must be a finite number greater than or equal to 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid price. Please enter a number.");
            }
        }
    }

    private static String readNonBlank(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    private static String readValidStatus(String prompt) {
        while (true) {
            String rawStatus = readLine(prompt);
            String status = Order.normalizeStatus(rawStatus);
            if (status != null) {
                return status;
            }
            System.out.println("Invalid status. Allowed values: " + Order.statusesAsText());
        }
    }
}
