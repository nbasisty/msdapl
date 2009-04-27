<%@ taglib uri="/WEB-INF/yrc-www.tld" prefix="yrcwww" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>


  <html:form action="/downloadProtInferResults" method="post" styleId="downloadForm" target="_blank" >
  <html:hidden name="proteinInferFilterForm" property="pinferId" />
  <html:hidden name="proteinInferFilterForm" property="minPeptides" />
  <html:hidden name="proteinInferFilterForm" property="minUniquePeptides" />
  <html:hidden name="proteinInferFilterForm" property="minCoverage" />
  <html:hidden name="proteinInferFilterForm" property="minSpectrumMatches" />
  <html:hidden name="proteinInferFilterForm" property="showAllProteins" />
  <html:hidden name="proteinInferFilterForm" property="validationStatusString" /> 	
  <html:hidden name="proteinInferFilterForm" property="accessionLike" />
  <html:hidden name="proteinInferFilterForm" property="descriptionLike" />
  <div align="center">
   	<a href="" onclick="javascript:downloadResults();return false;" >Download Results</a>
  </div>
  </html:form>
  