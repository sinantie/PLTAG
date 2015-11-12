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
package pltag.parser.semantics.classifier;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Pair;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.corpus.ElementaryStringTree;
import pltag.corpus.PltagExample;
import pltag.parser.Example;
import pltag.parser.Lexicon;
import pltag.parser.Options;
import pltag.parser.ParserModel;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.ConllExample;
import pltag.parser.semantics.conll.Predicate;
import pltag.parser.semantics.conll.Proposition;
import pltag.util.HistMap;
import pltag.util.Utils;

/**
 * Argument labeling classifier, based on features from Bjorkelund, Hafdell and Nugues (2009). 
 * Note we use only features that can be observed incrementally and relying on evidence obtained by PLTAG.
 * We train a logistic regression one-to-many classifier using liblinear.
 * Most features are categorical (lexical entries, POS tags, fringes), so we adopt 1-of-K encoding.
 * 
 * @author sinantie
 */
public class ArgumentClassifier
{
    
    private final Options opts;
    private final static int NUM_OF_BILEXICAL_FEATURES = 5, NUM_OF_SYNTACTIC_FEATURES = 1;
    private Model labellerModel, identifierModel;
    private final File labellerModelFile, identifierModelFile;
    private FeatureIndexers featureIndexers; // indexers holding 1-of-K representation of categorical values (lemmas, POS, fringes)    
    private final ParserModel parserModel;
    private List<FeatureVec> labellerTrain, identifierTrain;
    private Set<String> ambiguousLabels;
    
    public enum ModelType {identifier, labeller}
    
    public ArgumentClassifier(Options opts)
    {
        this(opts, null);
    }
    
    public ArgumentClassifier(Options opts, ParserModel parserModel)
    {
        this.opts = opts;        
        this.parserModel = parserModel;
        labellerModelFile = new File(opts.argumentLabellerModel);
        identifierModelFile = new File(opts.argumentIdentifierModel);
        featureIndexers = new FeatureIndexers();
        if(parserModel != null)
            parserModel.readExamples();
        labellerTrain = new ArrayList<FeatureVec>();
        identifierTrain = new ArrayList<FeatureVec>();
        ambiguousLabels = new HashSet<String>();
    }
    
    /**
     * Load models and feature indexers from file
     * @return 
     */
    public boolean loadModels()
    {
        try
        {
            loadFeatureIndexers();
            labellerModel = Model.load(labellerModelFile);
            identifierModel = Model.load(identifierModelFile);
        } 
        catch (IOException ex)
        {
            LogInfo.error("Error opening classifier models or feature indexers.");
            return false;
        }
        return true;
    }
    
    /**
     * Load feature indexers from serialized object file
     * @throws IOException 
     */
    public void loadFeatureIndexers() throws IOException
    {
        featureIndexers = (FeatureIndexers) IOUtils.readObjFileEasy(opts.featureIndexers);
    }
    
    /**
     * Save models and feature indexers to file
     * @return 
     */
    public boolean saveModels()
    {
        try
        {
            IOUtils.writeObjFileEasy(opts.featureIndexers, featureIndexers);
            labellerModel.save(labellerModelFile);
            identifierModel.save(identifierModelFile);
        } 
        catch (IOException ex)
        {
            LogInfo.error("Error saving classifier models or feature indexers.");
            return false;
        }
        return true;
    }
    
    public void train(Feature[][] instances, double[] labels, int numOfFeatures, ModelType modelType)
    {
        Problem problem = new Problem();
        problem.l = instances.length; // number of training examples
        problem.n = numOfFeatures; // number of features
        problem.x = instances; // feature nodes
        problem.y = labels; // target values
        
        if(modelType == ModelType.labeller)
        {
            SolverType solver = SolverType.L2R_LR; // [-s 0] L2-regularized logistic regression (primal)
            Parameter parameter = new Parameter(solver, opts.C, opts.eps);
            labellerModel = Linear.train(problem, parameter);
        }        
        else if(modelType == ModelType.identifier)
        {
            SolverType solver = SolverType.L2R_L2LOSS_SVC; // [-s 1] L2-regularized L2-loss support vector classification (dual)
            Parameter parameter = new Parameter(solver, opts.C, opts.eps);
            identifierModel = Linear.train(problem, parameter);
        }
    }
    
