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
import pltag.parser.semantics.SemanticLexicon;
import fig.basic.FullStatFig;
import fig.basic.IOUtils;
import fig.basic.Indexer;
import fig.basic.LogInfo;
import fig.basic.Pair;
import fig.basic.StopWatch;
import fig.exec.Execution;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import pltag.corpus.ElementaryStringTree;
import pltag.corpus.PltagExample;
import pltag.parser.ParsingTask.OutputType;
import pltag.parser.json.JsonInputWrapper;
import pltag.parser.json.JsonResult;
import pltag.parser.params.PltagParams;
import pltag.parser.params.Vec;
import pltag.parser.params.VecFactory;
import pltag.parser.performance.BracketPerformance;
import pltag.parser.performance.BracketPerformanceOracle;
import pltag.parser.performance.EmptyPerformance;
import pltag.parser.performance.IncrementalBracketPerformance;
import pltag.parser.performance.Performance;
import pltag.parser.performance.TrainPerformance;
import pltag.parser.semantics.IncrementalSemanticsPerformance;
import pltag.parser.semantics.SemanticsOraclePerformance;
import pltag.parser.semantics.SemanticsPerformance;
import pltag.parser.semantics.classifier.ArgumentClassifier;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.Proposition;
import pltag.parser.semantics.discriminative.DiscriminativeParams;
import pltag.parser.semantics.discriminative.ExtractFeatures;
import pltag.parser.semantics.discriminative.OfflineReranker;
import static pltag.parser.semantics.discriminative.OfflineReranker.createSemanticsAllLookupTable;
import static pltag.parser.semantics.discriminative.OfflineReranker.createSemanticsDMLookupTable;
import static pltag.parser.semantics.discriminative.OfflineReranker.createSemanticsLookupTable;
import static pltag.parser.semantics.discriminative.OfflineReranker.createSemanticsNNLookupTable;
import static pltag.parser.semantics.discriminative.OfflineReranker.semanticsSrlLookuptable;
import static pltag.parser.semantics.discriminative.OfflineReranker.semanticsVectorsSrlLookuptable;
import pltag.parser.semantics.maltparser.MaltParserWrapper;
import pltag.util.HistMap;
import pltag.util.PosTagger;
import pltag.util.Utils;

public class ParserModel extends Model
{    
    //public static final boolean combineNNVBcats = true;//false;//
    public static boolean useLeftMost = true;//false;// this only refers to the probability model, for solving adjunction ambiguity.
    public static boolean useClusterCode = true;// also only for probability model adjunction level ambiguity
    public static boolean useTimeOut = true;//false;// one big problem is that sometimes, all analyses have to be calculated because the calculateMostProbableTree
    //probability one is not a valid coherent analysis (we should discuss this, it is the most problematic aspect of the current parser implementation)
//    private static boolean pruneStrategyIsProp = true;// false;// switch between beam strategies (fixed beam width vs. beam relative to most probable analysis)
    public static boolean verbose = false; //true;// gives some more detailed output of what happens during parsing    
    private Lexicon lexicon, predLexicon;    // predLexicon is only instantiated in the oracleAllRoles semantics model
    private Map<ElementaryStringTree, ArrayList<Fringe>> shadowTreesMap;    
    private SuperTagger superTagger;        
    private final Indexer<String> roleIndexer, conllRoleIndexer, wordIndexer;    
    private Map<Integer, String> conllPredMap, conllGoldMap;    
    private Map<String, HistMap<String>> depsArgsFrequencies;
    private Map<Integer, Integer> testRoleFreqs, trainRoleFreqs;
    private ArgumentClassifier argumentClassifier;
    private MaltParserWrapper maltParser;
    private Map<String, Chart> incrAnalyses;
    private ExtractFeatures incrAnalysesFeatures;
    private static int incrAnalysesFileNo = 0;
    private DiscriminativeParams discriminativeParams;
    private VerificationLookAheadProbability verificationLookAheadProbability;
    
    public ParserModel(Options opts)
    {
        super(opts); 
        verbose = opts.verbose; // TODO: FIX Need to deprecate
        roleIndexer = new Indexer<String>();
        conllRoleIndexer = new Indexer<String>();
        wordIndexer = new Indexer<String>();
        testRoleFreqs = new HashMap<Integer, Integer>();
        trainRoleFreqs = new HashMap<Integer, Integer>();
    }

    @Override
    public void init()
    {                
        super.init();
        if(opts.fullLex)
        {
            Utils.beginTrack("Reading lexicons....");
            lexicon = newLexicon(opts.lexicon);
            if (!opts.train && opts.fullLex)
            {
//    		lexicon.lexiconReduce();
                lexicon.postProcessLexicon(false);
//                lexicon.removeHelps(); // delegated to postProcessLexicon
            }            
            Map[] predMap = readShadowTrees(newLexicon(opts.predLexicon));
            shadowTreesMap = predMap[0];            
            superTagger = opts.train ? null : new SuperTagger(params.getFreqMapStruct(), params.getFreqMapFringe(), 
                    opts.estimateInterpolationFactors, predMap[1], predMap[2], predMap[3]);
            Utils.logs("Read %s lexical entries and %s tree templates.", lexicon.getLexSize(), Lexicon.getNumOfTreeTemps());
            LogInfo.end_track();
            verificationLookAheadProbability = new VerificationLookAheadProbability(opts);
            verificationLookAheadProbability.load();
        }   
        if(opts.useSemantics)
        {
            if(opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles)
            {
                Utils.beginTrack("Reading lexicons....");
                lexicon = newLexicon(opts.lexicon);
                predLexicon = newLexicon(opts.predLexicon);
                Map[] predMap = readShadowTrees(predLexicon);
                shadowTreesMap = predMap[0]; 
                LogInfo.end_track();
            }                                    
            argumentClassifier = new ArgumentClassifier(opts);
            if(!opts.trainClassifiers)
            {
                if(opts.useClassifiers)
                {
                    Utils.beginTrack("Loading argument classifier model and feature indexers...");
                    argumentClassifier.loadModels();
                    LogInfo.end_track();
                }
                else if(opts.extractSrlFeatures) // load feature indexers only (useful for extracting vectors for the test set, using the indexers of the training set
                {
                    Utils.beginTrack("Loading feature indexers...");
                    try
                    {
                        argumentClassifier.loadFeatureIndexers();
                    }                    
                    catch(IOException ioe)
                    {
                        LogInfo.error("Error loading feature indexers");
                    }
                    LogInfo.end_track();
                }                
            }
            if(opts.baselineSemanticsModel == Options.BaselineSemanticsModel.maltParser && opts.maltParserModel != null)
            {
                Utils.beginTrack("Loading MaltParser model %s", opts.maltParserModel);
                maltParser = new MaltParserWrapper(opts.maltParserModel);
                LogInfo.end_track();
            }
        }   
        if(opts.saveIncrAnalyses)
        {
            boolean isOracle = opts.semanticsModel == Options.SemanticsModelType.oracle || 
                    opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;
            Utils.logs("Will save " + (isOracle ? "oracle" : "") + " incremental analyses...");
            incrAnalyses = Collections.synchronizedMap(new HashMap<String, Chart>()); 
        }
        if(opts.saveIncrAnalysesFeatures)
        {
            boolean isOracle = opts.semanticsModel == Options.SemanticsModelType.oracle || 
                    opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;
            Utils.logs("Will save " + (isOracle ? "oracle" : "") + " incremental analyses features...");   
            // Determine first features' object filename, in case we restart an experiment from a different point other than the start of the dataset
            if(opts.startExample > 0)
            {
                incrAnalysesFileNo = opts.startExample / opts.maxNumOfSentencesIncrAnalyses;
            }
        }
        if(opts.saveIncrAnalysesFeatures || opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle)
        {
            incrAnalysesFeatures = new ExtractFeatures(opts);
        }
        if (opts.estimateInterpolationFactors)
        {
            Utils.beginTrack("Calculating Interpolation Factors...");
            TreeProbElement.estimateInterpol(params.getFreqMapTree());
            WordProbElement.estimateInterpol(this, params.getFreqMapWord());
            LogInfo.end_track();
        }
        else
        {
            TreeProbElement.setInterpol(0.7763688883462904, 0.08928869439967384, 0.04901195421712926, 0.017292341259129748, 0.06091576941902258, 0.007122352358754198, 0.0);
            //TreeProbElement.setInterpol(0.7825862460980508, 0.08073940747494184, 0.05068305812014252, 0.017978927514188142, 0.06089000843392251, 0.007122352358754198);
            //WordProbElement.setInterpol(0.0, 0.0,0.44791363070709063, 0.42652516714812645,0.11467792972666002, 0.0032147101552357773);
            WordProbElement.setInterpol(0.6376685622628872, 0.0, 0.14791363070709063, 0.12652516714812645, 0.08467792972666002, 0.0032147101552357773);
        }
    }
    
