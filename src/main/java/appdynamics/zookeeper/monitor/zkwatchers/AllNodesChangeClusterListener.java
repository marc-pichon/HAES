package appdynamics.zookeeper.monitor.zkwatchers;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.springframework.beans.factory.annotation.Autowired;

import appdynamics.zookeeper.monitor.api.ESZkService;

/** @author "Marc Pichon" 08/10/20 */
@Slf4j
public class AllNodesChangeClusterListener implements IZkChildListener {

  /**
   * - This method will be invoked for any change in /all_nodes children
   * - During registering this
   * listener make sure you register with path /all_nodes
   * - after receiving notification it will update the local clusterInfo object
   *
   * @param parentPath this will be passed as /all_nodes
   * @param currentChildren current list of children, children's string value is znode name which is
   *     set as server hostname
   */

	@Autowired  private ESZkService eszkService;

  @Override
  public void handleChildChange(String parentPath, List<String> currentChildren) {
    log.info("AllNodesChangeClusterListener: Current all node size: {}", currentChildren.size());

  }
}
