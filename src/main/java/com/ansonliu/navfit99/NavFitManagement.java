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


import net.ucanaccess.converters.TypesMap.AccessType;
import net.ucanaccess.ext.FunctionType;
import net.ucanaccess.jdbc.UcanaccessConnection;
import net.ucanaccess.jdbc.UcanaccessDriver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class NavFitManagement {
	public static UUID generateNewFileUUID() {
		UUID targetUUID = UUID.randomUUID();

		while(JedisManager.checkNavFitUUIDExists(targetUUID.toString())) {
			targetUUID = UUID.randomUUID();
		}
		return targetUUID;

		/*
		//Old method for checking if navfit file existed on disk already
		//Check if current navfit file exists
		File navfitFile = new File(targetUUID.toString() + Constants.dbExtension);
		while (navfitFile.exists()) {
			targetUUID = UUID.randomUUID();
		}
		*/
	}

		public static NavFitDatabase getNavFitDatabaseFromString(String data) {
    String newDataJSON = data;
    Object decodedObj = JSONValue.parse(newDataJSON);

    return new NavFitDatabase((JSONObject)decodedObj);
    //System.out.println((new NavFitDatabase((JSONObject)decodedObj)).toJSONString());
	}

		public static Folder getFolderFromString(String data) {
    String newDataJSON = data;
    Object decodedObj = JSONValue.parse(newDataJSON);

    return new Folder((JSONObject)decodedObj);
    //System.out.println((new NavFitDatabase((JSONObject)decodedObj)).toJSONString());
	}

	public static EvalFitRep getReportFromString(String data) {
    String newDataJSON = data;
    Object decodedObj = JSONValue.parse(newDataJSON);

    return new EvalFitRep((JSONObject)decodedObj);
    //System.out.println((new NavFitDatabase((JSONObject)decodedObj)).toJSONString());
	}

	public static void deleteFile(String fileUUID) throws NoSuchFileException, DirectoryNotEmptyException, IOException {
		//System.out.println(fileUUID + this.dbExtension);
		//File file = new File(fileUUID + this.dbExtension);
		File file = new File(String.format("%s.%s", fileUUID, Constants.dbExtension));
		Path filePath = file.toPath();
		//System.out.println(file.getAbsolutePath());

		/*
		try {
		    
		} catch (NoSuchFileException x) {
		    System.err.format("%s: no such" + " file or directory%n", filePath);
		} catch (DirectoryNotEmptyException x) {
		    System.err.format("%s not empty%n", filePath);
		} catch (IOException x) {
		    // File permission problems are caught here.
		    System.err.println(x);
		}
		*/

		Files.delete(filePath);
	}

	public static void writeNavFitToFileSystem(NavFitDatabase obj, String fileUUID) {
		obj.overwriteFileUUID(fileUUID);
	}

	public static boolean writeNavFitToRedis(NavFitDatabase obj, String fileUUID, String editorID, String authToken) {
		try {
			//SET data on database
	    JedisManager.setNavFitDataForNavFitUUID(fileUUID, editorID, authToken, obj, true);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

}