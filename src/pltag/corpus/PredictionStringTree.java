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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.collections4.map.MultiValueMap;

public class PredictionStringTree extends CompositeStringTree {

	/**
	 * Generate a tree with no nodes but a name stub "Sentence # 10: "
	 * @param int tno
	 */
	private ArrayList<Integer> anchorlist = new ArrayList<Integer>();
	private int [] mapping = null;
//	private String [] mapping = null;
	
	public PredictionStringTree(int arraysize, boolean useSemantics) {
		super(useSemantics);
		makeArraysBigger(arraysize-1);
		auxtree = false;//default, is changed if corresponding 
		treeString = "prediction: ";// + leaf;
	}

	public PredictionStringTree copyPred(){
		PredictionStringTree copy = new PredictionStringTree(this.arraysize, useSemantics);
		copy.fullcategories = this.fullcategories.clone();
		copy.categories = this.categories.clone();
		copy.roles = this.roles.clone();
		copy.nodeTypes = this.nodeTypes.clone();
		copy.parent = this.parent.clone();
		copy.isHeadChild = this.isHeadChild.clone();
		copy.children = (HashMap<Integer, ArrayList<Integer>>) this.children.clone();
		copy.origin = new MultiValueMap();
		copy.origin.putAll(this.origin);
		copy.originDown = new MultiValueMap();
		copy.originDown.putAll(this.originDown);
		copy.originUp = new MultiValueMap();
		copy.originUp.putAll(this.originUp);
		//copy.indexUp = this.indexUp.clone();
		copy.root = this.root;
		copy.foot = this.foot;
		copy.auxtree = this.auxtree;
		copy.arraysize = this.arraysize;
		//copy.adjPossible = this.adjPossible.clone();
		return copy;
	}
	
	protected void addNode(CompositeStringTree oldtree, int nodeID, Collection connectionNodes, int currentWordNo){
		//int nodeID = Integer.parseInt(nodeID);
		
		categories[nodeID]= oldtree.categories[nodeID];
		fullcategories[nodeID]= oldtree.fullcategories[nodeID];
		roles[nodeID]= oldtree.roles[nodeID];
		parent[nodeID]= oldtree.parent[nodeID];

		if (origin == null) this.initializeOrigin();
		originUp.putAll(nodeID, (Collection<Integer>) oldtree.originUp.get(nodeID));
		originDown.putAll(nodeID, (Collection<Integer>) oldtree.originDown.get(nodeID));
		nodeTypes[nodeID]= oldtree.nodeTypes[nodeID];//MyNodeType.predicted;
		isHeadChild[nodeID]= oldtree.isHeadChild[nodeID];
		children.put(nodeID, oldtree.getChildren(nodeID));
		
		ArrayList<Integer> children = new ArrayList<Integer>();
		
		if (getChildren(nodeID)!=null){
			children.addAll(getChildren(nodeID)); 
		}
		else {
			return; 
		}
		boolean afterHead = false;
		//String headIndex = "";
		// keep all child nodes that are before the head child, or the foot, or needed for connectivity, 
		// OR IF NEEDED TO HAVE A LITTLE SPINE FOR PREDICTED BITS FROM THE TRACE ETC
		for (Integer child : getChildren(nodeID)){
			//int childid = Integer.parseInt(child);
			if (!connectionNodes.contains(child) && !child.equals(oldtree.foot) && afterHead){
				/*String childindex = oldtree.getLowestOrigin(child, oldtree.originUp);
				if (!headIndex.equals(childindex)){
					addNode(oldtree, child, connectionNodes);
				}
				else*/ children.remove(new Integer(child));
			}
			else if (oldtree.nodeTypes[child] == TagNodeType.internal && oldtree.isHeadChild(child)){
				afterHead = true;
				//headIndex = oldtree.getLowestOrigin(child, oldtree.originDown);
				if (categories[child]==null ){
					addNode(oldtree, child, connectionNodes, currentWordNo);
				}
			}
			else if (oldtree.nodeTypes[child]== TagNodeType.terminal || oldtree.nodeTypes[child]== TagNodeType.anchor ){
				Integer parent = oldtree.parent[child];
				if (oldtree.categories[child].contains("*") || oldtree.categories[child].equals("0")){
					addNode(oldtree, child, connectionNodes, currentWordNo);
					originUp.putAll(child, (Collection<Integer>)originDown.get(parent));
					originDown.putAll(child, (Collection<Integer>)originDown.get(parent));
					//this.children.put(parent, oldtree.getChildren(parent));
				}
				else{
					this.children.put(parent, new ArrayList<Integer>());
				}
				anchorlist.add(parent);
				return;
			}/*
			else if (oldtree.getLowestOrigin(child, oldtree.originUp).equals(oldtree.getLowestOrigin(child, oldtree.originDown))&&
					Integer.parseInt(oldtree.getLowestOrigin(child, oldtree.originDown))<currentWordNo){
				addNode(oldtree, child, connectionNodes, currentWordNo);
				this.children.put(child, new ArrayList<String>());
				this.originDown.remove(child, getLowestOrigin(child, originDown));
				this.nodeTypes[Integer.parseInt(child)] = TagNodeType.subst;
			}*/
			else if (categories[child]==null && !connectionNodes.contains(child) && !afterHead ){
				addNode(oldtree, child, connectionNodes, currentWordNo);
			}
		}
		this.children.put(nodeID, children);
	}
	


