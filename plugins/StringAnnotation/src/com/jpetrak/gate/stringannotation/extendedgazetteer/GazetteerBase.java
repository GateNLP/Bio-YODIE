/*
 * GazetterBase.java
 * 
 * Author: Johann Petrak
 * 
 * License: LGPL
 *
 */
package com.jpetrak.gate.stringannotation.extendedgazetteer;

import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.ANNIEConstants;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.util.BomStrippingInputStreamReader;
import gate.util.GateRuntimeException;
import gate.util.Strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.jpetrak.gate.stringannotation.extendedgazetteer.trie.GazStoreTrie3;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.yaml.snakeyaml.Yaml;

/**
 * Common Base class for all gazetteer implementations. All these PRs need to
 * have the config file URL, case sensitivity init parameters so the parameters
 * and the loading logic is moved into this base class.
 *
 * @author Johann Petrak
 */
public abstract class GazetteerBase extends AbstractLanguageAnalyser {

  /**
   *
   */
  @CreoleParameter(comment = "The URL to the gazetteer configuration file", suffixes = "def;defyaml", defaultValue = "")
  public void setConfigFileURL(java.net.URL theURL) {
    configFileURL = theURL;
  }

  public java.net.URL getConfigFileURL() {
    return configFileURL;
  }
  private java.net.URL configFileURL;

  @CreoleParameter(comment = "Should this gazetteer differentiate on case",
          defaultValue = "true")
  public void setCaseSensitive(Boolean yesno) {
    caseSensitive = yesno;
  }

  public Boolean getCaseSensitive() {
    return caseSensitive;
  }
  protected Boolean caseSensitive;

  @CreoleParameter(comment = "For case insensitive matches, the locale to use for normalizing case",
          defaultValue = "en")
  public void setCaseConversionLanguage(String val) {
    caseConversionLanguage = val;
    caseConversionLocale = new Locale(val);
  }

  public String getCaseConversionLanguage() {
    return caseConversionLanguage;
  }
  private String caseConversionLanguage;

  protected static final String unescapedSeparator = Strings.unescape("\\t");
  protected Locale caseConversionLocale = Locale.ENGLISH;
  protected Logger logger;
  //protected CharMapState initialState;
  protected GazStore gazStore;
  private static final int MAX_FEATURES_PER_ENTRY = 200;
  private static Pattern ws_pattern;
  private static final String ws_chars =
          "\\u0009" // CHARACTER TABULATION
          + "\\u000A" // LINE FEED (LF)
          + "\\u000B" // LINE TABULATION
          + "\\u000C" // FORM FEED (FF)
          + "\\u000D" // CARRIAGE RETURN (CR)
          + "\\u0020" // SPACE
          + "\\u0085" // NEXT LINE (NEL) 
          + "\\u00A0" // NO-BREAK SPACE
          + "\\u1680" // OGHAM SPACE MARK
          + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
          + "\\u2000" // EN QUAD 
          + "\\u2001" // EM QUAD 
          + "\\u2002" // EN SPACE
          + "\\u2003" // EM SPACE
          + "\\u2004" // THREE-PER-EM SPACE
          + "\\u2005" // FOUR-PER-EM SPACE
          + "\\u2006" // SIX-PER-EM SPACE
          + "\\u2007" // FIGURE SPACE
          + "\\u2008" // PUNCTUATION SPACE
          + "\\u2009" // THIN SPACE
          + "\\u200A" // HAIR SPACE
          + "\\u2028" // LINE SEPARATOR
          + "\\u2029" // PARAGRAPH SEPARATOR
          + "\\u202F" // NARROW NO-BREAK SPACE
          + "\\u205F" // MEDIUM MATHEMATICAL SPACE
          + "\\u3000" // IDEOGRAPHIC SPACE
          ;
  private static final String ws_class = "[" + ws_chars + "]";
  private static final String ws_patternstring = ws_class + "+";
  private static final String encoding = "UTF-8";

