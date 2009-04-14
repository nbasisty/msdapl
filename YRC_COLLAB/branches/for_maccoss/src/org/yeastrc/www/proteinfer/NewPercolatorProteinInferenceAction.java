/**
 * NewPercolatorProteinInference.java
 * @author Vagisha Sharma
 * Apr 8, 2009
 * @version 1.0
 */
package org.yeastrc.www.proteinfer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.yeastrc.ms.dao.DAOFactory;
import org.yeastrc.ms.domain.analysis.MsSearchAnalysis;
import org.yeastrc.ms.domain.search.Program;
import org.yeastrc.project.Projects;
import org.yeastrc.www.user.Groups;
import org.yeastrc.www.user.User;
import org.yeastrc.www.user.UserUtils;

import edu.uwpr.protinfer.ProteinInferenceProgram;
import edu.uwpr.protinfer.database.dto.ProteinferInput.InputType;

/**
 * 
 */
public class NewPercolatorProteinInferenceAction extends Action {

    public ActionForward execute( ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response )
    throws Exception {
        
        // User making this request
        User user = UserUtils.getUser(request);
        if (user == null) {
            ActionErrors errors = new ActionErrors();
            errors.add("username", new ActionMessage("error.login.notloggedin"));
            saveErrors( request, errors );
            return mapping.findForward("authenticate");
        }

        // Restrict access to members
        Groups groupMan = Groups.getInstance();
        if (!groupMan.isMember(user.getResearcher().getID(), Projects.MACCOSS) &&
          !groupMan.isMember( user.getResearcher().getID(), Projects.YATES) &&
          !groupMan.isMember(user.getResearcher().getID(), "administrators")) {
            ActionErrors errors = new ActionErrors();
            errors.add("access", new ActionMessage("error.access.invalidgroup"));
            saveErrors( request, errors );
            return mapping.findForward( "Failure" );
        }
        
        int searchAnalysisId = -1;
        if (request.getParameter("searchAnalysisId") != null) {
            try {searchAnalysisId = Integer.parseInt(request.getParameter("searchAnalysisId"));}
            catch(NumberFormatException e) {searchAnalysisId = -1;}
        }
        
        if (searchAnalysisId == -1) {
            ActionErrors errors = new ActionErrors();
            errors.add("proteinfer", new ActionMessage("error.proteinfer.invalid.analysisId", searchAnalysisId, ""));
            saveErrors( request, errors );
            return mapping.findForward("Failure");
        }
        // make sure a search analysis with the given Id exists AND it is a Percolator analysis
        MsSearchAnalysis searchAnalysis = DAOFactory.instance().getMsSearchAnalysisDAO().load(searchAnalysisId);
        if(searchAnalysis == null) {
            ActionErrors errors = new ActionErrors();
            errors.add("proteinfer", new ActionMessage("error.proteinfer.invalid.analysisId", searchAnalysisId, ""));
            saveErrors( request, errors );
            return mapping.findForward("Failure");
        }
        // make sure this is a Percolator analyssi
        if(searchAnalysis.getAnalysisProgram() != Program.PERCOLATOR) {
            ActionErrors errors = new ActionErrors();
            errors.add("proteinfer", new ActionMessage("error.proteinfer.invalid.analysisId", 
                    searchAnalysisId, "Not a Percolator analysis."));
            saveErrors( request, errors );
        }
        
        
        // We need the projectID so we can redirect back to the project page after
        // the protein inference job has been submitted.
        int projectId = -1;
        if (request.getParameter("projectId") != null) {
            try {projectId = Integer.parseInt(request.getParameter("projectId"));}
            catch(NumberFormatException e) {projectId = -1;}
        }
        
        if (projectId == -1) {
            ActionErrors errors = new ActionErrors();
            errors.add("proteinfer", new ActionMessage("error.proteinfer.invalid.projectId", projectId));
            saveErrors( request, errors );
            return mapping.findForward("Failure");
        }
        request.setAttribute("projectId", projectId);
        
        
        // Create our ActionForm
        ProteinInferenceForm formForAnalysis = createFormForAnalysisInput(searchAnalysis, projectId);

        if(formForAnalysis != null)
            request.setAttribute("proteinInferenceFormAnalysis", formForAnalysis);
            
        
        
        // Go!
        return mapping.findForward("Success");

    }

    private ProteinInferenceForm createFormForAnalysisInput(MsSearchAnalysis analysis, int projectId) {
        
        ProteinInferInputGetter inputGetter = ProteinInferInputGetter.instance();

        ProteinInferenceForm formForAnalysis = new ProteinInferenceForm();
        formForAnalysis.setInputType(InputType.ANALYSIS);
        formForAnalysis.setProjectId(projectId);
        ProteinInferInputSummary inputSummary = inputGetter.getInputAnalysisSummary(analysis);
        formForAnalysis.setInputSummary(inputSummary);
        // set the Protein Inference parameters
        ProgramParameters params2 = new ProgramParameters(ProteinInferenceProgram.PROTINFER_PERC);
        formForAnalysis.setProgramParams(params2);
        return formForAnalysis;
    }
}