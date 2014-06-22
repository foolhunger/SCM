package com.ebao.ctf.plugins;

import java.io.InputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Properties;
import org.apache.axis.AxisFault;
import com.collabnet.ce.soap60.types.SoapFieldValues;
import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.AssociationSoapList;
import com.collabnet.ce.soap60.webservices.cemain.AssociationSoapRow;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapList;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap60.webservices.cemain.UserSoapDO;
import com.collabnet.ce.soap60.webservices.frs.IFrsAppSoap;
import com.collabnet.ce.soap60.webservices.frs.PackageSoapList;
import com.collabnet.ce.soap60.webservices.frs.PackageSoapRow;
import com.collabnet.ce.soap60.webservices.frs.ReleaseSoapList;
import com.collabnet.ce.soap60.webservices.frs.ReleaseSoapRow;
import com.collabnet.ce.soap60.webservices.scm.CommitSoapDO;
import com.collabnet.ce.soap60.webservices.scm.IScmAppSoap;
import com.collabnet.ce.soap60.webservices.scm.ScmFileSoapList;
import com.collabnet.ce.soap60.webservices.scm.ScmFileSoapRow;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactSoapList;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactSoapRow;
import com.collabnet.ce.soap60.webservices.tracker.ITrackerAppSoap;

public class ReleaseReportCTFUtils {
  public ReleaseReportCTFUtils() {
    InputStream ins = ReleaseReportCTFUtils.class.getResourceAsStream("ReleaseReportBuilder.properties");
    if (ins == null) {
      System.out.println("==> Oops, property file [ReleaseReportBuilder.properties] does not exist");
      System.exit(-1);
    }
    Properties props = new Properties();
    try {
      props.load(ins);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to load properties from file [ReleaseReportBuilder.properties]");
      e.printStackTrace();
      System.exit(-1);
    }
    ctfURL = props.getProperty("ctf.url", "").trim();
    if (ctfURL.isEmpty()) {
      System.out.println("==> Oops, failed to get property [ctf.url] [ReleaseReportBuilder.properties]");
      System.exit(-1);
    }
    ctfUsername = props.getProperty("ctf.username", "").trim();
    if (ctfUsername.isEmpty()) {
      System.out.println("==> Oops, failed to get property [ctf.username] [ReleaseReportBuilder.properties]");
      System.exit(-1);
    }
    ctfPassword = props.getProperty("ctf.password", "").trim();
    if (ctfPassword.isEmpty()) {
      System.out.println("==> Oops, failed to get property [ctf.password] [ReleaseReportBuilder.properties]");
      System.exit(-1);
    }
    artfIdSoapRowMap = new HashMap<String, ArtifactSoapRow>(50);
    // instantiate CTF soap objects
    ICollabNetSoap cemainSoap = null;
    IScmAppSoap scmAppSoap = null;
    IFrsAppSoap frsAppSoap = null;
    ITrackerAppSoap trackerSoap = null;
    try {
      cemainSoap = (ICollabNetSoap) ClientSoapStubFactory.getSoapStub(ICollabNetSoap.class, ctfURL);
      scmAppSoap = (IScmAppSoap) ClientSoapStubFactory.getSoapStub(IScmAppSoap.class, ctfURL);
      frsAppSoap = (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, ctfURL);
      trackerSoap = (ITrackerAppSoap) ClientSoapStubFactory.getSoapStub(ITrackerAppSoap.class, ctfURL);
    }
    catch (Throwable e) {
      System.out.println("==> Oops, failed to instantiate CTF soap objects");
      e.printStackTrace();
      System.exit(-1);
    }
    ctfCemainSoap = cemainSoap;
    ctfScmAppSoap = scmAppSoap;
    ctfFrsAppSoap = frsAppSoap;
    ctfTrackerSoap = trackerSoap;
  }

