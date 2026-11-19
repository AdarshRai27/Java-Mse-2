package com.ecommerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contains every SQL statement used by the application.
 *
 * <p>All statements use PreparedStatement with "?" placeholders. This prevents
 * SQL injection and lets the driver reuse the compiled statement. Every JDBC
 * resource is closed automatically by try-with-resources.</p>
 */
public class OrderDAO {

    private static final String SELECT_COLUMNS =
            "SELECT order_id, customer_name, product_name, quantity, unit_price, status FROM Orders";

    /**
     * Inserts one order.
     *
     * @return true when inserted, false when the order id already exists
     */
    public boolean insertOrder(Order order) throws SQLException {
        String sql = "INSERT INTO Orders "
                + "(order_id, customer_name, product_name, quantity, unit_price, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, order.getOrderId());
            statement.setString(2, order.getCustomerName());
            statement.setString(3, order.getProductName());
            statement.setInt(4, order.getQuantity());
            statement.setDouble(5, order.getUnitPrice());
            statement.setString(6, order.getStatus());

            statement.executeUpdate();
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            // A duplicate primary key is an expected user error, not a crash.
            System.out.println("Order ID " + order.getOrderId() + " already exists - skipped");
            return false;

        } catch (SQLException e) {
            // Some MySQL driver versions report error 1062 as a plain SQLException.
            if (e.getErrorCode() == 1062) {
                System.out.println("Order ID " + order.getOrderId() + " already exists - skipped");
                return false;
            }
            throw e;
        }
    }

    /**
     * Returns orders with the given status and quantity greater than 2,
     * most expensive first. The query uses parameters for the status only
     * because the quantity limit is fixed by the assignment.
     */
    public List<Order> findByStatusWithQuantityGreaterThanTwo(String status) throws SQLException {
        String sql = SELECT_COLUMNS
                + " WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";

        // ArrayList keeps the SQL order and gives fast iteration for printing.
        List<Order> orders = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(mapRow(resultSet));
                }
            }
        }

        return orders;
    }

    /** Finds one order by primary key. Optional makes "not found" explicit. */
    public Optional<Order> findById(int orderId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE order_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Adds the given amount to the stored quantity.
     *
     * @return true when a row was updated, false when the id does not exist
     */
    public boolean increaseQuantity(int orderId, int additionalQuantity) throws SQLException {
        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, additionalQuantity);
            statement.setInt(2, orderId);

            // executeUpdate returns the number of affected rows.
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Changes the status of an existing order.
     *
     * @return true when a row was updated, false when the id does not exist
     */
    public boolean updateStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE Orders SET status = ? WHERE order_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newStatus);
            statement.setInt(2, orderId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Calculates SUM(quantity * unit_price) in the database.
     * SUM returns SQL NULL for an empty table, so that case is converted to 0.0.
     */
    public double getTotalOrderValue() throws SQLException {
        String sql = "SELECT SUM(quantity * unit_price) FROM Orders";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                double total = resultSet.getDouble(1);
                return resultSet.wasNull() ? 0.0 : total;
            }
            return 0.0;
        }
    }

    /**
     * Deletes one order by id.
     *
     * @return true when a row was deleted, false when the id does not exist
     */
    public boolean deleteOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM Orders WHERE order_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);
            return statement.executeUpdate() > 0;
        }
    }

    /** Returns every order, ordered by id for a predictable table. */
    public List<Order> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY order_id";

        List<Order> orders = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                orders.add(mapRow(resultSet));
            }
        }

        return orders;
    }

    /** Builds an Order from the current row. Kept in one place to avoid repetition. */
    private static Order mapRow(ResultSet resultSet) throws SQLException {
        return new Order(
                resultSet.getInt("order_id"),
                resultSet.getString("customer_name"),
                resultSet.getString("product_name"),
                resultSet.getInt("quantity"),
                resultSet.getDouble("unit_price"),
                resultSet.getString("status"));
    }
}
