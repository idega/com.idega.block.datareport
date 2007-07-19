package com.idega.block.datareport.business;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;

import com.idega.block.dataquery.business.QueryService;
import com.idega.block.dataquery.data.QueryRepresentation;
import com.idega.business.IBOLookup;
import com.idega.core.builder.presentation.ICPropertyHandler;
import com.idega.presentation.IWContext;
import com.idega.presentation.PresentationObject;
import com.idega.presentation.ui.DropdownMenu;

public class QueryHandler implements ICPropertyHandler {

	public List getDefaultHandlerTypes() {
		return null;
	}

	public PresentationObject getHandlerObject(String name, String stringValue, IWContext iwc) {
		DropdownMenu menu = new DropdownMenu(name);
		menu.addMenuElement("", "");
		try {
			QueryService business = (QueryService) IBOLookup.getServiceInstance(iwc, QueryService.class);
			Collection queries = business.getQueries(iwc);
			if (queries != null) {
				Iterator iter = queries.iterator();
				while (iter.hasNext()) {
					QueryRepresentation element = (QueryRepresentation) iter.next();
					menu.addMenuElement(element.getPrimaryKey().toString(), (String)element.getColumnValue(QueryRepresentation.NAME_KEY));
					menu.setSelectedElement(stringValue);
				}
			}
		}
		catch (RemoteException re) {
			re.printStackTrace();
		}
		catch (FinderException re) {
			re.printStackTrace();
		}

		return menu;
	}

	public void onUpdate(String[] values, IWContext iwc) {
	}
}
