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
package pltag.parser.semantics.discriminative.incrAnalyses;

import java.io.Serializable;
import java.util.BitSet;
import pltag.parser.semantics.conll.Proposition;
import pltag.parser.semantics.discriminative.DiscriminativeFeatureIndexers;
import pltag.parser.semantics.discriminative.DiscriminativeParams;
import pltag.parser.semantics.discriminative.ExtractFeatures;

/**
 *
 * @author sinantie
 */
public class IncrementalAnalysis implements Serializable
{

    private static final long serialVersionUID = 1L;
    
//    private final transient DiscriminativeFeatureIndexers featureIndexers;    
    int elemTree, prevElemTree, elemTreeBigram, elemTreeUnlex, prevElemTreeUnlex, 
            elemTreeUnlexBigram, integrationPoint; 
    int[] fringeOpenLeft, fringeOpenRight;
    byte[] fringeUpIndex, fringeDownIndex;
    int fringeSubstNode, numFringeNodes, numPredictFringeNodes;
    int rightBranchSpine, rightBranchRest, coPar, coLenPar, ipElemTree, ipElemTreeUnlex, wordL2, wordL3;
    int[] heavy, neighboursL1, neighboursL2;
    
    int[] srlTriples, srlTriplesPos;
    BitSet argComplete; // true if the SRL triple resulted by completing an argument, false if otherwise (i.e., completed a predicate)
    int[] srlIncompleteTriples, srlIncompleteTriplesPos;
    
    transient Proposition[] propositions;
    double baselineScore, baselineWordScore;//, semanticScore;

//    public IncrementalAnalysis(DiscriminativeFeatureIndexers featureIndexers)
//    {
//        this.featureIndexers = featureIndexers;
//    }
    public IncrementalAnalysis()
    {        
    }