    @Override
    public void newParams()
    {
        params = new PltagParams(opts);
    }
    
    public void stagedInitParams()
    {
        Utils.beginTrack("stagedInitParams");
        try
        {
            Utils.log("Loading discriminative parameters from " + opts.stagedParamsFile);
            ObjectInputStream ois = IOUtils.openObjIn(opts.stagedParamsFile);                
            discriminativeParams = newDiscriminativeParams();
            discriminativeParams.setVecs((Map<String, Vec>) ois.readObject());      
            System.out.println("Params Stats: \n" + Arrays.toString(discriminativeParams.getWeightsStats()));
//            System.out.println(discriminativeParams.outputNonZero(ParamsType.COUNTS));
            ois.close();
            
        }
        catch(Exception ioe)
        {
            Utils.log("Error loading "+ opts.stagedParamsFile);            
            ioe.printStackTrace(LogInfo.stderr);
            Execution.finish();
        }
        LogInfo.end_track();
    }
    
    protected DiscriminativeParams newDiscriminativeParams()
    {
        return new DiscriminativeParams(opts, incrAnalysesFeatures.getFeatureIndexers(), VecFactory.Type.DENSE);
    }
    
    @Override
    protected Performance newPerformance()
    {
        if(opts.train)
        {
            return new TrainPerformance();
        }
        else if (opts.interactiveMode)
        {
            return new EmptyPerformance();
        }
        else if(opts.useSemantics && !opts.estimateProcDifficulty)
        {
            if(opts.parserType == Options.ParserType.oracle || opts.parserType == Options.ParserType.discriminativeOracle)
            {
                return new SemanticsOraclePerformance(conllRoleIndexer, testRoleFreqs);
            }
            return opts.evaluateIncrementalDependencies ? new IncrementalSemanticsPerformance()
                    : new SemanticsPerformance(conllRoleIndexer, testRoleFreqs);
        }
        else if(opts.parserType == Options.ParserType.oracle)
        {
            return new BracketPerformanceOracle();
        }        
        else if(opts.evaluateIncrementalEvalb)
        {
            return new IncrementalBracketPerformance();
        }
        else
        {
            return new BracketPerformance();
        }
    }
    
    protected Lexicon newLexicon(String filename)
    {
        Lexicon lex = opts.useSemantics ? new SemanticLexicon(opts, listOfFreqWords, roleIndexer) : new Lexicon(opts, listOfFreqWords);        
        lex.processLexicon(filename);
        
        return lex;
    }    
    
    protected Lexicon newLexicon(String[] entries)
    {
        Lexicon lex = opts.useSemantics ? new SemanticLexicon(opts, listOfFreqWords, roleIndexer) : new Lexicon(opts, listOfFreqWords);        
        lex.processLexicon(entries);
        
        return lex;
    }
    
    protected Lexicon newLexicon(String[] entries, Lexicon lexiconWithAllRoles)
    {
        Lexicon lex = opts.useSemantics ? new SemanticLexicon(opts, listOfFreqWords, roleIndexer, (SemanticLexicon)lexiconWithAllRoles) : new Lexicon(opts, listOfFreqWords);        
        lex.processLexicon(entries);
        
        return lex;
    }    
    
    @Override
    public void readExamples(PltagExample example)
    {
        Example ex = readSingleExample(example);
        if(ex == null)
            return;
        if((opts.exactWordLength != Integer.MAX_VALUE&& ex.getNumOfWords() == opts.exactWordLength) || 
                (opts.exactWordLength == Integer.MAX_VALUE && ex.getNumOfWords() <= opts.maxWordLength))
        {
            Utils.beginTrack("(%s examples so far)", ++numExamples);
            if(useExamplesOnlyList == null || useExamplesOnlyList.contains(ex.getName()))
                examples.add(ex);
            LogInfo.end_track();
        }         
    } 
    
    public Example readSingleExample(PltagExample example)
    {
        if(!example.isParsed())
            return null;
        Lexicon lexiconEx;
        Map<ElementaryStringTree, ArrayList<Fringe>> shadowTreesMapEx;
        SuperTagger superTaggerEx;
        
        if(opts.fullLex) // use full lexicon and predicted lexicon (usually for testing)
        {
            lexiconEx = lexicon;
            shadowTreesMapEx = shadowTreesMap;
            superTaggerEx = superTagger;
        }
        else // usually during training, we will use the lexicon extracted per example
        {
            if(opts.examplesInSingleFile) // the lexicon is included in the file
            {
                lexiconEx = new Lexicon(opts, listOfFreqWords, example.getLexiconArray());
                Map[] predMap = readShadowTrees(new Lexicon(opts, listOfFreqWords, example.getPredLexiconArray()));
                shadowTreesMapEx = predMap[0];
                superTaggerEx = opts.train ? null : new SuperTagger(params.getFreqMapStruct(), params.getFreqMapFringe(), 
                    opts.estimateInterpolationFactors, predMap[1], predMap[2], predMap[3]);
            }
            else // we need to read and process the lexicon files online (at the moment using external perl scripts)
            {
               List<String[]> lexica = readFromPartialLexiconFile(example.getName());
               lexiconEx = new Lexicon(opts, listOfFreqWords, lexica.get(0));
               Map[] predMap = readShadowTrees(new Lexicon(opts, listOfFreqWords, lexica.get(1)));
               shadowTreesMapEx = predMap[0];
               superTaggerEx = opts.train ? null : new SuperTagger(params.getFreqMapStruct(), params.getFreqMapFringe(), 
                    opts.estimateInterpolationFactors, predMap[1], predMap[2], predMap[3]);
            }
        }
        FreqCounter freqCounterEx = null;
        if(opts.train) // use a separate frequency counter for storing parameter counts (TODO: FIX when we introduce Vecs)
        {
            freqCounterEx = new FreqCounter(opts);
        }
        return new Example(example.getName(), example.getGoldStandardArray(), lexiconEx, shadowTreesMapEx, superTaggerEx, freqCounterEx, opts);
    }
    
