package com.akash.auditapi.resolver;

import com.akash.auditapi.model.RevisionOperation;
import org.springframework.stereotype.Component;

@Component
public class EnversRevisionOperationResolver implements RevisionOperationResolver {
    @Override
    public RevisionOperation resolve(Object revisionType) {
        return RevisionOperation.from(revisionType);
    }
}
