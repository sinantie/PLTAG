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

import fig.basic.LogInfo;
import fig.basic.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import pltag.corpus.TagNodeType;
import pltag.corpus.StringTree;
import pltag.parser.semantics.Dependencies;
import pltag.util.Utils;

/**
 * just a wrapper class for the StringTree from the converter.
 * @author vera
 *
 */
public class StringTreeAnalysis implements Comparable<StringTreeAnalysis>
{
    private Options opts;
    private StringTree analysis;
    private Fringe fringe;
    private String trace;
    private StringBuilder builtTrace = new StringBuilder();
    //private SuperTagger supertagger;
    private FreqCounter fcnt;
    private int integrationPointId = Integer.MIN_VALUE; // empty value
    private String integrationPosition = "";
    private Dependencies dependencies;
    private double score = Double.MIN_VALUE;
    
    public static Pattern predMarker = Pattern.compile(".*[1-9]_[1-9].*");

    public StringTreeAnalysis(Options opts, BuildBlock bb, Fringe targetFringe)
    {
        this.opts = opts;
        fcnt = new FreqCounter(opts);
        if (bb.getOperation() == ParserOperation.initial)
        {
            analysis = bb.getElemTree();
            TreeState ts = new TreeState(analysis, (short) 0, opts.nBest);
            if (analysis.isAuxtree() && !analysis.hasFootLeft())
            {
                ts.removeRightFootFringe();
            }
            fringe = ts.getFringeAfterLastNonShadowNonNullLexAnchor();
            while (!fringe.equals(targetFringe))
            {
                if (ts.getUaAfter(fringe).isEmpty())
                {
                    System.err.println("Can't determine targetFringe in initial operation.");
                }
                fringe = ts.getUaAfter(fringe).get(0);
            }
            trace = analysis.toString();
        }
        else
        {
            System.err.println("Error in use of StringTreeAnalysis constructor.");
        }
    }

    public StringTreeAnalysis(Options opts, StringTree st, Fringe f, String t, int op, FreqCounter fcnt, StringBuilder btrace)
    {
        this.opts = opts;
        analysis = st;
        fringe = f;
        trace = t;
        //this.integrationPointId = op; // SHOULD ADD ???
        this.fcnt = fcnt;
        this.builtTrace = btrace;
    }

    public int getComplexity()
    {
        return analysis.getComplexity();
    }
    
    public double getProbability()
    {
        return analysis.getProbability();
    }
    
    public void setTrace(WordProbElement we, List<TreeProbElement> tpelist, int ipi, ParserOperation operation, StringTree elementaryTree,
                         TreeState prefixTS, String POScontext)
    {
        StringBuilder tracebuilder = new StringBuilder().append("\ntreeProbs:");
        TreeProbElement maintpe = null;
        for (TreeProbElement tpe : tpelist)
        {
            if (!tpe.isNoneAdj())
            {
                tracebuilder.append(tpe.toString());
                maintpe = tpe;
            }
            fcnt.addSingleTreeProbEl(tpe);
        }
        tracebuilder.append("\nwordProb: ").append(we);
        trace = tracebuilder.toString();
        fcnt.addSingleWordProbEl(we);
        String currentMainLeaf = elementaryTree.getMainLeaf(elementaryTree.getRoot());
        String elementTreeStruct = elementaryTree.getUnlexStruct(elementaryTree.getRoot());
        String operationName = operation.name();
        String currentPosCat;
        if (currentMainLeaf.contains("\t"))
        {
            currentPosCat = currentMainLeaf.substring(0, currentMainLeaf.indexOf("\t"));
        }
        else
        {
            currentPosCat = currentMainLeaf;
        }
        String elementaryTreeFringe = TreeState.getAuxFringe(elementaryTree, -1, new short[2], new HashMap<Short, Node>()).toCatString();

        if (operation == ParserOperation.initial)
        {
            fcnt.addFringeProb(new Fringe(), "-", currentPosCat, elementaryTreeFringe, "SoS");
            fcnt.addStructProb(elementTreeStruct, elementaryTreeFringe, currentPosCat);
        }
        else if (operation == ParserOperation.verify)
        {
            String integrationPointCat = analysis.getCategory(ipi);
            POScontext = getPrevContext();
            fcnt.addFringeProb(prefixTS.getFringe(), POScontext, currentPosCat, elementaryTreeFringe, integrationPointCat);
            fcnt.addStructProb(elementTreeStruct, elementaryTreeFringe, currentPosCat);

        }
        else
        {
            String integrationPointCat = analysis.getCategory(ipi);
            if (operationName.endsWith("F"))
            {
                POScontext = getPrevContext();
            }
            fcnt.addFringeProb(prefixTS.getFringe(), POScontext, currentPosCat, elementaryTreeFringe, integrationPointCat);
            fcnt.addStructProb(elementTreeStruct, elementaryTreeFringe, currentPosCat);
        }
    }

    private String getPrevContext()
    {
        String[] leaves = this.getStringTree().toString().split("<>");
        int length = leaves.length;
        String posContext = "";
        for (int i = length - 3; i >= Math.max(0, length - 6); i--)
        {
            posContext = leaves[i].substring(leaves[i].lastIndexOf("(") + 2, leaves[i].lastIndexOf("^")) + "-" + posContext;
        }
        if (posContext.equals(""))
        {
            return "-";
        }
        else
        {
            return posContext;
        }
    }

