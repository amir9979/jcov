package com.sun.tdk.listener;

import com.sun.tdk.jcov.util.Utils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import java.net.Socket;
import java.io.PrintStream;
import java.io.InputStream;
import com.sun.tdk.jcov.GrabberManager;

public class JUnitExecutionListener extends RunListener {

    public void testRunStarted(Description description) throws Exception {
        System.out.println("Number of tests to execute: " + description.testCount());
    }

    public void testRunFinished(Result result) throws Exception {
        System.out.println("Number of tests executed: " + result.getRunCount());
    }

    public void testStarted(Description description) throws Exception {
        System.out.println("Starting: " + description.getMethodName());
    }

    public void testFinished(Description description) throws Exception {
        System.out.println("Finished: " + description.getMethodName());
        Socket sock = new Socket("localhost", 3337);
        try {
            PrintStream ps = new PrintStream(sock.getOutputStream(), false, "UTF-8");
            ps.println("save");
            ps.flush();
            InputStream is = sock.getInputStream();
            byte[] buff = new byte[1024];
            is.read(buff);
        } catch (Exception e) {}
        finally {
            sock.close();
        }
        GrabberManager.saveCommand();
    }

    public void testFailure(Failure failure) throws Exception {
        System.out.println("Failed: " + failure.getDescription().getMethodName());
    }

    public void testAssumptionFailure(Failure failure) {
        System.out.println("Failed: " + failure.getDescription().getMethodName());
    }

    public void testIgnored(Description description) throws Exception {
        System.out.println("Ignored: " + description.getMethodName());
    }
}
