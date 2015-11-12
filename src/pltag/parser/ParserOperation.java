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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import pltag.corpus.ElementaryStringTree;
import pltag.corpus.StringTree;
import pltag.parser.semantics.DepTreeState;

public enum ParserOperation
{

    substituteDownXF
    {

        @Override
        public List<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState prefOrig, TreeState elemOrig, ElementaryStringTree tree, int timestamp)
        {
            Node prefTreeSubstNode = prefOrig.getFringe().getSubstNode();
            List<ChartEntry> states = new ArrayList<ChartEntry>();
            if (prefTreeSubstNode != null && prefTreeSubstNode.getCategory().equals(elemOrig.getRootNode().getCategory())
                    && elemOrig.hasNonShadowLeaf()
                    && //		!TreeState.isNullLexeme(elemOrig.getFringe().getLastAdjNode().getCategory())&& //TODO condition needed??? only for FF?
                    elemOrig.hasNoSubstBeforeLexAnchor())
            {
                TreeState elem = elemOrig.copy();
                short highestIdInPrefTree = makeUniqueIndices(elem, prefOrig);
                short[] wc =
                {
                    prefOrig.getWordCover()[0], elem.getWordCover()[1]
                };
                // go past lex anchor for elemtree; substnode must actually be null! go past subst node in pref tree
                Node elemRootNode = elem.getRootNode();
                elemRootNode.setUpIndex(prefTreeSubstNode.getUpIndex());
                Fringe newFringe = elem.getFringeAfterLastNonShadowNonNullLexAnchor();
                FreqCounter noneAdjFC = new FreqCounter(opts);
                Fringe histf = elem.getFringe();
                while (histf != newFringe)
                {
                    noneAdjFC.accountForHistFringe(histf, null, false);//account for none adjup operations between root and lexical anchor. (or traces which precede the lexical anchor)
                    elem.shiftNextUnaccessibleToAccessible();
                    histf = elem.getFringe();
                }
                //newElemUa can be empty
                ArrayList<Fringe> newUnaccessibles = (ArrayList<Fringe>) elem.getUaAfter(newFringe);//.getNextUa();
                ArrayList<Fringe> prefFringes = new ArrayList<Fringe>();
                TreeState pref = prefOrig.copy();
                pref.getFringe().getSubstNode().setDownPosTag(elemRootNode.getLambda());
                noneAdjFC.accountForHistFringe(pref.getFringe(), null, true);// account for none adjdown operations between last anchor and subst node
                pref.fixKernels(elemRootNode);
                if (newUnaccessibles.isEmpty())
                {
                    pref.shiftNextUnaccessibleToAccessible();
                    Fringe prefFringe = pref.getFringe();
                    //Fringe prefFringe = pref.getNextFringe();
                    fJoin(newFringe, prefFringe);
                    newUnaccessibles.addAll(pref.getUaAfter(prefFringe));
                }
                else
                {
                    prefFringes.addAll(pref.getUnaccessibles());
                }
                FringeAndProb newnext;//= prefOrig.getFutureFringe();
                if (prefFringes.isEmpty())
                {
                    newnext = prefOrig.getFutureFringe();
                }
                else
                {
                    ArrayList<FringeAndProb> newlist = prependPrefFringes(prefFringes, prefOrig.getFutureFringe());
                    newnext = new FringeAndProb(null, prefOrig.getFutureFringe().getnBestProbs(), newlist, prefOrig.getFutureFringe().getBBHist());
                }
                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(prefOrig.shadowListDeepCopy());
                for (ShadowStringTree st : elem.getShadowTrees())
                {
                    st.setIntegLambda(prefTreeSubstNode.getLambda());
                    st.setIntegLambdaId(prefTreeSubstNode.getLambdaTimestamp());
                }
                newShadowTrees.addAll(elem.getShadowTrees());
                TreeState ts = !opts.useSemantics ? 
                        new TreeState(prefOrig.isAux(), prefOrig.hasFootLeft(), wc, newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest) : 
                        new DepTreeState(prefOrig.isAux(), prefOrig.hasFootLeft(), wc, newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest, 
                                ((DepTreeState)prefOrig).getDependencies(), parserModel); 
                ts.addLeftMostAnnot(this.getPos(words, prefOrig) + "+" + newFringe.getAdjNodesOpenRight().get(0).getCategory());
                if(opts.useSemantics) // subDownXF
                {
//                    ((DepTreeState)ts).updateDependencies(prefTreeSubstNode, tree, prefTreeSubstNode.getNodeId(), highestIdInPrefTree, true, false);
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), prefTreeSubstNode, tree, (short)tree.getRoot(), highestIdInPrefTree, true, false, words, origPosTags, "S", false, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    BuildBlock buildBlock = new BuildBlock(opts, this, -1, prefTreeSubstNode, -1, tree);
                    buildBlock.setFreqCounter(noneAdjFC);
                    buildBlock.setWordProbAndTreeProb(parserModel, treeState.getFringe(), prefTreeSubstNode.getLambda());
                    // set history.
                    treeState.getFutureFringe().addCutOffLocation(buildBlock, wc[1]);
                    if (!prefFringes.isEmpty())
                    {
                        for (FringeAndProb f : treeState.getFutureFringe().getNext())
                        {
                            f.addCutOffLocation(buildBlock, wc[1]);
                        }
                    }
                    states.add(new ChartEntry(opts, treeState, buildBlock));
                    noneAdjFC.accountForHistFringe(treeState.getFringe(), null, true);//account for none adjdown operations between anchor and further traces 
                }
            }
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTree, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTreeAnalysis newTree = prefTree.clone();
            StringTree elemTreeCopy = elemTree.copy();
            newTree.substitute(elemTreeCopy, targetTreeState);

            return newTree;
        }
    },
    substituteDownXS
    {
        @Override
        public List<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState prefOrig, TreeState elemOrig, ElementaryStringTree tree, int timestamp)
        {
            Node prefTreeSubstNode = prefOrig.getFringe().getSubstNode();
            List<ChartEntry> states = new ArrayList<ChartEntry>();
            if (elemOrig.getFringe().hasTraceLeft())
            {
                return states;
            }
            if (prefTreeSubstNode != null && prefTreeSubstNode.getCategory().equals(elemOrig.getRootNode().getCategory())
                    && //					!prefTreeSubstNode.isShadow()&& 
                    !elemOrig.hasNonShadowLeaf())
            {
                TreeState elem = elemOrig.copy();
                short highestIdInPrefTree = makeUniqueIndices(elem, prefOrig);
                Node elemrootnode = elem.getRootNode();
                //elem.getRootNode().setLeftMostCover(prefTreeSubstNode.getLeftMostCover());
                elemrootnode.setUpIndex(prefTreeSubstNode.getUpIndex());
                Fringe newFringe = elem.getFringesBeforeFirstNullLexeme().get(0);
                ArrayList<Fringe> newUnaccessibles = (ArrayList<Fringe>) elem.getUaAfter(newFringe);
                ArrayList<Fringe> prefFringes = new ArrayList<Fringe>();
                //BuildBlock buildBlock = new BuildBlock(this, -1, prefTreeSubstNode, -1);
                TreeState pref = prefOrig.copy();
                pref.getFringe().getSubstNode().setDownPosTag(elemrootnode.getLambda());
                FreqCounter fc = new FreqCounter(opts);
                fc.accountForHistFringe(pref.getFringe(), null, true);// account for none adjdown between last anchor and subst site on pref tree.
                //buildBlock.setFreqCounter(fc);
                pref.fixKernels(elemrootnode);
                if (newUnaccessibles.isEmpty())
                {
                    Fringe prefFringe = pref.getUnaccessibles().remove(0);
                    fJoin(newFringe, prefFringe);
                    newUnaccessibles = pref.getUnaccessibles();
                }
                else
                {
                    prefFringes.addAll(pref.getUnaccessibles());
                }
                FringeAndProb newnext;
                if (prefFringes.isEmpty())
                {
                    newnext = prefOrig.getFutureFringe();
                }
                else
                {
                    ArrayList<FringeAndProb> newlist = prependPrefFringes(prefFringes, prefOrig.getFutureFringe());
                    newnext = new FringeAndProb(null, prefOrig.getFutureFringe().getnBestProbs(), newlist, prefOrig.getFutureFringe().getBBHist());
                    //for (FringeAndProb f : newnext.getNext()){
                    //	f.addCutOffLocation(buildBlock, prefOrig.getWordCover()[1]);
                    //}
                }
                //newnext.addCutOffLocation(buildBlock, prefOrig.getWordCover()[1]);	
                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(prefOrig.shadowListDeepCopy());
                for (ShadowStringTree st : elem.getShadowTrees())
                {
                    st.setIntegLambda(prefTreeSubstNode.getLambda());
                    st.setIntegLambdaId(prefTreeSubstNode.getLambdaTimestamp());
                }
                newShadowTrees.addAll(elem.getShadowTrees());
                TreeState ts = !opts.useSemantics ? 
                        new TreeState(prefOrig.isAux(), prefOrig.hasFootLeft(), prefOrig.getWordCover().clone(),
                        newFringe, newUnaccessibles, newnext, newShadowTrees, prefOrig.hasNonShadowLeaf(), opts.nBest) : 
                        new DepTreeState(prefOrig.isAux(), prefOrig.hasFootLeft(), prefOrig.getWordCover().clone(),
                        newFringe, newUnaccessibles, newnext, newShadowTrees, prefOrig.hasNonShadowLeaf(), opts.nBest, 
                                ((DepTreeState)prefOrig).getDependencies(), parserModel);
                if(opts.useSemantics) // subDownXS
                {
                    setPrefixOffsetOfShadowTrees(newShadowTrees, highestIdInPrefTree, prefTreeSubstNode.getNodeId(), elemOrig.getRootNode().getNodeId());
//                    ((DepTreeState)ts).updateDependencies(prefTreeSubstNode, tree, prefTreeSubstNode.getNodeId(), highestIdInPrefTree, true, true);
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), prefTreeSubstNode, tree, (short)tree.getRoot(), highestIdInPrefTree, true, true, words, origPosTags, "S", false, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    BuildBlock buildBlock = new BuildBlock(opts, this, -1, prefTreeSubstNode, -1, tree);
                    buildBlock.setFreqCounter(fc);
                    buildBlock.setWordProbAndTreeProb(parserModel, treeState.getFringe(), prefTreeSubstNode.getLambda());
                    // set history.
                    treeState.getFutureFringe().addCutOffLocation(buildBlock, prefOrig.getWordCover()[1]);
                    if (!prefFringes.isEmpty())
                    {
                        for (FringeAndProb f : treeState.getFutureFringe().getNext())
                        {
                            f.addCutOffLocation(buildBlock, prefOrig.getWordCover()[1]);
                        }
                    }
                    states.add(new ChartEntry(opts, treeState, buildBlock));
                    fc.accountForHistFringe(treeState.getFringe(), null, true);//account for none adjdown operations between anchor and further traces 
                }
                //states.add(new ChartEntry(ts, buildBlock));
            }
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTree, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTreeAnalysis newTree = prefTree.clone();
            StringTree elemTreeCopy = elemTree.copy();
            newTree.substitute(elemTreeCopy, targetTreeState);
            return newTree;
        }
    },
    substituteUpFF
    {
        @Override
        public List<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemOrig, ElementaryStringTree tree, int timestamp)
        {
            List<ChartEntry> states = new ArrayList<ChartEntry>();
            if (!preforig.getUnaccessibles().isEmpty())
            {
                return states;
            }
            FreqCounter fc = new FreqCounter(opts);
            Fringe fringe = elemOrig.getFringe();
            TreeState elemo = elemOrig;
            boolean copied = false;
            int counter = 0; // safety mechanism to avoid infinite loops
            while (fringe.hasTraceRight() && counter < 5)
            {
                fc.accountForHistFringe(fringe, null, false);//account for none adjup on path from root to trace.
                elemo = elemOrig.copy();
                if (copied)
                {
                    elemo.shiftNextUnaccessibleToAccessible();// for second trace.                    
                }
                elemo.shiftNextUnaccessibleToAccessible();
                if(copied && elemo.getFringe().hasTraceRight()) // for third trace (rare)
                    elemo.getFringe().hasTraceRight();
                copied = true;
                fringe = elemo.getFringe();
                counter++;
            }
            Node elemTreeSubstNode = elemo.getFringe().getSubstNode();
            Node prefTreeRootNode = preforig.getRootNode();
            if (prefTreeRootNode == null)
            {
                return states;
            }
            if (elemTreeSubstNode != null && elemTreeSubstNode.getCategory().equals(prefTreeRootNode.getCategory())
                    && !elemTreeSubstNode.isShadow() && !prefTreeRootNode.isShadow() && elemo.getUnaccessibles().get(0).getSubstNode() == null
                    && preforig.getFringe().getSubstNode() == null)
            {
                TreeState elem;
                if (copied)
                {
                    elem = elemo;
                }
                else
                {
                    elem = elemOrig.copy();
                }
                TreeState pref = preforig;//.copy();                
                short highestIdInPrefTree = makeUniqueIndices(elem, pref);
                short[] wc =
                {
                    pref.getWordCover()[0], elem.getWordCover()[1]
                };
                elem.getFringe().getSubstNode().setDownIndex(prefTreeRootNode.getDownIndex());
                elem.getFringe().getSubstNode().setDownPosTag(prefTreeRootNode.getDownPosTag());
                fc.accountForHistFringe(elem.getFringe(), null, false);//account for none adjup operations on path from element root to subst node on elementary tree.
                fc.accountForHistFringe(pref.getFringe(), null, true);//account for none adjdown operations on path from anchor to root on pref tree.
                elem.shiftNextUnaccessibleToAccessible();
                elem.getFringe().getAdjNodesOpenRight().get(0).setDownPosTag(prefTreeRootNode.getDownPosTag());
                elem.addLeftMostAnnot(preforig.getFringe().getLastAdjNode().getLeftMostCover());
                Fringe newFringe = elem.getFringeAfterLastNonShadowNonNullLexAnchor();
                while (elem.getFringe() != newFringe)
                {
                    fc.accountForHistFringe(elem.getFringe(), null, false);//account for noneadj operations on path from subst node to lexical anchor on elementary tree.
                    elem.shiftNextUnaccessibleToAccessible();
                }
                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(pref.shadowListDeepCopy());
                for (ShadowStringTree st : elem.getShadowTrees())
                {
                    st.setIntegLambda(prefTreeRootNode.getLambda());
                    st.setIntegLambdaId(prefTreeRootNode.getLambdaTimestamp());
                }
                newShadowTrees.addAll(elem.getShadowTrees());

                TreeState ts = !opts.useSemantics ? 
                        new TreeState(elem.isAux(), elem.hasFootLeft(), wc,
                        newFringe, elem.getUaAfter(newFringe), pref.getFutureFringe(), newShadowTrees, true, opts.nBest) :
                        new DepTreeState(elem.isAux(), elem.hasFootLeft(), wc,
                        newFringe, elem.getUaAfter(newFringe), pref.getFutureFringe(), newShadowTrees, true, opts.nBest, 
                                ((DepTreeState)pref).getDependencies(), parserModel);
                ts.addLeftMostAnnot(getPos(words, preforig) + "+" + newFringe.getAdjNodesOpenRight().get(0).getCategory());
                if(opts.useSemantics) // subUpFF
                {
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), prefTreeRootNode, tree, elemTreeSubstNode.getNodeId(), highestIdInPrefTree, false, false, words, origPosTags, "S", true, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    Node ipiNode = prefTreeRootNode.copy();
                    ipiNode.setPosInTree(elemTreeSubstNode.getPosInTree());
                    BuildBlock bb = new BuildBlock(opts, this, -1, ipiNode, -1, tree);
                    bb.setFreqCounter(fc);
                    bb.setWordProbAndTreeProb(parserModel, treeState.getFringe(), ipiNode.getLambda());
                    ts.getFutureFringe().addCutOffLocation(bb, wc[1]);
                    states.add(new ChartEntry(opts, treeState, bb));
                    fc.accountForHistFringe(treeState.getFringe(), null, true);//account for none adjdown operations from newtree anchor to traces. 
                }
            }
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTreeOrig, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTree prefTree = prefTreeOrig.getStringTree().copy();
            StringTree elemTreeCopy = elemTree.copy();
            TreeState elemTreeState = new TreeState(elemTreeCopy, (short) 0, opts.nBest);
            if (elemTreeState.getFringe().getSubstNode() == null && elemTreeState.getFringe().hasTraceRight())
            {
                elemTreeState.shiftNextUnaccessibleToAccessible();
            }
            // changed second "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
