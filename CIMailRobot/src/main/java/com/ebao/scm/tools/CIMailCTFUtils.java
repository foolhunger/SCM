package com.ebao.scm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Properties;

import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap60.webservices.cemain.UserSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.ITrackerAppSoap;

public final class CIMailCTFUtils extends CIMailLogger {
  public CIMailCTFUtils() {
    final InputStream ins = CIMailCTFUtils.class.getResourceAsStream("CIMailRobot.properties");
    if (ins == null) {
      System.out.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailCTFUtils()]");
      errLogger.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailCTFUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    final Properties props = new Properties();
    try {
      props.load(ins);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailCTFUtils()]");
      errLogger.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailCTFUtils()]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    ctfURL = props.getProperty("ctf.url", "").trim();
    if (ctfURL.isEmpty()) {
      System.out.println("==> Oops, failed to read property [ctf.url] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [ctf.url] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    ctfAuthUsername = props.getProperty("ctf.auth.username", "").trim();
    if (ctfAuthUsername.isEmpty()) {
      System.out.println("==> Oops, failed to read property [ctf.auth.username] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [ctf.auth.username] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    ctfAuthPassword = props.getProperty("ctf.auth.password", "").trim();
    if (ctfAuthPassword.isEmpty()) {
      System.out.println("==> Oops, failed to read property [ctf.auth.password] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [ctf.auth.password] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    ICollabNetSoap ctfCemainSoap = null;
    ITrackerAppSoap ctfTrackerSoap = null;
    try {
      ctfCemainSoap = (ICollabNetSoap) ClientSoapStubFactory.getSoapStub(ICollabNetSoap.class, ctfURL);
      ctfTrackerSoap = (ITrackerAppSoap) ClientSoapStubFactory.getSoapStub(ITrackerAppSoap.class, ctfURL);
    }
    catch (Throwable e) {
      System.out.println("==> Oops, failed to instantiate CTF SOAP objects [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to instantiate CTF SOAP objects [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    this.ctfCemainSoap = ctfCemainSoap;
    this.ctfTrackerSoap = ctfTrackerSoap;
  }

  public void login() {
    try {
      ctfSessionId = ctfCemainSoap.login(ctfAuthUsername, ctfAuthPassword);
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to login to Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to login to Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "] [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    stdLogger.println("==> Teamforge: logged in as user [" + ctfAuthUsername + "]");
  }

  public void logoff() {
    try {
      ctfCemainSoap.logoff(ctfAuthUsername, ctfSessionId);
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to logoff from Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to logoff from Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "] [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    stdLogger.println("==> Teamforge: logged off as user [" + ctfAuthUsername + "]");
  }

  public String getArtifactTitle(final String artifactId) {
    if (artifactId == null) {
      System.out.println("==> Oops, artifact id cannot be null [CIMailCTFUtils.getArtifactTitle()]");
      errLogger.println("==> Oops, artifact id cannot be null [CIMailCTFUtils.getArtifactTitle()]");
      houseKeeping();
      System.exit(-1);
    }
    String artifactTitle = "";
    try {
      ArtifactSoapDO artifactSoapDo = ctfTrackerSoap.getArtifactData(ctfSessionId, artifactId);
      artifactTitle = artifactSoapDo.getTitle();
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to retrieve artifact title [" + artifactId + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to retrieve artifact title [" + artifactId + "] [" + e.getMessage() + "]");
      return null;
    }
    return artifactTitle;
  }

  public String getUserFullName(final String username) {
    if (username == null) {
      System.out.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserFullName()]");
      errLogger.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserFullName()]");
      houseKeeping();
      System.exit(-1);
    }
    String userFullName = "";
    try {
      UserSoapDO userSoapDo = ctfCemainSoap.getUserData(ctfSessionId, username);
      userFullName = userSoapDo.getFullName();
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to retrieve user fullname [" + username + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to retrieve user fullname [" + username + "] [" + e.getMessage() + "]");
      return null;
    }
    return userFullName;
  }

  public String getUserEmail(final String username) {
    if (username == null) {
      System.out.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserEmail()]");
      errLogger.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserEmail()]");
      houseKeeping();
      System.exit(-1);
    }
    String userEmail = "";
    try {
      UserSoapDO userSoapDo = ctfCemainSoap.getUserData(ctfSessionId, username);
      userEmail = userSoapDo.getEmail();
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to retrieve user email [" + username + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to retrieve user email [" + username + "] [" + e.getMessage() + "]");
      return null;
    }
    return userEmail;
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  private final String ctfURL;
  private final String ctfAuthUsername;
  private final String ctfAuthPassword;
  private String ctfSessionId;
  private final ICollabNetSoap ctfCemainSoap;
  private final ITrackerAppSoap ctfTrackerSoap;
}