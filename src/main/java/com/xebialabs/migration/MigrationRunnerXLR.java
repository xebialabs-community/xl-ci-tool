/**
 * Copyright 2020 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xebialabs.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.DataFormatException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xebialabs.migration.action.ContentActionType;
import com.xebialabs.migration.action.ContentActionXLR;
import com.xebialabs.migration.action.KindConversionType;
import com.xebialabs.migration.action.PropertyValueType;
import com.xebialabs.migration.action.RepositoryAction;
import com.xebialabs.migration.util.CompressionUtils;
import com.xebialabs.migration.util.NamedParamStatement;


/**
 * Modify configuration items and their properties based on a user supplied
 * script.
 */
public class MigrationRunnerXLR extends MigrationRunner {

    // Used in the Mapping File
    private static final String OLD_VALUE = "oldValue";
    private static final String NEW_VALUE = "newValue";
    private static final String TASK_PARENT_TYPE = "taskParentType";
    private static final String VALUE_DATA_TYPE = "valueDataType";
    private static final String NAME = "name";
    private static final String NEW_NAME = "newName";
    private static final String VALUE = "value";
    private static final String OLD_KIND_TYPE = "oldKindType";
    private static final String NEW_KIND_TYPE = "newKindType";

    protected MigrationRunnerXLR(SystemType xlSystem, Connection dbconn,  DbType dbType, 
        Connection reportDbconn, DbType reportDbType, PrintStream os) {
        this.dbconn = dbconn;
        this.xlSystem = xlSystem;
        this.dbType = dbType;
        this.os = os;
        this.reportDbconn = reportDbconn;
        this.reportDbType = reportDbType;
    }

    // ACTION METHODS =========================================================

    /**
     * Update a CI type.
     */
    @Override
    protected void updateCI(RepositoryAction action, boolean preview)
            throws DataFormatException, IOException, SQLException, IllegalArgumentException {
        action.assertHasProperty(OLD_VALUE);
        action.assertHasProperty(NEW_VALUE);

        String newValue = action.getProperties().get("newValue");
        int cnt = 0;

        if (!preview) {

            String sql = lookupSql(action, preview);
            /*
             * update CI for XLR requires a select which returns a result set then for each,
             * get content blob, modify content blob then update the record with new ci type
             * and modified content blob
             */

            ResultSet result = null;
            String[] stringArray = createSqlArray(sql);
            NamedParamStatement pstmtSelect = null;
            PreparedStatement secondPstmtUpdate = null;
            Boolean origCommitState = dbconn.getAutoCommit();

            // ensure we have two sql statments
            // We expect the first statement to be the select and
            // the second statement is the update
            if (stringArray != null && stringArray.length == 2) {
                try {
                    // Turn autocommit off
                    dbconn.setAutoCommit(false);

                    pstmtSelect = new NamedParamStatement(this.dbconn, stringArray[0]);
                    pstmtSelect = populateNamedParamStmt(action, pstmtSelect);
                    result = pstmtSelect.executeQuery();
                    if (result != null) {
                        while (result.next()) {
                            int ci_uid = result.getInt("ci_uid");

                            byte [] currentBytes = null;

                            if (dbType == DbType.POSTGRESQL){
                                currentBytes = result.getBytes("content");
                            }
                            else {
                                Blob blobContent = result.getBlob("content");
                                currentBytes = convertBlobToByteArray(blobContent);
                            }
                        
                            String blobString = new String(CompressionUtils.decompress(currentBytes));
                            // Now, update blob
                            String updatedBlobString = updateBlob(blobString, "type", newValue);

                            //System.out.println("******BEGIN - this is the updated content blob for xlr - update ci");
                            //System.out.println(updatedBlobString);
                            
                            byte[] newByteArray = CompressionUtils.compress(updatedBlobString.getBytes());

                            secondPstmtUpdate = dbconn.prepareStatement(stringArray[1]);
                            secondPstmtUpdate.setString(1, action.getProperties().get("newValue"));

                            if (dbType == DbType.POSTGRESQL){
                                secondPstmtUpdate.setBytes(2, newByteArray);
                            }
                            else {
                                Blob blobFromBytes = dbconn.createBlob();
                                int numWritten = blobFromBytes.setBytes(1, newByteArray);
                                //System.out.println("******END - numWritten to blob = "+numWritten+", size = "+blobFromBytes.length()+", this is the updated content blob for xlr - update task");
                                // old way Blob blobFromBytes = new javax.sql.rowset.serial.SerialBlob(newByteArray); 
                                secondPstmtUpdate.setBlob(2, blobFromBytes);       
                            }

                            secondPstmtUpdate.setInt(3, ci_uid);

                            cnt = secondPstmtUpdate.executeUpdate();
                        }

                    } else {
                        os.println(String.format("\n%sUpdate CI from %s to %s. 0 row(s) altered. %s",
                                (preview ? "[PREVIEW] " : ""), action.getProperties().get(OLD_VALUE),
                                action.getProperties().get(NEW_VALUE), "No records found to update."));
                    }

                    // successfully reached the end, so commit
                    //os.println("About to commit the database transaction.");
                    dbconn.commit();
                    os.println(String.format("\n%sUpdate CI from %s to %s. %d row(s) altered.", (preview ? "[PREVIEW] " : ""),
                    action.getProperties().get(OLD_VALUE), action.getProperties().get("newValue"), cnt));
                    if(cnt>0 && action.getMessage() != null && !action.getMessage().isEmpty()){
                        os.println("\tAttention: "+action.getMessage());
                    }
                } catch (SQLException se){
                    dbconn.rollback();
                    os.println("\nERROR: SQLExecption thrown, so database transcaction has been rolled back.");
                    throw se; 
                } finally {
                    try {
                        if(dbconn != null) dbconn.setAutoCommit(origCommitState);
                        if (result != null) result.close();
                        if (secondPstmtUpdate != null) secondPstmtUpdate.close();
                        if (pstmtSelect != null) pstmtSelect.close();
                    } catch (Exception e) {
                    }
                }

            } else {
                throw new IllegalArgumentException(
                        "updateCI mapping must contain two SQL statements, a SELECT and an UPDATE.");
            }

        } else {
            // perform preview action
            cnt = processAction(action, preview);
            os.println(String.format("\n%sUpdate CI from %s to %s. %d row(s) altered.", (preview ? "[PREVIEW] " : ""),
                    action.getProperties().get(OLD_VALUE), action.getProperties().get("newValue"), cnt));
            
        }
    }