//            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, elemTreeState.getFringe(), "", Integer.MIN_VALUE, new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
//            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, elemTreeState.getFringe(), "", "", new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            // Fixed bug (09-01-2014)
            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy,elemTreeState.getFringe(), "", Integer.MIN_VALUE, prefTreeOrig.getFreqCounter(), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            newTree.substitute(prefTree, targetTreeState);
            return newTree;
        }
    },
    substituteUpFS
    {
        @Override
        public ArrayList<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemOrig, ElementaryStringTree tree, int timestamp)
        {
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            if (!preforig.getUnaccessibles().isEmpty())
            {
                return states;
            }
            FreqCounter fc = new FreqCounter(opts);
            Fringe fringe = elemOrig.getFringe();
            TreeState elemo = elemOrig;
            boolean copied = false;
            int counter = 0;
            while (fringe.hasTraceRight() && counter < 5)
            {
                fc.accountForHistFringe(fringe, null, false);//account for none adjup on path from root to trace.
                elemo = elemOrig.copy();
                if (copied)
                {
                    elemo.shiftNextUnaccessibleToAccessible();// for second trace.
                }
                if(copied && elemo.getFringe().hasTraceRight()) // for third trace (rare)
                    elemo.getFringe().hasTraceRight();
                copied = true;
                elemo.shiftNextUnaccessibleToAccessible();
                fringe = elemo.getFringe();
                counter++;
            }
            Node elemTreeSubstNode = elemo.getFringe().getSubstNode();
            Node prefTreeRootNode = preforig.getRootNode();
            if (prefTreeRootNode == null)
            {
                return states;
            }
            if (elemTreeSubstNode != null && elemTreeSubstNode.getCategory().equals(prefTreeRootNode.getCategory())
                    && !elemo.hasNonShadowLeaf() && !prefTreeRootNode.isShadow() && preforig.getFringe().getSubstNode() == null)
            {
                TreeState elem;
                if (copied)
                {
                    elem = elemo;
                }
                else
                {
                    elem = elemOrig.copy();
                }
                TreeState pref = preforig;
                short highestIdInPrefTree = makeUniqueIndices(elem, pref);
                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(pref.shadowListDeepCopy());
                for (ShadowStringTree st : elem.getShadowTrees())
                {
                    st.setIntegLambda(prefTreeRootNode.getLambda());
                    st.setIntegLambdaId(prefTreeRootNode.getLambdaTimestamp());
                }
                newShadowTrees.addAll(elem.getShadowTrees());
                //elem.getLastFringe().copyProbs(pref.getLastFringe());
                Node elemsubstNode = elem.getFringe().getSubstNode();
                elemsubstNode.setDownIndex(prefTreeRootNode.getDownIndex());
                elemsubstNode.setDownPosTag(prefTreeRootNode.getDownPosTag());
                fc.accountForHistFringe(elem.getFringe(), null, false);//account for none adjup operations on path from element root/trace to subst node on elementary tree.
                fc.accountForHistFringe(pref.getFringe(), null, true);//account for none adjdown operations on path from anchor to root on pref tree.
                elem.shiftNextUnaccessibleToAccessible();
                elem.getFringe().getAdjNodesOpenRight().get(0).setDownPosTag(prefTreeRootNode.getDownPosTag());
                elem.addLeftMostAnnot(preforig.getFringe().getLastAdjNode().getLeftMostCover());
                TreeState ts = !opts.useSemantics ? 
                        new TreeState(elem.isAux(), elem.hasFootLeft(), pref.getWordCover().clone(),
                        elem.getFringe(), elem.getUnaccessibles(), pref.getFutureFringe(), newShadowTrees, true, opts.nBest) : //, preforig.getContList());
                        new DepTreeState(elem.isAux(), elem.hasFootLeft(), pref.getWordCover().clone(),
                        elem.getFringe(), elem.getUnaccessibles(), pref.getFutureFringe(), newShadowTrees, true, opts.nBest, 
                                ((DepTreeState)pref).getDependencies(), parserModel);
                if(opts.useSemantics) // subUpFS
                {
                    setPrefixOffsetOfShadowTrees(newShadowTrees, highestIdInPrefTree, prefTreeRootNode.getNodeId(), elemTreeSubstNode.getNodeId());
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), prefTreeRootNode, tree, elemTreeSubstNode.getNodeId(), highestIdInPrefTree, false, true, words, origPosTags, "S", true, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    Node ipiNode = prefTreeRootNode.copy();
                    ipiNode.setPosInTree(elemTreeSubstNode.getPosInTree());
                    BuildBlock bb = new BuildBlock(opts, this, -1, ipiNode, -1, tree);
                    bb.setFreqCounter(fc);
                    bb.setWordProbAndTreeProb(parserModel, treeState.getFringe(), ipiNode.getLambda());
                    treeState.getFutureFringe().addCutOffLocation(bb, pref.getWordCover()[1]);
                    states.add(new ChartEntry(opts, treeState, bb));
                    fc.accountForHistFringe(treeState.getFringe(), null, true);//account for histfringe of shifting over traces.
                }
            }
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTreeOrig, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTree elemTreeCopy = elemTree.copy();
            StringTree prefTree = prefTreeOrig.getStringTree().copy();
            TreeState elemTreeState = new TreeState(elemTreeCopy, (short) 0, opts.nBest);
            if (elemTreeState.getFringe().getSubstNode() == null && elemTreeState.getFringe().hasTraceRight())
            {
                elemTreeState.shiftNextUnaccessibleToAccessible();
            }
            // changed second "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
            //StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, elemTreeState.getFringe(), "", Integer.MIN_VALUE, new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
