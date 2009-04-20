/**
 * ProteinDatasetSorter.java
 * @author Vagisha Sharma
 * Apr 18, 2009
 * @version 1.0
 */
package org.yeastrc.www.compare;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 
 */
public class ProteinDatasetSorter {

    private static ProteinDatasetSorter instance;
    private ProteinDatasetSorter() {}
    
    public static ProteinDatasetSorter instance() {
        if(instance == null)
            instance = new ProteinDatasetSorter();
        return instance;
    }
    
    public void sortByName(ProteinComparisonDataset dataset) throws SQLException {
        List<ComparisonProtein> proteins = dataset.getProteins();
        
        List<Integer> nrseqProteinIds = new ArrayList<Integer>(proteins.size());
        for(ComparisonProtein protein: proteins)
            nrseqProteinIds.add(protein.getNrseqId());
        
        CommonNameLookupUtil lookup = CommonNameLookupUtil.instance();
        List<CommonListing> listings = lookup.getCommonListings(nrseqProteinIds);
        for(int i = 0; i < nrseqProteinIds.size(); i++) {
            ComparisonProtein protein = proteins.get(i);
            CommonListing listing = listings.get(i);
            protein.setName(listing.getName());
            protein.setDescription(listing.getDescription());
        }
        
        Collections.sort(proteins, new NameCompartor());
    }
    
    public void sortByPeptideCount(ProteinComparisonDataset dataset) throws SQLException {
        List<ComparisonProtein> proteins = dataset.getProteins();
        for(ComparisonProtein protein: proteins) {
         // get the (max)number of peptides identified for this protein
            protein.setMaxPeptideCount(DatasetPeptideComparer.instance().getMaxPeptidesForProtein(protein));
        }
        Collections.sort(proteins, new PeptideCountCompartor());
    }
    
    private static class NameCompartor implements Comparator<ComparisonProtein> {

        @Override
        public int compare(ComparisonProtein o1, ComparisonProtein o2) {
            if(o1.getName() == null && o2.getName() == null)
                return 0;
            if(o1.getName() == null || o1.getName().length() == 0)
                return 1;
            if(o2.getName() == null || o2.getName().length() == 0)
                return -1;
            
            return o1.getName().compareTo(o2.getName());
        }
        
    }
    
    private static class PeptideCountCompartor implements Comparator<ComparisonProtein> {
        @Override
        public int compare(ComparisonProtein o1, ComparisonProtein o2) {
            return Integer.valueOf(o2.getMaxPeptideCount()).compareTo(o1.getMaxPeptideCount());
        }
        
    }
}