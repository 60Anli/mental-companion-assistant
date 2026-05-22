# mental-companion-assistant

蹇冪悊闄即鍔╂墜 MVP锛岀敤浜庡睍绀衡€滄湰鍦?鍦ㄧ嚎澶фā鍨嬫帴鍏?+ Prompt Engineering + 娣峰悎妫€绱?RAG + 璁板繂 + 瑙勫垯椋庨櫓璇嗗埆 + LLM 缁撴瀯鍖栧垎绫?+ 宸ュ叿璋冪敤鈥濈殑 AI Agent 宸ヤ綔娴佹惌寤鸿兘鍔涖€傜郴缁熷畾浣嶆槸蹇冪悊闄即銆佸績鐞嗙煡璇嗛棶绛斻€佸挩璇㈠垎娴佸拰楂橀闄╅璀︼紝涓嶆彁渚涘尰鐤楄瘖鏂紝涔熶笉鏇夸唬鍖荤敓銆佸績鐞嗘不鐤楀笀鎴栫揣鎬ユ湇鍔°€?
## 鎶€鏈爤

- Java 17, Spring Boot 3, Spring Security, JWT
- MySQL, MyBatis-Plus
- Redis 鐭湡璁板繂
- LangChain4j 渚濊禆寮曞叆锛屽綋鍓?MVP 浠ュ彲鎻掓嫈 `LlmClient` 灏佽妯″瀷璋冪敤
- Chroma 鍚戦噺鏁版嵁搴?+ Lucene BM25 绋€鐤忔绱?+ RRF 铻嶅悎鎺掑簭
- OpenAI-compatible API / Ollama 鏈湴妯″瀷 / 鏈湴寰皟妯″瀷鎺ュ叆閰嶇疆
- Spring Mail
- EasyExcel
- Vue3, Element Plus, Vite
- docker-compose 绠＄悊 MySQL銆丷edis 鍜?Chroma

## 椤圭洰缁撴瀯

```text
mental-companion-assistant
鈹溾攢 backend
鈹? 鈹溾攢 src/main/java/com/example/mentalcompanion
鈹? 鈹? 鈹溾攢 controller
鈹? 鈹? 鈹溾攢 service
鈹? 鈹? 鈹溾攢 tool
鈹? 鈹? 鈹溾攢 rag
鈹? 鈹? 鈹溾攢 llm
鈹? 鈹? 鈹溾攢 security
鈹? 鈹? 鈹溾攢 mapper
鈹? 鈹? 鈹斺攢 domain
鈹? 鈹斺攢 src/main/resources/application.yml
鈹溾攢 frontend
鈹溾攢 pretrain
鈹? 鈹溾攢 configs
鈹? 鈹溾攢 scripts
鈹? 鈹溾攢 data
鈹? 鈹斺攢 ollama
鈹溾攢 docker/mysql/init.sql
鈹溾攢 docker-compose.yml
鈹斺攢 data/sample-knowledge.md
```

## 宸ヤ綔娴?
```mermaid
flowchart TD
    A[鐢ㄦ埛杈撳叆] --> B[淇濆瓨鐢ㄦ埛娑堟伅鍒?chat_message]
    B --> C[KnowledgeSearchTool: 娣峰悎妫€绱?RAG]
    C --> C1[Chroma Dense 鍚戦噺鍙洖]
    C --> C2[Lucene BM25 Sparse 鍏抽敭璇嶅彫鍥瀅
    C1 --> C3[RRF 铻嶅悎鎺掑簭]
    C2 --> C3
    C3 --> R[鍔犺浇 Redis 鏈€杩?10 杞煭鏈熻蹇哴
    R --> S[鍔犺浇 MySQL 闀挎湡璁板繂鎽樿]
    S --> D[IntentRecognitionService: LLM JSON 鎰忓浘璇嗗埆]
    D --> E[RiskRuleService: 瑙勫垯鍏抽敭璇嶉闄╄瘑鍒玗
    E --> F[鍚堝苟 LLM 涓庤鍒欓闄╃瓑绾
    F --> G{鎰忓浘/椋庨櫓鍒嗘祦}
    G -->|CHAT| H[鐢熸垚鑷劧鍥炲]
    G -->|CONSULT| I[鐢熸垚涓撲笟瀹夋姎鍥炲]
    G -->|KNOWLEDGE| J[鍩轰簬 RAG 鐢熸垚鐭ヨ瘑鍥炵瓟]
    G -->|HIGH_RISK 鎴?HIGH| K[鐢熸垚瀹夊叏鍗辨満鍥炲]
    H --> L[淇濆瓨 AI 鍥炲鍒?chat_message]
    L --> T[鏇存柊 Redis 鐭湡璁板繂]
    T --> U[鎶藉彇骞朵繚瀛?MySQL 闀挎湡璁板繂]
    I --> M[WorkflowRecordTool 鍐?workflow_record]
    J --> M
    K --> M
    M --> N[WorkflowExcelTool 杩藉姞 Excel]
    K --> O[RiskRecordTool 鍐?risk_record]
    O --> P[EmailAlertTool 鍙戦€侀偖浠跺苟鍐?email_alert_log]
    N --> Q[杩斿洖鍝嶅簲涓庢墽琛屽姩浣淽
    P --> Q
    U --> Q
```

