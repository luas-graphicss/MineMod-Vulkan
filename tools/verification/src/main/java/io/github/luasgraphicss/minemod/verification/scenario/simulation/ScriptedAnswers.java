package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

final class ScriptedAnswers {
    private static final long NO_ANSWER = 0;

    private final Map<String, Deque<Long>> answersByFunction = new HashMap<>();

    void script(String function, long... answers) {
        Deque<Long> queued = answersByFunction.computeIfAbsent(function, unused -> new ArrayDeque<>());
        for (long answer : answers) {
            queued.add(answer);
        }
    }

    long next(String function) {
        Deque<Long> queued = answersByFunction.get(function);
        return queued == null || queued.isEmpty() ? NO_ANSWER : queued.poll();
    }
}
