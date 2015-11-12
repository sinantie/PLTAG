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

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import fig.basic.Indexer;

/**
 * Wrapper class for categorical features. The class supports conversion to 
 * 1-of-K representation, used in classifiers that accept only numerical values 
 * such as liblinear.
 * 
 * @author sinantie
 */
class CategoricalFeature implements FeatureInterface
{
    /**
     * a map of values to ascending unique ids
     */
    private final Indexer<String> indexer;
    /**
     * id corresponds to the position of the value in the indexer stored in this feature
     */
    private final int id;

    public CategoricalFeature(FeatureIndexers indexers, int fType, int id)
    {
        indexer = indexers.getIndexer(fType);
        this.id = id;
    }
    
    /**
     * Return a 1-of-k representation of the value stored in this feature. 
     * @return 
     */
    @Override
    public double[] getRepresentation()
    {
        if(id > indexer.size())
            return null;
        double[] out = new double[indexer.size()];
        out[id] = 1.0;
        return out;
    }

    @Override
    public int getSizeOfRepresentation()
    {
        return indexer.size();
    }

    /**
     * Output value to 1-of-k internal representation for liblinear. We only output
     * the featureNode with a value of 1, which corresponds to the k'th position in the
     * 1-of-k representation of this feature.
     * @param startPos the starting position in the dense representation of the vector
     * @param out 
     */
    @Override
    public Feature outputToSparseRepresentation(int startPos)
    {                    
        // bug fix (2014-12-18): during test time an unknown feature (denoted with <UNK>) 
        // has been identified for the first feature template, i.e., startPos=0 and id=0.
        // This will automatically, incur a value of '0:1.0' for the new FeatureNode, which
        // is a violation for liblinear, which is 1-based. To overcome this issue, we simply
        // point it to the index of the next categorical value for the <UNK> placeholder, 
        // *assuming* that the next feature template is categorical.
        return new FeatureNode(startPos == 0 && id == 0 ? indexer.size() : startPos + id, 1.0d);        
    }

    public String toString(int startPos)
    {
        return String.format("%s:%s", String.valueOf(startPos + id), String.valueOf(1.0d));
    }
    
    
}
