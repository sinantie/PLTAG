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

import java.io.Serializable;
import pltag.parser.SuperTagFringe;
import pltag.parser.semantics.discriminative.DiscriminativeFeatureIndexers;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public class Argument implements Serializable
{
    
    private static final long serialVersionUID = 1L;
    
    int timestamp;
    transient String role, form, posTag, elemTree, integrationPoint, operationPath, depRel; // form is the actual word non-lemmatized    
    transient SuperTagFringe prefixFringeAlphaSet;
    int roleInt, formInt, posTagInt;
    transient DiscriminativeFeatureIndexers featureIndexers;    

    public Argument()
    {
    }
        
    public Argument(int timestamp, String role, String form, String posTag, String elemTree, String ip, SuperTagFringe prefixFringe, String operationPath, String depRel)
    {
        this.timestamp = timestamp;
        this.role = Utils.convertRoleFromPropbankToConll(role);
        this.form = form;
        this.posTag = posTag;
        this.elemTree = elemTree;
        this.integrationPoint = ip;
        this.prefixFringeAlphaSet = prefixFringe;
        this.operationPath = operationPath;
        this.depRel = depRel;
    }
    
    public Argument(int timestamp, String role, String form, String posTag, String depRel)
    {
        this(timestamp, Utils.convertRoleFromPropbankToConll(role), form, posTag, null, null, null, null, depRel);        
    }
    
    public Argument(int timestamp, String role, String form, String posTag)
    {        
        this(timestamp, role, form, posTag, null);        
    }
    
    /**
     * 
     * @param featureIndexers
     * @param timestamp
     * @param role
     * @param form
     * @param posTag 
     */
    public Argument(DiscriminativeFeatureIndexers featureIndexers, boolean train, int timestamp, String role, String form, String posTag)
    {        
        this.roleInt = role != null ? featureIndexers.getRoleIndex(Utils.convertRoleFromPropbankToConll(role)) : -1;
        this.formInt = form != null ? featureIndexers.getWordIndex(form, train) : -1;
        this.posTagInt = posTag != null ? featureIndexers.getPosIndex(posTag, train) : -1;
        this.timestamp = timestamp;
        this.featureIndexers = featureIndexers;
    }        
 
    public String getForm()
    {
        return form;
    }

    public int getFormInt()
    {
        return formInt;
    }

    public String getRole()
    {
        return role;
    }

    public int getRoleInt()
    {
        return roleInt;
    }

    public String getPosTag()
    {
        return posTag;
    }

    public int getPosTagInt()
    {
        return posTagInt;
    }

    public String getDepRel()
    {
        return depRel;
    }

    public int getTimestamp()
    {
        return timestamp;
    }

    public String getElemTree()
    {
        return elemTree;
    }

    public String getIntegrationPoint()
    {
        return integrationPoint;
    }

    public SuperTagFringe getPrefixFringeAlphaSet()
    {
        return prefixFringeAlphaSet;
    }

    public String getOperationPath()
    {
        return operationPath;
    }
    
    public boolean beforeTimeStamp(int t)
    {
        return timestamp <= t;
    }

    @Override
    public String toString()
    {
        boolean compactRepresentation = featureIndexers != null;
        return compactRepresentation ? String.format("%s@%s:t=%s", featureIndexers.getWord(formInt), featureIndexers.getRole(roleInt), timestamp) :
                String.format("%s@%s:t=%s", form, role, timestamp);
    }

    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof Argument;
        Argument a = (Argument)obj;
        boolean compactRepresentation = featureIndexers != null;
        return timestamp == a.timestamp && (compactRepresentation ? roleInt == a.roleInt : role.equals(a.role));
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 61 * hash + this.timestamp;
        hash = 61 * hash + (this.role != null ? this.role.hashCode() : 0);
        hash = 61 * hash + this.roleInt;
        return hash;
    }
    
    
}
