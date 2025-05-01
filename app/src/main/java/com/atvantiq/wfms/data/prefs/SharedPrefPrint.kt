package com.ssas.jibli.data.prefs

interface SharedPrefPrint {
	/**
	 * Put integer
	 * @param key
	 * @param value
	 */
	fun put(key: String, value: Int)
	
	/**
	 * Get integer
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	operator fun get(key: String, defaultValue: Int): Int
	
	/**
	 * Put float
	 * @param key
	 * @param value
	 */
	fun put(key: String, value: Float)
	
	/**
	 * Get float
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	operator fun get(key: String, defaultValue: Float): Float
	
	/**
	 * Put boolean
	 * @param key
	 * @param value
	 */
	fun put(key: String, value: Boolean)
	
	/**
	 * Get boolean
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	operator fun get(key: String, defaultValue: Boolean): Boolean
	
	/**
	 * Put long
	 * @param key
	 * @param value
	 */
	fun put(key: String, value: Long)
	
	/**
	 * Get long
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	operator fun get(key: String, defaultValue: Long): Long
	
	/**
	 * Put string
	 * @param key
	 * @param value
	 */
	fun put(key: String, value: String?)
	
	/**
	 * Get string
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	operator fun get(key: String, defaultValue: String?): String?
	
	/**
	 * Delete updateDispatchingOrderData specified by key
	 * @param key
	 */
	fun delete(key: String)
	
	/**
	 * Delete all updateDispatchingOrderData from shared preferences
	 */
	fun deleteAll()
	
}
