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

import java.io.File;

/**
 *
 * @author sinantie
 */
public class MergeIncrementalDifficultyFiles
{
    private final String sourcePath, targetPath, outputPath;

    public MergeIncrementalDifficultyFiles(String sourcePath, String targetPath, String outputPath)
    {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.outputPath = outputPath;
    }
    
    
    public void execute()
    {
        if(new File(sourcePath).exists() && new File(targetPath).exists())
        {
            ExtractCriticalRegions reader = new ExtractCriticalRegions();
            ExtractCriticalRegions.DifficultyExample[] sourceExamples = reader.parseDiffScoreExamples(Utils.readLines(sourcePath));
            ExtractCriticalRegions.DifficultyExample[] targetExamples = reader.parseDiffScoreExamples(Utils.readLines(targetPath));
            // (SLOW) do a quadratic search of source example ids in the target list and replace
            for(int i = 0; i < sourceExamples.length; i++)
            {
                String sourceId = sourceExamples[i].name;
                for(int j = 0; j < targetExamples.length; j++)
                {
                    String targetId = targetExamples[j].name;
                    if(sourceId.equals(targetId))
                    {
                        targetExamples[j] = sourceExamples[i];
                    }
                }
            }
            // write to merged file
            reader.writeDifficultyExamples(targetExamples, outputPath, false);
        }
    }
    
    public static void main(String[] args)
    {
        String sourcePath = "results/output/pltag/test/dundee_all_tokenized-wordIds.noSemLexicon.correctQuotes.generative-wrongQuotesOnly/test.full-pred-gen";
        String targetPath = "results/output/pltag/test/dundee_all_tokenized-wordIds.noSemLexicon-correctQuotes.generative/test.full-pred-gen";
        String outputPath = "results/output/pltag/test/dundee_all_tokenized-wordIds.noSemLexicon-correctQuotes.generative/test.full-pred-gen-merged";
        MergeIncrementalDifficultyFiles exec = new MergeIncrementalDifficultyFiles(sourcePath, targetPath, outputPath);
        exec.execute();
        
    }
}
