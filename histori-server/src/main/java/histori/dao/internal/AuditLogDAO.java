package histori.dao.internal;

import histori.model.auth.RegistrationRequest;
import histori.model.internal.AuditLog;
import cloudos.model.auth.LoginRequest;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.stereotype.Repository;

@Repository public class AuditLogDAO extends AbstractCRUDDAO<AuditLog> {

    public AuditLog log (LoginRequest request, String context, String notes) {
        return create(new AuditLog(request, context, notes));
    }

    public AuditLog log (RegistrationRequest request, String context, String notes) {
        return create(new AuditLog(request, context, notes));
    }

}
