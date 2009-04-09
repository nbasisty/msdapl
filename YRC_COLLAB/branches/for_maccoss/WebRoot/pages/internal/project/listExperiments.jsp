
<%@ taglib uri="/WEB-INF/yrc-www.tld" prefix="yrcwww" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<script src="<yrcwww:link path='js/jquery.ui-1.6rc2/jquery-1.2.6.js'/>"></script>
<script>

$(document).ready(function() {
   $(".search_files").each(function() {
   		var $table = $(this);
   		$table.attr('width', "100%");
   		$('th', $table).attr("align", "left");
   		//$("tbody > tr:even", $table).css("background-color", "#FFFFFF");
   		$("tbody > tr:even", $table).addClass("project_A");
   });
});

</script>


<yrcwww:contentbox title="Experiments" centered="true" width="850">

	<logic:empty name="experiments">
		<div align="center" style="margin:20">
		There are no experiments for this project. To upload an experiment for this project click <a href="" onClick="javascript:goMacCoss(); return false;">here</a>
		</div>
	</logic:empty>

	<logic:notEmpty name="experiments">
		
		
		<logic:iterate name="experiments" id="experiment" scope="request">
		
		
			<div style="border:1px dotted gray;margin:5 5 5 5; padding:0 0 5 0;">
			<div style="background-color:#ED9A2E;width:100%; margin:0; padding:3 0 3 0; color:white;">
				<span style="padding-left:10;"><b>Experiment ID: <bean:write name="experiment" property="id"/></b></span>
			</div>
			
			
			<div style="padding:0; margin:0;"> 
			<div style="margin:0; padding:5;">
			<table cellspacing="0" cellpadding="0">		
				<tr>	
					<td><b>Date Uploaded: </b></td><td style="padding-left:10"><bean:write name="experiment" property="uploadDate"/></td>
				</tr>
				<tr>
					<td><b>Location: </b></td>
					<td style="padding-left:10"><bean:write name="experiment" property="serverDirectory"/></td>
				</tr>
				<tr>
					<td><b>Comments: </b></td>
					<td style="padding-left:10"><bean:write name="experiment" property="comments"/></td>
				</tr>
			</table>
			</div>
			
			<!-- SEARCHES FOR THE EXPERIMENT -->
			<logic:notEmpty name="experiment" property="searches">
				<logic:iterate name="experiment" property="searches" id="search">
					<div style="background-color: #FFFFE0; margin:5 5 5 5; padding:5; border: 1px dashed gray;" >
					<table width="90%">
						<tr>
							<td width="33%"><b>Search ID:</b>&nbsp; 
							<bean:write name="search" property="id"/>
								<html:link action="viewSequestResults.do" paramId="ID" paramName="search" paramProperty="id">[Results]</html:link>
							</td>
							<td width="33%"><b>Program: </b>&nbsp;
							<b><bean:write name="search" property="searchProgram"/>
							&nbsp;
							<bean:write name="search" property="searchProgramVersion"/></b></td>
							<td width="33%"><b>Search Date: </b>&nbsp;
							<bean:write name="search" property="searchDate"/></td>
						</tr>
						<tr>
							<td width="33%"><b>Enzyme: </b>&nbsp;
							<bean:write name="search" property="enzymes"/></td>
							<td width="33%"><b>Static Modifications: </b>
							<bean:write name="search" property="staticModifications"/></td>
							<td width="33%"><b>Dynamic Modifications: </b>
							<bean:write name="search" property="dynamicModifications"/></td>
						</tr>
						<tr>
							<td colspan="3"><b>Search Database: </b>&nbsp;
							<bean:write name="search" property="searchDatabase"/></td>
							
						</tr>
					</table>
					</div>	
				</logic:iterate>
			</logic:notEmpty>
			
			<!-- SEARCH ANALYSES FOR THE EXPERIMENT -->
			<logic:notEmpty name="experiment" property="analyses">
				<logic:iterate name="experiment" property="analyses" id="analysis">
				<div style="background-color: #F0FFF0; margin:5 5 5 5; padding:5; border: 1px dashed gray;" >
					<table width="90%">
					<tr>
						<td width="25%"><b>Analysis ID:</b>&nbsp;
						<bean:write name="analysis" property="id"/>
							<html:link action="viewPercolatorResults.do" paramId="ID" paramName="analysis" paramProperty="id">[Results]</html:link>
							<a href="<yrcwww:link path='newPercolatorProteinInference.do?'/>searchAnalysisId=<bean:write name='analysis' property='id' />&projectId=<bean:write name='project' property='ID'/>"> 
							[Infer Proteins]</a>
						</td>
						<td width="25%"><b>Program: </b>&nbsp;
						<b><bean:write name="analysis" property="analysisProgram"/>
						&nbsp;
						<bean:write name="analysis" property="analysisProgramVersion"/></b></td>
					</tr>
					
					</table>
				</div>
				</logic:iterate>
			</logic:notEmpty>
			
			<!-- PROTEIN INFERENCE RESULTS FOR THE EXPERIMENT -->
			<logic:equal name="experiment" property="hasProtInferResults" value="true" >
			<logic:present name="experiment" property="dtaSelect">
				<div style="background-color: #F5FFFA; margin:5 5 5 5; padding:5; border: 1px dashed gray;" > 
					<b>DTASelect ID:</b> <bean:write name="experiment" property="dtaSelect.id"/>&nbsp;
					<html:link action="viewYatesRun.do" paramId="id" paramName="experiment" paramProperty="dtaSelect.id">[Results]</html:link>
				</div>
			</logic:present>
			<logic:present name="experiment" property="protInferRuns">
				<div style="background-color: #F5FFFA; margin:5 5 5 5; padding:5; border: 1px dashed gray;" >
					<div><b>Protein Inference Results</b></div> 
					<table width="90%">
					<thead>
					<tr align="left"><th>ID</th><th>Date</th><th>Submitted By</th><th>Comments</th><th>Status</th></tr>
					</thead>
					<tbody>
					<logic:iterate name="experiment" property="protInferRuns" id="piJob" type="org.yeastrc.www.proteinfer.ProteinferJob">
						<tr>
						<td><b><bean:write name="piJob" property="pinferId"/></b></td>
						<td><bean:write name="piJob" property="submitDate"/></td>
						<td><bean:write name="piJob" property="researcher.lastName"/></td>
						<td><bean:write name="piJob" property="comments"/></td>
						
						<td>
						
						<!-- Job COMPLETE -->
						<logic:equal name="piJob" property="complete" value="true">
							<a href="<yrcwww:link path='viewProteinInferenceResult.do?'/>pinferId=<bean:write name='piJob' property='pinferId'/>">
							<b><font color="green"><bean:write name="piJob" property="statusDescription"/></font></b>
							</a>
						</logic:equal>
						<!-- Job FAILED -->
						<logic:equal name="piJob" property="failed" value="true">
							<a href="<yrcwww:link path='viewProteinInferenceJob.do?'/>pinferId=<bean:write name='piJob' property='pinferId'/>&projectId=<bean:write name='project' property='ID'/>">
							<b><font color="red"><bean:write name="piJob" property="statusDescription"/></font></b>
							</a>
						</logic:equal>
						<!-- Job RUNNING -->
						<logic:equal name="piJob" property="running" value="true">
							<a href="<yrcwww:link path='viewProteinInferenceJob.do?'/>pinferId=<bean:write name='piJob' property='pinferId'/>&projectId=<bean:write name='project' property='ID'/>">
							<b><font color="#000000"><bean:write name="piJob" property="statusDescription"/></font></b>
							</a>
						</logic:equal>
						
	   		 			</td>
						</tr>
					</logic:iterate>
					</tbody>
					</table>
				</div>
			</logic:present>
			</logic:equal>
			
			
			<!-- FILES FOR THE EXPERIMENT -->
			<div style="background-color: #FFFAF0; margin:5 5 5 5; padding:5; border: 1px dashed gray;" > 
			<bean:define name="experiment" property="id" id="experimentId" />
			<yrcwww:table name="experiment" tableId='<%="search_files_"+experimentId %>' tableClass="search_files" center="true" />
			</div>
			</div>
		</div> <!-- End of one experiment -->
		</logic:iterate>
	</logic:notEmpty>

</yrcwww:contentbox>