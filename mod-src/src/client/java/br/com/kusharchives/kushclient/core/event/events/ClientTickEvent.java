package br.com.kusharchives.kushclient.core.event.events;

import net.minecraft.client.Minecraft;

public record ClientTickEvent(Minecraft client) {}
