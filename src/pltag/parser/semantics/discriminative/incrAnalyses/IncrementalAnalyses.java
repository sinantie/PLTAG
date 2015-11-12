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
package pltag.parser.semantics.discriminative.incrAnalyses;

import java.io.Serializable;
import pltag.parser.semantics.discriminative.DiscriminativeFeatureIndexers;
import pltag.parser.semantics.discriminative.DiscriminativeParams;

/**
 *
 * @author sinantie
 */
public class IncrementalAnalyses implements Serializable
{
    
    private static final long serialVersionUID = -1L;
    private final NbestIncrementalAnalyses[] nbestAnalyses;
    
    public IncrementalAnalyses(int words)
    {
        nbestAnalyses = new NbestIncrementalAnalyses[words];        
    }

    public NbestIncrementalAnalyses[] getNbestAnalyses()
    {
        return nbestAnalyses;
    }
    
    public void addNbestAnalyses(NbestIncrementalAnalyses nbestIncrAnalyses, int pos)
    {
        nbestAnalyses[pos] = nbestIncrAnalyses;
    }

    public String toString(DiscriminativeFeatureIndexers featureIndexers, DiscriminativeParams params)
    {
        StringBuilder str = new StringBuilder();
        str.append("{");
        for(NbestIncrementalAnalyses incr : nbestAnalyses)
        {
            str.append(incr.toString(featureIndexers, params)).append(",");
        }
        str.append("}");
        return str.toString();
    }

    
}
