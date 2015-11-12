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

import java.util.HashMap;
import java.util.Map;

public class LeafNode extends TagNode
{

    private String leaf;
    private Map<String, LeafNode> supportnodes = new HashMap<String, LeafNode>();
    private boolean isTrace = false;
    private String leafnumber;
    private String baseform;
    private String sense;
    private int leafno;
    private Map<String, LeafNode> lexentryrest = new HashMap<String, LeafNode>();

    public LeafNode(IdGenerator idgen, String cat, Map<String, Trace> tracelist, String leaf)
    {
        super(idgen, cat);
        this.leaf = leaf;
        tracelist = traceTreatment(tracelist);
    }

    protected Map<String, Trace> traceTreatment(Map<String, Trace> tracelist)
    {

        String lcat = ((LeafNode) this).getLeaf();
        if (lcat.contains("*-"))
        {
            super.traceID = lcat.substring(lcat.length() - 1);
            tracelist = makeTrace(tracelist, super.traceID);
            isTrace = true;
        }

        return tracelist;
    }

    public void setLeaf(String l)
    {
        this.leaf = l;
    }

    public void setLeafNumber(int lno)
    {
        leafnumber = String.valueOf(lno);
        leafno = lno;
    }

    public int getLeafNo()
    {

        return leafno;
    }

    public String getLeafNumber()
    {

        return leafnumber;
    }

    public String getLeaf()
    {
        return leaf;
    }

    public boolean isLeaf()
    {
        return true;
    }

    public void addSupportNode(LeafNode nn)
    {
        supportnodes.put(nn.getLeafNumber(), nn);
    }

    public Map getSupportNodes()
    {
        return supportnodes;
    }

    public boolean isTrace()
    {
        return isTrace;
    }

    public void addLexEntryRest(LeafNode nn)
    {
        lexentryrest.put(nn.getLeafNumber(), nn);
    }

    public Map getLexEntryRest()
    {
        return lexentryrest;
    }

    public void setBaseForm(String baseform)
    {
        this.baseform = baseform;
    }

    public String getBaseForm()
    {
        return baseform;
    }

    public void setSense(String sense)
    {
        this.sense = sense;
    }

    public String getSense()
    {        
        return sense == null ? "" : ("." + sense);
    }

    public String toString()
    {
        return leafnumber + ": " + leaf;
    }
}
