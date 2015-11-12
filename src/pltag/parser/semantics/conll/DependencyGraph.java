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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pltag.parser.semantics.conll.Vertex.Direction;

/**
 *
 * @author sinantie
 */
public class DependencyGraph
{
    private final Map<Vertex, List<Vertex>> adjList;

    public DependencyGraph()
    {
        adjList = new HashMap<Vertex, List<Vertex>>();
    }

    void addDependency(int id, String depRel, String posTag, int headId, Direction direction)
    {
        Vertex v = new Vertex(id);
        List children = adjList.get(v);
        if(children == null)
        {
            children = new LinkedList<Vertex>();
            adjList.put(v, children);
        }
        children.add(new Vertex(headId, depRel, posTag, direction));       
    }
    
    List<Vertex> getPath(int srcId, int destId)
    {
        Vertex src = new Vertex(srcId);
        Vertex dest = new Vertex(destId);
        List<Vertex> path = new ArrayList<Vertex>();
        Set<Integer> visited = new HashSet<Integer>();
        dfsVisit(src, dest, path, visited);
        return path;
    }

    private boolean dfsVisit(Vertex src, Vertex dest, List<Vertex> path, Set<Integer> visited)
    {
        visited.add(src.id);
        if(src.equals(dest))
        {
            return true;
        }
        List<Vertex> children = adjList.get(src);
        if(children == null)
            return false;
        for(Vertex child : children) // explore edge (src, child_of_src)
        {
            if(!visited.contains(child.id))
            {                
                if(dfsVisit(child, dest, path, visited))
                {
                    path.add(child);
                    return true;
                }
            }
        }
        return false;
    }    
}
