package microgram.impl.clt.rest;

import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import microgram.api.java.MediaStorage;
import microgram.api.java.Result;
import microgram.api.rest.RestMediaStorage;

public class RestMediaClient extends RestClient implements MediaStorage {

	public RestMediaClient(URI serverUri) {
		super(serverUri, RestMediaStorage.PATH);
	}

	@Override
	public Result<String> upload(byte[] bytes) {
		Response r = target
				.request()
				.post( Entity.entity( bytes, MediaType.APPLICATION_OCTET_STREAM));
		
		return super.responseContents(r, Status.OK, new GenericType<String>(){});	
	}

	@Override
	public Result<byte[]> download(String id) {
		Response r = target.path(id)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		
		
		return super.responseContents(r, Status.OK, new GenericType<byte[]>(){});
	}

	@Override
	public Result<Void> delete(String id) {
		Response r = target.path(id)
				.request()
				.delete();
		
		return super.verifyResponse(r, Status.OK);
	}

}
