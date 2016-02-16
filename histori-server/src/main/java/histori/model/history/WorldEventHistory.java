package histori.model.history;

import histori.model.canonical.ImpactType;
import histori.model.support.GeoPolygon;
import histori.model.support.TimePoint;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @Accessors(chain=true)
public class WorldEventHistory extends EntityHistory {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldEvent;

    @AttributeOverrides({
            @AttributeOverride(name="instant",column=@Column(name="startInstant")),
            @AttributeOverride(name="year",column=@Column(name="startYear")),
            @AttributeOverride(name="month",column=@Column(name="startMonth")),
            @AttributeOverride(name="day",column=@Column(name="startDay")),
            @AttributeOverride(name="hour",column=@Column(name="startHour")),
            @AttributeOverride(name="minute",column=@Column(name="startMinute")),
            @AttributeOverride(name="second",column=@Column(name="startSecond")),
    })
    @Embedded
    @Getter @Setter private TimePoint startPoint;

    @AttributeOverrides({
            @AttributeOverride(name="instant",column=@Column(name="endInstant")),
            @AttributeOverride(name="year",column=@Column(name="endYear")),
            @AttributeOverride(name="month",column=@Column(name="endMonth")),
            @AttributeOverride(name="day",column=@Column(name="endDay")),
            @AttributeOverride(name="hour",column=@Column(name="endHour")),
            @AttributeOverride(name="minute",column=@Column(name="endMinute")),
            @AttributeOverride(name="second",column=@Column(name="endSecond")),
    })
    @Embedded
    @Getter @Setter private TimePoint endPoint;

    @Embedded
    @Getter @Setter private GeoPolygon polygon;

    public boolean hasActors () { return !empty(getRelationships(WorldActorHistory.class)); }
    @JsonIgnore @Transient public List<WorldActorHistory> getActors () { return (List<WorldActorHistory>) getRelationships(WorldActorHistory.class); }
    public void addActor(WorldActorHistory actorHistory) { addRelationship(WorldActorHistory.class, actorHistory); }

    public boolean hasImpacts () { return !empty(getRelationships(ImpactTypeHistory.class)); }
    @JsonIgnore @Transient public List<ImpactTypeHistory> getImpacts () { return (List<ImpactTypeHistory>) getRelationships(ImpactTypeHistory.class); }
    public void addImpact(ImpactType impact) { addRelationship(ImpactTypeHistory.class, impact); }

    public boolean hasIncidents () { return !empty(getRelationships(IncidentTypeHistory.class)); }
    @JsonIgnore @Transient public List<IncidentTypeHistory> getIncidents () { return (List<IncidentTypeHistory>) getRelationships(IncidentTypeHistory.class); }
    public void addIncident(IncidentTypeHistory impact) { addRelationship(IncidentTypeHistory.class, impact); }

}
