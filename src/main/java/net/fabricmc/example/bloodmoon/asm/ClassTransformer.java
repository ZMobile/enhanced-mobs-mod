package net.fabricmc.example.bloodmoon.asm;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.SWAP;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import org.spongepowered.asm.service.ILegacyClassTransformer;

public class ClassTransformer implements ILegacyClassTransformer
{
	Logger logger = LogManager.getLogger("Bloodmoon");

	public ClassTransformer()
	{

	}

	@Override
	public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass)
	{
		if (transformedName.equals("net.minecraft.client.renderer.RenderGlobal"))
		{
			return patchRenderGlobal(basicClass);
		}
		else if (transformedName.equals("net.minecraft.client.renderer.EntityRenderer"))
		{
			return patchEntityRendererClass(basicClass);
		}
		else if (transformedName.equals("net.minecraft.world.World"))
		{
			return patchWorld(basicClass);
		}
		return basicClass;
	}

	private byte[] patchWorld(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found World Class: " + classNode.name);

		MethodNode getSkyColor = null;
		MethodNode getMoonPhase = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals(MCPNames.method("func_72833_a")))
			{
				getSkyColor = mn;
			}
			else if (mn.name.equals(MCPNames.method("func_72853_d")))
			{
				getMoonPhase = mn;
			}
		}

		if (getSkyColor != null)
		{
			logger.log(Level.DEBUG, " - Found getSkyColor");

			for (int i = 0; i < getSkyColor.instructions.size(); i++)
			{
				AbstractInsnNode ain = getSkyColor.instructions.get(i);

				if (ain.getOpcode() == ARETURN)
				{
					InsnList toInsert = new InsnList();

					toInsert.add(new FieldInsnNode(GETSTATIC, "lumien/bloodmoon/client/ClientBloodmoonHandler", "INSTANCE", "Llumien/bloodmoon/client/ClientBloodmoonHandler;"));
					toInsert.add(new InsnNode(SWAP));
					toInsert.add(new MethodInsnNode(INVOKEVIRTUAL, "lumien/bloodmoon/client/ClientBloodmoonHandler", "skyColorHook", "(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", false));

					i += 3;
					getSkyColor.instructions.insertBefore(ain, toInsert);
				}
			}
		}

		if (getMoonPhase != null)
		{
			logger.log(Level.DEBUG, " - Found getMoonPhase");

			for (int i = 0; i < getMoonPhase.instructions.size(); i++)
			{
				AbstractInsnNode ain = getMoonPhase.instructions.get(i);

				if (ain.getOpcode() == IRETURN)
				{
					InsnList toInsert = new InsnList();

					toInsert.add(new FieldInsnNode(GETSTATIC, "lumien/bloodmoon/client/ClientBloodmoonHandler", "INSTANCE", "Llumien/bloodmoon/client/ClientBloodmoonHandler;"));
					toInsert.add(new MethodInsnNode(INVOKEVIRTUAL, "lumien/bloodmoon/client/ClientBloodmoonHandler", "moonColorHook", "()V", false));

					i += 2;
					getMoonPhase.instructions.insertBefore(ain, toInsert);
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchRenderGlobal(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);

		logger.log(Level.DEBUG, "Found Render Global Class: " + classNode.name);

		String renderSkyName = MCPNames.method("func_174976_a");
		MethodNode renderSky = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals(renderSkyName) && mn.desc.equals("(FI)V"))
			{
				renderSky = mn;
				break;
			}
		}

		if (renderSky != null && false)
		{
			logger.log(Level.DEBUG, " - Found renderSky");
			for (int i = 0; i < renderSky.instructions.size(); i++)
			{
				AbstractInsnNode ain = renderSky.instructions.get(i);
				if (ain instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode) ain;
					if (fin.name.equals(MCPNames.field("field_110927_h")))
					{
						logger.log(Level.DEBUG, " - Found moonColor");
						InsnList toInsert = new InsnList();

						renderSky.instructions.insert(fin, toInsert);
						i += 2;
					}
				}
				else if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;
					if (min.name.equals(MCPNames.method("func_72833_a")))
					{
						logger.log(Level.DEBUG, " - Found skyColor");
						InsnList toInsert = new InsnList();
						toInsert.add(new InsnNode(DUP));
						toInsert.add(new FieldInsnNode(GETSTATIC, "lumien/bloodmoon/client/ClientBloodmoonHandler", "INSTANCE", "Llumien/bloodmoon/client/ClientBloodmoonHandler;"));
						toInsert.add(new InsnNode(SWAP));
						toInsert.add(new MethodInsnNode(INVOKEVIRTUAL, "lumien/bloodmoon/client/ClientBloodmoonHandler", "skyColorHook", "(Lnet/minecraft/util/math/Vec3d;)V", false));
						renderSky.instructions.insert(min, toInsert);
						i += 4;
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchEntityRendererClass(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found EntityRenderer Class: " + classNode.name);

		String methodName = MCPNames.method("func_78472_g");

		MethodNode updateLightmap = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals(methodName))
			{
				updateLightmap = mn;
			}
		}

		if (updateLightmap != null)
		{
			logger.log(Level.DEBUG, " - Found updateLightmap");
			boolean insertedHook = false;
			for (int i = 0; i < updateLightmap.instructions.size(); i++)
			{
				AbstractInsnNode an = updateLightmap.instructions.get(i);
				if (an instanceof VarInsnNode && !insertedHook)
				{
					VarInsnNode iin = (VarInsnNode) an;
					if (iin.getOpcode() == ISTORE && iin.var == 23)
					{
						InsnList toInsert = new InsnList();

						toInsert.add(new FieldInsnNode(GETSTATIC, "lumien/bloodmoon/client/ClientBloodmoonHandler", "INSTANCE", "Llumien/bloodmoon/client/ClientBloodmoonHandler;"));
						toInsert.add(new VarInsnNode(ILOAD, 5));
						toInsert.add(new VarInsnNode(ILOAD, 21));
						toInsert.add(new MethodInsnNode(INVOKEVIRTUAL, "lumien/bloodmoon/client/ClientBloodmoonHandler", "manipulateRed", "(II)I", false));
						toInsert.add(new VarInsnNode(ISTORE, 21));

						toInsert.add(new FieldInsnNode(GETSTATIC, "lumien/bloodmoon/client/ClientBloodmoonHandler", "INSTANCE", "Llumien/bloodmoon/client/ClientBloodmoonHandler;"));
						toInsert.add(new VarInsnNode(ILOAD, 5));
						toInsert.add(new VarInsnNode(ILOAD, 22));
						toInsert.add(new MethodInsnNode(INVOKEVIRTUAL, "lumien/bloodmoon/client/ClientBloodmoonHandler", "manipulateGreen", "(II)I", false));
						toInsert.add(new VarInsnNode(ISTORE, 22));

						toInsert.add(new FieldInsnNode(GETSTATIC, "lumien/bloodmoon/client/ClientBloodmoonHandler", "INSTANCE", "Llumien/bloodmoon/client/ClientBloodmoonHandler;"));
						toInsert.add(new VarInsnNode(ILOAD, 5));
						toInsert.add(new VarInsnNode(ILOAD, 23));
						toInsert.add(new MethodInsnNode(INVOKEVIRTUAL, "lumien/bloodmoon/client/ClientBloodmoonHandler", "manipulateBlue", "(II)I", false));
						toInsert.add(new VarInsnNode(ISTORE, 23));

						updateLightmap.instructions.insert(iin, toInsert);
						insertedHook = true;
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchDummyClass(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.INFO, "Found Dummy Class: " + classNode.name);

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public boolean isDelegationExcluded() {
		return false;
	}
}
