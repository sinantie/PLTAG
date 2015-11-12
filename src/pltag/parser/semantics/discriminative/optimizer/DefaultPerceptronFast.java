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
package pltag.parser.semantics.discriminative.optimizer;

import fig.basic.LogInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import pltag.parser.params.Vec;
import pltag.parser.semantics.discriminative.DiscriminativeParams;
import pltag.util.Utils;

/*
 * Yannis Konstas, <ikonstas@inf.ed.ac.uk>
 * University of Edinburgh
 *
 * Zhifei Li, <zhifei.work@gmail.com>
 * Johns Hopkins University
 */

/*Classes extend this should include
 * (1) process_one_sent: get the reranked 1-best; get the feature counts
 * (2) rerank the hypothesis
 * (3) feature extraction from 1-best and oracle
 * */
public class DefaultPerceptronFast extends GradientBasedOptimizer<Long>
{

    // key: feature string; value: model paramemter
    HashMap<Long, Double> globalTableSumModel = null; 
    //key: feature string; val: (1) last avg-model parameter, 
    // (2) last iter-id; (3) the last sum-model parammter 
    HashMap<Long, Double[]> globalTableAverageModel = null;
    // fixed size arrays that hold the parameter vector gradient indices, corresponding values, and parameter vector ids.
    // The first half of the arrays hold the differences (if any) between the oracle and the model,
    // and the rest half the differences between model and oracle.
    private final int[] gradientIds, gradientParamVecIds;
    private final double[] gradientValues;
    private final int gradientModelFeaturesStartIndex; // denotes the start index of the features that exist in the model and not in the oracle
    private double gradientNorm;
    private final double TOLERANCE = 0, EPSILON = 0.0001;
    
    public DefaultPerceptronFast(HashMap<Long, Double> sumModel, 
                             HashMap<Long, Double[]> averageModel, int trainSize, 
                             int batchUpdateSize, int convergePass, double coolingSchedule, 
                             double initGain, int maxGradientSize, double tolerance)
    {
        super(trainSize, batchUpdateSize, convergePass, coolingSchedule, initGain);
        globalTableSumModel = sumModel;
        globalTableAverageModel = averageModel;
        if (globalTableSumModel == null || globalTableAverageModel == null)
        {
            LogInfo.error("model table is null");
            System.exit(0);
        }
        gradientIds = new int[maxGradientSize];
        gradientParamVecIds = new int[maxGradientSize];
        gradientValues = new double[maxGradientSize];
        gradientModelFeaturesStartIndex = maxGradientSize / 2;
    }
    

    @Override
    public void initModel(double minValue, double maxValue)
    {
        //TODO do nothing
    }
        
    @Override
    public void initModel(DiscriminativeParams params)
    {
        for(Entry<Integer, Vec> entry : params.getVecsWithIds().entrySet())
        {
            int vecId = entry.getKey();
            Vec vec = entry.getValue();
            double value = 0.0;
            for(int i = 0; i < vec.size(); i++)
            {
                value = vec.getCount(i);
                if(value > 0.0)
                {
                    globalTableSumModel.put(Utils.asLong(vecId, i), value);
                }
            }            
        }
    }

    //	update tbl_sum_model and tbl_avg_model inside
    @Override
    public void updateModel(HashMap<Long, Double> oracleFeatures, HashMap<Long, Double> modelFeatures)
    {
        throw new UnsupportedOperationException("Not supported in the fast perceptron implementation.");
    }

    
    public void updateModel(int[] oracleFeaturesIds, double[] oracleFeaturesValues, int[] modelFeaturesIds, double[] modelFeaturesValues,
            boolean useVariableLengthFeatures, int[][] oracleVariableFeaturesStartEndIndex, int[][] modelVariableNumOfFeaturesStartEndIndex,
            DiscriminativeParams params, int[] variableParamVecIds)
    {
        numModelChanges++;
        cleanGradient();
        getGradient(oracleFeaturesIds, oracleFeaturesValues, modelFeaturesIds, modelFeaturesValues, useVariableLengthFeatures, 
                oracleVariableFeaturesStartEndIndex, modelVariableNumOfFeaturesStartEndIndex, variableParamVecIds, params);
        double updateGain = computeGain(numModelChanges);     
//        System.out.println(gradientToString());
        updateSumModel(globalTableSumModel, updateGain, params);
        updateAverageModel(globalTableSumModel, globalTableAverageModel, numModelChanges);
    }
    
