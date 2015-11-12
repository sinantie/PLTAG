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

import fig.exec.Execution;
import fig.record.Record;
import pltag.parser.ProbStats;
import pltag.util.MyList;
import pltag.util.Utils;


/**
 *
 * @author konstas
 */
public abstract class Performance<Widget>
{
    protected ProbStats stats = new ProbStats(0, 0, 0);
    public void add(ProbStats newStats)
    {
        stats.add(newStats);
    }

    protected MyList<String> foreachStat()
    {
        MyList<String> list = new MyList();
        list.add( "logZ", Utils.fmt(stats.getAvg_logZ()) );
        list.add( "logVZ", Utils.fmt(stats.getAvg_logVZ()) );                
        list.add( "accuracy", Utils.fmt(getAccuracy()));
        return list;
    }

    public boolean isEmpty()
    {
        return stats.getN() == 0;
    }

    public String summary()
    {
        return foreachStat().toString(" = ", ", ");
    }

    public void record(String name)
    {
        Utils.logs(name + ": " + summary());
        Record.begin(name);
        for(String[] el : foreachStat())
        {
            Execution.putOutput(name + "." + el[0], el[1]);
            Record.add(el[0], el[1]);
        }
        Record.end();
    }

    public abstract double getAccuracy();
    public abstract double[] add(Widget predAnalyses, Widget goldStandard, String name);

    public abstract String output();

    public void output(String path)
    {
        String out = foreachStat().toString("\t", "\n");
        out += output();
        Utils.write(path, out);
    }    
}
