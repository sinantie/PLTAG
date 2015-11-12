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

import edu.stanford.nlp.international.Languages;
import edu.stanford.nlp.parser.lexparser.TreebankLangParserParams;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeTransformer;
import fig.basic.EvalResult;
import fig.basic.Fmt;
import fig.basic.Indexer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import pltag.parser.performance.EvalbImpl;
import pltag.parser.performance.Performance;
import pltag.parser.semantics.classifier.IncrementalSemanticsWidget;

/**
 *
 * @author sinantie
 */
public class IncrementalSemanticsPerformance extends Performance<IncrementalSemanticsWidget>
{
    
    /**
     * 
     * Maps containing individual results for identified predicates, argument (just lemma), 
     * argument role (both lemma and role) and combined overall SRL score (on complete dependencies) 
     * per timestamp.
     */
    private final Map<Integer, EvalResult> predResultMap, predWithSenseResultMap, argWordResultMap, argRoleResultMap, 
            argPredWordResultMap, incompleteTripleMap, srlResultMap;
    private final EvalResult predResult, argsResult, srlResult; // SRL on full sentence    
    
    private final Map<Integer, Double> evalbF1Map, evalbNumExamplesMap;
    private final EvalbImpl fullSentEvalb, partialEvalb;
    private final TreeTransformer treeCollinizer;    
    private double totalEvalbF1; // evalbF1 on full sentence
    
    private int processedExamplesCount = 0;
    
    public IncrementalSemanticsPerformance()
    {                
        predResultMap = new TreeMap<Integer, EvalResult>();
        predWithSenseResultMap = new TreeMap<Integer, EvalResult>();
        argWordResultMap = new TreeMap<Integer, EvalResult>(); // Unlabelled Argument score (UAS)
        argRoleResultMap = new TreeMap<Integer, EvalResult>();
        argPredWordResultMap = new TreeMap<Integer, EvalResult>(); // Unlabelled Prediction score (UPS)
        incompleteTripleMap = new TreeMap<Integer, EvalResult>(); // Unlabelled Incomplete score (UIS)
        srlResultMap = new TreeMap<Integer, EvalResult>(); // Combined Incremental SRL (CIS) score
        predResult = new EvalResult();
        argsResult = new EvalResult();
        srlResult = new EvalResult();
        
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
        return srlResult.f1();
    }

