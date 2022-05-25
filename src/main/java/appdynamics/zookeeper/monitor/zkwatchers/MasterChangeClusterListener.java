package appdynamics.zookeeper.monitor.zkwatchers;

import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.springframework.beans.factory.annotation.Autowired;

import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;


/** @author "Marc Pichon" 08/10/20 */
@Setter
@Slf4j
public class MasterChangeClusterListener implements IZkChildListener {

  private ZkService zkService;
  @Autowired private ESZkService eszkService;
  /**
   * listens for creation/deletion of znode "master" under /election znode and updates the
   * clusterinfo
   *
   * @param parentPath
   * @param currentChildren
   */
  @Override
  public void handleChildChange(String parentPath, List<String> currentChildren) {
    if (currentChildren.isEmpty()) {
      log.info("master deleted, recreating master!");
      try {
        zkService.electForMaster(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID());
      } catch (ZkNodeExistsException e) {
        log.info("master already created");
      }
    } else {
      String leaderNode = zkService.getLeaderNodeData(eszkService.getLocalESClusterID());
      log.info("updating new master: {}", leaderNode);
    }
  }
}
