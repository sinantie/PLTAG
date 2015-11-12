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
import pltag.parser.semantics.discriminative.ExtractFeatures;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class OfflineExtractFeatures implements Runnable
{
    Options opts = new Options();
    
    @Override
    public void run()
    {
        ExtractFeatures extractor = new ExtractFeatures(opts);
        extractor.execute();        
        Utils.beginTrack("Read Examples");        
        extractor.execute();
        LogInfo.end_track();        
    }
    
    public static void main(String[] args)
    {
        OfflineExtractFeatures x = new OfflineExtractFeatures();
        Execution.run(args, x, x.opts);
    }
}
