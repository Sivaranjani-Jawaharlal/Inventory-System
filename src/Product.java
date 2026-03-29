public class Product {
    private int id;
    private String name;
    private double price;
    private int quantity;
    private int minStock;
    private int totalSold;  // New field for sales analysis

    public Product() {}

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getMinStock() { return minStock; }
    public int getTotalSold() { return totalSold; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public void setTotalSold(int totalSold) { this.totalSold = totalSold; }
}