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

package gate.trendminer.lodie.utils;

/**
 * Class to make it easy to return some Pair of values.
 * 
 * @author Johann Petrak
 */
public class Pair<T1,T2> {
  public T1 value1;
  public T2 value2;
  private Pair() {}
  public Pair(T1 f1, T2 f2) {
    value1=f1;
    value2=f2;
  }
  @Override
  public boolean equals(Object other) {
    if(other == null) { return false; }
    if(!(other instanceof Pair)) { return false; }
    if(other == this) { return true; }
    Pair<?,?> otherPair = (Pair)other;
    return value1.equals(otherPair.value1) && value2.equals(otherPair.value2);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 37 * hash + (this.value1 != null ? this.value1.hashCode() : 0);
    hash = 37 * hash + (this.value2 != null ? this.value2.hashCode() : 0);
    return hash;
  }
  
  @Override
  public String toString() {
    return "Pair("+value1+","+value2+")";
  }
  
}
