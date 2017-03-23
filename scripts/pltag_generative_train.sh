#!/bin/bash

# Maximum memory (m for MBs, g for GBs)
memory=1000m

# Number of threads to use. If set higher than 1 then sentences are going to be parsed in parallel
numThreads=2

# Output Directory
execDir=output/train_generative

# Single input file containing gold standard trees and corresponding lexicon entries for each sentence
inputPath=input/single_wsj_00_sample

# Beam size
beamSize=400

cd ..
java -Xmx${memory} -cp bin/PLTAG.jar:lib/Helper.jar:lib/commons-collections4-4.1.jar:lib/stanford-corenlp-3.5.1.jar:stanford-corenlp-3.5.1-models.jar \
pltag.runtime.Parse \
-numThreads $numThreads \
-train \
-execDir ${execDir} \
-create \
-overwriteExecDir \
-listOfFreqWords data/wordsFreqOverFive.txt \
-beamMin ${beamSize} \
-beamEntry ${beamSize} \
-beamProp 8 \
-nBest 1 \
-examplesInSingleFile \
-inputPaths ${inputPath} \
-inputType pltag \
-outputParams \
-outputParamsTxt

#-pruneStrategyIsProp
