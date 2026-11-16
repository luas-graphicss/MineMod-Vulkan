package io.github.luasgraphicss.minemod.compatibility.layer.route;

import io.github.luasgraphicss.minemod.compatibility.translation.query.QueryCalls;

final class SpanRoutes {
    private final QueryCalls queries;

    SpanRoutes(QueryCalls queries) {
        this.queries = queries;
    }

    long call(String coreFunction, String function, long[] words) {
        switch (coreFunction) {
            case "glBeginQuery":
                queries.begin(function, (int) words[0], (int) words[1]);
                return 0;
            case "glEndQuery":
                queries.end(function, (int) words[0]);
                return 0;
            case "glBeginTransformFeedback":
                queries.beginTransformFeedback(function, (int) words[0]);
                return 0;
            case "glEndTransformFeedback":
                queries.endTransformFeedback(function);
                return 0;
            default:
                return queries.result(function, words);
        }
    }
}
