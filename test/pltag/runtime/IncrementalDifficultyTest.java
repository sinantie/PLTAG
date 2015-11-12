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

import fig.basic.StopWatchSet;
import fig.exec.Execution;
import pltag.parser.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pltag.parser.Model;
import pltag.parser.ParserModel;

/**
 *
 * @author sinantie
 */
public class IncrementalDifficultyTest
{
    Model parser;
    public IncrementalDifficultyTest()
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
                  "-execDir results/output/pltag/test/debug "
                + "-create -overwriteExecDir "
                + "-lexicon data/pltag/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix-Freq-Parser-tag "
                +  "-predLexicon data/pltag/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix-Freq-Parser-prediction "
                + "-listOfFreqWords resources/wordsFreqOverFive.txt "

//                
                + "-treeFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/TreeFrequencies.txt.final "
                + "-wordFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/WordFrequencies.txt.final "                
                + "-superTagStruct results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagStruct.txt.final "
                + "-superTagFringe results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagFringe.txt.final "
                
                + "-beamMin 350 "
                + "-beamEntry 350 "
                + "-beamProp 8 "
//                + "-pruneStrategyIsProp "
                + "-nBest 250 "
//                + "-maxWordLength 40 "
                + "-examplesInSingleFile "
//                + "-inputPath data/pado_stimuli/Items/PP-POS/RaynerPP-VA.txt.posTagged "
//                + "-inputPath data/dundee/dundee_corrected_sentences-pltag-dundee-noQuotes "
                + "-inputPath data/dundee/dundee.words.tokenized "
//                + "-useExamplesOnlyList results/output/pltag/test/dundee_all_tokenized-wordIds.noSemLexicon-correctQuotes.generative/wrongQuotes-examplesIds.txt "

                + "-fullLex "
                + "-outputFullPred "
                + "-inputType dundee "
                + "-numThreads 1 "                
//                + "-interactiveMode "
                + "-useSemantics "                
                + "-semanticsModel parsedAllRoles "
                + "-argumentLabellerModel data/classifiers/bilexical_no_sense_opPath.labeller.tuned.model "
                + "-argumentIdentifierModel data/classifiers/bilexical_no_sense_opPath.identifier.weight.tuned.model "
                + "-argumentLabellerFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.labeller.test2.vectors "
                + "-argumentIdentifierFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.identifier.test2.vectors "
                + "-featureIndexers data/classifiers/bilexical_no_sense_syntactic_opPath.indexers "
                + "-timeOutStage2 120000 "
                + "-timeOutStage1 300000 "
                + "-useClassifiers " 
                             
                // SRL-only (conllHeuristics)
                + "-applyConllHeuristics "
                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_conllHeuristics_sentenceLevel_extended " 
                + "-stagedParamsFile results/output/pltag/train/discriminative/perSentence-prefixBaseline-wordBaseline-srlFeat-moreSrlFeat-lemmatised-conllHeuristics-moreFixBugs-stepwise-random-tol0_001/params.discriminative.params.obj.gz " 
                + "-useSrlFeatures "
                + "-useSrlPosFeatures "
                
//                
                // Semantics - DIM 200
//                + "-discriminativeFeatureIndexers data/incrAnalyses/features/featureIndexers_globalFeats_fringeFeats_wsj_0221_sentenceLevel_extended " 
//                + "-stagedParamsFile results/output/pltag/train/discriminative/perSentence-prefixBaseline-wordBaseline-semantics-dim200-fixBugs-stepwise-random-tol0_001-slow/params.discriminative.params.obj.gz " 
//                + "-useSemanticsFeatures "
//                + "-semanticsLookupTable data/pltag/srlTriplesLemmatised "
//                + "-semanticsLookupTableVectors data/pltag/output.mate.srl-simple7.dim200.fvec "
//                
                + "-parserType generative "
                + "-usePrefixBaselineFeature "
                + "-useLemmatisedSrlFeatures "
                + "-goldPosTags "
                
//                + "-outputCompletedIncrementalDependencies "
                + "-estimateProcDifficulty ";
//                + "-printIncrementalDeriv "; 
//                + "-goldPosTags ";

        Options opts = new Options();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params
        parser = new ParserModel(opts);        
        parser.init();
        if(!opts.interactiveMode)
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
       System.out.println(StopWatchSet.getStats());
    }
    
//    @Test
    public void testInteractiveParse()
    {
       
       parser.testInteractiveParse();       
    }
}
