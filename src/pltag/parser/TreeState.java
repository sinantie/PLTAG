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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import pltag.corpus.ElementaryStringTree;
import pltag.corpus.TagNodeType;
import pltag.corpus.StringTree;
import pltag.parser.semantics.DepTreeState;

public class TreeState implements Serializable
{

    static final long serialVersionUID = -1L;
    private static String leftMostCover = "?";
    protected FringeAndProb futureFringe;
    protected Fringe fringe = new Fringe();
    //private ArrayList<Fringe> unaccessibleNodes = new ArrayList<Fringe>();
    //private String fringeString;
    protected boolean auxtree = false;
    protected short[] wordcover = new short[2];
    private int currentSeen;
    protected boolean footleft;
    protected transient ArrayList<ShadowStringTree> shadowTreeList = new ArrayList<ShadowStringTree>();
    protected boolean hasNonShadowLeaf;
    //private ArrayList<BuildBlock> fringeContHistory = new ArrayList<BuildBlock>();
    protected int nBest;

    public TreeState(boolean auxtree, boolean footleft, short[] wordcover, Fringe fringe,
            ArrayList<Fringe> unaccessibleNodes, FringeAndProb uAndProb,
            ArrayList<ShadowStringTree> shadowTreeList, boolean hasNonShadowLeaf, int nBest)
    {
        //this.root = root;
        this.auxtree = auxtree;
        this.wordcover = wordcover;
        this.fringe = fringe;
        if (uAndProb == null)
        {
            this.futureFringe = new FringeAndProb(unaccessibleNodes, nBest);
        } 
        else
        {
            this.futureFringe = new FringeAndProb(unaccessibleNodes, uAndProb.getnBestProbs().clone(), uAndProb.getNextClone(), uAndProb.getBBHist());
        }
        this.footleft = footleft;
        this.shadowTreeList = shadowTreeList;
        //this.shadowNodeList = shadowNodeList;
        this.hasNonShadowLeaf = hasNonShadowLeaf;
        this.nBest = nBest;
    }

    public TreeState()
    {        
    }

    /**
     * Constructor for creating a TreeState given an input elementary tree (not
     * a prediction tree)
     *
     * @param analysis the elementary shadow (prediction) tree
     * @param wordindex the current word index
     * @param nBest the number of n-best lists to keep
     */
    public TreeState(StringTree analysis, short wordindex, int nBest)
    {
        setUpTS(analysis, wordindex);

        HashMap<Short, Node> nodekernelmap = new HashMap<Short, Node>();
        fringe = getAuxFringe(analysis, currentSeen, wordcover, nodekernelmap);
        //root = fringe.getAdjNodesOpenLeft().get(0);
        ArrayList<Fringe> unaccessibleNodes = calculateUnaccessibles(analysis, fringe.getCurrentLeafNumber(), wordcover, nodekernelmap);
        this.futureFringe = new FringeAndProb(unaccessibleNodes, nBest);
        this.nBest = nBest;
        //NodeKernel rootkernel = new NodeKernel(analysis.getCategory(rootId+""), analysis.getMainLeaf(analysis.getRoot()), rootId);
        //root = new Node(rootkernel, analysis.getLowerIndex(rootId),analysis.getUpperIndex(rootId), (short) -1);
    }

    /**
     * Constructor for creating a TreeState given an input shadow tree
     *
     * @param analysis the elementary shadow (prediction) tree
     * @param wordindex the current word index
     * @param arrayList the list of pre-compiled possible fringes
     * @param nBest the number of n-best lists to keep
     */
    public TreeState(ElementaryStringTree analysis, short wordindex, ArrayList<Fringe> arrayList, int nBest)
    {
        this.nBest = nBest;
        setUpTS(analysis, (short) (wordindex + 1));
        ArrayList<Fringe> copied = allFringeCopy(arrayList, wordindex);
        fringe = copied.remove(0);
        //root = fringe.getAdjNodesOpenLeft().get(0);
        ArrayList<Fringe> unaccessibleNodes = copied;
        this.futureFringe = new FringeAndProb(unaccessibleNodes, nBest);
    }

    /**
     *
     * Constructor of a TreeState used during the reconstruction of tree, in
     * conjunction with a StringTreeAnalysis, not during search phase.
     *
     * @param analysis the elementary shadow (prediction) tree
     * @param wordindex the current word index
     * @param wordCover an array that contains the span of words of the
     * TreeStatate
     * @param nBest the number of n-best lists to keep
     */
    public TreeState(StringTree analysis, short wordindex, short[] wordCover, int nBest)
    {
        this.nBest = nBest;
        setUpTS(analysis, wordindex);
        int lai = analysis.getLastAnchorID();
        HashMap<Short, Node> nodekernelmap = new HashMap<Short, Node>();
        if (lai == -1)
        {
            fringe = getAuxFringe(analysis, currentSeen, wordCover, nodekernelmap);
        } else
        {
            fringe = getRightNodesFringe(analysis, lai, wordCover, nodekernelmap);
        }
        ArrayList<Fringe> unaccessibleNodes = calculateUnaccessibles2(analysis, fringe.getCurrentLeafNumber(), wordCover, nodekernelmap);
        this.futureFringe = new FringeAndProb(unaccessibleNodes, nBest);
        if (fringe.getSubstNode() == null && !fringe.getAdjNodesOpenLeft().isEmpty()
                && fringe.getLastAdjNode().getDownIndex() == 0 && !isNullLexeme(fringe.getLastAdjNode().getCategory()))// particle
        {
            shiftNextUnaccessibleToAccessible();
        }
    }

    public FringeAndProb getFutureFringe()
    {
        return this.futureFringe;
    }
    /*
     * copies fringe and unaccessibles while maintaining the references to the nodes 
     * (each node only exists once even though it can be seen from two sides.
     */

    public static ArrayList<Fringe> allFringeCopy(ArrayList<Fringe> original, Short wordindex)
    {
        HashMap<Short, Node> nodeMap = new HashMap<Short, Node>();
        ArrayList<Fringe> copied = new ArrayList<Fringe>();
        for (Fringe f : original)
        {
            ArrayList<Node> fr = new ArrayList<Node>();
            for (Node n : f.getAdjNodesOpenRight())
            {
                if (nodeMap.containsKey(n.getNodeId()))
                {
                    fr.add(nodeMap.get(n.getNodeId()));
                } else
                {
                    fr.add(n.copy());
                }
            }
            ArrayList<Node> fl = new ArrayList<Node>();
            for (Node n : f.getAdjNodesOpenLeft())
            {
                Node newNode = n.copy();
                fl.add(newNode);
                nodeMap.put(n.getNodeId(), newNode);
            }
            Node fs;
            if (f.getSubstNode() != null)
            {
                fs = f.getSubstNode().copy();
                nodeMap.put(fs.getNodeId(), fs);
            } else
            {
                fs = null;
            }
            Fringe copiedF = new Fringe(fr, fl, fs, f.getCurrentLeafNumber());//, f);//, f.getFringeConts(), f.getCutOffLocations());
            if (wordindex != null)
            {
                copiedF.setTimeStamp(wordindex);
            }
            //copiedF.copyProbs(f);
            copied.add(copiedF);
        }
        return copied;
    }

    /*
     * copies fringe and unaccessibles while maintaining the references to the nodes 
     * (each node only exists once even though it can be seen from two sides.
     */
    protected ArrayList<Fringe> allFringeCopy()
    {
        //HashMap<Short, Node> nodeMap = new HashMap<Short, Node>();
        LinkedList<Node> nodeStack = new LinkedList<Node>();
        ArrayList<Fringe> copied = new ArrayList<Fringe>();
        ArrayList<Fringe> flist = new ArrayList<Fringe>();
        flist.add(fringe);
        flist.addAll(futureFringe.getFringe());
        for (Fringe f : flist)
        {
            ArrayList<Node> fr = new ArrayList<Node>();
            for (Node n : f.getAdjNodesOpenRight())
            {
                //if (!nodeMap.containsKey(n.getNodeId())){
                if (nodeStack.isEmpty())
                {
                    fr.add(n.copy());
                } else if (nodeStack.peek().toShortString().equals(n.toShortString()))
                {
                    Node nback = nodeStack.pop();
                    if (ParserModel.useLeftMost)
                    {
                        nback.setLeftMostCover(n.getLeftMostCover());
                    }
                    fr.add(nback);
                    //System.out.println("ERROR IN UNIQUE FRINGE NODE IDs."+nodeMap + "\t"+n.getNodeId()+"="+n.toString());
                    //fr.add(n.copy());					
                } else
                {
                    System.out.println("check here.");
                }
                /*
                 }
                 else if (nodeMap.get(n.getNodeId()).toString().equals(n.toString())){
                 Node nback = nodeMap.get(n.getNodeId());
                 nback.setLeftMostCover(n.getLeftMostCover());
                 fr.add(nback);
                 }*/

            }
            ArrayList<Node> fl = new ArrayList<Node>();
            for (Node n : f.getAdjNodesOpenLeft())
            {
                Node newNode = n.copy();
                nodeStack.push(newNode);
                fl.add(newNode);
                //nodeMap.put(n.getNodeId(), newNode);
            }
            Node fs;
            if (f.getSubstNode() != null)
            {
                fs = f.getSubstNode().copy();
                nodeStack.push(fs);
                //nodeMap.put(fs.getNodeId(), fs);
            } else
            {
                fs = null;
            }
            Fringe copiedF = new Fringe(fr, fl, fs, f.getCurrentLeafNumber());//, f);
            //copiedF.copyProbs(f);
            copied.add(copiedF);
        }
        return copied;
    }

