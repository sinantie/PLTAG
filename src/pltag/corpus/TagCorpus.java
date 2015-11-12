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
//created now.

import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.StopWatchSet;
import fig.exec.Execution;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.util.Utils;

public class TagCorpus
{
    private final TagCorpusOptions opts;
    
    private List<StringTree> lexicon;    
    //private boolean moreThanOneFoot, justMultiWordLexemes;    
    public static boolean verbose = false;
    //public static boolean useSemantics = false; // TODO: FIX
    static List<String> listErrors;   
    private Writer lexWriter, goldStandardWriter, outputWriter;
    private ErrorTracker errorTracker;
    private Map<Integer, Integer> fullList;
    private PercolTable percolTable;
    private PredictiveLexicon predictiveLexicon;    
    private PropBank propBank;
    private NomBank nomBank;
    private int totalNumOfPropositionArgs, extractedNumOfPropositionArgs;
    //private String trackDir;
        
    public TagCorpus(TagCorpusOptions opts)
    {
        this.opts = opts;
        TagCorpus.verbose = opts.verbose;        
        fullList = new HashMap<Integer, Integer>();
        lexicon = new ArrayList<StringTree>();
        percolTable = new PercolTable(opts.percolTableFilename);
        predictiveLexicon = new PredictiveLexicon(opts.predLexFilename);
        errorTracker = new ErrorTracker(verbose);
        for (int i = 0; i < 8; i++)
        {
            fullList.put(i, 0);
        }        
    }

    public void init()
    {
        try
        {            
                        
            if(opts.examplesInSingleFile)
            {
                outputWriter = IOUtils.openOut(Execution.getFile(opts.outputFilename));
            }
            else
            {
                lexWriter = IOUtils.openOut(Execution.getFile(opts.lexiconFilename));
                goldStandardWriter = IOUtils.openOut(Execution.getFile(opts.goldStandardFilename));
            }
        }
        catch (IOException ioe)
        {
            LogInfo.error(ioe);            
        }
    }
    
    /**
     * Sets up the global resources, constructs the TAG version of the PTB, extracts the lexicon from it
     * and performs an error check in the end.
     */
    public void execute()
    {        
        //Iterate over sources and store errors        
        for (int k = opts.startTrack; k <= opts.endTrack; k++)
        {
            String trackDir = (k < 10 ? "0" : "") + String.valueOf(k);            
            propBank = new PropBank(trackDir, opts.propBankFilename);
            nomBank = new NomBank(trackDir, opts.nomBankFilename);

            for (int j = 0; j < 10; j++)
            {
                List<StringTree> corpus = new ArrayList<StringTree>();
                listErrors = new ArrayList<String>();
                for (int i = 0; i < 10; i++)
                {
                    String fileId = String.format("%s%s", j, i);
                    String filename = String.format("%s/wsj_%s%s.mrg", trackDir, trackDir, fileId);                    
//                    String filename = "23/wsj_2309.mrg";
                    if(new File(opts.ptbPath.concat(filename)).exists())
//                    if(new File(opts.ptbPath.concat(trackDir.concat(filename))).exists())
                    {
                        if(opts.outputIndividualFiles)
                        {
                            singleSentExtractor(fileId, trackDir, opts.individualFilesPath);
                        }
                        else
                        {
//                            LogInfo.logs("\nProcessing "+filename+"\n");
                            corpus.addAll(makeLexicon(constructTagCorpus(filename, 
                                percolTable, predictiveLexicon, propBank, nomBank), filename));
                        }
                    }                    
                }
                errorTracker.count(corpus, listErrors, k, j);
                errorTracker.printResult();
                if (verbose)
                {
                    lexicon.addAll(corpus);
                }
            } // for
            LogInfo.logs(errorTracker.printSummary(k));
        } // for
        for (int i = 0; i < fullList.size(); i++)
        {
            LogInfo.logs(i + " " + fullList.get(i) + "\n");
        }
        LogInfo.logs(String.format("Extracted %s out of %s proposition arguments", extractedNumOfPropositionArgs, totalNumOfPropositionArgs));
        try
        { 
            if(lexWriter != null)
                lexWriter.close();
            if(goldStandardWriter != null)                
                goldStandardWriter.close();
            if(outputWriter != null)
                outputWriter.close();
        }
        catch (IOException ioe)
        {
            LogInfo.error(ioe);
        }        
    }   
    
