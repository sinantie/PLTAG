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

import java.util.Random;
import java.util.Set;
import pltag.util.Pair;
import pltag.util.Utils.TypeAdd;

/**
 *
 * @author konstas
 */
public interface Vec
{

    public double getCount(int i);
    public Vec addCount(double x);
    public Vec addCount(int i, double x);  
    public Vec addCount(Vec vec, double x);
    public Vec addCount(Vec vec);
    public Vec addCount(double[] phi, double x);
    public Vec addCountKeepNonNegative(int i, double x);
    public Vec addProb(int i, double x);
    public Vec addProb(double[] phi, double x);
    public Vec addProbKeepNonNegative(int i, double x);
    public void copyDataFrom(Vec v);
    public Vec div(double x);
    public Vec expDigamma();
    public Pair getAtRank(int rank);
    public double[] getCounts(); // for serialisation use only
    public Set<Pair<Integer>> getCountsSorted();
    public String[] getLabels(); // for serialisation use only
    public int getMax();
    public double getOldSum();
    public double[] getProbs();
    public Set<Pair<Integer>> getProbsSorted();
    public double getProb(int i);
    public double getSum(); // for serialisation use only
    public Vec normalise();
    public Vec normalizeIfTooBig();
    public int sample(Random random);
    public void saveSum();
    public Vec set(double x);
    public void set(int pos, double x);
    public void setUnsafe(int pos, double x);
    public Vec set(final Random random, final double noise, final TypeAdd type);    
    public Vec set(final Random random, final double min, final double max, final double noise);    
    public void setCountToObtainProb(int i, double p);
    public void setCountsSortedIndices();
    public void setProbSortedIndices();
    public int size();    
    public double[] vecStatsNonZeros();
}
