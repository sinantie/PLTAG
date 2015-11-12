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

import fig.basic.Fmt;
import fig.basic.IOUtils;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import pltag.corpus.PltagExample;
import pltag.parser.Example;
import pltag.parser.Options;
import pltag.parser.performance.BracketPerformance;
import pltag.parser.performance.Performance;

/**
 *
 * @author konstas
 */
public class ProcessPltagOutput
{
    private Options opts;
    private String goldPath, externalSourcePath, inputPath;
    private Performance evalb;
    private boolean goldStandardFromExternalSource, normaliseInput;
    private final String EVALB_SCORE = "evalb F1", NBEST_POS = "nBest pos";
    public ProcessPltagOutput(Options opts, String goldPath, String externalSourcePath, 
            boolean goldStandardFromExternalSource, boolean normaliseInput)
    {
        this.opts = opts;
        this.goldPath = goldPath;
        this.externalSourcePath = externalSourcePath;
        this.evalb = new BracketPerformance();
        this.goldStandardFromExternalSource = goldStandardFromExternalSource;
        this.normaliseInput = normaliseInput;
        this.inputPath = opts.inputPaths.get(0);
    }
    
    public ProcessPltagOutput(Options opts, String goldPath, boolean goldStandardFromExternalSource, boolean normaliseInput)
    {
        this(opts, goldPath, null, goldStandardFromExternalSource, normaliseInput);
    }
    
    public ProcessPltagOutput(Options opts)
    {
        this(opts, null, null, false, true);
    }
    
    public void recalculateEvalb()
    {
        Map<String, Example> goldMap = readGoldStandard(true);
        List<FullPredExample> fullPredExamples = readExamples(inputPath, goldMap);
        for(FullPredExample ex : fullPredExamples)
        {
            String key = ex.getName();
            Example goldExample = goldMap.get(key);
            if(goldExample != null)
            {
                ex.setScore("F1", evalb.add(ex.getTree(), Utils.normaliseTree(goldExample.getGoldStandardNoTraces()), key)[0]);
            }     
        }        
        String path = inputPath.substring(0, inputPath.lastIndexOf("/"));
        Utils.write(path + "/results.performance.recalculated", evalb.output());
        if(opts.outputFullPred)
        {
            PrintWriter out = IOUtils.openOutEasy(inputPath + "-nonEmpty");
            PrintWriter outEmpty = IOUtils.openOutEasy(inputPath + "-listEmpty");
            PrintWriter outNonEmpty = IOUtils.openOutEasy(inputPath + "-listNonEmpty");            
            for(FullPredExample ex : fullPredExamples)
            {
                if(ex.isEmpty())
                {
                    outEmpty.write(ex.getName() + "\n");
                }
                else
                {
                    out.write(ex.toString());
                    outNonEmpty.write(ex.getName() + "\n");
                }                
            }
            out.flush(); out.close();
            outEmpty.flush(); outEmpty.close();
            outNonEmpty.flush(); outNonEmpty.close();
        }
        
    }
    
    private List<FullPredExample> readExamples(String inputPath, Map<String, Example> goldMap)
    {
        String[] lines = Utils.readLines(inputPath);
        List<FullPredExample> fullPredExamples = new ArrayList<FullPredExample>();
        for(int i = 0; i < lines.length; i++)
        {
            String keyIn = lines[i++];
            if(keyIn.equals("unknown"))
                i += 3;
            else if(lines[i].equals("") && !lines[i+1].startsWith("(")) // example with no output
            {
                i += !lines[i+1].equals("") ? 2 : 3;
            }
            else
            {
                boolean foundSrlTriples = false;
                while(lines[i].startsWith("<")) // input contains SRL-triples. Bypass
                {
                    foundSrlTriples = true;
                    i++;
                }
                if(foundSrlTriples || lines[i+1].startsWith("(")) // skip an extra empty line 
                    i++;
                FullPredExample ex = new FullPredExample(keyIn, lines[i++], lines[i++], normaliseInput);
    //            i++; // blank line            
                String key = ex.getName();
                Example goldExample = goldMap.get(key);
                if(goldExample != null)
                {
                    fullPredExamples.add(ex);                    
                }            
                else
                {
                    System.err.println(key + " not found in gold standard input file");
                }                            
            }            
        } // for
        return fullPredExamples;
    }
    
