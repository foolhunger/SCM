package com.ebao.scm.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.joda.time.DateTime;

import com.ebao.scm.tools.SLAMailCTFUtils.ArtifactDetailList;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public final class SLAMailWrapper extends SLAMailLogger {
  public SLAMailWrapper() {
    iniUtils = new SLAMailIniUtils("configs/SLAMailRules.properties");
    ctfUtils = new SLAMailCTFUtils("configs/SLAMailRobot.properties");
    timeUtils = new SLAMailTimeUtils("configs/SLAMailHolidays.properties");
    mailSender = new SLAMailSender("configs/SLAMailRobot.properties");

    ftlConfig = new Configuration();
    ftlConfig.setClassForTemplateLoading(getClass(), "templates");
    ftlConfig.setObjectWrapper(new DefaultObjectWrapper());
    ftlConfig.setDefaultEncoding("UTF-8");
    ftlConfig.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
    ftlConfig.setIncompatibleImprovements(new Version(2, 3, 20));
    Template template = null;
    try {
      template = ftlConfig.getTemplate("email.ftl");
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to get email template [email.ftl]");
      errLogger.println("==> Oops, failed to get email template [email.ftl]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    ftlTemplate = template;
  }

  public ArrayList<String> getCfgProjects() {
    final ArrayList<String> sectionNames = iniUtils.getSectionNames();
    if (sectionNames == null) {
      return new ArrayList<String>();
    }
    final ArrayList<String> cfgProjects = new ArrayList<String>();
    for (String cfgProject: sectionNames) {
      if (!cfgProject.startsWith("default")) {
        cfgProjects.add(cfgProject);
      }
    }
    return cfgProjects;
  }

  public LinkedHashMap<String, String> getProjectConfig(final String projectName) {
    if (projectName == null) {
      return new LinkedHashMap<String, String>();
    }
    final LinkedHashMap<String, String> projectCfg = iniUtils.getSectionProperties(projectName);
    if (projectCfg == null) {
      return new LinkedHashMap<String, String>();
    }
    return projectCfg;
  }

  public LinkedHashMap<String, String> getEffectiveProjectConfig(final String projectName) {
    if (projectName == null) {
      return new LinkedHashMap<String, String>();
    }
    final LinkedHashMap<String, String> projectCfg = iniUtils.getSectionProperties(projectName);
    if (projectCfg == null) {
      return new LinkedHashMap<String, String>();
    }
    String inheritedCfg = projectCfg.get("inherit");
    if (inheritedCfg == null) {
      inheritedCfg = "default";
    }
    if (inheritedCfg.equalsIgnoreCase("false") || inheritedCfg.equalsIgnoreCase("none") || inheritedCfg.equalsIgnoreCase("no")) {
      return projectCfg;
    }
    final LinkedHashMap<String, String> effectiveProjectCfg = iniUtils.getSectionProperties(inheritedCfg);
    if (effectiveProjectCfg == null) {
      return projectCfg;
    }
    for (String projectCfgPropertyName: projectCfg.keySet()) {
      final String projectCfgPropertyValue = projectCfg.get(projectCfgPropertyName);
      effectiveProjectCfg.put(projectCfgPropertyName, projectCfgPropertyValue);
    }
    return effectiveProjectCfg;
  }

  public ArtifactDetailList getProjectFilteredArtifactDetailList(final String projectName, final LinkedHashMap<String, String> effectiveProjectCfg) {
    if (projectName == null || effectiveProjectCfg == null) {
      return null;
    }
    final String trackerName = effectiveProjectCfg.get("tracker");
    if (trackerName == null) {
      System.out.println("==> Oops, tracker name is mandatory [" + projectName + "] [getProjectFilteredArtifactDetailList()]");
      errLogger.println("==> Oops, tracker name is mandatory [" + projectName + "] [getProjectFilteredArtifactDetailList()]");
      return null;
    }
    final LinkedHashMap<String, Boolean> sortKeys = new LinkedHashMap<String, Boolean>();
    sortKeys.put("priority", true);
    final LinkedHashMap<String, String> filters = new LinkedHashMap<String, String>();
    for (String filterKey: effectiveProjectCfg.keySet()) {
      if (filterKey.equalsIgnoreCase("tracker") || filterKey.equalsIgnoreCase("mailto") || filterKey.equalsIgnoreCase("inherit")) {
        continue;
      }
      final String filterValue = effectiveProjectCfg.get(filterKey);
      filters.put(filterKey, filterValue);
    }
    final ArtifactDetailList artifactDetailList = ctfUtils.getArtifactDetailList(projectName, trackerName, filters, sortKeys);
    return artifactDetailList;
  }

  public void loginCTF() {
    ctfUtils.login();
  }

  public void logoffCTF() {
    ctfUtils.logoff();
  }

  public int getTimeFrame(final int artifactPriority) {
    switch (artifactPriority) {
      case 1:
        return 1;
      case 2:
        return 2;
      case 3:
        return 5;
      case 4:
        return 15;
      default:
        return -1;
    }
  }

  public DateTime getArtifactExpireDateTime(final DateTime artifactSubmitDateTime, final int artifactPriority) {
    final int timeFrame = getTimeFrame(artifactPriority);
    if (timeFrame == -1) {
      System.out.println("==> Oops, invalid time frame [priority: " + artifactPriority + "]");
      errLogger.println("==> Oops, invalid time frame [priority: " + artifactPriority + "]");
      return null;
    }
    final DateTime artifactExpireDateTime = timeUtils.plusWorkDays(artifactSubmitDateTime, timeFrame);
    return artifactExpireDateTime;
  }

  public double getArtifactRemainingWorkDays(final DateTime artifactExpireDateTime) {
    final int seconds = timeUtils.getSecondsBetweenWorkDays(new DateTime(), artifactExpireDateTime);
    return timeUtils.secondsToDays(seconds);
  }

  private String generateMailToList(final String projectName) {
    final String cfgMailTo = getProjectConfig(projectName).get("mailto");
    if (cfgMailTo == null) {
      return null;
    }
    final String mailDefaultDomain = mailSender.getMailDefaultDomain();
    final String[] mailTos = cfgMailTo.split(",");
    final StringBuilder mailToList = new StringBuilder();
    for (String mailTo: mailTos) {
      mailTo = mailTo.trim();
      if (mailTo.isEmpty()) {
        continue;
      }
      if (!mailTo.contains("@")) {
        mailTo = mailTo + "@" + mailDefaultDomain;
      }
      mailToList.append(mailTo + ",");
    }
    return mailToList.toString();
  }

  private Template getFtlTemplate() {
    return ftlTemplate;
  }

  private LinkedHashMap<String, Object> getFtlDataModel(final ArtifactDetailList artifactDetailList) {
    final LinkedHashMap<String, Object> root = new LinkedHashMap<String, Object>();
    final String[] headers = {"Project Id", "Artifact Id", "Title", "Priority", "Status", "Submit Date", "Time Frame", "Expire Date", "Remaining Days"};
    root.put("headers", headers);

    final String artifactBaseURL = ctfUtils.getArtifactBaseURL();
    root.put("artifactBaseURL", artifactBaseURL);

    final ArrayList<ArrayList<String>> artifacts = new ArrayList<ArrayList<String>>();
    for (int idx = 0; idx < artifactDetailList.size(); ++idx) {
      final ArrayList<String> artifact = new ArrayList<String>();
      artifact.add(artifactDetailList.getProjectTitle(idx));
      artifact.add(artifactDetailList.getId(idx));
      artifact.add(artifactDetailList.getTitle(idx));
      artifact.add(artifactDetailList.getPriorityDetails(idx));
      artifact.add(artifactDetailList.getStatus(idx));
      // submit date
      final DateTime artifactSubmitDateTime = new DateTime(artifactDetailList.getSubmittedDate(idx));
      artifact.add(timeUtils.toDateTimeString(artifactSubmitDateTime));
      // time frame
      final int timeFrame = getTimeFrame(artifactDetailList.getPriority(idx));
      artifact.add(String.format("%d workday(s)", timeFrame));
      // expire date
      final DateTime artifactExpireDateTime = getArtifactExpireDateTime(artifactSubmitDateTime, artifactDetailList.getPriority(idx));
      artifact.add(timeUtils.toDateTimeString(artifactExpireDateTime));
      // remaining days
      final double remainWorkDays = getArtifactRemainingWorkDays(artifactExpireDateTime);
      if (remainWorkDays < 0) {
        artifact.add(String.format("Overdue %.1f workday(s)", Math.abs(remainWorkDays)));
      }
      else {
        artifact.add(String.format("Remains %.1f workday(s)", remainWorkDays));
      }
      artifacts.add(artifact);
    }
    root.put("artifacts", artifacts);
    return root;
  }

  private String generateMailBody(final ArtifactDetailList artifactDetailList) {
    if (artifactDetailList == null) {
      return null;
    }
    // freemarker: template + data-model = output
    final Template template = getFtlTemplate();
    final LinkedHashMap<String, Object> dataModel = getFtlDataModel(artifactDetailList);
    final StringWriter output = new StringWriter();
    try {
      template.process(dataModel, output);
    }
    catch (TemplateException e) {
      System.out.println("==> Oops, errors occurred while processing template [" + e.getMessage() + "]");
      errLogger.println("==> Oops, errors occurred while processing template [" + e.getMessage() + "]");
      e.printStackTrace();
      return null;
    }
    catch (IOException e) {
      System.out.println("==> Oops, errors occurred while writing template to output [" + e.getMessage() + "]");
      errLogger.println("==> Oops, errors occurred while writing template to output [" + e.getMessage() + "]");
      e.printStackTrace();
      return null;
    }
    return output.toString();
  }

  public void sendMail() {
    final DateTime dateTime = new DateTime();
    if (!timeUtils.isWorkDay(dateTime)) {
      System.out.println("==> Happy holiday today!!! Forget about those nagging SLA supporting issues [" + dateTime.toString("YYYY-MM-dd") + "]");
      stdLogger.println("==> Happy holiday today!!! Forget about those nagging SLA supporting issues [" + dateTime.toString("YYYY-MM-dd") + "]");
      return;
    }
    final ArrayList<String> cfgProjects = getCfgProjects();
    for (String cfgProject: cfgProjects) {
      System.out.println(">>> Working hard on sending SLA reminder mail for project [" + cfgProject + "]");
      stdLogger.println(">>> Working hard on sending SLA reminder mail for project [" + cfgProject + "]");
      final LinkedHashMap<String, String> effectiveProjectCfg = getEffectiveProjectConfig(cfgProject);
      stdLogger.println("effective config: " + effectiveProjectCfg);
      final ArtifactDetailList artifactDetailList = getProjectFilteredArtifactDetailList(cfgProject, effectiveProjectCfg);
      if (artifactDetailList == null) {
        System.out.println("==> Oops, failed to get project artifact detail list [" + cfgProject + "]");
        errLogger.println("==> Oops, failed to get project artifact detail list [" + cfgProject + "]");
        continue;
      }
      if (artifactDetailList.size() == 0) {
        System.out.println("==> Great job!!! No more pending SLA artifacts [" + cfgProject + "]");
        stdLogger.println("==> Great job!!! No more pending SLA artifacts [" + cfgProject + "]");
        continue;
      }
      final String mailSubject = String.format("[%s]SLA Reminder for Teamforge Remaining Artifacts@%s", cfgProject, dateTime.toString("YYYY-MM-dd"));
      final String mailBody = generateMailBody(artifactDetailList);
      if (mailBody == null) {
        System.out.println("==> Oops, failed to generate email body [" + cfgProject + "]");
        errLogger.println("==> Oops, failed to generate email body [" + cfgProject + "]");
        continue;
      }
      final String mailToList = generateMailToList(cfgProject);
      if (mailToList == null || mailToList.isEmpty()) {
        System.out.println("==> Oops, failed to get mailto recipient list [" + cfgProject + "]");
        errLogger.println("==> Oops, failed to get mailto recipient list [" + cfgProject + "]");
        continue;
      }
      if (mailSender.send(mailSubject, mailBody, mailToList)) {
        System.out.println("==> SLA reminder email sent successfully");
        stdLogger.println("==> SLA reminder email sent successfully");
      }
      else {
        System.out.println("==> SLA reminder email senting failed");
        errLogger.println("==> SLA reminder email senting failed");
      }
    }
    //
  }

  @Override
  public void houseKeeping() {
    stdLogger.close();
    errLogger.close();
  }

  private final PrintWriter stdLogger = stdLoggerFactory();
  private final PrintWriter errLogger = errLoggerFactory();
  private final SLAMailIniUtils iniUtils;
  private final SLAMailCTFUtils ctfUtils;
  private final SLAMailTimeUtils timeUtils;
  private final SLAMailSender mailSender;
  private final Configuration ftlConfig;
  private final Template ftlTemplate;
}
