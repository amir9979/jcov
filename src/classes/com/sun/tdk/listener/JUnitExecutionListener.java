package com.sun.tdk.listener;

import com.sun.tdk.jcov.util.Utils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import java.net.Socket;
import com.sun.tdk.jcov.GrabberManager;

public class JUnitExecutionListener extends RunListener {

    public void testRunStarted(Description description) throws Exception {
        System.out.println("Number of tests to execute: " + description.testCount());
    }

    public void testRunFinished(Result result) throws Exception {
        System.out.println("Number of tests executed: " + result.getRunCount());
    }

    public void testStarted(Description description) throws Exception {
        String testName = description.getClassName() + "."+ description.getMethodName();
        System.out.println("Starting: " + testName);
        GrabberManager.saveCommand();
        GrabberManager.startNewTestCommand(testName);
    }

    public void testFinished(Description description) throws Exception {
        System.out.println("Finished: " + description.getClassName() + "."+ description.getMethodName());
        try {
        GrabberManager.saveAgentDataCommand();
        GrabberManager.saveCommand();
    } catch (Exception e) { e.printStackTrace();}
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
