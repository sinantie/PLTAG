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

import java.util.List;

public class GuessArgMod {
	/**
	 * heuristics about whether something is an argument or a modifier.
	 */
	
	private boolean assigned = false;
	
	public GuessArgMod(LexEntry lexentry){
		TagNode root = lexentry.getRoot();
		
		String rc = root.getFullCategory();
		String rootcategory = root.getCategory();
		
		if (root.getNodeID() == 1){
//		if (root.getNodeID().equals("1")){
			assigned = makeArgument(lexentry);return;
		}

		//copula treatment: np-subj is always argument, and copulas should also be treated as arguments;
		if (rc.startsWith("NP-SBJ") || rc.equals("COP")|| rc.endsWith("-PRD")){
			/*if (rc.endsWith("-PRD") && root.hasParent() ){
				root.getParent().getHeadChild().makeNotHead();
				root.makeHead();
			}*/
			assigned = makeArgument(lexentry);return;
		}
		if (rc.equals("CCSIM")){
			assigned = makeModifier(lexentry);return;
		}
		if (root.hasParent()){
			TagNode parent = root.getParent();

			//SINV topicalized VP
			if (rc.startsWith("VP-TPC") && parent.getCategory().equals("SINV")){
				assigned = makeArgument(lexentry);return;
			}
			// copula
			if (parent.getFullCategory().endsWith("-PRD") && TagCorpus.equivCat(parent.getCategory(), rootcategory, null)){
				assigned = makeArgument(lexentry);return;
			}
			//coordination
			if (containsCC(parent.getChildlist()) 
					&& ! containsCC(root.getChildlist())
					&& parent.getHeadChild()!= null
					){
				//return;
				///*
				//sentence-initial modifier:
				TagNode node = parent.getChildlist().get(0);
				boolean isSentinitialmod = true;
				while(node != null && !node.getCategory().matches("C(C|ONJP)")){
					if (node.getCategory().matches("[A-Z]+")){
						isSentinitialmod = false;
					}
					node = node.getRight();
				}
				if (isSentinitialmod && !(node.isLeaf() && ((LeafNode) node).getLeaf().matches("((N|n)?(E|e)ither)|((B|b)oth)"))){
					node.setCategory("CCSIM");
				}
				if (parent.getHeadChild().getLexEntry()!= null && lexentry != parent.getHeadChild().getLexEntry()){
					boolean as = new GuessArgMod(parent.getHeadChild().getLexEntry()).hasHeuristic();
				
					if (as && parent.getHeadChild().isArgument()){
						assigned = makeArgument(lexentry);return;
					}
					else if (as && !parent.getHeadChild().isArgument()){
						assigned = makeModifier(lexentry);return;
					}//*/
				}
			}
			//determiner
			if (parent.getCategory().matches("(NP)|(NML)|(NAC)|(NX)")&& rootcategory.matches("(DT)|(PRP[$])")){
				assigned = makeArgument(lexentry);return;				
			}
			if (parent.getCategory().matches("(NP)|(NML)|(NAC)|(NX)")&& rootcategory.matches("(QP)|(CD)")){
				if (parent.getHeadChild().getCategory().equals("$")){
					boolean siblingCD = false;
					for (TagNode children : parent.getChildlist()){
						if (children.getCategory().equals("CD")) {
							siblingCD=true; 
							break;
						}
						else if(children.getCategory().equals("QP")){
							break;
						}
					}
					if (siblingCD){
						assigned = makeModifier(lexentry); return;
					}
						assigned = makeArgument(lexentry);return;
				}
				else assigned = makeModifier(lexentry); return;
			}
			if (parent.getCategory().equals("QP")&& rootcategory.equals("QP")){
				assigned = makeModifier(lexentry);return;				
			}
			//more than three _times_
			if (parent.getCategory().equals("QP")&& rootcategory.matches("NNS|CD|$")){
				assigned = makeArgument(lexentry);return;	
			}
//			//NPs modified with PP, ADJ or RCs; PP arguments should be captured by nombank annotation and not be solved by guessing.
			if (parent.getCategory().matches("(NP)|(NML)|(NAC)|(NN.*)|(NX)")
					&& (rootcategory.matches("(JJ.*)|(NN.*)|(ADJP)|(NML)|(NP)|(VBN)|(VBG)|(VP)|(CD)|(NAC)|(PRN)") //parentheticals
							||rootcategory.equals("PP") ||rootcategory.equals("SBAR"))
					){
				if (!containsCC(parent.getChildlist())){
					assigned = makeModifier(lexentry);return;				
				}
			}
			//prepositional phrases need an NP argument
			if (parent.getCategory().equals("PP") && 
					(rootcategory.equals("NP")|| rc.endsWith("-NOM")
							||rootcategory.equals("SBAR") || rootcategory.equals("ADJP"))
					&& (root.getLeft()!= null && (root.getLeft().getCategory().equals("IN")
							||root.getLeft().getCategory().equals("TO")
							||root.getLeft().getCategory().matches("''|``")))){
				assigned = makeArgument(lexentry);return;
			}
//			SBAR phrases need an S argument
			if (parent.getCategory().equals("SBAR") && rootcategory.equals("S") 
					&& root.getLeft().getCategory().matches("(IN)|(WH.*)")){
				assigned = makeArgument(lexentry);return;
			}
//			 more than //TODO
			if (rootcategory.equals("IN") && root.getLeft()!= null 
					&& ( root.getLeft().getCategory().matches("RBR?")|| root.getLeft().getCategory().equals("JJ")) 
					&& parent.getCategory().equals("QP")){
				assigned = makeModifier(lexentry);return;
			}
			if (rootcategory.equals("")){
				assigned = makeArgument(lexentry);return;
			}
			if (rootcategory.equals(":")){
				if (((LeafNode)root).getLeaf().equals("0")|| ((LeafNode)root).getLeaf().startsWith("*")){
					assigned = makeArgument(lexentry);
				}
				else 
					assigned = makeModifier(lexentry);
				return;
			}
			else if (rootcategory.equals(".")
					||rootcategory.equals("?")){
				assigned = makeModifier(lexentry);
				return;
			}
//	 handle some of the interpunctuation as auxiliary trees?
			if (rootcategory.equals("''")||rootcategory.equals("``")){
				//assigned = makeModifier(lexentry);
				assigned = makeArgument(lexentry);
				return;
			}
			
			if (parent.getCategory().equals("VP") && rootcategory.equals("TO") && root.getRight().getCategory().equals("VP") ){
					assigned = makeModifier(lexentry);
					return;
			}
			// she sais X (annotation error???)
			if (parent.getCategory().equals("VP") && rootcategory.equals("SBAR") ){
				assigned = makeArgument(lexentry);
				return;
			}	
			if (parent.getFullCategory().startsWith("ADVP") && parent.getHeadChild().getCategory().equals("IN")){
				if(rootcategory.equals("NP")||rc.matches("(JJ.*)|(NN.*)|(PRP.*)|(.*-NOM)")){
					assigned = makeArgument(lexentry);
					return;
				}
				if (rootcategory.equals("PP")|rootcategory.equals("SBAR")){
					assigned = makeModifier(lexentry);
					return;
				}
			}
			if (parent.getCategory().equals("ADJP") && parent.getHeadChild().getCategory().matches("(JJ)|(ADJP)")){
				if(rootcategory.equals("S")){
					assigned = makeArgument(lexentry);
					return;
				}
			}
			if (rootcategory.equals("S")&& !containsCC(parent.getChildlist())){
				assigned = makeArgument(lexentry);
				return;
			}
			if (parent.getCategory().equals("PP") && rootcategory.equals("ADVP")){
				assigned = makeModifier(lexentry);
				return;
			}
			if (parent.getCategory().equals("WHNP") && rootcategory.matches("N.*")){
				assigned = makeArgument(lexentry);
				return;
			}
			//apposition
			if (parent.getCategory().equals("NP") && rc.matches("(NP(-.+)?)|(.*-NOM)") 
					&& root.getRight() != null && root.getRight().getCategory().matches("[;,]") 
					&& root.getLeft()!= null && root.getLeft().getCategory().equals(",")
					&& !containsCC(parent.getChildlist())){
				assigned = makeModifier(lexentry);
				return;
				//makeArgument(root.getLeft().getLexEntry());
				//makeArgument(root.getRight().getLexEntry());
			}
//			decades later
			if (parent.getCategory().equals("ADVP") && parent.getHeadChild().getCategory().startsWith("JJ") 
					&& rootcategory.equals("NP")){
				assigned = makeModifier(lexentry);
				return;
			}
		}
//		 modifier categories are trivially modifiers
		if ((rootcategory.equals("RRC")|| rootcategory.equals("JJ")||rootcategory.equals("PRN")
				|| rc.matches("ADVP.*")||rc.matches("(AUX)|(MD)")||rc.equals("SBAR-TMP")
				|| rc.equals("RBR") || rc.startsWith("PP-")|| rc.equals("RB")) && 
				(root.getParent()==null || !root.getParent().getFullCategory().endsWith("-PRD"))){
			//TODO: check whether these are generally valid
			assigned = makeModifier(lexentry);
			return;
		}
		if (root.hasTrace()){
			Trace trace = root.getTrace();
			String traceargmod = "UNDEF";
			if (trace.getFiller() == root && trace.getHole()!= null){
				traceargmod = trace.getHole().getArgMod();
				return;
			}
			else if (trace.getHole() == root){
				traceargmod = trace.getFiller().getArgMod();
				return;
			}
			if (traceargmod.equals("ARG")){
				assigned = makeArgument(lexentry);
				return;
			}
			else if (traceargmod.equals("MOD")){
				assigned = makeModifier(lexentry);
				return;
			}
		}
		if(rootcategory.equals("-NONE-")){
			assigned = makeArgument(lexentry);
			return;
		}
		//sentence, (PP with X) as Modifier
		if (rootcategory.equals("PP")){
			assigned = makeModifier(lexentry);
			return;
		}

		// handle commas.
		if (rootcategory.equals(",") ){
			for (TagNode n : root.getParent().getChildlist()){
				if (n.getCategory().equals("CC")||n.getCategory().equals("CONJP")){
					if (root.getRight()==null || root.getLeft()==null){
						assigned = makeModifier(lexentry); return;
					} 
					else{
						assigned = makeArgument(lexentry); return;
					}
				}
			}
			//as modifier if left sibling is modifier ONLY IF SIBLING NOT COORDINATION!!! TODO otherwise need extra node.
			if (root.getLeft()!=null && !root.getLeft().isArgument() && (root.getRight()==null || root.getRight().isArgument())){
				assigned = makeModifier(lexentry);
				if (root.getRight()!=null && PennTree.numberOfCC(root.getLeft().getChildlist())==0) {//don't do anything if already under own node.
					root.getRight().setLeftsib(root.getLeft());
					root.getParent().removeChild(root);
					root.setParent(root.getLeft());
					TagNode lastChild = root.getLeft().getChildlist().get(root.getLeft().getChildlist().size()-1);
					root.getLeft().addChild(root);
					lastChild.setRightsib(root);
					root.setLeftsib(lastChild);
				}
			}
			//as modifier if right sibling modifier ONLY IF SIBLING NOT COORDINATION!!!TODO otherwise need extra node.
			else if (root.getRight()!=null && !root.getRight().isArgument()&& PennTree.numberOfCC(root.getRight().getChildlist())==0){
				assigned = makeModifier(lexentry);
				if (!root.getRight().getChildlist().isEmpty()) {//don't do anything if already under own node.
					if (root.getLeft()!=null){
						root.getLeft().setRightsib(root.getRight());
					}
					root.getRight().setLeftsib(root.getLeft());
					root.getParent().removeChild(root);
					root.setParent(root.getRight());
					
					TagNode firstChild = root.getRight().getChildlist().get(0);
					root.getRight().addChild(0,root);
					firstChild.setLeftsib(root);
					root.setRightsib(firstChild);
				}
			}
			//else argument
			else 
				assigned = makeArgument(lexentry);
		}
	}

	private boolean containsCC(List<TagNode> childlist) {
		for (TagNode node : childlist){
			if (node.getCategory().matches("C(C|ONJP)")){
				return true;
			}
		}
		return false;
	}

	private boolean makeModifier(LexEntry lexentry) {
		TagNode root = lexentry.getRoot();
		root.setModifier();
		lexentry.setAuxtree();
		return true;
	}

	private boolean makeArgument(LexEntry lexentry) {
		TagNode root = lexentry.getRoot();
		root.setArgument();
		lexentry.setSubsttree();
		return true;
	}
	
	public boolean hasHeuristic() {
		return assigned;
	}

}
