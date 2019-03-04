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

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import com.xebialabs.migration.action.ItemType;
import com.xebialabs.migration.action.PropertyValueType;
import com.xebialabs.migration.action.RepositoryAction;
import com.xebialabs.migration.util.NamedParamStatement;

/**
 * Modify configuration items and their properties based on a user supplied script.
 */
public abstract class MigrationRunner 
{
    /**
     * These SQL statements are the real workhorse of the migration runner.  
     * There are separate statements for preview mode.  These should return a count like the actual sql statement but they must not alter
     * any database tables.
     * 
     * NOTE: These have been tested for MySQL.
     */
    protected static Map<String, Map<String, String>> SQL_STMTS;
    
    static {
        SQL_STMTS = SqlMap.getAllMap();
    }

    protected Connection dbconn;
    protected SystemType xlSystem;
    protected DbType dbType;
    protected PrintStream os;
    protected Connection reportDbconn;
    protected DbType reportDbType;

    /**
     * Perform the given actions in order.  Preview mode will report progress but not alter the repository.
     */
    public void process(List<RepositoryAction> actions, boolean preview) throws DataFormatException, IOException, SQLException, IllegalArgumentException
    {
        // make sure actions are processed in order
        Collections.sort(actions, (o1, o2) -> o1.compareTo(o2));

        for ( RepositoryAction action : actions )
        {
            if ( ItemType.CI.equals(action.getType()))
            {
                switch (action.getAction() )
                {
                    case UPDATE :
                        updateCI(action, preview);
                        break;

                    case DELETE :
                        deleteCI(action, preview);
                        break;

                    case COPY :
                        copyCI(action, preview);
                        break;

                    default:
                        String msg = String.format("Action %s is not supported for type %s", action.getAction().name(), action.getType().name());
                        throw new IllegalArgumentException(msg);
                }
            }
            else if ( ItemType.CI_PROPERTY.equals(action.getType()))
            {
                switch (action.getAction() )
                {
                    case CREATE :
                        createCIProperty(action, preview);
                        break;

                    case UPDATE :
                        updateCIProperty(action, preview);
                        break;

                    case DELETE :
                        deleteCIProperty(action, preview);
                        break;

                    default:
                        String msg = String.format("Action %s is not supported for type %s", action.getAction().name(), action.getType().name());
                        throw new IllegalArgumentException(msg);
                }
            }
            else if ( ItemType.TASK.equals(action.getType()))
            {
                switch (action.getAction() )
                {
                    case UPDATE :
                        updateTask(action, preview);
                        break;

                    default:
                        String msg = String.format("Action %s is not supported for type %s", action.getAction().name(), action.getType().name());
                        throw new IllegalArgumentException(msg);
                }
            }
            else
            {
                String msg = String.format("Type %s is not supported", action.getType().name());
                throw new IllegalArgumentException(msg);
            }
        }
    }

    // ACTION METHODS =========================================================

    /**
     * Update a CI type.
     */
    abstract void updateCI(RepositoryAction action, boolean preview) throws DataFormatException, IOException, SQLException, IllegalArgumentException;
    
    /**
     * Update a task type.
     */
    abstract void updateTask(RepositoryAction action, boolean preview) throws DataFormatException, IOException, SQLException, IllegalArgumentException;
    
    /**
     * Delete a CI and all of its properties.
     */
    protected void deleteCI(RepositoryAction action, boolean preview) throws IOException, SQLException, IllegalArgumentException
    {
        action.assertHasProperty("ciName");

        String ciName = action.getProperties().get("ciName");

        if ( !preview )
        {
            int cnt = processAction(action, preview, "children");

            os.println(String.format("Deleted Properties for CI %s. %d row(s) removed.", 
                ciName, 
                cnt));
            if(action.getMessage() != null && !action.getMessage().isEmpty()){
                os.println("\tAttention: "+action.getMessage());
            }
        }

        // delete parent ci or preview
        int cnt = processAction(action, preview);

        os.println(String.format("%sDeleted CI %s. %d row(s) removed.", 
            (preview ? "[PREVIEW] ":""), 
            ciName, 
            cnt));
        if(action.getMessage() != null && !action.getMessage().isEmpty()){
            os.println("\tAttention: "+action.getMessage());
        }
    }

