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
package pltag.util;

import fig.basic.Fmt;

/**
 *
 * @author konstas
 */
public class Pair<E> implements Comparable
{
    public double value;
    public E label;
//    public int fieldValue;
    private final Utils.Compare choice; // default is value

    public Pair(double value, E label)
    {
        this.value = value;
        this.label = label;
        this.choice = Utils.Compare.VALUE;
    }

    public Pair(double value, E label, Utils.Compare choice)
    {
        this.value = value;
        this.label = label;
        this.choice = choice;
    }

    @Override
    public int compareTo(Object o)
    {
        assert(o instanceof Pair);
        Pair p = (Pair)o;
        if(choice == Utils.Compare.LABEL)
        {            
            return ((String)label).compareTo((String)p.label);
        }
        else
        {
//            return value > p.value ? 1 : (value < p.value) ? -1 : 0;
            return value >= p.value ? 1 : -1;
        }
    }

    @Override
    public String toString()
    {
        return label.toString() + " (" + Fmt.D(value) + ")";
    }
    
    
}
