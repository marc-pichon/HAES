package appdynamics.zookeeper.monitor.zkwatchers;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ELECTION_NODE_2;

import java.util.Collections;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.springframework.beans.factory.annotation.Autowired;

import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;

/** @author "Marc Pichon" 08/10/20 */
@Slf4j
@Setter
public class MasterChangeListenerClusterApproach2 implements IZkChildListener {

	private ZkService zkService;
	@Autowired private ESZkService eszkService;

	/**
	 * listens for deletion of sequential znode under /election znode and updates the
	 * clusterinfo
	 *
	 * @param parentPath
	 * @param currentChildren
	 */
	@Override
	public void handleChildChange(String parentPath, List<String> currentChildren) {
		log.info("MasterChangeListenerClusterApproach2 paramaters: parentPath=" + parentPath);
		log.info("MasterChangeListenerClusterApproach2 paramaters: currentChildren=" + currentChildren);
		if (parentPath == null) log.error("MasterChangeListenerClusterApproach2 : parentPath parameter is null" );
		if (currentChildren == null) 	  log.info("MasterChangeListenerClusterApproach2 : currentChildren parameter is null");
		if ((currentChildren != null)) {
			if (currentChildren.isEmpty()) {
                            // No leader for this cluster, forcing current node to become leader
                            // eszkService.forceNodeAsMaster(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID());
                            
                            // Finally must be sure to 
				// throw new RuntimeException("No node exists to select master!!");
			} else {
				//get least sequenced znode
				Collections.sort(currentChildren);
				String masterZNode = currentChildren.get(0);

				// once znode is fetched, fetch the znode data to get the hostname of new leader
				String masterNode = zkService.getZNodeData(ELECTION_NODE_2.concat("/").concat(masterZNode));
				log.info("new master is: {}", masterNode);
			}
		}

	}
}
