package org.yeastrc.ms.dao.search.sequest;

import java.util.List;

import org.yeastrc.ms.dao.search.GenericSearchResultDAO;
import org.yeastrc.ms.domain.search.sequest.SequestResultDataWId;
import org.yeastrc.ms.domain.search.sequest.SequestSearchResult;
import org.yeastrc.ms.domain.search.sequest.SequestSearchResultIn;

public interface SequestSearchResultDAO extends GenericSearchResultDAO<SequestSearchResultIn, SequestSearchResult> {

    public abstract void saveAllSequestResultData(List<SequestResultDataWId> dataList);
    
    public abstract List<Integer> loadTopResultIdsForRunSearch(int runSearchId);
    
    /**
     * Returns the search results without any associated proteins.
     * The peptide for each result can optionally be associated with itd 
     * dynamic residue modifications.  Terminal and static modification information is not
     * added. 
     * @param runSearchId
     * @return
     */
    public abstract List<SequestSearchResult> loadTopResultsForRunSearchN(int runSearchId, 
                                                        boolean getDynaResMods);
}
