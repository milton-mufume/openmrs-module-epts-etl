package org.openmrs.module.epts.etl.dbquickexport.model;

import java.sql.Connection;

import org.openmrs.module.epts.etl.conf.EtlItemConfiguration;
import org.openmrs.module.epts.etl.engine.RecordLimits;
import org.openmrs.module.epts.etl.engine.AbstractEtlSearchParams;
import org.openmrs.module.epts.etl.model.EtlDatabaseObject;
import org.openmrs.module.epts.etl.model.SearchClauses;
import org.openmrs.module.epts.etl.model.SearchParamsDAO;
import org.openmrs.module.epts.etl.utilities.DatabaseEntityPOJOGenerator;
import org.openmrs.module.epts.etl.utilities.db.conn.DBException;

public class DBQuickExportSearchParams extends AbstractEtlSearchParams<EtlDatabaseObject> {
	
	private boolean selectAllRecords;
	
	public DBQuickExportSearchParams(EtlItemConfiguration config, RecordLimits limits) {
		super(config, limits);
		
		setOrderByFields(getSrcTableConf().getPrimaryKey().parseFieldNamesToArray());
	}
	
	@Override
	public SearchClauses<EtlDatabaseObject> generateSearchClauses(Connection conn) throws DBException {
		SearchClauses<EtlDatabaseObject> searchClauses = new SearchClauses<EtlDatabaseObject>(this);
		
		searchClauses.addToClauseFrom(getSrcTableConf().generateSelectFromClauseContentOnSpecificSchema(conn));
		searchClauses.addColumnToSelect(getSrcTableConf().generateFullAliasedSelectColumns());
		
		if (!this.selectAllRecords) {
			tryToAddLimits(searchClauses);
			tryToAddExtraConditionForExport(searchClauses);
		}
		
		return searchClauses;
	}
	
	@Override
	public Class<EtlDatabaseObject> getRecordClass() {
		return DatabaseEntityPOJOGenerator
		        .tryToGetExistingCLass("org.openmrs.module.epts.etl.model.pojo.generic.GenericDatabaseObject");
	}
	
	@Override
	public int countAllRecords(Connection conn) throws DBException {
		DBQuickExportSearchParams auxSearchParams = new DBQuickExportSearchParams(this.getConfig(), this.getLimits());
		auxSearchParams.selectAllRecords = true;
		
		return SearchParamsDAO.countAll(auxSearchParams, conn);
	}
	
	@Override
	public synchronized int countNotProcessedRecords(Connection conn) throws DBException {
		RecordLimits bkpLimits = this.getLimits();
		
		this.removeLimits();
		
		int count = SearchParamsDAO.countAll(this, conn);
		
		this.setLimits(bkpLimits);
		
		return count;
	}
}
