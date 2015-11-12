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
package pltag.parser.semantics;

import fig.basic.Pair;
import java.io.Serializable;
import java.util.ArrayList;
import pltag.parser.semantics.conll.ConllExample;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.parser.Node;
import pltag.parser.ParserModel;
import pltag.parser.StringTreeAnalysis;
import static pltag.parser.semantics.DepTreeState.isAuxiliary;
import static pltag.parser.semantics.DepTreeState.isPostHonorific;
import pltag.parser.semantics.classifier.FeatureVec;
import pltag.parser.semantics.classifier.IncrementalSemanticsWidget;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.Predicate;
import pltag.parser.semantics.conll.Proposition;
import pltag.parser.semantics.discriminative.DiscriminativeFeatureIndexers;
import pltag.util.Utils;


/**
 *
 * @author konstas
 */
public class Dependencies implements Serializable
{

    static final long serialVersionUID = -1L;
    private final MultiValueMap<DepNode, DependencyArc> arcs;
    private final Map<String, DepNode> hangingRelations;
    private final Map<DepNode, DependencyArc> arcsWithShadowArgs;
    private final LinkedList<DepNode> infinitiveMarkers;
    private final Map<String, DepNode> auxVerbs;
    private final LinkedList<Node> npHeads;
    private final LinkedList<DependencyArc> hangingModifiers;
    private final LinkedList<Pair<DepNode, DepNode>> hangingPreps;
    private Pair<String, String> currentCoord;
    private DepNode currentRelative, currentAuxVerb;
    
    public Dependencies()
    {
//        arcs = new LinkedHashMap<SemNode, DependencyArc>();
        arcs = new MultiValueMap<DepNode, DependencyArc>();
        hangingRelations = new HashMap<String, DepNode>();
        arcsWithShadowArgs = new HashMap<DepNode, DependencyArc>();
        infinitiveMarkers = new LinkedList<DepNode>();
        auxVerbs = new HashMap<String, DepNode>();
        npHeads = new LinkedList<Node>();
        hangingModifiers = new LinkedList<DependencyArc>();
        hangingPreps = new LinkedList<>();
    }
    
    public Dependencies(Dependencies d)
    {
//        arcs = new LinkedHashMap(d.arcs);     
        Map<DependencyArc, DependencyArc> oldNewMap = new HashMap();
        arcs = cloneArcs(d.arcs, oldNewMap);
        hangingRelations = cloneStringDepNodeMap(d.hangingRelations);
        arcsWithShadowArgs = cloneArcWithShadowArgsMap(d.arcsWithShadowArgs, oldNewMap);
        infinitiveMarkers = cloneDepNodeList(d.infinitiveMarkers);
        auxVerbs = cloneStringDepNodeMap(d.auxVerbs);
        npHeads = cloneNpHeadsList(d.npHeads);
        hangingModifiers = cloneHangingModifiersList(d.hangingModifiers, oldNewMap);
        hangingPreps = cloneDepNodePairList(d.hangingPreps);
        currentCoord = d.currentCoord == null ? null : new Pair(d.currentCoord.getFirst(), d.currentCoord.getSecond());
        currentRelative = d.currentRelative == null ? null : new DepNode(d.currentRelative);
        currentAuxVerb = d.currentAuxVerb == null ? null : new DepNode(d.currentAuxVerb);
    }
    
    private LinkedList<Node> cloneNpHeadsList(LinkedList<Node> list)
    {
        LinkedList<Node> out = new LinkedList<Node>();
        for(Node n : list)
            out.add(n.copy());
        return out;
    }
    
    private LinkedList<DepNode> cloneDepNodeList(LinkedList<DepNode> list)
    {
        LinkedList<DepNode> out = new LinkedList<DepNode>();
        for(DepNode n : list)
            out.add(new DepNode(n));
        return out;
    }
    
    private LinkedList<Pair<DepNode, DepNode>> cloneDepNodePairList(LinkedList<Pair<DepNode, DepNode>> list)
    {
        LinkedList<Pair<DepNode, DepNode>> out = new LinkedList<>();
        for(Pair<DepNode, DepNode> p : list)
            out.add(new Pair(new DepNode(p.getFirst()), new DepNode(p.getSecond())));
        return out;
    }
    
