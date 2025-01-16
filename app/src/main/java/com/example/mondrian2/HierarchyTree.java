package com.example.mondrian2;
import joinery.DataFrame;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class HierarchyTree {
    private final String hierarchyType;
    private final Map<String, HierarchyTreeNode> nodeDict;
    private final HierarchyTreeNode root;
    private final Map<String, HierarchyTreeNode> leafIdDict;

    public HierarchyTree(String filePath) throws IOException {

        DataFrame<Object> df = DataFrame.readCsv(filePath);

        this.hierarchyType = new File(filePath).getName().split("_")[2];
        this.nodeDict = buildTree(df);
        this.root = this.nodeDict.get("#");
        this.leafIdDict = buildLeafIdDict();
        saveCoveredSubtreeNodes();
    }

    private Map<String, HierarchyTreeNode> buildTree(DataFrame<Object> df) {
        Map<String, HierarchyTreeNode> nodeDict = new HashMap<>();
        nodeDict.put("#", new HierarchyTreeNode("#", null, false, "0", 0));

        for (int rowNum = 0; rowNum < df.length(); rowNum++) {
            List<String> rowList = new ArrayList<>();
            for (int col = 0; col < df.size(); col++) {
                rowList.add(df.get(rowNum, col).toString());
            }
            Collections.reverse(rowList);

            for (int i = 0; i < rowList.size(); i++) {
                String value = rowList.get(i);
                boolean isLeaf = (i == rowList.size() - 2);

                if (i != rowList.size() - 1) {
                    if (!nodeDict.containsKey(value)) {
                        HierarchyTreeNode parent = nodeDict.get(rowList.get(i - 1));
                        HierarchyTreeNode newNode = new HierarchyTreeNode(value, parent, isLeaf, i + 1);
                        nodeDict.put(value, newNode);
                        parent.getChildren().add(newNode);
                    }
                } else {
                    nodeDict.get(rowList.get(i - 1)).setLeafId(value);
                }
            }
        }
        return nodeDict;
    }

    private Map<String, HierarchyTreeNode> buildLeafIdDict() {
        Map<String, HierarchyTreeNode> leafIdDict = new HashMap<>();
        for (HierarchyTreeNode node : nodeDict.values()) {
            if (node.isLeaf()) {
                leafIdDict.put(node.getLeafId(), node);
            }
        }
        return leafIdDict;
    }

    private void saveCoveredSubtreeNodes() {
        for (HierarchyTreeNode node : nodeDict.values()) {
            HierarchyTreeNode parent = node.getParent();
            while (parent != null) {
                parent.getCoveredSubtreeNodes().add(node);
                parent = parent.getParent();
            }
        }
    }

    public HierarchyTreeNode findCommonAncestor(String leaf1Id, String leaf2Id) {
        HierarchyTreeNode leaf1 = leafIdDict.get(leaf1Id);
        HierarchyTreeNode leaf2 = leafIdDict.get(leaf2Id);

        if (leaf1 == null || leaf2 == null) {
            return null;
        }

        Set<HierarchyTreeNode> ancestors = new HashSet<>();
        while (leaf1 != null) {
            ancestors.add(leaf1);
            leaf1 = leaf1.getParent();
        }

        while (leaf2 != null && !ancestors.contains(leaf2)) {
            leaf2 = leaf2.getParent();
        }

        return leaf2;
    }

    public Map<String, HierarchyTreeNode> getNodeDict() {
        return nodeDict;
    }

    public Map<String, HierarchyTreeNode> getLeafIdDict() {
        return leafIdDict;
    }
}