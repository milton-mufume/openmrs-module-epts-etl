package org.openmrs.module.eptssync.utilities.db.conn;

import java.sql.SQLException;

/**
 * Exception is thrown when any DB error occurs.
 * 
 */
public class DBException extends SQLException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Os atributos abaixo só se aplicam no caso de 
	 */
	private int SQLCodeError;
	private String SQLState;
	private String dataBaseName;
	

	public static final int ORACLE_UNIQUE_CONSTRAINTS_VIOLATED_COD=1;
	public static final int ORACLE_NAME_IS_ALREADY_USED_BY_AN_EXISTING_OBJECT=17081;
	public static final int ORACLE_TABLE_OR_VIEW_DOES_NOT_EXIST=942;
	
	
	
	public DBException(String errorMessage, SQLException e){
		super(errorMessage, e);
		
		if (e instanceof SQLException){
			
			SQLState = e.getSQLState();
            SQLCodeError = e.getErrorCode();
              
            dataBaseName = DBUtilities.determineDataBaseFromException(e);
            
            //printStackTrace();
            
            //System.out.println("SQLCodeError "+SQLCodeError);
            //System.out.println(getMessage()); 
            //System.out.println(getLocalizedMessage());
            //System.err.println(e);
            //System.out.println(dataBaseName);
       }
	}

	public DBException(SQLException e){
		this(e.getMessage(), e);
	}

	public DBException(String string)
	{
		super(string);
	}
	
	
	
	public int getSQLCodeError() {
		return SQLCodeError;
	}

	public void setSQLCodeError(int codeError) {
		SQLCodeError = codeError;
	}

	public String getSQLState() {
		return SQLState;
	}

	public void setSQLState(String state) {
		SQLState = state;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public String getDataBaseName() {
		return dataBaseName;
	}

	public void setDataBaseName(String dataBaseName) {
		this.dataBaseName = dataBaseName;
	}
	
	/**
	 * Determina se o erro foi causado pela tentativa de duplicação de dados num campo chave
	 * ATENCAO: Este método é limitado, apenas retorna resultados se a base de dados for ORACLE
	 * 
	 * @author JPBOANE. 17/12/2012
	 * @return
	 * @throws DBException 
	 */
	public boolean isDuplicatePrimaryKeyException() throws DBException{
		if (this.dataBaseName == null || this.dataBaseName.isEmpty()) throw new DBException("Impossivel determinar o tipo de erro pois o nome da base de dados nao foi definido");
		
		if (this.dataBaseName.equals(DBUtilities.ORACLE_DATABASE)){
			return this.SQLCodeError == ORACLE_UNIQUE_CONSTRAINTS_VIOLATED_COD;
		}
		
		System.err.println("WARNING: Nao foi possivel determinar a base de dados");
		return false;
	}
	
	/**
	 * Determina se o erro foi causado pela tentativa de duplicação de dados num campo chave
	 * ATENCAO: Este método é limitado, apenas retorna resultados se a base de dados for ORACLE
	 * 
	 * @author JPBOANE. 17/12/2012
	 * @return
	 * @throws DBException 
	 */
	public boolean isAlredyExistTableException() throws DBException{
		if (this.dataBaseName == null || this.dataBaseName.isEmpty()) throw new DBException("Impossivel determinar o tipo de erro pois o nome da base de dados nao foi definido");
		
		if (this.dataBaseName.equals(DBUtilities.ORACLE_DATABASE)){
			return this.SQLCodeError == ORACLE_NAME_IS_ALREADY_USED_BY_AN_EXISTING_OBJECT;
		}
		
		System.err.println("WARNING: Nao foi possivel determinar a base de dados");
		return false;
	}
	

	/**
	 * Determina se o erro foi causado pela tentativa de duplicação de dados num campo chave
	 * ATENCAO: Este método é limitado, apenas retorna resultados se a base de dados for ORACLE
	 * 
	 * @author JPBOANE. 17/12/2012
	 * @return
	 * @throws DBException 
	 */
	public boolean isTableOrViewDoesNotExistException() throws DBException{
		if (this.dataBaseName == null || this.dataBaseName.isEmpty()) throw new DBException("Impossivel determinar o tipo de erro pois o nome da base de dados nao foi definido");
		
		if (this.dataBaseName.equals(DBUtilities.ORACLE_DATABASE)){
			return this.SQLCodeError == ORACLE_TABLE_OR_VIEW_DOES_NOT_EXIST;
		}
		
		System.err.println("WARNING: Nao foi possivel determinar a base de dados");
		return false;
	}
}