package com.ansonliu.navfit99;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;

import java.lang.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.net.*;
import java.security.*;
import org.json.simple.*;

//import org.apache.commons.fileupload.*;



import org.apache.commons.fileupload.FileItem;

import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;



import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.util.UUID;

/*
 * /file?item=folder
 * /file?item=evalfitrep
 */

//https://stackoverflow.com/questions/33732110/file-upload-using-httphandler/33827895#33827895

public class HttpUploadHandler implements HttpHandler {

	private UUID storeUploadedFile(FileItem fi) {
		UUID fileUUID = NavFitManagement.generateNewFileUUID();
    File file = new File(String.format("%s.%s", fileUUID.toString(), Constants.dbExtension));
    try {
    	fi.write(file);

/*
    	File folder = new File("./");
File[] listOfFiles = folder.listFiles();

    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        System.out.println("File " + listOfFiles[i].getName());
      } else if (listOfFiles[i].isDirectory()) {
        System.out.println("Directory " + listOfFiles[i].getName());
      }
    }
    */

    } catch (Exception ex) {
      System.out.println(this.getClass() + "Write File Exception" + ex.getClass() + ": " + ex.getMessage());
    }
    return fileUUID;
	}

	public String processAndMakeResponse(HttpExchange t) {
		/*
		for (Entry < String, List < String >> header: t.getRequestHeaders().entrySet()) {
      System.out.println(header.getKey() + ": " + header.getValue().get(0));
    }
    */
    DiskFileItemFactory d = new DiskFileItemFactory();

    try {
      ServletFileUpload up = new ServletFileUpload(d);
      List < FileItem > result = up.parseRequest(new RequestContext() {

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public int getContentLength() {
            return 0; //tested to work with 0 as return
        }

        @Override
        public String getContentType() {
            return t.getRequestHeaders().getFirst("Content-type");
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return t.getRequestBody();
        }

      });
      
      Map<String, String> bodyMap = HttpExchangeUtilities.getRequestBodyMapFromFileItemList(result);

      for (FileItem item : result) {
        if (!item.isFormField()) {
          String fieldName = item.getFieldName();
          String fileName = item.getName();
          String contentType = item.getContentType();
          boolean isInMemory = item.isInMemory();
          long sizeInBytes = item.getSize();

          FileItem fi = item;
          UUID fileUUID = storeUploadedFile(fi);

          //System.out.println("Found uploaded file");

          //If editorID and authToken provided, set editors for NavFit
          String editorID = null;
          String authToken = null;
          if (bodyMap.containsKey(Constants.editorIDKey) && bodyMap.containsKey(Constants.authTokenKey) && bodyMap.get(Constants.editorIDKey).length() > 0 && bodyMap.get(Constants.authTokenKey).length() > 0) {
            //Store navfit parsed data to Redis
            editorID = bodyMap.get(Constants.editorIDKey);
            authToken = bodyMap.get(Constants.authTokenKey);
            //Set permissions for editor
            if (!JedisManager.addEditorForNavFitUUID(fileUUID.toString(), editorID, authToken)) {
              //Abort upload if editor id and authtoken no match
              return (new JSONResponse(-1, "Invalid editorID and authToken", null, null)).toJSONString();
            }
          }

          //Set NavFit data
          try {
            //Read NavFit file to object
            NavFitDatabase uploadedNavFit = new NavFitDatabase(fileUUID.toString());

            //SET data on database
            JedisManager.setNavFitDataForNavFitUUID(fileUUID.toString(), editorID, authToken, uploadedNavFit, true);

            return (new JSONResponse(0, fileUUID.toString(), null, null)).toJSONString();
          } catch (NavFit99FatalException ex) {
            return (new JSONResponse(-1, "Error creating NavFit file. " + ex.getMessage(), null, null)).toJSONString();
          } finally {
            try {
              NavFitManagement.deleteFile(fileUUID.toString());
            } catch (NoSuchFileException x) {
              System.err.format("%s: no such" + " file or directory%n", fileUUID.toString());
            } catch (DirectoryNotEmptyException x) {
              System.err.format("%s not empty%n", fileUUID.toString());
            } catch (IOException x) {
              // File permission problems are caught here.
              System.err.println(x);
            }
          }

        }
      }
      
      return (new JSONResponse(-1, "No file received.", null, null)).toJSONString();

      /*
      for(FileItem fi : result) {
          os.write(fi.getName().getBytes());
          os.write("\r\n".getBytes());
          os.write((new JSONResponse(0, null, null)).toJSONString().getBytes());
          System.out.println("File-Item: " + fi.getFieldName() + " = " + fi.getName());
          os.close();
      }
      */
      

    } catch (Exception ex) {
      System.out.println(this.getClass() + "Handle file upload exception" + ex.getClass() + ": " + ex.getMessage());
      return (new JSONResponse(-1, "Error uploading NavFit", null, null)).toJSONString();
    }
	}

  @Override
  public void handle(final HttpExchange t) throws IOException {
    String response = processAndMakeResponse(t);
		System.out.println(response);

		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		t.getResponseHeaders().add("Content-type", "application/json");
    t.sendResponseHeaders(200, response.length());

		t.getResponseBody().write(response.getBytes());
    t.getResponseBody().close();
  }
}