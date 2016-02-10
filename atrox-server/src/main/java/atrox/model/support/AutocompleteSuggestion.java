package atrox.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class AutocompleteSuggestion {

    @Getter @Setter private String value;
    @Getter @Setter private AutocompleteSuggestionData data;

    public AutocompleteSuggestion(String value, String category) {
        this.value = value;
        this.data = new AutocompleteSuggestionData(category);
    }
}