    public void singleSentExtractor(String fileId, String trackDir, String outputPath)
    {        
        String inputFilename = String.format("%s/wsj_%s%s.mrg", trackDir, trackDir, fileId);
        PennCorpus penn = constructTagCorpus(inputFilename, percolTable, 
                predictiveLexicon, propBank, nomBank);
        List<PennTree> treeList = penn.getTrees();                
        //for each sentence        
        String outputDir = String.format("%s/%s/%s", outputPath, trackDir, fileId);
        new File(Execution.execDir + "/" + outputDir).mkdirs();        
        for (int treeNo = 0; treeNo < treeList.size(); treeNo++)
        {                
            try
            {
                lexWriter = IOUtils.openOut(Execution.getFile(
                        String.format("/%s/lex.%s.txt", outputDir, treeNo)));
                goldStandardWriter = IOUtils.openOut(Execution.getFile(
                        String.format("/%s/gs.%s.txt", outputDir, treeNo)));

                singleSentExtractor(inputFilename, treeList.get(treeNo), treeNo);                                
                lexWriter.close();
                goldStandardWriter.close();
            }
            catch(Exception ioe)
            {
                LogInfo.error(ioe);
            }
        }        
    }

    public void singleSentExtractor(String inputFilename, PennTree tree, int treeNo)
    {                                                 
        try
        {                      
            if (verbose)
            {
                printOutput(tree.getString());
            }
            List<LexEntry> lexEntries = calculateLexEntries(tree);                
            reconstructAndBuildLexicon(inputFilename, treeNo, tree, 
                    convertToElementaryStringTrees(lexEntries, inputFilename, 
                    treeNo, tree.getRandomGenerator()));                        
        }            
        catch (IOException ex)
        {
            LogInfo.error("during conversion: " + ex.getClass().toString());
        }
        catch (StackOverflowError e)
        {
            LogInfo.error("during conversion: " + e.getClass().toString());
        }        
    }

    /**
     * Reads in the Penn Treebank, annotates it with the additional info from sources 
     * and makes TAG-specific transformations     
     * @param filename
     * @param percolationTable
     * @param predictiveLexicon
     * @param PropBank
     * @param NomBank
     */
    private PennCorpus constructTagCorpus(String filename, PercolTable percolTable, PredictiveLexicon predLexicon,
                                          PropBank propBank, NomBank nb)
    {
        // step 1: read in penn treebank
//        PennCorpus penn = new PennCorpus(ptbPath + "NP_PTB/".concat(dir.concat(fileno)));
        PennCorpus penn = new PennCorpus(opts.ptbPath.concat(filename));
//        PennCorpus penn = new PennCorpus(opts.ptbPath.concat(dir.concat(filename)));
        for (PennTree tree : penn.getTrees())
        {
            if (verbose)
            {
                LogInfo.logs(tree.getString());
            }
            tree.setRoot(percolTable.setHeadsInTree(tree.getRoot()));
        }
        // step 2: annotate treebank from Nombank, Propbank, Predictive Lexicon
        penn.mergeSources(filename, propBank);
//        penn.mergeSources(dir.concat(filename), pb);
        //penn.mergeSources(dir.concat(fileno), nb);
        penn.determinePredictive(predLexicon, opts.useSemantics);
        // step 3: additional annotation specific to TAG
        for (PennTree tree : penn.getTrees())
        {
            if (verbose)
            {
                LogInfo.logs(filename + " " + tree.getString());
            }
            tree.removeQuotationMarks(tree.getRoot());
            //tree.removeAllPunct();
            tree.removeSomeTraces();
            tree.insertAppositionTreatment(tree.getRoot(), percolTable);
            tree.removeFinalPunct();
            tree.insertQPtreatment(tree.getRoot(), percolTable);

            tree.insertExplicitRightBranching(tree.getRoot(), percolTable);
            tree.insertExtraVPNodes(tree.getRoot());// to allow for modifiers next to the VP head before arguments
        }
        return penn;
    }

    /**
     * Extracts the lexicon from the annotated Penn Treebank.
     * @param augmented: the annotated treebank
     * @param filename
     * @return the lexicon for the given part of the treebank
     */
    private List<StringTree> makeLexicon(PennCorpus augmented, String filename)
    {
        List treeList = augmented.getTrees();
        totalNumOfPropositionArgs += augmented.getTotalNumOfPropositionArgs();
        List<StringTree> allCorpus = new ArrayList<StringTree>();
        
        //for each sentence:
        for (int treeNo = 0; treeNo < treeList.size(); treeNo++)
        {//*/
            try
            {
                PennTree tree = (PennTree) treeList.get(treeNo);
                if (verbose)
                {
                    LogInfo.logs(tree.getString());                    
                }
                List<LexEntry> lexEntries = calculateLexEntries(tree);
                List<ElementaryStringTree> elementTrees =
                        convertToElementaryStringTrees(lexEntries, filename, treeNo, tree.getRandomGenerator());                
                allCorpus.addAll(reconstructAndBuildLexicon(filename, treeNo, tree, elementTrees));                                                
            }
            catch (Exception ex)
            {                
                if(verbose)
                {
                    LogInfo.error(ex);
                    //ex.printStackTrace();
                }
                listErrors.add("File: " + filename + " Tree: " + treeNo);                
            }
            catch (StackOverflowError e)
            {
                if(verbose)
                {
                    LogInfo.error(e.getClass().toString());
                }                
                listErrors.add("File: " + filename + " Tree: " + treeNo);
            }
        }
        for(StringTree elemTree : allCorpus)
        {
            extractedNumOfPropositionArgs += elemTree.getNumOfRoles();
        }
        return allCorpus;
    }

