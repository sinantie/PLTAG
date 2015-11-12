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
package pltag.parser.semantics.classifier;

import fig.basic.Indexer;
import java.io.Serializable;

/**
 *
 * @author sinantie
 */
public abstract class AbstractFeatureIndexers implements Serializable
{
    
    private static final long serialVersionUID = -1L;
    
    /**
     * Get unique id of value from selected feature type.
     * @param fType
     * @param value
     * @param train
     * @return 
     */
    public int getIndexOfFeature(int fType, String value, boolean train)
    {
        if(train)
            return getIndexOfFeature(fType, value);
        else
            return getIndexOfFeatureSafe(fType, value);
    }
    
    /**
     * Get unique id of value from selected feature type.
     * @param indexer
     * @param value
     * @param train
     * @param compact Compact representation for unknown symbol in indexers
     * @return 
     */
    public int getIndexOfFeature(Indexer<String> indexer, String value, boolean train, boolean compact)
    {
        if(train)
            return getIndexOfFeature(indexer, value);
        else
            return getIndexOfFeatureSafe(indexer, value, compact);
    }
    
    /**
     * Get unique id of value from selected feature type. We make sure we return
     * the id of the special <UNK> placeholder for unseen values (Used during prediction 
     * when feature indexers have already been filled in during training). 
     * @param fType
     * @param value
     * @return 
     */
    public abstract int getIndexOfFeatureSafe(int fType, String value);
    
    /**
     * Get unique id of value from selected feature type. The corresponding indexer
     * is automatically resized.
     * @param fType
     * @param value
     * @return 
     */
    public abstract int getIndexOfFeature(int fType, String value);
    
    /**
     * Return id of feature value, or <UNK> if it's not in the feature set.
     * (Used during prediction when feature indexers have already been filled in during training). 
     * @param indexer
     * @param value
     * @param compact Compact representation for unknown symbol in indexers
     * @return 
     */
    protected int getIndexOfFeatureSafe(Indexer<String> indexer, String value, boolean compact)
    {
        synchronized(indexer)
        {
            if(!indexer.contains(value))
            {
                return indexer.getIndex(compact ? "U" : "<UNK>");
            }
            return indexer.getIndex(value);
        }        
    }
    
    /**
     * Get unique id of value from selected feature type. The indexer
     * is automatically resized.
     * @param indexer
     * @param value
     * @return 
     */
    protected int getIndexOfFeature(Indexer<String> indexer, String value)
    {        
        synchronized(indexer)
        {
            return indexer.getIndex(value);
        }        
    }
    
    public String getValueOfFeature(int fType, int id)
    {
        return getIndexer(fType).getObject(id);
    }
    
    public abstract Indexer<String> getIndexer(int fType);
}
