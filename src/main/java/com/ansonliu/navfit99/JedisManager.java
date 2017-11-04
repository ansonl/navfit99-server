package com.ansonliu.navfit99;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.net.*;
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

public class JedisManager {
	private static JedisPool pool;
	static {
		JedisPoolConfig config = new JedisPoolConfig();
		URI redisConnectionURI = null;
		try {
			redisConnectionURI = new URI(System.getenv("REDIS_URL"));
		} catch (URISyntaxException ex) {
			System.out.println("Creating URI for redis exception: " + ex.getMessage());
		}
		if (redisConnectionURI != null) {
			pool = new JedisPool(config, redisConnectionURI);
			//System.out.println("Created redis connection pool.");
		}
	}

	//private static String allNavFitPrefix = "allNavFits";

	private static String navfitPrefix = "navfit";
	private static String navfitDataPrefix = "data";
	private static String navfitEditorPrefix = "editors";

	private static String editorPrefix = "editor";
	private static String editorAuthTokenPrefix = "tokens";

	public static boolean checkNavFitUUIDExists(String navfitUUID) {
		try (Jedis jedis = pool.getResource()) {
			String navfitDataKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitDataPrefix);

		  return jedis.exists(navfitDataKey).booleanValue();
		}	
	}

	private static void setExpirationForEditorID(String editorID, Jedis jedis) {
		String editorTokenKey = String.format("%s:%s:%s", editorPrefix, editorID, editorAuthTokenPrefix);

		jedis.expire(editorID, 60*60);
	}

	private static boolean authenticateEditorIDForAuthToken(String editorID, String authToken, Jedis jedis) {
		String editorTokenKey = String.format("%s:%s:%s", editorPrefix, editorID, editorAuthTokenPrefix);

		boolean reply = jedis.sismember(editorTokenKey, authToken).booleanValue();

		//extend all auth token expirations for editorID
		if (reply) {
			setExpirationForEditorID(editorID, jedis);
		}

		return reply;
	}

	//Check user authenticity -> If navfit is public -> If user is editor for navfit
	private static boolean authenticateEditorIDAndAuthTokenForNavFitUUID(String editorID, String authToken, String navfitUUID, Jedis jedis) {
		String navfitDataKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitDataPrefix);
		String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

		if (editorID != null && authToken != null) {
			if (authenticateEditorIDForAuthToken(editorID, authToken, jedis)) {
				if (jedis.exists(navfitEditorKey).booleanValue()) { //check if navfit has editor list
					return jedis.sismember(navfitEditorKey, editorID).booleanValue();
				} else {
					return true; //return true if no editor list exists for existing navfit, navfit is public
				}
			} 
		} else { //user no supply editorID or authToken
			if (jedis.exists(navfitEditorKey).booleanValue()) { //check if navfit has editor list
				return false;
			} else {
				return true; //return true if no editor list exists for existing navfit, navfit is public
			}
		}
		return false;
	}

	public static boolean getEditorListForNavFitUUID(String navfitUUID, String editorID, String authToken) {
		try (Jedis jedis = pool.getResource()) {
			//authenticate provided editorID and authToken
			if (authenticateEditorIDForAuthToken(editorID, authToken, jedis)) {
				addEditorForNavFitUUID(navfitUUID, editorID, jedis);
				return true;
			} else {
				return false;
			}
		}
	}

	private static void addEditorForNavFitUUID(String navfitUUID, String editorID, Jedis jedis) {
		String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

		Long reply = jedis.sadd(navfitEditorKey, editorID);	
	}

	//Add editor to NavFit WITHOUT checking if editor is on editors list for NavFit
	public static boolean addEditorForNavFitUUID(String navfitUUID, String editorID, String authToken) {
		try (Jedis jedis = pool.getResource()) {
			//authenticate provided editorID and authToken
			if (authenticateEditorIDForAuthToken(editorID, authToken, jedis)) {
				addEditorForNavFitUUID(navfitUUID, editorID, jedis);
				return true;
			} else {
				return false;
			}
		}
	}

	//Add addedEditorID to NavFit after checking if existingEditorID has access to NavFit
	public static boolean addEditorForNavFitUUID(String navfitUUID, String addedEditorID, String existingEditorID, String authToken) {
		try (Jedis jedis = pool.getResource()) {
		  //authenticate provided editorID and authToken
		  if (authenticateEditorIDAndAuthTokenForNavFitUUID(existingEditorID, authToken, navfitUUID, jedis)) {
		  	addEditorForNavFitUUID(navfitUUID, addedEditorID, jedis);
		  	return true;
			} else {
				return false;
			}
		}
	}

	private static void removeEditorForNavFitUUID(String navfitUUID, String editorID, Jedis jedis) {
		  
	  String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

		Long reply = jedis.srem(navfitEditorKey, editorID); 
	}

	public static void removeEditorForNavFitUUID(String navfitUUID, String removedEditorID, String existingEditorID, String authToken) {
		try (Jedis jedis = pool.getResource()) {
			//authenticate provided editorID and authToken
		  if (authenticateEditorIDAndAuthTokenForNavFitUUID(existingEditorID, authToken, navfitUUID, jedis)) {
		  	removeEditorForNavFitUUID(navfitUUID, removedEditorID, jedis);
		  }
		}		  
	}

	private static String getNavFitDataForNavFitUUID(String navfitUUID, Jedis jedis) throws Exception {
		String navfitDataKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitDataPrefix);

		String reply = jedis.get(navfitDataKey);

		String aesOutput = Encryptor.decrypt(Encryptor.padTrimString(System.getenv("NAVFIT_AES_KEY"), 16), Encryptor.padTrimString(navfitUUID, 16), reply);

		return aesOutput;
	}

	public static String getNavFitDataForNavFitUUID(String navfitUUID, String editorID, String authToken) throws Exception {
		try (Jedis jedis = pool.getResource()) {	

		  String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

	  	if (authenticateEditorIDAndAuthTokenForNavFitUUID(editorID, authToken, navfitUUID, jedis)) {
	  		return getNavFitDataForNavFitUUID(navfitUUID, jedis);
	  	} else {
	  		throw new Exception(String.format("Invalid editorID and authToken supplied. OR NAVFIT does not exist. OR You are not on the editor list for this NAVFIT."));
	  	}
		}
	}

	private static void setNavFitDataForNavFitUUID(String navfitUUID, NavFitDatabase navfitObj, boolean expire, Jedis jedis) throws Exception {
		String navfitDataKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitDataPrefix);
		String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

		String navfitJSONString = navfitObj.toJSONString();

		String aesOutput = Encryptor.encrypt(Encryptor.padTrimString(System.getenv("NAVFIT_AES_KEY"), 16), Encryptor.padTrimString(navfitUUID, 16), navfitJSONString);

		String reply;
	  if (expire) {
	  	reply = jedis.setex(navfitDataKey, 60*60, aesOutput);
	  	if (reply.equals("OK"))
	  		jedis.expire(navfitEditorKey, 60*60);
	  } else {
	  	reply = jedis.set(navfitDataKey, aesOutput);
	  }

	  if (!reply.equals("OK")) {
	  	throw new Exception(String.format("SET key %s failed with reply %S", navfitDataKey, reply));
	  }
	}

	public static void setNavFitDataForNavFitUUID(String navfitUUID, String editorID, String authToken, NavFitDatabase navfitObj, boolean expire) throws Exception {
		try (Jedis jedis = pool.getResource()) {		  
		  
		  String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

		  if (authenticateEditorIDAndAuthTokenForNavFitUUID(editorID, authToken, navfitUUID, jedis)) {
	  		setNavFitDataForNavFitUUID(navfitUUID, navfitObj, expire, jedis);
	  	} else {
	  		throw new Exception(String.format("SET uuid %s has editor list. Invalid editorID and authToken supplied.", navfitUUID));
	  	}
		}
	}

	private static void removeNavFitDataForNavFitUUID(String navfitUUID, Jedis jedis) throws Exception {
		String navfitDataKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitDataPrefix);
		String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

		Long reply = jedis.del(String.format("%S %S", navfitDataKey, navfitEditorKey));
	}

	public static void removeNavFitDataForNavFitUUID(String navfitUUID, String editorID, String authToken) throws Exception {
		try (Jedis jedis = pool.getResource()) {		  
		  String navfitEditorKey = String.format("%s:%s:%s", navfitPrefix, navfitUUID, navfitEditorPrefix);

		  if (authenticateEditorIDAndAuthTokenForNavFitUUID(editorID, authToken, navfitUUID, jedis)) {
	  		removeNavFitDataForNavFitUUID(navfitUUID, jedis);
	  	} else {
	  		throw new Exception(String.format("DEL uuid %s has editor list. Invalid editorID and authToken supplied.", navfitUUID));
	  	}
		}
	}
}