    /**
     * This method reconstructs the lexentries back into a tree, calculates the connection path 
     * and generates the predictive lexicon entries. It furthermore handles traces and fillers.
     * Too much responsibility!
     * 
     * @param lexentries
     * @param filename
     * @param treeno
     * @param tree
     * @param elementTrees
     * @return
     * @throws IOException
     */
    private List<StringTree> reconstructAndBuildLexicon(String filename, int treeno,
            PennTree tree, List<ElementaryStringTree> elementTrees) throws IOException
    {
        StopWatchSet.begin("reconstructAndBuildLexicon");
        CompositeStringTree sentenceTree = new CompositeStringTree(treeno, opts.useSemantics);
        //HashMap<String, ElementaryStringTree> sentenceWordLex = new HashMap<String, ElementaryStringTree>();
        ElementaryStringTree[] sentenceWordLex2 = new ElementaryStringTree[elementTrees.size() + 15];//allow for a max of 15 deleted words
        List<ElementaryStringTree> elementaryTrees = new ArrayList<ElementaryStringTree>();
        HashMap<String, ElementaryStringTree> fillers = new HashMap<String, ElementaryStringTree>();
        MultiValueMap<String, ElementaryStringTree> traces = new MultiValueMap<String, ElementaryStringTree>();
        ArrayList<StringTree> allCorpus = new ArrayList<StringTree>();
        for (ElementaryStringTree elementaryTree : elementTrees)
        {
            elementaryTree.simplifyCats();
            if (elementaryTree.isVerificTree())
            {
                sentenceTree.treeString += " " + elementaryTree.treeString;
            }
            else
            {
                sentenceTree.integrate(elementaryTree);
//                System.out.println("Word: " + elementaryTree.treeString + " head of tree : " + sentenceTree.getMainLeaf(sentenceTree.root));
//                System.out.println(sentenceTree.getStructure(sentenceTree.root, true));
            }
            //sentenceTree.putElementByOrigin(elementaryTree.getLowestOrigin(elementaryTree.getRoot(), elementaryTree.origin), elementaryTree);
            //if (verbose) System.out.println("$$"+sentenceTree.print());
            //sentenceWordLex.put(elementaryTree.getLowestOrigin(elementaryTree.getAnchor(), elementaryTree.origin), elementaryTree);
            for (Integer leaf : elementaryTree.getLeaves(elementaryTree.getRoot(), new ArrayList<Integer>()))
            {
                if (leaf >= sentenceWordLex2.length)
//                if (Integer.parseInt(leaf) >= sentenceWordLex2.length)
                {
                    LogInfo.error("Error while reconstructing and building lexicon");
                }
                sentenceWordLex2[leaf] = elementaryTree;
//                sentenceWordLex2[Integer.parseInt(leaf)] = elementaryTree;
            }
            //sentenceWordLex.put(elementaryTree.getLowestOrigin(elementaryTree.getPseudoAnchorNumber(), elementaryTree.origin), elementaryTree);
			/*if (elementaryTree.getLowestOrigin(elementaryTree.getPseudoAnchorNumber(), elementaryTree.origin).equals("0")){
            elementaryTree.setFirst();
            }*/
            elementaryTrees.add(elementaryTree);
            // associate and store traces and fillers
            String fillerPosition = elementaryTree.getFiller();
            if (!fillerPosition.equals(""))
            {
                fillers.put(fillerPosition, elementaryTree);
            }
            String traceposition = elementaryTree.getTrace();
            if (!traceposition.equals(""))
            {
                traces.put(traceposition, elementaryTree);
            }
        } // for each elementaryTree        
        sentenceTree.joinUnConnected();
        // massage lexicon entries
        elementaryTrees = traceAndFillerTreatment(elementaryTrees, sentenceTree, sentenceWordLex2, traces, fillers);
        //elementaryTrees = printNiceLexicon(elementaryTrees);
        allCorpus.add(sentenceTree);

        List<String> errors = errorTracker.leafCheck(sentenceTree, tree, filename);
        listErrors.addAll(errors);        
        attachTraces(elementaryTrees, sentenceTree, sentenceWordLex2);                
        
        if (errors.isEmpty())
        {            
            sentenceTree.removeUnaryNodes(sentenceTree.getRoot());                     
        }
        // calculate connection path	
        if (sentenceTree.treeString.startsWith("Unconnected"))
        {
            if(opts.outputEmptyExamples)
            {
                if(opts.examplesInSingleFile)
                {
                    PltagExample example = new PltagExample(String.format("Example:%s-sent_%s", filename , treeno));
                    example.setGoldStandard("NOT PARSED\nNOT PARSED\nNOT PARSED");
                    //example.setLexicon("\n"); example.setPredLexicon("\n");
                    printOutput(example.toString());
                }
                else
                {
                    printGoldStandard(String.format("Example_%s-sent_%s\n%s\n", filename, treeno, "NOT PARSED\nNOT PARSED\nNOT PARSED"));
                }                
            }
            return allCorpus;
        }
        ConnectionPathCalculator cpc = new ConnectionPathCalculator(sentenceTree, sentenceWordLex2, opts.useSemantics);              
        cpc.calculateConnectionPath(sentenceTree.root, Integer.MIN_VALUE, lexicon);                
        HashMap<Integer, Integer> newList = cpc.getNoOfSources();
        cpc.combinePredictedTreesFromSameOrigin();
        for (int i = 0; i < 6; i++)
        {
//            fullList.put(i + "", fullList.get(i + "").intValue() + newList.get(i + "").intValue());
            fullList.put(i, fullList.get(i) + newList.get(i));
        }
        // add predictive lexicon entries
        //if (verbose) allcorpus.addAll(cpc.getPredictedLexEntries());
        StringBuilder predLexStr = new StringBuilder();
        for (StringTree predTree : cpc.getPredictedLexEntries())
        {
            /*if (predTree.categories[Integer.parseInt(predTree.root)] != null){
            predTree.removeUnaryNodes(predTree.root);
            }
            else {
            predTree.removeUnaryNodes(predTree.coordanchor);
            }
            //	*/
            if (verbose)
            {
                LogInfo.logs("TagCorpus : reconstructAndBuildLex: " + predTree.print());
            }            
            predLexStr.append(predTree.print()).append("\n");                        
        }
        if(predLexStr.length() > 0)
            predLexStr.deleteCharAt(predLexStr.length() - 1); // remove last \n        
        // output the example (we assume there were no errors in creating the gold standard tree)
        if(errors.isEmpty())
        {
            if(opts.examplesInSingleFile) // do the necessary conversions
            {
                PltagExample example = new PltagExample(String.format("Example:%s-sent_%s", filename , treeno));
                example.setGoldStandard(sentenceTree.goldStandardToString());                
//                if(getNiceLexicon(elementaryTrees).contains("'"))
//                {
//                    String cmd = String.format("printf \"%%b\" '%s' | resources/extractLexicon.sh", getNiceLexicon(elementaryTrees).replaceAll("'", "'\\\\''"));
//                    String cmdArray[] = {"/bin/sh", "-c", cmd};  
//                    System.out.println(cmd+"\n");
//                    System.out.println(Utils.executeCmd(cmdArray));
//                    
//                }
                String cmd = String.format("printf \"%%b\" '%s' | resources/extractLexicon.sh", getNiceLexicon(elementaryTrees).replaceAll("'", "'\\\\''"));
                String cmdArray[] = {"/bin/sh", "-c", cmd};                
                example.setLexicon(Utils.executeCmd(cmdArray));
                cmd = String.format("printf \"%%b\" '%s' | resources/extractPredLexicon.sh", predLexStr.toString().replaceAll("'", "'\\\\''"));
                cmdArray[2] = cmd;
                example.setPredLexicon(Utils.executeCmd(cmdArray));                                            
                printOutput(example.toString());                                                         
            }
            else
            {
                if(opts.outputIndividualFiles)
                    printGoldStandard(sentenceTree.goldStandardToString());
                else
                    printGoldStandard(String.format("Example_%s-sent_%s\n%s\n", filename, treeno, sentenceTree.goldStandardToString()));
                printLex(predLexStr.toString());
                printLex(getNiceLexicon(elementaryTrees));                
            }
        }
        else if(opts.outputEmptyExamples)
        {
            if(opts.examplesInSingleFile)
            {
                PltagExample example = new PltagExample(String.format("Example:%s-sent_%s", filename , treeno));
                example.setGoldStandard("NOT PARSED\nNOT PARSED\nNOT PARSED");
                //example.setLexicon("\n"); example.setPredLexicon("\n");
                printOutput(example.toString());
            }
            else
            {
                printGoldStandard(String.format("Example_%s-sent_%s\n%s\n", filename, treeno, "NOT PARSED\nNOT PARSED\nNOT PARSED"));
            }
        }
        //*/
        StopWatchSet.end();
        return allCorpus;
    }

