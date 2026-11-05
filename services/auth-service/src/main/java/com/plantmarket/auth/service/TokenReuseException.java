package com.plantmarket.auth.service;

import com.plantmarket.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** Refresh token reuse means the token chain may be stolen; all sessions are revoked. */
public class TokenReuseException extends ApiException {

  public TokenReuseException() {
    super(
        HttpStatus.UNAUTHORIZED,
        PROBLEM_BASE + "refresh-token-reuse",
        "Refresh token reuse detected; all sessions revoked");
  }
}
