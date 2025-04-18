package net.ugi.sf_hypertube.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;


public class HypertubeSupportBlockEntity extends BlockEntity {
    private float rotation;//probalby not needed?
    public BlockPos targetPositive = null;//todo do we need to make this private and use get and set?
    public BlockPos targetNegative = null;
    public String targetPositiveType = null;
    public String targetNegativeType = null;
    public String positiveTypeInfo = null;
    public String negativeTypeInfo = null;

    public final WeakHashMap<Entity, Integer> discardEntities  = new WeakHashMap<>();

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    //constructor
    public HypertubeSupportBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.HYPERTUBE_SUPPORT_BE.get(), pos, blockState);
    }

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for(int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        if(targetPositive != null) {
            tag.putIntArray("targetPositive", List.of(targetPositive.getX(), targetPositive.getY(), targetPositive.getZ()));
        }
        if(targetPositiveType != null) {
            tag.putString("targetPositiveType", targetPositiveType);
        }
        if(targetNegative != null) {
            tag.putIntArray("targetNegative", List.of(targetNegative.getX(), targetNegative.getY(), targetNegative.getZ()));
        }
        if(targetNegativeType != null) {
            tag.putString("targetNegativeType", targetNegativeType);
        }
        if(positiveTypeInfo != null) {
            tag.putString("positiveTypeInfo", positiveTypeInfo);
        }
        if(negativeTypeInfo != null) {
            tag.putString("negativeTypeInfo", negativeTypeInfo);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));

        targetPositive = null;
        targetPositiveType = null;
        targetNegative = null;
        targetNegativeType = null;
        positiveTypeInfo = null;
        negativeTypeInfo = null;

        if (tag.contains("targetPositive")) {
            int[] arr = tag.getIntArray("targetPositive");
            if (arr.length == 3) {
                targetPositive = new BlockPos(arr[0], arr[1], arr[2]);
            }
        }
        if(tag.contains("targetPositiveType")) {
            targetPositiveType = tag.getString("targetPositiveType");
        }
        if (tag.contains("targetNegative")) {
            int[] arr = tag.getIntArray("targetNegative");
            if (arr.length == 3) {
                targetNegative = new BlockPos(arr[0], arr[1], arr[2]);
            }
        }
        if(tag.contains("targetNegativeType")) {
            targetNegativeType = tag.getString("targetNegativeType");
        }
        if (tag.contains("positiveTypeInfo")) {
            positiveTypeInfo = tag.getString("positiveTypeInfo");
        }
        if (tag.contains("negativeTypeInfo")) {
            negativeTypeInfo = tag.getString("negativeTypeInfo");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    public int getDirection(BlockPos previousBlockPos){
        if(targetPositive !=null &&  targetPositive.equals(previousBlockPos)) return 1;
        if(targetNegative !=null &&  targetNegative.equals(previousBlockPos)) return -1;
        return 0;
    }

    public String getCurveType(int direction){
        if(direction==1) return targetPositiveType;
        if(direction==-1) return targetNegativeType;
        return null;
    }

    public BlockPos getTargetPos(Integer direction){
        if(direction==1) return targetPositive;
        if(direction==-1) return targetNegative;
        return null;
    }

    public String getExtraInfo(Integer direction){
        if(direction==1) return positiveTypeInfo;
        if(direction==-1) return negativeTypeInfo;
        return null;
    }

    public void addEntityToDiscard(Entity entity) {
        this.discardEntities.remove(entity);
        this.discardEntities.put(entity, 5);
    }

    public void removeEntitiesFromDiscard(List<Entity> entitiesInRange) {
        List<Entity> entitiesToRemove = new ArrayList<>();
        this.discardEntities.forEach( (e,i) -> {
            this.discardEntities.put(e,i-1);
            if(entitiesInRange.contains(e)) this.discardEntities.put(e,5);
            if(i < 1) entitiesToRemove.add(e);


        });
        for(Entity e : entitiesToRemove) {
            this.discardEntities.remove(e);
        }
    }

    public float getRenderingRotation() {
        rotation=0;
        return rotation;
    }

}
