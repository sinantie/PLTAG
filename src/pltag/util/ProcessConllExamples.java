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

import fig.basic.Indexer;
import java.util.Collection;
import java.util.List;
import pltag.corpus.PltagExample;
import pltag.parser.Options;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.ConllExample;
import pltag.parser.semantics.conll.Proposition;

/**
 *
 * @author konstas
 */
public class ProcessConllExamples
{    
    private Options opts;    
    public ProcessConllExamples(Options opts)
    {
        this.opts = opts;
    }
    
        
    public void execute()
    {
        List<PltagExample> goldExamples = Utils.readConllExamples(opts.inputPaths.get(0));
        List<PltagExample> parsedExamples = Utils.readConllExamples(opts.inputPaths.get(1));
        Indexer<String> roleIndexer = new Indexer<String>();
        int goldA0Count = 0, goldA1Count = 0, goldPredCount = 0, goldArgCount = 0, parsedArgCount = 0, 
                parsedA0Count = 0, parsedA1Count = 0, parsedPredCount = 0,
                goldA0AndA1Count = 0, parsedA0AndA1Count = 0, goldAvgArgs = 0, parsedAvgArgs = 0;
        HistMap<Integer> sentenceLengthHist = new HistMap<Integer>();
        HistMap<Integer> argsNumHist = new HistMap<Integer>();
        
        for(PltagExample pltag : goldExamples)
        {
            ConllStats stats = countPredsArgs(new ConllExample(pltag.getName(), pltag.getGoldStandard(), opts, roleIndexer), true);
            goldPredCount += stats.preds;
            goldArgCount += stats.args;
            goldA0Count += stats.a0;
            goldA1Count += stats.a1;
            goldA0AndA1Count += stats.a0AndA1;
            sentenceLengthHist.add(pltag.getName().split(" ").length);
            argsNumHist.add(stats.args);
        }
        for(PltagExample pltag : parsedExamples)
        {
            ConllStats stats = countPredsArgs(new ConllExample(pltag.getName(), pltag.getGoldStandard(), opts, roleIndexer), false);
            parsedPredCount += stats.preds;
            parsedArgCount += stats.args;
            parsedA0Count += stats.a0;
            parsedA1Count += stats.a1;
            parsedA0AndA1Count += stats.a0AndA1;
        }
        System.out.println(String.format("[Gold] Total Preds = %s, Args = %s, A0s = %s, A1s = %s", goldPredCount, goldArgCount, goldA0Count, goldA1Count));
        System.out.println(String.format("[Gold] Average A0s/Preds = %s, A1s/Preds = %s", (double)goldA0Count / (double)goldPredCount, (double)goldA1Count / (double)goldPredCount));
        System.out.println(String.format("[Gold] Total A0s & A1s = %s. Average A0s&A1s / Preds = %s", goldA0AndA1Count, (double)goldA0AndA1Count / (double)goldPredCount));
        System.out.println(String.format("[Parsed] Total Preds = %s, Args = %s, A0s = %s, A1s = %s", parsedPredCount, parsedArgCount, parsedA0Count, parsedA1Count));
        System.out.println(String.format("[Parsed] Average A0s/Preds = %s, A1s/Preds = %s", (double)parsedA0Count / (double)parsedPredCount, (double)parsedA1Count / (double)parsedPredCount));
        System.out.println(String.format("[Parsed] Total A0s & A1s = %s. Average A0s&A1s / Preds = %s", parsedA0AndA1Count, (double)parsedA0AndA1Count / (double)parsedPredCount));
        
        System.out.println("\n\n\n");
        System.out.println("[Gold] Sentence length histogram\n" + sentenceLengthHist.toString());
        System.out.println("[Gold] Num of args per sentence histogram\n" + argsNumHist.toString());
    }
    
    private ConllStats countPredsArgs(ConllExample conll, boolean gold)
    {
        ConllStats stats = new ConllStats();
        Collection<Proposition> props = gold ? conll.getVerbPropositions() : conll.getPropositions().values();
        stats.preds = props.size();
        for(Proposition prop : props)
        {
            boolean a0Found = false; boolean a1Found = false;
            for(Argument arg : prop.getArguments())
            {                
                if(arg.getRole().equals("A0"))
                {
                    stats.a0++;
                    a0Found = true;
                }
                if(arg.getRole().equals("A1"))
                {
                    stats.a1++;
                    a1Found = true;
                }
                stats.args++;
            } // for
            if(a0Found && a1Found)
                stats.a0AndA1++;
        } // for
        return stats;
    }
    
    public static void main(String[] args)
    {
        Options opts = new Options();
        opts.inputPaths.add("../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English-edited.txt");
        opts.inputPaths.add("results/output/pltag/test/SRL/23_all_oracle_prevFeats_mergedArcs_origPosTags-identifier_bilexicalOnly-labeller_bilexicalOnly-tuning-SRL/test.full-pred-gen.conll");
        ProcessConllExamples pe = new ProcessConllExamples(opts);        
        pe.execute();
    }
    
    class ConllStats
    {
        int preds, args, a0, a1, a0AndA1, numWords;
              
    }
}
