package com.ebao.scm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.text.WordUtils;

public final class CIMailSender extends CIMailLogger {
  public CIMailSender() {
    ciProject = System.getProperty("ci_project", "").trim();
    ciBuildID = System.getProperty("ci_build_id", "YYYY-MM-DD_hh-mm-ss").trim();
    ciBuildURL = System.getProperty("ci_build_url", "").trim();
    ciBuildType = System.getProperty("ci_build_type", "CI").trim();
    ciMailCCs = System.getProperty("ci_mail_cclist", "").trim();
    ciSyncCTFArtfTitle = System.getProperty("ci_sync_ctf_artf_title", "No").trim();
    ciSyncCTFUserInfo = System.getProperty("ci_sync_ctf_user_info", "No").trim();

    if (ciBuildType.equalsIgnoreCase("CI")) {
      ciBuildType = "Continuous Compile";
    }
    if (ciBuildType.equalsIgnoreCase("Nightly")) {
      ciBuildType = "Nightly Build";
    }

    final InputStream ins = CIMailSender.class.getResourceAsStream("CIMailRobot.properties");
    if (ins == null) {
      System.out.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSender()]");
      errLogger.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSender()]");
      houseKeeping();
      System.exit(-1);
    }
    final Properties props = new Properties();
    try {
      props.load(ins);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSender()]");
      errLogger.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSender()]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    mailSmtpHost = props.getProperty("mail.smtp.host", "").trim();
    if (mailSmtpHost.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.smtp.host] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [mail.smtp.host] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    mailSmtpPort = props.getProperty("mail.smtp.port", "").trim();
    if (mailSmtpPort.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.smtp.port] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [mail.smtp.port] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    mailAuthUsername = props.getProperty("mail.auth.username", "").trim();
    if (mailAuthUsername.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.auth.username] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [mail.auth.username] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    mailAuthPassword = props.getProperty("mail.auth.password", "").trim();
    if (mailAuthPassword.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.auth.password] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [mail.auth.password] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    mailDefaultDomain = props.getProperty("mail.default.domain", "").trim();
    if (mailDefaultDomain.isEmpty()) {
      System.out.println("==> Oops, failed to read property [mail.default.domain] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [mail.default.domain] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    ciMailToList = new HashMap<String, String>(20);
  }

  private String generateMailBody(final ArrayList<String> compileErrLogs, final HashMap<String, String> compileErrDetails) {
    final InputStream ins = CIMailSender.class.getResourceAsStream("email.template");
    if (ins == null) {
      System.out.println("==> Oops, failed to read email template [CIMailSender.generateMailBody()]");
      errLogger.println("==> Oops, failed to read email template [CIMailSender.generateMailBody()]");
      houseKeeping();
      System.exit(-1);
    }
    final StringBuilder sbMailBody = new StringBuilder(4096);
    final Scanner sin = new Scanner(ins, "UTF-8");
    final Pattern ptnHL = Pattern.compile("\\A(\\s*)\\%HEADLINE\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    final Pattern ptnED = Pattern.compile("\\A(\\s*)\\%ERR_DETAILS\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    final Pattern ptnEL = Pattern.compile("\\A(\\s*)\\%ERR_LOGS\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    final Pattern ptnURL = Pattern.compile("\\A(\\s*)\\%BUILD_URL\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    CIMailCTFUtils ctfUtils = null;
    if (ciSyncCTFArtfTitle.equalsIgnoreCase("Yes") || ciSyncCTFUserInfo.equalsIgnoreCase("Yes")) {
      ctfUtils = new CIMailCTFUtils();
      ctfUtils.login();
    }
    ciMailToList.clear();
    while (sin.hasNextLine()) {
      final String line = sin.nextLine();
      final Matcher mtrHL = ptnHL.matcher(line);
      final Matcher mtrED = ptnED.matcher(line);
      final Matcher mtrEL = ptnEL.matcher(line);
      final Matcher mtrURL = ptnURL.matcher(line);
      if (mtrHL.matches()) {
        // ciProject.replaceAll("<", "(");
        // ciProject.replaceAll(">", ")");
        sbMailBody.append(String.format("%s%s for <span style=\"font-weight:bold;\">%s</span> " +
          "encountered some <span style=\"color:red; font-weight:bold;\">compilation errors</span>, " +
          "please refer to the details below:\n", mtrHL.group(1), ciBuildType, ciProject));
        continue;
      }
      if (mtrED.matches()) {
        for (Map.Entry<String, String> compileErrDetail: compileErrDetails.entrySet()) {
          final String[] infos = compileErrDetail.getValue().split("\\:", 3);
          if (infos.length != 3) {
            System.out.println("==> Oops, compile error details infomation is incorrect [" + compileErrDetail.getValue() + "]");
            errLogger.println("==> Oops, compile error details infomation is incorrect [" + compileErrDetail.getValue() + "]");
            houseKeeping();
            System.exit(-1);
          }
          String username = infos[0];
          String revision = infos[1];
          String message = infos[2];
          String fullname = "";
          String email = "";
          String artfId = "";
          String artfTitle = "";
          if (ciSyncCTFUserInfo.equalsIgnoreCase("Yes")) {
            fullname = ctfUtils.getUserFullName(username);
            if (fullname == null) {
              System.out.println("==> Oops, failed to retrieve fullname from CTF [" + username + "]");
              errLogger.println("==> Oops, failed to retrieve fullname from CTF [" + username + "]");
              fullname = username;
            }
            email = ctfUtils.getUserEmail(username);
            if (email == null) {
              System.out.println("==> Oops, failed to retrieve email from CTF [" + username + "]");
              errLogger.println("==> Oops, failed to retrieve email from CTF [" + username + "]");
              email = username + "@" + mailDefaultDomain;
            }
          }
          else {
            if (username.startsWith("alm.") || username.startsWith("cht.") || username.startsWith("sf.") || !username.contains(".")
                || (username.indexOf(".") != username.lastIndexOf(".")) || username.matches(".+\\d+.*")) {
              fullname = username;
            }
            else {
              fullname = username.replace('.', ' ');
              fullname = WordUtils.capitalizeFully(fullname);
            }
            email = username + "@" + mailDefaultDomain;
          }
          ciMailToList.put(email.trim(), "");

          message = message.replaceAll("<", "(");
          message = message.replaceAll(">", ")");
          final Pattern ptnArtifact = Pattern.compile("\\A\\s*\\[(artf\\d+)\\]\\s*\\:?\\s*(.*?)\\s*\\Z", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
          final Matcher mtrArtifact = ptnArtifact.matcher(message);
          if (mtrArtifact.find()) {
            artfId = mtrArtifact.group(1);
            artfTitle = mtrArtifact.group(2);
            if (ciSyncCTFArtfTitle.equalsIgnoreCase("Yes")) {
              artfTitle = ctfUtils.getArtifactTitle(artfId);
              if (artfTitle == null) {
                System.out.println("==> Oops, failed to retrieve artifact title from CTF [" + artfId + "]");
                errLogger.println("==> Oops, failed to retrieve artifact title from CTF [" + artfId + "]");
                artfTitle = mtrArtifact.group(2);
              }
            }
            message = String.format("[%s]{%s}", artfId, artfTitle);
          }
          else {
            message = "[N/A]{" + message + "}";
          }
          sbMailBody.append(mtrED.group(1) + "File: " + compileErrDetail.getKey() + ": r" + revision + "<br>\n");
          sbMailBody.append(mtrED.group(1) + "Detail: [<b style=\"color:#A52A2A;\">" + fullname + "</b>]" + message + "<br><br>\n");
        }
        continue;
      }
      if (mtrEL.matches()) {
        for (String compileErrLog: compileErrLogs) {
          compileErrLog = compileErrLog.replaceAll("<", "(");
          compileErrLog = compileErrLog.replaceAll(">", ")");
          sbMailBody.append(mtrEL.group(1) + compileErrLog + "<br>\n");
        }
        continue;
      }
      if (mtrURL.matches()) {
        sbMailBody.append(mtrURL.group(1) + "<a href=\"" + ciBuildURL + "\">" + ciBuildURL + "</a>");
        continue;
      }
      sbMailBody.append(line + "\n");
    }
    sin.close();
    if (ciSyncCTFArtfTitle.equalsIgnoreCase("Yes") || ciSyncCTFUserInfo.equalsIgnoreCase("Yes")) {
      ctfUtils.logoff();
    }
    return sbMailBody.toString();
  }

  public void send(final ArrayList<String> compileErrLogs, final HashMap<String, String> compileErrDetails) {
    if (compileErrLogs == null) {
      System.out.println("==> Oops, [compileErrLogs] cannot be null [CIMailSender.send()]");
      errLogger.println("==> Oops, [compileErrLogs] cannot be null [CIMailSender.send()]");
      houseKeeping();
      System.exit(-1);
    }
    if (compileErrDetails == null) {
      System.out.println("==> Oops, [compileErrDetails] cannot be null [CIMailSender.send()]");
      errLogger.println("==> Oops, [compileErrDetails] cannot be null [CIMailSender.send()]");
      houseKeeping();
      System.exit(-1);
    }

    final String mailBody = generateMailBody(compileErrLogs, compileErrDetails);

    String ciMailTOs = "";
    for (String ciMailTo: ciMailToList.keySet()) {
      if (ciMailTo.isEmpty()) {
        continue;
      }
      ciMailTOs += (ciMailTo + ",");
    }
    stdLogger.println("==> mailTOs: [" + ciMailTOs + "]");

    final String[] ciMailCcList = ciMailCCs.split(",");
    ciMailCCs = "";
    for (String ciMailCc: ciMailCcList) {
      if (ciMailCc.isEmpty()) {
        continue;
      }
      if (!ciMailCc.contains("@")) {
        ciMailCc += ("@" + mailDefaultDomain);
      }
      ciMailCCs += (ciMailCc + ",");
    }
    stdLogger.println("==> mailCCs: [" + ciMailCCs + "]");

    final Properties props = new Properties();
    props.put("mail.smtp.host", mailSmtpHost);
    props.put("mail.smtp.port", mailSmtpPort);
    try {
      final Session session = Session.getInstance(props);
      final MimeMessage msg = new MimeMessage(session);
      msg.setFrom(ciBuildType + " Mailing Robot <" + mailAuthUsername + "@" + mailDefaultDomain + ">");
      msg.setSubject("[" + ciProject + "]" + "Detailed Information for " + ciBuildType + "@" + ciBuildID, "UTF-8");
      msg.setSentDate(new Date());
      msg.setRecipients(Message.RecipientType.TO, ciMailTOs);
      msg.setRecipients(Message.RecipientType.CC, ciMailCCs);
      msg.setContent(mailBody, "text/html; charset=UTF-8");
      Transport.send(msg, mailAuthUsername, mailAuthPassword);
      // Transport.send(msg);
    }
    catch (MessagingException e) {
      System.out.println("==> Oops, failed to send CI mail [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to send CI mail [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  // file properties
  private final String mailSmtpHost;
  private final String mailSmtpPort;
  private final String mailAuthUsername;
  private final String mailAuthPassword;
  private final String mailDefaultDomain;
  // system properties [-D]
  private final String ciProject;
  private final String ciBuildID;
  private final String ciBuildURL;
  private final String ciSyncCTFArtfTitle;
  private final String ciSyncCTFUserInfo;
  private String ciBuildType;
  private String ciMailCCs;
  //
  private final HashMap<String, String> ciMailToList;
}