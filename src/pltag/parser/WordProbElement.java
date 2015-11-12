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
package pltag.parser;

import java.util.HashMap;
import java.util.Map;
import pltag.util.Utils;

public class WordProbElement
{

    private ParserModel parserModel;
    private String mainlex = "UNKNOWN";
    private String integrationPointLex = "UNKNOWN";
    private String mainCat = "";
    private String integrationPointCat = "";
    private String treeStructure = "";
    private static boolean recursiveSmooth = true;//false;//
    private static double wip1;//0.6376685622628872, 0.0,0.14791363070709063, 0.12652516714812645,0.08467792972666002, 0.0032147101552357773
    private static double wip2;//
    private static double wip3;//
    private static double wip4;//
    private static double wip5;//
    private static double wip6;//
    private int backoffLevel;

    private WordProbElement(ParserModel parserModel, String currentMainLex, String currentMainCat,
                            String integrationPointLex, String integrationPointCat, String elementTreeStruct, int backoffLevel)
    {
        this.parserModel = parserModel;
        if (currentMainLex != null)
        {
            this.mainlex = currentMainLex;//ParserMaster.getCutOffCorrectedMainLex(currentMainLex);
        }
        if (integrationPointLex != null)
        {
            this.integrationPointLex = integrationPointLex;
        }
        this.mainCat = currentMainCat;
        this.integrationPointCat = integrationPointCat;
        this.treeStructure = elementTreeStruct;//.substring(elementTreeStruct.lastIndexOf("\t"));
        this.backoffLevel = backoffLevel;
    }

    public WordProbElement(ParserModel parserModel, String currentMainLeaf, String integrationPointLeaf, String elementTreeStruct)
    {
        this.parserModel = parserModel;
        this.mainCat = currentMainLeaf;
        String[] current = currentMainLeaf.split("\t+");
        if (current.length == 2)
        {
            this.mainlex = current[1];//ParserMaster.getCutOffCorrectedMainLex2(current[1]);//
            this.mainCat = current[0];
        }
        String[] integPoint = integrationPointLeaf.split("\t+");
        this.integrationPointCat = integrationPointLeaf;
        if (integPoint.length == 2)
        {
            integrationPointLex = parserModel.getCutOffCorrectedMainLex(integPoint[1]);
            integrationPointCat = integPoint[0];
        }
        this.treeStructure = elementTreeStruct;//.substring(elementTreeStruct.lastIndexOf("\t"));
        this.backoffLevel = 1;
    }

    //used at reading in???
    WordProbElement(ParserModel parserModel, String tuple)
    {
        this.parserModel = parserModel;
        String[] array = tuple.split("\t");
        if (array.length == 6)
        {
            this.mainlex = array[1];
            this.mainCat = array[2];
            integrationPointLex = array[3];
            integrationPointCat = array[4];
            this.treeStructure = array[5];//.substring(elementTreeStruct.lastIndexOf("\t"));
            this.backoffLevel = Integer.parseInt(array[0]);
        }
        else if (ParserModel.verbose)
        {
            System.err.println("Was not able to interpret entry " + tuple);
        }
    }

    public String toString()
    {
//        return new StringBuilder().append(backoffLevel).append("\t").append(mainlex).append("\t").append(mainCat).append("\t").append(integrationPointLex).append("\t").append(integrationPointCat).append("\t").append(treeStructure).append("\n").toString();
        return new StringBuilder().append(backoffLevel).append("\t").append(mainlex).append("\t").append(mainCat).append("\t").append(integrationPointLex).append("\t").append(integrationPointCat).append("\t").append(treeStructure).toString();
    }

    public WordProbElement sum()
    {
        return new WordProbElement(parserModel, "WORD", "CAT", integrationPointLex, integrationPointCat, treeStructure, 0);
    }

    public boolean hasBackoff()
    {
        return backoffLevel == 5 ? false : true;        // TODO: FIX
        /*
        if (treeStructure.startsWith("POS: ") && mainlex.equals("LEXEME")){
        return false;
        }*/        
    }

