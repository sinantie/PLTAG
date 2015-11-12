**********************************************
*                                            *
*            ------------------              *
*            PLTAG v.2.5.0-rel               *
*            ------------------              *
*                                            *
*     Psycholinguistically Motivated         *
*     Tree-Adjoining Grammar Parser          *
*                                            *
*               Authors                      *
*               -------                      *
*  Vera Demberg, vera@coli.uni-saarland.de   *
*  Ioannis Konstas, ikonstas@inf.ed.ac.uk    *
*  Frank Keller, keller@inf.ed.ac.uk         *
*                                            *
**********************************************

Thanks for downloading the Psycholinguistically Motivated Tree-Adjoining Grammar (PLTAG) Parser.
This package contains a fully incremental PLTAG parser, with iSRL capabilities and discriminative reranking extensions
as implemented in the following publications. If you are going to use/extend this package in your work (and you are most 
welcome to do so), please cite:

V. Demberg, F. Keller, A. Koller, 2013. Incremental, Predictive Parsing with Psycholinguistically Motivated Tree-Adjoining Grammar, CL 2013.
I. Konstas, V. Demberg, F. Keller, and M. Lapata, 2014. Incremental Semantic Role Labeling with Tree Adjoining Grammar, EMNLP 2014.
I. Konstas. and F. Keller, 2015. Semantic Role Labeling Improves Incremental Parsing, ACL 2015. 

We have included a list of scripts that perform all the implemented functionalities found in these papers. An extensive list of 
features can be found in FEATURES.txt. 

1. INSTALLATION

The PLTAG parser can be installed by simply extracting the contents of the zipped file into an empty folder.
You should now see the following structure:

bin/		contains the parser executable (PLTAG.jar),
data/		contains trained lexicon, and parameters on WSJ sections 02-21.,
input/		contains sample input data,
lib/		contains all the necessary libraries for PLTAG to run,
output/ 	empty folder with sample output,
resources/	contains scripts and text resources for lexicon extraction,
scripts/	contains a series of scripts that cover the core functionalities of the parser,
README.txt	this file,
FEATURES.txt	contains an extensive list of features implemented in the code.
LICENSE.txt	GNU General Public License - TLDR; the license that applies for this package.

2. GETTING STARTED

Most likely, you will just need to run the trained parser (scripts/pltag_parser.sh) for PLTAG parsing and iSRL with the trained
argument identifier/labeller classifiers (scripts/pltag_iSRL.sh). 
For the discriminative reranker model you can run (scripts/pltag_parser_discriminative.sh). NOTE: it's a memory hungry process,
so is more intended for experimental purposes/extensions!
For a full explanation of what each parameter in the script does, read below in the USAGE sections 3.2.2, 3.2.3 and 3.3.3.

3. USAGE

3.1 Extract PLTAG Lexicon and Gold standard PLTAG trees

The PLTAG parser is a lexicalised parser that uses two types of lexica, namely a standard TAG lexicon with elementary trees, 
and a -unique to PLTAG- prediction lexicon that contains unlexicalised trees only. In order to extract both lexica from a standard corpus
such as the Penn Treebank Wall Street Journal (WSJ) corpus you need to run the script:

	> scripts/create_pltag_corpus.sh

Note that you need to include several resources that are not included in this distribution; usually they are distributed by LDC.
More specifically, in order to extract lexica for WSJ you need the corpus itself (v.3) and the NP-bracketing patch by Vadas and Curran, 2007.
You need to have the original structure of the dataset that consists of .mrg files containing documents in common LISP format, split into 24 folders or tracks.
The application of the patch should leave the directory structure intact.
You also need the Proposition Bank annotation (propbank, Palmer 2005) and NomBank (Meyers et al., 2007).
The current release contains samples for each of these resources. Change the script to point to the correct paths.
More info on the parameters used are included in the script itself.
The script also creates gold standard PLTAG trees for each sentence found in the .mrg files. These can be used either for testing or training the parser.

