package histori.model.tag;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class GenericTagBean extends GenericEntityTag {

    public GenericTagBean(String tagType, String tagName) {
        setEntity(tagName);
    }

    @Override public String simpleName() {
        return super.simpleName();
    }
}
