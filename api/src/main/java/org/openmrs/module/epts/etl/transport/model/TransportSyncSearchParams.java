package org.openmrs.module.epts.etl.transport.model;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.module.epts.etl.conf.EtlItemConfiguration;
import org.openmrs.module.epts.etl.engine.AbstractEtlSearchParams;
import org.openmrs.module.epts.etl.engine.RecordLimits;
import org.openmrs.module.epts.etl.model.SearchClauses;
import org.openmrs.module.epts.etl.model.base.VOLoaderHelper;
import org.openmrs.module.epts.etl.transport.controller.TransportController;
import org.openmrs.module.epts.etl.utilities.db.conn.DBException;

public class TransportSyncSearchParams extends AbstractEtlSearchParams<TransportRecord> implements FilenameFilter {
	
	private String firstFileName;
	
	private String lastFileName;
	
	private String fileNamePathern;
	
	public TransportSyncSearchParams(TransportController controller, EtlItemConfiguration config, RecordLimits limits) {
		super(config, limits, controller);
		
		if (limits != null) {
			this.firstFileName = getSrcTableConf().getTableName() + "_"
			        + utilities.garantirXCaracterOnNumber(limits.getCurrentFirstRecordId(), 10) + "_"
			        + utilities.garantirXCaracterOnNumber(limits.getCurrentFirstRecordId(), 10) + ".json";
			this.lastFileName = getSrcTableConf().getTableName() + "_"
			        + utilities.garantirXCaracterOnNumber(limits.getCurrentLastRecordId(), 10) + "_"
			        + utilities.garantirXCaracterOnNumber(limits.getCurrentLastRecordId(), 10) + ".json";
		}
	}

	
	@Override
	public List<TransportRecord> searchNextRecords(Connection conn) throws DBException {
		try {
			File[] files = getSyncDirectory().listFiles(this);
			
			List<TransportRecord> etlObjects = new ArrayList<>();
			
			if (files != null && files.length > 0) {
				etlObjects.add(new TransportRecord(files[0], getSyncDestinationDirectory(), getSyncBkpDirectory()));
			}
			
			return etlObjects;
		}
		catch (IOException e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
	}
	
	private File getSyncBkpDirectory() throws IOException {
		return getRelatedController().getSyncBkpDirectory(getSrcConf());
	}
	
	private File getSyncDestinationDirectory() throws IOException {
		return getRelatedController().getSyncDestinationDirectory(getSrcConf());
	}
	
	public String getFileNamePathern() {
		return fileNamePathern;
	}
	
	public void setFileNamePathern(String fileNamePathern) {
		this.fileNamePathern = fileNamePathern;
	}
	
	@Override
	public SearchClauses<TransportRecord> generateSearchClauses(Connection conn) throws DBException {
		return null;
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
		
		return isJSON && isNotMinimal && isInInterval && pathernOk;
	}
	
	@Override
	public int countAllRecords(Connection conn) throws DBException {
		return countNotProcessedRecords(conn);
	}
	
	@Override
	public int countNotProcessedRecords(Connection conn) throws DBException {
		File[] files = getSyncDirectory().listFiles(this);
		
		if (files != null)
			return files.length;
		
		return 0;
	}
	
	@Override
	public TransportController getRelatedController() {
		return (TransportController) super.getRelatedController();
	}
	
	private File getSyncDirectory() {
		return getRelatedController().getSyncDirectory(getSrcTableConf());
	}
	
	@Override
	protected VOLoaderHelper getLoaderHealper() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected AbstractEtlSearchParams<TransportRecord> cloneMe() {
		// TODO Auto-generated method stub
		return null;
	}
}
