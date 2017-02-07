#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <sys/ptrace.h>
#include <stdlib.h>
#include <errno.h>
#include <asm/page.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#define TAG "jni_trace"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR , TAG, __VA_ARGS__)

static const char *classPathName = "com/example/trace/NTrace";
jint JNI_OnLoad(JavaVM* vm, void* reserved);
void change_process_mem(int pid, long offset, long value);
void process_mem_snapshot(int pid, long offset, long size, char *memfilename);
int printf_help(int argc, char **argv);
int main_s(int argc, char **argv);
jint save_map(JNIEnv* env, jobject thiz, jint pid, jlong offset, jlong size, jstring filename);

static JNINativeMethod methods[] = {
		{ "dumpMem", "(IJJLjava/lang/String;)I", (void*) save_map },
};

void change_process_mem(int pid, long offset, long value){
	ptrace(PTRACE_ATTACH, pid, NULL, NULL);
	waitpid(pid, NULL, 0);
	ptrace(PTRACE_POKEDATA, pid, (void*)offset, (void*)value);
	ptrace(PTRACE_DETACH, pid, NULL, NULL);
	waitpid(pid, NULL, 0);
}

void process_mem_snapshot(int pid, long offset, long size, char *memfilename){
	long i = 0;
	FILE *out;
	if(ptrace(PTRACE_ATTACH, pid, NULL, NULL) == 0){
		LOGD("PTRACE_ATTACH success");
	}else{
		LOGD("PTRACE_ATTACH error");
	}
	waitpid(pid, NULL, 0);
	out = fopen(memfilename, "wb+");
	for(i=offset; i<offset+size; i+=4){
		long r = ptrace(PTRACE_PEEKDATA, pid, (void*)i, NULL);
		fwrite((char*)&r, 4, 1, out);
	}
	fclose(out);
	LOGD("write finish");
	if(ptrace(PTRACE_DETACH, pid, NULL, NULL) == 0){
		LOGD("PTRACE_DETACH success");
	}else{
		LOGD("PTRACE_DETACH error");
	}
	waitpid(pid, NULL, 0);
}

int printf_help(int argc, char **argv){
	if((argc < 5) || (argv[1][0] == 's' && argv[1][1] == 0 && argc < 6)){
		printf("\r\n\r\n"
				"Usage:\t\t\t<sm> pid offset size <FILE>\r\n\r\n"
				"\t\t\t s 1000 10000 1222 \"/sdcard/temp\"\r\n"
				"\t\t\t m 1000 20000 1234\r\n");
		return 0;
	}
	return 1;
}

jint save_map(JNIEnv* env, jobject thiz, jint jpid, jlong joffset, jlong jsize, jstring filename){
	int pid = jpid;
	long offset = joffset;
	long size = jsize;
	char *memfilename = (char*)(*env)->GetStringUTFChars(env, filename, 0);
	LOGD("[%s] pid:%d offset:%ld size:%ld file:%s\n", __FUNCTION__, pid, offset, size, memfilename);
	process_mem_snapshot(pid, offset, size, memfilename);
	return (jint)1;
}

int main_s(int argc, char **argv){
	int pid;
	long offset;
	long size;
	char *memfilename;

	pid = atoi(argv[2]);
	offset = atoi(argv[3]);
	size = atoi(argv[4]);
	memfilename = argv[5];
	printf("[%s] pid:%d offset:%ld size:%ld file:%s\n", __FUNCTION__, pid, offset, size, memfilename);
	process_mem_snapshot(pid, offset, size, memfilename);
	return 0;
}

int main_m(int argc, char **argv){
	int pid;
	long offset;
	long value;

	pid = atoi(argv[2]);
	offset = atoi(argv[3]);
	value = atoi(argv[4]);
	change_process_mem(pid, offset, value);
	return 0;
}

int main(int argc, char **argv){
	if(printf_help(argc, argv)){
		if(argv[1][0] == 's'){
			main_s(argc, argv);
		}else if(argv[1][0] == 'm'){
			main_m(argc, argv);
		}
	}

	return 0;
}


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env = NULL;
	jclass clazz;
	//获取JNI环境对象
	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		LOGE("ERROR: GetEnv failed\n");
		return JNI_ERR;
	}
	//注册本地方法.Load 目标类
	clazz = (*env)->FindClass(env, classPathName);
	if (clazz == NULL) {
		LOGE("Native registration unable to find class '%s'", classPathName);
		return JNI_ERR;
	}
	//注册本地native方法
	if ((*env)->RegisterNatives(env, clazz, methods, NELEM(methods)) < 0) {
		LOGE("ERROR: MediaPlayer native registration failed\n");
		return JNI_ERR;
	}

	/* success -- return valid version number */
	return JNI_VERSION_1_4;
}
