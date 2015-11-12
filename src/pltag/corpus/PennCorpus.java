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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PennCorpus
{

    private List<String> sentences = new ArrayList<String>();
    private final List<PennTree> trees = new ArrayList<PennTree>();
    //private ArrayList<ArrayList<Leafnode>> leaflists = new ArrayList<ArrayList<Leafnode>>();
    private int totalNumOfPropositionArgs;
    
    public PennCorpus(String filename)
    {
        sentences = read(filename);
        if (sentences.size() > 0 && sentences.get(0).equals(""))
        {
            sentences.remove(0);
        }
        buildTrees(sentences);
        //System.out.println("ready!");
    }

    private void buildTrees(List<String> sents)
    {
        Iterator<String> i = sents.iterator();
        while (i.hasNext())
        {
            String s = i.next();

            //System.out.println(trees.size()+"\t"+s);
            if (!s.equals(""))
            {
                PennTree tree = new PennTree(s);
                trees.add(tree);
                //tree.printleaves();

            }
        }
    }

    private ArrayList<String> read(String filename2)
    {
//		declared here only to make visible to finally clause
        StringBuffer sentence = new StringBuffer();
        BufferedReader input = null;
        List<String> sents = new ArrayList<String>();
        FileReader fr;
        try
        {
            //use buffering, reading one line at a time
            //FileReader always assumes default encoding is OK!

            fr = new FileReader(filename2);
            input = new BufferedReader(fr);

            String line = null; //not declared within while loop
			/*
             * readLine is a bit quirky :
             * it returns the content of a line MINUS the newline.
             * it returns null only for the END of the stream.
             * it returns an empty String if two newlines appear in a row.
             */
            while ((line = input.readLine()) != null)
            {
                if (line.startsWith("("))
                {
                    if (!sentence.toString().equals(""))
                    {
                        String sent = sentence.toString();
                        sents.add(sent);
                        //System.out.println(sentence);
                    }
                    sentence = new StringBuffer();
                    sentence.append(line);
                }
                else
                {
                    sentence.append(line.replaceAll("^(\t| )+", ""));
                }
            }
            if (!sentence.equals(""))
            {
                String sent = sentence.toString();
                sents.add(sent);
                //System.out.println(sentence);
            }
        }
        catch (FileNotFoundException e)
        {
            System.err.println(e.getMessage());
            //e.printStackTrace();
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
                    //flush and close both "input" and its underlying FileReader
                    input.close();
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return (ArrayList<String>) sents;

    }

    public List<PennTree> getTrees()
    {
        return trees;
    }

    public void determinePredictive(PredictiveLexicon pl, boolean useSemantics)
    {
        int length = trees.size();
        //for each sentence augment:
        for (int treeno = 0; treeno < length; treeno++)
        {
            PennTree tree = trees.get(treeno);
            ArrayList<LeafNode> leaves = tree.getLeaflist();
            int i = 0;
            for (LeafNode leaf : leaves)
            {
                LeafNode second = pl.check(i, leaves);
                i++;
                //should get found both ways round.
                if (second != null)
                {
//					do the following on penn corpus class level:
                    //more sophisticated: only return matching node if they are under the same tree.
                    //make sure that partial trees meet! (like connection path???) 
                    if (leaf.getParent() == second.getParent())
                    {
                        leaf.addLexEntryRest(second);
                    }
                }
                // make note of verbs with subcategorizes PPs
                if(useSemantics && leaf.getCategory().matches("VB.*"))
                {
                    for(TagNode child : leaf.getParent().getChildlist())
                    {
                        if(child.getCategory().equals("PP") && child.isArgument())
                        {                            
                            TagNode headChild = child.getHeadChild();
                            if(headChild.isLeaf())
                            {
                                LeafNode l = (LeafNode) headChild;
                                leaf.addLexEntryRest(l);
                                l.addLexEntryRest(leaf);
                            }                                                                
                        }
                    }
                }
            } // for
        } // for
    }

    public void mergeSources(String fileno, Bank bank)
    {
        fileno = "wsj/".concat(fileno);
        //need to do anything?
        if (bank.getBank().containsKey(fileno))
        {
            HashMap<String, HashMap<String, Entry>> relevantAnnots = bank.getBank().get(fileno);    
            totalNumOfPropositionArgs += numOfPropositionArgs(relevantAnnots);
            addInfo(relevantAnnots);
        }
    }
        
    private void addInfo(HashMap relevantAnnots)
    {
        int length = trees.size();
        //for each sentence augment:
        for (int treeno = 0; treeno < length; treeno++)
        {
            //System.out.println(treeno);
            String treeid = new Integer(treeno).toString();
            PennTree tree = trees.get(treeno);
            ArrayList leaves = tree.getLeaflist();
            if (relevantAnnots.containsKey(treeid))
            {
                HashMap treelevel = (HashMap) relevantAnnots.get(treeid);
                Set annotatedWords = treelevel.keySet();
                Iterator i = annotatedWords.iterator();
                //			 for each head which has dependents:
                while (i.hasNext())
                {
                    String wordID = (String) i.next();
                    //get the object with the annotation:
                    Entry annotationinfo = (Entry) treelevel.get(wordID);
                    List<Relation> argsAndMods = annotationinfo.getArgsMods();
                    int wID = new Integer(wordID).intValue();
                    LeafNode annotatedWord = (LeafNode) leaves.get(wID);
                    annotatedWord.setBaseForm(annotationinfo.baseForm);
                    annotatedWord.setSense(annotationinfo.senseNumber);
                    //check alignment:
					/* String headname = annotationinfo.baseForm;
                    if (! annotatedWord.getLeaf().contains(headname)){
                    System.err.print("counting wrong?! ");
                    System.err.println(annotatedWord.getLeaf().concat(" vs. ".concat(headname)));
                    }
                    //System.out.println(headname.concat(argsAndMods.toString()));*/
                    //for each of the annotated words, insert the info.
                    for(Relation rel : argsAndMods)
                    {
                        rel.processInfo(leaves, annotatedWord);
                    }
//                    Iterator j = argsAndMods.iterator();
//                    while (j.hasNext())
//                    {
//                        Relation rel = (Relation) j.next();
//                        rel.processInfo(leaves, annotatedWord);
//                    }
                } // while
            } // if
        } // for
    }
    
    private int numOfPropositionArgs(HashMap<String, HashMap<String, Entry>> sentences)
    {
        int total = 0;
        for(HashMap<String, Entry> sentence : sentences.values())
        {
            for(Entry proposition : sentence.values())
            {
                total += proposition.numOfArgs();
            }
        }
        return total;
    }

    public int getTotalNumOfPropositionArgs()
    {
        return totalNumOfPropositionArgs;
    }
    
    
}