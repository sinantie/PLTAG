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

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import fig.basic.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author sinantie
 */
public class PosTagger
{   
    
    private final String path;
    private static StanfordCoreNLP pipeline;
    
    public PosTagger(String path)
    {
        this.path = path;
        initPosTagger();
    }
    
    public void execute()
    {
        File f = new File(path);
        if(f.exists())
        {
            if(f.isDirectory())
            {
                for(File file : f.listFiles((File dir, String name) -> !name.startsWith(".")))
                {
                    posTagFile(file.getAbsolutePath());
                }
            }
            else
            {
                posTagFile(path);
            }
        }
    }
    
    private void posTagFile(String file)
    {
        StringBuilder str = new StringBuilder();
        String[] lines = Utils.readLines(file);
        for(String line : lines)
        {
            str.append(posTagLine(line)).append("\n");
        }
        Utils.write(file + ".posTagged", str.substring(0, str.length() - 1).toString());
    }
    
    public static String posTagLine(String line)
    {
        Annotation document = new Annotation(line);
        pipeline.annotate(document);
        StringBuilder str = new StringBuilder();
        for(CoreMap sentence: document.get(SentencesAnnotation.class))
        {
            for(CoreLabel token : sentence.get(TokensAnnotation.class))
            {
                str.append(token.get(PartOfSpeechAnnotation.class)).append(" ").append(token.get(TextAnnotation.class)).append("\t");
            }
        }        
        return str.toString().trim();
    }
    
    /**
     * 
     * POS-tag sentence and return an array of Pairs that contain the POS-tag and word.
     * @param line
     * @return 
     */
    public static fig.basic.Pair<String, String>[] posTagLineToArray(String line)
    {
        Annotation document = new Annotation(line);
        pipeline.annotate(document);    
        List<fig.basic.Pair<String, String>> out = new ArrayList<>();
        for(CoreMap sentence: document.get(SentencesAnnotation.class)) 
        {
            List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
            for(CoreLabel token : tokens)
            {
                out.add(new fig.basic.Pair(token.get(PartOfSpeechAnnotation.class), token.get(TextAnnotation.class))); 
            }
        }        
        return out.toArray(new fig.basic.Pair[0]);
    }
    
    public static String tokensToLinePosTagged(Pair<String, String>[] posWords)
    {
        StringBuilder str = new StringBuilder();
        for(Pair<String, String> p : posWords)
        {
            str.append(p.getFirst()).append(" ").append(p.getSecond()).append("\t");
        }
        return str.toString().trim();
    }
    
    /**
     * 
     * Detect whether input sentence comes from the dundee corpus, i.e., starts with annotation of Relative Clauses, and also contain
     * word ids. For example:<br />
     * SU|And 1_106 then 1_107 there 1_108 were 1_109 ...
     * @param line
     * @return true if the sentence 
     */
    private boolean isDundeeWithRCs(String line)
    {
        int indexOfRCDelimiter = line.indexOf("|");
        if(indexOfRCDelimiter > 0)
        {
            return true;
        }
        return false;
    }
    public static void initPosTagger()
    {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos");
//        props.setProperty("tokenizerOptions", "ptb3Escaping=false");
        pipeline = new StanfordCoreNLP(props);
    }
    
    public static void main(String[] args)
    {
        String path = args.length == 0 ? "data/pado_stimuli/Items/NP-S-simple/" : args[0];
        PosTagger ptf = new PosTagger(path);
        ptf.execute();
    }
    
}
