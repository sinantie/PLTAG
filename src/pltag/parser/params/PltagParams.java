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
package pltag.parser.params;

import java.util.HashMap;
import java.util.Map;
import pltag.parser.Options;
import pltag.parser.SuperTagElement;

/**
 *
 * @author konstas
 */
public class PltagParams
{
    
    private Map<String, Integer> freqMapTree, freqMapWord, freqMapStruct;
    private Map<SuperTagElement, Integer> freqMapFringe;
    private Options opts;
    
    public PltagParams(Options opts)
    {
        this.opts = opts;
        genParams();
    }

    private void genParams()
    {
        freqMapTree = new HashMap<String, Integer>();
        freqMapWord = new HashMap<String, Integer>();
        freqMapStruct = new HashMap<String, Integer>();
        freqMapFringe = new HashMap<SuperTagElement, Integer>();
    }

    public Map<SuperTagElement, Integer> getFreqMapFringe()
    {
        return freqMapFringe;
    }

    public Map<String, Integer> getFreqMapStruct()
    {
        return freqMapStruct;
    }

    public Map<String, Integer> getFreqMapTree()
    {
        return freqMapTree;
    }

    public Map<String, Integer> getFreqMapWord()
    {
        return freqMapWord;
    }    

    public void setFreqMapTree(Map<String, Integer> freqMapTree)
    {
        this.freqMapTree = freqMapTree;
    }

    public void setFreqMapWord(Map<String, Integer> freqMapWord)
    {
        this.freqMapWord = freqMapWord;
    }

    public void setFreqMapFringe(Map<SuperTagElement, Integer> freqMapFringe)
    {
        this.freqMapFringe = freqMapFringe;
    }

    public void setFreqMapStruct(Map<String, Integer> freqMapStruct)
    {
        this.freqMapStruct = freqMapStruct;
    }        
}
