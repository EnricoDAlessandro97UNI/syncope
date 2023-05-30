package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.junit.After;
import org.junit.Before;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;


/**
 * Support class for ExecuteTaskTest
 * 
 * @author Enrico D'Alessandro
 */
public abstract class PriorityPropagationTaskExecutorTest {

    protected MockedStatic<ApplicationContextProvider> context;
    protected MockedStatic<SecurityContextHolder> holder;

    public PriorityPropagationTaskExecutorTest() {
        /* Mock securityContext and auth */
        SecurityContext ctx = Mockito.mock(SecurityContext.class);
        List<GrantedAuthority> authorityList = new ArrayList<>();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROOT");
        authorityList.add(authority);
        Authentication auth = new UsernamePasswordAuthenticationToken("principal", "credentials", authorityList);
        holder = Mockito.mockStatic(SecurityContextHolder.class);
        holder.when(SecurityContextHolder::getContext).thenReturn(ctx);
        when(ctx.getAuthentication()).thenReturn(auth);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        holder.close();
        context.close();
    }
}
