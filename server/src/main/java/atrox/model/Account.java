package atrox.model;

import cloudos.model.AccountBase;
import cloudos.model.BasicAccount;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.filters.auth.TokenPrincipal;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.validator.constraints.Email;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Entity
public class Account extends AccountBase implements TokenPrincipal, BasicAccount {

    public static final String ERR_EMAIL_INVALID = "{err.email.invalid}";
    public static final String ERR_EMAIL_EMPTY = "{err.email.empty}";
    public static final String ERR_EMAIL_NOT_UNIQUE = "{err.email.notUnique}";
    public static final String ERR_EMAIL_LENGTH = "{err.email.length}";
    public static final int EMAIL_MAXLEN = 255;
    public static final int VERIFY_CODE_MAXLEN = 100;

    @Getter @Setter @Embedded
    @JsonIgnore
    private HashedPassword hashedPassword;

    public String initResetToken() { return hashedPassword.initResetToken(); }
    @JsonIgnore public long getResetTokenAge() { return hashedPassword.getResetTokenAge(); }
    public Account setPassword(String newPassword) { hashedPassword.setPassword(newPassword); return this; }
    public void setResetToken(String token) { hashedPassword.setResetToken(token); }

    @Email(message=ERR_EMAIL_INVALID)
    @HasValue(message=ERR_EMAIL_EMPTY)
    @Size(max=EMAIL_MAXLEN, message=ERR_EMAIL_LENGTH)
    @Column(unique=true, nullable=false, length=EMAIL_MAXLEN)
    @Getter private String email;

    @HasValue(message=ERR_EMAIL_EMPTY)
    @Size(max=EMAIL_MAXLEN, message=ERR_EMAIL_LENGTH)
    @Column(unique=true, nullable=false, length=EMAIL_MAXLEN)
    @Getter private String canonicalEmail;

    @JsonIgnore @Size(max=VERIFY_CODE_MAXLEN)
    @Getter @Setter private String emailVerificationCode;

    @JsonIgnore @Getter @Setter private Long emailVerificationCodeCreatedAt;
    @Getter private boolean emailVerified = false;

    @JsonIgnore public int getVerifyCodeLength () { return 16; }

    public String initEmailVerificationCode() {
        emailVerificationCode = randomAlphanumeric(getVerifyCodeLength());
        emailVerificationCodeCreatedAt = System.currentTimeMillis();
        return emailVerificationCode;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
        emailVerificationCode = null;
        emailVerificationCodeCreatedAt = null;
    }

    public boolean isEmailVerificationCodeValid (long expiration) {
        return emailVerificationCodeCreatedAt != null && emailVerificationCodeCreatedAt > (System.currentTimeMillis() - expiration);
    }

    public static String canonicalizeEmail (String email) {
        if (empty(email)) throw invalidEx(ERR_EMAIL_EMPTY);
        int atPos = email.indexOf('@');
        if (atPos == -1 || atPos == email.length()-1) throw invalidEx(ERR_EMAIL_INVALID);
        String addr = email.substring(0, atPos);
        String domain = email.substring(atPos+1);
        return CanonicallyNamedEntity.canonicalize(addr) + "@" + domain;
    }

    public Account setEmail (String email) {
        if (this.email == null || !this.email.equals(email)) {
            emailVerified = false;
            emailVerificationCode = null;
            emailVerificationCodeCreatedAt = null;
            this.email = email;
            this.canonicalEmail = canonicalizeEmail(email);
        }
        return this;
    }

    // Set by AtroxAuthFilter
    @JsonIgnore @Transient
    @Getter private String apiToken;
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    @JsonIgnore @Override public String getName() { return getUuid(); }

}
