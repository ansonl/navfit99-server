package com.ansonliu.navfit99;

import java.lang.*;
import java.nio.file.*;
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

public class HttpEditHandler implements HttpHandler {
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

    //Get editscope and editop
    Integer editScopeNumber = null;
    Integer editOpNumber = null;
    if (bodyMap.containsKey(Constants.editScopeKey) && bodyMap.containsKey(Constants.editOpKey)) {
    	try {
				editScopeNumber = Integer.valueOf(bodyMap.get(Constants.editScopeKey));
				editOpNumber = Integer.valueOf(bodyMap.get(Constants.editOpKey));
			} catch (NumberFormatException ex) {
				return (new JSONResponse(-1, Constants.editScopeKey + " " + Constants.editOpKey + " not parsable. Must be numbers.", null, null)).toJSONString();
			}
    } else {
    	return (new JSONResponse(-1, "Missing " + Constants.editScopeKey + " " + Constants.editOpKey + " query parameters.", null, null)).toJSONString();
    }

		NavFitDatabase updatedNavFit;
		try {
			updatedNavFit = NavFitManagement.getNavFitDatabaseFromString(JedisManager.getNavFitDataForNavFitUUID(targetUUID, editorID, authToken));
		} catch (Exception ex) {
			return (new JSONResponse(-1, ex.getMessage(), null, null)).toJSONString();
		}

		switch (editScopeNumber) {
			case 0: //entire NavFit scope
				switch (editOpNumber) {
					case 1: //new
						return (new JSONResponse(-1, "Use /newFile to create new NavFit. ", null, null)).toJSONString(); 
					case 2: //update
						//Read navfit database from client and make into object
						updatedNavFit = NavFitManagement.getNavFitDatabaseFromString(bodyMap.get(Constants.newDatakey));
						//System.out.println(clientNavFit.toJSONString());
						//Overwrite old file
						//NavFitManagement.writeNavFitToFileSystem(updatedNavFit, targetUUID);

						if (NavFitManagement.writeNavFitToRedis(updatedNavFit, targetUUID, editorID, authToken))
							return (new JSONResponse(0, "NavFit updated (overwrite mode). ", null, null)).toJSONString();
						else 
							return (new JSONResponse(-1, "Update fail", null, null)).toJSONString();
					case 3: //delete
						try {
						  NavFitManagement.deleteFile(targetUUID);
						} catch (NoSuchFileException x) {
						  System.err.format("%s: no such" + " file or directory%n", targetUUID);
						  return (new JSONResponse(-1, "Could not delete NavFit", null, null)).toJSONString();
						} catch (DirectoryNotEmptyException x) {
						  System.err.format("%s not empty%n", targetUUID);
						  return (new JSONResponse(-1, "Could not delete NavFit", null, null)).toJSONString();
						} catch (IOException x) {
						  // File permission problems are caught here.
						  System.err.println(x);
						  return (new JSONResponse(-1, "Could not delete NavFit", null, null)).toJSONString();
						}
						return (new JSONResponse(0, "NavFit deleted.", null, null)).toJSONString();
					default:
						return (new JSONResponse(-1, "Invalid " + Constants.editOpKey + " " + editOpNumber, null, null)).toJSONString();
				}
			case 1: //single Folder scope
				//Load Folder obj with client data
				Folder clientFolder = NavFitManagement.getFolderFromString(bodyMap.get(Constants.newDatakey));
				if (!clientFolder.valueMap.containsKey("FolderID")) {
					return (new JSONResponse(-1, "Missing FolderID in new Folder", null, null)).toJSONString();
				}

				switch (editOpNumber) {
					case 1: //new
					case 2: //update
						if (!updatedNavFit.updateFolderForFolderID(clientFolder)) {
							return (new JSONResponse(-1, "Update failed", null, null)).toJSONString();
						}
						//Overwrite old file
						//NavFitManagement.writeNavFitToFileSystem(updatedNavFit, targetUUID);

						if (NavFitManagement.writeNavFitToRedis(updatedNavFit, targetUUID, editorID, authToken)) {
							return (new JSONResponse(0, "Folder updated", null, null)).toJSONString();
						} else {
							return (new JSONResponse(-1, "Update fail", null, null)).toJSONString();
						}
						
					case 3: //delete
						Integer clientFolderID = Integer.valueOf(clientFolder.valueMap.get("FolderID").toString());
						if (!updatedNavFit.deleteFolderForFolderID(clientFolderID)) {
							return (new JSONResponse(-1, "Could not delete NavFit", null, null)).toJSONString();
						} 
						return (new JSONResponse(0, clientFolderID + "Folder deleted.", null, null)).toJSONString();
					default:
						return (new JSONResponse(-1, "Invalid " + Constants.editOpKey, null, null)).toJSONString();
				}
			case 2: //single Report (EvalFitRep) scope
				//Load EvalFitRep obj with client data
				EvalFitRep clientReport = NavFitManagement.getReportFromString(bodyMap.get(Constants.newDatakey));
				if (!clientReport.valueMap.containsKey("ReportID")) {
					return (new JSONResponse(-1, "Missing ReportID in new clientReport", null, null)).toJSONString();
				}

				switch (editOpNumber) {
					case 1: //new
					case 2: //update
						if (!updatedNavFit.updateReportForReportID(clientReport)) {
							return (new JSONResponse(-1, "Could not delete NavFit", null, null)).toJSONString();
						}
						//Overwrite old file
						//NavFitManagement.writeNavFitToFileSystem(updatedNavFit, targetUUID);

						if (NavFitManagement.writeNavFitToRedis(updatedNavFit, targetUUID, editorID, authToken))
							return (new JSONResponse(0, "Report updated", null, null)).toJSONString();
						else
							return (new JSONResponse(-1, "Update fail", null, null)).toJSONString();
					case 3: //delete
						Integer clientReportID = Integer.valueOf(clientReport.valueMap.get("ReportID").toString());
						if (!updatedNavFit.deleteReportForReportID(clientReportID)) {
							return (new JSONResponse(-1, "Could not delete NavFit", null, null)).toJSONString();
						} 
						return (new JSONResponse(0, clientReportID + "Report deleted.", null, null)).toJSONString();
					default:
						return (new JSONResponse(-1, "Invalid " + Constants.editOpKey, null, null)).toJSONString();
				}
			default:
				return (new JSONResponse(-1, "Invalid edit scope ", null, null)).toJSONString();
		}
			
