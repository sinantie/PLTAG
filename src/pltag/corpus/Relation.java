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
import java.util.Iterator;

public class Relation
{

    public String dependentwno;
    public String leveldepth;
    public String relation;
    public ArrayList<String> traces;
    public String[] gappedRest;

    @Override
    public String toString()
    {
        return dependentwno + ":" + leveldepth + " " + relation;
    }

    public Relation(String dep)
    {
        int minuspos = dep.indexOf("-");
        String deps = dep.substring(0, minuspos);
        relation = dep.substring(minuspos + 1);

        if (deps.contains("*"))
        {
            //traces = deps.split('*');
            int starindex = deps.indexOf("*");
            String t = deps.substring(starindex + 1);
            traces = new ArrayList<String>();
            String rest = "";
            while (t.contains("*"))
            {
                traces.add(t.substring(0, t.indexOf("*")));
                t = t.substring(t.indexOf("*") + 1);
            }
            if (t.contains(","))
            {
                traces.add(t.substring(0, t.indexOf(",")));
                rest = t.substring(t.indexOf(","));
            }
            else
            {
                traces.add(t);
            }
            deps = deps.substring(0, starindex) + rest;
        }
        if (deps.contains(","))
        {
            gappedRest = deps.split(",");
            deps = deps.substring(0, deps.indexOf(","));
        }

        int colonpos = deps.indexOf(":");
        dependentwno = deps.substring(0, colonpos);
        leveldepth = deps.substring(colonpos + 1);
        //System.out.println("");
    }

    public void processInfo(ArrayList leaflist, LeafNode annotatedWord)
    {
        int leafno = Integer.valueOf(dependentwno);
        //depth incorrect because of noun phrase annotation, therefore ignore.
        //int depth = new Integer(rel.leveldepth).intValue();
        LeafNode thisleafnode = (LeafNode) leaflist.get(leafno);
        boolean relationtypeIsArg;
        //if relation is rel, set child to head, and remove previous head assignment???
        //should probably not do this for nombank, because subject may be argument of noun, 
        //and verb can have support status, in which case the noun would make the head 
        //in terms of nombank annotation. So only apply to those nodes that are below???
        //example: receive approval: "from-PP" should be argument of approval, not receive, 
        //but NP approval and ARG-PP are in sister relationship.
        //receive and approval should maybe go into one TAG tree.
        if (relation.equals("rel"))
        {// the word itself or its second part as in "run out".
            //don't need to do anything?
            relationtypeIsArg = true;
        }
//		if relation is Support mark obligatory (but this is probably often the verb, 
        //so relation not well defined in terms or argument / modifier, dependencies may be different!)
        else if (relation.equals("Support"))
        {
            //annotatedWord.setArgument();
            annotatedWord.setModifier();
            //thisleafnode.setArgument();//may be unnecessary since they'll end up in the same tree anyway, but won't hurt.
            relationtypeIsArg = false;// true;//
            relationtypeIsArg = annotateNode(annotatedWord, thisleafnode, relationtypeIsArg, leveldepth);
            thisleafnode.addSupportNode(annotatedWord);
            annotatedWord.addSupportNode(thisleafnode);
        }
        //if relation is ARG\d set type to argument
        //inherit to parent nodes in separate step;
        else if (relation.matches("ARG[0-9].*"))
        {
            relationtypeIsArg = true;
            relationtypeIsArg = annotateNode(annotatedWord, thisleafnode, relationtypeIsArg, leveldepth);
        }
        //if relation is ARGM-(DIS|CONJ) mark as modifier and change CC to CCSIM
        else if (relation.matches("ARGM-DIS") && thisleafnode.getCategory().equals("CC") //&&annotatedWord.getParent().getCategory().matches("^S.*") 
                )
        {
            relationtypeIsArg = false;
            thisleafnode.setCategory("CCSIM");
            relationtypeIsArg = annotateNode(annotatedWord, thisleafnode, relationtypeIsArg, leveldepth);
        }
        //if relation is ARGM-(LOC|MOD|TMP|*) mark as modifier.
        else if (relation.startsWith("ARGM-"))
        {
            relationtypeIsArg = false;
            relationtypeIsArg = annotateNode(annotatedWord, thisleafnode, relationtypeIsArg, leveldepth);
        }
        //else print relation type
        else
        {
            //writetoLog("relation type doesn't match: ".concat(relation));
            //writetoLog("\n");
            relationtypeIsArg = false;//default assignment for problematic case
            relationtypeIsArg = annotateNode(annotatedWord, thisleafnode, relationtypeIsArg, leveldepth);
        }
        ///*
        if (traces != null)
        {
            for (String id : traces)
            {
                LeafNode ln = (LeafNode) leaflist.get(Integer.valueOf(id.substring(0, id.indexOf(":"))));
                String tracedepth = id.substring(id.indexOf(":") + 1);
                relationtypeIsArg = annotateNode(annotatedWord, ln, relationtypeIsArg, tracedepth);
            }
        }
        if (gappedRest != null)
        {
            if (relation.equals("rel"))
            {
                //example: With the UN accords, "relating to Afghanistan", the Soviet Union got 
                //everything they needed to consolidate... Relation: Proposition of accord 
                //this is not really syntactic. better leave such argument out altogether. 
                //not clear how to do this in an organized and principled way.
                // in the above example, " relating to Afghanistan , " the get are all 
                //marked (including the punctuation); other examples include "American Express"
                int l = ((String[]) gappedRest).length;
                for (int gi = 0; gi < l; gi++)
                {
                    String id = gappedRest[gi];
                    LeafNode ln = (LeafNode) leaflist.get(new Integer(id.substring(0, id.indexOf(":"))).intValue());
                    String tracedepth = id.substring(id.indexOf(":") + 1);
                    annotateNode(annotatedWord, ln, relationtypeIsArg, tracedepth);
                    ln.addSupportNode(annotatedWord);
                    annotatedWord.addSupportNode(ln);
                    ln.addLexEntryRest(annotatedWord);
                    annotatedWord.addLexEntryRest(ln);
                }
            }
            else
            {
                //System.out.println(annotatedWord+" "+gappedRest);
                int l = ((String[]) gappedRest).length;
                for (int gi = 0; gi < l; gi++)
                {
                    String id = gappedRest[gi];
                    LeafNode ln = (LeafNode) leaflist.get(new Integer(id.substring(0, id.indexOf(":"))).intValue());
                    String tracedepth = id.substring(id.indexOf(":") + 1);
                    annotateNode(annotatedWord, ln, relationtypeIsArg, tracedepth);
                    //ln.addSupportNode(annotatedWord);
                    //annotatedWord.addSupportNode(ln);
                    //ln.addLexEntryRest(annotatedWord);
                    //annotatedWord.addLexEntryRest(ln);
                }
            }
        }//*/		
    }

