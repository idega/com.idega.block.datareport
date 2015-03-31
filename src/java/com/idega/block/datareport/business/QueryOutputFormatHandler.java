package com.idega.block.datareport.business;

import java.util.List;

import com.idega.block.datareport.presentation.QueryResultViewer;
import com.idega.core.builder.presentation.ICPropertyHandler;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.presentation.PresentationObject;
import com.idega.presentation.ui.DropdownMenu;

public class QueryOutputFormatHandler implements ICPropertyHandler {

	public static final String ENTITY_IW_BUNDLE_IDENTIFIER = "com.idega.block.entity";

	@Override
	public List getDefaultHandlerTypes() {
		return null;
	}

	@Override
	public void onUpdate(String[] arg0, IWContext arg1) {
	}

	@Override
	public PresentationObject getHandlerObject(String name, String stringValue, IWContext iwc, boolean oldGenerationHandler, String instanceId, String method) {
		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(ENTITY_IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc.getCurrentLocale());
		DropdownMenu menu = new DropdownMenu(name);
		menu.addMenuElement("", "");
		menu.addMenuElement(QueryResultViewer.EXCEL_KEY, iwrb.getLocalizedString(QueryResultViewer.EXCEL_KEY, QueryResultViewer.EXCEL_KEY));
		menu.addMenuElement(QueryResultViewer.PDF_KEY, iwrb.getLocalizedString(QueryResultViewer.PDF_KEY, QueryResultViewer.PDF_KEY));
		menu.addMenuElement(QueryResultViewer.HTML_KEY, iwrb.getLocalizedString(QueryResultViewer.HTML_KEY, QueryResultViewer.HTML_KEY));
		menu.addMenuElement(QueryResultViewer.XML_KEY, iwrb.getLocalizedString(QueryResultViewer.XML_KEY, QueryResultViewer.XML_KEY));
		menu.setSelectedElement(stringValue);
		return menu;
	}
}