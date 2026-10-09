import os

files_to_fix = [
    'src/main/java/mari/samba/auth/BruteForceProtectionService.java',
    'src/main/java/mari/samba/auth/BruteForceProtectionServiceImpl.java',
    'src/main/java/mari/samba/filesystem/DirectoryItemDto.java'
]

for file in files_to_fix:
    with open(file, 'rb') as f:
        content = f.read()
    if content.startswith(b'\xef\xbb\xbf'):
        content = content[3:]
        with open(file, 'wb') as f:
            f.write(content)
