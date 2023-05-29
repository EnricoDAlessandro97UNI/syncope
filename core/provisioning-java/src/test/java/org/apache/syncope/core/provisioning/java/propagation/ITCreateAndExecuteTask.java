package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.propagation.utils.ParamType;
import org.apache.syncope.core.provisioning.java.propagation.utils.ExpectedType;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Test class 
 * 
 * @author Enrico D'Alessandro
 */
@RunWith(Parameterized.class)
public class ITCreateAndExecuteTask extends ITPropagationManagerAndExecutor {

    private PriorityPropagationTaskExecutor taskExecutor;
    private DefaultPropagationManager propagationManager;

    /* Creation attributes */
    private AnyTypeKind anyTypeKind;
    private String key;
    private Boolean enable;
    private PropagationByResource<String> propByRes;
    private Collection<Attr> vAttr;
    private Collection<String> noPropResourceKeys;

    /* Execution attributes */
    private Collection<PropagationTaskInfo> taskInfos;
    private boolean nullPriorityAsync; // TRUE or FALSE
    private String executor;

    private Exception expectedError;
    private PropagationReporter expected;

    public ITCreateAndExecuteTask(AnyTypeKind anyTypeKind, ParamType keyType, Boolean enable, ParamType propByResType, ParamType vAttrType, ParamType noPropResourceKeysType, ParamType taskInfoType, boolean nullPriorityAsync, ParamType executorType, ExpectedType expectedType) {
        super(anyTypeKind);
        configureCreation(anyTypeKind, keyType, enable, propByResType, vAttrType, noPropResourceKeysType);
        configureExecution(taskInfoType, nullPriorityAsync, executorType);
        configureResult(expectedType);
    }

    private void configureResult(ExpectedType expectedType) {
        List<PropagationStatus> statuses = new ArrayList<>();
        switch (expectedType) {
            case OK:
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                PropagationStatus statusCreated = new PropagationStatus();
                statusCreated.setStatus(ExecStatus.CREATED);
                statuses.add(statusCreated);
                break;
            case FAIL:
                this.expected = Mockito.mock(DefaultPropagationReporter.class);
                // To FIX
                break;
            case NULL_PTR_ERROR:
                this.expectedError = new NullPointerException();
                break;
            case NOT_FOUND_ERROR:
                this.expectedError = new NotFoundException("Not found");
                break;
        }
        if (expected != null) Mockito.when(this.expected.getStatuses()).thenReturn(statuses);
    }

    private void configureExecution(ParamType taskInfoType, boolean nullPriorityAsync, ParamType executorType) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        this.taskExecutor = new PriorityPropagationTaskExecutor(null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, executor);
        this.nullPriorityAsync = nullPriorityAsync;
        configureTaskInfos(taskInfoType);
        configureExecutor(executorType);
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
                // TODO forse non ci sta un executor non valido?
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

    private void configureCreation(AnyTypeKind anyTypeKind, ParamType keyType, Boolean enable, ParamType propByResType, ParamType vAttrType, ParamType noPropResourceKeysType) {
        this.propagationManager = new DefaultPropagationManager(
                virSchemaDAO,
                externalResourceDAO,
                null,
                null,
                mappingManager,
                derAttrHandler,
                anyUtilsFactory
        );

        this.anyTypeKind = anyTypeKind;
        this.enable = enable;

        configureAnyType(anyTypeKind);
        configureKey(keyType);
        configurePropByRes(propByResType);
        configureVAttr(vAttrType);
        configureNoPropResourceKeys(noPropResourceKeysType);
    }

    private void configureNoPropResourceKeys(ParamType noPropResourceKeysType) {
        switch (noPropResourceKeysType) {
            case NULL:
                System.out.println("CASE NULL");
                this.noPropResourceKeys = null;
                break;
            case EMPTY:
                System.out.println("CASE EMPTY");
                this.noPropResourceKeys = new ArrayList<>();
                break;
            case VALID:
                System.out.println("CASE VALID");
                this.noPropResourceKeys = new ArrayList<>();
                this.noPropResourceKeys.add("invalidKey");
                break;
            case INVALID:
                System.out.println("CASE INVALID");
                this.noPropResourceKeys = new ArrayList<>();
                this.noPropResourceKeys.add("validKey");
                break;
            default:
                System.out.println("CASE DEFAULT");
                break;
        }
    }

    private void configureVAttr(ParamType vAttrType) {
        Attr attr = new Attr();
        switch (vAttrType) {
            case NULL:
                System.out.println("CASE NULL");
                this.vAttr = null;
                break;
            case EMPTY:
                System.out.println("CASE EMPTY");
                this.vAttr = new ArrayList<>();
                break;
            case VALID:
                System.out.println("CASE VALID");
                attr.setSchema("vSchema");
                this.vAttr = new ArrayList<>();
                this.vAttr.add(attr);
                break;
            case INVALID:
                System.out.println("CASE INVALID");
                attr.setSchema("invalidSchema");
                this.vAttr = new ArrayList<>();
                this.vAttr.add(attr);
                break;
            default:
                System.out.println("CASE DEFAULT");
                break;
        }
    }

