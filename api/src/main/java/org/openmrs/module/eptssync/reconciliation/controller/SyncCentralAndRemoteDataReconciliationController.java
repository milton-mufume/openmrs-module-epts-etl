package org.openmrs.module.eptssync.reconciliation.controller;

import org.openmrs.module.eptssync.controller.OperationController;
import org.openmrs.module.eptssync.controller.ProcessController;
import org.openmrs.module.eptssync.controller.conf.AppInfo;
import org.openmrs.module.eptssync.controller.conf.SyncOperationConfig;
import org.openmrs.module.eptssync.controller.conf.SyncTableConfiguration;
import org.openmrs.module.eptssync.engine.Engine;
import org.openmrs.module.eptssync.engine.RecordLimits;
import org.openmrs.module.eptssync.monitor.EngineMonitor;
import org.openmrs.module.eptssync.reconciliation.engine.SyncCentralAndRemoteDataReconciliationEngine;
import org.openmrs.module.eptssync.utilities.db.conn.DBException;
import org.openmrs.module.eptssync.utilities.db.conn.DBUtilities;
import org.openmrs.module.eptssync.utilities.db.conn.OpenConnection;

/**
 * This class is responsible for control record changes process
 * 
 * @author jpboane
 *
 */
public class SyncCentralAndRemoteDataReconciliationController extends OperationController {
	private AppInfo actionPerformeApp;

	
	public SyncCentralAndRemoteDataReconciliationController(ProcessController processController, SyncOperationConfig operationConfig) {
		super(processController, operationConfig);
		
		this.controllerId = processController.getControllerId() + "_" + getOperationType();	
	}
	
	public AppInfo getActionPerformeApp() {
		return actionPerformeApp;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (!existDetectedRecordInfoTable()) {
			generateDetectedRecordInfoTable();
		}
	}
	
	@Override
	public Engine initRelatedEngine(EngineMonitor monitor, RecordLimits limits) {
		return new SyncCentralAndRemoteDataReconciliationEngine(monitor, limits);
	}

	@Override
	public long getMinRecordId(SyncTableConfiguration tableInfo) {
		OpenConnection conn = openConnection();
		
		try {
			
		
		} catch (DBException e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
		finally {
			conn.finalizeConnection();
		}
	}

	@Override
	public long getMaxRecordId(SyncTableConfiguration tableInfo) {
		OpenConnection conn = openConnection();
		
		try {
			
		} catch (DBException e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
		finally {
			conn.finalizeConnection();
		}
	}
	
	@Override
	public boolean mustRestartInTheEnd() {
		return false;
	}

	@Override
	public String getOperationType() {
		return  this.operationConfig.getOperationType();
	}
	
	@Override
	public OpenConnection openConnection() {
		OpenConnection conn = super.openConnection();
	
		if (getOperationConfig().isDoIntegrityCheckInTheEnd()) {
			try {
				DBUtilities.disableForegnKeyChecks(conn);
			} catch (DBException e) {
				e.printStackTrace();
				
				throw new RuntimeException(e);
			}
		}
		
		return conn;
	}
}
