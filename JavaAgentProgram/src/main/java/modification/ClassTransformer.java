package modification;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class ClassTransformer implements ClassFileTransformer {

    private static int numberOfLoadedClasses = 0;

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        byte[] byteCode = classfileBuffer;
        numberOfLoadedClasses++;

        if("TransactionProcessor".equals(className)) {
            try {
                ClassPool pool = ClassPool.getDefault();
                CtClass ctClass = pool.get("TransactionProcessor");
                ctClass.removeMethod(ctClass.getDeclaredMethod("main"));
                String classesNum = String.format("System.out.println(\"Number of loaded classes = %d\");", numberOfLoadedClasses);
                ctClass.addMethod(CtNewMethod.make("public static void main(String[] args) { " +
                    "TransactionProcessor tp = new TransactionProcessor();\n" +
                    "        long sumOfPastTime = 0;\n" +
                    "        long maxTime = Long.MIN_VALUE;\n" +
                    "        long minTime = Long.MAX_VALUE;\n" +
                    "        int count = 0;\n" +
                    "        for(int i = 0; i < 10; ++i) {\n" +
                    "            long start = System.currentTimeMillis();\n" +
                    "            tp.processTransaction(i);\n" +
                    "            long finish = System.currentTimeMillis();\n" +
                    "            long pastTime = finish - start;\n" +
                    "            sumOfPastTime += pastTime;\n" +
                    "            if (pastTime < minTime) {\n" +
                    "                minTime = pastTime;\n" +
                    "            }\n" +
                    "            if (pastTime > maxTime) {\n" +
                    "                maxTime = pastTime;\n" +
                    "            }\n" +
                    "            count++;\n" +
                    "        }\n" +
                    "        long averageTime = sumOfPastTime / count;\n" +
                    "        System.out.println(\"Max time = \" + maxTime);\n" +
                    "        System.out.println(\"Min time = \" + minTime);\n" +
                    "        System.out.println(\"Average time = \" + averageTime);\n " +
                        classesNum +
                        "}", ctClass));

                CtMethod[] methods = ctClass.getDeclaredMethods();

                for (CtMethod method : methods) {
                    if (method.getName().equals("processTransaction")) {
                        method.insertBefore("txNum += 99;");
                    }
                }
                try {
                    byteCode = ctClass.toBytecode();
                } catch (IOException | CannotCompileException e) {
                    e.printStackTrace();
                }
                ctClass.detach();
                return byteCode;

            } catch (NotFoundException | CannotCompileException e) {
                e.printStackTrace();
            }

        }
        return byteCode;
    }
}
