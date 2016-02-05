package atrox.dao;

import atrox.model.EffectType;
import atrox.model.tags.EventEffectTag;
import atrox.model.WorldEvent;
import org.springframework.stereotype.Repository;

@Repository
public class EventEffectDAO extends AssociatorEntityDAO<EventEffectTag, WorldEvent, EffectType> {}