	public void removeNode(Integer child){
		super.removeNode(child);
//		super.removeNode(Integer.parseInt(child));
		originDown.remove(child);
		originUp.remove(child);
	}
	
	/**
	 * Prunes the prediction tree (which is a copy of the original tree) such that unnecessary nodes 
	 * (those to the right of the spine and those low on the spine) are removed.
	 */
	public void cutTail(Collection connectionNodes) {
		ArrayList<Integer> removelist = new ArrayList<Integer>();
		ArrayList<Integer> addlist = new ArrayList<Integer>();
		// start going up from each leaf node to see whether any nodes can be removed
		for (Integer child : anchorlist){
			Integer origchild = child;
//			int child = Integer.parseInt(child);
			if (child == Integer.MIN_VALUE) return;
//			if (child.equals("")) return;
			/*if (this.getChildren(child)!=null && !this.getChildren(child).isEmpty()){
				Integer cid = Integer.parseInt(this.getChildren(child).get(0));
				if (this.categories[cid].contains("*")||this.categories[cid].equals("0")){
			
					continue;
				}
			}*/
			Integer parent = this.parent[child];
			// while the connectionnodes don't contain this node (i.e. it is not needed for connectivity)
			// (special case for if the parent node is UCP, this is a hack: keep the node so the parser can attach things without special UCP treatment)
			// and the parent is not needed for connectivity either (otherwise leaf a stump)
			// and all the siblings are substitution nodes
			// and the current node does not have other children, or they are also substitution nodes
			while(!connectionNodes.contains(child) && this.getNodes().size()>2
					&& parent != null && !this.getCategory(parent).equals("UCP")
					//&& !connectionNodes.contains(parent)
					&& allsubst(children.get(parent), connectionNodes)
					&& (children.get(child)==null||children.get(child).isEmpty()||
							allsubst(children.get(child), connectionNodes))){
				if (connectionNodes.contains(parent)
					 && (!(this.getCategory(parent).equals(getCategory(child)) && children.get(parent).size()==1))){//TODO check if this correct!
					//System.out.println(this);
					break;
				}
				else{
					for (Integer cc : children.get(child)){
						this.removeNode(cc);
					}
					/*if (!this.getLowestOrigin(child, originUp).equals(this.getLowestOrigin(child, originDown))
							&& anchorlist.contains(child)){
						this.nodeTypes[childid] = TagNodeType.subst;
						this.originDown.remove(child);
						continue;
					}*/
					//remove node.
					ArrayList<Integer> cs = this.children.get(parent);
					if (cs == null){
//						System.out.println("here");
					}
					cs.remove(new Integer(child));
					this.removeNode(child);
					child = parent;
					parent = this.parent[child];
//					parent = this.parent[Integer.parseInt(child)];
				}
			}
			removelist.add(origchild);
			addlist.add(child);
			// remove any subst nodes under the stump.
			ArrayList<Integer> grandcs = children.get(child);
			if ( allsubst(grandcs, connectionNodes) && !connectionNodes.contains(child)){
				for (Integer cc : grandcs){
					this.removeNode(cc);
				}
				children.put(child, new ArrayList<Integer>());
			}
			
		}
		anchorlist.removeAll(removelist);
		anchorlist.addAll(addlist);
		//if anchor has two different indices, make them the same?
	}
	
	
	private boolean allsubst(ArrayList<Integer> name, Collection connectionNodes) {
		if (name == null || name.isEmpty()) return true;
		for (Integer child: name){
			ArrayList<Integer> childchild = children.get(child);
			if (childchild==null || childchild.isEmpty()){
			}
			else if(categories[childchild.get(0)].equals("0")){
//			else if(categories[Integer.parseInt(childchild.get(0))].equals("0")){
				return false;//used to be "true";
			}
			else return false;
			if (connectionNodes.contains(child)){
				return false;
			}
			if (categories[child].contains("*")){
//			if (categories[Integer.parseInt(child)].contains("*")){
				return false;
			}
			if (nodeTypes[child] == TagNodeType.subst){
//			if (nodeTypes[Integer.parseInt(child)] == TagNodeType.subst){
				return false;
			}
		}
		return true;
	}

	
	protected String printOrigin(Integer nodeId, MultiValueMap<Integer, Integer> origin) {
		if (mapping == null){
			makeMapping(origin);
		}
		if (origin.containsKey(nodeId)){
			Integer index = origin.getCollection(nodeId).iterator().next();
//			String index = (String) origin.getCollection(nodeIDstring).iterator().next();
			if (index>=mapping.length){
				makeMapping(origin);
			}
			return String.valueOf(mapping[index]);
//			return mapping[Integer.parseInt(index)];
		}
		return null;
	}
	
