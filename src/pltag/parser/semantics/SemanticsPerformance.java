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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.parser.performance.EvalbImpl;
import pltag.parser.performance.Performance;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.Predicate;
import pltag.parser.semantics.conll.Proposition;
import pltag.util.MyList;
import pltag.util.Pair;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public class SemanticsPerformance<Widget> extends Performance<SemanticsWidget>
{

    protected final EvalbImpl evalb;
    private final TreeTransformer treeCollinizer;
//    private double totalEvalbF1, totalConllPrecision, totalConllRecall, totalConllF1;
    protected int processedExamplesCount, goldPropsCount, predPropsCount;
    protected EvalResult srlResult; // Precision/recall on semantic role labels
    protected final EvalResult predResult;
    protected final EvalResult argsResult;
    protected int[][] counts;    // Confusion matrix on role labels
    protected int[] correctCounts, goldCounts;
    protected final Indexer<String> conllRoleIndexer;
    protected final Map<Integer, Integer> roleFreqs;
    protected final int numOfRoles;
    private MultiValueMap<String, String> errorsMap;
    
    public SemanticsPerformance(Indexer<String> roleIndexer, Map<Integer, Integer> roleFreqs)
    {
        evalb = new EvalbImpl("Evalb LP/LR", true);
        TreebankLangParserParams tlpp = Languages.getLanguageParams(Languages.Language.English);
        tlpp.setInputEncoding("UTF-8");
        treeCollinizer = tlpp.collinizer(); 
        
        conllRoleIndexer = roleIndexer;
        numOfRoles = roleIndexer.size();
        this.roleFreqs = roleFreqs;
        counts = new int[numOfRoles + 1][numOfRoles + 1];
        correctCounts = new int[numOfRoles];
        goldCounts = new int[numOfRoles];
        srlResult = new EvalResult();
        predResult = new EvalResult();
        argsResult = new EvalResult();
        errorsMap = new MultiValueMap<String, String>();
    }

    
    @Override
    public double getAccuracy()
    {
        return srlResult.f1();
    }

    @Override
    public double[] add(SemanticsWidget predAnalysis, SemanticsWidget goldStandard, String name)
    {
        // compute evalb F1
        String predTree = predAnalysis.getNBestTrees()[0];
        String goldStandardTree = goldStandard.getNBestTrees()[0];
        Tree evalGuess = treeCollinizer.transformTree(Tree.valueOf(predTree));
        Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldStandardTree));
        // in case we couldn't parse the tree at all, then abort, otherwise it is going to be an unfair comparison for SRL
        if(!(evalb.evaluate(evalGuess, evalGold, name) || predAnalysis.isBaseline()))
            return new double[] {Double.NaN, Double.NaN};
//        totalEvalbF1 = evalb.getEvalbF1();
        
        // compute SRL F1
        if(goldStandard.getPropositions() == null)
            return new double[] {Double.NaN, Double.NaN};
        Map<Predicate, Proposition> goldProps = goldStandard.getPropositions();
        goldPropsCount += goldProps.size();
        Map<Predicate, Proposition> predProps = predAnalysis.getPropositions();
        predPropsCount += predProps.size();        
