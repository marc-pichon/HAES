package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("haes.mailsender")
public class MailSenderProperties {
	private String username;
	private String from;
	private String subject;
    private Integer sendInterval;
    private Boolean enable;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Boolean getEnable() {
		return enable;
	}
	public void setEnable(Boolean enable) {
		this.enable = enable;
	}
	public Integer getSendInterval() {
		return sendInterval;
	}
	public void setSendInterval(Integer sendInterval) {
		this.sendInterval = sendInterval;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
    @Override
    public String toString() {
        return "MailSenderProperties{" +
                "username='" + username +
                "', from='" + from +
                "', subject='" + subject +
                "', sendInterval='" + sendInterval +
                "', enabled='" + enable +
                "'}";
    }	


}