	protected String printOriginUp(int nodeId) {
		if (mapping == null){
			makeMapping(originUp);
		}
		if (originUp.containsKey(nodeId)){
			Integer index = originUp.getCollection(nodeId).iterator().next();
//			String index = (String) originUp.getCollection(nodeId).iterator().next();
			if (index>=mapping.length){
//			if (Integer.parseInt(index)>=mapping.length){
				makeMapping(originUp);
			}
			return String.valueOf(mapping[index]);
//			return mapping[Integer.parseInt(index)];
		}
		return null;
	}

	protected String printOriginDown(int nodeId) {
		if (mapping == null){
			makeMapping(originDown);
		}
		if (originDown.containsKey(nodeId)){
			Integer index = originDown.getCollection(nodeId).iterator().next();
//			String index = (String) originDown.getCollection(nodeId).iterator().next();
			if (index>=mapping.length){
//			if (Integer.parseInt(index)>=mapping.length){
				makeMapping(originDown);
			}
			return String.valueOf(mapping[index]);
//			return mapping[Integer.parseInt(index)];
		}
		return null;
		
		}	
		//return getOrigin(nodeIDstring, origin);
		/*
		if (getOrigin(nodeIDstring, origin)!=null){
			return "1";
		}		
		return null;//*/
	
//*/
    protected String printOriginUpChar(int nodeId)
    {
       if (mapping == null){
			makeMapping(originUp);
		}
		if (originUp.containsKey(nodeId)){
			Integer index = originUp.getCollection(nodeId).iterator().next();
			if (index>=mapping.length){
				makeMapping(originUp);
			}
			return mapping[index] < 10 ? String.valueOf((char) ('0' + mapping[index])) : String.valueOf(mapping[index]);
		}
		return "null";
    }

    protected String printOriginDownChar(int nodeId)
    {
        if (mapping == null){
			makeMapping(originDown);
		}
		if (originDown.containsKey(nodeId)){
			Integer index = originDown.getCollection(nodeId).iterator().next();
			if (index>=mapping.length){
				makeMapping(originDown);
			}
			return mapping[index] < 10 ? String.valueOf((char) ('0' + mapping[index])) : String.valueOf(mapping[index]);
		}
		return null;
    }
    
