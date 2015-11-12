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
 * @author konstas
 */
public class ExtractOutputFromSingleFile
{
    String inputFile;

    public ExtractOutputFromSingleFile(String inputFile)
    {
        this.inputFile = inputFile;
    }
    
    
    public void execute()
    {
        List<String> goldList = new ArrayList<String>();
        List<String> parsedList = new ArrayList<String>();
        int total = 0, found = 0;
        String[] lines = Utils.readLines(inputFile);
        for(int i = 0 ; i < lines.length; i++)
        {
            total++;
            goldList.add(fixBrackets(lines[i]));
            if(i + 1 < lines.length) // we are not past the end of the file
            {
                if(!lines[i + 1].equals("")) // the parser produced an output
                {
                    found++;
                    
//                    parsedList.add(fixBrackets(lines[i + 1].substring(lines[i + 1].indexOf(" ") + 1)));
                    parsedList.add(Utils.normaliseTree(lines[i + 1].substring(lines[i + 1].indexOf(" ") + 1)));
//                    parsedList.add(lines[i + 1].substring(lines[i + 1].indexOf(" ") + 1));
                    int counter = 1;
                    for(int j = i + 1; j < lines.length; j++) // go through and ignore the (possibly many) parses
                    {
                        if(lines[j].equals(""))
                        {
                            i += counter; // advance past the parser output
                            break;
                        }
                        counter++;
                    }
                }
                else
                {
                    parsedList.add("");
                    i++; // advance past the empty line
                }
            }
        }
        Utils.writeLines(inputFile + ".gld", goldList.toArray(new String[0]));
        Utils.writeLines(inputFile + ".tst", parsedList.toArray(new String[0]));
        System.out.println(String.format("Succesfully parsed %s out of %s total examples.", found, total));
    }
    
    public String fixBrackets(String in)
    {
        return in.replaceAll("\\^-?\\d_-?\\d|<>", "").replaceAll("\\(\\p{Space}", "(");
    }
    
    public static void main(String[] args)
    {
        String inputFile = args.length > 0 ? args[0] : 
                "results/output/pltag/test/23_small_beam_all_goldPos/test.full-pred-gen-with_bigger_beam";
        ExtractOutputFromSingleFile e = new ExtractOutputFromSingleFile(inputFile);
        e.execute();
    }
}
