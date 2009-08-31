
<%@ taglib uri="/WEB-INF/yrc-www.tld" prefix="yrcwww" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>


 
<%@ include file="/includes/header.jsp" %>

<%@ include file="/includes/errors.jsp" %>


<script src="<yrcwww:link path='/js/dragtable.js'/>"></script>

<script type="text/javascript" src="<yrcwww:link path='/js/jquery.ui-1.6rc2/ui/ui.core.js'/>"></script>
<script type="text/javascript" src="<yrcwww:link path='/js/jquery.ui-1.6rc2/ui/ui.tabs.js'/>"></script>
<script type="text/javascript" src="<yrcwww:link path='/js/jquery.ui-1.6rc2/ui/ui.dialog.js'/>"></script>
<script type="text/javascript" src="<yrcwww:link path='/js/jquery.ui-1.6rc2/ui/ui.draggable.js'/>"></script>
<script type="text/javascript" src="<yrcwww:link path='/js/jquery.ui-1.6rc2/ui/ui.resizable.js'/>"></script>

<script type="text/javascript" src="<yrcwww:link path='/js/jquery.history.js'/>"></script>
<script type="text/javascript" src="<yrcwww:link path='/js/jquery.cookie.js'/>"></script>

<script src="<yrcwww:link path='/js/tooltip.js'/>"></script>

<script src="<yrcwww:link path='/js/jquery.form.js'/>"></script>

<script src="<yrcwww:link path='/js/jquery.blockUI.js'/>"></script>


<link rel="stylesheet" href="<yrcwww:link path='/css/proteinfer.css'/>" type="text/css" >

<yrcwww:notauthenticated>
 <logic:forward name="authenticate" />
</yrcwww:notauthenticated>

<logic:notPresent name="proteinProphetFilterForm">
	<logic:forward  name="viewProteinProphetResult" />
</logic:notPresent>


<%
	int pinferId = (Integer)request.getAttribute("pinferId");
%>

<script>

// ---------------------------------------------------------------------------------------
// AJAX DEFAULTS
// ---------------------------------------------------------------------------------------
  $.ajaxSetup({
  	type: 'POST',
  	//timeout: 5000,
  	dataType: 'html',
  	error: function(xhr) {
  			
  				var statusCode = xhr.status;
		  		// status code returned if user is not logged in
		  		// reloading this page will redirect to the login page
		  		if(statusCode == 303)
 					window.location.reload();
 				
 				// otherwise just display an alert
 				else {
 					alert("Request Failed: "+statusCode+"\n"+xhr.statusText);
 				}
  			}
  });
  
  $.blockUI.defaults.message = '<b>Loading...</b>'; 
  $.blockUI.defaults.css.padding = 20;
  $.blockUI.defaults.fadeIn = 0;
  $.blockUI.defaults.fadeOut = 0;
  //$().ajaxStart($.blockUI).ajaxStop($.unblockUI);
  $().ajaxStop($.unblockUI);



