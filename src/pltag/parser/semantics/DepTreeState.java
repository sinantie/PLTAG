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
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import pltag.corpus.ElementaryStringTree;
import pltag.parser.Fringe;
import pltag.parser.FringeAndProb;
import pltag.parser.Node;
import pltag.parser.ParserModel;
import pltag.parser.ShadowStringTree;
import pltag.parser.TreeState;
import pltag.parser.semantics.classifier.ArgumentClassifier;
import pltag.parser.semantics.classifier.FeatureVec;

/**
 *
 * @author konstas
 */
public class DepTreeState extends TreeState implements Serializable
{
    
    static final long serialVersionUID = -1L;
    private transient final ParserModel model;
    private final Dependencies dependencies;        
    private final boolean applyConllHeuristics;
    
    private enum CandidateType {none, npSbj, trace}
    
    public DepTreeState(ParserModel model)
    {
        dependencies = new Dependencies();
        this.model = model;
        applyConllHeuristics = model.getOpts().applyConllHeuristics;
    }

    public DepTreeState(boolean auxtree, boolean footleft, short[] wordcover, 
            Fringe newFringe, ArrayList<Fringe> newUa, FringeAndProb futureFringe, 
            ArrayList<ShadowStringTree> shadowListDeepCopy, boolean hasNonShadowLeaf, 
            int nBest, Dependencies dependencies, ParserModel model)
    {
        super(auxtree, footleft, wordcover, newFringe, newUa, futureFringe, shadowListDeepCopy, hasNonShadowLeaf, nBest);
        this.dependencies = new Dependencies(dependencies); // call copy-constructor       
        this.model = model;
        applyConllHeuristics = model.getOpts().applyConllHeuristics;
    }
    
    @Override
    public DepTreeState copy()
    {
        ArrayList<Fringe> completeFringe = allFringeCopy();
        Fringe newFringe = completeFringe.remove(0);
        ArrayList<Fringe> newUa = completeFringe;
        return new DepTreeState(auxtree, footleft, wordcover.clone(), newFringe, newUa, futureFringe,
                shadowListDeepCopy(), hasNonShadowLeaf, nBest, dependencies, model);
    }
    
    /**
     * Update the list of dependencies by creating incomplete dependencies given the potential roles in the tree.
     * This method is called only by the initial operation.
     * @param tree 
     * @param origPosTags 
     * @param words 
     * @param operation the parsing operation applied (adjunction, substitution, verification or initial)     
     * @param timestamp the current word index
     */
    public void updateDependencies(ElementaryStringTree tree, String[] words, String[] origPosTags, String operation, int timestamp)
    {        
        retainInfoFromTreeHeuristics(this, null, tree, (short)0, false, timestamp, false, null, null);
        // Process the rest roles in the elementary tree
        if (tree.hasRoles())
        {            
            addIncompleteDependencies(tree, (short)0, false, tree.isRelation(), getRolesPerNode(tree, (short)0), null, words, origPosTags, operation, true, timestamp);
        }   
        // If the tree (//has no roles but) is a relation, make a note of it in a separate map
        // of hanging relations for later use.
        if(tree.isRelation())
        {            
            addHangingRelation(tree, (short)0, words, origPosTags, operation, true, false, timestamp);
        }
        dependencies.postProcessArcs(operation, true, false);
    }
    
    /**
     * Updates and/or adds new dependencies based on the current fringe, and more specifically
     * the integration point on the prefix tree, and the incoming elementary tree. The routine
     * checks first whether the attachment node (i.e., foot or substitution node) is on the prefix
     * tree or on the elementary tree. In case it is on the prefix tree (Down Substitution and Up Adjunction),
     * we assume that there already exists a dependency in the list of dependencies, so we retrieve and update 
     * it accordingly given the incoming information from the the elementary tree (i.e., roles, argument or relation anchors).
     * If the attachment node is on the elementary tree (Up Substitution and Down Adjunction), 
     * we add a new dependency, given the operation, head information from the prefix tree node and the roles on
     * the elementary tree. See {@link #addDependency(DepNode,ElementaryStringTree,short,MultiValueMap,Node,boolean)} 
     * @param elemTreeState the treeState of the elementary tree
     * @param prefixTreeNode the integration point {@see pltag.parser.Node} on the prefix tree
     * @param tree the {@see pltag.corpus.ElementaryStringTree} tree which attaches to the prefix tree via some TAG operation
     * @param adjoiningNodeInElemTreeId the id of the {@see pltag.parser.Node} that attaches to the integration point on the prefix tree
     * @param elemTreeOffset the offset of id positions in order to correctly identify the node ids of the elementary tree
     * after it has been integrated in the prefix tree
     * @param adjoiningNodeOnPrefixTree whether the attaching node is on the prefix tree. <code>true</code> in case we
     * are Substituting Down or Adjoining Up, <code>false</code> if we are Substituting Up or Adjoining Down
     * @param isShadowTree <code>true</code> if the incoming {@see pltag.corpus.ElementaryStringTree} tree is a shadow (prediction) tree
     * @param origPosTags an array of the gold POS tags, used for constructing the feature vector for identification/labeling
     * @param words
     * @param operation the parsing operation applied (adjunction, substitution, verification or initial)
     * @param direction true if the prefix tree was adjoined up on the elementary, false otherwise
     * @param timestamp the current word index
     */
    public void updateDependencies(TreeState elemTreeState, Node prefixTreeNode, ElementaryStringTree tree,
            short adjoiningNodeInElemTreeId, short elemTreeOffset, boolean adjoiningNodeOnPrefixTree, boolean isShadowTree, 
            String[] words, String[] origPosTags, String operation, boolean direction, int timestamp)
    {
        retainInfoFromTreeHeuristics(elemTreeState, prefixTreeNode, tree, elemTreeOffset, isShadowTree, timestamp, false, null, null);
        MultiValueMap<Integer, Role> rolesPerNode = getRolesPerNode(tree, elemTreeOffset);
//        DepNode integrationPoint = new DepNode(ipOnPrefixTree ? integrationPointId : integrationPointId + ipIdOffset, isShadowTree);                
        // The integration point is always set to be on the prefix tree node where the operation is taking place
        DepNode integrationPoint = new DepNode(prefixTreeNode.getNodeId(), timestamp, isShadowTree);
        // First check whether the attaching node is on the prefix tree
        if(adjoiningNodeOnPrefixTree) // Down-Subst, Up-Adj
        {
            Node shadowTreeRootNode = isShadowTree ? findNodeInFringeById((short)(tree.getRoot() + elemTreeOffset)) : null;
            if(prefixIntegrationPointHasRole(integrationPoint)) // the prefix tree attaching node has a role. 
                updateDependency(integrationPoint, shadowTreeRootNode, tree, elemTreeOffset, isShadowTree, words, origPosTags, operation, direction, isShadowTree, timestamp);
//            else if() // the elementary tree attaching node has a role.
                
        }
        // Then check whether the attaching node is on the elementary tree and has a role.
        else if(!adjoiningNodeOnPrefixTree && elemIntegrationPointHasRole(tree, rolesPerNode, adjoiningNodeInElemTreeId + elemTreeOffset)) // Up-Subst, Down-Adj
        {
            addDependency(integrationPoint, tree, adjoiningNodeInElemTreeId, elemTreeOffset, rolesPerNode, prefixTreeNode, isShadowTree, 
                    words, origPosTags,  operation, direction, timestamp);
            // remove ip node (on elementary tree) from rolesPerNode map after it's been dealt with
//            rolesPerNode.remove(integrationPoint.getId());
            rolesPerNode.remove(adjoiningNodeInElemTreeId + elemTreeOffset);
        }
        // Process the rest roles in the elementary tree
        if (tree.hasRoles())    // !rolesPerNode.isEmpty()
        {            
            addIncompleteDependencies(tree, elemTreeOffset, isShadowTree, tree.isRelation(), 
                    rolesPerNode, prefixTreeNode, words, origPosTags, operation, direction, timestamp);
        }                   
        // fill in an incomplete argument that is part-filled by the head node of a shadow tree.
        // Assign the anchor of the elementary tree as the argument, even if it's not the head of the whole subtree.
        // This is based on the assumption, that it is the first leaf node inside the argument boundaries (i.e, 
        // subtree that is rooted at the head node of the shadow tree), whose head is a token outside the
        // argument boundaries (i.e., the relation of the incomplete proposition).  WRONG assumption       
  //     fillShadowArgument(prefixTreeNode, tree, elemTreeOffset);        
        // If the tree (//has no roles but) is a relation, make a note of it in a separate map
        // of hanging relations for later use.
        if(tree.isRelation())
        {            
            addHangingRelation(tree, elemTreeOffset, words, origPosTags,  operation, direction, isShadowTree, timestamp);
        }
        dependencies.postProcessArcs(operation, direction, isShadowTree);
    }
    
