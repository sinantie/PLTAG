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
package pltag.util;

import edu.stanford.nlp.io.IOUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pltag.parser.semantics.conll.ConllExample;

/**
 *
 * @author sinantie
 */
public class UndoConllHeuristics
{
    private final String inputFilename, outputFilename;
    private final Set<String> SUBORDINATE_CONJUNCTIONS = new HashSet<String>(
            Arrays.asList(new String[] {
                "after","although","as","because","before","if",
                "once","since","than","that","though","unless","until",
                "when","whenever","where","whereas","wherever","whether","while","why"
            }));
    public UndoConllHeuristics(String inputFilename, String outputFilename)
    {
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
    }
    
    public void execute()
    {
        try
        {
            BufferedReader br = IOUtils.getBufferedFileReader(inputFilename);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilename));
            String sentence = readNextSentence(br);
            while(!sentence.equals(""))
            {
                boolean foundInfinitiveMarker;
                List<List<String>> tokens = Utils.unpackConllSentenceToTokens(sentence);
                for(int w = 0; w < tokens.size(); w++)
                {
                    List<String> wordTokens = tokens.get(w);
                    foundInfinitiveMarker = identifyInfinitiveMarker(wordTokens);
                    if(foundInfinitiveMarker)
                    {
                        undoInfinitiveMarkerHeuristic(tokens, wordTokens);
                    }
                    if(identifyPreposition(wordTokens, foundInfinitiveMarker))
                    {
                        undoAlternativeHeadHeuristic(tokens, wordTokens);
//                        System.out.println(wordTokens);
                    }
                    if(identifySubordinateConjunction(wordTokens))
                    {
                        undoAlternativeHeadHeuristic(tokens, wordTokens);
                    }
                }
                // repack conll sentence and write to output file
                String repackedSentence = Utils.repackConllTokensToSentence(tokens);
                bw.write(repackedSentence);
//                System.out.println(repackedSentence);
                sentence = readNextSentence(br); // go to the next sentence
            }
            br.close();
            bw.close();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
     
    /**
     * Identify an infinitive marker dependency relation. This is usually on 
     * a verb headed by the actual infinitive marker (TO).
     * @param wordTokens
     * @return
     */
    private boolean identifyInfinitiveMarker(List<String> wordTokens)
    {
        return wordTokens.get(ConllExample.DEPREL_COL).equals("IM");       
    }
    
    /**
     * Identify a preposition based on POS information (IN/TO). In case the word
     * has TO POS tag make sure it's not an infinitive marker, as it's already been
     * dealt with.
     * @param wordTokens
     * @param foundInfinitiveMarker
     * @return 
     */
    private boolean identifyPreposition(List<String> wordTokens, boolean foundInfinitiveMarker)
    {
        String pos = wordTokens.get(ConllExample.POS_COL);
        return pos.equals("IN") || !foundInfinitiveMarker && pos.equals("TO");
    }
    
    /**
     * Identify a subordinate conjunction with POS IN or DT.
     * @param wordTokens
     * @return 
     */
    private boolean identifySubordinateConjunction(List<String> wordTokens)
    {
        String lemma = wordTokens.get(ConllExample.LEMMA_COL);
        String pos = wordTokens.get(ConllExample.POS_COL);
        return SUBORDINATE_CONJUNCTIONS.contains(lemma) && (pos.equals("IN") || pos.equals("DT"));
    }
    
    /**
     * In CoNLL format the infinitive marker (TO) becomes the head of a VP. We 
     * revert this choice, by cutting the role information from the infinitive
     * marker and paste it to its child word.
     * @param tokens
     * @param wordTokens 
     */
    private void undoInfinitiveMarkerHeuristic(List<List<String>> tokens, List<String> wordTokens)
    {
        int parentId = Integer.valueOf(wordTokens.get(ConllExample.HEAD_COL));
        List<String> parentTokens = tokens.get(parentId - 1);
        // easy case: the infinitive marker should only have one role label attached to it.
        for(int i = ConllExample.APRED_START_COL; i < parentTokens.size(); i++)
        {
            String candLabel = parentTokens.get(i);
            if(!candLabel.equals("_"))
            {
                parentTokens.set(i, "_"); // remove label from infinitive marker (to)
                wordTokens.set(i, candLabel); // paste label to child word
                break;
            }
        }
    }
    
    /**
     * In CoNLL format prepositions are heads of PPs, and subordinate conjunctions
     * heads of SBARs. We undo these choices, by cutting and pasting the relevant label 
     * information from the preposition or subordinate conjunction to their
     * immediate child word.
     * @param tokens
     * @param wordTokens 
     */
    private void undoAlternativeHeadHeuristic(List<List<String>> tokens, List<String> wordTokens)
    {
        List<fig.basic.Pair<Integer, String>> list = new ArrayList<fig.basic.Pair<Integer, String>>();
        for(int i = ConllExample.APRED_START_COL; i < wordTokens.size(); i++)
        {
            String candLabel = wordTokens.get(i);
            if(!candLabel.equals("_"))
            {
                list.add(new fig.basic.Pair(i, candLabel));
                wordTokens.set(i, "_"); // delete entries from preposition
            }
        }
        if(!list.isEmpty())
        {
            String wordId = wordTokens.get(ConllExample.ID_COL);
            // find child node
            List<String> childWord = null;
            for(int i = 0; i < tokens.size(); i++)
            {
                List<String> candWord = tokens.get(i);
                if(candWord.get(ConllExample.HEAD_COL).equals(wordId))
                {
                    childWord = candWord;
                    break;
                }            
            }
            // paste label information to child node
            if(childWord != null)
            {
                for(fig.basic.Pair<Integer, String> pair : list)
                {
                    childWord.set(pair.getFirst(), pair.getSecond());
                }
            }
        }
    }
    
    private String readNextSentence(BufferedReader br) throws IOException
    {
        StringBuilder out = new StringBuilder();
        String line;
        while((line = br.readLine()) != null && !line.equals(""))
        {
            out.append(line).append("\n");
        }
        return out.toString();
    }
    
    public static void main(String[] args)
    {
//        String input = "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English.txt";
//        String output = "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English_heuristics.txt";
        
        if(args.length < 2)
        {
            System.err.println("Usage java -cp dist/PLTAG.jar pltag.util.UndoConllHeuristics input_file output_file");
        }
        else
        {
            String input = args[0];
            String output = args[1];
            UndoConllHeuristics uch = new UndoConllHeuristics(input, output);
            uch.execute();
        }        
    }
    
}
