/*
 * $Id: SimpleReportBusinessBean.java,v 1.6 2005/07/26 17:31:47 thomas Exp $
 * Created on 21.9.2004
 *
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.block.datareport.business;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.idega.block.datareport.util.ReportDescription;
import com.idega.block.datareport.util.ReportableField;
import com.idega.business.IBOServiceBean;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;
import com.idega.util.text.TextSoap;

import net.sf.jasperreports.engine.JRDataSource;


/**
 *
 *  Last modified: $Date: 2005/07/26 17:31:47 $ by $Author: thomas $
 *
 * @author <a href="mailto:gummi@idega.com">Gudmundur Agust Saemundsson</a>
 * @version $Revision: 1.6 $
 */
public class SimpleReportBusinessBean extends IBOServiceBean implements SimpleReportBusiness {

	public final static String NAME_OF_REPORT = "Report";
	public final static String REPORT_FONT = "Courier New";
	private static final String FIELD_NAME_COMPARING_YEAR = "comparing_year";

	/**
	 *
	 */
	public SimpleReportBusinessBean() {
		super();
	}



	@Override
	public void writeSimpleExcelFile(JRDataSource reportData, String nameOfReport, String filePathAndName, ReportDescription description) throws IOException{
		if(nameOfReport==null || "".equals(nameOfReport)){
			nameOfReport = NAME_OF_REPORT;
		}
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet(TextSoap.encodeToValidExcelSheetName(nameOfReport));
	    int rowIndex = 0;


	    //-- Report Name --//
	    // Create a row and put some cells in it. Rows are 0 based.
	    HSSFRow row = sheet.createRow((short)rowIndex++);
	    // Create a cell and put a value in it.
	    HSSFCell cell = row.createCell((short)0);

	    // Create a new font and alter it.
	    HSSFFont font = wb.createFont();
	    font.setFontHeightInPoints((short)24);
	    font.setFontName(REPORT_FONT);
	    font.setItalic(true);
	    font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);

	    // Fonts are set into a style so create a new one to use.
	    HSSFCellStyle style = wb.createCellStyle();
	    style.setFont(font);

	    // Create a cell and put a value in it.
	    cell.setCellValue(nameOfReport);
	    cell.setCellStyle(style);


	    //-- Report Parameters --//
	    rowIndex++;
	    HSSFRow row1 = null;
	    String parameterString = "";
	    List labels = description.getListOfHeaderParameterLabelKeys();
	    List parameters = description.getListOfHeaderParameterKeys();
	    Iterator labelIter = labels.iterator();
	    Iterator parameterIter = parameters.iterator();
	    boolean newLineForeEachParameter = description.doCreateNewLineForEachParameter();
	    while (labelIter.hasNext() && parameterIter.hasNext()) {
			String label = description.getParameterOrLabelName((String)labelIter.next());
			String parameter = description.getParameterOrLabelName((String)parameterIter.next());
			if(newLineForeEachParameter){
				row1 = sheet.createRow((short)rowIndex++);
				row1.createCell((short)0).setCellValue(label + " "+parameter);
			} else {
				parameterString += label + " "+parameter+"      ";
			}
		}
	    if(!newLineForeEachParameter){
		    row1 = sheet.createRow((short)rowIndex++);
		    row1.createCell((short)0).setCellValue(parameterString);
	    }
	    rowIndex++;

	    //-- Report ColumnHeader --//
	    List fields = description.getListOfFields();
	    HSSFRow headerRow = sheet.createRow((short)rowIndex++);

	 	HSSFCellStyle headerCellStyle = wb.createCellStyle();

