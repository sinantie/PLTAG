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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import pltag.corpus.PltagExample;
import pltag.parser.Example;
import pltag.parser.Options;

/**
 *
 * @author konstas
 */
public class CreateExamplesFromList
{
    private Options opts;
    private String listFilename, outputFilename;
    
    public CreateExamplesFromList(Options opts, String listFilename, String outputFilename)
    {
        this.opts = opts;
        this.listFilename = listFilename;
        this.outputFilename = outputFilename;
    }
    
    public void execute()
    {
        StringBuilder str = new StringBuilder();
        Set<String> keysOfExamples = new HashSet<String>(Arrays.asList(Utils.readLines(listFilename)));
        for(PltagExample plExample : Utils.readPltagExamples(opts.inputPaths.get(0), true))
        {
            if(keysOfExamples.contains(plExample.getName()))
            {
                Example ex = new Example(plExample.getName(), plExample.getGoldStandardArray(), opts);
                if(ex.getNumOfWords() <= opts.maxWordLength)
                {
                    str.append(plExample.toStringGoldStandardOnly()).append("\n");
                }
            }            
        }
        Utils.write(outputFilename, str.toString());
    }
    
    public void diffTwoLists(String list1Path, String list2Path)
    {
        Set<String> set = new TreeSet<String>();
        set.addAll(Arrays.asList(Utils.readLines(list1Path)));
        for(String line : Utils.readLines(list2Path))
        {
            set.remove(line);
        }
        Utils.writeLines(outputFilename, set.toArray(new String[0]));
    }
    
    public static void main(String[] args)
    {
        Options opts = new Options();
        opts.inputPaths.add("data/pltag/GoldStandard_wsj_23_noSemantics");
        opts.maxWordLength = Integer.MAX_VALUE;        
        String listFilename = "data/pltag/lists/list_bigger_beam_remaining_all";
        String outputFilename = "data/pltag/GoldStandard_wsj_23_noSemantics_bigger_beam_remaining";
        //String outputFilename = "data/pltag/lists/lists_diff";
        CreateExamplesFromList c = new CreateExamplesFromList(opts, listFilename, outputFilename);
        c.execute();
        //c.diffTwoLists("data/pltag/lists/list_remaining_all", "data/pltag/lists/list_small_beam_remaining_all");
    }
}