    /**
     * 
//     * @param coveredNodes map of verification node ids to (verified) shadow tree node ids
     * @param elemTreeState
     * @param coveredNodes map of (verified) shadow tree node ids to verification node ids
     * @param tree
     * @param ipIdOffset
     * @param offsetNodeIdsOfShadowTree 
     * @param shadowTreeRoot 
     * @param words 
     * @param origPosTags
     * @param operation the parsing operation applied (adjunction, substitution, verification or initial)     
     * @param timestamp 
     */
    public void updateDependencies(TreeState elemTreeState, DualHashBidiMap<Integer, Integer> coveredNodes, ElementaryStringTree tree, short ipIdOffset, 
            DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree, Node shadowTreeRoot, String[] words, String[] origPosTags,
            String operation, int timestamp)
    {            
        retainInfoFromTreeHeuristics(elemTreeState, shadowTreeRoot, tree, ipIdOffset, false, timestamp, true, coveredNodes, offsetNodeIdsOfShadowTree);
        DepNode anchorNode = getAnchorNode(tree, ipIdOffset, timestamp);
        Set<Integer> processedNodes = new HashSet<>();
        // First check if the verifying tree has roles
        if(tree.hasRoles())
        {
            MultiValueMap<Integer, Role> rolesPerNode = getRolesPerNode(tree, ipIdOffset);
            // Browse through all candidate integration points
            for(Integer ipCandidate : rolesPerNode.keySet())
            {
                processedNodes.add(ipCandidate);
                // find the ones that are part of the verification tree, hence exist on the prefix tree as part of shadow trees
                Integer shadowNodeId = coveredNodes.getKey(ipCandidate - ipIdOffset);
                if(shadowNodeId != null)
                {
                    // find the arc with the partially complete dependency
                    Short offsetShadowId = offsetNodeIdsOfShadowTree.get(shadowNodeId.shortValue());
                    if(offsetShadowId != null)
                    {
                        Collection<DependencyArc> arcs = dependencies.getDependenciesByIntegPoint(new DepNode(offsetShadowId.shortValue(), timestamp));
                        if(arcs != null)
                        {
//                            for(DependencyArc arc : arcs)
                            Iterator<DependencyArc> iterator = arcs.iterator();
                            while(iterator.hasNext())
                            {
                                DependencyArc arc = iterator.next();
                                if(arc.isIncomplete()) 
                                {                        
                                    if(tree.isRelation() && arc.isRelationIncomplete()) // avoid filling in an already complete relation entry
                                    {
                                        filterRoles(arc, rolesPerNode.getCollection(ipCandidate));
                                        setRelation(arc, anchorNode, iterator, words, origPosTags, operation, true, false);
                                        // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
                                        boolean keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags);
                                        if(!keepArc)
                                            removeArcSafe(arc, arc.getIntegrationPoint(), iterator);
                                    }
            //                        else if(!tree.isRelation() && arc.isArgumentIncomplete())
                                    // removed restriction of above if statement: a relation can be an argument to another relation
                                    else if(arc.isArgumentIncomplete())
                                    {
                                        filterRoles(arc, rolesPerNode.getCollection(ipCandidate));
                                        if(applyConllHeuristics) // Apply infinitive marker heuristic, if necessary
                                            applyInfinitiveMarkerHeuristic(arc, anchorNode, iterator, words, origPosTags, operation, true, false);
                                        else
                                        {
                                            // Apply PP heuristic, if neceessary
                                            boolean keepArc = applyPPArgumentHeuristic(arc, tree, anchorNode, ipIdOffset, words, origPosTags, operation, true, false);
                                            // Apply infinitive marker heuristic, if necessary
                                            if(keepArc)
                                                applyInfinitiveMarkerHeuristic(arc, anchorNode, iterator, words, origPosTags, operation, true, false);
                                        }                                        
            //                            setArgument(arc, anchorNode);
                                    }
                                } // if
                            } //for
                        } // if
                    } // if                    
                    else // the shadow sub-tree of the prefix tree doesn't have a role. Proceed with the normal addition of a new dependency
                    {
                        addDependency(new DepNode(ipCandidate, timestamp), tree, (short)(ipCandidate.shortValue() - ipIdOffset), 
                                ipIdOffset, rolesPerNode, shadowTreeRoot, false, words, origPosTags, operation, true, timestamp);
//                        System.out.println("Check! If we never end up here, consider dropping the if statement above");
                    }
                } // if
                // integration points on the elementary tree that are not verifying a shadow sub-tree on the prefix tree.
                // we need to consider it as an incomplete dependency
                else 
                {
                    addIncompleteDependency(ipCandidate, tree, ipIdOffset, false, tree.isRelation(), 
                            rolesPerNode.getCollection(ipCandidate), null, words, origPosTags, operation, true, timestamp);
                }
            } // for all candidate ips
        } // if hasRoles
        // Process the rest of the covered nodes in the shadow subtree of the prefix tree that have a role, hence a dependency arc already observed
        for(Entry<Integer, Integer> e : coveredNodes.entrySet())
        {
            DependencyArc arc;
            Short offsetShadowId = offsetNodeIdsOfShadowTree.get(e.getValue().shortValue());
            if(offsetShadowId != null && !processedNodes.contains(e.getValue() + ipIdOffset))                
            {
                Collection<DependencyArc> arcsByIp = dependencies.getDependenciesByIntegPoint(new DepNode(offsetShadowId.shortValue(), timestamp));
//                if((arc = dependencies.pollArcWithShadowArg(new DepNode(offsetShadowId.shortValue(), timestamp))) != null)
//                if((arc = dependencies.getArcWithShadowArg(new DepNode(offsetShadowId.shortValue(), timestamp))) != null)
//                {
//                    updateDependenciesUnderCoveredNode(tree, coveredNodes, offsetNodeIdsOfShadowTree, timestamp, anchorNode, arc, null, origPosTags);
//                }
//                else if(arcsByIp  != null)
                if(arcsByIp  != null)
                {
//                    for(DependencyArc arcByIp : arcsByIp)
                    Iterator<DependencyArc> iterator = arcsByIp.iterator();
                    while(iterator.hasNext())
                    {
                        DependencyArc arcByIp = iterator.next();
                        updateDependenciesUnderCoveredNode(tree, coveredNodes, offsetNodeIdsOfShadowTree, timestamp, anchorNode, arcByIp, iterator, words, origPosTags);
                    }
                }
            }
        }
        if(tree.isRelation())
        {            
            addHangingRelation(tree, ipIdOffset, words, origPosTags, operation, true, false, timestamp);
        }
        dependencies.postProcessArcs(operation, true, false);
    }
    
    private void updateDependenciesUnderCoveredNode(ElementaryStringTree tree, DualHashBidiMap<Integer, Integer> coveredNodes, 
            DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree, int timestamp, DepNode anchorNode, DependencyArc arc, 
            Iterator<DependencyArc> iterator, String[] words, String[] origPosTags)
    {
        boolean removedArcInfinitiveMarker = false;
        if(tree.isRelation()) 
        {
            if(arc.isRelationIncomplete()) // avoid filling in an already complete relation entry)
            {
                setRelation(arc, anchorNode, iterator, words, origPosTags, "V", true, false);
            }
            if(arc.isArgumentIncomplete()) // fill in the arg by re-using the argument of the integration point's child verified above
            {                            
                if(!fillArgumentFromChildNode(arc, coveredNodes, offsetNodeIdsOfShadowTree, tree, words, origPosTags, timestamp))
                {
                    // as a last resort fill in the anchor (relation) of the verifying tree as an argument.
                    // Apply infinitive marker heuristic, if necessary
                    removedArcInfinitiveMarker = applyInfinitiveMarkerHeuristic(arc, anchorNode, iterator, words, origPosTags, "V", true, false);
//                                setArgument(arc, anchorNode); 
                }
            }
        }
        else if(arc.isArgumentIncomplete()) // avoid filling in an argument with a relation as well as replacing a complete entry
        {
            setArgument(arc, anchorNode, iterator, words, origPosTags, "V", true, false);                
        }
        // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
        if(!removedArcInfinitiveMarker)
        {           
            boolean keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); 
            if(!keepArc)
            {
                removeArcSafe(arc, arc.getIntegrationPoint(), iterator);                
            }
        }        
    }
    
    private void updateDependency(DepNode integrationPoint, Node shadowTreeRootNode, ElementaryStringTree tree, 
            short offset, boolean isShadowTree, String[] words, String[] origPosTags, 
            String operation, boolean direction, boolean shadow, int timestamp)
    {
        Collection<DependencyArc> arcs = dependencies.getDependenciesByIntegPoint(integrationPoint);
        Iterator<DependencyArc> iterator = arcs.iterator();
//        for(DependencyArc arc : arcs)
        while(iterator.hasNext())
        {
            DependencyArc arc = iterator.next();
            if(arc.isIncomplete())
            {
                DepNode anchorNode = getAnchorNode(tree, offset, isShadowTree, integrationPoint, timestamp);
                if(tree.isRelation() && arc.isRelationIncomplete()) // avoid filling in an already complete relation entry
                {
                    setRelation(arc, anchorNode, iterator, words, origPosTags, operation, direction, shadow);
                    // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
                    boolean keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); 
                    if(!keepArc)
                        removeArcSafe(arc, arc.getIntegrationPoint(), iterator);
                }
                else if(arc.isArgumentIncomplete())
                {
                    applyInfinitiveMarkerHeuristic(arc, anchorNode, iterator, words, origPosTags, operation, direction, shadow);
//                    setArgument(arc, anchorNode, iterator, origPosTags, operation, direction, shadow);
                    // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
                    boolean keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); 
                    if(!keepArc)
                        removeArcSafe(arc, arc.getIntegrationPoint(), iterator);
                }
                // update map with arcs that have a shadow tree as an argument
                if(isShadowTree)
                {
                    arc.setShadowTreeRootNode(shadowTreeRootNode);
                    dependencies.addShadowArgument(anchorNode, arc);
                }
            }
        }
    }
    
    /**
     * Add dependency with integration point on the elementary tree.
     * @param integrationPoint
     * @param tree
     * @param offset
     * @param isRelation
     * @param rolesPerNode 
     */
    private void addDependency(DepNode integrationPoint, ElementaryStringTree tree, short adjoiningNodeInElemTreeId, short offset,
            MultiValueMap<Integer, Role> rolesPerNode, Node prefixTreeNode, boolean isShadowTree, String[] words, String[] origPosTags, 
            String operation, boolean direction, int timestamp)
    {        
        boolean isRelation = tree.isRelation();
//        integrationPoint.setCategory(tree.getCategory(integrationPoint.getId() - offset));
        integrationPoint.setCategory(tree.getCategory(adjoiningNodeInElemTreeId));        
//        DependencyArc arc = new DependencyArc(integrationPoint, rolesPerNode.getCollection(integrationPoint.getId()), null, null);
        DependencyArc arc = new DependencyArc(integrationPoint, rolesPerNode.getCollection(adjoiningNodeInElemTreeId + offset), null, null);
//        DependencyArc arc = newDependencyArc(integrationPoint, rolesPerNode.getCollection(adjoiningNodeInElemTreeId + offset), tree, isRelation);
        DepNode anchorNode = !isShadowTree ? getAnchorNode(tree, offset, timestamp) : null;
        // if integration point has common ancestor with existing dependency 
        // which is missing a relation/argument (depends on whether tree is an argument/relation) [AT the moment only attach to a hanging relation]
        DepNode relationWithMatchingAnchor = prefixTreeNode != null ? dependencies.getRelationWithCategory(getNodeCategory(prefixTreeNode), isRelation) : null;
//        if(isRelation && relationWithMatchingAnchor != null)
            //System.out.println("Need to check " + tree);      
        boolean mergedArc = false, ppMergedArc = false, keepArc = true;
        if(isRelation) // rel is anchor
        {
            mergedArc = setRelation(arc, anchorNode, words, origPosTags, operation, direction, isShadowTree);            
            if(prefixTreeNode != null)// arg is head of prefix tree
            {                
                mergedArc = setArgument(arc, new DepNode(getNodeCategory(prefixTreeNode), prefixTreeNode.getCategory(), // TODO: check getCategory() always return POS tag
                        prefixTreeNode.getNodeId(), prefixTreeNode.getOrigTree(), prefixTreeNode.getLambdaTimestamp()), 
                        words, origPosTags, operation, direction, isShadowTree);
            }            
            ppMergedArc = applyPPArgumentHeuristic(arc, tree, anchorNode, offset, words, origPosTags, operation, direction, isShadowTree);
            // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
            keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); 
        }
        else
        {   // arg is anchor (e.g. tree for 'will')
//            if(tree.isAuxtree() && !tree.hasFootLeft())
//            if(tree.isAuxtree() && (isShadowTree ? true : !isTreePP(tree, anchorNode, offset)))
            if(tree.isAuxtree())
            {                
                if(applyConllHeuristics || !(anchorNode == null || isTreePP(tree, anchorNode, offset) && arc.isArgumentIncomplete()))
                {
                    mergedArc = setArgument(arc, anchorNode, words, origPosTags, operation, direction, isShadowTree);
                }
                else
                {
                    ppMergedArc = isShadowTree ? setArgument(arc, anchorNode, words, origPosTags, operation, direction, isShadowTree) :
                        applyPPArgumentHeuristic(arc, tree, anchorNode, offset, words, origPosTags, operation, direction, isShadowTree);
                }
//                keepArc = true;
            }
            else if(prefixTreeNode != null)// arg is head of prefix tree
            {
                mergedArc = setArgument(arc, new DepNode(getNodeCategory(prefixTreeNode), prefixTreeNode.getCategory(), 
                        prefixTreeNode.getNodeId(), prefixTreeNode.getOrigTree(), prefixTreeNode.getLambdaTimestamp()), 
                        words, origPosTags, operation, direction, isShadowTree);
//                keepArc = true;
            }
            if(relationWithMatchingAnchor != null)
            {
                mergedArc = setRelation(arc, relationWithMatchingAnchor, words, origPosTags, operation, direction, isShadowTree);
                keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); // possibly created a complete arc, so we can disambiguate role labels discriminatively
            }
        }    
        if(!(integrationPoint.category.equals(":") || mergedArc || ppMergedArc || !keepArc)) // don't add trace; we will deal with it when we encounter the verb (late assignment)
            addArc(arc);
    }
    
    private void addIncompleteDependencies(ElementaryStringTree tree, short offset, boolean isShadowTree, boolean isRelation, 
            MultiValueMap<Integer, Role> rolesPerNode, Node prefixTreeNode, String[] words, String[] origPosTags, 
            String operation, boolean direction, int timestamp)//, Set<DepNode> excludeIntegPoints)
    {              
        for(Integer nodeId : rolesPerNode.keySet())
        {
            addIncompleteDependency(nodeId, tree, offset, isShadowTree, isRelation, rolesPerNode.getCollection(nodeId), prefixTreeNode, 
                    words, origPosTags, operation, direction, timestamp);
        }
    }
    
    private void addIncompleteDependency(int nodeId, ElementaryStringTree tree, short offset, boolean isShadowTree, boolean isRelation, 
            Collection<Role> roles, Node prefixTreeNode, String[] words, String[] origPosTags, String operation, boolean direction, int timestamp)//, Set<DepNode> excludeIntegPoints)
    {                       
        DepNode integrationPoint = new DepNode(tree.getCategory(nodeId - offset), null, nodeId, timestamp, isShadowTree);        
        DepNode anchorNode = isShadowTree ? null : getAnchorNode(tree, offset, timestamp);
//            dependencies.addArc(integrationPoint, rolesPerNode.getCollection(nodeId), !isRelation ? anchorNode : null, isRelation ? anchorNode : null);
        DependencyArc arc = new DependencyArc(integrationPoint, roles, null, null);
//        DependencyArc arc = newDependencyArc(integrationPoint, roles, tree, isRelation);
        // if integration point has common ancestor with existing dependency 
        // which is missing a relation/argument (depends on whether tree is an argument/relation) [AT the moment only attach to a hanging relation]
        DepNode relationWithMatchingAnchor = prefixTreeNode != null ? dependencies.getRelationWithCategory(getNodeCategory(prefixTreeNode), isRelation) : null;
//        if(isRelation && relationWithMatchingAnchor != null)
//            System.out.println("Need to check " + tree);
        boolean mergedArc = false, ppMergedArc = false, keepArc = true;
        if(!isShadowTree && (isRelation || relationWithMatchingAnchor != null))
//        if((!isShadowTree && (isRelation || relationWithMatchingAnchor != null)) || (isShadowTree && relationWithMatchingAnchor != null))
        {
            mergedArc = setRelation(arc, isRelation ? anchorNode : relationWithMatchingAnchor, words, origPosTags, operation, direction, isShadowTree);
            if(!isShadowTree)
                ppMergedArc = applyPPArgumentHeuristic(arc, tree, anchorNode, offset, words, origPosTags, operation, direction, isShadowTree);
//            keepArc = true;
            int candPPSubcatAnchor = tree.getSubcategorisedAnchor();
            if(candPPSubcatAnchor > Integer.MIN_VALUE) // possible subcategorised PP anchor in the same sub-tree with the integration point
            {
                Integer parentId = tree.getParent(nodeId - offset);
                if(parentId != null)
                {
                    String parentCategory = tree.getCategory(parentId);
                    if(parentCategory != null && parentCategory.equals("PP"))
                    {
                        String category = tree.getCategory(candPPSubcatAnchor);
                        String posTag = tree.getCategory(tree.getParent(candPPSubcatAnchor));
                        DepNode ppNode = new DepNode(category, posTag, candPPSubcatAnchor+offset, tree.toStringUnlex(false), 
                                findTimestampOfWord(words, timestamp, category));
                        mergedArc = setArgument(arc, ppNode, words, origPosTags, operation, direction, isShadowTree);
                        if(!applyConllHeuristics && !mergedArc && keepArc)
                        {
                            // Store the sister node of the preposition, which is either a foot or a substitution node.
                            // This is going to be used in the head-finding algorithm to fill in the triple with the head of the PP,
                            // rather than the prepopsition, which is the default for CoNLL.
                            List<Integer> ppChildren = tree.getChildren(tree.getParent(tree.getParent(candPPSubcatAnchor)));
                            if(ppChildren.size() > 1)
                            {                    
                                Node ppIntegrationPoint = findNodeInFringeById((short)(ppChildren.get(ppChildren.size() - 1) + offset));
                                arc.setShadowTreeRootNode(ppIntegrationPoint);                                
                                dependencies.addShadowArgument(ppNode, arc);
                            }                
                        }        
                    }
                }
            }
        }
        else
        {
            if(anchorNode != null) //&& !isTreePP(tree, anchorNode, offset))
            {
                mergedArc = setArgument(arc, anchorNode, words, origPosTags, operation, direction, isShadowTree);
                if(!applyConllHeuristics && !isShadowTree)
                    ppMergedArc = applyPPArgumentHeuristic(arc, tree, anchorNode, offset, words, origPosTags, operation, direction, isShadowTree);
                keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); // possibly created a complete arc, so we can disambiguate role labels discriminatively
            }
//            else
//            {
//                if(relationWithMatchingAnchor == null)
//                    System.out.println("Need to double check - adding dependency arc witout relation and argument [Up Adj Shadow]");
//            }
        }
        if(!(integrationPoint.category.equals(":") || mergedArc || ppMergedArc || !keepArc)) // don't add trace; we will deal with it when we encounter the verb (late assignment) 
            addArc(arc);        
    }
        
    /**
     * Grab argument information from the (potentially argument-complete) subtree rooted on the child node
     * that is attaching on the integration point of <code>arc</code>. We search through the dependencies
     * we have already encountered.
     * @param arc
     * @param coveredNodes
     * @param offsetNodeIdsOfShadowTree
     * @param tree
     * @param timestamp
     * @return 
     */
    private boolean fillArgumentFromChildNode(DependencyArc arc, DualHashBidiMap<Integer, Integer> coveredNodes, 
            DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree, ElementaryStringTree tree, String[] words, String[] origPosTags, int timestamp)
    {
        boolean filled = false;
        if(arc.getArgument() != null) // TODO Investigate
        {
            int idOfArgOnPrefix = arc.getArgument().getId();
            Short idOfArgOnShadow = offsetNodeIdsOfShadowTree.getKey((short)idOfArgOnPrefix);
            if(idOfArgOnShadow != null)
            {
                
                Integer idOfArgOnVerif = coveredNodes.get((int)idOfArgOnShadow);
                if(idOfArgOnVerif != null)
                {
                    for(int childId : tree.getChildren(idOfArgOnVerif))
                    {
                        Integer childIdOnShadow = coveredNodes.getKey(childId);
                        if(childIdOnShadow != null)
                        {
                            Collection<DependencyArc> childArcs = 
                                    dependencies.getDependenciesByIntegPoint(new DepNode(
                                    offsetNodeIdsOfShadowTree.get(childIdOnShadow.shortValue()), timestamp));
                            if(childArcs != null)                        
                            {
                                Iterator<DependencyArc> iterator = childArcs.iterator();
                                while(iterator.hasNext())
                                {
                                    DependencyArc childArc = iterator.next();
                                    if(!childArc.isArgumentIncomplete()) // make sure the dependency has argument information
                                    {
                                        setArgument(arc, childArc.getArgument(), iterator, words, origPosTags, "V", true, false);
                                        filled = true;
                                    }                                
                                } // if
                            } // for
                        }                    
                    } // for
                } // if
            }            
        } // if
        return filled;
    }
    
