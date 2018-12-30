package com.sun.tdk.listener;

import com.sun.tdk.jcov.util.Utils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import java.net.Socket;
import com.sun.tdk.jcov.Agent.CommandThread;
import com.sun.tdk.jcov.GrabberManager;

public class JUnitExecutionListener extends RunListener {

    public void testRunStarted(Description description) {
    }

    public void testRunFinished(Result result) {
    }

    public void testStarted(Description description) {
        try {
            String testName = description.getClassName() + "." + description.getMethodName();
            GrabberManager.startNewTestCommand(testName);
        } catch (Exception e) {e.printStackTrace(); }
    }

    public void testFinished(Description description) {
        saveAll();
    }

    public void testFailure(Failure failure) {
    }

    public void testAssumptionFailure(Failure failure) {
    }

    public void testIgnored(Description description) {
    }

    private void saveAll() {
        try {
            CommandThread.saveCollectData();
            //GrabberManager.saveAgentDataCommand();
            GrabberManager.saveCommand();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