    /**
     * Read examples from two sources, i.e, our proprietary pltag format and CoNLL 2009 format.
     * We will either use the full lexicon (normal testing) or the lexicon extracted per
     * example (oracle testing).
     * @param pltagExample
     * @param conllExample 
     */
    @Override
    public void readExamples(PltagExample pltagExample, PltagExample conllExample)
    {
        Lexicon lexiconEx;
        Map<ElementaryStringTree, ArrayList<Fringe>> shadowTreesMapEx;
        SuperTagger superTaggerEx = null;
        
        if(opts.fullLex) // use full lexicon and predicted lexicon (usually for testing)
        {
            lexiconEx = lexicon;
            shadowTreesMapEx = shadowTreesMap;
            superTaggerEx = superTagger;
        }
        else // for oracle testing, we will use the lexicon extracted per example
        {
            if(opts.examplesInSingleFile) // the lexicon is included in the file
            {
                if(!pltagExample.getGoldStandardArray()[0].equals("NOT PARSED"))
                {
                    lexiconEx = opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles ? 
                            newLexicon(pltagExample.getLexiconArray(), lexicon) : newLexicon(pltagExample.getLexiconArray());
                    lexiconEx.postProcessLexicon(false);
                    // TODO: fix oracleAllRoles case to propagate roles from fullLexicon
                    Map[] predMap = readShadowTrees(opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles ? 
                            newLexicon(pltagExample.getPredLexiconArray(), predLexicon) : newLexicon(pltagExample.getPredLexiconArray()));
                    shadowTreesMapEx = predMap[0];
                    superTaggerEx = opts.train ? null : new SuperTagger(params.getFreqMapStruct(), params.getFreqMapFringe(), 
                        opts.estimateInterpolationFactors, predMap[1], predMap[2], predMap[3]);
                }
                else
                {
                    lexiconEx = null; shadowTreesMapEx = null; superTaggerEx = null;
                }
            }
            else // we need to read and process the lexicon files online (at the moment using external perl scripts)
            {
               List<String[]> lexica = readFromPartialLexiconFile(pltagExample.getName());
               lexiconEx = new Lexicon(opts, listOfFreqWords, lexica.get(0));
               Map[] predMap = readShadowTrees(new Lexicon(opts, listOfFreqWords, lexica.get(1)));
               shadowTreesMapEx = predMap[0];
               superTaggerEx = opts.train ? null : new SuperTagger(params.getFreqMapStruct(), params.getFreqMapFringe(), 
                        opts.estimateInterpolationFactors, predMap[1], predMap[2], predMap[3]);
            }
        }        
        Example ex = new ConllExample(pltagExample.getName(), pltagExample.getGoldStandardArray(), conllExample.getGoldStandard(), 
                lexiconEx, shadowTreesMapEx, superTaggerEx, null, opts, conllRoleIndexer);
        if((opts.exactWordLength != Integer.MAX_VALUE&& ex.getNumOfWords() == opts.exactWordLength) || 
                (opts.exactWordLength == Integer.MAX_VALUE && ex.getNumOfWords() <= opts.maxWordLength))
        {
            if(opts.useSemantics && opts.inputPaths.size() > 1) // reading from conll gold file
            {
                Utils.beginTrack("(%s examples and %s propositions so far)", ++numExamples, 
                    totalNumOfGoldPropositions += ((ConllExample)ex).getVerbPropositionsMap().size());                
            }
            else
            {
                Utils.beginTrack("(%s examples so far)", ++numExamples);
            }
            if(useExamplesOnlyList == null || useExamplesOnlyList.contains(ex.getName()))
                examples.add(ex);
            LogInfo.end_track();
        }   
    } 
    
    private List<String[]> readFromPartialLexiconFile(String filename)
    {
        List lexica = new ArrayList<String[]>();
        String cmd = String.format("resources/extractLexicon.sh %s", filename);
        String cmdArray[] = {"/bin/sh", "-c", cmd};
        lexica.add(Utils.executeCmdToArray(cmdArray));
        cmd = String.format("resources/extractPredLexicon.sh %s", filename);
        cmdArray[2] = cmd;
        lexica.add(Utils.executeCmdToArray(cmdArray));
        
        return lexica;
    }       
        
//    private Map<ElementaryStringTree, ArrayList<Fringe>> readShadowTrees(Lexicon predLex)           
    private Map[] readShadowTrees(Lexicon predLex)           
    {
        HashMap<ElementaryStringTree, String> predTreeFringeMap = new HashMap<ElementaryStringTree, String>();
        HashMap<ElementaryStringTree, String> predTreeStructMap = new HashMap<ElementaryStringTree, String>();
        HashMap<ElementaryStringTree, String> predTreeMainLeafMap = new HashMap<ElementaryStringTree, String>();
        Map<ElementaryStringTree, ArrayList<Fringe>> map = new HashMap<ElementaryStringTree, ArrayList<Fringe>>();
        for (ElementaryStringTree t : predLex.getEntries("prediction: ", "prediction: ", "", false, 0))
        {
            ArrayList<Fringe> fringeList = new ArrayList<Fringe>();
            short[] nullarray = new short[2];
            nullarray[0] = -99;
            nullarray[1] = -99;
            HashMap<Short, Node> nodeKernelMap = new HashMap<Short, Node>();
            Fringe f = TreeState.getAuxFringe(t, -1, nullarray, nodeKernelMap);
            predTreeFringeMap.put(t, f.toCatString());
            predTreeStructMap.put(t, t.getUnlexStruct(t.getRoot()));
            predTreeMainLeafMap.put(t, t.getMainLeaf(t.getRoot()));
            fringeList.add(f);
            fringeList.addAll(TreeState.calculateUnaccessibles(t, f.getCurrentLeafNumber(), nullarray, nodeKernelMap));
            map.put(t, fringeList);
            t.setTreeString(t.getUnlexStruct(t.getRoot()));            
        }        
        return new Map[] {map, predTreeFringeMap, predTreeStructMap, predTreeMainLeafMap};
    }
    
