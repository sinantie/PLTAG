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

import java.util.List;
import org.apache.commons.collections4.map.MultiValueMap;

public class ConnectionPathCalculator
{

    private CompositeStringTree stringTree;
    private List<Integer> connectedNodes = new ArrayList<Integer>();
    private List<PredictionStringTree> predictedTrees = new ArrayList<PredictionStringTree>();
    //private HashMap<String,ElementaryStringTree> sentenceWordLex;
    private ElementaryStringTree[] sentenceWordLex;
    private HashMap<Integer, Integer> noOfSources = new HashMap<Integer, Integer>();
    private boolean useSemantics;
    
    /**
     * Generate a tree with no nodes but a name stub "Sentence # 10: "
     * @param sentenceWordLex 
     * @param sentenceWordLex 
     * @param lexicon 
     * @param int tno
     */
    public ConnectionPathCalculator(CompositeStringTree tree, ElementaryStringTree[] sentenceWordLex, boolean useSemantics)
    {//HashMap<String, ElementaryStringTree> sentenceWordLex){
        stringTree = tree;
        //System.out.println(stringtree.print());
        this.sentenceWordLex = sentenceWordLex;
        for (int i = 0; i < 10; i++)
        {
            noOfSources.put(i, 0);
        }
        this.useSemantics = useSemantics;
    }

    /**
     * Initialized with a sentence tree's root node, then calls itself recursively (tree depth-first traversal).
     * 
     * @param node
     */
    public int calculateConnectionPath(int node, int currentLeafNumber, List<StringTree> lexicon)
    {
//        int node = Integer.parseInt(node);
        // walk through tree depth-first
        // on way back up, check whether all nodes have numbers that are covered by the leaves seen.        
        if ((stringTree.nodeTypes[node] == TagNodeType.anchor
                || stringTree.nodeTypes[node] == TagNodeType.terminal))
        {
            if (stringTree.categories[node].equals("0") || stringTree.categories[node].contains("*"))
            {
                //System.out.println("null element");
            }
            else
            {
                currentLeafNumber = (Integer) stringTree.originDown.getCollection(node).iterator().next();
                connectedNodes.add(node);
                for (PredictionStringTree pt : predictedTrees)
                {
                    for (int i = 0; i < pt.categories.length; i++)
                    {
                        if (pt.categories[i] != null && connectedNodes.contains(i))
//                        if (pt.categories[i] != null && connectedNodes.contains(i + ""))
                        {
                            connectedNodes.remove(new Integer(i));
//                            connectedNodes.remove(i + "");
                        }
                    }
                }
                if (stringTree.origin.getCollection(node) == null)
                {
                    LogInfo.error("cpc, line 36 null pointer.");
                }
                List<PredictionStringTree> localPredictedTrees = generatePredictionTrees(currentLeafNumber, lexicon);
                predictedTrees.addAll(localPredictedTrees);
                connectedNodes = new ArrayList<Integer>();
            }
        }
        else if (stringTree.nodeTypes[node] == TagNodeType.subst)
        {
            LogInfo.error("Construction problem at original tree -  unconnected?.");
            if (!connectedNodes.contains(node) && currentLeafNumber != Integer.MIN_VALUE)
//            if (!connectedNodes.contains(node) && !currentLeafNumber.equals(""))
            {
                connectedNodes.add(node);
            }
            //return "???"; //This means that the tree was constructed incorrectly.
        }
        else
        {
            // don't add nodes twice, and don't add nodes if we haven't read the first word yet 
            // (remove this second condition to always require a sentence
            if (!connectedNodes.contains(node) && currentLeafNumber != Integer.MIN_VALUE)
//            if (!connectedNodes.contains(node) && !currentLeafNumber.equals(""))
            {
                connectedNodes.add(node);
            }
            for (Integer child : stringTree.children.get(node))
            {
                currentLeafNumber = calculateConnectionPath(child, currentLeafNumber, lexicon);
            }
        }
        return currentLeafNumber;
    }

