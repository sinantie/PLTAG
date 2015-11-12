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

/**
 *
 * @author konstas
 */
public class BracketPerformance<Widget> extends Performance<String>
{
    private final EvalbImpl evalb;
    private final TreeTransformer treeCollinizer;
    private double totalEvalbF1;
        
    
    public BracketPerformance()
    {
        evalb = new EvalbImpl("Evalb LP/LR", true);
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
    public double[] add(String predAnalysis, String goldStandard, String name)
    {        
        Tree evalGuess = treeCollinizer.transformTree(Tree.valueOf(predAnalysis));
        Tree evalGold = treeCollinizer.transformTree(Tree.valueOf(goldStandard));    
//        if (evalGold == null || evalGuess == null)
//        {
//            LogInfo.error(name + ": Cannot compare against a null gold or guess tree!\n");
//            return 0.0d;
//
//        }
//        if (evalGuess.yield().size() != evalGold.yield().size())
//        {
//            try
//            {
//                evalGuess = Utils.removeEmptyNodes(evalGuess);               
//            }
//            catch(Exception e)
//            {
//                LogInfo.error("Example " + name);
//                e.printStackTrace();
//            }
//        }
        evalb.evaluate(evalGuess, evalGold, name);
        totalEvalbF1 = evalb.getEvalbF1();
        return new double[] {evalb.getLastF1()};
    }

    @Override
    public String output()
    {           
        return evalb.summary();
    }   
}
