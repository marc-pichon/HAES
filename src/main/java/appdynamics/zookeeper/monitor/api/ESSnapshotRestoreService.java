package appdynamics.zookeeper.monitor.api;

import java.util.concurrent.CompletableFuture;

import org.I0Itec.zkclient.IZkDataListener;

public interface ESSnapshotRestoreService {

	CompletableFuture<Boolean>  doFailOver();

	CompletableFuture<Boolean>  doFailBack();
	
	/*
	 * this listener looks at cluster availability state
	 * when state goes ko (aka RED cluster), then doFailOver() executes automatically
	 */

	void registerESClusterAvailabilityChangeWatcher(String path, IZkDataListener iZkDataListener);

	boolean doRestore();

	boolean doSnapShot();

	boolean doRsync(String ESClusterID, String ESNodeID);

	boolean doCheckRepository();

	boolean doRestoreTo(String snapshotid);

}
