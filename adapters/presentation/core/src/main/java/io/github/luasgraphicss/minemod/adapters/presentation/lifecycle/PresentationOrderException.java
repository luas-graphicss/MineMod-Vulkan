package io.github.luasgraphicss.minemod.adapters.presentation.lifecycle;

public final class PresentationOrderException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    PresentationOrderException(String step, PresentationPhase phase) {
        super("the game asked to " + step + " while the main window was " + phase);
    }
}
