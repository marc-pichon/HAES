package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("esnode")
public class ESCluster1Properties {

    private List<Cluster1> cluster1 = new ArrayList<>();

    public static class Cluster1 {
        private String ip;
        private String port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "ESCluster1{" +
                    "ip='" + ip + '\'' +
                    ", port='" + port + '\'' +
                    '}';
        }
    }

    
 
    public List<Cluster1> getCluster1() {
        return cluster1;
    }

    public void setCluster1(List<Cluster1> escluster) {
        this.cluster1 = escluster;
    }

    @Override
    public String toString() {
        return "ESCluster1Properties{" +
                ", ES Cluster1=" + cluster1 +
                '}'
                ;
    }

}

