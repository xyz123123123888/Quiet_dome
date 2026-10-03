package com.quietdome.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给 ViaFabric 的设置界面换皮 + 重排（26.x 分支）。
 * 用字符串按名字注入，所以编译期不依赖 ViaFabric——没装也能正常编译。
 */
@Mixin(targets = "com.viaversion.fabric.gui.ViaConfigScreen")
public class ViaConfigScreenMixin {
    private static final int PANEL = 0xC00A1824;   // phi --phi-bg-panel + alpha
    private static final int CYAN  = 0xFF31A8CA;   // phi --phi-cyan-neon

    /** 换皮：控件底下垫一块深色面板加青边。 */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"))
    private void quietdome$skin(GuiGraphicsExtractor g, int mx, int my, float d, CallbackInfo ci) {
        int w = g.guiWidth(), h = g.guiHeight();
        int pw = Math.min(w - 40, 320);
        int px = (w - pw) / 2;
        int py = h / 3 - 8;
        int ph = 88;
        g.fill(px - 6, py - 6, px + pw + 6, py + ph + 6, PANEL);
        g.outline(px - 6, py - 6, pw + 12, ph + 12, CYAN);
    }

    /** 重排：居中一列——输入框单独一行，两个开关并排，Done 在下面。 */
    @Inject(method = "init()V", at = @At("TAIL"))
    private void quietdome$reflow(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        int contentW = Math.min(self.width - 40, 320);
        int cx = self.width / 2;
        int y = self.height / 3;

        EditBox edit = null;
        java.util.List<Button> buttons = new java.util.ArrayList<>();
        for (var child : self.children()) {
            if (child instanceof EditBox e) edit = e;
            else if (child instanceof Button b) buttons.add(b);
        }

        if (edit != null) {
            edit.setX(cx - contentW / 2);
            edit.setY(y);
            edit.setWidth(contentW);
            edit.setHeight(20);
        }
        int halfW = (contentW - 4) / 2;
        if (buttons.size() >= 1) place(buttons.get(0), cx - contentW / 2, y + 26, halfW, 20);
        if (buttons.size() >= 2) place(buttons.get(1), cx + 2, y + 26, halfW, 20);
        if (buttons.size() >= 3) place(buttons.get(2), cx - contentW / 2, y + 52, contentW, 20);
    }

    private static void place(AbstractWidget w, int x, int y, int width, int height) {
        w.setX(x); w.setY(y); w.setWidth(width); w.setHeight(height);
    }
}
