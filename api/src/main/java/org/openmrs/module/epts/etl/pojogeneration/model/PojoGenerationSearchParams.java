package org.openmrs.module.epts.etl.pojogeneration.model;

import java.sql.Connection;

import org.openmrs.module.epts.etl.engine.RecordLimits;
import org.openmrs.module.epts.etl.engine.SyncSearchParams;
import org.openmrs.module.epts.etl.model.SearchClauses;
import org.openmrs.module.epts.etl.model.pojo.generic.DatabaseObject;
import org.openmrs.module.epts.etl.pojogeneration.engine.PojoGenerationEngine;
import org.openmrs.module.epts.etl.utilities.db.conn.DBException;

public class PojoGenerationSearchParams extends SyncSearchParams<DatabaseObject>{
	private PojoGenerationEngine engine;
	
	public PojoGenerationSearchParams(PojoGenerationEngine engine, RecordLimits limits, Connection conn) {
		super(engine.getSyncTableConfiguration(), limits);
		
		this.engine = engine;
	}
	
	@Override
	public SearchClauses<DatabaseObject> generateSearchClauses(Connection conn) throws DBException {
		return null;
	}	
	
	@Override
	public Class<DatabaseObject> getRecordClass() {
		return this.tableInfo.getSyncRecordClass(engine.getDefaultApp());
	}

	@Override
	public int countAllRecords(Connection conn) throws DBException {
		return 1;
	}

	@Override
	public synchronized int countNotProcessedRecords(Connection conn) throws DBException {
		return engine.isPojoGenerated() ? 0 : 1;
	}
}