	 	headerCellStyle.setWrapText( true );
	 	headerCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
	 	headerCellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);

	    HSSFFont headerCellFont = wb.createFont();
	    //headerCellFont.setFontHeightInPoints((short)12);
	    headerCellFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
	 	headerCellStyle.setFont(headerCellFont);

		int colIndex = 0;
		int columnWithUnit = 256; // the unit is 1/256 of a character
		int numberOfCharactersPerLineInLongTextFields = 60;
		int numberOfCharactersPerLineInRatherLongTextFields = 35;
		int numberOfCharactersPerLineInUndifinedTextFields = 20;

	    for (Iterator iter = fields.iterator(); iter.hasNext();colIndex++) {
			ReportableField field = (ReportableField) iter.next();

			//column width
			int fieldsMaxChar = field.getMaxNumberOfCharacters();
			int colWith = numberOfCharactersPerLineInRatherLongTextFields*columnWithUnit;  //default, can be rather long text
			if(fieldsMaxChar > 0 && fieldsMaxChar < numberOfCharactersPerLineInRatherLongTextFields){
				colWith = (fieldsMaxChar+1)*columnWithUnit;  // short fields
			} else if(fieldsMaxChar > 500){ // when the field is set to be able to contain very long text
				colWith = numberOfCharactersPerLineInLongTextFields*columnWithUnit; //can be very long text
			} else if(fieldsMaxChar < 0){
				colWith = numberOfCharactersPerLineInUndifinedTextFields*columnWithUnit;
			}

//			//Add ALLS field before FIELD_NAME_COMPARING_YEAR
//			if (field.getName().equalsIgnoreCase(FIELD_NAME_COMPARING_YEAR)) {
//				HSSFCell headerCellAlls = headerRow.createCell((short)colIndex);
//				headerCellAlls.setCellValue("Alls");
//				headerCellAlls.setCellStyle(headerCellStyle);
//				sheet.setColumnWidth((short)colIndex,(short)colWith);
//				colIndex++;
//			}

			HSSFCell headerCell = headerRow.createCell((short)colIndex);
			headerCell.setCellValue(description.getColumnName(field));
			headerCell.setCellStyle(headerCellStyle);
			sheet.setColumnWidth((short)colIndex,(short)colWith);

//			//Add ALLS field in case this is the last field
//			if (!iter.hasNext() && !field.getName().equalsIgnoreCase(FIELD_NAME_COMPARING_YEAR)) {
//				HSSFCell headerCellAlls = headerRow.createCell((short)colIndex);
//				headerCellAlls.setCellValue("Alls");
//				headerCellAlls.setCellStyle(headerCellStyle);
//				sheet.setColumnWidth((short)colIndex,(short)colWith);
//				colIndex++;
//			}
		}

	    //-- Report ColumnDetail --//
	    try {
	    	 	HSSFCellStyle dataCellStyle = wb.createCellStyle();
	    	 	dataCellStyle.setWrapText( true );
	    	 	dataCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
	    	 	sheet.createFreezePane( 0, rowIndex );

	    	 	//Save the data into the hashmap
	    	 	Map<String, Integer> totalsBottomData = new HashMap<String, Integer>();

				while(reportData.next()) {
					HSSFRow dataRow = sheet.createRow((short)rowIndex++);
					colIndex = 0;
					for (Iterator iter = fields.iterator(); iter.hasNext();colIndex++) {
						ReportableField field = (ReportableField) iter.next();
						HSSFCell dataCell = dataRow.createCell((short)colIndex);
						Object fieldValue = reportData.getFieldValue(field);
						if(fieldValue != null) {
							dataCell.setCellValue(String.valueOf(fieldValue));
							dataCell.setCellStyle(dataCellStyle);

							//Add to totals bottom data
							try {
								if (field != null) {
									Integer totalData = totalsBottomData.get(field.getName());
									if (!StringUtil.isEmpty(field.getValueClassName()) && field.getValueClassName().equalsIgnoreCase("java.lang.Integer")) {
										if (totalData == null) {
											totalData = new Integer(0);
										}
										if (fieldValue != null) {
											totalData = totalData.intValue() + ((Integer) fieldValue).intValue();
										}
										totalsBottomData.put(field.getName(), totalData);
									}
								}
							} catch (Exception eTot) {
								getLogger().log(Level.WARNING, "Could not add totals to simple Excel report.", eTot);
							}
						}
					}
				}

				//Add the totals bottom line with data
				try {
					if (totalsBottomData != null && !totalsBottomData.isEmpty()) {
					 	HSSFCellStyle bottomCellStyle = wb.createCellStyle();

					 	bottomCellStyle.setWrapText( true );
					 	bottomCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
					 	bottomCellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
					 	bottomCellStyle.setFont(headerCellFont);

					 	HSSFRow dataRow = sheet.createRow((short)rowIndex++);
						colIndex = 0;
						boolean samtalsTitleUsed = false;

						for (Iterator iter = fields.iterator(); iter.hasNext(); colIndex++) {
							ReportableField field = (ReportableField) iter.next();
							HSSFCell dataCell = dataRow.createCell((short)colIndex);
							dataCell.setCellStyle(bottomCellStyle);
							if (field != null && !StringUtil.isEmpty(field.getName()) && totalsBottomData.get(field.getName()) != null) {
								dataCell.setCellValue(String.valueOf(totalsBottomData.get(field.getName())));
							} else {
								if (samtalsTitleUsed) {
									dataCell.setCellValue(CoreConstants.EMPTY);
								} else {
									dataCell.setCellValue("Samtals");
									samtalsTitleUsed = true;
								}
							}
						}
					}
				} catch (Exception eTot) {
					getLogger().log(Level.WARNING, "Could not add totals bottom to the simple Excel report.", eTot);
				}

		} catch (Exception e) {
			//-- Exception fetching data --//
			HSSFRow exceptionRow = sheet.createRow((short)rowIndex++);
		    HSSFCell exceptionCell = exceptionRow.createCell((short)0);

		    // Create a new font and alter it.
		    HSSFFont exceptionFont = wb.createFont();
		    exceptionFont.setFontName(REPORT_FONT);
		    exceptionFont.setItalic(true);

		    // Fonts are set into a style so create a new one to use.
		    HSSFCellStyle exceptionStyle = wb.createCellStyle();
		    exceptionStyle.setFont(exceptionFont);

		    // Create a cell and put a value in it.
		    exceptionCell.setCellValue("Error occurred while getting data. Check log for more details.");
		    exceptionCell.setCellStyle(exceptionStyle);

			e.printStackTrace();
		}


	    // Write the output to a file
	    FileOutputStream fileOut = new FileOutputStream(filePathAndName);
	    wb.write(fileOut);
	    fileOut.close();
	}


}
