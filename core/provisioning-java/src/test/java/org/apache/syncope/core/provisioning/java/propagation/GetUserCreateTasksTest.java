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
public class GetUserCreateTasksTest extends DefaultPropagationManagerTest {

    private String password;
    private PropagationByResource<Pair<String, String>> propByLinkedAccount;

    // Aggiunta per mutation testing
    private boolean noProp;

    
    public GetUserCreateTasksTest(ParamType keyType, ParamType passType, Boolean enable, ParamType propByResType, ParamType propByLinkedAccountType, boolean noProp, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
        super(AnyTypeKind.USER);
        configure(keyType, passType, enable, propByResType, propByLinkedAccountType, noProp, vAttrType, noPropResourceKeysType, expectedType);
    }

    private void configure(ParamType keyType, ParamType passType, Boolean enable, ParamType propByResType, ParamType propByLinkedAccountType, boolean noProp, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
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
        this.noProp = noProp;

        configureAnyType(AnyTypeKind.USER, this.getClass().getName());
        configureKey(keyType);
        configurePass(passType);
        configurePropByRes(propByResType);
        configureLinkedAccount(propByLinkedAccountType, noProp);
        configureVAttr(vAttrType);
        configureNoPropResourceKeys(noPropResourceKeysType);
        configureExpected(expectedType);
    }

    private void configurePass(ParamType passType) {
        switch (passType) {
            case NULL:
                this.password = null;
                break;
            case EMPTY:
                this.password = "";
                break;
            case VALID:
                this.password = "myPass";
                break;
            default:
                break;
        }
    }

    private void configureLinkedAccount(ParamType propByLinkedAccountType, boolean noProp) {
        Pair<String, String> pair = new ImmutablePair<>("email", MY_EMAIL);
        PropagationByResource<Pair<String, String>> linked = new PropagationByResource<>();

        switch (propByLinkedAccountType) {
            case NULL:
                this.propByLinkedAccount = null;
                break;
            case EMPTY:
            	// Mutation testing: propByLinkedAccount è vuoto
                this.propByLinkedAccount = linked;
                break;
            case VALID:
                if (noProp) {
                    pair = new ImmutablePair<>("validKey", "myAccount");
                    linked.add(ResourceOperation.CREATE, pair);
                } else {
                    linked.add(ResourceOperation.CREATE, pair);
                }
                this.propByLinkedAccount = linked;
                break;
            case INVALID:
                linked.add(ResourceOperation.DELETE, pair);
                this.propByLinkedAccount = linked;
                break;
            default:
                break;
        }

    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                //  KEY 				PASSWORD 		   	ENABLE 	PROP_BY_RES 		PROP_BY_LINK_ACC 		NO_PROP			V_ATTR 				NO_PRO_RES_KEY    	EXPECTED_RESULT  
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.INVALID,	ParamType.VALID, 	null, 	ParamType.VALID, 	ParamType.VALID,		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	ParamType.EMPTY, 	ParamType.VALID, 	null, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	ParamType.NULL, 	ParamType.VALID,	null, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	ParamType.VALID, 	ParamType.EMPTY,	null, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.NULL, 	null, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	true, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	false,	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.INVALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.FAIL				},
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.VALID, 	ParamType.INVALID, 		false,			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.VALID, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.VALID, 	ParamType.VALID, 		false,			ParamType.EMPTY, 	ParamType.VALID, 	ExpectedType.FAIL				},
        
                // Coverage & Mutation
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.VALID, 	ParamType.VALID, 		true, 			ParamType.EMPTY, 	ParamType.VALID, 	ExpectedType.FAIL				}, 
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.EMPTY, 	ParamType.VALID, 		true, 			ParamType.EMPTY, 	ParamType.VALID, 	ExpectedType.FAIL				}, 
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.EMPTY, 	ParamType.VALID, 		true, 			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	ParamType.VALID, 	ParamType.VALID, 	null, 	ParamType.EMPTY, 	ParamType.EMPTY, 		false, 			ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.FAIL				}
        });
    }

    @Test
    public void testGetUserCreateTask() {
        List<PropagationTaskInfo> createTasks;
        try {
            createTasks = propagationManager.getUserCreateTasks(key, password, enable, propByRes, propByLinkedAccount, vAttr, noPropResourceKeys);
            if (createTasks.size() == 1) {
                PropagationTaskInfo propagationTaskInfo = createTasks.get(0);
                String anyType = propagationTaskInfo.getAnyType();

                assertEquals("USER", anyType);
                if (!vAttr.isEmpty()) {
                    String attributes = propagationTaskInfo.getAttributes();
                    assertTrue(attributes.contains(MY_NAME) && attributes.contains(MY_UID));
                }
            }

            assertEquals(expected.size(), createTasks.size());
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
            return;
        }

    }

}
