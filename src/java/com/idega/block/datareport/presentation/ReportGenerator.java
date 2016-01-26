/*
 * Created on 15.7.2003
 *
 * To change the template for this generated file go to Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.idega.block.datareport.presentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.ejb.FinderException;

import com.idega.block.dataquery.business.QueryService;
import com.idega.block.dataquery.data.Query;
import com.idega.block.dataquery.data.QueryHome;
import com.idega.block.dataquery.data.xml.QueryConditionPart;
import com.idega.block.dataquery.data.xml.QueryFieldPart;
import com.idega.block.dataquery.data.xml.QueryHelper;
import com.idega.block.datareport.business.DynamicReportDesign;
import com.idega.block.datareport.business.JasperReportBusiness;
import com.idega.block.datareport.data.MethodInvocationXMLFile;
import com.idega.block.datareport.data.MethodInvocationXMLFileHome;
import com.idega.block.datareport.util.ReportDescription;
import com.idega.block.datareport.util.ReportableCollection;
import com.idega.block.datareport.util.ReportableField;
import com.idega.block.datareport.xml.methodinvocation.ClassDescription;
import com.idega.block.datareport.xml.methodinvocation.ClassHandler;
import com.idega.block.datareport.xml.methodinvocation.MethodDescription;
import com.idega.block.datareport.xml.methodinvocation.MethodInput;
import com.idega.block.datareport.xml.methodinvocation.MethodInvocationDocument;
import com.idega.block.datareport.xml.methodinvocation.MethodInvocationParser;
import com.idega.business.HiddenInputHandler;
import com.idega.business.IBOLookup;
import com.idega.business.IBOService;
import com.idega.business.IBOSession;
import com.idega.business.InputHandler;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.file.data.ICFile;
import com.idega.data.IDOEntity;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.PresentationObject;
import com.idega.presentation.Table;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.BackButton;
import com.idega.presentation.ui.CheckBoxInputHandler;
import com.idega.presentation.ui.Form;
import com.idega.presentation.ui.HiddenInput;
import com.idega.presentation.ui.InterfaceObject;
import com.idega.presentation.ui.SubmitButton;
import com.idega.presentation.ui.TextInput;
import com.idega.servlet.filter.IWBundleResourceFilter;
import com.idega.user.business.GroupBusiness;
import com.idega.user.dao.GroupDAO;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.reflect.MethodFinder;
import com.idega.xml.XMLException;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.design.JasperDesign;

/**
 * Title: ReportGenerator Description: Copyright: Copyright (c) 2003 Company: idega Software
 *
 * @author 2003 - idega team -<br>
 *         <a href="mailto:gummi@idega.is">Gudmundur Agust Saemundsson</a><br>
 * @version 1.0
 */
public class ReportGenerator extends Block {

	public static final String	HTML_FORMAT = "html",
								XML_FORMAT = "xml",
								PDF_FORMAT = "pdf",
								EXCEL_FORMAT = "excel",
								SIMPLE_EXCEL_FORMAT = "simple_excel",

								DEFAULT_REPORT_NAME = "Generated Report",

								STYLE = "font-family:arial; font-size:8pt; color:#000000; text-align: justify; border: 1 solid #000000;",
								STYLE_2 = "font-family:arial; font-size:8pt; color:#000000; text-align: justify;",

								PRIFIX_PRM = "dr_",

								IW_BUNDLE_IDENTIFIER = "com.idega.block.datareport";

	private static final String PRM_STATE = "dr_gen_state",
								VALUE_STATE_GENERATE_REPORT = "2",
								SESSION_KEY_TOP_NODES = "top_nodes_for_user";

	private Integer queryPK = null;
	private Integer methodInvocationPK = null;
	private Integer layoutICFilePK = null;
	private String layoutFileName = null;
	private IWBundle layoutIWBundle = null;
	private String methodInvocationFileName = null;
	private IWBundle methodInvocationIWBundle = null;

	private MethodInvocationDocument methodInvokeDoc = null;
	private List<ClassDescription> dynamicFields = new ArrayList<ClassDescription>();
	private List<ReportableField> reportableFields = new ArrayList<ReportableField>();
	private Map<String, String> reportFilePathsMap = new HashMap<String, String>();
	public Map<String, String> getReportFilePathsMap() {
		return reportFilePathsMap;
	}

	private QueryHelper queryParser = null;
	private JRDataSource dataSource = null;
	private Table fieldTable = null;
	private JasperDesign design = null;
	private ReportDescription reportDescription = null;// new ReportDescription();

	private List<String> maintainParameterList = new ArrayList<String>();

	private final static String prmLablePrefix = "label_";

	private String reportName = DEFAULT_REPORT_NAME;
	private boolean canChangeReportName = true;
	private boolean showReportNameInputIfCannotChangeIt = false;
	private final static String PRM_REPORT_NAME = "report_name";

	private boolean generateExcelReport = true;
	private boolean generateXMLReport = true;
	private boolean generateHTMLReport = true;
	private boolean generatePDFReport = true;
	private boolean generateSimpleExcelReport = true;

	private boolean generateStatistics = false;

	private boolean runAsThread = false;
	private String email = null;

	private List<Integer> groupsIds = null;

	/**
	 *
	 */
	public ReportGenerator() {
		super();
	}

	@Override
	public String getBundleIdentifier() {
		return IW_BUNDLE_IDENTIFIER;
	}

	private void parseQuery(IWContext iwc) throws XMLException, Exception {
		if (this.queryParser == null) {
			Query query = ((QueryHome) IDOLookup.getHome(Query.class)).findByPrimaryKey(this.queryPK);
			this.queryParser = new QueryHelper(query, iwc);
		}

		List allFields = this.queryParser.getListOfFields();
		if (allFields != null) {
			this.reportDescription.addFields(allFields);
			// System.out.println("ReportGenerator#parseQuery() - _queryParser.getListOfFields().size() = " + allFields.size());
		}
		else {
			// System.out.println("ReportGenerator#parseQuery() - _queryParser.getListOfFields() == null");
		}

		Collection conditionsCollection = this.queryParser.getListOfConditions();
		if (conditionsCollection != null) {
			Iterator iter = conditionsCollection.iterator();
			while (iter.hasNext()) {
				QueryConditionPart element = (QueryConditionPart) iter.next();
				if (element.isDynamic()) {
					this.reportableFields.add(new ReportableField(element.getIDOEntityField()));
				}
			}
		}
	}

	public void setParameterToMaintain(String param) {
		this.maintainParameterList.add(param);
	}

	public void setParametersToMaintain(List paramList) {
		this.maintainParameterList.addAll(paramList);
	}

