package com.ebao.scm.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

public final class CIMailParser extends CIMailLogger {
  public CIMailParser() {
    compileErrLogs = new ArrayList<String>(50);
    compileErrDetails = new HashMap<String, String>(50);
  }
  
  public boolean parse(String clog, String prefix) {
    if (clog == null) {
      System.out.println("==> Oops, clog cannot be null [CIMailParser.parse()]");
      errLogger.println("==> Oops, clog cannot be null [CIMailParser.parse()]");
      houseKeeping();
      System.exit(-1);
    }
    if (prefix == null) {
      System.out.println("==> Oops, prefix cannot be null [CIMailParser.parse()]");
      errLogger.println("==> Oops, prefix cannot be null [CIMailParser.parse()]");
      houseKeeping();
      System.exit(-1);
    }
    compileErrLogs.clear();
    compileErrDetails.clear();
    clog = clog.replaceAll("\\\\", "/").trim();
    try {
      FileUtils.copyFile(new File(clog), new File("clog.txt"));
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to copy compilation log [" + e.getMessage() + "] [CIMailParser.parse()]");
      errLogger.println("==> Oops, failed to copy compilation log [" + e.getMessage() + "] [CIMailParser.parse()]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    Scanner sin = null;
    try {
      sin = new Scanner(new File("clog.txt"));
    }
    catch (FileNotFoundException e) {
      System.out.println("==> Oops, failed to read compilation log [" + e.getMessage() + "] [CIMailParser.parse()]");
      errLogger.println("==> Oops, failed to read compilation log [" + e.getMessage() + "] [CIMailParser.parse()]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    // parse for compilation error logs
    Pattern ptnBOE = Pattern.compile("(?:\\[(?:javac|info)\\]\\s*compiling\\s+\\d+)|(?:\\[error\\]\\s*failed\\s+to.+compilation\\s+failure)", Pattern.CASE_INSENSITIVE);
    Pattern ptnEOE = Pattern.compile("(?:\\[(?:javac|info)\\]\\s*\\d+\\s*error)|(?:\\[error\\]\\s*->\\s*\\[\\s*help\\s*\\d+\\s*\\])|(?:\\[(?:java|info)\\]\\s*build\\s+(?:failed|failure))", Pattern.CASE_INSENSITIVE);
    boolean flag = false;
    while (sin.hasNextLine()) {
      String line = sin.nextLine().trim();
      Matcher mtrBOE = ptnBOE.matcher(line);
      Matcher mtrEOE = ptnEOE.matcher(line);
      if (mtrBOE.find()) {
        compileErrLogs.clear();
      }
      compileErrLogs.add(line);
      if (mtrEOE.find()) {
        if (!compileErrLogs.isEmpty()) {
          flag = true;
        }
        break;
      }
    }
    sin.close();
    if (!flag) {
      System.out.println("==> No compilation errors have been found [" + clog + "]");
      stdLogger.println("==> No compilation errors have been found [" + clog + "]");
      return false;
    }
    // parse for compilation error files
    prefix = prefix.replaceAll("\\\\", "/").trim();
    Pattern pattern = null;
    Matcher matcher = null;
    String localPath = prefix;
    if (SystemUtils.IS_OS_WINDOWS) {
      pattern = Pattern.compile(".+?\\/([a-zA-Z]\\$\\/.+?)\\/*\\Z", Pattern.CASE_INSENSITIVE);
      matcher = pattern.matcher(prefix);
      if (matcher.find()) {
        localPath = matcher.group(1).replaceFirst("\\$", "\\:");
      }
      else {
        System.out.println("==> Oops, path format is incorrent [" + prefix + "] [CIMailParser.parse()]");
        errLogger.println("==> Oops, path format is incorrent [" + prefix + "] [CIMailParser.parse()]");
        houseKeeping();
        System.exit(-1);
      }
    }
    stdLogger.println("==> local source path is [" + localPath + "] [CIMailParser.parse()]");
    pattern = Pattern.compile("\\[(?:javac|error)\\].*?\\Q" + localPath + "\\E\\/(.+?)\\s*\\:", Pattern.CASE_INSENSITIVE);
    for (String compileErrLog: compileErrLogs) {
      compileErrLog = compileErrLog.replaceAll("\\\\", "/").trim();
      matcher = pattern.matcher(compileErrLog);
      if (matcher.find()) {
        if (!compileErrDetails.containsKey(matcher.group(1))) {
          System.out.println("==> ##### [" + matcher.group(1) + "]");
          stdLogger.println("==> ##### [" + matcher.group(1) + "]");
          compileErrDetails.put(matcher.group(1), "N/A");
        }
      }
    }
    if (compileErrDetails.isEmpty()) {
      System.out.println("==> No compilation error files have been found [" + clog + "]");
      errLogger.println("==> No compilation error files have been found [" + clog + "]");
      houseKeeping();
      System.exit(-1);
    }
    // parse for compilation error details
    CIMailSVNUtils svnUtils = new CIMailSVNUtils();
    for (String compileErrFile: compileErrDetails.keySet()) {
      svnUtils.doLog(prefix + "/" + compileErrFile);
      final String svnAuthor = svnUtils.getAuthor();
      final long svnRevision = svnUtils.getRevision();
      final String svnMessage = svnUtils.getMessage();
      System.out.println("==> >>>>> [" + svnAuthor + "] [r" + svnRevision + "] {" + svnMessage + "}");
      stdLogger.println("==> >>>>> [" + svnAuthor + "] [r" + svnRevision + "] {" + svnMessage + "}");
      compileErrDetails.put(compileErrFile, svnAuthor + ":" + svnRevision + ":" + svnMessage);
    }
    stdLogger.println("--------------------------------------------------");
    stdLogger.println(compileErrDetails);
    stdLogger.println("--------------------------------------------------");
    return true;
  }
  
  /*
   * Caveats:
   * - parse() must be called before this method, otherwise, the behavior is undefined
   * - clone() is not invoked on the returned object, so even shallow copy is not implemented
   */
  public ArrayList<String> getCompileErrLogs() {
    return compileErrLogs;
  }
  
  /*
   * Caveats:
   * - parse() must be called before this method, otherwise, the behavior is undefined
   * - clone() is not invoked on the returned object, so even shallow copy is not implemented
   */
  public HashMap<String, String> getCompileErrDetails() {
    return compileErrDetails;
  }
  
  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }
  
  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  private final ArrayList<String> compileErrLogs;
  private final HashMap<String, String> compileErrDetails;
}