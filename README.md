## Java utils

This project contains some utilities written in Java

#### Cleaner

`Cleaner` is an utility class to clean up some generic objects (can be files, DB) based on a fixed retention policy.
For example X yearly elaborations, Y monthly, Z daily etc.

#### Collections

* `Node` class to represent a tree with a Key and a Value
* `PriorityHashSet` for choosing objects with same hash based on a priority
* `Chunker` to process chunks of a collection with a common key

#### Dequeuer

`Dequeuer` is an utility for processing many items using a `BlockingQueue` and multi-threading.
It has three implementations:
* `BasicDequeuer` for basic processing with a fixed number of threads
* `BalancedDequeuer` for processing with a dynamic number of threads based on a rudimentary balancing algorithm
* `ChainedDequeuer` for processing items in a chain of dequeuers


#### Delta

`DeltaCalculator` is a simple mechanism to compare two lists and find out common/additional/missing elements from both lists
