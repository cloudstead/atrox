package atrox.dao.tags;

import atrox.dao.AssociatorEntityDAO;
import atrox.model.EffectType;
import atrox.model.tags.EventEffectTag;
import atrox.model.WorldEvent;
import org.springframework.stereotype.Repository;

@Repository public class EventEffectTagDAO extends AssociatorEntityDAO<EventEffectTag, WorldEvent, EffectType> {}
