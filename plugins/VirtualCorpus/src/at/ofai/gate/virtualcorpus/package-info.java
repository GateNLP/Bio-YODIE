/**
 * This package implements the VirtualCorpus plugin which
 * provides new corpus LRs that represent directories or JDBC tables as a
 * corpus.
 *
 * The purpose of this plugin is to make it very simple to just run existing
 * corpus controllers on the documents in a directory or on documents stored
 * in a JDBC table. Thus, the documents do not have to first get imported
 * into a serial corpus in a datastore, than get exported back.
 * Since the intended use of the directory-backed and table-backed corpora
 * is simply to access, process and write back documents, the implementations
 * do not support adding or removing documents at this time: all VirtualCorpus
 * LRs are currently immutable (i.e. the content of corpus itself cannot be
 * changed, but the documents can of course be changed). 
 */
package at.ofai.gate.virtualcorpus;