    private LinkedList<DependencyArc> cloneHangingModifiersList(LinkedList<DependencyArc> list, Map<DependencyArc, DependencyArc> oldNewMap)
    {
        LinkedList<DependencyArc> out = new LinkedList<DependencyArc>();
        for(DependencyArc oldArc : list)
        {
            DependencyArc newArc = oldNewMap.get(oldArc);
            if(newArc != null) // classifiers might have removed the oldArc, so the list of hanging modifiers could contain stale data
                out.add(oldNewMap.get(oldArc));
        }
        return out;
    }
    
    private Map<String, DepNode> cloneStringDepNodeMap(Map<String, DepNode> map)
    {
        Map<String, DepNode> out = new HashMap<String, DepNode>();
        for(Entry<String, DepNode> e : map.entrySet())
        {
            out.put(e.getKey(), new DepNode(e.getValue()));
        }
        return out;
    }
        
    private Map<DepNode, DependencyArc> cloneArcWithShadowArgsMap(Map<DepNode, DependencyArc> map, Map<DependencyArc, DependencyArc> oldNewMap)
    {
        Map<DepNode, DependencyArc> out = new HashMap<DepNode, DependencyArc>();
        for(Entry<DepNode, DependencyArc> e : map.entrySet())
        {
            out.put(new DepNode(e.getKey()), oldNewMap.get(e.getValue()));
        }
        return out;
    }
    
    private MultiValueMap<DepNode, DependencyArc> cloneArcs(MultiValueMap<DepNode, DependencyArc> map, Map<DependencyArc, DependencyArc> oldNewMap)
    {
        MultiValueMap<DepNode, DependencyArc> out = new MultiValueMap<DepNode, DependencyArc>();
        for(Object o : map.values())
        {
            DependencyArc oldArc = (DependencyArc)o;
            DependencyArc newArc = new DependencyArc(oldArc);
            oldNewMap.put(oldArc, newArc);
            out.put(new DepNode(oldArc.getIntegrationPoint()), newArc);
        }
        return out;
    }
    
    private Map<DepNode, DepNode> clonePrepsMap(Map<DepNode, DepNode> map)
    {
        Map<DepNode, DepNode> out = new HashMap<DepNode, DepNode>();
        for(Entry<DepNode, DepNode> e : map.entrySet())
        {
            out.put(e.getKey(), new DepNode(e.getValue()));
        }
        return out;
    }
    
    public DependencyArc addArc(DepNode integrationPoint, Collection<Role> roles, DepNode argument, DepNode relation)
    {
        // check whether integrationPoint exists already, and if it exists add Role only (maybe)
        DependencyArc arc = new DependencyArc(integrationPoint, roles, argument, relation);        
        pushHangingModifier(arc); // keep track of relation-incomplete modifiers that may be arbitrarily adjoined later to their relation(s)
        arcs.put(integrationPoint, new DependencyArc(integrationPoint, roles, argument, relation));
        return arc;
    }

    public DependencyArc addArc(DependencyArc arc)
    {
        if(arc.isSelfReference()) // cyclic dependency (possible error)
        {
            return null;
        }
        pushHangingModifier(arc); // keep track of relation-incomplete modifiers that may be arbitrarily adjoined later to their relation(s)
        // check whether there is already a complete arc at the same integration point
        Collection<DependencyArc> existingArcs = arcs.getCollection(arc.getIntegrationPoint());
        if(existingArcs != null)
        {
            for(DependencyArc existingArc : existingArcs)
            {
//                boolean merge = false;
//                if(!(existingArc == null))// || existingArc.isIncomplete()))              
                // then check whether the existing arc has the same relation and argument. If true just merge roles
                if(arcsWithSameArgumentRelation(existingArc, arc))
                {
//                    merge = true;
                    for(Role role : arc.getRoles())
                        existingArc.addRole(role);
                    return arc;
                } // if
//                    else
//                        arc.integrationPointIncr();         
//                if(!merge)
//                    arcs.put(arc.getIntegrationPoint(), arc);
            }
//            return arc;
        }
        // check whether there is already a complete arc at any integration point
        else if(!getArcsWithArgumentRelation(arc.getArgument(), arc.getRelation()).isEmpty())
        {
            return null;                
        }
        arcs.put(new DepNode(arc.getIntegrationPoint()), arc);
        return arc;
    }
    
    public DepNode addHangingRelation(DepNode relation)
    {
        return hangingRelations.put(relation.category, relation); // Check: possibly putting two relations with same category together
    }
    
    public void addShadowArgument(DepNode anchor, DependencyArc arc)
    {
        arcsWithShadowArgs.put(anchor, arc);
    }
    
    public Collection<DependencyArc> getDependenciesByIntegPoint(DepNode ip)
    {
        return arcs.getCollection(ip);
    }
    
