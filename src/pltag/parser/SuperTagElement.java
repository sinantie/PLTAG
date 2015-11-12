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

import java.io.Serializable;
import java.util.Map;
import java.util.TreeSet;
import pltag.util.Utils;

public class SuperTagElement implements Serializable
{

    private static final long serialVersionUID = 1L;
    
    private SuperTagFringe prefixFringe;
    private String nextPosCat; // substNodeOfPrefixTree
    private String predTreeFringe; // 1 of the Nodes on Fringe and POS tag of next word
    private String currentPOSCat;//category of last node on spine
    private static double sip1;//
    private static double sip2;//
    private static double sip3;//
    private int backoffLevel;

    public SuperTagElement() {}
    
    public SuperTagElement(SuperTagFringe prefixFringe, String nextPosCat, String currentPosCat, String predTreeFringe, int backoffLevel)
    {
        this.prefixFringe = prefixFringe;
        this.nextPosCat = nextPosCat;
        this.currentPOSCat = currentPosCat;
        this.predTreeFringe = predTreeFringe;
        this.backoffLevel = backoffLevel;
    }
    
    public SuperTagElement(Fringe prefixFringe, String nextPosCat, String currentPosCat, String predTreeFringe)
    {        
        this(new SuperTagFringe(prefixFringe), nextPosCat, currentPosCat, predTreeFringe, 1);
    }
    
    public SuperTagElement(String prefixFringe, String nextPosCat, String currentPosCat, String predTreeFringe, int backoffLevel)
    {
        this(new SuperTagFringe(prefixFringe, false), nextPosCat, currentPosCat, predTreeFringe, backoffLevel);
    }        

    public SuperTagElement(String readIn)
    {        
        String[] array = readIn.split("\t");
        this.backoffLevel = Integer.parseInt(array[0]);        
        this.prefixFringe = new SuperTagFringe(array[1]);
        this.nextPosCat = array[2];
        this.currentPOSCat = array[3];
        this.predTreeFringe = array[4];
//        this.currentPOSCat = backoffLevel == 1 ? array[4] : array[3];
//        this.predTreeFringe = backoffLevel == 1 ? array[3] : array[4];
    }
  
    private double getInterpolationFactor()
    {
        switch(backoffLevel)
        {
            case 1 : return sip1;
            case 2 : return sip2;
            default : return 0.0;
        }        
    }

    public String toString()
    {
//        return new StringBuilder().append(backoffLevel).append("\t").append(prefixFringe).
//                append("\t").append(nextPosCat).append("\t").append(currentPOSCat).
//                append("\t").append(predTreeFringe).append("\n").toString();
        return new StringBuilder().append(backoffLevel).append("\t").append(prefixFringe).
                append("\t").append(nextPosCat).append("\t").append(currentPOSCat).
                append("\t").append(predTreeFringe).toString();
    }

    public SuperTagElement sum()
    {
        return new SuperTagElement(prefixFringe, nextPosCat, "POS", "FRINGE", 0);
    }

    public boolean hasBackoff()
    {
        return backoffLevel < 2;
    }

