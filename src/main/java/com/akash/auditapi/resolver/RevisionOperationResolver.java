package com.akash.auditapi.resolver;

import com.akash.auditapi.enums.RevisionOperation;

public interface RevisionOperationResolver {
    RevisionOperation resolve(Object revisionType);
}
