package appdynamics.zookeeper.monitor.api;

/** @author "mpichon" 26/10/20 */
public interface ZkEmbeddedService {

	void haesstop();

	int getClientPort();

}
