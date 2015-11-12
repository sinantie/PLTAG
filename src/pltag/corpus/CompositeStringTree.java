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

import fig.basic.LogInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;


public class CompositeStringTree extends StringTree {

	private ArrayList<CompositeStringTree> notConnected = new ArrayList<CompositeStringTree>();
	
	/**
	 * Generate a tree with no nodes but a name stub "Sentence # 10: "
	 * @param int tno
	 */
	public CompositeStringTree(int tno, boolean useSemantics) {
		super(useSemantics);
		auxtree = false;
		treeid = "Sentence # "+ tno + ": ";
		treeString = "";
	}
	
	public CompositeStringTree(boolean useSemantics) {
		super(useSemantics);
		treeid = "";
		treeString = "";
	}

	/**
	 * Iterates through the unconnected structures and tries to combine trees if possible.
	 * 
	 
	public void joinUnConnected2(){		
		int i = 0;
		int oldSize = notConnected.size()+1;
		while (notConnected.size() < oldSize){
			oldSize = notConnected.size();
			i = 0;
			while(i < notConnected.size()){
				CompositeStringTree unconnectedTree = notConnected.remove(i);
				ArrayList<CompositeStringTree> rmtreelist = integrate(unconnectedTree);
				if (!rmtreelist.contains(unconnectedTree)){
					notConnected.add(unconnectedTree);
				}
				i++;
			}
		}
	}

	
	public void joinUnConnected(){
		int i = 0;
		while(i < notConnected.size()){
			ArrayList<CompositeStringTree> rmlist =new ArrayList<CompositeStringTree>();
			integrateUnconnected(rmlist);
			notConnected.removeAll(rmlist);
			int j = i+1;
			while(j < notConnected.size()){
				CompositeStringTree firstTree = new CompositeStringTree();
				firstTree.integrate(notConnected.get(i));
				CompositeStringTree secondTree = new CompositeStringTree();
				secondTree.integrate(notConnected.get(j));
				//System.err.println(firstTree.treename + " " + secondTree.treename + "\n");
				for (CompositeStringTree rmtree : firstTree.integrate(secondTree)){
					notConnected.remove(i);
					notConnected.remove(j-1); //because first remove shifts list elements that are after i by one to left.
					notConnected.add(rmtree);
					j--;//because element at this position removed, repeat with this element.
				}
				j++;
			}
			i++;
		}
		
		if (notConnected.size() > 0){
			joinUnConnected2();
		}
		if (notConnected.size() > 0) {
			treeString = "Unconnected: " + treeString;			
		}
	}	
*/

	
	public void joinUnConnected(){
		boolean successful = true;
		while(notConnected.size()>0 && successful){
			ArrayList<CompositeStringTree> rmlist =new ArrayList<CompositeStringTree>();
			integrateUnconnected(rmlist);
			notConnected.removeAll(rmlist);
			if (rmlist.isEmpty()) successful = false;
		}
		if (notConnected.size() > 0) {
			treeString = "Unconnected: " + treeString;			
		}
	}	
	/**
	 * Combines another string tree with the current tree if possible.
	 * The other string tree could for example be generated from a lexicon entry.
	 */
	public ArrayList<CompositeStringTree> integrate(CompositeStringTree eT) {
		ArrayList<CompositeStringTree> rmlist = new ArrayList<CompositeStringTree>();
		if (treeString.equals("")){treeString = eT.treeString;}
		else {treeString += " "+eT.treeString;}
		if (eT.categories.length < categories.length){
			eT.makeArraysBigger(categories.length-1);
		}
		else if (categories.length < eT.categories.length){
			this.makeArraysBigger(eT.categories.length-1);
		}
		if (isEmpty(categories)){//first word
		//if (categories.size() == 0){//first word
			root = eT.getRoot();
			addInfo(eT, root);
			foot = eT.foot;
			auxtree = eT.auxtree;
			coordanchor = eT.coordanchor;
		}
		else{
			if (tryToIntegrate(eT, root, eT.root)){
				rmlist.add(eT);
				}
			else if ((coordanchor != Integer.MIN_VALUE &&	tryToIntegrate(eT, coordanchor, eT.root))
//			else if ((!coordanchor.equals("")&&	tryToIntegrate(eT, coordanchor, eT.root))
					||(coordAnchorList.size()==2 && tryToIntegrate(eT, coordAnchorList.get(0), eT.root))
					||(eT.coordanchor != Integer.MIN_VALUE &&	tryToIntegrate(eT, root, eT.coordanchor))
//					||(!eT.coordanchor.equals("")&&	tryToIntegrate(eT, root, eT.coordanchor))
					|| (eT.coordAnchorList.size()==2 && tryToIntegrate(eT, root, eT.coordAnchorList.get(0)))){
				notConnected.remove(eT);
				if (eT.coordAnchorList.contains(eT.root)) eT.originUp.remove(eT.root);//WAS COMMENTED OUT VERA
				rmlist.add(eT);
			}
			integrateUnconnected(rmlist);
			
		}
		return rmlist;
	}
	
