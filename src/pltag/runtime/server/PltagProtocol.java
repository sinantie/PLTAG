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
package pltag.runtime.server;

import fig.basic.SysInfoUtils;
import java.io.File;
import java.io.PrintWriter;
import pltag.parser.ParserModel;

/**
 * Simple protocol for php demo hook. The data exchange entails receiving
 * a string in json format. The server replies with an HTML formatted output for 
 * showing in the php frontend.
 * @author sinantie
 */
public class PltagProtocol implements Protocol
{
    ParserModel parser;
    String client;
    
    public PltagProtocol(ParserModel parser, String client)
    {
        this.parser = parser;
        this.client = client;
    }    
    
    @Override
    public String processInput(String input)
    {
        MultiServer.message(SysInfoUtils.getCurrentDateStr() + ": Client " + client + " made a request");        
        return "Server operates in incremental mode only";
    }

    @Override
    public void processInputIncrementally(String input, PrintWriter output)
    {
        parser.processExamplesJson(input, output);
    }
    
    @Override
    public void processInputIncrementally(String input)
    {
        
        if(input.startsWith("$"))
        {
            new File(input.substring(1)).delete();
        }
        else
        {
            parser.processExamplesJson(input);
        }        
    }
}
