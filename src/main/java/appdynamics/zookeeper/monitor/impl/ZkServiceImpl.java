package appdynamics.zookeeper.monitor.impl;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ALL_NODES;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ELECTION_MASTER;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ELECTION_NODE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ELECTION_NODE_2;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.LIVE_NODES;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER1;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.springframework.beans.factory.annotation.Autowired;

import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;
import appdynamics.zookeeper.monitor.util.ZkHaesUtil;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER1;
import org.I0Itec.zkclient.IZkDataListener;
import org.apache.zookeeper.data.Stat;

/** @author "Marc Pichon" 08/10/20 */
@Slf4j
public class ZkServiceImpl implements ZkService {

	private ZkClient zkClient;
	@Autowired 
	private ESZkService eszkService;
	//@Autowired
	//private ZkEmbeddedService zkEmbeddedService;

	public ZkServiceImpl(String hostPort) {
		try {
			
			//zkClient = new ZkClient(hostPort, 60000, 30000, new StringSerializer());
			// using same connection everywhere
			zkClient = ZkHaesUtil.getZkClientInstance(hostPort);
			
		}
		catch (Exception e) {
			if (e instanceof KeeperException.OperationTimeoutException) {
				log.error("connection timeout to Zookeeper");
			}
			log.error("connection error to Zookeeper: " + e.getLocalizedMessage());
			throw e;
		}

	}
        @Override
	public void closeConnection() {
		zkClient.close();
	}

	@Override	
	public ArrayList<ArrayList<String>> getAllZKNodes() {
		
		List<String> allZKNodesList = new ArrayList<String>();	
		ArrayList<ArrayList<String>> allZKNodesListWithData = new ArrayList<ArrayList<String>>();
		
		getChildrenZKNodes("/", allZKNodesList);	
		Collections.sort(allZKNodesList);
		/*
		 * addition: get the zk data for all the nodes
		 */
		
		for (int i = 0; i < allZKNodesList.size(); i++) {
			Object data = zkClient.readData(allZKNodesList.get(i));
			if (data != null) {
				ArrayList<String> localarray = new ArrayList<String>();	
				localarray.add(allZKNodesList.get(i));
				localarray.add(data.toString());
				allZKNodesListWithData.add(localarray);
			}
		}
		log.debug("getAllZKNodes result: {}", allZKNodesListWithData.toString());
		return allZKNodesListWithData;
	}
	private void getChildrenZKNodes(String path, List<String> allZKNodesList) {
		/*
		 * recursively call of subnodes
		 */
		List<String> list =  zkClient.getChildren(path);
		for (int i = 0; i < list.size(); i++) {
			log.debug("getChildrenZKNodes list elt: {}", list.get(i));
			if (!path.contentEquals("/")) {
				allZKNodesList.add(path.concat("/").concat(list.get(i)));
				getChildrenZKNodes(path.concat("/").concat(list.get(i)), allZKNodesList);
			} else {
				allZKNodesList.add(path.concat(list.get(i)));
				log.debug("getChildrenZKNodes recursive call with path: , {}", path.concat(list.get(i)));
				getChildrenZKNodes(path.concat(list.get(i)), allZKNodesList);				
			}
		}
	}
	
	
	
	@Override
	public String getLeaderNodeData(String ESClusterID) {
		return zkClient.readData(ELECTION_MASTER.concat("/").concat(ESClusterID), true);
	}

