package com.ebao.scm.tools;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Scanner;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import org.apache.commons.lang3.text.WordUtils;

public final class CIMailSender extends CIMailLogger {
  public CIMailSender() {
    ciProject = System.getProperty("ci.project", "").trim();
    ciBuildID = System.getProperty("ci.build.id", "YYYY-MM-DD_hh-mm-ss").trim();
    ciBuildURL = System.getProperty("ci.build.url", "").trim();
    ciBuildType = System.getProperty("ci.build.type", "CI").trim();
    ciMailCCs = System.getProperty("ci.mail.cclist", "").trim();
    ciSyncCTFArtfTitle = System.getProperty("ci.sync.ctf.artf.title", "No").trim();
    ciSyncCTFUserInfo = System.getProperty("ci.sync.ctf.user.info", "No").trim();
    
    if (ciBuildType.equalsIgnoreCase("CI")) {
      ciBuildType = "Continuous Compile";
    }
    if (ciBuildType.equalsIgnoreCase("Nightly")) {
      ciBuildType = "Nightly Build";
    }
    
    try {
      InputStream ins = CIMailSender.class.getResourceAsStream("CIMailRobot.properties");
      if (ins == null) {
        System.out.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSender()]");
        errLogger.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSender()]");
        houseKeeping();
        System.exit(-1);
      }
      Properties props = new Properties();
      props.load(ins);
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
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSender()]");
      errLogger.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSender()]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    ciMailToList = new HashMap<String, String>(20);
  }
  
  private String generateMailBody(final ArrayList<String> compileErrLogs, final HashMap<String, String> compileErrDetails) {
    InputStream ins = CIMailSender.class.getResourceAsStream("email.template");
    if (ins == null) {
      System.out.println("==> Oops, failed to read email template [CIMailSender.generateMailBody()]");
      errLogger.println("==> Oops, failed to read email template [CIMailSender.generateMailBody()]");
      houseKeeping();
      System.exit(-1);
    }
    StringBuilder sbMailBody = new StringBuilder(4096);
    Scanner sin = new Scanner(ins);
    Pattern ptnHL = Pattern.compile("\\A(\\s*)\\%HEADLINE\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    Pattern ptnED = Pattern.compile("\\A(\\s*)\\%ERR_DETAILS\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    Pattern ptnEL = Pattern.compile("\\A(\\s*)\\%ERR_LOGS\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    Pattern ptnURL = Pattern.compile("\\A(\\s*)\\%BUILD_URL\\%\\s*\\Z", Pattern.CASE_INSENSITIVE);
    CIMailCTFUtils ctfUtils = null;
    if (ciSyncCTFArtfTitle.equalsIgnoreCase("Yes") || ciSyncCTFUserInfo.equalsIgnoreCase("Yes")) {
      ctfUtils = new CIMailCTFUtils();
      ctfUtils.login();
    }
    ciMailToList.clear();
    while (sin.hasNextLine()) {
      String line = sin.nextLine();
      Matcher mtrHL = ptnHL.matcher(line);
      Matcher mtrED = ptnED.matcher(line);
      Matcher mtrEL = ptnEL.matcher(line);
      Matcher mtrURL = ptnURL.matcher(line);
      if (mtrHL.matches()) {
        ciProject.replaceAll("<", "(");
        ciProject.replaceAll(">", ")");
        sbMailBody.append(String.format("%s%s for <span style=\"font-weight:bold;\">%s</span> " + 
          "encountered some <span style=\"color:red; font-weight:bold;\">compilation errors</span>, " + 
          "please refer to the details below:\n", mtrHL.group(1), ciBuildType, ciProject));
        continue;
      }
      if (mtrED.matches()) {
        for (Map.Entry<String, String> compileErrDetail: compileErrDetails.entrySet()) {
          String[] infos = compileErrDetail.getValue().split("\\:", 3);
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
          
          message.replaceAll("<", "(");
          message.replaceAll(">", ")");
          Pattern ptnArtfact = Pattern.compile("\\A\\s*\\[(artf\\d+)\\]\\s*\\:?\\s*(.*?)\\s*\\Z", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
          Matcher mtrArtfact = ptnArtfact.matcher(message);
          if (mtrArtfact.find()) {
            artfId = mtrArtfact.group(1);
            artfTitle = mtrArtfact.group(2);
            if (ciSyncCTFArtfTitle.equalsIgnoreCase("Yes")) {
              artfTitle = ctfUtils.getArtifactTitle(artfId);
              if (artfTitle == null) {
                System.out.println("==> Oops, failed to retrieve artifact title from CTF [" + artfId + "]");
                errLogger.println("==> Oops, failed to retrieve artifact title from CTF [" + artfId + "]");
                artfTitle = mtrArtfact.group(2);
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
          compileErrLog.replaceAll("<", "(");
          compileErrLog.replaceAll(">", ")");
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
    
    String mailBody = generateMailBody(compileErrLogs, compileErrDetails);
    
    String ciMailTOs = "";
    for (String ciMailTo: ciMailToList.keySet()) {
      if (ciMailTo.isEmpty()) {
        continue;
      }
      ciMailTOs += (ciMailTo + ",");
    }
    stdLogger.println("==> mailTOs: [" + ciMailTOs + "]");
    
    String[] ciMailCcList = ciMailCCs.split(",");
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
    
    Properties props = new Properties();
    props.put("mail.smtp.host", mailSmtpHost);
    props.put("mail.smtp.port", mailSmtpPort);
    try {
      Session session = Session.getInstance(props);
      MimeMessage msg = new MimeMessage(session);
      msg.setFrom(ciBuildType + " Mailing Robot <" + mailAuthUsername + "@" + mailDefaultDomain + ">");
      msg.setSubject("[" + ciProject + "]" + "Detailed Information for " + ciBuildType + "@" + ciBuildID);
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
      System.exit(-1);
    }
  }
  
  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }
  
  private PrintWriter stdLogger = stdLoggerFactory();
  private PrintWriter errLogger = errLoggerFactory();
  // file properties
  private String mailSmtpHost;
  private String mailSmtpPort;
  private String mailAuthUsername;
  private String mailAuthPassword;
  private String mailDefaultDomain;
  // system properties [-D]
  private String ciProject;
  private String ciBuildID;
  private String ciBuildURL;
  private String ciBuildType;
  private String ciMailCCs;
  private String ciSyncCTFArtfTitle;
  private String ciSyncCTFUserInfo;
  //
  private HashMap<String, String> ciMailToList;
}