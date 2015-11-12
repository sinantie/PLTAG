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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import pltag.parser.TreeState;
import pltag.parser.semantics.RoleSignature;

public class ElementaryStringTree extends CompositeStringTree
{

    private int anchor = Integer.MIN_VALUE;
    private String filler = "";
    private String trace = "";
    private LexEntry tracefather = null;
    private StringTree MCrest = null;
    private String treeParsingString;
    private int originNumber = Integer.MIN_VALUE;
    private int pseudoAnchorNumber = Integer.MIN_VALUE;
    private boolean verificTree = false;
    private String semanticFrame = "";
    private static Pattern digits = Pattern.compile(".*-[0-9]+");
    private static Pattern tracedigits = Pattern.compile("([*]T)?[*]-[0-9]+");
//	private boolean firstInSentence = false;
    //private String tuple = "";

    private Map<Integer, String> rolesAtInternalNodes;
    private Set<RoleSignature> roleSignatures;
    private boolean isRelation;
    Map<Integer, String> annotatedRoles;
    private int subcategorisedAnchor = Integer.MIN_VALUE;
    /**
     * Converts a Lexicon Entry LexEntry into a StringTree.
     * 
     * Create stringtree only if not exists already! (this can happen in coordination, maybe traces, supportnodes!
     * @param le
     */
    public ElementaryStringTree(LexEntry le, IdGenerator idgen, boolean useSemantics)
    {
        super(useSemantics);
        if (TagCorpus.verbose)
        {
            LogInfo.error("ElementaryStringTree.build: " + le);
        }
        annotatedRoles = new HashMap<Integer, String>();
        rolesAtInternalNodes = new HashMap<Integer, String>();
        //iterate through nodes in lexicon entry
        //and decide on their node type
        List<LexEntry> lexEntries = new ArrayList<LexEntry>();
        lexEntries.add(le);

        if (le.isPartOfMultiLex())
        {
            //lexentries.addAll(le.getMultiEntry());
            this.verificTree = true;
        }
        if (le.isMultiLex())
        {
            lexEntries.addAll(le.getMultiEntry());
        }
        //build tree
        List<TagNode> unknowns = new ArrayList<TagNode>();
        //for (LexEntry lexentry: lexentries){
        for (TagNode node : le.getNodes().values())
        {
            addNodeToStringTree(node, unknowns, le, lexEntries, idgen);
            //addTraceTrees(node);
        }
        //}
        //special additional information:
        treeString = le.getExpression();
//		special mark on head lexical tree (anchor):
        makeAnchor(le);

        // if this is a coordination tree (which is not a sentence-initial conjunct)
        if (le != null && (le.getRootCat().equals("CONJP")
                || (le.getMultiEntry() != null && le.getMultiEntry().get(0).getMainLeafNode().getCategory().equals("CC"))
                || le.getMainLeafNode().getCategory().equals("CC")))
        {
            //makeRootAndFootNode(le, unknowns);	
            if (le.isMultiLex() || le.isPartOfMultiLex())
            {
                makeRootAndFootNode(le, unknowns, idgen);
            }
            // sentence-initial conjunction
            else if (le.getMainLeafNode().getLeft() == null && !le.getRootCat().equals("CONJP"))
            {
                le.getMainLeafNode().setCategory("SICC");
                makeRootAndFootNode(le, unknowns, idgen);
            }
            else if (le.getRootCat().equals("CONJP") && le.getRoot().getLeft() == null)
            {
                //System.out.println("make foot and root node");
                makeRootAndFootNode(le, unknowns, idgen);
            }
            else
            {
                //System.out.println("make coordination");
                makeCoordination(le);
            }
        }
        else if (isOtherConjunct(le))
        {
            //System.out.println("make other conjunct");
            makeOtherConjunct(le, idgen);
        }
        else
        {
            //System.out.println("make foot and root node");
            makeRootAndFootNode(le, unknowns, idgen);
        }
        cleanupRolesOnInternalNodes();
        originUp.remove(root);
        if (TagCorpus.verbose)
        {
            LogInfo.logs("ElemStringTree: " + this.toString());
        }
    }

    public ElementaryStringTree(String input, boolean useSemantics, Set<RoleSignature> roleSignatures, boolean isRelation)
    {
        super(useSemantics);
        String[] probAndString = input.split("\t");
        probability = Double.parseDouble(probAndString[0]);
        treeParsingString = probAndString[1];
        this.roleSignatures = roleSignatures;
        this.isRelation = isRelation;
    }
    
    public ElementaryStringTree(boolean useSemantics)
    {
        super(useSemantics);
    }
    
    public ElementaryStringTree(String input, boolean useSemantics)
    {
        super(useSemantics);
        String[] probAndString = input.split("\t");
        probability = Double.parseDouble(probAndString[0]);
        treeParsingString = probAndString[1];        
    }

    @Override
    public String print()
    {
        return super.print();/*
        String out = treeString + "\t";
        if (auxtree){
        out = out + "MOD "+"\t";
        }
        else{
        out = out + "ARG "+"\t";
        }
        //out += getStatistics()+"\t";//"@@" + "rootcat:"+categories[Integer.parseInt(root)]+
        out = out + getStructure(root);
        
        return out;*/
    }

    /**
     * Constructs the root node and foot node for auxiliary trees.
     * 
     * @param le
     * @param unknowns
     * @param idgen 
     */
    private void makeRootAndFootNode(LexEntry le, List<TagNode> unknowns, IdGenerator idgen)
    {
        //if complement auxiliary tree: here: if root node and subst node have the same category
        root = le.getRoot().getNodeID();//may be mistake here? if does not work, comment other same lines back in.
        le.getRoot().setStringTree(this);//just to make sure.
        if (le.getRootCat().equals(""))
        {
            auxtree = false;
        }
        else if (le.getRoot().getArgMod().equals("UNDEF") && isRecursiveTree(le, unknowns))
        {
            //recursiveTree method checks whether tree recursive. if yes, builds an auxiliary tree.
            createRecursiveTree(le, unknowns, idgen);
            //root = le.getRoot().getNodeID();
            auxtree = true;
            originDown.put(root, le.getFirstLeafNode().getLeafNo());
//            originDown.put(root, le.getFirstLeafNode().getLeafNumber());
        }
        //if athematic auxiliary tree
        else if (le.getRoot().getArgMod().equals("UNDEF") || le.isAuxtree())
        {
            auxtree = true;
            if (le.getRoot().hasParent())
            {
                if (!le.getRoot().getParent().getCategory().equals(""))
                {
                    makeFootNode(le.getRoot(), idgen);
                    originUp.put(foot, le.getFirstLeafNode().getLeafNo());
//                    originUp.put(foot, le.getFirstLeafNode().getLeafNumber());
                }
            }
            originDown.put(root, le.getFirstLeafNode().getLeafNo());
//            originDown.put(root, le.getFirstLeafNode().getLeafNumber());

        }
        //if substitution tree
        else
        {
            auxtree = false;
        }
        parent[root] = null;
//        parent[Integer.parseInt(root)] = null;
        // assign potential semantic role to the closest foot node. 
        //Abort if it has already been dealt with in case of a substitution node
        if(auxtree)
        {
            if(rolesAtInternalNodes.size() == 1) // (easy case) deal with trees that have only one role
            {
                Map.Entry<Integer, String> e = rolesAtInternalNodes.entrySet().iterator().next();
                setRole(e.getValue(), foot);
                rolesAtInternalNodes.remove(e.getKey());
                
            }
            else if(rolesAtInternalNodes.size() == 2)
            {
                boolean roleIsSet = false; // set outside for debugging purposes
                for(Map.Entry<Integer, String> e : rolesAtInternalNodes.entrySet())
                {
                    roleIsSet = false;
                    // find sister node and if it's either a foot or substitution node, 
                    // assign it the corresponding annotation of the internal node
                    for(int child : getChildren(parent[e.getKey()]))
                    {
                        if(!roleIsSet && child != e.getKey() && (child == foot || nodeTypes[child] == TagNodeType.subst)) // sister
                        {
                            roleIsSet = setRole(e.getValue(), child);
                            rolesAtInternalNodes.remove(e.getKey());
                        }
                    }
                }
                if(!roleIsSet)
                    LogInfo.logs("Role not set (rolesAtInternalNodes = 2): " + rolesAtInternalNodes + " " + le + " " + toString());
            }
            else if(rolesAtInternalNodes.size() > 2)
            {
                LogInfo.logs("Role not set (rolesAtInternalNodes > 2): " + rolesAtInternalNodes + " " + le + " " + toString());
            }
        }
    }

