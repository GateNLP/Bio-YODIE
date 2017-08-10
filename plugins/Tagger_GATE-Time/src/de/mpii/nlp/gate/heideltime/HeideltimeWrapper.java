/*
 *  HeideltimeWrapper.java
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
import gate.annotation.NodeImpl;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.Files;
import gate.util.OffsetComparator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.thoughtworks.xstream.InitializationException;

import de.unihd.dbs.heideltime.standalone.CLISwitch;
import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.components.JCasFactory;
import de.unihd.dbs.heideltime.standalone.components.impl.JCasFactoryImpl;
import de.unihd.dbs.heideltime.standalone.components.impl.UimaContextImpl;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Token;


/** 
 * This class is the implementation of the resource TAGGER_HEIDELTIME,
 * a GATE wrapper for the multilingual and domain-sensitive temporal 
 * tagger HeidelTIme 
 * (for details, see https://github.com/HeidelTime/heideltime/)
 * 
 * This wrapper can be used in two ways:
 * (1) creating sentence, token, and pos annotations in addition to TIMEX3 
 *     annotations. For this the TreeTagger (for most languages) and the 
 *     other taggers (for some languages) have to be installed and configured
 *     in config.props. Details how to get the taggers can be found here:
 *     https://github.com/HeidelTime/heideltime/
 * (2) creating only TIMEX3 annotations and relying on exisitng GATE annotations
 *     for sentence, token, and pos annotations. For instance, the ANNIE tokenizer,
 *     sentence splitter, and POS tagger can be used (at least for English). For 
 *     this procedure, only the HeidelTime standalone jar, the config.props, (and 
 *     uima-core) have to be available.
 *     
 * Implementation details:
 * If token, sentence, pos annotations are provided, HeidelTime standalone cannot be 
 * used directly and existing annotations are added to a UIMA jcas object, this is 
 * then processed by HeidelTime.
 * 
 * @author Jannik Strötgen 
 * @version 1.0
 * 
*/ 
@SuppressWarnings("serial")
@CreoleResource(name = "HeidelTime", 
        comment = "HeidelTime GATE wrapper, i.e., HeidelTime plugin for gate.", 
        helpURL = "https://github.com/HeidelTime/heideltime/")
public class HeideltimeWrapper extends gate.creole.AbstractLanguageAnalyser {
	
	private HeidelTimeStandalone hts;
		
	private JCasFactory jcasFactory;
	private HeidelTime heidelTime;
	private static Logger logger = Logger.getLogger("HeidelTimeWrapper");
	
