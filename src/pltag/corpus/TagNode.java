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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TagNode
{

    private TagNode parent;
    private TagNode rightsibling, leftsibling;
    private List<TagNode> children = new ArrayList<TagNode>();
    private boolean hole;
    private boolean head = false;
    private TagNode headchild;
    private String category;
    private String fullcategory;
    private boolean argument;
    private String argmod = "UNDEF";
    private LexEntry lexentry = null;
    private int nodeId;
    private boolean hasparent = false;
    protected boolean hasTrace = false;
    protected String traceID;
    protected Trace trace;
    private StringTree stree = null;
    private Map<LeafNode, String> role = new HashMap<LeafNode, String>();

    public TagNode(IdGenerator idgen, String cat)
    {

        this.fullcategory = cat;
        category = cat;
        if (fullcategory.contains("-") && !fullcategory.equals("-NONE-"))
        {
            this.category = fullcategory.substring(0, fullcategory.indexOf("-"));
        }
        nodeId = idgen.getNewId();
    }

    public void setStringTree(StringTree st)
    {
        stree = st;
    }

    public StringTree getStringTree()
    {
        return stree;
    }

    public TagNode(int nid, String cat)
    {

        this.fullcategory = cat;
        category = cat;
        if (fullcategory.contains("-") && !fullcategory.equals("-NONE-"))
        {
            this.category = fullcategory.substring(0, fullcategory.indexOf("-"));
        }
        nodeId = nid;
    }

    protected abstract Map<String, Trace> traceTreatment(Map<String, Trace> tracelist);

    public void setCategory(String string)
    {
        category = string;
        fullcategory = string;
    }

    public void toAux()
    {
        if (category.equals("VP"))
        {
            for (TagNode child : children)
            {
                if (child.isLeaf())
                {
                    String childLeaf = ((LeafNode) child).getLeaf();
                    if ((childLeaf.matches("(have)|('ve)|(has)|(had)|('d)|(having)")
                            || childLeaf.matches("(be)|(is)|('s)|(are)|('re)|(were)|(was)|(being)|(am)|('m)|(been)")
                            || childLeaf.matches("(do|(did)|(does)|(doing)|(done))"))
                            && (child.rightsibling != null)
                            && (child.rightsibling.category.startsWith("VP")
                            || (child.rightsibling.category.matches("(RB)|(ADVP.*)")
                            && child.rightsibling.rightsibling != null
                            && child.rightsibling.rightsibling.category.equals("VP"))))
                    {
                        child.category = "AUX";
                        child.fullcategory = "AUX";
                    }
                    // have to
                    else if (childLeaf.matches("(have)|('ve)|(has)|(had)|('d)|(having)")
                            && child.rightsibling != null && child.rightsibling.category.equals("S"))
                    {
                        child.category = "MOD";
                        child.fullcategory = "MOD";
                    }
                }
            }
        }
    }

    public void toCopula()
    {
        if (category.equals("VP"))
        {
            for (TagNode child : children)
            {
                if (child.fullcategory.matches(".*-PRD")
                        && child.leftsibling.category.startsWith("VB"))
                {
                    child.leftsibling.category = "COP";
                    child.leftsibling.fullcategory = "COP";
                }
            }
        }
    }

    public String printArgMod()
    {
        if (argmod.equals("GUESS"))
        {
            if (this.argument)
            {
                return "ARG";
            }
            else
            {
                return "MOD";
            }
        }
        else
        {
            return argmod;
        }
    }

    protected Map<String, Trace> makeTrace(Map<String, Trace> tracelist, String traceID)
    {
        Trace trace;
        if (!tracelist.containsKey(traceID))
        {
            trace = new Trace(this);
        }
        else
        {
            trace = tracelist.get(traceID);
            trace.addInfo(this);
        }
        tracelist.put(traceID, trace);
        hasTrace = true;
        this.trace = trace;
        return tracelist;
    }

    public Trace getTrace()
    {
        return trace;
    }

    /*public void setTrace(Trace t, String tid){
    trace = t;
    traceID = tid;
    }
    
    public String getTraceID(){
    return traceID;
    }
     */
    public boolean hasTrace()
    {
        return hasTrace;
    }

    public void setLexEntry(LexEntry le)
    {
        lexentry = le;
    }

    public LexEntry getLexEntry()
    {
        return lexentry;
    }

    public void setArgument()
    {
        argument = true;
        argmod = "ARG";
        //System.out.println(fullcategory);
    }

    public void setModifier()
    {
        argument = false;
        argmod = "MOD";
        //System.out.println(fullcategory);
    }

    public String getArgMod()
    {
        return argmod;
    }

    public void setArgModGuess()
    {
        argmod = "GUESS";
    }

    public boolean isArgument()
    {
        return argument;
    }

    public void fillHole()
    {
        hole = false;
    }

    public void makeHead()
    {
        head = true;
    }

    public void makeNotHead()
    {
        head = false;
    }

    public void setHeadChild(TagNode node)
    {
        headchild = node;
    }

    public void setParent(TagNode thisnode)
    {
        this.parent = thisnode;
        this.hasparent = true;
    }

    public boolean hasParent()
    {
        return hasparent;
    }

    public void setLeftsib(TagNode lastchild)
    {
        this.leftsibling = lastchild;
    }

    public void setRightsib(TagNode child)
    {
        this.rightsibling = child;
    }

    public void addChild(TagNode child)
    {
        children.add(child);
    }

    public void addChild(int pos, TagNode child)
    {
        children.add(pos, child);
    }

    public List<TagNode> getChildlist()
    {
        return children;
    }

    public TagNode getParent()
    {
        return parent;
    }

    public TagNode getLeft()
    {
        return leftsibling;
    }

    public TagNode getRight()
    {
        return rightsibling;
    }

    public Boolean isHole()
    {
        return hole;
    }

    public Boolean isHead()
    {
        return head;
    }

    public boolean isLeaf()
    {
        return false;
    }

    public String getCategory()
    {
        return category;
    }

    public String getFullCategory()
    {
        return fullcategory;
    }

    public TagNode getHeadChild()
    {
        return headchild;
    }

    public int getNodeID()
    {
        return nodeId;
    }

    public String getName()
    {
        return category;
    }

    protected void setHole(boolean b)
    {
        hole = b;
    }

    public boolean isOnSpine(LeafNode annotatedword)
    {
        TagNode node = annotatedword;//attention! don't leave current lexicon entry!!!
        while (node != this)
        {
            if (!node.hasParent())
            {//|| ! node.isHead()){
                return false;
            }
            node = node.getParent();
        }
        return true;
    }

    public boolean isInSameTree(LeafNode annotatedword)
    {
        TagNode node = annotatedword;//attention! don't leave current lexicon entry!!!
        while (node.isHead())
        {
            if (node == this)
            {
                return true;
            }
            node = node.getParent();
        }
        if (node == this)
        {
            return true;
        }
        return false;
    }

    public TagNode removeChild(TagNode currentNode)
    {
        children.remove(currentNode);
        return currentNode;
    }

    public String toString()
    {
        return nodeId + ": " + category;
    }

    public void setRole(LeafNode head, String relation)
    {
        role.put(head, relation);
    }

    @SuppressWarnings("unchecked")
    public String getVerboseRole()
    {
        StringBuilder sb = new StringBuilder();
        for (TagNode key : role.keySet())
        {
            if (sb.toString().contains("@" + role.get(key) + ";"))
            {
                continue;
            }
            sb.append("@") //		.append(key.toString())
                    //		.append("=")
                    .append(role.get(key)).append(";");
        }
        return sb.toString();
    }

    public String getRole()
    {
        if (hasparent && getParent().lexentry != null && role.containsKey(getParent().lexentry.getMainLeafNode()))
        {
            return "@" + role.get(getParent().lexentry.getMainLeafNode()) + ";";
        }
        else if (category.equals(":") && role.containsKey(getParent().getParent().lexentry.getMainLeafNode()))
        {
            return "@" + role.get(this.getParent().getParent().lexentry.getMainLeafNode()) + ";";
        }
        else
        {
            return getVerboseRole();
        }
    }
}
