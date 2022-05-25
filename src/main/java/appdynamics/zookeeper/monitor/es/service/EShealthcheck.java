package appdynamics.zookeeper.monitor.es.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import appdynamics.zookeeper.monitor.model.SnapshotsList;

public interface EShealthcheck {
	
	boolean esNodeHealthcheckRestcalls() throws JsonProcessingException;
	void esHealthcheckRestcalls() throws JsonProcessingException;
	void esMasterNodesRestcall();
	boolean esHealthcheckClusterRestcalls() throws JsonProcessingException;
	void update_escheckstatus(boolean erstatus);
	SnapshotsList esgetSnapshotsList() throws JsonProcessingException;
	String getDiagnostic();
        void esHealthCheckCluster1LastCheck();
}
