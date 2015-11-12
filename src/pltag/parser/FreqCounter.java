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

import java.util.ArrayList;
import java.util.List;

public class FreqCounter
{

    Options opts;
    
    private ArrayList<TreeProbElement> treeProbElList;
    private ArrayList<WordProbElement> wordProbElList;
//	private HashMap<String, Integer> noneMap;
    private ArrayList<SuperTagElement> fringeSuper;
    private ArrayList<SuperTagStructElement> structSuper;

    public FreqCounter(Options opts)
    {
        this.opts = opts;
        treeProbElList = new ArrayList<TreeProbElement>();
        wordProbElList = new ArrayList<WordProbElement>();
        //	noneMap = new HashMap<String, Integer>();
        fringeSuper = new ArrayList<SuperTagElement>();
        structSuper = new ArrayList<SuperTagStructElement>();
    }

    public FreqCounter(FreqCounter freqCounterIn)
    {
        opts = freqCounterIn.opts;
        treeProbElList = new ArrayList<TreeProbElement>(freqCounterIn.treeProbElList);        
        wordProbElList = new ArrayList<WordProbElement>(freqCounterIn.wordProbElList);        
        fringeSuper = (ArrayList<SuperTagElement>) freqCounterIn.fringeSuper.clone();
        structSuper = (ArrayList<SuperTagStructElement>) freqCounterIn.structSuper.clone();        
    }
    
    public void accountForHistFringe(Fringe histfringe, Node adjNode, boolean down)
    {
        boolean traceLeftSide = histfringe.hasTraceLeft();
        boolean traceRightSide = histfringe.hasTraceRight();
        histfringe.setClusterNumberCount(true);
        histfringe.setClusterNumberCount(false);
        for (Node hn : histfringe.getAdjNodesOpenRight())
        {
            if (hn == adjNode)
            {
                return;
            }
            if (hn.getUpIndex() == -1)
            {
                continue;// don't count root nodes. (instead, we account fro non-adj at foot nodes and subst nodes.)
            }
            addToNoneAdjList(hn, traceLeftSide, traceRightSide, down);
        }
        for (Node hn : histfringe.getAdjNodesOpenLeft())
        {
            if (hn == adjNode)
            {
                return;
            }
            if (hn.getUpIndex() == -1)
            {
                continue;// don't count root nodes. (instead, we account fro non-adj at foot nodes and subst nodes.)
            }
            addToNoneAdjList(hn, traceLeftSide, traceRightSide, down);
        }
        if (histfringe.getSubstNode() != null)
        {
            addToNoneAdjList(histfringe.getSubstNode(), traceLeftSide, traceRightSide, down);//note: keine features vom root node des initial trees der hierrein substituiert vorhanden.
        }
    }

    /*
     * when two trees are integrated, there are usually a few nodes on the fringe which are afterwards not accessible 
     * anymore because they are not on the current fringe any longer. We have to account for the propability of not adjoining
     * into these nodes.
     */
    public void addToNoneAdjList(Node node, boolean traceLeftSide, boolean traceRightSide, boolean down)
    {
        if(opts.countNoneAdj)
        {
            //down operation could have happened here.
            if (down)
            {
                TreeProbElement ntpe = new TreeProbElement(ParserOperation.adjoinDownXF.name(), node.getCategory(), "NONEADJ",
                        //node.getDownPosTag(), 
                        traceLeftSide, traceRightSide, node.getOrigTree(), node.getPosInTree(),
                        node.getLambda(), node.getLeftMostCover(), node.getClusterNumber());
                //System.out.println(ntpe);
                this.treeProbElList.add(ntpe);
                this.treeProbElList.add(new TreeProbElement(ParserOperation.adjoinDownXS.name(), node.getCategory(), "NONEADJ",
                        //node.getDownPosTag(), 
                        traceLeftSide, traceRightSide, node.getOrigTree(), node.getPosInTree(),
                        node.getLambda(), node.getLeftMostCover(), node.getClusterNumber()));
            }
            //up operation could have happened here.
            else
            {
                TreeProbElement ntpe = new TreeProbElement(ParserOperation.adjoinUpFF.name(), node.getCategory(), "NONEADJ",
                        //node.getDownPosTag(),
                        traceLeftSide, traceRightSide, node.getOrigTree(), node.getPosInTree(),
                        node.getLambda(), node.getLeftMostCover(), node.getClusterNumber());
                //System.out.println(ntpe);
                this.treeProbElList.add(ntpe);
                this.treeProbElList.add(new TreeProbElement(ParserOperation.adjoinUpFS.name(), node.getCategory(), "NONEADJ",
                        //node.getDownPosTag(), 
                        traceLeftSide, traceRightSide, node.getOrigTree(), node.getPosInTree(),
                        node.getLambda(), node.getLeftMostCover(), node.getClusterNumber()));
            }
        }        
    }

    public void addSingleTreeProbEl(TreeProbElement tpe)
    {
        this.treeProbElList.add(tpe);
    }

    public void expandTreeProbEl(TreeProbElement tpe)
    {
        treeProbElList.add(tpe);
        //System.out.println(tpe.toString());
        treeProbElList.add(tpe.sum());
        //System.out.println(tpe.sum().toString());
        //treeProbElList.add(tpe.getAttachProbEl());
        //treeProbElList.add(tpe.getAttachProbSumEl());
        while (tpe.hasBackoff())
        {
            tpe = tpe.getBackoff();
            treeProbElList.add(tpe);
            //System.out.println(tpe.toString());
            treeProbElList.add(tpe.sum());
            //System.out.println(tpe.sum().toString());
        }
    }

