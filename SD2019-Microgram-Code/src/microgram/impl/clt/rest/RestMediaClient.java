package microgram.impl.clt.rest;

import java.net.URI;

import microgram.api.java.Media;
import microgram.api.java.Result;
import microgram.api.rest.RestMediaStorage;

public class RestMediaClient extends RestClient implements Media {

	public RestMediaClient(URI serverUri) {
		super(serverUri, RestMediaStorage.PATH);
	}

	@Override
	public Result<String> upload(byte[] bytes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<byte[]> download(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