    @Override
    public void train()
    {
        try
        {
            trainPredOut = IOUtils.openOut(Execution.getFile("train.log"));
        }
        catch(Exception ioe)
        {
            Utils.logs("Error opening file(s) for writing. No output will be written!");            
        }
        FullStatFig complexity = new FullStatFig();
        performance = newPerformance();
        Collection<ParsingTask> list = new ArrayList(examples.size());
        for(int i = 0; i < examples.size(); i++)
        {                                 
            list.add(new ParsingTask(opts, this, examples.get(i), i, complexity, performance));
        }
        Utils.parallelForeach(opts.numThreads, list);
        LogInfo.end_track();
        list.clear();
        if(opts.outputParams)
        {            
            writeParamsObj("final");
            if(opts.outputParamsTxt)
                writeParams("final");
        }        
        record("train", complexity, true);
    }
       
    @Override
    public void parse()
    {
        try
        {
            testFullPredOut = opts.outputFullPred ? IOUtils.openOut(Execution.getFile("test.full-pred-gen")) : null;            
            testIncrDependenciesOut = opts.outputCompletedIncrementalDependencies ? IOUtils.openOut(Execution.getFile("test.completed-dependencies")) : null;            
        }
        catch(Exception ioe)
        {
            Utils.logs("Error opening file(s) for writing. No output will be written!");            
        }
        FullStatFig complexity = new FullStatFig();        
        if(opts.useSemantics)
        {
            if(goldConllPresent)
            {
                conllPredMap = new TreeMap<Integer, String>();
                conllGoldMap = new TreeMap<Integer, String>();
                testRoleFreqs = getGoldRoleFreqs(opts.inputPaths.get(1)); // // get role frequency information from test corpus (used for conf. matrix)
                if(opts.inputPaths.size() > 2) // get role frequency information from training corpus
                    trainRoleFreqs = getGoldRoleFreqs(opts.inputPaths.get(2));
            }            
            if(opts.freqBaseline)
            {
                if(opts.inputPaths.size() == 3)
                    depsArgsFrequencies = getTopNDepsArgsStats(opts.inputPaths.get(2));
                else
                    depsArgsFrequencies = new HashMap();
            }
        }
        performance = newPerformance();
        if(opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies || 
                (opts.estimateProcDifficulty && opts.outputFullPred && opts.numThreads > 1))
        {
            incrementalExamplesOutputMap = new HashMap<String, String>();
            incrementalDependenciesOutputMap = new HashMap<String, String>();
        }
        // We are using a discriminative reranker so load feature indexers. Alternatively we are saving incremental analyses and 
        // we are extending the existing indexers, in case e.g., we are running an oracle parser after a normal parse of the corpus.
//        if(opts.parserType == Options.ParserType.discriminative && opts.discriminativeFeatureIndexers != null && 
        if((opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle) &&
                opts.discriminativeFeatureIndexers != null && 
               (new File(opts.discriminativeFeatureIndexers + ".obj.gz").exists() ||
                new File(opts.discriminativeFeatureIndexers).exists()) && 
                (!opts.extendIndexers || opts.saveIncrAnalysesFeatures))
        {
            Utils.beginTrack("Reading Feature Indexers");
            Utils.logs("Loading incremental analyses features indexers from disk...");
            incrAnalysesFeatures.loadFeatureIndexers(opts.saveIncrAnalysesFeatures, new File(opts.discriminativeFeatureIndexers).exists());
            LogInfo.end_track();            
        }
        Utils.beginTrack("Reading SRL triple lookup table and vectors");
        if(!opts.semanticsLookupTable.isEmpty() && new File(opts.semanticsLookupTable.get(0)).exists())// &&
           //opts.semanticsLookupTableVectors != null && new File(opts.semanticsLookupTableVectors).exists())
        {            
            Utils.logs("Loading semantics SRL triple lookup table...");
//            semanticsSrlLookuptable = createSemanticsLemmatizedLookupTable(opts.semanticsLookupTable);
//            semanticsSrlLookuptable = createSemanticsLookupTable(opts.semanticsLookupTable);
            semanticsSrlLookuptable = new HashMap[2];
            switch(opts.semanticsType)
            {
                case nn : semanticsSrlLookuptable[0] = createSemanticsNNLookupTable(opts.semanticsLookupTable.get(0)); break;
                case dm : semanticsSrlLookuptable[0] = createSemanticsDMLookupTable(opts.semanticsLookupTable.get(0)); break;
                case all : semanticsVectorsSrlLookuptable = new HashMap<>(); semanticsSrlLookuptable = createSemanticsAllLookupTable(opts.semanticsLookupTable, opts.semanticsLookupTableVectors, semanticsVectorsSrlLookuptable); break;
                case quantum : default: semanticsVectorsSrlLookuptable = createSemanticsLookupTable(opts.semanticsLookupTable.get(0), opts.semanticsLookupTableVectors);
            }
        }
        else
        {
            Utils.logs("SRL triple lookup table does not exist...skipping.");
        }
        LogInfo.end_track();
        if(opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle 
                && !opts.saveIncrAnalysesFeatures)
        {            
            stagedInitParams();         
        }        
        Pair<Integer, Integer> startEndExample = determineStartEndExample(examples.size());
        int start = startEndExample.getFirst();
        int end = startEndExample.getSecond();
        Collection<ParsingTask> list = new ArrayList(end);
        for(int i = start; i < end; i++)
        {                                 
                list.add(new ParsingTask(opts, this, examples.get(i), i, complexity, performance));
        }
        Utils.parallelForeach(opts.numThreads, list);
        LogInfo.end_track();
        list.clear();
        if(opts.trainClassifiers)
        {
            argumentClassifier.trainModels();
            argumentClassifier.saveModels();
        }
        if(opts.extractSrlFeatures)
        {
            argumentClassifier.saveInstancesToFiles(opts.argumentIdentifierFeatureVectorsFile, opts.argumentLabellerFeatureVectorsFile);
        }
        if(opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies || 
                (opts.estimateProcDifficulty && opts.outputFullPred && opts.numThreads > 1))
        {
            for(Example ex : examples)
            {
                printOutput(testFullPredOut, incrementalExamplesOutputMap.get(ex.getName()));
                if(testIncrDependenciesOut != null)
                    printOutput(testIncrDependenciesOut, incrementalDependenciesOutputMap.get(ex.getName()));
            }
        }
        if(testFullPredOut != null) 
        {
            testFullPredOut.close();
        }
        if(opts.useSemantics && goldConllPresent) 
        {
            writeConllOutput();
        }        
        // save incremental analyses to disk. In case we are using the full parser, just write the final batch of sentences.
        if(opts.saveIncrAnalyses && incrAnalyses != null && !incrAnalyses.isEmpty()) 
        {
            writeIncrAnalyses();
        }
        // save incremental analyses features to disk. In case we are using the full parser, just write the final batch of sentences.        
        if(opts.saveIncrAnalysesFeatures && incrAnalysesFeatures != null) 
        {
            if(!incrAnalysesFeatures.isEmptySentenceLevel()) // write any remaining analyses to disk
            {
                Utils.logs("Saving remaining analyses to disk...");
                writeIncrAnalysesFeatures();
            }
            Utils.logs("Saving incremental analyses features indexers to disk...");            
            incrAnalysesFeatures.saveFeatureIndexers(opts.extendIndexers);
        }
        record("results", complexity, true);
        if(opts.evaluateIncrementalDependencies && !(performance == null || performance.isEmpty()))
        {
            IncrementalSemanticsPerformance incrPerf = (IncrementalSemanticsPerformance) performance;
            Utils.write(Execution.getFile("results.ups"), incrPerf.incrUpsScore());
            Utils.write(Execution.getFile("results.ciss"), incrPerf.incrCissScore());
            Utils.write(Execution.getFile("results.evalb"), incrPerf.incrEvalbScore());
        }
        if(opts.evaluateIncrementalEvalb && !(performance == null || performance.isEmpty()))
        {
            IncrementalBracketPerformance incrPerf = (IncrementalBracketPerformance) performance;
            Utils.write(Execution.getFile("results.evalb"), incrPerf.incrEvalbScore());
        }
//        Utils.write(Execution.getFile("ambiguousLabels.txt"), argumentClassifier.getAmbiguousLabels());
    }       
    
