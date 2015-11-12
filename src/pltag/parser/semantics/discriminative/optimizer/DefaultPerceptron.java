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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import pltag.parser.params.Vec;
import pltag.parser.semantics.discriminative.DiscriminativeParams;
import pltag.parser.semantics.discriminative.Feature;
import pltag.util.Utils;

/*Zhifei Li, <zhifei.work@gmail.com>
 * Johns Hopkins University
 */

/*Cleasses extend this should include
 * (1) process_one_sent: get the reranked 1-best; get the feature counts
 * (2) rerank the hypothesis
 * (3) feature extraction from 1-best and oracle
 * */
public class DefaultPerceptron extends GradientBasedOptimizer<Feature>
{

    // key: feature string; value: model paramemter
    HashMap<Feature, Double> globalTableSumModel = null; 
    //key: feature string; val: (1) last avg-model parameter, 
    // (2) last iter-id; (3) the last sum-model parammter 
    HashMap<Feature, Double[]> globalTableAverageModel = null;

    private double gradientNorm;
    
    public DefaultPerceptron(HashMap<Feature, Double> sumModel, 
                             HashMap<Feature, Double[]> averageModel, int trainSize, 
                             int batchUpdateSize, double tolerance)
    {
        super(trainSize, batchUpdateSize, tolerance);
        globalTableSumModel = sumModel;
        globalTableAverageModel = averageModel;
        if (globalTableSumModel == null || globalTableAverageModel == null)
        {
            LogInfo.error("model table is null");
            System.exit(0);
        }
    }
    
    public DefaultPerceptron(HashMap<Feature, Double> sumModel, 
                             HashMap<Feature, Double[]> averageModel, int trainSize, 
                             int batchUpdateSize, int convergePass, 
                             double initGain, double tolerance)
    {
        super(trainSize, batchUpdateSize, convergePass, initGain, tolerance);
        globalTableSumModel = sumModel;
        globalTableAverageModel = averageModel;
        if (globalTableSumModel == null || globalTableAverageModel == null)
        {
            LogInfo.error("model table is null");
            System.exit(0);
        }
    }
    
    public DefaultPerceptron(HashMap<Feature, Double> sumModel, 
                             HashMap<Feature, Double[]> averageModel, int trainSize, 
                             int batchUpdateSize, int convergePass, double coolingSchedule, 
                             double initGain, double tolerance)
    {
        super(trainSize, batchUpdateSize, convergePass, coolingSchedule, initGain, tolerance);
        globalTableSumModel = sumModel;
        globalTableAverageModel = averageModel;
        if (globalTableSumModel == null || globalTableAverageModel == null)
        {
            LogInfo.error("model table is null");
            System.exit(0);
        }
    }
    public DefaultPerceptron(HashMap<Feature, Double> sumModel, 
                             HashMap<Feature, Double[]> averageModel, int trainSize, 
                             int batchUpdateSize, int convergePass, double initGain, 
                             double sigma, double coolingSchedule, double tolerance, boolean isMinimizeScore)
    {
        super(trainSize, batchUpdateSize, convergePass, initGain, sigma, coolingSchedule, tolerance, isMinimizeScore);
        globalTableSumModel = sumModel;
        globalTableAverageModel = averageModel;
        if (globalTableSumModel == null || globalTableAverageModel == null)
        {
            LogInfo.error("model table is null");
            System.exit(0);
        }
    }

    @Override
    public void initModel(double minValue, double maxValue)
    {
        //TODO do nothing
    }

    @Override
    public void initModel(DiscriminativeParams params)
    {
        for(Vec vec : params.getVecs().values())
        {
            double value;
            for(int i = 0; i < vec.size(); i++)
            {
                value = vec.getCount(i);
                if(value > 0.0)
                {
                    globalTableSumModel.put(new Feature(vec, i), value);
                }
            }            
        }
    }
    
    //	update tbl_sum_model and tbl_avg_model inside
    @Override
    public void updateModel(HashMap<Feature, Double> oracleFeatures, HashMap<Feature, Double> modelFeatures)
    {
        numModelChanges++;
//        Utils.logs("Update the perceptron model " + numModelChanges);
        HashMap<Feature, Double> gradient = getGradient(oracleFeatures, modelFeatures);        
        double updateGain = computeGain(numModelChanges);
//        Utils.logs("Update gain is " + updateGain + "; gradient table size " + gradient.size());
        gradientNorm = gradient.size();
//        System.out.println(gradientToString(gradient));
        updateSumModel(globalTableSumModel, gradient, updateGain);
        updateAverageModel(globalTableSumModel, globalTableAverageModel, gradient, numModelChanges);
    }

    //update tbl_sum_model inside
    protected void updateSumModel(HashMap<Feature, Double> tableSumModel, 
                                  HashMap<Feature, Double> gradient, double updateGain)
    {
        for (Iterator<Feature> it = gradient.keySet().iterator(); it.hasNext();)
        {
            Feature key = it.next();
            Double oldValue = tableSumModel.get(key);
//            Double oldValue = key.getValue();
            Double update;
            if (oldValue != null)
//            if (!oldValue.equals(new Double(0.0)))
            {
                update = oldValue + updateGain * gradient.get(key);
                tableSumModel.put(key, update);                
            }
            else
            {
                update = updateGain * gradient.get(key);
                tableSumModel.put(key, update); // incrementally add feature
            }
            try {
            key.setValue(update); // propagate change to the ProbVecs
            } catch(AssertionError e) 
            {
                System.out.println("Error updating param vector: " + e.getMessage());
            }
        }
    }

    //	key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter
    //update tbl_avg_model inside
    protected void updateAverageModel(HashMap<Feature, Double> tableSumModel, 
                                      HashMap<Feature, Double[]> tableAverageModel, 
                                      HashMap featureSet, int curIterId)
    {//feature_set: the features need to be updated
        for (Iterator<Feature> it = featureSet.keySet().iterator(); it.hasNext();)
        {
            Feature key = it.next();
            updateAverageModelOneFeature(tableSumModel, tableAverageModel, key, curIterId);
        }
    }

    //tbl_sum_model has already been updated	
    //	key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter
    //	update tbl_avg_model inside
    protected void updateAverageModelOneFeature(HashMap<Feature, Double> tableSumModel, 
                                                HashMap<Feature, Double[]> tableAverageModel, 
                                                Feature featureKey, int curIterId)
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

    public HashMap getAvgModel()
    {
        return globalTableAverageModel;
    }

    public void updateParamsWithAvgWeights()
    {
        for(Feature f : globalTableAverageModel.keySet())
        {
            try{
                f.setValue(globalTableAverageModel.get(f)[0]);                
            } catch(AssertionError e) 
            {
                System.out.println("Error updating param vector: " + e.getMessage());
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
    public void setFeatureWeight(Feature feat, double weight)
    {
        globalTableSumModel.put(feat, weight);
        Double[] vals = new Double[3];
        vals[0] = weight;
        vals[1] = 1.0;//TODO
        vals[2] = 0.0;//TODO
        globalTableAverageModel.put(feat, vals);

    }
    
    public String gradientToString(Map<Feature, Double> gradient)
    {
        Set<String> treeSet = new TreeSet<String>();
        for(Entry<Feature, Double> e : gradient.entrySet())
            treeSet.add(e.getKey().toString() + " - " + e.getValue());
        return treeSet.toString();
    }
}
