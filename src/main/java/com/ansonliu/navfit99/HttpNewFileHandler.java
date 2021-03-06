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

public class HttpNewFileHandler implements HttpHandler {
	public String processAndMakeResponse(HttpExchange t) {
		//Decode request body
    Map<String, String> bodyMap = HttpExchangeUtilities.getRequestBodyMapFromHttpExchange(t);

		UUID newUUID = NavFitManagement.generateNewFileUUID();

		/*
		//For writing new file to disk when persistent storage on disk
		NavFitDatabase newNavFit = new NavFitDatabase();
		newNavFit.overwriteFileUUID(newUUID.toString());
		 */

		//If editorID and authToken provided, set editors for NavFit
    String editorID = null;
    String authToken = null;
    
    if (bodyMap.containsKey(Constants.editorIDKey) && bodyMap.containsKey(Constants.authTokenKey)) {
      //Store navfit parsed data to Redis
      editorID = bodyMap.get(Constants.editorIDKey);
      authToken = bodyMap.get(Constants.authTokenKey);
      //Set permissions for editor, use special addEditorForNavFitUUID() method since no NaVFIT or editors for it exist
      if (!JedisManager.addEditorForNavFitUUID(newUUID.toString(), editorID, authToken)) {
      	return (new JSONResponse(-1, "Invalid editorID and authToken", null, null)).toJSONString();
      }
    }

    

    //SET data on database
    try {
    	JedisManager.setNavFitDataForNavFitUUID(newUUID.toString(), editorID, authToken, new NavFitDatabase(Constants.nf98aEmptyWithRootName), true);
    	return (new JSONResponse(0, newUUID.toString(), null, null)).toJSONString();
		} catch (Exception ex) {
			return (new JSONResponse(-1, ex.getMessage(), null, null)).toJSONString();
		}

		
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
}