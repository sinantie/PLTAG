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
package pltag.parser.semantics.discriminative;

import fig.basic.Fmt;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import pltag.parser.Options;
import pltag.parser.params.AParams;
import pltag.parser.params.Vec;
import pltag.parser.params.VecFactory;
import pltag.parser.params.VecFactory.Type;

/**
 *
 * @author sinantie
 */
public class DiscriminativeParams extends AParams
{
    
    private final Options opts;    
    public Vec baselineWeight, baselineWordWeight, semanticWeight, elemTreeWeights, prevElemTreeWeights, 
            elemTreeUnlexWeights, prevElemTreeUnlexWeights, elemTreeBigramWeights, 
            elemTreeUnlexBigramWeights, integrationPointWeights;
    public Vec fringeOpenRightWeights, fringeOpenLeftWeights, fringeSubstNodeWeight, fringeNumNodesWeight, fringeNumPredictNodesWeight;
    public Vec fringeCategoryCountsWeights;
    public Vec[] semanticVecWeights;
    public Vec srlTriplesWeights, srlTriplesPosWeights, srlIncompleteTriplesWeights, srlIncompleteTriplesPosWeights,
            srlRoleWeights, srlDependencyWeights, srlDependencyPosWeights, 
            srlRolePredWeights, srlRolePredPosWeights, srlRoleArgWeights, srlRoleArgPosWeights,
            srlPredWeights, srlPredPosWeights, srlArgWeights, srlArgPosWeights,
            srlFrameWeights, srlFramePosWeights;
    public Vec rightBranchSpineWeight, rightBranchRestWeight, coParWeights, coLenParWeights, heavyWeights, neighboursL1Weights,
            neighboursL2Weights, ipElemTreeWeights, ipElemTreeUnlexWeights, wordL2Weights, wordL3Weights, wordProjWeights;
    private final DiscriminativeFeatureIndexers featureIndexers;
    
    public DiscriminativeParams(Options opts, DiscriminativeFeatureIndexers featureIndexers, VecFactory.Type vectorType)
    {
        super();
        this.opts = opts;
        this.featureIndexers = featureIndexers;
        genParams(featureIndexers, vectorType);
    }

    private void genParams(DiscriminativeFeatureIndexers featureIndexers, VecFactory.Type vectorType)
    {
        baselineWeight = VecFactory.zeros(vectorType, 1);
        addVec("baselineScore", baselineWeight);
        baselineWordWeight = VecFactory.zeros(vectorType, 1);
        addVec("baselineWordScore", baselineWordWeight);
        semanticWeight = VecFactory.zeros(vectorType, 1);
        addVec("semanticScoreWeights", semanticWeight);
        
        int semSize = ExtractFeatures.NUM_SEMANTIC_VEC_SIZE + (opts.semanticsType == Options.SemanticsType.all ? 2 : 0);
        semanticVecWeights = VecFactory.zeros2(vectorType, semSize, 1);
        String[] labels = new String[semSize];
        for(int i  = 0; i < semSize; i++)
            labels[i] = String.valueOf(i);
        addVec(getLabels(semSize, "semanticScoreVecWeights", labels), semanticVecWeights);
        
        elemTreeWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_ELEM_TREE));
        addVec("elemTreeWeights", elemTreeWeights);
        prevElemTreeWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_PREV_ELEM_TREE));
        addVec("prevElemTreeWeights", prevElemTreeWeights);
        elemTreeUnlexWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_ELEM_TREE_UNLEX));
        addVec("elemTreeUnlexWeights", elemTreeUnlexWeights);
        prevElemTreeUnlexWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX));
        addVec("prevElemTreeUnlexWeights", prevElemTreeUnlexWeights);
        elemTreeBigramWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_ELEM_TREE_BIGRAM));
        addVec("elemTreeBigramWeights", elemTreeBigramWeights);
        elemTreeUnlexBigramWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM));
        addVec("elemTreeUnlexBigramWeights", elemTreeUnlexBigramWeights);
        integrationPointWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_INTEGRATION_POINT));
        addVec("integrationPointWeights", integrationPointWeights);
        
        fringeOpenRightWeights = VecFactory.zeros(Type.MAP, -1);
        addVec("fringeOpenRightWeights", fringeOpenRightWeights);
        fringeOpenLeftWeights = VecFactory.zeros(Type.MAP, -1);
        addVec("fringeOpenLeftWeights", fringeOpenLeftWeights);        
        fringeSubstNodeWeight = VecFactory.zeros(vectorType, featureIndexers.getNumOfCategories());
        addVec("fringeSubstNodeWeights", fringeSubstNodeWeight);        
        fringeNumNodesWeight = VecFactory.zeros(vectorType, 1);
        addVec("fringeNumNodesWeight", fringeNumNodesWeight);
        fringeNumPredictNodesWeight = VecFactory.zeros(vectorType, 1);
        addVec("fringeNumPredictNodesWeight", fringeNumPredictNodesWeight);
        fringeCategoryCountsWeights = VecFactory.zeros(vectorType, featureIndexers.getNumOfCategories());
        addVec("fringeCategoryCountsWeights", fringeCategoryCountsWeights);
        
        srlTriplesWeights = opts.useLemmatisedSrlFeatures ? VecFactory.zeros(Type.MAP, -1) : 
                VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_SRL_TRIPLES));
        addVec("srlTripleWeights", srlTriplesWeights);
        srlTriplesPosWeights = opts.useLemmatisedSrlFeatures ? VecFactory.zeros(Type.MAP, -1) : 
                VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_SRL_TRIPLES_POS));
        addVec("srlTriplePosWeights", srlTriplesPosWeights);
        
