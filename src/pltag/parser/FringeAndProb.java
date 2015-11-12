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
import java.util.HashSet;
import java.util.LinkedList;
import static pltag.parser.ChartEntry.newEmptyPairArray;

public class FringeAndProb implements Serializable
{

    static final long serialVersionUID = -1L;
    private ArrayList<Fringe> fringePointer; // unaccessible (future) fringes
    private int nBest;
    private double[] nBestProbs;
    private double[] nBestScores;
    private ArrayList<FringeAndProb> next = null;
    private transient LinkedList<LinkedList<BuildBlock>> history = null;

    public FringeAndProb()
    {        
    }
    
    public FringeAndProb(ArrayList<Fringe> fp, int nBest)
    {
        fringePointer = fp;
        this.nBest = nBest;
        nBestProbs = new double[nBest];
        Arrays.fill(nBestProbs, 0.0);
    }

    public FringeAndProb(ArrayList<Fringe> fp, double[] probs, double[] scores)
    {
        fringePointer = fp;
        nBestProbs = probs;
        nBestScores = scores;
        nBest = nBestProbs.length;
    }

    public FringeAndProb(ArrayList<Fringe> fp, double[] p, ArrayList<FringeAndProb> next)
    {
        fringePointer = fp;
        nBestProbs = p;
        nBest = nBestProbs.length;
        this.next = next;
    }

    public FringeAndProb(ArrayList<Fringe> fp, double[] p, ArrayList<FringeAndProb> next, LinkedList<LinkedList<BuildBlock>> history)
    {
        fringePointer = fp;
        nBestProbs = p;
        nBest = nBestProbs.length;
        this.next = next;
        this.history = history;
        this.history = this.getBBHistClone();
    }

    public void mergeProbs(FringeAndProb CEFutFringe)
    {
        double[] thisNbestProbs = this.getnBestProbs();
        double[] CENbestProbs = CEFutFringe.getnBestProbs();
        if (thisNbestProbs[nBest - 1] < Double.NEGATIVE_INFINITY
                && thisNbestProbs[nBest - 1] > CENbestProbs[0])
        {
            setProb(thisNbestProbs.clone());
        } else if (CENbestProbs[nBest - 1] < Double.NEGATIVE_INFINITY
                && CENbestProbs[nBest - 1] > thisNbestProbs[0])
        {
            setProb(CENbestProbs.clone());
        } else
        {
            double[] mergedarray = new double[nBest];
            int a = 0;
            int b = 0;
            for (int i = 0; i < nBest; i++)
            {
                if (thisNbestProbs[a] > CENbestProbs[b])
                {
                    mergedarray[i] = thisNbestProbs[a];
                    a++;
                } else
                {
                    mergedarray[i] = CENbestProbs[b];
                    b++;
                }
            }
            setProb(mergedarray);
        }
    }
    
    public void mergeScoresProbs(FringeAndProb CEFutFringe, boolean pruneUsingScores)
    {
        Pair<double[], double[]> thisNbestScoresProbs = new Pair(getnBestScores(), getnBestProbs());
        Pair<double[], double[]> ceNbestScoresProbs = new Pair(CEFutFringe.getnBestScores(), CEFutFringe.getnBestProbs());
                
        if ((pruneUsingScores && thisNbestScoresProbs.getFirst()[nBest - 1] < Double.NEGATIVE_INFINITY
                && thisNbestScoresProbs.getFirst()[nBest - 1] > ceNbestScoresProbs.getFirst()[0]) ||
        (!pruneUsingScores && thisNbestScoresProbs.getSecond()[nBest - 1] < Double.NEGATIVE_INFINITY
                && thisNbestScoresProbs.getSecond()[nBest - 1] > ceNbestScoresProbs.getSecond()[0]))
        {            
            setScoresProbsClone(thisNbestScoresProbs);            
        } else if ((pruneUsingScores && ceNbestScoresProbs.getFirst()[nBest - 1] < Double.NEGATIVE_INFINITY
                && ceNbestScoresProbs.getFirst()[nBest - 1] > thisNbestScoresProbs.getFirst()[0]) ||
        (!pruneUsingScores && ceNbestScoresProbs.getSecond()[nBest - 1] < Double.NEGATIVE_INFINITY
                && ceNbestScoresProbs.getSecond()[nBest - 1] > thisNbestScoresProbs.getSecond()[0]))
        {
            setScoresProbsClone(ceNbestScoresProbs);
        } else
        {
            Pair<double[], double[]> mergedarray = newEmptyPairArray(nBest);            
            int a = 0;
            int b = 0;
            for (int i = 0; i < nBest; i++)
            {
                if ((pruneUsingScores && thisNbestScoresProbs.getFirst()[a] > ceNbestScoresProbs.getFirst()[b]) ||
                (!pruneUsingScores && thisNbestScoresProbs.getSecond()[a] > ceNbestScoresProbs.getSecond()[b]))
                {
                    mergedarray.getFirst()[i] = thisNbestScoresProbs.getFirst()[a];
                    mergedarray.getSecond()[i] = thisNbestScoresProbs.getSecond()[a];
                    a++;
                } else
                {
                    mergedarray.getFirst()[i] = ceNbestScoresProbs.getFirst()[b];
                    mergedarray.getSecond()[i] = ceNbestScoresProbs.getSecond()[b];
                    b++;
                }
            }
            setScoresProbs(mergedarray);
        }
    }

    public FringeAndProb clone()
    {
        return new FringeAndProb(fringePointer, nBestProbs.clone(), nBestScores.clone());
    }