## 鏈湴寰皟妯″瀷鎺ュ叆

椤圭洰鎻愪緵鏈湴寰皟妯″瀷鎺ュ叆閰嶇疆鍜岀鐞嗙杩愯鐘舵€佹帴鍙ｃ€傚井璋冩潈閲嶃€佽缁冩暟鎹拰妯″瀷浜х墿涓嶉殢浠撳簱鍒嗗彂锛涢儴缃叉椂鍙皢 LoRA / QLoRA 閫傞厤鍣ㄥ悎骞舵垨灏佽涓?Ollama 妯″瀷锛屽啀閫氳繃閰嶇疆鍒囨崲鍒版湰鍦版ā鍨嬨€?
`pretrain/` 鐩綍鎻愪緵 Qwen2.5-7B 蹇冪悊鍦烘櫙 QLoRA 寰皟宸ョ▼鑴氭墜鏋讹紝鍖呮嫭鏁版嵁娓呮礂銆佽缁冮厤缃€佽缁冭剼鏈€佽瘎浼拌剼鏈€丩oRA 鍚堝苟鑴氭湰鍜?Ollama Modelfile 妯℃澘銆傞粯璁ゅ弬鏁板寘鍚?rank=8銆乤lpha=16銆佸涔犵巼 2e-4銆?bit QLoRA銆傜湡瀹炶缁冩暟鎹€乤dapter銆丟GUF 鏂囦欢涓嶉殢浠撳簱鍒嗗彂銆?
```yaml
llm:
  provider: ollama
  base-url: http://localhost:11434
  model: qwen2.5-mental:latest
  embedding-model: nomic-embed-text

fine-tune:
  enabled: true
  base-model: Qwen2.5
  adapter-type: QLoRA
  adapter-path: ./models/adapters/qwen2.5-mental
  ollama-model: qwen2.5-mental:latest
  training-profile: emotion-dialog-sft
```

绠＄悊鍛樻帴鍙?`GET /api/admin/model/runtime` 鍙煡鐪嬪綋鍓嶆ā鍨嬫彁渚涙柟銆佽亰澶╂ā鍨嬨€佸祵鍏ユā鍨嬪拰寰皟閫傞厤鍣ㄩ厤缃€傝鎺ュ彛涓嶄細杩斿洖 API Key銆?
## 娣峰悎妫€绱笌妯″瀷鍗忓悓

