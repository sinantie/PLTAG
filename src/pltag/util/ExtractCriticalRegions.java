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

import fig.basic.EvalResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import fig.basic.Pair;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author sinantie
 */
public class ExtractCriticalRegions
{
    private final String originalDataPath, criticalPath, criticalSrlPath, diffScoresPath, outputPath, dataset;

    public ExtractCriticalRegions()
    {
        this.originalDataPath = null;
        this.criticalPath = null;
        this.criticalSrlPath = null;
        this.diffScoresPath = null;
        this.outputPath = null;
        this.dataset = null;
    }

    
    public ExtractCriticalRegions(String originalDataPath, String criticalPath, String criticalSrlPath, 
            String diffScoresPath, String outputPath, String dataset)
    {
        this.originalDataPath = originalDataPath;
        this.criticalPath = criticalPath;
        this.criticalSrlPath = criticalSrlPath;
        this.diffScoresPath = diffScoresPath;
        this.outputPath = outputPath;
        this.dataset = dataset;
    }   
        
    public void execute()
    {
        File originalDataFolder = new File(originalDataPath);
        File criticalFolder = new File(criticalPath);        
        File diffScoresFolder = new File(diffScoresPath);
        if(originalDataFolder.exists() && criticalFolder.exists() && diffScoresFolder.exists())
        {
            for(File originalDataFile : originalDataFolder.listFiles())
            {
                String experimentName = Utils.stripExtension(originalDataFile.getName());
//                extractCriticalRegion(experimentName, originalDataFile.getAbsolutePath());
                computeDependencyAccuracy(experimentName, originalDataFile.getAbsolutePath());
            }
        }
    }
    
    private void extractCriticalRegion(String experiment, String originalDataFilename)
    {
        // sanity check: see if all files exist in place
        File criticalFile = new File(criticalPath + experiment + ".txt");        
        File diffScoresFile = new File(diffScoresPath + experiment + "/test.full-pred-gen");        
        if(criticalFile.exists() && diffScoresFile.exists())
        {
            // read original dataset and tokenize
            String[] originalExamples = Utils.readLines(originalDataFilename);
            String[] criticalExamples = Utils.readLines(criticalFile.getAbsolutePath());
            DifficultyExample[] diffScoresExamples = parseDiffScoreExamples(Utils.readLines(diffScoresFile.getAbsolutePath()));            
            
            // copy-paste original diffScoreExamples to disk to output directory for convenience
            writeDifficultyExamples(diffScoresExamples, outputPath + experiment + ".full-pred-gen", false);
            
            if(originalExamples.length != criticalExamples.length || diffScoresExamples.length != originalExamples.length ||
                    diffScoresExamples.length != criticalExamples.length)
            {
                System.err.println("Lengths don't match for " + experiment);
            }
            else
            {
                System.out.println(criticalFile);
                for(int i = 0; i < originalExamples.length; i++)
                {
                    Pair<Integer, String>[] criticalRegionsIds = extractCriticalRegionIds(originalExamples[i], criticalExamples[i]);
                    diffScoresExamples[i].retainCriticalRegion(criticalRegionsIds);
                }
                writeDifficultyExamples(diffScoresExamples, outputPath + experiment + ".critical", true);
            }
        }        
    }
        
    public DifficultyExample[] parseDiffScoreExamples(String[] lines)
    {
        List<DifficultyExample> list = new ArrayList<>();
        String name = "", tree = "";
        List<String> scoreLines = new ArrayList<>();
        for(int i = 0; i < lines.length; i++)
        {
            // save parsed example, and start parsing the new one
            if(lines[i].startsWith("Example_") && !name.equals("") && !lines[i].equals(name))
            {
                list.add(new DifficultyExample(name, tree, scoreLines.toArray(new String[0])));
                scoreLines = new ArrayList<>();
                name = lines[i];
                tree = "";
            }
            else if(lines[i].startsWith("Example_")) // first example only
            {
                name = lines[i];
            }
            else if(lines[i].startsWith("(")) // found a parse tree
            {
                tree = lines[i];
            }
            else if(!lines[i].equals(""))
            {
                scoreLines.add(lines[i]);
            }                
        }
        // write the last example
        if(!scoreLines.isEmpty())
            list.add(new DifficultyExample(name, tree, scoreLines.toArray(new String[0])));
        return list.toArray(new DifficultyExample[0]);
    }
    
