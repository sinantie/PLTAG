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

import java.io.Serializable;

public class Node implements Serializable
{

    static final long serialVersionUID = -1L;
    private NodeKernel kernel;
    private byte downIndex;
    private byte upIndex;
    private String downPosTag;
    private short timestamp;    
    private boolean onSpine = false;
    private short nodeId;
    private String origElemTree;
    private int posInTree;
    private String leftMostCover;
    private String clusterNumber;
   
    public Node(NodeKernel k, short nodeID)
    {
        kernel = k;
        if (kernel == null)
        {
            System.out.println("kernel not assigned correctly.");
        }
        nodeId = nodeID;
    }

    public Node(NodeKernel k, byte dI, byte uI, short timestamp, short nodeID, String origTree, int posInTree,
            String downPosTag, String leftMostCover)
    {
        kernel = k;
        if (kernel == null)
        {
            System.out.println("kernel not assigned correctly.");
        }
        nodeId = nodeID;
        downIndex = dI;
        upIndex = uI;
        if (ParserModel.useLeftMost)
        {
            this.leftMostCover = leftMostCover;
        }
        this.timestamp = timestamp;
        this.origElemTree = origTree;
        this.posInTree = posInTree;
        this.downPosTag = downPosTag;
        //if (upIndex > -1 ) {
        //	this.downPosTag = null;
        //for pre-combined prediction trees with one prediction tree adjoined into the other.
			/*if (upIndex > 0 && downIndex > 0 && upIndex !=downIndex && 
         origElemTree.contains(downIndex+"_"+upIndex)){// && origElemTree.contains("^null_"+upIndex)){
         //				System.out.println(upIndex +" "+downIndex+" "+origElemTree);
         int i = origElemTree.lastIndexOf(downIndex+"_"+downIndex)-1;
         if (i > 0){
         String newDownPosTag = origElemTree.substring(0, i);
         //this.downPosTag = newDownPosTag.substring(newDownPosTag.lastIndexOf(" ")+1);
         //if (this.downPosTag.contains("(")) this.downPosTag = newDownPosTag.substring(newDownPosTag.lastIndexOf("(")+1);
         }
         }*/
        //}
    }

    public Node copy()
    {
        return new Node(new NodeKernel(kernel), downIndex, upIndex, timestamp, nodeId, origElemTree, posInTree, downPosTag, leftMostCover);
//		n.setDownPosTag(downPosTag);
        //return n;
    }

    public void setOnSpine(boolean b)
    {
        onSpine = b;
    }

    public boolean isOnSpine()
    {
        return onSpine;
    }

    @Override
    public String toString()
    {
        return new StringBuilder().append(kernel.getCategory())//.append(this.nodeId)
                .append("^").append(upIndex)//(convertByteToIndex(upIndex))
                .append("_").append(downIndex)//convertByteToIndex(downIndex))
                .append("@t:").append(timestamp)
                .append("L:").append(this.leftMostCover)
                .toString();
    }

    public String toShortString()
    {
        return new StringBuilder().append(kernel.getCategory())//.append(this.nodeId)
                .append("^").append(upIndex)//(convertByteToIndex(upIndex))
                .append("_").append(downIndex)//convertByteToIndex(downIndex))
                .append("@t:").append(timestamp)
                .toString();
    }

    public String getCategory()
    {
        return kernel.getCategory();
    }

    public byte getDownIndex()
    {
        return downIndex;
    }

    public byte getUpIndex()
    {
        return upIndex;
    }

    public boolean isShadow()
    {
        if (downIndex > 0)
        {
            return true;
        }
        if (upIndex > 0)
        {
            return true;
        }
        return false;
    }

    public void verifiedDownIndex()
    {
        downIndex = 0;
    }

    public void verifiedUpIndex()
    {
        upIndex = 0;
    }

    public boolean identical(Node on)
    {
        return kernel.getCategory().equals(on.kernel.getCategory())
                && downIndex == on.downIndex && upIndex == on.upIndex
                && timestamp == on.timestamp;
        //return toString().equals(on.toString());
    }

    /*
     public boolean identicalOld(Node on) {
     if (cat.equals(on.cat) && sameIndex(downIndex,on.downIndex) && sameIndex(upIndex,on.upIndex)){
     return true;
     }
     return false;
     }*/
    private boolean sameIndex(String index1, String index2)
    {
        if (index1 == null && index2 == null)
        {
            return true;
        }
        if (index1 == null && index2 != null)
        {
            return false;
        }
        if (index1 != null && index2 == null)
        {
            return false;
        }
        if (index1.equals(index2))
        {
            return true;
        }
        return false;
    }

    public void replaceIndeces(byte localShadowIndex, byte newIndex)
    {
        if (downIndex == localShadowIndex)
        {
            downIndex = newIndex;
        }
        if (upIndex == localShadowIndex)
        {
            upIndex = newIndex;
        }
    }

    public void setDownIndex(byte downIndex)
    {
        this.downIndex = downIndex;
    }

    public void setUpIndex(byte upIndex)
    {
        this.upIndex = upIndex;
    }

    public String getLambda()
    {
        return kernel.getLambda();
    }

    public Short getLambdaTimestamp()
    {
        return kernel.getLambdaTimestamp();
    }

    public String getLambdaPos()
    {
        return kernel.getLambdaPos();
    }
    
    public int getTimeStamp()
    {
        return this.timestamp;
    }

    public void setTimeStamp(short t)
    {
        //if (t> timestamp){
        this.timestamp = t;
        //}
    }

    public NodeKernel getKernel()
    {
        return kernel;
    }

    public void setKernel(NodeKernel kernel)
    {
        this.kernel = kernel;
    }

    public void setKernelAndTreeIfEmpty(NodeKernel kernel, String tree, boolean isAuxTree)
    {
        // update kernel information from incoming tree if the current node lambda is empty *OR*
        // the incoming tree is not a modifier, hence may become the head of the subtree
        if(kernelIsEmpty() || !isAuxTree)
        {
            this.kernel = kernel;
            origElemTree = tree;
        }
    }

    public boolean kernelIsEmpty()
    {
        return kernel.getLambdaTimestamp() == -99;
    }
    
    public short getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(short id)
    {
        nodeId = id;
    }

    public int getPosInTree()
    {
        return this.posInTree;
    }

    //only used for copy of adjnode to get correct value into ipi node.
    public void setPosInTree(int z)
    {
        this.posInTree = z;
    }

    public String getOrigTree()
    {
        return this.origElemTree;
    }

    public String getDownPosTag()
    {
        if (this.downPosTag == null)
        {
            return "-";
        }
        return this.downPosTag;
    }

    public void setDownPosTag(String pt)
    {

        this.downPosTag = pt;//TreeState.getPOStag(pt);
    }

    public String getLeftMostCover()
    {
        if (ParserModel.useLeftMost)
        {
            return this.leftMostCover;
        }
        else
        {
            return null;
        }
    }

    public void setLeftMostCover(String lmC)
    {
        this.leftMostCover = lmC;
    }

    public void setClusterNumber(String cn)
    {
        this.clusterNumber = cn;
    }

    public String getClusterNumber()
    {
        if (ParserModel.useClusterCode)
        {
            return this.clusterNumber;
        }
        else
        {
            return "ND";
        }
    }
    
    public void offsetNodeId(short offset)
    {
        nodeId += offset;
    }     
}
