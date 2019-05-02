package microgram.impl.clt.rest;

import java.net.URI;

import microgram.api.java.Media;
import microgram.api.java.Result;

public class RestMediaClient extends RestClient implements Media {

	public RestMediaClient(URI uri, String path) {
		super(uri, path);
		// TODO Auto-generated constructor stub
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
