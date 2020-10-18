package Ozone.Patch;

import Ozone.Event.Internal;
import arc.Events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static Ozone.Interface.registerWords;

public class Translation {
    public static final ArrayList<String> normalSinglet = new ArrayList<>(Arrays.asList("Run"));
    public static final ArrayList<String> singlet1 = new ArrayList<>(Arrays.asList("String", "Integer", "Float", "Long", "Boolean", "Commands"));
    public static final HashMap<String, String> settings = new HashMap<>();
    public static final HashMap<String, String> commands = new HashMap<>();
    public static final HashMap<String, String> keyBinds = new HashMap<>();

    public static void patch() {
        settings.put("antiSpam", "[Broken]Enable Anti-Spam");
        settings.put("debugMode", "Enable Debug Mode");
        settings.put("colorPatch", "Enable Colorized Text");
        settings.put("commandsPrefix", "Commands Prefix");
        registerWords("ozone.menu", "Ozone Menu");
        registerWords("ozone.hud", "Ozone HUD");
        registerWords("ozone.javaEditor", "Java Executor");
        registerWords("ozone.javaEditor.reformat", "Reformat");
        registerWords("ozone.javaEditor.run", "Run");
        registerWords("ozone.commandsUI", "Commands GUI");
        commands.put("help", "help desk");
        commands.put("chaos-kick", "vote everyone everytime everywhere");
        commands.put("task-move", "move using current unit with pathfinding algorithm");
        commands.put("info-pos", "get current info pos");
        commands.put("info-pathfinding", "get Pathfinding overlay");
        commands.put("force-exit", "you want to crash ?");
        commands.put("task-deconstruct", "deconstruct your block with AI");
        commands.put("send-colorize", "send Colorized text");
        commands.put("info-unit", "get current unit info");
        commands.put("random-kick", "random kick someone");
        commands.put("shuffle-sorter", "shufleelelelel Da Sorter And Everything");
        commands.put("javac", "run single line of code, like \nVars.player.unit().moveAt(new Vec2(100, 100));");
        commands.put("task-clear", "clear all bot task");
        Events.fire(Internal.Init.TranslationRegister);
        for (Map.Entry<String, String> s : commands.entrySet()) {
            registerWords("ozone.commands." + s.getKey(), s.getValue());
        }
        for (Map.Entry<String, String> s : settings.entrySet()) {
            registerWords("setting.ozone." + s.getKey() + ".name", s.getValue());
        }
        for (Map.Entry<String, String> s : keyBinds.entrySet()) {
            registerWords("section." + s.getKey() + ".name", s.getValue());
        }
        for (String s : singlet1) registerWords(s, "[" + s + "]");
        for (String s : normalSinglet) registerWords(s);

    }
}