	private void integrateUnconnected(ArrayList<CompositeStringTree> rmlist) {
		//try to integrate unconnected trees;
		ArrayList<StringTree> removelist = new ArrayList<StringTree>();
		for (int i = 0; i<notConnected.size(); i++){
			CompositeStringTree ncT = notConnected.get(i);
			if (ncT.auxtree && !ncT.hasFootLeft()) continue;
			if ((tryToIntegrate(ncT, root, ncT.root))
					|| (coordanchor != Integer.MIN_VALUE && tryToIntegrate(ncT, coordanchor, ncT.root))
//					|| (!coordanchor.equals("")&& tryToIntegrate(ncT, coordanchor, ncT.root))
					||(coordAnchorList.size()==2 && tryToIntegrate(ncT, coordAnchorList.get(0), ncT.root))
					||(ncT.coordanchor != Integer.MIN_VALUE && tryToIntegrate(ncT, root, ncT.coordanchor))
//					||(!ncT.coordanchor.equals("") && tryToIntegrate(ncT, root, ncT.coordanchor))
					||(ncT.coordAnchorList.size()==2 && tryToIntegrate(ncT, root, ncT.coordAnchorList.get(0)))){
				removelist.add(ncT);
				if (ncT.coordAnchorList.contains(ncT.root)) ncT.originUp.remove(ncT.root);//WAS COMMENTED OUT VERA
				rmlist.add(ncT);					
			}
		}
		notConnected.removeAll(removelist);
		for (int i = notConnected.size()-1; i>=0; i--){
			CompositeStringTree ncT = notConnected.get(i);
			if (ncT.auxtree&& !ncT.hasFootLeft()) {
				if ((tryToIntegrate(ncT, root, ncT.root))
						|| (coordanchor != Integer.MIN_VALUE && tryToIntegrate(ncT, coordanchor, ncT.root))
//						|| (!coordanchor.equals("")&& tryToIntegrate(ncT, coordanchor, ncT.root))
						||(coordAnchorList.size()==2 && tryToIntegrate(ncT, coordAnchorList.get(0), ncT.root))
						||(ncT.coordanchor != Integer.MIN_VALUE && tryToIntegrate(ncT, root, ncT.coordanchor))
//						||(!ncT.coordanchor.equals("") && tryToIntegrate(ncT, root, ncT.coordanchor))
						||(ncT.coordAnchorList.size()==2 && tryToIntegrate(ncT, root, ncT.coordAnchorList.get(0)))){
					notConnected.remove(ncT);
					if (ncT.coordAnchorList.contains(ncT.root)) ncT.originUp.remove(ncT.root);//WAS COMMENTED OUT VERA
					rmlist.add(ncT);					
				}
			}
		}
		//notConnected.removeAll(removelist);
	}