    public void writeDifficultyExamples(DifficultyExample[] examples, String path, boolean criticalFormat)
    {
        StringBuilder str = new StringBuilder();
        for(DifficultyExample ex : examples)
        {
            str.append(criticalFormat ? ex.toStringCriticalFormat() : ex.toString());
        }
        Utils.write(path, str.toString());
    }
    
    /**
     * 
     * Critical region sentence looks like : [1] concealed most of the night [2] was discovered.
     * [1] and [2] denote the labels of the regions.
     * @param originalSentence
     * @param criticalRegion
     * @return 
     */
    private Pair<Integer, String>[] extractCriticalRegionIds(String originalSentence, String criticalRegion)
    {
        String[] words = originalSentence.split(" ");
        String[] criticalWords = criticalRegion.split(" ");
        List<Pair<Integer, String>> ids = new ArrayList<>();
        int j = 0;
        String currentLabel = "";
        for(int i = 0; i < words.length && j < criticalWords.length; i++)
        {
            if(criticalWords[j].startsWith("["))
            {
                currentLabel = criticalWords[j];
                j++;
            }
            if(words[i].equals(criticalWords[j]))
            {
                ids.add(new Pair(i, currentLabel));
                j++;
            }
        }
        return ids.toArray(new Pair[0]);
    }
    
    /**
     * 
     * Compute SRL-related statistics from parser output, given a gold standard set of critical regions per example
     * and pattern(s) of desirable behaviour to look out for. We require two input files, namely the list of 
     * completed dependencies per example from the parser, and the critical regions of arguments and predicates
     * present in each example, along with a triple pattern. Note that the pattern should always be the first line of the
     * critical regions file. For example:<br />
     * <0,[ARG1],1>	\<2,[ARG0],1><br/>
     * [0] edited [1] magazine [2] amused<br />
     * means that we expect to see the triple <edited,[ARG1],magazine> in the list of dependencies first, and then 
     * the triple <amused,[ARG0], magazine>. Ideally, the former should be observed after parsing the word denoted by the argument, 
     * here [1] = magazine, and then the latter should 'replace' it upon parsing the verb denoted in [2] = amused (STRICT). 
     * This condition can be relaxed, and just observe the triples in succession (SUCCESSION). We can further relax this condition
     * and observe just the two triples (SRL).
     * @param experiment
     * @param originalDataFilename 
     */
    private void computeDependencyAccuracy(String experiment, String originalDataFilename)
    {
        // sanity check: see if all files exist in place        
        File criticalSrlFile = new File(criticalSrlPath + experiment + ".txt");        
        File dependenciesFile = new File(diffScoresPath + experiment + "/test.completed-dependencies");
        if(criticalSrlFile.exists() && dependenciesFile.exists())
        {
            DependencyExample[] depsExamples = parseDependencies(Utils.readLines(dependenciesFile.getAbsolutePath()));
            String[] lines = Utils.readLines(criticalSrlFile.getAbsolutePath());
            String[] experimentPattern = lines[0].split("\t");
                        
            SrlCriticalRegions[] criticalRegionsExamples = parseCriticalRegions(Arrays.copyOfRange(lines, 1, lines.length), experimentPattern);
           
            // write gold-standard SRL-triple patterns for reference            
            writeGoldSrlExamples(criticalRegionsExamples, criticalSrlPath + experiment + ".gold");

            // compute accuracy scores
            EvalResult strict = new EvalResult();
            EvalResult strictUnlabelled = new EvalResult();
            EvalResult succession = new EvalResult();
            EvalResult successionUnlabelled = new EvalResult();
            EvalResult srl = new EvalResult();
            EvalResult srlUnlabelled = new EvalResult();
            for(int i = 0; i < depsExamples.length; i++)
            {
                computeStrictScore(depsExamples[i], criticalRegionsExamples[i], strict, strictUnlabelled);
                computeSuccessionScore(depsExamples[i], criticalRegionsExamples[i], succession, successionUnlabelled);
                computeSrlScore(depsExamples[i], criticalRegionsExamples[i], srl, srlUnlabelled);
            }
            System.out.println(summary(strict, strictUnlabelled, succession, successionUnlabelled, srl, srlUnlabelled, experiment));
        }        
    }
    
