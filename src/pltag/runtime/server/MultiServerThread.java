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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import pltag.parser.Model;
import pltag.parser.ParserModel;

/**
 *
 * @author sinantie
 */
public class MultiServerThread extends Thread
{

    private Socket socket = null;
    private final ParserModel parser; 
    private final String client;
    
    public MultiServerThread(ParserModel parser, Socket socket)
    {
        super("MultiServerThread");
        this.parser = parser;
        this.socket = socket;
        this.client = socket.getRemoteSocketAddress().toString();
        MultiServer.message("Established connection with client " + client);
    }

    @Override
    public void run()
    {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    socket.getInputStream()));

            String inputLine;
            Protocol p = new PltagProtocol(parser, client);            
            inputLine = in.readLine(); // read single request
            MultiServer.message("Input:" + inputLine);
            // process input incrementally and output syntactic tree/SRL graph/syntactic surprisal score one word at a time            
//            p.processInputIncrementally(inputLine, out);            
            p.processInputIncrementally(inputLine);            
                                    
            out.close();
            in.close();
            socket.close();
            MultiServer.message("Closed connection with client " + client);
        }
        catch (IOException e) {
            MultiServer.error(e.getMessage());
        }
    }
}
