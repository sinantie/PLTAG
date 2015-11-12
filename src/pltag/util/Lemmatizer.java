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

import pltag.parser.semantics.discriminative.OfflineReranker;

/**
 *
 * @author sinantie
 */
public class Lemmatizer
{
    private final String word;

    public Lemmatizer(String word)
    {
        this.word = word;
        OfflineReranker.initLemmatiser();
    }
    
    public void execute()
    {
        System.out.println(OfflineReranker.lemmatise(word));
    }
    
    public static void main(String[] args)
    {
        String word = args[0];
        Lemmatizer l = new Lemmatizer(word);
        l.execute();
    }
}
