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

import java.util.Map;
import pltag.parser.semantics.conll.Predicate;
import pltag.parser.semantics.conll.Proposition;

/**
 *
 * @author konstas
 */
public class SemanticsWidget
{
    private final String[] nbestTrees; 
    private String conllSentence;
    private Map<Predicate, Proposition> propositions;
    private boolean baseline;
    
    public SemanticsWidget(String[] trees, String conllSentence)
    {
        this.nbestTrees = trees;
        this.conllSentence = conllSentence;
        baseline = false;
    }

    public SemanticsWidget(String[] trees, Map<Predicate, Proposition> propositions)
    {
        this(trees, propositions, false);
    }
    
    public SemanticsWidget(Map<Predicate, Proposition> propositions)
    {
        this(null, propositions, false);
    }
    
    public SemanticsWidget(String[] trees, Map<Predicate, Proposition> propositions, boolean baseline)
    {
        this.nbestTrees = trees;
        this.propositions = propositions;
        this.baseline = baseline;
    }

    public String[] getNBestTrees()
    {
        return nbestTrees;
    }

    public String getConllSentence()
    {
        return conllSentence;
    }

    public Map<Predicate, Proposition> getPropositions()
    {
        return propositions;
    }

    public boolean isBaseline()
    {
        return baseline;
    }
       
    @Override
    public String toString()
    {
        return String.format("%s\n%s", nbestTrees, conllSentence != null ? conllSentence : propositions);
    }    
}
