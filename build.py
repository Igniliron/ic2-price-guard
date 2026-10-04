"""Reproducible Java 17 build. No Forge SDK or network needed.
python build.py [output.jar]
Tests: python tests/run_tests.py IC2Classic.jar dependency-directory
"""
from pathlib import Path
import hashlib,json,subprocess,sys,zipfile,shutil
root=Path(__file__).resolve().parent
output=Path(sys.argv[1]) if len(sys.argv)>1 else root/'ic2-price-guard-2.0.0-mc1.19.2.jar'
classes=root/'build/classes'
if classes.exists(): shutil.rmtree(classes)
classes.mkdir(parents=True)
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',str(classes),
    *(str(p) for p in sorted((root/'java').rglob('*.java')))],check=True)
entries={}
for folder in (root/'src',classes):
    for p in sorted(folder.rglob('*')):
        if p.is_file(): entries[p.relative_to(folder).as_posix()]=p.read_bytes()
for name in ('README_RU.txt','LICENSE','VALIDATION.txt'):
    entries[name]=(root/name).read_bytes()
for name in ('build.py','README_RU.txt','LICENSE','VALIDATION.txt'):
    entries['development/'+name]=(root/name).read_bytes()
for folder in ('src','java','tests'):
    for p in sorted((root/folder).rglob('*')):
        if p.is_file() and '__pycache__' not in p.parts: entries['development/'+p.relative_to(root).as_posix()]=p.read_bytes()
entries['META-INF/MANIFEST.MF']=b'Manifest-Version: 1.0\r\nImplementation-Title: IC2 Price Guard\r\nImplementation-Version: 2.0.0\r\n\r\n'
assert json.loads(entries['META-INF/coremods.json'])['ic2_price_guard'] in entries
output.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED) as z:
    for name,data in sorted(entries.items()):
        info=zipfile.ZipInfo(name,(2026,10,3,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o644<<16
        z.writestr(info,data)
with zipfile.ZipFile(output) as z: assert z.testzip() is None
print(output.resolve())
print('SHA256',hashlib.sha256(output.read_bytes()).hexdigest())
print('Bytes',output.stat().st_size)