    private DependencyExample[] parseDependencies(String[] lines)
    {
        List<DependencyExample> list = new ArrayList<>();
        String name = "";
        List<Dependency> triples = new ArrayList<>();
        for(int i = 0; i < lines.length; i++)
        {
            // save parsed example, and start parsing the new one
            if(lines[i].startsWith("Example_") && !name.equals("") && !lines[i].equals(name))
            {
                list.add(new DependencyExample(name, triples.toArray(new Dependency[0])));
                triples = new ArrayList<>();
                name = lines[i];                
            }
            else if(lines[i].startsWith("Example_")) // first example only
            {
                name = lines[i];
            }
            
            else if(!lines[i].equals("") && !lines[i].equals("null"))
            {
                triples.add(new Dependency(lines[i], false));
            }                
        }
        // write the last example
        if(!triples.isEmpty())
            list.add(new DependencyExample(name, triples.toArray(new Dependency[0])));
        return list.toArray(new DependencyExample[0]);
    }
    
    private void writeGoldSrlExamples(SrlCriticalRegions[] examples, String path)
    {
        StringBuilder str = new StringBuilder();
        for(SrlCriticalRegions ex : examples)
        {
            str.append(ex.goldSrlToString()).append("\n");
        }
        Utils.write(path, str.toString());
    }
    
    private SrlCriticalRegions[] parseCriticalRegions(String[] criticalSrlLines, String[] experimentPattern)
    {   
        SrlCriticalRegions[] regions = new SrlCriticalRegions[criticalSrlLines.length];
        int i = 0;
        for(String line : criticalSrlLines)
        {
            regions[i++] = new SrlCriticalRegions(line, experimentPattern);
        }
        return regions;
    }        

    /**
     * 
     * The STRICT metric captures whether the gold SRL triple(s) are observed at designated places in the sentence.
     * These places-or-words are denoted by the currentWord of the gold SRL triple.
     * @param example
     * @param criticalRegion
     * @param metric 
     */
    private void computeStrictScore(DependencyExample example, SrlCriticalRegions criticalRegion, EvalResult metric, EvalResult unlabelledMetric)
    {
        Dependency[] goldTriples = criticalRegion.srlPatterns;
        if(goldTriples.length == 1) // usually control case
        {
            Dependency goldTriple = goldTriples[0];
            String goldCueWord = goldTriple.currentWord;
            boolean foundTriple = false, foundTripleUnlabelled = false;
            for(Dependency triple : example.triples)
            {
                if(triple.equalsUnlabelled(goldTriple) && !foundTripleUnlabelled)
                {
                    foundTripleUnlabelled = true;
                    if(triple.currentWord.equals(goldCueWord))
                        unlabelledMetric.add(true, true);
                    else // found the triple, but at the wrong word: discount as a false-negative
                    {
                        unlabelledMetric.add(true, false);
                    }                     
                } // if
                if(triple.equals(goldTriple))
                {
                    foundTriple = true;
                    if(triple.currentWord.equals(goldCueWord))
                        metric.add(true, true);
                    else // found the triple, but at the wrong word: discount as a false-negative
                    {
                        metric.add(true, false);
                    } 
                    break;
                } // if                
            } // for
            if(!foundTriple)
                metric.add(true, false); // Never found the triple: discount as a false-negative
            if(!foundTripleUnlabelled)
                unlabelledMetric.add(true, false); // Never found the triple: discount as a false-negative
        } // if
        else // two gold triples, so we are looking into observing one triple first and then the other later on.
        {
            int foundTriple = 0, foundExactTriple = 0;
            int foundTripleUnlabelled = 0, foundExactTripleUnlabelled = 0;
            for(Dependency goldTriple : goldTriples)
            {
                String goldCueWord = goldTriple.currentWord;            
                for(Dependency triple : example.triples)
                {                    
                    if(triple.equalsUnlabelled(goldTriple) && foundTripleUnlabelled < goldTriples.length)
                    {                    
                        if(triple.currentWord.equals(goldCueWord))
                            foundExactTripleUnlabelled++;
                        else 
                            foundTripleUnlabelled++;                        
                    } // if
                    if(triple.equals(goldTriple) && foundTriple < goldTriples.length)
                    {                        
                        if(triple.currentWord.equals(goldCueWord))
                            foundExactTriple++;
                        else 
                            foundTriple++;
                        break;
                    } // if                    
                } // for
            } // for
            if(foundExactTriple == goldTriples.length) // found all triples at the correct position
                metric.add(true, true);
            else if(foundTriple == goldTriples.length) // found all triples, but at the wrong words: discount as a false-negative
            {
                metric.add(true, false);
            }
            else
                metric.add(false, true); // didn't find any triple: discount as a false-negative
            if(foundExactTripleUnlabelled == goldTriples.length) // found all triples at the correct position
                unlabelledMetric.add(true, true);
            else if(foundTripleUnlabelled == goldTriples.length) // found all triples, but at the wrong words: discount as a false-negative
            {
                unlabelledMetric.add(true, false);
            }
            else
                unlabelledMetric.add(true, false); // didn't find any triple: discount as a false-negative
        } // else
    }
    
