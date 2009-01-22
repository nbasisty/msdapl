/**
 * IDPickerExecutor.java
 * @author Vagisha Sharma
 * Nov 3, 2008
 * @version 1.0
 */
package edu.uwpr.protinfer.idpicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.yeastrc.ms.dao.DAOFactory;
import org.yeastrc.ms.dao.analysis.MsRunSearchAnalysisDAO;
import org.yeastrc.ms.dao.nrseq.NrSeqLookupUtil;
import org.yeastrc.ms.dao.search.MsRunSearchDAO;
import org.yeastrc.ms.dao.search.MsSearchDatabaseDAO;
import org.yeastrc.ms.domain.analysis.MsRunSearchAnalysis;
import org.yeastrc.ms.domain.nrseq.NrDbProtein;
import org.yeastrc.ms.domain.search.MsRunSearch;
import org.yeastrc.ms.domain.search.MsSearchDatabase;
import org.yeastrc.ms.domain.search.Program;

import edu.uwpr.protinfer.PeptideDefinition;
import edu.uwpr.protinfer.database.dao.ProteinferDAOFactory;
import edu.uwpr.protinfer.database.dao.idpicker.ibatis.IdPickerRunDAO;
import edu.uwpr.protinfer.database.dto.idpicker.IdPickerParam;
import edu.uwpr.protinfer.database.dto.idpicker.IdPickerRun;
import edu.uwpr.protinfer.infer.InferredProtein;
import edu.uwpr.protinfer.infer.Peptide;
import edu.uwpr.protinfer.infer.PeptideEvidence;
import edu.uwpr.protinfer.infer.PeptideHit;
import edu.uwpr.protinfer.infer.PeptideSpectrumMatch;
import edu.uwpr.protinfer.infer.Protein;
import edu.uwpr.protinfer.infer.ProteinHit;
import edu.uwpr.protinfer.infer.ProteinInferrerMaximal;
import edu.uwpr.protinfer.infer.SpectrumMatch;
import edu.uwpr.protinfer.util.StringUtils;
import edu.uwpr.protinfer.util.TimeUtils;


public class IDPickerExecutor {

    private static Logger log = Logger.getLogger(IDPickerExecutor.class);
    
    
    public void execute(IdPickerRun idpRun) throws Exception {
        
        if(idpRun.getInputList().size() == 0)
            return;
        
        long start = System.currentTimeMillis();
        
        // create the parameters object
        IDPickerParams params = IdPickerParamsMaker.makeIdPickerParams(idpRun.getParams());
        log.info("\n"+params.toString());
        
        // Are we going to do FDR calculation?
        if(params.getDoFdrCalculation()) {
            IdPickerExecutorFDR fdrExe  = new IdPickerExecutorFDR();
            fdrExe.execute(idpRun, params);
        }
        else {
            IdPickerExecutorNoFDR noFdrExe = new IdPickerExecutorNoFDR();
            noFdrExe.execute(idpRun, params);
        }
        
        
        long end = System.currentTimeMillis();
        log.info("IDPicker TOTAL run time: "+TimeUtils.timeElapsedMinutes(start, end)+" minutes");
    }
    
    private static <T extends SpectrumMatch> void calculateProteinSequenceCoverage(List<InferredProtein<T>> proteins) throws Exception {
        
        long start = System.currentTimeMillis();
        
        for(InferredProtein<T> prot: proteins) {
            int nrseqProteinId = prot.getProteinId();
            String proteinSeq = null;
            try {
                proteinSeq = NrSeqLookupUtil.getProteinSequenceForNrSeqDbProtId(nrseqProteinId);
//                proteinSeq = NrSeqLookupUtil.getProteinSequence(nrseqProteinId);
            }
            catch (Exception e) {
                log.error("Exception getting nrseq protein for proteinId: "+nrseqProteinId, e);
                throw e;
            }
            
            if(proteinSeq == null) {
                log.error("Protein sequence for proteinId: "+nrseqProteinId+" is null.");
                throw new Exception("Protein sequence for proteinId: "+nrseqProteinId+" is null.");
            }
                
            List<String> peptides = new ArrayList<String>();
            for(PeptideEvidence<T> pev: prot.getPeptides()) {
                peptides.add(pev.getPeptide().getPeptideSequence());
            }
            int lengthCovered = StringUtils.getCoveredSequenceLength(proteinSeq, peptides);
            float percCovered = ((float)lengthCovered/(float)proteinSeq.length()) * 100.0f;
            prot.setPercentCoverage(percCovered);
        }
        long end = System.currentTimeMillis();
        log.info("Calculated protein sequence coverage in : "+TimeUtils.timeElapsedSeconds(start, end)+" seconds");
    }
    
