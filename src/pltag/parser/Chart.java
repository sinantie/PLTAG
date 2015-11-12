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

import fig.basic.LogInfo;
import fig.basic.Pair;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections4.map.MultiValueMap;


public class Chart implements Serializable
{
    
    static final long serialVersionUID = -1L;
    private transient Options opts;
    
    private LinkedList<ArrayList<ChartEntry>> treeStateListArray;
    private transient MultiValueMap<String, ChartEntry> chartEntrySorter;//fringe -> pointer
    private long startTime = 0;
    
    public Chart() 
    {
    }
    
    public Chart(Options opts, String[] words)
    {
        this.opts = opts;
        treeStateListArray = new LinkedList<ArrayList<ChartEntry>>();
        if (opts.aggregate)
        {
            chartEntrySorter = new MultiValueMap();
        }
        for (int i = 0; i < words.length; i++)
        {
            treeStateListArray.add(new ArrayList<ChartEntry>());
            //if (ParserRunner.aggregate) chartEntrySorter[i] = new MultiValueMap();
        }
    }

    public void putAll(ArrayList<ChartEntry> entryList, double startTime, boolean discriminative, boolean pruneUsingScores)
    {
        //double startTime = System.currentTimeMillis();
        if (!entryList.isEmpty())
        {
            int slice = entryList.get(0).getTreeState().getWordCover()[1];
            treeStateListArray.get(slice).ensureCapacity(entryList.size());
            for (ChartEntry ce : entryList)
            {
                if (opts.train && System.currentTimeMillis() - startTime > 300000)
                {
                    break;
                }
                try
                {
                    //double sum = ce.getBuildBlocks().get(0).getProbability()+ce.getBuildBlocks().get(0).getPrevChartEntry().getBestProbability();
                    //System.out.println(ce.getBuildBlocks().get(0));//+"\t"+ce.getBuildBlocks().get(0).getPrevChartEntry());
                    put(ce, startTime, discriminative, pruneUsingScores);
                }
                catch (TimeOutDuringSearchException e)
                {
                    LogInfo.error("Time out " + e);
                    return;
                }
                //System.out.println("Chart.putAll:"+ce.getTreeState().getFutureFringe());
            }
            treeStateListArray.get(slice).trimToSize();
        }

        //System.out.println("putAll took"+(System.currentTimeMillis()-startTime)/1000 +" secs." );
    }

    /**
     * add a new chart entry to the chart and register it in the chartEntrySorter.
     * @param chartEntry
     * @param startTime2 
     * @param discriminative use discriminative model to rerank chart entries
     * @param pruneUsingScores prune chart using discriminative model scores
     * @throws TimeOutDuringSearchException 
     */
    public void put(ChartEntry chartEntry, double startTime2, boolean discriminative, boolean pruneUsingScores) throws TimeOutDuringSearchException
    {
        TreeState ts = chartEntry.getTreeState();
        if (opts.aggregate)
        {
            ChartEntry existingCE = getChartEntryWithSameFringe(ts);
            if (existingCE != null)
            {
                //System.out.print("2");
                existingCE.combineCEs(chartEntry, discriminative, pruneUsingScores);
            }
            else
            {
                int slice = ts.getWordCover()[1];
                treeStateListArray.get(slice).add(chartEntry);
                chartEntrySorter.put(ts.getFringe().toString(), chartEntry);
            }
            if (opts.train && System.currentTimeMillis() - startTime2 > 300000)
            {
                throw new TimeOutDuringSearchException(System.currentTimeMillis() - startTime2);
            }
        }
        else
        {
            int slice = ts.getWordCover()[1];
            treeStateListArray.get(slice).add(chartEntry);
        }
    }