    public void train(List<FeatureVec> instances, ModelType modelType)
    {
        // convert feature vectors with internal representation, to
        // liblinear-native feature representation (e.g., convert booleans to -1,1, categorical to 1-of-k etc).
        double[] labels = new double[instances.size()];
        Feature[][] trainInstances = new Feature[instances.size()][instances.get(0).getNumOfFeatures()];
        for(int i = 0; i < instances.size(); i++)
        {
            FeatureVec vec = instances.get(i);
            trainInstances[i] = vec.getSparseRepresentation();
            labels[i] = vec.getLabel();
        }
        train(trainInstances, labels, instances.get(0).getDenseFeatureVecLength(), modelType);
    }
    
    public void trainModels()
    {
        train(identifierTrain, ModelType.identifier);
        train(labellerTrain, ModelType.labeller);
    }
    
    public double predict(Feature[] instance, ModelType modelType)
    {
        return Linear.predict(modelType == ModelType.labeller ? labellerModel : identifierModel, instance);
    }
    
    public double predictProbabilities(Feature[] instance, double[] probabilities, ModelType modelType)
    {
        return Linear.predictProbability(modelType == ModelType.labeller ? labellerModel : identifierModel, instance, probabilities);
    }
    
    public int predictLabelsSorted(Feature[] instance, int[] labels, ModelType modelType)
    {
        double[] probabilities = new double[labels.length];
        int bestLabel = (int) predictProbabilities(instance, probabilities, modelType);
        SortedSet<Pair<Integer, Double>> pairs = new TreeSet<Pair<Integer, Double>>(new Pair.ReverseSecondComparator<Integer, Double>());
        for(int i = 0; i < probabilities.length; i++)
        {
            pairs.add(new Pair<Integer, Double>(i, probabilities[i]));
        }
        int i = 0;
        for(Pair<Integer, Double> p : pairs)
        {
            labels[i++] = p.getFirst();
        }
        return bestLabel;
    }
    
    public double predict(FeatureVec instance, ModelType modelType)
    {
        return predict(instance.getSparseRepresentation(), modelType);
    }
    
    /**
     * Use argument labeler to rerank a list of candidate roles.
     * @param instance
     * @param candidateRoles
     * @return 
     */
    public String rerankRoleLabels(FeatureVec instance, String[] candidateRoles)
    {
        if(candidateRoles.length == 1) // don't need to predict or rerank anything
            return candidateRoles[0];
//        ambiguousLabels.add(Arrays.asList(candidateRoles).toString());
        int numOfLabels = featureIndexers.getRoleIndexer().size();
        int[] predLabels = new int[numOfLabels];
        int bestLabel = (int) predictLabelsSorted(instance.getSparseRepresentation(), predLabels, ModelType.labeller);
        String bestLabelStr = featureIndexers.getRole(bestLabel);
        // feeling lucky: if the top-1 predicted label is the same as the first in the 
        // list of candidate roles, don't need to rerank
        if(bestLabelStr.equals(candidateRoles[0])) 
            return candidateRoles[0];
        else if(bestLabelStr.startsWith("C-"))
            return featureIndexers.getRole(bestLabel);
        // convert candidate roles to labels (integers) for faster search
        int[] candidateLabels = new int[candidateRoles.length];
        for(int i = 0; i < candidateLabels.length; i++)
        {
            candidateLabels[i] = featureIndexers.getRoleIndex(candidateRoles[i]);
        }
        // search each element of the predicted labels in descending order, against
        // the least of candidate labels. When we find a match, return that value
        for(int predLabel : predLabels)
        {
            for(int candidateLabel : candidateLabels)
            {
                if(predLabel == candidateLabel)
                    return featureIndexers.getRole(predLabel);
            }
        }
        LogInfo.error("No matching role found!");
        return "_";
    }
    
    public boolean identifyArc(FeatureVec instance)
    {
        return predict(instance, ModelType.identifier) > 0;        
    }
    
