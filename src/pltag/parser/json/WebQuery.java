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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author sinantie
 */
public class WebQuery
{
    
    private String input;
    private String tmpFile;
    private Integer beamSize;

    @JsonProperty("beam")
    public Integer getBeamSize()
    {
        return beamSize;
    }

    @JsonProperty("input")
    public String getInput()
    {
        return input;
    }                

    @JsonProperty("tmpFile")
    public String getTmpFile()
    {
        return tmpFile;
    }
    
    
}
