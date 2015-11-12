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
package pltag.parser;

import fig.basic.FullStatFig;
import fig.basic.LogInfo;
import fig.exec.Execution;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pltag.corpus.PltagExample;
import pltag.parser.Options.InputType;
import pltag.parser.params.PltagParams;
import pltag.parser.performance.Performance;
import pltag.util.PosTagger;
import pltag.util.Utils;

public abstract class Model
{
    protected Options opts;
    protected List<Example> examples;    
    protected PltagParams params;
    protected Set<String> listOfFreqWords;
    protected int numExamples, totalNumOfGoldPropositions;
    protected PrintWriter trainPredOut, testPredOut, trainFullPredOut, testFullPredOut, testIncrDependenciesOut, conllPredOut, conllGoldOut;
    protected Performance performance;
    protected boolean goldConllPresent;
    protected Map<String, String> incrementalExamplesOutputMap, incrementalDependenciesOutputMap;
    protected List<String> useExamplesOnlyList;
    
    public Model(Options opts)
    {
        this.opts = opts;
        this.examples = new ArrayList<Example>();
    }

    public void init()
    {        
        if(opts.train) // set some flags to default mode for training
        {
            //opts.fullLex=false;
            opts.goldPosTags=true;            
            opts.treeFamilies=false;
            opts.useProbabilityModel = false;
            opts.calculateMostProbableTree = false;
        }
        else
        {
            //opts.fullLex = true;
            //opts.goldPosTags = false;
            opts.posOnly = false;
            opts.treeFamilies = true;
            opts.useProbabilityModel = true;
            opts.calculateMostProbableTree = true;
        }
        newParams();
        if(opts.useProbabilityModel)
        {
            loadParamsFromDisk();
        }
        if(opts.listOfFreqWords != null)
            listOfFreqWords = new HashSet<String>(Arrays.asList(Utils.readLines(opts.listOfFreqWords)));
        
    }           
        
    public void readExamples()
    {
        Utils.beginTrack("Reading examples");
        examples.clear();
        // initialise Stanford POS Tagger in case the user asks for goldPosTags and they are not included
        // in the input file. This is true when reading in from the Dundee Corpus with Relative Clauses annotations,
        // or from plain text file.
        if((opts.inputType == InputType.plain || opts.inputType == InputType.dundee) && opts.goldPosTags)
        {
            PosTagger.initPosTagger();
        }
        readExamplesOnlyList();
        if(opts.examplesInSingleFile)
            readFromSingleFile(opts.inputPaths);
        else
        {
            for(String path : opts.inputPaths)
                addPath(path);
        }
        if(opts.useSemantics && opts.inputPaths.size() > 1) // reading from conll gold file
        {
            LogInfo.logsForce("Read a total of %s examples and %s propositions", numExamples, totalNumOfGoldPropositions);
        }
        else
        {
            LogInfo.logsForce("Read a total of %s examples", numExamples);
        }           
        LogInfo.end_track();
    }
    
    private void addPath(String path)
    {
        File file = new File(path);
        if(file.isDirectory())
        {
            for(String fileStr : Utils.sortWithEmbeddedInt(file.list()))
            {                
                addPath(path + "/" + fileStr);             
            } // for
        } // if
        
        else if(validName(path) && file.length() > 0) // some lexicon files may be empty
        {
//            Utils.beginTrack("%s (%s examples so far)", path, ++numExamples);            
            File lexFilename = new File(path);
            File goldFilename = new File(String.format("%s/gs.%s", lexFilename.getParent(), lexFilename.getName().substring(4)));
            if(goldFilename.length() > 0) // some gold standard files may be empty
            {
                PltagExample ex = new PltagExample(goldFilename.getPath());
                ex.setGoldStandard(Utils.readLines(goldFilename.getPath()));
                readExamples(ex);
            }            
//            LogInfo.end_track();
        }
    }
   
    private boolean validName(String path)
    {
        String filename = new File(path).getName();
        return filename.startsWith("lex.") && filename.endsWith(".txt") &&
               !filename.startsWith("#");
    }
    
