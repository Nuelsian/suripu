package com.hello.suripu.coredw8.oauth;

import io.dropwizard.auth.AuthenticationException;

/**
 * Created by jnorgan on 5/3/16.
 */
public class MissingRequiredScopeAuthenticationException extends AuthenticationException {
  public MissingRequiredScopeAuthenticationException(Throwable cause) { super(cause); }
}
