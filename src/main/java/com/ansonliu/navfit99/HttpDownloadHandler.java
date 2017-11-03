package com.ansonliu.navfit99;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.net.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

//https://stackoverflow.com/questions/15902662/how-to-serve-static-content-using-suns-simple-httpserver

public class HttpDownloadHandler implements HttpHandler {

	public String processAndMakeResponse(HttpExchange t) {
		//Decode request body
    Map<String, String> bodyMap = HttpExchangeUtilities.getRequestBodyMapFromHttpExchange(t);

    //Get fileUUID
    String fileUUID = null;
    if (bodyMap.containsKey(Constants.fileUUIDKey)) {
      fileUUID = bodyMap.get(Constants.fileUUIDKey);
      //Strip query to prevent directory walk
			fileUUID = fileUUID.replaceAll("[\\.\\\\\\/]", "");

			//Check if navfit exists
			if (!JedisManager.checkNavFitUUIDExists(fileUUID)) {
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

    try {
    	NavFitDatabase tmp = NavFitManagement.getNavFitDatabaseFromString(JedisManager.getNavFitDataForNavFitUUID(fileUUID, editorID, authToken));
    	tmp.overwriteFileUUID(fileUUID);

    	File navfitFile = new File(String.format("%s.%s",fileUUID,Constants.dbExtension));
    	FileInputStream fs = new FileInputStream(navfitFile);
	    ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
	    int b;
	    while ((b = fs.read()) >= 0) {
	      outputBuffer.write(b);
	    }
	    fs.close();

	    //Write directly to response object, don't convert to string, return null for this function
	    t.getResponseHeaders().add("Content-type", "application/msaccess");
	    t.sendResponseHeaders(200, outputBuffer.size());
	    t.getResponseBody().write(outputBuffer.toByteArray());
	    t.getResponseBody().close();
	    return null;   

    } catch (Exception ex) {
    	return (new JSONResponse(-1, ex.getMessage(), null, null)).toJSONString();
    }
	}

  @Override
  public void handle(HttpExchange t) throws IOException {
		String response = processAndMakeResponse(t);
		if (response == null) {
			return;
		} else {
			t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			t.getResponseHeaders().add("Content-type", "application/json");
	    t.sendResponseHeaders(200, response.length());

			t.getResponseBody().write(response.getBytes());
	    t.getResponseBody().close();
		}
	}
}