    @Override
    public void interactiveParse()
    {
        opts.numThreads = 1; // it only makes sense to use one thread to parse a single sentence.
        if(opts.goldPosTags)
        {
            PosTagger.initPosTagger();
        }
        // We are using a discriminative reranker so load feature indexers.
        if((opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle) &&
                opts.discriminativeFeatureIndexers != null && 
               (new File(opts.discriminativeFeatureIndexers + ".obj.gz").exists() ||
                new File(opts.discriminativeFeatureIndexers).exists()))
        {
            Utils.beginTrack("Reading Feature Indexers");
            Utils.logs("Loading incremental analyses features indexers from disk...");
            incrAnalysesFeatures.loadFeatureIndexers(opts.saveIncrAnalysesFeatures, new File(opts.discriminativeFeatureIndexers).exists());
            LogInfo.end_track();    
        }
        if(opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle
                && !opts.saveIncrAnalysesFeatures)
        {            
            stagedInitParams();         
        }
        testFullPredOut = new PrintWriter(System.out);
        testIncrDependenciesOut = new PrintWriter(System.out);
        FullStatFig complexity = new FullStatFig();
        performance = newPerformance();
        System.out.println("\n\n*********************************\n*PLTAG Parser - Interactive Mode*\n*********************************\n\nInput a sentence, and hit Enter after that. Enter q to exit.");
        Console in = System.console();
        if(in == null)
        {
            System.err.println("No console");
            System.exit(1);
        }
        String line;
        while(!(line = in.readLine("Input: ")).equals("q"))
        {
            try
            {                
                new ParsingTask(opts, this, readSingleExample(Utils.readLineExample(line, 0)), 0, complexity, performance).call();
            } 
            catch (Exception ex)
            {
                LogInfo.error(" Error parsing sentence. Enter q to exit.");                
            }
        }        
    }
    
    /**
     * Process single example in JSON Format - for client-server use. 
     * The method goes through the whole pipeline: convert JSON to pltag example format,
     * creates a parsing task and returns the parsed output.
     * @param input
     * @param socketWriter
     * @return 
     */
    public void processExamplesJson(String input, PrintWriter...socketWriter)
    {
        JsonInputWrapper inputWrapper = new JsonInputWrapper(input);
        String tmpFile = inputWrapper.getTmpFile();
        if(socketWriter.length > 0)
            testFullPredOut = socketWriter[0];
        else
        {
            try
            {
                testFullPredOut = new PrintWriter(tmpFile);
            }
            catch(FileNotFoundException ioe)
            {
                LogInfo.error("Error opening temporary file: " + tmpFile); 
            } catch (Exception ex)
            {
                LogInfo.error("Error with temporary file: " + tmpFile); 
            }
        }
        FullStatFig complexity = new FullStatFig();
        performance = new EmptyPerformance();
        opts.beamEntry = inputWrapper.getBeamSize();
        StopWatch watch = new StopWatch();        
        try
        {                
            watch.start();
            new ParsingTask(opts, this, readSingleExample(Utils.readLineExample(inputWrapper.getSentence(), 0)), 0, complexity, performance, tmpFile).call();
            watch.stop();
            printOutput(testFullPredOut, Utils.encodeToJson(new JsonResult(OutputType.SUCCESS, "Finished parsing in " + watch.toString())));            
            
        } 
        catch (Exception ex)
        {            
            LogInfo.error("Error parsing sentence: " + inputWrapper.getSentence()); 
            printOutput(testFullPredOut, Utils.encodeToJson(JsonInputWrapper.ERROR_PARSING));
        } 
        JsonResult.COUNTER = 1;
        testFullPredOut.close();
    }        
    
    private Map<String, HistMap<String>> getTopNDepsArgsStats(String conllFile)
    {
        LogInfo.logs("Creating deps - args statistics from " + conllFile);
        // Map of DepRels to histograms of Arguments (with their relative frequencies)
        Map<String, HistMap<String>> histMap = new HashMap();
        HistMap<String> depRelsHist = new HistMap<String>();
        List<PltagExample> conllExamples = Utils.readConllExamples(conllFile);
        for(PltagExample ex : conllExamples)
        {
            try
            {
                ConllExample conllEx = new ConllExample(conllFile, ex.getGoldStandard(), opts, roleIndexer);
                for(Proposition prop : conllEx.getVerbPropositions())
                {
                    for(Argument arg : prop.getArguments())
                    {
                        String depRel = arg.getDepRel();
                        HistMap<String> hist = histMap.get(depRel);
                        if(hist == null)
                        {
                            hist = new HistMap<String>();
                            histMap.put(depRel, hist);
                        }
                        hist.add(arg.getRole());
                        depRelsHist.add(depRel);
                    }
                }
            } catch(Exception e)
            {
                LogInfo.error(e + "\n" + ex);
            }
        }
        Map<String, HistMap<String>> outMap = new HashMap();
        List<String> depRelsSorted = depRelsHist.getKeysSorted();
        for(int i = 0; i < opts.srlBaselineTopNDepRels; i++)
        {
            String depRel = depRelsSorted.get(i);
            HistMap<String> hist = histMap.get(depRel);
            outMap.put(depRel, hist);
        }
        return outMap;
    }        
    
