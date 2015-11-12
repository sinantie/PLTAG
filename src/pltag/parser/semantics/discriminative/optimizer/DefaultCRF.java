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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import pltag.parser.semantics.discriminative.DiscriminativeParams;
import pltag.parser.semantics.discriminative.Feature;


/* This class implementations
 * (1) set CRF parameters
 * (2) initialize the model
 * (3) update model
 * */
public class DefaultCRF extends GradientBasedOptimizer<Feature>
{

    protected HashMap<Feature, Double> modelTbl = null;

    public DefaultCRF(HashMap<Feature, Double> model, int train_size, int batch_update_size,
            int converge_pass, double init_gain, double sigma, double tolerance, boolean is_minimize_score)
    {
        super(train_size, batch_update_size, converge_pass, init_gain, sigma,
                train_size * converge_pass * 1.0 / batch_update_size, tolerance, is_minimize_score);
        modelTbl = model;
        if (modelTbl == null || modelTbl.size() <= 0)
        {
            System.out.println("model table is null or empty");
            System.exit(0);
        }
    }

//	############ common functions for PERCEPTRON update ###################################
    //the tbl_model should already have all the features, we are doing random start
    public void initModel(double min_value, double max_value)
    {
        System.out.println("random start between min:max =" + min_value + " , " + max_value);
        if (min_value == max_value)
        {
            scaleMapEntries(modelTbl, min_value);
        }
        else
        {
            Random rd = new Random(0);//TODO: fixed seed?
            for (Iterator<Feature> iter = modelTbl.keySet().iterator(); iter.hasNext();)
            {
                Feature key = iter.next();
                double val = rd.nextDouble() * (max_value - min_value) + min_value;
                modelTbl.put(key, val);
            }
        }
    }

    public void initModel(DiscriminativeParams params)
    {
        
    }
    
//	update tbl_sum_model and  tbl_avg_model inside
    public void updateModel(HashMap tbl_feats_empirical, HashMap tbl_feats_model)
    {
        numModelChanges++;
        System.out.println("######## update the crf model ############### " + numModelChanges);
        HashMap gradient = getGradient(tbl_feats_empirical, tbl_feats_model);
        //System.out.println("gradient is:\n"+gradient.toString());
        //System.out.println("gradient size is:"+gradient.size());
        update_model_helper(gradient);
    }

    //update tbl_sum_model inside
    private void update_model_helper(HashMap gradient)
    {
        double update_gain = computeGain(numModelChanges);
        System.out.println("update gain is " + update_gain + "; num model changes" + numModelChanges);

        /*clearly, if a feature does not active often, then it gradually becomes zero; since the term is smaller than one*/
        //update the whole feature vector with regularization
        double regu_scale = computeRegularizationScale(update_gain);
        System.out.println("regu scale is " + regu_scale);
        if (regu_scale != 1.0)
        {
            scaleMapEntries(modelTbl, regu_scale);
        }
        else
        {
            //System.out.println("### no scaling ###");
        }


        //further update activated features
        for (Iterator<Feature> it = gradient.keySet().iterator(); it.hasNext();)
        {
            Feature key = it.next();
            Double old_v = modelTbl.get(key);
            if (old_v != null)
            {
                modelTbl.put(key, old_v + update_gain * (Double) gradient.get(key));
            }
            else
            {
                System.out.println("The parameter is not initialized for feature " + key);
                System.exit(0);
                //tbl_sum_model.put(key, update_gain*(Double)gradient.get(key)); //incrementally add feature
            }
        }
        //System.out.println("baseline feature weight " + get_sum_model().get(HGDiscriminativeLearner.g_baseline_feat_name));
    }

    @Override
    public HashMap getAvgModel()
    {
        System.out.println("CRF does not use averge model");
        System.exit(0);
        return null;

    }

    @Override
    public HashMap getSumModel()
    {
        return modelTbl;
    }

    @Override
    public double getGradientNorm()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFeatureWeight(Feature feat, double weight)
    {
        modelTbl.put(feat, weight);
    }

    public static void scaleMapEntries(HashMap<?, Double> map, double scale)
    {
        for (Map.Entry<?, Double> entry : map.entrySet())
        {
            entry.setValue(entry.getValue() * scale);
        }
    }
}
