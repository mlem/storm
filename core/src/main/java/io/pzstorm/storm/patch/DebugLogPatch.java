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

import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import com.google.common.collect.ImmutableList;

import io.pzstorm.storm.core.StormClassTransformer;
import io.pzstorm.storm.util.AsmUtils;

public class DebugLogPatch implements ZomboidPatch {

	@Override
	public void applyPatch(StormClassTransformer transformer) {
		/* though most of the logging is handled by DebugLogStream
		 * some logs are printed by DebugLog#log(String) method
		 */
		InsnList log = transformer.getInstructionsForMethod(
				"log", "(Lzombie/debug/DebugType;Ljava/lang/String;)V"
		);
		LabelNode node = Objects.requireNonNull(AsmUtils.getNthLabelNode(log, 3));

		log.insertBefore(node, AsmUtils.createInsnList(new VarInsnNode(Opcodes.ALOAD, 2), new MethodInsnNode(Opcodes.INVOKESTATIC,
				"io/pzstorm/storm/logging/ZomboidLogger", "info", "(Ljava/lang/String;)V")));

	}

}