  public GazetteerBase() {
    logger = Logger.getLogger(this.getClass().getName());
  }

  @Override
  public Resource init() throws ResourceInstantiationException {
    // precompile the pattern used to replace all unicode whitespace in gazetteer
    // entries with a single space.
    ws_pattern = Pattern.compile(ws_patternstring);
    incrementGazStore();
    return this;
  }
  protected static Map<String, GazStore> loadedGazStores = new HashMap<String, GazStore>();

  private synchronized void incrementGazStore() throws ResourceInstantiationException {
    String uniqueGazStoreKey = genUniqueGazStoreKey();
    logger.info("Creating gazetteer for " + getConfigFileURL());
    System.gc();
    long startTime = System.currentTimeMillis();
    long before = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    GazStore gs = loadedGazStores.get(uniqueGazStoreKey);
    if (gs != null) {
      // The FSM for this file/parm combination already has been compiled, just
      // reuse it for this PR
      gazStore = gs;
      gazStore.refcount++;
      logger.info("Reusing already generated GazStore for " + uniqueGazStoreKey);
    } else {
      try {
        loadData();
        gazStore.compact();
      } catch (Exception ex) {
        throw new ResourceInstantiationException("Could not load gazetteer", ex);
      }
      gazStore.refcount++;
      loadedGazStores.put(uniqueGazStoreKey, gazStore);
      logger.info("New GazStore loaded for " + uniqueGazStoreKey);
    }
    long endTime = System.currentTimeMillis();
    System.gc();
    long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    logger.info("Gazetteer created in (secs):          " + ((endTime - startTime) / 1000.0));
    logger.info("Heap memory increase (estimate,MB):   "
            + String.format("%01.3f", ((after - before) / (1024.0 * 1024.0))));
    logger.info(gazStore.statsString());
  }

  private synchronized void decrementGazStore() {
    String key = genUniqueGazStoreKey();
    GazStore gs = loadedGazStores.get(key);
    gs.refcount--;
    if (gs.refcount == 0) {
      loadedGazStores.remove(key);
      logger.info("Removing GazStore for " + key);
    }
  }

  private synchronized void removeGazStore() {
    String key = genUniqueGazStoreKey();
    loadedGazStores.remove(key);
    logger.info("reInit(): force-removing GazStore for " + key);
  }

  protected String genUniqueGazStoreKey() {
    return " cs=" + caseSensitive + " url=" + configFileURL + " lang=" + caseConversionLanguage;
  }

  @Override
  public void cleanup() {
    decrementGazStore();
  }

  public void save(File whereTo) throws IOException {
    gazStore.save(whereTo);
  }

  @Override
  /**
   */
  // TODO: we may want to delete the cache and re-init from the list files when
  // reInit() is called?
  public void reInit() throws ResourceInstantiationException {
    removeGazStore();
    init();
  }

  protected void loadData() throws UnsupportedEncodingException, IOException, ResourceInstantiationException {
    // if we find the cache file, load it, else load the original files and create the cache file

    File configFile = gate.util.Files.fileFromURL(configFileURL);

    // check the extension and determine if we have an old format .def file or 
    // a new format .defyaml file
    String name = configFile.getName();
    int i = name.lastIndexOf('.') + 1;
    if (i < 0) {
      throw new GateRuntimeException("Config file must have a .def or .defyaml extension");
    }
    String ext = name.substring(i);
    if (ext.isEmpty() || !(ext.equals("def") || ext.equals("defyaml"))) {
      throw new GateRuntimeException("Config file must have a .def or .defyaml extension");
    }
    if (ext.equals("def")) {
      loadDataFromDef(configFile);
    } else {
      loadDataFromYaml(configFile);
    }
  }

