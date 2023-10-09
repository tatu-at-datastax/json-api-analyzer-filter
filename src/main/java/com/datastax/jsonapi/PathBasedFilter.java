package com.datastax.jsonapi;

import com.fasterxml.jackson.core.filter.TokenFilter;

import java.util.Map;

/**
 * Base class for {@link TokenFilter} implementations used for filtering
 * intermediate tree levels (for leaves we use "include all" filter).
 * Needs to match path going through, and exclude possible scalar values
 * (so that "a.x.y" will NOT match "a.x", but will match "a.x.y.z", for example).
 */
class PathBasedFilter extends TokenFilter {
    /**
     * Different from default implementation as we should NOT allow
     * scalar values to be included at intermediate (branch) level.
     */
    @Override
    protected boolean _includeScalar() {
        return false;
    }


    /**
     * Specialized implementation that matches just a single path through JSON Object.
     */
    static class SinglePathFilter extends PathBasedFilter {
        private final String matchedSegment;

        private final TokenFilter nextFilter;

        public SinglePathFilter(String matchedSegment, TokenFilter nextFilter) {
            this.matchedSegment = matchedSegment;
            this.nextFilter = nextFilter;
        }

        @Override
        public TokenFilter includeProperty(String property) {
            if (property.equals(matchedSegment)) {
                return nextFilter;
            }
            return null;
        }
    }

    /**
     * General implementation that matches multiple paths through JSON Object.
     */
    static class MultiPathFilter extends PathBasedFilter {
        private final Map<String, TokenFilter> nextFilters;

        public MultiPathFilter(Map<String, TokenFilter> nextFilters) {
            this.nextFilters = nextFilters;
        }

        @Override
        public TokenFilter includeProperty(String property) {
            return nextFilters.get(property);
        }
    }
}
