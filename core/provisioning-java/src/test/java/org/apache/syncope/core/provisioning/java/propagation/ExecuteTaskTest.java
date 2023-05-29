package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.propagation.utils.ParamType;
import org.apache.syncope.core.provisioning.java.propagation.utils.ExpectedType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ExecuteTaskTest extends PriorityPropagationTaskExecutorTest {

    private Collection<PropagationTaskInfo> taskInfos;
    private boolean nullPriorityAsync; // TRUE or FALSE
    private String executor;

    private Exception expectedError;
    private PropagationReporter expected;

    public ExecuteTaskTest(ParamType taskInfoType, boolean nullPriorityAsync, ParamType executorType, ExpectedType expectedType) {
        configure(taskInfoType, nullPriorityAsync, executorType, expectedType);
    }

    private void configure(ParamType taskInfoType, boolean nullPriorityAsync, ParamType executorType, ExpectedType expectedType) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        this.taskExecutor = new PriorityPropagationTaskExecutor(null, null, null, null, null, null, null, null, null, null, null, null, null, null, executor);
        this.nullPriorityAsync = nullPriorityAsync;
        configureTaskInfos(taskInfoType);
        configureExecutor(executorType);
        configureResult(expectedType);
    }

    private void configureResult(ExpectedType expectedType) {
        List<PropagationStatus> statuses = new ArrayList<>();
        switch (expectedType) {
            case OK:
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                PropagationStatus statusCreated = new PropagationStatus();
                // Indica che la richiesta HTTP è stata completata con successo e ha portato alla creazione di un nuovo oggetto o risorsa
                statusCreated.setStatus(ExecStatus.CREATED);
                statuses.add(statusCreated);
                break;
            case FAIL:
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                break;
            case NULL_PTR_ERROR:
                this.expectedError = new NullPointerException();
                break;
        }
        if (expected != null) Mockito.when(this.expected.getStatuses()).thenReturn(statuses);
    }

    private void configureExecutor(ParamType executorType) {
        switch (executorType) {
            case EMPTY:
                this.executor = "";
                break;
            case NULL:
                this.executor = null;
                break;
            case VALID:
                this.executor = "validExecutor";
                break;
            case INVALID:
                this.executor = "invalidExecutor";
                break;
        }
    }

    private void configureTaskInfos(ParamType taskInfoType) {
        switch (taskInfoType) {
            case EMPTY:
                this.taskInfos = new ArrayList<>();
                break;
            case NULL:
                this.taskInfos = null;
                break;
            case VALID:
                this.taskInfos = new ArrayList<>();
                this.taskInfos.add(new PropagationTaskInfo(new JPAExternalResource()));
                break;
            case INVALID:
                this.taskInfos = new ArrayList<>();
                this.taskInfos.add(new PropagationTaskInfo(null));
                break;
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
        		// 	TASK_INFO_TYPE      NULL_PRIORITY_ASYNC     EXECUTOR       		EXPECTED_RESULT
                {	ParamType.VALID, 	true,					ParamType.VALID, 	ExpectedType.OK				},
                {	ParamType.EMPTY, 	true, 					ParamType.VALID, 	ExpectedType.FAIL			},
                {	ParamType.NULL, 	true, 					ParamType.VALID, 	ExpectedType.NULL_PTR_ERROR	},
                {	ParamType.INVALID,	true, 					ParamType.VALID, 	ExpectedType.NULL_PTR_ERROR	},
                {	ParamType.VALID, 	false,					ParamType.VALID, 	ExpectedType.FAIL			},
                {	ParamType.VALID, 	true, 					ParamType.EMPTY, 	ExpectedType.OK				},
                {	ParamType.VALID, 	true, 					ParamType.NULL, 	ExpectedType.OK				},
                {	ParamType.VALID, 	true, 					ParamType.INVALID,	ExpectedType.OK				}
        });
    }

    @Test
    public void testExecute() {
        PropagationReporter reporter = null;
        try {
            reporter = taskExecutor.execute(taskInfos, nullPriorityAsync, executor);
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
            return;
        }
        assertEquals(expected.getStatuses(), reporter.getStatuses());
    }


}
