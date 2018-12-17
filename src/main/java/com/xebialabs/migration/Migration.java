package com.xebialabs.migration;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import com.xebialabs.migration.action.RepositoryAction;
import com.xebialabs.migration.parser.MigrationConfigParser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;


public class Migration {

    // Sortable to hold all dbView for this plugin TODO
    private ArrayList<RepositoryAction> reposActionList = new ArrayList<RepositoryAction>();

    private SystemType systemType = null;
    private DbType dbType = null;
    private String dbUrl = "";
    private String dbUsername = "";
    private String dbPwd = null;
    private String dbDriver = "";
    private String userInputDbPwd = null;
    private DbType reportDbType = null;
    private String reportDbUrl = "";
    private String reportDbUsername = "";
    private String reportDbPwd = null;
    private String reportDbDriver = "";
    private String userInputReportDbPwd = null;
    private String installDir = null;
    private String filePath = null;
    private Connection conn = null;
    private Connection reportConn = null;
    private boolean previewOnly = false;

    // XLR Defaults
    private static final String XLR_JDBC_DRIVER = "org.h2.Driver";   
    private static final String XLR_DB_URL_PREFIX = "jdbc:h2:";
    private static final String XLR_DB_URL_SUFFIX = "/repository/db";  
    private static final String XLR_USER = "sa"; 
    private static final String XLR_PWD = "";
    private static final String XLR_DBCONFIG_LOC = "/conf/xl-release.conf";

    private static final String XLR_REPORT_JDBC_DRIVER = "org.apache.derby.jdbc.AutoloadedDriver";   
    private static final String XLR_REPORT_DB_URL_PREFIX = "jdbc:derby:";
    private static final String XLR_REPORT_DB_URL_SUFFIX = "/archive/db";  
    private static final String XLR_REPORT_USER = ""; 
    private static final String XLR_REPORT_PWD = "";


    // XLD Defaults
    private static final String XLD_JDBC_DRIVER = "org.apache.derby.jdbc.AutoloadedDriver";   
    private static final String XLD_DB_URL_PREFIX = "jdbc:derby:";
    private static final String XLD_DB_URL_SUFFIX = "/repository/db";  
    private static final String XLD_USER = ""; 
    private static final String XLD_PWD = "";
    private static final String XLD_DBCONFIG_LOC = "/conf/xl-deploy.conf";

    // 


