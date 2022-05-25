package appdynamics.zookeeper.monitor.zkwatchers;


import org.I0Itec.zkclient.IZkDataListener;
import org.springframework.beans.factory.annotation.Autowired;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
/** @author "Marc Pichon" 08/10/20 */
@Slf4j
@Setter
public class ESClusterAvailabilityChangeListener implements IZkDataListener {
	
	@Autowired private ESZkService eszkService;
	@Autowired private ESSnapshotRestoreService esSnapshotRestoreService;
	@Autowired private ZkService zkService;
        public static long lastCluster1StatusCheckUpdate = System.currentTimeMillis();
	
	@Override
	public void handleDataDeleted(String arg0) throws Exception {}
	
    @Override
    public void handleDataChange(String path, Object data) throws Exception {
        // Le maitre du cluster2 prend en charge le failover auto si besoin
        if (eszkService.getLocalESClusterID().equals("CLUSTER2") && zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID())) {
            if (data == null) {
                log.error("ESClusterAvailabilityChangeListener: availability is null for path: ".concat(path));
                return;
            }

            log.info("ESClusterAvailabilityChangeListener: new cluster availability status is: ".concat(data.toString()).concat(" for path: ").concat(path));
            /*
		 * only CLUSTER1 can trigger automatic failover
		 * if data = ko it means cluster availability is ko = cluster is RED.
             */
            log.info("ESClusterAvailabilityChangeListener:  path: ".concat(path));
            log.info("ESClusterAvailabilityChangeListener:  data: ".concat(data.toString()));
            log.info("ESClusterAvailabilityChangeListener:  fail over occurs if cluster = CLUSTER1 and data = ko ");
            // log.info("doFailOver/handleDataChange:  fail over will not occur: not implmented here. handled with healthchcking the ES cluster at scheduler level");

            // Le cluster1 a ete detecte KO
            if (path.contains("CLUSTER1") && data.toString().contentEquals("ko")) {
                log.info("ESClusterAvailabilityChangeListener:  fail over has been triggered for execution");

                CompletableFuture<Boolean> rc_ok_cf = esSnapshotRestoreService.doFailOver();
                boolean rc_ok = false;
                try {
                    rc_ok = rc_ok_cf.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("ESClusterAvailabilityChangeListener:  fail over return code action execution control failed");
                    rc_ok = false;
                }

            } else {
                // Cluster1 update as a haes node ping : 
                lastCluster1StatusCheckUpdate = System.currentTimeMillis();
            }
        }
    }
}