    /**
     * Read input from single file(s). We currently support two formats: 
     * 1) A proprietary format that contains gold standard text, pos tags,
     * constituency parse, and optionally associated lexicons.<br/>
     * 2) CoNLL 2009 tabular format.<br/>
     * The user may give both files and we will try to match the CoNLL sentences
     * to the proprietary input that have an id.
     * 
     * @param inputFiles 
     */
    protected void readFromSingleFile(List<String> inputFiles)
    {
        if(inputFiles.size() == 1)
        {   // attempt reading proprietary format
            List<PltagExample> list = (opts.inputType == Options.InputType.plain || opts.inputType == Options.InputType.posTagged || 
                    opts.inputType == Options.InputType.dundee) ? 
                    Utils.readLinesExamples(inputFiles.get(0)) : Utils.readPltagExamples(inputFiles.get(0), true);
            if(list.size() > 0)
                readList(list);
            else
            {   // attempt reading CoNLL format
                list = Utils.readConllExamples(inputFiles.get(0));
                if(list.size() > 0)
                    readList(list);
                goldConllPresent = true;
            }
        }
        else
        {
            List<PltagExample> pltagExamples = Utils.readPltagExamples(inputFiles.get(0), true);
            List<PltagExample> conllExamples = Utils.readConllExamples(inputFiles.get(1));
            readPltagConllExamples(pltagExamples, conllExamples);
            goldConllPresent = true;
        }        
//        System.exit(0);
    }        

    private void readPltagConllExamples(List<PltagExample> pltagExamples, List<PltagExample> conllExamples)
    {
        boolean parsedPltag = true;
        int count = 0;
        int j = 0;
        for(int i = 0; i < conllExamples.size() && i < pltagExamples.size() && count < opts.maxNumOfExamples; i++)
        {
            PltagExample conll = conllExamples.get(i);
            String conllSentence = conll.getName();
            String pltagSentence = Utils.removeTraces(pltagExamples.get(j).getGoldStandardArray()[0]);
            // check whether the pltag sentence has a hyphen or slash within a word (but not as an isolated word), 
            // and simultaneously whether the conll sentence doesn't contain a hyphen or slash within a word
            // and hence is not split in separate tokens
            List<Integer> hyphenSlashInWord = Utils.hyphenSlashInWordIds(pltagSentence);
            if(hyphenSlashInWord.size() > 0)
            {                
                conllSentence = Utils.rejoinHyphenSlashesConll(conll, hyphenSlashInWord, pltagSentence);
            }
            if(pltagSentence.equals("NOT PARSED"))
            {
                parsedPltag = false;
            }
            if(conllSentence.equals(pltagSentence)) // conll and pltag identical
            {
//                LogInfo.logs(conllSentence + " SAME\n" + pltagSentence + " SAME\n");
                readExamples(pltagExamples.get(j), conll);
                count++;
                j++;
            }            
            else 
            {   // poll the next sentence in pltag
                String nextPltagSentence = Utils.removeTraces(pltagExamples.get(j+1).getGoldStandardArray()[0]);
                hyphenSlashInWord = Utils.hyphenSlashInWordIds(nextPltagSentence);
                if(hyphenSlashInWord.size() > 0)
                {                
                    conllSentence = Utils.rejoinHyphenSlashesConll(conll, hyphenSlashInWord, nextPltagSentence);
                }
                if(!parsedPltag)
                {
                    if(conllSentence.equals(nextPltagSentence)) // conll skipped sentence, pltag didn't parse: advance both
                    {                    
                        count++;
//                        LogInfo.logs(conllSentence + " SKIPPED\n" + nextPltagSentence + " NOT PARSED\n");
                        readExamples(pltagExamples.get(j+1), conll);
                        j+= 2;
                    } // if
                    else // conll parsed ok, but pltag didn't: give an empty example
                    {                        
//                        LogInfo.logs(conllSentence + " SAME\n" + pltagSentence + " NOT PARSED\n");
                        readExamples(pltagExamples.get(j), conll);
                        j++;
                    }
                } // if !parsedPltag
                // conll skipped sentence, but pltag parsed ok: roll back conll and advance pltag. 
                // This way we will re-process conll with the next pltag, in the next iteration.
                else if(conllSentence.equals(nextPltagSentence))
                {
                    //count++;
//                    LogInfo.logs(conllSentence + " SKIPPED\n" + nextPltagSentence + " SAME\n");
                    i--; j++;
                }
                // conll skipped sentence, pltag parsed ok, next pltag didn't parse, so give an empty example and advance both
                else if(nextPltagSentence.equals("NOT PARSED"))
                {                    
//                    LogInfo.logs(conllSentence + " SKIPPED\n" + pltagSentence + " SAME\n");
//                    LogInfo.logs(conllSentence + " SAME\n" + nextPltagSentence + " NOT PARSED\n");
                    readExamples(pltagExamples.get(j+1), conll); // empty pltag example
                    j+= 2;
                }
                else // conll parsed ok, but missing entirely from pltag file
                {
                    LogInfo.logs(conllSentence + " ERROR\n" + pltagSentence + " ERROR\n");
                        readExamples(new PltagExample("unknown", "NOT PARSED", false), conll); // empty example
                }
            } // else                        
            parsedPltag = true;
            
        }
        LogInfo.logs(String.format("Succesfully identified %s identical "
                + "sentences out of %s (CoNLL) and %s (Penn Treebank)", 
                count, conllExamples.size(), pltagExamples.size()));
    }
    