    public String getGreeting() {
        return "Beginning Migration";
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, 
    ParseException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Migration processor = new Migration();
        
        // create CLI Options object
        Options options = new Options();
        options.addOption("f", true, "Required - Supply the full path and file name for the mapping file ");
        options.addOption("i", true, "Required - Supply the full path to the XL Deploy or XL Release installation directory ");
        options.addOption("pw", false, "Optional - If this flag is set, user will be prompted for the database password");
        options.addOption("reportpw", false, "Optional - If this flag is set, user will be prompted for the report database password");
        options.addOption("preview", false, "Optional - If set, the application will only preview the mapping actions. The database will not be changed.");
        
        String header = "Application to update XL CI and Task Types\n\n";
        String footer = "\n";
        HelpFormatter formatter = new HelpFormatter();
        

        try {
               
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse( options, args);

            processor.filePath = cmd.getOptionValue("f");
            processor.installDir = cmd.getOptionValue("i");
            

            // if we have the path to the config file and the install directory
            if(processor.filePath != null && processor.installDir != null)
            {
                System.out.println(processor.getGreeting());
                // Process installDir
                if (processor.installDir != null && processor.installDir.endsWith(File.separator)){
                    processor.installDir = processor.installDir.substring(0,processor.installDir.length()-1);
                }

                // Prompt user to input db password
                if ( cmd.hasOption("pw") ) 
                {
                    Console console = System.console();

                    processor.userInputDbPwd =
                        new String(console.readPassword("Please enter the database password: "));
                    
                }

                // Prompt user to input db password
                if ( cmd.hasOption("reportpw") ) 
                {
                    Console console = System.console();

                    processor.userInputReportDbPwd =
                        new String(console.readPassword("Please enter the report database password: "));
                    
                }

                // If option invoked, application will only preview the actions, not implement
                if ( cmd.hasOption("preview") ) 
                {
                    System.out.println("Preview has been set. Application will preview the results of the mapping actions but will not change the database.");
                    processor.previewOnly = true;
                }

                // Load jar from the installDir/lib directory into the classpath
                //    so we have access to the database drivers
                String moduleFolder = processor.installDir+"/lib";
                loadJars(moduleFolder);

                // Parse the ConfigFile
                MigrationConfigParser configParser = new MigrationConfigParser();
                configParser.parseConfigFile(processor.filePath);
                setConfigurationValues(processor, configParser);
                
                // Begin database connection configuration
                // Try to find the database configuraton values in the xlr or xld config files
                String dbConfigFileLoc = null;
                
                if (SystemType.XLR == processor.systemType){
                    dbConfigFileLoc = processor.installDir+XLR_DBCONFIG_LOC;
                } else {
                    dbConfigFileLoc = processor.installDir+XLD_DBCONFIG_LOC; 
                }
                    
                configParser.parseDbConfiguration(dbConfigFileLoc, processor.systemType);
                setDbValues(processor, configParser);
                System.out.println("Configuration is complete. The following values will be used: "+processor);

                try {
                    // Create database connection
                    // Register JDBC driver 
                    Class.forName(processor.dbDriver);
                    Class.forName(processor.reportDbDriver); 
            
                    // Open a connection 
                    System.out.println("Connecting to database..."); 
                    processor.conn = DriverManager.getConnection(processor.dbUrl, processor.dbUsername, processor.dbPwd);

                    if(SystemType.XLR == processor.systemType)
                    {
                        //Open connection to report database
                        System.out.println("Connecting to reporting database..."); 
                        processor.reportConn = DriverManager.getConnection(processor.reportDbUrl, processor.reportDbUsername, processor.reportDbPwd);

                    }
                    
                    System.out.println("Beginning Actions\n");
                    MigrationRunner runner = MigrationRunnerFactory.getMigrationRunner(processor.systemType, processor.conn,  processor.dbType, 
                        processor.reportConn, processor.reportDbType, System.out);
                    runner.process(processor.reposActionList, processor.previewOnly);
                    // Clean-up environment 
                } catch(SQLException se) { 
                // Handle errors for JDBC 
                se.printStackTrace(); 
                } catch(Exception e) { 
                // Handle errors for Class.forName 
                e.printStackTrace(); 
                } finally { 
                    // finally block used to close resources 
                        try { 
                            if(processor.conn!=null) processor.conn.close(); 
                        } catch(SQLException se) { 
                            se.printStackTrace(); 
                    } // end finally try 
                } // end try 
                System.out.println("\nFinished processing actions."); 

            } else {
                formatter.printHelp("CI Migration Tool",header, options, footer, true);
            }
        } catch (UnrecognizedOptionException e) {
            formatter.printHelp("CI Migration Tool",header, options, footer, true);
        } 
            
    }

    @Override
    public String toString(){
        String outputStr = "system = "+systemType.name()+", dbType = "+dbType.name()
        +", dbDriver = "+dbDriver
        +", dbUrl = "+dbUrl+", dbUsername = "+dbUsername+", dbPwd = "+
        ((userInputDbPwd != null && userInputDbPwd.length()>0)?"User Entered":"Using Default");
        if(systemType != null && SystemType.XLR == systemType)
        {
            outputStr = outputStr + ", reportDbType = " +reportDbType+", reportDbDriver = "+reportDbDriver
            +", reportDdbUrl = "+reportDbUrl+", reportDbUsername = "+reportDbUsername+", reportDbPwd = "+
            ((userInputReportDbPwd != null && userInputReportDbPwd.length()>0)?"User Entered":"Using Default");

        }
        return (outputStr);
    }

    private static void setConfigurationValues(Migration processor, MigrationConfigParser configParser){
        // parse the mapping file
        processor.reposActionList = configParser.getReposActionList();
        processor.systemType = SystemType.valueOf(configParser.getSystemType().toUpperCase());
    }

