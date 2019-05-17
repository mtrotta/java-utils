package org.matteo.utils.collection;

import org.matteo.utils.util.NullSafeComparator;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 12/12/12
 */
public class Node<K extends Comparable<K>, V> {

    private final Comparator<K> comparator;
    private final K key;

    private V value;

    private Node<K, V> parent;
    private TreeMap<K, Node<K, V>> children;

    public Node() {
        this(new NullSafeComparator<>());
    }

    public Node(Comparator<K> comparator) {
        this(null, null, comparator);
    }

    public Node(K key) {
        this(key, null, new NullSafeComparator<>());
    }

    public Node(K key, V value) {
        this(key, value, new NullSafeComparator<>());
    }

    public Node(K key, Comparator<K> comparator) {
        this(key, null, comparator);
    }

    public Node(K key, V value, Comparator<K> comparator) {
        this.comparator = comparator;
        this.children = new TreeMap<>(comparator);
        this.key = key;
        this.value = value;
    }

    public Node(Node<K, V> node) {
        this(node.key, node.value, node.comparator);
    }

    private void putNode(Node<K, V> child) {
        children.put(child.getKey(), child);
    }

    private void putNodes(Collection<Node<K, V>> nodes) {
        for (Node<K, V> child : nodes) {
            children.put(child.key, child);
        }
    }

    private Node<K, V> put(Node<K, V> child) {
        child.setParent(this);
        putNode(child);
        return child;
    }

    private Node<K, V> addNode(Node<K, V> child) {
        Node<K, V> node = getChild(child);
        if (node == null) {
            node = put(child);
        }
        return node;
    }

    public Node<K, V> add(Node<K, V> child) {
        Node<K, V> node = addNode(child);
        for (Node<K, V> c : child.getChildren()) {
            node.add(c);
        }
        return node;
    }

    public Node<K, V> addCollapseEmpty(Node<K, V> child) {
        Node<K, V> node = !child.isEmpty() ? addNode(child) : this;
        for (Node<K, V> c : child.getChildren()) {
            node.addCollapseEmpty(c);
        }
        return node;
    }

    public Node<K, V> addSkipEmpty(Node<K, V> child) {
        if (!child.isEmpty()) {
            Node<K, V> node = addNode(child);
            for (Node<K, V> c : child.getChildren()) {
                node.addSkipEmpty(c);
            }
            return node;
        } else {
            return new Node<>();
        }
    }

    public Node<K, V> addTo(Node<K, V> parent) {
        parent.add(this);
        return parent;
    }

    public Node<K, V> addToCollapseEmpty(Node<K, V> parent) {
        parent.addCollapseEmpty(this);
        return parent;
    }

    public Node<K, V> addToSkipEmpty(Node<K, V> parent) {
        parent.addSkipEmpty(this);
        return parent;
    }

    public Node<K, V> addAll(Collection<Node<K, V>> children) {
        for (Node<K, V> node : children) {
            Node<K, V> child = getChild(node);
            if (child != null) {
                child.addAll(node.getChildren());
            } else {
                put(node);
            }
        }
        return this;
    }

    public Node<K, V> addSibling(Node<K, V> node) {
        return parent.add(node);
    }

    public Node<K, V> addSiblingTo(Node<K, V> node) {
        addTo(node.parent);
        return node;
    }

    public Node<K, V> replace(Node<K, V> child) {
        remove(child);
        return put(child);
    }

    public Node<K, V> replaceCollapseEmpty(Node<K, V> child) {
        Node<K, V> node = !child.isEmpty() ? replace(child) : remove(child);
        for (Node<K, V> c : child.getChildren()) {
            node.replaceCollapseEmpty(c);
        }
        return node;
    }

    public Node<K, V> replaceSkipEmpty(Node<K, V> child) {
        if (!child.isEmpty()) {
            Node<K, V> node = replace(child);
            for (Node<K, V> c : child.getChildren()) {
                node.replaceSkipEmpty(c);
            }
            return node;
        } else {
            return new Node<>();
        }
    }

    public Node<K, V> replaceTo(Node<K, V> parent) {
        parent.replace(this);
        return parent;
    }

    public Node<K, V> replaceToCollapseEmpty(Node<K, V> parent) {
        parent.replaceCollapseEmpty(this);
        return parent;
    }

    public Node<K, V> replaceToSkipEmpty(Node<K, V> parent) {
        parent.replaceSkipEmpty(this);
        return parent;
    }