    public void outputFilesForExternalEvalb()
    {
        Map<String, Example> goldMap = readGoldStandard(true);        
        Map<String, FullPredExample> fullPredExamplesMap = readFullPredExamples(inputPath, goldMap, true);
        PrintWriter outGold = IOUtils.openOutEasy(inputPath + "-evalb.gld");
        PrintWriter outTest = IOUtils.openOutEasy(inputPath + "-evalb.tst");
        for(Entry<String, Example> goldEntry : goldMap.entrySet())
        {
            outGold.write(Utils.normaliseTree(goldEntry.getValue().getGoldStandardNoTraces()) + "\n");
            FullPredExample predTree = fullPredExamplesMap.get(goldEntry.getKey());            
            outTest.write((predTree == null ? "" : predTree.getTree()) + "\n");            
        }
        outGold.flush(); outGold.close();
        outTest.flush(); outTest.close();
    }
    
    public void outputFilesForExternalNormalisationScript()
    {
        Map<String, Example> goldMap = readGoldStandard(false);        
        Map<String, FullPredExample> fullPredExamplesMap = readFullPredExamples(inputPath, goldMap, false);
        Map<String, Example> externalParsedOutputMap = externalSourcePath != null ? 
                readParsedOutputFromExternalSource(externalSourcePath) : null;
        
        PrintWriter outTest = IOUtils.openOutEasy(opts.inputPaths + ".forNormalisation" + (goldStandardFromExternalSource ? "WithExtGold" : ""));
        PrintWriter outExt = externalSourcePath != null ? 
                IOUtils.openOutEasy(externalSourcePath + ".forNormalisationWithExtGold") : null;
        for(Entry<String, Example> goldEntry : goldMap.entrySet())
        {
            outTest.write("\n" + goldEntry.getValue().getGoldStandardNoTraces() + "\n");
            FullPredExample predTree = fullPredExamplesMap.get(goldEntry.getKey());            
            outTest.write( (predTree == null ? "" : ("-100.0 " + predTree.getTree())) + "\n"); // -100 is a dummy instead of a log score
            if(externalSourcePath != null)
            {
                outExt.write("\n" + goldEntry.getValue().getGoldStandardNoTraces() + "\n");
                Example extTree = externalParsedOutputMap.get(goldEntry.getKey());
                outExt.write( (extTree == null ? "" : ("-100.0 " + extTree.getGoldStandardNoTraces())) + "\n"); // -100 is a dummy instead of a log score
//                outExt.write( (extTree.equals("") ? "" : ("-100.0 " + extTree)) + "\n"); // -100 is a dummy instead of a log score
            }
        }        
        outTest.flush(); outTest.close();
        if(externalSourcePath != null)
        {
            outExt.flush(); outExt.close();
        }
    }
    
    public void outputGoldStandardFiles()
    {
        Map<String, Example> goldMap = readGoldStandard(true);                
        Map<String, Example> externalGoldMap = readGoldStandardFromExternalSource(externalSourcePath);
        
        PrintWriter outGold = IOUtils.openOutEasy(goldPath + ".forComparison");
        PrintWriter outGoldExt = IOUtils.openOutEasy(externalSourcePath + ".forComparison");
        PrintWriter outGoldCombined = IOUtils.openOutEasy(externalSourcePath + ".forComparisonCombined");
        for(Entry<String, Example> goldEntry : goldMap.entrySet())
        {
            outGold.write("\n" + goldEntry.getValue().getGoldStandardNoTraces() + "\n");
            Example ex = externalGoldMap.get(goldEntry.getKey());
            outGoldExt.write(ex == null ? "\n" : "\n" + ex.getGoldStandardNoTraces() + "\n");                      
            outGoldCombined.write("\n" +goldEntry.getValue().getGoldStandardNoTraces() + (ex == null ? "\n" : "\n-100.0 " + ex.getGoldStandardNoTraces() + "\n"));
        }        
        outGold.flush(); outGold.close();        
        outGoldExt.flush(); outGoldExt.close();
        outGoldCombined.flush(); outGoldCombined.close();
    }
    