    /**
     * Update an XLR Task
     */
    @Override
    protected void updateTask(RepositoryAction action, boolean preview)
            throws DataFormatException, IOException, SQLException, IllegalArgumentException {
        action.assertHasProperty(OLD_VALUE);
        action.assertHasProperty(NEW_VALUE);
        action.assertHasProperty(TASK_PARENT_TYPE);
        List<ContentActionXLR> contentActions = action.getXlrContentActions();
        // Validate ContentActions
        validateContentActions(contentActions);

        String oldValue = action.getProperties().get(OLD_VALUE);
        String newValue = action.getProperties().get(NEW_VALUE);
        String taskParentTypeStr = action.getProperties().get(TASK_PARENT_TYPE);
        int cnt = 0;

        if (!preview) {
            String sql = lookupSql(action, preview);
            /*
             * update TASK for XLR requires a select from XLR_TASK which returns a result
             * set of release_uids. For each, get content blob from XLR_RELEASE_DATA,
             * modify content blob then update the XLR_RELEASE_DATA. 
             * Then, get task_id, content blob from XLR_TASK_BACKUPS for that release_id.
             * Modify the content blob and then update XLR_TASK_BACKUPS.
             * Update task_type in XLR_TASK
             * 
             * After completeing XLR database update, update tasks in the report database
             */

            PreparedStatement pstmtReleaseId = null;
            PreparedStatement pstmtContent = null;
            PreparedStatement pstmtContent2 = null;

            ResultSet rsRelease = null;
            ResultSet rsContent = null;
            ResultSet rsContent2 = null;

            PreparedStatement pstmtUpdateContent = null;
            PreparedStatement pstmtUpdateContent2 = null;
            PreparedStatement pstmtUpdateTask = null;
            Boolean origCommitState = dbconn.getAutoCommit();

            String[] stringArray = createSqlArray(sql);
            
            /*
             * ensure we have ten sql statments We expect something like the following
             * "SELECT DISTINCT release_uid FROM XLR_TASKS WHERE task_type = ?";
             * "SELECT content FROM XLR_RELEASES_DATA WHERE ci_uid = ?";
             * "UPDATE XLR_RELEASES_DATA SET content = ? WHERE ci_uid = ?";
             * "SELECT task_id, content FROM XLR_TASK_BACKUPS WHERE ci_uid = ?";
             * "UPDATE XLR_TASK_BACKUPS SET content = ? WHERE task_id = ?";
             * "UPDATE XLR_TASKS SET task_type = ? where task_type = ?";
             * 
             * the following sql will be run against the report database
             * "SELECT DISTINCT releaseid FROM TASKS WHERE tasktype = ?"
             * "SELECT releasejson FROM RELEASES WHERE releaseid = ?";
             * "UPDATE RELEASES SET releasejson = ? WHERE releaseid = ?";
             * "UPDATE TASKS SET tasktype = ? where tasktype = ?";
             */

            if (stringArray != null && stringArray.length == 10) {
                try {
                    // Turn autocommit off
                    dbconn.setAutoCommit(false);

                    pstmtReleaseId = dbconn.prepareStatement(stringArray[0]);
                    pstmtReleaseId.setString(1, oldValue);
                    rsRelease = pstmtReleaseId.executeQuery();

                    if (rsRelease != null) {
                        while (rsRelease != null && rsRelease.next()) {

                            int release_uid = rsRelease.getInt("release_uid");
                            pstmtContent = dbconn.prepareStatement(stringArray[1]);
                            pstmtContent.setInt(1, release_uid);
                            // Get all content blobs from the XLR_RELEASES_DATA table for this release_uid
                            rsContent = pstmtContent.executeQuery();

                            while (rsContent != null && rsContent.next()) {
                                byte [] currentBytes = null;

                                if (dbType == DbType.POSTGRESQL){
                                    currentBytes = rsContent.getBytes("content");
                                }
                                else {
                                    Blob blobContent = rsContent.getBlob("content");
                                    currentBytes = convertBlobToByteArray(blobContent);
                                }

                                String blobString = new String(CompressionUtils.decompress(currentBytes));

                                // Now, update blob
                                String updatedBlobString = updateReleaseDataBlob(blobString, taskParentTypeStr, oldValue, 
                                    contentActions, action);

                                //System.out.println("******BEGIN - this is the updated content blob for xlr - update task");
                                //System.out.println(updatedBlobString);
                                //System.out.println("******END - this is the updated content blob for xlr - update task");

                                // We now have the updated blobString, commit it to the database
                                byte[] newByteArray = CompressionUtils.compress(updatedBlobString.getBytes());

                                pstmtUpdateContent = dbconn.prepareStatement(stringArray[2]);
                                
                                if (dbType == DbType.POSTGRESQL){
                                    pstmtUpdateContent.setBytes(1, newByteArray);
                                }
                                else {
                                    Blob blobFromBytes = dbconn.createBlob();
                                    int numWritten = blobFromBytes.setBytes(1, newByteArray);
                                    //System.out.println("******END - numWritten to blob = "+numWritten+", size = "+blobFromBytes.length()+", this is the updated content blob for xlr - update task");
                                    // old way Blob blobFromBytes = new javax.sql.rowset.serial.SerialBlob(newByteArray); 
                                    pstmtUpdateContent.setBlob(1, blobFromBytes);       
                                }
                                
                                pstmtUpdateContent.setInt(2, release_uid);
                                int resultUpdateContent = pstmtUpdateContent.executeUpdate();
                            } // end of while content

                            // Now requery the database to get info from the XLR_TASK_BACKUPS table
                            pstmtContent2 = dbconn.prepareStatement(stringArray[3]);
                            pstmtContent2.setInt(1, release_uid);
                            rsContent2 = pstmtContent2.executeQuery();
                            while (rsContent2 != null && rsContent2.next()) {
                                //get Task_ID
                                String task_id = rsContent2.getString("task_id");

                                Blob blobContent2 = null;
                                byte [] currentBytes2 = null;

                                if (dbType == DbType.POSTGRESQL){
                                    currentBytes2 = rsContent2.getBytes("content");
                                }
                                else {
                                    blobContent2 = rsContent2.getBlob("content");
                                    currentBytes2 = convertBlobToByteArray(blobContent2);
                                }

                                String blobString2 = new String(CompressionUtils.decompress(currentBytes2));
                                JsonParser jsonParser = new JsonParser();
                                JsonObject parentObject2 = null;
                                
                                JsonObject objectFromString2 = jsonParser.parse(blobString2).getAsJsonObject();
                                // process content to get all task JsonObjects
                                if (taskParentTypeStr != null && !taskParentTypeStr.isEmpty() 
                                        && objectFromString2.has(taskParentTypeStr)) {
                                    parentObject2 = objectFromString2.getAsJsonObject(taskParentTypeStr);
                                    if (parentObject2.has("type")) {
                                        if (oldValue.equals(parentObject2.get("type").getAsString())) {
                                            // NOTE: the type will come from the properties of action
                                            // (NOT the xlrContentActions)
                                            updateParentObject(parentObject2, contentActions, jsonParser, action);

                                            //System.out.println("******BEGIN - this is the updated content blob for xlr - update task - backup");
                                            //System.out.println(objectFromString2.toString());
                                            //System.out.println("******END - this is the updated content blob for xlr - update update task - backup");

                                            byte[] newByteArray2 = CompressionUtils.compress(objectFromString2.toString().getBytes());
                                            
                                            pstmtUpdateContent2 = dbconn.prepareStatement(stringArray[4]);

                                            if (dbType == DbType.POSTGRESQL){
                                                pstmtUpdateContent2.setBytes(1, newByteArray2);
                                            }
                                            else {
                                                Blob blobFromBytes2 = dbconn.createBlob();
                                                int numWritten = blobFromBytes2.setBytes(1, newByteArray2);
                                                //System.out.println("******END - numWritten to blob = "+numWritten+", size = "+blobFromBytes.length()+", this is the updated content blob for xlr - update task");
                                                // old way Blob blobFromBytes2 = new javax.sql.rowset.serial.SerialBlob(newByteArray2); 
                                                pstmtUpdateContent2.setBlob(1, blobFromBytes2);      
                                            }

                                            pstmtUpdateContent2.setString(2, task_id);
                                            int resultUpdateContent2 = pstmtUpdateContent2.executeUpdate();
                                        } // end of if type = oldValue
                                    } // end of if has type
                                } // end of if has parent
                            }// end of second blob cycle 
                        } // end of while release_uid


                        // Now update XLR_TASKS table
                        pstmtUpdateTask = dbconn.prepareStatement(stringArray[5]);
                        pstmtUpdateTask.setString(1, newValue);
                        pstmtUpdateTask.setString(2, oldValue);
                        cnt = pstmtUpdateTask.executeUpdate();

                        // Now update the report database for this action
                        updateReportTasks(stringArray, action);

                        os.println(String.format("\n%sUpdate TASK from %s to %s. %d row(s) altered.",
                                (preview ? "[PREVIEW] " : ""), action.getProperties().get(OLD_VALUE),
                                action.getProperties().get(NEW_VALUE), cnt));
                        if(cnt>0 && action.getMessage() != null && !action.getMessage().isEmpty()){
                            os.println("\tAttention: "+action.getMessage());
                        }

                    } else {
                        os.println(String.format("\n%sUpdate TASK from %s to %s. 0 row(s) altered. %s",
                                (preview ? "[PREVIEW] " : ""), action.getProperties().get(OLD_VALUE),
                                action.getProperties().get(NEW_VALUE), "No records found to update."));
                    }

                    // successfully reached the end, so commit
                    //os.println("About to commit the database transaction.");
                    dbconn.commit();   

                } catch (SQLException se){
                    dbconn.rollback();
                    os.println("\nERROR: SQLExecption thrown, so database transcaction has been rolled back.");
                    throw se;
                } catch (IllegalArgumentException ie){
                    dbconn.rollback();
                    os.println("\nERROR: IllegalArgumentExecption thrown, so database transcaction has been rolled back.");
                    throw ie;
                } finally {
                    try {
                        if(dbconn != null) dbconn.setAutoCommit(origCommitState);
                        if (pstmtReleaseId != null) pstmtReleaseId.close();
                        if (pstmtContent != null) pstmtContent.close();
                        if (pstmtContent2 != null) pstmtContent2.close();
                        if (rsRelease != null) rsRelease.close();
                        if (rsContent != null) rsContent.close();
                        if (rsContent2 != null) rsContent2.close();
                        if (pstmtUpdateContent != null) pstmtUpdateContent.close();
                        if (pstmtUpdateContent2 != null) pstmtUpdateContent2.close();
                        if (pstmtUpdateTask != null) pstmtUpdateTask.close();
                    } catch (SQLException e) {
                        //os.println("Exception thrown while closing prepared statements and result sets");
                        //throw e;
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "updateTask SQL mapping must contain ten SQL statements, a SELECT, SELECT, UPDATE, SELECT, UPDATE, UPDATE, SELECT, SELECT, UPDATE, UPDATE.");
            }
        } else {
            // perform preview action
            cnt = processAction(action, preview);

            os.println(String.format("\n%sUpdate TASK from %s to %s. %d row(s) altered.", (preview ? "[PREVIEW] " : ""),
                    action.getProperties().get(OLD_VALUE), action.getProperties().get(NEW_VALUE), cnt));
        }
    }

    private void updateReportTasks(String[] stringArray, RepositoryAction action)
    throws DataFormatException, IOException, SQLException, IllegalArgumentException {
        List<ContentActionXLR> contentActions = action.getXlrContentActions();

        String oldValue = action.getProperties().get(OLD_VALUE);
        String newValue = action.getProperties().get(NEW_VALUE);
        String taskParentTypeStr = action.getProperties().get(TASK_PARENT_TYPE);
        int cnt = 0;

        PreparedStatement pstmtReleaseId = null;
        PreparedStatement pstmtContent = null;

        ResultSet rsRelease = null;
        ResultSet rsContent = null;

        PreparedStatement pstmtUpdateContent = null;
        PreparedStatement pstmtUpdateTask = null;
        Boolean origCommitState = reportDbconn.getAutoCommit();

        // NOTE: unlike the xlrelease database, blobs stored in the xlarchive database are not compressed

        try {
            // Turn autocommit off
            reportDbconn.setAutoCommit(false);

            pstmtReleaseId = reportDbconn.prepareStatement(stringArray[6]);
            pstmtReleaseId.setString(1, oldValue);
            rsRelease = pstmtReleaseId.executeQuery();

            if (rsRelease != null) {
                while (rsRelease != null && rsRelease.next()) {

                    String releaseid = rsRelease.getString("releaseid");
                    pstmtContent = reportDbconn.prepareStatement(stringArray[7]);
                    pstmtContent.setString(1, releaseid);
                    // Get all content blobs from the XLR_RELEASES_DATA table for this release_uid
                    rsContent = pstmtContent.executeQuery();

                    while (rsContent != null && rsContent.next()) {
                        byte [] currentBytes = null;

                        if (reportDbType == DbType.POSTGRESQL){
                            currentBytes = rsContent.getBytes("releasejson");
                        }
                        else {
                            Blob blobContent = rsContent.getBlob("releasejson");
                            currentBytes = convertBlobToByteArray(blobContent);
                        }

                        String blobString = new String(currentBytes);

                        // Now, update blob
                        String updatedBlobString = updateReleaseDataBlob(blobString, taskParentTypeStr, oldValue, 
                        contentActions, action);

                        //System.out.println("******BEGIN - this is the updated content blob for xlr - update  report task");
                        //System.out.println(updatedBlobString);
                        

                        // We now have the updated blob, commit it to the database
                        byte[] newByteArray = updatedBlobString.getBytes();
                        pstmtUpdateContent = reportDbconn.prepareStatement(stringArray[8]);

                        if (reportDbType == DbType.POSTGRESQL){
                            pstmtUpdateContent.setBytes(1, newByteArray);
                        }
                        else {
                            Blob blobFromBytes = reportDbconn.createBlob();
                            int numWritten = blobFromBytes.setBytes(1, newByteArray);
                            //System.out.println("******END - numWritten to blob = "+numWritten+", size = "+blobFromBytes.length()+", this is the updated content blob for xlr - update report task");
                            //Blob blobFromBytes = new javax.sql.rowset.serial.SerialBlob(newByteArray); 
                            pstmtUpdateContent.setBlob(1, blobFromBytes);       
                        }

                        pstmtUpdateContent.setString(2, releaseid);
                        int resultUpdateContent = pstmtUpdateContent.executeUpdate();
                    }

                }
                // Now update TASKS table
                pstmtUpdateTask = reportDbconn.prepareStatement(stringArray[9]);
                pstmtUpdateTask.setString(1, newValue);
                pstmtUpdateTask.setString(2, oldValue);
                cnt = pstmtUpdateTask.executeUpdate();
            }
            // successfully reached the end, so commit
            reportDbconn.commit();
        } catch (SQLException se){
            reportDbconn.rollback();
            os.println("\nERROR: SQLExecption thrown, so report database transcaction has been rolled back.");
            os.println("Exception: "+se);
            throw se;
        } finally {
            try {
                if(reportDbconn != null) reportDbconn.setAutoCommit(origCommitState);
                if (pstmtReleaseId != null) pstmtReleaseId.close();
                if (pstmtContent != null) pstmtContent.close();
                if (rsRelease != null) rsRelease.close();
                if (rsContent != null) rsContent.close();
                if (pstmtUpdateContent != null) pstmtUpdateContent.close();
                if (pstmtUpdateTask != null) pstmtUpdateTask.close();
            } catch (SQLException e) {
                //os.println("Exception thrown while closing prepared statements and result sets");
                //throw e;
            }
        }
    }

    // UTIL METHODS =========================================================

    private String[] createSqlArray(String sql) {
        String[] stringArray = sql.split(";");
        return stringArray;
    }

    private byte[] convertBlobToByteArray(Blob blob) throws SQLException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        InputStream in = blob.getBinaryStream();
        int n = 0;
        while ((n=in.read(buf))>=0)
        {
            baos.write(buf, 0, n);
        }
        in.close();
        byte[] bytes = baos.toByteArray();
        return bytes;
    }


