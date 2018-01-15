if (DataReportGenerator == null) var DataReportGenerator = {};

DataReportGenerator.submit = function(formId, paramState, valueState) {
	showLoadingMessage('');
	
	var host = window.location.protocol + '//' + window.location.hostname;
	jQuery('#' + formId).append('<input type="hidden" name="reportGeneratedOn" value="' + host + '" />')
	
	var form = document.getElementById(formId);
	
	var input = jQuery(form[paramState]);
	input.val(valueState);
	
	form.submit();
}