    /**
     * Constructs a substitution node.
     * @param child
     */
    private boolean  makeSubstNode(TagNode child)
    {        
        makeNewNode(child, TagNodeType.subst);
        boolean roleIsSet = setRole(child.getRole(), child);
        int cnid = child.getNodeID();
//        int cnid = Integer.parseInt(child.getNodeID());
        holes[cnid] = true;
        //overwrite children;
        children.put(child.getNodeID(), new ArrayList<Integer>());
        //if (child.getRole()!=null) 
        //	fullcategories[cnid]+="@"+child.getRole();
        if (originNumber == Integer.MIN_VALUE)
//        if (originNumber.equals(""))
        {
            originNumber = child.getParent().getLexEntry().getFirstLeafNode().getLeafNo();
//            originNumber = Integer.toString(child.getParent().getLexEntry().getFirstLeafNode().getLeafNo());
        }
        origin.remove(child.getNodeID());
        originUp.remove(child.getNodeID());
        origin.put(child.getNodeID(), originNumber);
        originUp.put(child.getNodeID(), originNumber);
        return roleIsSet;
    }

    /**
     * Constructs the anchor of the elementary string tree.
     * @param le
     */
    private void makeAnchor(LexEntry le)
    {
        int anchornodeparentid = le.getMainLeafNode().getNodeID();
        this.semanticFrame = le.getMainLeafNode().getSense();
        int anchornodeid = children.get(anchornodeparentid).get(0);
//        int anchornodeid = Integer.parseInt(children.get(anchornodeparentid).get(0));
        if (!le.getMainLeafNode().isTrace())
        {//TODO: additional condition before:!le.isPartOfMultiLex() &&

            nodeTypes[anchornodeid] = TagNodeType.anchor;
            anchor = anchornodeid;
//            anchor = Integer.toString(anchornodeid);
        }
        originNumber = le.getFirstLeafNode().getLeafNo();//anchornodeid;
//        originNumber = le.getFirstLeafNode().getLeafNumber();//anchornodeid;
        pseudoAnchorNumber = anchornodeid;
//        pseudoAnchorNumber = Integer.toString(anchornodeid);
    }