    public void outputFilesForComparisonEvalb()
    {
        Map<String, Example> goldMap = readGoldStandard(true);
        FullPredExamples[] rawExamples = new FullPredExamples[opts.inputPaths.size()];
        // read examples from different files
        for(int i = 0; i < rawExamples.length; i++)
            rawExamples[i] = new FullPredExamples(readExamples(opts.inputPaths.get(i), goldMap));        
        // retain only the common examples across files based on key name
        getIntersectionOfExamples(rawExamples);
        double[][] scores = extractEvalbScores(rawExamples, goldMap);
        Utils.write(new File(opts.inputPaths.get(0)).getParent() + "/comparison.scores", toString(scores));
        double[][] avgScoresPerLength = averagePerLength(scores);
        Utils.write(new File(opts.inputPaths.get(0)).getParent() + "/comparison-avg.scores", toString(avgScoresPerLength));
    }
    
    /**
     * 
     * Read examples from an oracle ouput file and another produced by the parser, and choose the examples
     * in the latter that match a particular string <code>matchStr</code> that are not in the 1-best position
     * of the n-best output of the parser. 
     * @param matchStrs 
     */
    public void outputWrongExamplesContainingString(String[] matchStrs)
    {
        StringBuilder str = new StringBuilder();
        Map<String, Example> goldMap = readGoldStandard(true);
        FullPredExamples[] examples = new FullPredExamples[opts.inputPaths.size()];
        // read examples from different files
        for(int i = 0; i < examples.length; i++)
            examples[i] = new FullPredExamples(readExamples(opts.inputPaths.get(i), goldMap));        
        // retain only the common examples across files based on key name
        getIntersectionOfExamples(examples);
        // the first file contains examples generatd with the oracle-version of the parser
        for(Entry<String, FullPredExample> entry : examples[0].getExamples().entrySet()) 
        {
            FullPredExample ex = entry.getValue();
            String key = entry.getKey();
            if(ex.getScore(NBEST_POS) > 0)
            {
                boolean matchesAll = true;
                for(String matchStr : matchStrs)
                {
                    if(!ex.getTree().contains(matchStr))
                     {
                         matchesAll = false;
                         break;
                     }
                }
                if(matchesAll)
                    str.append(String.format("%s\n%s\n\n", ex.getName(), examples[1].getExample(key).getTree()));
            }                
        }
        Utils.write(new File(opts.inputPaths.get(1)).getParent() + "/matchingStrExamples", str.toString());
    }
    
    private double[][] averagePerLength(double[][] scores)
    {
        int cols = scores[0].length; // cols = number of system scores + word_length
        HistMap<Integer> freqLengthHist = new HistMap(); // number of individual scores
        Map[] lengthTotalScoresMap = new HashMap[cols - 1];
        for(int i = 0; i < lengthTotalScoresMap.length; i++)
        {
            lengthTotalScoresMap[i] = new HashMap<Integer, Double>();
        }        
        for(double[] row : scores)
        {
            Integer words = Integer.valueOf((int)row[0]); 
            freqLengthHist.add(words); // add word length counts
            for(int i = 1; i < row.length; i++)
            {
                Double oldScore = (Double) lengthTotalScoresMap[i - 1].get(words);
                lengthTotalScoresMap[i - 1].put(words, oldScore == null ? row[i] : oldScore + row[i]);                    
            }            
        }
        int rows = freqLengthHist.size();        
        double[][] res = new double[rows][cols];        
        for(Entry<Integer, Integer> wordLengthNumOfExamples : freqLengthHist.getEntriesFreqs())
        {            
            int words = wordLengthNumOfExamples.getKey();
            res[words - 1][0] = words;
            for(int j = 1; j < cols; j++)
            {
                res[words - 1][j] = (Double) lengthTotalScoresMap[j - 1].get(words) / (double) wordLengthNumOfExamples.getValue();
            }
        }
        return res;
    }
    
