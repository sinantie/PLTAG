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
import java.util.Collection;
import pltag.parser.semantics.discriminative.DiscriminativeFeatureIndexers;

public class Predicate implements Serializable
{

    private static final long serialVersionUID = 1L;
    
    public static enum Type {noun, verb};    
    int timestamp;    
    transient String sense, lemma, posTag, elemTree;
    Type type;
    int lemmaInt, posTagInt;
    transient DiscriminativeFeatureIndexers featureIndexers;

    public Predicate()
    {
    }

    
    public Predicate(String sense, String lemma, String posTag, Type type, String elemTree, int timestamp)
    {
        this.timestamp = timestamp;
        this.sense = sense;
        this.lemma = lemma;
        this.posTag = posTag;
        this.elemTree = elemTree;
        this.type = type;
    }
    
    public Predicate(String sense, String lemma, String posTag, Type type, int timestamp)
    {
        this(sense, lemma, posTag, type, null, timestamp);
    }
    
    /**
     * Compact representation of predicate, using ids instead of strings. The mappings to the original
     * strings is stored in the featureIndexers.
     * @param lemma
     * @param posTag
     * @param type
     * @param timestamp
     * @param featureIndexers 
     */
    public Predicate(DiscriminativeFeatureIndexers featureIndexers, boolean train, String lemma, String posTag, Type type, int timestamp)
    {
        this.sense = null;
        this.lemmaInt = lemma != null ? featureIndexers.getWordIndex(lemma, train) : -1;
        this.posTagInt = posTag != null ? featureIndexers.getPosIndex(posTag, train) : -1;
        this.type = type;
        this.elemTree = null; 
        this.timestamp = timestamp;
        this.featureIndexers = featureIndexers;
    }
        
    public Predicate(String relation, String posTag, Type type, int timestamp)
    {
        String[] ar = relation.split("\\.");
        lemma = ar[0];
        sense = ar[1];
        this.posTag = posTag;        
        this.type = type;
        this.timestamp = timestamp;
    }

    public Predicate(Predicate predIn)
    {
        this(predIn.sense, predIn.lemma, predIn.posTag, predIn.type, predIn.elemTree, predIn.timestamp);       
    }

    public boolean beforeTimeStamp(int t)
    {
        return timestamp <= t;
    }
    
    public void setElemTree(String elemTree)
    {
        this.elemTree = elemTree;
    }

    public String getElemTree()
    {
        return elemTree;
    }
    
    public int getTimestamp()
    {
        return timestamp;
    }

    public String getLemma()
    {
        return lemma;
    }

    public int getLemmaInt()
    {
        return lemmaInt;
    }

    public String getSense()
    {
        return sense;
    }

    public String getPosTag()
    {
        return posTag;
    }

    public int getPosTagInt()
    {
        return posTagInt;
    }

    public static Predicate getPredicateIgnoreSense(Predicate predIn, Collection<Predicate> col)
    {
        for(Predicate pred : col)
        {
            if(pred.getLemma().equals(predIn.getLemma()))
            {
                return pred;
            }
        }
        return null;
    }
    
    @Override
    public String toString()
    {
        boolean compactRepresentation = featureIndexers != null;
        return compactRepresentation ? featureIndexers.getWord(lemmaInt) : lemma + "." + sense;
    }

    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof Predicate;
        Predicate p = (Predicate)obj;
        boolean compactRepresentation = featureIndexers != null;
        return timestamp == p.timestamp && 
                (compactRepresentation ? lemmaInt == p.lemmaInt && posTagInt == p.posTagInt : 
                lemma.equals(p.lemma) && sense.equals(p.sense) && posTag.equals(p.posTag));
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 67 * hash + this.timestamp;
        hash = 67 * hash + (this.sense != null ? this.sense.hashCode() : 0);
        hash = 67 * hash + (this.lemma != null ? this.lemma.hashCode() : 0);
        hash = 67 * hash + (this.posTag != null ? this.posTag.hashCode() : 0);
        hash = 67 * hash + this.lemmaInt;
        hash = 67 * hash + this.posTagInt;
        return hash;
    }

}