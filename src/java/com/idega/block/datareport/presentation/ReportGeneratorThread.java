package com.idega.block.datareport.presentation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.design.JasperDesign;

import com.idega.block.dataquery.data.xml.QueryFieldPart;
import com.idega.block.datareport.business.DynamicReportDesign;
import com.idega.block.datareport.business.JasperReportBusiness;
import com.idega.block.datareport.presentation.ReportGenerator.ThreadRunDataSourceCollector;
import com.idega.block.datareport.util.ReportDescription;
import com.idega.block.datareport.util.ReportableCollection;
import com.idega.block.datareport.util.ReportableField;
import com.idega.block.datareport.xml.methodinvocation.ClassDescription;
import com.idega.business.IBOLookup;
import com.idega.core.messaging.MessagingSettings;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.Timer;
import com.idega.xml.XMLException;

public class ReportGeneratorThread extends Thread {

	private static final String IW_BUNDLE_IDENTIFIER = "com.idega.block.datareport";
	private final static String prmLablePrefix = "label_";

	private JRDataSource dataSource = null;

	private boolean generateExcelReport = false;
	private boolean generateXMLReport = false;
	private boolean generateHTMLReport = false;
	private boolean generatePDFReport = false;
	private boolean generateSimpleExcelReport = false;

	private ReportDescription reportDescription = null;

	private String reportName = ReportGenerator.DEFAULT_REPORT_NAME;

	private JasperDesign design = null;

	private String email = null;

	private Integer methodInvocationPK = null;
	private String methodInvocationFileName = null;

	private ThreadRunDataSourceCollector collector;

	private Vector dynamicFields = new Vector();

	private String layoutFileName = null;
	private Integer layoutICFilePK = null;
	private IWBundle layoutIWBundle = null;

	private Locale currentLocale = null;
	private String tmpReportName = null;

	private IWApplicationContext iwac = null;
	private Timer timer = null;

	public ReportGeneratorThread() {
		super();
		timer = new Timer();
		timer.start();
	}

	public void interrupt() {
		super.interrupt();
	}

