package com.example.trace;

public class NTrace {
	static{
		System.loadLibrary("trace");
	}
	
	public native int dumpMem(int pid, long start, long offset, String filename);
}
