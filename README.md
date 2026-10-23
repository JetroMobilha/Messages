# 🛡️ Messages Pro & Stealth Private Zone

> **Aplicação Nativa de Mensagens Padrão (Default SMS/MMS) com Cofre Furtivo e Gateway Híbrido de Pagamentos.**

---

## 🌟 Visão Geral

O **Messages Pro** é uma aplicação nativa Android de mensagens SMS/MMS rápida, segura e leve, que incorpora uma **Zona Privada Furtiva (Cofre)** totalmente indetetável. 

Por ser a aplicação de SMS padrão do sistema operativo, intercepta e processa mensagens e chamadas secretas **na origem**, sem deixar qualquer rasto na base de dados pública do Android ou nas notificações do telefone.

---

## 🔐 Funcionalidades Secretas & Zona Privada (Vault)

### 1. Gatilhos de Acesso Furtivos (Stealth Triggers)
- **PIN na Barra de Pesquisa:** Digitar `0000` na pesquisa do ecrã principal abre a Zona Privada.
- **Toque Longo na Barra de Topo:** Pressionar continuamente (1-2s) a barra de título do menu superior.
- **PIN de Pânico / Coação (`9999`):** Sob ameaça ou coação, digitar `9999` abre uma **Falsa Zona Privada** completamente vazia, enganando terceiros.

### 2. Políticas de Interação por Contacto
Cada contacto adicionado ao Cofre pode ter uma política personalizada:
- **`0` - Furtivo Total:** Silêncio absoluto. A SMS/MMS ou Chamada é intercetada sem som, vibração ou notificação.
- **`1` - Discreto:** Emite uma notificação genérica do sistema (*"Tem uma mensagem nova"*). Ao tocar na notificação, abre a caixa pública normal (sem mensagens novas), sinalizando ao utilizador para abrir o Cofre manualmente.
- **`2` - Autodestruição:** A mensagem secreta apaga-se automaticamente e de forma permanente X tempo após ser lida pelo utilizador.

### 3. Proteção Ativa Anti-Snoop & Destruição Remota
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
- **Base de Dados:** Room Database (`MessagesDatabase` v13 com `vault_contacts`, `vault_messages`, `vault_calls`).
- **Comunicação Assíncrona:** `ensureBackgroundThread`, Coroutines, `goAsync()`.
- **Servidor Backend:** Laravel 12 API (`C:\xampp\htdocs\messages-api`).

---

## 🚀 Como Compilar e Executar

1. Abra o projeto no **Android Studio 2026.1.2+**.
2. Certifique-se de que o JDK 17 está selecionado nas definições do projeto.
3. Sincronize o Gradle e execute no emulador ou num dispositivo físico com Android 9.0+ (API 28+).
4. Para testar a comunicação com a API Laravel local no emulador, certifique-se de que o XAMPP/Apache está a correr em `localhost`.
