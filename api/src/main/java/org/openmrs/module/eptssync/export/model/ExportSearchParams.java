package org.openmrs.module.eptssync.export.model;

import java.sql.Connection;

import org.openmrs.module.eptssync.controller.conf.SyncTableConfiguration;
import org.openmrs.module.eptssync.engine.RecordLimits;
import org.openmrs.module.eptssync.engine.SyncSearchParams;
import org.openmrs.module.eptssync.model.SearchClauses;
import org.openmrs.module.eptssync.model.SearchParamsDAO;
import org.openmrs.module.eptssync.model.pojo.generic.DatabaseObject;
import org.openmrs.module.eptssync.utilities.db.conn.DBException;

public class ExportSearchParams extends SyncSearchParams<DatabaseObject>{
	private boolean selectAllRecords;
	
	public ExportSearchParams(SyncTableConfiguration tableInfo, RecordLimits limits, Connection conn) {
		super(tableInfo, limits);
		
		setOrderByFields(tableInfo.getPrimaryKey());
	}
	
	@Override
	public SearchClauses<DatabaseObject> generateSearchClauses(Connection conn) throws DBException {
		SearchClauses<DatabaseObject> searchClauses = new SearchClauses<DatabaseObject>(this);
		
		
		if (tableInfo.isFromOpenMRSModel() && tableInfo.getTableName().equalsIgnoreCase("patient")) {
			searchClauses.addColumnToSelect("patient.*, person.uuid");
			searchClauses.addToClauseFrom("inner join person on person.person_id = patient_id");
		}
		else {
			searchClauses.addColumnToSelect("*");
		}
		
		searchClauses.addToClauseFrom("inner join " + tableInfo.generateFullStageTableName() + " on record_origin_id  = " + tableInfo.getPrimaryKey());
		
		if (!this.selectAllRecords) {
			if (limits != null) {
				searchClauses.addToClauses(tableInfo.getPrimaryKey() + " between ? and ?");
				searchClauses.addToParameters(this.limits.getCurrentFirstRecordId());
				searchClauses.addToParameters(this.limits.getCurrentLastRecordId());
			}
		
			if (this.tableInfo.getExtraConditionForExport() != null) {
				searchClauses.addToClauses(tableInfo.getExtraConditionForExport());
			}
		}
		
		searchClauses.addToClauses("consistent = 1");

		return searchClauses;
	}	
	
	@Override
	public Class<DatabaseObject> getRecordClass() {
		return this.tableInfo.getSyncRecordClass(tableInfo.getRelatedSynconfiguration().getMainApp());
	}

	@Override
	public int countAllRecords(Connection conn) throws DBException {
		ExportSearchParams auxSearchParams = new ExportSearchParams(this.tableInfo, this.limits, conn);
		auxSearchParams.selectAllRecords = true;
		
		return SearchParamsDAO.countAll(auxSearchParams, conn);
	}

	@Override
	public synchronized int countNotProcessedRecords(Connection conn) throws DBException {
		RecordLimits bkpLimits = this.limits;
		
		this.limits = null;
		
		int count = SearchParamsDAO.countAll(this, conn);
		
		this.limits = bkpLimits;
		
		return count;
	}
}