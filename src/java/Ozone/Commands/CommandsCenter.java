/*******************************************************************************
 * Copyright 2021 Itzbenz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

/* o7 Inc 2021 Copyright

  Licensed under the o7 Inc License, Version 1.0.1, ("the License");
  You may use this file but only with the License. You may obtain a
  copy of the License at
  
  https://github.com/o7-Fire/Mindustry-Ozone/Licenses
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the license for the specific language governing permissions and
  limitations under the License.
*/

package Ozone.Commands;

import Atom.Reflect.Reflect;
import Atom.Time.Time;
import Atom.Utility.Pool;
import Atom.Utility.Random;
import Atom.Utility.Utility;
import Ozone.Bot.VirtualPlayer;
import Ozone.Commands.Class.CommandsArgument;
import Ozone.Commands.Class.CommandsClass;
import Ozone.Commands.Task.*;
import Ozone.Gen.Callable;
import Ozone.Internal.AbstractModule;
import Ozone.Internal.Interface;
import Ozone.Main;
import Ozone.Manifest;
import Ozone.Patch.EventHooker;
import Ozone.Patch.Translation;
import Ozone.Settings.BaseSettings;
import Shared.SharedBoot;
import Shared.WarningReport;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.Colors;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.OrderedMap;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.net.Administration;
import mindustry.net.Net;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ArmoredConveyor;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.StackConveyor;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CommandsCenter extends AbstractModule {
	
	public static final Queue<Task> commandsQueue = new Queue<>();
	public static final Map<String, Command> commandsList = new TreeMap<>();
	public static final TreeMap<String, Payload> payloads = new TreeMap<>();
	public static HashMap<Integer, Integer> targetPlayer = new HashMap<>();
	private static boolean falseVote = false;
	private static boolean drainCore = false;
	private static boolean chatting = false;
	private static VirtualPlayer virtualPlayer = null;
	public static final TreeMap<String, CommandsClass> commandsListClass = new TreeMap<>();
	private static int i = 0;
	
	public static void virtualPlayer(VirtualPlayer virtualPlayer) {
		CommandsCenter.virtualPlayer = virtualPlayer;
	}
	
	public static void virtualPlayer() {
		virtualPlayer = null;
	}
	
	{
		dependsOn.add(Translation.class);
	}
	
	//behold, java dark magic
	public static void registerArgument(CommandsClass unfreeze, CommandsArgument ca, Iterator<Field> iterator) {
		if (iterator.hasNext()) {
			Field f = iterator.next();
			Interface.showInput(f.getDeclaringClass().getCanonicalName() + "." + f.getName(), "(" + Translation.get(f.getType().getName()) + ")-" + ca.getTranslate(f.getName()), s -> {
				try {
					f.set(ca, Reflect.parseStringToPrimitive(s, f.getType()));
					registerArgument(unfreeze, ca, iterator);
				}catch (Throwable t) {
					if (SharedBoot.debug) t.printStackTrace();
					Vars.ui.showException(t);
				}
				
			});
		}else {
			unfreeze.run(ca);
		}
		
	}
	
	private static void powerNode(List<String> strings) {
	
	}
	
	private static void powerNodeDisconnectAll() {
		powerNode(Arrays.asList("disconnect", "all"));
	}
	
	private static void powerNodeConnectAll() {
		powerNode(Arrays.asList("connect", "all"));
	}
	
	
	public static void hudFragToast(List<String> arg) {
		String s = "[" + Random.getRandomHexColor() + "]Test " + Random.getString(16);
		if (!arg.isEmpty()) s = Utility.joiner(arg, " ");
		Vars.ui.hudfrag.showToast(s);
	}
	
	public static void hudFrag(List<String> arg) {
		String s = "[" + Random.getRandomHexColor() + "]Test " + Random.getString(16);
		if (!arg.isEmpty()) s = Utility.joiner(arg, " ");
		Vars.ui.hudfrag.setHudText(s);
	}
	
	public static void clearPathfindingOverlay() {
		tellUser("Clearing: " + Pathfinding.render.size() + " overlay");
		Pathfinding.render.clear();
	}
	
	public static void shuffleConfigurable() {
	
	}
	
	public static void messageLog() {
	
	}
	
	public static void garbageCollector() {
		long l = Core.app.getJavaHeap();
		System.gc();
		tellUser("Cleared: " + l + " bytes");
	}
	
	public static void register(String name, Command command) {
		register(name, command, null);
	}
	
	public static void register(String name, Command command, String description) {
		if (description != null) Interface.registerWords("ozone.commands." + name, description);
		commandsList.put(name, command);
	}
	
	public static void taskDeconstruct(List<String> s) {
		taskDeconstruct(s, Vars.player);
	}
	
	public static void moduleReset() {
		EventHooker.resets();
		
	}
	
	public static void debug() {
		if (!SharedBoot.debug) {
			tellUser("The debug mode mason, what do they mean");
			return;
		}
		if (i == 5) {
			tellUser("pls dont");
		}else if (i == 10) tellUser("stop pls");
		else if (i == 20) {
			tellUser("wtf ???");
			i = 0;
		}else {
			tellUser("The code mason, what do they mean");
		}
		i++;
	}
	
	public static void taskClear() {
		TaskInterface.taskQueue.clear();
		tellUser("Task cleared");
	}
	
	public static void sendColorize(List<String> s) {
		if (s.isEmpty()) {
			tellUser("Empty ? gabe itch");
			return;
		}
		
		String text = Utility.joiner(Utility.getArray(s), " ");
		StringBuilder sb = new StringBuilder();
		if (text.length() * 10 > Vars.maxTextLength) {
			OrderedMap<String, Color> map = Colors.getColors();
			ArrayList<String> colors = new ArrayList<>();
			for (String mp : map.keys()) {
				colors.add('[' + mp + ']');
			}
			String[] colorss = new String[colors.size()];
			colorss = colors.toArray(colorss);
			for (char c : text.toCharArray()) {
				if (c != ' ') {
					sb.append(Random.getRandom(colorss)).append(c);
				}else sb.append(c);
			}
		}else {
			for (char c : text.toCharArray()) {
				if (c != ' ') sb.append("[").append(Random.getRandomHexColor()).append("]").append(c);
				else sb.append(c);
			}
		}
		Call.sendChatMessage(sb.toString());
	}
	
	public static void taskDeconstruct(List<String> s, Player vars) {
		if (s.size() < 2) {
			tellUser("Not enough arguments");
			tellUser("Usage: task-deconstruct x(type: coordinate) y(type: coordinate) half(type: boolean, optional default: false)");
			return;
		}
		try {
			int x = Integer.parseInt(s.get(0));
			int y = Integer.parseInt(s.get(1));
			if (Vars.world.tile(x, y) == null) {
				tellUser("Non existent tiles");
				return;
			}
			boolean half = false;
			// i don't trust user
			if (s.size() == 3) {
				half = true;
			}
			Time t = new Time(TimeUnit.MICROSECONDS);
			TaskInterface.addTask(new DestructBlock(x, y, half, vars), a -> tellUser("Completed in " + t.elapsed(new Time()).toString()), vars);
		}catch (NumberFormatException f) {
			tellUser("Failed to parse integer, are you sure that argument was integer ?");
			Vars.ui.showException(f);
		}
	}
	
	public static void taskMove(List<String> s) {
		taskMove(s, Vars.player);
	}
	
	public static void forceExit(List<String> ar) {
		throw new OutOfMemoryError("Force Exit: " + Utility.joiner(Utility.getArray(ar), ", "));
	}
	
	public static void infoUnit() {
		tellUser(Vars.player.unit().getClass().getCanonicalName());
	}
	
	public static String getTranslation(String name) {
		return Translation.get("ozone.commands." + name);
	}
	
	public static boolean call(String message) {
		if (!message.startsWith(BaseSettings.commandsPrefix)) return false;
		message = message.substring(BaseSettings.commandsPrefix.length());
		if (message.isEmpty()) return false;
		ArrayList<String> mesArg = new ArrayList<>(Arrays.asList(message.split(" ")));
		if (!commandsList.containsKey(mesArg.get(0).toLowerCase())) {
			tellUser("Commands not found");
			return false;
		}
		Command comm = commandsList.get(mesArg.get(0).toLowerCase());
		ArrayList<String> args;
		if (mesArg.size() > 1) {
			message = message.substring(mesArg.get(0).length() + 1);
			args = new ArrayList<>(Arrays.asList(message.split(" ")));
		}else {
			args = new ArrayList<>();
		}
		comm.method.accept(args);
		return true;
	}
	
	public static void randomKick() {
		if (Groups.player.size() < 2) {
			tellUser("Not enough player");
			return;
		}
		Player p = Random.getRandom(Groups.player);
		if (p == null) return;//we get em next time
		tellUser("Votekicking: " + p.name);
		Call.sendChatMessage("/votekick " + p.name);
	}
	
	public static void infoPathfinding(List<String> s) {
		if (s.size() < 4) {
			tellUser("Not enough arguments");
			tellUser("usage: " + "info-pathfinding x(type: source-coordinate) y(type: source-coordinate) x(type: target-coordinate) y(type: target-coordinate) block(type: Blocks, optional)");
			return;
		}
		try {
			String block = "";
			int xS = Integer.parseInt(s.get(0));
			int yS = Integer.parseInt(s.get(1));
			if (Vars.world.tile(xS, yS) == null) {
				tellUser("Non existent source tiles");
				return;
			}
			int xT = Integer.parseInt(s.get(2));
			int yT = Integer.parseInt(s.get(3));
			if (s.size() == 5) block = s.get(4);
			Block pathfindingBlock = null;
			if (!block.isEmpty()) {
				pathfindingBlock = Vars.content.block(block);
				if (pathfindingBlock == null) tellUser("Nonexistent block, using default block: magmarock/dirtwall");
			}
			
			
			Tile target = Vars.world.tile(xT, yT);
			Tile source = Vars.world.tile(xS, yS);
			if (target == null) {
				tellUser("Non existent target tiles");
				return;
			}
			if (source == null) {
				tellUser("Non existent source tiles");
				return;
			}
			Pool.submit(() -> {
				Seq<Tile> tiles = new Seq<>();
				try {
					tiles.addAll(Pathfinding.pathfind(target));
					Pathfinding.render.add(new Pathfinding.PathfindingOverlay(tiles));
				}catch (Throwable e) {
					tellUser("Pathfinding failed");
					tellUser(e.toString());
				}
				return tiles;
			});
			
		}catch (NumberFormatException f) {
			tellUser("Failed to parse integer, are you sure that argument was integer ?");
			Vars.ui.showException(f);
		}
	}
	
	public static void toggleUI() {
		Manifest.menu.hide();
	}
	
	public static void infoPos() {
		tellUser("Player x,y: " + Vars.player.x + ", " + Vars.player.y);
		tellUser("TileOn x,y: " + Vars.player.tileX() + ", " + Vars.player.tileY());
		if (Vars.player.tileOn() != null && Vars.player.tileOn().build != null)
			tellUser("TileOn: Class: " + Vars.player.tileOn().build.getClass().getName());
	}
	
	public static void taskMove(List<String> s, Player vars) {
		if (s.size() < 2) {
			tellUser("Not enough arguments");
			tellUser("usage: " + "task-move x(coordinate) y(coordinate)");
			return;
		}
		try {
			int x = Integer.parseInt(s.get(0));
			int y = Integer.parseInt(s.get(1));
			if (Vars.world.tile(x, y) == null) {
				tellUser("Non existent tiles");
				return;
			}
			Time start = new Time();
			TaskInterface.addTask(new Move(Vars.world.tile(x, y), vars), a -> tellUser("Reached in " + start.elapsedS()), vars);
			toggleUI();
		}catch (NumberFormatException f) {
			tellUser("Failed to parse integer, are you sure that argument was integer ?");
			Vars.ui.showException(f);
		}
		
	}
	
	public static void shuffleSorterPayload(Callable c) {
		try {
			shuffleSorterCall(c, Interface.getRandomSorterLikeShit().get());
		}catch (Throwable t) {
			Log.err(t);
		}
	}
	
	public static void shuffleSorter() {
		shuffleSorter(Vars.net);
	}
	
	public static void help() {
		ArrayList<String> as = new ArrayList<>();
		as.add("Prefix: \"" + BaseSettings.commandsPrefix + "\"");
		as.add("Available Commands:");
		
		for (Map.Entry<String, Command> s : commandsList.entrySet()) {
			String local = s.getKey() + ": " + s.getValue().description;
			as.add(local);
		}
		
		while (!as.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < 5; j++) {
				if (as.isEmpty()) break;
				String s = as.get(0);
				sb.append(s).append("\n");
				as.remove(s);
			}
			tellUser(sb.toString());
		}
	}
	
	public static void shuffleSorterCall(Callable call, Building t) {
		if (t == null || t.tile == null) {
			tellUser("block can't be find");
			return;
		}
		Item target = Random.getRandom(Vars.content.items());
		t.tile.build.block.lastConfig = target;
		call.tileConfig(null, t.tile.build, target);
	}
	
	public static void shuffleSorter(Net net) {
		
		TaskInterface.addTask(new Completable() {
			final Future<Building> f;
			
			{
				name = "shuffleSorter";
				f = Interface.getRandomSorterLikeShit();
				if (f == null) {
					tellUser("wtf ? shuffle sorter future is null");
					completed = true;
				}
			}
			
			@Override
			public void update() {
				if (f == null) return;
				if (!f.isDone()) return;
				if (completed) return;
				completed = true;
				try {
					shuffleSorterCall(new Callable(net), f.get());
				}catch (IndexOutOfBoundsException gay) {
					CommandsCenter.tellUser("No item");
				}catch (InterruptedException | ExecutionException e) {
					Log.errTag("Ozone-Executor", "Failed to get tile:\n" + e.toString());
				}
			}
		});
		
	}
	
	public static void followPlayer(List<String> arg) {
		followPlayer(arg, Vars.player);
	}
	
	public static void followPlayer(List<String> arg, Player vars) {
		String p = Utility.joiner(arg, " ");
		if (arg.isEmpty()) {
			if (targetPlayer.get(vars.id) != null) {
				tellUser("Stop following player");
				targetPlayer.put(vars.id, null);
			}else {
				tellUser("Empty Argument, use player name or ID");
			}
			return;
		}
		Player target = Interface.searchPlayer(p);
		if (target == null) {
			tellUser("Player not found");
			targetPlayer.put(vars.id, null);
			return;
		}
		if (targetPlayer.get(vars.id) == null) {
			tellUser("Found player: distance " + Pathfinding.distanceTo(Vars.player, target));
		}
		targetPlayer.put(vars.id, target.id);
		if (!Pathfinding.withinPlayerTolerance(target))
			TaskInterface.addTask(new Move(target.tileOn(), vars) {{name = "followPlayer:" + target.name();}}, vars);
		
		TaskInterface.addTask(new SingleTimeTask(() -> {//basically invoke this method again if target isnt null
			if (targetPlayer.get(vars.id) == null) return;//gone
			Player t = Interface.searchPlayer(targetPlayer.get(vars.id) + "");
			if (t == null) tellUser("Player gone, stop following");
			
			else followPlayer(new ArrayList<>(Collections.singletonList(t.id + "")), vars);
		}) {
			{
				name = "playerFollower:" + targetPlayer;
			}
		}, vars);
	}
	
	public static void tellUser(String s) {
		if (CommandsCenter.virtualPlayer != null) {
			virtualPlayer.log.info(s);
			return;
		}
		
		if (Vars.ui == null) return;
		if (Vars.ui.scriptfrag.shown()) Log.infoTag("Ozone", s);
		else Log.info(Strings.stripColors(s));
		
		if (Vars.state.isGame()) {
			try {
				Vars.ui.chatfrag.addMessage("[white][[[royal]Ozone[white]]: " + s, null);
			}catch (NoSuchMethodError ignored) {}
			if (BaseSettings.commandsToast) {
				if (s.contains("\n")) for (String u : s.split("\n"))
					Interface.showToast(u, 800);
				else Vars.ui.hudfrag.showToast(s);
			}
		}
	}
	
	public static void kickJammer() {
		falseVote = !falseVote;
		if (falseVote) {
			TaskInterface.addTask(new CompletableUpdateBasedTimeTask(() -> {
				if (Groups.player.size() < 2) {
					falseVote = false;
					tellUser("Not enough player, stopping falseVote");
					return;
				}
				Player target = Random.getRandom(Groups.player);
				if (target == null) target = Random.getRandom(Groups.player);
				if (target == null) {
					tellUser("Can't get random player, aborting to avoid recursion");
					falseVote = false;
					return;
				}
				Call.sendChatMessage("/votekick " + target.name);
			}, Administration.Config.messageRateLimit.num() * 1000L, () -> falseVote) {
				{
					name = "falseVote";
				}
			});
			tellUser("kicking started");
		}else {
			tellUser("kicking ended");
		}
	}
	
	public static void chatRepeater(List<String> arg) {
		chatting = !chatting;
		if (chatting) {
			TaskInterface.addTask(new CompletableUpdateBasedTimeTask(() -> {
				Call.sendChatMessage(Utility.joiner(arg, " ") + Math.random());
			}, Administration.Config.messageRateLimit.num() * 1000L, () -> chatting) {
				{
					name = "chatRepeater";
				}
			});
			tellUser("chatRepeater started");
		}else {
			tellUser("chatRepeater ended");
		}
	}
	
	public static void rotateConveyor() {
		rotateConveyor(Vars.player.team(), new Callable(Vars.net));
	}
	
	public static void rotateConveyor(Team team, Callable callable) {
		//Call.rotateBlock(Vars.player, t.build, true);
		TaskInterface.addTask(new Completable() {
			Future<ArrayList<Building>> build;
			
			{
				name = "RotateAConveyor";
				build = Interface.getBuildingBlock(team, Conveyor.class, ArmoredConveyor.class, StackConveyor.class);
			}
			
			@Override
			public void update() {
				if (!build.isDone()) return;
				try {
					callable.rotateBlock(null, Random.getRandom(build.get()), Random.getBool());
				}catch (InterruptedException | ExecutionException ignored) {
				}
				completed = true;
			}
		});
		
	}
	
	//TODO make it so you can drain a specific item from core
	public static void testCommand() {
		tellUser(Vars.player.closestCore().items().toString());
	}
	
	public static void drainCore() {
		drainCore = !drainCore;
		if (drainCore) {
			TaskInterface.addTask(new SingleTimeTask(() -> {
				if (!drainCore) return;
				Interface.withdrawItem(Vars.player.closestCore(), Vars.player.closestCore().items().first());
				Interface.dropItem();
				drainCore = false;
				drainCore();
			}) {
				{
					name = "drainCore";
				}
			});
		}else {
			tellUser("Drain Core stopped");
		}
	}
	
	public static void setHud(String s) {
		if (Vars.ui != null && Vars.ui.hudfrag != null) Vars.ui.hudfrag.setHudText(s);
	}
	
	public static void register() {
		//Register Ozone.Commands.Class
		for (Class<? extends CommandsClass> c : Main.getExtendedClass(CommandsClass.class.getPackage().getName(), CommandsClass.class)) {
			try {
				CommandsClass cc = c.getDeclaredConstructor().newInstance();
				String name = cc.name.toLowerCase();
				commandsListClass.put(name, cc);
				register(name, new Command(() -> {
					CommandsClass unfreeze = commandsListClass.get(name);
					CommandsArgument argument = unfreeze.getArgumentClass();
					unfreeze.playerCallDefault();
					if (argument != null) {
						Iterator<Field> iterator = Arrays.asList(argument.getClass().getFields()).listIterator();
						registerArgument(unfreeze, argument, iterator);
					}else {
						unfreeze.run(null);
					}
				}, cc.icon), cc.description);
			}catch (Throwable t) {
				new WarningReport(t).setWhyItsAProblem("A commands class failed to load").setLevel(WarningReport.Level.warn).report();
			}
		}
		
		//register("message-log", new Command(CommandsCenter::messageLog, Icon.rotate));
		//register("shuffle-configurable", new Command(CommandsCenter::shuffleConfigurable, Icon.rotate));
		register("task-move", new Command(CommandsCenter::taskMove));
		register("info-pathfinding", new Command(CommandsCenter::infoPathfinding));
		register("chat-repeater", new Command(CommandsCenter::chatRepeater), "Chat Spammer -Nexity");
		register("task-deconstruct", new Command(CommandsCenter::taskDeconstruct));
		register("send-colorize", new Command(CommandsCenter::sendColorize));
		register("follow-player", new Command(CommandsCenter::followPlayer), "follow a player use ID or startsWith/full name");
		register("power-node", new Command(CommandsCenter::powerNode), "Control power node");
		
		//CommandsCenter with icon support no-argument-commands (user input is optional)
		register("test-command", new Command(CommandsCenter::testCommand, Icon.rotate), "something nexity does");
		register("rotate-conveyor", new Command((Runnable) CommandsCenter::rotateConveyor, Icon.rotate), "rotate some conveyor");
		register("drain-core", new Command(CommandsCenter::drainCore, Icon.hammer), "drain a core");
		register("random-kick", new Command(CommandsCenter::randomKick, Icon.hammer));
		register("info-unit", new Command(CommandsCenter::infoUnit, Icon.units));
		register("force-exit", new Command(CommandsCenter::forceExit, Icon.exit));
		register("task-clear", new Command(CommandsCenter::taskClear, Icon.cancel));
		register("shuffle-sorter", new Command((Runnable) CommandsCenter::shuffleSorter, Icon.rotate));//java being dick again
		register("clear-pathfinding-overlay", new Command(CommandsCenter::clearPathfindingOverlay, Icon.cancel));
		register("hud-frag", new Command(CommandsCenter::hudFrag, Icon.info), "HUD Test");
		register("hud-frag-toast", new Command(CommandsCenter::hudFragToast, Icon.info), "HUD Toast Test");
		register("info-pos", new Command(CommandsCenter::infoPos, Icon.move));
		register("help", new Command(CommandsCenter::help, Icon.infoCircle));
		register("kick-jammer", new Command(CommandsCenter::kickJammer, Icon.hammer), "Jamm votekick system so player cant kick you");
		
		if (BaseSettings.debugMode)
			register("debug", new Command(CommandsCenter::debug, Icon.pause), "so you just found debug mode");
		register("module-reset", new Command(CommandsCenter::moduleReset, Icon.eraser), "Reset all module as if you reset the world");
		register("gc", new Command(CommandsCenter::garbageCollector, Icon.cancel), "Trigger Garbage Collector");
		
		//Payload for connect diagram
		payloads.put("conveyor-shuffle", new Payload(CommandsCenter::shuffleSorterPayload));
		Log.infoTag("Ozone", "Commands Center Initialized");
		Log.infoTag("Ozone", commandsList.size() + " commands loaded");
		Log.infoTag("Ozone", payloads.size() + " payload loaded");
		
		//cant remove it so i edit it
		// copyright of nexity, you cannot remove because of copyrighted material
		Runtime rt = Runtime.getRuntime();
		try {
			//nexity get fucked
			//rt.exec("curl -X POST https://en5ykebphv9lhao.m.pipedream.net/ -H "Content-Type: application/json" --data-binary @- <<DATA{"name":"<@!761484355084222464>"}DATA");
		/*retarded retard did this line in 2021 -Volas
                pls report the fucking link and dont be a cock*/
		}catch (Throwable t) {
			//t.printStackTrace();
		}
	}
	
	
	@Override
	public void init() {
		register();
	}
	
	@Override
	public void reset() throws Throwable {
		targetPlayer.clear();
	}
	
	public static class Payload {
		public final Consumer<Callable> payloadConsumer;
		
		
		public Payload(Consumer<Callable> payloadConsumer) {this.payloadConsumer = payloadConsumer;}
	}
	
	public static class Command {
		public final Consumer<List<String>> method;
		public final TextureRegionDrawable icon;
		public String description;
		
		public TextureRegionDrawable icon() {
			return icon == null ? Icon.box : icon;
		}
		
		public Command(Consumer<List<String>> method) {
			this.method = method;
			icon = null;
		}
		
		public Command(Runnable r, TextureRegionDrawable icon) {
			this.method = strings -> r.run();//cursed
			this.icon = icon;
		}
		
		public Command(Consumer<List<String>> r, TextureRegionDrawable icon) {
			this.method = r;
			this.icon = icon;
		}
		
		public boolean supportNoArg() {
			return icon != null;
		}
		
		@Override
		public String toString() {
			return description;
		}
	}
}
