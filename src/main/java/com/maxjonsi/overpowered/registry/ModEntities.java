package com.maxjonsi.overpowered.registry;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.BlueVortexEntity;
import com.maxjonsi.overpowered.entity.DomainEntity;
import com.maxjonsi.overpowered.entity.HollowPurpleEntity;
import com.maxjonsi.overpowered.entity.HomingRocketEntity;
import com.maxjonsi.overpowered.entity.JudgementCutEndEntity;
import com.maxjonsi.overpowered.entity.JudgementCutEntity;
import com.maxjonsi.overpowered.entity.NukeEntity;
import com.maxjonsi.overpowered.entity.ShadowRemnantEntity;
import com.maxjonsi.overpowered.entity.ShadowSoldierEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.WitherSkeleton;

public class ModEntities {
    public static final EntityType<HomingRocketEntity> HOMING_ROCKET = register("homing_rocket",
            EntityType.Builder.<HomingRocketEntity>of(HomingRocketEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(1).build("homing_rocket"));

    public static final EntityType<NukeEntity> NUKE = register("nuke",
            EntityType.Builder.<NukeEntity>of(NukeEntity::new, MobCategory.MISC)
                    .sized(1.2f, 1.2f).clientTrackingRange(16).updateInterval(1).fireImmune().build("nuke"));

    public static final EntityType<ShadowRemnantEntity> SHADOW_REMNANT = register("shadow_remnant",
            EntityType.Builder.<ShadowRemnantEntity>of(ShadowRemnantEntity::new, MobCategory.MISC)
                    .sized(0.9f, 0.1f).clientTrackingRange(8).updateInterval(20).fireImmune().build("shadow_remnant"));

    public static final EntityType<JudgementCutEntity> JUDGEMENT_CUT = register("judgement_cut",
            EntityType.Builder.<JudgementCutEntity>of(JudgementCutEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f).clientTrackingRange(8).updateInterval(10).fireImmune().build("judgement_cut"));

    public static final EntityType<JudgementCutEndEntity> JUDGEMENT_CUT_END = register("judgement_cut_end",
            EntityType.Builder.<JudgementCutEndEntity>of(JudgementCutEndEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f).clientTrackingRange(16).updateInterval(10).fireImmune().build("judgement_cut_end"));

    public static final EntityType<HollowPurpleEntity> HOLLOW_PURPLE = register("hollow_purple",
            EntityType.Builder.<HollowPurpleEntity>of(HollowPurpleEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.5f).clientTrackingRange(16).updateInterval(1).fireImmune().build("hollow_purple"));

    public static final EntityType<BlueVortexEntity> BLUE_VORTEX = register("blue_vortex",
            EntityType.Builder.<BlueVortexEntity>of(BlueVortexEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(10).fireImmune().build("blue_vortex"));

    public static final EntityType<DomainEntity> DOMAIN = register("domain",
            EntityType.Builder.<DomainEntity>of(DomainEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(24).updateInterval(10).fireImmune().build("domain"));

    public static final EntityType<ShadowSoldierEntity> SHADOW_SOLDIER = register("shadow_soldier",
            EntityType.Builder.<ShadowSoldierEntity>of(ShadowSoldierEntity::new, MobCategory.MISC)
                    .sized(0.7f, 2.4f).clientTrackingRange(10).fireImmune().build("shadow_soldier"));

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, Overpowered.id(name), type);
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SHADOW_SOLDIER, WitherSkeleton.createAttributes());
    }
}
