package com.xebialabs.migration.action;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepositoryActionTest
{
    @Test public void testOrder()
    {
        List<RepositoryAction> actions = new ArrayList<RepositoryAction>();

        actions.add(new RepositoryAction(4, ActionType.UPDATE, ItemType.CI));
        actions.add(new RepositoryAction(1, ActionType.UPDATE, ItemType.CI));
        actions.add(new RepositoryAction(5, ActionType.UPDATE, ItemType.CI));
        actions.add(new RepositoryAction(3, ActionType.UPDATE, ItemType.CI));
        actions.add(new RepositoryAction(2, ActionType.UPDATE, ItemType.CI));

        int next = 0;
        Collections.sort(actions, (o1, o2) -> o1.compareTo(o2));
        for ( RepositoryAction action : actions)
        {
            next++;
            assertTrue(String.format("Order failure, got %d but expected %d", action.getOrder(), next), next == action.getOrder());
        }   
    }

    @Test public void testAssertHas()
    {
        RepositoryAction ra1 = new RepositoryAction(4, ActionType.UPDATE, ItemType.CI);
        ra1.addProperty("thing1", "value1");
        ra1.addProperty("thing2", "value2");

        try
        {
            ra1.assertHasProperty("thing1");
        }
        catch (IllegalArgumentException ex)
        {
            fail("Should not have thrown exception for 'thing1'");
        }

        try
        {
            ra1.assertHasProperty("thing3");
            fail("Should have thrown exception for 'thing3'");
        }
        catch (IllegalArgumentException ex)
        {
        }
    }
}