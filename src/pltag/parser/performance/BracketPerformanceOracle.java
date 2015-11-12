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
package pltag.parser.performance;

import edu.stanford.nlp.international.Languages;
import edu.stanford.nlp.international.Languages.Language;
import edu.stanford.nlp.parser.lexparser.TreebankLangParserParams;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeTransformer;
import fig.basic.Fmt;

/**
 *
 * @author konstas
 */
public class BracketPerformanceOracle<Widget> extends Performance<String[]>
{
    private final EvalbImpl evalb, curEvalb;
    private final TreeTransformer treeCollinizer;
    private double totalEvalbF1, accuracy;
        
    
    public BracketPerformanceOracle()
    {
        evalb = new EvalbImpl("Evalb LP/LR", true);
        curEvalb = new EvalbImpl("Evalb LP/LR", true);
        TreebankLangParserParams tlpp = Languages.getLanguageParams(Language.English);
        tlpp.setInputEncoding("UTF-8");
        treeCollinizer = tlpp.collinizer();                   
    }
    
    @Override
    public double getAccuracy()
    {
        return totalEvalbF1;
    }

    @Override
    public double[] add(String[] nBestAnalyses, String[] goldStandard, String name)
    {        
        Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldStandard[0]));
        int maxPos = 0, curPos = 0; double curF1, maxF1 = Double.NEGATIVE_INFINITY;
        String nBestAnalysis = null;
        for(String analysis : nBestAnalyses)
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
            Tree evalGuess = treeCollinizer.transformTree(Tree.valueOf(nBestAnalysis));
            evalb.evaluate(evalGuess, evalGold, name);
            totalEvalbF1 = evalb.getEvalbF1();
            accuracy += maxPos == 0 ? 1 : 0;
        }        
        
        return new double[] {evalb.getLastF1(), maxPos};
    }

    @Override
    public String output()
    {           
        return evalb.summary() + "\n\nTotal accuracy: " +Fmt.D(accuracy / (double) evalb.getNum());
    }   
}
