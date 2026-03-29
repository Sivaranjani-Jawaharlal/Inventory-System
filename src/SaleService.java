import java.util.List;

public class SaleService {
    private ProductDAO productDAO = new ProductDAO();

    public void processSale(List<CartItem> cart) throws Exception {
        // Lightweight guard: no DB call needed to check an empty list
        if (cart == null || cart.isEmpty()) {
            System.out.println("Cart is empty!");
            return;
        }

        // Calculate total (business logic)
        double totalAmount = calculateTotal(cart);

        // Apply discounts if any (business logic)
        double discountedAmount = applyDiscounts(totalAmount);

        // Delegate to DAO — validation happens inside the transaction there
        productDAO.processSale(cart, discountedAmount);

        // Send notification (business logic)
        sendSaleNotification(cart, discountedAmount);
    }

    /**
     * Calculate total amount from cart
     */
    private double calculateTotal(List<CartItem> cart) {
        return cart.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }

    /**
     * Apply business logic for discounts
     */
    private double applyDiscounts(double totalAmount) {
        // Example: 10% discount for orders over $1000
        if (totalAmount > 1000) {
            double discount = totalAmount * 0.10;
            System.out.println("Bulk discount applied: $" + discount);
            return totalAmount - discount;
        }
        return totalAmount;
    }

    /**
     * Send notification (can be extended to email/SMS)
     */
    private void sendSaleNotification(List<CartItem> cart, double totalAmount) {
        System.out.println("Sale notification sent to admin");
        // Future enhancement: email, SMS, etc.
    }
}
