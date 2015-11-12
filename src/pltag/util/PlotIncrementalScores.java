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
package pltag.util;

import fig.basic.Fmt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author konstas
 */
public class PlotIncrementalScores
{
    private enum Metric {evalb, semantics};
    private final String[] COLORS = {"red","green","blue","cyan","magenta","yellow","black","gray","white","darkgray","lightgray","brown","lime","olive","orange","pink","purple","teal","violet"};
    private final static String LATEX_PATH = "lib/latex/";
    private final static String TEMPLATE_FILE = LATEX_PATH + "plot_template.tikz";
    private final String outputFile;
    private final String[] inputFolders;    
    private final int scoreColNo;
    private final Metric metricType;
    private final String[] modelNames;
    private int maxTimeStamp;
    
    public PlotIncrementalScores(String[] inputFolders, String outputFile, int scoreColNo, Metric metricType)
    {
        this.inputFolders = inputFolders;
        this.outputFile = outputFile;
        this.scoreColNo = scoreColNo;
        this.metricType = metricType;
        modelNames = new String[inputFolders.length];
    }

    public void execute()
    {      
        if(metricType == Metric.semantics)
        {
            saveIncrScores("results.ups", "Unlabeled Prediction Score (UPS) F$_1$");
            saveIncrScores("results.ciss", "Combined Incomplete SRL Score (CISS) F$_1$");        
        }        
        else if(metricType == Metric.evalb)
            saveIncrScores("results.evalb", "Incremental evalb F$_1$");        
        else
            System.err.println("Unsupported metric type! Supported types are: evalb|semantics");
    }

    public void saveIncrScores(String typeFilename, String title)
    {
        String scores = parseIncrScoreFiles(inputFolders, typeFilename, scoreColNo);
        Utils.write(typeFilename, scores);
        try
        {
            String template = Utils.readFileAsString(TEMPLATE_FILE);
            // set title
            String out = template.replace("%titleStr", title);
            // set max x
            out = out.replace("%xmaxStr", String.valueOf(maxTimeStamp + 1));
            // set legends and add plots
            StringBuilder legendStr = new StringBuilder();
            StringBuilder addPlots = new StringBuilder();
            int i = 0;
            for(String modelName : modelNames)
            {
                legendStr.append(modelName.replaceAll("_", "\\\\_")).append(",");
                addPlots.append(String.format("\\addplot[smooth,%s,line width=1pt] table[x=t,y=%s] {%s};\n", COLORS[i++], modelName, typeFilename));
            }
            out = out.replace("%legend_entries", legendStr.deleteCharAt(legendStr.length() - 1).toString());
            out = out.replace("%add_plots", addPlots);
            // create plot and copy .tikz, .pdf and results file
            String tikzFilename = outputFile + ".tikz";
            Utils.write(tikzFilename, out);
            String cmd = String.format("%s/tikz2pdf -o %s ", LATEX_PATH, tikzFilename);
            String cmdArray[] ={"/bin/sh", "-c", cmd};
            System.out.println(Utils.executeCmd(cmdArray));            
            if(new File("tikz2pdf_temp.pdf").exists())
            {
                cmdArray[2] = "mv tikz2pdf_temp.pdf " + outputFile + "." + typeFilename + "-out.pdf";
                System.out.println(Utils.executeCmd(cmdArray));
                cmdArray[2] = "mv " + typeFilename + " " + outputFile + "." + typeFilename;
                System.out.println(Utils.executeCmd(cmdArray));
                
            }
            
        } catch (IOException ioe)
        {
            System.err.println(ioe.toString());
        }
    }
    
    private String parseIncrScoreFiles(String[] inputFolders, String filename, int scoreColNo)
    {
        StringBuilder str = new StringBuilder();
        int numOfModels = inputFolders.length;
        int modelNo = 0;
        // create header
        str.append("t"); // first column is the timestamp
        for(String inputFolder : inputFolders)
        {
            String modelName = pathToModelName(inputFolder);
            modelNames[modelNo++] = modelName;
            str.append("\t").append(modelName); // use filename as header
        }
        str.append("\n");
        List[] scores = new ArrayList[numOfModels];        
        // read individual model scores for each timestamp
        for(modelNo = 0; modelNo < numOfModels; modelNo++)
        {            
            scores[modelNo] = new ArrayList<String>();
            String[] lines = Utils.readLines(inputFolders[modelNo] + filename);
            for(String line : lines)
            {
                if(!line.isEmpty())
                {
                    String[] tokens = line.split("\t");
                    if(tokens[0].matches("\\p{Digit}+") && tokens.length > scoreColNo)
                    {
                        scores[modelNo].add(normalisePercent(tokens[scoreColNo])); // pick the correct metric
                    } // if
                } // if
            } // for            
        } // for
        // create final String
        int size = scores[0].size();
        maxTimeStamp = size;
        for(int i = 0; i < size; i++)
        {
            str.append(i+1).append("\t"); // append timestamp            
            for(modelNo = 0; modelNo < numOfModels; modelNo++)
            {
                str.append(scores[modelNo].get(i)).append("\t");
            }
            str.append("\n");
        }
        return str.toString();
    }

    private String normalisePercent(String in)
    {
        double val = Double.valueOf(in);
        if (val > 1 || val < - 1)
        {
            val /= 100.0d;
            return Fmt.D(val);
        }
        return in;
    }
    
    String pathToModelName(String input)
    {
        String[] tokens = input.split("/");
        return tokens[tokens.length - 1];
    }
    
    public static void main(String[] args)
    {
        String[] inputFolder = {
                                  "results/output/pltag/test/SRL/23_all_majority_joelIdentification_moreScores_incrEval20words-SRL/",
                                  "results/output/pltag/test/SRL/23_all_oraclAllRoles_moreScores_incrEval20words-SRL/"
                              };
        String outputFile = "data/pltag/incrementalPlots/test";
        int colNo = 3;
        Metric metricType = Metric.semantics;
        if(args.length < 3)
        {
            System.err.println("USAGE: java -cp dist/PLTAG.jar pltag.util.PlotIncrementalScores output_file metric_column_no input_file(s)");
            System.exit(1);
        }
        else
        {
            outputFile = args[0];
            colNo = Integer.valueOf(args[1]) - 1;
            metricType = Enum.valueOf(Metric.class, args[2]);
            inputFolder = Arrays.copyOfRange(args, 3, args.length);
        }
        
        PlotIncrementalScores plotter = new PlotIncrementalScores(inputFolder, outputFile, colNo, metricType);
        plotter.execute();
    }   
}