    /*public void addTreeProb(String operationName, String conditionalOn, String elementaryTree, String adjPos, 
    boolean traceLeftSide, boolean traceRightSide, String elementOrigTree, 
    int posInTree, String integlambda, String ipiLeftMost, String clusterNumber) {
    //full count
    TreeProbElement tpe = new TreeProbElement(operationName, conditionalOn, elementaryTree, adjPos, 
    traceLeftSide, traceRightSide, elementOrigTree, posInTree, integlambda, ipiLeftMost, clusterNumber);
    expandTreeProbEl(tpe);
    //System.out.println("t"+treeProbElList.size()+ " ");
    }*/
    /*protected void addNoneAdjs(Node node, boolean traceLeftSide, boolean traceRightSide) {
    addTreeProb(ParserOperation.adjoinDownXF.name(), node.getCategory(), "NONE", node.getDownPosTag(), traceLeftSide, traceRightSide, 
    node.getOrigTree(), node.getPosInTree(), node.getLambda(), node.getLeftMostCover(), node.getClusterNumber());
    addTreeProb(ParserOperation.adjoinDownXS.name(), node.getCategory(), "NONE", node.getDownPosTag(), traceLeftSide, traceRightSide, 
    node.getOrigTree(), node.getPosInTree(), node.getLambda(), node.getLeftMostCover(), node.getClusterNumber());
    }*/
    public void addStructProb(String struct, String fringe, String pos)
    {
        SuperTagStructElement stse = new SuperTagStructElement(struct, fringe, pos);
        structSuper.add(stse);
        structSuper.add(stse.sum());
        //	while(stse.hasBackoff()){
        //		stse = stse.getBackoff();
        //		structSuper.add(stse);
        //		structSuper.add(stse.sum());
        //	}
    }

    public void addFringeProb(Fringe prefixFringe, String nextPosCat, String currentPosCat, String predTreeFringe, String ipi)
    {
        //full count
        SuperTagElement ste = new SuperTagElement(prefixFringe, nextPosCat, currentPosCat, predTreeFringe);
        fringeSuper.add(ste);
        fringeSuper.add(ste.sum());
        while (ste.hasBackoff())
        {
            ste = ste.getBackoff(ipi);
            fringeSuper.add(ste);
            fringeSuper.add(ste.sum());
        }
    }
    
//    public FreqCounter clone(Options opts)
//    {
//        FreqCounter clone = new FreqCounter(opts);
//        clone.treeProbElList.addAll(this.treeProbElList);
//        clone.wordProbElList.addAll(this.wordProbElList);
//        clone.fringeSuper = (ArrayList<SuperTagElement>) this.fringeSuper.clone();
//        clone.structSuper = (ArrayList<SuperTagStructElement>) this.structSuper.clone();
//        return clone;
//    }

    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (TreeProbElement tpe : this.treeProbElList)
        {
            s.append(tpe.toString()).append("\n");
        }
        for (WordProbElement wpe : this.wordProbElList)
        {
            s.append(wpe.toString()).append("\n");
        }
        return s.toString();
    }

    public void addSingleWordProbEl(WordProbElement wpe)
    {
        wordProbElList.add(wpe);
    }

    public void expandWordProbEl(WordProbElement wpe)
    {
        wordProbElList.add(wpe);
        wordProbElList.add(wpe.sum());
        while (wpe.hasBackoff())
        {
            wpe = wpe.getBackoff();
            wordProbElList.add(wpe);
            wordProbElList.add(wpe.sum());
        }

    }
    /*
    public void addWordProb(String currentMainLeaf, String integrationPointLeaf, String elementTreeStruct) {
    WordProbElement wpe = new WordProbElement(currentMainLeaf, integrationPointLeaf, elementTreeStruct);
    addSingleWordProbEl(wpe);
    }*/

    public void join(FreqCounter freqCounterForCorrect)
    {

        treeProbElList.addAll(freqCounterForCorrect.treeProbElList);
        wordProbElList.addAll(freqCounterForCorrect.wordProbElList);
        structSuper.addAll(freqCounterForCorrect.structSuper);
        fringeSuper.addAll(freqCounterForCorrect.fringeSuper);
    }

    public void addFringeProbs(List<SuperTagElement> fp)
    {
        this.fringeSuper.addAll(fp);
    }

    public void addStructProbs(List<SuperTagStructElement> sp)
    {
        this.structSuper.addAll(sp);
    }

    public List<SuperTagElement> getFringeSuperProbs()
    {
        return this.fringeSuper;
    }

    public List<SuperTagStructElement> getStructSuperProbs()
    {
        return this.structSuper;
    }

    public List<TreeProbElement> getTreeProbs()
    {
        return this.treeProbElList;
    }

    public List<WordProbElement> getWordProbs()
    {
        return this.wordProbElList;
    }

    public boolean isEmpty()
    {
        return treeProbElList.isEmpty() || wordProbElList.isEmpty() || 
               fringeSuper.isEmpty() || structSuper.isEmpty();
    }
}