    public boolean containsDependencyByIntegPoint(DepNode ip)
    {
        return arcs.containsKey(ip);
    }
    
    public DepNode getRelationWithCategory(String category, boolean isRelation)
    {
//        if (hangingRelations.isEmpty())
//        {
//            return null;
//        }
//        for(DepNode relation : hangingRelations)
//        {
//            if(relation.category.equals(category))
//                return relation;
//        }
//        return null;
        String[] catLabel = category.split("\t");
        return hangingRelations.get(catLabel.length > 1 ? catLabel[1] : catLabel[0]);
    }
    
    public List<DependencyArc> getArcsWithRelation(String relation)
    {
        List<DependencyArc> list = new ArrayList();
        if(relation != null)
        {
            for(Entry<DepNode, Object> arcsWithSameIp : arcs.entrySet())
            {
                for(DependencyArc arc : (Collection<DependencyArc>)arcsWithSameIp.getValue())
                {
                    if(arc.getRelation() != null && arc.getRelation().category != null && arc.getRelation().category.equals(relation))
                        list.add(arc);
                } // for
            }            
        }
        return list;
    }
    
    public List<Pair<DepNode, DependencyArc>> getArcsWithArgumentRelation(DepNode argument, DepNode relation)
    {
        List<Pair<DepNode, DependencyArc>> list = new ArrayList();
        if(argument != null && relation != null)
        {
            for(Entry<DepNode, Object> arcsWithSameIp : arcs.entrySet())
            {
                DepNode ip = arcsWithSameIp.getKey();
                for(DependencyArc arc : (Collection<DependencyArc>)arcsWithSameIp.getValue())
                {                    
                    if(arc.getArgument() != null && arc.getRelation() != null && arc.getArgument().timestamp != Integer.MIN_VALUE && 
                        arc.getRelation().timestamp != Integer.MIN_VALUE && arc.getArgument().timestamp == argument.timestamp && 
                        arc.getRelation().timestamp == relation.timestamp)
                        list.add(new Pair<DepNode, DependencyArc>(ip, arc));
                } // for
            }            
        }
        return list;
    }
    
    public boolean arcsWithSameArgumentRelation(DependencyArc arc1, DependencyArc arc2)
    {
        if (arc1.getArgument() == null && arc2.getArgument() != null ||
            arc1.getArgument() != null && arc2.getArgument() == null)
            return false;
        if (arc1.getRelation() == null && arc2.getRelation() != null ||
            arc1.getRelation() != null && arc2.getRelation() == null)
            return false;
        if(arc1.getArgument() == null && arc2.getArgument() == null)
        {
            if(arc1.getRelation() == null && arc2.getRelation() == null)
                return true;
            else
                return arc1.getRelation().equals(arc2.getRelation());
        }
        if(arc1.getRelation() == null && arc2.getRelation() == null)
        {
            if(arc1.getArgument() == null && arc2.getArgument() == null)
                return true;
            else
                return arc1.getArgument().equals(arc2.getArgument());
        }
        return arc1.getArgument().equals(arc2.getArgument()) && arc1.getRelation().equals(arc2.getRelation());
//        return arc1.getArgument() != null && arc1.getRelation() != null && 
//                arc2.getArgument() != null && arc2.getRelation() != null && 
//                arc1.getArgument().timestamp != Integer.MIN_VALUE && arc1.getRelation().timestamp != Integer.MIN_VALUE && 
//                arc2.getArgument().timestamp != Integer.MIN_VALUE && arc2.getRelation().timestamp != Integer.MIN_VALUE && 
//                arc1.getArgument().timestamp == arc2.getArgument().timestamp && arc1.getRelation().timestamp == arc2.getRelation().timestamp;
    }
    
    public DependencyArc removeArc(DepNode integrationPoint, DependencyArc arc)
    {
        return arcs.remove(integrationPoint, arc);
    }
    
    public DependencyArc pollArcWithShadowArg(DepNode anchor)
    {
        return arcsWithShadowArgs.remove(anchor);    
    }
    
    public void removeDeadArcsWithShadowArg()
    {
        if(arcsWithShadowArgIsEmpty())
            return;
        Iterator<Entry<DepNode, DependencyArc>> it = arcsWithShadowArgs.entrySet().iterator();
        while(it.hasNext())
        {
            Entry<DepNode, DependencyArc> e = it.next();
            DependencyArc arc = e.getValue();
            if(arc == null || !arc.hasRoles() )
                it.remove();
        }
    }
    
