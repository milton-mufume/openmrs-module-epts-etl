package org.openmrs.module.eptssync.transport.model;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;

import org.openmrs.module.eptssync.controller.conf.SyncTableConfiguration;
import org.openmrs.module.eptssync.engine.RecordLimits;
import org.openmrs.module.eptssync.engine.SyncSearchParams;
import org.openmrs.module.eptssync.model.SearchClauses;
import org.openmrs.module.eptssync.model.openmrs.generic.OpenMRSObject;
import org.openmrs.module.eptssync.transport.controller.SyncTransportController;
import org.openmrs.module.eptssync.utilities.db.conn.DBException;

public class TransportSyncFilesSearchParams extends SyncSearchParams<OpenMRSObject> implements FilenameFilter{
	private String firstFileName;
	private String lastFileName;
	
	private String fileNamePathern;
	private SyncTransportController controller;
	
	public TransportSyncFilesSearchParams(SyncTransportController controller, SyncTableConfiguration tableInfo, RecordLimits limits) {
		super(tableInfo, limits);
		
		this.controller = controller;
		
		if (limits != null) {
			this.firstFileName = tableInfo.getTableName() + "_" + utilities.garantirXCaracterOnNumber(limits.getFirstRecordId(), 10) + ".json"; 
			this.lastFileName = tableInfo.getTableName() + "_" +  utilities.garantirXCaracterOnNumber(limits.getLastRecordId(), 10) + ".json"; 
		}
	}
	
	public String getFileNamePathern() {
		return fileNamePathern;
	}
	
	public void setFileNamePathern(String fileNamePathern) {
		this.fileNamePathern = fileNamePathern;
	}
	
	@Override
	public SearchClauses<OpenMRSObject> generateSearchClauses(Connection conn) throws DBException {
		return null;
	}	
	
	@Override
	public Class<OpenMRSObject> getRecordClass() {
		return this.tableInfo.getRecordClass();
	}
	
	@Override
	public boolean accept(File dir, String name) {
		boolean isJSON = name.toLowerCase().endsWith("json");
		boolean isNotMinimal = !name.toLowerCase().contains("minimal");
		
		boolean isInInterval = true;
		
		if (hasLimits()) {
			isInInterval = isInInterval && name.compareTo(this.firstFileName) >= 0;
			isInInterval = isInInterval && name.compareTo(this.lastFileName) <= 0;
		}
		
		boolean pathernOk = true;
		
		if (utilities.stringHasValue(this.fileNamePathern)) {
			pathernOk = name.contains(this.fileNamePathern);
		}
		
		return  isJSON && isNotMinimal && isInInterval && pathernOk;
	}
	
	@Override
	public int countAllRecords(Connection conn) throws DBException {
		return countNotProcessedRecords(conn);
	}

	@Override
	public int countNotProcessedRecords(Connection conn) throws DBException {
		File[] files = getSyncDirectory().listFiles(this);
		
		if (files != null) return files.length;
		
		return 0;
	}

	private File getSyncDirectory() {
		return controller.getSyncDirectory( tableInfo);
	}
}