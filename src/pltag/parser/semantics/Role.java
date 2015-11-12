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

import fig.basic.Indexer;
import java.io.Serializable;

/**
 *
 * @author konstas
 */
public final class Role implements Serializable
{

    static final long serialVersionUID = -1L;
    final Integer roleNameId;
    Integer secondRoleNameId;
    final int nodeId;
    Indexer<String> roleIndexer;
    
    public Role(Indexer roleIndexer, Integer firstId, Integer secondId, int position)
    {
        this.roleIndexer = roleIndexer;
        this.roleNameId = firstId;
        this.secondRoleNameId = secondId;
        this.nodeId = position;
    }
    
    public Role(Indexer roleIndexer, Integer id, int position)
    {
        this.roleIndexer = roleIndexer;
        this.roleNameId = id;
        this.nodeId = position;
    }

    public Role(Role roleIn) // copy constructor
    {
        this(roleIn.roleIndexer, roleIn.roleNameId, roleIn.secondRoleNameId, roleIn.nodeId);
    }
    
    public int getNodeId()
    {
        return nodeId;
    }
    
    public boolean sameName(Role role)
    {
        if(role.roleNameId == null)
            return false;
        return roleNameId.equals(role.roleNameId);
    }
    
    @Override
    public boolean equals(Object obj)
    {
//        assert obj instanceof Role;
        Role r = (Role) obj;
        return roleNameId.equals(r.roleNameId) && nodeId == r.nodeId;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 89 * hash + (this.roleNameId != null ? this.roleNameId.hashCode() : 0);
//        hash = 89 * hash + (this.secondRoleNameId != null ? this.secondRoleNameId.hashCode() : 0);
        hash = 89 * hash + this.nodeId;
        return hash;
    }

    

    @Override
    public String toString()
    {
        return roleIndexer.getObject(roleNameId);
    }
}
