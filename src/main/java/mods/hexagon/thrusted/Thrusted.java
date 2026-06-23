package mods.hexagon.thrusted;

import com.mojang.logging.LogUtils;
import mods.hexagon.thrusted.block.*;
import mods.hexagon.thrusted.blockentity.*;
import mods.hexagon.thrusted.client.ShieldColorScreen;
import mods.hexagon.thrusted.client.TurbofanSmokeParticle;
import mods.hexagon.thrusted.client.render.*;
import mods.hexagon.thrusted.item.ThrusterBlockItem;
import mods.hexagon.thrusted.menu.ShieldColorMenu;
import mods.hexagon.thrusted.network.FriendlyNamePayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Thrusted.MODID)
public class Thrusted {
    public static final String MODID = "turbotech";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> TURBOFAN_SMOKE =
            PARTICLE_TYPES.register("turbofan_smoke", () -> new net.minecraft.core.particles.SimpleParticleType(false) {});

    // ENGINES
    public static final DeferredBlock<TurbofanBlock> TURBOFAN_BLOCK = BLOCKS.register("turbofan", () -> new TurbofanBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).noOcclusion()));
    public static final DeferredBlock<IonThrusterBlock> ION_THRUSTER_BLOCK = BLOCKS.register("ion_thruster", () -> new IonThrusterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).noOcclusion()));
    public static final DeferredBlock<Raptor3Block> RAPTOR3_BLOCK = BLOCKS.register("raptor3", () -> new Raptor3Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f).noOcclusion()));

    // MISSILE MODULES
    public static final DeferredBlock<MissileCoreBlock> MISSILE_CORE = BLOCKS.register("missile_core", () -> new MissileCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0f)));
    public static final DeferredBlock<TimedFuzeBlock> TIMED_FUZE = BLOCKS.register("timed_fuze", () -> new TimedFuzeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f)));
    public static final DeferredBlock<ImpactFuzeBlock> IMPACT_FUZE = BLOCKS.register("impact_fuze", () -> new ImpactFuzeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f)));
    public static final DeferredBlock<HeatSeekingFuzeBlock> HEAT_SEEKING_FUZE = BLOCKS.register("heat_seeking_fuze", () -> new HeatSeekingFuzeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f)));
    public static final DeferredBlock<DesignatedFuzeBlock> DESIGNATED_FUZE = BLOCKS.register("designated_fuze", () -> new DesignatedFuzeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(1.5f)));

    public static final DeferredBlock<Block> FIRE_CHARGE_PAYLOAD = BLOCKS.register("fire_charge_payload", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).strength(1.0f)));
    public static final DeferredBlock<Block> EXPLOSIVE_PAYLOAD = BLOCKS.register("explosive_payload", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f)));
    public static final DeferredBlock<Block> REPULSION_PAYLOAD = BLOCKS.register("repulsion_payload", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0f)));

    public static final DeferredBlock<MissileThrusterBlock> MISSILE_THRUSTER = BLOCKS.register("missile_thruster", () -> new MissileThrusterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f)));
    public static final DeferredBlock<MissileWingsBlock> MISSILE_WINGS = BLOCKS.register("missile_wings", () -> new MissileWingsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).noOcclusion()));

    // SHIELD GENERATOR
    public static final DeferredBlock<ShieldGeneratorBlock> SHIELD_GENERATOR_BLOCK = BLOCKS.register("shield_generator", () -> new ShieldGeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0f).noOcclusion()));

    // ITEMS
    public static final DeferredItem<Item> TURBOFAN_BLOCK_ITEM = ITEMS.register("turbofan", () -> new ThrusterBlockItem(TURBOFAN_BLOCK.get(), new Item.Properties(), 10.0, 5000.0, "1x1x1", "Medium", "Medium", "White", "Thick White", "5m", false));
    public static final DeferredItem<Item> ION_THRUSTER_BLOCK_ITEM = ITEMS.register("ion_thruster", () -> new ThrusterBlockItem(ION_THRUSTER_BLOCK.get(), new Item.Properties(), 2.0, 1000.0, "1x1x1", "Slow", "Very Slow", "Blue", "None", "3m", true));
    public static final DeferredItem<Item> RAPTOR3_BLOCK_ITEM = ITEMS.register("raptor3", () -> new ThrusterBlockItem(RAPTOR3_BLOCK.get(), new Item.Properties(), 100.0, 2800000.0, "2x1x1", "Fast", "Medium", "Yellow/Orange", "Thick Black", "8m", false));
    public static final DeferredItem<Item> SHIELD_GENERATOR_BLOCK_ITEM = ITEMS.register("shield_generator", () -> new BlockItem(SHIELD_GENERATOR_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> TAB_ICON = ITEMS.register("tab_icon", () -> new Item(new Item.Properties()));

    // BLOCK ENTITIES
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbofanBlockEntity>> TURBOFAN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("turbofan", () -> BlockEntityType.Builder.of(TurbofanBlockEntity::new, TURBOFAN_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IonThrusterBlockEntity>> ION_THRUSTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("ion_thruster", () -> BlockEntityType.Builder.of(IonThrusterBlockEntity::new, ION_THRUSTER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Raptor3BlockEntity>> RAPTOR3_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("raptor3", () -> BlockEntityType.Builder.of(Raptor3BlockEntity::new, RAPTOR3_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MissileCoreBlockEntity>> MISSILE_CORE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("missile_core", () -> BlockEntityType.Builder.of(MissileCoreBlockEntity::new, MISSILE_CORE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TimedFuzeBlockEntity>> TIMED_FUZE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("timed_fuze", () -> BlockEntityType.Builder.of(TimedFuzeBlockEntity::new, TIMED_FUZE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatSeekingFuzeBlockEntity>> HEAT_SEEKING_FUZE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("heat_seeking_fuze", () -> BlockEntityType.Builder.of(HeatSeekingFuzeBlockEntity::new, HEAT_SEEKING_FUZE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DesignatedFuzeBlockEntity>> DESIGNATED_FUZE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("designated_fuze", () -> BlockEntityType.Builder.of(DesignatedFuzeBlockEntity::new, DESIGNATED_FUZE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MissileThrusterBlockEntity>> MISSILE_THRUSTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("missile_thruster", () -> BlockEntityType.Builder.of(MissileThrusterBlockEntity::new, MISSILE_THRUSTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MissileWingsBlockEntity>> MISSILE_WINGS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("missile_wings", () -> BlockEntityType.Builder.of(MissileWingsBlockEntity::new, MISSILE_WINGS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShieldGeneratorBlockEntity>> SHIELD_GENERATOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("shield_generator", () -> BlockEntityType.Builder.of(ShieldGeneratorBlockEntity::new, SHIELD_GENERATOR_BLOCK.get()).build(null));

    // MENUS
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<ShieldColorMenu>> SHIELD_COLOR_MENU =
            MENUS.register("shield_color", () -> new net.minecraft.world.inventory.MenuType<>(
                    (id, inv) -> new ShieldColorMenu(id, inv),
                    net.minecraft.world.flag.FeatureFlagSet.of()));

    // SOUNDS
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SHIELD_BOOTUP_SOUND =
            SOUND_EVENTS.register("shield_bootup", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "shield_bootup")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SHIELD_SHUTDOWN_SOUND =
            SOUND_EVENTS.register("shield_shutdown", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "shield_shutdown")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SHIELD_AMBIENT_SOUND =
            SOUND_EVENTS.register("shield_ambient", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "shield_ambient")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SHIELD_HIT_SOUND =
            SOUND_EVENTS.register("shield_hit", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "shield_hit")));

    // TAB
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THRUSTED_TAB = CREATIVE_MODE_TABS.register("turbotech_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.turbotech_tab")).icon(() -> TAB_ICON.get().getDefaultInstance()).displayItems((parameters, output) -> {
        output.accept(TURBOFAN_BLOCK_ITEM.get());
        output.accept(ION_THRUSTER_BLOCK_ITEM.get());
        output.accept(RAPTOR3_BLOCK_ITEM.get());
        output.accept(SHIELD_GENERATOR_BLOCK_ITEM.get());
    }).build());

    public Thrusted(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class, event -> {
            var registrar = event.registrar(MODID);
            registrar.playToServer(
                    FriendlyNamePayload.TYPE,
                    FriendlyNamePayload.STREAM_CODEC,
                    FriendlyNamePayload::handle
            );
        });
    }

    @EventBusSubscriber(modid = MODID)
    public static class GameEvents {
        @SubscribeEvent
        public static void onServerAboutToStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
            mods.hexagon.thrusted.space.CelestialBodyRegistry.init();
            mods.hexagon.thrusted.space.resource.SpaceResourceRegistry.init();
            mods.hexagon.thrusted.space.SpaceDimensions.registerDimensions(event.getServer());
            Thrusted.LOGGER.info("Thrusted space systems initialized - {} celestial bodies, {} resources",
                    mods.hexagon.thrusted.space.CelestialBodyRegistry.getAll().size(),
                    mods.hexagon.thrusted.space.resource.SpaceResourceRegistry.getAll().size());
        }

        @SubscribeEvent
        public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
            mods.hexagon.thrusted.command.SpaceCommand.register(event.getDispatcher());
        }
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(TURBOFAN_BLOCK_ENTITY.get(), TurbofanBlockEntityRenderer::new);
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ION_THRUSTER_BLOCK_ENTITY.get(), IonThrusterBlockEntityRenderer::new);
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(RAPTOR3_BLOCK_ENTITY.get(), Raptor3BlockEntityRenderer::new);
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(SHIELD_GENERATOR_BLOCK_ENTITY.get(), ShieldGeneratorBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(SHIELD_COLOR_MENU.get(), ShieldColorScreen::new);
        }

        @SubscribeEvent
        public static void registerParticleProviders(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(TURBOFAN_SMOKE.get(), TurbofanSmokeParticle.Provider::new);
        }

        @SubscribeEvent
        public static void onModelRegister(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
            event.register(ShieldGeneratorBlockEntityRenderer.TOP_MODEL_MRL);
            event.register(ShieldGeneratorBlockEntityRenderer.BASE_MODEL_MRL);
            event.register(ShieldGeneratorBlockEntityRenderer.TOP_EMISSIVE_MODEL_MRL);
            event.register(ShieldGeneratorBlockEntityRenderer.BASE_EMISSIVE_MODEL_MRL);
            event.register(TurbofanBlockEntityRenderer.FAN_MODEL_MRL);
        }

        @SubscribeEvent
        public static void registerDimensionEffects(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
            event.register(mods.hexagon.thrusted.space.render.SpaceSkyRenderer.SPACE_EFFECTS,
                    new mods.hexagon.thrusted.space.render.SpaceSkyRenderer());
        }

        @SubscribeEvent
        public static void registerOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
            event.registerAboveAll(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "nav_hud"),
                    mods.hexagon.thrusted.space.nav.NavHudOverlay.INSTANCE
            );
        }

        @SubscribeEvent
        public static void registerKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(mods.hexagon.thrusted.space.nav.NavKeybind.NAV_KEY);
        }

        @SubscribeEvent
        public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            mods.hexagon.thrusted.space.nav.NavKeybind.handleTick();
        }
    }
}
