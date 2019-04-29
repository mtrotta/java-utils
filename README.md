## Java utils

This project contains some various utilities written in Java that I used in production

#### Cleaner

Utility to clean up some generic objects (can be files, DB) based on a fixed retention policy.
For example X yearly elaborations, Y monthly, Z daily etc.

#### Collections

A Node class to represent a tree with a Key and a Value
A PrioritySet for choosing which object with same key to keep in a Set based on some priority

#### Dequeuer

Utility for processing many items using a Queue and multi-threading.
It also as a rudimental auto-balance feature

#### Delta

A simple mechanism to compare two lists and find out common/additional/missing elements from both lists
