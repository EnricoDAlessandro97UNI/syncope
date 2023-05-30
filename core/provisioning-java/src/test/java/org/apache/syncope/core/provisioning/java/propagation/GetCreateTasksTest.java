package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.propagation.utils.ExpectedType;
import org.apache.syncope.core.provisioning.java.propagation.utils.ParamType;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test class for getCreateTasks method of DefaultPropagationManager class
 * 
 * @author Enrico D'Alessandro - University of Rome Tor Vergata
 */
@RunWith(Parameterized.class)
public class GetCreateTasksTest extends DefaultPropagationManagerTest {
	
	// Aggiunte per il controllo delle mutazioni
    private boolean repeatedResUpdate;
    private boolean repeatedResDelete;
    private boolean wrongOpRightKey;
	
    public GetCreateTasksTest(AnyTypeKind anyTypeKind, ParamType keyType, Boolean enable,
            ParamType propByResType, boolean repeatedResUpdate, boolean repeatedResDelete,
            boolean wrongOpRightKey,
            ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
        super(anyTypeKind);
        System.out.println("anyTypeKind = " + anyTypeKind + ", keyType = " + keyType + ", enable = " + enable + ", propByResType = " + propByResType + ", vAttrType = " + vAttrType + ", noPropResourceKeysType = " + noPropResourceKeysType + ", ExpectedType = " + expectedType);
        configure(anyTypeKind, keyType, enable, propByResType, repeatedResUpdate, repeatedResDelete, wrongOpRightKey,
                vAttrType, noPropResourceKeysType, expectedType);
    }

    private void configure(AnyTypeKind anyTypeKind, ParamType keyType, Boolean enable, ParamType propByResType, boolean repeatedResUpdate, boolean repeatedResDelete, boolean wrongOpRightKey, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
        System.out.println("IN CONFIG: "+"anyTypeKind = " + anyTypeKind + ", keyType = " + keyType + ", enable = " + enable + ", propByResType = " + propByResType + ", vAttrType = " + vAttrType + ", noPropResourceKeysType = " + noPropResourceKeysType + ", ExpectedType = " + expectedType);
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
        this.repeatedResUpdate = repeatedResUpdate;
        this.repeatedResDelete = repeatedResDelete;
        this.wrongOpRightKey = wrongOpRightKey;
        
        configureAnyType(anyTypeKind, this.getClass().getName());
        configureKey(keyType);
        configurePropByRes(propByResType, repeatedResUpdate, repeatedResDelete, wrongOpRightKey);
        configureVAttr(vAttrType);
        configureNoPropResourceKeys(noPropResourceKeysType);
        configureExpected(expectedType);
        System.out.println("anyTypeKind = " + anyTypeKind + ", key = " + key + ", enable = " + enable + ", propByRes = " + propByRes+ ", vAttr = " + vAttr + ", noPropResourceKeys = " + noPropResourceKeys + ", ExpectedType = " + expectedType);
    }

