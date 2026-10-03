package com.quietdome;

import java.util.List;

/** 七个别名。顺序别动——索引是按 hash % size 算的。 */
public final class AliasRegistry {
    private AliasRegistry() {}

    public static final List<Alias> ALL = List.of(
            new Alias("Dome UI",      "dome_ui",      "穹顶 UI",  "Most direct, function-clear, chapter-9 feel"),
            new Alias("Silent Dome",  "silent_dome",  "寂穹",     "Quieter, sets off content less"),
            new Alias("Saturnine UI", "saturnine_ui", "土星界面", "Chapter-8 Saturn system, cold & British"),
            new Alias("Orbital UI",   "orbital_ui",   "轨道界面", "Ring/orbit feel, techy but restrained"),
            new Alias("Aperture UI",  "aperture_ui",  "光圈界面", "Geometric, reserved, indirect"),
            new Alias("Vault UI",     "vault_ui",     "穹拱界面", "Dome/vault, strong spatial feel"),
            new Alias("Caelum UI",    "caelum_ui",    "苍穹界面", "Latin 'sky/vault of heaven', elegant")
    );

    public static Alias byIndex(int i) {
        return ALL.get(Math.floorMod(i, ALL.size()));
    }
}
