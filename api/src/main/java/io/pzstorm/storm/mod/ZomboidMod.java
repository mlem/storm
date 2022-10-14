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

package io.pzstorm.storm.mod;

/**
 * This class represents a Project Zomboid Java mod entry point. Every mod is expected to have
 * a single class that implements this class. Mods that do not implement this class will not be
 * registered and will not be eligible to subscribe to events.
 */
public interface ZomboidMod {
	void registerEventHandlers();

	default void registerLuaClasses() {
		// default implementation, so it's optional to be overwritten
	}
}