    /**
     * attaches the traces to the lexicon entries that they are under.
     * @param elementaryTrees
     * @param sentenceWordLex2 
     * @param sentenceTree 
     */
    private void attachTraces(List<ElementaryStringTree> elementaryTrees, CompositeStringTree sentenceTree, ElementaryStringTree[] sentenceWordLex)
    {
//        ArrayList<ElementaryStringTree> removelist = new ArrayList<ElementaryStringTree>();
        for (ElementaryStringTree eTree : elementaryTrees)
        {
            if (eTree.treeString.startsWith("*") || eTree.treeString.equals("0"))
            {
                Integer traceNode = 0;
                for (Integer etreenode : eTree.getNodes())
                {
                    if (//eTree.nodeTypes[etreenode]==TagNodeType.anchor && 
                            eTree.categories[etreenode].startsWith("*"))
                    {
                        traceNode = etreenode;
                    }
                    else if (//eTree.nodeTypes[etreenode]==TagNodeType.anchor && 
                            eTree.categories[etreenode].equals("0") //&& eTree.categories[Integer.parseInt(eTree.parent[etreenode])]
                            )
                    {
                        traceNode = etreenode;
                    }

                }
                Integer node = eTree.getRoot();
                if (!eTree.auxtree)
                {
                    node = sentenceTree.getParent(node);
                }
                if (node == null || !sentenceTree.originDown.containsKey(node))
                { // e.g. if unconnected tree
                    return;
                }
                Iterator<Integer> it = sentenceTree.originDown.getCollection(node).iterator();
                int max = it.next();
//                int max = Integer.parseInt(it.next());
                ElementaryStringTree tree = sentenceWordLex[max];
                while (it.hasNext() && !eTree.isAuxtree())
                {
                    int ws = it.next();
//                    int ws = Integer.parseInt(it.next());
                    int ts = sentenceTree.originDown.getCollection(traceNode).iterator().next();
//                    int ts = Integer.parseInt((String) sentenceTree.originDown.getCollection(traceNode + "").iterator().next());
                    if (ws > max && ws < ts && sentenceWordLex[ws] != eTree)
                    {
                        tree = sentenceWordLex[ws];
                        max = ws;
                    }
                }///*/
                if (tree.getTraceFather() != null)
                {
                    tree = sentenceWordLex[tree.getTraceFather().getMainLeafNode().getLeafNo()];
                }
                Integer mainleafOrigin = tree.getLowestOrigin(tree.getRoot(), tree.originDown);
                ArrayList<CompositeStringTree> rest = tree.integrate(eTree);
                if (!rest.isEmpty())
                {
//                    removelist.add(eTree);

                    if (traceNode > 0)
                    {
                        if (eTree.getLowestOrigin(eTree.getRoot(), eTree.originDown) > mainleafOrigin)
//                        if (Integer.parseInt(eTree.getLowestOrigin(eTree.getRoot(), eTree.originDown)) > Integer.parseInt(mainleafOrigin))
                        {
                            for (Integer nodeid : eTree.getNodes())
                            {
                                if (tree.originDown.containsKey(nodeid))
                                {
                                    tree.originDown.remove(nodeid);
                                    tree.originDown.put(nodeid, mainleafOrigin);
                                }
                                if (tree.originUp.containsKey(nodeid))
                                {
                                    tree.originUp.remove(nodeid);
                                    tree.originUp.put(nodeid, mainleafOrigin);
                                }
                            }
                        }
                        else
                        {
                            tree.originDown.remove(eTree.getRoot(), tree.getOriginNumber());
                        }
                        tree.nodeTypes[traceNode] = TagNodeType.terminal;
                        sentenceTree.nodeTypes[traceNode] = TagNodeType.terminal;
                        eTree.nodeTypes[traceNode] = TagNodeType.terminal;
                    }
                    // role trace fix: In cases where the father NP (integration point)
                    // of a trace tree has the role information as the daughter trace subtree,
                    // delete the role information from the internal NP node.
                    Integer traceParentId = tree.getParent(traceNode);
                    String traceRole = tree.roles[traceParentId];
                    if(traceRole != null)
                    {
                        Integer integrationPointId = tree.getParent(traceParentId);
                        if(integrationPointId != null)
                        {
                            String ipRole = tree.roles[integrationPointId];
                            if(ipRole != null && ipRole.equals(traceRole))
                                tree.roles[integrationPointId] = null;
                        }
                    }
                } // if
                else
                {
                    LogInfo.error("Attaching traces was unsuccesful");
                }                
            } // if traceTree
        } // for each elementaryTree
    }

