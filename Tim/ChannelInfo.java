/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import org.pircbotx.Channel;

/**
 *
 * @author mwalker
 */
public class ChannelInfo {
	public Channel channel;
	public boolean isAdult;
	public boolean doMarkov;
	public boolean doRandomActions;
	public boolean doCommandActions;
	public boolean relayBotTimmy;
	public boolean relayNaNoWordSprints;
	public boolean relayNaNoWriMo;
	public boolean relayofficeduckfrank;
	public long chatterTimer;
	public int chatterMaxBaseOdds;
	public int chatterNameMultiplier;
	public int chatterTimeMultiplier;
	public int chatterTimeDivisor;

	/**
	 * Construct channel with default flags.
	 *
	 * @param name What is the name of the channel
	 */
	public ChannelInfo( Channel channel ) {
		this.channel = channel;
		this.isAdult = false;
		this.doCommandActions = true;
		this.doRandomActions = true;
		this.doMarkov = true;
		this.relayBotTimmy = false;
		this.relayNaNoWordSprints = false;
		this.relayNaNoWriMo = false;
		this.relayofficeduckfrank = false;
	}

	/**
	 * Construct channel by specifying values for flags.
	 *
	 * @param name    What is the name of the channel
	 * @param adult   Is the channel considered 'adult'
	 * @param markhov Should markov chain processing and generation happen on channel
	 * @param random  Should random actions happen on this channel
	 * @param command Should 'fun' commands be processed on channel
	 */
	public ChannelInfo( Channel channel, boolean adult, boolean markhov, boolean random, boolean command, boolean relayBotTimmy, boolean relayNaNoWordSprints, boolean relayNaNoWriMo, boolean relayofficeduckfrank ) {
		this.channel = channel;
		this.isAdult = adult;
		this.doRandomActions = random;
		this.doCommandActions = command;
		this.doMarkov = markhov;
		this.relayBotTimmy = relayBotTimmy;
		this.relayNaNoWordSprints = relayNaNoWordSprints;
		this.relayNaNoWriMo = relayNaNoWriMo;
		this.relayofficeduckfrank = relayofficeduckfrank;
	}

	public void setChatterTimers( int maxBaseOdds, int nameMultiplier, int timeMultiplier, int timeDivisor ) {
		this.chatterMaxBaseOdds = maxBaseOdds;
		this.chatterNameMultiplier = nameMultiplier;
		this.chatterTimeMultiplier = timeMultiplier;
		this.chatterTimeDivisor = timeDivisor;

		if (this.chatterMaxBaseOdds == 0) {
			this.chatterMaxBaseOdds = 20;
		}

		if (this.chatterNameMultiplier == 0) {
			this.chatterNameMultiplier = 4;
		}

		if (this.chatterTimeMultiplier == 0) {
			this.chatterTimeMultiplier = 4;
		}

		if (this.chatterTimeDivisor == 0) {
			this.chatterTimeDivisor = 2;
		}

		this.chatterTimer = System.currentTimeMillis() / 1000;
	}
	
	@Override
	public String toString() {
		String description;
	
		description = "Name: " + this.channel.getName();
		description += "  adult: " + isAdult;
		description += "  random: " + doRandomActions;
		description += "  command: " + doCommandActions;
		description += "  markov: " + doMarkov;
		description += "  bottimmy: " + relayBotTimmy;
		description += "  wordsprints: " + relayNaNoWordSprints;
		description += "  nanowrimo: " + relayNaNoWriMo;
		description += "  officeduck: " + relayofficeduckfrank;
		
		return description;
	}
}
