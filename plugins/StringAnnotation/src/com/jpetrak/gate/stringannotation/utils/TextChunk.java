package com.jpetrak.gate.stringannotation.utils;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Utils;
import gate.util.GateRuntimeException;

import java.util.Arrays;
import java.util.List;

/** 
 * Represents a chunk of text to generate gazetteer matches for.
 * The text is either a part of the original document text or text generated
 * from the content of a feature of the word annotations.
 * The text is made up of the parts that come from each wholly contained
 * word annotation separated by a space for each wholly contained
 * space annotation.
 * 
 * @author Johann Petrak
 *
 */
public class TextChunk {
  private TextChunk() { }
  
  private static final boolean debug = false;
  
  private char[] text;
  // an array of offsets that indicate for each character in our text, at which 
  // offset an annotation would have to start, if this character were at the beginning
  // of the match. If the text is created from the feature value of the word annotations,
  // then all offsets for all of the characters of the text from one annotation will
  // refer to the start offset of that annotation.
  // NOTE: this int ALSO stores information about whether that location in the text
  // indicates a word begginning. We use the sign bit for this and store the offset
  // in the following way: 
  // No word beginning: the offset, unchanged (which is always >= 0)
  // Word beginning: -(offset+1) (offset 0->-1, offset 1->-2 etc.)
  private int[] startOffsets;
  // an array of offsets that indicate for each character in our text, at which 
  // offset an annotation would have to end, if this character were at the end
  // of the match. If the text is created from the feature value of the word annotations,
  // then all offsets for all of the characters of the text from one annotation will
  // refer to the end offset of that annotation.
  // NOTE: this int ALSO stores information about whether that location in the text
  // indicates a word ending. We use the sign bit for this and store the offset
  // in the following way: 
  // No word ending: the offset, unchanged (which is always >= 0)
  // Word ending: -(offset+1) (offset 0->-1, offset 1->-2 etc.)
  private int[] endOffsets;
  
  private int length;
  private int initialLength;
  private int from;
  private int to;
  
  /**
   * Get the original document start offset for this offset in the text
   * 
   * @param off
   * @return
   */
  public int getStartOffset(int off) {
    int tmp = startOffsets[off];
    if(tmp < 0) {
      return (-tmp)-1;
    } else { 
      return tmp;
    }
  }
  /**
   * Get the original document end offset for this offset in the text
   * 
   * @param off
   * @return
   */
  public int getEndOffset(int off) {
    int tmp = endOffsets[off];
    if(tmp < 0) {
      return (-tmp)-1;
    } else { 
      return tmp;
    }
  }
  
  // store the original start offset for the text offset
  private void putStartOffset(int textoff, int origoff) {
    guardOffsetP(textoff);
    startOffsets[textoff] = origoff;
  }
  // store the original end offset for the text offset
  private void putEndOffset(int textoff, int origoff) {
    guardOffsetP(textoff);
    endOffsets[textoff] = origoff;
  }
  
  private void putTextChar(int textoff, char ch) {
    guardOffsetP(textoff);
    text[textoff] = ch;
  }
  
  
  // set this text offset as a word start
  private void setIsValidMatchStart(int textoff) {
    guardOffsetP(textoff);
    int tmp = startOffsets[textoff];
    if(tmp >= 0) {
      startOffsets[textoff] = -(tmp+1);
    }
  }
  // set this text offset as a word end
  private void setIsValidMatchEnd(int textoff) {
    guardOffsetP(textoff);
    int tmp = endOffsets[textoff];
    if(tmp >= 0) {
      endOffsets[textoff] = -(tmp+1);
    }
  }
  
