package net.ugi.hypertubes.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.ugi.hypertubes.block.entity.HypertubeSupportBlockEntity;

public class HypertubeSupportBlockEntityRenderer implements BlockEntityRenderer<HypertubeSupportBlockEntity> {
    public HypertubeSupportBlockEntityRenderer(BlockEntityRendererProvider.Context context) {

    }
    @Override
    public void render(HypertubeSupportBlockEntity hypertubeSupportBlockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack stack = hypertubeSupportBlockEntity.inventory.getStackInSlot(0);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.scale(2.2f, 2.2f, 2.2f);
        float rotation = hypertubeSupportBlockEntity.getRenderingRotation();
        if(rotation!=-1){
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        }

        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, getLightLevel(hypertubeSupportBlockEntity.getLevel(),
                hypertubeSupportBlockEntity.getBlockPos()), OverlayTexture.NO_OVERLAY, poseStack, bufferSource, hypertubeSupportBlockEntity.getLevel(), 1);
        poseStack.popPose();
    }
    private int getLightLevel(Level level, BlockPos pos) {
        int bLight = level.getBrightness(LightLayer.BLOCK, pos);
        int sLight = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(bLight, sLight);
    }

}
