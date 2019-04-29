package org.matteo.utils.collection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 03/05/12
 * Time: 16.35
 */
class NodeTest {

    enum NodeType {
        N1,
        N2,
        N3,
        LEAF
    }
    
    private class Data {
        private final Node<String, NodeType> root = new Node<>();
        private final Node<String, NodeType> child = new Node<>("1_Child");
        private final Node<String, NodeType> sameChild = new Node<>("1_Child");
        private final Node<String, NodeType> empty = new Node<>();
        private final Node<String, NodeType> leaf = new Node<>("1_Leaf", NodeType.LEAF);

        private final Node<String, NodeType> root2 = new Node<>();
        private final Node<String, NodeType> child2 = new Node<>("2_Child");
        private final Node<String, NodeType> sameChild2 = new Node<>("2_Child");
        private final Node<String, NodeType> leaf2 = new Node<>("2_Leaf");
    }

    private Node.Filter<Node<String, NodeType>> filterN1 = n -> NodeType.N1.equals(n.getValue());
    private Node.Filter<Node<String, NodeType>> filterN2 = n -> NodeType.N2.equals(n.getValue());

    @Test
    void testBase() {
        Data data = new Data();
        assertTrue(data.root.isEmpty());
        assertTrue(data.root.isRoot());
        assertTrue(data.empty.isEmpty());
    }

    @Test
    void testNoValue() {
        Node<String, String> r = new Node<>();
        Node<String, String> c = new Node<>("data.child");
        r.add(c);
        assertEquals(1, r.size());
        assertNull(r.getValue());
        assertNull(c.getValue());
    }

    @Test
    void testEquals() {
        Data data = new Data();
        assertEquals(data.root, data.root2);
        assertEquals(data.child, data.sameChild);
        assertEquals(data.child2, data.sameChild2);
        assertEquals(data.empty, data.empty);

        data.root.add(data.child);
        data.root2.add(data.sameChild);

        assertEquals(data.child, data.sameChild);

        data.root.destroy();
        data.root2.destroy();

        data.root.add(data.leaf).add(data.child);
        data.root2.add(data.leaf2).add(data.sameChild);

        assertNotEquals(data.child, data.sameChild);
    }

    @Test
    void testPosition() {
        Data data = new Data();
        data.root.add(data.child);
        data.root.add(data.child2);
        assertEquals(0, data.child.getPosition());
        assertEquals(1, data.child2.getPosition());
    }

    @Test
    void testReplace() {
        Data data = new Data();
        assertTrue(data.root.isLeaf());
        assertTrue(data.root.isRoot());

        data.root.replace(data.child);
        assertFalse(data.root.isLeaf());
        assertTrue(data.child.isLeaf());
        assertTrue(data.root.contains(data.child));
        assertTrue(data.root.containsKey(data.child.getKey()));
        assertEquals(data.root, data.child.getRoot());
        assertEquals(data.root, data.child.getParent());
        assertEquals(data.child, data.root.getChild(data.child));
        assertEquals(data.child, data.root.getFirstChild());
        assertEquals(1, data.root.getChildren().size());
        assertEquals(1, data.root.getChildrenList().size());
        assertEquals(1, data.root.getChildCount());
        assertEquals(1, data.root.getLeaves().size());
        assertEquals(1, data.root.getLeafMap().size());
        assertEquals(1, data.root.size());

        data.child.replace(data.leaf);
        assertFalse(data.root.isLeaf());
        assertFalse(data.child.isLeaf());
        assertFalse(data.root.contains(data.leaf));
        assertTrue(data.child.contains(data.leaf));
        assertEquals(data.root, data.leaf.getRoot());
        assertEquals(data.child, data.leaf.getParent());
        assertEquals(data.leaf, data.child.getChild(data.leaf));
        assertEquals(1, data.root.getChildCount());
        assertEquals(1, data.root.getLeaves().size());
        assertEquals(2, data.root.size());

        data.root.destroy();
        assertEquals(0, data.root.size());
    }

