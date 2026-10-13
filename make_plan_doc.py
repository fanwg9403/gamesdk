from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

OUT = "D:/Android/Project/WishfoxGame/WishFox_Dynamic_Package_Execution_Plan.docx"
BLUE = "1F4E79"
GRAY = "D9D9D9"
LIGHT = "F2F4F7"

doc = Document()
sec = doc.sections[0]
sec.top_margin = Inches(.68); sec.bottom_margin = Inches(.68)
sec.left_margin = Inches(.78); sec.right_margin = Inches(.78)

def font(run, name="Microsoft YaHei", size=10.5, bold=False, color=None):
    run.font.name = name
    rpr = run._element.get_or_add_rPr()
    rpr.rFonts.set(qn("w:eastAsia"), name)
    rpr.rFonts.set(qn("w:ascii"), name)
    run.font.size = Pt(size); run.bold = bold
    if color: run.font.color.rgb = RGBColor.from_string(color)

def spacing(p, before=0, after=5, line=1.15):
    p.paragraph_format.space_before = Pt(before)
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.line_spacing = line

def cell_style(c, fill=None):
    if fill:
        shd = OxmlElement("w:shd"); shd.set(qn("w:fill"), fill)
        c._tc.get_or_add_tcPr().append(shd)
    borders = OxmlElement("w:tcBorders")
    for e in ("top","left","bottom","right","insideH","insideV"):
        x = OxmlElement("w:" + e); x.set(qn("w:val"),"single")
        x.set(qn("w:sz"),"6"); x.set(qn("w:color"),GRAY); borders.append(x)
    c._tc.get_or_add_tcPr().append(borders)
    margins = OxmlElement("w:tcMar")
    for e in ("top","start","bottom","end"):
        x = OxmlElement("w:" + e); x.set(qn("w:w"),"110"); x.set(qn("w:type"),"dxa"); margins.append(x)
    c._tc.get_or_add_tcPr().append(margins)

def add_p(text="", size=10.5, bold=False, color=None, align=None, before=0, after=5):
    p = doc.add_paragraph()
    if text:
        font(p.add_run(text), size=size, bold=bold, color=color)
    if align is not None: p.alignment = align
    spacing(p, before, after)
    return p

def h(text, level=1):
    p = doc.add_paragraph()
    font(p.add_run(text), size={1:15,2:12.5,3:11}[level], bold=True)
    spacing(p, {1:13,2:9,3:6}[level], {1:6,2:4,3:3}[level], 1.05)
    return p

def bullet(text, number=False):
    p = doc.add_paragraph(style="List Number" if number else "List Bullet")
    font(p.add_run(text), size=10.2)
    spacing(p, after=2, line=1.1)
    return p

def code(text):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Inches(.18)
    p.paragraph_format.right_indent = Inches(.18)
    p.paragraph_format.space_before = Pt(3); p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.line_spacing = 1.0
    lines = text.strip("\n").split("\n")
    for i, line in enumerate(lines):
        font(p.add_run(line), name="Consolas", size=8.5, color="1F1F1F")
        if i < len(lines)-1: p.runs[-1].add_break()
    shd = OxmlElement("w:shd"); shd.set(qn("w:fill"), "F6F8FA")
    p._p.get_or_add_pPr().append(shd)
    return p

