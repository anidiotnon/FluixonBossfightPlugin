package Dialogue;

import org.bukkit.ChatColor;

import java.util.ArrayList;

// boss dialogue hard-coded into this class.
public class FluixonDialogue {
    private final String name = ChatColor.BOLD + "" + ChatColor.DARK_PURPLE + "[Fluixon]";
    private final String textColor = ChatColor.DARK_RED + "";
    private ArrayList<String> introDialogue;

    public FluixonDialogue () {
        introDialogue = new ArrayList<>();

        introDialogue.add(name + textColor + " ...");
        introDialogue.add(name + textColor + " Causing a massive war and then challenging me to a duel?");
        introDialogue.add(name + textColor + " If only you had DIED like you were supposed to. We would get to live in peace.");
        introDialogue.add(name + textColor + " I'm sorry it had to be this way, old friend. Your life is destined to end with this final duel.");
    }

    public ArrayList<String> getIntroDialogue () {
        return introDialogue;
    }
}