    @Override
    public void testTrain()
    {        
        trainPredOut = new PrintWriter(System.out);  
        FullStatFig complexity = new FullStatFig();
        performance = newPerformance();
        for(int i = 0; i < examples.size(); i++)
        {                                 
            try
            {
                new ParsingTask(opts, this, examples.get(i), i, complexity, performance).call();
            } 
            catch (Exception ex)
            {
                LogInfo.error(ex);
            }
        } 
        record("results", complexity, false);
        System.out.println(performance.output());
        if(opts.outputParams)
        {            
            writeParamsObj("final");
            if(opts.outputParamsTxt)
                writeParams("final");
        }
    }
         
    @Override
    public void testParse()
    {
        testFullPredOut = (opts.outputFullPred) ? new PrintWriter(System.out) : null;    
        FullStatFig complexity = new FullStatFig();        
        if(opts.useSemantics)
        {
            if(goldConllPresent)
            {
                conllPredMap = new TreeMap<Integer, String>();
                conllGoldMap = new TreeMap<Integer, String>();
                testRoleFreqs = getGoldRoleFreqs(opts.inputPaths.get(1)); // // get role frequency information from test corpus (used for conf. matrix)
                if(opts.inputPaths.size() > 2) // get role frequency information from training corpus
                    trainRoleFreqs = getGoldRoleFreqs(opts.inputPaths.get(2));
            }            
            if(opts.freqBaseline)
            {
                if(opts.inputPaths.size() == 3)
                    depsArgsFrequencies = getTopNDepsArgsStats(opts.inputPaths.get(2));
                else
                    depsArgsFrequencies = new HashMap();
            }
        }
        performance = newPerformance();
        if(opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies || 
                (opts.estimateProcDifficulty && opts.outputFullPred && opts.numThreads > 1))
        {
            incrementalExamplesOutputMap = new HashMap<String, String>();
            incrementalDependenciesOutputMap = new HashMap<String, String>();
        }
        // We are using a discriminative reranker so load feature indexers. Alternatively we are saving incremental analyses and 
        // we are extending the existing indexers, in case e.g., we are running an oracle parser after a normal parse of the corpus.
//       if(opts.parserType == Options.ParserType.discriminative && opts.discriminativeFeatureIndexers != null && 
       if((opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle) &&
               opts.discriminativeFeatureIndexers != null && 
               (new File(opts.discriminativeFeatureIndexers + ".obj.gz").exists() ||
                new File(opts.discriminativeFeatureIndexers).exists()) &&
                (!opts.extendIndexers || opts.saveIncrAnalysesFeatures))
        {
            Utils.beginTrack("Reading Feature Indexers");
            Utils.logs("Loading incremental analyses features indexers from disk...");
            incrAnalysesFeatures.loadFeatureIndexers(opts.saveIncrAnalysesFeatures, new File(opts.discriminativeFeatureIndexers).exists());
            LogInfo.end_track();            
        }
        Utils.beginTrack("Reading SRL triple lookup table and vectors");
        if(!opts.semanticsLookupTable.isEmpty() && new File(opts.semanticsLookupTable.get(0)).exists())// &&
           //opts.semanticsLookupTableVectors != null && new File(opts.semanticsLookupTableVectors).exists())
        {            
            Utils.logs("Loading semantics SRL triple lookup table...");
//            semanticsSrlLookuptable = createSemanticsLemmatizedLookupTable(opts.semanticsLookupTable);
//            semanticsSrlLookuptable = createSemanticsLookupTable(opts.semanticsLookupTable);
            semanticsSrlLookuptable = new HashMap[2];
            switch(opts.semanticsType)
            {
                case nn : semanticsSrlLookuptable[0] = createSemanticsNNLookupTable(opts.semanticsLookupTable.get(0)); break;
                case dm : semanticsSrlLookuptable[0] = createSemanticsDMLookupTable(opts.semanticsLookupTable.get(0)); break;
                case all : semanticsVectorsSrlLookuptable = new HashMap<>(); semanticsSrlLookuptable = createSemanticsAllLookupTable(opts.semanticsLookupTable, opts.semanticsLookupTableVectors, semanticsVectorsSrlLookuptable); break;
                case quantum : default: semanticsVectorsSrlLookuptable = createSemanticsLookupTable(opts.semanticsLookupTable.get(0), opts.semanticsLookupTableVectors);
            }
        }
        else
        {
            Utils.logs("SRL triple lookup table does not exist...skipping.");
        }
        LogInfo.end_track();
        if(opts.parserType == Options.ParserType.discriminative || opts.parserType == Options.ParserType.discriminativeOracle
                && !opts.saveIncrAnalysesFeatures)
        {            
            stagedInitParams();         
        }
        List<ParsingTask> list = new ArrayList();
        for(int i = 0; i < examples.size(); i++)
//        for(int i = 1387; i < examples.size(); i++)
        {                                 
//            if(examples.get(i).getName().equals("Example_23/wsj_2303.mrg-sent_2"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2330.mrg-sent_43"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2340.mrg-sent_0"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2328.mrg-sent_4"))
//            if(examples.get(i).getName().equals("Example_23/wsj_2316.mrg-sent_22"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2347.mrg-sent_11"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2306.mrg-sent_14"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2303.mrg-sent_11"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2300.mrg-sent_3"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2362.mrg-sent_14"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2306.mrg-sent_41"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2309.mrg-sent_7"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2399.mrg-sent_22"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2330.mrg-sent_6"))// || examples.get(i).getName().equals("Example:23/wsj_2321.mrg-sent_19"))
//            if(examples.get(i).getName().equals("Example:23/wsj_2300.mrg-sent_11"))
            if(!examples.get(i).getName().equals("Example:23/wsj_2300.mrg-sent_3"))
            {
                try
                {                
                    new ParsingTask(opts, this, examples.get(i), i, complexity, performance).call();
//                    list.add(new ParsingTask(opts, this, examples.get(i), i, complexity, performance));
                } 
                catch (Exception ex)
                {
                    LogInfo.error(ex + " in example " + examples.get(i).getName());
                    ex.printStackTrace();
                }
            }            
        }
//        Utils.parallelForeach(opts.numThreads, list);
        if(opts.trainClassifiers)
        {
            argumentClassifier.trainModels();
            argumentClassifier.saveModels();
        }
        if(opts.extractSrlFeatures)
        {
            argumentClassifier.saveInstancesToFiles(opts.argumentIdentifierFeatureVectorsFile, opts.argumentLabellerFeatureVectorsFile);
        }
        if(opts.outputIncrementalDependencies || opts.outputCompletedIncrementalDependencies || 
            (opts.estimateProcDifficulty && opts.outputFullPred && opts.numThreads > 1))
        {
            for(Example ex : examples)
            {
                String str = incrementalExamplesOutputMap.get(ex.getName());
                if(str != null)
                    printOutput(testFullPredOut, str);
                if(opts.outputCompletedIncrementalDependencies)
                {
                    str = incrementalDependenciesOutputMap.get(ex.getName());
                    if(str != null)
                        printOutput(testIncrDependenciesOut, str);
                }
            }
        }
        // save incremental analyses to disk. In case we are using the full parser, just write the final batch of sentences.
        if(opts.saveIncrAnalyses && incrAnalyses != null && !incrAnalyses.isEmpty()) 
        {
            writeIncrAnalyses();
        }
        // save incremental analyses features to disk. In case we are using the full parser, just write the final batch of sentences.
        if(opts.saveIncrAnalysesFeatures && incrAnalysesFeatures != null) 
        {
            if(!incrAnalysesFeatures.isEmptySentenceLevel()) // write any remaining analyses to disk
                writeIncrAnalysesFeatures();
            Utils.logs("Saving incremental analyses features indexers to disk...");
            incrAnalysesFeatures.saveFeatureIndexers(opts.extendIndexers);
        }
        record("results", complexity, false);
//        System.out.println(argumentClassifier.getAmbiguousLabels());
        System.out.println(performance.output());
        LogInfo.end_track();        
    }
    
