[![Build Status](https://travis-ci.org/heidisu/javazone2016-ml.svg?branch=master)](https://travis-ci.org/heidisu/javazone2016-ml)

# JavaZone 2016 - Machine learning

This repository contains code used to create examples for the talk [Maskinlæring skriver din neste presentasjon!](https://2016.javazone.no/program/maskinlaering-skriver-din-neste-presentasjon) (held in Norwegian) at [JavaZone 2016](https://2016.javazone.no/).

## Topic modeling
We have used [Mallet](http://mallet.cs.umass.edu/) for doing topic modeling with all the summary + description of all the presentations.
Good starting points for learning more about topic modeling are [Mallet Topic Modeling](http://mallet.cs.umass.edu/topics.php) and [The Programming Historian's lesson on Topic Modeling and Mallet](http://programminghistorian.org/lessons/topic-modeling-and-mallet). We have used the example code at [Topic Modeling for Java Developers](http://mallet.cs.umass.edu/topics-devel.php) as our starting point.

## Character sequences with recurrent neural network (RNN)
To genererate abstract based on the abstracts from previous years we use GravesLSTM from [Deeplearning4j](http://deeplearning4j.org/). We have two working versions, one using Spark and one custom made CharacterIterator (as in [this example](https://github.com/deeplearning4j/dl4j-examples/tree/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/recurrent/character) from Deeplearning4j). We seem to get best results with the Spark version, even with the same parameters.
Useful links to learn more about RNN:
* [A Beginner’s Guide to Recurrent Networks and LSTMs](deeplearning4j.org/lstm.html)
* [Recurrent Neural Networks in DL4J](http://deeplearning4j.org/usingrnns.html)
* [The Unreasonable Effectiveness of Recurrent Neural Networks](http://karpathy.github.io/2015/05/21/rnn-effectiveness/)