	@Override
	public void electForMaster(String ESClusterID, String ESNodeID) {
		if (!zkClient.exists(ELECTION_NODE.concat("/").concat(ESClusterID))) {
			zkClient.create(ELECTION_MASTER.concat("/").concat(ESClusterID), "election node", CreateMode.PERSISTENT);
		}
		try {
			zkClient.create(
					ELECTION_MASTER.concat("/").concat(ESClusterID),
					//getHostPostOfServer(),
					ESNodeID,
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
		} catch (ZkNodeExistsException e) {
			log.error("Master already created!!, {}", e);
		}
	}

	@Override
	public boolean masterExists(String ESClusterID) {
		return zkClient.exists(ELECTION_MASTER.concat("/").concat(ESClusterID));
	}

	@Override
	public void addToLiveNodes(String ESClusterID, String nodeName, String data) {
		log.info("addToLiveNodes with parameters: ESClusterID: " + ESClusterID + " nodeName: " + nodeName);

		if (!zkClient.exists(LIVE_NODES.concat("/").concat(ESClusterID))) {
			zkClient.create(LIVE_NODES.concat("/").concat(ESClusterID), "all live nodes are displayed here", CreateMode.PERSISTENT);
		}
		String childNode = LIVE_NODES.concat("/").concat(ESClusterID).concat("/").concat(nodeName);
                
                // Changing strategy due to cause explained at https://blog.box.com/a-gotcha-when-using-zookeeper-ephemeral-nodes
		/* if (!zkClient.exists(childNode)) {
			zkClient.create(childNode, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			log.info("addToLiveNodes " + childNode + " does not exists and is created");
		} else {
                    log.info("addToLiveNodes " + childNode + " exists and is not created");
                } */
                if (zkClient.exists(childNode)) {
                    zkClient.delete(childNode);
                    log.info("addToLiveNodes " + childNode + " old session still exists. Removing it before zookeeper remove it by itself.");
                }
                zkClient.create(childNode, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
	}

	@Override
	public List<String> getLiveNodes(String ESClusterID) {
		if (!zkClient.exists(LIVE_NODES.concat("/").concat(ESClusterID))) {
			throw new RuntimeException("No node /liveNodes exists");
		}
		return zkClient.getChildren(LIVE_NODES.concat("/").concat(ESClusterID));
	}

	
	@Override
	public boolean checkLiveNodeExist(String ESClusterID, String ESNodeID) {
		if (zkClient.exists(LIVE_NODES.concat("/").concat(ESClusterID).concat("/").concat(ESNodeID))) {
			return true;
		}
		return false;
	}
	
	@Override
	public void addToAllNodes(String ESClusterID,String nodeName, String data) {
		log.info("addToAllNodes parameters : {" + ESClusterID + ", " + nodeName  + ", " + data + "}");
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}
		if (!zkClient.exists(ALL_NODES.concat("/").concat(ESClusterID))) {
			zkClient.create(ALL_NODES.concat("/").concat(ESClusterID), "all nodes are displayed here", CreateMode.PERSISTENT);
		}
		String childNode = ALL_NODES.concat("/").concat(ESClusterID).concat("/").concat(nodeName);
		if (zkClient.exists(childNode)) {
			return;
		}
		zkClient.create(childNode, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}

	@Override
	public List<String> getAllNodes(String ESClusterID) {
		if (!zkClient.exists(ALL_NODES.concat("/").concat(ESClusterID))) {
			throw new RuntimeException("No node /allNodes exists");
		}
		return zkClient.getChildren(ALL_NODES.concat("/").concat(ESClusterID));
	}

	@Override
	public void deleteNodeFromCluster(String ESClusterID, String node) {
		zkClient.delete(ALL_NODES.concat("/").concat(ESClusterID).concat("/").concat(node));
		zkClient.delete(LIVE_NODES.concat("/").concat(ESClusterID).concat("/").concat(node));
	}

	@Override
	public void createAllParentNodes(String ESClusterID) {
		if (!zkClient.exists(ALL_NODES)) {
			zkClient.create(ALL_NODES, "all nodes are displayed here", CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(LIVE_NODES)) {
			zkClient.create(LIVE_NODES, "all live nodes are displayed here", CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(ELECTION_NODE)) {
			zkClient.create(ELECTION_NODE, "election node displayed here", CreateMode.PERSISTENT);
		}  
		if (!zkClient.exists(ALL_NODES.concat("/").concat(ESClusterID))) {
			zkClient.create(ALL_NODES.concat("/").concat(ESClusterID), "all nodes are displayed here for ".concat(ESClusterID), CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(LIVE_NODES.concat("/").concat(ESClusterID))) {
			zkClient.create(LIVE_NODES.concat("/").concat(ESClusterID), "all live nodes are displayed here for ".concat(ESClusterID), CreateMode.PERSISTENT);
		}
		if (!zkClient.exists(ELECTION_NODE.concat("/").concat(ESClusterID))) {
			zkClient.create(ELECTION_NODE.concat("/").concat(ESClusterID), "election node for ".concat(ESClusterID), CreateMode.PERSISTENT);
		}
	}

    @Override
    public String getLeaderNodeData2(String ESClusterID) {

        if (!zkClient.exists(ELECTION_NODE_2.concat("/").concat(ESClusterID))) {
            throw new RuntimeException("No node /election2 exists");
        }
        List<String> nodesInElection = zkClient.getChildren(ELECTION_NODE_2.concat("/").concat(ESClusterID));
        /*
		 * there is high possibility current nodes not updated with checking scheduler of which ES nodes are ES masters.
		 * we need to check:
         */
        if (!nodesInElection.isEmpty()) {
            Collections.sort(nodesInElection);
            String masterZNode = nodesInElection.get(0);
            log.info("getLeaderNodeData2/master found :" + masterZNode);
            return getZNodeData(ELECTION_NODE_2.concat("/").concat(ESClusterID).concat("/").concat(masterZNode));
        } else {
            log.info("getLeaderNodeData2/no master found yet.");
            return null;
        }
    }

	@Override
	public String getZNodeData(String path) {
		return zkClient.readData(path, null);
	}

	@Override
	public Stat getZNodeStat(String path) {
		return zkClient.getAcl(path).getValue();
	}

	@Override
	public void createNodeInElectionZnode(String ESClusterID, String ESNodeID) {
		//log.info("createNodeInElectionZnode: parameters : {" + ESClusterID + "," + ESNodeID + "}");
		log.info("createNodeInElectionZnode: parameters ESClusterID: {}", ESClusterID);
		log.info("createNodeInElectionZnode: parameters ESNodeID: {}", ESNodeID);

		if (!zkClient.exists(ELECTION_NODE_2)) {
			zkClient.create(ELECTION_NODE_2, "election node", CreateMode.PERSISTENT);
		}

		if (!zkClient.exists(ELECTION_NODE_2.concat("/").concat(ESClusterID))) {
			zkClient.create(ELECTION_NODE_2.concat("/").concat(ESClusterID), "election node", CreateMode.PERSISTENT);
		}
		/*
		 * this node will be in election zone only if this is a master at ES level
		 */
                // OLF //  HAES leader is independant from ES master. Creating ephemeral sequential node anyway
		/* if (eszkService.amIanESMaster(ESClusterID, ESNodeID)) {
			log.info("createNodeInElectionZnode: this is an ES live master. elected : {" + ESClusterID + "," +  ESNodeID + "}");
			zkClient.create(ELECTION_NODE_2.concat("/").concat(ESClusterID).concat("/node"), ESNodeID, CreateMode.EPHEMERAL_SEQUENTIAL);
		} else {
			log.info("createNodeInElectionZnode: this is an not ES live master. not elected : {" + ESClusterID + "," +  ESNodeID + "}");
		} */
                log.info("createNodeInElectionZnode: create election candidate entry for : {" + ESClusterID + "," +  ESNodeID + "}");
                zkClient.create(ELECTION_NODE_2.concat("/").concat(ESClusterID).concat("/node"), ESNodeID, CreateMode.EPHEMERAL_SEQUENTIAL);
	}

	@Override
	public void registerChildrenChangeWatcher(String ESClusterID, String path, IZkChildListener iZkChildListener) {
		zkClient.subscribeChildChanges(path.concat("/").concat(ESClusterID), iZkChildListener);
	}

        @Override
	public void registerOtherClusterChildrenChangeWatcher(String ESClusterID, String path, IZkChildListener iZkChildListener) {
		/*
		 * we look at changes on the other cluster
		 * this allow later to feed ClusterInfo for this other cluster
		 */
		if (ESClusterID.contentEquals(CLUSTER1)) zkClient.subscribeChildChanges(path.concat("/").concat(CLUSTER2), iZkChildListener);
		if (ESClusterID.contentEquals(CLUSTER2)) zkClient.subscribeChildChanges(path.concat("/").concat(CLUSTER1), iZkChildListener);

	}

	@Override
	public void registerZkSessionStateListener(String ESClusterID, IZkStateListener iZkStateListener) {
		// session level no need to use ESClusterID
		zkClient.subscribeStateChanges(iZkStateListener);
	}

}
