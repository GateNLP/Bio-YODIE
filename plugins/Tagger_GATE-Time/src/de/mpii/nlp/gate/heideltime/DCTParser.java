/*
 *  DCTParser.java
 *
 * Copyright (c) 2016, The University of Sheffield.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *  jstroetge, 14/3/2016 (jannik.stroetgen@gmail.com)
 *
 * For details on the configuration options, see the user guide:
 * http://gate.ac.uk/cgi-bin/userguide/sec:creole-model:config
 */

package de.mpii.nlp.gate.heideltime;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import gate.creole.metadata.Optional;
import gate.util.InvalidOffsetException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/** 
 * This class is the implementation of the resource DCTParser.
 * The goal is to parse and create DCTs from existing annotations so
 * that the GATE wrapper for the multilingual and domain-sensitive temporal 
 * tagger HeidelTime can be used with news-style documents on a whole corpus
 * without manually providing document creation times for each document.
 * (for details, see https://github.com/HeidelTime/heideltime/)
 * 
 *     
 * Implementation details:
 * - TimeML format can be read 
 *   (<DCT><TIMEX3 functionInDocument="CREATION_TIME" value="2016-03-16">...<\/TIMEX3><\/DCT>)
 * - document creation time can be provided manually
 * 
 * @author Jannik Strötgen 
 * @version 1.0
 * 
*/ 
@SuppressWarnings("serial")
@CreoleResource(name = "DCTParser", 
        comment = "DCTParser finds DCTs so that HeidelTime GATE wrapper can be run on news-style corpora with differing DCTs.", 
        helpURL = "https://github.com/HeidelTime/heideltime/")
public class DCTParser extends gate.creole.AbstractLanguageAnalyser {
	
	private static Logger logger = Logger.getLogger("DCTParser");
	  
	@Override
	/**
	 * Each document is processed now, either with HeidelTime's 
	 * standalone (i.e., adding sentence, token with pos, Timex annotations)
	 * or adding only Timex annotations and relying on existing annotations for
	 * sentence and token with pos.
	 */
	public void execute() throws ExecutionException {

		// check inputASName
		if(inputASName != null && inputASName.equals("")) inputASName = null;
		AnnotationSet inputAS = (inputASName == null) ?
								document.getAnnotations() :
								document.getAnnotations(inputASName);
		
	    // check outputASName
		if(outputASName != null && outputASName.equals("")) outputASName = null;
    	
    	// get the document
		gate.Document doc = getDocument();
		
		//why oh why was this happening?
		//doc.getFeatures().clear();
		
        AnnotationSet outputAS = (outputASName == null) ?
                document.getAnnotations() :
                document.getAnnotations(outputASName);
		
		if (parsingFormat.getName().equals(DctParsingFormat.MANUALDATE.getName())){
			if (creationDate.matches("^\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d$")){
				addDctAnnotation(creationDate, "manually provided DCT", doc, outputAS);
			}
			else{
				// TODO throw
			}
		}
		else if (parsingFormat.getName().equals(DctParsingFormat.TIMEML.getName())){
			
			// get DCT and TIMEX3 annotations of the original documents
			AnnotationSet timexAS = inputAS.get("TIMEX3");
			AnnotationSet dctAS = inputAS.get("DCT");
			
			// get the offset of the DCT tag in the document
	        Long dctBegin = (long) 0;
	        Long dctEnd = (long) 0;
	        
			if(dctAS != null && dctAS.size() > 0){
	
		        List<Annotation> dctList = new ArrayList<Annotation>(dctAS);
		        Iterator<Annotation> dctIter = dctList.iterator();
	
		        // There should be only one DCT, if not, take first one
		        if (dctIter.hasNext()){
		        	Annotation dct = dctIter.next();
		        	dctBegin = dct.getStartNode().getOffset();
		        	dctEnd = dct.getEndNode().getOffset();
		        }
		        else{
		        	// TODO throw
		        }
			}
			else{
				// TODO throw
			}
			
			// get the TIMEX surrounded by DCT tags
		    if(timexAS != null && timexAS.size() > 0){
	
		        List<Annotation> timexesList = new ArrayList<Annotation>(timexAS);
		        Iterator<Annotation> timexesIter = timexesList.iterator();

		        while(timexesIter.hasNext()){
		        	
		            Annotation currentTimex = timexesIter.next();
		            
	            	Long timexBegin = currentTimex.getStartNode().getOffset();
	            	Long timexEnd = currentTimex.getEndNode().getOffset();
	            	
	            	// get the timex inside of DCT tag
	            	if (dctBegin <= timexBegin && dctEnd >= timexEnd){
	            		
	            		String funcInDoc = currentTimex.getFeatures().get("functionInDocument").toString();
	    	            if (funcInDoc.equals("CREATION_TIME")){
	    	            	
	    	            	// store value and string (if possible)
	    	            	String dctValue = currentTimex.getFeatures().get("value").toString();
	    	            	String dctString = "";
							try {
								dctString = doc.getContent().getContent(currentTimex.getStartNode().getOffset(), 
										currentTimex.getEndNode().getOffset()).toString();
								
		        	            logger.log(Level.INFO, "Document creation time found: " + dctString +
		        	            		" with value: " + dctValue);

							} catch (InvalidOffsetException e1) {
								logger.log(Level.WARNING, "No string for DCT detected; value is " + dctValue);
								e1.printStackTrace();
							} 
	
	        	            // add DCT to document, offset over the full document
							addDctAnnotation(dctValue, dctString, doc, outputAS);

	    	            }
	            	}
		        }
		    }
		}
		else{
			// TODO throw
			logger.log(Level.WARNING, "Unknown format selected.");
		}
	}
    
