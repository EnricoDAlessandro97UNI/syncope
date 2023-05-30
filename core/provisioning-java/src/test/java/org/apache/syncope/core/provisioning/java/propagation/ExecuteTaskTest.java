package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceOperation;
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

    private Collection<PropagationTaskInfo> taskInfos;
    private boolean nullPriorityAsync;
    private String executor;

    private Exception expectedError;
    private PropagationReporter expected;

    @SuppressWarnings("deprecation")
	public ExecuteTaskTest(ParamType taskInfoType, int numElements, boolean nullPriorityAsync, ParamType executorType, ExpectedType ExpectedType) {
        MockitoAnnotations.initMocks(this);
        configure(taskInfoType, numElements, nullPriorityAsync, executorType, ExpectedType);
    }

    private void configure(ParamType taskInfoType, int numElements, boolean nullPriorityAsync, ParamType executorType, ExpectedType ExpectedType) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        this.propagationTaskExecutor = new PriorityPropagationTaskExecutor(connectorManager, null, null, null, null, null, resourceDAO, notificationManager, auditManager, null, null, null, entityFactory, null, executor);
        this.nullPriorityAsync = nullPriorityAsync;
        boolean withPriority = configureTaskInfos(taskInfoType, numElements);
        configureExecutor(executorType);
        configureResult(ExpectedType, withPriority);
    }

    private void configureResult(ExpectedType ExpectedType, boolean withPriority) {
        List<PropagationStatus> statuses = new ArrayList<>();
        switch (ExpectedType) {
            case OK:
            	/* Esecuzione con successo o CREATA */
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                PropagationStatus status = new PropagationStatus();
                if (withPriority) {
                    status.setStatus(ExecStatus.SUCCESS);
                } else {
                    status.setStatus(ExecStatus.CREATED);
                }
                statuses.add(status);
                break;
            case FAIL:
            	/* Esecuzione con fallimento */
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                PropagationStatus statusFail = new PropagationStatus();
                statusFail.setStatus(ExecStatus.FAILURE);
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
            default:
            	break;
        }
    }

    private boolean configureTaskInfos(ParamType taskInfoType, int numElements) {
        boolean withPriority = false;
        switch (taskInfoType) {
            case EMPTY:
                /* Collezione di task vuoti */
                this.taskInfos = new ArrayList<>();
                break;
            case VALID:
                /* Collezione di task non vuoti con priorità */
                withPriority = true;
                this.taskInfos = new ArrayList<>();
                for (int i = 0; i < numElements; i++) {
                    JPAExternalResource resource = new JPAExternalResource();
                    resource.setKey("priorityResource"+i);
                    resource.setPropagationPriority(numElements-i); // Verifica ordinamento dei task
                    resource.setConnector(new JPAConnInstance());
                    PropagationTaskInfo task = new PropagationTaskInfo(resource);
                    task.setObjectClassName("objectClassName"+i);
                    task.setOperation(ResourceOperation.CREATE);
                    task.setAttributes("[{\"name\":\"__NAME__\",\"value\":[\"Name"+i+"\"]},{\"name\":\"__UID__\",\"value\":[\"uid"+i+"\"]}]");
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
//             	TASK_INFO_TYPE		NUM_ELEMENTS	NULL_PRIORITY_ASYNC     EXECUTOR       		EXPECTED_RESULT
              {	ParamType.EMPTY, 	1,				false, 					ParamType.VALID, 	ExpectedType.VOID			},
              {	ParamType.VALID, 	1,				false, 					ParamType.VALID, 	ExpectedType.OK				},
              {	ParamType.INVALID, 	1,				false, 					ParamType.VALID,	ExpectedType.VOID			},
              {	ParamType.INVALID, 	1,				true, 					ParamType.VALID,	ExpectedType.OK 			},
              {	ParamType.VALID, 	1,				false, 					ParamType.EMPTY,	ExpectedType.FAIL			},
              {	ParamType.VALID, 	1,				false, 					ParamType.NULL, 	ExpectedType.FAIL			},
              
              /* For coverage and mutation */
              {	ParamType.VALID, 	2, 				false, 					ParamType.VALID, 	ExpectedType.OK				}
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
        taskInfos.forEach(taskInfo -> {
            doAnswer(invocationOnMock -> {
                DefaultPropagationReporter reporter = invocationOnMock.getArgument(1, DefaultPropagationReporter.class);
                return propagationTaskExecutor.execute(taskInfo, reporter, "validExecutor");
            }).when(taskExecutor).execute(any(), argThat(propagationReporter -> propagationReporter.getStatuses().isEmpty()), anyString());

            /* Mock entityFactory metodo newEntity() */
            PropagationTask task = new JPAPropagationTask();
            TaskExec exec = new JPATaskExec();
            when(entityFactory.newEntity(PropagationTask.class)).thenReturn(task);
            when(entityFactory.newEntity(TaskExec.class)).thenReturn(exec);

            /* Mock connectorManager metodo getConnetor()  */
            when(connectorManager.getConnector(any())).thenReturn(connector);
            doAnswer(invocationOnMock -> {
                AtomicReference propagationAttempt = invocationOnMock.getArgument(3, AtomicReference.class);
                propagationAttempt.set(true);
                return new Uid("newUid");
            }).when(connector).create(any(), any(), any(), any());
        });
    }

    @Test
    public void testExecute() {
        PropagationReporter reporter = null;
        try {
            reporter = propagationTaskExecutor.execute(taskInfos, nullPriorityAsync, executor);
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
            return;
        }
        
        for (int i = 0; i < expected.getStatuses().size(); i++) {
            assertEquals(expected.getStatuses().get(i).getStatus(), reporter.getStatuses().get(i).getStatus());
        }
    }


}