	public void run() {
		System.out.println("Starting ReportGeneratorThread");
		super.run();

		try {
			generateDataSource();
			if (doGenerateSomeJasperReport()) {
				getLayoutFromICFileOrGenerate();
			} else {
				prepareForLayoutGeneration(true);
			}
			generateReportAndSend();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("ReportGeneratorThread done");
	}

	private void prepareForLayoutGeneration(boolean isMethodInvocation)
			throws IOException, JRException {
		int prmLableWidth = 95;
		int prmValueWidth = 55;

		this.reportDescription.setLocale(this.currentLocale);

		if (this.getDynamicFields() != null
				&& this.getDynamicFields().size() > 0) {
			Iterator iter = this.getDynamicFields().iterator();
			while (iter.hasNext()) {
				ClassDescription element = (ClassDescription) iter.next();
				String prmName = element.getName();
				String tmpPrmLabel = (String) this.reportDescription
						.get(ReportGeneratorThread.prmLablePrefix + prmName);
				String tmpPrmValue = (String) this.reportDescription
						.get(prmName);
				if (tmpPrmLabel != null && tmpPrmValue != null) {
					int tmpPrmLabelWidth = (tmpPrmLabel != null) ? calculateTextFieldWidthForString(tmpPrmLabel)
							: prmLableWidth;
					int tmpPrmValueWidth = (tmpPrmValue != null) ? calculateTextFieldWidthForString(tmpPrmValue)
							: prmValueWidth;
					this.reportDescription.addHeaderParameter(
							ReportGeneratorThread.prmLablePrefix + prmName,
							tmpPrmLabelWidth, prmName, String.class,
							tmpPrmValueWidth);
				}
			}
		}
	}

	private int calculateTextFieldWidthForString(String str) {
		int fontSize = 9;
		return (int) (5 + (str.length() * fontSize * 0.58));
	}

	private void getLayoutFromICFileOrGenerate() throws IOException,
			JRException {
		boolean isMethodInvocation = false;
		if (getTmpReportName() != null) {
			this.reportName = getTmpReportName();
		}
		if (this.getMethodInvocationPK() != null
				|| this.getMethodInvocationFileName() != null) {
			isMethodInvocation = true;
			if (this.dataSource != null
					&& this.dataSource instanceof ReportableCollection) {
				this.reportDescription = ((ReportableCollection) this.dataSource)
						.getReportDescription();
			}
		}

		// Fetch or generate the layout

		// fetch, only available for method invocation, TODO make available for
		// other types
		if (((this.getLayoutFileName() != null) || (this.getLayoutICFilePK() != null))
				&& isMethodInvocation) {
			getLayoutAndAddParameters();
		} else { // generate
			generateLayoutAndAddParameters(isMethodInvocation);
		}

	}

	private void generateLayoutAndAddParameters(boolean isMethodInvocation)
			throws IOException, JRException {

		prepareForLayoutGeneration(isMethodInvocation);

		int columnWidth = 120;

		DynamicReportDesign designTemplate = new DynamicReportDesign(
				"GeneratedReport");

		List keys = this.reportDescription.getListOfHeaderParameterKeys();
		List labels = this.reportDescription
				.getListOfHeaderParameterLabelKeys();
		Iterator keyIter = keys.iterator();
		Iterator labelIter = labels.iterator();
		while (keyIter.hasNext() && labelIter.hasNext()) {
			String key = (String) keyIter.next();
			String label = (String) labelIter.next();
			designTemplate.addHeaderParameter(label,
					this.reportDescription.getWithOfParameterOrLabel(label),
					key, this.reportDescription.getParameterClassType(key),
					this.reportDescription.getWithOfParameterOrLabel(key));
		}

		List allFields = this.reportDescription.getListOfFields();
		if (allFields != null && allFields.size() > 0) {
			// //System.out.println("ReportGenerator.");
			// TODO thi: solve problem with the width of columns avoiding
			// merging of vertical cells in excel outputs
			// stretch with overflow merges two vertical cells, excel file can't
			// be sorted
			// see also and fix also JasperReportBusiness

			// ------------------------------------------------------------------------------------------------------------------------------------------------------------------
			// TMP
			// TODO get columnspacing (15) and it to columnsWidth;
			int numberOfFields = this.reportDescription.getNumberOfFields();
			int columnsWidth = columnWidth * numberOfFields + 15
					* (numberOfFields - 1);
			// TMP
			// TODO get page Margins (20) and add them to pageWidth;
			// does the width fit the page width?
			if (columnsWidth > DynamicReportDesign.PAGE_WIDTH_WITHOUT_MARGINS_PORTRAIT_A4) {
				// change to landscape!
				designTemplate.setOrientationLandscape();
				// does the the width now fit the page width?
				int landscapeWidth = (columnsWidth > DynamicReportDesign.PAGE_WIDTH_WITHOUT_MARGINS_LANDSCAPE_A4) ? columnsWidth
						+ DynamicReportDesign.PAGE_LEFT_MARGIN
						+ DynamicReportDesign.PAGE_RIGHT_MARGIN
						: DynamicReportDesign.PAGE_WIDTH_LANDSCAPE_A4;
				designTemplate.setPageWidth(landscapeWidth);
				designTemplate
						.setPageHeight(DynamicReportDesign.PAGE_HEIGHT_LANDSCAPE_A4);
			}
			// do not change the width of the page!! prior:
			// designTemplate.setPageWidth(columnsWidth + 20 + 20);
			designTemplate.setColumnWidth(columnsWidth);
			//
			Iterator iter = allFields.iterator();
			if (isMethodInvocation) {
				while (iter.hasNext()) {
					ReportableField field = (ReportableField) iter.next();
					String name = field.getName();
					designTemplate.addField(name, field.getValueClass(),
							columnWidth);
				}
			} else {
				while (iter.hasNext()) {
					try {
						QueryFieldPart element = (QueryFieldPart) iter.next();
						ReportableField field = new ReportableField(
								element.getIDOEntityField());
						String name = field.getName();
						designTemplate.addField(name, field.getValueClass(),
								columnWidth);
					} catch (IDOLookupException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
		designTemplate.close();
		this.design = designTemplate.getJasperDesign(this.iwac);
	}

	private void getLayoutAndAddParameters() throws RemoteException {
		JasperReportBusiness reportBusiness = getReportBusiness();
		if (this.layoutICFilePK != null) {
			int designId = this.layoutICFilePK.intValue();
			this.design = reportBusiness.getDesignBox(designId).getDesign();
		} else if (this.layoutFileName != null) {
			if (this.getLayoutIWBundle() != null) {
				this.design = reportBusiness.getDesignFromBundle(
						this.getLayoutIWBundle(), this.layoutFileName);
			} else {
				this.design = reportBusiness.getDesignFromBundle(
						this.iwac.getIWMainApplication().getBundle(
								ReportGeneratorThread.IW_BUNDLE_IDENTIFIER),
						this.layoutFileName);
			}
		}
		// add parameters and fields
	}

	private void generateReportAndSend() throws RemoteException, JRException {
		JasperReportBusiness business = getReportBusiness();
		if (this.dataSource != null) {
			String mailServer = null;
			String fromAddress = null;
			try {
				MessagingSettings messagingSetting = getIWApplicationContext()
						.getIWMainApplication().getMessagingSettings();
				mailServer = messagingSetting.getSMTPMailServer();
				fromAddress = messagingSetting.getFromMailAddress();
			} catch (Exception e) {
				System.err
						.println("MessageBusinessBean: Error getting mail property from bundle");
				e.printStackTrace();
			}

			timer.stop();
			String body = "Execution time : " + timer.getTimeString();
			
			if (doGenerateSomeJasperReport()
					&& (this.dataSource != null && this.design != null)) {
				this.reportDescription.put(DynamicReportDesign.PRM_REPORT_NAME,
						this.reportName);
				JasperPrint print = business.getReport(this.dataSource,
						this.reportDescription.getDisplayValueMap(),
						this.design);

				if (this.generatePDFReport) {
					String fileName = business.getPdfReport(print, "report");

					fileName = this.iwac.getIWMainApplication().getRealPath(
							fileName);
					System.out.println("generating pdf file : " + fileName);

					File file = new File(fileName);

					try {
						com.idega.util.SendMail.send(fromAddress, email.trim(),
								null, "", mailServer, "Excel", body, file);
					} catch (javax.mail.MessagingException me) {
						System.err
								.println("MessagingException when sending mail to address: "
										+ email
										+ " Message was: "
										+ me.getMessage());
					} catch (Exception e) {
						System.err
								.println("Exception when sending mail to address: "
										+ email
										+ " Message was: "
										+ e.getMessage());
					}
				}

				if (this.generateExcelReport) {
					String fileName = business.getExcelReport(print, "report");
					fileName = this.iwac.getIWMainApplication().getRealPath(
							fileName);
					System.out.println("generating excel file : " + fileName);

					File file = new File(fileName);

					try {
						com.idega.util.SendMail.send(fromAddress, email.trim(),
								null, "", mailServer, "Excel", body, file);
					} catch (javax.mail.MessagingException me) {
						System.err
								.println("MessagingException when sending mail to address: "
										+ email
										+ " Message was: "
										+ me.getMessage());
					} catch (Exception e) {
						System.err
								.println("Exception when sending mail to address: "
										+ email
										+ " Message was: "
										+ e.getMessage());
					}
				}

				if (this.generateHTMLReport) {
					String fileName = business.getHtmlReport(print, "report");
					fileName = this.iwac.getIWMainApplication().getRealPath(
							fileName);
					System.out.println("generating html file : " + fileName);

					File file = new File(fileName);

					try {
						com.idega.util.SendMail.send(fromAddress, email.trim(),
								null, "", mailServer, "Excel", body, file);
					} catch (javax.mail.MessagingException me) {
						System.err
								.println("MessagingException when sending mail to address: "
										+ email
										+ " Message was: "
										+ me.getMessage());
					} catch (Exception e) {
						System.err
								.println("Exception when sending mail to address: "
										+ email
										+ " Message was: "
										+ e.getMessage());
					}
				}

				if (this.generateXMLReport) {
					String fileName = business.getXmlReport(print, "report");
					fileName = this.iwac.getIWMainApplication().getRealPath(
							fileName);
					System.out.println("generating xml file : " + fileName);

					File file = new File(fileName);

					try {
						com.idega.util.SendMail.send(fromAddress, email.trim(),
								null, "", mailServer, "Excel", body, file);
					} catch (javax.mail.MessagingException me) {
						System.err
								.println("MessagingException when sending mail to address: "
										+ email
										+ " Message was: "
										+ me.getMessage());
					} catch (Exception e) {
						System.err
								.println("Exception when sending mail to address: "
										+ email
										+ " Message was: "
										+ e.getMessage());
					}
				}
			}

			if (this.generateSimpleExcelReport
					&& (this.dataSource instanceof ReportableCollection)) {
				String fileName = business.getSimpleExcelReport(
						((ReportableCollection) this.dataSource)
								.getJRDataSource(), this.reportName,
						this.reportDescription);
				fileName = this.iwac.getIWMainApplication().getRealPath(
						fileName);
				System.out
						.println("generating excel simple file : " + fileName);

				File file = new File(fileName);

				try {
					com.idega.util.SendMail.send(fromAddress, email.trim(),
							null, "", mailServer, "Excel", body, file);
				} catch (javax.mail.MessagingException me) {
					System.err
							.println("MessagingException when sending mail to address: "
									+ email
									+ " Message was: "
									+ me.getMessage());
				} catch (Exception e) {
					System.err
							.println("Exception when sending mail to address: "
									+ email + " Message was: " + e.getMessage());
				}
			}
		}
	}

	public JasperReportBusiness getReportBusiness() {
		try {
			return (JasperReportBusiness) IBOLookup.getServiceInstance(
					IWMainApplication.getDefaultIWApplicationContext(),
					JasperReportBusiness.class);
		} catch (RemoteException ex) {
			System.err
					.println("[ReportLayoutChooser]: Can't retrieve JasperReportBusiness. Message is: "
							+ ex.getMessage());
			throw new RuntimeException(
					"[ReportLayoutChooser]: Can't retrieve ReportBusiness");
		}
	}

	private boolean doGenerateSomeJasperReport() {
		return (this.generateExcelReport || this.generateHTMLReport
				|| this.generateXMLReport || this.generatePDFReport);
	}

	private void generateDataSource() throws XMLException, Exception {
		try {
			this.dataSource = (JRDataSource) this.collector.getMethod().invoke(
					this.collector.getForInvocationOfMethod(),
					this.collector.getPrmVal());
		} catch (InvocationTargetException e) {
			Throwable someException = e.getTargetException();
			if (someException != null && someException instanceof Exception) {
				throw (Exception) someException;
			} else {
				throw e;
			}

		}

		if (this.dataSource != null
				&& this.dataSource instanceof ReportableCollection) {
			this.reportDescription = ((ReportableCollection) this.dataSource)
					.getReportDescription();
			this.reportDescription.merge(this.collector.getDescription());
		} else {
			this.reportDescription = this.collector.getDescription();
		}
		this.reportDescription.setLocale(this.getCurrentLocale());
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public JRDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(JRDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public boolean isGenerateExcelReport() {
		return generateExcelReport;
	}

	public void setGenerateExcelReport(boolean generateExcelReport) {
		this.generateExcelReport = generateExcelReport;
	}

	public boolean isGenerateXMLReport() {
		return generateXMLReport;
	}

	public void setGenerateXMLReport(boolean generateXMLReport) {
		this.generateXMLReport = generateXMLReport;
	}

	public boolean isGenerateHTMLReport() {
		return generateHTMLReport;
	}

	public void setGenerateHTMLReport(boolean generateHTMLReport) {
		this.generateHTMLReport = generateHTMLReport;
	}

	public boolean isGeneratePDFReport() {
		return generatePDFReport;
	}

	public void setGeneratePDFReport(boolean generatePDFReport) {
		this.generatePDFReport = generatePDFReport;
	}

	public ReportDescription getReportDescription() {
		return reportDescription;
	}

	public void setReportDescription(ReportDescription reportDescription) {
		this.reportDescription = reportDescription;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public JasperDesign getDesign() {
		return design;
	}

	public void setDesign(JasperDesign design) {
		this.design = design;
	}

	public boolean isGenerateSimpleExcelReport() {
		return generateSimpleExcelReport;
	}

	public void setGenerateSimpleExcelReport(boolean generateSimpleExcelReport) {
		this.generateSimpleExcelReport = generateSimpleExcelReport;
	}

	public ThreadRunDataSourceCollector getCollector() {
		return collector;
	}

	public void setCollector(ThreadRunDataSourceCollector collector) {
		this.collector = collector;
	}

	public String getTmpReportName() {
		return tmpReportName;
	}

	public void setTmpReportName(String tmpReportName) {
		this.tmpReportName = tmpReportName;
	}

	public Locale getCurrentLocale() {
		return currentLocale;
	}

	public void setCurrentLocale(Locale currentLocale) {
		this.currentLocale = currentLocale;
	}

	public String getLayoutFileName() {
		return layoutFileName;
	}

	public void setLayoutFileName(String layoutFileName) {
		this.layoutFileName = layoutFileName;
	}

	public Integer getLayoutICFilePK() {
		return layoutICFilePK;
	}

	public void setLayoutICFilePK(Integer layoutICFilePK) {
		this.layoutICFilePK = layoutICFilePK;
	}

	public Integer getMethodInvocationPK() {
		return methodInvocationPK;
	}

	public void setMethodInvocationPK(Integer methodInvocationPK) {
		this.methodInvocationPK = methodInvocationPK;
	}

	public String getMethodInvocationFileName() {
		return methodInvocationFileName;
	}

	public void setMethodInvocationFileName(String methodInvocationFileName) {
		this.methodInvocationFileName = methodInvocationFileName;
	}

	public IWBundle getLayoutIWBundle() {
		return layoutIWBundle;
	}

	public void setLayoutIWBundle(IWBundle layoutIWBundle) {
		this.layoutIWBundle = layoutIWBundle;
	}

	public IWApplicationContext getIWApplicationContext() {
		return iwac;
	}

	public void setIWApplicationContext(IWApplicationContext iwac) {
		this.iwac = iwac;
	}

	public Vector getDynamicFields() {
		return dynamicFields;
	}

	public void setDynamicFields(Vector dynamicFields) {
		this.dynamicFields = dynamicFields;
	}
}