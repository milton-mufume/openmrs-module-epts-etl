package org.openmrs.module.eptssync.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import javax.ws.rs.ForbiddenException;

import org.openmrs.module.eptssync.controller.ProcessController;
import org.openmrs.module.eptssync.controller.conf.SyncConfiguration;
import org.openmrs.module.eptssync.controller.conf.SyncOperationConfig;
import org.openmrs.module.eptssync.controller.conf.SyncTableConfiguration;
import org.openmrs.module.eptssync.utilities.db.conn.DBException;
import org.openmrs.module.eptssync.utilities.io.FileUtilities;
import org.openmrs.util.OpenmrsUtil;

public class ConfVM {
	private static ConfVM sourceConfVM;
	private static ConfVM destConfVM;
	
	private static final String INSTALATION_TAB = "1";
	private static final String OPERATIONS_TAB = "2";
	private static String TABLES_TAB = "3";
	
	private SyncConfiguration syncConfiguration;
	private SyncConfiguration otherSyncConfiguration;
	
	private String activeTab;
	
	private SyncOperationConfig selectedOperation;
	
	private SyncTableConfiguration selectedTable;
	
	private File configFile;
	private String statusMessage;
	
	private ConfVM(String installationType) throws IOException, DBException {
		this.syncConfiguration = new SyncConfiguration();
		this.syncConfiguration.setInstallationType(installationType);
		
		reset();
	}
	
	public SyncOperationConfig getSelectedOperation() {
		return selectedOperation;
	}

	public SyncTableConfiguration getSelectedTable() {
		return selectedTable;
	}
	
	public void setSyncConfiguration(SyncConfiguration syncConfiguration) {
		this.syncConfiguration = syncConfiguration;
	}

	public void setSelectedOperation(SyncOperationConfig selectedOperation) {
		this.selectedOperation = selectedOperation;
	}

	public void setSelectedTable(SyncTableConfiguration selectedTable) {
		this.selectedTable = selectedTable;
	}

