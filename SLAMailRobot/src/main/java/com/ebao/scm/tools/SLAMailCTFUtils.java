package com.ebao.scm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Properties;

import com.collabnet.ce.soap60.types.SoapFilter;
import com.collabnet.ce.soap60.types.SoapSortKey;
import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapList;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap60.webservices.cemain.TrackerFieldSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactDetailSoapList;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactDetailSoapRow;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.ITrackerAppSoap;
import com.collabnet.ce.soap60.webservices.tracker.TrackerFieldValueSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.TrackerSoapList;
import com.collabnet.ce.soap60.webservices.tracker.TrackerSoapRow;

public final class SLAMailCTFUtils extends SLAMailLogger {
  public SLAMailCTFUtils(final String cfgFileName) {
    if (cfgFileName == null) {
      System.out.println("==> Oops, config file name is mandatory [SLAMailCTFUtils()]");
      errLogger.println("==> Oops, config file name is mandatory [SLAMailCTFUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    final InputStream ins = SLAMailCTFUtils.class.getResourceAsStream(cfgFileName);
    if (ins == null) {
      System.out.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailCTFUtils()]");
      errLogger.println("==> Oops, failed to read config file [" + cfgFileName + "] [SLAMailCTFUtils()]");
      houseKeeping();
      System.exit(-1);
    }
    final Properties props = new Properties();
    try {
      props.load(ins);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to load properties [" + cfgFileName + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to load properties [" + cfgFileName + "] [" + e.getMessage() + "]");
      e.printStackTrace();
      houseKeeping();
      System.exit(-1);
    }
    ctfURL = props.getProperty("ctf.url", "").trim();
    if (ctfURL.isEmpty()) {
      System.out.println("==> Oops, failed to read property [ctf.url] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [ctf.url] [" + cfgFileName + "]");
      houseKeeping();
      System.exit(-1);
    }
    ctfAuthUsername = props.getProperty("ctf.auth.username", "").trim();
    if (ctfAuthUsername.isEmpty()) {
      System.out.println("==> Oops, failed to read property [ctf.auth.username] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [ctf.auth.username] [" + cfgFileName + "]");
      houseKeeping();
      System.exit(-1);
    }
    ctfAuthPassword = props.getProperty("ctf.auth.password", "").trim();
    if (ctfAuthPassword.isEmpty()) {
      System.out.println("==> Oops, failed to read property [ctf.auth.password] [" + cfgFileName + "]");
      errLogger.println("==> Oops, failed to read property [ctf.auth.password] [" + cfgFileName + "]");
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

  public String getArtifactBaseURL() {
    return ctfURL.endsWith("/") ? ctfURL + "sf/go" : ctfURL + "/sf/go";
  }

  public String getProjectId(final String projectName) {
    if (projectName == null) {
      return null;
    }
    try {
      final ProjectSoapList projectSoapList = ctfCemainSoap.getProjectList(ctfSessionId, false);
      final ProjectSoapRow[] projectSoapRows = projectSoapList.getDataRows();
      for (ProjectSoapRow projectSoapRow: projectSoapRows) {
        if (projectSoapRow.getTitle().equals(projectName)) {
          return projectSoapRow.getId();
        }
      }
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to get project id [" + projectName + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to get project id [" + projectName + "] [" + e.getMessage() + "]");
      return null;
    }
    return null;
  }

  public String getTrackerId(final String projectName, final String trackerName) {
    if (projectName == null || trackerName == null) {
      return null;
    }
    final String projectId = getProjectId(projectName);
    if (projectId == null) {
      return null;
    }
    try {
      final TrackerSoapList trackerSoapList = ctfTrackerSoap.getTrackerList(ctfSessionId, projectId);
      final TrackerSoapRow[] trackerSoapRows = trackerSoapList.getDataRows();
      for (TrackerSoapRow trackerSoapRow: trackerSoapRows) {
        if (trackerSoapRow.getTitle().equals(trackerName)) {
          return trackerSoapRow.getId();
        }
      }
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to get tracker id [" + trackerName + "] [" + projectName + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to get tracker id [" + trackerName + "] [" + projectName + "] [" + e.getMessage() + "]");
      return null;
    }
    return null;
  }

  private LinkedHashMap<String, TrackerFieldSoapDO> getTrackerFlexFieldsData(final String trackerId) {
    if (trackerId == null) {
      return null;
    }
    final LinkedHashMap<String, TrackerFieldSoapDO> trackerFlexFieldsData = new LinkedHashMap<String, TrackerFieldSoapDO>();
    try {
      final TrackerFieldSoapDO[] trackerFieldSoapDOs = ctfTrackerSoap.getFields(ctfSessionId, trackerId);
      for (TrackerFieldSoapDO trackerFieldSoapDO: trackerFieldSoapDOs) {
        final String trackerFlexFieldName = trackerFieldSoapDO.getName();
        trackerFlexFieldsData.put(trackerFlexFieldName, trackerFieldSoapDO);
      }
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to get data for tracker flex fields [" + trackerId + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to get data for tracker flex fields [" + trackerId + "] [" + e.getMessage() + "]");
      return null;
    }
    return trackerFlexFieldsData;
  }

  private String getTrackerFlexFieldValueId(final String trackerFlexFieldValue, final TrackerFieldSoapDO trackerFieldSoapDO) {
    if (trackerFlexFieldValue == null || trackerFieldSoapDO == null) {
      return null;
    }
    final TrackerFieldValueSoapDO[] trackerFieldValueSoapDOs = trackerFieldSoapDO.getFieldValues();
    for (TrackerFieldValueSoapDO trackerFieldValueSoapDO: trackerFieldValueSoapDOs) {
      if (trackerFieldValueSoapDO.getValue().equals(trackerFlexFieldValue)) {
        return trackerFieldValueSoapDO.getId();
      }
    }
    return null;
  }

  private SoapSortKey[] soapSortKeysBuilder(final LinkedHashMap<String, Boolean> sortKeys) {
    if (sortKeys == null) {
      return null;
    }
    final ArrayList<SoapSortKey> soapSortKeyList = new ArrayList<SoapSortKey>();
    for (String sortKey: sortKeys.keySet()) {
      final boolean isAscending = sortKeys.get(sortKey);
      if (sortKey.equalsIgnoreCase("priority")) {
        soapSortKeyList.add(new SoapSortKey(ArtifactSoapDO.COLUMN_PRIORITY, isAscending));
      }
      else {
        stdLogger.println("==> Oops, invalid sort key [" + sortKey + "]");
        errLogger.println("==> Oops, invalid sort key [" + sortKey + "]");
        continue;
      }
    }
    SoapSortKey[] soapSortKeys = new SoapSortKey[soapSortKeyList.size()];
    soapSortKeys = soapSortKeyList.toArray(soapSortKeys);
    return soapSortKeys;
  }

  private SoapFilter[] soapFiltersBuilder(final LinkedHashMap<String, String> filters,
      final LinkedHashMap<String, TrackerFieldSoapDO> trackerFlexFieldsData) {
    if (filters == null || trackerFlexFieldsData == null) {
      return null;
    }
    final ArrayList<SoapFilter> soapFilterList = new ArrayList<SoapFilter>();
    for (String filterKey: filters.keySet()) {
      final String filterValue = filters.get(filterKey);
      if (filterKey.equalsIgnoreCase("category")) {
        soapFilterList.add(new SoapFilter(ArtifactSoapDO.FILTER_CATEGORY, filterValue));
      }
      else if (filterKey.equalsIgnoreCase("status")) {
        if (filterValue.equalsIgnoreCase("all open")) {
          soapFilterList.add(new SoapFilter(ArtifactSoapDO.FILTER_STATUS_CLASS, "Open"));
        }
        else if (filterValue.equalsIgnoreCase("all close")) {
          soapFilterList.add(new SoapFilter(ArtifactSoapDO.FILTER_STATUS_CLASS, "Close"));
        }
        else {
          final String[] statuses = filterValue.split(",");
          for (String status: statuses) {
            status = status.trim();
            if (status.isEmpty()) {
              continue;
            }
            soapFilterList.add(new SoapFilter(ArtifactSoapDO.FILTER_STATUS, status));
          }
        }
      }
      else if (filterKey.equalsIgnoreCase("submittedAfter")) {
        soapFilterList.add(new SoapFilter(ArtifactSoapDO.FILTER_SUBMITTED_AFTER, filterValue));
      }
      else {
        // ##### flex fields #####
        final TrackerFieldSoapDO trackerFieldSoapDO = trackerFlexFieldsData.get(filterKey);
        if (trackerFieldSoapDO == null) {
          stdLogger.println("==> Oops, invalid filter field [" + filterKey + "]");
          errLogger.println("==> Oops, invalid filter field [" + filterKey + "]");
          continue;
        }
        final String trackerFlexFieldType = trackerFieldSoapDO.getFieldType();
        if (trackerFlexFieldType.equals(TrackerFieldSoapDO.FIELD_TYPE_TEXT)) {
          soapFilterList.add(new SoapFilter(filterKey, filterValue));
        }
        else if (trackerFlexFieldType.equals(TrackerFieldSoapDO.FIELD_TYPE_SINGLE_SELECT)) {
          final String[] flexFieldValues = filterValue.split(",");
          for (String flexFieldValue: flexFieldValues) {
            flexFieldValue = flexFieldValue.trim();
            if (flexFieldValue.isEmpty()) {
              continue;
            }
            final String flexFieldValueId = getTrackerFlexFieldValueId(flexFieldValue, trackerFieldSoapDO);
            if (flexFieldValueId == null) {
              System.out.println("==> Oops, invalid filter field value [" + filterKey + "] [" + flexFieldValue + "]");
              errLogger.println("==> Oops, invalid filter field value [" + filterKey + "] [" + flexFieldValue + "]");
              continue;
            }
            soapFilterList.add(new SoapFilter(filterKey, flexFieldValueId));
          }
        }
        else {
          stdLogger.println("==> Oops, invalid filter field [" + filterKey + "]");
          errLogger.println("==> Oops, invalid filter field [" + filterKey + "]");
          continue;
        }
      }
    }
    SoapFilter[] soapFilters = new SoapFilter[soapFilterList.size()];
    soapFilters = soapFilterList.toArray(soapFilters);
    return soapFilters;
  }

  public ArtifactDetailList getArtifactDetailList(final String projectName, final String trackerName,
      final LinkedHashMap<String, String> filters, final LinkedHashMap<String, Boolean> sortKeys) {
    final String trackerId = getTrackerId(projectName, trackerName);
    if (trackerId == null) {
      System.out.println("==> Oops, failed to get tracker id [" + trackerName + "] [" + projectName + "]");
      errLogger.println("==> Oops, failed to get tracker id [" + trackerName + "] [" + projectName + "]");
      return null;
    }
    LinkedHashMap<String, TrackerFieldSoapDO> trackerFlexFieldsData = getTrackerFlexFieldsData(trackerId);
    SoapSortKey[] soapSortKeys = soapSortKeysBuilder(sortKeys);
    SoapFilter[] soapFilters = soapFiltersBuilder(filters, trackerFlexFieldsData);
    try {
      ArtifactDetailSoapList artifactDetailSoapList = ctfTrackerSoap.getArtifactDetailList(ctfSessionId, trackerId, null, soapFilters, soapSortKeys, 0, -1, false, true);
      return new ArtifactDetailList(artifactDetailSoapList);
    }
    catch (RemoteException e) {
      System.out.println("==> Oops, failed to get artifact detail list [" + trackerName + "] [" + projectName + "] [" + e.getMessage() + "]");
      errLogger.println("==> Oops, failed to get artifact detail list [" + trackerName + "] [" + projectName + "] [" + e.getMessage() + "]");
      return null;
    }
  }

  // ##### static inner class #####
  public static final class ArtifactDetailList {
    public ArtifactDetailList(final ArtifactDetailSoapList artifactDetailSoapList) {
      artifactDetailList = artifactDetailSoapList.getDataRows();
    }

    public int size() {
      return artifactDetailList.length;
    }

    public String getId(final int idx) {
      if (idx >= artifactDetailList.length) {
        return null;
      }
      return artifactDetailList[idx].getId();
    }

    public String getTitle(final int idx) {
      if (idx >= artifactDetailList.length) {
        return null;
      }
      return artifactDetailList[idx].getTitle();
    }

    public int getPriority(final int idx) {
      if (idx >= artifactDetailList.length) {
        return -1;
      }
      return artifactDetailList[idx].getPriority();
    }

    public String getPriorityDetails(final int idx) {
      if (idx >= artifactDetailList.length) {
        return null;
      }
      switch (artifactDetailList[idx].getPriority()) {
        case 1:
          return "1-Highest";
        case 2:
          return "2-High";
        case 3:
          return "3-Medium";
        case 4:
          return "4-Low";
        default:
          return null;
      }
    }

    public String getStatus(final int idx) {
      if (idx >= artifactDetailList.length) {
        return null;
      }
      return artifactDetailList[idx].getStatus();
    }

    public Date getSubmittedDate(final int idx) {
      if (idx >= artifactDetailList.length) {
        return null;
      }
      return artifactDetailList[idx].getSubmittedDate();
    }

    public String getProjectTitle(final int idx) {
      if (idx >= artifactDetailList.length) {
        return null;
      }
      return artifactDetailList[idx].getProjectTitle();
    }

    private final ArtifactDetailSoapRow[] artifactDetailList;
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
