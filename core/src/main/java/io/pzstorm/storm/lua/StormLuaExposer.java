/*
 * Zomboid Storm - Java modding toolchain for Project Zomboid
 * Copyright (C) 2021 Matthew Cain
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.pzstorm.storm.lua;

import java.util.ArrayList;
import java.util.List;

import zombie.Lua.LuaManager;

/**
 * Sometimes you want to expose new methods to Lua. Add your class by calling {@link #registerLuaClass(Class)}
 */
public class StormLuaExposer {

	private final static List<Class> EXPOSED = new ArrayList<>();

	public static void registerLuaClass(Class type) {
		EXPOSED.add(type);
	}

	/**
	 * used by injected code in {@link LuaManager.Exposer#exposeAll()}.
	 * You shouldn't have any use of it
	 * @param exposer the exposer class which is called for each registered class
	 */
	public static void exposeAll(LuaManager.Exposer exposer) {
		for(Class toExpose : EXPOSED) {
			exposer.setExposed(toExpose);
		}
	}

}
