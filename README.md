# CS4402-P2-Solver
Second practical for CS4402 Constraint Programming to create a constraint solver implementation.

Compilation and Execution Instructions

For convenience,  script called ’compile.sh’ has been written to simplify the process of compilation.

To compile the project using compile.sh, perform the following steps:

1.  Open a terminal window in the ’CS4402-P2-Solver/’ directory

2.  Run the following command:

./compile.sh

Another script called ’run.sh’ has also been developed to make it easier to run the constraint solver with a provided CSP instance file.

To run the constraint solver using this script, perform the following steps:

1.  Open a terminal window in the ’CS4402-P2-Solver/’ directory

2.  Run the following command:

./run.sh <CSP file name> <algorithm> <heuristic>

Where <CSP file name> is a valid CSP instance file, <algorithm> is either fc or mac (for forward checking or maintaining arc consistency), and <heuristic> is either a or s (for ascending or smallest-domain first). 

For example:./run.sh 10Queens.csp fc a
