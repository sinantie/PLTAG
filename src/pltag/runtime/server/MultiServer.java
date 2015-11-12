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

import fig.exec.Execution;
import java.io.IOException;
import java.net.ServerSocket;
import pltag.parser.Options;
import pltag.parser.ParserModel;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class MultiServer
{
    String name;
    ParserModel parser;
    Options opts;
    
    public MultiServer(String[] args)
    {
        setUp(args);
    }
    
    private void setUp(String[] args) 
    {
        /*initialisation procedure from Generation class*/
        opts = new Options();
        Execution.init(args, new Object[] {opts}); // parse input params
        parser = new ParserModel(opts);
        parser.init();
        Utils.logs("\nFinished loading. Parser running in server mode using "
                + "%s threads and listening on port %s", opts.numThreads, opts.port);
    }
    
    
    public void execute()
    {
        ServerSocket serverSocket = null;
        boolean listening = true;        
            
        try {
            serverSocket = new ServerSocket(opts.port);
        }
        catch (IOException e) {
            error("Could not listen on port: " + opts.port);
            
        }
        try {
            while (listening)             
                new MultiServerThread(parser, serverSocket.accept()).start();            
        }
        catch(IOException ioe) {
            message("Could not establish connection!");
        }
        try {
            serverSocket.close();
        }
        catch(IOException ioe) {
            error("Error closing socket");
        }
    }
    public static void main(String[] args) throws IOException
    {        
        MultiServer ms = new MultiServer(args);
        ms.execute();
    }
    
    public static void error(String msg)
    {
        System.err.println(msg);
        System.exit(-1);
    }
    
    public static void message(String msg)
    {
        System.out.println(msg);        
    }    
}
