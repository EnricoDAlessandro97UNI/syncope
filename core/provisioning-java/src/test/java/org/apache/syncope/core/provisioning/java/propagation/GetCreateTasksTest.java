package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.common.lib.types.AnyTypeKind;
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

    public GetCreateTasksTest(AnyTypeKind anyTypeKind, ParamType keyType, Boolean enable, ParamType propByResType, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
        super(anyTypeKind);
        System.out.println("anyTypeKind = " + anyTypeKind + ", keyType = " + keyType + ", enable = " + enable + ", propByResType = " + propByResType + ", vAttrType = " + vAttrType + ", noPropResourceKeysType = " + noPropResourceKeysType + ", ExpectedType = " + expectedType);
        configure(anyTypeKind, keyType, enable, propByResType, vAttrType, noPropResourceKeysType, expectedType);
    }

    private void configure(AnyTypeKind anyTypeKind, ParamType keyType, Boolean enable, ParamType propByResType, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
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

        configureAnyType(anyTypeKind, this.getClass().getName());
        configureKey(keyType);
        configurePropByRes(propByResType);
        configureVAttr(vAttrType);
        configureNoPropResourceKeys(noPropResourceKeysType);
        configureExpected(expectedType);
        System.out.println("anyTypeKind = " + anyTypeKind + ", key = " + key + ", enable = " + enable + ", propByRes = " + propByRes+ ", vAttr = " + vAttr + ", noPropResourceKeys = " + noPropResourceKeys + ", ExpectedType = " + expectedType);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
        		// ANY_TYPE_KIND		KEY					ENABLE	PROP_BY_RES			V_ATTR				NO_PROP_RES_KEY		EXPECTED_RESULT_TYPE
        		// Unidimensionale
                {	null, 				ParamType.NULL, 	true, 	ParamType.NULL, 	ParamType.NULL, 	ParamType.NULL, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 	ParamType.NULL, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 	ParamType.EMPTY, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 	ParamType.INVALID,	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	false,	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.EMPTY, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 	ParamType.VALID,	true, 	ParamType.INVALID,	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.EMPTY, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.NULL,	 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.INVALID,	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.NULL, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.INVALID,	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.EMPTY, 	ExpectedType.OK					}
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
        }
        assertEquals(expected.size(), createTasks.size());
    }
}