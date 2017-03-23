package org.nines;

import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.logging.Logger;

public class LoggingIT {

    protected Logger logger;

    @BeforeClass
    public static void configureLogging() throws IOException {
        Logging.configure();
    }

    @Before
    public void logger() {
        logger = Logging.forClass(getClass());
    }


}
