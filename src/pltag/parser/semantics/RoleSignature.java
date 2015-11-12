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
import java.util.HashMap;
import java.util.Map;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public class RoleSignature
{

    Map<Integer, Role> roles;
    int frequency;
    String treeString;
    Indexer<String> indexer;
    
    public RoleSignature(Indexer<String> indexer, String treeString, int frequency)
    {
        roles = new HashMap<Integer, Role>();
        this.treeString = treeString;
        this.frequency = frequency;
        this.indexer = indexer;
    }

    void addRole(String role, String category, int nodeId)
    {
        // check for multiple roles, e.g., NP@ARG1;ARG0
        String[] multRoles = role.split(";");
        if(multRoles.length > 1)
        {
            roles.put(nodeId, new Role(indexer, indexer.getIndex(multRoles[0]), indexer.getIndex(multRoles[0]), nodeId));
//            roles.put(nodeId, new Role(indexer, indexer.getIndex(Utils.convertRoleFromPropbankToConll(multRoles[0])), 
//                    indexer.getIndex(Utils.convertRoleFromPropbankToConll(multRoles[0])), nodeId));
        }
        else
            roles.put(nodeId, new Role(indexer, indexer.getIndex(role), nodeId));
//            roles.put(nodeId, new Role(indexer, indexer.getIndex(Utils.convertRoleFromPropbankToConll(role)), nodeId));
    }
    
    public Map<Integer, Role> getRoles()
    {
        return roles;
    }
    
    public int numOfRoles()
    {
        return roles.size();
    }

    public boolean isEmpty()
    {
        return roles.isEmpty();
    }

    public String getTreeString()
    {
        return treeString;
    }

    public void setTreeString(String treeString)
    {
        this.treeString = treeString;
    }

    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof RoleSignature;
        RoleSignature rs = (RoleSignature) obj;
        return roles.equals(rs.roles);
        
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 73 * hash + (this.roles != null ? this.roles.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString()
    {
        return roles.toString();
    }
}
