package com.xebialabs.migration;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.zip.DataFormatException;

import com.xebialabs.migration.action.RepositoryAction;

/**
 * Modify configuration items and their properties based on a user supplied script.
 */
public class MigrationRunnerXLD extends MigrationRunner
{
    
    protected MigrationRunnerXLD(SystemType xlSystem, Connection dbconn,  DbType dbType, 
        Connection reportDbconn, DbType reportDbType, PrintStream os)
    {
        this.dbconn = dbconn;
        this.xlSystem = xlSystem;
        this.dbType = dbType;
        this.os = os;
        // At this time, these will be null
        this.reportDbconn = reportDbconn;
        this.reportDbType = reportDbType;
    }

   

    // ACTION METHODS =========================================================

    /**
     * Update a CI type.
     */
    @Override
    protected void updateCI(RepositoryAction action, boolean preview) throws IOException, SQLException, IllegalArgumentException
    {
        action.assertHasProperty("oldValue");
        action.assertHasProperty("newValue");

        int cnt = processAction(action, preview);

        os.println(String.format("%sUpdate CI from %s to %s. %d row(s) altered.", 
            (preview ? "[PREVIEW] ":""), 
            action.getProperties().get("oldValue"), 
            action.getProperties().get("newValue"), 
            cnt));
        if(action.getMessage() != null && !action.getMessage().isEmpty()){
            os.println("\tAttention: "+action.getMessage());
        }
    }

    /**
     * Update an XLR Task, so not supported for XLD
     */
    @Override
    protected void updateTask(RepositoryAction action, boolean preview) throws DataFormatException, IOException, SQLException, IllegalArgumentException
    {
        String msg = String.format("Action %s for Type %s is not supported for XL Deploy Migration.", action.getAction().name(), action.getType().name());
        throw new IllegalArgumentException(msg);
    }

    
}