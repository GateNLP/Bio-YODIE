/* 
 * Copyright (C) 2015-2016 The University of Sheffield.
 *
 * This file is part of YodiePlugin.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package gate.miscutils;

import com.jpetrak.gate.jdbclookup.*;
import gate.trendminer.lodie.utils.LodieUtils;


/**
 * A class to help with properly creating a canonical version of an URI/IRI.
 * The main method for this class is canonicalize(String uriString) which returns a canonical
 * version of the passed URI. This implements the "standard" way of creating a canonical URI
 * as it is currently used within YODIE. <br>
 * NOTE that this is currently very specific to DBpedia, so URIs/IRIs or IDs used with other
 * KBs may need other forms of canoncialization or none at all. 
 * 
 * @author Johann Petrak
 * 
 */
public class UriCanonicalization {
  
  private JdbcString2StringLR interlanguage = null;
  private JdbcString2StringLR redirects = null;
  private JdbcString2StringLR iri2uri = null;
  private JdbcString2StringLR disambiguations = null;
  
  /** 
   * Create an UriCanoncialization object that makes use of the given tables.
   * Any or all of the JdbcLR objects may be null in which case the respective canonicalization
   * is not performed. 
   * The following steps are performed:
   * <ul>
   * <li>If the URI/IRI is not already shortened, shorten it
   * <li>unescape Unicode escapes of the form \\uXXXX
   * <li>replace the URI by the IRI from the iri2uri table, if iri2uri JdbcLR is given
   * <li>fix any coding/escaping still left 
   * <li>replace a non-English URI with the sameAs English URI, if it exists.
   * <li>replace with transitive redirect URI, if redirects JdbcLR is given
   * <li>return null if the resulting URI is a disambiguation URI, if disambiguations JdbcLR is given
   * </ul>
   * <p>
   * NOTE: not sure if non-English URIs should first get redirected and then mapped to English
   * URIs or the other way round? Potentially we also could need to redirect, map to English and
   * redirect again? Experimentation needed!
   * <p>
   * NOTE: filtering the disambiguation URIs can be dangerous!
   * <p>
   * @param redirects
   * @param iri2uri
   * @param disambiguations 
   */
  public UriCanonicalization(JdbcString2StringLR interlanguage, JdbcString2StringLR redirects, JdbcString2StringLR iri2uri, JdbcString2StringLR disambiguations) {
    this.interlanguage = interlanguage;
    this.redirects = redirects;
    this.iri2uri = iri2uri;
    this.disambiguations = disambiguations;
  }
  
  
  public String canonicalize(String uri) {
    uri = LodieUtils.shortenUri(uri);
    uri = LodieUtils.unescapeUnicode(uri);
    if(iri2uri != null) {
      String tmp = iri2uri.get(uri);
      if(tmp != null) uri = tmp;
    }    
    String uriOrig = uri;
    uri = LodieUtils.recodeUri(uri);
    if(interlanguage != null) {
      String tmp = interlanguage.get(uri);
      if(tmp != null) uri = tmp;
    }
    if(redirects != null) {
      String tmp = redirects.get(uri);
      if(tmp != null) uri = tmp;
    }
    if(!uri.equals(uriOrig)) {
      uriOrig = uri;
      if(interlanguage != null) {
        String tmp = interlanguage.get(uri);
        if(tmp != null) uri = tmp;
        if(!uriOrig.equals(uri)) {
          if(redirects != null) {
            tmp = redirects.get(uri);
            if(tmp != null) uri = tmp;
          }
        }
      }
    }
    if(disambiguations != null) {
      if(disambiguations.contains(uri)) {
        return null;
      }
    }
    return uri;
  }
}
