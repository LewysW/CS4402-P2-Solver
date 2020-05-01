import java.util.LinkedHashSet;
import java.util.Stack;

import static java.lang.System.exit;

/**
 * FCSolver subclass of Solver to implement the forward checking algorithm
 */
public class FCSolver extends Solver {
    /**
     * Constructor for FCSolver
     * @param binaryCSP - constraint problem to solve
     * @param heuristic - variable ordering heuristic in use
     */
    public FCSolver(BinaryCSP binaryCSP, Heuristic heuristic) {
        super(binaryCSP, heuristic);
    }

    /**
     * Main function for FCSolver
     */
    public void solve() {
        LinkedHashSet<Integer> varList = new LinkedHashSet<>();

        //Add variables to hash set for constant access
        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            varList.add(v);
        }

        //Run FC algorithm on unassigned variables
        forwardChecking(varList);
    }

    /**
     * FC algorithm to solve CSP
     * @param varList - list of unassigned variables
     */
    private void forwardChecking(LinkedHashSet<Integer> varList) {
        //If all variables have been assigned
        if (completeAssignment() && !solved) {
            //Print the solution and exit
            printSolution();
        } else if (!solved) {
            //Select variable to assign a value
            int var = selectVar(varList);
            int val = selectVal(domains.get(var));

            //Run left branch
            branchFCLeft(varList, var, val);
            //Run right branch
            branchFCRight(varList, var, val);
        }
    }

    /**
     * Left branch of FC algorithm
     * @param varList - list of unassigned variables
     * @param var - variable to assign a value
     * @param val - value to assign
     */
    private void branchFCLeft(LinkedHashSet<Integer> varList, int var, int val) {
        Stack<BinaryTuple> pruned = new Stack<>();
        //Assign value to variable
        assign(var, val, pruned);
        //If future arcs were revised successfully
        if (reviseFutureArcs(varList, var, pruned)) {
            //Create subset of varList without var
            LinkedHashSet<Integer> subset = (LinkedHashSet<Integer>) varList.clone();
            subset.remove(var);

            //Assign next variable from the subset of remaining variables
            //as one variable has now been assigned a value
            forwardChecking(subset);
        }
        //if this branch did not result in a solution
        //undo pruning
        undoPruning(pruned);
        //Undo assignment
        unassign(var);
    }

    /**
     * Right branch of FC algorithm
     * @param varList - list of unassigned variables
     * @param var - variable to assign a value
     * @param val - value to assign variable
     */
    private void branchFCRight(LinkedHashSet<Integer> varList, int var, int val) {
        //Remove value assign by previous left
        //as the right branch represents not(val)
        remove(val, var);
        Stack<BinaryTuple> pruned = new Stack<>();

        //if the domains of the current variable is not empty
        if (!domains.get(var).isEmpty()) {
            //Attempt arc revisions using variable
            if (reviseFutureArcs(varList, var, pruned)) {
                //If revisions were successful, recurse
                forwardChecking(varList);
            }
            //Otherwise if right branch did not return a solution
            //undo pruning
            undoPruning(pruned);
        }
        //Restore value to variable
        restore(val, var);
    }

    /**
     * Revises domain of future arcs
     * @param varList - list of unassigned variables
     * @param var - variable to assign a value
     * @param pruned - stack of pruned domain values
     * @return whether revisions of future arcs were successful
     */
    private boolean reviseFutureArcs(LinkedHashSet<Integer> varList, int var, Stack<BinaryTuple> pruned) {
        //For each future variable which is not var
        for (int futureVar : varList) {
            if (futureVar != var) {
                try {
                    //If an arc exists between the two variables
                    if (arc(var, futureVar) != null) {
                        //Revise the domain of the future variable
                        revise(arc(var, futureVar), pruned);
                    }
                 //Returns false iff a domain is emptied by a revision
                } catch (DomainEmptyException e) {
                    return false;
                }
            }
        }

        //returns true if each arc was revised
        // successfully without a domain being emptied
        return true;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("FC");
        result.append(super.toString());
        return result.toString();
    }
}
