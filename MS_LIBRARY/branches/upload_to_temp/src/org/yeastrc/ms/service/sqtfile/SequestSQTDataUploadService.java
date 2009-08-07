/**
 * SQTDataUploadService.java
 * @author Vagisha Sharma
 * Jul 15, 2008
 * @version 1.0
 */
package org.yeastrc.ms.service.sqtfile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.yeastrc.ms.domain.general.MsEnzymeIn;
import org.yeastrc.ms.domain.search.MsResidueModificationIn;
import org.yeastrc.ms.domain.search.MsSearchDatabaseIn;
import org.yeastrc.ms.domain.search.MsTerminalModificationIn;
import org.yeastrc.ms.domain.search.Program;
import org.yeastrc.ms.domain.search.SearchFileFormat;
import org.yeastrc.ms.domain.search.sequest.SequestParam;
import org.yeastrc.ms.domain.search.sequest.SequestResultData;
import org.yeastrc.ms.domain.search.sequest.SequestResultDataWId;
import org.yeastrc.ms.domain.search.sequest.SequestSearchIn;
import org.yeastrc.ms.domain.search.sequest.SequestSearchResultIn;
import org.yeastrc.ms.domain.search.sequest.SequestSearchScan;
import org.yeastrc.ms.parser.DataProviderException;
import org.yeastrc.ms.parser.sequestParams.SequestParamsParser;
import org.yeastrc.ms.parser.sqtFile.sequest.SequestSQTFileReader;
import org.yeastrc.ms.service.MsDataUploadProperties;
import org.yeastrc.ms.service.UploadException;
import org.yeastrc.ms.service.UploadException.ERROR_CODE;
import org.yeastrc.ms.upload.dao.UploadDAOFactory;
import org.yeastrc.ms.upload.dao.search.sequest.SequestSearchResultUploadDAO;
import org.yeastrc.ms.upload.dao.search.sequest.SequestSearchUploadDAO;
import org.yeastrc.ms.util.FileUtils;

/**
 * 
 */
public final class SequestSQTDataUploadService extends AbstractSQTDataUploadService {

    
    private final SequestSearchResultUploadDAO sqtResultDao;
    
    // these are the things we will cache and do bulk-inserts
    List<SequestResultDataWId> sequestResultDataList; // sequest scores
    
    private MsSearchDatabaseIn db = null;
    private boolean usesEvalue = false;
    private List<MsResidueModificationIn> dynaResidueMods;
    private List<MsTerminalModificationIn> dynaTermMods;
    
    private final Program program;
    private final SearchFileFormat format;
    
    public SequestSQTDataUploadService(SearchFileFormat format) {
        super();
        this.format = format;
        program = Program.programForFileFormat(format);
        this.sequestResultDataList = new ArrayList<SequestResultDataWId>();
        this.dynaResidueMods = new ArrayList<MsResidueModificationIn>();
        this.dynaTermMods = new ArrayList<MsTerminalModificationIn>();
        
        UploadDAOFactory daoFactory = UploadDAOFactory.getInstance();
        
        this.sqtResultDao = daoFactory.getSequestResultDAO();
    }
    
    void reset() {
        super.reset();
        usesEvalue = false;
        dynaResidueMods.clear();
        dynaTermMods.clear();
        db = null;
    }
    // resetCaches() is called by reset() in the superclass.
    void resetCaches() {
        super.resetCaches();
        sequestResultDataList.clear();
    }
    
    MsSearchDatabaseIn getSearchDatabase() {
        return db;
    }

    Program getSearchProgram() {
//        return Program.SEQUEST;
        return program;
    }
    
