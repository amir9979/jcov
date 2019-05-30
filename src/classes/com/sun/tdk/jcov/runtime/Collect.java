/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.tdk.jcov.runtime;

/**
 * <p> Strores all runtime coverage information. Coverage information is stored
 * in array of longs (counts[MAX_SLOTS]). </p> <p> Here should be no imports!
 * Collect should be usable in the earliest VM lifecycle - eg in String class
 * loading. </p> <p> slots count can be optimized at instrumentation time
 * by generation Collect class exactly for instrumented data. For agent it's
 * possible to use increasing array (see newSlot()). </p>
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */

class List {
    private long lst[];
    private int last;
    public List() {
        lst = new long[10];
    }

    public void add(long elem) {
        if(last < lst.length)
            lst[last++] = elem;
        else {
            long newList[] = new long[lst.length*2];
            System.arraycopy(lst, 0, newList, 0, lst.length);
            lst = newList;
            lst[last++] = elem;
        }
    }

    public long get(int index) {
        if(index < lst.length)
            return lst[index];
        return -1;
    }

    public long[] getArray() {
        return lst;
    }
}

public class Collect {

    // coverage data
    public static final int MAX_SLOTS = 2000000;
    public static int SLOTS = MAX_SLOTS;
    private static final int MAX_SAVERS = 10;
    private static int nextSlot = 0;
    private static long counts[];
    private static long counts_[];
    private static List adjacencies[];
    private static List adjacencies_[];
    private static long lastHitted = -1;
    // -- coverage data
    // savers
    private static JCovSaver[] savers = new JCovSaver[MAX_SAVERS];
    private static int nextSaver = 0;
    private static Class extension = null;
    // This constant is replaced in ANT build script (see files se.replace.properties, me.replace.properties and so on)
    private final static String saverClassnameString = "/*@BUILD_MODIFIED_SAVER_STRING@*/";
    // -- savers
    // saving state
    public static boolean enabled = false;
    public static boolean saveEnabled = true;
    public static boolean saveAtShutdownEnabled = true;
    public static boolean isInitialized = false;
    public static boolean isInternal = false;
    // -- saving state

    /**
     * <p> Reserves a new slot for coverage item. </p>
     *
     * @return next slot number
     */
    public static int newSlot() {
        if (nextSlot >= counts.length) {
            long[] newCounts = new long[nextSlot * 2];
            System.arraycopy(counts, 0, newCounts, 0, counts.length);
            List[] newAdjacencies = new List[nextSlot * 2];
            System.arraycopy(adjacencies, 0, newAdjacencies, 0, adjacencies.length);
            counts_ = counts = newCounts;
            adjacencies_ = adjacencies = newAdjacencies;
//            throw new Error("Method slot count exceeded");
        }
        return nextSlot++;
    }

    /**
     * <p> Get current number of slots </p>
     *
     * @return current number of slots
     */
    public static int slotCount() {
        return nextSlot;
    }

    /**
     * <p> Increase coverage statistics on certain slot. </p> <p> Slot is an
     * array element which is dedicated to a certain code member (eg a block of
     * code). This array element stores number of times this member was 'hit' or
     * called. </p>
     *
     * @param slot
     */
    public static void hit(int slot) {
        counts[slot]++;
        adjacencies[slot].add(lastHitted);
        lastHitted = slot;
    }

    /**
     * <p> Set number of slots </p>
     *
     * @param i new number of slots
     */
    public static void setSlot(int i) {
        nextSlot = i;
    }

    /**
     * <p> Check whether the member was hit at least once </p>
     *
     * @param slot
     * @return
     */
    public static boolean wasHit(int slot) {
        return counts_[slot] != 0;
    }

    /**
     * <p> Get all coverage data in the array. </p> <p> The real numbers are
     * returned always in this method. The coverage data is copied in a
     * temporary array while it's being saved so that new coverage data coming
     * from different threads would not corrupt saving coverage data. This
     * method will return data being saved in case Collect.saveResults() was
     * called. </p>
     *
     * @return coverage data
     */
    public static long[] counts() {
        return counts_;
    }

    public static List[] adjacencies() {
        return adjacencies_;
    }

    /**
     * <p> Get coverage data on a certain member </p>
     *
     * @param slot member ID
     * @return coverage data
     */
    public static long countFor(int slot) {
        return counts_[slot];
    }

    public static long[] adjacenciesFor(int slot) {
        return adjacencies_[slot].getArray();
    }

    /**
     * <p> Set coverage data for a certain member </p>
     *
     * @param slot member ID
     * @param count new coverage data
     */
    public static void setCountFor(int slot, long count) {
        counts[slot] = count;
    }

