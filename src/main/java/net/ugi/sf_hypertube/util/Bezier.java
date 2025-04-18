package net.ugi.sf_hypertube.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.ugi.sf_hypertube.block.ModBlocks;

import java.util.LinkedHashSet;
import java.util.Set;

public class Bezier {

    private double bezierHelpPosMultiplier = 0.5;//default 0.5

    private int block1UsedDirection = 0;
    private int block2UsedDirection = 0;

    public Bezier() {

    }

    public int getBlock1Direction(){
        return block1UsedDirection;
    }
    public int getBlock2Direction(){
        return block2UsedDirection;
    }

    public void setCurve(String name){
        if (name.equals("Curved")) {
            this.bezierHelpPosMultiplier = 0.5;
        }
        if (name.equals("Overkill")) {
            this.bezierHelpPosMultiplier = 3;
        }
        if (name.equals("Straight")) {
            this.bezierHelpPosMultiplier = 0;
        }
    }

    public BlockPos[] calcBezierArray(BlockPos b1Pos, Direction.Axis b1Axis, int b1Direction, BlockPos b2Pos, Direction.Axis b2Axis, int b2Direction) {
        BlockPos pos0 = b1Pos;
        BlockPos pos1 = null;
        BlockPos pos2 = null;
        BlockPos pos3 = b2Pos;
        double distanceBetweenBlocks = b2Pos.getCenter().distanceTo(b1Pos.getCenter());
        double helperPosOffSet = distanceBetweenBlocks * bezierHelpPosMultiplier;

        if (b1Direction == 0) { // 2 sides available
            pos1 = b2Pos.getCenter().distanceTo(b1Pos.relative(b1Axis,(int) helperPosOffSet).getCenter()) < b2Pos.getCenter().distanceTo(b1Pos.relative(b1Axis,-(int) helperPosOffSet).getCenter()) ? b1Pos.relative(b1Axis,(int) helperPosOffSet) : b1Pos.relative(b1Axis,-(int) helperPosOffSet);
            this.block1UsedDirection = b2Pos.getCenter().distanceTo(b1Pos.relative(b1Axis,(int) helperPosOffSet).getCenter()) < b2Pos.getCenter().distanceTo(b1Pos.relative(b1Axis,-(int) helperPosOffSet).getCenter()) ? 1:-1;

        } else { // 1 side available
            pos1 = b1Pos.relative(b1Axis, (int) helperPosOffSet * b1Direction);
            this.block1UsedDirection = b1Direction;
        }

        if (b2Direction == 0) { // 2 sides available
            pos2 = b1Pos.getCenter().distanceTo(b2Pos.relative(b2Axis, (int) helperPosOffSet).getCenter()) < b1Pos.getCenter().distanceTo(b2Pos.relative(b2Axis, -(int) helperPosOffSet).getCenter()) ? b2Pos.relative(b2Axis, (int) helperPosOffSet) : b2Pos.relative(b2Axis, -(int) helperPosOffSet);
            this.block2UsedDirection = b1Pos.getCenter().distanceTo(b2Pos.relative(b2Axis, (int) helperPosOffSet).getCenter()) < b1Pos.getCenter().distanceTo(b2Pos.relative(b2Axis, -(int) helperPosOffSet).getCenter()) ? 1 : -1;

        } else { // 1 side available
            pos2 = b2Pos.relative(b2Axis, (int) helperPosOffSet * b2Direction);
            this.block2UsedDirection = b2Direction;
        }

        //fix connection problems ( with direction ) in Straight mode (posmultiplier = 0)
        if(bezierHelpPosMultiplier == 0){
            if (b1Direction == 0) {
                if (b1Axis == Direction.Axis.X) {
                    this.block1UsedDirection = (pos3.getX() - pos0.getX());
                }
                if (b1Axis == Direction.Axis.Y) {
                    this.block1UsedDirection = (pos3.getY() - pos0.getY());
                }
                if (b1Axis == Direction.Axis.Z) {
                    this.block1UsedDirection = (pos3.getZ() - pos0.getZ());
                }
                if (this.block1UsedDirection == 0) this.block1UsedDirection = 1;
                this.block1UsedDirection = this.block1UsedDirection / Math.abs(this.block1UsedDirection);
            }

            if (b2Direction == 0) {
                if (b2Axis == Direction.Axis.X) {
                    this.block2UsedDirection = (pos3.getX() - pos0.getX());
                }
                if (b2Axis == Direction.Axis.Y) {
                    this.block2UsedDirection = (pos3.getY() - pos0.getY());
                }
                if (b2Axis == Direction.Axis.Z) {
                    this.block2UsedDirection = (pos3.getZ() - pos0.getZ());
                }
                if (this.block2UsedDirection == 0) this.block2UsedDirection = 1;
                this.block2UsedDirection = -this.block2UsedDirection / Math.abs(this.block2UsedDirection);
            }

        }

        int steps = (int)(1.5*Math.abs(b1Pos.getX() - b2Pos.getX()) + 1.5*Math.abs(b1Pos.getY() - b2Pos.getY()) + 1.5*Math.abs(b1Pos.getZ() - b2Pos.getZ()) + 10);


        BlockPos[] blockPosArray = new BlockPos[steps];

        //bezier curve calc
        //https://www.desmos.com/calculator/cahqdxeshd?lang=nl
        //3D version of this

        for(int i = 0 ; i < steps; i++) {
            double t = i/(double)steps;

            int x = (int)Math.round((1-t)*((1-t)*((1-t)*pos0.getX() + t*pos1.getX()) + t*((1-t)*pos1.getX() + t*pos2.getX())) + t*((1-t)*((1-t)*pos1.getX() + t*pos2.getX()) + t*((1-t)*pos2.getX() + t*pos3.getX())));
            int y = (int)Math.round((1-t)*((1-t)*((1-t)*pos0.getY() + t*pos1.getY()) + t*((1-t)*pos1.getY() + t*pos2.getY())) + t*((1-t)*((1-t)*pos1.getY() + t*pos2.getY()) + t*((1-t)*pos2.getY() + t*pos3.getY())));
            int z = (int)Math.round((1-t)*((1-t)*((1-t)*pos0.getZ() + t*pos1.getZ()) + t*((1-t)*pos1.getZ() + t*pos2.getZ())) + t*((1-t)*((1-t)*pos1.getZ() + t*pos2.getZ()) + t*((1-t)*pos2.getZ() + t*pos3.getZ())));

            blockPosArray[i] = new BlockPos(x,y,z);
        }
        Set<BlockPos> blockSet = new LinkedHashSet<>();
        for (BlockPos pos : blockPosArray) {
            if (pos != null) {
                blockSet.add(pos);
            }
        }

        return blockSet.toArray(new BlockPos[0]);
    }
}
