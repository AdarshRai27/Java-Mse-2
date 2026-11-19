package com.ecommerce;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Plain Java object that represents one row of the Orders table.
 */
public class Order {

    /**
     * Allowed status values. A Set is used because:
     * - duplicate values make no sense,
     * - membership checks are fast (hash-based),
     * - the allowed values are declared in one place.
     *
     * LinkedHashSet keeps the declaration order, so error messages always show
     * the statuses in the same order. The set is made read-only because it is
     * a shared constant.
     */
    public static final Set<String> VALID_STATUSES = Collections.unmodifiableSet(
            new LinkedHashSet<>(List.of("Pending", "Shipped", "Delivered", "Cancelled")));

    private final int orderId;
    private final String customerName;
    private final String productName;
    private final int quantity;
    private final double unitPrice;
    private final String status;

    public Order(int orderId, String customerName, String productName,
                 int quantity, double unitPrice, String status) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Converts user input such as "shipped" or "SHIPPED" to the canonical
     * stored form "Shipped".
     *
     * @return the canonical status, or null when the input is not allowed
     */
    public static String normalizeStatus(String rawStatus) {
        if (rawStatus == null) {
            return null;
        }
        String trimmed = rawStatus.trim();
        for (String validStatus : VALID_STATUSES) {
            if (validStatus.equalsIgnoreCase(trimmed)) {
                return validStatus;
            }
        }
        return null;
    }

    /** Returns the allowed statuses as one readable line for error messages. */
    public static String statusesAsText() {
        return String.join(", ", VALID_STATUSES);
    }

    @Override
    public String toString() {
        return String.format(
                "Order{id=%d, customer='%s', product='%s', quantity=%d, unitPrice=%.2f, status='%s'}",
                orderId, customerName, productName, quantity, unitPrice, status);
    }
}
