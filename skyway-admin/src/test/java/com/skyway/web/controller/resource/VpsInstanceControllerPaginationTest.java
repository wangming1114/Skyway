package com.skyway.web.controller.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.skyway.common.core.page.TableDataInfo;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IVpsInstanceService;

@ExtendWith(MockitoExtension.class)
public class VpsInstanceControllerPaginationTest {

    @Mock
    private IVpsInstanceService vpsInstanceService;

    @Test
    public void expandsCategoryBeforeStartingVpsPagination() {
        TestController controller = new TestController();
        ReflectionTestUtils.setField(controller, "vpsInstanceService", vpsInstanceService);
        VpsInstance query = new VpsInstance();
        query.setCategoryId(3L);
        doAnswer(invocation -> {
            VpsInstance value = invocation.getArgument(0);
            value.setCategoryIds(Collections.singletonList(3L));
            value.setCategoryId(null);
            return null;
        }).when(vpsInstanceService).prepareListFilter(query);
        when(vpsInstanceService.selectList(query)).thenAnswer(invocation -> {
            assertTrue(controller.pageStarted);
            assertNull(query.getCategoryId());
            assertEquals(Collections.singletonList(3L), query.getCategoryIds());
            return Collections.singletonList(new VpsInstance());
        });

        TableDataInfo result = controller.list(query);

        assertEquals(1, result.getRows().size());
        InOrder order = inOrder(vpsInstanceService);
        order.verify(vpsInstanceService).prepareListFilter(query);
        order.verify(vpsInstanceService).selectList(query);
    }

    private static final class TestController extends VpsInstanceController {
        private boolean pageStarted;

        @Override
        protected void startPage() {
            pageStarted = true;
        }
    }
}
