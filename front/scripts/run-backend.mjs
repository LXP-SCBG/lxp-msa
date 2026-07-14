// 백엔드(Gradle)를 OS에 맞는 wrapper로 실행한다.
// - Windows: gradlew.bat
// - macOS/Linux: ./gradlew
// (concurrently 인라인 명령은 OS별 셸 차이로 깨지므로 런처로 분리)
import { spawn } from "node:child_process";

const isWindows = process.platform === "win32";
const command = isWindows ? "gradlew.bat" : "./gradlew";

const child = spawn(command, ["bootRun"], {
  cwd: "..", // lxp-app 기준 상위 = 레포 루트(gradlew 위치)
  stdio: "inherit",
  shell: true,
});

child.on("exit", (code) => process.exit(code ?? 0));
