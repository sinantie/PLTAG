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
public class BinaryFeature implements FeatureInterface
{

    private final boolean value;

    public BinaryFeature(boolean value)
    {
        this.value = value;
    }
        
    @Override
    public double[] getRepresentation()
    {
        double[] out = new double[1];
        out[0] = value ? 1 : -1;
        return out;
    }

    @Override
    public int getSizeOfRepresentation()
    {
        return 1;
    }

    @Override
    public Feature outputToSparseRepresentation(int startPos)
    {
        return new FeatureNode(startPos, value ? 1.0d : -1.0d);
    }

    public String toString(int startPos)
    {
        return String.format("%s:%s", startPos, String.valueOf(value ? 1.0d : -1.0d));
    }
    
    
}
