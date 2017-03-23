#!/bin/bash

# Save runtime directory and find PLTAG directory
OLDDIR="$PWD"
SCRIPTDIR="$(dirname "$(readlink -f "$0")")"

# Maximum memory (m for MBs, g for GBs)
memory=7000m

# Number of threads to use. If set higher than 1 then sentences are going to be parsed in parallel
numThreads=2

# Output Directory
execDir=output/sample_output/generative

# Input file containing input sentences
inputPath=input/sample_input_pltag

# Input type. Currently supported types: plain, posTagged, pltag, dundee
inputType=plain

# Beam size
beamSize=400

# Nbest list size
nBest=250

# If set to true then the parser uses the provided gold POS tags (posTagged inputType and pltag), or
# predicted ones, computed using the Stanford POS tagger
goldPosTags=false

## OUTPUT
# If set to true then the parser generates syntactic surprisal, verification and combined scores per word
estimateProcDifficulty=true

# If set to true then the parser generates prefix trees for each word. NOTE you need to set estimateProcDifficulty=true as well.
printIncrementalDeriv=false

# If set to true, then the parser operates in interactive mode and accepts input from the console. Simply, enter tokenised sentences. Note, that the file set in the inputPath parameter is bypassed.
interactiveMode=true

## EVALUATION
# If set to true then the parser computes incremental evalb F1 scores
evaluateIncrementalEvalb=false

# Path and prefix to parameter files
paramsPath=data/params/0221_noCountNoneAdj_final

# Path and prefix to lexicon files
lexiconPath=data/lexicon/Lexicon_wsj_0221_noSemantics_files/Lexicon_wsj_0221_noSemantics
# Parameter files suffix
paramsSuffix=txt.final

cd $SCRIPTDIR
cd ..
java -Xmx${memory} -cp bin/PLTAG.jar:lib/Helper.jar:lib/commons-collections4-4.1.jar:lib/stanford-corenlp-3.5.1.jar:stanford-corenlp-3.5.1-models.jar:lib/concurrentlinkedhashmap-lru-1.4.jar \
pltag.runtime.Parse \
-numThreads $numThreads \
-create \
-overwriteExecDir \
-lexicon ${lexiconPath}-Freq-Parser-tag \
-predLexicon ${lexiconPath}-Freq-Parser-prediction \
-listOfFreqWords data/wordsFreqOverFive.txt \
-treeFrequencies ${paramsPath}/TreeFrequencies.${paramsSuffix} \
-wordFrequencies ${paramsPath}/WordFrequencies.${paramsSuffix} \
-superTagStruct ${paramsPath}/SuperTagStruct.${paramsSuffix} \
-superTagFringe ${paramsPath}/SuperTagFringe.${paramsSuffix} \
-beamMin ${beamSize} \
-beamEntry ${beamSize} \
-beamProp 8 \
-nBest ${nBest} \
-execDir ${execDir} \
-inputPaths ${inputPath} \
-examplesInSingleFile \
-fullLex \
-estimateProcDifficulty ${estimateProcDifficulty} \
-inputType ${inputType} \
-goldPosTags ${goldPosTags} \
-printIncrementalDeriv ${printIncrementalDeriv} \
-interactiveMode ${interactiveMode} \
-evaluateIncrementalEvalb ${evaluateIncrementalEvalb} \
-outputExampleFreq 10 \
-outputFullPred

cd $OLDDIR
