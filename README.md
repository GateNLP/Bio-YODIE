# Bio-YODIE

Bio-YODIE is GATE's biomedical named entity linking pipeline.

THIS VERSION WORKS FOR GATE 8.5 ONLY. Please contact us if you need run Bio-YODIE on 8.4 or previous Gate version 

To reference Bio-YODIE please use [Gorrell et al. 2018](https://arxiv.org/abs/1811.04860).

## How to run Bio-YODIE
Start by downloading or cloning this repository to the location of your choice.

In order to run Bio-YODIE you will need to install GATE. See here for instructions and downloads:

https://gate.ac.uk/download/

Having got YODIE you'll need to get its submodules. YODIE uses a variety of plugins that are stored in separate git repositories, because they are independent projects. YODIE contains a list of the ones it needs, though, so you can get them easily as follows:

```
git pull --recurse-submodules=on-demand
git submodule update --init --recursive
```

Then you need set the environment variable, before compiling all the plugins:

`export GATE_HOME=/path/to/gate/`

In preparation for the next step, make sure your version of Java is between 8 and 11 (inclusive). You can do this with the following command:
`java --version`

After that you'll need to run plugins/compilePlugins.sh to compile the jar files for the plugins. (We don't ever check jar files into a git repository, so when you clone something from git you invariably need to compile it!) When you compile them, there will be some warnings but there shouldn't be any errors.

`plugins/compilePlugins.sh`


Bio-YODIE consists of a number of sub-pipelines that between them make up the application. The main application is in the main-bio directory and is called "main-bio.xgapp". This is the application you need to run using GATE. The other subpipelines will automatically be called by it.

The application will create a number of annotations on the GATE documents to which it is applied. In the default annotation set are tokens, sentences etc. In the "Bio" set are Mentions, linking mentions in text to UMLS concept unique identifiers, as well as repeat annotations for the different types of these mentions. All individual semantic types, such as "Disease or Syndrome" receive their own annotation types, and some group types are also included such as "Disease", which includes other semantic types such as "Pathologic Function". "TIMEX3" HeidelTime annotations are also present.

If you want to limit the annotations to only those CUIs in particular vocabularies, you can add the vocabularies as parameters in the lookupinfo-en/lookupinfo.xgapp pipeline. The PR "Java:subsetByVocab" takes a feature map of parameters. Simply add a feature called "VOCABS" with a value of a semicolon-separated set of vocabulary identifiers; for example, "HPO;MTH".

The pipeline requires pre-compiled resources that will need to be provided by you, as they are built from UMLS, for which you will require your own license. These can be created from your own UMLS download using the scripts provided here:

https://github.com/GateNLP/bio-yodie-resource-prep



## Adding new labels to Bio-YODIE

There are two cases, described below:

1. Adding labels that map to existing UMLS labels (and therefore CUIs)
2. Adding a new set of labels


1. Adding labels that map to existing UMLS labels (and therefore CUIs)

a. Decide whether you want your new label to be case insensitive or case sensitive. Depending on this, you will need to edit one of these files:

bio-yodie-resources/en/gazetteer-en-bio/cased-labels.lst
bio-yodie-resources/en/gazetteer-en-bio/uncased-labels.lst

b. Add lines to the file from above, of the form:

[new label][tab]abbreviationOf=[existing label]

for example,

fibrosis myocardium abbreviationOf=fibrosis myocardial

be sure to put a tab between your new label and the abbreviationOf keyword.

c. Delete the cache file that matches the gazetteer lst file you edited above, i.e. one of:

bio-yodie-resources/en/gazetteer-en-bio/cased-labels.gazbin
bio-yodie-resources/en/gazetteer-en-bio/uncased-labels.gazbin

this ensures that the cache will be regenerated from your edited file, next time you run Bio-YODIE.

d. If Bio-YODIE is loaded in GATE Developer, re-initialise the PR that matches the gazetteer list file that you edited.

2. Adding a new set of labels

If you have a set of labels that you wish to match against, but which do not map to anything in UMLS, add them to a new gazetteer in the finalize pipeline.