    protected static <T extends PeptideSpectrumMatch<?>> void assignIdsToPeptidesAndProteins(List<T> filteredPsms, Program inputGenerator) throws Exception {
        assignNrSeqProteinIds(filteredPsms, inputGenerator);
        assignPeptideIds(filteredPsms);
    }

    private static <T extends PeptideSpectrumMatch<?>> void assignPeptideIds(List<T> filteredPsms) {
        Map<String, Integer> peptideIds = new HashMap<String, Integer>(filteredPsms.size());
        int currPeptId = 1;
        for(T psm: filteredPsms) {
            Peptide pept = psm.getPeptideHit().getPeptide();
            Integer id = peptideIds.get(pept.getPeptideKey());
            if (id == null) {
                id = currPeptId;
                peptideIds.put(pept.getPeptideKey(), id);
                currPeptId++;
            }
            pept.setId(id);
        }
    }

    private static int getNrSeqDatabaseId(int inputId, Program inputGenerator) {
        
        DAOFactory fact = DAOFactory.instance();
        MsRunSearchDAO runSearchDao = fact.getMsRunSearchDAO();
        MsSearchDatabaseDAO dbDao = fact.getMsSequenceDatabaseDAO();
        
        int runSearchId = 0;
        if(Program.isSearchProgram(inputGenerator)) {
            runSearchId = inputId;
        }
        else {
            MsRunSearchAnalysisDAO rsAnalysisDao = fact.getMsRunSearchAnalysisDAO();
            MsRunSearchAnalysis analysis = rsAnalysisDao.load(inputId);
            
            runSearchId = analysis.getRunSearchId();
        }
        MsRunSearch runSearch = runSearchDao.loadRunSearch(runSearchId);
        if(runSearch == null) {
            log.error("Could not load runSearch with id: "+runSearchId);
            return 0;
        }
        List<MsSearchDatabase> searchDbs = dbDao.loadSearchDatabases(runSearch.getSearchId());
        if(searchDbs.size() != 1) {
            log.warn("No search database found for searchID: "+runSearch.getSearchId());
            return 0;
        }
        int nrseqDbId = searchDbs.get(0).getSequenceDatabaseId();
        return nrseqDbId;
    }
    
