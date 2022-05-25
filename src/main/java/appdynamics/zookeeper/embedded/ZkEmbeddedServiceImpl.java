package appdynamics.zookeeper.embedded;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.DatadirCleanupManager;
import org.apache.zookeeper.server.PurgeTxnLog;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ErrorHandler;
import org.springframework.util.SocketUtils;

import appdynamics.zookeeper.monitor.util.StringSerializer;
import appdynamics.zookeeper.monitor.api.ZkEmbeddedService;
import appdynamics.zookeeper.monitor.configuration.ESCluster1Properties;
import appdynamics.zookeeper.monitor.configuration.ESCluster2Properties;
import appdynamics.zookeeper.monitor.configuration.LocalProperties;
import appdynamics.zookeeper.monitor.configuration.ZkProperties;
import lombok.extern.slf4j.Slf4j;



@Slf4j
// dedicated spring boot default thread created with @Async
// this thread should be handled when spring boot shuts down
//@Async
public class ZkEmbeddedServiceImpl extends QuorumPeer implements ZkEmbeddedService  {

	/*
	 * need check if task executor needed: https://dzone.com/articles/spring-and-threads-taskexecutor
	 * 
	 * https://line.github.io/centraldogma/xref/com/linecorp/centraldogma/server/internal/replication/EmbeddedZooKeeper.html
	 */
	private String zkConf;

	// autowired for zkcfgproperties don't work at the time zookeeper is launched!
	// ideally find  a solution...
	@Autowired
	private  ZkProperties zkProperties;

	private Properties zkcfgproperties = new Properties();
	File dataDirfile;
	File dataLogDirfile;

	@Autowired
	private ESCluster1Properties escluster1Properties;
	@Autowired
	private ESCluster2Properties escluster2Properties;
	@Autowired
	private LocalProperties localProperties;


	/**
	 * ZooKeeper client port. This will be determined dynamically upon startup.
	 */
	private final int clientPort;

	/**
	 * {@link ErrorHandler} to be invoked if an Exception is thrown from the ZooKeeper server thread.
	 */
	private ErrorHandler errorHandler;

	private boolean daemon = true;

	private boolean isDistributed = true;

	//private QuorumPeerConfig quorumPeerConfig;
	private QuorumPeerMain qp;

	private ServerCnxnFactory cnxnFactory;
	private DatadirCleanupManager purgeManager;


	/** ZooKeeper default client port. */
	public static final int DEFAULT_ZOOKEEPER_CLIENT_PORT = 2181;

	/** ZooKeeper default init limit. */
	public static final int DEFAULT_ZOOKEEPER_INIT_LIMIT = 10;

	/** ZooKeeper default sync limit. */
	public static final int DEFAULT_ZOOKEEPER_SYNC_LIMIT = 5;

	/** ZooKeeper default peer port. */
	public static final int DEFAULT_ZOOKEEPER_PEER_PORT = 2888;

	/** ZooKeeper default leader port. */
	public static final int DEFAULT_ZOOKEEPER_LEADER_PORT = 3888;

	static final String SASL_SERVER_LOGIN_CONTEXT = "QuorumServer";
	static final String SASL_LEARNER_LOGIN_CONTEXT = "QuorumLearner";


	public ZkEmbeddedServiceImpl(String zkConf) throws IOException {

		log.info("Embedded Zookeeper zkConf parameter: " +zkConf);
		this.zkConf = zkConf;

		//log.info("ZkEmbeddedServiceImpl/creating configuration for zk. : " + zkProperties.toString());

		//log.info("ZkEmbeddedServiceImpl/escluster1Properties for zk. : " + escluster1Properties.toString());
		/**
		 * Construct an EmbeddedZooKeeper with a random port.
		 * we are forcing to 2181
		 */

		clientPort = SocketUtils.findAvailableTcpPort();

		/* 
		 * zookeeper start
		 * before context initialization (context at the main level. so later)
		 */

		if (config()) {
			log.info("Embedded Zookeeper startup...");
			start();
			log.info("Embedded Zookeeper is running? " + isRunning());
		} else {
			log.info("Embedded Zookeeper: there is configuration problem. " + isRunning());
		}


	}


