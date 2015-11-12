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

import fig.basic.Fmt;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class LinkingTheory
{

    private final Options opts;
    private static final boolean bestVerification = false;//true;//
    private static final boolean weighedAverage = true;//false;//
    private int count = 0;
    private final int beamSize;// = 10000;
    private final int beamProp;
    private final int entrybeam;// = 10000;
    private static final int decayed = 10;
    private final ArrayList<PrefixProbability> prefixProbList = new ArrayList<PrefixProbability>();
    private final double decayFactor = .9;
    private final boolean verbose = false;//true;//

    public LinkingTheory(Options opts, int beamWidth, int entrybeam, int beamProp)
    {
        this.opts = opts;
        beamSize = beamWidth;
        this.entrybeam = entrybeam;
        this.beamProp = beamProp;
    }

    public void prune(ArrayList<ChartEntry> chartSlice, boolean isShadowPhase, int beamSizeMod, boolean proportional, boolean discriminative, boolean prunUsingScores, int timestamp)
    {
        if (proportional)
        {
            pruneProp(chartSlice, isShadowPhase, (beamSizeMod - 1) * 2, timestamp);
        }
        else
        {
            if(discriminative)
                pruneStatDiscriminative(chartSlice, isShadowPhase, beamSizeMod, prunUsingScores, timestamp);
            else
                pruneStat(chartSlice, isShadowPhase, beamSizeMod, timestamp);
        }
    }

    public void pruneProp(ArrayList<ChartEntry> chartSlice, boolean isShadowPhase, int additionalScope, int timestamp)
    {		//check whether pruning is necessary: 
        Double threshold;
        threshold = this.calculateMaxProb(chartSlice) - (beamProp + additionalScope);
        ArrayList<ChartEntry> removeList = new ArrayList<ChartEntry>();
        for (ChartEntry ce : chartSlice)
        {
            if (ce.getBestProbability() <= threshold)
            {
                for (BuildBlock b : ce.getBuildBlocks())
                {
                    b = null;
                }
                removeList.add(ce);
            }
            else
            {//remove all analyses with probability <= threshold
                count = 0;
                boolean stillExistent = ce.recursivePrune(ce.getTreeState().getFutureFringe(), threshold, false);//, isShadowPhase);
                if (!stillExistent)
                {
                    removeList.add(ce);
                }
            }
        }
        chartSlice.removeAll(removeList);
//        if (chartSlice.size() > beamSize)
        if (chartSlice.size() > entrybeam)
        {
            pruneStat(chartSlice, isShadowPhase, 1, timestamp);
        }
    }

    public void pruneStat(ArrayList<ChartEntry> chartSlice, boolean isShadowPhase, int beamSizeMod, int timestamp)
    {
        //check whether pruning is necessary: 
		/*if (!ParserModel.useProbabilityModel){
         for (int i = beamSize; i<chartSlice.size(); i++){
         chartSlice.remove(beamSize);
         }
         }*/
//        System.out.print(timestamp + "\tbefore stat: "+ chartSlice.size());
        Double threshold;        
        double[] probs;
//        int beam = beamSize * beamSizeMod;        
        int beam = entrybeam * beamSizeMod;        
        probs = calculateProbabilities(chartSlice);
        if (chartSlice.size() > beam)
        {
//            double[] probs = calculateProbabilities(chartSlice);
            Arrays.sort(probs);
            //do pruning
            threshold = probs[probs.length - beam];
            //double t2 = probs[probs.length-10];
            //System.out.println(threshold + "prune CEs " + chartSlice.size());
//            System.out.println(String.format("[%s,%s,%s], %s", Fmt.D((double)probs[0]), Fmt.D((double)threshold), Fmt.D((double)probs[probs.length - 1]), probs.length) + (isShadowPhase ? " prediction" : ""));
            ArrayList<ChartEntry> removeList = new ArrayList<ChartEntry>();
            for (ChartEntry ce : chartSlice)
            {
                /*				if (ce.getProbability()>t2){
                 if (ce.getProbability()==probs[probs.length-1]){
                 System.out.println("best analysis:");
                 }
                 System.out.print(ce.getProbability()+" "+ce);
                 }*/
                if (ce.getBestProbability() <= threshold)
                {
                    for (BuildBlock b : ce.getBuildBlocks())
                    {
                        b = null;
                    }
                    removeList.add(ce);
                }
                else
                {//remove all analyses with probability <= threshold
                    //				double startTime = System.currentTimeMillis();
                    count = 0;
                    double internthreshold = ce.getTop(entrybeam, false);
                    if (internthreshold < threshold)
                    {
                        internthreshold = threshold;
                    }
                    boolean stillExistent = ce.recursivePrune(ce.getTreeState().getFutureFringe(), internthreshold, false);//, isShadowPhase);
                    if (!stillExistent)
                    {
                        removeList.add(ce);
                    }
                    //				if (count > 100) System.out.println("prune count:" +count);
                    //				if (System.currentTimeMillis() - startTime > 1000){System.out.println("duration for recursivePruning > 1sec");}
                }
            }
            chartSlice.removeAll(removeList);
        }
        else
        {
//			threshold = getMinProbability(chartSlice)-0.0001;//TODO change. This will not work if e.g. chartSlice size = 1;
            ArrayList<ChartEntry> removeList = new ArrayList<ChartEntry>();
            for (ChartEntry ce : chartSlice)
            {
                //			double startTime = System.currentTimeMillis();
                count = 0;
                threshold = ce.getTop(entrybeam, false);
                boolean stillExistent = ce.recursivePrune(ce.getTreeState().getFutureFringe(), threshold, false);//, isShadowPhase);
                //			if (System.currentTimeMillis() - startTime > 1000){System.out.println("duration for recursivePruning > 1sec");}
                if (!stillExistent)
                {
                    removeList.add(ce); //should not happen given current definition of threshold.
                    for (BuildBlock b : ce.getBuildBlocks())
                    {
                        b = null;
                    }
                }
                //			if (count > 100 ) 
                //				System.out.println("prune count:" +count);
            }
            chartSlice.removeAll(removeList);
        }//*/
//        System.out.println(" after stat: "+ chartSlice.size());
    }
    
    public void pruneStatDiscriminative(ArrayList<ChartEntry> chartSlice, Boolean isShadowPhase, int beamSizeMod, boolean pruneUsingScores, int timestamp)
    {
//        System.out.print(timestamp + "\tbefore discr: "+ chartSlice.size());
        Double threshold;        
//        int beam = beamSize * beamSizeMod;        
        int beam = entrybeam * beamSizeMod;        
//        double[] scores = pruneUsingScores ? calculateScores(chartSlice) : calculateProbabilities(chartSlice);
        if (chartSlice.size() > beam)
        {            
            double[] scores = pruneUsingScores ? calculateScores(chartSlice) : calculateProbabilities(chartSlice);
            Arrays.sort(scores);
            //do pruning
            threshold = scores[scores.length - beam];
            ArrayList<ChartEntry> removeList = new ArrayList<ChartEntry>();
            for (ChartEntry ce : chartSlice)
            {
                if ((pruneUsingScores ? ce.getBestScore() : ce.getBestProbability()) <= threshold)
                {
                    for (BuildBlock b : ce.getBuildBlocks())
                    {
                        b = null;
                    }
                    removeList.add(ce);
                }
                else
                {//remove all analyses with probability <= threshold                    
                    count = 0;
                    double internthreshold = ce.getTop(entrybeam, pruneUsingScores);
                    if (internthreshold < threshold)
                    {
                        internthreshold = threshold;
                    }
                    boolean stillExistent = ce.recursivePrune(ce.getTreeState().getFutureFringe(), internthreshold, pruneUsingScores);
                    if (!stillExistent)
                    {
                        removeList.add(ce);
                    }
                }
            }
            chartSlice.removeAll(removeList);
        }
        else
        {
            ArrayList<ChartEntry> removeList = new ArrayList<ChartEntry>();
            for (ChartEntry ce : chartSlice)
            {             
                count = 0;
                threshold = ce.getTop(entrybeam, pruneUsingScores);
                boolean stillExistent = ce.recursivePrune(ce.getTreeState().getFutureFringe(), threshold, pruneUsingScores);
                if (!stillExistent)
                {
                    removeList.add(ce); //should not happen given current definition of threshold.
                    for (BuildBlock b : ce.getBuildBlocks())
                    {
                        b = null;
                    }
                }               
            }
            chartSlice.removeAll(removeList);
        }
//        System.out.println(" after discr: "+ chartSlice.size());
    }

    /*
     private double[] calculateLastProbs(ArrayList<ChartEntry> chartSlice) {
     double[] probs = new double[chartSlice.size()];
     int i=0;
     for (ChartEntry ce: chartSlice){
     probs[i] = ce.getTreeState().getLastFringe().getProbability(ce);
     ce.setProbability(probs[i]);
     i++;
     }
     return probs;
     }
	

     private double[] calculateProbabilities(ArrayList<ChartEntry> chartSlice) {
     double[] probs = new double[chartSlice.size()];
     int i=0;
     for (ChartEntry ce: chartSlice){
     probs[i] = ce.getTreeState().getLastFringe().getCurrentProbability(ce);
     ce.setProbability(probs[i]);
     i++;
     }
     return probs;
     }
     */
    private double[] calculateProbabilities(ArrayList<ChartEntry> chartSlice)
    {
        double[] probs = new double[chartSlice.size()];
        int i = 0;
        for (ChartEntry ce : chartSlice)
        {
            double[] ps = ce.getTreeState().getFutureFringe().getnBestProbs();
            ce.setProbabilities(ps);
            probs[i] = ps[0];
            i++;
        }
        return probs;
    }
    
    private double[] calculateScores(ArrayList<ChartEntry> chartSlice)
    {
        double[] scores = new double[chartSlice.size()];
        int i = 0;
        for (ChartEntry ce : chartSlice)
        {
            double[] sc = ce.getTreeState().getFutureFringe().getnBestScores();
            ce.setScores(sc);
            scores[i] = sc[0];
            i++;
        }
        return scores;
    }

    private double calculateMaxProb(ArrayList<ChartEntry> chartSlice)
    {
        //double[] max = new double[ParserModel.nbest];
        //Arrays.fill(max, Double.NEGATIVE_INFINITY);
        double max = Double.NEGATIVE_INFINITY;
        for (ChartEntry ce : chartSlice)
        {
            double[] pa = ce.getTreeState().getFutureFringe().getnBestProbs();
            ce.setProbabilities(pa);
            double p = pa[0];
            if (p > max)
            {
                max = p;
            }
        }
        return max;
    }
    
    public ChartEntry getTopSlice(ArrayList<ChartEntry> chartSlice)
    {
        int maxIndex = -1, index = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (ChartEntry ce : chartSlice)
        {
            double[] pa = ce.getTreeState().getFutureFringe().getnBestProbs();
            ce.setProbabilities(pa);
            double p = pa[0];
            if (p > max)
            {
                max = p;
                maxIndex = index;
            }
            index++;
        }
        return chartSlice.get(maxIndex);
    }
    
    public ChartEntry getTopSliceDiscriminative(ArrayList<ChartEntry> chartSlice)
    {
        int maxIndex = -1, index = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (ChartEntry ce : chartSlice)
        {
            double[] pa = ce.getTreeState().getFutureFringe().getnBestScores();
            ce.setScores(pa);
            double p = pa[0];
            if (p > max)
            {
                max = p;
                maxIndex = index;
            }
            index++;
        }
        return chartSlice.get(maxIndex);
    }

    @SuppressWarnings("static-access")
    public String calculateDifficulty(ArrayList<ChartEntry> chartSlice, int currentTime, String word, String pos, 
            String sentenceRc, String wordId, boolean useScores, double baselineWeight)
    {
        //ArrayList<ChartEntry> chartSlice = chart.get(currentTime);
        String out = "";
        double prefixProb = 0, prefixScore = 0, prefixBaselineScore = 0;
        double integCost = 0;
        HashMap<String, Double> verificationsDone = new HashMap<String, Double>();
        HashMap<String, Double> verifiedTreesProbMap = new HashMap<String, Double>();
        //ArrayList<String> verificationsDone = new ArrayList<String>();
        double minConst = 0;
        //if (!chartSlice.isEmpty()&& chartSlice.get(0).getProbability() < -13 ) minkonst = Math.ceil(chartSlice.get(0).getProbability())-13;
        double ICNormalizer = 0;
        for (ChartEntry ce : chartSlice)
        {
            //calculate prefix probability for each chartEntry.            
            double ceScoreSum = useScores ? ce.recursiveSumScores(ce.getTreeState().getFutureFringe(), baselineWeight, currentTime + 1, minConst) : 0;
            double ceProbSum = ce.recursiveSumProbs(ce.getTreeState().getFutureFringe(), minConst);
            double ceScoreBaselineSum = useScores ? ce.recursiveSumBaselineScores(ce.getTreeState().getFutureFringe(), baselineWeight, currentTime + 1, minConst) : 0;
            prefixProb += ceProbSum;
            prefixBaselineScore += ceScoreBaselineSum;
//            prefixScore += ceScoreSum == 1.0 ? 0 : ceScoreSum;
            prefixScore += ceScoreSum;
            //calculate integration cost incurred at each chart entry and associate with prefix probability.
            BuildBlock bb = ce.getBuildBlocks().get(0);

            if (!useScores && (weighedAverage || this.bestVerification) && ce.getBuildBlocks().get(0).getOperation().equals(ParserOperation.verify))
            {
                ICNormalizer += ceProbSum;
                String verification = bb.getVerifiedNodeString();
                double verifiedTreeProb = Math.exp(bb.getVerifiedTreeProb());
                //System.out.println(verifiedTreeProb);
                int nodeAge = currentTime - bb.getIpi().getTimeStamp(); // bb.ipi is original root node o
                String key = nodeAge + "@" + verification;

                if (!verificationsDone.containsKey(key))
                {
                    verificationsDone.put(key, ceProbSum);
                    verifiedTreesProbMap.put(key, verifiedTreeProb);
                }
                else
                {
                    verificationsDone.put(key, ceProbSum + verificationsDone.get(key));
                }
            }
        }
        //weigh integration cost by probability with which was observed.
        if (!useScores && weighedAverage)
        {
            for (String ageAndNodes : verificationsDone.keySet())
            {
                int atIndex = ageAndNodes.indexOf("@");
                int nodeAge = Integer.parseInt(ageAndNodes.substring(0, atIndex));
                integCost += Math.log(Math.pow(verifiedTreesProbMap.get(ageAndNodes), (1 - Math.pow(this.decayFactor, nodeAge))))
                        //	* (verificationsDone.get(ageAndNodes)/ICNormalizer);//prefixProb) ; // weighing by probability of seeing this
                        * (verificationsDone.get(ageAndNodes) / prefixProb); // weighing by probability of seeing this


            }
        }
        else if (!useScores && bestVerification && !verificationsDone.isEmpty())
        {
            String bestVerific = "";
            double maxProb = Double.NEGATIVE_INFINITY;
            for (String ageAndNodes : verificationsDone.keySet())
            {
                double prob = verificationsDone.get(ageAndNodes);
                if (prob > maxProb)
                {
                    maxProb = prob;
                    bestVerific = ageAndNodes;
                }
                if (prob == 0)
                {
                    System.out.println();
                }
            }
            int atIndex = bestVerific.indexOf("@");
            int nodeAge = Integer.parseInt(bestVerific.substring(0, atIndex));
            integCost = Math.log(Math.pow(verifiedTreesProbMap.get(bestVerific), (1 - Math.pow(this.decayFactor, nodeAge))));
        }

        //=====fine from here==========
//        double logPrefixProb = -1 * Math.log(prefixProb) + minkonst;

//        double logPrefixProb = useScores ?  Math.abs(Math.log(prefixScore == 0 ? 1.0 : prefixScore)) + minConst : Math.abs(Math.log(prefixProb)) + minConst;
        double logPrefixProb = Math.abs(Math.log(prefixProb)) + minConst;
//        double logIntegCost = -1 * integCost;
        double logIntegCost = useScores ? 0 : Math.abs(integCost);
        prefixProbList.add(new PrefixProbability(word, logPrefixProb));
        double surprisal;
        if (prefixProbList.size() == 1 || useScores)
        {
            surprisal = logPrefixProb;
        }
        else
        {
            surprisal = logPrefixProb - prefixProbList.get(prefixProbList.size() - 2).getPrefixProb();
            if (verbose)
            {
                out += "\n(" + logPrefixProb + "-"
                        + prefixProbList.get(prefixProbList.size() - 2).getPrefixProb() + ") +" + logIntegCost + "\n";
            }
        }
        NumberFormat f = NumberFormat.getInstance();
        f.setGroupingUsed(false);
       
        StringBuilder str = new StringBuilder();
        if(opts.estimateProcDifficulty && opts.inputType == Options.InputType.dundee)
        {
            str.append(String.format("%s |\t%s\t%s\t%s\t%s\t%s", sentenceRc, word, wordId, cutNumber(f.format(surprisal)), 
                    cutNumber(f.format(logIntegCost)), cutNumber(String.valueOf(surprisal + logIntegCost))));
        }
        else
        {
            if (opts.estimateProcDifficulty)
            {
                if(useScores)
                {
                    double scoreSurprisal = Math.abs(Math.log(prefixScore == 0 ? 1.0 : prefixScore)) + minConst;
                    double baselineSurprisal = Math.abs(Math.log(prefixBaselineScore)) + minConst;
//                    str.append(String.format("S: %s; ", cutNumber(f.format(surprisal))));
                    str.append(String.format("B: %s\tM: %s\tD: %s; ", cutNumber(f.format(baselineSurprisal)), 
                            cutNumber(f.format(scoreSurprisal)), cutNumber(f.format(baselineSurprisal + scoreSurprisal))));
                }
                else
                    str.append(String.format("S: %s\tIC: %s\tD: %s; ", cutNumber(f.format(surprisal)), 
                        cutNumber(f.format(logIntegCost)), cutNumber(String.valueOf(surprisal + logIntegCost))));
            }
            if (!(opts.goldPosTags || pos.equals("N/A")))
            {
                str.append(pos).append(" ");
            }
            str.append(word).append("\t");
        }        
        if (opts.estimateProcDifficulty && verbose)
        {
            str.append("\t").append(chartSlice.size()).append("\n[").append(out).append("]");
        }
        return str.toString();
    }

    /*
     * for (BuildBlock bb : ce.getBuildBlocks()){
     if (bb.getOperation() == ParserOperation.verify){
     //	integCost = calculateIntegrationCost(bb, verificationsDone, currentTime, integCost);
     String verification = bb.getVerifiedNodeString();
     double verifiedTreeProb = Math.exp(bb.getVerifiedTreeProb());
     int nodeAge = currentTime - bb.getIpi().getTimeStamp(); // bb.ipi is original root node of prediction tree.
     verificationsDone.put(nodeAge+verification, bb.getProbability()+bb.getPrevChartEntry().getProbability());
     }
     double currentTreeProb = bb.getProbability();
     ChartEntry previousCE = bb.getPrevChartEntry();
     if (previousCE!=null){//.getPrefTreeState()[0]!=-1){
     double previousPrefixProb = previousCE.getProbability();
     if (!bb.getOperation().toString().endsWith("S")){//don't count shadow trees: this means prediction is free as long as it isn't used.
     prefixProb += Math.exp(currentTreeProb +previousPrefixProb);
     if (verbose) out +=Math.log(prefixProb) +"="+currentTreeProb +"+"+ previousCE.getProbability()+"\t";
     }
     }
     else{
     prefixProb += Math.exp(currentTreeProb);
     if (verbose) out +=Math.log(prefixProb) +"="+currentTreeProb+"\t";
     }
     }
     */
    @SuppressWarnings("unchecked")
    private double calculateIntegrationCost(BuildBlock bb, HashSet<String> verificationsDone, int currentTime, double integCost)
    {
        int age = 0;
        double verificationCost = 0.0;
        //String verification =bb.getVerifiedNodeString();//
        //if (!contains(verificationsDone, verification)){
        HashSet<String> verification = (HashSet<String>) bb.getVerifiedNodeList().clone();
        verification.removeAll(verificationsDone);
        if (!verification.isEmpty())
        {
            System.out.println(verification);
        }
        double verifiedTreeProb = Math.exp(bb.getVerifiedTreeProb());

        //this is just approximate. Can only be done exactly after real integration has been done, 
        // but not during search stage because not all verified nodes (and hteir time stamps) might
        // be visible!!!
        int nodeAge = currentTime - bb.getIpi().getTimeStamp(); // bb.ipi is original root node of prediction tree.
        if (!verification.isEmpty())
        {
            System.out.println(nodeAge);
        }
        //for (int i = 0; i < bb.getNoOfVerifiedNodes(); i++){
        for (String nodeCat : verification)
        {
            verificationCost += Math.pow(verifiedTreeProb, (1 - Math.pow(this.decayFactor, nodeAge))) / bb.getNoOfVerifiedNodes();
            age += nodeAge;
        }
        //	}
        if (verificationCost > 0)
        {
            integCost = integCost * verificationCost;
            //verificationsDone.add(verification);
            verificationsDone.addAll(verification);
            //System.out.println(verification);
        }
        return integCost;
    }

    private boolean contains(ArrayList<String> verificationsDone, String verification)
    {
        for (String v : verificationsDone)
        {
            if (v.equals(verification))
            {
                return true;
            }
        }
        return false;
    }

    private String cutNumber(String string)
    {
        if (string.length() > 7)
        {
            return string.substring(0, 6);
        }
        return string;
    }

    private class PrefixProbability
    {

        private final String word;
        private final double difficulty;

        public PrefixProbability(String word, double difficulty)
        {
            this.word = word;
            this.difficulty = difficulty;
        }

        public double getPrefixProb()
        {
            return difficulty;
        }

        @Override
        public String toString()
        {
            return String.format("%s\t%s", word, Fmt.D(difficulty));
        }
        
        
    }

    public void newSentence()
    {
        prefixProbList.clear();
    }
}
