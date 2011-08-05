/**
 * 
 */
package org.yeastrc.ms.writer.mzidentml;

import java.util.List;

import org.yeastrc.ms.domain.search.sequest.SequestSearchResultIn;
import org.yeastrc.ms.service.ModifiedSequenceBuilderException;
import org.yeastrc.ms.writer.mzidentml.jaxb.AbstractParamType;
import org.yeastrc.ms.writer.mzidentml.jaxb.CVParamType;
import org.yeastrc.ms.writer.mzidentml.jaxb.SpectrumIdentificationItemType;

/**
 * SequestSpectrumIdentificationItemMaker.java
 * @author Vagisha Sharma
 * Aug 3, 2011
 * 
 */
public class SequestSpectrumIdentificationItemMaker {

	private final String filename;
	
	public SequestSpectrumIdentificationItemMaker(String filename) {
		this.filename = filename;
	}
	
	public SpectrumIdentificationItemType make(SequestSearchResultIn result, int resultIndex) throws ModifiedSequenceBuilderException {
		
		SpectrumIdentificationItemType specIdItem = new SpectrumIdentificationItemType();
		
		String id = makeId(filename, result.getScanNumber(), result.getCharge(), resultIndex);
		
		specIdItem.setId(id);
		
		specIdItem.setCalculatedMassToCharge(result.getSequestResultData().getCalculatedMass().doubleValue());
		
		specIdItem.setChargeState(result.getCharge());
		
		specIdItem.setExperimentalMassToCharge(result.getObservedMass().doubleValue());
		
		/*
		 * Set to true if the producers of the file has deemed that the identification has passed a 
		 * given threshold or been validated as correct. If no such threshold has been set, value of 
		 * true should be given for all results. 
		 */
		specIdItem.setPassThreshold(true); 
		
		// We are using the modified sequence of the peptide as a unique identifier
		specIdItem.setPeptideRef(result.getResultPeptide().getModifiedPeptide());
		
		specIdItem.setRank(result.getSequestResultData().getxCorrRank());
		
		List<AbstractParamType> params = specIdItem.getParamGroup();
		
		// DeltaCN
		CVParamType param = CvParamMaker.getInstance().make("MS:1001156", "Sequest:deltacn", 
				String.valueOf(result.getSequestResultData().getDeltaCN()), CvConstants.PSI_CV);
		params.add(param);
		
		// XCorr
		param = CvParamMaker.getInstance().make("MS:1001155", "Sequest:xcorr",
				String.valueOf(result.getSequestResultData().getxCorr()), CvConstants.PSI_CV);
		params.add(param);
		
		
		if(result.getSequestResultData().getSp() != null) {
			// Sp
			// NOTE: looks like there are two terms for Sp score in the psi-ms.obo
			// (http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo)
			// I am using the one from the example file
			//param = CvParamMaker.getInstance().make("MS:1001157", "Sequest:sp",
			param = CvParamMaker.getInstance().make("MS:1001215", "Sequest:PeptideSp",
					String.valueOf(result.getSequestResultData().getSp()), CvConstants.PSI_CV);
			params.add(param);
		}
		
		// some sqt files have e-value in the sp column
		if(result.getSequestResultData().getEvalue() != null) {
			
			param = CvParamMaker.getInstance().make("MS:1001159", "Sequest:expectation value",
					String.valueOf(result.getSequestResultData().getEvalue()), CvConstants.PSI_CV);
			params.add(param);
		}
		
		// Sp rank
		param = CvParamMaker.getInstance().make("MS:1001217", "Sequest:PeptideRankSp",
				String.valueOf(result.getSequestResultData().getSpRank()), CvConstants.PSI_CV);
		params.add(param);
		
		// # matched ions
		param = CvParamMaker.getInstance().make("MS:1001161", "Sequest:matched ions", 
				String.valueOf(result.getSequestResultData().getMatchingIons()), CvConstants.PSI_CV);
		params.add(param);
		
		// # total ions
		param = CvParamMaker.getInstance().make("MS:1001162", "Sequest:total ions", 
				String.valueOf(result.getSequestResultData().getPredictedIons()), CvConstants.PSI_CV);
		params.add(param);
		
		
		//param = CvParamMaker.getInstance().make("", "", "", CvConstants.PSI_CV);
		//params.add(param);
		
		/* From Example file: 
		 
		 <cvParam accession="MS:1001218" name="sequest:PeptideNumber" cvRef="PSI-MS" value="1"/>
          <userParam name="sequest:PeptideRank" value="1"/>
          <cvParam accession="MS:1001217" name="sequest:PeptideRankSp" cvRef="PSI-MS" value="115"/>
          <cvParam accession="MS:1001219" name="sequest:PeptideIdnumber" cvRef="PSI-MS" value="0"/>
          
          <cvParam accession="MS:1001156" name="sequest:deltacn" cvRef="PSI-MS" value="0.0"/>
          <cvParam accession="MS:1001155" name="sequest:xcorr" cvRef="PSI-MS" value="0.5477"/>
          
          <cvParam accession="MS:1001215" name="sequest:PeptideSp" cvRef="PSI-MS" value="29.4"/>
          <cvParam accession="MS:1001161" name="sequest:matched ions" cvRef="PSI-MS" value="3"/>
          <cvParam accession="MS:1001162" name="sequest:total ions" cvRef="PSI-MS" value="8"/>

		 */
		
		return specIdItem;
	}

	private String makeId(String filename, int scanNumber, int charge, int resultIndex) {
		
		return filename+"_"+scanNumber+"_"+charge+"_"+resultIndex;
	}
	
}