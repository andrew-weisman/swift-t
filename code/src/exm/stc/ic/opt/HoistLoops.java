package exm.stc.ic.opt;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import exm.stc.common.CompilerBackend.WaitMode;
import exm.stc.common.lang.Arg;
import exm.stc.common.lang.Types;
import exm.stc.common.lang.Types.Type;
import exm.stc.common.lang.Var;
import exm.stc.common.lang.Var.DefType;
import exm.stc.common.lang.Var.VarStorage;
import exm.stc.common.util.HierarchicalMap;
import exm.stc.ic.tree.ICContinuations.Continuation;
import exm.stc.ic.tree.ICContinuations.ContinuationType;
import exm.stc.ic.tree.ICContinuations.WaitStatement;
import exm.stc.ic.tree.ICInstructions.Instruction;
import exm.stc.ic.tree.ICTree.Block;
import exm.stc.ic.tree.ICTree.Function;
import exm.stc.ic.tree.ICTree.Program;

public class HoistLoops {
  public static void hoist(Logger logger, Program prog) {
    for (Function f: prog.getFunctions()) {
      Block block = f.getMainblock();
      HierarchicalMap<String, Block> globalMap =
                      new HierarchicalMap<String, Block>();
      // Global constants already written
      for (String gc: prog.getGlobalConsts().keySet()) {
        Var gcV = new Var(prog.lookupGlobalConst(gc).getType(), gc,
                VarStorage.GLOBAL_CONST, DefType.GLOBAL_CONST);
        if (trackVar(gcV)) { 
          globalMap.put(gc, null);
        }
      }
      
      // Set up map for top block of function
      HierarchicalMap<String, Block> writeMap = globalMap.makeChildMap();
      // Inputs are written
      for (Var in: f.getInputList()) {
        if (trackVar(in)) {
          writeMap.put(in.name(), block);
        }
      }
      hoistRec(logger, block, new ArrayList<Block>(), 0, writeMap);
    }
    /*
    StringBuilder sb = new StringBuilder();
    prog.prettyPrint(sb);
    System.err.println(sb.toString()); */
    // Might need to be updated
    FixupVariables.fixupVariablePassing(logger, prog);
  }
  
  /**
   * 
   * @param logger
   * @param curr current block
   * @param ancestors ancestors of current block
   * @param maxHoist maximum number of blocks can lift out
   * @param writeMap map for current block filled in with anything defined 
   *                by construct or outer blocks
   */
  private static void hoistRec(Logger logger, Block curr, List<Block> ancestors,
            int maxHoist, HierarchicalMap<String, Block> writeMap) {
    // Update map with variables written in this block
    updateMapWithWrites(curr, writeMap);
    
    // See if we can move any instructions from this block up
    if (maxHoist > 0) {
      tryHoist(logger, curr, ancestors, maxHoist, writeMap);
    }
    
    // Recurse down to child blocks
    ancestors.add(curr);
    for (Continuation c: curr.getContinuations()) {    
      int childHoist = canHoistThrough(c) ? maxHoist + 1 : 0;
      for (Block b: c.getBlocks()) {
        HierarchicalMap<String, Block> childWriteMap = writeMap.makeChildMap();
        List<Var> constructVars = c.constructDefinedVars();
        if (constructVars != null) {
          for (Var v: constructVars) {
            if (trackVar(v)) {
              childWriteMap.put(v.name(), b);
            }
          }
        }
        hoistRec(logger, b, ancestors, childHoist, childWriteMap);
      }
    }
    ancestors.remove(ancestors.size() - 1);
  }

  private static void updateMapWithWrites(Block curr,
          HierarchicalMap<String, Block> writeMap) {
    for (Instruction inst: curr.getInstructions()) {
      for (Var out: inst.getOutputs()) {
        if (trackVar(out)) {
          writeMap.put(out.name(), curr);
        }
      }
    }
  }