    /**
     * 
     * Find differences (gradient) between oracle and model features. We use a fixed size gradient array that is double the size of 
     * the sum of all feature templates, for speed-up purposes. A feature template corresponds to a 1-of-k representation,
     * i.e., a feature is the k_th non-zero value stored in the corresponding position of the feature template.
     * This entails that we know a-priori the position of each feature template and length of variable-sized feature templates (if any).
     * In the latter case, we note the start and end index in the feature template space. Since we use a fixed-size array we have defined
     * an arbitrary maximum number for variable-sized feature templates. We also assume that all the fixed size feature templates
     * are stored before the variable-sized ones.
     * 
     * @param oracleFeaturesIds
     * @param oracleFeaturesValues
     * @param modelFeaturesIds
     * @param modelFeaturesValues
     * @param useVariableLengthFeatures
     * @param oracleVariableFeaturesStartEndIndex
     * @param modelVariableFeaturesStartEndIndex 
     */
    private void getGradient(int[] oracleFeaturesIds, double[] oracleFeaturesValues, 
            int[] modelFeaturesIds, double[] modelFeaturesValues,            
            boolean useVariableLengthFeatures, int[][] oracleVariableFeaturesStartEndIndex, int[][] modelVariableFeaturesStartEndIndex,
            int[] variableParamVecIds, DiscriminativeParams params)
    {
        // process tbl_feats_oracle
        double gradient;
        int lastIndexOfFixedSizeFeatures = useVariableLengthFeatures ? oracleVariableFeaturesStartEndIndex[0][0] : oracleFeaturesIds.length;
        // process fixed-size feature templates
        for(int i = 0; i< lastIndexOfFixedSizeFeatures; i++)
        {
            if(oracleFeaturesIds[i] != Integer.MIN_VALUE)
            {
                gradient = oracleFeaturesValues[i];
                if(oracleFeaturesIds[i] == modelFeaturesIds[i])
                    gradient -= modelFeaturesValues[i]; // v_oracle - v_1best
//                if(gradient != 0)
                if(Math.abs(gradient) > TOLERANCE)
                {
                    if(IS_MINIMIZE_SCORE)
                        updateGradient(i, oracleFeaturesIds[i], -gradient, i); // note: we are minimizing the cost
                    else
                        updateGradient(i, oracleFeaturesIds[i], gradient, i); // note: we are maximizing the prob
//                        updateGradientNormalised(i, oracleFeaturesIds[i], gradient, i, params); // note: we are maximizing the prob
                    gradientNorm++;
                }
            }            
        }
        // process variable-sized feature templates
        if(useVariableLengthFeatures)
        {
            int k = 0;
            for(int[] startEndIndices : oracleVariableFeaturesStartEndIndex)
            {
                for(int i = startEndIndices[0]; i < startEndIndices[1]; i++)
                {
                    if(oracleFeaturesIds[i] != Integer.MIN_VALUE)
                    {
                        gradient = oracleFeaturesValues[i];
                        int modelId = Arrays.binarySearch(modelFeaturesIds, modelVariableFeaturesStartEndIndex[k][0], modelVariableFeaturesStartEndIndex[k][1], oracleFeaturesIds[i]);
                        if(modelId > 0)
                            gradient -= modelFeaturesValues[modelId];
//                        if(gradient != 0)
                        if(Math.abs(gradient) > TOLERANCE)
                        {
                            if(IS_MINIMIZE_SCORE)
                                updateGradient(i, oracleFeaturesIds[i], -gradient, variableParamVecIds[k]); // note: we are minimizing the cost
                            else
                                updateGradient(i, oracleFeaturesIds[i], gradient, variableParamVecIds[k]); // note: we are maximizing the prob
//                                updateGradientNormalised(i, oracleFeaturesIds[i], gradient, variableParamVecIds[k], params); // note: we are maximizing the prob
                            gradientNorm++;
                        } // if
                    } // if
                } // for
                k++;
            } // for
        } // if
        // process tbl_feats_1best
        lastIndexOfFixedSizeFeatures = useVariableLengthFeatures ? modelVariableFeaturesStartEndIndex[0][0] : modelFeaturesIds.length;
        // process fixed-size feature templates
        for(int i = 0; i< lastIndexOfFixedSizeFeatures; i++)
        {
            if(oracleFeaturesIds[i] != modelFeaturesIds[i]) // this feat only activates in the 1best, not in oracle
            {
                if(IS_MINIMIZE_SCORE)
                    updateGradient(gradientModelFeaturesStartIndex + i, modelFeaturesIds[i], modelFeaturesValues[i], i); // note: we are minizing the cost
                else
                    updateGradient(gradientModelFeaturesStartIndex + i, modelFeaturesIds[i], -modelFeaturesValues[i], i); // note: we are maximizing the prob
//                    updateGradientNormalised(gradientModelFeaturesStartIndex + i, modelFeaturesIds[i], -modelFeaturesValues[i], i, params); // note: we are maximizing the prob
                gradientNorm++;
            }
        }
        // process variable-sized feature templates
        if(useVariableLengthFeatures)
        {
            int k = 0;
            for(int[] startEndIndices : modelVariableFeaturesStartEndIndex)
            {
                for(int i = startEndIndices[0]; i < startEndIndices[1]; i++)
                {
                    if(modelFeaturesIds[i] != Integer.MIN_VALUE)
                    {
                        int oracleId = Arrays.binarySearch(oracleFeaturesIds, oracleVariableFeaturesStartEndIndex[k][0], oracleVariableFeaturesStartEndIndex[k][1], modelFeaturesIds[i]);
                        if(oracleId < 0)  // this feat only activates in the 1best, not in oracle
                        {
                            if(IS_MINIMIZE_SCORE)
                                updateGradient(gradientModelFeaturesStartIndex + i, modelFeaturesIds[i], modelFeaturesValues[i], variableParamVecIds[k]); // note: we are minizing the cost
                            else
                                updateGradient(gradientModelFeaturesStartIndex + i, modelFeaturesIds[i], -modelFeaturesValues[i], variableParamVecIds[k]); // note: we are maximizing the prob
//                                updateGradientNormalised(gradientModelFeaturesStartIndex + i, modelFeaturesIds[i], -modelFeaturesValues[i], variableParamVecIds[k], params); // note: we are maximizing the prob
                            gradientNorm++;
                        } // if
                    } // if
                } // for
                k++;
            } // for
        } // if        
    }
    
