package com.ansonliu.navfit99;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.sql.*;

import org.json.simple.*;

public class EvalFitRep extends NavFitItem {
	public static final String tableName = "Reports";
	public static final String[] columnNames = {"Parent","ReportID","ReportType","Name","Rate","Desig","SSN","Active","TAR","Inactive","ATADSW","UIC","ShipStation","PromotionStatus","DateReported","Periodic","DetInd","Frocking","Special","FromDate","ToDate","NOB","Regular","Concurrent","OpsCdr","PhysicalReadiness","BilletSubcat","ReportingSenior","RSGrade","RSDesig","RSTitle","RSUIC","RSSSN","Achievements","PrimaryDuty","Duties","DateCounseled","Counseler","PROF","QUAL","EO","MIL","PA","TEAM","LEAD","MIS","TAC","RecommendA","RecommendB","Rater","RaterDate","CommentsPitch","Comments","Qualifications","PromotionRecom","SummarySP","SummaryProg","SummaryProm","SummaryMP","SummaryEP","RetentionYes","RetentionNo","RSAddress","SeniorRater","SeniorRaterDate","StatementYes","StatementNo","RSInfo","UserComments","Password"};

	public void saveInSQL(Connection accessConn) {
		insertItemIntoDB(accessConn, this.tableName, this.columnNames);
	}

	public EvalFitRep(ResultSet rs) {
		super();
		loadDataFromResultSet(rs, this.columnNames);
	}

	public EvalFitRep(JSONObject obj) {
		super();
		loadDataFromJSONObject(obj, this.columnNames);
	}
}