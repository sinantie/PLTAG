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

import fig.basic.IOUtils;
import fig.exec.Execution;
import java.util.HashMap;
import pltag.parser.Options;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pltag.parser.semantics.discriminative.ExtractFeatures;
import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalysis;

/**
 *
 * @author sinantie
 */
public class ExtractFeaturesTest
{
    ExtractFeatures extractFeatures;
    public ExtractFeaturesTest()
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
                  "-execDir data/incrAnalyses "
                + "-create -overwriteExecDir "                
                + "-examplesInSingleFile "
//                + "-oracle "
                + "-inputPaths data/incrAnalyses/incrAnalyses_wsj_0221_test "                
                + "-outputParams ";

        Options opts = new Options();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params
        extractFeatures = new ExtractFeatures(opts);        
        
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testExtract()
    {       
       extractFeatures.execute();       
    }
    
//    @Test
    public void mapTest()
    {       
       HashMap<String, IncrementalAnalysis> map = new HashMap();
       for(int i = 0; i < 10; i++)
       {
           for(int j = 0; j < 500000; j++)
           {
               map.put(i + " " + j, new IncrementalAnalysis());
           }
           IOUtils.writeObjFileEasy("data/incrAnalyses/mapTest." + i + ".obj.gz", map);
           map.clear();
       }
       
    }
}
