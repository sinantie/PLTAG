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
package pltag.runtime;

import fig.exec.Execution;
import java.util.Arrays;
import pltag.parser.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pltag.parser.semantics.discriminative.OfflineReranker;

/**
 *
 * @author sinantie
// */
public class OfflineRerankerTrainTest
{
    OfflineReranker reranker;
    public OfflineRerankerTrainTest()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }
    
    @Before
    public void setUp()
    {
        String args =
                  "-numThreads 1 "                                
                + "-train "                                
                + "-execDir results/output/pltag/train/discriminative/test/ "                                
                + "-inputPaths dummy "                                
//                + "-incrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_globalFeats_incompleteSRL_sentenceLevel "
//                + "-oracleIncrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_globalFeats_incompleteSRL_sentenceLevel.oracle "
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_incompleteSRL_sentenceLevel_extended "

                + "-incrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_globalFeats_fringeFeats_sentenceLevel "
                + "-oracleIncrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_globalFeats_fringeFeats_sentenceLevel.oracle "                
                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_fringeFeats_wsj_0221_sentenceLevel_extended "
                
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_incompleteSRL_wsj_23 "
//                + "-incrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_noProps_wordProb_conllHeuristics_sentenceLevel "
//                + "-incrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_globalFeats_conllHeuristics_sentenceLevel "
//                + "-oracleIncrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_globalFeats_conllHeuristics_sentenceLevel.oracle "
//                + "-oracleIncrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_noProps_wordProb_conllHeuristics_fixBugs.oracle "
//                + "-oracleIncrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_globalFeats_conllHeuristics_sentenceLevel_sentenceLevel.oracle "
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_conllHeuristics_sentenceLevel_extended "
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_conllHeuristics_sentenceLevel_sentenceLevel "
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_noProps_wordProb_conllHeuristics_fixBugs_extended "
//                + "-incrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_noProps_wordProb_conllHeuristics "
//                + "-oracleIncrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_0221_noProps_wordProb_conllHeuristics.oracle "
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_noProps_wordProb_conllHeuristics_extended "

//                + "-incrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_debug_0221_sentenceLevel "
//                + "-oracleIncrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_debug_0221_sentenceLevel.oracle "                
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_debug_testing_sentenceLevel_extended "
                
//                + "-readExamplesInBatches "
                // QUANTUM
//                + "-semanticsType quantum "
//                + "-semanticsLookupTable data/pltag/srlTriplesLemmatised "
//                + "-semanticsLookupTableVectors data/pltag/output.mate.srl-simple7.dim100.fvec "
                
                // NN
//                + "-semanticsType nn "
//                + "-semanticsLookupTable data/pltag/nn-semantics/ottokar_results_clean_arg_pred.txt "
                                
                // DM
//                + "-semanticsType dm "
//                + "-semanticsLookupTable data/pltag/dm-semantics/fullSrlResult "
                        
                // ALL
                + "-semanticsType all "
                + "-semanticsLookupTable data/pltag/dm-semantics/fullSrlResult data/pltag/nn-semantics/ottokar_results_clean_arg_pred.txt data/pltag/srlTriplesLemmatised "
                + "-semanticsLookupTableVectors data/pltag/output.mate.srl-simple7.dim100.fvec "

                + "-usePrefixBaselineFeature "
//                + "-useWordBaselineFeature "
//                + "-useGlobalSyntacticFeatures "
//                + "-useSyntacticFeatures "
//                + "-useFringeFeatures "
                + "-useSemanticsFeatures "
//                + "-useSrlFeatures "
//                + "-useSrlPosFeatures "
                + "-useLemmatisedSrlFeatures "
                + "-sentenceLevel "
//                + "-maxNumOfExamples 14 "
                + "-initType random "
                + "-useSlowReranker "
                + "-tolerance 0.001 "
                + "-initSmoothing 0 "
                + "-initNoise 1 "
                + "-stepSizeReductionPower 0.7 "
                + "-miniBatchSize 50 "
                // incremental (default) batch stepwise
                + "-learningScheme stepwise "
                + "-numIters 15 ";

        Options opts = new Options();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params
        reranker = new OfflineReranker(opts);        
        
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testTrain()
    {            
        // uncomment when reading examples in one go (have to comment out readExamplesInBatches)
       reranker.readExamples();
       reranker.testTrain();
    }
    
//    @Test
    public void testPrintFullSrlTriples()
    {            
        // uncomment when reading examples in one go (have to comment out readExamplesInBatches)
       reranker.readExamples();
       String path = "data/pltag/fullSrlTriples_wsj_0221_debug";
       reranker.printFullSrlTriples(path);
    }
    
//    @Test
    public void testIncreaseMultipleCounts()
    {
        int[] featureIds = new int[10];
        Arrays.fill(featureIds, -1000);
        double[] featureValues = new double[10];
        Arrays.fill(featureValues, -1000);
        int startPosition = 5;
        
        int[] vecIds = new int[] {120, 129, 129, 130};
        reranker.increaseMultipleCounts(vecIds, startPosition, featureIds, featureValues);
        System.out.println("Feature Ids " + Arrays.toString(featureIds));
        System.out.println("Feature Values " + Arrays.toString(featureValues));    
        int[] targetIds = new int[] {-1000, -1000, -1000, -1000, -1000, 120, 129, 130, -1000, -1000};
        double[] targetValues = new double[] {-1000, -1000, -1000, -1000, -1000, 1.0, 2.0, 1.0, -1000, -1000};
        
        Assert.assertArrayEquals(targetIds, featureIds);
        Assert.assertArrayEquals(targetValues, featureValues, 0.0);
    }
}
