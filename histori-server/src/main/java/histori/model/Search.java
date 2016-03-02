package histori.model;

import histori.model.support.SearchQuery;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.validation.Valid;

@Entity @NoArgsConstructor
public class Search extends IdentifiableBase {

    @Embedded @Valid @Getter @Setter private SearchQuery query;

    public Search(SearchQuery searchQuery) { setQuery(searchQuery); }

}
