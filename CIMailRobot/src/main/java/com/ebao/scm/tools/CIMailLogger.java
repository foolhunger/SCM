package com.ebao.scm.tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public abstract class CIMailLogger {
  public static PrintWriter stdLoggerFactory() {
    return stdLogger;
  }
  
  public static PrintWriter errLoggerFactory() {
    return errLogger;
  }
  
  public abstract void houseKeeping();
  
  private static PrintWriter stdLogger;
  private static PrintWriter errLogger;
  
  static {
    try {
      stdLogger = new PrintWriter("stdLog.txt", "UTF-8");
      errLogger = new PrintWriter("errLog.txt", "UTF-8");
    }
    catch (FileNotFoundException e) {
      System.out.println("==> Oops, failed to instantiate logger objects [" + e.getMessage() + "]");
      e.printStackTrace();
      System.exit(-1);
    }
    catch (UnsupportedEncodingException e) {
      System.out.println("==> Oops, unsupported encoding for logger objects [" + e.getMessage() + "]");
      e.printStackTrace();
      System.exit(-1);
    }
  }
}