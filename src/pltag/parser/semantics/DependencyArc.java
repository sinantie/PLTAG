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

import edu.stanford.nlp.process.Morphology;
import fig.basic.Indexer;
import fig.basic.LogInfo;
import fig.basic.Pair;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import pltag.parser.Fringe;
import pltag.parser.Node;
import pltag.parser.ParserModel;
import pltag.parser.SuperTagFringe;
import pltag.parser.semantics.classifier.ArgumentClassifier;
import pltag.parser.semantics.classifier.FeatureVec;
import pltag.parser.semantics.conll.Argument;
import pltag.parser.semantics.conll.Predicate;
import pltag.util.Utils;

/**
 *
 * @author konstas
 */
public final class DependencyArc implements Serializable
{
    
    static final long serialVersionUID = -1L;
    private final DepNode integrationPoint;
    private DepNode argument, relation;
    private final Set<Role> roles;
    private List<DependencyArc> coordinatedArcs;
    static short coordinateIncr = 1;
    private SuperTagFringe prefixFringeAlphaSet;
    private StringBuilder operationPath;
    private boolean operationPathClosed;
    Node shadowTreeRootNode;
    
    DependencyArc(DepNode integrationPoint, Collection<Role> roles, DepNode argument, DepNode relation, SuperTagFringe prefixFringeAlphaSet)
    {
        this.integrationPoint = integrationPoint;
        this.roles = new HashSet<Role>(); // eliminate possible duplicates
        this.roles.addAll(roles);
        this.argument = argument;
        this.relation = relation;
        this.prefixFringeAlphaSet = prefixFringeAlphaSet;
        this.operationPath = new StringBuilder();
        this.operationPathClosed = false;
    }

    DependencyArc(DepNode integrationPoint, Collection<Role> roles, DepNode argument, DepNode relation)
    {
        this(integrationPoint, roles, argument, relation, null);
    }
    
    DependencyArc(DependencyArc d)
    {        
        integrationPoint = d.integrationPoint == null ? null : new DepNode(d.integrationPoint);
        roles = rolesClone(d.roles);
        argument = d.argument == null ? null : new DepNode(d.argument);
        relation = d.relation == null ? null : new DepNode(d.relation);
        prefixFringeAlphaSet = d.prefixFringeAlphaSet == null ? null : new SuperTagFringe(d.prefixFringeAlphaSet);
        this.operationPath = d.operationPath == null ? null : new StringBuilder(d.operationPath.toString());
        this.operationPathClosed = d.operationPathClosed;
        this.shadowTreeRootNode = d.shadowTreeRootNode;
    }
            
    DependencyArc(DependencyArc sourceArc, DepNode relation)
    {
        this(new DepNode(sourceArc.integrationPoint), new HashSet(sourceArc.roles), 
                sourceArc.argument == null ? null : new DepNode(sourceArc.argument), relation);
        // hack add a dummy number to the id so that there is no overlap in the map of dependencies indexed at integration point
//        integrationPoint.id += 1000 + (++coordinateIncr);
    }
        
    private HashSet<Role> rolesClone(Set<Role> roles)
    {
        HashSet<Role> h = new HashSet();
        for(Role r : roles)
            h.add(new Role(r));
        return h;
    }
    
    public void integrationPointIncr()
    {
        integrationPoint.id += 1000 + (++coordinateIncr);
    }
    
    public DepNode getIntegrationPoint()
    {
        return integrationPoint;
    }

    public Collection<Role> getRoles()
    {
        return roles;
    }
    
    public boolean addRole(Role role)
    {
        return roles.add(role);
    }
    
    public boolean addRoles(Collection<Role> roles)
    {
        return this.roles.addAll(roles);
    }
    
    public boolean hasRoles()
    {
        return !roles.isEmpty();
    }
    
    /**
     * set argument of the dependency arc. Automatically update coordinated arcs, 
     * and set their arguments as well. Return all affected arcs, in case we need
     * to do some post-hoc steps (add features used for classification later on).
     * @param argument
     * @return list of updated arcs
     */
    public List<DependencyArc> setArgument(DepNode argument)
    {
        List<DependencyArc> updatedArcs = new ArrayList<DependencyArc>();
        if(!isSelfReference(argument, relation) && passesTokenHeuristics(argument))
        {
            this.argument = argument;
            if(coordinatedArcs != null) // automatically update incomplete (or containing stale data) coordinated arcs
            {
                for(DependencyArc arc : coordinatedArcs)
                    arc.setArgument(argument);
                updatedArcs.addAll(coordinatedArcs);
            }
            updatedArcs.add(this);
        }
        return updatedArcs;
    }

