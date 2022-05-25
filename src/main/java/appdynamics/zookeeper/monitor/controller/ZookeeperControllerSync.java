package appdynamics.zookeeper.monitor.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;
import appdynamics.zookeeper.monitor.controller.exceptions.ClusterIDNotFoundException;
import appdynamics.zookeeper.monitor.controller.exceptions.NodeStatusNotFoundException;
import appdynamics.zookeeper.monitor.es.service.EShealthcheck;
import appdynamics.zookeeper.monitor.model.Snapshot;
import appdynamics.zookeeper.monitor.model.SnapshotsList;

import appdynamics.zookeeper.monitor.util.EmailService;
import lombok.extern.slf4j.Slf4j;

/** @author "Marc Pichon" 08/10/20 */
@Slf4j
@RestController
public class ZookeeperControllerSync implements ApplicationContextAware {

	@Autowired 
	private ZkService zkService;
	@Autowired 
	private ESZkService eszkService;
	@Autowired 
	private ESSnapshotRestoreService esSnapshotRestoreService;
	@Autowired 
	@Qualifier("eshealthcheckService")
	private EShealthcheck eshealthcheck;
	@Autowired 
	private EmailService emailService;

	private RestTemplate restTemplate = new RestTemplate();

	private ApplicationContext context;

	private Long totalRequestNB = 0L;
	private Long totalNegativeRequestNB = 0L;
	private Long totalPositiveRequestNB = 0L;


	//HAES
	/*
	 * serves Round Robin LB request for one node
	 * if node is in active ES cluster and node healty, then HTTP RC=200 and response is "OK"
	 * if node is in passive ES cluster or node unhealthy, then HTTP RC=403 and response is "KO"
	 */