    private String updateBlob(String blobString, String name, String value) {
        JsonParser jsonParser = new JsonParser();
        JsonObject objectFromString = jsonParser.parse(blobString).getAsJsonObject();

        if (objectFromString.has(name)) {
            objectFromString.remove(name);
        }
        objectFromString.addProperty(name, value);

        return objectFromString.toString();
    }

    private String updateReleaseDataBlob(String blobString, String taskParentTypeStr, String oldValue, 
        List<ContentActionXLR> contentActions, RepositoryAction action){
        // needed to process the JSON string from the blob
        JsonParser jsonParser = new JsonParser();
        JsonObject objectFromString = null;
        JsonArray phases = null;
        JsonArray tasks = null;
        JsonObject parentObject = null;

        objectFromString = jsonParser.parse(blobString).getAsJsonObject();
        // process content to get all task JsonObjects
        if (objectFromString != null && objectFromString.has("phases")) {
            phases = objectFromString.getAsJsonArray("phases");
            for (JsonElement phase : phases) {
                JsonObject phaseObj = phase.getAsJsonObject();
                if (phaseObj.has("tasks")) {
                    tasks = phaseObj.getAsJsonArray("tasks");
                    for (JsonElement task : tasks) {
                        JsonObject taskObj = task.getAsJsonObject();
                        if (taskParentTypeStr != null && !taskParentTypeStr.isEmpty() 
                                && taskObj.has(taskParentTypeStr)) 
                        {
                            parentObject = taskObj.getAsJsonObject(taskParentTypeStr);
                            if (parentObject.has("type")) {
                                if (oldValue.equals(parentObject.get("type").getAsString())) {
                                    // NOTE: the type will come from the properties of action
                                    // (NOT the xlrContentActions)
                                    updateParentObject(parentObject, contentActions, jsonParser, action);
                                } // end of if type = oldValue
                            } // end of if has type
                        } // end of if has parent
                    } // end for each tasks
                } // end of if has tasks
            } // end of for each phase
        } // end of 'if has phases'

        return objectFromString.toString();
    }

