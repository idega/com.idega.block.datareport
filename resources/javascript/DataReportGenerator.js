if (DataReportGenerator == null) var DataReportGenerator = {};

DataReportGenerator.submit = function(formId, paramState, valueState) {
	showLoadingMessage('');
	
	var host = window.location.protocol + '//' + window.location.hostname;
	jQuery('#' + formId).append('<input type="hidden" name="reportGeneratedOn" value="' + host + '" />')
	
	var form = document.getElementById(formId);
	
	var input = jQuery(form[paramState]);
	input.val(valueState);
	
	//Select all the clubs, if not selected at all
	DataReportGenerator.selectAllInSelectionBoxIfNoneSelected(jQuery(form['dr_clubs']));
	DataReportGenerator.selectAllInSelectionBoxIfNoneSelected(jQuery(form["dr_umficlubs"]));
	
	form.submit();
}

DataReportGenerator.selectAllInSelectionBoxIfNoneSelected = function(input) {
	if (input != null) {
		var noElementsSelected = true;
		var options = input[0];
		for( i = 0; i < options.length; i++ ) {
		    if (options[i].selected) {
		      noElementsSelected = false;
		    }
		}
		if (noElementsSelected){
		    for( i = 0; i < options.length; i++ ) {
		    	options[i].selected = true;
		    }
		}
	}
}

