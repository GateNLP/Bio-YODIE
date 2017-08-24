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
 * Class to make it easy to return a triple (3-tuple) from a method.
 * 
 * @author Johann Petrak
 */
public class Tuple3<T1,T2,T3> {
  public T1 value1;
  public T2 value2;
  public T3 value3;
  private Tuple3() {}
  public Tuple3(T1 f1, T2 f2, T3 f3) {
    value1=f1;
    value2=f2;
    value3=f3;
  }
}
