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

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.WhitespaceTokenizer;
import edu.stanford.nlp.trees.Tree;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.exec.Execution;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import pltag.corpus.ElementaryStringTree;
import pltag.corpus.IdGenerator;
import pltag.corpus.PltagExample;
import pltag.corpus.StringTree;
import pltag.parser.Lexicon;
import pltag.parser.json.JsonInputWrapper;
import pltag.parser.json.JsonResult;
import pltag.parser.semantics.conll.ConllExample;

/**
 *
 * @author konstas
 */
public class Utils
{    
        
    public static enum Compare
    {

        VALUE, LABEL
    };

    public static enum Justify
    {

        LEFT, CENTRE, RIGHT
    };

    public static enum TypeAdd
    {

        RANDOM, NOISE
    };

    public static double[] add(double[] a, double b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] += b;
        }
        return a;
    }

    public static double[] add(double[] a, double scale, double[] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] += scale * b[i];
        }
        return a;
    }

    public static void beginTrack(String format, Object... args)
    {
        LogInfo.track(fmts(format, args));
    }

    public static double[] div(double[] a, double x)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] /= x;
        }
        return a;
    }

    public static boolean isEmpty(String s)
    {
        return s == null || s.length() == 0;
    }

    public static int[] fill(boolean ascending, int fromIndex, int length)
    {
        int[] out = new int[length];
        for (int i = 0; i < length; i++)
        {
            out[i] = fromIndex + i;
        }
        return out;
    }

    public static int[] fill(boolean ascending, int length)
    {
        return fill(ascending, 0, length);
    }

    public static int find(int a, int b, boolean[] ar)
    {
        int i = a;
        while (i < b && !ar[i - 1])
        {
            i += 1;
        }
        return i;
    }

    public static String fmt(int x)
    {
        return x + "";
    }

    public static String fmt(double x)
    {
        if (Math.abs(x - (int) x) < 1e-40) // An integer (probably)
        {
            return String.valueOf((int) x);
        } else if (Math.abs(x) < 1e-3) // Scientific notation (close to 0)
        {
            return String.format("%.2e", x);
        } else if (Math.abs(x) > 1e3)
        {
            return String.format("%.2e", x);
        } else
        {
            return String.format("%.4f", x);
        }
    }

    public static String fmt(Object x)
    {
        if (x instanceof Double)
        {
            return fmt(((Double) x).doubleValue());
        } else if (x instanceof Integer)
        {
            return fmt(((Integer) x).intValue());
        } else
        {
            return x.toString();
        }
    }

    public static String fmts(String format, Object... args)
    {
        Object[] formattedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++)
        {
            formattedArgs[i] = fmt(args[i]);
        }

        return String.format(format, formattedArgs);
    }

    public static RuntimeException impossible()
    {
        return new RuntimeException("Internal error: this shouldn't happen");
    }

    public static Integer[] int2Integer(int[] in)
    {
        Integer[] out = new Integer[in.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = in[i];
        }
        return out;
    }

    public static int[] integer2Int(Integer[] in)
    {
        int[] out = new int[in.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = in[i];
        }
        return out;
    }

    public static List<Integer> asList(int[] ar)
    {
        List<Integer> list = new ArrayList<Integer>(ar.length);
        for (int a : ar)
        {
            list.add(a);
        }
        return list;
    }

    public static void log(Object obj)
    {
        LogInfo.logs(obj);
    }

    public static void logs(String format, Object... args)
    {
        LogInfo.logs("%s", fmts(format, args));
    }

    public static void logss(String format, Object... args)
    {
        LogInfo.logss("%s", fmts(format, args));
    }