    private void configurePropByRes(ParamType propByResType) {
        switch (propByResType) {
            case NULL:
                System.out.println("CASE NULL");
                this.propByRes = null;
                break;
            case EMPTY:
                System.out.println("CASE EMPTY");
                this.propByRes = new PropagationByResource<>();
                break;
            case INVALID:
                System.out.println("CASE INVALID");
                this.propByRes = new PropagationByResource<>();
                this.propByRes.add(ResourceOperation.DELETE, "invalidKey");
                break;
            case VALID:
                System.out.println("CASE VALID");
                this.propByRes = new PropagationByResource<>();
                this.propByRes.add(ResourceOperation.CREATE, "validKey");
                break;
            default:
                System.out.println("CASE DEFAULT");
                break;
        }
    }


    private void configureKey(ParamType keyType) {
        switch (keyType) {
            case NULL:
                System.out.println("CASE NULL");
                this.key = null;
                break;
            case EMPTY:
                System.out.println("CASE EMPTY");
                this.key = "";
                break;
            case VALID:
                System.out.println("CASE VALID");
                this.key = "validKey";
                break;
            case INVALID:
                System.out.println("CASE INVALID");
                this.key = "invalidKey";
                break;
            default:
                System.out.println("CASE DEFAULT");
                break;
        }
    }

    private void configureAnyType(AnyTypeKind anyTypeKind) {
        if (anyTypeKind != null) {
            Set<Attribute> attributes = new HashSet<>();
            Attribute name;
            Attribute uid;
            ImmutablePair<String, Set<Attribute>> attrs = new ImmutablePair<>("info", attributes);
            switch (anyTypeKind) {
                case USER:
                    name = new Name("Enrico Dale");
                    uid = new Uid("erik97");
                    attributes.add(name);
                    attributes.add(uid);

                    /* User */
                    Mockito.when(mappingManager.prepareAttrsFromAny(any(User.class), eq("myPass"), eq(true), eq(enable), any(provision.getClass()))).thenReturn(attrs);
                    Mockito.when(mappingManager.prepareAttrsFromAny(any(User.class), eq(null), eq(true), eq(enable), any(provision.getClass()))).thenReturn(attrs);
                    break;
                case GROUP:
                    name = new Name("Group Name");
                    uid = new Uid("groupuid");
                    attributes.add(name);
                    attributes.add(uid);

                    /* Group */
                    Mockito.when(mappingManager.prepareAttrsFromAny(any(Group.class), eq(null), eq(true), eq(enable), any(provision.getClass()))).thenReturn(attrs);
                    break;
                case ANY_OBJECT:
                    name = new Name("Any Name");
                    uid = new Uid("anyobjuid");
                    attributes.add(name);
                    attributes.add(uid);

                    /* Group */
                    Mockito.when(mappingManager.prepareAttrsFromAny(any(AnyObject.class), eq(null), eq(true), eq(enable), any(provision.getClass()))).thenReturn(attrs);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + anyTypeKind);
            }
        }
    }

    @BeforeClass
    public static void init() {
        SecurityContext context = SecurityContextHolder.getContext();
        SecurityContextHolder.getContextHolderStrategy().setContext(context);
        SyncopeGrantedAuthority auth = new SyncopeGrantedAuthority("test");
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(auth);
        SecurityContextHolder.getContextHolderStrategy().getContext().setAuthentication(new AnonymousAuthenticationToken("test", "test", authorities));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.OK					},
                {  	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.EMPTY, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 	ParamType.NULL, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR  	},
                {	AnyTypeKind.USER, 	ParamType.EMPTY, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR 	},
                {	AnyTypeKind.USER, 	ParamType.INVALID,	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR  	},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	false,	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.EMPTY, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.INVALID,	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.EMPTY, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID,	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.NULL, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.INVALID,	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true,	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.NULL, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.INVALID,	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.EMPTY, 	ParamType.VALID, true, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, false, ParamType.VALID, 	ExpectedType.FAIL				},	
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.NULL,		ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, true, 	ParamType.INVALID, 	ExpectedType.OK					}

        });
    }

    @Test
    public void testIntegration() {
        PropagationReporter reporter;
        try {
            List<PropagationTaskInfo> createTasks = propagationManager.getCreateTasks(anyTypeKind, key, enable, propByRes, vAttr, noPropResourceKeys);
            reporter = taskExecutor.execute(createTasks, nullPriorityAsync, executor);
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
            return;
        }
        assertEquals(expected.getStatuses(), reporter.getStatuses());
    }
}
