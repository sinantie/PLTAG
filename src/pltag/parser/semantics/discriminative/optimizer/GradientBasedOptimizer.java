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
import java.util.Map;
import pltag.parser.semantics.discriminative.DiscriminativeParams;
import pltag.parser.semantics.discriminative.Feature;

/*This class implements common functions:
 * (1) gradient computation
 * (2) batch update
 * (3) cooling schedule
 * (4) regularization
 * */
public abstract class GradientBasedOptimizer<T>
{

    // update the perceptron after processing BATCH_SIZE
    private final int BATCH_UPDATE_SIZE; 
    // number of training examples
    private final int TRAIN_SIZE;
    // assume the model will converge after pass CONVERGE_PASS
    private final int CONVERGE_PASS;
    // where parameter t was adjusted such that the gain is halved after 
    // one pass through the data (610k*2/30)
    private final double COOLING_SCHEDULE_T;
    private final double INITIAL_GAIN;
    // the smaller SIGMA, the sharp the prior is; the more regularized of the model 
    // (meaning more feature weights goes close to zero)
    private final double SIGMA;
    private double REG_CONSTANT_RATIO = 0;
    protected final boolean IS_MINIMIZE_SCORE;
    private final double TOLERANCE;
    // how many times the model is changed
    protected int numModelChanges = 0;
    // default is with regularization; (perceptron does not use this)
    private boolean noRegularization = false;
    // default is with cooling
    protected boolean noCooling = false;

    public void setNoRegularization()
    {
        noRegularization = true;
    }

    public void setNoCooling()
    {
        noCooling = true;
    }

    /*
     * Default constructor with reasonable initial parameters. TODO optimise
     */
    public GradientBasedOptimizer(int trainSize, int batchUpdateSize, double tolerance)
    {
        this(trainSize, batchUpdateSize, 1, 0.1, 1.0, trainSize * 1 / batchUpdateSize, tolerance, false);
    }
    
    public GradientBasedOptimizer(int trainSize, int batchUpdateSize, int convergePass, 
                                  double initGain, double tolerance)
    {
        this(trainSize, batchUpdateSize, convergePass, initGain, 1.0, 
             trainSize * convergePass * 1.0 / batchUpdateSize, tolerance, false);
    }
    
    public GradientBasedOptimizer(int trainSize, int batchUpdateSize, int convergePass, double coolingSchedule,
                                  double initGain, double tolerance)
    {
        this(trainSize, batchUpdateSize, convergePass, initGain, 1.0, coolingSchedule, tolerance, false);
    }

    public GradientBasedOptimizer(int trainSize, int batchUpdateSize, 
                                  int convergePass, double initGain, 
                                  double sigma, double coolingSchedule, 
                                  double tolerance, boolean isMinimizeScore)
    {
        TRAIN_SIZE = trainSize;
        BATCH_UPDATE_SIZE = batchUpdateSize;
        CONVERGE_PASS = convergePass;
        INITIAL_GAIN = initGain;
        COOLING_SCHEDULE_T = coolingSchedule;
        TOLERANCE = tolerance;
        SIGMA = sigma;
        REG_CONSTANT_RATIO = BATCH_UPDATE_SIZE * 1.0 / (TRAIN_SIZE * SIGMA * SIGMA);

        IS_MINIMIZE_SCORE = isMinimizeScore;
        LogInfo.logsForce("TRAIN_SIZE: " + TRAIN_SIZE + "\n" + 
                   "BATCH_UPDATE_SIZE: " + BATCH_UPDATE_SIZE + "\n" + 
                   "CONVERGE_PASS: " + CONVERGE_PASS + "\n" + 
                   "INITIAL_GAIN: " + INITIAL_GAIN + "\n" + 
                   "COOLING_SCHEDULE_T: " + COOLING_SCHEDULE_T + "\n" + 
                   "SIGMA: " + SIGMA + "\n" + 
                   "REG_CONSTANT_RATIO: " + REG_CONSTANT_RATIO + "\n" + 
                   "IS_MINIMIZE_SCORE: " + IS_MINIMIZE_SCORE);
    }

    public abstract void initModel(double minValue, double maxValue);// random start
    
    public abstract void initModel(DiscriminativeParams params);// initialise sum model with already initialised model parameters

    public abstract void updateModel(HashMap<T, Double> oracleFeatures, 
                                     HashMap<T, Double> modelFeatures);
   
    public abstract HashMap getAvgModel();

    public abstract HashMap getSumModel();

    public abstract void setFeatureWeight(T feat, double weight);

    public abstract double getGradientNorm();
    
    public int getBatchSize()
    {
        return BATCH_UPDATE_SIZE;
    }

    protected HashMap<T, Double> getGradient(HashMap<T, Double> oracleFeatures, 
              HashMap<T, Double> modelFeatures)
    {
        HashMap<T, Double> res = new HashMap<T, Double>();
        // process tbl_feats_oracle
        for (Map.Entry<T, Double> entry : oracleFeatures.entrySet())
        {
            T key = entry.getKey();
            double gradient = entry.getValue();
            Double v_1best = modelFeatures.get(key);
            if (v_1best != null)
            {
                gradient -= v_1best; // v_oracle - v_1best
            }
//            if (gradient != 0)//TODO
            if(Math.abs(gradient) > TOLERANCE)
            {
                if (IS_MINIMIZE_SCORE)
                {
                    res.put(key, -gradient); // note: we are minimizing the cost
                }
                else
                {
                    res.put(key, gradient); // note: we are max the prob
                }
            }
        }
        // process tbl_feats_1best
        for (Map.Entry<T, Double> entry : modelFeatures.entrySet())
        {
            T key = entry.getKey();
            Double v_oracle = oracleFeatures.get(key);
            if (v_oracle == null) // this feat only activate in the 1best, not in oracle
            {
                if (IS_MINIMIZE_SCORE)
                {
                    res.put(key, entry.getValue()); // note: we are minizing the cost
                }
                else
                {
                    res.put(key, -entry.getValue()); // note: we are maximize the prob
                }
            }
        }
        // System.out.println("gradient size is: " + res.size());
        return res;
    }

    protected double computeGain(int iterNumber)
    {
        // the numbers of updating the model
        if (noCooling)
        {
            return 1.0;
        }
        else
        {
//            return INITIAL_GAIN * COOLING_SCHEDULE_T / (COOLING_SCHEDULE_T + iterNumber);
            return 1.0 / Math.pow(iterNumber + 2, COOLING_SCHEDULE_T);
        }
    }

    protected double computeRegularizationScale(double updateGain)
    {
        if (noRegularization)
        {
            return 1.0;
        }
        else
        {
            return 1.0 - REG_CONSTANT_RATIO * updateGain;
        }
    }    
}
