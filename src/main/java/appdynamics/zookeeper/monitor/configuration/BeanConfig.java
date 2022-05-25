package appdynamics.zookeeper.monitor.configuration;


import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PreDestroy;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;

import appdynamics.snapshotrestore.impl.ESSnapshotRestoreServiceImpl;
import appdynamics.zookeeper.embedded.ZkEmbeddedServiceImpl;
import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkEmbeddedService;
import appdynamics.zookeeper.monitor.api.ZkService;
import appdynamics.zookeeper.monitor.es.service.EShealthcheck;
import appdynamics.zookeeper.monitor.es.service.EShealthcheckImpl;
import appdynamics.zookeeper.monitor.impl.ESZkServiceImpl;
import appdynamics.zookeeper.monitor.impl.ZkServiceImpl;
import appdynamics.zookeeper.monitor.util.EmailService;
import appdynamics.zookeeper.monitor.util.ZkHaesUtil;
import appdynamics.zookeeper.monitor.zkwatchers.AllNodesChangeClusterListener;
import appdynamics.zookeeper.monitor.zkwatchers.ConnectStateChangeClusterListener;
import appdynamics.zookeeper.monitor.zkwatchers.ESClusterAvailabilityChangeListener;
import appdynamics.zookeeper.monitor.zkwatchers.LiveNodeChangeClusterListener;
import appdynamics.zookeeper.monitor.zkwatchers.LiveNodeChangeOtherClusterListener;
import appdynamics.zookeeper.monitor.zkwatchers.MasterChangeClusterListener;
import appdynamics.zookeeper.monitor.zkwatchers.MasterChangeListenerClusterApproach2;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.web.client.RestTemplateBuilder;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
/** @author "Marc Pichon" 08/10/20 */
@Configuration
public class BeanConfig {
	@Autowired 
	private EmailService emailService;
	
	/*
	 * embedded zookeeper for HAES management here
	 */
	@Bean(name = "zkEmbeddedService")
	@Scope("singleton")
	public ZkEmbeddedService zkEmbeddedService() throws Exception {
		String zkConf = System.getProperty("zk.conf");
		int port = 2181;
		// false: don't start zk as daemon
		return new ZkEmbeddedServiceImpl(zkConf, port, false);
	}	

	/*
	 * HAES zookeeper management here
	 */
	@Bean(name = "zkService")
	@Scope("singleton")
	@DependsOn({"zkEmbeddedService"})
	public ZkService zkService() throws Exception {
		String zkHostPort = System.getProperty("zk.url");
		return new ZkServiceImpl(zkHostPort);
	}	
	/*
	 * HAES activities here
	 */
	@Bean(name = "eszkService")
	@Scope("singleton")
	@DependsOn({"zkEmbeddedService"})
	public ESZkService eszkService() {
		String zkHostPort = System.getProperty("zk.url");
		return new ESZkServiceImpl(zkHostPort);
	}
	// 03/09 ajout en place de l'annotation @service direct sur EShealthcheckImpl
	@Bean(name = "eshealthcheckService")
	@Scope("singleton")
	@DependsOn({"zkEmbeddedService"})
	//@Primary
	public EShealthcheck eshealthcheck() {
		return new EShealthcheckImpl(new RestTemplateBuilder());
	}
	@Bean(name = "esSnapshotRestoreService")
	@Scope("singleton")
	@DependsOn({"zkEmbeddedService"})
	public ESSnapshotRestoreService esSnapshotRestoreService() {
		String zkHostPort = System.getProperty("zk.url");
		return new ESSnapshotRestoreServiceImpl(zkHostPort);
	}
	@Bean(name = "allNodesChangeListener")
	@Scope("singleton")
	public IZkChildListener allNodesChangeListener() {
		return new AllNodesChangeClusterListener();
	}

	@Bean(name = "liveNodeChangeClusterListener")
	@Scope("singleton")
	public IZkChildListener liveNodeChangeClusterListener() {
		return new LiveNodeChangeClusterListener();
	}

	@Bean(name = "liveNodeOtherClusterChangeListener")
	@Scope("singleton")
	public IZkChildListener liveNodeOtherClusterChangeListener() {
		return new LiveNodeChangeOtherClusterListener();
	}

	@Bean(name = "masterChangeListener")
	@ConditionalOnProperty(name = "leader.algo", havingValue = "1")
	@Scope("singleton")
	public IZkChildListener masterChangeListener() throws Exception {
		MasterChangeClusterListener masterChangeListener = new MasterChangeClusterListener();
		masterChangeListener.setZkService(zkService());
		return masterChangeListener;
	}

	// default (matchIfMissing=true)
	@Bean(name = "masterChangeListener")
	@ConditionalOnProperty(name = "leader.algo", havingValue = "2", matchIfMissing = true)
	@Scope("singleton")
	public IZkChildListener masterChangeListener2() throws Exception {
		MasterChangeListenerClusterApproach2 masterChangeListener = new MasterChangeListenerClusterApproach2();
		masterChangeListener.setZkService(zkService());
		return masterChangeListener;
	}

	@Bean(name = "connectStateChangeListener")
	@Scope("singleton")
	public IZkStateListener connectStateChangeListener() throws Exception {
		ConnectStateChangeClusterListener connectStateChangeListener = new ConnectStateChangeClusterListener();
		connectStateChangeListener.setZkService(zkService());
		return connectStateChangeListener;
	}

	@Bean(name = "esClusterAvailabilityChangeListener")
	@Scope("singleton")
	public IZkDataListener esClusterAvailabilityChangeListener() {
		ESClusterAvailabilityChangeListener esClusterAvailabilityChangeListener = new ESClusterAvailabilityChangeListener();
		//esClusterAvailabilityChangeListener.setZkService(zkService());
		return esClusterAvailabilityChangeListener;
	}
	@PreDestroy
	public void onShutDown() {
		System.out.println("haes daemon closing application context.");
		String ip = "unknown";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.error("onShutDown/getLocalHost: UnknownHostException " + e.getLocalizedMessage());
		}
		ZkHaesUtil.closeZkClientInstance();
		/*
		 * sending email
		 */
		// create circular injection problem... String message = "haes daemon shutdown. node information: " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + ", IP:" + ip;
		String hostname = "unknown";
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			log.error("onShutDown/getLocalHost: UnknownHostException " + e.getLocalizedMessage());
		}

		String message = "daemon shutdown for Node IP: " + ip + " + hostname: " + hostname;


		if (emailService.sendMail(message)) {
			log.debug("onShutDown/sendMail succeeded");
		} else {
			log.error("onShutDown/sendMail failed");
		}

	}
}
