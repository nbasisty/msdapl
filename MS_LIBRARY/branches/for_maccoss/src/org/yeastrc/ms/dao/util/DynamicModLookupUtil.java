package org.yeastrc.ms.dao.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yeastrc.ms.dao.DAOFactory;
import org.yeastrc.ms.dao.search.MsSearchModificationDAO;
import org.yeastrc.ms.domain.search.MsResidueModification;
import org.yeastrc.ms.domain.search.MsResidueModificationIn;
import org.yeastrc.ms.domain.search.MsTerminalModification;
import org.yeastrc.ms.domain.search.MsTerminalModificationIn;
import org.yeastrc.ms.domain.search.MsTerminalModification.Terminal;

public class DynamicModLookupUtil {


    private MsSearchModificationDAO modDao;

    private static Map<String, Integer> residueModMap;
    private static Map<String, Integer> terminalModMap;
    
    private int searchId;

    public DynamicModLookupUtil(int searchId){
        modDao = DAOFactory.instance().getMsSearchModDAO();
        residueModMap = new HashMap<String, Integer>();
        terminalModMap = new HashMap<String, Integer>();
        buildModLookups(searchId);
        this.searchId = searchId;
    }

    public int getSearchId() {
        return searchId;
    }
    
    private void buildModLookups(int searchId) {
        buildResidueModLookup(searchId);
        buildTerminalModLookup(searchId);
    }
    
    private void buildResidueModLookup(int searchId) {
        residueModMap.clear();
        List<MsResidueModification> dynaMods = modDao.loadDynamicResidueModsForSearch(searchId);
        String key = null;
        for (MsResidueModification mod: dynaMods) {
            key = mod.getModifiedResidue()+""+mod.getModificationMass().doubleValue();
            residueModMap.put(key, mod.getId());
        }
    }

    private void buildTerminalModLookup(int searchId) {
        terminalModMap.clear();
        List<MsTerminalModification> dynaMods = modDao.loadDynamicTerminalModsForSearch(searchId);
        String key = null;
        for (MsTerminalModification mod: dynaMods) {
            key = mod.getModifiedTerminal()+""+mod.getModificationMass().doubleValue();
            terminalModMap.put(key, mod.getId());
        }
    }

    /**
     * @param searchId
     * @param mod
     * @return
     */
    public int getDynamicResidueModificationId(int searchId, MsResidueModificationIn mod) {
        return getDynamicResidueModificationId(searchId, mod.getModifiedResidue(), mod.getModificationMass());
    }
    
    /**
     * @param searchId
     * @param modChar
     * @param modMass
     * @return
     */
    public int getDynamicResidueModificationId(int searchId, char modChar, BigDecimal modMass) {
        Integer modId = residueModMap.get(modChar+""+modMass.doubleValue());
        if (modId != null)  return modId;
        return 0;
    }
    
    /**
     * @param searchId
     * @param mod
     * @return
     */
    public int getDynamicTerminalModificationId(int searchId, MsTerminalModificationIn mod) {
        return getDynamicTerminalModificationId(searchId, mod.getModifiedTerminal(), mod.getModificationMass());
    }
    
    /**
     * @param searchId
     * @param modTerminal
     * @param modMass
     * @return 0 if no match is found
     */
    public int getDynamicTerminalModificationId(int searchId, Terminal modTerminal, BigDecimal modMass) {
        Integer modId = terminalModMap.get(modTerminal+""+modMass.doubleValue());
        if (modId != null)  return modId;
        return 0;
    }
}
