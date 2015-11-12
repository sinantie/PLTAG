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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import pltag.util.Pair;
import pltag.util.Utils;
import pltag.util.Utils.TypeAdd;

/**
 * A sparse representation of feature counts, that is arbitrarily scaling up. 
 * We don't need to set the length beforehand (not useful for probability vectors)
 * 
 * @author konstas
 */
public class MapVec implements Serializable, Vec
{
    static final long serialVersionUID = -1L;
    private Map<Integer, Double> counts;
    private int[] sortedIndices;
    private String[] labels;
    private double sum;
    
    public MapVec()
    {
        counts = new HashMap<Integer, Double>();
    }
    
    public MapVec(String[] labels)
    {
        this();
        this.labels = labels;
    }

    public String[] getLabels()
    {
        return labels;
    }
    
    public void setData(Map<Integer, Double> counts, String[] labels)
    {
        this.counts = counts;
        this.labels = labels;
    }

    @Override
    public void copyDataFrom(Vec v)
    {
        assert v instanceof MapVec;
        MapVec mv = (MapVec)v;
        this.counts = mv.counts;
        this.labels = mv.labels;
    }
    
    private Set<Integer> getKeys()
    {
        return counts.keySet();
    }
    
    public Map<Integer, Double> getMapCounts()
    {
        return counts;
    }
    
    @Override
    public double getCount(int i)
    {
        return !counts.containsKey(i) ? 0 : counts.get(i);
    }
    
    @Override
    public Vec addCount(double x)
    {
        for(Entry<Integer, Double> entry : counts.entrySet())
            entry.setValue(entry.getValue() + x);
        return this;
    }
    
    @Override
    public Vec addCount(int i, double x)
    {
        counts.put(i, x);
        return this;
    }
    
    @Override
    public Vec addCount(Vec vec, double x)
    {
        MapVec mv = (MapVec)vec;
        for(Integer otherKey: mv.getKeys())
            counts.put(otherKey, x + mv.getCount(otherKey));
        return this;
    }
    
    @Override
    public Vec addCount(Vec vec)
    {
        MapVec mv = (MapVec)vec;
        counts.putAll(mv.counts);        
        return this;
    }
    
    @Override
    public Set<Pair<Integer>> getProbsSorted()
    {
        return getCountsSorted();
    }
    
    @Override
    public Set<Pair<Integer>> getCountsSorted()
    {
        int length = counts.size();        

        TreeSet<Pair<Integer>> pairs = new TreeSet<Pair<Integer>>();
        // sort automatically by probability (pair.value)
        for(int i = 0; i < length; i++)
        {
            pairs.add(new Pair(counts.get(i), new Integer(i)));
        }
        return pairs.descendingSet();
    }
    
    @Override
    public void setProbSortedIndices()
    {
        setCountsSortedIndices();
    }
    
    @Override
    public void setCountsSortedIndices()
    {
        sortedIndices = new int[counts.size()];
        int i = 0;
        for(Pair p: getCountsSorted())
        {
            sortedIndices[i++] = (Integer)p.label;
        }
    }
    
    @Override
    public Pair getAtRank(int rank)
    {
        return new Pair(counts.get(sortedIndices[rank]), sortedIndices[rank]);
    }
    
    @Override
    public Vec set(double x)
    {
        for(Entry<Integer, Double> entry : counts.entrySet())
            entry.setValue(entry.getValue() + x);
        return this;        
    }
    
    @Override
    public void set(int pos, double x)
    {
        setUnsafe(pos, x);
    }
    
    @Override
    public void setUnsafe(int pos, double x)
    {
//        assert pos < counts.size();
        counts.put(pos, x);
    }

    public int getMax()
    {
        int index = -1;
        double maxCount = Double.NEGATIVE_INFINITY;
        for(Entry<Integer, Double> mapEntry : counts.entrySet())
        {
            double value = mapEntry.getValue();
            if(value > maxCount)
            {
                index = mapEntry.getKey();
                maxCount = value;
            }
        }
        return index;
    }
    
    public int size()
    {
        return counts.size();
    }
    
    @Override
    public Vec addCount(double[] phi, double x)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec addCountKeepNonNegative(int i, double x)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec addProb(int i, double x)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec addProb(double[] phi, double x)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec addProbKeepNonNegative(int i, double x)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec div(double x)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec expDigamma()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getCounts()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getOldSum()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getProb(int i)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getSum()
    {
        return sum;
    }

    @Override
    public Vec normalise()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec normalizeIfTooBig()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveSum()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vec set(Random random, double noise, TypeAdd type)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getProbs()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int sample(Random random)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCountToObtainProb(int i, double p)
    {
        throw new UnsupportedOperationException("Not supported yet.");
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
        for(double value : counts.values())
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

    @Override
    public Vec set(Random random, double min, double max, double noise)
    {
        Utils.set(counts, random, min, max, noise);
        return computeSquaredSum();
    }
    
    public MapVec computeSum()
    {
        sum = Utils.sum(counts);
        return this;
    }
    
    public MapVec computeSquaredSum()
    {
        sum = Utils.sumSquared(counts);
        return this;
    }
}
