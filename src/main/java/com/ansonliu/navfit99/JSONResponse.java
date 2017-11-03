package com.ansonliu.navfit99;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.sql.*;

import org.json.simple.*;

public class JSONResponse implements JSONAware {
	public Map valueMap = new HashMap();

	@Override
	public String toJSONString() {
		org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
		obj.put("Status", this.valueMap.get("Status"));
		obj.put("StatusData", this.valueMap.get("StatusData"));
		obj.put("NavFitDatabase", this.valueMap.get("NavFitDatabase"));
		obj.put("Digest", this.valueMap.get("Digest"));
		StringWriter jsonString = new StringWriter();
		try {
			obj.writeJSONString(jsonString);
		} catch (IOException ex) {
			System.out.println(this.getClass() + "Encode to JSON IOException: " + ex.getMessage());
		}
		return jsonString.toString();
	}

	public JSONResponse(Integer status, String statusData, Object navFitDatabase, String digest) {
		valueMap.put("Status", status);
		valueMap.put("StatusData", statusData);
		valueMap.put("NavFitDatabase", navFitDatabase);
		valueMap.put("Digest", digest);
	}

}