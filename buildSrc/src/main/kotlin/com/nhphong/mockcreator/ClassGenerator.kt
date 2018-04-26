package com.nhphong.mockcreator

import com.nhphong.mockcreator.model.Method
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import java.io.File

class ClassGenerator(
		private val packageName: String?,
		private val staticImports: List<String>,
		private val contractName: String?,
		private val className: String?,
		private val classMethods: List<Method>
) {

	private val isCalledProperties = arrayListOf<String>()

	init {
		if (packageName.isNullOrEmpty() || className.isNullOrEmpty()) {
			throw IllegalArgumentException()
		}

		classMethods.forEach {
			isCalledProperties.add("${it.name}IsCalled")
		}
	}

	fun generate(directoryPath: String?): String {
		val displayClassName = "${contractName?.replace(Regex("[C|c]ontract"), "")
				?: ""}${className}Mock"
		val displaySuperClassName = if (contractName != null) "$contractName.$className" else "$className"

		val fileSpec = FileSpec.builder(packageName!!, displayClassName)
				.addType(createClass(displayClassName, displaySuperClassName))

		staticImports.forEach {
			val i = it.indexOf(".")
			fileSpec.addStaticImport(it.substring(0, i), it.substring(i + 1, it.length))
		}

		val file = fileSpec.build()

		if (directoryPath == null) {
			throw IllegalArgumentException("Invalid directory path")
		}

		val dir = File(directoryPath)
		if (!dir.exists() || !dir.isDirectory) {
			throw IllegalArgumentException("Invalid directory path")
		}

		file.writeTo(dir)
		return displayClassName
	}

	private fun createClass(displayClassName: String, displaySuperClassName: String): TypeSpec {
		return TypeSpec.classBuilder(ClassName(packageName!!, displayClassName))
				.addSuperinterface(ClassName("", displaySuperClassName))
				.addProperties(createClassProperties())
				.addFunction(createResetMethod())
				.addFunctions(createOverrideMethods())
				.addFunctions(createVerifyMethods())
				.build()
	}

	private fun createClassProperties(): List<PropertySpec> {
		val result = mutableListOf<PropertySpec>()
		isCalledProperties.forEach {
			result.add(PropertySpec.builder(it, Boolean::class, PRIVATE)
					.mutable(true)
					.initializer("false")
					.build())
		}

		classMethods.forEach {
			it.params.forEach {
				val simpleName = "${it.type}${if (it.type.contains("?")) "" else "?"} = null"
				result.add(PropertySpec.builder(it.name,
						ClassName("", simpleName), PRIVATE)
						.mutable(true)
						.build())
			}
		}
		return result
	}

	private fun createResetMethod(): FunSpec {
		val result = FunSpec.builder("reset")
		isCalledProperties.forEach {
			result.addStatement("$it = false")
		}

		classMethods.forEach {
			it.params.forEach {
				result.addStatement("${it.name} = null")
			}
		}
		return result.build()
	}

	private fun createOverrideMethods(): List<FunSpec> {
		val result = mutableListOf<FunSpec>()
		classMethods.forEach {
			val parameters = mutableListOf<ParameterSpec>()
			val recordingStatements = mutableListOf<String>()

			it.params.forEach {
				parameters.add(ParameterSpec.builder(it.name, ClassName("", it.type))
						.build())
				recordingStatements.add("this.${it.name} = ${it.name}")
			}

			val funSpec = FunSpec.builder(it.name)
					.addParameters(parameters)
					.addStatement("${isCalledProperties[classMethods.indexOf(it)]} = true")
					.addModifiers(OVERRIDE)

			recordingStatements.forEach {
				funSpec.addStatement(it)
			}

			result.add(funSpec.build())
		}
		return result
	}

	private fun createVerifyMethods(): List<FunSpec> {
		val result = mutableListOf<FunSpec>()
		classMethods.forEach {
			result.add(FunSpec.builder("verify${it.name.capitalize()}IsCalled")
					.addStatement("return ${isCalledProperties[classMethods.indexOf(it)]}")
					.build())

			if (it.params.isNotEmpty()) {
				val parameters = mutableListOf<ParameterSpec>()
				var statement = "return verify${it.name.capitalize()}IsCalled()"

				it.params.forEach {
					parameters.add(ParameterSpec.builder(it.name, ClassName("", it.type))
							.build())
					statement += " && this.${it.name} == ${it.name}"
				}

				result.add(FunSpec.builder("verify${it.name.capitalize()}IsCalledWith")
						.addParameters(parameters)
						.addStatement(statement)
						.build())
			}
		}
		return result
	}
}
