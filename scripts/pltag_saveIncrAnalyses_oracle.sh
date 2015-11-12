#!/bin/bash

# Maximum memory (m for MBs, g for GBs)
memory=7000m

# Number of threads to use. If set higher than 1 then sentences are going to be parsed in parallel
numThreads=2

# Output Directory
execDir=output/sample_output/discriminative/features

# Input file containing input sentences
inputPath=input/sample_input_pltag

# Input file containing input sentences in CoNLL format with SRL annotation
inputPathConll=input/sample_input_conll

# Add suffix to each generated file from this script
outputSuffix=sample

# Beam size
beamSize=350

# Nbest list size
nBest=250

# iSRL - Argument Classifier Paths
argumentLabellerModel=data/isrl_classifiers/bilexical_no_sense_opPath.labeller.tuned.model
argumentIdentifierModel=data/isrl_classifiers/bilexical_no_sense_opPath.identifier.weight.tuned.model
featureIndexers=data/isrl_classifiers/bilexical_no_sense_syntactic_opPath.indexers

## Extract Features
# The path where the incremental analyses features will get saved (automatically split to segments)
incrAnalysesFeaturesPath=${execDir}/incrAnalysesFeatures_wsj_0221_${outputSuffix}_sentenceLevel
# the path the dictionaries for the various features extracted
discriminativeFeatureIndexers=${execDir}/featureIndexers_${outputSuffix}_sentenceLevel

# This directive essentially creates dictionaries for the various features extracted, or extends an existing
# set of dictionaries. Set to false when extracting predicted features, and to true when extracting oracle features.
extendIndexers=true

# iSRL - Lexicon model type: set to 'oracle' in order to use gold standard lexicon entries for the
# input examples and gold standard oracle semantic role labels (oracle features), set to 'parsedAllRoles'
# to use the full lexicon and full SRL annotations (predicted features).
lexiconType=oracle

# Determine the type of parser flavour to use: generative for the standard PLTAG parser (predicted features), oracle for the
# version that uses gold standard trees for near-perfect accuracy (oracle features).
parserType=oracle

# Use the full lexicon and not just the gold standard trees.
# Set to true for extracting predicted features, and false for oracle features.
useFullLexicon=false

# iSRL - Apply heuristics adopted in the CoNLL 2008/9 annotation: preposition is head of a PP, infinitive marker
# (IM) is head of VPs, subordinating conjunction (IN/DT) is the head in SBARs. (DEFAULT=true)
applyConllHeuristics=true

# Path and prefix to parameter files
paramsPath=data/params/0221_noCountNoneAdj_final

# Path and prefix to lexicon files
lexiconPath=data/lexicon/Lexicon_wsj_0221_withSemantics_tracesFix_files/Lexicon_wsj_0221_withSemantics_tracesFix
# Parameter files suffix
paramsSuffix=txt.final

cd ..
java -Xmx${memory} -cp bin/PLTAG.jar:lib/Helper.jar:lib/commons-collections4-4.0-alpha1.jar:lib/stanford-corenlp-3.5.1.jar:stanford-corenlp-3.5.1-models.jar:lib/concurrentlinkedhashmap-lru-1.4.jar:lib/liblinear-1.94.jar \
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
-inputPaths ${inputPath} ${inputPathConll} ${inputPathConll} \
-inputType pltag \
-examplesInSingleFile \
-timeOutStage1 120000 \
-timeOutStage2 120000 \
-useSemantics \
-semanticsModel ${lexiconType} \
-fullLex ${useFullLexicon} \
-parserType ${parserType} \
-useClassifiers \
-argumentLabellerModel ${argumentLabellerModel} \
-argumentIdentifierModel ${argumentIdentifierModel} \
-featureIndexers ${featureIndexers} \
-applyConllHeuristics ${applyConllHeuristics} \
-goldPosTags \
-maxNumOfSentencesIncrAnalyses 200 \
-saveIncrAnalysesFeatures \
-incrAnalysesFeaturesPath ${incrAnalysesFeaturesPath} \
-discriminativeFeatureIndexers ${discriminativeFeatureIndexers} \
-extendIndexers ${extendIndexers} \
-maxNumOfExamples 200 \
-useGlobalSyntacticFeatures \
-outputExampleFreq 10 \
-outputFullPred


