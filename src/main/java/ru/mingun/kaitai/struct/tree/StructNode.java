/*
 * The MIT License
 *
 * Copyright 2020 Mingun.
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

import io.kaitai.struct.KaitaiStruct;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Collections.enumeration;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.swing.tree.TreeNode;

/**
 * Node, that represents single {@link KaitaiStruct} object. Each struct field
 * represented as child node.
 *
 * @author Mingun
 */
public class StructNode extends ChunkNode {
  private final KaitaiStruct value;
  private final ChunksNode fields;
  private final List<TreeNode> children;

  private final Map<String, Integer> attrStart;
  private final Map<String, Integer> attrEnd;
  private final Map<String, ? extends List<Integer>> arrStart;
  private final Map<String, ? extends List<Integer>> arrEnd;

  /**
   * Constructor used to create node for representing root structure.
   *
   * @param name displayed name of the structure
   * @param value the root structure, represented by this node
   * @param parent parent node, for including node in hierarchy
   *
   * @throws ReflectiveOperationException If kaitai class was genereted without
   *         debug info (which includes position information)
   */
  public StructNode(String name, KaitaiStruct value, TreeNode parent) throws ReflectiveOperationException {
    this(name, value, parent, 0, 0, value._io().pos());
  }
  StructNode(String name, KaitaiStruct value, TreeNode parent, int offset, int start, int end) throws ReflectiveOperationException {
    super(name, parent, offset, start, end);
    final Class<?> clazz = value.getClass();
    // getDeclaredMethods() doesn't guaranties any particular order, so sort fields
    // according order in the type
    final String[] names = (String[])clazz.getField("_seqFields").get(null);
    final List<String> order = Arrays.asList(names);

    final ArrayList<Method> fields = new ArrayList<>();
    final ArrayList<Method> params = new ArrayList<>();
    final ArrayList<Method> instances = new ArrayList<>();
    for (final Method m : clazz.getDeclaredMethods()) {
      // Skip static methods, i.e. "fromFile"
      // Skip all internal methods, i.e. "_io", "_parent", "_root"
      if (Modifier.isStatic(m.getModifiers()) || m.getName().charAt(0) == '_') {
        continue;
      }
      if (order.contains(m.getName())) {
        fields.add(m);
      } else {
        // TODO: Distinguish between parameters and instances
        params.add(m);
      }
    }

    fields.sort((Method m1, Method m2) -> {
      final int pos1 = order.indexOf(m1.getName());
      final int pos2 = order.indexOf(m2.getName());
      return pos1 - pos2;
    });

    this.value     = value;
    this.fields    = new ChunksNode("Fields", fields, this);
    this.children  = Arrays.asList(
      new ParamsNode(params, this),
      this.fields,
      new ChunksNode("Instances", instances, this)
    );
    this.attrStart = (Map<String, Integer>)clazz.getDeclaredField("_attrStart").get(value);
    this.attrEnd   = (Map<String, Integer>)clazz.getDeclaredField("_attrEnd").get(value);
    this.arrStart  = (Map<String, ? extends List<Integer>>)clazz.getDeclaredField("_arrStart").get(value);
    this.arrEnd    = (Map<String, ? extends List<Integer>>)clazz.getDeclaredField("_arrEnd").get(value);
  }

  @Override
  public KaitaiStruct getValue() { return value; }

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public TreeNode getChildAt(int childIndex) { return children.get(childIndex); }

  @Override
  public int getChildCount() { return children.size(); }

  @Override
  public int getIndex(TreeNode node) { return children.indexOf(node); }

  @Override
  public boolean getAllowsChildren() { return true; }

  @Override
  public boolean isLeaf() { return false; }

  @Override
  public Enumeration<? extends TreeNode> children() { return enumeration(children); }
  //</editor-fold>

  @Override
  public String toString() {
    return name + " [" + value.getClass().getSimpleName()
      + "; fields = " + fields.getChildCount()
      + "; offset = " + getStart()
      + "; size = " + size()
      + "]";
  }

  /**
   * Creates tree node for the specified struct field
   *
   * @param getter Method, that used to get data from structure
   * @return New tree node object, that represents value in the tree
   *
   * @throws ReflectiveOperationException If kaitai class was genereted without
   *         debug info (which includes position information)
   */
  ChunkNode create(Method getter, ChunksNode parent) throws ReflectiveOperationException {
    final Object field = getter.invoke(value);
    final String name  = getter.getName();
    final int s = attrStart.get(name);
    final int e = attrEnd.get(name);
    if (List.class.isAssignableFrom(getter.getReturnType())) {
      final List<Integer> sa = arrStart.get(name);
      final List<Integer> se = arrEnd.get(name);
      return new ListNode(name, (List<?>)field, parent, offset, s, e, sa, se);
    }
    return create(name, field, start, s, e);
  }
}
