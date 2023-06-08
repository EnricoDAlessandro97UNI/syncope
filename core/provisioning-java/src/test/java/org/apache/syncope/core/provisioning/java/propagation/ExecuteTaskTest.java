package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.dao.JPAExternalResourceDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskExec;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.propagation.utils.ParamType;
import org.apache.syncope.core.provisioning.java.propagation.utils.ExpectedType;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.identityconnectors.framework.common.objects.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.*;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Test class for getCreateTasks method of DefaultPropagationManager class
 * 
 * @author Enrico D'Alessandro
 */
@RunWith(Parameterized.class)
public class ExecuteTaskTest extends PriorityPropagationTaskExecutorTest {

    @Spy private DefaultListableBeanFactory factory;
    private PropagationTaskExecutor propagationTaskExecutor;
    @InjectMocks private DefaultPropagationTaskCallable taskCallable;
    @Spy private PropagationTaskExecutor taskExecutor;
    @Mock private JPAExternalResourceDAO resourceDAO;
    @Spy private EntityFactory entityFactory;
    @Mock private ConnectorManager connectorManager;
    @Mock private NotificationManager notificationManager;
    @Mock private AuditManager auditManager;
    @Mock private Connector connector;
    @Mock private TaskDAO taskDAO;

    private Collection<PropagationTaskInfo> taskInfos;
    private boolean nullPriorityAsync;
    private String executor;

    private Exception expectedError;
    private PropagationReporter expected;

    private int numElements; // aggiunta per coverage
    private int numSamePriority; // aggiunta per mutation testing
    private int numWithPriority; // aggiunta per mutation testing
    
    @SuppressWarnings("deprecation")
	public ExecuteTaskTest(ParamType taskInfoType, int numElements, int numSamePriority, int numWithPriority, boolean nullPriorityAsync, ParamType executorType, ExpectedType expectedType) {
        MockitoAnnotations.initMocks(this);
        configure(taskInfoType, numElements, numSamePriority, numWithPriority, nullPriorityAsync, executorType, expectedType);
    }