    private void setUpTS(StringTree analysis, short wordindex)
    {
        int rootId = analysis.getRoot();
//        int rootId = Integer.parseInt(analysis.getRoot());
        if (analysis.isAuxtree())
        {
            auxtree = true;
            if (analysis.hasFootLeft())
            {
                footleft = true;
            }
        }
        //operation = ParserOperation.initial;
        currentSeen = -1;
        if (analysis.getLowerIndex(rootId) > 0 || analysis.getUpperIndex(rootId) > 0)
        {
            if (((ElementaryStringTree) analysis).getAnchor() != Integer.MIN_VALUE
                    //            if (!((ElementaryStringTree) analysis).getAnchor().equals("")
                    && analysis.getNodeType(((ElementaryStringTree) analysis).getAnchor()) == TagNodeType.anchor
                    && !isNullLexeme(analysis.getCategory(((ElementaryStringTree) analysis).getAnchor())))
            {
                System.out.println("see this.");
                wordcover[0] = wordindex;
                wordcover[1] = wordindex;
                hasNonShadowLeaf = true;
            } else
            {
                wordcover[0] = (short) (wordindex - 1);
                wordcover[1] = (short) (wordindex - 1);
                hasNonShadowLeaf = false;
            }
            ShadowStringTree shadowPointer = new ShadowStringTree((ElementaryStringTree) analysis);
            shadowTreeList.add(shadowPointer);
            //shadowNodeList = getShadowNodeList(analysis, wordcover[1]);
        } else if (!analysis.getShadowSourceTreesRootList().isEmpty())
        {
            ShadowStringTree shadowPointer = new ShadowStringTree((ElementaryStringTree) analysis);
            shadowTreeList.add(shadowPointer);
            wordcover[0] = wordindex;
            wordcover[1] = wordindex;
            //shadowNodeList = getShadowNodeList(analysis, wordcover[1]);
            hasNonShadowLeaf = true;
        } else
        {
            wordcover[0] = wordindex;
            wordcover[1] = wordindex;
            hasNonShadowLeaf = true;
        }
        //root.setTimeStamp(wordcover[1]);
        //System.out.println(tree.print());
    }

    @Override
    public String toString()
    {
        return new StringBuilder().append(" FRINGE: ").append(fringe).append(";").append(futureFringe.getFringe())
                .append(" shadowindices: ").append(this.getShadowIndeces()).toString();
    }

    public static ArrayList<Fringe> calculateUnaccessibles(StringTree tree, int currentSeen, short[] wordcover, HashMap<Short, Node> nodekernelmap)
    {
        ArrayList<Fringe> unaccessibleNodes = new ArrayList<Fringe>();
        int leaf = currentSeen;
        while (leaf != -2)
        {
            Fringe nextfringe = getRightNodes(tree, leaf, wordcover, nodekernelmap);
            unaccessibleNodes.add(nextfringe);
            leaf = nextfringe.getCurrentLeafNumber();
        }
        return unaccessibleNodes;
    }

    public static ArrayList<Fringe> calculateUnaccessibles2(StringTree tree, int currentSeen, short[] wordcover, HashMap<Short, Node> nodekernelmap)
    {
        ArrayList<Fringe> unaccessibleNodes = new ArrayList<Fringe>();
        int leaf = currentSeen;
        while (leaf != -2)
        {
            Fringe nextfringe = getRightNodesFringe(tree, leaf, wordcover, nodekernelmap);
            unaccessibleNodes.add(nextfringe);
            leaf = nextfringe.getCurrentLeafNumber();
        }
        return unaccessibleNodes;
    }

    public static Fringe getAuxFringe(StringTree tree, int currentSeen, short[] wordcoverIn, HashMap<Short, Node> nodekernelmap)
    {
        Fringe fringe = new Fringe();
        if (currentSeen == -1)
        {
            int rootid = tree.getRoot();
            ArrayList<Short> fringeleft = getLeftNodes(tree, rootid, new ArrayList<Short>());
            short leaf = fringeleft.get(0);
//            String leafstring = Short.toString(leaf);
            TagNodeType leafNodeType = tree.getNodeType(leaf);
            if (leafNodeType == TagNodeType.anchor || leafNodeType == TagNodeType.subst)
            {
                fringeleft.remove(0);
            }
            currentSeen = leaf;
            if (leafNodeType == TagNodeType.subst)
            {
                NodeKernel k = new NodeKernel(tree.getCategory(leaf), tree.getMainLeaf(leaf), wordcoverIn[1]);
                String lambda = k.getLambda();
                int j = lambda.indexOf("\t");
                if (j > 0)
                {
                    lambda = lambda.substring(0, j);
                }
                String lowerPosTag = null;//lambda;//subst nodes should not have posDownTAG annotated.
                Node sNode = new Node(k, tree.getLowerIndex(leaf), tree.getUpperIndex(leaf), wordcoverIn[1], (short) leaf,
                        tree.getUnlexStruct(tree.getRoot()), leaf, lowerPosTag, leftMostCover);
                nodekernelmap.put(leaf, sNode);
                fringe.setSubstNode(sNode);
            }
            fringe.setCurrentLeafNumber(leaf);
            ArrayList<Node> leftOpenFringe = reverseOrder(convertToCats(fringeleft, tree, wordcoverIn, nodekernelmap));
            fringe.setAdjNodesOpenLeft(leftOpenFringe);
        }
        return fringe;
    }

    private static ArrayList<Node> reverseOrder(ArrayList<Node> leftNodes)
    {
        ArrayList<Node> reversedList = new ArrayList<Node>();
        for (Node i : leftNodes)
        {
            reversedList.add(0, i);
        }
        return reversedList;
    }

    private static ArrayList<Node> convertToCats(ArrayList<Short> fringeleft, StringTree tree, short[] wordcover,
            HashMap<Short, Node> nodekernelmap)
    {
        return convertToCats(fringeleft, tree, wordcover, nodekernelmap, true);
    }
    
    private static ArrayList<Node> convertToCats(ArrayList<Short> fringeleft, StringTree tree, short[] wordcover,
            HashMap<Short, Node> nodekernelmap, boolean setUnlexStruct)
    {
        ArrayList<Node> auxnodes = new ArrayList<Node>();
        for (short nodeid : fringeleft)
        {
            NodeKernel k = new NodeKernel(tree.getCategory(nodeid), tree.getMainLeaf(nodeid), wordcover[1]);
            String lowerPosTag = k.getLambda();//getPOStag(k.getLambda());
            if (tree.isOnRootToFootPath((int) nodeid))
            {
                lowerPosTag = null; // nodes on path between root and foot nodes should not have lowerPosTAG annotation.
            }
            Node auxnode = new Node(k, tree.getLowerIndex(nodeid), tree.getUpperIndex(nodeid), wordcover[1], nodeid,
                    setUnlexStruct ? tree.getUnlexStruct(tree.getRoot()) : null, nodeid, lowerPosTag, leftMostCover);
            nodekernelmap.put(nodeid, auxnode);
            auxnodes.add(auxnode);
        }
        return auxnodes;
    }

    public static String getPOStag(String lambda)
    {
        String pos = lambda;
        int j = lambda.indexOf("\t");
        if (j > 0)
        {
            pos = lambda.substring(0, j);
        }
        return pos;
    }

    private static ArrayList<Short> getLeftNodes(StringTree tree, int nodeid, ArrayList<Short> fringelist) // TODO: FIX
    {
        boolean beforeFirstLex = true;
        if (tree.getChildren(nodeid) != null)
        {
            for (int child : tree.getChildren(nodeid))
            {
                short childInt = (short) child;
                if (tree.getNodeType(child) == TagNodeType.internal && beforeFirstLex)
                {
                    getLeftNodes(tree, child, fringelist);
                } else if (tree.getNodeType(child) == TagNodeType.foot && beforeFirstLex)
                {
                    //do add to fringelist.
                    fringelist.add(childInt);
                    beforeFirstLex = false;
                } else if (tree.getNodeType(child) == TagNodeType.subst && beforeFirstLex)
                {
                    fringelist.add(childInt);
                    beforeFirstLex = false;
                }
                if (tree.isHeadChild(child) && beforeFirstLex)
                {
                    if (!fringelist.contains(childInt))
                    {
                        fringelist.add(childInt);
                    }
                    beforeFirstLex = false;
                }
                if (!fringelist.isEmpty())
                {
                    break;
                }
            }
        }
        fringelist.add((short) nodeid);
//        fringelist.add(Short.parseShort(nodeid));
        return fringelist;
    }

