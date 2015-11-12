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
package pltag.parser.semantics.discriminative;

import fig.basic.Indexer;
import fig.basic.LogInfo;
import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import pltag.parser.semantics.classifier.AbstractFeatureIndexers;
import pltag.util.Utils;

/**
 *
 * @author sinantie
 */
public class DiscriminativeFeatureIndexers extends AbstractFeatureIndexers implements Serializable
{

    private static final long serialVersionUID = -1L;
    private static final String CLASS_NAME = "pltag.parser.semantics.discriminative.DiscriminativeFeatureIndexers";
    
    // Maps from strings to integers: useful for doing 1-of-K encoding for categorical entries.        
    private final Indexer<String> elemTreeIndexer, prevElemTreeIndexer, elemTreeBigramIndexer, 
            elemTreeUnlexIndexer, prevElemTreeUnlexIndexer, elemTreeUnlexBigramIndexer, 
            integrationPointIndexer, fringeIndexer, 
            ipElemTreeIndexer, ipElemTreeUnlexIndexer, 
            coParIndexer, coLenParIndexer, heavyIndexer, neighboursL1Indexer, neighboursL2Indexer, 
            wordL2Indexer, wordL3Indexer,
            srlTripleIndexer, srlTripleUnlexIndexer, srlTripleIncompleteIndexer, srlTripleIncompleteUnlexIndexer,
            srlDependencyIndexer, srlDependencyUnlexIndexer, 
            srlRolePredIndexer, srlRolePredPosIndexer, srlRoleArgIndexer, srlRoleArgPosIndexer, 
            srlPredIndexer, srlPredPosIndexer, srlArgIndexer, srlArgPosIndexer,
            srlFrameIndexer, srlFramePosIndexer,
            roleIndexer, wordIndexer, posIndexer, categoryIndexer, wordLemmatizedIndexer;                
    
    private final Map<Integer, Integer> word2LemmaMap;
    
    public DiscriminativeFeatureIndexers()
    {
        elemTreeIndexer = newIndexer();        
        prevElemTreeIndexer = newIndexer();
        elemTreeBigramIndexer = newIndexer();
        elemTreeUnlexIndexer = newIndexer();
        prevElemTreeUnlexIndexer = newIndexer();
        elemTreeUnlexBigramIndexer = newIndexer();
        integrationPointIndexer = newIndexer();   
        fringeIndexer = newIndexer();
        srlTripleIndexer = newIndexer();
        srlTripleUnlexIndexer = newIndexer();
        roleIndexer = newIndexer();
        wordIndexer = newIndexer();
        posIndexer = newIndexer();        
        
        coParIndexer = newIndexer();
        coLenParIndexer = newIndexer();
        heavyIndexer = newIndexer();
        neighboursL1Indexer = newIndexer();
        neighboursL2Indexer = newIndexer();
        ipElemTreeIndexer = newIndexer();
        ipElemTreeUnlexIndexer = newIndexer();
        wordL2Indexer = newIndexer();
        wordL3Indexer = newIndexer();
        categoryIndexer = newIndexer();
        wordLemmatizedIndexer = newIndexer();
        word2LemmaMap = new HashMap<>();
        
        srlTripleIncompleteIndexer = newIndexer();
        srlTripleIncompleteUnlexIndexer = newIndexer();
        srlDependencyIndexer = newIndexer();
        srlDependencyUnlexIndexer = newIndexer();
        srlRolePredIndexer = newIndexer();
        srlRolePredPosIndexer = newIndexer();
        srlRoleArgIndexer = newIndexer();
        srlRoleArgPosIndexer = newIndexer();
        srlPredIndexer = newIndexer();
        srlPredPosIndexer = newIndexer();
        srlArgIndexer = newIndexer();
        srlArgPosIndexer = newIndexer();
        srlFrameIndexer = newIndexer();
        srlFramePosIndexer = newIndexer();
    }
    
