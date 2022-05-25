package appdynamics.zookeeper.monitor.impl;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ELECTION_NODE_2;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.LIVE_NODES;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER1;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER2;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER_AVAILABILITY; 
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER_TYPE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER_UP;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_NODE_PREFIX;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_NODE_STATUS;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_NODE_SHARD_STATUS;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_NODE_ISMASTER;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_LOCAL_NODE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_SNAPSHOT;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_RESTORE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_NODE;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_SNAPSHOT_FREEZE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_RESTORE_FREEZE; 
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_FAILOVER_FREEZE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_FAILOVER;

import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.beans.factory.annotation.Autowired;

import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.configuration.Cluster1TypeProperties;
import appdynamics.zookeeper.monitor.configuration.Cluster2TypeProperties;
import appdynamics.zookeeper.monitor.configuration.ESCluster1Properties;
import appdynamics.zookeeper.monitor.configuration.ESCluster1Properties.Cluster1;
import appdynamics.zookeeper.monitor.configuration.ESCluster2Properties;
import appdynamics.zookeeper.monitor.configuration.LocalProperties;
import appdynamics.zookeeper.monitor.configuration.ESCluster2Properties.Cluster2;
import appdynamics.zookeeper.monitor.util.EmailService;
import appdynamics.zookeeper.monitor.util.ZkHaesUtil;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.FAILOVER_READY_TO_START_STATUS;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_FAILOVER_STATUS;

/** @author "Marc Pichon" 08/10/20 */
@Slf4j
public class ESZkServiceImpl implements ESZkService {

	private ZkClient zkClient;


	public ESZkServiceImpl(String hostPort) {
		
		//zkClient = new ZkClient(hostPort, 60000, 30000, new StringSerializer());
		// using same connection everywhere
		zkClient = ZkHaesUtil.getZkClientInstance(hostPort);

		log.info("ESZkServiceImpl/ZkClient connection created.");
	}
@Override
	public void closeConnection() {
		zkClient.close();
		log.info("ESZkServiceImpl/ZkClient connection closed.");
	}
	/*
	 * (non-Javadoc)
	 * @see appdynamics.zookeeper.monitor.api.ESZkService#initNodes()
	 */
	@Autowired
	private Cluster1TypeProperties cluster1TypeProperties;
	@Autowired
	private Cluster2TypeProperties cluster2TypeProperties;

	@Autowired
	private ESCluster1Properties escluster1Properties;
	@Autowired
	private ESCluster2Properties escluster2Properties;
	@Autowired
	private LocalProperties localProperties;
	@Autowired 
	private EmailService emailService;


	@Override
	public boolean deleteZkTree() {

		/*
		 * erase all zk datas
		 */
		log.info("HAES: Deleting Zk Tree; All ES nodes of Clusters.");
		boolean rc = zkClient.deleteRecursive(HAES);

		return rc;

	}

