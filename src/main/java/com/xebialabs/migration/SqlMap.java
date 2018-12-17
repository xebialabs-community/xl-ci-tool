package com.xebialabs.migration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.xebialabs.migration.action.ActionType;
import com.xebialabs.migration.action.PropertyValueType;
import com.xebialabs.migration.action.ItemType;

/**
 * Map containing maps of database specific sql statements
 */
public class SqlMap 
{
    /**
     * These SQL statements are the real workhorse of the migration runner.  There are separate statements
     * for preview mode.  These should return a count like the actual sql statement but they must not alter
     * any database tables.
     * 
     */
    private static Map<String, Map<String, String>> ALL_SQL_STMTS;
    static {
        
        Map<String, Map<String, String>> masterMap = new HashMap<String, Map<String, String>>();
        Map<String, String> derbyMap = new HashMap<String, String>();
        Map<String, String> mySqlMap = new HashMap<String, String>();
        Map<String, String> h2Map = new HashMap<String, String>();
        Map<String, String> postgresqlMap = new HashMap<String, String>();
        Map<String, String> oracleMap = new HashMap<String, String>();
        Map<String, String> db2Map = new HashMap<String, String>();
        Map<String, String> sqlserverMap = new HashMap<String, String>();

       
        /// XLD.ACTION.COPY.CI.createTrigger //////////////////////////////////////
        // TODO - Derby does not support 'if exists' so we will catch the exception when executing the batch
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name()+".createtrigger", 
            "drop trigger cis_insert_trigger; create trigger cis_insert_trigger before insert on XLD_CIS for each row begin set new.id = ifnull((select max(id) from XLD_CIS), -1) + 1; end;"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name()+".createtrigger", 
            "drop trigger if exists cis_insert_trigger; create trigger cis_insert_trigger before insert on XLD_CIS for each row begin set new.id = ifnull((select max(id) from XLD_CIS), -1) + 1; end;"
        );
        /////////////////////////////////////////

