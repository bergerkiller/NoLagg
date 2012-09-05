package com.bergerkiller.bukkit.nolagg;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.bergerkiller.bukkit.common.SafeField;

/**
 * Uses reflection to transfer/copy all the fields of a class
 */
public class ClassTemplate<T> {
	private final Class<T> type;
	private final List<Field> fields;

	private ClassTemplate(Class<T> type) {
		this.type = type;
		this.fields = new ArrayList<Field>();
		this.fillFields(type);
	}

	private void fillFields(Class<?> clazz) {
		if (clazz == null) {
			return;
		}
		for (Field field : clazz.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			field.setAccessible(true);
			this.fields.add(field);
		}
		this.fillFields(clazz.getSuperclass());
	}

	public Class<T> getType() {
		return this.type;
	}

	public void transfer(T from, T to) {
		for (Field field : this.fields) {
			try {
				field.set(to, field.get(from));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Attempts to find the first field of the type specified
	 * 
	 * @param type to find
	 * @return field
	 */
	public <K> SafeField<K> getField(String name) {
		return new SafeField<K>(this.getType(), name);
	}

	/**
	 * Attempts to create a new template for the class specified
	 * 
	 * @param clazz to create
	 * @return a new template, or null if the template could not be made
	 */
	public static <T> ClassTemplate<T> create(Class<T> clazz) {
		try {
			return new ClassTemplate<T>(clazz);
		} catch (Throwable t) {
			Bukkit.getLogger().log(Level.SEVERE, "[Native Classes] Failed to hook into class '" + clazz.getSimpleName() + "':");
			t.printStackTrace();
			return null;
		}
	}
}