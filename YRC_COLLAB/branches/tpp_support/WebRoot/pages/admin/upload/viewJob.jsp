<%@ taglib uri="/WEB-INF/yrc-www.tld" prefix="yrcwww" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@ include file="/includes/header.jsp" %>

<%@ include file="/includes/errors.jsp" %>

<logic:notPresent name="job" scope="request">
  <logic:forward name="viewUploadJob" />
</logic:notPresent>


<yrcwww:contentbox title="View Upload Job" centered="true" width="80" widthRel="true" scheme="upload">
	<center>
	
	<a href="<yrcwww:link path='listUploadJobs.do?status=pending'/>"><b>View Pending Jobs</b></a> ||
	<a href="<yrcwww:link path='listUploadJobs.do?status=complete'/>"><b>View Completed Jobs</b></a>
	<br><br><br>
	
	<table border="0" width="85%" align="center" class="table_basic">
	
		<thead>
		<tr>
			<th width="100%" colspan="2" align="center"><span style="margin-bottom:20px;font-size:10pt;font-weight:bold;">Job Data</span></th>
		</tr>
		</thead>
		
		<tbody>
		<tr>
			<td width="20%" align="left" valign="top" class="left_align">Submitted By:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<a href="<yrcwww:link path='viewResearcher.do?'/>id=<bean:write name="job" property="submitter" />">
					<bean:write name="job" property="researcher.firstName" /> <bean:write name="job" property="researcher.lastName" /></a>
			</td>
		</tr>	

		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Submitted On:</td>
			<td width="80%" align="left" valign="top" class="left_align"><bean:write name="job" property="submitDate" /></td>
		</tr>
	
		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Last Change:</td>
			<td width="80%" align="left" valign="top" class="left_align"><bean:write name="job" property="lastUpdate" /></td>
		</tr>

		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Status:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<logic:equal name="job" property="status" value="4">
					<bean:write name="job" property="statusDescription" /> <a href="<yrcwww:link path='viewProject.do?ID='/><bean:write name="job" property="projectID" />#Expt<bean:write name="job" property="experimentID" />"><span style="color:red;">[View Experiment]</span></a>
				</logic:equal>
				<logic:notEqual name="job" property="status" value="4"><!-- not completed -->
					<bean:write name="job" property="statusDescription" />
					
					<logic:notEqual name="job" property="status" value="1"><!-- not running -->
						[<a style="color:red;" href="<yrcwww:link path='deleteJob.do?'/>id=<bean:write name="job" property="id" scope="request"/>">Delete</a>]
						
					<logic:notEqual name="job" property="status" value="0"><!-- not waiting to run -->
					
					
						[<a style="color:red;" href="<yrcwww:link path='resetJob.do?'/>id=<bean:write name="job" property="id" scope="request"/>">Retry</a>]
						
					
					</logic:notEqual>
					</logic:notEqual>
					
				</logic:notEqual>
			</td>
		</tr>

		<logic:notEmpty name="job" property="log">
			<tr >
				<td width="100%" colspan="2" class="left_align">
					<div style="width:100%;height:auto;overflow:auto;">
						Log Text:<br><br>
						<pre style="font-size:8pt;"><bean:write name="job" property="log" /></pre>
					</div>
				</td>
			</tr>
		</logic:notEmpty>
		</tbody>
	</table>
	<br><br>
	<table border="0" width="85%" style="margin-top:10px;" align="center" class="table_basic">

		<thead>
		<tr >
			<th width="100%" colspan="2" align="center"><span style="margin-bottom:20px;font-size:10pt;font-weight:bold;">Experiment Details</span></th>
		</tr>
		</thead>
		
		<tbody>
		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Project:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<a href="<yrcwww:link path='viewProject.do?'/>ID=<bean:write name="job" property="projectID" />">
					<bean:write name="job" property="project.title" /></a>
			</td>
		</tr>

		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Directory:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<bean:write name="job" property="serverDirectory" />
			</td>
		</tr>
		
		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Pipeline:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<bean:write name="job" property="pipelineLongName" />
			</td>
		</tr>

		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Run Date:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<bean:write name="job" property="runDate" />
			</td>
		</tr>

		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Bait Desc:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<bean:write name="job" property="baitProteinDescription" />
			</td>
		</tr>

		<tr >
			<td width="20%" align="left" valign="top" class="left_align">Comments:</td>
			<td width="80%" align="left" valign="top" class="left_align">
				<bean:write name="job" property="comments" />
			</td>
		</tr>
		</tbody>
	
	</table>
	</center>
	<br><br>
</yrcwww:contentbox>

<%@ include file="/includes/footer.jsp" %>