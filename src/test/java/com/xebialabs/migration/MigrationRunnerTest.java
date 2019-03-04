/**
 * Copyright 2019 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xebialabs.migration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import com.xebialabs.migration.action.ActionType;
import com.xebialabs.migration.action.ItemType;
import com.xebialabs.migration.action.RepositoryAction;

import org.junit.Test;

public class MigrationRunnerTest 
{
    @Test public void testCreate() 
    {
        MigrationRunner mr = new MigrationRunnerMock(SystemType.XLD, null,  DbType.DERBY, null, null, System.out);
        assertNotNull("migration runner should exist", mr);
    }

    @Test public void testRename() throws DataFormatException, IOException, SQLException
    {
        List<RepositoryAction> actions = new ArrayList<RepositoryAction>();
        
        RepositoryAction ra = new RepositoryAction(1, ActionType.UPDATE, ItemType.CI);
        ra.addProperty("oldName", "openshift.ResourceModule");
        ra.addProperty("newName", "openshift.Resources");

        actions.add(ra);

        MigrationRunnerMock mr = new MigrationRunnerMock(SystemType.XLD, null, DbType.DERBY, null, null, System.out);


        mr.process(actions, true);

        assertTrue("rename method not called", mr.renameCiCnt > 0);
    }

    @Test public void testMakeSqlKey()
    {
        MigrationRunner mr = new MigrationRunnerMock(SystemType.XLD, null, DbType.DERBY, null, null, System.out);

        String key = mr.makeSqlKey(new RepositoryAction(1, ActionType.UPDATE, ItemType.CI), false);
        assertTrue(String.format("Got wrong key '%s'", key), (key != null && key.equals("XLD.UPDATE.CI")));

        key = mr.makeSqlKey(new RepositoryAction(1, ActionType.UPDATE, ItemType.CI), false, "children");
        assertTrue(String.format("Got wrong key '%s'", key), (key != null && key.equals("XLD.UPDATE.CI.children")));
    }

    @Test public void testLookupSql()
    {
        MigrationRunner mr = new MigrationRunnerMock(SystemType.XLD, null, DbType.DERBY, null, null, System.out);

        String sql = mr.lookupSql(new RepositoryAction(1, ActionType.UPDATE, ItemType.CI), false);
        assertTrue(String.format("Got wrong sql '%s'", sql), sql.startsWith("update XLD_CIS set \"ci_type\""));

        sql = mr.lookupSql(new RepositoryAction(1, ActionType.UPDATE, ItemType.CI), true);
        assertTrue(String.format("Got wrong sql '%s'", sql), sql.startsWith("select count(\"ci_type\") from XLD_CIS"));

        try
        {
            sql = mr.lookupSql(new RepositoryAction(1, ActionType.CREATE, ItemType.CI), false);
            fail("Should have thrown exception for 'CREATE CI'");
        }
        catch (IllegalArgumentException ex)
        {
        }

        sql = mr.lookupSql(new RepositoryAction(1, ActionType.DELETE, ItemType.CI_PROPERTY), false);
        assertTrue(String.format("Got wrong sql '%s'", sql), sql.startsWith("delete from XLD_CI_PROPERTIES where \"name\" = :propertyName and \"ci_id\""));
    }
}

class MigrationRunnerMock extends MigrationRunner 
{
    int renameCiCnt = 0;

    public MigrationRunnerMock(SystemType xlSystem, Connection dbconn, DbType dbType, Connection reportDbconn, DbType reportDbType, PrintStream os)
    {
        //MigrationRunner runner = MigrationRunnerFactory.getMigrationRunner(xlSystem, dbconn, dbType, reportDbconn, reportDbType, os);
        this.dbconn = dbconn;
        this.xlSystem = xlSystem;
        this.dbType = dbType;
        this.os = os;
        this.reportDbconn = reportDbconn;
        this.reportDbType = reportDbType;
    }

    // mocked methods
    @Override
    protected void updateCI(RepositoryAction action, boolean preview) throws IOException
    {
        this.renameCiCnt++;
    }

    // mocked methods
    @Override
    protected void updateTask(RepositoryAction action, boolean preview) throws IOException
    {
        this.renameCiCnt++;
    }
}