    /**
     * Calculates which prediction trees need to be generated from the set of connectionNodes.
     * And combines prediction trees if possible.
     * 
     * @param currentLeafNumber
     * @param lexicon 
     * @return
     */
    @SuppressWarnings("unchecked")// cast to Collection<String>
    private List<PredictionStringTree> generatePredictionTrees(int currentLeafNumber,
                                                                    List<StringTree> lexicon)
    {//, String leaf) {
        //if (currentLeafNumber.equals("6"))
        //System.out.print("");
        MultiValueMap predictionTreeNodeMap = findNodesWithGreaterLeafnumbers(currentLeafNumber);
        ArrayList<PredictionStringTree> localPredictedTrees = new ArrayList<PredictionStringTree>();
        int sourcetreeno = 0;
        ArrayList<PredictionStringTree> unhelpfulpredtrees = new ArrayList<PredictionStringTree>();
        HashMap<Integer, Integer> translations = new HashMap<Integer, Integer>();
        //System.out.println("prediction tree to connect leaf " + currentLeafNumber + " number of pred trees needed: "+ predictionTreeNodeMap.size());
        //System.out.println("nodes needed: "+predictionTreeNodeMap);
        for (Integer predictionTreeOrigin : (Collection<Integer>) predictionTreeNodeMap.keySet())
        {
            ElementaryStringTree originalStringTree = (ElementaryStringTree) sentenceWordLex[predictionTreeOrigin];
//            ElementaryStringTree originalStringTree = (ElementaryStringTree) sentenceWordLex[Integer.parseInt(predictionTreeOrigin)];
            if (originalStringTree == null)
            {
                continue;
            }
            originalStringTree.makeLexiconEntry();
            if (originalStringTree.categories[originalStringTree.root] != null)
//            if (originalStringTree.categories[Integer.parseInt(originalStringTree.root)] != null)
            {
                translations.putAll(originalStringTree.removeUnaryNodes(originalStringTree.root));
            }
            else
            {
                translations.putAll(originalStringTree.removeUnaryNodes(originalStringTree.coordanchor));
            }
            if (originalStringTree.getAnchor() == Integer.MIN_VALUE && originalStringTree.treeString.startsWith("*"))
//            if (originalStringTree.getAnchor().equals("") && originalStringTree.treeString.startsWith("*"))
            {
                continue;
            }
            Collection<Integer> cn = predictionTreeNodeMap.getCollection(predictionTreeOrigin);
            PredictionStringTree predictionTree = buildPredictionTree(originalStringTree, cn, currentLeafNumber);
//            PredictionStringTree predictionTree = buildPredictionTree(originalStringTree, cn, Integer.parseInt(currentLeafNumber));
            predictionTree.cutTail(cn);
            if (predictionTree.hasUsefulNodes(cn, translations))
            {
                predictionTree = buildPredictionTree(originalStringTree, cn, currentLeafNumber);
//                predictionTree = buildPredictionTree(originalStringTree, cn, Integer.parseInt(currentLeafNumber));
                sourcetreeno++;
                //System.out.println(predictionTree.print());
                ArrayList<PredictionStringTree> newlist = new ArrayList<PredictionStringTree>();
                ArrayList<PredictionStringTree> removelist = new ArrayList<PredictionStringTree>();
                // combine prediction trees (trees can always be combined! I think.)
                for (PredictionStringTree otherTree : localPredictedTrees)
                {
                    PredictionStringTree ct = combinePredTrees(predictionTree, otherTree, predictionTreeNodeMap.values(), translations);//.copyPred();
                    if (ct != null)
                    {
                        removelist.add(otherTree);
                        removelist.add(predictionTree);
                        newlist.remove(predictionTree);
                        newlist.add(ct);
                        predictionTree = ct;
                    }
                }
                if (predictionTree.isAuxtree())
                {
                    localPredictedTrees.add(predictionTree);
                }
                else
                {
                    localPredictedTrees.add(0, predictionTree);
                }
                localPredictedTrees.removeAll(removelist);
//				might add too much here.
                for (PredictionStringTree npt : newlist)
                {
                    if (predictionTree.isAuxtree())
                    {
                        localPredictedTrees.add(npt);
                    }
                    else
                    {
                        localPredictedTrees.add(0, npt);
                    }
                }
            }
            else
            {
                predictionTree = buildPredictionTree(originalStringTree, cn, currentLeafNumber);
//                predictionTree = buildPredictionTree(originalStringTree, cn, Integer.parseInt(currentLeafNumber));
                unhelpfulpredtrees.add(predictionTree);
            }
        }
        if (localPredictedTrees.isEmpty() & unhelpfulpredtrees.size() > 0)
        {
            PredictionStringTree first = null;
            int min = Integer.MAX_VALUE;
//			String others = "";
            for (PredictionStringTree pt : unhelpfulpredtrees)
            {
                if (pt.isAuxtree())
                {
                    continue;
                }
                int anchorpos = pt.getAnchorList().get(0);
//                int anchorpos = Integer.parseInt(pt.getAnchorList().get(0));
                if (anchorpos < min)
                {
                    min = anchorpos;
                    if (first != null)
                    //				others += " @ "+first.toString();
                    {
                        first = pt;
                    }
                }
                //		else{ 
                //			if (first !=null)
                //			others += " @ "+first.toString();
                //		}
            }
            if (first != null)
            {
                //		System.out.println(first+"\t"+others);
                localPredictedTrees.add(first);
            }
        }//*/
        if (localPredictedTrees.size() > 1)
        {
            PredictionStringTree predictionTree = localPredictedTrees.get(0);
            ArrayList<PredictionStringTree> newlist =  new ArrayList<PredictionStringTree>();
            ArrayList<PredictionStringTree> removelist = new ArrayList<PredictionStringTree>();
            for (int i = 1; i < localPredictedTrees.size(); i++)
            {
                PredictionStringTree otherTree = localPredictedTrees.get(i);
                PredictionStringTree ct = combinePredTrees(predictionTree, otherTree, predictionTreeNodeMap.values(), translations);//.copyPred();
                if (ct != null)
                {
                    removelist.add(otherTree);
                    removelist.add(predictionTree);
                    newlist.remove(predictionTree);
                    newlist.add(ct);
                    predictionTree = ct;
                }
            }
            if (predictionTree.isAuxtree()){
                    localPredictedTrees.add(predictionTree);
            }
            else {
                    localPredictedTrees.add(0, predictionTree);
            }
            localPredictedTrees.removeAll(removelist);
//				might add too much here.
            for (PredictionStringTree npt : newlist){
                    if (predictionTree.isAuxtree()){
                            localPredictedTrees.add(npt);
                    }
                    else {
                            localPredictedTrees.add(0, npt);
                    }
            }
        }
        for (PredictionStringTree pst : localPredictedTrees)
        {
            pst.cutTail(predictionTreeNodeMap.values());
        }

        if (localPredictedTrees.size() > 1)
        {
            LogInfo.error("unaccounted case! combination of prediction trees; number of trees: " + sourcetreeno + " to connect leaf " + currentLeafNumber);
        }
//        noOfSources.put(sourcetreeno + "", noOfSources.get(sourcetreeno + "").intValue() + 1);
        noOfSources.put(sourcetreeno, noOfSources.get(sourcetreeno) + 1);
        return localPredictedTrees;
    }

