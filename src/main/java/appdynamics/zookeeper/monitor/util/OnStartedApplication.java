package appdynamics.zookeeper.monitor.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import appdynamics.zookeeper.monitor.configuration.ZkProperties;
import lombok.extern.slf4j.Slf4j;

/** @author "Marc Pichon" 08/10/20 */
/*
 * not working...
 */
@Component
@Slf4j
public class OnStartedApplication implements ApplicationListener<ApplicationPreparedEvent> {
	@Autowired
	private  ZkProperties zkProperties;
	
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		log.info("OnStartedApplication/configuration for zk. : " + zkProperties.toString());

		
	}

  
}
