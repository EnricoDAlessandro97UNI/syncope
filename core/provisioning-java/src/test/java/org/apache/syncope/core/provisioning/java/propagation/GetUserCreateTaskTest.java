package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.propagation.utils.ParamType;
import org.apache.syncope.core.provisioning.java.propagation.utils.ExpectedType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test class for getUserCreateTasks method of DefaultPropagationManager class
 * 
 * @author Enrico D'Alessandro - University of Rome Tor Vergata
 */
@RunWith(Parameterized.class)
public class GetUserCreateTaskTest extends DefaultPropagationManagerTest {

    private String password;
    private PropagationByResource<Pair<String, String>> propByLinkedAccount;

    public GetUserCreateTaskTest(ParamType keyType, ParamType passType, Boolean enable, ParamType propByResType, ParamType propByLinkedAccountType, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
        super(AnyTypeKind.USER);
        configure(keyType, passType, enable, propByResType, propByLinkedAccountType, vAttrType, noPropResourceKeysType, expectedType);
    }

    private void configure(ParamType keyType, ParamType passType, Boolean enable, ParamType propByResType, ParamType propByLinkedAccountType, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
        this.propagationManager = new DefaultPropagationManager(
                virSchemaDAO,
                externalResourceDAO,
                null,
                null,
                mappingManager,
                derAttrHandler,
                anyUtilsFactory
        );

        this.enable = enable;
        this.anyTypeKind = AnyTypeKind.USER;

        configureAnyType(AnyTypeKind.USER, this.getClass().getName());
        configureKey(keyType);
        configurePass(passType);
        configurePropByRes(propByResType);
        configureLinkedAccount(propByLinkedAccountType);
        configureVAttr(vAttrType);
        configureNoPropResourceKeys(noPropResourceKeysType);
        configureExpected(expectedType);
        System.out.println("anyTypeKind = " + anyTypeKind + ", key = " + key + ", enable = " + enable + ", propByRes = " + propByRes+ ", vAttr = " + vAttr + ", noPropResourceKeys = " + noPropResourceKeys + ", ExpectedType = " + expectedType);
    }

    private void configurePass(ParamType passType) {
        switch (passType) {
            case NULL:
                System.out.println("CASE NULL");
                this.password = null;
                break;
            case EMPTY:
                System.out.println("CASE EMPTY");
                this.password = "";
                break;
            case VALID:
                System.out.println("CASE VALID");
                this.password = "myPass";
                break;
            case INVALID:
                System.out.println("CASE INVALID");
                this.password = "invalidKey";
                break;
            default:
                System.out.println("CASE DEFAULT");
                break;
        }
    }

    private void configureLinkedAccount(ParamType propByLinkedAccountType) {
        Pair<String, String> pair = new ImmutablePair<>("email", MY_EMAIL);
        PropagationByResource<Pair<String, String>> linked = new PropagationByResource<>();

        switch (propByLinkedAccountType) {
            case NULL:
                System.out.println("CASE NULL");
                this.propByLinkedAccount = null;
                break;
            case EMPTY:
                System.out.println("CASE EMPTY");
                this.propByLinkedAccount = linked;
                break;
            case VALID:
                System.out.println("CASE VALID");
                linked.add(ResourceOperation.CREATE, pair);
                this.propByLinkedAccount = linked;
                break;
            case INVALID:
                System.out.println("CASE INVALID");
                linked.add(ResourceOperation.DELETE, pair);
                this.propByLinkedAccount = linked;
                break;
            default:
                System.out.println("CASE DEFAULT");
                break;
        }

    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                //  KEY 				PASSWORD 		   	ENABLE 	PROP_BY_RES 	   	PROP_BY_LINK_ACC 	V_ATTR 				NO_PRO_RES_KEY    	EXPECTED_RESULT
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.INVALID,	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR	},
                {	ParamType.NULL, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR	},
                {	ParamType.EMPTY, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NOT_FOUND_ERROR	},
                {	ParamType.VALID, 	ParamType.EMPTY,	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NULL_PTR_ERROR		},
                {	ParamType.VALID, 	ParamType.NULL, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.INVALID,	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NULL_PTR_ERROR		},
                {	ParamType.VALID, 	ParamType.VALID, 	false,	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.EMPTY,	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.NULL, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.NULL_PTR_ERROR		},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.INVALID,	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.EMPTY, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.NULL, 	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.INVALID,	ParamType.VALID, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.EMPTY, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.NULL, 	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.INVALID,	ParamType.VALID, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.NULL, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.VALID, 	ParamType.INVALID,	ExpectedType.FAIL				},
        });
    }

    @Test
    public void testGetUserCreateTask() {
        System.out.println("key: "+key);
        System.out.println("password: "+password);
        System.out.println("enable: "+enable);
        System.out.println("propByRes: "+propByRes);
        System.out.println("propByLinkedAccount: "+propByLinkedAccount);
        System.out.println("vAttr: "+vAttr);
        System.out.println("noPropResourceKeys: "+noPropResourceKeys);
        List<PropagationTaskInfo> createTasks = null;
        try {
            createTasks = propagationManager.getUserCreateTasks(key, password, enable, propByRes, propByLinkedAccount, vAttr, noPropResourceKeys);
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
            return;
        }

        if (createTasks.size() == 1) {
            PropagationTaskInfo propagationTaskInfo = createTasks.get(0);
            String anyType = propagationTaskInfo.getAnyType();
            String attributes = propagationTaskInfo.getAttributes();

            assertEquals("USER", anyType);
            assertTrue(attributes.contains(MY_NAME) && attributes.contains(MY_UID));
        }

        assertEquals(expected.size(), createTasks.size());
    }

}