    /**
     * expands the fringe as needed using fringe continuations (making currently
     * invisible unaccessible fringes visible) and adding a version of the fringe
     * which is shifted to after-empty-element-position if applicable.   
    
    @SuppressWarnings("unchecked")
    private ArrayList<ChartEntry> makeExpansions(ChartEntry ce) {
    ArrayList<ChartEntry> expandedCEs =  new ArrayList<ChartEntry> ();
    TreeState treeStateShrunk = ce.getTreeState();
    if (treeStateShrunk.getUnaccessibles().isEmpty()){
    ArrayList<TreeState> expanded = treeStateShrunk.expand(ce);
    if (expanded == null) return expandedCEs;
    for (TreeState expandedTS : expanded){
    for (TreeState traceFixedExpanded : expandedTS.fixForTrace()){
    ArrayList<TreeState> fullyExpanded; 
    if (TreeState.isNullLexeme(traceFixedExpanded.getFringe().getAdjNodesOpenRight().get(0).getCategory())&&
    traceFixedExpanded.getUnaccessibles().isEmpty()){
    fullyExpanded = traceFixedExpanded.expand(ce);
    if (fullyExpanded== null){
    fullyExpanded = new ArrayList<TreeState>();
    fullyExpanded.add(traceFixedExpanded);
    }else if (fullyExpanded.isEmpty())
    fullyExpanded.add(traceFixedExpanded);
    }
    else{
    fullyExpanded = new ArrayList<TreeState>();
    fullyExpanded.add(traceFixedExpanded);
    }
    for (TreeState fex : fullyExpanded){
    ChartEntry expandedCE = new ChartEntry(fex, (ArrayList<BuildBlock>) ce.getBuildBlocks().clone());
    if (!ParserModel.train) //expandedCE.setProbability(fex.getLastFringe().getProbability(ce));
    expandedCE.setProbability(fex.getFutureFringe().getnBestProbs());
    expandedCEs.add(expandedCE);
    }
    }
    }
    }
    if (expandedCEs.isEmpty()){
    expandedCEs.add(ce);
    }
    return expandedCEs;
    }
    //*/
    /**
     * return those entries with a fringe that is completely identical (also hidden bits of fringe).
     * @param ts
     * @return
     */
    @SuppressWarnings("unchecked")
    private ChartEntry getChartEntryWithSameFringe(TreeState ts)
    {
        Collection<ChartEntry> relevants =
                (Collection<ChartEntry>) chartEntrySorter.get(ts.getFringe().toString());
        if (relevants == null)
        {
            return null;
        }
        for (ChartEntry ce : relevants)
        {
            if (ce.getTreeState().identical(ts))
            {
                //System.out.print(" "+ce.getTreeState().getUnaccessibles().size() + ce.getBuildBlocks().get(0).getOperation());
                return ce;
            }
        }
        return null;
    }

    public int getTreeStateIndexNo(ArrayList<ChartEntry> currentSlice, TreeState ts)
    {
        int no = -1;
        for (ChartEntry ce : currentSlice)
        {
            no++;
            if (ce.getTreeState().identical(ts))
            {
                return no;
            }
        }

        return no;
    }

    public ArrayList<TreeState> getTreeStates(ArrayList<ChartEntry> currentSlice)
    {
        ArrayList<TreeState> statelist = new ArrayList<TreeState>();
        for (ChartEntry ce : currentSlice)
        {
            statelist.add(ce.getTreeState());
        }
        return statelist;
    }