    public void testConllRoleNames()
    {
        HistMap<String> roles = new HistMap();
        for(Example ex : examples)
        {
            ConllExample c = (ConllExample)ex;
            for(Proposition prop : c.getVerbPropositionsMap().values())
            {
                for(Argument arg : prop.getArguments())
                    roles.add(arg.getRole());
            }
        }
        System.out.println(roles);
    }
    
    public List<String[]> testReadFromPartialLexiconFile(String filename)
    {
        return readFromPartialLexiconFile(filename);
    }
       
    @Override
    protected void loadParamsFromDisk()
    {    
        LogInfo.track("Loading Params");
        // read from text files
        LogInfo.logs("Reading " + opts.treeFrequencies);
        TreeProbElement.readMap(opts.treeFrequencies, params.getFreqMapTree(), opts.combineNNVBcats);
        LogInfo.logs("Reading " + opts.wordFrequencies);
        WordProbElement.readMap(opts.wordFrequencies, params.getFreqMapWord(), opts.combineNNVBcats);
        LogInfo.logs("Reading " + opts.superTagStruct);
        SuperTagStructElement.readMap(opts.superTagStruct, params.getFreqMapStruct(), opts.combineNNVBcats);                
        LogInfo.logs("Reading " + opts.superTagFringe);
        SuperTagElement.readMap(opts.superTagFringe, params.getFreqMapFringe(), opts.combineNNVBcats);        
        
        //read from objects // TODO: FIX, need to emulate readMap from above, as it does some more calculations when reading from file
//        LogInfo.logs("Reading " + opts.treeFrequencies);
//        params.setFreqMapTree((HashMap<String, Integer>)IOUtils.readObjFileEasy(opts.treeFrequencies));       
//        TreeProbElement.calculateUniq(params.getFreqMapTree());
//        LogInfo.logs("Reading " + opts.wordFrequencies);
//        params.setFreqMapWord((HashMap<String, Integer>)IOUtils.readObjFileEasy(opts.wordFrequencies));        
//        WordProbElement.calculateUniq(params.getFreqMapWord());
//        LogInfo.logs("Reading " + opts.superTagStruct);
//        params.setFreqMapStruct((HashMap<String, Integer>)IOUtils.readObjFileEasy(opts.superTagStruct));        
//        SuperTagStructElement.calculateUniq(params.getFreqMapStruct());
//        LogInfo.logs("Reading " + opts.superTagFringe);
//        params.setFreqMapFringe((HashMap<SuperTagElement, Integer>)IOUtils.readObjFileEasy(opts.superTagFringe));
        
        LogInfo.end_track();
//        writeParams("combinednew2");
//        writeParamsObj("combinednew2");        
//        System.exit(0);
    }
    
    protected void writeConllOutput()
    {
        try
        {
            StringBuilder str = new StringBuilder();
            if(!conllPredMap.isEmpty())
            {
                conllPredOut = IOUtils.openOut(Execution.getFile("test.full-pred-gen.conll"));                
                for(Entry<Integer, String> entry : conllPredMap.entrySet())
                {
                    str.append(entry.getValue()).append("\n");
                }
                printOutput(conllPredOut, str.deleteCharAt(str.length() - 1).toString());
                conllPredOut.close();
            }   
            if(!conllGoldMap.isEmpty())
            {
                conllGoldOut = IOUtils.openOut(Execution.getFile("gold.full-pred-gen.conll"));
                str = new StringBuilder();
                for(Entry<Integer, String> entry : conllGoldMap.entrySet())
                {
                    str.append(entry.getValue()).append("\n");
                }
                printOutput(conllGoldOut, str.deleteCharAt(str.length() - 1).toString());
                conllGoldOut.close();
            }            
        }
        catch(IOException ioe)
        {
            Utils.logs("Error opening file(s) for writing. No CoNLL output will be written!");
        }
    }
    
    @Override
    protected void writeParams(String id)
    {
        Utils.writeMap(Execution.execDir + "/WordFrequencies.txt." + id, params.getFreqMapWord());
        Utils.writeMap(Execution.execDir + "/TreeFrequencies.txt." + id, params.getFreqMapTree());
        Utils.writeMap(Execution.execDir + "/SuperTagStruct.txt." + id, params.getFreqMapStruct());
        Utils.writeMap(Execution.execDir + "/SuperTagFringe.txt." + id, params.getFreqMapFringe());
    }
    
    @Override
    protected void writeParamsObj(String id)
    {
        IOUtils.writeObjFileEasy(Execution.execDir + "/WordFrequencies." + id + ".obj.gz", params.getFreqMapWord());
        IOUtils.writeObjFileEasy(Execution.execDir + "/TreeFrequencies." + id + ".obj.gz", params.getFreqMapTree());
        IOUtils.writeObjFileEasy(Execution.execDir + "/SuperTagStruct." + id + ".obj.gz", params.getFreqMapStruct());
        IOUtils.writeObjFileEasy(Execution.execDir + "/SuperTagFringe." + id + ".obj.gz", params.getFreqMapFringe());
    }    
    
    @Override
    protected void appendParams(String id)
    {       
        Utils.writeMapAppend(Execution.execDir + "/WordFrequencies.txt." + id, params.getFreqMapWord());
        Utils.writeMapAppend(Execution.execDir + "/TreeFrequencies.txt." + id, params.getFreqMapTree());
        Utils.writeMapAppend(Execution.execDir + "/SuperTagStruct.txt." + id, params.getFreqMapStruct());
        Utils.writeMapAppend(Execution.execDir + "/SuperTagFringe.txt." + id, params.getFreqMapFringe());
    }

