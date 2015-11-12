/* 
 * Copyright (C) 2015 ikonstas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pltag.corpus;


import java.util.ArrayList;


public class NomBank extends Bank {

	public NomBank(String dir, String sourcefile) {
		super(dir, sourcefile);
	}
	
	protected Entry processLine(String line) {
		String[] list = line.split("(\t| )");
		String key = list[0];
		Entry entry = new Entry();
		entry.file = key;
		entry.treeNumber = list[1];
		entry.wordNumber = list[2];
		entry.baseForm = list[3];
		entry.senseNumber = list[4];
		ArrayList<String> deps = new ArrayList<String>();
		for (int i = 5; i< list.length; i++){
			deps.add(list[i]);
		}
		entry.setArgsMods(deps);
		return entry;
	}
}
