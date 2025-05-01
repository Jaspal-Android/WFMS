package  com.atvantiq.wfms.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.ssas.jibli.data.prefs.PrefKeys
import com.ssas.jibli.data.prefs.SharedPrefPrint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefMain @Inject constructor(context: Context) : SharedPrefPrint {
	
	private val sharedPreferences: SharedPreferences =
		context.getSharedPreferences(PrefKeys.WFMS_PREF, Context.MODE_PRIVATE)

	override fun put(key: String, value: Int) {
		sharedPreferences.edit().putInt(key, value).apply()
	}
	
	override fun get(key: String, defaultValue: Int): Int {
		return sharedPreferences.getInt(key, defaultValue)
	}
	
	override fun put(key: String, value: Float) {
		sharedPreferences.edit().putFloat(key, value).apply()
	}
	
	override fun get(key: String, defaultValue: Float): Float {
		return sharedPreferences.getFloat(key, defaultValue)
	}
	
	override fun put(key: String, value: Boolean) {
		sharedPreferences.edit().putBoolean(key, value).apply()
	}
	
	override fun get(key: String, defaultValue: Boolean): Boolean {
		return sharedPreferences.getBoolean(key, defaultValue)
	}
	
	override fun put(key: String, value: Long) {
		sharedPreferences.edit().putLong(key, value).apply()
	}
	
	override fun get(key: String, defaultValue: Long): Long {
		return sharedPreferences.getLong(key, defaultValue)
	}
	
	override fun put(key: String, value: String?) {
		sharedPreferences.edit().putString(key, value).apply()
	}
	
	override fun get(key: String, defaultValue: String?): String? {
		return sharedPreferences.getString(key, defaultValue)
	}
	
	override fun delete(key: String) {
		sharedPreferences.edit().remove(key).apply()
	}
	
	override fun deleteAll() {
		sharedPreferences.edit().clear().apply()
	}
}
