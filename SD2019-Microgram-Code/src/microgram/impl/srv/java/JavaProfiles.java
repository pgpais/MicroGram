package microgram.impl.srv.java;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;
import static microgram.api.java.Result.ErrorCode.INTERNAL_ERROR;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import discovery.Discovery;
import microgram.api.Profile;
import microgram.api.java.Profiles;
import microgram.api.java.Result;
import microgram.api.java.Result.ErrorCode;
import microgram.impl.clt.java.RetryPostsClient;
import microgram.impl.clt.rest.RestPostsClient;
import microgram.impl.srv.rest.PostsRestServer;
import microgram.impl.srv.rest.ProfilesRestServer;
import microgram.impl.srv.rest.RestResource;
import utils.Sleep;

public class JavaProfiles extends RestResource implements microgram.api.java.Profiles {

	protected Map<String, Profile> users = new ConcurrentHashMap<>();
	protected Map<String, Set<String>> followers = new ConcurrentHashMap<>();
	protected Map<String, Set<String>> following = new ConcurrentHashMap<>();

	private List<URI> postServers = new LinkedList<URI>();
	private static int SLEEP_TIME = 5;

	// TODO: this isn't needed?
	public JavaProfiles() {

		new Thread(() -> {
			// TODO: check if this works
			while (true) {
				for (URI uri : Discovery.findUrisOf(PostsRestServer.SERVICE, 1))
					postServers.add(uri);
				Sleep.seconds(SLEEP_TIME);
			}
		}).start();

	}

	@Override
	public Result<Profile> getProfile(String userId) {
		Profile prof = users.get(userId);
		if (prof == null)
			return error(NOT_FOUND);

		prof.setFollowers(followers.get(userId).size());
		prof.setFollowing(following.get(userId).size());
		
		RetryPostsClient client = new RetryPostsClient(new RestPostsClient(postServers.get(0)));
		Result<Integer> res = client.getPostNumber(userId);
		
		if(res.isOK()) {
			prof.setPosts(res.value());
		} else {
			return error(NOT_FOUND);
		}
		
		
		return ok(prof);
	}

	@Override
	public Result<Void> createProfile(Profile profile) {

		Profile res = users.putIfAbsent(profile.getUserId(), profile);
		if (res != null)
			return error(CONFLICT);

		followers.put(profile.getUserId(), new HashSet<>());
		following.put(profile.getUserId(), new HashSet<>());
		return ok();
	}

	@Override
	public Result<Void> deleteProfile(String userId) {

		if (users.remove(userId) == null) {
			return error(NOT_FOUND);
		}
		System.out.println("Removed " + userId);
		Set<String> fing = following.remove(userId);
		Set<String> fers = followers.remove(userId);

		try {
			for (String u : fers) {
				Profile p = users.get(u);
				p.setFollowing(p.getFollowing() - 1);
				Set<String> temp = following.get(u);
				temp.remove(userId);
				following.replace(u, temp);
			}

			for (String u : fing) {
				Profile p = users.get(u);
				p.setFollowers(p.getFollowers() - 1);
				Set<String> temp = followers.get(u);
				temp.remove(userId);
				followers.replace(u, temp);
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		return ok();
	}

	@Override
	public Result<List<Profile>> search(String prefix) {
		return ok(users.values().stream().filter(p -> p.getUserId().startsWith(prefix)).collect(Collectors.toList()));
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing) {
		Set<String> s1 = following.get(userId1);
		Set<String> s2 = followers.get(userId2);

		if (s1 == null || s2 == null)
			return error(NOT_FOUND);

		if (isFollowing) {
			boolean added1 = s1.add(userId2), added2 = s2.add(userId1);
			if (!added1 || !added2)
				return error(CONFLICT);
		} else {
			boolean removed1 = s1.remove(userId2), removed2 = s2.remove(userId1);
			if (!removed1 || !removed2)
				return error(NOT_FOUND);
		}
		return ok();
	}

	@Override
	public Result<Boolean> isFollowing(String userId1, String userId2) {

		Set<String> s1 = following.get(userId1);
		Set<String> s2 = followers.get(userId2);

		if (s1 == null || s2 == null)
			return error(NOT_FOUND);
		else
			return ok(s1.contains(userId2) && s2.contains(userId1));
	}

	@Override
	public Result<Set<String>> getFollowing(String userId) {

		Set<String> res = following.get(userId);
		return res != null ? ok(res) : error(NOT_FOUND);
	}
}
