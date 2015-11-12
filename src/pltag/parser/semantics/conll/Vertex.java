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
package pltag.parser.semantics.conll;

/**
 *
 * @author sinantie
 */
public class Vertex
{
       
    public enum Direction {UP, DOWN};    
    int id;
    String depRel, posTag;
    Direction direction;
    
    public Vertex(int id)
    {
        this(id, null, null, null);
    }
    
    public Vertex(int id, String depRel, String posTag, Direction direction)
    {
        this.id = id;
        this.depRel = depRel;
        this.posTag = posTag;
        this.direction = direction;    
    }
  
    public String getDepRel()
    {
        return depRel;
    }

    public Direction getDirection()
    {
        return direction;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof Vertex;
        Vertex v = (Vertex)obj;
        return id == v.id;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 29 * hash + this.id;
        return hash;
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s%s", id, depRel, direction == Direction.UP ? "^" : "_");
    }

    
}
