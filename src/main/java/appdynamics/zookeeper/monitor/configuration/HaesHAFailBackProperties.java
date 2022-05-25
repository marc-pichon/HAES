package appdynamics.zookeeper.monitor.configuration;

import org.springframework.stereotype.Component;

@Component
public class HaesHAFailBackProperties {
	private String synced;

	public String getsynced() {
		return synced;
	}
	public void setsynced(String synced) {
		this.synced = synced;
	}
}
