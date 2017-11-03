package com.ansonliu.navfit99;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.net.*;
import java.security.*;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.json.simple.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/*
 * /file?item=folder
 * /file?item=evalfitrep
 */

public class HttpFileHandler implements HttpHandler {
	public String processAndMakeResponse(HttpExchange t) {
		//Decode request body
  	Map<String, String> bodyMap = HttpExchangeUtilities.getRequestBodyMapFromHttpExchange(t);

  	//Get fileUUID
    String targetUUID = null;
    if (bodyMap.containsKey(Constants.fileUUIDKey)) {
      targetUUID = bodyMap.get(Constants.fileUUIDKey);
      //Strip query to prevent directory walk
			targetUUID = targetUUID.replaceAll("[\\.\\\\\\/]", "");

			//Check if navfit exists
			if (!JedisManager.checkNavFitUUIDExists(targetUUID)) {
				return (new JSONResponse(-1, "NAVFIT does not exist.", null, null)).toJSONString();
			}

    } else { //Missing fileUUID
    	return (new JSONResponse(-1, "Missing " + Constants.fileUUIDKey + "  query parameter.", null, null)).toJSONString();
    }

    //If editorID and authToken provided
    String editorID = null;
    String authToken = null;
    if (bodyMap.containsKey(Constants.editorIDKey) && bodyMap.containsKey(Constants.authTokenKey)) {
      //Store navfit parsed data to Redis
      editorID = bodyMap.get(Constants.editorIDKey);
      authToken = bodyMap.get(Constants.authTokenKey);
    }

    //Get type (number)
    Integer typeNumber = null;
    if (bodyMap.containsKey(Constants.typeKey)) {
    	try {
				typeNumber = Integer.valueOf(bodyMap.get(Constants.typeKey));
			} catch (NumberFormatException ex) {
				return (new JSONResponse(-1, Constants.typeKey +  " not parsable. Must be number.", null, null)).toJSONString();
			}
    } else {
    	return (new JSONResponse(-1, "Missing " + Constants.typeKey + " query parameter.", null, null)).toJSONString();
    }

    //Create NAVFIT from Redis
		NavFitDatabase targetNavFit = null;
		try {
    	targetNavFit = NavFitManagement.getNavFitDatabaseFromString(JedisManager.getNavFitDataForNavFitUUID(targetUUID, editorID, authToken));
    } catch (Exception ex) {
    	return (new JSONResponse(-1, ex.getMessage(), null, null)).toJSONString();
    }

		switch(typeNumber) {
			case 1: //all folders list
				return (new JSONResponse(0, null, targetNavFit.createFixedFoldersMap(), null)).toJSONString();
			case 2: //all reports list
				return (new JSONResponse(0, null, targetNavFit.createFixedReportsMap(), null)).toJSONString();
			case 3: //reports for folder

				//Get typeData
		    Integer typeData = null;
		    if (bodyMap.containsKey(Constants.typeDataKey)) {
		    	try {
						typeData = Integer.valueOf(bodyMap.get(Constants.typeDataKey));
					} catch (NumberFormatException ex) {
						return (new JSONResponse(-1, Constants.typeDataKey +  " not parsable. Must be number.", null, null)).toJSONString();
					}
		    } else {
		    	return (new JSONResponse(-1, "Missing " + Constants.typeDataKey + " query parameter.", null, null)).toJSONString();
		    }

				Integer folderID = typeData;
				return (new JSONResponse(0, null, targetNavFit.createReportsForFolderIDMap(folderID), null)).toJSONString();

			default:
				return (new JSONResponse(0, null, targetNavFit, null)).toJSONString();
		}

		/*
		//For edit safety in future improvement
		String navfitDataDigest = null;
		try {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			md.update(targetNavFit.toJSONString().getBytes());
			navfitDataDigest = (new HexBinaryAdapter()).marshal(md.digest());
		} catch (NoSuchAlgorithmException ex) {
			System.out.println(this.getClass() + "No such algorithm MD5 " + ex.getMessage());
		}
		*/
	}

  @Override
  public void handle(HttpExchange t) throws IOException {
  	String response = processAndMakeResponse(t);
		System.out.println(response);

		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		t.getResponseHeaders().add("Content-type", "application/json");
    t.sendResponseHeaders(200, response.length());

		t.getResponseBody().write(response.getBytes());
    t.getResponseBody().close();
  }

  //https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
  public static Map<String, List<String>> splitQuery(URI url) throws UnsupportedEncodingException {
	  final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
	  final String[] pairs = url.getQuery().split("&");
	  for (String pair : pairs) {
	    final int idx = pair.indexOf("=");
	    final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
	    if (!query_pairs.containsKey(key)) {
	      query_pairs.put(key, new LinkedList<String>());
	    }
	    final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
	    query_pairs.get(key).add(value);
	  }
	  return query_pairs;
	}
}