    protected void readList(List<PltagExample> list)
    {
        int count = 0;
        for(PltagExample ex : list)
        {
            if(count++ <= opts.maxNumOfExamples)
                readExamples(ex);
        }
    }        
    
    private void readExamplesOnlyList()
    {
        if(opts.useExamplesOnlyList != null)
        {            
            useExamplesOnlyList = Arrays.asList(Utils.readLines(opts.useExamplesOnlyList));
        }
    }
        
    public String getCutOffCorrectedMainLex(String string)
    {
        return Utils.getCutOffCorrectedMainLex(string, listOfFreqWords, opts.train, opts.fullLex);        
    }                
    
    private ArrayList<Integer[]> readSingleSentIDs(String filename)
    {       
        ArrayList<Integer[]> idList = new ArrayList<Integer[]>();
        for(String line : Utils.readLines(filename))
        {
            line = line.trim();
            int minusIndex = line.indexOf("-");
            int section = Integer.parseInt(line.substring(4, minusIndex));
            int part = Integer.parseInt(line.substring(minusIndex + 1, minusIndex + 3));
            int file = Integer.parseInt(line.substring(line.indexOf("lex.") + 4, line.indexOf(".txt")));
            int beam = -1;
            if (line.contains("\t"))
            {
                beam = Integer.parseInt(line.substring(line.indexOf("\t") + 1));
            }                    
            idList.add(new Integer[] {section, part, file, beam});
        }
        
        return idList;
    }

    private void calcProbs()
    {
//        HashMap<String, Integer> bigFreqMapTreeEst = new HashMap<String, Integer>();
////        TreeProbElement.readMap(parserPath + "TreeFrequencies.txt.SeptLevel1Only", bigFreqMapTreeEst);
//        TreeProbElement.readMap(opts.treeFrequencies, bigFreqMapTreeEst);
        Map<String, Integer> freqMapTreeEst = new HashMap<String, Integer>();
        for (String key : params.getFreqMapTree().keySet())
        {
            if (key.startsWith("1\t"))
            {                            
                TreeProbElement tpe = new TreeProbElement(key);
                int num = params.getFreqMapTree().get(key);
                FreqCounter fc = new FreqCounter(opts);
                fc.expandTreeProbEl(tpe);
                for (TreeProbElement tpe2 : fc.getTreeProbs())
                {
                    String tpekey = tpe2.toString();
                    if (freqMapTreeEst.containsKey(tpekey))
                    {
                        freqMapTreeEst.put(tpekey, freqMapTreeEst.get(tpekey) + num);
                    }
                    else
                    {
                        freqMapTreeEst.put(tpekey, num);
                    }
                }
            }
        }
        params.setFreqMapTree(freqMapTreeEst);
        

//        HashMap<String, Integer> bigFreqMapWordEst = new HashMap<String, Integer>();
//        WordProbElement.readMap(opts.wordFrequencies, bigFreqMapWordEst);
//        this.bigFreqMapWord = bigFreqMapWordEst;
//        HashMap<String, Integer> bigFreqMapStructEst = new HashMap<String, Integer>();
//        SuperTagStructElement.readMap(opts.superTagStruct, bigFreqMapStructEst);
//        //	SuperTagStructElement.readMap(parserPath+"SuperTagStruct.txt.Aug2012n", bigFreqMapStructEst);
//        this.bigFreqMapStruct = bigFreqMapStructEst;
//        HashMap<String, Integer> bigFreqMapFringeEst = new HashMap<String, Integer>();
//        SuperTagElement.readMap(opts.superTagFringe, bigFreqMapFringeEst);
//        //	SuperTagElement.readMap(parserPath+"SuperTagFringe.txt.Aug2012n", bigFreqMapFringeEst);
//        this.bigFreqMapFringe = bigFreqMapFringeEst;
        writeParams("combinednew-nononeadj");
     
    }
        
