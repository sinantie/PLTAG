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
package pltag.parser;

import pltag.parser.semantics.conll.ConllExample;
import fig.basic.Fmt;
import fig.basic.FullStatFig;
import fig.basic.LogInfo;
import fig.basic.Pair;
import fig.basic.StopWatchSet;
import fig.exec.Execution;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.corpus.ElementaryStringTree;
import pltag.corpus.IdGenerator;
import pltag.corpus.StringTree;
import pltag.corpus.TagNodeType;
import static pltag.parser.ChartEntry.newNegativeInfinitiyPairArray;
import static pltag.parser.Lexicon.convertToTree;
import pltag.parser.json.JsonResult;
import pltag.parser.performance.IncrementalBracketPerformance;
import pltag.parser.performance.IncrementalBracketWidget;
import pltag.parser.performance.Performance;
import pltag.parser.semantics.DepTreeState;
import pltag.parser.semantics.Dependencies;
import pltag.parser.semantics.IncrementalSemanticsPerformance;
import pltag.parser.semantics.SemanticsWidget;
import pltag.parser.semantics.classifier.IncrementalSemanticsWidget;
import pltag.parser.semantics.discriminative.DiscriminativeParams;
import pltag.parser.semantics.discriminative.ExtractFeatures;
import pltag.parser.semantics.discriminative.OfflineReranker;
import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalysis;
import pltag.util.CallableWithLog;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public class ParsingTask extends CallableWithLog implements Comparable<ParsingTask>
{
    private final Options opts;
    private final ParserModel model;
    private final Example ex;
    String[] words;
    private final int num;
    private final FullStatFig complexity;
    private final Performance performance;
    private IncrementalSemanticsPerformance localIncrSemPerformance;
    private IncrementalBracketPerformance localIncrBracketPerformance;
    private String jsonFilename;
    
    public enum OutputType {FULL_TREE, PREFIX_TREE, ISRL, FULL_SRL, DIFFICULTY, ERROR, SUCCESS, OTHER};
//    private Chart chart;
    
    public ParsingTask(Options opts, ParserModel model, Example ex, int num, FullStatFig complexity, Performance performance, String jsonFilename)
    {
        this.opts = opts;
        this.model = model;
        this.ex = ex;
        this.num = num;
        this.complexity = complexity;
        this.performance = performance;
        this.jsonFilename = jsonFilename;
    }
    
    public ParsingTask(Options opts, ParserModel model, Example ex, int num, FullStatFig complexity, Performance performance)
    {
        this(opts, model, ex, num, complexity, performance, null);        
    }
        
        
    @Override
    public Object call() throws Exception
    {        
        boolean saveIncrAnalysesOracle = opts.saveIncrAnalysesFeatures && (opts.parserType == Options.ParserType.discriminativeOracle || opts.parserType == Options.ParserType.oracle);
        IncrementalSemanticsWidget incrSemGold = null; 
        IncrementalBracketWidget incrBracketGold = null;
        // pre-fetch gold-standard conll data in order to use either in the 
        // end of the parse (full SRL) or after every word (incremental SRL)
        if(opts.useSemantics && opts.evaluateIncrementalDependencies) 
        {
            incrSemGold = new IncrementalSemanticsWidget(model.isGoldConllPresent() ? ((ConllExample)ex).getVerbPropositionsMap() : null);
            incrSemGold.setIncrBracketWidget(new IncrementalBracketWidget(Utils.computeIncrementalTrees(Utils.normaliseTree(ex.getGoldStandardNoTraces()))));
        }
        if(opts.evaluateIncrementalEvalb)
        {
            incrBracketGold = new IncrementalBracketWidget(Utils.computeIncrementalTrees(Utils.normaliseTree(ex.getGoldStandardNoTraces())));
        }
        Pair<List<StringTreeAnalysis>, Chart> p = ex.isNotParsed() || (opts.useSemantics && opts.freqBaseline) ? new Pair(new ArrayList(), new Chart()) : parse(ex, incrSemGold, incrBracketGold);
        List<StringTreeAnalysis> analyses = p .getFirst();
        Chart chart = p.getSecond();
        // Evaluation
        if(analyses == null)
        {
            Utils.log("No parse found: " + ex.getName());
        }
        else
        {
            if(ex.isNotParsed())
            {
                if(!opts.silentMode)
                    Utils.log("No suitable lexicon extracted - no gold standard tree available for: " + ex.getName());
            }
            else if (analyses.isEmpty())
            {
                if(!opts.silentMode)
                    Utils.log("No analysis found: " + ex.getName());
            }
            else
            {
                synchronized(complexity)
                {
                    complexity.add(analyses.get(0).getComplexity());
                }
                synchronized(performance)
                {
                    performance.add(new ProbStats(ex.getNumOfWords(), analyses.get(0).getProbability(), 0));
                }
            } // else
            if(!opts.saveIncrAnalysesFeatures || saveIncrAnalysesOracle)
            {
//                synchronized(performance) // process results even if they are empty
//                {
                    if (opts.estimateProcDifficulty)
                    {
                        if(analyses.isEmpty())
                        {
                            Utils.log("No parse found: " + ex.getName());
                        }
                        else
                        {
                            StringTreeAnalysis top = analyses.get(0);
                            if (!(top == null || top.getStringTree() == null))
                //                    if (!(sta == null || sta.getStringTree() == null || sta.getStringTree().getRoot() == null))
                            {                            
                                String topToString = Utils.removeSubtreesAfterWord(Utils.normaliseTree(convertStringTree(top)), ex.getNumOfWords());
                                printOutput(model.testFullPredOut,  topToString, ex.getName(), true, OutputType.FULL_TREE);
                                if(ex.getGoldStandardNoTraces() != null && opts.outputFullPred)
                                {
                                    double modelScore = top.getStringTree().getProbability();
                                    synchronized(performance)
                                    {                                        
                                        double curF1 = performance.add(topToString, Utils.normaliseTree(ex.getGoldStandardNoTraces()), ex.getName())[0];                            
                                        String summary = String.format("%s\n%s\n[score=%s, evalb F1=%s]\n", ex.getName(), topToString, Fmt.D(modelScore), Fmt.D(curF1));
                                        printOutput(model.testFullPredOut,  summary, ex.getName(), true, OutputType.FULL_TREE);
                                    }
                                }                                                       
                            }
                        }                    
                    }
                    else
                    {                     
                        if (opts.train)
                        {
                            synchronized(performance)
                            {
                                double noOfCorrect = performance.add(convertStringTrees(analyses), Arrays.asList(new String[] {ex.getGoldStandardNoTraces()}), ex.getName())[0];
                                StringTreeAnalysis firstCorrectAnalysis = findCorrect(analyses, ex.getGoldStandardNoTraces());
                                FreqCounter freqCounterForCorrect = firstCorrectAnalysis == null ? null : firstCorrectAnalysis.getFreqCounter();
                                if(freqCounterForCorrect != null)
                                {
                                    //freqCounterForCorrect.doNoneAdjStats(sta.getStringTree().getCategories()); 
                                    //freqCounter.join(freqCounterForCorrect);
                                    model.updateParams(ex.getFreqs(), freqCounterForCorrect);
                                }
                                String summary = String.format("(%s): analyses found: %s; %s", ex.getName(), analyses.size(), 
                                    (freqCounterForCorrect != null ? (int) noOfCorrect + " correct" : "all wrong"));
                                printOutput(model.trainPredOut, summary);
                            }                            
                        }
                        else
                        {                           
                            String topToString = "";
                            double modelScore = 0.0;
                            if(!analyses.isEmpty())
                            {
                                StringTreeAnalysis top = analyses.get(0);
    //                            topToString = Utils.computeIncrementalTree(Utils.normaliseTree(convertStringTree(top)), words.length - 1);
                                topToString = Utils.removeSubtreesAfterWord(Utils.normaliseTree(convertStringTree(top)), ex.getNumOfWords());
                                modelScore = top.getStringTree().getProbability();
                            }                           
                            if(opts.useSemantics) // conll evaluation + evalb
                            {
                                ConllExample goldConll = model.isGoldConllPresent() ? (ConllExample)ex : null;
                                Dependencies predDependencies;
                                SemanticsWidget pred = null;
                                if(opts.freqBaseline && model.isGoldConllPresent())
                                {
                                    predDependencies = new Dependencies(); // dummy, just for printing out 
                                    synchronized(performance)
                                    {
                                        performance.add(new ProbStats(ex.getNumOfWords(), 0, 0)); // dummy, we don't have real statistics to print out
                                    }                                    
                                    if(opts.evaluateIncrementalDependencies)
                                    {
                                        localIncrSemPerformance = new IncrementalSemanticsPerformance();
                                        switch(opts.baselineSemanticsModel)
                                        {
                                            case goldIdentification: 
                                                goldConll.getIncrementalMajorityBaselineGoldIdentification(model.getMajorityDepArgs(), incrSemGold, ex.getNumOfWords(), localIncrSemPerformance);
                                                break;
                                            case directArcsIdentification :
                                                goldConll.getIncrementalMajorityBaselineDirectArcs(model.getMajorityDepArgs(), incrSemGold, ex.getNumOfWords(), localIncrSemPerformance);
                                                break;
                                            case heuristicsIdentification :
                                                goldConll.getIncrementalMajorityBaseline(model.getMajorityDepArgs(), incrSemGold, localIncrSemPerformance);
                                                break;
                                            case maltParser :
                                                goldConll.getIncrementalMaltParserBaseline(model.getMaltParser(), model.getMajorityDepArgs(), incrSemGold, localIncrSemPerformance);                                                
                                        }
                                    }
                                    else
                                    {                                    
    //                                    pred = new SemanticsWidget(topToString, goldConll.getMajorityBaseline(model.getMajorityDepArgs()), true);
                                        pred = new SemanticsWidget(new String[] {topToString}, goldConll.getMajorityBaselineGoldIdentification(model.getMajorityDepArgs()), true);
    //                                pred = new SemanticsWidget(topToString, goldConll.getMajorityBaselineAllA0s(), true);
                                    }                                     
                                }                    
                                else 
                                {
                                    predDependencies = ex.isNotParsed() || analyses.isEmpty() ? new Dependencies() : analyses.get(0).getDependencies();
                                    if(opts.parserType == Options.ParserType.oracle || opts.parserType == Options.ParserType.discriminativeOracle)
                                    {
                                        pred = new SemanticsWidget(convertAnalysesToTrees(analyses), model.isGoldConllPresent() ? predDependencies.toPropositions(analyses.get(0), goldConll, model) : null);
                                    }
                                    else
                                    {
                                        pred = new SemanticsWidget(new String[] {topToString}, model.isGoldConllPresent() ? predDependencies.toPropositions(analyses.get(0), goldConll, model) : null);
                                    }                                
                                }
                                String goldTree = Utils.normaliseTree(ex.getGoldStandardNoTraces());
                                SemanticsWidget gold = new SemanticsWidget(new String[] {goldTree}, model.isGoldConllPresent() ? goldConll.getVerbPropositionsMap() : null);   
                                if(opts.trainClassifiers || opts.extractSrlFeatures) // extract features and negative examples (for identifier)
                                {                                
                                    model.getArgumentClassifier().extractFeatures(pred.getPropositions(), goldConll.getVerbPropositionsMap(), ex.getLexicon(), opts.trainClassifiers);
                                }
                                else if(!opts.trainClassifiers)
                                {                            
                                    // in case of incremental evaluation copy values from a local performance object. This is to make sure
                                    // we update our global evaluation object only when we have full analyses.   
                                    double curF1[];
                                    synchronized(performance)
                                    {
                                        curF1 = opts.evaluateIncrementalDependencies ? 
                                            ((IncrementalSemanticsPerformance)performance).add(localIncrSemPerformance, topToString, goldTree, ex.getName()) : 
                                            performance.add(pred, gold, ex.getName());                                
                                    }                                    
                                    if(opts.outputFullPred && !(opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies) )
                                    {
                                        if(opts.parserType == Options.ParserType.discriminativeOracle || opts.parserType == Options.ParserType.oracle)
                                        {

                                            saveIncrAnalysesFeatures(analyses, chart, (int)curF1[2]);
                                            String summary = String.format("%s\n%s\n[score=%s, evalb F1=%s, SRL F1=%s, pos=%s]\n", ex.getName(), 
                                                    predDependencies + "\n"+pred.getNBestTrees()[0], Fmt.D(modelScore), Fmt.D(curF1[0]), Fmt.D(curF1[1]), (int)curF1[2]);
                                            printOutput(model.testFullPredOut,  summary);
                                        }
                                        else
                                        {
                                            saveIncrAnalysesFeatures(analyses, chart, -1);
                                            String summary = String.format("%s\n%s\n[score=%s, evalb F1=%s, SRL F1=%s]\n", ex.getName(), 
                                                    predDependencies + "\n"+topToString, Fmt.D(modelScore), Fmt.D(curF1[0]), Fmt.D(curF1[1]));
                                            printOutput(model.testFullPredOut,  summary);
                                        }                                    
                                        if(model.isGoldConllPresent())
                                        {                                
                                            synchronized(model.getConllPredList())
                                            {
                                                String predConll = predDependencies.toConll(analyses.get(0), goldConll, model);
                                                model.getConllPredList().put(num, predConll);
                                            }                                
                                            synchronized(model.getConllGoldList())
                                            {
                                                String goldConllVerbsOnly = goldConll.getGoldStandardVerbsOnlyConll();
                                                model.getConllGoldList().put(num, goldConllVerbsOnly);
                                            }                                
                                        }                                
                                    } // if
                                }                            
                            } // if (useSemantics)
                            else // evalb evaluation
                            {                            
                                String goldTree = Utils.normaliseTree(ex.getGoldStandardNoTraces());
                                if(opts.parserType == Options.ParserType.generative)
                                {                
                                    double curF1;
                                    synchronized(performance)
                                    {
                                        curF1 = opts.evaluateIncrementalEvalb ? 
                                            ((IncrementalBracketPerformance)performance).add(localIncrBracketPerformance, topToString, goldTree, ex.getName())[0] : 
                                            performance.add(topToString, goldTree, ex.getName())[0];
                                    }                                    
                                    if(opts.outputFullPred)
                                    {
                                        String summary = String.format("%s\n%s\n[score=%s, evalb F1=%s]\n", ex.getName(), topToString, Fmt.D(modelScore), Fmt.D(curF1));
                                        printOutput(model.testFullPredOut,  summary);
                                    }
                                }
                                else if(opts.parserType == Options.ParserType.oracle)
                                {
                                    String[] nBestTrees = convertAnalysesToTrees(analyses);   
                                    double[] curF1;
                                    synchronized(performance)
                                    {
                                        curF1 = performance.add(nBestTrees, new String[] {goldTree}, ex.getName());
                                    }                                    
                                    if(opts.outputFullPred)
                                    {
                                        String summary = String.format("%s\n%s\n[score=%s, evalb F1=%s, nBest pos=%s]\n", ex.getName(), topToString, Fmt.D(modelScore), Fmt.D(curF1[0]), (int)curF1[1]);
                                        printOutput(model.testFullPredOut,  summary);
                                    }
                                }                               
                            } // else (evalb evaluation)
                        } // else (not train)
                    } // else (not estimate processing difficulty)
//                } // synchronized   
            } // if !saveIncrAnalysesFeatures 
            else
            {
                saveIncrAnalysesFeatures(analyses, chart, -1);
            }
        }  // else analysis != null      
        if (opts.outputExampleFreq != 0 && num % opts.outputExampleFreq == 0)
        {
            Utils.logs("Example %s/%s %s", Utils.fmt(num+1), Utils.fmt(model.examples.size()), model.summary(num));            
            Execution.putOutput("currExample", num);                        
        }
        return null;
    }
   
    public Pair<List<StringTreeAnalysis>, Chart> parse(Example ex)
    {
        return parse(ex, null, null);
    }
    
    public Pair<List<StringTreeAnalysis>, Chart> parse(Example ex, IncrementalSemanticsWidget goldDeps, IncrementalBracketWidget goldBrackets)
    {              
        List<StringTreeAnalysis> analyses = null;// = new ArrayList<StringTreeAnalysis>();//parse(words);
        Chart chart = null;
        String sentence = ex.getPosTagged(); 
        String[] origPosTags = ex.getOrigPosTags();
        if (!sentence.isEmpty())
        {                        
            words = sentence.split("\t");          
//            if(words.length <= opts.maxWordLength || opts.estimateProcDifficulty)
//            {                
                StopWatchSet.begin("parse");
                //String solution = ex.getSolution();                            
                int beamProp = opts.beamProp;  
                int beamWidth = opts.beamMin;
                try
                {
                    if (opts.train)
                    {
                        beamProp = 12;
                        Pair<List<StringTreeAnalysis>, Chart> p = parse(words, origPosTags, beamWidth, opts.beamEntry, beamProp, false, ex.getSuperTagger(), goldDeps, goldBrackets);//input.getParsed().get(i));
                        analyses = p.getFirst();
                        chart = p.getSecond();
                    }
                    else if (opts.estimateProcDifficulty)
                    {
                        Pair<List<StringTreeAnalysis>, Chart> p = parse(words, origPosTags, beamWidth, opts.beamEntry, 8, false, ex.getSuperTagger(), goldDeps, goldBrackets);//input.getParsed().get(i));
                        analyses = p.getFirst();
                        chart = p.getSecond();
                        
                    }
                    else if (!opts.pruneStrategyIsProp)
                    {
                        Pair<List<StringTreeAnalysis>, Chart> p = parse(words, origPosTags, beamWidth, opts.beamEntry, 6, false, ex.getSuperTagger(), goldDeps, goldBrackets);
                        analyses = p.getFirst();
                        chart = p.getSecond();
                    }
                    else
                    {                        
                        try
                        {
                            beamProp = 8;                            
//                            Utils.log("beam = " + beamProp);
                            Pair<List<StringTreeAnalysis>, Chart> p = parse(words, origPosTags, beamWidth, opts.beamEntry, beamProp, false, ex.getSuperTagger(), goldDeps, goldBrackets);
                            analyses = p.getFirst();
                            chart = p.getSecond();
                        }
                        catch (OutOfMemoryError oom2)
                        {                            
                            LogInfo.error("out of memory: " + ex.getName());                         
                            try
                            {
                                beamProp = 12;
//                                Utils.log("beam = 12");
                                Pair<List<StringTreeAnalysis>, Chart> p = parse(words, origPosTags, beamWidth - 50, opts.beamEntry - 20, beamProp, false, ex.getSuperTagger(), goldDeps, goldBrackets);
                                analyses = p.getFirst();
                                chart = p.getSecond();
                            }
                            catch (OutOfMemoryError oom4)
                            {                                
                                LogInfo.error("out of memory: " + ex.getName());                                                 
                                try
                                {
                                    beamProp = 8;
//                                    Utils.log("beam = 8");
                                    Pair<List<StringTreeAnalysis>, Chart> p = parse(words, origPosTags, beamWidth - 100, opts.beamEntry - 20, beamProp, false, ex.getSuperTagger(), goldDeps, goldBrackets);
                                    analyses = p.getFirst();
                                    chart = p.getSecond();

                                }
                                catch (OutOfMemoryError oom5)
                                {                                    
                                    LogInfo.error("out of memory: " + ex.getName());   
                                    StopWatchSet.end();
                                    return null;
                                }
                            }                        
                        }
                    }                
                }
                catch (Exception e)
                {
                    LogInfo.error("Error during parsing: " + ex.getName());//ex.printStackTrace();
                    e.printStackTrace();
                    StopWatchSet.end();
                    return null;
                }
                catch (OutOfMemoryError oomx)
                {
                    LogInfo.error("out of memory: " + ex.getName()); 
                    StopWatchSet.end();
                    return null;
                }
                if (analyses == null)
                {
                    StopWatchSet.end();
                    return null;
                }
                if (analyses.isEmpty())
                {
//    				beamWidth += beamWidth;
                    //if (!train && pruneStrategyIsProp && propbeam ==12 && !this.estimateProcDifficulty) {
                    if (!opts.train && !opts.estimateProcDifficulty)
//                    if (!opts.train && opts.pruneStrategyIsProp && !opts.estimateProcDifficulty)
//                    if (!opts.train && opts.pruneStrategyIsProp && beamProp == 12 && !opts.estimateProcDifficulty)
                    {
                        try
                        {
//                            Utils.logs("Retrying example %s with beamWidth = %s, beamEntry = %s and beamProp = %s", ex.getName(), (beamWidth+100), opts.beamEntry, 20);
                            // NOTE: Changed false to true (repeat parse)
                            Pair<List<StringTreeAnalysis>, Chart> p = parse(words, origPosTags, beamWidth + 100, opts.beamEntry, 20, true, ex.getSuperTagger(), goldDeps, goldBrackets);//input.getParsed().get(i));
                            analyses = p.getFirst();
                            chart = p.getSecond();
                            if (analyses.isEmpty())
                            {
//                                Utils.logs("Retrying example %s with beamWidth = %s, beamEntry = %s and beamProp = %s", ex.getName(), (beamWidth+300), opts.beamEntry, 30);
                                // NOTE: Changed false to true (repeat parse)
                                p = parse(words, origPosTags, beamWidth + 300, opts.beamEntry, 30, true, ex.getSuperTagger(), goldDeps, goldBrackets);//input.getParsed().get(i));
                                analyses = p.getFirst();
                                chart = p.getSecond();
                            }
                            if (analyses.isEmpty())
                            {
//                                Utils.logs("Retrying example %s with beamWidth = %s, beamEntry = %s and beamProp = %s", ex.getName(), (beamWidth+500), opts.beamEntry, 40);
                                p = parse(words, origPosTags, beamWidth + 500, opts.beamEntry, 40, true, ex.getSuperTagger(), goldDeps, goldBrackets);//input.getParsed().get(i));
                                analyses = p.getFirst();
                                chart = p.getSecond();
//                                analyses = parse(words, beamWidth + 300, opts.beamEntry, 40, true, 0, model.getSuperTagger(), solution);//input.getParsed().get(i));
                            }
                            if (analyses.isEmpty())
                            {
//                                Utils.logs("Retrying example %s with beamWidth = %s, beamEntry = %s and beamProp = %s", ex.getName(), (beamWidth+1000), (opts.beamEntry+1000), 10000);
                                // NOTE: Changed false to true (repeat parse)
                                p = parse(words, origPosTags, beamWidth + 1000, opts.beamEntry + 1000, 10000, true, ex.getSuperTagger(), goldDeps, goldBrackets);//input.getParsed().get(i));
                                analyses = p.getFirst();
                                chart = p.getSecond();
                            }
                        }
                        catch (OutOfMemoryError outOfMem3)
                        {
                            LogInfo.error("out of memory: " + ex.getName());   
                            StopWatchSet.end();
                            return null;
                        }
                        catch (Exception e)
                        {                        
                            LogInfo.error("Error during parsing: " + ex.getName());
                            e.printStackTrace();
                            StopWatchSet.end();
                            return null;
                        }
                    }
                }                
                StopWatchSet.end();
//            } // if maxWordLength
        } // if 
        else
        {
            LogInfo.error("Can't read sentence: " + ex.getName());
        }
        return new Pair(analyses, chart);
    }

    private Pair<List<StringTreeAnalysis>, Chart> parse(String[] words, String[] origPosTags, int beamWidth, int entrybeam, int propbeam,
                                                boolean repeatParse, SuperTagger superTagger, IncrementalSemanticsWidget goldSemantics, 
                                                IncrementalBracketWidget goldBrackets)
    {
        String bestPartialAnalysisTree = null;
        boolean discriminative = opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle;
        localIncrSemPerformance = opts.evaluateIncrementalDependencies ? new IncrementalSemanticsPerformance() : null;
        localIncrBracketPerformance = opts.evaluateIncrementalEvalb ? new IncrementalBracketPerformance() : null;
        
        if(!opts.interactiveMode && !opts.serverMode && (opts.estimateProcDifficulty || opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies))
        {
            printOutput(model.testFullPredOut, "\n" + ex.getName(), ex.getName(), false, OutputType.OTHER);
            if(opts.outputCompletedIncrementalDependencies)
                printOutput(model.testIncrDependenciesOut, "\n" + ex.getName(), ex.getName(), false, OutputType.ISRL);
        }
        Chart chart = new Chart(opts, words);
        LinkingTheory linker = new LinkingTheory(opts, beamWidth, entrybeam, propbeam);
        linker.newSentence();
        //System.out.println("beam="+beamWidth);
        long startTime = System.currentTimeMillis();
        //try {
        //double cc = 0;
        for (short wno = 0; wno < words.length; wno++)
        {
            String[] wordPos = Utils.getWordPos(words[wno], opts.posOnly, opts.goldPosTags);
            String word = wordPos[0];
            String pos = wordPos[1];
            if (opts.verbose)
            {
                Utils.log("word " + wno + ": " + word + "\t");
            }
            Collection<ElementaryStringTree> fulltrees = getTrees(word, pos, repeatParse, wno);
            if (fulltrees == null || fulltrees.isEmpty())
            {             
                if(opts.estimateProcDifficulty)
                {
                    if(opts.timeProfile)
                    {
                        StopWatchSet.begin("estimateDifficulty");
                    }
                    String error = "S:\tNA\tIC: NA\tD: NA; " + word + " No lexicon entry found.";
                    LogInfo.error(error + " " + ex.getName());      
                    printOutput(model.testFullPredOut, error, ex.getName(), true, OutputType.ERROR);
                    if(opts.timeProfile)
                    {
                        StopWatchSet.end();
                    }
                }
                else
                {
                    if(!opts.silentMode)
                        LogInfo.error("no trees found for word " + word + " " + ex.getName());
                }
                
                continue;
            }
            
            double start = System.currentTimeMillis();
            if(opts.timeProfile)
            {
                StopWatchSet.begin("combineTrees");
            }
            ArrayList<ChartEntry> newEntries = combineTrees(fulltrees, chart, wno, origPosTags, start, "", superTagger, beamWidth, wno == words.length - 1, wno + 1);
            if (opts.train && ParserModel.useTimeOut && System.currentTimeMillis() - startTime > opts.timeOutStage1)
            {
                if(!opts.silentMode)
                    LogInfo.logs("Time out at stage 1 - search ( " + (((double)opts.timeOutStage1 / 1000.0)/60.0)+ " min). " + ex.getName());                
                return new Pair(new ArrayList<StringTreeAnalysis>(), chart);
            }            
            if (opts.timeProfile)
            {
                StopWatchSet.end();
                start = System.currentTimeMillis();
            }            
            if (newEntries.isEmpty() && opts.estimateProcDifficulty)
            {
                if (opts.estimateProcDifficulty)
                {
                    if(opts.timeProfile)
                    {
                        StopWatchSet.begin("estimateDifficulty");
                    }
                    String error = opts.inputType == Options.InputType.dundee ? String.format("%s |\t%s\t%s\tNA\tNA\tNA - Could not combine trees", ex.getSentenceRc(), wordPos[0], ex.getWordIds()[wno]) : "S: NA\tIC: NA\tD: NA; " + words[wno];
                    LogInfo.error(error);
                    if(!opts.serverMode)
                        printOutput(model.testFullPredOut, error);
                    for (short wno2 = (short) ((short) wno + 1); wno2 < words.length; wno2++)
                    {
                        error = opts.inputType == Options.InputType.dundee ? String.format("%s |\t%s\t%s\tNA\tNA\tNA - Could not combine trees", ex.getSentenceRc(), words[wno2], ex.getWordIds()[wno2]) : "S: NA\tIC: NA\tD: NA; " + words[wno2];
                        LogInfo.error(error);
                        printOutput(model.testFullPredOut, error, ex.getName(), true, OutputType.ERROR);
                    }
                    if(opts.timeProfile)
                    {
                        StopWatchSet.end();
                    }
                }
                else
                {
                    Utils.log("could not combine trees: " + ex.getName());
                }
                return new Pair(new ArrayList<StringTreeAnalysis>(), chart);
            }
            if (!opts.train && opts.estimateProcDifficulty)
            {
                if(opts.timeProfile)
                {
                    StopWatchSet.begin("estimateDifficulty");
                }
                if(discriminative)
                {
                    updateChartEntriesDiscriminative(chart, newEntries, wno, wno == words.length - 1, wno + 1);
                }
                double baselineWeight = discriminative ? model.getDiscriminativeParams().getBaselineWeight() : 0;
                if(opts.inputType == Options.InputType.dundee)
                    printOutput(model.testFullPredOut, linker.calculateDifficulty(newEntries, wno, word, pos, ex.getSentenceRc(), ex.getWordIds()[wno], discriminative, baselineWeight), ex.getName(), true, OutputType.DIFFICULTY);
                else
                    printOutput(model.testFullPredOut, wno + 1, word, pos, linker.calculateDifficulty(newEntries, wno, word, pos, null, null, discriminative, baselineWeight), ex.getName(), true);
                if(opts.timeProfile)
                {
                    StopWatchSet.end();
                }
            }
            if (opts.useProbabilityModel)
            {
                if(opts.timeProfile)
                {
                    StopWatchSet.begin("prune");
                }
                linker.prune(newEntries, false, 1, opts.pruneStrategyIsProp, discriminative, opts.pruneUsingScores, (wno+1));
                if(!opts.estimateProcDifficulty && (opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle))
                {
                    updateChartEntriesDiscriminative(chart, newEntries, wno, wno == words.length - 1, wno + 1);
                }
                if (opts.timeProfile)
                {                
                    StopWatchSet.end();
                    start = System.currentTimeMillis();
                }                
            }            
            if(opts.timeProfile)
            {
                StopWatchSet.begin("chart.putAll");
            }
            chart.putAll(newEntries, startTime, discriminative, opts.pruneUsingScores);
            if (opts.train && ParserModel.useTimeOut && System.currentTimeMillis() - startTime > opts.timeOutStage1)
            {
                if(!opts.silentMode)
                    LogInfo.logs("Time out at stage 1 - search ( " + (((double)opts.timeOutStage1 / 1000.0)/60.0)+ " min). " + ex.getName());
                return new Pair(new ArrayList<StringTreeAnalysis>(), chart);
            }
            if (opts.timeProfile)
            {
                StopWatchSet.end();
                start = System.currentTimeMillis();
            }            
            if (opts.verbose)
            {
                Utils.log("full trees finished\t");
            }
//            StringTreeAnalysis bestOld = null;
            StringTreeAnalysis bestPartialAnalysis = null;
            if (opts.printIncrementalDeriv || opts.evaluateIncrementalDependencies || opts.evaluateIncrementalEvalb || opts.outputCompletedIncrementalDependencies)
            {
                List<StringTreeAnalysis> partialAnalyses = getAnalyses(chart, wno, startTime, discriminative);
                if (partialAnalyses.isEmpty())
                {
//                    partialAnalyses = getAnalyses(chart, wno - 1, startTime, discriminative);
//                    Utils.log(partialAnalyses.get(0));
                    // Commented out return
//                    return partialAnalyses;
//                    System.out.println("No analyses found");
                }
                else
                {
                    bestPartialAnalysis = partialAnalyses.get(0);
//                    double bprob = bestPartialAnalysis.getStringTree().getProbability();
//                    System.out.println("Word: " + wno);
//                        for (StringTreeAnalysis c : partialAnalyses)
//                        {
    //                        if (c.getStringTree().getProbability() > bprob)
    //                        {
    //                            bprob = c.getStringTree().getProbability();
    //                            bestPartialAnalysis = c;
    //                        }
//                            System.out.println(Fmt.D(c.getScore()) + "\t" + Fmt.D(c.getProbability()) + "\t" + 
//    //                                Utils.removeSubtreesAfterWord(Utils.normaliseTree(convertStringTree(c)), wno + 1));
//                                    Utils.normaliseTree(convertStringTree(c)));
//                        }
                    // heuristic to add flat structure to partial parses (Commented out)
//                    StringTreeAnalysis stap = addMissingWords(bestOne, (short) (wno + 1), words);
                    
////                    Utils.log(stap);
////                    if(!bestOne.equals(bestOld))
////                    {
////                        Utils.log("ELEM: " + bestOne.getStringTree().printSimpleCat());
////                        bestOld = bestOne;
////                    }                    
////                    Utils.log(bestOne.getStringTree().printSimple(bestOne.getStringTree().getRoot(), true));
//                    Utils.log(bestOne.getStringTree().printSimple(stap.getStringTree().getRoot(), true));
//                    Utils.log(ex.getSolution());
//                    Utils.log(bestOne.getBuiltTrace());                    
                    bestPartialAnalysisTree = Utils.removeSubtreesAfterWord(Utils.normaliseTree(convertStringTree(bestPartialAnalysis)), wno + 1);
                    if(opts.printIncrementalDeriv)// && opts.outputFullPred)
                    {
//                        model.printOutput(model.testFullPredOut, bestOne.getBuiltTrace());
//                        model.printOutput(model.testFullPredOut, bestOne.getStringTree().printSimple(bestOne.getStringTree().getRoot(), true));                        
//                        System.out.println("Chart size = " + chart.printChartStats(wno));
                        printOutput(model.testFullPredOut, wno + 1, bestPartialAnalysisTree, ex.getName(), true, OutputType.PREFIX_TREE);
//                        System.out.println("Fringe: " + bestPartialAnalysis.getFringe());//.toCatString());
//                        model.printOutput(model.testFullPredOut, Utils.normaliseTree(convertStringTree(stap)));
                    }
                    //System.out.println(partialAnalyses.get(0));
                    if(opts.evaluateIncrementalEvalb && wno > 0) // compute evalb for prefix trees with more than 1 word
                    {                        
                        synchronized(localIncrBracketPerformance)
                        {
                            double curF1 = localIncrBracketPerformance.add(new IncrementalBracketWidget(wno, bestPartialAnalysisTree, wno == words.length - 1), goldBrackets, ex.getName())[0];
                            if(opts.printIncrementalDeriv)
                            {        
//                                    if(wno == words.length - 1)
//                                    {
//                                        printOutput(model.testFullPredOut, partialAnalyses.toString(), ex.getName(), true, OutputType.OTHER);
//                                    }
                                printOutput(model.testFullPredOut, String.format("[evalb F1=%s]\n", Fmt.D(curF1)), ex.getName(), true, OutputType.OTHER);                            
                            }                                                                                    
                        }                        
                    }
                }
            }
            //%%%%%%%%%%%%%%
            if (chart.get(wno).isEmpty() && (repeatParse | opts.estimateProcDifficulty))
            {
                for (short wno2 = (short) ((short) wno + 1); wno2 < words.length; wno2++)
                {
                    String word2 = words[wno2];
                    if (opts.estimateProcDifficulty || opts.verbose)
                    {
                        String error = "word " + wno2 + ": S: NA\tIC: NA\tD: NA; " + word2;
                        LogInfo.error(error);                        
                        printOutput(model.testFullPredOut, error, ex.getName(), true, OutputType.ERROR);
                    }
                }
                if(!opts.silentMode)
                    LogInfo.error("Was not able to add words, after word " + wno + " : (" + word + ") of " + words.length + ": " + ex.getName());
                return new Pair(new ArrayList<StringTreeAnalysis>(), chart);
                //break;
            }
            //FULL SEARCH THAT ALWAYS GENERATES ALL APPLICABLE PRED TREES
            if (opts.verbose)
            {
                Utils.log("\tusing shadow tree");
            }
            //Collection<ElementaryStringTree> trees = shadowTrees;//.keySet();
            Collection<ElementaryStringTree> trees = ex.getShadowTreesMap().keySet();
            if (trees != null && !trees.isEmpty() && wno + 1 != words.length)
            {
                if (opts.timeProfile)
                {
                    StopWatchSet.begin("combineTrees - shadow");
                    start = System.currentTimeMillis();
                }
                String nextcat = words.length == wno + 1 ? "EOS" : words[wno + 1].substring(0, words[wno + 1].indexOf(" "));
                ArrayList<ChartEntry> shadowce = combineTrees(trees, chart, (short) (wno + 1), null, start, nextcat, superTagger, beamWidth, wno == words.length - 1, wno + 1);
                if (opts.timeProfile)
                {
                    StopWatchSet.end();
                    start = System.currentTimeMillis();
                }
                if (!opts.train)
                {
                    if(opts.timeProfile)
                    {
                        StopWatchSet.begin("prune - shadow");
                    }
                    linker.prune(shadowce, true, 1, opts.pruneStrategyIsProp, discriminative, opts.pruneUsingScores, (wno + 1));
                    if(opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle)
                    {
                        updateChartEntriesDiscriminative(chart, shadowce, wno, wno == words.length - 1, wno + 1);
                    }
                    if (opts.timeProfile)
                    {
                        StopWatchSet.end();
                        start = System.currentTimeMillis();
                    }
                }                
                if(opts.timeProfile)
                {
                    StopWatchSet.begin("chart.putAll - shadow");
                }
                chart.putAll(shadowce, start, discriminative, opts.pruneUsingScores);
                if (opts.timeProfile)
                {
                    StopWatchSet.end();                    
                }                                
            }             
            if (opts.printChart)
            {
                chart.print(wno);
            }            
            if (chart.get(wno).isEmpty())
            {                
                if(!opts.silentMode)
                    LogInfo.error("Was not able to add words (shadow), after word " + wno + " : (" + word + ") of " + words.length + " " + ex.getName());
                return new Pair(new ArrayList<StringTreeAnalysis>(), chart);
            }
            chart.compressSlice(wno - 1);
//            if (opts.printIncrementalDeriv || opts.evaluateIncrementalDependencies || opts.evaluateIncrementalEvalb)
//            {
//                List<StringTreeAnalysis> partialAnalyses = getAnalyses(chart, wno, startTime, discriminative);
//                if (!partialAnalyses.isEmpty())
//                {
//                    System.out.println("Word (shadow): " + wno);
//                    for (StringTreeAnalysis c : partialAnalyses)
//                    {
//                        System.out.println(Fmt.D(c.getScore()) + "\t" + Fmt.D(c.getProbability()) + "\t" + 
////                                Utils.removeSubtreesAfterWord(Utils.normaliseTree(convertStringTree(c)), wno + 1));
//                                Utils.normaliseTree(convertStringTree(c)));
//                    }
//                }
//            }
            if(!repeatParse && opts.useSemantics && (opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies ||opts.evaluateIncrementalDependencies))
            {
                ChartEntry topEntry = discriminative ? linker.getTopSliceDiscriminative(chart.get(wno)) : linker.getTopSlice(chart.get(wno));
                Dependencies predDependencies = ((DepTreeState)topEntry.getTreeState()).getDependencies();                
                IncrementalSemanticsWidget pred = predDependencies.convertDependenciesAt(wno, wno == words.length - 1, bestPartialAnalysis, ex instanceof ConllExample ? ((ConllExample)ex) : null, model);
                 // compute evalb for prefix trees with more than 1 word
                if(wno > 0)
                    pred.setIncrBracketWidget(new IncrementalBracketWidget(wno, bestPartialAnalysisTree, wno == words.length - 1));
                double curF1[] = new double[] {Double.NaN, Double.NaN};
                if(localIncrSemPerformance != null)
                {
                    synchronized(localIncrSemPerformance)
                    {
                        if(goldSemantics.convertPropositionsAt(wno))
                        {
                            curF1 = localIncrSemPerformance.add(pred, goldSemantics, ex.getName());
                        }                    
                        else
                        {
                            LogInfo.error("Error while converting gold propositions in example " + ex.getName() + " at timestamp " + wno);
                        }
                    }
                }                
                if(!predDependencies.isEmpty() && (opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies))
                {
                    if(opts.outputCompletedIncrementalDependencies)
                    {
                        String output = predDependencies.toStringCompletedOnly(words[wno], wno + 1);
                        if(!output.equals(""))
                        {
                            printOutput(model.testIncrDependenciesOut, wno + 1, output, ex.getName(), true, OutputType.ISRL);
                        }                        
                    }
                    else
                        printOutput(model.testFullPredOut, wno + 1, predDependencies.toString() , ex.getName(), true, OutputType.ISRL);
                    if(opts.evaluateIncrementalDependencies)
                    {
                        printOutput(model.testFullPredOut, String.format("[evalb F1=%s, SRL F1=%s]\n", Fmt.D(curF1[0]), Fmt.D(curF1[1])), ex.getName(), true, OutputType.OTHER);
                    }
                }
            } // if
        } // for each word
//        Utils.log("search done; ");        
        //System.out.println(analyses.get(0));
        if(opts.saveIncrAnalyses && !chart.get(words.length - 1).isEmpty())
        {
            Map<String, Chart> incrAnalyses = model.getIncrAnalyses();
            incrAnalyses.put(ex.getName(), chart);
            synchronized(incrAnalyses)
            {
                boolean isOracle = opts.semanticsModel == Options.SemanticsModelType.oracle || opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;
                if(!isOracle && incrAnalyses.size() >= opts.maxNumOfSentencesIncrAnalyses)
                {
                    model.writeIncrAnalyses();
                    incrAnalyses.clear();
                }                
            }
        }
//        if(opts.saveIncrAnalysesFeatures && opts.parserType != Options.ParserType.oracle && !chart.get(words.length - 1).isEmpty())
//        {
//            ExtractFeatures incrAnalysesFeatures = model.getIncrAnalysesFeatures();            
//            synchronized(incrAnalysesFeatures)
//            {
//                boolean isOracle = opts.semanticsModel == Options.SemanticsModelType.oracle || opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;
//                incrAnalysesFeatures.processChart(ex.getName(), chart, isOracle);
//                if(!isOracle && incrAnalysesFeatures.size() >= opts.maxNumOfSentencesIncrAnalyses)
////                if(incrAnalysesFeatures.size() >= opts.maxNumOfSentencesIncrAnalyses)
//                {
//                    model.writeIncrAnalysesFeatures();
//                    incrAnalysesFeatures.clearFeatures();
//                }                
//            }
//        }
        return new Pair(getAnalyses(chart, words.length - 1, startTime, discriminative), opts.saveIncrAnalysesFeatures ? chart : null);
    }
   
    private void saveIncrAnalysesFeatures(List<StringTreeAnalysis> analyses, Chart chart, int oraclePos)
    {
        if(opts.saveIncrAnalysesFeatures && !chart.get(words.length - 1).isEmpty())
        {
            ExtractFeatures incrAnalysesFeatures = model.getIncrAnalysesFeatures();            
//            synchronized(incrAnalysesFeatures)
//            {
                StopWatchSet.begin("computeFeatures");
                boolean isOracleSemantics = opts.semanticsModel == Options.SemanticsModelType.oracle || opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;                    
                if(isOracleSemantics) // oracle-gold standard + oracle-maxF1
                    incrAnalysesFeatures.processChartOracle(ex.getName(), chart, analyses, analyses.get(oraclePos));
                else // full pred, oracle, full pred-sentenceLevel
                    incrAnalysesFeatures.processChart(ex.getName(), chart, analyses);
                StopWatchSet.end();
                // write features to disk
//                if(!isOracleSemantics && incrAnalysesFeatures.size() >= opts.maxNumOfSentencesIncrAnalyses)
                synchronized(incrAnalysesFeatures.getIncrAnalysesFeaturesSentLevel())
                {
                    if(!isOracleSemantics && incrAnalysesFeatures.sizeSentenceLevel() >= opts.maxNumOfSentencesIncrAnalyses)
    //                if(incrAnalysesFeatures.size() >= opts.maxNumOfSentencesIncrAnalyses)                    
                    {
                        model.writeIncrAnalysesFeatures();
                        incrAnalysesFeatures.clearFeatures();
                    }                    
                }                  
//                chart.clear();
//                chart = null;
//            }
        }
    }
    
    private List<String> convertStringTrees(List<StringTreeAnalysis> analyses)
    {
        List<String> list = new ArrayList<String>();
        for (StringTreeAnalysis sta : analyses)
        {            
            list.add(convertStringTree(sta));
        }
        return list;
    }
    
    public static String convertStringTree(StringTreeAnalysis sta)
    {                        
        sta.getStringTree().removeUnaryNodes(sta.getStringTree().getRoot());
        return sta.getStringTree().printNoTraces();
    }
    
    
    private Collection<ElementaryStringTree> getTrees(String word, String pos, boolean repeatParse, int wno)
    {         
        String wordCor = "";
        if (opts.train)
        {
            wordCor = word.trim();
        }
        else
        {
            for (String w : word.split("\t"))
            {
                String[] wordpos = w.split(" ");
                String w5;
                if (opts.goldPosTags & !opts.posOnly)
                {
                    w5 = model.getCutOffCorrectedMainLex(wordpos[1].toLowerCase());
                    wordCor = String.format("%s %s\t",wordpos[0], w5);
                }
                else
                {
                    w5 = model.getCutOffCorrectedMainLex(w.toLowerCase());
                    wordCor = w5 + "\t";
                }
            }
            wordCor = wordCor.trim();
        }

        wordCor = wordCor.toLowerCase();
//        double start = System.currentTimeMillis();
        //if (lexicon.containsKey(wordCor)){
        Collection<ElementaryStringTree> fulltrees = ex.getLexicon().getEntries(word, wordCor, pos, repeatParse, wno);
        if (opts.goldPosTags && opts.estimateProcDifficulty && pos.equals("AUX"))
        {
            fulltrees.addAll(ex.getLexicon().getEntries(word, wordCor, "VB", repeatParse, wno));
        }
//        double dur = (System.currentTimeMillis() - start) / 1000;
        return fulltrees;
    }

    private StringTreeAnalysis addMissingWords(StringTreeAnalysis stringTreeAnalysis, short wno, String[] words2)
    {

        for (int i = wno; i < words2.length; i++)
        {
            String bestpos = "";
            if (opts.goldPosTags)
            {
                bestpos = words2[i];
            }
            else
            {
                String[] wp = Utils.getWordPos(words2[i], opts.posOnly, opts.goldPosTags);
                Collection<String> pws = ex.getLexicon().getPOSs(wp[0]);
                if (pws == null || pws.isEmpty())
                {
                    bestpos = "nn " + wp[0];
                }
                else
                {
                    int max = 0;

                    for (String pw : pws)
                    {
                        if (pw.startsWith(" "))
                        {
                            continue;
                        }
                        int tranz = ex.getLexicon().getPosTagNo(pw);
                        if (tranz > max)
                        {
                            max = tranz;
                            bestpos = pw;
                        }
                    }
                }
            }
            StringTree st = stringTreeAnalysis.getStringTree();
            st.addNodeAtTop(bestpos);
        }
        return stringTreeAnalysis;
    }

    private List<StringTreeAnalysis> getAnalyses(Chart chart, int wordno, double startTime, boolean discriminative)
    {
//        boolean computeOracle = opts.parserType == Options.ParserType.oracle || opts.parserType == Options.ParserType.discriminativeOracle;
        boolean computeOracle = true;
        //double duration = System.currentTimeMillis() - startTime;
        //System.out.print("\n");//
//        Utils.logs("searchTime: " + duration + "words: " + wordno + "\t");
        if (opts.verbose)
        {
            Utils.logs(chart.printChartSize());
        }
        List<StringTreeAnalysis> analyses = new ArrayList<StringTreeAnalysis>();
//        double[] maxprob = new double[opts.nBest];
        Pair<double[], double[]> maxScoresProbs = newNegativeInfinitiyPairArray(opts.nBest);
//        double[] maxprobwrong = new double[opts.nBest];
        Pair<double[], double[]> maxScoresProbsWrong = newNegativeInfinitiyPairArray(opts.nBest);
//        double[] maxprobnonS = new double[opts.nBest];
        Pair<double[], double[]> maxScoresProbsNonS = newNegativeInfinitiyPairArray(opts.nBest);
//        Arrays.fill(maxprob, Double.NEGATIVE_INFINITY);        
//        Arrays.fill(maxprobwrong, Double.NEGATIVE_INFINITY);
//        Arrays.fill(maxprobnonS, Double.NEGATIVE_INFINITY);
        ArrayList<ChartEntry> chartSlice = chart.get(wordno);
        ArrayList<ChartEntry> topces = new ArrayList<ChartEntry>();//chartSlice.get(0);
        ArrayList<ChartEntry> topNonSentS = new ArrayList<ChartEntry>();
        ArrayList<ChartEntry> topwrongs = new ArrayList<ChartEntry>();//chartSlice.get(0);
        ArrayList<ChartEntry> validces = new ArrayList<ChartEntry>();
        chart.setTime(System.currentTimeMillis());
        boolean fullSent = false;
        //StringTreeAnalysis correctAnalysis = null;
        //long globalstarttime = System.currentTimeMillis();
        int j = 0;
        for (ChartEntry ce : chartSlice)
        {
            // need to account for noneAdjProbs on last fringe.
			/*for (BuildBlock b: ce.getBuildBlocks()){
            FreqCounter fc = new FreqCounter();
            //account for none adjup operations between lexical anchor and root.
            Fringe tsfringe = ce.getTreeState().getFringe();
            fc.accountForHistFringe(tsfringe, null, true);
            Node root = tsfringe.getLastAdjNode();
            boolean traceleft = tsfringe.hasTraceLeft();
            for (Fringe uf: ce.getTreeState().getUnaccessibles()){
            fc.accountForHistFringe(uf, null, true);
            root = uf.getLastAdjNode();
            traceleft = tsfringe.hasTraceLeft();
            }
            fc.addSingleTreeProbEl(new TreeProbElement(ParserOperation.substituteUpFF.name(), root.getCategory(), "NONESUBST",
            traceleft, false, root.getOrigTree(), 1, root.getLambda(), root.getLeftMostCover(), null));	
            
            //double newd = b.updateProbability(fc);
            //System.out.println(newd);
            }
            //			ce.updateProbs(ce.getTreeState().getFutureFringe());//doesn't work as will add previous b.probability a second time.
            //*/

            j++; 
            // incomplete parse - has prediction nodes and/or an unresolved substitution node in the fringe
            if (wordno == words.length - 1
                    && (!ce.getTreeState().getShadowTrees().isEmpty()
                    || ce.getTreeState().getFringe().getSubstNode() != null))
            {
                if (opts.calculateMostProbableTree)
                {
                    //collect calculateMostProbableTree wrong ones only in calculateMostProbableTree condition.
                    if(discriminative)
                        maxScoresProbsWrong = mergeProbsAndCEsDiscriminative(topwrongs, maxScoresProbsWrong, ce);
                    else
                        maxScoresProbsWrong.setSecond(mergeProbsAndCEs(topwrongs, maxScoresProbsWrong.getSecond(), ce));
                }		//System.out.println(j);
                continue;
            }
            fullSent = true; 
            // analysis that doesn't have S as the root symbol
            if (opts.calculateMostProbableTree && !ce.getTreeState().getLastFringe().getLastAdjNode().getCategory().startsWith("S"))
            {
                //collect calculateMostProbableTree wrong ones only in calculateMostProbableTree condition.
                if(discriminative)
                    maxScoresProbsNonS = mergeProbsAndCEsDiscriminative(topNonSentS, maxScoresProbsNonS, ce);
                else
                    maxScoresProbsNonS.setSecond(mergeProbsAndCEs(topNonSentS, maxScoresProbsNonS.getSecond(), ce));
                continue;
            }
            // complete analysis with S as the root symbol
            if (opts.calculateMostProbableTree)
            {
                //determine best analyses
                if(discriminative)
                    maxScoresProbs = mergeProbsAndCEsDiscriminative(topces, maxScoresProbs, ce);
                else
                    maxScoresProbs.setSecond(mergeProbsAndCEs(topces, maxScoresProbs.getSecond(), ce));
                validces.add(ce);
                //System.out.println(j+"\t"+maxprob[0]+"\t"+maxprob[1]+"\t"+maxprob[2]+"\t"+maxprob[3]+"\t"+maxprob[4]);
            }
            else
            {//add analyses
                ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = getEmptyBBList(chart, ce);
                chart.setTime(System.currentTimeMillis());
                ArrayList<StringTreeAnalysis> as = chart.getAnalyses(ce, emptyBBlist, "", opts.calculateMostProbableTree, discriminative ? ce.getBestScore() : ce.getBestProbability(), true);//true);//
//                FreqCounter freqCounterForCorrect = new Evaluator(model, opts).evaluate(as, solution, ex.getName(), false);
//                if (freqCounterForCorrect != null)                
                if (findCorrect(as, ex.getGoldStandardNoTraces()) != null)
                {
                    return as;
                }
                ce = null;
                //analyses.addAll(as);
                //System.out.println(System.currentTimeMillis()- globalstarttime);
//                if (System.currentTimeMillis() - globalstarttime > 600000)                                                
                if (ParserModel.useTimeOut && System.currentTimeMillis() - startTime > opts.timeOutStage2)
                {
                    if(!opts.silentMode)
                        LogInfo.logs("Time out at stage 2 - analyses reconstruction (" + (((double)opts.timeOutStage2 / 1000.0)/60.0)+ " min).");
                    break;
                }
            } // else
        } // for
        if (!fullSent)
        {//if no correct one was found, use best wrong analysis
            topces = topwrongs;
            maxScoresProbs = maxScoresProbsWrong;            
        }
        if (opts.calculateMostProbableTree)
        {
            // if there are no analyses with an S as their start symbol, or there are also analyses without an S as their start symbol *AND* top-scoring has
            // a higher score than the top-scoring of the former category, then search through the latter (top analysis only).
            if ((topces.isEmpty() && topNonSentS.size() > 0) || topNonSentS.size() > 0 && 
                    (discriminative ? maxScoresProbsNonS.getFirst()[0] > maxScoresProbs.getFirst()[0] + 5 : 
                    maxScoresProbsNonS.getSecond()[0] > maxScoresProbs.getSecond()[0] + 5))
            {                
//                ChartEntry topce = topNonSentS.get(0);
//                ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = this.getEmptyBBList(chart, topce);
//                //System.out.println("Analysis with probability"+maxprob[i]);
//                ArrayList<StringTreeAnalysis> as = chart.getAnalyses(topce, emptyBBlist, "", true, maxScoresProbsNonS.getSecond()[0]);
//                if(discriminative)
//                    setScoreToAnalyses(as, topce.getBestScore());
//                analyses.addAll(as);
//                if(opts.useSemantics)
//                    // NOTE: changed from analyses to as. This is to make sure we copy the right dependencies to their correspndoing analyses in the case of oracle models, 
//                    // where we keep an n-best list of analyses rather than the top-most.
//                    addDependencies(as, topce); // add dependencies on all analyses that correspond to the current chartEntry. 
  
                for (int i = 0; i < topces.size(); i++)
                {
                    ChartEntry topce = topces.get(i);
                    ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = this.getEmptyBBList(chart, topce);
                    //System.out.println("Analysis with probability"+maxprob[i]);
                    ArrayList<StringTreeAnalysis> as = chart.getAnalyses(topce, emptyBBlist, "", true, maxScoresProbs.getSecond()[i], false);
                    if(as != null)
                    {
                        if(discriminative)
                            setScoreToAnalyses(as, topce.getBestScore());
                        analyses.addAll(as);
                        if(opts.useSemantics)
                            // NOTE: changed from analyses to as. This is to make sure we copy the right dependencies to their correspndoing analyses in the case of oracle models, 
                            // where we keep an n-best list of analyses rather than the top-most.
                            addDependencies(as, topce); // add dependencies on all analyses that correspond to the current chartEntry
                    }                    
                    //System.out.println(as);
                    if (!computeOracle && !analyses.isEmpty()) // if we found an analysis then exit
                    {
                        break;
                    }                    
                } // for
                for (int i = 0; i < topNonSentS.size(); i++)
                {
                    ChartEntry topce = topNonSentS.get(i);
                    ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = this.getEmptyBBList(chart, topce);
                    //System.out.println("Analysis with probability"+maxprob[i]);
                    ArrayList<StringTreeAnalysis> as = chart.getAnalyses(topce, emptyBBlist, "", true, maxScoresProbs.getSecond()[i], false);
                    if(discriminative)
                        setScoreToAnalyses(as, topce.getBestScore());
                    analyses.addAll(as);
                    if(opts.useSemantics)
                        // NOTE: changed from analyses to as. This is to make sure we copy the right dependencies to their correspndoing analyses in the case of oracle models, 
                        // where we keep an n-best list of analyses rather than the top-most.
                        addDependencies(as, topce); // add dependencies on all analyses that correspond to the current chartEntry
                    //System.out.println(as);
                    if (!computeOracle && !analyses.isEmpty()) // if we found an analysis then exit
                    {
                        break;
                    }                    
                } // for
                if(analyses.size() > opts.nBest)
                {
                    return analyses.subList(0, opts.nBest);
                }
            }
            // if the topmost analysis without an S as their start symbol did not yield a result in the previous if-clause, 
            // go through the nBest analyses of the analyses with S *or* the incomplete analyses and compute a parse for the top analysis only.
            if (analyses.isEmpty())
            {
                for (int i = 0; i < opts.nBest; i++)
                {
                    if (topces.size() == i)
                    {
                        break;
                    }
                    ChartEntry topce = topces.get(i);
                    ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = this.getEmptyBBList(chart, topce);
                    //System.out.println("Analysis with probability"+maxprob[i]);
                    ArrayList<StringTreeAnalysis> as = chart.getAnalyses(topce, emptyBBlist, "", true, maxScoresProbs.getSecond()[i], false);
                    if(discriminative)
                        setScoreToAnalyses(as, topce.getBestScore());
                    analyses.addAll(as);
                    if(opts.useSemantics)
                        // NOTE: changed from analyses to as. This is to make sure we copy the right dependencies to their correspndoing analyses in the case of oracle models, 
                        // where we keep an n-best list of analyses rather than the top-most.
                        addDependencies(as, topce); // add dependencies on all analyses that correspond to the current chartEntry
                    //System.out.println(as);
                    if (!computeOracle && !analyses.isEmpty()) // if we found an analysis then exit
                    {
                        break;
                    }
                } // for
                if (analyses.isEmpty()) // search through the rest (other than top-scoring) analyses without an S symbol as their root symbol. Stop when we've found an analysis (top scoring only). 
                {
                    for (int i = 0; i < opts.nBest; i++)
                    {
                        if (topNonSentS.size() == i)
                        {
                            break;
                        }
                        ChartEntry topce = topNonSentS.get(i);
                        ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = this.getEmptyBBList(chart, topce);
                        //System.out.println("Analysis with probability"+maxprob[i]);
                        ArrayList<StringTreeAnalysis> as = chart.getAnalyses(topce, emptyBBlist, "", true, maxScoresProbsNonS.getSecond()[i], false);
                        if(discriminative)
                            setScoreToAnalyses(as, topce.getBestScore());
                        analyses.addAll(as);
                        if(opts.useSemantics)
                            addDependencies(as, topce); // add dependencies on all analyses that correspond to the current chartEntry
                        //System.out.println(as);
                        if (!computeOracle && !analyses.isEmpty()) // if we found an analysis then exit
                        {
                            break;
                        }
                    } // for

                    // search through *all* valid analyses (with S as their root symbol), i.e., not just the merged top nBest ChartEntries. 
                    // WARNING: it will cycle through analyses we have tried out before...
                    if (analyses.isEmpty()) 
                    {
//                        Utils.log("need to search among all results");
                        //return analyses;
//                        Utils.log("Need to search among all results: " + ex.getName());
                        for (ChartEntry ce : validces)
                        {
                            ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = this.getEmptyBBList(chart, ce);
                            //System.out.println("Analysis with probability"+maxprob[i]);
//                            for (Double d : ce.getNBestProbs())
                            for(int i = 0; i < ce.getNBestProbs().length; i++)
                            {
                                double prob = ce.getNBestProbs()[i];                                
                                ArrayList<StringTreeAnalysis> as = chart.getAnalyses(ce, emptyBBlist, "", true, prob, false);
                                if(discriminative)
                                    setScoreToAnalyses(as, ce.getnBestScores()[i]);
                                    
                                if (!(as == null || as.isEmpty()))
                                {
                                    analyses.addAll(as);
                                    if(opts.useSemantics)
                                        // NOTE: changed from analyses to as. This is to make sure we copy the right dependencies to their correspndoing analyses in the case of oracle models, 
                                        // where we keep an n-best list of analyses rather than the top-most.
                                        addDependencies(as, ce); // add dependencies on all analyses that correspond to the current chartEntry
                                    if(!computeOracle)
                                        break;
                                }
                            }
                        } // for
//                        double highestProb = Double.NEGATIVE_INFINITY;
//                        StringTreeAnalysis best = null;
                        // make sure we keep only the top-scoring analysis (Commented out, as analyses is a TreeList, hence automatically sorts StringTreeAnalyses
//                        for (StringTreeAnalysis sta : analyses)
//                        {
//                            if (sta.getStringTree().getProbability() > highestProb)
//                            {
//                                highestProb = sta.getStringTree().getProbability();
//                                best = sta;
//                            }
//                        } // for
//                        if(computeOracle)
//                        {
//                            if(best != null)
//                                analyses.add(0, best);
//                        }
//                        else
//                        {
//                            analyses.clear();
//                            if (best != null)
//                            {
//                                analyses.add(best);
//                                // NOTE: Commented out, as it is unneccesary (and wrong) to copy again dependencies; they have been copied already in the previous for-loop
//    //                            if(opts.useSemantics)
//    //                                addDependencies(analyses, topces.get(0)); // add dependencies on all analyses that correspond to the current chartEntry
//                            }
                        if(!computeOracle) // keep the top-scoring
                        {
                            analyses = analyses.subList(0, 1);                            
                        }                        
                    } // if
                } // if
            } // if
        } // if
        // search through *all* analyses without S as their root symbol.
        if (analyses.isEmpty() && !topNonSentS.isEmpty())
        {
            topces.addAll(topNonSentS);
            ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = this.getEmptyBBList(chart, topces.get(0));
            ArrayList<StringTreeAnalysis> as = chart.getAnalyses(topces.get(0), emptyBBlist, "", false, 0, false);
            if(discriminative)
                setScoreToAnalyses(as, topces.get(0).getBestScore());
            if (!as.isEmpty()) // (Commented out, as analyses is a TreeList, hence automatically sorts StringTreeAnalyses
            {                
//                StringTreeAnalysis best = as.get(0);
//                double highestProb = Double.NEGATIVE_INFINITY;
//                for (StringTreeAnalysis sta : as)
//                {
//                    if (sta.getStringTree().getProbability() > highestProb)
//                    {
//                        highestProb = sta.getStringTree().getProbability();
//                        best = sta;
//                    }
//                }
//                if(computeOracle)
//                    analyses.add(best);
//                else
//                {
                    analyses.addAll(as);
//                    analyses.add(0, best);
//                }
                if(opts.useSemantics)
                    addDependencies(analyses, topces.get(0)); // add dependencies on all analyses that correspond to the current chartEntry
            }//*/
            else
            {
                if(!opts.silentMode)
                    Utils.log("no complete analyses retrieved: " + ex.getName());
                return analyses;
            }
        }
        //sort analyses by score/probability
        Collections.sort(analyses);
        return analyses;
    }
    
    private void setScoreToAnalyses(List<StringTreeAnalysis> analyses, double score)
    {
        if(analyses == null)
            return;
        for(StringTreeAnalysis analysis : analyses)
            analysis.setScore(score);
    }
    
    public StringTreeAnalysis findCorrect(List<StringTreeAnalysis> analyses, String goldStandard)
    {
        if (analyses == null)
        {
            return null;
        }                            
        for (StringTreeAnalysis sta : analyses)
        {            
            if (goldStandard.equals(convertStringTree(sta)))
            {
                return sta;
            }
        }        
        return null; // no correct analysis found
    }
    
    private double[] mergeProbsAndCEs(ArrayList<ChartEntry> bestces, double[] maxprobs, ChartEntry chartEntry)
    {
        double[] mergedarray = new double[opts.nBest];
        Arrays.fill(mergedarray, Double.NEGATIVE_INFINITY);
        int a = 0;
        int b = 0;
        for (int i = 0; i < opts.nBest; i++)
        {
            if (maxprobs[a] > chartEntry.getNBestProbs()[b])
            {
                mergedarray[i] = maxprobs[a];
                a++;
            }
            else
            {
                if (chartEntry.getNBestProbs()[b] > Double.NEGATIVE_INFINITY)
                {
                    mergedarray[i] = chartEntry.getNBestProbs()[b];
                    bestces.add(i, chartEntry);
                    if (bestces.size() == opts.nBest + 1)
                    {
                        bestces.remove(opts.nBest);
                    }
                    b++;
                }
                else
                {
                    return mergedarray;
                }
            }
        }
        return mergedarray;
    }
    
    /**
     * Merge n-best lists of multiple cell entries of the chart, based on their discriminative model score (rerank)
     * @param bestces
     * @param maxScoresProbs
     * @param chartEntry
     * @return 
     */
    private Pair<double[], double[]> mergeProbsAndCEsDiscriminative(ArrayList<ChartEntry> bestces, Pair<double[], double[]> maxScoresProbs, ChartEntry chartEntry)
    {
        Pair<double[], double[]> mergedarray = newNegativeInfinitiyPairArray(opts.nBest);
        int a = 0;
        int b = 0;
        for (int i = 0; i < opts.nBest; i++)
        {
            if (maxScoresProbs.getFirst()[a] > chartEntry.getnBestScores()[b])
            {
                mergedarray.getFirst()[i] = maxScoresProbs.getFirst()[a];
                mergedarray.getSecond()[i] = maxScoresProbs.getSecond()[a];
                a++;
            }
            else
            {
                if (chartEntry.getnBestScores()[b] > Double.NEGATIVE_INFINITY)
                {
                    mergedarray.getFirst()[i] = chartEntry.getnBestScores()[b];
                    mergedarray.getSecond()[i] = chartEntry.getNBestProbs()[b];
                    bestces.add(i, chartEntry);
                    if (bestces.size() == opts.nBest + 1)
                    {
                        bestces.remove(opts.nBest);
                    }
                    b++;
                }
                else
                {
                    return mergedarray;
                }
            }
        }
        return mergedarray;
    }

    public static ArrayList<ArrayList<BuildBlock>>[] getEmptyBBList(Chart chart, ChartEntry ce)
    {
        ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = new ArrayList[chart.length()];
        FringeAndProb fap = ce.getTreeState().getFutureFringe();
        ArrayList<BuildBlock>[] contListNew = new ArrayList[chart.length()];
        int wordIndex = 0;
        if (fap.getBBHist() != null)
        {
            for (LinkedList<BuildBlock> lb : fap.getBBHist())
            {
                for (BuildBlock b : lb)
                {
                    if (b.getPrevChartEntry() == null)
                    {
                        continue;
                    }
                    if (contListNew[wordIndex] == null)
                    {
                        contListNew[wordIndex] = new ArrayList<BuildBlock>();
                    }
                    if (!contListNew[wordIndex].contains(b))
                    {
                        contListNew[wordIndex].add(b);
                    }
                }
                wordIndex++;
            }
        }
        for (int i = 0; i < contListNew.length; i++)
        {
            ArrayList<BuildBlock> bblist = contListNew[i];
            emptyBBlist[i] = new ArrayList<ArrayList<BuildBlock>>();
            if (bblist != null && !bblist.isEmpty())
            {
                emptyBBlist[i].add(bblist);
            }
        }

        return emptyBBlist;
    }
   
    /**
     * Combines two trees by first checking whether they can be combined (no two shadow trees, or unused shadow tree)
     * and then calling the integration function and inserting results into the chart.
     * @param trees
     * @param chart
     * @param chartindex
     * @param posOfNextWord 
     * @return
     */
    @SuppressWarnings("unchecked")//
    private ArrayList<ChartEntry> combineTrees(Collection<ElementaryStringTree> trees, Chart chart,// Collection<ChartEntry> lastSliceCopy, 
                                               short chartindex, String[] origPosTags, double startTime, 
                                               String posOfNextWord, SuperTagger superTagger, int beamWidth, boolean endfOfSent, int timestamp)
    {
        ArrayList<ChartEntry> newEntries = new ArrayList<ChartEntry>();
        // try to integrate each tree (some may have two alternative fringes) 
        // with prefix trees from prev slice.
        // add successful combinations to chart.
        ArrayList<ChartEntry> lastSliceCopy = getLastSlice(chartindex, chart);       
        HashMap<ElementaryStringTree, ArrayList<TreeState>> treeStateMap = new HashMap<ElementaryStringTree, ArrayList<TreeState>>();
        double totalTimeSuperTag = 0.0;
        int a = 0;
        int b = 0;
        double bestprob = Double.NEGATIVE_INFINITY;
        for (ChartEntry chartEntry : lastSliceCopy)
        {
            a++;
            Collection<ElementaryStringTree> selectedTrees;
            double start = System.currentTimeMillis();
            //System.out.print(trees.size()+" ");
            // call the supertagger only on prediction trees (when trees are elementary trees, posOfNextWord is always  "")
            boolean shadowTree = !posOfNextWord.equals("");
//            selectedTrees = opts.train || shadowTree ? superTagger.superTag(trees, chartEntry, posOfNextWord) : trees ;
            selectedTrees = opts.train || !shadowTree ? trees : superTagger.superTag(trees, chartEntry, posOfNextWord);
          //  selectedTrees = trees;
            //System.out.println(selectedTrees.size());
            totalTimeSuperTag += (System.currentTimeMillis() - start);
            for (ElementaryStringTree tree : selectedTrees)
            {                
                b++;
                ArrayList<TreeState> elemtreeStates;
                if (treeStateMap.containsKey(tree))
                {
                    elemtreeStates = treeStateMap.get(tree);
                }
                else
                {
                    elemtreeStates = getTreeStateForTree(tree, chartindex);
                    treeStateMap.put(tree, elemtreeStates);
                }
                for (TreeState elemtreetreeState : elemtreeStates)
                {
                    MultiValueMap<String, ParserOperation> combinations = new MultiValueMap();
                    //		if (System.currentTimeMillis() - startTime > 60000) return newEntries;
                    if (tree.hasShadowInd() && chartEntry.getTreeState().getShadowTrees().size() > 2)
                    {
                        continue;
                    }
                    TreeState treeState = chartEntry.getTreeState();
                    if (opts.verbose && treeState.getUnaccessibles().isEmpty() && treeState.getFutureFringe().getNext() != null)
                    {
                        LogInfo.error("why not expanded???");
                    }
                    String fringeString = treeState.getFringe().toString();
                    List<ChartEntry> ces = new ArrayList<ChartEntry>();
                    if (combinations.containsKey(fringeString))
                    {
                        Collection<ParserOperation> operations = combinations.getCollection(fringeString);
                        if (!elemtreetreeState.getRootNode().isShadow())
                        {
                            ces.addAll(match(tree, elemtreetreeState, chart, chartEntry, origPosTags, chartindex));
                        }
                        if (operations.size() == 1 && operations.contains(null))
                        {
                            //don't need to do anything because this prefix tree does not integrate with prefix tree
                            //need to do this at tree level.
                        }
                        else
                        {
                            for (ParserOperation operation : operations)
                            {
                                if (operation == null || operation == ParserOperation.verify)
                                {
                                    continue;
                                }
                                else
                                {
                                    ces.addAll(operation.combine(model, opts, words, origPosTags, treeState, elemtreetreeState, tree, chartindex));
                                }
                            }
                        }
                    }
                    else
                    {//*/
                        combinations.put(fringeString, null);
                        if (combineTwoShadowTreesInARow(chartEntry, elemtreetreeState))
                        {
                            continue;
                        }
                        //*****************
                        ces = integrate(treeState, elemtreetreeState, tree, chart, chartEntry, origPosTags, chartindex);
                        //*****************
                    }
                    // clean up results
                    for (ChartEntry cefirst : ces)
                    {
                        ArrayList<ChartEntry> expandedCEs = makeExpansions(cefirst);
                        for (ChartEntry ce : expandedCEs)
                        {
                            for (BuildBlock bb : ce.getBuildBlocks())
                            {
                                //System.out.println(bb.toString()+ bb.getIpi()+"ELEM:" + tree);
                                if (didNotUseShadow(chartEntry, bb, bb.getIpi(), chartindex, tree))
                                {
                                    LinkedList<BuildBlock> list = new LinkedList<BuildBlock>();
                                    list.add(bb);
//                                    ce.getTreeState().getFutureFringe().getBBHist().remove(bb); // TODO: FIX
                                    ce.getTreeState().getFutureFringe().getBBHist().remove(list); // TODO: FIX
                                    continue;
                                }
                                bb.setPrevChartEntry(chartEntry);
                                if (!opts.train || opts.useProbabilityModel)
                                {
                                    bb.retrieveProbability(model.getParams(), opts.freqBaseline);
                                    bb.removeElemTree();
//								bb.removeFreqCounter();
                                }
                                if (!elemtreetreeState.getShadowTrees().isEmpty())
                                {
                                    ShadowStringTree shadowt = ce.getTreeState().getShadowTrees().get(
                                            ce.getTreeState().getShadowTrees().size() - 1);
                                    shadowt.setPredictProb(bb.getProbability());
                                }
                                if (!combinations.containsValue(fringeString, bb.getOperation()))
                                {
                                    combinations.put(fringeString, bb.getOperation());
                                }
                                //System.out.print("\n"+ce.getTreeState().getFringe());
                                if (!opts.train || opts.useProbabilityModel)
                                {
//                                    double vlap = computeLAP(tree);
                                    ce.updateProbs(ce.getTreeState().getFutureFringe(), 0);
                                }
                                //System.out.println(ce.getBestProbability()+"\t"+ce+"\n"+elemtreetreeState+"\n"+tree+a+" "+b+"\n---\n");
                                if(opts.pruneUsingScores)
                                {                                    
                                    if (ce.getBestScore()> bestprob)
                                    {
                                        bestprob = ce.getBestScore();
                                    }
                                    if (ce.getBestScore() > bestprob - beamWidth)
                                    {
                                        newEntries.add(ce);
                                    }
                                }
                                else
                                {
                                    double bestPropWithVlap = ce.getBestProbability() + (opts.train || !shadowTree ? 0.0d : computeLAP(tree));
                                    if (bestPropWithVlap > bestprob)
                                    {
                                        bestprob = bestPropWithVlap;
                                    }
                                    if (opts.train || bestPropWithVlap > bestprob - beamWidth)
                                    {
                                        newEntries.add(ce);
                                    }
                                }                                
                            } // for each build block
                        } // for each expanded chart entry
                    } // for each new chart entry
                } // for each elementary tree state (fringes) ALWAYS ONE
            } // for each elementary tree
        } // for each chart entry
//        if (opts.timeProfile && totalTimeSuperTag > 100)
//        {
//            Utils.log("supertagtime: " + totalTimeSuperTag / 1000 + "\t");
//        }
        return newEntries;
    }

    /**
     * expands the fringe as needed using fringe continuations (making currently
     * invisible unaccessible fringes visible) and adding a version of the fringe
     * which is shifted to after-empty-element-position if applicable.   
     */
    @SuppressWarnings("unchecked")
    private ArrayList<ChartEntry> makeExpansions(ChartEntry ce)
    {
        ArrayList<ChartEntry> expandedCEs = new ArrayList<ChartEntry>();
        TreeState treeStateShrunk = ce.getTreeState();
        if (treeStateShrunk.getUnaccessibles().isEmpty())
        {
            ArrayList<TreeState> expanded = treeStateShrunk.expand(ce, opts.nBest, opts.useSemantics, model);
            if (expanded == null)
            {
                return expandedCEs;
            }
            for (TreeState expandedTS : expanded)
            {
                ArrayList<TreeState> tfes = expandedTS.fixForTrace(words);
                FreqCounter fc = new FreqCounter(opts);
                for (TreeState traceFixedExpanded : tfes)
                {
                    ArrayList<TreeState> fullyExpanded;
                    if (traceFixedExpanded.getFringe().hasTraceLeft() && traceFixedExpanded.getUnaccessibles().isEmpty())
                    {
                        fullyExpanded = traceFixedExpanded.expand(ce, opts.nBest, opts.useSemantics, model);
                        if (fullyExpanded == null)
                        {
                            fullyExpanded = new ArrayList<TreeState>();
                            fullyExpanded.add(traceFixedExpanded);
                        }
                        else if (fullyExpanded.isEmpty())
                        {
                            fullyExpanded.add(traceFixedExpanded);
                        }
                    }
                    else
                    {
                        fullyExpanded = new ArrayList<TreeState>();
                        fullyExpanded.add(traceFixedExpanded);
                    }
                    for (TreeState fex : fullyExpanded)
                    {
                        ChartEntry expandedCE;
                        if (fc.isEmpty())
                        {
                            expandedCE = new ChartEntry(opts, fex, ce.getBuildBlocks());
                        }
                        else
                        {
                            ArrayList<BuildBlock> oldbuildblocks = ce.getBuildBlocks();
                            //System.err.println(oldbuildblocks.toString());
                            ArrayList<BuildBlock> newbuildBlocks = ce.getBuildBlocksClone();
                            expandedCE = new ChartEntry(opts, fex, newbuildBlocks);
                            fex.getFutureFringe().getBBHist().getLast().removeAll(oldbuildblocks);
                            for (BuildBlock nbb : newbuildBlocks)
                            {
                                nbb.getFreqCounter().join(fc);
//								 nbb.updateProbability(fc);
                                fex.getFutureFringe().getBBHist().getLast().add(nbb);
                            }
                        }
                        /*						 if (ParserModel.useProbabilityModel) {//expandedCE.setProbability(fex.getLastFringe().getProbability(ce));
                        if (!fc.isEmpty())
                        expandedCE.updateProbs(fex.getFutureFringe());
                        expandedCE.setProbability(fex.getFutureFringe().getnBestProbs());
                        }*/
                        expandedCEs.add(expandedCE);
                    }
                    fc.accountForHistFringe(traceFixedExpanded.getFringe(), null, true);
                }
            }
        }
        if (expandedCEs.isEmpty())
        {
            expandedCEs.add(ce);
        }
        return expandedCEs;
    }//*/

    /**
     * get previous chart slice if not first word, otherwise return empty chartSlice;
     * @param chartindex
     * @param chart
     * @return
     */
    private ArrayList<ChartEntry> getLastSlice(int chartindex, Chart chart)
    {
        ArrayList<ChartEntry> lastChartSlice;
        if (chartindex == 0)
        {
            lastChartSlice = new ArrayList<ChartEntry>();
            lastChartSlice.add(new ChartEntry(opts, opts.useSemantics ? new DepTreeState(model) : new TreeState()));
        }
        else
        {
            lastChartSlice = chart.get(chartindex - 1);
        }
        return lastChartSlice;
    }

    private ArrayList<TreeState> getTreeStateForTree(ElementaryStringTree tree, short chartindex) // TODO: FIX (no need to return arraylist)
    {
        ArrayList<TreeState> elemtreeStates = new ArrayList<TreeState>();
        TreeState elemTreeState;
        ArrayList<Fringe> shadowFringes = ex.getShadowTreesMap().get(tree);
        if (shadowFringes != null)
        {
            elemTreeState = new TreeState(tree, (short) (chartindex - 1), shadowFringes, opts.nBest);//TS
        }
        else
        { //System.out.println("for lex tree number"+treeno +"/"+trees.size());
            elemTreeState = new TreeState(tree, chartindex, opts.nBest);//TS
        }
        elemtreeStates.add(elemTreeState);

        /*	if (elemtreets.getFringe().hasTraceRight()){
        //TreeState nts = elemtreets.copy(); //;
        TreeState nts;//= //new TreeState(tree, chartindex);//TS				
        if (shadowTreesMap.containsKey(tree)){
        nts = new TreeState(tree, (short) (chartindex-1), shadowTreesMap.get(tree));//TS
        }
        else{
        nts = new TreeState(tree, chartindex);//TS
        }
        nts.shiftNextUnaccessibleToAccessible();
        if (chartindex==0) nts.addLeftMostAnnot("SoS+"+nts.getFringe().getAdjNodesOpenRight().get(0).getCategory());
        else nts.addLeftMostAnnot(words[chartindex-1].substring(0,words[chartindex-1].indexOf(" "))+"+"+nts.getFringe().getAdjNodesOpenRight().get(0).getCategory());
        elemtreeStates.add(nts);
        if (nts.getFringe().hasTraceRight()){
        //TreeState nts = elemtreets.copy(); //;
        TreeState nts2;//= //new TreeState(tree, chartindex);//TS				
        if (shadowTreesMap.containsKey(tree)){
        nts2 = new TreeState(tree, (short) (chartindex-1), shadowTreesMap.get(tree));//TS
        }
        else{
        nts2 = new TreeState(tree, chartindex);//TS
        }
        nts2.shiftNextUnaccessibleToAccessible();
        nts2.shiftNextUnaccessibleToAccessible();
        if (chartindex==0) nts2.addLeftMostAnnot("SoS+"+nts2.getFringe().getAdjNodesOpenRight().get(0).getCategory());
        else nts2.addLeftMostAnnot(words[chartindex-1].substring(0,words[chartindex-1].indexOf(" "))+"+"+nts2.getFringe().getAdjNodesOpenRight().get(0).getCategory());
        elemtreeStates.add(nts2);
        }
        }//*/
        return elemtreeStates;
    }

    private boolean didNotUseShadow(ChartEntry ce, BuildBlock thisBuildBlock, Node ipi, int chartindex, ElementaryStringTree tree)
    {
        //make use of timestamp???
        ArrayList<BuildBlock> buildBlocks = ce.getBuildBlocks();
        if (thisBuildBlock.getOperation() == ParserOperation.initial)
        {
            return false;
        }
        for (BuildBlock b : buildBlocks)
        {
            //previously added tree was justified if it had a lexical anchor.
            if (b.hasNonShadowLeaf())
            {
                return false;
            }
        }
        // in case of a verification, the latest shadow tree was not used if the verification nodes have a timestamp older than 1 step ago.
        if (thisBuildBlock.getOperation() == ParserOperation.verify && ipi.getTimeStamp() - chartindex < -1)
        {
            return true;
        }
        // if the last tree was a shadow tree and it is being verified now, it is only justified 
        // to have been used if there is more than one shadow index, otherwise, nothing is
        //  gained by using the prediction tree, and leaving it in would lead to spurious ambiguities.
        if (thisBuildBlock.getOperation() == ParserOperation.verify
                && ipi.getTimeStamp() - chartindex >= -1 //				&& thisBuildBlock.getVerifiedTree().getShadowSourceTreesRootList().size()==1
                )
        {
            String verifiedStruct = thisBuildBlock.getVerifiedTreeStruct();
            String origStruct = buildBlocks.get(0).getElemTreeString();
            verifiedStruct = verifiedStruct.replaceAll(" ", "");
            verifiedStruct = verifiedStruct.replaceAll("[+]", "");
            verifiedStruct = verifiedStruct.replaceAll("[-]", "");
            origStruct = origStruct.replaceAll(" ", "");
            origStruct = origStruct.replaceAll("[-]", "");
            origStruct = origStruct.replaceAll("[+]", "");
            if (verifiedStruct.equals(origStruct) && thisBuildBlock.getNoOfVerifiedNodes() == verifiedStruct.split("_").length - 1)
            {
                //&& buildBlocks.get(0).getElemTree().getShadowSourceTreesRootList().size()==1){
                return true;//(S^null_2(NP^2_1(:^1_1 *^1_1)) VP^2_2)
            }
        }
        if (thisBuildBlock.getOperation() == ParserOperation.verify)
        {
            return false;
        }
        // shadow tree was used if ipi has both indexes with pattern, (adjunction) 
        // or one index null, other pattern (substitution, or adj at subst node)
        // or verification with tree with pattern. (verification) -> should hopefully be found otherwise.
        byte ipiD = ipi.getDownIndex();
        byte ipiU = ipi.getUpIndex();
        if ((ipiD > 0 && ipiU > 0)
                || ((ipiD == -1 && ipiU != 0) || (ipiU == -1 && ipiD == 0)))
        {//TODO check ipiD==0 in last condition.
            return false;
        }
        return true;
    }

    private boolean combineTwoShadowTreesInARow(ChartEntry chartEntry, TreeState elemTreeTS)
    {
        if (chartEntry.getBuildBlocks().isEmpty())
        {
            return false;
        }
        for (BuildBlock prefbb : chartEntry.getBuildBlocks())
        {
            //if last added tree was shadow tree and this is shadow tree, don't do anything.
            //BuildBlock prefbb = chartEntry.getBuildBlock();
            if (!prefbb.hasNonShadowLeaf() && !elemTreeTS.hasNonShadowLeaf())
            {
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    private ArrayList<ChartEntry> integrate(TreeState preftreetreeState, TreeState elemtreetreeState,
                                            ElementaryStringTree tree, Chart chart, ChartEntry chartEntry, String[] origPosTags, int timestamp)
    {
        //System.out.print(".");
        //combine the two treeStates;
        ArrayList<ChartEntry> chartentries = new ArrayList<ChartEntry>();
        if (preftreetreeState.getFringe().isEmpty())
        {
            chartentries.addAll(ParserOperation.initial.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
            return chartentries;
        }
        else if (!elemtreetreeState.getRootNode().isShadow())
        {
            chartentries = match(tree, elemtreetreeState, chart, chartEntry, origPosTags, timestamp);
        }
        // in general, operations should take care of whether input is valid themselves.
        if (elemtreetreeState.isAux())
        {
            chartentries.addAll(ParserOperation.adjoinDownXF.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
            chartentries.addAll(ParserOperation.adjoinDownXS.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
            //chartentries.addAll(ParserOperation.adjoinDownSF.combine(preftreetreeState, elemtreetreeState));
            //chartentries.addAll(ParserOperation.adjoinDownSS.combine(preftreetreeState, elemtreetreeState));
        }
        else
        {
            chartentries.addAll(ParserOperation.substituteDownXF.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
            chartentries.addAll(ParserOperation.substituteDownXS.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
            //chartentries.addAll(ParserOperation.substituteDownSF.combine(preftreetreeState, elemtreetreeState));
            //chartentries.addAll(ParserOperation.substituteDownSS.combine(preftreetreeState, elemtreetreeState));
        }
        if (preftreetreeState.isAux())
        {
            chartentries.addAll(ParserOperation.adjoinUpFF.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
            chartentries.addAll(ParserOperation.adjoinUpFS.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
        }
        else
        {
            chartentries.addAll(ParserOperation.substituteUpFF.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
            chartentries.addAll(ParserOperation.substituteUpFS.combine(model, opts, words, origPosTags, preftreetreeState, elemtreetreeState, tree, timestamp));
        }

        return chartentries;
    }

    private ArrayList<ChartEntry> match(ElementaryStringTree tree, TreeState elemtreetreeState, Chart chart, 
            ChartEntry chartEntry, String[] origPosTags, int timestamp)
    {
        TreeState preftreetreeState = chartEntry.getTreeState();
        ArrayList<ShadowStringTree> shadowtrees = preftreetreeState.getShadowTrees();
        ArrayList<ChartEntry> chartentries = new ArrayList<ChartEntry>();
        for (ShadowStringTree shadowtree : shadowtrees)
        {
            for (Integer rootnode : shadowtree.getShadowSourceTreesRootList())
            {
                if (shadowtree.getTreeOrigIndex().getCategory(rootnode).equals(tree.getCategory(tree.getRoot())))
//                if (shadowtree.getTreeOrigIndex().getCategory(Integer.toString(rootnode)).equals(tree.getCategory(tree.getRoot())))
                {
                    Map<Integer, Integer> coveredNodes = getCoveredNodes(tree, tree.getRoot(), shadowtree, rootnode, preftreetreeState.getFringe().getSubstNode(), opts.verbose);
//                    HashMap<Integer, Integer> coveredNodes = getCoveredNodes(tree, tree.getRoot(), shadowtree, Integer.toString(rootnode), preftreetreeState.getFringe().getSubstNode());
                    //All nodes that are not in coveredNodes must be after anchor. 
                    // if a tree contains a trace, it can verify more than one index.
                    //Integer[] indexlist  
                    if (coveredNodes == null)
                    {
                        continue;
                    }
                    ArrayList<Byte> pattern = checkTreeEquivalence(shadowtree, rootnode, tree, coveredNodes);
                    //all subst nodes before anchor must be substituted in.
                    for (int i = 1; i < tree.getAnchor(); i++)
//                    for (int i = 1; i < Integer.parseInt(tree.getAnchor()); i++)
                    {
                        if (tree.getNodeType(i) == TagNodeType.subst && !coveredNodes.containsValue(i))
//                        if (tree.getNodeType(String.valueOf(i)) == TagNodeType.subst && !coveredNodes.containsValue(i))
                        {
                            pattern = null;
                            break;
                        }
                    }
                    if (pattern == null)
                    {
                        continue;
                    }
                    //if (index == -1) continue;
//					check whether incrementality ok (subst nodes before lexical anchor have already been filled)
                    if (preftreetreeState.getFringe().getSubstNode() == null)
                    {
                        if (StringTreeAnalysis.matches(pattern, preftreetreeState.getFringe().getLastAdjNode().getDownIndex()))
                        {
                            //TODO preftreetreeState need to be expanded up to root node of shadow tree.
                            ArrayList<TreeState> expandedTreeStates = new ArrayList<TreeState>();
                            if (shadowtree.getTreeOrigIndex().isAuxtree() && !shadowtree.getTreeOrigIndex().hasFootLeft())
                            {
                                expandedTreeStates = preftreetreeState.expand(chartEntry, opts.nBest, opts.useSemantics, model);
                            }
                            if (expandedTreeStates == null)
                            {
                                return chartentries;
                            }
                            if (expandedTreeStates.isEmpty())
                            {
                                expandedTreeStates.add(preftreetreeState);
                            }
                            for (TreeState pref : expandedTreeStates)
                            {
                                for (ChartEntry ce : ParserOperation.verify.combine(model, opts, words, origPosTags, pref, elemtreetreeState, 
                                        pattern, tree, shadowtree, coveredNodes, shadowtree.getOffsetNodeIds(), timestamp))
                                {
                                    chartentries.add(ce);
                                }
                            }
                            return chartentries;
                        } // if
                    } // if
                } // if
            } // for
        } // for
        return chartentries;
    }

    /**
     * 
     * @param shadowtree
     * @param rootnode
     * @param tree
     * @param coveredNodes < shadownodeInt, treeNodeInt>
     * @return
     */
    @SuppressWarnings("unchecked")//at cast for clone().
    private ArrayList<Byte> checkTreeEquivalence(ShadowStringTree shadowtree, Integer rootnode, StringTree tree, Map<Integer, Integer> coveredNodes)
    {
        ArrayList<Byte> pattern = new ArrayList<Byte>();
        //int index = Integer.parseInt(shadowtree.getLowerIndex(rootnode));
//		 go through tree and see whether all nodes in shadow tree have same index
        //(same category, same nodetype, same parents not regarding adjunction trees)
        // have to go through in tree order to do this correctly.
        if (coveredNodes == null)
        {
            return null;
        }
        for (Integer shadowId : shadowtree.getShadowSourceTreesRootList())
        {
            if (coveredNodes.containsKey(shadowId) && tree.getLowerIndex(coveredNodes.get(shadowId)) != -1)
            {
                // get the updated index of the root's lowerIndex
                byte index = shadowtree.getLowerIndex(shadowId);
                pattern.add(index);
            }
        }
        //Pattern p = Pattern.compile(pattern);
        // check that there are no leftover-nodes in shadow tree with that index
        ArrayList<Integer> allCoveredTest = (ArrayList<Integer>) shadowtree.getTreeOrigIndex().getNodes().clone();
        allCoveredTest.removeAll(coveredNodes.keySet());
        for (Integer node : allCoveredTest)
        {
            if ((shadowtree.getLowerIndex(node) > -1 && StringTreeAnalysis.matches(pattern, shadowtree.getLowerIndex(node))
                    || (shadowtree.getUpperIndex(node) > -1 && StringTreeAnalysis.matches(pattern, shadowtree.getUpperIndex(node)))))
            {
                return null;
            }
        }



        return pattern;
    }

    /**
     * find the mapping between the verification tree and the shadow nodes it covers.
     * @param tree
     * @param node
     * @param shadowtree
     * @param shadowNode
     * @return
     */
    //All nodes before anchor must have been predicted!!!
    public static Map<Integer, Integer> getCoveredNodes(StringTree tree, int node, ShadowStringTree shadowtree, int shadowNode, Node prefixtreeFringeSubstnode, boolean verbose)
    {
        //String index = shadowtree.getLowerIndex(Integer.parseInt(shadowtree.getRoot()));
        ArrayList<Byte> pattern = new ArrayList<Byte>();
        for (Integer nodeid : shadowtree.getShadowSourceTreesRootList())
        {
            pattern.add(shadowtree.getLowerIndex(nodeid));
        }
        //Pattern pattern = Pattern.compile(pat);
        //check same category, indexes (as proxy for node type) and parent
//        Map<Integer, Integer> coverednodes = new DualHashBidiMap<Integer, Integer>();
        Map<Integer, Integer> coverednodes = new HashMap<Integer, Integer>();
//        int shadowNode = Integer.parseInt(shadowNode);
        if (tree.getCategory(node).equals(shadowtree.getTreeOrigIndex().getCategory(shadowNode))
                && (//(shadowtree.getLowerIndex(shadowNodeInt)==-1||
                StringTreeAnalysis.matches(pattern, shadowtree.getLowerIndex(shadowNode)) ||//)||
                //(shadowtree.getUpperIndex(shadowNodeInt)==-1 || 
                StringTreeAnalysis.matches(pattern, shadowtree.getUpperIndex(shadowNode)))//)
                )
        {
            //shadowtree.getUpperIndex(shadowNodeInt).equals(index)))){
            if (tree.getNodeType(node) == TagNodeType.subst || tree.getNodeType(node) == TagNodeType.foot)
            {
                if (!StringTreeAnalysis.matches(pattern, shadowtree.getUpperIndex(shadowNode)))
                {
                    return null;
                }/*
                else if (p.matcher(shadowtree.getUpperIndex(shadowNodeInt)).matches()&&
                shadowtree.getLowerIndex(shadowNodeInt)==null){
                
                }*/

                else if (StringTreeAnalysis.matches(pattern, shadowtree.getUpperIndex(shadowNode))
                        && //shadowtree.getLowerIndex(shadowNodeInt)!=null&&
                        shadowtree.getLowerIndex(shadowNode) == shadowtree.getUpperIndex(shadowNode))
                {
                    return null;
                }
            }
            else if (!tree.getChildren(node).isEmpty())
            {
                byte shadowtreelowerNodeindex = shadowtree.getLowerIndex(shadowNode);
                if (//shadowtree.getLowerIndex(shadowNodeInt)!=null && 
                        StringTreeAnalysis.matches(pattern, shadowtreelowerNodeindex))
                {
                    int childno = 0;
                    boolean headchildseen = false;
                    for (Integer child : tree.getChildren(node))
                    {
                        ArrayList<Integer> shadowtreechildren = shadowtree.getTreeOrigIndex().getChildren(shadowNode);
                        if (tree.getNodeType(child) == TagNodeType.foot && headchildseen)
                        {//!tree.hasFootLeft()){
                            if (shadowtreechildren.size() <= childno)
                            {
                                return null;
                            }
                            Map<Integer, Integer> dr =
                                    getCoveredNodes(tree, child, shadowtree, shadowtreechildren.get(childno), prefixtreeFringeSubstnode, verbose);
                            if (dr == null)
                            {
                                return null;
                            }
                            coverednodes.putAll(dr);
                        }/*
                        else if (tree.getNodeType(child)==TagNodeType.internal && headchildseen){
                        if (shadowtree.getChildren(shadownode).size()<=childno){
                        return null;
                        }
                        HashMap<Integer, Integer> dr = 
                        getCoveredNodes(tree, child, shadowtree, shadowtree.getChildren(shadownode).get(childno));
                        if (dr == null) {
                        coverednodes.put(Integer.parseInt(shadowtree.getChildren(shadownode).get(childno)),Integer.parseInt(child));
                        }
                        else coverednodes.putAll(dr);							
                        }*/
                        if (headchildseen)
                        {
                            break;
                        }
                        // if the child structure doesn't match it can't work.
                        // however, if this is the spine, the shadow tree can end earlier.
                        if (shadowtreechildren == null || shadowtreechildren.size() <= childno)
                        {
                            if (tree.isHeadChild(child))
                            {
                                break;
                            }
                            return null;
                        }
                        Map<Integer, Integer> dr =
                                getCoveredNodes(tree, child, shadowtree, shadowtreechildren.get(childno), prefixtreeFringeSubstnode, verbose);
                        if (dr == null)
                        {
                            return null;
                        }
                        coverednodes.putAll(dr);
                        childno++;
                        if (tree.isHeadChild(child))
                        {// && tree.getNodeType(child)!=TagNodeType.foot){
                            headchildseen = true;
                        }
                    }
                }
                //substnode in shadow tree but not in filler tree. Possibly already filled in prefix tree.
                else if (shadowtreelowerNodeindex == (byte) -1)
                {
                    if (prefixtreeFringeSubstnode == null)
                    {
                        return null;
                    }
                    else if (prefixtreeFringeSubstnode.getCategory().equals(shadowtree.getTreeOrigIndex().getCategory(shadowNode)))
                    {
                        return coverednodes;
                    }
                }
                // assume that something was adjoined here. 
                else
                {
                    ArrayList<Integer> descendents = shadowtree.getTreeOrigIndex().getDescendentNodes(shadowNode);
                    descendents.remove(0);
                    boolean endfound = false;
                    for (Integer shnode : descendents)
                    {
                        if (//shadowtree.getLowerIndex(shnode)!=null && 
                                shadowtree.getUpperIndex(shnode) == shadowtree.getLowerIndex(shadowNode) &&//)&&
                                StringTreeAnalysis.matches(pattern, shadowtree.getLowerIndex(shnode)))
                        {
                            if (verbose)
                            {
                                Utils.log(tree + "," + node + "," + shadowtree + "," + shnode);
                            }
                            Map<Integer, Integer> dr = getCoveredNodes(tree, node, shadowtree, shnode,
//                            HashMap<Integer, Integer> dr = getCoveredNodes(tree, node, shadowtree, Integer.toString(shnode),
                                    prefixtreeFringeSubstnode, verbose);
                            if (dr == null)
                            {
                                return null;
                            }
                            coverednodes.putAll(dr);
                            endfound = true;
                            break;
                        }
                    }
                    if (!endfound)
                    {
                        return null;
                    }
                }
            }
            coverednodes.put(shadowNode, node);
//            coverednodes.put(shadowNode, Integer.parseInt(node));
            return coverednodes;
        }
        else
        {
            return null;
        }
    }
    
    /**
     * 
     * Compute look-ahead probability (proxy to Brian Roark's LAP score, from CL 2004 paper).
     * We currently deal only with prediction trees only (substitution is more costly).
     * The process is the following:<br/>
     * 1. For each prediction tree that just got integrated,<br/>
     * 2. we find all the elementary trees that can verify it in the context of the current prefix tree 
     * (i.e., subject to incrementality restrictions)</br>
     * 2a. optionally filter out trees based on POS tag of upcoming word (same knowledge used for super-tagging
     * in order to filter out prediction tree candidates)</br>
     * 3. return the sum log score of trees
     * 
     * @param shadowTreeElem
     * @param shadowTreeState
     * @param chartEntry
     * @return the sum log score of verification trees for the incoming prediction tree
     */
    private double computeLAP(ElementaryStringTree shadowTreeElem)
    {
        return model.getVerificationLookAheadProbability() != null ? model.getVerificationLookAheadProbability().getVlap(shadowTreeElem) : 0.0;
    }
    
    private void addDependencies(List<StringTreeAnalysis> analyses, ChartEntry chartEntry)
    {
        for(StringTreeAnalysis analysis : analyses)
            analysis.setDependencies( ((DepTreeState)chartEntry.getTreeState()).getDependencies());
    }

    private String[] convertAnalysesToTrees(List<StringTreeAnalysis> analyses)
    {
        String[] out = new String[analyses.size()];
        int i = 0;
        for(StringTreeAnalysis analysis : analyses)
            out[i++] = Utils.normaliseTree(convertStringTree(analysis));        
        return out;
    }
    
    private void updateChartEntriesDiscriminative(Chart chart, ArrayList<ChartEntry> newEntries, short chartIndex, boolean endOfSent, int timestamp)
    {
        for(ChartEntry ce : newEntries)
        {
            BuildBlock bb = ce.getBuildBlocks().get(0); // should be only one
            if (ce.getBuildBlocks().size() > 1)
            {
                System.out.println("Unexpected case, more than 1 bb in ce.");
            }
            FringeAndProb fap = ce.getTreeState().getFutureFringe();
            ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = getEmptyBBList(chart, ce); 
            double prefixProb = fap.getBestProb();// + bb.getProbability();
            // create backpointers on buildblocks to be exploited in the feature extraction process
            chart.setTime(System.currentTimeMillis());
            try
            {
                chart.getAnalyses(ce, emptyBBlist, "", true, prefixProb, false); 
            }
            catch(Exception e)
            {
//                if(!opts.silentMode)
                    LogInfo.error("Could not extract any analyses hence features for word " + timestamp + " of Example " + ex.getName());
            }
//                                        ce.updateProbs(ce.getTreeState().getFutureFringe());
            DiscriminativeParams params = model.getDiscriminativeParams();
            IncrementalAnalysis analysis = model.getIncrAnalysesFeatures().extractFeaturesFromChartEntry(
                    chart, ce, prefixProb, endOfSent, chartIndex, false);
//                                        int sentenceLength = ex.getNumOfWords();                                        
            double modelScoreNoBaseline = analysis == null ? 0 : params.getModelWeight(OfflineReranker.computeFeatureCounts(
                    model.getDiscriminativeParams(), analysis, timestamp, false, 
                    opts.useWordBaselineFeature,
                    opts.useSyntacticFeatures, opts.useFringeFeatures, opts.useGlobalSyntacticFeatures, opts.useSemanticsFeatures, 
                    opts.semanticsType, opts.useSrlFeatures, opts.useSrlPosFeatures, opts.useLemmatisedSrlFeatures, false)) * 
                    opts.offsetModelScore;
            double baselineWeight = opts.usePrefixBaselineFeature ? params.getBaselineWeight() * opts.offsetBaselineScore : 0; 
            if(!opts.usePrefixBaselineFeature)
                modelScoreNoBaseline += bb.getPrevChartEntry().getnBestScores() != null ? bb.getPrevChartEntry().getBestScore() : 0;
            ce.updateProbsWithModelScore(fap, modelScoreNoBaseline, baselineWeight, timestamp, opts.pruneUsingScores, opts.usePrefixBaselineFeature);            
        }        
    }
    
    protected void printOutput(final PrintWriter writer, String out)
    {
        model.printOutput(writer, out);
    }
    
    protected void printOutput(final PrintWriter writer, String out, String name, boolean append, OutputType type)
    {
        if(opts.serverMode)
        {
            switch(type)
            {
                case ERROR : model.printOutput(writer, Utils.encodeToJson(new JsonResult(OutputType.ERROR, out)), jsonFilename); break;
                case FULL_TREE : model.printOutput(writer, Utils.encodeToJson(new JsonResult(OutputType.FULL_TREE, out)), jsonFilename); break;                
                case FULL_SRL : model.printOutput(writer, Utils.encodeToJson(new JsonResult(OutputType.FULL_SRL, out)), jsonFilename); break;                
            }
        }
        else if(type == OutputType.ISRL)
            model.printDependencies(writer, out, name, append);
        else
            model.printOutput(writer, out, name, append);
    }
    
    protected void printOutput(final PrintWriter writer, int timestamp, String prefixTree, String name, boolean append, OutputType type)
    {
        if(opts.serverMode)
            model.printOutput(writer, Utils.encodeToJson(new JsonResult(type, timestamp, prefixTree)), jsonFilename);
        else if(type == OutputType.ISRL)
            model.printDependencies(writer, prefixTree, name, append);
        else
            model.printOutput(writer, prefixTree, name, append);
    }
    
    protected void printOutput(final PrintWriter writer, int timestamp, String word, String posTag, String difficultyScores, String name, boolean append)
    {
        if(opts.serverMode)
            model.printOutput(writer, Utils.encodeToJson(new JsonResult(timestamp, word, posTag, difficultyScores)), jsonFilename);
        else
            model.printOutput(writer, difficultyScores, name, append);
    }
    
    
    @Override
    public int compareTo(ParsingTask o)
    {
        return ex.getNumOfWords() - o.ex.getNumOfWords();
    }
}