    public WordProbElement getBackoff()
    {
        //backoff level 1:
        //simpler category.
/*		if (backoffLevel == 1){
        if (this.integrationPointLex.equals("UNKNOWN"))
        return new WordProbElement(this.mainlex, this.mainCat, "PRED", integrationPointCat, treeStructure, 2);
        else 
        return new WordProbElement(this.mainlex, this.mainCat, "LEX", integrationPointCat, treeStructure, 2);
        }*/
        //backoff level 2:
        if (backoffLevel == 1)
        {
            return new WordProbElement(parserModel, this.mainlex, this.mainCat, "WORD", integrationPointCat, treeStructure, 3);
        }
        //backoff level 3:
        else if (!integrationPointCat.equals("CAT"))
        {
            return new WordProbElement(parserModel, this.mainlex, this.mainCat, "WORD", "CAT", treeStructure, 4);
        }
        //backoff level 4:
        else if (!treeStructure.startsWith("POS: "))
        {
            return new WordProbElement(parserModel, this.mainlex, this.mainCat, "WORD", "CAT", "POS: " + this.mainCat, 5);
        }
        //backoff level 5:
        //else if (treeStructure.startsWith("POS: ") && !mainlex.equals("LEXEME")){
        //	return new WordProbElement("LEXEME", this.mainCat, "WORD", "CAT", "POS: "+this.mainCat, 6);
        //}
        else
        {
            System.err.println("no more backoff possible for " + this);
        }
        return null;
    }

    public static void readMap(String filename, Map<String, Integer> map, boolean combineNNVBCats)
    {
       boolean needCalculateUniq = recursiveSmooth;       
       for(String line : Utils.readLines(filename))
       {
           /*String[] slots = line.split("\t");
            if (slots.length != 7) 
            if (ParserModel.verbose) System.out.println("wrong lex!");
             */
            int index = line.lastIndexOf("\t=");

            //String[] hash = line.split("\t=");
            String key = line.substring(0, index);
            key = Utils.getCatInventory(key, combineNNVBCats);
            if (map.containsKey(key))
            {
                map.put(key, map.get(key) + Integer.parseInt(line.substring(index + 2)));
            }
            else
            {
                map.put(key, Integer.parseInt(line.substring(index + 2)));
            }
            if (needCalculateUniq && key.startsWith("u0"))
            {
                needCalculateUniq = false;
            }
       }
       if (needCalculateUniq)
       {
           calculateUniq(map);
       }
    }

    public String getKey()
    {
//        return toString().trim();
        return toString();
    }