    public DependencyArc getArcWithShadowArg(DepNode anchor)
    {
        return arcsWithShadowArgs.get(anchor);
    }
    
    public Collection<DependencyArc> getArcsWithShadowArg()
    {
        return arcsWithShadowArgs.values();
    }
    
    public boolean arcsWithShadowArgIsEmpty()
    {
        return arcsWithShadowArgs.isEmpty();
    }
        

    public Set<DepNode> integrationPointsSet()
    {
        return arcs.keySet();
    }
        
    public boolean addInfinitiveMarkerNode(DepNode im)
    {
        return infinitiveMarkers.add(im);
    }
    
    public DepNode peekLastInfinitiveMarker()
    {
        return infinitiveMarkers.peekLast();
    }
    
    public DepNode popLastInfinitiveMarker()
    {             
        return infinitiveMarkers.pollLast();
    }
    
    public boolean infinitiveMarkerIsEmpty()
    {
        return infinitiveMarkers.isEmpty();
    }

    public void setCurrentAuxVerb(DepNode currentAuxVerb)
    {
        this.currentAuxVerb = currentAuxVerb;
    }

    public DepNode getCurrentAuxVerb()
    {
        return currentAuxVerb;
    }
    
    public void addAuxVerbNode(String verb)
    {
        auxVerbs.put(verb, new DepNode(currentAuxVerb));
        currentAuxVerb = null;
    }
    
    public boolean containsAuxVerb(String verb)
    {
        return auxVerbs.containsKey(verb);
    }
    
    public DepNode getAuxVerb(String verb)
    {                     
//        return auxVerbs.remove(verb);
        return auxVerbs.get(verb);
    }
    
    public boolean auxVerbsIsEmpty()
    {
        return auxVerbs.isEmpty();
    }
    
    public void pushNpHead(Node node)
    {
        npHeads.push(node);
    }
    
    public Node peekFirstNpHead()
    {
        return npHeads.peekFirst();
    }
    
    public Node peekLastNpHead()
    {
        return npHeads.peekLast();
    }
    
    public Node popNpHead()
    {
        return npHeads.pop();
    }
    
    public boolean npHeadsIsEmpty()
    {
        return npHeads.isEmpty();
    }
    
    public int npHeadsSize()
    {
        return npHeads.size();
    }
    
    public void pushHangingModifier(DependencyArc arc)
    {
        if(arc.isModifier() && arc.getRelation() == null && arc.getArgument() != null && !arc.equals(hangingModifiers.peekLast()))
            hangingModifiers.push(arc);        
    }
    
    public DependencyArc peekFirstHangingModifier()
    {
        return hangingModifiers.peekFirst();
    }
    
    public boolean hangingModifiersIsEmpty()
    {
        return hangingModifiers.isEmpty();
    }
    
    public DependencyArc popHangingModifier()
    {
        return hangingModifiers.isEmpty() ? null : hangingModifiers.pop();
    }

    public void setCurrentCoord(Pair<String, String> currentCoord)
    {
        this.currentCoord = currentCoord;
    }

    public Pair<String, String> getCurrentCoord()
    {
        return currentCoord;
    }
    
    public void removeCurrentCoord()
    {
        currentCoord = null;
    }

    public void setCurrentRelative(DepNode currentRelative)
    {
        this.currentRelative = currentRelative;
    }

    public DepNode getCurrentRelative()
    {
        return currentRelative;
    }
    
    public void removeCurrentRelative()
    {
        currentRelative = null;
    }
    
    public void addHangingPreposition(DepNode prep, DepNode ip)
    {
        hangingPreps.add(new Pair(prep, ip));
    }
    
    public Pair<DepNode, DepNode> popHangingPreposition()
    {
        return hangingPreps.pop();
    }
    
    public Pair<DepNode, DepNode> pollHangingPreposition()
    {
        return hangingPreps.peekLast();
    }
    
    public boolean hangingPrepositionsIsEmpty()
    {
        return hangingPreps.isEmpty();
    }
    
    public boolean twoFirstElementsEquals(short firstId, short secondId)
    {
        if (npHeads.size() > 1)
        {
            Node stackFirst = npHeads.getFirst();
            Node stackSecond = npHeads.get(1);
            return stackFirst == null ? false : stackFirst.getNodeId() == firstId && stackSecond.getNodeId() == secondId;
//            return stackLast.getCategory().equals(last.getCategory()) && 
//                    stackLast.getNodeId() == last.getNodeId() &&
//                    stackSecondToLast.getCategory().equals(secondToLast.getCategory()) && 
//                    stackSecondToLast.getNodeId() == secondToLast.getNodeId();
//                    
        }        
        return false;
    }