We have extracted the PLTAG lexica from sections 02-21 of WSJ, with and without semantic (propbank) annotation. They can be found under data/lexicon/ .

3.2 Generative - PLTAG model

3.2.1 Training

In order to train the generative PLTAG model you need to either use the extracted lexicon found under data/lexicon or the one extracted using the process described above.
You also need to extract the gold standard trees and the corresponding lexicon entries for each example of the training set in a single file, using the script described above and setting the parameter:
	examplesInSingleFile=true

To train the model, run the script:
	> pltag_generative_train.sh

3.2.2 Parsing

In order to parse with the generative PLTAG model you need to either use the extracted lexicon found under data/lexicon or the one extracted using the process described above.
To parse you simply have to run the script:
	> pltag_parser.sh 

There are various options you need to change in the script to accommodate your needs, including the input, output directories, etc.
More specifically:
- memory, corresponds to the maximum heap size allocated to the JVM
- numThreads, is the number of threads to use. When set higher than 1, then multiple threads will be spawned to parse each sentence.
- execDir, refers to the output execution directory
- inputPath, is the path to the file containing sentences to be parsed
- inputType, can take one out of four choices: plain, posTagged, pltag and dundee. 
	* plain reads tokenised sentences in the input file
	* posTagged reads tab-delimited tokenised sentences, that consist of POS-tag word pairs, e.g., DT The	NN player ...
	* pltag reads sentences in the following format:
	Example_id
	tokenised sentence
	posTagged sentence
	bracket Lisp-style gold standard parse
	* dundee reads tokenised sentences, that consist of word-id pairs	
	See the accompanying examples in the input folder.
- goldPosTags, uses the provided POS-tags in the input (posTagged and pltag inputType) or uses the Stanford POS tagger on the input sentence (plain, and dundee inputType) 
instead of predicting them	
- beamSize, refers to the beam size of the parser. Higher values generally increase performance, but require more memory and slow down the parser. 400 is the default value.
- nBest is the number of n-best lists to store at each chart cell. 250 is the default value
- paramsPath, is the path to the probability model parameters, trained using the script 'pltag_generative_train.sh'.
- lexiconPath, is the path to the lexicalised and prediction trees, extracted as explained above in the 'Extract PLTAG Lexicon and Gold standard PLTAG trees' section.

3.2.2.1 Output and Evaluation

PLTAG parser can be set to generate output in three different formats:
	- difficulty scores,
	- prefix trees,
	- full sentence parse tree.
The output format can be controlled by the two following parameters:
	- estimateProcDifficulty, generates syntactic surprisal, verification and combined scores per word
	- printIncrementalDeriv, generates prefix trees. NOTE you need to set estimateProcDifficulty=true as well.
	
NOTE that the full sentence parse trees is always printed at the end of each example sentence.

In terms of evaluation the parser can automatically compute model log score, sentence F1, evalb F1, and incremental evalb F1 scores.
NOTE that you need:
	 - an input file that contains gold standard input trees (inputPath parameter), 
	 - and to set the inputType parameter to 'pltag'.
Incremental evalb F1 scores are computed when:
	 - evaluateIncrementalEvalb=true 

There are also a few more options that can be set or unset (see at the bottom of the script) that enable a couple more output-related functionalities to the parser:

- interactiveMode, sets the parser in interactive mode and accepts input from the console. Simply, enter tokenised sentences. This is the DEFAULT mode of PLTAG parser.
- outputExampleFreq, sets the frequency of outputting progress information at the stdout
- outputFullPred, outputs difficulty scores (if estimateProceDifficulty=true), prefix trees, (if printIncrementalDeriv=true), and parse tree output. Normally, leave it on.

