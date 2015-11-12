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
package pltag.runtime;

import fig.exec.Execution;
import pltag.corpus.TagCorpus;
import pltag.corpus.TagCorpusOptions;

/**
 *
 * @author sinantie
 */
public class CreateTagCorpus implements Runnable
{

    TagCorpusOptions opts = new TagCorpusOptions();
    
    @Override
    public void run()
    {
        TagCorpus tagCorpus = new TagCorpus(opts);
        tagCorpus.init();
        tagCorpus.execute();
    }
    
    public static void main(String[] args)
    {
        CreateTagCorpus x = new CreateTagCorpus();
        Execution.run(args, x, x.opts);
    }
}
