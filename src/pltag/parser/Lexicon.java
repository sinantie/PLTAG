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

import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Pair;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections4.map.MultiValueMap;


import pltag.corpus.ElementaryStringTree;
import pltag.corpus.IdGenerator;
import pltag.corpus.TagNodeType;
import pltag.corpus.StringTree;
import pltag.util.Utils;

public class Lexicon
{

    protected Options opts;
    protected Set<String> listOfFreqWords;    
    
    //private ArrayList<PennTree> trees = new ArrayList<PennTree>();
    //private MultiValueMap modLexentriesTree = new MultiValueMap();
    //private MultiValueMap argLexentriesTree = new MultiValueMap();
    protected MultiValueMap<String, ?> lexEntriesTree = new MultiValueMap();
    protected MultiValueMap<String, String> wordPosMap = new MultiValueMap();
    //private ArrayList structStringArray = new ArrayList();
    //private HashMap<String, Integer> lexToArraySlot = new HashMap<String, Integer>();
    protected static Pattern digits = Pattern.compile("([^0-9]*)[0-9]+(.*)");
    protected static Pattern upperCase = Pattern.compile("[A-Z][A-Za-z]*");
    protected static Pattern num = Pattern.compile("NUM([^a-z]*NUM?)[^a-z]*");
    protected MultiValueMap<String, String> trees = new MultiValueMap();
    protected HashMap<String, Integer> noOfTrees = new HashMap<String, Integer>();
    protected HashMap<String, Integer> posTagNo = new HashMap<String, Integer>();
    
    protected MultiValueMap<String, Pair<String, String>> rootNodeTreeMap = new MultiValueMap();
    protected static int numOfTreeTemplates = 6934;
    protected static HashMap<String, Integer> biWordMap = new HashMap<String, Integer>();
    
    public Lexicon(Options opts, Set<String> listOfFreqWords)
    {
        this.opts = opts;
        this.listOfFreqWords = listOfFreqWords;
    }
    
    public Lexicon(Options opts, Set<String> listOfFreqWords, String[] lines)
    {
        this.opts = opts;
        this.listOfFreqWords = listOfFreqWords;
        if(lines.length == 1 && lines[0].equals("")) // empty lexicon (occasionally in predicted lexicon)
        {                
            lexEntriesTree = new MultiValueMap<String, String>();
        }
        else
        {
            MultiValueMap<String, ?>[] entries = read(lines);                
            lexEntriesTree = makeLexStrings(entries[0], "ARG");
            lexEntriesTree.putAll(makeLexStrings(entries[1], "MOD"));
        }                
    }

    public Lexicon()
    {
    }
    
    public void processLexicon(String filename)
    {
        MultiValueMap<String, String>[] entries = read(filename);
        //lexentriesTree = makeLextrees(entries[0], "ARG");
        //lexentriesTree.putAll(makeLextrees(entries[1], "MOD"));
        lexEntriesTree = makeLexStrings(entries[0], "ARG");
        lexEntriesTree.putAll(makeLexStrings(entries[1], "MOD"));
    }
    
    public void processLexicon(String[] lines)
    {
        MultiValueMap<String, String>[] entries = (MultiValueMap<String, String>[]) read(lines);
        //lexentriesTree = makeLextrees(entries[0], "ARG");
        //lexentriesTree.putAll(makeLextrees(entries[1], "MOD"));
        lexEntriesTree = makeLexStrings(entries[0], "ARG");
        lexEntriesTree.putAll(makeLexStrings(entries[1], "MOD"));
    }
    
    public void postProcessLexicon(boolean writeToDisk)
    {
//        lexiconReduce();  
        if(writeToDisk)
        {
            try
            {
                Writer lexWriter = IOUtils.openOutEasy("normal_lexicon.txt");
                for (String key : lexEntriesTree.keySet())
                {
                    lexWriter.append(key).append("-> [");
                    for (Object val : lexEntriesTree.getCollection(key))
                    {
                        lexWriter.append(val.toString()).append(",");
                    }
                    lexWriter.append("]\n");
                }
                lexWriter.close();

            }        
            catch (IOException e)
            {
                LogInfo.error(e);
            }        
            try
            {
                Writer familywriter = IOUtils.openOutEasy("family_lexicon.txt");       
                for (String key : trees.keySet())
                {
                    familywriter.append(key).append("-> [");
                    for (String val : trees.getCollection(key))
                    {
                        familywriter.append(val).append(",");
                    }
                    familywriter.append("]\n");
                }        
                familywriter.close();
            }
            catch (IOException e)
            {
                LogInfo.error(e);
            }       
        } // if
        extractFamilyLexicon(writeToDisk);        
        removeHelps();
    }

