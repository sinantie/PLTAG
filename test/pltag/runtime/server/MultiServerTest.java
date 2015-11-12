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
package pltag.runtime.server;

import fig.exec.Execution;
import pltag.parser.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pltag.parser.ParserModel;

/**
 *
 * @author sinantie
 */
public class MultiServerTest
{
    MultiServer server;
    public MultiServerTest()
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
                 "-serverMode "
                +"-port 4446 "
                +  "-lexicon data/pltag/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix-Freq-Parser-tag "
                + "-predLexicon data/pltag/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix-Freq-Parser-prediction "
                + "-listOfFreqWords resources/wordsFreqOverFive.txt "
                + "-treeFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/TreeFrequencies.txt.final "
                + "-wordFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/WordFrequencies.txt.final "                
                + "-superTagStruct results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagStruct.txt.final "
                + "-superTagFringe results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagFringe.txt.final "
                
                + "-beamMin 350 " // beamWidth
                + "-beamEntry 350 "
                + "-beamProp 8 "
//                + "-pruneStrategyIsProp "
                + "-nBest 250 "
                + "-inputPaths dummy "
                + "-inputType plain "
                + "-outputFullPred "
                + "-useSemantics "
                + "-applyConllHeuristics "
                + "-semanticsModel parsedAllRoles "
                + "-argumentLabellerModel data/classifiers/bilexical_no_sense_opPath.labeller.tuned.model "
                + "-argumentIdentifierModel data/classifiers/bilexical_no_sense_opPath.identifier.weight.tuned.model "
                + "-argumentLabellerFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.labeller.test2.vectors "
                + "-argumentIdentifierFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.identifier.test2.vectors "
                + "-featureIndexers data/classifiers/bilexical_no_sense_syntactic_opPath.indexers "
                + "-timeOutStage2 120000 "
                + "-timeOutStage1 300000 "
                + "-useClassifiers " 
                + "-numThreads 1 " 
         
                + "-parserType generative "
                + "-offsetBaselineScore 1 "
                + "-offsetModelScore 1 "
                
//                + "-pruneUsingScores "                
                + "-usePrefixBaselineFeature "
                + "-useWordBaselineFeature "
                + "-useSyntacticFeatures "
//                + "-useSemanticsFeatures "
                + "-useSrlFeatures "
                + "-useSrlPosFeatures "
//                + "-stagedParamsFile results/output/pltag/train/discriminative/perWord-prefixBaseline-wordBaseline-syntaxFeat-srlFeat-srlPosFeat-conllHeuristics-moreFixBugs-random-tol0_001-step0_7-batch50-slow/params.discriminative.params.obj.gz " 
//                + "-outputIncrementalDependencies "                
                + "-estimateProcDifficulty "
                + "-printIncrementalDeriv "
                + "-fullLex ";                
//                + "-countNoneAdj ";
        
        server = new MultiServer(args.split(" "));                
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testExecute()
    {
        server.execute();
    }
}
