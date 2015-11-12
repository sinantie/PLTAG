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
package pltag.corpus;

import java.util.Map;

public class InnerNode extends TagNode
{

    public InnerNode(IdGenerator idgen, String cat, Map<String, Trace> tracelist)
    {
        super(idgen, cat);
        tracelist = traceTreatment(tracelist);
    }

    protected Map<String, Trace> traceTreatment(Map<String, Trace> tracelist)
    {
        String fullcategory = getFullCategory();
        if (fullcategory.matches(".*-[0-9]"))
        {
            traceID = fullcategory.substring(fullcategory.length() - 1);
            tracelist = makeTrace(tracelist, traceID);
        }
        return tracelist;
    }

    public String toString()
    {
        return super.getNodeID() + ": " + super.getCategory();
    }
}