    private JsonObject updateParentObject(JsonObject parentObject, List<ContentActionXLR> contentActions, JsonParser jsonParser, 
        RepositoryAction action) throws IllegalArgumentException{
        parentObject.remove("type");
        parentObject.addProperty("type", action.getProperties().get(NEW_VALUE));

        // get all contentActionsXLR mappings
        for (ContentActionXLR conAction : contentActions) {
            switch (conAction.getAction()) {
            case CREATE:
                createContentProperty(conAction, parentObject, jsonParser);
                break;

            case DELETE:
                deleteContentProperty(conAction, parentObject);
                break;

            case UPDATENAME:
                updateContentPropertyName(conAction, parentObject);
                break;

            case UPDATEVALUE:
                updateContentPropertyValue(conAction, parentObject, jsonParser);
                break;

            case UPDATEKIND:
                updateContentPropertyKind(conAction, parentObject, jsonParser);
                break;

            default:
                String msg = String.format(
                        "ContentActionXLR %s is not supported for type %s",
                        conAction.getAction().name(),
                        action.getType().name());
                throw new IllegalArgumentException(msg);
            }
        } // end of for each contentAction
        return parentObject;
    }

    private void validateContentActions(List<ContentActionXLR> contentActions) throws IllegalArgumentException {

        for (ContentActionXLR conAction : contentActions) {
            if (ContentActionType.CREATE.equals(conAction.getAction())) {
                conAction.assertHasProperty(NAME);
                conAction.assertHasProperty(VALUE);
                conAction.assertHasProperty(VALUE_DATA_TYPE);
            } else if (ContentActionType.DELETE.equals(conAction.getAction())) {
                conAction.assertHasProperty(NAME);

            } else if (ContentActionType.UPDATENAME.equals(conAction.getAction())) {
                conAction.assertHasProperty(NAME);
                conAction.assertHasProperty(NEW_NAME);
                conAction.assertHasProperty(VALUE_DATA_TYPE);
            } else if (ContentActionType.UPDATEVALUE.equals(conAction.getAction())) {
                conAction.assertHasProperty(NAME);
                conAction.assertHasProperty(NEW_VALUE);
                conAction.assertHasProperty(VALUE_DATA_TYPE);
            } else if (ContentActionType.UPDATEKIND.equals(conAction.getAction())) {
                conAction.assertHasProperty(NAME);
                conAction.assertHasProperty(OLD_KIND_TYPE);
                conAction.assertHasProperty(NEW_KIND_TYPE);
            } else {
                String msg = String.format("ContentActionXLR %s is not currently supported.",
                        conAction.getAction().name());
                throw new IllegalArgumentException(msg);
            }
        }
    }