	public String getStatusMessage() {
		return statusMessage;
	}
	
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	
	protected static String retrieveClassPath() {
		String rootDirectory = Paths.get(".").normalize().toAbsolutePath().toString();
		
		File[] allFiles = new File(rootDirectory + FileUtilities.getPathSeparator() + "temp").listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().contains("openmrs-lib-cache");
			}
		}); 
		
		Arrays.sort(allFiles);
		
		for (int i = allFiles.length - 1; i >= 0; i--) {
			if (allFiles[i].isDirectory()) {
				//File classPath = new File(allFiles[i].getAbsoluteFile() + FileUtilities.getPathSeparator() + "eptssync" + FileUtilities.getPathSeparator() + "lib" + FileUtilities.getPathSeparator() + "eptssync.jar");
				File classPath = new File(allFiles[i].getAbsoluteFile() + FileUtilities.getPathSeparator() + "eptssync" + FileUtilities.getPathSeparator() + "eptssync.jar");
				
				return classPath.getAbsolutePath();
			}
		}
		
		return null;
	}
	
	protected static File retrieveModuleFolder(SyncConfiguration syncConfiguration) {
		String rootDirectory = Paths.get(".").normalize().toAbsolutePath().toString();
		
		File[] allFiles = new File(rootDirectory + FileUtilities.getPathSeparator() + "temp").listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().contains("openmrs-lib-cache");
			}
		}); 
		
		Arrays.sort(allFiles);
		
		for (int i = allFiles.length - 1; i >= 0; i--) {
			if (allFiles[i].isDirectory()) {
				
				String pojoFolderOnModule = "";
				
				pojoFolderOnModule += allFiles[i].getAbsoluteFile() + FileUtilities.getPathSeparator();
				pojoFolderOnModule += "eptssync" + FileUtilities.getPathSeparator();
				/*pojoFolderOnModule += "org" + FileUtilities.getPathSeparator();
				pojoFolderOnModule += "openmrs" + FileUtilities.getPathSeparator();
				pojoFolderOnModule += "module" + FileUtilities.getPathSeparator();
				pojoFolderOnModule += "eptssync" + FileUtilities.getPathSeparator();*/
				
				return new File( pojoFolderOnModule);
			}
		}
		
		return null;
	}
	
	public static ConfVM getInstance(String installationType) throws IOException, DBException {
		ConfVM vm = null;
		
		if (installationType.equals("source")) {
			if (sourceConfVM != null) {
				vm = sourceConfVM;
				
				vm.reset();
			}
			else {
				vm = new ConfVM(installationType);
			}
			
			sourceConfVM = vm;
			
			if (destConfVM != null) {
				vm.otherSyncConfiguration = destConfVM.getSyncConfiguration();
			}
			
		}
		else {
			if (destConfVM != null) {
				vm = destConfVM;
				
				vm.reset();
			}
			else {
				vm = new ConfVM(installationType);
				
			}

			if (sourceConfVM != null) {
				vm.otherSyncConfiguration = sourceConfVM.getSyncConfiguration();
			}
			
			destConfVM = vm;
		}
		
		if (vm.otherSyncConfiguration == null) {
			vm.determineOtherSyncConfiguration();
		}
		
		return vm;
	}
	
	private void determineOtherSyncConfiguration() throws DBException {
		String rootDirectory = OpenmrsUtil.getApplicationDataDirectory();
		
		String otherConfFile = this.syncConfiguration.getInstallationType().equals("source") ? "dest_sync_config.json" : "source_sync_config.json";

		File otherConfigFile = new File(rootDirectory + FileUtilities.getPathSeparator() + "resources" + FileUtilities.getPathSeparator() + otherConfFile);
		
		if (otherConfigFile.exists()) {
			try {
				this.otherSyncConfiguration = ConfVM.getInstance(this.syncConfiguration.getInstallationType().equals("source") ? "destination" : "source").getSyncConfiguration();
			} catch (IOException e) {
				throw new ForbiddenException(e);
			}
		}
	}

	private void reset() throws IOException, DBException {
		this.activeTab = ConfVM.INSTALATION_TAB;
		
		SyncConfiguration reloadedSyncConfiguration = null;
		
		String rootDirectory = OpenmrsUtil.getApplicationDataDirectory();
		
		String configFileName = this.syncConfiguration.getInstallationType().equals("source") ? "source_sync_config.json" : "dest_sync_config.json";

		this.configFile = new File(rootDirectory + FileUtilities.getPathSeparator() + "sync" + FileUtilities.getPathSeparator() + "conf" + FileUtilities.getPathSeparator() + configFileName);

		if (this.configFile.exists()) {
			reloadedSyncConfiguration = SyncConfiguration.loadFromFile(this.configFile);
		} else {
			String json = this.syncConfiguration.getInstallationType().equals("source") ? ConfigData.generateDefaultSourcetConfig() : ConfigData.generateDefaultDestinationConfig();
		
			reloadedSyncConfiguration = SyncConfiguration.loadFromJSON(json);
			
			reloadedSyncConfiguration.setSyncRootDirectory(rootDirectory+ FileUtilities.getPathSeparator() + "sync" + FileUtilities.getPathSeparator() + "data");
			reloadedSyncConfiguration.setRelatedConfFile(this.configFile);
			
			Properties properties = new Properties();
			
			File openMrsRuntimePropertyFile = new File(rootDirectory + FileUtilities.getPathSeparator() + "openmrs-runtime.properties");
			
			properties.load(FileUtilities.createStreamFromFile(openMrsRuntimePropertyFile));
			
			reloadedSyncConfiguration.getConnInfo().setConnectionURI(properties.getProperty("connection.url"));
			reloadedSyncConfiguration.getConnInfo().setDataBaseUserName(properties.getProperty("connection.username"));
			reloadedSyncConfiguration.getConnInfo().setDataBaseUserPassword(properties.getProperty("connection.password"));
		}
		
		reloadedSyncConfiguration.setRelatedController(this.syncConfiguration.getRelatedController() == null ? new ProcessController(reloadedSyncConfiguration) : this.syncConfiguration.getRelatedController());
		reloadedSyncConfiguration.getRelatedController().setConfiguration(reloadedSyncConfiguration);
		reloadedSyncConfiguration.loadAllTables();
		
		reloadedSyncConfiguration.setClassPath(retrieveClassPath());
		reloadedSyncConfiguration.setModuleRootDirectory(retrieveModuleFolder(getSyncConfiguration()));
		
		this.syncConfiguration = reloadedSyncConfiguration;
		
		if (this.syncConfiguration.isSourceInstallationType()) {
			if (this.syncConfiguration.getOriginAppLocationCode() == null) {
				this.syncConfiguration.tryToDetermineOriginAppLocationCode();
			}
		}
		
	}

	public void selectOperation(String operationType) {
		if (!operationType.isEmpty()) {
			this.selectedOperation = syncConfiguration.findOperation(operationType);
		}
		else {
			this.selectedOperation = null;
		}
	}
	
	public void selectTable(String tableName) {
		this.selectedTable = syncConfiguration.findSyncTableConfigurationOnAllTables(tableName);
		
		if (syncConfiguration.find(this.selectedTable) == null || !this.selectedTable.isFullLoaded()) {
			this.selectedTable.fullLoad();
		}
	}
	
	public String getActiveTab() {
		return activeTab;
	}
	
	public void activateTab(String tab) {
		this.activeTab = tab;
	}
	
	public SyncConfiguration getSyncConfiguration() {
		return syncConfiguration;
	}
	
	public boolean isInstallationTabActive() {
		return this.activeTab.equals(ConfVM.INSTALATION_TAB);
	}
	
	public boolean isOperationsTabActive() {
		return this.activeTab.equals(ConfVM.OPERATIONS_TAB);
	}
	
	public boolean isTablesTabActive() {
		return this.activeTab.equals(ConfVM.TABLES_TAB);
	}

	public void save() {
		FileUtilities.removeFile(this.configFile.getAbsolutePath());
		
		this.syncConfiguration.setAutomaticStart(true);
		FileUtilities.write(this.configFile.getAbsolutePath(), this.syncConfiguration.parseToJSON());
		
		//Make others not automcatic start
		
		if (this.otherSyncConfiguration != null && this.otherSyncConfiguration.getRelatedConfFile().exists()) {
			this.otherSyncConfiguration.setAutomaticStart(false);
			
			FileUtilities.removeFile(this.otherSyncConfiguration.getRelatedConfFile().getAbsolutePath());
			
			FileUtilities.write(this.otherSyncConfiguration.getRelatedConfFile().getAbsolutePath(), otherSyncConfiguration.parseToJSON());
		}
	}
}
