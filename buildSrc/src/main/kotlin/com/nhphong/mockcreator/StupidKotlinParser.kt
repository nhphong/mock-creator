package com.nhphong.mockcreator

import com.nhphong.mockcreator.model.Method
import com.nhphong.mockcreator.model.Param
import com.nhphong.mockcreator.model.ParseResult
import java.io.File

class StupidKotlinParser {
	fun parse(filePath: String): ParseResult {
		val file = File(filePath)

		if (!file.exists() || file.isDirectory) {
			throw IllegalArgumentException("File path is invalid")
		}

		val content = file.inputStream()
				.bufferedReader().use { it.readText() }
				.split(Regex("\\s+"))

		return ParseResult().apply {
			packageName = getPackageName(content)
			staticImports = getStaticImports(content)
			contractName = getContractName(content)
			className = getClassName(content)

			if (className.isNullOrEmpty()) {
				throw IllegalArgumentException("No Class Detected!")
			}

			classMethods = getMethods(content, className!!)
		}
	}

	private fun getPackageName(content: List<String>): String? {
		val index = content.indexOfFirst { it == "package" }
		return if (index != -1) content[index + 1] else null
	}

	private fun getStaticImports(content: List<String>): List<String> {
		val mutableContent = content.toMutableList()
		val result = mutableListOf<String>()

		while (mutableContent.contains("import")) {
			val index = mutableContent.indexOfFirst { it == "import" }
			result.add(mutableContent[index + 1])
			mutableContent.removeAt(index)
		}

		return result
	}

	private fun getContractName(content: List<String>): String? {
		return content.find {
			it.toLowerCase().contains("contract")
		}
	}

	private fun getClassName(content: List<String>): String? {
		var index = -1
		for (i in 0 until content.size - 1) {
			val token = content[i]
			val nextToken = content[i + 1].toLowerCase()
			if ((token == "class" || token == "interface") &&
					!nextToken.contains("contract")) {
				index = i
				break
			}
		}
		return if (index != -1) content[index + 1].replace(":", "") else null
	}

	private fun getMethods(content: List<String>, className: String): List<Method> {
		val classContent = mutableListOf<String>()
		val truncatedList = content.subList(content.indexOfFirst {
			it == className || it == "$className:" || it == "$className{"
		}, content.size)
		val nextClass = getClassName(truncatedList)
		if (nextClass.isNullOrEmpty()) {
			classContent.addAll(truncatedList)
		} else {
			val i = truncatedList.indexOfFirst {
				it == nextClass || it == "$nextClass:" || it == "$nextClass{"
			}
			classContent.addAll(truncatedList.subList(0, i))
		}

		//-----------

		val result = mutableListOf<Method>()
		val indices = mutableListOf<Int>()

		classContent.forEachIndexed { index, token ->
			if (token == "fun") {
				indices.add(index)
			}
		}

		indices.forEach {
			val prototype = getMethodPrototype(it, classContent)
			result.add(Method().apply {
				name = getMethodName(prototype)
				params = getParams(prototype)
			})
		}
		return result
	}

	private fun getMethodPrototype(start: Int, classContent: List<String>): List<String> {
		val startIndex = start + 1
		val truncatedList = classContent.subList(startIndex, classContent.size)
		val endIndex = truncatedList.indexOfFirst {
			it.contains(Regex("fun|abstract|}"))
		}
		return truncatedList.subList(0, endIndex)
	}

	private fun getMethodName(prototype: List<String>): String {
		val combinedString = prototype.joinToString(" ")
		val tokens = combinedString.split(Regex("[():,]")).toMutableList()
		return tokens.removeAt(0)
	}

	private fun getParams(prototype: List<String>): List<Param> {
		val combinedString = prototype.joinToString(" ")
		val tokens = combinedString.split(Regex("[():,]")).toMutableList()
		val methodName = tokens.removeAt(0)
		val result = mutableListOf<Param>()

		while(tokens.size > 0 && tokens[0].isNotEmpty()) {
			result.add(Param().apply {
				name = tokens.removeAt(0).trim()
				type = tokens.removeAt(0).trim()
				if (type.contains("=")) {
					type = type.substring(0, type.indexOf("=")).trim()
				}
			})
		}
		return result
	}
}
