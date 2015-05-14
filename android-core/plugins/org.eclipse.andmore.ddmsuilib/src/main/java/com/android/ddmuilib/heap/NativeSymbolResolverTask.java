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
import com.android.ddmlib.NativeLibraryMapInfo;
import com.android.ddmlib.NativeStackCallInfo;
import com.android.ddmuilib.DdmUiPreferences;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A symbol resolver task that can resolve a set of addresses to their corresponding
 * source method name + file name:line number.
 *
 * It first identifies the library that contains the address, and then runs addr2line on
 * the library to get the symbol name + source location.
 */
public class NativeSymbolResolverTask implements IRunnableWithProgress {
    private static final String ADDR2LINE;
    private static final String DEFAULT_SYMBOLS_FOLDER;

    static {
        String addr2lineEnv = System.getenv("ANDROID_ADDR2LINE");
        ADDR2LINE = addr2lineEnv != null ? addr2lineEnv : DdmUiPreferences.getAddr2Line();

        String symbols = System.getenv("ANDROID_SYMBOLS");
        DEFAULT_SYMBOLS_FOLDER = symbols != null ? symbols : DdmUiPreferences.getSymbolDirectory();
    }

    private List<NativeAllocationInfo> mCallSites;
    private List<NativeLibraryMapInfo> mMappedLibraries;
    private List<String> mSymbolSearchFolders;

    /** All unresolved addresses from all the callsites. */
    private SortedSet<Long> mUnresolvedAddresses;

    /** Set of all addresses that could were not resolved at the end of the resolution process. */
    private Set<Long> mUnresolvableAddresses;

    /** Map of library -> [unresolved addresses mapping to this library]. */
    private Map<NativeLibraryMapInfo, Set<Long>> mUnresolvedAddressesPerLibrary;

    /** Addresses that could not be mapped to a library, should be mostly empty. */
    private Set<Long> mUnmappedAddresses;

    /** Cache of the resolution for every unresolved address. */
    private Map<Long, NativeStackCallInfo> mAddressResolution;

    /** List of libraries that were not located on disk. */
    private Set<String> mNotFoundLibraries;
    private String mAddr2LineErrorMessage = null;

    public NativeSymbolResolverTask(List<NativeAllocationInfo> callSites,
                List<NativeLibraryMapInfo> mappedLibraries,
                String symbolSearchPath) {
        mCallSites = callSites;
        mMappedLibraries = mappedLibraries;
        mSymbolSearchFolders = new ArrayList<String>();
        mSymbolSearchFolders.add(DEFAULT_SYMBOLS_FOLDER);
        mSymbolSearchFolders.addAll(Arrays.asList(symbolSearchPath.split(":")));

        mUnresolvedAddresses = new TreeSet<Long>();
        mUnresolvableAddresses = new HashSet<Long>();
        mUnresolvedAddressesPerLibrary = new HashMap<NativeLibraryMapInfo, Set<Long>>();
        mUnmappedAddresses = new HashSet<Long>();
        mAddressResolution = new HashMap<Long, NativeStackCallInfo>();
        mNotFoundLibraries = new HashSet<String>();
    }

    @Override
    public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Resolving symbols", IProgressMonitor.UNKNOWN);

        collectAllUnresolvedAddresses();
        checkCancellation(monitor);

        mapUnresolvedAddressesToLibrary();
        checkCancellation(monitor);

        resolveLibraryAddresses(monitor);
        checkCancellation(monitor);

        resolveCallSites(mCallSites);

