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

import fig.basic.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.map.MultiValueMap;


import pltag.corpus.ElementaryStringTree;
import pltag.corpus.IdGenerator;
import pltag.corpus.TagNodeType;
import pltag.corpus.StringTree;
import pltag.parser.params.PltagParams;

/**
 * 
 * BuildBlock holds the information about how an elementary tree was integrated into the structure.
 * 
 */
public class BuildBlock implements Comparable<BuildBlock>
{

    private Options opts;
    private ChartEntry prevChartEntryPointer;
    private ElementaryStringTree elemtree;
    private ParserOperation operation;
    private int adjNodeNumber;
    private MultiValueMap<String, StringTreeAnalysis> analyses;//<String,StringTreeAnalysis>
    private Node ipi;
    private double probability = 0.0;
    private String adjPosition = "";
//	private StringTree verifiedTree;
    private String unlexVerifiedTreeStruct;
    private boolean shadowLeafStatus;
    private String elemtreeString = "";
    private double elemtreeprob;
    private String[] verifiedNodes;//int noOfVerifiedNodes;
    private double verifiedTreeProb;
    private FreqCounter freqCounter;
//	private ArrayList<WordProbElement> wpelist;

    public BuildBlock(Options opts, ParserOperation operation, int adjnodenumber, Node ipi, int numberOfAdjSites, ElementaryStringTree tree)
    {//, ArrayList<BuildBlock> fch){
        this.opts = opts;
        this.operation = operation;
        this.adjNodeNumber = adjnodenumber;
        this.ipi = ipi;
        this.adjPosition = adjnodenumber + "/" + numberOfAdjSites;
        this.freqCounter = new FreqCounter(opts);
        this.elemtree = tree;
        this.elemtreeString = new StringBuilder()//.append(tree.getProbability()).append("\t")
                .append(tree.getStructureWithHeadInfo(tree.getRoot())).toString();
        this.elemtreeprob = tree.getProbability();
        calculateShadowLeafStatus();
    }

    public BuildBlock(Options opts, ParserOperation operation, int adjnodenumber, Node ipi, int numberOfAdjSites, String clusterNo, ElementaryStringTree tree)
    {//, ArrayList<BuildBlock> fch){
        this.opts = opts;
        this.operation = operation;
        this.adjNodeNumber = adjnodenumber;
        this.ipi = ipi;
        this.adjPosition = adjnodenumber + "/" + numberOfAdjSites;
        this.ipi.setClusterNumber(clusterNo);
        this.freqCounter = new FreqCounter(opts);
        this.elemtree = tree;
        this.elemtreeString = new StringBuilder()//.append(tree.getProbability()).append("\t")
                .append(tree.getStructureWithHeadInfo(tree.getRoot())).toString();
        this.elemtreeprob = tree.getProbability();
        calculateShadowLeafStatus();
    }

    private BuildBlock(Options opts, ParserOperation operation, int adjnodenumber, Node ipi, String adjPosition, FreqCounter fc, ElementaryStringTree elemtree, double elemtreeprob,
            String elemtreeString, boolean shadowLeafStatus2, String unlexVerifiedTreeStruct2, String[] verifiedNodes2, double verifiedTreeProb2,
            ChartEntry prevChartEntryPointer2)
    {
        this.opts = opts;
        this.operation = operation;
        this.adjNodeNumber = adjnodenumber;
        this.ipi = ipi;
        this.adjPosition = adjPosition;
        this.freqCounter = fc;
        this.elemtree = elemtree;
        this.elemtreeprob = elemtreeprob;
        this.elemtreeString = elemtreeString;
        this.shadowLeafStatus = shadowLeafStatus2;
        this.unlexVerifiedTreeStruct = unlexVerifiedTreeStruct2;
        this.verifiedNodes = verifiedNodes2;
        this.verifiedTreeProb = verifiedTreeProb2;
        this.prevChartEntryPointer = prevChartEntryPointer2;
    }

    public BuildBlock clone()
    {
        FreqCounter fcclone = null;
        if (this.freqCounter != null)
        {
            fcclone = new FreqCounter(freqCounter);
        }
        BuildBlock clone = new BuildBlock(this.opts, this.operation, this.adjNodeNumber, this.ipi, this.adjPosition, fcclone, this.elemtree, this.elemtreeprob,
                this.elemtreeString, this.shadowLeafStatus, this.unlexVerifiedTreeStruct, this.verifiedNodes, this.verifiedTreeProb,
                this.prevChartEntryPointer);
        return clone;
    }

