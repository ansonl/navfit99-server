package com.ansonliu.navfit99;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardCopyOption.*;

import org.json.simple.*;

import net.ucanaccess.converters.TypesMap.AccessType;
import net.ucanaccess.ext.FunctionType;
import net.ucanaccess.jdbc.UcanaccessConnection;
import net.ucanaccess.jdbc.UcanaccessDriver;

public class NavFitDatabase implements JSONAware {
	//private static final String nf98aEmptyPath = "reference/NF98A_empty.accdb";
	private static final String[] navfitTables = {"Folders", "Reports", "Summary"};
	private static final String foldersKey = "folders";
	private static final String reportsKey = "reports";

	private static final String reportParentKey = "Parent";

	public ArrayList<Folder> folders = new ArrayList<Folder>();
	public ArrayList<EvalFitRep> reports = new ArrayList<EvalFitRep>();

	public ArrayList<Map<String,Object>> createFixedFoldersMap() {
		ArrayList<Map<String,Object>> foldersTmpMap = new ArrayList<Map<String,Object>>();
		for (Folder f : this.folders) {
			foldersTmpMap.add(f.createFixedMap());
		}
		return foldersTmpMap;
	}

	public ArrayList<Map<String,Object>> createFixedReportsMap() {
		ArrayList<Map<String,Object>> reportsTmpMap = new ArrayList<Map<String,Object>>();
		for (EvalFitRep f : this.reports) {
			reportsTmpMap.add(f.createFixedMap());
		}
		return reportsTmpMap;
	}

	public ArrayList<Map<String, Object>> createReportsForFolderIDMap(Integer folderID) {
		ArrayList<Map<String,Object>> reportsTmpMap = createFixedReportsMap();

		for (int i = reportsTmpMap.size()- 1; i >= 0; i--) {
			String parentKeyValue = reportsTmpMap.get(i).get(this.reportParentKey).toString();
			Integer parentFolderID = Integer.valueOf(parentKeyValue.substring(parentKeyValue.indexOf(' ')+1));
			
			if (!parentFolderID.equals(folderID)) {
				reportsTmpMap.remove(i);
			}
		}
		return reportsTmpMap;
	}

	@Override
	public String toJSONString() {
		org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
		obj.put(this.foldersKey, createFixedFoldersMap());
		obj.put(this.reportsKey, createFixedReportsMap());
		StringWriter jsonString = new StringWriter();
		try {
			obj.writeJSONString(jsonString);
		} catch (IOException ex) {
			System.out.println(this.getClass() + "Encode to JSON " + ex.getClass() + ": " + ex.getMessage());
		}
		return jsonString.toString();
	}

	public boolean updateFolderForFolderID(Folder newFolderObj) {
		if (!newFolderObj.valueMap.containsKey("FolderID")) {
			return false;
		}

		Integer newFolderID = Integer.valueOf(newFolderObj.valueMap.get("FolderID").toString());

		for (int i = 0; i < this.folders.size(); i++) {
			if (Integer.valueOf(this.folders.get(i).valueMap.get("FolderID").toString()).equals(newFolderID)) {
				this.folders.set(i, newFolderObj);
				return true;
			}
		}
		System.out.println("FolderID to update not found, adding new");
		this.folders.add(newFolderObj);
		return true;
	}

	public boolean updateReportForReportID(EvalFitRep newReportObj) {
		if (!newReportObj.valueMap.containsKey("ReportID")) {
			return false;
		}

		Integer newReportID = Integer.valueOf(newReportObj.valueMap.get("ReportID").toString());

		for (int i = 0; i < this.reports.size(); i++) {
			if (Integer.valueOf(this.reports.get(i).valueMap.get("ReportID").toString()).equals(newReportID)) {
				this.reports.set(i, newReportObj);
				return true;
			}
		}
		System.out.println("ReportID to update not found, adding new");
		this.reports.add(newReportObj);
		return true;
	}

	public boolean deleteFolderForFolderID(Integer folderID) {
		for (int i = 0; i < this.folders.size(); i++) {
			if (Integer.valueOf(this.folders.get(i).valueMap.get("FolderID").toString()).equals(folderID)) {
				this.folders.remove(i);
				return true;
			}
		}
		System.out.println("FolderID to delete not found");
		return false;
	}

