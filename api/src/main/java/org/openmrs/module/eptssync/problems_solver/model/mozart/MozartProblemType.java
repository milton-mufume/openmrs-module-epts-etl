package org.openmrs.module.eptssync.problems_solver.model.mozart;


public enum MozartProblemType {
	OLD_STRUCTURE,
	MISSIN_FIELDS,
	MISSING_TABLES,
	EMPTY_TABLES;
	
	public boolean isOldStructure() {
		return this.equals(OLD_STRUCTURE);
	}
	
	public boolean isMissingFields() {
		return this.equals(MISSIN_FIELDS);
	}
	
}