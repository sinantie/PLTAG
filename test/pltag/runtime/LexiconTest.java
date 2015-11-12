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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.parser.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pltag.corpus.ElementaryStringTree;
import pltag.parser.Lexicon;
import pltag.parser.ParserModel;
import pltag.parser.TreeState;

/**
 *
 * @author sinantie
 */
public class LexiconTest
{
    ParserModel parser;
    public LexiconTest()
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
                  "-lexicon data/pltag/Lexicon_wsj_0221_noSemantics_files/Lexicon_wsj_0221_noSemantics-Freq-Parser-tag "
                +  "-predLexicon data/pltag/Lexicon_wsj_0221_noSemantics_files/Lexicon_wsj_0221_noSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_0221_withSemantics_files/Lexicon_wsj_0221_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_0221_withSemantics_files/Lexicon_wsj_0221_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_0003_withSemantics_files/Lexicon_wsj_0003_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_0003_withSemantics_files/Lexicon_wsj_0003_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_0240_6_withSemantics_files/Lexicon_wsj_0240_6_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_0240_6_withSemantics_files/Lexicon_wsj_0240_6_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-tag "
//                +  "-predLexicon data/pltag/Lexicon_wsj_1502_withSemantics_files/Lexicon_wsj_1502_withSemantics-Freq-Parser-prediction "
//                  "-lexicon data/pltag/Lexicon-test-tag "
//                +  "-predLexicon data/pltag/Lexicon-test-prediction "
                + "-listOfFreqWords resources/wordsFreqOverFive.txt "

//                + "-treeFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/TreeFrequencies.txt.final "
//                + "-wordFrequencies results/output/pltag/train/0221_noCountNoneAdj_final/WordFrequencies.txt.final "                
//                + "-superTagStruct results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagStruct.txt.final "
//                + "-superTagFringe results/output/pltag/train/0221_noCountNoneAdj_final/SuperTagFringe.txt.final "
                
                + "-treeFrequencies results/output/pltag/train/00_again/TreeFrequencies.txt.final "
                + "-wordFrequencies results/output/pltag/train/00_again/WordFrequencies.txt.final "                
                + "-superTagStruct results/output/pltag/train/00_again/SuperTagStruct.txt.final "
                + "-superTagFringe results/output/pltag/train/00_again/SuperTagFringe.txt.final "

//                + "-treeFrequencies /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/TreeFrequencies.txt.last "                                
//                + "-wordFrequencies /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/WordFrequencies.txt.last "                                
//                + "-superTagStruct /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/SuperTagStruct.txt.last "                                
//                + "-superTagFringe /home/sinantie/EDI/synsem/konstas/PLTAG-Vera/SuperTagFringe.txt.last "
                
                + "-beamMin 550 "
                + "-beamEntry 50 "
                + "-beamProp 8 "
//                + "-pruneStrategyIsProp "
                + "-nBest 10 "
//                + "-maxWordLength 40 "
                + "-examplesInSingleFile "
//                + "-inputPaths data/pltag/GoldStandard_wsj_0003_noSemantics "
                + "-inputPaths data/pltag/GoldStandard_wsj_0240_6_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_1502_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_23_noSemantics "
//                + "-inputPaths data/pltag/GoldStandard_wsj_23_withSemantics_traces ../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English-edited.txt "
//                + "-inputPaths data/pltag/single_wsj_23_withSemantics "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English-edited.txt "
//                + "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train.txt "
                + "-outputFullPred "
//                + "-useSemantics "
//                + "-freqBaseline "
                + "-fullLex "
//                + "-printIncrementalDeriv "
                + "-goldPosTags ";
//                + "-countNoneAdj ";

        Options opts = new Options();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params
        parser = new ParserModel(opts);        
        parser.init();                     
        
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testLexiconUnlexEntriesWithSLabel()
    {
       Map<String, String> map = new HashMap(); 
       Lexicon lexicon = parser.getLexicon();
        MultiValueMap<String, ElementaryStringTree> col = lexicon.getLexEntriesContaining("S");
        for(Entry<String, Object> entry : col.entrySet())
        {
            for(ElementaryStringTree tree : (Collection<ElementaryStringTree>)entry.getValue())
            {                
                TreeState ts = new TreeState(tree, (short)0, 1);
                map.put(tree.getUnlexStruct(1), String.format("%s;%s", ts.getFringe(), ts.getFutureFringe().getFringe()));
//                System.out.println(String.format("%s\t%s\t%s ; %s", entry.getKey(), tree.getUnlexStruct(1), ts.getFringe(), ts.getFutureFringe().getFringe()));                
            }            
        }
        
        for(Entry<String, String> e : map.entrySet())
        {
            // filter only trees with NPs
            String tree = e.getKey().trim();
            String fringes[] = e.getValue().trim().split(";");
            String curFringe = fringes[0];
            String futureFringes = fringes[1];
            if(tree.contains("NP") && tree.startsWith("(S^null_0 (NP^0_0 (:^0_0 *^0_0))"))
            {
                //if(!curFringe.startsWith("[][S^-1_0@t:0L:?] : NP^0_-1@t:0L:?")) // filter out [][S]:NP cases in (trivial)
                {
                    //if(futureFringes.contains("[S^0_0@t:0L:?] : NP^0_-1@t:0L:?")) // S - NP subj elsewhere in the tree
                        System.out.println(tree + " ||| " + e.getValue());
                }
            }            
        }
    }        
}
