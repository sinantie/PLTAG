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
import fig.basic.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.util.Utils;


//import parser.StringTreeAnalysis;
public class StringTree
{

    protected String[] fullcategories;
    protected String[] categories;
    protected String[] roles;
    protected TagNodeType[] nodeTypes;
    //protected Boolean[] adjPossible;
    protected Integer[] parent;
    protected Boolean[] holes;
    protected Boolean[] isHeadChild;
    protected HashMap<Integer, ArrayList<Integer>> children;
    protected MultiValueMap<Integer, Integer> origin;
    protected MultiValueMap<Integer, Integer> originUp;
    protected MultiValueMap<Integer, Integer> originDown;
    protected byte[] indexUp;
    protected byte[] indexDown;
    protected Integer root;
    protected Integer foot;
    protected boolean auxtree;
    protected String treeid;
    protected String treeString;
    protected List<Integer> coordAnchorList;
    protected int coordanchor;
    protected double probability = 0.0;
    protected int arraysize = 12;
    protected List<Integer> rootToFootPath;
    protected boolean useSemantics;
    
    public StringTree(boolean useSemantics)
    {
        root = Integer.MIN_VALUE;   // empty value
        foot = Integer.MIN_VALUE;   // empty value
        coordanchor = Integer.MIN_VALUE;    // empty value
        children = new HashMap<Integer, ArrayList<Integer>>();
        coordAnchorList = new ArrayList<Integer>();
        fullcategories = new String[arraysize];
        categories = new String[arraysize];
        roles = new String[arraysize];
        nodeTypes = new TagNodeType[arraysize];
        parent = new Integer[arraysize];
        holes = new Boolean[arraysize];
        //this.adjPossible = new Boolean[arraysize];
        isHeadChild = new Boolean[arraysize];
        indexUp = new byte[arraysize];
        Arrays.fill(indexUp, (byte) -1);
        indexDown = new byte[arraysize];
        Arrays.fill(indexDown, (byte) -1);
        this.useSemantics = useSemantics;
    }

    protected void initializeOrigin()
    {
        origin = new MultiValueMap<Integer, Integer>();
        originUp = new MultiValueMap<Integer, Integer>();
        originDown = new MultiValueMap<Integer, Integer>();
    }

    public void makeArraysBigger(int leafNodeID)
    {
        if (leafNodeID < arraysize)
        {
            return;
        }
        while (leafNodeID >= arraysize)
        {
            arraysize = arraysize * 2;
        }

        String[] newfullcat = new String[arraysize];
        for (int i = 0; i < fullcategories.length; i++)
        {
            newfullcat[i] = fullcategories[i];
        }
        fullcategories = newfullcat;//Arrays.copyOf(fullcategories, arraysize);

        String[] newarray = new String[arraysize];
        for (int i = 0; i < categories.length; i++)
        {
            newarray[i] = categories[i];
        }
        categories = newarray;

        String[] newarrayRoles = new String[arraysize];
        for (int i = 0; i < roles.length; i++)
        {
            newarrayRoles[i] = roles[i];
        }
        roles = newarrayRoles;


        TagNodeType[] newarray2 = new TagNodeType[arraysize];
        for (int i = 0; i < nodeTypes.length; i++)
        {
            newarray2[i] = nodeTypes[i];
        }
        nodeTypes = newarray2;

        Integer[] newarray3 = new Integer[arraysize];
        for (int i = 0; i < parent.length; i++)
        {
            newarray3[i] = parent[i];
        }
        parent = newarray3;

        byte[] newarray6 = new byte[arraysize];
        Arrays.fill(newarray6, (byte) -1);
        for (int i = 0; i < indexUp.length; i++)
        {
            newarray6[i] = indexUp[i];
        }
        indexUp = newarray6;
        byte[] newarray7 = new byte[arraysize];
        Arrays.fill(newarray7, (byte) -1);
        for (int i = 0; i < indexDown.length; i++)
        {
            newarray7[i] = indexDown[i];
        }
        indexDown = newarray7;

        Boolean[] newarray4 = new Boolean[arraysize];
        for (int i = 0; i < holes.length; i++)
        {
            newarray4[i] = holes[i];
        }
        holes = newarray4;

        /*Boolean[] newarray4b = new Boolean[arraysize];
         for (int i =0; i<adjPossible.length; i++){
         newarray4b[i]= adjPossible[i];
         }	
         adjPossible = newarray4b;
         */

        Boolean[] newarray5 = new Boolean[arraysize];
        for (int i = 0; i < isHeadChild.length; i++)
        {
            newarray5[i] = isHeadChild[i];
        }
        isHeadChild = newarray5;
    }

    public double getProbability()
    {
        return probability;
    }

    public void setAuxTree()
    {
        auxtree = true;
    }

    public int getRoot()
    {
        return root;
    }

    public String getShortName(String nodeIDstring)
    {
        int nodeID = Integer.parseInt(nodeIDstring);
        if (nodeTypes[nodeID] == TagNodeType.anchor || nodeTypes[nodeID] == TagNodeType.terminal)
        {
            return new StringBuilder().append(categories[nodeID]).append(nodeTypes[nodeID].getMarker()).toString();
        }
        return new StringBuilder().append(categories[nodeID]).append(":").append(nodeID) //":"+getLowestOrigin(nodeID)+ 
                .append(nodeTypes[nodeID].getMarker()).toString();
    }

    public String getName(int nodeID)
    {
        //int nodeID = Integer.parseInt(nodeID);
        if (nodeTypes[nodeID] == TagNodeType.anchor || nodeTypes[nodeID] == TagNodeType.terminal)
        {
            return new StringBuilder().append(fullcategories[nodeID]).append(nodeTypes[nodeID].getMarker()).toString();
        }
        if (nodeTypes[nodeID] == null)
        {
            LogInfo.error("nodeType[" + nodeID + "] not available");
        }

        return new StringBuilder().append(fullcategories[nodeID]).append(":").append(nodeID).append(":").append("^").append(getOrigin(nodeID, originUp)).append("_").append(getOrigin(nodeID, originDown)).append(nodeTypes[nodeID].getMarker()).toString();
        //fullcategories[nodeID] + ":" + nodeID + ":"+"^"+getOrigin(nodeIDstring, originUp)+"_"
        //+getOrigin(nodeIDstring, originDown)+ nodeTypes[nodeID].getMarker();
    }

