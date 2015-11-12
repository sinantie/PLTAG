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

import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.Tree;
import fig.basic.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pltag.corpus.ElementaryStringTree;
import pltag.corpus.IdGenerator;
import pltag.corpus.StringTree;
import pltag.util.PosTagger;
import pltag.util.Utils;

/**
 * @author vera
 *
 */
public class Example
{
    private Options opts;    
    private String name;    
    private Lexicon lexicon;
    private Map<ElementaryStringTree, ArrayList<Fringe>> shadowTreesMap;    
    private SuperTagger superTagger;
    private FreqCounter freqCounter;             
    private String sentence, posTagged, parsed, goldStandardNoTraces, solution;
    private String[] origPosTags;
    private String[] wordIds; // dundee corpus: every word has a unique id attached to it.
    private String sentenceRc; // dundee corpus: RC annotation on sentence level 
    private final static Pattern digits = Pattern.compile("([^0-9]*)[0-9]+([^0-9]*)");
    private int numOfWords;
    boolean notParsed = false;
    
    public Example(String name, String[] input, Lexicon lexicon, Map<ElementaryStringTree, ArrayList<Fringe>> shadowTreesMap, 
            SuperTagger superTagger, FreqCounter freqCounter, Options opts)
    {        
        this.opts = opts;
        this.name = name;        
        this.lexicon = lexicon;
        this.shadowTreesMap = shadowTreesMap;
        this.superTagger = superTagger;
        this.freqCounter = freqCounter;       
        
        if(input[0].equals("NOT PARSED")) // we could not extract the lexicon or missing entirely from gold standard dataset
        {
            sentence = posTagged = parsed = goldStandardNoTraces = solution = "";
            numOfWords = 0;
            notParsed = true; 
        }
        else 
        {
            if (opts.estimateProcDifficulty)
            {
                if(opts.inputType == Options.InputType.dundee)
                {
                    readDundeeInput(input[0]);
                }
                else if(opts.inputType == Options.InputType.posTagged || 
                        (opts.inputType == Options.InputType.pltag && input[0].contains("\t"))) // tab delimited POS-word pairs
                {
                    readPosTagged(input[0]);
                }    
                else if(opts.inputType == Options.InputType.pltag)
                {
                    readPennTreebank(input, true);
                    solution = parsed; 
                }
                else // input is plain text without POS tags
                {
                    if(opts.goldPosTags)
                    {
                        readPosTagged(PosTagger.posTagLine(input[0]));
                    }
                    else
                    {
                        StringBuilder str = new StringBuilder();
    //                    for(String word : removeQuotesPlain(input[0]).split(" "))
                        for(String word :Utils.tokenizeStanford(input[0]).split(" "))
                        {
                            str.append(String.format("N/A %s\t", word));
                        }
                        readPosTagged(str.toString().trim());
                    }                    
                }
                solution = "";
            }
            else
            {
                readPennTreebank(input, true);
                solution = parsed;            
            }  
            this.numOfWords = posTagged.split("\t").length;
        }
    }       

    public Example(String name, String[] input, Options opts)
    {
        this(name, input, null, null, null, null, opts);
    }
    
    public Example(String name, String tree, boolean removeDigits, Options opts)
    {
        this.opts = opts;
        this.name = name;
        readTree(tree, removeDigits);        
    }
    
    /**
     * Reads the parsing input file containing only POS-tagged text
     * 
     * @param input     
     */
    private void readPosTagged(String input)
    {        
        //just words. first line: text string                
//        posTagged = correctPosTag(removeQuotesPlain(posTagRemoveDigits(input)), opts.combineNNVBcats);             
        posTagged = correctPosTag(posTagRemoveDigits(input), opts.combineNNVBcats);    
        origPosTags = posTags(input.trim());
    }

    /**
     * Reads the parsing input file
     * 
     * @param lines an array with String[3] pairs where String[0] is the text input, String[1]
     * is the POS tagged (with gold-standard POS tags) version, and String[3] is the gold standard analysis. 
     */
    private void readPennTreebank(String[] lines, boolean removeDigits)
    {                                           
        //first line: text string.                        
        sentence = textRemoveDigits(lines[0].trim());
        //POS-tagged sentence:            
        if (lines[1] == null)
        {
            System.err.println("Incorrect Input - every first line should be sentence, every second"
                    + "line should be its POS tagged version, and every third line its gold standard "
                    + "analysis.");
            return;
        }                                    
        posTagged = Utils.getCatInventory(posTagRemoveDigits(lines[1].trim()), opts.combineNNVBcats);
        origPosTags = posTags(lines[1].trim());
        //full analysis.            
        if (lines[2] == null)
        {
            return;
        }            
        if (!lines[2].startsWith("("))
        {
            System.err.println("Incorrect Input - every first line should be sentence, every second"
                    + "line should be its POS tagged version, and every third line its gold standard "
                    + "analysis. ParsingTask.read(filename)");
            return;
        }                        
        readTree(lines[2], removeDigits);
    }

