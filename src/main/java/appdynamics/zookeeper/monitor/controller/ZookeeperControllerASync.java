package appdynamics.zookeeper.monitor.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkEmbeddedService;
import appdynamics.zookeeper.monitor.api.ZkService;
import appdynamics.zookeeper.monitor.configuration.MailSenderProperties;
import appdynamics.zookeeper.monitor.es.service.EShealthcheck;

import appdynamics.zookeeper.monitor.util.EmailService;
import lombok.extern.slf4j.Slf4j;

/** @author "Marc Pichon" 08/10/20 */
@Slf4j
@Async
@RestController
public class ZookeeperControllerASync implements ApplicationContextAware {

	@Autowired 
	private ZkService zkService;
	@Autowired 
	private ESZkService eszkService;
	@Autowired 
	private ESSnapshotRestoreService esSnapshotRestoreService;
	@Autowired 
	@Qualifier("eshealthcheckService")
	private EShealthcheck eshealthcheck;
	@Autowired 
	private EmailService emailService;
	@Autowired 
	private MailSenderProperties mailSenderProperties; 

	@Autowired 
	private ZkEmbeddedService zkEmbeddedService; 

	private RestTemplate restTemplate = new RestTemplate();

	private ApplicationContext context;

	private Long totalRequestNB = 0L;
	private Long totalNegativeRequestNB = 0L;
	private Long totalPositiveRequestNB = 0L;


	//HAES
	/*
	 * serves Round Robin LB request for one node
	 * if node is in active ES cluster and node healty, then HTTP RC=200 and response is "OK"
	 * if node is in passive ES cluster or node unhealthy, then HTTP RC=403 and response is "KO"
	 */


	/*
	 * to shutdown properly the application
	 */
	@GetMapping("/HAES/DaemonShutdown")
	public @ResponseBody ResponseEntity<String> shutdownContext(HttpServletRequest request) {
		String reason = "Information: nothing can be returned at that point";
		
		/*
		 * closing zk connections
		 */
		eszkService.closeConnection();
		zkService.closeConnection();
		
		/*
		 * shutting down zk
		 */
		zkEmbeddedService.haesstop();
		/*
		 * stop services
		 */
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		((ConfigurableApplicationContext) context).close();
		
		/*
		 * JVM shutdown hook handling
		 */

		int exitCode = SpringApplication.exit(context, new ExitCodeGenerator() {
			@Override
			public int getExitCode() {
				log.info("Exiting Application Context.");
				return 0;
			}
		});

		System.exit(exitCode);
		// never reached
		return new ResponseEntity<String>(reason.concat("an attempt to shutdown has been executed.") + " HTTP RC: " + HttpStatus.OK, HttpStatus.OK);

	}
	/*
	 * to shutdown properly the application
	 */
	@PostMapping("/HAES/PostDaemonShutdown")
	public void postShutdownContext(HttpServletRequest request) {
		/*
		 * stop embedded zk
		 * 19/11/20 temporarily and partially disabled as the shutdown of zookeeper does not work
		 * TO BE FIXED AT: ZkEmbeddedServiceImpl/stop
		 */
		/*
		 * closing zk connections
		 */
		eszkService.closeConnection();
		zkService.closeConnection();
		
		/*
		 * shutting down zk
		 */

		zkEmbeddedService.haesstop();
		/*
		 * stop services
		 */
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//((ConfigurableApplicationContext) context).getBean(requiredType)
		((ConfigurableApplicationContext) context).close();
		
		/*
		 * JVM shutdown hook handling
		 */

		int exitCode = SpringApplication.exit(context, new ExitCodeGenerator() {
			@Override
			public int getExitCode() {
				log.info("Exiting Application Context.");
				return 0;
			}
		});

		System.exit(exitCode);
	}
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;

	}

}
