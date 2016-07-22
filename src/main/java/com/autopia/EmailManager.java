package com.autopia;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import uk.org.lidalia.slf4jext.Logger;
import uk.org.lidalia.slf4jext.LoggerFactory;

/**
 * Class to manage email message sending
 * @author vj
 */
public class EmailManager {
	
	private static final Logger logger = LoggerFactory.getLogger(EmailManager.class);
	private Boolean authRequired = false;
	private String username, password;
	
	private Properties properties;
	private MimeMessage message;
	private Multipart multipart;
	
	/**
	 * Default constructor for the {@link EmailManager} class.
	 * This causes the host to default to "localhost" and the port to 25.
	 */
	public EmailManager() {
		this("localhost", 25);
	}
	
	/**
	 * Constructor for the {@link EmailManager} class
	 * @param host The SMTP host to be used for sending email
	 * @param port The SMTP port to be used for sending email
	 */
	public EmailManager(String host, int port) {
		properties = new Properties();
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", port);
		
		Session session = Session.getInstance(properties);
		message = new MimeMessage(session);
		multipart = new MimeMultipart();
	}
	
	/**
	 * Function to enable or disable SSL while sending email
	 * @param sslEnable Boolean value which enables or disables SSL while sending email
	 */
	public void setSslEnable(Boolean sslEnable) {
		properties.put("mail.smtp.ssl.enable", sslEnable);
		
		logger.info("SSL enable set to " + sslEnable);
	}
	
	/**
	 * Function to set the authentication required while sending email
	 * @param username The username to be used while sending email
	 * @param password The password to be used while sending email
	 */
	public void setAuthentication(String username, String password) {
		this.authRequired = true;
		this.username = username;
		this.password = password;
		
		properties.put("mail.smtp.auth", true);
		
		logger.info("Mail authentication set successfully");
	}
	
	/**
	 * Function to compose the email as required
	 * @param from The FROM address
	 * @param to The TO address
	 * @param subject The email subject
	 * @param body The email body
	 */
	public void composeMail(String from, String to, String subject, String body) {
		try {
			message.setFrom(new InternetAddress(from));
			message.addRecipient(RecipientType.TO, new InternetAddress(to));
			message.setSubject(subject);
			message.setText(body);
			
			logger.info("Mail composed successfully");
		} catch (MessagingException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Function to add an attachment to the mail
	 * @param filePath The absolute path of the file to be attached
	 */
	public void addAttachment(String filePath) {
		try {
			DataSource dataSource = new FileDataSource(filePath);
			String fileName = new File(filePath).getName();
			
			BodyPart attachment = new MimeBodyPart();
			attachment.setDataHandler(new DataHandler(dataSource));
			attachment.setFileName(fileName);
			multipart.addBodyPart(attachment);
			
			logger.info("Mail attachment " + fileName + " added successfully");
		} catch(MessagingException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Function to add an attachment to the mail
	 * @param file The {@link File} to be attached
	 */
	public void addAttachment(File file) {
		this.addAttachment(file.getAbsolutePath());
	}
	
	/**
	 * Function to send the mail
	 */
	public void sendMail() {
		try {
			message.setContent(multipart);
			
			if(authRequired) {
				Transport.send(message, username, password);
			} else {
				Transport.send(message);
			}
			
			logger.info("Mail sent successfully");
		} catch (MessagingException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
}