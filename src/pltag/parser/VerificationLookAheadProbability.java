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

import fig.basic.IOUtils;
import fig.basic.Indexer;
import fig.basic.LogInfo;
import fig.basic.Pair;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import pltag.corpus.ElementaryStringTree;
import static pltag.parser.ParsingTask.getCoveredNodes;
import pltag.parser.semantics.LexiconEntryWithRoles;
import pltag.parser.semantics.SemanticLexicon;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class VerificationLookAheadProbability
{
    
    private final transient Options opts;
    private ParserModel model;
    private transient final Set<String> listOfFreqWords;
    private final transient Indexer<String> roleIndexer;
    private transient Lexicon lexicon, predLexicon;
    private Map<String, Double> vlapMap;    
    
    public VerificationLookAheadProbability(Options opts)
    {
        this.opts = opts;
        opts.posOnly = false;
        opts.treeFamilies = true;
        listOfFreqWords = new HashSet<>(Arrays.asList(Utils.readLines(opts.listOfFreqWords)));
        roleIndexer = new Indexer<>();
    }
    
    public void readLexicons()
    {
        Utils.beginTrack("Reading lexicons....");
        lexicon = newLexicon(opts.lexicon);
        lexicon.postProcessLexicon(false);
        lexicon.extractVerificationTrees();
        predLexicon = newLexicon(opts.predLexicon);
        Utils.logs("Read %s lexical entries and %s tree templates.", lexicon.getLexSize(), Lexicon.getNumOfTreeTemps());
        LogInfo.end_track();
    }
    
    public void compute()
    {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(opts.numThreads));
        vlapMap = new HashMap<>();
        final double numOfElemTrees = lexicon.getLexSize();        
        Utils.beginTrack("Computing Verification Look-ahead Probability...");
//        if(!(predLexicon instanceof SemanticLexicon))
        {
            predLexicon.getEntries("prediction: ", "prediction: ", "", false, 0).parallelStream().forEach((predTree) ->
            {            
                String key = predTree.getTreeString();                
                vlapMap.put(key.substring(key.indexOf("\t") + 1, predTree.getTreeString().length() - 1).replaceAll(" \\( |\\( ", "("), Math.log(countCoveringElemTrees(predTree) / numOfElemTrees));
            });
        }
//        else
//        {
//            // discount raw count of covering elementary trees by multiplying with the frequency of each prediction tree in the training set 
//            // TODO: This info is made available olny in semantic lexicon implementation; consider extending    
//            double temp = 0;
//            for(LexiconEntryWithRoles entry : (Collection<LexiconEntryWithRoles>) predLexicon.getEntries("prediction: "))
//            {
//                temp += ((LexiconEntryWithRoles)entry).getFrequency();
//            }
//            double totalNumOfPredTrees = temp;
////            System.out.println("Total Elem Trees : " + numOfElemTrees + ", Total Pred Tree Frequency " + temp);
//            predLexicon.getEntries("prediction: ").parallelStream().map((entry) -> (LexiconEntryWithRoles) entry).forEach((predEntry) ->
////            for(LexiconEntryWithRoles predEntry : (Collection<LexiconEntryWithRoles>) predLexicon.getEntries("prediction: "))
//            {
//                ElementaryStringTree predTree = predLexicon.makeToStringTree(predEntry.toString(), predEntry.toString());
//                if (predTree != null)
//                {
//                    double rawCount = countCoveringElemTrees(predTree);
//                    double frequency = predEntry.getFrequency();
////                    System.out.println(rawCount + " ::: " + frequency + " ::: " + Math.log( (rawCount / numOfElemTrees) * (frequency / totalNumOfPredTrees)) + " ::: " + predEntry.getLexEntry());
//                    vlapMap.put(predEntry.getLexEntry().substring(0, predEntry.getLexEntry().length() - 1).replaceAll(" \\( |\\( ", "("), Math.log( (rawCount / numOfElemTrees) * (frequency / totalNumOfPredTrees)));
//                }
//            });
//        }
        LogInfo.end_track();
    }
    
     /**
     * 
     * Compute verification look-ahead probability (proxy to Brian Roark's LAP score, from CL 2004 paper).
     * We currently deal only with prediction trees only (substitution is more costly).
     * The process is the following:<br/>
     * 1. For each prediction tree in the lexicon,<br/>
     * 2. we find all the elementary trees that cover its nodes          
     * 3. return the sum log score of trees
     * 
     * @param shadowTreeElem
     * @return the sum log score of verification trees for the incoming prediction tree
     */
    private double countCoveringElemTrees(ElementaryStringTree shadowTreeElem)
    {
//        System.out.println(shadowTree);
        ShadowStringTree shadowTree = new ShadowStringTree(shadowTreeElem);
        int count = 0;
//        for (Integer rootnode : shadowTree.getShadowSourceTreesRootList())
        {
            int rootNode = shadowTreeElem.getRoot();
            String shadowRootCategory = shadowTreeElem.getCategory(rootNode);            
            Collection<Pair<String, String>> elemTreesWithRootCategory = lexicon.getTreeWithRootCategory(shadowRootCategory);               
            for(Pair<String, String> wordUnlexTree : elemTreesWithRootCategory)
            {
                String unlexTreeString = wordUnlexTree.getSecond();
                String treeString = lexicon.insertLex(wordUnlexTree.getFirst(), unlexTreeString);                                
                ElementaryStringTree candidateTree = lexicon.makeToStringTree(treeString, unlexTreeString);//convertTree(treeString, opts.useSemantics);            
                if(candidateTree != null)
                {
                    Map<Integer, Integer> coveredNodes = getCoveredNodes(candidateTree, candidateTree.getRoot(), shadowTree, rootNode, null, opts.verbose);
                    if(coveredNodes != null)
                    {
                        count++;
                    }                    
                }
            } 
        }               
        return count;
    }
      
    public void serialize()
    {
        if(opts.vlapPath != null)
            IOUtils.writeObjFileHard(opts.vlapPath, vlapMap);
    }
    
    public void load()
    {
        Utils.beginTrack("Loading Verification Look-ahead Probability table...");
        if(opts.vlapPath != null && new File(opts.vlapPath).exists())
        {
            vlapMap = (Map<String, Double>) IOUtils.readObjFileHard(opts.vlapPath);
        }
        else
        {
            LogInfo.logs("VLAP table not found...skipping");
        }
        LogInfo.end_track();
    }
    
    public double getVlap(ElementaryStringTree predTree)
    {
        if(vlapMap != null)
        {
            Double vlap = vlapMap.get(predTree.getTreeString());
            return vlap != null ? vlap : 0.0;
        }
        return 0.0;
    }
    
    private Lexicon newLexicon(String filename)
    {
        Lexicon lexicon = opts.useSemantics ? new SemanticLexicon(opts, listOfFreqWords, roleIndexer) : new Lexicon(opts, listOfFreqWords);        
        lexicon.processLexicon(filename);        
        return lexicon;
    }
    
    public void testCompute()
    {
        compute();
        serialize();
//        System.out.println(vlapMap.toString());
    }
    
    public void testLoad()
    {
        load();
        System.out.println(vlapMap.toString());
    }
        
}
