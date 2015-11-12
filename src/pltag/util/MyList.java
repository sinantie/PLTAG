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

import java.util.ArrayList;

/**
 *
 * @author konstas
 */
public class MyList<T> extends ArrayList<T[]>
{    

    @Override
    public boolean add(T... element)
    {
        return super.add(element);
    }

    public String toString(String tokenDelim, String elementDelim)
    {
        String out = "";
        for(T[] element : subList(0, size()))
        {
            for(int i = 0; i < element.length - 1; i++)
            {
                out += element[i] + tokenDelim;
            }
            out += element[element.length -1] + elementDelim;
        }
        return out.substring(0,out.length() - elementDelim.length());
    }
}
