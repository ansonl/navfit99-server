package com.ansonliu.navfit99;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.sql.*;

import org.json.simple.*;

public class NavFitItem implements JSONAware {
	public Map<String, Object> valueMap = new HashMap<String, Object>();

	//Stringify all date objects to create valid json
	public Map<String, Object> createFixedMap() {
		Map fixedMap = new HashMap();
		for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
	    String key = entry.getKey();
	    Object value = entry.getValue();
    	
    	try {
		    if (Class.forName("java.util.Date").isInstance(value)) {
		    	fixedMap.put(key, value.toString());
		    } else {
		    	fixedMap.put(key, value);
		    }
		  } catch (ClassNotFoundException ex) {
		  	System.out.println(this.getClass() + "Get Java Date class exception." + ex.getClass() + ": " + ex.getMessage());
		  }
		} 
		return fixedMap;
	}

	@Override
	public String toJSONString() {
		org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
		obj.put("valueMap", createFixedMap());
		StringWriter jsonString = new StringWriter();
		try {
			obj.writeJSONString(jsonString);
		} catch (IOException ex) {
			System.out.println(this.getClass() + "Encode to JSON " + ex.getClass() + ": " + ex.getMessage());
		}
		return jsonString.toString();
	}

	public void insertItemIntoDB(Connection accessConn, String tableName, String[] keyNames) {
		StringBuilder keyListSQL = new StringBuilder();
		keyListSQL.append("(");

		StringBuilder valueListSQL = new StringBuilder();
		valueListSQL.append("(");

		StringBuilder valueListPreparedSQL = new StringBuilder();
		valueListPreparedSQL.append("(");

		int columnsToUpdate = 0;

		for (String key : keyNames) {	

			//System.out.println(key + key.getClass());
			//System.out.println(this.valueMap.get(key));

			//System.out.println(keyListSQL.toString());
			//System.out.println(valueListSQL.toString());

			if (this.valueMap.containsKey(key) && this.valueMap.get(key) != null ) {

				keyListSQL.append("[" + key + "]");
				keyListSQL.append(",");

				if (this.valueMap.get(key) instanceof String) {
					valueListSQL.append("'");
				}
				valueListSQL.append(this.valueMap.get(key).toString());
				valueListPreparedSQL.append("?");
				if (this.valueMap.get(key) instanceof String) {
					valueListSQL.append("'");
				}
				valueListSQL.append(",");
				valueListPreparedSQL.append(",");
			} else {
				//System.out.println("Missing keys in valuemap");
			}
		}
		keyListSQL.deleteCharAt(keyListSQL.length()-1);
		keyListSQL.append(")");

		valueListSQL.deleteCharAt(valueListSQL.length()-1);
		valueListSQL.append(")");

		valueListPreparedSQL.deleteCharAt(valueListPreparedSQL.length()-1);
		valueListPreparedSQL.append(")");

		//System.out.println(keyListSQL.toString() + valueListPreparedSQL.toString() + valueListSQL.toString());

		try {
			String preparedQuery = "INSERT INTO " + tableName + " " + keyListSQL.toString() + " VALUES " + valueListPreparedSQL.toString();
			//System.out.println(preparedQuery);
			PreparedStatement pst = accessConn.prepareStatement(preparedQuery);

			int paramNum = 0;
			for (String key : keyNames) {	
				if (this.valueMap.containsKey(key) && this.valueMap.get(key) != null ) {
					paramNum += 1;
					pst.setObject(paramNum, this.valueMap.get(key));
				}
			}
			pst.execute();


			/*
			Statement st = accessConn.createStatement();
			String query = "INSERT INTO " + tableName + " " + keyListSQL.toString() + " VALUES " + valueListSQL.toString();
			System.out.println(query);
			st.execute(query);
			st.close();
			*/
		} catch (SQLException ex) {
			System.out.println(this.getClass() + "Run SQL statement SQLException " + ex.getMessage());
		}
	}

	//Read SQL ResultSet into HashMap valueMap
	public void loadDataFromResultSet(ResultSet rs, String[] columnNames) {
		for (int columnNameIter = 0; columnNameIter < columnNames.length; columnNameIter++) {
			String targetColumnName = columnNames[columnNameIter];
			//System.out.println(rs.findColumn(targetColumnName));
			//System.out.println(rs.getObject(targetFolder.columnNames[columnNameIter]).getClass());
			Object targetObject;
			try {
				targetObject = rs.getObject(targetColumnName);
			} catch (SQLException ex) {
				targetObject = null;

				System.out.println("Decode ResultSet to Object SQLException: " + ex.getMessage());
			}
			this.valueMap.put(targetColumnName, targetObject);
		}
	}

	public void loadDataFromJSONObject(JSONObject obj, String[] columnNames) {
		for (String key : columnNames) {
			if (obj.containsKey(key))
				this.valueMap.put(key, obj.get(key));
			else
				System.out.println(this.getClass() + " Init data from JSONObject missing key " + key);
		}
	}

}