    /**
     * <p> Create the storage for coverage data. Allocates
     * <code>SLOTS</code> array of longs. </p>
     *
     * @see #SLOTS
     */
    public static void enableCounts() {
        counts_ = counts = new long[SLOTS];
        adjacencies_ = adjacencies = new List[SLOTS];
    }

    /**
     * <p> Agent should not instrument classes if Collect is disable. </p>
     */
    public static void disable() {
        enabled = false;
    }

    /**
     * <p> Agent should not instrument classes if Collect is disable. </p>
     */
    public static void enable() {
        enabled = true;
    }

    /**
     * <p> Adds a saver to be called when saveResults is invoked </p>
     *
     * @param saver
     */
    public static synchronized void addSaver(JCovSaver saver) {
        savers[nextSaver++] = saver;
    }

    /**
     * <p> Sets a saver to be called when saveResults is invoked. All previously
     * added savers will be removed. </p>
     *
     * @param saver
     */
    public static synchronized void setSaver(JCovSaver saver) {
        for (int i = 0; i < nextSaver; ++i) {
            savers[i] = null;
        }
        nextSaver = 0;
        addSaver(saver);
    }

    /**
     * <p> Save all collected data with all savers installed in Collect. If
     * "jcov.saver" property is set savers names would be read from this
     * property. </p> <p> Coverage data array will be hidden while saveResults
     * is working to be sure that other threads will not corrupt data that is
     * being saved. </p>
     */
    public static synchronized void saveResults() {
        if (!saveEnabled) {
            return;
        }
        // Disable hits. Can't use "enabled = false" as it will result in Agent malfunction
        counts = new long[counts.length]; // reset counts[] that are collecting hits - real hits will be available in counts_
        adjacencies = new List[adjacencies.length]; // reset counts[] that are collecting hits - real hits will be available in counts_
        lastHitted = -1;

        String s = PropertyFinder.findValue("saver", null);
        if (s != null) {
            String[] saver = new String[s.length()];
            int i = 0;
            while (s.length() > 0) {
                int k = s.indexOf(";");
                if (k == 0) {
                    s = s.substring(1);
                } else if (k > 0) {
                    String newS = s.substring(0, k);
                    if (newS.length() > 0) {
                        saver[i++] = newS;
                    }
                    s = s.substring(k);
                } else {
                    saver[i++] = s;
                    break;
                }
            }
            for (int j = 0; j < i; j++) {
                try {
                    instantiateSaver(saver[j]).saveResults();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } else {
            for (int i = 0; i < nextSaver; i++) {
                try {
                    savers[i].saveResults();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        counts_ = counts; // repoint counts_[] that are answering DataRoot about hits to newly created counts[]
        adjacencies_ = adjacencies; // repoint counts_[] that are answering DataRoot about hits to newly created counts[]
        // Enable hits. Can't use "enabled = false" as it will result in Agent malfunction
    }

    /**
     * <p> Loads satellite class if it's not loaded. </p>
     */
    public static void loadSaverExtension() {
        if (extension != null) {
            return;
        }

        String m = PropertyFinder.findValue("extension", null);
        if (m != null) {
            if (m.equals("javatest") || m.equals("jt") || m.equals("jtreg")) {
                m = "com.sun.tdk.jcov.runtime.NetworkSatelliteDecorator";
            }
            try {
                extension = Class.forName(m);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    /**
     * <p> Create Saver instance by name. The saver will be wrapped by Satellite
     * instance if any. </p>
     *
     * @param name Saver to create
     * @return Created Saver
     */
    public static JCovSaver instantiateSaver(String name) {
        try {
            return decorateSaver((JCovSaver) Class.forName(name).newInstance());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public static JCovSaver decorateSaver(JCovSaver saver) {
        if (extension != null) {
            try {
                SaverDecorator s = (SaverDecorator) extension.newInstance();
                s.init(saver);
                return s;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return saver;
    }

    /**
     * <p> Initialize JCov RT. This method is called in static initialization of
     * Collect class and in static initialization of every instrumented class
     * (&lt;clitin&gt; method) </p>
     */
    public static void init() {
        if (!isInitialized && !isInternal) {
            isInternal = true;
            if (PropertyFinder.isVMReady()) {
                loadSaverExtension();
                if (!saverClassnameString.startsWith("/*@")) {
                    addSaver(instantiateSaver(saverClassnameString));
                    PropertyFinder.addAutoShutdownSave();
                    isInitialized = true;
                } else {
                    isInitialized = true;
                }
            }
            isInternal = false;
        }
    }

    static {
        enableCounts();
        init();
    }
}
