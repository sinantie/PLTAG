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
package pltag.parser.performance;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sinantie
 */
public class IncrementalBracketWidget
{
    
    private final Map<Integer, String> incrTreesMap;
    private final int timestamp;
    private final boolean fullSentence;
    
    public IncrementalBracketWidget(int timestamp, String tree, boolean fullSentence)
    {
        incrTreesMap = new HashMap<Integer, String>();
        incrTreesMap.put(timestamp, tree);
        this.timestamp = timestamp;
        this.fullSentence = fullSentence;
    }

    public IncrementalBracketWidget(Map<Integer, String> incrTreesMap)
    {
        this.incrTreesMap = incrTreesMap;
        timestamp = -1;
        fullSentence = true;
    }
    
    public String getTreeAt(int timestamp)
    {
        return incrTreesMap.get(timestamp);
    }

    public int getTimestamp()
    {
        return timestamp;
    }       

    public boolean isFullSentence()
    {
        return fullSentence;
    }
        
}
