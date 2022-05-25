package appdynamics.snapshotrestore.impl;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;
import appdynamics.zookeeper.monitor.configuration.HaesHAProperties;
import appdynamics.zookeeper.monitor.configuration.LocalProperties;
import appdynamics.zookeeper.monitor.configuration.SRScriptsProperties;
import appdynamics.zookeeper.monitor.util.ZkHaesUtil;
import lombok.extern.slf4j.Slf4j;


import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER1;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER2;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_SNAPSHOT;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_RESTORE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_NODE;

import appdynamics.zookeeper.monitor.util.EmailService;
import appdynamics.zookeeper.monitor.util.FileUtils;
import appdynamics.zookeeper.monitor.util.ShellUtils;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.FAILBACK_FAILED_STATUS;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.FAILBACK_READY_TO_START_STATUS;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.FAILOVER_FAILED_STATUS;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.FAILOVER_OR_FAILBACK_RUNNING_STATUS;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.FAILOVER_READY_TO_START_STATUS;
/** @author "Marc Pichon" 08/10/20 */

@Slf4j
//@Service

/*
 * currently all methods here are using @Async so that they execute on a specific thread.
 */
public class ESSnapshotRestoreServiceImpl implements ESSnapshotRestoreService {

	private ZkClient zkClient;

	@Autowired 
	private ESZkService eszkService;
	@Autowired 
	private ZkService zkService;

	@Autowired
	private SRScriptsProperties srScriptsProperties;
	@Autowired
	private LocalProperties localProperties;
	@Autowired 
	private EmailService emailService;

	@Autowired
	private  HaesHAProperties haesHAProperties;


	/*
	 * allows for zk requests
	 */
	public ESSnapshotRestoreServiceImpl(String hostPort) {
		//zkClient = new ZkClient(hostPort, 60000, 30000, new StringSerializer());
		// using same connection everywhere
		zkClient = ZkHaesUtil.getZkClientInstance(hostPort);

		log.info("ESSnapshotRestoreServiceImpl/ZkClient connection created.");
	}

	public void closeConnection() {
		zkClient.close();
		log.info("ESSnapshotRestoreServiceImpl/ZkClient connection closed.");
	}

	@Override
	/*
	 * doFailOver is a command not scheduled from springbbot scheduler
	 * failover currently depends on conditions in 
	 * it is noit triggered using ESClusterAvailabilityChangeListener.
	 * 
	 * doing it async allows ESClusterAvailabilityChangeListener to not wait on completion of doFailOver
	 * but inside doFailOver, all sequence of actions must be sync 
	 * @see appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService#doFailOver()
	 */
	@Async 
	public CompletableFuture<Boolean> doFailOver() {
            if (eszkService.getFailoverStatus().equals(FAILOVER_READY_TO_START_STATUS)) {
		log.info("doFailOver: Begin.");
		log.info("doFailOver: Check if FailOver has to wait on syncing datas: " + haesHAProperties.getFailover().getsynced());
		/*
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */

		/*
		 * first, check this the elected node 
		 * if not, then let the task to the leader
		 */

		// defensive code here due to initialization phase not terminated

		if (eszkService.getLocalESNodeID() != null) {
			if (eszkService.getLocalESClusterID() != null) {
				if (zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()) != null) {

				} else {
					log.warn("doFailOver: Leader node  not yet defined. Exiting.");
					return CompletableFuture.completedFuture(false);

				}

			} else {
				log.warn("doFailOver: LocalESClusterID not yet defined. Exiting.");
				return CompletableFuture.completedFuture(false);
			}

		} else {
			log.warn("doFailOver: LocalESNodeID not yet defined. Exiting.");
			return CompletableFuture.completedFuture(false);
		}


		if (!zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID())) {
			log.info("doFailOver: This node is not the master elected node. Exiting.");
			return CompletableFuture.completedFuture(false);
		}
		
		/*
		 * check if failover has been frozen.
		 * if yes, exit
		 */
		if (eszkService.getFailoverFreezeStatus().contentEquals("true")) {
			log.info("doFailOver: Failover has been frozen. Exiting.");
			return CompletableFuture.completedFuture(false);
	
		}
		
		
		/*
		 * sending email
		 */
		String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Executing a Failover request.";
                eszkService.setFailoverStatus(FAILOVER_OR_FAILBACK_RUNNING_STATUS);

		if (emailService.sendMail(message)) {
			log.debug("doFailOver/sendMail succeeded");
		} else {
			log.error("doFailOver/sendMail failed");
		}

		/*
		 * check secondary cluster is available
		 */

		if (eszkService.getClusterAvailability(CLUSTER2).contentEquals("ko")) {
			log.info("doFailOver: The CLUSTER2 is not available. Exiting.");
			message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Exiting a Failover request. reason: The CLUSTER2 is not available.";

			if (emailService.sendMail(message)) {
				log.debug("doFailOver/sendMail succeeded");
			} else {
				log.error("doFailOver/sendMail failed");
			}
                        eszkService.setFailoverStatus(FAILOVER_FAILED_STATUS);

			return CompletableFuture.completedFuture(false);

		}

