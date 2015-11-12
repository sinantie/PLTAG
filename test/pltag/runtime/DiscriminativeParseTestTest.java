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
import pltag.parser.Model;
import pltag.parser.ParserModel;

/**
 *
 * @author sinantie
 */
public class DiscriminativeParseTestTest
{
    Model parser;
    public DiscriminativeParseTestTest()
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
//                  "-lexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_0003_withSemantics_files/Lexicon_wsj_0003_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_0003_withSemantics_files/Lexicon_wsj_0003_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_2300_46_withSemantics_files/Lexicon_wsj_2300_46_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_2300_46_withSemantics_files/Lexicon_wsj_2300_46_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_2300_1_withSemantics_files/Lexicon_wsj_2300_1_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_2300_1_withSemantics_files/Lexicon_wsj_2300_1_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_2300_18_withSemantics_files/Lexicon_wsj_2300_18_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_2300_18_withSemantics_files/Lexicon_wsj_2300_18_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_2300_19_withSemantics_files/Lexicon_wsj_2300_19_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_2300_19_withSemantics_files/Lexicon_wsj_2300_19_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_0240_6_withSemantics_files/Lexicon_wsj_0240_6_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_0240_6_withSemantics_files/Lexicon_wsj_0240_6_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon-test-tag "
//                +  "-predLexicon data/pltag/Lexicon-test-prediction "
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
                
//                    + "-treeFrequencies results/output/pltag/train/00_again/TreeFrequencies.txt.final "
//                    + "-wordFrequencies results/output/pltag/train/00_again/WordFrequencies.txt.final "                
//                    + "-superTagStruct results/output/pltag/train/00_again/SuperTagStruct.txt.final "
//                    + "-superTagFringe results/output/pltag/train/00_again/SuperTagFringe.txt.final "

//                + "-treeFrequencies /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/TreeFrequencies.txt.last "                                
//                + "-wordFrequencies /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/WordFrequencies.txt.last "                                
//                + "-superTagStruct /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/SuperTagStruct.txt.last "                                
//                + "-superTagFringe /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/SuperTagFringe.txt.last "
                
                + "-beamMin 350 "
                + "-beamEntry 350 "
                + "-beamProp 8 "
//                + "-pruneStrategyIsProp "
                + "-nBest 250 "
//                + "-maxWordLength 40 "
                + "-examplesInSingleFile "
//                + "-inputPaths data/pltag/GoldStandard_wsj_0003_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_2300_1_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_2300_46_withSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_2300_18_withSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_2300_19_withSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_23_noSemantics_small_beam_all "
//                    + "-inputPaths data/pltag/GoldStandard_wsj_0240_6_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_1502_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_23_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_23_withSemantics_traces "
                + "-inputType pltag "
                + "-inputPaths data/pltag/single_wsj_23_withSemantics_tracesFix "
                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English-edited.txt "
                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train.txt "
//                + "-inputPaths data/pltag/single_wsj_0221_withSemantics_edited "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train-edited.txt "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train.txt "
                + "-outputFullPred "
                + "-useSemantics "
                + "-applyConllHeuristics "
                + "-semanticsModel parsedAllRoles "
                + "-argumentLabellerModel data/classifiers/bilexical_no_sense_opPath.labeller.tuned.model "
                + "-argumentIdentifierModel data/classifiers/bilexical_no_sense_opPath.identifier.tuned.model "
                + "-argumentLabellerFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.labeller.test2.vectors "
                + "-argumentIdentifierFeatureVectorsFile data/classifiers/bilexical_no_sense_syntactic_opPath.identifier.test2.vectors "
                + "-featureIndexers data/classifiers/bilexical_no_sense_syntactic_opPath.indexers "
                + "-timeOutStage2 60000 "
                + "-timeOutStage1 90000 "
                + "-useClassifiers " 
//                + "-extractFeatures " 
//                + "-trainClassifiers "
//                + "-maxNumOfExamples 500 "
//                + "-outputIncrementalDependencies "
//                + "-evaluateIncrementalDependencies "
//                + "-freqBaseline "
//                + "-baselineSemanticsModel maltParser "
//                + "-maltParserModel ../../../maltparser-1.8/wsj-2planar-liblinear.mco "
//                + "-fullLex "
//                + "-printIncrementalDeriv "
                + "-goldPosTags ";
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
