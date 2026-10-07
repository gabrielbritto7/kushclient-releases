package br.com.kusharchives.kushclient.core.module;

import br.com.kusharchives.kushclient.KushClientClient;
import br.com.kusharchives.kushclient.core.config.ConfigManager;
import br.com.kusharchives.kushclient.modules.hud.CoordinatesModule;
import br.com.kusharchives.kushclient.modules.hud.FpsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private ConfigManager configManager;

    public void registerCoreModules() {
        register(new FpsModule());
        register(new CoordinatesModule());
        pending("direction", "Direction", "Mostra a direção do jogador.", Category.HUD);
        pending("clock", "Clock", "Mostra a hora local.", Category.HUD);
        pending("memory_usage", "Memory Usage", "Mostra uso de memória.", Category.HUD);
        pending("biome", "Biome", "Mostra o bioma atual.", Category.HUD);
        pending("resource_packs", "Resource Packs", "Mostra o resource pack em uso.", Category.HUD);
        pending("server_info", "Server Info", "Mostra informações do servidor.", Category.HUD);
        pending("ping", "Ping", "Mostra a latência com o servidor.", Category.HUD);
        pending("server_tps", "Server TPS", "Mostra TPS estimado do servidor.", Category.HUD);
        pending("day_counter", "Day Counter", "Mostra o dia atual do mundo.", Category.HUD);
        pending("speed", "Speed", "Mostra a velocidade do jogador.", Category.HUD);
        pending("saturation", "Saturation", "Mostra saturação de comida.", Category.HUD);
        pending("armor_status", "Armor Status", "Mostra o estado da armadura.", Category.HUD);
        pending("potion_effects", "Potion Effects", "Mostra efeitos de poção ativos.", Category.HUD);
        pending("keystrokes", "Keystrokes", "Mostra WASD e cliques.", Category.HUD);
        pending("cps_counter", "CPS Counter", "Mostra cliques por segundo.", Category.HUD);
        pending("active_modules", "Active Modules", "Lista módulos ativos.", Category.HUD);
        pending("combo_counter", "Combo Counter", "Mostra sequência de hits.", Category.HUD);
        pending("chat_timestamps", "Chat Timestamps", "Adiciona horário às mensagens.", Category.HUD);
        pending("damage_indicator", "Damage Indicator", "Mostra dano recente.", Category.HUD);
        pending("durability_warning", "Durability Warning", "Avisa sobre baixa durabilidade.", Category.HUD);
        pending("tab_ping", "Tab Ping", "Exibe ping na lista de jogadores.", Category.HUD);
        pending("waypoints", "Waypoints", "Marca pontos no mundo.", Category.HUD);
        pending("full_bright", "Full Bright", "Aumenta a iluminação visual.", Category.RENDER);
        pending("block_overlay", "Block Overlay", "Personaliza o contorno dos blocos.", Category.RENDER);
        pending("hitboxes", "Hitboxes", "Exibe hitboxes configuráveis.", Category.RENDER);
        pending("remove_hurt_shake", "Remove Hurt Shake", "Remove o balanço ao tomar dano.", Category.RENDER);
        pending("hide_boss_bar", "Hide Boss Bar", "Oculta a barra de bosses.", Category.RENDER);
        pending("time_changer", "Time Changer", "Altera o horário apenas visualmente.", Category.RENDER);
        pending("custom_crosshair", "Custom Crosshair", "Personaliza a mira.", Category.RENDER);
        pending("custom_scoreboard", "Custom Scoreboard", "Personaliza o placar lateral.", Category.RENDER);
        pending("particle_effects", "Particle Effects", "Personaliza partículas.", Category.RENDER);
        pending("motion_blur", "Motion Blur", "Aplica desfoque de movimento.", Category.RENDER);
        pending("kush_nametag", "Kush Nametag", "Nametag personalizado do KushClient.", Category.RENDER);
        pending("kush_skins_capes", "Kush Skins & Capes", "Integra skins e capas do KushClient.", Category.RENDER);
        pending("auto_sprint", "Auto Sprint", "Mantém sprint automaticamente.", Category.MOVEMENT);
        pending("cosmetics", "Cosmetics", "Cosméticos renderizados dentro do jogo.", Category.PLAYER);
        pending("toggle_sneak", "Toggle Sneak", "Alterna agachar com um toque.", Category.PLAYER);
        pending("zoom", "Zoom", "Zoom suave configurável.", Category.UTILITY);
        pending("auto_gg", "Auto GG", "Mensagem automática configurável.", Category.UTILITY);
        pending("quick_messages", "Quick Messages", "Atalhos para mensagens.", Category.UTILITY);
        pending("notifications", "Notifications", "Notificações visuais do KushClient.", Category.UTILITY);
        KushClientClient.LOGGER.info("Kush Mods registrados: {}", modules.size());
    }

    private void pending(String id, String name, String description, Category category) {
        register(new PlaceholderModule(id, name, description, category));
    }

    public void register(Module module) {
        if (modules.containsKey(module.getId())) throw new IllegalArgumentException("Módulo duplicado: " + module.getId());
        modules.put(module.getId(), module);
    }

    public Optional<Module> find(String id) { return Optional.ofNullable(modules.get(id)); }
    public List<Module> getModules() { return Collections.unmodifiableList(new ArrayList<>(modules.values())); }
    public List<Module> getModules(Category category) { return modules.values().stream().filter(module -> module.getCategory() == category).toList(); }

    public void onClientTick(Minecraft client) {
        for (Module module : modules.values()) if (module.isEnabled()) module.onClientTick(client);
        if (configManager != null) configManager.flushPendingSave();
    }

    public void onHudRender(Minecraft client, GuiGraphics graphics) {
        for (Module module : modules.values()) if (module.isEnabled()) module.onHudRender(client, graphics);
    }

    public void setConfigManager(ConfigManager configManager) { this.configManager = configManager; }
}
