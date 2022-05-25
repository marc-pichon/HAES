package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties("cluster")
public class Cluster1TypeProperties {

    private String cluster1type;
 
 
    public String getcluster1type() {
        return cluster1type;
    }

    public void setcluster1type(String cluster1type) {
        this.cluster1type = cluster1type;
    }

   
    @Override
    public String toString() {
        return "{" +
                "cluster type='" + cluster1type + 
                '}';
    }

}

