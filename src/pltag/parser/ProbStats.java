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

/**
 *
 * @author konstas
 */
public class ProbStats
{
    private int n; // |x|: just for normalization
           // q(z|x) \propto p(x,z)^{1/temperature}
           // z^* = argmax_z q(z|x)
    private double logZ; // \sum_z p(x, z)^{1/temperature}
    private double logVZ; // p(x, z^*)    

    public ProbStats(int n, double logZ, double logVZ)
    {
        this.n = n;
        this.logZ = logZ;
        this.logVZ = logVZ;        
    }

    private double assertValid(double x)
    {
        return x; // Don't be picky
    }

    public void add(ProbStats that)
    {
        n += that.n;

        logZ += assertValid(that.logZ);
        logVZ += assertValid(that.logVZ);        
    }

    public int getN()
    {
        return n;
    }

    public double getAvg_logZ()
    {
        return logZ / (double)n;
    }

    public double getAvg_logVZ()
    {
        return logVZ / (double)n;
    }  
}
