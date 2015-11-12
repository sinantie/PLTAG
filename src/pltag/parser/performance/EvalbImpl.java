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
package pltag.parser.performance;

import edu.stanford.nlp.parser.metrics.Evalb;
import edu.stanford.nlp.trees.Tree;
import fig.basic.LogInfo;
import java.io.PrintWriter;
import java.util.Set;

/**
 *
 * @author konstas
 */
public class EvalbImpl extends Evalb
{

    private double precision = 0.0;
    private double recall = 0.0;
    private double f1 = 0.0;
    private double exact = 0.0;
    private double precision2 = 0.0;
    private double recall2 = 0.0;
    private double pnum2 = 0.0;
    private double rnum2 = 0.0;

    public EvalbImpl(String str, boolean runningAverages)
    {
        super(str, runningAverages);
    }

    public boolean  evaluate(Tree guess, Tree gold, String exampleName)
    {
        if (gold == null || guess == null)
        {
//            LogInfo.error(exampleName + ": Cannot compare against a null gold or guess tree!\n");
            return false;

        }
        else if (guess.yield().size() != gold.yield().size())
        {
//            LogInfo.error(String.format("%s: Warning: yield differs", exampleName));
//            LogInfo.error(String.format("%s: Warning: yield differs:\nGuess: %s\nGold: %s", exampleName, guess.yield(), gold.yield()));
        }

        evaluate(guess, gold, null, 1.0);
        return true;
    }

    @Override
    public void evaluate(Tree guess, Tree gold, PrintWriter pw, double weight)
    {
        Set<?> dep1 = makeObjects(guess);
        Set<?> dep2 = makeObjects(gold);
        final double curPrecision = precision(dep1, dep2);
        final double curRecall = precision(dep2, dep1);
        curF1 = (curPrecision > 0.0 && curRecall > 0.0 ? 2.0 / (1.0 / curPrecision + 1.0 / curRecall) : 0.0);
        precision += curPrecision * weight;
        recall += curRecall * weight;
        f1 += curF1 * weight;
        num += weight;

        precision2 += dep1.size() * curPrecision * weight;
        pnum2 += dep1.size() * weight;

        recall2 += dep2.size() * curRecall * weight;
        rnum2 += dep2.size() * weight;

        if (curF1 > 0.9999)
        {
            exact += 1.0;
        }
        if (pw != null)
        {
            pw.print(" P: " + ((int) (curPrecision * 10000)) / 100.0);
            if (runningAverages)
            {
                pw.println(" (sent ave " + ((int) (precision * 10000 / num)) / 100.0 + ") (evalb " + ((int) (precision2 * 10000 / pnum2)) / 100.0 + ")");
            }
            pw.print(" R: " + ((int) (curRecall * 10000)) / 100.0);
            if (runningAverages)
            {
                pw.print(" (sent ave " + ((int) (recall * 10000 / num)) / 100.0 + ") (evalb " + ((int) (recall2 * 10000 / rnum2)) / 100.0 + ")");
            }
            pw.println();
            double cF1 = 2.0 / (rnum2 / recall2 + pnum2 / precision2);
            pw.print(str + " F1: " + ((int) (curF1 * 10000)) / 100.0);
            if (runningAverages)
            {
                pw.print(" (sent ave " + ((int) (10000 * f1 / num)) / 100.0 + ", evalb " + ((int) (10000 * cF1)) / 100.0 + ")   Exact: " + ((int) (10000 * exact / num)) / 100.0);
            }
            pw.println(" N: " + num);
        }
    }

    public double getEvalbF1()
    {
        return 2.0 / (rnum2 / recall2 + pnum2 / precision2);
    }

    public String summary()
    {
        StringBuilder out = new StringBuilder("\n\nevalb accuracy scores");
        out.append("\n---------------");
        out.append("\nPrecision (sent ave ").append(((int) (precision * 10000 / num)) / 100.0).append(", evalb ").append(((int) (precision2 * 10000 / pnum2)) / 100.0).append(")");
        out.append("\nRecall (sent ave ").append(((int) (recall * 10000 / num)) / 100.0).append(", evalb ").append(((int) (recall2 * 10000 / rnum2)) / 100.0).append(")");
        double cF1 = 2.0 / (rnum2 / recall2 + pnum2 / precision2);       
        out.append("\nF1 (sent ave ").append(((int) (10000 * f1 / num)) / 100.0).append(", evalb ").append(((int) (10000 * cF1)) / 100.0).append(")");
        out.append("\nExact: ").append(((int) (10000 * exact / num)) / 100.0);
        out.append("\nN: ").append((int) num);
        return out.toString();
    }
}