    protected void extractFamilyLexicon(boolean writeToDisk)
    {
        try
        {
            Writer unlexSizeWriter = writeToDisk ? IOUtils.openOutEasy("family_size_lexicon.txt") : null;
            Collection<String> keyset = new ArrayList<String>(noOfTrees.keySet());            
            for (String key : keyset)
            {
                if (!key.contains("LEXEME"))
                {
                    noOfTrees.remove(key);                    
                    continue;
                }
                Integer frequency = noOfTrees.get(key);
                String val = frequency.toString();
                if(unlexSizeWriter != null)
                    unlexSizeWriter.append(val).append("\t").append(key).append("\n");
                if (frequency < 5)
                {
                    noOfTrees.remove(key);
                }
                else if (frequency >= 100)
                {
                    String[] info = key.split("\t");
                    lexEntriesTree.put(info[0], "1\t" + info[1]);
                }
            }
            if(unlexSizeWriter != null)
                unlexSizeWriter.close();
        }
        catch (IOException e)
        {
            LogInfo.error(e);
        }
    }        
    
    protected void extractVerificationTrees()
    {
        for(String word : lexEntriesTree.keySet())
        {
            for(String entry : (Collection<String>)lexEntriesTree.getCollection(word))
            {
                String rootCategory = getRootCategory(entry);
                rootNodeTreeMap.put(rootCategory, new Pair(word, entry));
            }
        }        
    }
    
    public String getRootCategory(String tree)
    {
        int startIndex = tree.indexOf("(");
        int endIndex = tree.indexOf("^");
        return tree.substring(startIndex + 2, endIndex);
    }
    
    @SuppressWarnings("unchecked")
    protected int checkType(String string, MultiValueMap stringTreeMap)
    {
        Collection<StringTree> values = stringTreeMap.values();
        int errors = 0;
        for (StringTree tree : values)
        {
            if ((string.equals("MOD") && tree.isAuxtree())
                    || (string.equals("ARG") && !tree.isAuxtree()))
            {
                //ok
            }
            else
            {
                System.err.println("wrong tree type: " + string + tree.getStructure(tree.getRoot(), opts.useSemantics));
                errors++;
            }
        }
        return errors;
    }

    /**
     * Converts a MultiValueMap with String values to one with StringTree values.
     * @param treetype 
     * 
     * @param MultiValueMap lexString
     * @return MultiValueMap lexTree
     */
    @SuppressWarnings("unchecked")
    private MultiValueMap makeLexTrees(MultiValueMap lexString, String treetype)
    {
        MultiValueMap<String, ElementaryStringTree> lexTree = new MultiValueMap();
        Set<String> keys = lexString.keySet();
        for (String key : keys)
        {
            Collection<String> values = lexString.getCollection(key);
            HashMap<String, ElementaryStringTree> treelist = new HashMap<String, ElementaryStringTree>();
            for (String treeString : values)
            {
                ElementaryStringTree tree = makeToStringTreeOld(treeString, treetype);
                if (tree == null)
                {
                    continue;
                }
                String POSword = tree.getPOStagged(tree.getRoot()).trim();
                if (key.equals("prediction: "))
                {
                    POSword = key;
                }
                // for lexentries for "put up" etc, add three times into Map: as "put up", "put" and "up".
                String[] words = POSword.split("\t");
                ElementaryStringTree sametree = null;
                if (!treelist.containsKey(POSword + "@@" + tree.toString()))
                {
                    lexTree.put(POSword, tree);
                }
                treelist.put(POSword + "@@" + tree.toString(), tree);
                if (words.length > 1)
                {
                    for (String word : words)
                    {
                        if (sametree == null)
                        {
                            lexTree.put(word, tree);
                        }
                    }
                }
            }
        }
        return lexTree;
    }
    
    private ElementaryStringTree makeToStringTreeOld(String treeString, String treetype)
    {
        if (!treeString.contains("("))
        {
            System.err.println("invalid entry: " + treeString);
            return null;
        }
        IdGenerator idgen = new IdGenerator();
        ElementaryStringTree tree = convertToTree(new ElementaryStringTree(treeString, opts.useSemantics), idgen);
        tree.findChoppedSpine();
        if (tree.getAnchor() == Integer.MIN_VALUE)
//        if (tree.getAnchor().equals(""))
        {
            return null;
        }
        tree.annotateHeadStatus();
        if ((tree.isAuxtree() && treetype.equals("MOD")) || (!tree.isAuxtree() && treetype.equals("ARG")))
        {
        }
        else
        {
            System.err.println("wrong tree type: " + treetype + tree.getStructure(tree.getRoot(), opts.useSemantics));
            return null;
        }
        return tree;
    }

