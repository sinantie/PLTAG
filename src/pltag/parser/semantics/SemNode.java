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
package pltag.parser.semantics;

import java.io.Serializable;

/**
 *
 * @author sinantie
 */
public abstract class SemNode implements Serializable
{
    
    static final long serialVersionUID = -1L;
    protected int id;

    public SemNode()
    {
    }
    
    public SemNode(int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }
    
    @Override
    public boolean equals(Object obj)
    {
//        assert obj instanceof SemNode;
        DepNode d = (DepNode) obj;

        return id == d.id;  // check
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 89 * hash + this.id;
        return hash;
    }
}
