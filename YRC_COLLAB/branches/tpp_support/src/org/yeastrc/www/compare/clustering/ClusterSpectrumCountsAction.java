/**
 * ClusterSpectrumCountsAction.java
 * @author Vagisha Sharma
 * Apr 18, 2010
 * @version 1.0
 */
package org.yeastrc.www.compare.clustering;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.yeastrc.ms.util.StringUtils;
import org.yeastrc.ms.util.TimeUtils;
import org.yeastrc.www.compare.ProteinComparisonDataset;
import org.yeastrc.www.compare.ProteinGroupComparisonDataset;
import org.yeastrc.www.compare.ProteinSetComparisonForm;
import org.yeastrc.www.compare.SpeciesChecker;
import org.yeastrc.www.compare.util.VennDiagramCreator;

/**
 * 
 */
public class ClusterSpectrumCountsAction extends Action {

	private static final Logger log = Logger.getLogger(ClusterSpectrumCountsAction.class.getName());
    
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        
        log.info("Clustering spectrum counts for comparison results");
        
        ProteinSetComparisonForm myForm = (ProteinSetComparisonForm) request.getAttribute("comparisonForm");
        if(myForm == null) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionMessage("error.general.errorMessage", "Comparison form not found in request"));
            saveErrors( request, errors );
            return mapping.findForward("Failure");
        }
        
        String jobToken = myForm.getClusteringToken();
        if(jobToken == null || jobToken.trim().length() == 0) {
        	ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionMessage("error.general.errorMessage", "Token for clustering not found in request"));
            saveErrors( request, errors );
            return mapping.findForward("Failure");
        }
        
        // now mark the token as old
		myForm.setNewToken(false);
        
        if(!myForm.getGroupIndistinguishableProteins()) {
            ProteinComparisonDataset comparison = (ProteinComparisonDataset) request.getAttribute("comparisonDataset");
            if(comparison == null) {
                ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR, new ActionMessage("error.general.errorMessage", "Comparison dataset not found in request"));
                saveErrors( request, errors );
                return mapping.findForward("Failure");
            }
            
            long s = System.currentTimeMillis();
            SpectrumCountClusterer.getInstance().clusterProteinComparisonDataset(comparison);
            long e = System.currentTimeMillis();
            log.info("Time to culster ProteinComparisonDataset: "+TimeUtils.timeElapsedSeconds(s, e)+" seconds");
            
        }
        else {
            ProteinGroupComparisonDataset grpComparison = (ProteinGroupComparisonDataset) request.getAttribute("comparisonGroupDataset");
            if(grpComparison == null) {
                ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR, new ActionMessage("error.general.errorMessage", "Comparison dataset not found in request"));
                saveErrors( request, errors );
                return mapping.findForward("Failure");
            }
            
            long s = System.currentTimeMillis();
            StringBuilder errorMessage = new StringBuilder();
            String baseDir = request.getSession().getServletContext().getRealPath(ClusteringConstants.BASE_DIR);
            baseDir = baseDir+File.separator+jobToken;
            
            ProteinGroupComparisonDataset clusteredGrpComparison = 
            	SpectrumCountClusterer.getInstance().clusterProteinGroupComparisonDataset(grpComparison, errorMessage, baseDir);
            if(clusteredGrpComparison == null) {
            	ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR, new ActionMessage("error.general.errorMessage", "Clustering error: "+errorMessage.toString()));
                saveErrors( request, errors );
                return mapping.findForward("Failure");
            }
            long e = System.currentTimeMillis();
            log.info("Time to culster ProteinGroupComparisonDataset: "+TimeUtils.timeElapsedSeconds(s, e)+" seconds");
            
            
            // Serialize the ProteinGroupComparisonDataset
            if(!serializeObject(clusteredGrpComparison, ClusteringConstants.PROT_GRP_SER,
            		errorMessage, baseDir)) {
            	ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR, new ActionMessage("error.general.errorMessage", errorMessage.toString()));
                saveErrors( request, errors );
                return mapping.findForward("Failure");
            }
            
            // Serialize the ProteinSetComparisonForm
            if(!serializeObject(myForm, ClusteringConstants.FORM_SER,
            		errorMessage, baseDir)) {
            	ActionErrors errors = new ActionErrors();
                errors.add(ActionErrors.GLOBAL_ERROR, new ActionMessage("error.general.errorMessage", errorMessage.toString()));
                saveErrors( request, errors );
                return mapping.findForward("Failure");
            }
            
            // R image output
            String imgUrl = request.getSession().getServletContext().getContextPath()+"/"+ClusteringConstants.BASE_DIR+"/"+jobToken+"/"+ClusteringConstants.IMG_FILE;
            request.setAttribute("clusteredImgUrl", imgUrl);
            
            // Create Venn Diagram only if 2 or 3 datasets are being compared
            if(clusteredGrpComparison.getDatasetCount() == 2 || clusteredGrpComparison.getDatasetCount() == 3) {
                String googleChartUrl = VennDiagramCreator.instance().getChartUrl(clusteredGrpComparison);
                request.setAttribute("chart", googleChartUrl);
            }
            
            // create a list of the dataset ids being compared
            // Get the selected protein inference run ids
            List<Integer> allRunIds = myForm.getAllSelectedRunIds();
            request.setAttribute("datasetIds", StringUtils.makeCommaSeparated(allRunIds));
            
            request.setAttribute("comparison", clusteredGrpComparison);
            request.setAttribute("speciesIsYeast", SpeciesChecker.isSpeciesYeast(clusteredGrpComparison.getDatasets()));
            return mapping.findForward("ProteinGroupList");
        }
        
        return mapping.findForward("Failure");
    }

	private boolean serializeObject(
			Object object, String outFile,
			StringBuilder errorMessage, String dir) {
		
		String file = dir+File.separator+outFile;
		ObjectOutputStream oo = null;
		try {
			oo = new ObjectOutputStream(new FileOutputStream(file));
			oo.writeObject(object);
		}
		catch (IOException e) {
			errorMessage.append("Error writing file: "+outFile+" "+e.getMessage());
			return false;
		}
		finally {
			if(oo != null) try {oo.close();} catch(IOException e){}
		}
		return true;
	}
	
}