    /**
     *
     * This method is used to find the n-best paths through the chart, and thereby build the n-best trees.
     * @param ce
     * @param contList
     * @param nextPOStag
     * @param top
     * @param targetprob
     * @param train
     * @return 
     */
    @SuppressWarnings("unchecked")
    public ArrayList<StringTreeAnalysis> getAnalyses(ChartEntry ce, ArrayList<ArrayList<BuildBlock>>[] contList, 
    String nextPOStag, boolean top, double targetprob, boolean train)
    {
//        if (ParserModel.useTimeOut && System.currentTimeMillis() - startTime > 600000)
        if (ParserModel.useTimeOut && System.currentTimeMillis() - startTime > opts.timeOutStage2)
        {//1000 = 1sec, 60 000 = 1 min, 300 000 = 5 min
//            LogInfo.logs("Time out at stage 2 (10min).");
            if(!opts.silentMode)
                LogInfo.logs("Time out at stage 2 - analyses reconstruction ( " + (((double)opts.timeOutStage2 / 1000.0)/60.0)+ " min).");
//            ParserRunner.printConsole("Time out at stage 2 (10min).\t");
            return new ArrayList<StringTreeAnalysis>(); //null;
        }
        /*if ( Runtime.getRuntime().freeMemory()/(1024*1024) < 20){//1000 = 1sec, 60 000 = 1 min, 300 000 = 5 min
        System.out.print("-");//Time out at stage 2 (1min).");
        ParserRunner.printConsole("less than 20MB free mem left-A-.\t");
        return null;
        }//*/
        ArrayList<StringTreeAnalysis> derivations = new ArrayList<StringTreeAnalysis>();
        if (ce == null)
        {
            return new ArrayList<StringTreeAnalysis>(); //null;
        }
        //System.out.print("["+ce.getTreeState().getWordCover()[1]+", ");
        //System.out.print(treestatelistarray.get(ce.getTreeState().getWordCover()[1]).indexOf(ce)+"]");
        
       /*
        * in the following block of code, the goal is to identify a list of buildBlocks which were used for
        * generating the best analyses, based on the determined top probability analysis (basically, I am
        * trying to recover it by checking whether the determined best log-probability is equal to the sum of the
        * log-probs of the buildblocks used to create the analysis.
        * In a first step, we make sure to exclude any inconsistent analyses (that's what the restrictlist is used for).
        * In a second step, the buildblocks for analyses with matching probability are calculated.
        * There could be several analyses with same probability; all of these are calculated,
        *  we therefore might have several buildblocks.
        */
        ArrayList<ArrayList<BuildBlock>> restrictList = contList[ce.getTreeState().getWordCover()[1]];
        StringBuilder bbidbuilder = new StringBuilder();
        int max = ce.getTreeState().getWordCover()[1];
        //if (max >= contList.length) max = contList.length-1;
        for (int z = max; z >= 0; z--)
        {
            if (contList[z] != null && !contList[z].isEmpty())
            {
                bbidbuilder.append(contList[z].hashCode());
            }
        }
//        List<BuildBlock> buildBlocks;
        List<Pair<BuildBlock, Integer>> buildBlocks;
        if (restrictList != null && !restrictList.isEmpty())
        {
//            buildBlocks = (ArrayList<BuildBlock>) ce.getBuildBlocks().clone();
            buildBlocks = newBuildBlockList((ArrayList<BuildBlock>) ce.getBuildBlocks().clone());
            for (ArrayList<BuildBlock> restriction : restrictList)
            {
                List<Pair<BuildBlock, Integer>> restrictionPair = newBuildBlockList(restriction);
//                ArrayList<BuildBlock> intersection = (ArrayList<BuildBlock>) ce.getBuildBlocks().clone();
                List<Pair<BuildBlock, Integer>> intersection = newBuildBlockList((ArrayList<BuildBlock>) ce.getBuildBlocks().clone());
//                intersection.retainAll(restriction);
                intersection.retainAll(restrictionPair);
                if (intersection.isEmpty())
                {
                    if (ce.getBuildBlocks().get(0).getOperation().name().endsWith("S"))
                    {
                        continue;
                    }
                }
                else
                {
                    buildBlocks.retainAll(intersection);
                }
            }
            if (buildBlocks.isEmpty())
            {
//                buildBlocks = ce.getBuildBlocks();
                buildBlocks = newBuildBlockList(ce.getBuildBlocks());
            }
        }
        else
        {
//            buildBlocks = ce.getBuildBlocks();
            buildBlocks = newBuildBlockList(ce.getBuildBlocks());
        }/*just for baseline.
        BuildBlock single = buildBlocks.get(0);
        buildBlocks.clear();
        buidBlocks.add(single);*/
        if (top && ce.getTreeState().getWordCover()[1] > 0)
        {// && buildBlocks.size()>1){
            //double maxProb = Double.NEGATIVE_INFINITY;
            ArrayList<Pair<BuildBlock, Integer>> altlist = new ArrayList<Pair<BuildBlock, Integer>>();
            for (int i = buildBlocks.size() - 1; i >= 0; i--)
            {
//                BuildBlock bb = buildBlocks.get(i);
                Pair<BuildBlock, Integer> bbPair = buildBlocks.get(i);
                BuildBlock bb = bbPair.getFirst();
                for (double d : bb.getPrevChartEntry().getNBestProbs())
                {
                    if (d == Double.NEGATIVE_INFINITY)
                    {
                        continue;
                    }
                    double bbprobdiff = bb.getProbability() + d - targetprob;// bb.getPrevChartEntry().getNBestProb()//.getBestProbability();
					/*if (bbprob > maxProb){
                    maxProb = bbprob;
                    altlist.clear();altlist.add(bb);
                    }
                    else*/
//                    if (Math.abs(bbprobdiff) < 0.00000001 && !altlist.contains(bb))
                    if (Math.abs(bbprobdiff) < 0.00000001 && !altlist.contains(bbPair))
                    {
                        altlist.add(new Pair(bb, i));
                        //	System.out.print("->");
                    }
                }
            }
            buildBlocks.clear();
            buildBlocks.addAll(altlist);
            //if (buildBlocks.isEmpty()) System.out.println("Failed to find path back at word +"+ce.getTreeState().getWordCover()[1]);
        }

        bbidbuilder.append(treeStateListArray.get(ce.getTreeState().getWordCover()[1]).indexOf(ce));
        String bbid = bbidbuilder.toString();       
       /*
        * Now we loop over all buildblocks (these should all yield the analysis with target probability)
        */        
        int bbi = 0;
//        for (BuildBlock bb : buildBlocks)
        for (Pair<BuildBlock, Integer> bbPair : buildBlocks)
        {
            BuildBlock bb = bbPair.getFirst();
            //if (bbi>0) System.out.print("\t");
            double res = targetprob - bb.getProbability();
            //double rem = res - bb.getPrevChartEntry().getBestProbability();
//System.out.println(ce.getTreeState().getWordCover()[1]+" "+bbi+"\t"+ bb.getProbability() + "\t"+ bb.getPrevChartEntry().getBestProbability() +"\t"+ bb);//.getProbability()+"\t"+res + "\t"+ bb.getPrevChartEntry().getBestProbability()+"\t"+rem);
            //System.out.print("\n"+ce.getTreeState().getWordCover()[1]+" "+bbi+"\t"+ bb.getProbability() + "\t"+ bb.getOperation());			
//            bbi++;
            if (bb.getOperation() == ParserOperation.initial)
            {
                if (!top || Math.abs(res) < 0.00000001)// in the end it has to sum to zero, otherwise something went wrong.
                {
                    Fringe f = ce.getTreeState().getFringe();
                    f.setClusterNumberCount(true);
                    StringTreeAnalysis sta = new StringTreeAnalysis(opts, bb, f);
                    //if (ParserModel.train) 
                    // here we start building the proper tree.
                    // the following lines with buildTrace are for keeping track of the events that occur in the analysis
                    // which are needed for training, where we want to count the events that lead to the correct analysis.
                    
                    // changed "-" to Integer.MAX_VALUE. "-" is an illegal value, that is never used in the 
                    // called method, otherwise it would throw an exception, as it refers to an array index.
                    if(train)
                        sta.setTrace(bb.getFreqCounter().getWordProbs().get(0), bb.getFreqCounter().getTreeProbs(), Integer.MAX_VALUE, ParserOperation.initial, sta.getStringTree(), null, "-");
//                    sta.setTrace(bb.getFreqCounter().getWordProbs().get(0), bb.getFreqCounter().getTreeProbs(), "-", ParserOperation.initial, sta.getStringTree(), null, "-");
                    sta.getStringTree().setProbability(bb.getProbability());
                    //so here our analysis, which we are building up, is added to the set of trees that are constructed.
                    derivations.add(sta);
                    bb.addAnalysis(buildBlocks.toString(), sta);
                    sta.addToBuiltTrace("[" + ce.getTreeState().getWordCover()[1] + ", ");
//                    sta.addToBuiltTrace(treeStateListArray.get(ce.getTreeState().getWordCover()[1]).indexOf(ce) + "]");
//                    sta.addToBuiltTrace(treeStateListArray.get(ce.getTreeState().getWordCover()[1]).indexOf(ce) + " " + bbPair.getSecond()  +  " " + (bb.getAnalyses(buildBlocks.toString()).size() - 1) + "]");
                    sta.addToBuiltTrace(treeStateListArray.get(ce.getTreeState().getWordCover()[1]).indexOf(ce) + " " + bbPair.getSecond()  +  " " + targetprob + "]");
                    
                }
                //	System.out.println("");
            }
            else // for all other parser operations beside "initial"
            {
                // we don't want to do the same work over and over if calculating more than
                // one analysis. (In particular, it often happens that several final analysis start out the same.)
                // Therefore, we just re-use those trees that we've already built.
                if (!top && bb.hasAnalysis(bbid)) 
                {
                    derivations.addAll(bb.getAnalyses(bbid));
                    //System.out.println("has analysis");
                    //		System.out.println("");
                }
                else
                {
               /*
                * if we haven't done the tree that we need yet, still need to do the proper building work...
                * again, we first need to make sure that everything is consistent, and we use the history of
                * buildblocks for that (they are here put into contListNew). Basically, the idea is that the
                * buildblock we're working on now gives us some restrictions on what other buildblocks are
                * consistent with it.
                */
                    FringeAndProb fap = bb.getPrevChartEntry().getTreeState().getFutureFringe();
                    ArrayList<BuildBlock>[] contListNew = new ArrayList[contList.length];
                    int wind = 0;
                    if (fap.getBBHist() != null)
                    {
                        for (LinkedList<BuildBlock> lb : fap.getBBHist())
                        {
                            for (BuildBlock b : lb)
                            {
                                if (b.getPrevChartEntry() == null)
                                {
                                    continue;
                                }
                                if (contListNew[wind] == null)
                                {
                                    contListNew[wind] = new ArrayList<BuildBlock>();
                                }
                                if (!contListNew[wind].contains(b))
                                {
                                    contListNew[wind].add(b);
                                }
                            }
                            wind++;
                        }
                    }
                    ArrayList<ArrayList<BuildBlock>>[] contListRes = new ArrayList[contList.length];//contList.clone();// new ArrayList[contList.length];//
                    for (int i = 0; i < contListNew.length; i++)
                    {
                        ArrayList<BuildBlock> bblist = contListNew[i];
                        if (bblist != null && !bblist.isEmpty())
                        {
                            if (contList[i] == null)
                            {
                                contListRes[i] = new ArrayList<ArrayList<BuildBlock>>();
                            }
                            else
                            {
                                contListRes[i] = (ArrayList<ArrayList<BuildBlock>>) contList[i].clone();
                            }                            
                            contListRes[i].add(bblist);
                        }
                        else
                        {
                            contListRes[i] = contList[i];//.clone();
                        }
                    }

                    String nextPos = bb.getElemTreeString();
                    int anchorind = nextPos.indexOf("+<>");
                    if (anchorind == -1)
                    {
                        if (nextPos.contains("^null_1"))
                        {
                            anchorind = nextPos.indexOf("+^1_1)");
                        }
                        if (anchorind == -1 && nextPos.contains("^null_2"))
                        {
                            anchorind = nextPos.indexOf("+^2_2)");
                            if (anchorind == -1)
                            {
                                anchorind = nextPos.indexOf("_2)") - 3;
                            }
                        }
                        if (anchorind == -1 && nextPos.contains("^null_3"))
                        {
                            anchorind = nextPos.indexOf("+^3_3)");
                            if (anchorind == -1)
                            {
                                anchorind = nextPos.indexOf("_3)") - 3;
                            }
                        }
                        if (anchorind == -1 && nextPos.contains("^null_4"))
                        {
                            anchorind = nextPos.indexOf("+^4_4)");
                            if (anchorind == -1)
                            {
                                anchorind = nextPos.indexOf("_4)") - 3;
                            }
                        }
                        if (anchorind == -1 && nextPos.contains("^null_5"))
                        {
                            anchorind = nextPos.indexOf("+^5_5)");
                            if (anchorind == -1)
                            {
                                anchorind = nextPos.indexOf("_5)") - 3;
                            }
                        }
                        if (anchorind == -1 && nextPos.contains("^null_6"))
                        {
                            anchorind = nextPos.indexOf("+^6_6)");
                            if (anchorind == -1)
                            {
                                anchorind = nextPos.indexOf("_6)") - 3;
                            }
                        }

                        nextPos = nextPos.substring(0, anchorind);
                        nextPos = nextPos.substring(nextPos.lastIndexOf(" ") + 1);
                    }
                    else
                    {
                        nextPos = nextPos.substring(0, anchorind);
                        nextPos = nextPos.substring(nextPos.lastIndexOf("( ") + 2);
                        nextPos = nextPos.substring(0, nextPos.indexOf("^") - 1);
                    }
                   /*
                    * here is now the recursive call which will give us the analysis built up to this point.
                    */
                    ArrayList<StringTreeAnalysis> prevDerivations = getAnalyses(bb.getPrevChartEntry(), contListRes, nextPos, top, targetprob - bb.getProbability(), train);
                    
                    if(prevDerivations != null)
                    {
                       /*
                        * for all derivations up to the current point, we need to now integrate the current buildblock
                        * (or more correctly, the elementary tree that buildblock tells us to, using the operation and integration
                        * site saved in buildblock).
                        */
                        for (StringTreeAnalysis deriv : prevDerivations)
                        {
                            if (deriv == null)
                            {
                                System.out.println("prevderiv empty.");
                                continue;
                            }
                            if (opts.verbose)//just some trace to see what's going on.
                            {
                                System.out.println("Chart:getAnalyses: " + deriv.getStringTree() + "\n + " + bb.getElemTreeString());
                                System.out.println(ce.getTreeState());
                                FringeAndProb futureFringe = ce.getTreeState().getFutureFringe();
                                //if (ce.getTreeState().getFringe()!=null && !ce.getTreeState().getFringe().isEmpty())
                                //System.out.println(futureFringe.getFringe());
                                if (futureFringe != null && futureFringe.hasNext())
                                {
                                    System.out.println(futureFringe.getNext());
                                }
                                System.out.println();
                            }
                           /*
                            * So here is the real meat: the prefix tree gets integrated with the new elementary tree.
                            */
                            StringTreeAnalysis result = deriv.integrate(bb, bb.getPrevChartEntry().getTreeState(), ce.getTreeState(), nextPOStag, train);                            
                            // after that we just deal with what went wrong, and store the analysis and return it
//                            if (opts.timeProfile)
//                            {
//                                System.out.println(result);
//                                System.err.println(ce.getTreeState());
//                            }
                            if (result == null)
                            {
    //                            System.out.print("."); // COMMENTED OUT
                                //System.out.println("incorrect analysis."+bb+"\n("+deriv+")\n");
                                if (ParserModel.useTimeOut && System.currentTimeMillis() - startTime > opts.timeOutStage2)
                                {//1000 = 1sec, 60 000 = 1 min, 300 000 = 5 min
                        //            LogInfo.logs("Time out at stage 2 (10min).");
                                    if(!opts.silentMode)
                                        LogInfo.logs("Time out at stage 2 - analyses reconstruction ( " + (((double)opts.timeOutStage2 / 1000.0)/60.0)+ " min).");
//                                        System.currentTimeMillis() - startTime > 1800000)
    //                                ParserRunner.printConsole("Time out at stage 2 (30min).\t");
                                    return null;
                                }
                                deriv.integrate(bb, bb.getPrevChartEntry().getTreeState(), ce.getTreeState(), nextPOStag, train);
                                continue;
                            }                            
                            derivations.add(result);
                            bb.addAnalysis(bbid, result);
                            result.addToBuiltTrace("[" + ce.getTreeState().getWordCover()[1] + ", ");
//                            result.addToBuiltTrace(treeStateListArray.get(ce.getTreeState().getWordCover()[1]).indexOf(ce) + " " + bbPair.getSecond()  +  " " + (bb.getAnalyses(bbid).size() - 1) + "]");
                            result.addToBuiltTrace(treeStateListArray.get(ce.getTreeState().getWordCover()[1]).indexOf(ce) + " " + bbPair.getSecond()  +  " " + targetprob + "]");
                            if (ParserModel.useTimeOut && System.currentTimeMillis() - startTime > opts.timeOutStage2)
                            {//1000 = 1sec, 60 000 = 1 min, 300 000 = 5 min
                    //            LogInfo.logs("Time out at stage 2 (10min).");
                                if(!opts.silentMode)
                                    LogInfo.logs("Time out at stage 2 - analyses reconstruction ( " + (((double)opts.timeOutStage2 / 1000.0)/60.0)+ " min).");
    //                            ParserRunner.printConsole("Time out at stage 2 (10min).\t");
                                return derivations;
                            }//if ( Runtime.getRuntime().freeMemory()/(1024*1024) < 100){//1000 = 1sec, 60 000 = 1 min, 300 000 = 5 min
                            //System.out.print("o");//Time out at stage 2 (1min).");
                            //StatsRunner.printConsole("less than 20MB free mem left.\t");
                            //return derivations;
                            //}//*/						
                        } // for prevDerivations
                    }                    
                } // else
            } // else
            bbi++;
        }// for
        //if (!derivations.isEmpty())
        //	System.out.print("!"+derivations.get(0).getBuiltTrace());
        //	System.out.println("empty deriv.");

        return derivations;
    }

