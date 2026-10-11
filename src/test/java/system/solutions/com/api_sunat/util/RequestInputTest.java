package system.solutions.com.api_sunat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestInputTest {

    @Test
    void trimsTextAndMapsBlankOptionalTextToEmptyForFilters() {
        assertEquals("ACME", RequestInput.requiredText(" ACME ", "nombre", 20));
        assertEquals("", RequestInput.filterText("  ", "nombre", 20));
        assertNull(RequestInput.optionalText(null, "direccion", 20));
    }

    @Test
    void rejectsOversizedAndControlCharacterInput() {
        assertThrows(IllegalArgumentException.class,
                () -> RequestInput.requiredText("too long", "nombre", 4));
        assertThrows(IllegalArgumentException.class,
                () -> RequestInput.filterText("one\ntwo", "nombre", 20));
    }
}