	private boolean isEmpty(String[] categories) {
		for (int i = 0; i< categories.length; i++){
			if (categories[i]!=null){
				return false;
			}
		}
		return true;
	}

	/**
	 * Does the real integration work of merging two trees together using substitution or adjunction
	 * @param eT
	 * @param thisroot
	 * @param etRoot
	 * @return
	 */
        private boolean tryToIntegrate(CompositeStringTree eT, int thisroot, int etRoot) {
//		int etRoot = Integer.parseInt(etRoot);		
		if (thisroot == Integer.MIN_VALUE){
			thisroot = 255;
		}
//		else thisroot = Integer.parseInt(thisroot);
//		if new tree eT can be substituted into current tree
//		TODO the below condition is probably not universal
		if //((!eT.auxtree||! coordanchor.equals("")) && getSubstNodes().contains(etRoot)){
			(!eT.auxtree && getSubstNodes().contains(etRoot)){
			if (eT.root != etRoot){// should normally be the case, but not necessary if nested coordination. CAREFUL: In the original comparison between strings et.root, and etRoot
				eT.coordanchor = etRoot;
			}
			if (!categories[etRoot].equals(eT.categories[etRoot])){
				LogInfo.error("category clash! "+categories[etRoot]+" "+eT.categories[etRoot]+ 
						": "+etRoot+" 1\n");
			}
			addInfo(eT, etRoot);//TODO: fix other stuff.
			
			holes[etRoot]= false;
			nodeTypes[etRoot]= TagNodeType.internal;
			//parent[etRoot]= parent[etRoot];
			children.put(etRoot, eT.children.get(etRoot));

			eT.root = etRoot;
			//coordanchor = eT.coordanchor;
		}
		//if current tree can be substituted into eT
		else if (!auxtree && eT.getSubstNodes().contains(thisroot)){// 
				if (!categories[thisroot].equals(eT.categories[thisroot])){
					LogInfo.error("category clash! "+categories[thisroot]+" "+eT.categories[thisroot]+": "+thisroot+" 2\n");
				}
			ArrayList<Integer> cs = children.get(thisroot);
			addInfo(eT, etRoot);
			//origin.putAll(thisroot, eT.origin.getCollection(thisroot));
			holes[thisroot]= false;
			nodeTypes[thisroot]= TagNodeType.internal;
			parent[thisroot]= eT.parent[thisroot];
			children.put(thisroot, cs);
			auxtree = eT.auxtree;
			root= etRoot;
			foot = eT.foot;
			coordanchor = eT.coordanchor;
			eT.root = etRoot;
		}//if current tree can be adjoined into eT
		else if (auxtree 
				&& eT.categories[thisroot]!=null
				&& eT.nodeTypes[thisroot] == TagNodeType.internal
				//&& !etRoot.equals(thisroot)
				){
			if (!categories[thisroot].equals(eT.categories[thisroot])){
				LogInfo.error("category clash! "+categories[thisroot]+" "+eT.categories[thisroot]+": "+thisroot+" 4\n");
			}
			ArrayList<Integer> etrootcs = eT.children.get(thisroot);
//			athematic tree
			if (etrootcs.contains(foot) ){//&& !eT.foot.endsWith("-f")){
				//only remove annotation from eTfootnode (children, nodeType, hole, foot)
				//don't delete anything but overwrite.otherwise elementary trees are destroyed.
				ArrayList<Integer> oldFootCs = new ArrayList<Integer>();
				oldFootCs.addAll(eT.children.get(foot));
				//create children for eT.root node, preserving the order:
				ArrayList<Integer> mergedCs = eT.mergeChildren(foot,this.getChildren(this.getRoot()), thisroot);
				Integer oldFoot = foot;
				addInfo(eT, etRoot);
				children.put(thisroot, mergedCs);
				//etFoot children:
				children.put(oldFoot, oldFootCs);
				nodeTypes[oldFoot] =  TagNodeType.internal;
//				nodeTypes[Integer.parseInt(oldFoot)] =  TagNodeType.internal;
			}
			else {
				Integer auxparent = eT.parent[thisroot];
				Integer auxroot = thisroot;
				Integer auxfoot = foot;
				ArrayList<Integer> oldFootCs = children.get(auxroot);
				ArrayList<Integer> newFootCs = eT.children.get(auxroot);
				addInfo(eT, etRoot);
				
				//children.put(root, children.get(root));
				children.put(auxfoot, newFootCs);
				children.put(auxroot, oldFootCs);
				for (Integer c : newFootCs){
					parent[c]= auxfoot;
//					parent[Integer.parseInt(c)]= auxfoot;
				}
				parent[thisroot]= auxparent;
				
				nodeTypes[auxfoot]= TagNodeType.internal;
//				nodeTypes[Integer.parseInt(auxfoot)]= TagNodeType.internal;
			}
			//put origin values into etOrigin, then copy etorigin to origin.
			/*Iterator it = origin.getCollection(root).iterator();
			while(it.hasNext()){
				Object next = it.next();
				if (!eT.origin.containsValue(foot, next)){
					eT.origin.put(foot, next);
				}
			}
			Iterator it = eT.origin.getCollection(root).iterator();
			while(it.hasNext()){
				Object next = it.next();
				if (!origin.containsValue(foot, next)){
					origin.put(foot, next);
					originDown.put(foot, next);
				}

				if (originUp.containsValue(etRoot, next)){
					originDown.remove(etRoot, next);
				}
				if (originDown.containsValue(foot, next)){
					originUp.remove(foot, next);
				}
			}
			*/
			Integer elementtreeindex = eT.originDown.getCollection(root).iterator().next();
//			String elementtreeindex = (String) eT.originDown.getCollection(root).iterator().next();
			origin.put(foot, elementtreeindex);
			if (!originDown.containsValue(foot, elementtreeindex)){
				originDown.put(foot, elementtreeindex);// only relevant for athematic trees.
			}
			originDown.remove(root, elementtreeindex);
			originUp.remove(foot, elementtreeindex); // only relevant for athematic trees.
			
			
			//origin.putAll(eT.origin);
			//origin.putAll(eT.foot, eT.origin.get(eT.root));
			holes[foot]= false;
//			holes[Integer.parseInt(foot)]= false;
			foot =eT.foot;
			auxtree = eT.auxtree;
			root = etRoot;//should this be eT.root????
			coordanchor = eT.coordanchor;
		}

//		if new tree can be adjoined into current tree
		else if (eT.isAuxtree() 
				&& categories[etRoot]!=null 
				&& nodeTypes[etRoot]==TagNodeType.internal
				//&& !etRoot.equals(thisroot)
				){
			ArrayList<Integer> rootcs = children.get(etRoot);
			int eTfoot = eT.foot;
//			int eTfoot = Integer.parseInt(eT.foot);
			
			if (!categories[etRoot].equals(eT.categories[etRoot])){
				//System.err.print(eT.foot + "category clash! "+categories.get(etRoot)+" "+eT.categories.get(etRoot)+": "+etRoot+" 3\t"+eT.print() + this.print()+"\n" );
			}
			//String eTrootnode = etRoot;
            // athematic tree
			if (rootcs.contains(eT.foot)){
				//only remove annotation from eTfootnode (children, nodeType, hole, foot)
				//don't delete anything but overwrite.otherwise elementary trees are destroyed.
				ArrayList<Integer> oldeTFootCs = new ArrayList<Integer>();
				oldeTFootCs.addAll(children.get(eT.foot));
				//create children for eT.root node, preserving the order:
				ArrayList<Integer> mergedCs = mergeChildren(eT.foot, eT.children.get(etRoot), etRoot);
				addInfo(eT, etRoot);
				children.put(etRoot, mergedCs);
				//etFoot children:
				children.put(eT.foot, oldeTFootCs);				
			}
			// athematic tree after another tree was adjoined at the same node
			// for example in probably will, where the order of nodes is 9 -> 9-f -> 10, but the auxtree
			// expects 10 to be the child of 9.
			else if (categories[eT.foot]!=null){
//			else if (categories[Integer.parseInt(eT.foot)]!=null){
				//only remove annotation from eTfootnode (children, nodeType, hole, foot)
				//don't delete anything but overwrite. otherwise elementary trees are destroyed.
				ArrayList<Integer> oldeTFootCs = new ArrayList<Integer>();
				oldeTFootCs.addAll(children.get(eT.foot));				
				//create children for eT.root node, preserving the order:
				ArrayList<Integer> mergedCs = mergeChildren(eT.foot, eT.children.get(etRoot), parent[eTfoot]);
				HashMap<Integer, ArrayList<Integer>> etchildren = new HashMap<Integer, ArrayList<Integer>>();
				etchildren.putAll(eT.children);
				eT.children.remove(etRoot);//need to add this later, in order to preserve lexentries!
				
				for (Integer cint : mergedCs){
//					int cint = Integer.parseInt(cint);
					parent[cint]= parent[eTfoot];
					eT.parent[cint]=null;
				}
				addInfo(eT, etRoot);//TODO! osrt out problem with commas and gold.
				eT.children = etchildren;
				children.put(parent[eTfoot], mergedCs);
				//etFoot children:
				children.put(eT.foot, oldeTFootCs);
			}
			else {
				ArrayList<Integer> newFootCs = children.get(etRoot);
				addInfo(eT, etRoot);
				children.put(etRoot, eT.children.get(etRoot));
				children.put(eT.foot, newFootCs);
				for (Integer c : newFootCs){
					parent[c]= eT.foot;
//					parent[Integer.parseInt(c)]= eT.foot;
				}
			}
			nodeTypes[eTfoot]= TagNodeType.internal;
			holes[eTfoot]= false;
			
			Integer preftreeindex = originDown.getCollection(etRoot).iterator().next();
//			String preftreeindex = (String) originDown.getCollection(etRoot).iterator().next();
			origin.put(eT.foot, preftreeindex);
			if (!originDown.containsValue(eT.foot, preftreeindex)){
				originDown.put(eT.foot, preftreeindex);// only relevant for non-athematic trees.
			}
//			originDown.remove(etRoot, preftreeindex); // In the original version the key is String, but an int is used instead, so the particular line never fires
			originUp.remove(eT.foot, preftreeindex); // only relevant for athematic trees.
			/*
			Iterator it = origin.getCollection(eT.root).iterator();
			while(it.hasNext()){
				Object next = it.next();
				if (!origin.containsValue(eT.foot, next)){
					origin.put(eT.foot, next);
					originDown.put(eT.foot, next);
					//originUp.put(etRoot, next);
				}
				if (originUp.containsValue(etRoot, next)){
					originDown.remove(etRoot, next);
				}
				if (originDown.containsValue(eT.foot, next)){
					String etfoot = eT.foot;
					MultiValueMap ou = originUp;
					originUp.remove(eT.foot, next);
				}
			}
			//origin.putAll(eT.origin);
			*/
			eT.root = etRoot;
		}
		else {
			if (!notConnected.contains(eT)){
				notConnected.add(eT);
			}
			return false;
		}
		
		return true;
	}
	
	