    /**
     * 
     * @param elementaryTrees
     * @return
     * @throws IOException
     */
    private String getNiceLexicon(List<ElementaryStringTree> elementaryTrees)
            throws IOException
    {
        StringBuilder lexiconStr = new StringBuilder(); 
        // this kind of while loop becasue elementaryTrees array may be changed during loop 
        // by deleting elements.
        int i = 0;
        while (elementaryTrees.size() > i)
        {
            ElementaryStringTree etree = elementaryTrees.get(i);
            //printlex(etree.print());
            etree.makeLexiconEntry();
            // remove Unary Nodes!
            if (etree.categories[etree.root] != null)
//            if (etree.categories[Integer.parseInt(etree.root)] != null)
            {
                etree.removeUnaryNodes(etree.root);
            }
            else
            {
                etree.removeUnaryNodes(etree.coordanchor);
            }
            StringTree mcTag = etree.getMcTag();
            if (mcTag != null)
            {                
                lexiconStr.append(etree.print()).append("\t&\t").append(mcTag.print()).append("\n");             
                elementaryTrees.remove((ElementaryStringTree)mcTag);
            }
            else
            {                
                lexiconStr.append(etree.print()).append("\n");                            
            }
            i++;
        }
        if(lexiconStr.length() > 0)
            lexiconStr.deleteCharAt(lexiconStr.length() - 1);
        return lexiconStr.toString();
    }

