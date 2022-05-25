package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
//We use @Configuration so that Spring creates a Spring bean in the application context
//@Configuration
@ConfigurationProperties("zk")
public class ZkProperties {
	private String dynamic;
	private String initLimit;
	private String syncLimit;
	private String tickTime;
	private String maxCnxns;
	private String dataDir;
	private String dataLogDir;
	
    public String getDynamic() {
		return dynamic;
	}

	public void setDynamic(String dynamic) {
		this.dynamic = dynamic;
	}

	public String getInitLimit() {
		return initLimit;
	}

	public void setInitLimit(String initLimit) {
		this.initLimit = initLimit;
	}

	public String getSyncLimit() {
		return syncLimit;
	}

	public void setSyncLimit(String syncLimit) {
		this.syncLimit = syncLimit;
	}

	public String getTickTime() {
		return tickTime;
	}

	public void setTickTime(String tickTime) {
		this.tickTime = tickTime;
	}

	public String getMaxCnxns() {
		return maxCnxns;
	}

	public void setMaxCnxns(String maxCnxns) {
		this.maxCnxns = maxCnxns;
	}

	public String getDataDir() {
		return dataDir;
	}

	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}

	public String getDataLogDir() {
		return dataLogDir;
	}

	public void setDataLogDir(String dataLogDir) {
		this.dataLogDir = dataLogDir;
	}

	@Override
    public String toString() {
        return "ZkProperties{" +
                "dynamic='" + dynamic +
                ", initLimit='" + initLimit +
                ", syncLimit='" + syncLimit +
                ", tickTime='" + tickTime +
                ", maxCnxns='" + maxCnxns +
                ", dataDir='" + dataDir +
                ", dataLogDir='" + dataLogDir +
                '}';
    }
}