//        if(goldProps.isEmpty() && predProps.isEmpty()) // no SRLs in the sentence. Return 0, but don't count 
//            return 0.0;
        EvalResult subResult = new EvalResult();
        // Get the things in common (Predicates)
        List<Predicate> goldPreds = new ArrayList();
        goldPreds.addAll(goldProps.keySet());
        Set<Predicate> predPreds = new HashSet();
        predPreds.addAll(predProps.keySet());
        Iterator<Predicate> iterPreds = goldPreds.iterator();
        while(iterPreds.hasNext())
        {
            Predicate goldPred = iterPreds.next();
            if(predPreds.contains(goldPred))
            {
                addResult(predResult, subResult, true, true); // score +1 for predicate
                predPreds.remove(goldPred);
                iterPreds.remove();
            }
            else
            {
                Predicate predPredIgnoreCase = Predicate.getPredicateIgnoreSense(goldPred, predPreds);
                if(predPredIgnoreCase != null) // found correct predicate with wrong sense
                {
                    addResult(predResult, subResult, true, false); // add +1 false negative for missed sense
                    predPreds.remove(goldPred);
                    iterPreds.remove();
                }
            }
        }
        // Mark the differences between the two maps (entries left)
        for(Predicate goldPred : goldPreds)
        {
            if(Predicate.getPredicateIgnoreSense(goldPred, predPreds) == null) // in case we miss a predicate entirely (not just its sense)
                addResult(predResult, subResult, true, false); // add +1 false negative for missed predicate                      
        }
        for(Predicate predPred : predPreds)
        {
            if(Predicate.getPredicateIgnoreSense(predPred, goldPreds) == null)
                addResult(predResult, subResult, false, true); // add +1 false positive for missed predicate (hopefully rare)                    
        }
        // Get things in common (Arguments)
        Iterator<Entry<Predicate, Proposition>> iterProps = goldProps.entrySet().iterator();
        while(iterProps.hasNext())
        {
            Entry<Predicate, Proposition> goldEntry = iterProps.next();
            Proposition predProp = predProps.get(goldEntry.getKey());
            if(predProp == null) // if we didn't find the correct predicate
            {                                
                Predicate predPredIgnoreCase = Predicate.getPredicateIgnoreSense(goldEntry.getKey(), predProps.keySet());
                if(predPredIgnoreCase != null) // found correct predicate with wrong sense
                {                    
                    predProp = predProps.get(predPredIgnoreCase);
                }
            }
            if(predProp != null) // continue with identifying correct arguments
            {
                List<Argument> goldArgs = goldEntry.getValue().getArguments();
                List<Argument> predArgs = predProp.getArguments();
                Iterator<Argument> iterArgs = goldArgs.iterator();
                while(iterArgs.hasNext())
                {
                    Argument goldArg = iterArgs.next();
                    if(predArgs.contains(goldArg))
                    {
                        addResult(argsResult, subResult, true, true); // score +1 for each correct argument
                        int roleIndex = conllRoleIndexer.getIndex(goldArg.getRole());
                        counts[roleIndex][roleIndex]++;
                        correctCounts[roleIndex]++;
                        goldCounts[roleIndex]++;
                        iterArgs.remove();
                        predArgs.remove(goldArg);
                    }
                } //  while
                if(predArgs.isEmpty())
                    predProps.remove(goldEntry.getKey());
                if(goldArgs.isEmpty())
                    iterProps.remove();
            } // if
        } // while 
        // Mark the differences between the two maps (entries left)
        for(Proposition goldProp : goldProps.values())
        {            
            for(Argument goldArg : goldProp.getArguments())
            {
                addResult(argsResult, subResult, true, false); // add +1 false negative for all missed arguments         
                int roleIndex = conllRoleIndexer.getIndex(goldArg.getRole());
                goldCounts[roleIndex]++;
            }  
        }
        for(Proposition predProp : predProps.values())
        {
            for(int i = 0; i < predProp.getArguments().size(); i++)                            
                addResult(argsResult, subResult, false, true);  // add +1 false positive for all missed arguments       
        }
        
        // fill in confusion matrix        
        if(propositionsIsEmpty(goldProps.values()))
        {
            for(Proposition predProp : predProps.values())
            {
                for(Argument predArg : predProp.getArguments())
                {
                    int predRoleIndex = conllRoleIndexer.getIndex(predArg.getRole());
                    counts[numOfRoles][predRoleIndex]++;
                }
            }
        }
//        else if(propositionsIsEmpty(predProps.values()))
//        {
//            for(Proposition goldProp : goldProps.values())
//            {
//                for(Argument goldArg : goldProp.getArguments())
//                {
//                    int goldRoleIndex = conllRoleIndexer.getIndex(goldArg.getRole());
//                    counts[goldRoleIndex][numOfRoles]++;
//                }
//            }
//        }
        else
        {
            for(Proposition goldProp : goldProps.values()) // Heuristic: mark an error on all pairs
            {
                Proposition predProp = predProps.get(goldProp.getPredicate());
                if(predProp != null && !predProp.getArguments().isEmpty())
                {
                    for(Argument goldArg : goldProp.getArguments())
                    {
                        int goldRoleIndex = conllRoleIndexer.getIndex(goldArg.getRole());
                        for(Argument predArg : predProp.getArguments())
                        {
                            int predRoleIndex = conllRoleIndexer.getIndex(predArg.getRole());
                            counts[goldRoleIndex][predRoleIndex]++;
                            if(goldRoleIndex == predRoleIndex)
                            {
                                errorsMap.put(name, goldProp.getPredicate() + ": " + goldArg + " - " + predArg);
                            }
                        }
                    } // for
                } // if
                else
                {
                    for(Argument goldArg : goldProp.getArguments())
                    {
                        int goldRoleIndex = conllRoleIndexer.getIndex(goldArg.getRole());
                        counts[goldRoleIndex][numOfRoles]++;
                    }
                }
            } // for
        }        
        processedExamplesCount++;
        //confMatrix();        
        return new double[] {evalb.getLastF1(), subResult.f1()};
    }
    
    private boolean propositionsIsEmpty(Collection<Proposition> props)
    {        
        if(props.isEmpty())
            return true;
        for(Proposition prop : props)
        {
            if(!prop.getArguments().isEmpty())
                return false;
        }
        return true;
    }
    
    protected void addResult(EvalResult subResult, EvalResult localResult, boolean trueProbability,
                           boolean predictedProbability)
    {
        addResult(srlResult, subResult, localResult, trueProbability, predictedProbability);
    }

    protected void addResult(EvalResult totalResult, EvalResult subResult, EvalResult localResult, 
                           boolean trueProbability, boolean predictedProbability)
    {
        localResult.add(trueProbability, predictedProbability);
        subResult.add(trueProbability, predictedProbability);
        totalResult.add(trueProbability, predictedProbability);
    }
        