	private ArrayList<Integer> getSubstNodes(){
		ArrayList<Integer> substnodes = new ArrayList<Integer>();
		//for (String nid : nodeTypes.keySet()){
		for (int i = 0; i < nodeTypes.length; i++){
			if (nodeTypes[i]!=null && nodeTypes[i] == TagNodeType.subst){
				substnodes.add(i);
//				substnodes.add(i+"");
			}
		}
		return substnodes;
	}
	
	/**
	 * 
	 * @param node:footnode id (where new children are inserted
	 * @param auxtreeCs: children of auxtree (those that need to be inserted?)
	 * @param root: new parent node for merged children 
	 * @return
	 */
	private ArrayList<Integer> mergeChildren(Integer node, ArrayList<Integer> auxtreeCs, int root){
		ArrayList<Integer> maintreeCs = this.children.get(root); 
		ArrayList<Integer> rightorderCs = new ArrayList<Integer>();
		//children of real tree up to node, then children of auxtree up to node then node, 
		//then auxtree children after node then main tree children after node.
		//!!! wrong! children of real tree up to node, then among that node children up to where children have
		// already been seen (approximation of fringe).
		for (int i = 0; i < maintreeCs.indexOf(node); i++){
			Integer child = maintreeCs.get(i);
			rightorderCs.add(child);
		}
		for (int i = 0; i < auxtreeCs.indexOf(node); i++){
			Integer child = auxtreeCs.get(i);
			rightorderCs.add(child);
		}
		rightorderCs.add(node);
		for (int i =  auxtreeCs.indexOf(node)+1; i < auxtreeCs.size(); i++){
			Integer child = auxtreeCs.get(i);
			rightorderCs.add(child);
		}
		for (int i = maintreeCs.indexOf(node)+1; i < maintreeCs.size(); i++){
			Integer child = maintreeCs.get(i);
			rightorderCs.add(child);
		}	
		return rightorderCs;
	}
	