    public List<Pair<String, String>> getLeavesPreTerminal()
    {
        return analysis.getLeavesPreTerminal(analysis.getRoot(), new ArrayList());
    }
    
    public StringTreeAnalysis integrate(BuildBlock bb, TreeState prevTS, TreeState targetTreeState, String nextPOStag, boolean train)
    {
        //fringe = prevTS.getFringe();
        StringTree elemtree = bb.getElemTree();
        ParserOperation operation = bb.getOperation();
        int adjNodeNumber = bb.getAdjNodeNumber();
        StringTreeAnalysis result = operation.integrate(opts, this, elemtree, adjNodeNumber, targetTreeState);
        if (result == null || result.integrationPointId == Integer.MIN_VALUE)
//        if (result == null || result.integrationPointId.equals(""))
        {
            return null;
        }
        result.analysis.setProbability(analysis.getProbability() + bb.getProbability());
        result.trace = trace;
        result.trace += bb.getProbability() + "\t";
        //if (ParserModel.train)
        if(train)
            result.setTrace(bb.getFreqCounter().getWordProbs().get(0), bb.getFreqCounter().getTreeProbs(), result.integrationPointId, operation, elemtree, prevTS, nextPOStag);
        return result;
    }

    public StringTreeAnalysis clone()
    {        
        return new StringTreeAnalysis(opts, analysis.copy(false), // don't copy string tree representation, as it is very costly (call to getStructure() )
                fringe == null ? null : fringe.copy(), trace, 
                integrationPointId, new FreqCounter(fcnt), new StringBuilder(this.builtTrace));     
    }

    public String toString()
    {
        return analysis.toString();
    }

    public void substitute(StringTree initialTree, TreeState targetTreeState)
    {
        ArrayList<Integer> nodes = analysis.getNodes();
        fixShadowRoots(initialTree);
        for (Integer node : nodes)
        {
            // kind of risky - need to make sure it's the first substtree and that it's on fringe. This should be
            // the case because  getNodes() returns the nodes in order, and they are ordered depth-first from left to right.
            if (this.analysis.getNodeType(node) == TagNodeType.subst
                    && this.analysis.getCategory(node).equals(initialTree.getCategory(analysis.getRoot())))
            {
                //CREATE UNIQUE TREE INDICES!
                //shift all of local tree indices that are greater than 'node' (by number of nodes in initialTree -1)
                analysis.shiftNodeIndices(node + 1, initialTree.getNodes().size() - 1);
                //shift all of the indices in initialTree that are right of the root by the number of nodes in analysis tree that are
                // left of 'node'. the root node of the initialTree should then have the same index as the substitution node.
                initialTree.shiftNodeIndices(1, node - 1);
                //MAKE SUBSTITUTION
                //override analysis[node] with initialTree root node information (pay attention to indeces)
                byte substUpIndex = analysis.getUpperIndex(node);
                Integer substParent = analysis.getParent(node);
                //COPY REMAINING NODES INTO RESULT TREE
                //add other nodes from initialTree to analysis tree, thereby remember to use new index for root node
                addNodes(initialTree);
                analysis.putIndexUp(node, substUpIndex);
                analysis.putParent(node, substParent);
                integrationPointId = node;
//                integrationPointId = String.valueOf(node);
                this.integrationPosition = "-1/-1";
                break;
            }
        }
        fringe = targetTreeState.getFringe();
        /*Fringe f2 = new TreeState(analysis, (short)0,  targetTreeState.getWordCover()).getFringe();
        if (checkSynchronity(fringe, f2)==null){
        System.out.println(fringe);
        System.out.println(f2);
        }//*/

    }
    /*
     * following is the debugging variant where we check that both fringes are indeed the same.
    TreeState ts = new TreeState(analysis,0);
    Fringe ff = ts.getFringeAfterLastNonShadowNonNullLexAnchor();
    ArrayList<Fringe> flist = new ArrayList<Fringe>();
    flist.add(ff);
    flist.addAll(ts.getUaAfter(ff));//= ts.getFringesBeforeFirstNullLexeme();//VERA
    for (Fringe f : flist){
    fringe = checkSynchronity(targetTreeState.getFringe(), f);
    if (fringe !=null) break;
    }
    if (fringe == null){
    System.out.println("FRINGE MISMATCH");
    StatsRunner.printConsole("FRINGE MISMATCH");
    }
     */

    private Fringe checkSynchronity(Fringe targetFringe, Fringe actualFringe)
    {
        //if (targetFringe.getAdjoinableNodes().size() == actualFringe.getAdjoinableNodes().size()){
        //	return actualFringe;
        //}
        //if (targetFringe.getAdjoinableNodes().size() == cutOffForbidden(actualFringe.copy()).getAdjoinableNodes().size() ){
        //	return cutOffForbidden(actualFringe.copy());
        //}
        //else{
        if (checkSameBesidesCutoff(targetFringe, actualFringe))
        {
            return actualFringe;
        }
        else
        {
            if (actualFringe.hasTraceRight() || actualFringe.getLastAdjNode().getCategory().matches("[a-z]+"))
            {
                return null;
            }
            else
            {
                if (opts.verbose)
                {
                    LogInfo.error(actualFringe.getLastAdjNode().getCategory() + "========STA:checkSync==============");
                    LogInfo.error(targetFringe);
                    LogInfo.error(actualFringe);
                }
                return actualFringe;
            }
        }
        //}
    }

