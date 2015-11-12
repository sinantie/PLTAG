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
 * @author konstas
 */
public final class DepNode extends SemNode implements Serializable
{

    static final long serialVersionUID = -1L;
    String category, posTag, elemTree; 
    boolean shadow;    
    final int timestamp;

    public DepNode(int timestamp)
    {
        this(Integer.MIN_VALUE, timestamp);        
    }
    
    public DepNode(int id, int timestamp)
    {
        this(id, timestamp, false);        
    }
    
    public DepNode(String category, String posTag, int timestamp)
    {
        this(category, posTag, Integer.MIN_VALUE, timestamp);        
    }            
    
    public DepNode(int id, int timestamp, boolean isShadow)
    {
        this(null, null, id, timestamp, isShadow, null);        
    }
     
    public DepNode(String category, String posTag, int id, int timestamp)
    {
        this(category, posTag, id, timestamp, false, null);        
    }
    
    public DepNode(String category, String posTag, int id, String elemTree, int timestamp)
    {
        this(category, posTag, id, timestamp, false, elemTree);        
    }
    
    public DepNode(String category, String posTag, int id, int timestamp, boolean isShadow)
    {
        this(category, posTag, id, timestamp, isShadow, null);
    }
    
    public DepNode(String category, String posTag, int id, int timestamp, boolean isShadow, String elemTree)
    {
        super(id);
        this.category = category;  
        this.posTag = posTag;
        this.shadow = isShadow;
        this.timestamp = timestamp;
        this.elemTree = elemTree;
    }       

    public DepNode(DepNode node)
    {
        this(node.category, node.posTag, node.id, node.timestamp, node.shadow, node.elemTree);        
    }
    
    public boolean isShadow()
    {
        return shadow;
    }

    public boolean isIncomplete()
    {
        return shadow || category == null;
    }
       
    
    public void setCategory(String category)
    {
        this.category = category;
    }    

    public void setPosTag(String posTag)
    {
        this.posTag = posTag;
    }   

    @Override
    public boolean equals(Object obj)
    {
        DepNode d = (DepNode)obj;
        if (id == Integer.MIN_VALUE)
        {
            return timestamp == d.timestamp;
        }
        return super.equals(obj);
    }

    public String prettyPrint()
    {
        return category != null ? category : "-";
    }
    
    @Override
    public String toString()
    {
        return String.format("%s:%s:t=%s", id, category, timestamp+1); // word id's are 1-based
    }

}
