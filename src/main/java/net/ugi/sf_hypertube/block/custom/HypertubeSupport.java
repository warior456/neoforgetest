package net.ugi.sf_hypertube.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ugi.sf_hypertube.block.ModBlocks;
import net.ugi.sf_hypertube.block.entity.HypertubeSupportBlockEntity;
import net.ugi.sf_hypertube.entity.HypertubeEntity;
import net.ugi.sf_hypertube.entity.ModEntities;
import net.ugi.sf_hypertube.util.Bezier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class HypertubeSupport extends BaseEntityBlock {
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    public static final MapCodec<HypertubeSupport> CODEC = simpleCodec(HypertubeSupport::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public HypertubeSupport(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    //----------------pillar rotations ----------
    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return rotatePillar(state, rot);
    }

    public static BlockState rotatePillar(BlockState state, Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch ((Direction.Axis)state.getValue(AXIS)) {
                    case X:
                        return state.setValue(AXIS, Direction.Axis.Z);
                    case Z:
                        return state.setValue(AXIS, Direction.Axis.X);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    //----------------block entity-----------------


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(new BlockPos(pos), this, 1);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
        if (blockEntity instanceof HypertubeSupportBlockEntity hypertubeSupportBlockEntity ) {
            BlockPos checkpos = null;
            if (hypertubeSupportBlockEntity.targetNegative != null && hypertubeSupportBlockEntity.targetPositive == null) {
                checkpos  = pos.relative(axis, 2);
            }
            if (hypertubeSupportBlockEntity.targetNegative == null && hypertubeSupportBlockEntity.targetPositive != null) {
                checkpos = pos.relative(axis, -2);
            }

            if (checkpos == null) return;
            List<Entity> entities = level.getEntities(null, new AABB(checkpos.offset(1,2,1).getBottomCenter(), checkpos.offset(-1,0,-1).getBottomCenter()));
            hypertubeSupportBlockEntity.removeEntitiesFromDiscard(entities);
            if(entities.isEmpty()) return;


            BlockPos finalCheckpos = checkpos;
            entities.forEach(entity -> {
                if(!hypertubeSupportBlockEntity.discardEntities.containsKey(entity) && !(entity instanceof HypertubeEntity)) {
                    HypertubeEntity hyperTubeEntity = new HypertubeEntity(ModEntities.HYPERTUBE_ENTITY.get(), level);
                    hyperTubeEntity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    //hyperTubeEntity.path
                    //level.addFreshEntity(hyperTubeEntity);
                    //entity.startRiding(hyperTubeEntity);
                    BlockPos currentPos = pos;
                    /*
                    crash fix for: Cannot get property EnumProperty{name=axis, clazz=class net.minecraft.core.Direction$Axis, values=[x, y, z]} as it does not exist in Block{minecraft:air}
                    when entering 2 hypertubes
                    */
                    if(!level.getBlockState(currentPos).getProperties().contains(AXIS)) return;

                    Direction.Axis currentAxis = level.getBlockState(currentPos).getValue(AXIS);
                    int dir = ((pos.getX() - finalCheckpos.getX()) + (pos.getY() - finalCheckpos.getY()) + (pos.getZ() - finalCheckpos.getZ()));
                    int currentDirection = (dir) > 0 ? 1 : -1;
                    BlockPos nextPos = hypertubeSupportBlockEntity.getTargetPos(currentDirection);
                    if(!level.getBlockState(nextPos).getProperties().contains(AXIS)) return; //crashfix part 2
                    Direction.Axis nextAxis = level.getBlockState(nextPos).getValue(AXIS);
                    BlockEntity nextEntity = level.getBlockEntity(nextPos);
                    int nextDirection = 0;
                    if (nextEntity instanceof HypertubeSupportBlockEntity nextHypertubeSupportBlockEntity) {
                        nextDirection = nextHypertubeSupportBlockEntity.getDirection(currentPos);
                        Bezier bezier = new Bezier();
                        bezier.setCurve(nextHypertubeSupportBlockEntity.getCurveType(nextDirection));

                        String extraData1 =hypertubeSupportBlockEntity.getExtraInfo(nextDirection);
                        String extraData2 =nextHypertubeSupportBlockEntity.getExtraInfo(nextDirection);

                        hyperTubeEntity.addPath(
                                Arrays.stream(bezier.calcBezierArray(currentPos, currentAxis, currentDirection, extraData1, nextPos, nextAxis, nextDirection, extraData2)).toList(),
                                currentPos, nextPos);
                        level.addFreshEntity(hyperTubeEntity);
                        hyperTubeEntity.setSpeed(20f);
                        entity.startRiding(hyperTubeEntity);
                    }
                }
            });


        }
        super.tick(state, level, pos, random);
    }

    
    public List<BlockPos> getNextPath(Level level, BlockPos previousSupportPos, BlockPos currentSupportPos) {
        Direction.Axis currentAxis = level.getBlockState(currentSupportPos).getValue(AXIS);

        BlockEntity currentEntity = level.getBlockEntity(currentSupportPos);
        if(currentEntity instanceof HypertubeSupportBlockEntity currentHypertubeSupportBlockEntity) {
            int currentDirection = -currentHypertubeSupportBlockEntity.getDirection(previousSupportPos);
            BlockPos nextPos = currentHypertubeSupportBlockEntity.getTargetPos(currentDirection);
            BlockEntity nextEntity = level.getBlockEntity(nextPos);
            if(!(level.getBlockState(nextPos).getBlock() instanceof HypertubeSupport)){//extra anti crash
                if(currentHypertubeSupportBlockEntity.getDirection(nextPos)==1){//todo maybe make this a function and call more often
                    currentHypertubeSupportBlockEntity.targetPositive = null;
                    currentHypertubeSupportBlockEntity.targetPositiveType = null;
                }else if(currentHypertubeSupportBlockEntity.getDirection(nextPos)==-1){
                    currentHypertubeSupportBlockEntity.targetNegative = null;
                    currentHypertubeSupportBlockEntity.targetNegativeType = null;
                }
                return null;
            }
            Direction.Axis nextAxis = level.getBlockState(nextPos).getValue(AXIS);
            if(nextEntity instanceof HypertubeSupportBlockEntity nextHypertubeSupportBlockEntity) {
                int nextDirection = nextHypertubeSupportBlockEntity.getDirection(currentSupportPos);
                Bezier bezier = new Bezier();
                bezier.setCurve(currentHypertubeSupportBlockEntity.getCurveType(currentDirection));

                String extraData1 =currentHypertubeSupportBlockEntity.getExtraInfo(currentDirection);
                String extraData2 =nextHypertubeSupportBlockEntity.getExtraInfo(nextDirection);


                return List.of(bezier.calcBezierArray(currentSupportPos, currentAxis, currentDirection, extraData1, nextPos, nextAxis, nextDirection, extraData2));
            }
        }
        return null;
    }

    public List<BlockPos> getPathBetween(Level level, BlockPos supportPos1, BlockPos supportPos2) {
        Direction.Axis axis1 = level.getBlockState(supportPos1).getValue(AXIS);
        Direction.Axis axis2 = level.getBlockState(supportPos2).getValue(AXIS);
        return getPathBetween(level, supportPos1, axis1, supportPos2, axis2);
    }

    public List<BlockPos> getPathBetween(Level level, BlockPos supportPos1, Direction.Axis axis1, BlockPos supportPos2, Direction.Axis axis2) {
        BlockEntity blockEntity1 = level.getBlockEntity(supportPos1);
        BlockEntity blockEntity2 = level.getBlockEntity(supportPos2);
        int direction1 = 0;
        int direction2 = 0;

        if(blockEntity1 instanceof HypertubeSupportBlockEntity hypertubeSupportBlockEntity1) {
            direction1 = -hypertubeSupportBlockEntity1.getDirection(supportPos1);

/*            if(!(level.getBlockState(supportPos2).getBlock() instanceof HypertubeSupport)){//extra anti-crash
                if(hypertubeSupportBlockEntity1.getDirection(supportPos2)==1){//todo maybe make this a function and call more often
                    hypertubeSupportBlockEntity1.targetPositive = null;
                    hypertubeSupportBlockEntity1.targetPositiveType = null;
                }else if(hypertubeSupportBlockEntity1.getDirection(supportPos2)==-1){
                    hypertubeSupportBlockEntity1.targetNegative = null;
                    hypertubeSupportBlockEntity1.targetNegativeType = null;
                }
                return null;
            }*/
        }else {
            return null;
        }
        if(blockEntity2 instanceof HypertubeSupportBlockEntity hypertubeSupportBlockEntity2) {
            direction2 = hypertubeSupportBlockEntity2.getDirection(supportPos1);
        }else{
            return null;
        }
        String extraData1 =hypertubeSupportBlockEntity1.getExtraInfo(direction1);
        String extraData2 =hypertubeSupportBlockEntity2.getExtraInfo(direction2);
        Bezier bezier = new Bezier();
        bezier.setCurve(hypertubeSupportBlockEntity1.getCurveType(-hypertubeSupportBlockEntity1.getDirection(supportPos2)));
        return List.of(bezier.calcBezierArray(supportPos1, axis1, direction1, extraData1, supportPos2, axis2, direction2, extraData2));
    }

    public BlockPos getNextTargetPos(Level level, BlockPos previousPos, BlockPos currentPos) {
        BlockEntity currentEntity = level.getBlockEntity(currentPos);
        if(currentEntity instanceof HypertubeSupportBlockEntity currentHypertubeSupportBlockEntity) {
            int currentDirection = -currentHypertubeSupportBlockEntity.getDirection(previousPos);
            return currentHypertubeSupportBlockEntity.getTargetPos(currentDirection);
            }
        return null;
    }

    public boolean isConnectedBothSides(Level level, BlockPos pos){
        BlockEntity entity = level.getBlockEntity(pos);
        if(entity instanceof HypertubeSupportBlockEntity hypertubeSupportBlockEntity) {
            return hypertubeSupportBlockEntity.targetPositive != null && hypertubeSupportBlockEntity.targetNegative != null;
        }
        return false;
    }
    

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(new BlockPos(pos), this, 1);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new HypertubeSupportBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {//todo remove connected tube blocks
        if(state.getBlock() != newState.getBlock()) {
            if(level.getBlockEntity(pos) instanceof HypertubeSupportBlockEntity hypertubeSupportBlockEntity){
                hypertubeSupportBlockEntity.drops();
/*                if(hypertubeSupportBlockEntity.targetPositive != null){//experintal remove connected tube blocks
                    RemovePath(level, getPathBetween(level,  pos, state.getValue(AXIS),   hypertubeSupportBlockEntity.targetPositive, level.getBlockState(hypertubeSupportBlockEntity.targetPositive).getValue(AXIS)));
                }
                if(hypertubeSupportBlockEntity.targetNegative != null){
                    RemovePath(level, getPathBetween(level, pos, state.getValue(AXIS), hypertubeSupportBlockEntity.targetNegative, level.getBlockState(hypertubeSupportBlockEntity.targetPositive).getValue(AXIS)));
                }*/
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected void RemovePath(Level level, List<BlockPos> path){
        path.forEach( blockPos ->
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2)
                );
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(level.getBlockEntity(pos) instanceof HypertubeSupportBlockEntity hypertubeSupportBlockEntity) {
            if(hypertubeSupportBlockEntity.inventory.getStackInSlot(0).isEmpty() && stack.is(Items.PURPUR_PILLAR) || stack.is(ModBlocks.HYPERTUBE_ENTRANCE.asItem())) {
                hypertubeSupportBlockEntity.inventory.insertItem(0, stack.copy(), false);
                stack.shrink(1);
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 2f);
                return ItemInteractionResult.SUCCESS;
            } else if(stack.isEmpty()) {
                ItemStack stackOnPedestal = hypertubeSupportBlockEntity.inventory.extractItem(0, 1, false);
                if (stackOnPedestal.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                player.setItemInHand(InteractionHand.MAIN_HAND, stackOnPedestal);
                hypertubeSupportBlockEntity.clearContents();
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 1f);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return ItemInteractionResult.SUCCESS;
    }
}