    @Override
    int uploadSearchParameters(int experimentId, String paramFileDirectory, 
            String remoteServer, String remoteDirectory,
            Date searchDate) throws UploadException {
        
        SequestParamsParser parser = parseSequestParams(paramFileDirectory, remoteServer);
        
        usesEvalue = parser.reportEvalue();
        db = parser.getSearchDatabase();
        dynaResidueMods = parser.getDynamicResidueMods();
        dynaTermMods = parser.getDynamicTerminalMods();
        
        // get the id of the search database used (will be used to look up protein ids later)
        sequenceDatabaseId = getSearchDatabaseId(parser.getSearchDatabase());
        
        // create a new entry in the MsSearch table and upload the search options, databases, enzymes etc.
        try {
            SequestSearchUploadDAO searchDAO = UploadDAOFactory.getInstance().getSequestSearchDAO();
            return searchDAO.saveSearch(makeSearchObject(parser, getSearchProgram(),
                    remoteDirectory, searchDate), experimentId, sequenceDatabaseId);
        }
        catch(RuntimeException e) {
            UploadException ex = new UploadException(ERROR_CODE.RUNTIME_SQT_ERROR, e);
            ex.setErrorMessage(e.getMessage());
            throw ex;
        }
    }
    
    private SequestParamsParser parseSequestParams(String fileDirectory, final String remoteServer) throws UploadException {
        
        // parse the parameters file
        final SequestParamsParser parser = new SequestParamsParser();
        log.info("BEGIN Sequest search UPLOAD -- parsing parameters file: "+parser.paramsFileName());
        if (!(new File(fileDirectory+File.separator+parser.paramsFileName()).exists())) {
            UploadException ex = new UploadException(ERROR_CODE.MISSING_SEQUEST_PARAMS);
            throw ex;
        }
        try {
            parser.parseParams(remoteServer, fileDirectory);
            return parser;
        }
        catch (DataProviderException e) {
            UploadException ex = new UploadException(ERROR_CODE.PARAM_PARSING_ERROR);
            ex.setFile(fileDirectory+File.separator+parser.paramsFileName());
            ex.setErrorMessage(e.getMessage());
            throw ex;
        }
    }
    
    
    @Override
    int uploadSqtFile(String filePath, int runId) throws UploadException {
        
        log.info("BEGIN SQT FILE UPLOAD: "+(new File(filePath).getName())+"; RUN_ID: "+runId+"; SEARCH_ID: "+searchId);
//        lastUploadedRunSearchId = 0;
        long startTime = System.currentTimeMillis();
        SequestSQTFileReader provider = new SequestSQTFileReader();
        
        try {
            provider.open(filePath, usesEvalue);
            provider.setDynamicResidueMods(this.dynaResidueMods);
        }
        catch (DataProviderException e) {
            provider.close();
            UploadException ex = new UploadException(ERROR_CODE.READ_ERROR_SQT, e);
            ex.setFile(filePath);
            ex.setErrorMessage(e.getMessage()+"\n\t!!!SQT FILE WILL NOT BE UPLOADED!!!");
            throw ex;
        }
        
        int runSearchId;
        try {
            runSearchId = uploadSequestSqtFile(provider, searchId, runId, sequenceDatabaseId);
        }
        catch (UploadException ex) {
            ex.setFile(filePath);
            ex.appendErrorMessage("\n\t!!!SQT FILE WILL NOT BE UPLOADED!!!");
            throw ex;
        }
        catch (RuntimeException e) { // most likely due to SQL exception
            UploadException ex = new UploadException(ERROR_CODE.RUNTIME_SQT_ERROR, e);
            ex.setFile(filePath);
            ex.setErrorMessage(e.getMessage()+"\n\t!!!SQT FILE WILL NOT BE UPLOADED!!!");
            throw ex;
        }
        finally {provider.close();}
        
        long endTime = System.currentTimeMillis();
        
        log.info("END SQT FILE UPLOAD: "+provider.getFileName()+"; RUN_ID: "+runId+ " in "+(endTime - startTime)/(1000L)+"seconds\n");
        
        return runSearchId;
    }
    
