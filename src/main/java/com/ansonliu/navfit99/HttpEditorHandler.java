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

public class HttpEditorHandler implements HttpHandler {
	public String processAndMakeResponse(HttpExchange t) {
		//Decode request body
  	Map<String, String> bodyMap = HttpExchangeUtilities.getRequestBodyMapFromHttpExchange(t);

    //Get editorID and authToken provided
    String editorID = null;
    String authToken = null;
    if (bodyMap.containsKey(Constants.editorIDKey) && bodyMap.containsKey(Constants.authTokenKey)) {
      //Store navfit parsed data to Redis
      editorID = bodyMap.get(Constants.editorIDKey);
      authToken = bodyMap.get(Constants.authTokenKey);
    }

		if (editorID == null || authToken == null)
			return (new JSONResponse(-1, "Missing " + Constants.editorIDKey + " " + Constants.authTokenKey, null, null)).toJSONString();

    Set<String> navfitsList = JedisManager.getNavFitListForEditorID(editorID, authToken);
    List<String> list = new ArrayList(navfitsList);
    if (list == null)
    	return (new JSONResponse(-1, "Unable to get navfit list for editor. Incorrect editorID and authToken or editor does not exist.", null, null)).toJSONString();
    else
    	return (new JSONResponse(0, null, list, null)).toJSONString();
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