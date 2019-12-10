/*
 * Copyright (c) JDiscordBots 2019
 * File: Lyrics.java
 * Project: NightDream
 * Licensed under Boost Software License 1.0
 */

package io.github.jdiscordbots.nightdream.commands.ksoft;

import io.github.jdiscordbots.nightdream.commands.BotCommand;
import io.github.jdiscordbots.nightdream.commands.Command;
import io.github.jdiscordbots.nightdream.util.JDAUtils;
import io.github.jdiscordbots.nightdream.util.KSoftUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.explodingbush.ksoftapi.KSoftAPI;
import net.explodingbush.ksoftapi.entities.lyrics.Album;
import net.explodingbush.ksoftapi.entities.lyrics.Track;

import java.awt.Color;
import java.util.OptionalInt;
import java.util.stream.Collectors;

@BotCommand("lyrics")
public class Lyrics implements Command {
	@Override
	public void action(String[] args, GuildMessageReceivedEvent event) {
		KSoftAPI api = KSoftUtil.getApi();
		if(api==null) {
			JDAUtils.errmsg(event.getChannel(), "This command is disabled due there is no KSoft API token");
			return;
		}
		
		if(args.length==0) {
			JDAUtils.errmsg(event.getChannel(), "not enough arguments");
			return;
		}
		
		event.getChannel().sendTyping().queue();
		String query=String.join(" ", args);
		Track track = api.getLyrics().search(query).execute().get(0);
		if(track==null) {
			JDAUtils.errmsg(event.getChannel(), "not found");
			return;
		}
		OptionalInt released=track.getAlbums().stream().mapToInt(Album::getReleaseYear).min();
		EmbedBuilder builder=new EmbedBuilder();
		String lyrics=track.getLyrics();
		builder.setColor(Color.white)
		.setFooter("Results from KSoft.Si API")
		.setTitle("Found something :mag:");
		builder.addField("Artist: "+track.getArtist().getName(),"Album: "+track.getAlbums().stream().map(Album::getName).collect(Collectors.joining(" / ")),false);
		builder.addField("Song: "+track.getName(), released.isPresent()?"released "+released.getAsInt():"", false);
		builder.addField("Lyrics", lyrics.length()>=300?lyrics.substring(0,300)+"\n...":lyrics, false);
		event.getChannel().sendMessage(builder.build()).queue();	
	}

	@Override
	public String help() {
		return "Seaches a song by its lyrics";
	}

	@Override
	public CommandType getType() {
		return CommandType.FUN;
	}
}
