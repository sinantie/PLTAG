#!/bin/bash

# Maximum memory (m for MBs, g for GBs)
memory=200m

# Output Directory
execDir=output/sample_lexicon

# Input path containing the .mrg files of the WSJ portion of the Penn Treebank v.3 with NP bracketing (Vadas and Curran, 2007) applied.
# The folder should contain tracks in separate folders, i.e., 00/, 01/, ...
inputPath=input/ptb_sample/np_bracketing/wsj/

# Path to propbank annotation (Palmer, 2005)
propBankFilename=input/ptb_sample/propbank/prop.txt

# Path to nombank annotation (Meyers et al., 2007)
nomBankFilename=input/ptb_sample/nombank/nombank.1.0

# If set to true, then it outputs the gold standard PLTAG tree and corresponding lexicon entry in a single file (Use for training the PLTAG model).
# If set to false, then it outputs the gold standard PLTAG trees and lexicon entries in separate files (Use the PLTAG trees file, for testing the parser, and lexicon entries file for generating lexicon counts, via the resources/extractLexica.sh script).
examplesInSingleFile=true

# set to true in order to output semantically-enriched (propbank-only) lexicon
useSemantics=true

# output path that contains lexica
lexicon=Lexicon_wsj_00_sample

# output file that contains gold standard PLTAG trees for each input sentence in .mrg format
goldStandard=GoldStandard_wsj_00_sample

# output file that contains gold standard PLTAG tree and lexicon entries for each input sentence
outputFilename=single_wsj_00_sample

# start track number to read input sentences from(e.g., 02 for training, 23 for testing, 24 or 0 for development)
startTrack=0

# end track number to read input sentences from (e.g., 02 for training, 23 for testing, 24 or 0 for development)
endTrack=0


cd ..
java -Xmx${memory} -cp bin/PLTAG.jar:lib/Helper.jar:lib/commons-collections4-4.0-alpha1.jar:lib/stanford-corenlp-3.5.1.jar:stanford-corenlp-3.5.1-models.jar \
pltag.runtime.CreateTagCorpus \
-create \
-overwriteExecDir \
-execDir ${execDir} \
-ptbPath ${inputPath} \
-lexiconFilename ${lexicon} \
-goldStandardFilename ${goldStandard} \
-outputFilename ${outputFilename} \
-percolTableFilename resources/percolationTableVera.txt \
-predLexFilename resources/predlex.txt \
-propBankFilename ${propBankFilename} \
-nomBankFilename ${nomBankFilename} \
-justMultiWordLexemes \
-startTrack ${startTrack} \
-endTrack ${endTrack} \
-examplesInSingleFile ${examplesInSingleFile} \
-outputEmptyExamples \
-useSemantics ${useSemantics}

if [ $examplesInSingleFile = "false" ]; then
    resources/extractLexica.sh ${execDir}/${lexicon}
    rm -fr ${execDir}_files
fi

cd scripts
