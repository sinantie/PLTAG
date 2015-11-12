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
package pltag.parser;

import org.apache.commons.collections4.map.MultiValueMap;



public class UnkLexicon extends Lexicon {

	
	public UnkLexicon(String filename) {
		MultiValueMap<String, String>[] entries = read(filename);//inserting unk here not working.
		//lexentriesTree = makeLextrees(entries[0], "ARG");
		//lexentriesTree.putAll(makeLextrees(entries[1], "MOD"));
		lexEntriesTree = makeLexStrings(entries[0], "ARG");//need to fix makeLexStrings for UNK
		lexEntriesTree.putAll(makeLexStrings(entries[1], "MOD"));
	}
	
	
	//currently only for trees with one lex root TODO 
        @Override
	protected String getPosFromTreeString(String treeString, String key) {
		String[] words = key.split(" ");
		StringBuilder posWord = new StringBuilder();
		for (String w : words){
			String ts = treeString;
			//cut off everything after key word.
			String pos = "";
			if (ts.contains(w+"<>")){
				ts = ts.substring(0, ts.indexOf(w+"<>"));
				pos = ts.substring(ts.lastIndexOf("( ")+2, ts.lastIndexOf("^"));
				w = "unk";
			}
			posWord.append(pos).append(" ").append(w).append("\t");	
		}
		String posw = posWord.toString();
		posw = posw.trim();
		return posw;
	}

}