    private static Fringe getRightNodes(StringTree tree, int current, short[] wordcover, HashMap<Short, Node> nodekernelmap)
    {
        short leaf = -2;
        Fringe nextfringe = new Fringe();
        ArrayList<Node> openRights = new ArrayList<Node>();
        Integer parent = tree.getParent(current);
        if (tree.getNodeType(current) != TagNodeType.anchor)
        {
            short currentInt = (short) current;
            Node cnode = nodekernelmap.get(currentInt);
            openRights.add(cnode);
        }
        while (parent != null)
        {
            Integer rightsibling = getRightSibling(tree.getChildren(parent), current);
            //go to next child after original current and get leftfringe from there.
            if (rightsibling != null)
            {
                nextfringe.setAdjNodesOpenRight(openRights);
                //what if the leaf here is a foot node?	
                ArrayList<Short> openLefts = getLeftNodes(tree, rightsibling, new ArrayList<Short>());
                leaf = openLefts.get(0);
                //don't remove subst nodes in unaccessibles.
//                String leaf = Short.toString(leaf);
                if (tree.getNodeType(leaf) == TagNodeType.subst)
                {
                    NodeKernel k = new NodeKernel(tree.getCategory(leaf), tree.getMainLeaf(leaf), wordcover[1]);
                    //String lambda = k.getLambda();
                    //int j = lambda.indexOf("\t");
                    Node snode = new Node(k, tree.getLowerIndex(leaf), tree.getUpperIndex(leaf), wordcover[1], leaf,
                            tree.getUnlexStruct(tree.getRoot()), leaf, null, leftMostCover);
                    nodekernelmap.put(leaf, snode);
                    nextfringe.setSubstNode(snode);
                    openLefts.remove(0);
                } else if (tree.getNodeType(leaf) == TagNodeType.anchor)
                {
                    openLefts.remove(0);
                }
                nextfringe.setCurrentLeafNumber(leaf);
                //auxnodes.addAll(reverseOrder(convertToCats(leftFringe, tree, false, wordcover)));
                nextfringe.setAdjNodesOpenLeft(reverseOrder(convertToCats(openLefts, tree, wordcover, nodekernelmap)));
                return nextfringe;
            }
            //if no such child exists, go up one further; if arrive at root node, finished. leaf stays -2.
//            short parentInt = Short.parseShort(parent);
            Node anode = nodekernelmap.get(parent.shortValue());
            openRights.add(anode);
            current = parent;
            parent = tree.getParent(current);
        }
        nextfringe.setCurrentLeafNumber(leaf);
        nextfringe.setAdjNodesOpenRight(openRights);
        return nextfringe;
    }

    private static Fringe getRightNodesFringe(StringTree tree, int current, short[] wordcover, HashMap<Short, Node> nodekernelmap)
    {
        short leaf = -2;
        Fringe nextfringe = new Fringe();
        ArrayList<Node> openRights = new ArrayList<Node>();
        Integer parent = tree.getParent(current);
        if (tree.getNodeType(current) != TagNodeType.anchor)
        {
//            short currentInt = Short.parseShort(current);
            //Node cnode =nodekernelmap.get(currentInt);			
            NodeKernel k = new NodeKernel(tree.getCategory(current), tree.getMainLeaf(current), wordcover[1]);
            //	String lambda = k.getLambda();
            //int j = lambda.indexOf("\t");
            // commented out getUnlexStruct(...) as it is not used during reconstruction of analyses
            Node cnode = new Node(k, tree.getLowerIndex(current), tree.getUpperIndex(current), wordcover[1], (short) current,
//                    tree.getUnlexStruct(tree.getRoot()), current, null, leftMostCover);
                    null, current, null, leftMostCover);
            openRights.add(cnode);
        }
        while (parent != null)
        {
            Integer rightsibling = getRightSibling(tree.getChildren(parent), current);
            //go to next child after original current and get leftfringe from there.
            if (rightsibling != null)
            {
                nextfringe.setAdjNodesOpenRight(openRights);
                //what if the leaf here is a foot node?	
                ArrayList<Short> openLefts = getLeftNodes(tree, rightsibling, new ArrayList<Short>());
                leaf = openLefts.get(0);
                //don't remove subst nodes in unaccessibles.
//                String leaf = Short.toString(leaf);
                if (tree.getNodeType(leaf) == TagNodeType.subst)
                {
                    NodeKernel k = new NodeKernel(tree.getCategory(leaf), tree.getMainLeaf(leaf), wordcover[1]);
                    //			String lambda = k.getLambda();
                    //			int j = lambda.indexOf("\t");
                    // commented out getUnlexStruct(...) as it is not used during reconstruction of analyses
                    Node snode = new Node(k, tree.getLowerIndex(leaf), tree.getUpperIndex(leaf), wordcover[1], leaf,
                            null, leaf, null, leftMostCover);
//                            tree.getUnlexStruct(tree.getRoot()), leaf, null, leftMostCover);
                    nodekernelmap.put(leaf, snode);
                    nextfringe.setSubstNode(snode);
                    openLefts.remove(0);
                } else if (tree.getNodeType(leaf) == TagNodeType.anchor)
                {
                    openLefts.remove(0);
                }
                nextfringe.setCurrentLeafNumber(leaf);
                //auxnodes.addAll(reverseOrder(convertToCats(leftFringe, tree, false, wordcover)));
                nextfringe.setAdjNodesOpenLeft(reverseOrder(convertToCats(openLefts, tree, wordcover, nodekernelmap, false)));
                return nextfringe;
            }
            //if no such child exists, go up one further; if arrive at root node, finished. leaf stays -2.
//            short parentInt = (short) parent;
            //Node anode =nodekernelmap.get(parentInt);			
            NodeKernel k = new NodeKernel(tree.getCategory(parent), tree.getMainLeaf(parent), wordcover[1]);
            //String lambda = k.getLambda();
            //int j = lambda.indexOf("\t");
            //posdown tag can be set to null because the present method is only called during construction of the correct tree, not during the search phase.
            // commented out getUnlexStruct(...) as it is not used during reconstruction of analyses
            Node anode = new Node(k, tree.getLowerIndex(parent), tree.getUpperIndex(parent), wordcover[1], parent.shortValue(),
                    null, parent, null, leftMostCover);
//                    tree.getUnlexStruct(tree.getRoot()), parent, null, leftMostCover);
            openRights.add(anode);
            current = parent;
            parent = tree.getParent(current);
        }
        nextfringe.setCurrentLeafNumber(leaf);
        nextfringe.setAdjNodesOpenRight(openRights);
        return nextfringe;
    }

    private static Integer getRightSibling(ArrayList<Integer> children, int current)
    {
        int lastchild = children.size() - 1;
        if (children.get(lastchild).equals(current))
        {
            return null;
        }
        for (int i = 0; i < lastchild; i++)
        {
            int c = children.get(i);
            if (current == c)
            {
                return children.get(i + 1);
            }
        }
        System.err.println("why is child not contained??? Fringe:getRightSibling");
        return null;
    }

    public Fringe getFringe()
    {
        //if (fringe == null && fringeString !=null){
        //	parserFringeString();
        //}
        return fringe;
    }

    public ArrayList<Fringe> getUnaccessibles()
    {
        return futureFringe.getFringe();
    }

    public Fringe getLastUnaccessible()
    {
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        return unaccessibleNodes.get(unaccessibleNodes.size() - 1);
    }

    public short[] getWordCover()
    {
        return wordcover;
    }

    public Node getRootNode()
    {
        Node rootcandidate = null;
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        if (!fringe.getAdjNodesOpenLeft().isEmpty() && fringe.getAdjNodesOpenRight().isEmpty())
        {
            rootcandidate = fringe.getAdjNodesOpenLeft().get(0);
        }
        if (rootcandidate != null && rootcandidate.getUpIndex() == -1)
        {
            return rootcandidate;
        }

        if (!unaccessibleNodes.isEmpty())
        {
            rootcandidate = this.getLastUnaccessible().getLastAdjNode();
        } else if (unaccessibleNodes.isEmpty() && !fringe.getAdjNodesOpenRight().isEmpty() && fringe.getAdjNodesOpenLeft().isEmpty())
        {
            rootcandidate = fringe.getLastAdjNode();
        } else
        {
            //	System.err.print("null at " +this + "TreeState.getRootNode() pos 1");
            return null;
        }
        if (rootcandidate.getUpIndex() != -1)
        {
            if (isNullLexeme(fringe.getAdjNodesOpenRight().get(0).getCategory()) && this.isAux() && !this.footleft)
            {
                //return rootcandidate;//foot node.
            }//else 
            //System.err.println("check root in TreeState.getRootNode (1)");
            //System.err.print("null at " +this + "TreeState.getRootNode() pos 2");
            return null;
        }
        return rootcandidate;
    }

    public boolean isAux()
    {
//        if (auxtree)
//        {
//            return Boolean.TRUE;
//        }
//        return Boolean.FALSE;
        return auxtree;
    }

