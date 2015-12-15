/*
 * Created on 10.8.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.idega.block.datareport.xml.methodinvocation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.idega.xml.XMLElement;
import com.idega.xml.XMLException;

/**
 * Title:		MethodInput
 * Description:
 * Copyright:	Copyright (c) 2003
 * Company:		idega Software
 * @author		2003 - idega team - <br><a href="mailto:gummi@idega.is">Gudmundur Agust Saemundsson</a><br>
 * @version		1.0
 */
public class MethodInput extends XMLElement {

	private static final long serialVersionUID = 7516449381268714742L;

	static final String NAME = "input";
	private List<ClassDescription> _parameterClasses = new ArrayList<ClassDescription>();

	/**
	 * @param name
	 */
	public MethodInput() {
		super(NAME);
	}

	/**
	 * @param element
	 */
	public MethodInput(XMLElement element) throws XMLException {
		this();
		initialize(element);
	}

	private void initialize(XMLElement element) throws XMLException {
		List<XMLElement> methodDescriptions = element.getChildren(ClassDescription.NAME);
		Iterator<XMLElement> iter = methodDescriptions.iterator();
		if(iter != null){
			while (iter.hasNext()) {
				XMLElement localizedName = iter.next();
				this._parameterClasses.add(new ClassDescription(localizedName));
			}
		}
	}

	public void close(){

	}

	public List<ClassDescription> getClassDescriptions(){
		return this._parameterClasses;
	}


}
