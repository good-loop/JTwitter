package winterwell.jtwitter;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;

import org.junit.Test;

import com.winterwell.json.JSONException;
import com.winterwell.json.JSONObject;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import junit.framework.TestCase;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.KEntityType;
import winterwell.jtwitter.Twitter.KRequestType;
import winterwell.jtwitter.Twitter.TweetEntity;
import winterwell.jtwitter.TwitterException.E403;
import winterwell.jtwitter.TwitterException.E404;

/*
RM NOTES TO SELF 2019-10-17

DONE testUpdateStatusSpecialChars: added salt against repetition error

DONE testUpdateStatusLength: needs big rework for DRY & 280 but was comparing BigInteger with ==
DONE testFriendIDs: buncha deprecated shit, big refactor on isFollower
DONE testCanSend160: salted tweet string so sequential tests don't fail
DONE testSendMessage: sendMessage needs user id now
DONE testUpdateStatusUnicode: message text salt, was comparing BigInteger IDs with ==
DONE testGetUserTimelineString: was using @anonpoetry which has deleted all its tweets, switched to @berkherkson
DONE testReQuote: Repetition error, needs salt
DONE testRetweet: Repetition error, needs salt or un-RT
DONE testDoesBeckiFollowCoop: @coopbankuk_help doesn't exist any more, changed users
DONE testSendMention2: Tries to retrieve messages using getMentions but doesn't wait long enough - increased timeout
DONE testFollowerIds: refactor, see testFriendIds
DONE testSendMessageToSelf: see testSendMessage
DONE testStopFollowing: doesn't return null for stop following not-followed user, may be not-a-bug
DONE testGetUserTimelineWithRetweets: RTs @stephenfry for test, fails on sequential runs. un-rt
DONE testGetStatusLong: dakalom_ doesn't exist any more, using a Berk tweet instead
DONE testDirectMessage2: 1: DMs don't take @ on username 2: jtwittest2 wasn't following jtwit
DONE testRepetitionRetweet: New class of 403 error (code 327)
DONE testGetSetFavorite: API changed, double-unfav throws 404 now
DONE testGetDirectMessages: Was throwing NPEs as recipient is unpopulated for most msgs (recipientID is present) --> was failing because DM endpoint latency is huge --> now stores ID of sent message & retrieves as ID by recipient to confirm sending worked
DONE testGetDupeLinks: We have a canned copy, but the original tweet is long gone...

WONTFIX testIdenticaAccess: The identi.ca API is not what it used to be.
WONTFIX testIdentica: The identi.ca API is not what it used to be.
WONTFIX testIssue4280: @samdaat doesn't exist, fuck this test
WONTFIX testMarakana: not a thing

DONE testBigVideoUpload: can't find big video file
DONE testLargerVideoUpload: Fixed relative file path (still only works for WW/GL devs, but...)
DONE testSmallShortVideoUpload: Fixed relative file path (still only works for WW/GL devs, but...)
testPostToTwitterWithMedia: Switched to image files that exist




*/

/**
 * Unit tests for JTwitter.
 * These only provide partial testing -- sorry.
 *
 *
 * @author daniel
 */
