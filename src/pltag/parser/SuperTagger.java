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

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.map.MultiValueMap;


import pltag.corpus.ElementaryStringTree;

public class SuperTagger
{

    private static final int supertagThreshold = 20;
    private final Map<String, Integer> freqStruct;
    private final Map<SuperTagElement, Integer> freqFringe;
    private final Map<ElementaryStringTree, String> predTreeFringeMap;
    private final Map<ElementaryStringTree, String> predTreeStructMap;
    private final Map<ElementaryStringTree, String> predTreeMainLeafMap;
//    private final LRUMap<SuperTagStructElement, Double> superTagStructCache;
    private final ConcurrentLinkedHashMap<SuperTagElement, Double> superTagStructCache, superTagElementCache;
    
    public SuperTagger(Map<String, Integer> freqStruct,
            Map<SuperTagElement, Integer> freqFringe, boolean estimateInterpol,
            Map<ElementaryStringTree, String> predTreeFringeMap,
            Map<ElementaryStringTree, String> predTreeStructMap,
            Map<ElementaryStringTree, String> predTreeMainLeafMap)
    {
        this.freqStruct = freqStruct;
        this.freqFringe = freqFringe;
        if (estimateInterpol)
        {
            estimateInterpolation();
        }
        else
        {
            //SuperTagElement.setInterpol(0.8150451950523311, 0.08875637920595104, 0.09619842574171784);//supertag all;
            SuperTagElement.setInterpol(0.9012691986830528, 0.08005882103417558, 0.01867198028277168);
            //SuperTagStructElement.setInterpol(1.0, 0.0);
        }
        this.predTreeFringeMap = predTreeFringeMap;
        this.predTreeStructMap = predTreeStructMap;
        this.predTreeMainLeafMap = predTreeMainLeafMap;
//        superTagStructCache = new LRUMap<SuperTagStructElement, Double>(100000);
        superTagStructCache = new ConcurrentLinkedHashMap.Builder<SuperTagElement, Double>().maximumWeightedCapacity(10000).build();
        superTagElementCache = new ConcurrentLinkedHashMap.Builder<SuperTagElement, Double>().maximumWeightedCapacity(10000).build();
    }

    private void estimateInterpolation()
    {
//        SuperTagElement.estimateInterpol(freqFringe);
        //SuperTagStructElement.estimateInterpol(bigFreqMapStruct);

    }

    @SuppressWarnings("unchecked")
    public Collection<ElementaryStringTree> superTag(
            Collection<ElementaryStringTree> trees, ChartEntry chartEntry, String nextPosCat)
    {
        MultiValueMap<Double, ElementaryStringTree> mvm = new MultiValueMap();//<Double, ElementaryStringTree>
//		boolean predTree = true;
//		if (nextPosCat.equals("")) {
//			//nextPosCat = chartEntry.getPrevPOStags();//
////			predTree = false;
//			return trees; // 
//		}
        //else return trees;
        Fringe fringe = chartEntry.getTreeState().getFringe();
        for (ElementaryStringTree tree : trees)
        {
//			Fringe fringe = chartEntry.getTreeState().getFringe();
            // do this in combineTrees?
//			String predTreeStruct = tree.getUnlexStruct(tree.getRoot());//getTreeString();//.substring(tree.getTreeString().indexOf("\t")+1);
            String predTreeStruct = predTreeStructMap.get(tree);//getTreeString();//.substring(tree.getTreeString().indexOf("\t")+1);
            String predTreeFringeCat = predTreeFringeMap.get(tree);
//			String predTreeFringeCat;
//			if (predTree) predTreeFringeCat = this.catFringeMap.get(tree);
//			else {
//				short[] nullarray = new short[2];
//				nullarray[0] = -99;
//				nullarray[1] = -99;
//				HashMap<Short, Node> nodekernelmap = new HashMap<Short, Node>();
//				predTreeFringeCat = TreeState.getAuxFringe(tree, -1, nullarray, nodekernelmap).toCatString();
//			}
//			String currentPosCat = tree.getMainLeaf(tree.getRoot());
            String currentPosCat = predTreeMainLeafMap.get(tree);
            if (currentPosCat.contains("\t"))
            {
                currentPosCat = currentPosCat.substring(0, currentPosCat.indexOf("\t"));
            }
            if (currentPosCat.contains(" "))
            {
                currentPosCat = currentPosCat.substring(0, currentPosCat.indexOf(" "));
            }

            SuperTagStructElement stse = new SuperTagStructElement(predTreeStruct, predTreeFringeCat, currentPosCat);
            SuperTagElement ste = new SuperTagElement(fringe, nextPosCat, currentPosCat, predTreeFringeCat);
            Double s2;
            double s1 = stse.getSmoothedProb(freqStruct);
//            s1 = superTagElementCache.get(ste);
//            if(s1 == null)
//            {
//                s1 = stse.getSmoothedProb(freqStruct);
//                superTagStructCache.put(ste, s1);
//            }
            s2 = superTagElementCache.get(ste);
            if(s2 == null)
            {                
                s2 = ste.getSmoothedProb(freqFringe);
                superTagElementCache.put(ste, s2);
            }            
            //if (s2>1.0340192327577293E-2){
            //	System.out.println(s1+"\t"+s2+"\t"+stse+"\t"+ste+"\n");
            //	ste.getSmoothedProb(bigFreqMapFringe);
            //}
//            Double prob = s1 * s2;
            mvm.put(s1 * s2, tree);
        }
        ArrayList<ElementaryStringTree> resultlist = new ArrayList<ElementaryStringTree>();
        Double[] keys = ((Set<Double>) mvm.keySet()).toArray(new Double[mvm.keySet().size()]);
        Arrays.sort(keys);
        int i = keys.length - 1;
        while (i >= 0 && resultlist.size() < supertagThreshold)
        {
            resultlist.addAll(mvm.getCollection(keys[i]));
//			System.out.print(keys[i]+" ("+mvm.getCollection(keys[i]).size()+"); ");
            i--;
        }
        //superTag(resultlist, chartEntry, nextPosCat, true);
//		System.out.print("(" + resultlist.size()+" shadow trees)");
/*		if (resultlist.size() > 20){
         System.out.print("(" + resultlist.size()+" shadow trees)");
         superTag(resultlist, chartEntry, nextPosCat, true);
         }*/
        return resultlist;
    }
}