    /**
     * 
     * Post process arcs, including removing redundant incomplete arcs, and updating the operation
     * path on valid incomplete ones.          
     * @param operation the parsing operation applied (adjunction, substitution, verification or initial)
     * @param direction true if the prefix tree was adjoined up on the elementary, false otherwise     
     * @param isShadowTree <code>true</code> if the incoming {@see pltag.corpus.ElementaryStringTree} tree is a shadow (prediction) tree
     */
    
    public void postProcessArcs(String operation, boolean direction, boolean isShadowTree)
    {
        List<Pair<DepNode, DependencyArc>> toBeRemoved = new ArrayList<Pair<DepNode, DependencyArc>>();        
        Iterator<Entry<DepNode, DependencyArc>> it = arcs.iterator();
        while(it.hasNext())
        {
            Entry<DepNode, DependencyArc> e = it.next();
            removeRedundantIncompleteArcs(e.getValue(), toBeRemoved);
            e.getValue().updateOperationPath(operation, direction, isShadowTree);
        }
        for(Pair<DepNode, DependencyArc> pair : toBeRemoved)
            removeArc(pair.getFirst(), pair.getSecond());
//        removeDeadArcsWithShadowArg();
    }
    
    public void removeRedundantIncompleteArcs(DependencyArc arc, List<Pair<DepNode, DependencyArc>> toBeRemoved)
    {
        if(!arc.isIncomplete())
        {
            Role r = arc.getFirstRole(); // we assume the arc has been disambiguated
            // check for incomplete arcs with the same relation
            for(DependencyArc arcWithSameRelation : getArcsWithRelation(arc.getRelation().category))
            {
                if(arcWithSameRelation.isArgumentIncomplete())
                {
                    // we want to delete the same role labels since the incoming arc has already been completed
                    Iterator<Role> it = arcWithSameRelation.getRoles().iterator();
                    while(it.hasNext())
                    {
                        Role candRole = it.next();
                        if(r != null && candRole.sameName(r))
                            it.remove();
                    }
                    // if the incomplete arc had only one role label, and it got removed then we can safely remove the arc as well
                    if(arcWithSameRelation.getRoles().isEmpty())
                    {
                        toBeRemoved.add(new Pair(arcWithSameRelation.getIntegrationPoint(), arcWithSameRelation));
//                        removeArc(arcWithSameRelation.getIntegrationPoint(), arcWithSameRelation);
                    }
                }
            }
        }
    }
    
    public Map<Predicate, Proposition> toPropositions(StringTreeAnalysis tree, ConllExample gold, ParserModel model)
    {
        Map<Integer, Predicate> goldPreds = gold.getPredicates(); // ordered map of gold predicates
        Map<Integer, Predicate> predPreds = new HashMap(); // ordered map of gold predicates
        Map<Predicate, Proposition> predPropositions = new HashMap();
        for(Entry<DepNode, Object> arcsWithSameIp : arcs.entrySet())
        {
            for(DependencyArc dependency : (Collection<DependencyArc>)arcsWithSameIp.getValue())        
            {
                DepNode predRel = dependency.getRelation();
                if(predRel != null)
                {                
                    Predicate predPred = goldPreds.get(predRel.timestamp); // cheating: we are also copying the sense. In the future just copy the lemma
                    if(predPred != null) // cheating: normally we should just add the predicate with the incorrect sense (and related arguments)
                    {
                        predPred.setElemTree(predRel.elemTree); // add elementary tree information (for feature extraction)
                        predPreds.put(predRel.timestamp, predPred);
                        Proposition prop = predPropositions.get(predPred);                    
                        if(prop == null)
                        {
                            prop = new Proposition(predPred);
                            predPropositions.put(predPred, prop);
                        }
                        if(!dependency.isArgumentIncomplete())
                        {                            
//                            prop.addArgument(new Argument(predArg.timestamp, dependency.getTopRole(model, vec), 
                            if(!model.getOpts().applyConllHeuristics) // we need to apply CoNLL heuristics in order to evaluate correctly
                            {
                                reApplyConllHeuristics(dependency, tree);
                            }
                            DepNode predArg = dependency.getArgument();
                            String topRole;
                            if(model.getOpts().useClassifiers) // disambiguate using classifier
                            {
                                FeatureVec vec = dependency.extractBilexicalFeaturesOnly(model.getArgumentClassifier());
                                topRole = dependency.getTopRole(model, vec);
                            }
                            else // use majority class heuristic to disambiguate multiple role entries
                            {                                
                                topRole = model.getTrainRoleFreqs().isEmpty() ? dependency.getTopRole(model, null) : dependency.getMajorityTopRole(model);
                            }
                            prop.addArgument(new Argument(predArg.timestamp, topRole, 
                                    predArg.category, predArg.posTag, predArg.elemTree, 
                                    ((DepNode)dependency.getIntegrationPoint()).category, dependency.getPrefixFringeAlphaSet(), dependency.getOperationPath(), null));
                        }                    
                    } 
                    else
                    {
                        // TODO: Fill in incorrect sense
                    }
                } // if            
            } // for
        }
        return predPropositions;
    }
    
