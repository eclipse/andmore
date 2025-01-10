package org.eclipse.andmore.android;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.Client;
import com.android.ddmlib.FileListingService;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.ScreenRecorderOptions;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.log.LogReceiver;
import com.android.sdklib.AndroidVersion;

public class NullDevice implements IDevice {
	class NullFuture<T> implements Future<T> {
		T value;
		NullFuture(T value) {
			this.value = value;
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return value;
		}

		@Override
		public T get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
			
			return value;
		}
	}

	private static final String BLANK = "";
	private static final String DEVICE_NAME = "null device";
	
	@Override
	public String getName() {
		
		return DEVICE_NAME;
	}

	@Override
	public void executeShellCommand(String command, IShellOutputReceiver receiver, long maxTimeToOutputResponse,
			TimeUnit maxTimeUnits)
			throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public Future<String> getSystemProperty(String name) {
		return new NullFuture<String>(BLANK);
	}

	@Override
	public String getSerialNumber() {
		return BLANK;
	}

	@Override
	public String getAvdName() {
		return DEVICE_NAME;
	}

	@Override
	public DeviceState getState() {
		return DeviceState.OFFLINE;
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.emptyMap();
	}

	@Override
	public int getPropertyCount() {
		return 0;
	}

	@Override
	public String getProperty(String name) {
		return BLANK;
	}

	@Override
	public boolean arePropertiesSet() {
		return false;
	}

	@Override
	public String getPropertySync(String name)
			throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
		return BLANK;
	}

	@Override
	public String getPropertyCacheOrSync(String name)
			throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
		return BLANK;
	}

	@Override
	public boolean supportsFeature(Feature feature) {
		return false;
	}

	@Override
	public boolean supportsFeature(HardwareFeature feature) {
		return false;
	}

	@Override
	public String getMountPoint(String name) {
		
		return BLANK;
	}

	@Override
	public boolean isOnline() {
		return false;
	}

	@Override
	public boolean isEmulator() {
		return false;
	}

	@Override
	public boolean isOffline() {
		return true;
	}

	@Override
	public boolean isBootLoader() {
		return false;
	}

	@Override
	public boolean hasClients() {
		return false;
	}

	@Override
	public Client[] getClients() {
		
		return new Client[0];
	}

	@Override
	public Client getClient(String applicationName) {
		return null;
	}

	@Override
	public SyncService getSyncService() throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public FileListingService getFileListingService() {
		return null;
	}

	@Override
	public RawImage getScreenshot() throws TimeoutException, AdbCommandRejectedException, IOException {
		return new RawImage();
	}

	@Override
	public RawImage getScreenshot(long timeout, TimeUnit unit)
			throws TimeoutException, AdbCommandRejectedException, IOException {
		return new RawImage();
	}

	@Override
	public void startScreenRecorder(String remoteFilePath, ScreenRecorderOptions options, IShellOutputReceiver receiver)
			throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void executeShellCommand(String command, IShellOutputReceiver receiver, int maxTimeToOutputResponse)
			throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void executeShellCommand(String command, IShellOutputReceiver receiver)
			throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void runEventLogService(LogReceiver receiver)
			throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void runLogService(String logname, LogReceiver receiver)
			throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void createForward(int localPort, int remotePort)
			throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void createForward(int localPort, String remoteSocketName, DeviceUnixSocketNamespace namespace)
			throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void removeForward(int localPort, int remotePort)
			throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void removeForward(int localPort, String remoteSocketName, DeviceUnixSocketNamespace namespace)
			throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public String getClientName(int pid) {
		return DEVICE_NAME;
	}

	@Override
	public void pushFile(String local, String remote)
			throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void pullFile(String remote, String local)
			throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void installPackage(String packageFilePath, boolean reinstall, String... extraArgs) throws InstallException {
		throw new InstallException(DEVICE_NAME);
	}

	@Override
	public void installPackages(List<File> apks, boolean reinstall, List<String> installOptions, long timeout,
			TimeUnit timeoutUnit) throws InstallException {
		throw new InstallException(DEVICE_NAME);
	}

	@Override
	public String syncPackageToDevice(String localFilePath)
			throws TimeoutException, AdbCommandRejectedException, IOException, SyncException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public void installRemotePackage(String remoteFilePath, boolean reinstall, String... extraArgs)
			throws InstallException {
		throw new InstallException(DEVICE_NAME);
	}

	@Override
	public void removeRemotePackage(String remoteFilePath) throws InstallException {
		throw new InstallException(DEVICE_NAME);
	}

	@Override
	public String uninstallPackage(String packageName) throws InstallException {
		throw new InstallException(DEVICE_NAME);
	}

	@Override
	public void reboot(String into) throws TimeoutException, AdbCommandRejectedException, IOException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public boolean root()
			throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public boolean isRoot()
			throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public Integer getBatteryLevel()
			throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public Integer getBatteryLevel(long freshnessMs)
			throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
		throw new IOException(DEVICE_NAME);
	}

	@Override
	public Future<Integer> getBattery() {
		return new NullFuture<Integer>(0);
	}

	@Override
	public Future<Integer> getBattery(long freshnessTime, TimeUnit timeUnit) {
		return new NullFuture<Integer>(0);
	}

	@Override
	public List<String> getAbis() {
		return Collections.emptyList();
	}

	@Override
	public int getDensity() {
		return 0;
	}

	@Override
	public String getLanguage() {
		return BLANK;
	}

	@Override
	public String getRegion() {
		return BLANK;
	}

	@Override
	public AndroidVersion getVersion() {
		return AndroidVersion.DEFAULT;
	}

}
