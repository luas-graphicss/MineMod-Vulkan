package io.github.luasgraphicss.minemod.compatibility.provider.identity;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.GlVersion;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProviderIdentityParser {
    private static final String ZINK_PREFIX = "zink Vulkan ";
    private static final String CORE_PROFILE = "Core";
    private static final List<String> SOFTWARE_RASTERIZERS = Arrays.asList("llvmpipe", "softpipe", "SWR");
    private static final List<String> SOFTWARE_VULKAN_DRIVERS = Arrays.asList("MESA_LLVMPIPE", "GOOGLE_SWIFTSHADER");
    private static final Pattern VERSION = Pattern.compile(
            "^(\\d+)\\.(\\d+)(?:\\.\\d+)?(?: \\((Core|Compatibility) Profile\\))?(?: Mesa (\\S+))?.*$");

    private ProviderIdentityParser() {
    }

    public static ProviderIdentity parse(ProviderStrings strings) {
        Matcher version = VERSION.matcher(strings.version());
        if (!version.matches()) {
            throw new IllegalArgumentException("unreadable OpenGL version string: " + strings.version());
        }
        GlVersion glVersion = GlVersion.of(Integer.parseInt(version.group(1)), Integer.parseInt(version.group(2)));
        ContextProfile profile =
                CORE_PROFILE.equals(version.group(3)) ? ContextProfile.CORE : ContextProfile.COMPATIBILITY;
        Optional<String> mesaVersion = Optional.ofNullable(version.group(4));
        String renderer = strings.renderer();
        if (renderer.startsWith(ZINK_PREFIX)) {
            return zinkIdentity(renderer, glVersion, profile, mesaVersion);
        }
        boolean software = isSoftwareRasterizer(renderer);
        DriverKind kind = software ? DriverKind.SOFTWARE_RASTERIZER : DriverKind.FOREIGN;
        return new ProviderIdentity(kind, glVersion, profile, mesaVersion, renderer, software);
    }

    private static ProviderIdentity zinkIdentity(
            String renderer, GlVersion glVersion, ContextProfile profile, Optional<String> mesaVersion) {
        int open = renderer.indexOf('(');
        int close = renderer.lastIndexOf(')');
        if (open < 0 || close < open) {
            throw new IllegalArgumentException("unreadable Zink renderer string: " + renderer);
        }
        String device = renderer.substring(open + 1, close);
        String vulkanDriver = "";
        int driverOpen = device.lastIndexOf('(');
        if (driverOpen >= 0 && device.endsWith(")")) {
            vulkanDriver = device.substring(driverOpen + 1, device.length() - 1);
            device = device.substring(0, driverOpen).trim();
        }
        boolean software = SOFTWARE_VULKAN_DRIVERS.contains(vulkanDriver);
        return new ProviderIdentity(DriverKind.ZINK, glVersion, profile, mesaVersion, device, software);
    }

    private static boolean isSoftwareRasterizer(String renderer) {
        for (String rasterizer : SOFTWARE_RASTERIZERS) {
            if (renderer.startsWith(rasterizer)) {
                return true;
            }
        }
        return false;
    }
}
