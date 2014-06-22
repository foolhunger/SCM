package com.ebao.ctf.plugins;

import java.util.ArrayList;
import java.util.HashMap;

public class ReleaseReportBuilder {
  public ReleaseReportBuilder() {
    ctfProject = System.getProperty("ctf.project", "").trim();
    if (ctfProject.isEmpty()) {
      System.out.println("==> Oops, [ctf.project] must be specified");
      System.exit(-1);
    }
    ctfPackage = System.getProperty("ctf.package", "").trim();
    if (ctfPackage.isEmpty()) {
      System.out.println("==> Oops, [ctf.package] must be specified");
      System.exit(-1);
    }
    ctfRelease = System.getProperty("ctf.release", "").trim();
    if (ctfRelease.isEmpty()) {
      System.out.println("==> Oops, [ctf.release] must be specified");
      System.exit(-1);
    }

    rptHeaders = System.getProperty("rpt.headers", "").trim().split(";");
    ArrayList<String> headers = new ArrayList<>();
    for (String rptHeader: rptHeaders) {
      rptHeader = rptHeader.trim();
      if (!rptHeader.isEmpty()) {
        headers.add(rptHeader);
      }
    }
    if (headers.isEmpty()) {
      rptHeaders = new String[] {"Artifact ID", "Title", "Category", "Priority", "Status", "Assigned To", "Submitted By", "SCM Files", "SCM Actions"};
    }
    else {
      rptHeaders = new String[headers.size()];
      rptHeaders = headers.toArray(rptHeaders);
    }

    scmFilePrefix = System.getProperty("scmfile.prefix", "").trim();
    scmFilePrefix = scmFilePrefix.replaceAll("\\\\", "/");
    if (!scmFilePrefix.isEmpty() && !scmFilePrefix.endsWith("/")) {
      scmFilePrefix += "/";
    }

    String wbookName = System.getProperty("rpt.wbook.name", "").trim();
    if (wbookName.isEmpty()) {
      rptWbookName = ctfProject + "-" + ctfRelease + ".xls";
    }
    else {
      rptWbookName = wbookName + ".xls";
    }
    String sheetName = System.getProperty("rpt.sheet.name", "").trim();
    if (sheetName.isEmpty()) {
      rptSheetName = ctfRelease;
    }
    else {
      rptSheetName = sheetName;
    }
  }

  private boolean isReportArtifactScmFilesOrActions() {
    for (String rptHeader: rptHeaders) {
      rptHeader = rptHeader.trim();
      if (rptHeader.equals("SCM Files") || rptHeader.equals("SCM Actions")) {
        return true;
      }
    }
    return false;
  }

  public void build() {
    ReleaseReportCTFUtils ctfUtils = new ReleaseReportCTFUtils();
    ReleaseReportWorkbookUtils wbUtils = new ReleaseReportWorkbookUtils(rptWbookName, rptSheetName);
    wbUtils.writeHeaders(rptHeaders);
    ctfUtils.login();
    System.out.println("==> Building release report [" + ctfProject + "->" + ctfPackage + "->" + ctfRelease +"]");
    final boolean isReportArtfScmFilesOrActions = isReportArtifactScmFilesOrActions();
    ArrayList<String> artfListFixedInRelease = ctfUtils.getArtifactListFixedInRelease(ctfProject, ctfPackage, ctfRelease);
    HashMap<String, String> artfFlexFieldsMap = null;
    HashMap<String, String> artfScmFileActionsMap = null;
    for (int iRow = 0; iRow < artfListFixedInRelease.size(); ++iRow) {
      String artfId = artfListFixedInRelease.get(iRow).trim();
      System.out.println(">>>>> Writing artifact [" + artfId + "] to release report [" + (iRow + 1) + "]");
      artfFlexFieldsMap = null;
      if (isReportArtfScmFilesOrActions) {
        artfScmFileActionsMap = ctfUtils.getArtifactScmFileActionsMap(artfId);
      }
      for (int iCol = 0; iCol < rptHeaders.length; ++iCol) {
        String rptHeader = rptHeaders[iCol].trim();
        switch (rptHeader) {
          case "Artifact ID":
            wbUtils.writeArtifactIdToCell(iRow + 1, iCol, artfId);
            break;
          case "Title":
            String artfTitle = ctfUtils.getArtifactTitle(artfId);
            wbUtils.writeArtifactTitleToCell(iRow + 1, iCol, artfTitle);
            break;
          case "Priority":
            int artfPriority = ctfUtils.getArtifactPriority(artfId);
            wbUtils.writeArtifactPriorityToCell(iRow + 1, iCol, artfPriority);
            break;
          case "Category":
            String artfCategory = ctfUtils.getArtifactCategory(artfId);
            wbUtils.writeArtifactCategoryToCell(iRow + 1, iCol, artfCategory);
            break;
          case "Status":
            String artfStatus = ctfUtils.getArtifactStatus(artfId);
            wbUtils.writeArtifactStatusToCell(iRow + 1, iCol, artfStatus);
            break;
          case "Assigned To":
            String artfAssignedToFullname = ctfUtils.getArtifactAssignedToFullname(artfId);
            wbUtils.writeArtifactAssignedToFullnameToCell(iRow + 1, iCol, artfAssignedToFullname);
            break;
          case "Submitted By":
            String artfSubmittedByFullname = ctfUtils.getArtifactSubmittedByFullname(artfId);
            wbUtils.writeArtifactSubmittedByFullnameToCell(iRow + 1, iCol, artfSubmittedByFullname);
            break;
          case "SCM Files":
            wbUtils.writeArtifactScmFilesToCell(iRow + 1, iCol, artfScmFileActionsMap, scmFilePrefix);
            break;
          case "SCM Actions":
            wbUtils.writeArtifactScmActionsToCell(iRow + 1, iCol, artfScmFileActionsMap);
            break;
          // flex field
          default:
            if (artfFlexFieldsMap == null) {
              artfFlexFieldsMap = ctfUtils.getArtifactFlexFieldsMap(artfId);
            }
            String artfFlexFieldValue = "";
            if (artfFlexFieldsMap.containsKey(rptHeader)) {
              artfFlexFieldValue = artfFlexFieldsMap.get(rptHeader);
            }
            wbUtils.writeArtifactFlexFieldValueToCell(iRow + 1, iCol, artfFlexFieldValue);
            break;
        }
      }
    }
    ctfUtils.logoff();
    for (int i = 0; i < rptHeaders.length; ++i) {
      wbUtils.autoSizeColumn(i);
    }
    wbUtils.close();
  }

  public static void main(String[] args) {
    long startTime = System.currentTimeMillis();
    ReleaseReportBuilder rrBuilder = new ReleaseReportBuilder();
    rrBuilder.build();
    long endTime = System.currentTimeMillis();
    double elapsedTime = (endTime - startTime) / 1000.0 / 60.0;
    System.out.printf("==> Time elapsed: [%.2f] mins", elapsedTime);
  }

  private final String ctfProject;
  private final String ctfPackage;
  private final String ctfRelease;
  private String scmFilePrefix;
  private String[] rptHeaders;
  private final String rptWbookName;
  private final String rptSheetName;
}