//    private void fillShadowArgument(Node prefixTreeNode, ElementaryStringTree tree, short offset, String[] origPosTags, int timestamp)
//    {
//        DependencyArc arc;
//        if(dependencies.arcsWithShadowArgIsEmpty())
//            return;
//        if(prefixTreeNode.getCategory().equals(tree.getCategory(tree.getRoot())) &&
//            !((arc = dependencies.pollArcWithShadowArg(new DepNode(prefixTreeNode.getNodeId(), timestamp + 1))) == null || arc.isRelationIncomplete()))
//        {
//            setArgument(arc, getAnchorNode(tree, offset, timestamp), origPosTags);
//        }
//    }
    
    /**
     * Checks whether the integration point on the prefix tree has a role. It looks
     * up the input node on the set of existing dependencies added previously. Returns
     * true if the node has already been added as part of a dependency arc earlier.
     * @param ip Node of the integration point on the prefix tree
     * @return true if the input Node ip contains role(s)
     */
    private boolean prefixIntegrationPointHasRole(DepNode ip)
    {
        return dependencies.containsDependencyByIntegPoint(ip);
    }
    
    /**
     * Checks whether the integration point on the elementary tree has a role.
     * @param tree an ElementaryStringTree with potential roles attached to it
     * @param rolesPerNode a Map of nodes with their roles
     * @param integrationPointId integration point id
     * @param ipIdOffset the node id offset. This is the highest node id of the prefix tree.
     * @return 
     */
    private boolean elemIntegrationPointHasRole(ElementaryStringTree tree, MultiValueMap<Integer, Role> rolesPerNode, int integrationPointId)
    {        
        return tree.hasRoles() && rolesPerNode.containsKey(integrationPointId);
    }
    
    /**
     * We store each verb/relation separately in the Dependencies class for future use.
     * We also apply verb-related heuristics, such as trace attachment (late assignment), 
     * relative pronoun roles (R-A1, R-A0), hanging modifiers, and coordinated arguments.
     * @param tree
     * @param offset
     * @param timestamp 
     */
    private void addHangingRelation(ElementaryStringTree tree, short offset, String[] words, String[] origPosTags, 
            String operation, boolean direction, boolean shadow, int timestamp)
    {
        DepNode relation = getAnchorNode(tree, offset, timestamp);
        dependencies.addHangingRelation(relation);
        // add trace filler (late assignment. Add filler when encountering verb)
        if(!dependencies.npHeadsIsEmpty()) // found a candidate trace-filler
        {
            // check whether it is an argument trace (cat=*) or an argument-bar trace (cat=*T*)            
            TraceInfo traceInfo = findTrace(tree);
            if(traceInfo != null) // if tree has a trace
            {
                DepNode traceNode = traceInfo.node;
                String traceType = traceNode.category;
                String parentCategory = traceInfo.parentCategory;
                // argument traces, or argument-bar traces that fill an NP clause (e.g., subj below subordinate clause)
                if(traceType.equals("*") || (parentCategory != null && traceType.equals("*T*") && parentCategory.equals("NP")))
                {                
                    Node filler = dependencies.peekFirstNpHead();
                    if(filler != null)
                    {
                        DepNode argument = new DepNode(getNodeCategory(filler), filler.getCategory(), filler.getNodeId(), filler.getLambdaTimestamp()); // TODO: check getCategory() returns POS
                        DependencyArc arc = new DependencyArc(traceNode, traceInfo.roles, null, null);
                        List<DependencyArc> updatedArcs = arc.setArgument(argument);
                        arc.setRelation(relation);                        
                        for(DependencyArc updatedArc : updatedArcs)
                            updateArcFeatures(updatedArc, origPosTags, operation, direction, shadow);
                        boolean keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); // possibly created a complete arc, so we can disambiguate role labels discriminatively
    //                    arc.integrationPointIncr();
                        if(keepArc)
                            addArc(arc);
                    }                                        
                    // add relative pronoun args for traces R-A0, R-A1
                    DepNode currentRelative = dependencies.getCurrentRelative();
                    if(currentRelative != null)
                    {                                                
                        List<Role> roles = new ArrayList<Role>();
                        for(Role role : traceInfo.roles)
                        {                            
                            roles.add(new Role(role.roleIndexer, role.roleIndexer.getIndex(String.format("R-%s", role.toString())), -1000));                         
                        }
                        DependencyArc relArc = new DependencyArc(new DepNode(traceNode), roles, currentRelative, relation);
//                        relArc.integrationPointIncr();
                        // make sure first we don't keep duplicate arcs with identical argument/predicates
                        List<Pair<DepNode, DependencyArc>> list = dependencies.getArcsWithArgumentRelation(currentRelative, relation);
                        if(!list.isEmpty())
                        {
                            for(Pair<DepNode, DependencyArc> entry : list)
                            {
                                dependencies.removeArc(entry.getFirst(), entry.getSecond());
                            }
                        }
                        // then add R-A0, R-A1 arc
                        updateArcFeatures(relArc, origPosTags, operation, direction, shadow);
                        addArc(relArc);
                        dependencies.removeCurrentRelative();                        
                    } // if                    
                } // if
            } // if                        
        } // if
        // hack: attach relation to the most recent (below the same S-clause) hanging arc with modifier
        if(!dependencies.hangingModifiersIsEmpty())
        {
            DependencyArc modArc = new DependencyArc(dependencies.peekFirstHangingModifier());
            boolean mergedArc = setRelation(modArc, relation, words, origPosTags, operation, direction, shadow);
            // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
            boolean keepArc = identifyArcAndDisambiguateRoles(model, modArc, words, origPosTags); 
            if(!mergedArc && keepArc)
                addArc(modArc);
            // remove after use (unlike trace fillers, it should not be re-used unless in the context of a coordinator.
            // In that case we should copy-paste the modifier along with the rest arguments.
            dependencies.popHangingModifier(); 
        }
        // hack: in case of verb coordination copy-paste dependencies from left verb
        Pair<String, String> currentCoord = dependencies.getCurrentCoord();
        if(currentCoord != null)
        {
//            String coord = currentCoord.getSecond();
            List<DependencyArc> list = dependencies.getArcsWithRelation(currentCoord.getFirst());
            Iterator<DependencyArc> iterator = list.iterator();
            while(iterator.hasNext())
            {
                DependencyArc arc = iterator.next();
//                if(coord.equals("and"))
//                {
//                    if(arc.containsRole("ARG0") || arc.containsRole("ARGM"))
//                        addCoordinatedArc(arc, relation);
//                }
//                else if(coord.equals("or")) // add everything (correct??)
//                {
//                    addCoordinatedArc(arc, relation);
//                }
                //  we definitely want to copy A0 info. We also copy any incomplete
                // relation. If it is complete then its scope ends before the second verb
                if(arc.containsRole("ARG0") || arc.containsRole("ARGM") || arc.isIncomplete())
                {
                    addCoordinatedArc(arc, relation, origPosTags, operation, direction, shadow);
                    // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
                    boolean keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); 
                    if(!keepArc)
                        removeArcSafe(arc, arc.getIntegrationPoint(), iterator);
                }
            }
            // remove currentCoord. If we need to re-use it then it should be added when we encounter a new coordinate structure
            dependencies.removeCurrentCoord();
        } // if