    public SuperTagElement getBackoff(String ipi)
    {
        if (backoffLevel == 1 || backoffLevel == 2)
        {
            return new SuperTagElement(ipi, nextPosCat, currentPOSCat, predTreeFringe, 2);
        }
//        else if (backoffLevel == 2)
//        {
//            return new SuperTagElement(ipi, nextPosCat, currentPOSCat, predTreeFringe, 2);
//        }
        else
        {
            System.err.println("error in backoff levels.");
            return this;
        }
    }

//    public static void estimateInterpol(Map<SuperTagElement, Integer> bigFreqMapWord)
//    {
//        double ip1 = 0.0;
//        double ip2 = 0.0;
//        double ip3 = 0.0;
//        for (SuperTagElement tuple : bigFreqMapWord.keySet())
//        {            
//            double maxprob = 0.0;
//            int level = 0;
//            int freq = bigFreqMapWord.get(tuple);
//            if (tuple.backoffLevel == 1)
//            {
//                if (bigFreqMapWord.get(tuple.sum().getKey()) > 1)
//                {
//                    double d = (bigFreqMapWord.get(tuple.getKey()).doubleValue() - 1.0) / (bigFreqMapWord.get(tuple.sum().getKey()).doubleValue() - 1);
//                    if (d > maxprob)
//                    {
//                        level = tuple.backoffLevel;
//                        maxprob = d;
//                    }
//                }
//                String substNode = tuple.prefixFringe.substring(tuple.prefixFringe.lastIndexOf(":") + 1);
//                String openRight = tuple.prefixFringe.substring(1, tuple.prefixFringe.indexOf("]["));
//                String openLeft = tuple.prefixFringe.substring(tuple.prefixFringe.indexOf("][") + 2, tuple.prefixFringe.indexOf("]:"));
//                String[] or = openRight.split(", ");
//                String[] ol = openLeft.split(", ");
//                if (tuple.prefixFringe.equals("[][]:null"))
//                {
//                    substNode = "SoS";
//                }
//                for (String cat : or)
//                {//each of cats on fringe
//                    if (cat.equals(""))
//                    {
//                        break;
//                    }
//                    tuple = tuple.getBackoff(cat);
//                    if (bigFreqMapWord.containsKey(tuple.sum().getKey()) && bigFreqMapWord.containsKey(tuple.getKey()))
//                    {
//                        double d = (bigFreqMapWord.get(tuple.getKey()).doubleValue() - 1.0) / (bigFreqMapWord.get(tuple.sum().getKey()).doubleValue() - 1);
//                        if (d > maxprob)
//                        {
//                            level = tuple.backoffLevel;
//                            maxprob = d;
//                        }
//                    }
//                }
//                for (String cat : ol)
//                {//each of cats on fringe
//                    if (cat.equals(""))
//                    {
//                        break;
//                    }
//                    tuple = tuple.getBackoff(cat);
//                    if (bigFreqMapWord.containsKey(tuple.sum().getKey()) && bigFreqMapWord.containsKey(tuple.getKey()))
//                    {
//                        double d = (bigFreqMapWord.get(tuple.getKey()).doubleValue() - 1.0) / (bigFreqMapWord.get(tuple.sum().getKey()).doubleValue() - 1);
//                        if (d > maxprob)
//                        {
//                            level = tuple.backoffLevel;
//                            maxprob = d;
//                        }
//                    }
//                }
//                if (!substNode.equals("null"))
//                {
//                    tuple = tuple.getBackoff(substNode);
//                    if (bigFreqMapWord.containsKey(tuple.sum().getKey()) && bigFreqMapWord.containsKey(tuple.getKey()))
//                    {
//                        double d = (bigFreqMapWord.get(tuple.getKey()).doubleValue() - 1.0) / (bigFreqMapWord.get(tuple.sum().getKey()).doubleValue() - 1);
//                        if (d > maxprob)
//                        {
//                            level = tuple.backoffLevel;
//                            maxprob = d;
//                        }
//                    }
//                }
//
//                if (level == 1)
//                {
//                    ip1 += freq;
//                }
//                else if (level == 2)
//                {
//                    ip2 += freq;
//                }
//                else
//                {
//                    ip3++;
//                }
//            }
//        }
//        sip1 = ip1 / (ip1 + ip2 + ip3);
//        sip2 = ip2 / (ip1 + ip2 + ip3);
//        sip3 = ip3 / (ip1 + ip2 + ip3);
//    }

    public static void readMap(String filename, Map<SuperTagElement, Integer> map, boolean combineNNVBCats)
    {
//        boolean needCalculateUniq = false;//true;
        for (String line : Utils.readLines(filename))
        {
            String[] hash = line.split("\t=");
            SuperTagElement key = new SuperTagElement(Utils.getCatInventory(hash[0], combineNNVBCats));
            if (map.containsKey(key))
            {
                map.put(key, map.get(key) + Integer.parseInt(hash[1]));
            }
            else
            {
                map.put(key, Integer.parseInt(hash[1]));
            }
//            if (needCalculateUniq && key.startsWith("u"))
//            {
//                needCalculateUniq = false;
//            }
        }
//        if (needCalculateUniq)
//        {
//            calculateUniq(map);
//        }
    }

//    private static void calculateUniq(Map<String, Integer> bigFreqMap)
//    {
//        Map<String, Integer> uniqCounter = new HashMap<String, Integer>();
//        for (String key : bigFreqMap.keySet())
//        {
//            if (!key.startsWith("0"))
//            {
//                SuperTagElement tpe = new SuperTagElement(key);
//                String sum = tpe.sum().getKey();
//                String newsumid = "u" + sum;
//                if (uniqCounter.containsKey(newsumid))
//                {
//                    uniqCounter.put(newsumid, uniqCounter.get(newsumid) + 1);
//                }
//                else
//                {
//                    uniqCounter.put(newsumid, 1);
//                }
//            }
//        }
//        bigFreqMap.putAll(uniqCounter);
//    }

