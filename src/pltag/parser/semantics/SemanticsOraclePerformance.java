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
package pltag.parser.semantics;

import edu.stanford.nlp.international.Languages;
import edu.stanford.nlp.parser.lexparser.TreebankLangParserParams;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeTransformer;
import fig.basic.Fmt;
import fig.basic.Indexer;
import java.util.Arrays;
import java.util.Map;
import pltag.parser.performance.EvalbImpl;

/**
 *
 * @author sinantie
 */
public class SemanticsOraclePerformance extends SemanticsPerformance
{

    private final EvalbImpl curEvalb;
    private final TreeTransformer treeCollinizer;
    private double accuracy;
    
    public SemanticsOraclePerformance(Indexer<String> roleIndexer, Map<Integer, Integer> roleFreqs)
    {
        super(roleIndexer, roleFreqs);
        curEvalb = new EvalbImpl("Evalb LP/LR", true);
        TreebankLangParserParams tlpp = Languages.getLanguageParams(Languages.Language.English);
        tlpp.setInputEncoding("UTF-8");
        treeCollinizer = tlpp.collinizer();                   
    }
    
    @Override
    public double[] add(SemanticsWidget nbestAnalyses, SemanticsWidget goldStandard, String name)
    {
        Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldStandard.getNBestTrees()[0]));
        int maxPos = 0, curPos = 0; double curF1, maxF1 = Double.NEGATIVE_INFINITY;
        String nBestAnalysis = null;
        
        for(String analysis : nbestAnalyses.getNBestTrees())
        {
            Tree curEvalGuess = treeCollinizer.transformTree(Tree.valueOf(analysis));
            curEvalb.evaluate(curEvalGuess, evalGold, name);
            curF1 = curEvalb.getLastF1();
            if(curF1 > maxF1)
            {
                maxF1 = curF1;
                maxPos = curPos;
                nBestAnalysis = analysis;
            }
            curPos++;
        }   
        if(nBestAnalysis != null)
        {
            accuracy += maxPos == 0 ? 1 : 0;
            nbestAnalyses.getNBestTrees()[0] = nBestAnalysis; // hack: set the best analysis as the top-most in the list and call super-method to compute rest metrics        
            double[] results = Arrays.copyOf(super.add(nbestAnalyses, goldStandard, name), 3);
            results[2] = maxPos;
            return results;
        }
        else
        {
            double[] results = Arrays.copyOf(super.add(nbestAnalyses, goldStandard, name), 3);
            results[2] = -1;
            return results;
        }
    }

    @Override
    public String output()
    {
        return super.output() + "\n\nTotal accuracy: " +Fmt.D(accuracy / (double) evalb.getNum());
    }
    
    
}