    private void addNodes(StringTree initialTree)
    {
        for (int i : initialTree.getNodes())
        {
//            String i = i + "";
            analysis.putFullCategory(i, initialTree.getFullCategory(i));
            analysis.putCategory(i, initialTree.getCategory(i));
            analysis.putNodeType(i, initialTree.getNodeType(i));
            analysis.putHeadChild(i, initialTree.isHeadChild(i));
            analysis.putIndexDown(i, initialTree.getLowerIndex(i));
            analysis.putIndexUp(i, initialTree.getUpperIndex(i));
            analysis.putParent(i, initialTree.getParent(i));
            analysis.putChildren(i, initialTree.getChildren(i));
//            analysis.putChildren(i + "", initialTree.getChildren(i));
            //analysis.putAdjoinable(i, initialTree.isAdjPossible(i));
        }
    }

    public boolean adjoin(StringTree auxTree, int adjNodeNumber, TreeState targetTreeState, Boolean adjNodeShadowStatus)
    {//, boolean traceleft, boolean traceright) {		
        //CREATE UNIQUE TREE INDICES!
        //this is different for foot node left vs. foot node right (TIG!)
        //System.out.println(auxTree.analysis);
        boolean footLeft = auxTree.hasFootLeft();
        // create unique shadow indices
        fixShadowRoots(auxTree);
        //get the adjunction node (id within 'this' StringTreeAnalysis)
        String cat = auxTree.getCategory(auxTree.getRoot());
        Fringe currentFringe = getAdjCandidatesEfficient(footLeft, targetTreeState.getWordCover());
        if (currentFringe == null)
        {
            return false;
        }
        ArrayList<Node> candidates = selectCandidatesFromFringe(currentFringe, adjNodeShadowStatus, cat, footLeft);

        int noOfPossibleAdj = candidates.size();
        if (noOfPossibleAdj <= adjNodeNumber)
        {
            return false;
        }
        int node = this.getAdjunctionNode(adjNodeNumber, candidates);
        //for calculateMostProbableTree tree, put on historic fringe all nodes between root or last lexical anchor to adj site.		
		/*boolean found = false;
        boolean currentFringeEndsInLexeme =this.fringe.getLastAdjNode().getDownIndex()==0; 
        fringe.setClusterNumberCount(false);
        for (Node n : currentFringe.getAdjNodesOpenRight()){//TODO: add test for whether end of current fringe of calculateMostProbableTree tree is a lexical anchor. In that case don't break but "continue" (just jumping the adj node).
        if (n.getNodeId()==node){
        found = true;
        if (currentFringeEndsInLexeme){continue;}
        else break;
        }
        this.fcnt.addNoneAdjs(n, traceleft, traceright);
        }
        fringe.setClusterNumberCount(true);
        for (Node n : currentFringe.getAdjNodesOpenLeft()){
        if (!currentFringeEndsInLexeme && found) break;
        else if (!found && n.getNodeId()==node){
        if (currentFringeEndsInLexeme) continue;
        else break;
        }
        this.fcnt.addNoneAdjs(n, traceleft, traceright);
        }*/
//		TreeState auxtreetreestate = new TreeState(auxTree, (short)0);//TODO: this is not always an elementary tree. If this is prefix tree, need to find fringe after last lex anchor.
        if (footLeft)
        {
            // foot is leftmost child in auxtree
            int numberOfNodesBetweenFootAndRoot = getPathLength(auxTree);
            // shift all of local tree indices that are greater than 'node' by number of nodes between foot node and root node.
            analysis.shiftNodeIndices(node + 1, numberOfNodesBetweenFootAndRoot);
            // shift all of the tree indices right to the adjunction site by auxTree nodeNumber - number of nodes betw. root & foot
            int rightSibOfAdjNode = getRightSibOfNode(analysis, node);
//			wrong
            analysis.shiftNodeIndices(rightSibOfAdjNode, auxTree.getNodes().size() - 1 - numberOfNodesBetweenFootAndRoot);
            // shift all nodes in auxtree by node-1 indices.
            auxTree.shiftNodeIndices(1, node - 1);
            // shift all nodes from right sibling of foot by number of children under node (rightSibOfNode - node -1)
//			wrong
            auxTree.shiftNodeIndices(getRightSibOfNode(auxTree, auxTree.getFoot()), rightSibOfAdjNode - node - 2);
        }
        else
        {
            //footRight: foot is rightmost child in auxtree
            // shift all of the local tree indices that are greater than 'node' by the size of the auxtree -1
            analysis.shiftNodeIndices(node + 1, auxTree.getNodes().size() - 1);
            //if adjunction at foot node, must also shift foot node position.
            if (analysis.getFoot() == node )
//            if (analysis.getFoot().equals(node + ""))
            {
                analysis.setFoot(node + auxTree.getNodes().size() - 1);
            }
            // shift all of the nodes in the auxtree by 'node'-1.
            auxTree.shiftNodeIndices(1, node - 1);
        }
        integrationPointId = node;
        integrationPosition = adjNodeNumber + "/" + noOfPossibleAdj;
        //MAKE ADJUNCTION
        //override analysis[node] with auxTree root node information (pay attention to indeces for foot and root node)
        // & correct organization for assembling of children) & parents or original children
        byte adjSiteUpIndex = analysis.getUpperIndex(node);
        byte adjSiteDownIndex = analysis.getLowerIndex(node);
        Integer adjSiteParent = analysis.getParent(node);
        TagNodeType adjSiteNodeType = analysis.getNodeType(node);
        ArrayList<Integer> adjSiteChildren = analysis.getChildren(node);
        //COPY REMAINING NODES INTO RESULT TREE
        //add other nodes from initialTree to analysis tree, thereby remember to use new index for root node
        addNodes(auxTree);
        // CORRECT for overwriting
        Integer auxtreefoot = auxTree.getFoot();
        analysis.putIndexUp(node, adjSiteUpIndex);
        analysis.putIndexDown(auxtreefoot, adjSiteDownIndex);
//        analysis.putIndexDown(Integer.parseInt(auxtreefoot), adjSiteDownIndex);
        analysis.putParent(node, adjSiteParent);
        this.analysis.putChildren(auxtreefoot, adjSiteChildren);
        analysis.putNodeType(auxtreefoot, adjSiteNodeType);
        for (Integer c : analysis.getChildren(node))
        {
            analysis.putHeadChild(c, false);
        }
        analysis.putHeadChild(auxtreefoot, true);

        for (Integer c : adjSiteChildren)
        {
            analysis.putParent(c, auxtreefoot);
        }
        fringe = targetTreeState.getFringe();
        /*Fringe f2 = new TreeState(analysis, (short)0,  targetTreeState.getWordCover()).getFringe();
        if (checkSynchronity(fringe, f2)==null){
        System.out.println(fringe);
        System.out.println(f2);
        checkSynchronity(fringe, f2);
        }//*/

        return true;
    }


