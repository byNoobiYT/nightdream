/*
 * Copyright (c) JDiscordBots 2019
 * File: NightDream.java
 * Project: NightDream
 * Licensed under Boost Software License 1.0
 */

package io.github.jdiscordbots.nightdream.core;

import io.github.jdiscordbots.nightdream.commands.BotCommand;
import io.github.jdiscordbots.nightdream.commands.Command;
import io.github.jdiscordbots.nightdream.listeners.BotListener;
import io.github.jdiscordbots.nightdream.logging.LogType;
import io.github.jdiscordbots.nightdream.logging.NDLogger;
import io.github.jdiscordbots.nightdream.util.BotData;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.JDAImpl;
import org.reflections.Reflections;

import javax.security.auth.login.LoginException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class NightDream {

	private static final String ANNOTATED_WITH=" is annotated with @";
	public static final String VERSION = "0.0.4";
	
	private static final NDLogger LOG=NDLogger.getLogger("System");
	private static final NDLogger CMD_CTL_LOG=NDLogger.getLogger("Command Handler");
	private static final NDLogger DISCORD_CTL_LOG=NDLogger.getLogger("Discord");
	
	public static ShardManager initialize() {
		final DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder(BotData.getToken())
			.setAutoReconnect(true) //should the Bot reconnect?
			.setStatus(OnlineStatus.ONLINE) //the online Status
			/*	possible statuses:
				OnlineStatus.DO_NOT_DISTURB
				OnlineStatus.IDLE
				OnlineStatus.INVISIBLE
				OnlineStatus.ONLINE
				OnlineStatus.OFFLINE
				OnlineStatus.UNKNOWN
			*/
			.setActivity(Activity.playing(BotData.getDefaultPrefix() + "help | " + BotData.getGame())) //the name of the game the Bot is "playing"
			/*
				Activity.playing(String)//playing...
				Activity.listening(String)//listening...
				Activity.streaming(String, String)//streaming...(with url)
				Activity.watching(String)//watching...
			*/
			.setRequestTimeoutRetry(true);
		ShardManager bot=null;
		try {
			
			

			// initialize commands and listeners
			Reflections ref = new Reflections("io.github.jdiscordbots.nightdream");
			CMD_CTL_LOG.log(LogType.INFO, "Loading Commands and Listeners...");
			addCommandsAndListeners(ref, builder);
			CMD_CTL_LOG.log(LogType.INFO, "Loaded Commands and Listeners");
			CMD_CTL_LOG.log(LogType.INFO, "available Commands: "
					+ CommandHandler.getCommands().keySet().stream().collect(Collectors.joining(", ")));
			bot = builder.build();
			DISCORD_CTL_LOG.log(LogType.INFO, "Logging in with "+bot.getShardsTotal()+" shards...");
			bot.getShards().forEach(jda->{
				try {
					jda.awaitReady();
					((JDAImpl) jda).getGuildSetupController().clearCache();
				} catch (InterruptedException e) {
					DISCORD_CTL_LOG.log(LogType.WARN,"The main thread was interruped while waiting for a shard to connect initially",e);
					Thread.currentThread().interrupt();
				}
			});
			DISCORD_CTL_LOG.log(LogType.INFO, "Logged in. "+bot.getShardsRunning()+"/"+bot.getShardsTotal()+" shards online.");
			
		} catch (final LoginException e) {
			DISCORD_CTL_LOG.log(LogType.ERROR, "The entered token is not valid!");
		} catch (final IllegalArgumentException e) {
			DISCORD_CTL_LOG.log(LogType.ERROR, "There is no token entered!");
		}
		return bot;
	}
	public static void main(String[] args) {
		initialize();
	}
	
	/**
	 * adds Commands and Listeners
	 * @param ref The {@link Reflections} Object
	 * @param builder The Builder of the JDA objects(shards)
	 */
	private static void addCommandsAndListeners(Reflections ref,DefaultShardManagerBuilder builder) {
		addAction(ref, BotCommand.class,(cmdAsAnnotation,annotatedAsObject)->{
    		BotCommand cmdAsBotCommand = (BotCommand)cmdAsAnnotation;
    		Command cmd = (Command)annotatedAsObject;
    		for (String alias : cmdAsBotCommand.value()) {
				CommandHandler.addCommand(alias.toLowerCase(), cmd);
			}
		});
		addAction(ref, BotListener.class,(cmdAsAnnotation,annotatedAsObject)->{
    		ListenerAdapter listener = (ListenerAdapter) annotatedAsObject;
    		builder.addEventListeners(listener);
    	});
	}
	/**
	 * invokes Method Objects of all Classes from that are annotated with a specified {@link Annotation}
	 * @param ref The {@link Reflections} Object that scanned the Classes
	 * @param annotClass The Class Object of the Annotation
	 * @param function the code to be executed with every annotated Class
	 */
	private static void addAction(Reflections ref, Class<? extends Annotation> annotClass, BiConsumer<Annotation, Object> function) {
		for (Class<?> cl : ref.getTypesAnnotatedWith(annotClass,true)) {
            try {
				Object annotatedAsObject = cl.getDeclaredConstructor().newInstance();
				Annotation cmdAsAnnotation = cl.getAnnotation(annotClass);
				function.accept(cmdAsAnnotation, annotatedAsObject);
			} catch (InstantiationException e) {
				addActionWarn(cl,annotClass,"cannot be instantiated");
			} catch (IllegalAccessException e) {
				addActionWarn(cl,annotClass,"the no-args constructor is not visible");
			} catch (NoSuchMethodException e) {
				addActionWarn(cl,annotClass,"there is no no-args constructor");
			} catch (InvocationTargetException e) {
				addActionWarn(cl,annotClass,"there was an unknown Error: " + e.getClass().getName()+": "+e.getCause());
			}
        }
    }
	private static void addActionWarn(Class<?> cl,Class<? extends Annotation> annotClass,String err) {
		LOG.log(LogType.WARN,cl.getName() + ANNOTATED_WITH + annotClass.getName() + " but "+err);
	}
}