绠＄悊鍛橀€氳繃 `/api/admin/knowledge/upload` 涓婁紶 txt / md 鏂囨。銆傚悗绔細灏嗘枃妗ｅ垏鐗囷紝骞跺悓鏃跺啓鍏ヤ笁澶勶細`knowledge_document` 淇濆瓨鍘熸枃锛宍knowledge_chunk` 淇濆瓨鍒嗙墖锛孋hroma 淇濆瓨鍒嗙墖鍚戦噺锛汱ucene 浼氬熀浜?`knowledge_chunk` 寤虹珛 BM25 鏈湴鍏抽敭璇嶇储寮曘€?
鐢ㄦ埛姣忔鎻愰棶閮戒細鍏堟墽琛屾贩鍚堟绱?RAG锛?1. Dense Retrieval锛氳皟鐢?embedding 妯″瀷鍚戦噺鍖?query锛屽苟鍦?Chroma 涓仛璇箟鍙洖銆?2. Sparse Retrieval锛氫娇鐢?Lucene SmartChineseAnalyzer + BM25 鍋氫腑鏂囧叧閿瘝鍙洖銆?3. Rank Fusion锛氫娇鐢?RRF锛圧eciprocal Rank Fusion锛夋妸鍚戦噺鍙洖鍜?BM25 鍙洖缁撴灉铻嶅悎鎺掑簭銆?4. Prompt Augmentation锛氬彇铻嶅悎鍚庣殑 topK 鐗囨鎷煎叆 Prompt锛屽啀鐢卞ぇ妯″瀷鐢熸垚鍥炲銆?
杩欑璁捐姣斿崟绾悜閲忔绱㈡洿閫傚悎绠€鍘嗗睍绀猴細鍚戦噺鍙洖鎿呴暱璇箟鐩歌繎闂锛孊M25 鎿呴暱鍏抽敭璇嶃€佹湳璇拰绮剧‘鍖归厤锛孯RF 鑳藉湪涓嶄緷璧栫粺涓€鍒嗘暟灏哄害鐨勬儏鍐典笅铻嶅悎涓や釜鎺掑簭鍒楄〃銆傝嫢娌℃湁鍛戒腑鐗囨锛孭rompt 涓細甯︿笂鈥滃綋鍓嶇煡璇嗗簱娌℃湁鎵惧埌鏄庣‘渚濇嵁鈥濓紝妯″瀷浠嶅彲缁欏嚭涓€鑸€ч櫔浼存垨绉戞櫘鍥炵瓟锛屼絾涓嶄細浼€犵煡璇嗗簱鏉ユ簮銆?
妫€绱㈡ā寮忓彲閫氳繃閰嶇疆鍒囨崲锛?```yaml
retrieval:
  mode: hybrid       # hybrid / dense / sparse
  dense-top-k: 8
  sparse-top-k: 8
  rrf-k: 60
  lucene-index-path: ./data/lucene/knowledge
  rebuild-on-startup: true
```

## 璁板繂璁捐

鐭湡璁板繂浣跨敤 Redis锛宬ey 涓?`chat:memory:{userId}:{sessionId}`锛屼繚瀛樻渶杩?10 杞敤鎴蜂笌鍔╂墜瀵硅瘽锛孴TL 涓?7 澶┿€傚畠瑙ｅ喅鐨勬槸褰撳墠浼氳瘽閲岀殑涓婁笅鏂囪繛缁€с€?
闀挎湡璁板繂浣跨敤 MySQL 鐨?`user_memory` 琛紝淇濆瓨 LLM 浠庡璇濅腑鎶藉彇鍑虹殑鍙鐢ㄦ憳瑕侊紝渚嬪闀挎湡鍘嬪姏婧愩€佹寔缁叧娉ㄧ偣銆佸亸濂界殑鏀寔鏂瑰紡銆侀渶瑕佸悗缁叧娉ㄧ殑椋庨櫓鐐广€傞暱鏈熻蹇嗕笉鐩存帴澶嶅埗鍏ㄩ儴鑱婂ぉ鍘熸枃锛屼篃涓嶄繚瀛樺尰鐤楄瘖鏂粨璁恒€佽嚜浼ゆ柟寮忕粏鑺傘€佽韩浠借瘉鍙枫€佹墜鏈哄彿銆佷綇鍧€绛夐珮搴︽晱鎰熶俊鎭€傜敓鎴愬洖澶嶆椂浼氬悓鏃舵嫾鍏?RAG銆丷edis 鐭湡璁板繂鍜?MySQL 闀挎湡璁板繂鎽樿銆?
## 楂橀闄╄瘑鍒€昏緫

绯荤粺浣跨敤涓ゅ眰鍒ゆ柇锛?
1. 瑙勫垯鍏抽敭璇嶈瘑鍒細濡傗€滄兂姝烩€濃€滀笉鎯虫椿鈥濃€滄椿涓嶄笅鍘烩€濃€滆嚜鏉€鈥濃€滅粨鏉熺敓鍛解€濃€滀激瀹宠嚜宸扁€濃€滃壊鑵曗€濃€滆烦妤尖€濃€滄病浜烘晳鎴戔€濃€滄垜鎾戜笉浣忎簡鈥濃€滄姤澶嶁€濃€滄潃浜衡€濃€滀激瀹冲埆浜衡€濈瓑銆?2. LLM JSON 鍒嗙被锛氭ā鍨嬪繀椤昏緭鍑?`intent`銆乣riskLevel`銆乣riskType`銆乣reason`銆?
鏈€缁堥闄╃瓑绾у彇瑙勫垯璇嗗埆涓?LLM 鍒ゆ柇涓殑杈冮珮绛夌骇銆傚彧瑕佹渶缁?`riskLevel = HIGH` 鎴?`intent = HIGH_RISK`锛岀郴缁熷氨璧伴珮椋庨櫓瀹夊叏鍥炲銆佹暟鎹簱璁板綍銆丒xcel 鍐欏叆鍜岄偖浠堕璀︽祦绋嬨€?
## Excel 璁板綍閫昏緫

