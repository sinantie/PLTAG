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

import fig.basic.Pair;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class ChartEntry implements Serializable
{

    static final long serialVersionUID = -1L;
    private int nBest;
    private TreeState treestate;
    private final transient ArrayList<BuildBlock> originlist = new ArrayList<BuildBlock>();
//	private BuildBlock buildblock;
    private double[] nBestProbabilities, nBestScores;    
    private double difficulty = 0.0;
    private boolean used = false;
    //private HashMap<Double, Fringe> probList;
    private ArrayList<Double> probs = new ArrayList<Double>();
    private ArrayList<Double> scores = new ArrayList<Double>();
    final int MIN_THRESHOLD_VALUE = -10000;
    final int MIN_THRESHOLD_DISCR_VALUE =  Integer.MIN_VALUE;//-10000;
    
    //private ArrayList<ChartEntry> oldIDList = new ArrayList<ChartEntry>();
    public ChartEntry()
    {        
    }

    
    public ChartEntry(Options opts, TreeState treestate)
    {
        this.nBest = opts.nBest;
        nBestProbabilities = new double[opts.nBest];
        this.treestate = treestate;
        Arrays.fill(nBestProbabilities, Double.NEGATIVE_INFINITY);
    }

    public ChartEntry(Options opts, TreeState treestate, BuildBlock bb)
    {
        this.nBest = opts.nBest;
        nBestProbabilities = new double[opts.nBest];
        this.treestate = treestate;
        originlist.add(bb);
        Arrays.fill(nBestProbabilities, Double.NEGATIVE_INFINITY);
        //buildblock = bb;
    }

    public ChartEntry(Options opts, TreeState treestate, ArrayList<BuildBlock> bbList)
    {
        this.nBest = opts.nBest;
        nBestProbabilities = new double[opts.nBest];
        this.treestate = treestate;
        originlist.addAll(bbList);
        Arrays.fill(nBestProbabilities, Double.NEGATIVE_INFINITY);
    }

    public TreeState getTreeState()
    {
        return this.treestate;
    }

    public ArrayList<BuildBlock> getBuildBlocks()
    {
        return this.originlist;
    }

    public void addBuildBlock(BuildBlock bb)
    {
        originlist.add(bb);
        //buildblock = bb;
    }

    public void addAllBuildBlocks(LinkedList<BuildBlock> bblist)
    {
        ArrayList<String> bstringlist = new ArrayList<String>();
        for (BuildBlock oblock : originlist)
        {
            bstringlist.add(oblock.toString());
        }

        for (BuildBlock bbnew : bblist)
        {
            if (!bstringlist.contains(bbnew.toString()))
            {
                System.out.println(bbnew);
                System.out.println(originlist);
                originlist.add(bbnew);
            }
        }
    }

    @Override
    public String toString()
    {
        return new StringBuilder().append(originlist)
                //.append("\nlogP: ").append(this.probability).append("\t")
                .append(treestate).append("\n").toString();
    }

    public void setDifficulty(double d)
    {
        difficulty = d;
    }

    public double getBestProbability()
    {
        return this.nBestProbabilities[0];
    }

    public double getBestScore()
    {
        return this.nBestScores[0];
    }

    public double[] getNBestProbs()
    {
        return this.nBestProbabilities;
    }

    public double[] getnBestScores()
    {
        return nBestScores;
    }

    public void minimizeSpace()
    {
        treestate.saveSpace();
        probs = null;
        scores = null;
        //	oldIDList = null;
        originlist.trimToSize();
        for (BuildBlock b : originlist)
        {
            b.saveSpace();// 
        }
        //	this.oldIDList=null;
    }

    public boolean wasUsed()
    {
        return used;
    }

    public void setUsed()
    {
        used = true;
    }

    public void combineCEs(ChartEntry chartEntry, boolean discriminative, boolean pruneUsingScores)
    {
        for (BuildBlock bbnew : chartEntry.getBuildBlocks())
        {
            if (!originlist.contains(bbnew))
            {
                if (originlist.toString().contains(bbnew.getElemTreeString())
                        && originlist.get(0).getAdjNodeNumber() == bbnew.getAdjNodeNumber())
                {//TODO check this makes sense.
                    boolean prevcesame = false;
                    for (BuildBlock origbb : originlist)
                    {
                        if (origbb.getPrevChartEntry() == bbnew.getPrevChartEntry())
                        {
                            prevcesame = true;
                            break;
                        }
                    }
                    if (prevcesame)
                    {
                        continue;
                    }
                }
                originlist.add(bbnew);
                //System.out.print("osize: "+originlist.size());
            }
        }
        if(discriminative)
            mergeScoresProbs(chartEntry, pruneUsingScores);
        else
            mergeProbs(chartEntry);
        //current fringe and fringeAndProb.getFringe() are the same, but probabilities might be different.
        // probability at current fringe always just max of future fringe probs.
        // therefore can also just max at merged entry.fringeAndProbs.prob
        // add cutOffLocs and next from new ChartEntry into merged chartEntry.
        this.getTreeState().getFutureFringe().addAllCutOffLocs(chartEntry.getTreeState().getFutureFringe().getBBHist());
        mergeFringeConts(chartEntry.getTreeState().getFutureFringe().getNext(), this.getTreeState().getFutureFringe().getNext());
    }

    private void mergeProbs(ChartEntry chartEntry)
    {
        if (this.nBestProbabilities.length == nBest && this.nBestProbabilities[nBest - 1] > chartEntry.getBestProbability())
        {
            this.nBestProbabilities = this.nBestProbabilities.clone();
        }
        else if (chartEntry.nBestProbabilities.length == nBest && chartEntry.nBestProbabilities[nBest - 1] > this.getBestProbability())
        {
            this.nBestProbabilities = chartEntry.nBestProbabilities.clone();
        }
        else
        {
            this.nBestProbabilities = mergeArrays(this.nBestProbabilities, chartEntry.nBestProbabilities);

        }
        this.treestate.getFutureFringe().mergeProbs(chartEntry.getTreeState().getFutureFringe());
    }
    
    private void mergeScoresProbs(ChartEntry chartEntry, boolean pruneUsingScores)
    {
        if((pruneUsingScores && nBestScores.length == nBest && nBestScores[nBest - 1] > chartEntry.getBestScore()) ||
        (!pruneUsingScores && nBestScores.length == nBest && nBestProbabilities[nBest - 1] > chartEntry.getBestProbability()))
        {
            nBestScores = nBestScores.clone();
            nBestProbabilities = nBestProbabilities.clone();
        }
        else if ((pruneUsingScores && (chartEntry.nBestScores.length == nBest && chartEntry.nBestScores[nBest - 1] > getBestScore())) || 
        (!pruneUsingScores && chartEntry.nBestScores.length == nBest && chartEntry.nBestProbabilities[nBest - 1] > getBestProbability()))
        {
            nBestScores = chartEntry.nBestScores.clone();
            nBestProbabilities = chartEntry.nBestProbabilities.clone();
        }
        else
        {
            Pair<double[], double[]> pair = mergeArrays(new Pair(nBestScores, nBestProbabilities), new Pair(chartEntry.nBestScores, chartEntry.nBestProbabilities), pruneUsingScores);
            this.nBestScores = pair.getFirst();
            this.nBestProbabilities = pair.getSecond();
        }
        treestate.getFutureFringe().mergeScoresProbs(chartEntry.getTreeState().getFutureFringe(), pruneUsingScores);
    }

    private double[] mergeArrays(double[] thisnBestProbability,
            double[] cenBestProbability)
    {
        double[] mergedarray = new double[nBest];
        int a = 0;
        int b = 0;
        for (int i = 0; i < nBest; i++)
        {
            if (thisnBestProbability[a] > cenBestProbability[b])
            {
                mergedarray[i] = thisnBestProbability[a];
                a++;
            }
            else
            {
                mergedarray[i] = cenBestProbability[b];
                b++;
            }
        }
        return mergedarray;
    }
    
    private Pair<double[], double[]> mergeArrays(Pair<double[], double[]> thisScoresProbs,
            Pair<double[], double[]> ceScoresProbs, boolean pruneUsingScores)
    {
        Pair<double[], double[]> mergedarray = newEmptyPairArray(nBest);
        int a = 0;
        int b = 0;
        for (int i = 0; i < nBest; i++)
        {
            if ((pruneUsingScores && thisScoresProbs.getFirst()[a] > ceScoresProbs.getFirst()[b]) || 
            (!pruneUsingScores && thisScoresProbs.getSecond()[a] > ceScoresProbs.getSecond()[b]))
            {
                mergedarray.getFirst()[i] = thisScoresProbs.getFirst()[a];
                mergedarray.getSecond()[i] = thisScoresProbs.getSecond()[a];
                a++;
            }
            else
            {
                mergedarray.getFirst()[i] = ceScoresProbs.getFirst()[b];
                mergedarray.getSecond()[i] = ceScoresProbs.getSecond()[b];
                b++;
            }
        }
        return mergedarray;
    }

    /**
     * check whether fringe continuations are identical, in which case
     * cutOffLocations and Probabilities are merged. If not identical, put on
     * list of next fringes.
     */
    private void mergeFringeConts(ArrayList<FringeAndProb> tsLastFringeConts, ArrayList<FringeAndProb> thisLastFringeConts)
    {
        if (tsLastFringeConts == null || thisLastFringeConts == null)
        {            
            return;
        }
        HashMap<ArrayList<Fringe>, FringeAndProb> thisFringesMap = new HashMap< ArrayList<Fringe>, FringeAndProb>();
        for (FringeAndProb thisFutFringes : thisLastFringeConts)
        {
            thisFringesMap.put(thisFutFringes.getFringe(), thisFutFringes);
        }
        for (FringeAndProb tsFutFringe : tsLastFringeConts)
        {
            // if same continuation contained need to merge prob and bb-history
            if (thisFringesMap.containsKey(tsFutFringe.getFringe()))
            {
                // get FringeAndProb with same future fringe
                FringeAndProb thisFringe = thisFringesMap.get(tsFutFringe.getFringe());
                // merge future fringes
                mergeFringeConts(tsFutFringe.getNext(), thisFringe.getNext());
                // merge cutoff locations
                thisFringe.addAllCutOffLocs(tsFutFringe.getBBHist());
                // max prob.
                thisFringe.mergeProbs(tsFutFringe);
            }
            //if same future fringe not contained in merged entry, add future fringe to set of merged future fringes.
            else
            {
                thisLastFringeConts.add(tsFutFringe);
            }
        }
    }

    public void updateProbs(FringeAndProb fap, double vlap)
    {
        probs.clear();
        //System.out.print("\n"+fap.getFringe());
        this.setProbabilities(recursiveUpdateProbs(fap, "", vlap));
    }
    
    /**
     * 
     * Update n-best list with discriminative model score, i.e., nBestProbability_i * baselineWeight + model_score
     * @param fap
     * @param modelScoreNoBaseline the score of the discriminative model for the corresponding analysis without the 
     * baseline score
     * @param baselineWeight the model's estimate of the baseline feature weight
     * @param prefixLength the number of words in the sentence. We need it in order to normalize the baseline model score
     * @param pruneUsingScores prune using discriminative model score weights instead of probability model score
     * @param usePrefixBaselineFeature use prefix tree probability feature score
     */
    public void updateProbsWithModelScore(FringeAndProb fap, double modelScoreNoBaseline, double baselineWeight, int prefixLength, boolean pruneUsingScores, boolean usePrefixBaselineFeature)
    {                
        probs.clear();
        scores.clear();
        // alter semantics of nBestProbability array: use model scores instead of probabilities
        Pair<double[], double[]> scoresProbs = recursiveUpdateProbsDiscriminative(fap, "", modelScoreNoBaseline, baselineWeight, prefixLength, pruneUsingScores, usePrefixBaselineFeature);        
        setScores(scoresProbs.getFirst());
        setProbabilities(scoresProbs.getSecond());
    }

    public int countAnalyses(FringeAndProb fap)
    {
        int count = 0;
        if (fap.getNext() == null || fap.getNext().isEmpty())
        {
            return 1;
        }
        else
        {
            for (FringeAndProb nextfap : fap.getNext())
            {
                int c = countAnalyses(nextfap);
                count += c;
            }
            return count;
        }
    }
//	*/
    
    private double[] recursiveUpdateProbs(FringeAndProb fap, String string, double vlap)
    {
        if (fap.getNext() == null || fap.getNext().isEmpty())
        {
            int i = originlist.size();
            if (i > 1)
            {
                System.out.println("Unexpected case, more than 1 bb in ce.");
            }
            double s = fap.getBestProb() + originlist.get(0).getProbability() + vlap;            
            fap.setProb(s, 0);
            probs.add(s);
            //		if (ParserModel.timeProfile) System.out.println("a"+s);
            if (ParserModel.verbose)
            {
                System.out.println(string + s);
            }
            double[] nBestAr = new double[nBest];
            Arrays.fill(nBestAr, Double.NEGATIVE_INFINITY);
            nBestAr[0] = s;
            fap.setProb(nBestAr);
            return nBestAr;
        }
        else
        {
            double[] maxProb = new double[nBest];
            Arrays.fill(maxProb, Double.NEGATIVE_INFINITY);
            for (FringeAndProb nextfap : fap.getNext())
            {
                //	System.out.print("\n"+string+"fringe: "+ nextfap.getFringe());
                double[] newProb = recursiveUpdateProbs(nextfap, string + "\t", vlap);
                if (newProb == null)
                {
                    continue;
                }
                maxProb = mergeArrays(maxProb, newProb);
            }
            if (originlist.size() > 1)
            {
                System.out.println("Mehr als 1 bb");
            }
            //if (ParserModel.timeProfile) System.out.println("b"+maxProb);
            if (ParserModel.verbose)
            {
                System.out.println(string + maxProb);
            }
            fap.setProb(maxProb);
            return maxProb;
        }
    }
    
    private Pair<double[], double[]> recursiveUpdateProbsDiscriminative(FringeAndProb fap, String string, double modelScoreNoBaseline, 
            double baselineWeight, int prefixLength, boolean pruneUsingScores, boolean usePrefixBaselineFeature)
    {
        if (fap.getNext() == null || fap.getNext().isEmpty())
        {            
//            double wordProbability = originlist.get(0).getProbability();
            double prefixTreeProbability = fap.getBestProb();//+ wordProbability;
            double prefixScore = usePrefixBaselineFeature ? (prefixTreeProbability / (double) prefixLength) * baselineWeight + modelScoreNoBaseline : modelScoreNoBaseline;
            probs.add(prefixTreeProbability);
            scores.add(prefixScore);
            Pair<double[], double[]> scoresProbs = newNegativeInfinitiyPairArray(nBest);
            // store model scores, with top value only
            scoresProbs.getFirst()[0] = prefixScore;            
            // store prefix tree probabilities, with top value only
            scoresProbs.getSecond()[0] = prefixTreeProbability;
            fap.setScoresProbs(scoresProbs);
            return scoresProbs;
        }
        else
        {            
            Pair<double[], double[]> maxScoresProbs = newNegativeInfinitiyPairArray(nBest);
            for (FringeAndProb nextfap : fap.getNext())
            {                
                Pair<double[], double[]> newScoresProbs = recursiveUpdateProbsDiscriminative(nextfap, string + "\t", modelScoreNoBaseline, baselineWeight, prefixLength, pruneUsingScores, usePrefixBaselineFeature);
                if (newScoresProbs != null)                
                {
//                    maxScoresProbs = mergeArrays(maxScoresProbs, newScoresProbs, pruneUsingScores);
                    maxScoresProbs = mergeArrays(maxScoresProbs, newScoresProbs, true);
                }
            }
            fap.setScoresProbs(maxScoresProbs);
            return maxScoresProbs;
        }
    }

    public static Pair<double[], double[]> newEmptyPairArray(int size)
    {
        return new Pair<double[], double[]>(new double[size], new double[size]);
    }
    
    public static Pair<double[], double[]> newNegativeInfinitiyPairArray(int size)
    {
        Pair<double[], double[]> res = newEmptyPairArray(size);
        Arrays.fill(res.getFirst(), Double.NEGATIVE_INFINITY);
        Arrays.fill(res.getSecond(), Double.NEGATIVE_INFINITY);
        return res;
    }
    
    public Double recursiveSumProbs(FringeAndProb fap, double minkonst)
    {
        double sum = 0;
        //	if (minkonst < 0)
        //		System.out.println();
        for (double p : probs)
        {
            sum += Math.exp(p - minkonst);
        }
        return sum;
    }
    
    public double recursiveSumScores(FringeAndProb fap, double baselineWeight, int prefixLength, double minConst)
    {
        double sum = 0;        
        for(int i = 0; i < scores.size(); i++)
        {            
            double modelScore = scores.get(i) - (baselineWeight * (probs.get(i) / prefixLength));
            if(modelScore != 0.0)
            {
                double exp = Math.exp(modelScore - minConst);
                sum += exp / (1 + exp);
            }            
        }
//        sum = scores.stream().map((s) -> Math.exp(s - minConst)).map((exp) -> exp / (1 + exp)).reduce(sum, (accumulator, _item) -> accumulator + _item);
        return sum;
    }
    
    public double recursiveSumBaselineScores(FringeAndProb fap, double baselineWeight, int prefixLength, double minConst)
    {
        double sum = 0;        
        for(int i = 0; i < probs.size(); i++)
        {            
            Double exp = Math.exp((baselineWeight * (probs.get(i) / prefixLength)) - minConst);
            sum += exp / (1 + exp);
        }
//        sum = scores.stream().map((s) -> Math.exp(s - minConst)).map((exp) -> exp / (1 + exp)).reduce(sum, (accumulator, _item) -> accumulator + _item);
        return sum;
    }

    public boolean recursivePrune(FringeAndProb fap, double threshold, boolean discriminative)
    {//, Boolean isShadowPhase) {
        //if (isShadowPhase!=null) 
        //	lastFringe.nextSlice(isShadowPhase);
        Double lastProb = discriminative ? fap.getBestScore() : fap.getBestProb();
        if (lastProb == null)
        {
            return false; //false means that it'll be pruned
        }
        if (lastProb < threshold)
        {
            //System.out.println("\t"+threshold + "prune BBs");
            //lastFringe.prune(this);
            //if (lastFringe.hasNoProbs()) 
            return false;//false means that it'll be pruned
        }
        else
        {
            if (fap.getNext() != null)
            {
                ArrayList<FringeAndProb> removeList = new ArrayList<FringeAndProb>();
                for (FringeAndProb af : fap.getNext())
                {
                    boolean keep = recursivePrune(af, threshold, discriminative);
                    if (!keep)
                    {
                        removeList.add(af);
                    }
                }
                fap.getNext().removeAll(removeList);
            }
        }
        return true;
    }

    public void setProbabilities(double[] probability)
    {
        this.nBestProbabilities = probability;
    }

    public void setScores(double[] nBestScores)
    {
        this.nBestScores = nBestScores;
    }

    public double getTop(int beamThreshold, boolean pruneUsingScores)
    {
//        if (beamThreshold >= probs.size())
//        {
////            return -10000;
//            return -100000;
//        }
//        Double[] da = probs.toArray(new Double[probs.size() - 1]);
//        Arrays.sort(da);
//        return da[probs.size() - beamThreshold];//-0.001;
        
        Double[] da = pruneUsingScores ? scores.toArray(new Double[0]) : probs.toArray(new Double[0]);
        
        Arrays.sort(da);
        int length = da.length;
        return beamThreshold >= length ? (pruneUsingScores ? MIN_THRESHOLD_DISCR_VALUE : MIN_THRESHOLD_VALUE) : da[length - beamThreshold];
    }

    public String getPrevPOStags()
    {
        String prevPosTags = "";
        int counter = 0;
        ChartEntry ce = this;
        while (!ce.getBuildBlocks().isEmpty() && counter < 3)
        {
            BuildBlock bb = ce.getBuildBlocks().get(0);
            String bbstring = bb.getElemTreeString();
            if (bbstring.contains("<>"))
            {
                bbstring = bbstring.substring(0, bbstring.indexOf("<>"));
                bbstring = bbstring.substring(bbstring.lastIndexOf("(") + 2, bbstring.lastIndexOf("^") - 1);
                prevPosTags = bbstring + "-" + prevPosTags;
                counter++;
                ce = bb.getPrevChartEntry();
            }
            else
            {
                ce = bb.getPrevChartEntry();
                continue;
            }
        }
        if (prevPosTags.equals(""))
        {
            return "-";
        }
        return prevPosTags;
    }

    public ArrayList<BuildBlock> getBuildBlocksClone()
    {
        ArrayList<BuildBlock> clone = new ArrayList<BuildBlock>();
        for (BuildBlock bb : this.originlist)
        {
            clone.add(bb.clone());
        }
        return clone;
    }
}