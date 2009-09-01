/**
 * SORT_BY.java
 * @author Vagisha Sharma
 * Aug 28, 2009
 * @version 1.0
 */
package org.yeastrc.ms.domain.protinfer;

public enum SORT_BY {
    NUM_PEPT, 
    NUM_UNIQ_PEPT, 
    ACCESSION, 
    COVERAGE, 
    NUM_SPECTRA, 
    GROUP_ID,
    CLUSTER_ID,
    VALIDATION_STATUS,
    NSAF,
    PROBABILITY,
    PROTEIN_PROPHET_GROUP,
    NONE;
    
    public static SORT_BY getSortByForString(String sortBy) {
        if(sortBy == null)
            return NONE;
        else if (sortBy.equalsIgnoreCase(NUM_PEPT.name())) return NUM_PEPT;
        else if (sortBy.equalsIgnoreCase(NUM_UNIQ_PEPT.name())) return NUM_UNIQ_PEPT;
        else if (sortBy.equalsIgnoreCase(ACCESSION.name())) return ACCESSION;
        else if (sortBy.equalsIgnoreCase(COVERAGE.name())) return COVERAGE;
        else if (sortBy.equalsIgnoreCase(NUM_SPECTRA.name())) return NUM_SPECTRA;
        else if (sortBy.equalsIgnoreCase(GROUP_ID.name())) return GROUP_ID;
        else if (sortBy.equalsIgnoreCase(CLUSTER_ID.name())) return CLUSTER_ID;
        else if (sortBy.equalsIgnoreCase(VALIDATION_STATUS.name())) return VALIDATION_STATUS;
        else if (sortBy.equalsIgnoreCase(NSAF.name())) return NSAF;
        else if(sortBy.equalsIgnoreCase(PROBABILITY.name()))    return PROBABILITY;
        else if(sortBy.equalsIgnoreCase(PROTEIN_PROPHET_GROUP.name()))    return PROTEIN_PROPHET_GROUP;
        else    return NONE;
        
    }
}