    /**
     * Copy a CI type.
     */
    protected void copyCI(RepositoryAction action, boolean preview) throws IOException, SQLException, IllegalArgumentException
    {
        action.assertHasProperty("oldValue");
        action.assertHasProperty("newValue");
        action.assertHasProperty("newNameSuffix");

        // create trigger
        if ( !preview )
        {
            String sql = lookupSql(action, preview, "createtrigger");
            // this.dbconn.createStatement().execute(sql);
            /* 
            * create trigger requires multiple statements so we must use batch
            */
            Statement s = this.dbconn.createStatement();
            String[] stringArray = sql.split(";");
            for (int i = 0; i < stringArray.length; i++) {
                s.addBatch(stringArray[i]);
             }
            try {
                s.executeBatch();
            } catch (SQLException e){
                System.out.println(e.getMessage());
                if(e.getMessage() != null && e.getMessage().contains("does not exist")){
                    System.out.println("Ignoring SQLException - "+e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        int cnt = processAction(action, preview);

        // drop trigger
        if ( !preview )
        {
            String sql = lookupSql(action, preview, "droptrigger");
            try {
                this.dbconn.createStatement().execute(sql);
            } catch (SQLException e){
                System.out.println(e.getMessage());
                if(e.getMessage() != null && e.getMessage().contains("does not exist")){
                    System.out.println("Ignoring SQLException - "+e.getMessage());
                } else {
                    throw e;
                }
            }  
            
        }

        os.println(String.format("%sCopy CI from %s to %s. New Name Suffix is %s. %d row(s) altered.", 
            (preview ? "[PREVIEW] ":""), 
            action.getProperties().get("oldValue"), 
            action.getProperties().get("newValue"),
            action.getProperties().get("newNameSuffix"), 
            cnt));
        if(action.getMessage() != null && !action.getMessage().isEmpty()){
            os.println("\tAttention: "+action.getMessage());
        }
    }

    /**
     * Add a new property for the CI.
     */
    protected void createCIProperty(RepositoryAction action, boolean preview) throws IOException, SQLException, IllegalArgumentException
    {
        action.assertHasProperty("ciName");
        action.assertHasProperty("propertyName");

        // create trigger
        if ( !preview )
        {
            String sql = lookupSql(action, preview, "createtrigger");
            // this.dbconn.createStatement().execute(sql);
            /* 
            * create trigger requires multiple statements so we must use batch
            */
            Statement s = this.dbconn.createStatement();
            String[] stringArray = sql.split(";");
            for (int i = 0; i < stringArray.length; i++) {
                s.addBatch(stringArray[i]);
             }
            try {
                s.executeBatch();
            } catch (SQLException e){
                System.out.println(e.getMessage());
                if(e.getMessage() != null && e.getMessage().contains("does not exist")){
                    System.out.println("Ignoring SQLException - "+e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        // insert property
        String value = "";
        int cnt = -1;
        if ( action.getProperties().containsKey(PropertyValueType.STRING_VALUE.name().toLowerCase()) )
        {
            value = action.getProperties().get(PropertyValueType.STRING_VALUE.name().toLowerCase()); // for log msg below
            cnt = processAction(action, preview, PropertyValueType.STRING_VALUE.name());
        } else if ( action.getProperties().containsKey(PropertyValueType.BOOLEAN_VALUE.name().toLowerCase()) )
        {
            value = action.getProperties().get(PropertyValueType.BOOLEAN_VALUE.name().toLowerCase()); // for log msg below
            cnt = processAction(action, preview, PropertyValueType.BOOLEAN_VALUE.name());
        }
        else if ( action.getProperties().containsKey(PropertyValueType.INTEGER_VALUE.name().toLowerCase()) )
        {
            value = action.getProperties().get(PropertyValueType.INTEGER_VALUE.name().toLowerCase()); // for log msg below
            cnt = processAction(action, preview, PropertyValueType.INTEGER_VALUE.name());
        }
        else if ( action.getProperties().containsKey(PropertyValueType.DATE_VALUE.name().toLowerCase()) )
        {
            value = action.getProperties().get(PropertyValueType.DATE_VALUE.name().toLowerCase()); // for log msg below
            cnt = processAction(action, preview, PropertyValueType.DATE_VALUE.name());
        }
        else
        {
            throw new IllegalArgumentException("not implemented");
        }

        // drop trigger
        if ( !preview )
        {
            String sql = lookupSql(action, preview, "droptrigger");
            try {
                this.dbconn.createStatement().execute(sql);
            } catch (SQLException e){
                System.out.println(e.getMessage());
                if(e.getMessage() != null && e.getMessage().contains("does not exist")){
                    System.out.println("Ignoring SQLException - "+e.getMessage());
                } else {
                    throw e;
                }
            }   
        }

        os.println(String.format("%sCreate CI property '%s' = '%s' for CI '%s'. %d row(s) altered.", 
            (preview ? "[PREVIEW] ":""), 
            action.getProperties().get("propertyName"), 
            value, 
            action.getProperties().get("ciName"), 
            cnt));
        if(action.getMessage() != null && !action.getMessage().isEmpty()){
            os.println("\tAttention: "+action.getMessage());
        }
    }

    /**
     * Update the name of a property.
     */
    protected void updateCIProperty(RepositoryAction action, boolean preview) throws IOException, SQLException, IllegalArgumentException
    {
        action.assertHasProperty("ciName");
        action.assertHasProperty("oldValue");
        action.assertHasProperty("newValue");

        int cnt = processAction(action, preview);

        os.println(String.format("%sUpdate CI %s property name from %s to %s. %d row(s) altered.", 
            (preview ? "[PREVIEW] ":""), 
            action.getProperties().get("ciName"), 
            action.getProperties().get("oldValue"), 
            action.getProperties().get("newValue"), 
            cnt));
        if(action.getMessage() != null && !action.getMessage().isEmpty()){
            os.println("\tAttention: "+action.getMessage());
        }
    }

    /**
     * Remove an unused property.  E.g. a property that was present for the old CI but not the new one.
     */
    protected void deleteCIProperty(RepositoryAction action, boolean preview) throws IOException, SQLException, IllegalArgumentException
    {
        action.assertHasProperty("ciName");
        action.assertHasProperty("propertyName");

        int cnt = processAction(action, preview);

        os.println(String.format("%sDelete CI %s property %s. %d row(s) altered.", 
            (preview ? "[PREVIEW] ":""), 
            action.getProperties().get("ciName"), 
            action.getProperties().get("propertyName"), 
            cnt));
        if(action.getMessage() != null && !action.getMessage().isEmpty()){
            os.println("\tAttention: "+action.getMessage());
        }
    }

    // SUPPORTING METHODS =====================================================

    /**
     * Find and execute the appropriate SQL for the action and object type.
     */
    protected int processAction(RepositoryAction action, boolean preview) throws SQLException
    {
        return processAction(action, preview, null);
    }

    protected int processAction(RepositoryAction action, boolean preview, String qualifier) throws SQLException
    {
        String sql = lookupSql(action, preview, qualifier);
       
        NamedParamStatement pstmt = new NamedParamStatement(this.dbconn, sql);
        ResultSet countSet = null;
        
        try
        {
            for (int i = 0; i < pstmt.getFields().size(); i++)
            {
                String key = pstmt.getFields().get(i);
                if(action.getProperties().containsKey(key)){
                    if(key.equalsIgnoreCase(PropertyValueType.STRING_VALUE.name())){
                        pstmt.setString(i, action.getProperties().get(key));
                    } else if(key.equalsIgnoreCase(PropertyValueType.BOOLEAN_VALUE.name())){
                        pstmt.setBoolean(i, action.getProperties().get(key));
                    } else if(key.equalsIgnoreCase(PropertyValueType.INTEGER_VALUE.name())){
                        pstmt.setInteger(i, action.getProperties().get(key));
                    } else if(key.equalsIgnoreCase(PropertyValueType.DATE_VALUE.name())){
                        pstmt.setDate(i, action.getProperties().get(key));
                    } else {
                        pstmt.setString(i, action.getProperties().get(key));
                    }  
                }
            }
            
            if (preview){
                countSet = pstmt.executeQuery();
                if (countSet != null && countSet.next()){
                    return countSet.getInt(1);
                } else {
                    return 0;
                }
            } else {
                return pstmt.executeUpdate();
            }
        }
        finally
        {
            try { if (countSet != null) countSet.close(); } catch (Exception e) {};
            try { if (pstmt != null) pstmt.close(); } catch (Exception e) {};
        }
    }

    protected NamedParamStatement populateNamedParamStmt(RepositoryAction action, NamedParamStatement pstmt) throws SQLException
    {
        for (int i = 0; i < pstmt.getFields().size(); i++)
        {
            String key = pstmt.getFields().get(i);
            if(action.getProperties().containsKey(key)){
                if(key.equalsIgnoreCase(PropertyValueType.STRING_VALUE.name())){
                    pstmt.setString(i, action.getProperties().get(key));
                } else if(key.equalsIgnoreCase(PropertyValueType.BOOLEAN_VALUE.name())){
                    pstmt.setBoolean(i, action.getProperties().get(key));
                } else if(key.equalsIgnoreCase(PropertyValueType.INTEGER_VALUE.name())){
                    pstmt.setInteger(i, action.getProperties().get(key));
                } else if(key.equalsIgnoreCase(PropertyValueType.DATE_VALUE.name())){
                    pstmt.setDate(i, action.getProperties().get(key));
                } else {
                    pstmt.setString(i, action.getProperties().get(key));
                }  
            }
        }
        
        return pstmt;
    }

    /**
     * Return the sql map key for the give action and object type.  Separated from lookup to
     * facilitate unit testing.
     */
    protected String makeSqlKey(RepositoryAction action, boolean preview)
    {
        return makeSqlKey(action, preview, null);
    }
    protected String makeSqlKey(RepositoryAction action, boolean preview, String qualifier)
    {
        return this.xlSystem.name()
            +"."+action.getAction().name()
            +"."+action.getType().name()
            +(qualifier == null ? "":"."+qualifier)
            +(preview ? ".preview":"");
    }

    /**
     * Return the SQL for the given action and object type. 
     */
    protected String lookupSql(RepositoryAction action, boolean preview) throws IllegalArgumentException
    {
        return lookupSql(action, preview, null);
    }
    protected String lookupSql(RepositoryAction action, boolean preview, String qualifier) throws IllegalArgumentException
    {
        String sqlkey =  makeSqlKey(action, preview, qualifier);
        Map<String, String> sqlMap = SQL_STMTS.get(this.dbType.name());

        /*System.out.println("sqlkey = "+sqlkey);
        for (Map.Entry<String, String> entry : sqlMap.entrySet())
        {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }*/
        
        String sql =  sqlMap.get(sqlkey);
        if ( sql == null )
        {
            String msg = String.format("No SQL found for '%s' action on type '%s' for preview = '%b' and qualifier = '%s'", 
                action.getAction().name(), action.getType().name(), preview, qualifier);
            throw new IllegalArgumentException(msg);
        }
        //System.out.println("About to assign sql - "+sql+"  -> for action - "+action.getAction().name()+", properites - "+action.getProperties());
        return sql;
    }
}