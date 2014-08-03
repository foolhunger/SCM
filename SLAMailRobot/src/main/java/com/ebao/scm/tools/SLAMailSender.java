package com.ebao.scm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public final class SLAMailSender extends SLAMailLogger {
  public SLAMailSender(final String cfgFileName) {
    if (cfgFileName == null) {
      System.out.println("==> Oops, confg file name is mandatory [SLAMailSender()]");
      errLogger.println("==> Oops, confg file name is mandatory [SLAMailSender()]");
      houseKeeping();
      System.exit(-1);
    }
    final InputStream ins = SLAMailSender.class.getResourceAsStream(cfgFileName);
    if (ins == null) {
      System.out.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailSender()]");
      errLogger.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailSender()]");
      houseKeeping();
      System.exit(-1);
    }
    final Properties props = new Properties();
    try {
      props.load(ins);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to load properties [" + cfgFileName + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to load properties [" + cfgFileName + "] [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    mailSmtpHost = props.getProperty("mail.smtp.host", "").trim();
    if (mailSmtpHost.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.smtp.host] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [mail.smtp.host] [" + cfgFileName + "]");
      houseKeeping();
      System.exit(-1);
    }
    mailSmtpPort = props.getProperty("mail.smtp.port", "").trim();
    if (mailSmtpPort.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.smtp.port] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [mail.smtp.port] [" + cfgFileName + "]");
      houseKeeping();
      System.exit(-1);
    }
    mailAuthUsername = props.getProperty("mail.auth.username", "").trim();
    if (mailAuthUsername.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.auth.username] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [mail.auth.username] [" + cfgFileName + "]");
      houseKeeping();
      System.exit(-1);
    }
    mailAuthPassword = props.getProperty("mail.auth.password", "").trim();
    if (mailAuthPassword.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.auth.password] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [mail.auth.password] [" + cfgFileName + "]");
      houseKeeping();
      System.exit(-1);
    }
    mailDefaultDomain = props.getProperty("mail.default.domain", "").trim();
    if (mailDefaultDomain.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.default.domain] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [mail.default.domain] [" + cfgFileName + "]");
      houseKeeping();
      System.exit(-1);
    }
  }

  public String getMailSmtpHost() {
    return mailSmtpHost;
  }

  public String getMailSmtpPort() {
    return mailSmtpPort;
  }

  public String getMailDefaultDomain() {
    return mailDefaultDomain;
  }

  public boolean send(final String mailSubject, final String mailBody, final String mailToList) {
    final Properties props = new Properties();
    props.put("mail.smtp.host", mailSmtpHost);
    props.put("mail.smtp.port", mailSmtpPort);
    try {
      final Session session = Session.getInstance(props);
      final MimeMessage msg = new MimeMessage(session);
      msg.setFrom(mailAuthUsername + "@" + mailDefaultDomain);
      msg.setSubject(mailSubject, "UTF-8");
      msg.setSentDate(new Date());
      msg.setRecipients(Message.RecipientType.TO, mailToList);
      msg.setContent(mailBody, "text/html; charset=UTF-8");
      Transport.send(msg, mailAuthUsername, mailAuthPassword);
    }
    catch (MessagingException e) {
      System.out.println("==> Oops, failed to send mail [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to send mail [" + e.getMessage() + "]");
      e.printStackTrace();
      return false;
    }
    return true;
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  private final String mailSmtpHost;
  private final String mailSmtpPort;
  private final String mailAuthUsername;
  private final String mailAuthPassword;
  private final String mailDefaultDomain;
}