def table(headers, rows, widths=None):
    t = doc.add_table(rows=1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER; t.autofit = False
    for i, value in enumerate(headers):
        c = t.rows[0].cells[i]; p = c.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        font(p.add_run(value), size=9.1, bold=True, color="FFFFFF")
        cell_style(c, BLUE); c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        if widths: c.width = Inches(widths[i])
    for n, row in enumerate(rows):
        cells = t.add_row().cells
        for i, value in enumerate(row):
            c = cells[i]; font(c.paragraphs[0].add_run(str(value)), size=9.0)
            cell_style(c, LIGHT if n % 2 else None)
            c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            if widths: c.width = Inches(widths[i])
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return t

for s in ("Normal","Body Text"):
    st = doc.styles[s]; st.font.name = "Microsoft YaHei"
    st._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei"); st.font.size = Pt(10.5)

header = sec.header.paragraphs[0]; header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
font(header.add_run("WishFox \u52a8\u6001\u5206\u5305\u6280\u672f\u6267\u884c\u65b9\u6848"), size=8.5, color="6B7280")
footer = sec.footer.paragraphs[0]; footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
font(footer.add_run("\u5185\u90e8\u6280\u672f\u65b9\u6848"), size=8.5, color="6B7280")

p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
font(p.add_run("WishFox \u52a8\u6001\u5206\u5305\u6280\u672f\u6267\u884c\u65b9\u6848"), size=22, bold=True)
spacing(p, before=54, after=12, line=1)
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
font(p.add_run("SDK\u3001\u5206\u4eab\u7aef\u3001\u670d\u52a1\u7aef\u4e0e\u4e0b\u8f7d\u57fa\u7840\u8bbe\u65bd\u5b9e\u65bd\u8bf4\u660e"), size=12, color="4B5563")
spacing(p, after=20)
table(["\u6587\u6863\u5c5e\u6027","\u5185\u5bb9"], [
    ("\u6bcd\u5305\u6765\u6e90","\u7b2c\u4e09\u65b9\u5382\u5546\u6216\u5e73\u53f0\u6309 channel \u63d0\u4f9b\u5df2\u7b7e\u540d\u6bcd\u5305\uff0cWishFox \u53ea\u63a5\u6536\u3001\u6821\u9a8c\u548c\u767b\u8bb0"),
    ("\u52a8\u6001\u5185\u5bb9","\u540e\u7aef\u6784\u5efa\u7684 JSON \u5b57\u7b26\u4e32\uff0c\u5199\u5165 APK Signing Block"),
    ("SDK \u884c\u4e3a","\u8bfb\u53d6 JSON \u539f\u6587\uff0c\u4fdd\u5b58\u539f\u6587\uff0c\u52a0\u5165 DynamicParam \u8bf7\u6c42\u5934\uff0c\u4e0d\u89e3\u6790\u4e1a\u52a1\u5b57\u6bb5"),
    ("\u76ee\u6807\u7ed3\u679c","\u7528\u6237\u70b9\u51fb\u5206\u4eab\u94fe\u63a5\u540e\u81ea\u52a8\u4e0b\u8f7d\u5bf9\u5e94\u52a8\u6001\u5206\u5305 APK"),
], [1.25,5.7])
add_p("\u672c\u6587\u5df2\u53bb\u6389\u6bcd\u5305\u81ea\u52a8\u5316\u7f16\u8bd1\u6d41\u6c34\u7ebf\u3002\u6bcd\u5305\u7531\u7b2c\u4e09\u65b9\u63d0\u4f9b\uff0c\u5e73\u53f0\u53ea\u5efa\u8bbe\u6bcd\u5305\u63a5\u6536\u767b\u8bb0\u548c\u8fd0\u884c\u65f6\u52a8\u6001\u5206\u5305\u80fd\u529b\u3002", size=9.5, color="4B5563", align=WD_ALIGN_PARAGRAPH.CENTER)
doc.add_page_break()

h("\u4e00 \u65b9\u6848\u8303\u56f4\u548c\u6700\u7ec8\u5f62\u6001")
add_p("\u7b2c\u4e09\u65b9\u5e73\u53f0\u6309 appId \u548c channel \u63d0\u4f9b\u5df2\u7ecf\u7b7e\u540d\u7684\u6bcd\u5305\u3002\u7528\u6237\u70b9\u51fb\u5206\u4eab\u94fe\u63a5\u540e\uff0cdownload-gateway \u6839\u636e appId\u3001channel \u548c DynamicParam \u627e\u5230\u6bcd\u5305\uff0c\u8c03\u7528 apk-packer \u5c06 JSON \u5199\u5165 APK Signing Block\uff0c\u4e0a\u4f20\u5230\u5bf9\u8c61\u5b58\u50a8\uff0c\u518d\u8fd4\u56de CDN \u5730\u5740\u3002\u7528\u6237\u4e0d\u9700\u8981\u7b49\u5f85 Gradle \u7f16\u8bd1\uff0c\u5e73\u53f0\u4e5f\u4e0d\u9700\u8981\u4e3a\u6bcf\u4e00\u4e2a\u7528\u6237\u4fdd\u5b58\u6216\u6784\u5efa\u5b8c\u6574 APK\u3002")
table(["\u6570\u636e","\u6700\u7ec8\u7ea6\u5b9a"], [
    ("\u56fa\u5b9a\u8bf7\u6c42\u5934","AppId\u3001ChannelId\uff0c\u6cbf\u7528\u73b0\u6709 FoxSdkHeaderInterceptor"),
    ("\u52a8\u6001\u8bf7\u6c42\u5934","DynamicParam\uff0c\u503c\u4e3a APK \u4e2d\u8bfb\u53d6\u51fa\u7684\u539f\u59cb JSON \u5b57\u7b26\u4e32"),
    ("APK \u81ea\u5b9a\u4e49 ID","\u56fa\u5b9a WishFox ID\uff0c\u4f8b\u5982 0x57584644\uff0c\u4e0a\u7ebf\u524d\u51bb\u7ed3"),
    ("\u6bcd\u5305\u5b9a\u4f4d","appId + channel + \u5f53\u524d\u6709\u6548\u6bcd\u5305\u8bb0\u5f55"),
    ("\u52a8\u6001\u7f13\u5b58\u952e","motherSha256 + appId + channel + payloadBytes \u7684 SHA-256"),
], [1.5,5.45])
h("\u4e8c \u8fd0\u884c\u65f6\u94fe\u8def",1)
for x in [
    "A \u5728\u6e38\u620f\u4e2d\u5206\u4eab\uff0cSDK \u5c06 appId\u3001channel\u3001A \u7684\u5f53\u524d\u767b\u5f55 userId \u4f20\u7ed9\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u6216\u72ec\u7acb APP\u3002",
    "\u5c0f\u7a0b\u5e8f\u6216\u72ec\u7acb APP \u8c03\u7528 share-api\uff0c\u540e\u7aef\u6784\u5efa DynamicParam JSON \u5e76\u8fd4\u56de\u4e0b\u8f7d\u94fe\u63a5\u3002",
    "B \u70b9\u51fb\u94fe\u63a5\uff0cdownload-gateway \u89e3\u6790 appId\u3001channel \u548c data\uff0c\u67e5\u8be2\u6bcd\u5305\u3002",
    "\u7f13\u5b58\u672a\u547d\u4e2d\u65f6\uff0c\u7f51\u5173\u901a\u8fc7 Redis \u9501\u8c03\u7528 apk-packer\u3002",
    "apk-packer \u5199\u5165 JSON\u3001\u9a8c\u8bc1 APK \u5e76\u4e0a\u4f20\u52a8\u6001\u5bf9\u8c61\u3002",
    "\u7f51\u5173\u8fd4\u56de CDN \u5730\u5740\uff0cB \u4e0b\u8f7d\u3001\u5b89\u88c5\u5e76\u542f\u52a8\u6e38\u620f\u3002",
    "SDK \u8bfb\u53d6 DynamicParam\uff0c\u5728\u516c\u5171\u8bf7\u6c42\u4e2d\u53d1\u9001\u3002",
    "B \u518d\u5206\u4eab\u65f6\u4f7f\u7528 B \u81ea\u5df1\u7684\u767b\u5f55 userId\uff0c\u751f\u6210\u65b0\u7684 JSON \u548c\u52a8\u6001\u5305\u3002",
]: bullet(x, True)

h("\u4e09 \u6570\u636e\u534f\u8bae",1)
h("3.1 SDK \u5230\u5c0f\u7a0b\u5e8f\u6216\u72ec\u7acb APP",2)
code('''{
  "appId": "game_10001",
  "channel": "channel_a",
  "userId": "A123"
}''')
add_p("\u8fd9\u4e00\u6b65\u6cbf\u7528\u73b0\u6709\u5b9e\u73b0\u3002userId \u8868\u793a\u5f53\u524d\u5206\u4eab\u4eba\u3002")
h("3.2 \u5206\u4eab\u94fe\u63a5\u670d\u52a1\u8bf7\u6c42",2)
code('''POST /v1/share/download-link
Content-Type: application/json

{
  "appId": "game_10001",
  "channel": "channel_a",
  "userId": "A123",
  "scene": "wechat",
  "campaignId": "summer_2026"
}''')
add_p("\u540e\u7aef\u6784\u5efa\u6700\u7ec8 JSON\uff0c\u4f8b\u5982\uff1a")
code('''{"userId":"A123","scene":"wechat","campaignId":"summer_2026"}''')
add_p("SDK \u4e0d\u5b9a\u4e49\u5b57\u6bb5\u3001\u4e0d\u89e3\u6790\u5b57\u6bb5\u3001\u4e0d\u5224\u65ad\u7248\u672c\u3002\u670d\u52a1\u7aef\u4f7f\u7528\u7d27\u51d1 JSON\uff0c\u7981\u6b62\u6362\u884c\u548c\u63a7\u5236\u5b57\u7b26\uff1b\u4e2d\u6587\u5efa\u8bae\u7528 Unicode \u8f6c\u4e49\uff0c\u4fdd\u8bc1 HTTP \u8bf7\u6c42\u5934\u517c\u5bb9\u3002\u5efa\u8bae\u539f\u6587\u4e0d\u8d85\u8fc7 2 KB\uff0c\u7f51\u5173\u4e0a\u9650\u4e3a 4 KB\u3002")
h("3.3 \u4e0b\u8f7d\u94fe\u63a5",2)
code('''https://download.example.com/v1/download
  ?appId=game_10001
  &channel=channel_a
  &data=<Base64URL(DynamicParam UTF-8 bytes)>''')
add_p("data \u4e0d\u662f token\uff0c\u800c\u662f DynamicParam \u539f\u6587\u7684 Base64URL \u7f16\u7801\uff0c\u4e0d\u9700\u8981\u4fdd\u5b58\u6620\u5c04\u8868\u3002\u6d89\u53ca\u5206\u4f63\u6216\u5956\u52b1\u65f6\u589e\u52a0\u65e0\u72b6\u6001 HMAC sig\uff0c\u9632\u6b62\u4fee\u6539 data\u3002")

h("\u56db \u670d\u52a1\u90e8\u7f72",1)
table(["\u7ec4\u4ef6","\u90e8\u7f72\u4f4d\u7f6e","\u804c\u8d23","\u516c\u7f51"], [
    ("share-api","\u5e94\u7528\u96c6\u7fa4","\u6821\u9a8c\u5206\u4eab\u53c2\u6570\u3001\u6784\u5efa JSON\u3001\u751f\u6210\u4e0b\u8f7d URL","\u662f"),
    ("download-gateway","\u4e0b\u8f7d\u96c6\u7fa4","\u89e3\u6790\u94fe\u63a5\u3001\u67e5\u6bcd\u5305\u3001\u67e5\u7f13\u5b58\u3001\u8c03\u7528 packer","\u662f"),
    ("apk-packer","\u5185\u7f51\u670d\u52a1","\u4f7f\u7528 Walle \u5199\u5165\u81ea\u5b9a\u4e49 ID","\u5426"),
    ("PostgreSQL","\u6570\u636e\u5e93\u96c6\u7fa4","\u4fdd\u5b58\u6bcd\u5305\u5143\u6570\u636e","\u5426"),
    ("Redis","\u7f13\u5b58/\u9501\u96c6\u7fa4","\u52a8\u6001\u5305\u751f\u6210\u9501","\u5426"),
    ("\u5bf9\u8c61\u5b58\u50a8","S3/OSS/COS/MinIO","\u4fdd\u5b58\u6bcd\u5305\u548c\u52a8\u6001 APK","\u901a\u8fc7 CDN"),
    ("CDN","\u4e91 CDN","\u5206\u53d1\u6700\u7ec8 APK","\u662f"),
], [1.25,1.25,3.05,1.05])
code('''\u516c\u7f51
  api.example.com       -> share-api
  download.example.com  -> Nginx / Ingress -> download-gateway
                                                \u251c-> PostgreSQL
                                                \u251c-> Redis
                                                \u251c-> apk-packer
                                                \u2514-> \u5bf9\u8c61\u5b58\u50a8 -> CDN

apk-packer\u3001\u6570\u636e\u5e93\u3001Redis\u3001\u5bf9\u8c61\u5b58\u50a8\u4e0d\u76f4\u63a5\u5f00\u653e\u516c\u7f51\u3002''')
add_p("\u751f\u4ea7\u5efa\u8bae\u4f7f\u7528 Kubernetes\uff1b\u5c0f\u89c4\u6a21\u73af\u5883\u53ef\u4ee5\u4f7f\u7528 Docker Compose\u3002\u4e09\u4e2a\u4e1a\u52a1\u670d\u52a1\u5747\u5e94\u65e0\u72b6\u6001\u5e76\u652f\u6301\u6c34\u5e73\u6269\u5bb9\u3002apk-packer \u4e34\u65f6\u78c1\u76d8\u81f3\u5c11\u4e3a\u6700\u5927 APK \u7684\u4e24\u500d\u3002")

h("\u4e94 \u6bcd\u5305\u63a5\u6536\u3001\u6821\u9a8c\u548c\u767b\u8bb0",1)
add_p("\u6bcd\u5305\u7531\u7b2c\u4e09\u65b9\u5382\u5546\u6216\u5e73\u53f0\u6309 channel \u6253\u5305\u540e\u63d0\u4f9b\u3002WishFox \u4e0d\u91cd\u65b0\u7f16\u8bd1\u6bcd\u5305\uff0c\u53ea\u5efa\u7acb\u53ef\u4fe1\u7684\u6bcd\u5305\u767b\u8bb0\u8bb0\u5f55\u3002")
for x in [
    "\u7b2c\u4e09\u65b9\u4e0a\u4f20 APK\uff0c\u6216\u4e0a\u4f20\u5230\u6307\u5b9a\u5bf9\u8c61\u5b58\u50a8\u540e\u63d0\u4ea4\u767b\u8bb0\u3002",
    "\u670d\u52a1\u7aef\u8ba1\u7b97 SHA-256\uff0c\u4fdd\u5b58\u4e34\u65f6\u5bf9\u8c61\u3002",
    "\u9a8c\u8bc1\u5305\u540d\u3001versionCode\u3001versionName\u3001appId \u548c channel \u5173\u7cfb\u3002",
    "\u6267\u884c apksigner verify --verbose --print-certs\uff0c\u8bb0\u5f55\u8bc1\u4e66 SHA-256\u3002",
    "\u786e\u8ba4 APK \u542b\u6709 v2/v3 Signing Block\uff0c\u80fd\u591f\u88ab PayloadWriter \u5904\u7406\u3002",
    "\u786e\u8ba4\u6bcd\u5305\u4e0d\u5b58\u5728\u65e7\u7684 WishFox \u52a8\u6001 JSON\u3002",
    "\u6821\u9a8c\u901a\u8fc7\u540e\u5199\u5165\u4e0d\u53ef\u53d8\u6bcd\u5305\u8def\u5f84\u5e76\u767b\u8bb0 PostgreSQL\u3002",
]: bullet(x, True)
code('''CREATE TABLE mother_packages (
  id BIGSERIAL PRIMARY KEY,
  app_id VARCHAR(128) NOT NULL,
  channel VARCHAR(128) NOT NULL,
  package_name VARCHAR(255) NOT NULL,
  version_code BIGINT NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  sha256 CHAR(64) NOT NULL,
  certificate_sha256 CHAR(64) NOT NULL,
  file_size BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  UNIQUE(app_id, channel, version_code)
);''')
add_p("\u6bcd\u5305\u8def\u5f84\uff1amother/{appId}/{channel}/{versionCode}/{sha256}.apk\u3002\u6bcd\u5305\u5bf9\u8c61\u4e0d\u53ef\u8986\u76d6\uff0c\u65b0\u7248\u672c\u4f7f\u7528\u65b0 SHA-256 \u8def\u5f84\u3002")

h("\u516d Walle \u548c apk-packer",1)
h("6.1 \u9009\u578b\u548c\u8fd0\u884c\u65f6",2)
add_p("apk-packer \u4f7f\u7528 Java 17 \u72ec\u7acb\u670d\u52a1\u3002Walle \u7684 payload_writer \u662f\u7eaf Java \u6587\u4ef6\u5904\u7406\u6a21\u5757\uff0c\u8fd0\u884c\u65f6\u4e0d\u9700\u8981 Android Studio\u3001Gradle\u3001apktool \u6216\u7b7e\u540d\u79c1\u94a5\u3002\u5bb9\u5668\u53ef\u4ee5\u643a\u5e26 Android Build Tools \u7684 apksigner\uff0c\u4ec5\u7528\u4e8e\u9a8c\u8bc1\u8f93\u51fa APK\u3002")
h("6.2 \u4f9d\u8d56",2)
code('''dependencies {
    implementation("com.meituan.android.walle:payload_writer:1.1.7")
    implementation("com.meituan.android.walle:payload_reader:1.1.7")
    implementation("org.json:json:20080701")
}''')
add_p("Walle \u5b98\u65b9\u8bf4\u660e\uff1aPayloadWriter \u652f\u6301\u81ea\u5b9a\u4e49 ID \u5199\u5165\uff0c\u540c\u4e00 ID \u4f1a\u8986\u76d6\uff1bPayloadReader \u652f\u6301\u6309\u81ea\u5b9a\u4e49 ID \u8bfb\u53d6\u3002\u975e Android \u73af\u5883\u9700\u8981\u989d\u5916\u63d0\u4f9b org.json\u3002\u751f\u4ea7\u73af\u5883\u628a\u4f9d\u8d56\u56fa\u5b9a\u5230\u5185\u90e8 Maven \u4ed3\u5e93\u3002")
h("6.3 \u5199\u5165\u670d\u52a1\u63a5\u53e3",2)
code('''POST http://apk-packer:8090/internal/v1/pack

{
  "motherObjectKey": "mother/game_10001/channel_a/100/7a1b.apk",
  "outputObjectKey": "dynamic/game_10001/channel_a/7a1b/4c91.apk",
  "payloadBase64": "eyJ1c2VySWQiOiJBMTIzIiwic2NlbmUiOiJ3ZWNoYXQifQ"
}

200 OK
{
  "objectKey": "dynamic/game_10001/channel_a/7a1b/4c91.apk",
  "sha256": "4c91...",
  "fileSize": 183920384,
  "payloadSha256": "4c91..."
}''')
h("6.4 Java \u8c03\u7528",2)
code('''private static final int WISHFOX_DYNAMIC_JSON_BLOCK_ID = 0x57584644;

public void write(File motherApk, File outputApk, String json)
        throws IOException, SignatureNotFoundException {
    Files.copy(motherApk.toPath(), outputApk.toPath(),
            StandardCopyOption.REPLACE_EXISTING);

    PayloadWriter.put(
        outputApk,
        WISHFOX_DYNAMIC_JSON_BLOCK_ID,
        json,
        true // lowMemory
    );
}''')
add_p("PayloadWriter.put \u4f1a\u539f\u5730\u4fee\u6539 outputApk\uff0c\u5e76\u8986\u76d6\u76f8\u540c ID\u3002\u5b9e\u9645\u670d\u52a1\u5148\u590d\u5236\u5230\u4e34\u65f6\u6587\u4ef6\uff0c\u5199\u5165\u5b8c\u6210\u4e14\u9a8c\u8bc1\u901a\u8fc7\u540e\u518d\u4e0a\u4f20\u3002\u9700\u8981\u5b57\u8282\u7ea7\u4fdd\u6301\u65f6\uff0c\u4f7f\u7528 ByteBuffer \u7248\u672c\uff0c\u628a JSON \u7684 UTF-8 \u5b57\u8282\u76f4\u63a5\u4f20\u7ed9 PayloadWriter\u3002")
h("6.5 Walle \u6784\u5efa",2)
code('''git clone https://github.com/Meituan-Dianping/walle.git
cd walle

# \u65e7\u7248 Walle \u6784\u5efa\u5efa\u8bae\u4f7f\u7528 JDK 8 \u548c\u9879\u76ee Gradle Wrapper
./gradlew :payload_reader:jar :payload_writer:jar

# \u5c06 JAR \u53d1\u5e03\u5230\u5185\u90e8 Maven \u4ed3\u5e93\uff0capk-packer \u53ea\u4f9d\u8d56\u5185\u90e8\u5750\u6807\u3002''')
add_p("\u4e0d\u8981\u5728\u751f\u4ea7\u8bf7\u6c42\u4e2d\u542f\u52a8 walle-cli \u5b50\u8fdb\u7a0b\u3002CLI \u9002\u5408\u4eba\u5de5\u6216\u6279\u5904\u7406\u9a8c\u8bc1\uff0c\u670d\u52a1\u7aef\u76f4\u63a5\u8c03\u7528 PayloadWriter\u3002\u6784\u5efa\u9636\u6bb5\u56fa\u5b9a\u6e90\u7801\u63d0\u4ea4\u548c\u4f9d\u8d56\uff1b\u8fd0\u884c\u955c\u50cf\u53ea\u5305\u542b apk-packer JAR \u548c\u8fd0\u884c\u5e93\u3002Walle \u4ed3\u5e93\u4e3a Apache-2.0 \u8bb8\u53ef\u8bc1\u3002")
h("6.6 \u5199\u5165\u8fb9\u754c",2)
for x in [
    "\u8f93\u5165 APK \u5fc5\u987b\u5305\u542b v2 Signing Block\uff1b\u6ca1\u6709\u65f6\u62d2\u7edd\u5904\u7406\u3002",
    "\u53ea\u4f7f\u7528 WishFox \u81ea\u5df1\u7684\u672a\u77e5 ID\uff0c\u4e0d\u80fd\u4f7f\u7528 v2/v3 \u7b7e\u540d ID\u3002",
    "\u4fdd\u7559\u6240\u6709\u539f\u6709 ID-value \u548c\u5fc5\u8981\u7684 verity padding\u3002",
    "\u652f\u6301 ZIP comment\uff0c\u5e76\u7eb3\u5165\u9a8c\u6536\u6d4b\u8bd5\u3002",
    "\u5199\u5165\u540e\u7981\u6b62\u91cd\u65b0\u6267\u884c apksigner sign\u3001zipalign\u3001apktool rebuild \u6216\u91cd\u65b0\u52a0\u56fa\u3002",
    "\u5982\u679c\u53d1\u884c\u94fe\u8def\u4f7f\u7528 APK v4 idsig\uff0c\u5fc5\u987b\u91cd\u65b0\u751f\u6210\uff1b\u9996\u671f\u53ea\u5206\u53d1\u5b8c\u6574 APK\u3002",
]: bullet(x)

h("\u4e03 APK \u5199\u5165\u7b97\u6cd5",1)
for x in [
    "\u4ece ZIP End of Central Directory \u8bfb\u53d6 Central Directory \u8d77\u59cb\u504f\u79fb\u3002",
    "\u5728 Central Directory \u524d\u67e5\u627e APK Sig Block 42\u3002",
    "\u89e3\u6790\u6240\u6709 ID-value\uff0c\u4fdd\u7559 v2/v3 \u548c\u5176\u4ed6\u672a\u77e5\u9879\u3002",
    "\u5220\u9664\u5e76\u66ff\u6362 WishFox \u52a8\u6001 JSON ID\u3002",
    "\u91cd\u65b0\u751f\u6210 Signing Block \u957f\u5ea6\u3001ID-value\u3001\u5c3e\u90e8\u957f\u5ea6\u548c magic\u3002",
    "\u4fdd\u7559 Central Directory \u4e0e ZIP comment\uff0c\u66f4\u65b0 Central Directory \u504f\u79fb\u3002",
    "\u91cd\u65b0\u8bfb\u53d6\u8f93\u51fa\u6587\u4ef6\u5e76\u6bd4\u8f83 payload \u539f\u59cb\u5b57\u8282\u3002",
    "\u6267\u884c apksigner verify\uff0c\u68c0\u67e5\u5305\u540d\u3001\u7248\u672c\u548c\u8bc1\u4e66\u6307\u7eb9\u3002",
]: bullet(x, True)
add_p("Walle PayloadWriter \u5df2\u5c01\u88c5\u4e0a\u8ff0\u6587\u4ef6\u64cd\u4f5c\u3002\u56e2\u961f\u9700\u8981\u6d4b\u8bd5 ZIP comment\u3001\u91cd\u590d\u5199\u5165\u3001\u4e0d\u540c JSON \u957f\u5ea6\u548c\u6700\u5927 APK \u6587\u4ef6\u3002")

h("\u516b \u4e0b\u8f7d\u7f51\u5173\u548c\u7f13\u5b58",1)
h("8.1 \u7f13\u5b58\u952e",2)
code('''payloadBytes = UTF-8(DynamicParam)
payloadSha256 = SHA256(payloadBytes)
packageKey = SHA256(
  motherSha256 + "\\n" + appId + "\\n" + channel + "\\n" + payloadBytes
)
objectKey = dynamic/{appId}/{channel}/{motherSha256}/{packageKey}.apk''')
add_p("\u7f13\u5b58\u952e\u5fc5\u987b\u57fa\u4e8e\u539f\u59cb JSON \u5b57\u8282\uff0c\u800c\u4e0d\u662f\u89e3\u6790\u540e\u7684\u5bf9\u8c61\u3002\u76f8\u540c JSON \u547d\u4e2d\u7f13\u5b58\uff1bJSON\u3001channel \u6216\u6bcd\u5305\u53d8\u5316\u5f97\u5230\u65b0\u5bf9\u8c61\u3002")
h("8.2 Nginx/Ingress",2)
code('''location /v1/download {
    proxy_pass http://download-gateway:8080;
    proxy_http_version 1.1;
    proxy_request_buffering off;
    proxy_buffering off;
    proxy_connect_timeout 10s;
    proxy_send_timeout 300s;
    proxy_read_timeout 300s;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}''')
add_p("\u4e0b\u8f7d\u63a5\u53e3\u672c\u8eab\u8fd4\u56de Cache-Control: no-store\u3002\u751f\u6210\u5b8c\u6210\u540e\uff0c\u7f51\u5173\u8fd4\u56de\u4e0d\u5e26 Query \u53c2\u6570\u7684 CDN \u5730\u5740\u3002CDN \u5bf9\u8c61\u4f7f\u7528 public\u3001immutable\u3001Range \u548c\u6b63\u786e\u7684 APK Content-Type\u3002")
h("8.3 \u5e76\u53d1\u9501",2)
code('''lockKey = "pack:" + packageKey
owner = randomRequestId()
SET lockKey owner NX PX 300000

# \u91ca\u653e\u65f6\u5fc5\u987b\u6821\u9a8c owner\uff0c\u4e0d\u80fd\u76f4\u63a5 DEL
if GET(lockKey) == owner:
    DEL lockKey''')
add_p("\u672a\u62ff\u5230\u9501\u7684\u8bf7\u6c42\u7b49\u5f85\u6700\u7ec8\u5bf9\u8c61\u51fa\u73b0\uff0c\u4e0d\u91cd\u590d\u751f\u6210\u3002Redis \u5b98\u65b9\u7684\u5206\u5e03\u5f0f\u9501\u8bf4\u660e\u8981\u6c42\u4f7f\u7528\u552f\u4e00\u6301\u6709\u8005\u503c\u548c\u8fc7\u671f\u65f6\u95f4\uff0c\u5e76\u5728\u91ca\u653e\u65f6\u6821\u9a8c\u6301\u6709\u8005\u3002")

h("\u4e5d SDK \u7aef\u6539\u9020",1)
add_p("\u5f53\u524d FoxSdkConfig \u5df2\u4fdd\u5b58 appId \u548c channelId\uff0cFoxSdkHeaderInterceptor \u5df2\u53d1\u9001 AppId \u548c ChannelId\u3002\u6539\u9020\u96c6\u4e2d\u5728 FoxSdkConfig\u3001APK PayloadReader \u5c01\u88c5\u548c FoxSdkHeaderInterceptor\u3002")
code('''// FoxSdkConfig
private String dynamicParam;

public String getDynamicParam() {
    return dynamicParam;
}

// FoxSdkHeaderInterceptor
Request.Builder b = original.newBuilder()
    .header("AppId", config.getAppId())
    .header("ChannelId", config.getChannelId())
    .header("Authorization", authorization)
    .header("Version", "1.5.5");

String p = config.getDynamicParam();
if (p != null && !p.isEmpty()) {
    b.header("DynamicParam", p);
}''')
add_p("SDK \u4ece context.getApplicationInfo().sourceDir \u8bfb\u53d6\u56fa\u5b9a\u81ea\u5b9a\u4e49 ID \u7684 UTF-8 \u5b57\u8282\uff0c\u76f4\u63a5\u8f6c\u6362\u4e3a\u5b57\u7b26\u4e32\u3002\u7981\u6b62 JSON \u89e3\u6790\u3001\u91cd\u6392\u3001trim\u3001\u5b57\u6bb5\u8fc7\u6ee4\u548c\u91cd\u65b0\u5e8f\u5217\u5316\u3002\u9996\u6b21\u8bfb\u53d6\u540e\u4fdd\u5b58\u539f\u59cb\u5b57\u7b26\u4e32\uff1b\u666e\u901a\u5347\u7ea7\u5305\u6ca1\u6709\u52a8\u6001\u6570\u636e\u65f6\u7ee7\u7eed\u4f7f\u7528\u9996\u6b21\u5b89\u88c5\u503c\u3002\u6ca1\u6709\u52a8\u6001\u6570\u636e\u65f6\u4e0d\u53d1\u9001 DynamicParam\uff0c\u5176\u4ed6\u73b0\u6709\u8bf7\u6c42\u5934\u4fdd\u6301\u4e0d\u53d8\u3002")

h("\u5341 \u90e8\u7f72\u4e0e\u8fd0\u7ef4",1)
table(["\u7ec4\u4ef6","\u751f\u4ea7\u5efa\u8bae","\u5173\u952e\u914d\u7f6e"], [
    ("share-api","\u81f3\u5c11 2 \u526f\u672c","\u6570\u636e\u5e93\u3001HMAC \u5bc6\u94a5\u3001JSON \u957f\u5ea6"),
    ("download-gateway","2 \u81f3 4 \u526f\u672c","Redis\u3001\u5bf9\u8c61\u5b58\u50a8\u3001packer \u5730\u5740\u3001300 \u79d2\u8d85\u65f6"),
    ("apk-packer","\u6309\u751f\u6210\u91cf\u6269\u5bb9","Java 17\u3001\u4e34\u65f6\u78c1\u76d8\u3001\u5bf9\u8c61\u5b58\u50a8\u6743\u9650"),
    ("PostgreSQL","\u9ad8\u53ef\u7528\u6216\u4e91\u6258\u7ba1","\u6bcd\u5305\u548c\u5ba1\u8ba1\u8bb0\u5f55"),
    ("Redis","\u4e91\u6258\u7ba1\u6216\u9ad8\u53ef\u7528","\u751f\u6210\u9501 TTL 300 \u79d2"),
    ("\u5bf9\u8c61\u5b58\u50a8/CDN","\u5bf9\u8c61\u5b58\u50a8\u505a\u6e90\u7ad9","\u5b8c\u6574\u8def\u5f84\u7f13\u5b58\u3001APK Content-Type"),
], [1.45,1.55,4.15])
add_p("\u5bc6\u94a5\u653e\u5165\u5bc6\u94a5\u7ba1\u7406\u670d\u52a1\u6216 Kubernetes Secret\u3002\u65e5\u5fd7\u53ea\u8bb0\u5f55 appId\u3001channel\u3001payloadSha256\u3001packageKey\u3001\u8017\u65f6\u548c\u7ed3\u679c\uff0c\u4e0d\u6253\u5370\u5b8c\u6574 DynamicParam\u3002")
add_p("\u5bf9\u8c61\u5b58\u50a8\u8def\u5f84\uff1amother/{appId}/{channel}/{version}/{sha}.apk\uff1bdynamic/{appId}/{channel}/{motherSha}/{packageKey}.apk\u3002\u6bcd\u5305\u53ea\u8bfb\uff0c\u52a8\u6001\u5305\u7531 apk-packer \u5199\u5165\uff0cCDN \u53ea\u8bfb\u3002")

h("\u5341\u4e00 \u9a8c\u6536\u6807\u51c6",1)
for x in [
    "\u7b2c\u4e09\u65b9\u6bcd\u5305\u53ef\u4ee5\u767b\u8bb0\uff0c\u5305\u540d\u3001\u7248\u672c\u3001\u7b7e\u540d\u8bc1\u4e66\u548c channel \u5173\u7cfb\u6b63\u786e\u3002",
    "\u7528\u6237\u9996\u6b21\u70b9\u51fb\u5206\u4eab\u94fe\u63a5\u53ef\u4ee5\u751f\u6210\u5e76\u4e0b\u8f7d APK\u3002",
    "\u52a8\u6001 APK \u53ef\u4ee5\u5728\u76ee\u6807 Android \u7248\u672c\u5b89\u88c5\u548c\u542f\u52a8\u3002",
    "\u8f93\u51fa APK \u7684\u5305\u540d\u3001versionCode \u548c\u7b7e\u540d\u8bc1\u4e66\u4e0e\u6bcd\u5305\u4e00\u81f4\u3002",
    "SDK \u8bfb\u51fa\u7684 JSON \u4e0e\u5199\u5165\u524d\u7684\u539f\u59cb JSON \u5b57\u8282\u4e00\u81f4\u3002",
    "FoxSdkHeaderInterceptor \u53d1\u9001 AppId\u3001ChannelId \u548c DynamicParam\u3002",
    "\u4e0d\u540c JSON \u4e0d\u4f1a\u547d\u4e2d\u540c\u4e00\u4e2a\u52a8\u6001 APK\uff0c\u76f8\u540c JSON \u53ef\u4ee5\u547d\u4e2d\u7f13\u5b58\u3002",
    "\u5e76\u53d1\u8bf7\u6c42\u53ea\u751f\u6210\u4e00\u4e2a\u52a8\u6001\u5bf9\u8c61\uff0c\u4e0d\u4f1a\u8fd4\u56de\u534a\u6210\u54c1\u3002",
    "\u6bcd\u5305\u66f4\u65b0\u540e\u4f7f\u7528\u65b0\u7684\u6bcd\u5305 SHA\uff0c\u4e0d\u4f1a\u590d\u7528\u65e7\u52a8\u6001\u5305\u3002",
    "JSON \u589e\u52a0\u5b57\u6bb5\u4e0d\u9700\u8981\u5347\u7ea7 SDK\u3002",
    "\u52a8\u6001\u53c2\u6570\u7f3a\u5931\u6216\u8bfb\u53d6\u5931\u8d25\u65f6\uff0c\u6e38\u620f\u4ecd\u80fd\u6b63\u5e38\u521d\u59cb\u5316\u548c\u8bf7\u6c42\u3002",
]: bullet(x)

h("\u5341\u4e8c \u5404\u7aef\u804c\u8d23",1)
table(["\u8d23\u4efb\u65b9","\u5fc5\u987b\u5b8c\u6210\u7684\u5de5\u4f5c"], [
    ("\u7b2c\u4e09\u65b9\u5382\u5546/\u5e73\u53f0","\u6309 channel \u63d0\u4f9b\u5df2\u7b7e\u540d\u6bcd\u5305\u3001appId\u3001channel\u3001\u5305\u540d\u3001\u7248\u672c\u548c\u8bc1\u4e66\u4fe1\u606f\u3002"),
    ("SDK \u7aef","\u8bfb\u53d6\u56fa\u5b9a APK ID\uff0c\u4fdd\u5b58\u539f\u59cb JSON\uff0c\u4fee\u6539 FoxSdkHeaderInterceptor\uff0c\u53d1\u9001 DynamicParam\u3002"),
    ("\u5c0f\u7a0b\u5e8f/\u72ec\u7acb APP","\u63a5\u6536 SDK \u53c2\u6570\uff0c\u8c03\u7528 share-api\uff0c\u5c55\u793a\u548c\u8f6c\u53d1\u4e0b\u8f7d\u94fe\u63a5\u3002"),
    ("\u5206\u4eab\u670d\u52a1","\u6309\u4e1a\u52a1\u6784\u5efa JSON\uff0c\u7f16\u7801 data\uff0c\u53ef\u9009\u751f\u6210 HMAC\u3002"),
    ("\u6bcd\u5305\u63a5\u6536\u670d\u52a1","\u6821\u9a8c APK\u3001\u8bc1\u4e66\u548c v2/v3\uff0c\u4e0a\u4f20\u5bf9\u8c61\u5b58\u50a8\u5e76\u767b\u8bb0\u3002"),
    ("\u4e0b\u8f7d\u7f51\u5173","\u89e3\u6790\u94fe\u63a5\u3001\u8ba1\u7b97\u7f13\u5b58\u952e\u3001\u4f7f\u7528 Redis \u9501\u3001\u8c03\u7528 apk-packer\u3001\u8fd4\u56de CDN \u5730\u5740\u3002"),
    ("apk-packer","\u8c03\u7528 Walle PayloadWriter \u5199\u5165\u81ea\u5b9a\u4e49 ID\uff0c\u9a8c\u8bc1 APK\uff0c\u4e0a\u4f20\u52a8\u6001\u5bf9\u8c61\u3002"),
    ("\u57fa\u7840\u8bbe\u65bd/\u8fd0\u7ef4","\u90e8\u7f72\u6570\u636e\u5e93\u3001Redis\u3001\u5bf9\u8c61\u5b58\u50a8\u3001CDN\u3001HTTPS\u3001\u76d1\u63a7\u548c\u544a\u8b66\u3002"),
    ("\u6d4b\u8bd5","\u9a8c\u8bc1\u4e0d\u540c JSON\u3001\u4e0d\u540c channel\u3001\u5e76\u53d1\u3001\u7f13\u5b58\u9694\u79bb\u3001\u5b89\u88c5\u548c SDK \u8bfb\u53d6\u3002"),
], [1.55,5.6])

h("\u5341\u4e09 \u53c2\u8003\u8d44\u6599",1)
for x in [
    "Android APK Signature Scheme v2\uff1ahttps://source.android.com/docs/security/features/apksigning/v2?authuser=11",
    "Walle \u9879\u76ee\uff1ahttps://github.com/Meituan-Dianping/walle",
    "Walle PayloadWriter\uff1ahttps://github.com/Meituan-Dianping/walle/blob/master/payload_writer/README.md",
    "Walle PayloadReader\uff1ahttps://github.com/Meituan-Dianping/walle/blob/master/payload_reader/README.md",
    "Walle CLI\uff1ahttps://github.com/Meituan-Dianping/walle/blob/master/walle-cli/README.md",
    "Redis \u5206\u5e03\u5f0f\u9501\uff1ahttps://redis.io/docs/latest/develop/clients/patterns/distributed-locks/",
]: add_p(x, size=9.2, color="1F4E79")

doc.save(OUT)
print(OUT)