    private boolean annotateNode(LeafNode annotatedword, LeafNode dependentleaf, boolean isargument, String leveldepth)
    {
        TagNode dependent = dependentleaf;
        int ld = Integer.valueOf(leveldepth);
        for (int i = 0; i < ld; i++)
        {
            dependent = dependent.getParent();
        }
        // dependent should be in tree now unless NP annotation added nodes between dependent and head-tree. TODO
        LeafNode annotatedWord2 = null;
        if (!annotatedword.getLexEntryRest().isEmpty())
        {
            Iterator it = annotatedword.getLexEntryRest().values().iterator();
            annotatedWord2 = (LeafNode) it.next();
            while (annotatedword == annotatedWord2 && it.hasNext())
            {
                annotatedWord2 = (LeafNode) it.next();
            }

        }
        while (dependent.hasParent() && !(dependent.getParent().isOnSpine(annotatedword)
                || (annotatedWord2 != null && dependent.getParent().isOnSpine(annotatedWord2)))
                && dependent.getCategory().startsWith("N") && dependent.getParent().getCategory().equals("NP")
                && (dependent.getRight() == null || dependent.getRight().getCategory().startsWith("N"))
                && (dependent.getLeft() == null || dependent.getLeft().getCategory().startsWith("N")))
        {
            dependent = dependent.getParent();
            //System.out.println("Relation.annotateNode: one up.");
        }

        dependent.setRole(annotatedword, this.relation);
        while (dependent.hasParent() && !(dependent.getParent().isOnSpine(annotatedword)
                || (annotatedWord2 != null && dependent.getParent().isOnSpine(annotatedWord2))))
        {
            /*if(! dependent.getParent().getCategory().equals(dependent.getCategory())){//correct for coordination???
             System.out.print("##"+dependent.getParent().getCategory()+" "+dependent.getCategory());
             System.out.print(relation+ " "+annotatedword + " "+dependentleaf);
             System.out.println();
             }*/
            dependent = dependent.getParent();
        }
        //String test = dependent.getFullCategory();
        //	String rela = relation;
        if (dependent.hasParent() && !dependent.getParent().isInSameTree(annotatedword))
        {
            //System.err.println(dependent.getFullCategory());
        }
        else if (isargument //){
                && (relation.equals("rel") || !dependent.getFullCategory().matches(".*-((VOC)|(DIR)|(LOC)|(MNR)|(PRP)|(TMP)|(CLR)).*")))
        {//){//
            dependent.setArgument();
        }
        else
        {
            isargument = false;
            dependent.setModifier();
            //dependentleaf.setModifier();//TODO should this really stay this way? Maybe just dependent.
        }
        return isargument;
    }
}
