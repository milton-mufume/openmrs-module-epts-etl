package org.openmrs.module.epts.etl.detectgapes.engine;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.module.epts.etl.detectgapes.controller.DetectGapesController;
import org.openmrs.module.epts.etl.detectgapes.model.DetectGapesSearchParams;
import org.openmrs.module.epts.etl.detectgapes.model.GapeDAO;
import org.openmrs.module.epts.etl.engine.Engine;
import org.openmrs.module.epts.etl.engine.RecordLimits;
import org.openmrs.module.epts.etl.engine.SyncSearchParams;
import org.openmrs.module.epts.etl.model.DatabaseObjectSearchParamsDAO;
import org.openmrs.module.epts.etl.model.EtlDatabaseObject;
import org.openmrs.module.epts.etl.model.base.EtlObject;
import org.openmrs.module.epts.etl.model.pojo.generic.DatabaseObjectSearchParams;
import org.openmrs.module.epts.etl.monitor.EngineMonitor;
import org.openmrs.module.epts.etl.utilities.db.conn.DBException;

/**
 * Detect gapes within records in a specific tables. For Eg. If in a table we have a min record as 1
 * and the max as 10 then there will be gapes if the table only contains 1,2,3,6,7,8,10. The gapes
 * are 4,5,9. The gapes are writen on an csv file
 * 
 * @author jpboane
 */
public class DetectGapesEngine extends Engine {
	
	/*
	 * The previous record
	 */
	private EtlDatabaseObject prevRec;
	
	public DetectGapesEngine(EngineMonitor monitor, RecordLimits limits) {
		super(monitor, limits);
	}
	
	@Override
	public List<EtlObject> searchNextRecords(Connection conn) throws DBException {
		List<EtlObject> records = new ArrayList<EtlObject>();
		
		return utilities.parseList(
		    DatabaseObjectSearchParamsDAO.search((DatabaseObjectSearchParams) this.searchParams, conn), EtlObject.class);
	}
	
	@Override
	protected boolean mustDoFinalCheck() {
		return false;
	}
	
	@Override
	public DetectGapesController getRelatedOperationController() {
		return (DetectGapesController) super.getRelatedOperationController();
	}
	
	@Override
	protected void restart() {
	}
	
	@Override
	public void performeSync(List<EtlObject> etlObjects, Connection conn) throws DBException {
		logDebug("DETECTING GAPES ON " + etlObjects.size() + "' " + getMainSrcTableName());
		
		if (this.prevRec == null) {
			this.prevRec = (EtlDatabaseObject) etlObjects.get(0);
		}
		
		for (EtlObject record : etlObjects) {
			EtlDatabaseObject rec = (EtlDatabaseObject) record;
			
			int diff = rec.getObjectId().getSimpleValueAsInt() - prevRec.getObjectId().getSimpleValueAsInt();
			
			if (diff > 1) {
				logDebug("Found gape of " + diff + " between " + prevRec.getObjectId() + " and " + rec.getObjectId());
				
				for (int i = prevRec.getObjectId().getSimpleValueAsInt() + 1; i < rec.getObjectId()
				        .getSimpleValueAsInt(); i++) {
					GapeDAO.insert(getMainSrcTableConf(), i, conn);
				}
			}
			
			prevRec = rec;
		}
	}
	
	@Override
	protected SyncSearchParams<? extends EtlObject> initSearchParams(RecordLimits limits, Connection conn) {
		SyncSearchParams<? extends EtlObject> searchParams = new DetectGapesSearchParams(this.getEtlConfiguration(), limits,
		        getRelatedOperationController());
		searchParams.setQtdRecordPerSelected(getQtyRecordsPerProcessing());
		searchParams.setSyncStartDate(getEtlConfiguration().getRelatedSyncConfiguration().getStartDate());
		
		return searchParams;
	}
	
}