    protected ElementaryStringTree makeToStringTree(String treeString, String unlexString)
    {
        if (!treeString.contains("("))
        {
            System.err.println("invalid entry: " + treeString);
            return null;
        }
        IdGenerator idgen = new IdGenerator();
        ElementaryStringTree tree = convertToTree(new ElementaryStringTree(treeString, opts.useSemantics), idgen);
        if (tree == null)
        {
            return null;
        }
        tree.setTreeString(unlexString);
//		if (treeString.contains(" *")){
//			tree.setTreeString(tree.print());
//		}
//		else tree.setTreeString(treeString);
        //if (tree.getLowerIndex()
        if (tree.getAnchor() == Integer.MIN_VALUE)
//        if (tree.getAnchor().equals(""))
        {
            tree.findChoppedSpine();
        }
        if (tree.getAnchor() == Integer.MIN_VALUE)
//        if (tree.getAnchor().equals(""))
        {
            return null;
        }
        tree.annotateHeadStatus();
        //if ((tree.isAuxtree() && treetype.equals("MOD"))||(!tree.isAuxtree() && treetype.equals("ARG"))){}
        //else {
        //	System.err.println("wrong tree type: " + treetype + tree.getStructure(tree.getRoot()));
        //	return null;
        //}
        return tree;
    }

    /**
     * Converts a MultiValueMap with String values to one with StringTree values.
     * @param lexString
     * @param treetype
     * @return MultiValueMap lexTree
     */
    @SuppressWarnings("unchecked")
    protected MultiValueMap makeLexStrings(MultiValueMap lexString, String treetype)
    {
        MultiValueMap lexTree = new MultiValueMap();
        Set<String> keys = lexString.keySet();
        HashSet<String> treelist = new HashSet<String>();
        for (String key : keys)
        {
            Collection<String> values = lexString.getCollection(key);
            for (String treeString : values)
            {
                //if (tree == null) continue; //need to deal with errors at different point.
                //need to extract POS tag from treestring and unlexicalize tree.
                String posWord = key;
                String tree = treeString;
                if (!key.equals("prediction: "))
                {
                    if (opts.goldPosTags || opts.treeFamilies)
                    {//pos and word given
//                        if (this.getClass() == UnkLexicon.class) // FIX: Unnecessary check
//                        {
//                            posWord = UnkLexicon.getPosFromTreeString(treeString, key);
//                        }
//                        else
//                        {
//                            posWord = getPosFromTreeString(treeString, key);
//                        }                        
                        posWord = getPosFromTreeString(treeString, key);                        
                        if (opts.posOnly)
                        {//only pos tag given
                            String[] words = posWord.split("\t");
                            posWord = "";
                            for (String w : words)
                            {
                                if (w.contains("*") || w.equals("0"))
                                {
                                    continue;
                                }
                                else
                                {
                                    posWord += w.substring(0, w.indexOf(" ")) + "\t";
                                }
                            }
                            posWord = posWord.trim();
                        }
                    }
                    else
                    {// only word
                        posWord = Utils.getCutOffCorrectedMainLex(key.toLowerCase(), listOfFreqWords, opts.train, opts.fullLex);
                        if (key.contains(" "))
                        {
                            posWord = posWord.replace(" ", "\t");
                        }
                    }
                    tree = makeUnlex(treeString, key);
                }
                if (noOfTrees.containsKey(treeString) && posWord.contains(" ") && (opts.goldPosTags || opts.treeFamilies) && !opts.posOnly)
                {
                    String pos = posWord.substring(0, posWord.indexOf(" "));
                    String puretree = pos + "\t" + tree.substring(2);
                    if (noOfTrees.containsKey(puretree))
                    {
                        noOfTrees.put(puretree, noOfTrees.get(puretree) + noOfTrees.get(treeString));
                    }
                    else
                    {
                        noOfTrees.put(puretree, noOfTrees.get(treeString));
                    }
                    noOfTrees.remove(treeString);

                }
                // for lexentries for "put up" etc, add three times into Map: as "put up", "put" and "up".
                String[] words = posWord.split("\t");
                ElementaryStringTree sametree = null;

                if (!treelist.contains(posWord + "@@" + tree) && words.length == 1)
                {
                    String lc = posWord.toLowerCase();
                    // 
                    String wlc = lc.substring(lc.indexOf(" ") + 1);
                    if (!opts.goldPosTags && opts.treeFamilies && !lc.equals("prediction: "))
                    {
                        lexTree.put(wlc, tree);
                    }
                    else
                    {
                        lexTree.put(lc, tree);
                    }
                    if (!wordPosMap.containsValue(wlc, lc))
                    {
                        wordPosMap.put(wlc, lc);
                    }
                    trees.put(tree.substring(tree.indexOf("\t") + 1), lc);
                }
                treelist.add(posWord + "@@" + tree);
                if (words.length > 1)
                {
                    for (String word : words)
                    {
                        if (sametree == null && !word.startsWith(" *T*") && !word.startsWith(" *?*") && !word.startsWith(" *-") && !word.equals(" *") && !word.equals(" 0"))
                        {
                            String lc = word.toLowerCase();
                            String wlc = lc.substring(lc.indexOf(" ") + 1);
                            if (!opts.goldPosTags && opts.treeFamilies)
                            {
                                lexTree.put(wlc, tree);
                            }
                            else
                            {
                                lexTree.put(lc, tree);
                            }
                            if (!wordPosMap.containsValue(wlc, lc))
                            {
                                wordPosMap.put(wlc, lc);
                            }
                            trees.put(tree.substring(tree.indexOf("\t") + 1), lc);
                        } // if
                    } // for
                } // if
            }  // for (values)
        } // for (keys)
        return lexTree;
    }

