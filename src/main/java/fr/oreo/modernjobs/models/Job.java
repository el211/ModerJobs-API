package fr.oreo.modernjobs.models;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class Job {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final String icon;
    private final int customModelData;
    private final String color;

    private final int maxLevel;
    private final String formulaType;
    private final double formulaBase;
    private final double formulaMultiplier;

    private final int maxPlayers;
    private final String requiredPermission;

    private final List<String> worldWhitelist;
    private final List<String> worldBlacklist;

    private final Map<String, ActionEntry> blockBreak;
    private final Map<String, ActionEntry> blockPlace;
    private final Map<String, ActionEntry> mobKill;
    private final Map<String, ActionEntry> fishing;
    private final Map<String, ActionEntry> crafting;
    private final Map<String, ActionEntry> smelting;
    private final Map<String, ActionEntry> farming;

    private final Map<String, ActionEntry> iaBlockBreak;
    private final Map<String, ActionEntry> iaFurniture;

    private final Map<String, ActionEntry> nexoBlockBreak;
    private final Map<String, ActionEntry> nexoFurniture;


    private final Map<Integer, LevelReward> levelRewards;


    private final Map<Integer, Map<Integer, LevelReward>> rewardsByPrestige = new HashMap<>();
    private final LevelUpEffect defaultLevelUpEffect;
    private final Map<Integer, LevelUpEffect> levelUpEffects;

    private final Map<String, Long> cooldowns;
    private final int guiSlot;
    private final Map<Integer, JobPerk> perks;

    public Job(String id, FileConfiguration config) {
        this.id = id.toLowerCase();
        this.displayName = config.getString("display-name", id);
        this.description = config.getStringList("description");
        this.icon = config.getString("icon", "STONE").toUpperCase();
        this.customModelData = config.getInt("custom-model-data", -1);
        this.color = config.getString("color", "<white>");
        this.maxLevel = config.getInt("max-level", 100);
        this.maxPlayers = config.getInt("max-players", -1);
        this.requiredPermission = config.getString("required-permission", "");

        ConfigurationSection formula = config.getConfigurationSection("xp-formula");
        if (formula != null) {
            this.formulaType = formula.getString("type", "EXPONENTIAL").toUpperCase();
            this.formulaBase = formula.getDouble("base", 100.0);
            this.formulaMultiplier = formula.getDouble("multiplier", 1.5);
        } else {
            this.formulaType = "EXPONENTIAL";
            this.formulaBase = 100.0;
            this.formulaMultiplier = 1.5;
        }

        this.worldWhitelist = config.getStringList("worlds.whitelist");
        this.worldBlacklist = config.getStringList("worlds.blacklist");

        ConfigurationSection acts = config.getConfigurationSection("actions");
        this.blockBreak = loadActions(acts == null ? null : acts.getConfigurationSection("block-break"));
        this.blockPlace = loadActions(acts == null ? null : acts.getConfigurationSection("block-place"));
        this.mobKill = loadActions(acts == null ? null : acts.getConfigurationSection("mob-kill"));
        this.fishing = loadActions(acts == null ? null : acts.getConfigurationSection("fishing"));
        this.crafting = loadActions(acts == null ? null : acts.getConfigurationSection("crafting"));
        this.smelting = loadActions(acts == null ? null : acts.getConfigurationSection("smelting"));
        this.farming = loadActions(acts == null ? null : acts.getConfigurationSection("farming"));

        this.iaBlockBreak = loadActionsLower(acts == null ? null : acts.getConfigurationSection("ia-block-break"));
        this.iaFurniture = loadActionsLower(acts == null ? null : acts.getConfigurationSection("ia-furniture"));

        this.nexoBlockBreak = loadActionsLower(acts == null ? null : acts.getConfigurationSection("nexo-block-break"));
        this.nexoFurniture = loadActionsLower(acts == null ? null : acts.getConfigurationSection("nexo-furniture"));

        loadRewards(config);

        this.levelRewards = new LinkedHashMap<>(getRewardsForPrestige(0));
        this.defaultLevelUpEffect = loadDefaultLevelUpEffect(config.getConfigurationSection("level-up-effects"));
        this.levelUpEffects = loadLevelUpEffects(config.getConfigurationSection("level-up-effects"), defaultLevelUpEffect);

        this.guiSlot = config.getInt("gui-slot", -1);

        this.perks = new LinkedHashMap<>();
        ConfigurationSection perksSec = config.getConfigurationSection("perks");
        if (perksSec != null) {
            for (String key : perksSec.getKeys(false)) {
                int perkLevel;
                try { perkLevel = Integer.parseInt(key); } catch (NumberFormatException ignored) { continue; }
                ConfigurationSection perkSec = perksSec.getConfigurationSection(key);
                if (perkSec != null) perks.put(perkLevel, JobPerk.fromSection(perkLevel, perkSec));
            }
        }

        this.cooldowns = new HashMap<>();
        ConfigurationSection cdSec = config.getConfigurationSection("cooldowns");
        if (cdSec != null) {
            for (String key : cdSec.getKeys(false)) {
                cooldowns.put(key.toLowerCase(), cdSec.getLong(key, 0L) * 1000L);
            }
        }
    }

    private void loadRewards(FileConfiguration config) {
        ConfigurationSection rewardsRoot = config.getConfigurationSection("rewards");
        if (rewardsRoot != null) {
            boolean flatBaseRewards = rewardsRoot.getKeys(false).stream().allMatch(key -> {
                ConfigurationSection section = rewardsRoot.getConfigurationSection(key);
                return section != null && isRewardSection(section);
            });

            if (flatBaseRewards) {
                Map<Integer, LevelReward> base = new LinkedHashMap<>();
                for (String levelKey : rewardsRoot.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(levelKey);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }

                    ConfigurationSection levelSec = rewardsRoot.getConfigurationSection(levelKey);
                    if (levelSec == null) continue;
                    base.put(level, LevelReward.fromSection(level, levelSec));
                }
                rewardsByPrestige.put(0, base);
            } else {
            for (String prestigeKey : rewardsRoot.getKeys(false)) {
                int prestige;
                try {
                    prestige = Integer.parseInt(prestigeKey);
                } catch (NumberFormatException ignored) {
                    continue;
                }

                ConfigurationSection prestigeSec = rewardsRoot.getConfigurationSection(prestigeKey);
                if (prestigeSec == null) continue;

                Map<Integer, LevelReward> map = new LinkedHashMap<>();
                for (String levelKey : prestigeSec.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(levelKey);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }

                    ConfigurationSection levelSec = prestigeSec.getConfigurationSection(levelKey);
                    if (levelSec == null) continue;

                    map.put(level, LevelReward.fromSection(level, levelSec));
                }

                rewardsByPrestige.put(prestige, map);
            }
            }
        }

        ConfigurationSection legacy = config.getConfigurationSection("level-rewards");
        if (legacy != null) {
            Map<Integer, LevelReward> base = new LinkedHashMap<>(rewardsByPrestige.getOrDefault(0, new LinkedHashMap<>()));
            for (String key : legacy.getKeys(false)) {
                int lvl;
                try {
                    lvl = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                ConfigurationSection lvlSec = legacy.getConfigurationSection(key);
                if (lvlSec == null) continue;
                base.put(lvl, LevelReward.fromSection(lvl, lvlSec));
            }
            rewardsByPrestige.put(0, base);
        }
    }

    private boolean isRewardSection(ConfigurationSection section) {
        return section.contains("money")
                || section.contains("commands")
                || section.contains("items")
                || section.contains("permissions")
                || section.contains("firework")
                || section.contains("sound")
                || section.contains("sound-volume")
                || section.contains("sound-pitch");
    }

    private static Map<String, ActionEntry> loadActions(ConfigurationSection section) {
        Map<String, ActionEntry> map = new LinkedHashMap<>();
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            String upper = key.toUpperCase();
            if (section.isConfigurationSection(key)) {
                ConfigurationSection e = section.getConfigurationSection(key);
                double xp = e.getDouble("xp", 1.0);
                double money = e.getDouble("money", 0.0);
                double chance = e.getDouble("chance", 1.0);
                int lvlReq = e.getInt("level-requirement", 0);
                int maxLvlReq = e.getInt("max-level-requirement", -1);
                map.put(upper, new ActionEntry(upper, xp, money, chance, lvlReq, maxLvlReq));
            } else {
                map.put(upper, new ActionEntry(upper, section.getDouble(key, 1.0), 0.0, 1.0, 0, -1));
            }
        }
        return map;
    }

    private static Map<String, ActionEntry> loadActionsLower(ConfigurationSection section) {
        Map<String, ActionEntry> map = new LinkedHashMap<>();
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            String lower = key.toLowerCase();
            if (section.isConfigurationSection(key)) {
                ConfigurationSection e = section.getConfigurationSection(key);
                double xp = e.getDouble("xp", 1.0);
                double money = e.getDouble("money", 0.0);
                double chance = e.getDouble("chance", 1.0);
                int lvlReq = e.getInt("level-requirement", 0);
                int maxLvlReq = e.getInt("max-level-requirement", -1);
                map.put(lower, new ActionEntry(lower, xp, money, chance, lvlReq, maxLvlReq));
            } else {
                map.put(lower, new ActionEntry(lower, section.getDouble(key, 1.0), 0.0, 1.0, 0, -1));
            }
        }
        return map;
    }

    private static LevelUpEffect loadDefaultLevelUpEffect(ConfigurationSection section) {
        if (section == null) {
            return LevelUpEffect.empty();
        }
        ConfigurationSection defaults = section.getConfigurationSection("default");
        if (defaults != null) {
            return LevelUpEffect.fromSection(defaults, LevelUpEffect.empty());
        }
        return LevelUpEffect.fromSection(section, LevelUpEffect.empty());
    }

    private static Map<Integer, LevelUpEffect> loadLevelUpEffects(ConfigurationSection section, LevelUpEffect fallback) {
        Map<Integer, LevelUpEffect> effects = new HashMap<>();
        if (section == null) {
            return effects;
        }

        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) {
            return effects;
        }

        for (String key : levels.getKeys(false)) {
            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }

            ConfigurationSection levelSection = levels.getConfigurationSection(key);
            if (levelSection == null) {
                continue;
            }
            effects.put(level, LevelUpEffect.fromSection(levelSection, fallback));
        }
        return effects;
    }

    public Optional<ActionEntry> getBlockBreakEntry(String material) {
        return Optional.ofNullable(blockBreak.get(material.toUpperCase()));
    }

    public Optional<ActionEntry> getBlockPlaceEntry(String material) {
        return Optional.ofNullable(blockPlace.get(material.toUpperCase()));
    }

    public Optional<ActionEntry> getMobKillEntry(String entityType) {
        return Optional.ofNullable(mobKill.get(entityType.toUpperCase()));
    }

    public Optional<ActionEntry> getFishingEntry(String key) {
        return Optional.ofNullable(fishing.get(key.toUpperCase()));
    }

    public Optional<ActionEntry> getCraftingEntry(String material) {
        return Optional.ofNullable(crafting.get(material.toUpperCase()));
    }

    public Optional<ActionEntry> getSmeltingEntry(String material) {
        return Optional.ofNullable(smelting.get(material.toUpperCase()));
    }

    public Optional<ActionEntry> getFarmingEntry(String material) {
        return Optional.ofNullable(farming.get(material.toUpperCase()));
    }

    public Optional<ActionEntry> getIaBlockBreakEntry(String namespacedId) {
        return Optional.ofNullable(iaBlockBreak.get(namespacedId.toLowerCase()));
    }

    public Optional<ActionEntry> getIaFurnitureEntry(String namespacedId) {
        return Optional.ofNullable(iaFurniture.get(namespacedId.toLowerCase()));
    }

    public Optional<ActionEntry> getNexoBlockBreakEntry(String itemId) {
        return Optional.ofNullable(nexoBlockBreak.get(itemId.toLowerCase()));
    }

    public Optional<ActionEntry> getNexoFurnitureEntry(String itemId) {
        return Optional.ofNullable(nexoFurniture.get(itemId.toLowerCase()));
    }

    public Optional<LevelReward> getRewardForLevel(int level) {
        return Optional.ofNullable(levelRewards.get(level));
    }

    public Optional<LevelReward> getRewardForLevel(int prestige, int level) {
        return Optional.ofNullable(getRewardsForPrestige(prestige).get(level));
    }

    public boolean isWorldAllowed(String worldName) {
        if (!worldWhitelist.isEmpty() && !worldWhitelist.contains(worldName)) return false;
        return !worldBlacklist.contains(worldName);
    }

    public long getCooldown(String actionType) {
        return cooldowns.getOrDefault(actionType.toLowerCase(), 0L);
    }

    public Map<Integer, LevelReward> getRewardsForPrestige(int prestige) {
        return rewardsByPrestige.getOrDefault(prestige, Collections.emptyMap());
    }

    public LevelUpEffect getLevelUpEffect(int level) {
        return levelUpEffects.getOrDefault(level, defaultLevelUpEffect);
    }

    public void setRewardsForPrestige(int prestige, Map<Integer, LevelReward> rewards) {
        rewardsByPrestige.put(prestige, new LinkedHashMap<>(rewards));
    }

    /** Returns the fixed GUI slot for this job, or -1 if unset (use default order). */
    public int getGuiSlot() { return guiSlot; }
    public Map<Integer, JobPerk> getPerks() { return Collections.unmodifiableMap(perks); }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getDescription() { return description; }
    public String getIcon() { return icon; }
    public int getCustomModelData() { return customModelData; }
    public String getColor() { return color; }
    public int getMaxLevel() { return maxLevel; }
    public String getFormulaType() { return formulaType; }
    public double getFormulaBase() { return formulaBase; }
    public double getFormulaMultiplier() { return formulaMultiplier; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getRequiredPermission() { return requiredPermission; }
    public List<String> getWorldWhitelist() { return worldWhitelist; }
    public List<String> getWorldBlacklist() { return worldBlacklist; }
    public Map<String, ActionEntry> getBlockBreak() { return Collections.unmodifiableMap(blockBreak); }
    public Map<String, ActionEntry> getBlockPlace() { return Collections.unmodifiableMap(blockPlace); }
    public Map<String, ActionEntry> getMobKill() { return Collections.unmodifiableMap(mobKill); }
    public Map<String, ActionEntry> getFishing() { return Collections.unmodifiableMap(fishing); }
    public Map<String, ActionEntry> getCrafting() { return Collections.unmodifiableMap(crafting); }
    public Map<String, ActionEntry> getSmelting() { return Collections.unmodifiableMap(smelting); }
    public Map<String, ActionEntry> getFarming() { return Collections.unmodifiableMap(farming); }
    public Map<String, ActionEntry> getIaBlockBreak() { return Collections.unmodifiableMap(iaBlockBreak); }
    public Map<String, ActionEntry> getIaFurniture() { return Collections.unmodifiableMap(iaFurniture); }
    public Map<String, ActionEntry> getNexoBlockBreak() { return Collections.unmodifiableMap(nexoBlockBreak); }
    public Map<String, ActionEntry> getNexoFurniture() { return Collections.unmodifiableMap(nexoFurniture); }


    public Map<Integer, LevelReward> getLevelRewards() {
        return Collections.unmodifiableMap(levelRewards);
    }
}
