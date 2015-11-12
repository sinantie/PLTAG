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
package pltag.parser.semantics.conll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author konstas
 */
public class Proposition implements Serializable
{

    private static final long serialVersionUID = 1L;
    
    Predicate predicate;
    List<Argument> arguments;

    public Proposition()
    {
    }
        
    public Proposition(Predicate predicate, List<Argument> arguments)
    {
        this.predicate = predicate;
        this.arguments = arguments;
    }
    
    public Proposition(Predicate predicate)
    {
        this(predicate, new ArrayList<Argument>());        
    }
        
    
    public boolean addArgument(Argument arg)
    {        
        return !arguments.contains(arg) ? arguments.add(arg) : false;
    }
    
    public List<Argument> getArgsAtTimeStamp(int t)
    {
        List<Argument> argsOut = new ArrayList<Argument>();
        for(Argument arg : arguments)
        {
            if(arg.beforeTimeStamp(t))
                argsOut.add(arg);
        }
        return argsOut;
    }
    
    public boolean isIncomplete()
    {
        return arguments == null || arguments.isEmpty();
    }
        
    public List<Argument> getArguments()
    {
        return arguments;
    }

    public Predicate getPredicate()
    {
        return predicate;
    }
    
    public String[] toStringArray(int size)
    {
        String[] out = new String[size];
        Arrays.fill(out, "_");
        for(Argument arg : arguments)
        {
            if(arg.timestamp >= 0)            
                out[arg.timestamp] = arg.role;            
        }
        return out;
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder(predicate.toString());
        for(Argument arg : arguments)
        {
            str.append(" ").append(arg);
        }
        return str.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof Proposition;
        Proposition p = (Proposition)obj;        
        return predicate.equals(p.predicate) && arguments.equals(p.arguments);
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 71 * hash + (this.predicate != null ? this.predicate.hashCode() : 0);
        hash = 71 * hash + (this.arguments != null ? this.arguments.hashCode() : 0);
        return hash;
    }
    
    
}
