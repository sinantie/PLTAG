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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import pltag.corpus.PltagExample;
import pltag.parser.Options;

/**
 *
 * Read input in pseudo-CoNLL format and convert to standard pltag-dundee format.
 * The input should be in the following format:
 * word_id-1  word-1    POS-1
 * word-id-2    word-2  POS-2
 * ...
 * 
 * and the output will be:
 * U|word_id-1 word-1 word-id-2 word 2 ...
 * @author sinantie
 */
public class ReadConllDundeeCorpus
{
    private final String inputFilename, outputFilename, existingFilename;
    private Options opts;
    
    public ReadConllDundeeCorpus(String inputFilename, String outputFilename, String existingFilename)
    {
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
        this.existingFilename = existingFilename;
        initOpts();
    }
    
    private Options initOpts()
    {
        opts = new Options();
        opts.inputPaths = new ArrayList(Arrays.asList(new String[] {inputFilename, existingFilename}));
        opts.estimateProcDifficulty = true;
        opts.inputType = Options.InputType.dundee;
        return opts;
    }
    public void execute()
    {
        // read input in pseudo-conll format
        Map<String, PltagExample> conllInputMap = readConllInput(opts.inputPaths.get(0));
        // read existing input in pltag-dundee standard format
        if(opts.inputPaths.get(1) != null)
        {
            Map<String, PltagExample> existingInputMap = readPltagInput(opts.inputPaths.get(1));
            mergeExamples(existingInputMap, conllInputMap);
            writeOutputFile(existingInputMap);
        }
        else
        {
            writeOutputFile(conllInputMap);
        }
    }
    
    private void writeOutputFile(Map<String, PltagExample> examplesMap)
    {
        String[] ar = new String[examplesMap.size()];
        int i = 0;
        for(PltagExample ex : examplesMap.values())
        {
            ar[i++] = ex.getGoldStandard();
        }
        Utils.writeLines(outputFilename, ar);
    }
     
    /**
     * Merge existing examples with new examples. In case of duplicates (based on key identity)
     * replace old example with new one. Add remaining examples in to the existing.
     * @param existingInputMap
     * @param conllInputMap 
     */
    private void mergeExamples(Map<String, PltagExample> existingExamplesMap, Map<String, PltagExample> newExamplesMap)
    {
        Iterator<Entry<String, PltagExample>> it = newExamplesMap.entrySet().iterator();
        while(it.hasNext())
        {
            Entry<String, PltagExample> entry = it.next();
            PltagExample ex = existingExamplesMap.get(entry.getKey());
            if(ex != null)
            {   // find and replace
                char annotation = ex.getGoldStandard().charAt(0);
                String newExampleStr = annotation + entry.getValue().getGoldStandard();
                entry.getValue().setGoldStandard(newExampleStr);
                existingExamplesMap.put(entry.getKey(), entry.getValue());
                it.remove();
            }            
        }
        // copy-paste the remaining examples
        existingExamplesMap.putAll(newExamplesMap);
    }
    
    /**
     * Read input in pseudo-conll format. Lines with a relative clause verb are marked
     * with an '>>' at the start of the line. These become the key of the sentence.
     * Note a sentence can have multiple relative clauses, so there are going to multiple keys.
     * We simply make copies of the same sentence under a different key.
     * @param inputPath
     * @return 
     */
    private Map<String, PltagExample> readConllInput(String inputPath)
    {
        Map<String, PltagExample> map = new HashMap();
        StringBuilder str = new StringBuilder();
        List<String> keys = new ArrayList();
        int i = 1;
        for(String line : Utils.readLines(inputPath))
        {
            if(!line.startsWith("#"))
            {
                String[] ar = line.split("\t");
                if(ar.length > 1) // conll input is tab delimited
                {
                    String wordId = ar[0];
                    if(wordId.startsWith(">>"))
                    {
                        wordId = wordId.substring(2) + "V";
                        keys.add(wordId);
                    }
                    str.append(ar[1]).append(" ").append(wordId).append(" "); // ignore POS
                }
                else
                {
                    if(str.length() > 0) // we wrote a complete sentence, so add to map
                    {
                        if(keys.isEmpty())
                            map.put(String.valueOf(i), Utils.readLineExample("U|" +str.deleteCharAt(str.length() - 1).toString(), i++));
                        else
                        {
                            for(String key : keys)
                            {
                                map.put(key, Utils.readLineExample("U|" +str.deleteCharAt(str.length() - 1).toString(), i++));
                            }
                        }
                        str = new StringBuilder();
                        keys.clear();
                    }
                } // else
            } // if            
        } // for
        return map;
    }
    
    
    /**
     * Read input from pltag-dundee standard format and put into a map, where the key
     * is the relative clause verb-id. These are typically id's ending with a V, such as 
     * e.g., 20_2148V
     * @param inputPath
     * @return 
     */
    private Map<String, PltagExample> readPltagInput(String inputPath)
    {
        Map<String, PltagExample> map = new LinkedHashMap();
        int i = 0;
        for(String exStr : Utils.readLines(inputPath))
        {
            // search for the verb-id which indicates a relative clause
            String key = null;
            for(String token : exStr.split(" "))
            {
                if(token.matches("[0-9]+_[0-9]+V[)]*"))
                {
                    key = token; //token.substring(0, token.length() - 1);                    
                    break;
                }                
            }    
            map.put(key == null ? String.valueOf(i++) : key, Utils.readLineExample(exStr, i));
        }
        return map;
    }
    
    
    public static void main(String[] args)
    {
        String input = "data/dundee/dundee_corrected_sentences";
        String existing = "data/dundee/annotated_sentences_SOK_original";
        String output = input + "-pltag-dundee";        
        ReadConllDundeeCorpus rcdc = new ReadConllDundeeCorpus(input, output, existing);
        rcdc.execute();
    }
    
}
