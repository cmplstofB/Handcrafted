package earth.terrarium.handcrafted.block.specialbed;

import earth.terrarium.handcrafted.Handcrafted;
import earth.terrarium.handcrafted.block.SimpleEntityBlock;
import earth.terrarium.handcrafted.block.property.DirectionalBlockSide;
import earth.terrarium.handcrafted.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class SpecialBedBlock extends BedBlock {
    public static final EnumProperty<DirectionalBlockSide> BED_SHAPE = EnumProperty.create("shape", DirectionalBlockSide.class);

    public SpecialBedBlock(Properties properties) {
        super(DyeColor.WHITE, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpecialBedBlockEntity(pos, state);
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModTags.CUSHIONS) || stack.is(ModTags.SHEETS) || player.isCrouching()) {
            if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
                return SimpleEntityBlock.cushionUse(level, pos.relative(state.getValue(BedBlock.FACING).getOpposite()), player, new ResourceLocation(Handcrafted.MOD_ID, "white_cushion"));
            }
            return SpecialBedBlock.sheetUse(level, pos, player, new ResourceLocation(Handcrafted.MOD_ID, "white_sheet"));
        } else {
            return super.use(state, level, pos, player, hand, hit);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BED_SHAPE, FACING, PART, OCCUPIED);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == getNeighbourDirection(state.getValue(PART), state.getValue(FACING))) {
            return neighborState.is(this) && neighborState.getValue(PART) != state.getValue(PART) ? state.setValue(OCCUPIED, neighborState.getValue(OCCUPIED)).setValue(BED_SHAPE, DirectionalBlockSide.getShape(this, FACING, state, level, currentPos)) : Blocks.AIR.defaultBlockState();
        } else {
            return super.updateShape(state.setValue(BED_SHAPE, DirectionalBlockSide.getShape(this, FACING, state, level, currentPos)), direction, neighborState, level, currentPos, neighborPos);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        BlockPos blockPos = context.getClickedPos();
        BlockPos blockPos2 = blockPos.relative(direction);
        Level level = context.getLevel();
        return level.getBlockState(blockPos2).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(blockPos2) ? this.defaultBlockState().setValue(FACING, direction).setValue(BED_SHAPE, DirectionalBlockSide.getShape(this, FACING, this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()), context.getLevel(), blockPos)) : null;
    }

    private static Direction getNeighbourDirection(BedPart part, Direction direction) {
        return part == BedPart.FOOT ? direction : direction.getOpposite();
    }

    public static InteractionResult sheetUse(Level level, BlockPos pos, Player player, ResourceLocation defaultSheet) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof SpecialBedBlockEntity entity) {
                ItemStack stack = player.getMainHandItem();
                if (entity.getSheet().equals(defaultSheet) && stack.is(ModTags.SHEETS)) {
                    entity.setSheet(Registry.ITEM.getKey(stack.getItem()));
                    if (!player.isCreative()) stack.shrink(1);
                    return InteractionResult.SUCCESS;
                } else if (player.getMainHandItem().isEmpty() && player.isCrouching()) {
                    if (!entity.getSheet().equals(defaultSheet)) {
                        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, Registry.ITEM.get(entity.getSheet()).getDefaultInstance());
                        entity.setSheet(defaultSheet);
                        itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.5));
                        level.addFreshEntity(itemEntity);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.CONSUME_PARTIAL;
    }
}
