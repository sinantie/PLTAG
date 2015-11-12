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
package pltag.parser.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import fig.basic.LogInfo;
import java.io.IOException;
import pltag.parser.ParsingTask.OutputType;

/**
 *
 * @author konstas
 */
public class JsonInputWrapper
{
    public static final JsonResult ERROR_EVENTS = new JsonResult(OutputType.ERROR, "Error reading input!");
    public static final JsonResult ERROR_EXPORT_JSON = new JsonResult(OutputType.ERROR, "Error exporting json!");
    public static final JsonResult ERROR_PARSING = new JsonResult(OutputType.ERROR, "Error parsing sentence.");  

    private int beamSize;
    private String sentence;
    private String tmpFile;
    public static ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
    private boolean processUrl = true;
    
    public JsonInputWrapper(String query)
    {               
        processPltagJsonInput(query);        
    }

    public int getBeamSize()
    {
        return beamSize;
    }

    public String getSentence()
    {
        return sentence;
    }

    public String getTmpFile()
    {
        return tmpFile;
    }
      
    private boolean processPltagJsonInput(String example)
    {        
        try 
        {
            WebQuery query = mapper.readValue(example, WebQuery.class);

           // copy beam and input strings            
            beamSize = query.getBeamSize();
            sentence = query.getInput();
            tmpFile = query.getTmpFile();
            return true;
        }
        
        catch (IOException ex) 
        {
            LogInfo.error(ex);            
        }               
        return false;
    }
    
}
