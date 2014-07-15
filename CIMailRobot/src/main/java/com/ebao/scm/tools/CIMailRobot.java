package com.ebao.scm.tools;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public final class CIMailRobot extends CIMailLogger {
  public CIMailRobot() {
    ciErrLog = System.getProperty("ci.error.log", "").trim();
    ciSrcURL = System.getProperty("ci.source.url", "").trim();
    
    if (ciErrLog.isEmpty()) {
      System.out.println("==> Oops, property [ci.error.log] cannot be empty [CIMailRobot()]");
      errLogger.println("==> Oops, property [ci.error.log] cannot be empty [CIMailRobot()]");
      houseKeeping();
      System.exit(-1);
    }
    if (ciSrcURL.isEmpty()) {
      System.out.println("==> Oops, property [ci.source.url] cannot be empty [CIMailRobot()]");
      errLogger.println("==> Oops, property [ci.source.url] cannot be empty [CIMailRobot()]");
      houseKeeping();
      System.exit(-1);
    }
  }
  
  public void doWork() {
    CIMailParser parser = new CIMailParser();
    if(parser.parse(ciErrLog, ciSrcURL)) {
      final ArrayList<String> compileErrLogs = parser.getCompileErrLogs();
      final HashMap<String, String> compileErrDetails = parser.getCompileErrDetails();
      
      CIMailSender sender = new CIMailSender();
      sender.send(compileErrLogs, compileErrDetails);
    }
  }
  
  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }
  
  public static void main(String[] args) {
    CIMailRobot robot = new CIMailRobot();
    robot.stdLogger.println("==> CI Mail Robot Wakes Up...");
    // --------------------------------------------------
    robot.doWork();
    // --------------------------------------------------
    robot.stdLogger.println("==> CI Mail Robot Falls Asleep...");
    robot.houseKeeping();
  }
  
  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  // system properites [-D]
  private final String ciSrcURL;
  private final String ciErrLog;
}