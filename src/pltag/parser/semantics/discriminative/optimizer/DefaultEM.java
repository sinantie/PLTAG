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
package pltag.parser.semantics.discriminative.optimizer;

/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400  $
*/

public abstract class DefaultEM {
	//==== stop criterion
	private  int maxNumIter = 100;//run at most 100 iterations (i.e., number of funcion and gradient evaluation) for this particular run
	private double relativeLikelihoodThreshold = 1e-5;//if the relative change of the function value is smaller than this value, then we terminate
	private int maxConvergeNum = 1000;//if the number of times that the likelihood does not change, then stop

	private double lastLikelihood;
	
	public abstract void runOneEMStep(int iterNum);
	public abstract boolean isEMConverged();
	
	public abstract double getLastLikelihood();
	
	public abstract void printStatistics(int iter_num);
	
	
	public DefaultEM(int maxNumIter_, double relativeLikelihoodThreshold_, int maxConvergeNum_){
		maxNumIter = maxNumIter_;
		relativeLikelihoodThreshold = relativeLikelihoodThreshold_;
		maxConvergeNum = maxConvergeNum_;
	}
	
	public void runEM(){
		System.out.println("================ beging to run EM =======================");
        int numCalls=0;
        lastLikelihood=Double.NEGATIVE_INFINITY;

        int checkConverge=0;
        while (numCalls==0 || ( isEMConverged() == false ) && ( numCalls <= maxNumIter ) ){
        	numCalls++;
        	System.out.println("================ run iteration " + numCalls + "=======================");
        	double tLikelihood = getLastLikelihood();
        	if(tLikelihood<lastLikelihood) {
            	System.out.println("EM returns a bad optimal value; best: " + tLikelihood + "; last: " + lastLikelihood);
            	System.exit(1);
            }
        	 
        	//=== another way to terminate the em
            if( numCalls!=0 && Math.abs(lastLikelihood-tLikelihood)/lastLikelihood<relativeLikelihoodThreshold){
            	checkConverge++;
            	if(checkConverge>=maxConvergeNum){//does not change for several consecutive times
            		System.out.println("EM early stops because the likelihood does not change for several iterationss; break at iter " + numCalls);
            		break;
            	}       	             	 
            }else{
            	checkConverge=0;
            }
            
            lastLikelihood = tLikelihood;
           
        	runOneEMStep(numCalls);
            
            //printStatistics(num_calls, last_function_val, gradient_vector, weights_vector);
        }
        printStatistics(numCalls);        	
    }
	
}
