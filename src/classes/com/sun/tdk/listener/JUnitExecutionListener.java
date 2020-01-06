package com.sun.tdk.listener;

import com.sun.tdk.jcov.constants.MiscConstants;
import com.sun.tdk.jcov.util.Utils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import com.sun.tdk.jcov.Agent.CommandThread;
import com.sun.tdk.jcov.GrabberManager;

class Helper extends TimerTask
{
    public void run()
    {
        CommandThread.saveCollectData();
        //GrabberManager.saveAgentDataCommand();
        int port = Integer.parseInt(System.getenv("JcovGrabberCommandPort"));
        GrabberManager.saveCommand(port);
        java.lang.System.exit(1);
        Runtime.getRuntime().halt(1);
    }
}


public class JUnitExecutionListener extends RunListener {
    final Timer timer = new Timer();
    Helper helper;

    public JUnitExecutionListener(){
        helper = new Helper();
    }

    public void testRunStarted(Description description) {
    }

    public void testRunFinished(Result result) {
    }

    public void testStarted(Description description) {
        try {
            helper.cancel();
            helper = new Helper();
            timer.schedule(helper,600000);
            String testName = description.getClassName() + "." + description.getMethodName();
            int port = Integer.parseInt(System.getenv("JcovGrabberCommandPort"));
            GrabberManager.startNewTestCommand(testName, port);
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
            helper.cancel();
            CommandThread.saveCollectData();
            //GrabberManager.saveAgentDataCommand();
            int port = Integer.parseInt(System.getenv("JcovGrabberCommandPort"));
            GrabberManager.saveCommand(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
