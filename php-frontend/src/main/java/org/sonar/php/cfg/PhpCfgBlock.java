/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.php.cfg;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.plugins.php.api.tree.Tree;

class PhpCfgBlock implements CfgBlock {

  private Set<PhpCfgBlock> predecessors = new HashSet<>();
  protected Set<PhpCfgBlock> successors;
  private PhpCfgBlock syntacticSuccessor;
  boolean protectFromRemoval;

  private LinkedList<Tree> elements = new LinkedList<>();

  PhpCfgBlock(Set<PhpCfgBlock> successors, @Nullable PhpCfgBlock syntacticSuccessor) {
    this.successors = ImmutableSet.copyOf(successors);
    this.syntacticSuccessor = syntacticSuccessor;
  }

  PhpCfgBlock(PhpCfgBlock successor, PhpCfgBlock syntacticSuccessor) {
    this(ImmutableSet.of(successor), Preconditions.checkNotNull(syntacticSuccessor,
      "Syntactic successor cannot be null"));
  }

  PhpCfgBlock(Set<PhpCfgBlock> successors) {
    this(successors, null);
  }

  PhpCfgBlock(PhpCfgBlock successor) {
    this(ImmutableSet.of(successor));
  }

  @Override
  public Set<CfgBlock> predecessors() {
    return Collections.unmodifiableSet(predecessors);
  }

  @Override
  public Set<? extends CfgBlock> successors() {
    return successors;
  }

  @Nullable
  @Override
  public CfgBlock syntacticSuccessor() {
    return syntacticSuccessor;
  }

  @Override
  public List<Tree> elements() {
    return Collections.unmodifiableList(elements);
  }

  public void addElement(Tree element) {
    Preconditions.checkArgument(element != null, "Cannot add a null element to a block");
    elements.addFirst(element);
  }

  /**
   * Replace successors based on a replacement map.
   * This method is used when we remove empty blocks:
   * we have to replace empty successors in the remaining blocks by non-empty successors.
   */
  void replaceSuccessors(Map<PhpCfgBlock, Set<PhpCfgBlock>> replacements) {
    successors = successors.stream()
      .flatMap(successor -> replacement(successor, replacements))
      .collect(ImmutableSet.toImmutableSet());
    if (syntacticSuccessor != null && syntacticSuccessor.elements.isEmpty()) {
      List<PhpCfgBlock> replacement = replacement(syntacticSuccessor, replacements).collect(Collectors.toList());
      if (replacement.size() == 1) {
        syntacticSuccessor = Iterables.getOnlyElement(replacement);
      } else {
        // TODO find non-empty syntactic successor
        syntacticSuccessor = null;
      }
    }
  }

  /**
   * Replace oldSucc with newSucc
   */
  void replaceSuccessor(PhpCfgBlock oldSucc, PhpCfgBlock newSucc) {
    Map<PhpCfgBlock, Set<PhpCfgBlock>> map = new HashMap<>();
    map.put(oldSucc, Collections.singleton(newSucc));
    replaceSuccessors(map);
  }

  static Stream<PhpCfgBlock> replacement(PhpCfgBlock successor, Map<PhpCfgBlock, Set<PhpCfgBlock>> replacements) {
    Set<PhpCfgBlock> newSuccessor = replacements.get(successor);
    return newSuccessor == null ? Stream.of(successor) : newSuccessor.stream();
  }

  void addPredecessor(PhpCfgBlock predecessor) {
    predecessors.add(predecessor);
  }

  Set<PhpCfgBlock> skipEmptyBlocks() {
    Set<PhpCfgBlock> nonEmptySuccessors = new HashSet<>();
    Deque<PhpCfgBlock> worklist = new ArrayDeque<>(successors);
    Set<PhpCfgBlock> processed = new HashSet<>();
    while (!worklist.isEmpty()) {
      PhpCfgBlock next = worklist.pop();
      if (!processed.add(next)) {
        continue;
      }
      if (next.elements.isEmpty() && !next.isEnd()) {
        worklist.addAll(next.successors);
      } else {
        nonEmptySuccessors.add(next);
      }
    }
    return nonEmptySuccessors;
  }

  boolean isEnd() {
    return false;
  }

  @Override
  public String toString() {
    if (elements.isEmpty()) {
      return "empty";
    }
    return elements.get(0).toString();
  }
}