        monitor.done();
    }

    private void collectAllUnresolvedAddresses() {
        for (NativeAllocationInfo callSite : mCallSites) {
            mUnresolvedAddresses.addAll(callSite.getStackCallAddresses());
        }
    }

    private void mapUnresolvedAddressesToLibrary() {
        Set<Long> mappedAddresses = new HashSet<Long>();

        for (NativeLibraryMapInfo lib : mMappedLibraries) {
            SortedSet<Long> addressesInLibrary = mUnresolvedAddresses.subSet(lib.getStartAddress(),
                    lib.getEndAddress() + 1);
            if (addressesInLibrary.size() > 0) {
                mUnresolvedAddressesPerLibrary.put(lib, addressesInLibrary);
                mappedAddresses.addAll(addressesInLibrary);
            }
        }

        // unmapped addresses = unresolved addresses - mapped addresses
        mUnmappedAddresses.addAll(mUnresolvedAddresses);
        mUnmappedAddresses.removeAll(mappedAddresses);
    }

    private void resolveLibraryAddresses(IProgressMonitor monitor) throws InterruptedException {
        for (NativeLibraryMapInfo lib : mUnresolvedAddressesPerLibrary.keySet()) {
            String libPath = getLibraryLocation(lib);
            Set<Long> addressesToResolve = mUnresolvedAddressesPerLibrary.get(lib);

            if (libPath == null) {
                mNotFoundLibraries.add(lib.getLibraryName());
                markAddressesNotResolvable(addressesToResolve, lib);
            } else {
                monitor.subTask(String.format("Resolving addresses mapped to %s.", libPath));
                resolveAddresses(lib, libPath, addressesToResolve);
            }

            checkCancellation(monitor);
        }
    }

    private void resolveAddresses(NativeLibraryMapInfo lib, String libPath,
            Set<Long> addressesToResolve) {
        Process addr2line = null;
        try {
            addr2line = new ProcessBuilder(ADDR2LINE,
                    "-C",   // demangle
                    "-f",   // display function names in addition to file:number
                    "-e", libPath).start();
        } catch (IOException e) {
            // Since the library path is known to be valid, the only reason for an exception
            // is that addr2line was not found. We just save the message in this case.
            mAddr2LineErrorMessage = e.getMessage();
            markAddressesNotResolvable(addressesToResolve, lib);
            return;
        }

        BufferedReader resultReader = new BufferedReader(new InputStreamReader(
                                                                    addr2line.getInputStream()));
        BufferedWriter addressWriter = new BufferedWriter(new OutputStreamWriter(
                                                                    addr2line.getOutputStream()));

        long libStartAddress = isExecutable(lib) ? 0 : lib.getStartAddress();
        try {
            for (Long addr : addressesToResolve) {
                long offset = addr.longValue() - libStartAddress;
                addressWriter.write(Long.toHexString(offset));
                addressWriter.newLine();
                addressWriter.flush();
                String method = resultReader.readLine();
                String sourceFile = resultReader.readLine();

                mAddressResolution.put(addr,
                        new NativeStackCallInfo(addr.longValue(),
                                lib.getLibraryName(),
                                method,
                                sourceFile));
            }
        } catch (IOException e) {
            // if there is any error, then mark the addresses not already resolved
            // as unresolvable.
            for (Long addr : addressesToResolve) {
                if (mAddressResolution.get(addr) == null) {
                    markAddressNotResolvable(lib, addr);
                }
            }
        }

        try {
            resultReader.close();
            addressWriter.close();
        } catch (IOException e) {
            // we can ignore these exceptions
        }

        addr2line.destroy();
    }

    private boolean isExecutable(NativeLibraryMapInfo object) {
        // TODO: Use a tool like readelf or nm to determine whether this object is a library
        //       or an executable.
        // For now, we'll just assume that any object present in the bin folder is an executable.
        String devicePath = object.getLibraryName();
        return devicePath.contains("/bin/");
    }

    private void markAddressesNotResolvable(Set<Long> addressesToResolve,
                                NativeLibraryMapInfo lib) {
        for (Long addr : addressesToResolve) {
            markAddressNotResolvable(lib, addr);
        }
    }

    private void markAddressNotResolvable(NativeLibraryMapInfo lib, Long addr) {
        mAddressResolution.put(addr,
                new NativeStackCallInfo(addr.longValue(),
                        lib.getLibraryName(),
                        Long.toHexString(addr),
                        ""));
        mUnresolvableAddresses.add(addr);
    }

    /**
     * Locate on local disk the debug library w/ symbols corresponding to the
     * library on the device. It searches for this library in the symbol path.
     * @return absolute path if found, null otherwise
     */
    private String getLibraryLocation(NativeLibraryMapInfo lib) {
        String pathOnDevice = lib.getLibraryName();
        String libName = new File(pathOnDevice).getName();

        for (String p : mSymbolSearchFolders) {
            // try appending the full path on device
            String fullPath = p + File.separator + pathOnDevice;
            if (new File(fullPath).exists()) {
                return fullPath;
            }

            // try appending basename(library)
            fullPath = p + File.separator + libName;
            if (new File(fullPath).exists()) {
                return fullPath;
            }
        }

        return null;
    }

    private void resolveCallSites(List<NativeAllocationInfo> callSites) {
        for (NativeAllocationInfo callSite : callSites) {
            List<NativeStackCallInfo> stackInfo = new ArrayList<NativeStackCallInfo>();

            for (Long addr : callSite.getStackCallAddresses()) {
                NativeStackCallInfo info = mAddressResolution.get(addr);

                if (info != null) {
                    stackInfo.add(info);
                }
            }

            callSite.setResolvedStackCall(stackInfo);
        }
    }

    private void checkCancellation(IProgressMonitor monitor) throws InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }

    public String getAddr2LineErrorMessage() {
        return mAddr2LineErrorMessage;
    }

    public Set<Long> getUnmappedAddresses() {
        return mUnmappedAddresses;
    }

    public Set<Long> getUnresolvableAddresses() {
        return mUnresolvableAddresses;
    }

    public Set<String> getNotFoundLibraries() {
        return mNotFoundLibraries;
    }
}
