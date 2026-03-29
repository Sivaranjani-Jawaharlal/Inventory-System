public class InventoryConstants {
    // Restock multipliers
    public static final int HIGH_SALES_MULTIPLIER = 2;
    public static final int SUGGESTED_RESTOCK_MULTIPLIER = 2;
    public static final int LOW_STOCK_WARNING_MULTIPLIER = 2;

    // Validation messages
    public static final String INVALID_QUANTITY = "Invalid quantity! Quantity must be greater than 0.";
    public static final String PRODUCT_NOT_FOUND = "Product not found!";
    public static final String INSUFFICIENT_STOCK = "Not enough stock for %s! Available: %d, Requested: %d";

    private InventoryConstants() {
        // Private constructor to prevent instantiation
    }
}