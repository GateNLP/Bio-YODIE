package com.jpetrak.gate.stringannotation.extendedgazetteer.trie;

public class Utils {
  
  /** 
   * Converts two characters two an int number
   * 
   * @param chars
   * @return
   */
  public static int twoChars2Int(char ch, char cl) {
    int l = (ch << 16) + cl;
    return l;
  }
  
  public static int twoChars2Int(char[] cs) {
    if(cs.length != 2) {
      throw new RuntimeException("Called twoChars2Int with an array that does not have 2 elements but "+cs.length);
    }
    return twoChars2Int(cs[0],cs[1]);    
  }
  
  /**
   * Converts an int into an array of two characters with the high bits
   * in the first element and the low bits in the second element
   * @param i
   * @return
   */
  public static char[] int2TwoChars(int i) {
    char[] asChars = new char[2];
    asChars[0] =  (char)(i >>> 16);
    asChars[1] =  (char)(i & 0xFFFF);
    return asChars;
  }
  
  /**
   * Sets two characters at position pos to the representation of int as two successive 
   * characters.
   * @param i
   * @param chars
   * @param pos
   */
  public static void setTwoCharsFromInt(int i, char[] chars, int pos) {
    chars[pos] =  (char)(i >>> 16);
    chars[pos+1] =  (char)(i & 0xFFFF);    
  }
  
  
  /** 
   * Converts four characters two a long number
   * 
   * @param chars
   * @return
   */
  public static long fourChars2Long(char c4, char c3, char c2, char c1) {
    long l = c1;
    l += ((long)c2) << 16;
    l += ((long)c3) << 32;
    l += ((long)c4) << 48; 
    return l;
  }
  
  public static long fourChars2Long(char[] cs) {
    if(cs.length != 4) {
      throw new RuntimeException("Called fourChars2Long with an array that does not have 4 elements but "+cs.length);
    }
    return fourChars2Long(cs[0],cs[1],cs[2],cs[3]);
  }
  
  
  /**
   * Converts a long to a char array of four elements
   * @param i
   * @return
   */
  public static char[] long2FourChars(long i) {
    char[] asChars = new char[4];
    asChars[0] =  (char)(i >>> 48);
    asChars[1] =  (char)((i >>> 32) & 0xFFFF);
    asChars[2] =  (char)((i >>> 16) & 0xFFFF);
    asChars[3] =  (char)(i & 0xFFFF);
    return asChars;
  }
  
  
}
