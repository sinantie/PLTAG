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

import fig.basic.Pair;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import pltag.parser.performance.IncrementalBracketWidget;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.Predicate;
import pltag.parser.semantics.conll.Proposition;

/**
 *
 * @author sinantie
 */
public class IncrementalSemanticsWidget
{
    private final Map<Predicate, Proposition> propositions;
    private final Set<Predicate> predicatesWithSense;
    private final Set<String> predicates;
    private final Bag<String> argWords, incompleteArcs;    
    private final Set<Pair<Integer, Argument>> argRoles;
    private IncrementalBracketWidget incrBracketWidget;
    private int timestamp;
    private boolean fullSentence;
    
    /**
     * 
     * Constructor for predicted output. We pass only the current timestamp for use
     * during evaluation.
     * @param timestamp 
     * @param fullSentence 
     */
    public IncrementalSemanticsWidget(int timestamp, boolean fullSentence)
    {
        this(null, timestamp, fullSentence);
    }
    
    /**
     * Constructor for (usually) gold-standard propositions
     * @param propositions 
     */
    public IncrementalSemanticsWidget(Map<Predicate, Proposition> propositions)
    {
        this(propositions, -1, true);
    }
    
    private IncrementalSemanticsWidget(Map<Predicate, Proposition> propositions, int timestamp, boolean fullSentence)
    {
        this.propositions = propositions;
        predicatesWithSense = new HashSet<Predicate>();
        predicates = new HashSet<String>();
        argWords = new HashBag<String>();
        argRoles = new HashSet<Pair<Integer, Argument>>();
        incompleteArcs = new HashBag<String>();
        this.timestamp = timestamp;
        this.fullSentence = fullSentence;
    }

    /**
     * Convert propositions to bags of predicates and argument words (no role) and
     * predicate-argument pairs (with roles), up to a given timestamp.
     * @param timestamp 
     * @return  
     */
    public boolean convertPropositionsAt(int timestamp)
    {
        clear();
        if(propositions == null)
            return false;
        for(Predicate pred : propositions.keySet())
        {
            if(pred.beforeTimeStamp(timestamp))
            {
                predicatesWithSense.add(pred);
                predicates.add(String.format("%s:%s", pred.getTimestamp(), pred.getLemma()));
                incompleteArcs.add(String.format("%s:%s", pred.getTimestamp(), pred.getLemma()));
            }
        }
        
        for(Proposition prop : propositions.values())
        {
            List<Argument> args = prop.getArgsAtTimeStamp(timestamp);
            for(Argument arg : args)
            {
                argWords.add(String.format("%s:%s", arg.getTimestamp(), arg.getForm()));
                incompleteArcs.add(String.format("%s:%s", arg.getTimestamp(), arg.getForm()));
                // add timestamp of predicate instead of predicate, as we allow incorrect sense. 
                // We penalise an incorrect sense when evaluating predicates alone.
                int predTimeStamp = prop.getPredicate().getTimestamp();
                if(predTimeStamp <= timestamp)
                    argRoles.add(new Pair<Integer, Argument>(predTimeStamp, arg));
            }
        }
        return true;
    }
    
    private void clear()
    {
        predicatesWithSense.clear();
        predicates.clear();
        argWords.clear();
        argRoles.clear();
        incompleteArcs.clear();
    }

    public Set<Predicate> getPredicatesWithSense()
    {
        return predicatesWithSense;
    }
        
    public void addPredicateWithSense(Predicate predicate)
    {
        predicatesWithSense.add(predicate);
    }

    public Set<String> getPredicates()
    {
        return predicates;
    }
    
    public void addPredicate(String predicate)
    {
        predicates.add(predicate);
    }

    public Bag<String> getArgWords()
    {
        return argWords;
    }
    
    public void addArgWord(String argWord)
    {
        argWords.add(argWord);
    }

    public Set<Pair<Integer, Argument>> getArgRoles()
    {
        return argRoles;
    }
    
    public void addArgRole(Pair<Integer, Argument> argRole)
    {
        argRoles.add(argRole);        
    }

    public Bag<String> getIncompleteArcs()
    {
        return incompleteArcs;
    }
    
    public void addIncompleteArc(String word)
    {
        incompleteArcs.add(word);
    }
    
    public int getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(int timestamp)
    {
        this.timestamp = timestamp;
    }

    public boolean isFullSentence()
    {
        return fullSentence;
    }

    public void setFullSentence(boolean fullSentence)
    {
        this.fullSentence = fullSentence;
    }
        
    public Map<Predicate, Proposition> getPropositions()
    {
        return propositions;
    }

    public IncrementalBracketWidget getIncrBracketWidget()
    {
        return incrBracketWidget;
    }
        
    public void setIncrBracketWidget(IncrementalBracketWidget incrBracketWidget)
    {
        this.incrBracketWidget = incrBracketWidget;
    }        
    
    public String getTreeAt(int timestamp)
    {
        return incrBracketWidget.getTreeAt(timestamp);
    }
}
