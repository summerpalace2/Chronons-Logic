import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class AppNavProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ANNOTATION_NAME)
        val symbolsClass = symbols.filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }.toList()
        if (symbolsClass.isEmpty()) return emptyList()

        val entryList = mutableListOf<EntryMeta>()
        for (ksClass in symbolsClass) {
            val annotation = ksClass.annotations.firstOrNull {
                it.shortName.asString() == "AppNavDestination"
            } ?: continue

            val routeValue = annotation.arguments
                .firstOrNull { it.name?.asString() == "route" }?.value as? String
            if (routeValue.isNullOrEmpty()) {
                logger.error("@AppNavDestination 的 route 不能为空!", ksClass)
                continue
            }
            if (ksClass.classKind != ClassKind.CLASS && ksClass.classKind != ClassKind.OBJECT) {
                logger.error("@AppNavDestination 只能用于 Class 或 Object", ksClass)
                continue
            }
            val argumentTypeName = resolveArgumentType(ksClass)
            if (argumentTypeName == null) {
                logger.error("无法解析泛型参数 T", ksClass)
                continue
            }
            entryList.add(EntryMeta(route = routeValue, entryClass = ksClass, argumentTypeName = argumentTypeName))
        }
        generateCollectorCode(entryList)
        return emptyList()
    }

    private fun resolveArgumentType(ksClass: KSClassDeclaration): KSTypeReference? {
        for (superType in ksClass.superTypes) {
            val resolved = superType.resolve()
            if (resolved.declaration.qualifiedName?.asString() == ENTRY_NAME) {
                return resolved.arguments.firstOrNull()?.type
            }
        }
        return null
    }

    private fun generateCollectorCode(entryList: List<EntryMeta>) {
        if (entryList.isEmpty()) return
        val moduleName = options["MODULE_NAME"] ?: "App"
        val cleanModuleName = moduleName.replace("[^a-zA-Z0-9]".toRegex(), "_")

        val appNavCollectorClass = ClassName("com.chronotask.components.navigation.core.nav3", "AppNavCollector")
        val autoServiceClass = ClassName("com.google.auto.service", "AutoService")
        val kSerializerClass = ClassName("kotlinx.serialization", "KSerializer")
        val kClassClass = KClass::class.asClassName()

        entryList.forEach { meta ->
            val argumentTypeName = meta.argumentTypeName.toTypeName()
            val entryClassName = meta.entryClass.toClassName()
            val isObject = meta.entryClass.classKind == ClassKind.OBJECT
            val collectorInterface = appNavCollectorClass.parameterizedBy(argumentTypeName)
            val navEntryInitializer = if (isObject) entryClassName.toString() else "$entryClassName()"
            val serializerInitializer = "$argumentTypeName.serializer()"
            val fileName = "AppNavCollector_${cleanModuleName}_${meta.entryClass.simpleName.asString()}"

            val classSpec = TypeSpec.classBuilder(fileName)
                .addSuperinterface(collectorInterface)
                .addAnnotation(
                    com.squareup.kotlinpoet.AnnotationSpec.builder(autoServiceClass)
                        .addMember("%T::class", appNavCollectorClass)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("route", String::class)
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer("%S", meta.route)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("navEntry", entryClassName)
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer(navEntryInitializer)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("argumentClazz", kClassClass.parameterizedBy(argumentTypeName))
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer("%T::class", argumentTypeName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        "argumentSerializer",
                        kSerializerClass.parameterizedBy(argumentTypeName)
                    )
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer(serializerInitializer)
                        .build()
                )
                .build()

            val sourceFiles = listOfNotNull(meta.entryClass.containingFile).distinct().toTypedArray()
            val dependencies = Dependencies(aggregating = true, *sourceFiles)
            val fileSpec = FileSpec.builder(PACKAGE_NAME, fileName).addType(classSpec).build()
            try {
                fileSpec.writeTo(codeGenerator, dependencies)
            } catch (e: Exception) {
                logger.error("生成 $fileName 失败: ${e.stackTraceToString()}")
            }
        }
    }

    private data class EntryMeta(
        val route: String,
        val entryClass: KSClassDeclaration,
        val argumentTypeName: KSTypeReference
    )

    companion object {
        private const val ANNOTATION_NAME = "com.chronotask.components.navigation.core.nav3.AppNavDestination"
        private const val ENTRY_NAME = "com.chronotask.components.navigation.core.nav3.AppNavEntry"
        private const val PACKAGE_NAME = "com.chronotask.components.navigation.core.nav3.generated"
    }
}
