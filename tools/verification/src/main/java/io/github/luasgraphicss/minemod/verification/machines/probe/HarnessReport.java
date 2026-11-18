package io.github.luasgraphicss.minemod.verification.machines.probe;

import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.DeviceType;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.VulkanDevice;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HarnessReport(VulkanDevice device, int presentedFrames, OptionalInt validationMessages) {
    private static final Pattern DEVICE = Pattern.compile("device: (.+) \\(([^()]+)\\)");
    private static final Pattern DRIVER = Pattern.compile("driver: (.+)");
    private static final Pattern VULKAN = Pattern.compile("vulkan: (.+)");
    private static final Pattern PRESENTATION = Pattern.compile("presentation: ([0-9]{1,9}) frame\\(s\\) .*");
    private static final Pattern VALIDATION = Pattern.compile("validation: ([0-9]{1,9}) message\\(s\\)");

    private static final Map<String, DeviceType> DEVICE_TYPES = Map.of(
            "CPU", DeviceType.CPU,
            "integrated GPU", DeviceType.INTEGRATED_GPU,
            "discrete GPU", DeviceType.DISCRETE_GPU,
            "virtual GPU", DeviceType.VIRTUAL_GPU,
            "other device", DeviceType.OTHER);

    public static HarnessReport parse(String output) throws IOException {
        List<String> lines = output.lines().toList();
        Matcher device = required(lines, DEVICE, "device");
        DeviceType type = DEVICE_TYPES.get(device.group(2));
        if (type == null) {
            throw new IOException("the harness reported a device type this tool does not know: " + device.group(2));
        }
        String driver = required(lines, DRIVER, "driver").group(1);
        String vulkanApi = required(lines, VULKAN, "vulkan").group(1);
        int presentedFrames = Integer.parseInt(required(lines, PRESENTATION, "presentation").group(1));
        OptionalInt validationMessages = first(lines, VALIDATION)
                .map(counted -> OptionalInt.of(Integer.parseInt(counted.group(1))))
                .orElseGet(OptionalInt::empty);
        return new HarnessReport(
                new VulkanDevice(device.group(1), type, driver, vulkanApi), presentedFrames, validationMessages);
    }

    public boolean presentedWithoutValidationMessages() {
        return presentedFrames > 0 && validationMessages.isPresent() && validationMessages.getAsInt() == 0;
    }

    private static Matcher required(List<String> lines, Pattern pattern, String field) throws IOException {
        return first(lines, pattern)
                .orElseThrow(() -> new IOException("the harness did not report its " + field + " line"));
    }

    private static Optional<Matcher> first(List<String> lines, Pattern pattern) {
        return lines.stream().map(pattern::matcher).filter(Matcher::matches).findFirst();
    }
}