    private List<Pair<BuildBlock, Integer>> newBuildBlockList(List<BuildBlock> ceBbs)
    {
        List<Pair<BuildBlock, Integer>> list = new ArrayList();
        for(BuildBlock bb : ceBbs)
            list.add(new Pair(bb, 0));
        return list;
    }
    @SuppressWarnings("unchecked")
    private ArrayList<BuildBlock> combineRestrictList(
            ArrayList<BuildBlock> arrayList, ArrayList<BuildBlock> arrayList2)
    {
        if (arrayList == null)
        {
            return arrayList2;
        }
        if (arrayList2 == null)
        {
            return arrayList;
        }
        ArrayList<BuildBlock> alc = (ArrayList<BuildBlock>) arrayList.clone();
        alc.addAll(arrayList2);
        return alc;
    }

    private Integer getWIndex(BuildBlock b)
    {
        int index = b.getPrevChartEntry().getTreeState().getWordCover()[1];
        if (!b.getOperation().toString().endsWith("S"))
        {
            index++;
        }
        return index;
    }

    /*find b in chart.
    private String getId(BuildBlock b) {
    StringBuilder sb = new StringBuilder("]");
    int sliceId;
    if (! b.getOperation().toString().endsWith("S")){
    sliceId = b.getPrevChartEntry().getTreeState().getWordCover()[1]+1;
    }
    else sliceId =  b.getPrevChartEntry().getTreeState().getWordCover()[1];
    sb.append(sliceId).append(",");
    treestatelistarray[sliceId].indexOf(b)
    return sb.toString();
    }*/
    public ArrayList<ChartEntry> get(int position)
    {
        if (position < 0 || position >= treeStateListArray.size())
        {
            return null;
            /*Fringe noContext = new Fringe(ParserOperation.initial); 
            ArrayList<Fringe> newChart = new ArrayList<Fringe>();
            newChart.add(noContext);
            return newChart;
             */
        }
        return treeStateListArray.get(position);
    }

