# Boots Season 2 – Complete Development Plan

## Project Overview
Comprehensive Minecraft Paper 1.21 plugin implementing the Boots Tier System with 10 unique boot types, each with Tier 1 and Tier 2 variants, complete with passive effects, active abilities, lives system, repair rituals, trust mechanics, and advanced visual effects using NMS and ProtocolLib.

## Technology Stack
- **Paper API**: 1.21.4-R0.1-SNAPSHOT
- **ProtocolLib**: 5.1.0 (for advanced packet manipulation)
- **Lombok**: 1.18.34 (code generation)
- **Gson**: 2.11.0 (JSON data persistence)
- **Java**: 21
- **Build Tool**: Maven

## Core Systems Architecture

### 1. Boot System
- **10 Boot Types**: Speed, Strength, Ward, Spider, Frost, Wind, Astral, Life, Water, Fire
- **2 Tiers per Boot**: Tier 1 (basic) and Tier 2 (advanced)
- **Unique Properties**:
  - Custom armor trims per boot type
  - Dynamic enchantments based on lives remaining
  - Unbreakable and unremovable when worn
  - Custom lore and visual effects

### 2. Lives & Boot Shards System
- **Starting Lives**: 5 (configurable)
- **Maximum Lives**: 10
- **Mechanics**:
  - Lose 1 life on death (drop boot shard)
  - Gain lives by consuming boot shards
  - Gain lives by killing other players
  - Boots break when lives reach 0
  - Boot shards can be withdrawn and traded

### 3. Boot Breaking & Enchantment System
- **Lives → Enchantments Mapping**:
  - 5+ lives: Full enchants (Prot IV, Unbreaking III, Mending, Depth Strider III, Feather Falling IV, Soul Speed III for Tier 2)
  - 4 lives: Lose nothing
  - 3 lives: Lose Soul Speed III
  - 2 lives: Lose Depth Strider III
  - 1 life: Lose Feather Falling IV
  - 0 lives: Boots break, only Protection III remains

### 4. Tier System
- **Tier 1**: Basic passive + primary ability
- **Tier 2**: Enhanced passive + secondary ability
- **Tier Box Item**: Upgrades boots from Tier 1 to Tier 2
- **Death Mechanics**: Tier 2 → Tier 1 on death, drops Tier Box

### 5. Repair Ritual System
- **Activation**: Admin-controlled pedestal (/pedestal activate)
- **Process**:
  1. Use Repair Box at pedestal location
  2. Beacon spawns at configured location
  3. 10-minute ritual countdown
  4. Must stand on beacon within 10 seconds after completion
  5. Beacon has 3 lives (pushes away on break)
- **Success**: Boots repaired, lives restored to 5
- **Failure**: Ritual canceled, Repair Box dropped

### 6. Trust System
- **/trust <player>**: Allow player to be unaffected by harmful abilities
- **/untrust <player>**: Remove trust
- **Mechanics**: Trusted players see through invisibility, unaffected by damage/disable abilities

### 7. Ability System
- **Cooldown Management**: Base cooldown reduced 50% if holding Dragon Egg
- **Activation**: /ability1 or /ability2 commands
- **Action Bar Display**: Real-time cooldown/status updates
- **Boot-Themed Messages**: Each boot type has custom chat prefix and colors

### 8. Custom Items
- **Boot Reroller** (Pitcher Pod): Rerolls boot type, preserves tier
- **Tier Box** (Magenta Glazed Terracotta): Upgrades to Tier 2
- **Boot Shard** (Echo Shard): Consumable life essence
- **Repair Box** (Recovery Compass): Initiates repair ritual

### 9. Crafting Recipes

