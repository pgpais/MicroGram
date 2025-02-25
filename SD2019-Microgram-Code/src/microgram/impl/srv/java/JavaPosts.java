package microgram.impl.srv.java;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;
import static microgram.api.java.Result.ErrorCode.NOT_IMPLEMENTED;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import discovery.Discovery;
import microgram.api.Post;
import microgram.api.Profile;
import microgram.api.java.Posts;
import microgram.api.java.Result;
import microgram.api.java.Result.ErrorCode;
import microgram.impl.clt.java.RetryProfilesClient;
import microgram.impl.clt.rest.RestProfilesClient;
import microgram.impl.srv.rest.ProfilesRestServer;
import utils.Hash;
import utils.Sleep;

public class JavaPosts implements Posts {

	protected Map<String, Post> posts = new ConcurrentHashMap<>();
	protected Map<String, Set<String>> likes = new ConcurrentHashMap<>();
	protected Map<String, Set<String>> userPosts = new ConcurrentHashMap<>();

	private List<URI> profileServers;
	private static int SLEEP_TIMEOUT = 2;

	public JavaPosts() {

		profileServers = new LinkedList<URI>();

		new Thread(() -> {
			// TODO: check if this works
			while (true) {
				for (URI uri : Discovery.findUrisOf(ProfilesRestServer.SERVICE, 1))
					profileServers.add(uri);
				Sleep.seconds(SLEEP_TIMEOUT);
			}
		}).start();

	}

	@Override
	public Result<Post> getPost(String postId) {
		Post res = posts.get(postId);
		if (res != null)
			return ok(res);
		else
			return error(NOT_FOUND);
	}

	@Override
	public Result<Void> deletePost(String postId) {
		
		Result<Post> res = getPost(postId);
		
		if (!res.isOK())
			return error(NOT_FOUND);
		
		Post post = res.value();
		
		posts.remove(postId, post);

		likes.remove(postId);
		userPosts.get(post.getOwnerId()).remove(postId);
		return ok();

	}

	@Override
	public Result<String> createPost(Post post) {
		
		RetryProfilesClient client = new RetryProfilesClient(new RestProfilesClient(profileServers.get(0)));
		Result<Profile> res = client.getProfile(post.getOwnerId());
		if(!res.isOK()) {
			return error(NOT_FOUND);
		}
		
		String postId = Hash.of(post.getOwnerId(), post.getMediaUrl());
		if (posts.putIfAbsent(postId, post) == null) {

			likes.put(postId, new HashSet<>());

			Set<String> posts = userPosts.get(post.getOwnerId());
			if (posts == null)
				userPosts.put(post.getOwnerId(), posts = new LinkedHashSet<>());

			posts.add(postId);
		}
		return ok(postId);
	}

	@Override
	public Result<Void> like(String postId, String userId, boolean isLiked) {

		Set<String> res = likes.get(postId);
		if (res == null)
			return error(NOT_FOUND);

		if (isLiked) {
			if (!res.add(userId))
				return error(CONFLICT);
		} else {
			if (!res.remove(userId))
				return error(NOT_FOUND);
		}

		getPost(postId).value().setLikes(res.size());
		return ok();
	}

	@Override
	public Result<Boolean> isLiked(String postId, String userId) {
		Set<String> res = likes.get(postId);

		if (res != null)
			return ok(res.contains(userId));
		else
			return error(NOT_FOUND);
	}

	@Override
	public Result<List<String>> getPosts(String userId) {
		Set<String> res = userPosts.get(userId);
		if (res != null)
			return ok(new ArrayList<>(res));
		else
			return error(NOT_FOUND);
	}

	@Override
	public Result<List<String>> getFeed(String userId) {
		// Use profile server list, maybe make cycle for multiple servers
//		URI[] uri = Discovery.findUrisOf(ProfilesRestServer.SERVICE, 1);
//
//		RetryProfilesClient client = new RetryProfilesClient(new RestProfilesClient(uri[0]));

		RetryProfilesClient client = new RetryProfilesClient(new RestProfilesClient(profileServers.get(0)));
		Result<Set<String>> res = client.getFollowing(userId);
		Set<String> following = null;

		if (res.isOK()) {
			following = res.value();
		} else {
			return error(NOT_FOUND);
		}

		List<String> posts = new LinkedList<String>();

		for (String followId : following) {
			posts.addAll(userPosts.get(followId));
		}
		return ok(posts);
	}

	@Override
	public Result<Integer> getPostNumber(String userId) {
		Set<String> posts = userPosts.get(userId);
		int res = -1;
		if (posts == null) {
			return ok(0);
		} else
			res = posts.size();

		return ok(res);
	}
}