	/**
	 * 
	 * @param eT
	 * @param etRoot
	 */
	@SuppressWarnings("unchecked")
        protected void addInfo(CompositeStringTree eT, int etRoot) {
		ArrayList<Integer> cs = null;
		//int etRoot = Integer.parseInt(etRoot);
		Integer pnode= Integer.MIN_VALUE;
		if (!eT.root.equals(etRoot)){
			pnode= eT.parent[etRoot];
			cs = children.get(pnode);
			//par = parent.get(eT.parent.get(eT.coordanchor));
			//if (!categories.keySet().contains(eT.root)){
			if (categories[eT.root]==null){
//			if (categories[Integer.parseInt(eT.root)]==null){
				root = eT.root;
				auxtree = eT.auxtree;
				foot = eT.foot;
			}
			if (parent[etRoot] != null){
				Integer nodesAboveCoordanchor = parent[etRoot];
//				int nodesAboveCoordanchor = Integer.parseInt(parent[etRoot]);
				
				while (nodesAboveCoordanchor < eT.arraysize && eT.categories[nodesAboveCoordanchor]!=null){
					Integer parentNode = parent[nodesAboveCoordanchor];			
					if (children.containsKey(nodesAboveCoordanchor)){
						for (int noachild : children.get(nodesAboveCoordanchor)){
							if (noachild != eT.coordanchor){
//							if (!noachild.equals(eT.coordanchor)){
								eT.removeNode(noachild);
//								eT.removeNode(Integer.parseInt(noachild));
							}
						}
					}
					eT.removeNode(nodesAboveCoordanchor);
					if (parentNode == null){
						break;
					}
					nodesAboveCoordanchor = parentNode;
//					nodesAboveCoordanchor = Integer.parseInt(parentNode);
				}
			}
		}
		
		if (eT.categories.length > categories.length){
			this.makeArraysBigger(eT.categories.length);
		}
		
		categories = (String[]) putAllRootAndNew(categories,eT.categories, eT);
		fullcategories = (String[]) putAllRootAndNew(fullcategories,eT.fullcategories, eT);
		putAllRootAndNew2(children,eT.children, eT);
		parent = (Integer[]) putAllRootAndNew(parent,eT.parent, eT);
		nodeTypes = (TagNodeType[]) putAllRootAndNew(nodeTypes,eT.nodeTypes, eT);
		holes = (Boolean[]) putAllRootAndNew(holes, eT.holes, eT);
		isHeadChild = (Boolean[]) putAll(eT.isHeadChild, isHeadChild);
		roles = (String[]) putAllRootAndNew(roles, eT.roles, eT);
		for (Integer etOrigin : (Set<Integer>) eT.origin.keySet()){
			if (origin == null) this.initializeOrigin();
			
			origin.putAll(etOrigin, (Collection) eT.origin.get(etOrigin));
		}
		for (Integer etOrigin : (Set<Integer>) eT.originDown.keySet()){
			if (eT.originDown.getCollection(etOrigin).contains(null)){
				
			}
			else
			originDown.putAll(etOrigin, (Collection<Integer>) eT.originDown.get(etOrigin));
		}
		for (Integer etOrigin : (Set<Integer>) eT.originUp.keySet()){
			if (eT.originUp.getCollection(etOrigin).contains(null)){
				
			}
			else
			originUp.putAll(etOrigin, (Collection<Integer>) eT.originUp.get(etOrigin));
		}
		
		if (eT.coordanchor != Integer.MIN_VALUE && cs!=null){
//		if (!eT.coordanchor.equals("")&& cs!=null){
			children.put(pnode, cs);
		}
	}
	
