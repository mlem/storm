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

package io.pzstorm.storm.patch;

import io.pzstorm.storm.core.StormClassTransformer;
import io.pzstorm.storm.util.AsmUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Adds the ability to register your own Lua methods with {@link se.krka.kahlua.integration.annotations.LuaMethod}.
 * @see zombie.Lua.LuaManager.Exposer
 */
public class ExposerPatch implements ZomboidPatch {

	@Override
	public void applyPatch(StormClassTransformer transformer) {
		InsnList exposeAllMethod = transformer.getInstructionsForMethod(
				"exposeAll", "()V"
		);

		AbstractInsnNode node = AsmUtils.getFirstNode(exposeAllMethod, List.of(
				new VarInsnNode(Opcodes.ALOAD, 0),
				new FieldInsnNode(Opcodes.GETFIELD, "zombie/Lua/LuaManager$Exposer", "exposed", "Ljava/util/HashSet;"),
				new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/HashSet", "iterator", "()Ljava/util/Iterator;"),
				new VarInsnNode(Opcodes.ASTORE, 1)
		));

		exposeAllMethod.insertBefore(node, AsmUtils.createInsnList(
				new VarInsnNode(Opcodes.ALOAD, 0),
				new MethodInsnNode(Opcodes.INVOKESTATIC, "io/pzstorm/storm/lua/StormLuaExposer", "exposeAll",
						"(Lzombie/Lua/LuaManager$Exposer;)V", false)
		));
	}
}