`CONSULT`銆乣KNOWLEDGE`銆乣HIGH_RISK` 浼氬啓鍏?`workflow_record`锛屽苟閫氳繃 EasyExcel 杩藉姞鍒?`app.excel.workflow-path`锛岄粯璁?`./data/workflow-records.xlsx`銆俙CHAT` 鍙啓 `chat_message`锛屼笉鍐?Excel锛屼笉鍙戦偖浠躲€傚悗鍙版帴鍙?`/api/admin/workflow-records/export` 鍙互瀵煎嚭鍏ㄩ儴 `workflow_record`銆?
Excel 琛ㄥご鍖呮嫭锛氳褰旾D銆佺敤鎴稩D銆佷細璇滻D銆佺敤鎴烽棶棰樸€佹剰鍥剧被鍨嬨€侀闄╃被鍨嬨€侀闄╃瓑绾с€佹槸鍚﹀懡涓璕AG銆丷AG鍙傝€冪墖娈点€丄I鍥炲銆佹槸鍚﹀彂閫侀偖浠躲€佸垱寤烘椂闂淬€?
## 閭欢棰勮閫昏緫

楂橀闄╂祦绋嬩細鍐欏叆 `risk_record`锛岃皟鐢?`EmailAlertTool`锛屽苟鍦?`email_alert_log` 璁板綍鍙戦€佺姸鎬併€傞粯璁?`app.mail.enabled=false`锛屼笉浼氱湡瀹炲彂閫侊紱閰嶇疆鐪熷疄 SMTP 骞舵敼涓?`true` 鍚庝細鍙戦€侀偖浠剁粰 `app.mail.alert-receiver`銆?
鐪熷疄閭欢闇€瑕侀厤缃袱绫诲湴鍧€锛?
- `spring.mail.username`锛歋MTP 鐧诲綍璐﹀彿锛岄€氬父灏辨槸鍙戜欢閭銆?- `app.mail.from`锛氶偖浠?From 鍙戜欢浜哄湴鍧€锛岄€氬父鍜?`spring.mail.username` 鐩稿悓銆?- `app.mail.alert-receiver`锛氶珮椋庨櫓棰勮鏀朵欢浜哄湴鍧€銆?
寤鸿鎶婄湡瀹為偖绠便€佹巿鏉冪爜鍜屾敹浠朵汉鍐欏湪鏈湴 `backend/src/main/resources/application-local.yml`锛岃鏂囦欢宸插姞鍏?`.gitignore`锛屼笉浼氫笂浼?GitHub锛?
```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: your_email@qq.com
    password: your_smtp_authorization_code
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: false
          ssl:
            enable: true

app:
  mail:
    enabled: true
    from: your_email@qq.com
    alert-receiver: receiver@example.com
```

濡傛灉浣跨敤 587 + STARTTLS锛屽彲浠ユ敼鎴愶細

```yaml
spring:
  mail:
    port: 587
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
          ssl:
            enable: false