    public Node<K, V> replaceAll(Collection<Node<K, V>> children) {
        for (Node<K, V> child : children) {
            replace(child);
        }
        return this;
    }

    public Node<K, V> replaceSibling(Node<K, V> node) {
        return parent.replace(node);
    }

    public Node<K, V> replaceSiblingTo(Node<K, V> node) {
        replaceTo(node.parent);
        return node;
    }

    public Node<K, V> remove(K key) {
        Node<K, V> child = getChild(key);
        if (child != null) {
            child.setParent(null);
            children.remove(key);
        }
        return this;
    }

    public Node<K, V> remove(Node<K, V> child) {
        return remove(child.key);
    }

    public void removeBranch() {
        if (children.isEmpty() && parent != null) {
            Node<K, V> node = parent;
            node.remove(this);
            node.removeBranch();
        }
    }

    public Node<K, V> removeAll(Collection<Node<K, V>> children) {
        for (Node<K, V> child : children) {
            remove(child);
        }
        return this;
    }

    public Node<K, V> joinChildTree(Node<K, V> childRoot) {
        for (Node<K, V> node : getLeaves()) {
            Node<K, V> child = childRoot.getChild(node);
            if (child != null) {
                node.parent.replace(child);
            }
        }
        return this;
    }

    public Node<K, V> intersectChildTree(Node<K, V> childRoot) {
        for (Node<K, V> leaf : getLeaves()) {
            Node<K, V> child = childRoot.getChild(leaf);
            if (child != null) {
                leaf.parent.replace(child);
            } else {
                leaf.removeBranch();
            }
        }
        return this;
    }

    public Node<K, V> merge(Node<K, V> root) {
        for (Node<K, V> otherChild : root.getChildren()) {
            Node<K, V> child = getChild(otherChild);
            if (child != null) {
                child.merge(otherChild);
            } else {
                add(otherChild);
            }
        }
        return this;
    }

    public Node<K, V> subTree(Node<K, V> from) {
        Node<K, V> root = cloneNode();
        root.children.putAll(children.tailMap(from.key));
        return root;
    }

    public Node<K, V> subTree(Node<K, V> from, Node<K, V> to) {
        Node<K, V> root = cloneNode();
        root.children.putAll(children.subMap(from.key, true, to.key, true));
        return root;
    }

    public Node<K, V> cloneNode() {
        return new Node<>(this);
    }

    public Node<K, V> subTree(int from, int size) {
        Node<K, V> root = cloneNode();
        List<Node<K, V>> list = getChildrenList();
        if (from < 0) {
            from = 0;
        }
        int to = from + size;
        if (to > list.size()) {
            to = list.size();
        }
        root.putNodes(list.subList(from, to));
        return root;
    }

    public Node<K, V> filteredTree(Predicate<Node<K, V>> predicate) {
        return filter(this, this, cloneNode(), predicate);
    }

    private Node<K, V> filter(Node<K, V> ancestor, Node<K, V> current, Node<K, V> root, Predicate<Node<K, V>> predicate) {
        for (Node<K, V> node : current.getChildren()) {
            if (predicate.test(node)) {
                root.add(node.getBranch(ancestor));
            }
        }
        for (Node<K, V> child : current.getChildren()) {
            child.filter(ancestor, child, root, predicate);
        }
        return root;
    }

    public Node<K, V> find(K key) {
        return find(key, this, cloneNode(), tNode -> true);
    }

    public Node<K, V> find(K key, Predicate<Node<K, V>> predicate) {
        return find(key, this, cloneNode(), predicate);
    }

    private Node<K, V> find(K key, Node<K, V> current, Node<K, V> root, Predicate<Node<K, V>> predicate) {
        Node<K, V> found = current.getChild(key);
        if (found != null && predicate.test(found)) {
            root.add(found.getBranch());
        }
        for (Node<K, V> child : current.getChildren()) {
            child.find(key, child, root, predicate);
        }
        return root;
    }

    public Node<K, V> cloneTree() {
        Node<K, V> root = cloneNode();
        for (Node<K, V> child : children.values()) {
            root.put(child.cloneTree());
        }
        return root;
    }

    public void traverseByDepthTopDown(Consumer<Node<K, V>> consumer) {
        consumer.accept(this);
        for (Node<K, V> child : children.values()) {
            child.traverseByDepthTopDown(consumer);
        }
    }

