package io.github.bynoobiyt.nightdream.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import io.github.bynoobiyt.nightdream.util.GeneralUtils;
import io.github.bynoobiyt.nightdream.util.JDAUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@BotCommand("npm")
public class NPM implements Command{

	@Override
	public void action(String[] args, MessageReceivedEvent event) {
		if(args.length==0) {
			event.getTextChannel().sendMessage("<:IconProvide:553870022125027329> I need a package name").complete();
		}
		String url="http://registry.npmjs.org/"+args[0];
		try(Scanner scan=new Scanner(new URL(url).openConnection().getInputStream())){
			String json=scan.nextLine();
			String scope;
			if(args[0].startsWith("@")) {
				scope=args[0].substring(1).split("/")[0];
			}else {
				scope="undefined";
			}
			EmbedBuilder builder=new EmbedBuilder();
			builder.setColor(0xfb3b49)
			.setTitle("Result")
			.addField(new Field("name", "`"+GeneralUtils.getJSONString(json, "name")+"`", true))
			.addField(new Field("Description", GeneralUtils.getJSONString(json, "description"), true))
			.addField(new Field("Current Version", GeneralUtils.getJSONString(json, "latest"), true))
			.addField(new Field("Keywords", "`"+GeneralUtils.getMultipleJSONStrings(json, "keywords")+"`", true))
			.addField(new Field("Author", GeneralUtils.getJSONString(json, "author\":{\"name"), true))
			.addField(new Field("Scope", "`"+scope+"`", true));
			
			JDAUtils.msg(event.getTextChannel(), builder.build(),false);
		}catch(FileNotFoundException e) {
			JDAUtils.errmsg(event.getTextChannel(), "Not found");
		}catch (IOException e) {
			JDAUtils.errmsg(event.getTextChannel(), "An error occured.");
			e.printStackTrace();
		}
	}
	@Override
	public String help() {
		return "Allows you to view info about an NPM package";
	}
	
}
