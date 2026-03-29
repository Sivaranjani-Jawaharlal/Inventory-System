import java.util.*;

public class Main {
    private static Scanner sc = new Scanner(System.in);
    private static ProductDAO productDAO = new ProductDAO();
    private static SaleService saleService = new SaleService();
    private static RestockService restockService = new RestockService();
    private static List<CartItem> cart = new ArrayList<>();

    public static void main(String[] args) {
        while (true) {
            displayMainMenu();
            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
                continue;
            }

            try {
                switch (choice) {
                    case 1:
                        addProduct();
                        break;
                    case 2:
                        viewAllProducts();
                        break;
                    case 3:
                        checkLowStock();
                        break;
                    case 4:
                        billingSystem();
                        break;
                    case 5:
                        smartFeaturesMenu();
                        break;
                    case 6:
                        System.out.println("Thank you for using Inventory System!");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice! Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void displayMainMenu() {
        System.out.println("\n=== INVENTORY MANAGEMENT SYSTEM ===");
        System.out.println("1. Add Product");
        System.out.println("2. View All Products");
        System.out.println("3. Check Low Stock");
        System.out.println("4. Billing System");
        System.out.println("5. Smart Features");
        System.out.println("6. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void smartFeaturesMenu() throws Exception {
        while (true) {
            System.out.println("\n=== SMART FEATURES ===");
            System.out.println("1. Basic Restock Suggestions (Low Stock)");
            System.out.println("2. Top Selling Products Analysis");
            System.out.println("3. Smart Restock Suggestions");
            System.out.println("4. Back to Main Menu");
            System.out.print("Enter your choice: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    productDAO.suggestRestock();
                    break;
                case 2:
                    productDAO.topSellingProducts();
                    break;
                case 3:
                    restockService.displayRestockSuggestions();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private static void addProduct() throws Exception {
        System.out.println("\n--- Add New Product ---");
        Product p = new Product();

        System.out.print("Name: ");
        p.setName(sc.nextLine().trim());

        System.out.print("Price: ");
        try {
            p.setPrice(Double.parseDouble(sc.nextLine().trim()));
        } catch (NumberFormatException e) {
            System.out.println("Invalid price! Please enter a valid number.");
            return;
        }

        System.out.print("Quantity: ");
        try {
            p.setQuantity(Integer.parseInt(sc.nextLine().trim()));
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity! Please enter a whole number.");
            return;
        }

        System.out.print("Min Stock: ");
        try {
            p.setMinStock(Integer.parseInt(sc.nextLine().trim()));
        } catch (NumberFormatException e) {
            System.out.println("Invalid min stock! Please enter a whole number.");
            return;
        }

        productDAO.addProduct(p);
        System.out.println("Product added successfully!");
    }

    private static void viewAllProducts() throws Exception {
        List<Product> products = productDAO.getAllProducts();
        System.out.println("\n--- Product List ---");
        System.out.printf("%-5s %-20s %-10s %-10s %-10s\n",
                "ID", "Name", "Price", "Qty", "Min");
        System.out.println("------------------------------------------------");
        for (Product p : products) {
            System.out.printf("%-5d %-20s $%-9.2f %-10d %-10d\n",
                    p.getId(), p.getName(), p.getPrice(), p.getQuantity(), p.getMinStock());
        }
    }

    private static void checkLowStock() throws Exception {
        productDAO.checkLowStock();
    }

    private static void billingSystem() throws Exception {
        cart.clear();
        boolean continueShopping = true;

        System.out.println("\n=== BILLING SYSTEM ===");

        while (continueShopping) {
            System.out.println("\n--- Add Item to Cart ---");
            System.out.print("Enter Product ID (or 0 to finish): ");

            int productId;
            try {
                productId = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid Product ID! Please enter a number.");
                continue;
            }

            if (productId == 0) {
                continueShopping = false;
                break;
            }

            Product product = productDAO.getProductById(productId);
            if (product == null) {
                System.out.println("Product not found!");
                continue;
            }

            System.out.println("Product: " + product.getName());
            System.out.println("Price: $" + product.getPrice());
            System.out.println("Available Stock: " + product.getQuantity());

            System.out.print("Enter Quantity: ");
            int quantity;
            try {
                quantity = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(" Invalid quantity! Please enter a whole number.");
                continue;
            }

            if (productDAO.validateSale(productId, quantity)) {
                CartItem item = new CartItem(productId, quantity, product.getName(), product.getPrice());
                cart.add(item);
                System.out.println("Added to cart!");
            }
        }

        if (cart.isEmpty()) {
            System.out.println("Cart is empty!");
            return;
        }

        displayCart();

        System.out.print("\nProcess payment? (y/n): ");
        String confirm = sc.nextLine().trim();

        if (confirm.equalsIgnoreCase("y")) {
            saleService.processSale(cart);
            printReceipt();
        } else {
            System.out.println("Sale cancelled!");
        }
    }

    private static void displayCart() {
        System.out.println("\n=== YOUR CART ===");
        System.out.println("ID\tProduct\t\tQty\tPrice\tSubtotal");
        System.out.println("----------------------------------------");
        double total = 0;
        for (CartItem item : cart) {
            System.out.printf("%d\t%-15s%d\t$%-8.2f$%-8.2f\n",
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getSubtotal());
            total += item.getSubtotal();
        }
        System.out.println("----------------------------------------");
        System.out.printf("TOTAL: $%.2f\n", total);
    }

    private static void printReceipt() {
        System.out.println("\n=== RECEIPT ===");
        System.out.println("Date: " + new java.util.Date());
        System.out.println("----------------------------------------");
        displayCart();
        System.out.println("----------------------------------------");
        System.out.println("Thank you for shopping with us!");
        System.out.println("===========\n");
    }
}
