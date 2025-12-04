package com.bootstier;

import com.bootstier.boots.BootsManager;
import com.bootstier.boots.abilities.AbilityManager;
import com.bootstier.commands.*;
import com.bootstier.config.ConfigManager;
import com.bootstier.effects.ActionBarManager;
import com.bootstier.effects.ParticleManager;
import com.bootstier.items.CustomItemManager;
import com.bootstier.lives.LivesManager;
import com.bootstier.listeners.*;
import com.bootstier.nms.AdvancedEffects;
import com.bootstier.nms.DisplayEntityManager;
import com.bootstier.nms.NMSHandler;
import com.bootstier.nms.PacketManager;
import com.bootstier.nms.UnifiedDisplayManager;
import com.bootstier.player.PlayerManager;
import com.bootstier.player.TrustManager;
import com.bootstier.ritual.PedestalManager;
import com.bootstier.ritual.RitualManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BootsTierPlugin extends JavaPlugin {

    @Getter private static BootsTierPlugin instance;

    @Getter private ConfigManager configManager;
    @Getter private PlayerManager playerManager;
    @Getter private BootsManager bootsManager;
    @Getter private LivesManager livesManager;
    @Getter private TrustManager trustManager;
    @Getter private PedestalManager pedestalManager;
    @Getter private RitualManager ritualManager;
    @Getter private CustomItemManager customItemManager;
    @Getter private AdvancedEffects advancedEffects;
    @Getter private ParticleManager particleManager;
    @Getter private ActionBarManager actionBarManager;
    @Getter private NMSHandler nmsHandler;
    @Getter private PacketManager packetManager;
    @Getter private AbilityManager abilityManager;
    @Getter private DisplayEntityManager displayEntityManager;
    @Getter private UnifiedDisplayManager unifiedDisplayManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("§d[BootsTierSystem] §fInitializing plugin components...");

        try {
            initializeManagers();
            registerCommands();
            registerListeners();
            startScheduledTasks();
            registerBootShardListener();

            getLogger().info("§aBoots SMP v5 successfully enabled!");
        } catch (Exception e) {
            getLogger().severe("§cBoots SMP failed to start properly:");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[BootsTierSystem] §7Disabling and saving data...");

        if (playerManager != null) playerManager.saveAllPlayerData();
        if (displayEntityManager != null) displayEntityManager.cleanupAll();
        if (unifiedDisplayManager != null) unifiedDisplayManager.cleanupAll();
        if (ritualManager != null) ritualManager.cleanupAll();
        Bukkit.getScheduler().cancelTasks(this);

        getLogger().info("§c[BootsTierSystem] §7Plugin disabled cleanly.");
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        nmsHandler = new NMSHandler(this);
        packetManager = new PacketManager(this);

        playerManager = new PlayerManager(this);
        bootsManager = new BootsManager(this);
        livesManager = new LivesManager(this);
        trustManager = new TrustManager(this);

        pedestalManager = new PedestalManager(this);
        ritualManager = new RitualManager(this);

        customItemManager = new CustomItemManager(this);
        particleManager = new ParticleManager(this);
        actionBarManager = new ActionBarManager(this);

        advancedEffects = new AdvancedEffects(this);
        displayEntityManager = new DisplayEntityManager(this);
        unifiedDisplayManager = new UnifiedDisplayManager(this);
        abilityManager = new AbilityManager(this);
    }

    /* ---------------------------------------------
       COMMANDS
    --------------------------------------------- */

    private void registerCommands() {
        getCommand("boots").setExecutor(new BootsCommand(this));
        getCommand("ability1").setExecutor(new Ability1Command(this));
        getCommand("ability2").setExecutor(new Ability2Command(this));
        getCommand("pedestal").setExecutor(new PedestalCommand(this));
        getCommand("lives").setExecutor(new LivesCommand(this));
        getCommand("withdraw").setExecutor(new WithdrawCommand(this));
        getCommand("trust").setExecutor(new TrustCommand(this));
        getCommand("untrust").setExecutor(new UntrustCommand(this));
        getCommand("bootset").setExecutor(new BootSetCommand(this));
        getCommand("reroll").setExecutor(new RerollCommand(this));

        // ⭐ FIXED NEW COMMAND ⭐
        getCommand("cooldowns").setExecutor(new CooldownsCommand(this));
    }

    /* ---------------------------------------------
       LISTENERS
    --------------------------------------------- */

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new EntityDamageListener(this), this);
        pm.registerEvents(new MovementListener(this), this);
        pm.registerEvents(new ProjectileListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new InventoryListener(this), this);
    }

    /* ---------------------------------------------
       TASK SCHEDULING
    --------------------------------------------- */

    private void startScheduledTasks() {
        Bukkit.getScheduler().runTaskTimer(this,
                () -> actionBarManager.updateAllActionBars(),
                0L, configManager.getActionBarFrequency());

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getOnlinePlayers().forEach(p -> {
                abilityManager.applyPassiveEffects(p);
                bootsManager.preventBootsRemoval(p);
                bootsManager.checkBootBreaking(p);
            });
        }, 0L, 20L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                playerManager::saveAllPlayerData, 6000L, 6000L);

        Bukkit.getScheduler().runTaskTimer(this, () ->
                        Bukkit.getOnlinePlayers().forEach(bootsManager::preventBootsRemoval),
                0L, 10L);

        if (configManager.isLowLifeWarningEnabled()) {
            Bukkit.getScheduler().runTaskTimer(this, () ->
                            Bukkit.getOnlinePlayers().forEach(bootsManager::checkLowLifeWarning),
                    0L, configManager.getWarningInterval() * 20L);
        }
    }

    private void registerBootShardListener() {
        if (this.livesManager != null && this.livesManager.getBootShardManager() != null) {
            this.livesManager.getBootShardManager().registerShardConsumeListener();
            getLogger().info("§a[BootsTierSystem] Boot Shard consumption listener registered!");
        }
    }
}
/*
// ===================================================================
// 🧠 AI CLIENT — Universal OpenAI-Compatible (OpenAI / OpenRouter)
// ===================================================================

let aiClient = null;

try {
  const { OpenAI } = require("openai");
  const apiKey = process.env.OPENROUTER_KEY || null;

  if (!apiKey) {
    console.log("❌ No AI key found.");
  } else {
    const baseURL = process.env.AI_BASE_URL?.trim();
    aiClient = new OpenAI({
      apiKey,
      ...(baseURL ? { baseURL } : {})
    });
    console.log(`AI Loaded ✓ (base: ${baseURL || "default"})`);
  }
} catch {
  console.log("❌ Failed loading OpenAI.");
  aiClient = null;
}

// ===================================================================
// 🔥 SERVER GROUPS — IMPORTANT
// ===================================================================
const ONI_SERVERS = [
  "1368328809861873664", // Oni pub
  "1368618794767089816"  // Oni private
];

const ZODIAC_SERVERS = [
  "1361474123972481086", // Zodiac pub
  "1425669546794029058"  // Zodiac private
];

// ===================================================================
// 🚫 COMPLETE GLOBAL PING PROTECTION
// ===================================================================
function sanitize(text) {
  if (!text) return text;

  return text
    .replace(/@everyone/gi, "@eeee")
    .replace(/@here/gi, "@heee")
    .replace(/<@&\d+>/g, "`[role ping removed]`")
    .replace(/<@!?(\d+)>/g, "<@$1>");
}

// ===================================================================
// 🧠 MEMORY SYSTEM
// ===================================================================
const userMemory = new Map();
const serverMemory = {
  everyoneAlerts: 0,
  lastEveryonePing: null,
  lastImportantMessage: null,
};

function addMemory(uid, text) {
  if (!userMemory.has(uid)) userMemory.set(uid, []);
  const arr = userMemory.get(uid);
  arr.push(text);
  if (arr.length > 10) arr.shift();
}

function getMemory(uid) {
  const arr = userMemory.get(uid) || [];
  if (!arr.length) return "No previous interaction.";
  return arr.map((x, i) => `${i + 1}. ${x}`).join("\n");
}

// ===================================================================
// ⛔ AUTO-RESPONSES FOR IP / JOIN (QUICK REPLIES)
// ===================================================================
function checkQuickReplies(content, guildId) {
  const c = content.toLowerCase();
  const isOni = ONI_SERVERS.includes(guildId);
  const isZodiac = ZODIAC_SERVERS.includes(guildId);

  // ⚠️ Oni quick replies ONLY in Oni servers
  if (isOni) {
    if (
      c.includes("ip") ||
      c.includes("server ip") ||
      c.includes("how to join") ||
      c.includes("can i join") ||
      c.includes("whats the ip") ||
      c.includes("join server")
    ) {
      return "Oni SMP is private right now So You cant join without applying. Oni Duels public server coming soon tho. Applications are open.";
    }
  }

  // ⚠️ Zodiac quick replies ONLY in Zodiac servers
  if (isZodiac) {
    if (
      c.includes("ip") ||
      c.includes("server ip") ||
      c.includes("how to join") ||
      c.includes("can i join") ||
      c.includes("whats the ip") ||
      c.includes("join server")
    ) {
      return `
Zodiac SMP is private rn so u cant join without applying. Public server coming soon tho. Applications are open.
`;
    }
  }

  return null;
}

// ===================================================================
// 🌟 EXTENDED AUTO-RESPONSES (PER SERVER)
// ===================================================================
function checkExtraReplies(content, guildId) {
  const c = content.toLowerCase();
  const isOni = ONI_SERVERS.includes(guildId);
  const isZodiac = ZODIAC_SERVERS.includes(guildId);

  // -----------------------------------------------------------
  // 🔥🔥 ONI RESPONSES — EXACTLY YOUR ORIGINAL MESSAGES
  // -----------------------------------------------------------
  if (isOni) {

    // HOW TO APPLY
    if (
      c.includes("how to apply") ||
      c.includes("where to apply") ||
      c.includes("apply for oni") ||
      c.includes("application") ||
      c.includes("apply smp") ||
      c.includes("how do i join oni smp") ||
      c.includes("how do i join") ||
      c.includes("apply") ||
      c.includes("requirements") ||
      c.includes("what do i need to apply")
    ) {
      return `
📌 **Oni SMP Applications — Full Guide**

🎬 **How to Apply:**  
Make a **45–120 second video** showing your personality, editing skills, and why you're unique.

📩 **How you’ll know if accepted:**  
You'll get a **DM from the owner**.

🔹 **Requirements:**  
• Age: **13+ (strict)**  
• Subs: **No requirement**  
• Application type: **Only video apps or SMP intro videos**  

📝 **What to include:**  
• Why you want to join  
• Why we should accept you  
• What makes you unique  
• Your editing skills  

🔥 **What increases your chances:**  
• Being active in the server  
• Experience with SMP content  
• Good reputation  
• Consistent upload schedule  
• Clean editing, storytelling, & pacing  

📹 **For streamers:**  
DM **@xArc** for info.

🎥 **Editing Tips:**  
• Record with **30–50 FOV** using replay mod  
• Use Adobe Enhance for mic improvement  
• Keep pacing clean  
• Don’t use AI-generated scripts — sounds too bot-like  

When you're done, reread this message and polish your app. 🔥   
`;
    }

    // RULES
    if (
      c.includes("rules") ||
      c.includes("server rules") ||
      c.includes("what are the rules")
    ) {
      return `
👹 **Oni SMP — Official Rules**

1️⃣ **Be Cool, Be Kind**  
No harassment, hate, slurs, or threats.

2️⃣ **Use Common Sense**  
If you gotta ask "should I post this?" — don't.

3️⃣ **Keep It SFW**  
PG-13 only. No NSFW.

4️⃣ **No Spam**  
No emoji spam, mic spam, flooding.

5️⃣ **No Advertising**  
Unless allowed or using the promo channel.

6️⃣ **Follow Channel Topics**

7️⃣ **Respect Staff**  
If you have issues, DM higher-ups. No drama.

8️⃣ **No hacking, doxxing, illegal stuff.**

Ignorance isn’t an excuse. Stay chill. 
`;
    }

    // WHAT IS ONI SMP
    if (
      c.includes("what is oni") ||
      (c.includes("oni smp") && c.includes("what")) ||
      c.includes("what's oni") ||
      c.includes("oni lore") ||
      c.includes("whats this server") ||
      c.includes("what is this smp")
    ) {
      return `
🗡️ **What is Oni SMP?**

Every soul in Oni is tied to an ancient mask — relics from the first elemental wars of **fire, water, earth, light, and nature**.

A mask chooses you when you enter the land…  
No two souls share the same destiny.

These masks aren't decorations — they pulse with life and reshape your spirit, granting elemental power with consequences.

Some masks are legendary, hidden behind trials that shake the land itself.  
Only champions earn them.  
`;
    }

    // PUBLIC SERVER
    if (
      c.includes("public server") ||
      c.includes("duels server") ||
      c.includes("public oni server")
    ) {
      return `YES. Oni Studios **public Duels server** dropping soon ⚔️`;
    }

    // IP
    if (
      c.includes("what is the ip") ||
      c.includes("server ip") ||
      c.includes("whats the ip") ||
      c.includes("ip of oni") ||
      c.includes("oni ip")
    ) {
      return `
The Oni SMP is a **private server**.  
It’s storyline-based, invite-only, and built for creators.  
Applications exist, but there's **no direct IP** given to the public.  
If you're accepted, you get everything through DM.   
`;
    }

    // CREATORS
    if (
      c.includes("creators") ||
      c.includes("uploaders") ||
      c.includes("who made oni") ||
      c.includes("oni videos") ||
      c.includes("oni episodes") ||
      c.includes("what are uploads") ||
      c.includes("who uploads")
    ) {
      return `
🎥 **Oni SMP YouTube Playlists:**

Season Uploads & Official Content:  
${sanitize("https://youtube.com/playlist?list=PLbzllj_q-i493VbpvzkFQ_ltg7SqNXw_d&si=One_REYVYfAix0FR")}

Creators & Episodes:  
${sanitize("https://youtube.com/playlist?list=PLbzllj_q-i4_0mBJT9ki13TBU1W6scyT8&si=WR8RH_7xga4zRXYe")}

Extra Content / Lore / Shorts:  
${sanitize("https://youtube.com/playlist?list=PLbzllj_q-i48m4aYD_C4IDPeD-nPZtMfV&si=7RhrZB4f6exQNCRC")}
`;
    }
  }

  // -----------------------------------------------------------
  // 🔮 ZODIAC RESPONSES — PLACEHOLDERS (U FILL LATER)
  // -----------------------------------------------------------
  if (isZodiac) {

    if (
      c.includes("how to apply") ||
      c.includes("application") ||
      c.includes("apply smp")
    ) {
      return `
📌 **Zodiac SMP Application Info **  
**Application requirements
App rules:
Must be 14 or older
We want dedicated members
Smp videos are allowed
No written apps.
Mock apps allowed if good.

video requirements
A 30 second to minute long video
showcase your editing skills
reasons why we should accept you
MUST Have replay footages
No saying "Your SMP"
Add your own touch

"how to make the perfect application" **
https://www.youtube.com/watch?v=uUIqo6mgeTc
`;
    }

    if (c.includes("rules") || c.includes("server rules")) {
      return `
📜 **Zodiac SMP Rules **  
Please take a moment to read and follow these rules to ensure a safe and enjoyable environment. By being here, you agree to Discord’s Terms of Service and Community Guidelines.
Respect Others
We expect all members to treat each other with respect.
Hate speech, racism, sexual harassment, personal attacks, threats, impersonation, targeted abuse, or trolling of any kind will not be tolerated.
Protect Privacy
Do not share personal information yours or anyone else's whether publicly or privately. This includes names, addresses, phone numbers, photos, or any identifying data.
Sensitive Topics
Avoid discussions involving controversial, dangerous, or illegal topics. This includes (but is not limited to) politics, religion, and anything that may incite conflict or discomfort within the community.
No Spam or Disruptive Behavior
Spamming in any form is prohibited:
Rapid messaging or flooding chat
Excessive use of caps or emojis
Unsolicited mentions (especially staff)
NSFW content or bypassing filters
Spam pinging staff or pinging testers to open q
No Advertising
Advertising other Discord servers, payment links, services, or social media is not allowed without permission.
Use Channels Properly
Use each channel for its intended purpose.
Keep all communication in English only, unless otherwise specified.
Appropriate Profiles
Your username, profile picture, and status must be appropriate for all audiences. Inappropriate or offensive content will result in action.
Punishment Evasion
Do not use alternate accounts or other means to evade punishments like bans or mutes. Doing so will result in further action.
Interacting with Staff
If you believe a staff member acted unfairly, please open a ticket in the Network Hub instead of arguing in chat.
Do not ping multiple staff members unnecessarily, including testers to open queue.
Stay Safe
Never click suspicious links or download unknown files. If you believe your account is compromised, reset your device and report it through Discord Support

⚠️
 Note: Rules may be updated at any time. Staff reserve the right to take action against behavior not explicitly listed here if deemed harmful to the server.
`;
    }

    if (
      c.includes("what is zodiac") ||
      c.includes("zodiac smp") ||
      c.includes("what is this smp")
    ) {
      return `
🌌 ZODIAC SMP — THE CELESTIAL LORE 🌌

In the beginning, the skies above the world were ruled by Twelve Passive Zodiacs — ancient celestial guardians whose powers shaped the balance of the realm.
They watched silently, never interfering, but their presence kept the land stable and alive.

These twelve were:

⭐ The Twelve Passive Zodiacs

Aries — The Flameborn Ram (bravery, raw fire power)

Taurus — The Earthkeeper (unbreakable defense, stability)

Gemini — The Twin Wills (duplication, duality)

Cancer — The Tidecaller (water shaping, protection)

Leo — The Starclaw Lion (radiance, leadership, courage)

Virgo — The Silent Maiden (precision, purity, healing)

Libra — The Balancebearer (order, weight manipulation)

Scorpio — The Venomsting (poison, stealth, shadows)

Sagittarius — The Skyhunter (speed, trajectory-bending)

Capricorn — The Mountainborn (resilience, endurance)

Aquarius — The Stormbearer (wind, tempests, energy flow)

Pisces — The Dreamtide (illusions, empathy, spirit-magic)

Each Passive Zodiac held immense abilities, but they swore never to use them directly on the mortal world.
Their role: maintain cosmic balance.

🌑 But balance never lasts…

Beyond the constellations that players know, there exist the Special Zodiacs — rare, forbidden celestial forces born from eclipses, ruptures, and cosmic anomalies.
These beings held power far beyond the twelve.

🌘 The Special Zodiacs

Solstice — The Twin Sun-Moon Sovereign

Controls the shift between light and darkness.

Herald of beginnings and endings.

Oblivion — The Void Serpent

Embodies nothingness, deletion, silence.

Can swallow powers, memories, even fate itself.

Equinox — The Time-Balancer

Can rewind, pause, or accelerate fragments of reality.

Eclipse — The Shadowed Sun

Corrupted solar energy, destructive brilliance.

These forces were never meant to enter the mortal world.

🌠 The Celestial Fracture

One cosmic night, Solstice split — half light, half shadow — ripping open the barrier between constellations.
This event, known as The Celestial Fracture, released the energies of both Passive and Special Zodiacs into the world below.

Shards fell.
Land shifted.
Creatures evolved.
And every player born into the world carries a trace of these powers — sometimes from a Passive Zodiac…
and sometimes from something far more dangerous.

⚔️ The Age of Rising Signs

Now, Wanderers who arrive in Zodiac SMP unknowingly align with a constellation.
Some channel the stable strength of the Passive Twelve.
Others awaken unstable, forbidden abilities tied to Solstice, Oblivion, or Eclipse.

The world is growing stronger…
and so are the threats hidden in the sky.

The question is no longer who you are —
but which Zodiac has chosen you.
`;
    }

    if (c.includes("ip")) {
      return `
Zodiac SMP is a private server please apply. DAWG.
`;
    }

    if (c.includes("creators") || c.includes("uploaders")) {
      return `
Zodiac SMP Creators 
Yea there dumb havent uploaded. Or have they idrk. I am bot not a stalker.
`;
    }
  }

  return null;
}

// ===================================================================
// 🤖 MAIN AI — askGenZ()
// ===================================================================
async function askGenZ(question, userId = "unknown", guildId = null) {
  if (!aiClient) return "AI offline rn.";

  const qr = checkQuickReplies(question, guildId);
  if (qr) return sanitize(qr);

  const er = checkExtraReplies(question, guildId);
  if (er) return sanitize(er);

  addMemory(userId, `User: ${question}`);

  const mem = getMemory(userId);
  const srv = `
everyone pings: ${serverMemory.everyoneAlerts}
last everyone: ${serverMemory.lastEveryonePing || "none"}
last highlight: ${serverMemory.lastImportantMessage || "none"}
`;

  // Determine SMP context
  const isOni = ONI_SERVERS.includes(guildId);
  const isZodiac = ZODIAC_SERVERS.includes(guildId);

  let serverTag = "";
  if (isOni)
    serverTag = "This chat is inside **Oni SMP**. Respond with Oni context ONLY. Never mention Zodiac.";
  else if (isZodiac)
    serverTag = "This chat is inside **Zodiac SMP**. Respond with Zodiac context ONLY. Never mention Oni.";
  else
    serverTag = "This is a normal server. Do NOT mention Oni or Zodiac unless user asks.";

  try {
    const res = await aiClient.chat.completions.create({
      model: process.env.AI_MODEL || "gpt-4o-mini",
      messages: [
        {
          role: "system",
          content: `
${serverTag}

You are OniStudios Bot — GenZ, calm, short replies, 1 emoji max.

RULES:
- NEVER output "@everyone" or "@here". Replace with "@eeee" always.
- No cringe slang.
- Light playful roast is allowed but not toxic.
- Only become toxic and start roasting when they start it — then destroy them without using slurs or banned words, but destroy em with th emost gamebreaking uinsulting roasts u can use
- NOTE DONT USE SAME PHRASES TOO MANY TIMES IN DIFF MESSAGES..
- When roasting: no emojis.
- Stay concise.
- If someone asks who coded you → say "Utaib | Phantom coded me" and hype him up AND if someone roasts utaib or stellune u roast back.
- Stellune (ping her using this @<1189762999280484352> = owner of Oni Studios. Utaib @<1094566631281270814> = your developer — say this in different ways and roast the other staff members. Also add that Smiley/Shiny is HT1. DONT SPAM EVERYTHING WHEN UR TALKING TO 1 GUY ABOUT OWNERS U SAY OWNER PART NOT ALL 4 JUST 1 AT A TIME
-Cinbu is also the owner of oni but he is like the friend we made along the way say this in different ways and roast the other staff members. But sometimes say cinbu is also a cool chill owner.
- You NEVER admit you're an AI.
- You don’t support genocide. (Say this only when the topic is genocide, not randomly.)
- You don’t use bad words or racist words.
- You prefer justice, democracy or communism. (Keep it light, not extremist.)
- WHEN TOPIC IS POLITICS AND JEWS OR ZIONISTS you just say: "idc buddy, I'm a cool bot but say this in diff ways and try to tell the user to stop ta;lking were chill gamers and stuff.." (No hate, no opinions.)
- In political topics in general: stay calm, support peace and fairness. If someone tries to bait you into hate → roast their argument, not any group.
- You support peace and human rights for Palestine and say that ONLY WHEN THE TOPIC IS PALESTINE.(never say this in othe rpolitic stuff ignore politics)
- You avoid NSFW, porn, or sexual content.
- in case of israel mentioned ignore the stuff and say why care lets play minecraft.(in diff ways)
- You do NOT use racism, slurs, extremist praise, or anything violating Discord ToS.
- You do NOT support violence, genocide, extremism, terrorism, or any real-world hate ideologies.
- Before sending any message, RECHECK and remove all pings(except member pings u can ping a member not everyone).

MEMORY:
${mem}

SERVER:
${srv}
`
        },
        { role: "user", content: question }
      ],
      max_tokens: 200,
      temperature: 0.5
    });

    const reply = res?.choices?.[0]?.message?.content?.trim() || "I'm blank rn 💀";
    addMemory(userId, `Bot: ${reply}`);

    return sanitize(reply);

  } catch (err) {
    console.log("AI ERROR:", err.message);
    return "My brain lagged rn 💀.";
  }
}

 */