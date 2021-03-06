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
import pltag.parser.semantics.discriminative.DiscriminativeParserModel;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class DiscriminativeParse implements Runnable
{
    Options opts = new Options();
    
    @Override
    public void run()
    {
        Model parser = new DiscriminativeParserModel(opts);
        parser.init();
        parser.readExamples();
        Utils.beginTrack("Discriminative Parse Examples");
        if(opts.train)
        {
            parser.train();
        }
        else
        {
            parser.parse();
        }
        
        LogInfo.end_track();        
    }
    
    public static void main(String[] args)
    {
        DiscriminativeParse x = new DiscriminativeParse();
        Execution.run(args, x, x.opts);
    }
}