    private void makeFootNode(TagNode coordfather, IdGenerator idgen)
    {
        LexEntry le = coordfather.getLexEntry();
        TagNode rootparent = le.getRoot().getParent();
        TagNode headsibling = le.getRoot().getParent().getHeadChild();
        int orid = le.getRoot().getNodeID();
//        String orid = le.getRoot().getNodeID();
        int nrid;
        int footid;
        if (headsibling.getCategory().equals(rootparent.getCategory()) && rootparent.getChildlist().size() == 2)
        {
            //buildAthematic: create two new nodes for this tree: the root (rootparent) and the foot (headsibling).
            //onto the top of the current auxiliary tree (le.getRoot())
            nrid = rootparent.getNodeID();
            footid = headsibling.getNodeID();
            //children and parents
            ArrayList<Integer> nridCs = new ArrayList<Integer>();
            for (TagNode cs : rootparent.getChildlist())
            {
                if (cs == headsibling || cs == le.getRoot())
                {
                    nridCs.add(cs.getNodeID());
                }
            }
            children.put(nrid, nridCs);
        }
//		complicated case: recognize cases where the foot node is already in the Penn Tree; the second child must then also be a modifier.
        //XP-> this-mod, some-mod, head-XP or 
        else if (headsibling.getCategory().equals(rootparent.getCategory()) && rootparent.getChildlist().size() == 3
                && rootparent.getChildlist().get(0) == le.getRoot()
                && (!rootparent.getChildlist().get(1).isArgument() && rootparent.getChildlist().get(2).isHead()))
        {
//			buildAthematic: create two new nodes for this tree: the root (rootparent) and the foot (headsibling).
            //onto the top of the current auxiliary tree (le.getRoot())
            //ASSUME RIGHT BRACKETING; I.E.: LEFTMOST MODIFIER ADJOINS ABOVE.
            nrid = rootparent.getNodeID();
            footid = headsibling.getNodeID();
            //children and parents
            ArrayList<Integer> nridCs = new ArrayList<Integer>();
            for (TagNode cs : rootparent.getChildlist())
            {
                if (cs == headsibling || cs == le.getRoot())
                {
                    nridCs.add(cs.getNodeID());
                }
            }
            children.put(nrid, nridCs);
        }//XP -> this-mod, head-XP, some-mod; (attach high, foot to right)
        else if (headsibling.getCategory().equals(rootparent.getCategory()) && rootparent.getChildlist().size() == 3
                && rootparent.getChildlist().get(0) == le.getRoot()
                && !rootparent.getChildlist().get(2).isArgument() && rootparent.getChildlist().get(1).isHead())
        {
//			buildAthematic: create two new nodes for this tree: the root (rootparent) and the foot (headsibling).
            //onto the top of the current auxiliary tree (le.getRoot())
            //ASSUME RIGHT BRACKETING; I.E.: LEFTMOST MODIFIER ADJOINS ABOVE.
            nrid = rootparent.getNodeID();
            footid = headsibling.getNodeID();
            //children and parents
            ArrayList<Integer> nridCs = new ArrayList<Integer>();
            for (TagNode cs : rootparent.getChildlist())
            {
                if (cs == headsibling || cs == le.getRoot())
                {
                    nridCs.add(cs.getNodeID());
                }
            }
            children.put(nrid, nridCs);
        }//XP-> head-XP, this-mod, some-mod (attach low, foot to left)
        else if (headsibling.getCategory().equals(rootparent.getCategory()) && rootparent.getChildlist().size() == 3
                && rootparent.getChildlist().get(1) == le.getRoot()
                && !rootparent.getChildlist().get(2).isArgument() && rootparent.getChildlist().get(0).isHead())
        {
//			buildAthematic: create two new nodes for this tree: the root (rootparent) and the foot (headsibling).
            //onto the top of the current auxiliary tree (le.getRoot())
            //ASSUME RIGHT BRACKETING; I.E.: LEFTMOST MODIFIER ADJOINS ABOVE.
            nrid = headsibling.getNodeID();
            footid = idgen.getNewId();
//			children and parents
//			overwrite children of root node; and do this in right order!!!
            ArrayList<Integer> nc = new ArrayList<Integer>();
            nc.add(footid);
            nc.add(orid);
            children.put(nrid, nc);
        }
        else
        {
            //Create two new nodes for this tree: the root (headsibling) & the foot (completely new node inheriting from headsibling).
            //onto the top of the current auxiliary tree (le.getRoot())
            //ASSUME IMPLICIT LEFT BRANCHING: adjunction to head node

            if (headRightAttachLow(rootparent, le.getRoot()))
            {
                if (headsibling.getCategory().equals(rootparent.getCategory()))
                {
                    //if (le.getRoot().getParent().getChildlist().get(0) == le.getRoot()){
                    nrid = headsibling.getNodeID();//idgen.getNewId();//
                    //System.out.println("case1");
                    footid = idgen.getNewId();//nrid+"-frl";headsibling.getNodeID();//
                }
                else
                {
                    nrid = headsibling.getParent().getNodeID();//
                    //System.out.println("case2");
                    TagNode la = lowAttachmentRight(rootparent, le.getRoot());
                    if (la != null)
                    {
                        nrid = la.getNodeID();
                        rootparent = la;
                    }
                    footid = idgen.getNewId();//nrid+"-frl";
                }

            }
            else if (headLeftAttachLow(rootparent, le.getRoot()))
            {
                if (headsibling.getCategory().equals(rootparent.getCategory()))
                {
                    nrid = headsibling.getNodeID();//
                    //System.out.println("case3");
                }
                else
                {
                    nrid = headsibling.getParent().getNodeID();
                    //System.out.println("case4");
                    TagNode la = lowAttachmentLeft(rootparent, le.getRoot());
                    if (la != null)
                    {
                        nrid = la.getNodeID();
                        rootparent = la;
                    }
                }
                footid = idgen.getNewId();//nrid+"-fll";
            }
            // adjunction to father node
            else
            {
                nrid = rootparent.getNodeID();
                footid = idgen.getNewId();
                // generating the right amount of new nodes in the case of multiple adjunction at the same node 
                // e.g. it is resilent 1) once it enters the lungs 2) , 3) with brief exposures causing...
                ///*
                //ArrayList<MyNode> rootcs = rootparent.getChildlist();
                //String rightheadid = nrid;
                // low attach if there is an argument after the modifier. 

                if (rootIsLeftOfRootparentHeadChild(rootparent, le.getRoot()))
                {
                    //System.out.println("case7");//
                }
                else
                {
                    //System.out.println("case8");
                    TagNode la = lowAttachmentLeft(rootparent, le.getRoot());
                    if (la != null)
                    {
                        nrid = la.getNodeID();
                        rootparent = la;
                    }
                }//*/
            }
//			children and parents
//			overwrite children of root node; and do this in right order!!!
            ArrayList<Integer> nc = new ArrayList<Integer>();
            if (rootIsLeftOfRootparentHeadChild(le.getRoot().getParent(), le.getRoot()))
            {
                nc.add(orid);
                nc.add(footid);
            }
            else
            {
                nc.add(footid);
                nc.add(orid);
            }
            children.put(nrid, nc);
        }
        makeStringNode(nrid, TagNodeType.internal, rootparent, le);
        //originUp.put(id, le.getFirstLeafNode().getLeafNo()+"");
        makeStringNode(footid, TagNodeType.foot, rootparent, le);

        children.put(footid, new ArrayList<Integer>());
        isHeadChild[footid] = true;
//        isHeadChild[Integer.parseInt(footid)] = true;
        parent[orid] = nrid;
//        parent[Integer.parseInt(orid)] = nrid;
        parent[footid] = nrid;
//        parent[Integer.parseInt(footid)] = nrid;
        root = nrid;
    }

    /**
     * Constructs a leaf node.
     * @param node
     */
    private void makeLeafNode(LeafNode node, IdGenerator idgen)
    {
        int leafNodeID = idgen.getNewId();
//        int leafNodeID = Integer.parseInt(leafNodeIDstring);

        ArrayList<Integer> al = new ArrayList<Integer>();
        al.add(leafNodeID);
        children.put(node.getNodeID(), al);
        if (leafNodeID >= categories.length)
        {
            makeArraysBigger(leafNodeID);
        }
        categories[leafNodeID] = node.getLeaf();
        fullcategories[leafNodeID] = node.getLeaf();
        nodeTypes[leafNodeID] = TagNodeType.terminal;
        if (origin == null)
        {
            initializeOrigin();
        }
        origin.put(leafNodeID, node.getLeafNo());
//        origin.put(leafNodeID, node.getLeafNumber());
        originUp.put(leafNodeID, node.getLeafNo());
//        originUp.put(leafNodeID, node.getLeafNumber());
        originDown.put(leafNodeID, node.getLeafNo());
//        originDown.put(leafNodeID, node.getLeafNumber());
        isHeadChild[leafNodeID] = true;
        parent[leafNodeID] = node.getNodeID();
    }

    /**
     * Constructs a completely new node (this is necessary for introducing new nodes for TAG. 
     * @param node
     * @param type
     */
    private void makeNewNode(TagNode node, TagNodeType type)
    {
        int id = node.getNodeID();
        //int id = Integer.parseInt(id);
        while (id >= categories.length)
        {
            this.makeArraysBigger(id);
        }
        List<Integer> childIDs = getIDs(node.getChildlist());
        nodeTypes[id] = type;
        categories[id] = node.getCategory();
        fullcategories[id] = node.getFullCategory();        
        
        if (!node.isLeaf())
        {
            children.put(id, (ArrayList) childIDs);
        }
        if (node.hasParent())
        {
            parent[id] = node.getParent().getNodeID();
        }
    }

    /**
     * Needed to decide how to attach the foot node.
     * @param rootparent
     * @param thistree
     * @return
     */
    private boolean headLeftAttachLow(TagNode rootparent, TagNode thistree)
    {
        TagNode child = rootparent.getHeadChild();
        while (child.getRight() != null)
        {
            child = child.getRight();
            if (child == thistree)
            {
                return true;
            }
            if (child.isArgument())
            {
                return false;
            }
        }
        return false;
    }

    /**
     * Needed to decide how to attach the foot node.
     * @param rootparent
     * @param thistree
     * @return
     */
    private boolean headRightAttachLow(TagNode rootparent, TagNode thistree)
    {
        TagNode child = rootparent.getHeadChild();
        while (child.getLeft() != null)
        {
            child = child.getLeft();
            if (child == thistree)
            {
                return true;
            }
            if (child.isArgument())
            {
                return false;
            }
        }
        return false;
    }