    /*
    // debugging version where we check that the fringes are actually the same.
    TreeState ts = new TreeState(analysis,0);
    Fringe ff = ts.getFringeAfterLastNonShadowNonNullLexAnchor();
    ArrayList<Fringe> flist = ts.getUaAfter(ff);//getFringesBeforeFirstNullLexeme();
    flist.add(0, ff);
    for (Fringe f : flist){
    fringe = checkSynchronity(targetTreeState.getFringe(), f);
    if (fringe !=null) break;
    }
    if (fringe == null || flist.isEmpty()){
    System.out.println("FRINGE MISMATCH");
    StatsRunner.printConsole("FRINGE MISMATCH");
    }
     */
    private void fixShadowRoots(StringTree auxTree)
    {
        ArrayList<Integer> thisShadowRoots = analysis.getShadowSourceTreesRootList();
        ArrayList<Integer> auxTreeShadowRoots = auxTree.getShadowSourceTreesRootList();
        if (thisShadowRoots.size() > 0 && auxTreeShadowRoots.size() > 0)
        {
            if (thisShadowRoots.size() < auxTreeShadowRoots.size())
            {
                analysis.updateIndices(auxTreeShadowRoots, auxTree);

            }
            else
            {
                auxTree.updateIndices(thisShadowRoots, analysis);
            }
        }
    }

    private Fringe getAdjCandidatesEfficient(boolean footLeft, short[] wordcover)
    {
        //TreeState ts = new TreeState(analysis, (short) 0);
        Fringe fr = null;
        int anchors = 0;
        for (int i : analysis.getNodes())
        {
            if (analysis.getNodeType(i) == TagNodeType.anchor)
            {
                anchors++;
                if (anchors >= 2)
                {
                    break;
                }
            }
        }
        if (anchors <= 1 && !footLeft && trace.equals(""))
        {//trace used only as a proxi for whether this is the first or second word. 
            // If it is the first word, and something is integrated into it, need to go past lex anchor. In that case (first word), trace should not be empty.
            TreeState ts = new TreeState(analysis, (short) 0, opts.nBest);
            fr = ts.getFringe();
            if (TreeState.isNullLexeme(fr.getAdjNodesOpenLeft().get(fr.getAdjNodesOpenLeft().size() - 1).getCategory()))
            {
                ArrayList<Fringe> flist = new ArrayList<Fringe>();
                flist.add(fr);
                flist.addAll(ts.getUaAfter(fr));//= ts.getFringesBeforeFirstNullLexeme();//VERA
                for (Fringe f : flist)
                {
                    fr = checkSynchronity(this.fringe, f);
                    if (fr != null)
                    {
                        break;
                    }
                }
                if (fr == null || flist.isEmpty())
                {
                    //System.out.println("FRINGE MISMATCH");
                    //StatsRunner.printConsole("FRINGE MISMATCH");
                    return null;
                }
            }
        }
        else
        {
            //Fringe ff = ts2.getFringeAfterLastNonShadowNonNullLexAnchor();//
            TreeState ts2 = new TreeState(analysis, (short) 0, wordcover, opts.nBest);
            //if (ts2.toString().contains(", null")){
            //	new TreeState(analysis, (short)0, wordcover);
            //}
            Fringe ff = ts2.getFringe();
            ArrayList<Fringe> flist = new ArrayList<Fringe>();
            flist.add(ff);
            //flist.addAll(ts2.getUaAfter(ff));//= ts.getFringesBeforeFirstNullLexeme();//VERA  
            flist.addAll(ts2.getUnaccessibles());//
            for (Fringe f : flist)
            {
                fr = checkSynchronity(this.fringe, f);
                if (fr != null)
                {
                    break;
                }
            }
            if (fr == null || flist.isEmpty())
            {
                //	System.out.println("FRINGE MISMATCH");
                //	StatsRunner.printConsole("FRINGE MISMATCH");
                return null;
            }
        }
        return fr;
    }

