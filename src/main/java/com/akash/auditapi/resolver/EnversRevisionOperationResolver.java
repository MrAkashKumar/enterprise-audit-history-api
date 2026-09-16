package com.akash.auditapi.resolver;

import com.akash.auditapi.enums.RevisionOperation;
import org.springframework.stereotype.Component;

/**
 * Implements revision-operation mapping for Hibernate Envers-compatible codes.
 * The history assembler uses it through the resolver strategy interface.
 */
@Component
public class EnversRevisionOperationResolver implements RevisionOperationResolver {
    @Override
    public RevisionOperation resolve(Object revisionType) {
        return RevisionOperation.from(revisionType);
    }
}
