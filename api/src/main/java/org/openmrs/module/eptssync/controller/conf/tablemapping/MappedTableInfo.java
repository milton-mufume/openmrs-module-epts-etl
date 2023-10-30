package org.openmrs.module.eptssync.controller.conf.tablemapping;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.module.eptssync.controller.conf.AppInfo;
import org.openmrs.module.eptssync.controller.conf.SyncTableConfiguration;
import org.openmrs.module.eptssync.exceptions.ForbiddenOperationException;
import org.openmrs.module.eptssync.model.Field;
import org.openmrs.module.eptssync.model.pojo.generic.DatabaseObject;
import org.openmrs.module.eptssync.utilities.db.conn.DBException;
import org.openmrs.module.eptssync.utilities.db.conn.DBUtilities;

public class MappedTableInfo extends SyncTableConfiguration {
	
	private List<FieldsMapping> fieldsMapping;
	
	private SyncTableConfiguration relatedTableConfiguration;
	
	private List<MappingSrcData> additionalMappingDataSrc;
	
	public MappedTableInfo() {
	}
	
	public List<MappingSrcData> getAdditionalMappingDataSrc() {
		return additionalMappingDataSrc;
	}
	
	public void setAdditionalMappingDataSrc(List<MappingSrcData> additionalMappingDataSrc) {
		this.additionalMappingDataSrc = additionalMappingDataSrc;
	}
	
	public void setRelatedTableConfiguration(SyncTableConfiguration relatedTableConfiguration) {
		this.relatedTableConfiguration = relatedTableConfiguration;
	}
	
	public SyncTableConfiguration getRelatedTableConfiguration() {
		return relatedTableConfiguration;
	}
	
	public List<FieldsMapping> getFieldsMapping() {
		return fieldsMapping;
	}
	
	public void setFieldsMappings(List<FieldsMapping> fieldsMapping) {
		this.fieldsMapping = fieldsMapping;
	}
	
	private void addMapping(String srcField, String destField) {
		if (this.fieldsMapping == null) {
			this.fieldsMapping = new ArrayList<FieldsMapping>();
		}
		
		FieldsMapping fm = new FieldsMapping(srcField, this.getTableName(), destField);
		
		if (this.fieldsMapping.contains(fm))
			throw new ForbiddenOperationException("The field [" + fm + "] already exists on mapping");
		
		this.fieldsMapping.add(fm);
	}
	
	public static List<MappedTableInfo> generateFromSyncTableConfiguration(SyncTableConfiguration tableConfiguration) {
		if (!tableConfiguration.isFullLoaded())
			throw new ForbiddenOperationException("The tableInfo is not full loaded!");
		
		MappedTableInfo mappedTableInfo = new MappedTableInfo();
		
		mappedTableInfo.clone(tableConfiguration);
		
		mappedTableInfo.generateMappingFields(tableConfiguration);
		
		mappedTableInfo.loadAdditionalFieldsInfo();
		
		mappedTableInfo.setRelatedTableConfiguration(tableConfiguration);
		
		return utilities.parseObjectToList(mappedTableInfo, MappedTableInfo.class);
	}
	
	public void generateMappingFields(SyncTableConfiguration tableConfiguration) {
		for (Field field : tableConfiguration.getFields()) {
			this.addMapping(field.getName(), field.getName());
		}
	}
	
	public String getMappedField(String srcField) {
		List<FieldsMapping> machedFields = new ArrayList<FieldsMapping>();
		
		for (FieldsMapping field : this.fieldsMapping) {
			if (field.getSrcField().equals(srcField)) {
				machedFields.add(field);
				
				if (machedFields.size() > 1) {
					throw new ForbiddenOperationException("Cannot determine the mapping field for '" + srcField
					        + "' since it has multiple matching fields");
				}
			}
		}
		
		if (machedFields.isEmpty()) {
			throw new ForbiddenOperationException("Cannot determine the mapping field for '" + srcField + "'");
		}
		
		return machedFields.get(0).getDestField();
	}
	
	@Override
	public synchronized void fullLoad(Connection conn) {
		try {
			getPrimaryKey(conn);
			loadUniqueKeys(conn);
			
			loadParents(conn);
			loadChildren(conn);
			
			loadConditionalParents(conn);
			
			setFields(DBUtilities.getTableFields(getTableName(), DBUtilities.determineSchemaName(conn), conn));
			
			this.fullLoaded = true;
			
		}
		catch (SQLException e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
	}
	
	public MappingSrcData findAdditionalDataSrc(String tableName) {
		if (!utilities.arrayHasElement(this.additionalMappingDataSrc)) {
			return null;
		}
		
		for (MappingSrcData src : this.additionalMappingDataSrc) {
			if (src.getTableName().equals(tableName)) {
				return src;
			}
		}
		
		throw new ForbiddenOperationException("The table '" + tableName + "'cannot be foud on the mapping src tables");
	}
	
	public DatabaseObject generateMappedObject(DatabaseObject srcObject, AppInfo appInfo, Connection conn)
	        throws DBException, ForbiddenOperationException {
		try {
			
			List<DatabaseObject> srcObjects = new ArrayList<>();
			
			srcObjects.add(srcObject);
			
			if (utilities.arrayHasElement(this.additionalMappingDataSrc)) {
				for (MappingSrcData mappingInfo : this.additionalMappingDataSrc) {
					DatabaseObject relatedSrcObject = mappingInfo.loadRelatedSrcObject(srcObject, appInfo, conn);
					
					if (relatedSrcObject == null) {
						relatedSrcObject = mappingInfo.getSyncRecordClass(appInfo).newInstance();
					}
					
					srcObjects.add(relatedSrcObject);
					
				}
			}
			
			DatabaseObject mappedObject = getSyncRecordClass(appInfo).newInstance();
			
			for (FieldsMapping fieldsMapping : this.getFieldsMapping()) {
				
				Object srcValue = fieldsMapping.retrieveValue(this, srcObjects, appInfo, conn);
				
				mappedObject.setFieldValue(fieldsMapping.getDestFieldAsClassField(), srcValue);
			}
			
			return mappedObject;
		}
		catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public void loadAdditionalFieldsInfo() {
		if (!utilities.arrayHasElement(this.fieldsMapping)) {
			throw new ForbiddenOperationException("The mapping fields was not loaded yet");
		}
		
		for (FieldsMapping field : this.fieldsMapping) {
			if (!utilities.stringHasValue(field.getSrcTable())) {
				field.setSrcTable(this.relatedTableConfiguration.getTableName());
			}
		}
	}
}
