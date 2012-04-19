# -*- coding: utf-8 -*-
"""
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 
 Copyright © 2011 ForgeRock AS. All rights reserved.
 
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the License). You may not use this file except in
 compliance with the License.
 
 You can obtain a copy of the License at
 http://forgerock.org/license/CDDLv1.0.html
 See the License for the specific language governing
 permission and limitations under the License.
 
 When distributing Covered Code, include this CDDL
 Header Notice in each file and include the License file
 at http://forgerock.org/license/CDDLv1.0.html
 If applicable, add the following below the CDDL Header,
 with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"
"""

"""
@author yaromin
"""
import os, json, re, sys, shutil;

scriptDirectory = sys.path[0]

homeDir = os.path.expanduser('~')
jsonPathSplitter = re.compile(r'/')
openidmDirectory = scriptDirectory + "/.."
profileDirectory = sys.argv[1]
defaultConfigFilePath = profileDirectory + "/profile-config.json"

def readSubstitutionConfig(configFilePath):
	configFile = open(configFilePath)
	fileContent = configFile.read()
	configFile.close()

	fileContent = re.sub("\${openidm-dir}",openidmDirectory,fileContent)	
	fileContent = re.sub("\${profile-dir}",profileDirectory,fileContent)	
	fileContent = re.sub("\${home-dir}",homeDir,fileContent)	

	#print fileContent;
	return json.loads(fileContent)

def performRegexReplacement(srcFileContent, replacementConfig):
	for i in replacementConfig[u'substitutions']:
                key = i[u'key']
                value = i[u'value']
		
		print "replacing text using regex " + key
		replaceRegex=re.search(key,srcFileContent);
		if replaceRegex and replaceRegex.groups() > 0:
			srcFileContent = srcFileContent[0:replaceRegex.start(1)] + value + srcFileContent[replaceRegex.end(1):len(srcFileContent)]
			print "replaced by regex:" + key
		else:
			print "matching string not found for regex:" + key
	return srcFileContent

def performPropertiesReplacement(srcFileContent, replacementConfig):
	print "properties replacement"
	for i in replacementConfig[u'substitutions']:
                key = i[u'key']
                value = i[u'value']
		
		print "replacing property " + key
		replaceRegex=re.search('\n[^#]*' + key + '=(.*)',srcFileContent);
		if replaceRegex and replaceRegex.groups() > 0:
			srcFileContent = srcFileContent[0:replaceRegex.start(1)] + value + srcFileContent[replaceRegex.end(1):len(srcFileContent)]
			print "replaced property " + key
	return srcFileContent

def performJsonReplacement(srcFileContent, replacementConfig):
	jsonContent = json.loads(srcFileContent)
	for i in replacementConfig[u'substitutions']:
		key = i[u'key'] 
		value = i[u'value']
		
		print "replacing key " + key
		splittedPath = jsonPathSplitter.split(key)
		print "splittedPath " + ''.join(splittedPath);
		splittedPath.remove('')
		lastPathElement = splittedPath.pop()
		
		currentContext = jsonContent
		for j in splittedPath:
			currentContext = currentContext[j]
		print "replacing last path element " + lastPathElement
		currentContext[lastPathElement]=value
	return json.dumps(jsonContent, indent=4)
			

def performReplacement(replacementConfig):
	replacementMethod = replacementConfig[u'replacementMethod']
	filePath = replacementConfig[u'filePath']
	
	print "opening file for text replacement " + filePath
	srcFile = open(filePath, 'r')
	srcFileContent = srcFile.read()
	srcFile.close()

	if(replacementMethod == 'json'):
		result=performJsonReplacement(srcFileContent, replacementConfig)
	elif(replacementMethod =='properties'):
		result=performPropertiesReplacement(srcFileContent, replacementConfig)
	elif(replacementMethod =='regex'):
		result=performRegexReplacement(srcFileContent, replacementConfig)
	else:
		print "replacementMethod not recognized: " + replacementMethod
		result=srcFileContent

	targetFile = open(filePath, 'w')
	targetFile.write(result);
        targetFile.close()

def removeFile(removePath):
	print "remove file/directory " + removePath
	if os.path.exists(removePath):
		if os.path.isdir(removePath):
			try:
				shutil.rmtree(removePath)
			except OSError:
				os.remove(removePath)	
		else:
			os.remove(removePath)
	else:
		print "File hasn't been removed. File not found: " + removePath

def performFileRemoval(description):
	removePath = description[u'path']
	removeFile(removePath)
	

def performFileCopy(description):
	sourcePath = description[u'sourcePath']
	targetPath = description[u'targetPath']
	print "copy file src:" + sourcePath + " target:" + targetPath
	if os.path.exists(targetPath) and os.path.exists(sourcePath):
		removeFile(targetPath)
	if os.path.isdir(sourcePath):
		shutil.copytree(sourcePath, targetPath);
	else:
		shutil.copyfile(sourcePath, targetPath);

def performFileMove(description):
	sourcePath = description[u'sourcePath']
	targetPath = description[u'targetPath']
	print "move file src:" + sourcePath + " target:" + targetPath
	if os.path.exists(targetPath) and os.path.exists(sourcePath):
		removeFile(targetPath)
	shutil.move(sourcePath,targetPath)

def performCreateSymlink(description):
	symlinkPath = description[u'symlinkPath']
	targetPath = description[u'targetPath']
	os.symlink(targetPath, symlinkPath)


def performSubconfiguration(description):
	configurationFilePath = description[u'configurationFilePath']
	print "performing subconfiguration using file:" + configurationFilePath
	performConfiguration(configurationFilePath)

	
def performConfiguration(configFilePath = defaultConfigFilePath):
	substitutionConfig = readSubstitutionConfig(configFilePath)
	#print substitutionConfig
	for i in substitutionConfig[u'actions']:
		action = i[u'action']
		description = i[u'description']
		if action == 'substituteInFile':
			performReplacement(description)
		elif action == 'removeFile':
			performFileRemoval(description)
		elif action == 'moveFile':
			performFileMove(description)
		elif action == 'copyFile':
			performFileCopy(description)
		elif action == 'subConfigure':
			performSubconfiguration(description)
		elif action == 'symlink':
			performCreateSymlink(description)
		else:
			print "not recognized action:" + action
performConfiguration()
