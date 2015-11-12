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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PercolTable
{

    private Map<String, HashMap<String, Integer>> map = new HashMap<String, HashMap<String, Integer>>();
    private Map<String, Boolean> fromleft = new HashMap<String, Boolean>();

    public PercolTable(String filename)
    {
        readPT(filename);
        //printPt();
    }

    public void printPt()
    {
        System.out.println(map.keySet());
    }

    private void readPT(String filename2)
    {
//		declared here only to make visible to finally clause
        BufferedReader input = null;
        try
        {//use buffering, reading one line at a time
            //FileReader always assumes default encoding is OK!
            input = new BufferedReader(new FileReader(filename2));
            String line = null; //not declared within while loop
            while ((line = input.readLine()) != null)
            {//TODO
                String[] list = line.split("(\t| )");
                //ArrayList<String> array = new ArrayList<String>();
                HashMap<String, Integer> magermanMap = new HashMap<String, Integer>();
                Boolean direction_fromleft = false;
                if (list[2].equals("1"))
                {
                    direction_fromleft = true;
                }

                String key = list[1];
                for (int i = 3; i < list.length; i++)
                {
                    //array.add(list[i]);
                    magermanMap.put(list[i], i);
                }
                magermanMap.put("", 98);
                fromleft.put("", true);

                map.put(key, magermanMap);
                fromleft.put(key, direction_fromleft);
            }
            map.put("", new HashMap<String, Integer>());
        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                if (input != null)
                {
                    input.close();
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public TagNode setHeadsInTree(TagNode node)
    {
        List children = node.getChildlist();
        findHeadCategory(node);
        //go on recursively:
        Iterator ci = children.iterator();
        while (ci.hasNext())
        {
            TagNode child = (TagNode) ci.next();
            if (!child.isLeaf())
            {
                setHeadsInTree(child);
            }
        }
        return node;
    }

    public void findHeadCategory(TagNode node)
    {
        String category = node.getCategory();
        List<TagNode> children = node.getChildlist();
        int bestindex = -1;
        int best = 99;
        HashMap targetlist = (HashMap) map.get(category);
        //find head category:

        if (!fromleft.containsKey(category))
        {
            if (category.equals("NP") || category.equals("NML") || category.equals("NX"))
            {
                bestindex = npProcessing(children);
            }
        }
        else
        {
            if (((Boolean) fromleft.get(category)).booleanValue())
            {
                for (int i = 0; i < children.size(); i++)
                {
                    String z = (String) ((TagNode) children.get(i)).getCategory();
                    int currentval = 99;
                    if (targetlist.containsKey(z))
                    {
                        currentval = ((Integer) targetlist.get(z)).intValue();
                    }
                    if (currentval < best)
                    {
                        bestindex = i;
                        best = currentval;
                    }
                }
                if (bestindex == -1)
                {
                    bestindex = getBestHeadLeft(children);
                }
            }
            else
            {
                for (int i = children.size() - 1; i >= 0; i--)
                {
                    TagNode child = ((TagNode) children.get(i));
                    String z = child.getCategory();
                    int currentval = 99;
                    if (category.startsWith("S") && !category.startsWith("SBAR") && child.getFullCategory().contains("-PRD"))
                    {
                        currentval = ((Integer) targetlist.get("VP")).intValue();
                    }
                    else if (targetlist.containsKey(z))
                    {
                        currentval = ((Integer) targetlist.get(z)).intValue();
                    }
                    if (currentval < best)
                    {
                        bestindex = i;
                        best = currentval;
                    }
                }
                //if (bestindex == -1){bestindex =children.size()-1; }
                if (bestindex == -1)
                {
                    bestindex = getBestHeadRight(children);
                }
            }
        }
        if (bestindex == -1)
        {
            bestindex = getBestHeadLeft(children);
        }
        ((TagNode) children.get(bestindex)).makeHead();
        node.setHeadChild(((TagNode) children.get(bestindex)));
    }

    private int getBestHeadRight(List<TagNode> children)
    {
        int bestindex = children.size() - 1;
        while (bestindex > 0 && children.get(bestindex).getCategory().matches("[,.?:]|('')|(``)"))
        {
            bestindex--;
        }
        return bestindex;
    }

    private int getBestHeadLeft(List<TagNode> children)
    {
        int bestindex = 0;
        // < children.size-1, because if we're still going at that point, it means that we want to chose the last remaining child never
        // theless.
        while (bestindex < children.size() - 1 && children.get(bestindex).getCategory().matches("[,.?:]|('')|(``)"))
        {
            bestindex++;
        }
        return bestindex;
    }

    private int npProcessing(List children)
    {
        int lastIndex = children.size() - 1;
        //if the last word is tagged POS return lastword;
        if (((TagNode) children.get(lastIndex)).getCategory().equals("POS"))
        {
            return lastIndex;
        }
        // else search from right to left for the first child which is an NN, NNP, NNPS, NNS, NX, POS, JJR
        HashMap<String, Integer> nplist = new HashMap<String, Integer>();
        nplist.put("NN", 1);
        nplist.put("NNP", 1);
        nplist.put("NNPS", 1);
        nplist.put("NNS", 1);
        nplist.put("NNX", 1); // nplist.put("NP", 1);nplist.put("NML", 1); //nplist.put("NX", 1);
        nplist.put("POS", 1);
        nplist.put("JJR", 1);
        nplist.put("JJS", 1);
        nplist.put("VBG", 1);
        boolean marker = false;
        for (int i = lastIndex; i >= 0; i--)
        {
            String cat = ((String) ((TagNode) children.get(i)).getCategory());
            if (nplist.containsKey(cat))
            {
                if (marker)
                {
                    System.out.println("@@" + children);
                }
                return i;
            }
            else if (cat.equals("NP") || cat.equals("NML") || cat.equals("NAC"))
            {
                marker = true;
            }
        }
//		else if it's a coordinated noun phrase, search from right to left for the first child which is an NP
        int lastNP = lastIndex + 1;
        for (int i = lastIndex; i >= 0; i--)
        {
            String cat = ((String) ((TagNode) children.get(i)).getCategory());
            if (cat.equals("NP") || cat.equals("NML"))
            {//||((String)((TagNode)children.get(i)).getCategory()).equals("NX")){
                if (lastNP == lastIndex + 1)
                {
                    lastNP = i;
                }

            }
            if (((String) ((TagNode) children.get(i)).getCategory()).matches("C(C|ONJP)") && lastNP != lastIndex + 1)
            {
                return lastNP;
            }
        }//*/
        //else search from left to right for the first child which is an NP
        for (int i = 0; i <= lastIndex; i++)
        {
            if (((String) ((TagNode) children.get(i)).getCategory()).equals("NP") || ((String) ((TagNode) children.get(i)).getCategory()).equals("NML")
                    || ((String) ((TagNode) children.get(i)).getCategory()).equals("NX"))
            {
                return i;
            }
        }
        //else search from right to left for the first child which is a $, ADJP, PRN
        nplist.clear();
        nplist.put("$", 1);
        nplist.put("ADJP", 1);
        nplist.put("PRN", 1);
        for (int i = lastIndex; i >= 0; i--)
        {
            if (nplist.containsKey(((String) ((TagNode) children.get(i)).getCategory())))
            {
                return i;
            }
        }
        //else search from right to left for the first child which is a CD
        for (int i = lastIndex; i >= 0; i--)
        {
            if (((String) ((TagNode) children.get(i)).getCategory()).equals("CD"))
            {
                return i;
            }
        }
        //else search from right to left for the first child which is a JJ, JJS, RB, or QP, or FW
        nplist.clear();
        nplist.put("JJ", 1);
        nplist.put("JJS", 1);
        nplist.put("RB", 1);
        nplist.put("QP", 1);
        nplist.put("FW", 1);// nplist.put("S", 1);
        for (int i = lastIndex; i >= 0; i--)
        {
            if (nplist.containsKey(((String) ((TagNode) children.get(i)).getCategory())))
            {
                return i;
            }
        }
        //else return the last word	
        return getBestHeadRight(children);
    }
}