    /**
     * 
     * @param elementaryTrees
     * @param sentenceTree 
     * @param sentenceWordLex2 
     * @param traces
     * @param fillers
     * @return
     */
    private List<ElementaryStringTree> traceAndFillerTreatment(List<ElementaryStringTree> elementaryTrees, 
            CompositeStringTree sentenceTree, ElementaryStringTree[] sentenceWordLex2, MultiValueMap<String, ElementaryStringTree> traces,
            HashMap<String, ElementaryStringTree> fillers)
    {
        for (String traceId : traces.keySet())
        {
            for(ElementaryStringTree traceTree : traces.getCollection(traceId))
            {
//                ElementaryStringTree traceTree = traces.get(traceId);
                ElementaryStringTree tracefather = null;
                if (traceTree.getTraceFather() != null)
                {
                    //String rootNodeId = tracetree.getTraceFather().getRoot().getNodeID();
                    //String leafOrigin = sentenceTree.getLowestOrigin(rootNodeId, sentenceTree.originDown);
                    Integer leafOrigin = traceTree.getTraceFather().getMainLeafNode().getLeafNo();
                    Integer leafOrigincoord = sentenceTree.getLowestOrigin(traceTree.getRoot(), sentenceTree.originUp);
                    tracefather = sentenceWordLex2[leafOrigin];
                    if (leafOrigincoord != null)
                    {
                        ElementaryStringTree tracefathercoord = sentenceWordLex2[leafOrigincoord];
                        if (!leafOrigin.equals(leafOrigincoord))
                        {
                            if (tracefather.getRoot() == traceTree.getTraceFather().getRoot().getNodeID())
    //                        if (tracefather.getRoot().equals(tracetree.getTraceFather().getRoot().getNodeID()))
                            {
                            }
                            else if (tracefathercoord.getRoot() == traceTree.getTraceFather().getRoot().getNodeID())
    //                        else if (tracefathercoord.getRoot().equals(tracetree.getTraceFather().getRoot().getNodeID()))
                            {
                                tracefather = tracefathercoord;
                            }
                            else
                            {
                                LogInfo.error("stuck for ideas for where to stick trace TagCorpus:traceAndFillerTreatment.");
                            }
                        }
                    }
                }
                ElementaryStringTree fillertree = fillers.get(traceId);
                if (fillertree == null)
                {
                    return elementaryTrees;
                }
                if (fillertree.isAuxtree() && traceTree.isAuxtree() && tracefather != null
                        && tracefather.categories[traceTree.getRoot()] != null
                        && tracefather.categories[fillertree.getRoot()] != null)
    //            if (fillertree.isAuxtree() && tracetree.isAuxtree() && tracefather != null
    //                    && tracefather.categories[Integer.parseInt(tracetree.getRoot())] != null
    //                    && tracefather.categories[Integer.parseInt(fillertree.getRoot())] != null)
                {
                    fillertree.setMCTAG(traceTree);
                    traceTree.setMCTAG(fillertree);
                }
                //else if (fillertree.getSubstNodes().contains(tracefather) && !tracetree.isAuxtree()){
                //MCTAG
                //}
                else if (tracefather != null)
                {
                    if (traceTree.isAuxtree())
                    {
                        Integer rootid = traceTree.getRoot();
                        Integer traceFatherOldRootDownIndex = tracefather.getLowestOrigin(rootid, tracefather.originDown);
                        Integer footid = traceTree.getFoot();
                        Integer traceOldFootUpIndex = traceTree.getLowestOrigin(footid, traceTree.originUp);

                        tracefather.integrate(traceTree);
                        while (tracefather.originDown.containsValue(rootid, traceFatherOldRootDownIndex))
                        {
                            tracefather.originDown.remove(rootid, traceFatherOldRootDownIndex);
                        }
                        while (tracefather.originUp.containsValue(footid, traceFatherOldRootDownIndex))
                        {
                            tracefather.originUp.remove(footid, traceOldFootUpIndex);
                        }
                    }
                    else
                    {
                        tracefather.integrate(traceTree);
                    }
                    elementaryTrees.remove(traceTree);
                }
                else if (traceTree.getNodeType(fillertree.getRoot()) == TagNodeType.subst)
                {
                    fillertree.integrate(traceTree);
                    //tracetree.integrate(fillertree);
                    elementaryTrees.remove(traceTree);
                    traceTree = fillertree;
                }
                else if (fillertree.isAuxtree())
                {
                    fillertree.integrate(traceTree);
                    elementaryTrees.remove(traceTree);
                    sentenceWordLex2[traceTree.getLowestOrigin(traceTree.root, traceTree.originDown)] = fillertree;
    //                sentenceWordLex2[Integer.parseInt(tracetree.getLowestOrigin(tracetree.root, tracetree.originDown))] = fillertree;
                }
            }            
        } //for each trace id
        return elementaryTrees;
    }