    public String getLexName(int nodeID, boolean printSemantics) //TODO: FIX!
    {
        //int nodeID = Integer.parseInt(nodeID);
        if (nodeTypes[nodeID] == TagNodeType.anchor || nodeTypes[nodeID] == TagNodeType.terminal)
        {
            if (nodeTypes[nodeID] == TagNodeType.terminal)
            {
                if (fullcategories[nodeID].contains("*"))
                {

                    String cat = categories[nodeID];
                    int index = cat.indexOf("*-");
                    if (index > -1)
                    {
                        cat = cat.substring(0, index + 1);
                    }
                    else
                    {
                        index = cat.indexOf("=");
                        if (index > -1)
                        {
                            cat = cat.substring(0, index);
                        }
                    }
                    StringBuilder newstring = new StringBuilder().append(cat).append("^").append(printOriginUpChar(nodeID)).append("_").append(printOriginDownChar(nodeID)).append(nodeTypes[nodeID].getMarker());
//                    StringBuilder newstring = new StringBuilder().append(cat).append("^").append(printOriginUp(nodeID)).append("_").append(printOriginDown(nodeID)).append(nodeTypes[nodeID].getMarker());
//                    newstring.append(index);
                    return newstring.toString();                    
                }
            }
            else
            {
                return new StringBuilder().append(fullcategories[nodeID]).append(nodeTypes[nodeID].getMarker()).toString();
            }            
        }
//        if (nodeTypes[nodeID] == TagNodeType.predicted)
//        {
//        }
        if (nodeTypes[nodeID] == null && categories[nodeID] == null)
        {
//            if (categories[nodeID] == null)
//            {
                return "";
//            }
        }

        StringBuilder ns = new StringBuilder().append(categories[nodeID]);
//        if (printSemantics && fullcategories[nodeID].contains("@"))
//        {
//            ns.append(fullcategories[nodeID].substring(fullcategories[nodeID].indexOf("@")));
//        }
        if (printSemantics && roles[nodeID] != null)
        {
            ns.append(roles[nodeID]);
        }
        ns.append("^").append(printOriginUpChar(nodeID)).append("_").append(printOriginDownChar(nodeID)).append(nodeTypes[nodeID].getMarker());
//        ns.append("^").append(printOriginUp(nodeID)).append("_").append(printOriginDown(nodeID)).append(nodeTypes[nodeID].getMarker());
        return ns.toString();
    }

    public String getLexNameWithHeadInfo(int nodeID)
    {
        //int nodeID = Integer.parseInt(nodeID);
        char headMarker = '+';
        if (!isHeadChild[nodeID])
        {
            headMarker = '-';
        }
        if (nodeTypes[nodeID] == TagNodeType.anchor || nodeTypes[nodeID] == TagNodeType.terminal)
        {
            if (nodeTypes[nodeID] != TagNodeType.terminal)
            {
                return new StringBuilder().append(fullcategories[nodeID]).append(headMarker).append(nodeTypes[nodeID].getMarker()).toString();
            }
        }
        if (nodeTypes[nodeID] == null)
        {
            //System.out.println("nodeTypes"+ nodeID+"doesn't exist.");
            if (categories[nodeID] == null)
            {
                return "";
            }
        }
        return new StringBuilder().append(categories[nodeID]).append(headMarker).append("^").append(printOriginUpChar(nodeID)).append("_").append(printOriginDownChar(nodeID)).append(nodeTypes[nodeID].getMarker()).toString();
//        return new StringBuilder().append(categories[nodeID]).append(headMarker).append("^").append(printOriginUp(nodeID)).append("_").append(printOriginDown(nodeID)).append(nodeTypes[nodeID].getMarker()).toString();
        //categories[nodeID] +"^"+printOriginUp(nodeIDstring)+"_"+printOriginDown(nodeIDstring)+ nodeTypes[nodeID].getMarker();
        //fullcategories[nodeID] +"^"+printOriginUp(nodeIDstring)+"_"+printOriginDown(nodeIDstring)+ // ":"+getLowestOrigin(nodeID)+ 
        //nodeTypes[nodeID].getMarker();
    }

    protected String printOrigin(Integer nodeID, MultiValueMap<Integer, Integer> origin)
    {
        if (containsOriginNode(nodeID, origin))
//        if (getOrigin(nodeID, origin) != null)
        {
            return "x";
        }
        return null;
    }

    protected String printOriginUpChar(int nid)
    {
        byte index = indexUp[nid];
        if(index != -1)
            return index < 10 ? String.valueOf((char) ('0' + index)) : Byte.toString(indexUp[nid]);
        if (containsOriginNode(nid, originUp))
        {
            return "x";
        }
        return "null";
    }

    protected String printOriginDownChar(int nid)
    {
        Byte index = indexDown[nid];
        if(index != -1)
            return index < 10 ? String.valueOf((char) ('0' + index)) : Byte.toString(indexUp[nid]);
        if (containsOriginNode(nid, originDown))
//        if (getOrigin(nid, originDown) != null)
        {
            return "x";
        }
        return "null";
    }

    protected String printOriginUp(int nid)
    {
        //int nid = Integer.parseInt(nid);
        if (indexUp[nid] != -1)
        {
            return Byte.toString(indexUp[nid]);
        }
        if (containsOriginNode(nid, originUp))
//        if (getOrigin(nid, originUp) != null)
        {
            return "x";
        }
        return null;
    }

    protected String printOriginDown(int nid)
    {
        //int nid = Integer.parseInt(nid);
        if (indexDown[nid] != -1)
        {
            return Byte.toString(indexDown[nid]);
        }
        if (containsOriginNode(nid, originDown))
//        if (getOrigin(nid, originDown) != null)
        {
            return "x";
        }
        return null;
    }

    public ArrayList<Integer> getChildren(Integer nodeID)
    {
        ArrayList<Integer> out = children.get(nodeID);
        return out == null ? new ArrayList<Integer>() : out;
    }

    public TagNodeType getNodeType(int nodeID)
    {
        return nodeTypes[nodeID];
//        return nodeTypes[Integer.parseInt(nodeID)];
    }

    public String getNodeTypeName(int nodeID)
    {
        return getNodeType(nodeID).toString();
    }

    public String getTreeName()
    {
        return treeid + treeString;
    }

    public boolean isAuxtree()
    {
        return auxtree;
    }

    public int getFoot()
    {
        return foot;
    }

    public String print()
    {
        StringBuilder out = new StringBuilder().append(treeString);        
        if (useSemantics && this instanceof ElementaryStringTree)
        {
            out.append(((ElementaryStringTree) this).getSemanticFrame());
        }
        out.append("\t");
        if (auxtree)
        {
            out.append("MOD \t");
        }
        else
        {
            out.append("ARG \t");
        }
        out.append(getStructure(root, useSemantics));

        return out.toString();
    }