		boolean rc = true;
		log.info("doFailOver: Initiating ES cluster1 fail over to ES cluster2.");
		/*
		 * change type of ES clusters
		 * cluster1 active --> passive
		 * cluster2 stays passive 
		 */
		rc = eszkService.setClusterType(CLUSTER1, "passive");
		if (!rc) {
			log.error("doFailOver: Error trying to update CLUSTER1 to passive mode.");
                        eszkService.setFailoverStatus(FAILOVER_FAILED_STATUS);
			return CompletableFuture.completedFuture(false);
		}

		/*
		 * check cluster 2 is still passive (defending algorythm)
		 */
		String cluster2_type = eszkService.getESclusterType(CLUSTER2);
		if (cluster2_type.contentEquals("active")) {
			log.info("doFailOver: CLUSTER2 is active. forcing its type to passive");
			rc = eszkService.setClusterType(CLUSTER2, "passive");
			if (!rc) {
				log.error("doFailOver: Error trying to update CLUSTER2 to passive mode.");
                                eszkService.setFailoverStatus(FAILOVER_FAILED_STATUS);
				return CompletableFuture.completedFuture(false);			
			}
		}

		/*
		 * call snapshot/restore services
		 * sync last restore
		 */


		/*
		 * checking if restore is happening on passive cluster
		 * this is synchromous here: we need restore to finish first.
		 */
		log.info("doFailOver: Checking if one restore is executing on passive cluster.");

		String restoreNode = HAES_RESTORE.concat("/").concat(ES_NODE).concat("/").concat("ID");

		boolean one_restore_executing = false;
		boolean one_restore_executing_during_failover_request = false;

		if (zkClient.exists(restoreNode)) {
			one_restore_executing = true;
			one_restore_executing_during_failover_request = true;
		} else {
			one_restore_executing = false;
		}


		while (one_restore_executing) {
			if (zkClient.exists(restoreNode)) {
				one_restore_executing = true;
			} else {
				one_restore_executing = false;
				break;
			}
			/*
			 * waiting 1 hour for current restore to finish at most
			 */
			int countwaitperiod = 60; // a safeguard in case of problematic restore which never ends
			log.info("doFailOver: There is one restore still executing on passive cluster on node:" + zkClient.readData(restoreNode));
			long time = zkClient.getCreationTime(restoreNode);
			Instant instant = Instant.ofEpochMilli(time);
			LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			log.info("doFailOver: This began execution at:"  + date.format(formatter));
			log.info("doFailOver: waiting until it has finished.");

			countwaitperiod--;
			if (countwaitperiod == 0) {
				log.info("doFailOver: waiting periods has finished.");			
				/*
				 * sending email
				 */
				message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Restore Shell Execution during Failover longer than 1 hour. abandonning FailOver. node information: " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID();

				if (emailService.sendMail(message)) {
					log.debug("doFailOver/sendMail succeeded");
				} else {
					log.error("doFailOver/sendMail failed");
				}
                                eszkService.setFailoverStatus(FAILOVER_FAILED_STATUS);
				return CompletableFuture.completedFuture(false);
			}

			try {
				Thread.sleep(60000); // 1 min
			} catch (InterruptedException e) {
				log.error("doFailOver has been interrupted for reason: " + e.getLocalizedMessage());
				return CompletableFuture.completedFuture(false);

			} 

		}
		if (!one_restore_executing) {
			log.info("doFailOver: There is no more restore executing on passive cluster.");
		} else {
			log.info("doFailOver: There is still one restore executing on passive cluster.");
			log.error("doFailOver: Error in the sequence of actions. Failover is abandonned.");
			log.error("doFailOver: Try Failover manually.");
                        eszkService.setFailoverStatus(FAILOVER_FAILED_STATUS);
			return CompletableFuture.completedFuture(false);
		}

		/*
		 * if no restore was executing, then we need to ask for one:
		 * failover action in script will trigger a restore
		 * 
		 * also, if restore has been frozen we need to skip the restore
		 */

		/*
		 * check if snapshot execution has been frozen
		 */