    /**
     * Add DCT annotation with to the document.
     * @param dctValue
     * @param dctString
     * @param doc
     */
    public void addDctAnnotation(String dctValue, String dctString, gate.Document doc, AnnotationSet outputAS){
    	
		
		
		Long begin = (long) 0;
		Long end = doc.getContent().size();

		FeatureMap fm = Factory.newFeatureMap();
		fm.put("dctValue", dctValue);
		fm.put("dctString", dctString);
		fm.put("createdWith", "DCTParser");

		try {
			outputAS.add(begin, end, "DCT", fm);
		} catch (InvalidOffsetException e) {
			logger.log(Level.WARNING, "Cannot create DCT annotation due to invalid offset information.");
			e.printStackTrace();
		}
    }
    
    
    /*
     * Parameter definitions
     */

	@RunTime
	@Optional
	@CreoleParameter(comment="The annotation set to be used as output for DCT annotations")
	public void setOutputASName(String outputASName) {
		this.outputASName = outputASName;
	}
	public String getOutputASName() {
		return this.outputASName;
	}
    
	@Optional
	@RunTime
	@CreoleParameter(comment="The document creation date of the document (YYYY-MM-DD).", defaultValue="2016-03-16")
	public void setManuallySetDct(String creationDate) {
	    this.creationDate = creationDate;
	}
	public String getManuallySetDct() {
	    return creationDate;
	}

	@RunTime
	@CreoleParameter(comment="Format to parse DCT", defaultValue="TIMEML")
	public void setDctParsingFormat(DctParsingFormat parsingFormat) {
	    this.parsingFormat = parsingFormat;
	}
	public DctParsingFormat getDctParsingFormat() {
		  return parsingFormat;
	}
	
	@RunTime
	@Optional
	@CreoleParameter(comment="The annotation set to be used as input that must contain 'Token' and 'Sentence' annotations", 
						defaultValue="Original markups")
	public void setInputASName(String newInputASName) {
		inputASName = newInputASName;
	}
	public String getInputASName() {
		return inputASName;
	}
	
	// parameters
	private String creationDate;
	private DctParsingFormat parsingFormat;
	private String inputASName;
	private String outputASName;
}
