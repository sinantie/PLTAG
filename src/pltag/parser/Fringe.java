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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Fringe implements Serializable
{

    static final long serialVersionUID = -1L;
    private ArrayList<Node> adjunctionNodesOpenLeft = new ArrayList<Node>();
    private ArrayList<Node> adjunctionNodesOpenRight = new ArrayList<Node>();
    private Node substitutionNode = null;
    private int currentLeafNumber = -1;
    private String fringeString = null;
    //private LinkedList<AnalysisTracker> tracker;
//	private HashMap <BuildBlock, Double> last = new HashMap <BuildBlock, Double>();
//	private HashMap <BuildBlock, Double> current = new HashMap <BuildBlock, Double>();

//	private ArrayList<FringeAndProb> fringeContinuation = null;
//	private LinkedList<LinkedList<BuildBlock>> cutOffLocations;
    public Fringe()
    {
    }

    @SuppressWarnings("unchecked")
    public Fringe(ArrayList<Node> list, ArrayList<Node> list2, Node substitutionNode, int currentLeafNumber)
    {//, Fringe sourceForAdditionalInfo){
        //ArrayList<ArrayList<Fringe>> fringeContinuation, LinkedList<BuildBlock> cutOffLocs, 
        //HashMap<ChartEntry, Double> current, HashMap<ChartEntry, Double> last){
        this.adjunctionNodesOpenLeft = list2;
        this.adjunctionNodesOpenRight = list;
        if (substitutionNode != null)
        {
            this.substitutionNode = substitutionNode;
        }
        this.currentLeafNumber = currentLeafNumber;

        /*		this.fringeContinuation = getFringeContClone(sourceForAdditionalInfo.fringeContinuation);
         if (sourceForAdditionalInfo.getCutOffLocations() !=null)
         this.cutOffLocations = (LinkedList<LinkedList<BuildBlock>>) sourceForAdditionalInfo.getCutOffLocsClone();//cutOffLocs.clone();
         */	//this.current = (HashMap<ChartEntry, Double>) sourceForAdditionalInfo.current.clone();
        //this.last = sourceForAdditionalInfo.last;
    }
    /*
     @SuppressWarnings("unchecked")
     private LinkedList<LinkedList<BuildBlock>> getCutOffLocsClone() {
     if (this.cutOffLocations == null ) return null;
     LinkedList<LinkedList<BuildBlock>> clone = new LinkedList<LinkedList<BuildBlock>>();
     for (LinkedList<BuildBlock> llist : this.cutOffLocations){
     clone.add((LinkedList<BuildBlock>) llist.clone());
     }
     return clone;
     }

     @SuppressWarnings("unchecked")
     private LinkedList<LinkedList<BuildBlock>> cutOffLocsClone(LinkedList<LinkedList<BuildBlock>> l) {
     LinkedList<LinkedList<BuildBlock>> clone = new LinkedList<LinkedList<BuildBlock>>();
     for (LinkedList<BuildBlock> llist : l){
     clone.add((LinkedList<BuildBlock>) llist.clone());
     }
     return clone;
     }
     */
    /*	@SuppressWarnings("unchecked")
     private Fringe ( ArrayList<Node> list, ArrayList<Node> list2, Node substitutionNode, int currentLeafNumber, 
     ArrayList<FringeAndProb> fringeContinuation, LinkedList<LinkedList<BuildBlock>> cutOffLocs
     ){//, HashMap<BuildBlock, Double> current, HashMap<BuildBlock, Double> last){
     this.adjunctionNodesOpenLeft = list2;
     this.adjunctionNodesOpenRight = list;
     if (substitutionNode!=null) this.substitutionNode = substitutionNode;
     this.currentLeafNumber = currentLeafNumber;
		
     this.fringeContinuation = getFringeContClone(fringeContinuation);
     if (cutOffLocs !=null)
     this.cutOffLocations = cutOffLocsClone(cutOffLocs);
     //this.current = (HashMap<BuildBlock, Double>) current.clone();
     //this.last = last;
     }
     */

    public boolean equals(Fringe f)
    {
        if ((this.substitutionNode == null && f.substitutionNode == null)
                || (this.substitutionNode != null && f.substitutionNode != null
                && this.substitutionNode.identical(f.substitutionNode)))
        {
            if (this.adjunctionNodesOpenLeft.size() != f.adjunctionNodesOpenLeft.size()
                    && this.adjunctionNodesOpenRight.size() != f.adjunctionNodesOpenRight.size())
            {
                return false;
            }
            for (int i = 0; i < this.adjunctionNodesOpenLeft.size(); i++)
            {
                if (!this.adjunctionNodesOpenLeft.get(i).identical(f.adjunctionNodesOpenLeft.get(i)))
                {
                    return false;
                }
            }

            for (int i = 0; i < this.adjunctionNodesOpenRight.size(); i++)
            {
                if (!this.adjunctionNodesOpenRight.get(i).identical(f.adjunctionNodesOpenRight.get(i)))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public Fringe copy()
    {
        if (substitutionNode == null)
        {
            return new Fringe(adjcopy(adjunctionNodesOpenRight), adjcopy(adjunctionNodesOpenLeft), null, currentLeafNumber);//, 
            //getFringeContClone(fringeContinuation), cutOffLocations);//, current, last);
        }
        return new Fringe(adjcopy(adjunctionNodesOpenRight), adjcopy(adjunctionNodesOpenLeft), substitutionNode.copy(), currentLeafNumber);//, 
        //getFringeContClone(fringeContinuation), cutOffLocations);//, current, last);
    }
    /*
     @SuppressWarnings("unchecked")
     public ArrayList<FringeAndProb> getFringeContClone(ArrayList<FringeAndProb> fringeContinuation){
     if (fringeContinuation == null) return null;
     ArrayList<FringeAndProb> clone = new ArrayList<FringeAndProb>();
     for (FringeAndProb fap: fringeContinuation){
     clone.add(fap.clone());
     }		
     return clone;
     }
     */

    private ArrayList<Node> adjcopy(List<Node> adjNodeList)
    {
        ArrayList<Node> copy = new ArrayList<Node>();
        for (Node adj : adjNodeList)
        {
            copy.add(adj.copy());
        }
        copy.trimToSize();
        return copy;
    }

    public void setAdjNodesOpenLeft(ArrayList<Node> adjlist)
    {
        this.adjunctionNodesOpenLeft = adjlist;
        fringeString = null;
    }

    public void setAdjNodesOpenRight(ArrayList<Node> adjlist)
    {
        adjlist.trimToSize();
        this.adjunctionNodesOpenRight = adjlist;
        fringeString = null;
    }

    public void addAdjNodesOpenRight(ArrayList<Node> adjlist)
    {
        adjlist.trimToSize();
        adjunctionNodesOpenRight.addAll(adjlist);
        fringeString = null;
    }

    public void setSubstNode(Node sn)
    {
        substitutionNode = sn;
        fringeString = null;
    }

    public void setCurrentLeafNumber(int ln)
    {
        currentLeafNumber = ln;
    }

    public int getCurrentLeafNumber()
    {
        return currentLeafNumber;
    }

    public Node getSubstNode()
    {
        fringeString = null;
        return substitutionNode;
    }

    public ArrayList<Node> getAdjNodes()
    {
        if (adjunctionNodesOpenRight.isEmpty())
        {
            return adjunctionNodesOpenLeft;
        }
        if (adjunctionNodesOpenLeft.isEmpty())
        {
            return adjunctionNodesOpenRight;
        }
        ArrayList<Node> resultList = new ArrayList<Node>();
        resultList.addAll(adjunctionNodesOpenRight);
        resultList.addAll(adjunctionNodesOpenLeft);
        return resultList;
    }

    public ArrayList<Node> getAdjNodesOpenLeft()
    {
        fringeString = null;
        return adjunctionNodesOpenLeft;
    }

    public ArrayList<Node> getAdjNodesOpenRight()
    {
        fringeString = null;
        return adjunctionNodesOpenRight;
    }

    public ArrayList<Node> getAdjoinableNodes()
    {
        ArrayList<Node> resultList = new ArrayList<Node>();
        resultList.addAll(this.adjunctionNodesOpenRight);
        resultList.addAll(this.adjunctionNodesOpenLeft);
        if (substitutionNode == null)
        {
            return resultList;
        }
        resultList.add(substitutionNode);
        return resultList;
    }

    @Override
    public String toString()
    {
        return fringeString == null ? new StringBuilder()
                    .append(adjunctionNodesOpenRight.toString())
                    .append(adjunctionNodesOpenLeft.toString())
                    .append(" : ").append(substitutionNode).toString() : fringeString;
        
    }

    public ArrayList<Node> getRestOfAdjNodesOpenLeft(Node adjNode, boolean openToRightNeeded)
    {//, boolean targetShadow) {
        if (openToRightNeeded)
        {
            return this.adjunctionNodesOpenLeft;
        }
        ArrayList<Node> returnList = new ArrayList<Node>();
        int adjNodePos = this.adjunctionNodesOpenLeft.indexOf(adjNode);
        if (adjNodePos > -1)
        {
            returnList.addAll(this.adjunctionNodesOpenLeft.subList(adjNodePos, this.adjunctionNodesOpenLeft.size()));
        }
        return returnList;
    }

    //open to right nodes always come "before" open to left nodes.
    public ArrayList<Node> getRestOfAdjNodesOpenRight(Node adjNode, boolean openToRightNeeded)
    {//, boolean targetShadow) {
        if (!openToRightNeeded)
        {
            return new ArrayList<Node>();
        }
        ArrayList<Node> returnList = new ArrayList<Node>();
        returnList.addAll(this.adjunctionNodesOpenRight.subList(adjunctionNodesOpenRight.indexOf(adjNode), adjunctionNodesOpenRight.size()));
        return returnList;
    }

    public void replaceIndices(byte localShadowIndex, byte newIndex)
    {
        if (substitutionNode != null)
        {
            substitutionNode.replaceIndeces(localShadowIndex, newIndex);
        }
        for (Node an : adjunctionNodesOpenLeft)
        {
            an.replaceIndeces(localShadowIndex, newIndex);
        }
        for (Node an : adjunctionNodesOpenRight)
        {
            an.replaceIndeces(localShadowIndex, newIndex);
        }
        fringeString = null;
    }

    public boolean isEmpty()
    {
        return this.adjunctionNodesOpenLeft.isEmpty()
                && this.adjunctionNodesOpenRight.isEmpty()
                && this.substitutionNode == null;        
    }

    public void setTimeStamp(short t)
    {
        for (Node n : getAdjoinableNodes())
        {
            n.setTimeStamp(t);
        }
        fringeString = null;
    }

    public Node getLastAdjNode()
    {
        if (!adjunctionNodesOpenLeft.isEmpty())
        {
            return adjunctionNodesOpenLeft.get(adjunctionNodesOpenLeft.size() - 1);
        }
        if (!adjunctionNodesOpenRight.isEmpty())
        {
            return adjunctionNodesOpenRight.get(adjunctionNodesOpenRight.size() - 1);
        }
        return null;
    }

    public void saveSpace()
    {
        //	last.clear();
        //	current.clear();
//		fringeContinuation = null;
    }

    public String toCatString()
    {
        StringBuilder result = new StringBuilder("[");
        for (Node n : adjunctionNodesOpenRight)
        {
            result.append(n.getCategory()).append(" ");
        }
        result.append("][");
        for (Node n : adjunctionNodesOpenLeft)
        {
            result.append(n.getCategory()).append(" ");
        }
        if (substitutionNode != null)
        {
            result.append("]:").append(substitutionNode.getCategory());
        }
        else
        {
            result.append("]:null");
        }
        return result.toString();
    }

    public void setClusterNumberCount(boolean left)
    {
        ArrayList<Node> adjNodes;
        if (left)
        {
            if (this.substitutionNode != null)
            {
                adjNodes = new ArrayList<Node>();
                adjNodes.addAll(this.adjunctionNodesOpenLeft);
                adjNodes.add(this.substitutionNode);
            }
            else
            {
                adjNodes = this.adjunctionNodesOpenLeft;
            }

        }
        else
        {
            adjNodes = this.adjunctionNodesOpenRight;
        }
        HashMap<String, Integer> clusternumbermap = new HashMap<String, Integer>();
        HashMap<Node, String> nodesmap = new HashMap<Node, String>();
        String lastAdj = "null";
        for (Node n : adjNodes)
        {
            String cat = n.getCategory();
            Integer newnum;
            if (!clusternumbermap.containsKey(cat))
            {
                newnum = 1;
                clusternumbermap.put(cat, newnum);
                lastAdj = cat;
            }
            else if (!lastAdj.equals(cat))
            {
                newnum = clusternumbermap.get(cat) + 1;
                clusternumbermap.put(cat, newnum);
                lastAdj = cat;
            }
            else
            {
                newnum = clusternumbermap.get(cat);
            }
            nodesmap.put(n, newnum + "/");
        }
        for (Node n : adjNodes)
        {
            n.setClusterNumber(nodesmap.get(n) + clusternumbermap.get(n.getCategory()));
        }
    }

    public boolean hasTraceLeft()
    {
        if (this.getAdjNodesOpenRight().isEmpty())
        {
            return false;
        }
        return TreeState.isNullLexeme(this.getAdjNodesOpenRight().get(0).getCategory());
    }

    public boolean hasTraceRight()
    {
        if (adjunctionNodesOpenLeft.isEmpty())
        {
            return false;
        }
        return TreeState.isNullLexeme(adjunctionNodesOpenLeft.get(adjunctionNodesOpenLeft.size() - 1).getCategory());
    }
    
    public void shiftNodesBy(short offset)
    {
        for(Node n : adjunctionNodesOpenRight)
        {
            n.setNodeId((short) (n.getNodeId() + offset));
        }
        for(Node n : adjunctionNodesOpenLeft)
        {
            n.setNodeId((short) (n.getNodeId() + offset));
        }
        if(substitutionNode != null)
            substitutionNode.setNodeId((short) (substitutionNode.getNodeId() + offset));
    }
    
    public int size()
    {
        return adjunctionNodesOpenLeft.size() + adjunctionNodesOpenRight.size() + (substitutionNode == null ? 0 : 1);
    }
    
    public int numOfPredictNodes()
    {
        int count = 0;
        count = adjunctionNodesOpenLeft.stream().filter((node) -> (node.getDownIndex() > 0)).map((_item) -> 1).reduce(count, Integer::sum);
        count = adjunctionNodesOpenRight.stream().filter((node) -> (node.getDownIndex() > 0)).map((_item) -> 1).reduce(count, Integer::sum);
        if(substitutionNode != null && substitutionNode.getDownIndex() > 0)
            count++;
        return count;
    }
        
}
