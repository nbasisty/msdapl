/**
 * Ms2FileHeaders.java
 * @author Vagisha Sharma
 * Jun 16, 2008
 * @version 1.0
 */
package org.yeastrc.ms.dto.ms2File;

/**
 * 
 */
public class MS2FileHeader extends BaseHeader {

    private int id; // id(database) of the header
    private int runId;                  
    
    /**
     * @return the id
     */
    public int getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * @return the runId
     */
    public int getRunId() {
        return runId;
    }
    /**
     * @param runId the runId to set
     */
    public void setRunId(int runId) {
        this.runId = runId;
    }

}
