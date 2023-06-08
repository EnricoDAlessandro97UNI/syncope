package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.propagation.utils.ExpectedType;
import org.apache.syncope.core.provisioning.java.propagation.utils.ParamType;
import org.identityconnectors.framework.common.objects.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;

@RunWith(Parameterized.class)
public class GetUpdateTasksTest extends DefaultPropagationManagerTest {

    private boolean changePwd;
    private PropagationByResource<Pair<String, String>> propByLinkedAccount;

    public GetUpdateTasksTest(AnyTypeKind anyTypeKind, ParamType keyType, boolean changePwd, Boolean enable, ParamType propByResType, ParamType propByLinkedType, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
        super(anyTypeKind);
        configure(anyTypeKind, keyType, enable, changePwd, propByResType, propByLinkedType, vAttrType, noPropResourceKeysType, expectedType);
    }

    private void configure(AnyTypeKind anyTypeKind, ParamType keyType, Boolean enable, boolean changePwd, ParamType propByResType, ParamType propByLinkedType, ParamType vAttrType, ParamType noPropResourceKeysType, ExpectedType expectedType) {
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
        this.changePwd = changePwd;

        configureAnyType(anyTypeKind, this.getClass().getName());
        configureKey(keyType);
        configurePropByRes(propByResType);
        configurePropByLinkedAccount(propByLinkedType);
        configureVAttr(vAttrType);
        configureNoPropResourceKeys(noPropResourceKeysType);
        configureExpected(expectedType);
    }

    protected void configurePropByRes(ParamType propByResType) {
        switch (propByResType) {
            case INVALID:
                this.propByRes = new PropagationByResource<>();
                this.propByRes.add(ResourceOperation.DELETE, "invalidKey");
                break;
            case VALID:
                this.propByRes = new PropagationByResource<>();
                this.propByRes.add(ResourceOperation.UPDATE, "validKey");
                break;
            default:
                break;
        }

    }

    protected void configureNoPropResourceKeys(ParamType noPropResourceKeysType) {
        switch (noPropResourceKeysType) {
            case VALID:
                this.noPropResourceKeys = new ArrayList<>();
                this.noPropResourceKeys.add("validKey");
                break;
            case EMPTY:
                this.noPropResourceKeys = new ArrayList<>();
                break;
            default:
                break;
        }
    }

    private void configurePropByLinkedAccount(ParamType propByLinkedType) {
        Pair<String, String> pair = new ImmutablePair<>("resourceKey", "connObjectKey");
        PropagationByResource<Pair<String, String>> linked = new PropagationByResource<>();
        switch (propByLinkedType) {
            case VALID:
                linked.add(ResourceOperation.UPDATE, pair);
                break;
            case INVALID:
                linked.add(ResourceOperation.DELETE, pair);
                break;
            default:
                break;
        }
        this.propByLinkedAccount = linked;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                //  KIND 					PASSWORD			CHANGE_PWD, ENABLE, PROP_BY_RES, 		PROP_BY_LINK_ACC,		V_ATTR 				NO_PROP_RES_KEY 	EXPECTED_RESULT_TYPE
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.GROUP, 		ParamType.VALID, 	true, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.ANY_OBJECT, ParamType.VALID, 	true, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.INVALID, 	true, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 		ParamType.NULL, 	true, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 		true, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 		false, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 		null, 	ParamType.INVALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 		null, 	ParamType.VALID, 	ParamType.INVALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.VALID, 	ParamType.EMPTY, 	ExpectedType.OK					},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	true, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.VALID, 	ExpectedType.FAIL				},
                {	AnyTypeKind.USER, 		ParamType.INVALID, 	false, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 		ParamType.NULL, 	false, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NOT_FOUND_ERROR	},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.EMPTY, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.NULL, 	ParamType.VALID, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.VALID, 	ParamType.EMPTY, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.VALID, 	ParamType.NULL, 		ParamType.EMPTY, 	ParamType.EMPTY, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.INVALID, 	ParamType.EMPTY, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.NULL, 	ParamType.EMPTY, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.VALID, 	ParamType.INVALID, 	ExpectedType.NULL_PTR_ERROR		},
                {	AnyTypeKind.USER, 		ParamType.VALID, 	false, 		null, 	ParamType.VALID, 	ParamType.VALID, 		ParamType.VALID, 	ParamType.NULL, 	ExpectedType.NULL_PTR_ERROR		}, 
        });
    }

    @Before
    public void setUpMocks() {
        Mockito.when(mappingManager.prepareAttrsFromAny(any(), argThat(s -> s == null || s.equals("myPass")), eq(changePwd), eq(enable), any(provision.getClass()))).thenAnswer(invocationOnMock -> {
            Set<Attribute> attributes = new HashSet<>();
            String[] connObjectKeyValue = new String[1];

            if (enable != null) {
                attributes.add(AttributeBuilder.buildEnabled(enable));
            }
            if (!changePwd) {
                Attribute pwdAttr = AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attributes);
                if (pwdAttr != null) {
                    attributes.remove(pwdAttr);
                }
            }

            return Pair.of(connObjectKeyValue[0], attributes);
        });
    }

    @Test
    public void test() {
        List<PropagationTaskInfo> updateTasks;
        try {
            updateTasks = propagationManager.getUpdateTasks(anyTypeKind, key, changePwd, enable, propByRes, propByLinkedAccount, vAttr, noPropResourceKeys);
            assertEquals(expected.size(), updateTasks.size());
        } catch (Exception e) {
            assertEquals(expectedError.getClass(), e.getClass());
        }
    }
}