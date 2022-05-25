package appdynamics.zookeeper.monitor;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import appdynamics.zookeeper.monitor.configuration.ESCluster1Properties;
import appdynamics.zookeeper.monitor.configuration.ESCluster2Properties;
import appdynamics.zookeeper.monitor.configuration.EShealthcheckProperties;
import appdynamics.zookeeper.monitor.configuration.HaesHAProperties;
import appdynamics.zookeeper.monitor.configuration.HaesNodetypeProperties;
import appdynamics.zookeeper.monitor.configuration.LocalProperties;
import appdynamics.zookeeper.monitor.configuration.MailSenderProperties;
import appdynamics.zookeeper.monitor.configuration.SRScriptsProperties;
import appdynamics.zookeeper.monitor.configuration.ZkProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** @author "Marc Pichon" 08/10/20 */
/*
 * Create and Import Spring Boot Project
There are many ways to create a Spring Boot application. 
The simplest way is to use Spring Initializr at http://start.spring.io/, which is an online Spring Boot application generator.
 */
@SpringBootApplication
@EnableAutoConfiguration

/*
 * forcing only properties scan in sub package named configuration
 */
//@ConfigurationPropertiesScan("appdynamics.zookeeper.monitor.configuration")
@EnableScheduling
@Slf4j
//@EnableAsync the springboot schedulers is already async
//@EnableAsync the failover and failback are not part of scheduler, so we need it for those actions
//@EnableAsync the embedded zookeeper needs ThreadPoolTaskExecutor instead of it's own thread, so we need it 
@EnableAsync
public class Application implements CommandLineRunner {
	// reading properties files
	@Autowired
	private EShealthcheckProperties eshealthcheckProperties;
	@Autowired
	private ESCluster1Properties escluster1Properties;
	@Autowired
	private ESCluster2Properties escluster2Properties;
	/*
	 * test purpose. allows for dev env to connect to remote zk and ES node
	 */
	@Autowired
	private LocalProperties localProperties;
	@Autowired
	private SRScriptsProperties srScriptsProperties;
	@Autowired
	private  MailSenderProperties mailSenderProperties;
	@Autowired
	private  ZkProperties zkProperties;
	@Autowired
	private  HaesHAProperties haesHAProperties;
	@Autowired
	private  HaesNodetypeProperties haesNodetypeProperties;

	
	@Override

	public void run(String... args) {
        for(String arg:args) {
            System.out.println("MAIN ARGS: " + arg);
        }

		
		/* 
		 * zookeeper start
		 * before context initialization (context at the main level. so later)
		 */
		//ezk = new EmbeddedZooKeeper();
		//log.info("Embedded Zookeeper Configuration...");
		//ezk.config();
		//log.info("Embedded Zookeeper startup...");
		//ezk.start();
		//log.info("Embedded Zookeeper is running? " + ezk.isRunning());
		
		log.info(escluster1Properties.toString());
		log.info(escluster2Properties.toString());
		log.info(localProperties.toString());
		log.info(eshealthcheckProperties.toString());
		log.info(srScriptsProperties.toString());
		log.info(mailSenderProperties.toString());
		log.info(zkProperties.toString());
		log.info(haesHAProperties.toString());
		log.info(haesNodetypeProperties.toString());
	}

	public static void main(String[] args) {
		//SpringApplication.run(Application.class, args);
        
		ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
		// not used. handled manual shutdown thru controller. ctx.registerShutdownHook();
		
	}
}
