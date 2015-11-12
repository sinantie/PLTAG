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
package pltag.runtime;

import fig.basic.LogInfo;
import fig.exec.Execution;
import pltag.parser.Options;
import pltag.parser.Model;
import pltag.parser.ParserModel;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class Parse implements Runnable
{
    Options opts = new Options();
    
    @Override
    public void run()
    {
        Model parser = new ParserModel(opts);
        parser.init();
        if(!opts.interactiveMode)
            parser.readExamples();
        Utils.beginTrack("Parse Examples");
        if(opts.train)
        {
            parser.train();
        }
        else if(opts.interactiveMode)
        {
            parser.interactiveParse();
        }
        else
        {
            parser.parse();
        }
        
        LogInfo.end_track();        
    }
    
    public static void main(String[] args)
    {
        Parse x = new Parse();
        Execution.run(args, x, x.opts);
    }
}