    private void getIntersectionOfExamples(FullPredExamples[] rawExamples)
    {
        for(int i = 0; i < rawExamples.length; i++)
        {
            boolean changed;
            if(i < rawExamples.length - 1) // not reached the end
            {
                // take the intersection of examples between the pair of example sets
                changed = getIntersectionOfExamples(rawExamples[i], rawExamples[i + 1]);
                if(changed && i > 0) // propagate changes to the 
                {
                    for(int j = i - 1; j >= 0; j--)
                    {
                        rawExamples[j].retainAllExamples(rawExamples[i]);
                    }
                }
            }
        }
    }
    
    private boolean getIntersectionOfExamples(FullPredExamples ex1, FullPredExamples ex2)
    {        
        return ex1.retainAllExamples(ex2) | ex2.retainAllExamples(ex1);        
    }
    
    private double[][] extractEvalbScores(FullPredExamples[] rawExamples, Map<String, Example> goldMap)
    {
        int columns = rawExamples.length + 1; 
        int rows = rawExamples[0].size();
        double[][] res = new double[rows][columns];
        int i = 0;
        for(String key : rawExamples[0].getNames())
        {
            // example length
            int words = goldMap.get(key).getNumOfWords();
            res[i][0] = words;
            for(int j = 1; j < columns; j++)
            {
                res[i][j] = rawExamples[j - 1].getExample(key).getScore(EVALB_SCORE);
            }
            i++;
        }
        return res;
    }
        
    private String toString(double[][] ar)
    {
        StringBuilder str = new StringBuilder();
        for(double[] row : ar)
        {
            for(double el : row)
            {
                str.append(el).append("\t");
            }
            str.deleteCharAt(str.length() - 1);
            str.append("\n");
        }
        return str.toString();
    }
    
    private  Map<String, Example> readGoldStandard(boolean normalise)
    {        
        if(goldStandardFromExternalSource)
            return readGoldStandardFromExternalSource(goldPath);
        else
        {
            Map<String, Example> goldMap = new HashMap<String, Example>();
            for(PltagExample ex : Utils.readPltagExamples(goldPath, true))
            {
                goldMap.put(ex.getName(), new Example(ex.getName(), ex.getGoldStandardArray(), opts));
            }
            return goldMap;
        }        
    } 
    
    /**
     * Read gold standard from external file with the following format:
     * line1 key
     * line2 goldStandard tree
     * line3 parsed tree
     * ...
     * @return 
     */
    private  Map<String, Example> readGoldStandardFromExternalSource(String inputPath)
    {
        Map<String, Example> goldMap = new HashMap<String, Example>();
        String[] lines = Utils.readLines(inputPath);
        for(int i = 0; i < lines.length; i++)
        {            
            String key = extractKeyFromLineHeuristic(lines[i]);
            goldMap.put(key, new Example(key, lines[++i], true, opts));
            if(!lines[i+1].endsWith(".txt"))
                i++; // omit the parsed tree
        }       
        return goldMap;
    } 
    