    /**
     * 
     * The SUCCESSION metric measures whether the gold SRL triple(s) are observed in the correct order in the sentence, regardless of
     * the word they appear in. In case there is only one gold SRL triple, this metric collapses with the SRL metric.
     * @param example
     * @param criticalRegion
     * @param metric 
     */
    private void computeSuccessionScore(DependencyExample example, SrlCriticalRegions criticalRegion, EvalResult metric, EvalResult unlabelledMetric)
    {
        Dependency[] goldTriples = criticalRegion.srlPatterns;
        if(goldTriples.length == 1) // usually control case
        {
            Dependency goldTriple = goldTriples[0];
            boolean foundTriple = false, foundTripleUnlabelled = false;
            for(Dependency triple : example.triples)
            {
                if(triple.equalsUnlabelled(goldTriple) && !foundTripleUnlabelled)
                {
                    foundTripleUnlabelled = true;                    
                    unlabelledMetric.add(true, true);                    
                } // if
                if(triple.equals(goldTriple))
                {
                    foundTriple = true;                    
                    metric.add(true, true);                    
                    break;
                }
            }
            if(!foundTriple)
                metric.add(true, false); // Never found the triple: discount as a false-negative
            if(!foundTripleUnlabelled)
                unlabelledMetric.add(true, false); // Never found the triple: discount as a false-negative
        }
        else // two gold triples, so we are looking into observing one triple first and then the other later on.
        {
            int foundTriple = 0;
            int foundTripleUnlabelled = 0;
            for(Dependency goldTriple : goldTriples)
            {                
                for(Dependency triple : example.triples)
                {
                    if(triple.equalsUnlabelled(goldTriple))
                    {                                                
                        foundTripleUnlabelled++;                                                
                    } // if
                    if(triple.equals(goldTriple))
                    {                                                
                        foundTriple++;
                        break;
                    } // if                    
                } // for
            } // for
            if(foundTriple == goldTriples.length) // found all triples at the correct position
                metric.add(true, true);            
            else
                metric.add(false, true); // didn't find any triple: discount as a false-negative
            if(foundTripleUnlabelled == goldTriples.length) // found all triples, but at the wrong words: discount as a false-negative
            {
                unlabelledMetric.add(true, true);
            }
            else
                unlabelledMetric.add(true, false); // didn't find any triple: discount as a false-negative
        }
    }
        