// FOR HISTORY
function callback(hash)
{
	var $tabs = $("#results").tabs();
    // do stuff that loads page content based on hash variable
    if(hash) {
    	$("#load").text(hash + ".html");
		var $tabs = $("#results").tabs();
		
		if(hash == 'protlist')
			$tabs.tabs('select', 0);
		else if (hash == 'protdetails')
			$tabs.tabs('select', 1);
		else if (hash == 'protclusters')
			$tabs.tabs('select', 2);
		else if (hash == 'input')
			$tabs.tabs('select', 3);
	} else {
		$tabs.tabs('select', 0);
	}
}
// FOR HISTORY
$(document).ready(function() {
    $.history.init(callback);
    $("a[@rel='history']").click(function(){
    	var hash = this.href;
		hash = hash.replace(/^.*#/, '');
        $.history.load(hash);
        return false;
    });
});

  
// ---------------------------------------------------------------------------------------
// WHAT TO DO WHEN THE DOCUMENT LOADS
// ---------------------------------------------------------------------------------------
$(document).ready(function() {
	
	// reset the form.  When clicking the reload button the form is 
	// not resest, so we reset it manually. 
 	$("#filterForm")[0].reset();
 	
 	
	var selected = 0;
	if(location.hash == "#protdetails")  selected = 1;
	if(location.hash == "#roc") selected = 2;
	
	
	// set up the tabs and select the first tab
    $("#results > ul").tabs().tabs('select', selected);
  
 	$(".stripe_table th").addClass("pinfer_A");
 	$(".stripe_table tbody > tr:odd").addClass("pinfer_A");
 	
 	
  	setupProteinListTable();
  	
   	$('table.sortable').each(function() {
    	var $table = $(this);
    	makeSortable($table);
  	});

	// Make the comments for this protein inference run editable
	makeCommentsEditable();
	  	
  	// If the protein details cookie is saved load the protein details
	var cookie = $.cookie("protdetails");
	if(cookie) {
		var cookievals = cookie.split('_');
		var pinferId = cookievals[0];
		var proteinId = cookievals[1];
		// make sure the protein inference ID saved in the cookie is the same as the results we are displaying
		if(pinferId == <%=pinferId%>) {
			var block = selected == 2;
			//alert("protein details "+block);
			showProteinDetails(proteinId, false, block);
		}
	}
});
  
// ---------------------------------------------------------------------------------------
// SAVE COMMENTS FOR A PROTEIN INFERENCE RUN
// ---------------------------------------------------------------------------------------
var writeAccess = <bean:write name='writeAccess'/>;
 
function makeCommentsEditable() {

	if(writeAccess == true) {
		$(".editableComment").click(function() {
			var id = $(this).attr('id');
			var currentComments = $.trim($("#"+id+"_text").text());
			$("#"+id+"_text").hide();
			$("#"+id+"_edit .edit_text").val(currentComments);
			$("#"+id+"_edit").show();
		});
		
		$(".savePiRunComments").click(function() {
			var id = $(this).attr('id');
			var comments = $.trim($("#experiment_"+id+"_edit .edit_text").val());
			savePiRunComments(id, comments);
		});
		
		$(".cancelPiRunComments").click(function() {
			var id = $(this).attr('id');
			$("#piRun_"+id+"_text").show();
			$("#piRun_"+id+"_edit .edit_text").text("");
			$("#piRun_"+id+"_edit").hide();
		});
	}
}

function savePiRunComments(piRunId, comments) {
	saveComments("<yrcwww:link path='saveProtInferComments.do'/>", 'piRun', piRunId, comments);
}

function saveComments(url, idName, id, comments) {
	var oldComments = $.trim($("#"+idName+"_"+id+"_text").text());
	var newComments = $.trim($("#"+idName+"_"+id+"_edit .edit_text").val());
	
	var textFieldId = "#"+idName+"_"+id+"_text";
	var textBoxId   = "#"+idName+"_"+id+"_edit";
	
	$.ajax({
		url:      url,
		dataType: "text",
		data:     {'id': 			id, 
		           'comments': 		newComments},
		beforeSend: function(xhr) {
						$(textFieldId).text("Saving....");
						$(textFieldId).show();
						$(textBoxId).hide();
					},
		success:  function(data) {
			        if(data == 'OK') {
			        	$(textFieldId).text(newComments);
			        }
			        else {
			        	$(textFieldId).text(oldComments);
			        	alert("Error saving comments: "+data);
			        }
		          },
		complete:  function(xhr, textStatus) {}
		
	});
}

// ---------------------------------------------------------------------------------------
// SHOW/HIDE PROTEIN SEQUENCE
// --------------------------------------------------------------------------------------- 
// View the protein sequence
function toggleProteinSequence (pinferProteinId) {

	//alert("protein id: "+pinferProteinId+" pinferId: "+pinferId);
	var button = $("#protseqbutton_"+pinferProteinId);
	
	if(button.text() == "[View Sequence]") {
		//alert("View");
		if($("#protsequence_"+pinferProteinId).html().length == 0) {
			//alert("Getting...");
			// load data in the appropriate div
			$.blockUI(); 
			$("#protsequence_"+pinferProteinId).load("<yrcwww:link path='proteinSequence.do'/>",   				// url
							                        {'pinferProteinId': pinferProteinId}, 	// data
							                        function(responseText, status, xhr) {	// callback
								  						$.unblockUI();
								  					});
		}
		button.text("[Hide Sequence]");
		$("#protseqtbl_"+pinferProteinId).show();
	}
	else {
		button.text("[View Sequence]");
		$("#protseqtbl_"+pinferProteinId).hide();
	}
}

// ---------------------------------------------------------------------------------------
// SHOW/HIDE HITS FOR AN ION
// --------------------------------------------------------------------------------------- 
// View all the hits for an ion
function toggleHitsForIon (pinferIonId) {

	// alert("ion id: "+pinferIonId);
	var button = $("#showhitsforion_"+pinferIonId);
	
	if(button.text() == "[Show]") {
		//alert("View");
		if($("#hitsforion_"+pinferIonId).html().length == 0) {
			//alert("Getting...");
			// load data in the appropriate div
			$.blockUI(); 
			$("#hitsforion_"+pinferIonId).load("<yrcwww:link path='psmListForIon.do'/>",   					// url
							                        {'pinferId': <%=pinferId%>, 'pinferIonId': pinferIonId}, 		// data
							                        function(responseText, status, xhr) {	// callback
								  						$.unblockUI();
								  						// make table sortable
														var table = $("#allpsms_"+pinferIonId);
														makeSortable(table);
								  					});
		}
		button.text("[Hide]");
		$("#hitsforion_"+pinferIonId).show();
	}
	else {
		button.text("[Show]");
		$("#hitsforion_"+pinferIonId).hide();
	}
}

// ---------------------------------------------------------------------------------------
// SHOW SPECTRUM
// ---------------------------------------------------------------------------------------  
function viewSpectrum (scanId, hitId) {
	//alert("View spectrum for "+scanId+"; hit: "+hitId);
	var winHeight = 500
	var winWidth = 970;
	var doc = "<yrcwww:link path='/viewSpectrum.do'/>?scanID="+scanId+"&runSearchResultID="+hitId;
	//alert(doc);
	window.open(doc, "SPECTRUM_WINDOW", "width=" + winWidth + ",height=" + winHeight + ",status=no,resizable=yes,scrollbars=yes");
}
  
// ---------------------------------------------------------------------------------------
// SHOW PROTEIN DETAILS
// --------------------------------------------------------------------------------------- 
function showProteinDetails(proteinId, display, block) {
	
	if(display == undefined) display = true;
	if(block == undefined)   block = true;
	var showDiv = location.hash != "#protdetails" && display;

	
	// load content in the appropriate div
	if(block)	$.blockUI();
	$("#protein_div").load("<yrcwww:link path='proteinDetails.do'/>",   			    // url
								  {'pinferId': <%=pinferId%>, 'pinferProtId': proteinId}, 	// data
								  function(responseText, status, xhr) {						// callback
								  		
								  		if(block)	$.unblockUI();
								  		
								  		// stripe the table
										$("#protdetailstbl_"+proteinId+" th.main").addClass("pinfer_A");
										$("#protdetailstbl_"+proteinId+" tbody tr.main").addClass("pinfer_A");
										$(this).show();
										// save a cookie
										saveProtDetailCookie(<%=pinferId%>, proteinId);
										if(showDiv) {
											$("#protdetailslink").click(); // so that history works
											//var $tabs = $("#results").tabs();
											//$tabs.tabs('select', 1);
										}
								  });
}

function saveProtDetailCookie(pinferId, proteinId) {
	var COOKIE_NAME = 'protdetails';
	var date = new Date();
    date.setTime(date.getTime() + (2 * 60 * 60 * 1000)); // expire in two hours
    $.cookie(COOKIE_NAME, pinferId+"_"+proteinId, { path: '/', expires: date });
}
  
  
// ---------------------------------------------------------------------------------------
// SHOW SPECTRUM MATCHES
// ---------------------------------------------------------------------------------------    
function showSpectrumMatches(runSearchId, runName) {
	$(".input_psm").hide();
	$("#psm_"+runSearchId).show();
	
	if($("#psm_"+runSearchId).html().length == 0) {
		$("#psm_"+runSearchId).html("<b>Loading Peptide Spectrum Matches for: "+runName+"...</b>");
		$("#psm_"+runSearchId).load("<yrcwww:link path='psmMatches.do'/>", //url
								{'pinferId': <%=pinferId%>, 'runSearchId': runSearchId},
								function(responseText, status, xhr) {						// callback
							  		// stripe the table
									$("#psmtbl_"+runSearchId+" th").addClass("pinfer_A");
									$("#psmtbl_"+runSearchId+" tr:even").addClass("pinfer_A");
									makeSortable($("#psmtbl_"+runSearchId));
									$(this).show();
							  });
	}
} 
  

// ---------------------------------------------------------------------------------------
// MAKE PROTEIN LIST TABLE SORTABLE
// ---------------------------------------------------------------------------------------
function setupProteinListTable() {
  
  	// stripe table rows
  	$("#protlisttable tbody tr.protgrp_row").addClass("pinfer_A");
  	
  	setupShowPeptidesLinks();
  	
  	makeProteinListSortable();
  	
    setupAnnotationsLinks();
}

// ---------------------------------------------------------------------------------------
// SETUP SHOW/HIDE PEPTIDES LINK
// ---------------------------------------------------------------------------------------
function setupShowPeptidesLinks() {

// this function will be called when clicking on the "Show Peptides" link and proteins in a group are linked.
  	$(".showpeptForProtGrp").click(function() {
  		
  		var id = this.id;
  		
  		if($(this).text() == "Show Peptides") {
  			$(this).text("Hide Peptides");
  			if($("#peptforprot_"+id).html().length == 0) {
  				//alert("Sending request for proteinGroup: "+id);
  				$.blockUI();
  				$("#peptforprot_"+id).load("<yrcwww:link path='getProteinPeptides.do'/>", 	//url
  									{'pinferId': <%=pinferId%>, 		// data
  									 'proteinGroupId': id
  									 },
  									 function(responseText, status, xhr) {						// callback
  									 	$.unblockUI();
  										$(this).show();
  										makeSortable($("#peptforprottbl_"+id));
  								   });
  			}
  			else {
  				$("#peptforprot_"+id).show();
  			}
  		}
  		else {
  			$(this).text("Show Peptides");
  			$("#peptforprot_"+id).hide();
  		}
  	});
  	
  	
  	// this function will be called when clicking on the "Show Peptides" link and proteins in a group are NOT linked.
  	$(".showpeptForProt").click(function() {
  	
  		var grpId =  this.title;
  		var protId = this.id;
  		if($(this).text() == "Show Peptides") {
  			$(this).text("Hide Peptides");
  			if($("#peptforprot_"+protId+"_"+grpId).html().length == 0) {
  				//alert("Sending request");
  				$.blockUI();
  				$("#peptforprot_"+protId+"_"+grpId).load("<yrcwww:link path='getProteinPeptides.do'/>", 	//url
  									{'pinferId': <%=pinferId%>, 					// data
  									 'proteinGroupId': grpId,
  									 'proteinId': protId
  									 },
  									 function(responseText, status, xhr) {			// callback
  									 	$.unblockUI();
  										$(this).show();
  										makeSortable($("#peptforprottbl_"+protId+"_"+grpId));
  								   });
  			}
  			else {
  				$("#peptforprot_"+protId+"_"+grpId).show();
  			}
  		}
  		else {
  			$(this).text("Show Peptides");
  			$("#peptforprot_"+protId+"_"+grpId).hide();
  		}
  	});
}

// ---------------------------------------------------------------------------------------
// SETUP ANNOTATIONS
// ---------------------------------------------------------------------------------------
function setupAnnotationsLinks() {

	if(writeAccess) {
	$("#prot_annot_dialog").dialog({
    	autoOpen: false,
    	modal: true,
    	width: 400,
    	height: 200,
    	overlay: { 
        	opacity: 0.5, 
        	background: "black" 
    	},
    	buttons: {
    		"Save": 		function() {
    			
    			var accept = $("#prot_accept").attr("checked");
    			var reject = $("#prot_reject").attr("checked");
    			var notsure = $("#prot_notsure").attr("checked");
    			var protid = $("#prot_id").val();
    			var comments = $("#prot_comments").val();
    			var validation = "U";
    			if(accept)	validation = 'A';
    			if(reject)	validation = 'R';
    			if(notsure) validation = 'N';
    			
    			// send a request to update the annotation for this protein
    			$.post("<yrcwww:link path='saveProteinAnnotation.do'/>",
    					{'pinferProtId': protid,
    					 'comments': comments,
    					 'validation': validation
    					},
    					function(data) {
    						if(data == "OK") {
    							if(comments != null && comments.length > 0) {
									$("#annot_comment_"+protid).text(comments);
									$("#annot_validation_style_"+protid).addClass('prot_annot_U');
								}

				    			if(accept) {
				    				$("#annot_validation_style_"+protid).removeClass();
				    				$("#annot_validation_style_"+protid).addClass('prot_annot_A');
				    			}
								else if (reject) {
									$("#annot_validation_style_"+protid).removeClass();
									$("#annot_validation_style_"+protid).addClass('prot_annot_R');
								}
								else if (notsure) {
									$("#annot_validation_style_"+protid).removeClass();
									$("#annot_validation_style_"+protid).addClass('prot_annot_N');
								}
					
								$("#annot_validation_text_"+protid).text(validation);
    						}
    						else {
    							alert("Error saving protein annotation.\n"+data);
    						}
    					});
    			
    			$(this).dialog("close");
    			
    		},
    		"Delete": function() {
    			var protid = $("#prot_id").val();
    			$.post("<yrcwww:link path='deleteProteinAnnotation.do'/>",
    					{'pinferProtId': protid},
    					function(data) {
    						if(data == "OK") {
    							$("#annot_comment_"+protid).text();
    							$("#annot_validation_text_"+protid).text('U');
								$("#annot_validation_style_"+protid).removeClass();
								$("#annot_validation_style_"+protid).addClass('prot_annot_U');
    						}
    						else {
    							alert("Error deleting protein annotation.\n"+data);
    						}
    			});
    			$(this).dialog("close");
    		},
    		"Cancel": 	function() {$(this).dialog("close");}
    	}
    });
    }
    else {
    	$("#prot_annot_dialog").dialog({
    	autoOpen: false,
    	modal: true,
    	width: 400,
    	height: 200,
    	overlay: { 
        	opacity: 0.5, 
        	background: "black" 
    	}});
    }
  	
  	$(".editprotannot").click(function(e){
  		
  		var protid = this.id;
  		var protname = this.title;
  		// set the values for the protein that the user is editing
  		$("#prot_id").val(protid);
  		$("#prot_name").text(protname);
  		
  		// reset the dialog
  		var comment = $("#annot_comment_"+protid).text();
  		var validation = $("#annot_validation_text_"+protid).text();
  		
  		if(comment != null)
  			$("#prot_comments").val(comment);
  		else
  			$("#prot_comments").val("");
  			
  		$("#prot_accept").attr("checked", "");
  		$("#prot_reject").attr("checked", "");
  		$("#prot_notsure").attr("checked", "");
  		
  		if(validation == "A" || validation == "U")	
  			$("#prot_accept").attr("checked", "checked");
  		
  		else if(validation == "R")
  			$("#prot_reject").attr("checked", "checked");
  			
  		else if(validation == "N") 
  			$("#prot_notsure").attr("checked", "checked");
  		
  		
  		// show the dialog
  		$("#prot_annot_dialog").dialog('open');
  		
	});

}

// ---------------------------------------------------------------------------------------
// MAKE PROTEIN LIST TABLE SORTABLE
// ---------------------------------------------------------------------------------------
function makeProteinListSortable() {
	
	// the header for the column that is sorted is highlighted
	//$('th', $table).each(function(){$(this).removeClass('ms_selected_header');});
	//$(this).addClass('ms_selected_header');
	
	var $table = $("#protlisttable");
	$('th', $table).each(function(column) {
  		
  		if ($(this).is('.sortable')) {
  		
  			var $header = $(this);
  			var sortBy = $(this).attr('id');
  			
      		$(this).hover(
      			function() {$(this).addClass('pinfer_small_hover');} , 
      			function() {$(this).removeClass('pinfer_small_hover');}).click(function() {
				
					// alert("sorting by "+sortBy);
					// sorting direction
					var sortOrder = 1;
					if ($(this).is('.sorted-asc')) {
	          			sortOrder = -1;
	        		}
	        		else if ($(this).is('.sorted-desc')) {
	          			sortOrder = 1;
	        		}
	        		else if($(this).is('.def_sort_desc')) {
	        			sortOrder = -1;
	        		}
        			sortResults(<%=pinferId%>, sortBy, sortOrder);
      			});
		}
    	});
}

// ---------------------------------------------------------------------------------------
// UPDATE RESULTS
// ---------------------------------------------------------------------------------------
$(document).ready(function() { 

	var options = {
		target:   '#protlist_table',
		beforeSubmit: beforeSubmit,
		success:  updateResults
	};
    // bind 'filterForm' and provide a callback function 
    $('#filterForm').ajaxForm(options); 
});
// validate the form parameters before submit.
function beforeSubmit() {
	
    if(!validateForm())
    	return false;
	$.blockUI();
}
function validateForm() {

	// fieldValue is a Form Plugin method that can be invoked to find the 
    // current value of a field 
    
    // peptides
    var value = $("form#filterForm input[name='minPeptides']").fieldValue();
    var valid = validateInt(value, "Min. Peptides", 1);
    if(!valid)	return false;
    var minPept = parseInt(value);
    
    var value = $("form#filterForm input[name='maxPeptides']").fieldValue();
    if(!isNaN(parseInt(value))) {
    	var valid = validateInt(value, "Max. Peptides", minPept);
    	if(!valid)	return false;
    }
    
    
    
    // unique peptides
    value = $("form#filterForm input[@name='minUniquePeptides']").fieldValue();
    valid = validateInt(value, "Min. Unique Peptides", 0, minPept);
    if(!valid)	return false;
    var minUniqPept = parseInt(value);
    
    value = $("form#filterForm input[@name='maxUniquePeptides']").fieldValue();
    if(!isNaN(parseInt(value))) {
    	valid = validateInt(value, "Max. Unique Peptides", minUniqPept);
    	if(!valid)	return false;
    }
    
    
    // coverage
    value = $("form#filterForm input[@name='minCoverage']").fieldValue();
    valid = validateFloat(value, "Min. Coverage", 0.0, 100.0);
    if(!valid)	return false;
    var minCoverage = parseFloat(value);
    
    value = $("form#filterForm input[@name='maxCoverage']").fieldValue();
    valid = validateFloat(value, "Max. Coverage", minCoverage, 100.0);
    if(!valid)	return false;
    
    
    // spectrum matches
    value = $("form#filterForm input[@name='minSpectrumMatches']").fieldValue();
    valid = validateInt(value, "Min. Spectrum Matches", 1);
    if(!valid)	return false;
    var minSpecMatch = parseInt(value);
    
    value = $("form#filterForm input[@name='maxSpectrumMatches']").fieldValue();
    if(!isNaN(parseFloat(value))) {
    	valid = validateInt(value, "Max. Spectrum Matches", minSpecMatch);
    	if(!valid)	return false;
    }

	// probability
    value = $("form#filterForm input[@name='minProbability']").fieldValue();
    valid = validateFloat(value, "Min. Probability", 0.0, 1.0);
    if(!valid)	return false;
    var minProb = parseFloat(value);
    
    value = $("form#filterForm input[@name='maxProbability']").fieldValue();
    valid = validateFloat(value, "Max. Probability", minProb, 1.0);
    if(!valid)	return false;
    
    return true;
}
function validateInt(value, fieldName, min, max) {
	var intVal = parseInt(value);
	var valid = true;
	if(isNaN(intVal))						valid = false;
	if(valid && intVal < min)				valid = false;
	if(max && (valid && intVal > max))		valid = false;
	
	if(!valid) {
		if(max) alert("Value for "+fieldName+" should be between "+min+" and "+max);
		else	alert("Value for "+fieldName+" should be >= "+min);
	}
	return valid;
}
function validateFloat(value, fieldName, min, max) {
	var floatVal = parseFloat(value);
	var valid = true;
	if(isNaN(floatVal))						valid = false;
	if(valid && floatVal < min)			valid = false;
	if(max && (valid && floatVal > max))	valid = false;
	if(!valid) {
		if(max) alert("Value for "+fieldName+" should be between "+min+" and "+max);
		else	alert("Value for "+fieldName+" should be >= "+min);
	}
	return valid;
}
function updateResults(responseText, statusText) {
	$.unblockUI();
  	refreshProteinList(responseText);
}

function refreshProteinList(responseText) {
	if(responseText != "STALE_ID") {
		setupProteinListTable();
	}
	else {
		alert("Got stale Protein Inference ID. Please refresh the page.");
	}
	//setupProteinListTable();
}
// ---------------------------------------------------------------------------------------
// SORT RESULTS
// ---------------------------------------------------------------------------------------
function sortResults(pinferId, sortBy, sortOrder) {
  
  //alert(sortBy);
  
  //var useMods = $("input[@name='peptideDef_useMods']:checked").val() == null ? false : true;
  //var useCharge = $("input[@name='peptideDef_useCharge']:checked").val() == null ? false : true;
  //var groupProteins = $("input[@name='joinGroupProteins']:checked").val();
  
	var sortOrderStr  = sortOrder == 1 ? 'ASC' : 'DESC';
	// get data from the server and put it in the appropriate div
	$.blockUI();
	$("#proteinListTable").load("<yrcwww:link path='sortProteinProphetResult.do'/>",   			// url
							{'inferId': 		pinferId, 
							 'sortBy': 			sortBy,
							 'sortOrder': 		sortOrderStr}, 	            // data
							function(responseText, status, xhr) {			// callback
										$.unblockUI();
										refreshProteinList(responseText);
								   });	
	
	return false;
}

// ---------------------------------------------------------------------------------------
// PAGE RESULTS
// ---------------------------------------------------------------------------------------
function pageResults(pageNum) {
  
  	// get data from the server and put it in the appropriate div
  	$.blockUI();
  	$("#proteinListTable").load("<yrcwww:link path='pageProteinProphetResult.do'/>",   			// url
  							{'inferId': 		<%=pinferId%>, 
  							 'pageNum': 		pageNum}, 	            	// data
  							function(responseText, status, xhr) {			// callback
  										$.unblockUI();
  										refreshProteinList(responseText);
  								   });	
  	
  	return false;
}

// ---------------------------------------------------------------------------------------
// MAKE A TABLE SORTABLE
// ---------------------------------------------------------------------------------------
function makeSortable(table) {
  	
	var $table = table;
	$('th', $table).each(function(column) {
  		
  		if ($(this).is('.sort-alpha') || $(this).is('.sort-int') 
  			|| $(this).is('.sort-int-special') || $(this).is('.sort-float') ) {
  		
  			var $header = $(this);
      		$(this).addClass('clickable').hover(
      			function() {$(this).addClass('pinfer_small_hover');} , 
      			function() {$(this).removeClass('pinfer_small_hover');}).click(function() {

				
				// remove row striping
				if($table.is('.stripe_table')) {
					$("tbody > tr:odd", $table).removeClass("tr_odd");
					$("tbody > tr:even", $table).removeClass("tr_even");
				}
				
				// sorting direction
				var newDirection = 1;
        		if ($(this).is('.sorted-asc')) {
          			newDirection = -1;
        		}
        				
        		var rows = $table.find('tbody > tr').get();
        				
        		if ($header.is('.sort-alpha')) {
        			$.each(rows, function(index, row) {
						row.sortKey = $(row).children('td').eq(column).text().toUpperCase();
					});
				}
				
				if ($header.is('.sort-int')) {
        					$.each(rows, function(index, row) {
								var key = parseInt($(row).children('td').eq(column).text());
						row.sortKey = isNaN(key) ? 0 : key;
					});
				}
				
				if ($header.is('.sort-int-special')) {
        					$.each(rows, function(index, row) {
								var key = parseInt($(row).children('td').eq(column).text().replace(/\(\d*\)/, ''));
						row.sortKey = isNaN(key) ? 0 : key;
					});
				}
				
				if ($header.is('.sort-float')) {
        					$.each(rows, function(index, row) {
								var key = parseFloat($(row).children('td').eq(column).text());
						row.sortKey = isNaN(key) ? 0 : key;
					});
				}

     			rows.sort(function(a, b) {
       				if (a.sortKey < b.sortKey) return -newDirection;
					if (a.sortKey > b.sortKey) return newDirection;
					return 0;
     			});

     			$.each(rows, function(index, row) {
       				$table.children('tbody').append(row);
       				row.sortKey = null;
     			});
     			
     			// the header for the column used for sorting is highlighted
				$('th', $table).each(function(){
					$(this).removeClass('pinfer_selected_header_small');
					$(this).removeClass('sorted-desc');
	    			$(this).removeClass('sorted-asc');
				});
				$header.addClass('pinfer_selected_header_small');
				
     			var $sortHead = $table.find('th').filter(':nth-child(' + (column + 1) + ')');

	          	if (newDirection == 1) {$sortHead.addClass('sorted-asc'); $sortHead.removeClass('sorted-desc');} 
	          	else {$sortHead.addClass('sorted-desc'); $sortHead.removeClass('sorted-asc');}
        
        		
        		// add row striping back
        		if($table.is('.stripe_table')) {
					$("tbody > tr:odd", $table).addClass("tr_odd");
					$("tbody > tr:even", $table).addClass("tr_even");
        		}
      		});
	}
  });
}


function toggleDivVisibility(mydiv) {
  	if($(mydiv).is(':visible'))
  		$(mydiv).hide();
  	else
  		$(mydiv).show();
}



</script>



<CENTER>

<yrcwww:contentbox title="ProteinProphet Results" centered="true" width="90" widthRel="true" scheme="pinfer" >
  
  <div id="results" class="flora">
      <ul>
          <li><a href="#protlist" rel="history" id="protlistlink"><span>Protein List</span></a></li>
          <li><a href="#protdetails" rel="history" id="protdetailslink"><span>Protein Details</span></a></li>
          <li><a href="#roc" rel="history" id="roclink"><span>Sensitivity / Error</span></a></li>
      </ul>
   </div>
      
      
    <!-- Protein Annotation Dialog -->
	<div id="prot_annot_dialog" class="flora" title="Validate Protein">
		<input type="hidden" id="prot_id" value="" />
		Protein: <b><span id="prot_name"></span></b><br>
		<input type="radio" name="annotate" value="Accept" id="prot_accept" checked="checked"/>
		Accept	
		<input type="radio" name="annotate" value="Reject" id="prot_reject"/>
		Reject
		<input type="radio" name="annotate" value="Not Sure" id="prot_notsure" />
		Not Sure
		<br>
		<textarea name="comments" rows="4" cols="45" id="prot_comments"></textarea>
	</div>
      
    <!-- PROTEIN LIST -->
	<div id="protlist">
		<CENTER>
		<table><tr><td>
		
		<!-- SUMMARY -->
		<div style="padding:0 7 0 7; margin-bottom:5; border: 1px dashed gray;background-color: #FFFFF0;">
		<table align="center">
			<tr>
				<td>
					<b>Protein Inference ID:</b>
				</td>
				<td>
					<bean:write name="proteinProphetRun" property="id"/> &nbsp; (Program Version: <b> <bean:write name="proteinProphetRun" property="programVersion"/> </b>)
				</td>
			</tr>
			<tr>
				<td>
					<b>Date:</b>
				</td>
				<td>
					<bean:write name="proteinProphetRun" property="date"/>&nbsp;
				</td>
			</tr>
			<tr>
				<td><b>Comments </b>
					<logic:equal name="writeAccess" value="true">
					<span class="editableComment clickable" id="piRun_<bean:write name='proteinProphetRun' property='id'/>" style="font-size:8pt; color:red;">[Edit]</span>
					</logic:equal>
				</td>
				<td>
					<span id="piRun_<bean:write name='proteinProphetRun' property='id'/>_text"><bean:write name="proteinProphetRun" property="comments"/></span>
				</td>
			</tr>
			<tr>
				<td colspan="2" valign="top">
				<div id="piRun_<bean:write name='proteinProphetRun' property='id'/>_edit" align="center"
			     style="display:none;">
			     <textarea rows="5" cols="60" class="edit_text"></textarea>
			     <br>
			     <button class="savePiRunComments" id="<bean:write name='proteinProphetRun' property='id'/>">Save</button>
			     <button class="cancelPiRunComments" id="<bean:write name='proteinProphetRun' property='id'/>">Cancel</button>
				</div>
				</td>
			</tr>
		</table>
		</div>
	
		<%@ include file="proteinProphetFilterForm.jsp" %>
		</td></tr></table>
		</CENTER>
		
		<div id="protlist_table">
    	<%@ include file="proteinlist.jsp" %>
    	</div>
    	
    </div>
    
    
    
      
      <!-- PROTEIN DETAILS -->
      <div id="protdetails">
      		<!-- create a placeholder div for protein details -->
      		<div id="protein_div" style="display: none;" class="protdetail_prot"></div>
      </div>
      
 	<!-- SENSITIVITY / ERROR INFORMATION -->
 	<div id="roc">
    	<%@ include file="rocSummary.jsp" %>
    </div>
    	
    	
</yrcwww:contentbox>
</CENTER>

<%@ include file="/includes/footer.jsp" %>