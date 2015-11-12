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

import fig.basic.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author sinantie
 */
public class Options
{
    public enum ExampleType {path, singleFile, serialized};
    public enum SemanticsModelType {oracle, oracleAllRoles, parsedAllRoles};
    public enum BaselineSemanticsModel {goldIdentification, heuristicsIdentification, directArcsIdentification, maltParser};
    public enum InputType {plain, posTagged, pltag, dundee}
    public enum InitType { random, uniformz, staged };
    public enum ParserType {generative, discriminative, oracle, discriminativeOracle}
    public enum SemanticsType {quantum, dm, nn, all}
    
    @Option(gloss="Number of threads to use") public int numThreads = 1;
    
    // Parser-related parameters 
    @Option(gloss="Minimum beam setting") public int beamMin = 400;    
    @Option(gloss="Entry beam setting") public int beamEntry = 300;
    @Option(gloss="Prop beam setting") public int beamProp = 8;
    @Option(gloss="Switch between beam strategies (fixed beam width vs beam "
            + "relative to most probable analysis)") public boolean pruneStrategyIsProp = false;
    @Option(gloss="Start section (Penn treebank)") public int startSec = 0;
    @Option(gloss="End section (Penn treebank)") public int endSec = 23;
    @Option(gloss="Start document number (Penn treebank)") public int startDoc = 0;
    @Option(gloss="End document number (Penn treebank)") public int endDoc = 99;
    @Option(gloss="Start example number") public int startExample = 0;
    @Option(gloss="End example number") public int endExample = Integer.MAX_VALUE;
    @Option(gloss="nBest list to keep in each chart cell") public int nBest = 10;
    @Option(gloss="default lexicon size") public int defaultLexiconSize = 9671;
    @Option(gloss="Maximum number of examples") public int maxNumOfExamples = Integer.MAX_VALUE;
    @Option(gloss="Stage 1 -during search- time out (in ms)") public int timeOutStage1 = 300000;
    @Option(gloss="Stage 2 -analysis reconstruction- time out (in ms)") public int timeOutStage2 = 60000;
    @Option(gloss="Supress all error messages") public boolean silentMode = false;
    
    // always set to true. If set to false, chart will get huge.
    @Option(gloss="Put analyses with identical current fringes into the same chart entry") public boolean aggregate = true;    
    @Option(gloss="Use the full lexicon. If set to false, only a smaller lexicon is used "
            + "(only trees that are needed for the sentence -used for training)") public boolean fullLex = false;
    @Option public boolean goldPosTags = false;
    @Option(gloss="Ignore lexical items and only parses based on POS tag info") public boolean posOnly = false;
    @Option public boolean combineNNVBcats = true;
    @Option(gloss="Tree families will be used to generate possible lexicon entries for rare words") public boolean treeFamilies = false;
    @Option public boolean countNoneAdj = false;        
    @Option(gloss="Use the trained probability model. Only set to false if you want to do training, "
            + "or full parsing without probability model.") public boolean useProbabilityModel = false;
    @Option(gloss="This refers to the phase where trees are actually put together "
            + "(i.e. after the search phase). If set to true, only the most probable "
            + "tree will be calculated, otherwise, all trees will be calculated") public boolean calculateMostProbableTree = false; 
    
    @Option(gloss="SRL model: Gold standard lexicon and roles, "
            + "Gold standard lexicon and all roles, full lexicon and all roles") public SemanticsModelType semanticsModel = SemanticsModelType.oracle; 
    @Option(gloss="Use semantics for the lexicon and the integration with the syntactic parser") public boolean useSemantics = false;   
    
    @Option(gloss="Verification Look-ahead Probability (VLAP) path") public String vlapPath;
    
    // Debugging parameters
    @Option(gloss="Print chart during parsing") public boolean printChart = false;
    @Option(gloss="Print the top prefix analysis at each word") public boolean printIncrementalDeriv = false;
    @Option(gloss="Measure time for various parts of the parsing process (may slow down performance)") public boolean timeProfile = false;
    
    // Corpus-related and evaluation
    @Option public String lexicon;
    @Option public String predLexicon;    
    @Option public String listOfFreqWords;    
    @Option(required=true) public List<String> inputPaths = new ArrayList();
    @Option public String useExamplesOnlyList;
    @Option public boolean examplesInSingleFile = false;    
    @Option public int maxWordLength = Integer.MAX_VALUE;
    @Option public int exactWordLength = Integer.MAX_VALUE;
    @Option(gloss="Calculate the prediction theory difficulty prediction. "
            + "(outputs a difficulty for each word)") public boolean estimateProcDifficulty = false;
    @Option(gloss="Evaluate dependencies incrementally") public boolean evaluateIncrementalDependencies = false;
    @Option(gloss="Output incremental dependencies") public boolean outputIncrementalDependencies = false;
    @Option(gloss="Output only completed incremental dependencies") public boolean outputCompletedIncrementalDependencies = false;
    @Option(gloss="Evaluate evalb F1 score incrementally") public boolean evaluateIncrementalEvalb = false;
    @Option(gloss="Use a baseline probability model which prefers the most "
            + "frequent tree at every word.") public boolean freqBaseline = false;    
    @Option(gloss="The maximum number of DEPRELs to use to attach arguments on, "
            + "in the majority baseline model.") public int srlBaselineTopNDepRels = 20;
    @Option(gloss="Apply CoNLL heuristics (prepositions are heads of PPs, "
            + "subordinate conjunctions heads of SBARs, "
            + "and infinitive marker head of VPs)") public boolean applyConllHeuristics = false;
    @Option public boolean inputDundeeCorpus = false;  
    @Option public InputType inputType = InputType.plain;  
    @Option public boolean interactiveMode = false;  
    