		if ((!one_restore_executing) && (!one_restore_executing_during_failover_request) && eszkService.getRestoreFreezeStatus().contentEquals("false")) {

			/*
			 * check if haesfail.failover.synced=yes
			 */
			if (haesHAProperties.getFailover().getsynced().contentEquals("no")) {

				log.info("doFailOver: Executing sync action with snapshot/restore script: ".concat(srScriptsProperties.getdaemonscript()));

				String directory = srScriptsProperties.getrootlocation();
				String command = "./".concat(srScriptsProperties.getdaemonscript());

				String[] args = new String[]{"-U", srScriptsProperties.getuser(), "-a", "failover", "-l", srScriptsProperties.geteventservicelocation(), "-r", srScriptsProperties.getsnapshotrepositorylocation()};
				Map<String, String> environment = new HashMap<>();
				environment.put("context", "PROD");
				environment.put("type", "real");

				/*
				 * this is a synchronous shell call
				 */
				if (!ShellUtils.runShell(directory, command, args, environment)) {
					log.error("doFailOver: Error trying to execute shell script failover.");
					log.error("doFailOver: the effective restore of last snapshot did no happen.");
					log.error("doFailOver: continuing failover sequence.");
					/*
					 * sending email
					 */
					message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Failover Shell execution failed. the effective restore of last snapshot did no happen. node information: " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID();

					if (emailService.sendMail(message)) {
						log.debug("doFailOver/sendMail succeeded");
					} else {
						log.error("doFailOver/sendMail failed");
					}
				}
			}
		}

		/*
		 * change type of ES clusters
		 * cluster2 passive --> active
		 * cluster1 stays passive 
		 */
		rc = eszkService.setClusterType(CLUSTER2, "active");
		if (!rc) {
			log.error("doFailOver: Error trying to update CLUSTER2 to active mode.");
                        eszkService.setFailoverStatus(FAILOVER_FAILED_STATUS);
			return CompletableFuture.completedFuture(false);
		}
		/*
		 * check cluster 1 is still passive (defending algorythm)
		 */
		String cluster1_type = eszkService.getESclusterType(CLUSTER1);
		if (cluster1_type.contentEquals("active")) {
			log.info("doFailOver: CLUSTER1 is active. forcing its type to passive");
			rc = eszkService.setClusterType(CLUSTER1, "passive");
			if (!rc) {
				log.error("doFailOver: Error trying to update CLUSTER1 to passive mode.");
                                eszkService.setFailoverStatus(FAILOVER_FAILED_STATUS);
				return CompletableFuture.completedFuture(false);			
			}
		}

		/*
		 * sending email
		 */
		message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Failover execution has finished.";

