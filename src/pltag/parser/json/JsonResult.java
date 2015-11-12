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
package pltag.parser.json;

import edu.stanford.nlp.trees.Tree;
import pltag.parser.ParsingTask.OutputType;

/**
 *
 * PLTAG incremental output wrapper object to JSON format. Keeps track of current
 * word timestamp, surprisal scores, prefix tree and iSRLs.
 * 
 * @author sinantie
 */
public class JsonResult implements Comparable<JsonResult>
{
    public static long COUNTER = 1;
    public static final String ERROR = "ERROR", 
                         SUCCESS = "SUCCESS",
                         FULL_TREE = "FULL_TREE",
                         FULL_SRL = "FULL_SRL",
                         PREFIX_TREE = "PREFIX_TREE",
                         ISRL = "ISRL",
                         DIFFICULTY = "DIFFICULTY";
    private long messageId;
    private int timestamp;     
    private String word, posTag, difficultyScores, prefixTree, srlTriples, messages, status;

    public JsonResult(int timestamp, String word, String posTag, String difficultyScores)
    {
        messageId = COUNTER++;
        this.timestamp = timestamp;
        this.word = word;
        this.posTag = posTag;
        this.difficultyScores = difficultyScores;
        this.status = DIFFICULTY;
    }
    
    public JsonResult(OutputType outputType, String out)
    {
        messageId = COUNTER++;
        switch(outputType)
        {
            case FULL_TREE : prefixTree = toPtbFormat(out); status = FULL_TREE; break;
            case FULL_SRL : srlTriples = out; status = FULL_SRL; break;
            case ERROR : messages = out; status = ERROR; break;
            case SUCCESS : messages = out; status = SUCCESS; break;
        }
        
    }
    
    public JsonResult(OutputType type, int timestamp, String out)
    {
        messageId = COUNTER++;
        this.timestamp = timestamp;
        switch(type)
        {
            case PREFIX_TREE : prefixTree = out; status = PREFIX_TREE; break;
            case ISRL : srlTriples = out; status = ISRL; break;
        }
    }

    private String toPtbFormat(String input)
    {
        Tree tree = Tree.valueOf(input);
        return tree.pennString();
    }
    public long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(long messageId)
    {
        this.messageId = messageId;
    }
        
    public int getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(int timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getWord()
    {
        return word;
    }

    public void setWord(String word)
    {
        this.word = word;
    }

    public String getPosTag()
    {
        return posTag;
    }

    public void setPosTag(String posTag)
    {
        this.posTag = posTag;
    }

    public String getDifficultyScores()
    {
        return difficultyScores;
    }

    public void setDifficultyScores(String difficultyScores)
    {
        this.difficultyScores = difficultyScores;
    }

    public String getPrefixTree()
    {
        return prefixTree;
    }

    public void setPrefixTree(String prefixTree)
    {
        this.prefixTree = prefixTree;
    }

    public String getSrlTriples()
    {
        return srlTriples;
    }

    public void setSrlTriples(String srlTriples)
    {
        this.srlTriples = srlTriples;
    }

    public String getMessages()
    {
        return messages;
    }

    public void setMessages(String messages)
    {
        this.messages = messages;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
        
    @Override
    public int compareTo(JsonResult o)
    {
        return this.timestamp - o.timestamp;
    }

//    @Override
//    public String toString()
//    {
//        return String.format("{\"status\":\"%s\", \"timestamp\":\"%s\", \"word\":\"%s\", "
//                + "\"posTag\":\"%s\", \"difficultyScores\":\"%s\", "
//                + "\"prefixTree\":\"%s\", \"srlTriples\":\"%s\"}", 
//                status, timestamp, word, posTag, difficultyScores, prefixTree, srlTriples);
//    }        
}
