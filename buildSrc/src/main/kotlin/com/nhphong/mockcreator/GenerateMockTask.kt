package com.nhphong.mockcreator

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

open class GenerateMockTask: DefaultTask() {

	lateinit var source: String

	@TaskAction
	fun generateMockClass() {
		source = Scanner(System.`in`).nextLine()
		val paths = mutableListOf<Path>()

		Files.walk(project.rootDir.toPath())
				.filter { !Files.isDirectory(it) }
				.filter { it.endsWith(source) || it.endsWith("$source.kt") }
				.forEach { paths.add(it) }

		if (paths.isEmpty()) {
			println("File not found!")
			return
		}

		if (paths.size > 1) {
			println("Multiple paths found!")
			paths.forEach(System.out::println)
			return
		}

		source = paths[0].toString()
		val parseResult = StupidKotlinParser().parse(source)
		val classGenerator = ClassGenerator(parseResult.packageName, parseResult.staticImports, parseResult.contractName, parseResult.className, parseResult.classMethods)
		val pkgPath = parseResult.packageName!!.replace(".", "/")
		val outputPath = source.substring(0, source.indexOf(pkgPath))
		val generatedFileName = classGenerator.generate(outputPath)
		println("$generatedFileName.kt was created")
	}
}
