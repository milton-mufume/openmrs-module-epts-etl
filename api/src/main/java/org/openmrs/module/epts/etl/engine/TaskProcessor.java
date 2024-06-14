package org.openmrs.module.epts.etl.engine;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.module.epts.etl.conf.AppInfo;
import org.openmrs.module.epts.etl.conf.EtlConfiguration;
import org.openmrs.module.epts.etl.conf.EtlItemConfiguration;
import org.openmrs.module.epts.etl.conf.EtlOperationConfig;
import org.openmrs.module.epts.etl.conf.EtlOperationType;
import org.openmrs.module.epts.etl.conf.SrcConf;
import org.openmrs.module.epts.etl.controller.OperationController;
import org.openmrs.module.epts.etl.exceptions.ForbiddenOperationException;
import org.openmrs.module.epts.etl.model.EtlDatabaseObject;
import org.openmrs.module.epts.etl.model.base.EtlObject;
import org.openmrs.module.epts.etl.monitor.Engine;
import org.openmrs.module.epts.etl.utilities.CommonUtilities;
import org.openmrs.module.epts.etl.utilities.concurrent.MonitoredOperation;
import org.openmrs.module.epts.etl.utilities.concurrent.ThreadPoolService;
import org.openmrs.module.epts.etl.utilities.concurrent.TimeController;
import org.openmrs.module.epts.etl.utilities.concurrent.TimeCountDown;
import org.openmrs.module.epts.etl.utilities.db.conn.DBException;
import org.openmrs.module.epts.etl.utilities.db.conn.OpenConnection;

/**
 * Represent a Synchronization TaskProcessor. A Synchronization engine performes the task which will end up
 * producing or consuming the synchronization info.
 * <p>
 * There are several kinds of engines that performes diferents kind of operations. All the avaliable
 * operations are listed in {@link EtlOperationType} enum
 * 
 * @author jpboane
 */
public abstract class TaskProcessor implements Runnable, MonitoredOperation {
	
	public static CommonUtilities utilities = CommonUtilities.getInstance();
	
	protected List<TaskProcessor> children;
	
	protected TaskProcessor parent;
	
	protected Engine monitor;
	
	private AbstractEtlSearchParams<? extends EtlObject> searchParams;
	
	private int operationStatus;
	
	private boolean stopRequested;
	
	private String engineId;
	
	private boolean newJobRequested;
	
	private Exception lastException;
	
	protected MigrationFinalCheckStatus finalCheckStatus;
	
	public TaskProcessor(Engine monitr, ThreadRecordIntervalsManager limits) {
		this.monitor = monitr;
		
		OpenConnection conn;
		try {
			conn = openConnection();
		}
		catch (DBException e) {
			throw new RuntimeException(e);
		}
		
		this.searchParams = initSearchParams(limits, conn);
		
		conn.markAsSuccessifullyTerminated();
		conn.finalizeConnection();
		
		this.operationStatus = MonitoredOperation.STATUS_NOT_INITIALIZED;
		this.finalCheckStatus = MigrationFinalCheckStatus.NOT_INITIALIZED;
	}
	
	public AppInfo getDefaultApp() {
		return getRelatedOperationController().getDefaultApp();
	}
	
	public ThreadRecordIntervalsManager getLimits() {
		return getSearchParams().getLimits();
	}
	
	public int getQtyRecordsPerProcessing() {
		return monitor.getQtyRecordsPerProcessing();
	}
	
	public MigrationFinalCheckStatus getFinalCheckStatus() {
		return finalCheckStatus;
	}
	
	public Engine getMonitor() {
		return monitor;
	}
	
	public boolean mustRestartInTheEnd() {
		return getRelatedOperationController().mustRestartInTheEnd()
		        && !getRelatedOperationController().isParallelModeProcessing();
	}
	
	public String getEngineId() {
		return engineId;
	}
	
	public void setEngineId(String engineId) {
		this.engineId = engineId;
	}
	
	public OperationController getRelatedOperationController() {
		return this.monitor.getController();
	}
	
	public EtlOperationConfig getRelatedEtlOperationConfig() {
		return getRelatedOperationController().getOperationConfig();
	}
	
	public EtlConfiguration getRelatedEtlConfiguration() {
		return getRelatedOperationController().getConfiguration();
	}
	
