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

import com.esotericsoftware.wildcard.Paths;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import fig.basic.FullStatFig;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.StopWatchSet;
import fig.exec.Execution;
import fig.record.Record;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import pltag.parser.Options;
import pltag.parser.Options.InitType;
import static pltag.parser.Options.InitType.uniformz;
import pltag.parser.Options.LearningScheme;
import pltag.parser.Options.SemanticsType;
import pltag.parser.params.VecFactory;
import pltag.parser.performance.Performance;
import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalyses;
import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalysis;
import pltag.parser.semantics.discriminative.incrAnalyses.NbestIncrementalAnalyses;
import pltag.parser.semantics.discriminative.optimizer.DefaultPerceptron;
import pltag.parser.semantics.discriminative.optimizer.DefaultPerceptronFast;
import pltag.parser.semantics.discriminative.optimizer.GradientBasedOptimizer;
import pltag.util.CallableWithLog;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class OfflineReranker
{

    private final Options opts;
    private ExtractFeatures incrAnalysesFeatures, oracleIncrAnalysesFeatures;
    private DiscriminativeFeatureIndexers featureIndexers;
    private int numProcessedExamples = 0, curExampleNo = 0;
    protected DiscriminativeParams params;
    private Performance performance;
    private final boolean fastImplementation;
    private final int MAX_SRL_TRIPLES = 15;
    private final int FIRST_SRL_INDEX, FIRST_SRL_POS_INDEX, FEATURE_TEMPLATES_SIZE;
    /**
     * maps that contain the total feature counts extracted from the parser of
     * the oracle model and the model under train
     */
    private HashMap<Feature, Double> oracleFeatures, modelFeatures;
    private int[] oracleFeaturesIds, modelFeaturesIds, tempFeaturesIds;
    private double[] oracleFeaturesValues, modelFeaturesValues, tempFeaturesValues;
    // Some feature templates use a a variable number of features (rather than 1-of-k representation), such as the 
    // srl features (currently two categories, lexicalised and unlexicalised triples). 
    // These variables contain the start and end index in the feature template vector, that corresponds to the actual number of features used in each analysis.
    private int[][] oracleVariableFeaturesStartEndIndex, modelVariableFeaturesStartEndIndex, tempVariableFeaturesStartEndIndex;
    private int[] variableParamVecIds;
    boolean useVariableSizeFeatures;

    public static Map<String, Double>[] semanticsSrlLookuptable;
    public static Map<String, Double[]> semanticsVectorsSrlLookuptable;
    
    private static StanfordCoreNLP pipeline;
    
    public OfflineReranker(Options opts)
    {
        this.opts = opts;
        fastImplementation = !opts.useSlowReranker;
        FIRST_SRL_INDEX = ExtractFeatures.NUM_SYNTACTIC_FEATURES + ExtractFeatures.NUM_GLOBAL_SYNTACTIC_FEATURES + ExtractFeatures.NUM_SEMANTIC_FEATURES;
        FIRST_SRL_POS_INDEX = FIRST_SRL_INDEX + MAX_SRL_TRIPLES;
        FEATURE_TEMPLATES_SIZE = ExtractFeatures.NUM_SYNTACTIC_FEATURES + ExtractFeatures.NUM_GLOBAL_SYNTACTIC_FEATURES
                + ExtractFeatures.NUM_SEMANTIC_FEATURES + (MAX_SRL_TRIPLES * ExtractFeatures.NUM_SRL_FEATURES);
        init();
    }

    private void init()
    {
        if (opts.sentenceLevel && !opts.useSlowReranker)
        {
            throw new UnsupportedOperationException("Sentence level training not supported with fast implementation of the perceptron algorithm");
        }
        // read feature indexers 
        Utils.beginTrack("featureIndexers");
        Utils.logs("Loading incremental analyses features indexers from disk...");       
        if(new File(opts.discriminativeFeatureIndexers).exists()) // directory mode
//            featureIndexers = DiscriminativeFeatureIndexers.loadFeatureIndexers(opts.discriminativeFeatureIndexers);        
            featureIndexers = new DiscriminativeFeatureIndexers(opts.discriminativeFeatureIndexers);
        else
//            featureIndexers = (DiscriminativeFeatureIndexers) IOUtils.readObjFileHard(opts.discriminativeFeatureIndexers + ".obj.gz");        
            featureIndexers = new DiscriminativeFeatureIndexers((DiscriminativeFeatureIndexers) IOUtils.readObjFileHard(opts.discriminativeFeatureIndexers + ".obj.gz"));        
        LogInfo.end_track();
        Utils.beginTrack("loadSrlTripleSemantics");
        if(!opts.semanticsLookupTable.isEmpty() && new File(opts.semanticsLookupTable.get(0)).exists())// &&
//           opts.semanticsLookupTableVectors != null && new File(opts.semanticsLookupTableVectors).exists())
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
//        printSrlTriples();
        
        // read oracle features
        oracleIncrAnalysesFeatures = readFeatures(opts.oracleIncrAnalysesFeaturesPath + ".obj.gz", true, 1, 1);
        // init analyses features for training/testing
        incrAnalysesFeatures = new ExtractFeatures(opts);
        incrAnalysesFeatures.setFeatureIndexers(featureIndexers);
        // initialise parameters
//        params = newParams();
        initParams(opts.initType);
        if (!fastImplementation)
        {
            oracleFeatures = new HashMap<>();
            modelFeatures = new HashMap<>();
        } else
        {
            useVariableSizeFeatures = opts.useSrlFeatures || opts.useSrlPosFeatures;
            oracleFeaturesIds = new int[FEATURE_TEMPLATES_SIZE];
            modelFeaturesIds = new int[FEATURE_TEMPLATES_SIZE];
            tempFeaturesIds = new int[FEATURE_TEMPLATES_SIZE];
            oracleFeaturesValues = new double[FEATURE_TEMPLATES_SIZE];
            modelFeaturesValues = new double[FEATURE_TEMPLATES_SIZE];
            tempFeaturesValues = new double[FEATURE_TEMPLATES_SIZE];
            if (useVariableSizeFeatures)
            {
                int numOfVariableFeatures = ExtractFeatures.NUM_SRL_FEATURES;
                oracleVariableFeaturesStartEndIndex = new int[numOfVariableFeatures][2];
                modelVariableFeaturesStartEndIndex = new int[numOfVariableFeatures][2];
                tempVariableFeaturesStartEndIndex = new int[numOfVariableFeatures][2];
                variableParamVecIds = new int[numOfVariableFeatures];
            }
            cleanFeatureTemplateArrays();
        }
        Utils.logs(Arrays.toString(params.getVecSizesToString()));
    }

    public void initParams(InitType initType)
    {
        Utils.beginTrack("Init parameters: %s", initType);
        switch (initType)
        {
            case random:
                randomInitParams();
                break;
//          case staged : stagedInitParams(); break;
            case uniformz:
                params = newParams();
                break;
            default:
                throw new UnsupportedOperationException("Invalid init type");
        }
        featureIndexers = null;
        //params.output(Execution.getFile("init.params"));     
        LogInfo.end_track();
    }

    /**
     *
     * Read all examples at once and store into a single object in memory
     */
    public void readExamples()
    {
        // read examples            
        String[] paths = getBatchExamplesPaths();
        int i = 1;
        Collection<ReaderWorker> list = new ArrayList(paths.length);
        for (String path : paths)
        {
            list.add(new ReaderWorker(path, false, i++, paths.length, incrAnalysesFeatures));

        }
        Utils.parallelForeach(opts.numThreads, list);
    }

    /**
     *
     * We store incremental analyses in stand-alone batches batches. This method
     * retrieves their filenames in an array of Strings
     *
     * @return
     */
    private String[] getBatchExamplesPaths()
    {
        File f = new File(opts.incrAnalysesFeaturesPath);
        Paths paths = new Paths(f.getParent(), f.getName() + "-*");
        return paths.getPaths().toArray(new String[0]);
    }

    private ExtractFeatures readFeatures(String inputPath, boolean isOracle, int currentFileNo, int total)
    {
        StopWatchSet.begin("readFeatures");
        Utils.beginTrack("readFeatures");
        Utils.logs("Reading " + (isOracle ? "oracle" : "") + " incremental analyses from file " + inputPath + " ... (" + currentFileNo + "/" + total + ")", true);
        ExtractFeatures features = new ExtractFeatures();
        features.loadFeatures(inputPath);
        if (!features.isEmpty())
//        ExtractFeatures features = (ExtractFeatures) IOUtils.readObjFileEasy(inputPath);        
//        if (features != null)        
        {
            features.setFeatureIndexers(featureIndexers);
            Utils.logs("Done (read " + features.size() + " examples)");
        } else
        {
            LogInfo.error("Error loading " + (isOracle ? "oracle" : "") + " incremental analyses object file");
            features = new ExtractFeatures(opts);
        }
        StopWatchSet.end();
        LogInfo.end_track();
        return features;
    }

    public void train()
    {
        // initialise model
        HashMap perceptronSumModel = new HashMap(fastImplementation ? params.getTotalVecSize() : 16);
        HashMap perceptronAverageModel = new HashMap();

        int batchSize;
        boolean cooling = false;
        switch (opts.learningScheme)
        {
            case batch:
                batchSize = incrAnalysesFeatures.size();
                break;
            case stepwise:
                batchSize = opts.miniBatchSize;
                cooling = true;
                break;
            default:
            case incremental:
                batchSize = 1;
        }
        int trainSize = incrAnalysesFeatures.size();
        // percy's cooling
        GradientBasedOptimizer optimizer = fastImplementation
                ? new DefaultPerceptronFast(
                        perceptronSumModel, perceptronAverageModel,
                        trainSize,
                        batchSize,
                        opts.convergePass,
                        opts.stepSizeReductionPower,
                        opts.initTemperature,
                        FEATURE_TEMPLATES_SIZE * 2, opts.tolerance)
                : new DefaultPerceptron(
                        perceptronSumModel, perceptronAverageModel,
                        trainSize,
                        batchSize,
                        opts.convergePass,
                        opts.stepSizeReductionPower,
                        opts.initTemperature, opts.tolerance);
        if (opts.initType == InitType.random)
        {
            optimizer.initModel(params);
        }
        // we need the cooling scheduling in case we do stepwise updating
        if (!cooling)
        {
            optimizer.setNoCooling();
        }

        for (int iter = 0; iter < opts.numIters; iter++) // for t = 1...T do
        {
            FullStatFig complexity = new FullStatFig(); // Complexity inference
            Utils.beginTrack("Iteration %s/%s: ", Utils.fmt(iter + 1),
                    Utils.fmt(opts.numIters));
            Record.begin("iteration", iter + 1);
            performance = newPerformance();
            curExampleNo = 0;
            if (opts.readExamplesInBatches)
            {
                String[] examplePaths = getBatchExamplesPaths();
                int curFileNo = 1;
                for (String path : examplePaths) // for i = 1...N do
                {
                    processExamples(optimizer, readFeatures(path, false, curFileNo++, examplePaths.length), opts.sentenceLevel);
                }
            } else
            {
                processExamples(optimizer, incrAnalysesFeatures, opts.sentenceLevel); // for i = 1...N do
            }
            // purge any unprocessed examples
            if (opts.learningScheme == LearningScheme.stepwise)
            {
                updateOptimizer(true, optimizer);
            }
            // update the internal average model
            if (fastImplementation)
            {
                ((DefaultPerceptronFast) optimizer).forceUpdateAverageModel();
            } else
            {
                ((DefaultPerceptron) optimizer).forceUpdateAverageModel();
            }
            record(String.valueOf(iter), "train", complexity, false);
            LogInfo.end_track();
            Record.end();
            // Final
            if (iter == opts.numIters - 1)
            {
                LogInfo.track("Final", true);
                if (performance != null)
                {
                    performance.record("Final");
                }
                LogInfo.end_track();
            }
            if (Execution.shouldBail())
            {
                opts.numIters = iter;
            }
//            System.out.println("Params Stats: \n" + Arrays.toString(params.getWeightsStats()));
        } // for (all iterations)       
        // use average model weights instead of sum 
        // (reduces overfitting according to Collins, 2002)
        if (fastImplementation)
        {
            ((DefaultPerceptronFast) optimizer).updateParamsWithAvgWeights(params);
        } else
        {
            ((DefaultPerceptron) optimizer).updateParamsWithAvgWeights();
        }
        Utils.logs("Params Stats: \n" + Arrays.toString(params.getWeightsStats()));
        // save parameters
        saveParams("params");
        Utils.logs("Saving incremental analyses features...");        
        params.getFeatureIndexers().saveFlattenedFeatureIndexers(opts.discriminativeFeatureIndexers);
    }

    private void processExamples(GradientBasedOptimizer optimizer, ExtractFeatures predIncrAnalysesFeatures, boolean updatePerExample)
    {
        for (Entry<String, IncrementalAnalyses> example : predIncrAnalysesFeatures.getIncrAnalysesFeaturesSet())
        {
            if (curExampleNo < opts.maxNumOfExamples)
            {
//                System.out.println(example.getKey());
                IncrementalAnalyses pred = example.getValue();
                IncrementalAnalyses oracle = oracleIncrAnalysesFeatures.getIncrAnalysesFeatures(example.getKey());
                if (oracle != null)
                {
                    // compute features for parser and oracle
                    processExample(optimizer, pred, oracle, updatePerExample);
                    numProcessedExamples++;
                    if (updatePerExample) // update perceptron if necessary (batch update)
                    {
                        updateOptimizer(false, optimizer);
                    }
                }
//                System.out.println(curExampleNo + ": " + example.getKey());
                curExampleNo++;
            }
        }
    }

    private void processExample(GradientBasedOptimizer optimizer, IncrementalAnalyses pred, IncrementalAnalyses oracle, boolean updatePerExample)
    {
        int words = oracle.getNbestAnalyses().length;
        if (updatePerExample)
        {
            int nBest = pred.getNbestAnalyses()[0].getIncrAnalyses().length;
            Map<Feature, Double>[] nBestSentencePredFeatures = new HashMap[nBest];
            for (int j = 0; j < nBest; j++)
            {
                nBestSentencePredFeatures[j] = new HashMap<Feature, Double>();
            }
            for (int i = 0; i < words; i++) // for each word
            {
                IncrementalAnalysis oracleWord = oracle.getNbestAnalyses()[i].getIncrAnalyses()[0];
                if (oracleWord != null)
                {
                    oracleFeatures.putAll(computeFeatureCounts(params, oracleWord, (i + 1), 
                            opts.usePrefixBaselineFeature, opts.useWordBaselineFeature,
                            opts.useSyntacticFeatures, opts.useFringeFeatures, 
                            opts.useGlobalSyntacticFeatures, opts.useSemanticsFeatures, opts.semanticsType,
                            opts.useSrlFeatures, opts.useSrlPosFeatures, opts.useLemmatisedSrlFeatures, true));
                    for (int j = 0; j < nBest; j++)
                    {
                        IncrementalAnalysis predWord = pred.getNbestAnalyses()[i].getIncrAnalyses()[j];
                        if (predWord != null)
                        {
                            nBestSentencePredFeatures[j].putAll(computeFeatureCounts(params, predWord, (i + 1),
                                    opts.usePrefixBaselineFeature, opts.useWordBaselineFeature, opts.useSyntacticFeatures,
                                    opts.useFringeFeatures, opts.useGlobalSyntacticFeatures, opts.useSemanticsFeatures, opts.semanticsType,
                                    opts.useSrlFeatures, opts.useSrlPosFeatures, opts.useLemmatisedSrlFeatures, true));
                        }
                    }
                } // if                             
            } // for
            // normalise baseline scores
            if (opts.usePrefixBaselineFeature)
            {
                normaliseCount(new Feature(params.baselineWeight, 0), words, oracleFeatures);
            }
            if (opts.useWordBaselineFeature)
            {
                normaliseCount(new Feature(params.baselineWordWeight, 0), words, oracleFeatures);
            }
            modelFeatures.putAll(argMax(nBestSentencePredFeatures, words));
        } else
        {
            for (int i = 0; i < words; i++) // for each word
            {
                // get single oracle analysis for word i
                IncrementalAnalysis oracleWord = oracle.getNbestAnalyses()[i].getIncrAnalyses()[0];
                if (oracleWord != null)
                {
                    if (fastImplementation)
                    {
                        computeFeatureCounts(oracleWord, (i + 1), oracleFeaturesIds, oracleFeaturesValues, oracleVariableFeaturesStartEndIndex);
                        // find arg,max of predicted
                        argMax(pred.getNbestAnalyses()[i].getIncrAnalyses(), (i + 1), modelFeaturesIds, modelFeaturesValues, modelVariableFeaturesStartEndIndex);
                    } else
                    {
                        oracleFeatures.putAll(computeFeatureCounts(params, oracleWord, (i + 1), 
                                opts.usePrefixBaselineFeature, opts.useWordBaselineFeature,
                                opts.useSyntacticFeatures, opts.useFringeFeatures, opts.useGlobalSyntacticFeatures, 
                                opts.useSemanticsFeatures, opts.semanticsType, opts.useSrlFeatures, opts.useSrlPosFeatures, 
                                opts.useLemmatisedSrlFeatures, true));
                        // find arg,max of predicted
                        modelFeatures.putAll(argMax(pred.getNbestAnalyses()[i].getIncrAnalyses(), (i + 1)));
                    }
                    updateOptimizer(opts.learningScheme == LearningScheme.incremental, optimizer); // force update for every word       
                    //            System.out.println("word " + i + " : " + params.getWeightsStats()[7]);
                }
            }
        }

    }

    private Map<Feature, Double> argMax(IncrementalAnalysis[] analyses, int prefixLength)
    {
        double max = Integer.MIN_VALUE, curScore;
        Map<Feature, Double> maxFeatures = new HashMap<Feature, Double>();
        for (IncrementalAnalysis analysis : analyses)
        {
            if (analysis != null)
            {
                Map<Feature, Double> curFeatures = computeFeatureCounts(params, analysis, prefixLength, 
                        opts.useWordBaselineFeature, opts.useWordBaselineFeature,
                        opts.useSyntacticFeatures, opts.useFringeFeatures, opts.useGlobalSyntacticFeatures, 
                        opts.useSemanticsFeatures, opts.semanticsType, opts.useSrlFeatures, opts.useSrlPosFeatures, 
                        opts.useLemmatisedSrlFeatures, true);
                curScore = params.getModelWeight(curFeatures);
                if (curScore > max)
                {
                    maxFeatures = curFeatures;
                    max = curScore;
                }
            }
        }
        return maxFeatures;
    }

    private Map<Feature, Double> argMax(Map<Feature, Double>[] nBestSentencePredFeatures, int words)
    {
        double max = Integer.MIN_VALUE, curScore;
        Map<Feature, Double> maxFeatures = new HashMap<Feature, Double>();
        for (Map<Feature, Double> sentenceFeatures : nBestSentencePredFeatures)
        {
            if (!sentenceFeatures.isEmpty())
            {
                if (opts.usePrefixBaselineFeature)
                {
                    normaliseCount(new Feature(params.baselineWeight, 0), words, sentenceFeatures);
                }
                if (opts.useWordBaselineFeature)
                {
                    normaliseCount(new Feature(params.baselineWordWeight, 0), words, sentenceFeatures);
                }
                curScore = params.getModelWeight(sentenceFeatures);
                if (curScore > max)
                {
                    maxFeatures = sentenceFeatures;
                    max = curScore;
                }
            }
        }
        return maxFeatures;
    }

    private void argMax(IncrementalAnalysis[] analyses, int prefixLength, int[] featuresIds, double[] featuresValues, int[][] variableFeaturesStartEndIndex)
    {
        double max = Integer.MIN_VALUE, curScore;
        for (IncrementalAnalysis analysis : analyses)
        {
            if (analysis != null)
            {
                computeFeatureCounts(analysis, prefixLength, tempFeaturesIds, tempFeaturesValues, tempVariableFeaturesStartEndIndex);
                curScore = params.getModelWeight(tempFeaturesIds, tempFeaturesValues, FIRST_SRL_INDEX, FIRST_SRL_POS_INDEX,
                        opts.usePrefixBaselineFeature, opts.useWordBaselineFeature, opts.useSyntacticFeatures, opts.useGlobalSyntacticFeatures,
                        opts.useSemanticsFeatures, opts.useSrlFeatures, opts.useSrlPosFeatures);
                if (curScore > max)
                {
                    max = curScore;
                    System.arraycopy(tempFeaturesIds, 0, featuresIds, 0, tempFeaturesIds.length);
                    System.arraycopy(tempFeaturesValues, 0, featuresValues, 0, tempFeaturesValues.length);
                    if (useVariableSizeFeatures)
                    {
                        for (int i = 0; i < tempVariableFeaturesStartEndIndex.length; i++)
                        {
                            System.arraycopy(tempVariableFeaturesStartEndIndex[i], 0, variableFeaturesStartEndIndex[i], 0, tempVariableFeaturesStartEndIndex[0].length);
                        }
                    }
                    cleanSrlTempFeatureTemplateArrays(tempFeaturesIds);
                } else
                {
                    cleanSrlTempFeatureTemplateArrays(tempFeaturesIds);
                }
            }
        }

    }

    public static Map<Feature, Double> computeFeatureCounts(DiscriminativeParams params, IncrementalAnalysis analysis,
            int prefixLength, boolean computePrefixBaseline, boolean computeWordBaseline, boolean computeSyntacticFeatures,
            boolean computeFringeFeatures, boolean computeGlobalSyntacticFeatures,
            boolean computeSemanticsFeatures, SemanticsType semanticsType, 
            boolean computeSrlFeatures, boolean computeSrlPosFeatures, boolean useLemmatizedSrl, boolean train)
    {
        Map<Feature, Double> map = new HashMap<Feature, Double>();
        // normalise by yield size
        if (computePrefixBaseline)
        {
            increaseCount(new Feature(params.baselineWeight, 0), analysis.getBaselineScore() / (double) prefixLength, map);
        }
//            increaseCount(new Feature(params.baselineWeight, 0), Math.abs(analysis.getBaselineScore() / (double) prefixLength), map);
        if (computeWordBaseline)
        {
            increaseCount(new Feature(params.baselineWordWeight, 0), analysis.getBaselineWordScore(), map);
        }
//        if(computeSemanticsFeatures)
//            increaseCount(new Feature(params.semanticWeight, 0), analysis.getSemanticScore(), map);
        if (computeSyntacticFeatures)
        {
            increaseCount(new Feature(params.elemTreeWeights, analysis.getElemTree()), 1, map);
            increaseCount(new Feature(params.prevElemTreeWeights, analysis.getPrevElemTree()), 1, map);
            increaseCount(new Feature(params.elemTreeUnlexWeights, analysis.getElemTreeUnlex()), 1, map);
            increaseCount(new Feature(params.prevElemTreeUnlexWeights, analysis.getPrevElemTreeUnlex()), 1, map);
            increaseCount(new Feature(params.elemTreeBigramWeights, analysis.getElemTreeBigram()), 1, map);
            increaseCount(new Feature(params.elemTreeUnlexBigramWeights, analysis.getElemTreeUnlexBigram()), 1, map);
//            increaseCount(new Feature(params.integrationPointWeights, analysis.getIntegrationPoint()), 1, map);                        
        }
        if(computeFringeFeatures)
        {
            computeFringeFeatures(analysis, params, map, train);
        }
        if (computeGlobalSyntacticFeatures)
        {
            // normalise by yield size
            increaseCount(new Feature(params.rightBranchSpineWeight, 0), analysis.getRightBranchSpine() / (double) prefixLength, map);
            increaseCount(new Feature(params.rightBranchRestWeight, 0), analysis.getRightBranchRest() / (double) prefixLength, map);

            if (analysis.getHeavy() != null)
            {
                for (int heavyId : analysis.getHeavy())
                {
                    increaseCount(new Feature(params.heavyWeights, heavyId), 1, map);
                }
            }
            if (analysis.getNeighboursL1() != null)
            {
                for (int neighbourL1Id : analysis.getNeighboursL1())
                {
                    increaseCount(new Feature(params.neighboursL1Weights, neighbourL1Id), 1, map);
                }
            }
            if (analysis.getNeighboursL2() != null)
            {
                for (int neighbourL2Id : analysis.getNeighboursL2())
                {
                    increaseCount(new Feature(params.neighboursL2Weights, neighbourL2Id), 1, map);
                }
            }
            increaseCount(new Feature(params.coParWeights, analysis.getCoPar()), 1, map);
            increaseCount(new Feature(params.coLenParWeights, analysis.getCoLenPar()), 1, map);
            increaseCount(new Feature(params.ipElemTreeWeights, analysis.getIpElemTree()), 1, map);
            increaseCount(new Feature(params.ipElemTreeUnlexWeights, analysis.getIpElemTreeUnlex()), 1, map);
            increaseCount(new Feature(params.wordL2Weights, analysis.getWordL2()), 1, map);
            increaseCount(new Feature(params.wordL3Weights, analysis.getWordL3()), 1, map);
        }        
        if (computeSemanticsFeatures || computeSrlFeatures)
        {
            computeSrlFeatures(analysis.getSrlTriples(), analysis.getSrlTriplesPos(), 
                    analysis.getSrlIncompleteTriples(), analysis.getArgComplete(), false, useLemmatizedSrl, 
                    computeSrlFeatures, computeSemanticsFeatures, semanticsType, params, map, train);
        }
        if (computeSrlPosFeatures)
        {
            computeSrlFeatures(analysis.getSrlTriplesPos(), analysis.getSrlTriplesPos(), 
                    analysis.getSrlIncompleteTriplesPos(), analysis.getArgComplete(), true, useLemmatizedSrl, 
                    computeSrlFeatures, computeSemanticsFeatures, semanticsType, params, map, train);
        }
        return map;
    }

    private static void computeFringeFeatures(IncrementalAnalysis analysis, DiscriminativeParams params, Map<Feature, Double> countMap, boolean train)
    {
        DiscriminativeFeatureIndexers featureIndexers = params.getFeatureIndexers();
        // order lists to avoid sparsity: e.g., [NP VP SBAR] and [VP NP SBAR] are collapsed to be represented by the same feature. 
        // Note that the sorting order is NOT lexicographic, but is based on ids which are unique for each category.
        Arrays.sort(analysis.getFringeOpenRight());
        Arrays.sort(analysis.getFringeOpenLeft());
        int openRightId = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_FRINGE, Arrays.toString(analysis.getFringeOpenRight()), train);
        int openLeftId = featureIndexers.getIndexOfFeature(ExtractFeatures.FEAT_FRINGE, Arrays.toString(analysis.getFringeOpenLeft()), train);
        increaseCount(new Feature(params.fringeOpenRightWeights, openRightId), 1, countMap);
        increaseCount(new Feature(params.fringeOpenLeftWeights, openLeftId), 1, countMap);
        increaseCount(new Feature(params.fringeSubstNodeWeight, analysis.getFringeSubstNode()), 1, countMap);
        // each non-terminal is counted separately
        for(int nodeId : analysis.getFringeOpenRight())
        {
            if(nodeId > featureIndexers.getNumOfCategories())
                System.out.println(nodeId);
            increaseCount(new Feature(params.fringeCategoryCountsWeights, nodeId), 1, countMap);
        }
        for(int nodeId : analysis.getFringeOpenLeft())
        {
            if(nodeId > featureIndexers.getNumOfCategories())
                System.out.println(nodeId);
            increaseCount(new Feature(params.fringeCategoryCountsWeights, nodeId), 1, countMap);
        }
        increaseCount(new Feature(params.fringeCategoryCountsWeights, analysis.getFringeSubstNode()), 1, countMap);
        
        increaseCount(new Feature(params.fringeNumNodesWeight, 0), analysis.getNumFringeNodes(), countMap);
        increaseCount(new Feature(params.fringeNumPredictNodesWeight, 0), analysis.getNumPredictFringeNodes(), countMap);
    }
    
    private static void computeSrlFeatures(int[] srlTriples, int[] srlTriplePos, int[] srlIncompleteTriples, BitSet argComplete, 
            boolean unlex, boolean lemmatize, boolean computeSrlFeatures, boolean computeSemantics, 
            SemanticsType semanticsType, DiscriminativeParams params, Map<Feature, Double> countMap, boolean train)
    {
        DiscriminativeFeatureIndexers featureIndexers = params.getFeatureIndexers();
//        int a0Index = -1, a1Index = -1, a2Index = -1;
//        boolean containsModifier = false;        
        double semanticsScore = 0.0d;
        double[] semanticsScoreVec;
        if(computeSemantics && semanticsType == Options.SemanticsType.quantum)
        {
            semanticsScoreVec = new double[ExtractFeatures.NUM_SEMANTIC_VEC_SIZE];
        }
        else if(computeSemantics && semanticsType == Options.SemanticsType.all)
        {
            semanticsScoreVec = new double[ExtractFeatures.NUM_SEMANTIC_VEC_SIZE + 2]; // William's semantics + DM + NN
        }
        else
        {
            semanticsScoreVec = null;
        }
        int tripleCount = 0;
        for (int srlId : srlTriples)
        {
            int[] srlParts = srlTripleDecompose(srlId, unlex, featureIndexers);
            if(srlParts.length == 1) // unknown triple
            {
                if(computeSrlFeatures)
                    increaseCount(new Feature(unlex ? params.srlTriplesPosWeights : params.srlTriplesWeights, srlId), 1, countMap);
                // TO-DO: call model of semantics to produce scores on-the-fly
            }            
            // compute rest SRL features on-the-fly            
            else if(srlParts.length > 1)
            {
                int roleId = srlParts[0];
                int rawArgId = lemmatize && !unlex ? lookupWordIdLemmatized(srlParts[1], featureIndexers) : srlParts[1];
                int rawPredId = lemmatize && !unlex ? lookupWordIdLemmatized(srlParts[2], featureIndexers) : srlParts[2];
                if(computeSrlFeatures)
                {
                    increaseCount(new Feature(unlex ? params.srlTriplesPosWeights : params.srlTriplesWeights, srlId), 1, countMap);
                    // role
                    increaseCount(new Feature(params.srlRoleWeights, roleId), 1, countMap); // role
                    // lexical dependency
                    int lexDepId = featureIndexers.getIndexOfFeature(unlex ? ExtractFeatures.FEAT_SRL_DEPENDENCY_POS : ExtractFeatures.FEAT_SRL_DEPENDENCY,
                            String.format("%s,%s", rawArgId, rawPredId), train);
                    increaseCount(new Feature(unlex ? params.srlDependencyPosWeights : params.srlDependencyWeights, lexDepId), 1, countMap);
                    // role and predicate only
                    int rolePredId = featureIndexers.getIndexOfFeature(unlex ? ExtractFeatures.FEAT_SRL_ROLE_PRED_POS : ExtractFeatures.FEAT_SRL_ROLE_PRED, 
                            String.format("%s,%s", roleId, rawPredId), train);
                    increaseCount(new Feature(unlex ? params.srlRolePredPosWeights : params.srlRolePredWeights, rolePredId), 1, countMap);
                    // role and argument only
                    int roleArgId = featureIndexers.getIndexOfFeature(unlex ? ExtractFeatures.FEAT_SRL_ROLE_ARG_POS : ExtractFeatures.FEAT_SRL_ROLE_ARG, 
                            String.format("%s,%s", roleId, rawArgId), train);
                    increaseCount(new Feature(unlex ? params.srlRoleArgPosWeights : params.srlRoleArgWeights, roleArgId), 1, countMap);
                    // predicate only
                    int predFeatId = featureIndexers.getIndexOfFeature(unlex ? ExtractFeatures.FEAT_SRL_PRED_POS : ExtractFeatures.FEAT_SRL_PRED, 
                            String.valueOf(rawPredId), train);
                    increaseCount(new Feature(unlex ? params.srlPredPosWeights : params.srlPredWeights, predFeatId), 1, countMap);
                    // argument only
                    int argFeatId = featureIndexers.getIndexOfFeature(unlex ? ExtractFeatures.FEAT_SRL_ARG_POS : ExtractFeatures.FEAT_SRL_ARG, 
                            String.valueOf(srlParts[1]), train);
                    increaseCount(new Feature(unlex ? params.srlArgPosWeights : params.srlArgWeights, argFeatId), 1, countMap);
                }                
                String role = featureIndexers.getRole(roleId);
//                if (role.equals("A0"))
//                {
//                    a0Index = srlParts[1];
//                } else if (role.equals("A1"))
//                {
//                    a1Index = srlParts[1];
//                } else if (role.equals("A2"))
//                {
//                    a2Index = srlParts[1];
//                } else if (role.startsWith("AM"))
//                {
//                    containsModifier = true;
//                }
                if(!unlex && computeSemantics)// && admissableRoles(role)) // compute semantics score on lemmatized SRL triple
                {
                    if(semanticsType == SemanticsType.quantum)
                    {
                        Double[] lookupScore = lookupSrlTripleSemanticsVec(
                                semanticsVectorsSrlLookuptable, role, featureIndexers.getWord(srlParts[1]), 
                                featureIndexers.getWord(srlParts[2]), false);
                        if(lookupScore != null)
                        {
                            for(int i = 0; i < lookupScore.length; i++)
                                semanticsScoreVec[i] += lookupScore[i];
                        }
                    } // quantum
                    else if(semanticsType == SemanticsType.dm)
                    {
                        String[] srlPartsPos = featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES_POS, srlTriplePos[tripleCount]).split(",");
                        if(srlPartsPos.length > 1)
                        {
                            String queryTriple = String.format("<%s,%s/%s,%s/%s>:%s", 
                                featureIndexers.getRole(Integer.valueOf(srlParts[0])), // role
                                featureIndexers.getWord(Integer.valueOf(srlParts[1])), featureIndexers.getPos(Integer.valueOf(srlPartsPos[1])), // arg
                                featureIndexers.getWord(Integer.valueOf(srlParts[2])), featureIndexers.getPos(Integer.valueOf(srlPartsPos[2])),  // pred
                                argComplete.get(tripleCount));
                            tripleCount++;
                            Double lookupScore = semanticsSrlLookuptable[0].get(queryTriple);
                            if(lookupScore != null)
                                semanticsScore += lookupScore;
                        }                        
                    } // dm
                    else if(semanticsType == SemanticsType.nn)
                    {
                        String queryTriple = String.format("<%s,%s,%s>:%s", 
                                featureIndexers.getRole(Integer.valueOf(srlParts[0])), // role
                                lookupWordLemmatized(srlParts[1], featureIndexers), // arg
                                lookupWordLemmatized(srlParts[2], featureIndexers), // pred
                                argComplete.get(tripleCount));
                            tripleCount++;
                            Double lookupScore = semanticsSrlLookuptable[0].get(queryTriple.toLowerCase());
                            if(lookupScore != null)
                                semanticsScore += lookupScore;
                    } // nn
                    else if(semanticsType == SemanticsType.all)
                    {
                        // update quantum scores
                        Double[] lookupScore = lookupSrlTripleSemanticsVec(
                                semanticsVectorsSrlLookuptable, role, featureIndexers.getWord(srlParts[1]), 
                                featureIndexers.getWord(srlParts[2]), false);
                        if(lookupScore != null)
                        {
                            for(int i = 0; i < lookupScore.length; i++)
                                semanticsScoreVec[i] += lookupScore[i];
                        }
                        // update DM score
                        String[] srlPartsPos = featureIndexers.getValueOfFeature(ExtractFeatures.FEAT_SRL_TRIPLES_POS, srlTriplePos[tripleCount]).split(",");
                        if(srlPartsPos.length > 1)
                        {
                            String queryTriple = String.format("<%s,%s/%s,%s/%s>:%s", 
                                featureIndexers.getRole(Integer.valueOf(srlParts[0])), // role
                                featureIndexers.getWord(Integer.valueOf(srlParts[1])), featureIndexers.getPos(Integer.valueOf(srlPartsPos[1])), // arg
                                featureIndexers.getWord(Integer.valueOf(srlParts[2])), featureIndexers.getPos(Integer.valueOf(srlPartsPos[2])),  // pred
                                argComplete.get(tripleCount));                            
                            Double lookupScoreDm = semanticsSrlLookuptable[0].get(queryTriple);
                            if(lookupScoreDm != null)
                                semanticsScoreVec[ExtractFeatures.NUM_SEMANTIC_VEC_SIZE] += lookupScoreDm;
                        } // if
                        // update NN score
                        String queryTriple = String.format("<%s,%s,%s>:%s", 
                                featureIndexers.getRole(Integer.valueOf(srlParts[0])), // role
                                lookupWordLemmatized(srlParts[1], featureIndexers), // arg
                                lookupWordLemmatized(srlParts[2], featureIndexers), // pred
                                argComplete.get(tripleCount));
                        tripleCount++;
                        Double lookupScoreNn = semanticsSrlLookuptable[1].get(queryTriple.toLowerCase());
                        if(lookupScoreNn != null)
                            semanticsScoreVec[ExtractFeatures.NUM_SEMANTIC_VEC_SIZE + 1] += lookupScoreNn;
                    } // all
                } // if computing semantics
            } // if we identified an SRL triple            
//            index++;
        } // for
        if(srlTriples.length > 0)
        {
            // frame: [A0 triple]?, [A1 triple]?, [A2 triple]?, [modifier(s) boolean]?
//            int frameId = featureIndexers.getIndexOfFeature(unlex ? ExtractFeatures.FEAT_SRL_FRAME_POS : ExtractFeatures.FEAT_SRL_FRAME,
//                    String.format("%s %s %s %s", a0Index, a1Index, a2Index, containsModifier ? "y" : "n"), train);
//            increaseCount(new Feature(unlex ? params.srlFramePosWeights : params.srlFrameWeights, frameId), 1, countMap);
            if(computeSemantics)
            {
                if(semanticsType == SemanticsType.quantum || semanticsType == SemanticsType.all)
                {
                    for(int i = 0; i < semanticsScoreVec.length; i++)
                    {
                        increaseCount(new Feature(params.semanticVecWeights[i], 0), semanticsScoreVec[i] / (double) srlTriples.length, countMap);
                    }
                } // if
                else 
                    increaseCount(new Feature(params.semanticWeight, 0), semanticsScore / (double) srlTriples.length, countMap);
            } // if      
//                
        }
        
