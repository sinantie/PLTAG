#!/bin/bash

## Commandline args:
## -i INPUTFILE: Specify path to input file (one sentence per line)
## -o OUTPUTPATH: Specify output directory
## -m MEM: Maximum memory (m for MBs, g for GBs)
## -j JOBS: Number of threads to use. If set higher than 1 then sentences are going to be parsed in parallel
## -d: Estimate processing difficulty. If used, the parser generates syntactic surprisal, verification and combined scores per word

# Save runtime directory and find PLTAG directory
OLDDIR="$PWD"
SCRIPTDIR="$(dirname "$(readlink -f "$0")")"

# Process commandline args
memory='0'
execDir='0'
inputPath='0'
numThreads='0'
estimateProcDifficulty='0'
interactiveMode=false
while getopts o:i::m::j::d:: option
do
    case "${option}"
    in
        o) execDir=${OPTARG};;
        i) inputPath=${OPTARG};;
        m) memory=${OPTARG};;
        j) numThreads=${OPTARG};;
        d) estimateProcDifficulty=true;;
    esac
done

if [ $execDir = 0 ]; then
    echo "No output directory specified (use -o to specify)"
    #execDir=output/sample_output/generative
    exit 1
elif ! [[ $execDir =~ ^/ ]]; then
    # Make output path absolute if relative
    execDir=$OLDDIR'/'$execDir
fi
if [ $inputPath = 0 ]; then
    echo "No input file specified (use -i to specify)"
    echo "Reverting to interactive mode. Enter tokenized sentences"
    interactiveMode=true
    #inputPath=input/sample_input_pltag
elif ! [[ $inputPath =~ ^/ ]]; then
    # Make input path absolute if relative
    inputPath=$OLDDIR'/'$inputPath
fi
if [ $memory = 0 ]; then
    echo "No memory limit specified. Default is 8g"
    memory=8g
else
    echo "Using memory limit:" $memory
fi
if [ $numThreads = 0 ]; then
    echo "Number of threads unspecified. Default is 2"
    numThreads=2
else
    echo "Threading:" $numThreads "jobs"
fi
if [ $estimateProcDifficulty = 0 ]; then
    estimateProcDifficulty=false
else
    echo 'Estimating PLTAG processing difficulty'
fi

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
# If set to true then the parser generates prefix trees for each word. NOTE you need to set estimateProcDifficulty=true as well.
printIncrementalDeriv=false

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
