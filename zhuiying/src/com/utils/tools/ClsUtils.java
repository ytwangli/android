package com.utils.tools;



import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class ClsUtils {

	/**
	 * 与设备配对 参考源码：platform/packages/apps/Settings.git
	 * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public boolean createBond(Class btClass, BluetoothDevice btDevice)
			throws Exception {
		Method createBondMethod = btClass.getMethod("createBond");
		Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}

	/**
	 * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
	 * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static public boolean removeBond(Class btClass, BluetoothDevice btDevice)
			throws Exception {
		Method removeBondMethod = btClass.getMethod("removeBond");
		Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}

	
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static public String getUUId(Class btClass,BluetoothDevice btDevice) throws Exception{
		
		
		
		
		
		Method getUUIDMethod = btClass.getMethod("getUuids");
		
		String uuid=(String) getUUIDMethod.invoke(btDevice);
		
		
		
		
		return uuid;
		
		
		
		
		
		
		
		
		
		
	}
	
	
	
	
	
	
	
	
	/**
	 * 
	 * @param clsShow
	 */
	@SuppressWarnings("rawtypes")
	static public void printAllInform(Class clsShow) {
		try {
			// 取得所有方法
			Method[] hideMethod = clsShow.getMethods();
			int i = 0;
			for (; i < hideMethod.length; i++) {
				Log.e("method name", hideMethod[i].getName());
			}
			// 取得所有常量
			Field[] allFields = clsShow.getFields();
			for (i = 0; i < allFields.length; i++) {
				Log.e("Field name", allFields[i].getName());
			}
		} catch (SecurityException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
