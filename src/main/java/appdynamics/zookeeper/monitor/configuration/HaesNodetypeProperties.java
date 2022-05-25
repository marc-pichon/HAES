package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("haes.node")
public class HaesNodetypeProperties {
	
	private String type;

	public String getType() {
		return type;
	}
	public void setType(String nodetype) {
		this.type = nodetype;
	}

}
