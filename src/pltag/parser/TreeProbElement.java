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

public class TreeProbElement
{

    private static boolean recursiveSmooth = true;//false;//
    private String operationName = "";
    private String conditionalOnCat = "";
    private String elementaryTree = "";
    private String shadowStatus = "";
    //private String adjNodePosition = "";
    private String traceMark = "00";
    private String integElemTree = "";
    private String prevAndLeftmostTAG = "";
    private int posOfIpiNodeInIpiTree = -1;
    private String clusternumber = "";
    private String ipiPos = "";
    private String ipiWord = "";
    private static double tip1;//0.7763688883462904, 0.08928869439967384, 0.04901195421712926, 0.017292341259129748,0.06091576941902258, 0.007122352358754198
    private static double tip2;//0.7825862460980508, 0.08073940747494184, 0.05068305812014252, 0.017978927514188142, 0.06089000843392251, 0.007122352358754198
    private static double tip3;//
    private static double tip4;// 
    private static double tip5;//
    private static double tip6;//
    private static double tip7;//
    private int backoffLevel;
    //public static double[] count1 ={0,0,0,0,0,0,0};

    public TreeProbElement(String operationName, String conditionalOn, String elementaryTree,//String adjNodePosition, 
            boolean traceAtLeft, boolean traceAtRight, String integElemTree, int posOfIpiNodeInTree,
            String integrationLambda, String prevAndLeftmostTAG, String clusternumber)
    {
        if (operationName.equals("verify"))
        {
            shadowStatus = "verify";//"S";
            this.ipiPos = "-";//conditionalOn;
            this.ipiWord = "PRED";
            this.integElemTree = conditionalOn;
            this.posOfIpiNodeInIpiTree = 0;//posOfIpiNodeInTree;
            this.prevAndLeftmostTAG = "-";
            this.clusternumber = "0/0";
        }
        else if (operationName.equals("initial"))
        {
            shadowStatus = "initial";//"F";
            this.ipiPos = "NA";
            this.ipiWord = "NA";
            this.integElemTree = "NA";
            this.posOfIpiNodeInIpiTree = -1;
            this.prevAndLeftmostTAG = "-";
            this.clusternumber = "0/0";
        }
        else
        {
            shadowStatus = operationName;//.substring(operationName.length()-2);
            int i = integrationLambda.indexOf("\t");
            if (i > 0)
            {
                this.ipiPos = integrationLambda.substring(0, i);
                this.ipiWord = integrationLambda.substring(i + 1);
            }
            else
            {
                this.ipiPos = integrationLambda;
                this.ipiWord = "PRED";
            }
            this.prevAndLeftmostTAG = prevAndLeftmostTAG;
            this.integElemTree = integElemTree;
            this.posOfIpiNodeInIpiTree = posOfIpiNodeInTree;
            this.clusternumber = clusternumber;
        }
        if (traceAtLeft && !traceAtRight)
        {
            traceMark = "10";
        }
        else if (!traceAtLeft && traceAtRight)
        {
            traceMark = "01";
        }
        else if (traceAtLeft && traceAtRight)
        {
            traceMark = "11";
        }

        this.operationName = operationName;
        this.conditionalOnCat = conditionalOn;
        this.elementaryTree = elementaryTree;//.substring(elementaryTree.lastIndexOf("\t"));
        //	this.adjNodePosition =adjNodePosition;//"?";// 
        this.backoffLevel = 1;
    }

    //*/
    private TreeProbElement(String operationName, String shadowStatus, String conditionalOn, String elementaryTree, //String adjNodePosition, 
            int backofflevel, String traceMark, String integElemTree, int poiniit,
            String ipipos, String ipiWord, String prevAndLeftmostTAG, String clusterno)
    {
        this.operationName = operationName;
        this.conditionalOnCat = conditionalOn;
        this.elementaryTree = elementaryTree;//.substring(elementaryTree.lastIndexOf("\t"));
        this.shadowStatus = shadowStatus;
        //	this.adjNodePosition = adjNodePosition;
        this.backoffLevel = backofflevel;
        this.traceMark = traceMark;
        this.integElemTree = integElemTree;
        this.posOfIpiNodeInIpiTree = poiniit;
        this.ipiPos = ipipos;
        this.ipiWord = ipiWord;
        this.prevAndLeftmostTAG = prevAndLeftmostTAG;
        this.clusternumber = clusterno;
    }