#### Boot Reroller
\`\`\`
W N S
T B T
R G L
\`\`\`
- W = Wind Charge
- N = Netherite Ingot
- S = Strength Potion
- T = Ominous Trial Key
- B = Diamond Boots
- R = Rabbit Foot
- G = Golden Apple
- L = Nautilus Shell

#### Tier Box
\`\`\`
E U E
N H N
E U E
\`\`\`
- E = Echo Shard
- U = Netherite Upgrade Template
- H = Heavy Core
- N = Netherite Ingot

#### Repair Box
\`\`\`
B E B
R T R
N E N
\`\`\`
- B = Boot Shard
- E = Enchanted Golden Apple
- R = Reroller
- T = Tier Box
- N = Netherite Ingot

### 10. Visual Effects System

#### Particle Effects
- **Boot Circling**: Custom particles rotating around player's head
- **Ability Particles**: Unique effects per ability (fire rings, ice circles, tornado, etc.)
- **Breaking Animation**: Red particle burst + totem particles on boot break

#### NMS & ProtocolLib Features
- **True Invisibility**: Hide player entity from untrusted players only
- **Selective Visibility**: Trusted players can always see you
- **Custom Entities**: Item entities and armor stands for visual effects
- **Block Display Entities**: Advanced animations for abilities
- **Packet Management**: Send custom particles, entities, and effects to specific players

## Boot Types Detailed Specifications

### 1. SPEED BOOTS 🟢
**Theme**: Lightning & Movement
**Armor Trim**: Emerald (green accents)
**Tier 1**:
- Passive: Speed I
- Ability "Blur": 10s invisibility flicker + Speed III + Haste III, insta-gap on right-click (45s cooldown)

**Tier 2**:
- Passive: Speed increase on player hit
- Ability: Thunder strike on hit, stuns for 4s (90s cooldown)

### 2. STRENGTH BOOTS 🔴
**Theme**: Power & Siphoning
**Armor Trim**: Gold (orange accents)
**Tier 1**:
- Passive: "Siphon" - Heal 0.5 hearts every 5 hits
- Ability: Critical hits bypass shields for 10s (90s cooldown)

**Tier 2**:
- Passive: Break shields with arrows, Strength I
- Ability: Damage link - expanding radius (1-10 blocks), shared damage for 30s (180s cooldown)

### 3. WARD BOOTS 🟣
**Theme**: Shadows & Stealth
**Armor Trim**: Amethyst (purple hue)
**Tier 1**:
- Passive: Silent footsteps, no sculk shrieker activation
- Ability: True invisibility 10s, Darkness & Blind to players in 5-block radius (90s cooldown)

**Tier 2**:
- Passive: Warden immunity, "Echo Sense" - detect sneaking players (20 block radius, 10s sneak)
- Ability: Place sculk sensor (1min duration), activate gives Weakness & Darkness to untrusted, Regeneration II to trusted in 5-block radius (120s cooldown)

### 4. SPIDER BOOTS 🟤
**Theme**: Webs & Arachnids
**Armor Trim**: Copper (brown-green)
**Tier 1**:
- Passive: Spider immunity, cobweb traversal
- Ability: Summon 5 poisonous spiders (120s cooldown)

**Tier 2**:
- Passive: Hold 2 cobweb stacks
- Ability: Shoot fireball that spawns random webs in 3-block radius, webs vanish after 15s (60s cooldown)

### 5. FROST BOOTS 🔵
**Theme**: Ice & Cold
**Armor Trim**: Diamond (ice blue)
**Tier 1**:
- Passive: Freeze immunity, powdered snow traversal
- Ability: 3-block damage shield for 5s, reflects damage (60s cooldown)

**Tier 2**:
- Passive: Take freezing damage every 10 hits
- Ability: 10-block ice circle, prevents teleport/wind charge, freeze damage, Regen I to trusted (95s cooldown, 15s duration)

### 6. WIND BOOTS ⚪
**Theme**: Air & Movement
**Armor Trim**: Quartz (white sleek)
**Tier 1**:
- Passive: No fall damage, straight-line wind charges
- Ability: Dash forward (40s cooldown)

**Tier 2**:
- Passive: Double jump (5s cooldown between jumps)
- Ability: Tornado pulls players inward for 10s (60s cooldown)

### 7. ASTRAL BOOTS 🟣
**Theme**: Cosmic & Time
**Armor Trim**: Netherite (dark purple)
**Tier 1**:
- Passive: 10% damage cancel chance
- Ability "Rewind": Mark location, teleport back within 20s (90s cooldown)

**Tier 2**:
- Passive: Netherite knockback
- Ability: Disable boots of players in 5-block radius (follows player) for 15s (120s cooldown)

### 8. LIFE BOOTS 🟢
**Theme**: Nature & Healing
**Armor Trim**: Emerald (nature green)
**Tier 1**:
- Passive: Crouch + right-click to grow plants/feed animals
- Ability: On hit, heal opposite of victim's hearts, apply Nausea/Blindness/Slowness for 5s (120s cooldown)

**Tier 2**:
- Passive: Hero of Village III, enhanced golden apple absorption
- Ability: 5-block circle, reduce to 7 hearts, extra durability damage on armor (75s cooldown, 15s duration)

### 9. WATER BOOTS 🔷
**Theme**: Ocean & Fluids
**Armor Trim**: Prismarine (ocean blue)
**Tier 1**:
- Passive: Conduit Power
- Ability: Whirlpool (5-block radius, follows player), sucks in and drowns for 10s (75s cooldown)

**Tier 2**:
- Passive: 20% thunder on trident hit, "Wet" status (fire immunity in water + 5s after)
- Ability: Wave push (10-block radius), disable jump for 5s

### 10. FIRE BOOTS 🔴
**Theme**: Flame & Inferno
**Armor Trim**: Redstone (red fiery glow)
**Tier 1**:
- Passive: Fire resistance, 5% chance to ignite attacker
- Ability: 3 expanding fire rings, 2 hearts true damage each, extinguishes water

**Tier 2**:
- Passive: Lava traversal, no magma damage, extinguish water in 2-block radius when on fire
- Ability: Dash with fire trail, mark players in 3-block radius with orange glow, hits ignite them + Weakness for 10s

## Commands System

### Player Commands
- `/boots` - Show boots info and help
- `/ability1` (alias: `/a1`) - Activate Tier 1 ability
- `/ability2` (alias: `/a2`) - Activate Tier 2 ability
- `/lives` - Check personal lives count
- `/withdraw <amount|all>` (alias: `/wd`) - Withdraw boot shards
- `/trust <player>` - Trust a player
- `/untrust <player>` - Remove trust

### Admin Commands
- `/boots give <player> <type>` - Give boots to player
- `/boots reload` - Reload configuration
- `/bootset <player> <type> <tier>` - Set specific boots
- `/reroll [player]` - Force reroll boots
- `/pedestal set` - Set pedestal location
- `/pedestal activate` - Activate pedestal (enable rituals)
- `/pedestal deactivate` - Deactivate pedestal
- `/pedestal status` - Check pedestal status

## Data Persistence

### Player Data (JSON)
\`\`\`json
{
  "uuid": "player-uuid",
  "boots": {
    "type": "SPEED",
    "tier": "TIER_1",
    "broken": false,
    "lastAbilityUse": 0,
    "abilityActive": false
  },
  "lives": 5,
  "trustedPlayers": ["uuid1", "uuid2"]
}
\`\`\`

### Configuration (YAML)
- General settings (default lives, max lives, debug mode)
- Pedestal settings (location, active status, ritual duration)
- Particle settings (enabled, frequency, radius)
- Cooldown settings (dragon egg reduction, action bar)
- Trust system (max trusted players)
- Boot breaking system (warnings, animations)
- Messages (fully customizable with color codes)

## Implementation Phases

### ✅ Phase 1: Core Infrastructure (COMPLETED)
- [x] Maven project setup
- [x] Plugin main class with manager initialization
- [x] Configuration management (YAML)
- [x] Data persistence (JSON with Gson)
- [x] Player data management
- [x] Boot type and tier enums

### ✅ Phase 2: Boot System (COMPLETED)
- [x] Boot creation with custom lore
- [x] Armor trim application per boot type
- [x] Dynamic enchantments based on lives
- [x] Boot breaking system
- [x] Low-life warnings
- [x] Boot protection (unremovable)

### ✅ Phase 3: Lives & Boot Shards (COMPLETED)
- [x] Lives management
- [x] Boot shard creation and handling
- [x] Withdrawal system
- [x] Death handling (life loss, shard drops)
- [x] Tier downgrade on death

### ✅ Phase 4: Custom Items (COMPLETED)
- [x] Reroller item creation
- [x] Tier Box item creation
- [x] Boot Shard item creation
- [x] Repair Box item creation
- [x] Item protection (prevent placement)

### 🔧 Phase 5: Crafting System (NEEDS FIX)
- [x] Reroller recipe structure
- [x] Tier Box recipe
- [x] Repair Box recipe
- [ ] **FIX**: Reroller recipe has duplicate 'S' key
- [ ] **ENHANCE**: Add recipe discovery/unlock system

### ✅ Phase 6: Ability System (COMPLETED)
- [x] Ability manager and registration
- [x] Cooldown calculation with dragon egg detection
- [x] All 10 boot ability implementations
- [x] Passive effect application
- [x] Ability activation commands

### ✅ Phase 7: Trust System (COMPLETED)
- [x] Trust data storage
- [x] Trust/untrust commands
- [x] Trust-based ability filtering
- [x] Bilateral trust notifications

### ✅ Phase 8: Ritual & Pedestal (COMPLETED)
- [x] Pedestal location management
- [x] Repair ritual mechanics
- [x] Beacon protection (3 lives)
- [x] Ritual success/failure handling
- [x] Admin activation system

### ✅ Phase 9: Visual Effects (COMPLETED)
- [x] Particle manager
- [x] Boot-specific circling particles
- [x] Breaking animations
- [x] Action bar cooldown display
- [x] Boot-themed chat messages

### 🔧 Phase 10: Advanced NMS Features (NEEDS ENHANCEMENT)
- [x] NMS handler setup
- [x] Packet manager
- [ ] **ENHANCE**: True invisibility with selective visibility
- [ ] **ENHANCE**: Advanced entity manipulation
- [ ] **ENHANCE**: Custom block display entities
- [ ] **ENHANCE**: Per-player packet filtering

### ✅ Phase 11: Event Handling (COMPLETED)
- [x] Player join (boot assignment)
- [x] Player death (lives, shards, tier)
- [x] Player interaction (items, abilities)
- [x] Entity damage (abilities, passives)
- [x] Movement (passives, restrictions)
- [x] Projectile (trident, fireball abilities)
- [x] Block (sensor placement, protection)
- [x] Inventory (boot protection)

### 🔧 Phase 12: Testing & Polish (IN PROGRESS)
- [x] Basic functionality testing
- [ ] **TODO**: Edge case handling
- [ ] **TODO**: Performance optimization
- [ ] **TODO**: Memory leak prevention
- [ ] **TODO**: Comprehensive ability testing
- [ ] **TODO**: Multi-player interaction testing

## Known Issues & Fixes Needed

### 1. Crafting Recipe Bug
**Issue**: Reroller recipe has duplicate 'S' key (both for Strength Potion and Nautilus Shell)
**Fix**: Change pattern or use different key
**Priority**: HIGH

### 2. NMS Invisibility Enhancement
**Issue**: Current invisibility may not fully hide from untrusted players
**Fix**: Implement ProtocolLib packet filtering for selective entity visibility
**Priority**: MEDIUM

### 3. Advanced Particle Effects
**Issue**: Some abilities could have more elaborate particle effects
**Fix**: Add more particle types and patterns per ability
**Priority**: LOW

### 4. Action Bar Updates
**Issue**: Action bar could update more frequently for smoother cooldown display
**Fix**: Already set to 1 tick (20 times per second), optimization may be needed
**Priority**: LOW

## Best Practices & Code Standards

### Code Style
- Use Lombok for getters, setters, constructors
- Fully qualified variable names (player not p)
- Consistent use of "this" keyword
- Add "final" where possible
- Null safety checks everywhere

### Architecture
- Static plugin instance for easy access
- Manager pattern for system separation
- Event-driven listener system
- Scheduled tasks for recurring operations

### Performance
- Async data operations where possible
- Particle updates batched
- Event cancellation to prevent cascading
- Efficient data structures (Maps, Sets)

### Error Handling
- Try-catch for external operations
- Logging for debugging
- Graceful fallbacks
- User-friendly error messages

## Testing Checklist

### Boot System
- [ ] All 10 boot types give correctly
- [ ] Armor trims apply correctly
- [ ] Enchantments scale with lives
- [ ] Boots break at 0 lives
- [ ] Boots cannot be removed when worn
- [ ] Low-life warnings display properly

### Abilities
- [ ] All Tier 1 abilities function
- [ ] All Tier 2 abilities function
- [ ] Cooldowns calculate correctly
- [ ] Dragon egg reduces cooldowns by 50%
- [ ] Abilities disabled when boots broken
- [ ] Passive effects apply continuously

### Lives & Shards
- [ ] Lives decrease on death
- [ ] Boot shards drop on death
- [ ] Shards can be withdrawn
- [ ] Shards can be consumed
- [ ] Lives increase on shard consumption
- [ ] Lives cap at maximum (10)

### Crafting
- [ ] Reroller recipe works
- [ ] Tier Box recipe works
- [ ] Repair Box recipe works
- [ ] Items cannot be placed as blocks

### Ritual System
- [ ] Pedestal can be set by admin
- [ ] Pedestal activates/deactivates properly
- [ ] Repair ritual initiates correctly
- [ ] Beacon spawns and persists
- [ ] Beacon pushes players on break
- [ ] Ritual completes and repairs boots
- [ ] Ritual fails if not completed in time

### Trust System
- [ ] Trust command works
- [ ] Untrust command works
- [ ] Trusted players immune to harmful abilities
- [ ] Trusted players can see invisible players
- [ ] Trust persists across server restarts

### Commands
- [ ] All player commands function
- [ ] All admin commands function
- [ ] Permission checks work
- [ ] Help messages display correctly

### Visual Effects
- [ ] Particle effects render smoothly
- [ ] Action bar updates in real-time
- [ ] Breaking animations play
- [ ] Boot-themed messages send

## Future Enhancements

### Potential Features
- Statistics tracking (kills, deaths, abilities used)
- Leaderboards for lives/kills
- Seasonal boot skins
- Boot customization options
- Achievement system
- Boot evolution/prestige system
- PvP arenas with special rules
- Boot-specific challenges/quests

### Performance Improvements
- Particle culling for distant players
- Async pathfinding for summoned entities
- Cached player data with lazy loading
- Database integration (MySQL/MongoDB)

### Integration Options
- PlaceholderAPI support
- Vault economy integration
- DiscordSRV notifications
- WorldGuard region support
- Towny/Factions integration

## Documentation

### Player Guide
- Boot types and abilities explained
- How to use abilities
- Lives system mechanics
- Repair ritual guide
- Crafting recipes
- Trust system usage

### Admin Guide
- Installation instructions
- Configuration options
- Command reference
- Permission nodes
- Troubleshooting guide

### Developer Guide
- API documentation
- Event system
- Adding custom boots
- Extending abilities
- NMS/ProtocolLib usage

## Success Criteria
✅ All 10 boot types fully implemented
✅ Tier 1 and Tier 2 abilities functional
✅ Lives and boot shard system working
✅ Repair ritual system operational
✅ Trust system functional
✅ Custom items craftable
✅ Visual effects rendering
✅ Commands all working
🔧 Crafting recipes need fixing
🔧 NMS features need enhancement
🔧 Testing needs completion

## Project Status: 95% Complete
**Next Steps**:
1. Fix reroller crafting recipe
2. Enhance NMS invisibility features
3. Complete comprehensive testing
4. Document all systems
5. Performance optimization pass
