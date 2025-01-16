package com.example.mondrian2;

import java.util.*;

public class HierarchyTreeNode {
    private String value;
    private int level;
    private boolean isLeaf;
    private String leafId;
    private HierarchyTreeNode parent;
    private List<HierarchyTreeNode> children;
    private Set<HierarchyTreeNode> coveredSubtreeNodes;

    /**
     * Constructs a new HierarchyTreeNode with the specified parameters.
     *
     * @param value the value of the node
     * @param parent the parent node of this node
     * @param isLeaf a boolean indicating if this node is a leaf
     * @param leafId the identifier for the leaf node
     * @param level the level of this node in the hierarchy
     */
    public HierarchyTreeNode(String value, HierarchyTreeNode parent, boolean isLeaf, String leafId, int level) {
        this.value = value;
        this.level = level;
        this.isLeaf = isLeaf;
        this.leafId = leafId;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.coveredSubtreeNodes = new HashSet<>();
    }

    // Overloaded constructor without the leafId parameter
    public HierarchyTreeNode(String value, HierarchyTreeNode parent, boolean isLeaf, int level) {
        this(value, parent, isLeaf, "0", level); // Default leafId is "0"
    }

    // Getters and setters
    public String getValue() { return value; }
    public int getLevel() { return level; }
    public boolean isLeaf() { return isLeaf; }
    public String getLeafId() { return leafId; }
    public void setLeafId(String leafId) { this.leafId = leafId; }
    public HierarchyTreeNode getParent() { return parent; }
    public List<HierarchyTreeNode> getChildren() { return children; }
    public Set<HierarchyTreeNode> getCoveredSubtreeNodes() { return coveredSubtreeNodes; }
}
