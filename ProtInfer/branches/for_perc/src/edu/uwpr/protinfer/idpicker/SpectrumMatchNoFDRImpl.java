/**
 * SpectrumMatchImpl.java
 * @author Vagisha Sharma
 * Jan 2, 2009
 * @version 1.0
 */
package edu.uwpr.protinfer.idpicker;

import edu.uwpr.protinfer.infer.SpectrumMatch;

/**
 * 
 */
public class SpectrumMatchNoFDRImpl implements SpectrumMatch {

    private int charge;
    private int hitId;
    private int runSearchId;
    private int scanId;
    private String peptideSequence;
    private int rank;
    
    public void setCharge(int charge) {
        this.charge = charge;
    }
    
    public int getCharge() {
        return charge;
    }

    public void setHitId(int hitId) {
        this.hitId = hitId;
    }
    
    public int getHitId() {
        return this.hitId;
    }

    
    public int getSourceId() {
        return runSearchId;
    }
    
    public void setSourceId(int sourceId) {
        this.runSearchId = sourceId;
    }
    
    public void setScanId(int scanId) {
        this.scanId = scanId;
    }
    
    @Override
    public int getScanId() {
        return this.scanId;
    }

    @Override
    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
    }
    
    @Override
    public String getSequence() {
        return peptideSequence;
    }
    
    public void setSequence(String sequence) {
        this.peptideSequence = sequence;
    }
}