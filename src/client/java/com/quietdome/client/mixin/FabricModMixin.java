package com.quietdome.client.mixin;

import com.quietdome.AliasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让 Mod Menu 的条目显示本机别名，而不是写死的 "Quiet Dome"。
 * 同样按名字注入，编译期不依赖 Mod Menu。
 */
@Mixin(targets = "com.terraformersmc.modmenu.util.mod.fabric.FabricMod")
public class FabricModMixin {
    @Shadow
    public String getId() {
        throw new AssertionError();
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void quietdome$aliasName(CallbackInfoReturnable<String> cir) {
        if ("quiet_dome".equals(this.getId())) {
            cir.setReturnValue(AliasManager.get().enName());
        }
    }
}