//        // attach auxiliary information (already stored) to verb
//        if(dependencies.getCurrentAuxVerb() != null)
//            dependencies.addAuxVerbNode(relation.category);
    }        
    
    /**
     * Create a map of nodes that have a role. Note, we split role signatures into 
     * individual nodes with roles; this might potentially result in combinations of roles
     * in trees not existing in the training set.
     * @param tree an ElementaryStringTree with potential roles attached to it
     * @param offset the node id offset. This is the highest node id of the prefix tree. 
     * @return 
     */
    private MultiValueMap<Integer, Role> getRolesPerNode(ElementaryStringTree tree, short offset)
    {
        MultiValueMap<Integer, Role> roleMap = new MultiValueMap();
        for(RoleSignature sig : tree.getRoleSignatures())
        {            
            for(Role role : sig.getRoles().values()) // wrong! It might result in combination of roles in trees not existing in the training set
            {           
                int newId = role.getNodeId() + offset;
                Collection<Role> rolesPerNode = roleMap.getCollection(newId);
                if(rolesPerNode == null || !rolesPerNode.contains(role)) // if the collection is empty and doesn't contain the same role
                    roleMap.put(newId, role);
                if(role.secondRoleNameId != null) // make sure we treat roles with multiple argument labels separately (e.g., @ARG1;@ARG0)
                {
                    Role secondRole = new Role(role.roleIndexer, role.secondRoleNameId, role.nodeId);
                    if(!roleMap.getCollection(newId).contains(secondRole))
                        roleMap.put(newId, secondRole);
                }
            }         
        } 
        return roleMap;
    }
    
    /**
     * Create a DepNode based on the anchor of an elementary tree
     * @param tree an ElementaryStringTree with potential roles attached to it
     * @param offset the node id offset. This is the highest node id of the prefix tree.
     * @param isShadowTree boolean flag set to true of the tree is a shadow tree. 
     * @param integrationPoint the integration point node on the prefix tree
     * @return  a DepNode wrapper of the lexical anchor in the elementary tree
     */
    private DepNode getAnchorNode(ElementaryStringTree tree, short offset, boolean isShadowTree, DepNode integrationPoint, int timestamp)
    {
        // use the ip on the prefix tree as the node id, but hack the category from the adjoining node on the shadow tree which is its root.
        if(isShadowTree)
        {
            int anchorId = integrationPoint.getId();
            return new DepNode(tree.getCategory(tree.getRoot()), null, anchorId, timestamp, isShadowTree, tree.toStringUnlex(false));
//            return new DepNode(tree.getCategory(tree.getRoot()), null, anchorId, timestamp, isShadowTree);
//            int anchorId = tree.getMainLeafId(tree.getRoot());
//            return new DepNode(tree.getCategory(anchorId), offset + anchorId, isShadowTree);
        }
        else
        {
            int anchorId = tree.getAnchor();                
            Integer parent = tree.getParent(anchorId);
//            return new DepNode(tree.getCategory(anchorId), parent > Integer.MIN_VALUE ? tree.getCategory(parent) : null, offset + anchorId, timestamp, isShadowTree);
            return new DepNode(tree.getCategory(anchorId), parent > Integer.MIN_VALUE ? tree.getCategory(parent) : null, 
                    offset + anchorId, timestamp, isShadowTree, tree.toStringUnlex(false));
        }        
    }
    
    private DepNode getAnchorNode(ElementaryStringTree tree, short offset, int timestamp)
    {
        int anchorId = tree.getAnchor();
        Integer parent = tree.getParent(anchorId);
//        return new DepNode(tree.getCategory(anchorId), parent > Integer.MIN_VALUE ? tree.getCategory(parent) : null, offset + anchorId, timestamp);
        return new DepNode(tree.getCategory(anchorId), parent > Integer.MIN_VALUE ? tree.getCategory(parent) : null, 
                offset + anchorId, tree.toStringUnlex(false), timestamp);
    }
    
    /**
     * Create a ShadowDepNode based on the whole input prediction tree. We keep track of the root id of the tree.
     * @param tree
     * @param offset
     * @return 
     */
    private ShadowDepNode getAnchorNode(ShadowStringTree tree, short offset)
    {
        int anchorId = tree.getTreeOrigIndex().getRoot();
        return new ShadowDepNode(offset + anchorId, tree);
    }

    /**
     * Sets a relation for the arc. Then if the arc is complete it checks whether it
     * can merge it with an existing arc. If this is positive, the method returns
     * a value of true. The (optionally) merged arc is sent to a pipeline of classifiers
     * to identify whether it is a good candidate arc, and disambiguate (if necessary)
     * the roles assigned to it.
     * @param arc
     * @param relation
     * @param origPosTags
     * @return 
     */
    private boolean setRelation(DependencyArc arc, DepNode relation, String[] words, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {
        return setRelation(arc, relation, null, words, origPosTags, operation, direction, shadow);
    }
    
    /**
     * Sets a relation for the arc. Then if the arc is complete it checks whether it
     * can merge it with an existing arc. If this is positive, the method returns
     * a value of true. The (optionally) merged arc is sent to a pipeline of classifiers
     * to identify whether it is a good candidate arc, and disambiguate (if necessary)
     * the roles assigned to it.
     * @param arc
     * @param relation
     * @param iterator
     * @param origPosTags
     * @return 
     */
    private boolean setRelation(DependencyArc arc, DepNode relation, Iterator<DependencyArc> iterator, 
            String[] words, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {
        arc.setRelation(relation);
        updateArcFeatures(arc, origPosTags,operation, direction, shadow);        
        DependencyArc mergedArc = mergeWithExistingArc(arc, iterator); // existing arc is always complete
        if(mergedArc != null)
        {
            // we might potentially discard the merged arc if the classifier says so
            boolean keepMergedArc = identifyArcAndDisambiguateRoles(model, mergedArc, words, origPosTags);
            if(!keepMergedArc)
                removeArcSafe(mergedArc, mergedArc.getIntegrationPoint(), null);
            return true;
        }
        return false;
    }
    
    /**
     * Sets an argument for the arc. This process automatically updates any 
     * coordinated arcs. Then for each affected complete arc it checks whether it
     * can merge it with an existing arc. If this is positive, the method returns
     * a value of true. The (optionally) merged arc is sent to a pipeline of classifiers
     * to identify whether it is a good candidate arc, and disambiguate (if necessary)
     * the roles assigned to it.
     * @param arc
     * @param argument
     * @param origPosTags
     * @return 
     */
    private boolean setArgument(DependencyArc arc, DepNode argument, String[] words, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {
        return setArgument(arc, argument, null, words, origPosTags, operation, direction, shadow);
    }
    
    /**
     * Sets an argument for the arc. This process automatically updates any 
     * coordinated arcs.
     * Then for each affected complete arc it checks whether it
     * can merge it with an existing arc. If this is positive, the method returns
     * a value of true. The (optionally) merged arc is sent to a pipeline of classifiers
     * to identify whether it is a good candidate arc, and disambiguate (if necessary)
     * the roles assigned to it.
     * @param arc
     * @param argument
     * @param origPosTags
     * @return 
     */
    private boolean setArgument(DependencyArc arc, DepNode argument, Iterator<DependencyArc> iterator, 
            String[] words, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {
        boolean mergedArcFlag = false;        
        List<DependencyArc> updatedArcs = arc.setArgument(argument);
        for(DependencyArc updatedArc : updatedArcs)     
        {
            updateArcFeatures(updatedArc, origPosTags, operation, direction, shadow);
            DependencyArc mergedArc = mergeWithExistingArc(updatedArc, iterator); // existing arc is always complete
            if(mergedArc != null)
            {
                // we might potentially discard the merged arc if the classifier says so
                boolean keepMergedArc = identifyArcAndDisambiguateRoles(model, mergedArc, words, origPosTags);
                if(!keepMergedArc)
                    removeArcSafe(mergedArc, mergedArc.getIntegrationPoint(), null);
                mergedArcFlag = true;
            }
        }        
        return mergedArcFlag;
    }
    
    private DependencyArc addArc(DependencyArc arc)
    {
        return dependencies.addArc(arc);
    }        
    
    /**
     * Merge incoming arc with an existing identical (same argument, relation)
     * arc attached to a different integration point.
     * @param arc
     * @return 
     */
    private DependencyArc mergeWithExistingArc(DependencyArc arc, Iterator<DependencyArc> iterator)
    {
        DepNode integPoint = (DepNode) arc.getIntegrationPoint();
        List<Pair<DepNode, DependencyArc>> arcs = dependencies.getArcsWithArgumentRelation(arc.getArgument(), arc.getRelation());        
        if(arcs != null)
        {
            for(Pair<DepNode, DependencyArc> candPair : arcs)
            {                
                if(!candPair.getFirst().equals(integPoint))
                {
                    DependencyArc candArc = candPair.getSecond();
                    candArc.addRoles(arc.getRoles());
                    removeArcSafe(arc, integPoint, iterator);
                    return candArc;
                }
            }
        }
        return null;
    }
    
    private void removeArcSafe(DependencyArc arc, DepNode integPoint, Iterator<DependencyArc> iterator)
    {
        if(iterator != null)
        {
            try
            {
                iterator.remove();
            }
            catch(IllegalStateException e)
            {
                // suppress exception as the arc has already been removed. TODO: fix in the future, as this is not a clean solution
            }
            catch(ConcurrentModificationException e)
            {
                
            }
        }
        else                        
            dependencies.removeArc(integPoint, arc);        
    }
    
    /**
     * Use a discriminative model to classify whether this arc should be added or not.
     * Then use another multi-label classifier to choose the most suitable role for this arc.
     * Return true if the classifier decides we want to keep this arc.     
     * @param model
     * @param arc     
     */
    private boolean identifyArcAndDisambiguateRoles(ParserModel model, DependencyArc arc, String[] words, String[] origPosTags)
    {
        applyAuxVerbHeuristic(arc, arc.getArgument());
        DepNode prevArgPost = applyPostHonorificHeuristic(arc, words, origPosTags);        
        if(model.getOpts().trainClassifiers || !model.getOpts().useClassifiers || arc.getRoles().size() == 1)
        {
            return true;
        }
        if(!(arc == null || arc.isIncomplete()))
        {
//            DepNode prevArgPrep = !applyConllHeuristics ? replaceArgumentWithPrep(arc) : null;
            ArgumentClassifier argumentClassifier = model.getArgumentClassifier();
            Pair<FeatureVec, FeatureVec> vecs = arc.extractPairOfFeatures(argumentClassifier);
            boolean keepArc = arc.identifyArc(argumentClassifier, vecs.getFirst());
            if(keepArc)
            {
                arc.disambiguateRoles(argumentClassifier, vecs.getSecond());
//                if(prevArgPrep != null)
//                    arc.setArgument(prevArgPrep);
                if(!applyConllHeuristics && prevArgPost != null)
                    arc.setArgument(prevArgPost);
            }
            return keepArc;
        }     
        return true;
    }
    
    private void updateArcFeatures(DependencyArc arc, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {        
        if(!arc.isIncomplete()) 
        {
            // hack: we have to replacrc.ge pos tags of relation/argument with the original from
            // the gold standard. The parser loses this information (VB* become VB and NN* becomes NN) when reading in the input.
            DepNode node = arc.getRelation();
            int relTimestamp = node.timestamp;
            if(relTimestamp != -99 && origPosTags != null)
                node.setPosTag(origPosTags[relTimestamp]);
            node = arc.getArgument();
            int argTimestamp = node.timestamp;
            if(argTimestamp != -99 && origPosTags != null)
            {
                if(argTimestamp < origPosTags.length)
                    node.setPosTag(origPosTags[argTimestamp]);
            }
            
            // add current prefix tree fringe in a compressed format: keep the nodes in a lexicographically ordered set (same technique as in supertagging)            
            arc.setPrefixFringeAlphaSet(fringe);
            
            // update operation path if the arc is complete and the argument/predicate are adjacent words
            if(relTimestamp != -99 && argTimestamp != -99 && Math.abs(relTimestamp-argTimestamp) == 1)
            {
                arc.updateOperationPath(operation, direction, shadow);
            }
        }
    }
    public Dependencies getDependencies()
    {
        return dependencies;
    }       

    /**
     * Retain in the dependency arc only the roles that are in the intersection of the
     * roles already in the arc and in the <code>rolesIn</code> Collection
     * @param arc
     * @param rolesIn 
     */
    private void filterRoles(DependencyArc arc, Collection<Role> rolesIn)
    {
//        for(Iterator<Role> it = arc.getRoles().iterator(); it.hasNext();)        
//        {
//            if(!rolesIn.contains(it.next()))
//            {
//                it.remove();
//            }
//        }       
        Collection<Role> roles = arc.getRoles();
        roles.retainAll(rolesIn);
        // if there are no roles in the intersection of both sets, 
        // add all the roles from the verification tree (not so accurate)
        if(roles.isEmpty()) 
            roles.addAll(rolesIn);
    } 
    /**
     * House-keep information from the elementary trees that are going to be heuristically used
     * later on to correct semantic role labeling (e.g., identify infinitive marker and re-assign ARGX to 'to'
     * instead of the verb)
     * @param tree
     * @param elemTreeOffset
     * @param shadowTree
     * @param timestamp 
     */
    private void retainInfoFromTreeHeuristics(TreeState elemTreeState, Node prefixTreeNode, ElementaryStringTree tree, 
            short elemTreeOffset, boolean shadowTree, int timestamp, boolean verification, DualHashBidiMap<Integer, Integer> coveredNodes, DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree)
    {
        String rootCategory = tree.getCategory(tree.getRoot());
//        elemTreeState.shiftNodesBy(elemTreeOffset);
                
//        System.out.println("timestamp="+timestamp + " " + tree.toString() +"\t" + prefixTreeNode + "\n" + 
//                fringe.toString() + ";" + futureFringe.toString() + "\t ||| \t" + 
//                elemTreeState.getFringe().toString() + ";" + elemTreeState.getFutureFringe().getFringe() + "\n");
//                      
        boolean npHeadsWasEmpty = dependencies.npHeadsIsEmpty();
        int npHeadsSizeBefore = dependencies.npHeadsSize();
        CandidateType type = findCandidateNodesInFringe(tree, elemTreeOffset, elemTreeState, prefixTreeNode, 
                rootCategory, verification, coveredNodes, offsetNodeIdsOfShadowTree, timestamp);
        if(type != CandidateType.none)
        {
            // if it wasn't the first S-NP-SBJ subtree, or we added an np to the existing stack, or found a trace
            if(!npHeadsWasEmpty && (npHeadsSizeBefore != dependencies.npHeadsSize() || type == CandidateType.trace))
                dependencies.popHangingModifier(); // new scope added, so we need to remove the modifier
//            if(verification)
//                System.out.println("Verification");
//            System.out.println("timestamp="+timestamp + " " + tree.toString() + "\n");
        }                
        if(!shadowTree)
        {
            int terminalId = tree.getAnchor();
            int preTerminalId = tree.getParent(terminalId);
            String preTerminalCategory = tree.getCategory(preTerminalId);
            Integer parentId = tree.getParent(preTerminalId);
            String parentCategory = parentId != null ? tree.getCategory(parentId) : "N/A";
            
            // infinitive marker heuristic: make head of tree the infinitive marker instead of the VP
            // identify infinitive marker tree looks like: ( VP^null_x ( TO^x_x to<>) VP^x_null*)
            if(preTerminalCategory.equals("TO") && parentCategory.equals("VP") && tree.isAuxtree())
            {                
                // add candidate infinitive marker foot node. Hack: we add the timestamp of 'to'
                dependencies.addInfinitiveMarkerNode(new DepNode(tree.getCategory(terminalId), preTerminalCategory, 
                        terminalId + elemTreeOffset, tree.toStringUnlex(false), timestamp));                 
            }
            
            // auxiliary verb heuristic: make head of tree the auxilliary verb instead of the following verb, 
            // e.g., 'Peter is (head) interested in learning', 'We 've (head) got', 'it couldn't (head) provide'
            if(isAuxiliary(preTerminalCategory))
            {              
                // set only an empty aux verb, i.e., don't update. E.g., could (head) have come
                if(dependencies.getCurrentAuxVerb() == null)
                    dependencies.setCurrentAuxVerb(new DepNode(tree.getCategory(terminalId), preTerminalCategory, 
                            terminalId + elemTreeOffset, tree.toStringUnlex(false), timestamp));                 
            }
            // attach auxiliary information (already stored by the above if-clause in a previous step) to verb
            if(tree.isRelation() && dependencies.getCurrentAuxVerb() != null)
            {
                dependencies.addAuxVerbNode(tree.getCategory(terminalId));
            }
            // verb coordination heuristic: keep track of the left verb adjunct and the terminal of the coordinator
            // verb coordination tree looks like : ( VP^null_x VP^x_null* ( CC^x_x or<>) VP^x_null!)
            if(preTerminalCategory.equals("CC"))
            {
                String terminalCategory = tree.getCategory(terminalId);
//                if(tree.printSimple(1, true).equals(String.format("(VP VP (CC %s) VP)", terminalCategory)) && tree.isAuxtree())
                if(tree.isAuxtree() && tree.getCategory(tree.getFoot()).equals("VP"))
                {                    
                    if(prefixTreeNode.getCategory().equals("VP")) // make sure we are attaching to the left verb adjunct
                    {
                        String leftVerbCategory = getNodeCategory(prefixTreeNode);
                        dependencies.setCurrentCoord(new Pair<String, String>(leftVerbCategory, terminalCategory));
                    }
                }
            }
        }
        // head finding heuristic: arcs that contain a shadow argument, get updated with anchors of non-modifier trees.
        if(!applyConllHeuristics)
            dependencies.removeDeadArcsWithShadowArg();
        if(!dependencies.arcsWithShadowArgIsEmpty()) // update arg information rooted on a shadow tree
        {
            Iterator<DependencyArc> it = dependencies.getArcsWithShadowArg().iterator();
            while(it.hasNext())
            {
                DependencyArc arc = it.next(); 
                if(arc != null)
                {
                    // if the tree does not contain a foot node then it's the head of the subtree rooted on the shadow tree arg
                    if(!tree.isAuxtree() && (!applyConllHeuristics || dependencies.infinitiveMarkerIsEmpty()) &&                          
                            arc.getArgument() != null && //!dependencies.containsAuxVerb(arc.getArgument().category) && 
                            (!applyConllHeuristics || !isCategoryPP(arc.getArgument().posTag)))
                    {
                        int anchor = tree.getAnchor();
                        String category = tree.getCategory(anchor);
                        if(!category.equals(",")) // common in case of appositions, e.g., 'based in Augusta<HEAD> ,<INCORRECT-HEAD> Ga.'
                            arc.setArgument(new DepNode(category, shadowTree ? category : tree.getCategory(tree.getParent(anchor)), anchor, timestamp, shadowTree, tree.toStringUnlex(false)));
                        applyAuxVerbHeuristic(arc, arc.getArgument());
                    }              
                    Node node = arc.getShadowTreeRootNode();
                    if(node != null)
                    {
                        short shadowOrPpNode = node.getNodeId();
                        Node fringeNode = findNodeInFringeById(shadowOrPpNode);
                         // we are past the root of the shadow tree or the PP phrase, so remove
                        if(fringeNode == null && (applyConllHeuristics || findNodeInFringeByIdApprox(shadowOrPpNode) == null))
                        {
                            it.remove();
                            if(!applyConllHeuristics && isCategoryPP(arc.getArgument().posTag) && !dependencies.hangingPrepositionsIsEmpty())
                                dependencies.popHangingPreposition();
                        }
                    }                                
                }                
            }
        }        
    }
    
    private CandidateType findCandidateNodesInFringe(ElementaryStringTree elemTree, short elemTreeOffset, TreeState elemTreeState, Node prefixTreeNode, 
            String rootCategory, boolean verification, DualHashBidiMap<Integer, Integer> coveredNodes, 
            DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree, int timestamp)
    {
        // constantly update top NP node, in order to get the most recent head information due to adjoining
        Node topNpNode = dependencies.peekFirstNpHead();
        short topNpNodeId = -1;
        if(topNpNode != null && topNpNode.getCategory().equals("NP"))
            topNpNodeId = topNpNode.getNodeId();
        if(!verification && topNpNodeId != -1 && topNpNodeId == prefixTreeNode.getNodeId())
        {            
            if(prefixTreeNode.kernelIsEmpty()) // the head info is not on the prefix tree but on the incoming elementary tree
            {
                Node elemRootNode = elemTreeState.findNodeInFringeById((short)elemTree.getRoot());
                if(elemRootNode != null)
                    topNpNode.setKernelAndTreeIfEmpty(elemRootNode.getKernel(), elemTree.toStringUnlex(false), elemTree.isAuxtree());
            }
            else // the head info is on the prefix tree
            {   
                dependencies.popNpHead(); // remove from stack
                dependencies.pushNpHead(prefixTreeNode); // ...and add the new node with (hopefully) fresh head information
            }            
        }
        // special treatment for verification trees. Essentially try to recover the head information from verification trees rooted on NP.        
        else if (topNpNode != null && verification)            
        {
            Node verifNode;
            // prefixTreeNode is the root of the shadowTree
            if(offsetNodeIdsOfShadowTree.get(prefixTreeNode.getNodeId()) == topNpNodeId)
            {
                int verifTreeId = coveredNodes.getKey(new Integer(prefixTreeNode.getNodeId()));
                verifNode = elemTreeState.findNodeInFringeById((short)verifTreeId);
                if(verifNode != null && verifNode.getLambdaPos().startsWith("N"))
                    topNpNode.setKernelAndTreeIfEmpty(verifNode.getKernel(), elemTree.toStringUnlex(false), elemTree.isAuxtree());
            }
//            else if ((verifNode = elemTreeState.findNodeInFringeById(topNpNodeId)) != null)
            else // search for the top NP node in the verification tree
            {
                // map back from prefix node id of the top NP node, to the original shadow id...
                Short shadowNodeId = offsetNodeIdsOfShadowTree.getKey(topNpNodeId);
                if(shadowNodeId != null) // ... and then back to the verification tree id-space
                {
                    Integer verifTreeId = coveredNodes.getKey(new Integer(shadowNodeId));
                    if(verifTreeId != null)
                    {
                        verifNode = elemTreeState.findNodeInFringeById(verifTreeId.shortValue());
                        if(verifNode != null && verifNode.getLambdaPos().startsWith("N"))
                            topNpNode.setKernelAndTreeIfEmpty(verifNode.getKernel(), elemTree.toStringUnlex(false), elemTree.isAuxtree());
                    } // if
                } // if
            } // else
        } // else if
        Fringe curFringe = elemTreeState.getFringe();
        List<Fringe> futureFringes = elemTreeState.getFutureFringe().getFringe();        
        if(addNpSubstBelowSInFringe(curFringe, prefixTreeNode, verification, offsetNodeIdsOfShadowTree)) // root S, S - NP!
        {
            if(prefixTreeNode.getCategory().equals("NP")) // complete NP
            {
//                System.out.println("NP should be complete - (root S - NP!)");
                return CandidateType.npSbj;
            }
            else
            {
//                System.out.println("Might be incomplete - (root S - NP!)");
                return CandidateType.npSbj;
            }
        }
        else if(addNpSubstBelowSInFringe(futureFringes, prefixTreeNode, verification, offsetNodeIdsOfShadowTree)) // S - NP! elsewhere in tree
        {
//            if(!verification)
//                System.out.println("NP should be incomplete - (S - NP!)");
//            else
//                System.out.println("Correctly verified and didn't do anything to the stack");
            return CandidateType.npSbj;
        }
        else if(addNpHeadOfSbarInFringe(prefixTreeNode, elemTree, elemTreeState, timestamp))
        {
            return CandidateType.npSbj;
        }
        else if(identifyNpSubjTrace(curFringe)) // not doing anything with it
        {
//            System.out.println("Found trace - (root S) ");
//            if(verification)
//            {
//                
//                System.out.println(findNodeInFringeById((short) (coveredNodes.get(new Integer(prefixTreeNode.getNodeId())) + elemTreeOffset)));
//            }
            return CandidateType.trace;
        }
        else if(addSubjNpBelowSInFringe(curFringe, elemTreeOffset))
        {
//            System.out.println("NP should be incomplete - ( (root S (NP ...) )");
            return CandidateType.npSbj;
        }        
        else if(identifyNpSubjTrace(futureFringes)) // not doing anything with it
        {
//            System.out.println("Found trace - (elsewhere in tree)");
            return CandidateType.trace;
        }
//        else // [NP, S, ...]
//        {
//            
//        }
        return CandidateType.none;
    }
    
    private boolean  addNpSubstBelowSInFringe(Fringe fringe, int futureFringePos, Node prefixNode, 
            boolean openRightEmpty, boolean verification, DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree) // [][S]:NP
    {
        List<Node> openRight = fringe.getAdjNodesOpenRight();
        Node lastAdjNode = fringe.getLastAdjNode();
        Node elemSubst = fringe.getSubstNode();
        if(lastAdjNode == null || elemSubst == null)
            return false;        
        if( (!openRightEmpty || openRight.isEmpty()) && lastAdjNode.getCategory().equals("S") &&
           elemSubst.getCategory().equals("NP")) // NP in subject position ( S NP! ...) case, i.e., fringe=[][S]:NP
        {
            if(verification) // check whether we verify the first two nodes in the stack
            {
                Short firstEl = offsetNodeIdsOfShadowTree.get(elemSubst.getNodeId());
                Short secondEl = offsetNodeIdsOfShadowTree.get(lastAdjNode.getNodeId());
                if(firstEl == null || secondEl == null)
                    return false;
                return dependencies.twoFirstElementsEquals(firstEl, secondEl);
            }
            else
            {
                // treat S label
                // copy-paste the identical Node on the prefix's fringe or future fringe
                int futureFringeListSize = this.futureFringe.getFringe().size();
                Fringe prefixFringe = futureFringePos == -1 || futureFringePos >= futureFringeListSize ? this.fringe : this.futureFringe.getFringe().get(futureFringePos);
                if(prefixFringe.getLastAdjNode() != null && prefixFringe.getLastAdjNode().identical(fringe.getLastAdjNode()))
                {
                    dependencies.pushNpHead(prefixFringe.getLastAdjNode());
                }
                else // as a last resort search for the identical node on the prefix fringe or future fringe
                {               
                    Node sNode = findIdenticalNodeInFringe(fringe.getLastAdjNode());
                    if(sNode != null)
                        dependencies.pushNpHead(sNode);
                    else
                    {
                        dependencies.pushNpHead(fringe.getLastAdjNode());
//                        System.out.println("identical S not found in prefix tree");
                    }
                }            
                // treat NP label
                // in case the tree is ( root S - NP! ...) and the prefixNode is also an NP, i.e., 
                // adjoins to the leftmost NP! push the prefixNode in the stack (hopefully containing complete delta info)
                Node npHead;
                Node prefixSubstNode = prefixFringe.getSubstNode();
                if(openRightEmpty && prefixNode.getCategory().equals("NP"))
                {
                    npHead = prefixNode;
                }
                // copy-paste the identical Node on the prefix's fringe or future fringe
                else if(prefixSubstNode != null && prefixSubstNode.identical(elemSubst))
                {
                    npHead = prefixSubstNode;
                }
                else // as a last resort search for the identical node on the prefix fringe or future fringe
                { // TODO: check if it applies
                    Node npNode = findIdenticalSubstNodeInFringe(elemSubst);
                    if(npNode != null)        
                        npHead = npNode;
                    else
                    {
                        npHead = elemSubst;
//                        System.out.println("identical NP not found in prefix tree");
                    }
                }
                dependencies.pushNpHead(npHead);
            }              
            return true;
        }
        return false;
    }
    
    private boolean  addNpSubstBelowSInFringe(Fringe fringe, Node prefixNode, boolean verification, DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree)
    {
        return addNpSubstBelowSInFringe(fringe, -1, prefixNode, true, verification, offsetNodeIdsOfShadowTree);
    }
    
    private boolean addNpSubstBelowSInFringe(List<Fringe> fringes, Node prefixNode, boolean verification, DualHashBidiMap<Short, Short> offsetNodeIdsOfShadowTree)
    {
        boolean updated = false;
        int i = 0;
        for(Fringe f : fringes)
            if(addNpSubstBelowSInFringe(f, i++, prefixNode, false, verification, offsetNodeIdsOfShadowTree))
                updated = true;
        return updated;
    }

    private boolean addSubjNpBelowSInFringe(Fringe fringe, short elemTreeOffset) // [][S,NP]:x
    {
        List<Node> openRight = fringe.getAdjNodesOpenRight();
        List<Node> openLeft = fringe.getAdjNodesOpenLeft();
        Node elemSubst = fringe.getSubstNode();
        if(openRight.isEmpty() && openLeft.size() > 1 && openLeft.get(0).getCategory().equals("S") &&
           openLeft.get(1).getCategory().equals("NP")) // S root and NP in subject position ( S (NP ...) case, i.e., [][S,NP]:x
        {
//            dependencies.pushNpHead(fringe.getLastAdjNode());
//            dependencies.pushNpHead(elemSubst);
            Node sNode = openLeft.get(0);
            sNode.offsetNodeId(elemTreeOffset);
            Node npNode = openLeft.get(1);
            npNode.offsetNodeId(elemTreeOffset);
            dependencies.pushNpHead(sNode);
            dependencies.pushNpHead(npNode);
            return true;
        }
        return false;
    }
    
    private boolean addNpHeadOfSbarInFringe(Node prefixTreeNode, ElementaryStringTree elemTree, TreeState elemTreeState, int timestamp)
    {
        if(!( prefixTreeNode == null || prefixTreeNode.getCategory().equals("NP")))
            return false;
//        int sbarId = -1;
//        for(int i = 0; i < elemTree.getCategories().size(); i++)
//        {
//            if(elemTree.getCategory(i).equals("SBAR")) // found SBAR
//            {
//                List<Integer> children = elemTree.getChildren(i);
//                if(children != null)
//                {
//                    String firstChildCategory = elemTree.getCategory(children.get(0));
//                    if(firstChildCategory != null && firstChildCategory.startsWith("WH"))
//                    {
//                        return true;
//                    }
//                } // if
//            } // if
//        } // for
        List<Node> fringeNodes = elemTreeState.getNodesOnFringe();
        for(int i = 0; i< fringeNodes.size(); i++)
        {
            if(fringeNodes.get(i).getCategory().equals("SBAR"))
            {
                Node sbarNode = fringeNodes.get(i);
                if(i + 1 < fringeNodes.size())
                {
                    Node candWhNode = fringeNodes.get(i + 1);
                    if(candWhNode.getCategory().startsWith("WH"))
                    {
                        int anchorId = elemTree.getAnchor();
                        Integer anchorParentId = elemTree.getParent(anchorId);
                        String anchorParentCategory = anchorParentId > Integer.MIN_VALUE ? elemTree.getCategory(anchorParentId) : null;
                        
                        dependencies.pushNpHead(sbarNode);
                        // Prefix tree is NP-headed, e.g., ...Gave answers(prefix NP-Head) that we'd like to have 
                        if(prefixTreeNode != null && prefixTreeNode.getLambdaPos().startsWith("N"))
                            dependencies.pushNpHead(prefixTreeNode);
                        // Zero relative. The elementary tree contains a null complementizer (0), so we need
                        // to use it's anchor as the head and not the adjoining prefix tree head, e.g.,
                        // ...gave (prefix-head) answers (NP-Head) we'd like to have
                        else if(prefixTreeNode != null && anchorParentCategory != null && !isRelativePos(anchorParentCategory))
                        {
                            Node npHead = elemTreeState.getRootNode().copy();
                            npHead.setNodeId(prefixTreeNode.getNodeId());
                            dependencies.pushNpHead(npHead);
                        }
                        // find relative pronoun and save info for later (useful to add R-A0, R-A1s)                        
                        if(anchorParentCategory != null && isRelativePos(anchorParentCategory))
                        {
                            dependencies.setCurrentRelative(new DepNode(elemTree.getCategory(anchorId), anchorParentCategory, anchorId, timestamp));
                        }
                        return true;
                    } // if
                } // if
            } // if
        } // for
        return false;
    }
    
    private boolean identifyNpSubjTrace(Fringe fringe)
    {
//        List<Node> openRight = fringe.getAdjNodesOpenRight();
        List<Node> openLeft = fringe.getAdjNodesOpenLeft();
        return openLeft.size() > 3 && openLeft.get(0).getCategory().equals("S") &&
           openLeft.get(1).getCategory().equals("NP") &&
           openLeft.get(2).getCategory().equals(":") &&
           (openLeft.get(3).getCategory().equals("*") || // subject NP trace: ( S (NP (: *))) case, i.e., fringe=[x][S,NP,:,*]:x    
            openLeft.get(3).getCategory().equals("*T*")); // or (S NP (: *T*))) case, i.e., below a relative clause with fringe=[x][S,NP,:,*T*]:x
    }
    
    private boolean identifyNpSubjTrace(List<Fringe> fringe)
    {
        boolean found = false;
        for(Fringe f : fringe)
            if(identifyNpSubjTrace(f))
                found = true;
        return found;
    }
    
    private boolean isRelativePos(String category)
    {
        return category.equals("WDT") || category.equals("DT") || 
               category.equals("WP") || category.equals("IN");
    }
    
    private DependencyArc newDependencyArc(DepNode integrationPoint, Collection<Role> roles, ElementaryStringTree tree, boolean isRelation)
    {
//        String ipCategory = integrationPoint.category;
        DepNode argument = null;
//        if(ipCategory.equals(":") && !dependencies.npHeadsIsEmpty()) // found a trace
//        {
//            // check whether it is an argument trace (cat=*) or an argument-bar trace (cat=*T*)            
//            TraceInfo traceInfo = findTrace(tree);
//            if(traceInfo != null)
//            {
//                DepNode traceNode = traceInfo.node;
//                String traceType = traceNode.category;
//                String parentCategory = traceInfo.parentCategory;
//                // argument traces, or argument-bar traces that fill an NP clause (e.g., subj below subordinate clause)
//                if(traceType.equals("*") || (parentCategory != null && traceType.equals("*T*") && parentCategory.equals("NP")))
//                {                
//                    Node filler = dependencies.peekFirstNpHead();
//                    argument = new DepNode(getNodeCategory(filler), filler.getNodeId(), filler.getLambdaTimestamp());
//                }
//            }                        
//        }
        DependencyArc arc = new DependencyArc(integrationPoint, roles, argument, null);
//        dependencies.pushHangingModifier(arc);
        return arc;
    }
    
    
    /**
     * Assign label to the infinitive marker TO after a verb, if it exists, instead to 
     * the head of the following clause (e.g., VBD managed TO to VB push, 
     * assign label between managed and to and NOT managed and push).
     * @param arc
     * @param anchorNode 
     * @param iterator 
     */
    private boolean applyInfinitiveMarkerHeuristic(DependencyArc arc, DepNode anchorNode, Iterator<DependencyArc> iterator, 
            String[] words, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {
        DepNode candInfinitiveMarker = dependencies.peekLastInfinitiveMarker();        
        // infinitive marker is always right before the anchor node (timestamp - 1), e.g., 'to (IM) train'
        if(applyConllHeuristics && candInfinitiveMarker != null && candInfinitiveMarker.timestamp == anchorNode.timestamp - 1)
        {
            setArgument(arc, dependencies.popLastInfinitiveMarker(), iterator, words, origPosTags, operation, direction, shadow);
        }                                
        else
        {
            setArgument(arc, anchorNode, iterator, words, origPosTags, operation, direction, shadow);
        }
        // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
        boolean keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags); 
        if(!keepArc)
            removeArcSafe(arc, arc.getIntegrationPoint(), iterator);
        return !keepArc;
    }
    
    /**
     * Assign label to the auxiliary verb before a verb if it exists as in 'Peter is (head) interested in learning'.
     * @param anchorNode
     * @return 
     */
    private void applyAuxVerbHeuristic(DependencyArc arc, DepNode verb)
    {
        if (applyConllHeuristics && !arc.isIncomplete() && verb != null && dependencies.containsAuxVerb(verb.category))        
        {
            DepNode candAuxVerb = dependencies.getAuxVerb(verb.category);        
            if(candAuxVerb != null)
//                setArgument(arc, candAuxVerb, iterator, origPosTags, operation, direction, shadow);
                arc.setArgument(candAuxVerb);
        }        
    }
    
    /**
     * Set argument to the NE just before the post-honorific. Return the old argument if 
     * we performed a change for later use or null otherwise.
     * @param arc
     * @param words
     * @param origPosTags
     * @return 
     */
    private DepNode applyPostHonorificHeuristic(DependencyArc arc, String[] words, String[] origPosTags)
    {
        DepNode curArg = arc.getArgument();
        if (!arc.isIncomplete() && isPostHonorific(curArg.category))        
        {
            int timestamp = curArg.timestamp;
            arc.setArgument(new DepNode(splitWord(words[timestamp - 1]), origPosTags[timestamp - 1], timestamp - 1));
            return curArg;
        }   
        return null;
    }
    
    
    /**
     * Assign label to the head of the PP tree (i.e., the preposition itself) rather than
     * to the head of the adjacent prepositional phrase it modifies.
     * Then the method automatically checks whether the argument with the PP as argument, already
     * exists and hence need to be merged. Finally it runs through an ensemble of classifiers
     * to identify it as a good candidate arc and disambiguate its roles. Returns
     * true if we decide to keep this arc (i.e., it has not been merged and the classifier
     * identifies it as a good candidate).
     * @param arc
     * @param tree
     * @param anchorNode
     * @param offset 
     */
    private boolean applyPPArgumentHeuristic(DependencyArc arc, ElementaryStringTree tree, DepNode anchorNode, short offset, 
            String[] words, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {
        boolean mergedArc = false, keepArc = false;
        if(isTreePP(tree, anchorNode, offset) && arc.isArgumentIncomplete())
        {            
            mergedArc = setArgument(arc, anchorNode, words, origPosTags, operation, direction, shadow);
            // possibly created a complete arc, so we can identify and disambiguate role labels discriminatively
            keepArc = identifyArcAndDisambiguateRoles(model, arc, words, origPosTags);
            if(!applyConllHeuristics)
            {
                if(!mergedArc && keepArc)
                {
    //                dependencies.addHangingPreposition(anchorNode, arc.getIntegrationPoint());
    //                int treeIp = tree.isAuxtree() ? tree.getFoot() : tree.
                    // Store the sister node of the preposition, which is either a foot or a substitution node.
                    // This is going to be used in the head-finding algorithm to fill in the triple with the head of the PP,
                    // rather than the prepopsition, which is the default for CoNLL.
                    List<Integer> ppChildren = tree.getChildren(tree.getParent(tree.getParent(anchorNode.getId() - offset)));
                    if(ppChildren.size() > 1)
                    {                    
                        Node ppIntegrationPoint = findNodeInFringeById((short)(ppChildren.get(ppChildren.size() - 1) + offset));
                        arc.setShadowTreeRootNode(ppIntegrationPoint);
                        dependencies.addShadowArgument(anchorNode, arc);
                        dependencies.addHangingPreposition(anchorNode, arc.getIntegrationPoint());
                    }                
                }    
            }
            return mergedArc || !keepArc;
        }
        return mergedArc;
    }

    /**
     * Searches for a cached preposition of a PP that has the same integration point as the arc.
     * It then replaces the argument in the arc with the preposition, in order to apply
     * the set of classifiers for correct identification/labeling.
     * @param arc
     * @return the previous argument of the arc
     */
    private DepNode replaceArgumentWithPrep(DependencyArc arc)
    {
        DepNode prevArg = null;
        DepNode ip = arc.getIntegrationPoint();
        Pair<DepNode, DepNode> prep = dependencies.pollHangingPreposition();
        if(prep != null)
        {
            prevArg = arc.getArgument();
            //arc.setArgument(prep);
        }        
        return prevArg;
    }
    
    private TraceInfo findTrace(ElementaryStringTree tree)
    {
//        List<String> categories = tree.getNonEmptyCategories();
        List<String> categories = tree.getCategories();
        int traceId = -1;
        for(int i = 0; i < categories.size(); i++)
        {
            String category = categories.get(i);
            if(category != null && category.equals(":"))
            {
                traceId = i;
                break;
            }
        }
        if(traceId == -1)
            return null;
        List<Integer> children = tree.getChildren(traceId);
        if(!children.isEmpty())
        {
            Integer parent = tree.getParent(traceId);
            Integer childId = children.get(0);
            String parentCategory = parent != null ? categories.get(parent) : null;
            DepNode dn = new DepNode(categories.get(childId), parentCategory, traceId, -1000);            
            Collection<Role> roles = new ArrayList<Role>();
            for(RoleSignature rs : tree.getRoleSignatures())
            {
//                Role candidateRole = rs.getRoles().get(childId);
                Role candidateRole = rs.getRoles().get(traceId);
                if(candidateRole != null)
                    roles.add(candidateRole);
            } // add roles if any
            if(roles.isEmpty())
                return null;
            return new TraceInfo(dn, roles, parentCategory);
        }
        return null;
    }
    
    private int findTimestampOfWord(String[] words, int curTimestamp, String word)
    {
        for(int i = curTimestamp; i < words.length; i++)
        {
            String curWord = splitWord(words[i]);
            if(curWord.equals(word))
                return i;
        }
        return curTimestamp;
    }
    
    private void addCoordinatedArc(DependencyArc sourceArc, DepNode newRelation, String[] origPosTags, String operation, boolean direction, boolean shadow)
    {
        DependencyArc newArc = new DependencyArc(sourceArc, newRelation);
        updateArcFeatures(newArc, origPosTags, operation, direction, shadow);
        sourceArc.addCoordinatedArc(newArc);
        addArc(newArc);
    }
    
    private boolean isTreePP(ElementaryStringTree tree, DepNode anchorNode, int offset)
    {
        int nodeId = anchorNode.getId() - offset;
        if(!tree.hasParent(nodeId))
            return false;
        String category = tree.getCategory(tree.getParent(nodeId));
        return isCategoryPP(category);
    }
    
    static boolean isCategoryPP(String category)
    {
        return category != null && (category.equals("IN") || category.equals("TO"));
    }
   
    static boolean isAuxiliary(String category)
    {
        return category.equals("AUX") || category.equals("MD");
    }
    
    private static final Set<String> postHonorificSet = new HashSet<String>(Arrays.asList(new String[] {"Corp", "Corp.", "Co", "Co.", "Inc", "Inc.", "Ltd", "Ltd."}));
    
    static boolean isPostHonorific(String word)
    {
        return postHonorificSet.contains(word);
    }
    
    private String getNodeCategory(Node node)
    {
        String category = node.getLambda();
        return category.contains("\t") ? category.split("\\p{Space}")[1] : category;
    }
    
    private String splitWord(String posWord)
    {
        return posWord.contains(" ") ? posWord.split("\\p{Space}")[1] : posWord;
    }        
    
    @Override
    public boolean identical(TreeState tsIn)
    {
        DepTreeState ts = (DepTreeState)tsIn;
        if (//Auxtree
                (auxtree == ts.isAux())
                && //Wordcover
                (wordcover[0] == ts.getWordCover()[0] && wordcover[1] == ts.getWordCover()[1])
                && //footleft
                (!auxtree || footleft == ts.hasFootLeft())
                && //shadowTreeList
                shadowTreeList.size() == ts.getShadowTrees().size()
                && //unaccessible nodes
                futureFringe.getFringe().size() == ts.getFutureFringe().getFringe().size()//&&
                //Root
                //(root.identical(ts.root))//&&
                //fringe
                //(fringe.toString().equals(ts.fringe.toString()))
                )
        {
            for (int i = 0; i < shadowTreeList.size(); i++)
            {
                if (//!this.shadowTreeList.get(i).getTreeString().equals(ts.shadowTreeList.get(i).getTreeString())){
                        shadowTreeList.get(i).getTreeOrigIndex() != ts.getShadowTrees().get(i).getTreeOrigIndex())
                {
                    return false;
                }
                if (!shadowTreeList.get(i).getIndexChange().toString().equals(ts.getShadowTrees().get(i).getIndexChange().toString()))
                {
                    return false;
                }
            }
            ArrayList<Fringe> unaccessibleNodes = futureFringe.getFringe();
            for (int i = 0; i < unaccessibleNodes.size(); i++)
            {
                if (!unaccessibleNodes.get(i).toString().equals(ts.getFutureFringe().getFringe().get(i).toString()))
                {
                    return false;
                }
            }
            // NOTE: 
            return dependencies.equals(ts.dependencies); 
//            return true;
        }
        return false;
    }
    
    @Override
    public String toString()
    {
        return new StringBuilder().append(" FRINGE: ").append(fringe).append(";").append(futureFringe.getFringe())
                .append(" shadowindices: ").append(this.getShadowIndeces()).append(" dependencies: ").append(dependencies).toString();
    }      
   
    class TraceInfo
    {
        DepNode node;
        Collection<Role> roles;
        String parentCategory;

        public TraceInfo(DepNode node, Collection<Role> roles, String parentCategory)
        {
            this.node = node;
            this.roles = roles;
            this.parentCategory = parentCategory;
        }                
    }
}