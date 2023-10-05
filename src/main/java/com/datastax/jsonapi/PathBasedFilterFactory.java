package com.datastax.jsonapi;

import com.fasterxml.jackson.core.filter.TokenFilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PathBasedFilterFactory {
    private final static Pattern COMMA_SEPARATOR = Pattern.compile(",");

    private final static Pattern DOT_SEPARATOR = Pattern.compile("\\.");

    public static TokenFilter filterForPaths(String csPaths) {
        csPaths = csPaths.trim();
        // Empty String -> match nothing; caller needs to check
        if (csPaths.isEmpty()) {
            return null;
        }
        return filterForPaths(Arrays.asList(COMMA_SEPARATOR.split(csPaths)));
    }

    public static TokenFilter filterForPaths(List<String> csPaths) {
        // First: build a minimal inclusion tree, needed for construction of filters
        StringNode roots = buildInclusionTree(csPaths);
        if (roots == null) {
            return null;
        }
        return buildFilterFromInclusionTree(roots);
    }

    private static StringNode buildInclusionTree(List<String> paths)
    {
        StringNode root = new StringNode();
        for (String path : paths) {
            path = path.trim();
            if (path.isEmpty()) {
                continue;
            }
            StringNode curr = root;
            for (String segment : DOT_SEPARATOR.split(path)) {
                StringNode next = curr.find(segment);
                // If next doesn't exist, create it
                if (next == null) {
                    curr = curr.add(segment);
                } else if (next.isEmpty()) {
                    // If next exists and is leaf, we're done (current path longer than existing match)
                    break;
                } else {
                    // Otherwise, keep going
                    curr = next;
                }
            }
            // Make sure end of the path is marked as leaf
            curr.clear();
        }
        // Could still be empty if all paths were empty
        if (root.isEmpty()) {
            return null;
        }
        return root;
    }

    /**
     * Method for building a {@link TokenFilter} from a given inclusion tree.
     *
     * @param node Current tree node to build filter for
     *
     * @return Filter for the given node
     */
    private static TokenFilter buildFilterFromInclusionTree(StringNode node) {
        if (node.isEmpty()) {
            return TokenFilter.INCLUDE_ALL;
        }
        Map<String, StringNode> childNodes = node.getChildren();
        // Optimize single-path case
        if (childNodes.size() == 1) {
            Map.Entry<String, StringNode> entry = childNodes.entrySet().iterator().next();
            return new PathBasedFilter.SinglePathFilter(entry.getKey(),
                    buildFilterFromInclusionTree(entry.getValue()));
        }
        Map<String, TokenFilter> filters = new HashMap<>();
        for (Map.Entry<String, StringNode> entry : childNodes.entrySet()) {
            filters.put(entry.getKey(), buildFilterFromInclusionTree(entry.getValue()));
        }
        return new PathBasedFilter.MultiPathFilter(filters);
    }

    /**
     * Helper type used for creating "minimal" set of inclusion paths starting
     * from a root node. Order of creating paths doesn't matter; longer paths
     * will be pruned regardless of ordering.
     */
    static class StringNode {
        private Map<String, StringNode> next;

        public StringNode() { }

        public boolean isEmpty() {
            return next == null;
        }

        public Map<String, StringNode> getChildren() {
            return next;
        }

        public StringNode find(String key) {
            return (next == null) ? null : next.get(key);
        }

        public StringNode add(String key) {
            if (next == null) {
                next = new HashMap<>();
            }
            StringNode node = new StringNode();
            next.put(key, node);
            return node;
        }

        public void clear() {
            next = null;
        }
    }
}