    private void updateGradient(int pos, int featureId, double value, int paramVecId)
    {
        gradientIds[pos] = featureId;
        gradientValues[pos] = value;
        gradientParamVecIds[pos] = paramVecId;
    }
    
    private void updateGradientNormalised(int pos, int featureId, double value, int paramVecId, DiscriminativeParams params)
    {
        double weight = params.getParamVecWeight(paramVecId, featureId);
        double delta = - weight * value;
        gradientIds[pos] = featureId;
        gradientValues[pos] = ((delta + (value > 0 ? EPSILON : -EPSILON)) * value) / Math.sqrt(params.getVecL2Norm(paramVecId));
        gradientParamVecIds[pos] = paramVecId;
    }
    
    private void cleanGradient()
    {
        gradientNorm = 0;
        Arrays.fill(gradientIds, Integer.MIN_VALUE);     
        Arrays.fill(gradientParamVecIds, Integer.MIN_VALUE);
    }
     //update tbl_sum_model inside
    protected void updateSumModel(HashMap<Long, Double> tableSumModel, double updateGain, DiscriminativeParams params)
    {        
        for(int i = 0; i < gradientParamVecIds.length; i++)
        {
            if(gradientParamVecIds[i] != Integer.MIN_VALUE)
            {                
                // encode param vec id (type int) and feature index (type int) into a long. This way we avoid creating wrapper objects.
                Long key = Utils.asLong(gradientParamVecIds[i], gradientIds[i]);
                Double oldValue = tableSumModel.get(key);
                double newValue = updateGain * gradientValues[i];
                Double update = oldValue != null ? oldValue + newValue : newValue; // incrementally add feature          
                tableSumModel.put(key, update);
                try
                {
//                    params.setParamVecWeight(gradientParamVecIds[i], gradientIds[i], update); // propagate change to the param vecs
                    params.incrementParamVecWeight(gradientParamVecIds[i], gradientIds[i], newValue); // propagate change to the param vecs
                }
                catch(AssertionError e) 
                {
                    LogInfo.error("Error updating param vector: " + e.getMessage());
                }
            }
        }
    }
    
