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
package pltag.parser.performance;

import fig.basic.Fmt;
import java.util.List;

/**
 *
 * @author konstas
 */
public class TrainPerformance extends Performance<List<String>>
{
    private double totalCorrect, atLeastOneCorrect, totalExamples, allAnalysesFound;
    @Override
    public double getAccuracy()
    {
        return atLeastOneCorrect / totalExamples;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] add(List<String> predAnalyses, List<String> goldStandardList, String name)
    {
        String goldStandard = goldStandardList.get(0);
        int noOfCorrect = 0;
        boolean found = false;
        for(String analysis : predAnalyses)
        {
            if(goldStandard.equals(analysis))
            {
                found = true;                
                noOfCorrect++;
            }
            allAnalysesFound++;
        }
        totalExamples++;
        if(found)
            atLeastOneCorrect++;
        totalCorrect += noOfCorrect;
        return new double[] {noOfCorrect};
    }

    @Override
    public String output()
    {
        StringBuilder str = new StringBuilder("\n\nAccuracy scores");
        str.append("\n---------------");
        str.append("\nAt least one correct analyses' accuracy:\t").append(Fmt.D(getAccuracy()));
        str.append("\nTotal number of correct analyses' accuracy:\t").append(Fmt.D(totalCorrect / allAnalysesFound));
        str.append("\n---------------");
        str.append("\nTotal number of analyses found:\t").append((int)allAnalysesFound);
        str.append("\nTotal number of succesfully processed examples:\t").append((int)totalExamples);
        
        return str.toString();
    }

}