    private ArrayList<Node> selectCandidatesFromFringe(Fringe fr, Boolean adjNodeShadowStatus, String cat, boolean footLeft)
    {
        ArrayList<Node> adjoinableNodes;
        ArrayList<Node> candidates = new ArrayList<Node>();
        if (footLeft)
        {
            adjoinableNodes = fr.getAdjNodesOpenRight();
        }
        else
        {
            adjoinableNodes = new ArrayList<Node>();
            adjoinableNodes.addAll(fr.getAdjNodesOpenLeft());
            if (fr.getSubstNode() != null)
            {
                adjoinableNodes.add(fr.getSubstNode());
            }
        }
        for (Node n : adjoinableNodes)
        {
            if (n.getCategory().equals(cat))
            {
                if (adjNodeShadowStatus == null || n.isShadow() == adjNodeShadowStatus)
                {
                    candidates.add(n);
                }
            }
        }
        return candidates;
    }

    private boolean checkSameBesidesCutoff(Fringe targetFringe, Fringe testFringe)
    {
        int sizediff = testFringe.getAdjoinableNodes().size() - targetFringe.getAdjoinableNodes().size();
        for (int i = 1; i <= sizediff; i++)
        {
            if (!testFringe.getAdjNodesOpenRight().isEmpty())
            {
                testFringe.getAdjNodesOpenRight().remove(0);
            }
            else if (!testFringe.getAdjNodesOpenLeft().isEmpty())
            {
                testFringe.getAdjNodesOpenLeft().remove(0);
            }
            else
            {
                testFringe.setSubstNode(null);
            }
        }
        if (sizediff < 0)
        {
            return false;
        }
        int i = 0;
        for (Node n : targetFringe.getAdjoinableNodes())
        {
            Node tn = testFringe.getAdjoinableNodes().get(i);
            if (!n.getCategory().equals(tn.getCategory()))
            {
                return false;
            }
            i++;
        }
        return true;
    }

    private int getAdjunctionNode(int adjNodeNumber, ArrayList<Node> candidates)
    {
        if (candidates.size() > 1)
        {
            if (opts.verbose)
            {
                Utils.logs(candidates.toString());
            }
            for (Node n : candidates)
            {
                if (adjNodeNumber == 0)
                {
                    return n.getNodeId();
                }
                adjNodeNumber--;
            }
        }
        return candidates.get(adjNodeNumber).getNodeId();
    }

    private int getPathLength(StringTree tree)
    {// foot, String root) {
        int onpath = tree.getFoot();
        int numberOfNodesBetweenFootAndRoot = 0;
        while (onpath != tree.getRoot())
//        while (!onpath.equals(tree.getRoot()))
        {
            onpath = tree.getParent(onpath);
            numberOfNodesBetweenFootAndRoot++;
        }
        return numberOfNodesBetweenFootAndRoot;
    }

    private int getRightSibOfNode(StringTree tree, int node)
    {
        int rightsib = tree.getNodes().get(tree.getNodes().size() - 1) + 1;
        while (node != tree.getRoot())
//        while (!node.equals(tree.getRoot()))
        {
            boolean pastnode = false;
            ArrayList<Integer> siblings = tree.getChildren(tree.getParent(node));
            for (Integer sibl : siblings)
            {
                if (pastnode)
                {
                    rightsib = sibl;
//                    rightsib = Integer.parseInt(sibl);
                    //break;
                    return rightsib;
                }
                if (sibl.equals(node))
                {
                    pastnode = true;
                }
            }
            node = tree.getParent(node);
        }
        return rightsib;
    }

