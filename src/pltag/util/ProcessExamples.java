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

import pltag.corpus.PltagExample;
import pltag.parser.Example;
import pltag.parser.Options;

/**
 *
 * @author konstas
 */
public class ProcessExamples
{    
    private Options opts;    
    public ProcessExamples(Options opts)
    {
        this.opts = opts;
    }
    
        
    public void execute()
    {
        int total = 0, count = 0, argumentTraces = 0, argumentBarTraces = 0;
        for(PltagExample plExample : Utils.readPltagExamples(opts.inputPaths.get(0), true))
        {
            Example ex = new Example(plExample.getName(), plExample.getGoldStandardArray(), opts);
            if(ex.getNumOfWords() <= opts.maxWordLength)
            {
                argumentTraces += countTraces(ex.getSentence(), "\\*-[0-9]*|\\*");
                argumentBarTraces += countTraces(ex.getSentence(), "\\*T\\*-[0-9]*|\\*T\\*");
                count++;
            }
            total++;
        }
        System.out.println("Total number of examples with length <= " + opts.maxWordLength + ": " + count + " (out of " + total + ")");
        System.out.println("Total number of argument traces: " + argumentTraces);
        System.out.println("Total number of argument bar traces: " + argumentBarTraces);
    }
    
    private int countTraces(String sentence, String type)
    {
        int count = 0;
        for(String word : sentence.split(" "))
        {
            if(word.matches(type))
                count++;
        }
        return count;
    }
    
    public static void main(String[] args)
    {
        Options opts = new Options();
        opts.inputPaths.add("data/pltag/GoldStandard_wsj_23_noSemantics");
        opts.maxWordLength = Integer.MAX_VALUE;        
        ProcessExamples pe = new ProcessExamples(opts);        
        pe.execute();
    }
}
