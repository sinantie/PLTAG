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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author konstas
 */
public class ReadVectors
{
    private String inputFile;

    public ReadVectors(String inputFile)
    {
        this.inputFile = inputFile;
    }
    
    
    public void execute()
    {
        try
        {
            FileOutputStream fosVectors = new FileOutputStream(inputFile + ".vecs");
            FileOutputStream fosWords = new FileOutputStream(inputFile + ".words");
            BufferedReader fin = new BufferedReader(new FileReader(inputFile));
            String line;
            
            while((line = fin.readLine()) != null)
            {
                String ar[] = line.split(" ");
                fosWords.write((ar[0] + "\n").getBytes());
                for(int i = 1; i < ar.length; i++)
                    fosVectors.write((ar[i] + " ").getBytes());
                fosVectors.write("\n".getBytes());
            }
            fosVectors.close();
            fosWords.close();
            fin.close();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
    
    public static void main(String[] args)
    {
        String inputFile = "/home/konstas/EDI/synsem/konstas/Vector-Based Models/model-2280000000.LEARNING_RATE=1e-08.EMBEDDING_LEARNING_RATE=1e-07.EMBEDDING_SIZE=25.txt";
        ReadVectors rv = new ReadVectors(inputFile);
        rv.execute();
    }
}
