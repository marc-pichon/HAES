package appdynamics.zookeeper.monitor.util;

import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.isEmpty;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER1;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_CLUSTER_AVAILABILITY;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER1;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.CLUSTER2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkEmbeddedService;
import appdynamics.zookeeper.monitor.api.ZkService;
import appdynamics.zookeeper.monitor.configuration.ESCluster1Properties;
import appdynamics.zookeeper.monitor.configuration.ESCluster2Properties;
import appdynamics.zookeeper.monitor.configuration.EShealthcheckProperties;
import appdynamics.zookeeper.monitor.configuration.HaesNodetypeProperties;
import appdynamics.zookeeper.monitor.configuration.LocalProperties;
import appdynamics.zookeeper.monitor.configuration.MailSenderProperties;
import appdynamics.zookeeper.monitor.configuration.SRScriptsProperties;
import appdynamics.zookeeper.monitor.configuration.ZkProperties;
import appdynamics.zookeeper.monitor.controller.exceptions.ConfigurationHaesException;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

/** @author "Marc Pichon" 08/10/20 */
@Component
@Slf4j
public class OnStartUpApplication implements ApplicationListener<ContextRefreshedEvent> {

  private RestTemplate restTemplate = new RestTemplate();
  @Autowired private ZkService zkService;
  /*
   * handles ES healthchecks updates to zk
   */
  @Autowired private ESZkService eszkService;
  

  //static EmbeddedZooKeeper ezk;
  
  /*
   * handles snapshot/restore states to zk
   */
  @Autowired private ESSnapshotRestoreService esSnapshotRestoreService;

  // HAES

  @Autowired private IZkDataListener esClusterAvailabilityChangeListener;
  
  //@Autowired private IZkDataListener ClusterTypeChangeListener;
  
  //@Autowired private IZkDataListener NodesMastersStatusChangeListener;
  
  //@Autowired private IZkDataListener NodesStatusChangeListener;
  
  //@Autowired private IZkDataListener NodesShardsStatusChangeListener;
  
  
  
  
  
	/*
	 * test purpose. allows for dev env to connect to remote zk and ES node
	 */
	@Autowired
	private LocalProperties localProperties;
	@Autowired 
	private EmailService emailService;
	@Autowired
	private HaesNodetypeProperties haesNodetypeProperties;


  @Override
  public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
    try {
		/*
		 * sending email
		 */
        String ip = "unknown";
        try {
          ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        	log.error("onApplicationEvent/getLocalHost: UnknownHostException " + e.getLocalizedMessage());
        }
		String hostname = InetAddress.getLocalHost().getHostName();

		String message = "Daemon : " + " started for Node IP: " + ip + " + hostname: " + hostname;
		
		if (emailService.sendMail(message)) {
			log.debug("onApplicationEvent/sendMail succeeded");
		} else {
			log.error("onApplicationEvent/sendMail failed");
		}

    	log.info(localProperties.toString());
        
        // Dumping zKTree when entering
        log.info("zkTree when starting HAES node : ");
        ArrayList<ArrayList<String>> zknodes = zkService.getAllZKNodes();
        if (zknodes != null) {
            for (int i = 0; i < zknodes.size(); i++) {
                log.info(zknodes.get(i).get(0)+" => "+zknodes.get(i).get(1));
            }
        }

        /*
         * init nodes from application.properties
         */
    	// create all persistent nodes for all healthchecks
        eszkService.initNodes();
        
        /*
         * this sleep must be activated
         */
  		try {
  			Thread.sleep(5000);
  		} catch (InterruptedException e) {
  		     log.error("onApplicationEvent: sleep sequence, " + e.getLocalizedMessage());
  		     log.debug("onApplicationEvent: sleep sequence, " + e.getStackTrace().toString());
  		}

      // create all parent nodes /ELECTION, /ALL_NODES, /LIVE_NODES for each of the 2 clusters
        /*
         * this service maintains real up and running haes daemons so that one master for each of the ES cluster can be elected
         */
      zkService.createAllParentNodes(CLUSTER1);
      zkService.createAllParentNodes(CLUSTER2);
      

      // add this server to cluster by creating znode under /all_nodes, with name as "host:port"
      
      if (eszkService.getLocalESClusterID() == null) {
    	  log.error("onApplicationEvent: There is very likely a configuration issue in the application.properties file.");
    	  log.error("onApplicationEvent: please review your IP definitions of nodes, ip local node and ip loopback node.");
			throw new ConfigurationHaesException("LocalESClusterID is null: There is very likely a configuration issue in the application.properties file.");

      }
      if (eszkService.getLocalESNodeID() == null) {
    	  log.error("onApplicationEvent: There is very likely a configuration issue in the application.properties file.");
    	  log.error("onApplicationEvent: please review your IP definitions of nodes, ip local node and ip loopback node.");
			throw new ConfigurationHaesException("LocalESNodeID is null: There is very likely a configuration issue in the application.properties file.");
  	  
      }
		
      zkService.addToAllNodes(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID(), "cluster node");
      /*
       * this sleep must be activated
       */
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		     log.error("onApplicationEvent: sleep sequence, " + e.getLocalizedMessage());
		     log.debug("onApplicationEvent: sleep sequence, " + e.getStackTrace().toString());
		}
			  
