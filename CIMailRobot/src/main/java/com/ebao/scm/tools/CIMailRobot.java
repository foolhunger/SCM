package com.ebao.scm.tools;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public final class CIMailRobot extends CIMailLogger {
  public CIMailRobot() {
    ciErrLog = System.getProperty("ci_error_log", "").trim();
    ciSrcURL = System.getProperty("ci_source_url", "").trim();

    if (ciErrLog.isEmpty()) {
      System.out.println("==> Oops, property [ci_error_log] cannot be empty [CIMailRobot()]");
      errLogger.println("==> Oops, property [ci_error_log] cannot be empty [CIMailRobot()]");
      houseKeeping();
      System.exit(-1);
    }
    if (ciSrcURL.isEmpty()) {
      System.out.println("==> Oops, property [ci_source_url] cannot be empty [CIMailRobot()]");
      errLogger.println("==> Oops, property [ci_source_url] cannot be empty [CIMailRobot()]");
      houseKeeping();
      System.exit(-1);
    }
  }

  public void doWork() {
    final CIMailParser parser = new CIMailParser();
    if(parser.parse(ciErrLog, ciSrcURL)) {
      final ArrayList<String> compileErrLogs = parser.getCompileErrLogs();
      final HashMap<String, String> compileErrDetails = parser.getCompileErrDetails();

      final CIMailSender sender = new CIMailSender();
      sender.send(compileErrLogs, compileErrDetails);
    }
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  public static void main(String[] args) {
    final CIMailRobot robot = new CIMailRobot();
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