    @Test
    void testReplaceTo() {
        Data data = new Data();
        Node<String, NodeType> ret = data.child.replaceTo(data.root);

        assertSame(data.root, ret);
        assertTrue(data.root.isRoot());
        assertFalse(data.root.isLeaf());
        assertTrue(data.child.isLeaf());
        assertFalse(data.child.isRoot());
        assertTrue(data.root.contains(data.child));

        data.empty.replaceToCollapseEmpty(data.child);
        assertTrue(data.child.isLeaf());

        ret = data.leaf.replaceTo(data.empty).replaceToSkipEmpty(data.child);
        assertSame(data.child, ret);
        assertTrue(data.child.isLeaf());

        ret = data.leaf.replaceTo(data.empty).replaceToCollapseEmpty(data.child);
        assertSame(data.child, ret);
        assertFalse(data.child.isLeaf());
    }

    @Test
    void testAdd() {
        Data data = new Data();
        Node<String, NodeType> ret = data.root.add(data.child);
        assertSame(data.child, ret);
        assertEquals(data.child, data.root.getChild(data.child));
        data.root.add(data.sameChild);
        assertSame(data.child, data.root.getChild(data.child));
        assertNotSame(data.sameChild, data.root.getChild(data.child));
        assertNotSame(data.sameChild, data.root.getChild(data.sameChild));
        assertEquals(data.child, data.root.add(data.sameChild));
    }

    @Test
    void testAddTo() {
        Data data = new Data();
        Node<String, NodeType> ret = data.child.addTo(data.root);

        assertSame(ret, data.root);

        assertEquals(data.child, data.root.getChild(data.child));
        assertTrue(data.child.isLeaf());
        assertFalse(data.root.isLeaf());
        assertSame(data.root, data.child.getRoot());

        ret = data.sameChild.addTo(data.root);

        assertSame(data.root, ret);
        assertSame(data.child, data.root.getChild(data.sameChild));

        data.empty.addToCollapseEmpty(data.child);
        assertTrue(data.child.isLeaf());

        ret = data.leaf.addTo(data.empty).addToSkipEmpty(data.child);
        assertSame(data.child, ret);
        assertTrue(data.child.isLeaf());

        ret = data.leaf.addTo(data.empty).addToCollapseEmpty(data.child);
        assertSame(data.child, ret);
        assertFalse(data.child.isLeaf());
    }

    @Test
    void testRemove() {
        Data data = new Data();
        data.root.add(data.child);
        assertTrue(data.root.contains(data.child));
        assertSame(data.root, data.child.getParent());
        data.root.remove(data.child);
        assertFalse(data.root.contains(data.child));
        assertTrue(data.root.isLeaf());
        assertNull(data.child.getParent());

        data.root.add(data.child).add(data.sameChild);

        data.sameChild.removeBranch();
        assertTrue(data.root.isLeaf());

        data.root.add(data.child).addSibling(data.child2);
        assertEquals(2, data.root.getChildCount());
        List<Node<String, NodeType>> list = new ArrayList<>();
        list.add(data.child);
        list.add(data.child2);
        data.root.removeAll(list);
        assertTrue(data.root.isLeaf());

        data.root.destroy();
        data.root.add(data.child).addSibling(data.child2).add(data.leaf);
        data.leaf.removeBranch();
        assertTrue(data.child.isLeaf());

        data.root.destroy();
        data.root.add(data.child).addSibling(data.child2).add(data.leaf);
        data.leaf.removeBranch();
        assertTrue(data.child.isLeaf());
    }