    @Override
    protected void updateParams(FreqCounter lastOccurrences, FreqCounter newOccurences)
    {
        synchronized(params)
        {
            //update old frequencies (TODO: FIX maybe not necessary, as they are already empty)
            for (TreeProbElement tpe : newOccurences.getTreeProbs())
            {
                lastOccurrences.expandTreeProbEl(tpe);
            }
            for (WordProbElement wpe : newOccurences.getWordProbs())
            {
                lastOccurrences.expandWordProbEl(wpe);
            }
            lastOccurrences.addFringeProbs(newOccurences.getFringeSuperProbs());
            lastOccurrences.addStructProbs(newOccurences.getStructSuperProbs());
            
            // update global parameters
            for (TreeProbElement tpe : lastOccurrences.getTreeProbs())
            {            
                String key = tpe.toString();
                Integer freq = params.getFreqMapTree().get(key);            
                params.getFreqMapTree().put(key, freq != null ? freq + 1 : 1);

            }
    //        for (String nonekey : lastOccurrences.getNoneCounts().keySet()){
    //        if (bigFreqMapTree.containsKey(nonekey)){
    //        bigFreqMapTree.put(nonekey, bigFreqMapTree.get(nonekey)+lastOccurrences.getNoneCounts().get(nonekey));
    //        }
    //        else{
    //        bigFreqMapTree.put(nonekey, lastOccurrences.getNoneCounts().get(nonekey));
    //        }
    //        }
            for (WordProbElement wpe : lastOccurrences.getWordProbs())
            {
                String key = wpe.toString();
                Integer freq = params.getFreqMapWord().get(key);            
                params.getFreqMapWord().put(key, freq != null ? freq + 1 : 1);
            }

            for (SuperTagStructElement stse : lastOccurrences.getStructSuperProbs())
            {
                String key = stse.toString();
                Integer freq = params.getFreqMapStruct().get(key);            
                params.getFreqMapStruct().put(key, freq != null ? freq + 1 : 1);
            }

            for (SuperTagElement key : lastOccurrences.getFringeSuperProbs())
            {            
                Integer freq = params.getFreqMapFringe().get(key);            
                params.getFreqMapFringe().put(key, freq != null ? freq + 1 : 1);
            }
        }        
    }
    
    @Deprecated
    public void writeIncrAnalyses()
    {
        boolean isOracle = opts.semanticsModel == Options.SemanticsModelType.oracle || 
                    opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;
        Utils.logs("Saving " + (isOracle ? "oracle" : "") + " incremental analyses to disk...");
        
        IOUtils.writeObjFileHard(opts.incrAnalysesPath + (isOracle ? ".oracle" : "-" + (incrAnalysesFileNo++)) + ".obj.gz", incrAnalyses);
    }
    
    public void writeIncrAnalysesFeatures()
    {
        boolean isOracle = opts.semanticsModel == Options.SemanticsModelType.oracle || 
                    opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;
        Utils.logs("Saving " + (isOracle ? "oracle" : "") + " incremental analyses features to disk...");
        incrAnalysesFileNo++;
        String filename = opts.incrAnalysesFeaturesPath + (isOracle ? ".oracle" : "-" + (incrAnalysesFileNo)) + ".obj.gz";
        String filenameSentLevel = opts.incrAnalysesFeaturesPath + "_sentenceLevel" + (isOracle ? ".oracle" : "-" + (incrAnalysesFileNo)) + ".obj.gz";
//        String filename = opts.incrAnalysesFeaturesPath + (isOracle ? ".oracle" : "") + "-" + (incrAnalysesFileNo++) + ".obj.gz";
//        IOUtils.writeObjFileHard(filename, incrAnalysesFeatures);        
        incrAnalysesFeatures.saveFeatures(filename, filenameSentLevel);
    }
    
    public Lexicon getLexicon()
    {
        return lexicon;
    }  
    
    public int getLexiconSize()
    {
        return opts.fullLex ? lexicon.getLexSize() : opts.defaultLexiconSize;
    }

    public VerificationLookAheadProbability getVerificationLookAheadProbability()
    {
        return verificationLookAheadProbability;
    }
    
    public Indexer<String> getRoleIndexer()
    {
        return roleIndexer;
    }

    public Indexer<String> getWordIndexer()
    {
        return wordIndexer;
    }
    
    public SuperTagger getSuperTagger()
    {
        return superTagger;
    }

    public Map<Integer, String> getConllPredList()
    {
        return conllPredMap;
    }

    public Map<Integer, String> getConllGoldList()
    {
        return conllGoldMap;
    }    

    public boolean isGoldConllPresent()
    {
        return goldConllPresent;
    }

    public Map<String, HistMap<String>> getDepsArgsFrequencies()
    {
        return depsArgsFrequencies;
    }     

    public ArgumentClassifier getArgumentClassifier()
    {
        return argumentClassifier;
    }

    public Map<Integer, Integer> getTestRoleFreqs()
    {
        return testRoleFreqs;
    }

    public Map<Integer, Integer> getTrainRoleFreqs()
    {
        return trainRoleFreqs;
    }

    public Indexer<String> getConllRoleIndexer()
    {
        return conllRoleIndexer;
    }

    public MaltParserWrapper getMaltParser()
    {
        return maltParser;
    }

    public Map<String, Chart> getIncrAnalyses()
    {
        return incrAnalyses;
    }

    public ExtractFeatures getIncrAnalysesFeatures()
    {
        return incrAnalysesFeatures;
    }

    public DiscriminativeParams getDiscriminativeParams()
    {
        return discriminativeParams;
    }
        
    public Map<String, Pair<String, Integer>> getMajorityDepArgs()
    {
        // build a map of DepRels to the most frequent Argument role. Make note of its frequency for thresholding as well.
        Map<String, Pair<String, Integer>> majorityDepArgs = new HashMap();
        for(Entry<String, HistMap<String>> e : depsArgsFrequencies.entrySet())
        {
            majorityDepArgs.put(e.getKey(), e.getValue().getFirstEntry());
        }
        return majorityDepArgs;
    }

    private Map<Integer, Integer> getGoldRoleFreqs(String conllFile)
    {
        // Map of DepRels to histograms of Arguments (with their relative frequencies)
        HistMap<String> histMap = new HistMap();
        List<PltagExample> conllExamples = Utils.readConllExamples(conllFile);
        for(PltagExample ex : conllExamples)
        {
            try
            {
                ConllExample conllEx = new ConllExample(conllFile, ex.getGoldStandard(), opts, conllRoleIndexer);
                for(Proposition prop : conllEx.getVerbPropositions())
                {
                    for(Argument arg : prop.getArguments())
                    {
                        histMap.add(arg.getRole());
                    }
                }
            } catch(Exception e)
            {
                LogInfo.error(e + "\n" + ex);
            }
        }
        Map<Integer, Integer> map = new HashMap();
        for(Entry<String, Integer> e : histMap.getEntriesFreqs())
        {
            map.put(conllRoleIndexer.getIndex(e.getKey()), e.getValue());
        }
        return map;
    }

    private Pair<Integer, Integer> determineStartEndExample(int size)
    {
        assert opts.startExample < opts.endExample;
        return new Pair<Integer, Integer>(opts.startExample > 0 ? opts.startExample : 0, opts.endExample > size ? size : opts.endExample);
    }
}