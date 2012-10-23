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
	public boolean doMarkhov;
	public boolean doRandomActions;
	public boolean doCommandActions;
	public boolean relayTwitter;
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
		this.doMarkhov = true;
		this.relayTwitter = false;
	}

	/**
	 * Construct channel by specifying values for flags.
	 *
	 * @param name    What is the name of the channel
	 * @param adult   Is the channel considered 'adult'
	 * @param markhov Should markhov chain processing and generation happen on channel
	 * @param random  Should random actions happen on this channel
	 * @param command Should 'fun' commands be processed on channel
	 */
	public ChannelInfo( Channel channel, boolean adult, boolean markhov, boolean random, boolean command, boolean relay ) {
		this.channel = channel;
		this.isAdult = adult;
		this.doRandomActions = random;
		this.doCommandActions = command;
		this.doMarkhov = markhov;
		this.relayTwitter = relay;
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
}