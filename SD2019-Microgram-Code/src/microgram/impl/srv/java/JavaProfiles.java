package microgram.impl.srv.java;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import microgram.api.Profile;
import microgram.api.java.Profiles;
import microgram.api.java.Result;
import microgram.api.java.Result.ErrorCode;
import microgram.impl.srv.rest.RestResource;

public class JavaProfiles extends RestResource implements microgram.api.java.Profiles {

	protected Map<String, Profile> users = new HashMap<>();
	protected Map<String, Set<String>> followers = new HashMap<>();
	protected Map<String, Set<String>> following = new HashMap<>();
	
	
	@Override
	public Result<Profile> getProfile(String userId) {
		Profile res = users.get( userId );
		if( res == null ) 
			return error(NOT_FOUND);

		res.setFollowers( followers.get(userId).size() );
		res.setFollowing( following.get(userId).size() );
		return ok(res);
	}

	@Override
	public Result<Void> createProfile(Profile profile) {
		// TODO: Conflict when OK was expected
		// might also be related to slow VM?
		Profile res = users.putIfAbsent( profile.getUserId(), profile );
		if( res != null ) 
			return error(CONFLICT);
		
		followers.put( profile.getUserId(), new HashSet<>());
		following.put( profile.getUserId(), new HashSet<>());
		return ok();
	}
	
	@Override
	public Result<Void> deleteProfile(String userId) {
		//	TODO: not fixed, failed at 4g (concurrent stuff)
		//	might be related to number of cpu cores.
		//	should be tested in a non-VM ubuntu
		if(users.remove(userId) == null)
		{
			return error(NOT_FOUND);
		}
		
		Set<String> fing = following.remove(userId);
		Set<String> fers = followers.remove(userId);
		
		for(String u: fers) {
			Profile p = users.get(u);
			p.setFollowing(p.getFollowing()-1);
			Set<String> temp = following.get(u);
			temp.remove(userId);
		}
		
		for(String u: fing) {
			Profile p = users.get(u);
			p.setFollowers(p.getFollowers()-1);
			Set<String> temp = followers.get(u);
			temp.remove(userId);
		}
		
		return ok();
	}
	
	@Override
	public Result<List<Profile>> search(String prefix) {
		return ok(users.values().stream()
				.filter( p -> p.getUserId().startsWith( prefix ) )
				.collect( Collectors.toList()));
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing) {		
		Set<String> s1 = following.get( userId1 );
		Set<String> s2 = followers.get( userId2 );
		
		if( s1 == null || s2 == null)
			return error(NOT_FOUND);
		
		// TODO: this doesn't feel right
		if( isFollowing ) {
			boolean added1 = s1.add(userId2 ), added2 = s2.add( userId1 );
			if( ! added1 || ! added2 )
				return error(CONFLICT);		
		} else {
			boolean removed1 = s1.remove(userId2), removed2 = s2.remove( userId1);
			if( ! removed1 || ! removed2 )
				return error(NOT_FOUND);					
		}
		return ok();
	}

	@Override
	public Result<Boolean> isFollowing(String userId1, String userId2) {

		Set<String> s1 = following.get( userId1 );
		Set<String> s2 = followers.get( userId2 );
		
		if( s1 == null || s2 == null)
			return error(NOT_FOUND);
		else
			return ok(s1.contains( userId2 ) && s2.contains( userId1 ));
	}

	@Override
	public Result<Set<String>> getFollowing(String userId) {
		
		Set<String> res = following.get(userId);
		return res != null ? ok(res) : error(NOT_FOUND);
	}
}