The parser creates a folder as set in the 'execDir' parameter, that contains at least the following files:
info.map		contains generic host information, such as hostname, number of CPU cores used, memory, etc.
log			outputs various logging information (mostly what is printed on the stdout) indicating the progress of the parser
options.map		contains all the options used by the parser along with a short description
results.performance	outputs log scores, accuracy, evalb, etc., depending on whether the user is evaluating against a gold standard file (pltag inputType only)
results.evalb		contains the incremental evalb F1 scores for each word (if evaluateIncrementalEvalb is set to true)
test.full-pred-gen	contains the output of the parser per example. If 'estimateProcDifficulty' is set to true then it contains syntactic surprisal scores for each 
			word and the syntactic tree of the full sentence in the end in Common LISP format.
time.map		outputs logging info on the amount of time it took to parse the input dataset


3.2.3 Incremental Semantic Role Labeling: iSRL

In order to extract incremental Semantic Role Labels (iSRLs) you need to run the script:
	> pltag_iSRL.sh
The script essentially replicates the standard PLTAG parser, by extending it with the additional functionality
of outputting SRL triples in tandem with the prefix trees/full constituency tree. Hence, most parameters remain the
same, apart from the trained argument/label classifier paths, and optional CoNLL input format, if the user requires
to evaluate the iSRL performance.
Here is a list of the additional parameters that apply to iSRL only:

- inputPathConll, is the path to the test set in CoNLL format. Used for evaluating the iSRL model only. 
- argumentIdentifierModel, points to the trained argument identifier model,
- argumentLabellerModel, points to the trained argument labeller model,
- lexiconType, refers to the the degree to which the lexicon and the SRL annotation is going to be used by the parser in order to predict iSRLs. 
There are three different options: oracle, oracleRoles, parsedAllRoles.
	* oracle directs the parser to use gold standard lexicon entries for the input examples and gold standard oracle semantic role labels
	 (use ONLY in conjunction with a single test file that contains gold PLTAG trees and lexicon entries, 
	 and a CoNLL input file that contains gold SRL annotation)
	* oracleAllRoles lets the parser use gold standard lexicon entries only and all annotated SRL roles with ambiguity
	* parsedAllRoles (DEFAULT) gives the parser complete freedom to use the full lexicon and full SRL annotations. This is the default mode for the parser,
	as in the case of running the 'pltag_parser.sh' script.
	
- applyConllHeuristics (DEFAULT=true), applies heuristics adopted in the CoNLL 2008/9 annotation: preposition is head of a PP, infinitive marker
(IM) is head of VPs, subordinating conjunction (IN/DT) is the head in SBARs.
- useFullLexicon (DEFAULT=true), should be used in conjunction with the choice in the lexiconType option: Set to true if lexiconType=parsedAllRoles, i.e., 
in normal parser mode, or false otherwise.
- printIncrementalDependencies, output incremental (in)-complete semantic role triples (used in conjunction with gold standard/CoNLL input).
- printIncrementalCompleteDependencies, output incremental complete only semantic role triples (used in conjunction with gold standard/CoNLL input).
- evaluateIncrementalDependencies, outputs various metrics for evaluating incremental semantic role triples (used in conjunction with gold standard/CoNLL input).

The iSRL triples in the output file (test.full-pred-gen) or at the console (if running in interactive mode) should look like this:

	<73:VP:t=19,[ARGM-TMP],79:Oct.:t=19,75:occurred:t=18>
	<8:NP:t=5,[ARG1, ARG0, ARG2],8:decline:t=4,null>

Each tuple contains the following information:
	<integration-point_node_id:integration-point_node:t=timestamp, [list of labels], arg_node_id:arg:t=timestamp,pred_node_id:pred:t=timestamp>
Note that incomplete tuples (second example), may contain more than one candidate label. This is normal, since an incomplete triple has not observed yet
both argument and predicate, hence there is increased ambiguity as to which role should be assigned to the dependency. 

iSRL creates a few additional files in the folder as set in the 'execDir' parameter:
results.ups			contains the Unlabeled Prediction Score per word. This is the unlabelled attachment score between arguments and their corresponding predicate
results.ciss			contains the Combined Incremental SRL Score, i.e., measures the identification of complete semantic role triples (i.e., correct predi- cate, predicate
				sense, argument, and role label) per word; by the end of the sentence, CISS coincides with standard combined SRL accuracy, 
				as reported in CoNLL 2009 SRL-only task.