  protected void loadDataFromDef(File configFile) throws IOException {
    String configFileName = configFile.getAbsolutePath();
    String gazbinFileName = configFileName.replaceAll("\\.def$", ".gazbin");
    if (configFileName.equals(gazbinFileName)) {
      throw new GateRuntimeException("Config file must have def or defyaml extension");
    }
    File gazbinFile = new File(gazbinFileName);

    if (gazbinFile.exists()) {
      gazStore = new GazStoreTrie3();
      gazStore = gazStore.load(gazbinFile);

    } else {
      gazStore = new GazStoreTrie3();
      BufferedReader defReader =
              new BomStrippingInputStreamReader((configFileURL).openStream(), encoding);
      String line;
      //logger.info("Loading data");
      while (null != (line = defReader.readLine())) {
        String[] fields = line.split(":");
        if (fields.length == 0) {
          System.err.println("Empty line in file " + configFileURL);
        } else {
          String listFileName = "";
          String majorType = "";
          String minorType = "";
          String languages = "";
          String annotationType = ANNIEConstants.LOOKUP_ANNOTATION_TYPE;
          listFileName = fields[0];
          if (fields.length > 1) {
            majorType = fields[1];
          }
          if (fields.length > 2) {
            minorType = fields[2];
          }
          if (fields.length > 3) {
            languages = fields[3];
          }
          if (fields.length > 4) {
            annotationType = fields[4];
          }
          if (fields.length > 5) {
            defReader.close();
            throw new GateRuntimeException("Line has more that 5 fields in def file " + configFileURL);
          }
          logger.debug("Reading from " + listFileName + ", " + majorType + "/" + minorType + "/" + languages + "/" + annotationType);
          //logger.info("DEBUG: loading data from "+listFileName);
          loadListFile(listFileName, majorType, minorType, languages, annotationType);
        }
      } //while
      defReader.close();
      gazStore.compact();
      logger.info("Gazetteer loaded from list files");

      gazStore.save(gazbinFile);
    } // gazbinFile exists ... else
  }

  protected void loadDataFromYaml(File configFile) throws IOException {
    String configFileName = configFile.getAbsolutePath();
    String gazbinFileName = configFileName.replaceAll("\\.defyaml$", ".gazbin");
    if (configFileName.equals(gazbinFileName)) {
      throw new GateRuntimeException("Config file must have def or defyaml extension");
    }
    File gazbinFile = new File(gazbinFileName);

    String gazbinDir = gazbinFile.getParent();
    String gazbinName = gazbinFile.getName();

    // Always read the yaml file so we can get any special location of the cache
    // file or figure out that we should not try to load the cache file
    Yaml yaml = new Yaml();
    BufferedReader yamlReader =
            new BomStrippingInputStreamReader((configFileURL).openStream(), encoding);
    Object configObject = yaml.load(yamlReader);

    List<Map> configListFiles = null;
    if (configObject instanceof Map) {
      Map<String, Object> configMap = (Map<String, Object>) configObject;
      String configCacheDirName = (String) configMap.get("cacheDir");
      if (configCacheDirName != null) {
        gazbinDir = configCacheDirName;
      }
      String configCacheFileName = (String) configMap.get("chacheFile");
      if (configCacheFileName != null) {
        gazbinName = configCacheFileName;
      }
      gazbinFile = new File(new File(gazbinDir), gazbinName);
      configListFiles = (List<Map>) configMap.get("listFiles");
    } else if (configObject instanceof List) {
      configListFiles = (List<Map>) configObject;
    } else {
      throw new GateRuntimeException("Strange YAML format for the defyaml file " + configFileURL);
    }

    // if we want to load the cache and it exists, load it
    if (gazbinFile.exists()) {
      gazStore = new GazStoreTrie3();
      gazStore = gazStore.load(gazbinFile);
    } else {
      gazStore = new GazStoreTrie3();
      // go through all the list and tsv files to load and load them
      for (Map configListFile : configListFiles) {
        // TODO!!!
        //logger.debug("Reading from "+listFileName+", "+majorType+"/"+minorType+"/"+languages+"/"+annotationType);
        //logger.info("DEBUG: loading data from "+listFileName);
        //loadListFile(listFileName,majorType,minorType,languages,annotationType);
      } //while
      gazStore.compact();
      logger.info("Gazetteer loaded from list files");

      gazStore.save(gazbinFile);
    } // gazbinFile exists ... else
  }