	private int calculateTextFieldWidthForString(String str) {
		int fontSize = 9;
		return (int) (5 + (str.length() * fontSize * 0.58));
	}

	private void getLayoutFromICFileOrGenerate(IWContext iwc) throws IOException, JRException {
		boolean isMethodInvocation = false;
		String tmpName = iwc.getParameter(getParameterName(this.PRM_REPORT_NAME));
		if (tmpName != null) {
			this.reportName = tmpName;
		}
		if (this.queryPK == null && (this.methodInvocationPK != null || this.methodInvocationFileName != null)) {
			isMethodInvocation = true;
			if (this.dataSource != null && this.dataSource instanceof ReportableCollection) {
				this.reportDescription = ((ReportableCollection) this.dataSource).getReportDescription();
			}
		}

		// Fetch or generate the layout

		// fetch, only available for method invocation, TODO make available for other types
		if (((this.layoutFileName != null) || (this.layoutICFilePK != null)) && isMethodInvocation) {
			getLayoutAndAddParameters(iwc);
		}
		else { // generate
			generateLayoutAndAddParameters(iwc, isMethodInvocation);
		}

	}

	private void prepareForLayoutGeneration(IWContext iwc, boolean isMethodInvocation) throws IOException, JRException {
		int prmLableWidth = 95;
		int prmValueWidth = 55;

		this.reportDescription.setLocale(iwc.getCurrentLocale());

		if (this.queryPK != null) {
			Iterator<ReportableField> iter = this.reportableFields.iterator();
			while (iter.hasNext()) {
				ReportableField element = iter.next();
				String prmName = element.getName();
				String tmpPrmLabel = (String) this.reportDescription.get(this.prmLablePrefix + prmName);
				String tmpPrmValue = (String) this.reportDescription.get(prmName);
				int tmpPrmLabelWidth = (tmpPrmLabel != null) ? calculateTextFieldWidthForString(tmpPrmLabel) : prmLableWidth;
				int tmpPrmValueWidth = (tmpPrmValue != null) ? calculateTextFieldWidthForString(tmpPrmValue) : prmValueWidth;
				this.reportDescription.addHeaderParameter(this.prmLablePrefix + prmName, tmpPrmLabelWidth, prmName, String.class, tmpPrmValueWidth);
			}
		}
		else {
			Iterator<ClassDescription> iter = this.dynamicFields.iterator();
			while (iter.hasNext()) {
				ClassDescription element = iter.next();
				String prmName = element.getName();
				String tmpPrmLabel = (String) this.reportDescription.get(this.prmLablePrefix + prmName);
				String tmpPrmValue = (String) this.reportDescription.get(prmName);
				if (tmpPrmLabel != null && tmpPrmValue != null) {
					int tmpPrmLabelWidth = (tmpPrmLabel != null) ? calculateTextFieldWidthForString(tmpPrmLabel) : prmLableWidth;
					int tmpPrmValueWidth = (tmpPrmValue != null) ? calculateTextFieldWidthForString(tmpPrmValue) : prmValueWidth;
					this.reportDescription.addHeaderParameter(this.prmLablePrefix + prmName, tmpPrmLabelWidth, prmName, String.class, tmpPrmValueWidth);
				}
			}
		}
	}

