package atrox.model;

import cloudos.model.auth.AuthResponse;

public class AccountAuthResponse extends AuthResponse<Account> {

    public static final AccountAuthResponse TWO_FACTOR = new AccountAuthResponse(true);

    private AccountAuthResponse(boolean twoFactor) { setSessionId(TWO_FACTOR_SID); }

    public AccountAuthResponse(String sessionId, Account account) { super(sessionId, account); }

}