	public List<TaskProcessor> getChildren() {
		return children;
	}
	
	public void setChildren(List<TaskProcessor> children) {
		this.children = children;
	}
	
	public AbstractEtlSearchParams<? extends EtlObject> getSearchParams() {
		return searchParams;
	}
	
	public EtlItemConfiguration getEtlConfiguration() {
		return monitor.getEtlConfiguration();
	}
	
	public String getMainSrcTableName() {
		return getSrcConf().getTableName();
	}
	
	public SrcConf getSrcConf() {
		return monitor.getSrcMainTableConf();
	}
	
	public EtlProgressMeter getProgressMeter_() {
		return monitor.getProgressMeter();
	}
	
	public OpenConnection openConnection() throws DBException {
		return getRelatedOperationController().openConnection();
	}
	
	public TaskProcessor getParentConf() {
		return parent;
	}
	
	public void setParent(TaskProcessor parent) {
		this.parent = parent;
	}
	
	@Override
	public void run() {
		this.changeStatusToRunning();
		
		if (stopRequested()) {
			changeStatusToStopped();
			
			if (this.hasChild()) {
				for (TaskProcessor taskProcessor : this.getChildren()) {
					taskProcessor.requestStop();
				}
			}
		} else {
			if (getLimits() != null && !getLimits().isLoadedFromFile()) {
				retriveSavedLimits();
			}
			
			doRun();
		}
	}
	
	private void doRun() {
		
		while (isRunning()) {
			if (stopRequested()) {
				logWarn("STOP REQUESTED... TRYING TO STOP NOW");
				
				if (this.hasChild()) {
					
					boolean allStopped = false;
					
					while (!allStopped) {
						
						String runningThreads = "";
						
						for (TaskProcessor child : getChildren()) {
							if (!child.isStopped() && !child.isFinished()) {
								runningThreads = utilities.concatStringsWithSeparator(runningThreads, child.getEngineId(),
								    ";");
							}
						}
						
						if (utilities.stringHasValue(runningThreads)) {
							logWarn("WAITING FOR ALL CHILD ENGINES TO BE STOPPED", 60);
							logDebug("STILL RUNNING THREADS: " + runningThreads);
							
							TimeCountDown.sleep(10);
						} else {
							allStopped = true;
						}
					}
				}
				
				this.changeStatusToStopped();
			} else {
				if (finalCheckStatus.onGoing()) {
					logInfo("PERFORMING FINAL CHECK...");
				}
				
				OpenConnection conn = null;
				
				boolean finished = false;
				
				try {
					conn = openConnection();
					
					if (getLimits() != null && getLimits().canGoNext()) {
						//Move next at first.
						//Note that when the limits is create, it pos the firt record to minRecordId - qtyRecordsPerProcessing  
						//Note that when the processing is done sucessifuly, the current limits are saved
						getLimits().moveNext();
						
						int processedRecords_ = performe(conn);
						
						refreshProgressMeter(processedRecords_, conn);
						
						conn.markAsSuccessifullyTerminated();
						conn.finalizeConnection();
						
						reportProgress();
						
						getLimits().save(this.getMonitor());
					} else {
						if (getRelatedOperationController().mustRestartInTheEnd()) {
							this.requestANewJob();
						} else {
							if (this.isMainEngine() && finalCheckStatus.notInitialized()) {
								//Do the final check before finishing
								
								while (this.hasChild() && !isAllChildFinished()) {
									List<TaskProcessor> runningChild = getRunningChild();
									
									logDebug("WAITING FOR ALL CHILD FINISH JOB TO DO FINAL RECORDS CHECK! RUNNING CHILD ");
									logDebug(runningChild.toString());
									
									TimeCountDown.sleep(15);
								}
								
								if (mustDoFinalCheck()) {
									this.finalCheckStatus = MigrationFinalCheckStatus.ONGOING;
									
									//Start work with whole records range
									this.getLimits().getMaxLimits().reset(getMonitor().getMinRecordId(),
									    getMonitor().getMaxRecordId());
									this.getLimits().reset();
									
									//Change the engine to unique engine to prevent the original main engine processing history
									setEngineId(getMonitor().getEngineId());
									
									this.getLimits().refreshCode();
									
									logInfo("INITIALIZING FINAL CHECK...");
									
									doRun();
								} else {
									finished = true;
									
									this.finalCheckStatus = MigrationFinalCheckStatus.IGNORED;
								}
							} else {
								logDebug("NO MORE '" + this.getSrcConf().getTableName() + "' RECORDS TO "
								        + getRelatedOperationController().getOperationType().name().toLowerCase()
								        + " ON LIMITS [" + getLimits() + "]! FINISHING...");
								
								if (this.finalCheckStatus.onGoing()) {
									this.finalCheckStatus = MigrationFinalCheckStatus.DONE;
								}
								
								if (isMainEngine()) {
									finished = true;
								} else {
									this.markAsFinished();
								}
							}
						}
					}
					
					if (finished)
						markAsFinished();
				}
				catch (Exception e) {
					
					if (conn != null)
						conn.finalizeConnection();
					
					reportError(e);
				}
			}
		}
	}
	
