package com.idega.block.datareport.presentation;

import com.idega.presentation.IWContext;
import com.idega.presentation.text.Link;

public class QueryResultViewerLink extends Link {
	
	private int queryId = -1;
	private int designId = -1;
	private String outputFormat = null;

	public QueryResultViewerLink() {
		super();
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

	public void main(IWContext iwc) throws Exception {
		setWindowToOpen(QueryResultViewerWindow.class);
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