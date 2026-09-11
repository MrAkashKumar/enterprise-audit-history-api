package com.akash.auditapi.resolver;

import com.akash.auditapi.model.RevisionOperation;

public interface RevisionOperationResolver {
    RevisionOperation resolve(Object revisionType);
}