    public void shiftNextUnaccessibleToAccessible()
    {
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        if (unaccessibleNodes.size() > 0)
        {
            fringe = unaccessibleNodes.remove(0);
        } else
        {
            fringe = new Fringe();
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Fringe> getNextUa()
    {
        ArrayList<Fringe> nua = (ArrayList<Fringe>) futureFringe.getFringe().clone();
        nua.remove(0);
        return nua;
    }

    public Fringe getNextFringe()
    {
        if (futureFringe.getFringe() != null && !futureFringe.getFringe().isEmpty())
        {
            return futureFringe.getFringe().get(0);
        } else
        {
            return null;
        }
    }

    /*
     * returns nodes from preftreefringe that are candidates for adjunction with current tree.
     */
    public ArrayList<Node> findAdjoinable(Fringe preftreefringe, Boolean desiredShadowStatus, HashMap<Node, String> ipiNodeClusterNo)
    {
        ArrayList<Node> adjoinables = new ArrayList<Node>();
        ArrayList<Node> prefTreeAdjNodes;
        Node root = this.getLastFringe().getLastAdjNode();
        if (footleft)
        {
            prefTreeAdjNodes = preftreefringe.getAdjNodesOpenRight();
        } else
        {
            prefTreeAdjNodes = preftreefringe.getAdjNodesOpenLeft();
        }
        boolean flatLastCorrCat = false;
        int clusterCount = 0;
        for (Node candidate : prefTreeAdjNodes)
        {
            if (candidate.getCategory().equals(root.getCategory()))
            {
                if (desiredShadowStatus == null || candidate.isShadow() == desiredShadowStatus)
                {
                    if (!flatLastCorrCat)
                    {
                        clusterCount++;
                        flatLastCorrCat = true;
                    }
                    adjoinables.add(candidate);
                    ipiNodeClusterNo.put(candidate, clusterCount + "/");
                } else
                {
                    flatLastCorrCat = false;
                }
            } else
            {
                flatLastCorrCat = false;
            }
        }
        Node sn = preftreefringe.getSubstNode();
        if (!footleft && sn != null && sn.getCategory().equals(root.getCategory()))
        {
            if (desiredShadowStatus == null || sn.isShadow() == desiredShadowStatus)
            {
                if (!flatLastCorrCat)
                {
                    clusterCount++;
                    flatLastCorrCat = true;
                }
                adjoinables.add(sn);
                ipiNodeClusterNo.put(sn, clusterCount + "/");
            }
        }
        for (Node n : ipiNodeClusterNo.keySet())
        {
            ipiNodeClusterNo.put(n, ipiNodeClusterNo.get(n) + clusterCount);
        }
        return adjoinables;
    }

    public boolean hasFootLeft()
    {
        if (footleft)
        {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;//footleft;
    }

    public ArrayList<Fringe> getFringesBeforeFirstNullLexeme()
    {
        ArrayList<Fringe> possibleFringes = new ArrayList<Fringe>();
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        //last one does never have  a subst node because it's at the right edge of the tree, with root as last node
        boolean found = false;
        for (int i = unaccessibleNodes.size() - 2; i >= 0; i--)
        {
            Fringe f = unaccessibleNodes.get(i);
            Node lastAdjNode = f.getLastAdjNode();
            if (f.getSubstNode() == null && lastAdjNode.getDownIndex() != -1 && (lastAdjNode.getDownIndex() == 0
                    || isNullLexeme(lastAdjNode.getCategory()) || isNullLexeme(f.getAdjNodesOpenLeft().get(0).getCategory())))
            { // is anchor or is trace
                possibleFringes.add(0, unaccessibleNodes.get(i + 1));
                if (!isNullLexeme(lastAdjNode.getCategory()) && !isNullLexeme(f.getAdjNodesOpenLeft().get(0).getCategory()))
                {
                    found = true;
                    break;
                }
            }
        }
        if (!found)
        {
            Node lastAdjNode = fringe.getLastAdjNode();
            if (fringe.getSubstNode() == null && !isNullLexeme(lastAdjNode.getCategory()) && lastAdjNode.getDownIndex() == 0)
            {
                possibleFringes.add(0, unaccessibleNodes.get(0));

            } else
            {
                possibleFringes.add(0, fringe);
            }
        }
        return possibleFringes;
    }

    public Fringe getFringeAfterLastNonShadowNonNullLexAnchor()
    {
        int anchor = -1;
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        //last one does never have  a subst node because it's at the right edge of the tree, with root as last node
        for (int i = unaccessibleNodes.size() - 2; i >= 0; i--)
        {
            Fringe f = unaccessibleNodes.get(i);
            Node lastAdjNode = f.getLastAdjNode();
            if (f.getSubstNode() == null && (lastAdjNode.getDownIndex() == 0 && // not subst or foot node
                    (!isNullLexeme(lastAdjNode.getCategory()))))
            { // is anchor or is trace
                anchor = i + 1;
                break;
            }
        }
        if (anchor == -1)
        {
            Node lastAdjNode = fringe.getLastAdjNode();
            if (fringe.getSubstNode() == null && (!isNullLexeme(lastAdjNode.getCategory())
                    && lastAdjNode.getDownIndex() == 0))
            {
                anchor = 0;
            } else
            {
                return fringe;
            }
        }
        return unaccessibleNodes.get(anchor);
    }

    public static boolean isNullLexeme(String cat)
    {
        return cat.contains("*") || cat.equals("0");
//        if (cat.contains("*"))
//        {
//            return true;
//        }
//        if (cat.equals("0"))
//        {
//            return true;
//        }
//        return false;
    }

    public ArrayList<Fringe> getUaAfter(Fringe lastbefore)
    {
        ArrayList<Fringe> restfringe = new ArrayList<Fringe>();
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        boolean on = false;
        if (lastbefore.toString().equals(fringe.toString())) // TODO: FIX
        {
            return unaccessibleNodes;
        }

        for (Fringe f : unaccessibleNodes)
        {
            if (on)
            {
                restfringe.add(f);
            }
            if (f.toString().equals(lastbefore.toString()))
            {
                on = true;
            }
        }
        return restfringe;
    }

    public ArrayList<ShadowStringTree> getShadowTrees()
    {
        return this.shadowTreeList;
    }

    /**
     * about fringe: the parts to the right of the lexical anchor are not yet
     * accessible anyway (because of accessibility constraints of other rules.
     * Therefore can maintain fringe by just replacing fringe contributions from
     * shadow tree with fringe contributions from full tree. Here we only need
     * to update the shadowTreeList, the root, the fringe and the unaccessible
     * fringe if applicable Returns replaced (verified) nodes.
     */
    //public ArrayList<Node> replaceShadow(ShadowStringTree shadowtree, String indexPattern, StringTree tree, HashMap<Integer, Integer> coveredNodes) {
    public void replaceShadow(ShadowStringTree shadowtree, ArrayList<Byte> indexPattern)
    {//, StringTree tree){//, HashMap<Integer, Integer> coveredNodes) {
        // ROOT 
        //if (StringTreeAnalysis.matches(indexPattern, root.getDownIndex())){
        //	root.verifiedDownIndex();
        //}
        // FRINGE
        for (Node node : fringe.getAdjNodes())
        {
            if (StringTreeAnalysis.matches(indexPattern, node.getUpIndex()))
            {
                node.verifiedUpIndex();
            }
            if (StringTreeAnalysis.matches(indexPattern, node.getDownIndex()))
            {
                node.verifiedDownIndex();
            }
        }
        if (fringe.getSubstNode() != null)
        {
            fringe.getSubstNode().verifiedUpIndex();
            //System.out.println("Error, this should have been filtered out before, " +
            //		"Incrementality violation. TreeState:replaceShadow()");
        }
        // UNACCESSIBLE FRINGE
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        for (Fringe f : unaccessibleNodes)
        {
            for (Node node : f.getAdjNodes())
            {
                if (StringTreeAnalysis.matches(indexPattern, node.getUpIndex()))
                {
                    node.verifiedUpIndex();
                }
                if (StringTreeAnalysis.matches(indexPattern, node.getDownIndex()))
                {
                    node.verifiedDownIndex();
                }
            }
            if (f.getSubstNode() != null)
            {
                if (StringTreeAnalysis.matches(indexPattern, f.getSubstNode().getUpIndex()))
                {
                    f.getSubstNode().verifiedUpIndex();
                }
            }
        }
        // new hidden FRINGE:
        if (this.futureFringe.getNext() != null)
        {
            ArrayList<Fringe> hiddenFringes = this.futureFringe.getNext().get(0).getFringe();//this.getLastFringe().getFringeConts().get(0); 
            // can only be one at this point.
            //need deep clone here!
            //		ArrayList<Fringe> hiddenFringesClone = new ArrayList<Fringe>();
            for (Fringe cloneF : hiddenFringes)
            {
                //			Fringe cloneF = f.copy();//deep clone
                for (Node node : cloneF.getAdjNodes())
                {
                    if (StringTreeAnalysis.matches(indexPattern, node.getUpIndex()))
                    {
                        node.verifiedUpIndex();
                    }
                    if (StringTreeAnalysis.matches(indexPattern, node.getDownIndex()))
                    {
                        node.verifiedDownIndex();
                    }
                }
                if (cloneF.getSubstNode() != null)
                {
                    if (StringTreeAnalysis.matches(indexPattern, cloneF.getSubstNode().getUpIndex()))
                    {
                        cloneF.getSubstNode().verifiedUpIndex();
                    }
                }
                //		hiddenFringesClone.add(cloneF);
            }
            //	ArrayList<ArrayList<Fringe>> fc = new ArrayList<ArrayList<Fringe>>();
            //	fc.add(hiddenFringesClone);
            //	this.getLastFringe().setFringeConts(fc);	
        }
        // SHADOWTREELIST: Achtung! bei zusammengefuegten baeumen nur partial!!!
        ShadowStringTree matchingTree = null;
        for (ShadowStringTree st : shadowTreeList)
        {
            if (st.getTreeOrigIndex() == shadowtree.getTreeOrigIndex())
            {
                st.replaceIndeces(indexPattern, (byte) 0);
                matchingTree = st;
            }
        }
        if (!matchingTree.hasShadows())
        {
            this.shadowTreeList.remove(matchingTree);
            matchingTree = null;
        }
        if (this.hasNonShadowLeaf == false)
        {
            this.hasNonShadowLeaf = true;
        }
    }

    public ArrayList<Byte> getShadowIndeces()
    {
        ArrayList<Byte> indexlist = new ArrayList<Byte>();
        if (shadowTreeList == null)
        {
            return indexlist;
        }
        for (ShadowStringTree shadowtree : shadowTreeList)
        {
            for (int i = 1; i < shadowtree.getIndexChange().length; i++)
            {
                if (shadowtree.getIndexChange()[i] > 0)
                {
                    indexlist.add(shadowtree.getIndexChange()[i]);
                }
            }
        }
        return indexlist;
    }

    public void makeUniqueShadowIndeces(ArrayList<Byte> shadowIndeces)
    {
        //public HashMap<Integer, Integer> makeUniqueShadowIndeces(ArrayList<String> shadowIndeces) {
        //HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>();
        if (shadowTreeList.size() == 1)
        {
            ShadowStringTree st = (ShadowStringTree) shadowTreeList.get(0);
            int indexDiff = getHighest(shadowIndeces);
            if (indexDiff == 0)
            {
                return;//new HashMap<Integer, Integer>();
            }
            ArrayList<Byte> thisShadowInd = getShadowIndeces();
            byte[] currentIndices = sortHighestFirst(thisShadowInd, getHighest(thisShadowInd));
            for (byte localShadowIndex : currentIndices)
            {
                if (localShadowIndex == -1)
                {
                    continue;
                }
                //shadowtreelist
                st.replaceIndices(localShadowIndex, (byte) (localShadowIndex + indexDiff));
                //fringe
                fringe.replaceIndices(localShadowIndex, (byte) (localShadowIndex + indexDiff));
                //unaccessibleNodes
                ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
                for (Fringe ua : unaccessibleNodes)
                {
                    ua.replaceIndices(localShadowIndex, (byte) (localShadowIndex + indexDiff));
                }
                //root
                //root.replaceIndices(localShadowIndex, (byte) (localShadowIndex+indexDiff));
            }
        } else
        {
            if (shadowTreeList.size() > 1)
            {
                System.err.println("Complex tree??? TreeState.java makeUniqueShadowIndeces");
            }
        }
    }

    private byte[] sortHighestFirst(ArrayList<Byte> shadowIndeces, byte b)
    {
        byte[] sortedList = new byte[b];
        for (byte i : shadowIndeces)
        {
            sortedList[b - i] = i;
        }

        return sortedList;
    }

    private byte getHighest(ArrayList<Byte> shadowIndeces)
    {
        byte highest = 0;
        for (byte index : shadowIndeces)
        {
            if (index <= 0)
            {
                continue;
            }
            if (index > highest)
            {
                highest = index;
            }
        }
        return highest;
    }

    public boolean identical(TreeState ts)
    {
        if (//Auxtree
                (auxtree == ts.auxtree)
                && //Wordcover
                (wordcover[0] == ts.wordcover[0] && wordcover[1] == ts.wordcover[1])
                && //footleft
                (!auxtree || footleft == ts.footleft)
                && //shadowTreeList
                shadowTreeList.size() == ts.shadowTreeList.size()
                && //unaccessible nodes
                futureFringe.getFringe().size() == ts.futureFringe.getFringe().size()//&&
                //Root
                //(root.identical(ts.root))//&&
                //fringe
                //(fringe.toString().equals(ts.fringe.toString()))
                )
        {
            for (int i = 0; i < this.shadowTreeList.size(); i++)
            {
                if (//!this.shadowTreeList.get(i).getTreeString().equals(ts.shadowTreeList.get(i).getTreeString())){
                        this.shadowTreeList.get(i).getTreeOrigIndex() != ts.shadowTreeList.get(i).getTreeOrigIndex())
                {
                    return false;
                }
                if (!this.shadowTreeList.get(i).getIndexChange().toString().equals(ts.shadowTreeList.get(i).getIndexChange().toString()))
                {
                    return false;
                }
            }
            ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
            for (int i = 0; i < unaccessibleNodes.size(); i++)
            {
                if (!unaccessibleNodes.get(i).toString().equals(ts.futureFringe.getFringe().get(i).toString()))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public TreeState copy()
    {
        ArrayList<Fringe> completeFringe = this.allFringeCopy();
        Fringe newFringe = completeFringe.remove(0);
        ArrayList<Fringe> newUa = completeFringe;
        return new TreeState(auxtree, footleft, wordcover.clone(), newFringe, newUa, futureFringe,
                shadowListDeepCopy(), hasNonShadowLeaf, nBest);
    }

    public ArrayList<ShadowStringTree> shadowListDeepCopy()
    {
        ArrayList<ShadowStringTree> copyList = new ArrayList<ShadowStringTree>();
        if (shadowTreeList == null)
        {
            return null;
        }
        for (ShadowStringTree st : shadowTreeList)
        {
            copyList.add(st.copy());
        }
        return copyList;
    }

    /*
     * only used for adjunctions with foot node to the right.
     * 1) find adj node in fringe and combine with foot node and add root node (and any nodes in between them)
     * 2) find back side of adj node and fix back side accordingly.
     */
    @SuppressWarnings("unchecked")
    public void fixFringeAdj(Node adjNode, ArrayList<Node> footToRootNodes, short timeStamp)
    {//, String leftmostPos) {
        //foot node that needs to be inserted in fringes:
        //	Node rootcopy = footToRootNodes.get(footToRootNodes.size()-1).copy();
        //	rootcopy.setUpIndex(adjNode.getUpIndex());
        Node oldroot = footToRootNodes.get(footToRootNodes.size() - 1);
        Node rootcopy;
        if (adjNode.getDownIndex() == -1)
        {//adj into subst node or foot node. DownPOStag should then be null.
            rootcopy = new Node(new NodeKernel(adjNode.getCategory(), "@R", adjNode.getNodeId()), oldroot.getDownIndex(), adjNode.getUpIndex(),
                    timeStamp, oldroot.getNodeId(), adjNode.getOrigTree(), adjNode.getPosInTree(),
                    adjNode.getDownPosTag(), oldroot.getLeftMostCover());//adjNode.copy();//footToRootNodes.get(footToRootNodes.size()-1).copy();
            //rootcopy.setDownPosTag(adjNode.getDownPosTag());
        } else if (adjNode.getUpIndex() == -1 && adjNode.getDownIndex() != 0)
        {//adj into predicted root node
            rootcopy = new Node(new NodeKernel(adjNode.getCategory(), "@R", adjNode.getNodeId()), oldroot.getDownIndex(), adjNode.getUpIndex(),
                    timeStamp, oldroot.getNodeId(), adjNode.getOrigTree(), adjNode.getPosInTree(),
                    adjNode.getDownPosTag(), oldroot.getLeftMostCover());//adjNode.copy();//footToRootNodes.get(footToRootNodes.size()-1).copy();
            //rootcopy.setDownPosTag(adjNode.getDownPosTag());//? not necessary as upindex == -1	
        } else
        {
            rootcopy = adjNode.copy();//adjdownF: adjNode is node of upper tree, so rootcopy must have correct downPOStag
            rootcopy.setDownIndex(oldroot.getDownIndex());
            //rootcopy.setDownPosTag(oldroot.getDownPosTag());//this is not intended for adjdown.
            rootcopy.setNodeId(oldroot.getNodeId());
            rootcopy.setLeftMostCover(oldroot.getLeftMostCover());
            rootcopy.setTimeStamp(timeStamp);
        }
        adjNode.setUpIndex(footToRootNodes.get(0).getUpIndex());//set to upindex of foot node.
        //adjNode.setUpPosTag(footToRootNodes.get(0).getUpPosTag());
        adjNode.setTimeStamp(timeStamp);
        int adjNodeIndex = fringe.getAdjNodesOpenLeft().indexOf(adjNode);
        if (adjNodeIndex == -1 && adjNode == fringe.getSubstNode())
        {
            adjNodeIndex = fringe.getAdjNodesOpenLeft().size();
        }
        //fringe.getAdjNodesOpenLeft().add(adjNodeIndex,rootcopy);//TODO: why???
        for (Fringe f : this.futureFringe.getFringe())
        {
            int backSideAdjNodeIndex = f.getAdjNodesOpenRight().indexOf(adjNode);
            if (backSideAdjNodeIndex != -1)
            {
                f.getAdjNodesOpenRight().add(backSideAdjNodeIndex + 1, rootcopy);
                for (int interMedNode = footToRootNodes.size() - 2; interMedNode > 0; interMedNode--)
                {
                    Node intermednode = footToRootNodes.get(interMedNode).copy();
                    if (intermednode.getLambda().equals("@R") && !rootcopy.getLambda().equals("@R"))
                    {
                        Node newnode = new Node(new NodeKernel(intermednode.getCategory(), "@R", intermednode.getNodeId()),
                                intermednode.getDownIndex(), intermednode.getUpIndex(), (short) intermednode.getTimeStamp(),
                                (byte) intermednode.getNodeId(), adjNode.getOrigTree(), adjNode.getPosInTree(),
                                adjNode.getDownPosTag(), intermednode.getLeftMostCover());//TODO is the intermednode.getLeftMostCover correct?
                        //newnode.setDownPosTag(adjNode.getDownPosTag());
                        f.getAdjNodesOpenRight().add(backSideAdjNodeIndex + 1, newnode);
                        fringe.getAdjNodesOpenLeft().add(adjNodeIndex, newnode);
                    } else
                    {
                        intermednode.setDownPosTag(adjNode.getDownPosTag());
                        f.getAdjNodesOpenRight().add(backSideAdjNodeIndex + 1, intermednode);
                        fringe.getAdjNodesOpenLeft().add(adjNodeIndex, intermednode);
                    }
                }
                return;
            }
        }
        //if we get here, it means that we have not seen the backside of the adjNode.
        for (FringeAndProb fap : this.futureFringe.getNext())
        {
            ArrayList<Fringe> conts = fap.getFringe();
            ArrayList<Node> leftlist = new ArrayList<Node>();
            ArrayList<Fringe> contsCopy = (ArrayList<Fringe>) conts.clone();
            for (int i = 0; i < contsCopy.size(); i++)
            {
                Fringe f = contsCopy.get(i);
                for (Node n : f.getAdjNodesOpenRight())
                {
                    if (!leftlist.contains(n))
                    { // node found.
                        Fringe copyF = f.copy();
                        int backSideAdjNodeIndex = f.getAdjNodesOpenRight().indexOf(n);
                        ArrayList<Node> newRight = (ArrayList<Node>) f.getAdjNodesOpenRight().clone();
                        newRight.add(backSideAdjNodeIndex + 1, rootcopy);
                        Node ncopy = n.copy();
                        ncopy.setUpIndex(footToRootNodes.get(0).getUpIndex());
                        //ncopy.setUpPosTag(footToRootNodes.get(0).getUpPosTag());
                        ncopy.setTimeStamp(timeStamp);
                        //rootcopy.setLeftMostCover(leftmostPos);
                        newRight.set(backSideAdjNodeIndex, ncopy);
                        for (int interMedNode = footToRootNodes.size() - 2; interMedNode > 0; interMedNode--)
                        {
                            Node intermednode = footToRootNodes.get(interMedNode).copy();
                            intermednode.setDownPosTag(adjNode.getDownPosTag());
                            newRight.add(backSideAdjNodeIndex + 1, intermednode);
                            fringe.getAdjNodesOpenLeft().add(adjNodeIndex, intermednode);
                        }
                        copyF.setAdjNodesOpenRight(newRight);
                        contsCopy.set(i, copyF);
                        ArrayList<FringeAndProb> newConts = new ArrayList<FringeAndProb>();
                        //newConts.add(contsCopy);
                        //this.getLastFringe().setFringeConts(newConts);
                        FringeAndProb nfap = new FringeAndProb(contsCopy, fap.getnBestProbs().clone(), fap.getNext());
                        newConts.add(nfap);
                        this.getFutureFringe().setNext(newConts); //TODO check this!
                        return;
                    }
                }
                for (Node n : f.getAdjNodesOpenLeft())
                {
                    leftlist.add(n);
                }
                if (f.getSubstNode() != null)
                {
                    leftlist.add(f.getSubstNode());
                }
            }
        }
    }

    /**
     * fix elementary tree fringe and back side of aux tree root node in adjDown
     * operations with foot node at left side.
     */
    public Node fixFringeAdjLeft(Node adjNodeNew, Fringe prefFringe, Node adjNode)
    {
        Node oldFoot = getFringe().getAdjNodesOpenRight().remove(0);//remove original foot node

        Fringe lf = getLastFringe();
        int adjno = lf.getAdjNodesOpenRight().size() - 1;
        Node oldRootNode = lf.getAdjNodesOpenRight().remove(adjno);//elemroot node from back side.
        if (oldRootNode.getUpIndex() != -1)
        {
            System.out.println("unexpected case for root node with footLeft in elementary tree.");
        }
        Node newRootNode = adjNodeNew.copy();
        Node newFoot = adjNodeNew.copy();//make new foot node ; only one side relevant.
        //Node adjnode = null;
        if (newRootNode.getLambda().equals("@R"))
        {
            Node last = adjNodeNew;
            for (Node n : prefFringe.getAdjNodesOpenRight())
            {
                if (n == adjNodeNew)
                {
                    break;
                }
                if (n.getCategory().equals(adjNodeNew.getCategory()))
                {
                    last = n;
                }
            }
            newRootNode = new Node(new NodeKernel(adjNodeNew.getCategory(), last.getLambda(), last.getLambdaTimestamp()), oldRootNode.getDownIndex(), adjNodeNew.getUpIndex(),
                    (byte) adjNodeNew.getTimeStamp(), oldRootNode.getNodeId(), last.getOrigTree(), last.getPosInTree(),
                    adjNode.getDownPosTag(), adjNodeNew.getLeftMostCover());
            newFoot = new Node(new NodeKernel(adjNodeNew.getCategory(), last.getLambda(), last.getLambdaTimestamp()), adjNodeNew.getDownIndex(), oldFoot.getUpIndex(),
                    (byte) adjNodeNew.getTimeStamp(), oldFoot.getNodeId(), last.getOrigTree(), last.getPosInTree(),
                    adjNodeNew.getDownPosTag(), adjNodeNew.getLeftMostCover());
            adjNode = new Node(new NodeKernel(adjNodeNew.getCategory(), last.getLambda(), last.getLambdaTimestamp()), adjNode.getDownIndex(), adjNode.getUpIndex(),
                    (byte) adjNode.getTimeStamp(), adjNode.getNodeId(), last.getOrigTree(), last.getPosInTree(),
                    oldRootNode.getDownPosTag(), adjNodeNew.getLeftMostCover());
            adjNode.setDownPosTag(adjNodeNew.getDownPosTag());
            newFoot.setDownPosTag(adjNodeNew.getDownPosTag());
            newRootNode.setDownPosTag(adjNodeNew.getDownPosTag());
        } else
        {
            newRootNode.setDownIndex(oldRootNode.getDownIndex());
            newRootNode.setNodeId(oldRootNode.getNodeId());
            newRootNode.setDownPosTag(adjNode.getDownPosTag());
            newFoot.setDownPosTag(adjNode.getDownPosTag());
            newFoot.setUpIndex(oldFoot.getUpIndex());//use correct upper index for this foot node.
            newFoot.setNodeId(oldFoot.getNodeId());//id for old foot node.
            //newFoot.setUpPosTag(oldFoot.getUpPosTag());
            //.setDownIndex(adjNode.getDownIndex()); not necessary under this procedure.			
        }
        getFringe().getAdjNodesOpenRight().add(0, newFoot);//and put it back in
        lf.getAdjNodesOpenRight().add(newRootNode);
        //elemRootNode.setUpIndex(adjNode.getUpIndex()); not needed any more.
        return adjNode;
        /*Node elemRootNode =getRootNode(); 
         if (elemRootNode!=null) elemRootNode.setUpIndex(adjNodeNew.getUpIndex());
         geFringe().getAdjNodesOpenRight().get(0).setDownIndex(adjNodeNew.getDownIndex());*/
    }

    public Fringe removeRightFootFringe()
    {
        ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
        int lastindex = unaccessibleNodes.size() - 1;
        return unaccessibleNodes.remove(lastindex);
    }

    @SuppressWarnings("unchecked")
    public boolean checkForTrace()
    {
        boolean changed = false;
        ArrayList<Node> fringeAdjNodes = (ArrayList<Node>) fringe.getAdjNodes().clone();
        if (fringe != null && fringeAdjNodes != null && !fringeAdjNodes.isEmpty())
        {
            Node lastAdjNode = fringeAdjNodes.get(fringeAdjNodes.size() - 1);
            TreeState copy = copy();
            while (!copy.futureFringe.getFringe().isEmpty() && (isNullLexeme(lastAdjNode.getCategory())))
            {//|| isNullLexeme(firstAdjNodeCat))){
                copy.shiftNextUnaccessibleToAccessible();
                fringeAdjNodes = (ArrayList<Node>) copy.fringe.getAdjNodes().clone();
                changed = true;
                if (!fringeAdjNodes.isEmpty())
                {
                    lastAdjNode = fringeAdjNodes.get(fringeAdjNodes.size() - 1);
                } else
                {
                    break;
                }
            }
            if (changed == true && copy.getUnaccessibles().isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<TreeState> fixForTrace(String[] words)
    {
        ArrayList<TreeState> treeStateVersions = new ArrayList<TreeState>();
        ArrayList<Node> fringeAdjNodes = (ArrayList<Node>) fringe.getAdjNodes().clone();
        if (fringe != null && fringeAdjNodes != null && !fringeAdjNodes.isEmpty())
        {
            Node lastAdjNode = fringeAdjNodes.get(fringeAdjNodes.size() - 1);
            //String firstAdjNodeCat = fringeAdjNodes.get(0).getCategory();
            while (!this.futureFringe.getFringe().isEmpty() && (isNullLexeme(lastAdjNode.getCategory())))
            {//|| isNullLexeme(firstAdjNodeCat))){
                TreeState copy = copy();
                treeStateVersions.add(copy);
                shiftNextUnaccessibleToAccessible();
                String leftMost;
                if (copy.getFringe().getAdjNodesOpenRight().isEmpty())
                {
                    leftMost = words[copy.getWordCover()[1]].substring(0, words[copy.getWordCover()[1]].indexOf(" "));
                } else
                {
                    leftMost = copy.getFringe().getAdjNodesOpenRight().get(0).getCategory();
                }
                this.addLeftMostAnnot(leftMost + "+" + this.getFringe().getAdjNodesOpenRight().get(0).getCategory());
                fringeAdjNodes = (ArrayList<Node>) fringe.getAdjNodes().clone();
                if (!fringeAdjNodes.isEmpty())
                {
                    lastAdjNode = fringeAdjNodes.get(fringeAdjNodes.size() - 1);
                    //	firstAdjNodeCat = fringeAdjNodes.get(0).getCategory();
                } else
                {
                    break;
                }
            }
        }
        if (!treeStateVersions.contains(this))
        {
            treeStateVersions.add(this);
        }
        return treeStateVersions;
    }

    public ArrayList<Node> getNodesOnFringe()
    {
        ArrayList<Node> nodesOnStack = new ArrayList<Node>();
        nodesOnStack.addAll(fringe.getAdjoinableNodes());
        for (Fringe ua : this.futureFringe.getFringe())
        {
            nodesOnStack.addAll(ua.getAdjoinableNodes());
        }
        return nodesOnStack;
    }

    public boolean hasNonShadowLeaf()
    {
//        if (hasNonShadowLeaf)
//        {
//            return true;
//        }
//        return false;
        return hasNonShadowLeaf;
    }

    public void saveSpace()
    {
        shadowTreeList = null;
        fringe.saveSpace();
        //fringeString = new StringBuilder().append( fringe ).append(";").append( unaccessibleNodes ).toString();
        //fringe = null;
        //unaccessibleNodes = null;
    }

    public short makeUniqueNodeIndices(TreeState pref)
    {
        //find highest index in pref fringes.
        ArrayList<Fringe> flist = new ArrayList<Fringe>();
        flist.add(pref.fringe);
        flist.addAll(pref.futureFringe.getFringe());
        short highest = 0;
        for (Fringe f : flist)
        {
            for (Node n : f.getAdjNodesOpenRight())
            {
                if (n.getNodeId() > highest)
                {
                    highest = n.getNodeId();
                }
            }
        }
        if (highest <= 0)
        {
            return -1;
        }
        // set nodes in current tree to indices such that they don't overlap with pref tree.
        flist = new ArrayList<Fringe>();
        flist.add(fringe);
        flist.addAll(futureFringe.getFringe());
        for (Fringe f : flist)
        {
            for (Node n : f.getAdjNodesOpenRight())
            {
                n.setNodeId((short) (n.getNodeId() + highest));
            }
        }
        return highest;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<TreeState> expand(ChartEntry ce, int nBest, boolean useSemantics, ParserModel model)
    {
        ArrayList<FringeAndProb> expansions;
        expansions = findExpands(this.getFutureFringe().getNext());
        ArrayList<TreeState> expanded = new ArrayList<TreeState>();
        if (expansions.isEmpty())
        {
            return expanded;
        }
        for (FringeAndProb expansion : expansions)
        {
            ArrayList<Fringe> newExpansion = allFringeCopy(expansion.getFringe(), null);
            if (futureFringe.getFringe().isEmpty())
            {
                ParserOperation.pJoin(fringe.copy(), newExpansion);
            } else
            {
                ArrayList<Fringe> visibleFringe = (ArrayList<Fringe>) this.getFutureFringe().getFringe().clone();
                visibleFringe.add(0, fringe);
                ArrayList<Fringe> oldParts = allFringeCopy(visibleFringe, null);
                Fringe last = oldParts.remove(oldParts.size() - 1);
                ParserOperation.pJoin(last, newExpansion);
                newExpansion.addAll(0, oldParts);
            }
            TreeState nts = !useSemantics ?
                    new TreeState(isAux(), hasFootLeft(), getWordCover(), newExpansion.remove(0), newExpansion, expansion, shadowListDeepCopy(), hasNonShadowLeaf(), nBest) :
                    new DepTreeState(isAux(), hasFootLeft(), getWordCover(), newExpansion.remove(0), newExpansion, expansion, shadowListDeepCopy(), hasNonShadowLeaf(), nBest, 
                            ((DepTreeState)ce.getTreeState()).getDependencies(), model);
            //TODO add history from this.futureFringe.history to nts.getFutureFringe.getHist
            nts.getFutureFringe().addCutOffLocation(ce.getBuildBlocks().get(0), ce.getTreeState().wordcover[1]);
            boolean constraint = intersectHist(nts.getFutureFringe().getBBHist(), this.getFutureFringe().getBBHist());
            expanded.add(nts);
        }
        return expanded;
    }

    /**
     * Maintains buildBlock histories when expanding fringes. Checks whether
     * fringe constraints compatible if not, returns false.
     *
     * @param bbHist the new history
     * @param bbHist2 the old history
     * @return
     */
    private boolean intersectHist(LinkedList<LinkedList<BuildBlock>> bbHist,
            LinkedList<LinkedList<BuildBlock>> bbHist2)
    {
        for (int i = 0; i < bbHist.size(); i++)
        {
            LinkedList<BuildBlock> newhistoryAtwi = bbHist.get(i);
            LinkedList<BuildBlock> oldhistoryAtwi = bbHist2.get(i);
            if (oldhistoryAtwi == null || oldhistoryAtwi.isEmpty())
            {
                //nothing to update
                continue;
            } else if (newhistoryAtwi == null || newhistoryAtwi.isEmpty())
            {
                // replace the empty element by what's on the other history.
                bbHist.remove(i);
                bbHist.add(i, (LinkedList<BuildBlock>) oldhistoryAtwi.clone());
            }
            /*else{
             // the histories need to be merged
             // case 1 they are the same: do nothing.
             if (newhistoryAtwi.size()==oldhistoryAtwi.size()){
             boolean same = true;
             for (BuildBlock b : oldhistoryAtwi){
             if (!newhistoryAtwi.contains(b)){
             same = false;
             }
             }
             if (same) continue;
             }
             // case 2 find out which constraints clash: set new constraints to intersect
             // old must contain all constraints that are in new.
             LinkedList<BuildBlock> newRest = (LinkedList<BuildBlock>) newhistoryAtwi.clone();
             newRest.removeAll(oldhistoryAtwi);

             //		System.out.println("new: "+newhistoryAtwi);
             //		System.out.println("old: "+oldhistoryAtwi);
             //		System.out.println("rest: "+newRest);
             if (!newRest.isEmpty()){
             //			System.out.println("new: "+newhistoryAtwi);
             //			System.out.println("old: "+oldhistoryAtwi);
             //			System.out.println("rest: "+newRest);
             return false;
             }
             }*/

        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private ArrayList<FringeAndProb> findExpands(ArrayList<FringeAndProb> fringeConts)
    {
        ArrayList<FringeAndProb> result = new ArrayList<FringeAndProb>();
        if (fringeConts == null)
        {
            return result;
        }
        for (FringeAndProb expansionFAP : fringeConts)
        {
            ArrayList<Fringe> expansion = expansionFAP.getFringe();
            if (expansion.size() == 1)
            {
                Fringe exp = expansion.get(0);
                if (expansionFAP.getNext() == null)
                {
                    result.add(expansionFAP);
                    continue;
                }
                ArrayList<FringeAndProb> fe = findExpands(expansionFAP.getNext());
                for (FringeAndProb furtherexp : fe)
                {
                    ArrayList<Fringe> fep = furtherexp.getFringe();
                    ArrayList<Fringe> newexp = allFringeCopy(fep, null);
                    ArrayList<FringeAndProb> fconts = furtherexp.getNext();//(ArrayList<ArrayList<Fringe>>) newexp.get(newexp.size()-1).getFringeContClone();//Clone TODO check whether clone necessary or not!
                    LinkedList<LinkedList<BuildBlock>> cutofflist = furtherexp.getBBHist();
                    ParserOperation.pJoin(exp.copy(), newexp);
                    FringeAndProb combinedfexp = new FringeAndProb(newexp, furtherexp.getnBestProbs(), fconts, cutofflist);
                    result.add(combinedfexp);
                }
            } else
            {
                result.add(expansionFAP);
            }
        }
        return result;
    }
    /*	@SuppressWarnings("unchecked")
     public ArrayList<TreeState> expand(ChartEntry ce) {
     ArrayList<ArrayList<Fringe>> expansions;
     expansions = findExpands(this.getLastFringe().getFringeConts(), ce);
     if (expansions == null ) return null;
     ArrayList<TreeState> expanded = new ArrayList<TreeState>();
     for (ArrayList<Fringe> expansion: expansions){
     ArrayList<Fringe> newExpansion = allFringeCopy(expansion, null);
     if (this.futureFringe.getFringe().isEmpty()){
     ArrayList<ArrayList<Fringe>> fconts = (ArrayList<ArrayList<Fringe>>) newExpansion.get(newExpansion.size()-1).getFringeContClone();//Clone TODO check whether clone necessary or not!
     LinkedList<LinkedList<BuildBlock>> cutofflist = newExpansion.get(0).getCutOffLocations();
     ParserOperation.pJoin(fringe.copy(), newExpansion);
     newExpansion.get(newExpansion.size()-1).setFringeConts(fconts);
     newExpansion.get(0).addAllCutOffLocs(cutofflist);
     }
     else{
     ArrayList<Fringe> oldParts = allFringeCopy(this.futureFringe.getFringe(), null);
     Fringe last = oldParts.remove(oldParts.size()-1);
     LinkedList<LinkedList<BuildBlock>> cutofflist = newExpansion.get(0).getCutOffLocations();
     ArrayList<ArrayList<Fringe>> fconts = (ArrayList<ArrayList<Fringe>>) newExpansion.get(newExpansion.size()-1).getFringeContClone();//Clone TODO check whether clone necessary or not!
     ParserOperation.pJoin(last, newExpansion);
     //newExpansion.get(newExpansion.size()-1).setFringeConts(fconts);
     newExpansion.get(0).addAllCutOffLocs(cutofflist);
     newExpansion.addAll(0, oldParts);
     newExpansion.add(0, fringe.copy());
     }
			
     TreeState nts = new TreeState(isAux(), hasFootLeft(), getWordCover(), newExpansion.remove(0), 
     newExpansion, shadowListDeepCopy(),	hasNonShadowLeaf());
     expanded.add(nts);
     }
     return expanded;		
     }

     @SuppressWarnings("unchecked")
     private ArrayList<ArrayList<Fringe>> findExpands(ArrayList<ArrayList<Fringe>> fringeConts, ChartEntry ce) {
     ArrayList<ArrayList<Fringe>> result = new ArrayList<ArrayList<Fringe>>();
     if (fringeConts == null) return result;
     for (ArrayList<Fringe> expansion : fringeConts){
     if (!ParserModel.train&& !expansion.get(expansion.size()-1).containsCE(ce)){
     continue;
     }
     if (expansion.size()==1){
     Fringe exp = expansion.get(0);
     ArrayList<ArrayList<Fringe>> fe = findExpands(exp.getFringeConts(), ce);
     if (fe == null) continue;
     if (fe.isEmpty()) {
     result.add(expansion);
     }
     for (ArrayList<Fringe> furtherexp : fe){
     ArrayList<Fringe> newexp = allFringeCopy(furtherexp, null);
     ArrayList<ArrayList<Fringe>> fconts = (ArrayList<ArrayList<Fringe>>) newexp.get(newexp.size()-1).getFringeContClone();//Clone TODO check whether clone necessary or not!
     LinkedList<LinkedList<BuildBlock>> cutofflist = newexp.get(0).getCutOffLocations();
     ParserOperation.pJoin(exp.copy(), newexp);
     newexp.get(newexp.size()-1).setFringeConts(fconts);
     newexp.get(0).addAllCutOffLocs(cutofflist);
     result.add(newexp);
     }
     }
     else {
     result.add(expansion);
     }
     }
     if (result.isEmpty()) 
     return null; //in case everything was pruned.
     return result;
     }*/

    public Fringe getLastFringe()
    {
        if (this.futureFringe.getFringe().isEmpty())
        {
            return fringe;
        }
        return this.getLastUnaccessible();
    }

    public boolean hasNoSubstBeforeLexAnchor()
    {
        if (fringe.getSubstNode() != null)
        {
            return false;
        } else if (!isNullLexeme(fringe.getLastAdjNode().getCategory()) && fringe.getLastAdjNode().getDownIndex() == 0)
        {
            return true;
        }
        Fringe nextFringe = fringe;
        int i = 0;
        while (isNullLexeme(nextFringe.getLastAdjNode().getCategory()) || (footleft && fringe == nextFringe))
        {
            nextFringe = this.futureFringe.getFringe().get(i);
            if (nextFringe.getSubstNode() != null)
            {
                return false;
            } else if (!isNullLexeme(nextFringe.getLastAdjNode().getCategory()) && nextFringe.getLastAdjNode().getDownIndex() == 0)
            {
                return true;
            }
            i++;
        }
        return true;
    }

    public void fixKernels(Node rootNode)
    {
        Fringe f = this.getUnaccessibles().get(0);

        for (int i = 0; i < f.getAdjNodesOpenRight().size(); i++)
        {
            Node n = f.getAdjNodesOpenRight().get(i);
            if (!rootNode.getCategory().equals(n.getCategory()))
            {
                break;
            }
            if (n.getLambda().equals("@R") || n.getDownPosTag().equals("-"))
            {
//				System.err.println("TreeState.fixKernels: n.getLambda().equals(@R)");
                Node removedNode = f.getAdjNodesOpenRight().remove(i);
                Node newNode = rootNode.copy();
                newNode.setNodeId(removedNode.getNodeId());
                newNode.setDownIndex(removedNode.getDownIndex());
                newNode.setUpIndex(removedNode.getUpIndex());
                newNode.setTimeStamp((short) removedNode.getTimeStamp());
                //newNode.setDownPosTag(rootNode.getDownPosTag()); not necessary as is copy of rootNode 
                newNode.setLeftMostCover(removedNode.getLeftMostCover());
                f.getAdjNodesOpenRight().add(i, newNode);
            }
        }

    }

    /**
     * leftmost annotation describes the right side of the subtree: the POS of a
     * node's rightmost daughter + the POS tag of the word before that.
     *
     * @param leftMostAnnot
     */
    @SuppressWarnings("unchecked")
    public void addLeftMostAnnot(String leftMostAnnot)
    {
        if (!ParserModel.useLeftMost)
        {
            return;
        }
        //for nodes from anchor up spine, stopping as soon as a node which is already annotated is encountered.
        // (an already annotated node must have a leftmost child which has already been seen and therefore is more to
        // the left than the current node.
        for (Node n : this.fringe.getAdjNodesOpenRight())
        {
            if (n.getLeftMostCover().equals("?"))
            {
                n.setLeftMostCover(leftMostAnnot);
            } else
            {
                break;
            }
        }
        // need to walk up the spine even for nodes which are on the future fringe.
        // therefore need to account for nodes which are not on spine -- these are stored on "others".
        ArrayList<Node> others = new ArrayList<Node>();
        others.addAll(this.fringe.getAdjNodesOpenLeft());
        if (this.fringe.getSubstNode() != null)
        {
            others.add(this.fringe.getSubstNode());
        }
        if (!this.futureFringe.getFringe().isEmpty())
        {
            for (Fringe next : this.futureFringe.getFringe())
            {
                for (Node n : next.getAdjNodesOpenRight())
                {
                    if (!others.isEmpty()
                            && n.getCategory().equals(others.get(others.size() - 1).getCategory())
                            && n.getNodeId() == others.get(others.size() - 1).getNodeId())
                    {
                        // this is not a node on spine, but backside of a node previously stored in others.
                        others.remove(others.size() - 1);
                    } else
                    {// this must be a node on the spine.
                        if (!others.isEmpty())
                        {
                            System.err.println("TreeState:addLeftMostAnnot others is not empty. check case.");
                            continue;// is this some error case? all nodes on openRight should be on others.
                        }
                        if (n.getLeftMostCover().equals("?")) //node on spine which needs to get new annotation.
                        {
                            n.setLeftMostCover(leftMostAnnot);
                        } else
                        {
                            return;
                        }
                    }
                }
                others.addAll(next.getAdjNodesOpenLeft());
                if (next.getSubstNode() != null)
                {
                    others.add(next.getSubstNode());
                }
            }
        }
        ArrayList<FringeAndProb> next = this.getFutureFringe().getNext();
        if (next != null && !next.isEmpty() && !others.isEmpty())
        {
            for (FringeAndProb fap : next)
            {
                fixLeftMostAnnot(fap, (ArrayList<Node>) others.clone(), leftMostAnnot);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fixLeftMostAnnot(FringeAndProb fap, ArrayList<Node> others,
            String prevPOS)
    {
        others.remove(others.size() - 1);
        for (Fringe next : fap.getFringe())
        {
            for (Node n : next.getAdjNodesOpenRight())
            {
                if (!others.isEmpty()
                        && n.getCategory().equals(others.get(others.size() - 1).getCategory())
                        && n.getNodeId() == others.get(others.size() - 1).getNodeId())
                {
                    others.remove(others.size() - 1);
                } else
                {
                    if (!others.isEmpty())
                    {
                        continue;
                    }
                    if (n.getLeftMostCover().equals("?"))
                    {
                        n.setLeftMostCover(prevPOS);
                    } else
                    {
                        return;
                    }
                }
            }
            others.addAll(next.getAdjNodesOpenLeft());
            if (next.getSubstNode() != null)
            {
                others.add(next.getSubstNode());
            }

            ArrayList<FringeAndProb> nextrec = fap.getNext();
            if (nextrec != null && !nextrec.isEmpty() && !others.isEmpty())
            {
                for (FringeAndProb faprecurs : nextrec)
                {
                    fixLeftMostAnnot(faprecurs, (ArrayList<Node>) others.clone(), prevPOS);
                }
            }
        }
    }

    public Node findNodeInFringeById(short nodeId)
    {
        for(Node n : getNodesOnFringe())
        {
            if(n.getNodeId() == nodeId)
                return n;
        }
        return null;
    }
    
    public Node findNodeInFringeByIdApprox(short nodeId)
    {
        for(Node n : getNodesOnFringe())
        {
            if(inRange(n.getNodeId(), nodeId, 5))
                return n;
        }
        return null;
    }
    
    private boolean inRange(short source, short target, int range)
    {
        return Math.abs(source - target) <= range;
    }
            
    public Node findIdenticalNodeInFringe(Node node)
    {
        for(Node n : getNodesOnFringe())
        {
            if(n.identical(node))
                return n;
        }
        return null;
    }
    
    public Node findIdenticalSubstNodeInFringe(Node node)
    {
        for(Node n : getNodesOnFringe())
        {
            if(n.getCategory().equals(node.getCategory()) && 
                    n.getTimeStamp() == node.getTimeStamp() && 
                    n.getUpIndex() == node.getUpIndex())
                return n;
        }
        return null;
    }         
}