    /*
     public BuildBlock(Integer[] ptsi, ElementaryStringTree tree, ParserOperation operation, int adjnodenumber, Node ipi, int numberOfAdjSites){
     this.preftreestateIndex = ptsi;
     this.elemtree = tree;
     this.elemtreeString = tree.getProbability()+"\t"+tree.getStructureWithHeadInfo(tree.getRoot());
     calculateShadowLeafStatus();
     this.operation = operation;
     this.adjNodeNumber = adjnodenumber;
     this.ipi = ipi;
     this.adjPosition = adjnodenumber +"/" + numberOfAdjSites;
     }*/
    /*public void setElemTree(ElementaryStringTree tree){
     this.elemtree = tree;
     this.elemtreeString = new StringBuilder()//.append(tree.getProbability()).append("\t")
     .append(tree.getStructureWithHeadInfo(tree.getRoot())).toString();
     this.elemtreeprob = tree.getProbability();
     calculateShadowLeafStatus();
     }*/
    /*
     public void setTSIndex(Integer wordIndex, Integer tsIndex){
     this.preftreestateIndex = new Integer[2];
     this.preftreestateIndex[0]=wordIndex;
     this.preftreestateIndex[1]=tsIndex;		
     }
	
	
     public Integer[] getPrefTreeState(){
     return this.preftreestateIndex;
     }*/
    public void setPrevChartEntry(ChartEntry ce)
    {
        this.prevChartEntryPointer = ce;
        ce.setUsed();
    }

    public ChartEntry getPrevChartEntry()
    {
        return this.prevChartEntryPointer;
    }

    public String getElemTreeString()
    {
        return elemtreeString;
    }

    public StringTree getElemTree()
    {
        if (elemtree == null)
        {
            IdGenerator idgen = new IdGenerator();
            ElementaryStringTree tree = Lexicon.convertToTree(new ElementaryStringTree(elemtreeprob + "\t" + elemtreeString, opts.useSemantics), idgen);
            tree.setTreeString(elemtreeString);
            return tree;
        }
        return elemtree;
    }

    public ParserOperation getOperation()
    {
        return operation;
    }

    public int getAdjNodeNumber()
    {
        return this.adjNodeNumber;
    }
    /*
     public HashMap<Integer, Integer> getIndexChange(){
     return indexChange;
     }*/

    public String toString()
    {
        return new StringBuilder()//.append(this.preftreestateIndex[0])
                //.append(",").append(this.preftreestateIndex[1])
                .append(" timestamp:").append(ipi.getTimeStamp())//": (adjnn ").append(getAdjNodeNumber()).append(") "
                .append(" ").append(this.operation.name())
                .append(" ").append(this.adjNodeNumber)
                //.append( "\tlogP: ").append( String.valueOf(probability))//.substring(0, 6) )
                .append(" ").append(elemtreeString).toString();
    }

    public void addAnalysis(String id, StringTreeAnalysis stringTree)
    {
        if (analyses == null)
        {
            analyses = new MultiValueMap<String, StringTreeAnalysis>();//<String, StringTreeAnalysis>
        }
        analyses.put(id, stringTree);
    }

    public boolean hasAnalysis(String id)
    {
//		if (analyses == null || analyses.isEmpty() || !analyses.containsKey(id)) return false;
//		return true;
        return !(analyses == null || analyses.isEmpty() || !analyses.containsKey(id));
    }

//	public Collection<StringTreeAnalysis> getAnalyses(){
//		if (analyses == null ) return new ArrayList<StringTreeAnalysis>();
//		return analyses.values();
//	}
    public Collection<StringTreeAnalysis> getAnalyses(String id)
    {
        return analyses == null ? new ArrayList<StringTreeAnalysis>() : analyses.getCollection(id);
    }

    public StringTreeAnalysis getFirstAnalysis()
    {
        return analyses == null ? null : (StringTreeAnalysis)analyses.values().iterator().next();
    }
    
    public StringTreeAnalysis getAnalysis(double prob)
    {
        if(analyses == null)
            return null;
        Iterator it = analyses.values().iterator();
        StringTreeAnalysis res = null;
        while(it.hasNext())
        {
            res = (StringTreeAnalysis)it.next();
            if(Math.abs(res.getProbability() - prob) < 0.00000001)
                return res;
        }
//        for(int i = 0; i <= pos; i++)
//        {
//            res = (StringTreeAnalysis)it.next();
//        }
        return res;
    }
    
    public Collection<Pair<StringTreeAnalysis, BuildBlock>> getAnalyses()
    {        
        List<Pair<StringTreeAnalysis, BuildBlock>> list = new ArrayList();
        if(!(analyses == null || analyses.isEmpty()))
        {
            for(Object o : analyses.values())
                list.add(new Pair((StringTreeAnalysis)o, this));
        }        
        return list;
    }
    
