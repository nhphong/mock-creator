package com.nhphong.mockcreator.model

class ParseResult {
	var packageName: String? = ""
	var staticImports: List<String> = mutableListOf()
	var contractName: String? = ""
	var className: String? = ""
	var classMethods: List<Method> = mutableListOf()
}
