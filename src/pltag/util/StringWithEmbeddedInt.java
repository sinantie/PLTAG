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

/**
 *
 * @author konstas
 */
public class StringWithEmbeddedInt implements Comparable
{
    private final String value;

    public StringWithEmbeddedInt(String str)
    {
        this.value = str;
    }

    public String getValue()
    {
        return value;
    }

    public int compareTo(Object o)
    {
        assert(o instanceof StringWithEmbeddedInt);
        StringWithEmbeddedInt that = (StringWithEmbeddedInt)o;

        String s1 = value; String s2 = that.value;
        int n1 = s1.length(); int n2 = s2.length();
        int i1 = 0, i2 = 0;
        while (i1 < n1 && i2 < n2)
        {
            if (Character.isDigit(s1.charAt(i1)) && Character.isDigit(s2.charAt(i2)))
            {
                int x1 = 0;
                while (i1 < n1 && Character.isDigit(s1.charAt(i1)))
                {
                    x1 = x1 * 10 + (s1.charAt(i1) - '0');
                    i1 += 1;
                }
                int x2 = 0;
                while (i2 < n2 && Character.isDigit(s2.charAt(i2)))
                {
                    x2 = x2 * 10 + (s2.charAt(i2) - '0');
                    i2 += 1;
                }
                if (x1 != x2)
                    return x1-x2;
            }
            else
            {
                if (s1.charAt(i1) != s2.charAt(i2))
                    return s1.charAt(i1) - s2.charAt(i2);
                i1 += 1;
                i2 += 1;
            }
        }
        return n1-n2;
    }
}