    private PredictionStringTree combinePredTrees(PredictionStringTree predictionTree, PredictionStringTree otherTree,
                                                  Collection cn, HashMap<Integer, Integer> translations)
    {
        // if the other tree contains the root of the prediction tree (predicted tree can be adjoined or substituted into the other tree)
        ArrayList<PredictionStringTree> removelist = new ArrayList<PredictionStringTree>();
        PredictionStringTree combinedTree = null;
        Integer predTreeRoot = predictionTree.getRoot();
        Integer otherTreeRoot = otherTree.root;
        Integer predTreeFirstOriginDownForOwnRoot = predictionTree.originDown.getCollection(predTreeRoot).iterator().next();
//        String predTreeFirstOriginDownForOwnRoot = ((String) predictionTree.originDown.getCollection(predTreeRoot).iterator().next());
        Integer otherTreeFirstOriginDownForOwnRoot = otherTree.originDown.getCollection(otherTreeRoot).iterator().next();
//        String otherTreeFirstOriginDownForOwnRoot = ((String) otherTree.originDown.getCollection(otherTreeRoot).iterator().next());
        if (!otherTree.children.containsKey(predTreeRoot) && otherTree.children.containsKey(translations.get(predTreeRoot)))
        {
            predTreeRoot = translations.get(predTreeRoot);
        }
        if (!predictionTree.children.containsKey(otherTreeRoot) && predictionTree.children.containsKey(translations.get(otherTreeRoot)))
        {
            otherTreeRoot = translations.get(otherTreeRoot);
        }
        if (otherTree.children.containsKey(predTreeRoot) && !predictionTree.children.containsKey(otherTree.getRoot()))
        {
//			substitute prediction tree into other tree
            if (!predictionTree.isAuxtree()
                    && otherTree.originDown.get(predTreeRoot) == null
                    && otherTree.originUp.getCollection(predTreeRoot) != null
                    && stringTree.originDown.getCollection(predTreeRoot).contains(predTreeFirstOriginDownForOwnRoot)
                    && stringTree.originUp.getCollection(predTreeRoot).contains(
                    otherTree.originUp.getCollection(predTreeRoot).iterator().next()))
//                    ((String) otherTree.originUp.getCollection(predTreeRoot).iterator().next())))
            {
                otherTree.addInfo(predictionTree, predTreeRoot);
                //otherTree.cutTail(cn);
                //System.out.println(otherTree.print());
                combinedTree = otherTree;
                removelist.add(predictionTree);
            }
//		 	adjoin prediction tree into other tree
            else if (predictionTree.isAuxtree()
                    && (stringTree.origin.getCollection(predTreeRoot).contains(predTreeFirstOriginDownForOwnRoot)
                    || stringTree.origin.getCollection(predictionTree.getRoot()).contains(predTreeFirstOriginDownForOwnRoot))
                    && (otherTree.originUp.getCollection(predTreeRoot) == null
                    || stringTree.origin.getCollection(predTreeRoot).contains(
                    otherTree.originUp.getCollection(predTreeRoot).iterator().next())))
//                    (String) otherTree.originUp.getCollection(predTreeRoot).iterator().next())))
            {
                if (otherTree.isAuxtree() && otherTree.foot.equals(predictionTree.foot))
                {
                    return null;
                }
                ArrayList<Integer> oldchildren = otherTree.children.get(predTreeRoot);
                if (oldchildren.contains(predictionTree.foot))
                {
                    oldchildren = otherTree.children.get(predictionTree.foot);
                }
                else
                {
                    //System.out.println("");
                }
                Integer oldOriginUp = otherTree.getLowestOrigin(predTreeRoot, otherTree.originUp);
                Integer oldOriginDown = otherTree.getLowestOrigin(predTreeRoot, otherTree.originDown);

                TagNodeType ipiNodeType = otherTree.getNodeType(predTreeRoot);
                otherTree.addInfo(predictionTree, predictionTree.getRoot());
                otherTree.putNodeType(predictionTree.foot, ipiNodeType);
                if (ipiNodeType != TagNodeType.subst && ipiNodeType != TagNodeType.foot
                        && !otherTree.originDown.containsValue(predictionTree.foot, oldOriginDown))
                {
                    otherTree.originDown.put(predictionTree.foot, oldOriginDown);
                }
                otherTree.children.put(predictionTree.foot, oldchildren);
                for (Integer c : oldchildren)
                {
                    otherTree.parent[c] = predictionTree.foot;
//                    otherTree.parent[Integer.parseInt(c)] = predictionTree.foot;
                }
                //need to do proper adjunction with new child etc. 
                //correct originannotation at otherTree.getRoot() and otherTree.foot
                //otherTree.originDown.put(predictionTree.foot, otherTreeFirstOriginDownForOwnRoot);
                while (otherTree.originDown.containsValue(predTreeRoot, oldOriginDown))
                {
                    otherTree.originDown.remove(predTreeRoot, oldOriginDown);
                }
                while (otherTree.originUp.containsValue(predictionTree.foot, oldOriginUp))
                {
                    otherTree.originUp.remove(predictionTree.foot, oldOriginUp);
                }
                combinedTree = otherTree;
                //repair in case of node name mismatch.
                if (!predTreeRoot.equals(predictionTree.root))
                {
                    combinedTree.children.put(predTreeRoot, combinedTree.children.get(predictionTree.root));
                    combinedTree.putNodeType(predTreeRoot, combinedTree.getNodeType(predictionTree.root));
                    if (combinedTree.originDown.containsKey(predictionTree.root))
                    {
                        combinedTree.originDown.put(predTreeRoot, combinedTree.originDown.getCollection(predictionTree.root).iterator().next());
                    }
                    if (combinedTree.originUp.containsKey(predictionTree.root))
                    {
                        combinedTree.originUp.put(predTreeRoot, combinedTree.originUp.getCollection(predictionTree.root).iterator().next());
                    }
                    combinedTree.putParent(predTreeRoot, combinedTree.getParent(predictionTree.root));
                }
                removelist.add(predictionTree);
            }
        }
        // if the prediction tree contains the root of the other tree (other tree can be adjoined or substituted into the prediction tree)
        if (predictionTree.children.containsKey(otherTreeRoot) && !removelist.contains(predictionTree))
        {
            //String predTreeFirstOriginDownForOtherTreeRoot = ((String)predictionTree.originDown.getCollection(otherTree.getRoot()).iterator().next());
//			substitute other tree into prediction tree
            if (!otherTree.isAuxtree()
                    && predictionTree.originDown.get(otherTreeRoot) == null
                    && stringTree.origin.getCollection(otherTreeRoot).contains(otherTreeFirstOriginDownForOwnRoot)
                    && stringTree.origin.getCollection(otherTreeRoot).contains(
                    predictionTree.originUp.getCollection(otherTreeRoot).iterator().next()))
//                    (String) predictionTree.originUp.getCollection(otherTreeRoot).iterator().next()))
            {
                predictionTree.addInfo(otherTree, otherTreeRoot);
                //fix substitution node such that no "null" in OriginUp (from substituted node).
                combinedTree = predictionTree;
                removelist.add(otherTree);
            }
            // adjoin other tree into prediction tree
            else if (otherTree.isAuxtree()
                    && (stringTree.originDown.getCollection(otherTreeRoot).contains(otherTreeFirstOriginDownForOwnRoot)
                    || stringTree.originDown.getCollection(otherTree.root).contains(otherTreeFirstOriginDownForOwnRoot))
                    && predictionTree.originUp.containsKey(otherTreeRoot)
                    && (stringTree.originUp.getCollection(otherTreeRoot).contains(
                    predictionTree.originUp.getCollection(otherTreeRoot).iterator().next())
//                    (String) predictionTree.originUp.getCollection(otherTreeRoot).iterator().next())
                    || predictionTree.originUp.getCollection(otherTreeRoot).iterator().next() == null))
            {
                if (predictionTree.isAuxtree() && otherTree.foot.equals(predictionTree.foot))
                {
                    return null;
                }
                ArrayList<Integer> oldchildren = predictionTree.children.get(otherTreeRoot);
                if (oldchildren.contains(otherTree.foot))
                {
                    oldchildren = predictionTree.children.get(otherTree.foot);
                }
                Integer oldOriginUp = predictionTree.getLowestOrigin(otherTreeRoot, predictionTree.originUp);
                Integer oldOriginDown = predictionTree.getLowestOrigin(otherTreeRoot, predictionTree.originDown);
                TagNodeType ipiNodeType = predictionTree.getNodeType(otherTreeRoot);
                predictionTree.addInfo(otherTree, otherTree.root);
                predictionTree.putNodeType(otherTree.foot, ipiNodeType);
                if (ipiNodeType != TagNodeType.subst && ipiNodeType != TagNodeType.foot
                        && !predictionTree.originDown.containsValue(otherTree.foot, oldOriginDown))
                {
                    predictionTree.originDown.put(otherTree.foot, oldOriginDown);
                }
                predictionTree.children.put(otherTree.foot, oldchildren);

                for (Integer c : oldchildren)
                {
                    predictionTree.parent[c] = otherTree.foot;
//                    predictionTree.parent[Integer.parseInt(c)] = otherTree.foot;
                }
                //correct originannotation at otherTree.getRoot() and otherTree.foot
                //need to do proper adjunction with new child etc.

                while (predictionTree.originDown.containsValue(otherTreeRoot, oldOriginDown))
                {
                    predictionTree.originDown.remove(otherTreeRoot, oldOriginDown);
                }
                while (predictionTree.originUp.containsValue(otherTree.foot, oldOriginUp))
                {
                    predictionTree.originUp.remove(otherTree.foot, oldOriginUp);
                }
                // predictionTree.originDown.put(otherTree.foot, predTreeFirstOriginDownForOwnRoot);
                // predictionTree.originDown.remove(otherTree.getRoot(), predTreeFirstOriginDownForOwnRoot);
                //predictionTree.cutTail(cn); 
                //System.out.println(predictionTree.print());	
                combinedTree = predictionTree;
                //repair in case of node name mismatch.
                if (!otherTreeRoot.equals(otherTree.root))
                {
                    combinedTree.children.put(otherTreeRoot, combinedTree.children.get(otherTree.root));
                    combinedTree.putNodeType(otherTreeRoot, combinedTree.getNodeType(otherTree.root));
                    if (combinedTree.originDown.containsKey(otherTree.root))
                    {
                        combinedTree.originDown.put(otherTreeRoot, combinedTree.originDown.getCollection(otherTree.root).iterator().next());
                    }
                    if (combinedTree.originUp.containsKey(otherTree.root))
                    {
                        combinedTree.originUp.put(otherTreeRoot, combinedTree.originUp.getCollection(otherTree.root).iterator().next());
                    }
                    combinedTree.putParent(otherTreeRoot, combinedTree.getParent(otherTree.root));
                }
                removelist.add(otherTree);
            }
        }

        return combinedTree;
    }

