/**
 * Copyright 2019 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xebialabs.migration.util;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamedParamStatement 
{
    private PreparedStatement prepStmt;
    private List<String> fields = new ArrayList<String>();
    private int index = 0;

    public NamedParamStatement(Connection conn, String sql) throws SQLException 
    {
        Pattern findParametersPattern = Pattern.compile("(?<!')(:[\\w]*)(?!')"); 
        Matcher matcher = findParametersPattern.matcher(sql); 
        while (matcher.find()) { 
            fields.add(index, matcher.group().substring(1)); 
            index++;
        } 
        this.prepStmt = conn.prepareStatement(sql.replaceAll(findParametersPattern.pattern(), "?"));
    }

    public List<String> getFields(){
        return this.fields;
    }

    public PreparedStatement getPreparedStatement() {
        return this.prepStmt;
    }

    public ResultSet executeQuery() throws SQLException {
        return this.prepStmt.executeQuery();
    }

    public int executeUpdate() throws SQLException {
        return this.prepStmt.executeUpdate();
    }

    public void close() throws SQLException {
        this.prepStmt.close();
    }

    public void setString(int index, String value) throws SQLException {
        this.prepStmt.setString(getParameterIndex(index), value);
    }

    public void setBoolean(int index, String value) throws SQLException {
        Boolean b = new Boolean(value);
        this.prepStmt.setBoolean(getParameterIndex(index), b);
    }

    public void setInteger(int index, String value) throws SQLException {
        Integer i = new Integer(value);
        this.prepStmt.setInt(getParameterIndex(index), i);
    }

    public void setDate(int index, String value) throws SQLException {
        Timestamp timestamp = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
            Date parsedDate = dateFormat.parse(value);
            timestamp = new java.sql.Timestamp(parsedDate.getTime());
        } catch(Exception e) { 
            e.printStackTrace(System.out);
        }
        this.prepStmt.setTimestamp(getParameterIndex(index), timestamp);
    }

    public void setBlob(int index, Blob blob) throws SQLException{
        this.prepStmt.setBlob(getParameterIndex(index), blob);
    }

    private int getParameterIndex(int index) {
        return index+1;
    }
}
