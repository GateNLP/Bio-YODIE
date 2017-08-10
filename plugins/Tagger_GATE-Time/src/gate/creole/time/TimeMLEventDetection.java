/*
 *  TimeMLEventDetection.java
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
 * Mark A. Greenwood, 09/05/2016
 */

package gate.creole.time;

import gate.creole.PackagedController;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.AutoInstanceParam;
import gate.creole.metadata.CreoleResource;

@CreoleResource(name = "TimeML Event Detection", autoinstances = @AutoInstance(parameters = {
  @AutoInstanceParam(name="pipelineURL", value="resources/applications/tml-events-ml-application.gapp"),
  @AutoInstanceParam(name="menu", value="TimeML")}),
    comment = "TimeML Event Detection Application")
public class TimeMLEventDetection extends PackagedController {

  private static final long serialVersionUID = 2014950451164916200L;

}