    public String toString()
    {

        StringBuilder p = new StringBuilder();
        if (this.probability != 0.0)
        {
            p.append(this.probability);
        }
        return p.append("\t").append(print()).toString();
    }

    public String goldStandardToString()
    {
        return new StringBuilder().append(treeString.trim()).append("\n").append(getPOStagged(root).trim()).append("\n").append(getStructure(root, false)).toString();
    }

    public String getPOStagged(int nodeID)
    {
        //int nodeID = Integer.parseInt(nodeID);
        String struct = "";
        ArrayList<Integer> childlist = children.get(nodeID);
        if ((childlist == null || childlist.isEmpty()))
        {
            if (nodeTypes[nodeID] == TagNodeType.anchor //&& categories[nodeID].equals("0"))
                    || (nodeTypes[nodeID] == TagNodeType.terminal && !(categories[nodeID].contains("*"))))
            {
                if (categories[nodeID].equals("0"))
                {
                    return "";
                }
                else
                {
                    String cat = categories[parent[nodeID]];
                    return new StringBuilder().append(cat).append(" ").append(fullcategories[nodeID]).append("\t").toString();
                }
                //fullcategories[Integer.parseInt(parent[nodeID])] + " " + fullcategories[nodeID] + "\t";
            }
            else if (nodeTypes[nodeID] == TagNodeType.internal)
            {
                String cat = categories[parent[nodeID]];
                return new StringBuilder().append(cat).append(" ").append(categories[nodeID]).append("\t").toString();
                //return fullcategories[Integer.parseInt(parent[nodeID])] + " " + fullcategories[nodeID] + "\t";
            }
            else
            {
                return "";
            }
        }

        for (Integer childid : childlist)
        {
            struct += getPOStagged(childid);
        }

        return struct;
    }

    /**
     * Prints the structure of a string tree in bracketed notation
     *
     * @param nodeID
     * @return
     */
    public String getStructureWithHeadInfo(int nodeID)
    {
        StringBuilder struct = new StringBuilder();
        ArrayList<Integer> childlist = children.get(nodeID);
        if (childlist == null || childlist.isEmpty())
        {
            return getLexNameWithHeadInfo(nodeID);
        }
        StringBuilder childString = new StringBuilder();
        for (Integer childid : childlist)
        {
            childString.append(" ").append(getStructureWithHeadInfo(childid));
        }
        struct.append("( ").append(getLexNameWithHeadInfo(nodeID)).append(childString.toString()).append(")");
        return struct.toString();
    }

    /**
     * Prints the structure of a string tree in bracketed notation
     *
     * @param nodeID
     * @param printSemantics
     * @return
     */
    public String getStructure(int nodeID, boolean printSemantics)
    {
        StringBuilder struct = new StringBuilder();
        ArrayList<Integer> childlist = children.get(nodeID);
        if (childlist == null || childlist.isEmpty())
        {
            return getLexName(nodeID, printSemantics);
            //return nodeID + ":" +getLexName(nodeID);
        }
        StringBuilder childString = new StringBuilder();
        for (Integer childid : childlist)
        {
            //System.err.print(nodeID + ":" + childString + " " + childid + "\n");
            childString.append(" ").append(getStructure(childid, printSemantics));
        }
        String structString;
        if (!fullcategories[nodeID].equals(""))
//        if (!fullcategories[Integer.parseInt(nodeID)].equals(""))
        {
            struct.append("( ").append(getLexName(nodeID, printSemantics)).append(childString.toString()).append(")");
            structString = struct.toString();
        }
        else
        {
            int upind = childString.indexOf("^");
            int downind = childString.indexOf("_");
            structString = childString.subSequence(0, upind + 1) + "null" + childString.substring(downind);
            structString = structString.trim();
        }
        //struct = "( "+nodeID + ":" + getLexName(nodeID)+childString+")";
        return structString;
    }

    public int getComplexity()
    {
       return 0;
    }
    
    /**
     * Calls the method that changes tree excepts into lexicon entries after
     * cutting off unneeded structure in coordinated entries.
     *
     */
    @SuppressWarnings("unchecked")
    public void makeLexiconEntry()
    {
        // mark multientry second bits as predicted!
        if (originDown != null)
        {            
            Set<Integer> origins = new HashSet<Integer>();
            for(Object val : originDown.values())
            {                
                origins.add((Integer)val);             

            }
//            Iterator it = originDown.values().iterator();
//            ArrayList<String> origins = new ArrayList<String>();
//            while (it.hasNext())
//            {
//                String val = (String) it.next();
//                if (!origins.contains(val))
//                {
//                    origins.add(val);
//                }
//
//            }
            if (origins.size() > 1 && this.getClass() != pltag.corpus.PredictionStringTree.class)
            {
                // could be traces or multientry
                int index = 1;
                ArrayList<Integer> lexemes = getLex();
                for (Integer lexleaf : lexemes)
                {
                    if (nodeTypes[lexleaf] == TagNodeType.terminal && !(categories[lexleaf].contains("*")
                            || categories[lexleaf].equals("0")))
                    {
                        this.markAsPrediction(lexleaf, index);
                        index++;
                    }
                }

            }
        }
    }

    private void markAsPrediction(Integer lexleaf, int index)
    {
        Integer origin = this.originDown.getCollection(lexleaf).iterator().next();
//        String origin = (String) this.originDown.getCollection(Integer.toString(lexleaf)).iterator().next();
        for (Integer node : getNodes())
        {
            //String nodeString = Integer.toString(node);
            if (getOrigin(node, originDown) != null && getOrigin(node, originDown).contains(origin))
            {
                this.indexDown[node] = (byte) index;
            }
            if (getOrigin(node, originUp) != null && getOrigin(node, originUp).contains(origin))
            {
                this.indexUp[node] = (byte) index;
            }
        }

    }

    private ArrayList<Integer> getLex()
    {
        ArrayList<Integer> lexleaves = new ArrayList<Integer>();
        for (int i = 0; i < categories.length; i++)
        {
            if (nodeTypes[i] == TagNodeType.anchor || nodeTypes[i] == TagNodeType.terminal)
            {
                lexleaves.add(i);
            }
        }
        return lexleaves;
    }