    /**
     * Read parsed output from external file with the following format:
     * line1 key
     * line2 goldStandard tree
     * line3 parsed tree
     * ...
     * @return 
     */
    private  Map<String, Example> readParsedOutputFromExternalSource(String inputPath)
    {
        Map<String, Example> output = new HashMap<String, Example>();
        String[] lines = Utils.readLines(inputPath);
        for(int i = 0; i < lines.length; i++)
        {            
            String key = extractKeyFromLineHeuristic(lines[i]);
            i++; // omit the gold standard tree
//            String[] parsed = lines[++i].split("\t");
            
            String out;
            if(lines[i+1].endsWith(".txt"))
            {
                out = null;
            }
            else
            {
                String parse = lines[++i];
                if(parse.contains("\t"))
                {
                    String[] parsed = parse.split("\t");
                    out = parsed.length > 1 ? parsed[2] : null;
                }
                else
                {
                    out = parse.substring(parse.indexOf(" ") + 1);
                }
            }
            
            output.put(key, out != null ? new Example(key, out, false, opts) : null);
//            output.put(key, parsed.length > 1 ? parsed[2].replaceAll("null", "-1") : "");
        }       
        return output;
    }        
    
    /**
     * Extract key in standard 'Example_' format, from input which is the following:
     * /xxxx/xxxx/dataAA-BB/lex.CC.txt search done;
     * @param keyIn
     * @return 
     */
    private String extractKeyFromLineHeuristic(String keyIn)
    {   
        try{
        String []ar = keyIn.split(" ")[0].split("/");
        String sent = ar[ar.length - 1].split("\\.")[1];
        String doc, sec;
        if(keyIn.contains("-"))
        {
            String []ar2 = ar[ar.length - 2].split("-");
            doc = ar2[1];
            sec = ar2[0].substring(4);                               
        }
        else
        {
            doc = ar[ar.length - 2];
            sec = ar[ar.length - 3];
        }
        return String.format("Example_%s/wsj_%s%s.mrg-sent_%s", sec, sec, doc, sent);
        }catch(Exception e)
        {
            System.out.println(keyIn);
        }
        return null;
    }
    
    private  Map<String, FullPredExample> readFullPredExamples(String inputPath, Map<String, Example> goldMap, boolean normalise)
    {
        String[] lines = Utils.readLines(inputPath);
        Map<String, FullPredExample> fullPredExamplesMap = new HashMap<String, FullPredExample>();
        for(int i = 0; i < lines.length; i++)
        {
            FullPredExample ex = new FullPredExample(lines[i++], lines[i++], lines[i++], normalise);
//            i++; // blank line            
            String key = ex.getName();
            Example goldExample = goldMap.get(key);
            if(goldExample != null)
            {                
                fullPredExamplesMap.put(ex.getName(), ex);
            }            
            else
            {
                System.err.println(key + " not found in gold standard input file");
            }                                    
        }
        return fullPredExamplesMap;
    }
    
    public static void main(String[] args)
    {
        Options opts = new Options();        
//        opts.inputPaths.add("results/output/pltag/test/discriminative/wsj_23_all-srl_prefixBase_wordBase_conllHeuristcs_probPrune_fixBugs_random_base1_model1_predPos/test.full-pred-gen");
//        opts.inputPaths.add("results/output/pltag/test/discriminative/wsj_23_all-srlPos_prefixBase_wordBase_conllHeuristcs_probPrune_fixBugs_random_base1_model1_predPos/test.full-pred-gen");
//        opts.inputPaths.add("results/output/pltag/test/23_all_generative_sameExamples/test.full-pred-gen");                
        opts.inputPaths.add("results/output/pltag/test/23_all_generative_oracle_again/test.full-pred-gen");
        opts.inputPaths.add("results/output/pltag/test/discriminative/wsj_23_all-srl_prefixBase_wordBase_conllHeuristcs_probPrune_fixBugs_random_base1_model1_predPos/test.full-pred-gen");        
        opts.outputFullPred = true; // re-write the output (only non-empty)
        String[] matchStrs = new String[] {"MD", "(RB n't)"};
        String goldPath = "data/pltag/GoldStandard_wsj_23_noSemantics";
//            String goldPath = "../PLTAG-Vera/cl-article-data.cleaned";
//        String goldPath = "../../Pltag_synsem/Console-23.txt";
//        String externalSourcePath = "../../Pltag_synsem/Console-23.txt";
        String externalSourcePath = "../PLTAG-Vera/cl-article-data.cleaned";
        boolean goldStandardFromExternalSource = false;
        boolean normaliseInput = false; // check whether the parsed output file used for input has already been normalised
//        RecalculateEvalb re = new RecalculateEvalb(opts, goldPath, externalSourcePath, goldStandardFromExternalSource, normaliseInput);
        ProcessPltagOutput re = new ProcessPltagOutput(opts, goldPath, goldStandardFromExternalSource, normaliseInput);
        if(opts.inputPaths.size() == 1)
        {
            re.recalculateEvalb();
    //        re.outputFilesForExternalEvalb();
    //        re.outputFilesForExternalNormalisationScript();
    //        re.outputGoldStandardFiles();            
        }
        else // output evalb scores from different files in separate columns
        {
//            re.outputFilesForComparisonEvalb();
            re.outputWrongExamplesContainingString(matchStrs);
        }
    }           
    
