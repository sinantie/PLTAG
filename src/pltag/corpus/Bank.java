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
//st

import fig.basic.LogInfo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public abstract class Bank
{

    private HashMap<String, HashMap<String, HashMap<String, Entry>>> files;
    //class vars
    
    public Bank(String dir, String sourcefile)
    {
        files = new HashMap<String, HashMap<String, HashMap<String, Entry>>>();
        readBank(dir, sourcefile);        
    }    
    
    private void readBank(String dir, String filename)
    {
        BufferedReader input = null;
        try
        {
            input = new BufferedReader(new FileReader(filename));
            String line = null;
            while ((line = input.readLine()) != null)
            {
                if (line.startsWith("wsj/".concat(dir)))
                {
                    //System.out.println(dir+filename2+"\n");
                    addEntryToFile(processLine(line));
                }
            }
        }
        catch (IOException ex)
        {
            LogInfo.error(ex);
        }
        finally
        {
            try
            {
                if (input != null)
                {
                    input.close();
                }
            }
            catch (IOException ex)
            {
                LogInfo.error(ex);
            }
        }
    }

    protected abstract Entry processLine(String line);

    private void addEntryToFile(Entry entry)
    {
        HashMap<String, HashMap<String, Entry>> treeMapper;
        HashMap<String, Entry> wordMapper;
        if (!files.containsKey(entry.file))
        {
            treeMapper = new HashMap<String, HashMap<String, Entry>>();
            wordMapper = new HashMap<String, Entry>();
            //wordmapper.put(entry.wordNumber, entry);
            //treemapper.put(entry.treeNumber, wordmapper);
            //files.put(entry.file, treemapper);
        }
        else
        {
            treeMapper = files.get(entry.file);
            if (!treeMapper.containsKey(entry.treeNumber))
            {
                wordMapper = new HashMap<String, Entry>();
                //wordmapper.put(entry.wordNumber, entry);
                //treemapper.put(entry.treeNumber, wordmapper);
            }
            else
            {
                wordMapper = treeMapper.get(entry.treeNumber);
            }
        }

        wordMapper.put(entry.wordNumber, entry);
        treeMapper.put(entry.treeNumber, wordMapper);
        files.put(entry.file, treeMapper);
    }

    public HashMap<String, HashMap<String, HashMap<String, Entry>>> getBank()
    {
        return files;
    }
}
