package system.solutions.com.api_sunat.pdf;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpanishAmountWordsTest {

    private final SpanishAmountWords converter = new SpanishAmountWords();

    @Test
    void convierteCientoCincuentaSoles() {
        assertEquals("SON: CIENTO CINCUENTA CON 00/100 SOLES", converter.toSoles(new BigDecimal("150.00")));
    }

    @Test
    void convierteUnSolYCentimosRedondeados() {
        assertEquals("SON: UN CON 01/100 SOL", converter.toSoles(new BigDecimal("1.005")));
    }

    @Test
    void convierteMilesYMillonesEnFormaGramatical() {
        assertEquals("SON: CIENTO VEINTIÚN MIL UN CON 50/100 SOLES",
                converter.toSoles(new BigDecimal("121001.50")));
        assertEquals("SON: UN MILLÓN CON 00/100 SOLES",
                converter.toSoles(new BigDecimal("1000000.00")));
        assertEquals("SON: MIL MILLONES CON 00/100 SOLES",
                converter.toSoles(new BigDecimal("1000000000.00")));
    }
}
