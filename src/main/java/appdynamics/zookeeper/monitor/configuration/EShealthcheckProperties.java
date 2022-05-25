package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;



@Component
@ConfigurationProperties("es.healthcheck")
public class EShealthcheckProperties {

    private String cronexpression;
 
 
    public String getcron() {
        return cronexpression;
    }

    public void setcron(String cron) {
        this.cronexpression = cron;
    }

   
    @Override
    public String toString() {
        return "EShealthcheckProperties{" +
                "cronexpression='" + cronexpression + '\'' +
                '}';
    }

}