//            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, elemTreeState.getFringe(), "", "", new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            // Fixed bug (09-01-2014)
            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy,elemTreeState.getFringe(), "", Integer.MIN_VALUE, prefTreeOrig.getFreqCounter(), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            newTree.substitute(prefTree, targetTreeState);
            return newTree;
        }
    },
    adjoinDownXF
    {
        @Override
        public ArrayList<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemOrig, ElementaryStringTree tree, int timestamp)
        {
            HashMap<Node, String> ipiNodeClusterNo = new HashMap<Node, String>();
            ArrayList<Node> possibleAdjNodes = elemOrig.findAdjoinable(preforig.getFringe(), null, ipiNodeClusterNo);
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            if (!elemOrig.hasNonShadowLeaf() || !elemOrig.hasNoSubstBeforeLexAnchor() || possibleAdjNodes.isEmpty())
            {
                return states;
            }
            TreeState elem = elemOrig.copy();
            short highestIdInPrefTree = makeUniqueIndices(elem, preforig);
            ArrayList<Node> elemfootToRootPath = null;
            FreqCounter fc = new FreqCounter(opts);
            if (elem.hasFootLeft())
            {
                fc.accountForHistFringe(elem.getFringe(), null, false);//account for none-adjup events on path from root to foot of elem tree.
                elem.shiftNextUnaccessibleToAccessible();
            }
            else
            {//then can delete last fringe bit from elem
                // there can't be any open left fringe bits on path from right foot to root
                elemfootToRootPath = elem.removeRightFootFringe().getAdjNodesOpenRight();
            }
            int adjnodenum = -1;
            TreeState elemcopy = null;
            if (possibleAdjNodes.size() > 1)
            {
                elemcopy = elem.copy();
            }
            for (Node adjNode : possibleAdjNodes)
            {
                FreqCounter fc2 = new FreqCounter(fc);//fc.clone();
                String clusterNo = ipiNodeClusterNo.get(adjNode);
                TreeState pref = preforig.copy();
                int adjNodePos = preforig.getFringe().getAdjoinableNodes().indexOf(adjNode);
                Fringe prefFringe = pref.getFringe();
                Node adjNodeNew = prefFringe.getAdjoinableNodes().get(adjNodePos);
                if (adjnodenum > -1)
                {
                    if (adjnodenum == possibleAdjNodes.size() - 2)
                    {
                        elem = elemcopy;
                    }
                    else
                    {
                        elem = elemcopy.copy();
                    }
                }
                adjnodenum++;
                short[] wc =
                {
                    pref.getWordCover()[0], elem.getWordCover()[1]
                };
                //go past lex anchor for elemtree; substnode must actually be null!
                // go past subst node in pref tree
                Fringe newFringe = elem.getFringeAfterLastNonShadowNonNullLexAnchor();
                //newElemUa can be empty
                ArrayList<Fringe> newUnaccessibles = (ArrayList<Fringe>) elem.getUaAfter(newFringe);//getNextUa();//.clone();
                ArrayList<Fringe> prefFringes = new ArrayList<Fringe>();

                Fringe newPrefFringe = new Fringe(
                        prefFringe.getRestOfAdjNodesOpenRight(adjNodeNew, elem.hasFootLeft()),
                        prefFringe.getRestOfAdjNodesOpenLeft(adjNodeNew, elem.hasFootLeft()),
                        prefFringe.getSubstNode(), -2);
                //go past lex anchor for elemtree; substnode must actually be null! go past subst node in pref tree
                if (!elem.hasFootLeft())
                {
                    Node elemRootNode = elem.getRootNode();
                    pref.fixFringeAdj(adjNodeNew, elemfootToRootPath, wc[1]);
                    if (elemRootNode != null)
                    {
                        elemRootNode.setUpIndex(adjNode.getUpIndex());
                    }
                }
                else
                {
                    adjNode = elem.fixFringeAdjLeft(adjNodeNew, pref.getFringe(), adjNode);
                }
                Fringe histfringe = elem.getFringe();
                while (histfringe != newFringe)
                {
                    fc2.accountForHistFringe(histfringe, null, false);// account for none adj operation to hist fringes between root or foot and anchor of elemtree
                    elem.shiftNextUnaccessibleToAccessible();
                    histfringe = elem.getFringe();
                }

                if (newPrefFringe.isEmpty())
                {
                    newPrefFringe = null;
                }
                if (newUnaccessibles.isEmpty())
                {
                    fJoin(newFringe, newPrefFringe);
                    newUnaccessibles = pref.getUnaccessibles();
                }
                else
                {
                    prefFringes.add(newPrefFringe);
                    prefFringes.addAll(pref.getUnaccessibles());
                }
                FringeAndProb newnext;
                if (prefFringes.isEmpty())
                {
                    newnext = pref.getFutureFringe();
                }
                else
                {
                    ArrayList<FringeAndProb> newlist = prependPrefFringes(prefFringes, pref.getFutureFringe());
                    newnext = new FringeAndProb(null, pref.getFutureFringe().getnBestProbs(), newlist, pref.getFutureFringe().getBBHist());
                }
                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(pref.getShadowTrees());
                for (ShadowStringTree st : elem.getShadowTrees())
                {
                    st.setIntegLambda(adjNode.getDownPosTag());
                    st.setIntegLambdaId(adjNode.getLambdaTimestamp()); // TODO: CHECK!
                }
                newShadowTrees.addAll(elem.getShadowTrees());
                fc2.accountForHistFringe(prefFringe, adjNodeNew, true);// account for none adjdown on nodes between anchor and adj site on pref fringe.
                TreeState ts = !opts.useSemantics ? 
                        new TreeState(preforig.isAux(), preforig.hasFootLeft(), wc, newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest) :
                        new DepTreeState(preforig.isAux(), preforig.hasFootLeft(), wc, newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest, 
                                ((DepTreeState)preforig).getDependencies(), parserModel);
                ts.addLeftMostAnnot(getPos(words, preforig) + "+" + newFringe.getAdjNodesOpenRight().get(0).getCategory());//TODO muss schon vorher stattfinden, da einige Fringes schon versteckt in future fringes. next.
                if(opts.useSemantics) // adjDownXF
                {                    
//                    ((DepTreeState)ts).updateDependencies(adjNodeNew, tree, elem.hasFootLeft() ? elem.getFringe().getLastAdjNode() : elemfootToRootPath.get(0), highestIdInPrefTree, false, false);
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), adjNodeNew, tree, (short)tree.getFoot(), highestIdInPrefTree, false, false, words, origPosTags, "A", false, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    BuildBlock buildBlock = new BuildBlock(opts, this, adjnodenum, adjNode, possibleAdjNodes.size(), clusterNo, tree);
                    String integLambda = adjNode.getDownPosTag();
                    if (opts.countNoneAdj)
                    {
                        fc2.addSingleTreeProbEl(new TreeProbElement(ParserOperation.adjoinDownXS.name(), adjNode.getCategory(), "NONEADJ",
                                treeState.getFringe().hasTraceLeft(), treeState.getFringe().hasTraceRight(), adjNode.getOrigTree(), adjNode.getPosInTree(),
                                adjNode.getLambda(), adjNode.getLeftMostCover(), adjNode.getClusterNumber()));
                    }
                    buildBlock.setFreqCounter(fc2);
                    buildBlock.setWordProbAndTreeProb(parserModel, treeState.getFringe(), integLambda);
                    treeState.getFutureFringe().addCutOffLocation(buildBlock, wc[1]);
                    if (!prefFringes.isEmpty())
                    {
                        for (FringeAndProb f : treeState.getFutureFringe().getNext())
                        {
                            f.addCutOffLocation(buildBlock, wc[1]);
                        }
                    }
                    fc2.accountForHistFringe(treeState.getFringe(), null, true);//account for none adjdown nodes between anchor and further traces on elem fringe.
                    states.add(new ChartEntry(opts, treeState, buildBlock));
                }
            } // for possibleAdjNodes
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTree, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTreeAnalysis newTree = prefTree.clone();
            StringTree elemTreeCopy = elemTree.copy();
            boolean successful = newTree.adjoin(elemTreeCopy, adjNodeNumber, targetTreeState, null);
            if (!successful && TreeState.isNullLexeme(newTree.getFringe().getLastAdjNode().getCategory()))
            {
                //can't easily access next fringe from prefTree state. Therefore need to calculate it from "scratch" from StringTree.
                StringTree copiedTree = prefTree.getStringTree().copy();
                TreeState ts = new TreeState(copiedTree, (short) 0, opts.nBest);
                Fringe f = ts.getFringeAfterLastNonShadowNonNullLexAnchor();
                if(opts.verbose)
                    LogInfo.error("\n### equal?: " + f + prefTree.getFringe());
                ArrayList<Fringe> flist = ts.getUaAfter(f);
                if (flist.isEmpty())
                {
                    return null;
                }
                f = flist.get(0);
                // changed "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
                newTree = new StringTreeAnalysis(opts, copiedTree, f, prefTree.getTrace(), Integer.MIN_VALUE, prefTree.getFreqCounter(), new StringBuilder(prefTree.getBuiltTrace()));
//                newTree = new StringTreeAnalysis(opts, copiedTree, f, prefTree.getTrace(), "", prefTree.getFreqCounter(), new StringBuilder(prefTree.getBuiltTrace()));
                newTree.adjoin(elemTreeCopy, adjNodeNumber, targetTreeState, null);
            }
            return newTree;
        }
    },
    adjoinDownXS
    {
        @Override
        public ArrayList<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemOrig, ElementaryStringTree tree, int timestamp)
        {
            HashMap<Node, String> ipiNodeClusterNo = new HashMap<Node, String>();
            ArrayList<Node> possibleAdjNodes = elemOrig.findAdjoinable(preforig.getFringe(), null, ipiNodeClusterNo);
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            if (elemOrig.hasNonShadowLeaf() || possibleAdjNodes.isEmpty())
            {
                return states;
            }
            TreeState elem = elemOrig.copy();
            ArrayList<Node> elemfootToRootPath = null;
            short highestIdInPrefTree = makeUniqueIndices(elem, preforig);
            FreqCounter fc = new FreqCounter(opts);
            if (elem.hasFootLeft())
            {
                fc.accountForHistFringe(elem.getFringe(), null, false);//account for none-adjup events on path from root to foot of elem tree.
                elem.shiftNextUnaccessibleToAccessible();
            }
            else
            { //then can delete last fringe bit from elem
                elemfootToRootPath = elem.removeRightFootFringe().getAdjNodesOpenRight();
            }
            int adjnodenum = -1;
            TreeState elemcopy = null;
            if (possibleAdjNodes.size() > 1)
            {
                elemcopy = elem.copy();
            }
            for (Node adjNode : possibleAdjNodes)
            {
                TreeState pref = preforig.copy();
                FreqCounter fc2 = new FreqCounter(fc);//fc.clone();
                String clusterNo = ipiNodeClusterNo.get(adjNode);
                int adjNodePos = preforig.getFringe().getAdjoinableNodes().indexOf(adjNode);
                Fringe prefFringe = pref.getFringe();
                Node adjNodeNew = prefFringe.getAdjoinableNodes().get(adjNodePos);
                if (adjnodenum > -1)
                {
                    if (adjnodenum == possibleAdjNodes.size() - 2)
                    {
                        elem = elemcopy;
                    }
                    else
                    {
                        elem = elemcopy.copy();
                    }
                }
                adjnodenum++;
                //go past lex anchor for elemtree; substnode must actually be null!
                // go past subst node in pref tree
                Fringe newFringe = elem.getFringe();
                //newElemUa can be empty
                ArrayList<Fringe> newUnaccessibles = (ArrayList<Fringe>) elem.getUnaccessibles();
                //if elem root node on fringe
                ArrayList<Fringe> prefFringes = new ArrayList<Fringe>();
                Fringe newPrefFringe = new Fringe(
                        prefFringe.getRestOfAdjNodesOpenRight(adjNodeNew, elem.hasFootLeft()),
                        prefFringe.getRestOfAdjNodesOpenLeft(adjNodeNew, elem.hasFootLeft()),
                        prefFringe.getSubstNode(), -2);//, prefFringe);//.getFringeConts(), prefFringe.getCutOffLocations());
                if (elem.hasFootLeft())
                {//root and foot should be based on adjnode (for correct kernel).
                    //if (ParserModel.useLeftMost && adjNode.getLeftMostCover().equals("?"))
                    //	System.out.print("\n"+adjNode+"\t"+ elem.toString());
                    elem.getFringe().getAdjNodesOpenRight().get(0).setDownIndex(adjNode.getDownIndex());
                    adjNode = elem.fixFringeAdjLeft(adjNodeNew, pref.getFringe(), adjNode);
                    //if (kernel !=null) 
                    //	adjNode = new Node(kernel, adjNode.getDownIndex(), adjNode.getUpIndex(), (byte) adjNode.getTimeStamp(), adjNode.getNodeId(),
                    //			);
                    //System.out.println(elem);
                }
                else
                {
                    Node elemRootNode = elem.getRootNode();
                    pref.fixFringeAdj(adjNodeNew, elemfootToRootPath, pref.getWordCover()[1]);//,"?");
                    if (elemRootNode != null)
                    {
                        elemRootNode.setUpIndex(adjNode.getUpIndex());
                    }
                }
                prefFringes.add(newPrefFringe);
                prefFringes.addAll(pref.getUnaccessibles());
                FringeAndProb newnext;
                if (prefFringes.isEmpty())
                {
                    newnext = pref.getFutureFringe();
                }
                else
                {
                    ArrayList<FringeAndProb> newlist = prependPrefFringes(prefFringes, pref.getFutureFringe());
                    newnext = new FringeAndProb(null, pref.getFutureFringe().getnBestProbs(), newlist, pref.getFutureFringe().getBBHist());
                }

                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(pref.getShadowTrees());
                for (ShadowStringTree st : elem.getShadowTrees())
                {
                    st.setIntegLambda(adjNode.getDownPosTag());
                    st.setIntegLambdaId(adjNode.getLambdaTimestamp());
                }
                newShadowTrees.addAll(elem.getShadowTrees());
                fc2.accountForHistFringe(prefFringe, adjNodeNew, true);// account for none adjdown on nodes between anchor and adj site on pref fringe.
                TreeState ts = !opts.useSemantics ? 
                        new TreeState(preforig.isAux(), preforig.hasFootLeft(), preforig.getWordCover().clone(), newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest) ://, preforig.getContList());
                        new DepTreeState(preforig.isAux(), preforig.hasFootLeft(), preforig.getWordCover().clone(), newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest, 
                                ((DepTreeState)preforig).getDependencies(), parserModel);
                if(opts.useSemantics) // adjDownXS
                {                    
                    setPrefixOffsetOfShadowTrees(newShadowTrees, highestIdInPrefTree, adjNodeNew.getNodeId(), (short)tree.getFoot());
//                    ((DepTreeState)ts).updateDependencies(adjNodeNew, tree, elem.hasFootLeft() ? elem.getFringe().getLastAdjNode() : elemfootToRootPath.get(0), highestIdInPrefTree, false, true);
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), adjNodeNew, tree, (short)tree.getFoot(), highestIdInPrefTree, false, true, words, origPosTags, "A", false, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    BuildBlock buildBlock = new BuildBlock(opts, this, adjnodenum, adjNode, possibleAdjNodes.size(), clusterNo, tree);
                    if (opts.countNoneAdj)
                    {
                        fc2.addSingleTreeProbEl(new TreeProbElement(ParserOperation.adjoinDownXF.name(), adjNode.getCategory(), "NONEADJ",
                                treeState.getFringe().hasTraceLeft(), treeState.getFringe().hasTraceRight(), adjNode.getOrigTree(), adjNode.getPosInTree(),
                                adjNode.getLambda(), adjNode.getLeftMostCover(), adjNode.getClusterNumber()));
                    }
                    buildBlock.setFreqCounter(fc2);
                    buildBlock.setWordProbAndTreeProb(parserModel, treeState.getFringe(), adjNode.getDownPosTag());
                    treeState.getFutureFringe().addCutOffLocation(buildBlock, preforig.getWordCover()[1]);
                    if (!prefFringes.isEmpty())
                    {
                        for (FringeAndProb f : treeState.getFutureFringe().getNext())
                        {
                            f.addCutOffLocation(buildBlock, preforig.getWordCover()[1]);
                        }
                    }
                    fc2.accountForHistFringe(treeState.getFringe(), null, true);//account for none adjdown nodes between anchor and further traces on elem fringe.
                    states.add(new ChartEntry(opts, treeState, buildBlock));
                }
            }
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTree, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTreeAnalysis newTree = prefTree.clone();
            StringTree elemTreeCopy = elemTree.copy();
            TreeState elemts = new TreeState(elemTreeCopy, (short) 0, opts.nBest);
            boolean successful = newTree.adjoin(elemTreeCopy, adjNodeNumber, targetTreeState, null);//, traceleft, traceright);

            if (!successful && TreeState.isNullLexeme(newTree.getFringe().getLastAdjNode().getCategory()))
            {
                //can't easily access next fringe from prefTree state. Therefore need to calculate it from "scratch" from StringTree.
                StringTree copiedTree = prefTree.getStringTree().copy();
                TreeState ts = new TreeState(copiedTree, (short) 0, opts.nBest);
                Fringe f = ts.getFringeAfterLastNonShadowNonNullLexAnchor();
                if(opts.verbose)
                    LogInfo.error("\n### equal?: " + f + prefTree.getFringe());
                ArrayList<Fringe> flist = ts.getUaAfter(f);
                if (flist.isEmpty())
                {
                    return null;
                }
                f = flist.get(0);
                // changed "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
                newTree = new StringTreeAnalysis(opts, copiedTree, f, prefTree.getTrace(), Integer.MIN_VALUE, prefTree.getFreqCounter(), new StringBuilder(prefTree.getBuiltTrace()));
//                newTree = new StringTreeAnalysis(opts, copiedTree, f, prefTree.getTrace(), "", prefTree.getFreqCounter(), new StringBuilder(prefTree.getBuiltTrace()));
                newTree.adjoin(elemTreeCopy, adjNodeNumber, targetTreeState, null);//, true, false);
            }
            /*	old probably buggy version	
            while (!successful && TreeState.isNullLexeme(newTree.getFringe().getLastAdjNode().getCategory())){
            ts.shiftNextUnaccessibleToAccessible();
            newTree = new StringTreeAnalysis(prefStringTreeClone,ts.getFringe(), prefTree.getTrace(), "", prefTree.getFreqCounter(),  new StringBuilder(prefTree.getBuiltTrace()));
            successful = newTree.adjoin(elemTreeCopy, adjNodeNumber, targetTreeState, null, traceleft, traceright);
            }//*/
            return newTree;
        }
    },
    adjoinUpFF
    {
        @Override
        public ArrayList<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemorig, ElementaryStringTree tree, int timestamp)
        {
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            FreqCounter fcOuter = new FreqCounter(opts);
            Fringe fringe = elemorig.getFringe();
            states.addAll(adjoinUpFFfixedElem(parserModel, opts, words, origPosTags, preforig, elemorig, fcOuter, tree, timestamp));
            if (fringe.hasTraceRight())
            {
                fcOuter.accountForHistFringe(fringe, null, false);//account for none adjup on path from root to trace.
                TreeState elemo = elemorig.copy();
                elemo.shiftNextUnaccessibleToAccessible();
                states.addAll(adjoinUpFFfixedElem(parserModel, opts, words, origPosTags, preforig, elemo, fcOuter, tree, timestamp));
                fringe = elemo.getFringe();
                if (fringe.hasTraceRight())
                {
                    fcOuter.accountForHistFringe(fringe, null, false);//account for none adjup on path from root to trace.
                    TreeState elemo2 = elemo.copy();
                    elemo2.shiftNextUnaccessibleToAccessible();
                    states.addAll(adjoinUpFFfixedElem(parserModel, opts, words, origPosTags, preforig, elemo2, fcOuter, tree, timestamp));
                }
            }
            return states;
        }

        private Collection<ChartEntry> adjoinUpFFfixedElem(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemOrig, FreqCounter fcouter, ElementaryStringTree tree, int timestamp)
        {
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            HashMap<Node, String> ipiNodeClusterNo = new HashMap<Node, String>();
            ArrayList<Node> possibleAdjNodes = preforig.findAdjoinable(elemOrig.getFringe(), false, ipiNodeClusterNo);
            if ((elemOrig.isAux() && elemOrig.hasFootLeft()) || possibleAdjNodes.isEmpty() || preforig.getFringe().getSubstNode() != null
                    || preforig.getFringe().getLastAdjNode().isShadow() || TreeState.isNullLexeme(preforig.getFringe().getLastAdjNode().getCategory())
                    || preforig.getFutureFringe().hasNext()
                    || !elemOrig.hasNoSubstBeforeLexAnchor() || preforig.getRootNode() == null || preforig.getRootNode().isShadow())
            {
                return states;
            }
            int adjnodenum = -1;
            TreeState pref = preforig;//.copy();
            for (Node adjNode : possibleAdjNodes)
            {
                adjnodenum++;
                String clusterNo = ipiNodeClusterNo.get(adjNode);
                if (preforig.isAux() && elemOrig.isAux() && !preforig.hasFootLeft() && adjNode == elemOrig.getRootNode() && !elemOrig.hasFootLeft())
                {
                    continue; // don't allow for adjUp at root node (because equivalent to adjdown at foot node of prefix tree).
                }
                TreeState elem = elemOrig.copy();
                int adjNodePos = elemOrig.getFringe().getAdjoinableNodes().indexOf(adjNode);
                Node adjNodeNew = elem.getFringe().getAdjoinableNodes().get(adjNodePos);
                FreqCounter fc = new FreqCounter(fcouter);//fcouter.clone();
                fc.accountForHistFringe(elem.getFringe(), adjNodeNew, false);//account for none adjup operations on way from root to adj site on elementary tree.
                fc.accountForHistFringe(pref.getFringe(), null, true);// account for none adjdown operations on way from last anchor to foot / root.
                short highestIdInPrefTree = this.makeUniqueIndices(elem, pref);
                short[] wc =
                {
                    pref.getWordCover()[0], elem.getWordCover()[1]
                };
                if (adjNode.getDownIndex() == -1)
                {
                    elem.getFringe().getAdjNodesOpenLeft().clear();
                }
                else
                {
                    for (int an = 0; an < elemOrig.getFringe().getAdjNodesOpenLeft().indexOf(adjNode); an++)
                    {
                        elem.getFringe().getAdjNodesOpenLeft().remove(0);
                    }
                }
                ArrayList<Node> preflastfringe = pref.getLastFringe().getAdjNodesOpenRight();
                elem.addLeftMostAnnot(preflastfringe.get(preflastfringe.size() - 1).getLeftMostCover());
                if (!pref.hasFootLeft())
                {
                    elem.fixFringeAdj(adjNodeNew, preflastfringe, wc[1]);//,"?");
                }
                Fringe newFringe = elem.getFringeAfterLastNonShadowNonNullLexAnchor();
                elem.getFringe().getAdjNodesOpenLeft().remove(0); // remove adj node from fringe of elemtree.
                fc.accountForHistFringe(elem.getFringe(), null, false);//account for none adjup operations one way from adj site to lexical anchor.
                elem.shiftNextUnaccessibleToAccessible();
                Fringe histfringe = elem.getFringe();
                while (histfringe != newFringe)
                {
                    fc.accountForHistFringe(histfringe, null, false);//account for none adjup operations one way from first leaf to lexical anchor of elemtree.
                    elem.shiftNextUnaccessibleToAccessible();
                    histfringe = elem.getFringe();
                }
                //newElemUa can be empty
                ArrayList<Fringe> newUnaccessibles = (ArrayList<Fringe>) elem.getUaAfter(newFringe);
                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(pref.shadowListDeepCopy());
                for (ShadowStringTree st : elem.getShadowTrees())
                {
                    st.setIntegLambda(preforig.getRootNode().getLambda());
                    st.setIntegLambdaId(preforig.getRootNode().getLambdaTimestamp());
                }
                newShadowTrees.addAll(elem.getShadowTrees());
                TreeState ts = !opts.useSemantics ? 
                        new TreeState(elemOrig.isAux(), elemOrig.hasFootLeft(), wc, newFringe, newUnaccessibles, pref.getFutureFringe(), newShadowTrees, true, opts.nBest) : //, preforig.getContList());
                        new DepTreeState(elemOrig.isAux(), elemOrig.hasFootLeft(), wc, newFringe, newUnaccessibles, pref.getFutureFringe(), newShadowTrees, true, opts.nBest, 
                                ((DepTreeState)preforig).getDependencies(), parserModel);
                ts.addLeftMostAnnot(this.getPos(words, preforig) + "+" + newFringe.getAdjNodesOpenRight().get(0).getCategory());
                if(opts.useSemantics) // adjUpFF
                {                    
//                    Node prefFootNode = pref.getFringe().getAdjNodesOpenLeft().get(pref.getFringe().getAdjNodesOpenLeft().size() - 1);
                    Node prefFootNode = preforig.getRootNode();
//                    ((DepTreeState)ts).updateDependencies(prefFootNode, tree, prefFootNode.getNodeId(), highestIdInPrefTree, true, false);
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), prefFootNode, tree, (short)-1, highestIdInPrefTree, true, false, words, origPosTags, "A", true, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    Node ipiNode = preforig.getRootNode().copy();
                    ipiNode.setPosInTree(adjNode.getPosInTree());
                    BuildBlock buildBlock = new BuildBlock(opts, this, adjnodenum, ipiNode, possibleAdjNodes.size(), clusterNo, tree);
                    if (opts.countNoneAdj)
                    {
                        fc.addSingleTreeProbEl(new TreeProbElement(ParserOperation.adjoinUpFS.name(), ipiNode.getCategory(), "NONEADJ",
                                treeState.getFringe().hasTraceLeft(), treeState.getFringe().hasTraceRight(), ipiNode.getOrigTree(), ipiNode.getPosInTree(),
                                ipiNode.getLambda(), ipiNode.getLeftMostCover(), ipiNode.getClusterNumber()));
                    }
                    buildBlock.setFreqCounter(fc);
                    buildBlock.setWordProbAndTreeProb(parserModel, treeState.getFringe(), ipiNode.getLambda());
                    //BuildBlock buildBlock = new BuildBlock(this, adjnodenum, preforig.getRootNode(), possibleAdjNodes.size());//adjnode
                    treeState.getFutureFringe().addCutOffLocation(buildBlock, wc[1]);
                    states.add(new ChartEntry(opts, treeState, buildBlock));
                    fc.accountForHistFringe(treeState.getFringe(), null, true);//account for none adjdown operations in preftree on path from anchor to trace(s).
                }
            }
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTreeOrig, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTree elemTreeCopy = elemTree.copy();
            // changed second "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
            //StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, 
            //        new TreeState(elemTreeCopy, (short) 0, opts.nBest).getFringe(), "", Integer.MIN_VALUE, new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
