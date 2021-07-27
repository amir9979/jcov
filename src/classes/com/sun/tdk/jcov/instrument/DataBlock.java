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
package com.sun.tdk.jcov.instrument;

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.data.ScaleOptions;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.util.Utils;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

/**
 * <p> DataBlock is an abstract class storing location & count info about some
 * instruction block in classfile. DataBlockTarget is an abstract class for all
 * branch target blocks: DataBlockTargetCase, DataBlockTargetDefault,
 * DataBlockTargetCond and DataBlockTargetGoto. </p> <p> DataBlocks are stored
 * in BasicBlock (or just as DataBlockMethEnter in DataMethodEntryOnly) in a
 * Map. DataBlockTargets are stored in a list in DataBranch. </p>
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public abstract class DataBlock extends LocationRef {

    protected int slot;
    protected int extraSlot;
    protected int merged_to;
    protected long count;
    protected long[][] hitInformation;
    protected boolean attached;
    protected Scale scale;

    /**
     * Creates a new instance of DataBlock
     */
    DataBlock(int rootId) {
        super(rootId);
        this.slot = Collect.newSlot();
        attached = true;
        hitInformation = new long[0][0];
        extraSlot = -1;
        merged_to = -1;
    }

    DataBlock(int rootId, int slot, boolean attached, long count) {
        super(rootId);
        this.slot = slot;
        this.attached = attached;
        this.count = count;
        hitInformation = new long[0][0];
        extraSlot = -1;
        merged_to = -1;

        if (attached) {
            setCollectCount(count);
        }
    }

    /**
     * @return ID of this block
     */
    public int getId() {
        return slot;
    }

    /**
     * Set ID of this block. Block ID is used to collect coverage (each
     * instrumented block hits it's own slot)
     *
     * @param slot
     */
    public void setId(int slot) {
        this.slot = slot;
    }

    void attach() {
        setCollectCount(count);
        attached = true;
    }

    void detach() {
        update();
        attached = false;
    }

    void update() {
        count = collectCount();
    }

    /**
     * @return true if this block was hit at least once
     */
    public boolean wasHit() {
        if (attached) {
            return wasCollectHit();
        } else {
            return count != 0;
        }
    }

    /**
     * @return the number of times this block was hit
     */
    public long getCount() {
        if (attached) {
            return collectCount();
        } else {
            return count;
        }
    }
    public long[][] getHitInformation() {
        if (attached) {
            return collectSlotInformation();
        } else {
            return hitInformation;
        }
    }


    public String getHitInformationString() {
        StringBuilder result = new StringBuilder();
        result.append('[');
        int j = 0;
        while(j < hitInformation.length) {
            if (j > 0) {
                result.append(',');
            }
            result.append(String.format("(%d,%d,%d,%d,%d,%d)",
                    hitInformation[j][0], hitInformation[j][1], hitInformation[j][2], hitInformation[j][3], hitInformation[j][4], hitInformation[j][5]));
            j++;
        }
        result.append(']');
        return result.toString();
    }

    public String getExtraSlot() {
        return "" + extraSlot;
    }

    /**
     * Set the count of times this block was hit
     *
     * @param count
     */
    public void setCount(long count) {
        if (attached) {
            setCollectCount(count);
        }

        this.count = count;
    }

    public void setHitInformation(long[][] hitInformation) {
        this.hitInformation = hitInformation;
    }

    public void setExtraSlot(int slot){
        extraSlot = slot;
    }

    /**
     * Default implementations
     *
     */
    protected boolean wasCollectHit() {
        return Collect.wasHit(slot);
    }

    protected long collectCount() {
        return Collect.countFor(slot);
    }

    protected long[][] collectSlotInformation() {
        return Collect.slotInformationFor(slot);
    }

    protected void setCollectCount(long count) {
        Collect.setCountFor(slot, count);
    }

    boolean isNested() {
        return false;
    }

    /**
     * Does the previous block fall into this one? Override for blocks that are
     * fallen into.
     */
    boolean isFallenInto() {
        return false;
    }

    /**
     * XML Generation
     */
    @Override
    void xmlAttrs(XmlContext ctx) {
        super.xmlAttrs(ctx);
        ctx.attr(XmlNames.ID, getId());
        ctx.attr(XmlNames.COUNT, getCount());ctx.attr(XmlNames.HIT_INFORMATION, getHitInformationString());ctx.attr(XmlNames.EXTRA_SLOTS, getExtraSlot());

        printScale(ctx);
        assertCounter();
    }

    void printScale(XmlContext ctx) {
        DataRoot r = DataRoot.getInstance(rootId);
        if (scale != null) {
            ScaleOptions opts = r.getScaleOpts();
            StringBuffer sb = new StringBuffer(Utils.halfBytesRequiredFor(scale.size()));
            sb.setLength(sb.capacity());
            sb.setLength(scale.convertToChars(opts.scalesCompressed(), sb,
                    opts.getScaleCompressor()));
            ctx.attr(XmlNames.SCALE, sb);
        }
    }

    @Override
    void xmlBody(XmlContext ctx) {
//        if (cnt() > 0) {
//            ctx.indentPrintln("<count>" + cnt() + "</count>");
//        }
    }

    @Override
    void xmlGen(XmlContext ctx) {
        xmlGenBodiless(ctx);
    }

    /**
     * Not supposed to be used from outside
     *
     * @param s
     */
    public void readScale(String s) {
        if (s != null && s.length() > 0) {
            try {
                DataRoot r = DataRoot.getInstance(rootId);
                ScaleOptions opts = r.getScaleOpts();
                if (opts.needReadScales()) {
                    scale = new Scale(s.toCharArray(), s.length(),
                            opts.getScaleSize(), opts.getScaleCompressor(), opts.scalesCompressed());
                }
            } catch (FileFormatException ex) {
            }
        }
    }

    /**
     * Increase size of scales assigned to this block. If count of this block is
     * above zero - "1" is written to the end of scale
     *
     * @param newSize
     * @param add_before where to put zeroes
     */
    public void expandScales(int newSize, boolean add_before) {
        scale = Scale.expandScale(scale, newSize, add_before, getCount());
    }

    /**
     * Increase size of scales assigned to this block. If count is above zero -
     * "1" is written to the end of scale
     *
     * @param newSize
     * @param add_before where to put zeroes
     * @param count
     */
    public void expandScales(int newSize, boolean add_before, long count) {
        scale = Scale.expandScale(scale, newSize, add_before, count);
    }

    void mergeScale(DataBlock other) {
        boolean readScale = DataRoot.getInstance(rootId).getScaleOpts().needReadScales();
        if (!readScale) {
            return;
        }

        scale = Scale.merge(scale, other.scale, getCount(), other.getCount());
    }

    /**
     * Not supposed to be used from outside
     *
     * @param new_size
     * @param pairs
     */
    public void illuminateDuplicatesInScales(int new_size, ArrayList pairs) {
        scale = Scale.illuminateDuplicates(scale, new_size, pairs);
    }

    /**
     * @return scale information of this block (null if scales were not
     * generated)
     * @see Scale
     */
    public Scale getScale() {
        return scale;
    }

    /**
     * Remove all scale information
     */
    public void cleanScale() {
        try {
            scale = new Scale(new char[0], 0, 0, null, false);
        } catch (FileFormatException ignore) {
        }
    }

    void assertCounter(){
//        long counter = 0;
//        for(int j=0;j< getHitInformation().length;j++){
//            counter += getHitInformation()[j][0];
//        }
//        if (counter != getCount()){
//            throw new RuntimeException("counter != getCount()");
//        }
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.writeLong(getCount());
        out.writeInt(slot);
        out.writeInt(extraSlot);
        hitInformation = getHitInformation();
        out.writeInt(hitInformation.length);
        for (int i = 0; i < hitInformation.length; i++) {
            out.writeInt(hitInformation[i].length);
            for (int j = 0; j < hitInformation[i].length; j++) {
                out.writeLong(hitInformation[i][j]);
            }
        }

        if (scale != null) {
            out.writeBoolean(true);
            scale.writeObject(out);
        } else {
            out.writeBoolean(false);
        }
        assertCounter();
    }

    DataBlock(int rootId, DataInput in) throws IOException {
        super(rootId, in);
        count = in.readLong();
        slot = in.readInt();
        extraSlot = in.readInt();
        int hitInformationLength = in.readInt();
        hitInformation = new long[hitInformationLength][];
        for (int i = 0; i < hitInformation.length; i++) {
            int hitInformation_i_length = in.readInt();
            hitInformation[i] = new long[hitInformation_i_length];
            for (int j = 0; j < hitInformation[i].length; j++) {
                hitInformation[i][j] = in.readLong();
            }
        }
        if (in.readBoolean()) {
            scale = new Scale(in);
        } else {
            scale = null;
        }
        assertCounter();
    }

    public void merge(DataBlock other){
        assertCounter();
        other.assertCounter();
        mergeScale(other);
        setCount(getCount() + other.getCount());
        other.setCount(0);
        setExtraSlot(other.slot);
//        other.setExtraSlot(-1);
//        other.setId(-1);
        other.merged_to = slot;
        if (other.getHitInformation().length == 0){
            return;
        }
        if (getHitInformation().length == 0){
            setHitInformation(other.getHitInformation());
            other.setHitInformation(new long[0][0]);
            return;
        }
        long[][] hitInformation = new long[getHitInformation().length + other.getHitInformation().length][];
        int i=0;
        for (; i< getHitInformation().length; i++){
            hitInformation[i] = getHitInformation()[i];
        }
        for (int j = 0; j< other.getHitInformation().length; j++){
            hitInformation[i+j] = other.getHitInformation()[j];
        }
        setHitInformation(hitInformation);
        other.setHitInformation(new long[0][0]);
        assertCounter();
        other.assertCounter();
    }
}