    public void setElemTree(String elemTreeStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.elemTree = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_ELEM_TREE, elemTreeStr, train);
    }

    public int getElemTree()
    {
        return elemTree;
    }

    public void setPrevElemTree(String prevElemTreeStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.prevElemTree = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_PREV_ELEM_TREE, prevElemTreeStr, train);
    }

    public int getPrevElemTree()
    {
        return prevElemTree;
    }

    public void setElemTreeBigram(String elemTreeBigramStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.elemTreeBigram = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_ELEM_TREE_BIGRAM, elemTreeBigramStr, train);
    }

    public int getElemTreeBigram()
    {
        return elemTreeBigram;
    }

    public void setElemTreeUnlex(String elemTreeUnlexStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.elemTreeUnlex = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_ELEM_TREE_UNLEX, elemTreeUnlexStr, train);
    }

    public int getElemTreeUnlex()
    {
        return elemTreeUnlex;
    }

    public void setPrevElemTreeUnlex(String prevElemTreeUnlexStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.prevElemTreeUnlex = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX, prevElemTreeUnlexStr, train);
    }

    public int getPrevElemTreeUnlex()
    {
        return prevElemTreeUnlex;
    }

    public void setElemTreeUnlexBigram(String elemTreeUnlexBigramStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.elemTreeUnlexBigram = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM, elemTreeUnlexBigramStr, train);
    }

    public int getElemTreeUnlexBigram()
    {
        return elemTreeUnlexBigram;
    }

    public void setIntegrationPoint(String integrationPointStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.integrationPoint = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_INTEGRATION_POINT, integrationPointStr, train);
    }

    public int getIntegrationPoint()
    {
        return integrationPoint;
    }

    public void setFringeOpenLeft(int[] fringeOpenLeft)
    {
        this.fringeOpenLeft = fringeOpenLeft;
    }

    public int[] getFringeOpenLeft()
    {
        return fringeOpenLeft;
    }

    public void setFringeOpenRight(int[] fringeOpenRight)
    {
        this.fringeOpenRight = fringeOpenRight;
    }

    public int[] getFringeOpenRight()
    {
        return fringeOpenRight;
    }

    public void setFringeSubstNode(int fringeSubstNode)
    {
        this.fringeSubstNode = fringeSubstNode;
    }

    public int getFringeSubstNode()
    {
        return fringeSubstNode;
    }

    public void setFringeUpIndex(byte[] fringeUpIndex)
    {
        this.fringeUpIndex = fringeUpIndex;
    }

    public byte[] getFringeUpIndex()
    {
        return fringeUpIndex;
    }

    public void setFringeDownIndex(byte[] fringeDownIndex)
    {
        this.fringeDownIndex = fringeDownIndex;
    }

    public byte[] getFringeDownIndex()
    {
        return fringeDownIndex;
    }
    
    public void setNumFringeNodes(int numFringeNodes)
    {
        this.numFringeNodes = numFringeNodes;
    }

    public int getNumFringeNodes()
    {
        return numFringeNodes;
    }

    public void setNumPredictFringeNodes(int numPredictFringeNodes)
    {
        this.numPredictFringeNodes = numPredictFringeNodes;
    }

    public int getNumPredictFringeNodes()
    {
        return numPredictFringeNodes;
    }
    
    public int getRightBranchSpine()
    {
        return rightBranchSpine;
    }

    public void setRightBranchSpine(int rightBranchSpine)
    {
        this.rightBranchSpine = rightBranchSpine;
    }

    public int getRightBranchRest()
    {
        return rightBranchRest;
    }

    public void setRightBranchRest(int rightBranchRest)
    {
        this.rightBranchRest = rightBranchRest;
    }
       
    public void setCoPar(String coParStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.coPar = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_CO_PAR, coParStr, train);
    }

    public int getCoPar()
    {
        return coPar;
    }
        
    public void setCoLenPar(String coLenParStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.coLenPar = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_CO_LEN_PAR, coLenParStr, train);
    }

    public int getCoLenPar()
    {
        return coLenPar;
    }
    
    public void setHeavy(String[] heavyStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.heavy = new int[heavyStr.length];
        for(int i = 0; i < heavyStr.length; i++)
            heavy[i] = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_HEAVY, heavyStr[i], train);
    }

    public int[] getHeavy()
    {
        return heavy;
    }
    
    public void setNeighboursL1(String[] neighboursStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.neighboursL1 = new int[neighboursStr.length];
        for(int i = 0; i < neighboursStr.length; i++)
            neighboursL1[i] = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_NEIGHBOURS_L1, neighboursStr[i], train);
    }

    public int[] getNeighboursL1()
    {
        return neighboursL1;
    }
    
    public void setNeighboursL2(String[] neighboursStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.neighboursL2 = new int[neighboursStr.length];
        for(int i = 0; i < neighboursStr.length; i++)
            neighboursL2[i] = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_NEIGHBOURS_L2, neighboursStr[i], train);
    }

    public int[] getNeighboursL2()
    {
        return neighboursL2;
    }
    
    public void setIpElemTree(String ipElemTreeStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.ipElemTree = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_IP_ELEM_TREE, ipElemTreeStr, train);
    }

    public int getIpElemTree()
    {
        return ipElemTree;
    }

    public void setIpElemTreeUnlex(String ipElemTreeStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.ipElemTree = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX, ipElemTreeStr, train);
    }

    public int getIpElemTreeUnlex()
    {
        return ipElemTreeUnlex;
    }
    
    public void setWordL2(String wordL2Str, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.wordL2 = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_WORD_L2, wordL2Str, train);
    }

    public int getWordL2()
    {
        return wordL2;
    }
    
    public void setWordL3(String wordL3Str, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.wordL3 = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_WORD_L3, wordL3Str, train);
    }

    public int getWordL3()
    {
        return wordL3;
    }

    public void setSrlTriples(String[] srlTriplesStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.srlTriples = new int[srlTriplesStr.length];
        for(int i = 0; i < srlTriplesStr.length; i++)
            srlTriples[i] = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES, srlTriplesStr[i], train);
    }

    public int[] getSrlTriples()
    {
        return srlTriples;
    }

    public void setArgComplete(BitSet argComplete)
    {
        this.argComplete = argComplete;
    }

    public BitSet getArgComplete()
    {
        return argComplete;
    }
    
    public void setSrlTriplesPos(String[] srlTriplesPosStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.srlTriplesPos = new int[srlTriples.length];
        for(int i = 0; i < srlTriplesPosStr.length; i++)
            srlTriplesPos[i] = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES_POS, srlTriplesPosStr[i], train);
    }

    public int[] getSrlTriplesPos()
    {
        return srlTriplesPos;
    }

    public void setSrlIncompleteTriples(String[] srlIncompleteTriplesStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.srlIncompleteTriples = new int[srlIncompleteTriplesStr.length];
        for(int i = 0; i < srlIncompleteTriplesStr.length; i++)
            srlIncompleteTriples[i] = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES, srlIncompleteTriplesStr[i], train);
    }

    public int[] getSrlIncompleteTriples()
    {
        return srlIncompleteTriples;
    }
    
    public void setSrlIncompletePosTriples(String[] srlIncompleteTriplesPosStr, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        this.srlIncompleteTriplesPos = new int[srlIncompleteTriplesPosStr.length];
        for(int i = 0; i < srlIncompleteTriplesPosStr.length; i++)
            srlIncompleteTriplesPos[i] = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS, srlIncompleteTriplesPosStr[i], train);
    }

    public int[] getSrlIncompleteTriplesPos()
    {
        return srlIncompleteTriplesPos;
    }
        
    public void setPropositions(Proposition[] propositions)
    {
        this.propositions = propositions;
    }
    
    public void setBaselineScore(double baselineScore)
    {
        this.baselineScore = baselineScore;
    }

    public double getBaselineScore()
    {
        return baselineScore;
    }

    public void setBaselineWordScore(double baselineWordScore)
    {
        this.baselineWordScore = baselineWordScore;
    }

    public double getBaselineWordScore()
    {
        return baselineWordScore;
    }