  void loadListFile(String listFileName, String majorType, String minorType,
          String languages, String annotationType)
          throws MalformedURLException, IOException {

    //logger.info("Loading list file "+listFileName);
    URL lurl = new URL(configFileURL, listFileName);
    FeatureMap listFeatures = Factory.newFeatureMap();
    listFeatures.put(LOOKUP_MAJOR_TYPE_FEATURE_NAME, majorType);
    listFeatures.put(LOOKUP_MINOR_TYPE_FEATURE_NAME, minorType);
    if (languages != null) {
      listFeatures.put(LOOKUP_LANGUAGE_FEATURE_NAME, languages);
    }
    gazStore.addListInfo(annotationType, lurl.toString(), listFeatures);
    int infoIndex = gazStore.getListInfos().size() - 1;
    //Lookup defaultLookup = new Lookup(listFileName, majorType, minorType, 
    //        languages, annotationType);
    BufferedReader listReader = null;
    if (listFileName.endsWith(".gz")) {
      listReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(lurl.openStream()), encoding));
    } else {
      listReader = new BomStrippingInputStreamReader(lurl.openStream(), encoding);
    }
    String line;
    int lines = 0;
    String[] entryFeatures = new String[0];
    while (null != (line = listReader.readLine())) {
      entryFeatures = new String[0];
      lines++;
      String entry = line;
      // check if we have a separator in the line, if yes, we should take
      // the part before the first separator to be the entry and extract
      // the features from everything that comes after it.
      // All this only, if the separator is set at all
      if (unescapedSeparator != null) {
        int firstSepIndex = line.indexOf(unescapedSeparator);
        if (firstSepIndex > -1) {
          entry = line.substring(0, firstSepIndex);
          // split the rest of the line real fast
          int lastSepIndex = firstSepIndex;
          int nrFeatures = 0;
          String[] featureBuffer = new String[MAX_FEATURES_PER_ENTRY * 2];
          int nextSepIndex = 0;
          do {
            //logger.info("Feature nr: "+(nrFeatures+1));
            // check if we already have maximum number of features allows
            if (nrFeatures == MAX_FEATURES_PER_ENTRY) {
              throw new GateRuntimeException(
                      "More than " + MAX_FEATURES_PER_ENTRY + " features in gazetteer entry in list " + listFileName
                      + " line " + lines);
            }
            // get the index of the next separator
            nextSepIndex = line.indexOf(unescapedSeparator, lastSepIndex + 1);
            if (nextSepIndex < 0) { // if none found, use beyond end of String
              nextSepIndex = line.length();
            }
            // find the first equals character in the string section for this feature
            int equalsIndex = line.indexOf('=', lastSepIndex + 1);
            //logger.info("lastSepIndex="+lastSepIndex+", nextSepIndex="+nextSepIndex+", equalsIndex="+equalsIndex);
            // if we do not find one or only after the end of this feature string,
            // make a fuss about it
            if (equalsIndex < 0 || equalsIndex >= nextSepIndex) {
              throw new GateRuntimeException(
                      "Not a proper feature=value in gazetteer list " + listFileName
                      + " line " + lines + "\nlooking at " + line.substring(lastSepIndex, nextSepIndex)
                      + " lastSepIndex is " + lastSepIndex
                      + " nextSepIndex is " + nextSepIndex
                      + " equals at " + equalsIndex);
            }
            // add the key/value to the features string array: 
            // key to even positions, starting with 0, value to uneven starting with 1 
            nrFeatures++;
            featureBuffer[nrFeatures * 2 - 2] = line.substring(lastSepIndex + 1, equalsIndex);
            featureBuffer[nrFeatures * 2 - 1] = line.substring(equalsIndex + 1, nextSepIndex);
            lastSepIndex = nextSepIndex;
          } while (nextSepIndex < line.length());
          if (nrFeatures > 0) {
            entryFeatures = new String[nrFeatures * 2];
            for (int i = 0; i < entryFeatures.length; i++) {
              entryFeatures[i] = featureBuffer[i];
            }
          } else {
            entryFeatures = new String[0];
          }
        }
      } // have separator 
      // entry Features are passed as null if there are no entry features
      addLookup(entry, infoIndex, entryFeatures);
    } // while
    listReader.close();
    //logger.info("DEBUG: lines read "+lines);
    logger.debug("Lines read: " + lines);
  }

  public void addLookup(String text, int listInfoIndex, String[] entryFeatures) {
    // 1) instead of translating every character that is not within a word
    // on the fly when adding states, first normalize the text string and then
    // trim it. If the resulting word is empty, skip the whole processing because
    // the original consisted only of characters that are not word characters!
    // 2) if something remains and we want not exact case matching, convert
    // the whole string to both only upper and only lower case first, then
    // compare the lengths. If the lengths differ, add both in addition to
    // the original!

    String textNormalized = new String(text).trim();
    // convert anything that is a sequence of whitespace to a single space
    // WAS: textNormalized = textNormalized.replaceAll("  +", " ");
    textNormalized = ws_pattern.matcher(textNormalized).replaceAll(" ");
    if (textNormalized.isEmpty()) {
      //logger.info("Ignoring, is empty");
      return;
    }

    // TODO: at some point this should get changed to allow for both totally
    // ignoring case (as now) and for matching either the original or a 
    // case-normalization (in the runtime). This would also need a setting
    // for specifying what the case normalization should be (e.g. UPPERCASE).

    // For now, we always normalize to upper case when case is ignored. 
    // The gazetteer should contain lowercase or firstCaseUpper words, but
    // better not ALLCAPS in order for lower case characters which get mapped
    // to two characters in uppercase to be mapped correctly.
    // For these special cases, we add two UPPERCASE normalizations:
    // the one with the two characters and the one where the char.toUpperCase 
    // is used.
    if (!caseSensitive) {
      String textNormalizedUpper = textNormalized.toUpperCase(caseConversionLocale);
      if (textNormalizedUpper.length() != textNormalized.length()) {
        gazStore.addLookup(textNormalizedUpper, listInfoIndex, entryFeatures);
        char[] textChars2 = new char[textNormalized.length()];
        for (int i = 0; i < textNormalized.length(); i++) {
          textChars2[i] = Character.toUpperCase(textNormalized.charAt(i));
        }
        gazStore.addLookup(new String(textChars2), listInfoIndex, entryFeatures);
      } else {
        // if both version are of the same length, it is sufficient to add the 
        // upper case version
        gazStore.addLookup(textNormalizedUpper, listInfoIndex, entryFeatures);
      }
    } else {
      gazStore.addLookup(textNormalized, listInfoIndex, entryFeatures);
    }


  } // addLookup

  /**
   * For a given lookups iterator, return a list of feature maps filled with the
   * information from those lookups.
   *
   * @param lookups
   * @return
   */
  public List<FeatureMap> lookups2FeatureMaps(Iterator<Lookup> lookups) {
    List<FeatureMap> fms = new ArrayList<FeatureMap>();
    if (lookups == null) {
      return fms;
    }
    while (lookups.hasNext()) {
      FeatureMap fm = Factory.newFeatureMap();
      Lookup currentLookup = lookups.next();
      gazStore.addLookupListFeatures(fm, currentLookup);
      fm.put("_listnr", gazStore.getListInfoIndex(currentLookup));
      gazStore.addLookupEntryFeatures(fm, currentLookup);
      fms.add(fm);
    }
    return fms;
  }
} // ExtendedGazetteer

