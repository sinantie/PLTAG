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

public class SuperTagStructElement
{

    private String predTreeStruct = ""; //predicted tree
    private String predFringe = "";
    private String currentPOSCat = "";//category of last node on spine
    //private static double sip1;
    //private static double sip2;
    private int backoffLevel;
    private final double lambda = 0.9;
    private final double smoother = 1.0 / (double) Lexicon.getNumOfTreeTemps();
    private final double regulariser = (1 - lambda) * smoother;
            
    public SuperTagStructElement(String predTreeStruct, String predTreeFringe, String currentPosCat)
    {
        this.currentPOSCat = currentPosCat;
        this.predTreeStruct = predTreeStruct;
        this.predFringe = predTreeFringe;
        this.backoffLevel = 1;
    }

    private SuperTagStructElement(String predTreeStruct, String predTreeFringe, String currentPosCat, int backofflevel)
    {
        this.currentPOSCat = currentPosCat;
        this.predTreeStruct = predTreeStruct;
        this.predFringe = predTreeFringe;
        this.backoffLevel = backofflevel;
    }

    public SuperTagStructElement(String readIn)
    {
        String[] array = readIn.split("\t");
        if (array.length == 4)
        {
            this.predTreeStruct = array[1];
            this.predFringe = array[2];
            this.currentPOSCat = array[3];
            this.backoffLevel = Integer.parseInt(array[0]);
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

    /*
    private double getInterpolationFactor(){
    if (backoffLevel == 1){
    return sip1;
    }
    else if (backoffLevel == 2){
    return sip2;
    }
    else return 0.0;
    }
    
    public static void estimateInterpol(HashMap<String, Integer> bigFreqMapWord) {
    double ip1 = 0.0;
    double ip2 = 0.0;
    for (String tuple : bigFreqMapWord.keySet()){
    SuperTagStructElement t = new SuperTagStructElement(tuple);
    double maxprob = 0.0;
    int level = 0;
    int freq = bigFreqMapWord.get(tuple);
    if (t.backoffLevel==1){
    while(true){
    if (bigFreqMapWord.get(t.sum().getKey()) > 1){
    double d = (bigFreqMapWord.get(t.getKey()).doubleValue()-1.0) / (bigFreqMapWord.get(t.sum().getKey()).doubleValue()-1);
    if (d > maxprob){
    level = t.backoffLevel;
    maxprob = d;
    }
    }
    if (!t.hasBackoff()){
    break;
    }
    else t = t.getBackoff();
    }
    
    if (level==1) ip1+= freq;
    else if (level == 2) ip2+= freq; 
    
    }
    }
    sip1 = ip1 / (ip1 + ip2);
    sip2 = ip2 / (ip1 + ip2);
    }
     */
    public String toString()
    {
//        return new StringBuilder().append(backoffLevel).append("\t").append(predTreeStruct).append("\t").append(predFringe).append("\t").append(currentPOSCat).append("\n").toString();
        return new StringBuilder().append(backoffLevel).append("\t").append(predTreeStruct).append("\t").append(predFringe).append("\t").append(currentPOSCat).toString();
    }

    public SuperTagStructElement sum()
    {
        return new SuperTagStructElement("TREE", predFringe, currentPOSCat, 0);
    }

    /*
    public boolean hasBackoff() {
    if (backoffLevel < 2) return true;
    return false;	
    }
    
    public SuperTagStructElement getBackoff() {
    if (backoffLevel == 1){
    return new SuperTagStructElement(predTreeStruct, predFringe, "POS", 2);
    }else{
    System.err.println("error in backoff levels.");
    return this;
    }
    }
     */
    public static void readMap(String filename, Map<String, Integer> map, boolean combineNNVBCats)
    {
        boolean needCalculateUniq = true;
        for (String line : Utils.readLines(filename))
        {
            String[] hash = line.split("\t=");
            String key = Utils.getCatInventory(hash[0], combineNNVBCats);
            if (map.containsKey(key))
            {
                map.put(key, map.get(key) + Integer.parseInt(hash[1]));
            }
            else
            {
                map.put(key, Integer.parseInt(hash[1]));
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

    public static void calculateUniq(Map<String, Integer> bigFreqMap)
    {
        Map<String, Integer> uniqCounter = new HashMap<String, Integer>();
        for (String key : bigFreqMap.keySet())
        {
            if (!key.startsWith("0"))
            {
                SuperTagStructElement tpe = new SuperTagStructElement(key);
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

    public String getKey()
    {
//        return this.toString().trim();
        return this.toString();
    }

    public double getSmoothedProb(Map<String, Integer> bigFreqMap)
    {
        double probability;
        String sumkey = sum().getKey();
        if (bigFreqMap.containsKey(sumkey))
        {   
//            double lambda = 0.9;
//            probability = lambda * getProb(bigFreqMap, sumkey) + (1 - lambda) * 1.0 / (double) Lexicon.getNumOfTreeTemps();
            probability = lambda * getProb(bigFreqMap, sumkey) + regulariser;
        }
        else
        {
            probability = smoother;
        }
        return probability;
    }

    private double getProb(Map<String, Integer> superTagFreqs, String sumKey)
    {
        String key = getKey();
//        if (superTagFreqs.get(sum().getKey()) > 1 && superTagFreqs.containsKey(getKey()))
        if (superTagFreqs.get(sumKey) > 1 && superTagFreqs.containsKey(key))
        {
//            double a = superTagFreqs.get(getKey()).doubleValue();
//            double b = superTagFreqs.get(sum().getKey()).doubleValue();
            double a = superTagFreqs.get(key).doubleValue();
            double b = superTagFreqs.get(sumKey).doubleValue();
            return (a - 1) / b;
        }
        return 0;
    }
    //public static void setInterpol(double d, double e) {
    //	sip1 = d;
    //	sip2 = e;
    //}
}
