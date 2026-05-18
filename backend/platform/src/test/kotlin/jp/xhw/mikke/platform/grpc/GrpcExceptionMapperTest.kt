package jp.xhw.mikke.platform.grpc

import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GrpcExceptionMapperTest {
    @Test
    fun `maps domain exceptions to grpc status`() {
        assertEquals(Status.Code.INVALID_ARGUMENT, ValidationException("bad").toStatus().code)
        assertEquals(Status.Code.NOT_FOUND, NotFoundException("missing").toStatus().code)
        assertEquals(Status.Code.PERMISSION_DENIED, PermissionDeniedException("denied").toStatus().code)
        assertEquals(Status.Code.ALREADY_EXISTS, AlreadyExistsException("dup").toStatus().code)
        assertEquals(Status.Code.FAILED_PRECONDITION, FailedPreconditionException("conflict").toStatus().code)
    }
}