    public Node getIpi()
    {
        return ipi;
    }

    public void retrieveProbability(PltagParams params, boolean freqBaseline)
    {
        //System.out.println("\n"+this);
        if (freqBaseline)
        {
            this.probability = getFrequency();
        }
        else
        {
            double buildBlockprobability = 0;
            for (WordProbElement wpe : this.freqCounter.getWordProbs())
            {
                double wordProb = wpe.getSmoothedProb(params.getFreqMapWord());
                //System.out.println("w:"+wordProb+"\t"+wpe);
                buildBlockprobability += Math.log(wordProb);
            }
            for (TreeProbElement tpe : this.freqCounter.getTreeProbs())
            {
                double treeProb = tpe.getSmoothedProb(params.getFreqMapTree());
                //if (!tpe.toString().contains("NONEADJ"))System.out.println("t:"+treeProb+"\t"+tpe);
                buildBlockprobability += Math.log(treeProb);
            }
            this.probability = buildBlockprobability;
        }
    }

    /*public double updateProbability(FreqCounter fc) {
     double buildBlockprobability = 0;
     if (ParserModel.freqbaseline){
     this.probability = getFrequency();
     }
     else{
     for (WordProbElement wpe : fc.getWordProbs()){
     double wordProb = wpe.getSmoothedProb(ParserModel.bigFreqMapWord);
     //System.out.println("wordProb: "+wordProb+"\t"+wpe);
     buildBlockprobability += Math.log(wordProb);
     }
     for (TreeProbElement tpe: fc.getTreeProbs()){
     double treeProb = tpe.getSmoothedProb(ParserModel.bigFreqMapTree);
     //System.out.println("treeProb: "+treeProb+"\t"+tpe);
     buildBlockprobability += Math.log(treeProb);
     }
			
     this.probability += buildBlockprobability;// + lastTS; // because log probs
     //this.elemtree.setProbability(buildBlockprobability);//TODO: WHY? for shadow tree integration?
     //if (ParserModel.train) 
     getFreqCounter().join(fc);
     }
     return buildBlockprobability;
     }*/
    public boolean hasNonShadowLeaf()
    {
        return shadowLeafStatus;
    }

    /**
     *
     */
    private void calculateShadowLeafStatus()
    {
        ElementaryStringTree analysis = elemtree;
        int rootId = analysis.getRoot();
//		int rootId = Integer.parseInt(analysis.getRoot());
        byte downIndex = analysis.getLowerIndex(rootId);
        byte upIndex = analysis.getUpperIndex(rootId);
        if (!((downIndex == -1 && upIndex == -1) || upIndex == 0 || downIndex == 0))
        {
            if (((ElementaryStringTree) analysis).getAnchor() != Integer.MIN_VALUE
                    //			if (!((ElementaryStringTree)analysis).getAnchor().equals("") 
                    && analysis.getNodeType(((ElementaryStringTree) analysis).getAnchor()) == TagNodeType.anchor
                    && !TreeState.isNullLexeme(analysis.getCategory(((ElementaryStringTree) analysis).getAnchor())))
            {
                shadowLeafStatus = true;
            }
            else
            {
                shadowLeafStatus = false;
            }
        }
        else
        {

            if (!analysis.hasShadowInd())
            {//getShadowSourceTreesRootList().isEmpty()){
                shadowLeafStatus = true;
            }
            else
            {
                shadowLeafStatus = true;
            }
        }
    }

    public double getProbability()
    {
        return probability;
    }
    /*
     public void setVerifiedNodes(ArrayList<Node> verifiedNodes) {
     this.verifiedNodes = verifiedNodes;
     }
	
     public ArrayList<Node> getVerifiedNodes(){
     return this.verifiedNodes;
     }
     */

    public void setVerifiedTree(ElementaryStringTree verifiedPredTree, double prob, Set<Integer> verifiedNodes)
    {
        this.unlexVerifiedTreeStruct = verifiedPredTree.getUnlexStruct(verifiedPredTree.getRoot());
        if (opts.estimateProcDifficulty && this.unlexVerifiedTreeStruct.contains("0_0") && StringTreeAnalysis.predMarker.matcher(this.unlexVerifiedTreeStruct).matches())
        {
            //System.out.println("calculate prob for predicted subpart of structure (getVerifiedTreeProb)"+elemtreeString);
            this.verifiedTreeProb = Math.log(Lexicon.lexWord(verifiedPredTree.getCategory(verifiedPredTree.getAnchor()), verifiedPredTree.getPredictedLex()));
        }
        else
        {
            this.verifiedTreeProb = prob;
        }

        this.verifiedNodes = new String[verifiedNodes.size()];
        int j = 0;
        for (Integer i : verifiedNodes)
        {
            this.verifiedNodes[j] = verifiedPredTree.getCategory(i);
            j++;
        }
    }

