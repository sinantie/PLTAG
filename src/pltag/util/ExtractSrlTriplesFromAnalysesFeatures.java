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

import com.esotericsoftware.wildcard.Paths;
import fig.basic.IOUtils;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import pltag.parser.semantics.discriminative.DiscriminativeFeatureIndexers;
import pltag.parser.semantics.discriminative.ExtractFeatures;
import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalyses;
import pltag.parser.semantics.discriminative.incrAnalyses.IncrementalAnalysis;
import pltag.parser.semantics.discriminative.incrAnalyses.NbestIncrementalAnalyses;

/**
 *
 * @author sinantie
 */
public class ExtractSrlTriplesFromAnalysesFeatures
{
    private final String analysesFeaturesPath, featureIndexersFilename, outputFilename;
    private final int numThreads;
    private final Set<Integer> srlTriples;
    DiscriminativeFeatureIndexers featureIndexers;
    
    public ExtractSrlTriplesFromAnalysesFeatures(String analysesFeaturesPath, String featureIndexersFilename, String outputFilename, int numThreads)
    {
        this.analysesFeaturesPath = analysesFeaturesPath;
        this.featureIndexersFilename = featureIndexersFilename;
        this.outputFilename = outputFilename;
        this.numThreads = numThreads;
        srlTriples = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    }

   public void execute()
   {
       // load featuerIndexers
       loadFeatureIndexers();
       // read examples        
        String[] paths = getBatchExamplesPaths();
        int i = 1;
        Collection<Worker> list = new ArrayList(paths.length);
        for (String path : paths)
        {
            list.add(new Worker(path, i++, paths.length));
            
        }
        Utils.parallelForeach(numThreads, list);
        saveSrlTriples();
   }       
    
    /**
     * 
     * We store incremental analyses in stand-alone batches batches. This method
     * retrieves their filenames in an array of Strings
     * @return 
     */
    private String[] getBatchExamplesPaths()
    {        
        File f = new File(analysesFeaturesPath);
        Paths paths = new Paths(f.getParent(), f.getName() + "-*");
        return paths.getPaths().toArray(new String[0]);
    }
    
    public void loadFeatureIndexers()
    {
        System.out.println("Loading feature indexers from " + featureIndexersFilename);
        featureIndexers = (DiscriminativeFeatureIndexers) IOUtils.readObjFileEasy(featureIndexersFilename + ".obj.gz");
    }
    
    public void saveSrlTriples()
    {
        System.out.print("Writing to file " + outputFilename + "... " + srlTriples.size() + " unique SRL triples.");
        PrintWriter out = IOUtils.openOutEasy(outputFilename);
        for(Integer srlTriple : srlTriples)
        {
            String srlTripleIds = featureIndexers.getIndexer(ExtractFeatures.FEAT_SRL_TRIPLES).getObject(srlTriple);
            String[] tokens = srlTripleIds.split(",");
            String role = featureIndexers.getRole(Integer.valueOf(tokens[0]));
            String arg = featureIndexers.getWord(Integer.valueOf(tokens[1]));
            String pred = featureIndexers.getWord(Integer.valueOf(tokens[2]));
            out.format("%s,%s,%s\n", arg, pred, role);
        }
        System.out.println("Done");
        out.close();
    }
    
    
    class Worker extends CallableWithLog
    {
        
        String inputPath;
        int currentFileNo;
        int total;
        
        public Worker(String inputPath, int currentFileNo, int total)
        {
            this.inputPath = inputPath;
            this.currentFileNo = currentFileNo;
            this.total = total;            
        }                
        
        @Override
        public Object call() throws Exception
        {            
            ExtractFeatures features = readFeatures(inputPath, currentFileNo, total);
            processExamples(features);
            return null;
        }   
        
        private void processExamples(ExtractFeatures features)
        {
            for(Map.Entry<String, IncrementalAnalyses> example : features.getIncrAnalysesFeaturesSet())
            {
                for(NbestIncrementalAnalyses nBest : example.getValue().getNbestAnalyses())
                {
                    for (IncrementalAnalysis analysis : nBest.getIncrAnalyses())
                    {
                        for(int srlTriple : analysis.getSrlTriples())
                        {                            
                            srlTriples.add(srlTriple);
                        }
                    }
                }
                
            }
        }
        
        private ExtractFeatures readFeatures(String inputPath, int currentFileNo, int total)
        {        
            System.out.print("Reading incremental analyses from file " + inputPath + " ... (" + currentFileNo + "/" + total + ")");        
            ExtractFeatures features = new ExtractFeatures();
            features.loadFeatures(inputPath);            
            if (!features.isEmpty())    
            {            
                features.setFeatureIndexers(featureIndexers);
                System.out.println(" Done (read " + features.size() + " examples)");
            } 
            else
            {
                System.err.println("Error loading incremental analyses object file");
                features = new ExtractFeatures();
            }            
            System.out.println("Done");
            return features;
        }
    }
    public static void main(String[] args)
    {
        String inputPath, indexers, outputPath;
        int numThreads = 4;
        if(args.length < 3)
        {
            inputPath = "data/incrAnalyses/features/incrAnalysesFeatures_wsj_debug_0221";
            indexers = "data/incrAnalyses/features/featureIndexers_debug_extended";
            outputPath = "data/incrAnalyses/features/srlTriples_debug";
        }
        else
        {
            inputPath = args[0];
            indexers = args[1];
            outputPath = args[2];
            if(args.length > 3)
                numThreads = Integer.valueOf(args[3]);
        }
        
        ExtractSrlTriplesFromAnalysesFeatures e = new ExtractSrlTriplesFromAnalysesFeatures(inputPath, indexers, outputPath, numThreads);
        e.execute();
        
    }
}