    private void configure(ParamType taskInfoType, int numElements, int numSamePriority, int numWithPriority, boolean nullPriorityAsync, ParamType executorType, ExpectedType expectedType) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        this.propagationTaskExecutor = new PriorityPropagationTaskExecutor(connectorManager, null, null, null, null, taskDAO, resourceDAO, notificationManager, auditManager, null, null, null, entityFactory, null, executor);
        this.nullPriorityAsync = nullPriorityAsync;
        this.numElements = numElements;
        this.numSamePriority = numSamePriority;
        this.numWithPriority = numWithPriority;
        List<Boolean> withPriority = configureTaskInfos(taskInfoType, numElements, numSamePriority, numWithPriority);
        configureExecutor(executorType);
        configureResult(expectedType, withPriority);
    }

    private void configureResult(ExpectedType ExpectedType, List<Boolean> withPriority) {
        List<PropagationStatus> statuses = new ArrayList<>();
        // Ordino i task per priorità così da poterne verificare la corretta esecuzione in base alla priorità
        List<PropagationTaskInfo> sorted = taskInfos.stream().sorted(Comparator.comparing(o -> o.getExternalResource().getPropagationPriority())).collect(Collectors.toList());
        switch (ExpectedType) {
            case OK:
            	/* Esecuzione con successo o CREATA */
            	for (int i = 0; i < numElements; i++) {
                    PropagationTaskInfo taskInfo = sorted.get(i);
                    this.expected = Mockito.mock(DefaultPropagationReporter.class);
                    PropagationStatus status = new PropagationStatus();
                    if (!withPriority.get(i) && nullPriorityAsync) {
                        status.setStatus(ExecStatus.CREATED);
                    } else {
                        status.setStatus(ExecStatus.SUCCESS);
                    }
                    status.setResource(taskInfo.getExternalResource().getKey());
                    statuses.add(status);
                }
                break;
            case FAIL:
            	/* Esecuzione con fallimento */
            	PropagationTaskInfo taskInfo = sorted.get(0);
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                PropagationStatus statusFail = new PropagationStatus();
                statusFail.setStatus(ExecStatus.FAILURE);
                statusFail.setResource(taskInfo.getExternalResource().getKey());
                statuses.add(statusFail);
                break;
            case VOID:
                /* Nessun task è stato eseguito */
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                break;
            case NULL_PTR_ERROR:
            	/* Eccezione */
                this.expectedError = new NullPointerException();
                break;
			default:
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
            default:
            	break;
        }
    }

    private List<Boolean> configureTaskInfos(ParamType taskInfoType, int numElements, int numSamePriority, int numWithPriority) {
    	List<Boolean> withPriority = new ArrayList<>();
        switch (taskInfoType) {
            case EMPTY:
                /* Collezione di task vuoti */
                this.taskInfos = new ArrayList<>();
                break;
            case VALID:
                /* Collezione di task non vuoti con priorità */
                this.taskInfos = new ArrayList<>();
                for (int i = 0; i < numElements; i++) {
                	boolean priority = false;
                    JPAExternalResource resource = new JPAExternalResource();
                    if (i < numWithPriority) {
                    	// Caso con un insieme di task misti (con e senza priorità)
                        if (i < numSamePriority) {
                            // Caso in cui task multipli hanno la stessa priorità
                            resource.setPropagationPriority(numElements);
                        } else {
                            // I restanti task verranno ordinati per priorità grazie all'indice "i"
                            resource.setPropagationPriority(i);
                        }
                        resource.setKey("priorityResource"+i);
                        priority = true;
                    } else {
                        resource.setKey("nonPriorityResource"+i);
                    }
                    resource.setConnector(new JPAConnInstance());
                    PropagationTaskInfo task = new PropagationTaskInfo(resource);
                    task.setObjectClassName("objectClassName"+i);
                    task.setOperation(ResourceOperation.CREATE);
                    task.setAttributes("[{\"name\":\"__NAME__\",\"value\":[\"Name"+i+"\"]}," + "{\"name\":\"__UID__\",\"value\":[\"uid"+i+"\"]}]");
                    withPriority.add(priority);
                    this.taskInfos.add(task);
                }
                break;
            case INVALID:
                /* Collezione di task non vuoti senza priorità */
                this.taskInfos = new ArrayList<>();
                this.taskInfos.add(new PropagationTaskInfo(new JPAExternalResource()));
                break;
            default:
                break;
        }
        return withPriority;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {                
//             	TASK_INFO_TYPE		NUM_ELEMENTS	NUM_SAME_PR	NUM_WITH_PR	NULL_PRIORITY_ASYNC     EXECUTOR       		EXPECTED_RESULT
				{ParamType.EMPTY, 	1, 				1, 			1, 			false, 					ParamType.VALID, 	ExpectedType.VOID	},
				{ParamType.VALID, 	1, 				1, 			0, 			false, 					ParamType.VALID, 	ExpectedType.OK		},
				{ParamType.VALID, 	1, 				1, 			1, 			false, 					ParamType.VALID,	ExpectedType.OK		},
				{ParamType.VALID, 	1, 				1, 			0, 			true, 					ParamType.VALID, 	ExpectedType.OK		},
				{ParamType.VALID, 	1, 				1, 			1, 			false, 					ParamType.EMPTY, 	ExpectedType.FAIL	},
				{ParamType.VALID, 	1, 				1, 			1, 			false, 					ParamType.NULL, 	ExpectedType.FAIL	},
				
				/* For coverage and mutation */
				{ParamType.VALID, 	2, 				1, 			2, 			false, 					ParamType.VALID, 	ExpectedType.OK		},
				{ParamType.VALID, 	3, 				2, 			3, 			false, 					ParamType.VALID, 	ExpectedType.OK		}   
        });
    }

    @Before
    public void mockExecute() {
        /* taskCallable deve essere non null in taskExecutor */
        factory.registerSingleton("callable", taskCallable);
        context = Mockito.mockStatic(ApplicationContextProvider.class);
        context.when(ApplicationContextProvider::getBeanFactory).thenReturn(factory);
        doReturn(taskCallable).when(factory).createBean(DefaultPropagationTaskCallable.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);

        /* Stub metodo execute() di taskExecutor */
        doAnswer(invocationOnMock -> {
            DefaultPropagationReporter reporter = invocationOnMock.getArgument(1, DefaultPropagationReporter.class);
            PropagationTaskInfo propTaskInfo = invocationOnMock.getArgument(0, PropagationTaskInfo.class);
            return propagationTaskExecutor.execute(propTaskInfo, reporter, "validExecutor");
        }).when(taskExecutor).execute(any(), any(), anyString());

        /* mock entityFactory newEntity() method calls */
        PropagationTask task = new JPAPropagationTask();
        TaskExec exec = new JPATaskExec();
        when(entityFactory.newEntity(PropagationTask.class)).thenReturn(task);
        when(entityFactory.newEntity(TaskExec.class)).thenReturn(exec);

        /* Mock read connector from connectorManager */
        when(connectorManager.getConnector(any())).thenReturn(connector);
        doAnswer(invocationOnMock -> {
            AtomicReference propagationAttempt = invocationOnMock.getArgument(3, AtomicReference.class);
            propagationAttempt.set(true);
            return new Uid("newUid");
        }).when(connector).create(any(), any(), any(), any());
    }

    @Test
    public void testExecute() {
        PropagationReporter reporter;
        try {
            reporter = propagationTaskExecutor.execute(taskInfos, nullPriorityAsync, executor);
            assertEquals(expected.getStatuses().size(), reporter.getStatuses().size());

            for (int i = 0; i < expected.getStatuses().size(); i++) {
                // Verifico che i task siano stati eseguiti in ordine 
                assertEquals(expected.getStatuses().get(i).getStatus(), reporter.getStatuses().get(i).getStatus());
                assertEquals(expected.getStatuses().get(i).getResource(), reporter.getStatuses().get(i).getResource());
            }
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
            return;
        }
    }


}