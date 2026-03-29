import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class ProductDAO {

    // ============ CORE VALIDATION LOGIC (Single Source of Truth) ============


    private ValidationResult validateProductStock(Connection conn, int productId, int requestedQty) throws Exception {
        ValidationResult result = new ValidationResult();

        // Check quantity validity
        if (requestedQty <= 0) {
            result.setValid(false);
            result.setMessage(InventoryConstants.INVALID_QUANTITY);
            return result;
        }

        String sql = "SELECT name, quantity FROM products WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                result.setValid(false);
                result.setMessage(InventoryConstants.PRODUCT_NOT_FOUND);
                return result;
            }

            String productName = rs.getString("name");
            int available = rs.getInt("quantity");

            if (available >= requestedQty) {
                result.setValid(true);
                result.setProductName(productName);
                result.setAvailableStock(available);
            } else {
                result.setValid(false);
                result.setMessage(String.format(InventoryConstants.INSUFFICIENT_STOCK,
                        productName, available, requestedQty));
                result.setProductName(productName);
                result.setAvailableStock(available);
            }
        }

        return result;
    }

    /**
     * Validation with existing connection (for transactions)
     */
    public boolean validateSale(Connection conn, int productId, int requestedQty) throws Exception {
        ValidationResult result = validateProductStock(conn, productId, requestedQty);
        if (!result.isValid()) {
            System.out.println(result.getMessage());
        }
        return result.isValid();
    }

    /**
     * Validation without connection (creates new connection)
     */
    public boolean validateSale(int productId, int requestedQty) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            ValidationResult result = validateProductStock(conn, productId, requestedQty);
            if (!result.isValid()) {
                System.out.println(result.getMessage());
            }
            return result.isValid();
        }
    }

    /**
     * Check stock availability (public facing)
     */
    public boolean validateStockAvailability(int productId, int requestedQty) throws Exception {
        return validateSale(productId, requestedQty);
    }

    // ============ PRODUCT CRUD OPERATIONS ============

    public void addProduct(Product p) throws Exception {
        String sql = "INSERT INTO products(name, price, quantity, min_stock) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setDouble(2, p.getPrice());
            ps.setInt(3, p.getQuantity());
            ps.setInt(4, p.getMinStock());

            ps.executeUpdate();
        }
    }

    public List<Product> getAllProducts() throws Exception {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products";

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setPrice(rs.getDouble("price"));
                p.setQuantity(rs.getInt("quantity"));
                p.setMinStock(rs.getInt("min_stock"));
                list.add(p);
            }
        }
        return list;
    }

    public Product getProductById(int id) throws Exception {
        String sql = "SELECT * FROM products WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("id"));
                product.setName(rs.getString("name"));
                product.setPrice(rs.getDouble("price"));
                product.setQuantity(rs.getInt("quantity"));
                product.setMinStock(rs.getInt("min_stock"));
                return product;
            }
        }
        return null;
    }

    public List<Product> getAllProductsWithSalesData() throws Exception {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.id, p.name, p.price, p.quantity, p.min_stock, " +
                "COALESCE(SUM(si.quantity), 0) as total_sold " +
                "FROM products p " +
                "LEFT JOIN sale_items si ON p.id = si.product_id " +
                "GROUP BY p.id, p.name, p.price, p.quantity, p.min_stock";

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setPrice(rs.getDouble("price"));
                p.setQuantity(rs.getInt("quantity"));
                p.setMinStock(rs.getInt("min_stock"));
                p.setTotalSold(rs.getInt("total_sold"));
                list.add(p);
            }
        }
        return list;
    }

    public void checkLowStock() throws Exception {
        String sql = "SELECT * FROM products WHERE quantity <= min_stock";

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n=== LOW STOCK ALERT ===");
            boolean hasLowStock = false;

            while (rs.next()) {
                hasLowStock = true;
                System.out.println("LOW STOCK: " + rs.getString("name") +
                        " (Quantity: " + rs.getInt("quantity") +
                        ", Min Stock: " + rs.getInt("min_stock") + ")");
            }

            if (!hasLowStock) {
                System.out.println("All products have sufficient stock.");
            }
        }
    }

    // ============ STOCK MANAGEMENT ============

    public void reduceStock(Connection conn, int productId, int quantitySold) throws Exception {
        String sql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantitySold);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    // ============ SALE PROCESSING ============

    public void processSale(List<CartItem> cart, double totalAmount) throws Exception {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Validate all items first (before any writes)
            for (CartItem item : cart) {
                if (!validateSale(conn, item.getProductId(), item.getQuantity())) {
                    System.out.println("Sale cancelled due to validation failure!");
                    conn.rollback();
                    return;
                }
            }

            // Insert sale record
            String saleSql = "INSERT INTO sales(total_amount, sale_date) VALUES (?, NOW())";
            int saleId;

            try (PreparedStatement ps = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDouble(1, totalAmount);
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    saleId = rs.getInt(1);
                } else {
                    throw new Exception("Failed to get sale ID");
                }
            }

            // Insert sale items with batch processing
            String itemSql = "INSERT INTO sale_items(sale_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";

            try (PreparedStatement psItem = conn.prepareStatement(itemSql)) {
                for (CartItem item : cart) {
                    psItem.setInt(1, saleId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setDouble(4, item.getPrice());
                    psItem.addBatch();
                }
                // Execute batch FIRST, then reduce stock — both within the same transaction
                psItem.executeBatch();
            }

            // Reduce stock AFTER successful batch insert (still within same transaction)
            for (CartItem item : cart) {
                reduceStock(conn, item.getProductId(), item.getQuantity());
            }

            conn.commit();
            System.out.println("\nSale completed successfully!");
            System.out.println("Sale ID: " + saleId);
            System.out.println("Total Amount: $" + totalAmount);

        } catch (Exception e) {
            // Explicit rollback on any failure to keep database consistent
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Transaction rolled back due to error.");
                } catch (Exception rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            System.err.println(" Sale failed: " + e.getMessage());
            throw e;
        } finally {
            // Always close the connection
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception closeEx) {
                    System.err.println(" Connection close failed: " + closeEx.getMessage());
                }
            }
        }
    }

    // ============ RESTOCK SUGGESTIONS ============

    public void suggestRestock() throws Exception {
        String sql = "SELECT id, name, quantity, min_stock FROM products WHERE quantity <= min_stock";

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n LOW STOCK RESTOCK SUGGESTIONS:");
            System.out.println("=====================================");

            boolean hasLowStock = false;
            while (rs.next()) {
                hasLowStock = true;
                int productId = rs.getInt("id");
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");
                int minStock = rs.getInt("min_stock");
                int suggestedRestock = minStock * InventoryConstants.SUGGESTED_RESTOCK_MULTIPLIER;

                System.out.printf(" [ID: %d] %s - Only %d left (Min Stock: %d)\n",
                        productId, name, quantity, minStock);
                System.out.printf(" Suggested Restock Quantity: %d units\n\n", suggestedRestock);
            }

            if (!hasLowStock) {
                System.out.println(" No products need restocking at the moment.");
            }
        }
    }

    public void topSellingProducts() throws Exception {
        String sql = "SELECT p.id, p.name, p.quantity, p.min_stock, " +
                "COALESCE(SUM(si.quantity), 0) as total_sold " +
                "FROM products p " +
                "LEFT JOIN sale_items si ON p.id = si.product_id " +
                "GROUP BY p.id, p.name, p.quantity, p.min_stock " +
                "ORDER BY total_sold DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nTOP SELLING PRODUCTS ANALYSIS:");
            System.out.println("===================================");
            System.out.printf("%-20s %-12s %-10s %s\n", "Product", "Total Sold", "Current Qty", "Status");
            System.out.println("--------------------------------------------------------");

            boolean hasSales = false;
            while (rs.next()) {
                hasSales = true;
                String name = rs.getString("name");
                int totalSold = rs.getInt("total_sold");
                int currentQty = rs.getInt("quantity");
                int minStock = rs.getInt("min_stock");

                boolean isHighSeller = totalSold > (minStock * InventoryConstants.HIGH_SALES_MULTIPLIER);

                String status;
                if (isHighSeller && currentQty <= minStock) {
                    status = " HIGH PRIORITY - RESTOCK NOW!";
                } else if (isHighSeller) {
                    status = "High Sales - Consider Restocking";
                } else if (currentQty <= minStock) {
                    status = "Low Stock - Restock Soon";
                } else {
                    status = " OK";
                }

                System.out.printf("%-20s %-12d %-10d %s\n",
                        name.length() > 18 ? name.substring(0, 15) + "..." : name,
                        totalSold, currentQty, status);
            }

            if (!hasSales) {
                System.out.println("No sales recorded yet.");
            }
        }
    }

    // ============ HELPER CLASS ============

    private static class ValidationResult {
        private boolean valid;
        private String message;
        private String productName;
        private int availableStock;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public int getAvailableStock() { return availableStock; }
        public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }
    }
}
