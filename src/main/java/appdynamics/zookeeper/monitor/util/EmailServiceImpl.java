package appdynamics.zookeeper.monitor.util;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import appdynamics.zookeeper.monitor.configuration.MailSenderProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service("mailService")
public class EmailServiceImpl implements EmailService {
	@Autowired
	private JavaMailSender emailSender;
	@Autowired
	private MailSenderProperties mailSenderProperties;
        
        Map<String, Long> lastMails = new HashMap<>();

	public boolean sendMail(String message) {
		if (mailSenderProperties.getEnable()) {
			SimpleMailMessage mailMessage = new SimpleMailMessage();

			log.info("sendMail/parameter toEmail:" + mailSenderProperties.getUsername());
			log.info("sendMail/parameter subject: " + mailSenderProperties.getSubject());
			log.info("sendMail/parameter message: " + message);
			long now = System.currentTimeMillis();

			if (!lastMails.containsKey(message) || lastMails.containsKey(message)
					&& now - lastMails.get(message) > mailSenderProperties.getSendInterval() * 60000) {

				try {
					mailMessage.setFrom(mailSenderProperties.getFrom());
					mailMessage.setTo(mailSenderProperties.getUsername());
					mailMessage.setSubject(mailSenderProperties.getSubject());
					mailMessage.setText(message);

					emailSender.send(mailMessage);

					log.info("sendMail executed.");
					lastMails.put(message, System.currentTimeMillis());
					return true;

				} catch (Exception e) {
					log.info("EmailService/Sent message in eror using: " + mailSenderProperties.getUsername() + " "
							+ mailSenderProperties.getSubject() + " " + message);
					log.info("EmailService/Sent message eror detail: {}", e.getLocalizedMessage());
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean sendMailTest(String toEmail, String subject, String message) {	
		log.info("sendMail/parameter toEmail:" + toEmail);
		log.info("sendMail/parameter subject: " + subject);
		log.info("sendMail/parameter message: " + message);

		// Recipient's email ID needs to be mentioned.
		String to = "marcpichonkrav@gmail.com";//change accordingly

		// Sender's email ID needs to be mentioned
		String from = mailSenderProperties.getFrom();
		final String username = "";//change accordingly
		final String password = "";//change accordingly

		// Assuming you are sending email through relay.jangosmtp.net
		String host = "192.168.56.101";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "false");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "2500");
		props.put("mail.debug", "true");
		props.put("mail.smtp.debug", "true");

		// Get the Session object.
		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			Message messagetest = new MimeMessage(session);

			// Set From: header field of the header.
			messagetest.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			messagetest.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(to));

			// Set Subject: header field
			messagetest.setSubject("Testing Subject");

			// Now set the actual message
			messagetest.setText("Hello, this is sample for to check send "
					+ "email using JavaMailAPI ");

			// Send message
			Transport.send(messagetest);

			log.info("EmailService/Sent message successfully: {}", toEmail + " " + subject + " "  + message);
			return true;


		} catch (MessagingException e) {
			log.info("EmailService/Sent message in eror: {}", toEmail + " " + subject + " "  + message);
			log.info("EmailService/Sent message eror detail: {}", e.getLocalizedMessage());
			return false;
		}	
	}
	
	public boolean sendMailSpringBoot(String message) {	

		
		SimpleMailMessage mailMessage = new SimpleMailMessage();

		log.info("sendMail/parameter toEmail:" + mailSenderProperties.getUsername());
		log.info("sendMail/parameter subject: " + mailSenderProperties.getSubject());
		log.info("sendMail/parameter message: " + message);
		
		try {
			mailMessage.setFrom(mailSenderProperties.getFrom());
			mailMessage.setTo(mailSenderProperties.getUsername()); 
			mailMessage.setSubject(mailSenderProperties.getSubject()); 
			mailMessage.setText(message);
			
			
		    
			emailSender.send(mailMessage);

			log.info("sendMail executed.");
			return true;

		} catch (Exception e) {
			log.info("EmailService/Sent message in eror using: " + mailSenderProperties.getUsername() + " " + mailSenderProperties.getSubject() + " "  + message);
			log.info("EmailService/Sent message eror detail: {}", e.getLocalizedMessage());
			return false;
		}
		
	}
	
	public boolean sendMailtest(String toEmail, String subject, String message) {	

		log.info("EmailService/sending email: {}", toEmail + " " + subject + " "  + message);

		// Recipient's email ID needs to be mentioned.
		String to = "marcpichonkrav@gmail.com";//change accordingly

		// Sender's email ID needs to be mentioned
		String from = mailSenderProperties.getFrom();
		final String username = "";//change accordingly
		final String password = "";//change accordingly

		// Assuming you are sending email through relay.jangosmtp.net
		String host = "192.168.56.101";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "false");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "2500");
		props.put("mail.debug", "true");
		props.put("mail.smtp.debug", "true");

		// Get the Session object.
		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			Message messagetest = new MimeMessage(session);

			// Set From: header field of the header.
			messagetest.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			messagetest.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(to));

			// Set Subject: header field
			messagetest.setSubject("Testing Subject");

			// Now set the actual message
			messagetest.setText("Hello, this is sample for to check send "
					+ "email using JavaMailAPI ");

			// Send message
			Transport.send(messagetest);

			log.info("EmailService/Sent message successfully: {}", toEmail + " " + subject + " "  + message);
			return true;


		} catch (MessagingException e) {
			log.info("EmailService/Sent message in eror: {}", toEmail + " " + subject + " "  + message);
			log.info("EmailService/Sent message eror detail: {}", e.getLocalizedMessage());
			return false;
			//throw new RuntimeException(e);
		}
	}
	

	public boolean sendMailSampleGoogle(String toEmail, String subject, String message) {	

		SimpleMailMessage mailMessage = new SimpleMailMessage();

		log.info("EmailService/sending email: {}", toEmail + " " + subject + " "  + message);

		// Recipient's email ID needs to be mentioned.
		String to = "marcpichonkrav@gmail.com";//change accordingly

		// Sender's email ID needs to be mentioned
		String from = "marcpichonkrav@gmail.com";//change accordingly
		final String username = "marc";//change accordingly
		final String password = "WQAxszcde01!";//change accordingly

		// Assuming you are sending email through relay.jangosmtp.net
		String host = "smtp.gmail.com";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "587");

		// Get the Session object.
		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			Message messagetest = new MimeMessage(session);

			// Set From: header field of the header.
			messagetest.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			messagetest.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(to));

			// Set Subject: header field
			messagetest.setSubject("Testing Subject");

			// Now set the actual message
			messagetest.setText("Hello, this is sample for to check send "
					+ "email using JavaMailAPI ");

			// Send message
			Transport.send(messagetest);

			log.info("EmailService/Sent message successfully: {}", toEmail + " " + subject + " "  + message);
			return true;


		} catch (MessagingException e) {
			log.info("EmailService/Sent message in eror: {}", toEmail + " " + subject + " "  + message);
			log.info("EmailService/Sent message eror detail: {}", e.getLocalizedMessage());
			return false;
			//throw new RuntimeException(e);
		}
	}
	
	
}