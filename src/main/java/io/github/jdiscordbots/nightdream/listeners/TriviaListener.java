/*
 * Copyright (c) JDiscordBots 2019
 * File: TriviaListener.java
 * Project: NightDream
 * Licensed under Boost Software License 1.0
 */

package io.github.jdiscordbots.nightdream.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class TriviaListener extends ListenerAdapter {
	private static TriviaListener listener;
	private Map<String, Set<Set<String>>> questions=new HashMap<>();
	private Timer timer=new Timer();
	private JDA jda;
	private TriviaListener(JDA jda) {
		this.jda=jda;
	}
	private static TriviaListener getListener(JDA jda) {//Attention: if sharding is implemented, this will need to be adjusted(multiple JDA objects)
		synchronized(TriviaListener.class) {
			if(listener==null) {
				listener=new TriviaListener(jda);
				jda.addEventListener(listener);
			}
			return listener;
		}
		
	}
	public static Set<Set<String>> getQuestions(final MessageChannel chan) {
		return getListener(chan.getJDA()).questions.get(chan.getId());
	}
	public static void addQuestion(final MessageChannel chan,String... correct) {
		for (int i = 0; i < correct.length; i++) {
			correct[i]=correct[i].toLowerCase();
		}
		final Set<String> correctAnswers=new HashSet<>(Arrays.asList(correct));
		synchronized(TriviaListener.class) {
			getListener(chan.getJDA());
			Set<Set<String>> questions;
			if(listener.questions.containsKey(chan.getId())) {
				questions=listener.questions.get(chan.getId());
				
			}else {
				questions=new HashSet<>();
				listener.questions.put(chan.getId(), questions);
			}
			questions.add(correctAnswers);
			listener.timer.schedule(new TimerTask() {
				@Override
				public void run() {
					synchronized(TriviaListener.class) {
						if(listener!=null&&listener.questions.containsKey(chan.getId())&&listener.questions.get(chan.getId()).contains(correctAnswers)) {
							chan.sendMessage("Nobody got the answer this time. Sad.").queue();
							
							listener.questions.get(chan.getId()).remove(correctAnswers);
							if(listener.questions.get(chan.getId()).isEmpty()) {
								listener.questions.remove(chan.getId());
								if(listener.questions.isEmpty()) {
									removeListener(listener.jda);
								}
							}
						}
					}
				}
			}, 30000);
		}
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(questions.containsKey(event.getChannel().getId())) {
			synchronized (getClass()) {
				Set<String> toDelete=null;
				for (Set<String> answers : questions.get(event.getChannel().getId())) {
					if(answers.contains((event.getMessage().getContentDisplay().toLowerCase()))){
						event.getChannel().sendMessage(event.getMember().getEffectiveName()+" got it!").queue();
						toDelete=answers;
					}
				}
				if(toDelete!=null) {
					questions.get(event.getChannel().getId()).remove(toDelete);
					if(questions.get(event.getChannel().getId()).isEmpty()) {
						questions.remove(event.getChannel().getId());
						if(questions.isEmpty()) {
							timer.cancel();
							timer.purge();
							removeListener(jda);
						}
					}
				}
			}
		}
	}
	private static void removeListener(JDA jda) {
		jda.removeEventListener(TriviaListener.listener);
		TriviaListener.listener.timer.cancel();
		TriviaListener.listener=null;
	}
}
