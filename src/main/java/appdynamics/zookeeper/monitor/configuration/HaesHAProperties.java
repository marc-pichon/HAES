package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("haesfail")
public class HaesHAProperties {
	public HaesHAFailOverProperties getFailover() {
		return failover;
	}

	public void setFailover(HaesHAFailOverProperties failover) {
		this.failover = failover;
	}

	public HaesHAFailBackProperties getFailback() {
		return failback;
	}

	public void setFailback(HaesHAFailBackProperties failback) {
		this.failback = failback;
	}

	private HaesHAFailOverProperties failover;
	private HaesHAFailBackProperties failback;

	@Override
	public String toString() {
		return "haes failover/failback properties{" +
				"failover synced=" + failover.getsynced() +
				" failback synced=" + failback.getsynced() +
				'}';
	}	
}