```

鍚庡彴鎺ュ彛 `POST /api/admin/email/test` 鍙敤浜庢祴璇曠湡瀹為偖浠堕摼璺紝鍙戦€佺粨鏋滀細鍐欏叆 `email_alert_log`銆?
## 妯″瀷鍒囨崲

榛樿浣跨敤 OpenAI-compatible API銆傝亰澶╂ā鍨嬪拰宓屽叆妯″瀷鏄袱涓厤缃細鑱婂ぉ妯″瀷鐢ㄤ簬鎰忓浘璇嗗埆銆侀闄╁垎绫诲拰鍥炲鐢熸垚锛涘祵鍏ユā鍨嬬敤浜庣煡璇嗗簱鏂囨。鍜岀敤鎴烽棶棰樺悜閲忓寲锛屾槸 RAG 妫€绱㈠繀椤荤殑銆傝嫢閮ㄧ讲鏈湴寰皟妯″瀷锛屽皢 `llm.provider` 鍒囨崲涓?`ollama` 骞堕厤缃湰鍦版ā鍨嬪悕鍗冲彲銆?
```yaml
llm:
  provider: openai
  base-url: https://dashscope.aliyuncs.com/compatible-mode
  api-key: ${LLM_API_KEY:}
  model: qwen-plus
  embedding-model: text-embedding-v3
```

## 鍚姩姝ラ

1. 鍚姩 Docker MySQL 鍜?Chroma銆傞娆″惎鍔ㄤ細鎷夊彇 `mysql:8.0` 鍜?`chromadb/chroma:0.5.23`锛屽鏋滃嚭鐜?`context deadline exceeded`锛岄€氬父鏄?Docker Hub 缃戠粶瓒呮椂锛岄噸璺戝悓涓€鏉″懡浠ゅ嵆鍙鐢ㄥ凡涓嬭浇鐨勯暅鍍忓眰锛?
```powershell
docker compose up -d mysql chroma
```

濡傛灉澶氭瓒呮椂锛屽彲浠ュ厛鍒嗗紑鎷夊彇闀滃儚锛屽啀鍚姩鏈嶅姟锛?
```powershell
docker pull mysql:8.0
docker pull chromadb/chroma:0.5.23
docker compose up -d mysql chroma
```

椤圭洰閰嶇疆涓?MySQL 浣跨敤 `127.0.0.1:3307`锛屽搴?Docker 瀹瑰櫒鍐呯殑 `3306`锛汣hroma 浣跨敤 `127.0.0.1:8000`锛汻edis 浣跨敤澶栭儴鍦板潃 `192.168.255.131:6379`銆?
2. 璁剧疆鍦ㄧ嚎妯″瀷 API Key锛?
```powershell
$env:LLM_API_KEY="浣犵殑鍦ㄧ嚎妯″瀷 API Key"
```

鏈湴婕旂ず鐜涔熷彲浠ヤ娇鐢?`backend/src/main/resources/application-local.yml`锛岃鏂囦欢宸插姞鍏?`.gitignore`锛岀敤浜庝繚瀛樻湰鏈?MySQL銆丷edis 鍜屾ā鍨?API Key锛屼笉浼氫笂浼犲埌 GitHub銆?
3. 鎵撳寘骞跺惎鍔ㄥ悗绔€備繚鎸佸惎鍔ㄥ悗绔殑 PowerShell 绐楀彛鎵撳紑锛?
```powershell
C:\Users\83848\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd -f backend\pom.xml -DskipTests package
D:\JAVA\jdk21\bin\java.exe -jar backend\target\mental-companion-assistant-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

4. 鍚姩鍓嶇锛?
```powershell
cd frontend
npm install
npm run dev
```

5. 鎵撳紑椤甸潰锛?
```text
http://localhost:5173
```