    public void verify(StringTree fullTree, int adjNodeNumber, TreeState targetTreeState)
    {//, boolean traceleft, boolean traceright){//, String lastPredictedCat){//, String clusterNumberLastPredictedSpineNode) {
        fixShadowRoots(fullTree);
        ArrayList<Integer> thisShadowRoots = analysis.getShadowSourceTreesRootList();
        //TreeState ts = new TreeState(analysis, (short) 0);
        byte index = -1;
        byte traceIndex = -1;
        byte secondtraceIndex = -1;


        for (int n : analysis.getNodes())
        {
            String cat = analysis.getCategory(n);
            if (TreeState.isNullLexeme(cat) && analysis.getLowerIndex(n) > 0)
            {
                if (traceIndex == -1)
                {
                    traceIndex = analysis.getLowerIndex(n);
                }
                else
                {
                    secondtraceIndex = analysis.getLowerIndex(n);
                }
            }
        }

        //	Node fn = ts.getFringe().getLastAdjNode();

        //find all indices before or at anchor that is to be verified.
/*		if (fn.getDownIndex()!=0 && TreeState.isNullLexeme(fn.getCategory())) traceIndex = fn.getDownIndex();
        for (Fringe f : ts.getUnaccessibles()){ 
        Node n = f.getLastAdjNode();
        // could also be that spine node has been adjunction site; exclude anchors that are traces.
        if (n.getDownIndex()!=0 && n.isShadow()){
        if (!TreeState.isNullLexeme(n.getCategory())){
        index = n.getDownIndex();
        break; 
        }
        else {
        if (traceIndex == -1) 
        traceIndex = n.getDownIndex();
        else
        secondtraceIndex = n.getDownIndex();
        }
        }
        }
         */ TreeState ts2 = new TreeState(analysis, (short) 0, targetTreeState.getWordCover(), opts.nBest);
        while (ts2.getFringe().getLastAdjNode().getDownIndex() == 0)
        {
            ts2.shiftNextUnaccessibleToAccessible();
            if (ts2.getFringe().isEmpty())
            {
                return;
            }
        }
        Node n = ts2.getFringe().getLastAdjNode();
        if (n.getDownIndex() != 0 && n.isShadow())
        {
            if (!TreeState.isNullLexeme(n.getCategory()))
            {
                index = n.getDownIndex();
            }
            else
            {
                //		traceIndex = n.getDownIndex();
                for (Fringe f : ts2.getUnaccessibles())
                {
                    n = f.getLastAdjNode();
                    if (!TreeState.isNullLexeme(n.getCategory()))
                    {
                        index = n.getDownIndex();
                        break;
                    }
                    //		else
                    //			secondtraceIndex = n.getDownIndex();
                }
            }
        }

        //	*/

        for (int counter = thisShadowRoots.size() - 1; counter >= 0; counter--)
        {//run backwards because of shiftNodeIndices which is invoked below!
            Integer shadowroot = thisShadowRoots.get(counter);
            if (analysis.getLowerIndex(shadowroot) == index
                    && analysis.getCategory(shadowroot).equals(fullTree.getCategory(fullTree.getRoot())))
            {
                integrationPointId = shadowroot;
                this.integrationPosition = "-1/-1";
                HashMap<Integer, Integer> mapping = getMapping(fullTree, fullTree.getRoot(), analysis, shadowroot, index, (byte) -2);
//                HashMap<Integer, Integer> mapping = getMapping(fullTree, fullTree.getRoot(), analysis, shadowroot.toString(), index, (byte) -2);
                HashMap<Integer, Integer> traceMapping = null;
                if (traceIndex > -1)
                {
                    traceMapping = getMapping(fullTree, fullTree.getRoot(), analysis, shadowroot, index, traceIndex);
//                    traceMapping = getMapping(fullTree, fullTree.getRoot(), analysis, shadowroot.toString(), index, traceIndex);
                }
                HashMap<Integer, Integer> traceMapping2 = null;
                if (secondtraceIndex > -1)
                {
                    traceMapping2 = getMapping(fullTree, fullTree.getRoot(), analysis, shadowroot, index, secondtraceIndex);
//                    traceMapping2 = getMapping(fullTree, fullTree.getRoot(), analysis, shadowroot.toString(), index, secondtraceIndex);
                }
                if (opts.verbose)
                {
                    Utils.logs(mapping.toString());
                }
                // replace indices in prefixtree for each of mapped nodes
                analysis.replaceIndeces(index, (byte) 0);
                if (traceIndex > -1)
                {
                    if (traceMapping != null && traceMapping.size() > mapping.size())
                    {
                        analysis.replaceIndeces(traceIndex, (byte) 0);

                    }
                    else
                    {
                        traceIndex = -2;
                    }
                    if (secondtraceIndex > -1)
                    {
                        if (traceMapping2 != null && traceMapping2.size() > mapping.size())
                        {
                            analysis.replaceIndeces(secondtraceIndex, (byte) 0);
                            mapping.putAll(traceMapping2);
                        }
                        else
                        {
                            secondtraceIndex = -2;
                        }
                    }
                    mapping.putAll(traceMapping);
                }
                // for each of the remaining nodes (that aren't in the shadow tree), shift all the nodes after them by 1, and add the node to the mapping.
                ArrayList<Integer> verificationTreeNodes = fullTree.getNodes();
                //HashMap<Integer, Integer> addnodes = new HashMap<Integer, Integer>();
                verificationTreeNodes.removeAll(mapping.values());
                /*
                // now add any nodes on the spine that were not predicted to the freqcounter to account for none adj 
                TreeState verificTreeTreeState = new TreeState(fullTree, (short)0);
                Fringe verificfringe =verificTreeTreeState.getFringe(); 
                while(verificfringe.getSubstNode()!=null || verificfringe.getLastAdjNode().getDownIndex()==-1){
                verificTreeTreeState.shiftNextUnaccessibleToAccessible();
                verificfringe =verificTreeTreeState.getFringe();
                }
                //tricky: this has to be a mix of old and new fringe. 
                for (Node vn : verificfringe.getAdjNodesOpenLeft()){
                for (Integer i : verificationTreeNodes){
                if (i == vn.getNodeId()){
                if (vn.getCategory().equals(lastPredictedCat)){
                vn.setClusterNumber(clusterNumberLastPredictedSpineNode);
                }
                else{
                vn.setClusterNumber("1/1");
                }
                this.fcnt.addNoneAdjs(vn, traceleft, traceright);
                break;
                }
                }
                }*/
                for (int newverificNode : verificationTreeNodes)
                {
                    Integer parent = fullTree.getParent(newverificNode);
                    int shadowparent = 0;
                    ArrayList<Integer> shadowNodeCands = new ArrayList<Integer>();
                    for (Integer key : mapping.keySet())
                    {
                        if (mapping.get(key) == parent)
//                        if (mapping.get(key) == Integer.parseInt(parent))
                        {
                            shadowNodeCands.add(key);
                        }
                        else if (traceMapping != null && traceMapping.containsKey(key) && traceMapping.get(key) == parent)
//                        else if (traceMapping != null && traceMapping.containsKey(key) && traceMapping.get(key) == Integer.parseInt(parent))
                        {
                            shadowNodeCands.add(key);
                        }
                    }
                    if (shadowNodeCands.size() == 1)
                    {
                        shadowparent = shadowNodeCands.get(0);
                    }
                    else if (shadowNodeCands.size() == 2)
                    {
                        shadowparent = Math.max(shadowNodeCands.get(0), shadowNodeCands.get(1));
                    }
                    //int shadowparent = mapping.get(Integer.parseInt(parent));
                    // decide on new index: it's shadowparent.rightsibling
                    int newIndex = getRightSibOfNode(analysis, shadowparent);
                    //mapping.put(newverificNode, newIndex);
                    mapping.put(newIndex, newverificNode);
                    analysis.shiftNodeIndices(newIndex, 1);
                    analysis.addNode(fullTree, newIndex, newverificNode);
                    analysis.putParent(newIndex, shadowparent );
                    ArrayList<Integer> children = new ArrayList<Integer>();
                    if (analysis.getChildren(shadowparent) != null)
                    {
                        children.addAll(analysis.getChildren(shadowparent));
                    }
                    children.add(newIndex);
                    analysis.putChildren(shadowparent, children);
                }
            }
        }
        fringe = targetTreeState.getFringe();
        //if (checkSynchronity(fringe, new TreeState(analysis,(short)0).getFringeAfterLastNonShadowNonNullLexAnchor())==null)//VERA LOOK HERE
        //	System.out.println(fringe);
        //fringe = new TreeState(analysis,0).getFringeAfterLastNonShadowLexAnchor();
        //fringe = cutOffForbidden(fringe);
    }

