package io.github.luasgraphicss.minemod.journey.plan;

import com.google.gson.annotations.SerializedName;

public record PlanStep(@SerializedName("do") String kind, @SerializedName("with") String argument) {}
