package com.xebialabs.migration;

import org.junit.Test;
import static org.junit.Assert.*;

public class MigrationTest {
    @Test public void testMigrationHasAGreeting() {
        Migration classUnderTest = new Migration();
        assertNotNull("migration should have a greeting", classUnderTest.getGreeting());
    }
}