  private static class NoExitSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission p) {
      // allow anything
    }

    @Override
    public void checkPermission(Permission p, Object context) {
      // allow anything.
    }

    @Override
    public void checkExit(int status) {
      super.checkExit(status);
      throw new ExitException(status);
    }

  }
	
	protected static class ExitException extends SecurityException {
		
		public final int status;
		
	    public ExitException(int status) {
	          super("System exit!");
	          this.status = status;
	    }
	    
	}
    
    
	/**
	 * Initialization of the HeidelTimeGateWrapper. It is called when 
	 * HeidelTime is loaded as processing resource in GATE, and the 
	 * following parameters have to be set:
	 * -- language
	 * -- domain
	 * -- sentenceAnnotationType
	 * -- tokenAnnotationType
	 * -- posAnnotationAsTokenAttribute
	 * The document creation time has to be set separately.
	 */
	@Override
	public Resource init() throws InitializationException{

    // TODO fix HeidelTime so it doesn't do System.exit() other than in the main
    // method so we can call it without worrying about the JVM closing with no
    // visible error messages. Once that's done we can remove this rather
    // horrendous hack.
	  SecurityManager securityManager = null;
		try {
  	  securityManager = System.getSecurityManager();
		  System.setSecurityManager(new NoExitSecurityManager());
  		
  		// without this initialization, calling jcas factory fails
  		hts = new HeidelTimeStandalone(language, documentType, OutputType.XMI, 
  				Files.fileFromURL(configFile).getAbsolutePath());
			
  		// create a UIMA jcas factory
  		try {
  			heidelTime = new HeidelTime();
  			heidelTime.initialize(new UimaContextImpl(language, documentType, CLISwitch.VERBOSITY2.getIsActive()));
  			logger.log(Level.INFO, "HeidelTime initialized");
  		} catch (Exception e) {
  			e.printStackTrace();
  			logger.log(Level.WARNING, "HeidelTime could not be initialized");
  			throw new InitializationException("HeidelTimeWrapper could not be initialized. " +
  					"Something wrong with the setup?");
  		}
  		
  		try {
  			TypeSystemDescription[] descriptions = new TypeSystemDescription[] {
  					UIMAFramework
  					.getXMLParser()
  					.parseTypeSystemDescription(
  							new XMLInputSource(
  									this.getClass()
  									.getClassLoader()
  									.getResource(
  											Config.get(Config.TYPESYSTEMHOME)))) };
  			jcasFactory = new JCasFactoryImpl(descriptions);
  		} catch (Exception e) {
  			e.printStackTrace();
  			logger.log(Level.WARNING, "JCas factory could not be initialized");
  			throw new InitializationException("HeidelTimeWrapper could not be initialized. " +
  					"Something wrong with the setup?");
  		}
  		return this;
		}
    finally {
      System.setSecurityManager(securityManager);
    }
	}
    
    
    @Override
	public void cleanup() {
		
	    System.setSecurityManager(null);
	    
	}
	  
	@Override
	/**
	 * Each document is processed now, either with HeidelTime's 
	 * standalone (i.e., adding sentence, token with pos, Timex annotations)
	 * or adding only Timex annotations and relying on existing annotations for
	 * sentence and token with pos.
	 */
	public void execute() throws ExecutionException {

		//check the parameters
	    if(document == null) throw new ExecutionException(
	      "No document to process!");
	    
    	// check preprocessing parameters
		if (doPreprocessing == false){

			if ( tokenAnnotation.equals("") || 
					sentenceAnnotation.equals("") ||
					posAnnotation.equals("")){
				
				throw new ExecutionException("\nIf preprocessing shall not be performed by the HeidelTimeWrapper " +
						"and existing, existing annotations have to be provided. Names have to be specified as " +
						"parameters (e.g., ANNIE's sentence, token, and pos annotations are 'Sentence', 'Token', " +
						"and 'category').\n" + 
						"If preprocessing shall be performed by the HeidelTimeWrapper, " +
						"set doPreprocessing to 'true'.\n\n");
			}
		}
		else{
			// if names of annotations are given although preprocessing shall be performed,
			// ignore the names
			if (!( tokenAnnotation.equals("") && 
					sentenceAnnotation.equals("") &&
					posAnnotation.equals("") )){
				logger.log(Level.WARNING, "Preprocessing will be performed by HeidelTime wrapper.\n" +
										  "Given parameters for token, sentence, pos annotations will be ignored.");
			}
		}
		
		// check inputASName
		if(inputASName != null && inputASName.equals("")) inputASName = null;
		AnnotationSet inputAS = (inputASName == null) ?
								document.getAnnotations() :
								document.getAnnotations(inputASName);
	    		
	    // check outputASName
		if(outputASName != null && outputASName.equals("")) outputASName = null;
	                            
 		// get the document cration time as specified by DCTParser
		AnnotationSet dctAS = inputAS.get(dctAnnotation);
		String dctValue = "";
		
	    if(dctAS != null && dctAS.size() > 0){
	    	
	        List<Annotation> dctList = new ArrayList<Annotation>(dctAS);
	        Iterator<Annotation> dctIter = dctList.iterator();

	        // There should be only one DCT, if not, take first one
	        if (dctIter.hasNext()){
	        	Annotation dct = dctIter.next();
	        	dctValue = dct.getFeatures().get("dctValue").toString();
	        }
		    else{
		    	// Document creation time is missing
		    	if (documentType == DocumentType.NEWS) {
		    		// But should be provided in case of news-document
		    		throw new ExecutionException("Document creation time not correctly provided. As domain " +
		    				"to be processed is 'news', document creation time is required.");
		    	}
		    	if (documentType == DocumentType.COLLOQUIAL) {
		    		// But should be provided in case of colloquial-document
		    		throw new ExecutionException("Document creation time not correctly provided. As domain " +
		    				"to be processed is 'colloquial', document creation time is required.");		    	}
		    }
	    }
	    else{
	    	// Document creation time is missing
	    	if (documentType == DocumentType.NEWS) {
	    		// But should be provided in case of news-document
	    		throw new ExecutionException("Document creation time not correctly provided. As domain " +
	    				"to be processed is 'news', document creation time is required.");
	    	}
	    	if (documentType == DocumentType.COLLOQUIAL) {
	    		// But should be provided in case of colloquial-document
	    		throw new ExecutionException("Document creation time not correctly provided. As domain " +
	    				"to be processed is 'colloquial', document creation time is required.");		    	
	    	}
	    }

		gate.Document doc = getDocument();
		
		//why oh why was this being done
		doc.getFeatures().clear();
		
		String output = "";
		Document d = null;
		
		// run HeidelTime standalone, add Timex, Sentence, Token with pos annotations
		if (doPreprocessing) {
			try {
				
				// for HeidelTime standalone, a Date object is required
				DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );
				try{
					Date date = formatter.parse(dctValue);
	
					// running HeidelTime
					output = hts.process(doc.getContent().toString(), date);
					InputStream in = new ByteArrayInputStream(output.getBytes("UTF-8"));
					
					// to parse HeidelTime's xml output (UIMA XMI format)
					DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					d = db.parse(in);
				}
				catch (Exception e){
					logger.log(Level.INFO, "Checking if HeidelTime can be run without DCT...");
					if (documentType.equals(DocumentType.NARRATIVES)){
						logger.log(Level.INFO, "NARRATIVES detected, procssing without DCT possible.");
						output = hts.process(doc.getContent().toString());
						InputStream in = new ByteArrayInputStream(output.getBytes("UTF-8"));
						
						// to parse HeidelTime's xml output (UIMA XMI format)
						DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						d = db.parse(in);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new ExecutionException("Something wrong calling HeidelTime standalone");
			}
			
			// add all the annotations
			AnnotationSet outputAS = (outputASName == null) ?
					document.getAnnotations() :
					document.getAnnotations(outputASName);
			
			// get the annotations
			NodeList nlSentences = d.getElementsByTagName("heideltime:Sentence");
			NodeList nlTokens = d.getElementsByTagName("heideltime:Token");
			NodeList nlTimexes = d.getElementsByTagName("heideltime:Timex3");
			
			// sentences
			for(int i = 0; i < nlSentences.getLength(); i++) {

				NamedNodeMap attributes = nlSentences.item(i).getAttributes();
				
				// attributes id, begin, end
				Node nodeId = attributes.getNamedItem("xmi:id");
				int id = Integer.parseInt(nodeId.getTextContent());
				String sBegin = attributes.getNamedItem("begin").getTextContent();
				String sEnd = attributes.getNamedItem("end").getTextContent();
				NodeImpl begin = new NodeImpl(id, Long.parseLong(sBegin));
				NodeImpl end = new NodeImpl(id, Long.parseLong(sEnd));

				// add features
				FeatureMap fm = Factory.newFeatureMap();
				String sSentId = attributes.getNamedItem("sentenceId").getTextContent();
				fm.put("sentenceId", sSentId);
				fm.put("createdWith", "HeidelTime GATE wrapper");
				
				// add to annotations
				outputAS.add(begin, end, "Sentence", fm);
			}
			
			// tokens
			for(int i = 0; i < nlTokens.getLength(); i++) {
				
				NamedNodeMap attributes = nlTokens.item(i).getAttributes();
				
				// attributes id, begin, end
				Node nodeId = attributes.getNamedItem("xmi:id");
				int id = Integer.parseInt(nodeId.getTextContent());
				String sBegin = attributes.getNamedItem("begin").getTextContent();
				String sEnd = attributes.getNamedItem("end").getTextContent();
				NodeImpl begin = new NodeImpl(id, Long.parseLong(sBegin));
				NodeImpl end = new NodeImpl(id, Long.parseLong(sEnd));

				// add features
				FeatureMap fm = Factory.newFeatureMap();
				String sTokenId =attributes.getNamedItem("tokenId").getTextContent();
				fm.put("tokenId", sTokenId);
				String sPos = attributes.getNamedItem("pos").getTextContent();
				fm.put("pos", sPos);
				fm.put("createdWith", "HeidelTime GATE wrapper");
				
				// add to annotations
				outputAS.add(begin, end, "Token", fm);
			}
			
			// timexes
			for(int i = 0; i < nlTimexes.getLength(); i++) {
				
				NamedNodeMap attributes = nlTimexes.item(i).getAttributes();
				
				// attributes id, begin, end
				Node nodeId = attributes.getNamedItem("xmi:id");
				int id = Integer.parseInt(nodeId.getTextContent());
				String sBegin = attributes.getNamedItem("begin").getTextContent();
				String sEnd = attributes.getNamedItem("end").getTextContent();
				NodeImpl begin = new NodeImpl(id, Long.parseLong(sBegin));
				NodeImpl end = new NodeImpl(id, Long.parseLong(sEnd));
				
				// add features
				FeatureMap fm = Factory.newFeatureMap();
				String sType = attributes.getNamedItem("timexType").getTextContent();
				fm.put("timexType", sType);
				String sTimexId = attributes.getNamedItem("timexId").getTextContent();
				fm.put("timexId", sTimexId);
				String sTimexValue = attributes.getNamedItem("timexValue").getTextContent();
				fm.put("timexValue", sTimexValue);
				String sFoundByRule = attributes.getNamedItem("foundByRule").getTextContent();
				fm.put("foundByRule", sFoundByRule);
				String sTimexQuant = attributes.getNamedItem("timexQuant").getTextContent();
				fm.put("timexQuant", sTimexQuant);
				String sTimexFreq = attributes.getNamedItem("timexFreq").getTextContent();
				fm.put("timexFreq", sTimexFreq);
				String sTimexMod = attributes.getNamedItem("timexMod").getTextContent();
				fm.put("timexMod", sTimexMod);
				fm.put("createdWith", "HeidelTime GATE wrapper");
				
				// add to annotations
				outputAS.add(begin, end, "TIMEX3", fm);
			}
		}
		// now: use existing preprocessing annotations
		else { 
//			if (!(tokenAnnotation.equals("")) ||
//			!(sentenceAnnotation.equals("")) ||
//			!(posAnnotation.equals(""))) {

			// create a jcas for the document
			JCas jcas = null;
			try {
				jcas = jcasFactory.createJCas();
				jcas.setDocumentText(doc.getContent().toString());
				logger.log(Level.FINE, "CAS object generated");
			} catch (Exception e) {
				e.printStackTrace();
				logger.log(Level.WARNING, "Cas object could not be generated");
			}
			
			// get sentence, token (with pos) annotations
			// with names as specified in the parameters
			AnnotationSet sentencesAS = inputAS.get(sentenceAnnotation);
			AnnotationSet tokensAS = inputAS.get(tokenAnnotation);
			
			try{
			    if(sentencesAS != null && sentencesAS.size() > 0
			    	       && tokensAS != null && tokensAS.size() > 0){
			    	
			        //prepare the input for HeidelTime
			        List<String> sentenceForTagger = new ArrayList<String>();
			        List<List<String>> sentencesForTagger = new ArrayList<List<String>>(1);
			        sentencesForTagger.add(sentenceForTagger);
	
			        // define a comparator for annotations by start offset
			        Comparator<Annotation> offsetComparator = new OffsetComparator();
	
			        //read all the tokens and all the sentences
			        List<Annotation> sentencesList = new ArrayList<Annotation>(sentencesAS);
			        Collections.sort(sentencesList, offsetComparator);
			        List<Annotation> tokensList = new ArrayList<Annotation>(tokensAS);
			        Collections.sort(tokensList, offsetComparator);
	
			        // create iterators
			        Iterator<Annotation> sentencesIter = sentencesList.iterator();
			        Iterator<Annotation> tokensIter = tokensList.iterator();
			        
			        while(sentencesIter.hasNext()){
			            Annotation currentSentence = sentencesIter.next();
			            
			            // creating UIMA Annotations for sentences and add them to the CAS
			            Sentence sent = new Sentence(jcas);
			            sent.setBegin(currentSentence.getStartNode().getOffset().intValue());
			            sent.setEnd(currentSentence.getEndNode().getOffset().intValue());
			            sent.addToIndexes(jcas);
			        }
			        while(tokensIter.hasNext()){
			            Annotation currentToken = tokensIter.next();
	
			            // creating UIMA Annotations for tokens (with pos) and add them to the CAS
			            Token tok = new Token(jcas);
			            tok.setBegin(currentToken.getStartNode().getOffset().intValue());
			            tok.setEnd(currentToken.getEndNode().getOffset().intValue());
			            tok.setPos(currentToken.getFeatures().get(posAnnotation).toString());
			            tok.addToIndexes(jcas);
			        }
			        logger.log(Level.INFO, "Process existing annotations for HeidelTime ... done!");
			        
			        // run HeidelTime based on existing sentence, token, and part-of-speech information
						try {
							logger.log(Level.FINER, "Checking preconditions to run HeidelTime...");
		
							provideDocumentCreationTime(jcas, dctValue);
							logger.log(Level.FINER, "Preconditions checked. Start pocessing with HeidelTime");
		
							heidelTime.process(jcas);
							
							// iterate over Timex3 annotations in the jcas
							FSIndex<Timex3> indexTimex   = jcas.getAnnotationIndex(Timex3.type);
							FSIterator<Timex3> iterTimex = indexTimex.iterator();
							
							while (iterTimex.hasNext()) {
								Timex3 t = iterTimex.next();
								
								// create annotation set for each UIMA TIMEX3
						        AnnotationSet outputAS = (outputASName == null) ?
						                  document.getAnnotations() :
						                  document.getAnnotations(outputASName);
								
								// attribute id, begin and end
								int id = t.getAddress();
								String stringBegin = t.getBegin() + "";
								NodeImpl begin = new NodeImpl(id, Long.parseLong(stringBegin));
								String stringEnd = t.getEnd() + "";
								NodeImpl end = new NodeImpl(id, Long.parseLong(stringEnd));
								
								// add features
								FeatureMap fm = Factory.newFeatureMap();
								fm.put("timexType", t.getTimexType());
								fm.put("timexId", t.getTimexId());
								fm.put("timexValue", t.getTimexValue());
								fm.put("foundByRule", t.getFoundByRule());
								fm.put("timexQuant", t.getTimexQuant());
								fm.put("timexFreq", t.getTimexFreq());
								fm.put("timexMod", t.getTimexMod());
								
								// add to annotations
								outputAS.add(begin, end, "TIMEX3", fm);
							}
							logger.log(Level.INFO, "Processing HeidelTime finished");
						} catch (Exception e) {
						e.printStackTrace();
						logger.log(Level.WARNING, "Processing aborted due to errors");
					}
			    }
			    // now: preprocessing annotations shall be used, but are not found!
			    else{
	
			    	throw new ExecutionException("\nNo sentence and/or token annotations " +
			    			"(including pos information) have been found with provided names.\n" + 
			    			"Looked for sentences called: " + sentenceAnnotation +
			    			"\nLooked for tokens called: " + tokenAnnotation +
			    			"\nLooked for token attribute for pos information called: " + posAnnotation +
			    			"\nPlease provide correct annotation names.");
			    }
			}catch (ExecutionException e){
		    	throw new ExecutionException("\nNo sentence and/or token annotations " +
		    			"(including pos information) have been found with provided names.\n" + 
		    			"Looked for sentences called: " + sentenceAnnotation +
		    			"\nLooked for tokens called: " + tokenAnnotation +
		    			"\nLooked for token attribute for pos information called: " + posAnnotation +
		    			"\nPlease provide correct annotation names." +
		    			"\nProbably the pos feature name is wrong.");
		    }
		}
	}
    
    
    
	/**
	 * Provides jcas object with document creation time if
	 * <code>documentCreationTime</code> is not null.
	 * 
	 * @param jcas
	 * @param documentCreationTime
	 * @throws DocumentCreationTimeMissingException
	 *             If document creation time is missing when processing a
	 *             document of type {@link DocumentType#NEWS}.
	 */
	private void provideDocumentCreationTime(JCas jcas,
			String documentCreationTime)
			throws DocumentCreationTimeMissingException {
		if (documentCreationTime == null) {
			// Document creation time is missing
			if (documentType == DocumentType.NEWS) {
				// But should be provided in case of news-document
				throw new DocumentCreationTimeMissingException();
			}
			if (documentType == DocumentType.COLLOQUIAL) {
				// But should be provided in case of colloquial-document
				throw new DocumentCreationTimeMissingException();
			}
		} else {
			// Document creation time provided
			
			if (documentCreationTime.matches("\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d")){

				// Create dct object for jcas
				Dct dct = new Dct(jcas);
				dct.setValue(documentCreationTime);
				System.err.println("FORMATTED DCT: " + documentCreationTime);
				dct.addToIndexes();
			}
			else{
				logger.log(Level.WARNING, "No correctly formatted DCT provided.");
			}
		}
	}
    
    
    /*
     * Parameter definitions
     */
 	
	@CreoleParameter(comment="The language of the documents that are to be processed", defaultValue="ENGLISH")
	public void setLanguage(Language language) {
		this.language = language;
	}
	public Language getLanguage() {
	    return language;
	}
	
	@CreoleParameter(comment="The name of the domain (HeidelTime domain names))", defaultValue="NEWS")
	public void setDocumentType(DocumentType documentType) {
	    this.documentType = documentType;
	}
	public DocumentType getDocumentType() {
	    return documentType;
	}

	@RunTime
	@CreoleParameter(comment="The document creation date of the document", defaultValue="DCT")
	public void setCreationDateAnnotationType(String dctAnnotation) {
	    this.dctAnnotation = dctAnnotation;
	}
	public String getCreationDateAnnotationType() {
	    return dctAnnotation;
	}
	
	@CreoleParameter(comment="Location of the 'config.props' file (distributed with HeidelTime standalone)", 
	                 defaultValue="config.props")
	public void setConfigFile(URL configFile) {
	    this.configFile = configFile;
	}
	public URL getConfigFile() {
		  return configFile;
	}
	
	@RunTime
	@CreoleParameter(comment="The name of the 'Token' annotation type", defaultValue="Token")
	public void setDoPreprocessing(Boolean doPreprocessing) {
	    this.doPreprocessing = doPreprocessing;
	}
	public Boolean getDoPreprocessing() {
		  return doPreprocessing;
	}
	
	@RunTime
	@CreoleParameter(comment="The name of the 'Token' annotation type", defaultValue="Token")
	public void setTokenAnnotationType(String tokenAnnotation) {
	    this.tokenAnnotation = tokenAnnotation;
	}
	public String getTokenAnnotationType() {
		  return tokenAnnotation;
	}

	@RunTime
	@CreoleParameter(comment="The name of the 'Sentence' annotation type", defaultValue="Sentence")
	public void setSentenceAnnotationType(String sentenceAnnotation) {
	    this.sentenceAnnotation = sentenceAnnotation;
	}
	public String getSentenceAnnotationType() {
		  return sentenceAnnotation;
	}

	@RunTime
	@CreoleParameter(comment="Name of the part-of-speech attribute of the 'Token'", defaultValue="category")
	public void setPosAnnotationNameAsTokenAttribute(String posAnnotation) {
	    this.posAnnotation = posAnnotation;
	}
	public String getPosAnnotationNameAsTokenAttribute() {
		  return posAnnotation;
	}
	  
	@RunTime
	@Optional
	@CreoleParameter(comment="The annotation set to be used as output for 'TIMEX3' annotations")
	public void setOutputASName(String outputASName) {
		this.outputASName = outputASName;
	}
	public String getOutputASName() {
		return this.outputASName;
	}
	
	@RunTime
	@Optional
	@CreoleParameter(comment="The annotation set to be used as input that must contain 'DCT', 'Token' (with 'pos'), 'Sentence' annotations")
	public void setInputASName(String newInputASName) {
		inputASName = newInputASName;
	}
	public String getInputASName() {
		return inputASName;
	}
	
	// parameters
	private String inputASName;
	private String outputASName;
	private Language language;
	private DocumentType documentType;
	private URL configFile;
	private String dctAnnotation;
	private Boolean doPreprocessing;
	private String tokenAnnotation;
	private String sentenceAnnotation;
	private String posAnnotation;
}
