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

import org.apache.commons.math.linear.OpenMapRealVector;

/**
 *
 * @author sinantie
 */
public class VecFactory
{
    public static enum Type {DENSE, SPARSE, MAP};
    
    public static Vec zeros(Type type, int n)
    {
        switch(type)
        {            
            case SPARSE : return new SparseVec(new OpenMapRealVector(n), 0, 0);
            case MAP : return new MapVec();
            default: case DENSE : return new ProbVec(new double[n], 0, 0);
        }        
    }   

    public static Vec[] zeros2(Type type, int n1, int n2)
    {
        Vec[] result = type == Type.DENSE ? new ProbVec[n1] : new SparseVec[n1];
        for(int i = 0; i < n1; i++)
        {
            result[i] = zeros(type, n2);
        }
        return result;
    }    

    public static Vec[][] zeros3(Type type, int n1, int n2, int n3)
    {
        Vec[][] result = type == Type.DENSE ? new ProbVec[n1][n2] : new SparseVec[n1][n2];
        for(int i = 0; i < n1; i++)
        {
            result[i] = zeros2(type, n2, n3);
        }
        return result;
    }
    
    public static Vec[][] zeros3(Type type, int n1, int n2, int[] n3)
    {
        Vec[][] result = type == Type.DENSE ? new ProbVec[n1][n2] : new SparseVec[n1][n2];
        for(int i = 0; i < n1; i++)
        {
            //result[i] = new Vec[n2];
            for(int j = 0; j < n2; j++)
            {
                result[i][j] = zeros(type, n3[i]);
            }
        }
        return result;
    }
    
    public static OpenMapRealVector copyFromArray(double[] values)
    {
        return new OpenMapRealVector(values);
    }
}