        /// XLD.ACTION.COPY.CI //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name(), 
            "insert into XLD_CIS (\"ci_type\", \"name\", \"parent_id\", \"token\", \"created_at\", \"created_by\", \"modified_at\", \"modified_by\", \"path\", \"secured_ci\") select :newValue, \"name\" || :newNameSuffix, \"parent_id\", \"token\", \"created_at\", \"created_by\", \"modified_at\", \"modified_by\", \"path\" || :newNameSuffix, \"secured_ci\" from XLD_CIS where \"ci_type\" = :oldValue"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name(), 
            "insert into XLD_CIS (ci_type, name, parent_id, token, created_at, created_by, modified_at, modified_by, path, secured_ci) select :newValue, CONCAT(name, :newNameSuffix), parent_id, token, created_at, created_by, modified_at, modified_by, CONCAT(path, :newNameSuffix), secured_ci from XLD_CIS where ci_type = :oldValue"
        );
        /////////////////////////////////////////

        /// XLD.COPY.CI.droptrigger //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name()+".droptrigger", 
            "drop trigger cis_insert_trigger"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name()+".droptrigger", 
            "drop trigger if exists cis_insert_trigger"
        );
        /////////////////////////////////////////

        /// XLD.COPY.CI.preview //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :oldValue"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.COPY.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :oldValue"
        );
        /////////////////////////////////////////

        /// XLD.UPDATE.CI //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "update XLD_CIS set \"ci_type\" = :newValue where \"ci_type\" = :oldValue"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "update XLD_CIS set ci_type = :newValue where ci_type = :oldValue"
        );
        /////////////////////////////////////////


        /// XLD.UPDATE.CI.preview //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :oldValue"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :oldValue"
        );
        /////////////////////////////////////////

        /// XLD.DELETE.CI.children //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI.name()+".children", 
            "delete from XLD_CI_PROPERTIES where \"ci_id\" in (select id from XLD_CIS where \"ci_type\" = :ciName)"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI.name()+".children", 
            "delete from XLD_CI_PROPERTIES where ci_id in (select id from XLD_CIS where ci_type = :ciName)"
        );
        /////////////////////////////////////////

        ///  XLD.DELETE.CI //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI.name(), 
            "delete from XLD_CIS where \"ci_type\" = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI.name(), 
            "delete from XLD_CIS where ci_type = :ciName"
        );
        /////////////////////////////////////////

        /// XLD.DELETE.CI.preview //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :ciName"
        );
        /////////////////////////////////////////


        /// XLD.CREATE.CI_PROPERTY.createtrigger //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+".createtrigger", 
            "drop trigger ci_properties_insert_trigger; create trigger ci_properties_insert_trigger before insert on XLD_CI_PROPERTIES for each row begin set new.id = ifnull((select max(id) from XLD_CI_PROPERTIES), -1) + 1; end;"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+".createtrigger", 
            "drop trigger if exists ci_properties_insert_trigger; create trigger ci_properties_insert_trigger before insert on XLD_CI_PROPERTIES for each row begin set new.id = ifnull((select max(id) from XLD_CI_PROPERTIES), -1) + 1; end;"
        );
        /////////////////////////////////////////

        /// XLD.CREATE.CI_PROPERTY.STRING or BOOLEAN_VALUE or INTEGER or DATE //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.STRING_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (\"ci_id\", \"name\", \"string_value\") select id, :propertyName, :string_value from XLD_CIS where \"ci_type\" = :ciName"
        );
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.BOOLEAN_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (\"ci_id\", \"name\", \"boolean_value\") select id, :propertyName, :boolean_value from XLD_CIS where \"ci_type\" = :ciName"
        );
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.INTEGER_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (\"ci_id\", \"name\", \"integer_value\") select id, :propertyName, :integer_value from XLD_CIS where \"ci_type\" = :ciName"
        );
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.DATE_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (\"ci_id\", \"name\", \"date_value\") select id, :propertyName, :date_value from XLD_CIS where \"ci_type\" = :ciName"
        );

        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.STRING_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (ci_id, name, string_value) select id, :propertyName, :string_value from XLD_CIS where ci_type = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.BOOLEAN_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (ci_id, name, boolean_value) select id, :propertyName, :boolean_value from XLD_CIS where ci_type = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.INTEGER_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (ci_id, name, integer_value) select id, :propertyName, :integer_value from XLD_CIS where ci_type = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.DATE_VALUE.name(), 
            "insert into XLD_CI_PROPERTIES (ci_id, name, date_value) select id, :propertyName, :date_value from XLD_CIS where ci_type = :ciName"
        );
        /////////////////////////////////////////

        /// XLD.CREATE.CI_PROPERTY.STRING or BOOLEAN or INTEGER or DATE.preview ////////////////////////////////////// 
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.STRING_VALUE.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :ciName"
        );
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.BOOLEAN_VALUE.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :ciName"
        );
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.INTEGER_VALUE.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :ciName"
        );
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.DATE_VALUE.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :ciName"
        );

        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.STRING_VALUE.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.BOOLEAN_VALUE.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.INTEGER_VALUE.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+"."+PropertyValueType.DATE_VALUE.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :ciName"
        );
        /////////////////////////////////////////

        /// XLD.CREATE.CI_PROPERTY.droptrigger //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+".droptrigger", 
            "drop trigger ci_properties_insert_trigger"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+".droptrigger", 
            "drop trigger if exists ci_properties_insert_trigger"
        );
        /////////////////////////////////////////

        /// XLD.CREATE.CI_PROPERTY.preview //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+".preview", 
            "select count(\"ci_type\") from XLD_CIS where \"ci_type\" = :ciName"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.CREATE.name()+"."+ItemType.CI_PROPERTY.name()+".preview", 
            "select count(ci_type) from XLD_CIS where ci_type = :ciName"
        );
        /////////////////////////////////////////

        /// XLD.UPDATE.CI_PROPERTY //////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI_PROPERTY.name(), 
            "update XLD_CI_PROPERTIES set \"name\" = :newValue where \"name\" = :oldValue and \"ci_id\" in (select id from XLD_CIS where \"ci_type\" = :ciName)"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI_PROPERTY.name(), 
            "update XLD_CI_PROPERTIES set name = :newValue where name = :oldValue and ci_id in (select id from XLD_CIS where ci_type = :ciName)"
        );
        /////////////////////////////////////////

        /// XLD.UPDATE.CI_PROPERTY.preview /////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI_PROPERTY.name()+".preview", 
            "select count(\"name\") from XLD_CI_PROPERTIES where \"name\" = :oldValue and \"ci_id\" in (select id from XLD_CIS where \"ci_type\" = :ciName)"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI_PROPERTY.name()+".preview", 
            "select count(name) from XLD_CI_PROPERTIES where name = :oldValue and ci_id in (select id from XLD_CIS where ci_type = :ciName)"
        );
        ////////////////////////////////////////

        /// XLD.DELETE.CI_PROPERTY /////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI_PROPERTY.name(), 
            "delete from XLD_CI_PROPERTIES where \"name\" = :propertyName and \"ci_id\" in (select id from XLD_CIS where \"ci_type\" = :ciName)"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI_PROPERTY.name(), 
            "delete from XLD_CI_PROPERTIES where name = :propertyName and ci_id in (select id from XLD_CIS where ci_type = :ciName)"
        );
        ////////////////////////////////////////

        /// XLD.DELETE.CI_PROPERTY.preview /////////////////////////////////////
        derbyMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI_PROPERTY.name()+".preview", 
            "select count(\"name\") from XLD_CI_PROPERTIES where \"name\" = :propertyName and \"ci_id\" in (select id from XLD_CIS where \"ci_type\" = :ciName)"
        );
        mySqlMap.put(
            SystemType.XLD.name()+"."+ActionType.DELETE.name()+"."+ItemType.CI_PROPERTY.name()+".preview", 
            "select count(name) from XLD_CI_PROPERTIES where name = :propertyName and ci_id in (select id from XLD_CIS where ci_type = :ciName)"
        );
        ////////////////////////////////////////

        /// XLR ACTIONS ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /// XLR.UPDATE.CI //////////////////////////////////////
        h2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "select ci_uid, content from XLR_CONFIGURATIONS where ci_type = :oldValue; "
            +"update XLR_CONFIGURATIONS set ci_type = ?, content = ? where ci_uid = ?"
        );
        mySqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "select ci_uid, content from XLR_CONFIGURATIONS where ci_type = :oldValue; "
            +"update XLR_CONFIGURATIONS set ci_type = ?, content = ? where ci_uid = ?"
        );
        postgresqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "select ci_uid, content from XLR_CONFIGURATIONS where ci_type = :oldValue; "
            +"update XLR_CONFIGURATIONS set ci_type = ?, content = ? where ci_uid = ?"
        );
        oracleMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "select ci_uid, content from XLR_CONFIGURATIONS where ci_type = :oldValue; "
            +"update XLR_CONFIGURATIONS set ci_type = ?, content = ? where ci_uid = ?"
        );
        db2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "select ci_uid, content from XLR_CONFIGURATIONS where ci_type = :oldValue; "
            +"update XLR_CONFIGURATIONS set ci_type = ?, content = ? where ci_uid = ?"
        );
        sqlserverMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name(), 
            "select ci_uid, content from XLR_CONFIGURATIONS where ci_type = :oldValue; "
            +"update XLR_CONFIGURATIONS set ci_type = ?, content = ? where ci_uid = ?"
        );
        /////////////////////////////////////////

        /// XLR.UPDATE.CI.preview //////////////////////////////////////
        h2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLR_CONFIGURATIONS where ci_type = :oldValue"
        );
        mySqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLR_CONFIGURATIONS where ci_type = :oldValue"
        );
        postgresqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLR_CONFIGURATIONS where ci_type = :oldValue"
        );
        oracleMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLR_CONFIGURATIONS where ci_type = :oldValue"
        );
        db2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLR_CONFIGURATIONS where ci_type = :oldValue"
        );
        sqlserverMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.CI.name()+".preview", 
            "select count(ci_type) from XLR_CONFIGURATIONS where ci_type = :oldValue"
        );
        /////////////////////////////////////////

        /// XLR.UPDATE.TASK //////////////////////////////////////
        h2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name(),
            "select distinct release_uid from XLR_TASKS where task_type = ?; " 
            +"select content from XLR_RELEASES_DATA where ci_uid = ?; "
            +"update XLR_RELEASES_DATA set content = ? where ci_uid = ?; "
            +"select task_id, content from XLR_TASK_BACKUPS where ci_uid = ?; "
            +"update XLR_TASK_BACKUPS set content = ? where task_id = ?; "
            +"update XLR_TASKS set task_type = ? where task_type = ?;"
            +"select distinct releaseid from TASKS where tasktype = ?;"
            +"select releasejson from RELEASES where releaseid = ?;"
            +"update RELEASES set releasejson = ? where releaseid = ?;"
            +"update TASKS set tasktype = ? where tasktype = ? "
        );
        
        mySqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name(),
            "select distinct release_uid from XLR_TASKS where task_type = ?; " 
            +"select content from XLR_RELEASES_DATA where ci_uid = ?; "
            +"update XLR_RELEASES_DATA set content = ? where ci_uid = ?; "
            +"select task_id, content from XLR_TASK_BACKUPS where ci_uid = ?; "
            +"update XLR_TASK_BACKUPS set content = ? where task_id = ?; "
            +"update XLR_TASKS set task_type = ? where task_type = ?;"
            +"select distinct releaseid from TASKS where tasktype = ?;"
            +"select releasejson from RELEASES where releaseid = ?;"
            +"update RELEASES set releasejson = ? where releaseid = ?;"
            +"update TASKS set tasktype = ? where tasktype = ? "
        );
        postgresqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name(),
            "select distinct release_uid from XLR_TASKS where task_type = ?; " 
            +"select content from XLR_RELEASES_DATA where ci_uid = ?; "
            +"update XLR_RELEASES_DATA set content = ? where ci_uid = ?; "
            +"select task_id, content from XLR_TASK_BACKUPS where ci_uid = ?; "
            +"update XLR_TASK_BACKUPS set content = ? where task_id = ?; "
            +"update XLR_TASKS set task_type = ? where task_type = ?;"
            +"select distinct releaseid from TASKS where tasktype = ?;"
            +"select releasejson from RELEASES where releaseid = ?;"
            +"update RELEASES set releasejson = ? where releaseid = ?;"
            +"update TASKS set tasktype = ? where tasktype = ? "
        );
        oracleMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name(),
            "select distinct release_uid from XLR_TASKS where task_type = ?; " 
            +"select content from XLR_RELEASES_DATA where ci_uid = ?; "
            +"update XLR_RELEASES_DATA set content = ? where ci_uid = ?; "
            +"select task_id, content from XLR_TASK_BACKUPS where ci_uid = ?; "
            +"update XLR_TASK_BACKUPS set content = ? where task_id = ?; "
            +"update XLR_TASKS set task_type = ? where task_type = ?;"
            +"select distinct releaseid from TASKS where tasktype = ?;"
            +"select releasejson from RELEASES where releaseid = ?;"
            +"update RELEASES set releasejson = ? where releaseid = ?;"
            +"update TASKS set tasktype = ? where tasktype = ? "
        );
        db2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name(),
            "select distinct release_uid from XLR_TASKS where task_type = ?; " 
            +"select content from XLR_RELEASES_DATA where ci_uid = ?; "
            +"update XLR_RELEASES_DATA set content = ? where ci_uid = ?; "
            +"select task_id, content from XLR_TASK_BACKUPS where ci_uid = ?; "
            +"update XLR_TASK_BACKUPS set content = ? where task_id = ?; "
            +"update XLR_TASKS set task_type = ? where task_type = ?;"
            +"select distinct releaseid from TASKS where tasktype = ?;"
            +"select releasejson from RELEASES where releaseid = ?;"
            +"update RELEASES set releasejson = ? where releaseid = ?;"
            +"update TASKS set tasktype = ? where tasktype = ? "
        );
        sqlserverMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name(),
            "select distinct release_uid from XLR_TASKS where task_type = ?; " 
            +"select content from XLR_RELEASES_DATA where ci_uid = ?; "
            +"update XLR_RELEASES_DATA set content = ? where ci_uid = ?; "
            +"select task_id, content from XLR_TASK_BACKUPS where ci_uid = ?; "
            +"update XLR_TASK_BACKUPS set content = ? where task_id = ?; "
            +"update XLR_TASKS set task_type = ? where task_type = ?;"
            +"select distinct releaseid from TASKS where tasktype = ?;"
            +"select releasejson from RELEASES where releaseid = ?;"
            +"update RELEASES set releasejson = ? where releaseid = ?;"
            +"update TASKS set tasktype = ? where tasktype = ? "
        );
        /////////////////////////////////////////

        /// XLR.UPDATE.TASK.preview //////////////////////////////////////
        h2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name()+".preview", 
            "select count(task_type) from XLR_TASKS where task_type = :oldValue"
        );
        mySqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name()+".preview", 
            "select count(task_type) from XLR_TASKS where task_type = :oldValue"
        );
        postgresqlMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name()+".preview", 
            "select count(task_type) from XLR_TASKS where task_type = :oldValue"
        );
        oracleMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name()+".preview", 
            "select count(task_type) from XLR_TASKS where task_type = :oldValue"
        );
        db2Map.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name()+".preview", 
            "select count(task_type) from XLR_TASKS where task_type = :oldValue"
        );
        sqlserverMap.put(
            SystemType.XLR.name()+"."+ActionType.UPDATE.name()+"."+ItemType.TASK.name()+".preview", 
            "select count(task_type) from XLR_TASKS where task_type = :oldValue"
        );
        /////////////////////////////////////////

        masterMap.put(DbType.DERBY.name(), derbyMap);
        masterMap.put(DbType.MYSQL.name(), mySqlMap);
        masterMap.put(DbType.H2.name(), h2Map);
        masterMap.put(DbType.POSTGRESQL.name(), h2Map);
        masterMap.put(DbType.ORACLE.name(), h2Map);
        masterMap.put(DbType.DB2.name(), h2Map);
        masterMap.put(DbType.SQLSERVER.name(), h2Map);


        ALL_SQL_STMTS = Collections.unmodifiableMap(masterMap);
    }

    public static Map<String, Map<String, String>> getAllMap() {
        return ALL_SQL_STMTS;
    }
    
}