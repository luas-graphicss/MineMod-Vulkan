package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.draw;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.draw.ShaderInstanceAccessor;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.ShaderInstance;

final class ShaderTraits {
    private static final float CUTOUT = 0.1f;
    private static final float INVISIBLE = 0.001f;
    private static final float KEEP_ALL = 0.0f;
    private static final String TEXTURE_SAMPLER = "Sampler0";
    private static final String OVERLAY_SAMPLER = "Sampler1";
    private static final String LIGHTMAP_SAMPLER = "Sampler2";
    private static final Set<String> CUTS_FAINT_TEXELS = new HashSet<>(Arrays.asList(
            "particle",
            "position_color_tex_lightmap",
            "position_tex_color",
            "rendertype_armor_cutout_no_cull",
            "rendertype_armor_entity_glint",
            "rendertype_breeze_wind",
            "rendertype_clouds",
            "rendertype_crumbling",
            "rendertype_energy_swirl",
            "rendertype_entity_cutout",
            "rendertype_entity_cutout_no_cull",
            "rendertype_entity_cutout_no_cull_z_offset",
            "rendertype_entity_decal",
            "rendertype_entity_glint",
            "rendertype_entity_glint_direct",
            "rendertype_entity_smooth_cutout",
            "rendertype_entity_translucent",
            "rendertype_entity_translucent_cull",
            "rendertype_entity_translucent_emissive",
            "rendertype_glint",
            "rendertype_glint_translucent",
            "rendertype_item_entity_translucent_cull",
            "rendertype_text",
            "rendertype_text_background",
            "rendertype_text_background_see_through",
            "rendertype_text_intensity",
            "rendertype_text_intensity_see_through",
            "rendertype_text_see_through",
            "rendertype_tripwire"));
    private static final Set<String> CUTS_INVISIBLE_TEXELS = new HashSet<>(Arrays.asList(
            "position_color",
            "position_tex",
            "rendertype_gui",
            "rendertype_gui_ghost_recipe_overlay",
            "rendertype_gui_overlay",
            "rendertype_gui_text_highlight",
            "rendertype_outline"));

    private ShaderTraits() {}

    static Set<Shading> shadingOf(ShaderInstance shader) {
        List<String> samplers = ((ShaderInstanceAccessor) shader).minemod$samplerNames();
        Set<Shading> steps = EnumSet.noneOf(Shading.class);
        if (samplers.contains(TEXTURE_SAMPLER)) {
            steps.add(Shading.TEXTURE);
        }
        if (samplers.contains(LIGHTMAP_SAMPLER)) {
            steps.add(Shading.LIGHTMAP);
        }
        if (samplers.contains(OVERLAY_SAMPLER)) {
            steps.add(Shading.OVERLAY);
        }
        if (shader.LIGHT0_DIRECTION != null) {
            steps.add(Shading.NORMAL);
        }
        if (shader.FOG_START != null) {
            steps.add(Shading.FOG);
        }
        if (shader.LINE_WIDTH != null) {
            steps.add(Shading.WIDE_LINES);
        }
        return steps;
    }

    static boolean samplesATexture(ShaderInstance shader) {
        return ((ShaderInstanceAccessor) shader).minemod$samplerNames().contains(TEXTURE_SAMPLER);
    }

    static float alphaCutoffOf(ShaderInstance shader) {
        if (CUTS_FAINT_TEXELS.contains(shader.getName())) {
            return CUTOUT;
        }
        return CUTS_INVISIBLE_TEXELS.contains(shader.getName()) ? INVISIBLE : KEEP_ALL;
    }
}
