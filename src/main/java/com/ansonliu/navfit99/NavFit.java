/*
 * Create empty directory at project directory level named "navfit99"
 * Compile navfit99 pcakage from project directory
 * javac -d . *.java
 * Run navfit99.NavFit
 * java navfit99.NavFit


 * Maven method
 * mvn clean package
 * java -jar target/navfit99-server-1.0-jar-with-dependencies.jar
 */

package com.ansonliu.navfit99;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import org.json.simple.*;

import net.ucanaccess.converters.TypesMap.AccessType;
import net.ucanaccess.ext.FunctionType;
import net.ucanaccess.jdbc.UcanaccessConnection;
import net.ucanaccess.jdbc.UcanaccessDriver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import redis.clients.jedis.*;

public class NavFit {

		public static void main(String[] args) {

		java.sql.Connection accessConn;

		HttpFileHandler fileHandler = new HttpFileHandler();

		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(Integer.valueOf(System.getenv("PORT"))), 0);
			server.createContext("/newFile", new HttpNewFileHandler());
			server.createContext("/upload", new HttpUploadHandler());
			server.createContext("/download", new HttpDownloadHandler());
			
			server.createContext("/file", new HttpFileHandler());
			server.createContext("/edit", new HttpEditHandler());
			server.start();

			
			
			NavFitDatabase emptyNavFitTest = new NavFitDatabase();
			//emptyNavFitTest.loadFromDBFile("reference/NF98A_empty.accdb");
			emptyNavFitTest.loadFromDBFile("reference/NF98A_empty.accdb");
			System.out.println("Loaded empty NAVFIT test.");
			//System.out.println(emptyNavFitTest.toJSONString());

			try {
				JedisManager.setNavFitDataForNavFitUUID("empty", null, null, emptyNavFitTest, false);
				System.out.println("Successfully SET empty NavFit data on Redis database.");
			} catch (Exception ex) {
				System.out.println("Redis exception: " + ex.getMessage());
			}


			
		} catch (Exception ex) {
			System.out.println("Exception message: " + ex.getClass() + ex.getMessage());	
		}
	}
}