    protected void printOutput(PrintWriter writer, String out)
    {
        synchronized(writer)
        {
            writer.println(out);
            writer.flush();            
        }          
    }
    
    protected void printOutput(PrintWriter writer, String out, String filename)
    {
        synchronized(writer)
        {
            writer.append(out + "\n");
            writer.flush();            
            writer.close();
            try
            {
                testFullPredOut = new PrintWriter(new FileOutputStream(filename, true));
            }        
            catch(FileNotFoundException e)
            {
                System.out.println("File not found" + filename);
            }
        }  
    }
 
    protected void printOutput(final PrintWriter writer, String out, String name, boolean append)
    {
        if(opts.numThreads == 1)
            printOutput(writer, out);
        else
            printOutputIncremental(name, out, append);
    }
    /**
     * Save output to memory first rather than directly to disk. This is essential when
     * running the parser with many threads, to ensure that the order of output does
     * not get mixed.
     * @param name the sentence id
     * @param out the output to write
     * @param append start a new entry or append output
     */
    protected void printOutputIncremental(String name, String out, boolean append)
    {
        if(append)
        {
            String currentOutput = incrementalExamplesOutputMap.get(name);            
            incrementalExamplesOutputMap.put(name, (currentOutput != null ? currentOutput + "\n" : "") + out);
        }
        else
        {
            incrementalExamplesOutputMap.put(name, out);
        }
    }
    
    protected void printDependencies(final PrintWriter writer, String out, String name, boolean append)
    {
        if(opts.numThreads == 1)
            printOutput(writer, out);
        else
            printDependenciesIncremental(name, out, append);
    }
    /**
     * Save incremental dependencies output to memory first rather than directly to disk. This is essential when
     * running the parser with many threads, to ensure that the order of output does
     * not get mixed.
     * @param name the sentence id
     * @param out the output to write
     * @param append start a new entry or append output
     */
    protected void printDependenciesIncremental(String name, String out, boolean append)
    {
        if(out != null && !out.equals(""))
        {
            if(append)
            {
                String currentOutput = incrementalDependenciesOutputMap.get(name);            
                incrementalDependenciesOutputMap.put(name, (currentOutput != null ? currentOutput + "\n" : "") + out);
            }
            else
            {
                incrementalDependenciesOutputMap.put(name, out);
            }
        }        
    }
    
    
    protected void record(String name, FullStatFig complexity, boolean outputPerformance)
    {
        Utils.logs("Inference complexity: %s", complexity);
        if (!(performance == null || performance.isEmpty()))
        {
            performance.record(name);
            if(outputPerformance)
                performance.output(Execution.getFile(name+".performance"));
        }        
    }
    
    public String summary(int i)
    {        
        return (opts.train ? "train: " : "test: ") + performance.summary();     
    } 
    
    protected abstract void readExamples(PltagExample example);
    
    protected abstract void readExamples(PltagExample pltagExample, PltagExample conllExample);
    
    public abstract void newParams();
    
    protected abstract Performance newPerformance();
    
    protected abstract void loadParamsFromDisk();
    
    protected abstract void writeParams(String id); // TODO: FIX (when we add Vecs)    
    
    protected abstract void writeParamsObj(String id);   
    
    protected abstract void appendParams(String id);   

    protected abstract void updateParams(FreqCounter lastOccurrences, FreqCounter newOccurences);

    public abstract void train();
    
    public abstract void parse();
    
    public abstract void interactiveParse();
    
    public PltagParams getParams()
    {
        return params;
    }

    public Options getOpts()
    {
        return opts;
    }

    public List<Example> getExamples()
    {
        return examples;
    }
        
    public void testTrain()
    {
        train();
    }
    
    public void testParse()
    {
        parse();
    }
    
    public void testInteractiveParse()
    {
        interactiveParse();
    }
    
    private void createPartialLexiconFiles(String dir, String filename, String id)
    {
        try
        {
            String cmd = String.format("sh resources/generateLexicaID.sh %s %s %s", dir, filename, id);//lex.1.txt            
            Process p = Runtime.getRuntime().exec(cmd);
            p.getOutputStream().flush();
            p.waitFor();
        }
        catch (Exception e)
        {
            LogInfo.error(e);
        }
    }       
    
    class LexFilter implements FilenameFilter {

        @Override
	public boolean accept(File dir, String name) {
		return (name.startsWith("lex.") && name.endsWith(".txt"));
	}

    }
}