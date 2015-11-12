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
package pltag.parser.semantics.discriminative;

import fig.basic.ListUtils;
import java.util.ArrayList;
import java.util.List;
import pltag.parser.performance.Performance;
import pltag.util.MyList;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public class DiscriminativePerformance extends Performance
{
    List<Double> gradientList = new ArrayList<Double>();
           

    public void add(double gradient)
    {
        gradientList.add(gradient);
    }
    
    private double getAverageGradientNorm()
    {
        return ListUtils.mean(gradientList);
    }
    
    @Override
    public double getAccuracy()
    {
        return getAverageGradientNorm();
    }
   
    @Override
    protected MyList<String> foreachStat()
    {
        MyList<String> list = new MyList();
        list.add( "Average Gradient Norm", Utils.fmt(getAverageGradientNorm()) );
//        list.addAll(super.foreachStat());
        return list;
    }
    
    @Override
    public String output()
    {
        return "Average gradient norm: " + Utils.fmt(getAccuracy()) + "\n";
//                super.output();
    }

    @Override
    public double[] add(Object predAnalyses, Object goldStandard, String name)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
