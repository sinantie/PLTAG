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
import java.net.UnknownHostException;

/**
 *
 * @author sinantie
 */
public class PongClient
{

    public static void main(String[] args) throws IOException
    {        
        String host = "localhost";
        int port = 4446;
        if(args.length > 1)
        {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }
        Socket pongSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            pongSocket = new Socket(host, port);
            out = new PrintWriter(pongSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pongSocket.getInputStream()));
        }
        catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + host);
            System.exit(1);
        }
        catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + host);
            System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String fromServer = "";
        String fromUser;
                            
        fromUser = stdIn.readLine();
        if (fromUser != null) {
            System.out.println("Client: " + fromUser);
            out.println(fromUser);
        }
        while(fromServer != null)
        {
            fromServer = in.readLine();
            System.out.println("Server: " + fromServer);            
        }        

//        while ((fromServer = in.readLine()) != null) {
//            System.out.println("Server: " + fromServer);
//            if (fromServer.equals("Bye.")) {
//                break;
//            }
//
//            fromUser = stdIn.readLine();
//            if (fromUser != null) {
//                System.out.println("Client: " + fromUser);
//                out.println(fromUser);
//            }
//        }

        out.close();
        in.close();
        stdIn.close();
        pongSocket.close();
    }
}