    private static void setDbValues(Migration processor, MigrationConfigParser configParser){
        processor.dbDriver = configParser.getDriver();
        processor.dbUrl = configParser.getUrl();
        processor.dbUsername = configParser.getUser();

        processor.reportDbDriver = configParser.getReportDriver();
        processor.reportDbUrl = configParser.getReportUrl();
        processor.reportDbUsername = configParser.getReportUser();

        // If we were not able to find information about a custom database will use default
        if (processor.dbDriver == null){
            if (SystemType.XLR == processor.systemType){
                processor.dbDriver = XLR_JDBC_DRIVER;
            } else {
                processor.dbDriver = XLD_JDBC_DRIVER;
            }
        }
        if (processor.dbUrl == null){
            if (SystemType.XLR == processor.systemType){
                processor.dbUrl = XLR_DB_URL_PREFIX + processor.installDir + XLR_DB_URL_SUFFIX;
            } else {
                processor.dbUrl = XLD_DB_URL_PREFIX + processor.installDir + XLD_DB_URL_SUFFIX;
            }
        }
        if (processor.dbUsername == null){
            if (SystemType.XLR == processor.systemType){
                processor.dbUsername = XLR_USER;
            } else {
                processor.dbUsername = XLD_USER;
             }
        }

        
        // Set the db type based upon the db driver
        if (processor.dbDriver != null) {
            if(processor.dbDriver.toUpperCase().contains(DbType.MYSQL.name())){
                processor.dbType = DbType.MYSQL;
            } else if(processor.dbDriver.toUpperCase().contains(DbType.DERBY.name())){
                processor.dbType = DbType.DERBY;
            } else if(processor.dbDriver.toUpperCase().contains(DbType.H2.name())){
                processor.dbType = DbType.H2;
            } else if(processor.dbDriver.toUpperCase().contains(DbType.ORACLE.name())){
                processor.dbType = DbType.ORACLE;
            } else if(processor.dbDriver.toUpperCase().contains(DbType.POSTGRESQL.name())){
                processor.dbType = DbType.POSTGRESQL;
            } else if(processor.dbDriver.toUpperCase().contains(DbType.SQLSERVER.name())){
                processor.dbType = DbType.SQLSERVER;
            }else {
                processor.dbType = DbType.DERBY;
            }
        } 
        // If user has supplied a db password, use it
        if(processor.userInputDbPwd != null && processor.userInputDbPwd.length()>0)
        {
            processor.dbPwd = processor.userInputDbPwd;
        } else {
            if (SystemType.XLR == processor.systemType){
                processor.dbPwd = XLR_PWD;
            } else {
                processor.dbPwd = XLD_PWD;
            }
        }

        // if this is XLR, do the same for the report Database info
        if (SystemType.XLR == processor.systemType){
            processor.reportDbDriver = configParser.getReportDriver();
            processor.reportDbUrl = configParser.getReportUrl();
            processor.reportDbUsername = configParser.getReportUser();

            // If we were not able to find information about a custom database will use default
            if (processor.reportDbDriver == null){
                
                processor.reportDbDriver = XLR_REPORT_JDBC_DRIVER;
            }
            if (processor.reportDbUrl == null){
                
                processor.reportDbUrl = XLR_REPORT_DB_URL_PREFIX + processor.installDir + XLR_REPORT_DB_URL_SUFFIX;
            }
            if (processor.reportDbUsername == null){
                
                processor.reportDbUsername = XLR_REPORT_USER;
            }

            // Set the db type based upon the db driver
            if (processor.reportDbDriver != null) {
                if(processor.reportDbDriver.toUpperCase().contains(DbType.MYSQL.name())){
                    processor.reportDbType = DbType.MYSQL;
                } else if(processor.reportDbDriver.toUpperCase().contains(DbType.DERBY.name())){
                    processor.reportDbType = DbType.DERBY;
                } else if(processor.reportDbDriver.toUpperCase().contains(DbType.H2.name())){
                    processor.reportDbType = DbType.H2;
                } else if(processor.reportDbDriver.toUpperCase().contains(DbType.ORACLE.name())){
                    processor.reportDbType = DbType.ORACLE;
                } else if(processor.reportDbDriver.toUpperCase().contains(DbType.POSTGRESQL.name())){
                    processor.reportDbType = DbType.POSTGRESQL;
                } else if(processor.reportDbDriver.toUpperCase().contains(DbType.SQLSERVER.name())){
                    processor.reportDbType = DbType.SQLSERVER;
                }else {
                    processor.reportDbType = DbType.DERBY;
                }
            } 

            // If user has supplied a db password, use it
            if(processor.userInputReportDbPwd != null && processor.userInputReportDbPwd.length()>0)
            {
                processor.reportDbPwd = processor.userInputReportDbPwd;
            } else {
                processor.reportDbPwd = XLR_REPORT_PWD;
            }

        } 

    }

    private static void loadJars (String moduleFolder) throws IOException, NoSuchMethodException, 
    IllegalAccessException, InvocationTargetException{
        File moduleDirectory = new File(moduleFolder);
         String classpath = System.getProperty("java.class.path");
         File[] moduleFiles = moduleDirectory.listFiles();
         for (int i = 0; i < moduleFiles.length; i++){
            File moduleFile = moduleFiles[i];
                if (moduleFile.getName().endsWith(".jar")){
                    if(classpath.indexOf(moduleFiles[i].getName()) == -1){
                    try{
                        addSoftwareLibrary(moduleFiles[i]);
                    }catch(IOException e){}
                }
                }else{
                    moduleFile.delete();
                }
            }
    }

    private static void addSoftwareLibrary(File file) throws IOException, NoSuchMethodException, 
        IllegalAccessException, InvocationTargetException {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{file.toURI().toURL()});
    }

}