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

import fig.basic.Option;

/**
 *
 * @author sinantie
 */
public class TagCorpusOptions
{    
    @Option public String logFilename;
    @Option(required=true) public String ptbPath;
    @Option public boolean examplesInSingleFile = false;
    @Option(required=true, gloss="Lexicon output file") public String lexiconFilename;
    @Option(required=true, gloss="Gold standard output file") public String goldStandardFilename;
    @Option public String outputFilename;
    @Option(required=true) public String percolTableFilename;
    @Option(required=true) public String predLexFilename;
    @Option(required=true) public String propBankFilename;
    @Option public String nomBankFilename;
    @Deprecated @Option public String individualFilesPath;
    @Option public boolean moreThanOneFoot = false;
    @Option public boolean justMultiWordLexemes = false;    
    @Option public boolean verbose = false;
    @Option public boolean useSemantics = false;
    @Option(gloss="Output empty examples even though we could not extract the associated lexicon") public boolean outputEmptyExamples = false;
    @Deprecated @Option(gloss="Output tag annotation per penn treebank file") public boolean outputIndividualFiles = false;
        
    @Option(gloss="Start track (Penn treebank)") public int startTrack = 0;
    @Option(gloss="End track (Penn treebank)") public int endTrack = 0;
    
            
}
