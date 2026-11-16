package io.github.luasgraphicss.minemod.compatibility.state.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ErrorLedgerTest {
    private static final int INVALID_ENUM = 0x0500;
    private static final int INVALID_VALUE = 0x0501;
    private static final int INVALID_OPERATION = 0x0502;

    private final SingleFlagProvider provider = new SingleFlagProvider();
    private final ErrorLedger ledger = new ErrorLedger(provider);

    @Test
    void forwardsTheProviderErrorWhenTheLayerRaisedNone() {
        provider.fail(INVALID_ENUM);

        assertEquals(INVALID_ENUM, ledger.take());
        assertEquals(ErrorLedger.NO_ERROR, ledger.take());
    }

    @Test
    void aLayerErrorRaisedFirstHidesLaterProviderErrors() {
        ledger.raise(INVALID_OPERATION);
        provider.fail(INVALID_ENUM);

        assertEquals(INVALID_OPERATION, ledger.take());
        assertEquals(ErrorLedger.NO_ERROR, ledger.take());
    }

    @Test
    void anEarlierProviderErrorWinsOverALaterLayerError() {
        provider.fail(INVALID_VALUE);
        ledger.raise(INVALID_OPERATION);
        provider.fail(INVALID_ENUM);

        assertEquals(INVALID_VALUE, ledger.take());
        assertEquals(ErrorLedger.NO_ERROR, ledger.take());
    }

    @Test
    void keepsOnlyTheFirstLayerError() {
        ledger.raise(INVALID_OPERATION);
        ledger.raise(INVALID_ENUM);

        assertEquals(INVALID_OPERATION, ledger.take());
        assertEquals(ErrorLedger.NO_ERROR, ledger.take());
    }

    @Test
    void recordsAgainOnceTheErrorWasTaken() {
        ledger.raise(INVALID_OPERATION);
        ledger.take();

        ledger.raise(INVALID_ENUM);

        assertEquals(INVALID_ENUM, ledger.take());
    }

    @Test
    void refusesToRaiseTheAbsenceOfAnError() {
        assertThrows(IllegalArgumentException.class, () -> ledger.raise(ErrorLedger.NO_ERROR));
    }

    private static final class SingleFlagProvider implements ProviderErrors {
        private int flag = ErrorLedger.NO_ERROR;

        void fail(int error) {
            if (flag == ErrorLedger.NO_ERROR) {
                flag = error;
            }
        }

        @Override
        public int takeError() {
            int taken = flag;
            flag = ErrorLedger.NO_ERROR;
            return taken;
        }
    }
}
