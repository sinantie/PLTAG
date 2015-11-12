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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PredictiveLexicon {
	HashMap<String, ArrayList<String>> pairsfirst = new HashMap<String, ArrayList<String>>();
	HashMap<String, ArrayList<String>> pairssecond = new HashMap<String, ArrayList<String>>();
	public PredictiveLexicon(String file) {
		readPredictiveLex(file);
	}
	
	private void readPredictiveLex(String filename2) {
//		declared here only to make visible to finally clause
		BufferedReader input = null;
		try {//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			input = new BufferedReader( new FileReader(filename2) );
			String line = null; //not declared within while loop
			while (( line = input.readLine()) != null){//TODO
				String[] list = line.split("\t");
				//ArrayList<String> array = new ArrayList<String>();	
				ArrayList<String> al;
				if (pairsfirst.containsKey(list[0])){
					al = pairsfirst.get(list[0]);
				}
				else {
					al = new ArrayList<String>();					
				}
				al.add(list[1]);
				pairsfirst.put(list[0], al);
				
				ArrayList<String> al2;
				if (pairssecond.containsKey(list[1])){
					al2 = pairssecond.get(list[1]);
				}
				else {
					al2 = new ArrayList<String>();					
				}
				al2.add(list[0]);
				pairssecond.put(list[1], al2);
			}
		}
		catch (FileNotFoundException ex) {ex.printStackTrace();}
		catch (IOException ex){ex.printStackTrace();}
		finally {
			try {if (input!= null) {input.close();}}
			catch (IOException ex) {ex.printStackTrace();}
		}
	}	
	

	public LeafNode check(int i, ArrayList<LeafNode> leaves) {
		// check whether current leaf node is in predictive lexicon
		//if it is, return corresponding matching node
		String leafcontent = leaves.get(i).getLeaf();
		if (pairsfirst.containsKey(leafcontent)){
			ArrayList namesOfSecond = pairsfirst.get(leafcontent);
			for (int j = i; j < leaves.size(); j++){
				LeafNode leaf = leaves.get(j);
				if (namesOfSecond.contains(leaf.getLeaf())){
					return leaf;
				}
			}
			//do the following on penn corpus class level:
			//more sophisticated: only return matching node if they are under the same tree.
			//make sure that partial trees meet! (like connection path???) 
			
			return null;//return matching node.
		}
		else if (pairssecond.containsKey(leafcontent)){
			ArrayList namesOfSecond = pairssecond.get(leafcontent);
			for (int j = i; j >= 0; j--){
				LeafNode leaf = leaves.get(j);
				if (namesOfSecond.contains(leaf.getLeaf())){
					return leaf;
				}
			}
			//do the following on penn corpus class level:
			//more sophisticated: only return matching node if they are under the same tree.
			//make sure that partial trees meet! (like connection path???) 
			
			return null;//return matching node.
		}
//		if not return null;
		else return null;
		}

}
