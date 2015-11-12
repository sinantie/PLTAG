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

/**
 *
 * @author sinantie
 */
public class PltagExample
{
    
    private String[] records;
    private boolean conll = false;
    
    public PltagExample()
    {
        records = new String[4];
    }
    
    public PltagExample(String name, String goldStandard, String predLexicon, String lexicon, boolean conll)
    {
        records = new String[] {name, goldStandard, predLexicon, lexicon};
        this.conll = conll;
    }
    
    public PltagExample(String[] records)
    {
        this.records = records;
        conll = false;
    }
    
    public PltagExample(String name, String goldStandard, String predLexicon, String lexicon)
    {
        this(name, goldStandard, predLexicon, lexicon, false);
    }
            
    public PltagExample(String name)
    {
        this();
        records[0] = name;
        conll = false;
    }          
      
    public PltagExample(String name, String goldStandard, boolean conll)
    {
        this();   
        records[0] = name;
        records[1] = goldStandard;
        this.conll = conll;
    }
    
    public PltagExample(String name, String goldStandard)
    {
        this();   
        records[0] = name;
        records[1] = goldStandard;
    }
    
    public String getName()
    {
        return records.length > 0 ? records[0] : "";
    }
    
    public void setName(String text)
    {
        if(records.length > 0)
            records[0] = text;
    }
    
    public String getGoldStandard()
    {
        return records.length > 0 ? records[1] : "";
    }
       
    public void setGoldStandard(String text)
    {
        if(records.length > 0)
            records[1] = text;
    }
    
    public void setGoldStandard(String[] lines)
    {
        if(records.length > 0)
        {
            StringBuilder str = new StringBuilder();
            for(String line : lines)
                str.append(line).append("\n");
            records[1] = str.deleteCharAt(str.length() - 1).toString();
        }
    }
    public String[] getGoldStandardArray()
    {
        return records.length > 0 ? records[1].split("\n") : new String[0];
    }
      
    public String getPredLexicon()
    {
        return hasPredLexicon() ? records[2] : "";
    }        
    
    public void setPredLexicon(String text)
    {
        if(records.length > 0)
            records[2] = text;
    }
    
    public String[] getPredLexiconArray()
    {
        return records.length > 0 ? records[2].split("\n") : new String[0];
    }
    
    public String getLexicon()
    {
        return hasLexicon() ? records[3] : "";
    }
   
    public void setLexicon(String text)
    {
        if(records.length > 0)
            records[3] = text;
    }
    
    public String[] getLexiconArray()
    {
        return records.length > 0 ? records[3].split("\n") : new String[0];
    }
    
    public boolean hasPredLexicon()
    {
        return records[2] != null;
    }
    
    public boolean hasLexicon()
    {
        return records[3] != null;
    }        
    
    public int getNumberOfRecords()
    {
        return records.length;
    }
      
    public String toStringGoldStandardOnly()
    {
        return String.format("%s\n%s\n", getName(), getGoldStandard());
    }         
    
    public boolean isParsed()
    {
        return !getGoldStandardArray()[0].equals("NOT PARSED");
    }
    
    @Override
    public String toString()
    {
        if(hasLexicon())
            return String.format("$NAME\n%s\n$GOLD_STANDARD\n%s\n$PRED_LEXICON\n%s\n$LEXICON\n%s", 
                getName(), getGoldStandard(), getPredLexicon().trim(), getLexicon());
        return String.format("$NAME\n%s\n$GOLD_STANDARD\n%s", 
            getName(), getGoldStandard());
    }         
}