    //	key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter
    //update tbl_avg_model inside
    protected void updateAverageModel(HashMap<Long, Double> tableSumModel, 
                                      HashMap<Long, Double[]> tableAverageModel, int curIterId)
    {//feature_set: the features need to be updated
        for(int i = 0; i < gradientParamVecIds.length; i++)
        {
            if(gradientParamVecIds[i] != Integer.MIN_VALUE)
            { 
                // encode param vec id (type int) and feature index (type int) into a long. This way we avoid creating wrapper objects.
                Long key = Utils.asLong(gradientParamVecIds[i], gradientIds[i]);
                updateAverageModelOneFeature(tableSumModel, tableAverageModel, key, curIterId);
            }
        }
    }
        
    //	key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter
    //update tbl_avg_model inside
    protected void updateAverageModel(HashMap<Long, Double> tableSumModel, 
                                      HashMap<Long, Double[]> tableAverageModel, 
                                      HashMap<Long, Double> featureSet, int curIterId)
    {//feature_set: the features need to be updated
        for (Long key : featureSet.keySet())
        {            
            updateAverageModelOneFeature(tableSumModel, tableAverageModel, key, curIterId);
        }
    }
    
    protected void updateSumModel(HashMap<Integer, Double> tableSumModel, 
                                  HashMap<Integer, Double> gradient, double updateGain)
    {
        throw new UnsupportedOperationException("Not supported in the fast perceptron implementation.");
    }         

    //tbl_sum_model has already been updated	
    //	key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter
    //	update tbl_avg_model inside
    protected void updateAverageModelOneFeature(HashMap<Long, Double> tableSumModel, 
                                                HashMap<Long, Double[]> tableAverageModel, 
                                                Long featureKey, int curIterId)
    {
        Double[] oldValues = tableAverageModel.get(featureKey);
        Double[] newValues = new Double[3];
        newValues[1] = new Double(curIterId);//iter id 
        newValues[2] = tableSumModel.get(featureKey);//sum model para
        if (oldValues != null)
        {
            newValues[0] = (oldValues[0] * oldValues[1] + oldValues[2] * (curIterId - oldValues[1] - 1) + newValues[2]) / curIterId;//avg
        }
        else//incrementally add feature
        {
            newValues[0] = newValues[2] / curIterId;//avg			
        }
        tableAverageModel.put(featureKey, newValues);
    }

    /*
     * Force update the whole avg model. 
     * For each feature, it will automatically handle cases where the feature is 
     * already updated.
     */
    public void forceUpdateAverageModel()
    {
//        Utils.logs("force average update is called");
        updateAverageModel(globalTableSumModel, globalTableAverageModel, 
                           globalTableSumModel, numModelChanges); // update all features

    }

    @Override
    public HashMap getAvgModel()
    {
        return globalTableAverageModel;
    }

    public void updateParamsWithAvgWeights(DiscriminativeParams params)
    {
        for(Entry<Long, Double[]> entry : globalTableAverageModel.entrySet())
        {
            try
            {
                // decode key from an entangled long representation into its' parts, i.e., a param vec id (type int) and feature index (type int).
                int paramVecId = Utils.getFirst(entry.getKey());
                int vecIndex = Utils.getSecond(entry.getKey());
                params.setParamVecWeight(paramVecId, vecIndex, entry.getValue()[0]);                
            } catch(AssertionError e) 
            {
                LogInfo.error("Error updating param vector: " + e.getMessage());
            }
        }
    }
    
    @Override
    public HashMap getSumModel()
    {
        return globalTableSumModel;
    }

    @Override
    public double getGradientNorm()
    {
        return gradientNorm;
    }

    @Override
    public void setFeatureWeight(Long feat, double weight)
    {
        globalTableSumModel.put(feat, weight);
        Double[] vals = new Double[3];
        vals[0] = weight;
        vals[1] = 1.0;//TODO
        vals[2] = 0.0;//TODO
        globalTableAverageModel.put(feat, vals);

    }    
    
    public String gradientToString()
    {
        Set<String> treeSet = new TreeSet<String>();
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < gradientIds.length; i++)
        {
            if(gradientIds[i] != Integer.MIN_VALUE)
            {
                treeSet.add(gradientIds[i] + " - " + gradientValues[i]);
            }
        }
        return treeSet.toString();
    }
}