    private List<ElementaryStringTree> convertToElementaryStringTrees(
            List<LexEntry> lexentries, String fileno, int treeno, IdGenerator idgen)
    {
        StopWatchSet.begin("convertToElementaryStringTrees");
        List<ElementaryStringTree> elementTrees = new ArrayList<ElementaryStringTree>();
        String tagTreeControl = "";
        
        Map<String, Entry> treeProps = propBank.getBank().get("wsj/"+fileno).get(String.valueOf(treeno));
        Map<Integer, String> annotatedProps = new HashMap<Integer, String>();        
        for (LexEntry le : lexentries)
        {            
            if (le != null)
            {//&&  !le.isPartOfMultiLex()){
                if (le.isPartOfMultiLex())
                {
                    //int leindex = lexentries.indexOf(le.getMultiEntry().get(0));
                    if (le.getMultiEntry().size() > 1)
                    {
                        LogInfo.error("Warning: long multi-entry! TAGCorpus.java convertToElementaryStringTrees.");
                    }
                    // le = le.getMultiEntry().get(0);
                    // lexentries.remove(leindex);
                }
                ElementaryStringTree tagtree = new ElementaryStringTree(le, idgen, opts.useSemantics);
                annotatedProps.putAll(tagtree.annotatedRoles);
//				needed to generate error if tree built up incorrectly.
//                if(!tagtree.isAuxtree() && tagtree.getCategory(tagtree.root).equals("PP"))
//                    System.out.println(tagtree);
                tagTreeControl += tagtree.print();
                elementTrees.add(tagtree);
                if (verbose)
                {
                    try
                    {
                        printOutput(fileno + "-" + treeno + "\t" + tagtree.print());
                    }
                    catch (IOException e)
                    {                        
                        LogInfo.error(e);
                    }
                } // if
            } // if
        } // for
//        int treePropsSize = 0;
//        for(Entry prop : treeProps.values())
//        {
//            treePropsSize += prop.numOfArgs();
//        }
//        System.out.println("Propbank = " + treePropsSize + " Extracted = " + annotatedProps.size());
        StopWatchSet.end();
        return elementTrees;        
    }

    private List<LexEntry> calculateLexEntries(PennTree tree)
    {
        List<LexEntry> lexEntries = new ArrayList<LexEntry>();
        for (LeafNode leaf : tree.getLeaflist())
        {
            LexEntry le = tree.determineTree(leaf, opts.moreThanOneFoot, opts.justMultiWordLexemes);
            //LogInfo.logs(le);
            le.setFather(le.getRoot().getParent());
            lexEntries.add(le);
        }
        if (verbose)
        {
            try
            {
                printOutput(gettLexicalisedTagLex(lexEntries));
            }
            catch (IOException e)
            {                
                LogInfo.error("Calcuate lexical entries " + e);
            } //deprecated
        }
        return lexEntries;
    }

    /**
     * Prints the lexicon to standard out. 
     * 
     * @param lexentries
     */
    private String gettLexicalisedTagLex(List<LexEntry> lexentries)
    {
        //print lexical entries for tree 
        Iterator<LexEntry> i = lexentries.iterator();
        StringBuilder string = new StringBuilder();
        //HashMap<String, Integer> control = new HashMap<String, Integer>();
        while (i.hasNext())
        {
            LexEntry entry = i.next();
            string.append(entry.getExpression().concat("\t"));
            //print treetype: substitution, auxiliary, aux-left or aux-right
            string.append(entry.getRoot().printArgMod().concat("\t"));
            //print(entry.getSpine());//preliminary version for spine only, no treatment arg/mod info, 
            //no treatment of extended domain of locality, flat structure, tested! i.e. works fine!

            string.append(entry.getStruct(entry.getRoot()).concat("\n"));
            //get TAG spine (not as flat! add nodes for each arg and each modifier!?!)
        }
        return string.toString();
    }