  public void login() {
    try {
      ctfSessionId = ctfCemainSoap.login(ctfUsername, ctfPassword);
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to login to Teamforge [" + ctfURL + "] [" + ctfUsername + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    System.out.println("==> Teamforge: logged in as user [" + ctfUsername + "]");
  }

  public void logoff() {
    try {
      ctfCemainSoap.logoff(ctfUsername, ctfSessionId);
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to logoff from Teamforge [" + ctfURL + "] [" + ctfUsername + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    System.out.println("==> Teamforge: logged off as user [" + ctfUsername + "]");
  }

  private String getProjectId(final String ctfProject) {
    try {
      ProjectSoapList projSoapList = ctfCemainSoap.getProjectList(ctfSessionId, false);
      ProjectSoapRow[] projSoapRows = projSoapList.getDataRows();
      for (ProjectSoapRow projSoapRow: projSoapRows) {
        if (projSoapRow.getTitle() != null && projSoapRow.getTitle().equals(ctfProject)) {
          return projSoapRow.getId();
        }
      }
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve project id [" + ctfProject + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return null;
  }

  private String getPackageId(final String ctfPackage, final String ctfProjectId) {
    try {
      PackageSoapList pkgSoapList = ctfFrsAppSoap.getPackageList(ctfSessionId, ctfProjectId);
      PackageSoapRow[] pkgSoapRows = pkgSoapList.getDataRows();
      for (PackageSoapRow pkgSoapRow: pkgSoapRows) {
        if (pkgSoapRow.getTitle() != null && pkgSoapRow.getTitle().equals(ctfPackage)) {
          return pkgSoapRow.getId();
        }
      }
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve package id [" + ctfPackage + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return null;
  }

  private String getReleaseId(final String ctfRelease, final String ctfPackageId) {
    try {
      ReleaseSoapList relSoapList = ctfFrsAppSoap.getReleaseList(ctfSessionId, ctfPackageId);
      ReleaseSoapRow[] relSoapRows = relSoapList.getDataRows();
      for (ReleaseSoapRow relSoapRow: relSoapRows) {
        if (relSoapRow.getTitle() != null && relSoapRow.getTitle().equals(ctfRelease)) {
          return relSoapRow.getId();
        }
      }
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve release id [" + ctfRelease + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return null;
  }

  public ArrayList<String> getArtifactListFixedInRelease(final String ctfProject, final String ctfPackage, final String ctfRelease) {
    final String ctfProjectId = getProjectId(ctfProject);
    if (ctfProjectId == null) {
      System.out.println("==> Oops, failed to retrieve project id [" + ctfProject + "]");
      System.exit(-1);
    }
    final String ctfPackageId = getPackageId(ctfPackage, ctfProjectId);
    if (ctfPackageId == null) {
      System.out.println("==> Oops, failed to retrieve package id [" + ctfPackage + "]");
      System.exit(-1);
    }
    final String ctfReleaseId = getReleaseId(ctfRelease, ctfPackageId);
    if (ctfReleaseId == null) {
      System.out.println("==> Oops, failed to retrieve release id [" + ctfRelease + "]");
      System.exit(-1);
    }
    ArrayList<String> artfListFixedInRelease = new ArrayList<>(50);
    try {
      ArtifactSoapList artfSoapList = ctfFrsAppSoap.getArtifactListResolvedInRelease(ctfSessionId, ctfReleaseId);
      ArtifactSoapRow[] artfSoapRows = artfSoapList.getDataRows();
      for (ArtifactSoapRow artfSoapRow: artfSoapRows) {
        artfListFixedInRelease.add(artfSoapRow.getId());
        artfIdSoapRowMap.put(artfSoapRow.getId(), artfSoapRow);
      }
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve artifact list fixed in release [" + ctfProject + "->" + ctfPackage + "->" + ctfRelease + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return artfListFixedInRelease;
  }

  public String getArtifactTitle(final String artfId) {
    ArtifactSoapRow artfSoapRow = artfIdSoapRowMap.get(artfId);
    if (artfSoapRow == null) {
      System.out.println("==> Oops, no mapping [artfSoapRow] value for key [" + artfId +"]");
      System.exit(-1);
    }
    return artfSoapRow.getTitle();
  }

  public int getArtifactPriority(final String artfId) {
    ArtifactSoapRow artfSoapRow = artfIdSoapRowMap.get(artfId);
    if (artfSoapRow == null) {
      System.out.println("==> Oops, no mapping [artfSoapRow] value for key [" + artfId +"]");
      System.exit(-1);
    }
    return artfSoapRow.getPriority();
  }

  public String getArtifactCategory(final String artfId) {
    ArtifactSoapRow artfSoapRow = artfIdSoapRowMap.get(artfId);
    if (artfSoapRow == null) {
      System.out.println("==> Oops, no mapping [artfSoapRow] value for key [" + artfId +"]");
      System.exit(-1);
    }
    return artfSoapRow.getCategory();
  }

  public String getArtifactStatus(final String artfId) {
    ArtifactSoapRow artfSoapRow = artfIdSoapRowMap.get(artfId);
    if (artfSoapRow == null) {
      System.out.println("==> Oops, no mapping [artfSoapRow] value for key [" + artfId +"]");
      System.exit(-1);
    }
    return artfSoapRow.getStatus();
  }

  public String getArtifactAssignedToFullname(final String artfId) {
    ArtifactSoapRow artfSoapRow = artfIdSoapRowMap.get(artfId);
    if (artfSoapRow == null) {
      System.out.println("==> Oops, no mapping [artfSoapRow] value for key [" + artfId +"]");
      System.exit(-1);
    }
    return artfSoapRow.getAssignedToFullname();
  }

  public String getArtifactSubmittedByFullname(final String artfId) {
    ArtifactSoapRow artfSoapRow = artfIdSoapRowMap.get(artfId);
    if (artfSoapRow == null) {
      System.out.println("==> Oops, no mapping [artfSoapRow] value for key [" + artfId +"]");
      System.exit(-1);
    }
    return artfSoapRow.getSubmittedByFullname();
  }

  public HashMap<String, String> getArtifactFlexFieldsMap(final String artfId) {
    HashMap<String, String> artfFlexFieldsMap = new HashMap<>(50);
    try {
      ArtifactSoapDO artfSoapDo = ctfTrackerSoap.getArtifactData(ctfSessionId, artfId);
      SoapFieldValues soapFieldValues = artfSoapDo.getFlexFields();
      String[] ffNames = soapFieldValues.getNames();
      String[] ffTypes = soapFieldValues.getTypes();
      Object[] ffValues = soapFieldValues.getValues();
      for (int i = 0; i < ffNames.length; ++i) {
        String ffName = ffNames[i];
        String ffType = ffTypes[i];
        Object ffValue = ffValues[i];
        if ((ffName == null) || (ffValue == null)) {
          continue;
        }
        if (ffType.equalsIgnoreCase("Date")) {
          int year = ((GregorianCalendar) ffValue).get(GregorianCalendar.YEAR);
          int month = ((GregorianCalendar) ffValue).get(GregorianCalendar.MONTH) + 1;
          int day = ((GregorianCalendar) ffValue).get(GregorianCalendar.DAY_OF_MONTH) + 1;
          ffValue = String.format("%04d-%02d-%02d", year, month, day);
        }
        else if (ffType.equalsIgnoreCase("User")) {
          ffValue = getUserFullName(ffValue.toString());
        }
        else if (ffType.equalsIgnoreCase("String")) {
          // do nothing
        }
        else {
          System.out.println("==> Oops, unknown flex field type [" + ffName + "] [" + ffType + "] [" + ffValue +"]");
        }

        if (artfFlexFieldsMap.containsKey(ffName)) {
          ffValue = artfFlexFieldsMap.get(ffName) + "\n" + ffValue.toString();
        }
        artfFlexFieldsMap.put(ffName, ffValue.toString());
      }
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve artifact flex fields [" + artfId + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return artfFlexFieldsMap;
  }

  private String getUserFullName(final String username) {
    String userFullName = null;
    try {
      UserSoapDO userSoapDo = ctfCemainSoap.getUserData(ctfSessionId, username);
      userFullName = userSoapDo.getFullName();
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve user full name [" + username +"]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return userFullName;
  }

  private ArrayList<String> getArtifactCommitIds(final String artfId) {
    ArrayList<String> artfCommitIds = new ArrayList<>(20);
    try {
      AssociationSoapList assoSoapList = ctfCemainSoap.getAssociationList(ctfSessionId, artfId);
      AssociationSoapRow[] assoSoapRows = assoSoapList.getDataRows();
      for (AssociationSoapRow assoSoapRow: assoSoapRows) {
        String assoTargetId = assoSoapRow.getTargetId();
        if (assoTargetId != null && assoTargetId.startsWith("cmmt")) {
          artfCommitIds.add(assoTargetId);
        }
      }
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve artifact commit ids [" + artfId + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return artfCommitIds;
  }

  public HashMap<String, String> getArtifactScmFileActionsMap(final String artfId) {
    HashMap<String, String> artfScmFileActionsMap = new HashMap<>(50);
    ArrayList<String> artfCommitIds = getArtifactCommitIds(artfId);
    try {
      for (String artfCommitId: artfCommitIds) {
        CommitSoapDO commitSoapDo = ctfScmAppSoap.getCommitData(ctfSessionId, artfCommitId);
        ScmFileSoapList scmFileSoapList = commitSoapDo.getFiles();
        ScmFileSoapRow[] scmFileSoapRows = scmFileSoapList.getDataRows();
        lblScmFileSoapRow:
        for (ScmFileSoapRow scmFileSoapRow: scmFileSoapRows) {
          String scmFileName = scmFileSoapRow.getFilename();
          String scmFileStatus = scmFileSoapRow.getStatus();
          if (artfScmFileActionsMap.containsKey(scmFileName)) {
            String[] scmFileActions = artfScmFileActionsMap.get(scmFileName).split(";");
            for (String scmFileAction: scmFileActions) {
              if (scmFileAction.equals(scmFileStatus)) {
                continue lblScmFileSoapRow;
              }
            }
            scmFileStatus = artfScmFileActionsMap.get(scmFileName) + ";" + scmFileStatus;
          }
          artfScmFileActionsMap.put(scmFileName, scmFileStatus);
        }
      }
    }
    catch (RemoteException e) {
      String faultString = ((AxisFault)e).getFaultString();
      System.out.println("==> Oops, failed to retrieve artifact scm files and actions [" + artfId + "]");
      System.out.println("==> Cause: " + faultString);
      e.printStackTrace();
      System.exit(-1);
    }
    return artfScmFileActionsMap;
  }

  private final String ctfURL;
  private final String ctfUsername;
  private final String ctfPassword;
  private String ctfSessionId;
  private final ICollabNetSoap ctfCemainSoap;
  private final IScmAppSoap ctfScmAppSoap;
  private final IFrsAppSoap ctfFrsAppSoap;
  private final ITrackerAppSoap ctfTrackerSoap;
  private final HashMap<String, ArtifactSoapRow> artfIdSoapRowMap;
}