package winterwell.jtwitter;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import winterwell.utils.TodoException;

/**
 * Connect to the streaming API.
 * <p>
 * Duplicate messages may be delivered when reconnecting to the Streaming API.
 * <p>
 * Status: This class is in an early stage, and may change.
 * @author Daniel
 */
public class TwitterStream extends AStream {

	/**
	 * Used to help avoid breaking api limits.
	 */
	private static Map<String, AStream> user2stream = new ConcurrentHashMap();

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TwitterStream");
		sb.append("["+method);
		if (track!=null) sb.append(" track:"+InternalUtils.join(track, 0, 3));
		if (follow!=null) sb.append(" follow:"+InternalUtils.join(follow, 0, 3));
		if (locns!=null) sb.append(" in:"+InternalUtils.join(locns, 0, 3));
		sb.append("]");
		return sb.toString();
	}

	@Override
	void fillInOutages2(Twitter jtwit2, Outage outage) {
		if (method != KMethod.filter) throw new UnsupportedOperationException();
		// keywords?
		if (track!=null) {
			for(String keyword : track) {
				List<Status> msgs = jtwit.search(keyword);
				for (Status status : msgs) {
					if (tweets.contains(status)) continue;
					tweets.add(status);
				}
			}
		}
		// users?
		if (follow!=null) {
			for(Long user : follow) {
				List<Status> msgs = jtwit.getUserTimeline(user);
				for (Status status : msgs) {
					if (tweets.contains(status)) continue;
					tweets.add(status);
				}
			}
		}
		// regions?
		if (locns != null && ! locns.isEmpty()) {
			throw new UnsupportedOperationException("TODO"); // TODO
		}
	}

	/**
	 *
	 * @param client This will have it's timeout set to 90 seconds.
	 * So you probably don't want to reuse the object with the REST api.
	 */
	public TwitterStream(Twitter jtwit) {
		super(jtwit);
	}

	KMethod method = KMethod.sample;

	private List<String> track;
	private List<Long> follow;
	private List<double[]> locns;

	public static enum KMethod {
		/**
		 * Follow hashtags, users or regions
		 */
		filter,

		/**
		 * Spritzer or Garden-hose: A sample of tweets, suitable for trend analysis. <br>
		 * The default level (spritzer) is roughly 1% of all public tweets. <br>
		 * The upgraded level (garden-hose - apply to Twitter for this) is 10%. <br>
		 * In both cases the algorithm is based on the tweet-id modulo 100.
		 */
		sample,

		/** Requires special access privileges! */
		links,

		/** New-style retweets. Requires special access privileges!
		 *  From dev.twitter.com: <i>Few applications require this level of access.
		 *  Creative use of a combination of other resources and various access levels
		 *  can satisfy nearly every application use case.</i>
		 *  */
		retweet,

		/** Everything! Requires special access privileges! */
		firehose
	}

	/**
	 * Set the method. The default is "sample", as this is the only one which
	 * works with no extra settings.
	 * @param method
	 */
	void setMethod(KMethod method) {
		this.method = method;
	}

	@Override
	HttpURLConnection connect2() throws Exception {
		connect3_rateLimit();

		String url = "https://stream.twitter.com/1/statuses/"+method+".json";
		Map<String, String> vars = new HashMap();
		vars.put("delimited", "length");
		if (follow!=null && ! follow.isEmpty()) {
			vars.put("follow", InternalUtils.join(follow, 0, Integer.MAX_VALUE));
		}
		if (track!=null && ! track.isEmpty()) {
			vars.put("track", InternalUtils.join(track, 0, Integer.MAX_VALUE));
		}
		// use post in case it's a long set of vars
		HttpURLConnection con = client.post2_connect(url, vars);
		return con;
	}

	/**
	 * Protect the rate limits & _help_ you avoid annoying Twitter
	 * (only locally! And forgetful! Do NOT rely on this)
	 */
	private void connect3_rateLimit() {
		if (jtwit.getScreenName() == null) return; // dunno
		AStream s = user2stream.get(jtwit.getScreenName());
		if (s != null && s.isConnected()) {
			throw new TwitterException.TooManyLogins("One account, one stream (running: "+s
					+"; trying to run"+this+").\n	But streams OR their filter parameters, so one stream can do a lot.");
		}
		// memory paranoia
		if (user2stream.size() > 1000) {
			user2stream = new ConcurrentHashMap<String, AStream>();
		}
		user2stream.put(jtwit.getScreenName(), this);		
	}

	/**
	 * , 5,000 follow userids and
	 * @param userIds
	 */
	public void setFollowUsers(List<Long> userIds) {
		method = KMethod.filter;
//		if (userIds!=null&&! userIds.isEmpty()){
		follow = userIds;
//		}
		//If not, really don't! it screws stuff up.
	}

	/**
	25 0.1-360 degree location boxes.

	Only tweets that are both created using the Geotagging API and are placed from within a tracked bounding box will be included in the stream – the user’s location field is not used to filter tweets

	@param boundingBoxes
	Each element consists of longitude/latitude south-west, north-east.
	*/
	public void setLocation(List<double[]> boundingBoxes) {
		method = KMethod.filter;
		this.locns = boundingBoxes;
		throw new TodoException();
	}

	/**
	 * See https://dev.twitter.com/docs/streaming-api/methods#track
	 * @param keywords The default access level allows up to 400 track keywords.
	 */
	public void setTrackKeywords(List<String> keywords) {
		this.track = keywords;
		method = KMethod.filter;
	}

}


