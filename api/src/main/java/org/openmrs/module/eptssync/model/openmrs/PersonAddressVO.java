package org.openmrs.module.eptssync.model.openmrs; 
 
import org.openmrs.module.eptssync.model.GenericSyncRecordDAO; 
 
import org.openmrs.module.eptssync.model.base.BaseVO; 
 
import org.openmrs.module.eptssync.utilities.db.conn.DBException; 
import org.openmrs.module.eptssync.utilities.db.conn.OpenConnection; 
import org.openmrs.module.eptssync.exceptions.ParentNotYetMigratedException; 
 
import java.sql.Connection; 
 
import com.fasterxml.jackson.annotation.JsonIgnore; 
 
public class PersonAddressVO extends AbstractOpenMRSObject implements OpenMRSObject { 
	private int personAddressId;
	private int personId;
	private byte preferred;
	private String address1;
	private String address2;
	private String cityVillage;
	private String stateProvince;
	private String postalCode;
	private String country;
	private String latitude;
	private String longitude;
	private int creator;
	private java.util.Date dateCreated;
	private byte voided;
	private int voidedBy;
	private java.util.Date dateVoided;
	private String voidReason;
	private String countyDistrict;
	private String address3;
	private String address6;
	private String address5;
	private String address4;
	private String uuid;
	private java.util.Date dateChanged;
	private int changedBy;
	private java.util.Date startDate;
	private java.util.Date endDate;
	private java.util.Date lastSyncDate;
	private int originRecordId;
	private String originAppLocationCode;
 
	public PersonAddressVO() { 
	} 
 
	public void setPersonAddressId(int personAddressId){ 
	 	this.personAddressId = personAddressId;
	}
 
	public int getPersonAddressId(){ 
		return this.personAddressId;
	}	public void setPersonId(int personId){ 
	 	this.personId = personId;
	}
 
	public int getPersonId(){ 
		return this.personId;
	}	public void setPreferred(byte preferred){ 
	 	this.preferred = preferred;
	}
 
	public byte getPreferred(){ 
		return this.preferred;
	}	public void setAddress1(String address1){ 
	 	this.address1 = address1;
	}
 
	public String getAddress1(){ 
		return this.address1;
	}	public void setAddress2(String address2){ 
	 	this.address2 = address2;
	}
 
	public String getAddress2(){ 
		return this.address2;
	}	public void setCityVillage(String cityVillage){ 
	 	this.cityVillage = cityVillage;
	}
 
	public String getCityVillage(){ 
		return this.cityVillage;
	}	public void setStateProvince(String stateProvince){ 
	 	this.stateProvince = stateProvince;
	}
 
	public String getStateProvince(){ 
		return this.stateProvince;
	}	public void setPostalCode(String postalCode){ 
	 	this.postalCode = postalCode;
	}
 
	public String getPostalCode(){ 
		return this.postalCode;
	}	public void setCountry(String country){ 
	 	this.country = country;
	}
 
	public String getCountry(){ 
		return this.country;
	}	public void setLatitude(String latitude){ 
	 	this.latitude = latitude;
	}
 
	public String getLatitude(){ 
		return this.latitude;
	}	public void setLongitude(String longitude){ 
	 	this.longitude = longitude;
	}
 
	public String getLongitude(){ 
		return this.longitude;
	}	public void setCreator(int creator){ 
	 	this.creator = creator;
	}
 
	public int getCreator(){ 
		return this.creator;
	}	public void setDateCreated(java.util.Date dateCreated){ 
	 	this.dateCreated = dateCreated;
	}
 
	public java.util.Date getDateCreated(){ 
		return this.dateCreated;
	}	public void setVoided(byte voided){ 
	 	this.voided = voided;
	}
 
	public byte getVoided(){ 
		return this.voided;
	}	public void setVoidedBy(int voidedBy){ 
	 	this.voidedBy = voidedBy;
	}
 
	public int getVoidedBy(){ 
		return this.voidedBy;
	}	public void setDateVoided(java.util.Date dateVoided){ 
	 	this.dateVoided = dateVoided;
	}
 
	public java.util.Date getDateVoided(){ 
		return this.dateVoided;
	}	public void setVoidReason(String voidReason){ 
	 	this.voidReason = voidReason;
	}
 
	public String getVoidReason(){ 
		return this.voidReason;
	}	public void setCountyDistrict(String countyDistrict){ 
	 	this.countyDistrict = countyDistrict;
	}
 
	public String getCountyDistrict(){ 
		return this.countyDistrict;
	}	public void setAddress3(String address3){ 
	 	this.address3 = address3;
	}
 
	public String getAddress3(){ 
		return this.address3;
	}	public void setAddress6(String address6){ 
	 	this.address6 = address6;
	}
 
	public String getAddress6(){ 
		return this.address6;
	}	public void setAddress5(String address5){ 
	 	this.address5 = address5;
	}
 
	public String getAddress5(){ 
		return this.address5;
	}	public void setAddress4(String address4){ 
	 	this.address4 = address4;
	}
 
	public String getAddress4(){ 
		return this.address4;
	}	public void setUuid(String uuid){ 
	 	this.uuid = uuid;
	}
 
	public String getUuid(){ 
		return this.uuid;
	}	public void setDateChanged(java.util.Date dateChanged){ 
	 	this.dateChanged = dateChanged;
	}
 
	public java.util.Date getDateChanged(){ 
		return this.dateChanged;
	}	public void setChangedBy(int changedBy){ 
	 	this.changedBy = changedBy;
	}
 