    // parse and upload a sqt file
    private int uploadSequestSqtFile(SequestSQTFileReader provider, int searchId, int runId, int searchDbId) throws UploadException {
        
        int runSearchId;
        try {
            runSearchId = uploadSearchHeader(provider, runId, searchId);
            log.info("Uploaded top-level info for sqt file. runSearchId: "+runSearchId);
        }
        catch(DataProviderException e) {
            UploadException ex = new UploadException(ERROR_CODE.INVALID_SQT_HEADER, e);
            ex.setErrorMessage(e.getMessage());
            throw ex;
        }

        // upload the search results for each scan + charge combination
        int numResults = 0;
        while (provider.hasNextSearchScan()) {
            SequestSearchScan scan = null;
            try {
                scan = provider.getNextSearchScan();
            }
            catch (DataProviderException e) {
                UploadException ex = new UploadException(ERROR_CODE.INVALID_SQT_SCAN);
                ex.setErrorMessage(e.getMessage());
                throw ex;
            }
            
            int scanId = getScanId(runId, scan.getScanNumber());
            
            // save spectrum data
            if(uploadSearchScan(scan, runSearchId, scanId)) {
                // save all the search results for this scan
                for (SequestSearchResultIn result: scan.getScanResults()) {
                    // upload results only upto the given xCorrRank cutoff.
                    if(useXcorrRankCutoff && result.getSequestResultData().getxCorrRank() > xcorrRankCutoff)
                        continue;
                    uploadSearchResult(result, runSearchId, scanId);
                    numResults++;
                }
            }
            else {
                log.info("Ignoring search scan: "+scan.getScanNumber()+"; scanId: "+scanId+"; charge: "+scan.getCharge()+"; mass: "+scan.getObservedMass());
            }
        }
        flush(); // save any cached data
        log.info("Uploaded SQT file: "+provider.getFileName()+", with "+numResults+
                " results. (runSearchId: "+runSearchId+")");
        
        return runSearchId;
    }

    static SequestSearchIn makeSearchObject(final SequestParamsParser parser, final Program searchProgram,
                final String remoteDirectory, final java.util.Date searchDate) {
        return new SequestSearchIn() {
            @Override
            public List<SequestParam> getSequestParams() {return parser.getParamList();}
            @Override
            public List<MsResidueModificationIn> getDynamicResidueMods() {return parser.getDynamicResidueMods();}
            @Override
            public List<MsTerminalModificationIn> getDynamicTerminalMods() {return parser.getDynamicTerminalMods();}
            @Override
            public List<MsEnzymeIn> getEnzymeList() {
                if (parser.isEnzymeUsedForSearch())
                    return Arrays.asList(new MsEnzymeIn[]{parser.getSearchEnzyme()});
                else 
                    return new ArrayList<MsEnzymeIn>(0);
            }
            @Override
            public List<MsSearchDatabaseIn> getSearchDatabases() {return Arrays.asList(new MsSearchDatabaseIn[]{parser.getSearchDatabase()});}
            @Override
            public List<MsResidueModificationIn> getStaticResidueMods() {return parser.getStaticResidueMods();}
            @Override
            public List<MsTerminalModificationIn> getStaticTerminalMods() {return parser.getStaticTerminalMods();}
            @Override
            public Program getSearchProgram() {return searchProgram;}
//            public Program getSearchProgram() {return parser.getSearchProgram();}
            @Override
            public String getSearchProgramVersion() {return null;} // we don't have this information in sequest.params
            public java.sql.Date getSearchDate() {return new java.sql.Date(searchDate.getTime());}
            public String getServerDirectory() {return remoteDirectory;}
        };
    }
    
    void uploadSearchResult(SequestSearchResultIn result, int runSearchId, int scanId) throws UploadException {
        
        int resultId = super.uploadBaseSearchResult(result, runSearchId, scanId);
        
        // upload the SQT sequest specific information for this result.
        uploadSequestResultData(result.getSequestResultData(), resultId);
    }

    private void uploadSequestResultData(SequestResultData resultData, int resultId) {
        // upload the Sequest specific result information if the cache has enough entries
        if (sequestResultDataList.size() >= BUF_SIZE) {
            uploadSequestResultBuffer();
        }
        // add the Sequest specific information for this result to the cache
        ResultData resultDataDb = new ResultData(resultId, resultData);
        sequestResultDataList.add(resultDataDb);
    }
    
