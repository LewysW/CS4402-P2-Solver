import java.util.*;

import static java.lang.System.exit;

/**
 * Solver base class from which FC and MAC inherit
 */
public abstract class Solver {
    //Hash map to store variable assignments, a missing key has not yet had a value assigned
    protected LinkedHashMap<Integer, Integer> assignments = new LinkedHashMap<>();
    //List of variable domains to allow for the removal and addition of values
    protected ArrayList<LinkedHashSet<Integer>> domains = new ArrayList<>();
    //Constraints which represent arcs in the problem
    protected LinkedHashMap<Integer, LinkedHashMap<Integer, BinaryConstraint>> constraints = new LinkedHashMap<>();

    //Problem to solve
    protected BinaryCSP binaryCSP;
    //Heuristic strategy used
    protected Heuristic heuristic;

    protected String solution;

    /**
     * Initialises data structures to attempt to improve access time
     * to variables, domains and constraints
     * @param binaryCSP - problem to access constraints
     * @param heuristic - heuristic technique used (ascending or smallest domain first)
     */
    public Solver(BinaryCSP binaryCSP, Heuristic heuristic) {
        this.binaryCSP = binaryCSP;
        this.heuristic = heuristic;

        //For each variable
        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            //Add new domain to domains for variable v
            this.domains.add(new LinkedHashSet<>());

            //Add value d in domain for variable v
            for (int d = binaryCSP.getLB(v); d <= binaryCSP.getUB(v); d++) {
                this.domains.get(v).add(d);
            }

            //For each constraint
            for (BinaryConstraint bc : binaryCSP.getConstraints()) {
                //Add entry in map for constraints relating to variable
                if (!constraints.containsKey(bc.getFirstVar())) {
                    constraints.put(bc.getFirstVar(), new LinkedHashMap<>());
                }

                //Add entry to inner map for second variable in constraint
                constraints.get(bc.getFirstVar()).put(bc.getSecondVar(), bc);
            }
        }
    }

    /**
     * Gets arc between two variables
     * @param futureVar - future variable xi
     * @param var - current variable xj
     * @return arc(xi, xj)
     */
    protected BinaryConstraint arc(int var, int futureVar) {
        //If constraint exists for (xi, xj), return it
        if (constraints.containsKey(var) && constraints.get(var).containsKey(futureVar)) {
            return constraints.get(var).get(futureVar);
        } else if (constraints.containsKey(futureVar) && constraints.get(futureVar).containsKey(var)) {
            //Otherwise find equivalent constraint, modify it accordingly and return it
            BinaryConstraint constraint = constraints.get(futureVar).get(var);
            constraint.reverse();
            return constraint;
        } else {
            //No constraint between xi and xj
            return null;
        }
    }

    /**
     * Function to revise domains of variables
     * @param constraint - constraint between variables xi and xj
     * @param pruned - stack storing pruned values
     * @return whether or not change was made
     * @throws DomainEmptyException - throws exception is domain of xi is empty to exit early
     */
    protected boolean revise(BinaryConstraint constraint, Stack<BinaryTuple> pruned) throws DomainEmptyException {
        boolean changed = false;
        int Di_index = constraint.getSecondVar();

        //Get domain of xi
        LinkedHashSet<Integer> Di = (LinkedHashSet<Integer>) domains.get(constraint.getSecondVar()).clone();
        //Get domain of xj
        LinkedHashSet<Integer> Dj = domains.get(constraint.getFirstVar());

        //For each value di in the domain Di of xi
        for (Integer di : Di) {
            boolean supported = false;

            //For each value in the domain Dj of xj
            for (Integer dj : Dj) {
                //if xi = di and xj = dj satisfies the constraint
                if (constraint.satisfies(di, dj)) {
                    //Supported is set to true
                    supported = true;
                }
            }
            //If no pair of values xi = di and xj = dj satisfy the constraint
            if (!supported) {
                //Remove (prune) di from Di the domain of xi
                remove(di, Di_index);
                //Mark variable domain as changed
                changed = true;
                //Store pruned value for later in case it needs to be restored
                pruned.add(new BinaryTuple(Di_index, di));
            }
        }

        //If the domain Di is empty
        if (domains.get(Di_index).isEmpty()) {
            //Fail and exit early
            throw new DomainEmptyException("Domain of variable is empty!\n");
        }

        //Return whether change to domain Di was made
        return changed;
    }

    /**
     * Removes a value from the domain of a variable
     * @param val - value to remove
     * @param var - variable to reduce the domain of
     */
    protected void remove(int val, int var) {
        //Remove value from domain of variable
        domains.get(var).remove(val);
    }

    /**
     * Restores a value to the domain of a variable
     * @param val - value to restore
     * @param var - variable to have value added to its domain
     */
    protected void restore(int val, int var) {
        //Add value to domain of variable
        domains.get(var).add(val);
    }

    /**
     * Selects a variable depending on the variable ordering heuristic in use
     * @return next variable to assign
     */
    protected int selectVar(LinkedHashSet<Integer> varList) {
        //If heuristic is ascending
        if (heuristic == Heuristic.ASCENDING) {
            //Get value of next variable which has not be assigned a value
            return assignments.size();
        } else {
            int smallest = -1;

            for (int v : varList) {
                if (smallest == -1 || smallest > varList.size()) {
                    smallest = v;
                }
            };
            return smallest;
        }
    }

    /**
     * Select value from domain of chosen variable
     * @param domain - to select value from
     * @return first value in domain
     */
    protected int selectVal(LinkedHashSet<Integer> domain) {
        return domain.iterator().next();
    }

    /**
     * Assign selected value to selected variable
     * @param var - chosen variable
     * @param val - chosen value
     * @param pruned - stack of pruned domain values
     */
    protected void assign(int var, int val, Stack<BinaryTuple> pruned) {
        assignments.put(var, val);
        Iterator<Integer> domain = domains.get(var).iterator();

        //For each value di in the domain of var
        while (domain.hasNext()) {
            Integer di = domain.next();

            //If value di is not the selected value val
            if (di != val) {
                //Prune the value di from the domain
                domain.remove();

                //Store the value on the stack in case it needs to be restored
                pruned.push(new BinaryTuple(var, di));
            }
        }
    }

    /**
     * Remove from assignments
     * @param var - variable to unassign
     */
    protected void unassign(int var) {
        assignments.remove(var);
    }

    /**
     * Checks if solution has been found
     * @return whether every variable has an assignment
     */
    protected boolean completeAssignment() {
        return (assignments.size() == binaryCSP.getNoVariables());
    }

    /**
     * Prints the solution
     */
    public void printSolution() {
        System.out.println(this.toString());
    }

    /**
     * Converts the problem solution to a string
     * @return string solution
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(" Solver CSP Solution running with the ");
        result.append(heuristic.name());
        result.append(" variable ordering strategy:\n");
        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            result.append("Var ");
            result.append(v);
            result.append(": ");
            result.append(assignments.get(v));
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * Restores pruned values using stack
     * @param pruned - pruned tree values to be restored
     */
    protected void undoPruning(Stack<BinaryTuple> pruned) {
        //Loops until all pruned values have been restored
        while (!pruned.empty()) {
            //Get value and variable from stack
            int val = pruned.peek().getVal2();
            int var = pruned.pop().getVal1();

            //Restore value to domain of variable
            restore(val, var);
        }
    }
}
