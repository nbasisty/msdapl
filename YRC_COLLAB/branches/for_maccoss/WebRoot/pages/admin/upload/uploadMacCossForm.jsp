<%@ taglib uri="/WEB-INF/yrc-www.tld" prefix="yrcwww" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@ include file="/includes/header.jsp" %>

<%@ include file="/includes/errors.jsp" %>

<script language="javascript">

// keep track of the form field we're finding a project for
var currentProjectField;

function projectSearcherPopUp(field) {
	currentProjectField = field;
	var doc = "<yrcwww:link path='pages/admin/upload/uploadProjectSearch.jsp'/>";

	var winHeight = 500
	var winWidth = 600;

	window.open(doc, "PROJECT_SELECTION_WINDOW", "width=" + winWidth + ",height=" + winHeight + ",status=no,resizable=yes,scrollbars=yes");
}

</script>


<logic:equal name="queued" value="true" scope="request">
 <center>
 <hr width="50%">
  <B><font color="red">The MS/MS data upload request has been successfully added to the job queue.<BR>
  You will be notified by email when it is completed.</font></B>
 <hr width="50%">
 </center>
</logic:equal>

<yrcwww:contentbox title="Upload Data" centered="true" width="700" scheme="upload">

<html:form action="uploadData" method="POST">
 <html:hidden property="group" value="MacCoss"/>

 <CENTER>
  <table border="0">
   <tr>
    <td colspan="2">Select the project to which this data belongs:</td>
   </tr>
			<tr>
				<td valign="top">
					<span><b>Project:</b></span><br>
					<span style="font-size:8pt;color:red;">IMPORTANT: This <span style="text-decoration:underline;">must</span> be the project<br>
							       to which this data belongs.</span>
				</td>
				<td valign="top">
					<html:select property="projectID">
						<html:option value="0">None</html:option>
						<html:options collection="researcherProjects" property="id" labelProperty="title"/>
   					</html:select>
					<!--<html:text property="projectID" size="10" maxlength="10" /><br>-->
					<!--  <a href="javascript:projectSearcherPopUp(document.uploadDataForm.projectID)" style="text-decoration:none;"><span style="font-size:8pt;color:red;text-decoration:none;">[SEARCH PROJECTS]</span></a> -->
				</td>
			</tr>

   <tr>
    <td valign="top"><b>Data directory:</b><br>
    <font style="font-size:8pt;">e.g.: /home/maccoss/Davis/121005-digest</font></td>
    <td valign="top"><html:text property="directory" size="40" maxlength="255"/></td>
   </tr>

   <tr>
    <td><b>Experiment date:</b></td>
    <td>
     <html:select property="year">
      <html:option value="0">Year</html:option>
      <html:option value="2003">2003</html:option>
      <html:option value="2004">2004</html:option>
      <html:option value="2005">2005</html:option>
      <html:option value="2006">2006</html:option>
      <html:option value="2007">2007</html:option>
      <html:option value="2008">2008</html:option>
      <html:option value="2009">2009</html:option>
      <html:option value="2010">2010</html:option>
      <html:option value="2010">2011</html:option>
     </html:select>
     
     <b> - </b>
      
     <html:select property="month">
      <html:option value="0">Month</html:option>
      <html:option value="01">01</html:option>
      <html:option value="02">02</html:option>
      <html:option value="03">03</html:option>
      <html:option value="04">04</html:option>
      <html:option value="05">05</html:option>
      <html:option value="06">06</html:option>
      <html:option value="07">07</html:option>
      <html:option value="08">08</html:option>
      <html:option value="09">09</html:option>
      <html:option value="10">10</html:option>
      <html:option value="11">11</html:option>
      <html:option value="12">12</html:option>
     </html:select>
     
     <b> - </b>
     
     <html:select property="day">
      <html:option value="0">Day</html:option>
      <html:option value="01">01</html:option>
      <html:option value="02">02</html:option>
      <html:option value="03">03</html:option>
      <html:option value="04">04</html:option>
      <html:option value="05">05</html:option>
      <html:option value="06">06</html:option>
      <html:option value="07">07</html:option>
      <html:option value="08">08</html:option>
      <html:option value="09">09</html:option>
      <html:option value="10">10</html:option>
      <html:option value="11">11</html:option>
      <html:option value="12">12</html:option>
      <html:option value="13">13</html:option>
      <html:option value="14">14</html:option>
      <html:option value="15">15</html:option>
      <html:option value="16">16</html:option>
      <html:option value="17">17</html:option>
      <html:option value="18">18</html:option>
      <html:option value="19">19</html:option>
      <html:option value="20">20</html:option>
      <html:option value="21">21</html:option>
      <html:option value="22">22</html:option>
      <html:option value="23">23</html:option>
      <html:option value="24">24</html:option>
      <html:option value="25">25</html:option>
      <html:option value="26">26</html:option>
      <html:option value="27">27</html:option>
      <html:option value="28">28</html:option>
      <html:option value="29">29</html:option>
      <html:option value="30">30</html:option>
      <html:option value="31">31</html:option>
     </html:select>
     
    </td>
   </tr>

   <tr>
    <td valign="top"><br><b>Species:</b><br>
     <font style="font-size:8pt;">(Species of sample)</font></td>
    <td valign="top"><br>
     <html:select property="species">
      <html:option value="9913">B. taurus (cow)</html:option>
      <html:option value="6239">C. elegans</html:option>
      <html:option value="7227">D. melanogaster (fruit fly)</html:option>
      <html:option value="9031">G. gallus (chicken)</html:option>
      <html:option value="9606">H. sapiens</html:option>
      <html:option value="10090">M. musculus (mouse)</html:option>
      <html:option value="10116">R. norvegicus (rat)</html:option>
      <html:option value="4932">S. cerevisiae (budding yeast)</html:option>
      <html:option value="4896">S. pombe (fission yeast)</html:option>
      <html:option value="0">Not Specified/Not Applicable</html:option>
     </html:select>
    </td>
   </tr>
   
   <tr>
    <td valign="top"><b>Comments:</b></td>
    <td><html:textarea property="comments" cols="40" rows="5"/></td>
   </tr>

  </table> 
 
 <P>Depending on the size of the data, <u>the upload process may take several minutes</u>.

 <p><html:submit value="Upload Data"/>
 </CENTER>

</html:form>

</yrcwww:contentbox>

<%@ include file="/includes/footer.jsp" %>