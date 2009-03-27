/**
 * MsExperimentUploader.java
 * @author Vagisha Sharma
 * Mar 25, 2009
 * @version 1.0
 */
package org.yeastrc.ms.service;

import java.util.Date;

import org.apache.log4j.Logger;
import org.yeastrc.ms.dao.DAOFactory;
import org.yeastrc.ms.dao.general.MsExperimentDAO;
import org.yeastrc.ms.domain.general.impl.ExperimentBean;
import org.yeastrc.ms.service.UploadException.ERROR_CODE;

/**
 * 
 */
public class MsExperimentUploader implements UploadService {

    private static final Logger log = Logger.getLogger(MsExperimentUploader.class);
    
    private String remoteServer;
    private String remoteDirectory;
    private String uploadDirectory;
    
    private RawDataUploadService rdus;
    private SearchDataUploadService sdus;
    private AnalysisDataUploadService adus;
    
    private boolean do_rdupload = true;
    private boolean do_sdupload = false;
    private boolean do_adupload = false;
    
    
    private StringBuilder preUploadCheckMsg;
    
    private int searchId = 0; // uploaded searchId
    private int experimentId = 0; // uploaded experimentId
    
    public MsExperimentUploader () {
        this.preUploadCheckMsg = new StringBuilder();
    }
    
    public void setRemoteServer(String remoteServer) {
        this.remoteServer = remoteServer;
    }

    public void setRemoteDirectory(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }

    public void setRawDataUploader(RawDataUploadService rdus) {
        this.rdus = rdus;
    }

    public void setSearchDataUploader(SearchDataUploadService sdus) {
        this.sdus = sdus;
        this.do_sdupload = true;
    }

    public void setAnalysisDataUploader(AnalysisDataUploadService adus) {
        this.adus = adus;
        this.do_adupload = true;
    }

    @Override
    public void setDirectory(String directory) {
        this.uploadDirectory = directory;
    }
    
    @Override
    public boolean preUploadCheckPassed() {
        boolean passed = true;
        
        log.info("Doing pre-upload check for raw data uploader....");
        // Raw data uploader check
        if(rdus == null) {
            appendToMsg("RawDataUploader was null");
            passed = false;
        }
        if(!passed) log.info("...FAILED");
        else        log.info("...PASSED");
        
        
        
        // Search data uploader check
        if(do_sdupload) {
            log.info("Doing pre-upload check for search data uploader....");
            if(sdus == null) {
                appendToMsg("SearchDataUploader was null");
                passed = false;
            }
            else {
                if(!sdus.preUploadCheckPassed()) {
                    appendToMsg(sdus.getPreUploadCheckMsg());
                    passed = false;
                }
            }
            if(!passed) log.info("...FAILED");
            else        log.info("...PASSED");
        }
        
        // Analysis data uploader check
        if(do_adupload) {
            log.info("Doing pre-upload check for analysis data uploader....");
            if(!do_sdupload) {
                appendToMsg("No search results uploader found. Cannot upload analysis results without uploading search results first.");
                passed = false;
            }
            if(adus == null) {
                appendToMsg("AnalysisDataUploader was null");
                passed = false;
            }
            else {
                if(!adus.preUploadCheckPassed()) {
                    appendToMsg(adus.getPreUploadCheckMsg());
                    passed = false;
                }
            }
            if(!passed) log.info("...FAILED");
            else        log.info("...PASSED");
        }
        return passed;
    }
    
    @Override
    public String getPreUploadCheckMsg() {
        return preUploadCheckMsg.toString();
    }
    
    
    public String getUploadSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("\n\tExperiment ID: "+experimentId+"\n");
        summary.append("\tRemote server: "+remoteServer+"\n");
        summary.append("\tRemote directory: "+remoteDirectory+"\n");
        summary.append(rdus.getUploadSummary()+"\n");
        summary.append(sdus.getUploadSummary()+"\n");
        summary.append(adus.getUploadSummary()+"\n");
        summary.append("\n");
        return summary.toString();
    }
    
    private void appendToMsg(String msg) {
        preUploadCheckMsg.append(msg+"\n");
    }
    
    @Override
    public int upload() throws UploadException {
       
        logBeginExperimentUpload();
        long start = System.currentTimeMillis();
        
        // first create an entry in the msExperiment table
        experimentId = saveExperiment();
        log.info("\n\nAdded entry for experiment ID: "+experimentId+"\n\n");
        
        
        // upload raw data
        rdus.setExperimentId(experimentId);
        try {
            rdus.upload();
        }
        catch(UploadException ex) {
            deleteExperiment(experimentId);
            throw ex;
        }
        
        
        // if we have search data upload that next
        if(do_sdupload) {
            sdus.setExperimentId(experimentId);
            searchId = sdus.upload();
        }
        
        // if we have post-search analysis data upload that next
        int searchAnalysisId = 0;
        if(do_adupload) {
            adus.setSearchId(searchId);
            searchAnalysisId = adus.upload();
        }
        
        long end = System.currentTimeMillis();
        logEndExperimentUpload(start, end);
        
        return experimentId;
    }
    
    private int saveExperiment() throws UploadException {
        MsExperimentDAO experimentDao = DAOFactory.instance().getMsExperimentDAO();
        ExperimentBean experiment = new ExperimentBean();
        experiment.setServerAddress(remoteServer);
        experiment.setServerDirectory(remoteDirectory);
        experiment.setUploadDate(new java.sql.Date(new Date().getTime()));
        try { return experimentDao.saveExperiment(experiment);}
        catch(RuntimeException e) {
            UploadException ex = new UploadException(ERROR_CODE.CREATE_EXPT_ERROR);
            ex.appendErrorMessage("!!!\n\tERROR CREATING EXPERIMENT. EXPERIMENT WILL NOT BE UPLOADED\n!!!");
            ex.setErrorMessage(e.getMessage());
            throw ex;
        }
    }
    
    private void deleteExperiment(int experimentId) {
        log.error("\n\tDELETING EXPERIMENT: "+experimentId);
        MsExperimentDAO exptDao = DAOFactory.instance().getMsExperimentDAO();
        exptDao.deleteExperiment(experimentId);
    }
    
    private void logEndExperimentUpload(long start, long end) {
        log.info("END EXPERIMENT UPLOAD: "+((end - start)/(1000L))+"seconds"+
                "\n\tTime: "+(new Date().toString())+"\n"+
                getUploadSummary());
    }

    private void logBeginExperimentUpload() {
        log.info("BEGIN EXPERIMENT UPLOAD"+
                "\n\tRemote server: "+remoteServer+
                "\n\tRemote directory: "+remoteDirectory+
                "\n\tDirectory: "+uploadDirectory+
                "\n\tTime: "+(new Date().toString())+
                "\n\tRAW DATA UPLOAD: "+do_rdupload+
                "\n\tSEARCH DATA UPLOAD: "+do_sdupload+
                "\n\tANALYSIS DATA UPLOAD: "+do_adupload);
                
    }
}