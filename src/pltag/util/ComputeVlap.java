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

import fig.exec.Execution;
import pltag.parser.Options;
import pltag.parser.VerificationLookAheadProbability;

/**
 *
 * @author sinantie
 */
public class ComputeVlap implements Runnable
{
 
    Options opts = new Options();
        
    @Override
    public void run()
    {
        VerificationLookAheadProbability vlap = new VerificationLookAheadProbability(opts);
        vlap.readLexicons();
        vlap.compute();
        vlap.serialize();
    }
    
    public static void main(String[] args)
    {
        ComputeVlap x = new ComputeVlap();
        Execution.run(args, x, x.opts);
    }
}