//    public static <A extends Object> A[] map(int n, A f)
//    {
//        final A[] result = (A[]) new Object[n];
//        for(int i = 0; i < n; i++)
//        {
//            result[i] =
//        }
//    }
    public static <A extends Object> String mkString(A[] array, String delimiter)
    {
        String out = "";
        for (Object el : array)
        {
            out += el.toString() + delimiter;
        }
        return out.length() > 0
                ? out.substring(0, out.length() - delimiter.length())
                : " ";
    }

    public static <A extends CallableWithLog, B>
            void parallelForeach(int numOfThreads, Collection<A> actions)
    {
        if (actions.size() == 1)
        {
            try
            {
                A action = actions.iterator().next();
                action.setLog(true);
                action.call();
            } catch (Exception ex)
            {
                LogInfo.error("Error: " + ex.getMessage());
//                ex.printStackTrace();
            }
        } else
        {
            ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
            Exception ex = null;
            Thread primaryThread = null;

            for (A action : actions)
            {
                if (!Execution.shouldBail())
                {
                    try
                    {
                        if (ex == null)
                        {
                            synchronized (executor)
                            {
                                if (primaryThread == null)
                                {
                                    primaryThread = Thread.currentThread();
                                }
                            }
                            action.setLog(primaryThread == Thread.currentThread());
                            executor.submit(action);                            
                        } // if - extends == null
                    } // try
                    catch (Exception e)
                    {
                        ex = e;
                        LogInfo.error(e.getMessage());
//                        e.printStackTrace();
                    }
                } // if !Execution.shouldBail()
            } // for              
            executor.shutdown();
            try
            {
                while (!executor.awaitTermination(1, TimeUnit.SECONDS))
                {
                }
            } catch (InterruptedException ie)
            {
                LogInfo.logs("Interrupted");
            }
        }
    }

    public static <A extends CallableWithLog, B>
            List<B> parallelForeachWithResults(int numOfThreads, Collection<A> actions)
    {
        List<B> results = Collections.synchronizedList(new ArrayList<B>());
        if (actions.size() == 1)
        {
            try
            {
                A action = actions.iterator().next();
                action.setLog(true);
                action.call();
            } catch (Exception ex)
            {
                LogInfo.error("Error: " + ex.getMessage());
//                ex.printStackTrace();
            }
        } else
        {
            ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
            Exception ex = null;
            Thread primaryThread = null;

            for (A action : actions)
            {
                if (!Execution.shouldBail())
                {
                    try
                    {
                        if (ex == null)
                        {
                            synchronized (executor)
                            {
                                if (primaryThread == null)
                                {
                                    primaryThread = Thread.currentThread();
                                }
                            }
                            action.setLog(primaryThread == Thread.currentThread());
                            results.add((B) executor.submit(action).get());
                        } // if - extends == null
                    } // try
                    catch (Exception e)
                    {
                        ex = e;
                        LogInfo.error(e);
//                        e.printStackTrace();
                    }
                } // if !Execution.shouldBail()
            } // for
            executor.shutdown();
            try
            {
                while (!executor.awaitTermination(1, TimeUnit.SECONDS))
                {
                }
            } catch (InterruptedException ie)
            {
                LogInfo.logs("Interrupted");
            }
        }
        return results;
    }

    public static String[] readLines(String path, int maxLines)
    {
        ArrayList<String> linesList = null;
        final BufferedReader in = IOUtils.openInEasy(path);
        if (in != null)
        {
            linesList = new ArrayList<String>();
            String line = "";
            int i = 0;
            try
            {
                while (((line = in.readLine()) != null) && i < maxLines)
                {
                    linesList.add(line.trim());
                    i++;
                }
                in.close();
            } catch (IOException ioe)
            {
                LogInfo.logs("Error reading file %s", path);
            }
        }
        String[] out = new String[linesList.size()];
        return linesList.toArray(out);
    }

    public static String[] readLines(String path)
    {
        return readLines(path, Integer.MAX_VALUE);
    }

    public static String[] readDocuments(String path)
    {
        ArrayList<String> docsList = null;
        final BufferedReader in = IOUtils.openInEasy(path);
        if (in != null)
        {
            docsList = new ArrayList<String>();
            String line = "";
            StringBuilder str = new StringBuilder();
            try
            {
                while ((line = in.readLine()) != null)
                {
                    if (line.equals("")) // documents are separated by an empty line
                    {
                        docsList.add(str.toString());
                        str = new StringBuilder();
                    }
                    str.append(line);
                }
                docsList.add(str.toString()); // add the last one
                in.close();
            } catch (IOException ioe)
            {
                LogInfo.logs("Error reading file %s", path);
            }
        }
        return docsList.toArray(new String[docsList.size()]);
    }

    public static String readFileAsString(String filePath) throws java.io.IOException
    {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try
        {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally
        {
            if (f != null)
            {
                try
                {
                    f.close();
                } catch (IOException ignored)
                {
                }
            }
        }
        return new String(buffer);
    }

    public static String stripExtension(String name)
    {
        int index = name.lastIndexOf(".");
        return index == -1 ? name : name.substring(0, index);
    }

    // Detect overflow, then set to cap (MAX_VALUE or MIN_VALUE)
    public static int safeAdd(int x, int y)
    {
        if (x > 0 && y > 0)
        {
            int z = x + y;
            if (z > 0)
            {
                return z;
            } else
            {
                return Integer.MAX_VALUE;
            }
        } else if (x < 0 && y < 0)
        {
            int z = x + y;
            if (z < 0)
            {
                return z;
            } else
            {
                return Integer.MIN_VALUE;
            }
        } else
        { // No chance of overflow if both have different sign
            return x + y;
        }
    }

    public static int same(int x, int y)
    {
        if (x != y)
        {
            throw new IllegalArgumentException(fmts("Different: %s %s", x, y));
        }
        return x;
    }

    public static <A> A same(A x, A y)
    {
        if (!x.equals(y))
        {
            throw new IllegalArgumentException(fmts("Different: %s %s",
                    x.toString(), y.toString()));
        }
        return x;
    }

    public static double[] set(double[] a, double b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] = b;
        }
        return a;
    }

    public static double[] set(double[] a, Random random, double noise, TypeAdd type)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] = (type == TypeAdd.RANDOM) ? Math.pow(1 + random.nextDouble(), noise)
                    : random.nextDouble() * noise;
        }
        return a;
    }
    
    public static double[] set(double[] a, Random random, double min, double max, double noise)
    {
        for (int i = 0; i < a.length; i++)
        {
//            a[i] = Math.pow(min + (random.nextDouble() * ((max - min) + 1)), noise);                   
            a[i] = Math.pow(random.nextDouble() * 2 - 1, noise);                   
        }
        return a;
    }
    
    public static Map<Integer, Double> set(Map<Integer, Double> a, Random random, double min, double max, double noise)
    {
        for(Integer key : a.keySet())
        {
            a.put(key, Math.pow(random.nextDouble() * 2 - 1, noise));
        }        
        return a;
    }

    public static int[] set(int[] a, int b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] = b;
        }
        return a;
    }

    public static String[] sortWithEmbeddedInt(String[] a)
    {
        StringWithEmbeddedInt[] swei = new StringWithEmbeddedInt[a.length];
        for (int i = 0; i < a.length; i++)
        {
            swei[i] = new StringWithEmbeddedInt(a[i]);
        }
        Arrays.sort(swei);
        String[] out = new String[swei.length];
        for (int i = 0; i < swei.length; i++)
        {
            out[i] = swei[i].getValue();
        }
        return out;
    }

    public static double sum(double[] a)
    {
        double result = 0.0;
        for (int i = 0; i < a.length; i++)
        {
            result += a[i];
        }
        return result;
    }
    
    public static double sum(Map<Integer, Double> a)
    {
        double result = 0.0;
        for(Double v : a.values())
            result += v;
        return result;
    }
    
    public static double sumSquared(double[] a)
    {
        double result = 0.0;
        for (int i = 0; i < a.length; i++)
        {
            result += a[i] * a[i];
        }
        return result;
    }

    public static double sumSquared(Map<Integer, Double> a)
    {
        double result = 0.0;
        for(Double v : a.values())
            result += v * v;
        return result;
    }
    
    // Assume the array is already sorted, just like the Unix command
    public static <A> A[] uniq(A[] a)
    {
        ArrayList<A> list = new ArrayList();
        for (int i = 0; i < a.length; i++)
        {
            if (i == 0 || !a[i].equals(a[i - 1]))
            {
                list.add(a[i]);
            }
        }
        return (A[]) list.toArray();
    }

    public static boolean writeLines(String path, String[] lines)
    {
        PrintWriter out = IOUtils.openOutEasy(path);
        if (out != null)
        {
            for (String line : lines)
            {
                out.println(line);
            }
            out.close();
            return true;
        }
        return false;
    }

    public static boolean writeMapAppend(String path, Map<?, Integer> map)
    {
        PrintWriter out = IOUtils.openOutAppendEasy(path);
        if (out != null)
        {
            for (Entry<?, Integer> e : map.entrySet())
            {
                String key = e.getKey().toString().trim();
                Integer value = e.getValue();
                out.append(String.format("%s\t=%s\n", key, value));
            }
            out.close();
            return true;
        }
        return false;
    }

    public static boolean writeMap(String path, Map<?, Integer> map)
    {
        PrintWriter out = IOUtils.openOutEasy(path);
        if (out != null)
        {
            for (Entry<?, Integer> e : map.entrySet())
            {
                String key = e.getKey().toString().trim();
                Integer value = e.getValue();
                out.append(String.format("%s\t=%s\n", key, value));
            }
            out.close();
            return true;
        }
        return false;
    }

    public static boolean write(String path, String text)
    {
        PrintWriter out = IOUtils.openOutEasy(path);
        if (out != null)
        {
            out.println(text);
            out.close();
            return true;
        }
        return false;
    }

    /**
     * computes word error rate of current hypothesis against transcription
     *
     * @param lineTrans the current hypothesis
     * @param trueTrans the transciption
     * @return word error rate for current hypothesis against transcription
     */
    public static float computeWER(String lineTrans, String trueTrans)
    {
        return computeWER(lineTrans.toUpperCase().split("\\p{Space}"), trueTrans.toUpperCase().split("\\p{Space}"));
    }

    public static float computeWER(Object[] transArray, Object[] trueArray)
    {
        int ld = 0;
        if (transArray.length == 0)
        {
            transArray = new Object[1];
        }
        // compute levenshtein distance
        ld = getLevenshteinDistance(trueArray, transArray);

        return (float) ld / (float) trueArray.length;
    }

    public static int getLevenshteinDistance(Object[] s, Object[] t)
    {
        if (s == null || t == null)
        {
            throw new IllegalArgumentException("Strings must not be null");
        }

        /*
         The difference between this impl. and the previous is that, rather
         than creating and retaining a matrix of size s.length()+1 by t.length()+1,
         we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
         is the 'current working' distance array that maintains the newest distance cost
         counts as we iterate through the characters of String s.  Each time we increment
         the index of String t we are comparing, d is copied to p, the second int[].  Doing so
         allows us to retain the previous cost counts as required by the algorithm (taking
         the minimum of the cost count to the left, up one, and diagonally up and to the left
         of the current cost count being calculated).  (Note that the arrays aren't really
         copied anymore, just switched...this is clearly much better than cloning an array
         or doing a System.arraycopy() each time  through the outer loop.)

         Effectively, the difference between the two implementations is this one does not
         cause an out of memory condition when calculating the LD over two very large strings.
         */
        int n = s.length; // length of s
        int m = t.length; // length of t

        if (n == 0)
        {
            return m;
        } else if (m == 0)
        {
            return n;
        }

        int p[] = new int[n + 1]; //'previous' cost array, horizontally
        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        Object t_j; // jth character of t

        int cost; // cost

        for (i = 0; i <= n; i++)
        {
            p[i] = i;
        }

        for (j = 1; j <= m; j++)
        {
            t_j = t[j - 1];
            d[0] = j;

            for (i = 1; i <= n; i++)
            {
                cost = s[i - 1].equals(t_j) ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    static int argmax(double[] weights)
    {
        if (weights == null)
        {
            return -1;
        }
        if (weights.length == 1)
        {
            return 0;
        }
        double max = weights[0];
        int maxI = 0;
        for (int i = 1; i < weights.length; i++)
        {
            if (weights[i] > max)
            {
                maxI = i;
            }
        }
        return maxI;
    }

    public static String deTokenize(String input)
    {
        boolean capitalise = false;
        StringBuilder str = new StringBuilder();
        String[] ar = input.split(" ");
        // capitalise first word
        str.append(capitalise(ar[0])).append(" ");
        for (int i = 1; i < ar.length; i++)
        {
            String current = ar[i];
            // remove space before delimeter
            if (current.equals(",") || current.equals("?"))
            {
                str.deleteCharAt(str.length() - 1).append(current);
//                str.append(current);
            } // capitalise next word and delete space before a fullstop
            else if (current.equals("."))
            {
                str.deleteCharAt(str.length() - 1).append(current);
                capitalise = true;
            } else if (capitalise)
            {
                str.append(capitalise(current));
                capitalise = false;
            } else
            {
                str.append(current);
            }
            str.append(" ");
        }
        return str.toString().trim();
    }

    public static String[] tokenize(String input)
    {
        return input.toLowerCase().split("\\s");
    }

    public static String[] removeStopWords(String[] tokens, Set<String> stopWords)
    {
        List<String> res = new ArrayList<String>();
        for (String token : tokens)
        {
            if (!stopWords.contains(token))
            {
                res.add(token);
            }
        }
        return res.toArray(new String[0]);
    }

    public static String tokensToString(String[] tokens)
    {
        if (tokens.length == 0)
        {
            return "";
        }
        StringBuilder str = new StringBuilder(tokens[0]);
        for (int i = 1; i < tokens.length; i++)
        {
            str.append(" ").append(tokens[i]);
        }
        return str.toString();
    }

    public static String tokenizeStanford(String line)
    {
        StringBuilder str = new StringBuilder();
        Tokenizer<CoreLabel> tokenizer = new PTBTokenizer(new StringReader(line), new CoreLabelTokenFactory(), "asciiQuotes=true untokenizable=allDelete");
        while(tokenizer.hasNext())
        {
            CoreLabel label = tokenizer.next();
            if(!label.toString().matches("``|\'\'|\"|-[LR][RCR]B-"))
                str.append(label).append(" ");
        }
        return str.toString().trim();
    }
    
    public static String applyDictionary(String input, Properties dictionary)
    {
        if (dictionary.isEmpty())
        {
            return input;
        }
        StringBuilder str = new StringBuilder();
        for (String token : input.split(" "))
        {
            str.append(dictionary.containsKey(token) ? dictionary.getProperty(token) : token).append(" ");

        }
        return str.toString().trim();
    }

    public static String capitalise(String input)
    {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static void writePaths(String filename, List<List> lists)
    {
        try
        {
            PrintWriter out = IOUtils.openOut(filename);
            for (List list : lists)
            {
                IOUtils.printLinesNoNewLine(out, list);
            }
            out.close();
        } catch (IOException ioe)
        {
            System.out.println(ioe.getMessage());
        }
    }

    public static boolean isSentencePunctuation(String s)
    {
        return s.equals("./.") || s.equals("--/:") || s.equals(".") || s.equals("--");
    }

    public static boolean isPunctuation(String s)
    {
        return s.equals("./.") || s.equals(",/,") || s.equals("--/:")
                || s.equals("-LRB-/-LRB-") || s.equals("-RRB-/-RRB-")
                || s.equals(".") || s.equals(",") || s.equals("--")
                || s.equals("(") || s.equals(")");
    }

    public static String arrayToString(String[] array)
    {
//        if(array.length == 0)
//            return "empty";
        StringBuilder str = new StringBuilder();
        for (String ar : array)
        {
            str.append(ar).append(" ");
        }
        return str.toString().trim();
    }

    public static String[] getWordPos(String posWord, boolean posOnly, boolean goldPosTags)
    {
        int index = posWord.indexOf(" ");
        String pos = posWord.substring(0, index);
        String word;
        if (posOnly)
        {
            word = pos;
        } else
        {
            word = goldPosTags ? posWord : posWord.substring(index + 1);
        }
        return new String[]
        {
            word, pos
        };
    }

    public static String getCatInventory(String key, boolean combineNNVBCats)
    {
        return combineNNVBCats ? key.replaceAll("NNP", "NN").replaceAll("NNS", "NN").replaceAll("VB(N|D|Z|G|P)", "VB") : key;
//        if (opts.combineNNVBcats)
//        {
//            key = key.replaceAll("NNP", "NN");
//            key = key.replaceAll("NNS", "NN");
//            //key = key.replaceAll("VB(N|Z|D|P)", "VB");
//            key = key.replaceAll("VB(N|D|Z|G|P)", "VB");
//            //key = key.replaceAll("VB(P|Z)", "VB");
//        }
//        return key;
    }

    public static String getCutOffCorrectedMainLex(String string, Set<String> listOfFreqWords, boolean train, boolean fullLex)
    {
        return train || !fullLex ||
            (listOfFreqWords == null || listOfFreqWords.contains(string.toLowerCase())) || 
            string.contains("*") || string.equals("0") || 
            string.equals("WORD") || string.equals("UNKNOWN") ? string : "UNK";        
    }  
    
    public static String[] executeCmdToArray(String[] cmd)
    {
        List<String> out = new ArrayList<String>();
        try
        {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null)
            {
                out.add(line);
            }
            p.waitFor();
        } catch (Exception e)
        {
            LogInfo.error(e);
        }
        return out.toArray(new String[0]);
    }

    public static String executeCmd(String[] cmd)
    {
        StringBuilder str = new StringBuilder();
        for (String line : executeCmdToArray(cmd))
        {
            if (!line.equals(""))
            {
                str.append(line).append("\n");
            }
        }
        if (str.length() > 0)
        {
            str.deleteCharAt(str.length() - 1);
        }
        return str.toString();
    }

    /**
     * Input String has the following format: Example_xxx (name) \n text
     * (optional) \n events \n record_tree (optional) \n align (optional)
     *
     * @param input
     * @return an array of Strings with name, text, events and align data in
     * each position
     */
    public static String[] extractExampleFromString(String input)
    {
        String[] res = new String[4];
        String ar[] = input.split("\n");
        StringBuilder str = new StringBuilder();
        if (ar[0].equals("$NAME")) // event3 v.2 format
        {
            res[0] = ar[1]; // name
            // parse text
            int i = 3; // 2nd line is the $TEXT tag
            while (i < ar.length && !ar[i].equals("$PRED_LEXICON"))
            {
                str.append(ar[i++]).append("\n");
            }
            res[1] = str.deleteCharAt(str.length() - 1).toString(); // delete last \n
            if (i < ar.length) // didn't reach the end of input, so there is predicted lexicon and lexicon data
            {
                str = new StringBuilder();
                i++; // move past $PRED_LEXICON tag
                while (i < ar.length && !(ar[i].equals("$LEXICON")))
                {
                    str.append(ar[i++]).append("\n");
                }
                res[2] = str.deleteCharAt(str.length() - 1).toString(); // delete last \n
                i++; // move past $ALIGN tag
                str = new StringBuilder();
                while (i < ar.length)
                {
                    str.append(ar[i++]).append("\n");
                }
                res[3] = str.deleteCharAt(str.length() - 1).toString(); // delete last \n
            }
        } else // gold standard data only
        {
            res[0] = ar[0]; // name
            str = new StringBuilder();
            int i = 1; // move past name
            while (i < ar.length) // append gs data
            {
                str.append(ar[i++]).append("\n");
            }
            res[1] = str.deleteCharAt(str.length() - 1).toString(); // delete last \n
        }
        return res;
    }    
    public static List<PltagExample> readLinesExamples(String inputPath)
    {
        return readLinesExamples(Utils.readLines(inputPath));        
    }
    
    /**
     * Simple example reader. We assume that each example is in a single
     * separate line. We also support examples that have an id in the line above:
     * Example_1
     * line1....
     * Example_2
     * line2....
     *
     * @param lines
     * @return
     */
    public static List<PltagExample> readLinesExamples(String[] lines)
    {
        List<PltagExample> examples = new ArrayList<PltagExample>();
//        int i = 1;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            if(line.startsWith("Example_"))
                examples.add(readLineExample(lines[++i], line));
            else
                examples.add(readLineExample(line, i));
        }
        return examples;
    }

    public static PltagExample readLineExample(String line, int lineNo)
    {
        int indexOfComment = line.indexOf("#");
        if (indexOfComment > -1)
        {
            line = line.substring(0, indexOfComment).trim();
        }
        return new PltagExample("Example_" + lineNo, line);
    }
    
    public static PltagExample readLineExample(String line, String id)
    {
        int indexOfComment = line.indexOf("#");
        if (indexOfComment > -1)
        {
            line = line.substring(0, indexOfComment).trim();
        }
        return new PltagExample(id, line);
    }

    public static List<PltagExample> readPltagExamples(String inputPath, boolean examplesInSingleFile)
    {
        List<PltagExample> examples = new ArrayList<PltagExample>();
        try
        {
            PltagExample example;
            if (examplesInSingleFile)
            {
                String key = null;
                StringBuilder str = new StringBuilder();
                for (String line : Utils.readLines(inputPath))
                {
                    if (line.startsWith("Example_") || line.equals("$NAME"))
                    {
                        if (key != null) // only for the first example
                        {
                            example = new PltagExample(extractExampleFromString(str.toString()));
                            examples.add(example);
                            str = new StringBuilder();
                        }
                        key = line;
                    } // if
                    str.append(line).append("\n");
                }  // for
                // don't forget last example
                example = new PltagExample(extractExampleFromString(str.toString()));
                examples.add(example);
            } // if
            else
            {
                throw new UnsupportedOperationException("Currently we support reading examples in single file only");
            }
        } catch (Exception ioe)
        {
            LogInfo.error(ioe);
        }
        return examples;
    }

    public static List<PltagExample> readConllExamples(String inputPath)
    {
        List<PltagExample> examples = new ArrayList<PltagExample>();
        StringBuilder str = new StringBuilder();
        for (String line : Utils.readLines(inputPath))
        {
            try
            {
                if (line.equals(""))
                {

                    String sentence = normaliseConll(str.toString());
                    examples.add(new PltagExample(getConllGoldSentence(sentence), sentence, true));
                    str = new StringBuilder();
                } else
                {
                    str.append(line).append("\n");
                }
            } catch (Exception ioe)
            {
                LogInfo.error(ioe);
            }
        }  // for                      
        return examples;
    }

    public static String getConllGoldSentence(String input)
    {
        StringBuilder str = new StringBuilder();
        for (String line : unpackConllSentence(input))
        {
            str.append(line.split("\t")[1]).append(" ");
        }
        return str.toString().trim();
    }

    /**
     * Remove punctuation, quotation marks, and brackets, from CoNLL input, as
     * they are discarded from the PLTAG parser
     *
     * @param input
     * @return
     */
    public static String normaliseConll(String input)
    {
        List<String> tokens = unpackConllSentence(input);
        for (Iterator<String> iter = tokens.iterator(); iter.hasNext();)
        {
            String token = iter.next();
            String word = token.split("\t")[1];
            if (word.equals("``") || word.equals("`") || word.equals("''") || word.equals("{") || word.equals("}")
                    || word.equals("(") || word.equals(")"))
            {
                iter.remove();
            }
        }

        String finalToken = tokens.get(tokens.size() - 1);
        String finalWord = finalToken.split("\t")[1];
        while (finalWord.matches("\\p{Punct}") && !finalWord.equals("%") && !finalWord.equals(":") && !finalWord.equals(","))
        {
            tokens.remove(tokens.size() - 1);
            finalToken = tokens.get(tokens.size() - 1);
            finalWord = finalToken.split("\t")[1];
        }
        return repackConllSentence(tokens);
    }

    public static String removeTraces(String input)
    {
        StringBuilder str = new StringBuilder();
        for (String token : input.split(" "))
        {
            if (!(token.equals("0") || token.contains("*")))
            {
                str.append(token).append(" ");
            }
        }
        return str.toString().trim();
    }

    public static List<Integer> hyphenSlashInWordIds(String sentence)
    {
        List<Integer> ids = new ArrayList<Integer>();
        String[] ar = sentence.split(" ");
        for (int i = 0; i < ar.length; i++)
        {
            String word = ar[i];
            if ((word.contains("-") && !word.equals("-") && !word.equals("--"))
                    && !(word.startsWith("198") && word.endsWith("90")) && !word.equals("1-2-3")) // really ugly
            {
                for (int c = 0; c < numOfHyphenSlashInWord(word, "-"); c++)
                {
                    ids.add(i);
                }
            }
            if (word.contains("\\/") && !word.equals("/") && !word.matches("[0-9]+\\\\/[0-9]+"))
            {
                for (int c = 0; c < numOfHyphenSlashInWord(word, "\\/"); c++)
                {
                    ids.add(i);
                }
            }
        }
        return ids;
    }

    /**
     * returns the number of times the delimiter character occurs in the word.
     * For example for the word 'London-based' it will return 1, and for the
     * word million-square-foot, it will return 2
     *
     * @param word
     * @param delimiter
     * @param id
     * @return
     */
    public static int numOfHyphenSlashInWord(String word, String delimiter)
    {
        return word.split(delimiter).length - 1;
    }

    public static String rejoinHyphenSlashesConll(String refSentence, String conllSentence, List<Integer> hyphenSlashIds)
    {
        String refAr[] = refSentence.split(" ");
        int prevId = -1;
        for (int id : hyphenSlashIds)
        {
            List<String> unpack = unpackConllSentence(conllSentence);
            String wordInConll = getWordInConllSentence(unpack, id);
            if (wordInConll != null)
            {
                if (refAr[id].startsWith(wordInConll) && (!refAr[id].equals(wordInConll) || prevId == id))
                {
                    if (id + 2 < unpack.size())
                    {
                        setWordInConllSentence(unpack, refAr[id], id, unpack.get(id + 2)); // copy word with hyphen/slash from reference sentence to conll sentence
                        unpack.remove(id + 1);
                        unpack.remove(id + 1); // remove the hyphen/slash and the following conjunctive from the conll sentence
                        conllSentence = repackConllSentence(unpack);
                    }
                }
                prevId = id;
            }
        }
        return conllSentence;
    }

    public static String rejoinHyphenSlashesConll(PltagExample conll, List<Integer> hyphenSlashInWord, String pltagSentence)
    {
        String rejoinedConllSentence = Utils.rejoinHyphenSlashesConll(pltagSentence, conll.getGoldStandard(), hyphenSlashInWord);
        conll.setGoldStandard(rejoinedConllSentence);
        conll.setName(Utils.getConllGoldSentence(rejoinedConllSentence));
        return conll.getName();
    }

    public static List<String> unpackConllSentence(String input)
    {
        return unpack(input, "\n");
    }

    public static List<String> unpackConllWord(String input)
    {
        return unpack(input, "\t");
    }

    public static List<List<String>> unpackConllSentenceToTokens(String input)
    {
        List<List<String>> list = new ArrayList();
        for (String word : unpackConllSentence(input))
        {
            list.add(unpackConllWord(word));
        }
        return list;
    }

    public static String repackConllSentence(List<String> input)
    {
        return repack(input, "\n");
    }

    public static String repackConllToken(String[] token)
    {
        return repack(Arrays.asList(token), "\t");
    }

    public static String repackConllToken(List<String> token)
    {
        return repack(token, "\t");
    }

    public static String repackConllTokensToSentence(List<List<String>> input)
    {
        StringBuilder str = new StringBuilder();
        for (List<String> row : input)
        {
            for (String cell : row)
            {
                str.append(cell).append("\t");
            }
            str.replace(str.length() - 1, str.length(), "\n");
        }
        return str.toString();
    }

    public static String repackConllTokensToSentence(String[][] input)
    {
        StringBuilder str = new StringBuilder();
        for (String[] row : input)
        {
            for (String cell : row)
            {
                str.append(cell).append("\t");
            }
            str.replace(str.length() - 1, str.length(), "\n");
        }
        return str.toString();
    }

    public static List<List<String>> cutConllTokens(String input, int[] cols)
    {
        List<List<String>> sentences = unpackConllSentenceToTokens(input);
        List<List<String>> out = new ArrayList(sentences.size());
        for (List<String> sentence : sentences)
        {
            List<String> sentenceOut = new ArrayList<String>(cols.length);
            for (int col : cols)
            {
                sentenceOut.add(sentence.get(col));
            }
            out.add(sentenceOut);
        }
        return out;
    }

    public static List<String> unpack(String input, String delimiter)
    {
        List<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(input.split(delimiter)));
        return list;
    }

    public static String repack(List<String> input, String delimiter)
    {
        StringBuilder str = new StringBuilder(input.get(0));
        for (int i = 1; i < input.size(); i++)
        {
            str.append(delimiter).append(input.get(i));
        }
        return str.toString();
    }

    public static String getWordInConllSentence(List<String> sentence, int pos)
    {
        return pos < sentence.size() ? sentence.get(pos).split("\t")[1] : null;
    }

    public static void setWordInConllSentence(List<String> sentence, String newWord, int pos, String oldWordConll)
    {
        String[] token = sentence.get(pos).split("\t");
        token[ConllExample.FORM_COL] = newWord;
        List<String> oldWordTokens = unpackConllWord(oldWordConll);
        for (int i = ConllExample.FILLPRED_COL; i < oldWordTokens.size(); i++) // copy-paste any predicate/argument information attached to word after the hyphen
        {
            token[i] = oldWordTokens.get(i);
        }
        sentence.set(pos, repackConllToken(token));
    }

    public static Tree removeEmptyNodes(Tree tree)
    {
        Tree[] children = tree.children();
        for (int i = 0; i < children.length; i++)
        {
            Tree child = children[i];
            // heuristic: all leaf nodes have a special anchor symbol '<>' in the end.
            // If we don't find this, then we should remove the node.
            if (child.numChildren() == 0 && !child.label().value().contains("<>"))
            {
//                System.out.println("node " + child);                
                tree.removeChild(i);
            } else
            {
                removeEmptyNodes(child);
            }
        }
        return tree;
    }

    public static String normaliseTree(String tree)
    {
        if (tree.equals(""))
        {
            return tree;
        }
        IdGenerator idgen = new IdGenerator();
        StringTree stringTree = Lexicon.convertToTree(new ElementaryStringTree("1\t" + tree, false), idgen);
        return stringTree.printSimple(stringTree.getRoot(), false);
    }

    public static boolean isLowerCase(String str)
    {
        for (char c : str.toCharArray())
        {
            if (!Character.isLowerCase(c))
            {
                return false;
            }
        }
        return true;
    }

    public static String formatTable(String[][] table, Justify justify)
    {
        String out = "";
        final int numOfRows = table.length;
        final int numOfCols = table[0].length;

        // find widths of columns
        int[] widths = new int[numOfCols];
        for (int c = 0; c < numOfCols; c++)
        {
            for (int r = 0; r < numOfRows; r++)
            {
                widths[c] = Math.max(widths[c], table[r][c].length());
            }
        }
        int padding = 0;
        for (int r = 0; r < numOfRows; r++)
        {
            for (int c = 0; c < numOfCols; c++)
            {
                padding = widths[c] - table[r][c].length();
                switch (justify)
                {
                    case LEFT:
                        out += table[r][c] + spaces(padding);
                        break;
                    case CENTRE:
                        out += spaces(padding / 2) + table[r][c]
                                + spaces((padding + 1) / 2);
                        break;
                    case RIGHT:
                        out += spaces(padding) + table[r][c];
                }
                out += " ";
            }
            out += "\n";
        }
        return out;
    }

    private static String spaces(int n)
    {
        char[] out = new char[n];
        Arrays.fill(out, ' ');
        return String.valueOf(out);
    }

    public static String convertRoleFromPropbankToConll(String roleIn)
    {
        if (roleIn == null)
        {
            return null;
        }
        if (roleIn.matches("ARG[0-9]-[a-zA-Z]+"))
        {
            roleIn = roleIn.substring(0, roleIn.indexOf("-"));
        }
        return roleIn.replace("ARG", "A");
    }

    public static long asLong(int x, int y)
    {
//        return (((long) x) << 32) | y;
        return (((long) x) << 32) | (y & 0xffffffffL);
    }

    public static int getFirst(long container)
    {
        return (int) ((container >> 32) & 0xFF);
    }

    public static int getSecond(long container)
    {        
//        return (int) (container & 0xFF);
        return (int) container;
    }
        
    public static Map<Integer, String> computeIncrementalTrees(String treeStr)
    {
        Map<Integer, String> map = new TreeMap<Integer, String>();        
        Tree tree = Tree.valueOf(treeStr);
        List<Tree> leaves = tree.getLeaves();
        Tree firstLeaf = leaves.get(0);
        // first prefix tree by default is the sub-tree rooted on the preterminal (don't add, as we compute evalb for words>1)
//        map.put(0, firstLeaf.parent(tree).toString());
        for(int i = 1; i < leaves.size(); i++)
        {
            Tree lastLeaf = leaves.get(i);
            map.put(i, getMinimalConnectedStructure(tree, firstLeaf, lastLeaf, i).toString());
        }
        return map;
    }
    
    public static String computeIncrementalTree(String treeStr)
    {
        Tree tree = Tree.valueOf(treeStr);
        List<Tree> leaves = tree.getLeaves();
        leaves.get(leaves.size() - 1);
        return computeIncrementalTree(treeStr, leaves.size() - 1).toString();
    }
    
    public static String computeIncrementalTree(String treeStr, int leafNo)
    {
        Tree tree = Tree.valueOf(treeStr);
        List<Tree> leaves = tree.getLeaves();
        Tree firstLeaf = leaves.get(0);
        Tree lastLeaf = leaves.get(leafNo);
        return getMinimalConnectedStructure(tree, firstLeaf, lastLeaf, leafNo).toString();
    }
    
    public static Tree getMinimalConnectedStructure(Tree tree, Tree firstLeaf, Tree lastLeaf, int lastLeafIndex)
    {
        // find common ancestor node by traversing the tree bottom-up from the last leaf and up
        Tree commonAncestorNode = lastLeaf.parent(tree);
        while(!commonAncestorNode.getLeaves().get(0).equals(firstLeaf))
        {
            commonAncestorNode = commonAncestorNode.parent(tree);
        }
        // found the common ancestor, now we need to clone the tree and chop the children non-terminals the span of which is outwith the last leaf
        Tree result = commonAncestorNode.deepCopy();
        List<Tree> leaves = result.getLeaves();
        lastLeaf = leaves.get(lastLeafIndex);
        Tree p = lastLeaf.parent(result);
        Tree d = lastLeaf;
        while(p != null)
        {
            if(p.numChildren() > 1)
            {
                // remove siblings to the right of d
                int index = indexOfChild(p, d.nodeNumber(result), result);
                pruneChildrenAfter(p, index);
            }
            d = p;
            p = p.parent(result);
        }
        return result;
    }
    
    public static int indexOfChild(Tree parent, int childNodeNumber, Tree tree)
    {
        if(parent.children() == null)
            return -1;
        int i = 0;
        for(Tree sibling : parent.children())
        {
//            if(sibling.equals(child))
            if(sibling.nodeNumber(tree) == childNodeNumber)
                return i;
            i++;
        }
        return -1;
    }
    
    public static void pruneChildrenAfter(Tree parent, int index)
    {
        int numOfChildren = parent.numChildren();
        for(int i = index + 1; i < numOfChildren; i++)
        {
            parent.removeChild(index + 1);
        }
    }
    
    public static String removeSubtreesAfterWord(String inputTree, int numOfLeaves)
    {
        Tree tree = Tree.valueOf(inputTree);
        List<Tree> leaves = tree.getLeaves();
        if(leaves.size() > numOfLeaves)
        {
            // find common ancestor between last valid leaf and extraneous leaf
            Tree firstLeaf = leaves.get(numOfLeaves - 1);
            Tree lastLeaf = leaves.get(leaves.size() - 1);
            Tree commonAncestorNode = lastLeaf.parent(tree);            
            while(!commonAncestorNode.getLeaves().contains(firstLeaf))
            {
                commonAncestorNode = commonAncestorNode.parent(tree);
            }
            // found the common ancestor, now we need to chop the children nodes the span of which is outwith the last valid leaf

            Tree p = lastLeaf.parent(tree);
            while(p != commonAncestorNode)
            {
                int numOfChildren = p.numChildren();
                for(int i = 0; i < numOfChildren; i++)
                    p.removeChild(0);     
                p = p.parent(tree);
            }
            // remove last leftover parent node of the invalid leaf
            commonAncestorNode.removeChild(commonAncestorNode.numChildren() - 1);
            return tree.toString();
        }
        else
        {        
            return inputTree;
        }        
        
    }
    
    public static String encodeToJson(JsonResult result)
    {
        try
        {
            return JsonInputWrapper.mapper.writeValueAsString(result);
        }
        catch(IOException ioe)
        {
            return JsonInputWrapper.ERROR_EXPORT_JSON.toString();
        }
    }
}
