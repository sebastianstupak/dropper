package com.examplemod.items;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RubySword item.
 * Co-located with implementation.
 */
class RubySwordTest {

    @Test
    void testRubySwordCreation() {
        RubySword sword = new RubySword();
        assertNotNull(sword, "RubySword should be created successfully");
    }

    @Test
    void testRubySwordProperties() {
        RubySword sword = new RubySword();
        // Basic test - ensure item exists
        assertNotNull(sword.toString());
    }
}