    public StringTree getStringTree()
    {
        return analysis;
    }

    private HashMap<Integer, Integer> getMapping(StringTree tree, int node, StringTree shadowtree, int shadowNode,
                                                 byte index, byte traceIndex)
    {
        ArrayList<Byte> p = new ArrayList<Byte>();
        p.add(index);
        if (traceIndex > -1)
        {
            p.add(traceIndex);
        }
        //check same category, indexes (as proxi for node type) and parent
        HashMap<Integer, Integer> coverednodes = new HashMap<Integer, Integer>();
//        int shadowNode = Integer.parseInt(shadownode);
        byte shadowNodeUpperInd = shadowtree.getUpperIndex(shadowNode);
        byte shadowNodeLowerInd = shadowtree.getLowerIndex(shadowNode);
        //ArrayList<String> nodeCs = ;
        if (tree.getCategory(node).equals(shadowtree.getCategory(shadowNode))
                && ((shadowNodeLowerInd == -1 || matches(p, shadowNodeLowerInd)
                || (shadowNodeUpperInd == -1 || matches(p, shadowNodeUpperInd)))))
        {
            if (tree.getNodeType(node) == TagNodeType.subst || tree.getNodeType(node) == TagNodeType.foot)
            {
                if (!matches(p, shadowNodeUpperInd))
                {
                    return null;
                }
            }
            else if (!tree.getChildren(node).isEmpty())
            {
                if (matches(p, shadowNodeLowerInd))
                {
                    int childno = 0;
                    boolean headchildseen = false;
                    ArrayList<Integer> shadowCs = shadowtree.getChildren(shadowNode);
                    for (Integer child : tree.getChildren(node))
                    {
                        if (tree.getNodeType(child) == TagNodeType.foot && headchildseen)
                        {//!tree.hasFootLeft()){
                            HashMap<Integer, Integer> dr =
                                    getMapping(tree, child, shadowtree, shadowCs.get(childno), index, traceIndex);
                            coverednodes.putAll(dr);
                        }
                        if (headchildseen)
                        {
                            break;
                        }
                        // if the child structure doesn't match it can't work.
                        // however, if this is the spine, the shadow tree can end earlier.
                        if (shadowCs == null || shadowCs.size() <= childno)
                        {
                            if (tree.isHeadChild(child))
                            {
                                break;
                            }
                            return null;
                        }
                        HashMap<Integer, Integer> dr =
                                getMapping(tree, child, shadowtree, shadowCs.get(childno), index, traceIndex);
                        if (dr == null)
                        {
                            //return null;
                        }
                        else
                        {
                            coverednodes.putAll(dr);
                        }
                        childno++;
                        if (tree.isHeadChild(child))
                        {// && tree.getNodeType(child)!=TagNodeType.foot){
                            headchildseen = true;
                        }
                    }
                }
                else
                {// assume that something was adjoined here.
                    for (Integer shnode : shadowtree.getNodes())
                    {
                        if (//shadowtree.getUpperIndex(shnode)!= -1 &&shadowtree.getLowerIndex(shnode)!= -1 &&  
                                shadowtree.getUpperIndex(shnode) == shadowtree.getLowerIndex(shadowNode)
                                && matches(p, shadowtree.getLowerIndex(shnode)) && shnode > shadowNode)
                        {
                            HashMap<Integer, Integer> dr = getMapping(tree, node, shadowtree, shnode, index, traceIndex);
                            if (dr == null)
                            {
                                return null;
                            }
                            coverednodes.putAll(dr);
                            break;
                        }
                    }
                }
            }
            //if (!coverednodes.containsKey(Integer.parseInt(node))) coverednodes.put(Integer.parseInt(node), Integer.parseInt(shadownode));
            if (!coverednodes.containsKey(node))
//            if (!coverednodes.containsKey(Integer.parseInt(node)))
            {
                coverednodes.put(shadowNode, node);
//                coverednodes.put(Integer.parseInt(shadowNode), Integer.parseInt(node));
            }
            return coverednodes;
        }
        else
        {
            return null;
        }
    }