    public DepNode getRelation()
    {
        return relation;
    }
    
    public void setRelation(DepNode relation)
    {
        if(!isSelfReference(argument, relation))
            this.relation = relation;
    }

    public DepNode getArgument()
    {
        return argument;
    }

    public void setPrefixFringeAlphaSet(Fringe fringe)
    {
        this.prefixFringeAlphaSet = new SuperTagFringe(fringe);
    }

    public SuperTagFringe getPrefixFringeAlphaSet()
    {
        return prefixFringeAlphaSet;
    }
    
    public boolean isRelationIncomplete()
    {
        return relation == null || relation.isShadow();
    }
    
    public boolean isArgumentIncomplete()
    {
        return argument == null || argument.isShadow();
    }
    
    public boolean isIncomplete()
    {
        return isArgumentIncomplete() || isRelationIncomplete();
    }
    
    public boolean isSelfReference()
    {
        return isSelfReference(argument, relation);
    }
    
    /**
     * Returns true if both the argument and predicate are null.
     * @return 
     */
    public boolean isEmpty()
    {
        return argument == null && relation == null;
    }
    
    public boolean isSelfReference(DepNode argument, DepNode relation)
    {
        return !(argument == null || relation == null || 
                argument.category == null || relation.category == null) 
                && argument.category.equals(relation.category);            
    }
    
    public boolean isModifier()
    {
        return containsRole("ARGM");
    }

    public Node getShadowTreeRootNode()
    {
        return shadowTreeRootNode;
    }
        
    public void setShadowTreeRootNode(Node shadowTreeRootNode)
    {
        this.shadowTreeRootNode = shadowTreeRootNode;
    }
     
    public boolean containsRole(String role)
    {
        for(Role r : roles)
        {
            if(r.toString().startsWith(role))
            {
                return true;
            } // if
        } // for
        return false;
    }
    
    public void addCoordinatedArc(DependencyArc coordinatedArc)
    {
        if(coordinatedArcs == null)
            coordinatedArcs = new ArrayList();
        coordinatedArcs.add(coordinatedArc);
    }
    
    private boolean passesTokenHeuristics(DepNode node)
    {
        return node == null || node.category == null || !node.category.equals("and");
    }
    
    /**
     * Use a role label classifier to rerank roles based on information stored
     * in the predicate and argument.     
     * @param argumentClassifier
     * @param vec
     */
    public void disambiguateRoles(ArgumentClassifier argumentClassifier, FeatureVec vec)
    {        
        if(roles.size() > 1 && !isIncomplete())
        {            
            // map from role labels to roles, to be able to map back from the best role label to Role object
            Map<String, Role> candidateRolesMap = new HashMap<String, Role>(); 
            String[] candidateRoles = rolesToStringArray(candidateRolesMap);
            String bestRoleLabel = argumentClassifier.rerankRoleLabels(vec, candidateRoles);
//            String bestRoleLabel = rerankRoleLabelsMajorityClass(model, candidateRoles);
            Role bestRoleFromMap = candidateRolesMap.get(bestRoleLabel);
            Role bestRole;
            if(bestRoleFromMap != null)
            {
                bestRole = new Role(bestRoleFromMap);
            }
            else
            {
                Role anyRole = candidateRolesMap.values().iterator().next();
                bestRole = new Role(anyRole.roleIndexer, anyRole.roleIndexer.getIndex(bestRoleLabel), anyRole.nodeId);
            }            
            roles.clear();
            roles.add(bestRole);                        
        }                
    }

