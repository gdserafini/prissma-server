# Prissma Server

## Descrição
...

## Pré-requisitos
* Docker/Docker Desktop

## Como rodar o projeto localmente

Copie o arquivo de exemplo de variaveis:

```bash
cp .env.example .env
```

No Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Depois ajuste apenas os valores necessarios no `.env` e execute:

```bash
docker compose up --build
```

O Docker Compose inicia a aplicacao Spring Boot e um PostgreSQL local. A API fica
disponivel em `http://localhost:8080`.

## Cloud Run + Neon PostgreSQL

O backend nao precisa ter credenciais do Neon versionadas no GitHub. O Cloud Run
injeta as configuracoes em tempo de execucao por variaveis de ambiente.

No Neon, copie os dados da conexao. Uma URL fornecida pelo Neon costuma ter este
formato:

```text
postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require
```

Para o Spring Boot, use o formato JDBC e configure no Cloud Run:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=USUARIO
SPRING_DATASOURCE_PASSWORD=SENHA
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0
```

O Flyway continua habilitado e executa automaticamente as migrations em
`src/main/resources/db/migration` quando a aplicacao inicia.

### Outras variaveis de producao

Tambem devem ser configuradas fora do repositorio:

```text
JWT_SECRET
APP_ADMIN_PASSWORD
SPRING_MAIL_USERNAME
SPRING_MAIL_PASSWORD
OPENAI_API_KEY
PASSWORD_RESET_FRONTEND_URL
```

Nunca coloque senhas, tokens ou connection strings reais em
`application.yaml`, `docker-compose.yaml` ou arquivos versionados.
