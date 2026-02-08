package com.examplemod;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Example test class demonstrating co-located tests.
 * Test files live next to the code they test.
 */
class ExampleModTest {

    @Test
    void testModIdIsCorrect() {
        assertEquals("example-mod", ExampleMod.MOD_ID);
    }

    @Test
    void testLoggerNotNull() {
        assertNotNull(ExampleMod.LOGGER);
    }
}