    /**
     * Get compact collection of prepositions. Useful for storing to disk (discriminative reranker)
     * @param featureIndexers     
     * @param train     
     * @return 
     */
    public Pair<Proposition[], Proposition[]> toPropositions(DiscriminativeFeatureIndexers featureIndexers, boolean train)
    {                
        Map<Predicate, Proposition> predPropositions = new HashMap();
        List<Proposition> predIncompletePropositions = new ArrayList();
        for(Entry<DepNode, Object> arcsWithSameIp : arcs.entrySet())
        {
            for(DependencyArc dependency : (Collection<DependencyArc>)arcsWithSameIp.getValue())        
            {
                DepNode predRel = dependency.getRelation();
                if(predRel != null)
                {                
                    Predicate predPred = new Predicate(featureIndexers, train, predRel.category, predRel.posTag, Predicate.Type.verb, predRel.timestamp); // we are not copying the sense
                    Proposition prop = predPropositions.get(predPred); 
                    boolean newProposition = prop == null;
                    if(newProposition)
                    {
                        prop = new Proposition(predPred);                        
                    }
                    if(!dependency.isArgumentIncomplete())
                    {
                        DepNode predArg = dependency.getArgument();
                        String topRole = dependency.getFirstRole().toString();                        
                        prop.addArgument(new Argument(featureIndexers, train, predArg.timestamp, topRole, predArg.category, predArg.posTag));
                        if(newProposition)
                            predPropositions.put(predPred, prop);
                    }    
                    else
                    {
                        Proposition incomplete = new Proposition(predPred);
                        Argument emptyArg = new Argument(featureIndexers, train, -1, null, null, null);
                        incomplete.addArgument(emptyArg);
                        predIncompletePropositions.add(incomplete);
                    }
                } // if    
                else if(dependency.isRelationIncomplete() && !dependency.isArgumentIncomplete())
                {
                    Predicate emptyPred = new Predicate(featureIndexers, train, null, null, Predicate.Type.verb, -1); // we are not copying the sense                    
                    Proposition incomplete = new Proposition(emptyPred);                    
                    DepNode predArg = dependency.getArgument();
                    incomplete.addArgument(new Argument(featureIndexers, train, predArg.timestamp, null, predArg.category, predArg.posTag));
                    predIncompletePropositions.add(incomplete);
                }
            } // for
        }
        return new Pair(predPropositions.values().toArray(new Proposition[0]), predIncompletePropositions.toArray(new Proposition[0]));
    }
    
