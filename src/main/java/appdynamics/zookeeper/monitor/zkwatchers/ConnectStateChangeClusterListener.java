package appdynamics.zookeeper.monitor.zkwatchers;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.isEmpty;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkStateListener;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.springframework.web.client.RestTemplate;

import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;

/**
 * @author "Marc Pichon" 08/10/20
 */
@Slf4j
@Setter
public class ConnectStateChangeClusterListener implements IZkStateListener {

    private ZkService zkService;
    private ESZkService eszkService;
    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public void handleStateChanged(KeeperState state) throws Exception {
        log.info(state.name()); // 1. disconnected, 2. expired, 3. SyncConnected
    }

    @Override
    public void handleNewSession() throws Exception {
        log.info("ConnectStateChangeClusterListener: connected to zookeeper");

        // sync data from master
        //syncDataFromMaster();
        // add new znode to /live_nodes to make it live
        zkService.addToLiveNodes(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID(), "cluster node");

        // re try creating znode under /election
        // this is needed, if there is only one server in cluster
        String leaderElectionAlgo = System.getProperty("leader.algo");
        if (isEmpty(leaderElectionAlgo) || "2".equals(leaderElectionAlgo)) {
            log.info("ConnectStateChangeClusterListener: leaderElectionAlgo=2");
            zkService.createNodeInElectionZnode(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID());
        } else {
            log.info("ConnectStateChangeClusterListener: leaderElectionAlgo=1");
            if (!zkService.masterExists(eszkService.getLocalESClusterID())) {
                zkService.electForMaster(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID());
            }

        }

        /*
     * ES cluster
     * add configuration datas from application.properties (can be dev or prod depending on profile)
         */
        eszkService.initNodes();

    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {
        log.info("could not establish session");
    }

}