//    public void setSemanticScore(double semanticScore)
//    {
//        this.semanticScore = semanticScore;
//    }
//
//    public double getSemanticScore()
//    {
//        return semanticScore;
//    }

    public String toString(DiscriminativeFeatureIndexers featureIndexers, DiscriminativeParams params)
    {
        StringBuilder str = new StringBuilder();
        str.append(baselineScore).append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_BASELINE_SCORE, 0)).append("\n").
            append(baselineWordScore).append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_BASELINE_WORD_SCORE, 0)).append("\n").
            append(toStringFeatureWeight(ExtractFeatures.FEAT_ELEM_TREE, elemTree, featureIndexers, params)).append("\n").
            append(toStringFeatureWeight(ExtractFeatures.FEAT_PREV_ELEM_TREE, prevElemTree, featureIndexers, params)).append("\n").
            append(toStringBigram(featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_ELEM_TREE_BIGRAM, elemTreeBigram), false, featureIndexers)).
                append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_ELEM_TREE_BIGRAM, elemTreeBigram)).append("\n").
            append(toStringFeatureWeight(ExtractFeatures.FEAT_ELEM_TREE_UNLEX, elemTreeUnlex, featureIndexers, params)).append("\n").
            append(toStringFeatureWeight(ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX, prevElemTreeUnlex, featureIndexers, params)).append("\n").
            append(toStringBigram(featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM, elemTreeUnlexBigram), true, featureIndexers)).
                append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM, elemTreeUnlexBigram)).append("\n").
            append(toStringFeatureWeight(ExtractFeatures.FEAT_INTEGRATION_POINT, integrationPoint, featureIndexers, params)).append("\n").
            append(toStringIpTree(featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_IP_ELEM_TREE, ipElemTree), false, featureIndexers)).append("\n").
                append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_IP_ELEM_TREE, ipElemTree)).append("\n").
            append(toStringIpTree(featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_IP_ELEM_TREE, ipElemTree), true, featureIndexers)).append("\n").
                append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX, ipElemTreeUnlex)).append("\n");
            for(Integer srlTriple : srlTriples)
                str.append(toStringSrlTriple(featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES, srlTriple), false, featureIndexers)).
                        append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_SRL_TRIPLES, srlTriple)).append(", ");
            str.append("\n");
            for(Integer srlTriplePos : srlTriplesPos)
                str.append(toStringSrlTriple(featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES_POS, srlTriplePos), true, featureIndexers)).
                        append("\t:\t").append(params.getParamVecWeight(ExtractFeatures.FEAT_SRL_TRIPLES_POS, srlTriplePos)).append(", ");
