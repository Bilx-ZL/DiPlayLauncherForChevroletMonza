#!/usr/bin/env python3
"""把 Guard 挂进 App.onCreate，并注册两个带图标的入口 Activity。"""
import os
import sys

work = sys.argv[1] if len(sys.argv) > 1 else 'work'

# ---------- 1. smali 注入 ----------
smali = os.path.join(work, 'smali/com/tricheer/launcherk218/App.smali')
if not os.path.exists(smali):
    print('[patch] FATAL: smali not found at', smali)
    sys.exit(1)

src = open(smali, encoding='utf-8').read()
if 'Lcom/tricheer/guard/Guard;' in src:
    print('[patch] smali already patched')
else:
    lines = src.split('\n')
    out = []
    in_method = False
    done = False
    for line in lines:
        if line.startswith('.method'):
            in_method = 'onCreate()V' in line
        if in_method and line.strip() == 'return-void' and not done:
            out.append('    invoke-static {p0}, Lcom/tricheer/guard/Guard;->start(Landroid/content/Context;)V')
            done = True
        out.append(line)
        if line.startswith('.end method'):
            in_method = False
    if not done:
        print('[patch] FATAL: onCreate()V / return-void not found')
        sys.exit(1)
    with open(smali, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out))
    print('[patch] smali injected')

# ---------- 1.5 拷贝图标资源 ----------
import shutil
icon_dir = os.path.join(work, 'res/drawable-xxhdpi')
os.makedirs(icon_dir, exist_ok=True)
for _f in ('ic_guard_apps.png', 'ic_guard_settings.png', 'ic_guard_diplay.png'):
    _src = os.path.join('icons', _f)
    if os.path.exists(_src):
        shutil.copy(_src, os.path.join(icon_dir, _f))
        print('[patch] icon copied:', _f)
    else:
        print('[patch] WARN: icon missing:', _src)

# ---------- 2. 注册入口 Activity（带 LAUNCHER，可被桌面加图标） ----------
man = os.path.join(work, 'AndroidManifest.xml')
xml = open(man, encoding='utf-8').read()

ACTS = [
    ('com.tricheer.guard.ui.AllAppsActivity', '全部应用', 'allapps', '@drawable/ic_guard_apps'),
    ('com.tricheer.guard.ui.SettingsActivity', '守护设置', 'settings', '@drawable/ic_guard_settings'),
    ('com.tricheer.guard.ui.OpenDiPlay', '打开DiPlay', 'diplay', '@drawable/ic_guard_diplay'),
]

added = 0
for cls, label, aff, icon in ACTS:
    if cls in xml:
        continue
    tag = (
        '    <activity android:name="%s" android:exported="true"\n'
        '        android:screenOrientation="landscape" android:label="%s"\n'
        '        android:taskAffinity="com.tricheer.guard.%s"\n'
        '        android:launchMode="singleTask" android:excludeFromRecents="true"\n'
        '        android:icon="%s"\n'
        '        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">\n'
        '        <intent-filter>\n'
        '            <action android:name="android.intent.action.MAIN" />\n'
        '            <category android:name="android.intent.category.LAUNCHER" />\n'
        '        </intent-filter>\n'
        '    </activity>\n' % (cls, label, aff, icon)
    )
    if '</application>' not in xml:
        print('[patch] FATAL: </application> not found')
        sys.exit(1)
    xml = xml.replace('</application>', tag + '</application>', 1)
    added += 1

if added:
    with open(man, 'w', encoding='utf-8') as f:
        f.write(xml)
print('[patch] activities added:', added)

# ---------- 3. 校验 ----------
chk = open(smali, encoding='utf-8').read()
if 'Lcom/tricheer/guard/Guard;->start' not in chk:
    print('[patch] FATAL: invoke missing after patch')
    sys.exit(1)

man2 = open(man, encoding='utf-8').read()
for cls, label, aff, icon in ACTS:
    if cls not in man2:
        print('[patch] FATAL: activity not registered:', cls)
        sys.exit(1)
if man2.count('android.intent.category.LAUNCHER') < 2:
    print('[patch] FATAL: launcher category missing')
    sys.exit(1)
print('[patch] ok')