    TreeProbElement(String readIn)
    {
        String[] array = readIn.split("\t");
        if (array.length == 12)
        {
            this.backoffLevel = Integer.parseInt(array[0]);
            this.shadowStatus = array[1];
            //	this.adjNodePosition =array[2];// "?";//
            this.conditionalOnCat = array[3];
            this.integElemTree = array[4];
            this.posOfIpiNodeInIpiTree = Integer.parseInt(array[5]);
            this.ipiPos = array[6];
            this.ipiWord = array[7];
            this.elementaryTree = array[8];
            this.traceMark = array[9];
            this.prevAndLeftmostTAG = array[10];
            this.clusternumber = array[11];
        }
        else if (array.length == 11)
        {
            this.backoffLevel = Integer.parseInt(array[0]);
            this.shadowStatus = array[1];
            //	this.adjNodePosition =array[2];// "?";//
            this.conditionalOnCat = array[3];
            this.integElemTree = array[4];
            this.posOfIpiNodeInIpiTree = Integer.parseInt(array[5]);
            this.ipiPos = array[6];
            this.ipiWord = array[7];
            this.elementaryTree = array[8];
            this.traceMark = array[9];
            this.prevAndLeftmostTAG = array[10];
        }
        else if (array.length == 10)
        {
            this.backoffLevel = Integer.parseInt(array[0]);
            this.shadowStatus = array[1];
            //	this.adjNodePosition =array[2];// "?";//
            this.conditionalOnCat = array[3];
            this.integElemTree = array[4];
            this.posOfIpiNodeInIpiTree = Integer.parseInt(array[5]);
            this.ipiPos = array[6];
            this.ipiWord = array[7];
            this.elementaryTree = array[8];
            this.traceMark = array[9];
        }
        else if (array.length == 2)
        {
            // all and non stats
        }
        else if (ParserModel.verbose)
        {
            System.err.println("Entry not recognized for " + readIn);
        }
    }

    private double getInterpolationFactor()
    {
        if (backoffLevel == 1)
        {
            return tip1;
        }
        else if (backoffLevel == 2)
        {
            return tip2;
        }
        else if (backoffLevel == 3)
        {
            return tip3;
        }
        else if (backoffLevel == 4)
        {
            return tip4;
        }
        else if (backoffLevel == 5)
        {
            return tip5;
        }
        else if (backoffLevel == 6)
        {
            return tip6;
        }
        else
        {
            return tip7;
        }
    }

    public String toString()
    {
        if (ParserModel.useLeftMost && ParserModel.useClusterCode)
        {
            return new StringBuilder().append(backoffLevel).append("\t")
                    .append(shadowStatus).append("\t")
                    //.append(adjNodePosition).append("\t")
                    .append("-\t")
                    .append(conditionalOnCat).append("\t")
                    .append(integElemTree).append("\t")
                    .append(posOfIpiNodeInIpiTree).append("\t")
                    .append(ipiPos).append("\t")
                    .append(ipiWord).append("\t")
                    .append(elementaryTree).append("\t")
                    .append(this.traceMark).append("\t")
                    .append(this.prevAndLeftmostTAG).append("\t")
                    .append(this.clusternumber)
                    .toString();
        }
        else if (!ParserModel.useLeftMost && ParserModel.useClusterCode)
        {
            return new StringBuilder().append(backoffLevel).append("\t")
                    .append(shadowStatus).append("\t")
                    .append("-\t")
                    //		.append(adjNodePosition).append("\t")
                    .append(conditionalOnCat).append("\t")
                    .append(integElemTree).append("\t")
                    .append(posOfIpiNodeInIpiTree).append("\t")
                    .append(ipiPos).append("\t")
                    .append(ipiWord).append("\t")
                    .append(elementaryTree).append("\t")
                    .append(this.traceMark).append("\t")
                    .append(this.clusternumber)
                    .toString();
        }
        else if (ParserModel.useLeftMost && !ParserModel.useClusterCode)
        {
            return new StringBuilder().append(backoffLevel).append("\t")
                    .append(shadowStatus).append("\t")
                    .append("-\t")//	.append(adjNodePosition).append("\t")
                    .append(conditionalOnCat).append("\t")
                    .append(integElemTree).append("\t")
                    .append(posOfIpiNodeInIpiTree).append("\t")
                    .append(ipiPos).append("\t")
                    .append(ipiWord).append("\t")
                    .append(elementaryTree).append("\t")
                    .append(this.traceMark).append("\t")
                    .append(this.prevAndLeftmostTAG)
                    .toString();
        }
        else //if (!ParserModel.useLeftMost && !ParserModel.useClusterCode)
        {
            return new StringBuilder().append(backoffLevel).append("\t")
                    .append(shadowStatus).append("\t")
                    .append("-\t")//.append(adjNodePosition).append("\t")
                    .append(conditionalOnCat).append("\t")
                    .append(integElemTree).append("\t")
                    .append(posOfIpiNodeInIpiTree).append("\t")
                    .append(ipiPos).append("\t")
                    .append(ipiWord).append("\t")
                    .append(elementaryTree).append("\t")
                    .append(this.traceMark).append("\t")
                    .toString();
        }
    }

