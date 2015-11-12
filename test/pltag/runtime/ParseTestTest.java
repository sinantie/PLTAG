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
import pltag.parser.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pltag.parser.Lexicon;
import pltag.parser.Model;
import pltag.parser.ParserModel;

/**
 *
 * @author sinantie
 */
public class ParseTestTest
{
    Model parser;
    public ParseTestTest()
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
//                  "-execDir results/output/pltag/test/test "
//                + "-create -overwriteExecDir "
//                  "-lexicon data/pltag/Lexicon_wsj_0221_noSemantics_files/Lexicon_wsj_0221_noSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_0221_noSemantics_files/Lexicon_wsj_0221_noSemantics-Freq-Parser-prediction "
                  "-lexicon data/pltag/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix-Freq-Parser-tag "
                +  "-predLexicon data/pltag/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix-Freq-Parser-prediction "
                + "-listOfFreqWords resources/wordsFreqOverFive.txt "
//                + "-treeFrequencies data/PLTAGLexicon/TreeFrequencies.txt.combinednew-nononeadj.tar.gz " // countNoneAdj = false
//                

                + "-treeFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/TreeFrequencies.txt.final "
                + "-wordFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/WordFrequencies.txt.final "                
                + "-superTagStruct results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagStruct.txt.final "
                + "-superTagFringe results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagFringe.txt.final "
//                + "-treeFrequencies results/output/pltag/train/0221_countNoneAdj_final/TreeFrequencies.txt.final "
//                + "-wordFrequencies results/output/pltag/train/0221_countNoneAdj_final/WordFrequencies.txt.final "                
//                + "-superTagStruct results/output/pltag/train/0221_countNoneAdj_final/SuperTagStruct.txt.final "
//                + "-superTagFringe results/output/pltag/train/0221_countNoneAdj_final/SuperTagFringe.txt.final "
                                
                + "-beamMin 10 " // beam width, i.e., the difference between entries with highest and lowest probability in the beam
                + "-beamEntry 350 " // number of elements on beam
                + "-beamProp 8 "
//                + "-pruneStrategyIsProp "
                + "-nBest 250 "
//                + "-maxWordLength 40 "
                + "-examplesInSingleFile "
//                + "-inputPaths data/pltag/GoldStandard_wsj_23_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_23_withSemantics_traces "
                + "-inputPaths data/pltag/single_wsj_23_withSemantics_tracesFix "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English-edited.txt "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train.txt "
                + "-inputType pltag "
//                + "-inputPaths data/pltag/single_wsj_0221_withSemantics_edited "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train-edited.txt "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train.txt "
                + "-outputFullPred "
                + "-useSemantics "
//                + "-applyConllHeuristics "
                + "-semanticsModel parsedAllRoles "
                + "-argumentLabellerModel data/classifiers/bilexical_no_sense_opPath.labeller.tuned.model "
                + "-argumentIdentifierModel data/classifiers/bilexical_no_sense_opPath.identifier.weight.tuned.model "
//                + "-argumentLabellerFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.labeller.test2.vectors "
//                + "-argumentIdentifierFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.identifier.test2.vectors "
                + "-featureIndexers data/classifiers/bilexical_no_sense_syntactic_opPath.indexers "
                + "-timeOutStage2 120000 "
                + "-timeOutStage1 300000 "
                + "-useClassifiers " 
                + "-numThreads 1 " 
                
