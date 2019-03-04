/**
 * Copyright 2019 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xebialabs.migration.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.stream.JsonReader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xebialabs.migration.SystemType;
import com.xebialabs.migration.action.ActionType;
import com.xebialabs.migration.action.ContentActionType;
import com.xebialabs.migration.action.ContentActionXLR;
import com.xebialabs.migration.action.ItemType;
import com.xebialabs.migration.action.RepositoryAction;

public class MigrationConfigParser {

    
    public static final String DB_DRIVER = "dbDriver";
    public static final String DB_URL = "dbUrl";
    public static final String DB_USERNAME = "dbUsername";
    public static final String DB_PWD = "dbPwd";

    public static final String SYSTEM_TYPE = "systemType";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String MIGRATE_PLUGIN = "migratePlugin";

    
    public static final String ACTIONS = "actions";

    //public static final String REPOS_ACTION = "reposAction";


    public static final String ORDER = "order";
    public static final String ACTION = "action";
    public static final String TYPE = "type";

    public static final String PROPERTIES = "properties";

    public static final String MESSAGE = "message";
    public static final String TASK_PARENT_TYPE = "taskParentType";
    public static final String CONTENT_ACTIONS_XLR = "contentActionsXLR";

    public static final String SYSTEM_TYPE_XLD = "xld";
    public static final String SYSTEM_TYPE_XLR = "xlr";


     // Counters, in case order of operations is not configured
    private int reposActionCounter = 0;
    private int contentActionCounter = 0;
    
 

    // Sortable to hold all Actions for this plugin
    private ArrayList<RepositoryAction> reposActionList = new ArrayList<RepositoryAction>();

    // either xld or xlr
    private String systemType = null;

    // name and description in the configuration file
    private String name = null;
    private String description = null;

    // JDBC driver name and database URL 
    private String dbDriver = null;   
    private String dbUrl = null;  
    private String dbUser = null; 

    private String reportDbDriver = null;   
    private String reportDbUrl = null;  
    private String reportDbUser = null; 
    

    public void parseConfigFile(String filePath) throws IOException{
        // Parse the mapping file
            JsonReader jsonReader = new JsonReader(new FileReader(filePath));
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (name.equals(ACTIONS)){
                    parseActions(jsonReader);
                } else if (name.equals(SYSTEM_TYPE)){
                    systemType = jsonReader.nextString();
                } else if (name.equals(NAME)){
                    name = jsonReader.nextString();
                } else if (name.equals(DESCRIPTION)){
                    description = jsonReader.nextString();
                } else {
                    System.out.println("WARNING: Unable to process unknown configuration - "+ name);
                    jsonReader.skipValue();
                }
            }   
            jsonReader.endObject();
            jsonReader.close();
           
    }

    public void parseDbConfiguration(String filePath, SystemType systemType){
        try {
            File file = new File(filePath);

            Config config = ConfigFactory.parseFile(file);

            if(SystemType.XLR.equals(systemType))
            {
                if(config.hasPath("xl.database.db-driver-classname"))
                {
                    dbDriver = cleanValue(config.getString("xl.database.db-driver-classname"));
                }

                if(config.hasPath("xl.database.db-url"))
                {
                    dbUrl = cleanValue(config.getString("xl.database.db-url"));
                }

                if(config.hasPath("xl.database.db-username"))
                {
                    dbUser = config.getString("xl.database.db-username");
                }

                if(config.hasPath("xl.reporting.db-driver-classname"))
                {
                    reportDbDriver = cleanValue(config.getString("xl.reporting.db-driver-classname"));
                }

                if(config.hasPath("xl.reporting.db-url"))
                {
                    reportDbUrl = cleanValue(config.getString("xl.reporting.db-url"));
                }

                if(config.hasPath("xl.reporting.db-username"))
                {
                    reportDbUser = config.getString("xl.reporting.db-username");
                }
            }
            else 
            { // XLD
                if(config.hasPath("xl.repository.database.db-driver-classname"))
                {
                    dbDriver = cleanValue(config.getString("xl.repository.database.db-driver-classname"));
                }

                if(config.hasPath("xl.repository.database.db-url"))
                {
                    dbUrl = cleanValue(config.getString("xl.repository.database.db-url"));
                }

                if(config.hasPath("xl.repository.database.db-username"))
                {
                    dbUser = config.getString("xl.repository.database.db-username");
                }
            }

            
        } catch (Exception e) {
            System.out.println("Exception is - "+e.getMessage());
            System.out.println("Unable to find a custom database configuration, will use the default configuration.");
        }
        
    }


    public ArrayList<RepositoryAction> getReposActionList() {
        return reposActionList;
    }

    public String getSystemType() {
        return systemType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getReportDriver() {
        return reportDbDriver;
    }

    public String getReportUrl() {
        return reportDbUrl;
    }

    public String getReportUser(){
        return reportDbUser;
    }

    public String getDriver() {
        return dbDriver;
    }

    public String getUrl() {
        return dbUrl;
    }

    public String getUser(){
        return dbUser;
    }

     // Private Util Methods

     private String cleanValue(String s){
         String subStr = s.substring(s.indexOf("=")+1).trim();
         s = subStr.replace("\"", ""); 
         return s;
     }



    private void parseActions(JsonReader jsonReader)throws IOException {
        RepositoryAction reposAction;
        jsonReader.beginArray();
         while (jsonReader.hasNext()) {
                reposAction = createReposAction(jsonReader); 
                //System.out.println("Adding new repositoryAction object - "+reposAction);
                reposActionList.add(reposAction);
        } 
        jsonReader.endArray();
    }

   private List<ContentActionXLR> parseContentActionsXLR(JsonReader jsonReader)throws IOException {
        ContentActionXLR conAction;
        List<ContentActionXLR> actionList = new ArrayList<ContentActionXLR>();
        jsonReader.beginArray();
         while (jsonReader.hasNext()) {
                conAction = createConActionXLR(jsonReader); 
                //System.out.println("Adding new ContentActionXLR object - "+conAction);
                actionList.add(conAction);
        } 
        jsonReader.endArray();
        return actionList;
    }


    private RepositoryAction createReposAction(JsonReader jsonReader) throws IOException {
        Integer order = null;
        ActionType action = null;
        ItemType type = null;
        String message = null;
        String taskParentType = null;
        Map<String,String> properties = new HashMap<String,String>();
        List <ContentActionXLR> xlrContentActions = new ArrayList <ContentActionXLR>();
        
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String n = jsonReader.nextName();
            if (n.equals(TYPE)){
                String typeStr = jsonReader.nextString();
                if(typeStr != null){
                    type = ItemType.valueOf(typeStr.toUpperCase());
                }
            } else if (n.equals(ACTION)){
                String actionStr = jsonReader.nextString();
                if(actionStr != null){
                    action = ActionType.valueOf(actionStr.toUpperCase());
                }
            } else if (n.equals(ORDER)){
                order = jsonReader.nextInt();
            } else if (n.equals(MESSAGE)){
                message = jsonReader.nextString();
            } else if (n.equals(TASK_PARENT_TYPE)){
                taskParentType = jsonReader.nextString();
            } else if (n.equals(PROPERTIES)){
                properties = createPropertyUpdate(jsonReader);
            } else if (n.equals(CONTENT_ACTIONS_XLR)){
                xlrContentActions = parseContentActionsXLR(jsonReader);
            } else if (n.equals(DESCRIPTION)){
                // Do nothing
                jsonReader.skipValue();
            } else {
                System.out.println("WARNING: Unable to process unknown configuration - "+ n);
                jsonReader.skipValue();   
            }
        }
        jsonReader.endObject();
        // If order is null or empty, use Counter
        reposActionCounter++;
        if(order == null){
           order = new Integer(reposActionCounter); 
        }
        return new RepositoryAction(order, action, type, message, properties, taskParentType, xlrContentActions);
    }

    

    private ContentActionXLR createConActionXLR(JsonReader jsonReader) throws IOException {
        Integer order = null;
        ContentActionType action = null;
        Map<String,String> properties = new HashMap<String,String>();
        
        
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String n = jsonReader.nextName();
            if (n.equals(ACTION)){
                String actionStr = jsonReader.nextString();
                if(actionStr != null){
                    action = ContentActionType.valueOf(actionStr.toUpperCase());
                }
            } else if (n.equals(ORDER)){
                order = jsonReader.nextInt();
            } else if (n.equals(PROPERTIES)){
                properties = createPropertyUpdate(jsonReader);
            } else if (n.equals(DESCRIPTION)){
                // Do nothing
                jsonReader.skipValue();
            } else {
                System.out.println("WARNING: Unable to process unknown configuration - "+ n);
                jsonReader.skipValue();   
            }
        }
        jsonReader.endObject();
        // If order is null or empty, use Counter
        contentActionCounter++;
        if(order == null){
           order = new Integer(contentActionCounter); 
        }
        return new ContentActionXLR(order, action, properties);
    }

    private Map<String,String> createPropertyUpdate(JsonReader jsonReader)
        throws IOException {
        String name = null;
        String value = null;
        Map<String,String> properties = new HashMap<String,String>();
        
        
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            name = jsonReader.nextName();
            value = jsonReader.nextString();
           properties.put(name,value);
        }
        jsonReader.endObject();
        
        return properties;
    }

    

}