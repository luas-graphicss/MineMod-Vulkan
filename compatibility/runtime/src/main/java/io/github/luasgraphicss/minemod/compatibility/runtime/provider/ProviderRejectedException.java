package io.github.luasgraphicss.minemod.compatibility.runtime.provider;

import io.github.luasgraphicss.minemod.compatibility.provider.diagnosis.ProviderDiagnosis;

public final class ProviderRejectedException extends Exception {
    private static final long serialVersionUID = 1L;
    private final transient ProviderDiagnosis diagnosis;

    public ProviderRejectedException(ProviderDiagnosis diagnosis) {
        super("the OpenGL provider was rejected: " + String.join("; ", diagnosis.rejections().values()));
        this.diagnosis = diagnosis;
    }

    public ProviderDiagnosis diagnosis() {
        return diagnosis;
    }
}