	private void generateLayoutAndAddParameters(IWContext iwc, boolean isMethodInvocation) throws IOException, JRException {

		prepareForLayoutGeneration(iwc, isMethodInvocation);

		int columnWidth = 120;

		DynamicReportDesign designTemplate = new DynamicReportDesign("GeneratedReport");

		List keys = this.reportDescription.getListOfHeaderParameterKeys();
		List labels = this.reportDescription.getListOfHeaderParameterLabelKeys();
		Iterator keyIter = keys.iterator();
		Iterator labelIter = labels.iterator();
		while (keyIter.hasNext() && labelIter.hasNext()) {
			String key = (String) keyIter.next();
			String label = (String) labelIter.next();
			designTemplate.addHeaderParameter(label, this.reportDescription.getWithOfParameterOrLabel(label), key, this.reportDescription.getParameterClassType(key), this.reportDescription.getWithOfParameterOrLabel(key));
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
			int columnsWidth = columnWidth * numberOfFields + 15 * (numberOfFields - 1);
			// TMP
			// TODO get page Margins (20) and add them to pageWidth;
			// does the width fit the page width?
			if (columnsWidth > DynamicReportDesign.PAGE_WIDTH_WITHOUT_MARGINS_PORTRAIT_A4) {
				// change to landscape!
				designTemplate.setOrientationLandscape();
				// does the the width now fit the page width?
				int landscapeWidth = (columnsWidth > DynamicReportDesign.PAGE_WIDTH_WITHOUT_MARGINS_LANDSCAPE_A4) ? columnsWidth + DynamicReportDesign.PAGE_LEFT_MARGIN + DynamicReportDesign.PAGE_RIGHT_MARGIN : DynamicReportDesign.PAGE_WIDTH_LANDSCAPE_A4;
				designTemplate.setPageWidth(landscapeWidth);
				designTemplate.setPageHeight(DynamicReportDesign.PAGE_HEIGHT_LANDSCAPE_A4);
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
					designTemplate.addField(name, field.getValueClass(), columnWidth);
				}
			}
			else {
				while (iter.hasNext()) {
					try {
						QueryFieldPart element = (QueryFieldPart) iter.next();
						ReportableField field = new ReportableField(element.getIDOEntityField());
						String name = field.getName();
						designTemplate.addField(name, field.getValueClass(), columnWidth);
					}
					catch (IDOLookupException e) {
						e.printStackTrace();
					}
					catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
		designTemplate.close();
		this.design = designTemplate.getJasperDesign(iwc);
	}

	private void getLayoutAndAddParameters(IWContext iwc) throws RemoteException {
		JasperReportBusiness reportBusiness = getReportBusiness();
		if (this.layoutICFilePK != null) {
			int designId = this.layoutICFilePK.intValue();
			this.design = reportBusiness.getDesignBox(designId).getDesign();
		}
		else if (this.layoutFileName != null) {
			if (this.layoutIWBundle != null) {
				this.design = reportBusiness.getDesignFromBundle(this.layoutIWBundle, this.layoutFileName);
			}
			else {
				this.design = reportBusiness.getDesignFromBundle(getBundle(iwc), this.layoutFileName);
			}
		}
		// add parameters and fields
	}

	public class ThreadRunDataSourceCollector {
		private Method method;
		private ReportDescription description;
		private Object forInvocationOfMethod;
		private Object[] prmVal;

		public Method getMethod() {
			return method;
		}
		public void setMethod(Method method) {
			this.method = method;
		}
		public ReportDescription getDescription() {
			return description;
		}
		public void setDescription(ReportDescription description) {
			this.description = description;
		}
		public Object getForInvocationOfMethod() {
			return forInvocationOfMethod;
		}
		public void setForInvocationOfMethod(Object forInvocationOfMethod) {
			this.forInvocationOfMethod = forInvocationOfMethod;
		}
		public Object[] getPrmVal() {
			return prmVal;
		}
		public void setPrmVal(Object[] prmVal) {
			this.prmVal = prmVal;
		}

	}

	private Object forInvocationOfMethod = null;
	private Method method = null;
	private Map<String, String[]> values = new HashMap<>();
	private String[] getValues(String param) {
		return values.get(param);
	}

	public void addValues(String param, String value) {
		addValues(param, new String[] {value});
	}
	public void addValues(String param, String[] values) {
		this.values.put(param, values);
	}

	private ThreadRunDataSourceCollector generateDataSource(IWContext iwc) throws XMLException, Exception {
		Locale currentLocale = iwc.getCurrentLocale();
		if (this.queryPK != null) {
			getLogger().info("Creating data source from query: " + queryPK);
			QueryService service = (IBOLookup.getServiceInstance(iwc, QueryService.class));
			this.dataSource = service.generateQueryResult(this.queryPK, iwc);
		}
		else if (this.methodInvokeDoc != null) {
			ReportDescription tmpReportDescriptionForCollectingData = new ReportDescription();
			List<MethodDescription> mDescs = this.methodInvokeDoc.getMethodDescriptions();
			if (mDescs != null) {
				Iterator<MethodDescription> it = mDescs.iterator();
				if (it.hasNext()) {
					MethodDescription mDesc = it.next();

					ClassDescription mainClassDesc = mDesc.getClassDescription();
					Class<?> mainClass = mainClassDesc.getClassObject();
					String type = mainClassDesc.getType();
					String methodName = mDesc.getName();

					MethodInput input = mDesc.getInput();
					List<ClassDescription> parameters = null;
					if (input != null) {
						parameters = input.getClassDescriptions();
					}

					Object[] prmVal = null;
					Class<?>[] paramTypes = null;

					if (parameters != null) {
						prmVal = new Object[parameters.size()];
						paramTypes = new Class[parameters.size()];
						ListIterator<ClassDescription> iterator = parameters.listIterator();
						while (iterator.hasNext()) {
							int index = iterator.nextIndex();
							ClassDescription clDesc = iterator.next();
							Class<?> prmClassType = clDesc.getClassObject();
							paramTypes[index] = prmClassType;

							String param = getParameterName(clDesc.getName());
							String[] prmValues = iwc.isParameterSet(param) ? iwc.getParameterValues(param) : getValues(param);

							String prm = null;
							Object obj = null;

							if (prmValues != null && prmValues.length > 0) {
								prm = prmValues[0];
							}

							ClassHandler cHandler = clDesc.getClassHandler();
							InputHandler iHandler = null;
							boolean isHidden = false;
							if (cHandler != null) {
								iHandler = cHandler.getHandler();
								isHidden = iHandler instanceof HiddenInputHandler;
							}

							if (iHandler != null) {
								obj = iHandler.getResultingObject(prmValues, iwc);
								String displayNameOfValue = iHandler.getDisplayForResultingObject(obj, iwc);
								if (displayNameOfValue != null) {
									tmpReportDescriptionForCollectingData.put(clDesc.getName(), displayNameOfValue);
								}
								if (isHidden) {
									tmpReportDescriptionForCollectingData.remove(clDesc.getName());
								}
							}
							else {
								// ONLY HANDLES ONE VALUE!
								obj = getParameterObject(iwc, prm, prmClassType);
								if (!isHidden) {
									tmpReportDescriptionForCollectingData.put(clDesc.getName(), prm);
								}
							}

							if (!isHidden) {
								tmpReportDescriptionForCollectingData.put(prmLablePrefix + clDesc.getName(), clDesc.getLocalizedName(currentLocale) + ":");
							}
							else {
								tmpReportDescriptionForCollectingData.remove(prmLablePrefix + clDesc.getName());
							}

							prmVal[index] = obj;
						}
					}

					Object forInvocationOfMethod = this.forInvocationOfMethod == null ? null : this.forInvocationOfMethod;
					if (forInvocationOfMethod == null) {
						if (ClassDescription.VALUE_TYPE_IDO_SESSION_BEAN.equals(type)) {
							forInvocationOfMethod = IBOLookup.getSessionInstance(iwc, (Class<? extends IBOSession>) mainClass);
						}
						else if (ClassDescription.VALUE_TYPE_IDO_SERVICE_BEAN.equals(type)) {
							forInvocationOfMethod = IBOLookup.getServiceInstance(iwc, (Class<? extends IBOService>) mainClass);
						}
						else if (ClassDescription.VALUE_TYPE_IDO_ENTITY_HOME.equals(type)) {
							forInvocationOfMethod = IDOLookup.getHome((Class<? extends IDOEntity>) mainClass);

						}
						else { // ClassDescription.VALUE_TYPE_CLASS.equals(type))
							forInvocationOfMethod = mainClass.newInstance();
						}
					}

					MethodFinder mf = MethodFinder.getInstance();


					Method method = null;

					if ((this.runAsThread) && !isGenerateStatistics()) {
						Object threadPrmVal[] = new Object[prmVal.length + 6];
						Class<?>[] threadParamTypes = new Class[paramTypes.length + 6];
						int i = 0;
						for (; i < prmVal.length; i++) {
							threadPrmVal[i] = prmVal[i];
							threadParamTypes[i] = paramTypes[i];
						}
						threadPrmVal[i] = iwc.getCurrentLocale();
						threadParamTypes[i++] = Locale.class;
						threadPrmVal[i] = Boolean.valueOf(iwc.isSuperAdmin());
						threadParamTypes[i++] = Boolean.class;
						threadPrmVal[i] = iwc.getCurrentUser();
						threadParamTypes[i++] = User.class;;
						threadPrmVal[i] = iwc.getSessionAttribute(SESSION_KEY_TOP_NODES + iwc.getCurrentUser().getPrimaryKey().toString());
						threadParamTypes[i++] = Collection.class;

						//Hack, fix later
						Group group = null;
						List<Group> groups = null;
						String groupIDFilter = (String) prmVal[0];
						String groupsRecursiveFilter = (String) prmVal[1];
						Collection<String> groupTypesFilter = (Collection) prmVal[2];

						try {
							if (groupIDFilter != null && !groupIDFilter.equals("")) {
								groupIDFilter = groupIDFilter.substring(groupIDFilter.lastIndexOf("_") + 1);
								group = getGroupBusiness().getGroupByGroupID(Integer.parseInt((groupIDFilter)));
								if (group.isAlias()) {
									group = group.getAlias();
								}
							}
							if (group != null) {
								boolean loadChildGroups = groupsRecursiveFilter != null && groupsRecursiveFilter.equals(CheckBoxInputHandler.CHECKED);
								if (!ListUtil.isEmpty(groupsIds)) {
									Map<Integer, Boolean> tmpGroups = new HashMap<>();
									GroupDAO groupDAO = ELUtil.getInstance().getBean(GroupDAO.class);
									Map<Integer, List<Integer>> allChildGroupsIds = groupDAO.getChildGroupsIds(groupsIds, groupTypesFilter == null ? null : new ArrayList<String>(groupTypesFilter), true);
									if (!MapUtil.isEmpty(allChildGroupsIds)) {
										for (List<Integer> childGroupsIds: allChildGroupsIds.values()) {
											if (ListUtil.isEmpty(childGroupsIds)) {
												continue;
											}

											for (Integer id: childGroupsIds) {
												if (!tmpGroups.containsKey(id)) {
													tmpGroups.put(id, Boolean.TRUE);
												}
											}
										}
									}

									groups = new ArrayList<Group>();
									if (!MapUtil.isEmpty(tmpGroups)) {
										Collection<String> tmp = new ArrayList<>();
										for (Integer id: tmpGroups.keySet()) {
											tmp.add(String.valueOf(id));
										}
										Collection<Group> groupsByIds = getGroupBusiness().getGroups(tmp);
										if (!ListUtil.isEmpty(groupsByIds)) {
											for (Group groupById: groupsByIds) {
												groups.add(groupById);
											}
										}
									}
								} else {
									groups = new ArrayList<Group>();
									if (loadChildGroups) {
										Collection<Group> childGroups = getGroupBusiness().getChildGroupsRecursiveResultFiltered(group, groupTypesFilter, true, true, true);	//	TODO: improve
										if (childGroups != null) {
											groups.addAll(childGroups);
										}
									}
								}
								groups.add(group);

								User currentUser = iwc.getCurrentUser();
								List<Group> viewGroups = new ArrayList<Group>();
								for (Group g: groups) {
									if (hasViewPermission(iwc, currentUser, g))	{//	TODO: improve
										viewGroups.add(g);
									}
								}
//								groups.parallelStream().forEach(g -> {
//									if (hasViewPermission(iwc, currentUser, g)) {	//	TODO: improve
//										viewGroups.add(g);
//									}
//								});

								groups = viewGroups;
							}
						} catch (FinderException e) {
							e.printStackTrace();
						}

						threadPrmVal[i] = groups;
						threadParamTypes[i++] = Collection.class;;
						threadPrmVal[i] = group;
						threadParamTypes[i++] = Group.class;;

						method = mf.getMethodWithNameAndParameters(mainClass, methodName, threadParamTypes);

						ThreadRunDataSourceCollector ret = new ThreadRunDataSourceCollector();
						ret.setMethod(method);
						ret.setDescription(tmpReportDescriptionForCollectingData);
						ret.setForInvocationOfMethod(forInvocationOfMethod);
						ret.setPrmVal(threadPrmVal);

						return ret;
					} else {
						if (this.runAsThread){
							method = this.method == null ? mf.getMethodWithNameAndParameters(mainClass, methodName, paramTypes) : this.method;
							ThreadRunDataSourceCollector ret = new ThreadRunDataSourceCollector();
							ret.setMethod(method);
							ret.setDescription(tmpReportDescriptionForCollectingData);
							ret.setForInvocationOfMethod(forInvocationOfMethod);
							ret.setPrmVal(prmVal);
							return ret;
						}
						else {
							method = this.method == null ? mf.getMethodWithNameAndParameters(mainClass, methodName, paramTypes) : this.method;
						}
					}

					try {
						this.dataSource = (JRDataSource) method.invoke(forInvocationOfMethod, prmVal);
					}
					catch (InvocationTargetException e) {
						Throwable someException = e.getTargetException();
						if (someException != null && someException instanceof Exception) {
							throw (Exception) someException;
						}
						else {
							throw e;
						}
					}

					if (this.dataSource != null && this.dataSource instanceof ReportableCollection) {
						this.reportDescription = ((ReportableCollection) this.dataSource).getReportDescription();
						this.reportDescription.merge(tmpReportDescriptionForCollectingData);
					}
					else {
						this.reportDescription = tmpReportDescriptionForCollectingData;
					}
					this.reportDescription.setLocale(iwc.getCurrentLocale());
				}
			}
		}

		return null;
	}

	private GroupBusiness getGroupBusiness() throws RemoteException {
		return IBOLookup.getServiceInstance(this.getIWApplicationContext(), GroupBusiness.class);
	}

	private boolean hasViewPermission(IWContext iwc, User user, Group group) {
		AccessController accessController = iwc.getAccessController();

		boolean isCurrentUserSuperAdmin = iwc.isSuperAdmin();

		boolean hasViewPermissionForRealGroup = isCurrentUserSuperAdmin;
		//boolean hasEditPermissionForRealGroup = isCurrentUserSuperAdmin;
		//boolean hasDeletePermissionForRealGroup = isCurrentUserSuperAdmin;
		boolean hasOwnerPermissionForRealGroup = isCurrentUserSuperAdmin;
		boolean hasPermitPermissionForRealGroup = isCurrentUserSuperAdmin;

		try {
			if (!isCurrentUserSuperAdmin) {
				if (group.getAlias() != null) {// thats the real group
					hasOwnerPermissionForRealGroup = accessController.isOwnerLegacy(group.getAlias(), iwc);
					if (!hasOwnerPermissionForRealGroup) {
						hasViewPermissionForRealGroup = accessController.hasViewPermissionFor(group.getAlias(), iwc);
						//hasEditPermissionForRealGroup = accessController
						//		.hasEditPermissionFor(group.getAlias(),
						//				this.getUserContext());
						//hasDeletePermissionForRealGroup = accessController
						//		.hasDeletePermissionFor(group.getAlias(),
						//				this.getUserContext());
						hasPermitPermissionForRealGroup = accessController.hasPermitPermissionFor(group.getAlias(), iwc);
					} else {
						// the user is the owner so he can do anything
						hasViewPermissionForRealGroup = true;
						//hasEditPermissionForRealGroup = true;
						//hasDeletePermissionForRealGroup = true;
						hasPermitPermissionForRealGroup = true;
					}
				} else if (group != null) {
					hasOwnerPermissionForRealGroup = accessController.isOwnerLegacy(group, iwc);
					if (!hasOwnerPermissionForRealGroup) {
						hasViewPermissionForRealGroup = accessController.hasViewPermissionFor(group, iwc);
						//hasEditPermissionForRealGroup = accessController
						//		.hasEditPermissionFor(group,
						//				this.getUserContext());
						//hasDeletePermissionForRealGroup = accessController
						//		.hasDeletePermissionFor(group,
						//				this.getUserContext());
						hasPermitPermissionForRealGroup = accessController.hasPermitPermissionFor(group, iwc);
					} else {
						// the user is the owner so he can do anything
						hasViewPermissionForRealGroup = true;
						//hasEditPermissionForRealGroup = true;
						//hasDeletePermissionForRealGroup = true;
						hasPermitPermissionForRealGroup = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return hasViewPermissionForRealGroup || hasPermitPermissionForRealGroup;
	}


	private void generateReport() throws RemoteException, JRException {
		if (this.dataSource == null) {
			getLogger().warning("Datasource is not provided!");
			return;
		}

		JasperReportBusiness business = getReportBusiness();
		if (doGenerateSomeJasperReport() && (this.dataSource != null && this.design != null)) {
			getLogger().info("Using datasource " + dataSource + " and design: " + design);
			this.reportDescription.put(DynamicReportDesign.PRM_REPORT_NAME, this.reportName);
			JasperPrint print = business.getReport(this.dataSource, this.reportDescription.getDisplayValueMap(), this.design);
			getLogger().info("Got print: " + print);

			if (this.reportFilePathsMap == null) {
				this.reportFilePathsMap = new HashMap<String, String>();
			}

			if (this.generateHTMLReport) {
				getLogger().info("Fetching HTML report for " + reportName + " from print " + print);
				String html = business.getHtmlReport(print, "report");
				getLogger().info("HTML report (" + reportName + "): " + html);
				this.reportFilePathsMap.put(HTML_FORMAT, html);
			}

			if (this.generatePDFReport) {
				getLogger().info("Fetching PDF report for " + reportName + " from print " + print);
				String pdf = business.getPdfReport(print, "report");
				getLogger().info("PDF report (" + reportName + "): " + pdf);
				this.reportFilePathsMap.put(PDF_FORMAT, pdf);
			}

			if (this.generateXMLReport) {
				getLogger().info("Fetching XML report for " + reportName + " from print " + print);
				String xml = business.getXmlReport(print, "report");
				getLogger().info("XML report (" + reportName + "): " + xml);
				this.reportFilePathsMap.put(XML_FORMAT, xml);
			}

			if (this.generateExcelReport &&
					(IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("data_report.always_generate_excel", false) ||
					!(this.generateSimpleExcelReport && (this.dataSource instanceof ReportableCollection)))
			) {
				getLogger().info("Fetching Excel report for " + reportName + " from print " + print);
				String excel = business.getExcelReport(print, "report");
				getLogger().info("Excel report (" + reportName + "): " + excel);
				this.reportFilePathsMap.put(EXCEL_FORMAT, excel);
			}
		}

		if (this.generateSimpleExcelReport && (this.dataSource instanceof ReportableCollection)) {
			getLogger().info("Using simple Excel format");
			if (this.reportFilePathsMap == null) {
				this.reportFilePathsMap = new HashMap<String, String>();
			}
			this.reportFilePathsMap.put(SIMPLE_EXCEL_FORMAT, business.getSimpleExcelReport(((ReportableCollection) this.dataSource).getJRDataSource(), this.reportName, this.reportDescription));
		}
	}

	/**
	 * @return
	 */
	private boolean doGenerateSomeJasperReport() {
		return (this.generateExcelReport || this.generateHTMLReport || this.generateXMLReport || this.generatePDFReport);
	}

	public JasperReportBusiness getReportBusiness() {
		try {
			return IBOLookup.getServiceInstance(getIWApplicationContext(), JasperReportBusiness.class);
		}
		catch (RemoteException ex) {
			System.err.println("[ReportLayoutChooser]: Can't retrieve JasperReportBusiness. Message is: " + ex.getMessage());
			throw new RuntimeException("[ReportLayoutChooser]: Can't retrieve ReportBusiness");
		}
	}

	public void setQuery(Integer queryPK) {
		this.queryPK = queryPK;
	}

	public void setMethodInvocationICFileID(Integer methodInvocationPK) {
		this.methodInvocationPK = methodInvocationPK;
	}

	public void setMethodInvocation(Integer methodInvocationPK) {
		setMethodInvocationICFileID(methodInvocationPK);
	}

	public void setMethodInvocationICFile(ICFile file) {
		if (file != null) {
			setMethodInvocationICFileID((Integer) file.getPrimaryKey());
		}
	}

	public void setMethodInvocationBundleAndFileName(IWBundle bundle, String fileName) {
		this.methodInvocationFileName = fileName;
		this.methodInvocationIWBundle = bundle;
	}

	public void setMethodInvocationFileNameAndUseDefaultBundle(String fileName) {
		setMethodInvocationBundleAndFileName(null, fileName);
	}

	public void setLayoutICFile(ICFile file) {
		if (file != null) {
			this.layoutICFilePK = (Integer) file.getPrimaryKey();
		}
	}

	public void setLayoutBundleAndFileName(IWBundle bundle, String fileName) {
		this.layoutFileName = fileName;
		this.layoutIWBundle = bundle;
	}

	public void setLayoutFileNameAndUseDefaultBundle(String fileName) {
		setLayoutBundleAndFileName(null, fileName);
	}

	public void setLayoutICFileID(Integer layoutICFilePK) {
		this.layoutICFilePK = layoutICFilePK;
	}

	private boolean reportState = false;

	public boolean isReportState() {
		return reportState;
	}

	public void setReportState(boolean reportState) {
		this.reportState = reportState;
	}

	private String result = null;

	@SuppressWarnings("unchecked")
	public <V extends Serializable> V getResult() {
		V result = (V) this.result;
		if (MapUtil.isEmpty(getReportFilePathsMap())) {
			return result;
		}

		result = (V) getReportFilePathsMap();
		return result;
	}

	@Override
	public void main(IWContext iwc) throws Exception {
		ReportGeneratorThread thread = new ReportGeneratorThread();

		IWResourceBundle iwrb = getResourceBundle(iwc);
		if (!iwc.isParameterSet(PRM_REPORT_NAME) && this.reportName.equals(DEFAULT_REPORT_NAME)) {
			this.reportName = iwrb.getLocalizedString(PRM_REPORT_NAME, DEFAULT_REPORT_NAME);
		}

		try {
			if (this.queryPK != null) {
				String genState = iwc.getParameter(PRM_STATE);
				if (genState == null || "".equals(genState)) {
					parseQuery(iwc);
					lineUpElements(iwrb, iwc);
					Form submForm = new Form();
					submForm.maintainParameters(this.maintainParameterList);
					submForm.add(this.fieldTable);
					this.add(submForm);
				}
				else {
					getLogger().info("Parsing query: start");
					parseQuery(iwc);
					getLogger().info("Parsing query: done");

					getLogger().info("Generating data source: start");
					ThreadRunDataSourceCollector dataSoruce = generateDataSource(iwc);
					getLogger().info("Generated data source: " + dataSoruce);

					getLogger().info("Generating layout: start");
					getLayoutFromICFileOrGenerate(iwc);
					getLogger().info("Generating layout: done");

					try {
						getLogger().info("Generating report: start");
						generateReport();
						getLogger().info("Generating report: done");
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error generating report: " + reportName, e);
					}

					this.add(getReportLink(iwc));
				}
			}
			else if ((this.methodInvocationPK != null) || (this.methodInvocationFileName != null)) {
				String genState = iwc.getParameter(PRM_STATE);
				boolean generateReport = isReportState();
				generateReport = generateReport ? generateReport : !StringUtil.isEmpty(genState);
				if (!generateReport) {
					parseMethodInvocationXML(iwc, iwrb);
					lineUpElements(iwrb, iwc);
					Form submForm = new Form();
					submForm.maintainParameters(this.maintainParameterList);
					submForm.add(this.fieldTable);
					this.add(submForm);
				}
				else {
					if (this.runAsThread) {
						if (this.getEmail() == null || "".equals(getEmail().trim())) {
							this.add(iwrb.getLocalizedString("report_generator.cant_run_thread","This report can't be executed for users without email."));
							return;
						}

						parseMethodInvocationXML(iwc, iwrb);
						ThreadRunDataSourceCollector collector = generateDataSource(iwc);

						String tmpReportName = iwc.isParameterSet(getParameterName(PRM_REPORT_NAME)) ? iwc.getParameter(getParameterName(PRM_REPORT_NAME)) : reportName;

						thread.setDataSource(this.dataSource);
						thread.setGenerateExcelReport(this.generateExcelReport);
						thread.setGenerateXMLReport(this.generateXMLReport);
						thread.setGenerateHTMLReport(this.generateHTMLReport);
						thread.setGeneratePDFReport(this.generatePDFReport);
						thread.setGenerateSimpleExcelReport(this.generateSimpleExcelReport);
						thread.setReportDescription(this.reportDescription);
						thread.setReportName(this.reportName);
						thread.setDesign(this.design);
						thread.setEmail(this.getEmail());
						thread.setCollector(collector);
						thread.setTmpReportName(tmpReportName);
						thread.setCurrentLocale(iwc.getCurrentLocale());
						thread.setLayoutFileName(this.layoutFileName);
						thread.setLayoutICFilePK(this.layoutICFilePK);
						thread.setMethodInvocationPK(this.methodInvocationPK);
						thread.setMethodInvocationFileName(this.methodInvocationFileName);
						thread.setLayoutIWBundle(this.layoutIWBundle);
						thread.setIWApplicationContext(iwc.getApplicationContext());
						thread.setDynamicFields(this.dynamicFields);

						thread.start();

						result = iwrb.getLocalizedString("report_generator.running_as_thread","This report is now running in the background. The result will be sent to you on the supplied email when it's done. Please don't re-run the report.");
						this.add(result);
					} else {
						getLogger().info("2 Parsing method invocation XML: start");
						parseMethodInvocationXML(iwc, iwrb);
						getLogger().info("2 Parsing method invocation XML: done");

						getLogger().info("2 Generating data source: start");
						ThreadRunDataSourceCollector dataSoruce = generateDataSource(iwc);
						getLogger().info("2 Generated data source: " + dataSoruce);

						if (doGenerateSomeJasperReport()) {
							getLogger().info("2 Getting layout: start");
							getLayoutFromICFileOrGenerate(iwc);
							getLogger().info("2 Getting layout: done");
						} else {
							getLogger().info("2 Preparing layout: start");
							prepareForLayoutGeneration(iwc, true);
							getLogger().info("2 Preparing layout: done");
						}
						try {
							getLogger().info("2 Generating report: start");
							generateReport();
							getLogger().info("2 Generating report: done");

							this.add(getReportLink(iwc));
						} catch (Exception e) {
							this.add(iwrb.getLocalizedString("report_generator.error_generating_report","Error generating report"));
							getLogger().log(Level.WARNING, "Error generating report: " + reportName, e);
						}
					}

				}
			}
			else if (hasEditPermission()) {
				add(iwrb.getLocalizedString("no_query_has_been_chosen_for_this_instance", "No query has been chosen for this instance"));
			} // else{//Do nothing}
		}
		catch (OutOfMemoryError e) {
			add(iwrb.getLocalizedString("datareport.out_of_memory", "The server was not able to finish your request. Try to be more specific in your request or partition it so the result will be smaller."));
			add(Text.getBreak());
			add(Text.getBreak());
			BackButton back = new BackButton();
			setStyle(back);
			add(back);
			e.printStackTrace();

			getLogger().log(Level.WARNING, "Error generating report: " + reportName, e);
		}
		catch (ReportGeneratorException e) {
			add(e.getLocalizedMessage());
			add(Text.getBreak());
			add(Text.getBreak());
			BackButton back = new BackButton();
			setStyle(back);
			add(back);

			// TMP
			Throwable cause = e.getCause();
			if (cause != null) {
				cause.printStackTrace();
			}
			else {
				e.printStackTrace();
			}

			getLogger().log(Level.WARNING, "Error generating report: " + reportName, e);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error generating report: " + reportName, e);
		}
	}

	/**
	 *
	 */
	private void parseMethodInvocationXML(IWContext iwc, IWResourceBundle iwrb) throws IDOLookupException, ReportGeneratorException {
		MethodInvocationXMLFile file = null;
		InputStream fileStream = null;

		if (this.methodInvocationPK != null) {
			try {
				file = (MethodInvocationXMLFile) ((MethodInvocationXMLFileHome) IDOLookup.getHome(MethodInvocationXMLFile.class)).findByPrimaryKey(this.methodInvocationPK);

				fileStream = file.getFileValue();
			}
			catch (FinderException e) {
				throw new ReportGeneratorException(iwrb.getLocalizedString("report_transcription_not_found", "The report transcription was not found"), e);
				// e.printStackTrace();
			}
		}
		else if (this.methodInvocationFileName != null) {
			if (this.methodInvocationIWBundle == null) {
				this.methodInvocationIWBundle = getBundle(iwc);
			}

			String realPath = this.methodInvocationIWBundle.getRealPathWithFileNameString(this.methodInvocationFileName);
			try {
				File tmp = new File(realPath);
				fileStream = tmp.exists() && tmp.canRead() ? new FileInputStream(realPath) : null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (fileStream == null) {
				String uri = realPath.substring(realPath.indexOf("/idegaweb/bundles/"));
				try {
					File tmpFile = IWBundleResourceFilter.copyResourceFromJarToWebapp(iwc.getIWMainApplication(), uri);
					fileStream = new FileInputStream(tmpFile);
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error getting stream for " + this.methodInvocationFileName + " in " + this.methodInvocationIWBundle + " using request: " + uri, e);
				}
			}
		}

		if (fileStream != null) {
			try {
				this.methodInvokeDoc = (MethodInvocationDocument) new MethodInvocationParser().parse(fileStream);
			}
			catch (XMLException e1) {
				throw new ReportGeneratorException(iwrb.getLocalizedString("error_while_parsing_transcription", "Error occured when trying to read the report generation transcription file"), e1);
			}
		}

		if (this.methodInvokeDoc != null) {
			List<MethodDescription> methods = this.methodInvokeDoc.getMethodDescriptions();
			if (methods != null) {
				Iterator<MethodDescription> iter = methods.iterator();
				if (iter.hasNext()) {
					MethodDescription mDesc = iter.next();

					MethodInput mInput = mDesc.getInput();
					if (mInput != null) {
						this.dynamicFields.addAll(mInput.getClassDescriptions());
					}
				}
			}
		}
	}

	/**
	 * @param iwc
	 * @return
	 */
	private PresentationObject getReportLink(IWContext iwc) {
		IWResourceBundle iwrb = getResourceBundle(iwc);
		Table reports = new Table();
		reports.mergeCells(1, 1, 2, 1);
		reports.add(getResourceBundle(iwc).getLocalizedString("ReportGenerator.click_on_format", "Select a link for the desired output format."), 1, 1);

		String formats[] = new String[] { EXCEL_FORMAT, SIMPLE_EXCEL_FORMAT, PDF_FORMAT, XML_FORMAT, HTML_FORMAT };
		String formatNames[] = new String[] { iwrb.getLocalizedString(EXCEL_FORMAT, "Excel"), iwrb.getLocalizedString(SIMPLE_EXCEL_FORMAT, "Excel without template"), iwrb.getLocalizedString(PDF_FORMAT, "PDF"), iwrb.getLocalizedString(XML_FORMAT, "XML"), iwrb.getLocalizedString(HTML_FORMAT, "HTML") };

		int j = 1;
		for (int i = 0; i < formats.length; i++) {
			String relativeFilePath = this.reportFilePathsMap.get(formats[i]);
			if (relativeFilePath != null) {
				j++;
				Link link = new Link(this.reportName, relativeFilePath);
				link.setTarget(Link.TARGET_NEW_WINDOW);
				// DownloadLink link = new DownloadLink(_reportName);
				// link.setRelativeFilePath(relativeFilePath);
				//
				reports.add(formatNames[i] + " : ", 1, j);
				reports.add(link, 2, j);
			}
		}
		return reports;
	}

	/**
	 *
	 */
	private void lineUpElements(IWResourceBundle iwrb, IWContext iwc) {
		// IWMainApplication iwma = iwc.getApplicationContext().getIWMainApplication();
		// IWBundle coreBundle = iwma.getBundle(IW_CORE_BUNDLE_IDENTIFIER);

		this.fieldTable = new Table();
		// _fieldTable.setBorder(1);

		int row = 0;

		if (this.canChangeReportName || (!this.canChangeReportName && this.showReportNameInputIfCannotChangeIt)) {
			row++;
			this.fieldTable.add(getFieldLabel(iwrb.getLocalizedString("choose_report_name", "Report name")) + ":", 1, row);
			InterfaceObject nameInput = getFieldInputObject(this.PRM_REPORT_NAME); // null, String.class);
			nameInput.setDisabled(!this.canChangeReportName);
			nameInput.setValue(this.reportName);
			this.fieldTable.add(nameInput, 2, row);
		}

		// TODO Let Reportable field and ClassDescription impliment the same
		// interface (IDODynamicReportableField) to decrease code duplications
		if (this.queryPK != null) {
			if (this.reportableFields.size() > 0) {

				Iterator<ReportableField> iterator = this.reportableFields.iterator();

				while (iterator.hasNext()) {
					ReportableField element = iterator.next();
					row++;
					this.fieldTable.add(getFieldLabel(element.getLocalizedName(iwc.getCurrentLocale())) + ":", 1, row);
					InterfaceObject input = getFieldInputObject(element.getName()); // null, element.getValueClass());
					// _busy.addDisabledObject(input);
					this.fieldTable.add(input, 2, row);
				}

			}
		}
		else {

			if (this.dynamicFields.size() > 0) {

				Iterator iterator = this.dynamicFields.iterator();
				while (iterator.hasNext()) {
					try {
						ClassDescription element = (ClassDescription) iterator.next();

						row++;
						this.fieldTable.add(getFieldLabel(element.getLocalizedName(iwc.getCurrentLocale())) + ":", 1, row);

						ClassHandler cHandler = element.getClassHandler();
						PresentationObject input = null;
						if (cHandler != null) {
							InputHandler iHandler = cHandler.getHandler();
							input = iHandler.getHandlerObject(getParameterName(element.getName()), cHandler.getValue(), iwc);
							setStyle(input);
						}
						else {
							input = getFieldInputObject(element.getName()); // null, element.getClassObject());
							// _busy.addDisabledObject(input);
						}
						this.fieldTable.add(input, 2, row);

					}
					catch (InstantiationException e) {
						e.printStackTrace();
					}
					catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}

			}

		}

		InterfaceObject generateButton = (InterfaceObject) getSubmitButton(iwrb.getLocalizedString("generate_report", " Generate "));
		this.fieldTable.add(generateButton, 1, ++row);
		this.fieldTable.add(new HiddenInput(PRM_STATE, VALUE_STATE_GENERATE_REPORT), 1, row);
		if (this.fieldTable.getRows() > 1) {
			this.fieldTable.mergeCells(1, row, 2, row);
		}
		this.fieldTable.setColumnAlignment(1, Table.HORIZONTAL_ALIGN_RIGHT);

		this.fieldTable.mergeCells(1, row, 2, row);
		this.fieldTable.setColumnAlignment(1, Table.HORIZONTAL_ALIGN_RIGHT);

	}

	private PresentationObject getSubmitButton(String text) {
		SubmitButton button = new SubmitButton(text, PRM_STATE, VALUE_STATE_GENERATE_REPORT);
		setStyle(button);
		return button;
	}

	private Text getFieldLabel(String text) {
		Text fieldLabel = new Text(text);
		setStyle(fieldLabel);
		return fieldLabel;
	}

	private String getParameterName(String key) {
		return PRIFIX_PRM + key;
	}

	private Object getParameterObject(IWContext iwc, String prmValue, Class prmClassType) throws ReportGeneratorException {
		Locale locale = iwc.getCurrentLocale();
		IWResourceBundle iwrb = getResourceBundle(iwc);
		if (prmValue != null) {
			if (prmClassType.equals(Integer.class)) {
				try {
					return Integer.decode(prmValue);
				}
				catch (NumberFormatException e) {
					throw new ReportGeneratorException("'" + prmValue + "' " + iwrb.getLocalizedString("integer_format_not_right", "is not of the right format, it should be an integer"), e);
				}
			}
			else if (prmClassType.equals(Time.class)) {
				DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
				try {
					java.util.Date current = df.parse(prmValue);
					return new Time(current.getTime());
				}
				catch (ParseException e) {
					throw new ReportGeneratorException("'" + prmValue + "' " + iwrb.getLocalizedString("time_format_not_right_" + locale.getLanguage() + "_" + locale.getCountry(), "is not of the right format, it should be of the format: " + df.format(IWTimestamp.RightNow().getDate())), e);
				}
			}
			else if (prmClassType.equals(Date.class)) {
				DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
				try {
					java.util.Date current = df.parse(prmValue);
					return new Date(current.getTime());
				}
				catch (ParseException e) {

					throw new ReportGeneratorException("'" + prmValue + "' " + iwrb.getLocalizedString("date_format_not_right_" + locale.getLanguage() + "_" + locale.getCountry(), "is not of the right format, it should be of the format: " + df.format(IWTimestamp.RightNow().getDate())), e);
				}
			}
			else if (prmClassType.equals(Timestamp.class)) {
				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, iwc.getCurrentLocale());
				try {
					java.util.Date current = df.parse(prmValue);
					return new Timestamp(current.getTime());
				}
				catch (ParseException e) {
					throw new ReportGeneratorException("'" + prmValue + "' " + iwrb.getLocalizedString("timestamp_format_not_right_" + locale.getLanguage() + "_" + locale.getCountry(), "is not of the right format, it should be of the format: " + df.format(IWTimestamp.RightNow().getDate())), e);
				}
			}
		}
		// else {
		return prmValue;
		// }

	}

	private InterfaceObject getFieldInputObject(String key) { // String value, Class dataType) {

		// if(dataType == Integer.class){
		// IntegerInput fieldInput = new IntegerInput(getParameterName(key));
		// setStyle(fieldInput);
		// return fieldInput;
		// }else if(dataType == Time.class){
		// TimeInput fieldInput = new TimeInput(getParameterName(key));
		// setStyle(fieldInput);
		// return fieldInput;
		// }else if(dataType == Date.class){
		// DateInput fieldInput = new DateInput(getParameterName(key));
		// setStyle(fieldInput);
		// return fieldInput;
		// }else if(dataType == Timestamp.class){
		// TimestampInput fieldInput = new
		// TimestampInput(getParameterName(key));
		// setStyle(fieldInput);
		// return fieldInput;
		// }else{
		TextInput fieldInput = new TextInput(getParameterName(key));
		setStyle(fieldInput);
		return fieldInput;
		// }
	}

	public void setStyle(PresentationObject obj) {
		if (obj instanceof Text) {
			this.setStyle((Text) obj);
		}
		else {
			obj.setMarkupAttribute("style", STYLE);
		}
	}

	public void setStyle(Text obj) {
		obj.setMarkupAttribute("style", STYLE_2);
	}

	@Override
	public synchronized Object clone() {
		ReportGenerator clone = (ReportGenerator) super.clone();

		clone.reportableFields = new ArrayList<>();
		clone.dynamicFields = new ArrayList<>();
		clone.dataSource = null;
		clone.design = null;
		clone.reportFilePathsMap = null;
		clone.queryParser = null;
		clone.fieldTable = null;
		clone.reportDescription = null;// new ReportDescription();

		return clone;
	}

	public void setReportName(String name) {
		if (name != null && !"".equals(name)) {
			this.canChangeReportName = false;
			this.reportName = name;
		}
	}

	public void setGenerateExcelReport(boolean value) {
		this.generateExcelReport = value;
	}

	public void setGenerateXMLReport(boolean value) {
		this.generateXMLReport = value;
	}

	public void setGenerateHTMLReport(boolean value) {
		this.generateHTMLReport = value;
	}

	public void setGeneratePDFReport(boolean value) {
		this.generatePDFReport = value;
	}

	public void setGenerateSimpleExcelReport(boolean value) {
		this.generateSimpleExcelReport = value;
	}


	public void setRunAsThread(boolean value) {
		this.runAsThread = value;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	private class ReportGeneratorException extends Exception {

		private static final long serialVersionUID = 8761255223787885849L;

		// jdk 1.3 - 1.4 fix
		private Throwable _cause = this;

		private String _localizedMessage = null;

		//
		// private String _localizedKey = null;
		// private String _defaultUserFriendlyMessage = null;

		/*public ReportGeneratorException(String tecnicalMessage, Throwable cause, String localizedMessage) {
			this(tecnicalMessage, cause);
			this._localizedMessage = localizedMessage;
			// _localizedKey = localizedKey;
			// _defaultUserFriendlyMessage = defaultUserFriendlyMessage;
		}*/

		/**
		 * @param message
		 * @param cause
		 */
		public ReportGeneratorException(String message, Throwable cause) {
			super(message);
			this._localizedMessage = message;
			// jdk 1.3 - 1.4 fix
			this._cause = cause;
		}

		// jdk 1.3 - 1.4 fix
		@Override
		public Throwable getCause() {
			return this._cause;
		}

		// public String getLocalizedMessageKey(){
		// return _localizedKey;
		// }
		//
		// public String getDefaultLocalizedMessage(){
		// return _defaultUserFriendlyMessage;
		// }

		@Override
		public String getLocalizedMessage() {
			return this._localizedMessage;
		}

	}

	public Object getForInvocationOfMethod() {
		return forInvocationOfMethod;
	}

	public void setForInvocationOfMethod(Object forInvocationOfMethod) {
		this.forInvocationOfMethod = forInvocationOfMethod;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public boolean isGenerateStatistics() {
		return generateStatistics;
	}

	public void setGenerateStatistics(boolean generateStatistics) {
		this.generateStatistics = generateStatistics;
	}

	public List<Integer> getGroupsIds() {
		return groupsIds;
	}

	public void setGroupsIds(List<Integer> groupsIds) {
		this.groupsIds = groupsIds;
	}

}