    /**
     * 
     * The SRL metric measures whether the gold SRL triple(s) are observed anywhere in the sentence, regardless of
     * the word they appear in. In case there is only one gold SRL triple, this metric collapses with the SUCCESION metric.
     * @param example
     * @param criticalRegion
     * @param metric 
     */
    private void computeSrlScore(DependencyExample example, SrlCriticalRegions criticalRegion, EvalResult metric, EvalResult unlabelledMetric)
    {
        Dependency[] goldTriples = criticalRegion.srlPatterns;
        for(Dependency goldTriple : goldTriples)
        {            
            int foundTriple = 0, foundTripleUnlabelled = 0;
            for(Dependency triple : example.triples)
            {
                if(triple.equalsUnlabelled(goldTriple) && foundTripleUnlabelled < goldTriples.length )
                {
                    foundTripleUnlabelled++;                    
                    unlabelledMetric.add(true, true);                    
                }
                if(triple.equals(goldTriple) && foundTriple < goldTriples.length)
                {
                    foundTriple++;                    
                    metric.add(true, true);                    
                    break;
                }                
            }
            if(foundTriple != goldTriples.length)
                metric.add(true, false); // Never found the triple: discount as a false-negative
            if(foundTripleUnlabelled != goldTriples.length)
                unlabelledMetric.add(true, false);
        }       
    }
    
    public String summary(EvalResult strict, EvalResult strictUnlabelled, EvalResult succession, EvalResult successionUnlabelled, EvalResult srl, EvalResult srlUnlabelled, String experimentName)
    {
        StringBuilder str = new StringBuilder(experimentName + "\n-----------\n");
        str.append("STRICT: ").append(strict.toString()).append("\n");
        str.append("STRICT (unlabelled): ").append(strictUnlabelled.toString()).append("\n");
        str.append("SUCCESSION: ").append(succession.toString()).append("\n");
        str.append("SUCCESSION (unlabelled): ").append(successionUnlabelled.toString()).append("\n");
        str.append("SRL: ").append(srl.toString()).append("\n");
        str.append("SRL (unlabelled): ").append(srlUnlabelled.toString()).append("\n\n");
        return str.toString();
    }
    
    public class DifficultyExample
    {
        String name, tree;
        String[] scoreLines;
        Pair<Integer, String>[] criticalIds;
        
        public DifficultyExample(String name, String tree, String[] scoreLines)
        {
            this.name = name;
            this.tree = tree;
            this.scoreLines = scoreLines;
        }

        private void retainCriticalRegion(Pair<Integer, String>[] criticalRegionsIds)
        {            
            String[] scoreLinesNew = new String[criticalRegionsIds.length];
            int i = 0;
            for(Pair<Integer, String> pair : criticalRegionsIds)
            {
                scoreLinesNew[i++] = scoreLines[pair.getFirst()];
            }
            scoreLines = scoreLinesNew;
            criticalIds = criticalRegionsIds;
        }
        
        private String toStringCriticalFormat()
        {
            StringBuilder str = new StringBuilder();
            for(int i  = 0; i < scoreLines.length; i++)
            {
                String[] toks = scoreLines[i].split(";");
                String[] posWord = toks[1].trim().split(" ");
                str.append(String.format("exampleId: %s\tid: %s\tlabel: %s\t%s\tword: %s\tpos: %s\n", 
                        name, criticalIds[i].getFirst(), criticalIds[i].getSecond(), toks[0], posWord[1], posWord[0]));
            }
            return str.toString();
        }
        
        public String toStringNoTree()
        {
            StringBuilder str = new StringBuilder(name + "\n");
            for(String scoreLine : scoreLines)
            {
                str.append(scoreLine).append("\n");
            }
            return str.toString();
        }
        
        @Override
        public String toString()
        {
            StringBuilder str = new StringBuilder(toStringNoTree());            
            if(tree != null && !tree.equals(""))
                str.append(tree).append("\n");
            str.append("\n");
            return str.toString();
        }               
        
    }
    
    class DependencyExample
    {
        String name;
        Dependency[] triples;

        public DependencyExample(String name, Dependency[] triples)
        {
            this.name = name;
            this.triples = triples;
        }

        @Override
        public String toString()
        {
            StringBuilder str = new StringBuilder(name).append("\n");
            if(triples != null)
            {
                for(Dependency triple : triples)
                    str.append(triple).append("\n");
            }
            return str.toString();
        }
        
        
    }
    
