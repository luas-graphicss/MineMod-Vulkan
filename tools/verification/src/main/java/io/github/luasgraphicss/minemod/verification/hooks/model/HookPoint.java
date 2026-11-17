package io.github.luasgraphicss.minemod.verification.hooks.model;

public record HookPoint(String id, String hook, String callee, String within, String named, int matches) {}
