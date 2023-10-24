package nl.rrd.senseeact.service.sso;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.QueryRunner;
import nl.rrd.senseeact.service.ValidateTokenResult;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.exception.UnauthorizedException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class SSOTokenJwt extends SSOToken {
	private static final int MAX_TOKEN_VALID_MARGIN = 10; // minutes

	private String tokenHeader;
	private List<String> publicKeys = new ArrayList<>();

	public SSOTokenJwt(String project, String tokenHeader) {
		super(project);
		this.tokenHeader = tokenHeader;
	}

	public SSOTokenJwt(String project, String tokenHeader, String publicKey) {
		super(project);
		this.tokenHeader = tokenHeader;
		this.publicKeys.add(publicKey);
	}

	public void addPublicKey(String publicKey) {
		this.publicKeys.add(publicKey);
	}

	@Override
	public boolean requestHasToken(HttpServletRequest request) {
		String token = request.getHeader(tokenHeader);
		return token != null;
	}

	/**
	 * Reads the user ID or email address of the subject from the specified
	 * JWT Claims and HTTP request. By default this method returns the "sub"
	 * field of the JWT token, but you can override this method if the subject
	 * should be obtained in another way. If the subject cannot be read, you
	 * can return null, which will result in an authentication error response.
	 *
	 * @param request the HTTP request
	 * @param claims the JWT Claims
	 * @return the user ID or email address or null
	 */
	protected String readSubject(HttpServletRequest request,
			JWTClaimsSet claims) {
		return claims.getSubject();
	}

	/**
	 * Validates a JWT token from a trusted third party as specified in request
	 * header X-*-Auth-Token. If it's empty or invalid, it will throw an
	 * UnauthorizedException. Otherwise it will return the user object for the
	 * authenticated user.
	 *
	 * @param version the protocol version
	 * @param authDb the authentication database
	 * @param request the request
	 * @param project the code of the project that the user wants to access,
	 * or null
	 * @return the authenticated user
	 * @throws HttpException if the token is empty or invalid
	 * @throws Exception if any unexpected error occurs
	 */
	@Override
	public ValidateTokenResult validateToken(ProtocolVersion version,
			HttpServletRequest request, HttpServletResponse response,
			Database authDb, String project) throws HttpException, Exception {
		Logger logger = AppComponents.getLogger(
				QueryRunner.class.getSimpleName());
		String token = request.getHeader(tokenHeader);
		if (token.trim().isEmpty()) {
			logger.error(String.format("Invalid JWT token in %s: Token empty",
					tokenHeader));
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		JWTClaimsSet claims = parseToken(token);
		ZonedDateTime now = DateTimeUtils.nowMs();
		ZonedDateTime min = now.minusMinutes(MAX_TOKEN_VALID_MARGIN);
		ZonedDateTime max = now.plusMinutes(MAX_TOKEN_VALID_MARGIN);
		ZonedDateTime issuedAt = ZonedDateTime.ofInstant(
				claims.getIssueTime().toInstant(), ZoneId.systemDefault());
		if (issuedAt.isBefore(min) || issuedAt.isAfter(max)) {
			logger.error(String.format(
					"JWT token in %s does not match time constraint",
					tokenHeader));
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		String subject = readSubject(request, claims);
		if (subject == null) {
			logger.error(String.format("Subject not found for JWT token in %s",
					tokenHeader));
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		SSOUserValidator userValidator = getValidator(project);
		User user = userValidator.findAuthenticatedUser(version, response,
				authDb, subject);
		if (user == null) {
			logger.error(String.format("User for JWT token in %s not found: ",
					tokenHeader) + claims.getSubject());
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		return new ValidateTokenResult(user, null);
	}

	private JWTClaimsSet parseToken(String token) throws UnauthorizedException {
		if (publicKeys.isEmpty())
			throw new RuntimeException("No public keys found");
		UnauthorizedException exception = null;
		for (String publicKey : publicKeys) {
			try {
				return parseTokenForKey(token, publicKey);
			} catch (UnauthorizedException ex) {
				if (exception == null)
					exception = ex;
			}
		}
		throw exception;
	}

	private JWTClaimsSet parseTokenForKey(String token, String publicKey)
			throws UnauthorizedException {
		Logger logger = AppComponents.getLogger(
				QueryRunner.class.getSimpleName());
		RSAPublicKey pubKey;
		try {
			byte[] encPubKey = Base64.decodeBase64(publicKey.getBytes(
					StandardCharsets.UTF_8));
			KeyFactory factory = KeyFactory.getInstance("RSA");
			pubKey = (RSAPublicKey)factory.generatePublic(
					new X509EncodedKeySpec(encPubKey));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new RuntimeException(String.format(
					"Can't get public key to verify token in %s: ",
					tokenHeader) + ex.getMessage(), ex);
		}
		JWSVerifier verifier = new RSASSAVerifier(pubKey);
		boolean verified;
		JWTClaimsSet claims;
		try {
			SignedJWT signedJwt = SignedJWT.parse(token);
			verified = signedJwt.verify(verifier);
			claims = signedJwt.getJWTClaimsSet();
		} catch (JOSEException | ParseException ex) {
			logger.error(String.format("Invalid JWT token in %s: ",
					tokenHeader) + ex.getMessage());
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		if (!verified) {
			logger.error(String.format("Invalid JWT token in %s", tokenHeader));
			throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID,
					"Authentication token invalid");
		}
		return claims;
	}

	/**
	 * Returns the user validator for this SSO token. The validator can check
	 * whether the requested project is valid. If an endpoint is called that is
	 * not related to a project, then "requestedProject" will be null. By
	 * default this method returns a {@link SSOProjectUserValidator
	 * SSOProjectUserValidator}.
	 *
	 * @param requestedProject the requested project or null
	 * @return the user validator
	 */
	protected SSOUserValidator getValidator(String requestedProject) {
		return new SSOProjectUserValidator(getProject(), requestedProject);
	}
}