    public ChartEntry getEntry(Integer[] pos)
    {
        int sliceNo = pos[0];
        int arrayPos = pos[1];
        if (sliceNo < 0 || sliceNo >= treeStateListArray.size()
                || arrayPos >= treeStateListArray.get(sliceNo).size())
        {
            return null;
        }
        return treeStateListArray.get(sliceNo).get(arrayPos);
    }

    /*
    public Collection<String> getFringes(int sliceNo){
    return chartEntrySorter[sliceNo].keySet();
    }
    
    public Collection<ChartEntry> getEntriesByFringe(int sliceNo, String fringe){
    return chartEntrySorter[sliceNo].getCollection(fringe);
    }
    
    
    public boolean charttest(int sliceno){
    return this.chartEntrySorter[sliceno].values().size() == treestatelistarray[sliceno].size();
    }
     */
    public String printChartSize()
    {
        String string = "";
        for (int i = 0; i < treeStateListArray.size(); i++)
        {
            if (treeStateListArray.get(i).isEmpty())
            {
                string += i + "-" + treeStateListArray.size() + ": 0\n";
                return string;
            }
            string += i + ": " + treeStateListArray.get(i).size() + "\n";
        }
        return string;
    }

    public void setTime(long currentTimeMillis)
    {
        startTime = currentTimeMillis;
    }

