package microgram.impl.srv.rest;

import java.net.URI;
import java.util.List;

import microgram.api.Post;
import microgram.api.java.Posts;
import microgram.api.rest.RestPosts;
import microgram.impl.srv.java.JavaPosts;

// Make this class concrete.
public class RestPostsResources extends RestResource implements RestPosts {

	Posts impl;
		
	public RestPostsResources(URI serverUri) {
		this.impl = impl();
	}
	
	synchronized Posts impl() {
		if ( impl == null)
			impl = new JavaPosts();
		return impl;
	}
	
	@Override
	public Post getPost(String postId) {
		return super.resultOrThrow(impl.getPost(postId));
	}

	@Override
	public void deletePost(String postId) {
		super.resultOrThrow(impl.deletePost(postId));
	}

	@Override
	public String createPost(Post post) {
		return super.resultOrThrow(impl.createPost(post));
	}

	@Override
	public boolean isLiked(String postId, String userId) {
		return super.resultOrThrow(impl.isLiked(postId, userId));
	}

	@Override
	public void like(String postId, String userId, boolean isLiked) {
		super.resultOrThrow(impl.like(postId, userId, isLiked));
	}

	@Override
	public List<String> getPosts(String userId) {
		return super.resultOrThrow(impl.getPosts(userId));
	}

	@Override
	public List<String> getFeed(String userId) {
		return super.resultOrThrow(impl.getFeed(userId));
	}

	@Override
	public Integer getPostsNumber(String userId) {
		return super.resultOrThrow(impl.getPostNumber(userId));
	}
 
}