    /**
     * Creates a lexicon entry from the tree except (removing unary nodes which
     * may have been introduced through modification.
     *
     */
    public HashMap<Integer, Integer> removeUnaryNodes(int nodeid)
    {
        HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>();
        //int nodeid = Integer.parseInt(nodeid);
        String cat = categories[nodeid];
        String matchcat = cat;
        if (cat.contains("-"))
        {
            matchcat = cat.substring(0, cat.indexOf("-"));
        }
        ArrayList<Integer> childlist = children.get(nodeid);
        if (childlist == null)
        {
            return mapping;
        }
        else if (childlist.size() == 1)
        {
            int child = childlist.get(0);
            //int child = Integer.parseInt(child);
            if (categories[child].equals(matchcat)
                    && ((originUp != null && originDown != null
                    && this.getLowestOrigin(child, originUp).equals(this.getLowestOrigin(child, originDown)))
                    || (this.getLowerIndex(child) != -1 && this.getUpperIndex(child) != -1 && this.getLowerIndex(child) == this.getUpperIndex(child)))
                    && (nodeTypes[child] == TagNodeType.internal || nodeTypes[child] == TagNodeType.predicted))
            {
                children.put(nodeid, children.get(child));
//                children.put(Integer.toString(nodeid), children.get(child));
                mapping.put(nodeid, child);
                mapping.put(child, nodeid);
                if (children.get(child) != null)
                {
                    for (Integer newchild : children.get(child))
                    {
                        removeNode(child);
                        parent[newchild] = nodeid;
//                        parent[Integer.parseInt(newchild)] = Integer.toString(nodeid);
                        mapping.putAll(removeUnaryNodes(nodeid));
//                        mapping.putAll(removeUnaryNodes(Integer.toString(nodeid)));
                    }
                }
                return mapping;
            }
        }
        for (Integer child : childlist)
        {
            mapping.putAll(removeUnaryNodes(child));
        }
        return mapping;
    }

    protected void removeNode(Integer node)
    {
        //String nodeString = Integer.toString(node);
        if (children.containsKey(node) && children.get(node) != null)
        {
            for (int c : children.get(node))
            {
                parent[c] = parent[node];
//                parent[Integer.parseInt(c)] = parent[node];
            }
        }
        children.remove(node);
        if (node >= this.arraysize)
        {
            return;
        }
        categories[node] = null;
        fullcategories[node] = null;
        nodeTypes[node] = null;
        holes[node] = null;
        parent[node] = null;
        if (origin != null)
        {
            origin.remove(node);
        }
        if (originUp != null)
        {
            originUp.remove(node);
        }
        if (originDown != null)
        {
            originDown.remove(node);
        }
        //adjPossible[node]=null;
    }

    public int correctOrder(int nodeID, int leafnumber)
    {
        ArrayList<Integer> leaflist = getLeaves(nodeID, new ArrayList<Integer>());
        int last = -1;
        //System.out.print(leaflist);
        for (Integer lid : leaflist)
        {
            //int ln = Integer.parseInt(lid);
            if (last >= lid)
            {                
                //System.out.print(leaflist);
                return lid;
            }
            last = lid;
        }
        //System.out.println ("");
        return 0;
    }

    public ArrayList<Integer> getLeaves(int nodeID, ArrayList<Integer> leaflist)
    {
        //int nodeID = Integer.parseInt(nodeID);
        ArrayList<Integer> childlist = children.get(nodeID);
        if (childlist == null)
        {
            if (nodeTypes[nodeID] == TagNodeType.terminal || nodeTypes[nodeID] == TagNodeType.anchor)
            {
                leaflist.add(getLowestOrigin(nodeID, origin));
            }
            else
            {                
                LogInfo.error(getTreeName());
                if (nodeTypes[nodeID] == null)
                {
                    //	System.err.println(nodeID+" doesn't exist.\n");
                    return new ArrayList<Integer>();
                }
                else
                {
                    LogInfo.error(nodeTypes[nodeID].toString());
                }
            }
        }
        else
        {
            for (Integer childid : childlist)
            {
                leaflist = getLeaves(childid, leaflist);
            }
        }
        return leaflist;
    }

    public List<Pair<String, String>> getLeavesPreTerminal(int nodeId, List<Pair<String, String>> leafList)
    {
        ArrayList<Integer> childlist = children.get(nodeId);
        if (childlist == null || childlist.isEmpty())
        {
            if (nodeTypes[nodeId] == TagNodeType.terminal || nodeTypes[nodeId] == TagNodeType.anchor)
            {                
                leafList.add(new Pair(getFullCategory(nodeId), getFullCategory(getParent(nodeId))));
            }
//            else
//            {                
//                LogInfo.error(getTreeName());
//                if (nodeTypes[nodeId] == null)
//                {
//                    //	System.err.println(nodeID+" doesn't exist.\n");
//                    return new ArrayList<>();
//                }
//                else
//                {
//                    LogInfo.error(nodeTypes[nodeId].toString());
//                }
//            }
        }
        else
        {
            for (Integer childid : childlist)
            {
                leafList = getLeavesPreTerminal(childid, leafList);
            }
        }
        return leafList;
    }
    
    protected boolean containsOriginNode(Integer nodeID, MultiValueMap<Integer, Integer> origin)
    {
        if (origin == null)
        {
            return false;
        }
        return origin.containsKey(nodeID);
    }

    @SuppressWarnings("unchecked")
    protected Collection<Integer> getOrigin(Integer nodeID, MultiValueMap<Integer, Integer> origin)
    {
        if (origin == null)
        {
            return null;
        }
        if (origin.containsKey(nodeID))
        {
            return origin.getCollection(nodeID);
        }
        return null;
    }

//    protected Collection getOrigin(String nodeID, MultiValueMap origin)
//    {
//        if (origin != null)
//        {
//            Collection c = origin.getCollection(nodeID);            
//            return c != null ? c : null;
//        }
//        return null;
//    }
    @SuppressWarnings("unchecked")
    protected Integer getLowestOrigin(Integer nodeID, MultiValueMap<Integer, Integer> origin)
    {
        Integer lowest = 9999999;
        if (origin.containsKey(nodeID))
        {
            Collection<Integer> nodeIds = origin.getCollection(nodeID);
            if(nodeIds.isEmpty())
                return -1;
            for(Integer id : nodeIds)
            {
                if (id < lowest)
                {
                    lowest = id;
                }
            }            
//            Iterator<Integer> nodeids = origin.getCollection(nodeID).iterator();
//            if (origin.getCollection(nodeID).isEmpty())
//            {
//                return -1;
////                return "-1";
//                //return null;
//            }
//            while (nodeids.hasNext())
//            {
//                Integer id = nodeids.next();
////                int id = Integer.parseInt((String) nodeids.next());
//                if (id < lowest)
//                {
//                    lowest = id;
//                }
//            }
        }
        else
        {
            return null;
        }
        return lowest;
//        return Integer.toString(lowest);
    }