    private void createContentProperty(ContentActionXLR conAction, JsonObject parentObject, JsonParser jsonParser){
        PropertyValueType valueType = PropertyValueType.valueOf(conAction.getProperties().get(VALUE_DATA_TYPE).toUpperCase());
        String name = conAction.getProperties().get(NAME);
        if(!parentObject.has(name))
        {
            switch (valueType) {
                case JSON_VALUE:
                    try{
                        String jsonString = conAction.getProperties().get(VALUE);
                        JsonObject newObj = jsonParser.parse(jsonString).getAsJsonObject();
                        parentObject.add(name, newObj);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to convert string value %s to json", conAction.getProperties().get(VALUE));
                        throw new IllegalArgumentException(msg);
                    }
                    break;
    
                case INTEGER_VALUE:
                    try{
                        int value = Integer.valueOf(conAction.getProperties().get(VALUE));
                        parentObject.addProperty(name, value);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to convert string value %s to int", conAction.getProperties().get(VALUE));
                        throw new IllegalArgumentException(msg);
                    }   
                    break;
    
                case STRING_VALUE:
                    parentObject.addProperty(name, conAction.getProperties().get(VALUE));
                    break;

                case LIST_OF_STRING_VALUE:
                    try{
                        String jsonString = conAction.getProperties().get(VALUE);
                        JsonArray newArray = jsonParser.parse(jsonString).getAsJsonArray();
                        parentObject.add(name, newArray);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to convert string value %s to list(array)", conAction.getProperties().get(VALUE));
                        throw new IllegalArgumentException(msg);
                    }
                    break;
    
                case BOOLEAN_VALUE:
                    try{
                        boolean value = Boolean.valueOf(conAction.getProperties().get(VALUE));
                        parentObject.addProperty(name, value);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to convert string value %s to boolean", conAction.getProperties().get(VALUE));
                        throw new IllegalArgumentException(msg);
                    } 
                    break;
    
                default:
                    String msg = String.format(
                            "ContentActionXLR "+VALUE_DATA_TYPE+" '%s' is not supported for action %s",
                            conAction.getProperties().get(VALUE_DATA_TYPE),
                            conAction.getAction().name());
                    throw new IllegalArgumentException(msg);
                }   
        }
        else 
        {
            String msg = String.format(
                            "ContentActionXLR property '%s' already exists and so cannot be created.",
                            name);
            os.println("\nWARNING - "+msg);
                    //throw new IllegalArgumentException(msg);
        }   
    }

