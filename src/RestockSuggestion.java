public class RestockSuggestion {
    private int productId;
    private String productName;
    private int currentQuantity;
    private int minStock;
    private int totalSold;
    private int suggestedQuantity;
    private RestockPriority priority;
    private String reason;

    public RestockSuggestion() {}

    public RestockSuggestion(int productId, String productName, int currentQuantity,
                             int minStock, int totalSold, int suggestedQuantity,
                             RestockPriority priority, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.currentQuantity = currentQuantity;
        this.minStock = minStock;
        this.totalSold = totalSold;
        this.suggestedQuantity = suggestedQuantity;
        this.priority = priority;
        this.reason = reason;
    }

    // Getters
    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public int getMinStock() {
        return minStock;
    }

    public int getTotalSold() {
        return totalSold;
    }

    public int getSuggestedQuantity() {
        return suggestedQuantity;
    }

    public RestockPriority getPriority() {
        return priority;
    }

    public String getReason() {
        return reason;
    }

    // Setters
    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public void setTotalSold(int totalSold) {
        this.totalSold = totalSold;
    }

    public void setSuggestedQuantity(int suggestedQuantity) {
        this.suggestedQuantity = suggestedQuantity;
    }

    public void setPriority(RestockPriority priority) {
        this.priority = priority;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // Helper method to get formatted display
    public String getFormattedDisplay() {
        return String.format("%s - %s\n Product: %s (ID: %d)\n   Current Stock: %d | Min Stock: %d | Sold: %d times\n   Suggested Restock: %d units",
                priority.toString(), reason, productName, productId, currentQuantity, minStock, totalSold, suggestedQuantity);
    }

    @Override
    public String toString() {
        return String.format("RestockSuggestion{productId=%d, productName='%s', currentQuantity=%d, " +
                        "minStock=%d, totalSold=%d, suggestedQuantity=%d, priority=%s, reason='%s'}",
                productId, productName, currentQuantity, minStock, totalSold,
                suggestedQuantity, priority, reason);
    }
}