    public String getOperation()
    {
        return operationName;
    }

    public String getConditional()
    {
        return conditionalOnCat;
    }

    public TreeProbElement sum()
    {
        //if (backoffLevel == 4)
        //return new TreeProbElement(this.operationName, this.shadowStatus, this.conditionalOnCat, "TREE", "?", 5);
        return new TreeProbElement(this.operationName, this.shadowStatus, this.conditionalOnCat, "TREESTRUCT", //adjNodePosition,
                0, traceMark, this.integElemTree, this.posOfIpiNodeInIpiTree, ipiPos, ipiWord, this.prevAndLeftmostTAG, this.clusternumber);
    }

    public boolean hasBackoff()
    {
        if (backoffLevel < 6)
        {
            return true;
        }
        return false;
        /*
         if (!shadowStatus.equals("_")){
         return true;
         }
         else if (!adjNodePosition.equals("?/?")){
         return true;
         }
         return false;
         */
    }

    public TreeProbElement getBackoff()
    {
        if (backoffLevel == 1)
        {
            return new TreeProbElement(this.operationName, "_", this.conditionalOnCat, this.elementaryTree, //adjNodePosition, 
                    backoffLevel + 1, traceMark, this.integElemTree, this.posOfIpiNodeInIpiTree, ipiPos, "LEX", this.prevAndLeftmostTAG, this.clusternumber);
        }
        else if (backoffLevel == 2)
        {
            String leftMostHalf = "?";
            if (this.prevAndLeftmostTAG != null && this.prevAndLeftmostTAG.indexOf("+") > -1)
            {
                leftMostHalf = this.prevAndLeftmostTAG.substring(this.prevAndLeftmostTAG.indexOf("+"));
            }
            return new TreeProbElement(this.operationName, "_", this.conditionalOnCat, this.elementaryTree, //"?",
                    backoffLevel + 1, "TM", this.integElemTree, this.posOfIpiNodeInIpiTree, ipiPos, "LEX", leftMostHalf, this.clusternumber);
        }
        else if (backoffLevel == 3)
        {
            return new TreeProbElement(this.operationName, "_", this.conditionalOnCat, this.elementaryTree, //"?", 
                    backoffLevel + 1, "TM", this.integElemTree, this.posOfIpiNodeInIpiTree, "POS", "LEX", "LEFT", this.clusternumber);
        }
        else if (backoffLevel == 4)
        {
            return new TreeProbElement(this.operationName, "_", this.conditionalOnCat, this.elementaryTree,// "?", 
                    backoffLevel + 1, "TM", "STRUCT", -1, "POS", "LEX", "LEFT", "CNO");
        }
        else if (backoffLevel == 5)
        {
            return new TreeProbElement(this.operationName, "_", "-", this.elementaryTree, //"?", 
                    backoffLevel + 1, "TM", "STRUCT", -1, "POS", "LEX", "LEFT", "CNO");
        }
        else
        {
            System.out.println("error in backoff levels.");
            return this;
        }
    }

