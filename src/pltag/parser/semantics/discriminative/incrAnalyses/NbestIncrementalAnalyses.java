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
public class NbestIncrementalAnalyses implements Serializable
{

    private static final long serialVersionUID = -1L;
    IncrementalAnalysis[] incrAnalyses;

    public NbestIncrementalAnalyses()
    {
    }

    public NbestIncrementalAnalyses(int nbest)
    {
        incrAnalyses = new IncrementalAnalysis[nbest];
    }

    public void addIncrementalAnalysis(IncrementalAnalysis analysis, int pos)
    {
        incrAnalyses[pos] = analysis;
    }

    public IncrementalAnalysis[] getIncrAnalyses()
    {
        return incrAnalyses;
    }

    public String toString(DiscriminativeFeatureIndexers featureIndexers, DiscriminativeParams params)
    {
        StringBuilder str = new StringBuilder();
        str.append("[");
        for(IncrementalAnalysis incr : incrAnalyses)
        {
            str.append(incr.toString(featureIndexers, params)).append(",");
        }
        str.append("]");
        return str.toString();
    }
    
}
