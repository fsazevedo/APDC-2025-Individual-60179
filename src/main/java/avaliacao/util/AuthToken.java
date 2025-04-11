package avaliacao.util;

import java.util.UUID;

import com.google.gson.Gson;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;

import jakarta.ws.rs.core.HttpHeaders;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2;

	public String user;
	public String role;
	public long creationData;
	public long expirationData;
	public String verifier;

	public AuthToken() {
	}

	public AuthToken(String user, String role) {
		this.user = user;
		this.role = role;
		this.creationData = System.currentTimeMillis();
		this.expirationData = this.creationData + EXPIRATION_TIME;
		this.verifier = UUID.randomUUID().toString();
	}
	public static AuthToken extractFromHeader(HttpHeaders headers) {
	    String header = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
	    if (header == null || header.isBlank()) return null;

	    try {
	        return new Gson().fromJson(header, AuthToken.class);
	    } catch (Exception e) {
	        return null;
	    }
	}
	public boolean isRevoked(Datastore datastore) {
	    Key revokedKey = datastore.newKeyFactory()
	        .setKind("RevokedToken")
	        .newKey(this.verifier);
	    return datastore.get(revokedKey) != null;
	}

	
    public boolean isValid() {
        return System.currentTimeMillis() < this.expirationData;
    }

}
