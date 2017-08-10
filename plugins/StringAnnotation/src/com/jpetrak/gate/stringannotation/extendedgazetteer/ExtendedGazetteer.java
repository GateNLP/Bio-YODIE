/*
 * ExtendedGazeteer.java
 * 
 * 
 *
 */
package com.jpetrak.gate.stringannotation.extendedgazetteer;


import com.jpetrak.gate.stringannotation.utils.TextChunk;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;



/**
 * See documentation in the wiki:
 * https://github.com/johann-petrak/gateplugin-stringannotation/wiki/Extended-Gazetteer
 *
 *  @author Johann Petrak
 */
@CreoleResource(
  name = "Extended Gazetteer",
  comment = "Fast, low-memory footprint gazetteer with many additional features",
  icon="shefGazetteer.gif",
  helpURL="https://github.com/johann-petrak/gateplugin-stringannotation/wiki/Extended-Gazetteer"
)
@SuppressWarnings("javadoc")
public class ExtendedGazetteer extends GazetteerBase
{

  /**
   * 
   */
  private static final long serialVersionUID = 9043237658071049098L;

  // constants
  private static boolean debug = false;
  
  
  // *************************************************************************
  // PR Parameters
  // *************************************************************************
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The input annotation set",
  defaultValue = "")
  public void setInputAnnotationSet(String ias) {
    this.inputAnnotationSet = ias;
  }
  public String getInputAnnotationSet() {
    return inputAnnotationSet;
  }
  private String inputAnnotationSet = "";
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The word annotation set type",
  defaultValue = "Token")
  public void setWordAnnotationType(String val) {
    this.wordAnnotationType = val;
  }
  public String getWordAnnotationType() {
    return wordAnnotationType;
  }
  private String wordAnnotationType = "";
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The feature from the word annotation to use as text - if empty, use the document text",
  defaultValue = "")
  public void setTextFeature(String val) {
    this.textFeature = val;
  }
  public String getTextFeature() {
    return textFeature;
  }
  private String textFeature = "";
  
  
  
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The space annotation set type",
  defaultValue = "SpaceToken")
  public void setSpaceAnnotationType(String val) {
    this.spaceAnnotationType = val;
  }
  public String getSpaceAnnotationType() {
    return spaceAnnotationType;
  }
  private String spaceAnnotationType = "";
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The containing annotation set type",
  defaultValue = "")
  public void setContainingAnnotationType(String val) {
    this.containingAnnotationType = val;
  }
  public String getContainingAnnotationType() {
    return containingAnnotationType;
  }
  private String containingAnnotationType = "";
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The splitting annotation set type",
  defaultValue = "Split")
  public void setSplitAnnotationType(String val) {
    this.splitAnnotationType = val;
  }
  public String getSplitAnnotationType() {
    return splitAnnotationType;
  }
  private String splitAnnotationType = "";
  
  
  
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The ouput annotation set",
  defaultValue = "")
  public void setOutputAnnotationSet(String oas) {
    this.outputAnnotationSet = oas;
  }
  public String getOutputAnnotationSet() {
    return outputAnnotationSet;
  }
  private String outputAnnotationSet = "";
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The ouput annotation type, overwrites what is configured in the def file",
  defaultValue = "")
  public void setOutputAnnotationType(String oat) {
    this.outputAnnotationType = oat;
  }
  public String getOutputAnnotationType() {
    return outputAnnotationType;
  }
  private String outputAnnotationType = "";
  
  @CreoleParameter(comment = "Should this gazetteer restrict matches to start only at beginning of words",
    defaultValue = "true")
  @RunTime
  public void setMatchAtWordStartOnly(Boolean yesno) {
    matchAtWordStartOnly = yesno;
  }
  public Boolean getMatchAtWordStartOnly() {
    return matchAtWordStartOnly;
  }
  private boolean matchAtWordStartOnly;
  
  @CreoleParameter(comment = "Should this gazetteer restrict matches to end only at the end of words",
      defaultValue = "true")
    @RunTime
    public void setMatchAtWordEndOnly(Boolean yesno) {
      matchAtWordEndOnly = yesno;
    }
    public Boolean getMatchAtWordEndOnly() {
      return matchAtWordEndOnly;
    }
    private boolean matchAtWordEndOnly;
    
  @CreoleParameter(comment = "Should this gazetteer only match the longest possible match at each offset?",
    defaultValue = "true")
  @RunTime
  public void setLongestMatchOnly(Boolean yesno) {
    longestMatchOnly = yesno;
  }
  public Boolean getLongestMatchOnly() {
    return longestMatchOnly;
  }
  private boolean longestMatchOnly;
    
  
  // ************************************************************************
  // other class fields 
  // ************************************************************************


  // This will be set to the output annotation set during execute.
  AnnotationSet outputAS = null;
  
  
  public ExtendedGazetteer() {
    logger = Logger.getLogger(this.getClass().getName());
  }

   @Override
  public void execute() throws ExecutionException{
    doExecute(document); // delegate so that a subclass can overwrite execute() and still use doExecute
   }

   public void doExecute(Document theDocument) throws ExecutionException {
    interrupted = false;
    //check the input
    if(theDocument == null) {
      throw new ExecutionException(
        "No document to process!"
      );
    }

    AnnotationSet inputAS = null; 
    if(inputAnnotationSet == null ||
       inputAnnotationSet.equals("")) inputAS = theDocument.getAnnotations();
    else inputAS = theDocument.getAnnotations(inputAnnotationSet);

    outputAS = null; 
    if(outputAnnotationSet == null ||
       outputAnnotationSet.equals("")) outputAS = theDocument.getAnnotations();
    else outputAS = theDocument.getAnnotations(outputAnnotationSet);


    AnnotationSet processAnns = null;
    if(wordAnnotationType == null || wordAnnotationType.isEmpty()) {
      throw new GateRuntimeException("Word annotation type must not be empty!");
    } 
    
    if(spaceAnnotationType == null || spaceAnnotationType.isEmpty()) {
      throw new GateRuntimeException("Space annotation type must not be empty!");
    }
    Set<String> typeSet = new HashSet<String>();
    typeSet.add(wordAnnotationType);
    typeSet.add(spaceAnnotationType);
    processAnns = inputAS.get(typeSet);
    
    AnnotationSet containingAnns = null;
    if(containingAnnotationType == null || containingAnnotationType.isEmpty()) {
      // leave the containingAnns null to indicate we do not use containing annotations
    } else {
      containingAnns = inputAS.get(containingAnnotationType);
      //System.out.println("DEBUG: got containing annots: "+containingAnns.size()+" type is "+containingAnnotationType);
    }
    
    AnnotationSet splitAnns = null;
    if(splitAnnotationType == null || splitAnnotationType.isEmpty()) {
      // leave the splitAnns null to indicate we do not use containing annotations
    } else {
      splitAnns = inputAS.get(splitAnnotationType);
      //System.out.println("DEBUG: got split annots: "+splitAnns.size()+" type is "+splitAnnotationType);
      if(splitAnns.size() == 0) {
        splitAnns = null; 
      }
    }
    
    
    fireStatusChanged("Performing look-up in " + theDocument.getName() + "...");

    long endOffset = theDocument.getContent().size();

    // now split the document into chunks if necessary:
    // = for each containing annotation we create a chunk,
    // = each split annotation forces the end of a chunk
    // Each chunk is represented by an instance of TextChunk 
    if(containingAnns == null) {
      if(splitAnns != null) { // we need to do some additional chunking
        List<Annotation> splitAnnsList = Utils.inDocumentOrder(splitAnns);
        long lastOffset = 0;
        for(Annotation splitAnn : splitAnnsList) {
          long splitOffset = splitAnn.getStartNode().getOffset();
          if(splitOffset > lastOffset) {
            doAnnotateChunk(TextChunk.makeChunk(
                document,lastOffset,splitOffset,!caseSensitive,
                processAnns,wordAnnotationType,textFeature,spaceAnnotationType,
                matchAtWordStartOnly,matchAtWordEndOnly));
          }
          lastOffset = splitOffset;
        } // for
        // anything left?
        if(lastOffset < endOffset) {
          doAnnotateChunk(TextChunk.makeChunk(document,lastOffset,endOffset,!caseSensitive,
              processAnns,wordAnnotationType,textFeature,spaceAnnotationType,
              matchAtWordStartOnly,matchAtWordEndOnly));
        }
      } else {
        // create a chunk from the whole document
        doAnnotateChunk(TextChunk.makeChunk(document,0,endOffset,!caseSensitive,
            processAnns,wordAnnotationType,textFeature,spaceAnnotationType,
            matchAtWordStartOnly,matchAtWordEndOnly));
      }
    } else {
      for(Annotation containingAnn : containingAnns) {
        //System.out.println("processing containing annot "+containingAnn);
        // if we do have split annotations and we have split annotations within the range
        // of this containing annotation, we need to do further chunking
        if(splitAnns != null) {
          AnnotationSet containedSplits = Utils.getContainedAnnotations(splitAnns, containingAnn);
          if(containedSplits.size() > 0) {
            // we need to split
            
            
            List<Annotation> splitAnnsList = Utils.inDocumentOrder(containedSplits);
            long lastOffset = containingAnn.getStartNode().getOffset();
            endOffset = containingAnn.getEndNode().getOffset();
            for(Annotation splitAnn : splitAnnsList) {
              long splitOffset = splitAnn.getStartNode().getOffset();
              if(splitOffset > lastOffset) {
                doAnnotateChunk(TextChunk.makeChunk(
                    document,lastOffset,splitOffset,!caseSensitive,
                    processAnns,wordAnnotationType,textFeature,spaceAnnotationType,
                    matchAtWordStartOnly,matchAtWordEndOnly));
              }
              lastOffset = splitOffset;
            } // for
            // anything left?
            if(lastOffset < endOffset) {
             doAnnotateChunk(TextChunk.makeChunk(
                 document,lastOffset,endOffset,!caseSensitive,
                 processAnns,wordAnnotationType,textFeature,spaceAnnotationType,
                 matchAtWordStartOnly,matchAtWordEndOnly));
            }
            
            
            
          } else {
            // nothing within this containining annotation, just annotate the whole chunk
            doAnnotateChunk(TextChunk.makeChunk(
                document,containingAnn,!caseSensitive,
                processAnns,wordAnnotationType,textFeature,spaceAnnotationType,
                matchAtWordStartOnly,matchAtWordEndOnly));
          }
        } else {
          // no splits, just annotate the chunk for this containing annotation
          doAnnotateChunk(TextChunk.makeChunk(document,containingAnn,!caseSensitive,
              processAnns,wordAnnotationType,textFeature,spaceAnnotationType,
              matchAtWordStartOnly,matchAtWordEndOnly));
        }
      }
    }

    fireProcessFinished();
    fireStatusChanged("Look-up complete!");
  } // execute

   public void doAnnotateChunk(
       TextChunk chunk)
     throws ExecutionException{
    interrupted = false;
    int length = chunk.getLength();
    char currentChar;
    State currentState = gazStore.getInitialState();
    State nextState;
    State lastMatchingState = null;
    int matchedRegionEnd = 0;
    int matchedRegionStart = 0;
    int charIdx = 0;
    int oldCharIdx = 0;

    if(debug) {
      System.out.println("Annotating chunk: "+chunk);
    }
    
    if(chunk.isEmpty()) {
      return;
    }
    
    // TODO: here and below: always skip to the next position where a match may
    // start, since we will just always mark all positions as "isMatchStart" instead 
    // of "isWordStart".
    //if(matchAtWordStartOnly) {
      // skip to the first wordstart
      while(charIdx < length) {
        if(chunk.isValidMatchStart(charIdx)) {
          break;
        }
        charIdx++;
      }
    //}
    while(charIdx < length) {
      // the character we get here is case normalized if necessary!
      currentChar = chunk.getCharAt(charIdx);
      currentChar = caseSensitive.booleanValue() ?
          currentChar :
          Character.toUpperCase(currentChar);
      nextState = currentState.next(currentChar);
      if(nextState == null) {
        //the matching stopped
        //if we had a successful match then act on it;
        if(lastMatchingState != null){
          createLookups(chunk,lastMatchingState,matchedRegionStart,matchedRegionEnd);
          lastMatchingState = null;
        }
        //reset the GazStoreTrie1 and skip to next candidate position - either next char or next
        // char where a word starts
        //if(matchAtWordStartOnly) {
          // skip to the first wordstart
          charIdx = matchedRegionStart + 1;
          while(charIdx < length) {
            if(chunk.isValidMatchStart(charIdx)) {
              break;
            }
            charIdx++;
          }
        //} else {        
        //  charIdx = matchedRegionStart + 1;
        //}
        matchedRegionStart = charIdx;
        currentState = gazStore.getInitialState();
      } else{//go on with the matching
        currentState = nextState;
        // if we have a successful state, i.e. an end state:
        // if we restrict the match to start or end of words, check if this is true too!
        /*
        if(currentState.isFinal()) {
          System.out.println("Found a final state at "+charIdx);       
          System.out.println("Matched region start="+matchedRegionStart);
          System.out.println("Matched region start char="+chunk.getCharAt(matchedRegionStart));
          System.out.println("Matched region start is start="+chunk.isWordStart(matchedRegionStart));
          System.out.println("Matched region end="+matchedRegionEnd);
          System.out.println("Matched region end char="+chunk.getCharAt(charIdx));
          System.out.println("Matched region end is end="+chunk.isWordEnd(charIdx));
        }
        */
        if(currentState.isFinal() &&
            //(!matchAtWordStartOnly || chunk.isValidMatchStart(matchedRegionStart)) &&
            //(!matchAtWordEndOnly || chunk.isValidMatchEnd(charIdx))
            chunk.isValidMatchStart(matchedRegionStart) &&
            chunk.isValidMatchEnd(charIdx)
            ) {
          //System.out.println("Final state and wordboundaries ok at "+charIdx);
          // we have a match
          // if there is a previous matching state to act upon and we do not
          // just annotate the longest match, then annotate that previous
          // match before updating the last matching state.
          if(!longestMatchOnly && lastMatchingState != null){
            createLookups(chunk,lastMatchingState,matchedRegionStart,matchedRegionEnd); 
          } 
          matchedRegionEnd = charIdx;
          lastMatchingState = currentState; 
        } // is final
        charIdx ++;
        if(charIdx == chunk.getLength()){
          //System.out.println("At end of chunk");
          //we can't go on, use the last matching state and restart matching
          //from the next char
          if(lastMatchingState != null){
            //let's add the new annotation(s)
            createLookups(chunk,lastMatchingState,matchedRegionStart,matchedRegionEnd);
            lastMatchingState = null;
          }
          //reset the GazStoreTrie1
          //if(matchAtWordStartOnly) {
            // skip to the first wordstart
            charIdx = matchedRegionStart + 1;
            while(charIdx < length) {
              if(chunk.isValidMatchStart(charIdx)) {
                break;
              }
              charIdx++;
            }
          //} else {        
          //  charIdx = matchedRegionStart + 1;
          //}
          //System.out.println("Skipped forward to "+charIdx);
          matchedRegionStart = charIdx;
          currentState = gazStore.getInitialState();
        }
      }
      //fire the progress event
      if(charIdx - oldCharIdx > 256) {
        fireProgressChanged((100 * charIdx )/ length );
        oldCharIdx = charIdx;
        if(isInterrupted()) throw new ExecutionInterruptedException(
            "The execution of the " + getName() +
            " gazetteer has been abruptly interrupted!");
      }
    } // while(charIdx < length)
    //we've finished. If we had a stored match, then apply it.
    if(lastMatchingState != null) {
      createLookups(chunk,lastMatchingState,matchedRegionStart,matchedRegionEnd); 
    }
    fireProcessFinished();
    fireStatusChanged("Look-up complete!");
  } // execute
  
  
  
  
  protected void createLookups(TextChunk chunk,State matchingState, 
      int matchedRegionStart, int matchedRegionEnd)
  {
    Iterator<Lookup> lookupIter = gazStore.getLookups(matchingState);
    if(!lookupIter.hasNext()) {
      return;
    }
    
    while(lookupIter.hasNext()) {
      Lookup currentLookup = lookupIter.next();
      FeatureMap fm = Factory.newFeatureMap();
      gazStore.addLookupListFeatures(fm, currentLookup);
      fm.put("_listnr",gazStore.getListInfoIndex(currentLookup));
      gazStore.addLookupEntryFeatures(fm, currentLookup);
      // added features for ExtendedGazetteer
      fm.put("_firstcharCategory", Character.getType(chunk.getCharAt(matchedRegionStart)));
      if (Character.isUpperCase(chunk.getCharAt(matchedRegionStart))) {
        fm.put("_firstcharUpper", true);
      } else {
        fm.put("_firstcharUpper", false);
      }
      fm.put("_string", chunk.getTextString(matchedRegionStart, matchedRegionEnd));

      // addEntryFeatures(fm,currentLookup.entryFeatures);
      Integer lookupid =        
        addAnAnnotation(
            chunk.getStartOffset(matchedRegionStart), 
            chunk.getEndOffset(matchedRegionEnd),
            getAnnotationTypeName(gazStore.getLookupType(currentLookup)),
            fm);

    }//while(lookupIter.hasNext())
  }


  // helper method that adds an annotation to an annotation set and if the
  // annotationset is a AnnotationSetImpl (currently all are, but who knows ...),
  // returns the id of the annotation, otherwise null.
  // This allows to link prefix, suffix and lookup annotations by their IDs
  protected Integer addAnAnnotation(int from, int to, String type, FeatureMap fm) {
    if(debug) {
      //System.out.println("Trying to add annotation from "+from+" to "+to+" fm="+fm);
    }
    Integer id = null;
    if (outputAS instanceof AnnotationSetImpl) {
      AnnotationSetImpl setasannimpl = (AnnotationSetImpl) outputAS;
      try {
        id = setasannimpl.add(new Long(from), new Long(to+1), type, fm);
      } catch (InvalidOffsetException ex) {
        throw new GateRuntimeException("Invalid offset exception - doclen/from/to="
          + document.getContent().size() + "/" + from + "/" + to, ex);
      }
    } else {
      try {
        outputAS.add(new Long(from), new Long(to+1), type, fm);
      } catch (InvalidOffsetException ex) {
        throw new GateRuntimeException("Invalid offset exception - doclen/from/to="
          + document.getContent().size() + "/" + from + "/" + to + " / ", ex);
      }
    }
    return id;
  }




  // in the program, we use only this method to find the annotation type
  // name to assign: if the annotation type is set as a runtime parameter,
  // always use that, otherwise use the one we get from the gazetteer list
  // specification in the def file.
  public String getAnnotationTypeName(String defaultType) {
    if(outputAnnotationType == null || outputAnnotationType.isEmpty()) {
      return defaultType;
    } else {
      return outputAnnotationType;
    }
  }
  
  
  // TODO: add some API methods to make the gazetteer useful in other situations, e.g. when
  // there is a need to match strings, find longest prefixes etc.
  // In general, all matching methods will need to return a collection or iterator of matches
  // (since even a single string can potentially be matched by many entries) and for each 
  // match we get information about the list (e.g. the list name, number, list-associated annotation type),
  // list specific features, and entry specific features.
  // For now, the internal datastructure does not actually return any of these but rather
  // a placeholder that can be used to e.g. add features to a feature set based on the the
  // lookup the placeholder stands for (this avoids the creation of objects that will get
  // thrown away anyways). For a more generic approach this should get wrapped up into some kind
  // of visitor class, so that the information can be used in whatever way necessary by the client.
  // The visitor class is a stateful class similar to a java matcher class: there are several
  // methods to make the visitor class attempt a match (a match can be attempted in several ways e.g.
  // just at some position of a string, find the next position that matches, find just the longest matches
  // etc). After an attempt, a method can be
  // called to check if the attempt was successful (maybe make it conform to Groovy truthiness?).
  // If a match was successful, there may be several methods to find or process all the matches.
  
  
  
  

} // ExtendedGazetteer