    private void uploadSequestResultBuffer() {
        sqtResultDao.saveAllSequestResultData(sequestResultDataList);
        sequestResultDataList.clear();
    }
    
    void flush() {
        super.flush();
        if (sequestResultDataList.size() > 0) {
            uploadSequestResultBuffer();
        }
    }
    
    private static final class ResultData implements SequestResultDataWId {
        private final SequestResultData data;
        private final int resultId;
        public ResultData(int resultId, SequestResultData data) {
            this.data = data;
            this.resultId = resultId;
        }
        @Override
        public int getResultId() {
            return resultId;
        }
        @Override
        public BigDecimal getCalculatedMass() {
            return data.getCalculatedMass();
        }
        @Override
        public BigDecimal getDeltaCN() {
            return data.getDeltaCN();
        }
        @Override
        public Double getEvalue() {
            return data.getEvalue();
        }
        @Override
        public int getMatchingIons() {
            return data.getMatchingIons();
        }
        @Override
        public int getPredictedIons() {
            return data.getPredictedIons();
        }
        @Override
        public BigDecimal getSp() {
            return data.getSp();
        }
        @Override
        public int getSpRank() {
            return data.getSpRank();
        }
        @Override
        public BigDecimal getxCorr() {
            return data.getxCorr();
        }
        @Override
        public int getxCorrRank() {
            return data.getxCorrRank();
        }
        @Override
        public BigDecimal getDeltaCNstar() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    
    @Override
    SearchFileFormat getSearchFileFormat() {
        return this.format;
    }

    @Override
    String searchParamsFile() {
        SequestParamsParser parser = new SequestParamsParser();
        return parser.paramsFileName();
    }

    @Override
    protected void copyFiles(int experimentId) throws UploadException {
        
        String backupDir = MsDataUploadProperties.getBackupDirectory();
        if(!new File(backupDir).exists()) {
            UploadException ex = new UploadException(ERROR_CODE.GENERAL);
            ex.appendErrorMessage("Backup directory: "+backupDir+" does not exist");
            throw ex;
        }
        // create a directory for the experiment
        String exptDir = backupDir+File.separator+experimentId;
        if(new File(exptDir).exists()) {
            UploadException ex = new UploadException(ERROR_CODE.GENERAL);
            ex.appendErrorMessage("Experiment backup directory: "+exptDir+" already exists");
            throw ex;
        }
        if(!new File(exptDir).mkdir()) {
            UploadException ex = new UploadException(ERROR_CODE.GENERAL);
            ex.appendErrorMessage("Could not create directory: "+exptDir);
            throw ex;
        }
        // copy sqt files from the data directory
        for(String file: getFileNames()) {
             File src = new File(getDataDirectory()+File.separator+file);
             File dest = new File(exptDir+File.separator+file);
             try {
                FileUtils.copyFile(src, dest);
            }
            catch (IOException e) {
                UploadException ex = new UploadException(ERROR_CODE.GENERAL, e);
                ex.appendErrorMessage("Could not copy file: "+getDataDirectory()+File.separator+file);
                throw ex;
            }
        }
        
        // create a decoy directory
        String decoyDir = exptDir+File.separator+"decoy";
        if(!new File(decoyDir).mkdir()) {
            UploadException ex = new UploadException(ERROR_CODE.GENERAL);
            ex.appendErrorMessage("Could not create decoy directory: "+decoyDir);
            throw ex;
        }
        // copy sqt files from the decoy directory
        for(String file: getFileNames()) {
            File src = new File(getDecoyDirectory()+File.separator+file);
            File dest = new File(decoyDir+File.separator+file);
            try {
               FileUtils.copyFile(src, dest);
           }
           catch (IOException e) {
               UploadException ex = new UploadException(ERROR_CODE.GENERAL, e);
               ex.appendErrorMessage("Could not copy file: "+getDataDirectory()+File.separator+file);
               throw ex;
           }
       }
    }
    
}