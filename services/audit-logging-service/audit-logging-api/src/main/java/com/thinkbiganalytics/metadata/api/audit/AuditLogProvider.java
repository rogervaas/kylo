/**
 * 
 */
package com.thinkbiganalytics.metadata.api.audit;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Sean Felten
 */
public interface AuditLogProvider {

    AuditLogEntry.ID resolveId(Serializable id);
    
    List<AuditLogEntry> list();
    
    List<AuditLogEntry> list(int limit);
    
    Optional<AuditLogEntry> findById(AuditLogEntry.ID id);

    List<AuditLogEntry> findByUser(Principal user);
    
    AuditLogEntry createEntry(Principal user, String type, String description);
    
    AuditLogEntry createEntry(Principal user, String type, String description, String entityId);
}