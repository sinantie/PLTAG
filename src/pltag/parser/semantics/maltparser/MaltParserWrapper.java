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
package pltag.parser.semantics.maltparser;

import fig.basic.LogInfo;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.core.lw.graph.LWDependencyGraph;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class MaltParserWrapper
{
    private boolean debug = false;
    private static final int ID_COL = 0,
                            FORM_COL = 1,
                            LEMMA_COL = 2,
                            POS_COL = 4,
                            HEAD_COL = 8,
                            DEPREL_COL = 10;
    
    ConcurrentMaltParserModel model;

    public MaltParserWrapper(String modelFilename)
    {
        try
        {
            URL swemaltMiniModelURL = new File(modelFilename).toURI().toURL();
            model = ConcurrentMaltParserService.initializeParserModel(swemaltMiniModelURL);
        } catch (Exception e)
        {
            LogInfo.error("Error load maltparser model: " + modelFilename);
        }
    }

    public String[] parse(String[] wordsArray, String name)
    {
        // create an array of conll-formatted parsed output strings (each line represents a word), 
        // one for each word in the input sentence.
        String[] out = new String[wordsArray.length];        
        try
        {
             LWDependencyGraph incrementalGraph = model.incrementalParse(wordsArray);
             List<List<String>> prevParse = new ArrayList();
             for(String[] parse : incrementalGraph.getIncrementalOutput()) // for each parse
             {                               
                 List<List<String>> words = new ArrayList();
//                 for(String word : parse) // for each line
                 int lastNonEmptyHeadPos = -1;
                 for(int wordPos = 0; wordPos < parse.length -1; wordPos++) // for each line
                 {
                     String word = parse[wordPos];
                     if(word != null)
                     {
                         List<String> tokens = Utils.unpackConllWord(word);
                         String head = tokens.get(HEAD_COL);
                         if(!head.equals("-1"))
                             lastNonEmptyHeadPos = wordPos;
                         words.add(tokens);
                     }
                     if(debug)
                         System.out.println(word);
                 }
                 // save only the sublist up to the first non-empty head
                 if(lastNonEmptyHeadPos != -1)
                 {
                     if(out[lastNonEmptyHeadPos] == null)
                     {
                         out[lastNonEmptyHeadPos] = Utils.repackConllTokensToSentence(words.subList(0, lastNonEmptyHeadPos+1));
                         fillInPrevEmptyWords(out, prevParse, lastNonEmptyHeadPos);
                     }
                     
                 }       
                 prevParse = words;
                 if(debug)
                     System.out.println("\n");
             }
        } catch (Exception e)
        {
            LogInfo.error("Error parsing example: " + name);
            e.printStackTrace();
        }
        if(debug)
        {
            for(String s : out)
                System.out.println(s + "\n");
        }
        return out;
    }
    
    private void fillInPrevEmptyWords(String[] out, List<List<String>> words, int lastNonEmptyHeadPos)
    {
        if(lastNonEmptyHeadPos < 1)
            return;
        for(int i = lastNonEmptyHeadPos - 1; i >= 0; i--)
        {
            if(out[i] == null)
            {
                if(!words.isEmpty())
                    out[i] = Utils.repackConllTokensToSentence(words.subList(0, i+1));
            }
            else
            {
                break;
            }
        }
    }
    
    public static void main(String[] args)
    {
        String modelFile = "../../../maltparser-1.8/wsj-2planar-liblinear.mco";
//        String modelFile = "../../../maltparser-1.8/wsj-planar-liblinear.mco";
//        String modelFile = "../../../maltparser-1.8/engmalt.linear-1.7.mco";
        String[] tokens = Utils.readLines("../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/test_cut3.gold");
        MaltParserWrapper mpw = new MaltParserWrapper(modelFile);
        mpw.parse(tokens, "test");
    }  
}