	private List<TaskProcessor> getRunningChild() {
		if (!hasChild())
			throw new ForbiddenOperationException("This TaskProcessor does not have child!!!");
		
		List<TaskProcessor> runningChild = new ArrayList<TaskProcessor>();
		
		for (TaskProcessor child : this.children) {
			if (child.isRunning()) {
				runningChild.add(child);
			}
		}
		
		return runningChild;
	}
	
	protected boolean mustDoFinalCheck() {
		if (getRelatedEtlOperationConfig().skipFinalDataVerification()) {
			return false;
		} else {
			return true;
		}
	}
	
	private void reportError(Exception e) {
		e.printStackTrace();
		
		this.lastException = e;
		
		getRelatedOperationController().requestStopDueError(getMonitor(), e);
	}
	
	public Exception getLastException() {
		return lastException;
	}
	
	public boolean isMainEngine() {
		return this.getParentConf() == null;
	}
	
	private int performe(Connection conn) throws DBException {
		if (getLimits() != null) {
			logDebug("SERCHING NEXT RECORDS FOR LIMITS " + getLimits());
		} else {
			logDebug("SERCHING NEXT RECORDS");
		}
		
		List<? extends EtlObject> records = null;
		
		if (finalCheckStatus.onGoing() || (isMainEngine() && !hasChild())) {
			//When the final check is on going, only one engine is working, so do the search on multithreads
			//When there is only one engine too
			
			records = getSearchParams().searchNextRecordsInMultiThreads(this, conn);
		} else {
			records = getSearchParams().searchNextRecords(this.getMonitor(), conn);
		}
		
		logDebug("SERCH NEXT MIGRATION RECORDS FOR ETL '" + this.getEtlConfiguration().getConfigCode() + "' ON TABLE '"
		        + getSrcConf().getTableName() + "' FINISHED. FOUND: '" + utilities.arraySize(records) + "' RECORDS.");
		
		if (utilities.arrayHasElement(records)) {
			logDebug("INITIALIZING " + getRelatedOperationController().getOperationType().name().toLowerCase() + " OF '"
			        + records.size() + "' RECORDS OF TABLE '" + this.getSrcConf().getTableName() + "'");
			
			beforeSync(records, conn);
			
			performeSync(records, conn);
		}
		
		return utilities.arraySize(records);
	}
	
	private void beforeSync(List<? extends EtlObject> records, Connection conn) {
		for (EtlObject rec : records) {
			if (rec instanceof EtlDatabaseObject) {
				((EtlDatabaseObject) rec).loadObjectIdData(getSrcConf());
			}
		}
	}
	
	public synchronized void refreshProgressMeter(int newlyProcessedRecords, Connection conn) throws DBException {
		this.monitor.refreshProgressMeter(newlyProcessedRecords, conn);
	}
	
	protected boolean hasChild() {
		return utilities.arrayHasElement(this.children);
	}
	
	private boolean hasParent() {
		return this.parent != null;
	}
	
	@Override
	public TimeController getTimer() {
		return monitor.getTimer();
	}
	
	@Override
	public boolean stopRequested() {
		return this.stopRequested;
	}
	
	@Override
	public boolean isNotInitialized() {
		if (utilities.arrayHasElement(this.children)) {
			for (TaskProcessor taskProcessor : this.children) {
				if (taskProcessor.isNotInitialized()) {
					return true;
				}
			}
		}
		
		return this.operationStatus == MonitoredOperation.STATUS_NOT_INITIALIZED;
	}
	
