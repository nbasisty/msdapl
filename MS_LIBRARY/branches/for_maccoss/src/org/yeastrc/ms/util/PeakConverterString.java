/**
 * PeakConverterString.java
 * @author Vagisha Sharma
 * Jul 28, 2008
 * @version 1.0
 */
package org.yeastrc.ms.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.yeastrc.ms.domain.run.Peak;

/**
 * 
 */
public class PeakConverterString {

    private static PeakConverterString instance;
    private PeakConverterString(){}
    
    public static PeakConverterString instance() {
        if(instance == null)
            instance = new PeakConverterString();
        return instance;
    }
    
    public List<String[]> convert(String peakString) {

        List<String[]> peakList = new ArrayList<String[]>();

        if (peakString == null || peakString.length() == 0)
            return peakList;

        String[] peaksStr = peakString.split("\\n");
        for (String peak: peaksStr) {
            String [] peakVals = splitPeakVals(peak);
            peakList.add(peakVals);
        }
        return peakList;
    }

    private String[] splitPeakVals(String peak) {
        int i = peak.indexOf(" ");
        String[] vals = new String[2];
        vals[0] = peak.substring(0, i);
        if (vals[0].lastIndexOf('.') == -1) vals[0] = vals[0]+".0";
        vals[1] = peak.substring(i+1, peak.length());
        if (vals[1].lastIndexOf('.') == -1) vals[1] = vals[1]+".0";
        return vals;
    }
    
    public List<String[]> convert(byte[] peakData, boolean hasNumbers) throws IOException {
        if(!hasNumbers) {
            return convert(new String(peakData));
        }
        else {
            ByteArrayInputStream bis = null;
            DataInputStream dis = null;
            List<String[]> peaks = new ArrayList<String[]>();
            try {
                bis = new ByteArrayInputStream(peakData);
                dis = new DataInputStream(bis);
                while(true) {
                    try {
                        String mz = String.valueOf(dis.readDouble());
                        String intensity = String.valueOf(dis.readFloat());
                        peaks.add(new String[]{mz, intensity});
                    }
                    catch (EOFException e) {
                        break;
                    }
                }
            }
            finally {
                if(dis != null) dis.close();
                if(bis != null) bis.close();
            }
            return peaks;
        }
    }
}