    /**
     * Find the original StringTree that these nodes belong to and generate the prediction tree from it.
     * @param predictionTreeOrigin 
     * 
     * @param connectionNodes
     * @return the new PredictionStringTree
     */
    private PredictionStringTree buildPredictionTree(ElementaryStringTree originalStringTree, Collection<Integer> connectionNodes, int leafNo)
    {//, String leaf) {
        if (!originalStringTree.originUp.containsValue(originalStringTree.getRoot(), null))
        {
            originalStringTree.originUp.remove(originalStringTree.getRoot());
            //originalStringTree.originUp.put(originalStringTree.getRoot(), null);
        }
        PredictionStringTree predTree = new PredictionStringTree(originalStringTree.arraysize, useSemantics);
//		add root
        Integer r = originalStringTree.getRoot();
//        String r = originalStringTree.getRoot() + "";
        predTree.setRoot(r);
        if (predTree.categories[r] == null)
//        if (predTree.categories[Integer.parseInt(r)] == null)
        {
            // can for example happen in auxtrees.
            if (originalStringTree.arraysize > predTree.arraysize)
            {
                predTree.makeArraysBigger(originalStringTree.arraysize - 1);
            }
            predTree.addNode(originalStringTree, r, connectionNodes, leafNo);
            ArrayList<Integer> children = new ArrayList<Integer>();
            children.addAll(predTree.getChildren(r));

            predTree.children.put(r, children);
        }

        // add nodes from connectionNodes collection.
        //for (String node : originalStringTree.categories.keySet()){
        ArrayList<Integer> provisionalList = new ArrayList<Integer>();
        for (int i = 0; i < originalStringTree.categories.length; i++)
        {
            if (originalStringTree.categories[i] != null)
            {
                if (connectionNodes.contains(i) && predTree.categories[i] == null)
//                if (connectionNodes.contains(i + "") && predTree.categories[i] == null)
                {
                    predTree.addNode(originalStringTree, i, connectionNodes, leafNo);
//                    predTree.addNode(originalStringTree, i + "", connectionNodes, leafNo);
                    Integer inode = i;
                    provisionalList.add(inode);
                    // add nodes between root and node:
                    while (inode != null && !inode.equals(r) && predTree.categories[inode] == null)
//                    while (inode != null && !inode.equals(r) && predTree.categories[Integer.parseInt(inode)] == null)
                    {
//						predTree.addNode(originalStringTree, inode, connectionNodes, leafNo);
                        provisionalList.add(inode);
                        inode = originalStringTree.parent[inode];
//                        inode = originalStringTree.parent[Integer.parseInt(inode)];
                    }
                    if (inode != null && inode.equals(r))
                    {
                        for (Integer iinode : provisionalList)
                        {
                            predTree.addNode(originalStringTree, iinode, connectionNodes, leafNo);
                        }
                    }
                }
            }
        }
        // add foot node
        if (originalStringTree.isAuxtree())
        {
            predTree.setAuxTree();
            int foot = originalStringTree.getFoot();
            predTree.foot = foot;
            Integer internode = foot;
//            int internode = Integer.parseInt(foot);
//			add other things that are necessary: foot, root, subst before anchor, path to anchor
            Integer parent = originalStringTree.parent[internode];
            while (predTree.categories[internode] == null)
            {//&& !originalStringTree.children.get(parent).contains(root)){
                predTree.addNode(originalStringTree, internode, connectionNodes, leafNo);
//                predTree.addNode(originalStringTree, internode + "", connectionNodes, leafNo);
                if (parent == null)
                {
                    return predTree;
                }
                internode = parent;
//                internode = Integer.parseInt(parent);
                parent = originalStringTree.parent[internode];
            }
        }

        predTree.parent[predTree.root] = null;
//        predTree.parent[Integer.parseInt(predTree.root)] = null;
        //fix indices if adjunction root and foot nodes left in tree.
        ArrayList<Integer> indices = new ArrayList<Integer>();
        for (Integer shadowRoot : predTree.getNodes())
        {
            //root if unseen lower index.
            Integer anc = shadowRoot;
//            String anc = shadowRoot + "";
            Integer anchorindex = predTree.getLowestOrigin(anc, predTree.originDown);
            if (indices.contains(anchorindex))
            {
                continue;
            }
            else
            {
                indices.add(anchorindex);
            }
            boolean containsrealchild = false;

            ArrayList<Integer> ancCs = predTree.getChildren(anc);
            if (ancCs.size() != 1)
            {
                continue;
            }
            for (Integer anchorChild : ancCs)
            {
                if (predTree.originDown.containsKey(anchorChild) && !predTree.originDown.getCollection(anchorChild).isEmpty()
                        && predTree.getLowestOrigin(anchorChild, predTree.originDown).equals(predTree.getLowestOrigin(anchorChild, predTree.originUp))
                        && predTree.getLowestOrigin(anchorChild, predTree.originDown).equals(anchorindex))
                {
                    containsrealchild = true;
                }
            }
            if (!containsrealchild)
            {
                Integer ancChild = ancCs.get(0);                
                if (predTree.originUp.containsKey(anc) && predTree.getLowestOrigin(ancChild, predTree.originDown) != null && predTree.getLowestOrigin(ancChild, predTree.originDown).equals(predTree.getLowestOrigin(anc, predTree.originUp)))
                {
                    predTree.originDown.remove(anc);
                    predTree.originDown.put(anc, predTree.getLowestOrigin(anc, predTree.originUp));
                    predTree.originUp.remove(ancChild);
                    predTree.originUp.put(ancChild, predTree.getLowestOrigin(anc, predTree.originUp));
                }
                else if (predTree.getLowestOrigin(ancChild, predTree.originUp) != null && predTree.getLowestOrigin(ancChild, predTree.originUp).equals(predTree.getLowestOrigin(anc, predTree.originDown))
                        && predTree.originDown.containsKey(ancChild) && predTree.originUp.containsKey(anc)
                        && !predTree.getLowestOrigin(ancCs.get(0), predTree.originUp).equals(predTree.getLowestOrigin(ancCs.get(0), predTree.originDown))
                        && !predTree.getLowestOrigin(anc, predTree.originUp).equals(predTree.getLowestOrigin(anc, predTree.originDown)))
                {
                    predTree.originDown.remove(anc);
                    predTree.originDown.put(anc, predTree.getLowestOrigin(anc, predTree.originUp));
                    predTree.originUp.remove(ancChild);
                    predTree.originUp.put(ancChild, predTree.getLowestOrigin(anc, predTree.originUp));
                }
                else if (!predTree.originUp.containsKey(anc) && predTree.getCategory(anc).equals(predTree.getCategory(ancCs.get(0))))
                {
                    predTree.root = ancCs.get(0);
                    predTree.removeNode(anc);
                    predTree.originUp.remove(predTree.root);
                }
            }
        }

        return predTree;
    }