	@Override
	public boolean isRunning() {
		if (utilities.arrayHasElement(this.children)) {
			for (TaskProcessor taskProcessor : this.children) {
				if (taskProcessor.isRunning())
					return true;
			}
		}
		
		return this.operationStatus == MonitoredOperation.STATUS_RUNNING;
	}
	
	@Override
	public boolean isStopped() {
		if (utilities.arrayHasElement(this.children)) {
			for (TaskProcessor taskProcessor : this.children) {
				if (!taskProcessor.isStopped())
					return false;
			}
		}
		
		return this.operationStatus == MonitoredOperation.STATUS_STOPPED;
	}
	
	@Override
	public boolean isFinished() {
		if (isNotInitialized())
			return false;
		
		if (utilities.arrayHasElement(this.children)) {
			for (TaskProcessor taskProcessor : this.children) {
				if (!taskProcessor.isFinished())
					return false;
			}
		}
		
		return this.operationStatus == MonitoredOperation.STATUS_FINISHED;
	}
	
	@Override
	public boolean isPaused() {
		if (utilities.arrayHasElement(this.children)) {
			for (TaskProcessor taskProcessor : this.children) {
				if (!taskProcessor.isPaused())
					return false;
			}
		}
		
		return this.operationStatus == MonitoredOperation.STATUS_PAUSED;
	}
	
	@Override
	public boolean isSleeping() {
		if (utilities.arrayHasElement(this.children)) {
			for (TaskProcessor taskProcessor : this.children) {
				if (!taskProcessor.isSleeping())
					return false;
			}
		}
		
		return this.operationStatus == MonitoredOperation.STATUS_SLEEPING;
	}
	
	@Override
	public void changeStatusToSleeping() {
		this.operationStatus = MonitoredOperation.STATUS_SLEEPING;
	}
	
	@Override
	public void changeStatusToRunning() {
		this.operationStatus = MonitoredOperation.STATUS_RUNNING;
	}
	
	@Override
	public void changeStatusToStopped() {
		this.operationStatus = MonitoredOperation.STATUS_STOPPED;
		
		if (this.isMainEngine()) {
			EtlProgressMeter pm = this.getProgressMeter_();
			
			if (pm != null) {
				pm.changeStatusToStopped();
			}
		}
	}
	
	@Override
	public void changeStatusToFinished() {
		if (this.hasChild()) {
			for (TaskProcessor child : getChildren()) {
				while (!child.isFinished()) {
					logDebug("WAITING FOR ALL CHILD ENGINES TO BE FINISHED");
					TimeCountDown.sleep(10);
				}
			}
			
			this.operationStatus = MonitoredOperation.STATUS_FINISHED;
		} else {
			this.operationStatus = MonitoredOperation.STATUS_FINISHED;
		}
		
		if (isMainEngine()) {
			EtlProgressMeter pm = this.getProgressMeter_();
			
			if (pm != null) {
				pm.changeStatusToFinished();
			}
		}
	}
	
	@Override
	public void changeStatusToPaused() {
		this.operationStatus = MonitoredOperation.STATUS_PAUSED;
		
		throw new RuntimeException("Trying to pause engine " + getEngineId());
	}
	
	public void reportProgress() {
		this.monitor.reportProgress();
	}
	
	public void resetLimits(ThreadRecordIntervalsManager limits) {
		if (limits != null) {
			limits.setEngine(this);
		}
		
		getSearchParams().setLimits(limits);
		
		retriveSavedLimits();
	}
	
	public void requestANewJob() {
		this.newJobRequested = true;
		
		this.monitor.scheduleNewJobForEngine(this);
	}
	
	@Override
	public String toString() {
		return getEngineId() + " Limits [" + getSearchParams().getLimits() + "]";
	}
	
	@Override
	public synchronized void requestStop() {
		if (isNotInitialized()) {
			changeStatusToStopped();
		} else {
			if (this.hasChild()) {
				for (TaskProcessor taskProcessor : this.getChildren()) {
					taskProcessor.requestStop();
				}
			}
			
			this.stopRequested = true;
		}
	}
	