    /**
     * Read input from Dundee corpus. The format is:
     * RC_label|w_1 id_1 w_2 id_2 ...
     * 
     * @param line 
     */
    private void readDundeeInput(String line)
    {
        int index = line.indexOf("|");
        if(index > -1)
            sentenceRc = line.substring(0, index);        
        line = line.substring(index + 1);
        StringBuilder sent = new StringBuilder();
        boolean usePosTagger = opts.goldPosTags;
        StringBuilder posTagDummy = new StringBuilder();
        if(line.charAt(0) == '(') // some examples may contain already parsed input in tree format
        {
            List<Word> words = Tree.valueOf(treeProcessDundeeIds(line)).yieldWords();
            for(Word word : words)
            {
                sent.append(word).append(" ");
                if(!usePosTagger)
                    posTagDummy.append("N/A ").append(word).append("\t");
            }
        }
        else
        {
            // remove quotes
            line = replaceParenthesesDundee(removeQuotesDundee(line).trim()).trim();
            String[] tokens = line.split(" ");            
            wordIds = new String[tokens.length / 2];            
            for(int i = 0; i < tokens.length - 1; i+=2)
            {
                String word = !usePosTagger ? wordRemoveDigits(tokens[i]) : tokens[i];
                sent.append(word).append(" ");
                if(!usePosTagger)
                    posTagDummy.append("N/A ").append(word).append("\t");
                wordIds[i / 2] = tokens[i + 1];
            }                    
            sentence = sent.toString().trim();
            if(usePosTagger)
            {
                Pair<String, String>[] posWords = PosTagger.posTagLineToArray(sentence);
                // One or more words has been expanded due to PTB-compliant splitting. 
                // For each new constituent assign the word-id of the original word.
                if(posWords.length != wordIds.length)                
                {
                   adjustWordIdsDundee(posWords, sentence.split(" ")); 
                }
                sentence = sentRemoveDigits(sentence);
                readPosTagged(wordRemoveDigits(PosTagger.tokensToLinePosTagged(posWords)));
            }
            else
                posTagged = posTagDummy.toString().trim();
        }
    }      
    
    private void readTree(String tree, boolean removeDigits)
    {                
        parsed = Utils.getCatInventory(removeDigits ? treeRemoveDigits(tree.trim()) : tree.trim(), opts.combineNNVBcats);      
        IdGenerator idgen = new IdGenerator();
        StringTree gsTree = Lexicon.convertToTree(new ElementaryStringTree("1\t" + parsed, opts.useSemantics), idgen);
        gsTree.removeUnaryNodes(gsTree.getRoot());//makeLexiconEntry();
        goldStandardNoTraces = gsTree.printNoTraces();//.printSimpleCat();
    }
    
    private String treeProcessDundeeIds(String line)
    {
        StringBuilder out = new StringBuilder();
        List<String> wids = new ArrayList<String>();
        for(String token : line.split(" "))
        {
            if(token.matches("[0-9]+_[0-9]+[A-Z]?\\)+"))
            {
                int index = token.indexOf((")"));
                wids.add(token.substring(0, index)); // add wordId                
                out.deleteCharAt(out.length() - 1).append(token.substring(index));
            }
            else
            {
                out.append(token).append(" ");
            }
        }
        wordIds = wids.toArray(new String[0]);
        return out.toString().trim();
    }
    
    private void adjustWordIdsDundee(Pair<String, String>[] newTokensPosWords, String[] originalTokens)
    {
        List<String> wordIdsList = new ArrayList<>();
//        System.out.println(name);
        assert newTokensPosWords.length > originalTokens.length;
        int pointer = 0;
        for(int i = 0; i < newTokensPosWords.length; i++)
        {
            if(newTokensPosWords[i].getSecond().equals(originalTokens[pointer]))
            {
                wordIdsList.add(wordIds[pointer]);
            }
            else
            {
                for(int j = i; j < newTokensPosWords.length; j++)
                {
                    if(i < originalTokens.length && newTokensPosWords[j].getSecond().equals(originalTokens[pointer + 1]) 
                            || originalTokens[pointer + 1].contains(newTokensPosWords[j].getSecond()) ) 
                    {
                        i = j - 1;
                        break;
                    }
                    wordIdsList.add(wordIds[pointer]);
                }                
            }
            pointer++;
        }
        assert wordIdsList.size() == newTokensPosWords.length;
        wordIds = wordIdsList.toArray(new String[0]);
    }
    
    private String treeRemoveDigits(String line)
    {
        line = line.replaceAll("[)][)]", ") )");
        String[] lexemes = line.split("[)]");
        String resultsentence = "";
        for (String l : lexemes)
        {
            if (l.contains(" "))
            {
                String w = l.substring(l.lastIndexOf(" "), l.length());
                String pref = l.substring(0, l.lastIndexOf(" "));
                if (w.contains("*"))
                {
                }
                else if (w.matches(".*[0-9].*") && !w.startsWith(" 0^") && !w.endsWith("^0_0"))
                {
                    w = toNum(w);
                }
                resultsentence += pref + w + ")";
            }
            else
            {
                resultsentence += l + ")";
            }
        }
        resultsentence += ")";
        resultsentence = resultsentence.replaceAll("[)] [)]", "))");
        return resultsentence;
    }

