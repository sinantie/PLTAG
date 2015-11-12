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

/**
 *
 * @author sinantie
 */
public class FeatureVec
{
    private final int numOfFeatures;
    private final FeatureInterface[] vec;
    private int label;
    
    public FeatureVec(int size)
    {
        this.numOfFeatures = size;
        vec = new FeatureInterface[size];
    }

    public FeatureVec(FeatureVec featureVecIn)
    {
        numOfFeatures = featureVecIn.numOfFeatures;  
        vec = featureVecIn.vec;
        label = featureVecIn.label;
    }
    
    public void addFeature(FeatureInterface feature, int position)
    {
        vec[position] = feature;
    }

    public void setLabel(int label)
    {
        this.label = label;
    }

    public int getLabel()
    {
        return label;
    }
        
    public int getNumOfFeatures()
    {
        return numOfFeatures;
    }
    
    public Feature[] getSparseRepresentation()
    {
        // int get total size of feature vector
        Feature[] out = new FeatureNode[numOfFeatures];
        int startPos = 0;
        int i = 0;
        for(FeatureInterface feat : vec)
        {
            out[i++] = feat.outputToSparseRepresentation(startPos);
            // shift right based on size of the feature representation 
            // (e.g., binary size=2, double=1, categorical=k, where k=num of categories, 
            // according to a 1-of-k representation
            startPos += feat.getSizeOfRepresentation(); 
        }
        return out;
    }
        
    public int getDenseFeatureVecLength()
    {
        int total = 0;
        for(FeatureInterface feat : vec)
        {
            total += feat.getSizeOfRepresentation();
        }
        return total;
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        int startPos = 0;
        str.append(label);
        for(FeatureInterface feat : vec)
        {
            str.append(" ").append(feat.toString(startPos));
            // shift right based on size of the feature representation 
            // (e.g., binary size=2, double=1, categorical=k, where k=num of categories, 
            // according to a 1-of-k representation
            startPos += feat.getSizeOfRepresentation(); 
        }
        return str.toString();
    }
    
    
}