    public DiscriminativeFeatureIndexers(DiscriminativeFeatureIndexers indexersIn)
    {
        this();
        try
        {
            Field[] fields = Class.forName(CLASS_NAME).getDeclaredFields();
            for(Field field : fields)
            {
                if(field.getType() == Indexer.class)
                {                    
                    Indexer<String> indexerIn = (Indexer<String>) field.get(indexersIn);
                    Indexer<String> indexerThis = (Indexer<String>) field.get(this);
                    if(indexerIn != null)
                        indexerThis.addAll(indexerIn);
                }
                else if(field.getType() == HashMap.class)
                {
                    Map<Integer, Integer> mapIn = (HashMap) field.get(indexersIn);
                    Map<Integer, Integer> mapThis = (HashMap) field.get(this);
                    if(mapIn != null)
                        mapThis.putAll(mapIn);
                }
            }
        } 
        catch (ClassNotFoundException ex)
        {
            LogInfo.error(ex);
        } catch (IllegalArgumentException ex)
        {
            LogInfo.error(ex);
        } catch (IllegalAccessException ex)
        {
            LogInfo.error(ex);
        }
        
    }
    
    public DiscriminativeFeatureIndexers(String folder)
    {
        this();
        try
        {            
            Class cl = Class.forName(CLASS_NAME);
            File files[] = new File(folder).listFiles(new FilenameFilter()
            {

                @Override
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".gz");
                }
            });
            for(File f : files) // for every indexer file
            {
                String indexerFieldName = f.getName().substring(0, f.getName().lastIndexOf("."));
                String values[] = Utils.readLines(f.getAbsolutePath());
                if(!indexerFieldName.contains("Map"))
                {
                    Indexer<String> indexer = (Indexer<String>) cl.getDeclaredField(indexerFieldName).get(this);
                    for(String value : values)
                    {
                        indexer.getIndex(value);
                    }
                }
                else
                {
                    Map<Integer, Integer> map = (HashMap) cl.getDeclaredField(indexerFieldName).get(this);
                    for(String value : values)
                    {
                        String[] ar = value.split("\t");
                        map.put(Integer.valueOf(ar[0]), Integer.valueOf(ar[1]));
                    }
                }
            }
        } 
        catch (ClassNotFoundException ex)
        {
            LogInfo.error(ex);
        } catch (NoSuchFieldException ex)
        {
            LogInfo.error(ex);
        } catch (SecurityException ex)
        {
            LogInfo.error(ex);
        } catch (IllegalArgumentException ex)
        {
            LogInfo.error(ex);
        } catch (IllegalAccessException ex)
        {
            LogInfo.error(ex);
        }
        
