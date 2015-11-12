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
package pltag.parser.performance;

import edu.stanford.nlp.international.Languages;
import edu.stanford.nlp.parser.lexparser.TreebankLangParserParams;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeTransformer;
import fig.basic.Fmt;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author sinantie
 */
public class IncrementalBracketPerformance extends Performance<IncrementalBracketWidget>
{
    
    private final Map<Integer, Double> evalbF1Map, evalbNumExamplesMap;
    private final EvalbImpl fullSentEvalb, partialEvalb;
    private final TreeTransformer treeCollinizer;    
    private double totalEvalbF1; // evalbF1 on full sentence
    private int processedExamplesCount = 0;
    
    public IncrementalBracketPerformance()
    {
        evalbF1Map = new TreeMap<Integer, Double>();
        evalbNumExamplesMap = new HashMap<Integer, Double>();
        fullSentEvalb = new EvalbImpl("Evalb LP/LR", true);
        partialEvalb = new EvalbImpl("Evalb LP/LR", true);
        TreebankLangParserParams tlpp = Languages.getLanguageParams(Languages.Language.English);
        tlpp.setInputEncoding("UTF-8");
        treeCollinizer = tlpp.collinizer();
    }

    @Override
    public double getAccuracy()
    {
        return totalEvalbF1;
    }

    public double[] add(IncrementalBracketPerformance performanceIn, String predSent, String goldSent, String name)
    {        
        updateMap(evalbF1Map, performanceIn.evalbF1Map);        
        updateMap(evalbNumExamplesMap, performanceIn.evalbNumExamplesMap);        
        Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldSent));
        Tree evalGuess = treeCollinizer.transformTree(Tree.valueOf(predSent));
        fullSentEvalb.evaluate(evalGuess, evalGold, name);
        processedExamplesCount++;
        totalEvalbF1 = fullSentEvalb.getEvalbF1();
        return new double[] {fullSentEvalb.getLastF1()};
    }
    
    @Override
    public double[] add(IncrementalBracketWidget predWidget, IncrementalBracketWidget goldWidget, String name)
    {
        int timestamp = predWidget.getTimestamp();
        String goldPartialTree = goldWidget.getTreeAt(timestamp);
        String predPartialTree = predWidget.getTreeAt(timestamp);
        Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldPartialTree));
        Tree evalGuess = treeCollinizer.transformTree(Tree.valueOf(predPartialTree));
        partialEvalb.evaluate(evalGuess, evalGold, name);
        add(evalbF1Map, timestamp, partialEvalb.getLastF1());
        add(evalbNumExamplesMap, timestamp, 1.0d);
//        if(predWidget.isFullSentence())
//        {
//            fullSentEvalb.evaluate(evalGuess, evalGold, name);
//            totalEvalbF1 = fullSentEvalb.getEvalbF1();
//            processedExamplesCount++;
//        }
        return new double[] {partialEvalb.getLastF1()};
    }

    private void add(Map<Integer, Double> map, int timestamp, double f1)
    {
        Double oldF1 = map.get(timestamp);
        map.put(timestamp, oldF1 == null ? f1 : oldF1 + f1);
    }
    
    private void updateMap(Map<Integer, Double> mapToChange, Map<Integer, Double> mapIn)
    {
        for(Map.Entry<Integer, Double> entry : mapIn.entrySet())
        {
            add(mapToChange, entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public String output()
    {
        return String.format("%s\n\n%s", evalbSummary(), incrEvalbSummary());
    }
    
    public String evalbSummary()
    {
        return fullSentEvalb.summary();
    }
    
    public String incrEvalbSummary()
    {
        StringBuilder out = new StringBuilder("\n\nIncremental evalb F1 scores");
        out.append(incrSummary(evalbF1Map));
        return out.toString();
    }
    
    public String incrSummary(Map<Integer, Double> map)
    {
//        StringBuilder out = new StringBuilder("\nt\tPrec\tRec\tF1");
        StringBuilder out = new StringBuilder("\nt\tF1");
        for(Map.Entry<Integer, Double> entry : map.entrySet())
        {
            Double totalIncrF1 = entry.getValue();
            out.append(String.format("\n%s\t%s", entry.getKey(), 
                    Fmt.D(totalIncrF1 / evalbNumExamplesMap.get(entry.getKey()))));
        }        
        return out.toString();
    }
    
    public String incrEvalbScore()
    {
        return incrSummary(evalbF1Map);
    }
}