    /**
     * Needed to decide about configuration for foot node attachment.
     * @param rootparent
     * @param thistree
     * @return
     */
    private boolean rootIsLeftOfRootparentHeadChild(TagNode rootparent, TagNode thistree)
    {
        TagNode headchild = rootparent.getHeadChild();
        for (TagNode child : rootparent.getChildlist())
        {
            if (child == headchild)
            {
                return false;
            }
            if (child == thistree)
            {
                return true;
            }
        }
        LogInfo.error("Something wrong with rootparent's children. StringTree - rootIsLeftOfRootparentHeadChild");
        return false;
    }

    /**
     * Needed to decide about configuration for foot node attachment.
     * @param rootparent
     * @param thistree
     * @return
     */
    private TagNode lowAttachmentLeft(TagNode rootparent, TagNode thistree)
    {
        boolean seenHeadOrArg = false;
        boolean laterHeadOrArg = false;
        boolean seencurrent = false;
        TagNode lastArg = null;
        for (TagNode child : rootparent.getChildlist())
        {
            if (child.isArgument() || child.isHead())
            {
                if (seencurrent)
                {
                    laterHeadOrArg = true;
                }
                else
                {
                    seenHeadOrArg = true;
                    if (!seencurrent)
                    {
                        lastArg = child;
                    }
                }
            }
            if (child == thistree)
            {
                seencurrent = true;
            }
        }
        if (seenHeadOrArg && laterHeadOrArg)
        {
            return lastArg;
        }
        return null;
    }

    /**
     * Needed to decide about configuration for foot node attachment.
     * @param rootparent
     * @param thistree
     * @return
     */
    private TagNode lowAttachmentRight(TagNode rootparent, TagNode thistree)
    {
        boolean seenHeadOrArg = false;
        boolean laterHeadOrArg = false;
        boolean seencurrent = false;
        TagNode nextArg = null;
        for (TagNode child : rootparent.getChildlist())
        {
            if (child.isArgument() || child.isHead())
            {
                if (seencurrent)
                {
                    laterHeadOrArg = true;
                    if (nextArg == null)
                    {
                        nextArg = child;
                    }
                }
                else
                {
                    seenHeadOrArg = true;

                }
            }
            if (child == thistree)
            {
                seencurrent = true;
            }
        }
        if (seenHeadOrArg && laterHeadOrArg)
        {
            return nextArg;
        }
        return null;
    }

    /**
     * Converts a node from the LexEntry to a StringTree node and integrates it with the other nodes. 
     * @param node
     * @param unknowns
     * @param le
     * @param lexentries
     */
    private void addNodeToStringTree(TagNode node, List<TagNode> unknowns, LexEntry le, List<LexEntry> lexentries, IdGenerator idgen)
    {
        if (digits.matcher(node.getFullCategory()).matches())
        {
            String fullcat = node.getFullCategory();
            filler = fullcat.substring(fullcat.lastIndexOf("-") + 1);
        }
        if (node.isLeaf() && tracedigits.matcher(((LeafNode) node).getLeaf()).matches())
        {
            String leaftext = ((LeafNode) node).getLeaf();
            trace = leaftext.substring(leaftext.lastIndexOf("-") + 1);
            if (le.hasFatherEntry())
            {
                tracefather = le.getFatherEntry();
            }
//            anchor = ((LeafNode) node).getLeafNumber();
            anchor = ((LeafNode) node).getLeafNo();
        }
        node.setStringTree(this);
        TagNodeType type = decideTagNodeType(node, le, lexentries, idgen);
        setOrigin(node, le, type, lexentries);

        int nodeid = node.getNodeID();
//        int nodeid = Integer.parseInt(node.getNodeID());
        if (nodeid >= categories.length)
        {
            this.makeArraysBigger(nodeid);
        }
        if (node.isHead())
        {
            isHeadChild[nodeid] = true;
        }
        else
        {
            isHeadChild[nodeid] = false;
        }
        makeNewNode(node, type);
        le.addNode(node);
        holes[nodeid] = false;
        String role = node.getRole();
        boolean roleIsSet = false;
        for (TagNode child : node.getChildlist())
        {
            if (!lexentries.contains(child.getLexEntry()) && child.isArgument())
            {
                roleIsSet = makeSubstNode(child);
                 // Display the syntactic role of a PP on its' child NP substitution node (it doesn't have an annotation already)
                if(!isEmptyRole(role) && categories[node.getNodeID()].equals("PP") && categories[child.getNodeID()].equals("NP"))
                {   
                    roleIsSet = setRole(role, child); // don't overrule an existing role annotation on the NP node, and make a note of that
//                    if(!roleIsSet) 
//                    {
//                        rolesAtInternalNodes.put(nodeid, role);
//                    }
                }
            }
        }
        // annotate traces with roles
        if(!trace.equals("") || node.getCategory().equals(":"))
        {
            roleIsSet = setRole(role, node);
            // if the node doesn't contain role information, check its parent in the 
            // list of yet-unprocessed internal nodes
            if(!roleIsSet) 
            {
                Integer parentId = getParent(nodeid);
                if(parentId != null)
                {
                    String parentRole = rolesAtInternalNodes.get(parentId);
                    if(parentRole != null)
                    {
                        roleIsSet = setRole(parentRole, node);
                        if(roleIsSet)
                            rolesAtInternalNodes.remove(parentId);
                    }
                }
            }
        }
        // keep track of any remaining roles on internal nodes so that we deal with them later on foot nodes.
        // We are not interested in ARG0s, ARG1s, etc.; if they haven't been dealt with on a substitution node, then 
        // discard them, as the label will occur on the substitution node of the parental tree.
        if(!(roleIsSet || isEmptyRole(role)))// || role.matches("@ARG[0-2];")))
        {
            rolesAtInternalNodes.put(nodeid, role);
        }        
        if (isSpineConjunct(node, le))
        {//TODO what happens here for either or constructions?
            // remove all but your headchild.
            //I think that other nodes in the lexicon entry would not be affected.			
            makeSpineConjunct(node, le);
        }
    }

    private boolean setRole(String role, TagNode target)
    {
        return setRole(role, target.getNodeID());
    }
    
    private boolean setRole(String role, int targetNodeId)
    {        
        // don't replace existing annotation (e.g. if NP under PP has already an annotation from a different relation)
        if(!isEmptyRole(role) && roles[targetNodeId] == null)
        {
            roles[targetNodeId] = role;
            annotatedRoles.put(targetNodeId, role);
            return true;
        }
        return false;
    }
    
    private boolean isEmptyRole(String role)
    {        
        return role.equals("") || role.contains("rel"); // we are not interested in the actual relation annotation
    }
    
