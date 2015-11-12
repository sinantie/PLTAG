perl -pe 's/\t((ARG)|(MOD)) \t/\t\1\t/' $1 | grep "prediction:" | sed 's/\*\-[0-9]+/*/g' | sort | uniq -ic | perl -pe 's/^ +([0-9]+) (\w+.*)$/$1\t$2/g'
