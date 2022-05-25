package appdynamics.zookeeper.monitor.api;

import java.util.ArrayList;
import java.util.List;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.apache.zookeeper.data.Stat;

/** @author "Bikas Katwal" 26/03/19 
/*
 * all the mechanic to maintain live nodes for the 2 clusters here
 * the maintenance is done per cluster, only requests for snapshot or restore need a common view , but per cluster
 * 
 * detail:
 * we need to ensure one snapshot is executed by one live master node of the active cluster
 * one live master node need to be elected in the active cluster
 * so live_nodes represent live running ES nodes (as ephemeral, they will be removed by zk when dying)
 * as 2 masters is minimum per cluster, the least sequential is elected (until dying, where the next in sequence is elected)
 * 
 * the same applies for ensuring one restore is done by a live master
 * 
 * all ES nodes per cluster will be live nodes: reason being to be able to advertize admins that one haes daemon is down.
 * 
 * the electForMaster is giving the master of snapshot or restore
 */
public interface ZkService {

  String getLeaderNodeData(String ESClusterID);

  void electForMaster(String ESClusterID, String ESNodeID);

  boolean masterExists(String ESClusterID);

  void addToLiveNodes(String ESClusterID, String nodeName, String data);

  List<String> getLiveNodes(String ESClusterID);

  void addToAllNodes(String ESClusterID,String nodeName, String data);

  List<String> getAllNodes(String ESClusterID);

  void deleteNodeFromCluster(String ESClusterID, String node);

  void createAllParentNodes(String ESClusterID);

  String getLeaderNodeData2(String ESClusterID);

  String getZNodeData(String path);
  
  public Stat getZNodeStat(String path);

  void createNodeInElectionZnode(String ESClusterID, String data);

  void registerChildrenChangeWatcher(String ESClusterID, String path, IZkChildListener iZkChildListener);

  void registerZkSessionStateListener(String ESClusterID, IZkStateListener iZkStateListener);

boolean checkLiveNodeExist(String esClusterIDFromIP, String esNodeIDFromIP);

void registerOtherClusterChildrenChangeWatcher(String ESClusterID, String path, IZkChildListener iZkChildListener);

ArrayList<ArrayList<String>>  getAllZKNodes();

void closeConnection();

}