    public IncrementalSemanticsWidget convertDependenciesAt(int timestamp, boolean fullSentence, StringTreeAnalysis partialTree, ConllExample gold, ParserModel model)
    {
        IncrementalSemanticsWidget widget = new IncrementalSemanticsWidget(timestamp, fullSentence);
        Map<Integer, Predicate> goldPreds = gold != null ? gold.getPredicates() : null; // ordered map of gold predicates                
        for(Entry<DepNode, Object> arcsWithSameIp : arcs.entrySet())
        {
            for(DependencyArc dependency : (Collection<DependencyArc>)arcsWithSameIp.getValue())        
            {
                DepNode predRel = dependency.getRelation();
                if(predRel != null)
                {                
                    // cheating: we are also copying the sense. In the future just copy the lemma
                    Predicate predPred = goldPreds != null ? goldPreds.get(predRel.timestamp) : new Predicate(predRel.category + ".XX", predRel.posTag, Predicate.Type.verb, predRel.timestamp); 
                    if(predPred != null) // cheating: normally we should just add the predicate with the incorrect sense (and related arguments)
                    {
                        widget.addPredicateWithSense(predPred);
                        widget.addPredicate(String.format("%s:%s", predPred.getTimestamp(), predPred.getLemma()));
                                                
                        if(!dependency.isArgumentIncomplete())
                        {                            
                            if(!model.getOpts().applyConllHeuristics) // we need to apply CoNLL heuristics in order to evaluate correctly
                            {
                                reApplyConllHeuristics(dependency, partialTree);
                            }
                            DepNode predArg = dependency.getArgument();
                            String topRole;
                            if(model.getOpts().useClassifiers) // disambiguate using classifier 
                            {
                                FeatureVec vec = dependency.extractBilexicalFeaturesOnly(model.getArgumentClassifier());
                                topRole = dependency.getTopRole(model, vec);
                            }
                            else // use majority class heuristic to disambiguate multiple role entries
                            {                                
                                topRole = model.getTrainRoleFreqs().isEmpty() ? dependency.getTopRole(model, null) : dependency.getMajorityTopRole(model);
                            }
                            widget.addArgRole(new Pair(predPred.getTimestamp(), new Argument(predArg.timestamp, topRole, predArg.category, predArg.posTag)));
                            widget.addArgWord(String.format("%s:%s", predArg.timestamp, predArg.category)); // add identified word even if it's in a complete dependency
                        }
                        else
                        {
                            widget.addIncompleteArc(String.format("%s:%s", predPred.getTimestamp(), predPred.getLemma())); // add argument-incomplete arc
                        }
                    } 
                    else
                    {
                        // TODO: Fill in incorrect sense
                    }
                } // if
                else if(!dependency.isArgumentIncomplete()) // depedency arc with incomplete relation
                {
                    DepNode arg = dependency.getArgument();
                    widget.addArgWord(String.format("%s:%s", arg.timestamp, arg.category));
                    widget.addIncompleteArc(String.format("%s:%s", arg.timestamp, arg.category));
                }
            } // for
        } // for
        return widget;
    }
    
    /**
     * 
     * In case iSRL is forced to not apply the CoNLL heuristics for certain pred-arg dependencies (see list below),
     * we need to re-apply them in order to allow for a fair evaluation.
     * The list of heuristics are:<br/>
     *
     * <li>
     *      Infinitive Markers: the head of a VP is the IM to, if it is present, rather than the verb itself,
     * </li>
     * <li>
     *      PPs: the head of a PP is the preposition rather than the head of the clause that follows,
     * </li>
     * <li>
     *      Auxiliaries: AUX becomes the head instead of the following verb,
     * </li>
     * <li>
     *      Post Honorific: assign the argument to the word that precedes the post-honorific.
     * </li>
     */
    private void reApplyConllHeuristics(DependencyArc dependency, StringTreeAnalysis tree)
    {        
        DepNode predArg = dependency.getArgument();
        int timestamp = predArg.timestamp;
        if(timestamp > 0)
        {
            if(isVerb(predArg.posTag)) // found verb
            {
                // look at the previous word and POS-tag (if any) of the argument
                List<Pair<String, String>> leavesPreTerminals = tree.getLeavesPreTerminal();
                Pair<String, String> pair = leavesPreTerminals.get(timestamp - 1);
                String prevWord = pair.getFirst();
                String prevPos = pair.getSecond();
                // check for infinitive marker
                if(prevWord.equals("to") && prevPos.equals("TO"))
                {
                    dependency.setArgument(new DepNode(prevWord, prevPos, timestamp - 1));
                    pollArcWithShadowArg(dependency.getIntegrationPoint());
                }
                else
                {
                    // check for auxiliary verb in the list of words preceding the verb
                    for(int i = timestamp - 1; i >= 0; i--)
                    {                    
                        Pair<String, String> wordPos = leavesPreTerminals.get(i);
                        if(isAuxiliary(wordPos.getSecond()) && !isParticiple(wordPos.getFirst()))
                        {
                            dependency.setArgument(new DepNode(wordPos.getFirst(), wordPos.getSecond(), i));
                        }
                    }
                }                                
            }
            else if(isPostHonorific(predArg.category))
            {
                // look at the previous word and POS-tag (if any) of the argument                
                Pair<String, String> pair = getPreviousWordPos(tree, timestamp);
                String prevWord = pair.getFirst();
                String prevPos = pair.getSecond();
                dependency.setArgument(new DepNode(prevWord, prevPos, timestamp - 1));
            }
            else if(!hangingPrepositionsIsEmpty())
            {
                Pair<DepNode, DepNode> prepIp = pollHangingPreposition();
                if(prepIp.getSecond().equals(dependency.getIntegrationPoint()))
                {
                    dependency.setArgument(prepIp.getFirst());
                }
            }
        }
    }
    