	/**
	 * Construct an EmbeddedZooKeeper with the provided port.
	 *
	 * @param clientPort port for ZooKeeper server to bind to
	 * @param daemon     is daemon or not thread.
	 */
	public ZkEmbeddedServiceImpl(String zkConf, int clientPort, boolean daemon) throws IOException {
		this.clientPort = clientPort;
		this.daemon = daemon;

		log.info("Embedded Zookeeper zkConf parameter: " +zkConf);
		this.zkConf = zkConf;

		/* 
		 * zookeeper start
		 * before context initialization (context at the main level. so later)
		 */



		if (config()) {
			log.info("Embedded Zookeeper startup...");
			start();
			log.info("Embedded Zookeeper is running? " + isRunning());
		} else {
			log.info("Embedded Zookeeper: there is configuration problem. " + isRunning());
		}

		/*
		 * sleep for zk server to be up
		 */

		try {
			Thread.sleep(5 * 1000);
		} catch (InterruptedException e) {
			log.error("Embedded Zookeeper: thread sleep after start triggered problem:  {}" + e.getLocalizedMessage());
		}


		/* 
		 * control local access
		 */
		try {
			log.info("Embedded Zookeeper startup... control local access on 127.0.0.1:2181...");
			String zkNodes = "127.0.0.1:2181";
			waitForServerStartup(zkNodes);
		} catch (IOException e) {
			log.error("Embedded Zookeeper startup/waitForServerStartup failed: " + e.getLocalizedMessage());
		}
		/* 
		 * control access to all zknodes
		 */

		/*
		log.info("Embedded Zookeeper startup... control access to all zknodes...");

		String zkNodes = "";
		boolean first = true;
		Enumeration<?> ep = zkcfgproperties.propertyNames();
		while (ep.hasMoreElements()) {
			String key = (String) ep.nextElement();
			String value = zkcfgproperties.getProperty(key);

			if (key.startsWith("server."))	{
				String[] parts = value.split(":");
				if (first) {
					zkNodes = zkNodes.concat(parts[0]).concat(":").concat("2181");	
					first = false;
				} else {
					zkNodes = zkNodes.concat(",").concat(parts[0]).concat(":").concat("2181");
				}
			}
		}

		log.info("checking zookeeper contact to : " + zkNodes);
		try {
			waitForServerStartup(zkNodes);
		} catch (IOException e) {
			log.error("Embedded Zookeeper startup/waitForServerStartup failed: " + e.getLocalizedMessage());
		}
		 */

		/* 
		 * control access to all InetAddress zknodes
		 */

		List<String> zkNodesInetAddress = new ArrayList<String>();
		Enumeration<?> epInetAddress = zkcfgproperties.propertyNames();
		while (epInetAddress.hasMoreElements()) {
			String key = (String) epInetAddress.nextElement();
			String value = zkcfgproperties.getProperty(key);	
			if (key.startsWith("server."))	{
				String[] parts = value.split(":");
				zkNodesInetAddress.add(new String(parts[0]));
			}
		}

		try {
			log.info("Embedded Zookeeper startup... control access to all InetAddress zknodes...");

			CheckZooKeeperNodesInetAddress(zkNodesInetAddress);
		} catch (Exception e) {		
			log.error("Embedded Zookeeper startup/CheckZooKeeperNodesInetAddress failed: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Returns the port that clients should use to connect to this embedded server.
	 *
	 * @return dynamically determined client port
	 */
	@Override
	public int getClientPort() {
		return this.clientPort;
	}


	private void waitForServerStartup(String zkNodes) throws IOException {
		final AtomicBoolean connected = new AtomicBoolean(false);
		final ZooKeeper zk = new ZooKeeper(zkNodes, 30000, new Watcher() {
			@Override
			public void process(WatchedEvent watchedEvent) {
				log.info("Embedded Zookeeper startup/waitForServerStartup waiting on connection for type: " + watchedEvent.getType() + "state: " + watchedEvent.getState());

				if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
					connected.set(true);
				}
			}
		});
		try {
			long start = System.currentTimeMillis();
			while (!connected.get() && (System.currentTimeMillis() - start) < 30000) {}
			if (!connected.get()) {
				log.info("Embedded Zookeeper startup/waitForServerStartup not connected yet... ");

				throw new RuntimeException("Embedded Zookeeper startup/waitForServerStartup failed to connect to zookeeper");

			}
		} finally {
			try {
				log.info("Embedded Zookeeper startup/waitForServerStartup end of check, closing zk. ");
				zk.close(1000);
				//zk.close();
			} catch (InterruptedException e) {
				log.info("Embedded Zookeeper startup/waitForServerStartup InterruptedException occurted: " + e.getLocalizedMessage());
				//e.printStackTrace();
			}
		}
	}

	/**
	 * Provide an {@link ErrorHandler} to be invoked if an Exception is thrown from the ZooKeeper server thread. If none
	 * is provided, only error-level logging will occur.
	 *
	 * @param errorHandler the {@link ErrorHandler} to be invoked
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}


	public void testconnection() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.error("testconnection/ Exception {}", e.getLocalizedMessage());

		}

		ZkClient zkClient = null;		
		zkClient = new ZkClient("127.0.0.1:".concat(String.valueOf(getClientPort())), 15000, 6000, new StringSerializer());

		String value = "";
		if (!zkClient.exists("/titi")) zkClient.create("/titi", "tata", CreateMode.PERSISTENT);
		value = zkClient.readData("/titi", true);
		log.info("valeur de titi: " + value);

	}
	public static boolean CheckZooKeeperNodesInetAddress(List<String> zkNodes) throws Exception {
		boolean result = false;
		int port = 2181;
		// An attempt will be made to resolve the hostname into an InetAddress. 
		// If that attempt fails, the address will be flagged as unresolved.
		for(String node : zkNodes){
			InetSocketAddress nameNodeAddr = new InetSocketAddress(node, port);
			if (nameNodeAddr.isUnresolved()) {
				result = false;
				log.error("CheckZooKeeperNodesInetAddress/ Error for:" + nameNodeAddr.getHostString());
			}
		}

		return result;
	}
	/**
	 * Sets required zkcfgproperties to reasonable defaults and logs it.
	 * 
	 * NOT USED. later to add
	 */
	private static void setRequiredProperties(Properties zkProps) {
		// Set default client port
		if (zkProps.getProperty("clientPort") == null) {
			zkProps.setProperty("clientPort", String.valueOf(DEFAULT_ZOOKEEPER_CLIENT_PORT));

			log.warn("No 'clientPort' configured. Set to '{}'.", DEFAULT_ZOOKEEPER_CLIENT_PORT);
		}

		// Set default init limit
		if (zkProps.getProperty("initLimit") == null) {
			zkProps.setProperty("initLimit", String.valueOf(DEFAULT_ZOOKEEPER_INIT_LIMIT));

			log.warn("No 'initLimit' configured. Set to '{}'.", DEFAULT_ZOOKEEPER_INIT_LIMIT);
		}

		// Set default sync limit
		if (zkProps.getProperty("syncLimit") == null) {
			zkProps.setProperty("syncLimit", String.valueOf(DEFAULT_ZOOKEEPER_SYNC_LIMIT));

			log.warn("No 'syncLimit' configured. Set to '{}'.", DEFAULT_ZOOKEEPER_SYNC_LIMIT);
		}

		// Set default data dir
		if (zkProps.getProperty("dataDir") == null) {
			String dataDir = String.format("%s/%s/zookeeper",
					System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());

			zkProps.setProperty("dataDir", dataDir);

			log.warn("No 'dataDir' configured. Set to '{}'.", dataDir);
		}

		int peerPort = DEFAULT_ZOOKEEPER_PEER_PORT;
		int leaderPort = DEFAULT_ZOOKEEPER_LEADER_PORT;

		// Set peer and leader ports if none given, because ZooKeeper complains if multiple
		// servers are configured, but no ports are given.
		for (Map.Entry<Object, Object> entry : zkProps.entrySet()) {
			String key = (String) entry.getKey();

			if (entry.getKey().toString().startsWith("server.")) {
				String value = (String) entry.getValue();
				String[] parts = value.split(":");

				if (parts.length == 1) {
					String address = String.format("%s:%d:%d", parts[0], peerPort, leaderPort);
					zkProps.setProperty(key, address);
					log.info("Set peer and leader port of '{}': '{}' => '{}'.",
							key, value, address);
				}
				else if (parts.length == 2) {
					String address = String.format("%s:%d:%d",
							parts[0], Integer.valueOf(parts[1]), leaderPort);
					zkProps.setProperty(key, address);
					log.info("Set peer port of '{}': '{}' => '{}'.", key, value, address);
				}
			}
		}
	}
	@Override
	public void haesstop() {
		log.info("Embedded Zookeeper stop request executing...");
		shutdown();
		log.info("Embedded Zookeeper stop request finished.");

	}
	public boolean config() {		

		//log.info("ZkEmbeddedServiceImpl/creating configuration for zk. : " + zkProperties.toString());
		String zkmyid = null;
		String dataDir = null;
		boolean write_myid_file_ok =  false;

		zkcfgproperties.setProperty("clientPort", String.valueOf(clientPort));
		zkcfgproperties.setProperty("admin.enableServer", "false");
		zkcfgproperties.setProperty("maxClientCnxns", "30");
		zkcfgproperties.setProperty("maxSessionTimeout", "120000");
		zkcfgproperties.setProperty("minSessionTimeout", "60000");
		// forced for haes. but still can put the option in zoo.cfg
		zkcfgproperties.setProperty("standaloneEnabled", "false");

		// nodes not communicating... don't use it: dangerous
		//zkcfgproperties.setProperty("quorumListenOnAllIPs", "true");

		try (InputStream input = new FileInputStream(zkConf)) {

			Properties prop = new Properties();

			// load a zkcfgproperties file
			prop.load(input);

			Enumeration<?> e = prop.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = prop.getProperty(key);
				//System.out.println("Key : " + key + ", Value : " + value);
				if (key.contentEquals("dynamic")) {
					/*
					 * dynamics option must NOT be used: creates a problem not finding the myid file of zk yet
					 */
					if (value.contentEquals("yes")) {
						log.info("Using Dynamic zookeeper configuration.");
						/*
						 * if using dynamic files, we use this
						 */

						dataDirfile = new File(System.getProperty("java.io.tmpdir")
								+ File.separator + UUID.randomUUID());
						dataDirfile.deleteOnExit();
						zkcfgproperties.setProperty("dataDir", dataDirfile.getAbsolutePath());

						dataLogDirfile = new File(System.getProperty("java.io.tmpdir")
								+ File.separator + UUID.randomUUID());
						dataLogDirfile.deleteOnExit();
						zkcfgproperties.setProperty("dataLogDir", dataLogDirfile.getAbsolutePath());

					} else {
						log.info("Using Static zookeeper configuration.");
					}
				}
				//this should be always false for haes
				if (key.contentEquals("standaloneEnabled")) zkcfgproperties.setProperty("standaloneEnabled", value.trim());
				if (key.contentEquals("initLimit")) zkcfgproperties.setProperty("initLimit", value.trim());
				if (key.contentEquals("syncLimit")) zkcfgproperties.setProperty("syncLimit", value.trim());
				if (key.contentEquals("tickTime")) zkcfgproperties.setProperty("tickTime", value.trim());

				/*
				 * servers defined in static zkcfgproperties file
				 */
				if (key.startsWith("server.")) {
					zkcfgproperties.setProperty(key, value.trim());
					/*
					if (value.startsWith("0.0.0.0")) {
						String[] parts = key.split("\\.");
						zkmyid = parts[1];
					}
					 */
				}
				if (key.startsWith("lclserver")) zkmyid = value;

				if (key.contentEquals("maxClientCnxns")) zkcfgproperties.setProperty("maxClientCnxns", value.trim());

				if (key.contentEquals("dataDir")) {
					zkcfgproperties.setProperty("dataDir", value.trim());
					dataDir = value.trim();
				}
				if (key.contentEquals("dataLogDir")) zkcfgproperties.setProperty("dataLogDir", value.trim());

			}	 
			log.info("zookeeper configuration:");
			Enumeration<?> ep = zkcfgproperties.propertyNames();
			while (ep.hasMoreElements()) {
				String key = (String) ep.nextElement();
				String value = zkcfgproperties.getProperty(key);

				log.info("zookeeper configuration: " + key + " " + value);
			}

			/*
			 * initiate the quorum
			 */

			QuorumPeerConfig zkCfg = new QuorumPeerConfig();
			try {
				zkCfg.parseProperties(zkcfgproperties);
			} catch (ConfigException e1) {
				log.error("zk configuration/properties parsing error: {}", e1.getLocalizedMessage());
				return false;
			}

			try {
				zkCfg.checkValidity();
			} catch (ConfigException ec) {
				log.error("zk configuration/properties configuration error: {}", ec.getLocalizedMessage());
				return false;
			}

			log.info("zk configuration/Check: {}", zkCfg.getDataDir());


			cnxnFactory = ServerCnxnFactory.createFactory();
			cnxnFactory.configure(zkCfg.getClientPortAddress(), zkCfg.getMaxClientCnxns());



			setTxnFactory(new FileTxnSnapLog(zkCfg.getDataLogDir(), zkCfg.getDataDir()));
			enableLocalSessions(zkCfg.areLocalSessionsEnabled());
			enableLocalSessionsUpgrading(zkCfg.isLocalSessionsUpgradingEnabled());
			setElectionType(zkCfg.getElectionAlg());
			setMyid(zkCfg.getServerId());
			setTickTime(zkCfg.getTickTime());
			setMinSessionTimeout(zkCfg.getMinSessionTimeout());
			setMaxSessionTimeout(zkCfg.getMaxSessionTimeout());
			setInitLimit(zkCfg.getInitLimit());
			setSyncLimit(zkCfg.getSyncLimit());
			setConfigFileName(zkCfg.getConfigFilename());
			setZKDatabase(new ZKDatabase(getTxnFactory()));
			setQuorumVerifier(zkCfg.getQuorumVerifier(), false);
			if (zkCfg.getLastSeenQuorumVerifier() != null) {
				setLastSeenQuorumVerifier(zkCfg.getLastSeenQuorumVerifier(), false);
			}
			initConfigInZKDatabase();
			setCnxnFactory(cnxnFactory);
			setLearnerType(zkCfg.getPeerType());
			setSyncEnabled(zkCfg.getSyncEnabled());
			setQuorumListenOnAllIPs(zkCfg.getQuorumListenOnAllIPs());

			if ((dataDir != null) && (zkmyid != null)) {
				// The myid file needs to be written before creating the instance. Otherwise, this
				// will fail
				write_myid_file_ok =  writeMyidFile(dataDir, zkmyid);
			} else log.error("creation of local zk myid file in datadir failed: datadir or id is null.");

			//configureSasl();

			purgeManager = new DatadirCleanupManager(zkCfg.getDataDir(), zkCfg.getDataLogDir(),
					zkCfg.getSnapRetainCount(), zkCfg.getPurgeInterval());



		} catch (IOException e) {
			if (errorHandler != null) {
				errorHandler.handleError(e);
			} else {
				log.error("Exception running embedded ZooKeeper {}", e);
			}
		}
		return write_myid_file_ok;
	}

	boolean writeMyidFile(String dataDir, String zkmyid) {
		if (Files.isDirectory(Paths.get(dataDir))) {
			FileWriter fw= null;
			File file =null;
			try {
				file=new File(dataDir.concat("/myid"));
				if(!file.exists()) {
					file.createNewFile();
				}
				fw = new FileWriter(file);
				/*
				 * replace what was previously there
				 */
				fw.write(zkmyid);
				fw.flush();
				fw.close();
				log.info("writeMyidFile/ myid file has been created/updated successfully: " + file.getAbsolutePath() + " with value: " + zkmyid);
			} catch (IOException e) {
				log.error("writeMyidFile/ myid file has not been created/updated: " + file.getAbsolutePath() + "with value: " + zkmyid);
				log.error("writeMyidFile/ Exception writing MyidFile {}", e.getLocalizedMessage());
				return false;
			}
		} else {
			log.info("writeMyidFile/ the data directory does not exist: " + dataDir);
			return false;
		}
		return true;	    
	}

	private void configureSasl() {
		quorumServerSaslAuthRequired = true;
		quorumLearnerSaslAuthRequired = true;
		quorumServerLoginContext = SASL_SERVER_LOGIN_CONTEXT;
		quorumLearnerLoginContext = SASL_LEARNER_LOGIN_CONTEXT;
	}
	private void purgeTxnLogs() {
		log.info("Purging old ZooKeeper snapshots and logs ..");
		try {
			PurgeTxnLog.purge(purgeManager.getDataLogDir(),
					purgeManager.getSnapDir(),
					purgeManager.getSnapRetainCount());
			log.info("Purged old ZooKeeper snapshots and logs.");
		} catch (IOException e) {
			log.error("Failed to purge old ZooKeeper snapshots and logs: {}", e);
		}
	}
	@Override
	public synchronized void start() {

		purgeTxnLogs();
		purgeManager.start();
		super.start();
	}
	/**
	 * Shutdown the ZooKeeper server.
	 */

	@Override
	public void shutdown() {
		// Close the network stack first so that the shutdown process is done quickly.
		cnxnFactory.shutdown();
		purgeManager.shutdown();
		
		super.shutdown();
	
	}
}