    private void setOrigin(TagNode node, LexEntry le, TagNodeType type, List<LexEntry> lexentries)
    {
        if (type == TagNodeType.internal || type == TagNodeType.terminal)
        {
            if (originNumber == Integer.MIN_VALUE)
//            if (originNumber.equals(""))
            {
                originNumber = le.getFirstLeafNode().getLeafNo();
//                originNumber = le.getFirstLeafNode().getLeafNo() + "";
            }
            if (origin == null)
            {
                this.initializeOrigin();
            }
            origin.remove(node.getNodeID());
            originDown.remove(node.getNodeID());
            originUp.remove(node.getNodeID());
            origin.put(node.getNodeID(), originNumber);// le.getFirstLeafNode().getLeafNo()+"");
            originDown.put(node.getNodeID(), originNumber);// le.getFirstLeafNode().getLeafNo()+"");
            originUp.put(node.getNodeID(), originNumber);// le.getFirstLeafNode().getLeafNo()+"");
        }
        else if (type == TagNodeType.subst || type == TagNodeType.foot)
        {
            if (originNumber == Integer.MIN_VALUE)
//            if (originNumber.equals(""))
            {
                originNumber = le.getFirstLeafNode().getLeafNo();
//                originNumber = le.getFirstLeafNode().getLeafNo() + "";
            }
            origin.remove(node.getNodeID());
            originUp.remove(node.getNodeID());
            origin.put(node.getNodeID(), originNumber);// le.getFirstLeafNode().getLeafNo()+"");
            originUp.put(node.getNodeID(), originNumber);// le.getFirstLeafNode().getLeafNo()+"");
        }
        if (le.getMultiEntry() != null)
        {        
            //if node is part of a multilex entry:
            for (LexEntry otherle : le.getMultiEntry())
            {
                if (otherle.getNodes().containsKey(node.getNodeID()))
                {
                    Integer otherOriginNumber = otherle.getMainLeafNode().getLeafNo();
//                    String otherOriginNumber = Integer.toString(otherle.getMainLeafNode().getLeafNo());
                    //if root node
                    if (otherle.getRoot() == node)
                    {
                        origin.put(node.getNodeID(), otherOriginNumber);
                        originDown.remove(node.getNodeID());
                        originDown.put(node.getNodeID(), otherOriginNumber);
                    }
                    // if substNode
                    else if (!node.isHead() && node.getLexEntry() == le)
                    {
                        origin.put(node.getNodeID(), otherOriginNumber);
                        originUp.remove(node.getNodeID());
                        originUp.put(node.getNodeID(), otherOriginNumber);
                    }
                    // assume normal internal node
                    else
                    {
                        origin.put(node.getNodeID(), otherOriginNumber);
                        originUp.remove(node.getNodeID());
                        originUp.put(node.getNodeID(), otherOriginNumber);
                        originDown.remove(node.getNodeID());
                        originDown.put(node.getNodeID(), otherOriginNumber);
                    }
                }
            }
        } // if
    }

    /**
     * Checks whether one of the the children of a node are a coordinated expression.
     * @param node
     * @param le
     * @return
     */
    private boolean isSpineConjunct(TagNode node, LexEntry le)
    {
        boolean containsCoordination = false;
        if (le.getMainLeafNode().getCategory().equals("CC")
                || (le.getMultiEntry() != null && le.getMultiEntry().get(0).getMainLeafNode().getCategory().equals("CC"))
                || le.getRootCat().equals("CONJP"))
        {
            return false;
        }
        List<TagNode> childlist = node.getChildlist();
        for (TagNode child : childlist)
        {
            if (child.getCategory().matches("C(C|ONJP)"))
            {
                containsCoordination = true;
            }
        }
        if (!containsCoordination)
        {
            return false;
        }
        return true;
    }

    /**
     * Removes from the node all children that are not the headchild and registers the node's 
     * headchild as the coordination anchor for this tree.
     * @param node
     * @param le
     */
    private void makeSpineConjunct(TagNode node, LexEntry le)
    {
        int onspineConjunct = Integer.MIN_VALUE;
        List<TagNode> childlist = node.getChildlist();
        for (TagNode child : childlist)
        {
            if (le.getNodes().containsKey(child.getNodeID()))
            {
                onspineConjunct = child.getNodeID();
            }
            else
            {
                removeNode(child.getNodeID());
//                removeNode(Integer.parseInt(child.getNodeID()));
            }
        }
        int coordinatedNode = node.getNodeID();

        //this tree: CC and other coordinates should not be part of this tree.	
        if (node.getHeadChild().getNodeID() == onspineConjunct)
//        if (node.getHeadChild().getNodeID().equals(onspineConjunct))
        {
            coordAnchorList.add(onspineConjunct);
            coordanchor = onspineConjunct;
            //System.out.println("coordanchor:"+onspineConjunct.toString());
        }

        List<Integer> nc = new ArrayList<Integer>();
        nc.add(onspineConjunct);
        children.put(coordinatedNode, (ArrayList) nc);
    }

