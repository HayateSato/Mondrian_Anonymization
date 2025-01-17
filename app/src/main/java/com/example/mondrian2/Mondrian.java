package com.example.mondrian2;

import joinery.DataFrame;
import java.io.File;
import java.io.FileNotFoundException;
//import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.InputStream; // ADDED //////////////////////////////////////

public class Mondrian {

    private static DataFrame<Object> summarized(DataFrame<Object> partition, String dim, List<String> qiList) {
        for (int i = 0; i < qiList.size(); i++) {
            String qi = qiList.get(i);
            partition = partition.sortBy(qi);
            Object firstVal = partition.get(0, i);
            Object lastVal = partition.get(partition.length() - 1, i);

            if (!firstVal.equals(lastVal)) {
                String range = firstVal + "-" + lastVal;
                Object[] newColumn = new Object[partition.length()];
                Arrays.fill(newColumn, range);

                // Update the values in the existing column
                for (int k = 0; k < partition.length(); k++) {
                    partition.set(k, i, range);
                }
            }
        }
        return partition;
    }

    private static DataFrame<Object> anonymize(DataFrame<Object> partition,
                                               List<Map.Entry<String, Integer>> ranks, int k, List<String> qiList) {
        String dim = ranks.get(0).getKey();

        //partition = partition.sortBy(partition.col(dim));
        partition = partition.sortBy(dim);
        int size = partition.length();
        int mid = size / 2;

        DataFrame<Object> leftPartition = partition.slice(0, mid);
        DataFrame<Object> rightPartition = partition.slice(mid, size);

        if (leftPartition.length() >= k && rightPartition.length() >= k) {
            DataFrame<Object> leftAnonymized = anonymize(leftPartition, ranks, k, qiList);
            DataFrame<Object> rightAnonymized = anonymize(rightPartition, ranks, k, qiList);
            return leftAnonymized.concat(rightAnonymized);
        }
        return summarized(partition, dim, qiList);
    }


    public static DataFrame<Object> mondrian(DataFrame<Object> partition, List<String> qiList, int k) {
        Map<String, Integer> ranks = new HashMap<>();

        for (String qi : qiList) {
            // Get column values and count unique entries using a Set
            Set<Object> uniqueValues = new HashSet<>();
            List<Object> columnValues = partition.col(qi);
            uniqueValues.addAll(columnValues);
            ranks.put(qi, uniqueValues.size());
        }

        List<Map.Entry<String, Integer>> sortedRanks = ranks.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        return anonymize(partition, sortedRanks, k, qiList);
    }

    private static DataFrame<Object> mapTextToNum(DataFrame<Object> df, List<String> qiList,
                                                  Map<String, HierarchyTree> hierarchyTreeDict) {
        DataFrame<Object> result = new DataFrame<>();

        for (String column : qiList) {
            if (!df.columns().contains(column)) {
                throw new IllegalArgumentException("Column " + column + " not found in the table");
            }

            HierarchyTree hierarchyTree = hierarchyTreeDict.get(column);
            if (hierarchyTree == null) {
                throw new IllegalArgumentException("Hierarchy tree not found for column " + column);
            }

            Map<String, String> mapping = new HashMap<>();
            List<Object> col = df.col(column);

            if (col.isEmpty()) {
                continue;
            }

            hierarchyTree.getLeafIdDict().forEach((leafId, leaf) ->
                    mapping.put(leaf.getValue(), leafId));

            List newValues = new ArrayList();
            for (Object value : col) {
                newValues.add(mapping.getOrDefault(value.toString(), value.toString()));
            }
            result.add(column.trim(), newValues);
        }
        return result;
    }


    private static DataFrame<Object> mapNumToText(DataFrame<Object> df, List<String> qiList,
                                                  Map<String, HierarchyTree> hierarchyTreeDict) {
        DataFrame<Object> df3 = new DataFrame<>();
        for (String column : qiList) {
            HierarchyTree hierarchyTree = hierarchyTreeDict.get(column);
            List<Object> col = df.col(column);

            List newValues = new ArrayList();
            for (Object value : col) {
                String strValue = value.toString();
                if (strValue.matches("\\d+")) {
                    HierarchyTreeNode leaf = hierarchyTree.getLeafIdDict().get(strValue);
                    newValues.add(leaf.getValue());
                } else if (strValue.contains("-")) {
                    String[] parts = strValue.split("-");
                    HierarchyTreeNode commonAncestor =
                            hierarchyTree.findCommonAncestor(parts[0], parts[1]);
                    newValues.add(commonAncestor.getValue());
                } else {
                    newValues.add(strValue);
                }
            }


            df3 = df3.add(column.trim(), newValues);
        }
        return df3;
    }


    private static boolean checkKAnonymity(DataFrame<Object> df, List<String> qiList, int k) {
        // Use a map to store group frequencies based on quasi-identifier combinations
        Map<String, Integer> groupCounts = new HashMap<>();

        // Iterate over each row in the DataFrame
        for (int i = 0; i < df.length(); i++) {
            // Combine all quasi-identifier values into a unique group key (e.g., "value1|value2|value3")
            StringBuilder groupKeyBuilder = new StringBuilder();
            for (String qi : qiList) {
                groupKeyBuilder.append(df.get(i, qi)).append("|"); // Use "|" as delimiter
            }
            String groupKey = groupKeyBuilder.toString();

            // Update the group count for this group key
            groupCounts.merge(groupKey, 1, Integer::sum);
        }

        // Check if all group counts meet the k-anonymity requirement
        return groupCounts.values().stream().allMatch(count -> count >= k);
    }


    public static DataFrame<Object> runAnonymize(List<String> qiList, InputStream dataInputStream,
                                                 String hierarchyFileDir, int k) throws IOException {
        DataFrame<Object> df = DataFrame.readCsv(dataInputStream);

        Map<String, HierarchyTree> hierarchyTreeDict = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(hierarchyFileDir), "*.csv")) {
            for (Path path : stream) {
                String hierarchyType = path.getFileName().toString().split("_")[2].split("\\.")[0];
                hierarchyTreeDict.put(hierarchyType, new HierarchyTree(path.toString()));
            }
        }

        df = mapTextToNum(df, qiList, hierarchyTreeDict);
        df = mondrian(df, qiList, k);

        if (!checkKAnonymity(df, qiList, k)) {
            throw new RuntimeException("Not all partitions are k-anonymous");
        }

        df = mapNumToText(df, qiList, hierarchyTreeDict);
        return df;
    }

}
