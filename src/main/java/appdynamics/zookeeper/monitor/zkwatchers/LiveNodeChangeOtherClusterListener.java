package appdynamics.zookeeper.monitor.zkwatchers;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER1;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER2;


/** @author "Marc Pichon" 08/10/20 */
@Slf4j
public class LiveNodeChangeOtherClusterListener implements IZkChildListener {

	/**
	 * - This method will be invoked for any change in /live_nodes children  on the other cluster
	 * - During registering this listener make sure you register with path /live_nodes
	 * - after receiving notification it will update the local clusterInfo object
	 *
	 * @param parentPath this will be passed as /live_nodes
	 * @param currentChildren new list of children that are present in /live_nodes, children's string value is znode name which is set as server hostname
	 */

	@Override
	public void handleChildChange(String parentPath, List<String> currentChildren) {
		/*
		 * update current ClusterInfo with all available nodes
		 * parentPath: /LIVENODES/CLUSTER(1|2)
		 */
		String[] clusterid = parentPath.split("/");
		log.info("handleChildChange/parameter parentPath: {}", parentPath);
		log.info("handleChildChange/parameter currentChildren: {}", currentChildren.toString());
		log.info("handleChildChange/parameter clusterid: {}", clusterid[2]);


	}
}