    /**
     * Checks whether the current lexentry should be an auxiliary tree.
     * @param lexEntry
     * @param unknowns
     * @return
     */
    private boolean isRecursiveTree(LexEntry lexEntry, List<TagNode> unknowns)
    {
        TagNode root = lexEntry.getRoot();
        if (root.getCategory().equals("") && root.getNodeID() == 1)
//        if (root.getCategory().equals("") && root.getNodeID().equals("1"))
        {
            root = root.getHeadChild();
            root.setArgument();
        }
        for (TagNode node : unknowns)
        {
            if (node.getParent().getLexEntry() == lexEntry && node.getCategory().equals(root.getCategory())) // TODO
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an auxiliary tree.
     * @param lexentry
     * @param unknowns
     */
    private void createRecursiveTree(LexEntry lexentry, List<TagNode> unknowns, IdGenerator idgen)
    {
        TagNode root = lexentry.getRoot();
        if (root.getCategory().equals("") && root.getNodeID() == 1)
//        if (root.getCategory().equals("") && root.getNodeID().equals("1"))
        {
            root = root.getHeadChild();
            root.setArgument();
        }
        for (TagNode node : unknowns)
        {
            if (node.getParent().getLexEntry() == lexentry && node.getCategory().equals(root.getCategory())) // TODO
            {
                String argmod = root.getArgMod();
                if (argmod.equals("ARG"))
                {
                    node.setArgument();
                    node.getLexEntry().setSubsttree();
                }
                else
                {
                    node.setModifier();
                    node.getLexEntry().setAuxtree();
                }
                foot = idgen.getNewId();//nodeID.concat("-f");
                makeStringNode(foot, TagNodeType.foot, node, null);
                int parentID = node.getParent().getNodeID();
                parent[foot] = parentID;
//                parent[Integer.parseInt(foot)] = parentID;
                ArrayList<Integer> siblings = children.get(parentID);
                siblings.add(foot);
                children.put(parentID, siblings);
                children.put(foot, new ArrayList<Integer>());
                auxtree = true;
            }
        }
    }

    /**
     * Determines the id for a list of nodes.
     * @param childlist
     * @return
     */
    private List<Integer> getIDs(List<TagNode> childlist)
    {
        List<Integer> list = new ArrayList<Integer>();
        for (TagNode childnode : childlist)
        {

            if (childnode.isHead() || childnode.isArgument()
                    || (childnode.getArgMod().equals("UNDEF") && childnode.getNodeID() < categories.length
                    && categories[childnode.getNodeID()] != null))
//            if (childnode.isHead() || childnode.isArgument()
//                    || (childnode.getArgMod().equals("UNDEF") && Integer.parseInt(childnode.getNodeID()) < categories.length
//                    && categories[Integer.parseInt(childnode.getNodeID())] != null))
            {                
                list.add(childnode.getNodeID());
            }
        }
        return list;
    }

    /**
     * Decides the node type for a given node in the context of its lexicon entry.
     * @param node
     * @param le
     * @param lexentries
     * @return
     */
    private TagNodeType decideTagNodeType(TagNode node, LexEntry le, List<LexEntry> lexentries, IdGenerator idgen)
    {
        TagNodeType type;
        /*if(node == le.getRoot()&& !le.isPartOfMultiLex()){
        type = TagNodeType.anchor;
        if (node.isLeaf()){
        makeLeafNode((LeafNode) node);
        }
        }
        else*/
        if (!node.isHead() && node != le.getRoot() && !lexentries.contains(node.getLexEntry()))
        {
            type = TagNodeType.subst;
        }
        else if (!node.isLeaf() && node.getCategory().equals(le.getRootCat()) && node.getChildlist().isEmpty())
        {
            //should not be needed in current implementation state.
            type = TagNodeType.foot;
        }
        else if (node.isLeaf())
        {
            type = TagNodeType.internal;
            makeLeafNode((LeafNode) node, idgen);
        }
        else
        {
            type = TagNodeType.internal;
        }
        return type;
    }

    public int getAnchor()
    {
        return anchor;
    }

    public String getFiller()
    {
        return filler;
    }

    public String getTrace()
    {
        return trace;
    }

    public LexEntry getTraceFather()
    {
        return tracefather;
    }

    public void setMCTAG(StringTree othertree)
    {
        MCrest = othertree;
    }

    public StringTree getMcTag()
    {
        return MCrest;
    }
   
    /**
     * 
     * @param le Lexicon Entry that's a coordinator.
     */
    private void makeCoordination(LexEntry le)
    {
        //default:
        TagNode cc = le.getMainLeafNode();
        if (le.getRootCat().equals("CONJP"))
        {
            cc = le.getRoot();
        }
        int ccId = cc.getNodeID();
        TagNode rootNode = cc.getLexEntry().getRoot().getParent();
        TagNode footNode = determineCoordFootNode(rootNode);
        if (footNode == null)
        {
            LogInfo.error("Could not find valid foot node. ElementaryStringTree -  makeCoordination");
        }
        int rootNodeId = rootNode.getNodeID();
        int footNodeId = footNode.getNodeID();
        makeStringNode(rootNodeId, TagNodeType.internal, rootNode, le);
        makeStringNode(footNodeId, TagNodeType.foot, footNode, le);
//		parent changes
        parent[footNodeId] = rootNodeId;
//        parent[Integer.parseInt(footNodeId)] = rootNodeId;
        parent[ccId] = rootNodeId;
//        parent[Integer.parseInt(ccId)] = rootNodeId;
        //for all remaining children:
//		update children
        ArrayList<Integer> rootChildren = new ArrayList<Integer>();
        ArrayList<TagNode> remainingChildren = new ArrayList<TagNode>();
        remainingChildren.addAll(rootNode.getChildlist());
        Integer originid = le.getFirstLeafNode().getLeafNo();
//        String originid = le.getFirstLeafNode().getLeafNumber();
        boolean childAfterCoordinator = false;
        for (TagNode substNode : remainingChildren)
        {
            int substNodeId = substNode.getNodeID();
            if (substNode.getCategory().equals(",")
                    || (substNode.getCategory().equals(":") && substNode.isLeaf()
                    && ((LeafNode) substNode).getLeaf().equals(";"))
                    || (substNode.getCategory().equals(":") && substNode.isLeaf()
                    && ((LeafNode) substNode).getLeaf().equals("--")))
            {
                if (!childAfterCoordinator)
                {
                    rootChildren.add(footNodeId);
                }
                childAfterCoordinator = true;
            }
            if (substNode == cc)
            {
                if (!childAfterCoordinator)
                {
                    rootChildren.add(footNodeId);
                }
                rootChildren.add(ccId);
                substNode.makeHead();
                isHeadChild[ccId] = true;
//                isHeadChild[Integer.parseInt(ccId)] = true;
                childAfterCoordinator = true;
            }
            else if (childAfterCoordinator)
            {
                makeStringNode(substNodeId, TagNodeType.subst, substNode, null);
                originUp.put(substNodeId, originid);
                origin.put(substNodeId, originid);
                parent[substNodeId] = rootNodeId;
//                parent[Integer.parseInt(substNodeId)] = rootNodeId;
                children.put(substNodeId, new ArrayList<Integer>());
                substNode.getLexEntry().setSubsttree();
                rootChildren.add(substNodeId);
                substNode.makeNotHead();
            }
            else if (!childAfterCoordinator)
            {
                if (substNode.getCategory().equals("''") || substNode.getCategory().equals("``"))
                {
                    rootChildren.add(substNodeId);
                    makeStringNode(substNodeId, TagNodeType.subst, substNode, null);
                    originUp.put(substNodeId, originid);
                    origin.put(substNodeId, originid);
                }
            }
        } // for each subst node
        children.put(footNodeId, new ArrayList<Integer>());
        children.put(rootNodeId, rootChildren);// here wrong children (23, 23, 24, 25); 23,23 zeigen auf gleiches object.
        auxtree = true;
        root = rootNodeId;
        originDown.put(root, originid);
        origin.put(root, originid);
        originUp.put(foot, originid);
    }

    /**
     * Sets the properties for a new node.
     * @param type
     * @param node
     */
    private void makeStringNode(int id, TagNodeType type, TagNode node, LexEntry le)
    {

//        int id = Integer.parseInt(id);
        if (id >= categories.length)
        {
            this.makeArraysBigger(id);
        }
        categories[id] = node.getCategory();
//        if(useSemantics)
//            fullcategories[id] = node.getFullCategory() + node.getRole();
//        else
            fullcategories[id] = node.getFullCategory();
        nodeTypes[id] = type;
        isHeadChild[id] = node.isHead();
        /*if (le != null){
        origin.put(id, le.getFirstLeafNode().getLeafNumber());
        originUp.put(id, le.getFirstLeafNode().getLeafNumber());
        originDown.put(id, le.getFirstLeafNode().getLeafNumber());
        }*/
        if (type == TagNodeType.internal)
        {
            holes[id] = false;
            if (le != null)
            {
                origin.put(id, originNumber);
                if (id != root)
//                if (!id.equals(root))
                {
                    originUp.put(id, originNumber);
                }
                originDown.put(id, originNumber);
            }
        }
        else if (type == TagNodeType.foot)
        {
            isHeadChild[id] = false;
            holes[id] = true;
            foot = id;
            if (le != null)
            {
                origin.put(id, originNumber);
                originUp.put(id, originNumber);
            }
        }
        else if (type == TagNodeType.subst)
        {
            isHeadChild[id] = false;
            if (le != null)
            {
                origin.put(id, originNumber);
                originUp.put(id, originNumber);                
            }
            holes[id] = true;            
            setRole(node.getRole(), node);
        }
        else
        {
            if (le != null)
            {
                origin.put(id, originNumber);
                if (id != root)
//                if (!id.equals(root))
                {
                    originUp.put(id, originNumber);
                }
                originDown.put(id, originNumber);
            }
        }
    }

    /**
     * 
     * @param rootNode
     * @return
     */
    private TagNode determineCoordFootNode(TagNode rootNode)
    {
        if (rootNode.getCategory().equals("UCP"))
        {
            for (TagNode child : rootNode.getChildlist())
            {
                if (child.getRight() != null && child.getRight().getCategory().matches(":|,|(CC)|(CONJP)"))
                {
                    return child;
                }
                else
                {
                    LogInfo.error("not first child");
                }
            }
        }
        else
        {
            String cattype = rootNode.getCategory().substring(0, 1);
            for (TagNode child : rootNode.getChildlist())
            {
                if (child.getCategory().startsWith(cattype)
                        && child.getRight() != null
                        && child.getRight().getCategory().matches("(,|(CC)|(CONJP)|(PRN))"))
                {
                    return child;
                }
                if (child.getCategory().matches("(,|(CC)|(CONJP))") && !rootNode.getChildlist().get(0).getCategory().matches("(,|(CC)|(CONJP))"))
                {
                    return rootNode.getChildlist().get(0);
                }
            }
        }
        return null;
    }

    private boolean siblingsCoord(List<TagNode> siblings)
    {
        for (TagNode sibling : siblings)
        {
            if (sibling.getCategory().equals("CC") || sibling.getCategory().equals("CONJP"))
            {
                return true;
            }
        }
        return false;
//        boolean containsCoordination = false;
//        for (TagNode sibling : siblings)
//        {
//            if (sibling.getCategory().equals("CC") || sibling.getCategory().equals("CONJP"))
//            {
//                containsCoordination = true;
//            }
//        }
//        return containsCoordination;
    }

    /**
     * 
     * @param le
     * @return
     */
    private boolean isOtherConjunct(LexEntry le)
    {
        TagNode coordFather = le.getRoot().getParent();
        if (coordFather == null)
        {
            return false;
        }
        List<TagNode> siblings = coordFather.getChildlist();
        boolean containsCoordination = siblingsCoord(siblings);
        // if non of the siblings is a CC node, decide this is not a conjunct part.
        if (!containsCoordination)
        {
            return false;
        }
        // if this is a comma in coordination
        if (containsCoordination
                && (le.getRootCat().equals(",") || le.getRootCat().equals("``") || le.getRootCat().equals("''")
                || (le.getRootCat().equals(":") && le.getMainLeafNode().getLeaf().equals(";"))))
        {
            auxtree = false;
            //le.getRoot().setArgMod("ARG");
            //le.setSubsttree();
            ArrayList<TagNode> remainingChildren = new ArrayList<TagNode>();
            remainingChildren.addAll(le.getRoot().getParent().getChildlist());
            remainingChildren.remove(0);//footnode
            for (TagNode substNode : remainingChildren)
            {
                if (!substNode.getCategory().matches("C(C|ONJP)"))
                {
                    substNode.getLexEntry().setSubsttree();
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Transforms coordinated phrases to match the incremental target structure.
     * @param le
     * @param idgen 
     * @return
     */
    private void makeOtherConjunct(LexEntry le, IdGenerator idgen)
    {
        TagNode coordfather = le.getRoot().getParent();
        // transform the rest of the coordinated phrases to match the incremental target structure.
        LexEntry toplexentry = coordfather.getLexEntry();
        //MyNode newRootForOldCoordHeadChild= coordfather.getHeadChild();
        if (toplexentry == null)
        {
            LogInfo.error("problem at makeOtherConjunct");
        }
        Set topNodeIds = toplexentry.getNodes().keySet();
        TagNode p = coordfather; // do not include the coordination father node, because this should only be introduced 
        // by the conjunction.
        int prevId = le.getRoot().getNodeID();
        ArrayList<LexEntry> lelist = new ArrayList<LexEntry>();
        lelist.add(toplexentry);
        TagNodeType[] oldNodeTypes = nodeTypes.clone();

        // collect all nodes on top of the coordination that belong to this tree.
        HashMap<Integer, ArrayList<Integer>> oldchildren = new HashMap<Integer, ArrayList<Integer>>();
        oldchildren.putAll(children);
        while (topNodeIds != null && p != null && topNodeIds.contains(p.getNodeID()))
        {// && !siblingsCoord(p.getParent().getChildlist())){
            addNodeToStringTree(p, new ArrayList<TagNode>(), le, lelist, idgen);// toplexentry, lelist );
            prevId = p.getNodeID();
            p = p.getParent();
        }
        // integrate these nodes into the tree
        for (int i = 0; i < oldNodeTypes.length; i++)
        {
            if (oldNodeTypes[i] != null)
            {
                nodeTypes[i] = oldNodeTypes[i];
            }
        }

        children.putAll(oldchildren);
        if (coordanchor != le.getRoot().getNodeID())
//        if (!coordanchor.equals(le.getRoot().getNodeID()))
        {
            coordAnchorList.add(le.getRoot().getNodeID());
            coordanchor = le.getRoot().getNodeID();
        }
        //auxtree = false; WHY AUXTREE = false? first conjunct can be modifier!
        //connect top bit of tree to bottom bit
        if (coordfather.getLexEntry() == coordfather.getParent().getLexEntry())
        {//
            parent[le.getRoot().getNodeID()] = coordfather.getNodeID(); //skip the coordination node
//            parent[Integer.parseInt(le.getRoot().getNodeID())] = coordfather.getNodeID(); //skip the coordination node
            ArrayList<Integer> cl = new ArrayList<Integer>();
            cl.add(le.getRoot().getNodeID());
            children.put(coordfather.getNodeID(), cl);
        }
        root = prevId;
        parent[root] = null;
//        parent[Integer.parseInt(root)] = null;
        if (le.isAuxtree() != null && le.isAuxtree())
        {
            if (coordfather.getLexEntry().isAuxtree() != null && !coordfather.getLexEntry().isAuxtree())
            {
                le.setSubsttree();
                auxtree = false;
            }
            else if (coordfather.getLexEntry().getRoot().getParent() != null)
            {
                makeFootNode(coordfather, idgen);
                auxtree = true;

                //originUp.put(coordfather, originNumber);
            }
        }
        else if (le.isAuxtree() == null)
        {
            if (!coordfather.getLexEntry().getMainLeafNode().getArgMod().equals("UNDEF")
                    && coordfather.getLexEntry().isAuxtree() != null && !coordfather.getLexEntry().isAuxtree())
            {
                auxtree = false;
                le.setSubsttree();
            }
            else if (!coordfather.getLexEntry().getRoot().getArgMod().equals("UNDEF")
                    && coordfather.getLexEntry().isAuxtree())
            {
                le.setAuxtree();
                auxtree = true;
                if (!coordfather.getCategory().equals(""))
                {
                    makeFootNode(coordfather, idgen);
                    originDown.put(root, originNumber);
                    originUp.put(foot, originNumber);
                }
            }
        }
        if (le.getRoot() != coordfather.getLexEntry().getRoot())
        {
            le.setRoot(coordfather.getLexEntry().getRoot());
            if (isOtherConjunct(le))
            {
                makeOtherConjunct(le, idgen);
            }
        }
    }

    public void addChild(int parentnodeId, int nodeId)
    {
        ArrayList<Integer> clist = children.get(parentnodeId);
        if (clist == null)
        {
            clist = new ArrayList<Integer>();
        }
        clist.add(nodeId);
        children.put(parentnodeId, clist);
    }

    public void setTreeString(String s)
    {
        treeParsingString = s;
    }

    public String getTreeString()
    {
        return treeParsingString;
    }

    public void setFootNode(int parentnodeId)
    {
        foot = parentnodeId;
        setAuxTree();
    }

    public int getOriginNumber()
    {
        return originNumber;
    }

    public int getPseudoAnchorNumber()
    {
        return pseudoAnchorNumber;
    }

    public String getIndices(int childid, String child)
    {
        if (!child.contains("_"))
        {
            return child;
        }
        String down = child.substring(child.indexOf("_") + 1);
        child = child.substring(0, child.indexOf("_"));
        if (down.equals("null"))
        {
            indexDown[childid] = -1;
        }
        else if (down.equals("x"))
        {
            indexDown[childid] = 0;
        }
        else
        {
            indexDown[childid] = Byte.parseByte(down);
        }
        String up = child.substring(child.indexOf("^") + 1);
        child = child.substring(0, child.indexOf("^"));
        if (up.equals("null"))
        {
            indexUp[childid] = -1;
        }
        else if (up.equals("x"))
        {
            indexUp[childid] = 0;
        }
        else
        {
            indexUp[childid] = Byte.parseByte(up);
        }
        if (up.equals("null") && down.equals("null"))
        {            
//            LogInfo.error("both indices null at category " + child);
            return null;
        }
        return child;
    }

    public void annotateHeadStatus()
    {
        int node = anchor;
        //int node = Integer.parseInt(node);
        while (parent[node] != null)
        {
            isHeadChild[node] = true;
            node = parent[node];
//            node = Integer.parseInt(parent[node]);
        }
        for (int i = 0; i < categories.length; i++)
        {
            if (categories[i] != null && isHeadChild[i] == null)
            {
                isHeadChild[i] = false;
            }
        }
    }

    public void setAnchor(int childId)
    {
        anchor = childId;
    }

    public void findChoppedSpine()
    {
        byte rootind = this.getLowerIndex(root);
//        byte rootind = this.getLowerIndex(Integer.parseInt(root));
        for (int i = 0; i < categories.length; i++)
        {
            //String i = Integer.toString(i);
            if (categories[i] != null && (!children.containsKey(i) || children.get(i).isEmpty()))
//            if (categories[i] != null && (!children.containsKey(i) || children.get(i + "").isEmpty()))
            {
                children.put(i, new ArrayList<Integer>());
                if (nodeTypes[i] == TagNodeType.internal && indexDown[i] == rootind)
                {
                    anchor = i;
                }
            }
        }
        if (anchor == Integer.MIN_VALUE)
//        if (anchor.equals(""))
        {
            LogInfo.error("Bad tree: " + print());
        }
    }

    public void setProbability(double d)
    {
        this.probability = d;
    }

    public boolean isVerificTree()
    {
        return this.verificTree;
    }
    //public void setTuple(String string) {
    //	tuple = string;
    //}

    public boolean hasShadowInd()
    {
        for (byte s : this.indexDown)
        {
            if (s > 0)
            {
                return true;
            }
        }
        return false;
    }

    public String getHeadAnnotation(int parseInt, String child)
    {
        if (child.length() < 1)
        {
            return child;
        }
        String headAnnot = child.substring(child.length() - 1, child.length());
        if (headAnnot.equals("+"))
        {
            this.isHeadChild[parseInt] = Boolean.TRUE;
        }
        else if (headAnnot.equals("-"))
        {
            this.isHeadChild[parseInt] = Boolean.FALSE;
        }
        else
        {
            return child;
        }
        return child.substring(0, child.length() - 1);
    }

    public void simplifyCats()
    {
        for (int i = 0; i < categories.length; i++)
        {
            String cat = categories[i];
            if (cat != null)
            {
                /*if (cat.startsWith("VB")){
                categories[i] = "VB";
                }
                else if (cat.startsWith("NN"))
                categories[i]="NN";
                else*/ if (cat.equals("JJP"))
                {
                    categories[i] = "ADJP";
                }
                else if (cat.startsWith("JJ"))
                {
                    categories[i] = "JJ";
                }
                else if (cat.startsWith("RB"))
                {
                    categories[i] = "RB";
                }
                else if (cat.equals("NX"))
                {
                    categories[i] = "NP";
                }
                else if (cat.equals("NAC"))
                {
                    categories[i] = "NP";
                }
                else if (cat.equals("NML"))
                {
                    categories[i] = "NP";
                }
                //else if (cat.endsWith("$")&&cat.length()>1)
                //	categories[i]=cat.substring(0, cat.indexOf("$"));
                else if (cat.equals("PRP$"))
                {
                    categories[i] = "DT";
                }
                else if (cat.contains("="))
                {
                    categories[i] = cat.substring(0, cat.indexOf("="));
                }
                else if (cat.contains("|"))
                {
                    categories[i] = cat.substring(0, cat.indexOf("|"));
                }
            }
        }
    }

    public String getPredictedLex()
    {
        for (int i = anchor + 1; i < categories.length; i++)
//        for (int i = Integer.parseInt(anchor) + 1; i < categories.length; i++)
        {
            String cat = categories[i];
            if (cat == null)
            {
                return null;
            }
            if (cat.length() > 1 && Character.isLowerCase(cat.charAt(1)) && !TreeState.isNullLexeme(cat))
            {
                return cat;
            }
            else if (cat.length() == 1 && Character.isLowerCase(cat.charAt(0)) && !TreeState.isNullLexeme(cat))
            {
                return cat;
            }
        }
        return null;
    }

    /*
    public void setAdjoinable(String childId, boolean b) {
    this.adjPossible[Integer.parseInt(childId)] = b;		
    }
     */       
    
    private void cleanupRolesOnInternalNodes()
    {
        for(int nodeid : rolesAtInternalNodes.keySet())
        {
            roles[nodeid] = null;
            annotatedRoles.remove(nodeid);
        }
    }
    
    public void setSemanticFrame(String frameno)
    {
        this.semanticFrame = frameno;
    }

    public String getSemanticFrame()
    {
        return semanticFrame == null ? "" : semanticFrame;
    }

    public Set<RoleSignature> getRoleSignatures()
    {
        return roleSignatures;
    }
        
    public boolean hasRoles()
    {
        return roleSignatures.size() == 1 ? !roleSignatures.iterator().next().isEmpty() : roleSignatures.size() > 0;        
    }

    public boolean isRelation()
    {
        return isRelation;
    }        

    public int getSubcategorisedAnchor()
    {
        return subcategorisedAnchor;
    }

    public void setSubcategorisedAnchor(int subcategorisedAnchor)
    {
        this.subcategorisedAnchor = subcategorisedAnchor;
    }        
}
