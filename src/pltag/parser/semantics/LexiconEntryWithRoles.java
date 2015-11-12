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
package pltag.parser.semantics;

import fig.basic.Indexer;
import java.util.HashSet;
import java.util.Set;
import pltag.corpus.IdGenerator;
import static pltag.parser.semantics.SemanticLexicon.removeAnnotation;

/**
 *
 * @author konstas
 */
public class LexiconEntryWithRoles
{

    boolean isRelation;
    String lexEntry, unlexEntry;
    Set<RoleSignature> roles;
    int frequency;
    boolean freqBaseline;
    Indexer<String> roleIndexer;
    
    public LexiconEntryWithRoles(int frequency, String unlexEntryWithSemantics, String lexEntry, boolean isRelation, Indexer<String> roleIndexer, boolean freqBaseline)
    {
        this.frequency = frequency;
        this.lexEntry = lexEntry;
        this.isRelation = isRelation;
        this.roleIndexer = roleIndexer;
//            unlexEntriesWithSemantics = new HashSet<String>();
//            unlexEntriesWithSemantics.add(unlexEntryWithSemantics);
        roles = new HashSet<>();
        roles.add(extractRoles(new RoleSignature(roleIndexer, unlexEntryWithSemantics, frequency), new IdGenerator()));
    }

    public LexiconEntryWithRoles(LexiconEntryWithRoles entry)
    {
        this.isRelation = entry.isRelation;
        this.lexEntry = entry.lexEntry;
        this.unlexEntry = entry.unlexEntry;
        this.roles = new HashSet<>(entry.roles);
//            this.unlexEntriesWithSemantics = new HashSet<String>(entry.unlexEntriesWithSemantics);
        this.frequency = entry.frequency;
        this.freqBaseline = entry.freqBaseline;
        this.roleIndexer = entry.roleIndexer;
    }

    void addEntry(LexiconEntryWithRoles entry)
    {
        // entry with unlexicalised entries with semantics. Append incoming set to the existing.
        this.roles.addAll(entry.roles);
//            this.unlexEntriesWithSemantics.addAll(entry.unlexEntriesWithSemantics);

    }

    void addEntry(LexiconEntryWithRoles entry, String unlexEntryWithSemantics)
    {
        IdGenerator idgen = new IdGenerator();
        RoleSignature signature = extractRoles(new RoleSignature(roleIndexer, unlexEntryWithSemantics, entry.frequency), idgen);
//            if(!roles.contains(signature))
//            {                
        roles.add(signature);
        this.frequency += entry.frequency;
//            }          
//            if(!unlexEntriesWithSemantics.contains(unlexEntryWithSemantics))
//            {
//                // extract roles
//                unlexEntriesWithSemantics.add(unlexEntryWithSemantics);
//                this.frequency += entry.frequency;
//            }          
    }

