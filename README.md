# Mulib
Mulib is a library for transforming search regions specified in Java bytecode into a format that can be non-deterministically executed.
For this, the bytecode transformation replaces potentially symbolic variables and operations by variables of library types and library functionality.
This new representation of the search region is then non-deterministically executed.


# Set up for running examples

## Required software
### Java
The `de.wwu.mulib.examples` have been executed using Java 16. Please set up your PATH and JAVA_HOME accordingly.

To use -Xmx8G, `export JAVA_TOOL_OPTIONS="-Xmx8G"` can be set.

### Z3
Furthermore, a binary of Z3 is needed:
https://github.com/Z3Prover/z3/releases

For Ubuntu:
https://github.com/Z3Prover/z3/releases/download/z3-4.8.8/z3-4.8.8-x64-ubuntu-16.04.zip

Add the path to `LD_LIBRARY_PATH` so that the Z3 binary can be found. In Ubuntu, e.g.: 
`export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/usr/lib/z3-4.8.8-x64-ubuntu-16.04/bin/"`

In `build.gradle` the path `implementation files('lib/z3-4.8.8-x64-ubuntu-16.04/bin/com.microsoft.z3.jar')` 
is assumed to hold the .jar suitable for your operating system. If this is not the case, it too must be changed.

## Running the de.wwu.mulib.examples

Running the `de.wwu.mulib.examples` can be done with the following command
```
./gradlew run -Dexec.args="<Program> <Search configurations> <Number of iterations>"
```

### Programs
Available example programs are:
* `WBS` for WBS
* `P3` for 3-Partition
* `TSP` for TspSolver
* `NQ` for NQueens (with 14 queens)
* `GC` for GraphColoring
* `H` for Hanoi

### Configurations
Available search configurations for the de.wwu.mulib.examples are:
* `DFS` for single-threaded depth-first search
* `PDFS` for multi-threaded (naive) depth-first search
* `PDSAS` for multi-threaded deepest-shared-ancestor search
* `DFSN` for multi-threaded non-incremental depth-first search