    public String getKey()
    {
//        return this.toString().trim();
        return this.toString();
    }

//	/*
    public double getSmoothedProb(Map<SuperTagElement, Integer> freqMap)
    {
        double probability = sip3;//1 * sip3;
        SuperTagElement currentLevel = this;
        if (currentLevel.backoffLevel != 1)
        {
            System.out.println("unexpected backoff level.");
        }
        if (freqMap.containsKey(currentLevel))
//        if (freqMap.containsKey(currentLevel.getKey()))
        {//0	[MD][VP]:null	PRP-VB-DT-MD-	POS	FRINGE=2
            probability += currentLevel.getProb(freqMap) * currentLevel.getInterpolationFactor();
        }               
//        String substNode = prefixFringe.substring(prefixFringe.lastIndexOf(":") + 1);
//        String openRight = prefixFringe.substring(1, prefixFringe.indexOf("]["));
//        String openLeft = prefixFringe.substring(prefixFringe.indexOf("][") + 2, prefixFringe.indexOf("]:"));
//        String[] or = openRight.split(", ");
//        String[] ol = openLeft.split(", ");
//        if (prefixFringe.equals(new SuperTagFringe(new TreeSet(), new TreeSet(), "null"))) // [][]:null
////        if (prefixFringe.equals(new SuperTagFringe("[][]:null")))
//        {
//            substNode = "SoS";
//        }
        SuperTagFringe prefixFringe = currentLevel.prefixFringe;
        String substNode = prefixFringe.equals(new SuperTagFringe(new TreeSet(), new TreeSet(), "null")) ? "SoS" : prefixFringe.getSubstNode();
        double backoffProb = 0.0;// take max of backoff instead of multiplying them. rationale:
        if(!prefixFringe.getPrefixNodesRight().isEmpty())
        {
            for (String cat : prefixFringe.getPrefixNodesRight())
            {//each of cats on fringe                
                currentLevel = currentLevel.getBackoff(cat);    
                Integer sumValue = freqMap.get(currentLevel.sum());
                Integer currentLevelValue = freqMap.get(currentLevel);
                if (sumValue != null && currentLevelValue != null)
//                if (freqMap.containsKey(currentLevel.sum()) && freqMap.containsKey(currentLevel))
                {
                    double d = currentLevel.getProb(sumValue, currentLevelValue);
//                    double d = currentLevel.getProb(freqMap);
                    if (d > backoffProb)
                    {
                        backoffProb = d;
                    }
                }
            }
        }
        if(!prefixFringe.getPrefixNodesLeft().isEmpty())
        {
            for (String cat : prefixFringe.getPrefixNodesLeft())
            {//each of cats on fringe                
                currentLevel = currentLevel.getBackoff(cat);
                Integer sumValue = freqMap.get(currentLevel.sum());
                Integer currentLevelValue = freqMap.get(currentLevel);
                if (sumValue != null && currentLevelValue != null)
//                if (freqMap.containsKey(currentLevel.sum()) && freqMap.containsKey(currentLevel))
                {
                    double d = currentLevel.getProb(sumValue, currentLevelValue);
//                    double d = currentLevel.getProb(freqMap);
                    if (d > backoffProb)
                    {
                        backoffProb = d;
                    }
                }
            }
        }        
        if (!substNode.equals("null"))
        {
            currentLevel = currentLevel.getBackoff(substNode);
            Integer sumValue = freqMap.get(currentLevel.sum());
            Integer currentLevelValue = freqMap.get(currentLevel);
            if (sumValue != null && currentLevelValue != null)
//            if (freqMap.containsKey(currentLevel.sum()) && freqMap.containsKey(currentLevel))
            {
                double d = currentLevel.getProb(sumValue, currentLevelValue);
//                double d = currentLevel.getProb(freqMap);
                if (d > backoffProb)
                {
                    backoffProb = d;
                }
            }
        }
        probability += backoffProb * currentLevel.getInterpolationFactor();
        return probability;
    }
    
    private double getProb(Map<SuperTagElement, Integer> bigFreqMap)
    {
        double sum = bigFreqMap.get(sum()).doubleValue();        
        return sum > 1 ? (bigFreqMap.get(this).doubleValue() - 1.0) / (sum - 1.0) : 0;
//        return sum > 1 && v != null ? (v.doubleValue() - 1.0) / (sum - 1.0) : 0;
        
    }
    
    private double getProb(Integer sum, Integer currentLevel)
    {        
        return sum > 1 ? (currentLevel.doubleValue() - 1.0) / (sum - 1.0) : 0;
//        return sum > 1 && v != null ? (v.doubleValue() - 1.0) / (sum - 1.0) : 0;
        
    }

    public static void setInterpol(double d, double e, double f)
    {
        sip1 = d;
        sip2 = e;
        sip3 = f;
    }    

    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof SuperTagElement;
        SuperTagElement other = (SuperTagElement)obj;
        
        return prefixFringe.equals(other.prefixFringe) &&
                nextPosCat.equals(other.nextPosCat) &&
                currentPOSCat.equals(other.currentPOSCat) &&
                predTreeFringe.equals(other.predTreeFringe) &&
                backoffLevel == other.backoffLevel;
        
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 53 * hash + (this.prefixFringe != null ? this.prefixFringe.hashCode() : 0);
        hash = 53 * hash + (this.nextPosCat != null ? this.nextPosCat.hashCode() : 0);
        hash = 53 * hash + (this.predTreeFringe != null ? this.predTreeFringe.hashCode() : 0);
        hash = 53 * hash + (this.currentPOSCat != null ? this.currentPOSCat.hashCode() : 0);
        hash = 53 * hash + this.backoffLevel;
        return hash;
    }
    
    
}