    /**
     * Prints the lexicon to standard out. 
     * 
     * @param lexentries
     */
//    private static void printLexicalisedTAGLEX(ArrayList<LexEntry> lexentries)
//    {
//        //print lexical entries for tree 
//        Iterator<LexEntry> i = lexentries.iterator();
//        //HashMap<String, Integer> control = new HashMap<String, Integer>();
//        while (i.hasNext())
//        {
//            LexEntry entry = i.next();
//            System.out.print(entry.getExpression().concat("\t"));
//            //print treetype: substitution, auxiliary, aux-left or aux-right
//            System.out.print(entry.getRoot().printArgMod().concat("\t"));
//            //print(entry.getSpine());//preliminary version for spine only, no treatment arg/mod info, 
//            //no treatment of extended domain of locality, flat structure, tested! i.e. works fine!
//
//            System.out.println(entry.getStruct(entry.getRoot()));
//            //get TAG spine (not as flat! add nodes for each arg and each modifier!?!)
//        }
//    }

    /**
     * Used for printing to other media (e.g. the lexicon) but appending the string to the 
     * writer object. 
     *  
     * @param lexEntry
     * @throws IOException
     */
    private void print(Writer writer, String entry) throws IOException
    {
        writer.append(entry + "\n");
        writer.flush();
    }
    
    private void printLex(String entry) throws IOException
    {
        print(lexWriter, entry);
    }

    private void printOutput(String entry) throws IOException
    {
        print(outputWriter, entry);        
    }

    private void printGoldStandard(String entry) throws IOException
    {        
        print(goldStandardWriter, entry);
    }

    public List<StringTree> getLexicon()
    {
        return lexicon;
    }

    /**
     * If fatherHeadChildCat is specified, only works properly if the first category in the parameters
     * is the child category and the second category is the father category.
     * 
     * @param firstCat
     * @param secondCat
     * @param fatherHeadChildCat
     * @return whether categories are compatible
     */
    protected static boolean equivCat(String firstCat, String secondCat, String fatherHeadChildCat)
    {
        if (firstCat.equals(secondCat))
        {
            return true;
        }
        if (firstCat.matches("^N.*") && secondCat.matches("^N.*"))
        {
            return true;
        }
        if (firstCat.matches("^((JJ.*)|(VBN)|(VBG))") && secondCat.equals("ADJP"))
        {
            return true;
        }
        if (secondCat.matches("^((JJ.*)|(VBN)|(VBG))") && firstCat.equals("ADJP"))
        {
            return true;
        }
        if (firstCat.equals("WRB") && secondCat.equals("WHADVP"))
        {
            return true;
        }
        if (secondCat.matches("WRB") && firstCat.equals("WHADVP"))
        {
            return true;
        }
        if (firstCat.equals("RB") && secondCat.equals("ADVP"))
        {
            return true;
        }
        if (secondCat.equals("RB") && firstCat.equals("ADVP"))
        {
            return true;
        }
        if (fatherHeadChildCat != null && fatherHeadChildCat.equals(firstCat))
        {
            return true;
        }
        if (secondCat.equals("QP"))
        {
            return true;
        }
        if (secondCat.equals("WHNP") && firstCat.matches("NN.*") && fatherHeadChildCat.equals("WP$"))
        {
            return true;
        }
        return false;
    }
   
    public void testExecute()
    {        
        TagCorpus.verbose = opts.verbose;
        lexWriter = new DummyWriter();
        goldStandardWriter = new DummyWriter();
        outputWriter = new DummyWriter();
//        lexWriter = new OutputStreamWriter(System.out);
//        goldStandardWriter = new OutputStreamWriter(System.out);
//        outputWriter = new OutputStreamWriter(System.out);
        
        fullList = new HashMap<Integer, Integer>();
        lexicon = new ArrayList<StringTree>();
        percolTable = new PercolTable(opts.percolTableFilename);
        predictiveLexicon = new PredictiveLexicon(opts.predLexFilename);
        errorTracker = new ErrorTracker(verbose);
        for (int i = 0; i < 8; i++)
        {
            fullList.put(i, 0);
        } 
        execute();
    }
       
    class DummyWriter extends Writer
    {

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException
        {
            System.out.println(new String(cbuf, off, len));
        }

        @Override
        public void flush() throws IOException
        {
            
        }

        @Override
        public void close() throws IOException
        {
            
        }
        
    }
}