    @Test
    void testReplaceAll() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.leaf);
        data.root2.replace(data.child2).replace(data.leaf2);

        data.root.replaceAll(data.root2.getChildren());

        assertEquals(2, data.root.getChildCount());
        assertTrue(data.root.contains(data.child));
        assertTrue(data.root.contains(data.child2));

        data.root.destroy();
        data.root2.destroy();
        data.root.replace(data.child).replace(data.leaf);
        data.root2.replace(data.sameChild).replace(data.child2).replace(data.leaf2);

        data.root.replaceAll(data.root2.getChildren());

        assertEquals(1, data.root.getChildCount());
        assertFalse(data.root.getChild(data.child).contains(data.leaf));
        assertSame(data.sameChild, data.root.getChild(data.child));
        assertSame(1, data.root.getChild(data.child).getChildCount());
        assertSame(1, data.root.getLeaves().size());

        data.root.destroy();
        data.root2.destroy();
        data.root.replace(data.child).replace(data.leaf);
        data.root2.replace(data.sameChild).replace(data.empty).replace(data.leaf2);
    }

    @Test
    void testReplaceCollapseEmpty() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.leaf);
        data.sameChild.replace(data.empty).replace(data.leaf2);

        Node<String, NodeType> ret = data.root.replaceCollapseEmpty(data.sameChild);

        assertSame(data.sameChild, ret);
        assertEquals(1, data.root.getChildCount());
        assertFalse(data.root.getChild(data.child).contains(data.leaf));
        assertTrue(data.root.getChild(data.child).contains(data.leaf2));
    }

    @Test
    void testReplaceSkipEmpty() {
        Data data = new Data();
        Node<String, NodeType> ret = data.root.replace(data.child).replaceSkipEmpty(data.empty).replaceSkipEmpty(data.leaf2);

        assertSame(data.leaf2, ret);
        assertEquals(1, data.root.getChildCount());
        assertTrue(data.root.getChild(data.child).isLeaf());
    }

    @Test
    void testAddCollapseEmpty() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.leaf);
        data.sameChild.replace(data.empty).replace(data.leaf2);

        Node<String, NodeType> ret = data.root.addCollapseEmpty(data.sameChild);

        assertSame(data.child, ret);
        assertEquals(1, data.root.getChildCount());
        assertTrue(data.root.getChild(data.child).contains(data.leaf));
        assertFalse(data.root.getChild(data.child).contains(data.empty));
        assertTrue(data.root.getChild(data.child).contains(data.leaf2));
    }

    @Test
    void testAddSkipEmpty() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.leaf);
        data.sameChild.replace(data.empty).replace(data.leaf2);

        Node<String, NodeType> ret = data.root.addSkipEmpty(data.sameChild);

        assertSame(data.child, ret);
        assertEquals(1, data.root.getChildCount());
        assertFalse(data.root.getChild(data.child).isLeaf());
        assertTrue(data.root.getChild(data.child).contains(data.leaf));
        assertFalse(data.root.getChild(data.child).contains(data.empty));
        assertFalse(data.root.getChild(data.child).contains(data.leaf2));
    }

    @Test
    void testAddAll() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.leaf);
        data.root2.replace(data.child2).replace(data.leaf2);

        Node<String, NodeType> ret = data.root.addAll(data.root2.getChildren());

        assertSame(data.root, ret);
        assertEquals(2, data.root.getChildCount());
        assertTrue(data.root.contains(data.child));
        assertTrue(data.root.contains(data.child2));

        data.root.destroy();
        data.root2.destroy();

        data.root.replace(data.child).replace(data.leaf);
        data.root2.replace(data.sameChild).replace(data.child2).replace(data.leaf2);

        data.root.addAll(data.root2.getChildren());

        assertEquals(1, data.root.getChildCount());
        assertTrue(data.root.getChild(data.child).contains(data.leaf));
        assertSame(data.child, data.root.getChild(data.sameChild));
        assertSame(2, data.root.getChild(data.child).getChildCount());
        assertSame(2, data.root.getLeaves().size());
    }

    @Test
    void testJoinChildTree() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.child2);
        data.child.replace(data.leaf);
        data.root2.replace(data.sameChild2).replace(data.sameChild);

        data.root.joinChildTree(data.root2);

        assertFalse(data.sameChild2.isLeaf());
        assertTrue(data.child.contains(data.leaf));
        assertEquals(1, data.sameChild2.getChildCount());
        assertSame(data.sameChild2, data.child.getChild(data.child2));
    }

    @Test
    void testIntersectChildTree() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.child2);
        data.child.replace(data.leaf);
        data.root2.replace(data.sameChild2).replace(data.sameChild);

        data.root.intersectChildTree(data.root2);

        assertFalse(data.sameChild2.isLeaf());
        assertFalse(data.child.contains(data.leaf));
        assertEquals(1, data.sameChild2.getChildCount());
        assertSame(data.sameChild2, data.child.getChild(data.child2));
    }

    @Test
    void testMerge() {
        Data data = new Data();
        data.root.add(data.child).add(data.child2);
        data.child.add(data.leaf);

        data.root2.add(data.sameChild).addSibling(data.sameChild2).add(data.sameChild);

        data.root.merge(data.root2);

        assertEquals(5, data.root.size());
        assertTrue(data.root.contains(data.sameChild2));
        assertTrue(data.root.getChild(data.sameChild2).contains(data.sameChild));
    }

    @Test
    void testSubTree() {
        Data data = new Data();
        data.root.replace(data.child);
        data.root.replace(data.child2);
        data.root.replace(data.leaf);
        data.root.replace(data.leaf2);
        assertEquals(4, data.root.getChildCount());

        Node<String, NodeType> sub = data.root.subTree(1, 1);
        assertEquals(1, sub.getChildCount());

        sub = data.root.subTree(0, 0);
        assertEquals(0, sub.getChildCount());

        sub = data.root.subTree(1, 0);
        assertEquals(0, sub.getChildCount());

        sub = data.root.subTree(1, 2);
        assertEquals(2, sub.getChildCount());

        sub = data.root.subTree(3, 1);
        assertEquals(1, sub.getChildCount());

        sub = data.root.subTree(4, 1);
        assertEquals(0, sub.getChildCount());

        sub = data.root.subTree(-1, 10);
        assertEquals(4, sub.getChildCount());

        sub = data.root.subTree(data.child);
        assertEquals(4, sub.getChildCount());

        sub = data.root.subTree(data.child, data.leaf2);
        assertEquals(4, sub.getChildCount());

        sub = data.root.subTree(data.leaf2);
        assertEquals(1, sub.getChildCount());
    }

    @Test
    void testFind() {
        Data data = new Data();
        Node<String, NodeType> n1 = new Node<>("1_N", NodeType.N1);
        Node<String, NodeType> l1 = new Node<>("1_L", NodeType.LEAF);
        data.root.replace(data.child).replace(data.child2).replace(data.leaf);
        data.root.replace(n1).replace(l1).replace(new Node<>(data.leaf.getKey(), NodeType.N2));
        Node<String, NodeType> found = data.root.find(data.leaf.getKey());
        assertEquals(6, found.size());
        assertEquals(2, found.getLeaves().size());

        data.root.destroy();
        data.root.replace(data.child).replace(data.child2).replace(data.leaf);
        data.root.replace(n1).replace(l1).replace(new Node<>(data.leaf.getKey(), NodeType.N2));
        found = data.root.find(data.leaf.getKey(), filterN2);
        assertEquals(3, found.size());
        found = data.root.find(n1.getKey(), filterN1);
        assertEquals(3, found.size());
        found = data.root.find(n1.getKey(), filterN2);
        assertEquals(0, found.size());

        data.root.destroy();
        data.root.replace(data.child).replace(data.child2).replace(data.leaf).replace(new Node<>("matchA")).replaceSibling(new Node<>("Nomatch"));
        data.root.replace(n1).replace(l1).replace(data.leaf.cloneNode()).replace(new Node<>("matchB"));

        found = data.root.filteredTree(node -> node.getKey().startsWith("match"));
        assertEquals(2, found.getLeaves().size());
    }

    @Test
    void testBranch() {
        Data data = new Data();
        Node<String, NodeType> n1 = new Node<>("1_N");
        Node<String, NodeType> l1 = new Node<>("1_L");
        data.root.replace(data.child).replace(data.child2).replace(data.leaf);
        data.child.replace(n1).replace(l1);
        Node<String, NodeType> branch = data.leaf.getBranch();
        assertEquals(2, branch.size());
        assertEquals(data.child.getKey(), branch.getKey());
        branch = data.leaf.getBranch(data.child);
        assertEquals(1, branch.size());
        assertEquals(data.child2.getKey(), branch.getKey());
    }

    @Test
    void testCloneTree() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.child2).replace(data.leaf).replaceSibling(data.leaf2);
        Node<String, NodeType> clonedRoot = data.root.cloneTree();
        assertNotSame(data.root, clonedRoot);
        assertEquals(data.root.size(), clonedRoot.size());
        for (Node<String, NodeType> node : data.root.getChildren()) {
            assertNotSame(node, clonedRoot.getChild(node));
        }
    }

    @Test
    void testCloneTreeSubClasses() {
        Data data = new Data();
        Node<String, NodeType> sub1 = new Node<>("sub1", NodeType.N1);
        data.root.replace(sub1);
        Node<String, NodeType> clonedRoot = data.root.cloneTree();
        assertEquals(clonedRoot.getChild(sub1).getValue(), NodeType.N1);
        assertNotSame(sub1, clonedRoot.getChild(sub1));
    }

    @Test
    void testTraverse() {
        Data data = new Data();
        data.root.replace(data.child).replace(data.leaf);
        data.root.replace(data.child2).replace(data.leaf).replaceSibling(data.leaf2);

        final List<String> result = new ArrayList<>();

        data.root.traverseByDepthTopDown(node -> result.add(node.getKey()));
        assertEquals(6, result.size());
        assertNull(result.get(0));
        assertEquals(data.child.getKey(), result.get(1));
        assertEquals(data.leaf.getKey(), result.get(2));
        assertEquals(data.child2.getKey(), result.get(3));
        assertEquals(data.leaf.getKey(), result.get(4));
        assertEquals(data.leaf2.getKey(), result.get(5));

        result.clear();
        data.root.traverseByDepthBottomUp(node -> result.add(node.getKey()));
        assertEquals(6, result.size());
        assertEquals(data.leaf.getKey(), result.get(0));
        assertEquals(data.child.getKey(), result.get(1));
        assertEquals(data.leaf.getKey(), result.get(2));
        assertEquals(data.leaf2.getKey(), result.get(3));
        assertEquals(data.child2.getKey(), result.get(4));
        assertNull(result.get(5));

        result.clear();
        data.root.traverseByBreadthTopDown(node -> result.add(node.getKey()));
        assertEquals(6, result.size());
        assertNull(result.get(0));
        assertEquals(data.child.getKey(), result.get(1));
        assertEquals(data.child2.getKey(), result.get(2));
        assertEquals(data.leaf.getKey(), result.get(3));
        assertEquals(data.leaf.getKey(), result.get(4));
        assertEquals(data.leaf2.getKey(), result.get(5));

        result.clear();
        data.root.traverseByBreadthBottomUp(node -> result.add(node.getKey()));
        assertEquals(6, result.size());
        assertEquals(data.leaf.getKey(), result.get(0));
        assertEquals(data.leaf.getKey(), result.get(1));
        assertEquals(data.leaf2.getKey(), result.get(2));
        assertEquals(data.child.getKey(), result.get(3));
        assertEquals(data.child2.getKey(), result.get(4));
        assertNull(result.get(5));
    }

    @Test
    void testReplaceSibling() {
        Data data = new Data();
        Node<String, NodeType> ret = data.root.replace(data.child).replaceSibling(data.leaf);
        assertSame(data.leaf, ret);
        assertEquals(2, data.root.getChildCount());
        assertTrue(data.root.contains(data.child));
        assertTrue(data.root.contains(data.leaf));
        ret = data.child2.replaceSiblingTo(data.child);
        assertSame(data.child, ret);
        assertEquals(3, data.root.getChildCount());
        assertTrue(data.root.contains(data.child2));
    }

    @Test
    void testAddSibling() {
        Data data = new Data();
        Node<String, NodeType> ret = data.root.add(data.child).addSibling(data.sameChild).addSibling(data.child2).addSibling(data.leaf);
        assertSame(data.leaf, ret);
        assertEquals(3, data.root.getChildCount());
        assertTrue(data.root.contains(data.child));
        assertTrue(data.root.contains(data.leaf));
        assertTrue(data.root.contains(data.child2));
        ret = data.sameChild2.addSiblingTo(data.child);
        assertSame(data.child, ret);
        assertEquals(3, data.root.getChildCount());
        assertTrue(data.root.contains(data.child2));
    }

    @Test
    void testSubNode() {
        Data data = new Data();
        Node<String, NodeType> node1 = new Node<>("Node1", NodeType.N1);
        Node<String, NodeType> node2 = new Node<>("Node2", NodeType.N2);
        Node<String, NodeType> node2Empty = new Node<>(null, NodeType.N2);
        Node<String, NodeType> node3 = new Node<>("Node3", NodeType.N3);

        assertEquals(NodeType.N1, node1.getValue());

        data.root.add(node1).add(node2).add(node3);
        assertTrue(data.root.contains(node1));
        assertTrue(node1.contains(node2));
        assertTrue(node2.contains(node3));
        assertTrue(node3.isLeaf());
        assertSame(node1, node3.getAncestor(filterN1));
        assertSame(node1, node1.getAncestor(filterN1));

        data.root.destroy();
        data.root.add(node3.replaceTo(node2).replaceTo(node1));
        assertTrue(data.root.contains(node1));
        assertTrue(node1.contains(node2));
        assertTrue(node2.contains(node3));
        assertTrue(node3.isLeaf());

        data.root.destroy();
        data.root.add(node1).add(node2Empty).add(node3);
        assertTrue(data.root.contains(node1));
        assertTrue(node1.contains(node2Empty));
        assertTrue(node2Empty.contains(node3));
        assertTrue(node3.isLeaf());

        data.root.destroy();
        data.root.add(node1).addCollapseEmpty(node2Empty).add(node3);
        assertTrue(data.root.contains(node1));
        assertFalse(node1.contains(node2Empty));
        assertFalse(node2Empty.contains(node3));
        assertTrue(node1.contains(node3));
        assertTrue(node3.isLeaf());

        data.root.destroy();
        data.root.addSkipEmpty(node1).addSkipEmpty(node2Empty).addSkipEmpty(node3);
        assertTrue(data.root.contains(node1));
        assertFalse(node1.contains(node2Empty));
        assertFalse(node2Empty.contains(node3));
        assertFalse(node1.contains(node3));
        assertTrue(node1.isLeaf());
    }

    @Test
    void testComparator() {
        NodeComparator root = new NodeComparator(null);
        root.replace(new NodeComparator("A"));
        root.replace(new NodeComparator("B"));
        root.replace(new NodeComparator("C"));
        root.replace(new NodeComparator("Z"));
        assertEquals("Z", root.getFirstChild().getKey());
    }

    static class NodeComparator extends Node<String, NodeType> {

        NodeComparator(String data) {
            super(data, new TestComparator());
        }

        private static class TestComparator implements Comparator<String> {
            @Override
            public int compare(String s1, String s2) {
                return s2.compareTo(s1);
            }
        }
    }

}