    public double[] add(IncrementalSemanticsPerformance performanceIn, String predSent, String goldSent, String name)
    {
        updateMap(predResultMap, performanceIn.predResultMap);
        updateMap(predWithSenseResultMap, performanceIn.predWithSenseResultMap);
        updateMap(argWordResultMap, performanceIn.argWordResultMap);
        updateMap(argRoleResultMap, performanceIn.argRoleResultMap);
        updateMap(argPredWordResultMap, performanceIn.argPredWordResultMap);
        updateMap(incompleteTripleMap, performanceIn.incompleteTripleMap);
        updateMap(srlResultMap, performanceIn.srlResultMap);
        predResult.add(performanceIn.predResult);
        argsResult.add(performanceIn.argsResult);
        srlResult.add(performanceIn.srlResult);
        processedExamplesCount += performanceIn.processedExamplesCount;
        
        updateMapDouble(evalbF1Map, performanceIn.evalbF1Map);        
        updateMapDouble(evalbNumExamplesMap, performanceIn.evalbNumExamplesMap);
        Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldSent));
        Tree evalGuess = treeCollinizer.transformTree(Tree.valueOf(predSent));
        fullSentEvalb.evaluate(evalGuess, evalGold, name);
        totalEvalbF1 = fullSentEvalb.getEvalbF1();
        
        return new double[] {fullSentEvalb.getLastF1(), performanceIn.srlResult.f1()};
    }
    
    @Override
    public double[] add(IncrementalSemanticsWidget predWidget, IncrementalSemanticsWidget goldWidget, String name)
    {
        int timestamp = predWidget.getTimestamp();
        EvalResult predicateResult = new EvalResult(), 
                   predWithSenseResult = new EvalResult(), 
                   argWordResult = new EvalResult(), 
                   argRoleResult = new EvalResult(), 
                   incompleteArcResult = new EvalResult(), 
                   srlResultLocal = new EvalResult();
        add(getEvalResultAt(timestamp, predResultMap), predicateResult, predWidget.getPredicates(), goldWidget.getPredicates());
        add(getEvalResultAt(timestamp, predWithSenseResultMap), predWithSenseResult, predWidget.getPredicatesWithSense(), goldWidget.getPredicatesWithSense());
        add(getEvalResultAt(timestamp, argWordResultMap), argWordResult, predWidget.getArgWords(), goldWidget.getArgWords());        
        add(getEvalResultAt(timestamp, argRoleResultMap), argRoleResult, predWidget.getArgRoles(), goldWidget.getArgRoles());
        // add incomplete arc score (predicate-incomplete, and argument-incomplete only
        add(getEvalResultAt(timestamp, incompleteTripleMap), incompleteArcResult, predWidget.getIncompleteArcs(), goldWidget.getIncompleteArcs());
        // add UPS (argument-incomplete, predicate-incomplete and complete triples, without role disambiguation)

        // add UAS + predicate identification score
        getEvalResultAt(timestamp, argPredWordResultMap).add(argWordResult);
        getEvalResultAt(timestamp, argPredWordResultMap).add(predicateResult);
        // add full SRL score (complete dependencies)
        srlResultLocal.add(argRoleResult);
//        srlResultLocal.add(predWithSenseResult);
        srlResultLocal.add(predicateResult);
        getEvalResultAt(timestamp, srlResultMap).add(srlResultLocal);
        if(predWidget.isFullSentence())
        {
//            predResult.add(predWithSenseResult);            
            predResult.add(predicateResult);            
            argsResult.add(argRoleResult);
            srlResult.add(srlResultLocal);
            processedExamplesCount++;
        }
        
        if(timestamp > 0)
        {
            String goldPartialTree = goldWidget.getTreeAt(timestamp);
            String predPartialTree = predWidget.getTreeAt(timestamp);
            Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldPartialTree));
            Tree evalGuess = treeCollinizer.transformTree(Tree.valueOf(predPartialTree));
            partialEvalb.evaluate(evalGuess, evalGold, name);
            add(evalbF1Map, timestamp, partialEvalb.getLastF1());
            add(evalbNumExamplesMap, timestamp, 1.0d);
        }        
        
        return new double[] {partialEvalb.getLastF1(), srlResultLocal.f1()};
    }

    private void add(EvalResult totalResult, EvalResult subResult, Collection<?> pred, Collection<?> gold)
    {
        // Get the things in common
        Iterator<?> it = gold.iterator();
        while(it.hasNext())
        {
            Object element = it.next();
            if(pred.contains(element))
            {
                it.remove();
                pred.remove(element);
                addResult(totalResult, subResult, true, true);
            }
        }
        // Record differences between two sets
        for(int i = 0; i < gold.size(); i++)
        {
            addResult(totalResult, subResult, true, false);
        }
        for(int i = 0; i < pred.size(); i++)
        {
            addResult(totalResult, subResult, false, true);
        }
    }
    
    protected void addResult(EvalResult totalResult, EvalResult subResult, boolean trueProbability,
                           boolean predictedProbability)
    {
        subResult.add(trueProbability, predictedProbability);
        totalResult.add(trueProbability, predictedProbability);
    }
    
    private EvalResult getEvalResultAt(int timestamp, Map<Integer, EvalResult> map)
    {
        EvalResult evalResult = map.get(timestamp);
        if(evalResult == null)
        {
            evalResult = new EvalResult();
            map.put(timestamp, evalResult);            
        }
        return evalResult;
    }
    
    private void updateMap(Map<Integer, EvalResult> mapToChange, Map<Integer, EvalResult> mapIn)
    {
        for(Entry<Integer, EvalResult> entry : mapIn.entrySet())
        {
            EvalResult evalResult = getEvalResultAt(entry.getKey(), mapToChange);
            evalResult.add(entry.getValue());
        }
    }
    
    private void updateMapDouble(Map<Integer, Double> mapToChange, Map<Integer, Double> mapIn)
    {
        for(Map.Entry<Integer, Double> entry : mapIn.entrySet())
        {
            add(mapToChange, entry.getKey(), entry.getValue());
        }
    }
    
    private void add(Map<Integer, Double> map, int timestamp, double f1)
    {
        Double oldF1 = map.get(timestamp);
        map.put(timestamp, oldF1 == null ? f1 : oldF1 + f1);
    }
    
    @Override
    public String output()
    {
        return String.format("%s\n\n%s\n\n%s\n\n%s", evalbSummary(), incrEvalbSummary(), srlSummary(), incrSrlSummary());
    }

    public String incrSrlSummary(Map<Integer, EvalResult> map)
    {
        StringBuilder out = new StringBuilder("\nt\tPrec\tRec\tF1");
        for(Entry<Integer, EvalResult> entry : map.entrySet())
        {
            EvalResult evalResult = entry.getValue();
            out.append(String.format("\n%s\t%s\t%s\t%s", entry.getKey(), 
                    Fmt.D(evalResult.precision()), Fmt.D(evalResult.recall()), Fmt.D(evalResult.f1())));
        }
        
        return out.toString();
    }
    
    public String incrSrlSummary()
    {
        StringBuilder out = new StringBuilder("\n\nIncremental SRL accuracy scores");
        out.append("\n----Predicate Scores----");
        out.append(incrSrlSummary(predResultMap));
        out.append("\n\n----Predicate with sense Scores----");
        out.append(incrSrlSummary(predWithSenseResultMap));
        out.append("\n\n----Argument words (no roles) (UAS) Scores----");
        out.append(incrSrlSummary(argWordResultMap));
        out.append("\n\n----Argument with roles Scores (CISS)----");
        out.append(incrSrlSummary(argRoleResultMap));
        out.append("\n\n----UPS Argument/Predicate (no roles) Scores----");
        out.append(incrSrlSummary(argPredWordResultMap));
        out.append("\n\n----Argument/Predicate-incomplete Arcs Scores----");
        out.append(incrSrlSummary(incompleteTripleMap));
        out.append("\n\n----Combined Incremental SRL Score----");
        out.append(incrSrlSummary(srlResultMap));
        return out.toString();
    }
    
    public String incrUpsScore()
    {
        return incrSrlSummary(argPredWordResultMap);
    }
    
    public String incrCissScore()
    {
        return incrSrlSummary(argRoleResultMap);
    }
    
    
    public String srlSummary()
    {
        StringBuilder out = new StringBuilder("\n\nSRL accuracy scores");
        out.append("\n----Predicate Scores----");
        out.append("\nPrecision ").append(Fmt.D(predResult.precision()));
        out.append("\nRecall ").append(Fmt.D(predResult.recall()));        
        out.append("\nF1 ").append(Fmt.D(predResult.f1()));
        out.append("\n----Argument Scores----");
        out.append("\nPrecision ").append(Fmt.D(argsResult.precision()));
        out.append("\nRecall ").append(Fmt.D(argsResult.recall()));        
        out.append("\nF1 ").append(Fmt.D(argsResult.f1()));
        out.append("\n\n----Combined Scores----");
        out.append("\nPrecision ").append(Fmt.D(srlResult.precision()));
        out.append("\nRecall ").append(Fmt.D(srlResult.recall()));        
        out.append("\nF1 ").append(Fmt.D(srlResult.f1()));
        out.append("\nTotal number of predicates: ").append(predResult.count());
        out.append("\nTotal number of arguments: ").append(argsResult.count());
        out.append("\nTotal number of predicates + arguments: ").append(srlResult.count());        
        out.append("\nTotal number of examples processed successfully: ").append(processedExamplesCount);        
        return out.toString();
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