//        return featureIndexers;
    }
    
    private Indexer<String> newIndexer()
    {
        Indexer<String> indexer = new Indexer<String>();
        indexer.getIndex("U");
        return indexer;
    }
    
    @Override
    public int getIndexOfFeatureSafe(int fType, String value)
    {
        switch(fType)
        {
            case ExtractFeatures.FEAT_ELEM_TREE : return getIndexOfFeatureSafe(elemTreeIndexer, value, true);
            case ExtractFeatures.FEAT_PREV_ELEM_TREE : return getIndexOfFeatureSafe(prevElemTreeIndexer, value, true);
            case ExtractFeatures.FEAT_ELEM_TREE_BIGRAM : return getIndexOfFeatureSafe(elemTreeBigramIndexer, value, true);
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX : return getIndexOfFeatureSafe(elemTreeUnlexIndexer, value, true);
            case ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX : return getIndexOfFeatureSafe(prevElemTreeUnlexIndexer, value, true);
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM : return getIndexOfFeatureSafe(elemTreeUnlexBigramIndexer, value, true);
            case ExtractFeatures.FEAT_INTEGRATION_POINT : return getIndexOfFeatureSafe(integrationPointIndexer, value, true);
            case ExtractFeatures.FEAT_FRINGE : return getIndexOfFeatureSafe(fringeIndexer, value, true);
                
            case ExtractFeatures.FEAT_CO_PAR : return getIndexOfFeatureSafe(coParIndexer, value, true);
            case ExtractFeatures.FEAT_CO_LEN_PAR : return getIndexOfFeatureSafe(coLenParIndexer, value, true);
            case ExtractFeatures.FEAT_HEAVY : return getIndexOfFeatureSafe(heavyIndexer, value, true);
            case ExtractFeatures.FEAT_NEIGHBOURS_L1 : return getIndexOfFeatureSafe(neighboursL1Indexer, value, true);
            case ExtractFeatures.FEAT_NEIGHBOURS_L2 : return getIndexOfFeatureSafe(neighboursL2Indexer, value, true);
            case ExtractFeatures.FEAT_IP_ELEM_TREE : return getIndexOfFeatureSafe(ipElemTreeIndexer, value, true);
            case ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX : return getIndexOfFeatureSafe(ipElemTreeUnlexIndexer, value, true);
            case ExtractFeatures.FEAT_WORD_L2 : return getIndexOfFeatureSafe(wordL2Indexer, value, true);
            case ExtractFeatures.FEAT_WORD_L3 : return getIndexOfFeatureSafe(wordL3Indexer, value, true);           
            
            case ExtractFeatures.FEAT_SRL_TRIPLES : return getIndexOfFeatureSafe(srlTripleIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_TRIPLES_POS : return getIndexOfFeatureSafe(srlTripleUnlexIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES : return getIndexOfFeatureSafe(srlTripleIncompleteIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS : return getIndexOfFeatureSafe(srlTripleIncompleteUnlexIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_ROLE : return getIndexOfFeatureSafe(roleIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_DEPENDENCY : return getIndexOfFeatureSafe(srlDependencyIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_DEPENDENCY_POS : return getIndexOfFeatureSafe(srlDependencyUnlexIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_ROLE_PRED : return getIndexOfFeatureSafe(srlRolePredIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_ROLE_PRED_POS : return getIndexOfFeatureSafe(srlRolePredPosIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_ROLE_ARG : return getIndexOfFeatureSafe(srlRoleArgIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_ROLE_ARG_POS : return getIndexOfFeatureSafe(srlRoleArgPosIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_PRED : return getIndexOfFeatureSafe(srlPredIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_PRED_POS : return getIndexOfFeatureSafe(srlPredPosIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_ARG : return getIndexOfFeatureSafe(srlArgIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_ARG_POS : return getIndexOfFeatureSafe(srlArgPosIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_FRAME : return getIndexOfFeatureSafe(srlFrameIndexer, value, true);
            case ExtractFeatures.FEAT_SRL_FRAME_POS : return getIndexOfFeatureSafe(srlFramePosIndexer, value, true);
            default : return Integer.MIN_VALUE;   
        }
    }

    @Override
    public int getIndexOfFeature(int fType, String value)
    {
        switch(fType)
        {
            case ExtractFeatures.FEAT_ELEM_TREE : return getIndexOfFeature(elemTreeIndexer, value);
            case ExtractFeatures.FEAT_PREV_ELEM_TREE : return getIndexOfFeature(prevElemTreeIndexer, value);
            case ExtractFeatures.FEAT_ELEM_TREE_BIGRAM : return getIndexOfFeature(elemTreeBigramIndexer, value);
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX : return getIndexOfFeature(elemTreeUnlexIndexer, value);
            case ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX : return getIndexOfFeature(prevElemTreeUnlexIndexer, value);
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM : return getIndexOfFeature(elemTreeUnlexBigramIndexer, value);
            case ExtractFeatures.FEAT_INTEGRATION_POINT : return getIndexOfFeature(integrationPointIndexer, value);
            case ExtractFeatures.FEAT_FRINGE : return getIndexOfFeature(fringeIndexer, value);
            
            case ExtractFeatures.FEAT_CO_PAR : return getIndexOfFeature(coParIndexer, value);
            case ExtractFeatures.FEAT_CO_LEN_PAR : return getIndexOfFeature(coLenParIndexer, value);
            case ExtractFeatures.FEAT_HEAVY : return getIndexOfFeature(heavyIndexer, value);
            case ExtractFeatures.FEAT_NEIGHBOURS_L1 : return getIndexOfFeature(neighboursL1Indexer, value);
            case ExtractFeatures.FEAT_NEIGHBOURS_L2 : return getIndexOfFeature(neighboursL2Indexer, value);
            case ExtractFeatures.FEAT_IP_ELEM_TREE : return getIndexOfFeature(ipElemTreeIndexer, value);
            case ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX : return getIndexOfFeature(ipElemTreeUnlexIndexer, value);
            case ExtractFeatures.FEAT_WORD_L2 : return getIndexOfFeature(wordL2Indexer, value);
            case ExtractFeatures.FEAT_WORD_L3 : return getIndexOfFeature(wordL3Indexer, value);            
            
            case ExtractFeatures.FEAT_SRL_TRIPLES : return getIndexOfFeature(srlTripleIndexer, value);
            case ExtractFeatures.FEAT_SRL_TRIPLES_POS : return getIndexOfFeature(srlTripleUnlexIndexer, value);
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES : return getIndexOfFeature(srlTripleIncompleteIndexer, value);
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS : return getIndexOfFeature(srlTripleIncompleteUnlexIndexer, value);
            case ExtractFeatures.FEAT_SRL_ROLE : return getIndexOfFeature(roleIndexer, value);
            case ExtractFeatures.FEAT_SRL_DEPENDENCY : return getIndexOfFeature(srlDependencyIndexer, value);
            case ExtractFeatures.FEAT_SRL_DEPENDENCY_POS : return getIndexOfFeature(srlDependencyUnlexIndexer, value);
            case ExtractFeatures.FEAT_SRL_ROLE_PRED : return getIndexOfFeature(srlRolePredIndexer, value);
            case ExtractFeatures.FEAT_SRL_ROLE_PRED_POS : return getIndexOfFeature(srlRolePredPosIndexer, value);
            case ExtractFeatures.FEAT_SRL_ROLE_ARG : return getIndexOfFeature(srlRoleArgIndexer, value);
            case ExtractFeatures.FEAT_SRL_ROLE_ARG_POS : return getIndexOfFeature(srlRoleArgPosIndexer, value);
            case ExtractFeatures.FEAT_SRL_PRED : return getIndexOfFeature(srlPredIndexer, value);
            case ExtractFeatures.FEAT_SRL_PRED_POS : return getIndexOfFeature(srlPredPosIndexer, value);
            case ExtractFeatures.FEAT_SRL_ARG : return getIndexOfFeature(srlArgIndexer, value);
            case ExtractFeatures.FEAT_SRL_ARG_POS : return getIndexOfFeature(srlArgPosIndexer, value);
            case ExtractFeatures.FEAT_SRL_FRAME : return getIndexOfFeature(srlFrameIndexer, value);    
            case ExtractFeatures.FEAT_SRL_FRAME_POS : return getIndexOfFeature(srlFramePosIndexer, value);    
            default : return Integer.MIN_VALUE;   
        }
    }

    @Override
    public Indexer<String> getIndexer(int fType)
    {
        switch(fType)
        {            
            case ExtractFeatures.FEAT_ELEM_TREE : return elemTreeIndexer;
            case ExtractFeatures.FEAT_PREV_ELEM_TREE : return prevElemTreeIndexer;
            case ExtractFeatures.FEAT_ELEM_TREE_BIGRAM : return elemTreeBigramIndexer;
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX : return elemTreeUnlexIndexer;
            case ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX : return prevElemTreeUnlexIndexer;
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM : return elemTreeUnlexBigramIndexer;
            case ExtractFeatures.FEAT_INTEGRATION_POINT : return integrationPointIndexer;
            case ExtractFeatures.FEAT_FRINGE : return fringeIndexer;
                
            case ExtractFeatures.FEAT_CO_PAR : return coParIndexer;
            case ExtractFeatures.FEAT_CO_LEN_PAR : return coLenParIndexer;
            case ExtractFeatures.FEAT_HEAVY : return heavyIndexer;
            case ExtractFeatures.FEAT_NEIGHBOURS_L1 : return neighboursL1Indexer;
            case ExtractFeatures.FEAT_NEIGHBOURS_L2 : return neighboursL2Indexer;
            case ExtractFeatures.FEAT_IP_ELEM_TREE : return ipElemTreeIndexer;
            case ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX : return ipElemTreeUnlexIndexer;
            case ExtractFeatures.FEAT_WORD_L2 : return wordL2Indexer;
            case ExtractFeatures.FEAT_WORD_L3 : return wordL3Indexer;
            
            case ExtractFeatures.FEAT_SRL_TRIPLES : return srlTripleIndexer;
            case ExtractFeatures.FEAT_SRL_TRIPLES_POS : return srlTripleUnlexIndexer;
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES : return srlTripleIncompleteIndexer;
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS : return srlTripleIncompleteUnlexIndexer;
            case ExtractFeatures.FEAT_SRL_ROLE : return roleIndexer;
            case ExtractFeatures.FEAT_SRL_DEPENDENCY : return srlDependencyIndexer;
            case ExtractFeatures.FEAT_SRL_DEPENDENCY_POS : return srlDependencyUnlexIndexer;
            case ExtractFeatures.FEAT_SRL_ROLE_PRED : return srlRolePredIndexer;
            case ExtractFeatures.FEAT_SRL_ROLE_PRED_POS : return srlRolePredPosIndexer;
            case ExtractFeatures.FEAT_SRL_ROLE_ARG : return srlRoleArgIndexer;
            case ExtractFeatures.FEAT_SRL_ROLE_ARG_POS : return srlRoleArgPosIndexer;
            case ExtractFeatures.FEAT_SRL_PRED : return srlPredIndexer;
            case ExtractFeatures.FEAT_SRL_PRED_POS : return srlPredPosIndexer;
            case ExtractFeatures.FEAT_SRL_ARG : return srlArgIndexer;
            case ExtractFeatures.FEAT_SRL_ARG_POS : return srlArgPosIndexer;
            case ExtractFeatures.FEAT_SRL_FRAME : return srlFrameIndexer;
            case ExtractFeatures.FEAT_SRL_FRAME_POS : return srlFramePosIndexer;
                
            default : return null;
        }
    }
    
    public int getIndexerSize(int fType)
    {
        switch(fType)
        {            
            case ExtractFeatures.FEAT_ELEM_TREE : return elemTreeIndexer.size();
            case ExtractFeatures.FEAT_PREV_ELEM_TREE : return prevElemTreeIndexer.size();
            case ExtractFeatures.FEAT_ELEM_TREE_BIGRAM : return elemTreeBigramIndexer.size();
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX : return elemTreeUnlexIndexer.size();
            case ExtractFeatures.FEAT_PREV_ELEM_TREE_UNLEX : return prevElemTreeUnlexIndexer.size();
            case ExtractFeatures.FEAT_ELEM_TREE_UNLEX_BIGRAM : return elemTreeUnlexBigramIndexer.size();
            case ExtractFeatures.FEAT_INTEGRATION_POINT : return integrationPointIndexer.size();
            case ExtractFeatures.FEAT_FRINGE : return fringeIndexer.size();
            
            case ExtractFeatures.FEAT_CO_PAR : return coParIndexer.size();
            case ExtractFeatures.FEAT_CO_LEN_PAR : return coLenParIndexer.size();
            case ExtractFeatures.FEAT_HEAVY : return heavyIndexer.size();
            case ExtractFeatures.FEAT_NEIGHBOURS_L1 : return neighboursL1Indexer.size();
            case ExtractFeatures.FEAT_NEIGHBOURS_L2 : return neighboursL2Indexer.size();
            case ExtractFeatures.FEAT_IP_ELEM_TREE : return ipElemTreeIndexer.size();
            case ExtractFeatures.FEAT_IP_ELEM_TREE_UNLEX : return ipElemTreeUnlexIndexer.size();
            case ExtractFeatures.FEAT_WORD_L2 : return wordL2Indexer.size();
            case ExtractFeatures.FEAT_WORD_L3 : return wordL3Indexer.size();
            
            case ExtractFeatures.FEAT_SRL_TRIPLES : return srlTripleIndexer.size();
            case ExtractFeatures.FEAT_SRL_TRIPLES_POS : return srlTripleUnlexIndexer.size();
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES :  return srlTripleIncompleteIndexer.size();
            case ExtractFeatures.FEAT_SRL_INCOMPLETE_TRIPLES_POS : return srlTripleIncompleteUnlexIndexer.size();
            case ExtractFeatures.FEAT_SRL_ROLE : return roleIndexer.size();
            case ExtractFeatures.FEAT_SRL_DEPENDENCY : return srlDependencyIndexer.size();
            case ExtractFeatures.FEAT_SRL_DEPENDENCY_POS : return srlDependencyUnlexIndexer.size();
            case ExtractFeatures.FEAT_SRL_ROLE_PRED : return srlRolePredIndexer.size();
            case ExtractFeatures.FEAT_SRL_ROLE_PRED_POS : return srlRolePredPosIndexer.size();
            case ExtractFeatures.FEAT_SRL_ROLE_ARG : return srlRoleArgIndexer.size();
            case ExtractFeatures.FEAT_SRL_ROLE_ARG_POS : return srlRoleArgPosIndexer.size();
            case ExtractFeatures.FEAT_SRL_PRED : return srlRolePredIndexer.size();
            case ExtractFeatures.FEAT_SRL_PRED_POS : return srlRolePredPosIndexer.size();
            case ExtractFeatures.FEAT_SRL_ARG : return srlRoleArgIndexer.size();
            case ExtractFeatures.FEAT_SRL_ARG_POS : return srlRoleArgPosIndexer.size();
            case ExtractFeatures.FEAT_SRL_FRAME : return srlFrameIndexer.size();
            case ExtractFeatures.FEAT_SRL_FRAME_POS : return srlFramePosIndexer.size();
            default : return 0;
        }
    }
    
    public int getRoleIndex(String label, boolean train)
    {
        return getIndexOfFeature(roleIndexer, label, train, true);
    }
    
    public int getRoleIndex(String label)
    {
        return roleIndexer.getIndex(label);
    }

    public String getRole(int id)
    {
        return roleIndexer.getObject(id);
    }
    
    public Indexer<String> getRoleIndexer()
    {
        return roleIndexer;
    }
    
    public int getWordIndex(String word, boolean train)
    {
        return getIndexOfFeature(wordIndexer, word, train, true);
    }
    
    public int getWordIndex(String word)
    {
        return wordIndexer.getIndex(word);
    }
    
    public int getWordIndexSafe(String word)
    {
        return getIndexOfFeatureSafe(wordIndexer, word, true);
    }
    
    public String getWord(int id)
    {        
        return id < wordIndexer.size() ? wordIndexer.getObject(id) : "U";        
    }

    public Indexer<String> getWordIndexer()
    {
        return wordIndexer;
    }
    
    public int getPosIndex(String pos, boolean train)
    {
        return getIndexOfFeature(posIndexer, pos, train, true);
    }
    
    public int getPosIndex(String pos)
    {
        return posIndexer.getIndex(pos);
    }

    public int getPosIndexSafe(String pos)
    {
        return getIndexOfFeature(posIndexer, pos);
    }
    
    public String getPos(int id)
    {
        return posIndexer.getObject(id);
    }        
    
    public int getCategoryIndex(String cat, boolean train)
    {
        return getIndexOfFeature(categoryIndexer, cat, train, true);
    }
    
    public int getCategoryIndex(String cat)
    {
        return categoryIndexer.getIndex(cat);
    }
    
    public int getCategoryIndexSafe(String cat)
    {
        return getIndexOfFeature(categoryIndexer, cat);
    }
    
    public String getCategory(int id)
    {
        return categoryIndexer.getObject(id);
    }
    
    public int getNumOfCategories()
    {
        return categoryIndexer.size();
    }
    
    public int getWordLemmatizedIndex(String word, boolean train)
    {
        return getIndexOfFeature(wordLemmatizedIndexer, word, train, true);
    }
    
    public int getWordLemmatizedIndex(String word)
    {
        return wordLemmatizedIndexer.getIndex(word);
    }
    
    public int getWordLemmatizedIndexSafe(String word)
    {
        return getIndexOfFeature(wordLemmatizedIndexer, word);
    }
    
    public String getWordLemmatized(int id)
    {
        return wordLemmatizedIndexer.getObject(id);
    }

    public Map<Integer, Integer> getWord2LemmaMap()
    {
        return word2LemmaMap;
    }
        
    public String srlTripleToString(int id)
    {
        String triple = srlTripleIndexer.getObject(id);
        if(triple.equals("U"))
            return triple;        
        String[] ar = triple.split(",");             
        return String.format("<%s,%s,%s>", getRole(Integer.valueOf(ar[0])), getWord(Integer.valueOf(ar[1])), getWord(Integer.valueOf(ar[2])));
    }
    
    public String[] srlTripleToArray(int id)
    {
        String triple = srlTripleIndexer.getObject(id);
        if(triple.equals("U"))
            return new String[]{triple};        
        String[] ar = triple.split(",");        
        return new String[] {getRole(Integer.valueOf(ar[0])), getWord(Integer.valueOf(ar[1])), getWord(Integer.valueOf(ar[2]))};
    }
    
    public String srlTriplePosToString(int id)
    {
        String triple = srlTripleUnlexIndexer.getObject(id);
        if(triple.equals("U"))
            return triple;        
        String[] ar = triple.split(",");        
        return String.format("<%s,%s,%s>", getRole(Integer.valueOf(ar[0])), getPos(Integer.valueOf(ar[1])), getPos(Integer.valueOf(ar[2])));
    }
    
    public String[] elemTreesStringArray()
    {
        return elemTreeIndexer.getObjects().toArray(new String[0]);        
    }
    
    public String[] elemTreesUnlexStringArray()
    {
        return elemTreeUnlexIndexer.getObjects().toArray(new String[0]);        
    }
    
    public String[] srlTriplesStringArray()
    {
        String[] out = new String[srlTripleIndexer.size()];
        for(int i = 0 ; i < out.length; i++)
        {
            out[i]  = srlTripleToString(i);
        }
        return out;
    }
    
    public String[][] srlTriplesArraysArray()
    {
        String[][] out = new String[srlTripleIndexer.size()][3];
        for(int i = 0 ; i < out.length; i++)
        {
            out[i]  = srlTripleToArray(i);
        }
        return out;
    }
    
    public String[] srlTriplesPosStringArray()
    {
        String[] out = new String[srlTripleUnlexIndexer.size()];
        for(int i = 0 ; i < out.length; i++)
        {
            out[i]  = srlTriplePosToString(i);
        }
        return out;
    }
    
    public String[] wordsToStringArray()
    {
        String[] out = new String[wordIndexer.size()];
        return wordIndexer.getObjects().toArray(out);
    }
    
    public Map<String, String[]> flattenedIndexers()
    {
        Map<String, String[]> out = new HashMap<String, String[]>();
        try
        {                                    
            Field[] fields = Class.forName(CLASS_NAME).getDeclaredFields();            
            for(Field field : fields)
            {
                if(field.getType() == Indexer.class)
                {
                    String indexerName = field.getName();                    
                    Indexer<String> indexer = (Indexer<String>) field.get(this);
                    out.put(indexerName, indexer.toArray(new String[0]));                    
                }
                else if(field.getType() == Map.class)
                {
                    String indexerName = field.getName();                    
                    Map<Integer, Integer> map = (HashMap) field.get(this);
                    String[] lines = new String[map.size()];
                    int i = 0;
                    for(Entry<Integer, Integer> entry : map.entrySet())
                        lines[i++] = String.format("%s\t%s", entry.getKey(), entry.getValue());
                    out.put(indexerName, lines);
                }                
            }
        } 
        catch (ClassNotFoundException ex)
        {
            LogInfo.error(ex);
        } catch (IllegalArgumentException ex)
        {
            LogInfo.error(ex);
        } 
        catch (IllegalAccessException ex)
        {
            LogInfo.error(ex);
        }
        return out;
    }
    
    public void saveFlattenedFeatureIndexers(String directory)
    {
        new File(directory).mkdir();
        for(Entry<String, String[]> entry : flattenedIndexers().entrySet())
        {
            Utils.writeLines(directory + "/"  + entry.getKey() + ".gz", entry.getValue());
        }
    }            
}