	private void makeMapping(MultiValueMap<Integer, Integer> origin) {
		Iterator valuit = origin.values().iterator();
		HashMap<Integer, Boolean> singlevals = new HashMap<Integer, Boolean>();
		int maxval = -1;
		while (valuit.hasNext()){
			Integer val = (Integer) valuit.next();
			singlevals.put(val, true);
			if (val > maxval){maxval = val;}
//			if (Integer.parseInt(val) > maxval){maxval = Integer.parseInt(val);}
		}
		Boolean[] v = new Boolean[maxval+1];
		for (Integer key : singlevals.keySet()){
//		for (String key : singlevals.keySet()){
//			int intkey= Integer.parseInt(intkey);
			v[key]= true;
		}
		mapping = new int[maxval+1];
		int j=1;
		for (int i = 0; i < v.length; i++){
			if (v[i] != null){
				mapping[i] = j;
//				mapping[i] = j+"";
				j++;
			}
		}
	}

	protected void addInfo(PredictionStringTree eT, int etRoot) {
		super.addInfo(eT, etRoot);
		if (eT.foot != Integer.MIN_VALUE){ nodeTypes[eT.foot] = TagNodeType.internal;}
//		if (!eT.foot.equals("")){ nodeTypes[Integer.parseInt(eT.foot)] = TagNodeType.internal;}
		anchorlist.addAll(eT.anchorlist);
		this.makeMapping(originUp);
	}
	
	public boolean hasUsefulNodes(Collection<Integer> cn, HashMap<Integer, Integer> mapper) {
		Iterator<Integer> it = cn.iterator();
		//remove all nodes that are not actually in tree
		ArrayList<Integer> removelist = new ArrayList<Integer>();
		while(it.hasNext()){
			Integer cnnode = it.next();
			if (cnnode >= arraysize ||getCategory(cnnode) == null){
//			if (Integer.parseInt(cnnode) >= arraysize ||getCategory(cnnode) == null){
				removelist.add(cnnode);
			}
		}
		cn.removeAll(removelist);
		// if no nodes in tree, then clearly no useful ones
		if (cn.isEmpty()) return false;
		// if one useful node in tree, check it's not the root or foot node of an aux tree
		if (cn.size()==1){
			Integer cnnode = (Integer) cn.iterator().next();
			if (cnnode >= arraysize || getCategory(cnnode) == null) return false;
//			if (Integer.parseInt(cnnode) >= arraysize || getCategory(cnnode) == null) return false;
			if (auxtree && cnnode.equals(foot)){
				if (mapper.containsKey(root)|| mapper.containsKey(foot)){
					//System.out.println("further mapping needed");
				}
				mapper.put(root, foot);
				mapper.put(foot, root);
				return false;
			}
			// can also remove if root of an initial tree???
			else if (cnnode.equals(root)){//&& auxtree){
				if (auxtree){
					if (mapper.containsKey(root)|| mapper.containsKey(foot)){
						//System.out.println("further mapping needed");
					}
					mapper.put(root, foot);
					mapper.put(foot, root);
				}
				return false;
				
			}
			else{
				return true;
			}
		}
		// if two useful nodes in tree, check they're not the foot and root node
		else if (cn.size()==2){
			if (auxtree && cn.contains(foot) && cn.contains(root)){
				if (mapper.containsKey(root)|| mapper.containsKey(foot)){
					//System.out.println("further mapping needed");
				}
				mapper.put(root, foot);
				mapper.put(foot, root);
				return false;
			}
			else {
				Iterator cnit = cn.iterator();
				boolean hasUsefulNode = false;
				while(cnit.hasNext()){
					Integer cnel = (Integer) cnit.next();
					if (this.arraysize>cnel && getCategory(cnel) !=null){
//					if (this.arraysize>Integer.parseInt(cnel) &&getCategory(cnel) !=null){
						hasUsefulNode = true;
					}
				}
				
				return hasUsefulNode;
			}
		}
		//else there must be at least one useful non-foot non-root node
		else 
			return true;
	}
	
	public boolean hasUsefulNodes2() {
		//for (String node : categories.keySet()){
		ArrayList<Integer> cats = new ArrayList<Integer>();
		for (int i =0; i< categories.length; i++){
			Integer node = i;
			if (originDown.get(node)!= null && originUp.get(node)!= null && originDown.get(node).equals(originUp.get(node))){
				return true;
			}
			if (categories[i]!=null) cats.add(node);
		}	
		if (cats.size() == 2 && cats.contains(root) && cats.contains(foot)){
			return false;
		}
		return true;
	}
	
	public ArrayList<Integer> getAnchorList(){
		return this.anchorlist;
	}
	
}
