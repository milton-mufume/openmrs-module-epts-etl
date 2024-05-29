package org.openmrs.module.epts.etl.model;

import java.sql.Connection;
import java.util.List;

import org.openmrs.module.epts.etl.model.pojo.generic.DatabaseObjectSearchParams;
import org.openmrs.module.epts.etl.utilities.db.conn.DBException;

public class DatabaseObjectSearchParamsDAO extends SearchParamsDAO {
	
	public static List<EtlDatabaseObject> search(DatabaseObjectSearchParams searchParams, Connection conn)
	        throws DBException {
		
		SearchClauses<EtlDatabaseObject> searchClauses = searchParams.generateSearchClauses(conn);
		
		if (searchParams.getOrderByFields() != null) {
			searchClauses.addToOrderByFields(searchParams.getOrderByFields());
		}
		
		String sql = searchClauses.generateSQL(conn);
		
		List<EtlDatabaseObject> l = search(searchParams.getLoaderHealper(), searchParams.getRecordClass(), sql,
		    searchClauses.getParameters(), conn);
		
		int i = 0;
		
		while (utilities.arrayHasNoElement(l) && searchParams.getLimits().canGoNext()) {
			searchParams.getLimits().save();
			
			if (i++ == 0) {
				searchParams.getRelatedController()
				        .logInfo("Empty result on fased quering... The application will keep searching next pages");
			} else {
				searchParams.getRelatedController()
				        .logDebug("Empty result on fased quering... The application will keep searching next pages "
				                + searchParams.getLimits());
			}
			searchParams.getLimits().moveNext(searchParams.getLimits().getQtyRecordsPerProcessing());
			
			searchClauses = searchParams.generateSearchClauses(conn);
			
			if (searchParams.getOrderByFields() != null) {
				searchClauses.addToOrderByFields(searchParams.getOrderByFields());
			}
			
			sql = searchClauses.generateSQL(conn);
			
			l = search(searchParams.getLoaderHealper(), searchParams.getRecordClass(), sql, searchClauses.getParameters(),
			    conn);
		}
		
		return l;
	}
}