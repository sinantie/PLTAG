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
package pltag.parser.params;

import fig.prob.DirichletUtils;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.math.linear.RealVector;
import pltag.util.Pair;
import pltag.util.Utils;
import pltag.util.Utils.TypeAdd;

/**
 *
 * Augment a regular array with a sum and old sum
 * Used for storing probability vectors allowing for an O(1) update by storing normalization constant
 * The whole motivation for storing the sum is that we can update counts and get probability vectors for online EM
 * The only reason for storing the oldSum is that for the aggressive online EM update, we need to scale a series
 * of updates by the same sum, but updating it affects the sum
 **/
public class ProbVec implements Serializable, Vec
{
    static final long serialVersionUID = -8235238691651895455L;
    private double[] counts;
    private int[] sortedIndices;
    private double sum, oldSum;
    private String[] labels;
    
    public ProbVec(double[] counts, double sum, double oldSum)
    {
        this.counts = counts;
        this.sum = sum;
        this.oldSum = oldSum;
    }

    private ProbVec(double[] counts, double sum, double oldSum, String[] labels)
    {
        this(counts, sum, oldSum);
        this.labels = labels;
    }

    public double[] getCounts() // for serialisation use only
    {
        return counts;
    }

    public double getSum() // for serialisation use only
    {
        return sum;
    }

    public String[] getLabels() // for serialisation use only
    {
        return labels;
    }

//    public void setData(double[] counts, double sum, double oldSum, String[] labels) // for serialisation use only
//    {
//        this.counts = counts;
//        this.sum = sum;
//        this.oldSum = oldSum;
//        this.labels = labels;
//    }

    public void copyDataFrom(Vec v)
    {
        assert v instanceof ProbVec;
        ProbVec pv = (ProbVec)v;
        this.counts = pv.counts;
        this.sum = pv.sum;
        this.oldSum = pv.oldSum;
        this.labels = pv.labels;
    }
    
    public double getProb(int i)
    {
        return counts[i] / sum;
    }

    @Override
    public double getCount(int i)
    {
        return counts[i];
    }

    @Override
    public Vec addCount(double x)
    {
        Utils.add(counts, x);
        sum += counts.length * x;
        return this;
    }

    @Override
    public Vec addCount(int i, double x)
    {
        counts[i] += x;
//        sum += x;
        sum += Math.abs(x);
//        sum += x * x;
        return this;
    }

    @Override
    public Vec addCountKeepNonNegative(int i, double x)
    {
        // If adding would make it < 0, just set it to 0
        // This is mostly for numerical precision errors (it shouldn't go too much below 0)
        if(counts[i] + x < 0)
        {
            sum -= counts[i];
            counts[i] = 0;
        }
        else
        {
            counts[i] += x;
            sum += x;
        }
        return this;
    }

    // Add a feature vector phi (usually, phi is indicator at some i
    @Override
    public Vec addCount(double[] phi, double x)
    {
        Utils.add(counts, x, phi);
        sum += x;
        return this;
    }

    @Override
    public Vec addCount(Vec vec, double x)
    {        
        Utils.add(counts, x, vec.getCounts());
        sum += x * vec.getSum();
        return this;
    }

    @Override
    public Vec addCount(Vec vec)
    {        
        Utils.add(counts, 1.0, vec.getCounts());
        sum += vec.getSum();
        return this;
    }
    
    // For the special aggressive online EM update
    @Override
    public Vec addProb(int i, double x)
    {
        return addCount(i, x * oldSum);
    }

    @Override
    public Vec addProbKeepNonNegative(int i, double x)
    {
        return addCountKeepNonNegative(i, x * oldSum);
    }

    public Vec addProb(double[] phi, double x)
    {
        return addCount(phi, x * oldSum);
    }

    public void saveSum()
    {
        oldSum = sum;
    }

    public void setCountToObtainProb(int i, double p)
    {
        assert(p < 1);
        final double x = (sum-counts[i]) * p / (1-p) - counts[i];
        counts[i] += x;
        sum += x;
    }

    public double[] getProbs()
    {
        // in the discriminative model we save weights not probabilities, so no need to normalise
        return sum == 0 ? counts : Utils.div(Arrays.copyOf(counts, counts.length), sum);
    }

    @Override
    public Set<Pair<Integer>> getProbsSorted()
    {        
        return getSorted(getProbs());
    }
    
    @Override
    public Set<Pair<Integer>> getCountsSorted()
    {
        return getSorted(getCounts());
    }
    
    private Set<Pair<Integer>> getSorted(double[] counts)
    {
        int length = counts.length;

        TreeSet<Pair<Integer>> pairs = new TreeSet<Pair<Integer>>();
        // sort automatically by probability (pair.value)
        for(int i = 0; i < length; i++)
        {
            pairs.add(new Pair(counts[i], new Integer(i)));
        }
        return pairs.descendingSet();
    }

    @Override
    public void setProbSortedIndices()
    {
        sortedIndices = new int[counts.length];
        int i = 0;
        for(Pair p: getProbsSorted())
        {
            sortedIndices[i++] = (Integer)p.label;
        }
    }

