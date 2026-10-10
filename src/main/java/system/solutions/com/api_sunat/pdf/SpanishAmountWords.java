package system.solutions.com.api_sunat.pdf;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class SpanishAmountWords {

    private static final String[] UNITS = {
            "CERO", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS",
            "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };
    private static final String[] TENS = {
            "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] HUNDREDS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    public String toSoles(BigDecimal amount) {
        return toCurrency(amount, "PEN");
    }

    public String toCurrency(BigDecimal amount, String currencyCode) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("El monto debe ser un valor positivo.");
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long soles;
        int cents;
        try {
            soles = rounded.setScale(0, RoundingMode.DOWN).longValueExact();
            cents = rounded.remainder(BigDecimal.ONE).movePointRight(2).intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("El monto excede el rango soportado para conversión a letras.", exception);
        }
        if (soles > 999_999_999_999L) {
            throw new IllegalArgumentException("El monto excede el rango soportado para conversión a letras.");
        }
        String currency = switch (currencyCode == null ? "" : currencyCode.toUpperCase(Locale.ROOT)) {
            case "PEN" -> soles == 1 ? "SOL" : "SOLES";
            case "USD" -> soles == 1 ? "DÓLAR" : "DÓLARES";
            default -> throw new IllegalArgumentException("Moneda no soportada para conversión a letras.");
        };
        return "SON: " + apocopate(numberToWords(soles))
                + " CON " + String.format(Locale.ROOT, "%02d", cents) + "/100 " + currency;
    }

    private String numberToWords(long number) {
        if (number == 0) {
            return "CERO";
        }
        long billions = number / 1_000_000_000;
        long millions = number / 1_000_000;
        long thousands = (number / 1_000) % 1_000;
        long remainder = number % 1_000;
        StringBuilder result = new StringBuilder();
        if (billions > 0) {
            result.append(billions == 1 ? "MIL MILLONES" : forScale(billions) + " MIL MILLONES");
            millions %= 1_000;
        }
        if (millions > 0) {
            appendSpace(result);
            result.append(millions == 1 ? "UN MILLÓN" : forScale(millions) + " MILLONES");
        }
        if (thousands > 0) {
            appendSpace(result);
            result.append(thousands == 1 ? "MIL" : forScale(thousands) + " MIL");
        }
        if (remainder > 0) {
            appendSpace(result);
            result.append(underThousand(remainder));
        }
        return result.toString();
    }

    private String forScale(long value) {
        return apocopate(underThousand(value));
    }

    private String apocopate(String words) {
        return words.replaceAll("VEINTIUNO$", "VEINTIÚN").replaceAll("UNO$", "UN");
    }

    private String underThousand(long value) {
        if (value < 0 || value > 999) {
            throw new IllegalArgumentException("El grupo numérico debe estar entre cero y 999.");
        }
        if (value == 100) {
            return "CIEN";
        }
        int hundreds = (int) value / 100;
        int rest = (int) value % 100;
        StringBuilder result = new StringBuilder();
        if (hundreds > 0) {
            result.append(HUNDREDS[hundreds]);
        }
        if (rest > 0) {
            appendSpace(result);
            if (rest < 30) {
                result.append(UNITS[rest]);
            } else {
                result.append(TENS[rest / 10]);
                if (rest % 10 > 0) {
                    result.append(" Y ").append(UNITS[rest % 10]);
                }
            }
        }
        return result.toString();
    }

    private void appendSpace(StringBuilder text) {
        if (!text.isEmpty()) {
            text.append(' ');
        }
    }
}
