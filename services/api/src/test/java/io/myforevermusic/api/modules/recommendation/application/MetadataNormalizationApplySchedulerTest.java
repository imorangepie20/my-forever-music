package io.myforevermusic.api.modules.recommendation.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MetadataNormalizationApplySchedulerTest {

    @Test
    void shouldSkipWhenDisabled() {
        MetadataNormalizationAdminService adminService = mock(MetadataNormalizationAdminService.class);
        MetadataNormalizationApplyScheduler scheduler = new MetadataNormalizationApplyScheduler(adminService);
        ReflectionTestUtils.setField(scheduler, "enabled", false);
        ReflectionTestUtils.setField(scheduler, "adminUserId", "user-admin");

        scheduler.run();

        verifyNoInteractions(adminService);
    }

    @Test
    void shouldSkipWhenAdminUserIdIsBlank() {
        MetadataNormalizationAdminService adminService = mock(MetadataNormalizationAdminService.class);
        MetadataNormalizationApplyScheduler scheduler = new MetadataNormalizationApplyScheduler(adminService);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "adminUserId", " ");

        scheduler.run();

        verifyNoInteractions(adminService);
    }

    @Test
    void shouldApplyAcceptedIsrcCandidatesWhenEnabled() {
        MetadataNormalizationAdminService adminService = mock(MetadataNormalizationAdminService.class);
        MetadataNormalizationApplyScheduler scheduler = new MetadataNormalizationApplyScheduler(adminService);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "adminUserId", "user-admin");
        ReflectionTestUtils.setField(scheduler, "batchLimit", 25);
        when(adminService.applyAcceptedIsrcCandidates("user-admin", 25))
            .thenReturn(new MetadataNormalizationAdminService.IsrcApplyResult(3, 2, List.of(), List.of(), List.of()));

        scheduler.run();

        verify(adminService).applyAcceptedIsrcCandidates("user-admin", 25);
    }
}