    // Training
    @Option(gloss="Turn on training mode") public boolean train = false;
    @Option public String treeFrequencies;
    @Option public String wordFrequencies;    
    @Option public String superTagStruct;
    @Option public String superTagFringe;  
    @Option(gloss="Estimate interpolation factors. Depends on the backoff strategy you're using; "
            + "only set to true for brants' smoothing.") public boolean estimateInterpolationFactors = false;
    
    @Option(gloss="Save parameters to disk") public boolean outputParams = false;
    @Option(gloss="Save parameters as flat text files") public boolean outputParamsTxt = false;
    @Option(gloss="Output every this number of examples") public double outputExampleFreq = 10;        
    @Option(gloss="Output full predictions") public boolean outputFullPred = false;
    @Option(gloss="Output more debugging information (caution may slow down operation)") public boolean verbose = false;
    
    // Semantic Role Labelling - Classifier
    @Option(gloss="Liblinear C (cost of constraints violation)") public double C = 10.0;
    @Option(gloss="Liblinear eps (stopping criterion)") public double eps = 0.01;
    @Option(gloss="Liblinear model filename for argument labelling") public String argumentLabellerModel;
    @Option(gloss="Liblinear model filename for argument identification") public String argumentIdentifierModel;
    @Option(gloss="Filename of feature indexers containing mappings from lemmata, POS, etc. to feature ids") public String featureIndexers;
    @Option(gloss="Train classifier models") public boolean trainClassifiers = false;
    @Option(gloss="Extract features for argument identifier and labeller. Useful for extracting feature vectors on the test set for offline use") public boolean extractSrlFeatures = false;
    @Option(gloss="Output feature vectors for argument labelling") public String argumentLabellerFeatureVectorsFile;
    @Option(gloss="Output feature vectors for argument identifier") public String argumentIdentifierFeatureVectorsFile;
    @Option(gloss="Use discriminative classifiers (recommended)") public boolean useClassifiers;
    @Option(gloss="Baseline model type") public BaselineSemanticsModel baselineSemanticsModel = BaselineSemanticsModel.heuristicsIdentification;
    @Option(gloss="Path of MaltParser .mco model") public String maltParserModel;
    
    // Discriminative Reranking 
    
    // Save incremental analyses
    @Option(gloss="Path to incremental analyses serialised object") public String incrAnalysesPath;
    @Option(gloss="Path to incremental analyses features serialised object") public String incrAnalysesFeaturesPath;
    @Option(gloss="Path to oracle incremental analyses serialised object (used for training only)") public String oracleIncrAnalysesPath;
    @Option(gloss="Path to oracle incremental analyses features serialised object (used for training only)") public String oracleIncrAnalysesFeaturesPath;
    @Option(gloss="Filename of feature indexers containing mappings from elem. trees, SRL triples, etc., to feature ids") public String discriminativeFeatureIndexers;
    @Option(gloss="Extend feature indexers. Useful when running separate training experiments and want to keep same index space for feature mappings") public boolean extendIndexers;
    @Deprecated @Option(gloss="Serialise incremental analyses to disk") public boolean saveIncrAnalyses;
    @Option(gloss="Read training examples in batches, in case the dataset is too big to keep in memory") public boolean readExamplesInBatches;
    @Option(gloss="Serialise incremental analyses features to disk") public boolean saveIncrAnalysesFeatures;    
    @Option(gloss="Max number of sentences to keep in each serialised object (for full parser model only)") public int maxNumOfSentencesIncrAnalyses = 100000;
   
    // Train reranker
    @Option(gloss="Serialised object containing parameters for the discriminative model") public String stagedParamsFile;
    @Option(gloss="Number of training iterations") public int numIters = 10;
    @Option(gloss="Update model non-incrementally after every example has been processed (default=false, i.e., update incrementally per word)") public boolean sentenceLevel = false;
    @Option public boolean miniBatches = false;
    @Option public int miniBatchSize = 30;
    @Option public int convergePass = 1;  
    @Option(gloss="Step size power 1/T^power") public double stepSizeReductionPower = 0.5;
    @Option(gloss="Initial temperature") public double initTemperature = 1;
    @Option(gloss="Initialisation type") public InitType initType = InitType.uniformz;
    @Option public double initSmoothing = 0;
    @Option public double p = 0.01;
    @Option public double tolerance = 0.001;
    @Option public double initNoise = 1;
    @Option public Random initRandom = new Random(1);
    public enum LearningScheme { incremental, stepwise, batch };
    @Option public LearningScheme learningScheme = LearningScheme.incremental;
    @Option public double offsetBaselineScore = 1;
    @Option public double offsetModelScore = 1;
    @Option public boolean usePrefixBaselineFeature = false;
    @Option public boolean useWordBaselineFeature = false;
    @Option public boolean useSyntacticFeatures = false;
    @Option public boolean useFringeFeatures = false;
    @Option public boolean useGlobalSyntacticFeatures = false;
    @Option public boolean useSemanticsFeatures = false;
    @Option public boolean useSrlFeatures = false;
    @Option public boolean useSrlPosFeatures = false;
    @Option public boolean useLemmatisedSrlFeatures = false;
    @Option public boolean useSlowReranker = false;
    @Option public SemanticsType semanticsType = SemanticsType.quantum;
    @Option public List<String> semanticsLookupTable = new ArrayList();
    @Option public String semanticsLookupTableVectors;
    @Option(gloss="Use discriminative model scores to apply pruning") public boolean pruneUsingScores = false;
    
    // Use Reranker
    @Option(gloss="Parser type: use generative, discriminative, oracle, discriminativeOracle") public ParserType parserType = ParserType.generative;
  
    // Server mode  
    @Option(gloss="Run in server mode") public boolean serverMode = false;
    @Option(gloss="Port number the server listens on") public int port = 4446;
}
