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

import pltag.parser.params.Vec;

/**
 *
 * @author konstas
 */
public class Feature
{
    private final Vec vec;
    private final int index;
    
    public Feature(Vec probVec, int index)
    {        
        this.vec = probVec;
        this.index = index;    
    }

    public int getIndex()
    {
        return index;
    }
        
    public double getValue()
    {
        return vec.getCount(index);
    }
    
    public void setValue(double value)
    {
        vec.setUnsafe(index, value);
    }
    
    public void increment(double value)
    {
        setValue(getValue() + value);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Feature other = (Feature) obj;
        if (this.vec != other.vec && (this.vec == null || !this.vec.equals(other.vec)))
        {
            return false;
        }
        return this.index == other.index;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 83 * hash + (this.vec != null ? this.vec.hashCode() : 0);
        hash = 83 * hash + this.index;
        return hash;
    }

    @Override
    public String toString()
    {
        return String.valueOf(index);
    }
    
    
}
