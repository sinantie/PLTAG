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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author konstas
 */
public class ConvertPTBTreesToLatex
{

    private final static StringBuilder INDEX_UP = new StringBuilder("^");
    private final static StringBuilder INDEX_UP_REPLACE = new StringBuilder("$^");
    private final static StringBuilder BRACKET_CLOSE = new StringBuilder(")");
    private final static StringBuilder BRACKET_CLOSE_REPLACE = new StringBuilder(" ]");
    private final static StringBuilder BRACKET_OPEN = new StringBuilder("( ");
    private final static StringBuilder BRACKET_OPEN_REPLACE = new StringBuilder("[.");
    private final static String LATEX_PATH = "lib/latex/";
    private final static String TEMPLATE_FILE = LATEX_PATH + "tree_template.tikz";
    private String inputFile, outputFile;
    private final int cols;

    public ConvertPTBTreesToLatex(String inputFile, String outputFile, int cols)
    {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.cols = cols;
    }

    public void execute()
    {
        String[] rawText = Utils.readLines(inputFile);
        String[] latexTrees = convertPTBTrees(extractPTBTrees(rawText));
        try
        {
            String template = Utils.readFileAsString(TEMPLATE_FILE);
            String out = template.replace("%REPLACE_HERE", tabulate(latexTrees, cols));
            Utils.write(outputFile, out);
            String cmd = String.format("%s/tikz2pdf -o %s ", LATEX_PATH, outputFile);
            String cmdArray[] ={"/bin/sh", "-c", cmd};
            System.out.println(Utils.executeCmd(cmdArray));
            if(new File("tikz2pdf_temp.pdf").exists())
            {
                cmdArray[2] = "mv tikz2pdf_temp.pdf " + outputFile + ".pdf";
                System.out.println(Utils.executeCmd(cmdArray));
            }
            
        } catch (IOException ioe)
        {
            System.err.println(ioe.toString());
        }
    }

    private String[] extractPTBTrees(String[] lines)
    {
        List<String> list = new ArrayList<String>();
        for (String line : lines)
        {
            int indexOfFirstOpenBracket = line.indexOf("(");
            if (indexOfFirstOpenBracket >= 0)
            {
                int indexOfLastClosingBracket = line.lastIndexOf(")");
                if (indexOfLastClosingBracket > 0) // found a possible tree
                {
                    list.add(line.substring(indexOfFirstOpenBracket, indexOfLastClosingBracket + 1));
                }
            }
        }
        return list.toArray(new String[0]);
    }

    private String[] convertPTBTrees(String[] trees)
    {
        List<String> list = new ArrayList<String>();
        for (String tree : trees)
        {
            list.add(convertPTBTree(tree));
        }
        return list.toArray(new String[0]);
    }

    private String convertPTBTree(String tree)
    {

        tree = tree.replace(BRACKET_OPEN, BRACKET_OPEN_REPLACE).replace(BRACKET_CLOSE, BRACKET_CLOSE_REPLACE); // fix opening and closing brackets
        tree = tree.replace(INDEX_UP, INDEX_UP_REPLACE).replaceAll("null", "-"); // fix opening indices and null index
        tree = tree.replaceAll("<>", ""); // remove anchor annotation
        StringBuilder str = new StringBuilder();
        for (String token : tree.split(" "))
        {
            if (token.contains("$^"))
            {
                str.append(token).append("$");
            }
            else
            {
                str.append(token);
            }
            str.append(" ");
        }
        return "\\node{\\Tree " + str.toString().trim() + "};";
    }

    private String tabulate(String[] trees, int cols)
    {        
        if (trees.length == 1)
        {
            return trees[0] + "\\\\";
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < trees.length; i++)
        {
            str.append(trees[i]).append(i % cols == 0 ? "\\&" : "\\\\").append("\n");
        }
        if (trees.length % cols != 0)
        {
            str.delete(str.length() - 2, str.length());
        }
        return str.deleteCharAt(str.length() - 1).toString();
    }

    public static void main(String[] args)
    {
        String inputFile = null; //= "data/pltag/Lexicon_wsj_test_withSemantics";
        String outputFile = null; //= "data/pltag/latexTrees/Lexicon_wsj_test_withSemantics.tex";        
        int cols = 2;
        if(args.length > 3 || args.length == 0)
        {
            System.err.println("USAGE: java -cp dist/PLTAG.jar pltag.utils.ConvertPTBTreesToLatex input_file [columns output_file]");
            System.exit(1);
        }
        switch(args.length)
        {            
            case 1 : inputFile = args[0]; outputFile = args[0] + ".tex"; break;
            case 2 : inputFile = args[0]; outputFile = args[1]; break; //cols = Integer.valueOf(args[1]); break;    
//            case 3 : inputFile = args[0]; cols = Integer.valueOf(args[1]); outputFile = args[2]; break;    
        }        
        ConvertPTBTreesToLatex converter = new ConvertPTBTreesToLatex(inputFile, outputFile, cols);
        converter.execute();
    }
}