    public static boolean matches(ArrayList<Byte> p, byte b)
    {
        for (byte pat : p)
        {
            if (pat == b)
            {
                return true;
            }
        }
        return false;
//        return p.contains(b);
    }

    public Fringe getFringe()
    {
        return fringe;
    }

    public String getTrace()
    {
        return trace;
    }

    public FreqCounter getFreqCounter()
    {
        return fcnt;
    }

    public void addToBuiltTrace(String info)
    {
        this.builtTrace.append(info);
    }

    public String getBuiltTrace()
    {
        return this.builtTrace.toString();
    }

    public void setDependencies(Dependencies dependencies)
    {
        this.dependencies = dependencies;
    }

    public Dependencies getDependencies()
    {
        return dependencies;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double score)
    {
        this.score = score;
    }
    
    @Override
    public int compareTo(StringTreeAnalysis o)
    {
        if(this.score != Double.MIN_VALUE)
            return (int)o.score - (int)this.score;
        else
            return (int)o.getProbability() - (int)this.getProbability();
    }
    
    
}
/*
private Fringe cutOffForbidden(Fringe fringe) {
String index = fringe.getAdjNodes().get(fringe.getAdjNodes().size()-1).getDownIndex();
String index2 = fringe.getAdjNodes().get(fringe.getAdjNodes().size()-1).getUpIndex();
if (fringe.getSubstNode()==null && (index == null || index.equals("x") || index2 == null || !index2.equals(index))){
}
else if (fringe.getSubstNode()!=null && !fringe.getSubstNode().getUpIndex().equals("x")){
boolean remove = false;
for (int j = fringe.getAdjNodes().size()-1; j >= 0; j--){
Node n = fringe.getAdjNodes().get(j);
if ( remove){
fringe.getAdjNodes().remove(0);
}
if (((n.getUpIndex()==null || !n.getUpIndex().equals(index)) && !n.isOpenToRight())||
(!n.getDownIndex().equals(index) && n.isOpenToRight())){
remove = true;
}
}			
}
///*
else{
boolean remove = false;
for (int j = fringe.getAdjNodes().size()-1; j >= 0; j--){
Node n = fringe.getAdjNodes().get(j);
if ( remove){
fringe.getAdjNodes().remove(0);
}
if (!integrationPointId.equals(""))
if ((!n.getDownIndex().equals(index) && n.isOpenToRight())||
(!n.getUpIndex().equals(index) && !n.isOpenToRight()&& 
((!integrationPointId.equals("")&&
(analysis.getUpperIndex(Integer.parseInt(this.integrationPointId))==null||analysis.getUpperIndex(Integer.parseInt(this.integrationPointId)).equals("x")))||
!n.getUpIndex().equals("x")))){
remove = true;
}
}
}
return fringe;
}
 */

/*
private StringTree getTreeWithIndex(ArrayList<StringTree> shadowTrees, String lowerIndex) {
for (StringTree shtree : shadowTrees){
for (Integer shroot : shtree.getShadowSourceTreesRootList()){
if (shtree.getLowerIndex(shroot).equals(lowerIndex)){
return shtree;
}
}

}
return null;
}*/