    public void saveInstancesToFiles(String identifierOutFile, String labellerOutFile)
    {
        if(!identifierTrain.isEmpty())
            saveInstancesToFile(identifierTrain, identifierOutFile);
        if(!labellerTrain.isEmpty())
            saveInstancesToFile(labellerTrain, labellerOutFile);
    }
    
    public void saveInstancesToFile(List<FeatureVec> instances, String outFile)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(outFile);
            for(FeatureVec vec : instances)
            {
                fos.write((vec.toString() + "\n").getBytes());
            }
            fos.close();
        }
        catch(IOException ioe)
        {
            LogInfo.error("Error writing to file");
        }
    }
    
    public static String extractWeights(String inputVector)
    {
        StringBuilder out = new StringBuilder();
        HistMap<String> hist = new HistMap<String>();
        for(String line : Utils.readLines(inputVector))
        {
            String[] tokens = line.split(" ");
            if(tokens.length > 1)
                hist.add(tokens[0]);
        }   
        double total = hist.getTotalFrequency();        
        for(Pair<String, Integer> entry : hist.getEntriesSorted())
        {
            out.append(String.format(" -w%s %.2f", entry.getFirst(), 1.0-(double)entry.getSecond()/total));
        }        
        return out.toString();
    }
    
    public String printConfMatrix(String goldVectorFile, String predVectorFile, boolean binary)
    {
        try
        {
            loadFeatureIndexers();
        }        
        catch(IOException ioe)
        {
            LogInfo.error(ioe.getMessage());
        }
        int[] labelsIdTest = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,20,21,22,23,24,25,26,27,30,31,32,33,36,39,40};
        Map<Integer, Integer> inverseMap = new HashMap();
        for(int i = 0; i < labelsIdTest.length; i++)
            inverseMap.put(labelsIdTest[i], i);
        String[] labels;
        int[][] counts;
        if(binary)
        {
            labels = new String[] {"-1", "1"};
            counts = new int[2][2];    // Confusion matrix on role labels              
        }
        else
        {            
            int size = featureIndexers.getRoleIndexer().size();
//            int size = labelsIdTest.length;
            labels = new String[size];
            for(String label : featureIndexers.getRoleIndexer())
                labels[featureIndexers.getRoleIndex(label)] = label;
//            for(int i = 0; i < labelsIdTest.length; i++)
//                labels[i] = featureIndexers.getRole(labelsIdTest[i]);
            counts = new int[size][size];
        }        
        
        StringBuilder out = new StringBuilder();
        String[] goldLines = Utils.readLines(goldVectorFile);
        String[] predLines = Utils.readLines(predVectorFile);
        for(int i  = 0; i < goldLines.length; i++)
        {
            int goldLabel, predLabel;
            String goldLine = goldLines[i];
            String[] tokens = goldLine.split(" ");
            if(tokens.length > 1)
            {
                goldLabel = Integer.valueOf(tokens[0]);
                predLabel = Integer.valueOf(predLines[i]);
                if(binary)
                    counts[goldLabel == -1 ? 0 : 1][predLabel == -1 ? 0 : 1]++;
                else
                    counts[goldLabel][predLabel]++;
//                    counts[inverseMap.get(goldLabel)][inverseMap.get(predLabel)]++;
            }
        }
        System.out.println(confMatrix(counts, labels));
        return out.toString();
    }
    
    public static String confMatrix(int[][] counts, String[] labels)
    {
        int size = labels.length;
        TreeSet<pltag.util.Pair<String>> ts = new TreeSet<pltag.util.Pair<String>>();
        for(int r = 0; r < size; r++)
        {
            ts.add(new pltag.util.Pair(r, labels[r], Utils.Compare.LABEL));
        }
        String[][] table = new String[size + 1][size + 1];
        // write the headers
        table[0][0] = "";
        for(pltag.util.Pair<String> pair : ts)
        {
            table[0][(int)pair.value + 1] = table[(int)pair.value + 1][0] = pair.label;
        }
//        table[size + 1][0] = table[0][size + 1] = "(NONE)";
        // write the data
        int tt = 0;
        for(pltag.util.Pair pairRow : ts)
        {
            tt = (int) pairRow.value;
            int pt = 0;
            for(pltag.util.Pair pairColumn : ts)
            {
                pt = (int) pairColumn.value;
//                table[tt+1][pt+1] = ((pt == tt) ? correctCounts[tt] + "/" : "") + Utils.fmt(counts[tt][pt]);
                table[tt+1][pt+1] = Utils.fmt(counts[tt][pt]);

            }
//            table[tt+1][size + 1] = Utils.fmt(counts[tt][size]);
        }
//        int pt = 0;
//        for(pltag.util.Pair pairColumn : ts)
//        {
//            pt = (int) pairColumn.value;
//            table[size + 1][pt + 1] = Utils.fmt(counts[size][pt]);
//        }
//        table[size + 1][size + 1] = "";
        return Utils.formatTable(table, Utils.Justify.CENTRE);
    }
    
    
    public void extractFeatures(Map<Predicate, Proposition> predProps, Map<Predicate, Proposition> goldProps, Lexicon lexicon, boolean train)
    {
        MultiValueMap<Predicate, Argument> identifiedArcs = new MultiValueMap<Predicate, Argument>();
        // Get things in common (Arguments)
        Iterator<Map.Entry<Predicate, Proposition>> iterProps = goldProps.entrySet().iterator();
        while(iterProps.hasNext())
        {
            Map.Entry<Predicate, Proposition> goldEntry = iterProps.next();
            Proposition predProp = predProps.get(goldEntry.getKey());
            if(predProp == null) // if we didn't find the correct predicate
            {                                
                Predicate predPredIgnoreCase = Predicate.getPredicateIgnoreSense(goldEntry.getKey(), predProps.keySet());
                if(predPredIgnoreCase != null) // found correct predicate with wrong sense
                {                    
                    predProp = predProps.get(predPredIgnoreCase);
                }
            }
            if(predProp != null) // continue with identifying correct arguments
            {
                List<Argument> goldArgs = goldEntry.getValue().getArguments();
                List<Argument> predArgs = predProp.getArguments();
                
                Iterator<Argument> iterGoldArgs = goldArgs.iterator();
                while(iterGoldArgs.hasNext())
                {
                    Argument goldArg = iterGoldArgs.next();
                    Iterator<Argument> iterPredArgs = predArgs.iterator();
                    while(iterPredArgs.hasNext()) // True Positive (gold role label, +1 identifier label)
                    {                                      
                        Argument predArg = iterPredArgs.next();
                        if(predArg.getTimestamp() == goldArg.getTimestamp())
                        {
                            addFeatureVecs(predProp.getPredicate(), predArg, goldArg.getRole(), true, identifiedArcs, train);                        
                            iterGoldArgs.remove();
                            iterPredArgs.remove();
                            break;
                        }                        
                    }
                } //  while
                if(predArgs.isEmpty())
                    predProps.remove(goldEntry.getKey());
                if(goldArgs.isEmpty())
                    iterProps.remove();
            } // if
        } // while
        // Mark the differences between the two maps (entries left)
        for(Proposition predProp : predProps.values())
        {
            for(Argument predArg : predProp.getArguments()) // False Positive
            {
                addFeatureVecs(predProp.getPredicate(), predArg, null, false, identifiedArcs, train);
            }
        }
        for(Proposition goldProp : goldProps.values())
        {            
            for(Argument goldArg : goldProp.getArguments()) // False Negatives (inaccurate)
            {
                Predicate goldPred = goldProp.getPredicate();
                // try to find a predicate we've already correctly predicted earlier
                Predicate predPred = Predicate.getPredicateIgnoreSense(goldPred, identifiedArcs.keySet());
                if(predPred != null)
                {
                    // try to find argument in list of identified arcs (attached to a wrong predicate)
                    boolean foundInIdentified = false;
                    for(Object predObj : identifiedArcs.values())
                    {
                        Argument predArg = (Argument)predObj;
                        if(predArg.getTimestamp() == goldArg.getTimestamp())
                        {
                            addFeatureVecs(predPred, predArg, goldArg.getRole(), true, identifiedArcs, train);
                            foundInIdentified = true;
                            break;
                        } // if
                    } // for
                    if(!foundInIdentified) // reconstruct argument (fairly inaccurate) from lexicon and info from rest predictions
                    {
                        Argument reconstructedArgument = reconstructArgument(lexicon, predPred, goldArg, identifiedArcs);
                        if(reconstructedArgument != null)
                            addFeatureVecs(predPred, reconstructedArgument, goldArg.getRole(), true, identifiedArcs, train);
                    }
                } // if
            } // for
        } // for
    }
     
    private Argument reconstructArgument(Lexicon lexicon, Predicate predPred, Argument goldArg, MultiValueMap<Predicate, Argument> identifiedArcs)
    {
        String goldWord = Example.wordRemoveDigits(goldArg.getForm());
        String goldPosTag = goldArg.getPosTag();
        String simplifiedPosTag = simplifyPosTag(goldWord, goldPosTag, goldArg.getTimestamp());
        String posWord = simplifiedPosTag + " " + goldWord;
        List<ElementaryStringTree> entries = (List)lexicon.getEntries(posWord, posWord.toLowerCase(), simplifiedPosTag, false, goldArg.getTimestamp());
        if(entries.size() > 0)
        {
            String elemTree = entries.get(0).toStringUnlex(false); // always choose the first. In case of ambiguity then we've picked the wrong tree!
            // try to find an argument with the same POS tag as the word in the gold standard
            boolean foundPosTag = false;
            Argument predArg = null;
            Collection<Argument> candArgs = identifiedArcs.getCollection(predPred);
            for(Argument candArg : candArgs)
            {                
                if(Utils.getCatInventory(candArg.getPosTag(), true).equals(simplifiedPosTag)) // simplify POS tag
                {
                    predArg = candArg;
                    foundPosTag = true;
                    break;
                }
            }
            if(!foundPosTag) // couldn't find an argument with same POS tag, so choose pick the first one (inaccurate)
                predArg = candArgs.iterator().next();
            
            return new Argument(goldArg.getTimestamp(), goldArg.getRole(), goldWord, goldPosTag, elemTree, 
                    predArg.getIntegrationPoint(), predArg.getPrefixFringeAlphaSet(), predArg.getOperationPath(), null);
        }
//        else if(!opts.silentMode)
//        {
//            LogInfo.error("Could not find lexicon entry while reconstructing argument, during feature extraction (gold POS was " + goldPosTag + " and got simplified to " + simplifiedPosTag + ")");
//        }
        return null;
    }
    
    private String simplifyPosTag(String word, String pos, int timestamp)
    {
        word = word.toLowerCase();
        if(word.matches("ha(s|d|ve)|was|were"))
            return "AUX";
        if(timestamp == 0 && pos.equals("CC"))
                return "CCSIM";
        if (pos.matches("JJ(R|S)"))
        {
            return "JJ";
        }
        else if (pos.equals("PRP$"))
        {
            return "DT";
        }
        else if (pos.matches("RB(R|S)"))
        {
            return "RB";
        }
        else if (pos.equals("AUXG"))
        {
            return "AUX";
        }
        if (pos.equals("AUX") && word.startsWith("need"))
        {
            return "VB";
        }
        return Utils.getCatInventory(pos, true);
    }
    
    private void addFeatureVecs(Predicate predicate, Argument argument, String roleLabel, boolean identifyLabel, MultiValueMap<Predicate, Argument> identifiedArcs, boolean train)
    {
        identifiedArcs.put(predicate, argument);
        FeatureVec vec = newFeatureVec();
        extractBilexicalFeatures(predicate, argument, train, vec);
        extractSyntacticFeatures(predicate, argument, train, vec);
        vec.setLabel(identifyLabel ? 1 : -1);
        identifierTrain.add(vec);
        if(roleLabel != null)
        {
            FeatureVec labellerVec = new FeatureVec(vec);
            labellerVec.setLabel(featureIndexers.getRoleIndex(roleLabel));
            labellerTrain.add(labellerVec);
        }
    }
    
    public List<FeatureVec> extractFeatures(List<Example> examples, boolean train)
    {
        List<FeatureVec> instances = new ArrayList<FeatureVec>();
        for(Example example : examples)
        {
            ConllExample ex = (ConllExample)example;
            try
            {
                for(Proposition prop : ex.getVerbPropositions())
                {
                    for(Argument arg : prop.getArguments())
                    {
                        instances.add(extractBilexicalFeaturesLabeller(prop.getPredicate(), arg, train));
                    }
                }
            } catch(Exception e)
            {
                LogInfo.error(e + "\n" + ex);
            } 
        }
        return instances;
    }
    
    /**
     * Extract features using only bilexical information, between predicate and argument (and their relative position).
     * We can cheaply train this model relying on information from the CoNLL training file, and
     * avoid the much more costly solution of parsing sections 02-21 of WSJ.
     * @param conllFile the filename of the file that contains the training set of WSJ sections 02-21 in CoNLL 2009 format.
     * @param train true if the input CoNLL file is used for training a model
     * @return 
     */
    public List<FeatureVec> extractFeaturesConllOnly(String conllFile, boolean train)
    {
        List<FeatureVec> instances = new ArrayList<FeatureVec>();
        List<PltagExample> conllExamples = Utils.readConllExamples(conllFile);
        for(PltagExample ex : conllExamples)
        {
            try
            {
                ConllExample conllEx = new ConllExample(conllFile, ex.getGoldStandard(), opts, featureIndexers.getRoleIndexer());
                for(Proposition prop : conllEx.getVerbPropositions())
                {
                    for(Argument arg : prop.getArguments())
                    {
                        instances.add(extractBilexicalFeaturesLabeller(prop.getPredicate(), arg, train));
                    }
                }
            } catch(Exception e)
            {
                LogInfo.error(e + "\n" + ex);
            }
        }
        return instances;
    }
    
    public void extractBilexicalFeatures(Predicate pred, Argument arg, boolean train, FeatureVec vec)
    {
        if(vec == null)
            return;
        // predPOS
        int featureType = FeatureIndexers.FEAT_PRED_POS;
        int id = featureIndexers.getIndexOfFeature(featureType, pred.getPosTag(), train);
        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
        
        // predLemma
        featureType = FeatureIndexers.FEAT_PRED_LEMMA;
        id = featureIndexers.getIndexOfFeature(featureType, pred.getLemma(), train);
        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
        
        // sense
//        featureType = FeatureIndexers.FEAT_SENSE;
//        id = featureIndexers.getIndexOfFeature(featureType, pred.getSense(), train);
//        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
        
        // argWord
        featureType = FeatureIndexers.FEAT_ARG_WORD;
        id = featureIndexers.getIndexOfFeature(featureType, arg.getForm(), train);
        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
        
        // argPOS
        featureType = FeatureIndexers.FEAT_ARG_POS;
        id = featureIndexers.getIndexOfFeature(featureType, arg.getPosTag(), train);
        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
        
        // position
        featureType = FeatureIndexers.FEAT_POSITION;
        boolean argBeforePred = arg.getTimestamp() < pred.getTimestamp();
        vec.addFeature(new BinaryFeature(argBeforePred), featureType);
    }
    
    public void extractSyntacticFeatures(Predicate pred, Argument arg, boolean train, FeatureVec vec)
    {
        if(vec == null)
            return;
        // pred elementary tree
//        int featureType = FeatureIndexers.FEAT_PRED_ETREE;
//        int id = featureIndexers.getIndexOfFeature(featureType, pred.getElemTree(), train);
//        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
//        
//        // arg elementary tree
//        featureType = FeatureIndexers.FEAT_ARG_ETREE;
//        id = featureIndexers.getIndexOfFeature(featureType, arg.getElemTree(), train);
//        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
//        
//        // integration point
//        featureType = FeatureIndexers.FEAT_INTEG_POINT;
//        id = featureIndexers.getIndexOfFeature(featureType, arg.getIntegrationPoint(), train);
//        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);
//        
//        // prefix fringe (alphabetically ordered set of current fringe)
//        featureType = FeatureIndexers.FEAT_FRINGE;
//        id = featureIndexers.getIndexOfFeature(featureType, arg.getPrefixFringeAlphaSet().toString(), train);
//        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);        
        
        // prefix fringe (alphabetically ordered set of current fringe)
        int featureType = FeatureIndexers.FEAT_OPERATION_PATH;
        int id = featureIndexers.getIndexOfFeature(featureType, arg.getOperationPath(), train);
        vec.addFeature(new CategoricalFeature(featureIndexers, featureType, id), featureType);        
    }
   
    public FeatureVec extractBilexicalFeaturesLabeller(Predicate pred, Argument arg, boolean train)
    {
        FeatureVec vec = new FeatureVec(NUM_OF_BILEXICAL_FEATURES);
        extractBilexicalFeatures(pred, arg, train, vec);
        
        // label
        String label = arg.getRole();
        if(label != null) // usually empty when predicting
            vec.setLabel(featureIndexers.getRoleIndex(arg.getRole()));
        
        return vec;
    }
       
    public FeatureVec newFeatureVec()
    {
        return new FeatureVec(NUM_OF_BILEXICAL_FEATURES + NUM_OF_SYNTACTIC_FEATURES);
    }
    
    public FeatureVec newBilexicalFeatureVec()
    {
        return new FeatureVec(NUM_OF_BILEXICAL_FEATURES);
    }

    public String getAmbiguousLabels()
    {
        StringBuilder str  = new StringBuilder();
        for(String s : ambiguousLabels)
            str.append(s).append("\n");
        return str.toString();
    }
    
    public static void main(String[] args)
    {
        Options opts = new Options();
        opts.argumentLabellerModel = "data/classifiers/bilexical_no_sense.labeller.tuned.model";
        opts.argumentIdentifierModel = "data/classifiers/bilexical_no_sense.identifier.tuned.model";
        opts.featureIndexers = "data/classifiers/bilexical_no_sense_syntactic_opPath.indexers";
        String conllTrainFile = "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-English-train-edited.txt";
        String conllTestFile = "../../../conll/2009/2009_conll_p2/data/CoNLL2009-ST-English/CoNLL2009-ST-evaluation-English.txt";
        String instancesTestFile = "data/classifiers/bilexical_no_sense_tuned.test";
        String examplesTrainFile = "data/pltag/single_wsj_0221_withSemantics_edited";
        String examplesTrainVectorFile = "data/classifiers/bilexical_no_sense_opPath.labeller.train.vectors";
//        String goldTestVectorFile = "data/classifiers/bilexical_no_sense.identifier.test.vectors";
//        String goldTestVectorFile = "data/classifiers/bilexical_no_sense_opPath.identifier.test.vectors";
        String goldTestVectorFile = "data/classifiers/bilexical_no_sense_opPath.labeller.test.vectors";
        String predTestVectorFile = "data/classifiers/out";
        String examplesTestFile = "data/pltag/single_wsj_23_withSemantics";
        opts.inputPaths = Arrays.asList(new String[] {examplesTrainFile, conllTrainFile});
        opts.examplesInSingleFile = true;
        opts.useSemantics = true;
        opts.train = false;
//        ParserModel parserModel = new ParserModel(opts);
//        ArgumentClassifier ac = new ArgumentClassifier(opts, parserModel);
        ArgumentClassifier ac = new ArgumentClassifier(opts, null);
//        ac.train(ac.extractFeatures(parserModel.getExamples(), true), ModelType.labeller);

//        ArgumentClassifier ac = new ArgumentClassifier(opts);
//        ac.train(ac.extractFeaturesConllOnly(conllTrainFile, true), ModelType.labeller);
//
//        ac.saveModels();
//        //ac.loadModel();
//        ac.saveInstancesToFile(ac.extractFeaturesConllOnly(conllTestFile, false), instancesTestFile);
//        System.out.println(extractWeights(examplesTrainVectorFile));
        System.out.println(ac.printConfMatrix(goldTestVectorFile, predTestVectorFile, false));
    }
}
