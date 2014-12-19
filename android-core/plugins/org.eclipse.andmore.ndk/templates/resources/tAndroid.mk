LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := %{libraryName}
LOCAL_SRC_FILES := %{libraryName}.cpp

include $(BUILD_SHARED_LIBRARY)
