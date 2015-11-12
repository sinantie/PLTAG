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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author konstas
 */
public final class SuperTagFringe implements Serializable
{
    
    private static final long serialVersionUID = 1L;
    
    private Set<String> nodesRight;
    private Set<String> nodesLeft;    
    private String substNode;
    private boolean noFringe = false;
    
    public SuperTagFringe() {}
    
    public SuperTagFringe(Set<String> nodesRight, Set<String> nodesLeft, String substNode)
    {
        this.nodesRight = nodesRight;
        this.nodesLeft = nodesLeft;        
        this.substNode = substNode;
    }
        
    public SuperTagFringe(Fringe fringe)
    {        
        nodesRight = new TreeSet<String>();
        for(Node node : fringe.getAdjNodesOpenRight())
        {
            nodesRight.add(node.getCategory());
        }
        nodesLeft = new TreeSet<String>();
        for(Node node : fringe.getAdjNodesOpenLeft())
        {
            nodesLeft.add(node.getCategory());
        }
        substNode = fringe.getSubstNode() == null ? "null" : fringe.getSubstNode().getCategory();
    }
    
    public SuperTagFringe(String fringe, boolean fullFringe)
    {        
        if(fullFringe)
        {
            String openRight = fringe.substring(1, fringe.indexOf("]["));
            String openLeft = fringe.substring(fringe.indexOf("][") + 2, fringe.indexOf("]:"));
            nodesRight = new TreeSet<String>();
            nodesRight.addAll(Arrays.asList(openRight.split(", ")));
            nodesLeft = new TreeSet<String>();        
            nodesLeft.addAll(Arrays.asList(openLeft.split(", ")));
            substNode = fringe.substring(fringe.lastIndexOf(":") + 1);
        }
        else // in case we only pass the (candidate) most likely integration point of the fringe
        {
            noFringe = true;
            substNode = fringe;
        }
    }
    
    public SuperTagFringe(String fringe)
    {        
            this(fringe, fringe.contains("]["));        
    }

    public SuperTagFringe(SuperTagFringe st)
    {
        nodesRight = new TreeSet<String>();
        for(String s : st.nodesRight)
            nodesRight.add(s);
        nodesLeft = new TreeSet<String>();
        for(String s : st.nodesLeft)
            nodesLeft.add(s);
        substNode = st.substNode;
        noFringe = st.noFringe;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof SuperTagFringe;
        SuperTagFringe other = (SuperTagFringe)obj;
        return noFringe ? substNode.equals(other.substNode) : 
               nodesRight.equals(other.nodesRight) && 
               nodesLeft.equals(other.nodesLeft) && 
               substNode.equals(other.substNode);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + (this.nodesLeft != null ? this.nodesLeft.hashCode() : 0);
        hash = 71 * hash + (this.nodesRight != null ? this.nodesRight.hashCode() : 0);
        hash = 71 * hash + (this.substNode != null ? this.substNode.hashCode() : 0);
        return hash;
    }

    public Set<String> getPrefixNodesLeft()
    {
        return nodesLeft;
    }

    public Set<String> getPrefixNodesRight()
    {
        return nodesRight;
    }        

    public String getSubstNode()
    {
        return substNode;
    }

    @Override
    public String toString()
    {
        if(noFringe)
            return substNode;                    
        return new StringBuilder().append(nodesRight).append(nodesLeft).append(":").append(substNode).toString();
    }
    
    
}