    /**
     * From all the nodes that are in the set of connection nodes, find those that have 
     * larger smallest origins than the current leaf number, and sort them by their smallest
     * origins, and put them into a MultiValueHashMap. This map will now contain as many keys
     * as prediction trees needed to connect the current word. The values of these keys contain
     * the nodes that are needed in the prediction tree to achieve connectivity. 
     * (+ one additional node for the head node???, all arguments before spine? that should be 
     * in prediction tree build procedure.)
     * 
     * @param currentLeafNumber
     * @return
     */
    private MultiValueMap findNodesWithGreaterLeafnumbers(int currentLeafNumber)
    {
        MultiValueMap<Integer, Integer> connectionNodes = new MultiValueMap();
        //deleteUnary(connectedNodes);

        int leafToBeConnectedNumber = currentLeafNumber;
//        int leafToBeConnectedNumber = Integer.parseInt(currentLeafNumber);
        for (Integer node : connectedNodes)
        {
            Integer innerNodeLowestUp = stringTree.getLowestOrigin(node, stringTree.originUp);
//            int innerNodeLowestUp = Integer.parseInt(stringTree.getLowestOrigin(node, stringTree.originUp));
            Integer inld = stringTree.getLowestOrigin(node, stringTree.originDown);

            if (innerNodeLowestUp > leafToBeConnectedNumber && !connectionNodes.containsValue(innerNodeLowestUp, node))
            {

                //check whether that node can be accounted for by one of the connection trees that were already found?

                connectionNodes.put(innerNodeLowestUp, node);
            }
            if (inld != null)
            {
                Integer innerNodeLowestDown = stringTree.getLowestOrigin(node, stringTree.originDown);
//                int innerNodeLowestDown = Integer.parseInt(stringTree.getLowestOrigin(node, stringTree.originDown));
                if (innerNodeLowestDown > leafToBeConnectedNumber
                        && !connectionNodes.containsValue(innerNodeLowestDown, node))
//                        && !connectionNodes.containsValue(innerNodeLowestDown + "", node))
                {
                    connectionNodes.put(innerNodeLowestDown, node);
//                    connectionNodes.put(innerNodeLowestDown + "", node);
                }
            }
        }
        return connectionNodes;
    }

