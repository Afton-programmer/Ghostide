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

echo 'Welcome to Ghost Ide'

