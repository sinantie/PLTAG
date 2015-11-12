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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pltag.corpus.TagCorpus;
import pltag.corpus.TagCorpusOptions;

/**
 *
 * @author sinantie
 */
public class TagCorpusTest
{
    TagCorpus corpus;
    
    public TagCorpusTest()
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
                "-ptbPath ../../../ptb_v3/np_bracketing/wsj/ "
//                "-ptbPath ../../../ptb_v3/test_files/ "
                + "-lexiconFilename DummyLexicon.txt "
                + "-predLexFilename resources/predlex.txt "                
                + "-goldStandardFilename DummyGoldStandard.txt "
                + "-examplesInSingleFile "
                + "-outputFileName outputSingleFile.txt "
                + "-percolTableFilename resources/percolationTableVera.txt "
                + "-propBankFilename ../../../propbank/prop.txt "
//                + "-propBankFilename ../../../propbank/prop_test.txt "
                + "-nomBankFilename ../../../nombank.1.0/nombank.1.0 "                
//                + "-moreThanOneFoot "
                + "-justMultiWordLexemes "
//                + "-verbose "
                + "-useSemantics "
                + "-outputEmptyExamples "
//                + "-individualFilesPath wsj_tag " // DEPRECATED
//                + "-outputIndividualFiles " // DEPRECATED
                + "-startTrack 23 "
                + "-endTrack 23";
        
        TagCorpusOptions opts = new TagCorpusOptions();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params
        corpus = new TagCorpus(opts);
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testExecute()
    {
        corpus.testExecute();
    }
}
