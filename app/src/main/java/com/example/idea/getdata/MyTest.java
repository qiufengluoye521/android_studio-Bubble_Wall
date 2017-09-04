package com.example.idea.getdata;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

public class MyTest extends AndroidTestCase {

	private final String TAG = "MyTest";

	public MyTest() {
		// TODO Auto-generated constructor stub
	}

	public void saveFile() {
		Context context = getContext();
		Fileservice fileservice = new Fileservice(context);
		boolean flag = fileservice.saveContentTosdCard("hello.txt",
				"hello android");
		Log.i("TAG", "---------->>" + flag);

	}

	public void readFile() {
		Context context = getContext();
		Fileservice fileservice = new Fileservice(context);
		String outString = fileservice.getInputstream("hello.txt");
		Log.i("TAG", "---------->>" + outString);
	}

}
