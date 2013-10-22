/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Tim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.pircbotx.Channel;

/**
 *
 * @author Matthew Walker
 */
public class NewChannelInfo {
	public Channel channel;
	protected HashMap<String, Boolean> chatter_enabled = new HashMap<>(16);
	protected HashMap<String, Boolean> commands_enabled = new HashMap<>(16);
	protected Set<String> twitter_accounts = new HashSet<>(16);

	public long chatterTimer;
	public int chatterMaxBaseOdds;
	public int chatterNameMultiplier;
	public int chatterTimeMultiplier;
	public int chatterTimeDivisor;
	public int chatterLevel;

	public long twitterTimer;
	public int tweetBucket;
	public int tweetBucketMax;
	public int tweetBucketChargeRate;

	public NewChannelInfo(Channel channel) {
		this.channel = channel;
	}

	public void setDefaultOptions() {
		this.chatterMaxBaseOdds = 12;
		this.chatterNameMultiplier = 3;
		this.chatterTimeMultiplier = 1;
		this.chatterTimeDivisor = 2;
		this.chatterTimer = System.currentTimeMillis() / 1000;
		this.chatterLevel = 1;
		
		
	}
	
	
}