                // save incremental analyses features
//                + "-saveIncrAnalysesFeatures "                 
//                + "-incrAnalysesFeaturesPath data/incrAnalyses/features/incrAnalysesFeatures_wsj_debug "                 
//                + "-sentenceLevel "                 
//                + "-extendIndexers " 
//                + "-maxNumOfExamples 10 "
//                + "-maxNumOfSentencesIncrAnalyses 5 "
                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_fringeFeats_wsj_0221_sentenceLevel_extended "

//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_conllHeuristics_sentenceLevel_extended " 
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_incompleteSRL_conllHeuristics_sentenceLevel_extended " 
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_noProps_wordProb_conllHeuristics_fixBugs_extended " 
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_noProps_wordProb_conllHeuristics_sentenceLevel_maxF1_extended " 
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_debug_testing_sentenceLevel " 
                // use trained discriminative model                
                + "-parserType generative "
                + "-offsetBaselineScore 1 "
                + "-offsetModelScore 1 "                
//                + "-pruneUsingScores "
                + "-usePrefixBaselineFeature "
//                + "-useWordBaselineFeature "
//                + "-useSyntacticFeatures "
//                + "-useGlobalSyntacticFeatures "
                + "-useSemanticsFeatures "
//                + "-useSrlFeatures "
//                + "-useSrlPosFeatures "
                + "-useLemmatisedSrlFeatures "
//                + "-stagedParamsFile results/output/pltag/train/discriminative/perSentence-prefixBaseline-wordBaseline-semantics-dim100-fixBugs-stepwise-random-tol0_001-slow/params.discriminative.params.obj.gz " 

//                + "-stagedParamsFile results/output/pltag/train/discriminative/perSentence-prefixBaseline-wordBaseline-srlFeat-moreSrlFeat-lemmatised-conllHeuristics-moreFixBugs-stepwise-random-tol0_001/params.discriminative.params.obj.gz " 
                + "-stagedParamsFile results/output/pltag/train/discriminative/perSentence-prefixBaseline-dmSemantics-moreFixBugs-stepwise-random-tol0_001-slow/params.discriminative.params.obj.gz " 

//                + "-stagedParamsFile results/output/pltag/train/discriminative/perSentence-prefixBaseline-wordBaseline-srlFeat-moreSrlFeat-semanticsFeat-conllHeuristics-moreFixBugs-stepwise-random-tol0_001/params.discriminative.params.obj.gz " 
//                + "-stagedParamsFile results/output/pltag/train/discriminative/perWord-prefixBaseline_wordBaseline-syntax-srlFeat-srlPosFeat-conllHeuristics-fixBugs-random-tol0_001-fast/params.discriminative.params.obj.gz " 
//                + "-stagedParamsFile results/output/pltag/train/discriminative/perWord-wordBaseline-syntaxFeat-srlFeat-srlPosFeat-conllHeuristics-random-tol0_001-fast/params.discriminative.params.obj.gz " 
//                + "-stagedParamsFile results/output/pltag/train/discriminative/debug/params.discriminative.params.obj.gz " 
//                + "-outputIncrementalDependencies "
//                + "-evaluateIncrementalDependencies "
                // QUANTUM
//                + "-semanticsType quantum "
//                + "-semanticsLookupTable data/pltag/srlTriplesLemmatised "
//                + "-semanticsLookupTableVectors data/pltag/output.mate.srl-simple7.dim100.fvec "
                
                // NN
//                + "-semanticsType nn "
//                + "-semanticsLookupTable data/pltag/nn-semantics/fullSrlResult "
//                                
                // DM
                + "-semanticsType dm "
                + "-semanticsLookupTable data/pltag/dm-semantics/fullSrlResult "

//                + "-evaluateIncrementalEvalb "
//                + "-freqBaseline "
//                + "-baselineSemanticsModel maltParser "
//                + "-maltParserModel ../../../maltparser-1.8/wsj-2planar-liblinear.mco "
//                + "-printIncrementalDeriv "
//                + "-evaluateIncrementalEvalb "
//                + "-exactWordLength 30 "
//                + "-useExamplesOnlyList data/pltag/lists/incrEvalWithSemanticsList_noSemantics "
//                + "-maxNumOfExamples 5 "
//                + "-maxNumOfSentencesIncrAnalyses 5 "
//                + "-estimateProcDifficulty "                
//                + "-vlapPath data/pltag/vlap_wsj_0221.obj.gz "
                + "-fullLex ";                
//                + "-goldPosTags ";
//                + "-countNoneAdj ";

        Options opts = new Options();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params
        parser = new ParserModel(opts);        
        parser.init();
        parser.readExamples();           
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testTest()
    {
       
       parser.testParse();
    }
    
    //@Test
    public void testConllRoleNames()
    {
        ((ParserModel)parser).testConllRoleNames();
    }
}
