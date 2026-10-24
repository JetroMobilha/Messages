# 🛡️ Messages Pro & Stealth Private Zone

> **Aplicação Nativa de Mensagens Padrão (Default SMS/MMS) com Cofre Furtivo, Filtro de Palavras-Chave, Limpeza de Fundo e Gateway Híbrido de Pagamentos.**

---

## 🌟 Visão Geral

O **Messages Pro** é uma aplicação nativa Android de mensagens SMS/MMS rápida, segura e leve, que incorpora uma **Zona Privada Furtiva (Cofre)** totalmente indetetável. 

Por ser a aplicação de SMS padrão do sistema operativo, intercepta e processa mensagens e chamadas secretas **na origem**, sem deixar qualquer rasto na base de dados pública do Android, no histórico de chamadas ou nas notificações do telefone.

---

## 🔐 Funcionalidades Secretas & Zona Privada (Vault)

### 1. Gatilhos de Acesso Furtivos (Stealth Triggers)
- **PIN na Barra de Pesquisa:** Digitar o PIN do Cofre (padrão `0000`) na pesquisa do ecrã principal abre a Zona Privada.
- **PIN de Pânico / Coação:** Sob ameaça ou coação, digitar o PIN de emergência (padrão `9999`) abre uma **Falsa Zona Privada** (`FakeVaultActivity`) completamente vazia, enganando terceiros.
- **Toque Longo na Barra de Topo:** Pressionar continuamente (1-2s) a barra de título do menu superior.
- **PINs Totalmente Customizáveis:** Altere o PIN do Cofre e o PIN de Pânico facilmente no painel de definições.

### 2. Gestão de Contactos & Importação Facilitada
- **Múltiplas Origens de Inserção:** Importe contactos para o Cofre diretamente do **Registo de Chamadas Recentes** (Call Log), da **Agenda Telefónica** (`ACTION_PICK`) ou inserindo manualmente.
- **Organização por Grupos / Pastas:** Atribua nomes de grupos (ex: *Trabalho*, *Família*, *Geral*) para manter a lista do Cofre organizada.
- **Botão "Ligar de Volta" (Dialer):** Faça chamadas rápidas para os contactos do Cofre a partir de um botão de atalho direto no app de telefone nativo.
- **Controlo Independente (SMS vs Chamadas):** Ative ou desative o bloqueio de chamadas (`blockCalls`) individualmente por contacto. Se desativado, as SMS continuam escondidas no Cofre, mas as chamadas tocam normalmente.

### 3. Filtro de Palavras-Chave Furtivas (Stealth Keywords)
- O sistema varre o texto de todas as mensagens recebidas. Se uma SMS contiver uma palavra ou frase protegida (ex: *"Projeto X"* ou *"Surpresa"*), ela é **automaticamente movida para o Cofre**, mesmo que o número do remetente não esteja na lista de contactos!

### 4. Políticas de Interação por Contacto / Keyword
- **`0` - Furtivo Total:** Silêncio absoluto. A SMS/MMS ou Chamada é intercetada sem som, vibração ou notificação.
- **`1` - Discreto:** Emite uma notificação genérica do sistema (*"Tem uma mensagem nova"*). Ao tocar na notificação, abre a caixa pública normal (sem mensagens novas), sinalizando ao utilizador para abrir o Cofre manualmente.
- **`2` - Autodestruição:** A mensagem secreta apaga-se automaticamente e de forma permanente X tempo após ser lida pelo utilizador.

### 5. Expurgo Imediato (Purge) & Limpeza Automática em Segundo Plano
- **Expurgo Instantâneo (Purge):** No exato segundo em que um contacto entra para o Cofre, todas as SMS, MMS e chamadas antigas desse número visíveis na caixa pública e nos registos do telemóvel são **pulverizadas**.
- **Background Sweeper (`VaultCleanupWorker` + `WorkManager`):** Motor de limpeza automática em segundo plano que executa varreduras periódicas (configuráveis de 15 min a 24 horas).
- **Rastreador de Arrancamento (`VaultBootReceiver`):** Reativa automaticamente o motor de limpeza e o rastreador no milissegundo em que o telemóvel volta a ligar (`BOOT_COMPLETED`).

### 6. Proteção Ativa Anti-Snoop & Destruição Remota
- **Filtro de Chamadas Silencioso:** `VaultCallScreeningService` rejeita chamadas de contactos-alvo antes de tocarem e oculta o registo do histórico (`setSkipCallLog(true)`).
- **Proteção Visual:** `FLAG_SECURE` bloqueia capturas de ecrã (screenshots) e esconde a app da lista de aplicações recentes.
- **Destruição Remota por SMS:** Enviar uma SMS contendo `#WIPE_VAULT#` de qualquer número limpa **imediatamente** todas as mensagens, contactos e registos do Cofre.
- **Bloqueio Biométrico:** Validação por Impressão Digital/Facial ao abrir o Cofre.

---

## ☁️ Backup Automático Encriptado (Zero-Knowledge)

- **Encriptação Forte:** Algoritmo **AES-256-CBC** com derivação de chave **PBKDF2WithHmacSHA256** (10.000 iterações) a partir do Nome de Utilizador + Palavra-passe.
- **Sincronização Nuvem em 1 Clique:** Envia os pacotes encriptados automaticamente para a API Laravel (`messages-api`).
- **Restauro Sem Stress:** Reinstale a app ou mude de dispositivo, introduza o Utilizador/Senha e restaure tudo com 1 clique.

---

## 💳 Gateway Híbrido de Pagamentos ("Traffic Cop")

A aplicação integra um sistema inteligente de checkout que direciona o utilizador com base na sua localização:

- **Angola (AppyPay / Multicaixa):** Detecta prefixos `+244` e gera pagamentos em Kwanzas (**5.000,00 AOA**) via **Multicaixa Express** ou **Referência Multicaixa** (Entidade/Referência).
- **Internacional (Paddle / Stripe):** Processa cartões Visa/Mastercard/PayPal em USD (**$4.99 USD**).

---

## 🛠️ Arquitetura e Estrutura Técnica

- **Linguagem:** Kotlin 100% Nativo.
- **Base de Dados:** Room Database (`MessagesDatabase` v15 com `vault_contacts`, `vault_messages`, `vault_calls`, `vault_keywords`).
- **Comunicação Assíncrona:** `ensureBackgroundThread`, Coroutines, `goAsync()`, `WorkManager`.
- **Servidor Backend:** Laravel 12 API (`C:\xampp\htdocs\messages-api`).

---

## 🚀 Como Compilar e Executar

1. Abra o projeto no **Android Studio 2026.1.2+**.
2. Certifique-se de que o JDK 17 está selecionado nas definições do projeto.
3. Sincronize o Gradle e execute no emulador ou num dispositivo físico com Android 9.0+ (API 28+).
4. Para testar a comunicação com a API Laravel local no emulador, certifique-se de que o XAMPP/Apache está a correr em `localhost`.
