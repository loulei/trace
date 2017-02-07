LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_LDLIBS += -llog
LOCAL_MODULE    := trace
LOCAL_SRC_FILES := trace.c

#include $(BUILD_SHARED_LIBRARY)
include $(BUILD_EXECUTABLE)
#include $(call all-makefiles-under, $(LOCAL_PATH))

#$(call import-module,android/cpufeatures)