    private void deleteContentProperty(ContentActionXLR conAction, JsonObject parentObject){
        String name = conAction.getProperties().get(NAME);
        if(parentObject.has(name))
        {
            parentObject.remove(name);
        }
        else 
        {
            String msg = String.format(
                            "ContentActionXLR property '%s' does not exist and so cannot be deleted.",
                            name);
            os.println("\nWARNING - "+msg);
                    //throw new IllegalArgumentException(msg);
        } 
    }

    private void updateContentPropertyName(ContentActionXLR conAction, JsonObject parentObject){
        PropertyValueType valueType = PropertyValueType.valueOf(conAction.getProperties().get(VALUE_DATA_TYPE).toUpperCase());
        String name = conAction.getProperties().get(NAME);
        if(parentObject.has(name))
        {
            switch (valueType) {
                case JSON_VALUE:
                    try{
                        JsonObject currentValueObj = parentObject.getAsJsonObject(name);
                        parentObject.remove(name);
                        parentObject.add(conAction.getProperties().get(NEW_NAME), currentValueObj);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to get current value of %s as json and so could not update the name", name);
                        throw new IllegalArgumentException(msg);
                    }
                    break;
    
                case INTEGER_VALUE:
                    try{
                        int currentIntValue = parentObject.get(name).getAsInt();
                        parentObject.remove(name);
                        parentObject.addProperty(conAction.getProperties().get(NEW_NAME), currentIntValue);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to get current value of %s as int and so could not update the name", name);
                        throw new IllegalArgumentException(msg);
                    }   
                    break;
    
                case STRING_VALUE:
                    String currentStrValue = parentObject.get(name).getAsString();
                    parentObject.remove(name);
                    parentObject.addProperty(conAction.getProperties().get(NEW_NAME), currentStrValue);
                    break;

                case LIST_OF_STRING_VALUE:
                    JsonArray currentArrayValue = parentObject.get(name).getAsJsonArray();
                    parentObject.remove(name);
                    parentObject.add(conAction.getProperties().get(NEW_NAME), currentArrayValue);
                    break;
    
                case BOOLEAN_VALUE:
                    try{
                        boolean currentBoolValue = parentObject.get(name).getAsBoolean();
                        parentObject.remove(name);
                        parentObject.addProperty(conAction.getProperties().get(NEW_NAME), currentBoolValue);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to get current value of %s as boolean and so could not update the name", name);
                        throw new IllegalArgumentException(msg);
                    }   
                    break;
    
                default:
                    String msg = String.format(
                            "ContentActionXLR "+VALUE_DATA_TYPE+" '%s' is not supported for action %s",
                            conAction.getProperties().get(VALUE_DATA_TYPE),
                            conAction.getAction().name());
                    throw new IllegalArgumentException(msg);
                } 
        }
        else 
        {
            String msg = String.format(
                            "ContentActionXLR property '%s' does not exist and so the name cannot be updated.",
                            name);
            os.println("\nWARNING - "+msg);
                    //throw new IllegalArgumentException(msg);
        }   
        
    }

