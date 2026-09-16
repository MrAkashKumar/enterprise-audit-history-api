package com.akash.auditapi.resolver;

import com.akash.auditapi.enums.RevisionOperation;

/**
 * Strategy contract for converting stored revision-type values into API operations.
 * Alternative audit schemes can provide another implementation without changing assembly logic.
 */
public interface RevisionOperationResolver {
    RevisionOperation resolve(Object revisionType);
}