    protected String makeUnlex(String treeString, String key)
    {
        String[] words = key.split(" ");
        int id = 1;
        String unlexTreeString = treeString;
        for (String word : words)
        {
            unlexTreeString = unlexTreeString.replace(word + "<>", "@LEXEME" + id + "@<>");
            id++;
        }
        return unlexTreeString;
    }

    //currently only for trees with one lex root TODO 
    protected String getPosFromTreeString(String treeString, String key)
    {
        String[] words = key.split(" ");
        StringBuilder posWord = new StringBuilder();
        for (String w : words)
        {            
            String ts = treeString;
            //cut off everything after key word.
            String pos = "";
            if (ts.contains(w + "<>"))
            {
                ts = ts.substring(0, ts.indexOf(w + "<>"));
                pos = ts.substring(ts.lastIndexOf("( ") + 2, ts.lastIndexOf("^"));
//                pos = stripPosAndSemantics(pos)[0]; // if there is any semantic role information, strip it out                
                if (opts.train)
                {
                    w = w.toLowerCase();
                }
                else if (w.equals("@LEXEME1@"))
                {
                    return pos;
                }
                else
                {
                    w = Utils.getCutOffCorrectedMainLex(w.toLowerCase(), listOfFreqWords, opts.train, opts.fullLex);
                }
            }
            if (w.contains("*") || w.contains("0"))
            {
                continue;
            }
            posWord.append(pos).append(" ").append(w).append("\t");
        }
        String posw = posWord.toString();
        posw = posw.trim();
        return posw;
    }


    
    /**
     * Converts a string of the lexicon format into a StringTree.
     * 
     * @param tree
     * @param idgen 
     * @return
     */
    public static ElementaryStringTree convertToTree(ElementaryStringTree tree, IdGenerator idgen)
    {

        String s = tree.getTreeString().trim();
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
                tree = makeNode(parent, Integer.MIN_VALUE, parentId, tree);

                tree.setRoot(parentId);//in recursive process: always overwrite this info! see below.
                for (int i = 1; i < parentAndChildren.length; i++)
                {
                    String node = parentAndChildren[i];
                    int nodeId = idgen.getNewId();
                    tree = makeNode(node, parentId, nodeId, tree);
                }
                tree.setTreeString(s.substring(index + 1));
                return tree;
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
        if (parent.equals("") && treeString.length() > 0) // parse children in bracket recursively
        {
            tree.setTreeString(treeString);
            tree = convertToTree(tree, idgen);
            if (tree == null)
            {
                return null;
            }
            if (treeString.startsWith("  ("))
            {
                //System.out.println("!" + tree.getStructure(tree.getRoot()));
                int rootNodeId = idgen.getNewId();
                tree = makeNode("", Integer.MIN_VALUE, rootNodeId, tree);
                tree.addChild(rootNodeId, tree.getRoot());
                tree.putParent(tree.getRoot(), rootNodeId);
                tree.setRoot(rootNodeId);
            }
        } // if
        else // parse sibling
        {
            treeString = treeString.trim();
            int parentId = idgen.getNewId();
            tree = makeNode(parent, Integer.MIN_VALUE, parentId, tree);
            if (tree == null)
            {
                return null;
            }
            while (!treeString.startsWith(")"))
            {
                if (treeString.startsWith("(")) // parse children in bracket recursively
                {
                    tree.setTreeString(treeString);
                    tree = convertToTree(tree, idgen); // recursion
                    tree.putParent(tree.getRoot(), parentId); // attach parent to children
                    tree.addChild(parentId, tree.getRoot());
                    treeString = tree.getTreeString();
                    treeString = treeString.trim();
                }
                else // parse children within the bracket iteratively
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
                    tree = makeNode(child, parentId, childId, tree);
                }
            }
            if (treeString.startsWith(")"))
            {
                treeString = treeString.substring(1);
                tree.setTreeString(treeString);
                tree.setRoot(parentId);
            }
        } // else