//        if(opts.useSrlExtendedFeatures)
        {
            srlIncompleteTriplesWeights = opts.useLemmatisedSrlFeatures ? VecFactory.zeros(Type.MAP, -1) :
                    VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES));
            addVec("srlIncompleteTriplesWeights", srlIncompleteTriplesWeights);
            srlIncompleteTriplesPosWeights = opts.useLemmatisedSrlFeatures ? VecFactory.zeros(Type.MAP, -1) : 
                    VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS));
            addVec("srlIncompleteTriplesPosWeights", srlIncompleteTriplesPosWeights);
            srlRoleWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlRoleWeights", srlRoleWeights);
            srlDependencyWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlDependencyWeights", srlDependencyWeights);
            srlDependencyPosWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlDependencyPosWeights", srlDependencyPosWeights);
            srlRolePredWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlRolePredWeights", srlRolePredWeights);
            srlRolePredPosWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlRolePredPosWeights", srlRolePredPosWeights);
            srlRoleArgWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlRoleArgWeights", srlRoleArgWeights);
            srlRoleArgPosWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlRoleArgPosWeights", srlRoleArgPosWeights);
            srlPredWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlPredWeights", srlPredWeights);
            srlPredPosWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlPredPosWeights", srlPredPosWeights);
            srlArgWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlArgWeights", srlArgWeights);
            srlArgPosWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlArgPosWeights", srlArgPosWeights);
            
            srlFrameWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlFrameWeights", srlFrameWeights);
            srlFramePosWeights = VecFactory.zeros(Type.MAP, -1);
            addVec("srlFramePosWeights", srlFramePosWeights);

        }
        
        // global syntactic features
        if(opts.useGlobalSyntacticFeatures)
        {
            rightBranchSpineWeight = VecFactory.zeros(vectorType, 1);
            addVec("rightBranchBoneWeight", rightBranchSpineWeight);
            rightBranchRestWeight = VecFactory.zeros(vectorType, 1);
            addVec("rightBranchRestWeight", rightBranchSpineWeight);
            coParWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_CO_PAR));
            addVec("coParWeights", coParWeights);
            coLenParWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_CO_LEN_PAR));        
            addVec("coLenParWeights", coLenParWeights);
            heavyWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_HEAVY));        
            addVec("heavyWeights", heavyWeights);
            neighboursL1Weights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_NEIGHBOURS_L1));        
            addVec("neighboursL1Weights", neighboursL1Weights);
            neighboursL2Weights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_NEIGHBOURS_L2));        
            addVec("neighboursL2Weights", neighboursL2Weights);
            ipElemTreeWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_IP_ELEM_TREE));
            addVec("ipElemTreeWeights", ipElemTreeWeights);
            ipElemTreeUnlexWeights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX));
            addVec("ipElemTreeUnlexWeights", ipElemTreeUnlexWeights);
            wordL2Weights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_WORD_L2));
            addVec("wordL2Weights", wordL2Weights);
            wordL3Weights = VecFactory.zeros(vectorType, featureIndexers.getIndexerSize(ExtractFeatures.FEAT_WORD_L3));
            addVec("wordL3Weights", wordL3Weights);            
        }
        
        featureIndexers = null;
    }    
    
    /**
     * 
     * Compute overall discriminative model weight for given features. This returns
     * sum(w_i * f_i(x))
     * @param features
     * @return 
     */
    public double getModelWeight(Map<Feature, Double> features)
    {
        double score = 0.0d;
        for(Entry<Feature, Double> entry : features.entrySet())
        {            
            double count = entry.getValue(); // f_i(x)                       
            double weight = entry.getKey().getValue(); // w_i                        
            score += weight * count;            
        }
        return score;
    }
    
    public double getModelWeight(int[] featureIndices, double[] featureValues, int firstSrlIndex, int firstSrlPosIndex, 
            boolean computePrefixBaselineFeature, boolean computeWordBaselineFeature, boolean computeSyntacticFeatures, 
            boolean computeGlobalSyntacticFeatures,
            boolean computeSemanticsFeatures, boolean computeSrlFeatures, boolean computeSrlPosFeatures)
    {
        double score = 0.0d;
        
        if(computePrefixBaselineFeature)
            score += baselineWeight.getCount(0) * featureValues[ExtractFeatures.FEAT_BASELINE_SCORE];
        if(computeWordBaselineFeature)
            score += baselineWordWeight.getCount(0) * featureValues[ExtractFeatures.FEAT_BASELINE_WORD_SCORE];
        if(computeSemanticsFeatures)
            score += semanticWeight.getCount(0) * featureValues[ExtractFeatures.FEAT_SEMANTIC_SCORE];
        if(computeSyntacticFeatures)
        {
            score += elemTreeWeights.getCount(featureIndices[ExtractFeatures.FEAT_ELEM_TREE]) * featureValues[ExtractFeatures.FEAT_ELEM_TREE];
            score += prevElemTreeWeights.getCount(featureIndices[ExtractFeatures.FEAT_PREV_ELEM_TREE]) * featureValues[ExtractFeatures.FEAT_PREV_ELEM_TREE];
            score += elemTreeUnlexWeights.getCount(featureIndices[ExtractFeatures.FEAT_ELEM_TREE_UNLEX]) * featureValues[ExtractFeatures.FEAT_ELEM_TREE_UNLEX];
            score += prevElemTreeUnlexWeights.getCount(featureIndices[ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX]) * featureValues[ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX];
            score += elemTreeBigramWeights.getCount(featureIndices[ExtractFeatures.FEAT_ELEM_TREE_BIGRAM]) * featureValues[ExtractFeatures.FEAT_ELEM_TREE_BIGRAM];
            score += elemTreeUnlexBigramWeights.getCount(featureIndices[ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM]) * featureValues[ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM];
//            score += integrationPointWeights.getCount(featureIndices[ExtractFeatures.FEAT_INTEGRATION_POINT]) * featureValues[ExtractFeatures.FEAT_INTEGRATION_POINT];            
        }
        if(computeGlobalSyntacticFeatures)
        {
            score += rightBranchSpineWeight.getCount(0) * featureValues[ExtractFeatures.FEAT_RIGHT_BRANCH_SPINE];
            score += rightBranchRestWeight.getCount(0) * featureValues[ExtractFeatures.FEAT_RIGHT_BRANCH_REST];
            score += coParWeights.getCount(featureIndices[ExtractFeatures.FEAT_CO_PAR]) * featureValues[ExtractFeatures.FEAT_CO_PAR];
            score += coParWeights.getCount(featureIndices[ExtractFeatures.FEAT_CO_LEN_PAR]) * featureValues[ExtractFeatures.FEAT_CO_LEN_PAR];
            score += ipElemTreeWeights.getCount(featureIndices[ExtractFeatures.FEAT_IP_ELEM_TREE]) * featureValues[ExtractFeatures.FEAT_IP_ELEM_TREE];
            score += ipElemTreeWeights.getCount(featureIndices[ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX]) * featureValues[ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX];
            score += wordL2Weights.getCount(featureIndices[ExtractFeatures.FEAT_WORD_L2]) * featureValues[ExtractFeatures.FEAT_WORD_L2];
            score += wordL3Weights.getCount(featureIndices[ExtractFeatures.FEAT_WORD_L3]) * featureValues[ExtractFeatures.FEAT_WORD_L3];            
        }
        if(computeSrlFeatures)
        {
            for(int i = firstSrlIndex; i < firstSrlPosIndex; i++)
            {
                if(featureIndices[i] != Integer.MIN_VALUE)
                    score += srlTriplesWeights.getCount(featureIndices[i]) * featureValues[i];
            }
        }
        if(computeSrlPosFeatures)
        {
            for(int i = firstSrlPosIndex; i < featureIndices.length; i++)
            {
                if(featureIndices[i] != Integer.MIN_VALUE)
                    score += srlTriplesPosWeights.getCount(featureIndices[i]) * featureValues[i];
            }
        }
        return score;
    }
    
    public double getParamVecWeight(int vecId, int vecIndex)
    {
        return getVec(vecId).getCount(vecIndex);
    }
    
    public void setParamVecWeight(int vecId, int vecIndex, double value)
    {
        getVec(vecId).setUnsafe(vecIndex, value);
    }
    
    public void incrementParamVecWeight(int vecId, int vecIndex, double newValue)
    {
//        Vec vec = getVec(vecId);
//        vec.setUnsafe(vecIndex, vec.getCount(vecId) + value);
        getVec(vecId).addCount(vecIndex, newValue);
    }
    
    public double getVecL2Norm(int id)
    {
        return Math.sqrt(getVec(id).getSum());
    }
    
    public Vec getVec(int id)
    {
        switch(id)
        {
            case ExtractFeatures.FEAT_BASELINE_SCORE : return baselineWeight;
            case ExtractFeatures.FEAT_BASELINE_WORD_SCORE : return baselineWordWeight;
            case ExtractFeatures.FEAT_SEMANTIC_SCORE : return semanticWeight;
            case ExtractFeatures.FEAT_ELEM_TREE : return elemTreeWeights;
            case ExtractFeatures.FEAT_PREV_ELEM_TREE : return prevElemTreeWeights;
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX : return elemTreeUnlexWeights;
            case ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX : return prevElemTreeUnlexWeights;
            case ExtractFeatures.FEAT_ELEM_TREE_BIGRAM : return elemTreeBigramWeights;
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM : return elemTreeUnlexBigramWeights;
            case ExtractFeatures.FEAT_INTEGRATION_POINT : return integrationPointWeights;
            
            case ExtractFeatures.FEAT_RIGHT_BRANCH_SPINE : return rightBranchSpineWeight;
            case ExtractFeatures.FEAT_RIGHT_BRANCH_REST : return rightBranchRestWeight;
            case ExtractFeatures.FEAT_CO_PAR : return coParWeights;
            case ExtractFeatures.FEAT_CO_LEN_PAR : return coLenParWeights;
            case ExtractFeatures.FEAT_HEAVY : return heavyWeights;
            case ExtractFeatures.FEAT_NEIGHBOURS_L1 : return neighboursL1Weights;
            case ExtractFeatures.FEAT_NEIGHBOURS_L2 : return neighboursL2Weights;
            case ExtractFeatures.FEAT_IP_ELEM_TREE : return ipElemTreeWeights;
            case ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX : return ipElemTreeUnlexWeights;
            case ExtractFeatures.FEAT_WORD_L2 : return wordL2Weights;
            case ExtractFeatures.FEAT_WORD_L3 : return wordL3Weights;            
            
            case ExtractFeatures.FEAT_SRL_TRIPLES : return srlTriplesWeights;
            case ExtractFeatures.FEAT_SRL_TRIPLES_POS : return srlTriplesPosWeights;
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES : return srlIncompleteTriplesWeights;
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS : return srlIncompleteTriplesPosWeights;
            case ExtractFeatures.FEAT_SRL_ROLE : return srlRoleWeights;
            case ExtractFeatures.FEAT_SRL_DEPENDENCY : return srlDependencyWeights;
            case ExtractFeatures.FEAT_SRL_DEPENDENCY_POS : return srlDependencyPosWeights;
            case ExtractFeatures.FEAT_SRL_ROLE_PRED : return srlRolePredWeights;
            case ExtractFeatures.FEAT_SRL_ROLE_PRED_POS : return srlRolePredPosWeights;
            case ExtractFeatures.FEAT_SRL_ROLE_ARG : return srlRoleArgWeights;
            case ExtractFeatures.FEAT_SRL_ROLE_ARG_POS : return srlRoleArgPosWeights;
            case ExtractFeatures.FEAT_SRL_PRED : return srlPredWeights;
            case ExtractFeatures.FEAT_SRL_PRED_POS : return srlPredPosWeights;
            case ExtractFeatures.FEAT_SRL_ARG : return srlArgWeights;
            case ExtractFeatures.FEAT_SRL_ARG_POS : return srlArgPosWeights;
            case ExtractFeatures.FEAT_SRL_FRAME : return srlFrameWeights;
            case ExtractFeatures.FEAT_SRL_FRAME_POS : return srlFramePosWeights;
            default : throw new UnsupportedOperationException("incorrect parameter vector type");
        }
    }
    
    public Map<Integer, Vec> getVecsWithIds()
    {
        Map<Integer, Vec> map = new HashMap<Integer, Vec>();
        map.put(ExtractFeatures.FEAT_BASELINE_SCORE, baselineWeight);
        map.put(ExtractFeatures.FEAT_BASELINE_WORD_SCORE, baselineWordWeight);
        map.put(ExtractFeatures.FEAT_SEMANTIC_SCORE, semanticWeight);
        map.put(ExtractFeatures.FEAT_ELEM_TREE, elemTreeWeights);
        map.put(ExtractFeatures.FEAT_PREV_ELEM_TREE, prevElemTreeWeights);
        map.put(ExtractFeatures.FEAT_ELEM_TREE_UNLEX, elemTreeUnlexWeights);
        map.put(ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX, prevElemTreeUnlexWeights);
        map.put(ExtractFeatures.FEAT_ELEM_TREE_BIGRAM, elemTreeBigramWeights);
        map.put(ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM, elemTreeUnlexBigramWeights);
        map.put(ExtractFeatures.FEAT_INTEGRATION_POINT, integrationPointWeights);
        
        map.put(ExtractFeatures.FEAT_RIGHT_BRANCH_SPINE, rightBranchSpineWeight);
        map.put(ExtractFeatures.FEAT_RIGHT_BRANCH_REST, rightBranchRestWeight);
        map.put(ExtractFeatures.FEAT_CO_PAR, coParWeights);
        map.put(ExtractFeatures.FEAT_CO_LEN_PAR, coLenParWeights);
        map.put(ExtractFeatures.FEAT_HEAVY, heavyWeights);
        map.put(ExtractFeatures.FEAT_NEIGHBOURS_L1, neighboursL1Weights);
        map.put(ExtractFeatures.FEAT_NEIGHBOURS_L2, neighboursL2Weights);
        map.put(ExtractFeatures.FEAT_IP_ELEM_TREE, ipElemTreeWeights);
        map.put(ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX, ipElemTreeUnlexWeights);
        map.put(ExtractFeatures.FEAT_WORD_L2, wordL2Weights);
        map.put(ExtractFeatures.FEAT_WORD_L3, wordL3Weights);        
        
        map.put(ExtractFeatures.FEAT_SRL_TRIPLES, srlTriplesWeights);
        map.put(ExtractFeatures.FEAT_SRL_TRIPLES_POS, srlTriplesPosWeights);
        map.put(ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES, srlIncompleteTriplesWeights);
        map.put(ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS, srlIncompleteTriplesPosWeights);
        map.put(ExtractFeatures.FEAT_SRL_ROLE, srlRoleWeights);
        map.put(ExtractFeatures.FEAT_SRL_DEPENDENCY, srlDependencyWeights);
        map.put(ExtractFeatures.FEAT_SRL_DEPENDENCY_POS, srlDependencyPosWeights);
        map.put(ExtractFeatures.FEAT_SRL_ROLE_PRED, srlRolePredWeights);
        map.put(ExtractFeatures.FEAT_SRL_ROLE_PRED_POS, srlRolePredPosWeights);
        map.put(ExtractFeatures.FEAT_SRL_ROLE_ARG, srlRoleArgWeights);
        map.put(ExtractFeatures.FEAT_SRL_ROLE_ARG_POS, srlRoleArgPosWeights);
        map.put(ExtractFeatures.FEAT_SRL_PRED, srlRolePredWeights);
        map.put(ExtractFeatures.FEAT_SRL_PRED_POS, srlPredPosWeights);
        map.put(ExtractFeatures.FEAT_SRL_ARG, srlArgWeights);
        map.put(ExtractFeatures.FEAT_SRL_ARG_POS, srlArgPosWeights);
        map.put(ExtractFeatures.FEAT_SRL_FRAME, srlFrameWeights);
        map.put(ExtractFeatures.FEAT_SRL_FRAME_POS, srlFramePosWeights);
        return map;
    }
    
    public String[] getWeightsStats()
    {
        String[] out = new String[vecsMap.size()];
        int i = 0;
        for(Entry<String, Vec> entry : vecsMap.entrySet())
        {
            double[] avgAndSparcity = entry.getValue().vecStatsNonZeros();
            out[i++] = entry.getKey() + " - " + Fmt.D(avgAndSparcity);
        }
        return out;
    }
    
    public int getTotalVecSize()
    {
        int out = 0;
        for(Vec vec : vecsMap.values())
            out += vec.size();
        return out;        
    }
    
    public String[] getVecSizesToString()
    {
        String[] out = new String[vecsMap.size()];
        int i = 0;
        for(Entry<String, Vec> entry : vecsMap.entrySet())
        {            
            out[i++] = entry.getKey() + " - size=" + entry.getValue().size();
        }
        return out;
    }
    
    /**
     * Returns baseline weight of trained model
     * @return 
     */
    public double getBaselineWeight()
    {
        return baselineWeight.getCount(0);
    }

    public DiscriminativeFeatureIndexers getFeatureIndexers()
    {
        return featureIndexers;
    }
    
    @Override
    public String output(ParamsType paramsType)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String outputNonZero(ParamsType paramsType)
    {
        StringBuilder out = new StringBuilder();
        out.append("\nElementary trees\n");
        out.append(forEachCountNonZero(elemTreeWeights, featureIndexers.elemTreesStringArray()));
        out.append("\nElementary unlex trees\n");
        out.append(forEachCountNonZero(elemTreeUnlexWeights, featureIndexers.elemTreesUnlexStringArray()));
        out.append("\nSRL triples\n");
        out.append(forEachCountNonZero(srlTriplesWeights, featureIndexers.srlTriplesStringArray()));
        out.append("\nSRL POS triples\n");
        out.append(forEachCountNonZero(srlTriplesPosWeights, featureIndexers.srlTriplesPosStringArray()));
        return out.toString();
    }
}
