
package org.openmrs.module.eptssync.utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.apache.log4j.Logger;
import org.openmrs.module.eptssync.controller.conf.RefInfo;
import org.openmrs.module.eptssync.controller.conf.SyncTableConfiguration;
import org.openmrs.module.eptssync.exceptions.ForbiddenOperationException;
import org.openmrs.module.eptssync.model.pojo.generic.OpenMRSObject;
import org.openmrs.module.eptssync.utilities.db.conn.DBUtilities;
import org.openmrs.module.eptssync.utilities.io.FileUtilities;

public class OpenMRSPOJOGenerator {
	static CommonUtilities utilities = CommonUtilities.getInstance();

	public static Class<OpenMRSObject> generate(SyncTableConfiguration syncTableInfo, Connection conn) throws IOException, SQLException, ClassNotFoundException {
		if (!syncTableInfo.isFullLoaded()) syncTableInfo.fullLoad();

		File sourceFile = new File(syncTableInfo.getPOJOSourceFilesDirectory().getAbsolutePath() + "/org/openmrs/module/eptssync/model/pojo/" + syncTableInfo.getClasspackage() + "/" + syncTableInfo.generateClassName() + ".java");
		
		String fullClassName = syncTableInfo.generateFullClassName();
		
		Class<OpenMRSObject> existingCLass = tryToGetExistingCLass(syncTableInfo.getPOJOCopiledFilesDirectory(), fullClassName);
			
		if (existingCLass != null && !utilities.createInstance(existingCLass).isGeneratedFromSkeletonClass() ) return existingCLass;
	
		String attsDefinition = "";
		String getttersAndSetterDefinition = "";
		String resultSetLoadDefinition = "		";
		
		PreparedStatement st = conn.prepareStatement("SELECT * FROM " + syncTableInfo.getTableName() + " WHERE 1 != 1");

		ResultSet rs = st.executeQuery();
		ResultSetMetaData rsMetaData = rs.getMetaData();

		
		String insertSQLFieldsWithoutObjectId = "";
		String insertSQLQuestionMarksWithoutObjectId = "";
		
		String updateSQLDefinition = "UPDATE " + syncTableInfo.getTableName() + " SET ";
		
		String insertParamsWithoutObjectId = "";
		String updateParamsDefinition = "Object[] params = {";
		
		String insertValuesDefinition = "";
		
		AttDefinedElements attElements;
		
		for (int i = 1; i <= rsMetaData.getColumnCount() - 1; i++) {
			attElements = AttDefinedElements.define(rsMetaData.getColumnName(i), rsMetaData.getColumnTypeName(i), false, syncTableInfo);
			
			attsDefinition = utilities.concatStrings(attsDefinition, attElements.getAttDefinition(), "\n");
			getttersAndSetterDefinition = utilities.concatStrings(getttersAndSetterDefinition, attElements.getSetterDefinition());
			
			getttersAndSetterDefinition += "\n \n";
			getttersAndSetterDefinition = utilities.concatStrings(getttersAndSetterDefinition, attElements.getGetterDefinition());

			getttersAndSetterDefinition += "\n \n";
			
			insertSQLFieldsWithoutObjectId = utilities.concatStrings(insertSQLFieldsWithoutObjectId, attElements.getSqlInsertFirstPartDefinition());
			insertSQLQuestionMarksWithoutObjectId = utilities.concatStrings(insertSQLQuestionMarksWithoutObjectId, attElements.getSqlInsertLastEndPartDefinition());
			
			updateSQLDefinition = utilities.concatStrings(updateSQLDefinition, attElements.getSqlUpdateDefinition());
			
			insertValuesDefinition = utilities.concatStrings(insertValuesDefinition, attElements.getSqlInsertValues());
			
			insertParamsWithoutObjectId = utilities.concatStrings(insertParamsWithoutObjectId, attElements.getSqlInsertParamDefinifion());
			
			updateParamsDefinition = utilities.concatStrings(updateParamsDefinition, attElements.getSqlUpdateParamDefinifion());
			
			resultSetLoadDefinition = utilities.concatStrings(resultSetLoadDefinition, attElements.getResultSetLoadDefinition());
			resultSetLoadDefinition += "\n		";
		}
	
		attElements = AttDefinedElements.define(rsMetaData.getColumnName(rsMetaData.getColumnCount()), rsMetaData.getColumnTypeName(rsMetaData.getColumnCount()), true, syncTableInfo);
		
		attsDefinition = utilities.concatStrings(attsDefinition, attElements.getAttDefinition(), "\n");
		getttersAndSetterDefinition = utilities.concatStrings(getttersAndSetterDefinition, attElements.getSetterDefinition());
			
		getttersAndSetterDefinition += "\n\n";
		
		getttersAndSetterDefinition += "\n \n";
		getttersAndSetterDefinition = utilities.concatStrings(getttersAndSetterDefinition, attElements.getGetterDefinition());
		
		updateSQLDefinition += attElements.getSqlUpdateDefinition() + " WHERE " + syncTableInfo.getPrimaryKey() + " = ?;";
		
		updateParamsDefinition += attElements.getSqlUpdateParamDefinifion();
		
		resultSetLoadDefinition += attElements.getResultSetLoadDefinition();
		resultSetLoadDefinition += "\n";
		
		insertParamsWithoutObjectId += attElements.getSqlInsertParamDefinifion();
	
		insertSQLFieldsWithoutObjectId = utilities.concatStrings(insertSQLFieldsWithoutObjectId, attElements.getSqlInsertFirstPartDefinition());
		insertSQLQuestionMarksWithoutObjectId = utilities.concatStrings(insertSQLQuestionMarksWithoutObjectId, attElements.getSqlInsertLastEndPartDefinition());
		
		if (syncTableInfo.getPrimaryKey() != null) {
			updateParamsDefinition += ", this." + syncTableInfo.getPrimaryKeyAsClassAtt() + "};"; 
		}
		else {
			updateParamsDefinition += ", null};"; 				
		}
		
		String insertSQLDefinitionWithoutObjectId = "INSERT INTO " + syncTableInfo.getTableName() + "(" + insertSQLFieldsWithoutObjectId + ") VALUES( " + insertSQLQuestionMarksWithoutObjectId + ");";
		String insertParamsWithoutObjectIdDefinition = "Object[] params = {" + insertParamsWithoutObjectId + "};";
		
		String insertSQLDefinitionWithObjectId = "INSERT INTO " + syncTableInfo.getTableName() + "(" + syncTableInfo.getPrimaryKey() + ", " + insertSQLFieldsWithoutObjectId + ") VALUES(?, " + insertSQLQuestionMarksWithoutObjectId + ");";
		String insertParamsWithObjectIdDefinition = "Object[] params = {this." + syncTableInfo.getPrimaryKeyAsClassAtt() + ", "  + insertParamsWithoutObjectId + "};";
		
		insertValuesDefinition += attElements.getSqlInsertValues();
		
		//GENERATE INFO FOR UNAVALIABLE COLUMNS
		if (!DBUtilities.isColumnExistOnTable(syncTableInfo.getTableName(), "uuid", conn)) {
			getttersAndSetterDefinition += generateDefaultGetterAndSetterDefinition("uuid", "String");
		}
		
		if (!DBUtilities.isColumnExistOnTable(syncTableInfo.getTableName(), "origin_record_id", conn)) {
			getttersAndSetterDefinition += generateDefaultGetterAndSetterDefinition("originRecordId", "int");
		}
		
		if (!DBUtilities.isColumnExistOnTable(syncTableInfo.getTableName(), "origin_app_location_code", conn)) {
			getttersAndSetterDefinition += generateDefaultGetterAndSetterDefinition("originAppLocationCode", "String");
		}
		
		if (!DBUtilities.isColumnExistOnTable(syncTableInfo.getTableName(), "consistent", conn)) {
			getttersAndSetterDefinition += generateDefaultGetterAndSetterDefinition("consistent", "int");
		}

		String methodFromSuperClass = "";

		String primaryKeyAtt = syncTableInfo.hasPK() ? syncTableInfo.getPrimaryKeyAsClassAtt() : null;
		
		methodFromSuperClass += "	public int getObjectId() { \n ";
		if (syncTableInfo.isNumericColumnType() && syncTableInfo.hasPK()) methodFromSuperClass += "		return this."+ primaryKeyAtt + "; \n";
		else methodFromSuperClass += "		return 0; \n";
		methodFromSuperClass += "	} \n \n";

		methodFromSuperClass += "	public void setObjectId(int selfId){ \n";
		if (syncTableInfo.isNumericColumnType() && syncTableInfo.hasPK()) methodFromSuperClass += "		this." + primaryKeyAtt + " = selfId; \n";
		methodFromSuperClass += "	} \n \n";
	
		methodFromSuperClass += "	public void load(ResultSet rs) throws SQLException{ \n";
		methodFromSuperClass +=   		resultSetLoadDefinition;
		methodFromSuperClass += "	} \n \n";
		
		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public String generateDBPrimaryKeyAtt(){ \n ";
		methodFromSuperClass += "		return \""+ syncTableInfo.getPrimaryKey() + "\"; \n";
		methodFromSuperClass += "	} \n \n";

		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public String getInsertSQLWithoutObjectId(){ \n ";
		methodFromSuperClass += "		return \""+ insertSQLDefinitionWithoutObjectId + "\"; \n";
		methodFromSuperClass += "	} \n \n";

		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public Object[]  getInsertParamsWithoutObjectId(){ \n ";
		methodFromSuperClass += "		" + insertParamsWithoutObjectIdDefinition;
		methodFromSuperClass += "		return params; \n";
		methodFromSuperClass += "	} \n \n";
		
		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public String getInsertSQLWithObjectId(){ \n ";
		methodFromSuperClass += "		return \""+ insertSQLDefinitionWithObjectId + "\"; \n";
		methodFromSuperClass += "	} \n \n";

		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public Object[]  getInsertParamsWithObjectId(){ \n ";
		methodFromSuperClass += "		" + insertParamsWithObjectIdDefinition;
		methodFromSuperClass += "		return params; \n";
		methodFromSuperClass += "	} \n \n";
	
		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public Object[]  getUpdateParams(){ \n ";
		methodFromSuperClass += "		" + updateParamsDefinition;
		methodFromSuperClass += "		return params; \n";
		methodFromSuperClass += "	} \n \n";
	
		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public String getUpdateSQL(){ \n ";
		methodFromSuperClass += "		return \""+ updateSQLDefinition + "\"; \n";
		methodFromSuperClass += "	} \n \n";
		
		methodFromSuperClass += "	@JsonIgnore\n";
		methodFromSuperClass += "	public String generateInsertValues(){ \n ";
		methodFromSuperClass += "		return \"\"+"+ insertValuesDefinition + "; \n";
		methodFromSuperClass += "	} \n \n";
		
		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public boolean isGeneratedFromSkeletonClass() {\n";
		methodFromSuperClass += "		return false;\n";
		methodFromSuperClass += "	}\n\n";
			
		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public boolean hasParents() {\n";
		
		for(RefInfo refInfo : syncTableInfo.getParents()) {		
			if (refInfo.isNumericRefColumn()) {
				methodFromSuperClass += "		if (this." + refInfo.getRefColumnAsClassAttName() + " != 0) return true;\n\n";
			}
			else {
				methodFromSuperClass += "		if (this." + refInfo.getRefColumnAsClassAttName() + " != null) return true;\n\n";
			}
		}
	
		methodFromSuperClass += "		return false;\n";
		
		methodFromSuperClass += "	}\n\n";

		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public int retrieveSharedPKKey(Connection conn) throws ParentNotYetMigratedException, DBException {\n";
			
		RefInfo sharedKeyRefInfo = syncTableInfo.getSharedKeyRefInfo(conn);
		
		if (sharedKeyRefInfo != null) {
			methodFromSuperClass += "		OpenMRSObject parentOnDestination = null;\n \n";
			
			methodFromSuperClass += "		parentOnDestination = retrieveParentInDestination(";
			methodFromSuperClass += sharedKeyRefInfo.getRefTableConfiguration().generateFullClassName() + ".class,";
				
			methodFromSuperClass += " this." +  sharedKeyRefInfo.getRefColumnAsClassAttName() + ", false, conn); \n";
			methodFromSuperClass += "		return parentOnDestination.getObjectId();\n \n";
		}
		else {
			methodFromSuperClass += "		throw new RuntimeException(\"No PKSharedInfo defined!\");";
		}
		
		methodFromSuperClass += "	}\n\n";
		
		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public int getParentValue(String parentAttName) {";
		
		for(RefInfo refInfo : syncTableInfo.getParents()) {
			if (refInfo.isNumericRefColumn()) {
				methodFromSuperClass += "		\n		if (parentAttName.equals(\"" + refInfo.getRefColumnAsClassAttName() + "\")) return this."+refInfo.getRefColumnAsClassAttName() + ";";
			}
			else {
				methodFromSuperClass += "		\n		if (parentAttName.equals(\"" + refInfo.getRefColumnAsClassAttName() + "\")) return 0;";
			}
		}
		
		methodFromSuperClass += "\n\n";
		
		methodFromSuperClass += "		throw new RuntimeException(\"No found parent for: \" + parentAttName);";
		
		methodFromSuperClass += "	}\n\n";
		
	
		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public void changeParentValue(String parentAttName, OpenMRSObject newParent) {";
		
		for(RefInfo refInfo : syncTableInfo.getParents()) {
			if (refInfo.isNumericRefColumn()) {
				methodFromSuperClass += "		\n		if (parentAttName.equals(\"" + refInfo.getRefColumnAsClassAttName() + "\")) {\n			this."+refInfo.getRefColumnAsClassAttName() + " = newParent.getObjectId();\n			return;\n		}";
			}
			else {
				methodFromSuperClass += "		\n		if (parentAttName.equals(\"" + refInfo.getRefColumnAsClassAttName() + "\")) {\n			this."+refInfo.getRefColumnAsClassAttName() + " = \"\" + newParent.getObjectId();\n			return;\n		}";
			}
		}
		
		methodFromSuperClass += "\n\n";
		
		methodFromSuperClass += "		throw new RuntimeException(\"No found parent for: \" + parentAttName);\n";
		
		methodFromSuperClass += "	}\n\n";
	
		
		
		String classDefinition ="";
		
		classDefinition += "package org.openmrs.module.eptssync.model.pojo." +  syncTableInfo.getClasspackage() + "; \n \n";
		
		classDefinition += "import org.openmrs.module.eptssync.model.pojo.generic.*; \n \n";
		classDefinition += "import org.openmrs.module.eptssync.utilities.DateAndTimeUtilities; \n \n";
		classDefinition += "import org.openmrs.module.eptssync.utilities.db.conn.DBException; \n";
		classDefinition += "import org.openmrs.module.eptssync.utilities.AttDefinedElements; \n";
		classDefinition += "import org.openmrs.module.eptssync.exceptions.ParentNotYetMigratedException; \n \n";
		
		classDefinition += "import java.sql.Connection; \n";
		classDefinition += "import java.sql.SQLException; \n";
		classDefinition += "import java.sql.ResultSet; \n \n";
		classDefinition += "import com.fasterxml.jackson.annotation.JsonIgnore; \n \n";
		
		classDefinition += "public class " + syncTableInfo.generateClassName() + " extends AbstractOpenMRSObject implements OpenMRSObject { \n";
		classDefinition += 		attsDefinition + "\n \n";
		classDefinition += "	public " + syncTableInfo.generateClassName() + "() { \n";
		classDefinition += "		this.metadata = " + syncTableInfo.isMetadata() + ";\n";
		classDefinition += "	} \n \n";
		classDefinition +=  	getttersAndSetterDefinition + "\n \n";
		classDefinition +=  	methodFromSuperClass + "\n";
		
		classDefinition += "}";
		
		FileUtilities.tryToCreateDirectoryStructureForFile(sourceFile.getAbsolutePath());
		
		FileWriter writer = new FileWriter(sourceFile);

		writer.write(classDefinition);

		writer.close();
		
		compile(sourceFile, syncTableInfo.getPOJOCopiledFilesDirectory(), new File(syncTableInfo.getRelatedSynconfiguration().getClassPath()));
		
		st.close();
		rs.close();
				
		return tryToGetExistingCLass(syncTableInfo.getPOJOCopiledFilesDirectory(), fullClassName);
	}
	
	public static Class<OpenMRSObject> generateSkeleton(SyncTableConfiguration syncTableInfo, Connection conn) throws IOException, SQLException, ClassNotFoundException {
		if (!syncTableInfo.isFullLoaded()) syncTableInfo.fullLoad();
			
		File sourceFile = new File(syncTableInfo.getPOJOSourceFilesDirectory().getAbsolutePath() + "/org/openmrs/module/eptssync/model/pojo/" + syncTableInfo.getClasspackage() + "/" + syncTableInfo.generateClassName() + ".java");
		
		String fullClassName = "org.openmrs.module.eptssync.model.pojo." +  syncTableInfo.getClasspackage() + "." + FileUtilities.generateFileNameFromRealPathWithoutExtension(sourceFile.getName());
		
		if (sourceFile.exists()) throw new ForbiddenOperationException("The source file exists [" + sourceFile.getAbsoluteFile() + "]");
		
		Class<OpenMRSObject> existingCLass = tryToGetExistingCLass(syncTableInfo.getPOJOCopiledFilesDirectory(), fullClassName);
			
		if (existingCLass != null) return existingCLass;
	
		//String getttersAndSetterDefinition = "";
		String methodFromSuperClass = "";
			
		methodFromSuperClass += generateDefaultGetterAndSetterDefinition("originRecordId", "int");
		methodFromSuperClass += generateDefaultGetterAndSetterDefinition("originAppLocationCode", "String");
		methodFromSuperClass += generateDefaultGetterAndSetterDefinition("consistent", "int");
		methodFromSuperClass += generateDefaultGetterAndSetterDefinition("objectId", "int");
		methodFromSuperClass += generateDefaultGetterAndSetterDefinition("uuid", "String");
		
		methodFromSuperClass += "	public String generateDBPrimaryKeyAtt(){ \n ";
		methodFromSuperClass += "		return null; \n";
		methodFromSuperClass += "	} \n \n";
		
		methodFromSuperClass += "	public Object[]  getInsertParams(){ \n ";
		methodFromSuperClass += "		return null; \n";
		methodFromSuperClass += "	} \n \n";
	
		methodFromSuperClass += "	public Object[]  getUpdateParams(){ \n ";
		methodFromSuperClass += "		return null; \n";
		methodFromSuperClass += "	} \n \n";
		
		methodFromSuperClass += "	public String getInsertSQL(){ \n ";
		methodFromSuperClass += "		return null; \n";
		methodFromSuperClass += "	} \n \n";
	
		methodFromSuperClass += "	public String getUpdateSQL(){ \n ";
		methodFromSuperClass += "		return null; \n";
		methodFromSuperClass += "	} \n \n";
		
		methodFromSuperClass += "	public String generateInsertValues(){ \n ";
		methodFromSuperClass += "		return null; \n";
		methodFromSuperClass += "	} \n \n";
			
		methodFromSuperClass += "	public boolean hasParents() {\n";
		methodFromSuperClass += "		return false;\n";
		methodFromSuperClass += "	}\n\n";

		methodFromSuperClass += "	public int retrieveSharedPKKey(Connection conn) throws ParentNotYetMigratedException, DBException {\n";
		methodFromSuperClass += "		throw new RuntimeException(\"No PKSharedInfo defined!\");\n";
		methodFromSuperClass += "	}\n\n";
		
		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public void loadDestParentInfo(Connection conn) throws ParentNotYetMigratedException, DBException {\n";
		methodFromSuperClass += "	}\n\n";
		
		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public int getParentValue(String parentAttName) {";
		methodFromSuperClass += "		return 0;\n";
		methodFromSuperClass += "	}\n\n";
		
		methodFromSuperClass += "	@Override\n";
		methodFromSuperClass += "	public boolean isGeneratedFromSkeletonClass() {\n";
		methodFromSuperClass += "		return true;\n";
		methodFromSuperClass += "	}\n\n";
		
		String classDefinition ="";
		
		
		classDefinition += "package org.openmrs.module.eptssync.model.pojo." + syncTableInfo.getClasspackage() + "; \n \n";
		
		classDefinition += "import org.openmrs.module.eptssync.model.pojo.generic.*; \n \n";
		classDefinition += "import org.openmrs.module.eptssync.utilities.DateAndTimeUtilities; \n \n";
		classDefinition += "import org.openmrs.module.eptssync.utilities.db.conn.DBException; \n";
		classDefinition += "import org.openmrs.module.eptssync.exceptions.ParentNotYetMigratedException; \n \n";
		
		classDefinition += "import java.sql.Connection; \n";
		classDefinition += "import java.sql.SQLException; \n";
		classDefinition += "import java.sql.ResultSet; \n \n";
				
		classDefinition += "import com.fasterxml.jackson.annotation.JsonIgnore; \n \n";
		
		classDefinition += "public class " + syncTableInfo.generateClassName() + " extends AbstractOpenMRSObject implements OpenMRSObject { \n";
		classDefinition += "	public " + syncTableInfo.generateClassName() + "() { \n";
		classDefinition += "	} \n \n";
		classDefinition +=  	methodFromSuperClass + "\n";
		classDefinition += "}";
		
		
		FileUtilities.tryToCreateDirectoryStructureForFile(sourceFile.getAbsolutePath());

		FileWriter writer = new FileWriter(sourceFile);

		writer.write(classDefinition);

		writer.close();

		compile(sourceFile, syncTableInfo.getPOJOCopiledFilesDirectory(), new File(syncTableInfo.getRelatedSynconfiguration().getClassPath()));
		
		return tryToGetExistingCLass(syncTableInfo.getPOJOCopiledFilesDirectory(), fullClassName);
	}
	
	static Logger logger = Logger.getLogger(OpenMRSPOJOGenerator.class);
	
	private static String generateDefaultGetterAndSetterDefinition(String attName, String attType) {
		String getttersAndSetterDefinition = "";
		
		getttersAndSetterDefinition = utilities.concatStrings(getttersAndSetterDefinition, AttDefinedElements.defineDefaultGetterMethod(attName, attType));
		
		getttersAndSetterDefinition += "\n \n";
		getttersAndSetterDefinition = utilities.concatStrings(getttersAndSetterDefinition, AttDefinedElements.defineDefaultSetterMethod(attName, attType));

		getttersAndSetterDefinition += "\n \n";
		
		return getttersAndSetterDefinition;
	}
	
	@SuppressWarnings("unchecked")
	public static Class<OpenMRSObject> tryToGetExistingCLass(File targetDirectory, String fullClassName) {
		try {
			URLClassLoader loader = URLClassLoader.newInstance(new URL[] {targetDirectory.toURI().toURL()});
	        
	        Class<OpenMRSObject> c = (Class<OpenMRSObject>) loader.loadClass(fullClassName);
	        
	        loader.close();
	        
	        return c;
		} 
		catch (ClassNotFoundException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			
			return null;
		}
	}
	
	
	public static void compile(File sourceFile, File destinationFile, File classPath) throws IOException {
		if (!destinationFile.exists()) FileUtilities.tryToCreateDirectoryStructure(destinationFile.getAbsolutePath());
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(destinationFile));
		
		fileManager.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(destinationFile, classPath));

		// Compile the file
		compiler.getTask(null, fileManager, null, null, null, fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile))).call();
		
		fileManager.close();
	}
	
	public static void addToClasspath(File file) {
		try {
			URL url = file.toURI().toURL();

			URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(classLoader, url);
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception", e);
		}
	}
	
	/*
	public static void compile(File sourceFile, File destinationFile) throws IOException {
		FileUtilities.tryToCreateDirectoryStructure(destinationFile.getAbsolutePath());
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(destinationFile));
		
		File[] jars = FileUtilities.getParent(Main.getProjectRoot()).listFiles(new FilenameFilter() {
																			@Override
																				public boolean accept(File dir, String name) {
																					return name.toLowerCase().endsWith("jar");
																				}
																			});
		
		 List<File> classPathFiles = new ArrayList<File>();
		 classPathFiles.add(destinationFile);
		 
		 for (File classPath : jars) {
			 classPathFiles.add(classPath);
		 }
		 
		fileManager.setLocation(StandardLocation.CLASS_PATH, classPathFiles);
		
		// Compile the file
		compiler.getTask(null, fileManager, null, null, null, fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile))).call();
		
		fileManager.close();
	}
	*/
	
}