package mods.hexagon.thrusted.space.dimension;

import mods.hexagon.thrusted.Thrusted;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.OptionalLong;

public class SpaceDimension {
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES =
            DeferredRegister.create(Registries.DIMENSION_TYPE, Thrusted.MODID);

    public static final DeferredHolder<DimensionType, DimensionType> SPACE_DIMENSION_TYPE =
            DIMENSION_TYPES.register("space_type", () -> new DimensionType(
                    OptionalLong.empty(),
                    true,
                    false,
                    false,
                    true,
                    1.0,
                    false,
                    false,
                    0,
                    256,
                    256,
                    BlockTags.INFINIBURN_OVERWORLD,
                    net.minecraft.world.level.dimension.BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                    0.0f,
                    new DimensionType.MonsterSettings(false, false,
                            net.minecraft.util.valueproviders.UniformInt.of(0, 0), 0)
            ));
}