    @Override
    public void setCountsSortedIndices()
    {
        sortedIndices = new int[counts.length];
        int i = 0;
        for(Pair p: getCountsSorted())
        {
            sortedIndices[i++] = (Integer)p.label;
        }
    }
    
    public int getMax()
    {
        int index = -1;
        double maxCount = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < counts.length; i++)
        {
            if(counts[i] > maxCount)
            {
                index = i;
                maxCount = counts[i];
            }
        }
        return index;
    }

    @Override
    public Pair getAtRank(int rank)
    {
        return new Pair(counts[sortedIndices[rank]], sortedIndices[rank]);
    }
    /**
     * Usage: call saveSum, normalize, getOldSum
     * Useful for printing out posteriors - get an idea of posterior mass on these rules
     **/
    public double getOldSum()
    {
        return oldSum;
    }

    public Vec expDigamma()
    {
        if(sum > 0)
        {
            DirichletUtils.fastExpExpectedLogMut(counts);
            computeSum();
        }
        return this;
    }

    public Vec normalise()
    {
        if (sum == 0)
        {
            Utils.set(counts, 1.0/counts.length);
        }
        else
        {
            Utils.div(counts, sum);
        }
        sum = 1;
        return this;
    }

    public Vec normalizeIfTooBig()
    {
        if (sum > 1e20)
        {
            normalise();
        }
        return this;
    }

    @Override
    public ProbVec set(Random random, double noise, final TypeAdd type)
    {

        Utils.set(counts, random, noise, type);
        return computeSum();
    }
    
    @Override
    public ProbVec set(Random random, double min, double max, double noise)
    {

        Utils.set(counts, random, min, max, noise);
        return computeSquaredSum();
    }

    @Override
    public ProbVec set(double x)
    {
        Utils.set(counts, x);
        return computeSum();
    }
    @Override
    public void set(int pos, double x)
    {
        setUnsafe(pos, x);
        computeSum();
    }
    
    @Override
    public void setUnsafe(int pos, double x)
    {
        assert pos < counts.length;
        counts[pos] = x;        
    }
    
    public ProbVec div(double x)
    {
        Utils.div(counts, x);
        return computeSum();
    }

    public int sample(Random random)
    {
        final double target = random.nextDouble() * sum;
        int i = -1;
        double accum = 0.0;
        while (accum < target)
        {
            i += 1;
            accum += counts[i];
        }
        return i;
    }

    @Override
    public int size()
    {
        return counts.length;
    }
    
    public ProbVec computeSum()
    {
        sum = Utils.sum(counts);
        return this;
    }
    
    public ProbVec computeSquaredSum()
    {
        sum = Utils.sumSquared(counts);
        return this;
    }
    
     /**
     * 
     * Computes useful statistics for the vector on the non-zero elements only.
     * These include the average, the absolute value average, positive values only average, 
     * negative values only average, max positive value, max negative value, num of positive values,
     * num of negative values, sparsity, L1-norm, count
     * @return 
     */
    @Override
    public double[] vecStatsNonZeros()
    {
        double sumNonZero = 0;
        double positiveSum = 0, negativeSum = 0, minNegative = 0, maxPositive = 0;
        int counter = 0, positiveCounter = 0, negativeCounter = 0;
        double l1Norm = 0;
        for(double value : counts)
        {
            if(value != 0)
            {
                sumNonZero += value;
                l1Norm += Math.abs(value);
                if(value > 0)
                {
                    positiveSum += value;
                    positiveCounter++;
                    if(value > maxPositive)
                        maxPositive = value;
                }
                else
                {
                    negativeSum += value;
                    negativeCounter++;
                    if(value < minNegative)
                        minNegative = value;
                }
                counter++;
            }
        }
        return new double[] {sumNonZero / (double) counter, l1Norm / (double) counter, 
            positiveSum / (double) positiveCounter, negativeSum / (double) negativeCounter,
            maxPositive, minNegative, 
            (double) counter / (double) size(), l1Norm, positiveCounter, negativeCounter, counter};
    }
    
    @Deprecated
    public static ProbVec zeros(int n)
    {
        return new ProbVec(new double[n], 0, 0);
    }   
    @Deprecated
    public static ProbVec[] zeros2(int n1, int n2)
    {
        ProbVec[] result = new ProbVec[n1];
        for(int i = 0; i < n1; i++)
        {
            result[i] = zeros(n2);
        }
        return result;
    }    
    @Deprecated
    public static ProbVec[][] zeros3(int n1, int n2, int n3)
    {
        ProbVec[][] result = new ProbVec[n1][n2];
        for(int i = 0; i < n1; i++)
        {
            result[i] = zeros2(n2, n3);
        }
        return result;
    }

    public static void main(String[] args)
    {
        double[] a1 = new double[3]; a1[0] = 0; a1[1] = 1; a1[2] = 2;
        ProbVec v1 = new ProbVec(a1, 3, 3);
        String[] labels = {"test1", "test2", "test3"};
        v1.labels = labels;
        Set<Pair<Integer>> a2 = v1.getProbsSorted();
        v1.setProbSortedIndices();
        Set<Pair<Integer>> a3 = v1.getProbsSorted();        
        System.out.println("");
    }
}
