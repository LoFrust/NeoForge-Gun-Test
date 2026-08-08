package com.lofrust.lgm.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, "lgm");

    public static final Supplier<SoundEvent> MAXIMUM_EFFORT_SHOOT =
            registerSoundEvent("maximum_effort_shoot");

    public static final Supplier<SoundEvent> MAXIMUM_EFFORT_MELEE =
            registerSoundEvent("maximum_effort_melee");

   // public static final Supplier<SoundEvent> MAXIMUM_EFFORT_RELOAD =
           // registerSoundEvent("maximum_effort_reload");

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("lgm", name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}