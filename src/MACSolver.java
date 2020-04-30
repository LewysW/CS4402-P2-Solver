import java.util.*;

import static java.lang.System.exit;

/**
 * MACSolver subclass of Solver to implement maintaining arc consistency
 */
public class MACSolver extends Solver {
    /**
     * Constructor for MACSolver
     * @param binaryCSP - constraint problem to solve
     * @param heuristic - variable ordering heuristic in use
     */
    public MACSolver(BinaryCSP binaryCSP, Heuristic heuristic) {
        super(binaryCSP, heuristic);
    }

    /**
     * Main function of MACSolver
     */
    public void solve() {
        LinkedHashSet<Integer> varList = new LinkedHashSet<>();

        //Add variables to hash set for constant access
        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            varList.add(v);
        }

        //Run MAC3 algorithm on variables
        MAC3(varList);
    }

    /**
     * MAC3 algorithm to solve CSP
     * @param varList - list of unassigned variables
     */
    public void MAC3(LinkedHashSet<Integer> varList) {
        //Select variable to assign value
        int var = selectVar();
        int val = selectVal(domains.get(var));

        Stack<BinaryTuple> pruned = new Stack<>();

        //Assign variable selected value
        assign(var, val, pruned);

        //If all variables have been assigned
        if (completeAssignment()) {
            //Print the solution and exit
            printSolution();
            exit(0);

        //Re-establish arc consistency after assigning variable a value
        } else if (AC3(pruned)) {
            //Create subset of varList without var
            LinkedHashSet<Integer> subset = (LinkedHashSet<Integer>) varList.clone();
            subset.remove(var);

            //Assign next variable from subset of variables
            // as one of the variables now has an assigned value
            MAC3(subset);
        }
        //If not consistent
        //Undo pruning of variables
        undoPruning(pruned);
        //Unassign value of variable
        unassign(var);
        //Remove value from domain of variable
        remove(val, var);

        //If domain of variable is not empty
        if (!domains.get(var).isEmpty()) {
            //Establish arc consistency of right hand branch
            if (AC3(pruned)) {
                //Assign next variable
                MAC3(varList);
            }
            //If not consistent, undo pruning...
            undoPruning(pruned);
        }
        //...and restore value of variable
        restore(val, var);
    }

    /**
     * Establishes arc consistency of problem
     * @param pruned - stack to store pruned values
     * @return whether arc consistency has been established
     */
    public boolean AC3(Stack<BinaryTuple> pruned) {
        //Queue to store arcs on
        Queue<BinaryConstraint> queue = new LinkedList<>(binaryCSP.getConstraints());

        //While there are arcs/constraints left in the queue
        while (!queue.isEmpty()) {
            try {
                //Remove arc(xi, xj) in queue
                BinaryConstraint topConstraint = ((LinkedList<BinaryConstraint>) queue).pop();

                //If arcs have been revised
                if (revise(topConstraint, pruned)) {
                    //Get values xi and xj of arc
                    int xi = topConstraint.getSecondVar();
                    int xj = topConstraint.getFirstVar();

                    //Add to queue all arcs(xh, xi) where (h != j)
                    for (int xh = 0; xh < binaryCSP.getNoVariables(); xh++) {
                        //Ensures h not equal to j
                        if (xh != xj) {
                            //TODO - prevent checking repeated values in QUEUE!!!
                            //If an arc(xh, xi) exists
                            if (constraints.containsKey(xh) && constraints.get(xh).containsKey(xi)) {
                                //Add to queue
                                ((LinkedList<BinaryConstraint>) queue).push(constraints.get(xh).get(xi));
                            }
                        }
                    }
                }
            } catch (DomainEmptyException e) {
                //Return false if a variable domain becomes empty
                return false;
            }
        }

        //returns true if arcs are made consistent
        return true;
    }
}