    private void updateContentPropertyValue(ContentActionXLR conAction, JsonObject parentObject, JsonParser jsonParser){
        PropertyValueType valueType = PropertyValueType.valueOf(conAction.getProperties().get(VALUE_DATA_TYPE).toUpperCase());
        String name = conAction.getProperties().get(NAME);
        if(parentObject.has(name))
        {
            switch (valueType) {
                case JSON_VALUE:
                    try{
                        String jsonString = conAction.getProperties().get(NEW_VALUE);
                        JsonObject newObj = new JsonObject();
                        if(jsonString != null && !(jsonString.trim().equals(""))){
                            newObj = jsonParser.parse(jsonString).getAsJsonObject();
                        } 
                        parentObject.remove(name);
                        parentObject.add(name, newObj);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to get new value of %s as json and so could not update the value", name);
                        throw new IllegalArgumentException(msg);
                    }
                    break;
    
                case INTEGER_VALUE:
                    try{
                        int intValue = Integer.valueOf(conAction.getProperties().get(NEW_VALUE));
                        parentObject.remove(name);
                        parentObject.addProperty(name, intValue);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to get new value of %s as int and so could not update the value", name);
                        throw new IllegalArgumentException(msg);
                    }   
                    break;
    
                case STRING_VALUE:
                    String strValue = conAction.getProperties().get(NEW_VALUE);
                    parentObject.remove(name);
                    parentObject.addProperty(name, strValue);
                    break;

                case LIST_OF_STRING_VALUE:
                    try{
                        String jsonString = conAction.getProperties().get(NEW_VALUE);
                        JsonArray newArray = new JsonArray();
                        if(jsonString != null && !(jsonString.trim().equals(""))){
                            newArray = jsonParser.parse(jsonString).getAsJsonArray();
                        } 
                        parentObject.remove(name);
                        parentObject.add(name, newArray);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to get new value of %s as list(array) and so could not update the value", name);
                        throw new IllegalArgumentException(msg);
                    }
                    break;
    
                case BOOLEAN_VALUE:
                    try{
                        boolean boolValue = Boolean.valueOf(conAction.getProperties().get(NEW_VALUE));
                        parentObject.remove(name);
                        parentObject.addProperty(name, boolValue);
                    } catch (Exception e){
                        String msg = String.format("ContentActionXLR - Failed to get new value of %s as boolean and so could not update the value", name);
                        throw new IllegalArgumentException(msg);
                    }   
                    break;
    
                default:
                    String msg = String.format(
                            "ContentActionXLR "+VALUE_DATA_TYPE+" '%s' is not supported for action %s",
                            conAction.getProperties().get(VALUE_DATA_TYPE),
                            conAction.getAction().name());
                    throw new IllegalArgumentException(msg);
                } 
        }
        else 
        {
            String msg = String.format(
                            "ContentActionXLR property '%s' does not exist and so the value cannot be updated.",
                            name);
            os.println("\nWARNING - "+msg);
                    //throw new IllegalArgumentException(msg);
        }  
    }   