		//Object for navfit file to overwrite
		//NavFitDatabase targetNavFit = new NavFitDatabase(targetUUID);

		//Read navfit database from client and make into object
					
		/*
		//Compare md5 hash of target and client data converted to JSONString
		//Edit safety for future implementation.
		String navfitDataDigestServer = null;
		String navfitDataDigestClient = null;
		try {
			MessageDigest mdServer;
			mdServer = MessageDigest.getInstance("MD5");
			mdServer.update(targetNavFit.toJSONString().getBytes());
			navfitDataDigestServer = (new HexBinaryAdapter()).marshal(mdServer.digest());

			MessageDigest mdClient;
			mdClient = MessageDigest.getInstance("MD5");
			mdClient.update(targetNavFit.toJSONString().getBytes());
			navfitDataDigestClient = (new HexBinaryAdapter()).marshal(mdClient.digest());
		} catch (NoSuchAlgorithmException ex) {
			System.out.println(this.getClass() + "No such algorithm MD5 " + ex.getMessage());
		}
		
		if (navfitDataDigestServer != null && navfitDataDigestClient != null && navfitDataDigestServer.equals(navfitDataDigestClient)) {
			//digests match, do edit!
			response = (new JSONResponse(0, null, targetNavFit, navfitDataDigest)).toJSONString();
		} else {
			response = (new JSONResponse(-1, "File has been editted to newer revision. Edit not performed. ", null, null)).toJSONString();
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
}