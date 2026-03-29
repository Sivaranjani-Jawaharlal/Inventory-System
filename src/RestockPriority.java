// BUG FIXED: Removed unnecessary "import java.lang.*;" (auto-imported by JVM)
// BUG FIXED: Removed illegal compareTo(RestockPriority other) method.
//            Java Enum<E> already declares a FINAL compareTo(E o) method from
//            Comparable<E>. Defining it again causes a compile-time error:
//            "cannot override final method from Enum".
//            RestockService already sorts using getPriority().getLevel() directly,
//            so this override was both illegal and unnecessary.

public enum RestockPriority {
    HIGH(3, "HIGH PRIORITY"),
    MEDIUM(2, "MEDIUM PRIORITY"),
    LOW(1, "LOW PRIORITY");

    private final int level;
    private final String displayName;

    RestockPriority(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