    public double getVerifiedTreeProb()
    {

        return this.verifiedTreeProb;
    }

    public String getVerifiedTreeStruct()
    {
        return this.unlexVerifiedTreeStruct;
    }

    /*public StringTree getVerifiedTree(){
     return this.verifiedTree;
     }*/
    public String getAdjPos()
    {
        return this.adjPosition;
    }

    public void removeElemTree()
    {
        this.elemtree = null;
    }

    public int getNoOfVerifiedNodes()
    {
        return this.verifiedNodes.length;//.noOfVerifiedNodes;
    }

    public String getVerifiedNodeString()
    {
        StringBuilder result = new StringBuilder();
        for (String v : verifiedNodes)
        {
            if (v.equals(":") || TreeState.isNullLexeme(v))
            {
                continue;
            }
            result.append(v).append(" ");
        }
        return result.toString();
    }

    public HashSet<String> getVerifiedNodeList()
    {
        HashSet<String> result = new HashSet<String>();
        for (String v : verifiedNodes)
        {
            if (v.equals(":") || TreeState.isNullLexeme(v))
            {
                continue;
            }
            result.add(v);
        }
        return result;
    }
    /*public void setNumberOfVerifiedNodes(int noOfVerifiedNodes) {
     this.noOfVerifiedNodes = noOfVerifiedNodes;
     }*/

    public Integer getWIndex()
    {
        int index = this.prevChartEntryPointer.getTreeState().getWordCover()[1];
        if (!operation.toString().endsWith("S"))
        {
            index++;
        }
        return index;
    }

    public void saveSpace()
    {
        elemtree = null;
    }

    public double getFrequency()
    {
        return elemtree.getProbability();
    }

    public void setFreqCounter(FreqCounter fc)
    {
        if (!this.freqCounter.isEmpty())
        {
            System.err.println("FreqCounter will be overwritten.");
        }
        this.freqCounter = new FreqCounter(fc);
    }

    public FreqCounter getFreqCounter()
    {
        return this.freqCounter;
    }

    public void setWordProbAndTreeProb(ParserModel parserModel, Fringe lastFringe, String integrationLambda)
    {
        String conditionalOn;
        String direc = "";
        boolean traceAtLeft = lastFringe.hasTraceLeft();
        boolean traceAtRight = false;
        if (!lastFringe.isEmpty() && lastFringe.hasTraceRight())
        {
            traceAtRight = true;
        }
        if (operation == ParserOperation.initial)
        {
            conditionalOn = "SoS";
            if (elemtree.isAuxtree())
            {
                direc += elemtree.hasFootLeft();
            }
        }
        else if (operation == ParserOperation.verify)
        {
            conditionalOn = this.unlexVerifiedTreeStruct;// this.getVerifiedTree().getUnlexStruct(this.getVerifiedTree().getRoot());
        }
        else
        {
            conditionalOn = ipi.getCategory();
            if (elemtree.isAuxtree())
            {
                direc += elemtree.hasFootLeft();
            }
        }
        int elemroot = elemtree.getRoot();
        String elemunlex = elemtree.getUnlexStruct(elemroot);
        String currentLambda = elemtree.getMainLeaf(elemroot);
        //if (integrationLambda.equals("@R")) System.out.print("\n"+currentLambda+"\t"+integrationLambda+"\t"+this);
        String downpos = "-";
        if (ipi.getDownPosTag() != null && operation.name().startsWith("adjoinDown"))
        {
            downpos = ipi.getDownPosTag();
        }
        TreeProbElement thisTreeProb = new TreeProbElement(operation.name(), conditionalOn, elemunlex, //downpos, 
                traceAtLeft, traceAtRight, ipi.getOrigTree(), ipi.getPosInTree(), integrationLambda, ipi.getLeftMostCover(), ipi.getClusterNumber());
        this.freqCounter.addSingleTreeProbEl(thisTreeProb);
//		System.out.print("\n"+operation.name()+": "+currentLambda+"\t"+integrationLambda+"\t'"+downpos+"'"+this);
        this.freqCounter.addSingleWordProbEl(new WordProbElement(parserModel, currentLambda, integrationLambda, elemunlex));
    }

    public void removeFreqCounter()
    {
        this.freqCounter = null;
    }

    @Override
    public int compareTo(BuildBlock o)
    {
        return (int)o.getProbability() - (int)this.getProbability(); 
    }
}
