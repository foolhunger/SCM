package com.ebao.scm.tools;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Seconds;

public final class SLAMailTimeUtils extends SLAMailLogger {
  public SLAMailTimeUtils(final String cfgFileName) {
    if (cfgFileName == null) {
      System.out.println("==> Oops, confg file name is mandatory [SLAMailTimeUtils()]");
      errLogger.println("==> Oops, confg file name is mandatory [SLAMailTimeUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    final InputStream ins = SLAMailTimeUtils.class.getResourceAsStream(cfgFileName);
    if (ins == null) {
      System.out.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailTimeUtils()]");
      errLogger.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailTimeUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    nationalHolidays = new HashSet<String>();
    workWeekends = new HashSet<String>();
    String currentSection = "none";
    final Scanner sin = new Scanner(ins, "UTF-8");
    while (sin.hasNextLine()) {
      final String inputLine = sin.nextLine().trim();
      if (inputLine.isEmpty() || inputLine.startsWith("#")) {
        continue;
      }
      if (inputLine.matches("(?i)\\A\\s*\\[\\s*National\\s+Holidays\\s*\\]\\s*\\Z")) {
        currentSection = "sectionHolidays";
        continue;
      }
      if (inputLine.matches("(?i)\\A\\s*\\[\\s*Work\\s+Weekends\\s*\\]\\s*\\Z")) {
        currentSection = "sectionWorkdays";
        continue;
      }
      if (currentSection.equalsIgnoreCase("sectionHolidays")) {
        nationalHolidays.add(inputLine);
      }
      if (currentSection.equalsIgnoreCase("sectionWorkdays")) {
        workWeekends.add(inputLine);
      }
    }
    sin.close();
  }

  public void printNationalHolidays() {
    for (String nationalHoliday: nationalHolidays) {
      System.out.println(">>> [" + nationalHoliday + "]");
    }
  }

  public void printWorkWeekends() {
    for (String workWeekend: workWeekends) {
      System.out.println(">>> [" + workWeekend + "]");
    }
  }

  public boolean isWorkDay(final DateTime dateTime) {
    final String simpleDateTime = dateTime.toString("YYYY-MM-dd");
    final int dayOfWeek = dateTime.getDayOfWeek();
    if (dayOfWeek == DateTimeConstants.SATURDAY || dayOfWeek == DateTimeConstants.SUNDAY) {
      return workWeekends.contains(simpleDateTime);
    }
    return !nationalHolidays.contains(simpleDateTime);
  }

  public DateTime plusWorkDays(final DateTime dateTime, final int workdays) {
    DateTime plusDateTime = dateTime;
    for (int idx = 0; idx < workdays;) {
      plusDateTime = plusDateTime.plusDays(1);
      if (isWorkDay(plusDateTime)) {
        ++idx;
      }
    }
    return plusDateTime;
  }

  public boolean isSameDate(final DateTime firstDateTime, final DateTime secondDateTime) {
    return (firstDateTime.getYear() == secondDateTime.getYear()
        && firstDateTime.getMonthOfYear() == secondDateTime.getMonthOfYear()
        && firstDateTime.getDayOfMonth() == secondDateTime.getDayOfMonth());
  }

  public double secondsToDays(final int seconds) {
    return ((double) seconds) / DateTimeConstants.SECONDS_PER_DAY;
  }

  public String toDateTimeString(final DateTime dateTime) {
    return dateTime.toString("YYYY-MM-dd HH:mm:ss");
  }

  public int getSecondsBetweenWorkDays(final DateTime dateTimeStart, final DateTime dateTimeEnd) {
    DateTime dtStart = dateTimeStart;
    DateTime dtEnd = dateTimeEnd;
    int factor = 1;
    if (!dateTimeStart.isBefore(dateTimeEnd)) {
      dtStart = dateTimeEnd;
      dtEnd = dateTimeStart;
      factor = -1;
    }
    int holidays = 0;
    while (!isSameDate(dtStart, dtEnd)) {
      if (!isWorkDay(dtStart)) {
        ++holidays;
      }
      dtStart = dtStart.plusDays(1);
    }
    dtStart = dateTimeStart;
    dtEnd = dateTimeEnd;
    if (factor == -1) {
      dtStart = dateTimeEnd;
      dtEnd = dateTimeStart;
    }
    final int seconds = Seconds.secondsBetween(dtStart, dtEnd).getSeconds() - holidays * DateTimeConstants.SECONDS_PER_DAY;
    return factor * seconds;
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  private final HashSet<String> nationalHolidays;
  private final HashSet<String> workWeekends;
}