    class FullPredExamples
    {
        final Map<String, FullPredExample> map;

        public FullPredExamples(List<FullPredExample> list)
        {
            map = new HashMap<String, FullPredExample>();
            for(FullPredExample ex : list)
            {
                map.put(ex.name, ex);
            }
        }
        public boolean containExample(String key)
        {
            return map.containsKey(key);
        }
        
        public FullPredExample getExample(String key)
        {
            return map.get(key);
        }

        public Map<String, FullPredExample> getExamples()
        {
            return map;
        }  
        
        public Set<String> getNames()
        {
            return map.keySet();
        }
        
        public boolean retainAllExamples(FullPredExamples examplesIn)
        {
            boolean changed = false;
            List<String> keysToRemove = new ArrayList<String>();
            for(String key : map.keySet())
            {
                if (!examplesIn.containExample(key))
                {
                    keysToRemove.add(key);
                    changed = true;
                }
            }
            for(String key : keysToRemove)
                map.remove(key);
            return changed;
        }
        
        public int size()
        {
            return map.size();
        }
    }
    
    class FullPredExample
    {
        private final String name, tree;
        private final Map<String, Double> scores;        
        
        public FullPredExample(String name, String tree, String scoresIn, boolean normalise)
        {
            int indexOfSemicolon = name.indexOf(":"); // newer type of example ids, e.g., Example:wsj_23...
            this.name = indexOfSemicolon > 0 ? name.replaceFirst(":", "_") : name;
            this.tree = tree.equals("") ? "" : (normalise ? Utils.normaliseTree(tree) : tree);
            scores = new LinkedHashMap<String, Double>();        
            for(String score : scoresIn.substring(scoresIn.indexOf("[")+ 1, scoresIn.indexOf("]")).split(","))
            {
                String[] s = score.trim().split("=");                
                scores.put(s[0], Double.valueOf(s[1]));                
            }             
        }
        
        public boolean isEmpty()
        {
            return tree.equals("");
        }
        
        public String scoresToString()
        {
            StringBuilder str = new StringBuilder("[");
            for(Entry<String, Double> e : scores.entrySet())
            {
                str.append(e.getKey()).append("=").append(Fmt.D(e.getValue().doubleValue())).append(", ");
            }
            return str.replace(str.length() - 2, str.length(), "]").toString();
        }
        
        public void setScore(String key, double value)
        {
            if(scores.containsKey(key))
            {
                scores.put(key, value);
            }
        }
        
        public double getScore(String key)
        {
            return scores.get(key);
        }
        
        public String getName()
        {
            return name;
        }

        public String getTree()
        {
            return tree;
        }

        @Override
        public String toString()
        {
            return String.format("%s\n%s\n%s\n\n", name, tree, scoresToString());
        }     
    }
}
