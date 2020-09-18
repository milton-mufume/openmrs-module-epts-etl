package org.openmrs.module.eptssync.engine.export;

import java.util.List;

import org.openmrs.module.eptssync.controller.conf.SyncTableInfo;
import org.openmrs.module.eptssync.controller.export_.SyncExportController;
import org.openmrs.module.eptssync.engine.RecordLimits;
import org.openmrs.module.eptssync.engine.SyncEngine;
import org.openmrs.module.eptssync.engine.SyncSearchParams;
import org.openmrs.module.eptssync.model.SearchParamsDAO;
import org.openmrs.module.eptssync.model.SyncJSONInfo;
import org.openmrs.module.eptssync.model.base.SyncRecord;
import org.openmrs.module.eptssync.model.export.SyncExportSearchParams;
import org.openmrs.module.eptssync.model.openmrs.generic.OpenMRSObject;
import org.openmrs.module.eptssync.model.openmrs.generic.OpenMRSObjectDAO;
import org.openmrs.module.eptssync.utilities.db.conn.DBConnectionService;
import org.openmrs.module.eptssync.utilities.db.conn.DBException;
import org.openmrs.module.eptssync.utilities.db.conn.OpenConnection;
import org.openmrs.module.eptssync.utilities.io.FileUtilities;

public class ExportSyncEngine extends SyncEngine {
	
	public ExportSyncEngine(SyncTableInfo syncTableInfo, RecordLimits limits, SyncExportController syncController) {
		super(syncTableInfo, limits, syncController);
	}

	@Override	
	public List<SyncRecord> searchNextRecords(){
		OpenConnection conn = openConnection();
		
		try {
			return  utilities.parseList(SearchParamsDAO.search(this.searchParams, conn), SyncRecord.class);
			
		} catch (DBException e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
		finally {
			conn.finalizeConnection();
		}
	}
	
	@Override
	public SyncExportController getSyncController() {
		return (SyncExportController) super.getSyncController();
	}
	
	@Override
	protected void restart() {
	}
	
	@Override
	public void performeSync(List<SyncRecord> syncRecords) {
		List<OpenMRSObject> syncRecordsAsOpenMRSObjects = utilities.parseList(syncRecords, OpenMRSObject.class);
		
		for (OpenMRSObject obj : syncRecordsAsOpenMRSObjects) {
			obj.setOriginAppLocationCode(getSyncTableInfo().getOriginAppLocationCode());
		}
		
		this.syncController.logInfo("GENERATING '"+syncRecords.size() + "' " + getSyncTableInfo().getTableName() + " TO JSON FILE");
		
		SyncJSONInfo jsonInfo = SyncJSONInfo.generate(syncRecordsAsOpenMRSObjects);
		jsonInfo.setOriginAppLocationCode(getSyncTableInfo().getOriginAppLocationCode());
		
		this.syncController.logInfo("WRITING '"+syncRecords.size() + "' " + getSyncTableInfo().getTableName() + " TO JSON FILE [" + generateJSONFileName(jsonInfo) + "]");
		
		FileUtilities.write(generateJSONFileName(jsonInfo), jsonInfo.parseToJSON());
		
		this.syncController.logInfo("JSON [" + generateJSONFileName(jsonInfo) + "] CREATED!");
		
		this.syncController.logInfo("MARKING '"+syncRecords.size() + "' " + getSyncTableInfo().getTableName() + " AS SYNCHRONIZED");
			
		markAllAsSynchronized(utilities.parseList(syncRecords, OpenMRSObject.class));
		
		this.syncController.logInfo("MARKING '"+syncRecords.size() + "' " + getSyncTableInfo().getTableName() + " AS SYNCHRONIZED FINISHED");
	}

	private void markAllAsSynchronized(List<OpenMRSObject> syncRecords) {
		OpenConnection conn = DBConnectionService.getInstance().openConnection();
		
		try {
			
			OpenMRSObjectDAO.refreshLastSyncDate(syncRecords, conn);
			
			/*
			for (OpenMRSObject syncRecord : syncRecords) {
				syncRecord.refreshLastSyncDate(conn);
			}*/
			
			conn.markAsSuccessifullyTerminected();
		} 
		catch (DBException e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
		finally {
			conn.finalizeConnection();
		}
	}

	private String generateJSONFileName(SyncJSONInfo jsonInfo) {
		return getSyncController().generateJSONFileName(jsonInfo, getSyncTableInfo());
	}

	@Override
	public void requestStop() {
	}

	@Override
	protected SyncSearchParams<? extends SyncRecord> initSearchParams(RecordLimits limits) {
		SyncSearchParams<? extends SyncRecord> searchParams = new SyncExportSearchParams(this.syncTableInfo, limits);
		searchParams.setQtdRecordPerSelected(getSyncTableInfo().getQtyRecordsPerSelect());
	
		return searchParams;
	}
}