    public String printChartStats(int currentWordNo)
    {
        ArrayList<ChartEntry> thisSlice = treeStateListArray.get(currentWordNo);
        return "number of entries: " + thisSlice.size()
                + getAggregationFactor(thisSlice)
                + "   thereof unique fringes: "
                + chartEntrySorter.keySet().size();
    }

    private String getAggregationFactor(ArrayList<ChartEntry> thisSlice)
    {
        if (!opts.aggregate)
        {
            return "";
        }
        int buildBlocksNumber = 0;
        for (ChartEntry ce : thisSlice)
        {
            buildBlocksNumber += ce.getBuildBlocks().size();
        }
        return " (" + Integer.toString(buildBlocksNumber) + ") ";
    }

    public void print(int wno)
    {
        int jnum = 0;
        System.out.println("Chart slice size: " + get(wno).size());
        for (ChartEntry j : get(wno))
        {
            for (BuildBlock bb : j.getBuildBlocks())
            {
                //BuildBlock bb = j.getBuildBlock();
                System.out.println(wno + "," + jnum + ": " + bb.toString() + " " + j.getTreeState());
            }
            jnum++;
        }
    }

    public boolean removeAll(int sliceNo, ArrayList<ChartEntry> removeList)
    {
        for (ChartEntry entry : removeList)
        {

            for (BuildBlock b : entry.getBuildBlocks())
            {
                b = null;
            }
            boolean r1 = treeStateListArray.get(sliceNo).remove(entry);
            chartEntrySorter.remove(entry.getTreeState().getFringe().toString(), entry);
            if (!r1)
            {// || r1 && chartEntrySorter[sliceNo].remove(entry.getTreeState().getFringe().toString(), entry)==null){
                return false;
            }
        }
        return true;
    }

    public void compressSlice(int i)
    {
        if (i < 0)
        {
            return;
        }
        ArrayList<ChartEntry> slice = treeStateListArray.get(i);
        int size = slice.size();
        for (int j = size - 1; j >= 0; j--)
        {
            ChartEntry ce = slice.get(j);
            if (!ce.wasUsed())
            {
                for (BuildBlock b : ce.getBuildBlocks())
                {
                    b = null;
                }
                slice.remove(ce);
                //chartEntrySorter.remove(ce);
            }
            else
            {
                ce.minimizeSpace();
            }
        }
        slice.trimToSize();
        if (opts.aggregate)
        {
            chartEntrySorter.clear();
        }
    }   

    public int length()
    {
        return this.treeStateListArray.size();
    }
    
    public void clear()
    {
        treeStateListArray.clear();
        chartEntrySorter.clear();
    }
}