	public boolean deleteReportForReportID(Integer reportID) {
		for (int i = 0; i < this.reports.size(); i++) {
			if (Integer.valueOf(this.reports.get(i).valueMap.get("ReportID").toString()).equals(reportID)) {
				this.reports.remove(i);
				return true;
			}
		}
		System.out.println("ReportID to delete not found");
		return false;
	}

	private static void copyFileUsingStream(File source, File dest) throws IOException {
    InputStream is = null;
    OutputStream os = null;
    try {
        is = new FileInputStream(source);
        os = new FileOutputStream(dest);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }
    } finally {
        is.close();
        os.close();
    }
}

	public void overwriteFileUUID(String fileUUID) {

		try {
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
		} catch (ClassNotFoundException ex) {
			System.out.println(this.getClass() + " ClassNotFoundException " + ex.getMessage());
		}

		try {
			File emptyFile = new File(Constants.nf98aEmptyPath);
			File navfitFile = new File(String.format("%s.%s",fileUUID,Constants.dbExtension));
			copyFileUsingStream(emptyFile, navfitFile);
		} catch (IOException ex) {
			System.out.println("Copy file failed " + ex.getMessage());
		}

		try {
			Connection accessConn;
			accessConn = DriverManager.getConnection(String.format("jdbc:ucanaccess://%s.%s;immediatelyReleaseResources=true", fileUUID, Constants.dbExtension));

			//System.out.println(folders.size());

			for (Folder folder : folders) {
				folder.saveInSQL(accessConn);
			}

			for (EvalFitRep report : reports) {
				report.saveInSQL(accessConn);
			}

			accessConn.commit();
			accessConn.close();

		} catch (SQLException ex) {
			System.out.println(this.getClass() + "Run SQL statement SQLException " + ex.getMessage());
		}
	}

	protected void loadFromDBFile(String filename) throws NavFit99FatalException {
		try {
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
		} catch (ClassNotFoundException ex) {
			System.out.println(this.getClass() + " ClassNotFoundException " + ex.getMessage());
			throw new NavFit99FatalException("UCanAccessDriver not found.");
		}

		
		try {
			//Remember to include UCANACCESS param to release resources or else we cannot delete the file.https://sourceforge.net/p/ucanaccess/discussion/help/thread/4539a56c/
			Connection accessConn = DriverManager.getConnection(String.format("jdbc:ucanaccess://%s;immediatelyReleaseResources=true", filename));

			Statement st = accessConn.createStatement();
			ResultSet rs;
			ResultSetMetaData rsmd;
			rs = st.executeQuery("SELECT * FROM " + navfitTables[0]);
			rsmd = rs.getMetaData();
			while(rs.next()){
				Folder targetFolder = new Folder(rs);
				folders.add(targetFolder);
			}
			rs = st.executeQuery("SELECT * FROM " + navfitTables[1]);
			rsmd = rs.getMetaData();
			while(rs.next()){
				EvalFitRep targetReport = new EvalFitRep(rs);
				reports.add(targetReport);
			}
			rs.close();
			st.close();
			accessConn.close();
		} catch (SQLException ex) {
			System.out.println(this.getClass() + " loadFromDBFile Run SQL statement SQLException " + ex.getMessage());
			throw new NavFit99FatalException("Error running SQL statement.");
		}
	}

	public NavFitDatabase(String fileUUID) throws NavFit99FatalException {
		loadFromDBFile(String.format("%s.%s",fileUUID,Constants.dbExtension));
	}

	public NavFitDatabase(JSONObject obj) {
		if (obj.containsKey(this.foldersKey) && obj.containsKey(this.reportsKey)) {
			Object foldersArr = obj.get(this.foldersKey);
			Object reportsArr = obj.get(this.reportsKey);

			for (Object folderObj : (JSONArray)foldersArr) {
				folders.add(new Folder((JSONObject)folderObj));
			}

			for (Object reportObj : (JSONArray)reportsArr) {
				reports.add(new EvalFitRep((JSONObject)reportObj));
			}

		} else {
			System.out.println("Init NavFitDatabase missing keys " + this.foldersKey + " and " + this.reportsKey);
		}
	}

	public NavFitDatabase() {
		super();

		try {
			loadFromDBFile(Constants.nf98aEmptyPath);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

}