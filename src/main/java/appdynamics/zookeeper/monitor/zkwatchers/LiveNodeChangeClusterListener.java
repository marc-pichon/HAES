package appdynamics.zookeeper.monitor.zkwatchers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.springframework.beans.factory.annotation.Autowired;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER1;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER2;


/** @author "Marc Pichon" 08/10/20 */
@Slf4j
public class LiveNodeChangeClusterListener implements IZkChildListener {

	/**
	 * - This method will be invoked for any change in /live_nodes children
	 * - During registering this listener make sure you register with path /live_nodes
	 * - after receiving notification it will update the local clusterInfo object
	 *
	 * @param parentPath this will be passed as /live_nodes
	 * @param currentChildren new list of children that are present in /live_nodes, children's string value is znode name which is set as server hostname
	 */
	@Autowired private ESZkService eszkService;
	@Autowired private ESSnapshotRestoreService esSnapshotRestoreService;
	@Autowired private ZkService zkService;

	@Override
	public void handleChildChange(String parentPath, List<String> currentChildren) {
		/*
		 * update current ClusterInfo with all available nodes
		 */
		log.info("LiveNodeChangeClusterListener: parameter parentPath: {}", parentPath);
		log.info("LiveNodeChangeClusterListener: parameter currentChildren: {}", currentChildren.toString());
		
		/*
		 * something happens for one live node
		 * check if some node disappeared
		 * if that node belongs to active cluster, we must check how much left nodes on the active cluster
		 * if number of still living node is more than one, then available nodes can answer the LB
		 * one, as number is important: we don't want multiple failover being triggered each time one live node goes down
		 *  if number of still living node is less than one, then we should trigger a failover
		 */
		/*
		 * get active cluster
		 */
		String active_cluster = null;
		if (eszkService.getESclusterType(CLUSTER1).contentEquals("active")) {
			active_cluster = CLUSTER1;
		} 
		if (eszkService.getESclusterType(CLUSTER2).contentEquals("active")) {
			active_cluster = CLUSTER2;
		}
		if (active_cluster == null) {
			log.error("LiveNodeChangeClusterListener: Unable to find an haes active cluster, no failover possible");
			return;
		}
		/* 
		 * find how much are left from now
		 * 
		 */
		if (currentChildren.size() > 0) {
			log.info("LiveNodeChangeClusterListener: at least one live node exist on active cluster. no failover will be triggered.");
			return;
		}
		/*
		 * should we failover?
		 * when no more live nodes, the master on passive cluster will execute failover.
		 * problem here is it will happen each time an admin action such as shutting down all live nodes on active cluster
		 * we need to find a solution here...
		 * 
		 * conditions: if size of live nodes = 0, active cluster1 is active, node is cluster2 and this node is the local cluster leader
		 * so here the leader of cluster2 will trigger the failover
		 */
		
		if ((currentChildren.size() == 0) && (active_cluster.contentEquals(CLUSTER1)) && (zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID()))) {
			log.info("LiveNodeChangeClusterListener: no more live node exist on active primary cluster. failover will be triggered by an elected node.");
			CompletableFuture<Boolean> rc_ok_cf = esSnapshotRestoreService.doFailOver();
			boolean rc_ok = false;
			try {
				rc_ok = rc_ok_cf.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("LiveNodeChangeClusterListener:  fail over return code action execution control failed: {}", e.getLocalizedMessage());
				rc_ok = false;
			}
		}
	}
}