    private boolean isVerb(String category)
    {
        return category.startsWith("VB");
    }
    
    private boolean isParticiple(String word)
    {
        return word.equals("have") || word.equals("be");
    }
    
    private Pair<String, String> getPreviousWordPos(StringTreeAnalysis tree, int curTimestamp)
    {
        List<Pair<String, String>> leavesPreTerminals = tree.getLeavesPreTerminal();
//        return leavesPreTerminals.get(leavesPreTerminals.size() - 2);
        return leavesPreTerminals.get(curTimestamp - 1);
    }
    
    public Map<Integer, Predicate> toPredicates(Map<Predicate, Proposition> propositions)
    {
        Map<Integer, Predicate> map = new HashMap();
        for(Predicate pred : propositions.keySet())
        {
            map.put(pred.getTimestamp(), pred);
        }
        return map;
    }
    
    public String toConll(StringTreeAnalysis tree, ConllExample gold, ParserModel model)
    {        
        Map<Predicate, Proposition> predPropositions = toPropositions(tree, gold, model);
        Map<Integer, Predicate> predPreds = toPredicates(predPropositions);
        List<List<String>> goldWords = Utils.unpackConllSentenceToTokens(gold.getGoldStandardConll());                
        String[][] predConll = new String[goldWords.size()][ConllExample.NO_OF_FIXED_COLS + predPreds.size()];
        Map<Integer, String[]> predPropsArrays = new TreeMap();
        // convenience string arrays with correct size, that contain the predicted arg labels or _ for null entry, ordered by predicate position
        for(Entry<Predicate, Proposition> e : predPropositions.entrySet())
        {
            predPropsArrays.put(e.getKey().getTimestamp(), e.getValue().toStringArray(goldWords.size()));
        }
        for(int i = 0; i < goldWords.size(); i++)
        {
            // copy word ids and forms
            predConll[i][0] = goldWords.get(i).get(0);
            predConll[i][1] = goldWords.get(i).get(1);
            for(int j = 2; j < ConllExample.FILLPRED_COL; j++)
            {
                predConll[i][j] = "_"; // leave everything blank
            }                      
            // write predicate
            predConll[i][ConllExample.PRED_COL] = predPreds.containsKey(i) ? predPreds.get(i).toString() : "_";
            predConll[i][ConllExample.FILLPRED_COL] = predPreds.containsKey(i) ? "Y" : "_";
            int k = 0;
            // write predicate arguments
            for(String[] propArray : predPropsArrays.values())
            {
                predConll[i][ConllExample.NO_OF_FIXED_COLS+ k] = propArray[i];
                k++;
            }
        }
        // write back to a nice conll-style string
        return Utils.repackConllTokensToSentence(predConll);
    }
    
    public boolean isEmpty()
    {
        return arcs.isEmpty();
    }

    @Override
    public boolean equals(Object obj)
    {
        assert obj instanceof Dependencies;
        Dependencies d = (Dependencies)obj;
        return arcs.equals(d.arcs);
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 29 * hash + (this.arcs != null ? this.arcs.hashCode() : 0);
        return hash;
    }
    
       
    public String toStringCompletedOnly(String currentWord, int currentId)
    {
        StringBuilder str = new StringBuilder();
        for(Entry<DepNode, Object> arcsWithSameIp : arcs.entrySet())
        {
            for(DependencyArc arc : (Collection<DependencyArc>)arcsWithSameIp.getValue())        
            {
                if(!arc.isIncomplete())
                    str.append(arc.prettyPrint()).append(String.format("\tid: %s\tword:%s\n", currentId, currentWord));
            }
        }
        if(str.length() > 0) // remove final line break
            str.deleteCharAt(str.length() - 1);
        return str.toString(); 
    }
    
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        for(Entry<DepNode, Object> arcsWithSameIp : arcs.entrySet())
        {
            for(DependencyArc arc : (Collection<DependencyArc>)arcsWithSameIp.getValue())        
            {
                str.append(arc).append("\n");
            }
        }
//        str.append("NPs: ");
//        for(Node n : npHeads)
//            str.append(n.toString()).append(" ").append(n.getLambda()).append("\t").append(n.getNodeId()).append("\t[").append(n.getOrigTree()).append("]\n");
        return str.toString();
    }   
    
    
}
