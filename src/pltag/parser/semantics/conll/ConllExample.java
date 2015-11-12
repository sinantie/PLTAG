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
package pltag.parser.semantics.conll;

import edu.stanford.nlp.process.Morphology;
import fig.basic.Indexer;
import fig.basic.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.corpus.ElementaryStringTree;
import pltag.parser.Example;
import pltag.parser.FreqCounter;
import pltag.parser.Fringe;
import pltag.parser.Lexicon;
import pltag.parser.Options;
import pltag.parser.SuperTagger;
import pltag.parser.semantics.IncrementalSemanticsPerformance;
import pltag.parser.semantics.classifier.IncrementalSemanticsWidget;
import pltag.parser.semantics.conll.Predicate.Type;
import pltag.parser.semantics.conll.Vertex.Direction;
import pltag.parser.semantics.maltparser.MaltParserWrapper;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public class ConllExample extends Example
{

    private final String goldStandardConll;
    private final LinkedHashMap<Integer, Predicate> predicates;
    private final List<Integer> verbPredPositions;
    // map of noun predicate absolute positions in APRED cols, indexed by word position in sentence
    private final Map<Integer, Integer> nounPredPositions;
    private Map<Predicate, Proposition> propositions;
    public static final int APRED_START_COL = 14,
                            ID_COL = 0,
                            FORM_COL = 1,
                            LEMMA_COL = 2,
                            POS_COL = 4,
                            HEAD_COL = 8,
                            DEPREL_COL = 10,
                            FILLPRED_COL = 12,
                            PRED_COL = 13,
                            NO_OF_FIXED_COLS = 14;
    private final Set<String> auxilliaryVerbs = new HashSet(Arrays.asList(
            new String[] {"be", "can", "do", "have", "may", "might", "must", "shall", "will"}));
    private final Set<String> identificationHeuristicsRule2Set = new HashSet(Arrays.asList(
            new String[] {"IM", "COORD", "P", "PMOD", "SUB"}));
    private final Set<String> identificationHeuristicsRule2UpSet = new HashSet(Arrays.asList(
            new String[] {"OBJ", "ADV", "ROOT", "TMP", "SBJ", "OPRD"}));    
    private final Set<String> identificationHeuristicsRule4Set = new HashSet(Arrays.asList(
            new String[] {"ADV", "AMOD", "APPO", "BNF", "CONJ", "COORD", "DIR", "DTV", "EXT", "EXTR", "HMOD", "IOBJ",
                          "LGS", "LOC", "MNR", "NMOD", "OBJ", "OPRD", "POSTHON", "PRD", "PRN", "PRP", "PRT", "PUT",
                          "SBJ", "SUB", "SUFFIX", "TMP", "VOC"}));
    
    private Indexer<String> roleIndexer;
    
    public ConllExample(String name, String goldStandardConll, Options opts, Indexer<String> roleIndexer)
    {
        this(name, new String[] {"NOT PARSED"}, goldStandardConll, null, null, null, null, opts, roleIndexer);        
    }
    
    public ConllExample(String name, String[] input, String goldStandardConll, Lexicon lexicon,
            Map<ElementaryStringTree, ArrayList<Fringe>> shadowTreesMap, SuperTagger superTagger, FreqCounter freqCounter, 
            Options opts, Indexer<String> roleIndexer)
    {
        super(name, input, lexicon, shadowTreesMap, superTagger, freqCounter, opts);
        predicates = new LinkedHashMap<Integer, Predicate>();
        verbPredPositions = new ArrayList<Integer>();
        nounPredPositions = new HashMap();
        this.goldStandardConll = goldStandardConll;
        this.roleIndexer = roleIndexer;
        parse(goldStandardConll);
    }

    private void parse(String sentence)
    {
        List<List<String>> words = Utils.unpackConllSentenceToTokens(sentence);
        findPredicates(words);
        propositions = new HashMap<Predicate, Proposition>(predicates.size());
        findArguments(words, propositions);
    }

    private void findPredicates(List<List<String>> words)
    {
        int i = 0, aPredPos = 0;
        for (List<String> word : words)
        {
            if (word.get(FILLPRED_COL).equals("Y")) // verb relation [12:FILLPRED Y means there is a relation]
            {
                if (word.get(POS_COL).matches("VB[A-Z]*"))
                {
                    predicates.put(i, new Predicate(word.get(PRED_COL), word.get(POS_COL), Type.verb, i));
                    verbPredPositions.add(i);
                } else //if(word.get(4).matches("NN[A-Z]*"))
                {
                    predicates.put(i, new Predicate(word.get(PRED_COL), word.get(POS_COL), Type.noun, i));
                    nounPredPositions.put(i, aPredPos);
                }
                aPredPos++;
            } // if
            i++;
        } // for
    }

    private void findArguments(List<List<String>> words, Map<Predicate, Proposition> propositions)
    {
        int i = 0;
        for (Predicate pred : predicates.values())
        {
            int predCol = APRED_START_COL + i;
            List<Argument> args = new ArrayList();
            for (int wordPos = 0; wordPos < words.size(); wordPos++)
            {
                List<String> row = words.get(wordPos);
                if(row.size() > predCol)
                {
                    String aPred = row.get(predCol);
                    if (!aPred.equals("_"))
                    {
                        args.add(new Argument(wordPos, aPred, words.get(wordPos).get(FORM_COL), words.get(wordPos).get(POS_COL), 
                                words.get(wordPos).get(DEPREL_COL)));
                        roleIndexer.add(aPred);
                    }
                }                
            }
            propositions.put(pred, new Proposition(pred, args));
            i++;
        }
    }

    public Map<Integer, Predicate> getPredicates()
    {
        return predicates;
    }

    public List<Predicate> getVerbPredicates()
    {
        List<Predicate> list = new ArrayList<Predicate>();
        for (int pos : verbPredPositions)
        {
            list.add(predicates.get(pos));
        }
        return list;
    }

    public Proposition getProposition(Predicate pred)
    {
        return propositions.get(pred);
    }

    public List<Proposition> getVerbPropositions()
    {
        List<Proposition> list = new ArrayList<Proposition>();
        for (int pos : verbPredPositions)
        {
            list.add(propositions.get(predicates.get(pos)));
        }
        return list;
    }
    
    public Map<Predicate, Proposition> getVerbPropositionsMap()
    {
        Map<Predicate, Proposition> map = new HashMap<Predicate, Proposition>();
        for (Entry<Predicate, Proposition> e : propositions.entrySet())
        {
            if(e.getKey().type == Type.verb)
                map.put(e.getKey(), e.getValue());
        }
        return map;
    }
    
    public List<Argument> getVerbArgsAtTimeStamp(int t)
    {
        List<Argument> args = new ArrayList<Argument>();
        for (Proposition proposition : getVerbPropositions())
        {
            args.addAll(proposition.getArgsAtTimeStamp(t));
        }
        return args;
    }

    public String getGoldStandardVerbsOnlyConll()
    {
        List<List<String>> words = Utils.unpackConllSentenceToTokens(goldStandardConll);
        // delete noun predicates
        for (int pos : nounPredPositions.keySet())
        {
            words.get(pos).set(FILLPRED_COL, "_");
            words.get(pos).set(PRED_COL, "_");
        }
        List<List<String>> wordsOut = new ArrayList();
        int row = 0;
        for(List<String> word : words)
        {
            wordsOut.add(new ArrayList<String>());
            List<String> wordOut = wordsOut.get(row++);
            for(int col = 0; col < NO_OF_FIXED_COLS; col++)
            {
                wordOut.add(word.get(col));
            }
            for(int col = 0; col < predicates.size(); col++)
            {
                if(!nounPredPositions.containsValue(col))
                {
                    wordOut.add(word.get(NO_OF_FIXED_COLS + col));
                }
            }
        }
//        // and their columns as well        
//        for (int col : nounPredPositions.values())
//        {
//            for (int i = 0; i < words.size(); i++)
//            {
//                words.get(i).remove(NO_OF_FIXED_COLS + col);
//            }
//        }        
        return Utils.repackConllTokensToSentence(wordsOut);
    }

    public Map<Predicate, Proposition> getMajorityBaselineDirectArcs(Map<String, Pair<String, Integer>> majorityDepArgs)
    {
        int threshold = 10;
        List<List<String>> words = Utils.unpackConllSentenceToTokens(goldStandardConll);
        // Build a map of Heads to pairs of DepRels and their timestamp. Essentially we gather all the
        // arguments of each word in the sentence.
        MultiValueMap<String, Pair> headsPosDepRels = new MultiValueMap<String, Pair>();
        for(int wordPos = 0; wordPos < words.size(); wordPos++)
        {
            List<String> word = words.get(wordPos);
            headsPosDepRels.put(word.get(HEAD_COL), new Pair<Integer, String>(wordPos, word.get(DEPREL_COL+1))); // Pair: word_position - DepRel
        }
        // Guess predicates and arguments
        Map<Predicate, Proposition> props = new HashMap();
        int i = 0;
        for(Predicate goldPred : getVerbPredicates())
        {
            // cheating. We are copying the whole predicate. 
            // This is going to give us 100% Precision and Recall
            Predicate pred = new Predicate(goldPred);
            int predPos = verbPredPositions.get(i++);
            String predId = words.get(predPos).get(ID_COL); // id of the predicate.
            Collection<Pair> predArgs = headsPosDepRels.getCollection(predId);
            Proposition prop = new Proposition(pred);
            if(predArgs != null)
            {             
                for(Pair pair : predArgs)
                {
                    Pair<Integer, String> p = (Pair<Integer, String>)pair; 
                    Pair<String, Integer> argFreq = majorityDepArgs.get(p.getSecond()); // poll the depRel in the map of the top N depRels
                    if(argFreq != null)
                    {
                        int freqOfRole = argFreq.getSecond();
                        if(freqOfRole > threshold)
                        {
                            String role = argFreq.getFirst();
                            prop.addArgument(new Argument(p.getFirst(), role, "_", p.getSecond()));
                        }
                    }                    
                } // for each arg                
            } // if
            props.put(pred, prop);
        } // for each predicate
        
        return props;
    }
    
    public void getIncrementalMajorityBaselineDirectArcs(Map<String, Pair<String, Integer>> majorityDepArgs, 
            IncrementalSemanticsWidget gold, int numOfWords, IncrementalSemanticsPerformance sentenceIncrPerformance)
    {
        Map<Predicate, Proposition> predPropositions = getMajorityBaselineDirectArcs(majorityDepArgs);
        IncrementalSemanticsWidget pred = new IncrementalSemanticsWidget(predPropositions);
        for(int timestamp = 0; timestamp < numOfWords; timestamp++)
        {
            gold.convertPropositionsAt(timestamp);
            pred.convertPropositionsAt(timestamp);
            pred.setTimestamp(timestamp);
            pred.setFullSentence(timestamp == numOfWords - 1);
            sentenceIncrPerformance.add(pred, gold, getName());            
        }
    }
    
    public Map<Predicate, Proposition> getMajorityBaselineGoldIdentification(Map<String, Pair<String, Integer>> majorityDepArgs)
    {
        int threshold = 10;
        Map<Predicate, Proposition> props = new HashMap();
        for(Predicate goldPred : getVerbPredicates())
        {
            Predicate pred = new Predicate(goldPred);
            Proposition prop = new Proposition(pred);
            for(Argument goldArg : propositions.get(pred).getArguments())
            {
                Pair<String, Integer> argFreq = majorityDepArgs.get(goldArg.depRel); // poll the depRel in the map of the top N depRels
                if(argFreq != null)
                {
                    int freqOfRole = argFreq.getSecond();
                    if(freqOfRole > threshold)
                    {
                        String role = argFreq.getFirst();
                        prop.addArgument(new Argument(goldArg.getTimestamp(), role, goldArg.getForm(), goldArg.getPosTag()));
                    }
                }
            }
            props.put(pred, prop);
        }
        
        return props;
    }
    
    public void getIncrementalMajorityBaselineGoldIdentification(Map<String, Pair<String, Integer>> majorityDepArgs, 
            IncrementalSemanticsWidget gold, int numOfWords, IncrementalSemanticsPerformance sentenceIncrPerformance)
    {
        Map<Predicate, Proposition> predPropositions = getMajorityBaselineGoldIdentification(majorityDepArgs);
        IncrementalSemanticsWidget pred = new IncrementalSemanticsWidget(predPropositions);
        for(int timestamp = 0; timestamp < numOfWords; timestamp++)
        {
            gold.convertPropositionsAt(timestamp);
            pred.convertPropositionsAt(timestamp);
            pred.setTimestamp(timestamp);
            pred.setFullSentence(timestamp == numOfWords - 1);
            sentenceIncrPerformance.add(pred, gold, getName());            
        }
    }
    
    public Map<Predicate, Proposition> getMajorityBaselineAllA0s()
    {
        Map<Predicate, Proposition> props = new HashMap();
        int numOfWords = Utils.unpackConllSentence(goldStandardConll).size();
        for(Predicate goldPred : getVerbPredicates())
        {
            // cheating. We are copying the whole predicate. 
            // This is going to give us 100% Precision and Recall
            Predicate pred = new Predicate(goldPred);
            Proposition prop = new Proposition(pred);
            for(int i = 0; i < numOfWords; i++)
            {
                prop.addArgument(new Argument(i, "A0", "_", "_"));
            }
            props.put(pred, prop);
        }
        return props;
    }
    
    public void getIncrementalMajorityBaseline(Map<String, Pair<String, Integer>> majorityDepArgs, IncrementalSemanticsWidget gold,
            IncrementalSemanticsPerformance sentenceIncrPerformance)
    {
        List<List<String>> words = Utils.unpackConllSentenceToTokens(goldStandardConll);        
        for(int wordPos = 0; wordPos < words.size(); wordPos++)
        {
            gold.convertPropositionsAt(wordPos);
            IncrementalSemanticsWidget pred = getIncrementalMajorityBaseline(words.subList(0, wordPos+1), 
                    wordPos, wordPos == words.size() - 1, majorityDepArgs, false);            
            sentenceIncrPerformance.add(pred, gold, getName());
        }
    }
    
    public IncrementalSemanticsWidget getIncrementalMajorityBaseline(List<List<String>> words, 
            int timestamp, boolean fullSentence, Map<String, Pair<String, Integer>> majorityDepArgs, boolean maltParser)
    {
        int threshold = 10;
        IncrementalSemanticsWidget widget = new IncrementalSemanticsWidget(timestamp, fullSentence);
        DependencyGraph graph = new DependencyGraph();
        Set<Pair<Integer, Integer>> identifiedPreds = new HashSet(); // map of identified predicates (id -> timestamp)
        for(int wordPos = 0; wordPos < words.size(); wordPos++) // do one parse to identify all predicates, and build adjacency list tree
        {
            List<String> word = words.get(wordPos);     
            String depRel = word.get(DEPREL_COL);
            String posTag = word.get(POS_COL);
            String lemma = word.get(LEMMA_COL);
            int headId = Integer.valueOf(word.get(HEAD_COL));
            int wordId = Integer.valueOf(word.get(ID_COL));
            boolean isPredicate = (maltParser && posTag.startsWith("VB") && !isAuxilliary(lemma, posTag)) || // in case of Malt or Roark's parser
                    (word.get(FILLPRED_COL).equals("Y") && word.get(POS_COL).startsWith("VB"));
            if(isPredicate)
            {                
                widget.addPredicate(String.format("%s:%s", wordPos, lemma));    
                identifiedPreds.add(new Pair<Integer, Integer>(wordId, wordPos));//, wordPos);                
            }
            if(headId < words.size() && headId != -1) // make sure the arc is within the bounds of the incremental window of the tree we are observing
            {
                graph.addDependency(wordId, depRel, posTag, headId, Vertex.Direction.UP);
                graph.addDependency(headId, depRel, posTag, wordId, Vertex.Direction.DOWN);
            }
        } // for
        List<Pair<Integer, Argument>> candArguments = new ArrayList();
        for(int wordPos = 0; wordPos < words.size(); wordPos++) // do another parse for arguments
        {
            List<String> word = words.get(wordPos);
            String depRel = word.get(DEPREL_COL);
            String form = word.get(FORM_COL);    
            String pos = word.get(POS_COL);
            int wordId = Integer.valueOf(word.get(ID_COL));
            int headId = Integer.valueOf((word.get(HEAD_COL)));
            Pair<String, Integer> argFreq = majorityDepArgs.get(depRel); // poll the depRel in the map of the top N depRels
            if(argFreq != null)
            {
                int freqOfRole = argFreq.getSecond();
                if(freqOfRole > threshold)
                {
                    widget.addArgWord(String.format("%s:%s", wordPos, form)); // argument identification without role (possibly an incomplete dependency)
                    // for all identified predicates, try to see whether this argument is a good candidate
                    for(Pair<Integer, Integer> predicate : identifiedPreds)
                    {
                        // a predicate cannot be an argument of itself. Identify argument based on heuristics                        
                        if(wordPos != predicate.getFirst() && isCandidate(wordId, pos, form, depRel, headId, predicate.getFirst(), graph)) 
                        {
                            String role = argFreq.getFirst();
                            // create a list of candidates, in order to apply Rule 3 correctly (closest subject argument candidate to predicate)
//                            candArguments.add(new Pair<Integer, Argument>(predicate.getSecond(), new Argument(wordPos, role, form, pos, depRel)));    
                            widget.addArgRole(new Pair<Integer, Argument>(predicate.getSecond(), new Argument(wordPos, role, form, pos, depRel)));
                        }
                    }
                } // if
            } // if
        } // for
//        boolean foundSubjArg = false;
//        for(int i = candArguments.size() - 1; i >= 0; i--) // Apply Rule 3 from argument identification heuristics in Lang and Lapata, CL 2014
//        {
//            Pair<Integer, Argument> cand = candArguments.get(i);
//            String depRel = cand.getSecond().getDepRel();                        
//            if(depRel.equals("SBJ"))
//            {                
//                if(!foundSubjArg)
//                {
//                    foundSubjArg = true;
//                    widget.addArgRole(cand);
//                }                
//            }
//            else
//            {
//                widget.addArgRole(cand);
//            }
//        }
        return widget;
    }
    
    public void getIncrementalMaltParserBaseline(MaltParserWrapper maltParser, 
            Map<String, Pair<String, Integer>> majorityDepArgs, IncrementalSemanticsWidget gold,
            IncrementalSemanticsPerformance sentenceIncrPerformance)
    {       
        // we have to slice the input and keep columns: 1,2,3,5,6,7 only
        List<List<String>> words = Utils.cutConllTokens(goldStandardConll, new int[] {0,1,2,4,5,6});
        String[] maltInput = new String[words.size()];
        int i = 0;
        for(List<String> sentence : words)
        {
            maltInput[i++] = Utils.repackConllToken(sentence.toArray(new String[0]));
        }
        // get incremental output from malt parser.
        String[] parsed = maltParser.parse(maltInput, getName());        
        for(int wordPos = 0; wordPos < words.size(); wordPos++)
        {
            gold.convertPropositionsAt(wordPos);
            IncrementalSemanticsWidget pred = getIncrementalMajorityBaseline(Utils.unpackConllSentenceToTokens(parsed[wordPos]), 
                    wordPos, wordPos == words.size() - 1, majorityDepArgs, true);            
            sentenceIncrPerformance.add(pred, gold, getName());
        }
    }
    
    /**
     * 
     * Decide whether a word is a candidate argument of a given predicate according
     * to Lang and Lapata, CL 2014 argument identification heuristics.
     * @param argId
     * @param pos
     * @param depRel
     * @param predId
     * @return 
     */
    private boolean isCandidate(int argId, String pos, String form, String depRel, int headId, int predId, DependencyGraph graph)
    {
        if(isCoordConjunct(pos) || isPunctuation(depRel)) // Rules 1
            return false;        
        List<Vertex> argPredPath = graph.getPath(predId, argId); // the path is returned in reverse, i.e., from argument to predicate
        if(argPredPath.isEmpty()) // no path found
            return false;
        Vertex lastDepRel = argPredPath.get(0);
        if(identificationHeuristicsRule2Set.contains(lastDepRel.depRel) ||  // Rule 2
                (lastDepRel.direction == Direction.UP && identificationHeuristicsRule2UpSet.contains(lastDepRel.depRel + "^")) ||
                (lastDepRel.direction == Direction.DOWN && lastDepRel.depRel.equals("PRT")))
            return false;
//        if(lastDepRel.depRel.equals("SBJ")) // Rule 3
//        {
//            if(argPredPath.size() > 1) // Rule 4
//            {
//                for(int i = 1; i < argPredPath.size(); i++)
//                {
//                    Vertex depRelVertex = argPredPath.get(i);
//                    if(depRelVertex.direction != Direction.UP)
//                        return false;
//                }
//                return true;
//            }
//        }
        if(argPredPath.size() > 1) // Rule 4
        {
            for(int i = 1; i < argPredPath.size(); i++)
            {
                Vertex depRelVertex = argPredPath.get(i);
                if(identificationHeuristicsRule4Set.contains(depRelVertex.depRel))
                    return false;
            }
        }
        if(isAuxilliary(form, pos)) // Rule 5
            return false;
        if(headId == predId) // Rule 6
            return true;
        int verbCountAlongPath = 0;
        for(Vertex v : argPredPath) // Rule 7
        {
            if(v.posTag.startsWith("VB"))
                verbCountAlongPath++;
        }
        if(verbCountAlongPath > 0)
            return true;
        return false; // Rule 8
    }
    
    private boolean isCoordConjunct(String pos)
    {
        return pos.equals("CC");
    }
    
    private boolean isPunctuation(String depRel)
    {
        return depRel.equals("P");
    }
    
    private boolean isAuxilliary(String word, String pos)
    {
        String lemma = Morphology.lemmaStaticSynchronized(word, pos, false);        
        return auxilliaryVerbs.contains(lemma);
    }
    
    
    public String getGoldStandardConll()
    {
        return goldStandardConll;
    }

    public Map<Predicate, Proposition> getPropositions()
    {
        return propositions;
    }

    @Override
    public String toString()
    {
        return goldStandardConll;
    }    
   
}
