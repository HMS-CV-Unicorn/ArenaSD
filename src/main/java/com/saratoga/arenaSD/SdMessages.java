package com.saratoga.arenaSD;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.battleplugins.arena.messages.Message;

import static org.battleplugins.arena.messages.Messages.error;
import static org.battleplugins.arena.messages.Messages.info;
import static org.battleplugins.arena.messages.Messages.success;

/**
 * Messages for Search and Destroy game mode.
 */
public final class SdMessages {
        public static final TextColor PRIMARY_COLOR = NamedTextColor.YELLOW;
        public static final TextColor SECONDARY_COLOR = NamedTextColor.GOLD;
        public static final TextColor ATTACKER_COLOR = NamedTextColor.RED;
        public static final TextColor DEFENDER_COLOR = NamedTextColor.BLUE;

        static final TagResolver RESOLVER = TagResolver.builder()
                        .tag("primary", Tag.styling(PRIMARY_COLOR))
                        .tag("secondary", Tag.styling(SECONDARY_COLOR))
                        .tag("attacker", Tag.styling(ATTACKER_COLOR))
                        .tag("defender", Tag.styling(DEFENDER_COLOR))
                        .build();

        // Team assignment
        public static final Message YOU_ARE_ATTACKERS = info("you-are-attackers",
                        "<attacker><bold>You are the ATTACKERS!</bold></attacker> Plant the bomb at site A or B to win.");
        public static final Message YOU_ARE_DEFENDERS = info("you-are-defenders",
                        "<defender><bold>You are the DEFENDERS!</bold></defender> Stop the attackers and defuse the bomb if planted.");

        // Bomb events
        public static final Message BOMB_SPAWNED = info("bomb-spawned",
                        "<secondary>The bomb has spawned! Attackers, pick it up!</secondary>");
        public static final Message BOMB_PICKED_UP = info("bomb-picked-up",
                        "<secondary>{} has picked up the bomb!</secondary>");
        public static final Message BOMB_DROPPED = info("bomb-dropped",
                        "<secondary>The bomb has been dropped!</secondary>");
        public static final Message BOMB_CARRIER_DIED = info("bomb-carrier-died",
                        "<secondary>{} has died and dropped the bomb!</secondary>");

        // Planting
        public static final Message PLANTING_BOMB = info("planting-bomb",
                        "<secondary>Planting the bomb...</secondary>");
        public static final Message BOMB_PLANTED = success("bomb-planted",
                        "<attacker><bold>BOMB PLANTED at site {}!</bold></attacker> {} seconds until detonation!");
        public static final Message PLANT_CANCELLED = error("plant-cancelled",
                        "<error>Bomb planting cancelled!</error>");

        // Defusing
        public static final Message DEFUSING_BOMB = info("defusing-bomb",
                        "<secondary>Defusing the bomb...</secondary>");
        public static final Message BOMB_DEFUSED = success("bomb-defused",
                        "<defender><bold>BOMB DEFUSED!</bold></defender> {} has saved the day!");
        public static final Message DEFUSE_CANCELLED = error("defuse-cancelled",
                        "<error>Bomb defusing cancelled!</error>");

        // Detonation
        public static final Message BOMB_DETONATED = error("bomb-detonated",
                        "<attacker><bold>BOOM!</bold></attacker> The bomb has detonated!");

        // Round messages
        public static final Message ROUND_START = info("round-start",
                        "<primary>Round {} starting!</primary>");
        public static final Message ROUND_WIN_ATTACKERS = success("round-win-attackers",
                        "<attacker>Attackers win the round!</attacker>");
        public static final Message ROUND_WIN_DEFENDERS = success("round-win-defenders",
                        "<defender>Defenders win the round!</defender>");
        public static final Message SIDES_SWAPPED = info("sides-swapped",
                        "<primary><bold>SIDES SWAPPED!</bold></primary> Teams have switched roles.");
        public static final Message TIME_EXPIRED = info("time-expired",
                        "<secondary>Time expired! Defenders win the round.</secondary>");

        // Score
        public static final Message SCORE_UPDATE = info("score-update",
                        "<primary>Score: <attacker>Attackers {}</attacker> - <defender>Defenders {}</defender></primary>");

        // Carrier notification
        public static final Message PROTECT_CARRIER = info("protect-carrier",
                        "<secondary>Protect <bold>{}</bold> who has the bomb!</secondary>");
        public static final Message YOU_HAVE_BOMB = info("you-have-bomb",
                        "<secondary><bold>You have the bomb!</bold> Plant it at site A or B.</secondary>");

        // Progress bars
        public static final Message PLANT_PROGRESS = info("plant-progress", "{}");
        public static final Message DEFUSE_PROGRESS = info("defuse-progress", "{}");

        // Spectator
        public static final Message NOW_SPECTATING = info("now-spectating",
                        "<secondary>You are now spectating {}. Use scroll wheel to switch players.</secondary>");

        // Editor messages
        public static final Message EDITOR_SET_BOMB_SPAWN = info("editor-set-bomb-spawn",
                        "Type \"bomb\" to set the bomb spawn location, or \"cancel\" to cancel.");
        public static final Message EDITOR_SET_SITE_POSITION = info("editor-set-site-position",
                        "Type \"site\" to set the bomb site position, or \"cancel\" to cancel.");
        public static final Message BOMB_SPAWN_SET = success("bomb-spawn-set",
                        "<green>Bomb spawn location has been set!</green>");
        public static final Message BOMB_SITE_SET = success("bomb-site-set",
                        "<green>Bomb site {} has been set!</green>");
        public static final Message SITE_RADIUS_SET = success("site-radius-set",
                        "<green>Site {} radius set to {}.</green>");
        public static final Message NOT_SD_MAP = error("not-sd-map",
                        "<red>This map is not a Search and Destroy map!</red>");
        public static final Message SITE_NOT_FOUND = error("site-not-found",
                        "<red>Site {} not found! Set it first with /snd site {}</red>");

        private SdMessages() {
        }

        public static void init() {
                // Trigger class loading
        }
}