    class Dependency
    {
        String pred, arg, role, currentWord, currentPos;
        int currentId;

        /**
         * 
         * Dependency constructor. 
         * If simple is false then the input line should look like: 
         * <pred,[role],arg>\tid: num\tword:POS(optional) word.
         * Otherwise it parses a normal triple.
         * @param line 
         * @param
         */
        public Dependency(String line, boolean simple)
        {            
            if(simple)
            {
                String[] tripleToks = parseTriple(line);
                if(tripleToks.length == 3)
                {                
                    pred = tripleToks[0]; 
                    role = filterRole(tripleToks[1]); 
                    arg = tripleToks[2];
                }
                else if(tripleToks.length == 4)
                {
                    pred = tripleToks[0]; 
                    role = filterRole(tripleToks[1]); 
                    arg = tripleToks[2];
                    currentWord = tripleToks[3];
                }
            }
            else
            {
                String[] toks = line.split("\t");            
                String[] tripleToks = parseTriple(toks[0]);
                if(tripleToks.length == 3)
                {                
                    pred = tripleToks[0]; 
                    role = filterRole(tripleToks[1]); 
                    arg = tripleToks[2];
                }
                currentId = Integer.valueOf(toks[1].split(":")[1].trim());
                int index = toks[2].indexOf(":");
                String[] posWordToks = toks[2].substring(index + 1).split(" ");
                if(posWordToks.length == 2) // POS word format
                {                    
                    currentPos = posWordToks[0];
                    currentWord = posWordToks[1];
                }
                else
                {
                    currentPos = "";
                    currentWord = posWordToks[0];
                }  
            }                      
        }

        public Dependency(String pred, String role, String arg)
        {
            this(pred, role, arg, null);
        }
        
        public Dependency(String pred, String role, String arg, String currentWord)
        {
            this.pred = pred;
            this.role = filterRole(role);
            this.arg = arg;
            this.currentWord = currentWord;
        }
        
        private String[] parseTriple(String triple)
        {
            int index = triple.lastIndexOf(">");
            String[] toks = triple.substring(1, index).split(",");
            toks[1] = toks[1].substring(1, toks[1].length() - 1);
            int indexOfCurrentWord = triple.lastIndexOf(":");
            if(indexOfCurrentWord >= 0)
            {
                String curWord = triple.substring(indexOfCurrentWord + 1);
                toks = Arrays.copyOf(toks, toks.length + 1);
                toks[toks.length - 1] = curWord;
            }
            return toks;
        }
        
        private String filterRole(String roleIn)
        {            
            if(roleIn.matches("ARG[0-9]-"))
            {
                int index = roleIn.indexOf("-");
                return roleIn.substring(0, index);
            }
            return roleIn;
        }
        
        private String tripleToString()
        {
            return "<" + pred + ",[" + role + "]," + arg + ">";
        }
        
        public boolean equalsUnlabelled(Object obj)
        {
            assert obj instanceof Dependency;
            Dependency d = (Dependency)obj;
            return pred.equals(d.pred) && arg.equals(d.arg);
        }
        
        @Override
        public boolean equals(Object obj)
        {
            assert obj instanceof Dependency;
            Dependency d = (Dependency)obj;
            return pred.equals(d.pred) && role.equals(d.role) && arg.equals(d.arg);
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 41 * hash + Objects.hashCode(this.pred);
            hash = 41 * hash + Objects.hashCode(this.arg);
            hash = 41 * hash + Objects.hashCode(this.role);
            return hash;
        }
        
        
        @Override
        public String toString()
        {
            return currentPos != null && currentWord != null ? 
                    String.format("%s\tid:%s\tword:%s %s", tripleToString(), currentId, currentPos, currentWord) : tripleToString();
        }        
    }
    
    /**
     * 
     * Represents the critical regions of a single example, and the resulting gold standard SRL 
     * given a pattern
     */
    class SrlCriticalRegions
    {
        Map<String, CriticalRegion> regions;
        Dependency[] srlPatterns;
        
