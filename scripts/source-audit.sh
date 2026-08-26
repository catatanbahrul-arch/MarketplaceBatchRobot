#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
find "$root/app/src/main/java" -name '*.kt' -print0 | while IFS= read -r -d '' f; do
  python3 - "$f" <<'PY'
import sys
p=sys.argv[1]; s=open(p,encoding='utf-8').read()
assert s.count('{')==s.count('}'), p
assert s.count('(')==s.count(')'), p
PY
done
if find "$root" -name '*.kt.tmp' -o -name '*.tmp' | grep -q .; then
  echo 'Temporary files found'; exit 1
fi
python3 - "$root" <<'PY'
import sys, xml.etree.ElementTree as ET
root=sys.argv[1]
for p in [root+'/app/src/main/AndroidManifest.xml',root+'/app/src/main/res/xml/marketplace_accessibility_service.xml',root+'/app/src/main/res/values/styles.xml']:
    ET.parse(p)
print('XML PASS')
PY
echo 'SOURCE AUDIT PASS'
