package org.openmrs.module.epts.etl.synchronization.model;

import java.sql.Connection;

import org.openmrs.module.epts.etl.common.model.SyncImportInfoSearchParams;
import org.openmrs.module.epts.etl.common.model.SyncImportInfoVO;
import org.openmrs.module.epts.etl.conf.AbstractTableConfiguration;
import org.openmrs.module.epts.etl.conf.EtlItemConfiguration;
import org.openmrs.module.epts.etl.engine.AbstractEtlSearchParams;
import org.openmrs.module.epts.etl.engine.RecordLimits;
import org.openmrs.module.epts.etl.etl.model.EtlDatabaseObjectSearchParams;
import org.openmrs.module.epts.etl.model.SearchClauses;
import org.openmrs.module.epts.etl.model.SearchParamsDAO;
import org.openmrs.module.epts.etl.model.base.VOLoaderHelper;
import org.openmrs.module.epts.etl.synchronization.controller.DatabaseMergeFromJSONController;
import org.openmrs.module.epts.etl.utilities.db.conn.DBException;

public class DataBaseMergeFromJSONSearchParams extends SyncImportInfoSearchParams {
	
	private boolean forProgressMeter;
	
	private DatabaseMergeFromJSONController relatedController;
	
	public DataBaseMergeFromJSONSearchParams(EtlItemConfiguration config, RecordLimits limits, DatabaseMergeFromJSONController relatedController) {
		super(config, limits);
		
		setOrderByFields("id");
	}
	
	public DatabaseMergeFromJSONController getRelatedController() {
		return relatedController;
	}
	
	public DataBaseMergeFromJSONSearchParams(EtlItemConfiguration config, RecordLimits limits, String appOriginLocationCode) {
		super(config, limits, appOriginLocationCode);
		setOrderByFields("id");
	}
	
	@Override
	public SearchClauses<SyncImportInfoVO> generateSearchClauses(Connection conn) throws DBException {
		SearchClauses<SyncImportInfoVO> searchClauses = new SearchClauses<SyncImportInfoVO>(this);
		
		AbstractTableConfiguration tableInfo = this.getSrcTableConf();
		
		searchClauses.addColumnToSelect(tableInfo.generateFullStageTableName() + ".*");
		
		searchClauses.addToClauseFrom(tableInfo.generateFullStageTableName());
		
		if (!forProgressMeter) {
			searchClauses.addToClauses("last_sync_date is null or last_sync_date < ?");
			searchClauses.addToParameters(this.getSyncStartDate());
			
			if (this.getLimits() != null) {
				searchClauses.addToClauses("id between ? and ?");
				searchClauses.addToParameters(this.getLimits().getCurrentFirstRecordId());
				searchClauses.addToParameters(this.getLimits().getCurrentLastRecordId());
			}
		} else {
			searchClauses.addToClauses("migration_status in (?, ?)");
			
			searchClauses.addToParameters(SyncImportInfoVO.MIGRATION_STATUS_FAILED);
			searchClauses.addToParameters(SyncImportInfoVO.MIGRATION_STATUS_PENDING);
		}
		
		searchClauses.addToClauses("record_origin_location_code = ?");
		searchClauses.addToParameters(this.getAppOriginLocationCode());
		
		if (utilities.stringHasValue(getExtraCondition())) {
			searchClauses.addToClauses(getExtraCondition());
		}
		
		return searchClauses;
	}
	
	@Override
	public Class<SyncImportInfoVO> getRecordClass() {
		return SyncImportInfoVO.class;
	}
	
	@Override
	public int countAllRecords(Connection conn) throws DBException {
		EtlDatabaseObjectSearchParams migratedRecordSearchParams = new EtlDatabaseObjectSearchParams(getConfig(), null, getRelatedController());
		
		int migrated = SearchParamsDAO.countAll(migratedRecordSearchParams, conn);
		int notMigrated = countNotProcessedRecords(conn);
		
		return migrated + notMigrated;
	}
	
	@Override
	public int countNotProcessedRecords(Connection conn) throws DBException {
		return SearchParamsDAO.countAll(this, conn);
	}

	@Override
	protected VOLoaderHelper getLoaderHealper() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AbstractEtlSearchParams<SyncImportInfoVO> cloneMe() {
		// TODO Auto-generated method stub
		return null;
	}
}
