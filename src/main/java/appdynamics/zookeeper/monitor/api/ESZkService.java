package appdynamics.zookeeper.monitor.api;

/** @author "Bikas Katwal" 26/03/19 */
public interface ESZkService {
	/*
	 * init values in zk HAES tree from application.properties
	 */
	boolean initNodes();
	boolean setClusterType(String clusterid, String type);
	boolean deleteZkTree();

	void initLocalNode(String clusterID, String nodeID);

	/*
	 * for controller to get info on LB request
	 */
	String getESclusterIdFromNode(String ipport);

	String getLocalESClusterID();
	String getLocalESNodeID();
        boolean isLocalClusterRunningRestore();
	String getESclusterType(String ESclusterId);
	String getNodeStatus(String ESclusterId, String ipport);
	boolean   setNodeStatus(String status);
	boolean   setNodeAsMasterRole(String ESclusterId, String ESNodeID, String role);
	boolean setClusterAvailability(String clusterid, String availability);
	String getClusterAvailability(String clusterid);

	boolean forceNodeStatus(String ESClusterID, String ESNodeID, String status);
	String getESNodeIDFromIP(String ip);
	String getESClusterIDFromIP(String ip);
	boolean amIanESMaster(String ESclusterId, String ESNodeID);
	boolean forceNodeAsMaster(String clusterid, String nodeid);
	boolean forceSnapshotFreezeStatus(String state);
	boolean forceRestoreFreezeStatus(String state);
	String getSnapshotFreezeStatus();
	String getRestoreFreezeStatus();
	boolean forceFailOverFreezeStatus(String state);
	String getFailoverFreezeStatus();
	String getFailoverStatus();
        void setFailoverStatus(String failoverStatus);
	void closeConnection();

}
