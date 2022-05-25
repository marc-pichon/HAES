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
import java.util.ListIterator;
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
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ErrorHandler;
import org.springframework.util.SocketUtils;

import appdynamics.zookeeper.monitor.util.StringSerializer;
import appdynamics.zookeeper.monitor.configuration.ESCluster1Properties;
import appdynamics.zookeeper.monitor.configuration.ESCluster2Properties;
import appdynamics.zookeeper.monitor.configuration.LocalProperties;
import appdynamics.zookeeper.monitor.configuration.ZkProperties;
import appdynamics.zookeeper.monitor.configuration.ESCluster1Properties.Cluster1;
import appdynamics.zookeeper.monitor.configuration.ESCluster2Properties.Cluster2;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class ZkEmbedded extends QuorumPeer {

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

	private QuorumPeerConfig quorumPeerConfig;
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

	public ZkEmbedded(String zkConf) throws IOException {

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
	public ZkEmbedded(String zkConf, int clientPort, boolean daemon) throws IOException {
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

	/**
	 * Runnable implementation that starts the ZooKeeper server.
	 */
	/* ORIGIN
    private class ServerRunnable implements Runnable {

        @Override
        public void run() {
            try {
                Properties zkcfgproperties = new Properties();
                File file = new File(System.getProperty("java.io.tmpdir")
                    + File.separator + UUID.randomUUID());
                file.deleteOnExit();
                zkcfgproperties.setProperty("dataDir", file.getAbsolutePath());
                zkcfgproperties.setProperty("clientPort", String.valueOf(clientPort));

                QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
                quorumPeerConfig.parseProperties(zkcfgproperties);

                zkServer = new ZooKeeperServerMain();
                ServerConfig configuration = new ServerConfig();
                configuration.readFrom(quorumPeerConfig);

                zkServer.runFromConfig(configuration);
            } catch (Exception e) {
                if (errorHandler != null) {
                    errorHandler.handleError(e);
                } else {
                    log.error("Exception running embedded ZooKeeper", e);
                }
            }
        }

    }
	 */

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



			if ((dataDir != null) && (zkmyid != null)) {
				// The myid file needs to be written before creating the instance. Otherwise, this
				// will fail
				write_myid_file_ok =  writeMyidFile(dataDir, zkmyid);
			} else log.error("creation of local zk myid file in datadir failed: datadir or id is null.");


		} catch (IOException e) {
			if (errorHandler != null) {
				errorHandler.handleError(e);
			} else {
				log.error("Exception running embedded ZooKeeper {}", e);
			}
		}
		return write_myid_file_ok;
	}


	/**
	 * get zoo.cfg zkcfgproperties file.
	 */
	/*
	 * getting all zkcfgproperties from here does not work. so we need to keep zk conf file....
	 * once solved zk conf file will not be needed anymore
	 */
	public boolean configORIG() {
		log.info("creating configuration for zk.");
		log.info("creating configuration for zk. InitLimit: " + zkProperties.getInitLimit());

		zkcfgproperties.setProperty("clientPort", String.valueOf(clientPort));
		zkcfgproperties.setProperty("admin.enableServer", "false");
		zkcfgproperties.setProperty("initLimit", zkProperties.getInitLimit());
		zkcfgproperties.setProperty("syncLimit", zkProperties.getSyncLimit());
		zkcfgproperties.setProperty("tickTime", zkProperties.getTickTime());
		zkcfgproperties.setProperty("MaxCnxns", zkProperties.getMaxCnxns());
		zkcfgproperties.setProperty("DataDir", zkProperties.getDataDir());
		zkcfgproperties.setProperty("DataLogDir", zkProperties.getDataLogDir());

		log.info("ZOOKEEPER CONFIGURATION clientPort: " + String.valueOf(clientPort));
		log.info("ZOOKEEPER CONFIGURATION admin.enableServe: false");
		log.info("ZOOKEEPER CONFIGURATION initLimit: " + zkProperties.getInitLimit());
		log.info("ZOOKEEPER CONFIGURATION syncLimit: " + zkProperties.getSyncLimit());
		log.info("ZOOKEEPER CONFIGURATION tickTime: " + zkProperties.getTickTime());
		log.info("ZOOKEEPER CONFIGURATION MaxCnxns: " + zkProperties.getTickTime());
		log.info("ZOOKEEPER CONFIGURATION DataDir: " + zkProperties.getDataDir());
		log.info("ZOOKEEPER CONFIGURATION DataLogDir: " + zkProperties.getDataLogDir());

		/*
		 * get local zk node IP
		 */
		String loopbackIP = localProperties.getloopback();

		/*
		 * create zk nodes configuration
		 */
		log.info("HAES: Initiating ZK nodes of Cluster1.");
		List<Cluster1> lc1 = escluster1Properties.getCluster1();

		ListIterator<Cluster1> litr_nodes = null;
		litr_nodes = lc1.listIterator();
		if (litr_nodes == null) {
			log.error("nodes list unavailable. please check your configuration file");
			return false;
		}
		String configline;
		String zkmyid;
		boolean write_myid_file_ok =  false;

		while(litr_nodes.hasNext()){
			Integer current_indice = litr_nodes.nextIndex();
			Cluster1 node_detail = litr_nodes.next();
			log.info(node_detail.getIp().toString());

			/*
			 * server.1=0.0.0.0:2888:3888
			 * server.2=1.2.3.4:2888:3888
			 */
			if (loopbackIP.contentEquals(node_detail.getIp())) {
				zkcfgproperties.setProperty("server.".concat(Integer.toString(current_indice + 1)), "0.0.0.0".concat(":2888:3888"));
				configline = "server.".concat(Integer.toString(current_indice + 1)).concat("=").concat("0.0.0.0").concat(":2888:3888");
				zkmyid = Integer.toString(current_indice + 1);
				write_myid_file_ok =  writeMyidFile(zkProperties.getDataDir(), zkmyid);

			} else {
				zkcfgproperties.setProperty("server.".concat(Integer.toString(current_indice + 1)), node_detail.getIp().concat(":2888:3888"));
				configline = "server.".concat(Integer.toString(current_indice + 1)).concat("=").concat(node_detail.getIp()).concat(":2888:3888");				
			}
			log.info("ZOOKEEPER CONFIGURATION server: " + configline);


		}
		log.info("HAES: Initiating ZK nodes of Cluster2.");
		List<Cluster2> lc2 = escluster2Properties.getCluster2();


		ListIterator<Cluster2> litr2_nodes = null;
		litr2_nodes = lc2.listIterator();
		if (litr2_nodes == null) {
			log.error("nodes list unavailable. please check your configuration file");
			return false;
		}
		while(litr2_nodes.hasNext()){
			Integer current_indice = litr2_nodes.nextIndex();
			Cluster2 node_detail = litr2_nodes.next();
			log.info(node_detail.getIp().toString());

			if (loopbackIP.contentEquals(node_detail.getIp())) {
				zkcfgproperties.setProperty("server.".concat(Integer.toString(current_indice + 1)), "0.0.0.0".concat(":2888:3888"));
				configline = "server.".concat(Integer.toString(current_indice + 1)).concat("=").concat("0.0.0.0").concat(":2888:3888");
				zkmyid = Integer.toString(current_indice + 1);
				write_myid_file_ok =  writeMyidFile(zkProperties.getDataDir(), zkmyid);

			} else {
				zkcfgproperties.setProperty("server.".concat(Integer.toString(current_indice + 1)), node_detail.getIp().concat(":2888:3888"));
				configline = "server.".concat(Integer.toString(current_indice + 1)).concat("=").concat(node_detail.getIp()).concat(":2888:3888");				
			}
			log.info("ZOOKEEPER CONFIGURATION server: " + configline);

		}
		if (write_myid_file_ok) {
			return true;
		} else return false;
	}
	public void configOLD() {

		zkcfgproperties.setProperty("clientPort", String.valueOf(clientPort));
		zkcfgproperties.setProperty("admin.enableServer", "false");

		try (InputStream input = new FileInputStream("/Users/marc.pichon/APPD-Services/Customers/CNAV/HA_ES/embedded-zookeeper/zoo.cfg")) {

			Properties prop = new Properties();

			// load a zkcfgproperties file
			prop.load(input);

			Enumeration<?> e = prop.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = prop.getProperty(key);
				System.out.println("Key : " + key + ", Value : " + value);
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
				if (key.contentEquals("initLimit")) zkcfgproperties.setProperty("initLimit", value);
				if (key.contentEquals("syncLimit")) zkcfgproperties.setProperty("syncLimit", value);
				if (key.contentEquals("tickTime")) zkcfgproperties.setProperty("tickTime", value);

				/*
				 * servers defined in static zkcfgproperties file
				 */
				if (key.startsWith("server.")) zkcfgproperties.setProperty(key, value);

				if (key.startsWith("maxCnxns")) zkcfgproperties.setProperty("maxCnxns", value);

				if (key.contentEquals("dataDir")) zkcfgproperties.setProperty("dataDir", value);
				if (key.contentEquals("dataLogDir")) zkcfgproperties.setProperty("dataLogDir", value);

			}	            

		} catch (IOException e) {
			if (errorHandler != null) {
				errorHandler.handleError(e);
			} else {
				log.error("Exception running embedded ZooKeeper {}", e);
			}
		}

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

	boolean writeMyidFileToBeDone(String dataDir, String zkmyid) {

		// Check dataDir and create if necessary
		if (dataDir == null) {
			log.error("writeMyidFile/No dataDir configured.");
			return false;
		}

		File fdataDir = new File(dataDir);

		if (!fdataDir.isDirectory() && !fdataDir.mkdirs()) {
			log.warn("writeMyidFile/dataDir does not exist, and directory can nit be crearted.");
		}


		log.info("writeMyidFile/Writing {} to myid file in 'dataDir'.", zkmyid);

		// Write myid to file. We use a File Writer, because that properly propagates errors,
		// while the PrintWriter swallows errors
		try (FileWriter writer = new FileWriter(new File(dataDir, "myid"))) {
			writer.write(String.valueOf(zkmyid));
		}
		catch (IOException e) {
			log.error("writeMyidFile/ myid file has not been created/updated in: " + fdataDir + "with value: " + zkmyid);
			log.error("writeMyidFile/ Exception writing MyidFile {}", e.getLocalizedMessage());
			return false;
		} 
		return true;	    
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

	private static ServerCnxnFactory createCnxnFactory(QuorumPeerConfig zkCfg) throws IOException {
		final InetSocketAddress bindAddr = zkCfg.getClientPortAddress();
		final ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
		// Listen only on 127.0.0.1 because we do not want to expose ZooKeeper to others.
		cnxnFactory.configure(new InetSocketAddress("127.0.0.1", bindAddr != null ? bindAddr.getPort() : 0),
				zkCfg.getMaxClientCnxns());
		return cnxnFactory;
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
		//cnxnFactory.shutdown();
		purgeManager.shutdown();
		super.shutdown();
	}
}
