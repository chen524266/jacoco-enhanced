/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.internal.analysis;

import java.util.BitSet;
import java.util.Collection;

import org.jacoco.core.analysis.ICounter;

/**
 * Execution status of a single bytecode instruction internally used for
 * coverage analysis. The execution status is recorded separately for each
 * outgoing branch. Each instruction has at least one branch, for example in
 * case of a simple sequence of instructions (by convention branch 0). Instances
 * of this class are used in two steps:
 *
 * <h2>Step 1: Building the CFG</h2>
 * <p>
 * For each bytecode instruction of a method a {@link Instruction} instance is
 * created. In correspondence with the CFG these instances are linked with each
 * other with the <code>addBranch()</code> methods. The executions status is
 * either directly derived from a probe which has been inserted in the execution
 * flow ({@link #addBranch(boolean, int)}) or indirectly propagated along the
 * CFG edges ({@link #addBranch(Instruction, int)}).
 *
 * <h2>Step 2: Querying the Coverage Status</h2>
 * <p>
 * After all instructions have been created and linked each instruction knows
 * its execution status and can be queried with:
 *
 * <ul>
 * <li>{@link #getLine()}</li>
 * <li>{@link #getInstructionCounter()}</li>
 * <li>{@link #getBranchCounter()}</li>
 * </ul>
 * <p>
 * For the purpose of filtering instructions can be combined to new
 * instructions. Note that these methods create new {@link Instruction}
 * instances and do not modify the existing ones.
 *
 * <ul>
 * <li>{@link #merge(Instruction)}</li>
 * <li>{@link #replaceBranches(Collection)}</li>
 * </ul>
 */
public class Instruction {

    private final int line;

    public int branches;

    public final BitSet coveredBranches;

    private Instruction predecessor;

    private int predecessorBranch;

    // 指令签名，判断method内的指令是否同一个指令，line不考虑，只考虑指令操作码，值和顺序是否一样
    private String sign;

    public int getProbeIndex() {
        return probeIndex;
    }

    public void setProbeIndex(int probeIndex) {
        this.probeIndex = probeIndex;
    }

    // 记录探针的probeId，合并的时候可以做到合并到exec文件，在每次计算合并计算完成时候保留exec文件即可，用来一下次合并
    private int probeIndex;

    /**
     * New instruction at the given line.
     *
     * @param line source line this instruction belongs to
     */
    public Instruction(final int line) {
        this.line = line;
        this.branches = 0;
        this.coveredBranches = new BitSet();
    }

    public Instruction(final int line, String sign) {
        this.line = line;
        this.branches = 0;
        this.coveredBranches = new BitSet();
        this.sign = sign;

    }

    /**
     * Adds a branch to this instruction which execution status is indirectly
     * derived from the execution status of the target instruction. In case the
     * branch is covered the status is propagated also to the predecessors of
     * this instruction.
     * <p>
     * Note: This method is not idempotent and must be called exactly once for
     * every branch.
     *
     * @param target target instruction of this branch
     * @param branch branch identifier unique for this instruction
     */
    public void addBranch(final Instruction target, final int branch) {
        branches++;
        target.predecessor = this;
        target.predecessorBranch = branch;
        if (!target.coveredBranches.isEmpty()) {
            propagateExecutedBranch(this, branch);
        }
    }

    /**
     * Adds a branch to this instruction which execution status is directly
     * derived from a probe. In case the branch is covered the status is
     * propagated also to the predecessors of this instruction.
     * <p>
     * Note: This method is not idempotent and must be called exactly once for
     * every branch.
     *
     * @param executed whether the corresponding probe has been executed
     * @param branch   branch identifier unique for this instruction
     */
    public void addBranch(final boolean executed, final int branch) {
        branches++;
        if (executed) {
            propagateExecutedBranch(this, branch);
        }
    }

    private static void propagateExecutedBranch(Instruction insn, int branch) {
        // No recursion here, as there can be very long chains of instructions
        while (insn != null) {
            if (!insn.coveredBranches.isEmpty()) {
                insn.coveredBranches.set(branch);
                break;
            }
            insn.coveredBranches.set(branch);
            branch = insn.predecessorBranch;
            insn = insn.predecessor;
        }
    }

    /**
     * Returns the source line this instruction belongs to.
     *
     * @return corresponding source line
     */
    public int getLine() {
        return line;
    }

    public String getSign() {
        return sign;
    }

    /**
     * Merges information about covered branches of this instruction with
     * another instruction.
     *
     * @param other instruction to merge with
     * @return new instance with merged branches
     */
    public Instruction merge(final Instruction other) {
        final Instruction result = new Instruction(this.line);
        result.branches = other.branches;
        result.coveredBranches.or(this.coveredBranches);
        result.coveredBranches.or(other.coveredBranches);
        return result;
    }

    public Instruction mergeNew(final Instruction other) {
        this.coveredBranches.or(other.coveredBranches);
        return this;
    }

    /**
     * Creates a copy of this instruction where all outgoing branches are
     * replaced with the given instructions. The coverage status of the new
     * instruction is derived from the status of the given instructions.
     *
     * @param newBranches new branches to consider
     * @return new instance with replaced branches
     */
    public Instruction replaceBranches(
            final Collection<Instruction> newBranches) {
        final Instruction result = new Instruction(this.line);
        result.branches = newBranches.size();
        int idx = 0;
        for (final Instruction b : newBranches) {
            if (!b.coveredBranches.isEmpty()) {
                result.coveredBranches.set(idx++);
            }
        }
        return result;
    }

    public Instruction mergeBranches(
            final Collection<Instruction> newBranches) {
        final Instruction result = new Instruction(this.line);
        result.branches = newBranches.size();
        for (final Instruction b : newBranches) {
            if (!b.coveredBranches.isEmpty()) {
                result.branches++;
            }
        }
        return result;
    }

    /**
     * Returns the instruction coverage counter of this instruction. It is
     * always 1 instruction which is covered or not.
     *
     * @return the instruction coverage counter
     */
    public ICounter getInstructionCounter() {
        return coveredBranches.isEmpty() ? CounterImpl.COUNTER_1_0
                : CounterImpl.COUNTER_0_1;
    }

    /**
     * Returns the branch coverage counter of this instruction. Only
     * instructions with at least 2 outgoing edges report branches.
     *
     * @return the branch coverage counter
     */
    public ICounter getBranchCounter() {
        if (branches < 2) {
            return CounterImpl.COUNTER_0_0;
        }
        final int covered = coveredBranches.cardinality();
        return CounterImpl.getInstance(branches - covered, covered);
    }

}