濡傛灉浣犲凡缁忓湪鏃х増鏈惎鍔ㄨ繃 MySQL volume锛宍init.sql` 涓嶄細鑷姩閲嶈窇銆傚綋鍓嶅悗绔惎鍔ㄦ椂浼氳嚜鍔ㄨˉ寤?`knowledge_chunk` 琛紱涔熷彲浠ユ墽琛屼笅闈㈠懡浠ゅ悓姝ュ叏閮ㄥ垵濮嬪寲 SQL锛屾垨鑰?`docker compose down -v` 娓呯┖婕旂ず鏁版嵁鍚庨噸鍚細

```powershell
docker exec -i mental-companion-mysql mysql -uroot -p1111 mental_companion < docker/mysql/init.sql
```

鍗囩骇娣峰悎妫€绱㈠悗锛屽缓璁噸鏂颁笂浼犱竴娆＄煡璇嗗簱鏂囨。銆傛柊涓婁紶鐨勬枃妗ｄ細鍚屾椂杩涘叆 Chroma 鍚戦噺绱㈠紩鍜?Lucene BM25 绱㈠紩锛涙棫鐗堟湰宸茬粡涓婁紶杩囦絾娌℃湁鍐欏叆 `knowledge_chunk` 鐨勬枃妗ｏ紝闇€瑕侀噸鏂颁笂浼犲悗鎵嶈兘鍙備笌 BM25 鍙洖銆?
## 娴嬭瘯璐﹀彿

- 绠＄悊鍛橈細`admin / admin123`
- 鏅€氱敤鎴凤細`user / user123`

## 婕旂ず娴佺▼

1. 浣跨敤绠＄悊鍛樿处鍙风櫥褰曘€?2. 杩涘叆绠＄悊鍛橀〉锛屼笂浼?`data/sample-knowledge.md`銆?3. 鍥炲埌鑱婂ぉ椤碉紝渚濇杈撳叆涓嬮潰鐨勬紨绀虹敤渚嬨€?4. 鍦ㄥ彸渚у伐浣滄祦鐘舵€侀潰鏉胯瀵熸剰鍥俱€侀闄╃瓑绾с€丷AG 鍛戒腑銆丒xcel 鍐欏叆鍜岄偖浠跺姩浣溿€?5. 鍥炲埌绠＄悊鍛橀〉鏌ョ湅宸ヤ綔娴佽褰曘€侀暱鏈熻蹇嗐€侀闄╄褰曘€侀偖浠舵棩蹇楋紝骞跺鍑?Excel銆?
## 婕旂ず鐢ㄤ緥

### 1. 闂茶亰

杈撳叆锛?
```text
浣犲ソ锛屼粖澶╂湁鐐规棤鑱娿€?```

棰勬湡锛?
- 鎵ц RAG
- `intent = CHAT`
- 涓嶅啓 Excel
- 涓嶅彂閭欢
- 鐢熸垚鑷劧鍥炲

### 2. 鏅€氬績鐞嗗挩璇?
杈撳叆锛?
```text
鎴戞渶杩戝帇鍔涘緢澶э紝缁忓父鐫′笉鐫€锛屾劅瑙夊緢绱€?```

棰勬湡锛?
- 鎵ц RAG
- `intent = CONSULT`
- `riskLevel = LOW` 鎴?`MEDIUM`
- 鍐欏叆 `workflow_record`
- 鍐欏叆 Excel
- 涓嶅彂閭欢

### 3. 鐭ヨ瘑搴撻棶绛?
杈撳叆锛?
```text
闀挎湡鐒﹁檻鏃跺彲浠ョ敤鍝簺鏀炬澗鏂规硶锛?```

棰勬湡锛?
- 鎵ц RAG
- `intent = KNOWLEDGE`
- 鍐欏叆 `workflow_record`
- 鍐欏叆 Excel
- 杩斿洖甯︾煡璇嗗簱渚濇嵁鐨勫洖绛?
### 4. 楂橀闄?
杈撳叆锛?
```text
鎴戠湡鐨勬椿涓嶄笅鍘讳簡锛屾兂缁撴潫杩欎竴鍒囥€?```

棰勬湡锛?
- 鎵ц RAG
- `intent = HIGH_RISK`
- `riskLevel = HIGH`
- 鍐欏叆 `workflow_record`
- 鍐欏叆 `risk_record`
- 鍐欏叆 Excel
- 鍙戦€侀偖浠堕璀︽垨鍦ㄦ湭鍚敤 SMTP 鏃跺啓鍏?`SKIPPED` 閭欢鏃ュ織
- 杩斿洖瀹夊叏鍥炲

## API 鎽樿

- `POST /api/auth/login`
- `POST /api/chat/send`
- `POST /api/admin/knowledge/upload`
- `GET /api/admin/knowledge/list`
- `GET /api/admin/workflow-records`
- `GET /api/admin/workflow-records/export`
- `GET /api/admin/risk-records`
- `GET /api/admin/memories`
- `GET /api/admin/model/runtime`
- `POST /api/admin/email/test`
- `GET /api/admin/email/logs`
