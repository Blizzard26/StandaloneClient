package org.stt.persistence.h2;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class H2ItemReaderTest {

	private H2ConnectionProvider connectionProvider;
	private H2ItemReader sut;
	
	@Mock
	H2Configuration configuration;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:H2ItemReaderTest");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		
		sut = new H2ItemReader(connectionProvider);
	}
	
	@After
	public void tearDown() throws IOException {
		sut.close();
	}
	
	@Test
	public void test() {
		fail("Not yet implemented");
	}

}
