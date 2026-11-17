package io.github.luasgraphicss.minemod.verification.machines.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public record ReferenceMachine(String id, HostSystem system, VulkanDevice device, List<Evidence> evidence) {

    public enum SystemFamily {
        LINUX("linux"),
        WINDOWS("windows");

        private final String label;

        SystemFamily(String label) {
            this.label = label;
        }

        @JsonValue
        public String label() {
            return label;
        }
    }

    public enum DeviceType {
        CPU("cpu"),
        INTEGRATED_GPU("integrated-gpu"),
        DISCRETE_GPU("discrete-gpu"),
        VIRTUAL_GPU("virtual-gpu"),
        OTHER("other");

        private final String label;

        DeviceType(String label) {
            this.label = label;
        }

        @JsonValue
        public String label() {
            return label;
        }
    }

    public enum Evidence {
        NATIVE_PRESENTATION("native-presentation"),
        WINDOW_PRESENTATION("window-presentation"),
        SHARED_IMAGE_INTEROP("shared-image-interop");

        private final String label;

        Evidence(String label) {
            this.label = label;
        }

        @JsonValue
        public String label() {
            return label;
        }
    }

    public record HostSystem(SystemFamily family, String description) {}

    public record VulkanDevice(String name, DeviceType type, String driver, String vulkanApi) {}
}
