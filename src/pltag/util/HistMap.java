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

import fig.basic.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author konstas
 */
public class HistMap<T>
{
    private HashMap<T, Counter> map = new HashMap<T, Counter>();

    public void add(T word)
    {
        Counter counter = map.get(word);
        if (counter == null) 
        {
            counter = new Counter(word);
            map.put(word, counter);
        }
        counter.incr();
    }
   
    public Set<Entry<T, Counter>> getEntries()
    {
        return map.entrySet();
    }
    
    public Set<Entry<T, Integer>> getEntriesFreqs()
    {
        Map<T, Integer> m = new HashMap();
        for(Entry<T, Counter> e : map.entrySet())
        {
            m.put(e.getKey(), e.getValue().value);
        }
        return m.entrySet();
    }
    
    public Set<T> getKeys()
    {
        return map.keySet();
    }
   
    public int getTotalFrequency()
    {
        int total = 0;
        for(Entry <T, Integer> e : getEntriesFreqs())
            total += e.getValue();
        return total;
    }
    
    public int size()
    {
        return map.size();
    }
    
    /**
     *
     * Return the most frequent key in the Map.
     * Warning: this can be a slow computation. Always pre-cache.
     */
    public T getFirstKey()
    {
        return getKeysSorted().get(0);
    }
    
    public Pair<T, Integer> getFirstEntry()
    {
        return getEntriesSorted().get(0);
    }
    
    public List<T> getKeysSorted()
    {
        List<Counter> list = new ArrayList<Counter>(map.values());
        Collections.sort(list);
        List<T> out = new ArrayList();
        for(Counter c : list)
            out.add(c.key);
        return out;
    }
    
    public List<Pair<T,Integer>> getEntriesSorted()
    {
        List<Counter> list = new ArrayList<Counter>(map.values());
        Collections.sort(list);
        List<Pair<T,Integer>> out = new ArrayList();
        for(Counter c : list)
            out.add(new Pair(c.key, c.value));
        return out;
    }
    
    public int getFrequency(T key)
    {
        return map.get(key).value;
    }
    
    /**
     * Returns frequency map in decreasing order
     * @return 
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        List<Counter> list = new ArrayList<Counter>(map.values());
        Collections.sort(list);
        for(Counter c : list)
            str.append(c).append("\n");
//        for(Entry<T, Counter> e : map.entrySet())
//        {
//            str.append(String.format("%s : %s\n", e.getKey(), e.getValue()));
//        }
//        str.delete(str.lastIndexOf(","), str.length());
        return str.toString();
    }
    
    final class Counter implements Comparable
    {
        private T key;
        private int value;

        public Counter(T key)
        {
            this.key = key;
        }
        
        public int getValue()
        {
            return value;
        }

        public void incr()
        {
            value++;
        }

        @Override
        public String toString()
        {
//            return String.valueOf(value);
            return String.format("%s : %s", key, value);
        }

        @Override
        public int compareTo(Object o)
        {            
            return ((Counter)o).value - value;            
        }
    }         
}