    public static void estimateInterpol(ParserModel parserModel, Map<String, Integer> bigFreqMapWord)
    {
        double ip1 = 0.0;
        double ip2 = 0.0;
        double ip3 = 0.0;
        double ip4 = 0.0;
        double ip5 = 0.0;
        double ip6 = 0.0;
        for (String tuple : bigFreqMapWord.keySet())
        {
            WordProbElement t = new WordProbElement(parserModel, tuple);
            double maxprob = 0.0;
            int level = 0;
            int freq = bigFreqMapWord.get(tuple);
            if (t.backoffLevel == 1)
            {
                while (true)
                {
                    if (bigFreqMapWord.get(t.sum().getKey()) > 1)
                    {
                        double d = (bigFreqMapWord.get(t.getKey()).doubleValue() - 1.0) / (bigFreqMapWord.get(t.sum().getKey()).doubleValue() - 1);
                        if (d > maxprob)
                        {// && t.backoffLevel !=1){
                            level = t.backoffLevel;
                            maxprob = d;
                        }
                    }
                    if (!t.hasBackoff())
                    {
                        break;
                    }
                    else
                    {
                        t = t.getBackoff();
                    }
                }

                if (level == 1)
                {
                    ip1 += freq;
                }
                else if (level == 2)
                {
                    ip2 += freq;
                }
                else if (level == 3)
                {
                    ip3 += freq;
                }
                else if (level == 4)
                {
                    ip4 += freq;
                }
                else if (level == 5)
                {
                    ip5 += freq;
                }
                else
                {
                    ip6++;
                }
            }
        }
        wip1 = ip1 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6);
        wip2 = ip2 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6);
        wip3 = ip3 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6);
        wip4 = ip4 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6);
        wip5 = ip5 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6);
        wip6 = ip6 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6);

    }

    public static void setInterpol(double d, double e, double f, double g, double h, double i)
    {
        wip1 = d;
        wip2 = e;
        wip3 = f;
        wip4 = g;
        wip5 = h;
        wip6 = i;
    }

    public double getSmoothedProb(Map<String, Integer> bigFreqMap)
    {
        if (recursiveSmooth)
        {
            return recursiveSmooth(bigFreqMap, 0);
        }
        return getBrantsSmooth(bigFreqMap);
    }

    private double getBrantsSmooth(Map<String, Integer> bigFreqMapWord)
    {
        double probability = 1 * wip6;
        WordProbElement currentLevel = this;
        while (true)
        {
            if (bigFreqMapWord.containsKey(currentLevel.getKey()) && !currentLevel.mainlex.equals("UNK"))
            {// && currentLevel.backoffLevel!=1){
                double p = currentLevel.getProbBrants(bigFreqMapWord) * currentLevel.getInterpolationFactor();
                probability += p;
                //	if (currentLevel.backoffLevel == 1) System.out.println("bolevel 1 found"); 
            }
            if (currentLevel.hasBackoff())
            {
                currentLevel = currentLevel.getBackoff();
            }
            else
            {
                break;
            }
        }
        /*		
        if (probability == 0.0){
        //System.err.println("can't estimate probability of "+ thisTreeProb);
        probability = 0.00000001;
        }
         */
        return probability;
    }

    public double recursiveSmooth(Map<String, Integer> bigFreqMap, double occurAtLastLevel)
    {
        double p;
        double occurAtCurrentLevel;
        String sumkey = sum().getKey();
        if (bigFreqMap.containsKey(sumkey) && bigFreqMap.containsKey("u" + sumkey))
        {
            occurAtCurrentLevel = bigFreqMap.get(sumkey) * 1.0;
            double uniqAtCurrentLevel = bigFreqMap.get("u" + sumkey) * 1.0;
            double lambda = (1.0 - (occurAtLastLevel) / occurAtCurrentLevel) * (1.0 / (1.0 + (5.0 * uniqAtCurrentLevel / occurAtCurrentLevel)));
            if (hasBackoff())
            {
                p = lambda * getProb(bigFreqMap) + (1 - lambda) * getBackoff().recursiveSmooth(bigFreqMap, occurAtCurrentLevel);
            }
            else
            {
                p = lambda * getProb(bigFreqMap) + (1 - lambda) * 1.0 / (double)parserModel.getLexiconSize(); // TODO: FIX
//                p = lambda * getProb(bigFreqMap) + (1 - lambda) * 1 /parserModel.getLexicon().getLexSize(); // TODO: FIX
            }
            //	if (this.backoffLevel==5 && getProb(bigFreqMap)==0){System.out.print("\nOOV:"+this);}
            //System.out.println(this.backoffLevel+"\t"+p+"\t"+lambda);
        }
        else
        {
            if (hasBackoff())
            {
                p = getBackoff().recursiveSmooth(bigFreqMap, 0);
            }
            else
            {
                //System.out.println("backoff to last step.");
                p = 1.0 / (double) parserModel.getLexiconSize();
//                p = 1 / parserModel.getLexicon().getLexSize(); // TODO: FIX
            }
        }
        return p;
    }

    public static void calculateUniq(Map<String, Integer> bigFreqMap)
    {
        Map<String, Integer> uniqCounter = new HashMap<String, Integer>();
        for (String key : bigFreqMap.keySet())
        {
            if (!key.startsWith("0"))
            {
                WordProbElement wpe = new WordProbElement(null, key); // TODO: null is wrong
                String sum = wpe.sum().getKey();
                String newsumid = "u" + sum;
                if (uniqCounter.containsKey(newsumid))
                {
                    uniqCounter.put(newsumid, uniqCounter.get(newsumid) + 1);
                }
                else
                {
                    uniqCounter.put(newsumid, 1);
                }
            }
        }
        bigFreqMap.putAll(uniqCounter);
    }

    private double getInterpolationFactor()
    {
        switch(backoffLevel)
        {
            case 1 : return wip1;
            case 2 : return wip2;
            case 3 : return wip3;
            case 4 : return wip4;
            case 5 : return wip5;
            case 0 : default : return 0.0;
        }        
    }

    private double getProb(Map<String, Integer> bigFreqMap)
    {
        //if (backoffLevel == 1 && this.integrationPointLex.equals("UNKNOWN")) return 0;
        double sum = bigFreqMap.get(sum().getKey()).doubleValue();
        if (sum > 1 && bigFreqMap.containsKey(getKey()))
        {
            return (bigFreqMap.get(getKey()).doubleValue()) / sum;
        }
        else
        {
            return 0;
        }
    }

    private double getProbBrants(Map<String, Integer> bigFreqMap)
    {
        //if (backoffLevel == 1 && this.integrationPointLex.equals("UNKNOWN")) return 0;
        double sum = bigFreqMap.get(sum().getKey()).doubleValue();

        if (sum > 1 && bigFreqMap.containsKey(getKey()))
        {
            //System.out.println(bigFreqMap.get(getKey()).doubleValue() +"-.5 /"+ sum+"-.5");
            return (bigFreqMap.get(getKey()).doubleValue() - 1)
                    / (sum - 1);
        }
        else
        {
            return 0;
        }
    }
}
