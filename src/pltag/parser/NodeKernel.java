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

public final class NodeKernel implements Serializable
{

    static final long serialVersionUID = -1L;
    private final String cat;
    private final String lambda;
    private final short lambdaTimestamp;
    
    public NodeKernel(String category, String lambda, short lambdaId)
    {
        cat = category;
        this.lambda = lambda;
        this.lambdaTimestamp = lambdaId;
    }
       
    public NodeKernel(NodeKernel k)
    {
        this(k.cat, k.lambda, k.lambdaTimestamp);
    }
    
    public String getLambda()
    {
        return lambda;
    }

    public String getLambdaPos()
    {
        return lambda.contains("\t") || lambda.contains(" ") ? lambda.split("\\p{Space}")[0] : "";
    }
    public short getLambdaTimestamp()
    {
        return lambdaTimestamp;
    }

    public String getCategory()
    {
        return cat;
    }
}