        public SrlCriticalRegions(String line, String[] experimentPattern)
        {
            regions = new LinkedHashMap<>();
            String[] toks = line.split(" ");
            String currentLabel = null, currentWord;
            
            for(String tok : toks)
            {
                if(tok.startsWith("["))
                {
                    currentLabel = tok;
                }
                else
                {
                    currentWord = tok;
                    CriticalRegion region = new CriticalRegion(currentLabel, currentWord);
                    regions.put(region.label, region);
                }
            }
            srlPatterns = new Dependency[experimentPattern.length];
            int i = 0;
            for(String pattern : experimentPattern)
            {
                srlPatterns[i++] = createSrl(regions, pattern);
            }
        }
        
        
        public void add(CriticalRegion region)
        {
            regions.put(region.label, region);
        }

        private Dependency createSrl(Map<String, CriticalRegion> regions, String pattern)
        {
            Dependency patternSrl = new Dependency(pattern, true);
            String pred = regions.get(patternSrl.pred).word;
            String arg = regions.get(patternSrl.arg).word;
            if(patternSrl.currentWord == null) // user has not assigned the current word, that this pattern SRL should be observed
            {
                boolean predIsBeforeArg = predIsBeforeArg(regions, patternSrl);
                return new Dependency(pred, patternSrl.role, arg, predIsBeforeArg ? arg : pred);
            }            
            else
            {
                String currentWord = regions.get(patternSrl.currentWord).word;
                return new Dependency(pred, patternSrl.role, arg, currentWord);
            }
        }
        
        /**
         * 
         * Decide whether the predicate is before the argument or not. We assume
         * that the region ids are traversed in the order the were added to the map.
         * @param regions
         * @param patternSrl
         * @return 
         */
        private boolean predIsBeforeArg(Map<String, CriticalRegion> regions, Dependency patternSrl)
        {
            boolean foundPredFirst = false, foundArgFirst = false;
            for(String label : regions.keySet())
            {
                if(label.equals(patternSrl.pred))
                {
                    foundPredFirst = true;
                    break;
                }
                else if(label.equals(patternSrl.arg))
                {
                    foundArgFirst = true;
                    break;
                }
            }
            return foundPredFirst && !foundArgFirst;
                
        }
        
        public String goldSrlToString()
        {
            StringBuilder str = new StringBuilder();
            for(Dependency triple : srlPatterns)
            {
                str.append(triple).append("\t");
            }
            return str.toString();
        }
        
        @Override
        public String toString()
        {
            StringBuilder str = new StringBuilder();
            for(CriticalRegion region : regions.values())
            {
                str.append(region.toString()).append(" ");
            }
            return str.length() > 0 ? str.deleteCharAt(str.length() - 1).toString() : str.toString();
        }                       
    }
    
    class CriticalRegion
    {
        String label, word;

        public CriticalRegion(String label, String word)
        {
            this.label = label.startsWith("[") ? label.substring(1, label.lastIndexOf("]")) : label;
            this.word = word;
        }

        @Override
        public String toString()
        {
            return String.format("[%s] %s", label, word);
        }                
    }
    
    public static void main(String[] args)
    {
        // MC-RR, NP-S, NP-0, PP
        String dataset = "MC-RR-simple";
        String inputPath = "data/pado_stimuli/Items/";
        String masterOutputPath = "results/output/pltag/test/pado_stimuli/discriminative/";
//        String masterOutputPath = "results/output/pltag/test/pado_stimuli/generative/";
        String originalDataPath = inputPath + dataset + "/";
        String criticalPath = inputPath + dataset + "-critical/";
        String criticalSrlPath = inputPath + dataset + "-SRL-critical/";
        String diffScoresPath = masterOutputPath + "POS-gold-SRL/"+dataset+"/";
//        String diffScoresPath = masterOutputPath + "POS-gold/"+dataset+"/";
        String outputPath = masterOutputPath + "pado_stimuli_output-POS-gold-SRL/"+dataset+"/";
        ExtractCriticalRegions ecr = new ExtractCriticalRegions(originalDataPath, criticalPath, criticalSrlPath, diffScoresPath, outputPath, dataset);
        ecr.execute();
    }    
}
