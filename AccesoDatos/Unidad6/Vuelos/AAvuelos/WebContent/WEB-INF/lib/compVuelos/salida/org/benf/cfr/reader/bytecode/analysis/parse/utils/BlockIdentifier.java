/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.Predicate;

public class BlockIdentifier
implements Comparable<BlockIdentifier> {
    private final int index;
    private BlockType blockType;
    private int knownForeignReferences = 0;

    public BlockIdentifier(int index, BlockType blockType) {
        this.index = index;
        this.blockType = blockType;
    }

    public BlockType getBlockType() {
        return this.blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }

    public String getName() {
        return "block" + this.index;
    }

    public int getIndex() {
        return this.index;
    }

    public void addForeignRef() {
        ++this.knownForeignReferences;
    }

    public void releaseForeignRef() {
        --this.knownForeignReferences;
    }

    public boolean hasForeignReferences() {
        return this.knownForeignReferences > 0;
    }

    public String toString() {
        return "" + this.index + "[" + (Object)((Object)this.blockType) + "]";
    }

    public static boolean blockIsOneOf(BlockIdentifier needle, Set<BlockIdentifier> haystack) {
        return haystack.contains(needle);
    }

    public static BlockIdentifier getOutermostContainedIn(Set<BlockIdentifier> endingBlocks, final Set<BlockIdentifier> blocksInAtThisPoint) {
        List<BlockIdentifier> containedIn = Functional.filter(ListFactory.newList(endingBlocks), new Predicate<BlockIdentifier>(){

            @Override
            public boolean test(BlockIdentifier in) {
                return blocksInAtThisPoint.contains(in);
            }
        });
        if (containedIn.isEmpty()) {
            return null;
        }
        Collections.sort(containedIn);
        return containedIn.get(0);
    }

    public static BlockIdentifier getInnermostBreakable(List<BlockIdentifier> blocks) {
        BlockIdentifier res = null;
        for (BlockIdentifier block : blocks) {
            if (!block.blockType.isBreakable()) continue;
            res = block;
        }
        return res;
    }

    public static BlockIdentifier getOutermostEnding(List<BlockIdentifier> blocks, Set<BlockIdentifier> blocksEnding) {
        for (BlockIdentifier blockIdentifier : blocks) {
            if (!blocksEnding.contains(blockIdentifier)) continue;
            return blockIdentifier;
        }
        return null;
    }

    @Override
    public int compareTo(BlockIdentifier blockIdentifier) {
        return this.index - blockIdentifier.index;
    }
}