//    public String callSrlScript(String gold, String system)
//    {
//        String cmd = String.format("resources/extractLexicon.sh -g %s -s %s", gold, system);
//        String cmdArray[] = {"/bin/sh", "-c", cmd};
//        Utils.executeCmdToArray(cmdArray);
//    }
    
    private String roleToString(int roleIndex)
    {
        if(roleIndex == numOfRoles)
            return "(none)";
        return conllRoleIndexer.getObject(roleIndex);
    }
    
    private String confMatrix()
    {
        TreeSet<Pair<String>> ts = new TreeSet<Pair<String>>();
        for(int r = 0; r < numOfRoles; r++)
        {
            ts.add(new Pair(r, roleToString(r), Utils.Compare.LABEL));
        }
        String[][] table = new String[numOfRoles + 3][numOfRoles + 2];
        // write the headers
        table[0][0] = "(gold freqs)";
        table[1][0] = "(cor/total)";
        for(Pair<String> pair : ts)
        {
            table[0][(int)pair.value + 1] = Utils.fmt(goldCounts[(int)pair.value]);
            table[1][(int)pair.value + 1] = table[(int)pair.value + 2][0] = pair.label;
        }
        table[0][numOfRoles + 1] = "";
        table[numOfRoles + 2][0] = table[1][numOfRoles + 1] = "(NONE)";
        // write the data
        int tt = 0;
        for(Pair pairRow : ts)
        {
            tt = (int) pairRow.value;
            int pt = 0;
            for(Pair pairColumn : ts)
            {
                pt = (int) pairColumn.value;
                table[tt+2][pt+1] = ((pt == tt) ? correctCounts[tt] + "/" : "") + Utils.fmt(counts[tt][pt]);
//                table[tt+1][pt+1] = Utils.fmt(counts[tt][pt]) + ((pt == tt) ? "/" + roleFreqs.get(pt) : "");
//                table[tt+1][pt+1] = Utils.fmt(counts[tt][pt]) + ((pt == tt) ? "/" + correctCounts[tt] : "");

            }
            table[tt+2][numOfRoles + 1] = Utils.fmt(counts[tt][numOfRoles]);
        }
        int pt = 0;
        for(Pair pairColumn : ts)
        {
            pt = (int) pairColumn.value;
            table[numOfRoles + 2][pt + 1] = Utils.fmt(counts[numOfRoles][pt]);
        }
        table[numOfRoles + 2][numOfRoles + 1] = "";
        return Utils.formatTable(table, Utils.Justify.RIGHT);
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
        out.append(String.format("\nTotal number of propositions: %s (out of %s gold) ", predPropsCount, goldPropsCount));
        out.append("\nTotal number of examples processed successfully: ").append(processedExamplesCount);
//        out.append(String.format("\nSuccesfully parsed %s out of %s examples", parsed, srlResult.count()));
        out.append("\n\n\n").append(confMatrix());
        out.append("\n\n\n").append(errorsMapToString());
        return out.toString();
    }
    
    private String errorsMapToString()
    {
        StringBuilder str = new StringBuilder("Example_name\npredicate: gold argument - predicted argument\n\n");
        for(String exName : errorsMap.keySet())
        {
            str.append(exName).append("\n");
            for(String error : errorsMap.getCollection(exName))
            {
                str.append(error).append(", ");
            }
            str.append("\n\n");
        }
        return str.toString();
    }
    
    @Override
    protected MyList<String> foreachStat()
    {
        MyList<String> list = new MyList();
        list.add( "logZ", Utils.fmt(stats.getAvg_logZ()) );
        list.add( "evalb F1", Utils.fmt(evalb.getEvalbF1()) );                
        list.add( "srl F1", Utils.fmt(srlResult.f1()));
        return list;
    }
    
    @Override
    public String output()
    {
        return String.format("%s\n\n%s", evalb.summary(), srlSummary());
    }    

}