    public static void readMap(String filename, Map<String, Integer> map, boolean combineNNVBCats)
    {
        boolean needCalculateUniq = recursiveSmooth;
        for (String line : Utils.readLines(filename))
        {
            String[] hash = line.split("\t=");
            String key = hash[0];
            if (hash.length > 2)
            {
                for (int i = 1; i < hash.length - 1; i++)
                {
                    key += hash[i];
                }
                hash[1] = hash[hash.length - 1];
            }
            key = Utils.getCatInventory(key, combineNNVBCats);
            if (map.containsKey(key))
            {
                map.put(key, map.get(key) + Integer.parseInt(hash[1]));
            }
            else
            {
                try
                {
                    map.put(key, Integer.parseInt(hash[1]));
                } catch (Exception e)
                {
                    System.out.println(line);
                }
            }
            if (needCalculateUniq && key.startsWith("u"))
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
        //	adjNodePosition = adjPos.matcher(adjNodePosition).replaceFirst("");
        return this.toString().trim();
    }

    public double getSmoothedProb(Map<String, Integer> bigFreqMapTree)
    {
        if (recursiveSmooth)
        {
            return recursiveSmooth(bigFreqMapTree, 0);
        }
        return getBrantsSmooth(bigFreqMapTree);
    }

    private double getBrantsSmooth(Map<String, Integer> bigFreqMapTree)
    {
        double probability = 1 * tip7;
        //	count1[6]++;
        TreeProbElement currentLevel = this;
        //	System.out.println(currentLevel);
        while (true)
        {
            if (bigFreqMapTree.containsKey(currentLevel.getKey()))
            {
                //			count1[currentLevel.backoffLevel]++;
                //			System.out.print(currentLevel.backoffLevel);
                probability = probability + currentLevel.getProbBrants(bigFreqMapTree) * currentLevel.getInterpolationFactor();
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
//		System.out.println(probability);
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
                p = lambda * getProb(bigFreqMap) + (1 - lambda) * 1.0 / (double) Lexicon.getNumOfTreeTemps();
//                p = lambda * getProb(bigFreqMap) + (1 - lambda) * 1 / Lexicon.getNumOfTreeTemps();
            }
            //if (!this.elementaryTree.equals("NONEADJ"))System.out.println(this.backoffLevel+"\tp:"+getProb(bigFreqMap)+"\tl:"+lambda+"\t"+this);
            ///if (this.backoffLevel==6 && getProb(bigFreqMap)==0){
            //System.out.print("\nOOTV:"+this);}
        }
        else
        {
            //System.out.println("no sumkey");
            if (hasBackoff())
            {
                p = getBackoff().recursiveSmooth(bigFreqMap, 0);
            }
            else
            {
                p = 1.0 / (double) Lexicon.getNumOfTreeTemps();
//                p = 1 / Lexicon.getNumOfTreeTemps();
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
                TreeProbElement tpe = new TreeProbElement(key);
                String sum = tpe.sum().getKey();
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

    private double getProbBrants(Map<String, Integer> bigFreqMap)
    {
        double sum = bigFreqMap.get(sum().getKey()).doubleValue();
        if (sum > 1 && bigFreqMap.containsKey(getKey()))
        {
            //System.out.println(this.backoffLevel+"\t"+bigFreqMap.get(getKey()).doubleValue() +"-.5 /"+ sum+"-.5");
            return (bigFreqMap.get(getKey()).doubleValue() - 1) / (sum - 1);
        }
        return 0.0;
    }

    private double getProb(Map<String, Integer> bigFreqMap)
    {
        double sum = bigFreqMap.get(sum().getKey()).doubleValue();
        if (bigFreqMap.containsKey(getKey()))
        {
            return (bigFreqMap.get(getKey()).doubleValue()) / sum;
        }
        return 0.0;
    }

    public static void estimateInterpol(Map<String, Integer> bigFreqMapTree)
    {
        double ip1 = 0.0;
        double ip2 = 0.0;
        double ip3 = 0.0;
        double ip4 = 0.0;
        double ip5 = 0.0;
        double ip6 = 0.0;
        double ip7 = 0.0;
        for (String tuple : bigFreqMapTree.keySet())
        {
            TreeProbElement t = new TreeProbElement(tuple);
            int freq = bigFreqMapTree.get(tuple);
            double maxprob = 0.0;
            int level = 0;
            if (t.backoffLevel == 1)
            {
                //double attachp = t.getAttachProb(bigFreqMapTree);
                //if (attachp > maxprob){
                //	level = 5;
                //	maxprob = attachp;
                //}
                while (true)
                {
                    if (bigFreqMapTree.get(t.sum().getKey()) > 1)
                    {
                        double d = (bigFreqMapTree.get(t.getKey()).doubleValue() - 1.0)
                                / (bigFreqMapTree.get(t.sum().getKey()).doubleValue() - 1.0);
                        if (d > maxprob)
                        {// && t.backoffLevel!=1){
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
                else if (level == 6)
                {
                    ip6 += freq;
                }
                else
                {
                    ip7++;
                }
            }
        }
        tip1 = ip1 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6 + ip7);
        tip2 = ip2 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6 + ip7);
        tip3 = ip3 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6 + ip7);
        tip4 = ip4 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6 + ip7);
        tip5 = ip5 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6 + ip7);
        tip6 = ip6 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6 + ip7);
        tip7 = ip7 / (ip1 + ip2 + ip3 + ip4 + ip5 + ip6 + ip7);
    }

    public static void setInterpol(double d, double e, double f, double g, double h, double i, double j)
    {
        tip1 = d;
        tip2 = e;
        tip3 = f;
        tip4 = g;
        tip5 = h;
        tip6 = i;
        tip7 = j;
    }

    public boolean isNoneAdj()
    {
        if (this.backoffLevel == 1 && this.elementaryTree.equals("NONE"))
        {
            return true;
        }
        return false;
    }
}
