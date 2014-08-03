package com.ebao.scm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.ini4j.Ini;
import org.ini4j.Profile;

public final class SLAMailIniUtils extends SLAMailLogger {
  public SLAMailIniUtils(final String cfgFileName) {
    if (cfgFileName == null) {
      System.out.println("==> Oops, config file name is mandatory [SLAMailIniUtils()]");
      errLogger.println("==> Oops, config file name is mandatory [SLAMailIniUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    final InputStream ins = SLAMailIniUtils.class.getResourceAsStream(cfgFileName);
    if (ins == null) {
      System.out.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailIniUtils()]");
      errLogger.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailIniUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    Ini iniCfg = null;
    try {
      iniCfg = new Ini(ins);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to instantiate ini config object [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to instantiate ini config object [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    this.iniCfg = iniCfg;
  }

  public ArrayList<String> getSectionNames() {
    if (iniCfg == null) {
      return null;
    }
    final ArrayList<String> sectionNames = new ArrayList<String>();
    for (String sectionName: iniCfg.keySet()) {
      sectionNames.add(sectionName);
    }
    return sectionNames;
  }

  public LinkedHashMap<String, String> getSectionProperties(final String sectionName) {
    if (iniCfg == null || sectionName == null) {
      return null;
    }
    final LinkedHashMap<String, String> sectionProperties = new LinkedHashMap<String, String>();
    final Profile.Section section = iniCfg.get(sectionName);
    if (section == null) {
      return null;
    }
    for (String sectionPropertyName: section.keySet()) {
      final String sectionPropertyValue = section.get(sectionPropertyName);
      sectionProperties.put(sectionPropertyName, sectionPropertyValue);
    }
    return sectionProperties;
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  private final Ini iniCfg;
}