    public int countLeaves(int nodeID)
    {
//        int nodeID = Integer.parseInt(nodeID);
        int leaveNumber = 0;
        ArrayList<Integer> childlist = children.get(nodeID);
        if (childlist == null)
        {
            if (nodeTypes[nodeID] == TagNodeType.terminal
                    || nodeTypes[nodeID] == TagNodeType.anchor)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
        else
        {
            for (Integer childid : childlist)
            {
                leaveNumber += countLeaves(childid);
            }
        }
        return leaveNumber;
    }

    public void setRoot(int root)
    {
        this.root = root;
    }

    public boolean isHeadChild(int child)
    {
        return isHeadChild[child];
//        return isHeadChild[Integer.parseInt(child)];
    }

    public String printHeadChild(int child)
    {
        //int child = Integer.parseInt(child);
        if (isHeadChild[child] != null)
        {
            return isHeadChild[child].toString();
        }
        else
        {
            return "?";
        }
    }

    public String getCategory(int nodeid)
    {
        return categories[nodeid];
//        return categories[Integer.parseInt(nodeid)];
    }

    public Integer getParent(int current)
    {
        return parent[current];
//        return parent[Integer.parseInt(current)];
    }

    public boolean hasParent(int id)
    {
        return id >= 0 && id < parent.length;
    }
    
    public byte getLowerIndex(int leaf)
    {
        return indexDown[leaf];
    }

    public byte getUpperIndex(int leaf)
    {
        return indexUp[leaf];
    }

    public boolean hasFootLeft()
    {
        if (nodeTypes[getChildren(getParent(foot)).get(0)] == TagNodeType.foot)
//        if (nodeTypes[Integer.parseInt(getChildren(getParent(foot)).get(0))] == TagNodeType.foot)
        {
            return true;
        }
        if (nodeTypes[getChildren(getParent(foot)).get(getChildren(getParent(foot)).size() - 1)] == TagNodeType.foot)
        {
            return false;
        }
        LogInfo.error("TRUE NON-TIG tree!!!" + print());
        return false;
    }

    public ArrayList<Integer> getNodes()
    {
        ArrayList<Integer> nodeids = new ArrayList<Integer>();
        for (int i = 0; i < categories.length; i++)
        {
            if (categories[i] != null)
            {
                nodeids.add(i);
            }
        }
        return nodeids;
    }

    public ArrayList<Integer> getShadowSourceTreesRootList()
    {
        ArrayList<Integer> shadowroots = new ArrayList<Integer>();
        ArrayList<Byte> shadowIndeces = new ArrayList<Byte>();
        int rootInt = getRoot();
//        int rootInt = Integer.parseInt(getRoot());
        if (getLowerIndex(rootInt) > 0)
        {
            shadowroots.add(rootInt);
            shadowIndeces.add(getLowerIndex(shadowroots.get(0)));
        }
        for (int node : getNodes())
        {
            byte lIn = getLowerIndex(node);
            byte uIn = getUpperIndex(node);
            if (uIn > -1 && lIn > 0 && uIn != lIn && !shadowIndeces.contains(lIn))
            {
                shadowroots.add(node);
                shadowIndeces.add(lIn);
            }
        }
        return shadowroots;
    }

    public void updateIndices(ArrayList<Integer> rootList, StringTree tree)
    {
        int newIndex = 0;
        for (int root : rootList)
        {
            byte index = tree.getLowerIndex(root);
            if (index == 0)
            {
                continue;
            }
            if (index > newIndex)
            {
                newIndex = index;
            }
        }
        for (int sr : getShadowSourceTreesRootList())
        {
            if (indexDown[sr] == 0)
            {
                continue;
            }
            newIndex++;
            replaceIndeces(indexDown[sr], (byte) newIndex);
        }
    }

    public void replaceIndeces(byte localShadowIndex, byte newIndex)
    {
        for (Integer node : getNodes())
        {
            if (indexUp[node] != -1 && indexUp[node] == localShadowIndex)
            {
                indexUp[node] = newIndex;
            }

            if (indexDown[node] != -1 && indexDown[node] == localShadowIndex)
            {
                indexDown[node] = newIndex;
            }
        }
    }

    /*
     public void replaceIndeces(String localShadowIndex, String newIndex) {
     for (Integer node : getNodes()){
     if (indexUp[node]!=null &&indexUp[node].equals(localShadowIndex)){
     indexUp[node] = newIndex;
     }
    
     if (indexDown[node]!=null &&indexDown[node].equals(localShadowIndex)){
     indexDown[node] = newIndex;
     }
     }	
     }
     */
    public void replaceWithNewTree(StringTree tree, ArrayList<Byte> ip, HashMap<Integer, Integer> coveredNodes)
    {

        for (Integer nodeid : coveredNodes.keySet())
        {
            Integer verificationNode = coveredNodes.get(nodeid);
            if (tree.indexDown[verificationNode] == -1)
            {
            }
            else if (matches(ip, indexDown[nodeid]))
            {
                indexDown[nodeid] = tree.indexDown[verificationNode];
            }
            if (tree.indexUp[verificationNode] == -1)
            {
            }
            else if (indexUp[nodeid] != -1 && matches(ip, indexUp[nodeid]))
            {
                indexUp[nodeid] = tree.indexUp[verificationNode];
            }
        }
        // I think we don't need to know this for the TreeState because for calculating the fringe, we can 
        // use the results from the verification process which uses the new tree.
		/*
         ArrayList<Integer> remainingNodes = tree.getNodes();
         remainingNodes.removeAll(coveredNodes.values());
         for (Integer remainingNode : remainingNodes){
        
         }*/
    }

    private static boolean matches(ArrayList<Byte> p, byte testpattern)
    {
        for (byte pat : p)
        {
            if (pat == testpattern)
            {
                return true;
            }
        }
        return false;
    }
    
    public ElementaryStringTree copy()
    {
        return copy(true);
    }
    
    @SuppressWarnings("unchecked")
    public ElementaryStringTree copy(boolean copyStringTree)
    {
//        ElementaryStringTree copy = new ElementaryStringTree(this.probability + "\t" + (copyStringTree ? this.getStructure(this.root, useSemantics) : " "), useSemantics);
        ElementaryStringTree copy;
        if(copyStringTree)
            copy = new ElementaryStringTree(this.probability + "\t" + this.getStructure(this.root, useSemantics), useSemantics);
        else
            copy = new ElementaryStringTree(useSemantics);
        copy.fullcategories = this.fullcategories.clone();
        copy.categories = this.categories.clone();
        copy.roles = this.roles.clone();
        copy.nodeTypes = this.nodeTypes.clone();
        copy.parent = this.parent.clone();
        copy.holes = this.holes.clone();
        copy.isHeadChild = this.isHeadChild.clone();
        copy.children = (HashMap<Integer, ArrayList<Integer>>) this.children.clone();
        copy.indexDown = this.indexDown.clone();
        copy.indexUp = this.indexUp.clone();
        copy.root = this.root;
        copy.foot = this.foot;
        copy.auxtree = this.auxtree;
        copy.arraysize = this.arraysize;        
        //copy.adjPossible = this.adjPossible.clone();
        return copy;
    }

    public String getFullCategory(int string)
    {
        return this.fullcategories[string];
//        return this.fullcategories[Integer.parseInt(string)];
    }

    public void putNodeType(int id, TagNodeType nodeType)
    {
        nodeTypes[id] = nodeType;
//        nodeTypes[Integer.parseInt(id)] = nodeType;
    }

    public void putFullCategory(int id, String fullcat)
    {
        fullcategories[id] = fullcat;
//        fullcategories[Integer.parseInt(id)] = fullcat;
    }

    public void putCategory(int id, String cat)
    {
        categories[id] = cat;
//        categories[Integer.parseInt(id)] = cat;
    }

    public void putParent(int nodeId, Integer parentnodeId)
    {
        parent[nodeId] = parentnodeId;
//        parent[Integer.parseInt(nodeId)] = parentnodeId;
    }

    public void putHeadChild(int string, boolean b)
    {
        this.isHeadChild[string] = b;
//        this.isHeadChild[Integer.parseInt(string)] = b;
    }

    public void putIndexUp(Integer node, byte upIndex)
    {
        this.indexUp[node] = upIndex;
    }

    public void putIndexDown(Integer node, byte downIndex)
    {
        this.indexDown[node] = downIndex;
    }

    public void putChildren(Integer string, ArrayList<Integer> children)
    {
        this.children.put(string, children);

    }

    public void shiftNodeIndices(int startNode, int shiftDistance)
    {
        //System.out.println("\n"+ print());
        if (shiftDistance == 0)
        {
            return;
        }
        int maxindex = getNodes().get(getNodes().size() - 1) + shiftDistance;
        if (maxindex >= arraysize)
        {
            this.makeArraysBigger(maxindex);
        }
        for (int i = getNodes().get(getNodes().size() - 1); i >= startNode; i--)
        {
            int newIndex = i + shiftDistance;
            fullcategories[newIndex] = fullcategories[i];
            fullcategories[i] = null;
            categories[newIndex] = categories[i];
            categories[i] = null;
            nodeTypes[newIndex] = nodeTypes[i];
            nodeTypes[i] = null;
            isHeadChild[newIndex] = isHeadChild[i];
            isHeadChild[i] = null;
            indexDown[newIndex] = indexDown[i];
            indexDown[i] = -1;
            indexUp[newIndex] = indexUp[i];
            indexUp[i] = -1;
            //adjPossible[newIndex] = adjPossible[i];
            //adjPossible[i]=null;
            //hope this works for identifying the right parents...
            if (parent[i] != null)
            {
                int parentId = parent[i];
//                int parentId = Integer.parseInt(parent[i]);
                if (parentId >= startNode)
                {
                    parent[newIndex] = parentId + shiftDistance;
//                    parent[newIndex] = Integer.toString(parentId + shiftDistance);
                }
                else
                {
//                    String parentIdString = Integer.toString(parentId);
                    parent[newIndex] = parentId;
                    ArrayList<Integer> newChildren = new ArrayList<Integer>();
                    for (Integer origChild : children.get(parentId))
                    {
                        int origChildId = origChild;
//                        int origChildId = Integer.parseInt(origChild);
                        if (origChildId == i)
                        {
                            newChildren.add(newIndex);
//                            newChildren.add(Integer.toString(newIndex));
                        }
                        else
                        {
                            newChildren.add(origChild);
                        }
                    }
                    children.put(parentId, newChildren);
                }
            }
            parent[i] = null;
            ArrayList<Integer> newChildren = new ArrayList<Integer>();
            //String i = Integer.toString(i);
            if (children.containsKey(i))
            {
                for (Integer origChild : children.get(i))
                {
                    // int origChildId = origChild;
//                    int origChildId = Integer.parseInt(origChild);
                    if (origChild >= startNode)
                    {
                        newChildren.add(origChild + shiftDistance);
//                        newChildren.add(Integer.toString(origChildId + shiftDistance));
                    }
                    else
                    {
                        newChildren.add(origChild);
                    }
                }
                children.put(newIndex, newChildren);
//                children.put(Integer.toString(newIndex), newChildren);
                children.remove(i);
            }
        }
        if (root >= startNode)
//        if (Integer.parseInt(root) >= startNode)
        {
            root += shiftDistance;
//            root = Integer.toString(Integer.parseInt(root) + shiftDistance);
        }
        if (auxtree && foot >= startNode)
//        if (auxtree && Integer.parseInt(foot) >= startNode)
        {
            foot += shiftDistance;
//            foot = Integer.toString(Integer.parseInt(foot) + shiftDistance);
        }
        //System.out.println(print()+"\n------");
    }

    public void addNode(StringTree fullTree, int newPosition, int fullTreePosition)
    {
        fullcategories[newPosition] = fullTree.fullcategories[fullTreePosition];
        categories[newPosition] = fullTree.categories[fullTreePosition];
        nodeTypes[newPosition] = fullTree.nodeTypes[fullTreePosition];
        isHeadChild[newPosition] = fullTree.isHeadChild[fullTreePosition];
        indexDown[newPosition] = fullTree.indexDown[fullTreePosition];
        indexUp[newPosition] = fullTree.indexUp[fullTreePosition];
        //	adjPossible[newPosition] = fullTree.adjPossible[fullTreePosition];
		/* sort out parent and child relation in main program
         if (parent[i]!=null){
         int parentId = Integer.parseInt(parent[i]);
         if (parentId >= startNode){
         parent[newIndex] = (parentId+shiftDistance)+"";
         }
         else{
         parent[newIndex] = parentId+"";
         ArrayList<String> newChildren = new ArrayList<String>();
         for (String origChild : children.get(parentId+"")){
         int origChildId =Integer.parseInt(origChild);
         if (origChildId >= startNode)
         newChildren.add((origChildId + shiftDistance) + "" );
         else newChildren.add(origChild);
         }
         children.put(parentId+"", newChildren);
         }
         }
         parent[i] = null;
         */
    }

    public ArrayList<Integer> getDescendentNodes(int shadownode)
    {
        ArrayList<Integer> descendents = new ArrayList<Integer>();
        descendents.add(shadownode);
//        descendents.add(Integer.parseInt(shadownode));
        for (Integer child : children.get(shadownode))
        {
            descendents.addAll(getDescendentNodes(child));
        }
        return descendents;
    }

    public String getMainLeaf(int startNode)
    {        
        int origStartNode = startNode;
        //int startNode = Integer.parseInt(startNode);
        if (nodeTypes[startNode] != TagNodeType.internal)
        {
            startNode = parent[startNode];
        }
        Integer headChild = getHeadChild(startNode);
        int parentNode = Integer.MIN_VALUE;
        while (headChild != null || (children.containsKey(startNode) && children.get(startNode).size() == 1))
        {
            if (headChild != null)
            {
                startNode = headChild;
                headChild = getHeadChild(startNode);
            }
            else
            {//trace
                if (parentNode == Integer.MIN_VALUE)
//                if (parentNode.equals(""))
                {
                    parentNode = startNode;
                }
                startNode = children.get(startNode).get(0);
                headChild = getHeadChild(startNode);
            }
        }
        TagNodeType startNodeNodeType = getNodeType(startNode);
        //startNode = Integer.parseInt(startNode);
        if (startNodeNodeType == TagNodeType.anchor)
        {
            return new StringBuilder().append(categories[parent[startNode]]).append("\t").append(categories[startNode]).toString();
//            return new StringBuilder().append(categories[Integer.parseInt(parent[startNode])]).append("\t").append(categories[startNode]).toString();
        }
        else if (startNodeNodeType == TagNodeType.internal)
        {
//            if (parentNode.equals(""))
            if (parentNode == Integer.MIN_VALUE)
            {
                return categories[startNode];
            }
            //	System.out.println("StringTree.getMainLeaf()"+categories[Integer.parseInt(parentNode)]+"\t"+categories[startNodeInt]);
            if (categories[startNode].startsWith("*"))
            {
                return new StringBuilder(categories[parentNode]).append("\t").append(categories[startNode]).toString();
//                return new StringBuilder(categories[Integer.parseInt(parentNode)]).append("\t").append(categories[startNode]).toString();
            }
            else
            {
                return categories[startNode];
            }
        }
        else if (startNodeNodeType == TagNodeType.foot)
        {
            return categories[startNode];
        }
        else
        {
            return getMainLeaf(parent[origStartNode]);
//            return getMainLeaf(parent[Integer.parseInt(origStartNode)]);
        }
    }

    public int getMainLeafId(int startNode)
    {
        int origStartNode = startNode;
        if (nodeTypes[startNode] != TagNodeType.internal)
        {
            startNode = parent[startNode];
        }
        Integer headChild = getHeadChild(startNode);
        int parentNode = Integer.MIN_VALUE;
        while (headChild != null || (children.containsKey(startNode) && children.get(startNode).size() == 1))
        {
            if (headChild != null)
            {
                startNode = headChild;
                headChild = getHeadChild(startNode);
            }
            else
            {//trace
                if (parentNode == Integer.MIN_VALUE)
                {
                    parentNode = startNode;
                }
                startNode = children.get(startNode).get(0);
                headChild = getHeadChild(startNode);
            }
        }
        TagNodeType startNodeNodeType = getNodeType(startNode);        
        if (startNodeNodeType == TagNodeType.anchor)
        {
            return startNode; // parent[startNode];
        }
        else if (startNodeNodeType == TagNodeType.internal)
        {
            if (parentNode == Integer.MIN_VALUE)
            {
                return startNode;
            }            
            if (categories[startNode].startsWith("*"))
            {
                return startNode; // parentNode;
            }
            else
            {
                return startNode;
            }
        }
        else if (startNodeNodeType == TagNodeType.foot)
        {
            return startNode;
        }
        else
        {
            return getMainLeafId(parent[origStartNode]);
        }
    }
    
    public Integer getHeadChild(Integer startnode)
    {
        List<Integer> children = this.getChildren(startnode);
        if (children == null || children.isEmpty())
        {
            return null;
        }
        for (Integer c : children)
        {
            if (this.isHeadChild[c])
//            if (this.isHeadChild[Integer.parseInt(c)])
            {
                return c;
            }
        }
        return null;
    }

    public String getUnlexStruct(int nodeID)
    {
        return getUnlexStruct(nodeID, useSemantics);
    }
    public String getUnlexStruct(int nodeID, boolean useSemanticsIn)
    {

        StringBuilder struct = new StringBuilder();
        List<Integer> childlist = children.get(nodeID);
        if (childlist == null || childlist.isEmpty())
        {
            if (getNodeType(nodeID) != TagNodeType.anchor)
            {
                return " " + getLexName(nodeID, useSemanticsIn);
            }
            else
            {
                return ""; // (space produces better-looking output, but breaks features stored in file (pre-April 2014), without spaces)
            }
        }
        StringBuilder childString = new StringBuilder();
        for (Integer childid : childlist)
        {            
            childString.append(getUnlexStruct(childid, useSemanticsIn));
        }
        struct.append("(").append(getLexName(nodeID, useSemanticsIn)).append(childString).append(")"); // ( " ( " produces better-looking output)
        return struct.toString();
    }

    public String toStringUnlex(boolean useSemantics)
    {
        return getUnlexStruct(root, useSemantics);
    }
    
    public List<String> getNonEmptyCategories()
    {
        List<String> nodecats = new ArrayList<String>();
        for (int i = 0; i < categories.length; i++)
        {
            if (categories[i] != null && this.nodeTypes[i] == TagNodeType.internal)
            {
                nodecats.add(categories[i]);
            }
        }

        return nodecats;
    }

    public List<String> getCategories()
    {
        return Arrays.asList(categories);
    }
    
    public String printNoTraces()
    {
        StringBuilder out = new StringBuilder();
        out.append(getStructureNoTraces(root));
        return out.toString();
    }

    private String getStructureNoTraces(int nodeID)
    {
        StringBuilder struct = new StringBuilder();
        ArrayList<Integer> childlist = children.get(nodeID);
        if (childlist == null || childlist.isEmpty())
        {
            return getLexNameSimpleCat(nodeID);
        }
        StringBuilder childString = new StringBuilder();
        int childno = 0;
        for (Integer childid : childlist)
        {
            String childs = getStructureNoTraces(childid);
            if ((childno > 0 || (childno == 0 && childlist.size() > 1))
                    && childs.contains("*"))
            {
                continue;
            }
            if ((childno > 0 || (childno == 0 && childlist.size() > 1))
                    && childs.contains(" 0"))
            {
                continue;
            }
            childString.append(" ").append(childs);
            childno++;
        }
        struct.append("( ").append(getLexNameSimpleCat(nodeID)).append(childString).append(")");
        return struct.toString();
    }

    public String printSimpleCat()
    {
        StringBuilder out = new StringBuilder().append(treeString).append("\t");
        if (auxtree)
        {
            out.append("MOD \t");
        }
        else
        {
            out.append("ARG \t");
        }
        out.append(getStructureSimpleCat(root));
        return out.toString();
    }

    public String printSimple(int nodeID, boolean keepEmptyNodes)
    {
        StringBuilder struct = new StringBuilder();
        ArrayList<Integer> childlist = children.get(nodeID);
        if (childlist == null || childlist.isEmpty())
        {   // remove possibly partial derivations with no yield
            return keepEmptyNodes || nodeTypes[nodeID] == TagNodeType.anchor ||
                    // HACK: check whether this is actually a word and is wrongly 
                    // not assigned an anchor type (happens with phrasal verbs)
                    Utils.isLowerCase(categories[nodeID]) 
                    ? categories[nodeID] : "";            
        }
        StringBuilder childString = new StringBuilder();
        for (Integer childid : childlist)
        {
            childString.append(" ").append(printSimple(childid, keepEmptyNodes));
        }
        struct.append("(").append(categories[nodeID]).append(childString).append(")");
        return struct.toString();
    }
    
    private String getStructureSimpleCat(int nodeID)
    {
        StringBuilder struct = new StringBuilder();
        ArrayList<Integer> childlist = children.get(nodeID);
        if (childlist == null || childlist.isEmpty())
        {
            return getLexNameSimpleCat(nodeID);
        }
        StringBuilder childString = new StringBuilder();
        for (Integer childid : childlist)
        {
            childString.append(" ").append(getStructureSimpleCat(childid));
        }
        struct.append("( ").append(getLexNameSimpleCat(nodeID)).append(childString).append(")");
        return struct.toString();
    }

    private String getLexNameSimpleCat(int nodeID)
    {
        //int nodeID = Integer.parseInt(nodeID);
        if (nodeTypes[nodeID] == TagNodeType.anchor || nodeTypes[nodeID] == TagNodeType.terminal)
        {
            if (categories[nodeID].contains("*"))
            {
                return new StringBuilder(categories[nodeID]).append("^").append(printOrigin(nodeID, originUp)).append("_").append(printOrigin(nodeID, originDown)).append(nodeTypes[nodeID].getMarker()).toString();
            }
            else
            {
                return new StringBuilder().append(categories[nodeID]).append(nodeTypes[nodeID].getMarker()).toString();
            }
        }
        if (nodeTypes[nodeID] == TagNodeType.predicted)
        {
        }
        if (originUp == null || originUp.isEmpty())
        {
            return new StringBuilder().append(categories[nodeID]).append("^").append(indexUp[nodeID]).append("_").append(indexDown[nodeID]).append(nodeTypes[nodeID].getMarker()).toString();
        }
        else
        {
            return new StringBuilder().append(categories[nodeID]).append("^").append(printOrigin(nodeID, originUp)).append("_").append(printOrigin(nodeID, originDown)).append(nodeTypes[nodeID].getMarker()).toString();
        }
    }

    public void setProbability(double d)
    {
        this.probability = d;
    }

    public void setFoot(int i)
    {
//        foot = i + "";
        foot = i;

    }

    public int getLastAnchorID()
    {
        int id = -1;
        for (int i = 1; i < this.nodeTypes.length; i++)
        {
            TagNodeType mnt = nodeTypes[i];
            if (mnt == null)
            {
                return id;
            }
            if (mnt == TagNodeType.anchor)
            {
                id = i;
            }
        }
        return id;
    }

    public void addNodeAtTop(String bestpos)
    {
        int newid = 0;
        for (int i = categories.length - 1; i > 0; i--)
        {
            if (categories[i] != null)
            {
                newid = i + 1;
                break;
            }
        }
        if (newid + 1 >= categories.length)
        {
            this.makeArraysBigger(newid + 1);
        }
        String[] posword = bestpos.split(" ");
        String pos = posword[0].toUpperCase();
        String word = posword[1];
        categories[newid] = pos;
        categories[newid + 1] = word;
        ArrayList<Integer> cs = children.get(root);
        int posid = newid;
//        String posid = newid + "";
        int wordid = newid + 1;
        //String wordid = wordid + "";
        cs.add(posid);
        children.put(root, cs);
        ArrayList<Integer> cs2 = new ArrayList<Integer>();
        cs2.add(wordid);
        children.put(posid, cs2);
        fullcategories[newid] = pos;
        fullcategories[wordid] = word;
        indexDown[newid] = 0;
        indexUp[newid] = 0;
        indexDown[wordid] = 0;
        indexUp[wordid] = 0;
        isHeadChild[wordid] = false;
        isHeadChild[newid] = false;
        nodeTypes[newid] = TagNodeType.internal;
        nodeTypes[wordid] = TagNodeType.anchor;
        parent[wordid] = posid;
        parent[newid] = root;
    }

    public boolean isOnRootToFootPath(Integer nodeid)
    {
        if (!this.isAuxtree())
        {
            return false;
        }
        if (this.rootToFootPath == null)
        {
            calculateRootToFoot();
        }
        if (this.rootToFootPath.contains(nodeid))
        {
            return true;
        }
        return false;
    }

    private void calculateRootToFoot()
    {
        Integer nodeOnPath = this.getFoot();
        this.rootToFootPath = new ArrayList<Integer>();
        rootToFootPath.add(nodeOnPath);
        nodeOnPath = this.getParent(nodeOnPath);
        while (nodeOnPath != null)
        {
            rootToFootPath.add(nodeOnPath);
            nodeOnPath = getParent(nodeOnPath);
        }
    }
    
    public int getNumOfRoles() // slow
    {
        int count = 0;
        for(String role : roles)
        {
            count += role != null ? 1 : 0;
        }
        return count;
    }
}