    private static <T extends PeptideSpectrumMatch<?>> void assignNrSeqProteinIds(List<T> filteredPsms, Program inputGenerator) throws Exception {
        
        long start = System.currentTimeMillis();
        
        Map<Integer, Integer> nrseqDbIds = new HashMap<Integer, Integer>();
        Map<String, NrDbProtein> nrseqIdMap = new HashMap<String, NrDbProtein>();
       
        for(T hit: filteredPsms) {
            
           int inputId = hit.getSpectrumMatch().getSourceId();
           Integer nrseqDbId = nrseqDbIds.get(inputId);
           if(nrseqDbId == null) {
               nrseqDbId = getNrSeqDatabaseId(inputId, inputGenerator);
               if(nrseqDbId == 0) {
                   log.error("Could not find nrseq db ID for runSearchID "+inputId);
                   throw new Exception("Could not find nrseq db ID for runSearchID "+inputId);
               }
               nrseqDbIds.put(inputId, nrseqDbId);
           }
           
           
           List<ProteinHit> moreProteins = new ArrayList<ProteinHit>();
           
           PeptideHit phit = hit.getPeptideHit();
           for(ProteinHit prHit: phit.getProteinList()) {
               Protein pr = prHit.getProtein();
               
               // look for a match in our map
               NrDbProtein nrDbProt = nrseqIdMap.get(pr.getAccession());
               
               // this protein is not in our map
               if(nrDbProt == null) {
                   
                   // look for an exact match
                   nrDbProt  = NrSeqLookupUtil.getDbProtein(nrseqDbId, pr.getAccession());
                   
                   // exact match not found
                   if(nrDbProt == null) {
                       // look for a match LIKE accession
                       List<Integer> ids = NrSeqLookupUtil.getDbProteinIdsPartialAccession(nrseqDbId, pr.getAccession());
                       
                       // more than one match found
                       if(ids.size() != 1) {
                           
                           // finally try to match the peptide sequence and accession
                           ids = NrSeqLookupUtil.getDbProteinIdsForPeptidePartialAccession(nrseqDbId, pr.getAccession(),
                                   phit.getPeptide().getPeptideSequence());
                           if(ids.size() != 1) {
                               log.error("Found multiple ("+ids.size()+") nrseq ids for protein: "+pr.getAccession()+
                                           "; database: "+nrseqDbId+"; peptide: "+phit.getPeptide().getPeptideSequence());
                              // throw new Exception("Could not find nrseq id for protein: "+pr.getAccession()+"; database: "+nrseqDbId);
                           
                               // IF WE HAVE MULTIPLE MATCHES IT MEANS WE HAVE A TRUNCATED ACCESSION AND
                               // A VERY SHORT PEPTIDE SEQUENCE.  ADD THEM ALL TO THE LIST
                               
                               for(int id: ids) {
                                   NrDbProtein nrDbProtM = NrSeqLookupUtil.getDbProtein(id);
                                   nrseqIdMap.put(nrDbProtM.getAccessionString(), nrDbProtM);
                                   Protein prM = new Protein(nrDbProtM.getAccessionString(), -1);
                                   prM.setId(nrDbProtM.getId());
                                   // pr.setId(nrDbProt.getProteinId()); // protein ID, NOT the id (primary key) from tblProteinDatabase
                                   prM.setAccession(nrDbProtM.getAccessionString());
                                   moreProteins.add(new ProteinHit(prM, '\u0000', '\u0000'));
                               }
                           
                           }
                           // match found -- with peptide sequence and partial accession
                           else {
                               nrDbProt = NrSeqLookupUtil.getDbProtein(ids.get(0));
                               nrseqIdMap.put(pr.getAccession(), nrDbProt);
                           }
                       }
                       // match found with partial accession
                       else {
                           nrDbProt = NrSeqLookupUtil.getDbProtein(ids.get(0));
                           nrseqIdMap.put(pr.getAccession(), nrDbProt);
                       }
                   }
                   // exact match found
                   else
                       nrseqIdMap.put(pr.getAccession(), nrDbProt);
               }
               
               // If we found an exact match
               if(nrDbProt != null) {
                   pr.setId(nrDbProt.getId());
//                 pr.setId(nrDbProt.getProteinId()); // protein ID, NOT the id (primary key) from tblProteinDatabase
                   pr.setAccession(nrDbProt.getAccessionString()); // this will set the correct accession; 
                                                               // SQT files sometimes have truncated accessions
               }
               else {pr.setId(-1);}
           }
           
           // REMOVE ALL PROTEINS FOR WHICH NO ID WAS FOUND
           Iterator<ProteinHit> iter = phit.getProteinList().iterator();
           while(iter.hasNext()) {
               ProteinHit prot = iter.next();
               if(prot.getProtein().getId() == -1)
                   iter.remove();
           }
           // ADD ALL THE ADDITIONAL PROTEINS, IF ANY.
           for(ProteinHit prot: moreProteins) {
               phit.addProteinHit(prot);
           }
        }
        long end = System.currentTimeMillis();
        log.info("Retrieved NRSEQ ids in: "+TimeUtils.timeElapsedMinutes(start, end)+" minutes");
    }
    
    
    protected static <T extends PeptideSpectrumMatch<?>> void removeSpectraWithMultipleResults(List<T> psmList) {
        
        long s = System.currentTimeMillis();
        // sort by scanID
        Collections.sort(psmList, new Comparator<PeptideSpectrumMatch<?>>() {
            public int compare(PeptideSpectrumMatch<?> o1, PeptideSpectrumMatch<?> o2) {
                return Integer.valueOf(o1.getScanId()).compareTo(o2.getScanId());
            }});
        
        // get a list of scan Ids that have multiple results
        Set<Integer> scanIdsToRemove = new HashSet<Integer>();
//        Set<Integer> allScanIds = new HashSet<Integer>();
        
        int lastScanId = -1;
        for (int i = 0; i < psmList.size(); i++) {
            T psm = psmList.get(i);
//            allScanIds.add(psm.getScanId());
            if(lastScanId != -1){
                if(lastScanId == psm.getScanId()) {
                    scanIdsToRemove.add(lastScanId);
                }
            }
            lastScanId = psm.getScanId();
        }
        
        Iterator<T> iter = psmList.iterator();
        while(iter.hasNext()) {
            T psm = iter.next();
            if(scanIdsToRemove.contains(psm.getScanId())) {
//                log.info("Removing for scanID: "+psm.getScanId()+"; resultID: "+psm.getHitId());
                iter.remove();
            }
        }
        long e = System.currentTimeMillis();
//        log.info("\nRR\t"+runSearchAnalysisId+"\t"+allScanIds.size()+"\t"+scanIdsToRemove.size());
        log.info("Removed "+scanIdsToRemove.size()+" scans with multiple results. "+
                "Remaining results: "+psmList.size()+". Time: "+TimeUtils.timeElapsedSeconds(s, e)+" seconds\n");
    }
    
    
    protected static <S extends SpectrumMatch, T extends PeptideSpectrumMatch<S>> 
        List<InferredProtein<S>> inferProteins(List<T> psms, IDPickerParams params) throws Exception {
        
        // first infer all proteins
        ProteinInferrerMaximal maxInferrer = new ProteinInferrerMaximal();
        List<InferredProtein<S>> allProteins = maxInferrer.inferProteins(psms);
        
        // calculate protein coverage
        calculateProteinSequenceCoverage(allProteins); // throws exception
        
        // filter proteins by coverage
        if(params.getMinCoverage() > 0.0) {
            List<InferredProtein<S>> removed = new ArrayList<InferredProtein<S>>();
            float coverage = params.getMinCoverage();
            Iterator<InferredProtein<S>> iter = allProteins.iterator();
            while(iter.hasNext()) {
                InferredProtein<S> prot = iter.next();
                if(prot.getPercentCoverage() < coverage) {
                    removed.add(prot);
                    iter.remove();
                }
            }
        }
        
        ProteinInferrerIdPicker inferrer = new ProteinInferrerIdPicker();
        return inferrer.inferProteins(allProteins, params);
    }
    
