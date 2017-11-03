package com.ansonliu.navfit99;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.sql.*;

import org.json.simple.*;

public class Folder extends NavFitItem {
	public static final String tableName = "Folders";
	public static final String[] columnNames = {"FolderName","FolderID","Parent","Name","Rate","Desig","SSN","Active","TAR","Inactive","ATADSW","UIC","ShipStation","PromotionStatus","DateReported","Periodic","DetInd","Frocking","Special","FromDate","ToDate","NOB","Regular","Concurrent","OpsCdr","PhysicalReadiness","BilletSubcat","ReportingSenior","RSGrade","RSDesig","RSTitle","RSUIC","RSSSN","Achievements","PrimaryDuty","Duties","DateCounseled","Counseler","PROF","QUAL","EO","MIL","PA","TEAM","LEAD","MIS","TAC","RecommendA","RecommendB","Rater","RaterDate","CommentsPitch","Comments","Qualifications","PromotionRecom","SummarySP","SummaryProg","SummaryProm","SummaryMP","SummaryEP","RetentionYes","RetentionNo","RSAddress","SeniorRater","SeniorRaterDate","StatementYes","StatementNo","RSInfo","AutoSummary","UserComments"};

	public void saveInSQL(Connection accessConn) {
		insertItemIntoDB(accessConn, this.tableName, this.columnNames);
	}

	public Folder(ResultSet rs) {
		super();
		loadDataFromResultSet(rs, this.columnNames);
	}

	public Folder(JSONObject obj) {
		super();
		loadDataFromJSONObject(obj, this.columnNames);
	}
	

	/*
	public String folderName;
	public Number folderID;
	public Number parent;
	public String name;
	public String rate;
	public String desig;
	public String ssn;
	public Boolean active;
	public Boolean tar;
	public Boolean inactive;
	public Boolean atadsw;
	public String uic;
	public String shipStation;
	public String PromotionStatus;
	public Date dateReported;
	public Boolean periodic;
	public Boolean detInd;
	public Boolean frocking;
	public Boolean special;
	public Date fromDate;
	public Date toDate;
	public Boolean nob;
	public Boolean regular;
	public Boolean concurrent;
	public Boolean opsCdr;
	public String physicalReadiness;
	public String billetSubcat;
	public String reportingSenior;
	public String rsGrade;
	public String rsDesig;
	public String rsTitle;
	public String rsUIC;
	public String rsSSN;
	public String achievements;
	public String primaryDuty;
	public String duties;
	public String dateCounseled;
	public String counseler;
	public Number prof;
	public Number qual;
	public Number eo;
	public Number mil;
	public Number pa;
	public Number team;
	public Number lead;
	public Number mis;
	public Number tac;
	public String recommendA;
	public String recommendB;
	public String rater;
	public Date raterDate;
	public String commentsPitch;
	public String comments;
	public String qualifications;
	public Number promotionRecom;
	public String summarySP;
	public String summaryProg;
	public String summaryMP;
	public String summaryEP;
	public Boolean retentionYes;
	public Boolean retentionNo;
	public String rsAddress;
	public String seniorRater;
	public Date seniorRaterDate;
	public Boolean statementYes;
	public Boolean statementNo;
	public String rsInfo;
	public Boolean autoSummary;
	public String userComments;
	public String password;
	*/
}