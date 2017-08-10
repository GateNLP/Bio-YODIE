/*
 *  JavaRegexpAnnotator.java
 *
 *  $Id: JavaRegexpAnnotator.java  $
 *
 */
package com.jpetrak.gate.stringannotation.regexp;

import com.jpetrak.gate.stringannotation.utils.TextChunk;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import gate.*;
import gate.creole.*;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.*;
import java.io.BufferedReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.text.*;

// TODO:
// = use TextChunk 
// = runtime parameter containingAnnotationType
// = and wordAnnotationType/textFeature for indirect annotation

/** 
 * Annotator for annotating document text based on Java regular expressions.
 * See documentation in the wiki: 
 * https://github.com/johann-petrak/gateplugin-stringannotation/wiki/Java-Regexp-Annotator
 * 
 * @author Johann Petrak
 */
@CreoleResource(name = "Java Regexp Annotator",
helpURL="https://github.com/johann-petrak/gateplugin-stringannotation/wiki/Java-Regexp-Annotator",
icon="shefGazetteer.gif",
comment = "Create annotations based on Java regular expressions")
public class JavaRegexpAnnotator extends AbstractLanguageAnalyser
  implements ProcessingResource {

  private static final long serialVersionUID = 1L;

  @CreoleParameter(comment = "The URL of the regular expression annotator list")
  public void setPatternFileURL(URL patternfileurl) {
    patternFileURL = patternfileurl;
  }
  public URL getPatternFileURL() {
    return patternFileURL;
  }
  protected URL patternFileURL = null;

  
  @CreoleParameter(comment = "The annotation set where to create the annotations",
     defaultValue = "")
  // @Optional
  @RunTime
  public void setOutputAnnotationSet(String name) {
    outputAnnotationSet = name;
  }
  public String getOutputAnnotationSet() {
    return outputAnnotationSet;
  } 
  protected String outputAnnotationSet = "";
 
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
    comment = "The input annotation set, for the containing and the input annotation types",
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
    comment = "The input annotation type, if indirect annotation is wanted, if empty: no indirect annotation",
  defaultValue = "")
  public void setInputAnnotationType(String val) {
    this.inputAnnotationType = val;
  }
  public String getInputAnnotationType() {
    return inputAnnotationType;
  }
  private String inputAnnotationType = "";
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The space annotation set type, if indirect annotation is wanted, otherwise ignored",
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
    comment = "The feature from the input annotation to use as text for indirect annotation - if empty, use the document text",
  defaultValue = "")
  public void setTextFeature(String val) {
    this.textFeature = val;
  }
  public String getTextFeature() {
    return textFeature;
  }
  private String textFeature = "";
  
  

  
  
  @CreoleParameter(comment = "Should overlapping matches be allowed?", defaultValue = "false")
  @RunTime
  public void setOverlappingMatches(Boolean flag) {
    overlappingMatches = flag;
  }
  public Boolean getOverlappingMatches() {
    return overlappingMatches;
  }
  Boolean overlappingMatches = false;

  @CreoleParameter(comment = "Which matches/rules to prefer, if there are several at some offset", defaultValue = "LONGEST_LASTRULE")
  @RunTime
  public void setMatchPreference(MatchPreference pref) {
    matchPreference = pref;
  }
  public MatchPreference getMatchPreference() {
    return matchPreference;
  }
  MatchPreference matchPreference = MatchPreference.LONGEST_LASTRULE;
  
  @CreoleParameter(comment = "Debugging mode: if true, add debuggin features to created annotations",
    defaultValue = "false")
  @RunTime
  public void setDebugging(Boolean flag) {
    debugging = flag;
  }
  public Boolean getDebugging() {
    return debugging;
  }
  protected Boolean debugging = false;
  
  protected Boolean debugMessages = false;
  
  List<PatternRule> rulesList;
  Pattern ruleStartPattern = 
    Pattern.compile(
    "^\\s*([0-9]+(?:,[0-9]+)*)\\s*=>\\s*(\\w+)(\\s+\\w+=(?:\"[^\"]*\"|\\$[0-9]+)(?:,\\w+=(?:\"[^\"]*\"|\\$[0-9]+))*)?");

  @Override
  public Resource init() throws ResourceInstantiationException {
    super.init();
    //sanity checks
    if (patternFileURL == null) {
      throw new ResourceInstantiationException("No pattern file URL specified!");
    }
    try {
      rulesList = loadRulesList(patternFileURL);
    } catch (UnsupportedEncodingException ex) {
      throw new ResourceInstantiationException(ex);
    } catch (IOException ex) {
      throw new ResourceInstantiationException(ex);
    }
    //System.out.println("SimpleRegexpAnnotator: loaded " + rulesList.size() + " pattern rules from "+patternFileURL);
    return this;
  }

  @Override 
  public void execute() throws ExecutionException {
    doExecute(document);
  }
  
  public void doExecute(Document theDocument) throws ExecutionException {
    interrupted = false;
    int lastProgress = 0;
    fireProgressChanged(lastProgress);
    //get pointers to the annotation sets
    AnnotationSet outputAS = (outputAnnotationSet == null
      || outputAnnotationSet.trim().length() == 0)
      ? theDocument.getAnnotations()
      : theDocument.getAnnotations(outputAnnotationSet);

    AnnotationSet inputAS = null; 
    if(inputAnnotationSet == null ||
       inputAnnotationSet.equals("")) inputAS = theDocument.getAnnotations();
    else inputAS = theDocument.getAnnotations(inputAnnotationSet);    
    
    AnnotationSet containingAnns = null;
    if(containingAnnotationType == null || containingAnnotationType.isEmpty()) {
      // leave the containingAnns null to indicate we do not use containing annotations
    } else {
      containingAnns = inputAS.get(containingAnnotationType);
      //System.out.println("DEBUG: got containing annots: "+containingAnns.size()+" type is "+containingAnnotationType);
    }
    
    // check if an input annotation type is specified. If yes, we want indirect
    // annotation
    boolean indirect = false;
    AnnotationSet processAnns = null;
    if(getInputAnnotationType() != null && !getInputAnnotationType().isEmpty()) {
      indirect = true;
      Set<String> typeSet = new HashSet<String>();
      typeSet.add(getInputAnnotationType());
      typeSet.add(getSpaceAnnotationType());
      processAnns = inputAS.get(typeSet);
    }
    
    // now create the chunks to annotate: if we have a containing annotation, 
    // one chunk for each containing annotation, otherwise just one chunk for
    // the whole document
    TextChunk chunk = null;
    if(containingAnns == null) {
      if(indirect) {
        chunk = TextChunk.makeChunk(document, 0, document.getContent().size(),false,
                processAnns, getInputAnnotationType(), getTextFeature(), getSpaceAnnotationType(), 
                false, false);
        //System.out.println("Created chunk notContaining/indirect: "+chunk);
      } else {
        chunk = TextChunk.makeChunk(document, 0, document.getContent().size());
        //System.out.println("Created chunk notContaining/direct: "+chunk);
      }
      annotateChunk(chunk,outputAS);
    } else {
      for(Annotation containing : containingAnns) {
        if(indirect) {
          chunk = TextChunk.makeChunk(document, containing, false,
                  processAnns, getInputAnnotationType(), getTextFeature(), getSpaceAnnotationType(), 
                  false, false);          
          //System.out.println("Created chunk containing/indirect: "+chunk);
        } else {
          chunk = TextChunk.makeChunk(document,Utils.start(containing),Utils.end(containing));
          //System.out.println("Created chunk containing/direct: "+chunk);
        }
        annotateChunk(chunk,outputAS);
      }
    }
  }
   
  protected void annotateChunk(TextChunk chunk, AnnotationSet outputAS) {
    String docText = chunk.getTextString();

    boolean haveActive = false;
    
    // Initialize all the matchers with the document string and find the first 
    // match, if any
    for(PatternRule rule : rulesList) {
      rule.matcher = rule.pattern.matcher(docText);
      rule.matcher_active = rule.matcher.find();
      if(rule.matcher_active) {
        rule.matcher_length = rule.matcher.end()-rule.matcher.start();
      }
      haveActive = haveActive || rule.matcher_active;
    }
    // if we did not find anything at all, give up already
    if(!haveActive) {
      return;
    }
    
    int curOffset = 0;
    int smallestOffset;
    int longestLength;
    // as long as there is still more document content to match and active
    // matchers, proceed
    while(curOffset < docText.length()) {      
      // for all matchers where the last match was before the current offset,
      // re-find the next match starting from the current offset
      // Matchers where the next match is only later, are left as they are
      if(debugMessages) {
        System.out.println("(Re)trying at offset "+curOffset);
      }
      haveActive = false;
      smallestOffset = docText.length()+1;
      for(PatternRule rule : rulesList) {
        if(rule.matcher_active) {
          if(rule.matcher.start() < curOffset) {
            rule.matcher_active = rule.matcher.find(curOffset);
            if(rule.matcher_active) {
              rule.matcher_length = rule.matcher.end()-rule.matcher.start();
              if(rule.matcher.start() < smallestOffset) {
                smallestOffset = rule.matcher.start();
              }
              haveActive = true;
            } else {
              rule.matcher_length = 0;
            }
          } else {
            if(rule.matcher.start() < smallestOffset) {
              smallestOffset = rule.matcher.start();
            }
            haveActive = true;            
          }
        }
      }
      if(!haveActive) {
        if(debugMessages) {
          System.out.println("No more matches, exiting loop");
        }
        break;
      }
      curOffset = smallestOffset;
      if(debugMessages) {
        System.out.println("Found next match(es) and setting curOffset to "+curOffset);
      }
      // find all the rules that match at the smallest offset, ordered by
      // increasing rule number
      // Also, already find the longest length
      longestLength = 0;
      List<PatternRule> candidates = new LinkedList<PatternRule>();
      for(PatternRule rule : rulesList) {
        if(rule.matcher_active && rule.matcher.start() == smallestOffset) {
          candidates.add(rule);
          if(rule.matcher_length > longestLength) {
            longestLength = rule.matcher_length;
          }
        }
      }
      
      if(debugMessages) {
        System.out.println("Found the following matches at this offset: "+candidates);
      }
      
      if(matchPreference == MatchPreference.ALL) {
        for(PatternRule rule : candidates) {
          if(debugMessages) {
            System.out.println("Annotating for ALL: "+rule);
          }
          annotateMatch(rule, outputAS,chunk);
        }
      } else {
        if(candidates.size() == 1) {
          if(debugMessages) {
            System.out.println("Annotating size 1 "+candidates.get(0));
          }
          annotateMatch(candidates.get(0), outputAS,chunk);
          longestLength = candidates.get(0).matcher_length;
        } else if(matchPreference == MatchPreference.FIRSTRULE) {
          if(debugMessages) {
            System.out.println("Annotating FIRSTRULE "+candidates.get(0));
          }
          annotateMatch(candidates.get(0),outputAS,chunk);
          longestLength = candidates.get(0).matcher_length; 
        } else if(matchPreference == MatchPreference.LASTRULE) {
          if(debugMessages) {
            System.out.println("Annotating LASTRULE "+candidates.get(candidates.size()-1));
          }
          annotateMatch(candidates.get(candidates.size()-1),outputAS,chunk);
          longestLength = candidates.get(candidates.size()-1).matcher_length;
        } else {
          // filter to only take the longest matches
          List<PatternRule> longestRules = new ArrayList<PatternRule>();
          for(PatternRule rule : candidates) {
            if(rule.matcher_length == longestLength) {
              if(matchPreference == MatchPreference.LONGEST_ALLRULES) {
                if(debugMessages) {
                  System.out.println("Annotating LONGEST_ALLRULES "+rule);
                }
                annotateMatch(rule, outputAS,chunk);
              } else {
                longestRules.add(rule);
              }
            }
          }
          if(matchPreference == MatchPreference.LONGEST_FIRSTRULE) {
            if(debugMessages) {
              System.out.println("Annotating LONGEST_FIRSTRULE "+longestRules.get(0));
            }
            annotateMatch(longestRules.get(0), outputAS,chunk);
          } else if(matchPreference == MatchPreference.LONGEST_LASTRULE) {
            if(debugMessages) {
              System.out.println("Annotating LONGEST_LASTRULE "+longestRules.get(longestRules.size()-1));
            }
            annotateMatch(longestRules.get(longestRules.size()-1),outputAS,chunk);
          }
        }
      }      
      // advance the curOffset to either the next offset or 
      // to the offset after the longest match we have annotated.
      if(overlappingMatches) {
        curOffset += 1;
      } else {
        curOffset += longestLength;
      }
    }
    
      
    fireProcessFinished();
  }

  protected void annotateMatch(PatternRule rule, AnnotationSet outputAS, TextChunk chunk) {
    List<AnnDesc> anndescs = rule.annDescs;
    Matcher matcher = rule.matcher;
    //System.out.println("annotateMatch for "+rule);
    for (AnnDesc anndesc : anndescs) {
      //System.out.println("Processing for anndesc "+anndesc);
      String anntype = anndesc.typename;
      Map<String, String> stringfeatures = anndesc.constantfeatures;
      Map<String, Integer> groupfeatures = anndesc.groupfeatures;
      List<Integer> groupnrs = anndesc.groupnumbers;

      //System.out.println("Found a match: " + matcher.group());

      for (int groupnr : groupnrs) {
        //System.out.println("Processing for group nr "+groupnr);
        // get the offsets
        String match;
        try {
          match = matcher.group(groupnr);
        } catch (RuntimeException ex) {
          throw new GateRuntimeException("Error matching group "+groupnr+" in rule "+rule,ex);
        }
        if (match != null) {
          int from = matcher.start(groupnr);
          int to = matcher.end(groupnr);
          FeatureMap fm = Factory.newFeatureMap();
          if (stringfeatures != null) {
            for (String key : stringfeatures.keySet()) {
              fm.put(key, stringfeatures.get(key));
            }
          }
          if (groupfeatures != null) {
            for (String key : groupfeatures.keySet()) {
              String gstr = matcher.group(groupfeatures.get(key));
              if (gstr != null) {
                fm.put(key, gstr);
              }
            }
          }
          if(getDebugging()) {
            // in debugging mode, we add the rule number and groupd number
            // that matched to each annotation
            fm.put("sra_debug_rule",rule.rulenumber);
            fm.put("sra_debug_anndesc",anndesc.anndescnumber);
            fm.put("sra_debug_group",groupnr);
          }
          int fromOff = chunk.getStartOffset(from);
          // TODO: why is this (.-1)+1 necessary here but things work with the 
          // indirect extended gazetteer?
          int toOff = chunk.getEndOffset(to-1)+1;
          try {
            if(debugMessages) {
              System.out.println("Adding annotation "+anntype+" anndesc="+anndesc+" groupnr="+groupnr);
            }
            outputAS.add(new Long(fromOff), new Long(toOff), anntype, fm);
          } catch (InvalidOffsetException ex) {
            throw new GateRuntimeException(
              "Invalid offset exception for from=" + fromOff + ", fromInChunk="+from+", to=" + toOff + ", toInChunk="+to + 
              ",doclen="+document.getContent().size(),ex);
          }
        }
      }

    } // for(anndescs)
  }
  
  List<PatternRule> loadRulesList(URL patternFile) throws UnsupportedEncodingException, IOException, ResourceInstantiationException {
    List<PatternRule> patternrules = new ArrayList<PatternRule>();
    BufferedReader reader = new BomStrippingInputStreamReader(patternFile.openStream(), "UTF-8");

    StringBuilder patternString = new StringBuilder();    
    
    String line = reader.readLine();
    Pattern macroLine = Pattern.compile(" *([a-zA-Z0-9_]+)=(.+)");
    Map<String,String> macros = new HashMap<String,String>();
    StrLookup macroLookup = StrLookup.mapLookup(macros);
    StrSubstitutor macroSubst = new StrSubstitutor(macroLookup,"<<",">>",'\\');
    int currentRuleNumber = 1;  // we start counting by 1
    int currentAnnDescNumber;
    PatternRule currentPatternRule = new PatternRule();
    currentPatternRule.rulenumber = currentRuleNumber++;
    currentAnnDescNumber = 1;
    boolean haveRule = false;
    int linenr = 0;
    while (line != null) {
      linenr++;
      line = line.trim();
      if (line.length() == 0) {
        line = reader.readLine();
        continue;
      }
      if (line.startsWith("//")) {
        line = reader.readLine();
        continue;
      }
      Matcher matchMacro = macroLine.matcher(line);
      if (line.startsWith("|")) {
        if (haveRule) {
          patternrules.add(currentPatternRule);
          currentPatternRule = new PatternRule();
          currentPatternRule.rulenumber = currentRuleNumber++;
          currentAnnDescNumber = 1;
          haveRule = false;
        }
        line = line.substring(1);
        // replace any macro variables in the line with the values we
        // already have defined.
        line = macroSubst.replace(line);
        //System.out.println("JavaRegexpAnnotator: PATTERN:"+line);

        // collect the regexp lines in the current regexp
        if (patternString.length() > 0) {
          patternString.append("|");
        }
        patternString.append("(?:");
        patternString.append(line);
        patternString.append(")");
      } else if (matchMacro.matches()) {
        String macroVar = matchMacro.group(1);
        String macroPat = matchMacro.group(2);
        // first replace any variables that may occur in the pattern of this macro
        macroPat = macroSubst.replace(macroPat);
        //System.out.println("JavaRegexpAnnotator: MACRO:"+macroVar+"="+macroPat);
        macros.put(macroVar, macroPat);
      } else {
        // this must be a rule body line of the form
        // groupnumber => Typename [key/value list]
        // Each such line adds an AnnDesc to the current PatternRule
        //System.out.println("JavaRegexpAnnotator: BODY:"+line);
        Matcher ruleBodyMatcher = ruleStartPattern.matcher(line);
        if (ruleBodyMatcher.matches()) {
          // at this point the patternString must be non-empty!
          if (!haveRule) {
            // the first time we get a rule body
            haveRule = true;
            if (patternString.length() == 0) {
              throw new GateRuntimeException("Rule body must be preceded by patterns");
            }
            String ps = patternString.toString();
            currentPatternRule.pattern = Pattern.compile(ps,Pattern.MULTILINE);
            patternString = new StringBuilder();
            currentPatternRule.annDescs = new ArrayList<AnnDesc>();
          }
          AnnDesc anndesc = new AnnDesc();
          anndesc.anndescnumber = currentAnnDescNumber++;
          anndesc.typename = ruleBodyMatcher.group(2);
          String groupliststring = ruleBodyMatcher.group(1);
          // split the grouplist and create the actual list, then sort it ascending
          List<Integer> grouplist = new ArrayList<Integer>();
          String[] groupitemstrings = groupliststring.split(",");
          for (String groupitemstring : groupitemstrings) {
            grouplist.add(new Integer(groupitemstring));
          }
          anndesc.groupnumbers = grouplist;
          // process the optional feature list
          String featurelist = ruleBodyMatcher.group(3);
          if (featurelist == null) {
            // no features, just assign null
            anndesc.constantfeatures = null;
            anndesc.groupfeatures = null;
          } else {
            featurelist = featurelist.trim();
            Map<String, Integer> groupfeatures = null;
            Map<String, String> constantfeatures = null;
            String[] featureitems = featurelist.split(",");
            for (String featureitem : featureitems) {
              String[] keyval = featureitem.split("=");
              String key = keyval[0];
              String value = keyval[1];
              if (value.matches("^\\$[0-9]+$")) {
                if (groupfeatures == null) {
                  groupfeatures = new HashMap<String, Integer>();
                }
                groupfeatures.put(key, new Integer(value.substring(1)));
              } else if(value.matches("^\"[^\"]*\"$")) {
                value = value.substring(1, value.length()-1);
                if (constantfeatures == null) {
                  constantfeatures = new HashMap<String, String>();
                }
                constantfeatures.put(key, value);
              } else {
                throw new GateRuntimeException("Feature value must be $n or a quoted string not "+key+" in line "+linenr);
              }
            }
            anndesc.constantfeatures = constantfeatures;
            anndesc.groupfeatures = groupfeatures;
          }
          // add this AnnDesc to the pattern rule
          currentPatternRule.annDescs.add(anndesc);
        } else {
          throw new GateRuntimeException("Strange rule body line nr "+linenr+": "+line);
        }
      }
      line = reader.readLine();
    }
    if (haveRule) {
      patternrules.add(currentPatternRule);
    }
    return patternrules;
  }

  // a class representing a pattern rule. Each rule is associated with
  // a regular expression pattern, an annotation type name, a list of
  // group numbers, and a map of feature/value pairs.
  protected class PatternRule {

    public Pattern pattern;
    public Matcher matcher;
    public boolean matcher_active;  // state: if the matcher is still active
    public int matcher_length;      // state: the length of the current match
    public int rulenumber;
    public List<AnnDesc> annDescs;
    @Override
    public String toString() {
      return "R"+rulenumber+"="+pattern.toString();
    }
  }

  protected class AnnDesc {

    public int anndescnumber;
    public String typename;
    public List<Integer> groupnumbers;
    public Map<String, String> constantfeatures;
    public Map<String, Integer> groupfeatures;
  }
  
} // class SimpleRegexpAnnotator

