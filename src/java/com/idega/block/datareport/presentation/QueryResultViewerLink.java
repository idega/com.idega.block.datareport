package com.idega.block.datareport.presentation;

import com.idega.presentation.IWContext;
import com.idega.presentation.text.Link;

public class QueryResultViewerLink extends Link {
	
	
	private String text = null;
	private int queryId = -1;
	private int designId = -1;
	private String outputFormat = null;

	public QueryResultViewerLink() {
		super();
	}

	public QueryResultViewerLink(String text) {
		super(text);
	}

	public void setQueryId(int queryId) {
		this.queryId = queryId;
	}

	public void setDesignId(int designId) {
		this.designId = designId;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void main(IWContext iwc) throws Exception {
		setWindowToOpen(QueryResultViewer.class);
        if (this.text != null) {
        	super.setText(this.text);
        }
		addParameter(QueryResultViewer.QUERY_ID_KEY, this.queryId);
        addParameter(QueryResultViewer.DESIGN_ID_KEY, this.designId);
        if (this.outputFormat != null) {
        	addParameter(QueryResultViewer.OUTPUT_FORMAT_KEY, this.outputFormat);
        } else {
        	addParameter(QueryResultViewer.OUTPUT_FORMAT_KEY, QueryResultViewer.EXCEL_KEY);
        }
        super.main(iwc);
	}
}