	private Object[] putAllRootAndNew(Object[] target, Object[] source, StringTree eT) {
		if (eT.root == eT.coordanchor | eT.coordanchor == Integer.MIN_VALUE){
//		if (eT.root == eT.coordanchor | eT.coordanchor.equals("")){
			target = putAll(source, target);
		}
		else{
			for (int i = 0; i<source.length; i++){//(String s : source.keySet()){
				
				if (target[i]!=null && eT.coordanchor != i){
//				if (target[i]!=null && !eT.coordanchor.equals(i)){
				}
				else if (source[i]!=null){
					target[i] =  source[i];
				}
			}
		}		
		return target;
	}
	
	
	
	private Object[] putAll(Object[] source, Object[] target) {
		for (int i = 0; i<source.length; i++){
			if (source[i]!=null){
				target[i] = source[i];
			}
		}
		return target;
	}

	private void putAllRootAndNew2(HashMap<Integer,ArrayList<Integer>> target, HashMap<Integer,ArrayList<Integer>> source, StringTree et) {
		if (et.root == et.coordanchor | et.coordanchor == Integer.MIN_VALUE){
//		if (et.root == et.coordanchor | et.coordanchor.equals("")){
			target.putAll(source);//TODO!! need to merge?!
		}
		else{
			for (Integer s : source.keySet()){
				if (target.containsKey(s)&& !s.equals(et.coordanchor)){
				}
				else{
					target.put(s, source.get(s));
				}
			}
		}
	}
	/*
	private void putAllRootAndNew1(String[] target, String[] source, StringTree eT) {
		if (eT.root == eT.coordanchor | eT.coordanchor.equals("")){
			target = (String[]) putAll(source, target);
//			target.putAll(source);
		}
		else{
			for (int i = 0; i<source.length; i++){//(String s : source.keySet()){
				
				if (source[i]!=null && target[i]!=null && !eT.coordanchor.equals(i)){
				}
				else{
					target[i] =  source[i];
				}
			}
		}
	}

	private void putAllRootAndNew3(TagNodeType[] target, TagNodeType[] source, StringTree et) {
		if (et.root == et.coordanchor | et.coordanchor.equals("")){
			target.putAll(source);
		}
		else{
			for (String s : source.keySet()){
				if (target.containsKey(s)&& !s.equals(et.coordanchor)){
					
				}
				else{
					target.put(s, source.get(s));
				}
			}
		}
	}
	private void putAllRootAndNew4(Boolean[] target, Boolean[] source, StringTree et) {
		if (et.root == et.coordanchor | et.coordanchor.equals("")){
			target.putAll(source);
		}
		else{
			for (String s : source.keySet()){
				if (target.containsKey(s)&& !s.equals(et.coordanchor)){
					
				}
				else{
					target.put(s, source.get(s));
				}
			}
		}
	}
	*/
}