    private RoleSignature extractRoles(RoleSignature signature, IdGenerator idgen)
    {
        String s = signature.getTreeString().trim();
//            String s = treeStringIn.trim();
        if (s.charAt(0) == ' ')
        {
            s = s.substring(1);
        }
        int index = 0;
        boolean notEnd = true;
        while (s.charAt(index) != '(' && s.charAt(index) != ' ' && notEnd)
        {//as long as no subcategory started
            if (s.charAt(index) == ')')
            {//leaf
                notEnd = false;
                String catleaf = s.substring(1, index);
                catleaf = catleaf.trim();
                String[] parentAndChildren = catleaf.split(" ");
                String parent = parentAndChildren[0];
                int parentId = idgen.getNewId();
                //tree = makeNode(parent, Integer.MIN_VALUE, parentId, tree);
                extractRole(signature, parent, parentId);
                //tree.setRoot(parentId);//in recursive process: always overwrite this info! see below.
                for (int i = 1; i < parentAndChildren.length; i++)
                {
                    String node = parentAndChildren[i];
                    int nodeId = idgen.getNewId();
                    //tree = makeNode(node, parentId, nodeId, tree);
                    extractRole(signature, node, nodeId);
                }
                signature.setTreeString(s.substring(index + 1));
                return signature;
            }
            else
            {
                index++;
            }
        }
        s = s.trim();
        if (s.charAt(0) == '(')
        {
            s = s.substring(1);
        }
        String parent = (s.substring(0, index)).trim();//because always start after opening bracket
        String treeString = s.substring(index, s.length());
        if (parent.equals("") && treeString.length() > 0)
        {
            signature.setTreeString(treeString);
            signature = extractRoles(signature, idgen);
//                tree = convertToTree(tree, idgen);
            if (signature == null)
            {
                return null;
            }
            if (treeString.startsWith("  ("))
            {
                //System.out.println("!" + tree.getStructure(tree.getRoot()));
                int rootNodeId = idgen.getNewId();
//                    tree = makeNode("", Integer.MIN_VALUE, rootNodeId, tree);
//                    tree.addChild(rootNodeId, tree.getRoot());
//                    tree.putParent(tree.getRoot(), rootNodeId);
//                    tree.setRoot(rootNodeId);
            }
        } // if
        else
        {
            treeString = treeString.trim();
            int parentId = idgen.getNewId();
//                tree = makeNode(parent, Integer.MIN_VALUE, parentId, tree);
            extractRole(signature, parent, parentId);
            while (!treeString.startsWith(")"))
            {
                if (treeString.startsWith("("))
                {
                    signature.setTreeString(treeString);
                    signature = extractRoles(signature, idgen);
//                        tree = convertToTree(tree, idgen);
//                        tree.putParent(tree.getRoot(), parentId);
//                        tree.addChild(parentId, tree.getRoot());
                    treeString = signature.getTreeString();
                    treeString = treeString.trim();
                }
                else
                {
                    int blankindex = treeString.indexOf(" ");
                    int endindex = treeString.indexOf(")");
                    String child;
                    if (blankindex < endindex && blankindex > 0)
                    {
                        child = treeString.substring(0, blankindex).trim();
                        treeString = treeString.substring(blankindex).trim();
                    }
                    else
                    {
                        if (endindex < 0)
                        {
                            System.err.println("problem");
                        }
                        child = treeString.substring(0, endindex).trim();
                        treeString = treeString.substring(endindex).trim();
                    }
                    int childId = idgen.getNewId();
//                        tree = makeNode(child, parentId, childId, tree);
                    extractRole(signature, child, childId);
                }
            }
            if (treeString.startsWith(")"))
            {
                treeString = treeString.substring(1);
                signature.setTreeString(treeString);
//                    tree.setRoot(parentId);
            }
        } // else
        return signature;
    }

    private void extractRole(RoleSignature signature, String category, int id)
    {
        String categoryRole[] = SemanticLexicon.stripPosAndSemanticsToken(removeAnnotation(category));
        String role = categoryRole[1];
        if (!(role.equals("") || role.startsWith("LEXEME")))
        {
            signature.addRole(categoryRole[1], categoryRole[0], id);
        }
    }   
    
    public boolean frequencyMoreThanOne()
    {
        return frequency > 1;
    }

    public int getFrequency()
    {
        return frequency;
    }

    public String getLexEntry()
    {
        return lexEntry;
    }

    public String getUnlexEntry()
    {
        return unlexEntry;
    }

    public Set<RoleSignature> getRoles()
    {
        return roles;
    }

    public void setUnlexEntry(String unlexEntry)
    {
        this.unlexEntry = unlexEntry;
    }

    public boolean isRelation()
    {
        return isRelation;
    }

    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof LexiconEntryWithRoles;
        LexiconEntryWithRoles e = (LexiconEntryWithRoles) obj;

        return unlexEntry == null ? lexEntry.equals(e.lexEntry) : unlexEntry.equals(e.unlexEntry);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + unlexEntry == null ? (this.lexEntry != null ? this.lexEntry.hashCode() : 0) : this.unlexEntry.hashCode();
        return hash;
    }

    @Override
    public String toString()
    {
        return (frequencyMoreThanOne() ? (freqBaseline ? String.valueOf(frequency) : "1") : "0") + "\t"
                + (unlexEntry == null ? lexEntry : unlexEntry);
    }
}