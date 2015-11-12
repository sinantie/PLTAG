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
package pltag.corpus;

import fig.basic.LogInfo;
import java.util.ArrayList;
import java.util.List;

public class ErrorTracker
{

    List<String> summary;
    boolean verbose;
    int errorsSum = 0;
    int wrongOrderSum = 0;
    int successfulTreeSum = 0;

    /**
     * Constructor
     * @param verbose
     */
    public ErrorTracker(boolean verbose)
    {
        this.verbose = verbose;
        summary = new ArrayList<String>();
    }

    /**
     * Counts the errors in a given corpus.
     * @param corpus
     * @param listErrors, int k, int j 
     */
    public void count(List<StringTree> corpus, List<String> listErrors, int k, int j)
    {
        int successfulTree = 0;
        int treesWithWrongLeafOrder = 0;
        for (StringTree tagTree : corpus)
        {
            if (tagTree != null)
            {
                if (tagTree.getTreeName().contains("Unconnected:"))
                {
                    //listErrors.add(tagtree.getName());
                    if (verbose)
                    {
                        LogInfo.logs(tagTree.getTreeName());
                    }
                }
                else if (tagTree.correctOrder(tagTree.getRoot(), -1) != 0)
                {
                    if (verbose)                    
                    {
                        LogInfo.logs("wo: " + tagTree.correctOrder(tagTree.getRoot(), -1) + tagTree.print());
                    }                    
                    treesWithWrongLeafOrder++;
//                    LogInfo.error(tagTree.print() + tagTree.correctOrder(tagTree.getRoot(), -1));
                }
                else
                {
                    successfulTree++;
                }
            }
        }
        for (String error : listErrors)
        {
            summary.add(error);
        }

        summary.add("===========\nNumber of wrong trees (track = " + k + "; processed " + (j+1)*10 + " docs): " + listErrors.size());
        errorsSum += listErrors.size();
        summary.add("Number of trees with leaves in wrong order (track = " + k + "; processed " + (j+1)*10 + " docs): " + treesWithWrongLeafOrder);
        wrongOrderSum += treesWithWrongLeafOrder;
        summary.add("Number of successfully converted trees(track = " + k + "; processed " + (j+1)*10 + " docs): " + successfulTree + "\n===========\n");
        successfulTreeSum += successfulTree;
        //System.out.println("TAG corpus fertig!");
    }

    /**
     * Prints out the information stored in "summary" so far, and clears it.
     *
     */
    public void printResult()
    {
        for (String s : summary)
        {
            LogInfo.logs(s);
        }
        summary.clear();
    }

    /**
     * Prints the overall summary.
     * @param trackNumber
     */
    public String printSummary(int trackNumber)
    {
        StringBuilder str = new StringBuilder("\n\n\n===========\n");
        str.append(String.format("Number of wrong trees (track = %s): %s\n", trackNumber, errorsSum));
        str.append(String.format("Number of trees with leaves in wrong order (track = %s): %s\n", trackNumber, wrongOrderSum));
        str.append(String.format("Number of successfully converted trees(track = %s): %s\n", trackNumber, successfulTreeSum));
        float rate = (float) (errorsSum * 1.0 / (successfulTreeSum * 1.0));
        str.append(String.format("Percentage of incorrect ones: %s\n", rate));
        return str.toString();
    }

    /**
     * Checks whether the number of leaves in the converted tree corresponds to the number of trees in the
     * original tree.
     * 
     * @param sentenceTree
     * @param tree
     * @param fileno
     * @return
     */
    public List<String> leafCheck(CompositeStringTree sentenceTree, PennTree tree, String fileno)
    {
        List<String> listErrors = new ArrayList<String>();
        try{
        if (sentenceTree.countLeaves(sentenceTree.getRoot()) != tree.getLeaflist().size())
        {
            listErrors.add("Number of Leaves Wrong " + fileno + ": " + sentenceTree.getTreeName());
        }
        }catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return listErrors;
    }
}
