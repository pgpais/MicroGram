package microgram.impl.srv.rest;


import java.net.URI;
import java.util.List;
import java.util.Set;

import microgram.api.Profile;
import microgram.api.java.Profiles;
import microgram.api.rest.RestProfiles;
import microgram.impl.srv.java.JavaProfiles;

public class RestProfilesResources extends RestResource implements RestProfiles {

	Profiles impl;
	
	public RestProfilesResources(URI serverUri) {
		this.impl = impl();
	}
	
	synchronized Profiles impl() {
		if ( impl == null)
			impl = new JavaProfiles();
		return impl;
	}
	
	@Override
	public Profile getProfile(String userId) {
		return super.resultOrThrow( impl.getProfile(userId));
	}

	@Override
	public void createProfile(Profile profile) {
		super.resultOrThrow(impl.createProfile(profile));
		
	}

	@Override
	public List<Profile> search(String name) {
		return super.resultOrThrow(impl.search(name));
	}

	@Override
	public void follow(String userId1, String userId2, boolean isFollowing) {
		super.resultOrThrow(impl.follow(userId1, userId2, isFollowing));
	}

	@Override
	public boolean isFollowing(String userId1, String userId2) {
		return super.resultOrThrow(impl.isFollowing(userId1, userId2));
	}

	@Override
	public Set<String> getFollowing(String userId) {
		return super.resultOrThrow(impl.getFollowing(userId));
	}

	@Override
	public void deleteProfile(String userId) {
		super.resultOrThrow(impl.deleteProfile(userId));
	}	
}
