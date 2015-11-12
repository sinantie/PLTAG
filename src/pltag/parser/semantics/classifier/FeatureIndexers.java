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

import fig.basic.Indexer;
import java.io.Serializable;

/**
 *
 * @author sinantie
 */
public class FeatureIndexers extends AbstractFeatureIndexers implements Serializable
{

    private static final long serialVersionUID = -1L;
    
    //public enum FeatureType {predPOS, predLemma, sense, argWord, argPOS, position, leftSiblingPOS}
    public static final int FEAT_PRED_POS = 0, 
                            FEAT_PRED_LEMMA = 1, 
//                            FEAT_SENSE = 2, 
                            FEAT_ARG_WORD = 2, 
                            FEAT_ARG_POS = 3, 
                            FEAT_POSITION = 4,
                            FEAT_OPERATION_PATH = 5,
                            FEAT_PRED_ETREE = 6,
                            FEAT_ARG_ETREE = 7,
                            FEAT_INTEG_POINT = 8,
                            FEAT_FRINGE = 9;
//                            FEAT_LEFT_SIBLING_POS = 6;
    // Maps from strings to integers: useful for doing 1-of-K encoding for categorical entries.        
    private final Indexer<String> predPosIndexer, predLemmaIndexer, senseIndexer, // predicate-related features
            argWordIndexer, argPosIndexer, //leftSiblingPosIndexer,           // argument-related features
            predElemTreeIndexer, argElemTreeIndexer,                        // syntax-related features
            integPointIndexer, fringeIndexer, operationPathIndexer,           // syntax-related features
            roleIndexer;                                                    // label, i.e., SRL
    
    public FeatureIndexers()
    {
        predPosIndexer = new Indexer<String>();
        predPosIndexer.getIndex("<UNK>");
        predLemmaIndexer = new Indexer<String>();
        predLemmaIndexer.getIndex("<UNK>");
        senseIndexer = new Indexer<String>();
        senseIndexer.getIndex("<UNK>");
        argWordIndexer = new Indexer<String>();
        argWordIndexer.getIndex("<UNK>");
        argPosIndexer = new Indexer<String>();
        argPosIndexer.getIndex("<UNK>");
//        leftSiblingPosIndexer = new Indexer<String>();
//        leftSiblingPosIndexer.getIndex("<UNK>");
        predElemTreeIndexer = new Indexer<String>();
        argElemTreeIndexer = new Indexer<String>();
        integPointIndexer = new Indexer<String>();
        integPointIndexer.getIndex("<UNK>");
        fringeIndexer = new Indexer<String>();
        fringeIndexer.getIndex("<UNK>");
        operationPathIndexer = new Indexer<String>();
        operationPathIndexer.getIndex("<UNK>");
        roleIndexer = new Indexer<String>();
    }
        
    
    /**
     * Get unique id of value from selected feature type. We make sure we return
     * the id of the special <UNK> placeholder for unseen values (Used during prediction 
     * when feature indexers have already been filled in during training). 
     * @param fType
     * @param value
     * @return 
     */
    @Override
    public int getIndexOfFeatureSafe(int fType, String value)
    {
        switch(fType)
        {
            case FEAT_PRED_POS : return getIndexOfFeatureSafe(predPosIndexer, value, false);
            case FEAT_PRED_LEMMA : return getIndexOfFeatureSafe(predLemmaIndexer, value, false);
//            case FEAT_SENSE : return getIndexOfFeatureSafe(senseIndexer, value);
            case FEAT_ARG_WORD : return getIndexOfFeatureSafe(argWordIndexer, value, false);
            case FEAT_ARG_POS : return getIndexOfFeatureSafe(argPosIndexer, value, false);
//            case FEAT_LEFT_SIBLING_POS : return getIndexOfFeatureSafe(leftSiblingPosIndexer, value);
            case FEAT_PRED_ETREE : return getIndexOfFeatureSafe(predElemTreeIndexer, value, false);
            case FEAT_ARG_ETREE : return getIndexOfFeatureSafe(argElemTreeIndexer, value, false);
            case FEAT_INTEG_POINT : return getIndexOfFeatureSafe(integPointIndexer, value, false);
            case FEAT_FRINGE : return getIndexOfFeatureSafe(fringeIndexer, value, false);
            case FEAT_OPERATION_PATH : return getIndexOfFeatureSafe(operationPathIndexer, value, false);
            default: return Integer.MIN_VALUE;
        }
    }        
    
    /**
     * Get unique id of value from selected feature type. The corresponding indexer
     * is automatically resized.
     * @param fType
     * @param value
     * @return 
     */
    @Override
    public int getIndexOfFeature(int fType, String value)
    {
        switch(fType)
        {
            case FEAT_PRED_POS : return getIndexOfFeature(predPosIndexer, value);
            case FEAT_PRED_LEMMA : return getIndexOfFeature(predLemmaIndexer, value);
//            case FEAT_SENSE : return getIndexOfFeature(senseIndexer, value);
            case FEAT_ARG_WORD : return getIndexOfFeature(argWordIndexer, value);
            case FEAT_ARG_POS : return getIndexOfFeature(argPosIndexer, value);
//            case FEAT_LEFT_SIBLING_POS : return getIndexOfFeature(leftSiblingPosIndexer, value);                
            case FEAT_PRED_ETREE : return getIndexOfFeature(predElemTreeIndexer, value);
            case FEAT_ARG_ETREE : return getIndexOfFeature(argElemTreeIndexer, value);
            case FEAT_INTEG_POINT : return getIndexOfFeature(integPointIndexer, value);
            case FEAT_FRINGE : return getIndexOfFeature(fringeIndexer, value);                
            case FEAT_OPERATION_PATH : return getIndexOfFeature(operationPathIndexer, value);                
            default: return Integer.MIN_VALUE;
        }
    }        
    
    @Override
    public Indexer<String> getIndexer(int fType)
    {
        switch(fType)
        {
            case FEAT_PRED_POS : return predPosIndexer;
            case FEAT_PRED_LEMMA : return predLemmaIndexer;
//            case FEAT_SENSE : return senseIndexer;
            case FEAT_ARG_WORD : return argWordIndexer;
            case FEAT_ARG_POS : return argPosIndexer;
            case FEAT_PRED_ETREE : return predElemTreeIndexer;
            case FEAT_ARG_ETREE : return argElemTreeIndexer;
            case FEAT_INTEG_POINT : return integPointIndexer;
            case FEAT_FRINGE : return fringeIndexer;
            case FEAT_OPERATION_PATH : return operationPathIndexer;
//            case FEAT_LEFT_SIBLING_POS : return leftSiblingPosIndexer;
            default: return null;
        }
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
           
}