//        for(int srlIncompleteId : srlIncompleteTriples)
//        {            
//            increaseCount(new Feature(unlex ? params.srlIncompleteTriplesWeights : params.srlIncompleteTriplesPosWeights, srlIncompleteId), 1, countMap);            
//        }
    }

    private static int[] srlTripleDecompose(int srlTripleInt, boolean unlex, DiscriminativeFeatureIndexers featureIndexers)
    {
        String input = featureIndexers.getValueOfFeature(unlex ? ExtractFeatures.FEAT_SRL_TRIPLES_POS : ExtractFeatures.FEAT_SRL_TRIPLES, srlTripleInt);
        if (input.equals("0") || input.equals("U"))
        {
            return new int[]
            {
                0
            };
        }
        int[] out = new int[3];
        String[] ar = input.split(",");
        out[0] = Integer.valueOf(ar[0]);
        out[1] = Integer.valueOf(ar[1]);
        out[2] = Integer.valueOf(ar[2]);
        return out;
    }        

    private void computeFeatureCounts(IncrementalAnalysis analysis, int prefixLength,
            int[] featureIds, double[] featureValues, int[][] variableFeaturesStartEndIndex)
    {
        if (opts.usePrefixBaselineFeature)
        {
            increaseCount(ExtractFeatures.FEAT_BASELINE_SCORE, 0, analysis.getBaselineScore() / (double) prefixLength, featureIds, featureValues);
//            increaseCount(ExtractFeatures.FEAT_BASELINE_SCORE, 0, Math.abs(analysis.getBaselineScore() / (double) prefixLength), featureIds, featureValues);  
        }
        if (opts.useWordBaselineFeature)
        {
            increaseCount(ExtractFeatures.FEAT_BASELINE_WORD_SCORE, 0, analysis.getBaselineWordScore(), featureIds, featureValues);
        }
//        if(opts.useSemanticsFeatures)
//        {
//            increaseCount(ExtractFeatures.FEAT_SEMANTIC_SCORE, 0, analysis.getSemanticScore(), featureIds, featureValues);            
//        }
        if (opts.useSyntacticFeatures)
        {
            increaseCount(ExtractFeatures.FEAT_ELEM_TREE, analysis.getElemTree(), 1, featureIds, featureValues);
            increaseCount(ExtractFeatures.FEAT_PREV_ELEM_TREE, analysis.getPrevElemTree(), 1, featureIds, featureValues);
            increaseCount(ExtractFeatures.FEAT_ELEM_TREE_UNLEX, analysis.getElemTreeUnlex(), 1, featureIds, featureValues);
            increaseCount(ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX, analysis.getPrevElemTreeUnlex(), 1, featureIds, featureValues);
            increaseCount(ExtractFeatures.FEAT_ELEM_TREE_BIGRAM, analysis.getElemTreeBigram(), 1, featureIds, featureValues);
            increaseCount(ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM, analysis.getElemTreeUnlexBigram(), 1, featureIds, featureValues);
//            increaseCount(ExtractFeatures.FEAT_INTEGRATION_POINT, analysis.getIntegrationPoint(), 1, featureIds, featureValues);            
        }
        if (opts.useSrlFeatures)
        {
            int[] ar = analysis.getSrlTriples();
            variableFeaturesStartEndIndex[0][0] = FIRST_SRL_INDEX;
            variableFeaturesStartEndIndex[0][1] = FIRST_SRL_INDEX + ar.length;
            variableParamVecIds[0] = ExtractFeatures.FEAT_SRL_TRIPLES;
            if (ar.length == 1)
            {
                increaseCount(FIRST_SRL_INDEX, ar[0], 1, featureIds, featureValues);
            } else if (ar.length > 1)
            {
                Arrays.sort(ar);
                variableFeaturesStartEndIndex[0][1] = FIRST_SRL_INDEX + increaseMultipleCounts(ar, FIRST_SRL_INDEX, featureIds, featureValues);
            }
        }
        if (opts.useSrlPosFeatures)
        {
            int[] ar = analysis.getSrlTriplesPos();
            if (!opts.useSrlFeatures)
            {
                variableFeaturesStartEndIndex[0][0] = variableFeaturesStartEndIndex[0][1] = FIRST_SRL_INDEX;
//                variableParamVecIds[0] = ExtractFeatures.FEAT_SRL_TRIPLES;
            }
            variableFeaturesStartEndIndex[1][0] = FIRST_SRL_POS_INDEX;
            variableFeaturesStartEndIndex[1][1] = FIRST_SRL_POS_INDEX + ar.length;
            variableParamVecIds[1] = ExtractFeatures.FEAT_SRL_TRIPLES_POS;
            if (ar.length == 1)
            {
                increaseCount(FIRST_SRL_POS_INDEX, ar[0], 1, featureIds, featureValues);
            } else if (ar.length > 1)
            {
                Arrays.sort(ar);
                variableFeaturesStartEndIndex[1][1] = FIRST_SRL_POS_INDEX + increaseMultipleCounts(ar, FIRST_SRL_POS_INDEX, featureIds, featureValues);
            }
        }
    }

    protected static void increaseCount(Feature feat, double increment, Map<Feature, Double> features)
    {
        if (feat.getIndex() >= 0)
        {
            Double oldCount = features.get(feat);
            if (oldCount != null)
            {
                features.put(feat, oldCount + increment);
            } else
            {
                features.put(feat, increment);
            }
        }
    }

    private void increaseCount(int featTemplateId, int vecPosition, double increment, final int[] featureIds, final double[] featureValues)
    {
        featureIds[featTemplateId] = vecPosition;
        featureValues[featTemplateId] = increment;
    }

    public int increaseMultipleCounts(int[] vecIds, int startPosition, final int[] featureIds, final double[] featureValues)
    {
        int prevValue = -1, storePos = startPosition, uniqueIdsCount = 0;
        for (int i = 0; i < vecIds.length; i++)
        {
            if (prevValue == vecIds[i])
            {
                increaseCount(storePos - 1, vecIds[i], featureValues[storePos - 1] + 1, featureIds, featureValues);
            } else
            {
                increaseCount(storePos++, vecIds[i], 1, featureIds, featureValues);
                uniqueIdsCount++;
            }
            prevValue = vecIds[i];
        }
        return uniqueIdsCount;
    }

    public static double computeModelScore(DiscriminativeParams params, IncrementalAnalysis analysis,
            int sentenceLength, boolean computePrefixBaseline, boolean computeWordBaseline,
            boolean computeSyntacticFeatures, boolean computeFringeFeatures, boolean computeGlobalSyntacticFeatures,
            boolean computeSemanticsFeatures, SemanticsType semanticsType, boolean computeSrlFeatures, 
            boolean computeSrlPosFeatures, boolean useLemmatizedSrl)
    {
        double res = 0.0;
        for (Entry<Feature, Double> feature : computeFeatureCounts(params, analysis, sentenceLength, computePrefixBaseline,
                computeWordBaseline, computeSyntacticFeatures, computeFringeFeatures, computeGlobalSyntacticFeatures,
                computeSemanticsFeatures, semanticsType, computeSrlFeatures, computeSrlPosFeatures, useLemmatizedSrl, true).entrySet())
        {
            Feature feat = feature.getKey();
            double count = feature.getValue();
            res += feat.getValue() * count; // model_weight * f(x)
        }
        return res;
    }

    protected static void normaliseCount(Feature feat, int words, Map<Feature, Double> features)
    {
        Double oldCount = features.get(feat);
        features.put(feat, oldCount / (double) words);
    }

    private void updateOptimizer(boolean forceUpdate, GradientBasedOptimizer optimizer)
    {
        if (forceUpdate || numProcessedExamples >= optimizer.getBatchSize())
        {
            if (!fastImplementation)
            {
                optimizer.updateModel(oracleFeatures, modelFeatures);
                oracleFeatures.clear();
                modelFeatures.clear();
            } else
            {
                ((DefaultPerceptronFast) optimizer).updateModel(oracleFeaturesIds, oracleFeaturesValues, modelFeaturesIds, modelFeaturesValues,
                        useVariableSizeFeatures, oracleVariableFeaturesStartEndIndex, modelVariableFeaturesStartEndIndex, params, variableParamVecIds);
//                cleanSrlFeatureTemplateArrays();
            }
            numProcessedExamples = 0;
            synchronized (performance)
            {
                ((DiscriminativePerformance) performance).add(optimizer.getGradientNorm());
            }
        }
    }

    protected void saveParams(String name)
    {
        try
        {
            ObjectOutputStream oos = IOUtils.openObjOut(Execution.getFile(name + ".discriminative.params.obj.gz"));
            oos.writeObject(params.getVecs());
            oos.close();
        } catch (IOException ex)
        {
            Utils.log(ex.getMessage());
            ex.printStackTrace(LogInfo.stderr);
//            ex.printStackTrace();
        }
    }

    protected DiscriminativeParams newParams()
    {
        return new DiscriminativeParams(opts, featureIndexers, VecFactory.Type.DENSE);
    }

    protected void randomInitParams()
    {
        params = newParams();
        // initialise randomly all parameters
//        params.randomise(opts.initRandom, opts.initNoise);
        params.randomiseUniformBounded(opts.initRandom, -1.0, 1.0, opts.initNoise);
        //params.optimise(opts.initSmoothing);
    }

    // ext specifies the iteration or example number
    // Use the given params (which are actually counts so we can evaluate even in batch EM)
    protected void record(String ext, String name, FullStatFig complexity, boolean outputPerformance)
    {
//        Utils.logs("Inference complexity: %s", complexity);
        Utils.logs(performance.output());
//        if (!(performance == null || performance.isEmpty()))
        {
            performance.record("train");
            if (outputPerformance)
            {
                performance.output(Execution.getFile(name + ".train.performance." + ext));
            }
        }
    }

    private void cleanFeatureTemplateArrays()
    {
        Arrays.fill(oracleFeaturesIds, Integer.MIN_VALUE);
        Arrays.fill(modelFeaturesIds, Integer.MIN_VALUE);
        Arrays.fill(tempFeaturesIds, Integer.MIN_VALUE);
        if (useVariableSizeFeatures)
        {
            for (int[] ar : oracleVariableFeaturesStartEndIndex) // TODO: check if null. Check if it holds in Perceptron code
            {
                Arrays.fill(ar, Integer.MIN_VALUE);
            }
            for (int[] ar : modelVariableFeaturesStartEndIndex)
            {
                Arrays.fill(ar, Integer.MIN_VALUE);
            }
            for (int[] ar : tempVariableFeaturesStartEndIndex)
            {
                Arrays.fill(ar, Integer.MIN_VALUE);
            }
        }
    }

    private void cleanSrlFeatureTemplateArrays()
    {
        Arrays.fill(oracleFeaturesIds, FIRST_SRL_INDEX, FEATURE_TEMPLATES_SIZE, Integer.MIN_VALUE);
        Arrays.fill(modelFeaturesIds, FIRST_SRL_INDEX, FEATURE_TEMPLATES_SIZE, Integer.MIN_VALUE);
        Arrays.fill(tempFeaturesIds, FIRST_SRL_INDEX, FEATURE_TEMPLATES_SIZE, Integer.MIN_VALUE);
        if (useVariableSizeFeatures)
        {
            for (int[] ar : oracleVariableFeaturesStartEndIndex)
            {
                Arrays.fill(ar, Integer.MIN_VALUE);
            }
            for (int[] ar : modelVariableFeaturesStartEndIndex)
            {
                Arrays.fill(ar, Integer.MIN_VALUE);
            }
            for (int[] ar : tempVariableFeaturesStartEndIndex)
            {
                Arrays.fill(ar, Integer.MIN_VALUE);
            }
        }
    }

    private void cleanSrlTempFeatureTemplateArrays(int[] featureIds)
    {
//        Arrays.fill(featureIds, FIRST_SRL_INDEX, FEATURE_TEMPLATES_SIZE, Integer.MIN_VALUE);
        Arrays.fill(featureIds, Integer.MIN_VALUE);
    }

    private void srlTriplesLemmatisedToString()
    {
        if(pipeline == null)
            initLemmatiser();
        for(String[] srlTriple : featureIndexers.srlTriplesArraysArray())
        {
            if(srlTriple.length > 1)
            {
                StringBuilder str = new StringBuilder(String.format("<%s", srlTriple[0]));
                for(int i = 1; i < srlTriple.length; i++)
                {
                    
                    Annotation document = new Annotation(srlTriple[i]);    
                    // run all Annotators on this text
                    pipeline.annotate(document);
                    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
                    for(CoreMap sentence: sentences) {
                      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                        String tok = token.get(LemmaAnnotation.class);            
                        str.append(",").append(tok);
                      }
                    }
                }
                str.append(">");
                System.out.println(str);
            }            
        }
    }
    
    public static void initLemmatiser()
    {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);
    }
    
    public static String lemmatise(String input)
    {
        Annotation document = new Annotation(input);
        pipeline.annotate(document);
        return document.get(SentencesAnnotation.class).get(0).get(TokensAnnotation.class).get(0).get(LemmaAnnotation.class);
    }
    
    /**
     * Read SRL triples and vectors
     * @param triplesFilename
     * @param vectorsFilename
     * @return 
     */
    public static Map<String, Double[]> createSemanticsLookupTable(String triplesFilename, String vectorsFilename)
    {
        Map<String, Double[]> map = new HashMap<>();
        Map<Integer, String> srlTriples = readSrlTriples(triplesFilename);
        // read vectors
        for(String line : Utils.readLines(vectorsFilename))
        {
            if(!line.startsWith("@"))
            {
                String[] tokens = line.split(" ");
                Integer id = Integer.parseInt(tokens[0]);
                String triple = srlTriples.get(id);
                if(triple != null)
                {
                    Double[] vector = new Double[tokens.length - 1];
                    for(int i = 1; i < tokens.length; i++)
                    {
                        vector[i - 1] = Double.parseDouble(tokens[i]);
                    }
                    map.put(triple, vector);
                }
            }            
        }
        return map;
    }
    
    public static Map<String, Double> createSemanticsLookupTable(String triplesFilename)
    {
        Map<String, Double> map = new HashMap<>();
        for(String line : Utils.readLines(triplesFilename))
        {
            String[] tokens = line.split("\t");
            if(tokens.length == 9)
            {
                String triple = tokens[8];
                Double score = tokens[4].equals("null") ? null : Double.parseDouble(tokens[4]);
                if(score != null)
                    map.put(triple, score);
            }            
        }
        return map;
    }
    
    public static Map<String, Double> createSemanticsNNLookupTable(String triplesFilename)
    {
        Map<String, Double> map = new HashMap<>();
        for(String line : Utils.readLines(triplesFilename))
        {
            String[] tokens = line.split("[|]");
            if(tokens.length == 3)
            {
                String triple = tokens[0];
                Double score = Double.parseDouble(tokens[1]);
                map.put(triple, score);
            }            
        }
        return map;
    }
    
    public static Map<String, Double> createSemanticsDMLookupTable(String triplesFilename)
    {
        Map<String, Double> map = new HashMap<>();
        for(String line : Utils.readLines(triplesFilename))
        {
            String[] tokens = line.split("\t");
            if(tokens.length == 6)
            {
                String triple = tokens[4];
                Double score = Double.parseDouble(tokens[5]);
                map.put(triple, score);
            }            
        }
        return map;
    }
    
    public static Map<String, Double>[] createSemanticsAllLookupTable(List<String> triplesFilename, String vectorsFilename, Map<String, Double[]> semanticsVectorsSrlLookuptable)
    {
        Map<String, Double> map[] = new HashMap[2];
        map[0] = createSemanticsDMLookupTable(triplesFilename.get(0));
        map[1] = createSemanticsNNLookupTable(triplesFilename.get(1));        
        semanticsVectorsSrlLookuptable.putAll(createSemanticsLookupTable(triplesFilename.get(2), vectorsFilename));
        return map;
    }
    
    private static Map<Integer, String> readSrlTriples(String filename)
    {
        Map<Integer, String> map = new HashMap<>();
        for(String line : Utils.readLines(filename))
        {
            String[] tokens = line.split("\t");
            if(tokens.length == 9 && !tokens[4].equals("null")) // each line contains the srl triple with calculated score
            {
                map.put(Integer.parseInt(tokens[0]), tokens[8]);                
            }   
            else if(tokens.length == 4 && !tokens[0].equals("null")) // each line contains the srl triple only
            {
                map.put(Integer.parseInt(tokens[0]), String.format("<%s,%s,%s>", tokens[1].substring(3), tokens[2], tokens[3]));
            }
        }
        return map;
    }
    
    public static Map<String, Double> createSemanticsLemmatizedLookupTable(String filename)
    {
        Map<String, Double> map = new HashMap<>();
        for(String line : Utils.readLines(filename))
        {
            String[] tokens = line.split("\t");
            if(tokens.length == 5)
            {
                String role = tokens[1].substring(3);
                String arg = tokens[2];
                String pred = tokens[3];
                Double score = tokens[4].equals("null") ? null : Double.parseDouble(tokens[4]);
                if(score != null)
                    map.put(String.format("%s %s %s", role, arg, pred), score);
            }            
        }
        return map;
    }
    
    public static Double lookupSrlTripleSemantics(Map<String, Double> lookupTable, String role, String arg, String pred, boolean lemmatise)
    {
        if(pipeline == null && lemmatise)
            initLemmatiser();
        if(arg.contains("NUM"))
            arg = "@card@";        
        return lemmatise ? lookupTable.get(String.format("%s %s %s", role, lemmatise(arg), lemmatise(pred))) : 
                lookupTable.get(String.format("<%s,%s,%s>", role, arg, pred));
//        return lookupTable.get(String.format("%s %s %s", role, arg.toLowerCase(), pred.toLowerCase()));
    }
    
    public static Double[] lookupSrlTripleSemanticsVec(Map<String, Double[]> lookupTable, String role, String arg, String pred, boolean lemmatise)
    {
        if(pipeline == null && lemmatise)
            initLemmatiser();
        if(arg.contains("NUM"))
            arg = "@card@";        
        return lemmatise ? lookupTable.get(String.format("%s %s %s", role, lemmatise(arg), lemmatise(pred))) : 
                lookupTable.get(String.format("<%s,%s,%s>", role, arg, pred));
//        return lookupTable.get(String.format("%s %s %s", role, arg.toLowerCase(), pred.toLowerCase()));
    }
    
    public static int lookupWordIdLemmatized(int wordId, DiscriminativeFeatureIndexers featureIndexers)
    {
        if(pipeline == null)
            initLemmatiser();
        Map<Integer, Integer> word2LemmaMap = featureIndexers.getWord2LemmaMap();
        Integer lemmaId = word2LemmaMap.get(wordId); // check whether we've already lemmatized the word
        if(lemmaId == null)
        {
            String word = featureIndexers.getWord(wordId);
            lemmaId = featureIndexers.getWordLemmatizedIndex(lemmatise(word));
            word2LemmaMap.put(wordId, lemmaId);
        }
        return lemmaId;
    }
    
    public static String lookupWordLemmatized(int wordId, DiscriminativeFeatureIndexers featureIndexers)
    {
        if(pipeline == null)
            initLemmatiser();
        Map<Integer, Integer> word2LemmaMap = featureIndexers.getWord2LemmaMap();
        Integer lemmaId = word2LemmaMap.get(wordId); // check whether we've already lemmatized the word
        if(lemmaId == null)
        {
            String word = featureIndexers.getWord(wordId);
            String lemma = lemmatise(word);
            lemmaId = featureIndexers.getWordLemmatizedIndex(lemma);
            word2LemmaMap.put(wordId, lemmaId);
            return lemma;
            
        }
        else
        {
            return featureIndexers.getWordLemmatized(lemmaId);
        }        
    }
    
    /**
     * we currently accept only roles A0-A5, which conveniently have only 2 characters
     * @param role
     * @return 
     */
    private static boolean admissableRoles(String role)
    {
        return role.length() == 2;
    }
    
    public void testTrain()
    {
        train();
    }

    protected Performance newPerformance()
    {
        return new DiscriminativePerformance();
    }

    private void printSrlTriples()
    {
        Set<String> lemmatisedSrlTriples = new HashSet<>();
        initLemmatiser();
        for(String triple : featureIndexers.srlTriplesStringArray())
        {
            if(!triple.equals("U"))
            {
                String[] tokens = triple.split(",");
                if(tokens.length == 3)
                    lemmatisedSrlTriples.add(String.format("%s,%s,%s>", tokens[0], lemmatise(tokens[1]), lemmatise(tokens[2].substring(0, tokens[2].lastIndexOf(">")))));                
            }            
        }       
        for(String s : lemmatisedSrlTriples)
            System.out.println(s);
        System.exit(0);

    }

    public void printFullSrlTriples(String path)
    {
        final Set<String> lemmatisedSrlTriples = new HashSet<>();
        initLemmatiser();
        lemmatisedSrlTriples.addAll(getFullSrlTriples(oracleIncrAnalysesFeatures));
        lemmatisedSrlTriples.addAll(getFullSrlTriples(incrAnalysesFeatures));
        Utils.writeLines(path, lemmatisedSrlTriples.toArray(new String[0]));
    }
    
    private Set<String> getFullSrlTriples(ExtractFeatures analysesFeatures)
    {
        Set<String> lemmatisedSrlTriples = new HashSet<>();
        for(Entry<String, IncrementalAnalyses> analyses : analysesFeatures.getIncrAnalysesFeaturesSet())
        {
            for(NbestIncrementalAnalyses nbest : analyses.getValue().getNbestAnalyses())
            {
                for(IncrementalAnalysis analysis : nbest.getIncrAnalyses())
                {
                    if(analysis != null)
                        lemmatisedSrlTriples.addAll(Arrays.asList(analysis.srlTriplesFull(analysesFeatures.getFeatureIndexers())));
                }
            }
        }
        return lemmatisedSrlTriples;
    }
    
    class ReaderWorker extends CallableWithLog
    {

        String inputPath;
        boolean isOracle;
        int currentFileNo;
        int total;
        final ExtractFeatures incrAnalysesFeatures;

        public ReaderWorker(String inputPath, boolean isOracle, int currentFileNo, int total, ExtractFeatures incrAnalysesFeatures)
        {
            this.inputPath = inputPath;
            this.isOracle = isOracle;
            this.currentFileNo = currentFileNo;
            this.total = total;
            this.incrAnalysesFeatures = incrAnalysesFeatures;
        }

        @Override
        public Object call() throws Exception
        {
            synchronized (incrAnalysesFeatures)
            {
                incrAnalysesFeatures.addAll(readFeatures(inputPath, isOracle, currentFileNo, total));
            }
            return null;
        }

    }
}
