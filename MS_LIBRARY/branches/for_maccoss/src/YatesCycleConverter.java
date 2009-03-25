

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipException;

import org.apache.log4j.Logger;
import org.yeastrc.ms.service.MsDataUploader;
import org.yeastrc.ms.service.UploadException;
import org.yeastrc.ms2.data.DTAPeptide;

/**
 * 
 */
public class YatesCycleConverter {

    private static final Logger log = Logger.getLogger(YatesCycleConverter.class);
    
    public static void main(String[] args) throws Exception {
        
//        List<Integer> yatesRunIds = YatesTablesUtils.getAllYatesRunIds("ORDER BY runID DESC limit 50");
        List<Integer> yatesRunIds = new ArrayList<Integer>(1);
        yatesRunIds.add(2930);
        
        if (yatesRunIds.size() == 0) {
            log.error("No runIds found!!");
            return;
        }
       
        log.info("STARTED UPLOAD: "+new Date());
//        String dataDir = "/Users/vagisha/WORK/MS_LIBRARY/YATES_CYCLE_DUMP/UploadTest/dataDir";
//        String dataDir = "/Users/vagisha/WORK/MS_LIBRARY/YATES_CYCLE_DUMP/2930/FromServer/parc";
//        String dataDir = "/Users/vagisha/WORK/MS_LIBRARY/ProlucidData_dir/2985/RE";
//        String dataDir = "/a/scratch/ms_data/1217439094327";
//        String dataDir = "/Users/vagisha/WORK/MS_LIBRARY/YATES_CYCLE_DUMP/3079/parc";
        String dataDir = "/Users/vagisha/WORK/MS_LIBRARY/YATES_CYCLE_DUMP/3048/parc";
        int runId = 3048;
//        for (Integer runId: yatesRunIds) {
            log.info("------UPLOADING YATES runID: "+runId);
//            List<YatesTablesUtils.YatesCycle> cycles = YatesTablesUtils.getCyclesForRun(runId);
//            // download the files first
//            for (YatesTablesUtils.YatesCycle cycle: cycles) {
//                YatesCycleDownloader downloader = new YatesCycleDownloader();
//                downloader.downloadMS2File(cycle.cycleId, dataDir, cycle.cycleName+".ms2");
//                downloader.downloadSQTFile(cycle.cycleId, dataDir, cycle.cycleName+".sqt");
//            }
            
            // upload data to msData database
//            MsDataUploader uploader = new MsDataUploader();
//            int searchId = uploader.uploadExperimentToDb("remoteServer", "remoteDirectory", dataDir, new Date());
            int searchId = 1;
            updateDTASelectTable(searchId, runId);
           
            // mysql> select pep.id from tblYatesRun as run, tblYatesRunResult as res, tblYatesResultPeptide as pep where run.id = 3079 and run.id = res.runID and res.id =  pep.resultID;

            // delete the files
//            for (YatesTablesUtils.YatesCycle cycle: cycles) {
//                log.info("Deleting yates cycle files......");
//                new File(dataDir+File.separator+cycle.cycleName+".ms2").delete();
//                new File(dataDir+File.separator+cycle.cycleName+".sqt").delete();
//            }
//            // make sure not ms2 or sqt files are left in the directory;
//            String[] files = new File(dataDir).list();
//            if (files.length > 0)
//                throw new IllegalStateException("Files for previous experiment were not all deleted. Cannot continue...");
            log.info("------UPLOADED EXPERIMENT: "+searchId+" for yates run: "+runId+"\n\n");
//        }
        log.info("FINISHED UPLOAD: "+new Date());
    }
    
    public static void updateDTASelectTable(int searchId, int yatesRunId) throws Exception {
        // get the ids from tblYatesResultPeptide which belong to the given yatesRunId
        List<Integer> ypIds = YatesTablesUtils.getYatesResultPeptideIds(yatesRunId);
        for (Integer id: ypIds) {
            // load the peptide
            DTAPeptide peptide = YatesTablesUtils.loadDTAPeptide(id);
            peptide.setSearchID(searchId);
            // get the scanId
            String runFileScanString = peptide.getFilename();
            int scanId = MsDataUploader.getScanIdFor(runFileScanString, searchId);
            peptide.setScanID(scanId);
            System.out.println("searchID: "+searchId+"; scanId: "+scanId+"; scanstring: "+runFileScanString);
            YatesTablesUtils.updateDTAPeptide(peptide);
        }
    }
}