    public void setNext(ArrayList<FringeAndProb> fap)
    {
        next = fap;
    }

    public void addNext(FringeAndProb fap)
    {
        next.add(fap);
    }

    public ArrayList<FringeAndProb> getNext()
    {
        return next;
    }

    public boolean hasNext()
    {
        return next != null;
    }

    public ArrayList<Fringe> getFringe()
    {
        return fringePointer;
    }

    public double getBestProb()
    {
        return this.nBestProbs[0];
    }

    public double[] getnBestProbs()
    {
        return this.nBestProbs;
    }

    public void setProb(double[] p)
    {
        this.nBestProbs = p;
    }

    public void setProb(double s, int position)
    {
        this.nBestProbs[position] = s;
    }
    
    public double getBestScore()
    {
        return nBestScores[0];
    }

    public double[] getnBestScores()
    {
        return nBestScores;
    }
      
    public void setScoresProbs(Pair<double[], double[]> scoresProbs)
    {
        nBestScores = scoresProbs.getFirst();
        nBestProbs = scoresProbs.getSecond();
    }
    
    public void setScoresProbsClone(Pair<double[], double[]> scoresProbs)
    {
        nBestScores = scoresProbs.getFirst().clone();
        nBestProbs = scoresProbs.getSecond().clone();
    }
    
    public ArrayList<FringeAndProb> getNextClone()
    {
        ArrayList<FringeAndProb> clone = new ArrayList<FringeAndProb>();
        if (next == null)
        {
            return null;
        }
        for (FringeAndProb f : next)
        {
            clone.add(new FringeAndProb(f.fringePointer, f.nBestProbs.clone(), f.getNextClone(), f.getBBHistClone()));
        }
        return clone;
    }

    public void addToBBHist(BuildBlock bb)
    {
        if (history == null)
        {
            history = new LinkedList<LinkedList<BuildBlock>>();
        }
        int index = bb.getWIndex();
        while (history.size() <= index)
        {
            LinkedList<BuildBlock> l = new LinkedList<BuildBlock>();
            history.add(l);
        }
        if (!history.get(index).contains(bb))
        {
            history.get(index).add(bb);
        }
        //	history.add(bb);
    }

    public LinkedList<LinkedList<BuildBlock>> getBBHist()
    {
        return this.history;
    }

    public void addAllCutOffLocs(LinkedList<LinkedList<BuildBlock>> bbHist)
    {
        if (this.history == null)
        {
            history = getCutOffLocsClone(bbHist);
            return;
        }
        int i = 0;
        while (history.size() < bbHist.size())
        {
            LinkedList<BuildBlock> nl = new LinkedList<BuildBlock>();
            history.add(nl);
        }
        //instead of adding, make new list! history = getCutOffLocsClone(history); doesn't work.
        for (LinkedList<BuildBlock> l : bbHist)
        {
            HashSet<BuildBlock> h1 = new HashSet<BuildBlock>();
            LinkedList<BuildBlock> li = history.get(i);
            h1.addAll(li);
            for (BuildBlock bi : l)
            {
                if (!h1.contains(bi))
                {
                    li.add(bi); //TODO
                }
            }
            i++;
        }

    }

    @SuppressWarnings("unchecked")
    private LinkedList<LinkedList<BuildBlock>> getCutOffLocsClone()
    {
        return getCutOffLocsClone(this.history);
    }

    @SuppressWarnings("unchecked")
    private LinkedList<LinkedList<BuildBlock>> getCutOffLocsClone(LinkedList<LinkedList<BuildBlock>> history)
    {
        if (history == null)
        {
            return null;
        }
        LinkedList<LinkedList<BuildBlock>> clone = new LinkedList<LinkedList<BuildBlock>>();
        for (LinkedList<BuildBlock> llist : history)
        {
            clone.add((LinkedList<BuildBlock>) llist.clone());
        }
        return clone;
    }

    public LinkedList<LinkedList<BuildBlock>> getCutOffLocations()
    {
        if (history == null)
        {
            return new LinkedList<LinkedList<BuildBlock>>();
        }
        return this.history;
    }

    public LinkedList<BuildBlock> getCutOffLocations(int i)
    {
        if (history == null)
        {
            return new LinkedList<BuildBlock>();
        }
        return this.history.get(i);
    }

    public void addCutOffLocation(BuildBlock b, int index)
    {
        if (history == null)
        {
            history = new LinkedList<LinkedList<BuildBlock>>();
        }
        //int index = b.getWIndex();
        while (history.size() <= index)
        {
            LinkedList<BuildBlock> l = new LinkedList<BuildBlock>();
            history.add(l);
        }
        if (!history.get(index).contains(b))
        {
            history.get(index).add(b);
        }
    }

    public LinkedList<LinkedList<BuildBlock>> getBBHistClone()
    {
        if (this.history == null)
        {
            return null;
        }
        LinkedList<LinkedList<BuildBlock>> result = new LinkedList<LinkedList<BuildBlock>>();
        for (LinkedList<BuildBlock> lb : this.history)
        {
            result.add((LinkedList<BuildBlock>) lb.clone());
        }
        return result;
    }

    @Override
    public String toString()
    {
        if (next != null)
        {
            return this.nBestProbs[0] + "\t" + this.fringePointer.toString() + "\t" + next.toString();
        } else
        {
            return this.nBestProbs[0] + "\t" + this.fringePointer.toString();
        }
    }    

}
