#bin/shell

# download in server
rm -f /etc/resolv.conf
echo "nameserver 8.8.8.8" > /etc/resolv.conf

if command -v node >/dev/null 2>&1; then
  echo "[GHOSTIDE] node already installed: $(node -v)"
else
  echo "[GHOSTIDE] installing node.js via nodesource..."
  curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && apt-get install -y nodejs

  if ! command -v node >/dev/null 2>&1; then
    echo "[GHOSTIDE] nodesource failed, falling back to Debian apt package..."
    apt-get update
    apt-get install -y nodejs npm
  fi

  if command -v node >/dev/null 2>&1; then
    echo "[GHOSTIDE] node ready: $(node -v)"
  else
    echo "[GHOSTIDE] ERROR: node could not be installed (check network/interfaces)."
    echo "[GHOSTIDE] Run manually inside Debian: apt-get update && apt-get install -y nodejs"
  fi
fi

clear

echo "Loading download web lsp from Html js css..."

npm install -g typescript ts-node
npm i -g typescript typescript-language-server
npm i -g @t1ckbase/vscode-langservers-extracted
npm i -g @olrtg/emmet-language-server
chmod -R 755 /usr/lib/node_modules/ 2>/dev/null

# Java: Adoptium Temurin (17 or 25) + Eclipse JDT Language Server
JAVA_VERSION="__GHOSTIDE_JAVA_VERSION__"
case "$JAVA_VERSION" in
  17|25) ;;
  *) JAVA_VERSION=17 ;;
esac

JDK_HOME=$(ls -d /usr/lib/jvm/temurin-${JAVA_VERSION}-jdk-* 2>/dev/null | head -n 1)
if [ -n "$JDK_HOME" ] && [ -x "$JDK_HOME/bin/javac" ]; then
  echo "[GHOSTIDE] JDK already installed: $("$JDK_HOME/bin/java" -version 2>&1 | head -n 1)"
else
  echo "[GHOSTIDE] installing Temurin JDK ${JAVA_VERSION}..."
  export DEBIAN_FRONTEND=noninteractive
  apt-get update && apt-get install -y gnupg wget gpg ca-certificates
  mkdir -p /etc/apt/keyrings
  wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor --yes -o /etc/apt/keyrings/adoptium.gpg
  CODENAME=$(. /etc/os-release; echo "${VERSION_CODENAME:-bookworm}")
  echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${CODENAME} main" > /etc/apt/sources.list.d/adoptium.list
  apt-get update && apt-get install -y temurin-${JAVA_VERSION}-jdk
  JDK_HOME=$(ls -d /usr/lib/jvm/temurin-${JAVA_VERSION}-jdk-* 2>/dev/null | head -n 1)
fi

if [ ! -x /root/jdtls/bin/jdtls ]; then
  echo "[GHOSTIDE] installing Eclipse JDT Language Server..."
  mkdir -p /root/jdtls
  (cd /root/jdtls && wget -q https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz -O jdtls.tar.gz && tar -xzf jdtls.tar.gz && rm -f jdtls.tar.gz && chmod +x bin/jdtls)
fi

if command -v java >/dev/null 2>&1; then
  java -version
else
  "$JDK_HOME/bin/java" -version
fi

echo 'Welcome to Ghost Ide'

