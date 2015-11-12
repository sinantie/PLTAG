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

import java.util.ArrayList;
import java.util.Map;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import pltag.corpus.ElementaryStringTree;

public class ShadowStringTree
{

    private ElementaryStringTree tree;
    private byte[] indexChange;
    private Integer[] shadowRoots;
    private String integrationLambda;
    private short integrationLambdaId;
    private double predictProbability;
    private short absolutePosOfRootInPrefixTree, prefixIpiNode, elemIpiNode;

    private ShadowStringTree(ElementaryStringTree tree, byte[] indexChange, Integer[] roots, String integLambda, short integLambdaId, 
            double pp, short absolutePosOfRootInPrefixTree, short prefixIpiNode, short elemIpiNode)
    {
        this.tree = tree;
        this.indexChange = indexChange;
        shadowRoots = roots;
        this.integrationLambda = integLambda;
        this.integrationLambdaId = integLambdaId;
        this.predictProbability = pp;
        this.absolutePosOfRootInPrefixTree = absolutePosOfRootInPrefixTree;
        this.prefixIpiNode = prefixIpiNode;
        this.elemIpiNode = elemIpiNode;
    }

    public ShadowStringTree(ElementaryStringTree tree)
    {
        this.tree = tree;
        ArrayList<Integer> roots = tree.getShadowSourceTreesRootList();
        shadowRoots = (Integer[]) roots.toArray(new Integer[roots.size()]);
        this.indexChange = new byte[roots.size() + 1];
        for (int n : roots)
        {
            byte index = tree.getLowerIndex(n);
            if (index >= indexChange.length)
            {
                indexChange = replaceWithBiggerArray(index + 1);
            }
            indexChange[index] = index;
        }
        
    }

    private byte[] replaceWithBiggerArray(int i)
    {
        byte[] indexChange2 = new byte[i + 1];
        for (int j = 0; j < indexChange.length; j++)
        {
            indexChange2[j] = indexChange[j];
        }
        return indexChange2;
    }

    public ElementaryStringTree getTreeOrigIndex()
    {
        return tree;
    }

    public byte[] getIndexChange()
    {
        return indexChange;
    }

    public ElementaryStringTree getTreeNewIndex()
    {
        ElementaryStringTree treecopy = tree.copy();
        for (byte i = 0; i < indexChange.length; i++)
        {
            treecopy.replaceIndeces(i, indexChange[i]);
        }
        return treecopy;
    }

    public void replaceIndices(byte shadowindex, byte i)
    {
        for (int j = 1; j < indexChange.length; j++)
        {
            if (indexChange[j] > -1 && indexChange[j] == shadowindex)
            {
                indexChange[j] = i;
                return;
            }
        }
    }

    public void replaceIndeces(ArrayList<Byte> shadowindices, byte i)
    {
        for (byte lsi : shadowindices)
        {
            for (int j = 1; j < indexChange.length; j++)
            {
                if (indexChange[j] == lsi)
                {
                    indexChange[j] = i;
                }
            }
        }
    }

    public boolean hasShadows()
    {
        for (byte index : indexChange)
        {
            if (index > 0)
            {
                return true;
            }
        }
        return false;
    }

    public ShadowStringTree copy()
    {
        return new ShadowStringTree(tree, indexChange.clone(), shadowRoots, 
                this.integrationLambda, this.integrationLambdaId, this.predictProbability, 
                this.absolutePosOfRootInPrefixTree, this.prefixIpiNode, this.elemIpiNode);
    }

    private byte getIndexMapped(byte index)
    {
        return indexChange[index];
    }

    public byte getLowerIndex(Integer node)
    {
        byte origIndex = tree.getLowerIndex(node);
        if (origIndex <= 0)
        {
            return origIndex;
        }
        return getIndexMapped(origIndex);
    }

    public byte getUpperIndex(Integer node)
    {
        byte origIndex = tree.getUpperIndex(node);
        if (origIndex <= 0)
        {
            return origIndex;
        }
        return getIndexMapped(origIndex);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        for (byte i : indexChange)
        {
            sb.append(i).append(",");
        }
        sb.append("]").append(tree.getTreeString());
        return sb.toString();
    }

    public ArrayList<Integer> getShadowSourceTreesRootList()
    {
        ArrayList<Integer> shadowroots = new ArrayList<Integer>();
        for (Integer i : this.shadowRoots)
        {
            if (getLowerIndex(i) == 0)
            {
                continue;
            }
            shadowroots.add(i);
        }
        return shadowroots;
    }

    public String getIntegLambda()
    {
        return this.integrationLambda;
    }

    public void setIntegLambda(String integlambda)
    {
        this.integrationLambda = integlambda;
    }

    public short getIntegrationLambdaId()
    {
        return integrationLambdaId;
    }
    
    public void setIntegLambdaId(short integlambdaId)
    {
        this.integrationLambdaId = integlambdaId;
    }

    public void setPredictProb(double pp)
    {
        this.predictProbability = pp;
    }

    public double getProb()
    {
        return this.predictProbability;
    }

    /**
     * Keep track of the absolute position of a shadow tree in the prefix tree.
     * This entails offsetting all nodes by <code>absolutePosOfRootInPrefixTree</code> 
     * given the position it was used in the tree
     * (e.g., if the highest node id of the prefix tree is 29 then when an adjoining
     * operation takes place, all the nodes of the shadow tree will be shifted by 29).
     * There are cases (Up Substitution and Down Adjunction) when the substitution or
     * foot node on the shadow tree is replaced by the corresponding integration point
     * in the prefix tree, hence the parameters <code>prefixIpiNode</code> which holds
     * the id of the integration point node in the prefix tree, and <code>elemIpiNode</code>
     * which is the id of the corresponding node on the shadow tree.
     * @param absolutePosOfRootInPrefixTree the offset that signifies the absolute position of the root of the shadow tree
     * with respect to the prefix tree it gets adjoined to
     * @param prefixIpiNode the node id of the integration point on the prefix tree
     * @param elemIpiNode  the node id of the adjoining point on the shadow tree
     */
    public void setAbsolutePosOfShadowInPrefixTree(short absolutePosOfRootInPrefixTree, short prefixIpiNode, short elemIpiNode)
    {        
        this.absolutePosOfRootInPrefixTree = absolutePosOfRootInPrefixTree;
        this.elemIpiNode = elemIpiNode;
        this.prefixIpiNode = prefixIpiNode;
    }

    public Map<Short, Short> getOffsetNodeIds()
    {
        DualHashBidiMap<Short, Short> map = new DualHashBidiMap<Short, Short>();
        for(Integer id : tree.getNodes())
        {
            map.put(id.shortValue(), id == elemIpiNode ? prefixIpiNode : (short)(id + absolutePosOfRootInPrefixTree));
        }
        return map;
    }
       
}
