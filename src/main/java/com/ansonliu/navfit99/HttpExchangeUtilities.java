package com.ansonliu.navfit99;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.net.*;
import org.json.simple.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.commons.fileupload.FileItem;


public class HttpExchangeUtilities {
	private static final String cookieHeader = "Cookie";
	

	public static Map<String, String> getCookieMapFromHTTPExchange(HttpExchange t) {
		Map<String, String> cookieMap = new HashMap<String, String>();

		// Testing getting cookies...
		if ( t.getRequestHeaders().get("Cookie") != null ) {
		  for (String cookieHeader : t.getRequestHeaders().get("Cookie") ) {
		      for (String cookieKVPair : cookieHeader.split(";")) {
		      	int i = 0;
		      	String keyBuf = null;
		      	for (String cookieKVAlternating: cookieKVPair.split("=")) {
		      		if (i % 2 == 0)
		      			keyBuf = cookieKVAlternating.trim();
		      		else
		      			cookieMap.put(keyBuf, cookieKVAlternating.trim());

		      		i += 1;
		      	}
		      }
		  }
		}

		/*
		//Print out Map for debugging
		for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
		  String key = entry.getKey();
		  String value = entry.getValue();
		  System.out.println(String.format("%s %s", key, value));
		}
		 */

		return cookieMap;
	}

	public static Map<String, String> getRequestBodyMapFromFileItemList(List<FileItem> results) {
		Map<String, String> bodyMap = new HashMap<String, String>();

		for (FileItem item : results) {
	    // Process a regular form field
	    if (item.isFormField()) {
	      String name = item.getFieldName();
	      String value = item.getString();
	      //System.out.println(String.format("Found form field %s %s", name, value));
	    	bodyMap.put(name, value);
	    }
	  }
	  return bodyMap;

	    /*
	    if (!item.isFormField()) {
        String fieldName = item.getFieldName();
        String fileName = item.getName();
        String contentType = item.getContentType();
        boolean isInMemory = item.isInMemory();
        long sizeInBytes = item.getSize();

        FileItem fi = item;
      }
       */
	}

	private static String decodeRequestBodyFromHttpExchange(HttpExchange t) {
		InputStream input = t.getRequestBody();
    StringBuilder stringBuilder = new StringBuilder();

    new BufferedReader(new InputStreamReader(input))
        .lines()
        .forEach( (String s) -> stringBuilder.append(s + "\n") );

    String decodedString = null;
    try {
    	decodedString = URLDecoder.decode(stringBuilder.toString(), "UTF-8");
    } catch (UnsupportedEncodingException ex) {
    	System.out.println("Decode exception " + ex.getMessage());
    }

    //System.out.println(decodedString);
    return decodedString;
	}

	public static Map<String, String> getRequestBodyMapFromHttpExchange(HttpExchange t) {
		
		Map<String, String> bodyMap = new HashMap<String, String>();

		String decodedBody = decodeRequestBodyFromHttpExchange(t);

    for (String bodyKVPair : decodedBody.split("&")) {
    	int i = 0;
    	String keyBuf = null;
    	for (String bodyKVAlternating: bodyKVPair.split("=")) {
    		if (i % 2 == 0)
    			keyBuf = bodyKVAlternating.trim();
    		else
    			bodyMap.put(keyBuf, bodyKVAlternating.trim());

    		i += 1;
    	}
    }
		/*
		//Print out Map for debugging
		for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
		  String key = entry.getKey();
		  String value = entry.getValue();
		  System.out.println(String.format("%s %s", key, value));
		}
		 */
		 

		return bodyMap;
	}

}