    public List<PredictionStringTree> getPredictedLexEntries()
    {
        return predictedTrees;
    }

    public HashMap<Integer, Integer> getNoOfSources()
    {
        return noOfSources;
    }

    public void combinePredictedTreesFromSameOrigin()
    {
        HashMap<Integer, PredictionStringTree> originToPredTreeMap = new HashMap<Integer, PredictionStringTree>();
        ArrayList<PredictionStringTree> removeList = new ArrayList<PredictionStringTree>();
        for (PredictionStringTree pt : this.predictedTrees)
        {
            for (Integer anchor : pt.getAnchorList())
            {
                Integer originDown = pt.getLowestOrigin(anchor, pt.originDown);//.getCollection().get(0);

                if (countIndices(pt, originDown) == 1)
                {
                    pt.originDown.remove(anchor, originDown);
                    pt.nodeTypes[anchor] = TagNodeType.subst;
//                    pt.nodeTypes[Integer.parseInt(anchor)] = TagNodeType.subst;
                    continue;
                }

                if (originToPredTreeMap.containsKey(originDown))
                {
                    //System.out.println(pt);
                    PredictionStringTree pt1 = originToPredTreeMap.get(originDown);
                    //System.out.println(pt1);
                    //System.out.println("need to combine");
                    if (pt.toString().equals(pt1.toString()))
                    {
                        //removeList.add(pt); don't remove because this happens otherwise for trees with 2 anchors!
                    }
                    else
                    {
                        //assume difference in size due to cutTail.
                        int ptcount = countIndices(pt, originDown);

                        int pt1count = 0;
                        Integer pt1anchor = 0;
                        for (Integer node : pt1.getNodes())
                        {
                            if (pt1.originDown.get(node) != null && pt1.getLowestOrigin(node, pt1.originDown).equals(originDown))
//                            if (pt1.originDown.get(node + "") != null && pt1.getLowestOrigin(node + "", pt1.originDown).equals(originDown))
                            {
                                pt1count++;
                                if (pt1.getAnchorList().contains(node))
//                                if (pt1.getAnchorList().contains(node + ""))
                                {
                                    pt1anchor = node;
                                }
                            }
                            if (pt1.originUp.get(node) != null && pt1.getLowestOrigin(node, pt1.originUp).equals(originDown))
//                            if (pt1.originUp.get(node + "") != null && pt1.getLowestOrigin(node + "", pt1.originUp).equals(originDown))
                            {
                                pt1count++;
                            }
                        }
                        if (ptcount == pt1count)
                        {
                            if (pt.getNodes().size() > pt1.getNodes().size())
                            {
                                removeList.add(pt1);
                            }
                            else
                            {
                                removeList.add(pt);
                            }
                        }
                        else if (ptcount > pt1count && pt.originUp.get(anchor) != null && pt.getLowestOrigin(anchor, pt.originUp).equals(originDown))
                        {
                            //this case only holds for situations in which exactly one node was cut off.
                            int test = ptcount - 2;
                            if (test == pt1count)
                            {                                
//                                int anchorInt = Integer.parseInt(anchor);

                                pt1.children.put(pt1anchor, pt.getChildren(pt.getParent(anchor)));
//                                pt1.children.put(pt1anchor + "", pt.getChildren(pt.getParent(anchor)));
                                pt1.categories[anchor] = pt.categories[anchor];
                                pt1.fullcategories[anchor] = pt.fullcategories[anchor];
                                pt1.parent[anchor] = pt.parent[anchor];

                                pt1.originUp.putAll(anchor, (Collection<Integer>) pt.originUp.get(anchor));
                                pt1.originDown.putAll(anchor, (Collection<Integer>) pt.originDown.get(anchor));
                                pt1.nodeTypes[anchor] = pt.nodeTypes[anchor];//MyNodeType.predicted;
                                pt1.isHeadChild[anchor] = pt.isHeadChild[anchor];
                                pt1.children.put(anchor, pt.getChildren(anchor));//*/
                                removeList.add(pt);
                            }
                        }
                        else
                        {
                            LogInfo.error("unimplemented case: ConnectionPathCalculator:combinePredictedTreesFromSameOrigin()");

                        }
                    }
                }
                else
                {
                    originToPredTreeMap.put(originDown, pt);
                }
            }
        }
        predictedTrees.removeAll(removeList);
    }

    private int countIndices(PredictionStringTree pt, Integer index)
    {
        int ptcount = 0;
        for (Integer node : pt.getNodes())
        {
            if (pt.originDown.get(node) != null && pt.getLowestOrigin(node, pt.originDown).equals(index))
//            if (pt.originDown.get(node + "") != null && pt.getLowestOrigin(node + "", pt.originDown).equals(index))
            {
                ptcount++;
            }
            if (pt.originUp.get(node) != null && pt.getLowestOrigin(node, pt.originUp).equals(index))
            {
                ptcount++;
            }
        }
        return ptcount;
    }
}