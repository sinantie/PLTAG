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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LexEntry{

	private LeafNode leafpointer; 
	private ArrayList<LexEntry> multientry;
	private String word;
	private String baseform="";
	private Boolean auxtree;
	private String rootcategory;
	private Map<Integer, TagNode> nodes = new TreeMap<Integer, TagNode>();
	//private int wordno;
	//private Boolean hole;
	private Boolean multilex = false;
	private Boolean partofMultilex = false;
	private String expression;
	private TagNode root;
	private TagNode fatherNode;
	
	/**
	 * Constructor (based on the anchor for the lexicon entry)
	 * @param leaf
	 */
	public LexEntry(LeafNode leaf) {
		this.leafpointer = leaf;
		this.word = leaf.getLeaf();
		if (leaf.getBaseForm()!= null){
			baseform = leaf.getBaseForm();
		}
		if (baseform.equals("")){
			expression = word;
		}
		else expression =  word;//baseform;//
		nodes.put(leaf.getNodeID(), leaf);
		multientry = null;
	}
	

	/**
	 * Determines if the argument/ modifier status of an elemantary tree can be guessed. 
	 * 
	 * @return
	 */
	public boolean guessArgMod() {
		//System.err.println(category.concat("-").concat(argmod));
		boolean assigned = new GuessArgMod(this).hasHeuristic();
				
		if (assigned){ 
			root.setArgModGuess();
			return true;
		}
		return false;
	}
	
	

	/**
	 * Create lexicon entries with more than 1 anchor.
	 * 
	 */
    // don't destruct lexEntry for support; 
	// get expression correct; 
	// shadow status only in Stringtree???
	public void mergeLexentries(LexEntry support) {
//		figure out which one is root node, category root node,
		//(figure out whether auxiliary or substitution node, i.e. same as finding category node)
		if (getAncestorIDs().contains(support.getRoot().getNodeID()) ){
			if (auxtree) return;
			if (support.getRoot().getArgMod().equals("UNDEF")){
				setUndef(); 
				}
			else if (support.isAuxtree()) setAuxtree();
			else setSubsttree();
			//keep them separate.
			return;
			//root = support.getRoot();
			//leafpointer = support.getMainLeafNode();
		}                                
//		else if (support.getAncestorIDs().contains(root.getNodeID())){
//		else if (support.getAncestorIDs().contains(root)){ // suspicious call of contains directly to TagNode
//			//currently never used!!!
//			//substtree must not be an auxiliary tree
//			if (!support.getRoot().isArgument()) return;
//		}
		// treatment for predictive lexicon entries: both and, either or, neither nor etc., and subcategorized PPs for verbs
		else if (support.getRoot().getParent() == root.getParent()){
			addPredictedWord(support);
			//TODO update nodes!
			setMultiLex();
			//note down as not alone-standing lexicon entry.
			support.setPartOfMultiLex();

			additionalLexfoot(support);
			support.additionalLexfoot(this);
			return;
		}
		else if (getNodes().containsValue(support.root.getParent()) || 
                         support.getAncestorIDs().contains(support.root.getNodeID())){			
			setMultiLex();
			//note down as not alone-standing lexicon entry.
			support.setPartOfMultiLex();
			//add to leaf list! TODO

			additionalLexfoot(support);
			support.additionalLexfoot(this);
			//add nodes of all support entries
			Iterator<Integer> i = support.getNodes().keySet().iterator();
			while(i.hasNext()){
				TagNode subnode = (TagNode) support.getNodes().get(i.next());
				addNode(subnode);
				//subnode.setLexEntry(this);
			}
			/*root = support.getRoot();
			//leafpointer = support.getMainLeafNode();
			if (root.getArgMod().equals("UNDEF")){
				this.setSubsttree();
			}
			else 
				if (root.getArgMod().equals("ARG")){
				this.setSubsttree();
			}
			else if (root.getArgMod().equals("MOD")){
				this.setAuxtree();
			}
			*/
		}
		else{
			System.err.println("Problem in LexEntry multiple lex entries, relationship between parts");
		}

	}
	
   /**
	* Treatment for predictive lexicon entries: both and, either or, neither nor etc.
	*/ 
	private void addPredictedWord(LexEntry support) {
		addNode(root.getParent());
		for (TagNode cs : root.getParent().getChildlist()){//root.getParent().getLexEntry().getRoot().getChildlist()
			cs.setArgument();
			//root.getParent().getLexEntry().getRoot().getHeadChild().setArgument();
		}
		TagNode originalHeadChild = root.getParent().getHeadChild();
		
		originalHeadChild.makeNotHead();
		support.getRoot().makeHead();
		//this.getRoot().makeHead();
		
		TagNode node = support.getRoot();		
		while (node.isHead()){
			//adds current node
			addNode(node);
			if (!support.getNodes().containsKey(node.getNodeID())){
				node.setLexEntry(this);
			}
			TagNode p = node.getParent();
			node = p;	
		}		
//		check whether tree is arg or mod = auxiliary or substitution tree
		addNode(node);
		node.setLexEntry(this);			
		setRoot(node);
		if (originalHeadChild.getArgMod().equals("ARG")){// && 
			setSubsttree();
			node.setArgument();
		}
		else if (originalHeadChild.getArgMod().equals("MOD")){
			setAuxtree();
			node.setModifier();
		}
		rootcategory = root.getCategory();
	}


	/**
	 * Stores an additional Lexical foot for a lexicon entry.
	 * @param le
	 */
	private void additionalLexfoot(LexEntry le){
		if (multientry == null){
			multientry = new ArrayList<LexEntry>();
		}
		/**
		if (baseform.equals("")){
			//expression = le.expression + word;
		}
		else expression = baseform;// word;
		*/
		multientry.add(le);
	}
	

	/**
	 * Adds a node to the set of nodes of this lexicon entry
	 * @param node
	 */
	public void addNode(TagNode node) {
		nodes.put(node.getNodeID(), node);
	}
	
	
	/**
	 * Prints out the structure into a bracketed notation.
	 * @param node
	 * @return
	 */
	public String getStruct(TagNode node) {
		if (node.isLeaf()){
			return "( " + node.getCategory() + ":" + node.getNodeID() + " "
			             + ((LeafNode)node).getLeaf() + ":" + ((LeafNode)node).getLeafNumber() + ")";
		}		
		Iterator i = node.getChildlist().iterator();
		String childlist = "";
		while (i.hasNext()){
			TagNode child = (TagNode) i.next();
			//if the node is part of this tree: (automatically solves the domain of locality problem)
			if (child.getLexEntry() == this){
				childlist= childlist.concat(" ").concat(getStruct(child));
			}
			//if not mark with substitution sign if argument or don't print if modifier
			//TODO! what about flat structure? expand this tree?!?
			else{
				if (child.isArgument()){
					childlist = childlist + " " + child.getFullCategory() + ":" 
						+ child.getNodeID()+ "! ";
				}
				else{
					childlist = childlist + " " + child.getFullCategory() + ":" 
						+ child.getNodeID() + "-" + child.getArgMod();
				}
			}
		}
		return "( " + node.getCategory() + ":" + node.getNodeID() + childlist + ")";
	}


	
	/**
	 * ACCESSORS AND SMALL METHODS FROM HERE
	 */
	public String getExpression(){
		return expression;
	}
	
	public LeafNode getMainLeafNode(){
		return leafpointer;
	}
	
	public LeafNode getFirstLeafNode(){
		if (multilex || partofMultilex){
			ArrayList<LexEntry> les = getMultiEntry();
			int smallest = leafpointer.getLeafNo();
			LeafNode firstleaf = leafpointer;
			for (LexEntry le : les){
				if (le.getMainLeafNode().getLeafNo() < smallest){
					smallest = le.getMainLeafNode().getLeafNo();
					firstleaf = le.getMainLeafNode();
				}
			}
			return firstleaf;
		}
		return leafpointer;
	}
	
	public ArrayList<LexEntry> getMultiEntry(){
		return multientry;
	}

	public void setAuxtree(){
		auxtree = true;
	}
	
	public void setSubsttree(){
		auxtree = false;
	}
	
	public void setUndef(){
		auxtree = null;
	}
	
	public Boolean isAuxtree(){
		return auxtree;
	}
	
	public void setRootCat(String rc){
		rootcategory = rc;
	}
	
	public String getRootCat(){
		return rootcategory;
	}
	
	public Map<Integer, TagNode> getNodes(){
		return nodes;
	}

	public void setMultiLex() {
		multilex = true;
	}
	
	public boolean isMultiLex() {
		return multilex;
	}

	public void setPartOfMultiLex() {
		partofMultilex = true;		
	}

	public boolean isPartOfMultiLex() {
		
		return partofMultilex;
	}

	public List<Integer> getAncestorIDs() {
		List<Integer> ancestors = new ArrayList<Integer>();
		TagNode node = root;
		while (node.getParent()!= null){
			ancestors.add(node.getNodeID());
			node = node.getParent();
		}
		return ancestors;
	}

	public void setRoot(TagNode node) {
		setRootCat(node.getCategory());
		//setRootNodeId(node.getNodeID());
		root = node;		
	}
	
	public TagNode getRoot(){
		return root;
	}
	

	public void setFather(TagNode parent) {
		fatherNode = parent;		
	}	
	
	public LexEntry getFatherEntry(){
		return fatherNode.getLexEntry();
	}

	public boolean hasFatherEntry() {
		if (fatherNode!= null)
			return true;
		return false;
	}
	
	public String toString(){
		return getStruct(root);
	}
}
