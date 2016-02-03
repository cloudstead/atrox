package atrox.auth;

import atrox.ApiConstants;
import atrox.model.Account;
import atrox.resources.AccountsResource;
import atrox.server.AtroxConfiguration;
import com.sun.jersey.spi.container.ContainerRequest;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.cobbzilla.util.collection.SingletonSet;
import org.cobbzilla.util.collection.StringPrefixTransformer;
import org.cobbzilla.wizard.filters.auth.AuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.Set;

@Provider @Service
public class AtroxAuthFilter extends AuthFilter<Account> {

    @Override public String getAuthTokenHeader() { return ApiConstants.API_TOKEN; }
    @Getter private final Set<String> skipAuthPaths = Collections.emptySet();

    @Autowired private AtroxConfiguration configuration;
    @Autowired @Getter private AtroxAuthProvider authProvider;

    private static final Set<String> SKIP_AUTH_PREFIXES = new SingletonSet<>(AccountsResource.ENDPOINT);

    @Getter(lazy=true) private final Set<String> skipAuthPrefixes = initSkipAuthPrefixes();
    public Set<String> initSkipAuthPrefixes() {
        final StringPrefixTransformer transformer = new StringPrefixTransformer(configuration.getHttp().getBaseUri());
        return new HashSet<>(CollectionUtils.collect(SKIP_AUTH_PREFIXES, transformer));
    }

    @Override protected boolean isPermitted(Account principal, ContainerRequest request) {
        return true;
    }

}
