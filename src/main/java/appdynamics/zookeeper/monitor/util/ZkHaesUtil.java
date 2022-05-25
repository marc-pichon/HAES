package appdynamics.zookeeper.monitor.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher.Event.KeeperState;



/** @author "Marc Pichon" 08/10/20 */
public final class ZkHaesUtil  {

	public static final String ELECTION_MASTER = "/ELECTION/MASTER";
	public static final String ELECTION_NODE = "/ELECTION";
	public static final String ELECTION_NODE_2 = "/ELECTION2";
	public static final String LIVE_NODES = "/LIVENODES";
	public static final String ALL_NODES = "/ALLNODES";

	public static final String HAES = "/HAES";
	public static final String HAES_CLUSTER1 = "/HAES/CLUSTER1";
	public static final String HAES_CLUSTER2 = "/HAES/CLUSTER2";
	public static final String CLUSTER1 = "CLUSTER1";
	public static final String CLUSTER2 = "CLUSTER2";

	
	public static final String HAES_CLUSTER_TYPE = "type"; 
	public static final String HAES_CLUSTER_UP = "up"; 
	public static final String HAES_CLUSTER_AVAILABILITY = "availability"; 
	
	public static final String ES_NODE_PREFIX = "ES-NODE";
	
	public static final String ES_NODE_STATUS = "status"; 
	public static final String ES_NODE_SHARD_STATUS = "shard_status"; 
	public static final String ES_NODE_ISMASTER = "ismaster";
	
	public static final String ES_LOCAL_NODE = "notdefined";
	public static final String ES_NODE = "ES_NODE";
	
	public static final String HAES_RESTORE = "/RESTORE";
	public static final String HAES_SNAPSHOT = "/SNAPSHOT";
	public static final String HAES_FAILOVER = "/FAILOVER";
	public static final String HAES_SNAPSHOT_FREEZE = "/SNAPSHOT/FREEZE";
	public static final String HAES_RESTORE_FREEZE = "/RESTORE/FREEZE";
	public static final String HAES_FAILOVER_FREEZE = "/FAILOVER/FREEZE";
	public static final String HAES_FAILOVER_STATUS = "/FAILOVER/STATUS";
	public static final String SNAPSHOTID = "SNAPSHOTID";
	public static final String SNAPSHOTID_STATUS = "SNAPSHOTID_STATUS";

        public static final String FAILOVER_OR_FAILBACK_RUNNING_STATUS = "FAILOVER_OR_FAILBACK_RUNNING";
	public static final String FAILOVER_READY_TO_START_STATUS = "FAILOVER_READY_TO_START";
	public static final String FAILBACK_READY_TO_START_STATUS = "FAILBACK_READY_TO_START";
	public static final String FAILOVER_FAILED_STATUS = "FAILOVER_FAILED";
	public static final String FAILBACK_FAILED_STATUS = "FAILBACK_FAILED";


	private static String ipPort = null;
	
	private static ZkClient zkClient;
	private volatile KeeperState state = KeeperState.SyncConnected;

	public static String getHostPostOfServer() {
		if (ipPort != null) {
			return ipPort;
		}
		String ip;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new RuntimeException("failed to fetch Ip!", e);
		}
		int port = Integer.parseInt(System.getProperty("server.port"));
		ipPort = ip.concat(":").concat(String.valueOf(port));
		return ipPort;
	}

	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	private ZkHaesUtil() {
	}
	public static ZkClient getZkClientInstance(String hostPort) {
		if (zkClient == null)
			zkClient = new ZkClient(hostPort, 60000, 30000, new StringSerializer());
		
		zkClient.waitUntilConnected();
		
		return zkClient;
		
	}
	public static void closeZkClientInstance() {
		zkClient.close();		
	}
}