  /**
   * True if the variable is one we should track
   * @param in
   * @return
   */
  private static boolean trackVar(Var in) {
    Type t = in.type();
    if (Types.isScalarFuture(t) || Types.isScalarValue(t)) {
      return true;
    } else if (Types.isRef(t) && Types.isScalarFuture(t.memberType())) {
      return true;
    }
    return false;
  }

  private static boolean canHoistThrough(Continuation c) {
    if (c.getType() == ContinuationType.FOREACH_LOOP ||
          c.getType() == ContinuationType.RANGE_LOOP ||
          c.getType() == ContinuationType.LOOP ||
          c.getType() == ContinuationType.NESTED_BLOCK) {
      return true;
    } else if (c.getType() == ContinuationType.WAIT_STATEMENT &&
            ((WaitStatement)c).getMode() == WaitMode.DATA_ONLY) {
      return true;
    }
    return false;
  }

  private static void tryHoist(Logger logger, Block curr,
          List<Block> ancestors, int maxHoist,
          HierarchicalMap<String, Block> writeMap) {
    // See if we can lift any instructions out of block
    ListIterator<Instruction> it = curr.instructionIterator();
    while (it.hasNext()) {
      Instruction inst = it.next();
      if (inst.hasSideEffects()) {
        // Don't try to mess with things with side-effects
        continue;
      }
      
      // See where the input variables were written
      // minDepth: how many blocks out can be hoisted
      int minDepth = -1;
      boolean canHoist = true;
      for (Arg in: inst.getInputs()) {
        if (in.isVar()) {
          int depth = writeMap.getDepth(in.getVar().name());
          if (depth < 0) {
            // Not written
            canHoist = false;
            break;
          } else if (minDepth < 0 || depth < minDepth) {
            minDepth = depth;
          }
        }
      }
      
      if (canHoist) {
        // Max hoist for instruction determined by inputs and maxHoist
        int hoistDepth;
        if (minDepth < 0) {
          // Case where no variables
          hoistDepth = maxHoist;
        } else {
          hoistDepth = Math.min(maxHoist, minDepth);
        }

        if (hoistDepth > 0) {
          doHoist(logger, ancestors, curr, inst, it, hoistDepth, writeMap);
        }
      }
    }
  }

  private static void doHoist(Logger logger,
          List<Block> ancestors, Block curr,
          Instruction inst, ListIterator<Instruction> currInstIt,
          int hoistDepth, HierarchicalMap<String, Block> writeMap) {
    assert(hoistDepth > 0);
    assert(hoistDepth <= ancestors.size());
    Block target = ancestors.get(ancestors.size() - hoistDepth);
    logger.trace("Hoisting instruction up " + hoistDepth + " blocks: "
                 + inst.toString());
    assert(target != null);
    
    // Move the instruction
    currInstIt.remove();
    target.addInstruction(inst);
    
    // Move variable declaration if needed to outer block.
    relocateVarDefs(curr, target, ancestors, inst, hoistDepth);
    
    // need to update write map to reflect moved instruction
    for (Var out: inst.getOutputs()) {
      writeMap.remove(out.name());
      writeMap.put(out.name(), target, hoistDepth);
    }
  }

  private static void relocateVarDefs(Block curr, Block target,
          List<Block> ancestors, Instruction inst, int hoistDepth) {
    // Rely on variable passing being fixed up later
    for (int i = 0; i < hoistDepth; i++) {
      Block ancestor;
      if (i == 0) {
        ancestor = curr;
      } else {
        int ancestorPos = ancestors.size() - i;
        ancestor = ancestors.get(ancestorPos);
      } 
      ListIterator<Var> varIt = ancestor.variableIterator();
      while (varIt.hasNext()) {
        Var def = varIt.next();
        for (Var out: inst.getOutputs()) {
          if (def.name().equals(out.name())) {
            varIt.remove();
            target.addVariable(def);
            break;
          }
        }
      }
    }
  }
}
