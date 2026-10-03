package com.quietdome.client.mixin;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 把 OptionsSubScreen.list 露出来——它在父类里，shadow 不稳。 */
@Mixin(OptionsSubScreen.class)
public interface OptionsSubScreenAccessor {
    @Accessor("list")
    OptionsList quietdome$getList();
}
