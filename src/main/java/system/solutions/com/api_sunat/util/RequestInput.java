package system.solutions.com.api_sunat.util;

public final class RequestInput {

    private RequestInput() {
    }

    public static String requiredText(String value, String field, int maxLength) {
        String normalized = normalize(value, field, maxLength);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return normalized;
    }

    public static String optionalText(String value, String field, int maxLength) {
        return normalize(value, field, maxLength);
    }

    public static String filterText(String value, String field, int maxLength) {
        if (value == null) {
            return "";
        }
        return normalize(value, field, maxLength);
    }

    private static String normalize(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " excede el tamaño permitido o contiene caracteres de control.");
        }
        return normalized;
    }
}
