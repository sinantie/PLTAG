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
import java.util.List;

/**
 *
 * @author sinantie
 */
public class ProcessDundeeCorpus
{
    private final String outputFilename, originalFilename, editedFilename, filterIdsFilename, filterString;

    public ProcessDundeeCorpus(String outputFilename, String originalFilename, String editedFilename, String filterIdsFilename, String filterString)
    {
        this.outputFilename = outputFilename;
        this.originalFilename = originalFilename;
        this.editedFilename = editedFilename;
        this.filterIdsFilename = filterIdsFilename;
        this.filterString = filterString;
    }
    
    public void execute()
    {
        Integer[] ids = extractIdsFromFile(filterIdsFilename);
        String[] outLines = new String[ids.length];
        String[] originalLines = Utils.readLines(originalFilename);
        String[] editedLines = Utils.readLines(editedFilename);
        for(int i = 0; i < ids.length; i++)
        {
            String key = findKey(originalLines[i]);
            if(key == null)
                System.out.println("Didn't find key for example " + filterString + i);
            outLines[i] = String.format("%s\t%s\t%s", key, originalLines[i], editedLines[i]);
        }
        Utils.writeLines(outputFilename, outLines);
    }
    
    /**
     * Find id of verb used in the relative clause of the sentence (we assume that the
     * input sentences contain a relative clause), annotated with a 'V' in the end, e.g., ate 5_343V.
     * @param line
     * @return 
     */
    private String findKey(String line)
    {
        String[] ar = line.substring(line.indexOf("|") + 1).split(" ");
        for(int i = 1; i < ar.length; i += 2)
        {
            String candId = ar[i];
            if(candId.endsWith("V"))
                return candId;
        }
        return null;
    }
    
    private Integer[] extractIdsFromFile(String filterIdsFilename)
    {
        List<Integer> ids = new ArrayList<Integer>();
        for(String line : Utils.readLines(filterIdsFilename))
        {
            if(line.contains(filterString))
            {
                String[] ar = line.split(" ");
                for(String word : ar)
                {
                    if(word.contains(filterString))
                        ids.add(Integer.valueOf(word.split(filterString)[1].trim()));
                }
            }
        }
        return ids.toArray(new Integer[0]);
    }
    
    public static void main(String[] args)
    {
        String filterString = "Example_";
        String suffix = "ae";
        String output = "data/dundee/annotated_sentences_original-edited_" + suffix;
        String original = "data/dundee/annotated_sentences_original_" + suffix;
        String edited = "data/dundee/annotated_sentences_SOK_no_quotes_edited_" + suffix;
        String filterIds = "data/dundee/not_parsed_" + suffix;
        
        ProcessDundeeCorpus pdc = new ProcessDundeeCorpus(output, original, edited, filterIds, filterString);
        pdc.execute();
    }        
}
