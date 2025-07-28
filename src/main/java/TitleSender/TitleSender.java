package TitleSender;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TitleSender {
    // basically, performs the same thing as the /title command in minecraft.
    public static void sendTitle (String titleString, String subtitleString, Player player, long fadeInDurationMillis,
                                  long stayDurationMillis, long fadeOutDurationMillis, NamedTextColor titleColor, NamedTextColor subtitleColor) {

        Component title = Component.text(titleString).color(titleColor);
        Component subtitle = Component.text(subtitleString).color(subtitleColor);

        // Define title display and fade durations
        Title.Times times = Title.Times.times(Duration.ofMillis(fadeInDurationMillis), Duration.ofMillis(stayDurationMillis),
                Duration.ofMillis(fadeOutDurationMillis));

        // Get audience and send title
        ((Audience)player).showTitle(Title.title(title, subtitle, times));
    }
}