      // check which leader election algorithm(1 or 2) need is used
      String leaderElectionAlgo = System.getProperty("leader.algo");

      // if approach 2 - create ephemeral sequential znode in /election
      // then get children of  /election and fetch least sequenced znode, among children znodes
      
      /*
       * znode election is handled once the ES healthcheck is done.
       * only after this checking we know which are the es master nodes that can be elected.
       * eliminating here the need to create an election node just based on fact that one node is live or not.
       */
      
      if (!haesNodetypeProperties.getType().equals("arbitrator")) {
    	  if (isEmpty(leaderElectionAlgo) || "2".equals(leaderElectionAlgo)) {
    		  log.info("onApplicationEvent: leaderElectionAlgo = 2. " );
    		  log.info("onApplicationEvent: trying to  call createNodeInElectionZnode with parameters getLocalESClusterID: {}", eszkService.getLocalESClusterID());
    		  log.info("onApplicationEvent: trying to  call createNodeInElectionZnode with parameters getLocalESNodeID: {}", eszkService.getLocalESNodeID());

    		  zkService.createNodeInElectionZnode(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID());
    	  } else {
    		  log.info("onApplicationEvent: leaderElectionAlgo = 1. " );
    		  if (!zkService.masterExists(eszkService.getLocalESClusterID())) {
    			  zkService.electForMaster(eszkService.getLocalESClusterID(), eszkService.getLocalESNodeID());
    		  }
    	  }
      }
      

      // sync person data from master
      //syncDataFromMaster();

      // add child znode under /live_node, to tell other servers that this server is ready to serve
      // read request      

      zkService.addToLiveNodes(eszkService.getLocalESClusterID(),eszkService.getLocalESNodeID(), "live cluster node");
            
      
      // ClusterInfo.getClusterInfo().getLiveNodes(eszkService.getLocalESClusterID()).clear();
      

      /*
       * we add only local cluster (1 or2) cluster info at that moment.
       * ClusterInfo has all live nodes of local cluster
       * to get global view, then the OtherClusterliveNodeChangeListener will feed the second cluster
       * -not sure this is necessary work...
       */
      // OLF //  ClusterInfo useless
      /* ClusterInfo.getClusterInfo().getLiveNodes(eszkService.getLocalESClusterID()).addAll(zkService.getLiveNodes(eszkService.getLocalESClusterID()));

	  log.info("onApplicationEvent: Added Livenodes to Clusterinfo result: " + 
			  ClusterInfo.getClusterInfo().getLiveNodes(eszkService.getLocalESClusterID().toString())
      ); */

      
      // register watchers for leader change, live nodes change, all nodes change and zk session
      // state change
      
      // OLF // Useless. Election is automatic, first node in the sorted child list in /ELECTION2
      /* if (isEmpty(leaderElectionAlgo) || "2".equals(leaderElectionAlgo)) {
        zkService.registerChildrenChangeWatcher(eszkService.getLocalESClusterID(),ELECTION_NODE_2, masterChangeListener);
      } else {
        zkService.registerChildrenChangeWatcher(eszkService.getLocalESClusterID(),ELECTION_NODE, masterChangeListener);
      } */
      
      /*
       * this listener on the current cluster for its live nodes
       * example: cluster1
       */
      // OLF // zkService.registerChildrenChangeWatcher(eszkService.getLocalESClusterID(),LIVE_NODES, liveNodeChangeClusterListener);
      /*
       * this listener on the non current cluster for its live nodes
       * example: cluster2
       */
      // OLF // zkService.registerOtherClusterChildrenChangeWatcher(eszkService.getLocalESClusterID(),LIVE_NODES, liveNodeOtherClusterChangeListener);
 
      // OLF // Useless
      // zkService.registerChildrenChangeWatcher(eszkService.getLocalESClusterID(),ALL_NODES, allNodesChangeListener);
      // OLF // zkService.registerZkSessionStateListener(eszkService.getLocalESClusterID(),connectStateChangeListener);
      

     
      /*
       * HAES 
       */
      //sert a rien
      //eszkService.initLocalNode();
      
      /*
       * register watchers for all cluster or node changes
       */
      //eszkService.registerClusterAvailabilityChangeWatcher(path, ClusterAvailabilityChangeListener);
      //eszkService.registerClusterTypeChangeWatcher(path, ClusterTypeChangeListener);
      //eszkService.registerNodesMastersStatusChangeWatcher(path, NodesMastersStatusChangeListener);
      //eszkService.registerNodesStatusChangeWatcher(path, NodesStatusChangeListener);
      //eszkService.registerNodesShardsStatusChangeWatcher(path, NodesShardsStatusChangeListener);
      
      /*
       * register a watcher on cluster 1 availability.
       * if cluster 2 is no more available, then the listener should trigger failover
       * 
       * this event based design is not relevant and active at the moment
       */
      esSnapshotRestoreService.registerESClusterAvailabilityChangeWatcher(HAES_CLUSTER1.concat("/").concat(HAES_CLUSTER_AVAILABILITY), esClusterAvailabilityChangeListener);
      
      
      
    } catch (Exception e) {
      throw new RuntimeException("Startup failed!!", e);
    }
  }

}
