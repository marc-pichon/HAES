package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("local.ip")
public class LocalProperties {
	
	private String esnode;
	private String loopback;
	
    public String getesnode() {
        return esnode;
    }

    public void setesnode(String esnode) {
        this.esnode = esnode;
    }
    public String getloopback() {
        return loopback;
    }

    public void setloopback(String loopback) {
        this.loopback = loopback;
    }
    @Override
    public String toString() {
        return "LocalProperties{" +
                "esnode='" + esnode +
                ", loopback='" + loopback +
                '}';
    }
}
