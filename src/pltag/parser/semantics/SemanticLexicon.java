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

import fig.basic.IOUtils;
import fig.basic.Indexer;
import fig.basic.LogInfo;
import fig.basic.Pair;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.corpus.ElementaryStringTree;
import pltag.corpus.IdGenerator;
import pltag.corpus.TagNodeType;
import pltag.parser.Lexicon;
import pltag.parser.Options;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public class SemanticLexicon extends Lexicon
{
    
    SemanticLexicon lexiconWithAllRoles;
    private final Indexer<String> roleIndexer;
    
    public SemanticLexicon(Options opts, Set<String> listOfFreqWords, Indexer<String> roleIndexer)
    {
        super(opts, listOfFreqWords);
        this.roleIndexer = roleIndexer;
    }  
    
    public SemanticLexicon(Options opts, Set<String> listOfFreqWords, Indexer<String> roleIndexer, SemanticLexicon lexiconWithAllRoles)
    {
        super(opts, listOfFreqWords);
        this.lexiconWithAllRoles = lexiconWithAllRoles;
        this.roleIndexer = roleIndexer;
    }  

    @Override
    public void processLexicon(String filename)
    {
        MultiValueMap<String, ?>[] entries = read(filename);        
        lexEntriesTree = makeLexTrees(entries[0], null);        
    }
    
    @Override
    public void processLexicon(String[] lines)
    {
        if(lines.length == 1 && lines[0].equals("")) // empty lexicon (occasionally in predicted lexicon)
        {                
            lexEntriesTree = new MultiValueMap<String, String>();
        }
        else
        {
            MultiValueMap<String, ?>[] entries = read(lines);
            lexEntriesTree = makeLexTrees(entries[0], null);
        }        
    }
    
    @Override
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
                    String[] posUnlexTree = key.split("\t");
                    MultiValueMap temp = new MultiValueMap();
                    for(Object obj : lexEntriesTree.values())
                    {
                        LexiconEntryWithRoles entry = (LexiconEntryWithRoles)obj;
                        if(entry.getUnlexEntry().equals(posUnlexTree[1]))
                        {
                            updateEntryWithRolesMap(temp, posUnlexTree[0], entry);                            
                        }
                    }
                    lexEntriesTree.putAll(temp);
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
    
    @Override
    protected void extractVerificationTrees()
    {
        for(String word : lexEntriesTree.keySet())
        {
            for(LexiconEntryWithRoles entry : (Collection<LexiconEntryWithRoles>)lexEntriesTree.getCollection(word))
            {
                String rootCategory = getRootCategory(entry.getUnlexEntry());
                rootNodeTreeMap.put(rootCategory, new Pair(word, entry.toString()));
            }
        }        
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
    @Override
    protected MultiValueMap<String, ?>[] read(String[] lines)
    {                     
        MultiValueMap<String, LexiconEntryWithRoles> lexEntries = new MultiValueMap(); 
        // temporary map that aggregates frequencies of trees with stripped semantics
        Map<String, Integer> lexTreesFreqs = new HashMap<String, Integer>(); 
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
            } // if
            if (lexcontent.length < 4)
            {
                if (opts.verbose)
                {
                    LogInfo.error("wrong lex");
                }
            }
            lexcontent = lexEntryRemoveDigits(lexcontent);
            int frequency = Integer.parseInt(lexcontent[0]);
            boolean isRelation = hasSemanticFrame(lexcontent[1]);
            String anchorNoSemantics = stripSemanticFrame(lexcontent[1]);            
            String treeNoSemantics = stripSemantics(lexcontent[3]);
            String posword = getPosFromTreeString(treeNoSemantics, anchorNoSemantics).toLowerCase();
            Integer oldFreq = lexTreesFreqs.get(treeNoSemantics);
            if(oldFreq != null)
            {
                lexTreesFreqs.put(treeNoSemantics, frequency + oldFreq);
            }
            else
            {
                lexTreesFreqs.put(treeNoSemantics, frequency);
            }
            String rawUnlexEntry = makeUnlex(lexcontent[3], anchorNoSemantics);            
//            LexiconEntryWithRoles entry = new LexiconEntryWithRoles(frequency, lexcontent[3], treeNoSemantics);
            LexiconEntryWithRoles entry = new LexiconEntryWithRoles(frequency, rawUnlexEntry, treeNoSemantics, isRelation, roleIndexer, opts.freqBaseline);
//            if (!lexcontent[1].equals("NUM"))
//            {
                updateEntryWithRolesMap(lexEntries, anchorNoSemantics, entry, rawUnlexEntry);
//            } // if
//            else
//            {
//                LogInfo.error("Incorrect Lexicon format: line " + line);
//            }
            if (posTagNo.containsKey(posword))
            {
                posTagNo.put(posword, posTagNo.get(posword) + frequency);
            }
            else
            {
                posTagNo.put(posword, frequency);
            }            
        } // for
        // update noOfTrees map with correct aggregated frequencies
        for(Map.Entry<String, Integer> entry : lexTreesFreqs.entrySet())
        {
            String val = (entry.getValue() > 1 ? 
                    (opts.freqBaseline ? String.valueOf(entry.getValue()) : "1") : "0") + 
                    "\t" + entry.getKey();
            noOfTrees.put(val, entry.getValue());
        } // for                
        return new MultiValueMap[] {lexEntries};
    }
      
    /**
     * Converts a MultiValueMap with String values to one with StringTree values.
     * @param lexEntriesWithOrigWordsAsKeys
     * @param treetype
     * @return MultiValueMap lexTree
     */
    @SuppressWarnings("unchecked")
    protected MultiValueMap makeLexTrees(MultiValueMap<String, ?> lexEntriesWithOrigWordsAsKeys, String treetype)
    {
        MultiValueMap<String, LexiconEntryWithRoles> unlexEntriesWithNormWordsAsKeys = new MultiValueMap();
//        HashSet<String> unlexTreeList = new HashSet<String>();
        Map<String, String> origWordsToNormWordsMap = new HashMap<String, String>();
        
        for (String key : lexEntriesWithOrigWordsAsKeys.keySet())
        {            
            for (LexiconEntryWithRoles e : (Collection<LexiconEntryWithRoles>)lexEntriesWithOrigWordsAsKeys.getCollection(key))
            {
                //need to extract POS tag from treestring and unlexicalize tree.
                LexiconEntryWithRoles entry = new LexiconEntryWithRoles(e);
                String posWord = key;
//                String treeString = entry.getLexEntry();
                String treeString = entry.toString();
                String unlexTree = treeString;
                if (!key.equals("prediction: "))
                {
                    posWord = processKey(treeString, key);
                    unlexTree = makeUnlex(treeString, key);
//                    entry.setUnlexEntry(unlexTree.substring(unlexTree.indexOf("\t") + 1));
                }
                entry.setUnlexEntry(unlexTree.substring(unlexTree.indexOf("\t") + 1));
                updateNoOfTreesMap(treeString, posWord, unlexTree);
                String[] words = posWord.split("\t");                
//                if (!unlexTreeList.contains(posWord + "@@" + unlexTree) && words.length == 1)
                if (words.length == 1)
                {
                    int tempIndex = key.indexOf(" ");
                    String origWord = tempIndex != -1 ? key.substring(0, tempIndex) : key; // key with original case
                    String posWordLowerCase = posWord.toLowerCase();
                    String wordLowerCase = posWordLowerCase.substring(posWordLowerCase.indexOf(" ") + 1);
                    if (!opts.goldPosTags && opts.treeFamilies && !posWordLowerCase.equals("prediction: "))
                    {
//                        lexTree.put(wlc, unlexTree);
                        updateEntryWithRolesMap(unlexEntriesWithNormWordsAsKeys, wordLowerCase, entry);
//                        origWordsToNormWordsMap.put(posWord.substring(posWord.indexOf(" ") + 1), wordLowerCase);
                        origWordsToNormWordsMap.put(origWord, wordLowerCase);
                    }
                    else
                    {
//                        lexTree.put(lc, unlexTree);
                        updateEntryWithRolesMap(unlexEntriesWithNormWordsAsKeys, posWordLowerCase, entry);
                        origWordsToNormWordsMap.put(posWord.substring(0, posWord.indexOf(" ") + 1) + origWord, posWordLowerCase);                        
                    }
                    if (!wordPosMap.containsValue(wordLowerCase, posWordLowerCase))
                    {
                        wordPosMap.put(wordLowerCase, posWordLowerCase);
                    }
                    trees.put(unlexTree.substring(unlexTree.indexOf("\t") + 1), posWordLowerCase);
                } // if
//                unlexTreeList.add(posWord + "@@" + unlexTree);
                // for lexentries for "put up" etc, add three times into Map: as "put up", "put" and "up".
                if (words.length > 1)
                {
                    for (String word : words)
                    {
                        if (!(word.startsWith(" *T*") || word.startsWith(" *?*") || 
                              word.startsWith(" *-") || word.equals(" *") || word.equals(" 0")))
                        {
                            String lc = word.toLowerCase();
                            String wlc = lc.substring(lc.indexOf(" ") + 1);
                            if (!opts.goldPosTags && opts.treeFamilies)
                            {
//                                lexTree.put(wlc, unlexTree);
                                updateEntryWithRolesMap(unlexEntriesWithNormWordsAsKeys, wlc, entry);
                                origWordsToNormWordsMap.put(word.substring(word.indexOf(" ") + 1), wlc);
                            }
                            else
                            {
//                                lexTree.put(lc, unlexTree);
                                updateEntryWithRolesMap(unlexEntriesWithNormWordsAsKeys, lc, entry);
                                origWordsToNormWordsMap.put(word, lc); // TODO: FIX Not handling potential uppercase partial lexemes correctly
                            }
                            if (!wordPosMap.containsValue(wlc, lc))
                            {
                                wordPosMap.put(wlc, lc);
                            }
                            trees.put(unlexTree.substring(unlexTree.indexOf("\t") + 1), lc);
                        } // if
                    } // for
                } // if
            }  // for (values)
        } // for (keys)
        
//        MultiValueMap<String, ElementaryStringTree> stringTreeEntriesWithOrigWordsAsKeys = new MultiValueMap<String, ElementaryStringTree>();
//        for (Map.Entry<String, String> e : origWordsToNormWordsMap.entrySet())
//        {                        
//            Collection<EntryWithRoles> unlexEntries = unlexEntriesWithNormWordsAsKeys.getCollection(e.getValue());
//            // we need to convert to StringTree instances as well
//            for(LexiconEntryWithRoles entry : unlexEntries)
//            {
//                String unlexTreeString = entry.toString();
//                String treeString = insertLex(e.getKey(), unlexTreeString);
//                ElementaryStringTree tree = makeToStringTree(treeString, unlexTreeString, entry.getUnlexEntriesWithSemantics());
//                stringTreeEntriesWithOrigWordsAsKeys.put(e.getKey(), tree);
//            }            
//        }
//        return stringTreeEntriesWithOrigWordsAsKeys;
        //printEntriesWithComplexRolesStats(unlexEntriesWithNormWordsAsKeys);
        return unlexEntriesWithNormWordsAsKeys;
    }

    private void printEntriesWithComplexRolesStats(MultiValueMap<String, LexiconEntryWithRoles> unlexEntriesWithNormWordsAsKeys)
    {
        for(String key : unlexEntriesWithNormWordsAsKeys.keySet())
        {
            StringBuilder treesStr = new StringBuilder();
            for(LexiconEntryWithRoles tree : unlexEntriesWithNormWordsAsKeys.getCollection(key))
            {
                if(tree.getRoles().size() > 0)
                {   
                    boolean treeMoreThanOneRole = false;
                    StringBuilder treeStr = new StringBuilder("\n");
                    for(RoleSignature sig : tree.getRoles())
                    {
                        treeStr.append(sig).append(", ");
                        if(sig.numOfRoles() > 1)
                        {
                            treeMoreThanOneRole = true;
                        } // if
                    } // for
                    if(treeMoreThanOneRole && tree.getRoles().size() > 1)
                        treesStr.append("\t").append(treeStr);
                } // if
            } // for
            if(treesStr.length() > 0)
                System.out.println(key + treesStr);
        }
    }
    
    private void updateNoOfTreesMap(String treeString, String posWord, String unlexTree)
    {
        if (!noOfTrees.containsKey(treeString))
        {
            String prefix = treeString.charAt(0) == '0' ? "1" : "0"; // switch potentially incorrect count prefix (it is 0 for freqs=1, 1 otherwise)
            treeString = prefix + treeString.substring(1);
        }
        if (noOfTrees.containsKey(treeString) && posWord.contains(" ") 
                && (opts.goldPosTags || opts.treeFamilies) && !opts.posOnly)
        {
            String pos = posWord.substring(0, posWord.indexOf(" "));
            String puretree = pos + "\t" + unlexTree.substring(2);
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
    }
    
    private void updateEntryWithRolesMap(MultiValueMap<String, LexiconEntryWithRoles> map, String key, LexiconEntryWithRoles entry)
    {
        if(lexiconWithAllRoles != null) // oracleAllRoles: expand entry with roles from the full lexicon (not just with roles from the gold standard tree)
        {
            Collection<LexiconEntryWithRoles> col = ((MultiValueMap<String, LexiconEntryWithRoles>) lexiconWithAllRoles.lexEntriesTree).getCollection(key);
            if(col != null && col.contains(entry))
            {
                for(LexiconEntryWithRoles e : col)
                {
                    if(entry.equals(e)) // syntactically same tree with (potentially) different role assignments
                    {
                        entry.addEntry(e); // add roles to new entry from the existing lexicon entry
                        break;
                    }
                }
            }
        }
        Collection<LexiconEntryWithRoles> col = map.getCollection(key);
        if(col != null && col.contains(entry))
        {                                        
            for(LexiconEntryWithRoles e : col)
            {
                if(entry.equals(e)) // syntactically same tree with (potentially) different role assignments
                {
                    e.addEntry(entry);
                    break;
                }
            } // for                    
        } // if
        else
        {
            map.put(key, entry);
        }        
    }
    
    private void updateEntryWithRolesMap(MultiValueMap<String, LexiconEntryWithRoles> map, String key, LexiconEntryWithRoles entry, String unlexEntryWithSemantics)
    {
        Collection<LexiconEntryWithRoles> col = map.getCollection(key);                   
        if(col != null && col.contains(entry))
        {                                        
            for(LexiconEntryWithRoles e : col)
            {
                if(entry.equals(e)) // syntactically same tree with (potentially) different role assignments
                {
                    e.addEntry(entry, unlexEntryWithSemantics);
                    break;
                }
            } // for                    
        } // if
        else
        {
            map.put(key, entry);
        }
    }
    
    private String processKey(String treeString, String key)
    {
        String posWord;
        if (opts.goldPosTags || opts.treeFamilies)
        {
            posWord = getPosFromTreeString(treeString, key);                        
            if (opts.posOnly)
            {//only pos tag given
                String[] words = posWord.split("\t");
                posWord = "";
                for (String w : words)
                {
                    if (!(w.contains("*") || w.equals("0")))                                
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
        return posWord;
    }
    
    @Override
    public Collection<ElementaryStringTree> getEntries(String word, String wCor, String posTag, 
            boolean noAnalysisParse, int wno)
    {        
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
        for (LexiconEntryWithRoles entry : (Collection<LexiconEntryWithRoles>) lexEntriesTree.getCollection(searchWord))
        {
            String treeString = entry.toString();
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
            treeString = insertLex(word, treeString);
            ElementaryStringTree tree = makeToStringTree(treeString, unlexTreeString, entry);
            if (tree != null && !treeStrings.contains(tree.getTreeString().substring(2)))
            {
                treesOut.add(tree);
                treeStrings.add(tree.getTreeString().substring(2));
            }
        } // for
        if (treesOut.size() > 20)
        {
            return treesOut;
        }
        HashMap<String, Integer> posTags = new HashMap<String, Integer>();
        if (!posTag.equals("") && opts.treeFamilies && !searchWord.equals("unk"))
        {//don't do this for prediction trees.
            if (!opts.goldPosTags)
            {
                posTags = getPosTags(treeStrings, searchWord);
            }
            else
            {
                posTags.put(posTag, 1);
            }
            if (posTags.size() > 1)
            {
                for (String ptag : posTags.keySet())
                {
                    if (lexEntriesTree.containsKey(ptag))
                    {
                        for (LexiconEntryWithRoles entry : (Collection<LexiconEntryWithRoles>) lexEntriesTree.getCollection(ptag))
                        {
                            String treeString = entry.toString();
                            String unlexTreeString = treeString;
                            treeString = insertLex(word, treeString);
                            ElementaryStringTree tree = makeToStringTree(treeString, unlexTreeString, entry);
                            String ts = tree.getTreeString().substring(2);
                            if (!treeStrings.contains(ts) && noOfTrees.get(ptag + "\t" + unlexTreeString.substring(unlexTreeString.indexOf("\t") + 1)) > 100)
                            {
                                treesOut.add(tree);
                                treeStrings.add(ts);
                            }
                        } // for
                    } // if
                } // for
            } // if
        } // if
        if (treesOut.size() > 6)
        {
            return treesOut;
        }
        else //correct for bad gold pos tag.
        {
            if (!searchWord.equals("prediction: ") && opts.goldPosTags && opts.fullLex)
            {
                posTags = getPosTags(treeStrings, searchWord);                
            }
            posTags.remove(searchWord);
        }
        for (String sw : posTags.keySet())
        {
            if (lexEntriesTree.containsKey(sw))
            {
                for (LexiconEntryWithRoles entry : (Collection<LexiconEntryWithRoles>) lexEntriesTree.getCollection(sw))
                {
                    String treeString = entry.toString();
                    String unlexTreeString = treeString;
                    treeString = insertLex(word, treeString);
                    ElementaryStringTree tree = makeToStringTree(treeString, unlexTreeString, entry);
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
    
    protected ElementaryStringTree makeToStringTree(String treeString, String unlexString, LexiconEntryWithRoles entry)
    {
        if (!treeString.contains("("))
        {
            LogInfo.error("invalid entry: " + treeString);
            return null;
        }
        IdGenerator idgen = new IdGenerator();        
        ElementaryStringTree tree = convertToTree(new ElementaryStringTree(treeString, opts.useSemantics, entry.getRoles(), entry.isRelation()), idgen);
        if (tree == null)
        {
            return null;
        }
        tree.setTreeString(unlexString);
        if (tree.getAnchor() == Integer.MIN_VALUE)
        {
            tree.findChoppedSpine();
        }
        if (tree.getAnchor() == Integer.MIN_VALUE)
        {
            return null;
        }
        tree.annotateHeadStatus();        
        return tree;
    }
      
     //currently only for trees with one lex root TODO 
    @Override
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
                pos = stripPosAndSemanticsToken(pos)[0]; // if there is any semantic role information, strip it out                
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
    
    protected String stripSemantics(String str)
    {
        int indexOfDelimiter = str.indexOf("@");
        if(indexOfDelimiter != -1)
        {                    
            while(indexOfDelimiter != -1)
            {
                int endOfRoleIndex = str.indexOf(";", indexOfDelimiter);
                if(endOfRoleIndex != -1) // rare false alarm case: '@' is not a delimiter but an actual lexical anchor
                {
                    str = str.replace(str.substring(indexOfDelimiter, endOfRoleIndex + 1), "");
                    indexOfDelimiter = str.indexOf("@", indexOfDelimiter);
                }
                else
                {
                    break;
                }
            }
        }
            return str;
    }
    
    protected List<String[]> stripPosAndSemantics(String str)
    {
        List<String[]> res = new ArrayList<String[]>();
        int indexOfDelimiter = str.indexOf("@");
        if(indexOfDelimiter != -1)
        {                    
            while(indexOfDelimiter != -1)
            {
                int endOfRoleIndex = str.indexOf(";", indexOfDelimiter);
                res.add(stripPosAndSemanticsToken(str.substring(0, endOfRoleIndex)));
                indexOfDelimiter = str.indexOf("@", endOfRoleIndex);
            }        
            for(String[] ar : res)
            {
                str = str.replace("@" + ar[1] + ";", "");
            }
        }
        res.add(new String[] {str});
        
        return res;
    }
    
    /**
     * Return the pos tag and semantic annotation (if existing) from an input string token
     * that has the following format: POS@SEM;, e.g., NP@ARGO;
     * @param str the input string
     * @return an array of strings. The first element is the POS tag, and the second is the semantic role label
     * or an empty string if there is none.
     */
    public static String[] stripPosAndSemanticsToken(String str) // TODO: Fix tackle cases with more than one role
    {        
        int indexOfDelimiter = str.indexOf("@");
        if(indexOfDelimiter == -1)
        {
            return new String[] {str, ""};
        }
        else
        {
            return new String[] {str.substring(0, indexOfDelimiter), str.substring(indexOfDelimiter + 1, str.length() - 1)};
        }        
    }
        
    /**
     * Strips the semantic frame (if any) from an input string
     * @param str the input string
     * @return 
     */
    public static String stripSemanticFrame(String str)
    {        
        int index = str.indexOf(".");        
//        if(index > 1 && index < str.length() - 1) // semantic frame of relation (and not the fullstop lexeme, or the fullstop at the end of a word)
        if(index > 1 && (str.substring(index + 1).matches("\\p{Digit}+") || str.substring(index + 1).equals("XX"))) // semantic frame of relation (and not the fullstop lexeme)
        {
            return str.substring(0, index);
        }
        return str;
    }
    
    private boolean hasSemanticFrame(String str)
    {
        int index = str.indexOf(".");
        return index > 1 && (str.substring(index + 1).matches("\\p{Digit}+") || str.substring(index + 1).equals("XX"));
    }
        
    protected static String removeAnnotation(String node)
    {
        if (node.indexOf("<") >= 0)
        {
            node = node.substring(0, node.indexOf("<"));
        }
        else if (node.indexOf("!") > 0)
        {
            node = node.substring(0, node.indexOf("!"));
        }
        else if (node.indexOf("*") > 0 && getNodeType(node) == TagNodeType.foot)
        {
            node = node.substring(0, node.indexOf("*"));
        }
        if (node.contains("_"))
        {
            node = node.substring(0, node.indexOf("^"));
        }
        return node;
    }        
    
}