    public void traverseByDepthBottomUp(Consumer<Node<K, V>> consumer) {
        for (Node<K, V> child : children.values()) {
            child.traverseByDepthBottomUp(consumer);
        }
        consumer.accept(this);
    }

    public void traverseByBreadthTopDown(Consumer<Node<K, V>> consumer) {
        consumer.accept(this);
        topDown(children.values(), consumer);
    }

    private void topDown(Collection<Node<K, V>> children, Consumer<Node<K, V>> consumer) {
        for (Node<K, V> child : children) {
            consumer.accept(child);
        }
        for (Node<K, V> child : children) {
            topDown(child.getChildren(), consumer);
        }
    }

    public void traverseByBreadthBottomUp(Consumer<Node<K, V>> consumer) {
        bottomUp(children.values(), consumer);
        consumer.accept(this);
    }

    private void bottomUp(Collection<Node<K, V>> children, Consumer<Node<K, V>> consumer) {
        for (Node<K, V> child : children) {
            bottomUp(child.getChildren(), consumer);
        }
        for (Node<K, V> child : children) {
            consumer.accept(child);
        }
    }

    public boolean isEmpty() {
        return key == null;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public Node<K, V> getRoot() {
        Node<K, V> root = parent;
        if (root != null) {
            while (!root.isRoot()) {
                root = root.parent;
            }
        }
        return root;
    }

    public Node<K, V> getBranch() {
        return getBranch(getRoot());
    }

    public Node<K, V> getBranch(Node<K, V> ancestor) {
        Node<K, V> root = cloneNode();
        root.children = children;
        Node<K, V> node = parent;
        while (node != null && !node.equals(ancestor)) {
            root = root.replaceTo(node.cloneNode());
            node = node.parent;
        }
        return root;
    }

    public boolean contains(Node<K, V> child) {
        return children.containsKey(child.key);
    }

    public boolean containsKey(K key) {
        return children.containsKey(key);
    }

    public Node<K, V> getChild(K key) {
        return children.get(key);
    }

    public Node<K, V> getChild(Node<K, V> child) {
        return children.get(child.key);
    }

    public Node<K, V> getFirstChild() {
        return !children.isEmpty() ? children.firstEntry().getValue() : null;
    }

    public Collection<Node<K, V>> getChildren() {
        return children.values();
    }

    public List<K> getChildrenKeyList() {
        return new ArrayList<>(children.keySet());
    }

    public List<Node<K, V>> getChildrenList() {
        return new ArrayList<>(children.values());
    }

    public List<Node<K, V>> getLeaves() {
        return getLeaves(new ArrayList<>());
    }

    private List<Node<K, V>> getLeaves(List<Node<K, V>> leaves) {
        if (isLeaf()) {
            leaves.add(this);
        } else {
            for (Node<K, V> child : children.values()) {
                child.getLeaves(leaves);
            }
        }
        return leaves;
    }

    public Map<K, Node<K, V>> getLeafMap() {
        return getLeafMap(new TreeMap<>());
    }

    private Map<K, Node<K, V>> getLeafMap(Map<K, Node<K, V>> leafMap) {
        if (isLeaf()) {
            leafMap.put(key, this);
        } else {
            for (Node<K, V> child : children.values()) {
                child.getLeafMap(leafMap);
            }
        }
        return leafMap;
    }

    public Node<K, V> getParent() {
        return parent;
    }

    public void setParent(Node<K, V> parent) {
        this.parent = parent;
    }

    public Node<K, V> getAncestor(Predicate<Node<K, V>> filter) {
        Node<K, V> node = this;
        while (node != null) {
            if (filter.test(node)) {
                break;
            }
            node = node.getParent();
        }
        return node;
    }

    public int getChildCount() {
        return children.size();
    }

    public int size() {
        int size = getChildCount();
        for (Node<K, V> child : children.values()) {
            size += child.size();
        }
        return size;
    }

    public int getPosition() {
        return parent != null ? Collections.binarySearch(parent.getChildrenKeyList(), key) : 0;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void clear() {
        parent = null;
        children.clear();
    }

    public void clearBranch() {
        for (Node<K, V> child : children.values()) {
            child.destroy();
        }
    }

    public void destroy() {
        clearBranch();
        clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node<?, ?> node = (Node<?, ?>) o;
        return Objects.equals(key, node.key) &&
                Objects.equals(parent, node.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, parent);
    }

    @Override
    public String toString() {
        return key + (children.isEmpty() ? "" : " -> " + children.values());
    }

}