	public int getChangedBy(){ 
		return this.changedBy;
	}	public void setStartDate(java.util.Date startDate){ 
	 	this.startDate = startDate;
	}
 
	public java.util.Date getStartDate(){ 
		return this.startDate;
	}	public void setEndDate(java.util.Date endDate){ 
	 	this.endDate = endDate;
	}
 
	public java.util.Date getEndDate(){ 
		return this.endDate;
	}	public void setLastSyncDate(java.util.Date lastSyncDate){ 
	 	this.lastSyncDate = lastSyncDate;
	}
 
	public java.util.Date getLastSyncDate(){ 
		return this.lastSyncDate;
	}	public void setOriginRecordId(int originRecordId){ 
	 	this.originRecordId = originRecordId;
	}
 
	public int getOriginRecordId(){ 
		return this.originRecordId;
	}	public void setOriginAppLocationCode(String originAppLocationCode){ 
	 	this.originAppLocationCode = originAppLocationCode;
	}


 
	public String getOriginAppLocationCode(){ 
		return this.originAppLocationCode;
	}
 
	public int getObjectId() { 
 		return this.personAddressId; 
	} 
 
	public void setObjectId(int selfId){ 
		this.personAddressId = selfId; 
	} 
 
	public void refreshLastSyncDate(OpenConnection conn){ 
		try{
			GenericSyncRecordDAO.refreshLastSyncDate(this, conn); 
		}catch(DBException e) {
			throw new RuntimeException(e);
		}
	}

	@JsonIgnore
	public String generateDBPrimaryKeyAtt(){ 
 		return "person_address_id"; 
	} 
 
	@JsonIgnore
	public Object[]  getInsertParams(){ 
 		Object[] params = {this.personId == 0 ? null : this.personId, this.preferred, this.address1, this.address2, this.cityVillage, this.stateProvince, this.postalCode, this.country, this.latitude, this.longitude, this.creator == 0 ? null : this.creator, this.dateCreated, this.voided, this.voidedBy == 0 ? null : this.voidedBy, this.dateVoided, this.voidReason, this.countyDistrict, this.address3, this.address6, this.address5, this.address4, this.uuid, this.dateChanged, this.changedBy == 0 ? null : this.changedBy, this.startDate, this.endDate, this.lastSyncDate, this.originRecordId, this.originAppLocationCode};		return params; 
	} 
 
	@JsonIgnore
	public Object[]  getUpdateParams(){ 
 		Object[] params = {this.personId == 0 ? null : this.personId, this.preferred, this.address1, this.address2, this.cityVillage, this.stateProvince, this.postalCode, this.country, this.latitude, this.longitude, this.creator == 0 ? null : this.creator, this.dateCreated, this.voided, this.voidedBy == 0 ? null : this.voidedBy, this.dateVoided, this.voidReason, this.countyDistrict, this.address3, this.address6, this.address5, this.address4, this.uuid, this.dateChanged, this.changedBy == 0 ? null : this.changedBy, this.startDate, this.endDate, this.lastSyncDate, this.originRecordId, this.originAppLocationCode, this.personAddressId};		return params; 
	} 
 
	@JsonIgnore
	public String getInsertSQL(){ 
 		return "INSERT INTO person_address(person_id, preferred, address1, address2, city_village, state_province, postal_code, country, latitude, longitude, creator, date_created, voided, voided_by, date_voided, void_reason, county_district, address3, address6, address5, address4, uuid, date_changed, changed_by, start_date, end_date, last_sync_date, origin_record_id, origin_app_location_code) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"; 
	} 
 
	@JsonIgnore
	public String getUpdateSQL(){ 
 		return "UPDATE person_address SET person_id = ?, preferred = ?, address1 = ?, address2 = ?, city_village = ?, state_province = ?, postal_code = ?, country = ?, latitude = ?, longitude = ?, creator = ?, date_created = ?, voided = ?, voided_by = ?, date_voided = ?, void_reason = ?, county_district = ?, address3 = ?, address6 = ?, address5 = ?, address4 = ?, uuid = ?, date_changed = ?, changed_by = ?, start_date = ?, end_date = ?, last_sync_date = ?, origin_record_id = ?, origin_app_location_code = ? WHERE person_address_id = ?;"; 
	} 
 
	@JsonIgnore
	public int getMainParentId(){ 
 		return personId; 
	} 
 
	public void setMainParentId(int mainParentId){ 
 		this.personId = mainParentId; 
	} 
 
	@JsonIgnore
	public String getMainParentTable(){ 
 		return "person";
	} 
 
	public void loadDestParentInfo(Connection conn) throws ParentNotYetMigratedException, DBException {
		OpenMRSObject parentOnDestination = null;
 
		parentOnDestination = loadParent(org.openmrs.module.eptssync.model.openmrs.PersonVO.class, this.personId,true, conn); 
		if (parentOnDestination  != null) this.personId = parentOnDestination.getObjectId();
 
		parentOnDestination = loadParent(org.openmrs.module.eptssync.model.openmrs.UsersVO.class, this.creator,false, conn); 
		if (parentOnDestination  != null) this.creator = parentOnDestination.getObjectId();
 
		parentOnDestination = loadParent(org.openmrs.module.eptssync.model.openmrs.UsersVO.class, this.changedBy,true, conn); 
		if (parentOnDestination  != null) this.changedBy = parentOnDestination.getObjectId();
 
		parentOnDestination = loadParent(org.openmrs.module.eptssync.model.openmrs.UsersVO.class, this.voidedBy,true, conn); 
		if (parentOnDestination  != null) this.voidedBy = parentOnDestination.getObjectId();
 
	}
}