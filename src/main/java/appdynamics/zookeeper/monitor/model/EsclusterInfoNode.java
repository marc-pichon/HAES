package appdynamics.zookeeper.monitor.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** @author "Bikas Katwal" 26/03/19 */
@Getter
@Setter
@AllArgsConstructor
public class EsclusterInfoNode {

  private String id;
  private String name;
  private boolean status;
  private String status_detail;
 
}