    public static String toNum(String w)
    {
        Matcher matchedexp = digits.matcher(w);
        String numw = matchedexp.replaceAll("$1NUM$2");
        return numw;
    }

    private String posTagRemoveDigits(String line)
    {
        String[] posAndWords = line.split("\t");
        String resultsentence = "";
        for (String pw : posAndWords)
        {
            String[] posword = pw.split(" ");
            String w = posword[1];
            String p = posword[0];
            if (w.contains("*"))
            {
            }
            else if (w.matches(".*[0-9].*") && !w.equals("0"))
            {
                w = toNum(w);
            }
            resultsentence += p + " " + w + "\t";
        }

        return resultsentence.trim();
    }

    private String removeQuotesPlain(String line)
    {
        return line.replaceAll(" ''", "");
    }
    
    private String removeQuotesDundee(String line)
    {
        return line.replaceAll(" '' [0-9]+_[0-9]+[NV]?", "").replaceAll(" `` [0-9]+_[0-9]+[NV]?", "").replaceAll("`` [0-9]+_[0-9]+[NV]?", " ");
    }
    
    private String replaceParenthesesDundee(String line)
    {
        return line.replaceAll(" -LRB- [0-9]+_[0-9]+[NV]?", "").replaceAll(" -RRB- [0-9]+_[0-9]+[NV]?", "").
                replaceAll("-LRB- [0-9]+_[0-9]+[NV]?", " ").replaceAll("-RRB- [0-9]+_[0-9]+[NV]?", " ").
                replaceAll(" -LSB- [0-9]+_[0-9]+[NV]?", "").replaceAll(" -RSB- [0-9]+_[0-9]+[NV]?", "").
                replaceAll("-LSB- [0-9]+_[0-9]+[NV]?", " ").replaceAll("-RSB- [0-9]+_[0-9]+[NV]?", " ");
    }
        
    private String[] posTags(String line)
    {
        String[] posAndWords = line.split("\t");
        String[] out = new String[posAndWords.length];
        int i = 0;
        for (String pw : posAndWords)
        {
            out[i++] = pw.split(" ")[0];
        }
        return out;
    }
    
    public static String correctPosTag(String line, boolean combineNNVBcats)
    {
        if (line.startsWith("CC "))
        {
            line = "CCSIM " + line.substring(3);
        }
        String[] posAndWords = line.split("\t");
        String resultSentence = "";
        for (String pw : posAndWords)
        {
            String[] posword = pw.split(" ");
            String w = posword[1];
            String p = Utils.getCatInventory(posword[0], combineNNVBcats);
            if (p.equals("JJR") || p.equals("JJS"))
            {
                p = "JJ";
            }
            else if (p.equals("PRP$"))
            {
                p = "DT";
            }
            else if (p.equals("RBR") || p.equals("RBS"))
            {
                p = "RB";
            }
            else if (p.equals("AUXG"))
            {
                p = "AUX";
            }
            if (p.equals("AUX") && w.startsWith("need"))
            {
                p = "VB";
            }
            resultSentence += p + " " + w + "\t";
        }

        return resultSentence.trim();
    }

    public static String sentRemoveDigits(String sent)
    {
        StringBuilder str = new StringBuilder();
        for(String word : sent.split(" "))
        {
            str.append(wordRemoveDigits(word)).append(" ");
        }            
        return str.toString().trim();
    }
    public static String wordRemoveDigits(String word)
    {
        if (word.contains("*") || word.equals("0"))
        {
            return word;
        }
        else if (word.matches(".*[0-9].*"))
        {
            return toNum(word);
        }
        return word;
    }
    
    private String textRemoveDigits(String line)
    {

        String[] words = line.split(" ");
        String resultSentence = "";
        for (String w : words)
        {
            if (w.contains("*") || w.equals("0"))
            {
            }
            else if (w.matches(".*[0-9].*"))
            {
                w = toNum(w);
            }
            resultSentence += w + " ";
        }

        return resultSentence.trim();
    }

    public String getName()
    {
        return name;
    }
    
    public String getSentence()
    {
        return sentence;
    }

    public String getPosTagged()
    {
        return posTagged;
    }

    public String getParsed()
    {
        return parsed;
    }  

    public String getSolution()
    {
        return solution;
    }

    public String getGoldStandardNoTraces()
    {
        return goldStandardNoTraces;
    }
    
    public int getNumOfWords()
    {
        return numOfWords;
    }
        
    public FreqCounter getFreqs()
    {
        return freqCounter;
    }        

    public Map<ElementaryStringTree, ArrayList<Fringe>> getShadowTreesMap()
    {
        return shadowTreesMap;
    }

    public SuperTagger getSuperTagger()
    {
        return superTagger;
    }

    public Lexicon getLexicon()
    {
        return lexicon;
    }

    public boolean isNotParsed()
    {
        return notParsed;
    }      

    public String[] getOrigPosTags()
    {
        return origPosTags;
    }   

    public String getSentenceRc()
    {
        return sentenceRc;
    }

    public String[] getWordIds()
    {
        return wordIds;
    }  

}