    private void updateContentPropertyKind(ContentActionXLR conAction, JsonObject parentObject, JsonParser jsonParser){
        KindConversionType conversionType = KindConversionType.valueOf(conAction.getProperties().get(OLD_KIND_TYPE).toUpperCase() + "_TO_" +
                conAction.getProperties().get(NEW_KIND_TYPE).toUpperCase());
        String name = conAction.getProperties().get(NAME);
        if(parentObject.has(name))
        {
            switch (conversionType) {
                case STRING_VALUE_TO_LIST_OF_STRING_VALUE:
                    String currentStrValue = parentObject.get(name).getAsString();
                    JsonArray newArray = new JsonArray();
                    if(currentStrValue != null && !(currentStrValue.trim().equals(""))){
                        newArray.add(currentStrValue);
                    }
                    parentObject.remove(name);
                    parentObject.add(name, newArray);
                    break;
    
                default:
                    String msg = String.format(
                            "ContentActionXLR kind type conversion from %s to %s is not supported for action %s",
                            conAction.getProperties().get(OLD_KIND_TYPE),
                            conAction.getProperties().get(NEW_KIND_TYPE),
                            conAction.getAction().name());
                    throw new IllegalArgumentException(msg);
                } 
        }
        else 
        {
            String msg = String.format(
                            "ContentActionXLR property '%s' does not exist and so the kind type cannot be converted.",
                            name);
            os.println("\nWARNING - "+msg);
                    //throw new IllegalArgumentException(msg);
        }     
    }



}