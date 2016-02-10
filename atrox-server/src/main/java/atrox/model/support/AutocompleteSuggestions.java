package atrox.model.support;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteSuggestions {

    @Getter @Setter private String query;
    @Getter @Setter private List<AutocompleteSuggestion> suggestions = new ArrayList<>();

    public void add(AutocompleteSuggestion s) { suggestions.add(s); }
    public void add(String value, String category) { add(new AutocompleteSuggestion(value, category)); }

}
