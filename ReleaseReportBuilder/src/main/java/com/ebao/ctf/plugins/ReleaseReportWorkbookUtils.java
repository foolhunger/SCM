package com.ebao.ctf.plugins;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;

public class ReleaseReportWorkbookUtils {
  public ReleaseReportWorkbookUtils(final String wbTitle, final String shtTitle) {
    this.wbook = new HSSFWorkbook();
    this.wbTitle = wbTitle;
    this.wbSheet = wbook.createSheet(WorkbookUtil.createSafeSheetName(shtTitle, '-'));
  }

  public void writeHeaders(final String[] rptHeaders) {
    Row row = wbSheet.getRow(0);
    if (row == null) {
      row = wbSheet.createRow(0);
    }
    row.setHeightInPoints(row.getHeightInPoints() * 3F);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(true);
    for (int i = 0; i < rptHeaders.length; ++i) {
      Cell cell = row.createCell(i);
      cell.setCellValue(rptHeaders[i].trim());
      cell.setCellStyle(cellStyle);
    }
  }

  public void autoSizeColumn(final int column) {
    wbSheet.autoSizeColumn(column);
  }

  public void close() {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(wbTitle);
    }
    catch (FileNotFoundException e) {
      System.out.println("==> Oops, failed to create workbook output stream [" + wbTitle + "]");
      e.printStackTrace();
      System.exit(-1);
    }
    try {
      wbook.write(fos);
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to write to workbook [" + wbTitle + "]");
      e.printStackTrace();
      System.exit(-1);
    }
    try {
      fos.close();
    }
    catch (IOException e) {
      System.out.println("==> Oops, failed to close workbook output stream [" + wbTitle + "]");
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public void writeArtifactIdToCell(final int iRow, final int iCol, final String artfId) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellValue(artfId);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(false);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactTitleToCell(final int iRow, final int iCol, final String artfTitle) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellValue(artfTitle);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(true);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactPriorityToCell(final int iRow, final int iCol, final int artfPriority) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellType(Cell.CELL_TYPE_NUMERIC);
    cell.setCellValue(artfPriority);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(false);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactCategoryToCell(final int iRow, final int iCol, final String artfCategory) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellValue(artfCategory);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.TAN.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(false);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactStatusToCell(final int iRow, final int iCol, final String artfStatus) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellValue(artfStatus);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(false);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactAssignedToFullnameToCell(final int iRow, final int iCol, final String artfAssignedToFullname) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellValue(artfAssignedToFullname);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(false);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactSubmittedByFullnameToCell(final int iRow, final int iCol, final String artfSubmittedByFullname) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellValue(artfSubmittedByFullname);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(false);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactFlexFieldValueToCell(final int iRow, final int iCol, final String artfFlexFieldValue) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    cell.setCellValue(artfFlexFieldValue);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(true);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactScmFilesToCell(final int iRow, final int iCol, final HashMap<String, String> artfScmFileActionsMap, final String scmFilePrefix) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    StringBuilder sbArtfScmFiles = new StringBuilder(artfScmFileActionsMap.size() * 50);
    for (String artfScmFile: artfScmFileActionsMap.keySet()) {
      if (sbArtfScmFiles.length() != 0) {
        sbArtfScmFiles.append("\n");
      }
      if (!scmFilePrefix.isEmpty() && artfScmFile.startsWith(scmFilePrefix)) {
        artfScmFile = artfScmFile.substring(scmFilePrefix.length());
      }
      sbArtfScmFiles.append(artfScmFile);
    }
    if (sbArtfScmFiles.length() > 32767) {
      sbArtfScmFiles = sbArtfScmFiles.delete(32760, sbArtfScmFiles.length() + 1);
      sbArtfScmFiles.append("\n......");
    }
    sbArtfScmFiles.trimToSize();
    cell.setCellValue(sbArtfScmFiles.toString());
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(true);
    cell.setCellStyle(cellStyle);
  }

  public void writeArtifactScmActionsToCell(final int iRow, final int iCol, final HashMap<String, String> artfScmFileActionsMap) {
    Row row = wbSheet.getRow(iRow);
    if (row == null) {
      row = wbSheet.createRow(iRow);
    }
    Cell cell = row.getCell(iCol);
    if (cell != null) {
      System.out.println("==> Oops, cell already exists [" + iRow + "] [" + iCol +"]");
      System.exit(-1);
    }
    cell = row.createCell(iCol);
    String artfScmActions = "";
    for (String artfScmAction: artfScmFileActionsMap.values()) {
      if (!artfScmActions.isEmpty()) {
        artfScmActions += "\n";
      }
      artfScmActions += artfScmAction;
    }
    cell.setCellValue(artfScmActions);
    CellStyle cellStyle = wbook.createCellStyle();
    cellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
    cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    cellStyle.setBorderTop(CellStyle.BORDER_THIN);
    cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
    cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
    cellStyle.setBorderRight(CellStyle.BORDER_THIN);
    cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
    cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    cellStyle.setWrapText(true);
    cell.setCellStyle(cellStyle);
  }

  private final String wbTitle;
  private final HSSFWorkbook wbook;
  private final Sheet wbSheet;
}