	@GetMapping("/EScluster/Node/Status")
	public @ResponseBody ResponseEntity<String> getNodeStatus(HttpServletRequest request) {
		/*
		 * access on localhost ES node only
		 */
		String local_node_IP = "127.0.0.1";
		String local_node_port = "9200";
		String ipport = local_node_IP.concat(":").concat(local_node_port);
		boolean ipport_status;
		String ESclusterId;
		String ESnodeId;
		boolean EScluster_active = false;
		String EScluster_type;
		String EScluster_availability;
		boolean EScluster_available = false;
		boolean node_available = false;
		String node_status;
		String reason = "Information: ";

		/*
		 * identify which ES cluster this node belongs to
		 */
		ESclusterId = eszkService.getLocalESClusterID();

		/*
		 * check if that ES cluster is available or not
		 */
		if (ESclusterId != null) {
			/*
			 * check if cluster is available (green/yellow or Red status of cluster itself
			 */
			EScluster_availability = eszkService.getClusterAvailability(ESclusterId);
			if (EScluster_availability != null) {
				if (EScluster_availability.contentEquals("ok")) {
					EScluster_available = true;
					reason = reason +"/EScluster is available";
				} else {
					EScluster_available = false;
					reason = reason +"/EScluster is not available";
				}
			} else {
				EScluster_available = false;
				reason = reason +"/EScluster availability check failed";
			}
			/*
			 * check if that ES cluster is passive or active
			 */

			EScluster_type = eszkService.getESclusterType(ESclusterId);
			if (EScluster_type.contentEquals("active")) {
				EScluster_active = true;
				reason = reason +"/EScluster is active";
			} else {
				reason = reason + "/EScluster is passive";
			}
			/*
			 * check if node is available
			 */
			//node_status = eszkService.getNodeStatus(ESclusterId,ipport);
			ESnodeId = eszkService.getLocalESNodeID();
			node_status = eszkService.getNodeStatus(ESclusterId,ESnodeId);
			if (node_status != null) {
				if (node_status.contentEquals("ok")) {
					node_available = true;
					reason = reason + "/Node is available";
				} else {
					reason = reason + "/Node not available";
				}

				totalRequestNB++;
				reason = reason + " Req Nb: " + String.valueOf(totalRequestNB);
				if (EScluster_available && EScluster_active && node_available) {
					totalPositiveRequestNB++;
					reason = reason + " OK Req Nb: " + String.valueOf(totalPositiveRequestNB);
					reason = reason + " KO Req Nb: " + String.valueOf(totalNegativeRequestNB);

					return new ResponseEntity<String>(reason.concat("  /ESclusterId=") + ESclusterId.concat("/NodeID=") + ESnodeId + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
				} else {
					totalNegativeRequestNB++;
					reason = reason + " OK Req Nb: " + String.valueOf(totalPositiveRequestNB);
					reason = reason + " KO Req Nb: " + String.valueOf(totalNegativeRequestNB);

					return new ResponseEntity<String>(reason.concat("  /ESclusterId=") + ESclusterId.concat("/NodeID=") + ESnodeId + " HTTP RC: " + HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN);
				}
			} else {
				throw new NodeStatusNotFoundException("Node status is null");
			}
		} else {
			throw new ClusterIDNotFoundException("ESclusterId is null");
			//return new ResponseEntity<String>(reason.concat(" Internal Error: ESclusterId is null.") + ESnodeId, HttpStatus.FORBIDDEN);
		}
		/*
		 * default
		 */
		//return new ResponseEntity<String>(reason.concat("/NodeID=") + ESnodeId, HttpStatus.FORBIDDEN);

	}

	/*
	 * a way to get control on ES node status
	 * clusterid is either CLUSTER1 or CLUSTER2
	 * status is either ok or ko 
	 * 
	 */
	//@PutMapping("/EScluster/ForceNodeStatus/{clusterid}/{nodeid}/{status}")
	@GetMapping("/EScluster/ForceNodeStatus/{clusterid}/{nodeid}/{status}")
	public @ResponseBody ResponseEntity<String> ForceNodeStatus (@PathVariable String clusterid, @PathVariable String nodeid, @PathVariable String status) {
		String reason = "Information: ";
		if (!"CLUSTER1".contentEquals(clusterid) && !"CLUSTER2".contentEquals(clusterid)) {
			return new ResponseEntity<String>(reason.concat(clusterid).concat(" as parameter. only allowed values: CLUSTER1,CLUSTER2"), HttpStatus.FORBIDDEN);
		}
		if (!"ok".contentEquals(status) && !"ko".contentEquals(status)) {
			return new ResponseEntity<String>(reason.concat(status).concat(" as parameter. only allowed values: ok,ko"), HttpStatus.FORBIDDEN);
		}

		boolean rc_ok = eszkService.forceNodeStatus(clusterid, nodeid, status);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("update of node status for ").concat(clusterid).concat("/").concat(nodeid).concat(" succedded"), HttpStatus.OK);

		}
		/*
		 * default
		 */
		return new ResponseEntity<String>(reason.concat("update of node status for ").concat(clusterid).concat("/").concat(nodeid).concat(" failed"), HttpStatus.FAILED_DEPENDENCY);

	}

	/*
	 * a way to get control on ES node status
	 * clusterid is either CLUSTER1 or CLUSTER2
	 * state is either true or false 
	 * 
	 */
	//@PutMapping("/EScluster/ForceNodeAsMaster/{clusterid}/{nodeid}/{state}")
	@GetMapping("/EScluster/ForceNodeAsMaster/{clusterid}/{nodeid}/{state}")
	public @ResponseBody ResponseEntity<String> ForceNodeAsMaster (@PathVariable String clusterid, @PathVariable String nodeid, @PathVariable String state) {
            
            // OLF // No more possible for the moment. To study...
		/* String reason = "Information: ";
		if (!"CLUSTER1".contentEquals(clusterid) && !"CLUSTER2".contentEquals(clusterid)) {
			return new ResponseEntity<String>(reason.concat(clusterid).concat(" as parameter. only allowed values: CLUSTER1,CLUSTER2"), HttpStatus.FORBIDDEN);
		}
		if (!"true".contentEquals(state) && !"false".contentEquals(state)) {
			return new ResponseEntity<String>(reason.concat(state).concat(" as parameter. only allowed values: true,false"), HttpStatus.FORBIDDEN);
		}

		boolean rc_ok = eszkService.forceNodeAsMaster(clusterid, nodeid);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("update of node master state for ").concat(clusterid).concat("/").concat(nodeid).concat(" succedded. be aware you are breaking the logic of master election node."), HttpStatus.OK);

		}
		/*
		 * default
		 */
		// return new ResponseEntity<String>(reason.concat("update of node master state for ").concat(clusterid).concat("/").concat(nodeid).concat(" failed"), HttpStatus.FAILED_DEPENDENCY);
                return new ResponseEntity<String>("Cannot force an HAES leader in this version... Will come soon", HttpStatus.FAILED_DEPENDENCY);

	}

	//@PutMapping("/EScluster/FailOver")
	@GetMapping("/EScluster/FailOver")
	public @ResponseBody ResponseEntity<String> doFailOver(HttpServletRequest request) {
		String reason = "Information: ";
		/*
		 * first, check this the elected node 
		 * if not, then ask the task to the leader
		 */
		if (!zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID())) {
			return new ResponseEntity<String>(reason.concat("ES cluster1 fail over to ES cluster2/HAES FailOver failed: this node is not the elected master on this cluster") + " HTTP RC: " + HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN);
		}

		CompletableFuture<Boolean> rc_ok_cf = esSnapshotRestoreService.doFailOver();
		boolean rc_ok = false;
		try {
			rc_ok = rc_ok_cf.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("doFailBack error: fail over return code action execution control failed");
			rc_ok = false;
		}
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("ES cluster1 fail over to ES cluster2/HAES FailOver succeeded") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("ES cluster1 fail over to ES cluster2/HAES FailOver failed") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}
	}
	/*
	 * allows to freeze/un-freeze snapshot executions.
	 */
	@GetMapping("/EScluster/Snapshot/freeze/{state}")
	public @ResponseBody ResponseEntity<String> doSnapshotFreezeState(@PathVariable String state) {
		String reason = "Information: ";
		if (!"true".contentEquals(state) && !"false".contentEquals(state)) {
			return new ResponseEntity<String>(reason.concat(state).concat(" as parameter. only allowed values: true,false"), HttpStatus.FORBIDDEN);
		}

		boolean rc_ok = eszkService.forceSnapshotFreezeStatus(state);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("Snapshot freeze/unfreeze request succedded"), HttpStatus.OK);

		}
		/*
		 * default
		 */
		return new ResponseEntity<String>(reason.concat("Snapshot freeze/unfreeze request failed"), HttpStatus.FAILED_DEPENDENCY);
	}
	
	/*
	 * allows to freeze/un-freeze restore executions.
	 */
	@GetMapping("/EScluster/Restore/freeze/{state}")
	public @ResponseBody ResponseEntity<String> doRestoretFreezeState(@PathVariable String state) {
		String reason = "Information: ";
		if (!"true".contentEquals(state) && !"false".contentEquals(state)) {
			return new ResponseEntity<String>(reason.concat(state).concat(" as parameter. only allowed values: true,false"), HttpStatus.FORBIDDEN);
		}

		boolean rc_ok = eszkService.forceRestoreFreezeStatus(state);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("Restore freeze/unfreeze request succedded"), HttpStatus.OK);

		}
		/*
		 * default
		 */
		return new ResponseEntity<String>(reason.concat("Restore freeze/unfreeze request failed"), HttpStatus.FAILED_DEPENDENCY);
	}
	//@PutMapping("/EScluster/FailBack")
	@GetMapping("/EScluster/FailBack")
	public @ResponseBody ResponseEntity<String> doFailBack(HttpServletRequest request) {
		String reason = "Information: ";
		/*
		 * first, check this the elected node 
		 * if not, then ask the task to the leader
		 */
		if (!zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID())) {
			return new ResponseEntity<String>(reason.concat("ES cluster2 fail back to ES cluster1/HAES FailBack failed: this node is not the elected master on this cluster") + " HTTP RC: " + HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN);
		}


		CompletableFuture<Boolean> rc_ok_cf = esSnapshotRestoreService.doFailBack();
		boolean rc_ok = false;
		try {
			rc_ok = rc_ok_cf.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("doFailBack error: fail over return code action execution control failed");
			rc_ok = false;
		}

		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("ES cluster1 fail back from ES cluster2/HAES FailBack succeeded") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("ES cluster1 fail back from ES cluster2/HAES FailBack failed") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}


	}
	
	@GetMapping("/EScluster/LeaderNodeForFailBack")
	public @ResponseBody ResponseEntity<String> getLeaderNodeForFailBack(HttpServletRequest request) {
		String reason = "Information: ";
		
		String leader = zkService.getLeaderNodeData2("CLUSTER2");
		
		if (leader != null) {
			String leaderIPport = zkService.getZNodeData("/HAES/CLUSTER2/".concat(leader));
			String[] parts = leaderIPport.split(":");
			String leaderIP = parts[0];
			return new ResponseEntity<String>(reason.concat("current leader node for cluster2:").concat(leader).concat(" reference: ").concat(leaderIP) + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("leader node for cluster2 not yet available") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}
	}
	/*
	 * allows to freeze/un-freeze failover execution.
	 * state = true or false
	 */
	@GetMapping("/EScluster/FailOver/freeze/{state}")
	public @ResponseBody ResponseEntity<String> doFailOverFreezeState(@PathVariable String state) {
		String reason = "Information: ";
		if (!"true".contentEquals(state) && !"false".contentEquals(state)) {
			return new ResponseEntity<String>(reason.concat(state).concat(" as parameter. only allowed values: true,false"), HttpStatus.FORBIDDEN);
		}

		boolean rc_ok = eszkService.forceFailOverFreezeStatus(state);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("FailOver freeze/unfreeze request succedded"), HttpStatus.OK);

		}
		/*
		 * default
		 */
		return new ResponseEntity<String>(reason.concat("Snapshot freeze/unfreeze request failed"), HttpStatus.FAILED_DEPENDENCY);
	}
	/*
	 * allows to freeze/un-freeze failover execution.
	 * state = true or false
	 */
	@GetMapping("/EScluster/FailOver/freeze/state")
	public @ResponseBody ResponseEntity<String> getFailOverFreezeState() {
		String reason = "Information: ";

		String state = eszkService.getFailoverFreezeStatus();
		if (state != null) {
			return new ResponseEntity<String>(reason.concat("FailOver freeze state: " + state), HttpStatus.OK);

		}
		/*
		 * default
		 */
		return new ResponseEntity<String>(reason.concat("FailOver freeze state request failed"), HttpStatus.FAILED_DEPENDENCY);
	}
	/*
	 * a way to get control on ES cluster type
	 * clusterid is either CLUSTER1 or CLUSTER2
	 * type is either active or passive 
	 * 
	 */

	@GetMapping("/EScluster/SetType/{clusterid}/{type}")
	public @ResponseBody ResponseEntity<String> setClusterType (@PathVariable String clusterid, @PathVariable String type) {
		String reason = "Information: ";
		if (!"CLUSTER1".contentEquals(clusterid) && !"CLUSTER2".contentEquals(clusterid)) {
			return new ResponseEntity<String>(reason.concat(clusterid).concat(" as parameter. only allowed values: CLUSTER1,CLUSTER2"), HttpStatus.FORBIDDEN);
		}
		if (!"active".contentEquals(type) && !"passive".contentEquals(type)) {
			return new ResponseEntity<String>(reason.concat(type).concat(" as parameter. only allowed values: active,passive"), HttpStatus.FORBIDDEN);
		}

		boolean rc_ok = eszkService.setClusterType(clusterid, type);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("update of type for ").concat(clusterid).concat(" succedded"), HttpStatus.OK);

		}
		/*
		 * default
		 */
		return new ResponseEntity<String>(reason.concat("update of type for ").concat(clusterid).concat(" failed"), HttpStatus.FORBIDDEN);

	}
	/*
	 * a way to get control on ES cluster availability
	 * clusterid is either CLUSTER1 or CLUSTER2
	 * type is either ok (green/yellow) or ko (red) 
	 * 
	 */
	//@PutMapping("/EScluster/SetAvailability/{clusterid}/{availability}")
	@GetMapping("/EScluster/SetAvailability/{clusterid}/{availability}")
	public @ResponseBody ResponseEntity<String> setClusterAvailability (@PathVariable String clusterid, @PathVariable String availability) {
		String reason = "Information: ";
		if (!"CLUSTER1".contentEquals(clusterid) && !"CLUSTER2".contentEquals(clusterid)) {
			return new ResponseEntity<String>(reason.concat(clusterid).concat(" as parameter. only allowed values: CLUSTER1,CLUSTER2"), HttpStatus.FORBIDDEN);
		}
		if (!"ok".contentEquals(availability) && !"ko".contentEquals(availability)) {
			return new ResponseEntity<String>(reason.concat(availability).concat(" as parameter. only allowed values: active,passive"), HttpStatus.FORBIDDEN);
		}

		boolean rc_ok = eszkService.setClusterAvailability(clusterid, availability);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("update of type for ").concat(clusterid).concat(" succedded"), HttpStatus.OK);

		}
		/*
		 * default
		 */
		return new ResponseEntity<String>(reason.concat("update of type for ").concat(clusterid).concat(" failed"), HttpStatus.FAILED_DEPENDENCY);

	}
	//@PutMapping("/HAES/CreateTree")
	@GetMapping("/HAES/CreateTree")
	public @ResponseBody ResponseEntity<String> createZkTree(HttpServletRequest request) {
		String reason = "Information: ";

		boolean rc_ok = eszkService.initNodes();
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("/HAES zk tree created") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("/HAES zk tree not created") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}

	}
	//@PutMapping("/HAES/DeleteTree")
	@GetMapping("/HAES/DeleteTree")
	public @ResponseBody ResponseEntity<String> deleteZkTree(HttpServletRequest request) {
		String reason = "Information: ";

		boolean rc_ok = eszkService.deleteZkTree();
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("/HAES zk tree deleted") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("/HAES zk tree not deleted") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}

	}
	@GetMapping("/HAES/TestMail")
	public @ResponseBody ResponseEntity<String> testEmail(HttpServletRequest request) {
		String reason = "Information: ";

		boolean rc_ok = emailService.sendMail("Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " Test mail thru URL /HAES/TestMail");
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("an attempt to send a test mail has been executed.") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("an attempt to send a test mail has been executed.") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}

	}
	@GetMapping("/EScluster/RestoreTo/{snapshotid}")
	public @ResponseBody ResponseEntity<String> doRestoreTo(@PathVariable String snapshotid) {
		String reason = "Information: ";
		/*
		 * first, check this the elected node 
		 * if not, then ask the task to the leader
		 */
		if (!zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID())) {
			return new ResponseEntity<String>(reason.concat("RestoreTo failed: this node is not the elected master on this cluster") + " HTTP RC: " + HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN);
		}


		boolean rc_ok = esSnapshotRestoreService.doRestoreTo(snapshotid);
		if (rc_ok) {
			return new ResponseEntity<String>(reason.concat("RestoreTo succeeded") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("RestoreTo failed") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}


	}

	@GetMapping("/EScluster/GetSnapshotsList")
	public @ResponseBody ResponseEntity<String> getSnapshotsList() {
		String reason = "<p align=\"center\">Snapshots Information</p><br>";
		SnapshotsList snapshotslist;
		try {
			snapshotslist = eshealthcheck.esgetSnapshotsList();

			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			String output = "";
			if (snapshotslist != null) {
				Iterator it = snapshotslist.getSnapshots().iterator();
				while(it.hasNext()) {
					Snapshot snapshot = (Snapshot) it.next();
					log.debug("getSnapshotsList/Snapshots is: " + snapshot.getSnapshot() + " status: " + snapshot.getState()  + " Start time: " + snapshot.getStartTime()  + " End time: " + snapshot.getEndTime()  + " Duration (minutes): " + (snapshot.getDurationInMillis() / 3600000));
					output = "Snapshot ID " + snapshot.getSnapshot() + " status: " + snapshot.getState()  + " Start time: " + snapshot.getStartTime()  + " End time: " + snapshot.getEndTime();
					//printWriter.println(output);
					reason = reason.concat(output).concat("<br>");
				}
				log.debug("getSnapshotsList/stringWriter is: " + stringWriter.toString());

				//reason.concat(stringWriter.toString());

				log.debug("getSnapshotsList/reason is: " + reason);

				return new ResponseEntity<String>(reason.concat("<p>GetSnapshotsList succeeded") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
			} else {
				return new ResponseEntity<String>(reason.concat(" GetSnapshotsList failed") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
			}

		} catch (JsonParseException e1) {
			log.error("getSnapshotsList/JsonParseException error is: {}", e1.getLocalizedMessage());
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			log.error("getSnapshotsList/JsonMappingException error is: {}", e1.getLocalizedMessage());
			e1.printStackTrace();
		} catch (IOException e1) {
			log.error("getSnapshotsList/IOException error is: {}", e1.getLocalizedMessage());
			e1.printStackTrace();
		}
		return new ResponseEntity<String>(reason.concat(" GetSnapshotsList failed") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);

	}
	@GetMapping("/EScluster/GetProcessList")
	public @ResponseBody ResponseEntity<String> getProcessList() {
		String reason = "<p align=\"center\">Process Information - java processes</p><br>";
		String line;
		Process process;
		try {
			process = Runtime.getRuntime().exec("ps -ef");
			process.getOutputStream().close();
			BufferedReader input =
					new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = input.readLine()) != null) {
				if (line.contains("java")) {
					reason = reason.concat("<br>");
					reason = reason.concat(line).concat("<br>");
				}
			}
			input.close();

		} catch (IOException e) {
			log.error("getProcessList/Error is: " + e.getLocalizedMessage().toString());
			return new ResponseEntity<String>(reason.concat(" GetProcessList failed") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);

		}
		return new ResponseEntity<String>(reason.concat("<p>GetProcessList succeeded\"") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
	}
	@GetMapping("/EScluster/Diagnostic")
	public @ResponseBody ResponseEntity<String> getDiagnostic() {
		String reason = "";
		reason = eshealthcheck.getDiagnostic();
		
		/*
		 * test/under construction. needs ES libs to be embedded and align to AppD ES
		 */
		//ESDiagnosticViaAPI.getInstance().clusterHealthDiag();
		//ESDiagnosticViaAPI.getInstance().indexPerformanceDiag();
		//ESDiagnosticViaAPI.getInstance().nodeJVMDiag();
		//ESDiagnosticViaAPI.getInstance().nodeResourcesDiag();
		//ESDiagnosticViaAPI.getInstance().searchPerformanceDiag();
		//ESDiagnosticViaAPI.getInstance().close();		
		/*
		 * end test
		 */

		
		
		log.debug("getDiagnostic/reason is: " + reason);
		if (reason != null) {
			return new ResponseEntity<String>(reason.concat("<p>getDiagnostic succeeded") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("<p>getDiagnostic failed") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}
	}
	
	/*
	 * internal check of zk tree
	 */
	@GetMapping("/HAES/getZKTree")
	public @ResponseBody ResponseEntity<String> getZkTree(HttpServletRequest request) {
		String reason = "<p align=\\\"center\\\">Zookeeper Tree Information</p><br>";

		ArrayList<ArrayList<String>> zknodes = zkService.getAllZKNodes();
		if (zknodes != null) {
			for (int i = 0; i < zknodes.size(); i++) {
				for (int j = 0; j < zknodes.get(i).size(); j++) {
					
					reason = reason.concat(zknodes.get(i).get(j)).concat("<br>");
				}
			}
			return new ResponseEntity<String>(reason.concat("/HAES zk tree:") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>(reason.concat("/HAES zk tree request failed.") + " HTTP RC: " + HttpStatus.FAILED_DEPENDENCY, HttpStatus.FAILED_DEPENDENCY);
		}

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;

	}

}
