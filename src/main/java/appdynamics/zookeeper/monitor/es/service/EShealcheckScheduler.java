package appdynamics.zookeeper.monitor.es.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import appdynamics.zookeeper.monitor.configuration.EShealthcheckProperties;
import appdynamics.zookeeper.monitor.configuration.HaesNodetypeProperties;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component

/*
 * Component annotation force scan of that class and instanciation. else class
 * not executed
 */
public class EShealcheckScheduler extends EShealthcheckImpl {
	// public EShealcheckScheduler(RestTemplateBuilder restTemplateBuilder, String
	// hostPort) {
	// super(restTemplateBuilder, hostPort);
	public EShealcheckScheduler(RestTemplateBuilder restTemplateBuilder) {
		super(restTemplateBuilder);

	}

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Autowired
	private HaesNodetypeProperties haesNodetypeProperties;

	/*
	 * @Scheduled(fixedRate = 2000) public void scheduleTaskWithFixedRate()
	 * {log.info("ScheduledTasks/result is: {}",
	 * dateTimeFormatter.format(LocalDateTime.now()));}
	 * 
	 * @Scheduled(fixedDelay = 2000) public void scheduleTaskWithFixedDelay()
	 * {log.info("ScheduledTasks/result is: {}",
	 * dateTimeFormatter.format(LocalDateTime.now()));}
	 * 
	 * @Scheduled(fixedRate = 2000, initialDelay = 5000) public void
	 * scheduleTaskWithInitialDelay() {log.info("ScheduledTasks/result is: {}",
	 * dateTimeFormatter.format(LocalDateTime.now()));}
	 */

	/*
	 * A fixedDelay task:
	 ** 
	 * @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}") A fixedRate
	 * task:
	 ** 
	 * @Scheduled(fixedRateString = "${fixedRate.in.milliseconds}") A cron
	 * expression based task:
	 ** 
	 * @Scheduled(cron = "${cron.expression}")
	 ** 
	 * <!-- Configure the scheduler --> <task:scheduler id="myScheduler"
	 * pool-size="10" />
	 ** 
	 * <!-- Configure parameters --> <task:scheduled-tasks scheduler="myScheduler">
	 ** <task:scheduled ref="beanA" method="methodA" fixed-delay="5000"
	 * initial-delay="1000" /> <task:scheduled ref="beanB" method="methodB"
	 ** fixed-rate="5000" /> <task:scheduled ref="beanC" method="methodC"
	 ** cron="* /5 * * * * MON-FRI" />
	 * 
	 * 
	 * /* every minute
	 */

	private EShealthcheckProperties eshealthcheckProperties;

	@Scheduled(cron = "${es.healthcheck.cron}")
	// @Scheduled(cron = "0 * * * * ?")

	public void scheduleTaskWithCronExpression() {
		if (!haesNodetypeProperties.getType().equals("arbitrator")) {

			log.info("ScheduledTasks/result is: {}", dateTimeFormatter.format(LocalDateTime.now()));
			log.info("Current Thread : {}", Thread.currentThread().getName());
			/*
			 * execute all local ES node healthchecks and updates zk just one scheduler task
			 * needed
			 */

			/*
			 * First, check node health (http://127.0.0.1:9080/_ping &&
			 * http://127.0.0.1:9081/_ping)
			 */
			boolean ConnectionC2 = esNodeHealthcheckRestcalls();

			/*
			 * then, check cluster availability (http://127.0.0.1:9200/_cluster/health)
			 */
			boolean ConnectionC1 = esHealthcheckClusterRestcalls();

			if (ConnectionC1) {
				/*
				 * then, check which nodes are master (http://127.0.0.1:9200/_snapshot)
				 */
				esRepositoryRestcall();

				/*
				 * then, check repository (http://127.0.0.1:9200/_cat/nodes)
				 */
				esMasterNodesRestcall();
			} else {
				log.info("Disabling esRepository and esMasterNodes tests due to EL HTTP Port connectivity issue");
			}

			if (ConnectionC2) {
				/*
				 * currently checking healthcheck for Event Service components and tasks only
				 * logging information in case of problems should be restricted on elected node
				 * only
				 */
				esHealthcheckRestcalls();
			} else {
				log.info(
						"Disabling healthcheck for Event Service components and tasks tests due to ES HTTP Port connectivity issue");
			}

			/*
			 * For cluster2 master only : check last cluster1 availability update...
			 */
			esHealthCheckCluster1LastCheck();

		}
	}

}
