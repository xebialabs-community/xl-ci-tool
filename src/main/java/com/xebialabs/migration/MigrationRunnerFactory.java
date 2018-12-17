package com.xebialabs.migration;

import java.io.PrintStream;
import java.sql.Connection;

public class MigrationRunnerFactory {

    public static MigrationRunner getMigrationRunner(SystemType xlSystem, Connection dbconn,  DbType dbType, 
        Connection reportDbconn, DbType reportDbType, PrintStream os){

        MigrationRunner runner = null;

        switch (xlSystem){
            case XLR :
                runner = new MigrationRunnerXLR(xlSystem, dbconn, dbType, reportDbconn, reportDbType, os);
                break;
            default:
                runner = new MigrationRunnerXLD(xlSystem, dbconn, dbType, reportDbconn, reportDbType, os);
                break;
        }
        return runner;
    }

}