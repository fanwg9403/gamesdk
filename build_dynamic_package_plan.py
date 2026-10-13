from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

OUT = r"D:\Android\Project\WishfoxGame\WishFox_动态分包技术执行方案.docx"
BLUE = "1F4E79"
GRAY = "D9D9D9"
LIGHT = "F2F4F7"

doc = Document()
sec = doc.sections[0]
sec.top_margin = Inches(.68)
sec.bottom_margin = Inches(.68)
sec.left_margin = Inches(.78)
sec.right_margin = Inches(.78)

def font(run, name="Microsoft YaHei", size=10.5, bold=False, color=None):
    run.font.name = name
    rpr = run._element.get_or_add_rPr()
    rpr.rFonts.set(qn("w:eastAsia"), name)
    rpr.rFonts.set(qn("w:ascii"), name)
    run.font.size = Pt(size)
    run.bold = bold
    if color: run.font.color.rgb = RGBColor.from_string(color)

def para(p, before=0, after=5, line=1.15):
    p.paragraph_format.space_before = Pt(before)
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.line_spacing = line

def shade(cell, color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), color)
    tcPr.append(shd)

def borders(cell):
    tcPr = cell._tc.get_or_add_tcPr()
    bs = OxmlElement("w:tcBorders")
    for e in ("top", "left", "bottom", "right", "insideH", "insideV"):
        x = OxmlElement("w:" + e)
        x.set(qn("w:val"), "single")
        x.set(qn("w:sz"), "6")
        x.set(qn("w:color"), GRAY)
        bs.append(x)
    tcPr.append(bs)

def margins(cell):
    tcPr = cell._tc.get_or_add_tcPr()
    m = OxmlElement("w:tcMar")
    for e in ("top", "start", "bottom", "end"):
        x = OxmlElement("w:" + e)
        x.set(qn("w:w"), "110")
        x.set(qn("w:type"), "dxa")
        m.append(x)
    tcPr.append(m)

def add_p(text, size=10.5, bold=False, color=None, align=None, before=0, after=5):
    p = doc.add_paragraph()
    if text:
        r = p.add_run(text)
        font(r, size=size, bold=bold, color=color)
    if align is not None: p.alignment = align
    para(p, before, after)
    return p

def heading(text, level=1):
    p = doc.add_paragraph()
    r = p.add_run(text)
    font(r, size={1:15,2:12.5,3:11}[level], bold=True, color="000000")
    para(p, {1:13,2:9,3:6}[level], {1:6,2:4,3:3}[level], 1.05)
    return p

def bullet(text, numbered=False):
    p = doc.add_paragraph(style="List Number" if numbered else "List Bullet")
    r = p.add_run(text)
    font(r, size=10.2)
    para(p, after=2, line=1.1)
    return p

def code(text):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Inches(.18)
    p.paragraph_format.right_indent = Inches(.18)
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.line_spacing = 1.0
    lines = text.strip("\n").split("\n")
    for i, line in enumerate(lines):
        r = p.add_run(line)
        font(r, name="Consolas", size=8.5, color="1F1F1F")
        if i < len(lines)-1: r.add_break()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), "F6F8FA")
    p._p.get_or_add_pPr().append(shd)
    return p

