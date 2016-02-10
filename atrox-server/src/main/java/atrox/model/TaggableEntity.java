package atrox.model;

import atrox.model.canonical.Citation;
import atrox.model.canonical.Idea;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MappedSuperclass
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="type")
public abstract class TaggableEntity extends StrongIdentifiableBase {

    // Used to populate json
    @Transient @Getter @Setter private List<String> ideas = new ArrayList<>();
    @Transient @Getter @Setter private List<String> citations = new ArrayList<>();

    public TaggableEntity addIdea(Idea idea) {
        ideas.add(idea.getName());
        return this;
    }

    public TaggableEntity addIdea(String name) {
        ideas.add(name);
        return this;
    }

    public TaggableEntity addIdeas(String... ideas) {
        for (String idea : ideas) addIdea(idea);
        return this;
    }

    public TaggableEntity addCitation(Citation citation) {
        citations.add(citation.getUrl());
        return this;
    }

    public TaggableEntity addCitation(String url) {
        citations.add(url);
        return this;
    }


    public TaggableEntity addCitations(String... urls) {
        for (String url : urls) addCitation(url);
        return this;
    }

    @Transient @Getter @Setter private Map<String, List<TaggableEntity>> relationships = new HashMap<>();

    public List<? extends TaggableEntity> getRelationships(Class type) { return relationships.get(type.getSimpleName()); }

    public List<? extends TaggableEntity> addRelationship(Class type, TaggableEntity entity) {
        String typeName = type.getSimpleName();
        List<TaggableEntity> list = relationships.get(typeName);
        if (list == null) {
            list = new ArrayList<>();
            relationships.put(typeName, list);
        }
        list.add(entity);
        return list;
    }

}