public class TwitterTest
extends TestCase // Comment out to remove the JUnit dependency
{	

	/**
	 * Bug seen with Kolbi Aug 2019
	 */
	public void testExcludeIDsBug() {
		Twitter jtwit = newTestTwitter();
		Map vars = new HashMap();
		List excludeReplyIds = Arrays.asList(0, 187567, 2.3, "@winterstein", "12");
		jtwit.updateStatus3_vars2_autopop(vars, excludeReplyIds);
		String exc = (String) vars.get("exclude_reply_user_ids");
		assert exc.contains("187567");
		assert exc.contains("2");
		assert ! exc.contains("winter");
	}
	
	public void testQuotedRetweet() {
		Twitter jtwit = newTestTwitter();
		// http://twitter.com/narrator/status/748823821231218688
		Status s = jtwit.getStatus(new BigInteger("748823821231218688"));
		System.out.println(s);
		assert s.getOriginal() != null;
		assert s.isQuotedStatus();
//		https://twitter.com/Danacea/status/748812973389385728
	}
	
	public void testCountCharacters() {
		{
			int cnt = Twitter.countCharacters("The perfect way to spend a Sunday... We suggest a trip to Columbia Road Flower Market. #HarrodsLovesLondon");
			assert cnt == 106;
		}
	}
	
	public void testGetTaxiUK() {
		Twitter jtwit = newTestTwitter();
		User user = jtwit.users().show(305506258L);
		System.out.println(user);
	}
	

	public void testReQuote() {
		Twitter jtwit = newTestTwitter();
		Status danSaid = jtwit.show("winterstein").status;
		String salt = new Random().nextInt(1000)+" ";
		Status s = jtwit.retweetWithComment(danSaid, salt + "Interesting");		
		System.out.println(jtwit+" "+s+" "+s.getUrl());
	}
	
	/**
	 * coopbankuk_help no longer exists - changing this from
	 * @beckishort and @coopbankuk_help to @berkherkson and @forkmcguffin
	 */
	public void testDoesBeckiFollowCoop() {
		Twitter jtwit = newTestTwitter();
		boolean f = jtwit.users().isFollower("@berkherkson", "@forkmcguffin");
		boolean f2 = jtwit.users().isFollower("@forkmcguffin", "@berkherkson");
		System.out.println(f);
		System.out.println(f2);
		assert f && f2;
	}
	
	public void testClientClone() {
		Twitter jtwit = newTestTwitter();
		OAuthSignpostClient c = (OAuthSignpostClient) jtwit.getHttpClient();
		IHttpClient c2 = c.copy();
		Twitter jtwit2 = new Twitter(null, c2);
		System.out.println(jtwit2.getStatus());
	}
	
	public void testDisplayTextBug() {
		Twitter jtwit = newTestTwitter();
				
		Status _tweet = jtwit.getStatus(new BigInteger("255592522955505665"));
		System.out.println(_tweet.getId());
		System.out.println(_tweet.getText());
		System.out.println(_tweet.getTweetEntities(KEntityType.urls));
		System.out.println(_tweet.getDisplayText());
		System.out.println();
		
		List<Status> tweets = jtwit.search("USEFUL Printable diary calendar decluttering lists Village Voices");
		for (Status tweet : tweets) {
			System.out.println(tweet.getId());
			System.out.println(tweet.getText());
			System.out.println(tweet.getTweetEntities(KEntityType.urls));
			System.out.println(tweet.getDisplayText());
			System.out.println();
		}
	}
	
	public void testSource() {
		Twitter jtwit = newTestTwitter();

		List<Status> tweets = jtwit.getUserTimeline("johnnieingram");
		for (Status status : tweets) {
			System.out.println(status.getSource()+"\t"+status.getCreatedAt());
		}
		if (true) return;
		
		HashMap<String,Integer> distro = new HashMap();
		TwitterStream ts = null;
		try {
			ts = new TwitterStream(jtwit);
			ts.connect();
			Utils.sleep(500);
			for (ITweet status : ts.popTweets()) {
				String s = ((Status) status).getSource();
				Containers.plus(distro, s, 1);
			}			
		} catch(Exception ex) {
			ex.printStackTrace();
		} finally {
			ts.close();
		}
		assert ! distro.isEmpty();
		Collection<String> keys = distro.keySet();
		for (String app : keys) {
			System.out.println(app+"\t"+distro.get(app));
		}
	}
	
	public void testHttpHttp() {
		{
			Twitter jtwit = newTestTwitter();
			BigInteger id = new BigInteger("213657059722407936");;
			Status s = jtwit.getStatus(id);
	//		assert s.getText().equals(tweet) : s.getText();
			String sdt = s.getDisplayText();
			System.out.println(sdt);
			s.getDisplayText();
			assert sdt.equals("Good for M&S -- they've become carbon neutral http://soda.sh/xVJ @guardian");			
		}
		{
			Twitter jtwit = newTestTwitter();
			BigInteger id = new BigInteger("213657059722407936");;
			Status s = jtwit.getStatus(id);
	//		assert s.getText().equals(tweet) : s.getText();
			String sdt = s.getDisplayText();
			System.out.println(sdt);
			s.getDisplayText();
			assert sdt.equals("Good for M&S -- they've become carbon neutral http://soda.sh/xVJ @guardian");
		}
	}
	
	public void testAPIStatus() throws Exception {
		// just a smoke test
		Map<String, Double> map = Twitter.getAPIStatus();
		System.out.println(map);
	}
	
	public void testUpdateConfig() {
		// just a smoke test
		Twitter jtwit = newTestTwitter();		
		jtwit.updateConfiguration();
		System.out.println("Link length: "+Twitter.LINK_LENGTH);
		System.out.println("Photo limit: "+Twitter.PHOTO_SIZE_LIMIT);
		System.out.println("Media length: "+Twitter.MEDIA_LENGTH);
	}
	
	public void testHasURL(){
		String statusText1a = "hello, I like http://www.google.com";
		String statusText1b = "hello, I like google.com";
		String statusText1c = "hello, I love herring";
		
		Matcher m2a = Regex.VALID_URL.matcher(statusText1a);
		Matcher m2b = Regex.VALID_URL.matcher(statusText1b);
		Matcher m2c = Regex.VALID_URL.matcher(statusText1c);

		assert m2a.find();
		assert m2b.find();
		assert !m2c.find();

		Printer.out("Start" +m2b.start());
		Printer.out("End" +m2b.end());

	}
	
	/**
	 * A random user who protects their tweets.
	 */
	static String PROTECTED_USER = "berkprivate";
	
	/**
	 * Location + OR = Twitter API fail -- July 2011
	 * But Location + OR + -term = success.
	 * Strange.
	 */
	public void testSearchBug() {
		Twitter ts = newTestTwitter();
		// London
		ts.setSearchLocation(51.506321, 0, "50km");
		
		// this will contain gibberish if we don't have Twitter.search2_bugHack enabled
		List<Status> tweets = ts.search("apple OR pear");
		for (Status status : tweets) {
			String text = status.getDisplayText().toLowerCase()+" "+status.getUser().screenName.toLowerCase();
			assert text.contains("apple") || text.contains("pear") || text.contains("http") : text;
		}
		
		List<Status> tweets2 = ts.search("apple OR pear -kxq -http");
		for (Status status : tweets2) {
			String text = status.getDisplayText().toLowerCase()+" "+status.getUser().screenName.toLowerCase();
			assert text.contains("apple") || text.contains("pear") || text.contains("http") : text;
		}
	}
	
	public void testCornerCaseNastiness(){
		Twitter ts = newTestTwitter();
		
		List<Status> tweets = ts.search("\"Justin Beiber\" OR \"solar energy\"");
		assert tweets.isEmpty()==false;
		
		List<Status> tweets2 = ts.search("\"Justin Beiber\" OR \"solar energy\" -kxq");
		assert tweets2.isEmpty()==false;
		
		assert tweets.size() == tweets2.size();
	}
	
	/**
	 * "apples" OR "pears" = Twitter API fail -- 24th AUgust 2011
	 */
	public void testSearchBug2() {
		Twitter ts = newTestTwitter();
		// this will work
		List<Status> tweets1 = ts.search("apple OR pear");
		// this will only have apples AND or AND pear! (without the search2_bugHack in 
		List<Status> tweets2 = ts.search("\"apple\" OR \"pear\"");
		// these are OK
		List<Status> tweets2a = ts.search("apple OR \"pear\"");
		List<Status> tweets2b = ts.search("\"apple\" OR pear");
		List<Status> tweets2c = ts.search("\"apple\" OR \"pear\" -fxz");
		// this is fine
		List<Status> tweets3 = ts.search("apple OR \"orange\" OR \"pear\"");
		// works
		List<Status> tweets3a = ts.search("\"apple\" OR \"orange\" OR pear");
		// fails
		List<Status> tweets3b = ts.search("\"apple\" OR pear OR \"orange\"");
		// fails
		List<Status> tweets3c = ts.search("\"apple\" OR (pear -fxz) OR \"orange\"");
		// fails
		List<Status> tweets3d = ts.search("\"apple\" OR pear OR \"orange\" -\"fxz\"");
		// 3a and 3b should be equivalent!
		assert tweets3b.size() == tweets3a.size();
		// OK - it looks like the bug only affects searches which use OR and start and end with quotes
	}
	
	
	public void testGetListsContaining() {
		Twitter jtwit = newTestTwitter();
		List<TwitterList> lists = jtwit.getListsContaining("patrickharvie", false);
		System.out.println(lists);
	}

	public void testParsingLocation() {
//		String json = "[252059223,19082904,12435562,18881316,72806554,213104665]";
//		JSONArray arr = new JSONArray(json);

		String locn = "ÜT: 37.892943,-122.270439";
		Matcher m = InternalUtils.latLongLocn.matcher(locn);
		assert m.matches();
		assert m.group(2).equals("37.892943");
		assert m.group(3).equals("-122.270439");
	}
	
	public void testParseDate() {
		Date date = InternalUtils.parseDate(""+System.currentTimeMillis());
		assert date != null;
		assert Math.abs(System.currentTimeMillis() - date.getTime()) < 1000;
		date = InternalUtils.parseDate("Wed Aug 24 11:54:46 +0000 2011");
	}

	/**
	 * Previously, attempting to /destroy a friendship that didn't exist
	 * made Twitter throw an exception & returned null - now Twitter just
	 * returns the User object as if you'd called /destroy successfully
	 * So now we use isFollower to test for successful creation/destruction 
	 */
	public void testStopFollowing() {
		Twitter tw = newTestTwitter();
		{
			User bieber = new User("justinbieber");
			tw.users().follow(bieber);
			Utils.sleep(100);
			assert tw.users().isFollower(tw.getScreenName(), bieber.getScreenName());
			tw.users().stopFollowing(bieber);
			Utils.sleep(100);
			assert !tw.users().isFollower(tw.getScreenName(), bieber.getScreenName());
		}
		{	// Test return null for no-op
			User charlie = new User("charliesheen");
			tw.users().stopFollowing(charlie);
			Utils.sleep(100);
			assert !tw.users().isFollower(tw.getScreenName(), charlie.getScreenName());
			tw.users().stopFollowing(charlie);
			Utils.sleep(100);
			assert !tw.users().isFollower(tw.getScreenName(), charlie.getScreenName());
		}
	}

	public void testgetRateLimitStatus() {
		Twitter tw = newTestTwitter();
		int i = tw.getRateLimitStatus();
		System.out.println("RateLimit: "+i);
	}
	
	public void testRateLimits() {
		Twitter tw = newTestTwitter();
		{
			tw.search("stuff");
			Object rateLimit = tw.getRateLimit(KRequestType.SEARCH);
			System.out.println(rateLimit);
		}
		{
			tw.show("winterstein");
			Object rateLimit = tw.getRateLimit(KRequestType.SHOW_USER);
			System.out.println(rateLimit);
		}
		{
			tw.getStatus("winterstein");
			Object rateLimit = tw.getRateLimit(KRequestType.NORMAL);
			System.out.println(rateLimit);
		}
	}


	public void testNewestFirstSorting() {
		Twitter tw = newTestTwitter();
		List<Status> tweets = tw.getUserTimeline("winterstein");
		Collections.sort(tweets, InternalUtils.NEWEST_FIRST);
		Date prev=null;
		System.out.println(tweets);
		for (Status status : tweets) {
			assert prev==null || status.getCreatedAt().before(prev) : prev+" vs "+status.getCreatedAt();
			prev = status.getCreatedAt();
		}
	}


	public void testIdFilteredSearch() {
		// The IDs used here fell out of Twitter's supported time-window (of course)
//		Twitter tw = newTestTwitter();
//		tw.setSinceId(new BigInteger("255406480277262336")); // 8th Oct 9:37pm
//		tw.setUntilId(new BigInteger("255773053018054657")); // 9th Oct 9:53pm
//		tw.setMaxResults(100);		
//		List<Status> tweets = tw.search("\"british gas\" lang:en");
//		assert ! tweets.isEmpty();
//		System.out.println(tweets.size());
//		for (Status status : tweets) {
//			Time time = new Time(status.getCreatedAt());
//			System.out.println(time);
//		}
	}
	
	
	public void testDateFilteredSearch() {
		Twitter tw = newTestTwitter();
		Time s = new Time().minus(5, TUnit.DAY);
		Time e = new Time().minus(1, TUnit.DAY);
		tw.setSinceDate(s.getDate());
		tw.setUntilDate(e.getDate());
//		tw.setUntilId(new BigInteger("255780110832107520")); Fallen out of the window
		tw.setMaxResults(100);		
		List<Status> tweets = tw.search("edinburgh tech");
		assert ! tweets.isEmpty();
		System.out.println(tweets.size());
		for (Status status : tweets) {
			Time time = new Time(status.getCreatedAt());
			assert time.isAfter(s) : time;
			assert time.isBefore(e) : time;
		}
	}
	
	public void testSinceId() {
//		investigating URI uri = new URI("http://api.twitter.com/1/statuses/replies.json?since_id=22090245178&?since_id=22090245178&");
		Twitter tw = newTestTwitter();
		tw.setSinceId(22090245178L);
		tw.setMaxResults(30);
		List<Status> tweets = tw.getUserTimeline();
		assert tweets.size() != 0;
	}

	public void testJSON() throws JSONException {
		String lng = "10765432100123456789";
//		Long itsLong = 10765432100123456789L;
		BigInteger bi = new BigInteger(lng);
		long bil = bi.longValue();
//		Long itsLong2 = Long.parseLong(lng);
		String s = "{\"id\": 10765432100123456789, \"id_str\": \"10765432100123456789\"}";
//		Map map = (Map) WebUtils2.parseJSON(s);
		JSONObject jo = new JSONObject(s);
//		Object joid = jo.get("id");
//		String ids = jo.getString("id_str");
		assertEquals(""+new BigInteger(lng), jo.getString("id_str"));
	}


	public void testGetFavorites() throws InterruptedException {
		Twitter twitter = newTestTwitter();
		{
			List<Status> fs = twitter.getFavorites();
			System.out.println(fs);
		}
		{
			List<Status> fs = twitter.getFavorites("winterstein");
			System.out.println(fs);
		}
	}
	
	/** 
	 * RM Oct 2019: Calling /favorites/destroy on an unfaved tweet will return a 404
	 * ...but calling /favorites/create on a faved tweet won't.
	 * The old comments on this test stated it was the other way around.
	 * 
	 * @throws InterruptedException
	 */
	public void testGetSetFavorite() throws InterruptedException {
		boolean doubleFavFails = false;
		boolean doubleUnfavFails = false;
		
		Twitter twitter = newTestTwitter();
		
		Status s = twitter.getStatus("winterstein");
		System.out.println(s);
		
		// Unfavorite if it's already favorited so we know where we're starting
		if (s.isFavorite()) {
			twitter.setFavorite(s, false);
			Thread.sleep(1000);
		}
		
		// Confirm favorite works
		Status s2 = twitter.setFavorite(s, true);
		assert s2.isFavorite();
		Thread.sleep(5000);
		// Check if double-fav throws an exception (it shouldn't - but the API might change)
		try {
			twitter.setFavorite(s, true);
		} catch (TwitterException e) {
			doubleFavFails = true;
		}
		
		// Fetch a fresh copy of the tweet and confirm it's favorited
		Status s3 = twitter.getStatus(s.id);
		assert s3.isFavorite();
		
		// Confirm unfavorite works
		Status s4 = twitter.setFavorite(s, false);
		assert !s4.isFavorite();
		Thread.sleep(5000);
		// Check if double-unfav throws an exception (it should - but the API might change)
		try {
			twitter.setFavorite(s, false);
		} catch (TwitterException e) {
			doubleUnfavFails = true;
		}
		
		// If these asserts fail the API has changed - just take note and rewrite the test
		assert !doubleFavFails;
		assert doubleUnfavFails;
	}

	

	public void testRepetitionSetStatus() {
		Twitter twitter = newTestTwitter();
		int salt = new Random().nextInt(100);
		Status s1 = twitter.updateStatus("repetitive tweet "+salt);
		try {
			Status s2 = twitter.updateStatus("repetitive tweet "+salt);
			assert false;
		} catch (TwitterException.Repetition e) {
			assert true;
		}
	}

	public void testRepetitionRetweet() {
		Twitter twitter = newTestTwitter();
		Status tweet = twitter.getStatus("winterstein");
		assert tweet != null;
		try {
			Status s = twitter.getStatus();
			Status s1 = twitter.retweet(tweet);
			Status sb = twitter.getStatus();
			Status s2 = twitter.retweet(tweet);
			assert false;
		} catch (TwitterException.RepeatRetweet e) {
			assert true;
		}
	}

	/** TODO This test fails because the Tweet referenced by ID in the second half is by a suspended user.
	 * Ask DW if he remembers what exactly it was testing for, probably remove. RM Oct 2019
	 */
	public void testGetDupeLinks(){
		{	// canned json
			String json = "{\"created_at\":\"Wed Jul 04 06:38:00 +0000 2012\",\"id\":220406085545242624,\"id_str\":\"220406085545242624\",\"text\":\"RT: Stomach bugs to rise during Olympics: scientists - http:\\/\\/t.co\\/juz2kA1L\\\"&gt;http:\\/\\/t.co\\/juz2kA1L http:\\/\\/t.... http:\\/\\/t.co\\/6q3iWaCf\",\"source\":\"\\u003ca href=\\\"http:\\/\\/twitterfeed.com\\\" rel=\\\"nofollow\\\"\\u003etwitterfeed\\u003c\\/a\\u003e\",\"truncated\":false,\"in_reply_to_status_id\":null,\"in_reply_to_status_id_str\":null,\"in_reply_to_user_id\":null,\"in_reply_to_user_id_str\":null,\"in_reply_to_screen_name\":null,\"user\":{\"id\":372713961,\"id_str\":\"372713961\",\"name\":\"HarpendenRetweet\",\"screen_name\":\"RTHarpenden\",\"location\":\"AL5\",\"description\":\"Retweeting to Harpenden. Do you have a question or a message for Harpenden tweeters? Tweet us for a retweet to our Harpenden followers.\",\"url\":null,\"protected\":false,\"followers_count\":263,\"friends_count\":228,\"listed_count\":2,\"created_at\":\"Tue Sep 13 08:42:43 +0000 2011\",\"favourites_count\":0,\"utc_offset\":null,\"time_zone\":null,\"geo_enabled\":false,\"verified\":false,\"statuses_count\":10872,\"lang\":\"en\",\"contributors_enabled\":false,\"is_translator\":false,\"profile_background_color\":\"C0DEED\",\"profile_background_image_url\":\"http:\\/\\/a0.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"profile_background_image_url_https\":\"https:\\/\\/si0.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"profile_background_tile\":false,\"profile_image_url\":\"http:\\/\\/a0.twimg.com\\/profile_images\\/1540869827\\/Harpendedn_normal.jpg\",\"profile_image_url_https\":\"https:\\/\\/si0.twimg.com\\/profile_images\\/1540869827\\/Harpendedn_normal.jpg\",\"profile_link_color\":\"0084B4\",\"profile_sidebar_border_color\":\"C0DEED\",\"profile_sidebar_fill_color\":\"DDEEF6\",\"profile_text_color\":\"333333\",\"profile_use_background_image\":true,\"show_all_inline_media\":false,\"default_profile\":true,\"default_profile_image\":false,\"following\":false,\"follow_request_sent\":false,\"notifications\":false},\"geo\":null,\"coordinates\":null,\"place\":null,\"contributors\":null,\"retweet_count\":0,\"entities\":{\"hashtags\":[],\"urls\":[{\"url\":\"http:\\/\\/t.co\\/juz2kA1L\",\"expanded_url\":\"http:\\/\\/Telegraph.co.uk\",\"display_url\":\"Telegraph.co.uk\",\"indices\":[55,75]},{\"url\":\"http:\\/\\/t.co\\/juz2kA1L\",\"expanded_url\":\"http:\\/\\/Telegraph.co.uk\",\"display_url\":\"Telegraph.co.uk\",\"indices\":[80,100]},{\"url\":\"http:\\/\\/t.co\\/6q3iWaCf\",\"expanded_url\":\"http:\\/\\/bit.ly\\/R8Qsxg\",\"display_url\":\"bit.ly\\/R8Qsxg\",\"indices\":[114,134]}],\"user_mentions\":[]},\"favorited\":false,\"retweeted\":false,\"possibly_sensitive\":false}";
			JSONObject jobj = new JSONObject(json);
			Status s = new Status(jobj, null);
			System.out.println(s.getText());
			System.out.println(s.getDisplayText());			
			List<TweetEntity> urlInfo = s.getTweetEntities(KEntityType.urls);
			int lastEntityEnd = 0;
			for (TweetEntity entity : urlInfo) {
				// FIXME
				if (lastEntityEnd>entity.start) {						
					System.out.println("end of the one entity occurs before the start of another!:" + lastEntityEnd + " vs " + entity.start);
					fail();
				} else {
					//All's well!
				}
				lastEntityEnd = entity.end;
			}
		}
//		{
//			// Hopefully this one will keep on existing. Twitter prune these a bit.
//			// The raw text is: "RT: Stomach bugs to rise during Olympics: scientists - http://t.co/juz2kA1L">http://t.co/juz2kA1L http://t.... http://t.co/6q3iWaCf"
//			BigInteger bi = new BigInteger("220406085545242624");
//			Twitter twitter = newTestTwitter();
//			Status badStatus = twitter.getStatus(bi);
//			List<TweetEntity> urlInfo = badStatus.getTweetEntities(KEntityType.urls);
//			int lastEntityEnd = 0;
//			for (TweetEntity entity : urlInfo) {
//				// FIXME
//				if (lastEntityEnd>entity.start) {						
//					System.out.println("end of the one entity occurs before the start of another!:" + lastEntityEnd + " vs " + entity.start);
//					fail();
//				} else {
//					//All's well!
//				}
//				lastEntityEnd = entity.end;
//			}
//		}
	}
	

	public void testMisc() {
		//
	}

	public void testOldSearch() {
		try {
			Twitter twitter = newTestTwitter();
			twitter.setSinceId(13415168197L);
			List<Status> results = twitter.search("dinosaurs");
		} catch (TwitterException.BadParameter e) {
			String m = e.getMessage();
			boolean old = m.contains("too old");
			assert old;
		}
	}

	static final String TEST_USER = "jtwit";

	public static final String TEST_PASSWORD = "notsofast";

	public static final String[] TEST_ACCESS_TOKEN = new String[]{
		"59714113-b8dOCGjVKS717QUCjN7gdJx1AQ1urfZLx6q54waFs",
		"FQkHfEJYY9JuICUp7lj5wbT6yKxYOpnVRvACtiD62ik"
	};
	
	/**
	 * In my runs, the video tests have failed with a 400 error on the first run and succeeded when run identically again.
	 * I wasn't able to directly examine the 400 response (it was discarded in the process of throwing the error) so I don't
	 * know what Twitter's problem was the first time. RM Oct 2019
	 */
	public void testSmallShortVideoUpload() {
		Twitter jtwit = newTestTwitter();
//		String upload = jtwit.uploadVideo();
//		File f = new File("/home/daniel/winterwell/adserver/web-as/vert/ingreedies_720p.m4v");
		// 10 seconds
		File f = new File("../adserver/web-as/vert/simba-10s-288p.m4v");
		String mediaId = jtwit.uploadVideo(f);
		Status s = jtwit.updateStatusWithUploadedMedia("I uploaded a video with @JTwit :) "+new Random().nextInt(1000), null, Arrays.asList(mediaId));
		System.out.println(s);
	}
	
	/** Seems to fail on first run, see testSmallShortVideoUpload above */
	public void testLargerVideoUpload() {
		Twitter jtwit = newTestTwitter();
		File f = new File("../adserver/web-as/vert/ingreedies_720p.m4v");
		String mediaId = jtwit.uploadVideo(f);
		Status s = jtwit.updateStatusWithUploadedMedia("I uploaded a medium video with @JTwit :) "+new Random().nextInt(1000), null, Arrays.asList(mediaId));
		System.out.println(s);
	}
	
	/** Seems to fail on first run, see testSmallShortVideoUpload above */
	public void testBigVideoUpload() {
		Twitter jtwit = newTestTwitter();
		File f = new File("../adserver/web-as/vert/Fugu.mp4");
		String mediaId = jtwit.uploadVideo(f);
		System.out.println("Uplaod of "+mediaId);
		Status s = jtwit.updateStatusWithUploadedMedia("I uploaded a large-ish video with @JTwit :) "+new Random().nextInt(1000), null, Arrays.asList(mediaId));
		System.out.println(s);
	}
	
	
	public void testPostToTwitterWithMedia(){
		{
			Twitter tw = newTestTwitter();
			File t = new File("test/winterwell/jtwitter/sodash.jpg");
			Printer.out(t.getAbsolutePath());
			tw.updateStatusWithMedia(
					"A test tweet " + Utils.getRandomString(5), null, t);
		}
		{
			Twitter tw = newTestTwitter();
			File t = new File("test/winterwell/jtwitter/sodash.png");
			Printer.out(t.getAbsolutePath());
			tw.updateStatusWithMedia(
					"A test tweet " + Utils.getRandomString(5), null, t);
		}		
	}


	public static void main(String[] args) {
		TwitterTest tt = new TwitterTest();
		Method[] meths = TwitterTest.class.getMethods();
		for(Method m : meths) {
			if ( ! m.getName().startsWith("test")
					|| m.getParameterTypes().length != 0) continue;
			try {
				m.invoke(tt);
				System.out.println(m.getName());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.out.println("TEST FAILED: "+m.getName());
				System.out.println("\t"+e.getCause());
			}
		}
	}

	/**
	 * Check that you can send 160 chars if you wants.
	 * Nope: it's 140 now
	 * ...and now it's 280
	 * Added a salt so consecutive tests don't cause repetition error
	 */
	public void testCanSend160() {
		String s = Double.toString(Math.random()).substring(2, 12); // start with a random 10 digit salt
		Twitter tw = newTestTwitter();
		for (int i = 0; i < 28; i++) {
			tw.setStatus(s);
			System.out.println("SENT " + s.length());
			s += " " + (i % 10) + "23456789";
		}				
	}


	/**
	 *  NONDETERMINISTIC! Had to increase sleep time to make it more reliable.
	 * @throws InterruptedException
	 */
	public void testDestroyStatus() throws InterruptedException {
		Twitter tw = newTestTwitter();
		tw.setStatus("This wont last long! "+new Random().nextInt(1000));
		Status s1 = tw.getStatus();
		System.out.println(s1);
		tw.destroyStatus(s1.getId());
		Utils.sleep(1000);
		Status s0 = tw.getStatus();
		assert s0.id != s1.id : "Status id should differ from that of destroyed status";

		// no repeats
		try {
			tw.destroyStatus(s1.getId());
			assert false;
		} catch(TwitterException.E404 ex) {
			// good
		}
	}

	public void testDestroyStatusBad() {
		// Check security failure
		Twitter tw = newTestTwitter();
		Status hs = tw.getStatus("winterstein");
		try {
			tw.destroyStatus(hs);
			assert false;
		} catch (Exception ex) {
			// OK
		}
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFollowerIDs()}
	 * and {@link winterwell.jtwitter.Twitter#getFollowerIDs(String)}.
	 *
	 */
	public void testFollowerIDs() {
		Twitter tw = newTestTwitter();
		List<Number> ids = tw.getFollowerIDs();
		for (Number id : ids) {
			// Getting a 403 Forbidden error here - not sure what that means
			// user id = 33036740 is causing the problem
			// possibly to do with protected updates?
			try {
				assert tw.users().isFollower(id, TEST_USER) : id;
			} catch (E403 e) {
				// this seems to be a corner issue with Twitter's API rather than a bug in JTwitter
				System.out.println(id+" "+e);
			} catch (E404 e) {
				// this seems to be a corner issue with Twitter's API rather than a bug in JTwitter
				System.out.println(id+" "+e);
			}
		}
		List<Number> ids2 = tw.getFollowerIDs(TEST_USER);
		assert ids.equals(ids2);
	}

	/**
	 * Test the new cursor-based follower/friend methods.
	 */
	public void testManyFollowerIDs() {
		Twitter tw = newTestTwitter();
		tw.setMaxResults(50000);
		List<Number> ids = tw.getFollowerIDs("stephenfry");
		assertTrue(ids.size() >= 50000);
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriendIDs()}
	 * and {@link winterwell.jtwitter.Twitter#getFriendIDs(String)}.
	 */
	public void testFriendIDs() {
		Twitter tw = newTestTwitter();
		Twitter_Users users = tw.users();
		List<Number> ids = users.getFriendIDs(); // tw.getFriendIDs();
		for (Number id : ids) {
			try {
				assert users.isFollower(TEST_USER, id); // tw.isFollower(TEST_USER, id.toString());
			} catch (E403 e) {
				// ignore
				e.printStackTrace();
			}
		}
		List<Number> ids2 = users.getFriendIDs(TEST_USER); //tw.getFriendIDs(TEST_USER);
		assert ids.equals(ids2);
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getDirectMessages()}.
	 * The latency between sending a message and seeing it on direct_messages/events/list.json
	 * endpoint is more than a minute - so if it's not there, we fall back to retrieving it
	 * directly by ID on direct_messages/events/show.json.
	 */
	public void testGetDirectMessages() {
		// send one to make sure there is one
		Twitter tw2 = newTestTwitter2();
		String salt = Utils.getRandomString(4);
		Message sent = tw2.sendMessage(TEST_USER, "Here's a DM to make sure you have at least one: " + salt);
		Utils.sleep(1000);

		Twitter tw = newTestTwitter();
		List<Message> msgs = tw.getDirectMessages();
		boolean foundMessage = false;
		for (Message message : msgs) {
			// Find the message just sent to jtwit
			if (message.getText().contains(salt)) {
				assert tw.self.getId().equals(message.getRecipientId());
				foundMessage = true;
			}
		}
		// Not picked up due to latency? Try fetching it directly
		if (!foundMessage) {
			Message received = tw.getDirectMessage(sent.id);
			foundMessage = received.getText().contains(salt);
		}
		
		assert msgs.size() != 0;
		assert foundMessage : "Didn't find the message jtwittest2 sent to jtwit with salt " + salt;
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFeatured()}.
	 */
	public void testGetHomeTimeline() {
		Twitter tw = newTestTwitter();
		List<Status> ts = tw.getHomeTimeline();
		assert ts.size() > 0;
		assert ts.get(0).text != null;
	}


	public void testTooOld() {
		Twitter tw = newTestTwitter();
		try {
			tw.setSinceId(new BigInteger("10584958134"));
			tw.setUntilId(new BigInteger("20584958134"));
			tw.setSearchLocation(55.954151,-3.20277,"18km");
			List<Status> tweets = tw.search("stuff");
			System.out.println(tweets.size());
//			assert false : tweets; // Oh well - Twitter being nice?
			for (Status status : tweets) {
				assert status.getId().longValue() > 10584958134L;
				assert status.getId().longValue() < 20584958134L;
			}
		} catch (TwitterException.E403 e) {
			String msg = e.getMessage();
			System.out.println(msg);
		}
	}



	public void testGetRateLimitStats() throws InterruptedException {
		{
			Twitter tw = newTestTwitter();
			int i = tw.getRateLimitStatus();
			if (i<1) return;
			tw.getStatus();
			Thread.sleep(1000);
			int i2 = tw.getRateLimitStatus();			
			RateLimit rl = tw.getRateLimit(KRequestType.NORMAL);
			assert rl != null;
			assert rl.getRemaining() == i2;
			assert i - 1 == i2;
		}
		{
			Twitter tw = newTestTwitter();
			int i = tw.getRateLimitStatus();
		}
//		{
//			Twitter twitter = newTestTwitter();
//			while (true)
//			{
//			   int rate = twitter.getRateLimitStatus();
//			   System.out.println(rate+" "+twitter.getHomeTimeline().get(0));
//			}
//		}
	}

	public static Twitter newTestTwitter() {
		OAuthSignpostClient client = new OAuthSignpostClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
				TEST_ACCESS_TOKEN[0], TEST_ACCESS_TOKEN[1]);
		return new Twitter(TEST_USER, client);
	}
	
	/**
	 * Uses a connection client which deliberately breaks!
	 */
	public static Twitter newBadTestTwitter() {
		BadHttpClient client = new BadHttpClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
				TEST_ACCESS_TOKEN[0], TEST_ACCESS_TOKEN[1]);
		return new Twitter(TEST_USER, client);
	}	
	
	/**
	 * Run this to do an out-of-bounds (no web server) authorisation.
	 */
	public void testAuthUser() {
		OAuthSignpostClient client = new OAuthSignpostClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,"oob");
		client.authorizeDesktop();
		String pin = client.askUser("The Pin?");
		System.out.println(pin);
		client.setAuthorizationCode(pin);
		String[] tokens = client.getAccessToken();
		System.out.println("Tokens: "+tokens[0] + " " + tokens[1]);
		
		OAuthSignpostClient authdClient = new OAuthSignpostClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
				tokens[0], tokens[1]);
		System.out.println(new Twitter(null, authdClient).getSelf());
	}
	
	
	
	/**
	 * A second test account (for testing messaging)
	 */
	public static Twitter newTestTwitter2() {
//		OAuthSignpostClient client = new OAuthSignpostClient(
//				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
//				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,"oob");
//		client.authorizeDesktop();
//		String pin = client.askUser("The Pin?");
//		System.out.println(pin);
//		client.setAuthorizationCode(pin);
//		String[] tokens = client.getAccessToken();
//		System.out.println(tokens[0]+" "+tokens[1]);		
		OAuthSignpostClient client = new OAuthSignpostClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
				"360672625-vymkSUAesDdSEedqnQ5U28aLDNrKIcJMCnolv8pH",
				"claNlUI3rWxDYFCpTZU3mJIe4yAGFbv7Aeudj1lw");
		return new Twitter("jtwittest2", client);
	}
		
	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getMentions()}.
	 */
	public void testGetMentions() {
		{
			Matcher m = Status.AT_YOU_SIR.matcher("@dan hello");
			assert m.find();
			m.group(1).equals("dan");
		}
		//		{	// done in code
		//			Matcher m = Status.atYouSir.matcher("dan@email.com hello");
		//			assert ! m.find();
		//		}
		{
			Matcher m = Status.AT_YOU_SIR.matcher("hello @dan");
			assert m.find();
			m.group(1).equals("dan");
		}

		Twitter tw = newTestTwitter();
		List<Status> r = tw.getMentions();
		for (Status message : r) {
			List<String> ms = message.getMentions();
			assert ms.contains(TEST_USER) : message;
		}
		System.out.println("Replies "+r);
	}
	
	
	public void testSendMention() throws InterruptedException {
		Twitter jtwit = newTestTwitter();
		int salt = new Random().nextInt(1000);
		Status s = jtwit.setStatus("@winterstein Public hello "+salt);
		Thread.sleep(1000);
		Status s2 = jtwit.setStatus("Public hello "+salt+" v2 to @winterstein");
		System.out.println(s);
	}

	/**
	 * An exploration test for checking whether the lowest level twitter
	 * functionality is working. It isn't reliably.
	 * NB There's a sleep(10000) between posting the messages and retrieving mentions.
	 * I (RM Oct 2019) set that timeout experimentally - it seems to take that long
	 * for mentions to propagate & be retrievable now. Maybe it'll be longer in the future.
	 * @throws InterruptedException 
	 */
	public void testSendMention2() throws InterruptedException{
		Twitter jtwit = newTestTwitter();
		Twitter jtwit2 = newTestTwitter2();
		Time time = new Time().plus(1, TUnit.HOUR);
		String timeStr = (time.getHour()+1) + " " + time.getMinutes() + " " + time.getSeconds();
		int salt = new Random().nextInt(100000);
		String messageText = "Hoopla!" + salt;
		
		Status s3 = jtwit2.setStatus("@jtwit " + messageText + " " + time);
		Thread.sleep(1000);
		System.out.println(s3);
		Status s4 = jtwit2.setStatus("Public "+ messageText + " " + time + " v2 to @jtwit");
		Thread.sleep(1000);
		System.out.println(s4);
		Thread.sleep(3000);
	
		Status s = jtwit.setStatus("@jtwittest2 Public hello "+ messageText + " " + time);
		Thread.sleep(1000);
		System.out.println(s);
		Status s2 = jtwit.setStatus("Public "+ messageText + " " + time + " v2 to @jtwittest2");
		Thread.sleep(1000);
		System.out.println(s2);
		
		// Try a short delay as most recent mentions aren't showing up in getMentions()
		Thread.sleep(10000);
		
		//Jtwit gets recent mentions.
		List<Status> aList = jtwit.getMentions();
		for (Status stat : aList) {
			if (stat.toString().contains("" + salt)) {
				System.out.println("J1's mentions: "+ stat);
			}
		}
		//Mysteriously, jtwit2 doesn't get recent mentions
		List<Status> aList2 = jtwit2.getMentions();
		for (Status stat : aList2){
			if (stat.toString().contains("" + salt)){
				System.out.println("J2's mentions: "+ stat);
			}
		}
		//Let's try to get all of the mentions for jtwit2
		String name = jtwit2.getSelf().screenName;
		System.out.println("name = " + name);
		{
			List<Status> bigList = jtwit2.search(name);
			int success = 0;
			for (Status stat : bigList) {
				if (stat.toString().contains("" + salt)) {
					success++;
					System.out.println("J2's biglist: "+ stat);
				}
			}
			//This appears to be successful, both messages received
			assert success==2 : success;
		}
		{
//			new UserStream(jtwit2);
//			new TwitterStream(jtwit2);
			List<Status> bigList = jtwit2.search("@" + name);
			int success = 0;
			for (Status stat : bigList) {
				if (stat.toString().contains("" + salt)) {
					success++;
					System.out.println("J2's biglist: "+ stat);
				}
			}
			// This fails, you can't get mentions this way.
			assert success == 0 : success;
		}
	}
	/**
	 * This tests two users pinging DMs at each other, it seems to work fine.
	 * @throws InterruptedException
	 */
	public void testDirectMessage2() throws InterruptedException{
		Twitter jtwit = newTestTwitter();
		Twitter jtwit2 = newTestTwitter2();
		Time time = new Time().plus(1, TUnit.HOUR);
		String timeStr = (time.getHour()+1) + " " + time.getMinutes() + " " + time.getSeconds();
		int salt = new Random().nextInt(100000);
		String messageText = "Dee EMM!" + salt;
		jtwit.sendMessage(jtwit2.getSelf().id, messageText + " I'm jtwit " + time);
		jtwit2.sendMessage(jtwit.getSelf().id, messageText + " I'm jtwittest2 " + time);
		Thread.sleep(10000);
		List<Message> mList1 = jtwit.getDirectMessages();
		for (Message mess : mList1){
			if (mess.toString().contains("" + salt)){
				System.out.println("J1's DMs sent/received : "+ mess);
			}
		}

		List<Message> mList2 = jtwit2.getDirectMessages();
		for (Message mess : mList2){
			if (mess.toString().contains("" + salt)){
				System.out.println("J2's DMs sent/received : "+ mess);
			}
		}
		
		String placeholder = "";

	}

	/**
	 * An exploration test for checking whether the lowest level twitter
	 * functionality is working
	 */
	public void testGetMentionScratch(){
	
		
		
	}

	
	
	// Test written to flush out a problem with the paging code
	public void testPaging() {
		Twitter tw = newTestTwitter();
		tw.setMaxResults(100);
		List<Status> stati = tw.getUserTimeline("joehalliwell");
		assert stati.size() > 100 : stati.size();

		// To see the bug we need a status ID that's within
		// maxResults
		BigInteger sinceId = stati.get(50).id;
		tw.setSinceId(sinceId);
		tw.setMaxResults(100);

		// Previously this would hang
		stati = tw.getUserTimeline("joehalliwell");
	}

	public void testAagha() {
		Twitter tw = newTestTwitter();
		Status s = tw.getStatus("aagha");
		assert s != null;
	}


	/**
	 * Try a call with gzip encoding on
	 */
	public void testGzipOn() {
		Twitter tw = newTestTwitter();
		((URLConnectionHttpClient)tw.getHttpClient()).setGzip(true);
		List<Status> tweets = tw.getUserTimeline();
		assert ! tweets.isEmpty();
	}
	
	/**
	 * Bug seen on LatAm long searches
	 */
	public void testCode195() {
		Twitter tw = newTestTwitter();
		tw.setSearchResultType(tw.SEARCH_RECENT);
		tw.setSearchLocation(19.4326077, -99.133208, "37.570701599121094km");
		String search = "\"necesito conseguir empleo\" OR \"ocupo conseguir empleo\" OR \"necesito un empleo\" OR \"deseo un empleo\" OR \"quisiera un empleo\" OR \"me muero por un empleo\" OR \"quiero una chamba\" OR \"requiero una chamba\" OR \"estoy buscando chamba\" OR \"ando buscando chamba\" OR \"busco chamba\" OR \"ocupo tener chamba\" OR \"quiero conseguir chamba\" OR \"necesito conseguir chamba\" OR \"ocupo conseguir chamba\" OR \"necesito una chamba\" OR \"deseo una chamba\" OR \"quisiera una chamba\" OR \"me muero por una chamba\"";
		String[] bits = search.split(" OR ");
		String s = null;
		for (String string : bits) {
			s = s==null? string : s+" OR "+string;
			System.out.println(s.length()+"\t"+s);
			try {
				List<Status> rs = tw.search(s);		
			} catch(TwitterException.E406 ex) {
				System.out.println(ex);
				assert s.length() > 120 : s;
				break;
			}
		}
	}
	
	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getStatus(int)}.
	 */
	public void testGetStatus() {
		Twitter tw = newTestTwitter();
		Status s = tw.getStatus();
		assert s != null;
		System.out.println(s);

		// source field
		assert s.source.contains("<") : s.source;
		assert ! s.source.contains("&lt;") : s.source;

		//		// test no status
		//		tw = new Twitter(ANOther Account);
		//		s = tw.getStatus();
		//		assert s == null;
	}



	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getStatus(long)}.
	 */
	public void testGetStatusLong() {
		Twitter tw = newTestTwitter();
		{	// See bug #3057
			BigInteger bi = new BigInteger("326717073755602944");
			Status toreg = tw.getStatus(bi);
			System.out.println(toreg.text+" by "+toreg.user+" = "+toreg.getDisplayText());
		}
		{	// Test a specific tweet ("I'm going to keep on posting until I'm confident this is working reliably. If you're following me..." by berkHerkson)
			BigInteger bi = new BigInteger("1029742539795062785");
			Status toreg = tw.getStatus(bi);
			System.out.println(toreg.text+" by "+toreg.user);
		}
		{	// Test with a twitter image
			BigInteger bi = new BigInteger("230445874193518592"); //"273012511874363392");
			Status toreg = tw.getStatus(bi);
			System.out.println(toreg.text+" by "+toreg.user);
			System.out.println(toreg.getDisplayText());
			Printer.out(toreg.getTweetEntities(KEntityType.urls));
		}
		Status s = tw.getStatus();
		Status s2 = tw.getStatus(s.getId());
		assert s.text.equals(s2.text) : "Fetching a status by id should yield correct text";
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getUserTimeline()}.
	 */
	public void testGetUserTimeline() {
		Twitter tw = newTestTwitter();
		List<Status> ut = tw.getUserTimeline();
		assert ut.size() > 0;
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getUserTimeline(java.lang.String, java.lang.Integer, java.util.Date)}.
	 */
	public void testGetUserTimelineString() {
		Twitter tw = newTestTwitter();
		// changed user from "anonpoetry" as they've deleted all their tweets
		List<Status> ns = tw.getUserTimeline("berkherkson");
		System.out.println(ns.get(0));
	}

	public void testTweetEntities() {
		Twitter tw = newTestTwitter();
		tw.setIncludeTweetEntities(true);
		{	// both these examples caused (non-repeatable) bugs in the wild
			// This tweet has now been deleted!
			BigInteger id = new BigInteger("119509089008095232");
			Status s = tw.getStatus(id);
			System.out.println(s.getDisplayText());
		}		
		{
			BigInteger id = new BigInteger("119673041927159808"); 
			Status s = tw.getStatus(id); 
			System.out.println(s.getDisplayText());
		}
		{
			int salt = new Random().nextInt(1000);
			String raw = "@jtwit423gg see http://bit.ly/cldEfd #cool"+salt+" :)";
			Status s = tw.setStatus(raw);
			String dtext = s.getDisplayText();
			assertEquals(raw, dtext);
			List<Status> statuses = tw.getUserTimeline();
			System.out.println(statuses);
		}
		{
			int salt = new Random().nextInt(1000);
			String raw = "http://bit.ly/cldEfd #test "+salt+" ▢▶ >o< &)  http://soda.sh/xr http://maps.google.co.uk/maps?q=39A+grassmarket+&hl=en&sll=55.947372,-3.19599&sspn=0.006295,0.006295&t=h&z=19&vpsrc=0";
			Status s = tw.setStatus(raw);
			String dtext = s.getDisplayText();
			assertEquals(raw, dtext);
		}
	}

	public void testGetUserTimelineWithRetweets() {
		Twitter tw = newTestTwitter();
		Status ws = tw.getStatus("stephenfry");
		
		// Did the user already RT this? Un-RT it before doing it again.
		// Tried adding the target user's screen name to the search query but it was failing for no apparent reason
		// ...so we search all recent RTs
		List<Status> rts = tw.search("from:" + tw.getScreenName() + " filter:retweets");
		for (Status rt : rts) {
			// filter:retweets picks up old-style "RT @username:" retweets which we don't want
			if (!rt.isRetweet()) continue;
			
			// We either retweeted an original post by the target user OR a RT by them - in which case
			// our RT's "original" will point directly to the original post, NOT the target user's RT.
			// So we need to check against the target tweet ID AND its original's ID if that exists
			BigInteger ourOriginalId = rt.getOriginal().getId();
			BigInteger theirId = ws.getId();
			BigInteger theirOriginalId = ws.isRetweet() ? ws.getOriginal().getId() : null;
			
			if (ourOriginalId.equals(theirId) || ourOriginalId.equals(theirOriginalId)) { 
				tw.destroy(rt);
			}
		}
		Utils.sleep(3000);
				
		tw.retweet(ws);
		tw.setIncludeRTs(true);
		List<Status> ns = tw.getUserTimeline();
		System.out.println(ns.get(0));
		Status rt = ns.get(0);
		assert rt != null;
		assert ws.equals(rt.getOriginal()) : rt.getOriginal();
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#isFollower(String)}
	 * and {@link winterwell.jtwitter.Twitter#isFollower(String, String)}.
	 */
	public void testIsFollower() throws InterruptedException {
		Twitter tw = newTestTwitter();

		assert tw.isFollower("winterstein");
		int LAG = 5000;
		User u = tw.stopFollowing("winterstein");
		Thread.sleep(LAG);
		assert ! tw.isFollowing("winterstein");
		tw.follow("winterstein");
		Thread.sleep(LAG);
		assert tw.isFollowing("winterstein");
	}


	public void testRetweet() {
		Twitter tw = newTestTwitter();
		String[] tweeps = new String[]{
				"stephenfry"/*, "ladygaga", "justinbieber"*/
//				"winterstein", 
//				"joehalliwell", "spoonmcguffin", "forkmcguffin", "johnnieingram"
				};
		String chosen = tweeps[new Random().nextInt(tweeps.length)];
		Status s = tw.getStatus(chosen);
		System.out.println("RT @"+chosen+": "+s+"\tBY: "+tw.getScreenName());
		// Did the user already RT this? Un-RT it before doing it again.
		// Tried adding the target user's screen name to the search query but it was failing for no apparent reason
		// ...so we search all recent RTs
		List<Status> rts = tw.search("from:" + tw.getScreenName() + " filter:retweets");
		for (Status rt : rts) {
			if (!rt.isRetweet()) continue; // filter:retweets picks up old-style "RT @username:" retweets which we don't want
			// We either retweeted an original post by the target user OR a RT by them - in which case
			// our RT's "original" will point directly to the original post, NOT the target user's RT.
			// So we need to check against the target tweet ID AND its original's ID if that exists
			BigInteger ourOriginalId = rt.getOriginal().getId();
			BigInteger theirId = s.getId();
			BigInteger theirOriginalId = s.isRetweet() ? s.getOriginal().getId() : null;
			
			if (ourOriginalId.equals(theirId) || ourOriginalId.equals(theirOriginalId)) { 
				tw.destroy(rt);
			}
		}
		Utils.sleep(3000);
		Status rt1 = tw.retweet(s);
		assert rt1.getDisplayText().contains(s.getDisplayText()) : rt1+ " vs "+s;
		
		Status original = rt1.getOriginal();
		assert original != null;
		User user = original.getUser();

		// Retweeters of a retweet get counted against the original, so retrieve RTers for that
		List<User> rters;
		if (s.isRetweet()) {
			rters = tw.getRetweeters(s.getOriginal());
		} else {
			rters = tw.getRetweeters(s);
		}
		assert rters.contains(new User(TEST_USER)) : rters;

		// user timeline includes RTs
		List<Status> tweets = tw.getUserTimeline();
		assert tweets.contains(rt1) : tweets;
		/*
		// (a) Nobody does this any more, (b) it fails on successive test runs due to duplication
		{
			Status s2 = tw.getStatus("joehalliwell");
			Status rt2 = tw.updateStatus("RT @"+s2.user.screenName+" "+s2.text);
			assert rt2.text.contains(s2.text) : rt2;
		}
		*/
	}
	
	public void testSearchWithOR() {
		{
			Twitter tw = newTestTwitter();
			String s = "(medio social) OR (atenciaaaan al cliente sociales) OR (digital marketing)";
			List<Status> javaTweets = tw.search(s);			
			assert javaTweets.size() != 0;
			System.out.println(javaTweets);
			Status tweet = javaTweets.get(0);
			System.out.println(tweet+"\n\t"+tweet.getUser()+" "+tweet.getUser().followersCount);			
		}
	}

	public void testSearch() {		
		{
			Twitter tw = newTestTwitter();
			List<Status> javaTweets = tw.search("java");	
			assert javaTweets.size() != 0;
			Status tweet = javaTweets.get(0);
			System.out.println(tweet+"\n\t"+tweet.getUser()+" "+tweet.getUser().followersCount);
		}
		{	// with entities
			Twitter tw = newTestTwitter();
			List<Status> linkTweets = tw.search("http");
			assert linkTweets.size() != 0;
			Status tweet = linkTweets.get(0);
			System.out.println(tweet);
			List<TweetEntity> urls = tweet.getTweetEntities(KEntityType.urls);
			System.out.println(urls); // Nope, we don't get any
		}
//		{	// long search - This is too complex, gets an exception
//			Twitter tw = newTestTwitter();
//			List<Status> javaTweets = tw.search("santander -banco -de -minutos -en -por -el -hola -buenos -hoy -la -este -esta -nueva");
//			assert javaTweets.size() != 0;
//		}
		{	// few results
			Twitter tw = newTestTwitter();
			tw.setMaxResults(10);
			List<Status> tweets = tw.search(":)");
			assert tweets.size() == 10 : tweets.size();
		}
		{	// Lots of results
			Twitter tw = newTestTwitter();
			tw.setMaxResults(300);
			List<Status> tweets = tw.search(":)");
			assert tweets.size() > 100 : tweets.size();
		}
	}

//	Relies on outdated code FIXME - put in a joining mechanism.
//	public void testSearchWithLocation() {
//		{	// location = London
//			Twitter tw = newTestTwitter();
//			tw.setSearchLocation(51.5, 0, "20km");
//			int londonCnt=0, cnt=0;
//			List<Status> tweets = tw.search("the");
//			for (Status status : tweets) {
//				cnt++;
//				String s = InternalUtils.join(Arrays.asList(status.getLocation(), status.getPlace(), status.getUser().getLocation(), status.getUser().getPlace()), ", ");
//				s = StrUtils.compactWhitespace(s);
//				if (s.toLowerCase().contains("london")) londonCnt++;
//				System.out.println(s);
//			}
//			System.out.println(londonCnt+" out of "+cnt);
//			assert tweets.size() > 10 : tweets.size();
//		}
//
//	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#sendMessage(java.lang.String, java.lang.String)}.
	 */
	public void testSendMessage() {
		Twitter tw = newTestTwitter();
		Twitter_Account ta = new Twitter_Account(tw);
		// TODO ta.setgeotagging true
		tw.setMyLocation(new double[]{-55,1});
		String msg = "Please ignore this message http://www.winterwell.com "+new Random().nextInt(1000);
		Message sent = tw.sendMessage("winterstein", msg);
		System.out.println(""+sent);
		tw.setIncludeTweetEntities(true);
		String msg2 = "Please ignore this message too http://www.winterwell.com "+new Random().nextInt(1000);
		Message sent2 = tw.sendMessage("winterstein", msg2);
		System.out.println(""+sent2.getTweetEntities(KEntityType.urls));
	}

	public void testSendMessage_disguisedNumericalID() {
		Twitter tw = newTestTwitter();
		String msg = "Numerical ID in disguise! Please ignore this message. "+new Random().nextInt(1000);
		Message sent = tw.sendMessage("59714113", msg);
		System.out.println(""+sent);
	}


	public void testSendMessageToSelf() {
		Twitter tw = newTestTwitter();
		String msg = "Hello you, I mean me "+new Random().nextInt(1000);
		tw.setIncludeTweetEntities(true);
		Message sent = tw.sendMessage(tw.getScreenName(), msg);
		System.out.println(sent);
	}


	/**
	 * How to send to 2 people??
	 */
	public void tstSendMessage2Users() {
		Twitter tw = newTestTwitter();
		Twitter_Account ta = new Twitter_Account(tw);
		// TODO ta.setgeotagging true
		tw.setMyLocation(new double[]{-55,1});
		String msg = "Please ignore this message http://www.winterwell.com "+new Random().nextInt(1000);
		Message sent = tw.sendMessage("winterstein,spoonmcguffin", msg);
		System.out.println(""+sent);
		tw.setIncludeTweetEntities(true);
		String msg2 = "Please ignore this message too http://www.winterwell.com "+new Random().nextInt(1000);
		Message sent2 = tw.sendMessage("winterstein", msg2);
		System.out.println(""+sent2.getTweetEntities(KEntityType.urls));
	}

	
	/**
	 * Twitter bug :( -- https://dev.twitter.com/issues/461
	 */
	public void testDateAnomaly() {
		Twitter tw = newTestTwitter();
		Status tweet = tw.getStatus(new BigInteger("230092369926692864"));
		System.out.println(tweet);
		System.out.println(tweet.createdAt);
		System.out.println(tweet.createdAt.getTime());
	}
	
	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#show(java.lang.String)}.
	 */
	public void testShow() {
		Twitter tw = newTestTwitter(); //TEST_USER, TEST_PASSWORD);
		
		User show = tw.users().show("goodloophq");
		assert show != null;
		System.out.println(show.profileImageUrl);
		System.out.println(show.profileBackgroundImageUrl);		
		User show2 = tw.users().show("winterstein");
		assert show2 != null;
		System.out.println(show2);
		
		// a protected user
		User ts = tw.users().show(PROTECTED_USER);
		assert ts.isProtectedUser() : ts;
	}

	public void testTrends() {
		Twitter tw = newTestTwitter();
//		String page = tw.getHttpClient().getPage("https://api.twitter.com/1.1/trends/available.json", null, true);
//		System.out.println(page);
		List<String> trends = tw.getTrends();
		System.out.println(trends);
		assert trends.size() > 0;
	}
	
	
	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#updateStatus(java.lang.String)}.
	 */
	public void testUpdateStatus() {		
		Twitter tw = newTestTwitter();
		int salt = new Random().nextInt(1000);
		
		// a bug from May 2012, now fixed
		Status notNull = tw.updateStatus("Do you agree with @spoonmcguffin that Twitter is great? "+salt);
		assert notNull != null;
		System.out.println(notNull);
				
		String s = "Experimenting "+salt+" (http://winterwell.com at "+new Date().toString()+")";
		Status s2a = tw.updateStatus(s);
//		Status s2b = tw.getStatus(); // There's a lag!
		assert s2a.text.startsWith("Experimenting "+salt) : s2a.text;
//		assert s2b.text.startsWith("Experimenting "+salt) : s2b.text;
//		assert s2a.id.equals(s2b.id) : s2a+" vs "+s2b;
		//		assert s2b.source.equals("web") : s2b.source;
	}
	
	/**
	 * Test for bug 1965
	 */
	public void testUpdateStatusSpecialChars() {		
		Twitter tw = newTestTwitter();
		int salt = new Random().nextInt(1000);
		{
			Status wchars = tw.updateStatus("Hello *"+new Date()+"* world");
			System.out.println(wchars);
		}
		{
			Status wchars = tw.updateStatus("\"Hello *"+salt+"* world!\" - 'test some more chars ._£$%&@");
			System.out.println(wchars);
		}
		{
			Status wchars = tw.updateStatus("Unicode rocks! ¡sʞɔoᴚ ǝpoɔıu∩" + salt);
			System.out.println(wchars);
		}
	}
	
	/**
	 * Test for bug 1965
	 */
	public void testUpdateStatusWithMediaSpecialChars() {		
		Twitter tw = newTestTwitter();
		System.out.println(tw.getScreenName());
		int salt = new Random().nextInt(1000);
		File file = new File("test/winterwell/jtwitter/sodash.png");
		assert file.exists();
		{
			Status wchars = tw.updateStatusWithMedia("How much work goes into the making of a Lady @Dior bag? Find out from the #DioratHarrods exhibition tomorrow onwards-", null, file);
			System.out.println(wchars);
		}
		{
			Status wchars = tw.updateStatusWithMedia("Pictures *"+new Date()+"* for the world", null, file);
			System.out.println(wchars);
		}
		{
			String s = "\"Image *"+salt+"* world!\" - 'test some more chars ._£$%&@";
			Status wchars = tw.updateStatusWithMedia(s, null, file);
			System.out.println(wchars);
			assert wchars.getText().startsWith(s);
		}
		{
			Status wchars = tw.updateStatusWithMedia("Media: Unicode rocks! ¡sʞɔoᴚ ǝpoɔıu∩", null, file);
			System.out.println(wchars);
			assert wchars.getText().startsWith("Media: Unicode rocks! ¡sʞɔoᴚ ǝpoɔıu∩");
		}
	}
	
	/**
	 * Test for bug #6748
	 */
	public void testUpdateStatusWithMediaUnicode() {
		Twitter tw = newTestTwitter();
		System.out.println(tw.getScreenName());
		int salt = new Random().nextInt(1000);
		File file = new File("test/winterwell/jtwitter/sodash.png");
		assert file.exists();
		{
			String s = "It's Christmas time… bring on the trees and the presents… and canapés "+salt;
			Status wchars = tw.updateStatusWithMedia(s, null, file);
			System.out.println(wchars);
			assert wchars.getText().startsWith(s);
		}
	}

	/**
	 * This crashes out at above 140, which is correct
	 * @throws InterruptedException
	 */
	public void testUpdateStatusLength() throws InterruptedException {
		Twitter tw = newTestTwitter();
		Random rnd = new Random();
		{	// WTF?!
			Status s2a = tw.updateStatus("Test tweet aaaa "+rnd.nextInt(1000));
		}
		String salt = new Random().nextInt(1000)+" ";
		Thread.sleep(1000);
		{	// well under
			String s = salt+"help help ";
			for(int i=0; i<2; i++) {
				s += rnd.nextInt(1000);
				s += " ";
			}
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s.trim()) : s2b.text;
			assert s2a.id.equals(s2b.id) : s2a.id+" != "+s2b.id;
		}
		{	// 130
			String s = salt;
			for(int i=0; i<12; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id.equals(s2b.id);
		}
		{	// 140
			String s = salt;
			for(int i=0; i<13; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id.equals(s2b.id);
		}
		// uncomment if you wish to test longer statuses
		if (true) return;
		{	// 150
			String s = salt;
			for(int i=0; i<14; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id.equals(s2b.id);
		}
		{	// 160
			String s = salt;
			for(int i=0; i<15; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id.equals(s2b.id);
		}
		{	// 170
			String s = salt;
			for(int i=0; i<16; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id.equals(s2b.id);
		}

	}


	private String repeat(char c, int i) {
		String s = "";
		for(int j=0; j<i; j++) {
			s += c;
		}
		return s;
	}

	public void testUpdateStatusUnicode() {
		Twitter tw = newTestTwitter();
		
		String salt = Integer.toString(new Random().nextInt(1000));
		String s1 = "Katten är hemma. Hur mår du? お元気ですか " + salt;
		String s2 = salt + " Гладыш Владимир";
		String s3 = "123\u0416" + salt;
		
		{
			Status s2a = tw.updateStatus(s1);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s1) : s2b.text;
			assert s2a.id.equals(s2b.id);
		}
		{
			Status s2a = tw.updateStatus(s2);
			Status s2b = tw.getStatus();
			assert s2a.text.equals(s2) : s2a.text;
			assert s2b.text.equals(s2) : s2b.text;
			assert s2a.id.equals(s2b.id);
		}
		{
			Status s2a = tw.updateStatus(s3);
			assert s2a.text.equals("123\u0416" + salt) : s2a.getText();
		}
		{
			Message s2a = tw.sendMessage("winterstein", s2);
			assert s2a.text.equals(s2) : s2a.getText();
		}
		{
			Message s2a = tw.sendMessage("winterstein", s3);
			assert s2a.text.equals(s3) : s2a.getText();
		}
	}



	public void testUserExists() {
		Twitter tw = newTestTwitter();
		assert tw.users().userExists("spoonmcguffin") : "There is a Spoon, honest";
		assert ! tw.users().userExists("chopstickmcguffin") : "However, there is no Chopstick";
		assert ! tw.users().userExists("Alysha6822") : "Suspended users show up as nonexistent";
	}


	/**
	 * Created on a day when Twitter's followers API was being particularly flaky,
	 * in order to find out just how bad the lag was.
	 * @author miles
	 * @throws IOException if the output file can't be opened for writing
	 * @throws InterruptedException
	 *
	 */
	public void dontTestFollowLag() throws IOException, InterruptedException {
		Twitter jt = new Twitter(TEST_USER, TEST_PASSWORD);
		String spoon = "spoonmcguffin";
		long timestamp = (new Date()).getTime();
		FileWriter outfile = new FileWriter("twitlag" + timestamp + ".txt");
		for (int i = 0; i < 1000; i++) {
			System.out.println("Starting iteration " + i);
			try {
			if (jt.isFollowing(spoon)) {
				System.out.println("jtwit was following Spoon");
				jt.stopFollowing(spoon);
				int counter = 0;
				while (jt.isFollowing(spoon)) {
					Thread.sleep(1000);
					// jt.stopFollowing(spoon);
					counter++;
				}
				try {
					outfile.write("Stopped following: " + counter + "00ms\n");
				} catch (IOException e) {
					System.out.println("Couldn't write to file: " + e);
				}
			} else {
				System.out.println("jtwit was not following Spoon");
				jt.follow(spoon);
				int counter = 0;
				while (!jt.isFollowing(spoon)) {
					Thread.sleep(1000);
					// jt.follow(spoon);
					counter++;
				}
				try {
					outfile.write("Started following: " + counter + "00ms\n");
				} catch (IOException e) {
					System.out.println("Couldn't write to file: " + e);
				}
			}
			} catch (E403 e) {
				System.out.println("isFollower() was mistaken: " + e);
			}
			outfile.flush();
		}
		outfile.close();
	}

	/**
	 *
	 */
	public void testIsValidLogin() {
		{
			Twitter tw = newTestTwitter();
			assert tw.isValidLogin();
		}
		{
			Twitter tw = newTestTwitter();
			assertTrue(tw.isValidLogin());
		}
		{
			Twitter twitter = new Twitter("rumpelstiltskin", "thisisnotarealpassword");
			assertFalse(twitter.isValidLogin());
		}
	}

	
	/**
	 * Sometimes tweets aren't available for search, but they might show up on mentions. Pass/Fail
	 * meaningless.
	 */
	public void testHiddenTweetScratch() {
		Twitter tw = newTestTwitter();
		List<Status> r = tw.search("I genuinely LOVE @virgintrains");
		for (Status message : r) {
			List<String> ms = message.getMentions();
			assert ! ms.isEmpty();
		}
	}

	
	@Test
	public void testBadJSON() throws IOException {
		String json = readFile("test/winterwell/jtwitter/bad_json.txt");
		Status s = readStatusFromJson(json);
		for (TweetEntity te : s.getTweetEntities(KEntityType.hashtags)){
			assert te.toString().length() > 1;
		}
		for (TweetEntity te : s.getTweetEntities(KEntityType.urls)){
			assert te.toString().length() > 1;
		}
	}

	@Test
	public void testBadJSON2_Contents() throws IOException {
		String json = readFile("test/winterwell/jtwitter/bad_json.txt");
		Status s = readStatusFromJson(json);
		for (TweetEntity te : s.getTweetEntities(KEntityType.hashtags)){
			assert te.toString().length() > 1;
		}
		for (TweetEntity te : s.getTweetEntities(KEntityType.urls)){
			assert te.toString().length() > 1;
		}
	}
	
	/**
	 * Tests for the error seen in Issue #31920 - emoji before url causes incorrect positioning of url in tweet & broken display-text
	 * Uses tweet https://twitter.com/aihaiku/status/677535325086986240
	 */
	@Test
	public void testEmojiLength() {
		Twitter twitter = newTestTwitter();
		{
			Number tweetId = BigInteger.valueOf(677535325086986240L);
			Status tweet = twitter.getStatus(tweetId);
			
			String expectedDisplayText = "🌲 Season's Greetings Humans http://soda.sh/xWE0 🌲 🌠";
			String displayText = tweet.getDisplayText();
			assert expectedDisplayText.equals(displayText) : "Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\", original text: \"" + tweet.getText() + "\"";
		}
		{
			Number tweetId = BigInteger.valueOf(686570542208552961L);		
			Status tweet = twitter.getStatus(tweetId);
			String expectedDisplayText = "🎀😡🌠🐙🐌";
			String displayText = tweet.getDisplayText();
			assert expectedDisplayText.equals(displayText) : "Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\", original text: \"" + tweet.getText() + "\"";
		}
		{	
			String expectedDisplayText = "🎀🌠 🌏🎨☕🚊⏬Hello World " + new Random().nextInt(1000) + " 🐙🐌";
			Status tweet = twitter.setStatus(expectedDisplayText);
			String displayText = tweet.getDisplayText();
			assert expectedDisplayText.equals(displayText) : "Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\", original text: \"" + tweet.getText() + "\"";
		}
		{	
			String expectedDisplayText = "🎀🌠 http://sogrow.co.uk 🌏🎨☕🚊⏬Hello World " + new Random().nextInt(1000) + " 🐙🐌";
			Status tweet = twitter.setStatus(expectedDisplayText);
			String displayText = tweet.getDisplayText();
			assert expectedDisplayText.equals(displayText) : "Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\", original text: \"" + tweet.getText() + "\"";
		}
		{	
			String expectedDisplayText = "🎀🌠http://sogrow.co.uk🌏🎨☕🚊⏬Hello World " + new Random().nextInt(1000) + " 🐙🐌";
			Status tweet = twitter.setStatus(expectedDisplayText);
			String displayText = tweet.getDisplayText();
			assert expectedDisplayText.equals(displayText) : "Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\", original text: \"" + tweet.getText() + "\"";
		}
		{	
			String expectedDisplayText = "🎀🌠http://sogrow.co.uk🌏🎨☕🚊⏬Hello World " + new Random().nextInt(1000) + " http://whatalongurlmightlooklike.com/maybeifitrunson?abitlikethis 🐙🐌";
			Status tweet = twitter.setStatus(expectedDisplayText);
			String displayText = tweet.getDisplayText();
			assert expectedDisplayText.equals(displayText) : "Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\", original text: \"" + tweet.getText() + "\"";
		}

		if (false){	// The kitchen sink
//			https://twitter.com/spoonmcguffin/status/686569009299480576
			Number tweetId = BigInteger.valueOf(686569897141383168L);		
			Status tweet = twitter.getStatus(tweetId);			
			String expectedDisplayText = "😡🎀🌠http://sogrow.co.uk🌏🎨☕🚊⏬🇰🇵🇽️🇳️Hello World http://whatalongurlmightlooklike.com/maybeifitrunson?abitlikethis 📭🐙🐌";
			String displayText = tweet.getDisplayText();
			assert expectedDisplayText.equals(displayText) : "Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\", original text: \"" + tweet.getText() + "\"";
			assert expectedDisplayText.equals(displayText) : "JTwitter is not assembling display-text for this tweet correctly. Expected: \"" + expectedDisplayText + "\", saw \"" + displayText +"\".";
		}
	}
	

	public static Status readStatusFromJson(String json) {
		return new Status(new JSONObject(json),null);
	}
	
	private String readFile(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}
		return stringBuilder.toString();
	}
	
	
	/* DEPRECATED TESTS */
	
	/** What's Identica? What's Marakana? Well, whatever they were, they doesn't have the same API they used to. */
//	public void testIdenticaAccess() throws InterruptedException {
//		Twitter jtwit = new Twitter(TEST_USER, TEST_PASSWORD);
//		jtwit.setAPIRootUrl("https://identi.ca/api");
//		int salt = new Random().nextInt(1000);
//		System.out.println(salt);
//		Status s1 = null;
//		try {
//			s1 = jtwit.updateStatus(salt+" Hello to you shiny open source people");
//		} catch (TwitterException.Timeout e) {
//			// identi.ca has problems
//		}
//		Thread.sleep(2000);
//		Status s2 = jtwit.getStatus();
//		assertEquals(s1.toString(), s2.toString());
//		assert s1.equals(s2);
//	}
//	/**
//	 * This works fine
//	 */
//	/*
//	public void testIdentica() {
//		Twitter twitter = new Twitter(TEST_USER, TEST_PASSWORD);
//		twitter.setAPIRootUrl("https://identi.ca/api");
//		String salt = "" + new Random().nextInt(10000);
//		twitter.setStatus("Testing jTwitter http://winterwell.com/software/jtwitter.php "+salt);
//		List<Status> timeline = twitter.getFriendsTimeline();
//	}
//	/**
//	 * But this fails with in Date.parse
//	 */
//	public void testMarakana() {
//		Twitter twitter = new Twitter("student", "password");
//		twitter.setAPIRootUrl("http://yamba.marakana.com/api");
//		String salt = "" + new Random().nextInt(10000);
//		twitter.setStatus("Testing jTwitter http://winterwell.com/software/jtwitter.php"+salt);
//		List<Status> timeline = twitter.getFriendsTimeline();
//	}
	
	/** I can't find Issue 4280 on our bug tracker, and @samdaat isn't a user any more. */
//	public void testIssue4280() {
//		Twitter jtwit = newTestTwitter();
//		List<Status> tweets = jtwit.getUserTimeline("samdaat");
//	}
	
}
