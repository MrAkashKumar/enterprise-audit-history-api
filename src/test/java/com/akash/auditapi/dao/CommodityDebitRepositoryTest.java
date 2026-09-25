package com.akash.auditapi.dao;

import com.akash.auditapi.entity.CommodityDebit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CommodityDebitRepositoryTest {
    @Autowired
    private CommodityDebitRepository repository;

    @Test
    void readsCommodityDebitDataThroughJpa() {
        CommodityDebit entity = new CommodityDebitForTest().entity();

        repository.saveAndFlush(entity);

        assertThat(repository.findById(10L))
                .get().extracting(CommodityDebit::getTransactionReference)
                .isEqualTo("TRN-10");
    }

    private static final class CommodityDebitForTest {
        private CommodityDebit entity() {
            CommodityDebit entity = instantiate();
            ReflectionTestUtils.setField(entity, "id", 10L);
            ReflectionTestUtils.setField(entity, "transactionReference", "TRN-10");
            return entity;
        }

        private CommodityDebit instantiate() {
            try {
                var constructor = CommodityDebit.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