//            str.append("\n").append(semanticScore);
        return str.toString();
    }
    
    private String toStringFeatureWeight(int vecId, int featureId, DiscriminativeFeatureIndexers featureIndexers, DiscriminativeParams params)
    {
        return String.format("%s\t:\t%s", featureIndexers.getValueOfFeature(vecId, featureId), params.getParamVecWeight(vecId, featureId));
    }
    
    /**
     * Bigrams encode indices of trees, rather than the String representations themselves, i.e., 
     * "3 56", denotes the prevElemTree indexed at position 3, and the elemTree at position 56 of
     * their corresponding indexers.
     * @param input
     * @param unlex
     * @return 
     */
    private String toStringBigram(String input, boolean unlex, DiscriminativeFeatureIndexers featureIndexers)
    {
        if(input.equals("0") || input.equals("U"))
            return "U";
        StringBuilder str = new StringBuilder();
        String[] inputAr = input.split(" ");    
        if(inputAr[0].equals("U"))
            str.append(inputAr[0]);
        else
        {
            int prevId = Integer.valueOf(inputAr[0]);
            str.append(unlex ? featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX, prevId) : 
                               featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_PREV_ELEM_TREE, prevId));
        }        
        str.append(" ");
        if(inputAr[1].equals("U"))
        {
            str.append(inputAr[1]);
        }
        else
        {
            int curId = Integer.valueOf(inputAr[1]);
            str.append(unlex ? featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_ELEM_TREE_UNLEX, curId) : 
                               featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_ELEM_TREE, curId));
        }        
        return str.toString();
    }
    
    private String toStringIpTree(String input, boolean unlex, DiscriminativeFeatureIndexers featureIndexers)
    {
        if(input.equals("0") || input.equals("U"))
            return "U";
        StringBuilder str = new StringBuilder();
        String[] inputAr = input.split(",");    
        if(inputAr[0].equals("U"))
            str.append(inputAr[0]);
        else
        {
            int ipId = Integer.valueOf(inputAr[0]);
            str.append(featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_INTEGRATION_POINT, ipId));
        }        
        str.append(" ");
        if(inputAr[1].equals("U"))
        {
            str.append(inputAr[1]);
        }
        else
        {
            int curId = Integer.valueOf(inputAr[1]);
            str.append(unlex ? featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_ELEM_TREE_UNLEX, curId) : 
                               featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_ELEM_TREE, curId));
        }        
        return str.toString();
    }
    
    /**
     * Input srl triple is encoded with ids, to save space, in the format role_id, arg_id, pred_id. 
     * @param input
     * @param unlex
     * @return 
     */
    private String toStringSrlTriple(String input, boolean unlex, DiscriminativeFeatureIndexers featureIndexers)
    {
        if(input.equals("0"))
            return "U";
        String[] ar = input.split(",");
        if(!unlex)
            return String.format("<%s,%s,%s>", featureIndexers.getRole(Integer.valueOf(ar[0])), featureIndexers.getWord(Integer.valueOf(ar[1])), featureIndexers.getWord(Integer.valueOf(ar[2])));
        else
            return String.format("<%s,%s,%s>", featureIndexers.getRole(Integer.valueOf(ar[0])), featureIndexers.getPos(Integer.valueOf(ar[1])), featureIndexers.getPos(Integer.valueOf(ar[2])));
    }    
    
    public String[] srlTriplesFull(DiscriminativeFeatureIndexers featureIndexers)
    {
        String[] out = new String[srlTriples.length];
        for(int i  = 0; i < srlTriples.length; i++)
        {
            String srlTriple = featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES, srlTriples[i]);
            if(srlTriple.equals("0"))
            {
                out[i] = "U";
            }
            else
            {
                String[] srlTripleAr = srlTriple.split(",");
                String[] srlTriplePosAr = featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES_POS, srlTriplesPos[i]).split(",");
                out[i] = String.format("<%s,%s/%s,%s/%s>:%s", 
                        featureIndexers.getRole(Integer.valueOf(srlTripleAr[0])), // role
                        featureIndexers.getWord(Integer.valueOf(srlTripleAr[1])), featureIndexers.getPos(Integer.valueOf(srlTriplePosAr[1])), // arg
                        featureIndexers.getWord(Integer.valueOf(srlTripleAr[2])), featureIndexers.getPos(Integer.valueOf(srlTriplePosAr[2])),  // pred
                        argComplete.get(i));
            }            
        }
        return out;              
    }
}