	@Override
	public boolean initNodes() {
		boolean rc = true;
		/*
		 * erase all zk datas
		 */
		//zkClient.deleteRecursive(HAES);

		log.info("HAES: Initiating ES nodes of Cluster1.");
		List<Cluster1> lc1 = escluster1Properties.getCluster1();
		log.info(lc1.toString());


		ListIterator<Cluster1> litr_nodes = null;
		litr_nodes = lc1.listIterator();
		if (litr_nodes == null) {
			log.error("nodes list unavailable. please check your configuration file");
			return false;
		}

		while(litr_nodes.hasNext()){
			Integer current_indice = litr_nodes.nextIndex();
			Cluster1 node_detail = litr_nodes.next();
			log.info(node_detail.getIp().toString());
			log.info(node_detail.getPort().toString());
			String ipPort = node_detail.getIp().toString().concat(":").concat(node_detail.getPort().toString());
			if (!zkClient.exists(HAES)) {
				zkClient.create(HAES, "cluster1 and cluster2 are monitored here", CreateMode.PERSISTENT);
			}
			if (!zkClient.exists(HAES_CLUSTER1)) {
				zkClient.create(HAES_CLUSTER1, "all live nodes of cluster1 are monitores here", CreateMode.PERSISTENT);


				/*
				 *  now we can add sub nodes for type, up, availability
				 */

				zkClient.create(HAES_CLUSTER1.concat("/").concat(HAES_CLUSTER_TYPE), cluster1TypeProperties.getcluster1type(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(HAES_CLUSTER1.concat("/").concat(HAES_CLUSTER_UP),  "false", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(HAES_CLUSTER1.concat("/").concat(HAES_CLUSTER_AVAILABILITY),  "ko", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

			String nodeName = ES_NODE_PREFIX.concat(Integer.toString(current_indice + 1));

			String childNode = HAES_CLUSTER1.concat("/").concat(nodeName);
			log.info("creating zk node if not exist: " + childNode);

			if (!zkClient.exists(childNode)) {
				zkClient.create(childNode, nodeName + " monitored here", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.writeData(childNode, ipPort);

				/*
				 * check if this is the local node
				 */
				// sert a rien
				//if (localnodeProperties.getip().contentEquals(node_detail.getIp())) initLocalNode(HAES_CLUSTER1, nodeName);

				/*
				 * now we can add sub nodes for shard_status, status, ismaster
				 */
				zkClient.create(childNode.concat("/").concat(ES_NODE_STATUS),  "ko", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(childNode.concat("/").concat(ES_NODE_SHARD_STATUS),  "ko", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(childNode.concat("/").concat(ES_NODE_ISMASTER),  "false", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

		}

		log.info("HAES: Initiating ES nodes of Cluster2.");
		List<Cluster2> lc2 = escluster2Properties.getCluster2();
		log.info(lc2.toString());


		ListIterator<Cluster2> litr2_nodes = null;
		litr2_nodes = lc2.listIterator();
		if (litr2_nodes == null) {
			log.error("nodes list unavailable. please check your configuration file");
			return false;
		}

		while(litr2_nodes.hasNext()){
			Integer current_indice = litr2_nodes.nextIndex();
			Cluster2 node_detail = litr2_nodes.next();
			log.info(node_detail.getIp().toString());
			log.info(node_detail.getPort().toString());
			String ipPort = node_detail.getIp().toString().concat(":").concat(node_detail.getPort().toString());
			if (!zkClient.exists(HAES)) {
				zkClient.create(HAES, "cluster1 and cluster2 are monitored here", CreateMode.PERSISTENT);
			}
			if (!zkClient.exists(HAES_CLUSTER2)) {
				zkClient.create(HAES_CLUSTER2, "all live nodes of cluster1 are monitores here", CreateMode.PERSISTENT);

				/*
				 *  now we can add sub nodes for type, up, availability
				 */

				zkClient.create(HAES_CLUSTER2.concat("/").concat(HAES_CLUSTER_TYPE), cluster2TypeProperties.getcluster2type(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(HAES_CLUSTER2.concat("/").concat(HAES_CLUSTER_UP),  "false", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(HAES_CLUSTER2.concat("/").concat(HAES_CLUSTER_AVAILABILITY),  "ko", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

			}

			String nodeName = ES_NODE_PREFIX.concat(Integer.toString(current_indice + 1));

			String childNode = HAES_CLUSTER2.concat("/").concat(nodeName);
			log.info("creating zk node if not exist: " + childNode);

			if (!zkClient.exists(childNode)) {
				zkClient.create(childNode, nodeName + " monitored here", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.writeData(childNode, ipPort);

				/*
				 * check if this is the local node
				 */
				// sert a rien
				//if (localnodeProperties.getip().contentEquals(node_detail.getIp())) initLocalNode(HAES_CLUSTER2, nodeName);


				/*
				 * now we can add sub nodes for shard_status, status, ismaster
				 */
				zkClient.create(childNode.concat("/").concat(ES_NODE_STATUS),  "ko", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(childNode.concat("/").concat(ES_NODE_SHARD_STATUS),  "ko", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				zkClient.create(childNode.concat("/").concat(ES_NODE_ISMASTER),  "false", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

			}
		}
		/*
		 * creates main snapshot/restore zk tree
		 */
		if (!zkClient.exists(HAES_SNAPSHOT)) {
			zkClient.create(HAES_SNAPSHOT, "snapshot are monitored here", CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(HAES_SNAPSHOT.concat("/").concat(ES_NODE))) {
			zkClient.create(HAES_SNAPSHOT.concat("/").concat(ES_NODE), "snapshot node info here", CreateMode.PERSISTENT);
		}

		if (!zkClient.exists(HAES_RESTORE)) {
			zkClient.create(HAES_RESTORE, "restore are monitored here", CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(HAES_RESTORE.concat("/").concat(ES_NODE))) {
			zkClient.create(HAES_RESTORE.concat("/").concat(ES_NODE), "restore node info here", CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(HAES_SNAPSHOT_FREEZE)) {
			zkClient.create(HAES_SNAPSHOT_FREEZE, "false", CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(HAES_RESTORE_FREEZE)) {
			zkClient.create(HAES_RESTORE_FREEZE, "false", CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(HAES_FAILOVER)) {
			zkClient.create(HAES_FAILOVER, "false", CreateMode.PERSISTENT);
		}

		if (!zkClient.exists(HAES_FAILOVER_FREEZE)) {
			zkClient.create(HAES_FAILOVER_FREEZE, "false", CreateMode.PERSISTENT);
		}

		if (!zkClient.exists(HAES_FAILOVER_STATUS)) {
			zkClient.create(HAES_FAILOVER_STATUS, FAILOVER_READY_TO_START_STATUS, CreateMode.PERSISTENT);
		}

		return rc;
	}

	@Override
	// sert a rien
	public void initLocalNode(String clusterID, String nodeID) {

		if (zkClient.exists(HAES)) {
			zkClient.create(HAES.concat("/").concat(ES_LOCAL_NODE), "local node monitored here", CreateMode.EPHEMERAL);
		}
		/*
		 * we need to find which cluster ID and node ID is this
		 */
		zkClient.create(HAES.concat("/").concat(ES_LOCAL_NODE).concat("/").concat("CLUSTERID"),  clusterID, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		zkClient.create(HAES.concat("/").concat(ES_LOCAL_NODE).concat("/").concat("NODEID"),  nodeID, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);  
	}
        
	@Override
	public String getESclusterIdFromNode(String ipport) {
		String clusterID = null;
		ListIterator<String> litr_clusters = null;
		ListIterator<String> litr_nodes = null;
		List<String> esclusters =  zkClient.getChildren(HAES);
		litr_clusters = esclusters.listIterator();
		while(litr_clusters.hasNext()){
			clusterID = litr_clusters.next();
			List<String> esnodes =  zkClient.getChildren(HAES.concat("/").concat(clusterID));
			litr_nodes = esnodes.listIterator();
			while(litr_nodes.hasNext()){
				if (litr_nodes.next().contentEquals(ipport)) return clusterID;
			}
		}
		return null;
	}

	@Override
	public String getESclusterType(String ESclusterId) {
		String cluster_type = null;
		byte[] data; 

		log.debug("getESclusterType: ESclusterId: " + ESclusterId);
		log.debug("getESclusterType: path: " + HAES.concat("/").concat(ESclusterId).concat("/").concat(HAES_CLUSTER_TYPE));

		cluster_type = zkClient.readData(HAES.concat("/").concat(ESclusterId).concat("/").concat(HAES_CLUSTER_TYPE), true);
		/*
		data = zkClient.readData(HAES.concat("/").concat(ESclusterId).concat("/").concat(HAES_CLUSTER_TYPE), true);

		try {
			cluster_type = new String(data,"UTF-8");

			if (cluster_type != null) {
				return cluster_type;
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */
		log.debug("getESclusterType: returning cluster_type: " + cluster_type);
		return cluster_type;
	}
	@Override
	public String getNodeStatus(String ESclusterId, String ESnodeId) {
		String node_status = null;
		byte[] data; 

		log.debug("getNodeStatus: ESclusterId: " + ESclusterId);
		log.debug("getNodeStatus: ESnodeId: " + ESnodeId);
		log.debug("getNodeStatus: path: " + HAES.concat("/").concat(ESclusterId).concat("/").concat(ESnodeId).concat("/").concat(ES_NODE_STATUS));

		/*
		data = zkClient.readData(HAES.concat("/").concat(ESclusterId).concat("/").concat(ESnodeId).concat("/").concat(ES_NODE_STATUS), true);
		try {
			node_status = new String(data,"UTF-8");
			if (node_status != null) {
				return node_status;
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */
		node_status = zkClient.readData(HAES.concat("/").concat(ESclusterId).concat("/").concat(ESnodeId).concat("/").concat(ES_NODE_STATUS), true);

		log.debug("getNodeStatus: returning node_status: " + node_status);

		return node_status;
	}

	@Override
	public String getLocalESNodeID() {
		String clusterID = null;
		String nodeID = null;
		ListIterator<String> litr_clusters = null;
		ListIterator<String> litr_nodes = null;
		List<String> esclusters =  zkClient.getChildren(HAES);
		litr_clusters = esclusters.listIterator();
		while(litr_clusters.hasNext()){
			clusterID = litr_clusters.next();
			log.debug("getLocalESNodeID: clusterID: " + clusterID);

			List<String> esnodes =  zkClient.getChildren(HAES.concat("/").concat(clusterID));
			litr_nodes = esnodes.listIterator();
			while(litr_nodes.hasNext()){
				nodeID = litr_nodes.next();
				log.debug("getLocalESNodeID: nodeID: " + nodeID);

				String node_data = zkClient.readData(HAES.concat("/").concat(clusterID).concat("/").concat(nodeID));
				log.debug("getLocalESNodeID: node_data: " + node_data);
				log.debug("getLocalESNodeID: localProperties.getesnode(): " + localProperties.getesnode());

				if (node_data.contains(localProperties.getesnode())) {
					//log.info("getLocalESNodeID: found node_data: " + node_data + " = local node ip and port " + localnodeProperties.getip());
					log.debug("getLocalESNodeID: returning nodeID: " + nodeID);
					return nodeID;
				} 
			}

		}
		return null;
	}
	@Override
	public String getESNodeIDFromIP(String ip) {
		String clusterID = null;
		String nodeID = null;
		ListIterator<String> litr_clusters = null;
		ListIterator<String> litr_nodes = null;
		List<String> esclusters =  zkClient.getChildren(HAES);
		litr_clusters = esclusters.listIterator();
		while(litr_clusters.hasNext()){
			clusterID = litr_clusters.next();
			List<String> esnodes =  zkClient.getChildren(HAES.concat("/").concat(clusterID));
			litr_nodes = esnodes.listIterator();
			while(litr_nodes.hasNext()){
				nodeID = litr_nodes.next();
				String node_data = zkClient.readData(HAES.concat("/").concat(clusterID).concat("/").concat(nodeID));
				log.debug("getESNodeIDFromIP: node_data: " + node_data);
				/*
				 * check against IP
				 */
				if (node_data.contains(ip)) {
					log.debug("getESNodeIDFromIP: returning nodeID: " + nodeID);
					return nodeID;
				} 
			}

		}
		return null;
	}

	@Override
	public String getESClusterIDFromIP(String ip) {
		String clusterID = null;
		String nodeID = null;
		ListIterator<String> litr_clusters = null;
		ListIterator<String> litr_nodes = null;
		List<String> esclusters =  zkClient.getChildren(HAES);
		litr_clusters = esclusters.listIterator();
		while(litr_clusters.hasNext()){
			clusterID = litr_clusters.next();
			List<String> esnodes =  zkClient.getChildren(HAES.concat("/").concat(clusterID));
			litr_nodes = esnodes.listIterator();
			while(litr_nodes.hasNext()){
				nodeID = litr_nodes.next();
				String node_data = zkClient.readData(HAES.concat("/").concat(clusterID).concat("/").concat(nodeID));
				log.debug("getESClusterIDFromIP: node_data: " + node_data);
				/*
				 * check against IP
				 */
				if (node_data.contains(ip)) {
					log.debug("getESClusterIDFromIP: returning clusterID: " + clusterID);
					return clusterID;
				} 
			}

		}
		return null;
	}
	@Override
	public String getLocalESClusterID() {
		String clusterID = null;
		String nodeID = null;
		ListIterator<String> litr_clusters = null;
		ListIterator<String> litr_nodes = null;
		List<String> esclusters =  zkClient.getChildren(HAES);
		litr_clusters = esclusters.listIterator();
		while(litr_clusters.hasNext()){
			clusterID = litr_clusters.next();
			log.debug("getLocalESClusterID: clusterID: " + clusterID);

			List<String> esnodes =  zkClient.getChildren(HAES.concat("/").concat(clusterID));
			litr_nodes = esnodes.listIterator();
			while(litr_nodes.hasNext()){
				nodeID = litr_nodes.next();
				log.debug("getLocalESClusterID: nodeID: " + nodeID);

				String node_data = zkClient.readData(HAES.concat("/").concat(clusterID).concat("/").concat(nodeID));
				log.debug("getLocalESClusterID: node_data: " + node_data);
				log.debug("getLocalESClusterID: localProperties.getesnode(): " + localProperties.getesnode());

				if (node_data.contains(localProperties.getesnode())) {
					//log.info("getLocalESNodeID: found node_data: " + node_data + " = local node ip and port " + localnodeProperties.getip());
					log.debug("getLocalESClusterID: returning clusterID: " + clusterID);
					return clusterID;
				} 
			}

		}
		return null;

	}

	@Override
	public boolean setNodeStatus(String status) {

		String localESClusterID = getLocalESClusterID();
		String localESNodeID = getLocalESNodeID();
		log.debug("setNodeStatus/parameters: {" + localESClusterID + ","+ localESNodeID + ","+ status  + "}");
		try {
			zkClient.writeData(HAES.concat("/").concat(localESClusterID).concat("/").concat(localESNodeID).concat("/").concat(ES_NODE_STATUS), status);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("setNodeStatus error message: " + e.getMessage());
			return false;
		}

		return true;

	}
	@Override
	public boolean forceNodeStatus(String clusterID, String nodeID, String status) {
		log.debug("forceNodeStatus/parameters: {" + clusterID + ","+ nodeID + ","+ status  + "}");
		try {
			zkClient.writeData(HAES.concat("/").concat(clusterID).concat("/").concat(nodeID).concat("/").concat(ES_NODE_STATUS), status);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("forceNodeStatus error message: " + e.getMessage());
			return false;
		}

		return true;

	}
	@Override
	public boolean forceNodeAsMaster(String clusterID, String nodeID) {
		log.debug("forceNodeAsMaster/parameters: {" + clusterID + ","+ nodeID + "}");
		try {

			/*
			 * update corresponding election node: creating it if not exists already
			 * check that this node is still alive
			 * then insert into election tree
			 */
			if (!zkClient.exists(ELECTION_NODE_2.concat("/").concat(clusterID))) {
				zkClient.create(ELECTION_NODE_2.concat("/").concat(clusterID), "election node", CreateMode.PERSISTENT);
			}
			if (!zkClient.exists(LIVE_NODES.concat("/").concat(clusterID).concat("/").concat(nodeID))) {
				log.info("forceNodeAsMaster/ the live nodemust exists exists.");
				return false;

			}
			zkClient.create(ELECTION_NODE_2.concat("/").concat(clusterID).concat("/node"), nodeID, CreateMode.EPHEMERAL_SEQUENTIAL);				
                        
                        // HAES leader has nothing to do with ES master....
			// zkClient.writeData(HAES.concat("/").concat(clusterID).concat("/").concat(nodeID).concat("/").concat(ES_NODE_ISMASTER), state);



		} catch (Exception e) {
			e.printStackTrace();
			log.error("forceNodeAsMaster error message: " + e.getMessage());
			return false;
		}

		return true;

	}

	public boolean setClusterType(String clusterid, String type) {
		log.debug("setClusterType/parameters: {" + clusterid + ","+ type  + "}");
		try {
			zkClient.writeData(HAES.concat("/").concat(clusterid).concat("/").concat(HAES_CLUSTER_TYPE), type);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("setClusterType error message: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean setClusterAvailability(String clusterid, String availability) {
		log.debug("setClusterAvailability/parameters: {" + clusterid + ","+ availability  + "}");
		try {
			zkClient.writeData(HAES.concat("/").concat(clusterid).concat("/").concat(HAES_CLUSTER_AVAILABILITY), availability);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("setClusterAvailability error message: " + e.getMessage());
			return false;
		}

		return true;

	}
	@Override
	public String getClusterAvailability(String clusterid) {
		String availability = null;
		try {
			availability = zkClient.readData(HAES.concat("/").concat(clusterid).concat("/").concat(HAES_CLUSTER_AVAILABILITY));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("getClusterAvailability: " + e.getMessage());
			return null;
		}
		log.debug("getClusterAvailability: returning availability: " + availability);

		return availability;

	}

	@Override
	public boolean setNodeAsMasterRole(String ESclusterId, String ESNodeID, String role) {

		log.debug("setNodeAsMasterRole/parameters: {" + ESclusterId + ","+ ESNodeID + ","+ role  + "}");
		try {
			zkClient.writeData(HAES.concat("/").concat(ESclusterId).concat("/").concat(ESNodeID).concat("/").concat(ES_NODE_ISMASTER), role);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("setNodeAsMasterRole error message: " + e.getMessage());
			return false;
		}

		return true;
	}
        
        @Override
	public boolean amIanESMaster(String ESclusterId, String ESNodeID) {

		log.debug("amIanESMaster/parameters: {" + ESclusterId + ","+ ESNodeID + "}");
		String master_role = null;
		try {
			master_role = zkClient.readData(HAES.concat("/").concat(ESclusterId).concat("/").concat(ESNodeID).concat("/").concat(ES_NODE_ISMASTER));
			if (master_role != null) {
				if (master_role.contains("true")) return true;
			} else {
				log.error("amIanESMaster master_role is null.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("amIanESMaster error message: " + e.getMessage());
			return false;
		}

		return false;
	}

	@Override
	public boolean forceSnapshotFreezeStatus(String state) {
		log.debug("forceSnapshotFreezeStatus/parameters: {" + state  + "}");
		try {
			zkClient.writeData(HAES_SNAPSHOT_FREEZE, state);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("forceSnapshotFreezeStatus error message: " + e.getMessage());
			return false;
		}
		String message = "Daemon : " + this.getLocalESClusterID() + "/" + this.getLocalESNodeID() + " Snapshot Freeze processing has been forced to " + state;
		
		if (emailService.sendMail(message)) {
			log.debug("forceSnapshotFreezeStatus/sendMail succeeded");
		} else {
			log.error("forceSnapshotFreezeStatus/sendMail failed");
		}
		return true;
	}	

	@Override
	public boolean forceFailOverFreezeStatus(String state) {
		log.debug("forceFailOverFreezeStatus/parameters: {" + state  + "}");
		try {
			zkClient.writeData(HAES_FAILOVER_FREEZE, state);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("forceFailOverFreezeStatus error message: " + e.getMessage());
			return false;
		}
		String message = "Daemon : " + this.getLocalESClusterID() + "/" + this.getLocalESNodeID() + " FailOver Freeze processing has been forced to " + state;
		
		if (emailService.sendMail(message)) {
			log.debug("forceFailOverFreezeStatus/sendMail succeeded");
		} else {
			log.error("forceFailOverFreezeStatus/sendMail failed");
		}
		return true;
	}	
	
	@Override
	public boolean forceRestoreFreezeStatus(String state) {
		log.debug("forceSnapshotFreezeStatus/parameters: {" + state  + "}");
		try {
			zkClient.writeData(HAES_RESTORE_FREEZE, state);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("forceRestoreFreezeStatus error message: " + e.getMessage());
			return false;
		}
		String message = "Daemon : " + this.getLocalESClusterID() + "/" + this.getLocalESNodeID() + " Restore Freeze processing has been forced to " + state;
		
		if (emailService.sendMail(message)) {
			log.debug("forceRestoreFreezeStatus/sendMail succeeded");
		} else {
			log.error("forceRestoreFreezeStatus/sendMail failed");
		}

		return true;
	}
	@Override
	public String getSnapshotFreezeStatus() {
		String state = null;
		try {
			state = zkClient.readData(HAES_SNAPSHOT_FREEZE);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("getSnapshotFreezeStatus: " + e.getMessage());
			return null;
		}
		log.info("getSnapshotFreezeStatus: returning state: " + state);

		return state;

	}

	@Override
	public String getFailoverFreezeStatus() {
		String state = null;
		try {
			state = zkClient.readData(HAES_FAILOVER_FREEZE);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("getFailoverFreezeStatus: " + e.getMessage());
			return null;
		}
		log.info("getFailoverFreezeStatus: returning state: " + state);

		return state;

	}        
	@Override
	public String getFailoverStatus() {
		String state = null;
		try {
			state = zkClient.readData(HAES_FAILOVER_STATUS);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("getFailoverStatus: " + e.getMessage());
			return null;
		}
		return state;
	}
        
	@Override
	public void setFailoverStatus(String failoverStatus) {
		log.debug("setFailoverStatus/parameters: {" + failoverStatus  + "}");
		try {
			zkClient.writeData(HAES_FAILOVER_STATUS, failoverStatus);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("setFailoverStatus error message: " + e.getMessage());
		}
	}
        

	@Override
	public String getRestoreFreezeStatus() {
		String state = null;
		try {
			state = zkClient.readData(HAES_RESTORE_FREEZE);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("getRestoreFreezeStatus: " + e.getMessage());
			return null;
		}
		log.info("getRestoreFreezeStatus: returning state: " + state);

		return state;

	}

    @Override
    public boolean isLocalClusterRunningRestore() {
        if (this.getESclusterType(this.getLocalESClusterID()).contentEquals("passive")) {
            String childNode = HAES_RESTORE.concat("/").concat(ES_NODE).concat("/").concat("ID");
            if (zkClient.exists(childNode)) {
                return true;
            }
        }
        return false;
    }
            	
}
