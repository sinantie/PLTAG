#!/bin/bash

lexicon=$1
dir_path=`dirname $lexicon`
base_name=`basename $lexicon`
save_path=$dir_path/${base_name}_files
save_prefix=$save_path/$base_name
mkdir -p $save_path

echo $save_prefix

#Lexicon.txt contains complex categories, lexicalized, both prediction and non-prediction trees
cp ${lexicon} ${save_prefix}-save
perl -pe 's/\t((ARG)|(MOD)) \t/\t\1\t/' ${save_prefix}-save> ${save_prefix}

# simple categories, lexicalized -> LexSimpleCat.txt
perl -e 'while(<>){$_ =~ /^(.*\t((ARG)|(MOD))\s+)(\(.*\))$/; $prefix = $1;$tree = $5; $tree =~ s/( [A-Z]+)\-[A-Z0-9-]+([*!]?)/$1$2/g; print $prefix.$tree."\n";}' ${save_prefix} > ${save_prefix}-LexSimpleCat

# simple categories, not lexicalized -> UnlexSimpleCat.txt
perl -e  'while(<>){$_ =~ s/\s\S+(\<\>)/ $1/g; $_=~s/\*\-\d+\^/\*\^/g; print;}' ${save_prefix}-LexSimpleCat > ${save_prefix}-UnlexSimpleCat

# all in versions "-prediction" and "-tag", and counted, sorted

grep "prediction:" ${save_prefix} | cut -f 2- | sort | uniq -ic | sort -nr > ${save_prefix}-prediction.sortcount

grep -v "prediction:" ${save_prefix} | cut -f 2- | sort | uniq -ic | sort -nr > ${save_prefix}-tag.sortcount

grep "prediction:" ${save_prefix}-LexSimpleCat | cut -f 2- | sort | uniq -ic | sort -nr > ${save_prefix}-LexSimpleCat-prediction.sortcount

grep -v "prediction:" ${save_prefix}-LexSimpleCat | cut -f 2- | sort | uniq -ic | sort -nr > ${save_prefix}-LexSimpleCat-tag.sortcount

grep "prediction:" ${save_prefix}-UnlexSimpleCat | cut -f 2- | sort | uniq -ic | sort -nr > ${save_prefix}-UnlexSimpleCat-prediction.sortcount

grep -v "prediction:" ${save_prefix}-UnlexSimpleCat | cut -f 2- | sort | uniq -ic | sort -nr > ${save_prefix}-UnlexSimpleCat-tag.sortcount

#wc -l *sortcount

# lexical AMBIGUITY: (based on simple cat)

grep -v "prediction:" ${save_prefix}-LexSimpleCat |grep -v [0-9]| sort | uniq -i | sort -k 1,1 | cut -f 1 | uniq -ic | sort -rn > ${save_prefix}-ambiguity-count-simplecat 

grep -v "prediction:" ${save_prefix}-LexSimpleCat |grep -v [0-9]| sort  | uniq -i | sort  -k 1,1> ${save_prefix}-count-simplecat

xx=`wc -l ${save_prefix}-count-simplecat| awk '{print $1}'` 
yx=`wc -l ${save_prefix}-ambiguity-count-simplecat| awk '{print $1}'` 
ambiguityratex=`echo "scale = 5; $xx / $yx " | bc`
echo "average ambiguity: $ambiguityratex"

# lexical AMBIGUITY: (based on rich cat)

grep -v "prediction:" ${save_prefix} |grep -v [0-9]| sort| uniq -i| sort  -k 1,1| cut -f 1 | uniq -ic | sort -rn > ${save_prefix}-ambiguity-count-richcat 

grep -v "prediction:" ${save_prefix} |grep -v [0-9]| sort | uniq -i| sort  -k 1,1 > ${save_prefix}-count-richcat

x=`wc -l ${save_prefix}-count-richcat| awk '{print $1}'` 
y=`wc -l ${save_prefix}-ambiguity-count-richcat| awk '{print $1}'` 
ambiguityrate=`echo "scale = 5; $x / $y " | bc`
echo "average ambiguity: $ambiguityrate"

### GENERATE LEXICON FOR PARSER

grep "prediction:" ${save_prefix}-LexSimpleCat | sed 's/\*\-[0-9]+/*/g' | sort |uniq -i > ${save_prefix}-Parser-LexSimpleCat-prediction

grep -v "prediction:" ${save_prefix}-LexSimpleCat| perl -e 'while(<>){s/^(\-?\d+(([:.,]|\\\/)\d+)*\%?(th)?)( \*\S*)?\s+((ARG)|(MOD))\s+(.*\( CD )\1(\<\>\).*)$/NUM$4\t$6\t$9NUM$10/; print;}'|perl -e 'while(<>){s/^(\d+([.,]\d+)*\%?(th)?)((\-\w+)+)\s+((ARG)|(MOD))\s+(\(.*)\1(\4\<\>\)).*$/NUM$4\t$6\t$9NUM$10/; print;}'|sed 's/\*\-[0-9]+/*/g'| sort | uniq -i > ${save_prefix}-Parser-LexSimpleCat-tag

### GENERATE LEXICON WITH PROBABILITIES

grep "prediction:" ${save_prefix} | sed 's/\*\-[0-9]+/*/g' | sort |uniq -ic  | perl -pe 's/^ +([0-9]+) (\w+.*)$/$1\t$2/g' > ${save_prefix}-Freq-Parser-prediction

grep -v "prediction:" ${save_prefix} | perl -e 'while(<>){s/^(\-?\d+(([:.,]|\\\/)\d+)*\%?(th)?)( \*\S*)?\s+((ARG)|(MOD))\s+(.*\( CD )\1(\<\>\).*)$/NUM$4\t$6\t$9NUM$10/; print;}'|perl -e 'while(<>){s/^(\d+([.,]\d+)*\%?(th)?)((\-\w+)+)\s+((ARG)|(MOD))\s+(\(.*)\1(\4\<\>\).*)$/NUM$4\t$6\t$9NUM$10/; print;}'|sed 's/\*\-[0-9]+/*/g'| sort | uniq -ic | perl -pe 's/^ +([0-9]+) (\S+.*)$/$1\t$2/g'> ${save_prefix}-Freq-Parser-tag

rm ${save_prefix}
rm ${save_prefix}-save
