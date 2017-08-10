/*
 *  DctParsingFormat.java
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

/**
 * Hardcoded DCT Format information for use with DCTParser.
 * 
 * @author Jannik Strötgen
 */
public enum DctParsingFormat {
	TIMEML ("timeml"),
	MANUALDATE ("manualdate"),
	;

	private String formatName;
	
	DctParsingFormat(String formatName) {
		this.formatName = formatName;
	}
	
	/*
	 * getter
	 */
	
	public final String getName() {
		return this.formatName;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}


