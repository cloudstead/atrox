package atrox.model;

import cloudos.model.auth.LoginRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;

import static org.cobbzilla.wizard.model.UniquelyNamedEntity.NAME_MAXLEN;

@Entity @Accessors(chain=true) @NoArgsConstructor
public class LoginAttempt extends IdentifiableBase {

    public LoginAttempt (LoginRequest login, String notes) {
        setName(login.hasName() ? login.getName() : "-empty-");
        setUserAgent(login.getUserAgent());
        setNotes(notes);
    }

    @Column(length=1000, nullable=false, updatable=false)
    @Getter @Setter private String userAgent;

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String name;

    @Column(length=1000, nullable=false, updatable=false)
    @Getter @Setter private String notes;
}