    /**
     * Use a binary classifier to decide whether to keep or not the arc based on 
     * information stored in the predicate and argument.
     * @param argumentClassifier
     * @param vec
     * @return 
     */
    public boolean identifyArc(ArgumentClassifier argumentClassifier, FeatureVec vec)
    {        
        if(!isIncomplete())
        {
            try
            {
                return argumentClassifier.identifyArc(vec);
            }            
            catch(Exception e)
            {
                LogInfo.error("Error identifying arc");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Extract features based on predicate/argument and return a pair of feature vectors,
     * one for the identifier and another for the labeler.
     * @param argumentClassifier
     * @return 
     */
    public Pair<FeatureVec, FeatureVec> extractPairOfFeatures(ArgumentClassifier argumentClassifier)
    {
        String predLemma = Morphology.lemmaStaticSynchronized(relation.category, relation.posTag, true);
        Predicate pred = new Predicate(null, predLemma, relation.posTag, Predicate.Type.verb, relation.elemTree, relation.timestamp);
        Argument arg = new Argument(argument.timestamp, null, argument.category, argument.posTag, 
                argument.elemTree, integrationPoint.category, prefixFringeAlphaSet, getOperationPath(), null);
        FeatureVec identifierFeatureVec = argumentClassifier.newFeatureVec();
//        FeatureVec identifierFeatureVec = argumentClassifier.newBilexicalFeatureVec();
        argumentClassifier.extractBilexicalFeatures(pred, arg, false, identifierFeatureVec);
        argumentClassifier.extractSyntacticFeatures(pred, arg, false, identifierFeatureVec);
        FeatureVec labelerFeatureVec = argumentClassifier.newFeatureVec();
//        FeatureVec labelerFeatureVec = argumentClassifier.newBilexicalFeatureVec();
        argumentClassifier.extractBilexicalFeatures(pred, arg, false, labelerFeatureVec);
        argumentClassifier.extractSyntacticFeatures(pred, arg, false, labelerFeatureVec);
        return new Pair(identifierFeatureVec, labelerFeatureVec);        
    }
    
    /**
     * Extract features based on predicate/argument and return a pair of feature vectors,
     * one for the identifier and another for the labeler.
     * @param argumentClassifier
     * @return 
     */
    public FeatureVec extractBilexicalFeaturesOnly(ArgumentClassifier argumentClassifier)
    {
        String predLemma = Morphology.lemmaStaticSynchronized(relation.category, relation.posTag, true);
        Predicate pred = new Predicate(null, predLemma, relation.posTag, Predicate.Type.verb, relation.elemTree, relation.timestamp);
        Argument arg = new Argument(argument.timestamp, null, argument.category, argument.posTag, 
                argument.elemTree, integrationPoint.category, prefixFringeAlphaSet, getOperationPath(), null);
//        FeatureVec featureVec = argumentClassifier.newFeatureVec();        
        FeatureVec featureVec = argumentClassifier.newBilexicalFeatureVec();        
        argumentClassifier.extractBilexicalFeatures(pred, arg, false, featureVec);
//        argumentClassifier.extractSyntacticFeatures(pred, arg, false, featureVec);        
        return featureVec;
    }
    
    String rerankRoleLabelsMajorityClass(ParserModel model, String[] candidateRoles)
    {
        Indexer<String> roleIndexer = model.getConllRoleIndexer();
        Map<Integer, Integer> roleFreqs = model.getTrainRoleFreqs();
        SortedSet<Pair<String, Integer>> pairs = new TreeSet<Pair<String, Integer>>(new Pair.ReverseSecondComparator<String, Integer>());
        for(String candidateRole : candidateRoles)
        {
            Integer roleId = roleIndexer.getIndex(candidateRole);           
            Integer freq = roleFreqs.get(roleId);
            if(freq != null)
                pairs.add(new Pair<String, Integer>(candidateRole, freq));
        }        
        return pairs.isEmpty() ? candidateRoles[0] : pairs.first().getFirst();
    }
    
    /**
     *  Return the top scoring role label for the arc. Run the role label classifier
     *  if necessary.
     * @param model
     * @param vec
     * @return 
     */
    public String getTopRole(ParserModel model, FeatureVec vec)
    {
        if(roles.size() > 1 && !model.getOpts().trainClassifiers && vec != null)
        {
            disambiguateRoles(model.getArgumentClassifier(), vec);
        }
        return getFirstRole().toString(); // use the first one
    }
    
    public String getMajorityTopRole(ParserModel model)
    {
        if(roles.size() > 1)
        {
            return rerankRoleLabelsMajorityClass(model, rolesToStringArray(null));
        }
        return getFirstRole().toString(); // use the first (and only) one
    }
    
    public Role getFirstRole()
    {
        if(roles.isEmpty())
            return null;
        return roles.toArray(new Role[0])[0];
    }
    
    /**
     * Convert roles set to a list of string role candidates
     * @param candidateRolesMap map from role labels to roles, to be able to map back from the best role label to Role object        
     * @return 
     */
    private String[] rolesToStringArray(Map<String, Role> candidateRolesMap)
    {
        String[] candidateRoles = new String[roles.size()];
        int i = 0;
        for(Role role : roles)
        {
            String roleLabel = Utils.convertRoleFromPropbankToConll(role.toString());
            candidateRoles[i++] = roleLabel;
            if(candidateRolesMap != null)
                candidateRolesMap.put(roleLabel, role);
        }
        return candidateRoles;
    }
    
    public void updateOperationPath(String operation, boolean direction, boolean shadow)
    {
        if(!(operationPathClosed || isEmpty())) // update path for incomplete or just completed path
        {
            // in case this is a predicate-first triple, make a note of it, so we can reverse the paht in the end
            if(isArgumentIncomplete() && operationPath.length() == 0)
                operationPath.append("p");
            if(integrationPoint.category.equals(":"))
                operationPath.append("T");
            else if(roles.size() == 1 && getFirstRole().toString().startsWith("R-"))
                operationPath.append("R");
            else
                operationPath.append(operation).append(shadow ? "s" : "f").append(direction ? "^" : "_");
        }
        if(!operationPathClosed && !isIncomplete()) // triple is complete, so stop updating the operation path
            operationPathClosed = true;
    }

    public String getOperationPath()
    {
        String out;
        if(operationPath.length() > 0 && operationPath.charAt(0) == 'p') // need to reverse
            out = reversePath(operationPath.subSequence(1, operationPath.length()));
        else
            out = operationPath.toString();
        if(out.length() > 1 && out.split("[_^]").length > 10)
            return "long";
        return out;
    }
    
    public String reversePath(CharSequence str)
    {
        StringBuilder out = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        char nextChar = str.charAt(str.length() - 1);
        buffer.append(nextChar == '^' ? '_' : '^');
        for(int i = str.length() - 2; i >= 0; i--)
        {            
            nextChar = str.charAt(i);
            while(nextChar != '^' && nextChar != '_')
            {
                buffer.append(nextChar);
                if(i == 0)
                    break;
                nextChar = str.charAt(--i);
            }
            out.append(buffer.reverse().subSequence(0, buffer.length()));            
            buffer = new StringBuilder(nextChar == '^' ? "_" : "^");
        }
        return out.toString();
    }
    
    @Override
    public boolean equals(Object obj)
    {
//        assert obj instanceof DependencyArc;
        if(obj == null)
            return false;
        DependencyArc arc = (DependencyArc)obj;
        if (argument == null && arc.argument != null ||
            argument != null && arc.argument == null)
            return false;
//        if (relation == null && arc.relation != null ||
//            relation != null && arc.relation == null)
//            return false;        
        if(argument == null && arc.argument == null)
        {
//            if(relation == null && arc.relation == null)
                return integrationPoint.equals(arc.integrationPoint) && roles.equals(arc.roles);
//            else
//                return integrationPoint.equals(arc.integrationPoint) && roles.equals(arc.roles) && relation.equals(arc.relation);
        }
//        if(relation == null && arc.relation == null)
//        {
//            if(argument == null && arc.argument == null)
//                return integrationPoint.equals(arc.integrationPoint) && roles.equals(arc.roles);
//            else
//                return integrationPoint.equals(arc.integrationPoint) && roles.equals(arc.roles) && argument.equals(arc.argument);
//        }
        return integrationPoint.equals(arc.integrationPoint) &&
                argument.equals(arc.argument) &&
                roles.equals(arc.roles);// &&
//                relation.equals(arc.relation);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + (this.integrationPoint != null ? this.integrationPoint.hashCode() : 0);
        hash = 97 * hash + (this.argument != null ? this.argument.hashCode() : 0);
        hash = 97 * hash + (this.relation != null ? this.relation.hashCode() : 0);
        hash = 97 * hash + (this.roles != null ? this.roles.hashCode() : 0);
        return hash;
    }
    
    public String prettyPrint()
    {
        return String.format("<%s,%s,%s>", relation.prettyPrint(), roles, argument.prettyPrint());
    }
    
    @Override
    public String toString()
    {
        return String.format("<%s,%s,%s,%s>", integrationPoint, roles, argument, relation);
    }      
       
}
