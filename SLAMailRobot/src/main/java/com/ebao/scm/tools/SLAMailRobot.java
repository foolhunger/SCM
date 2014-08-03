package com.ebao.scm.tools;

import java.io.PrintWriter;

public final class SLAMailRobot extends SLAMailLogger {
  public SLAMailRobot() {
    System.out.println("==> SLA Mailing Robot Wakes Up ...");
    stdLogger.println("==> SLA Mailing Robot Wakes Up ...");
  }

  public void doWork() {
    final SLAMailWrapper wrapper = new SLAMailWrapper();
    wrapper.loginCTF();
    wrapper.sendMail();
    wrapper.logoffCTF();
  }

  public void close() {
    System.out.println("==> SLA Mailing Robot Falls Asleep ...");
    stdLogger.println("==> SLA Mailing Robot Falls Asleep ...");
    houseKeeping();
  }

  public static void main(String[] args) {
    final SLAMailRobot robot = new SLAMailRobot();
    robot.doWork();
    robot.close();
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
}
