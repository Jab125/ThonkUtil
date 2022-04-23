package com.jab125.thonkutil.misc.asm;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.knot.Knot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Modifies specified enums to allow runtime extension by making the $VALUES field non-final and
 * injecting constructor calls which are not valid in normal java code.
 */
public class RuntimeEnumExtender {

    private final Type STRING = Type.getType(String.class);
    private final Type ENUM = Type.getType(Enum.class);
    private final Type EXTENDABLE_ENUM_INTERFACE = Type.getType("Lcom/jab125/thonkutil/misc/impl/v1/ExtendableEnum;");
    private final Type ARRAY_UTILS = Type.getType("Lorg/apache/commons/lang3/ArrayUtils;"); //Don't directly reference this to prevent class loading.
    private final String ADD_DESC = Type.getMethodDescriptor(Type.getType(Object[].class), Type.getType(Object[].class), Type.getType(Object.class));
    private final Type ASM_UTILS = Type.getType("Lcom/jab125/thonkutil/misc/impl/v1/AsmUtils;"); //Again, not direct reference to prevent class loading.
    private final String CLEAN_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Class.class));
    private final String NAME_DESC = Type.getMethodDescriptor(STRING);
    private final String EQUALS_DESC = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, STRING);

    private static final boolean YAY = true;
    private static final boolean NAY = false;

    public boolean handlesClass(Type classType, boolean isEmpty)
    {
        return isEmpty ? NAY : YAY;
    }

    //@Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        //super.visit(version, access, name, signature, superName, interfaces);
        var d = new ClassNode();
        d.visit(version, access, name, signature, superName, interfaces);
        processClassWithFlags(d, Type.getType("L"+name+";"));
    }

    public int processClassWithFlags(final ClassNode classNode, final Type classType)
    {
        System.out.println(classNode.access);
        System.out.println(Opcodes.ACC_ENUM);
        System.out.println(classNode.access & Opcodes.ACC_ENUM);
        if ((classNode.access & Opcodes.ACC_ENUM) == 0) {
            System.exit(-1);
            throw new IllegalArgumentException("WHAT?");
            //return 0;
        }

        Type array = Type.getType("[" + classType.getDescriptor());
        final int flags = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;

        FieldNode values = classNode.fields.stream().filter(f -> f.desc.contentEquals(array.getDescriptor()) && ((f.access & flags) == flags)).findFirst().orElse(null);
        
        if (!classNode.interfaces.contains(EXTENDABLE_ENUM_INTERFACE.getInternalName())) {
            throw new RuntimeException("...");
            //return 0;
        }
        
        //Static methods named "create" with first argument as a string
        List<MethodNode> candidates = classNode.methods.stream()
                .filter(m -> ((m.access & Opcodes.ACC_STATIC) != 0) && m.name.equals("create"))
                .collect(Collectors.toList());
        
        if (candidates.isEmpty()) {
            throw new IllegalStateException("IExtensibleEnum has no candidate factory methods: " + classType.getClassName());
        }
        
        candidates.forEach(mtd ->
        {
            Type[] args = Type.getArgumentTypes(mtd.desc);
            if (args.length == 0 || !args[0].equals(STRING)) {
                throw new IllegalStateException("Enum has create method without String as first parameter: " + mtd.name + mtd.desc);
            }

            Type ret = Type.getReturnType(mtd.desc);
            if (!ret.equals(classType)) {
                throw new IllegalStateException("Enum has create method with incorrect return type: " + mtd.name + mtd.desc);
            }
            
            Type[] ctrArgs = new Type[args.length + 1];
            ctrArgs[0] = STRING;
            ctrArgs[1] = Type.INT_TYPE;
            for (int x = 1; x < args.length; x++)
                ctrArgs[1 + x] = args[x];

            String desc = Type.getMethodDescriptor(Type.VOID_TYPE, ctrArgs);

            MethodNode ctr = classNode.methods.stream().filter(m -> m.name.equals("<init>") && m.desc.equals(desc)).findFirst().orElse(null);
            if (ctr == null)
            {
                throw new IllegalStateException("Enum has create method with no matching constructor: " + desc);
            }

            if (values == null)
            {
                throw new IllegalStateException("Enum has create method but we could not find $VALUES");
            }

            values.access &= values.access & ~Opcodes.ACC_FINAL; //Strip the final so JITer doesn't inline things.

            mtd.access |= Opcodes.ACC_SYNCHRONIZED;
            mtd.instructions.clear();
            mtd.localVariables.clear();
            if (mtd.tryCatchBlocks != null)
            {
                mtd.tryCatchBlocks.clear();
            }
            if (mtd.visibleLocalVariableAnnotations != null)
            {
                mtd.visibleLocalVariableAnnotations.clear();
            }
            if (mtd.invisibleLocalVariableAnnotations != null)
            {
                mtd.invisibleLocalVariableAnnotations.clear();
            }
            InstructionAdapter ins = new InstructionAdapter(mtd);

            int vars = 0;
            for (Type arg : args)
                vars += arg.getSize();

            {
                vars += 1; //int x
                Label for_start = new Label();
                Label for_condition = new Label();
                Label for_inc = new Label();

                ins.iconst(0);
                ins.store(vars, Type.INT_TYPE);
                ins.goTo(for_condition);
                //if (!VALUES[x].name().equalsIgnoreCase(name)) goto for_inc
                ins.mark(for_start);
                ins.getstatic(classType.getInternalName(), values.name, values.desc);
                ins.load(vars, Type.INT_TYPE);
                ins.aload(array);
                ins.invokevirtual(ENUM.getInternalName(), "name", NAME_DESC, false);
                ins.load(0, STRING);
                ins.invokevirtual(STRING.getInternalName(), "equalsIgnoreCase", EQUALS_DESC, false);
                ins.ifeq(for_inc);
                //return VALUES[x];
                ins.getstatic(classType.getInternalName(), values.name, values.desc);
                ins.load(vars, Type.INT_TYPE);
                ins.aload(array);
                ins.areturn(classType);
                //x++
                ins.mark(for_inc);
                ins.iinc(vars, 1);
                //if (x < VALUES.length) goto for_start
                ins.mark(for_condition);
                ins.load(vars, Type.INT_TYPE);
                ins.getstatic(classType.getInternalName(), values.name, values.desc);
                ins.arraylength();
                ins.ificmplt(for_start);
            }

            {
                vars += 1; //enum ret;
                //ret = new ThisType(name, VALUES.length, args..)
                ins.anew(classType);
                ins.dup();
                ins.load(0, STRING);
                ins.getstatic(classType.getInternalName(), values.name, values.desc);
                ins.arraylength();
                int idx = 1;
                for (int x = 1; x < args.length; x++)
                {
                    ins.load(idx, args[x]);
                    idx += args[x].getSize();
                }
                ins.invokespecial(classType.getInternalName(), "<init>", desc, false);
                ins.store(vars, classType);
                // VALUES = ArrayUtils.add(VALUES, ret)
                ins.getstatic(classType.getInternalName(), values.name, values.desc);
                ins.load(vars, classType);
                ins.invokestatic(ARRAY_UTILS.getInternalName(), "add", ADD_DESC, false);
                ins.checkcast(array);
                ins.putstatic(classType.getInternalName(), values.name, values.desc);
                //EnumHelper.cleanEnumCache(ThisType.class)
                ins.visitLdcInsn(classType);
                ins.invokestatic(ASM_UTILS.getInternalName(), "cleanEnumCache", CLEAN_DESC, false);
                //init ret
                ins.load(vars, classType);
                ins.invokeinterface(EXTENDABLE_ENUM_INTERFACE.getInternalName(), "init", "()V");
                //return ret
                ins.load(vars, classType);
                ins.areturn(classType);
            }
        });
        return 1;
    }

}