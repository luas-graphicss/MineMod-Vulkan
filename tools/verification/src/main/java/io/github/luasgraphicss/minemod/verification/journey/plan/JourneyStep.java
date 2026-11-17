package io.github.luasgraphicss.minemod.verification.journey.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public record JourneyStep(@JsonProperty("do") Kind kind, @JsonProperty("with") String argument) {
    public enum Kind {
        WAIT_FOR_TITLE("wait-for-title"),
        CREATE_WORLD("create-world"),
        OPEN_WORLD("open-world"),
        LEAVE_WORLD("leave-world"),
        COMMAND("command"),
        ATTEMPT("attempt"),
        EXPECT_DIMENSION("expect-dimension"),
        WAIT("wait"),
        SETTLE("settle"),
        SELECT_PACKS("select-packs"),
        RELOAD("reload"),
        HUD("hud"),
        SCREEN("screen"),
        CAPTURE("capture"),
        INSPECT("inspect"),
        QUIT("quit");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        @JsonValue
        public String label() {
            return label;
        }
    }

    public String description() {
        return argument.isEmpty() ? kind.label() : kind.label() + " " + argument;
    }
}