test.full-pred-gen.conll	contains the predicted iSRL output at the sentence level in CoNLL format.
gold.full-pred-gen.conll	contains the gold standard ConLL output for the sentences that were parsed, for easier reference.
test.completed-dependencies	contains complete-only incremental SRL triples. The format of each line in the file is as follows:
				<pred,[role],arg>	id:integration-point_timestamp	word: integration-point_POS integration-point_word				
				Example: <occurred,[ARG1],crash>	id: 18	word:VB occurred
		

3.3 Discriminative Reranking - PLTAG model	

If you just want to use the already trained parser (recommended) just go to 3.3.3. For the bolder among you, then follow these steps:

3.3.1 Extracting Features for Incremental Analyses (Offline Process)

- We first need to extract predicted features from each incremental analysis in the chart for each example in the training set (such as the WSJ sections 02-21), 
and save them to disk. This is a lengthy process as it involves parsing the whole training set using the generative PLTAG parser. On top of this,
some features (syntactic features) require traversing most entries in the chart (see Konstas and Keller, 2015 for exact details), 
and constructing analyses for each word, which is an extra processing burden. 

- Since we need to train a discriminative model (we have implemented the Average Structured Perceptron as described in Collins, 2002. 
Discriminative training methods for hidden markov models: Theory and experiments with perceptron algorithms.), we also need labels for each training instance.
Hence we need to also extract incremental analyses using an oracle model (again see Konstas and Keller, 2015 for the exact details), i.e., parse the training
set using gold standard lexicon entries (much faster process).

So to recap, remember that you have to extract two sets of training features from incremental analyses!
To help you out we have created two separate (and almost identical, less the parameters below).

Here is the list of common parameters you need to set only once in each script:

- inputPath			the path to a single file containing the PLTAG training examples,
- inputPathConll		the path to the CoNLL version of the tranining examples,
- incrAnalysesFeaturesPath	the path where the incremental analyses features will get saved (automatically split to segments),
- discriminativeFeatureIndexers the path the dictionaries for the various features extracted.

3.3.1.1 Predicted Feature Extraction
Run the script:
	> pltag_saveIncrAnalyses_predicted.sh
	
and make sure these parameters are set like this:
- lexiconType			parsedAllRoles (see iSRL section 3.2.3 for a full description)
- parserType			generative, i.e., use the baseline PLTAG parser with the full lexicon
- extendIndexers		false. This directive essentially creates dictionaries for the various features extracted.
- useFullLexicon		true. Use full lexica during parsing.

3.3.1.2 Oracle Feature Extraction
Run the script:
	> pltag_saveIncrAnalyses_oracle.sh
	
and make sure these parameters are set like this:
- lexiconType			oracle (see iSRL section 3.2.3 for a full description)
- parserType			oracle, i.e., use the gold standard lexicon to extract near-perfect prefix trees,
- extendIndexers		true. This directive loads the dictionaries stored in the process above and extends it with (potentially) new entries
				coming from the oracle features. It will keep the old dictionaries and automatically create new ones.
- useFullLexicon		false, i.e., use the gold standard lexicon to extract near-perfect prefix trees,
- discriminativeFeatureIndexers make sure it points to the correct path set in the previous step! Watch out for any path suffixes produced automatically by the parser.


3.3.2 Training the Perceptron Reranker (Offline Process)

3.3.3 Parsing



Copyright 2015, Ioannis Konstas, University of Edinburgh.

-------
HISTORY
-------

v.2.5.0: Added discriminative reranking support using PLTAG, Tree and SRL features.
v.2.2.0: Added web service output.
v.2.1.0: Added incremental Semantic Role Labelling support.
v.2.0.1: Added multi-thread support in conjunction with estimation of processing difficulty scores.
v.2.0.0: Major refactoring and speed-ups. Included incremental semantic role labeling, multiple input/output formats, interactive mode.
v.1.0.0: First release of code. 