	public synchronized void requestStopDueError() {
		if (this.hasChild()) {
			for (TaskProcessor taskProcessor : this.getChildren()) {
				taskProcessor.requestStop();
			}
		}
		
		this.stopRequested = true;
		
		if (lastException != null) {
			if (this.hasChild()) {
				for (TaskProcessor taskProcessor : this.getChildren()) {
					while (!taskProcessor.isStopped() && !taskProcessor.isFinished()) {
						logError(
						    "AN ERROR OCURRED... WAITING FOR ALL CHILD STOP TO REPORT THE ERROR END STOP THE OPERATION");
						
						TimeCountDown.sleep(5);
					}
				}
			}
			
			changeStatusToStopped();
		}
	}
	
	/**
	 * @return
	 */
	public boolean isNewJobRequested() {
		return newJobRequested;
	}
	
	public void setNewJobRequested(boolean newJobRequested) {
		this.newJobRequested = newJobRequested;
	}
	
	@Override
	public void onStart() {
	}
	
	@Override
	public void onSleep() {
	}
	
	@Override
	public void onStop() {
	}
	
	@Override
	public void onFinish() {
		if (!this.hasParent()) {
			
			if (this.hasChild()) {
				while (!isFinished()) {
					logDebug(
					    "THE ENGINE " + getEngineId() + " IS WAITING FOR ALL CHILDREN FINISH TO TERMINATE THE OPERATION");
					TimeCountDown.sleep(15);
				}
			}
			
			getTimer().stop();
		}
		
		if (hasChild()) {
			for (TaskProcessor child : this.children) {
				ThreadPoolService.getInstance().terminateTread(getRelatedOperationController().getLogger(),
				    child.getEngineId(), this);
			}
		}
		
		ThreadPoolService.getInstance().terminateTread(getRelatedOperationController().getLogger(), getEngineId(), this);
	}
	
	public void markAsFinished() {
		if (!this.hasParent()) {
			if (hasChild()) {
				for (TaskProcessor child : this.children) {
					while (!child.isFinished()) {
						logDebug("WATING FOR ALL CHILDREN BEEN TERMINATED!");
						TimeCountDown.sleep(15);
					}
				}
			}
			
			tmp();
		} else
			changeStatusToFinished();
	}
	
	void tmp() {
		this.changeStatusToFinished();
	}
	
	public boolean isAllChildFinished() {
		if (!hasChild())
			throw new ForbiddenOperationException("This TaskProcessor does not have child!!!");
		
		for (TaskProcessor child : this.children) {
			if (!child.isFinished())
				return false;
		}
		
		return true;
	}
	
	@Override
	public int getWaitTimeToCheckStatus() {
		return 5;
	}
	
	public void logError(String msg) {
		monitor.logErr(msg);
	}
	
	public void logInfo(String msg) {
		monitor.logInfo(msg);
	}
	
	public void logDebug(String msg) {
		monitor.logDebug(msg);
	}
	
	public void logWarn(String msg) {
		monitor.logWarn(msg);
	}
	
	public void logWarn(String msg, long interval) {
		monitor.logWarn(msg, interval);
	}
	
	protected void retriveSavedLimits() {
		if (!getLimits().hasThreadCode())
			getLimits().setThreadCode(this.getEngineId());
		
		logDebug("Retrieving saved limits for " + getLimits());
		
		getLimits().tryToLoadFromFile(new File(getLimits().generateFilePath(this.getMonitor())), this);
		
		if (getLimits().isLoadedFromFile()) {
			logDebug("Saved limits found [" + getLimits() + "]");
		} else {
			logDebug("No saved limits found for [" + getLimits() + "]");
		}
	}
	
	public boolean writeOperationHistory() {
		return getRelatedEtlOperationConfig().writeOperationHistory();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof TaskProcessor)) {
			return false;
		}
		
		TaskProcessor e = (TaskProcessor) obj;
		
		return this.getEngineId().equals(e.getEngineId());
	}
	
	protected abstract void restart();
	
	protected abstract AbstractEtlSearchParams<? extends EtlObject> initSearchParams(ThreadRecordIntervalsManager limits,
	        Connection conn);
	
	public abstract void performeSync(List<? extends EtlObject> records, Connection conn) throws DBException;
}