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
package pltag.parser.semantics.discriminative;

import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalyses;
import com.esotericsoftware.wildcard.Paths;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.Tree;
import fig.basic.IOUtils;
import static fig.basic.IOUtils.openObjIn;
import fig.basic.LogInfo;
import fig.basic.Pair;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import pltag.parser.BuildBlock;
import pltag.parser.Chart;
import pltag.parser.ChartEntry;
import pltag.parser.Fringe;
import pltag.parser.Node;
import pltag.parser.Options;
import pltag.parser.ParsingTask;
import pltag.parser.StringTreeAnalysis;
import pltag.parser.semantics.DepTreeState;
import pltag.parser.semantics.Dependencies;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.Predicate;
import pltag.parser.semantics.conll.Proposition;
import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalysis;
import pltag.parser.semantics.discriminative.incrAnalyses.NbestIncrementalAnalyses;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class ExtractFeatures implements Serializable
{
    private static final long serialVersionUID = -1L;
    
    public static final int FEAT_BASELINE_SCORE = 0,
            FEAT_BASELINE_WORD_SCORE = 1,
            FEAT_ELEM_TREE = 2,
            FEAT_PREV_ELEM_TREE = 3,
            FEAT_ELEM_TREE_BIGRAM = 4,
            FEAT_ELEM_TREE_UNLEX = 5,
            FEAT_PREV_ELEM_TREE_UNLEX = 6,
            FEAT_ELEM_TREE_UNLEX_BIGRAM = 7,
            FEAT_INTEGRATION_POINT = 8,
            FEAT_FRINGE = 9,
            FEAT_NUM_FRINGE_NODES = 10,
            FEAT_NUM_FRINGE_PREDICT_NODES = 11,
            
            FEAT_RIGHT_BRANCH_REST = 12,
            FEAT_RIGHT_BRANCH_SPINE = 13,
            FEAT_HEAVY = 14,
            FEAT_NEIGHBOURS_L1 = 15,
            FEAT_NEIGHBOURS_L2 = 16,
            FEAT_CO_PAR = 17,
            FEAT_CO_LEN_PAR = 18,
            FEAT_IP_ELEM_TREE = 19,
            FEAT_IP_ELEM_TREE_UNLEX = 20,
            FEAT_WORD_L2 = 21,
            FEAT_WORD_L3 = 22,
            
            FEAT_SEMANTIC_SCORE = 23,
            FEAT_SEMANTIC_VEC_SCORES = 24,
            
            FEAT_SRL_TRIPLES = 25,
            FEAT_SRL_TRIPLES_POS = 26, 
            FEAT_SRL_INCOMPLETE_TRIPLES = 27,
            FEAT_SRL_INCOMPLETE_TRIPLES_POS = 28,
            FEAT_SRL_ROLE = 29,
            FEAT_SRL_DEPENDENCY = 30,
            FEAT_SRL_DEPENDENCY_POS = 31,
            FEAT_SRL_ROLE_PRED = 32,
            FEAT_SRL_ROLE_PRED_POS =33,
            FEAT_SRL_ROLE_ARG = 34,
            FEAT_SRL_ROLE_ARG_POS = 35,
            FEAT_SRL_PRED = 36,
            FEAT_SRL_PRED_POS = 37,
            FEAT_SRL_ARG = 38,
            FEAT_SRL_ARG_POS = 39,            
            FEAT_SRL_FRAME = 40,
            FEAT_SRL_FRAME_POS = 41,
            
            NUM_SYNTACTIC_FEATURES = 12,
            NUM_GLOBAL_SYNTACTIC_FEATURES = 11,
            NUM_SRL_FEATURES = 17,
            NUM_SEMANTIC_FEATURES = 2,
            NUM_SEMANTIC_VEC_SIZE = 36;

//    private final CollinsHeadFinder headFinder;
    private transient final Options opts;
    @Deprecated private Map<String, Chart> incrAnalyses;
    private final Map<String, IncrementalAnalyses> incrAnalysesFeatures, incrAnalysesFeaturesSentLevel;
    private DiscriminativeFeatureIndexers featureIndexers, featureIndexersSentLevel;
    private final ConcurrentLinkedHashMap<String, String> unlexTreeCache;
    public ExtractFeatures()
    {
        this.opts = null;
//        unlexTreeCache = new LRUMap<String, String>(1000);
        incrAnalysesFeatures = new  HashMap<String, IncrementalAnalyses>();
        incrAnalysesFeaturesSentLevel = new  HashMap<String, IncrementalAnalyses>();
        unlexTreeCache = new ConcurrentLinkedHashMap.Builder<String, String>().maximumWeightedCapacity(1000).build();
//        headFinder = new CollinsHeadFinder();
    }
    
    public ExtractFeatures(Options opts)
    {
        this.opts = opts;
        featureIndexers = new DiscriminativeFeatureIndexers();
        featureIndexersSentLevel = new DiscriminativeFeatureIndexers();
        incrAnalysesFeatures = new HashMap<String, IncrementalAnalyses>();
        incrAnalysesFeaturesSentLevel = new  HashMap<String, IncrementalAnalyses>();
//        unlexTreeCache = new LRUMap<String, String>(1000);
        unlexTreeCache = new ConcurrentLinkedHashMap.Builder<String, String>().maximumWeightedCapacity(1000).build();
//        headFinder = new CollinsHeadFinder();
    }

    public void execute()
    {
        boolean isOracle = opts.semanticsModel == Options.SemanticsModelType.oracle || opts.semanticsModel == Options.SemanticsModelType.oracleAllRoles;
        if(isOracle)
        {
            incrAnalyses = readCharts(opts.inputPaths.get(0) + ".oracle.obj.gz", true);
            processCharts(incrAnalyses, true);
        } else
        {
            File f = new File(opts.inputPaths.get(0));
            Paths paths = new Paths(f.getParent(), f.getName() + "-*");
            for (String path : paths.getPaths())
            {
                incrAnalyses = readCharts(path, false);
                processCharts(incrAnalyses, false);
            }
        }
    }

    /**
     * Load feature indexers from serialized object file
     *
     * @param saveIncrAnalysesFeatures
     */
      public void loadFeatureIndexers(boolean saveIncrAnalysesFeatures, boolean isDirectory)
    {
        if(isDirectory)
//            featureIndexers = DiscriminativeFeatureIndexers.loadFeatureIndexers(opts.discriminativeFeatureIndexers);
            featureIndexers = new DiscriminativeFeatureIndexers(opts.discriminativeFeatureIndexers);
        else
//            featureIndexers = (DiscriminativeFeatureIndexers) IOUtils.readObjFileHard(opts.discriminativeFeatureIndexers + ".obj.gz");
            featureIndexers = new DiscriminativeFeatureIndexers((DiscriminativeFeatureIndexers) IOUtils.readObjFileHard(opts.discriminativeFeatureIndexers + ".obj.gz"));
        if(saveIncrAnalysesFeatures)
        {
            if(isDirectory)
//                featureIndexersSentLevel = DiscriminativeFeatureIndexers.loadFeatureIndexers(opts.discriminativeFeatureIndexers);
                featureIndexersSentLevel = new DiscriminativeFeatureIndexers(opts.discriminativeFeatureIndexers);
            else
//                featureIndexersSentLevel = (DiscriminativeFeatureIndexers) IOUtils.readObjFileHard(opts.discriminativeFeatureIndexers + ".obj.gz");// + "_sentenceLevel.obj.gz");
                featureIndexersSentLevel = new DiscriminativeFeatureIndexers((DiscriminativeFeatureIndexers) IOUtils.readObjFileHard(opts.discriminativeFeatureIndexers + ".obj.gz"));// + "_sentenceLevel.obj.gz");
        }
    }

    public void setFeatureIndexers(DiscriminativeFeatureIndexers featureIndexers)
    {
        this.featureIndexers = featureIndexers;
    }

    /**
     * Save feature indexers to file
     *
     * @param extend
     * @return
     */
    public void saveFeatureIndexers(boolean extend)
    {
        // Commented out saving indexers for full chart features, as it is a very lengthy process
//        featureIndexers.saveFlattenedFeatureIndexers(opts.discriminativeFeatureIndexers + (extend ? "_extended" : ""));
        featureIndexersSentLevel.saveFlattenedFeatureIndexers(opts.discriminativeFeatureIndexers + "_sentenceLevel" + (extend ? "_extended" : ""));
        try
        {
            // Commented out saving indexers for full chart features, as it is a very lengthy process
//            IOUtils.writeObjFileHard(opts.discriminativeFeatureIndexers + (extend ? "_extended" : "") + ".obj.gz", featureIndexers);
            IOUtils.writeObjFileHard(opts.discriminativeFeatureIndexers + "_sentenceLevel" + (extend ? "_extended" : "") + ".obj.gz", featureIndexersSentLevel); //&&
        }
        catch(Exception e)
        {
            LogInfo.error(e);
        }
//               IOUtils.writeObjFileEasy(opts.incrAnalysesFeaturesPath, incrAnalysesFeatures);

    }

//    public boolean saveFeatureIndexersFlat(boolean extend)
//    {
//        String indexerFilename = opts.discriminativeFeatureIndexers + (extend ? "_extended" : "") + ".obj.gz";
//        String indexerSentLevelFilename = opts.discriminativeFeatureIndexers + "_sentenceLevel" + (extend ? "_extended" : "") + ".obj.gz";
//        
//    }
    
    public void loadFeatures(String path)
    {
//        incrAnalysesFeatures = (Map<String, IncrementalAnalyses>) IOUtils.readObjFileHard(path);        
        try
        {
            ObjectInputStream in = openObjIn(path);        
            Map<String, IncrementalAnalyses> map = (Map<String, IncrementalAnalyses>) in.readObject();
//            for(String key : map.keySet())
//                System.out.println(key);
            incrAnalysesFeatures.putAll(map);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public boolean saveFeatures(String path, String pathSentLevel)
    {
        // Commented out saving of full incremental features as it is a very length process.
//        return IOUtils.writeObjFileEasy(path, incrAnalysesFeatures) &&
//        IOUtils.writeObjFileEasy(pathSentLevel, incrAnalysesFeaturesSentLevel);        
        return IOUtils.writeObjFileEasy(pathSentLevel, incrAnalysesFeaturesSentLevel);        
    }
    
    @Deprecated
    private Map<String, Chart> readCharts(String inputPath, boolean isOracle)
    {
        Utils.logs("Reading " + (isOracle ? "oracle" : "") + " incremental analyses from file " + inputPath + " ...");
        Map<String, Chart> map = (Map<String, Chart>) IOUtils.readObjFileEasy(inputPath);
        if (map != null)
        {
            System.out.println("Done");
            System.out.println(map.size()); // remove
        } else
        {
            LogInfo.error("Error loading " + (isOracle ? "oracle" : "") + " incremental analyses object file");
            map = new HashMap();
        }
        return map;
    }

    @Deprecated
    private void processCharts(Map<String, Chart> incrAnalyses, boolean isOracle)
    {        
        for (Entry<String, Chart> entry : incrAnalyses.entrySet())
        {
            incrAnalysesFeatures.put(entry.getKey(), extractFeaturesFromChart(entry.getValue(), isOracle));
        }     
    }

    /**
     * Process chart entries and extract features for prediction model, outputting features from arg,max analysis at word and sentence 
     * level
     * @param name
     * @param chart
     * @param analyses 
     */
    public void processChart(String name, Chart chart, List<StringTreeAnalysis> analyses)
    {
        // word-level feature extraction: arg,max
//        IncrementalAnalyses incr = extractFeaturesFromChart(chart, false);
//        synchronized(incrAnalysesFeatures)
//        {
//            incrAnalysesFeatures.put(name, incr);
//        }
        // sentence-level feature extraction: arg,max      
        IncrementalAnalyses incrSentence = extractFeaturesFromChartSentenceLevel(chart, analyses);
        synchronized(incrAnalysesFeaturesSentLevel)
        {
            incrAnalysesFeaturesSentLevel.put(name, incrSentence);            
        }        
    }
    
    /**
     * 
     * Process chart entries and extract features for oracle model, outputting features from gold-standard (word-Level) and oracle,max F1
     * (sentenceLevel analysis with max F1 score)
     * @param name
     * @param chart
     * @param analyses
     * @param maxF1Analysis 
     */
    public void processChartOracle(String name, Chart chart, List<StringTreeAnalysis> analyses,  StringTreeAnalysis maxF1Analysis)
    {
        IncrementalAnalyses incr = extractFeaturesFromChart(chart, true);
        synchronized(incrAnalysesFeatures)
        {
            incrAnalysesFeatures.put(name, incr);
        }
        IncrementalAnalyses incrMaxF1 = extractFeaturesFromChartOracle(chart, maxF1Analysis);
        synchronized(incrAnalysesFeaturesSentLevel)
        {
            incrAnalysesFeaturesSentLevel.put(name, incrMaxF1);        
        }        
    }
    
    public IncrementalAnalyses extractFeaturesFromChart(Chart chart, boolean isOracle)
    {
//        int count = 0;
        int words = chart.length();        
        IncrementalAnalyses analyses = new IncrementalAnalyses(words);
        for (int i = 0; i < words; i++)
        {
            ArrayList<ChartEntry> nbest = chart.get(i);
            int nbestPos = 0;
            NbestIncrementalAnalyses nbestAnalyses;
            if(isOracle)
            {
                nbestAnalyses = new NbestIncrementalAnalyses(1);
                nbestAnalyses.addIncrementalAnalysis(extractFeaturesFromChartEntry(chart, nbest.get(0), nbest.get(0).getBestProbability(), i == words - 1, i, featureIndexers, true), 0);
            }
            else
            {
                nbestAnalyses = new NbestIncrementalAnalyses(nbest.size());
                for (ChartEntry entry : nbest)
                {
//                    IncrementalAnalysis incrAnalysis = extractFeaturesFromChartEntry(chart, entry, entry.getBestProbability(), i, true);
//                    if(incrAnalysis == null)
//                        count++;
//                    nbestAnalyses.addIncrementalAnalysis(incrAnalysis, nbestPos++);
                    nbestAnalyses.addIncrementalAnalysis(extractFeaturesFromChartEntry(chart, entry, entry.getBestProbability(), i == words - 1, i, featureIndexers, true), nbestPos++);
                }
            }            
            analyses.addNbestAnalyses(nbestAnalyses, i);
        }
//        System.out.println("Null incr analyses: " + count);
        return analyses;
    }
    
    /**
     * 
     * Extract features from chart at a sentence level. The method extract the backpointer traces
     * at the last word, and extracts features from the chart entries for each trace. The result is
     * an <code>IncrementalAnalyses</code> object with a fixed number of n-best entries for each word.
     * Each entry corresponds to the n-th part of the trace of the n-th sentence, where n is the number 
     * of analyses at the end of the sentence.
     * @param chart
     * @param treeAnalyses
     * @return 
     */
    public IncrementalAnalyses extractFeaturesFromChartSentenceLevel(Chart chart, List<StringTreeAnalysis> treeAnalyses)
    {
        int words = chart.length();
        int numOfsentences = treeAnalyses.size();
        IncrementalAnalyses analyses = new IncrementalAnalyses(words);
        for (int i = 0; i < words; i++)
        {
            analyses.addNbestAnalyses(new NbestIncrementalAnalyses(numOfsentences), i);
        }
        // get built traces from analyses (the format is [wordId_1, chartSlice_1.entryId][wordId_2, chartSlize_2.entryId]...
        int col = 0;
        for(StringTreeAnalysis analysis : treeAnalyses)
        {            
            Map<Integer, Trace> traceMap = reFormatTrace(analysis.getBuiltTrace());
//            System.out.println("Complete analysis: " + analysis.getProbability() + " " + ParsingTask.convertStringTree(analysis));
//            System.out.println(analysis.getBuiltTrace());
            for(Entry<Integer, Trace> entry : traceMap.entrySet())
            {
                Integer timestamp = entry.getKey();
                Trace trace = entry.getValue();                
                ArrayList<ChartEntry> nbest = chart.get(timestamp);
//                analyses.getNbestAnalyses()[timestamp].addIncrementalAnalysis(extractFeaturesFromChartEntry(nbest.get(trace.entryPos), trace.bbPos, trace.analysisPos, timestamp, true), col);
                analyses.getNbestAnalyses()[timestamp].addIncrementalAnalysis(extractFeaturesFromChartEntry(chart, nbest.get(trace.entryPos), trace.analysisProb, timestamp == words - 1, timestamp, featureIndexersSentLevel, true), col);
            }
            col++;
        }                        
        return analyses;
    }
    
    /**
     * 
     * Extracts features for analysis with highest evalb F1 score at the end of the sentence.
     * @param chart
     * @param analysis
     * @return 
     */
    public IncrementalAnalyses extractFeaturesFromChartOracle(Chart chart, StringTreeAnalysis analysis)
    {
        return extractFeaturesFromChartSentenceLevel(chart, Arrays.asList(new StringTreeAnalysis[] {analysis}));
    }

    /**
     * 
     * Reformat built traces from analyses. The input format is [wordId_1, chartSlice_1.entryId][wordId_2, chartSlice_2.entryId]...
     * and output format is a map with the word_id as the key and chartSlice.entryId as the value.
     * @param trace
     * @return 
     */
    private Map<Integer, Trace> reFormatTrace(String trace)
    {
        Map<Integer, Trace> map = new HashMap();
        for(String token : trace.split("]"))
        {
            String[] pair = token.split(",");
            Integer wordId = Integer.valueOf(pair[0].substring(1));
            String[] ceIndexBbPos = pair[1].substring(1).split(" ");
            Integer entryId = Integer.valueOf(ceIndexBbPos[0]);
            Integer bbPos = Integer.valueOf(ceIndexBbPos[1]);
//            Integer analysisPos = Integer.valueOf(ceIndexBbPos[2]);
            Double analysisProb = Double.valueOf(ceIndexBbPos[2]);
//            map.put(wordId, new Trace(entryId, bbPos, analysisProb));
            map.put(wordId, new Trace(entryId, bbPos, analysisProb));
        }
        return map;
    }
    
    public IncrementalAnalysis extractFeaturesFromChartEntry(Chart chart, ChartEntry entry, double analysisProb, boolean endOfSent, int timestamp, boolean train)
    {
        return extractFeaturesFromChartEntry(chart, entry, analysisProb, endOfSent, timestamp, featureIndexers, train);
    }
            
    public IncrementalAnalysis extractFeaturesFromChartEntry(Chart chart, ChartEntry entry, double analysisProb, boolean endOfSent, int timestamp, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        IncrementalAnalysis analysis = new IncrementalAnalysis();
//        analysis.setBaselineScore(entry.getBestProbability());
//        analysis.setBaselineWordScore(entry.getBuildBlocks().get(0).getProbability());
        if(!extractSyntacticFeatures(chart, entry, analysisProb, endOfSent, analysis, timestamp, featureIndexers, train))
            return null;
        DepTreeState treeState = ((DepTreeState) entry.getTreeState());
        extractSrlFeatures(treeState.getDependencies(), analysis, timestamp, featureIndexers, train);
//        extractSemanticFeatures(analysis); // TODO: implement only if we need semantics on the fly
//        double partialSyntacticScore = computeSyntacticScore(entry);
//        double partialSrlScore = computeSemanticScore(treeState.getDependencies());
        return analysis;
    }

    private boolean extractSyntacticFeatures(Chart chart, ChartEntry entry, double analysisProb, boolean endOfSent, IncrementalAnalysis analysis, int timestamp, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        // contains information on the current tree and pointer to the previous tree
        List<BuildBlock> bbList = entry.getBuildBlocks();
        if (!bbList.isEmpty())
        {            
            Pair<StringTreeAnalysis, BuildBlock> currentAnalysisPair = getCurrentAnalysis(bbList, analysisProb);
            if(currentAnalysisPair == null)
            {
                ArrayList<ArrayList<BuildBlock>>[] emptyBBlist = ParsingTask.getEmptyBBList(chart, entry);                 
                // create backpointers on buildblocks to be exploited in the feature extraction process
                chart.setTime(System.currentTimeMillis());
                chart.getAnalyses(entry, emptyBBlist, "", true, analysisProb, false); 
//                LogInfo.error("Current buildblock not found");
                currentAnalysisPair = getCurrentAnalysis(bbList, analysisProb);
                if(currentAnalysisPair == null)
                    return false;
            }
            BuildBlock curBb = currentAnalysisPair.getSecond();
            StringTreeAnalysis sta = currentAnalysisPair.getFirst();
//            BuildBlock curBb = bbList.get(buildBlockPos);
//            StringTreeAnalysis sta = curBb.getAnalysis(analysisProb);
//            System.out.println("\t" + Utils.normaliseTree(ParsingTask.convertStringTree(sta)));
            analysis.setBaselineScore(sta.getProbability());
            analysis.setBaselineWordScore(curBb.getProbability());
            extractTreeFeatures(curBb, analysis, false, featureIndexers, train);
            extractFringeFeatures(entry, analysis, featureIndexers, train);
            // get previous entry (if any)
            if (timestamp == 0)
            {
                analysis.setPrevElemTree("S", featureIndexers, train);
                analysis.setPrevElemTreeUnlex("S", featureIndexers, train);
            } else
            {
                List<BuildBlock> prevBbList = curBb.getPrevChartEntry().getBuildBlocks();
                if (!prevBbList.isEmpty())
                {
                    Pair<StringTreeAnalysis, BuildBlock> prevAnalysisPair = getCurrentAnalysis(prevBbList, analysis.getBaselineScore() - analysis.getBaselineWordScore());
                    if(prevAnalysisPair == null)
                    {
//                        LogInfo.error("Current prev buildblock not found");
                        return false;
                    }
//                    System.out.println("\t\t" + Utils.normaliseTree(ParsingTask.convertStringTree(prevAnalysisPair.getFirst())));
                    extractTreeFeatures(prevAnalysisPair.getSecond(), analysis, true, featureIndexers, train);
                }
            }
            // get bigrams
            analysis.setElemTreeBigram(analysis.getPrevElemTree() + " " + analysis.getElemTree(), featureIndexers, train);
            analysis.setElemTreeUnlexBigram(analysis.getPrevElemTreeUnlex() + " " + analysis.getElemTreeUnlex(), featureIndexers, train);
            // get integration point
            analysis.setIntegrationPoint(curBb.getIpi().getCategory(), featureIndexers, train);
            
            // global syntactic features
            if(opts.useGlobalSyntacticFeatures)
                extractStringTreeAnalysisFeatures(sta, analysis, timestamp + 1, endOfSent, featureIndexers, train);                        
        }
        return true;
    }

    private Pair<StringTreeAnalysis, BuildBlock> getCurrentAnalysis(List<BuildBlock> bbList, double probability)
    {
//        Set<Pair<StringTreeAnalysis, BuildBlock>> set = new TreeSet(new Pair.FirstComparator());
//        List<Pair<StringTreeAnalysis, BuildBlock>> list = new ArrayList();
        for(BuildBlock bb : bbList)
        {
            for(Pair<StringTreeAnalysis, BuildBlock> pair : bb.getAnalyses())
            {
                if(Math.abs(pair.getFirst().getProbability() - probability) < 0.00000001)
                {
//                    System.out.println("\t" + Utils.normaliseTree(ParsingTask.convertStringTree(pair.getFirst())));
                    return pair;
                }
            }
        }
        return null;
    }
    
    private void extractTreeFeatures(BuildBlock bb, IncrementalAnalysis analysis, boolean prev, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        // get elementary tree        
//        String elemTree = bb.getElemTreeString();
        String elemTree = bb.getElemTreeString();
        if(!prev)
            analysis.setElemTree(elemTree, featureIndexers, train);
        else
            analysis.setPrevElemTree(elemTree, featureIndexers, train);
        // get unlex elementary tree
//        String elemTreeUnlex = bb.getElemTree().toStringUnlex(false); // TODO: Use LRU cache
        String elemTreeUnlex = getUnlexTree(elemTree, bb);
        if(!prev)
            analysis.setElemTreeUnlex(elemTreeUnlex, featureIndexers, train);
        else
            analysis.setPrevElemTreeUnlex(elemTreeUnlex, featureIndexers, train);
        //
    }
    
    private void extractFringeFeatures(ChartEntry entry, IncrementalAnalysis analysis, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        // get raw fringe 
        Fringe fringe = entry.getTreeState().getFringe();
        // create indexed open-left and open-right fringe lists, substitution node and up/down indices (to indicate prediction nodes)
        byte[] fringeUpIndex = new byte[fringe.size()];
        byte[] fringeDownIndex = new byte[fringe.size()];        
        int[] openRight = new int[fringe.getAdjNodesOpenRight().size()];
        int[] openLeft = new int[fringe.getAdjNodesOpenLeft().size()];        
        int i = 0;        
        for(Node node : fringe.getAdjNodesOpenRight())
        {
            openRight[i] = featureIndexers.getCategoryIndex(node.getCategory(), train);
            fringeUpIndex[i] = node.getUpIndex();
            fringeDownIndex[i] = node.getDownIndex();
            i++;
        }
        int j = 0;
        for(Node node : fringe.getAdjNodesOpenLeft())
        {            
            openLeft[j] = featureIndexers.getCategoryIndex(node.getCategory(), train);
            fringeUpIndex[i] = node.getUpIndex();
            fringeDownIndex[i] = node.getDownIndex();
            i++; j++;
        }        
        Node substNode = fringe.getSubstNode();
        if(substNode != null)
        {
            fringeUpIndex[i] = substNode.getUpIndex();
            fringeDownIndex[i] = substNode.getDownIndex();
        }    
        analysis.setFringeOpenRight(openRight);
        analysis.setFringeOpenLeft(openLeft);
        analysis.setFringeSubstNode(featureIndexers.getCategoryIndex(substNode == null ? "U" : substNode.getCategory(), train));
        analysis.setFringeUpIndex(fringeUpIndex);
        analysis.setFringeDownIndex(fringeDownIndex);
        analysis.setNumFringeNodes(fringe.size());
        analysis.setNumPredictFringeNodes(fringe.numOfPredictNodes());

    }
    
    private void extractStringTreeAnalysisFeatures(StringTreeAnalysis sta, IncrementalAnalysis analysis, int numOfWords, boolean endOfSent, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        String stringAnalysisTree = ParsingTask.convertStringTree(sta);
        String inputTree = Utils.removeSubtreesAfterWord(Utils.normaliseTree(stringAnalysisTree), numOfWords);
        extractStringTreeAnalysisFeatures(inputTree, stringAnalysisTree, endOfSent, analysis, featureIndexers, train);
    }
    
    private void extractStringTreeAnalysisFeatures(String inputTree, String stringAnalysisTree, boolean endOfSent, IncrementalAnalysis analysis, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
//        System.out.println(inputTree);        
        Tree tree = Tree.valueOf(inputTree);        
        List<Tree> leaves = tree.getLeaves();
        Tree currentWord = leaves.get(leaves.size() - 1);
        int currentWordIndex = featureIndexers.getWordIndex(currentWord.nodeString(), train);
        // right branch (2): 1. Count number of nodes from root to rightmost non-terminal, 2. Count rest nodes
        // compute domination path from root to rightmost leaf. Subtract 2 from size to exclude root and pre-terminal
        int pathSize = tree.dominationPath(currentWord.parent(tree)).size();
        analysis.setRightBranchSpine(pathSize > 2 ? pathSize - 2 : 0);
        int rest = tree.size() - analysis.getRightBranchSpine() - leaves.size() * 2 - 1;
        analysis.setRightBranchRest(rest > 0 ? rest : 0); // Subtract the number of terminals, pre-terminals (leaves.size()*2) and root symbol
                
        // get list of rightmost complete non-terminals. We don't compute the whole list of complete non-terminals, but only the ones that have been just completed,
        // hence lie at the rightmost position of the tree. Since we compute the features piecemeal, by the end of the sentence we will have computed all complete
        // non-terminals, depending on the training strategy. Used for features: heavy, neighbours, and edges
        Tree analysisTree = Tree.valueOf(stringAnalysisTree);
        analysisTree.indexLeaves();
        List<Tree> complete = getListOfRightMostCompleteNonTerminals(analysisTree);
        String[] heavyStr = new String[complete.size()];
        String[] neighboursL1Str = new String[complete.size()];
        String[] neighboursL2Str = new String[complete.size()];
        int i = 0;
        for(Tree subTree : complete)
        {
            // heavy feature
            int categoryIndex = featureIndexers.getCategoryIndex(subTree.nodeString(), train);
            List<Label> yield = subTree.yield();
            String yieldSize = yield.size() > 5 ? "5+" : String.valueOf(yield.size());
            heavyStr[i] = String.format("%s %s %s", categoryIndex, yieldSize, endOfSent ? "y" : "n");            
            // neighbours l1, l2 features            
            int leftmostLeafId = ((CoreLabel)yield.get(0)).index();
            if(leftmostLeafId > 1)
            {
                int l1CategoryId = featureIndexers.getCategoryIndex(leaves.get(leftmostLeafId - 2).parent(tree).nodeString(), train);
                if(leftmostLeafId > 2)
                {
                    neighboursL1Str[i] = String.format("%s %s %s", categoryIndex, yieldSize, l1CategoryId);
                    int l2CategoryId = featureIndexers.getCategoryIndex(leaves.get(leftmostLeafId - 3).parent(tree).nodeString(), train);
                    neighboursL2Str[i] = String.format("%s %s %s %s", categoryIndex, yieldSize, l2CategoryId, l1CategoryId);
                }
                else
                {
                    neighboursL2Str[i] = String.format("%s %s SoS %s", categoryIndex, yieldSize, l1CategoryId);
                }
            }
            else // leftmost leaf is at the beginning of the sentence
            {
                neighboursL1Str[i] = String.format("%s %s SoS", categoryIndex, yieldSize);
                neighboursL2Str[i] = String.format("%s %s SoS SoS", categoryIndex, yieldSize);
            }
            
            // coPar and coLenPar features
            Tree[] children = subTree.children();
            if(children.length > 2)
            {
                // found structure: (X (A ...) (CC and/or) (B ...))
                if(children.length == 3 && children[1].nodeString().startsWith("CC"))
                {
                    analysis.setCoPar(getCoParString(children[0], children[2]), featureIndexers, train);                                        
                    analysis.setCoLenPar(getCoLenParString(children[0], children[2], endOfSent), featureIndexers, train);
                }  
                // found structure ((CC either) (A ...) (CC or) (B...))
                else if(children.length == 4 && children[0].nodeString().startsWith("CC") && children[2].nodeString().startsWith("CC"))
                {
                    analysis.setCoPar(getCoParString(children[1], children[3]), featureIndexers, train);                                        
                    analysis.setCoLenPar(getCoLenParString(children[1], children[3], endOfSent), featureIndexers, train);
                }
                // found structure ((A ...) (, ,) (CC but) (B...))
                else if(children.length == 4 && children[1].nodeString().equals(",") && children[2].nodeString().startsWith("CC"))
                {
                    analysis.setCoPar(getCoParString(children[0], children[3]), featureIndexers, train);                                        
                    analysis.setCoLenPar(getCoLenParString(children[0], children[3], endOfSent), featureIndexers, train);
                }
            }
            i++;
        }
        analysis.setHeavy(heavyStr, featureIndexers, train);
        analysis.setNeighboursL1(neighboursL1Str, featureIndexers, train);
        analysis.setNeighboursL2(neighboursL2Str, featureIndexers, train);
        
        // compute word + L=2 ancestor nodes, L=3 ancestor nodes
        Tree preTerminal = currentWord.parent(tree);        
        Tree wordL2 = preTerminal.parent(tree);
        if(wordL2 != null)
        {
            int preTerminalIndex = featureIndexers.getCategoryIndex(preTerminal.nodeString(), train);
            int wordL2Index = featureIndexers.getCategoryIndex(wordL2.nodeString(), train);
            analysis.setWordL2(String.format("%s %s %s", currentWordIndex, preTerminalIndex, wordL2Index), featureIndexers, train);
            Tree wordL3 = wordL2.parent(tree);
            if(wordL3 != null)
            {
                analysis.setWordL3(String.format("%s %s %s %s", currentWordIndex, preTerminalIndex, wordL2Index, 
                        featureIndexers.getCategoryIndex(wordL3.nodeString())), featureIndexers, train);
            }
        }  
        
        // get integration point + elem tree (Parent-emulation feature)
        analysis.setIpElemTree(String.format("%s,%s", analysis.getIntegrationPoint(), analysis.getElemTree()), featureIndexers, train);
        analysis.setIpElemTreeUnlex(String.format("%s,%s", analysis.getIntegrationPoint(), analysis.getElemTreeUnlex()), featureIndexers, train);
    }
    
    /**
     * Identify the list of rightmost non-terminals that span a complete subtree, i.e., one that
     * a) the leaf of its' rightmost child is a word, OR
     * b) the index of the leaf of its' rightmost is a word AND is the last in the yield (AND this leaf is the last word - optional, as this condition breeches incrementality).
     * @param analysisTree
     * @return 
     */
    private List<Tree> getListOfRightMostCompleteNonTerminals(Tree tree)
    {
        List<Tree> list = new ArrayList();
        List<Tree> leaves = tree.getLeaves();
        // check if the last leaf is a word.
        Tree currentWord = leaves.get(leaves.size() - 1);
        if(currentWord.nodeString().endsWith("<>"))
        {
            Tree parent = currentWord.parent(tree);
            while(parent != tree)
            {
                if(parent.isPhrasal())
                {
                    list.add(parent);
                }
                parent = parent.parent(tree);
            }
            list.add(tree);
        }
        return list;
    }
    
    private String getCoParString(Tree left, Tree right)
    {
        if(sameLabelAtLevel(left, right, 0))
        {
            if(sameLabelAtLevel(left, right, 1))
            {
                if(sameLabelAtLevel(left, right, 2))
                {
                    if(sameLabelAtLevel(left, right, 3))
                        return "1111";
                    else
                        return "1110";
                }
                else
                {
                    return "1100";
                }
            } // if
            else
            {
                return "1000";
            }
        }
        else
        {
            return "0000";
        }
    }
    
    private String getCoLenParString(Tree left, Tree right, boolean endOfSent)
    {
        int leftSize = left.yield().size();
        int rightSize = right.yield().size();
        int diff = Math.abs(rightSize - leftSize);
        return String.format("%s %s", diff > 5 ? "5+" : String.valueOf(diff), endOfSent ? "y" : "n");
    }
    
    private boolean sameLabelAtLevel(Tree left, Tree right, int level)
    {
        if(left.isLeaf() || right.isLeaf())
            return false;
        if(level == 0)
            return left.nodeString().equals(right.nodeString());
        Tree[] leftChildren = left.children();
        Tree[] rightChildren = right.children();
        if(leftChildren.length != rightChildren.length)
            return false;
        for(int i = 0; i < leftChildren.length; i++)
        {
            if(!sameLabelAtLevel(leftChildren[i], rightChildren[i], level - 1))
                return false;
        }
        return true;
    }
    
    private void extractSrlFeatures(Dependencies dependencies, IncrementalAnalysis analysis, int timestamp, DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {
        // get the set of all dependencies for more future use with more complex semantic-aware features        
        Pair<Proposition[], Proposition[]> completeIncompleteProps = dependencies.toPropositions(featureIndexers, train);
//        analysis.setPropositions(propositions);
        List<String> triples = new ArrayList();
        List<String> triplesPos = new ArrayList();
        BitSet triplesArgComplete = new BitSet();
        // Get collection of propositions at a specific timestamp. The list includes all semantic triples
        // with either their argument or predicate at the given timestamp
        int i =0;
        for (Proposition prop : completeIncompleteProps.getFirst())
        {
            Predicate pred = prop.getPredicate();
            int predTimestamp = pred.getTimestamp();
            if (predTimestamp == timestamp) // current word is a predicate. Add all triples in the proposition and exit
            {
                for (Argument arg : prop.getArguments())
                {
                    triples.add(String.format("%s,%s,%s", arg.getRoleInt(), arg.getFormInt(), pred.getLemmaInt()));
                    triplesPos.add(String.format("%s,%s,%s", arg.getRoleInt(), arg.getPosTagInt(), pred.getPosTagInt()));
                    i++;
                }
                break;
            } else // search all arguments
            {
                for (Argument arg : prop.getArguments())
                {
                    // current word is an argument. Add triple and go to the next proposition in case the same argument
                    // is in an another proposition as well
                    if (arg.getTimestamp() == timestamp)
                    {
                        triples.add(String.format("%s,%s,%s", arg.getRoleInt(), arg.getFormInt(), pred.getLemmaInt()));
                        triplesPos.add(String.format("%s,%s,%s", arg.getRoleInt(), arg.getPosTagInt(), pred.getPosTagInt()));
                        triplesArgComplete.set(i++);
                        break;
                    }
                }
            } // else
        } // for all propositions
        analysis.setSrlTriples(triples.toArray(new String[0]), featureIndexers, train);
        analysis.setSrlTriplesPos(triplesPos.toArray(new String[0]), featureIndexers, train);
        analysis.setArgComplete(triplesArgComplete);
        
        // deal with incomplete propositions
        List<String> incompleteTriples = new ArrayList();
        List<String> incompleteTriplesPos = new ArrayList();
        for (Proposition prop : completeIncompleteProps.getSecond())
        {
            Predicate pred = prop.getPredicate();
            int predTimestamp = pred.getTimestamp();
            if(predTimestamp == -1) // predicate incomplete
            {
                Argument arg = prop.getArguments().get(0);
                if(arg.getTimestamp() == timestamp && arg.getFormInt() != -1)
                {
                    incompleteTriples.add(String.format("%s,-", arg.getFormInt()));
                    incompleteTriplesPos.add(String.format("%s,-", arg.getPosTagInt()));
                }
            }
            else if(predTimestamp == timestamp && pred.getLemmaInt() != -1)
            {
                incompleteTriples.add(String.format("-,%s", pred.getLemmaInt()));
                incompleteTriplesPos.add(String.format("-,%s", pred.getPosTagInt()));
            }
        }
        analysis.setSrlIncompleteTriples(incompleteTriples.toArray(new String[0]), featureIndexers, train);
        analysis.setSrlIncompletePosTriples(incompleteTriplesPos.toArray(new String[0]), featureIndexers, train);
    }

    private String getUnlexTree(String elemTree, BuildBlock bb)
    {
        String unlexFromCache;
//        synchronized(unlexTreeCache)
//        {
            unlexFromCache = unlexTreeCache.get(elemTree);
//        }        
        if(unlexFromCache != null)
            return unlexFromCache;
        String unlexFromStringTree = bb.getElemTree().toStringUnlex(false);
//        synchronized(unlexTreeCache)
//        {
            unlexTreeCache.put(elemTree, unlexFromStringTree);
//        }        
        return unlexFromStringTree;
    }

    public boolean isEmpty()
    {
        return incrAnalysesFeatures.isEmpty();
    }
    
    public boolean isEmptySentenceLevel()
    {
        return incrAnalysesFeaturesSentLevel.isEmpty();
    }

    public int size()
    {
        return incrAnalysesFeatures.size();
    }
    
    public int sizeSentenceLevel()
    {
        return incrAnalysesFeaturesSentLevel.size();
    }

    public void clearFeatures()
    {
        incrAnalysesFeatures.clear();
        incrAnalysesFeaturesSentLevel.clear();
    }

    public Set<Entry<String, IncrementalAnalyses>> getIncrAnalysesFeaturesSet()
    {
        return incrAnalysesFeatures.entrySet();
    }    
    
    public IncrementalAnalyses getIncrAnalysesFeatures(String name)
    {
        return incrAnalysesFeatures.get(name);
    }
    
    public void addAll(ExtractFeatures featuresIn)
    {
        incrAnalysesFeatures.putAll(featuresIn.incrAnalysesFeatures);
    }

    public DiscriminativeFeatureIndexers getFeatureIndexers()
    {
        return featureIndexers;
    }

    public Map<String, IncrementalAnalyses> getIncrAnalysesFeaturesSentLevel()
    {
        return incrAnalysesFeaturesSentLevel;
    }
        
    public void extractStringTreeAnalysisFeaturesTest(String inputTree, String stringAnalysisTree, boolean endOfSent, DiscriminativeFeatureIndexers featureIndexers)
    {
        extractStringTreeAnalysisFeatures(inputTree, stringAnalysisTree, endOfSent, new IncrementalAnalysis(), featureIndexers, true);
    }   
    
    class Trace 
    {
        int entryPos, bbPos, analysisPos;
        double analysisProb;

        public Trace(int entryPos, int bbPos, int analysisPos, double analysisProb)
        {
            this.entryPos = entryPos;
            this.bbPos = bbPos;
            this.analysisPos = analysisPos;
            this.analysisProb = analysisProb;
        }       
        
        public Trace(int entryPos, int bbPos, int analysisPos)
        {
            this(entryPos, bbPos, analysisPos, -1);
        }       
        
        public Trace(int entryPos, int bbPos, double analysisProb)
        {
            this(entryPos, bbPos, -1, analysisProb);
        }       

        @Override
        public String toString()
        {
            return String.format("[%s %s %s]", entryPos, bbPos, analysisPos != -1 ? analysisPos : analysisProb);
        }                
    }
}
