package nl.rrd.senseeact.service.mail;

import jakarta.activation.URLDataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.model.User;
import nl.rrd.utils.AppComponents;
import org.slf4j.Logger;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EmailSender {
	private EmailConfiguration config;

	public EmailSender(EmailConfiguration config) {
		this.config = config;
	}

	public void trySendThread(EmailTemplate template,
			HttpServletRequest request, User user, String to,
			Map<String,Object> contentParams) {
		Logger logger = AppComponents.getLogger(
				EmailSender.class.getSimpleName());
		String error = String.format("Failed to send email (host: %s, to: %s)",
				config.getHost(), to);
		SendInfo sendInfo;
		try {
			sendInfo = getSendInfo(template, request, user, to, contentParams);
		} catch (MessagingException | IOException ex) {
			logger.error(error + ": " + ex.getMessage(), ex);
			return;
		}
		new Thread(() -> {
			try {
				send(sendInfo);
			} catch (MailException ex) {
				logger.error(error + ": " + ex.getMessage(), ex);
			}
		}).start();
	}

	public void send(EmailTemplate template, HttpServletRequest request,
			User user, String to, Map<String,Object> contentParams)
			throws MailException, MessagingException, IOException {
		send(getSendInfo(template, request, user, to, contentParams));
	}

	private SendInfo getSendInfo(EmailTemplate template,
			HttpServletRequest request, User user, String to,
			Map<String,Object> contentParams) throws MessagingException,
			IOException {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(config.getHost());
		Properties mailProps = sender.getJavaMailProperties();
		if (!config.getUsername().isEmpty() &&
				!config.getPassword().isEmpty()) {
			mailProps.put("mail.smtp.auth", "true");
			sender.setUsername(config.getUsername());
			sender.setPassword(config.getPassword());
		}
		if (config.isSmtpTls()) {
			sender.setPort(587);
			mailProps.put("mail.smtp.starttls.enable", "true");
		}
		mailProps.put("mail.debug", "true");
		mailProps.put("mail.smtp.localhost", "ubuntu");
		MimeMessage message = sender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true,
				"UTF-8");
		String from = template.getFrom(request, user);
		helper.setFrom(from);
		helper.setTo(to);
		String subject = template.getSubject(request, user);
		helper.setSubject(subject);
		String html = template.getHtmlContent(request, user, contentParams);
		helper.setText(html, true);
		List<EmailTemplate.InlineFile> files = template.getInlineFiles(
				request, user);
		for (EmailTemplate.InlineFile file : files) {
			helper.addInline(file.getContentId(), new URLDataSource(
					file.getUrl()));
		}
		SendInfo result = new SendInfo();
		result.sender = sender;
		result.message = message;
		result.host = config.getHost();
		result.from = from;
		result.to = to;
		return result;
	}

	private static void send(SendInfo info) throws MailException {
		Logger logger = AppComponents.getLogger(
				EmailSender.class.getSimpleName());
		logger.info("Send email (host: {}, from: {}, to: {})",
				info.host, info.from, info.to);
		info.sender.send(info.message);
		logger.info("Send email to {} succeeded", info.to);
	}

	private static class SendInfo {
		public JavaMailSenderImpl sender;
		public MimeMessage message;
		public String host;
		public String from;
		public String to;
	}
}