		if (emailService.sendMail(message)) {
			log.debug("doFailOver/sendMail succeeded");
		} else {
			log.error("doFailOver/sendMail failed");
		}
		log.info("doFailOver: End.");
		// default
                eszkService.setFailoverStatus(FAILBACK_READY_TO_START_STATUS);
		return CompletableFuture.completedFuture(rc);
            }  else {
                log.info("doFailOver: failover is running or has already been performed...");
                return CompletableFuture.completedFuture(false);
            }
	}

	@Override
	/*
	 * doFailBack is a command not scheduled from springbbot scheduler
	 * doing it async allows rest url request ZookeeperControllerASync.doFailBack to not wait on completion of doFailBack
	 * but inside doFailOver, all sequence of actions must be sync 
	 * @see appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService#doFailBack()
	 */

	@Async 
	public CompletableFuture<Boolean> doFailBack() {
            if (eszkService.getFailoverStatus().equals(FAILBACK_READY_TO_START_STATUS)) {
		log.info("doFailBack: Begin.");
		log.info("doFailOver: Check if FailBack has to wait on syncing datas: " + haesHAProperties.getFailback().getsynced());

		/*
		 * first, check this the elected node 
		 * if not, then let the task to the leader
		 */
		// defensive code here due to initialization phase not terminated

		if (eszkService.getLocalESNodeID() != null) {
			if (eszkService.getLocalESClusterID() != null) {
				if (zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()) != null) {

				} else {
					log.warn("doFailBack: Leader node  not yet defined. Exciting.");
					return CompletableFuture.completedFuture(false);
				}

			} else {
				log.warn("doFailBack: LocalESClusterID not yet defined. Exciting.");
				return CompletableFuture.completedFuture(false);
			}

		} else {
			log.warn("doFailBack: LocalESNodeID not yet defined. Exciting.");
			return CompletableFuture.completedFuture(false);
		}

		if (!zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID())) {
			log.info("doFailBack: This node is not the master elected node. Exciting.");
			return CompletableFuture.completedFuture(false);
		}
		/*
		 * sending email
		 */
		String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Executing a Failback request.";
                eszkService.setFailoverStatus(FAILOVER_OR_FAILBACK_RUNNING_STATUS);
                
		if (emailService.sendMail(message)) {
			log.debug("doFailBack/sendMail succeeded");
		} else {
			log.error("doFailBack/sendMail failed");
		}

		/*
		 * check primary cluster is available
		 */

		if (eszkService.getClusterAvailability(CLUSTER1).contentEquals("ko")) {
			log.info("doFailOver: The CLUSTER1 is not available. Exciting.");
			message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Exiting a Failback request. reason: The CLUSTER1 is not available.";

			if (emailService.sendMail(message)) {
				log.debug("doFailOver/sendMail succeeded");
			} else {
				log.error("doFailOver/sendMail failed");
			}
                        eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
			return CompletableFuture.completedFuture(false);

		}


		boolean rc = true;
		log.info("doFailBack: Initiating ES cluster1 back from to ES cluster2.");
		/*
		 * change type of ES clusters
		 * cluster2 active --> passive
		 * cluster1 stays passive 
		 */
		rc = eszkService.setClusterType(CLUSTER2, "passive");
		if (!rc) {
			log.error("doFailBack: Error trying to update CLUSTER2 to passive mode.");
                        eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
			return CompletableFuture.completedFuture(false);
		}

		/*
		 * check cluster 1 is still passive (defending algorythm)
		 */
		String cluster1_type = eszkService.getESclusterType(CLUSTER1);
		if (cluster1_type.contentEquals("active")) {
			log.info("doFailBack: CLUSTER1 is active. forcing its type to passive");
			rc = eszkService.setClusterType(CLUSTER1, "passive");
			if (!rc) {
				log.error("doFailBack: Error trying to update CLUSTER1 to passive mode.");
                                eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
				return CompletableFuture.completedFuture(false);			
			}
		}

		/*
		 * call snapshot/restore services
		 * sync last restore
		 */


		/*
		 * checking if restore is happening on passive cluster1
		 * this is synchromous here: we need restore to finish first.
		 */
		log.info("doFailBack: Checking if one restore is executing on passive cluster1.");

		String restoreNode = HAES_RESTORE.concat("/").concat(ES_NODE).concat("/").concat("ID");

		boolean one_restore_executing = false;
		boolean one_restore_executing_during_failback_request = false;

		if (zkClient.exists(restoreNode)) {
			one_restore_executing = true;
			one_restore_executing_during_failback_request = true;
		} else {
			one_restore_executing = false;
		}


		while (one_restore_executing) {
			if (zkClient.exists(restoreNode)) {
				one_restore_executing = true;
			} else {
				one_restore_executing = false;
				break;
			}
			/*
			 * waiting 1 hour for current restore to finish at most
			 */
			int countwaitperiod = 60; // a safeguard in case of problematic restore which never ends
			log.info("doFailBack: There is one restore still executing on passive cluster on node:" + zkClient.readData(restoreNode));
			long time = zkClient.getCreationTime(restoreNode);
			Instant instant = Instant.ofEpochMilli(time);
			LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			log.info("doFailBack: This began execution at:"  + date.format(formatter));
			log.info("doFailBack: waiting until it has finished.");

			countwaitperiod--;
			if (countwaitperiod == 0) {
				log.info("doFailBack: waiting periods has finished.");
                                eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
				return CompletableFuture.completedFuture(false);
			}

			try {
				Thread.sleep(60000); // 1 min
			} catch (InterruptedException e) {
				log.error("doFailBack has been interrupted for reason: " + e.getLocalizedMessage());
                                eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
				return CompletableFuture.completedFuture(false);

			} 

		}
		if (!one_restore_executing) {
			log.info("doFailBack: There is no more restore executing on passive cluster.");
		} else {
			log.info("doFailBack: There is still one restore executing on passive cluster.");
			log.error("doFailBack: Error in the sequence of actions. Failover is abandonned.");
			log.error("doFailBack: Try FailBack manually again.");
                        eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
			return CompletableFuture.completedFuture(false);
		}

		/*
		 * if no restore was executing, then we need to ask for one:
		 * failover action in script will trigger a restore
		 * 
		 * Also restore will need not be executed in case restore has been frozen
		 */
		if ((!one_restore_executing) && (!one_restore_executing_during_failback_request) && eszkService.getRestoreFreezeStatus().contentEquals("false")) {

			/*
			 * check if haesfail.failback.synced=yes
			 */
			if (haesHAProperties.getFailover().getsynced().contentEquals("no")) {

				log.info("doFailBack: Executing sync action with snapshot/restore script: ".concat(srScriptsProperties.getdaemonscript()));

				String directory = srScriptsProperties.getrootlocation();
				String command = "./".concat(srScriptsProperties.getdaemonscript());

				String[] args = new String[]{"-U", srScriptsProperties.getuser(), "-a", "failback", "-l", srScriptsProperties.geteventservicelocation(), "-r", srScriptsProperties.getsnapshotrepositorylocation()};
				Map<String, String> environment = new HashMap<>();
				environment.put("context", "PROD");
				environment.put("type", "real");

				/*
				 * this is a synchronous shell call
				 */
				if (!ShellUtils.runShell(directory, command, args, environment)) {

					log.error("doFailBack: Error trying to execute shell script failback.");
					log.error("doFailBack: the effective restore of last snapshot did no happen.");
					log.error("doFailBack: continuing failback sequence.");

					/*
					 * sending email
					 */
					message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Failback Shell execution failed. the effective restore of last snapshot did no happen. node information: " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID();

					if (emailService.sendMail(message)) {
						log.debug("doFailBack/sendMail succeeded");
					} else {
						log.error("doFailBack/sendMail failed");
					}

				}
			}
		}

		/*
		 * change type of ES clusters
		 * cluster1 passive --> active
		 * cluster2 stays passive 
		 */
		rc = eszkService.setClusterType(CLUSTER1, "active");
		if (!rc) {
			log.error("doFailBack: Error trying to update CLUSTER1 to active mode.");
                        eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
			return CompletableFuture.completedFuture(false);
		}
		/*
		 * check cluster 2 is still passive (defending algorythm)
		 */
		String cluster2_type = eszkService.getESclusterType(CLUSTER2);
		if (cluster2_type.contentEquals("active")) {
			log.info("doFailBack: CLUSTER2 is active. forcing its type to passive");
			rc = eszkService.setClusterType(CLUSTER2, "passive");
			if (!rc) {
				log.error("doFailBack: Error trying to update CLUSTER2 to passive mode.");
                                eszkService.setFailoverStatus(FAILBACK_FAILED_STATUS);
				return CompletableFuture.completedFuture(false);			
			}
		}
		/*
		 * sending email
		 */
		message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Failback execution has finshed.";

		if (emailService.sendMail(message)) {
			log.debug("doFailBack/sendMail succeeded");
		} else {
			log.error("doFailBack/sendMail failed");
		}

		log.info("doFailBack: End.");

		// default
                eszkService.setFailoverStatus(FAILOVER_READY_TO_START_STATUS);
		return CompletableFuture.completedFuture(rc);
            }  else {
                log.info("doFailBack: failback is running or has already been performed...");
                return CompletableFuture.completedFuture(false);
            }
	}
	@Override
	//@Async the springboot scheduler is already async 
	public boolean doSnapShot() {
		log.info("doSnapShot: Begin.");

		String local_loopback_IP = localProperties.getloopback();
		log.debug("doSnapShot/local_loopback_IP: {}", local_loopback_IP);
		boolean rc = true;
		boolean snapshot_executing = false;
		boolean amIelected = false;

		/*
		 * check if snapshot execution has been frozen
		 */
		if (eszkService.getSnapshotFreezeStatus().contentEquals("true"))
		{
			log.info("doSnapShot: Snapshot execution has been frozen. stopping.");
			return false;
		}
		/*
		 * check if local cluster is active
		 */
		if (eszkService.getESclusterType(eszkService.getLocalESClusterID()).contentEquals("passive")) {
			log.info("doSnapShot: this node is on the passive cluster. stopping.");
			return false;

		}

		/*
		 * check if local node is master and elected to execute snapshot)
		 * if clusterinfo does not contain any master yet, we need to return
		 */

		// defensive code here due to initialization phase not terminated

		if (eszkService.getLocalESNodeID() != null) {
			if (eszkService.getLocalESClusterID() != null) {
				if (zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()) != null) {

				} else {
					log.warn("doSnapShot: Leader node  not yet defined. Exiting.");
					return false;
				}

			} else {
				log.warn("doSnapShot: LocalESClusterID not yet defined. Exiting.");
				return false;
			}

		} else {
			log.warn("doSnapShot: LocalESNodeID not yet defined. Exiting.");
			return false;
		}


		//if (ClusterInfo.getClusterInfo().getMaster(eszkService.getLocalESNodeID()) == null) {
		log.info("doSnapShot: eszkService.getLocalESClusterID(): " + eszkService.getLocalESClusterID());

		if (eszkService.getLocalESNodeID().contentEquals(zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()))) {
			amIelected = true;
		} else {
			amIelected = false;
		}
		if (!amIelected) {
			log.info("doSnapShot: this node is not master. stopping.");
			return false;		
		}

		/*
		 * create an ephemeral nodes for this action
		 */
		String childNode = HAES_SNAPSHOT.concat("/").concat(ES_NODE).concat("/").concat("ID");
		if (zkClient.exists(childNode)) {
			snapshot_executing=true;
		} else {
			snapshot_executing=false;
			SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
			Date date = new Date(System.currentTimeMillis());

			String begindataTime = "snaphsot start time: ".concat(formatter.format(date));
			String nodeID = eszkService.getLocalESNodeID();

			zkClient.create(childNode, nodeID, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

			/*
			 * update /SNAPSHOT
			 */

			zkClient.writeData(HAES_SNAPSHOT, begindataTime);
			zkClient.writeData(HAES_SNAPSHOT.concat("/").concat(ES_NODE), nodeID);
		}

		/*
		 * check if one snapshot is not already executing
		 */
		if (snapshot_executing) {
			log.info("doSnapShot: one snapshot is already executing. stopping.");
			return false;			
		}

		/*
		 * call snapshot/restore services with snapshot parameter
		 */
		log.info("doSnapShot: Executing sync action with snapshot/restore script: ".concat(srScriptsProperties.getdaemonscript()));

		String directory = srScriptsProperties.getrootlocation();
		String command = "./".concat(srScriptsProperties.getdaemonscript());

		String[] args = new String[]{"-U", srScriptsProperties.getuser(), "-a", "snapshot", "-l", srScriptsProperties.geteventservicelocation(), "-r", srScriptsProperties.getsnapshotrepositorylocation()};
		Map<String, String> environment = new HashMap<>();
		environment.put("context", "PROD");
		environment.put("type", "real");

		/*
		 * this is a synchronous shell call
		 */
		log.info("doSnapShot: Running shell with argument directory: " + directory);
		log.info("doSnapShot: Running shell with argument command: " + command);
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < args.length; i++) {
			sb.append(" "+args[i]);
		}
		log.info("doSnapShot: Running shell with argument args: " + sb.toString());

		if (!ShellUtils.runShell(directory, command, args, environment)) {
			/*
			 * sending email
			 */
			String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Snapshot Shell Execution failed on node: " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID();

			if (emailService.sendMail(message)) {
				log.debug("doSnapShot/sendMail succeeded");
			} else {
				log.error("doSnapShot/sendMail failed");
			}

			/*
			 * need to revert all statuses and zk nodes created, which is done down bellow
			 */
			rc =  false;
		}

		/*
		 * this snapshot hold by that node has finished
		 */


		log.info("doSnapShot: Running shell ended.");
		if (zkClient.exists(childNode)) {
			zkClient.delete(childNode);
			snapshot_executing=false;
		} 
		log.info("doSnapShot: End.");

		// default
		return rc;
	}
	@Override
	//@Async the springboot scheduler is already async
	public boolean doRestore() {
		log.info("doRestore: Begin.");

		String local_loopback_IP = localProperties.getloopback();
		log.debug("doRestore/local_loopback_IP: {}", local_loopback_IP);
		boolean rc = true;
		boolean restore_executing = false;

		/*
		 * check if restore execution has been frozen
		 */
		if (eszkService.getRestoreFreezeStatus().contentEquals("true"))
		{
			log.info("doRestore: Restore execution has been frozen. stopping.");
			return false;
		}

		/*
		 * check if local cluster is passive
		 */
		if (eszkService.getESclusterType(eszkService.getLocalESClusterID()).contentEquals("active")) {
			log.info("doRestore: this node is on the active cluster. stopping.");
			return false;

		}

		/*
		 * check if local node is master (and elected to execute snapshot)
		 * if clusterinfo does not contain any master yet, we need to return
		 */
		// defensive code here due to initialization phase not terminated

		if (eszkService.getLocalESNodeID() != null) {
			if (eszkService.getLocalESClusterID() != null) {
				if (zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()) != null) {

				} else {
					log.warn("doRestore: Leader node  not yet defined. Exciting.");
					return false;
				}

			} else {
				log.warn("doRestore: LocalESClusterID not yet defined. Exciting.");
				return false;
			}

		} else {
			log.warn("doRestore: LocalESNodeID not yet defined. Exciting.");
			return false;
		}

		//if (ClusterInfo.getClusterInfo().getMaster(eszkService.getLocalESNodeID()) == null) {
		log.info("doRestore: eszkService.getLocalESClusterID(): " + eszkService.getLocalESClusterID());

		if (!eszkService.getLocalESNodeID().contentEquals(zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()))) {
			log.info("doRestore: this node is not master. stopping.");
			return false;		
		}

		/*
		 * create an ephemeral nodes for this action
		 */
		String childNode = HAES_RESTORE.concat("/").concat(ES_NODE).concat("/").concat("ID");
		if (zkClient.exists(childNode)) {
			restore_executing=true;
		} else {
			restore_executing=false;
			SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
			Date date = new Date(System.currentTimeMillis());

			String begindataTime = "restore start time: ".concat(formatter.format(date));
			String nodeID = eszkService.getLocalESNodeID();

			zkClient.create(childNode, nodeID, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

			/*
			 * update /SNAPSHOT
			 */

			zkClient.writeData(HAES_RESTORE, begindataTime);
			zkClient.writeData(HAES_RESTORE.concat("/").concat(ES_NODE), nodeID);
		}

		/*
		 * check if one snapshot is not already executing
		 */
		if (restore_executing) {
			log.info("doRestore: one restore is already executing. stopping.");
			return false;			
		}

		/*
		 * call snapshot/restore services with snapshot parameter
		 */
		log.info("doRestore: Executing sync action with snapshot/restore script: ".concat(srScriptsProperties.getdaemonscript()));

		String directory = srScriptsProperties.getrootlocation();
		String command = "./".concat(srScriptsProperties.getdaemonscript());

		String[] args = new String[]{"-U", srScriptsProperties.getuser(), "-a", "fastrestore", "-l", srScriptsProperties.geteventservicelocation(), "-r", srScriptsProperties.getsnapshotrepositorylocation()};
		Map<String, String> environment = new HashMap<>();
		environment.put("context", "PROD");
		environment.put("type", "real");

		/*
		 * this is a synchronous shell call
		 */
		log.info("doRestore: Running shell with argument directory: " + directory);
		log.info("doRestore: Running shell with argument command: " + command);
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < args.length; i++) {
			sb.append(" "+args[i]);
		}
		log.info("doRestore: Running shell with argument args: " + sb.toString());


		if (!ShellUtils.runShell(directory, command, args, environment)) {
			/*
			 * sending email
			 */
			String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Restore Shell Execution failed on node: " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID();

			if (emailService.sendMail(message)) {
				log.debug("doRestore/sendMail succeeded");
			} else {
				log.error("doRestore/sendMail failed");
			}

			/*
			 * need to revert all statuses and zk nodes created, which is done down bellow
			 */
			rc =  false;
		}

		/*
		 * this snapshot hold by that node has finished
		 */


		log.info("doRestore: Running shell ended.");
		if (zkClient.exists(childNode)) {
			zkClient.delete(childNode);
			restore_executing=false;
		} 

		log.info("doRestore: Begin.");

		// default
		return rc;
	}
	@Override
	//@Async the springboot scheduler is already async
	public boolean doRestoreTo(String snapshotid) {
		log.info("doRestoreTo: Begin.");

		String local_loopback_IP = localProperties.getloopback();
		log.debug("doRestoreTo/local_loopback_IP: {}", local_loopback_IP);
		boolean rc = true;
		boolean restore_executing = false;
		boolean amIelected = false;

		/*
		 * check if restore execution has been frozen
		 */
		if (eszkService.getRestoreFreezeStatus().contentEquals("true"))
		{
			log.info("doRestoreTo: Restore execution has been frozen. stopping.");
			return false;
		}

		/*
		 * check if local cluster is passive.
		 * for this RestoreTo, we allow it on active cluster: this is an emergency case here
		 */
		/*
		if (eszkService.getESclusterType(eszkService.getLocalESClusterID()).contentEquals("active")) {
			log.info("doRestoreTo: this node is on the active cluster. stopping.");
			return false;

		}
		 */

		/*
		 * check if local node is master (eligible to execute snapshot)
		 * if clusterinfo does not contain any master yet, we need to return
		 */
		// defensive code here due to initialization phase not terminated

		if (eszkService.getLocalESNodeID() != null) {
			if (eszkService.getLocalESClusterID() != null) {
				if (zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()) != null) {

				} else {
					log.warn("doRestoreTo: Leader node  not yet defined. Exciting.");
					return false;
				}

			} else {
				log.warn("doRestoreTo: LocalESClusterID not yet defined. Exciting.");
				return false;
			}

		} else {
			log.warn("doRestoreTo: LocalESNodeID not yet defined. Exciting.");
			return false;
		}

		log.info("doRestoreTo: eszkService.getLocalESClusterID(): " + eszkService.getLocalESClusterID());

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			log.info("doRestoreTo: Thread sleep error: " + e.getLocalizedMessage());
		}


		if (eszkService.getLocalESNodeID().contentEquals(zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()))) {
			amIelected = true;
		} else {
			amIelected = false;
		}
		if (!amIelected) {
			log.info("doRestoreTo: this node is not master. stopping.");
			return false;		
		}

		/*
		 * create an ephemeral nodes for this action
		 */
		String childNode = HAES_RESTORE.concat("/").concat(ES_NODE).concat("/").concat("ID");
		if (zkClient.exists(childNode)) {
			restore_executing=true;
		} else {
			restore_executing=false;
			SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
			Date date = new Date(System.currentTimeMillis());

			String begindataTime = "restore start time: ".concat(formatter.format(date));
			String nodeID = eszkService.getLocalESNodeID();

			zkClient.create(childNode, nodeID, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

			/*
			 * update /SNAPSHOT
			 */

			zkClient.writeData(HAES_RESTORE, begindataTime);
			zkClient.writeData(HAES_RESTORE.concat("/").concat(ES_NODE), nodeID);
		}

		/*
		 * check if one snapshot is not already executing
		 */
		if (restore_executing) {
			log.info("doRestoreTo: one restore is already executing. stopping.");
			return false;			
		}

		/*
		 * call snapshot/restore services with snapshot parameter
		 */
		log.info("doRestoreTo: Executing sync action with snapshot/restore script: ".concat(srScriptsProperties.getdaemonscript()));

		String directory = srScriptsProperties.getrootlocation();
		String command = "./".concat(srScriptsProperties.getdaemonscript());

		String[] args = new String[]{"-U", srScriptsProperties.getuser(), "-a", "restoreto", "-l", srScriptsProperties.geteventservicelocation(), "-r", srScriptsProperties.getsnapshotrepositorylocation(), "-s", snapshotid};
		Map<String, String> environment = new HashMap<>();
		environment.put("context", "PROD");
		environment.put("type", "real");

		/*
		 * this is a synchronous shell call
		 */
		log.info("doRestoreTo: Running shell with argument directory: " + directory);
		log.info("doRestoreTo: Running shell with argument command: " + command);
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < args.length; i++) {
			sb.append(args[i]);
		}
		log.info("doRestoreTo: Running shell with argument args: " + sb.toString());


		if (!ShellUtils.runShell(directory, command, args, environment)) {
			/*
			 * sending email
			 */
			String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Restore Shell Execution failed on node: " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID();

			if (emailService.sendMail(message)) {
				log.debug("doRestoreTo/sendMail succeeded");
			} else {
				log.error("doRestoreTo/sendMail failed");
			}

			/*
			 * need to revert all statuses and zk nodes created, which is done down bellow
			 */
			rc =  false;
		}

		/*
		 * this snapshot hold by that node has finished
		 */


		log.info("doRestoreTo: Running shell ended.");
		if (zkClient.exists(childNode)) {
			zkClient.delete(childNode);
			restore_executing=false;
		} 
		log.info("doRestoreTo: End.");


		// default
		return rc;
	}


	@Override
	//@Async the springboot scheduler is already async
	public boolean doRsync(String ESClusterID, String ESNodeID) {
		String local_loopback_IP = localProperties.getloopback();
		log.debug("doRsync/local_loopback_IP: {}", local_loopback_IP);

		boolean rc = true;
		log.info("doRsync: Initiating rsync from one active cluster node to one passive cluster node.");
		boolean amIelected = true;

		/*
		 * check which cluster is active
		 */

		/*
		 * check which node did the last snapshot successfully
		 */

		/*
		 * check if that node has been elected and master (one writer only)
		 */
		// defensive code here due to initialization phase not terminated

		if (eszkService.getLocalESNodeID() != null) {
			if (eszkService.getLocalESClusterID() != null) {
				if (zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()) != null) {

				} else {
					log.warn("doRsync: Leader node  not yet defined. Exciting.");
					return false;
				}

			} else {
				log.warn("doRsync: LocalESClusterID not yet defined. Exciting.");
				return false;
			}

		} else {
			log.warn("doRsync: LocalESNodeID not yet defined. Exciting.");
			return false;
		}


		/*
		 * call snapshot/restore services with rsync parameter
		 */
		log.info("doRsync: Executing rsync action with snapshot/restore script: ".concat(srScriptsProperties.getdaemonscript()));

		String directory = srScriptsProperties.getrootlocation();
		String command = "./".concat(srScriptsProperties.getdaemonscript());

		String[] args = new String[]{"-U", srScriptsProperties.getuser(), "-a", "rsync", "-l", srScriptsProperties.geteventservicelocation(), "-r", srScriptsProperties.getsnapshotrepositorylocation(), "-H", srScriptsProperties.getRsynctargethostdns()};
		Map<String, String> environment = new HashMap<>();
		environment.put("context", "PROD");
		environment.put("type", "real");

		/*
		 * this is a synchronous shell call
		 */
		if (!ShellUtils.runShell(directory, command, args, environment)) return false;

		// default
		return rc;

	}


    @Override
    public boolean doCheckRepository() {

        boolean rc = true;
        /*
		 * check mount point is ok 
		 * 
         */
        if (!srScriptsProperties.getFilesystemtype().equals("S3")) {
            log.info("doCheckRepository: checking Snapshot Repository Mount point.");
            String directory = srScriptsProperties.getrootlocation();
            String command = "./".concat(srScriptsProperties.getdaemonscript());

            String[] args = new String[]{"-U", srScriptsProperties.getuser(), "-a", "mount", "-l", srScriptsProperties.geteventservicelocation(), "-r", srScriptsProperties.getsnapshotrepositorylocation()};
            Map<String, String> environment = new HashMap<>();
            environment.put("context", "PROD");
            environment.put("type", "real");

            /*
		 * this is a synchronous shell call
             */
            if (!ShellUtils.runShell(directory, command, args, environment)) {
                /* 
			 * sending email
                 */
                String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " detected an error on the repository mount point.";

                if (emailService.sendMail(message)) {
                    log.debug("doCheckRepository/sendMail succeeded");
                } else {
                    log.error("doCheckRepository/sendMail failed");
                }

                return false;
            }


            /*
		 * check FS level is ok
		 * 
             */
            log.info("doCheckRepository: checking Snapshot Repository File System.");
            rc = FileUtils.isStorageAvailable(srScriptsProperties.getsnapshotrepositorylocation(), eszkService.getESclusterType(eszkService.getLocalESClusterID()));

            if (!rc) {
                /* 
			 * sending email
                 */
                String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " detected that less than 100 Mb space left on repository.";

                if (emailService.sendMail(message)) {
                    log.debug("doCheckRepository/sendMail succeeded");
                } else {
                    log.error("doCheckRepository/sendMail failed");
                }
            }
        }

        // default
        return rc;

    }

	@Override
	public void registerESClusterAvailabilityChangeWatcher(String path, IZkDataListener iZkDataListener) {
		zkClient.subscribeDataChanges(path, iZkDataListener);
	}

}