    public static <T extends InferredProtein<?>>void replaceNrSeqDbProtIdsWithProteinIds(List<T> proteins) {
        
        long s = System.currentTimeMillis();
        for(T prot: proteins) {
            Protein pr = prot.getProtein();
            int nrseqDbProtId = pr.getId(); // This is the id (primary key) from tblProteinDatabase
            NrDbProtein nrDbProt = NrSeqLookupUtil.getDbProtein(nrseqDbProtId);
            pr.setId(nrDbProt.getProteinId());
        }
        long e = System.currentTimeMillis();
        log.info("Replaced NRSEQ dbProt Ids with NRSEQ protein Ids in: "+TimeUtils.timeElapsedSeconds(s, e)+" seconds");
    }

    
    public static void main(String[] args) {
        
        ProteinferDAOFactory factory = ProteinferDAOFactory.instance();
        
//        // save a protein inference run and input files
//        ProteinferRun pirun = new ProteinferRun();
//        pirun.setInputGenerator(Program.PERCOLATOR);
//        pirun.setProgram(ProteinInferenceProgram.IDPICKER_PERC);
//        int pinferId = factory.getProteinferRunDao().save(pirun);
//        
//        int searchAnalysisId = 4;
//        MsRunSearchAnalysisDAO adao = DAOFactory.instance().getMsRunSearchAnalysisDAO();
//        List<Integer> rsAnalysisIds = adao.getRunSearchAnalysisIdsForAnalysis(searchAnalysisId);
//        for(int id: rsAnalysisIds) {
////            MsRunSearchAnalysis analysis = adao.load(id); 
//            ProteinferInput input = new ProteinferInput();
//            input.setProteinferId(pinferId);
//            input.setInputId(id);
//            factory.getProteinferInputDao().saveProteinferInput(input);
//        }
        
//        // save the parameters
//        IdPickerFilterDAO filterDao = factory.getProteinferFilterDao();
//        IdPickerFilter filter = new IdPickerFilter();
//        filter.setFilterName("pep_percolator");
//        filter.setFilterValue("0.05");
//        filter.setProteinferId(pinferId);
//        filterDao.saveProteinferFilter(filter);
//        filter = new IdPickerFilter();
//        filter.setFilterName("qval_percolator");
//        filter.setFilterValue("0.05");
//        filter.setProteinferId(pinferId);
//        filterDao.saveProteinferFilter(filter);
        
        
        
        
        IdPickerRunDAO runDao = factory.getIdPickerRunDao();
        IdPickerRun run = runDao.loadProteinferRun(6);
        System.out.println("Number of files: "+run.getInputList().size());
        System.out.println("Number of filters: "+run.getParams().size());
        
        IDPickerExecutor executor = new IDPickerExecutor();
        try {
            executor.execute(run);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