//            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, 
//                    new TreeState(elemTreeCopy, (short) 0, opts.nBest).getFringe(), "", "", new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            // Fixed bug (09-01-2014)
            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, 
                    new TreeState(elemTreeCopy, (short) 0, opts.nBest).getFringe(), "", Integer.MIN_VALUE,  prefTreeOrig.getFreqCounter(), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            StringTree prefTree = prefTreeOrig.getStringTree().copy();
            if (prefTree.hasFootLeft())
            {
                System.out.println("unexpected case of adjupFF with foot left; ParserOperation.adjoinUpFF.integrate()");
            }
            boolean successful = newTree.adjoin(prefTree, adjNodeNumber, targetTreeState, false);//, traceleft, traceright);

            if (!successful && newTree.getFringe().hasTraceRight())
            {
                elemTreeCopy = elemTree.copy();
                TreeState ts = new TreeState(elemTreeCopy, (short) 0, opts.nBest);
                Fringe elemfringe = ts.getFringe();
                ts.shiftNextUnaccessibleToAccessible();
                // changed second "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
//                newTree = new StringTreeAnalysis(opts, elemTreeCopy, ts.getFringe(), "", Integer.MIN_VALUE, new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
//                newTree = new StringTreeAnalysis(opts, elemTreeCopy, ts.getFringe(), "", "", new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
                // Fixed bug (09-01-2014)
                newTree = new StringTreeAnalysis(opts, elemTreeCopy,ts.getFringe(), "", Integer.MIN_VALUE, prefTreeOrig.getFreqCounter(), new StringBuilder(prefTreeOrig.getBuiltTrace()));
                newTree.adjoin(prefTree, adjNodeNumber, targetTreeState, false);
            }
            return newTree;
        }
    },
    adjoinUpFS
    {
        @Override
        public ArrayList<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemorig, ElementaryStringTree tree, int timestamp)
        {
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            FreqCounter fcOuter = new FreqCounter(opts);
            Fringe fringe = elemorig.getFringe();
            states.addAll(adjoinUpFSfixedElem(parserModel, opts, words, origPosTags, preforig, elemorig, fcOuter, tree, timestamp));
            if (fringe.hasTraceRight())
            {
                fcOuter.accountForHistFringe(fringe, null, false);//account for none adjup on path from root to trace.
                TreeState elemo = elemorig.copy();
                elemo.shiftNextUnaccessibleToAccessible();
                states.addAll(adjoinUpFSfixedElem(parserModel, opts, words, origPosTags, preforig, elemo, fcOuter, tree, timestamp));
                fringe = elemo.getFringe();
                if (fringe.hasTraceRight())
                {
                    fcOuter.accountForHistFringe(fringe, null, false);//account for none adjup on path from root to trace.
                    TreeState elemo2 = elemo.copy();
                    elemo2.shiftNextUnaccessibleToAccessible();
                    states.addAll(adjoinUpFSfixedElem(parserModel, opts, words, origPosTags, preforig, elemo2, fcOuter, tree, timestamp));
                }
            }
            return states;
        }

        private ArrayList<ChartEntry> adjoinUpFSfixedElem(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemOrig, FreqCounter fcouter, ElementaryStringTree tree, int timestamp)
        {
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            HashMap<Node, String> ipiNodeClusterNo = new HashMap<Node, String>();
            ArrayList<Node> possibleAdjNodes = preforig.findAdjoinable(elemOrig.getFringe(), true, ipiNodeClusterNo);

            if (elemOrig.isAux() && elemOrig.hasFootLeft() || possibleAdjNodes.isEmpty()
                    || !(preforig.getUnaccessibles().size() == 1 && !preforig.getFutureFringe().hasNext()
                    && preforig.getRootNode() != null && !preforig.getRootNode().isShadow() && preforig.getFringe().getSubstNode() == null))
            {
                return states;
            }
            int adjnodenum = -1;
            TreeState pref = preforig;
            for (Node adjNode : possibleAdjNodes)
            {
                adjnodenum++;
                String clusterNo = ipiNodeClusterNo.get(adjNode);
                if (preforig.isAux() && elemOrig.isAux() && !preforig.hasFootLeft() && adjNode == elemOrig.getRootNode() && !elemOrig.hasFootLeft())
                {
                    continue; // don't allow for adjUp at root node (because equivalent to adjdown at foot node of prefix tree).
                }
                TreeState elemcopy = elemOrig.copy();
                int adjNodePos = elemOrig.getFringe().getAdjoinableNodes().indexOf(adjNode);
                Node adjNodeNew = elemcopy.getFringe().getAdjoinableNodes().get(adjNodePos);
                FreqCounter fc = new FreqCounter(fcouter);//fcouter.clone();
                fc.accountForHistFringe(elemcopy.getFringe(), adjNodeNew, false);//account for none adjup operations on way from root to adj site on elementary tree.
                fc.accountForHistFringe(pref.getFringe(), null, true);// account for none adjdown operations on way from last anchor to foot / root on prefix tree.
                short highestIdInPrefTree = this.makeUniqueIndices(elemcopy, pref);
                if (adjNode.getDownIndex() == -1)
                {
                    elemcopy.getFringe().getAdjNodesOpenLeft().clear();
                }
                else
                {
                    for (int an = 0; an < elemOrig.getFringe().getAdjNodesOpenLeft().indexOf(adjNode); an++)
                    {
                        elemcopy.getFringe().getAdjNodesOpenLeft().remove(0);
                    }
                }
                ArrayList<Node> preflastfringe = pref.getLastFringe().getAdjNodesOpenRight();
                elemcopy.addLeftMostAnnot(preflastfringe.get(preflastfringe.size() - 1).getLeftMostCover());
                Fringe newFringe = new Fringe(
                        elemcopy.getFringe().getRestOfAdjNodesOpenRight(adjNodeNew, false),
                        elemcopy.getFringe().getRestOfAdjNodesOpenLeft(adjNodeNew, false),
                        elemcopy.getFringe().getSubstNode(), -2);//, elemcopy.getFringe());//null, null); TODO check.

                if (!pref.hasFootLeft())
                {
                    elemcopy.fixFringeAdj(adjNodeNew, preflastfringe, pref.getWordCover()[1]);//,"?");
                }

                ArrayList<Fringe> newUnaccessibles = (ArrayList<Fringe>) elemcopy.getUnaccessibles();
                ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
                newShadowTrees.addAll(pref.shadowListDeepCopy());
                for (ShadowStringTree st : elemcopy.getShadowTrees())
                {
                    st.setIntegLambda(preforig.getRootNode().getLambda());
                    st.setIntegLambdaId(preforig.getRootNode().getLambdaTimestamp());
                }
                newShadowTrees.addAll(elemcopy.getShadowTrees());

                short[] wc =
                {
                    pref.getWordCover()[0], elemOrig.getWordCover()[1]
                };

                TreeState ts = !opts.useSemantics ? 
                        new TreeState(elemOrig.isAux(), elemOrig.hasFootLeft(), wc, newFringe, newUnaccessibles, pref.getFutureFringe(), newShadowTrees, true, opts.nBest) :
                        new DepTreeState(elemOrig.isAux(), elemOrig.hasFootLeft(), wc, newFringe, newUnaccessibles, pref.getFutureFringe(), newShadowTrees, true, opts.nBest, 
                                ((DepTreeState)preforig).getDependencies(), parserModel);
                ts.addLeftMostAnnot(preforig.getFringe().getAdjNodesOpenRight().get(preforig.getFringe().getAdjNodesOpenRight().size() - 1).getLeftMostCover());// TODO: check needed?
                if(opts.useSemantics) // adjUpFS
                {                                        
//                    Node prefFootNode = pref.getFringe().getAdjNodesOpenLeft().get(pref.getFringe().getAdjNodesOpenLeft().size() - 1);
                    Node prefFootNode = preforig.getRootNode();
                    setPrefixOffsetOfShadowTrees(newShadowTrees, highestIdInPrefTree, prefFootNode.getNodeId(), adjNode.getNodeId());
//                    ((DepTreeState)ts).updateDependencies(prefFootNode, tree, prefFootNode.getNodeId(), highestIdInPrefTree, true, true);
                    ((DepTreeState)ts).updateDependencies(elemOrig.copy(), prefFootNode, tree, (short)-1, highestIdInPrefTree, true, true, words, origPosTags, "A", true, timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                for (TreeState treeState : tss)
                {
                    Node ipiNode = preforig.getRootNode().copy();
                    ipiNode.setPosInTree(adjNode.getPosInTree());
                    BuildBlock bb = new BuildBlock(opts, this, adjnodenum, ipiNode, possibleAdjNodes.size(), clusterNo, tree);
                    if (opts.countNoneAdj)
                    {
                        fc.addSingleTreeProbEl(new TreeProbElement(ParserOperation.adjoinUpFF.name(), ipiNode.getCategory(), "NONEADJ",
                                treeState.getFringe().hasTraceLeft(), treeState.getFringe().hasTraceRight(), ipiNode.getOrigTree(), ipiNode.getPosInTree(),
                                ipiNode.getLambda(), ipiNode.getLeftMostCover(), ipiNode.getClusterNumber()));
                    }
                    bb.setFreqCounter(fc);
                    bb.setWordProbAndTreeProb(parserModel, treeState.getFringe(), ipiNode.getLambda());
                    treeState.getFutureFringe().addCutOffLocation(bb, wc[1]);
                    states.add(new ChartEntry(opts, treeState, bb));
                    fc.accountForHistFringe(treeState.getFringe(), null, true);
                }
            }
            return states;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTreeOrig, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTree elemTreeCopy = elemTree.copy();
            // changed second "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
            //StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, new TreeState(elemTreeCopy, (short) 0, opts.nBest).getFringe(), "", Integer.MIN_VALUE, new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
//            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, new TreeState(elemTreeCopy, (short) 0, opts.nBest).getFringe(), "", "", new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            // Fixed bug (09-01-2014)
            StringTreeAnalysis newTree = new StringTreeAnalysis(opts, elemTreeCopy, new TreeState(elemTreeCopy, (short) 0, opts.nBest).getFringe(), "", Integer.MIN_VALUE, prefTreeOrig.getFreqCounter(), new StringBuilder(prefTreeOrig.getBuiltTrace()));
            StringTree prefTree = prefTreeOrig.getStringTree().copy();
            if (prefTree.hasFootLeft())
            {
                System.out.println("unexpected case of adjupFF with foot left; ParserOperation.adjoinUpFF.integrate()");
            }
            boolean successful = newTree.adjoin(prefTree, adjNodeNumber, targetTreeState, true);//, traceleft, false);

            if (!successful && TreeState.isNullLexeme(newTree.getFringe().getLastAdjNode().getCategory()))
            {
                elemTreeCopy = elemTree.copy();
                TreeState ts = new TreeState(elemTreeCopy, (short) 0, opts.nBest);
                ts.shiftNextUnaccessibleToAccessible();
                // changed second "" to Integer.MIN_VALUE as this is the default agreed empty value. NOTE: never used in the called constructor
//                newTree = new StringTreeAnalysis(opts, elemTreeCopy, ts.getFringe(), "", Integer.MIN_VALUE, new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
//                newTree = new StringTreeAnalysis(opts, elemTreeCopy, ts.getFringe(), "", "", new FreqCounter(opts), new StringBuilder(prefTreeOrig.getBuiltTrace()));
                // Fixed bug (09-01-2014)
                newTree = new StringTreeAnalysis(opts, elemTreeCopy,ts.getFringe(), "", Integer.MIN_VALUE, prefTreeOrig.getFreqCounter(), new StringBuilder(prefTreeOrig.getBuiltTrace()));
                newTree.adjoin(prefTree, adjNodeNumber, targetTreeState, true);//, traceleft, false);
            }
            return newTree;
        }
    },
    // is it ever possible that two different shadow trees match the verification tree given the incrementality assumption???
    // I think NO! because only one of the pref trees can have their lex anchor coming up next. Tree elem was already verified
    // with pref tree before, this function only calculates the resulting fringe.
    verify
    {

        @SuppressWarnings("unchecked")
        @Override
        public ArrayList<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preforig, TreeState elemOrig, ArrayList<Byte> indexPattern,
                                             ElementaryStringTree tree, ShadowStringTree shadowtree, Map<Integer, Integer> coveredNodes, 
                                             Map<Short, Short> offsetNodeIdsOfShadowTree, int timestamp)
        {            
            TreeState pref = preforig.copy();
            TreeState elem = elemOrig.copy();
            short highestIdInPrefTree = this.makeUniqueIndices(elem, pref);
            if (elem.isAux() && !elem.hasFootLeft())
            {
                elem.removeRightFootFringe();
            }
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            short[] wc =
            {
                pref.getWordCover()[0], elem.getWordCover()[1]
            };
            // update shadow status of all affected nodes in fringe (also pref root, foot etc. if affected), done by Parsing Engine.

            // whenever a shadow tree is introduced earlier, the fringe remains to the left, because the anchor has not
            // been seen yet. However, when the shadow tree is verified, any substitution node before the anchor must be
            // filled. The lexical anchor is instantiated, so the fringe is switched to after the lex anchor. 
            // this means that the pref tree fringe MUST contain the lex anchor as the next accessible leaf.
            // we can however not just switch the preftreefringe, but have to switch the elem tree fringe to the bit 
            // after the leaf node.
            Fringe newFringe = elem.getFringeAfterLastNonShadowNonNullLexAnchor();
            if (!newFringe.getAdjNodesOpenLeft().isEmpty() && TreeState.isNullLexeme(newFringe.getAdjNodesOpenLeft().get(0).getCategory()))
            {
                newFringe = elem.getFringesBeforeFirstNullLexeme().get(0);
            }
            //The unaccessibles are then the rest of the fringe from there, plus the fringe above the now verified tree... 
            // so have to find how the root of the elem tree connects to the rest of the tree (should logically be still 
            // in the substitution or adjunction nodes of pref tree fringe, no?!?!?!) This in turn means that because of the
            // way the fringes are controlled when putting trees together (in particular in the FS operations), the root node 
            // of the elem tree is definitely in the adjunction nodes of the current preftree next fringe (from after pJoining)
            ArrayList<Fringe> newUnaccessibles = elem.getUaAfter(newFringe);//elem.getUaAfterLastLexAnchor();
            Fringe prefNextFringe = null;
            //need to find original root node in shadow tree (is it guarantied to be in pref.getNextFringe()?, I think yes.)
            //assumption that left side of root node is not on visible fringe anymore.
            //underlying problem is related to adjunction at subst nodes and whether pred tree was used.
            Node adjNode = null;
            //ArrayList<BuildBlock> cutOffsEarlierFringes = new ArrayList<BuildBlock>();
            //cutOffsEarlierFringes = (ArrayList<BuildBlock>) pref.getFringe().getCutOffLocations().clone();
            FreqCounter fc = new FreqCounter(opts);
            fc.accountForHistFringe(pref.getFringe(), null, true);//account for none adjdown operations on prefix tree between last lexical anchor and spine which is being verified.
            for (Fringe f : pref.getUnaccessibles())
            {
                adjNode = findOriginalRootNode(f, elem, indexPattern);//check correctness here.
				/*if (adjNode.getPosInTree()>1 && preforig.getShadowTrees().size()==1 && preforig.getShadowTrees().get(0).getShadowSourceTreesRootList().size()==1){
                System.out.println();
                //adjNode = findOriginalRootNode(f, elem, indexPattern);
                }*/
                if (adjNode != null)
                {
                    prefNextFringe = f;
                    break;
                }
            }
            if (prefNextFringe == null || adjNode == null)
            {
                // can actually happen if there are empty lexemes (such as 0 or traces).
                return states;
                //System.err.println("\nunexpected case in Parseroperation.verify. Could not find root node in pref.nextFringe.");
                //	prefNextFringe = pref.getNextFringe();
                //	adjNode = pref.getRootNode();
                //	if (adjNode ==null || prefNextFringe ==null) return states;
            }
            //newFringe.addAllCutOffLocs(cutOffsEarlierFringes); //TODO check whether necessary!
            ArrayList<Fringe> prefUa = pref.getUaAfter(prefNextFringe);
            Fringe newAboveShadowFringe;
            if (elem.isAux())
            {
                newAboveShadowFringe = new Fringe(
                        prefNextFringe.getRestOfAdjNodesOpenRight(adjNode, elem.hasFootLeft()),
                        prefNextFringe.getRestOfAdjNodesOpenLeft(adjNode, elem.hasFootLeft()),
                        prefNextFringe.getSubstNode(), -2);//, prefNextFringe);//.getFringeConts(), prefNextFringe.getCutOffLocations());
            }
            else
            {
                newAboveShadowFringe = new Fringe(
                        prefNextFringe.getRestOfAdjNodesOpenRight(adjNode, true),
                        prefNextFringe.getRestOfAdjNodesOpenLeft(adjNode, true),
                        prefNextFringe.getSubstNode(), -2);//, prefNextFringe);//.getFringeConts(), prefNextFringe.getCutOffLocations());	
            }
            //if nothing needs to be done, nodeIDofLastCommon = -1;
            short nodeIDofLastCommon = fixFringeForVerificationOld(newFringe, newUnaccessibles, prefNextFringe, indexPattern, elem.isAux(), newAboveShadowFringe);
            //if (!elem.isAux()) 
            //	elem.getRootNode().setUpIndex(adjNode.getUpIndex());
            if (nodeIDofLastCommon > -1)
            {
                boolean notfound = true;
                Fringe ef = elem.getFringe();
                ArrayList<Fringe> rest = elem.getUnaccessibles();
                while (notfound && rest.size() > 0)
                {
                    rest.get(0).setClusterNumberCount(false);
                    for (Node node : ef.getAdjNodesOpenLeft())
                    {
                        if (!notfound && opts.countNoneAdj)
                        {
                            fc.addSingleTreeProbEl(new TreeProbElement(ParserOperation.adjoinUpFF.name(), node.getCategory(), "NONEADJ",
                                    //node.getDownPosTag(),
                                    ef.hasTraceLeft(), false, node.getOrigTree(), node.getPosInTree(),
                                    node.getLambda(), node.getLeftMostCover(), node.getClusterNumber()));
                            fc.addSingleTreeProbEl(new TreeProbElement(ParserOperation.adjoinUpFS.name(), node.getCategory(), "NONEADJ",
                                    //node.getDownPosTag(), 
                                    ef.hasTraceLeft(), false, node.getOrigTree(), node.getPosInTree(),
                                    node.getLambda(), node.getLeftMostCover(), node.getClusterNumber()));
                        }
                        if (node.getNodeId() == nodeIDofLastCommon)
                        {
                            notfound = false;
                        }
                    }
                    ef = rest.remove(0);
                }
            }
            ArrayList<Fringe> prefFringes = new ArrayList<Fringe>();
            if (newUnaccessibles.isEmpty())
            {
                fJoin(newFringe, newAboveShadowFringe);
                newUnaccessibles.addAll(prefUa);
            }
            else
            {
                prefFringes.add(newAboveShadowFringe);
                prefFringes.addAll(prefUa);
            }
            FringeAndProb newnext;
            if (prefFringes.isEmpty())
            {
                newnext = pref.getFutureFringe();
            }
            else
            {
                ArrayList<FringeAndProb> newlist = prependPrefFringes(prefFringes, pref.getFutureFringe());
                newnext = new FringeAndProb(null, pref.getFutureFringe().getnBestProbs(), newlist, pref.getFutureFringe().getBBHist());
            }

            // following line not valid because should stop after h (could have lex anchor), and start new P slice.
            //shadow roots should have been corrected already during matching process.

            ArrayList<ShadowStringTree> newShadowTrees = new ArrayList<ShadowStringTree>();
            newShadowTrees.addAll(pref.getShadowTrees());
            for (ShadowStringTree st : elem.getShadowTrees())
            {
                Node n = elemOrig.getFringe().getAdjoinableNodes().get(0);
                st.setIntegLambda(n.getLambda());//if a combined tree (show ..up) verifies a prediction of VP.
                st.setIntegLambdaId(n.getLambdaTimestamp());
            }
            newShadowTrees.addAll(elem.getShadowTrees());
            TreeState ts = !opts.useSemantics ? 
                    new TreeState(pref.isAux(), pref.hasFootLeft(), wc, newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest) : 
                    new DepTreeState(pref.isAux(), pref.hasFootLeft(), wc, newFringe, newUnaccessibles, newnext, newShadowTrees, true, opts.nBest, 
                            ((DepTreeState)preforig).getDependencies(), parserModel);
            List<Node> adjNodes = newFringe.getAdjNodesOpenRight();
            if(adjNodes.size() > 0)
                ts.addLeftMostAnnot(this.getPos(words, pref) + "+" + adjNodes.get(0).getCategory());
            if(opts.useSemantics)
            {
                // map of verification nodes to (verified) shadow tree nodes
//                DualHashBidiMap<Integer, Integer> coveredNodesInverse = new DualHashBidiMap<Integer, Integer>();
//                for(Entry<Integer, Integer> e : coveredNodes.entrySet())
//                {
//                    coveredNodesInverse.put(e.getValue(), e.getKey());
//                }
                // coveredNodes is a map of (verified) shadow tree nodes to verification nodes 
                // warning: this is not safe, as we may be losing information caused by the restriction of having
                // an extra map on the values. For example the map [1=>1,2=>2,2=>5] will be converted to [1=>1,2=>5]
                DualHashBidiMap<Integer, Integer> coveredNodesBidi = new DualHashBidiMap<Integer, Integer>(coveredNodes);
                ElementaryStringTree shadowOrig = shadowtree.getTreeOrigIndex();
                Node shadowRoot = new Node(new NodeKernel(shadowOrig.getCategory(shadowOrig.getRoot()), 
                        shadowtree.getIntegLambda(),shadowtree.getIntegrationLambdaId()), (short)shadowOrig.getRoot());
                ((DepTreeState)ts).updateDependencies(elemOrig.copy(), coveredNodesBidi, tree, highestIdInPrefTree, (DualHashBidiMap)offsetNodeIdsOfShadowTree, shadowRoot, words, origPosTags, "V", timestamp);
            }
            ArrayList<TreeState> tss = ts.fixForTrace(words);
            Set<Integer> coveredNodeskeys = coveredNodes.keySet();
            for (TreeState treeState : tss)
            {
                BuildBlock buildBlock = new BuildBlock(opts, this, -1, adjNode, -1, tree);
                buildBlock.setVerifiedTree(shadowtree.getTreeOrigIndex(), shadowtree.getProb(), coveredNodeskeys);
                buildBlock.setFreqCounter(fc);
                buildBlock.setWordProbAndTreeProb(parserModel, treeState.getFringe(), shadowtree.getIntegLambda());//TODO check whether this has been set.
                treeState.getFutureFringe().addCutOffLocation(buildBlock, wc[1]);
                if (!prefFringes.isEmpty())
                {//else{
                    for (FringeAndProb f : treeState.getFutureFringe().getNext())
                    {
                        f.addCutOffLocation(buildBlock, wc[1]);
                    }
                }
                treeState.replaceShadow(shadowtree, indexPattern);
//				System.out.println(treeState.getShadowIndeces()+"\t"+ treeState.getShadowTrees());
                states.add(new ChartEntry(opts, treeState, buildBlock));
                fc.accountForHistFringe(treeState.getFringe(), null, true);
            }
            return states;
        }

        private Node findOriginalRootNode(Fringe fringe, TreeState elem, ArrayList<Byte> indexPattern)
        {
            Node node = null;
            ArrayList<Node> adjoinableNodes;
            String elemRootNodeCat;
            if (!elem.isAux())
            {
                adjoinableNodes = fringe.getAdjoinableNodes();
                elemRootNodeCat = elem.getRootNode().getCategory();
            }
            else if (elem.hasFootLeft())
            {
                elemRootNodeCat = elem.getRootNode().getCategory();
                adjoinableNodes = fringe.getAdjNodesOpenRight();
            }
            else
            {
                adjoinableNodes = new ArrayList<Node>();
                adjoinableNodes.addAll(fringe.getAdjNodesOpenLeft());
                if (fringe.getSubstNode() != null)
                {
                    adjoinableNodes.add(fringe.getSubstNode());
                }
                elemRootNodeCat = elem.getLastUnaccessible().getLastAdjNode().getCategory();
            }

            for (Node candidate : adjoinableNodes)
            {
                if (candidate.getCategory().equals(elemRootNodeCat) && candidate.isShadow())
                {
                    if ((!elem.isAux() || elem.hasFootLeft()) && StringTreeAnalysis.matches(indexPattern, candidate.getDownIndex())
                            && (candidate.getUpIndex() == -1 || !StringTreeAnalysis.matches(indexPattern, candidate.getUpIndex())))
                    {
                        node = candidate;
                    }
                    else if (elem.isAux() && !elem.hasFootLeft() && StringTreeAnalysis.matches(indexPattern, candidate.getUpIndex())
                            && (candidate.getDownIndex() == -1 || !StringTreeAnalysis.matches(indexPattern, candidate.getDownIndex())))
                    {
                        node = candidate;
                    }
                }
            }
            return node;
        }

        /**
         * change indices of all verified node indices to "0"; 
         * and insert any additional nodes from the verification tree (they are on the spine or to the right of the spine)
         * fix lambda and downPOStag annotation of nodes on spine.
         * @param newAboveShadowFringe 
         */
        private short fixFringeForVerificationOld(Fringe newFringe, ArrayList<Fringe> newUnaccessibles, Fringe nextFringe,// Node integRoot,
                                                  ArrayList<Byte> indexPattern, boolean isAuxElem, Fringe newAboveShadowFringe)
        {
            ArrayList<Fringe> newCombined = new ArrayList<Fringe>();
            //need to base this on pref tree? NextFringe shows nodes with added adjunctions... maybe problem specific to previous adjunction at subst node
            newCombined.add(newFringe);
            newCombined.addAll(newUnaccessibles);
            int posOfRootNodeInFringe = 0;
            boolean found = false;
            //FIND ROOT NODE IN OTHER FRINGE. ("root" is root or foot in the case of an auxiliary tree with foot right.)
            Node root = newCombined.get(newCombined.size() - 1).getLastAdjNode();
            ArrayList<Node> nextfringeAdjNodes = nextFringe.getAdjoinableNodes();
            for (int jj = nextfringeAdjNodes.size() - 1; jj >= 0; jj--)
            {
                Node n = nextfringeAdjNodes.get(jj);
                if (n.getCategory().equals(root.getCategory()) && //same category and if root, correct downindex, if foot correct upindex.
                        (root.getUpIndex() == -1 && StringTreeAnalysis.matches(indexPattern, n.getDownIndex()))
                        || (root.getDownIndex() == -1 && StringTreeAnalysis.matches(indexPattern, n.getUpIndex())))
                {
                    Node nc = n;
                    for (int jj2 = jj + 1; jj2 < nextfringeAdjNodes.size(); jj2++)
                    {
                        Node n2 = nextfringeAdjNodes.get(jj2);
                        if (n2.getCategory().equals(root.getCategory()) && n2.getLambda().equals("@R"))
                        {//.getOrigTree()==n.getOrigTree()){
                            found = true;
                            posOfRootNodeInFringe = jj2;
                            newAboveShadowFringe.getAdjNodesOpenRight().remove(nc);
                            newAboveShadowFringe.getAdjNodesOpenLeft().remove(nc);
                            nc = n2;
                        }
                    }
                    if (!found)
                    {
                        posOfRootNodeInFringe = jj;
                        found = true;
                    }
                    break;
                }
            }
            if (!found)
            {
                return -1;
            }
            boolean rootboolean = true;

            //////determine which nodes are on spine.
            ArrayList<Node> stack = new ArrayList<Node>();
            for (int i = newCombined.size() - 1; i >= 0; i--)
            {
                Fringe f = newCombined.get(i);
                if (f.getSubstNode() != null)
                {
                    Node n = f.getSubstNode();
                    if (!stack.isEmpty() && n.getCategory().equals(stack.get(0).getCategory()))
                    {
                        Node othersideN = stack.remove(0);
                        othersideN.setOnSpine(false);
                    }
                    else if (n.getDownIndex() == -1)
                    {
                        //assume foot
                        n.setOnSpine(true);
                        stack.add(0, n);
                    }
                }
                for (int ii = f.getAdjNodesOpenLeft().size() - 1; ii >= 0; ii--)
                {
                    Node n = f.getAdjNodesOpenLeft().get(ii);
                    if (!stack.isEmpty() && n.getCategory().equals(stack.get(0).getCategory()))
                    {
                        Node othersideN = stack.remove(0);
                        othersideN.setOnSpine(false);
                    }
                    else if (n.getDownIndex() == -1)
                    {
                        //assume foot
                        n.setOnSpine(true);
                        stack.add(0, n);
                    }
                    else
                    {
                        System.err.println("ParserOperation.fixFringeForVerificationOld: Can't find this in right sides.");
                    }
                }
                for (int ii = f.getAdjNodesOpenRight().size() - 1; ii >= 0; ii--)
                {
                    Node n = f.getAdjNodesOpenRight().get(ii);
                    //seeing right side of a node, need to check that left side also there. put on stack.
                    n.setOnSpine(true);
                    stack.add(0, n);
                }
            }
            // add nodes that came into spine through adjunction into the elementary tree fringe.
            int positionOfCurrentSpineNodeOnFringe = posOfRootNodeInFringe;
            for (int i = newCombined.size() - 1; i >= 0; i--)
            {
                Fringe f = newCombined.get(i);
                for (int ii = f.getAdjoinableNodes().size() - 1; ii >= 0; ii--)
                {
                    Node n = f.getAdjoinableNodes().get(ii);
                    Node spineNode = nextFringe.getAdjoinableNodes().get(positionOfCurrentSpineNodeOnFringe);
                    if (!n.isOnSpine())
                    {
                        continue;
                    }
                    if (n.getCategory().equals(spineNode.getCategory()))
                    {
                        if (rootboolean && n.getUpIndex() == -1)
                        {
                            //must be root
                            if (!StringTreeAnalysis.matches(indexPattern, spineNode.getDownIndex()))
                            {
                                int start = positionOfCurrentSpineNodeOnFringe;
                                while (!StringTreeAnalysis.matches(indexPattern, spineNode.getDownIndex()))
                                {
                                    positionOfCurrentSpineNodeOnFringe--;
                                    spineNode = nextFringe.getAdjoinableNodes().get(positionOfCurrentSpineNodeOnFringe);
                                }
                                positionOfCurrentSpineNodeOnFringe = dealWithAdjunction(start, positionOfCurrentSpineNodeOnFringe, indexPattern, spineNode, ii, f, nextFringe);
                            }
                            else
                            {
                                positionOfCurrentSpineNodeOnFringe--;
                                if (isAuxElem)
                                {
                                    int index = ii;
                                    if (f.getSubstNode() != null)
                                    {//should not happen
                                        System.out.println("Unexpected in parserOperation fix Verification");
                                    }
                                    int openL = f.getAdjNodesOpenLeft().size(); // should be 0
                                    if (index >= openL && openL > 0)
                                    {
                                        index -= openL;
                                    }
                                    Node oldRoot = f.getAdjNodesOpenRight().remove(index);
                                    Node newRoot = spineNode.copy();
                                    newRoot.setDownIndex(oldRoot.getDownIndex());
                                    newRoot.setDownPosTag(oldRoot.getDownPosTag());//if "root" node is an root or foot node of an auxiliary tree, it doesn't have its own downpostag.
                                    f.getAdjNodesOpenRight().add(index, newRoot);
                                }
                                else
                                {
                                    n.setLeftMostCover(spineNode.getLeftMostCover());
                                }
                            }
                            rootboolean = false;
                        }
                        else if (rootboolean == true && n.getDownIndex() == -1 && isAuxElem)
                        {
                            //must be foot
                            positionOfCurrentSpineNodeOnFringe--;
                            int index = ii - f.getAdjNodesOpenRight().size();
                            Node oldfoot = f.getAdjNodesOpenLeft().remove(index);
                            Node newFoot = spineNode.copy();
                            newFoot.setUpIndex(oldfoot.getUpIndex());
                            newFoot.setDownPosTag(oldfoot.getDownPosTag());
                            f.getAdjNodesOpenLeft().add(index, newFoot);
                            rootboolean = false;
                        }
                        else if (StringTreeAnalysis.matches(indexPattern, spineNode.getUpIndex())
                                && StringTreeAnalysis.matches(indexPattern, spineNode.getDownIndex()))
                        {
                            //internal node at which no adjunction happened.
                            n.setLeftMostCover(spineNode.getLeftMostCover());
                            positionOfCurrentSpineNodeOnFringe--;
                        }
                        else if (StringTreeAnalysis.matches(indexPattern, spineNode.getUpIndex())
                                && !StringTreeAnalysis.matches(indexPattern, spineNode.getDownIndex()))
                        {
                            //internal node on spine where adjunction happened.
                            int start = positionOfCurrentSpineNodeOnFringe;
                            while (!StringTreeAnalysis.matches(indexPattern, spineNode.getDownIndex()))
                            {
                                positionOfCurrentSpineNodeOnFringe--;
                                spineNode = nextFringe.getAdjoinableNodes().get(positionOfCurrentSpineNodeOnFringe);
                            }
                            positionOfCurrentSpineNodeOnFringe = dealWithAdjunction(start, positionOfCurrentSpineNodeOnFringe, indexPattern, spineNode, ii, f, nextFringe);
                        }
                        if (positionOfCurrentSpineNodeOnFringe == -1)
                        {
                            if (ii > 0)
                            {
                                if (f.getAdjoinableNodes().get(ii).getDownIndex() > 0)
                                {
                                    return f.getAdjoinableNodes().get(ii + 1).getNodeId();
                                }
                                return f.getAdjoinableNodes().get(ii).getNodeId();
                            }
                            return -1;
                        }
                    }
                }
            }
            return -1;
        }

        private int dealWithAdjunction(int start, int positionOfCurrentSpineNodeOnFringe, ArrayList<Byte> indexPattern, Node spineNode, int ii, Fringe f, Fringe nextFringe)
        {
            ArrayList<Node> adjNodes;
            int removepos = ii;
            if (f.getAdjNodesOpenRight().size() > ii)
            {
                adjNodes = f.getAdjNodesOpenRight();
            }
            else
            {
                adjNodes = f.getAdjNodesOpenLeft();
                removepos = ii - f.getAdjNodesOpenRight().size();
            }
            Node firstNode = adjNodes.get(removepos);
            Node equivNode = nextFringe.getAdjoinableNodes().get(start);
            firstNode.setTimeStamp((short) equivNode.getTimeStamp());
            firstNode.setDownIndex(equivNode.getDownIndex());
            // don't change downPOStag, this should be adjNode's downPOStag. firstNode.setDownPosTag(equivNode.getDownPosTag());
            firstNode.setUpIndex(equivNode.getUpIndex());
            firstNode.setLeftMostCover(equivNode.getLeftMostCover());
            for (int z = start - 1; z >= positionOfCurrentSpineNodeOnFringe; z--)
            {
                Node replacementNode = nextFringe.getAdjoinableNodes().get(z);
                //if (StringTreeAnalysis.matches(indexPattern,replacementNode.getDownIndex()) 
                //		&& !replacementNode.getLambda().contains("\t")){
                if (replacementNode.getCategory().equals(firstNode.getCategory()))
                {
                    Node replacementNode2 = firstNode.copy();
                    replacementNode2.setDownIndex(replacementNode.getDownIndex());
                    replacementNode2.setUpIndex(replacementNode.getUpIndex());
                    //replacementNode2.setDownPosTag(replacementNode.getDownPosTag());//same downPOStag as rest of spine.
                    replacementNode2.setNodeId(replacementNode.getNodeId());//necessary???
                    replacementNode2.setTimeStamp((short) replacementNode.getTimeStamp());//necessary???
                    replacementNode2.setLeftMostCover(replacementNode.getLeftMostCover());
                    adjNodes.add(removepos, replacementNode2);
                }
                else
                {
                    replacementNode.setDownPosTag(firstNode.getDownPosTag());
                    adjNodes.add(removepos, replacementNode);
                }
            }
            positionOfCurrentSpineNodeOnFringe--;
            return positionOfCurrentSpineNodeOnFringe;
        }

        @Override
        public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis prefTree, StringTree elemTree, int adjNodeNumber, TreeState targetTreeState)
        {
            StringTreeAnalysis newTree = prefTree.clone();
            StringTree elemTreeCopy = elemTree.copy();
            newTree.verify(elemTreeCopy, adjNodeNumber, targetTreeState);//, traceleft, false);//,lastPredictedNode.getCategory(), lastPredictedNode.getClusterNumber());
            return newTree;
        }
    },
    initial
    {

        @Override
        public List<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preftreetreeState, TreeState elem, ElementaryStringTree tree, int timestamp)
        {
            //don't use preftreefringe; 
            ArrayList<ChartEntry> states = new ArrayList<ChartEntry>();
            // There must not be any substitution nodes to the left of the anchor
            if (elem.getFringe().getSubstNode() == null && (!elem.isAux() || !elem.hasFootLeft()))
            {
                FreqCounter fc = new FreqCounter(opts);
                Fringe f = elem.getFringe();
                Fringe newcurrentfringe = elem.getFringeAfterLastNonShadowNonNullLexAnchor();
                while (f != newcurrentfringe)
                {
                    fc.accountForHistFringe(f, null, false);        // TODO: FIX (maybe not needed for parsing)
                    elem.shiftNextUnaccessibleToAccessible();
                    f = elem.getFringe();
                }
                TreeState ts = !opts.useSemantics ? 
                        new TreeState(elem.isAux(), elem.hasFootLeft(), elem.getWordCover(), newcurrentfringe, elem.getUaAfter(newcurrentfringe), null, elem.shadowListDeepCopy(), true, opts.nBest) :
                        new DepTreeState(elem.isAux(), elem.hasFootLeft(), elem.getWordCover(), newcurrentfringe, elem.getUaAfter(newcurrentfringe), null, elem.shadowListDeepCopy(), true, opts.nBest, 
                                ((DepTreeState)preftreetreeState).getDependencies(), parserModel);
                //		newcurrentfringe, elem.getNextUa(), elem.getFutureFringe(), elem.shadowListDeepCopy(), true);
                for (ShadowStringTree st : ts.getShadowTrees())
                {
                    st.setIntegLambda("SoS");
                    st.setIntegLambdaId((short)0);
                }
                ts.addLeftMostAnnot("SoS+" + ts.getFringe().getAdjNodesOpenRight().get(0).getCategory());
                if(opts.useSemantics)
                {
                    ((DepTreeState)ts).updateDependencies(tree, words, origPosTags, "I", timestamp);
                }
                ArrayList<TreeState> tss = ts.fixForTrace(words);
                NodeKernel k = new NodeKernel("", "NA", Short.MIN_VALUE);
                for (TreeState treeState : tss)
                {
                    BuildBlock bb = new BuildBlock(opts, this, -1, new Node(k, (byte) -1, (byte) -1, (short) 0, (short) 0, "NA", -1, null, null), -1, tree);
                    bb.setFreqCounter(fc);
                    bb.setWordProbAndTreeProb(parserModel, treeState.getFringe(), "SoS");
                    treeState.getFutureFringe().addCutOffLocation(bb, elem.getWordCover()[1]);
                    states.add(new ChartEntry(opts, treeState, bb));
                    fc.accountForHistFringe(treeState.getFringe(), null, true);
                }
            }
            return states;
        }
    };   

    protected String getPos(String[] words, TreeState preforig)
    {
        if (!preforig.getFringe().getAdjNodesOpenRight().isEmpty())
        {
            return preforig.getFringe().getAdjNodesOpenRight().get(0).getCategory();
        }
        String pos = words[preforig.getWordCover()[1]];
        return pos.substring(0, pos.indexOf(" "));
    }

    protected Node getNewRoot(Fringe newFringe,
                              ArrayList<Fringe> newUnaccessibles)
    {
        if (newUnaccessibles.size() > 0)
        {
            return newUnaccessibles.get(newUnaccessibles.size() - 1).getLastAdjNode();
        }
        return newFringe.getLastAdjNode();
    }

    public List<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preftreetreeState, TreeState elemtreetreeState, ArrayList<Byte> pattern,
                                         ElementaryStringTree tree, ShadowStringTree shadowtree, Map<Integer, Integer> coveredNodes, 
                                         Map<Short, Short> offsetNodeIdsOfShadowTree, int timestamp)
    {
        return new ArrayList<ChartEntry>();
    }
    
    public List<ChartEntry> combine(ParserModel parserModel, Options opts, String[] words, String[] origPosTags, TreeState preftreetreeState, TreeState elemtreetreeState, ElementaryStringTree tree, int timestamp)
    {
        return new ArrayList<ChartEntry>();
    }

    public StringTreeAnalysis integrate(Options opts, StringTreeAnalysis analysis, StringTree elemtree, int adjNodeNumber, TreeState targetTreeState)
    {
        return null;
    }

    /**
     * Adjoins two unaccessible fringes. If the last fringe slice of the first unaccessible set does not have a subst.
     * node last (I think it can not have one, unless a correct pJoin was done before to combine two other fringes),
     * the last fringe slice of the first P is prepended to the auxnodes of the first fringe slice from the second P.
    
    protected void pJoin(ArrayList<Fringe> lowerUa, ArrayList<Fringe> upperF) {
    if (upperF.size()==0) return;
    if (lowerUa.size()==0) 
    System.out.println("unexpected event at ParserOperation:pJoin");
    Fringe lastLower = lowerUa.get(lowerUa.size()-1);
    if (lastLower.getSubstNode()==null){
    Fringe firstOfUpper =upperF.remove(0); 
    fJoin(lastLower, firstOfUpper);
    }
    lowerUa.addAll(upperF);
    }
     */
    
    public static void pJoin(Fringe lowerUa, ArrayList<Fringe> upperF)
    {
        if (upperF.isEmpty())
        {
            return;
        }
        if (lowerUa.getSubstNode() == null)
        {
            Fringe firstOfUpper = upperF.remove(0);
            fJoin(lowerUa, firstOfUpper);
            /*if (upperF.isEmpty()) {
            lowerUa.setFringeConts(firstOfUpper.getFringeConts());
            lowerUa.copyProbs(firstOfUpper);
            }*/
        }
        upperF.add(0, lowerUa);
    }

    /*
     * combines the parts of two fringes after substitution and adjunction (backside of where the event happened).
     */
    protected static void fJoin(Fringe lowerFringe, Fringe upperFringe)
    {
        if (lowerFringe.getAdjNodesOpenLeft().isEmpty())
        {//substitution or adjunction with foot left
            if (!upperFringe.getAdjNodesOpenRight().isEmpty())
            {
                upperFringe.getAdjNodesOpenRight().remove(0);
            }
            else
            {
                System.out.println("did not expect this at ParserOperation:fjoin (1)");
            }
            //TODO fix upperFringe if same Cat and lambda @R.
            Node lowerlast = lowerFringe.getAdjNodesOpenRight().get(lowerFringe.getAdjNodesOpenRight().size() - 1);//root node of subst / adj tree in down operations
            //subst down: don't need to do anything about downPOStag because it's already correct since we keep initial tree's root node.
            for (int i = 0; i < upperFringe.getAdjNodesOpenRight().size(); i++)
            {
                Node n = upperFringe.getAdjNodesOpenRight().get(i);
                if (n.getCategory().equals(lowerlast.getCategory()) && n.getLambda().equals("@R"))
                {
                    //should only happen at verification time; will miss out on nodes where a node with different cat was on root to foot path. TODO
                    Node oldNode = upperFringe.getAdjNodesOpenRight().remove(i);
                    Node newNode = lowerlast.copy();
                    newNode.setDownIndex(oldNode.getDownIndex());
                    newNode.setUpIndex(oldNode.getUpIndex());
                    newNode.setNodeId(oldNode.getNodeId());
                    newNode.setTimeStamp((short) oldNode.getTimeStamp());
                    upperFringe.getAdjNodesOpenRight().add(i, newNode);
                }
                else
                {
                    break;
                }
            }
            lowerFringe.getAdjNodesOpenRight().addAll(upperFringe.getAdjNodesOpenRight());
            lowerFringe.setAdjNodesOpenLeft(upperFringe.getAdjNodesOpenLeft());
            lowerFringe.setSubstNode(upperFringe.getSubstNode());
        }
        else
        {//adjunction with foot right;
            if (!upperFringe.getAdjNodesOpenRight().isEmpty())
            {
                System.out.println("did not expect this at ParserOperation:fjoin (2)");
            }
            lowerFringe.getAdjNodesOpenLeft().remove(lowerFringe.getAdjNodesOpenLeft().size() - 1);
            if (upperFringe.getSubstNode() != null)
            {
                lowerFringe.setSubstNode(upperFringe.getSubstNode());
            }
            lowerFringe.getAdjNodesOpenLeft().addAll(upperFringe.getAdjNodesOpenLeft());
        }

    }

    @Override
    public String toString()
    {
        return this.name();
    }

    protected short makeUniqueIndices(TreeState elem, TreeState pref)
    {
        elem.makeUniqueShadowIndeces(pref.getShadowIndeces());
        return elem.makeUniqueNodeIndices(pref);

    }

    protected ArrayList<FringeAndProb> prependPrefFringes(
            ArrayList<Fringe> prefFringes, FringeAndProb futureFringe)
    {
        ArrayList<FringeAndProb> newlist = new ArrayList<FringeAndProb>();
        // prefFringes if just one fringe, pJoin with prefOrig.fringeAndProb.next
        if (prefFringes.size() == 1 && futureFringe.hasNext())
        {
            Fringe exp = prefFringes.get(0);
            for (FringeAndProb furtherexp : futureFringe.getNext())
            {
                ArrayList<Fringe> fep = furtherexp.getFringe();
                ArrayList<Fringe> newexp = TreeState.allFringeCopy(fep, null);
                ArrayList<FringeAndProb> fconts = furtherexp.getNext();//(ArrayList<ArrayList<Fringe>>) newexp.get(newexp.size()-1).getFringeContClone();//Clone TODO check whether clone necessary or not!
                LinkedList<LinkedList<BuildBlock>> cutofflist = furtherexp.getBBHist();
                ParserOperation.pJoin(exp.copy(), newexp);
                FringeAndProb combinedfexp = new FringeAndProb(newexp, furtherexp.getnBestProbs(), fconts, cutofflist);
                newlist.add(combinedfexp);
            }
        }
        else
        {
            //if more than one fringe, make separate layer of next.
            FringeAndProb newFAP = new FringeAndProb(prefFringes, futureFringe.getnBestProbs(),
                    futureFringe.getNext(), futureFringe.getBBHist());
            newlist.add(newFAP);
        }
        return newlist;
    }
    
    protected void setPrefixOffsetOfShadowTrees(List<ShadowStringTree> trees, short offset, short prefixIpiNode, short elemIpiNode)
    {
        for(ShadowStringTree tree : trees)
        {
            tree.setAbsolutePosOfShadowInPrefixTree(offset, prefixIpiNode, elemIpiNode);
        }
    }
}