def table(headers, rows, widths=None):
    t = doc.add_table(rows=1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    for i, value in enumerate(headers):
        c = t.rows[0].cells[i]
        p = c.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(value)
        font(r, size=9.1, bold=True, color="FFFFFF")
        shade(c, BLUE); borders(c); margins(c)
        c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        if widths: c.width = Inches(widths[i])
    for n, row in enumerate(rows):
        cells = t.add_row().cells
        for i, value in enumerate(row):
            c = cells[i]
            p = c.paragraphs[0]
            r = p.add_run(str(value))
            font(r, size=9.0)
            if n % 2: shade(c, LIGHT)
            borders(c); margins(c)
            c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            if widths: c.width = Inches(widths[i])
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return t

for s in ("Normal", "Body Text"):
    st = doc.styles[s]
    st.font.name = "Microsoft YaHei"
    st._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    st.font.size = Pt(10.5)

header = sec.header.paragraphs[0]
header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
font(header.add_run("WishFox 动态分包技术执行方案"), size=8.5, color="6B7280")
footer = sec.footer.paragraphs[0]
footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
font(footer.add_run("内部技术方案"), size=8.5, color="6B7280")

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
font(p.add_run("WishFox 动态分包技术执行方案"), size=22, bold=True)
para(p, before=54, after=12, line=1)
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
font(p.add_run("SDK、分享端、服务端与下载基础设施实施说明"), size=12, color="4B5563")
para(p, after=20)
table(["文档属性", "内容"], [
    ("母包来源", "第三方厂商或平台按 channel 提供已签名母包，WishFox 负责接收、校验和登记"),
    ("动态内容", "后端构建的 JSON 字符串，写入 APK Signing Block"),
    ("SDK 行为", "读取 JSON 原文，保存原文，加入 DynamicParam 请求头，不解析业务字段"),
    ("目标结果", "用户点击分享链接后自动下载对应的动态分包 APK"),
], [1.25,5.7])
add_p("本文已去掉母包自动化编译流水线。母包由第三方提供，平台只建设母包接收登记和运行时动态分包能力。", size=9.5, color="4B5563", align=WD_ALIGN_PARAGRAPH.CENTER)
doc.add_page_break()

heading("一 方案结论")
add_p("采用“母包登记一次、动态分包按需生成”的模式。第三方按 appId 和 channel 提供已经签名的母包。用户点击分享链接后，下载网关根据 appId、channel 和动态 JSON 找到母包，调用独立的 apk-packer，将 JSON 写入 APK Signing Block，上传对象存储并返回 CDN 地址。")
table(["数据", "约定"], [
    ("固定请求头", "AppId、ChannelId，沿用现有 FoxSdkHeaderInterceptor"),
    ("动态请求头", "DynamicParam，值为 APK 中读取出的原始 JSON 字符串"),
    ("APK 自定义 ID", "固定 WishFox ID，例如 0x57584644，上线前冻结"),
    ("母包定位", "appId + channel + 当前有效母包"),
    ("缓存键", "motherSha256 + appId + channel + payloadBytes 的 SHA-256"),
], [1.5,5.45])

heading("二 端到端流程")
for x in [
    "A 在游戏中分享，SDK 将 appId、channel、A 的当前登录 userId 传给微信小程序或独立 APP。",
    "小程序或独立 APP 调用 share-api，后端构建 DynamicParam JSON 并返回下载链接。",
    "B 点击链接，download-gateway 解析 appId、channel 和 data，查询母包。",
    "动态包未命中时，网关通过 Redis 锁调用 apk-packer。",
    "apk-packer 写入 JSON、验证 APK 并上传动态对象。",
    "网关返回 CDN 地址，B 下载、安装并启动游戏。",
    "SDK 读取 DynamicParam，并在公共请求中发送。",
    "B 再分享时使用 B 自己的登录 userId，生成新的 JSON 和新的动态包。"
]: bullet(x, True)

heading("三 参数协议")
heading("3.1 SDK 传给小程序或独立 APP", 2)
code('''{
  "appId": "game_10001",
  "channel": "channel_a",
  "userId": "A123"
}''')
add_p("这一步沿用现有实现。userId 表示当前分享人。")

heading("3.2 分享链接服务请求", 2)
code('''POST /v1/share/download-link
Content-Type: application/json

{
  "appId": "game_10001",
  "channel": "channel_a",
  "userId": "A123",
  "scene": "wechat",
  "campaignId": "summer_2026"
}''')
add_p("后端按业务构建最终 JSON，例如：")
code('''{"userId":"A123","scene":"wechat","campaignId":"summer_2026"}''')
add_p("SDK 不定义字段、不解析字段、不判断版本。服务端应使用紧凑 JSON，禁止换行和控制字符；中文建议使用 Unicode 转义，保证 HTTP 请求头兼容。建议原文不超过 2 KB，网关绝对上限为 4 KB。")

heading("3.3 下载链接", 2)
code('''https://download.example.com/v1/download
  ?appId=game_10001
  &channel=channel_a
  &data=<Base64URL(DynamicParam UTF-8 bytes)>''')
add_p("data 不是 token，而是 DynamicParam 原文的 Base64URL 编码，不需要保存映射表。涉及分佣或奖励时，增加无状态 HMAC sig 防止修改 data。")

heading("四 服务部署")
table(["组件", "部署位置", "职责", "公网"], [
    ("share-api", "应用集群", "校验分享参数、构建 JSON、生成下载 URL", "是"),
    ("download-gateway", "下载集群", "解析链接、查母包、查缓存、调用 packer", "是"),
    ("apk-packer", "内网服务", "使用 Walle 写入自定义 ID", "否"),
    ("PostgreSQL", "数据库集群", "保存母包元数据", "否"),
    ("Redis", "缓存/锁集群", "动态包生成锁", "否"),
    ("对象存储", "S3/OSS/COS/MinIO", "保存母包和动态 APK", "通过 CDN"),
    ("CDN", "云 CDN", "分发最终 APK", "是"),
], [1.25,1.25,3.05,1.05])
code('''公网
  api.example.com       -> share-api
  download.example.com  -> Nginx / Ingress -> download-gateway
                                                ├-> PostgreSQL
                                                ├-> Redis
                                                ├-> apk-packer
                                                └-> 对象存储 -> CDN

apk-packer、数据库、Redis、对象存储不直接开放公网。''')
add_p("推荐 Kubernetes 部署；小规模可以使用 Docker Compose。share-api、download-gateway 和 apk-packer 都应无状态并支持水平扩容。apk-packer 临时磁盘至少为最大 APK 的两倍。")

heading("五 母包接收、校验和登记")
add_p("母包由第三方厂商或平台按 channel 打包后提供。WishFox 不重新编译母包，只建立可信的母包登记记录。")
for x in [
    "第三方上传 APK 或上传到指定对象存储后提交登记。",
    "服务端计算 SHA-256，保存临时对象。",
    "验证包名、versionCode、versionName、appId 和 channel 关系。",
    "执行 apksigner verify --verbose --print-certs，记录证书 SHA-256。",
    "确认 APK 含有 v2/v3 Signing Block，能够被 PayloadWriter 处理。",
    "确认母包不存在旧的 WishFox 动态 JSON。",
    "校验通过后将对象写入不可变母包路径，并登记 PostgreSQL。"
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
add_p("母包路径：mother/{appId}/{channel}/{versionCode}/{sha256}.apk。母包对象不可覆盖；新版本使用新 SHA-256 路径。")

heading("六 Walle 与 apk-packer")
heading("6.1 选型和运行时", 2)
add_p("apk-packer 使用 Java 17 独立服务。Walle 的 payload_writer 是纯 Java 文件处理模块，运行时不需要 Android Studio、Gradle、apktool 或签名私钥。容器可额外携带 Android Build Tools 的 apksigner，仅用于验证输出 APK。")
heading("6.2 依赖", 2)
code('''dependencies {
    implementation("com.meituan.android.walle:payload_writer:1.1.7")
    implementation("com.meituan.android.walle:payload_reader:1.1.7")
    implementation("org.json:json:20080701")
}''')
add_p("Walle 官方文档说明：PayloadWriter 支持自定义 ID 写入，同一 ID 会覆盖；PayloadReader 支持按自定义 ID 读取。非 Android 环境需要额外提供 org.json。生产环境应将依赖固定到内部 Maven 仓库。")
heading("6.3 写入服务接口", 2)
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
heading("6.4 Java 调用", 2)
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
add_p("PayloadWriter.put 会原地修改 outputApk，并覆盖相同 ID。实际服务必须先复制到临时文件，写入完成且验证通过后再上传对象存储。需要字节级保持时，使用 ByteBuffer 版本，把 JSON 的 UTF-8 字节直接传给 PayloadWriter。")

heading("6.5 Walle 自行构建", 2)
code('''git clone https://github.com/Meituan-Dianping/walle.git
cd walle

# 旧版 Walle 构建建议使用 JDK 8 和项目 Gradle Wrapper
./gradlew :payload_reader:jar :payload_writer:jar

# 将 JAR 发布到内部 Maven 仓库，apk-packer 只依赖内部坐标。''')
add_p("不要在生产请求中启动 walle-cli 子进程。CLI 适合人工或批处理验证，服务端直接调用 PayloadWriter。构建阶段固定源码提交和依赖；运行镜像只包含 apk-packer JAR 和运行库。Walle 仓库为 Apache-2.0 许可证。")

heading("6.6 写入边界", 2)
for x in [
    "输入 APK 必须包含 v2 Signing Block；没有时拒绝处理。",
    "只使用 WishFox 自己的未知 ID，不能使用 v2/v3 签名 ID。",
    "保留所有原有 ID-value 和必要的 verity padding。",
    "支持包含 ZIP comment 的 APK，并纳入验收测试。",
    "写入后禁止重新执行 apksigner sign、zipalign、apktool rebuild 或重新加固。",
    "如果发行链路使用 APK v4 idsig，必须重新生成；首期只分发完整 APK。"
]: bullet(x)

heading("七 APK 写入算法")
for x in [
    "从 ZIP End of Central Directory 读取 Central Directory 起始偏移。",
    "在 Central Directory 前查找 APK Sig Block 42。",
    "解析所有 ID-value 项，保留 v2/v3 和其他未知项。",
    "删除并替换 WishFox 动态 JSON ID。",
    "重新生成 Signing Block 长度、ID-value、尾部长度和 magic。",
    "保留 Central Directory 与 ZIP comment，更新 Central Directory 偏移。",
    "重新读取输出文件并比较 payload 原始字节。",
    "执行 apksigner verify，检查包名、版本和证书指纹。",
]: bullet(x, True)
add_p("Walle PayloadWriter 已封装上述文件操作。团队需要在测试中验证 ZIP comment、重复写入、不同 JSON 长度和最大 APK 文件的行为。")

heading("八 下载网关和缓存")
heading("8.1 缓存键", 2)
code('''payloadBytes = UTF-8(DynamicParam)
payloadSha256 = SHA256(payloadBytes)
packageKey = SHA256(
  motherSha256 + "\\n" + appId + "\\n" + channel + "\\n" + payloadBytes
)
objectKey = dynamic/{appId}/{channel}/{motherSha256}/{packageKey}.apk''')
add_p("缓存键必须基于原始 JSON 字节，而不是解析后的对象。相同 JSON 可以命中缓存；JSON、channel 或母包变化都会得到新对象。")
heading("8.2 Nginx/Ingress", 2)
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
add_p("下载接口本身返回 Cache-Control: no-store。动态对象生成后，网关返回不带 Query 参数的 CDN 地址。CDN 对象使用 public、immutable、Range 和正确的 APK Content-Type。")
heading("8.3 Redis 生成锁", 2)
code('''lockKey = "pack:" + packageKey
owner = randomRequestId()
SET lockKey owner NX PX 300000

# 释放时必须校验 owner，不能直接 DEL
if GET(lockKey) == owner:
    DEL lockKey''')
add_p("Redis 官方的锁模式要求使用唯一持有者值和过期时间，释放时比较持有者，避免旧请求删除新请求的锁。未拿到锁的请求等待最终对象出现，不重复生成。")

heading("九 SDK 端改造")
add_p("当前 FoxSdkConfig 已保存 appId 和 channelId，FoxSdkHeaderInterceptor 已发送 AppId 和 ChannelId。SDK 改造集中在 FoxSdkConfig、APK PayloadReader 封装和 FoxSdkHeaderInterceptor。")
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
add_p("SDK 从 context.getApplicationInfo().sourceDir 读取固定自定义 ID 的 UTF-8 字节，直接转换为字符串。禁止 JSON 解析、重排、trim、字段过滤和重新序列化。首次读取后建议保存原始字符串；普通升级包没有动态数据时继续使用首次安装值。")
add_p("没有动态数据时不发送 DynamicParam，但 AppId、ChannelId 和其他现有公共请求头保持不变。")

heading("十 部署与运行要求")
table(["组件", "生产建议", "关键配置"], [
    ("share-api", "至少 2 副本", "数据库、HMAC 密钥、JSON 最大长度"),
    ("download-gateway", "2 至 4 副本", "Redis、对象存储、packer 地址、300 秒超时"),
    ("apk-packer", "按生成量扩容", "Java 17、临时磁盘、对象存储权限"),
    ("PostgreSQL", "高可用或云托管", "母包和审计记录"),
    ("Redis", "云托管或高可用", "生成锁 TTL 300 秒"),
    ("对象存储/CDN", "对象存储做源站", "完整路径缓存、APK Content-Type"),
], [1.45,1.55,4.15])
add_p("密钥放入密钥管理服务或 Kubernetes Secret。日志只记录 appId、channel、payloadSha256、packageKey、耗时和结果，不打印完整 DynamicParam。")
add_p("对象存储路径：mother/{appId}/{channel}/{version}/{sha}.apk；dynamic/{appId}/{channel}/{motherSha}/{packageKey}.apk。母包只读，动态包由 apk-packer 写入，CDN 只读。")

heading("十一 验收标准")
for x in [
    "第三方母包可以登记，包名、版本、签名证书和 channel 关系正确。",
    "用户首次点击分享链接可以生成并下载 APK。",
    "动态 APK 可以在目标 Android 版本安装和启动。",
    "输出 APK 的包名、versionCode 和签名证书与母包一致。",
    "SDK 读出的 JSON 与写入前的原始 JSON 字节一致。",
    "FoxSdkHeaderInterceptor 发送 AppId、ChannelId 和 DynamicParam。",
    "不同 JSON 不会命中同一个动态 APK，相同 JSON 可以命中缓存。",
    "并发请求只生成一个动态对象，不会返回半成品。",
    "母包更新后使用新的母包 SHA，不会复用旧动态包。",
    "JSON 增加字段不需要升级 SDK。",
    "动态参数缺失或读取失败时，游戏仍能正常初始化和请求。",
]: bullet(x)

heading("十二 各端职责")
table(["责任方", "必须完成的工作"], [
    ("第三方厂商/平台", "按 channel 提供已签名母包、appId、channel、包名、版本和证书信息。"),
    ("SDK 端", "读取固定 APK ID，保存原始 JSON，修改 FoxSdkHeaderInterceptor，发送 DynamicParam。"),
    ("小程序/独立 APP", "接收 SDK 参数，调用 share-api，展示和转发下载链接。"),
    ("分享服务", "按业务构建 JSON，编码 data，可选生成 HMAC。"),
    ("母包接收服务", "校验 APK、证书和 v2/v3，上传对象存储并登记。"),
    ("下载网关", "解析链接，计算缓存键，使用 Redis 锁，调用 apk-packer，返回 CDN 地址。"),
    ("apk-packer", "调用 Walle PayloadWriter 写入自定义 ID，验证 APK，上传动态对象。"),
    ("基础设施/运维", "部署数据库、Redis、对象存储、CDN、HTTPS、监控和告警。"),
    ("测试", "验证不同 JSON、不同 channel、并发、缓存隔离、安装和 SDK 读取。"),
], [1.55,5.6])

heading("十三 参考资料")
for x in [
    "Android APK Signature Scheme v2：https://source.android.com/docs/security/features/apksigning/v2?authuser=11",
    "Walle 项目：https://github.com/Meituan-Dianping/walle",
    "Walle PayloadWriter：https://github.com/Meituan-Dianping/walle/blob/master/payload_writer/README.md",
    "Walle PayloadReader：https://github.com/Meituan-Dianping/walle/blob/master/payload_reader/README.md",
    "Walle CLI：https://github.com/Meituan-Dianping/walle/blob/master/walle-cli/README.md",
    "Redis 分布式锁：https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/"
]: add_p(x, size=9.2, color="1F4E79")

doc.save(OUT)
print(OUT)

