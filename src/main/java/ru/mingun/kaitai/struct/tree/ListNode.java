/*
 * The MIT License
 *
 * Copyright 2020-2021 Mingun.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.mingun.kaitai.struct.tree;

import java.util.ArrayList;
import static java.util.Collections.enumeration;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreeNode;

/**
 * Node, that represents a repeated data in struct definition. An each repeated value
 * represented as a child node.
 *
 * @author Mingun
 */
public class ListNode extends ChunkNode {
  private final List<?> value;
  /** Lazy populated list of child nodes. */
  private List<ChunkNode> children;
  /** Start positions in root stream of each value object in {@link #value}. */
  private final List<Integer> arrStart;
  /** Endo positions in root stream of each value object in {@link #value} (exclusive). */
  private final List<Integer> arrEnd;

  ListNode(String name, List<?> value, StructNode parent,
    long offset, long start, long end,
    List<Integer> arrStart,
    List<Integer> arrEnd,
    boolean isSequential
  ) {
    super(name, parent, offset, start, end, isSequential);
    this.value = value;
    this.arrStart = arrStart;
    this.arrEnd   = arrEnd;
  }

  @Override
  public List<?> getValue() { return value; }

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public ChunkNode getChildAt(int childIndex) { return init().get(childIndex); }

  @Override
  public int getChildCount() { return value.size(); }

  @Override
  public int getIndex(TreeNode node) { return init().indexOf(node); }

  @Override
  public boolean getAllowsChildren() { return true; }

  @Override
  public boolean isLeaf() { return value.isEmpty(); }

  @Override
  public Enumeration<? extends ChunkNode> children() { return enumeration(init()); }
  //</editor-fold>

  @Override
  public String toString() {
    return name + " [count = " + value.size() + "; offset = " + getStart() + "; size = " + size() + "]";
  }

  private List<ChunkNode> init() {
    if (children == null) {
      children = new ArrayList<>(value.size());
      int index = 0;
      for (final Object obj : value) {
        try {
          final int s = arrStart.get(index);
          final int e = arrEnd.get(index);
          children.add(create("[" + index + ']', obj, 0, s, e, isSequential));
          ++index;
        } catch (ReflectiveOperationException ex) {
          throw new UnsupportedOperationException("Can't get list value at index " + index, ex);
        }
      }
    }
    return children;
  }
}
