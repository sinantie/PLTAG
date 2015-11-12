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

import fig.basic.Indexer;
import fig.exec.Execution;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
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
public class DiscriminativeParseTrainTest
{
    Model parser;
    public DiscriminativeParseTrainTest()
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
                  "-train "
//                + "-execDir results/output/pltag/train/0001 "
//                + "-create -overwriteExecDir "               
                + "-listOfFreqWords resources/wordsFreqOverFive.txt "                
//                + "-treeFrequencies data/PLTAGLexicon/TreeFrequencies.txt.combinednew-nononeadj.tar.gz " // countNoneAdj = false
                + "-beamMin 50 "                
                + "-beamEntry 50 "
                + "-beamProp 8 "
//                + "-pruneStrategyIsProp "
                + "-nBest 1 "
                + "-examplesInSingleFile "
                + "-inputType pltag "
                + "-inputPaths data/pltag/single_wsj_0003_noSemantics "
                + "-outputExampleFreq 1 "
                + "-outputParams "
                + "-outputParamsTxt "                               
                + "-countNoneAdj ";

        Options opts = new Options();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params
        parser = new ParserModel(opts);        
        
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testTrain()
    {
       parser.init();
       parser.readExamples();
       parser.testTrain();
    }
}
