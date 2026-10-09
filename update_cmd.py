import re

with open('src/main/java/mari/samba/infra/LinuxCommands.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'public static String findDirectories(String path) {\n        return String.format(\n                "sudo find -L %s -mindepth 1 -maxdepth 1 -type d ! -name \'.*\' 2>/dev/null | sort",\n                escape(requireValidPath(path)));\n    }',
    'public static String findDirectories(String path) {\n        return String.format(\n                "sudo find -L %s -mindepth 1 -maxdepth 1 ! -name \'.*\' -printf \'%%y|%%s|%%p\\\\n\' 2>/dev/null | sort",\n                escape(requireValidPath(path)));\n    }'
)

with open('src/main/java/mari/samba/infra/LinuxCommands.java', 'w', encoding='utf-8') as f:
    f.write(content)