  public boolean isValidMatchStart(int offset) {
    guardOffset(offset);
    if(startOffsets[offset] < 0) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isValidMatchEnd(int offset) {
    guardOffset(offset);
    if(endOffsets[offset] < 0) {
      return true;
    } else {
      return false;
    }
  }
  
  
  public char getCharAt(int textoff) {
    guardOffset(textoff);
    return text[textoff];
  }
  
  private void guardOffset(int off) {
    if(!(off < length) || off<0) {
      throw new GateRuntimeException("Attempt to use invalid chunk text offset: off="+off+" length="+length);
    }    
  }
  // if necessary, enlarge our arrays to hold more data!
  private void guardOffsetP(int off) {
    if(!(off < text.length)) {
      if(debug) {
        System.out.println("enlarging arrays!");
      }
      int newLength = text.length+initialLength;
      char[] newtext = new char[newLength];
      System.arraycopy(text, 0, newtext, 0, text.length);
      text = newtext;
      int[] newStarts = new int[newLength];
      System.arraycopy(startOffsets, 0, newStarts, 0, startOffsets.length);
      startOffsets = newStarts;
      int[] newEnds   = new int[newLength];
      System.arraycopy(endOffsets, 0, newEnds, 0, endOffsets.length);
      endOffsets = newEnds;
    }    
  }
  
  public static TextChunk makeChunk(
          Document document,
          long fromOffset, long toOffset          
          ) {
    TextChunk chunk = new TextChunk();
    String docText = document.getContent().toString();
    if((toOffset - fromOffset)<0) {
      throw new GateRuntimeException("Cannot annotate range of negative length");
    }
    int length = (int)(toOffset - fromOffset);
    chunk.initialLength = length;
    chunk.from = (int)fromOffset;
    chunk.to = (int)toOffset;
    chunk.text = docText.substring(chunk.from, chunk.to).toCharArray();
    chunk.endOffsets = new int[chunk.initialLength];
    chunk.startOffsets = new int[chunk.initialLength];
    for(int i = 0; i < chunk.to; i++) {
      chunk.putStartOffset(i, chunk.from+i);
      chunk.putEndOffset(i, chunk.from+i);
      chunk.setIsValidMatchEnd(i);
      chunk.setIsValidMatchStart(i);
    }
    return chunk;
  }
  
  
  public static TextChunk makeChunk(
      Document document, 
      long fromOffset, long toOffset,
      boolean caseNormalize,
      AnnotationSet processAnns, String wordAnnotationType, String wordAnnotationFeature, String spaceAnnotationType,
      boolean startWithWordStart, boolean endWithWordEnd) {
    
    // TODO: at the moment we do not do language specific case normalization here because this would
    // make it more complex to keep track of the indices -- String.toUpper(locale) can change the size of the string!
    // Instead we use char.toUpper which is not perfect but makes things easier!
    TextChunk chunk = new TextChunk();
    boolean haveFeature = !(wordAnnotationFeature == null || wordAnnotationFeature.equals(""));
    String doctext = document.getContent().toString();
    chunk.initialLength = Math.max((int)(toOffset-fromOffset),500);
    chunk.from = (int)fromOffset;
    chunk.to = (int)toOffset;
    chunk.text = new char[chunk.initialLength];
    chunk.endOffsets = new int[chunk.initialLength];
    chunk.startOffsets = new int[chunk.initialLength];
    AnnotationSet actualAnns = processAnns.get(fromOffset,toOffset);
    AnnotationSet wordAnns = actualAnns.get(wordAnnotationType);
    if(wordAnns.isEmpty()) {
      chunk.length = 0;
      return chunk;
    }
    List<Annotation> actualAnnsList = Utils.inDocumentOrder(actualAnns);
    int i = 0; // index into the text, startOffsets and endOffsets arrays
    boolean firstSpace = true;
    if(debug) {
      System.out.println("Number of total processAnns="+processAnns.size());
      System.out.println("Number of anns in range="+actualAnns.size());
    }
    
    for(Annotation actualAnn : actualAnnsList) {
      int curStart = actualAnn.getStartNode().getOffset().intValue();
      int curEnd   = actualAnn.getEndNode().getOffset().intValue();
      String curType = actualAnn.getType();
      if(curType.equals(spaceAnnotationType)) {
        if(firstSpace) {
          // add a space 
          chunk.putTextChar(i,' ');
          // was: chunk.text[i] = ' ';
          i++;
          firstSpace = false;
        }        
      } else {
        // we have a word
        if(debug) {
          System.out.println("Add word annotation: "+actualAnn);
          System.out.println("i="+i);
        }
        
        String wordText = null;
        
        // unless we havent set the text already to 0 earlier
        if (wordText == null) {
          if (haveFeature) {
            wordText = (String) actualAnn.getFeatures().get(
                wordAnnotationFeature);
            if (debug) {
              System.out.println("Got feature value=" + wordText);
            }
          } else {
            wordText = gate.Utils.stringFor(document, actualAnn);
            if (debug) {
              System.out.println("Got document text=" + wordText);
            }
          }
        }
        if(wordText == null || wordText.isEmpty()) {
          if(debug) {
            System.out.println("Skipping: no text!");
          }
          continue;
        }

        // TODO: if the wordStartFeature is given, get its value and set the default
        // for validMatchStart accordingly, otherwise set it to true.
        // Same for wordEndFeature
        boolean validMatchEndDefault = true;
        boolean validMatchStartDefault = true;
                
        firstSpace = true; 
        // add the wordText
        char[] wordTextChars = wordText.toCharArray();
        for(int j=0;j<wordTextChars.length;j++) {
          char c = wordTextChars[j];
          if(caseNormalize) {
            c = Character.toUpperCase(c);
          }
          chunk.putTextChar(i, wordTextChars[j]);
          // was: chunk.text[i] = wordTextChars[j];
          if(haveFeature) {
            chunk.putStartOffset(i, curStart);
            // was: chunk.startOffsets[i] = curStart; 
            chunk.putEndOffset(i, curEnd-1);
            // was: chunk.endOffsets[i] = curEnd-1;
          } else {
            chunk.putStartOffset(i, curStart+j);
            // was: chunk.startOffsets[i] = curStart+j;
            chunk.putEndOffset(i, curStart+j);
            // chunk.endOffsets[i] = curStart+j;
          }
          

          // If the word specifically set matchStartFeature to true,
          // then set the default for this being a valid match to true,
          // otherwise set it to false.
          // Same for matchEndFeauture.
          boolean validMatchStart = validMatchStartDefault;
          boolean validMatchEnd = validMatchEndDefault;
          
          // if startWithWordStart is set, then make sure we do not have 
          // a valid match start unless we are at the first character of the word
          if(startWithWordStart && j!=0) {
            validMatchStart = false;
          }
          // if startWithWordEnd is set, then make sure we do not have 
          // a valid match end unless we are at the last character of the word
          if(endWithWordEnd && j!=(wordTextChars.length-1)) {
            validMatchEnd = false;
          }
          if(validMatchStart) {
            chunk.setIsValidMatchStart(i);
          }
          if(validMatchEnd) {
            chunk.setIsValidMatchEnd(i);
          }
          i++;
        }
      }
    } // for actualAnn
    if(debug) {
      System.out.println("Created chunk: "+chunk);
    }
    chunk.length = i;
    return chunk;
  }
  public static TextChunk makeChunk(Document document, Annotation ann, boolean caseNormalise,
      AnnotationSet processAnns, String wordAnnotationType, String wordAnnotationFeature, String spaceAnnotationType,
      boolean startWithWordStart, boolean endWithWordEnd) {
    return makeChunk(document, ann.getStartNode().getOffset(),ann.getEndNode().getOffset(),caseNormalise, processAnns, 
        wordAnnotationType,wordAnnotationFeature,spaceAnnotationType,
        startWithWordStart, endWithWordEnd);
  }
 
  public int getLength() {
    return length;
  }
  
  public String getTextString(int from, int to) {
    guardOffset(from);
    guardOffset(to);
    return new String(Arrays.copyOfRange(text, from, to+1));
  }
  
  public String getTextString() {
    return new String(text);
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nChunk: ");
    sb.append("from=").append(from).append(" to=").append(to).append(" length="+length);
    sb.append("\ntext=");
    sb.append(new String(text));
    sb.append("\nstartOffsets: ");
    for(int i=0; i<length; i++ ) {
      sb.append(getStartOffset(i)).append(",");
    }
    sb.append("\nstartWord: ");
    for(int i=0; i<length; i++ ) {
      sb.append(isValidMatchStart(i)).append(",");
    }
    sb.append("\nendOffsets: ");
    for(int i=0; i<length; i++ ) {
      sb.append(getEndOffset(i)).append(",");
    }
    sb.append("\nendWord: ");
    for(int i=0; i<length; i++ ) {
      sb.append(isValidMatchEnd(i)).append(",");
    }
    return sb.toString();
  }
  
  public boolean isEmpty() {
    return (length == 0);
  }
  
}
