package com.ebao.scm.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;

public final class CIMailSVNUtils extends CIMailLogger {
  public CIMailSVNUtils() {
    InputStream ins = CIMailSVNUtils.class.getResourceAsStream("CIMailRobot.properties");
    if (ins == null) {
      System.out.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSVNUtils()]");
      errLogger.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSVNUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    Properties props = new Properties();
    try {
      props.load(ins);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSVNUtils()]");
      errLogger.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSVNUtils()]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    svnAuthUsername = props.getProperty("svn.auth.username", "").trim();
    if (svnAuthUsername.isEmpty()) {
      System.out.println("==> Oops, failed to read property [svn.auth.username] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [svn.auth.username] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    svnAuthPassword = props.getProperty("svn.auth.password", "").trim();
    if (svnAuthPassword.isEmpty()) {
      System.out.println("==> Oops, failed to read property [svn.auth.password] [CIMailRobot.properites]");
      errLogger.println("==> Oops, failed to read property [svn.auth.password] [CIMailRobot.properites]");
      houseKeeping();
      System.exit(-1);
    }
    svnClientManager = SVNClientManager.newInstance(null, svnAuthUsername, svnAuthPassword);
    svnLogEntry = null;
  }
  
  public void doLog(final String filename) {
    SVNLogClient svnLogClient = svnClientManager.getLogClient();
    File[] files = {new File(filename)};
    try {
      svnLogClient.doLog(files, null, null, true, true, 1L, new ISVNLogEntryHandler() {
        @Override
        public void handleLogEntry(SVNLogEntry svnLogEntry) {
          CIMailSVNUtils.this.svnLogEntry = svnLogEntry;
        }
      });
    }
    catch (SVNException e) {
      System.out.println("==> Oops, failed to retrieve SVN info [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to retrieve SVN info [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
  }
  
  // doLog() must be called before this method, otherwise, the behavior is undefined
  public String getAuthor() {
    if (svnLogEntry == null)
      return "";
    return svnLogEntry.getAuthor();
  }
  
  // doLog() must be called before this method, otherwise, the behavior is undefined
  public String getMessage() {
    if (svnLogEntry == null)
      return "";
    return svnLogEntry.getMessage();
  }
  
  // doLog() must be called before this method, otherwise, the behavior is undefined
  public long getRevision() {
    if (svnLogEntry == null)
      return -1;
    return svnLogEntry.getRevision();
  }
  
  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }
  
  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  private final String svnAuthUsername;
  private final String svnAuthPassword;
  private final SVNClientManager svnClientManager;
  private SVNLogEntry svnLogEntry;
}