    protected void configureExpected(ExpectedType expectedType) {
        switch (expectedType) {
            case OK:
                PropagationTaskInfo taskInfo = new PropagationTaskInfo(externalResourceDAO.find("testResource"));
                taskInfo.setAnyType(anyTypeKind.name());
                taskInfo.setAnyTypeKind(anyTypeKind);
                taskInfo.setObjectClassName("__ACCOUNT__");
                taskInfo.setConnObjectKey("info");
                switch (anyTypeKind) {
                    case USER:
                        taskInfo.setEntityKey("userTested");
                        break;
                    case GROUP:
                        taskInfo.setEntityKey("groupTested");
                        break;
                    case ANY_OBJECT:
                        taskInfo.setEntityKey("anyObjectTested");
                        break;
                    default:
                        break;
                }
                if (repeatedResUpdate) {
                    taskInfo.setOperation(ResourceOperation.UPDATE);
                } else if (wrongOpRightKey){
                    taskInfo.setOperation(ResourceOperation.DELETE);
                } else {
                    taskInfo.setOperation(ResourceOperation.CREATE);
                }
                if (repeatedResDelete) {
                    taskInfo.setOperation(ResourceOperation.DELETE);
                }
                this.expected = new ArrayList<>(Collections.singleton(taskInfo));
                break;
            case NULL_PTR_ERROR:
                this.expectedError = new NullPointerException();
                break;
            case NOT_FOUND_ERROR:
                this.expectedError = new NotFoundException("msg");
                break;
            case FAIL:
                this.expected = new ArrayList<>();
                break;
            default:
                System.out.println("CASE DEFAULT");
                break;
        }
    }
    
    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
        		// 	ANY_TYPE_KIND			KEY					ENABLE	PROP_BY_RES			REP_RES_UPDATE		REP_RES_DELETE		WRONG_OP_RIGHT_KEY			V_ATTR				NO_PROP_RES_KEY		EXPECTED_RESULT_TYPE
        		//	Unidimensionale
        		{	AnyTypeKind.USER, 		ParamType.VALID, 	null, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
        		{	AnyTypeKind.GROUP, 		ParamType.VALID, 	null, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.ANY_OBJECT, ParamType.VALID, 	null, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.INVALID, 	null, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 		ParamType.NULL, 	null, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID,	null, 	ParamType.INVALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	null, 	ParamType.VALID, 	false,				false,				false,						ParamType.VALID, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	null, 	ParamType.VALID, 	false,				false,				false,						ParamType.EMPTY, 	ParamType.VALID, 	ExpectedType.FAIL				},
        
                // coverage & mutation 
                {	AnyTypeKind.USER, 		ParamType.VALID, 	null,	ParamType.NULL, 	false, 				false, 				false, 						ParamType.EMPTY, 	ParamType.EMPTY,	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	null, 	ParamType.EMPTY, 	false, 				false, 				false, 						ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.FAIL				}
                
        });
    }

    @Test
    public void testGetCreateTask() {
        System.out.println("anyType: "+anyTypeKind);
        System.out.println("key: "+key);
        System.out.println("enable: "+enable);
        System.out.println("propByRes: "+propByRes);
        System.out.println("vAttr: "+vAttr);
        System.out.println("noPropResourceKeys: "+noPropResourceKeys);
        List<PropagationTaskInfo> createTasks = null;
        try {
            createTasks = propagationManager.getCreateTasks(anyTypeKind, key, enable, propByRes, vAttr, noPropResourceKeys);
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
            return;
        }

        /* test result size of created tasks */
        if (createTasks.size() == 1) {
            PropagationTaskInfo propagationTaskInfo = createTasks.get(0);
            PropagationTaskInfo expectedTask = expected.get(0);
            String attributes = propagationTaskInfo.getAttributes();
            String anyType = propagationTaskInfo.getAnyType();

            System.out.println(attributes);
            if (anyType.equals("GROUP")) {
                assertTrue(attributes.contains("Group Name"));
                assertTrue(attributes.contains("groupuid"));
            } else if (anyType.equals("USER")){
                assertTrue(attributes.contains(MY_NAME));
                assertTrue(attributes.contains(MY_UID));
            } else {
                assertTrue(attributes.contains("Any Name"));
                assertTrue(attributes.contains("anyobjuid"));
            }
            
            assertEquals(expectedTask.getOperation(), propagationTaskInfo.getOperation());
            assertEquals(expectedTask.getObjectClassName(), propagationTaskInfo.getObjectClassName());
            assertEquals(expectedTask.getAnyType(), propagationTaskInfo.getAnyType());
            assertEquals(expectedTask.getAnyTypeKind(), propagationTaskInfo.getAnyTypeKind());
            assertEquals(expectedTask.getEntityKey(), propagationTaskInfo.getEntityKey());
            assertEquals(expectedTask.getConnObjectKey(), propagationTaskInfo.getConnObjectKey());
        }
        
        assertEquals(expected.size(), createTasks.size());
    }
}