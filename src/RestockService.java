import java.util.ArrayList;
import java.util.List;

public class RestockService {
    private ProductDAO productDAO = new ProductDAO();

    /**
     * Get smart restock suggestions with business logic
     */
    public List<RestockSuggestion> getSmartRestockSuggestions() throws Exception {
        List<Product> products = productDAO.getAllProductsWithSalesData();
        List<RestockSuggestion> suggestions = new ArrayList<>();

        for (Product product : products) {
            int totalSold = product.getTotalSold();
            int currentQty = product.getQuantity();
            int minStock = product.getMinStock();

            // Business logic for prioritization
            boolean isLowStock = currentQty <= minStock;
            boolean isHighSeller = totalSold > (minStock * InventoryConstants.HIGH_SALES_MULTIPLIER);

            RestockSuggestion suggestion = new RestockSuggestion();
            suggestion.setProductId(product.getId());
            suggestion.setProductName(product.getName());
            suggestion.setCurrentQuantity(currentQty);
            suggestion.setMinStock(minStock);
            suggestion.setTotalSold(totalSold);
            suggestion.setSuggestedQuantity(calculateSuggestedRestock(minStock, totalSold));

            if (isLowStock && isHighSeller) {
                suggestion.setPriority(RestockPriority.HIGH);
                suggestion.setReason("URGENT: Low stock and high demand!");
            } else if (isLowStock) {
                suggestion.setPriority(RestockPriority.MEDIUM);
                suggestion.setReason("Low stock alert");
            } else if (isHighSeller) {
                suggestion.setPriority(RestockPriority.MEDIUM);
                suggestion.setReason("High sales volume");
            } else if (currentQty < minStock * InventoryConstants.LOW_STOCK_WARNING_MULTIPLIER) {
                suggestion.setPriority(RestockPriority.LOW);
                suggestion.setReason("Approaching minimum stock level");
            }

            if (suggestion.getPriority() != null) {
                suggestions.add(suggestion);
            }
        }

        // Sort by priority (higher level first)
        suggestions.sort((s1, s2) -> Integer.compare(
                s2.getPriority().getLevel(),
                s1.getPriority().getLevel()
        ));

        return suggestions;
    }

    /**
     * Calculate suggested restock quantity based on business rules
     */
    private int calculateSuggestedRestock(int minStock, int totalSold) {
        int stockBasedSuggestion = minStock * InventoryConstants.SUGGESTED_RESTOCK_MULTIPLIER;
        int salesBasedSuggestion = totalSold / 2;

        if (totalSold > 0) {
            return Math.max(stockBasedSuggestion, salesBasedSuggestion);
        }

        return stockBasedSuggestion;
    }

    /**
     * Get high priority suggestions only
     */
    public List<RestockSuggestion> getHighPrioritySuggestions() throws Exception {
        List<RestockSuggestion> allSuggestions = getSmartRestockSuggestions();
        List<RestockSuggestion> highPriority = new ArrayList<>();

        for (RestockSuggestion suggestion : allSuggestions) {
            if (suggestion.getPriority() == RestockPriority.HIGH) {
                highPriority.add(suggestion);
            }
        }

        return highPriority;
    }

    /**
     * Get suggestions by priority level
     */
    public List<RestockSuggestion> getSuggestionsByPriority(RestockPriority priority) throws Exception {
        List<RestockSuggestion> allSuggestions = getSmartRestockSuggestions();
        List<RestockSuggestion> filtered = new ArrayList<>();

        for (RestockSuggestion suggestion : allSuggestions) {
            if (suggestion.getPriority() == priority) {
                filtered.add(suggestion);
            }
        }

        return filtered;
    }

    /**
     * Display suggestions in formatted way
     */
    public void displayRestockSuggestions() throws Exception {
        List<RestockSuggestion> suggestions = getSmartRestockSuggestions();

        System.out.println("\nSMART RESTOCK SUGGESTIONS");
        System.out.println("============================================");

        if (suggestions.isEmpty()) {
            System.out.println("All products are well-stocked!");
            return;
        }

        // Display statistics
        int highCount = 0, mediumCount = 0, lowCount = 0;
        for (RestockSuggestion suggestion : suggestions) {
            switch (suggestion.getPriority()) {
                case HIGH:
                    highCount++;
                    break;
                case MEDIUM:
                    mediumCount++;
                    break;
                case LOW:
                    lowCount++;
                    break;
            }
        }

        System.out.printf("\nSummary: %d HIGH | %d MEDIUM | %d LOW priority suggestions\n",
                highCount, mediumCount, lowCount);
        System.out.println("------------------------------------------------------------");

        // Display suggestions
        for (RestockSuggestion suggestion : suggestions) {
            System.out.println("\n" + suggestion.getFormattedDisplay());
        }

        // Add actionable recommendation
        if (highCount > 0) {
            System.out.println("\nACTION REQUIRED: Please restock HIGH priority items immediately!");
        } else if (mediumCount > 0) {
            System.out.println("\nACTION: Plan restocking for MEDIUM priority items this week.");
        } else if (lowCount > 0) {
            System.out.println("\nINFO: Monitor LOW priority items for future restocking needs.");
        }

        System.out.println("============================================\n");
    }

    /*
     Display only high priority suggestions
     */
    public void displayHighPrioritySuggestions() throws Exception {
        List<RestockSuggestion> highPriority = getHighPrioritySuggestions();

        System.out.println("\nURGENT RESTOCK NEEDS:");
        System.out.println("========================");

        if (highPriority.isEmpty()) {
            System.out.println("No urgent restocking needs at the moment.");
            return;
        }

        for (RestockSuggestion suggestion : highPriority) {
            System.out.println("\n" + suggestion.getFormattedDisplay());
        }
    }


    public String generateRestockReport() throws Exception {
        List<RestockSuggestion> suggestions = getSmartRestockSuggestions();
        StringBuilder report = new StringBuilder();

        // Count priorities from the already-fetched list (no extra DB calls)
        int highCount = 0, mediumCount = 0, lowCount = 0;
        for (RestockSuggestion s : suggestions) {
            switch (s.getPriority()) {
                case HIGH:   highCount++;   break;
                case MEDIUM: mediumCount++; break;
                case LOW:    lowCount++;    break;
            }
        }

        report.append("=== INVENTORY RESTOCK REPORT ===\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n\n");

        if (suggestions.isEmpty()) {
            report.append("All products are well-stocked. No restock needed.\n");
        } else {
            report.append(String.format("Total suggestions: %d\n", suggestions.size()));
            report.append(String.format("High priority: %d\n", highCount));
            report.append(String.format("Medium priority: %d\n", mediumCount));
            report.append(String.format("Low priority: %d\n\n", lowCount));

            for (RestockSuggestion suggestion : suggestions) {
                report.append(suggestion.toString()).append("\n");
            }
        }

        return report.toString();
    }

    /**
     * Check if any product needs urgent restock
     */
    public boolean hasUrgentRestockNeeds() throws Exception {
        return !getHighPrioritySuggestions().isEmpty();
    }

    /**
     * Get total suggested restock quantity across all products
     */
    public int getTotalSuggestedRestockQuantity() throws Exception {
        List<RestockSuggestion> suggestions = getSmartRestockSuggestions();
        int total = 0;
        for (RestockSuggestion suggestion : suggestions) {
            total += suggestion.getSuggestedQuantity();
        }
        return total;
    }
}
