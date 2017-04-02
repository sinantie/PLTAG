#!/bin/bash

# Maximum memory (m for MBs, g for GBs)
memory=7000m

# Number of threads to use. If set higher than 1 then sentences are going to be parsed in parallel
numThreads=1

# Output Directory
execDir=output/sample_output/iSRL

# Input file containing input sentences
inputPath=input/sample_input_pltag

# Input file containing input sentences in CoNLL format with SRL annotation
# (Use only in conjunction with lexiconType=oracle, oracleRoles, and fullLex=false)
inputPathConll=input/sample_input_conll

# Input type. Currently supported types: plain, posTagged, pltag, dundee
inputType=pltag

# Beam size
beamSize=400

# Nbest list size
nBest=250

# If set to true then the parser uses the provided gold POS tags (posTagged inputType and pltag), or
# predicted ones, computed using the Stanford POS tagger
goldPosTags=false

# iSRL - Argument Classifier Paths
argumentLabellerModel=data/isrl_classifiers/bilexical_no_sense_opPath.labeller.tuned.model
argumentIdentifierModel=data/isrl_classifiers/bilexical_no_sense_opPath.identifier.weight.tuned.model
featureIndexers=data/isrl_classifiers/bilexical_no_sense_syntactic_opPath.indexers

# iSRL - Lexicon model type: set to 'oracle' in order to use gold standard lexicon entries for the
# input examples and gold standard oracle semantic role labels (use ONLY in conjunction with a single test
# file that contains gold PLTAG trees and lexicon entries, and a CoNLL input file that contains gold
# SRL annotation), set to 'oracleAllRoles' to use gold standard lexicon entries only and all annotated
# SRL roles with ambiguity, set to 'parsedAllRoles (DEFAULT) to use the full lexicon and full SRL annotations.
lexiconType=parsedAllRoles

# iSRL - Apply heuristics adopted in the CoNLL 2008/9 annotation: preposition is head of a PP, infinitive marker
# (IM) is head of VPs, subordinating conjunction (IN/DT) is the head in SBARs. (DEFAULT=true)
applyConllHeuristics=true

# Use the full lexicon and not just the gold standard trees (DEFAULT=true).
# Use in conjunction with lexiconType=parsedAllRoles. For lexiconType=oracle, oracleRoles set to false.
useFullLexicon=true

## OUTPUT
# If set to true then the parser generates syntactic surprisal, verification and combined scores per word
estimateProcDifficulty=false

# Output incremental (in)-complete semantic role triples.
printIncrementalDependencies=true

# Output incremental complete only semantic role triples.
printIncrementalCompleteDependencies=false

# If set to true, then the parser operates in interactive mode and accepts input from the console. Simply, enter tokenised sentences. Note, that the file set in the inputPath parameter is bypassed.
interactiveMode=false

## EVALUATION
# If set to true then the parser computes incremental evalb F1 scores
evaluateIncrementalDependencies=true

# Path and prefix to parameter files
paramsPath=data/params/0221_noCountNoneAdj_final

# Path and prefix to lexicon files
lexiconPath=data/lexicon/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix
# Parameter files suffix
paramsSuffix=txt.final

cd ..
java -Xmx${memory} -cp bin/PLTAG.jar:lib/Helper.jar:lib/commons-collections4-4.1.jar:lib/stanford-corenlp-3.5.1.jar:stanford-corenlp-3.5.1-models.jar:lib/concurrentlinkedhashmap-lru-1.4.jar:lib/liblinear-1.94.jar \
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
-inputPaths ${inputPath} ${inputPathConll} \
-examplesInSingleFile \
-timeOutStage2 60000 \
-timeOutStage1 300000 \
-useSemantics \
-semanticsModel ${lexiconType} \
-fullLex ${useFullLexicon} \
-useClassifiers \
-argumentLabellerModel ${argumentLabellerModel} \
-argumentIdentifierModel ${argumentIdentifierModel} \
-featureIndexers ${featureIndexers} \
-applyConllHeuristics ${applyConllHeuristics} \
-inputType ${inputType} \
-goldPosTags ${goldPosTags} \
-estimateProcDifficulty ${estimateProcDifficulty} \
-outputIncrementalDependencies ${printIncrementalDependencies} \
-outputCompletedIncrementalDependencies ${printIncrementalCompleteDependencies} \
-interactiveMode ${interactiveMode} \
-evaluateIncrementalDependencies ${evaluateIncrementalDependencies} \
-outputExampleFreq 10 \
-outputFullPred

#-inputPaths ${inputPath} ${inputPathConll} \
