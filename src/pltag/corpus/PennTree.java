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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PennTree
{

    private TagNode rootnode;
    private Map<String, Trace> tracelist = new HashMap<String, Trace>();
    private String currentstring;
    private String sourceString;
    private ArrayList<LeafNode> leaflist;
    private IdGenerator idgen = new IdGenerator();
    private boolean verbose = false;//true;

    public PennTree(String sentence)
    {
        // TODO Auto-generated constructor stub		
        leaflist = new ArrayList<LeafNode>();
        currentstring = sentence;
        sourceString = sentence;
        //System.out.println(sentence);

        rootnode = parseString();
        //System.out.println(sentence);
    }

    private TagNode parseString()
    {
        //System.out.println(currentstring);
        String s = currentstring.trim();

        //int length = s.length(); 
        int index = 1;
        boolean notEnd = true;
        String category;
        TagNode thisnode;
        while (s.charAt(index) != '(' & notEnd)
        {//as long as no subcategory started
            if (s.charAt(index) == ')')
            {//leaf
                notEnd = false;
                String catleaf = s.substring(1, index);
                catleaf = catleaf.trim();
                category = catleaf.substring(0, catleaf.indexOf(" "));

                String leaf = catleaf.substring(catleaf.indexOf(" ") + 1, catleaf.length());
                if (category.matches("^-[A-Z]+-$"))
                {
                    category = ":";
                }
                thisnode = new LeafNode(idgen, category, tracelist, leaf);
                currentstring = s.substring(index + 1);
                ((LeafNode) thisnode).setLeafNumber(leaflist.size());//size should be the same as the position of the new entry.
                leaflist.add((LeafNode) thisnode);
                return thisnode;
            }
            else
            {
                index++;
            }
        }

        category = (s.substring(1, index)).trim();//because always start after opening bracket
        thisnode = new InnerNode(idgen, category, tracelist);
        String srest = s.substring(index, s.length());
        currentstring = srest;
        currentstring = currentstring.trim();
        TagNode lastchild = null;
        while (!currentstring.startsWith(")"))
        {
            TagNode child = parseString();
            child.setParent(thisnode);
            thisnode.addChild(child);
            if (lastchild != null)
            {
                child.setLeftsib(lastchild);
                lastchild.setRightsib(child);
            }
            lastchild = child;
            currentstring = currentstring.trim();
        }
        if (currentstring.startsWith(")"))
        {
            currentstring = currentstring.substring(1);
        }
        //change VB.? tags of auxiliaries to AUX
        thisnode.toAux();
        //thisnode.toCopula();
        return thisnode;
    }

    public TagNode getRoot()
    {
        return rootnode;
    }

    public String getString()
    {
        return sourceString;
    }

    public ArrayList<LeafNode> getLeaflist()
    {
        return leaflist;
    }

    public void printleaves()
    {
        for (LeafNode ln : leaflist)
        {
            System.out.print(ln.getLeaf());
            System.out.print(" ");
        }
        System.out.println("");
    }

    public void setRoot(TagNode node)
    {
        rootnode = node;
    }

    public IdGenerator getRandomGenerator()
    {
        return idgen;
    }

    /**
     * Left branching nodes have been annotated with NML tags in Curran and Vadas. Inconsistently, right
     * branching was still left implicit. Here we insert right branching as explicit nodes as well.
     * However, this doesn't mean that everything becomes binary, since this only applies to NPs. 
     * Note: this may mean that we destroy some of the domain of locality for NPs, right???
     * Should add distinction between modifiers and arguments!
     * @param node
     */
    public void insertExplicitRightBranching(TagNode node, PercolTable pt)
    {
        if (node.getCategory().matches("N.*")
                && node.getChildlist().size() > 2
                && node.getChildlist().get(node.getChildlist().size() - 1).isHead())
        {
            addNodes(node);
        }
        if (node.getCategory().matches("QP")
                && node.getChildlist().size() > 2)
        {
            addNodes(node);
        }
        if (numberOfCC(node.getChildlist()) >= 2)
        {
            TagNode lastCC = node.getChildlist().get(node.getChildlist().size() - 1);
            while (!lastCC.getCategory().equals("CC"))
            {
                lastCC = lastCC.getLeft();
            }

            TagNode newNode = insertNodeforPQ(node.getChildlist().get(0), lastCC.getLeft(), node, node.getCategory(), pt);

            TagNode deleteNode = lastCC.getLeft();
            while (deleteNode != null)
            {
                node.removeChild(deleteNode);
                deleteNode = deleteNode.getLeft();
            }
            ArrayList<TagNode> restNodes = new ArrayList<TagNode>();
            restNodes.addAll(node.getChildlist());
            while (node.getChildlist().size() > 0)
            {
                node.removeChild(node.getChildlist().get(0));
            }
            node.addChild(newNode);
            for (TagNode n : restNodes)
            {
                node.addChild(n);
            }
            lastCC.setLeftsib(newNode);
            newNode.setRightsib(lastCC);
            newNode.setParent(node);
            node.setHeadChild(lastCC.getRight());
            lastCC.getRight().makeHead();
            newNode.makeNotHead();
            //deal with comma which may precede and / but / or

            pt.findHeadCategory(newNode);
        }
        for (TagNode child : node.getChildlist())
        {
            if (!child.isLeaf())
            {
                insertExplicitRightBranching(child, pt);
            }
        }
    }

    /**
     * Determines how many CC nodes are among the childlist, not counting any CC nodes that are phrase-initial
     * and should therefore be treated as sentence-initial modifiers.
     * 
     * @param childlist
     * @return
     */
    public static int numberOfCC(List<TagNode> childlist)
    {
        int noOfCC = 0;
        boolean seenRealNode = false;
        for (TagNode node : childlist)
        {
            if (seenRealNode && node.getCategory().matches("C(C|ONJP)"))
            {
                noOfCC++;
            }
            if (node.getCategory().matches("[A-Z]+") && !node.getCategory().matches("C(C|ONJP)"))
            {
                seenRealNode = true;
            }
        }
        return noOfCC;
    }

    /**
     * Insert additional QP nodes for coordinated QP expressions. 
     */
    public void insertQPtreatment(TagNode node, PercolTable pt)
    {
        List<TagNode> childlist = node.getChildlist();
        int listSize = childlist.size();
        if (node.getCategory().matches("QP") && listSize > 3)
        {
            for (int i = 0; i < listSize; i++)
            {
                TagNode child = childlist.get(i);
                if (child.getCategory().equals("CC"))
                {
                    TagNode firstNodeOfConj = childlist.get(0);
                    TagNode lastNodeOfConj = childlist.get(childlist.size() - 1);
                    if (i >= 1)
                    {
                        firstNodeOfConj = insertNodeforPQ(firstNodeOfConj, childlist.get(i - 1), node, "QP", pt);
                    }
                    if (i < listSize - 1)
                    {
                        lastNodeOfConj = insertNodeforPQ(childlist.get(i + 1), lastNodeOfConj, node, "QP", pt);
                    }
                    while (node.getChildlist().size() > 0)
                    {
                        node.removeChild(node.getChildlist().get(0));
                    }
                    node.addChild(firstNodeOfConj);
                    node.addChild(child);
                    node.addChild(lastNodeOfConj);
                    firstNodeOfConj.setRightsib(child);
                    child.setRightsib(lastNodeOfConj);
                    child.setLeftsib(firstNodeOfConj);
                    lastNodeOfConj.setLeftsib(child);
                    firstNodeOfConj.setParent(node);
                    lastNodeOfConj.setParent(node);
                    node.setHeadChild(firstNodeOfConj);
                    firstNodeOfConj.makeHead();
                    pt.findHeadCategory(node);
                    return;
                }
            }
        }
        // assume maximally one level of QPs, so don't search below one a QP was found.
        else
        {
            for (TagNode child : node.getChildlist())
            {
                if (!child.isLeaf())
                {
                    insertQPtreatment(child, pt);
                }
            }
        }
    }

    private TagNode insertNodeforPQ(TagNode firstNodeOfNew, TagNode lastNodeOfNew, TagNode parent, String category, PercolTable pt)
    {
        TagNode newNode = new InnerNode(idgen, category, new HashMap<String, Trace>());
        TagNode currentNode = firstNodeOfNew;
        //set correct parents and siblings
        currentNode.setLeftsib(null);
        while (currentNode != lastNodeOfNew)
        {
            if (currentNode == null)
            {
                System.out.println(sourceString);
            }
            newNode.addChild(currentNode);
            currentNode.setParent(newNode);
            currentNode = currentNode.getRight();
        }
        newNode.addChild(currentNode);
        currentNode.setRightsib(null);
        currentNode.setParent(newNode);
        pt.findHeadCategory(newNode);

        return newNode;
    }

    private void addNodes(TagNode node)
    {
        for (TagNode child : node.getChildlist())
        {
            // don't deal with coordination here.
            if (child.getCategory().matches("C(C|ONJP)"))
            {
                return;
            }
        }
        while (node.getChildlist().size() > 2)
        {
            TagNode newNode = new InnerNode(idgen, node.getHeadChild().getCategory(), new HashMap<String, Trace>()); // tracelist instead of new HashMap?
            TagNode lastnode = node.getChildlist().remove(node.getChildlist().size() - 1);
            TagNode secondlast = node.getChildlist().remove(node.getChildlist().size() - 1);
            TagNode thirdlast = node.getChildlist().get(node.getChildlist().size() - 1);
            thirdlast.setRightsib(newNode);
            newNode.setLeftsib(thirdlast);
            secondlast.setLeftsib(null);
            node.getChildlist().add(newNode);
            newNode.addChild(secondlast);
            newNode.addChild(lastnode);
            lastnode.setParent(newNode);
            secondlast.setParent(newNode);
            newNode.setParent(node);
            if (node.getHeadChild() == lastnode)
            {
                newNode.setHeadChild(lastnode);
                node.setHeadChild(newNode);
                newNode.makeHead();
            }
            else if (node.getHeadChild() == secondlast)
            {
                newNode.setHeadChild(secondlast);
                node.setHeadChild(newNode);
                newNode.makeHead();
            }
            else
            {
                newNode.makeNotHead();
                newNode.setHeadChild(lastnode);
                lastnode.makeHead();
            }
            if (newNode.getHeadChild() != null && newNode.getHeadChild().isArgument())
            {
                newNode.setArgument();
            }
        }
    }

    /**
     * Cuts the PennTree up into lexicon entries.
     * Initiates the insertion of additional nodes for flat coordination.
     * 
     * @param leaf
     * @param morethanonefoot, justMultiWordLexemes (conditions on how large to make the lexicon entries)
     * @return a lexicon entry that belongs to the leaf node
     */
    LexEntry determineTree(LeafNode leaf, boolean moreThanOneFoot, boolean justMultiWordLexemes)
    {
        int wid = leaf.getLeafNo();
        LexEntry entry = new LexEntry((LeafNode) leaf);
        //move up the chain until not head any more
        leaf.setLexEntry(entry);
        TagNode node = leaf;
        while (node.isHead())
        {
            //adds current node
            entry.addNode(node);
            node.setLexEntry(entry);            
            node = node.getParent();
        }
//		check whether tree is arg or mod = auxiliary or substitution tree
        entry.addNode(node);
        node.setLexEntry(entry);
        entry.setRoot(node);
        if (node.getArgMod().equals("ARG"))
        {
            entry.setSubsttree();
        }
        else if (node.getArgMod().equals("MOD"))
        {
            entry.setAuxtree();
        }
        else if (node.getArgMod().equals("UNDEF"))
        {
            entry.guessArgMod();
//            if (!entry.guessArgMod())
//            {
//                //lexical foot may be head of something else and defined for that! but undefined for current dependency!
//            }
        }

        /* adds its supportnodes, and traces TODO! TRACES!
         * gaps: not sure if these should really be in the same tree, probably not(they are things 
         * like "proposition" as an argument, which itself is fully compositional, or things like 
         * "American Express") combine support nodes at the end, when tree for supportnode has already been generated
         */
        if (moreThanOneFoot || justMultiWordLexemes)
        {
            Map supportNodes = null;
            if (moreThanOneFoot)
            {
                supportNodes = leaf.getSupportNodes();
            }
            else if (justMultiWordLexemes)
            {
                supportNodes = leaf.getLexEntryRest();
            }
            Iterator jj = supportNodes.keySet().iterator();
            while (jj.hasNext())
            {
                String key = (String) jj.next();
                LexEntry le = ((LeafNode) supportNodes.get(key)).getLexEntry();
                if ((new Integer(key)).intValue() < wid && !le.isPartOfMultiLex())
                {
                    // this is used for predictive lexicon entries, like either-or, both-and etc. 
                    le.mergeLexentries(entry);//don't destruct entry; get expression correct; shadow status only in Stringtree???
                    //entry.mergeLexentries(le);
                    TagNode rootnode = entry.getRoot();
                    if (rootnode.getArgMod().equals("ARG"))
                    {
                        entry.setSubsttree();
                    }
                    else if (rootnode.getArgMod().equals("MOD"))
                    {
                        entry.setAuxtree();
                    }
                    else if (rootnode.getArgMod().equals("UNDEF"))
                    {
                        if (!entry.guessArgMod())
                        {
                            //lexical foot may be head of something else and defined for that! but undefined for current dependency!
                        }
                    }
                    //fixCoordinationNodes(le);
                }
            }
        }

        fixCoordinationNodes(entry);
        //mountForModifiers(entry);
        return entry;
    }

    /**	
     * the following is a fix for coordination nodes which are too flat and have additional stuff in them which is
     * 	not part of the coordintated expression.
     * 	@param entry
     */
    private void fixCoordinationNodes(LexEntry entry)
    {
        for (TagNode n : entry.getNodes().values())
        {
            if (n.getCategory().equals("CC") && !n.getLexEntry().isPartOfMultiLex())
            {
                TagNode fathernode = n.getParent();
                if (fathernode.getChildlist().get(0).getCategory().equals(","))
                {
                    TagNode lastchild = fathernode.getChildlist().get(fathernode.getChildlist().size() - 1);
                    while (!lastchild.getChildlist().isEmpty())
                    {
                        lastchild = lastchild.getChildlist().get(lastchild.getChildlist().size() - 1);
                    }
                    TagNode nextLeaf = null;
                    int startleafno = ((LeafNode) lastchild).getLeafNo();
                    if (startleafno >= leaflist.size() - 1)
                    {
                        startleafno = leaflist.size() - 1;
                    }
                    for (int i = startleafno; i >= 0; i--)
                    {
                        LeafNode ln = this.leaflist.get(i);
                        if (ln == lastchild && leaflist.size() > i + 1)
                        {
                            nextLeaf = leaflist.get(i + 1);
                            break;
                        }
                    }
                    if (nextLeaf == null || nextLeaf.getCategory().equals(","))
                    {
                        // insert apposition
                    }
                    else
                    {
                        // modifier comma???
                    }
                }
                if (fathernode.getCategory().equals("UCP"))
                {
                }
                else
                {
                    TagNode childNode = fathernode.getChildlist().get(0);
                    while (childNode != null)
                    {
                        fathernode = childNode.getParent();
                        String fatherCat = fathernode.getCategory();
                        String childCat = childNode.getCategory();
                        String firstChildCat = fathernode.getChildlist().get(0).getCategory();
                        String lastChildCat = fathernode.getChildlist().get(fathernode.getChildlist().size() - 1).getCategory();

                        TagNode nextChild = childNode.getRight();
                        if ((childCat.equals("CC") || childCat.equals(",") || childCat.equals("``") || childCat.equals("''")
                                || (childCat.equals(":"))
                                && childNode.isLeaf() && ((LeafNode) childNode).getLeaf().equals(";"))
                                || TagCorpus.equivCat(childCat, fatherCat, fathernode.getHeadChild().getCategory()) || (entry.isMultiLex()))
                        {//last condition in order to exclude cases like either... or
                            if (entry.isMultiLex())
                            {
                            }
                            ///* ccordination with an uneven number of leaves
                            else if (fathernode.getChildlist().size() % 2 == 0
                                    && n.getLeft() != null
                                    && n.getLeft().getCategory().matches("[A-Z]+")
                                    && (TagCorpus.equivCat(firstChildCat, fatherCat, fathernode.getHeadChild().getCategory())
                                    && TagCorpus.equivCat(lastChildCat, fatherCat, fathernode.getHeadChild().getCategory()))
                                    && !fathernode.getChildlist().get(1).getCategory().matches("([,;:]|(CC))") //&& fathernode.getChildlist().get(fathernode.getChildlist().size()-2).getCategory().matches("([,.]|(CC))")
                                    )
                            {
                                //assume implicit right branching.
                                //System.err.println("\nUnexpected number of nodes in coordination: "
                                //		+ childNode.getCategory() + "; expected: " + fathernode.getCategory()
                                //		+ "; found " + fathernode.getChildlist().size() + " nodes.\n");
                                insertNode(fathernode, fathernode.getChildlist().get(0), entry);
                            }
                        }
                        else if (childNode.getRight() == null
                                || (childNode.getRight() != null && !childNode.getRight().getCategory().matches("([,;:]|(CC))")))
                        {
                            if (verbose)
                            {
                                System.out.println("\nUnexpected type in coordination: " + childNode.getFullCategory() + "(child)"
                                        + "; expected: " + fathernode.getFullCategory() + "(father)");
                            }
                            insertNode(fathernode, childNode, entry);
                        }
                        childNode = nextChild;
                    }
                    return;
                }
            }
        }
    }

    /**
     * default: the wrong (not matching) node is left because of default right branching.
     * BUT: this is not true for right node raising or interpunctuation at the end of a sentence etc. 
     * TODO!!!
     * Further shortcoming: only applies to cases where one dependent is not part of the conjunction.
     * 
     * @param fathernode : the fathernode which needs to be split up
     * @param childNode : the non-matching child node (not part of the coordination)
     * @param entry : the lexicon entry
     */
    private void insertNode(TagNode fathernode, TagNode childNode, LexEntry entry)
    {

        TagNode newCoordNode;
        if (childNode.isHead())
        {
            newCoordNode = new InnerNode(idgen, childNode.getRight().getCategory(), new HashMap<String, Trace>());
        }
        else
        {
            newCoordNode = new InnerNode(idgen, fathernode.getCategory(), new HashMap<String, Trace>());
        }
        // Common left node, i.e. "those" / "the" in NP
        if (childNode.getLeft() == null)
        {
            newCoordNode.setParent(fathernode);
            childNode.getRight().setLeftsib(null);
            TagNode currentNode = childNode;
            while (currentNode.getRight() != null)
            {
                currentNode = currentNode.getRight();
                currentNode.setParent(newCoordNode);
                newCoordNode.addChild(currentNode);
                fathernode.removeChild(currentNode);
            }
            fathernode.addChild(newCoordNode);
            childNode.setRightsib(newCoordNode);
            newCoordNode.setLeftsib(childNode);//woher wissen was rechts und links???

            //entry.addNode(newCoordNode);
            //fathernode.getHeadChild().getLexEntry().addNode(newCoordNode);
            //newCoordNode.setLexEntry(fathernode.getHeadChild().getLexEntry());
            if (childNode.isHead())
            {
                newCoordNode.setHeadChild(newCoordNode.getChildlist().get(newCoordNode.getChildlist().size() - 1));
                newCoordNode.getChildlist().get(newCoordNode.getChildlist().size() - 1).makeHead();
                newCoordNode.makeNotHead();
                newCoordNode.setLexEntry(newCoordNode.getHeadChild().getLexEntry());
            }
            else
            {
                newCoordNode.setHeadChild(fathernode.getHeadChild());
                fathernode.setHeadChild(newCoordNode);
                newCoordNode.makeHead();
                newCoordNode.setLexEntry(entry);
            }
        }
        //Right Node Raising
        ///*
        else if (childNode.getRight() == null)
        {
            newCoordNode.setParent(fathernode);
            TagNode currentNode = fathernode.getChildlist().get(0);
            while (currentNode.getRight() != null
                    && !currentNode.getCategory().equals(childNode.getCategory()))
            {
                currentNode.setParent(newCoordNode);
                newCoordNode.addChild(currentNode);
                fathernode.removeChild(currentNode);
                currentNode = currentNode.getRight();
            }
            childNode.getLeft().setRightsib(null);
            fathernode.removeChild(childNode);
            fathernode.addChild(newCoordNode);
            fathernode.addChild(childNode);
            childNode.setLeftsib(newCoordNode);
            newCoordNode.setRightsib(childNode);
            if (!childNode.isHead())
            {
                newCoordNode.makeHead();
                if (fathernode.getHeadChild().getLexEntry() != null)
                {
                    newCoordNode.setLexEntry(fathernode.getHeadChild().getLexEntry());
                    fathernode.getHeadChild().getLexEntry().addNode(newCoordNode);
                }
            }

            newCoordNode.setLexEntry(entry);
            entry.addNode(newCoordNode);

            newCoordNode.setHeadChild(fathernode.getHeadChild());
            fathernode.setHeadChild(newCoordNode);
        }//*/
    }

    public void insertAppositionTreatment(TagNode node, PercolTable pt)
    {
        List<TagNode> childlist = node.getChildlist();
        int listSize = childlist.size();
        if (listSize > 2)
        {
            for (int i = 0; i < listSize; i++)
            {
                TagNode child = childlist.get(i);
                if (child.getCategory().equals(",") && childlist.size() > i + 2
                        && childlist.get(i + 2).getCategory().equals(",")
                        && (node.getCategory().equals(node.getHeadChild().getCategory())
                        || (node.getCategory().equals("S") && node.getHeadChild().getCategory().equals("VP"))
                        || node.getCategory().equals("FRAG"))
                        && !childlist.get(i + 1).getArgMod().equals("ARG")
                        && !childlist.get(i + 1).isHead()
                        && numberOfCC(childlist) == 0)
                {
                    if (childlist.size() > i + 4 && !childlist.get(childlist.size() - 1).getCategory().matches("[.,?!;:]"))
                    {
                        //to prevent appositions in listings with no "and"
                        return;
                    }
                    TagNode firstkomma = childlist.get(i);
                    TagNode apposition = childlist.get(i + 1);
                    TagNode secondkomma = childlist.get(i + 2);
                    TagNode leftbound = firstkomma.getLeft();
                    TagNode rightbound = secondkomma.getRight();
                    TagNode newAppositionNode = insertNodeforPQ(firstkomma, secondkomma, node, "APP", pt);
                    ArrayList<TagNode> childstock = new ArrayList<TagNode>();

                    for (int d = listSize - 1; d >= i + 3; d--)
                    {
                        childstock.add(0, (TagNode) node.removeChild(node.getChildlist().get(d)));
                    }
                    node.removeChild(firstkomma);
                    node.removeChild(apposition);
                    node.removeChild(secondkomma);
                    node.addChild(newAppositionNode);
                    apposition.setArgument();
                    for (TagNode cs : childstock)
                    {
                        node.addChild(cs);
                    }
                    newAppositionNode.setLeftsib(leftbound);
                    if (leftbound != null)
                    {
                        leftbound.setRightsib(newAppositionNode);
                    }
                    newAppositionNode.setRightsib(rightbound);
                    if (rightbound != null)
                    {
                        rightbound.setLeftsib(newAppositionNode);
                    }
                    newAppositionNode.setParent(node);
                    newAppositionNode.makeNotHead();
                    newAppositionNode.setModifier();
                    return;
                }
                else if (child.getCategory().equals(",") && childlist.size() == i + 2
                        && node.getCategory().equals(node.getHeadChild().getCategory())
                        && numberOfCC(childlist) == 0
                        && !childlist.get(i + 1).isHead()
                        && !childlist.get(i + 1).getArgMod().equals("ARG")
                        && nextLeafCat(childlist.get(i + 1)).matches("[,.:]|(-1)"))
                {
                    TagNode firstkomma = childlist.get(i);
                    TagNode apposition = childlist.get(i + 1);
                    TagNode leftbound = firstkomma.getLeft();
                    TagNode newAppositionNode = insertNodeforPQ(firstkomma, apposition, node, "APP", pt);
                    ArrayList<TagNode> childstock = new ArrayList<TagNode>();

                    for (int d = listSize - 1; d >= i + 2; d--)
                    {
                        childstock.add(0, (TagNode) node.removeChild(node.getChildlist().get(d)));
                    }
                    node.removeChild(firstkomma);
                    node.removeChild(apposition);
                    node.addChild(newAppositionNode);
                    apposition.setArgument();
                    for (TagNode cs : childstock)
                    {
                        node.addChild(cs);
                    }
                    newAppositionNode.setLeftsib(leftbound);
                    if (leftbound != null)
                    {
                        leftbound.setRightsib(newAppositionNode);
                    }
                    newAppositionNode.setRightsib(null);
                    newAppositionNode.setParent(node);
                    newAppositionNode.makeNotHead();
                    newAppositionNode.setModifier();
                    return;
                }
            }
            for (TagNode child : node.getChildlist())
            {
                if (!child.isLeaf())
                {
                    insertAppositionTreatment(child, pt);
                }
            }
        }
        // assume maximally one level of QPs, so don't search below one a QP was found.
        for (TagNode child : node.getChildlist())
        {
            if (!child.isLeaf())
            {
                insertAppositionTreatment(child, pt);
            }
        }
    }

    private String nextLeafCat(TagNode node)
    {
        while (!node.isLeaf())
        {
            node = node.getChildlist().get(node.getChildlist().size() - 1);
        }
        Iterator leafit = leaflist.iterator();
        while (leafit.hasNext())
        {
            LeafNode leaf = (LeafNode) leafit.next();
            if (leaf == node)
            {
                if (leafit.hasNext())
                {
                    return ((LeafNode) leafit.next()).getCategory();
                }
                else
                {
                    return "-1";
                }
            }
        }
        return "";
    }

    /**
     * Removes quotation marks and round brackets from the penn tree.
     * This can't be done before, because of the necessity to conform to the word count for nombank and propbank.
     * 
     * @param node
     */
    public void removeQuotationMarks(TagNode node)
    {
        if (node.isLeaf())
        {
            if (node.getCategory().matches(".*((``)|('')).*")
                    || ((LeafNode) node).getLeaf().contains("-RRB-")
                    || ((LeafNode) node).getLeaf().contains("-LRB-")
                    || ((LeafNode) node).getLeaf().contains("-RCB-")
                    || ((LeafNode) node).getLeaf().contains("-LCB-"))
            {
                leaflist.remove(node);
                while (node.isHead())
                {
                    node = node.getParent();
                }
                TagNode parent = node.getParent();
                parent.removeChild(node);
                if (node.getRight() != null)
                {
                    node.getRight().setLeftsib(node.getLeft());
                }
                if (node.getLeft() != null)
                {
                    node.getLeft().setRightsib(node.getRight());
                }
            }
            return;
        }
        TagNode child = node.getChildlist().get(0);
        while (child != null)
        {
            removeQuotationMarks(child);
            child = child.getRight();
        }
    }

    /**
     * Removes sentence-final punctuation.
     * 
     * @param node
     */
    public void removeFinalPunct()
    {
        LeafNode nodeLeaf = this.leaflist.get(leaflist.size() - 1);
        while (nodeLeaf.getCategory().equals(".") || nodeLeaf.getLeaf().equals("?"))
        {
            leaflist.remove(nodeLeaf);
            TagNode node = nodeLeaf;
            while (node.isHead())
            {
                node = node.getParent();
            }
            TagNode parent = node.getParent();
            parent.removeChild(node);
            if (node.getRight() != null)
            {
                node.getRight().setLeftsib(node.getLeft());
            }
            if (node.getLeft() != null)
            {
                node.getLeft().setRightsib(node.getRight());
            }
            nodeLeaf = this.leaflist.get(leaflist.size() - 1);
        }
    }

    /*public void removeAllPunct() {
    for(int i = leaflist.size()-1; i>=0; i--){
    LeafNode nodeLeaf = this.leaflist.get(i);
    if(nodeLeaf.getCategory().equals(".") || nodeLeaf.getCategory().equals(":")|| nodeLeaf.getCategory().equals(",")){
    if (((LeafNode)nodeLeaf).getLeaf().equals("0")|| ((LeafNode)nodeLeaf).getLeaf().startsWith("*")){
    continue;
    }
    leaflist.remove(nodeLeaf);
    TagNode node = nodeLeaf;
    while(node.isHead()){
    node = node.getParent();
    }
    TagNode parent = node.getParent();
    parent.removeChild(node);
    if (node.getRight()!= null)
    node.getRight().setLeftsib(node.getLeft());
    if (node.getLeft()!= null)
    node.getLeft().setRightsib(node.getRight());
    nodeLeaf = this.leaflist.get(leaflist.size()-1);
    }
    }
    }*/
    public void insertExtraVPNodes(TagNode node)
    {

        if (node.getCategory().matches("VB.*"))
        {
            TagNode father = node.getParent();
            if (!father.getCategory().equals("VP"))
            {
                //System.err.println("unexpected fathernode for "+node.getCategory()+" node (not VP but "+ father.getCategory()+").");
                return;
            }
            TagNode newVPNode = new InnerNode(idgen, "VP", new HashMap<String, Trace>());
            father.removeChild(node);
            ArrayList<TagNode> otherCs = new ArrayList<TagNode>();
            TagNode sibling = node.getRight();
            while (sibling != null)
            {
                otherCs.add(sibling);
                father.removeChild(sibling);
                sibling = sibling.getRight();
            }
            father.addChild(newVPNode);
            for (TagNode oC : otherCs)
            {
                father.addChild(oC);
            }
            newVPNode.addChild(node);
            newVPNode.setParent(father);
            if (father.getHeadChild().equals(node))
            {
                father.setHeadChild(newVPNode);
                newVPNode.makeHead();
            }
            newVPNode.setHeadChild(node);
            node.makeHead();
            node.setParent(newVPNode);
            TagNode right = node.getRight();
            TagNode left = node.getLeft();
            newVPNode.setRightsib(right);
            newVPNode.setLeftsib(left);
            if (right != null)
            {
                right.setLeftsib(newVPNode);
            }
            if (left != null)
            {
                left.setRightsib(newVPNode);
            }
            node.setRightsib(null);
            node.setLeftsib(null);
            TagNode iterateSiblings = newVPNode.getRight();
            while (iterateSiblings != null)
            {
                insertExtraVPNodes(iterateSiblings);
                iterateSiblings = iterateSiblings.getRight();
            }
        }
        if (node.getChildlist().size() > 0)
        {
            TagNode child = node.getChildlist().get(0);
            while (child != null)
            {
                insertExtraVPNodes(child);
                child = child.getRight();
            }
        }
    }

    public String toString()
    {
        return "sourceString: " + sourceString + "\ncurrentString: " + currentstring + "\n";
    }

    public void removeSomeTraces()
    {

        for (int i = leaflist.size() - 1; i >= 0; i--)
        {
            LeafNode nodeLeaf = this.leaflist.get(i);
            if (nodeLeaf.getLeaf().startsWith("*ICH*") || nodeLeaf.getLeaf().startsWith("*U*")
                    || nodeLeaf.getLeaf().startsWith("*EXP*") || nodeLeaf.getLeaf().startsWith("*PPA*")
                    || nodeLeaf.getLeaf().startsWith("*RNR*"))
            {
                leaflist.remove(nodeLeaf);
                TagNode node = nodeLeaf;
                while (node.isHead() && node.getParent().getParent() != null)
                {
                    node = node.getParent();
                }
                TagNode parent = node.getParent();
                parent.removeChild(node);
                if (node.getRight() != null)
                {
                    node.getRight().setLeftsib(node.getLeft());
                }
                if (node.getLeft() != null)
                {
                    node.getLeft().setRightsib(node.getRight());
                }
                nodeLeaf = this.leaflist.get(leaflist.size() - 1);
            }
        }
    }
}
