package io.github.luasgraphicss.minemod.journey.inspection;

import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;

final class RendererDetails {
    private static final String TITLE = "Journey inspection";
    private static final String CATEGORY = "-- MineMod Vulkan --";
    private static final String NEXT_CATEGORY = "\n-- ";

    private RendererDetails() {}

    static String reportedOrNull(Minecraft minecraft) {
        CrashReport report = minecraft.fillReport(new CrashReport(TITLE, new Throwable(TITLE)));
        StringBuilder details = new StringBuilder();
        report.getDetails(details);
        int start = details.indexOf(CATEGORY);
        if (start < 0) {
            return null;
        }
        int end = details.indexOf(NEXT_CATEGORY, start + CATEGORY.length());
        return details.substring(start, end < 0 ? details.length() : end).strip();
    }
}
