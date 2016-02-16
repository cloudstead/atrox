package histori.auth;

import histori.ApiConstants;
import histori.model.Account;
import histori.server.HistoriConfiguration;
import com.sun.jersey.spi.container.ContainerRequest;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.cobbzilla.util.collection.StringPrefixTransformer;
import org.cobbzilla.wizard.filters.auth.AuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static histori.ApiConstants.ACCOUNTS_ENDPOINT;
import static histori.ApiConstants.EP_PUBLIC;
import static histori.ApiConstants.MAP_IMAGES_ENDPOINT;

@Provider @Service
public class HistoriAuthFilter extends AuthFilter<Account> {

    @Override public String getAuthTokenHeader() { return ApiConstants.API_TOKEN; }
    @Getter private final Set<String> skipAuthPaths = Collections.emptySet();

    @Autowired private HistoriConfiguration configuration;
    @Autowired @Getter private HistoriAuthProvider authProvider;

    @Getter(lazy=true) private final Set<String> skipAuthPrefixes = initSkipAuthPrefixes();
    public Set<String> initSkipAuthPrefixes() {
        final StringPrefixTransformer transformer = new StringPrefixTransformer(configuration.getHttp().getBaseUri());
        final List<String> prefixes = Arrays.asList(new String[]{ACCOUNTS_ENDPOINT, MAP_IMAGES_ENDPOINT+EP_PUBLIC});
        return new HashSet<>(CollectionUtils.collect(prefixes, transformer));
    }

    @Override protected boolean isPermitted(Account principal, ContainerRequest request) {
        return true;
    }

}
