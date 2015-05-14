/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ddmuilib.heap;

import com.android.ddmlib.NativeAllocationInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Models a heap snapshot that is the difference between two snapshots.
 */
public class NativeHeapDiffSnapshot extends NativeHeapSnapshot {
    private long mCommonAllocationsTotalMemory;

    public NativeHeapDiffSnapshot(NativeHeapSnapshot newSnapshot, NativeHeapSnapshot oldSnapshot) {
        // The diff snapshots behaves like a snapshot that only contains the new allocations
        // not present in the old snapshot
        super(getNewAllocations(newSnapshot, oldSnapshot));

        Set<NativeAllocationInfo> commonAllocations =
                new HashSet<NativeAllocationInfo>(oldSnapshot.getAllocations());
        commonAllocations.retainAll(newSnapshot.getAllocations());

        // Memory common between the old and new snapshots
        mCommonAllocationsTotalMemory = getTotalMemory(commonAllocations);
    }

    private static List<NativeAllocationInfo> getNewAllocations(NativeHeapSnapshot newSnapshot,
            NativeHeapSnapshot oldSnapshot) {
        Set<NativeAllocationInfo> allocations =
                new HashSet<NativeAllocationInfo>(newSnapshot.getAllocations());
        allocations.removeAll(oldSnapshot.getAllocations());
        return new ArrayList<NativeAllocationInfo>(allocations);
    }

    @Override
    public String getFormattedMemorySize() {
        // for a diff snapshot, we report the following string for display:
        //       xxx bytes new allocation + yyy bytes retained from previous allocation
        //          = zzz bytes total

        long newAllocations = getTotalSize();
        return String.format("%s bytes new + %s bytes retained = %s bytes total",
                formatMemorySize(newAllocations),
                formatMemorySize(mCommonAllocationsTotalMemory),
                formatMemorySize(newAllocations + mCommonAllocationsTotalMemory));
    }
}
