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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import com.google.common.collect.ImmutableList;

import io.pzstorm.storm.core.StormClassTransformer;
import io.pzstorm.storm.util.AsmUtils;

public class ZomboidFileSystemPatch implements ZomboidPatch {

	@Override
	public void applyPatch(StormClassTransformer transformer) {
		//  public void loadMods(ArrayList<String> var1)
		InsnList loadModsInstructions = transformer.getInstructionsForMethod("loadMods", "(Ljava/util/ArrayList;)V");

		// ...
		// var2 = this.mods.iterator();
		// -> insert <-
		// while(var2.hasNext()) {
		//    var3 = (String)var2.next();
		// 	  this.loadMod(var3);
		// }
		// ...
		AbstractInsnNode iteratorTarget = AsmUtils.getFirstNode(loadModsInstructions, ImmutableList.of(
						new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
								"java/util/ArrayList", "iterator", "()Ljava/util/Iterator;"
						),
						new VarInsnNode(Opcodes.ASTORE, 2)
				)
		);
		// skip to last instruction before the next LabelNode
		while (!(iteratorTarget.getNext() instanceof LabelNode)) {
			iteratorTarget = iteratorTarget.getNext();
		}

		// StormBootstrap.resetClassLoader();
		// StormModLoader.resetCatalogs();
		InsnList resetJarCatalog = AsmUtils.createInsnList(
				new MethodInsnNode(Opcodes.INVOKESTATIC, "io/pzstorm/storm/core/StormBootstrap", "resetClassLoader",
						"()V", false),
				new LabelNode(),
				new MethodInsnNode(Opcodes.INVOKESTATIC, "io/pzstorm/storm/core/StormModLoader", "resetCatalogs",
						"()V", false));

		// insert after instructions
		loadModsInstructions.insert(iteratorTarget, resetJarCatalog);

		// ...
		// var2 = this.mods.iterator();
		// StormModLoader.resetCatalogs();
		// while(var2.hasNext()) {
		//    var3 = (String)var2.next();
		// 	  this.loadMod(var3);
		// }
		// -> insert <-
		// ...
		AbstractInsnNode endOfWhileTarget = AsmUtils.getLastInstructionOfType(loadModsInstructions, JumpInsnNode.class);

		// skip to next FrameNode
		do {
			endOfWhileTarget = endOfWhileTarget.getNext();
		} while (!(endOfWhileTarget instanceof FrameNode));

		// StormBootstrap.loadAndRegisterMods();
		InsnList registerAllMods = AsmUtils.createInsnList(
				new MethodInsnNode(Opcodes.INVOKESTATIC, "io/pzstorm/storm/core/StormModLoader", "registerAllMods",
						"()V", false));

		// insert after instructions
		loadModsInstructions.insert(endOfWhileTarget, registerAllMods);

		//  public void loadMod(String var1)
		InsnList loadModInstructions = transformer.getInstructionsForMethod("loadMod", "(Ljava/lang/String;)V");

		// ...
		// this.loadList.clear();
		// this.searchFolders(modDir);
		// -> insert <-
		// ...
		AbstractInsnNode searchFoldersTarget = AsmUtils.getFirstNode(loadModInstructions, ImmutableList.of(
				new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
						"zombie/ZomboidFileSystem", "searchFolders", "(Ljava/io/File;)V"
				))
		);

		// skip to last instruction
		while (!(searchFoldersTarget.getNext() instanceof LabelNode)) {
			searchFoldersTarget = searchFoldersTarget.getNext();
		}

		// StormBootstrap.addJarFiles(var2);
		// StormBootstrap.registerModInfo(var1, this.loadList);
		InsnList addJarFiles = AsmUtils.createInsnList(
				new VarInsnNode(Opcodes.ALOAD, 2),
				new MethodInsnNode(Opcodes.INVOKESTATIC, "io/pzstorm/storm/core/StormModLoader", "addJarFiles",
						"(Ljava/io/File;)V", false),
				new LabelNode(),
				new VarInsnNode(Opcodes.ALOAD, 1),
				new VarInsnNode(Opcodes.ALOAD, 0),
				new FieldInsnNode(Opcodes.GETFIELD, "zombie/ZomboidFileSystem", "loadList", "Ljava/util/ArrayList;"),
				new MethodInsnNode(Opcodes.INVOKESTATIC, "io/pzstorm/storm/core/StormModLoader", "registerModInfo",
						"(Ljava/lang/String;Ljava/util/List;)V", false),
				new LabelNode()
		);

		// insert after instructions
		loadModInstructions.insert(searchFoldersTarget, addJarFiles);
	}
}