        return tree;
    }

    protected static ElementaryStringTree makeNode(String child, int parentId, int childId, ElementaryStringTree tree)
    {
        tree.makeArraysBigger(childId);
//        tree.makeArraysBigger(Integer.parseInt(childId));
        TagNodeType type = getNodeType(child);
        tree.putNodeType(childId, type);
        if (type == TagNodeType.foot)
        {
            tree.setFootNode(childId);
        }
        else if (type == TagNodeType.anchor)
        {
            if (tree.getAnchor() == Integer.MIN_VALUE)
//            if (tree.getAnchor().equals(""))
            {
                tree.setAnchor(childId);
            }
        }
        else if (type == TagNodeType.internal && child.substring(0, child.indexOf("^")).matches("[a-z]+"))
        {
            tree.setSubcategorisedAnchor(childId);
        }
        child = removeAnnotation(child);
        child = tree.getIndices(childId, child);
//        child = tree.getIndices(Integer.parseInt(childId), child);
        if (child == null)
        {
            return null; //invalid indices.
        }
        child = tree.getHeadAnnotation(childId, child);
//        child = tree.getHeadAnnotation(Integer.parseInt(childId), child);
        if (parentId != Integer.MIN_VALUE)
//        if (!parentId.equals(""))
        {
            tree.addChild(parentId, childId);
            tree.putParent(childId, parentId);
        }
        tree.putFullCategory(childId, child);
        String childShort = child;
        if (child.matches(".*-[A-Z0-9].*") && !child.equals("--") && tree.getNodeType(childId) != TagNodeType.anchor)
        {
            childShort = child.substring(0, child.indexOf("-"));
        }
        tree.putCategory(childId, childShort);

        return tree;
    }

    protected static TagNodeType getNodeType(String parent)
    {
        TagNodeType type;
        if (parent.indexOf("<>") >= 0)
        {
            type = TagNodeType.anchor;
        }
        else if (parent.endsWith("!"))
        {
            type = TagNodeType.subst;
        }
        else if (parent.endsWith("_null*"))
        {
            type = TagNodeType.foot;
        }
        else
        {
            type = TagNodeType.internal;
        }

        return type;
    }

    protected static String removeAnnotation(String parent)
    {
        if (parent.indexOf("<") >= 0)
        {
            parent = parent.substring(0, parent.indexOf("<"));
        }
        else if (parent.indexOf("!") > 0)
        {
            parent = parent.substring(0, parent.indexOf("!"));
        }
        else if (parent.indexOf("*") > 0 && getNodeType(parent) == TagNodeType.foot)
        {
            parent = parent.substring(0, parent.indexOf("*"));
        }
        return parent;
    }
    
    protected MultiValueMap[] read(String filename)
    {
        return read(Utils.readLines(filename));        
    }

    /**
     * Reads the lexicon file and sorts entries by their type (arg or mod).
     * For each of those types, it creates a MultiValueMap that's keyed on the lexeme, and whose
     * values are the Strings that represent the trees.
     * 
     * @param lines
     * @return a MultiValueMap Array, with the arg string lexicon in first position, and 
     * mod string lexicon in second position. 
     */
    protected MultiValueMap<String, ?>[] read(String[] lines)
    {        
        MultiValueMap<String, String> modLexentriesString = new MultiValueMap();
        MultiValueMap<String, String> argLexentriesString = new MultiValueMap();        
        for(String line : lines)
        {                        
            String[] lexcontent = Utils.getCatInventory(line.trim(), opts.combineNNVBcats).split("\t+");
            int freq = Integer.parseInt(lexcontent[0]);
            if (lexcontent[3].contains("<>"))
            {
                String endswithLex = lexcontent[3].substring(0, lexcontent[3].indexOf("<>"));
                String anchor = endswithLex.substring(endswithLex.lastIndexOf(" ") + 1);
                if (!biWordMap.containsKey(anchor))
                {
                    biWordMap.put(anchor, freq);
                }
                else
                {
                    biWordMap.put(anchor, biWordMap.get(anchor) + freq);//*/
                }
                if (lexcontent[3].contains("1_1)"))
                {
                    endswithLex = lexcontent[3].substring(0, lexcontent[3].indexOf("1_1)") - 1);
                    anchor += "%" + endswithLex.substring(endswithLex.lastIndexOf(" ") + 1);
                    if (!biWordMap.containsKey(anchor))
                    {
                        biWordMap.put(anchor, freq);
                    }
                    else
                    {
                        biWordMap.put(anchor, biWordMap.get(anchor) + freq);//*/
                    }
                    anchor = "UNK%" + endswithLex.substring(endswithLex.lastIndexOf(" ") + 1);
                    if (!biWordMap.containsKey(anchor))
                    {
                        biWordMap.put(anchor, 1);
                    }
                    else
                    {
                        biWordMap.put(anchor, biWordMap.get(anchor) + 1);//*/
                    }
                    if (!biWordMap.containsKey("UNK"))
                    {
                        biWordMap.put("UNK", 1);
                    }
                    else
                    {
                        biWordMap.put("UNK", biWordMap.get("UNK") + 1);
                    }
                }

            }
            if (lexcontent.length < 4)
            {
                if (opts.verbose)
                {
                    System.out.println("wrong lex");
                }
            }
            lexcontent = lexEntryRemoveDigits(lexcontent);
            int baumAnz = Integer.parseInt(lexcontent[0]);
            if (lexcontent[0].equals("1"))
            {
                lexcontent[0] = "0";
                //continue;
            }
            else if (!opts.freqBaseline)
            {
                lexcontent[0] = "1";
            }
            String word = lexcontent[1];
//            String wordNoSemantics = stripSemanticFrame(lexcontent[1]);
            String val = lexcontent[0] + "\t" + lexcontent[3];
            if (lexcontent[2].equals("ARG"))
            {
//                if (!lexcontent[1].equals("NUM") || !argLexentriesString.containsValue(lexcontent[1], lexcontent[0].toString() + "\t" + lexcontent[3].toString()))
                if (!lexcontent[1].equals("NUM") || !argLexentriesString.containsValue(word, lexcontent[0].toString() + "\t" + lexcontent[3].toString()))
                {
//                    argLexentriesString.put(lexcontent[1], val);
                    argLexentriesString.put(word, val);
                }
            }
            else if (lexcontent[2].equals("MOD"))
            {
//                if (!lexcontent[1].equals("NUM") || !modLexentriesString.containsValue(lexcontent[1], lexcontent[0].toString() + "\t" + lexcontent[3].toString()))
                if (!lexcontent[1].equals("NUM") || !modLexentriesString.containsValue(word, lexcontent[0].toString() + "\t" + lexcontent[3].toString()))
                {
                    modLexentriesString.put(word, val);
//                    modLexentriesString.put(lexcontent[1], val);
                }
            }
            else
            {
                System.err.println("Incorrect Lexicon format: line " + line);
            }
//            String posword = getPosFromTreeString(lexcontent[3], lexcontent[1]).toLowerCase();
            String posword = getPosFromTreeString(lexcontent[3], word).toLowerCase();
            if (posTagNo.containsKey(posword))
            {
                posTagNo.put(posword, posTagNo.get(posword) + baumAnz);
            }
            else
            {
                posTagNo.put(posword, baumAnz);
            }
            if (noOfTrees.containsKey(val))
            {
                this.noOfTrees.put(val, noOfTrees.get(val) + baumAnz);
            }
            else
            {
                this.noOfTrees.put(val, baumAnz);
            }
        }
        
        MultiValueMap<String, String>[] entries = new MultiValueMap[2];
        entries[0] = argLexentriesString;
        entries[1] = modLexentriesString;
        return entries;
    }
    protected String[] lexEntryRemoveDigits(String[] lexcontent)
    {

        lexcontent[3] = lexcontent[3].replaceAll("[)][)]", ") )");
        String[] lexemes = lexcontent[3].split("[)]");
        String resultsentence = "";
        for (String l : lexemes)
        {
            if (l.contains(" "))
            {
                String w = l.substring(l.lastIndexOf(" "), l.length());
                String indeces = "";
                if (w.contains("^"))
                {
                    indeces = w.substring(w.indexOf("^"));
                    w = w.substring(0, w.indexOf("^"));
                }
                String pref = l.substring(0, l.lastIndexOf(" "));
                if (w.contains("*"))
                {
                }
                else if (digits.matcher(w).matches() && !(w.equals(" 0") && pref.contains(":")) && !w.contains("@"))
                {
                    w = toNUM(w);
                    lexcontent[1] = toNUM(lexcontent[1]);
                    if (digits.matcher(w).matches() && !(w.equals(" 0") && pref.contains(":")))
                    {
                        w = toNUM(w);
                        lexcontent[1] = toNUM(lexcontent[1]);
                        if (digits.matcher(w).matches() && !(w.equals(" 0") && pref.contains(":")))
                        {
                            w = toNUM(w);
                            lexcontent[1] = toNUM(lexcontent[1]);
                        }
                    }
                }
                resultsentence += pref + w + indeces + ")";
            }
            else
            {
                resultsentence += l + ")";
            }
        }
        resultsentence += ")";
        resultsentence = resultsentence.replaceAll("[)] [)]", "))");
        lexcontent[3] = resultsentence;
        return lexcontent;
    }

    private String toNUM(String w)
    {
        Matcher matchedexp = digits.matcher(w);
        String numw = matchedexp.replaceAll("$1NUM$2");
        return numw;
    }

    public boolean containsKey(String word)
    {
       return lexEntriesTree.containsKey(word);
//        if (lexEntriesTree.containsKey(word))
//        {
//            return true;
//        }
//        return false;
    }

    @SuppressWarnings("unchecked")
    public Collection<ElementaryStringTree> getEntries(String word, String wCor, String posTag, boolean noAnalysisParse, int wno)
    {
        // expand into ElementaryStringTrees! =
        Collection<ElementaryStringTree> treesOut = new ArrayList<ElementaryStringTree>();
        Collection<String> treeStrings = new ArrayList<String>();
        if (lexEntriesTree.isEmpty())
        {
            return treesOut;
        }
        String searchWord = wCor;//word.toLowerCase();
        if (!lexEntriesTree.containsKey(searchWord))
        {
            searchWord = "";
            if (opts.goldPosTags)
            {
                for (String w : posTag.split("\t"))
                {
                    searchWord += w.toLowerCase() + " unk";
                }
            }
            else
            {
                searchWord += "unk";
            }
        }
        if (!lexEntriesTree.containsKey(searchWord)) // TODO: FIX
        {
            return treesOut;
        }        
        //System.out.println(trees.size()+ "\t"+ wordPosMap.getCollection(searchWord.substring(searchWord.indexOf(" ")+1)).size());
        for (String treeString : (Collection<String>) lexEntriesTree.getCollection(searchWord))
        {
            if (treeString.contains("LEXEME1"))
            {
                String postag1 = treeString.substring(0, treeString.indexOf(" @LEXEME1@"));
                posTag = postag1.substring(postag1.lastIndexOf("(") + 2, postag1.lastIndexOf("^"));
            }
            String sts = posTag + "\t" + treeString.substring(treeString.indexOf("\t") + 1);
            if (//StatsRunner.fullLex
                    this.lexEntriesTree.size() > 100
                    && ((!noOfTrees.containsKey(sts) && treeString.contains("^x"))
                    || (noOfTrees.containsKey(sts) && noOfTrees.get(sts) < 3 && treeString.contains("^x"))))
            {
                if (!noAnalysisParse)
                {
                    continue;
                }
            }
            if (num.matcher(word).matches() && !posTag.equals("CD"))
            {
                continue;
            }
            if (!word.contains("NUM") && upperCase.matcher(word).matches() && wno != 0 && (!posTag.startsWith("NN") && !posTag.startsWith("JJ")) && !word.equals("I"))
            {
                continue;
            }
            String unlexTreeString = treeString;
            treeString = insertLex(word,  treeString);
            ElementaryStringTree tree = makeToStringTree(treeString, unlexTreeString);
            if (tree != null && !treeStrings.contains(tree.getTreeString().substring(2)))
            {
                treesOut.add(tree);
                treeStrings.add(tree.getTreeString().substring(2));
            }
        } // for
//		System.out.print(trees.size()+" lexTrees\t");
        if (treesOut.size() > 20)
        {
            return treesOut;
        }
        HashMap<String, Integer> posTags = new HashMap<String, Integer>();
        if (!posTag.equals("") && !posTag.equals("N/A") && opts.treeFamilies && !searchWord.equals("unk"))
        {//don't do this for prediction trees.
            if (!opts.goldPosTags)
            {
                posTags = getPosTags(treeStrings, searchWord);
            }
            else
            {
                posTags.put(posTag, 1);
            }
//			System.out.print(postags.size()+"postags\t");
            if (posTags.size() > 1)
            {
                for (String ptag : posTags.keySet())
                {
                    if (lexEntriesTree.containsKey(ptag))
                    {
                        for (String treeString : (Collection<String>) lexEntriesTree.getCollection(ptag))
                        {
                            String unlexTreeString = treeString;
                            treeString = insertLex(word, treeString);
                            ElementaryStringTree tree = makeToStringTree(treeString, unlexTreeString);
                            String ts = tree.getTreeString().substring(2);
                            if (tree != null && !treeStrings.contains(ts) && noOfTrees.get(ptag + "\t" + unlexTreeString.substring(unlexTreeString.indexOf("\t") + 1)) > 100)
                            {
                                //System.out.println(noOfTrees.get(ptag+"\t"+unlexTreeString.substring(unlexTreeString.indexOf("\t")+1))+"\t"+ts);
                                treesOut.add(tree);
                                treeStrings.add(ts);
                            }
                        } // for
                    } // if
                } // for
            } // if
        } // if
//		System.out.println(trees.size());
        if (treesOut.size() > 6)
        {
            return treesOut;
        }
        else //correct for bad gold pos tag.
        {
            if (!searchWord.equals("prediction: ") && opts.goldPosTags && opts.fullLex)
            {
                posTags = getPosTags(treeStrings, searchWord);
                //postags = wordPosMap.getCollection(searchWord.substring(searchWord.indexOf(" ")+1));
            }
            posTags.remove(searchWord);
        }
        for (String sw : posTags.keySet())
        {
            if (lexEntriesTree.containsKey(sw))
            {
                for (String treeString : (Collection<String>) lexEntriesTree.getCollection(sw))
                {
                    //if (treeString.startsWith("1") && searchWord.endsWith("unk"))
                    //	continue;
                    String unlexTreeString = treeString;
                    treeString = insertLex(word, treeString);
                    ElementaryStringTree tree = makeToStringTree(treeString, unlexTreeString);
                    if (tree != null && !treeStrings.contains(tree.getTreeString().substring(2)))
                    {
                        treesOut.add(tree);
                        treeStrings.add(tree.getTreeString().substring(2));
                    }
                }
            }
        }
        return treesOut;
    }

    protected HashMap<String, Integer> getPosTags(Collection<String> treeStrings, String searchWord)
    {
        int maxfreq = 0;
        HashMap<String, Integer> postags = new HashMap<String, Integer>();
        for (String ts : treeStrings)
        {
            String candpostag = getPosFromTreeString(ts, "@LEXEME1@");
            if (!postags.keySet().contains(candpostag))
            {
                Integer freq = posTagNo.get(candpostag.toLowerCase() + " " + searchWord);
                if (freq == null)
                {
                    freq = 1;
                }
                postags.put(candpostag, freq);
                if (freq > maxfreq)
                {
                    maxfreq = freq;
                }
            }
        }
        ArrayList<String> pt = new ArrayList<String>();
        pt.addAll(postags.keySet());
        for (String key : pt)
        {
            if (postags.get(key) * 50 < maxfreq)
            {
                postags.remove(key);
            }
        }
        return postags;
    }

    protected String insertLex(String word, String tree)
    {
        String[] words = word.split("\t");
        String treeString = tree;
        int id = 1;
        for (String w : words)
        {
            if (w.contains(" "))
            {
                w = w.substring(w.indexOf(" ") + 1, w.length());
            }
            treeString = treeString.replace("@LEXEME" + id + "@<>", w + "<>");
            id++;
        }
        while (treeString.contains("LEXEME"))
        {
            //		System.out.println("incorrect lexicon entry "+treeString);
            //insertLex(word, tree);
            treeString = treeString.replace("@LEXEME" + id + "@<>", "*^x_x");
            id++;
        }
        return treeString;
    }

    public void lexiconReduce()
    {
        ArrayList<String> removelist = new ArrayList<String>();
        for (String tree : trees.keySet())
        {
            Collection<String> words = trees.getCollection(tree);
            if (words.size() == 1)
            {
                String wd = words.iterator().next();
                String success = (String) this.lexEntriesTree.remove(wd, "0\t" + tree);
                if (success != null)
                {
                    removelist.add(tree);
                }
            }
        }
        for (String t : removelist)
        {
            trees.remove(t);
        }
    }

    public void getFamily(String string)
    {
        HashMap<String, Integer> similars = new HashMap<String, Integer>();
        int most = 0;
        HashSet<String> mostSimilar = new HashSet<String>();
        for (String tree : (Collection<String>) lexEntriesTree.getCollection(string))
        {
            if (!this.noOfTrees.containsKey(tree) || noOfTrees.get(tree) < 5)
            {
                continue;
            }
            String t = tree.substring(tree.indexOf("\t") + 1);
            for (String assoc : trees.getCollection(t))
            {
                if (assoc.contains(" unk") || assoc.contains(string))
                {
                    continue;
                }
                if (similars.containsKey(assoc))
                {
                    int newNum = similars.get(assoc) + 1;
                    similars.put(assoc, newNum);
                    if (newNum > most)
                    {
                        most = newNum;
                        mostSimilar.clear();
                        mostSimilar.add(assoc);
                    }
                    if (newNum == most)
                    {
                        mostSimilar.add(assoc);
                    }
                }
                else
                {
                    similars.put(assoc, 1);
                }
            }
        }
        HashSet<String> simtrees = new HashSet<String>();
        for (String mostSimWords : mostSimilar)
        {
            simtrees.addAll((Collection<String>) lexEntriesTree.getCollection(mostSimWords));

        }
        System.out.println(mostSimilar + "\t");
        System.out.print(simtrees.toString() + "\n");
    }

    public void removeHelps()
    {
        numOfTreeTemplates = trees.keySet().size();
//        numOfTreeTemplates = 6410;
        trees = null;
        //this.noOfTrees = null;
    }

    public static int getNumOfTreeTemps()
    {
        return numOfTreeTemplates;
    }

    public int getLexSize()
    {
//        if (!opts.fullLex)
//        {
//            return 9671; //System.out.println(1.0/wordPosMap.keySet().size());
//        }
        return wordPosMap.keySet().size();
    }

    public int getPosTagNo(String word)
    {
        if (this.posTagNo.containsKey(word))
        {
            return this.posTagNo.get(word);
        }
        return 0;
    }

    public Collection<String> getPOSs(String word)
    {
        return wordPosMap.getCollection(word.toLowerCase());
    }

    public static double lexWord(String category, String predictedLex)
    {
        double a, b;        
        if (biWordMap.containsKey(category + "%" + predictedLex))
        {
            a = biWordMap.get(category + "%" + predictedLex).doubleValue();
            b = biWordMap.get(category).doubleValue();
        }
        else
        {
            a = biWordMap.get("UNK%" + predictedLex).doubleValue();
            b = biWordMap.get("UNK").doubleValue();
        }
        return a / b;
    }

    public MultiValueMap<String, ElementaryStringTree> getLexEntriesContaining(String category)
    {
        MultiValueMap<String, ElementaryStringTree> out = new MultiValueMap();
        for(String key : wordPosMap.keySet())
        {
            String firstWordPos = wordPosMap.getCollection(key).iterator().next();
            Collection<ElementaryStringTree> col = getEntries(key, firstWordPos, firstWordPos.split(" ")[0], false, 0);
            for(ElementaryStringTree e : col)
            {
                if(e.toString().contains(" " +category + "^"))
                    out.put(key, e);
            }
        }
        return out;
    }
    
    public Collection<Pair<String, String>> getTreeWithRootCategory(String rootCategory)
    {
        return (Collection<Pair<String, String>>) rootNodeTreeMap.get(rootCategory);
    }
    
    protected Collection<?> getEntries(String word)
    {
        return lexEntriesTree.getCollection(word);
    }
}
